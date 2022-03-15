package manager;

import common.Resource;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class DashBoardService {

    private final Map<String, SmartCoffeeMachineDigitalTwin> digitalTwins;
    private final Map<SmartCoffeeMachineDigitalTwin, List<Resource>> allResources;
    private final CountDownLatch latch;

    public DashBoardService() {
        final int[] ports = new int[]{ 10000, 10001, 10002 };
        this.digitalTwins = new HashMap<>();
        this.allResources = new HashMap<>();
        this.latch = new CountDownLatch(ports.length);
        for (final int port : ports) {
            final SmartCoffeeMachineDigitalTwin digitalTwin = new SmartCoffeeMachineDigitalTwin("localhost", port);
            digitalTwin.getName().onComplete(res -> {
                if (res.succeeded())
                    this.digitalTwins.put(res.result(), digitalTwin);
                else
                    this.digitalTwins.put("scm" + port, digitalTwin);
                this.latch.countDown();
            });
            digitalTwin.getResources().onSuccess(resources -> this.allResources.put(digitalTwin, resources));
        }
    }

    public Set<String> getDigitalTwinNames() {
        try {
            this.latch.await();
            return this.digitalTwins.keySet();
        } catch (InterruptedException e) {
            return Collections.emptySet();
        }
    }

    public List<Resource> getResourcesOfDigitalTwin(final String name) {
        return this.allResources.get(this.digitalTwins.get(name));
    }

    public void subscribeToServed(final String name, final Handler<JsonObject> handler) {
        this.digitalTwins.get(name).subscribeToServed(handler);
    }

    public void subscribeToStatusChanged(final String name, final Handler<String> handler, final Handler<Void> closeHandler) {
        this.digitalTwins.get(name).subscribeToStatusChanged(handler, closeHandler);
    }

    public Future<String> getStatusOfDigitalTwin(final String name) {
        return this.digitalTwins.get(name).getStatus();
    }
}
