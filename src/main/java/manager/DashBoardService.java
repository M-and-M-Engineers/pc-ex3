package manager;

import scm.Resource;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import scm.State;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class DashBoardService {

    private final Map<Integer, SmartCoffeeMachineDigitalTwin> digitalTwins;
    private final Map<Integer, String> names;
    private final Map<Integer, String> states;
    private final Map<Integer, List<Resource>> allResources;
    private final CountDownLatch namesLatch;
    private final CountDownLatch stateLatch;
    private final CountDownLatch resourcesLatch;
    private boolean notificationsActive;

    public DashBoardService() {
        final int[] ports = new int[]{ 10000, 10001, 10002 };
        this.digitalTwins = new HashMap<>();
        this.names = new HashMap<>();
        this.states = new HashMap<>();
        this.allResources = new HashMap<>();
        this.namesLatch = new CountDownLatch(ports.length);
        this.stateLatch = new CountDownLatch(ports.length);
        this.resourcesLatch = new CountDownLatch(ports.length);
        
        for (final int port : ports) {
            this.digitalTwins.put(port, new SmartCoffeeMachineDigitalTwin("localhost", port));
            this.getProperties(port);
        }
    }

    private void getProperties(int port) {
        final SmartCoffeeMachineDigitalTwin digitalTwin = this.digitalTwins.get(port);
        digitalTwin.getName().onComplete(res -> {
            if (res.succeeded())
                this.names.put(port, res.result());
            else
                this.names.put(port, "scm" + port);
            this.namesLatch.countDown();
        });
        digitalTwin.getState().onComplete(res -> {
            if (res.succeeded())
                this.states.put(port, res.result());
            else
                this.states.put(port, State.OUT_OF_SERVICE.toString());
            this.stateLatch.countDown();
        });
        digitalTwin.getResources().onComplete(res -> {
            if (res.succeeded())
                this.allResources.put(port, res.result());
            else
                this.allResources.put(port, Collections.emptyList());

            this.resourcesLatch.countDown();
        });
    }

    public boolean areNotificationsActive() {
        return this.notificationsActive;
    }

    public void activateNotifications() {
        this.notificationsActive = true;
    }
    
    public Map<Integer, String> getNames() {
        try {
            this.namesLatch.await();
            return new HashMap<>(this.names);
        } catch (InterruptedException e) {
            return Collections.emptyMap();
        }
    }

    public String getStates(final int port) {
        try {
            this.stateLatch.await();
            return this.states.get(port);
        } catch (InterruptedException e) {
            return State.OUT_OF_SERVICE.toString();
        }
    }

    public List<Resource> getResources(final int port) {
        try {
            this.resourcesLatch.await();
            return this.allResources.get(port);
        } catch (InterruptedException e) {
            return Collections.emptyList();
        }
    }

    public void subscribeToServed(final int port, final Handler<JsonObject> handler) {
        this.digitalTwins.get(port).subscribeToServed(handler);
    }

    public void subscribeToStateChanged(final int port, final Handler<String> handler, final Handler<Void> closeHandler) {
        this.digitalTwins.get(port).subscribeToStateChanged(handler, closeHandler);
    }

}
