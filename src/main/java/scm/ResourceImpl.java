package scm;

public class ResourceImpl implements Resource {

    private final String name;
    private int remaining;

    public ResourceImpl(final String name, final int remaining) {
        this.name = name;
        this.remaining = remaining;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getRemaining() {
        return this.remaining;
    }

    @Override
    public void consume() {
        this.remaining--;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public Resource clone() {
        try {
            return (ResourceImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
