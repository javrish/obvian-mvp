package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import core.DagExecutionResult;
import core.ExecutionResult;
import core.NodeExecutionResult;
import core.DagValidationResult;
import core.ValidationError;
import core.ValidationWarning;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for DAG execution API
 */
public class DagExecutionResponse {
    
    @JsonProperty("executionId")
    private String executionId;
    
    @JsonProperty("success")
    private boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("async")
    private boolean async;
    
    @JsonProperty("dryRun")
    private boolean dryRun;
    
    @JsonProperty("trace")
    private Object trace;
    
    @JsonProperty("nodeResults")
    private Map<String, NodeExecutionResult> nodeResults;
    
    @JsonProperty("validationResult")
    private ValidationResult validationResult;
    
    @JsonProperty("executionStats")
    private ExecutionStats executionStats;
    
    @JsonProperty("errorType")
    private String errorType;
    
    @JsonProperty("statusUrl")
    private String statusUrl;
    
    @JsonProperty("dagStructure")
    private Object dagStructure;
    
    public DagExecutionResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public static DagExecutionResponse success(String executionId, String message, Object data) {
        DagExecutionResponse response = new DagExecutionResponse();
        response.setExecutionId(executionId);
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
    
    public static DagExecutionResponse failure(String executionId, String message, String errorType) {
        DagExecutionResponse response = new DagExecutionResponse();
        response.setExecutionId(executionId);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorType(errorType);
        return response;
    }
    
    public static DagExecutionResponse validationFailure(String executionId, DagValidationResult validationResult) {
        DagExecutionResponse response = new DagExecutionResponse();
        response.setExecutionId(executionId);
        response.setSuccess(false);
        response.setMessage("DAG validation failed");
        response.setErrorType("VALIDATION_ERROR");
        response.setValidationResult(ValidationResult.fromDagValidationResult(validationResult));
        return response;
    }
    
    public static DagExecutionResponse dryRun(String executionId, Object dagStructure, DagValidationResult validationResult) {
        DagExecutionResponse response = new DagExecutionResponse();
        response.setExecutionId(executionId);
        response.setSuccess(validationResult.isValid());
        response.setMessage(validationResult.isValid() ? "Dry run validation successful" : "Dry run validation failed");
        response.setDryRun(true);
        response.setDagStructure(dagStructure);
        response.setValidationResult(ValidationResult.fromDagValidationResult(validationResult));
        if (!validationResult.isValid()) {
            response.setErrorType("VALIDATION_ERROR");
        }
        return response;
    }
    
    public static DagExecutionResponse async(String executionId, String statusUrl) {
        DagExecutionResponse response = new DagExecutionResponse();
        response.setExecutionId(executionId);
        response.setSuccess(true);
        response.setMessage("DAG execution started asynchronously");
        response.setAsync(true);
        response.setStatusUrl(statusUrl);
        return response;
    }
    
    public static DagExecutionResponse fromExecutionResult(String executionId, ExecutionResult result, Object trace) {
        DagExecutionResponse response = new DagExecutionResponse();
        response.setExecutionId(executionId);
        response.setSuccess(result.isSuccess());
        response.setMessage(result.getMessage());
        response.setData(result.getData());
        response.setTrace(trace);
        
        if (result.getErrorType() != null) {
            response.setErrorType(result.getErrorType().toString());
        }
        
        if (result instanceof DagExecutionResult) {
            DagExecutionResult dagResult = (DagExecutionResult) result;
            response.setNodeResults(dagResult.getNodeResults());
            
            // Create execution stats
            ExecutionStats stats = new ExecutionStats();
            stats.setTotalNodes(dagResult.getNodeResults().size());
            stats.setSuccessfulNodes((int) dagResult.getNodeResults().values().stream()
                .filter(NodeExecutionResult::isSuccess).count());
            stats.setFailedNodes((int) dagResult.getNodeResults().values().stream()
                .filter(nodeResult -> !nodeResult.isSuccess() && 
                    nodeResult.getStatus() != NodeExecutionResult.ExecutionStatus.SKIPPED).count());
            stats.setSkippedNodes((int) dagResult.getNodeResults().values().stream()
                .filter(nodeResult -> nodeResult.getStatus() == NodeExecutionResult.ExecutionStatus.SKIPPED).count());
            
            // Calculate execution time
            long minStartTime = dagResult.getNodeResults().values().stream()
                .mapToLong(NodeExecutionResult::getStartTime)
                .min().orElse(System.currentTimeMillis());
            long maxEndTime = dagResult.getNodeResults().values().stream()
                .mapToLong(NodeExecutionResult::getEndTime)
                .max().orElse(System.currentTimeMillis());
            stats.setExecutionTimeMs(maxEndTime - minStartTime);
            
            response.setExecutionStats(stats);
        }
        
        return response;
    }
    
    // Getters and setters
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isAsync() {
        return async;
    }
    
    public void setAsync(boolean async) {
        this.async = async;
    }
    
    public boolean isDryRun() {
        return dryRun;
    }
    
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
    
    public Object getTrace() {
        return trace;
    }
    
    public void setTrace(Object trace) {
        this.trace = trace;
    }
    
    public Map<String, NodeExecutionResult> getNodeResults() {
        return nodeResults;
    }
    
    public void setNodeResults(Map<String, NodeExecutionResult> nodeResults) {
        this.nodeResults = nodeResults;
    }
    
    public ValidationResult getValidationResult() {
        return validationResult;
    }
    
    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }
    
