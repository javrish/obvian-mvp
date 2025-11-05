package api.dto;

import core.RuleExecutionResult;
import core.RuleAction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Transfer Object representing an entry in business rule execution history.
 * Contains comprehensive information about a rule execution including timing,
 * results, actions performed, and any errors encountered.
 * 
 * @author Obvian Labs
 * @since Phase 26.2b-service
 */
public class RuleExecutionHistoryEntry {
    
    @NotBlank(message = "Execution ID is required")
    private String executionId;
    
    @NotBlank(message = "Rule ID is required")
    private String ruleId;
    
    private String ruleName;
    
    private String ruleVersion;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotNull(message = "Execution timestamp is required")
    private LocalDateTime executedAt;
    
    private long executionTimeMs;
    
    private RuleExecutionResult.ExecutionStatus status;
    
    private boolean successful;
    
    private String errorMessage;
    
    private String errorType;
    
    private int executedActions;
    
    private int failedActions;
    
    private int skippedActions;
    
    private List<String> actionTypes;
    
    private Map<String, Object> executionContext;
    
    private Map<String, Object> results;
    
    private String sessionId;
    
    private String executionEnvironment;
    
    private String triggerType;
    
    private String triggerSource;
    
    private Map<String, Object> performanceMetrics;
    
    private List<String> warnings;
    
    private Map<String, String> tags;
    
    /**
     * Execution trigger types
     */
    public enum TriggerType {
        MANUAL,
        SCHEDULED,
        EVENT_DRIVEN,
        API_CALL,
        WEBHOOK,
        SIMULATION,
        TEST
    }
    
    /**
     * Execution environments
     */
    public enum ExecutionEnvironment {
        PRODUCTION,
        STAGING,
        DEVELOPMENT,
        TEST,
        SIMULATION
    }
    
