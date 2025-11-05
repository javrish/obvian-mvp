package plugins;

import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

/**
 * Represents the result of plugin execution.
 */
public class PluginResult {

    public enum Status {
        SUCCESS,
        FAILURE,
        PARTIAL_SUCCESS,
        TIMEOUT
    }

    private final Status status;
    private final Map<String, Object> output;
    private final String message;
    private final Exception error;
    private final LocalDateTime timestamp;
    private final long executionTimeMs;

    public PluginResult(Status status, Map<String, Object> output, String message, Exception error, long executionTimeMs) {
        this.status = status;
        this.output = output != null ? new HashMap<>(output) : new HashMap<>();
        this.message = message;
        this.error = error;
        this.timestamp = LocalDateTime.now();
        this.executionTimeMs = executionTimeMs;
    }

    // Factory methods
    public static PluginResult success(Map<String, Object> output) {
        return new PluginResult(Status.SUCCESS, output, "Execution completed successfully", null, 0);
    }

    public static PluginResult success(Map<String, Object> output, String message) {
        return new PluginResult(Status.SUCCESS, output, message, null, 0);
    }

    public static PluginResult failure(String message) {
        return new PluginResult(Status.FAILURE, null, message, null, 0);
    }

    public static PluginResult failure(String message, Exception error) {
        return new PluginResult(Status.FAILURE, null, message, error, 0);
    }

    // Getters
    public Status getStatus() {
        return status;
    }

    public Map<String, Object> getOutput() {
        return new HashMap<>(output);
    }

    public String getMessage() {
        return message;
    }

    public Exception getError() {
        return error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }
}