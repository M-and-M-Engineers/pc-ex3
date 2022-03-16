package scm;

public interface Resource extends Cloneable {

    String getName();

    int getRemaining();

    void consume();

    Resource clone();
}