    public RuleExecutionHistoryEntry() {
        this.actionTypes = new ArrayList<>();
        this.executionContext = new HashMap<>();
        this.results = new HashMap<>();
        this.performanceMetrics = new HashMap<>();
        this.warnings = new ArrayList<>();
        this.tags = new HashMap<>();
        this.successful = false;
        this.executedActions = 0;
        this.failedActions = 0;
        this.skippedActions = 0;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private RuleExecutionHistoryEntry entry = new RuleExecutionHistoryEntry();
        
        public Builder executionId(String executionId) {
            entry.executionId = executionId;
            return this;
        }
        
        public Builder ruleId(String ruleId) {
            entry.ruleId = ruleId;
            return this;
        }
        
        public Builder ruleName(String ruleName) {
            entry.ruleName = ruleName;
            return this;
        }
        
        public Builder ruleVersion(String ruleVersion) {
            entry.ruleVersion = ruleVersion;
            return this;
        }
        
        public Builder userId(String userId) {
            entry.userId = userId;
            return this;
        }
        
        public Builder executedAt(LocalDateTime executedAt) {
            entry.executedAt = executedAt;
            return this;
        }
        
        public Builder executionTimeMs(long executionTimeMs) {
            entry.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public Builder status(RuleExecutionResult.ExecutionStatus status) {
            entry.status = status;
            return this;
        }
        
        public Builder successful(boolean successful) {
            entry.successful = successful;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            entry.errorMessage = errorMessage;
            return this;
        }
        
        public Builder errorType(String errorType) {
            entry.errorType = errorType;
            return this;
        }
        
        public Builder executedActions(int executedActions) {
            entry.executedActions = executedActions;
            return this;
        }
        
        public Builder failedActions(int failedActions) {
            entry.failedActions = failedActions;
            return this;
        }
        
        public Builder skippedActions(int skippedActions) {
            entry.skippedActions = skippedActions;
            return this;
        }
        
        public Builder actionTypes(List<String> actionTypes) {
            entry.actionTypes = new ArrayList<>(actionTypes);
            return this;
        }
        
        public Builder addActionType(String actionType) {
            entry.actionTypes.add(actionType);
            return this;
        }
        
        public Builder executionContext(Map<String, Object> executionContext) {
            entry.executionContext = new HashMap<>(executionContext);
            return this;
        }
        
        public Builder addContextValue(String key, Object value) {
            entry.executionContext.put(key, value);
            return this;
        }
        
        public Builder results(Map<String, Object> results) {
            entry.results = new HashMap<>(results);
            return this;
        }
        
        public Builder addResult(String key, Object value) {
            entry.results.put(key, value);
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            entry.sessionId = sessionId;
            return this;
        }
        
        public Builder executionEnvironment(String executionEnvironment) {
            entry.executionEnvironment = executionEnvironment;
            return this;
        }
        
        public Builder executionEnvironment(ExecutionEnvironment executionEnvironment) {
            entry.executionEnvironment = executionEnvironment.toString();
            return this;
        }
        
        public Builder triggerType(String triggerType) {
            entry.triggerType = triggerType;
            return this;
        }
        
        public Builder triggerType(TriggerType triggerType) {
            entry.triggerType = triggerType.toString();
            return this;
        }
        
        public Builder triggerSource(String triggerSource) {
            entry.triggerSource = triggerSource;
            return this;
        }
        
        public Builder performanceMetrics(Map<String, Object> performanceMetrics) {
            entry.performanceMetrics = new HashMap<>(performanceMetrics);
            return this;
        }
        
        public Builder addPerformanceMetric(String key, Object value) {
            entry.performanceMetrics.put(key, value);
            return this;
        }
        
        public Builder warnings(List<String> warnings) {
            entry.warnings = new ArrayList<>(warnings);
            return this;
        }
        
        public Builder addWarning(String warning) {
            entry.warnings.add(warning);
            return this;
        }
        
        public Builder tags(Map<String, String> tags) {
            entry.tags = new HashMap<>(tags);
            return this;
        }
        
        public Builder addTag(String key, String value) {
            entry.tags.put(key, value);
            return this;
        }
        
        public RuleExecutionHistoryEntry build() {
            if (entry.executionId == null || entry.executionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Execution ID is required");
            }
            if (entry.ruleId == null || entry.ruleId.trim().isEmpty()) {
                throw new IllegalArgumentException("Rule ID is required");
            }
            if (entry.userId == null || entry.userId.trim().isEmpty()) {
                throw new IllegalArgumentException("User ID is required");
            }
            if (entry.executedAt == null) {
                entry.executedAt = LocalDateTime.now();
            }
            return entry;
        }
        
        // Factory methods for common scenarios
        public Builder fromExecutionResult(RuleExecutionResult result) {
            if (result != null) {
                entry.executionId = result.getExecutionId();
                entry.ruleId = result.getRule() != null ? result.getRule().getId() : null;
                entry.ruleName = result.getRule() != null ? result.getRule().getName() : null;
                entry.ruleVersion = result.getRule() != null ? result.getRule().getVersion() : null;
                entry.userId = result.getUserId();
                entry.executedAt = result.getStartTime();
                entry.executionTimeMs = result.getDuration() != null ? result.getDuration().toMillis() : 0;
                entry.status = result.getStatus();
                entry.successful = result.getStatus() == RuleExecutionResult.ExecutionStatus.SUCCESS;
                entry.errorMessage = result.getErrorMessage();
                entry.sessionId = result.getSessionId();
                
                if (result.getExecutedActions() != null) {
                    entry.executedActions = result.getExecutedActions().size();
                    entry.actionTypes = result.getExecutedActions().stream()
                        .map(RuleAction::getActionType)
                        .distinct()
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                }
                
                if (result.getFailedActions() != null) {
                    entry.failedActions = result.getFailedActions().size();
                }
                
                if (result.getSkippedActions() != null) {
                    entry.skippedActions = result.getSkippedActions().size();
                }
            }
            return this;
        }
    }
    
    // Getters and Setters
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public String getRuleId() {
        return ruleId;
    }
    
    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
    
    public String getRuleVersion() {
        return ruleVersion;
    }
    
    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
    
    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public Duration getExecutionDuration() {
        return Duration.ofMillis(executionTimeMs);
    }
    
    public RuleExecutionResult.ExecutionStatus getStatus() {
        return status;
    }
    
    public void setStatus(RuleExecutionResult.ExecutionStatus status) {
        this.status = status;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
    
    public int getExecutedActions() {
        return executedActions;
    }
    
    public void setExecutedActions(int executedActions) {
        this.executedActions = executedActions;
    }
    
    public int getFailedActions() {
        return failedActions;
    }
    
    public void setFailedActions(int failedActions) {
        this.failedActions = failedActions;
    }
    
    public int getSkippedActions() {
        return skippedActions;
    }
    
    public void setSkippedActions(int skippedActions) {
        this.skippedActions = skippedActions;
    }
    
    public int getTotalActions() {
        return executedActions + failedActions + skippedActions;
    }
    
    public List<String> getActionTypes() {
        return new ArrayList<>(actionTypes);
    }
    
    public void setActionTypes(List<String> actionTypes) {
        this.actionTypes = new ArrayList<>(actionTypes);
    }
    
    public void addActionType(String actionType) {
        if (!this.actionTypes.contains(actionType)) {
            this.actionTypes.add(actionType);
        }
    }
    
    public Map<String, Object> getExecutionContext() {
        return new HashMap<>(executionContext);
    }
    
    public void setExecutionContext(Map<String, Object> executionContext) {
        this.executionContext = new HashMap<>(executionContext);
    }
    
    public Object getContextValue(String key) {
        return executionContext.get(key);
    }
    
    public void addContextValue(String key, Object value) {
        this.executionContext.put(key, value);
    }
    
    public Map<String, Object> getResults() {
        return new HashMap<>(results);
    }
    
    public void setResults(Map<String, Object> results) {
        this.results = new HashMap<>(results);
    }
    
    public Object getResult(String key) {
        return results.get(key);
    }
    
    public void addResult(String key, Object value) {
        this.results.put(key, value);
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getExecutionEnvironment() {
        return executionEnvironment;
    }
    
    public void setExecutionEnvironment(String executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }
    
    public String getTriggerType() {
        return triggerType;
    }
    
    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }
    
    public String getTriggerSource() {
        return triggerSource;
    }
    
    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }
    
    public Map<String, Object> getPerformanceMetrics() {
        return new HashMap<>(performanceMetrics);
    }
    
    public void setPerformanceMetrics(Map<String, Object> performanceMetrics) {
        this.performanceMetrics = new HashMap<>(performanceMetrics);
    }
    
    public Object getPerformanceMetric(String key) {
        return performanceMetrics.get(key);
    }
    
    public void addPerformanceMetric(String key, Object value) {
        this.performanceMetrics.put(key, value);
    }
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = new ArrayList<>(warnings);
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    public Map<String, String> getTags() {
        return new HashMap<>(tags);
    }
    
    public void setTags(Map<String, String> tags) {
        this.tags = new HashMap<>(tags);
    }
    
    public String getTag(String key) {
        return tags.get(key);
    }
    
    public void addTag(String key, String value) {
        this.tags.put(key, value);
    }
    
    public boolean hasTag(String key) {
        return tags.containsKey(key);
    }
    
    // Utility methods
    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }
    
