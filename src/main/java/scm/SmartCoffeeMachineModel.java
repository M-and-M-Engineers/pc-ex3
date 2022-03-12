package scm;

import common.*;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SmartCoffeeMachineModel implements SmartCoffeeMachineAPI {

    private static final List<String> ALL_PRODUCTS = new ArrayList<>(Arrays.asList("caffè corto", "caffè lungo", "caffè espresso", "caffè ginseng", "caffè macchiato", "cappuccino espresso", "cappuccino ginseng", "cioccolata", "thè nero", "thè verde"));
    private static final PrimitiveIterator.OfInt QUANTITY_STREAM = IntStream.generate(() -> new Random().nextInt(10)).iterator();
    private static final int INITIAL_SUGAR_AMOUNT = 100;
    private static final int INITIAL_GLASSES_AMOUNT = 50;
    private static final String OPERATIONAL_STATUS = "working";
    private static final int MINIMUM_SUGAR_AMOUNT = 30;
    private static final int MINIMUM_GLASSES_AMOUNT = 15;
    private final List<Resource> resources = new ArrayList<>();
    private final ExecutorService scheduler;
    private final Vertx vertx;
    private final Sugar sugar;
    private final Glasses glasses;
    private String status;
    private int queueNumber;
    private int ticketNumber = 1;

    public SmartCoffeeMachineModel(Vertx vertx) {
        this.vertx = vertx;
        this.scheduler = Executors.newSingleThreadExecutor();
        this.status = OPERATIONAL_STATUS;
        Collections.shuffle(ALL_PRODUCTS);
        this.resources.addAll(ALL_PRODUCTS.stream()
                .limit(5)
                .map(product -> new ResourceImpl(product, QUANTITY_STREAM.next()))
                .collect(Collectors.toList()));
        this.sugar = new Sugar(INITIAL_SUGAR_AMOUNT);
        this.glasses = new Glasses(INITIAL_GLASSES_AMOUNT);
        this.resources.add(this.sugar);
        this.resources.add(this.glasses);
    }

    @Override
    public Future<String> getStatus() {
        Promise<String> promise = Promise.promise();
        synchronized (this) {
            promise.complete(this.status);
        }
        return promise.future();
    }

    @Override
    public Future<List<Resource>> getResources() {
        Promise<List<Resource>> promise = Promise.promise();
        synchronized (this) {
            promise.complete(this.resources);
        }
        return promise.future();
    }

    @Override
    public Future<Integer> make(String product, Integer sugarLevel) {
        Promise<Integer> promise = Promise.promise();
        Resource resource = this.resources.stream().filter(r -> r.getName().equals(product)).findFirst().get();

        if (resource.getRemaining() > 0) {
            promise.complete(this.ticketNumber++);
            scheduler.submit(() -> {
                try {
                    this.queueNumber++;
                    this.publishServing();

                    Thread.sleep(5000);
                    for (int i = 0; i < sugarLevel; i++) {
                        this.sugar.consume();
                    }
                    this.glasses.consume();

                    resource.consume();
                    this.publishRemaining(resource);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } else
            promise.fail("Not anymore.");
        return promise.future();
    }


    @Override
    public Future<Void> subscribeToServing(Handler<JsonObject> handler) {
        Promise<Void> promise = Promise.promise();
        vertx.eventBus().consumer("events/serving", ev -> handler.handle((JsonObject) ev.body()));
        promise.complete();
        return promise.future();
    }

    @Override
    public Future<Void> subscribeToRemaining(Handler<JsonObject> handler) {
        Promise<Void> promise = Promise.promise();
        vertx.eventBus().consumer("events/remaining", ev -> handler.handle((JsonObject) ev.body()));
        promise.complete();
        return promise.future();
    }

    @Override
    public Future<Void> subscribeToStatusChanged(Handler<String> handler, Handler<Void> closeHandler) {
        Promise<Void> promise = Promise.promise();
        vertx.eventBus().consumer("events/statusChanged", ev -> handler.handle(ev.body().toString()));
        promise.complete();
        return promise.future();
    }

    public Vertx getVertx() {
        return vertx;
    }

    private void publishServing() {
        JsonObject json = new JsonObject();
        json.put("queueNumber", this.queueNumber);
        this.vertx.eventBus().publish("events/serving", json);
    }

    private void publishRemaining(final Resource product) {
        JsonObject json = new JsonObject();
        json.put("product", product.getName());
        json.put("remaining", product.getRemaining());
        json.put("sugarRemaining", this.sugar.getRemaining());
        json.put("glassesRemaining", this.glasses.getRemaining());
        this.vertx.eventBus().publish("events/remaining", json);
    }

    private void publishStatus() {
        this.vertx.eventBus().publish("events/statusChanged", this.status);
    }

}
