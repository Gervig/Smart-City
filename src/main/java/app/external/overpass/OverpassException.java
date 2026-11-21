package app.external.overpass;

public class OverpassException extends RuntimeException {
    private final int statusCode;
    private final boolean timeout;

    public OverpassException(String message) {
        this(message, null, 0, false);
    }

    public OverpassException(String message, Throwable cause) {
        this(message, cause, 0, false);
    }

    public OverpassException(String message, int statusCode) {
        this(message, null, statusCode, false);
    }

    public OverpassException(String message, Throwable cause, int statusCode, boolean timeout) {
        super(message, cause);
        this.statusCode = statusCode;
        this.timeout = timeout;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
