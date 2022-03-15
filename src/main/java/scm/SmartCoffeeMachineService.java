package scm;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
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
    private static final String PROPERTY_NAME = "/scm/properties/name";
    private static final String PROPERTY_STATUS = "/scm/properties/status";
    private static final String PROPERTY_RESOURCES = "/scm/properties/resources";
    private static final String ACTIONS_MAKE = "/scm/actions/make";
    private static final String EVENTS = "/scm/events/";
    private static final String SERVING = EVENTS + "serving";
    private static final String STATUS_CHANGED = EVENTS + "statusChanged";
    private static final String ORDERED = EVENTS + "ordered";
    private static final String SERVED = EVENTS + "served";
    private final List<ServerWebSocket> servingSubscribers;
    private final List<ServerWebSocket> orderedSubscribers;
    private final List<ServerWebSocket> servedSubscribers;
    private final List<ServerWebSocket> statusSubscribers;
    private final String name;
    private final int port;
    private SmartCoffeeMachineModel model;

    public SmartCoffeeMachineService(final String name, int port) {
        this.name = name;
        this.port = port;
        this.servingSubscribers = new ArrayList<>();
        this.statusSubscribers = new ArrayList<>();
        this.orderedSubscribers = new ArrayList<>();
        this.servedSubscribers = new ArrayList<>();
    }

    @Override
    public void start() {
        this.model = new SmartCoffeeMachineModel(this.name, this.getVertx().eventBus());
        this.createSockets();
        this.startServer(this.createRoutes());
    }

    private Router createRoutes() {
        Router router = Router.router(this.getVertx());
        router.get(PROPERTY_NAME).handler(this::getName);
        router.get(PROPERTY_STATUS).handler(this::getStatus);
        router.get(PROPERTY_RESOURCES).handler(this::getResources);
        router.post(ACTIONS_MAKE).handler(this::make);
        return router;
    }

    private void createSockets() {
        final EventBus eventBus = this.getVertx().eventBus();
        eventBus.consumer("events/serving", jsonObject -> {
            Iterator<ServerWebSocket> iterator = this.servingSubscribers.iterator();
            while (iterator.hasNext()){
                ServerWebSocket socket = iterator.next();
                if (socket.isClosed())
                    iterator.remove();
                else {
                    try {
                        socket.write(((JsonObject) jsonObject.body()).toBuffer());
                    } catch (Exception exception) {
                        iterator.remove();
                    }
                }
            }
        });
        eventBus.consumer("events/ordered", jsonObject -> {
            Iterator<ServerWebSocket> iterator = this.orderedSubscribers.iterator();
            while (iterator.hasNext()){
                ServerWebSocket socket = iterator.next();
                if (socket.isClosed())
                    iterator.remove();
                else {
                    try {
                        socket.write(((JsonObject) jsonObject.body()).toBuffer());
                    } catch (Exception exception) {
                        iterator.remove();
                    }
                }
            }
        });
        eventBus.consumer("events/served", jsonObject -> {
            Iterator<ServerWebSocket> iterator = this.servedSubscribers.iterator();
            while (iterator.hasNext()){
                ServerWebSocket socket = iterator.next();
                if (socket.isClosed())
                    iterator.remove();
                else {
                    try {
                        socket.write(((JsonObject) jsonObject.body()).toBuffer());
                    } catch (Exception exception) {
                        iterator.remove();
                    }
                }
            }
        });
        eventBus.consumer("events/statusChanged", status -> {
            Iterator<ServerWebSocket> iterator = this.statusSubscribers.iterator();
            while (iterator.hasNext()){
                ServerWebSocket socket = iterator.next();
                if (socket.isClosed())
                    iterator.remove();
                else {
                    try {
                        socket.writeTextMessage((String) status.body());
                    } catch (Exception exception) {
                        iterator.remove();
                    }
                }
            }
        });
    }

    private void startServer(final Router router) {
        this.getVertx().createHttpServer().webSocketHandler(handler -> {
            if (handler.path().equals(SERVING))
                this.servingSubscribers.add(handler);
            else if (handler.path().equals(STATUS_CHANGED))
                this.statusSubscribers.add(handler);
            else if (handler.path().equals(ORDERED))
                this.orderedSubscribers.add(handler);
            else if (handler.path().equals(SERVED))
                this.servedSubscribers.add(handler);
            else
                handler.reject();
        })
        .requestHandler(router)
        .listen(this.port);
    }

    private void getName(final RoutingContext routingContext) {
       this.model.getName().onSuccess(name -> routingContext.response().end(name));
    }

    private void getStatus(final RoutingContext routingContext) {
        this.model.getStatus().onSuccess(status -> routingContext.response().end(status));
    }

    private void getResources(final RoutingContext routingContext) {
        JsonArray response = new JsonArray();
        this.model.getResources().onSuccess(products -> products.forEach(response::add));
        routingContext.response().end(response.toBuffer());
    }

    private void make(final RoutingContext routingContext) {
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

    public static void main(final String[] args) {
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new SmartCoffeeMachineService(args[1], Integer.parseInt(args[0])));
    }
}
