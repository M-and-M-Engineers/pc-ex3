package scm;

public enum State {
    WORKING, NOT_AVAILABLE, OUT_OF_SERVICE;

    @Override
    public String toString() {
        switch (this){
            case WORKING:
                return "Working";
            case NOT_AVAILABLE:
                return "Not available";
            case OUT_OF_SERVICE:
                return "Out of Service";
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
