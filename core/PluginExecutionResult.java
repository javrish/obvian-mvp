package core;

/**
 * Enhanced result for plugin execution with detailed error categorization
 */
public class PluginExecutionResult {
    
    public enum Status {
        SUCCESS,
        FAILURE,
        TIMEOUT,
        UNAVAILABLE,
        HEALTH_CHECK_FAILED
    }
    
    public enum ErrorCategory {
        PLUGIN_NOT_FOUND,
        PLUGIN_UNAVAILABLE,
        PLUGIN_TIMEOUT,
        PLUGIN_EXCEPTION,
        PLUGIN_HEALTH_FAILURE,
        CONFIGURATION_ERROR,
        RESOURCE_EXHAUSTION
    }
    
    private final Status status;
    private final ExecutionResult executionResult;
    private final ErrorCategory errorCategory;
    private final String errorMessage;
    private final Exception exception;
    private final long executionTimeMs;
    private final boolean fallbackUsed;
    private final String pluginId;
    
    private PluginExecutionResult(Status status, ExecutionResult executionResult, 
                                 ErrorCategory errorCategory, String errorMessage, 
                                 Exception exception, long executionTimeMs, 
                                 boolean fallbackUsed, String pluginId) {
        this.status = status;
        this.executionResult = executionResult;
        this.errorCategory = errorCategory;
        this.errorMessage = errorMessage;
        this.exception = exception;
        this.executionTimeMs = executionTimeMs;
        this.fallbackUsed = fallbackUsed;
        this.pluginId = pluginId;
    }
    
    public static PluginExecutionResult success(ExecutionResult result, long executionTimeMs, String pluginId) {
        return new PluginExecutionResult(Status.SUCCESS, result, null, null, null, 
                                       executionTimeMs, false, pluginId);
    }
    
    public static PluginExecutionResult successWithFallback(ExecutionResult result, long executionTimeMs, String pluginId) {
        return new PluginExecutionResult(Status.SUCCESS, result, null, null, null, 
                                       executionTimeMs, true, pluginId);
    }
    
    public static PluginExecutionResult failure(ErrorCategory category, String message, 
                                               Exception exception, long executionTimeMs, String pluginId) {
        return new PluginExecutionResult(Status.FAILURE, null, category, message, exception, 
                                       executionTimeMs, false, pluginId);
    }
    
    public static PluginExecutionResult timeout(String message, long executionTimeMs, String pluginId) {
        return new PluginExecutionResult(Status.TIMEOUT, null, ErrorCategory.PLUGIN_TIMEOUT, 
                                       message, null, executionTimeMs, false, pluginId);
    }
    
    public static PluginExecutionResult unavailable(String message, String pluginId) {
        return new PluginExecutionResult(Status.UNAVAILABLE, null, ErrorCategory.PLUGIN_UNAVAILABLE, 
                                       message, null, 0, false, pluginId);
    }
    
    public static PluginExecutionResult healthCheckFailed(String message, String pluginId) {
        return new PluginExecutionResult(Status.HEALTH_CHECK_FAILED, null, ErrorCategory.PLUGIN_HEALTH_FAILURE, 
                                       message, null, 0, false, pluginId);
    }
    
    public Status getStatus() { return status; }
    public ExecutionResult getExecutionResult() { return executionResult; }
    public ErrorCategory getErrorCategory() { return errorCategory; }
    public String getErrorMessage() { return errorMessage; }
    public Exception getException() { return exception; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public String getPluginId() { return pluginId; }
    
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isFailure() { return status != Status.SUCCESS; }
    
    /**
     * Get a detailed error message for logging and debugging
     */
    public String getDetailedErrorMessage() {
        if (status == Status.SUCCESS) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Plugin execution failed - ");
        sb.append("Plugin: ").append(pluginId);
        sb.append(", Status: ").append(status);
        
        if (errorCategory != null) {
            sb.append(", Category: ").append(errorCategory);
        }
        
        if (errorMessage != null) {
            sb.append(", Message: ").append(errorMessage);
        }
        
        if (executionTimeMs > 0) {
            sb.append(", Duration: ").append(executionTimeMs).append("ms");
        }
        
        if (fallbackUsed) {
            sb.append(", Fallback used: true");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "PluginExecutionResult{" +
                "status=" + status +
                ", pluginId='" + pluginId + '\'' +
                ", errorCategory=" + errorCategory +
                ", executionTimeMs=" + executionTimeMs +
                ", fallbackUsed=" + fallbackUsed +
                '}';
    }
}