    public ExecutionStats getExecutionStats() {
        return executionStats;
    }
    
    public void setExecutionStats(ExecutionStats executionStats) {
        this.executionStats = executionStats;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
    
    public String getStatusUrl() {
        return statusUrl;
    }
    
    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
    }
    
    public Object getDagStructure() {
        return dagStructure;
    }
    
    public void setDagStructure(Object dagStructure) {
        this.dagStructure = dagStructure;
    }
    
    /**
     * Validation result wrapper for API response
     */
    public static class ValidationResult {
        @JsonProperty("valid")
        private boolean valid;
        
        @JsonProperty("errors")
        private List<ValidationError> errors;
        
        @JsonProperty("warnings")
        private List<ValidationWarning> warnings;
        
        @JsonProperty("suggestions")
        private List<String> suggestions;
        
        public static ValidationResult fromDagValidationResult(DagValidationResult dagResult) {
            ValidationResult result = new ValidationResult();
            result.setValid(dagResult.isValid());
            result.setErrors(dagResult.getErrors());
            result.setWarnings(dagResult.getWarnings());
            
            // Generate suggestions based on errors
            result.setSuggestions(generateSuggestions(dagResult.getErrors()));
            
            return result;
        }
        
        private static List<String> generateSuggestions(List<ValidationError> errors) {
            return errors.stream()
                .map(ValidationError::getSuggestion)
                .filter(suggestion -> suggestion != null && !suggestion.isEmpty())
                .distinct()
                .toList();
        }
        
        // Getters and setters
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public List<ValidationError> getErrors() {
            return errors;
        }
        
        public void setErrors(List<ValidationError> errors) {
            this.errors = errors;
        }
        
        public List<ValidationWarning> getWarnings() {
            return warnings;
        }
        
        public void setWarnings(List<ValidationWarning> warnings) {
            this.warnings = warnings;
        }
        
        public List<String> getSuggestions() {
            return suggestions;
        }
        
        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }
    }
    
    /**
     * Execution statistics for API response
     */
    public static class ExecutionStats {
        @JsonProperty("totalNodes")
        private int totalNodes;
        
        @JsonProperty("successfulNodes")
        private int successfulNodes;
        
        @JsonProperty("failedNodes")
        private int failedNodes;
        
        @JsonProperty("skippedNodes")
        private int skippedNodes;
        
        @JsonProperty("executionTimeMs")
        private long executionTimeMs;
        
        // Getters and setters
        public int getTotalNodes() {
            return totalNodes;
        }
        
        public void setTotalNodes(int totalNodes) {
            this.totalNodes = totalNodes;
        }
        
        public int getSuccessfulNodes() {
            return successfulNodes;
        }
        
        public void setSuccessfulNodes(int successfulNodes) {
            this.successfulNodes = successfulNodes;
        }
        
        public int getFailedNodes() {
            return failedNodes;
        }
        
        public void setFailedNodes(int failedNodes) {
            this.failedNodes = failedNodes;
        }
        
        public int getSkippedNodes() {
            return skippedNodes;
        }
        
        public void setSkippedNodes(int skippedNodes) {
            this.skippedNodes = skippedNodes;
        }
        
        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
        
        public void setExecutionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }
    }
}