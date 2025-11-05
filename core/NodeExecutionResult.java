package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Represents the result of executing a single node in the DAG
 */
public class NodeExecutionResult {
    
    public enum ExecutionStatus {
        SUCCESS, FAILURE, SKIPPED
    }
    
    @JsonProperty("nodeId")
    private final String nodeId;
    @JsonProperty("status")
    private final ExecutionStatus status;
    @JsonProperty("result")
    private final ExecutionResult result;
    @JsonProperty("startTime")
    private final long startTime;
    @JsonProperty("endTime")
    private final long endTime;
    @JsonProperty("error")
    private final Exception error;
    
    @JsonCreator
    public NodeExecutionResult(
            @JsonProperty("nodeId") String nodeId, 
            @JsonProperty("status") ExecutionStatus status, 
            @JsonProperty("result") ExecutionResult result, 
            @JsonProperty("startTime") long startTime, 
            @JsonProperty("endTime") long endTime, 
            @JsonProperty("error") Exception error) {
        this.nodeId = nodeId;
        this.status = status;
        this.result = result;
        this.startTime = startTime;
        this.endTime = endTime;
        this.error = error;
    }
    
    /**
     * Create a successful node execution result
     */
    public static NodeExecutionResult success(String nodeId, String message) {
        long currentTime = System.currentTimeMillis();
        ExecutionResult result = ExecutionResult.success(message);
        return new NodeExecutionResult(nodeId, ExecutionStatus.SUCCESS, result, 
                                     currentTime, currentTime, null);
    }
    
    /**
     * Create a successful node execution result with data
     */
    public static NodeExecutionResult success(String nodeId, String message, Object data) {
        long currentTime = System.currentTimeMillis();
        ExecutionResult result = ExecutionResult.success(message, data);
        return new NodeExecutionResult(nodeId, ExecutionStatus.SUCCESS, result, 
                                     currentTime, currentTime, null);
    }
    
    /**
     * Create a failed node execution result
     */
    public static NodeExecutionResult failure(String nodeId, String message, Exception error) {
        long currentTime = System.currentTimeMillis();
        ExecutionResult result = ExecutionResult.failure(message, error);
        return new NodeExecutionResult(nodeId, ExecutionStatus.FAILURE, result, 
                                     currentTime, currentTime, error);
    }
    
    /**
     * Create a failed node execution result with a specific error type
     */
    public static NodeExecutionResult failure(String nodeId, String message, Exception error, ExecutionErrorType errorType) {
        long currentTime = System.currentTimeMillis();
        ExecutionResult result = ExecutionResult.failure(message, error, errorType);
        return new NodeExecutionResult(nodeId, ExecutionStatus.FAILURE, result, 
                                     currentTime, currentTime, error);
    }
    
    /**
     * Create a skipped node execution result
     */
    public static NodeExecutionResult skipped(String nodeId, String reason) {
        long currentTime = System.currentTimeMillis();
        ExecutionResult result = ExecutionResult.failure("Skipped: " + reason);
        return new NodeExecutionResult(nodeId, ExecutionStatus.SKIPPED, result, 
                                     currentTime, currentTime, null);
    }
    
    // Getters
    public String getNodeId() {
        return nodeId;
    }
    
    public ExecutionStatus getStatus() {
        return status;
    }
    
    public ExecutionResult getResult() {
        return result;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    @JsonIgnore
    public long getDuration() {
        return endTime - startTime;
    }
    
    public Exception getError() {
        return error;
    }
    
    @JsonIgnore
    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }
    
    @JsonIgnore
    public boolean isFailure() {
        return status == ExecutionStatus.FAILURE;
    }
    
    @JsonIgnore
    public boolean isSkipped() {
        return status == ExecutionStatus.SKIPPED;
    }
    
    @Override
    public String toString() {
        return "NodeExecutionResult{" +
                "nodeId='" + nodeId + '\'' +
                ", status=" + status +
                ", duration=" + getDuration() + "ms" +
                '}';
    }
}