package scm;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SmartCoffeeMachineService extends AbstractVerticle {

    private static final int SERVICE_UNAVAILABLE = 503;
    private static final String PROPERTY_STATUS = "/scm/properties/status";
    private static final String PROPERTY_RESOURCES = "/scm/properties/resources";
    private static final String ACTIONS_MAKE = "/scm/actions/make";
    private static final String EVENTS = "/scm/events/";
    private static final String SERVING = EVENTS + "serving";
    private static final String STATUS_CHANGED = EVENTS + "statusChanged";
    private static final String REMAINING = EVENTS + "remaining";
    private final int port;
    private final List<ServerWebSocket> servingSubscribers;
    private final List<ServerWebSocket> remainingSubscribers;
    private final List<ServerWebSocket> statusSubscribers;
    private final SmartCoffeeMachineModel model;

    public SmartCoffeeMachineService(SmartCoffeeMachineModel smartCoffeeMachineModel, int port) {
        this.model = smartCoffeeMachineModel;
        this.port = port;
        this.servingSubscribers = new ArrayList<>();
        this.statusSubscribers = new ArrayList<>();
        this.remainingSubscribers = new ArrayList<>();
        this.setupRoutes();
    }

    private void setupRoutes() {
        Router router = Router.router(this.getVertx());
        router.get(PROPERTY_STATUS).handler(this::getStatus);
        router.get(PROPERTY_RESOURCES).handler(this::getProducts);
        router.post(ACTIONS_MAKE).handler(this::make);
        this.setupSocket();
        this.startServer(router);
    }

    private void setupSocket() {
        this.model.subscribeToServing(jsonObject -> {
            Iterator<ServerWebSocket> iterator = this.servingSubscribers.iterator();
            while (iterator.hasNext()){
                ServerWebSocket socket = iterator.next();
                if (socket.isClosed())
                    iterator.remove();
                else {
                    try {
                        socket.write(jsonObject.toBuffer());
                    } catch (Exception exception) {
                        iterator.remove();
                    }
                }
            }
        }).onFailure(Throwable::printStackTrace);
        this.model.subscribeToRemaining(jsonObject -> {
            Iterator<ServerWebSocket> iterator = this.remainingSubscribers.iterator();
            while (iterator.hasNext()){
                ServerWebSocket socket = iterator.next();
                if (socket.isClosed())
                    iterator.remove();
                else {
                    try {
                        socket.write(jsonObject.toBuffer());
                    } catch (Exception exception) {
                        iterator.remove();
                    }
                }
            }
        }).onFailure(Throwable::printStackTrace);
        this.model.subscribeToStatusChanged(status -> {
            Iterator<ServerWebSocket> iterator = this.statusSubscribers.iterator();
            while (iterator.hasNext()){
                ServerWebSocket socket = iterator.next();
                if (socket.isClosed())
                    iterator.remove();
                else {
                    try {
                        socket.writeTextMessage(status);
                    } catch (Exception exception) {
                        iterator.remove();
                    }
                }
            }
        }, null);
    }

    private void startServer(Router router) {
        this.model.getVertx().createHttpServer().webSocketHandler(handler -> {
            if (handler.path().equals(SERVING))
                this.servingSubscribers.add(handler);
            else if (handler.path().equals(STATUS_CHANGED))
                this.statusSubscribers.add(handler);
            else if (handler.path().equals(REMAINING))
                this.remainingSubscribers.add(handler);
            else
                handler.reject();
        })
        .requestHandler(router)
        .listen(port);
    }

    private void getStatus(RoutingContext routingContext) {
        JsonObject response = new JsonObject();
        this.model.getStatus().onSuccess(status -> {
            response.put("status", status);
            routingContext.response().end(response.toBuffer());
        });
    }

    private void getProducts(RoutingContext routingContext) {
        JsonArray response = new JsonArray();
        this.model.getResources().onSuccess(products -> products.forEach(response::add));
        routingContext.response().end(response.toBuffer());
    }

    private void make(RoutingContext routingContext) {
        routingContext.request().bodyHandler(b -> {
            JsonObject body = b.toJsonObject();
            this.model.make(body.getString("product"), body.getInteger("sugarLevel")).onComplete(res -> {
                JsonObject response = new JsonObject();
                if (res.succeeded()) {
                    response.put("ticket", res.result());
                    routingContext.response().end(response.toBuffer());
                } else
                    routingContext.fail(SERVICE_UNAVAILABLE);
            });
        });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new SmartCoffeeMachineService(new SmartCoffeeMachineModel(vertx), Integer.parseInt(args[0])));
    }
}
