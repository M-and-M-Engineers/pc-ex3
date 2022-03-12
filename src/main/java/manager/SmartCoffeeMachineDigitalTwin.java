package manager;

import common.Resource;
import common.ResourceImpl;
import common.SmartCoffeeMachineAPI;
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

    private static final String PROPERTY_STATUS = "/scm/properties/status";
    private static final String PROPERTY_RESOURCES = "/scm/properties/resources";
    private static final String ACTIONS_MAKE = "/scm/actions/make";
    private static final String EVENTS = "/scm/events/";
    private static final String SERVING = EVENTS + "serving";
    private static final String STATUS_CHANGED = EVENTS + "statusChanged";
    private static final String REMAINING = EVENTS + "remaining";
    private final WebClient client;
    private final HttpClient cli;
    private final String host;
    private final int port;

    public SmartCoffeeMachineDigitalTwin(final String host, final int port) {
        Vertx vertx = Vertx.vertx();
        this.client = WebClient.create(vertx);
        this.host = host;
        this.port = port;
        this.cli = vertx.createHttpClient();
    }

    @Override
    public Future<String> getStatus() {
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
    public Future<Integer> make(String product, Integer sugarLevel) {
        JsonObject body = new JsonObject();
        body.put("product", product);
        body.put("sugarLevel", sugarLevel);
        return this.client.post(this.port, this.host, ACTIONS_MAKE).sendJsonObject(body)
                .map(res -> Integer.parseInt(res.bodyAsString()));
    }

    @Override
    public Future<Void> subscribeToServing(Handler<JsonObject> handler) {
        return this.cli.webSocket(port, host, SERVING).onComplete(res -> {
            if (res.succeeded()) {
                res.result().handler(buf -> handler.handle(buf.toJsonObject()));
            }
        }).mapEmpty();
    }

    @Override
    public Future<Void> subscribeToRemaining(Handler<JsonObject> handler) {
        return this.cli.webSocket(port, host, REMAINING).onComplete(res -> {
            if (res.succeeded()) {
                res.result().handler(buf -> handler.handle(buf.toJsonObject()));
            }
        }).mapEmpty();
    }

    @Override
    public Future<Void> subscribeToStatusChanged(Handler<String> statusHandler, Handler<Void> closeHandler) {
        return this.cli.webSocket(port, host, STATUS_CHANGED).onComplete(res -> {
            if (res.succeeded()) {
                res.result()
                        .handler(buf -> statusHandler.handle(buf.toString()))
                        .closeHandler(closeHandler);
            }
        }).mapEmpty();
    }
}
