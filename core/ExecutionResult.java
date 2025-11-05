package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of executing a task or DAG
 */
public class ExecutionResult {
    @JsonProperty("success")
    private final boolean success;
    @JsonProperty("message")
    private final String message;
    @JsonProperty("data")
    private final Object data;
    @JsonProperty("error")
    private final Exception error;
    @JsonProperty("errorType")
    private final ExecutionErrorType errorType;
    
    @JsonCreator
    protected ExecutionResult(
            @JsonProperty("success") boolean success, 
            @JsonProperty("message") String message, 
            @JsonProperty("data") Object data, 
            @JsonProperty("error") Exception error, 
            @JsonProperty("errorType") ExecutionErrorType errorType) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.errorType = errorType;
    }
    
    public static ExecutionResult success(String message) {
        return new ExecutionResult(true, message, null, null, null);
    }
    
    public static ExecutionResult success(String message, Object data) {
        return new ExecutionResult(true, message, data, null, null);
    }
    
    public static ExecutionResult failure(String message) {
        return new ExecutionResult(false, message, null, null, null);
    }
    
    public static ExecutionResult failure(String message, Exception error) {
        return new ExecutionResult(false, message, null, error, ExecutionErrorType.EXECUTION_ERROR);
    }
    
    public static ExecutionResult failure(String message, Exception error, Object data) {
        return new ExecutionResult(false, message, data, error, ExecutionErrorType.EXECUTION_ERROR);
    }
    
    /**
     * Create a failure result with a specific error type
     * 
     * @param message The error message
     * @param errorType The type of error
     * @return ExecutionResult with the specified error type
     */
    public static ExecutionResult failure(String message, ExecutionErrorType errorType) {
        return new ExecutionResult(false, message, null, null, errorType);
    }
    
    /**
     * Create a failure result with a specific error type and exception
     * 
     * @param message The error message
     * @param error The exception that caused the failure
     * @param errorType The type of error
     * @return ExecutionResult with the specified error type and exception
     */
    public static ExecutionResult failure(String message, Exception error, ExecutionErrorType errorType) {
        return new ExecutionResult(false, message, null, error, errorType);
    }
    
    /**
     * Create a failure result with a specific error type, exception, and data
     * 
     * @param message The error message
     * @param error The exception that caused the failure
     * @param data Additional data related to the failure
     * @param errorType The type of error
     * @return ExecutionResult with the specified error type, exception, and data
     */
    public static ExecutionResult failure(String message, Exception error, Object data, ExecutionErrorType errorType) {
        return new ExecutionResult(false, message, data, error, errorType);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Object getData() {
        return data;
    }
    
    public Exception getError() {
        return error;
    }
    
    public ExecutionErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Check if this result has a specific error type
     * 
     * @param type The error type to check for
     * @return true if this result has the specified error type, false otherwise
     */
    public boolean hasErrorType(ExecutionErrorType type) {
        return !success && errorType == type;
    }
    
    @Override
    public String toString() {
        return "ExecutionResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                (errorType != null ? ", errorType=" + errorType : "") +
                ", data=" + data +
                ", error=" + error +
                '}';
    }
}