package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import core.DAG;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.util.Map;

/**
 * Request DTO for DAG execution API
 */
public class DagExecutionRequest {
    
    @JsonProperty("dag")
    @NotNull(message = "DAG cannot be null")
    @Valid
    private DAG dag;
    
    @JsonProperty("context")
    private Map<String, Object> context;
    
    @JsonProperty("dryRun")
    private boolean dryRun = false;
    
    @JsonProperty("trace")
    private boolean trace = false;
    
    @JsonProperty("async")
    private boolean async = false;
    
    @JsonProperty("timeoutMs")
    @Min(value = 0, message = "Timeout must be non-negative")
    private Long timeoutMs;
    
    @JsonProperty("validatePlugins")
    private boolean validatePlugins = true;
    
    @JsonProperty("webhookUrl")
    private String webhookUrl;
    
    @JsonProperty("executionOptions")
    private ExecutionOptions executionOptions;
    
    public DagExecutionRequest() {}
    
    public DAG getDag() {
        return dag;
    }
    
    public void setDag(DAG dag) {
        this.dag = dag;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
    
    public boolean isDryRun() {
        return dryRun;
    }
    
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
    
    public boolean isTrace() {
        return trace;
    }
    
    public void setTrace(boolean trace) {
        this.trace = trace;
    }
    
    public boolean isAsync() {
        return async;
    }
    
    public void setAsync(boolean async) {
        this.async = async;
    }
    
    public Long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    public boolean isValidatePlugins() {
        return validatePlugins;
    }
    
    public void setValidatePlugins(boolean validatePlugins) {
        this.validatePlugins = validatePlugins;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public ExecutionOptions getExecutionOptions() {
        return executionOptions;
    }
    
    public void setExecutionOptions(ExecutionOptions executionOptions) {
        this.executionOptions = executionOptions;
    }
    
    /**
     * Nested class for execution options
     */
    public static class ExecutionOptions {
        @JsonProperty("maxConcurrency")
        private Integer maxConcurrency;
        
        @JsonProperty("retryPolicy")
        private String retryPolicy = "default";
        
        @JsonProperty("failFast")
        private boolean failFast = false;
        
        @JsonProperty("debugMode")
        private boolean debugMode = false;
        
        @JsonProperty("priority")
        private String priority;
        
        public Integer getMaxConcurrency() {
            return maxConcurrency;
        }
        
        public void setMaxConcurrency(Integer maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
        }
        
        public String getRetryPolicy() {
            return retryPolicy;
        }
        
        public void setRetryPolicy(String retryPolicy) {
            this.retryPolicy = retryPolicy;
        }
        
        public boolean isFailFast() {
            return failFast;
        }
        
        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }
        
        public boolean isDebugMode() {
            return debugMode;
        }
        
        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }
        
        public String getPriority() {
            return priority;
        }
        
        public void setPriority(String priority) {
            this.priority = priority;
        }
    }
}