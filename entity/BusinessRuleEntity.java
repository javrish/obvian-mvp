package api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity for persisting business rules in the database.
 * Maps to the business_rules table and provides full persistence
 * for natural language business rules.
 */
@Entity
@Table(name = "business_rules", indexes = {
    @Index(name = "idx_rule_name", columnList = "name"),
    @Index(name = "idx_rule_type", columnList = "rule_type"),
    @Index(name = "idx_rule_active", columnList = "active"),
    @Index(name = "idx_rule_priority", columnList = "priority"),
    @Index(name = "idx_rule_author", columnList = "author"),
    @Index(name = "idx_rule_created", columnList = "created_at"),
    @Index(name = "idx_rule_archived", columnList = "archived")
})
public class BusinessRuleEntity {
    
    @Id
    @Column(name = "id", length = 100)
    private String id;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "rule_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RuleType ruleType;
    
    @Column(name = "natural_language_text", columnDefinition = "TEXT")
    private String naturalLanguageText;
    
    @Column(name = "condition_json", columnDefinition = "TEXT")
    private String conditionJson;
    
    @Column(name = "actions_json", columnDefinition = "TEXT")
    private String actionsJson;
    
    @Column(name = "schedule", length = 255)
    private String schedule;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    @Column(name = "version", length = 50)
    private String version;
    
    @Column(name = "author", length = 255)
    private String author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ElementCollection
    @CollectionTable(name = "business_rule_tags", 
                     joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();
    
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
    
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;
    
    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;
    
    @Column(name = "timeout_ms")
    private Long timeoutMs = 30000L; // Default 30 seconds
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "archived", nullable = false)
    private Boolean archived = false;
    
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
    
    @Column(name = "archived_by", length = 255)
    private String archivedBy;
    
    // Audit fields
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;
    
    @Column(name = "execution_count")
    private Long executionCount = 0L;
    
    @Column(name = "success_count")
    private Long successCount = 0L;
    
    @Column(name = "failure_count")
    private Long failureCount = 0L;
    
    @Column(name = "avg_execution_time_ms")
    private Long avgExecutionTimeMs;
    
    // Validation tracking
    @Column(name = "last_validated_at")
    private LocalDateTime lastValidatedAt;
    
    @Column(name = "validation_status", length = 50)
    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus;
    
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
    
    // Dependencies and conflicts
    @ManyToMany
    @JoinTable(
        name = "business_rule_dependencies",
        joinColumns = @JoinColumn(name = "rule_id"),
        inverseJoinColumns = @JoinColumn(name = "dependency_id")
    )
    private Set<BusinessRuleEntity> dependencies = new HashSet<>();
    
    @ManyToMany
    @JoinTable(
        name = "business_rule_conflicts",
        joinColumns = @JoinColumn(name = "rule_id"),
        inverseJoinColumns = @JoinColumn(name = "conflict_id")
    )
    private Set<BusinessRuleEntity> conflicts = new HashSet<>();
    
    // Execution history relationship
    @OneToMany(mappedBy = "businessRule", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BusinessRuleExecution> executions = new HashSet<>();
    
    public enum RuleType {
        CONDITIONAL,
        TEMPORAL,
        CALCULATION,
        VALIDATION,
        WORKFLOW,
        EXCEPTION
    }
    
    public enum ValidationStatus {
        VALID,
        WARNING,
        ERROR,
        NOT_VALIDATED
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (validationStatus == null) {
            validationStatus = ValidationStatus.NOT_VALIDATED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public RuleType getRuleType() {
        return ruleType;
    }
    
    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }
    
    public String getNaturalLanguageText() {
        return naturalLanguageText;
    }
    
    public void setNaturalLanguageText(String naturalLanguageText) {
        this.naturalLanguageText = naturalLanguageText;
    }
    
    public String getConditionJson() {
        return conditionJson;
    }
    
    public void setConditionJson(String conditionJson) {
        this.conditionJson = conditionJson;
    }
    
    public String getActionsJson() {
        return actionsJson;
    }
    
    public void setActionsJson(String actionsJson) {
        this.actionsJson = actionsJson;
    }
    
    public String getSchedule() {
        return schedule;
    }
    
    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Set<String> getTags() {
        return tags;
    }
    
    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
    
    public String getMetadataJson() {
        return metadataJson;
    }
    
    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
    
    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }
    
    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }
    
    public LocalDateTime getEffectiveUntil() {
        return effectiveUntil;
    }
    
    public void setEffectiveUntil(LocalDateTime effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }
    
    public Long getTimeoutMs() {
        return timeoutMs;
    }
    
    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getArchived() {
        return archived;
    }
    
    public void setArchived(Boolean archived) {
        this.archived = archived;
    }
    
    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }
    
    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }
    
    public String getArchivedBy() {
        return archivedBy;
    }
    
    public void setArchivedBy(String archivedBy) {
        this.archivedBy = archivedBy;
    }
    
    public LocalDateTime getLastExecutedAt() {
        return lastExecutedAt;
    }
    
    public void setLastExecutedAt(LocalDateTime lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }
    
    public Long getExecutionCount() {
        return executionCount;
    }
    
    public void setExecutionCount(Long executionCount) {
        this.executionCount = executionCount;
    }
    
    public Long getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }
    
    public Long getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(Long failureCount) {
        this.failureCount = failureCount;
    }
    
    public Long getAvgExecutionTimeMs() {
        return avgExecutionTimeMs;
    }
    
    public void setAvgExecutionTimeMs(Long avgExecutionTimeMs) {
        this.avgExecutionTimeMs = avgExecutionTimeMs;
    }
    
    public LocalDateTime getLastValidatedAt() {
        return lastValidatedAt;
    }
    
    public void setLastValidatedAt(LocalDateTime lastValidatedAt) {
        this.lastValidatedAt = lastValidatedAt;
    }
    
    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }
    
    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }
    
    public String getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public Set<BusinessRuleEntity> getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(Set<BusinessRuleEntity> dependencies) {
        this.dependencies = dependencies;
    }
    
    public Set<BusinessRuleEntity> getConflicts() {
        return conflicts;
    }
    
    public void setConflicts(Set<BusinessRuleEntity> conflicts) {
        this.conflicts = conflicts;
    }
    
    public Set<BusinessRuleExecution> getExecutions() {
        return executions;
    }
    
    public void setExecutions(Set<BusinessRuleExecution> executions) {
        this.executions = executions;
    }
    
    // Helper methods
    public void incrementExecutionCount() {
        this.executionCount = (this.executionCount != null ? this.executionCount : 0) + 1;
    }
    
    public void incrementSuccessCount() {
        this.successCount = (this.successCount != null ? this.successCount : 0) + 1;
    }
    
    public void incrementFailureCount() {
        this.failureCount = (this.failureCount != null ? this.failureCount : 0) + 1;
    }
    
    public void updateAverageExecutionTime(long executionTimeMs) {
        if (this.avgExecutionTimeMs == null || this.executionCount == 1) {
            this.avgExecutionTimeMs = executionTimeMs;
        } else {
            // Calculate new average
            long totalTime = this.avgExecutionTimeMs * (this.executionCount - 1) + executionTimeMs;
            this.avgExecutionTimeMs = totalTime / this.executionCount;
        }
    }
}