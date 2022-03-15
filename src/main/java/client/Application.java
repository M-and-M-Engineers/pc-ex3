package client;

import common.Glasses;
import common.Resource;
import common.ResourceImpl;
import common.Sugar;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.List;

public class Application {

    private static final int ACCEPT_CODE = 200;
    private static final String BASE_URI = "/scm/";
    private static final String PROPERTY_STATUS = BASE_URI + "properties/status";
    private static final String PROPERTY_RESOURCES = BASE_URI + "properties/resources";
    private static final String ACTIONS_MAKE = BASE_URI + "actions/make";
    private static final String EVENTS = BASE_URI + "events/";
    private static final String SERVING = EVENTS + "serving";
    private static final String ORDERED = EVENTS + "ordered";
    private static final String STATUS_CHANGED = EVENTS + "statusChanged";
    private final WebClient client;
    private final HttpClient cli;
    private final MainGui gui;
    private final int port;
    private final List<Resource> resources;

    public Application(int port){
        this.port = port;
        Vertx vertx = Vertx.vertx();
        this.client = WebClient.create(vertx);
        this.cli = vertx.createHttpClient();

        this.resources = new ArrayList<>();

        this.gui = new MainGui();
        this.gui.setSelectProductAction(product -> {
            final Resource sugar = this.resources.stream().filter(r -> r.getName().equals("Sugar")).findFirst().orElse(new Sugar(0));
            final OrderGui gui = new OrderGui(this.gui, product.getName(), product.getRemaining(), sugar.getRemaining());
            gui.setMakeAction(sugarLevel -> this.make(sugarLevel, product));
            gui.setVisible(true);
        });
        this.gui.setVisible(true);

        this.getStatus();
        this.getResources();
        this.subscribeToServing();
        this.subscribeToRemaining();
        this.subscribeToStatusChanged();
    }

    private void getStatus() {
       client.get(this.port, "localhost", PROPERTY_STATUS)
               .send(r -> this.gui.setStatusText(r.result().bodyAsString()));
    }

    private void getResources() {
        client.get(this.port, "localhost", PROPERTY_RESOURCES)
                .send(r -> {
                    JsonArray array = r.result().bodyAsJsonArray();
                    List<Resource> resources = new ArrayList<>(array.size());
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject jsonObject = array.getJsonObject(i);
                        final Resource resource = new ResourceImpl(jsonObject.getString("name"), jsonObject.getInteger("remaining"));
                        this.resources.add(resource);
                        if (!resource.getName().equals("Sugar") && !resource.getName().equals("Glasses"))
                            resources.add(resource);
                    }
                    this.gui.setProducts(resources);
                });
    }

    private void make(int sugarLevel, Resource product) {
        JsonObject body = new JsonObject();
        body.put("product", product.getName());
        body.put("sugarLevel", sugarLevel);
        client.post(this.port, "localhost", ACTIONS_MAKE).sendJsonObject(body, res -> {
            if (res.result().statusCode() == ACCEPT_CODE) {
                this.gui.showTicket(res.result().bodyAsJsonObject().getInteger("ticket"));
            } else
                this.gui.showDialog("Product unavailable");
        });
    }

    private void subscribeToServing() {
        cli.webSocket(this.port, "localhost", SERVING).onSuccess(socket ->
                socket.handler(buf -> this.gui.setServingNumber(buf.toJsonObject().getInteger("queueNumber"))));
    }

    private void subscribeToRemaining() {
        cli.webSocket(this.port, "localhost", ORDERED).onSuccess(socket ->
                socket.handler(buffer -> {
                    final JsonObject json = buffer.toJsonObject();
                    System.out.println(json);
                    final String product = json.getString("product");
                    final int remaining = json.getInteger("remaining");
                    final int sugarRemaining = json.getInteger("sugarRemaining");
                    final int glassesRemaining = json.getInteger("glassesRemaining");

                    final List<Resource> products = new ArrayList<>(this.resources.size());
                    for (int i = 0; i < this.resources.size(); i++) {
                        final String currentName = this.resources.get(i).getName();
                        if (currentName.equals(product))
                            this.resources.set(i, new ResourceImpl(product, remaining));

                        if (currentName.equals("Sugar"))
                            this.resources.set(i, new Sugar(sugarRemaining));

                        if (currentName.equals("Glasses"))
                            this.resources.set(i, new Glasses(glassesRemaining));

                        if (!currentName.equals("Sugar") && !currentName.equals("Glasses"))
                            products.add(this.resources.get(i));
                    }

                    this.gui.setProducts(products);
                }));
    }

    private void subscribeToStatusChanged() {
        cli.webSocket(this.port, "localhost", STATUS_CHANGED).onSuccess(socket ->
                socket.handler(buf -> this.gui.setStatusText(buf.toString())));
    }

    public static void main(final String[] args) {
        new Application(Integer.parseInt(args[0]));
    }
}
