package app.external.llm;

public class LlmException extends RuntimeException {
    private final int statusCode;
    private final boolean timeout;

    public LlmException(String message) {
        this(message, null, 0, false);
    }

    public LlmException(String message, Throwable cause) {
        this(message, cause, 0, false);
    }

    public LlmException(String message, int statusCode) {
        this(message, null, statusCode, false);
    }

    public LlmException(String message, Throwable cause, int statusCode, boolean timeout) {
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
