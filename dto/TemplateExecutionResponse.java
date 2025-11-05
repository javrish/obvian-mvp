package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for template execution operations.
 * Contains execution results, status, and performance metrics.
 * 
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of template execution")
public class TemplateExecutionResponse {

    @Schema(description = "Unique execution identifier", example = "exec-123e4567-e89b-12d3-a456-426614174000")
    private String executionId;

    @Schema(description = "Template ID that was executed", example = "daily-standup-report")
    private String templateId;

    @Schema(description = "Whether execution was successful")
    private boolean success;

    @Schema(description = "Execution status")
    private ExecutionStatus status;

    @Schema(description = "Execution result message")
    private String message;

    @Schema(description = "Execution start timestamp")
    private Instant startedAt;

    @Schema(description = "Execution completion timestamp")
    private Instant completedAt;

    @Schema(description = "Total execution time in milliseconds")
    private long executionTimeMs;

    @Schema(description = "Whether this was a dry-run execution")
    private boolean dryRun;

    @Schema(description = "Whether this was an async execution")
    private boolean async;

    @Schema(description = "URL to check execution status (for async executions)")
    private String statusUrl;

    @Schema(description = "Generated DAG structure")
    private Object dagStructure;

    @Schema(description = "Execution trace information")
    private List<TraceEntry> trace;

    @Schema(description = "Results from individual nodes")
    private Map<String, NodeExecutionResult> nodeResults;

    @Schema(description = "Overall execution statistics")
    private ExecutionStats executionStats;

    @Schema(description = "Validation result if validation was performed")
    private ValidationResult validationResult;

    @Schema(description = "Error information if execution failed")
    private ErrorInfo errorInfo;

    @Schema(description = "Output data produced by execution")
    private Map<String, Object> outputData;

    @Schema(description = "Execution metadata")
    private Map<String, Object> metadata;

    @Schema(description = "Performance metrics")
    private PerformanceMetrics performanceMetrics;

    // Constructors
    public TemplateExecutionResponse() {}

    public TemplateExecutionResponse(String executionId, String templateId, boolean success) {
        this.executionId = executionId;
        this.templateId = templateId;
        this.success = success;
        this.status = success ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED;
    }

    // Getters and Setters
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public boolean isAsync() { return async; }
    public void setAsync(boolean async) { this.async = async; }

    public String getStatusUrl() { return statusUrl; }
    public void setStatusUrl(String statusUrl) { this.statusUrl = statusUrl; }

    public Object getDagStructure() { return dagStructure; }
    public void setDagStructure(Object dagStructure) { this.dagStructure = dagStructure; }

    public List<TraceEntry> getTrace() { return trace; }
    public void setTrace(List<TraceEntry> trace) { this.trace = trace; }

    public Map<String, NodeExecutionResult> getNodeResults() { return nodeResults; }
    public void setNodeResults(Map<String, NodeExecutionResult> nodeResults) { this.nodeResults = nodeResults; }

    public ExecutionStats getExecutionStats() { return executionStats; }
    public void setExecutionStats(ExecutionStats executionStats) { this.executionStats = executionStats; }

    public ValidationResult getValidationResult() { return validationResult; }
    public void setValidationResult(ValidationResult validationResult) { this.validationResult = validationResult; }

    public ErrorInfo getErrorInfo() { return errorInfo; }
    public void setErrorInfo(ErrorInfo errorInfo) { this.errorInfo = errorInfo; }

    public Map<String, Object> getOutputData() { return outputData; }
    public void setOutputData(Map<String, Object> outputData) { this.outputData = outputData; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) { this.performanceMetrics = performanceMetrics; }

    // Nested classes

    @Schema(description = "Execution status enumeration")
    public enum ExecutionStatus {
        @Schema(description = "Execution is queued and waiting to start")
        QUEUED,
        @Schema(description = "Execution is currently running")
        RUNNING,
        @Schema(description = "Execution completed successfully")
        COMPLETED,
        @Schema(description = "Execution failed with errors")
        FAILED,
        @Schema(description = "Execution was cancelled")
        CANCELLED,
        @Schema(description = "Execution timed out")
        TIMEOUT
    }

    @Schema(description = "Trace entry for execution debugging")
    public static class TraceEntry {
        @Schema(description = "Trace entry timestamp")
        private Instant timestamp;

        @Schema(description = "Trace level")
        private String level;

        @Schema(description = "Node ID associated with this trace")
        private String nodeId;

        @Schema(description = "Trace message")
        private String message;

        @Schema(description = "Additional trace data")
        private Map<String, Object> data;

        // Constructors, getters, and setters
        public TraceEntry() {}

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    @Schema(description = "Result from a single node execution")
    public static class NodeExecutionResult {
        @Schema(description = "Node identifier")
        private String nodeId;

        @Schema(description = "Whether node execution was successful")
        private boolean success;

        @Schema(description = "Node execution status")
        private String status;

        @Schema(description = "Node execution time in milliseconds")
        private long executionTimeMs;

        @Schema(description = "Output produced by the node")
        private Map<String, Object> output;

        @Schema(description = "Error message if node failed")
        private String errorMessage;

        @Schema(description = "Number of retry attempts")
        private int retryAttempts;

        // Constructors, getters, and setters
        public NodeExecutionResult() {}

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