    public boolean wasSkipped() {
        return status == RuleExecutionResult.ExecutionStatus.SKIPPED;
    }
    
    public boolean hadPartialSuccess() {
        return status == RuleExecutionResult.ExecutionStatus.PARTIAL_SUCCESS;
    }
    
    public double getSuccessRate() {
        int total = getTotalActions();
        return total > 0 ? (double) executedActions / total : 0.0;
    }
    
    public double getFailureRate() {
        int total = getTotalActions();
        return total > 0 ? (double) failedActions / total : 0.0;
    }
    
    public boolean isLongRunning(long thresholdMs) {
        return executionTimeMs > thresholdMs;
    }
    
    public boolean isRecentExecution(Duration timeWindow) {
        return executedAt != null && executedAt.isAfter(LocalDateTime.now().minus(timeWindow));
    }
    
    // Common factory methods
    public static RuleExecutionHistoryEntry fromResult(RuleExecutionResult result) {
        return builder()
            .fromExecutionResult(result)
            .build();
    }
    
    public static RuleExecutionHistoryEntry success(String executionId, String ruleId, String userId, long executionTimeMs) {
        return builder()
            .executionId(executionId)
            .ruleId(ruleId)
            .userId(userId)
            .executionTimeMs(executionTimeMs)
            .status(RuleExecutionResult.ExecutionStatus.SUCCESS)
            .successful(true)
            .build();
    }
    
    public static RuleExecutionHistoryEntry failure(String executionId, String ruleId, String userId, String errorMessage) {
        return builder()
            .executionId(executionId)
            .ruleId(ruleId)
            .userId(userId)
            .status(RuleExecutionResult.ExecutionStatus.FAILURE)
            .successful(false)
            .errorMessage(errorMessage)
            .build();
    }
    
    @Override
    public String toString() {
        return "RuleExecutionHistoryEntry{" +
                "executionId='" + executionId + '\'' +
                ", ruleId='" + ruleId + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", userId='" + userId + '\'' +
                ", executedAt=" + executedAt +
                ", executionTimeMs=" + executionTimeMs +
                ", status=" + status +
                ", successful=" + successful +
                ", executedActions=" + executedActions +
                ", failedActions=" + failedActions +
                ", skippedActions=" + skippedActions +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        RuleExecutionHistoryEntry that = (RuleExecutionHistoryEntry) o;
        
        if (!executionId.equals(that.executionId)) return false;
        if (!ruleId.equals(that.ruleId)) return false;
        return userId.equals(that.userId);
    }
    
    @Override
    public int hashCode() {
        int result = executionId.hashCode();
        result = 31 * result + ruleId.hashCode();
        result = 31 * result + userId.hashCode();
        return result;
    }
}