package manager;

import scm.Resource;
import scm.ResourceImpl;
import scm.SmartCoffeeMachineAPI;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.util.ArrayList;
import java.util.List;

public class SmartCoffeeMachineDigitalTwin implements SmartCoffeeMachineAPI {

    private static final String PROPERTY_NAME = "/scm/properties/name";
    private static final String PROPERTY_STATUS = "/scm/properties/state";
    private static final String PROPERTY_RESOURCES = "/scm/properties/resources";
    private static final String ACTIONS_MAKE = "/scm/actions/make";
    private static final String EVENTS = "/scm/events/";
    private static final String SERVING = EVENTS + "serving";
    private static final String STATUS_CHANGED = EVENTS + "stateChanged";
    private static final String ORDERED = EVENTS + "ordered";
    private static final String SERVED = EVENTS + "served";
    private final WebClient client;
    private final String host;
    private final int port;
    private final List<Handler<JsonObject>> servingHandlers;
    private final List<Handler<JsonObject>> orderedHandlers;
    private final List<Handler<JsonObject>> servedHandlers;
    private final List<Handler<String>> stateHandlers;
    private final List<Handler<Void>> closeHandlers;

    public SmartCoffeeMachineDigitalTwin(final String host, final int port) {
        final Vertx vertx = Vertx.vertx();
        this.client = WebClient.create(vertx);
        this.host = host;
        this.port = port;
        final HttpClient cli = vertx.createHttpClient();

        this.servingHandlers = new ArrayList<>();
        this.stateHandlers = new ArrayList<>();
        this.orderedHandlers = new ArrayList<>();
        this.servedHandlers = new ArrayList<>();
        this.closeHandlers = new ArrayList<>();
        cli.webSocket(port, host, SERVING).onSuccess(socket ->
                socket.handler(buf -> this.servingHandlers.forEach(handler -> handler.handle(buf.toJsonObject()))));

        cli.webSocket(port, host, ORDERED).onSuccess(socket ->
                socket.handler(buf -> this.orderedHandlers.forEach(handler -> handler.handle(buf.toJsonObject()))));

        cli.webSocket(port, host, SERVED).onSuccess(socket ->
                socket.handler(buf -> this.servedHandlers.forEach(handler -> handler.handle(buf.toJsonObject()))));

        cli.webSocket(port, host, STATUS_CHANGED).onSuccess(socket ->
                socket.handler(buf -> this.stateHandlers.forEach(handler -> handler.handle(buf.toString())))
                        .closeHandler(unused -> this.closeHandlers.forEach(h -> h.handle(null))));
    }

    @Override
    public Future<String> getName() {
        return this.client.get(this.port, this.host, PROPERTY_NAME)
                .send().map(HttpResponse::bodyAsString);
    }

    @Override
    public Future<String> getState() {
        return this.client.get(this.port, this.host, PROPERTY_STATUS)
                .send().map(HttpResponse::bodyAsString);
    }

    @Override
    public Future<List<Resource>> getResources() {
        return this.client.get(this.port, this.host, PROPERTY_RESOURCES)
                .send().map(res -> {
                    JsonArray array = res.bodyAsJsonArray();
                    List<Resource> resources = new ArrayList<>(array.size());
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject jsonObject = array.getJsonObject(i);
                        resources.add(new ResourceImpl(jsonObject.getString("name"), jsonObject.getInteger("remaining")));
                    }
                    return resources;
                });
    }

    @Override
    public Future<Integer> make(final String product, final Integer sugarLevel) {
        final JsonObject body = new JsonObject();
        body.put("product", product);
        body.put("sugarLevel", sugarLevel);
        return this.client.post(this.port, this.host, ACTIONS_MAKE).sendJsonObject(body)
                .map(res -> Integer.parseInt(res.bodyAsString()));
    }

    public void subscribeToServing(final Handler<JsonObject> handler) {
       this.servingHandlers.add(handler);
    }

    public void subscribeToOrdered(final Handler<JsonObject> handler) {
        this.orderedHandlers.add(handler);
    }

    public void subscribeToServed(final Handler<JsonObject> handler) {
        this.servedHandlers.add(handler);
    }

    public void subscribeToStateChanged(final Handler<String> handler, final Handler<Void> closeHandler) {
        this.stateHandlers.add(handler);
        this.closeHandlers.add(closeHandler);
    }

}
