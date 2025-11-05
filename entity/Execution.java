package api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Execution entity representing DAG/workflow execution history.
 */
@Entity
@Table(name = "executions", indexes = {
    @Index(name = "idx_execution_user_id", columnList = "user_id"),
    @Index(name = "idx_execution_status", columnList = "status"),
    @Index(name = "idx_execution_created_at", columnList = "created_at"),
    @Index(name = "idx_execution_type", columnList = "execution_type")
})
public class Execution {
    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "execution_type", nullable = false, length = 50)
    private String executionType; // DAG, PROMPT, WORKFLOW

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status;

    @Column(name = "input_prompt", columnDefinition = "TEXT")
    private String inputPrompt;

    @Lob
    @Column(name = "dag_definition", columnDefinition = "TEXT")
    private String dagDefinition;

    @Lob
    @Column(name = "execution_result", columnDefinition = "TEXT")
    private String executionResult;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "parent_execution_id", length = 36)
    private String parentExecutionId;

    @ElementCollection
    @CollectionTable(name = "execution_metadata", joinColumns = @JoinColumn(name = "execution_id"))
    @MapKeyColumn(name = "key")
    @Column(name = "value")
    private Map<String, String> metadata = new HashMap<>();

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ExecutionNode> nodes = new HashSet<>();

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = ExecutionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.status == ExecutionStatus.COMPLETED || this.status == ExecutionStatus.FAILED) {
            if (this.completedAt == null) {
                this.completedAt = Instant.now();
            }
            if (this.startedAt != null && this.durationMs == null) {
                this.durationMs = this.completedAt.toEpochMilli() - this.startedAt.toEpochMilli();
            }
        }
    }

    public Execution() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.status = ExecutionStatus.PENDING;
        this.retryCount = 0;
    }

    public Execution(User user, String executionType, String inputPrompt) {
        this();
        this.user = user;
        this.executionType = executionType;
        this.inputPrompt = inputPrompt;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = executionType;
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

    public String getInputPrompt() {
        return inputPrompt;
    }

    public void setInputPrompt(String inputPrompt) {
        this.inputPrompt = inputPrompt;
    }

    public String getDagDefinition() {
        return dagDefinition;
    }

    public void setDagDefinition(String dagDefinition) {
        this.dagDefinition = dagDefinition;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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

    public String getParentExecutionId() {
        return parentExecutionId;
    }

    public void setParentExecutionId(String parentExecutionId) {
        this.parentExecutionId = parentExecutionId;
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

    public Set<ExecutionNode> getNodes() {
        return nodes;
    }

    public void setNodes(Set<ExecutionNode> nodes) {
        this.nodes = nodes;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Execution)) return false;
        Execution execution = (Execution) o;
        return id != null && id.equals(execution.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}