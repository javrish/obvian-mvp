package api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity for tracking individual business rule executions.
 * Provides audit trail and execution history for business rules.
 */
@Entity
@Table(name = "business_rule_executions", indexes = {
    @Index(name = "idx_rule_exec_rule_id", columnList = "business_rule_id"),
    @Index(name = "idx_rule_exec_status", columnList = "status"),
    @Index(name = "idx_rule_exec_executed_at", columnList = "executed_at"),
    @Index(name = "idx_rule_exec_user_id", columnList = "executed_by_user_id")
})
public class BusinessRuleExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", length = 100)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_rule_id", nullable = false)
    private BusinessRuleEntity businessRule;
    
    @Column(name = "execution_id", length = 100)
    private String executionId;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;
    
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "duration_ms")
    private Long durationMs;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by_user_id")
    private User executedByUser;
    
    @Column(name = "trigger_type", length = 50)
    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;
    
    @Column(name = "trigger_source", length = 255)
    private String triggerSource;
    
    @Column(name = "input_context", columnDefinition = "TEXT")
    private String inputContext;
    
    @Column(name = "output_result", columnDefinition = "TEXT")
    private String outputResult;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;
    
    @Column(name = "actions_executed")
    private Integer actionsExecuted = 0;
    
    @Column(name = "actions_failed")
    private Integer actionsFailed = 0;
    
    @Column(name = "plugins_used", columnDefinition = "TEXT")
    private String pluginsUsed;
    
    @Column(name = "execution_trace", columnDefinition = "TEXT")
    private String executionTrace;
    
    @Column(name = "memory_usage_bytes")
    private Long memoryUsageBytes;
    
    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;
    
    // Relationships to other executions
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_execution_id")
    private BusinessRuleExecution parentExecution;
    
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    @Column(name = "is_dry_run", nullable = false)
    private Boolean isDryRun = false;
    
    @Column(name = "is_simulation", nullable = false)
    private Boolean isSimulation = false;
    
    public enum TriggerType {
        MANUAL,
        SCHEDULED,
        EVENT_DRIVEN,
        API_CALL,
        DEPENDENCY,
        CASCADE,
        RETRY
    }
    
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
    
    public BusinessRuleEntity getBusinessRule() {
        return businessRule;
    }
    
    public void setBusinessRule(BusinessRuleEntity businessRule) {
        this.businessRule = businessRule;
    }
    
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public ExecutionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ExecutionStatus status) {
        this.status = status;
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
    
    public User getExecutedByUser() {
        return executedByUser;
    }
    
    public void setExecutedByUser(User executedByUser) {
        this.executedByUser = executedByUser;
    }
    
    public TriggerType getTriggerType() {
        return triggerType;
    }
    
    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }
    
    public String getTriggerSource() {
        return triggerSource;
    }
    
    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }
    
    public String getInputContext() {
        return inputContext;
    }
    
    public void setInputContext(String inputContext) {
        this.inputContext = inputContext;
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
    
    public Integer getActionsExecuted() {
        return actionsExecuted;
    }
    
    public void setActionsExecuted(Integer actionsExecuted) {
        this.actionsExecuted = actionsExecuted;
    }
    
    public Integer getActionsFailed() {
        return actionsFailed;
    }
    
    public void setActionsFailed(Integer actionsFailed) {
        this.actionsFailed = actionsFailed;
    }
    
    public String getPluginsUsed() {
        return pluginsUsed;
    }
    
    public void setPluginsUsed(String pluginsUsed) {
        this.pluginsUsed = pluginsUsed;
    }
    
    public String getExecutionTrace() {
        return executionTrace;
    }
    
    public void setExecutionTrace(String executionTrace) {
        this.executionTrace = executionTrace;
    }
    
    public Long getMemoryUsageBytes() {
        return memoryUsageBytes;
    }
    
    public void setMemoryUsageBytes(Long memoryUsageBytes) {
        this.memoryUsageBytes = memoryUsageBytes;
    }
    
    public Double getCpuUsagePercent() {
        return cpuUsagePercent;
    }
    
    public void setCpuUsagePercent(Double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }
    
    public BusinessRuleExecution getParentExecution() {
        return parentExecution;
    }
    
    public void setParentExecution(BusinessRuleExecution parentExecution) {
        this.parentExecution = parentExecution;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public Boolean getIsDryRun() {
        return isDryRun;
    }
    
    public void setIsDryRun(Boolean isDryRun) {
        this.isDryRun = isDryRun;
    }
    
    public Boolean getIsSimulation() {
        return isSimulation;
    }
    
    public void setIsSimulation(Boolean isSimulation) {
        this.isSimulation = isSimulation;
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