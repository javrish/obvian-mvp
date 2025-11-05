package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for node execution updates sent via WebSocket for visual trace
 * 
 * This DTO represents real-time updates about node execution progress
 * within a DAG execution, providing frontend components with the
 * information needed to update visual trace displays.
 * 
 * Features:
 * - Node identification and status tracking
 * - Execution timing and progress information
 * - Contextual data and results
 * - Error information when applicable
 * - Visual trace metadata
 * 
 * @author Obvian Labs
 * @since Phase 26.2
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeExecutionUpdateDto {
    
    @JsonProperty("executionId")
    private String executionId;
    
    @JsonProperty("nodeId")
    private String nodeId;
    
    @JsonProperty("nodeName")
    private String nodeName;
    
    @JsonProperty("nodeType")
    private String nodeType;
    
    @JsonProperty("status")
    private NodeExecutionStatus status;
    
    @JsonProperty("progress")
    private Double progress; // 0.0 to 1.0
    
    @JsonProperty("startTime")
    private Instant startTime;
    
    @JsonProperty("endTime")
    private Instant endTime;
    
    @JsonProperty("duration")
    private Long durationMs;
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error")
    private ErrorInfo error;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("visualContext")
    private VisualContext visualContext;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    // Constructors
    public NodeExecutionUpdateDto() {
        this.timestamp = Instant.now();
    }
    
    public NodeExecutionUpdateDto(String executionId, String nodeId, NodeExecutionStatus status) {
        this();
        this.executionId = executionId;
        this.nodeId = nodeId;
        this.status = status;
    }
    
    public NodeExecutionUpdateDto(String executionId, String nodeId, String nodeName, 
                                 String nodeType, NodeExecutionStatus status) {
        this(executionId, nodeId, status);
        this.nodeName = nodeName;
        this.nodeType = nodeType;
    }
    
    // Getters and Setters
    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }
    
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    
    public NodeExecutionStatus getStatus() { return status; }
    public void setStatus(NodeExecutionStatus status) { this.status = status; }
    
    public Double getProgress() { return progress; }
    public void setProgress(Double progress) { this.progress = progress; }
    
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    
    public ErrorInfo getError() { return error; }
    public void setError(ErrorInfo error) { this.error = error; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public VisualContext getVisualContext() { return visualContext; }
    public void setVisualContext(VisualContext visualContext) { this.visualContext = visualContext; }
    
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    // Fluent setters for builder pattern
    public NodeExecutionUpdateDto withExecutionId(String executionId) {
        this.executionId = executionId;
        return this;
    }
    
    public NodeExecutionUpdateDto withNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }
    
    public NodeExecutionUpdateDto withNodeName(String nodeName) {
        this.nodeName = nodeName;
        return this;
    }
    
    public NodeExecutionUpdateDto withNodeType(String nodeType) {
        this.nodeType = nodeType;
        return this;
    }
    
    public NodeExecutionUpdateDto withStatus(NodeExecutionStatus status) {
        this.status = status;
        return this;
    }
    
    public NodeExecutionUpdateDto withProgress(Double progress) {
        this.progress = progress;
        return this;
    }
    
    public NodeExecutionUpdateDto withStartTime(Instant startTime) {
        this.startTime = startTime;
        return this;
    }
    
    public NodeExecutionUpdateDto withEndTime(Instant endTime) {
        this.endTime = endTime;
        return this;
    }
    
    public NodeExecutionUpdateDto withDuration(Long durationMs) {
        this.durationMs = durationMs;
        return this;
    }
    
    public NodeExecutionUpdateDto withResult(Object result) {
        this.result = result;
        return this;
    }
    
    public NodeExecutionUpdateDto withError(ErrorInfo error) {
        this.error = error;
        return this;
    }
    
    public NodeExecutionUpdateDto withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }
    
    public NodeExecutionUpdateDto withVisualContext(VisualContext visualContext) {
        this.visualContext = visualContext;
        return this;
    }
    
    // Helper methods
    public boolean isCompleted() {
        return status == NodeExecutionStatus.COMPLETED || status == NodeExecutionStatus.FAILED;
    }
    
    public boolean isInProgress() {
        return status == NodeExecutionStatus.RUNNING || status == NodeExecutionStatus.PENDING;
    }
    
    public boolean hasError() {
        return error != null || status == NodeExecutionStatus.FAILED;
    }
    
    // Nested classes
    
    /**
     * Node execution status enumeration
     */
    public enum NodeExecutionStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED,
        SKIPPED,
        RETRYING,
        PAUSED
    }
    
    /**
     * Error information for failed executions
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorInfo {
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("details")
        private Map<String, Object> details;
        
        @JsonProperty("stackTrace")
        private String stackTrace;
        
        public ErrorInfo() {}
        
        public ErrorInfo(String type, String message) {
            this.type = type;
            this.message = message;
        }
        
        public ErrorInfo(String type, String message, String code) {
            this(type, message);
            this.code = code;
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
        
        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    }
    
    /**
     * Visual context information for rendering
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VisualContext {
        @JsonProperty("position")
        private Position position;
        
        @JsonProperty("style")
        private Map<String, Object> style;
        
        @JsonProperty("highlight")
        private Boolean highlight;
        
        @JsonProperty("opacity")
        private Double opacity;
        
        @JsonProperty("color")
        private String color;
        
        @JsonProperty("size")
        private String size; // small, medium, large
        
        @JsonProperty("animation")
        private String animation; // pulse, glow, shake, etc.
        
        public VisualContext() {}
        
        public VisualContext(Position position) {
            this.position = position;
        }
        
        // Getters and Setters
        public Position getPosition() { return position; }
        public void setPosition(Position position) { this.position = position; }
        
        public Map<String, Object> getStyle() { return style; }
        public void setStyle(Map<String, Object> style) { this.style = style; }
        
        public Boolean getHighlight() { return highlight; }
        public void setHighlight(Boolean highlight) { this.highlight = highlight; }
        
        public Double getOpacity() { return opacity; }
        public void setOpacity(Double opacity) { this.opacity = opacity; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        
        public String getAnimation() { return animation; }
        public void setAnimation(String animation) { this.animation = animation; }
    }
    
    /**
     * Position information for visual layout
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Position {
        @JsonProperty("x")
        private Double x;
        
        @JsonProperty("y")
        private Double y;
        
        @JsonProperty("z")
        private Double z;
        
        @JsonProperty("layer")
        private Integer layer;
        
        public Position() {}
        
        public Position(Double x, Double y) {
            this.x = x;
            this.y = y;
        }
        
        public Position(Double x, Double y, Double z) {
            this(x, y);
            this.z = z;
        }
        
        // Getters and Setters
        public Double getX() { return x; }
        public void setX(Double x) { this.x = x; }
        
        public Double getY() { return y; }
        public void setY(Double y) { this.y = y; }
        
        public Double getZ() { return z; }
        public void setZ(Double z) { this.z = z; }
        
        public Integer getLayer() { return layer; }
        public void setLayer(Integer layer) { this.layer = layer; }
    }
    
    @Override
    public String toString() {
        return String.format("NodeExecutionUpdate{executionId='%s', nodeId='%s', status=%s, progress=%s, timestamp=%s}",
            executionId, nodeId, status, progress, timestamp);
    }
}