        public Map<String, Object> getOutput() { return output; }
        public void setOutput(Map<String, Object> output) { this.output = output; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
    }

    @Schema(description = "Overall execution statistics")
    public static class ExecutionStats {
        @Schema(description = "Total number of nodes in template")
        private int totalNodes;

        @Schema(description = "Number of successfully executed nodes")
        private int successfulNodes;

        @Schema(description = "Number of failed nodes")
        private int failedNodes;

        @Schema(description = "Number of skipped nodes")
        private int skippedNodes;

        @Schema(description = "Average node execution time in milliseconds")
        private double avgNodeExecutionTimeMs;

        @Schema(description = "Maximum node execution time in milliseconds")
        private long maxNodeExecutionTimeMs;

        @Schema(description = "Minimum node execution time in milliseconds")
        private long minNodeExecutionTimeMs;

        // Constructors, getters, and setters
        public ExecutionStats() {}

        public int getTotalNodes() { return totalNodes; }
        public void setTotalNodes(int totalNodes) { this.totalNodes = totalNodes; }

        public int getSuccessfulNodes() { return successfulNodes; }
        public void setSuccessfulNodes(int successfulNodes) { this.successfulNodes = successfulNodes; }

        public int getFailedNodes() { return failedNodes; }
        public void setFailedNodes(int failedNodes) { this.failedNodes = failedNodes; }

        public int getSkippedNodes() { return skippedNodes; }
        public void setSkippedNodes(int skippedNodes) { this.skippedNodes = skippedNodes; }

        public double getAvgNodeExecutionTimeMs() { return avgNodeExecutionTimeMs; }
        public void setAvgNodeExecutionTimeMs(double avgNodeExecutionTimeMs) { this.avgNodeExecutionTimeMs = avgNodeExecutionTimeMs; }

        public long getMaxNodeExecutionTimeMs() { return maxNodeExecutionTimeMs; }
        public void setMaxNodeExecutionTimeMs(long maxNodeExecutionTimeMs) { this.maxNodeExecutionTimeMs = maxNodeExecutionTimeMs; }

        public long getMinNodeExecutionTimeMs() { return minNodeExecutionTimeMs; }
        public void setMinNodeExecutionTimeMs(long minNodeExecutionTimeMs) { this.minNodeExecutionTimeMs = minNodeExecutionTimeMs; }
    }

    @Schema(description = "Validation result")
    public static class ValidationResult {
        @Schema(description = "Whether validation passed")
        private boolean valid;

        @Schema(description = "Validation errors")
        private List<String> errors;

        @Schema(description = "Validation warnings")
        private List<String> warnings;

        @Schema(description = "Validation suggestions")
        private List<String> suggestions;

        // Constructors, getters, and setters
        public ValidationResult() {}

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }

        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }

        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }

    @Schema(description = "Error information")
    public static class ErrorInfo {
        @Schema(description = "Error type")
        private String errorType;

        @Schema(description = "Error code")
        private String errorCode;

        @Schema(description = "Detailed error message")
        private String message;

        @Schema(description = "Node where error occurred")
        private String failedNodeId;

        @Schema(description = "Stack trace (in debug mode)")
        private String stackTrace;

        @Schema(description = "Suggested remediation")
        private String remediation;

        // Constructors, getters, and setters
        public ErrorInfo() {}

        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }

        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getFailedNodeId() { return failedNodeId; }
        public void setFailedNodeId(String failedNodeId) { this.failedNodeId = failedNodeId; }

        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }

        public String getRemediation() { return remediation; }
        public void setRemediation(String remediation) { this.remediation = remediation; }
    }

    @Schema(description = "Performance metrics")
    public static class PerformanceMetrics {
        @Schema(description = "CPU usage percentage")
        private double cpuUsagePercent;

        @Schema(description = "Memory usage in MB")
        private long memoryUsageMb;

        @Schema(description = "Peak memory usage in MB")
        private long peakMemoryUsageMb;

        @Schema(description = "Disk I/O operations")
        private long diskIOOperations;

        @Schema(description = "Network requests made")
        private long networkRequests;

        @Schema(description = "Cache hit ratio")
        private double cacheHitRatio;

        // Constructors, getters, and setters
        public PerformanceMetrics() {}

        public double getCpuUsagePercent() { return cpuUsagePercent; }
        public void setCpuUsagePercent(double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }

        public long getMemoryUsageMb() { return memoryUsageMb; }
        public void setMemoryUsageMb(long memoryUsageMb) { this.memoryUsageMb = memoryUsageMb; }

        public long getPeakMemoryUsageMb() { return peakMemoryUsageMb; }
        public void setPeakMemoryUsageMb(long peakMemoryUsageMb) { this.peakMemoryUsageMb = peakMemoryUsageMb; }

        public long getDiskIOOperations() { return diskIOOperations; }
        public void setDiskIOOperations(long diskIOOperations) { this.diskIOOperations = diskIOOperations; }

        public long getNetworkRequests() { return networkRequests; }
        public void setNetworkRequests(long networkRequests) { this.networkRequests = networkRequests; }

        public double getCacheHitRatio() { return cacheHitRatio; }
        public void setCacheHitRatio(double cacheHitRatio) { this.cacheHitRatio = cacheHitRatio; }
    }
}