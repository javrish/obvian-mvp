package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a business rule that can be defined in natural language and executed.
 * Supports various rule types including conditional, temporal, calculation, validation,
 * workflow, and exception handling rules.
 * 
 * This class is part of the Natural Language Business Rule Engine (Phase 26.2b)
 * which implements Product Patent 24 - Natural Language Business Rule Processing.
 * 
 * Patent Alignment: Implements core business rule representation with support for
 * conversational definition, complex conditions, and plugin-based action execution.
 * 
 * @author Obvian Labs
 * @since Phase 26.2b
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessRule {
    
    /**
     * Types of business rules supported by the engine
     */
    public enum RuleType {
        CONDITIONAL,    // If-then-else logic
        TEMPORAL,       // Time-based scheduling
        CALCULATION,    // Mathematical computations
        VALIDATION,     // Data validation rules
        WORKFLOW,       // Multi-step processes
        EXCEPTION       // Error handling and recovery
    }
    
    @JsonProperty("id")
    private final String id;
    
    @JsonProperty("name")
    private final String name;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("ruleType")
    private final RuleType ruleType;
    
    @JsonProperty("condition")
    private final RuleCondition condition;
    
    @JsonProperty("actions")
    private final List<RuleAction> actions;
    
    @JsonProperty("schedule")
    private final String schedule; // Cron expression for temporal rules
    
    @JsonProperty("priority")
    private final int priority;
    
    @JsonProperty("active")
    private final boolean active;
    
    @JsonProperty("version")
    private final String version;
    
    @JsonProperty("author")
    private final String author;
    
    @JsonProperty("tags")
    private final List<String> tags;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    @JsonProperty("effectiveFrom")
    private final LocalDateTime effectiveFrom;
    
    @JsonProperty("effectiveUntil")
    private final LocalDateTime effectiveUntil;
    
    @JsonProperty("timeout")
    private final long timeout; // Execution timeout in milliseconds
    
    @JsonProperty("createdAt")
    private final LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
    private final LocalDateTime updatedAt;
    
    @JsonProperty("archived")
    private boolean archived;
    
    @JsonProperty("archivedAt")
    private LocalDateTime archivedAt;
    
    @JsonProperty("archivedBy")
    private String archivedBy;
    
    @JsonCreator
    private BusinessRule(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("ruleType") RuleType ruleType,
            @JsonProperty("condition") RuleCondition condition,
            @JsonProperty("actions") List<RuleAction> actions,
            @JsonProperty("schedule") String schedule,
            @JsonProperty("priority") int priority,
            @JsonProperty("active") boolean active,
            @JsonProperty("version") String version,
            @JsonProperty("author") String author,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("effectiveFrom") LocalDateTime effectiveFrom,
            @JsonProperty("effectiveUntil") LocalDateTime effectiveUntil,
            @JsonProperty("timeout") long timeout,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("updatedAt") LocalDateTime updatedAt,
            @JsonProperty("archived") boolean archived,
            @JsonProperty("archivedAt") LocalDateTime archivedAt,
            @JsonProperty("archivedBy") String archivedBy) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ruleType = ruleType;
        this.condition = condition;
        this.actions = actions != null ? new ArrayList<>(actions) : new ArrayList<>();
        this.schedule = schedule;
        this.priority = priority;
        this.active = active;
        this.version = version;
        this.author = author;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.timeout = timeout;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
        this.archived = archived;
        this.archivedAt = archivedAt;
        this.archivedBy = archivedBy;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public RuleType getRuleType() { return ruleType; }
    public RuleCondition getCondition() { return condition; }
    public List<RuleAction> getActions() { return new ArrayList<>(actions); }
    public String getSchedule() { return schedule; }
    public int getPriority() { return priority; }
    public boolean isActive() { return active; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public List<String> getTags() { return new ArrayList<>(tags); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public LocalDateTime getEffectiveUntil() { return effectiveUntil; }
    public long getTimeout() { return timeout; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isArchived() { return archived; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public String getArchivedBy() { return archivedBy; }
    
    // Setters for archive fields (since they need to be mutable)
    public void setArchived(boolean archived) { this.archived = archived; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
    public void setArchivedBy(String archivedBy) { this.archivedBy = archivedBy; }
    
    /**
     * Check if this rule is effective at the given time
     */
    public boolean isEffectiveAt(LocalDateTime dateTime) {
        if (!active) return false;
        if (effectiveFrom != null && dateTime.isBefore(effectiveFrom)) return false;
        if (effectiveUntil != null && dateTime.isAfter(effectiveUntil)) return false;
        return true;
    }
    
    /**
     * Check if this rule is currently effective
     */
    public boolean isCurrentlyEffective() {
        return isEffectiveAt(LocalDateTime.now());
    }
    
    /**
     * Builder pattern for creating BusinessRule instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private RuleType ruleType;
        private RuleCondition condition;
        private List<RuleAction> actions = new ArrayList<>();
        private String schedule;
        private int priority = 100;
        private boolean active = true;
        private String version = "1.0.0";
        private String author;
        private List<String> tags = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveUntil;
        private long timeout = 30000; // 30 seconds default
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean archived = false;
        private LocalDateTime archivedAt;
        private String archivedBy;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder ruleType(RuleType ruleType) {
            this.ruleType = ruleType;
            return this;
        }
        
        public Builder condition(RuleCondition condition) {
            this.condition = condition;
            return this;
        }
        
        public Builder addAction(RuleAction action) {
            this.actions.add(action);
            return this;
        }
        
        public Builder actions(List<RuleAction> actions) {
            this.actions = new ArrayList<>(actions);
            return this;
        }
        
        public Builder schedule(String schedule) {
            this.schedule = schedule;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder author(String author) {
            this.author = author;
            return this;
        }
        
        public Builder addTag(String tag) {
            this.tags.add(tag);
            return this;
        }
        
        public Builder tags(List<String> tags) {
            this.tags = new ArrayList<>(tags);
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }
        
        public Builder effectiveFrom(LocalDateTime effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
            return this;
        }
        
        public Builder effectiveUntil(LocalDateTime effectiveUntil) {
            this.effectiveUntil = effectiveUntil;
            return this;
        }
        
        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public Builder archived(boolean archived) {
            this.archived = archived;
            return this;
        }
        
        public Builder archivedAt(LocalDateTime archivedAt) {
            this.archivedAt = archivedAt;
            return this;
        }
        
        public Builder archivedBy(String archivedBy) {
            this.archivedBy = archivedBy;
            return this;
        }
        
        public BusinessRule build() {
            validate();
            return new BusinessRule(id, name, description, ruleType, condition, actions,
                                  schedule, priority, active, version, author, tags, metadata,
                                  effectiveFrom, effectiveUntil, timeout, createdAt, updatedAt,
                                  archived, archivedAt, archivedBy);
        }
        
        private void validate() {
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("Rule ID is required");
            }
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Rule name is required");
            }
            if (ruleType == null) {
                throw new IllegalArgumentException("Rule type is required");
            }
            if (actions.isEmpty()) {
                throw new IllegalArgumentException("Rules must have at least one action");
            }
            
            // Type-specific validations
            switch (ruleType) {
                case CONDITIONAL:
                case WORKFLOW:
                case VALIDATION:
                case EXCEPTION:
                    if (condition == null) {
                        throw new IllegalArgumentException(
                            ruleType + " rules must have a condition");
                    }
                    break;
                case TEMPORAL:
                    if (schedule == null || schedule.trim().isEmpty()) {
                        throw new IllegalArgumentException("Temporal rules must have a schedule");
                    }
                    break;
                case CALCULATION:
                    // Calculation rules may or may not have conditions
                    break;
            }
            
            // Validate effective date range
            if (effectiveFrom != null && effectiveUntil != null && 
                effectiveFrom.isAfter(effectiveUntil)) {
                throw new IllegalArgumentException(
                    "Effective from date must be before effective until date");
            }
            
            // Validate timeout
            if (timeout <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusinessRule that = (BusinessRule) o;
        return Objects.equals(id, that.id) && 
               Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }
    
    @Override
    public String toString() {
        return "BusinessRule{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", ruleType=" + ruleType +
                ", priority=" + priority +
                ", active=" + active +
                ", version='" + version + '\'' +
                ", actionsCount=" + actions.size() +
                '}';
    }
}