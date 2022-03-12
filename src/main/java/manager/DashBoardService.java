package manager;

import common.Resource;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.*;

public class DashBoardService {

    private final Map<String, SmartCoffeeMachineDigitalTwin> digitalTwins;
    private final Map<SmartCoffeeMachineDigitalTwin, List<Resource>> allResources;

    public DashBoardService() {
        final int[] proxiesPorts = new int[]{ 10000, 10001, 10002 };
        this.digitalTwins = new HashMap<>();
        this.allResources = new HashMap<>();
        Arrays.stream(proxiesPorts).forEach(port -> {
            SmartCoffeeMachineDigitalTwin digitalTwin = new SmartCoffeeMachineDigitalTwin("localhost", port);
            this.digitalTwins.put(String.valueOf(port), digitalTwin);
        });
        this.digitalTwins.values().forEach(scm -> scm.getResources().onSuccess(resources -> this.allResources.put(scm, resources)));
    }

    public Set<String> getDigitalTwinNames() {
        return this.digitalTwins.keySet();
    }

    public List<Resource> getResourcesOfDigitalTwin(final String name) {
        return this.allResources.get(this.digitalTwins.get(name));
    }

    public void subscribeToRemaining(final String name, final Handler<JsonObject> handler) {
        this.digitalTwins.get(name).subscribeToRemaining(handler);
    }
}
