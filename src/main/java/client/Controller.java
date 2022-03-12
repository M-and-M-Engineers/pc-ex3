package client;

import common.Resource;
import common.ResourceImpl;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Controller {

    private static final int ACCEPT_CODE = 200;
    private static final String BASE_URI = "/scm/";
    private static final String PROPERTY_STATUS = BASE_URI + "properties/status";
    private static final String PROPERTY_RESOURCES = BASE_URI + "properties/resources";
    private static final String ACTIONS_MAKE = BASE_URI + "actions/make";
    private static final String EVENTS = BASE_URI + "events/";
    private static final String SERVING = EVENTS + "serving";
    private final WebClient client;
    private final HttpClient cli;
    private final MainGui gui;
    private final int port;
    private Resource selectedProduct;

    public Controller(MainGui gui, int port){
        this.port = port;
        Vertx vertx = Vertx.vertx();
        this.client = WebClient.create(vertx);
        this.cli = vertx.createHttpClient();
        this.gui = gui;
        this.gui.setSelectProductAction(p -> this.selectedProduct = p);
        this.gui.setMakeAction(this::make);
        this.gui.setAvailabilityConsumer(this::getProductStatus);
        this.getStatus();
        this.getResources();
        this.serving();
    }

    private void getProductStatus(String s) {
        this.gui.setProductStatusText(this.gui.getResources().stream().filter(r -> r.getName().equals(s)).findFirst().get().getRemaining());
    }

    public void getStatus() {
       client.get(this.port, "localhost", PROPERTY_STATUS)
               .send(r -> this.gui.setStatusText(r.result().bodyAsJsonObject().getString("status")));
    }

    public void getResources() {
        client.get(this.port, "localhost", PROPERTY_RESOURCES)
                .send(r -> {
                    JsonArray array = r.result().bodyAsJsonArray();
                    List<Resource> resources = new ArrayList<>(array.size());
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject jsonObject = array.getJsonObject(i);
                        if (!jsonObject.getString("name").equals("sugar") && !jsonObject.getString("name").equals("sugar"))
                            resources.add(new ResourceImpl(jsonObject.getString("name"), jsonObject.getInteger("remaining")));
                    }
                    this.gui.setProducts(resources);
                });
    }

    public void make(int sugarLevel) {
        JsonObject body = new JsonObject();
        body.put("product", this.selectedProduct.getName());
        body.put("sugarLevel", sugarLevel);
        client.post(this.port, "localhost", ACTIONS_MAKE).sendJsonObject(body, res -> {
            if (res.result().statusCode() == ACCEPT_CODE) {
                this.gui.showTicket(res.result().bodyAsJsonObject().getInteger("ticket"));
            } else
                this.gui.showDialog("Product unavailable");
        });
    }

    public void serving() {
        cli.webSocket(this.port, "localhost", SERVING, res -> {
            if (res.succeeded()) {
                WebSocket ws = res.result();
                ws.handler(buf -> this.gui.setServingNumber(buf.toJsonObject().getInteger("queueNumber")));
            }
        });
    }
}
