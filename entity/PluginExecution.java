package api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity for tracking individual plugin executions.
 * Provides audit trail and execution history for plugins.
 */
@Entity
@Table(name = "plugin_executions", indexes = {
    @Index(name = "idx_plugin_exec_plugin_id", columnList = "plugin_id"),
    @Index(name = "idx_plugin_exec_status", columnList = "status"),
    @Index(name = "idx_plugin_exec_executed_at", columnList = "executed_at"),
    @Index(name = "idx_plugin_exec_execution_id", columnList = "dag_execution_id")
})
public class PluginExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 100)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plugin_id", nullable = false)
    private PluginEntity plugin;
    
    @Column(name = "dag_execution_id", length = 100)
    private String dagExecutionId;
    
    @Column(name = "node_id", length = 100)
    private String nodeId;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    
    @Column(name = "action", length = 255)
    private String action;
    
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    @Column(name = "input_params", columnDefinition = "TEXT")
    private String inputParams;
    
    @Column(name = "output_result", columnDefinition = "TEXT")
    private String outputResult;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "is_fallback", nullable = false)
    private Boolean isFallback = false;
    
    @Column(name = "memory_usage_bytes")
    private Long memoryUsageBytes;
    
    @Column(name = "cpu_time_ms")
    private Long cpuTimeMs;
    
    @Column(name = "execution_context", columnDefinition = "TEXT")
    private String executionContext;
    
    @Column(name = "trace_id", length = 100)
    private String traceId;
    
    @Column(name = "span_id", length = 100)
    private String spanId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User executedByUser;
    
    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ExecutionStatus.PENDING;
        }
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public PluginEntity getPlugin() {
        return plugin;
    }
    
    public void setPlugin(PluginEntity plugin) {
        this.plugin = plugin;
    }
    
    public String getDagExecutionId() {
        return dagExecutionId;
    }
    
    public void setDagExecutionId(String dagExecutionId) {
        this.dagExecutionId = dagExecutionId;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public ExecutionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
    
    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public Long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
    
    public String getInputParams() {
        return inputParams;
    }
    
    public void setInputParams(String inputParams) {
        this.inputParams = inputParams;
    }
    
    public String getOutputResult() {
        return outputResult;
    }
    
    public void setOutputResult(String outputResult) {
        this.outputResult = outputResult;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Boolean getIsFallback() {
        return isFallback;
    }
    
    public void setIsFallback(Boolean isFallback) {
        this.isFallback = isFallback;
    }
    
    public Long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }
    
    public void setMemoryUsageBytes(Long memoryUsageBytes) {
        this.memoryUsageBytes = memoryUsageBytes;
    }
    
    public Long getCpuTimeMs() {
        return cpuTimeMs;
    }
    
    public void setCpuTimeMs(Long cpuTimeMs) {
        this.cpuTimeMs = cpuTimeMs;
    }
    
    public String getExecutionContext() {
        return executionContext;
    }
    
    public void setExecutionContext(String executionContext) {
        this.executionContext = executionContext;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    public String getSpanId() {
        return spanId;
    }
    
    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }
    
    public User getExecutedByUser() {
        return executedByUser;
    }
    
    public void setExecutedByUser(User executedByUser) {
        this.executedByUser = executedByUser;
    }
    
    // Helper methods
    public void complete(boolean success) {
        this.completedAt = LocalDateTime.now();
        this.status = success ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED;
        if (this.executedAt != null) {
            this.durationMs = java.time.Duration.between(this.executedAt, this.completedAt).toMillis();
        }
    }
}