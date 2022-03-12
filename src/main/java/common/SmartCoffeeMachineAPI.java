package common;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface SmartCoffeeMachineAPI {

    Future<String> getStatus();

    Future<List<Resource>> getResources();

    Future<Integer> make(String product, Integer sugarLevel);

    Future<Void> subscribeToServing(Handler<JsonObject> handler);

    Future<Void> subscribeToRemaining(Handler<JsonObject> handler);

    Future<Void> subscribeToStatusChanged(Handler<String> statusHandler, Handler<Void> closeHandler);
}
