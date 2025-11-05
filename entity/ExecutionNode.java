package api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * ExecutionNode entity representing individual nodes within an execution DAG.
 */
@Entity
@Table(name = "execution_nodes", indexes = {
    @Index(name = "idx_node_execution_id", columnList = "execution_id"),
    @Index(name = "idx_node_status", columnList = "status"),
    @Index(name = "idx_node_plugin_name", columnList = "plugin_name")
})
public class ExecutionNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private Execution execution;

    @Column(name = "node_id", nullable = false, length = 100)
    private String nodeId;

    @Column(name = "node_name", nullable = false)
    private String nodeName;

    @Column(name = "plugin_name", length = 100)
    private String pluginName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status;

    @Lob
    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Lob
    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @ElementCollection
    @CollectionTable(name = "node_metadata", joinColumns = @JoinColumn(name = "node_id"))
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    private Map<String, String> metadata = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ExecutionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if ((this.status == ExecutionStatus.COMPLETED || this.status == ExecutionStatus.FAILED) 
            && this.completedAt == null) {
            this.completedAt = Instant.now();
            if (this.startedAt != null && this.durationMs == null) {
                this.durationMs = this.completedAt.toEpochMilli() - this.startedAt.toEpochMilli();
            }
        }
    }

    public ExecutionNode() {
        this.status = ExecutionStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    public ExecutionNode(Execution execution, String nodeId, String nodeName) {
        this();
        this.execution = execution;
        this.nodeId = nodeId;
        this.nodeName = nodeName;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
        if (status == ExecutionStatus.RUNNING && this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getOutputData() {
        return outputData;
    }

    public void setOutputData(String outputData) {
        this.outputData = outputData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetries && this.status == ExecutionStatus.FAILED;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecutionNode)) return false;
        ExecutionNode that = (ExecutionNode) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}