package scm;

import io.vertx.core.Future;

import java.util.List;

public interface SmartCoffeeMachineAPI {

    Future<String> getName();

    Future<String> getState();

    Future<List<Resource>> getResources();

    Future<Integer> make(String product, Integer sugarLevel);
}
