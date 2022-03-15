package scm;

import common.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SmartCoffeeMachineModel implements SmartCoffeeMachineAPI {

    private static final List<String> ALL_PRODUCTS = new ArrayList<>(Arrays.asList(
            "Caffè corto",
            "Caffè lungo",
            "Caffè espresso",
            "Caffè ginseng",
            "Caffè macchiato",
            "Cappuccino espresso",
            "Cappuccino ginseng",
            "Cioccolata",
            "Thè nero",
            "Thè verde"));
    private static final PrimitiveIterator.OfInt QUANTITY_STREAM = IntStream.generate(() -> new Random().nextInt(5)).iterator();
    private static final int INITIAL_SUGAR_AMOUNT = 100;
    private static final int INITIAL_GLASSES_AMOUNT = 50;
    private static final int MINIMUM_PRODUCT_AMOUNT = 1;
    private final String name;
    private final EventBus eventBus;
    private final List<Resource> resources;
    private final List<Resource> notConsumed;
    private final ExecutorService scheduler;
    private final Sugar sugar;
    private final Glasses glasses;
    private final Sugar sugarCopy;
    private final Glasses glassesCopy;
    private int queueNumber;
    private int ticketNumber;
    private String status;

    public SmartCoffeeMachineModel(final String name, EventBus eventBus) {
        this.name = name;
        this.eventBus = eventBus;
        this.resources = new ArrayList<>();
        this.scheduler = Executors.newSingleThreadExecutor();

        Collections.shuffle(ALL_PRODUCTS);
        this.resources.addAll(ALL_PRODUCTS.stream()
                .limit(5)
                .map(product -> new ResourceImpl(product, QUANTITY_STREAM.next() + MINIMUM_PRODUCT_AMOUNT))
                .collect(Collectors.toList()));
        this.sugar = new Sugar(INITIAL_SUGAR_AMOUNT);
        this.glasses = new Glasses(INITIAL_GLASSES_AMOUNT);

        this.notConsumed = new ArrayList<>();
        this.resources.forEach(r -> this.notConsumed.add(r.clone()));
        this.sugarCopy = new Sugar(INITIAL_SUGAR_AMOUNT);
        this.glassesCopy = new Glasses(INITIAL_GLASSES_AMOUNT);

        this.queueNumber = 0;
        this.ticketNumber = 1;
        this.status = "Working";
        this.publishStatus();
    }

    @Override
    public Future<String> getName() {
        final Promise<String> promise = Promise.promise();
        synchronized (this) {
            promise.complete(this.name);
        }
        return promise.future();
    }

    @Override
    public Future<String> getStatus() {
        final Promise<String> promise = Promise.promise();
        synchronized (this) {
            promise.complete(this.status);
        }
        return promise.future();
    }

    @Override
    public Future<List<Resource>> getResources() {
        final Promise<List<Resource>> promise = Promise.promise();
        synchronized (this) {
            final List<Resource> resources = new ArrayList<>(this.resources);
            resources.add(this.sugar);
            resources.add(this.glasses);
            promise.complete(resources);
        }
        return promise.future();
    }

    @Override
    public Future<Integer> make(String product, Integer sugarLevel) {
        final Promise<Integer> promise = Promise.promise();

        final Optional<Resource> res = this.notConsumed.stream().filter(r -> r.getName().equals(product)).findFirst();
        if (res.isPresent()) {
            final Resource resourceCopy = res.get();
            if (resourceCopy.getRemaining() > 0 && this.glassesCopy.getRemaining() > 0 && this.sugarCopy.getRemaining() >= sugarLevel) {

                // CONSUMO COPIE
                for (int i = 0; i < sugarLevel; i++) {
                    this.sugarCopy.consume();
                }
                this.glassesCopy.consume();

                resourceCopy.consume();
                this.publishOrdered(resourceCopy);

                promise.complete(this.ticketNumber++);

                this.scheduler.submit(() -> {
                    try {
                        this.queueNumber++;
                        this.publishServing();

                        Thread.sleep(5000);

                        // CONSUMO EFFETTIVO
                        for (int i = 0; i < sugarLevel; i++) {
                            this.sugar.consume();
                        }
                        this.glasses.consume();

                        final Resource resource = this.resources.stream().filter(r -> r.getName().equals(product)).findFirst().orElseThrow();
                        resource.consume();

                        this.publishServed(resource);

                        if (this.glasses.getRemaining() == 0 || this.resources.stream().allMatch(r -> r.getRemaining() == 0)) {
                            this.status = "Not available";
                            this.publishStatus();
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            } else
                promise.fail("Not anymore.");
        } else
            promise.fail("Product does NOT exist.");
        return promise.future();
    }

    private void publishServing() {
        final JsonObject json = new JsonObject();
        json.put("queueNumber", this.queueNumber);
        this.eventBus.publish("events/serving", json);
    }

    private void publishOrdered(final Resource product) {
        final JsonObject json = new JsonObject();
        json.put("product", product.getName());
        json.put("remaining", product.getRemaining());
        json.put("sugarRemaining", this.sugarCopy.getRemaining());
        json.put("glassesRemaining", this.glassesCopy.getRemaining());
        this.eventBus.publish("events/ordered", json);
    }

    private void publishServed(final Resource product) {
        final JsonObject json = new JsonObject();
        json.put("product", product.getName());
        json.put("remaining", product.getRemaining());
        json.put("sugarRemaining", this.sugar.getRemaining());
        json.put("glassesRemaining", this.glasses.getRemaining());
        this.eventBus.publish("events/served", json);
    }

    private void publishStatus() {
        this.eventBus.publish("events/statusChanged", this.status);
    }

}
