package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a validation issue found during business rule validation.
 * Provides detailed information about the issue including type, severity,
 * location, and suggested fixes.
 * 
 * This class is part of the BusinessRuleValidator system (Phase 26.2c)
 * which implements comprehensive rule validation with detailed issue reporting.
 * 
 * Patent Alignment: Implements validation issue representation that enables
 * detailed feedback and actionable suggestions for business rule improvements.
 * 
 * @author Obvian Labs
 * @since Phase 26.2c
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationIssue {
    
    /**
     * Types of validation issues
     */
    public enum IssueType {
        // Structural Issues
        MISSING_REQUIRED_FIELD,
        INVALID_FIELD_VALUE,
        STRUCTURAL_ERROR,
        MALFORMED_CONDITION,
        INCOMPLETE_ACTION,
        INVALID_SCHEDULE,
        
        // Logical Issues
        LOGICAL_INCONSISTENCY,
        UNREACHABLE_CONDITION,
        CONTRADICTORY_ACTIONS,
        CIRCULAR_DEPENDENCY,
        MISSING_DEPENDENCY,
        
        // Performance Issues
        PERFORMANCE_WARNING,
        UNREALISTIC_TIMEOUT,
        COMPLEX_CONDITION,
        INEFFICIENT_ACTION,
        RESOURCE_INTENSIVE,
        
        // Conflict Issues
        RULE_CONFLICT,
        PRIORITY_CONFLICT,
        TEMPORAL_OVERLAP,
        RESOURCE_CONTENTION,
        
        // Data Issues
        INVALID_DATA_TYPE,
        MISSING_FIELD_REFERENCE,
        UNKNOWN_ENTITY,
        INVALID_PARAMETER,
        
        // Security Issues
        SECURITY_RISK,
        UNAUTHORIZED_ACTION,
        DATA_EXPOSURE,
        
        // Other Issues
        VALIDATION_TIMEOUT,
        SIMULATION_FAILURE,
        CONFIGURATION_ERROR,
        DEPRECATED_FEATURE,
        BEST_PRACTICE_VIOLATION
    }
    
    /**
     * Severity levels for validation issues
     */
    public enum Severity {
        CRITICAL,   // Rule cannot be executed safely
        ERROR,      // Error conditions that prevent execution
        HIGH,       // Significant issues that should be addressed
        WARNING,    // Warning conditions that may cause issues
        MEDIUM,     // Moderate issues that may cause problems
        LOW,        // Minor issues or suggestions
        INFO        // Informational messages
    }
    
    @JsonProperty("type")
    private final IssueType type;
    
    @JsonProperty("severity")
    private final Severity severity;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("location")
    private final String location;
    
    @JsonProperty("fieldPath")
    private final String fieldPath;
    
    @JsonProperty("suggestedFix")
    private final String suggestedFix;
    
    @JsonProperty("code")
    private final String code;
    
    @JsonProperty("detectedAt")
    private final LocalDateTime detectedAt;
    
    @JsonProperty("context")
    private final Map<String, Object> context;
    
    @JsonProperty("relatedIssues")
    private final List<String> relatedIssues;
    
    @JsonProperty("confidence")
    private final double confidence;
    
    @JsonCreator
    private ValidationIssue(
            @JsonProperty("type") IssueType type,
            @JsonProperty("severity") Severity severity,
            @JsonProperty("description") String description,
            @JsonProperty("location") String location,
            @JsonProperty("fieldPath") String fieldPath,
            @JsonProperty("suggestedFix") String suggestedFix,
            @JsonProperty("code") String code,
            @JsonProperty("detectedAt") LocalDateTime detectedAt,
            @JsonProperty("context") Map<String, Object> context,
            @JsonProperty("relatedIssues") List<String> relatedIssues,
            @JsonProperty("confidence") double confidence) {
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.location = location;
        this.fieldPath = fieldPath;
        this.suggestedFix = suggestedFix;
        this.code = code;
        this.detectedAt = detectedAt != null ? detectedAt : LocalDateTime.now();
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
        this.relatedIssues = relatedIssues != null ? new ArrayList<>(relatedIssues) : new ArrayList<>();
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
    }
    
    // Getters
    public IssueType getType() { return type; }
    public Severity getSeverity() { return severity; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getFieldPath() { return fieldPath; }
    public String getSuggestedFix() { return suggestedFix; }
    public String getCode() { return code; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public Map<String, Object> getContext() { return new HashMap<>(context); }
    public List<String> getRelatedIssues() { return new ArrayList<>(relatedIssues); }
    public double getConfidence() { return confidence; }
    public String getMessage() { return description; } // Alias for description
    
    /**
     * Check if this is a critical issue
     */
    public boolean isCritical() {
        return severity == Severity.CRITICAL;
    }
    
    /**
     * Check if this issue blocks rule execution
     */
    public boolean isBlocking() {
        return severity == Severity.CRITICAL || 
               severity == Severity.ERROR ||
               (severity == Severity.HIGH && isStructuralIssue());
    }
    
    /**
     * Check if this is a structural issue
     */
    public boolean isStructuralIssue() {
        return type == IssueType.MISSING_REQUIRED_FIELD ||
               type == IssueType.STRUCTURAL_ERROR ||
               type == IssueType.MALFORMED_CONDITION ||
               type == IssueType.INCOMPLETE_ACTION ||
               type == IssueType.INVALID_SCHEDULE;
    }
    
    /**
     * Check if this is a performance issue
     */
    public boolean isPerformanceIssue() {
        return type == IssueType.PERFORMANCE_WARNING ||
               type == IssueType.UNREALISTIC_TIMEOUT ||
               type == IssueType.COMPLEX_CONDITION ||
               type == IssueType.INEFFICIENT_ACTION ||
               type == IssueType.RESOURCE_INTENSIVE;
    }
    
    /**
     * Check if this is a security issue
     */
    public boolean isSecurityIssue() {
        return type == IssueType.SECURITY_RISK ||
               type == IssueType.UNAUTHORIZED_ACTION ||
               type == IssueType.DATA_EXPOSURE;
    }
    
    /**
     * Get severity level as numeric score (higher = more severe)
     */
    public int getSeverityScore() {
        return switch (severity) {
            case CRITICAL -> 6;
            case ERROR -> 5;
            case HIGH -> 4;
            case WARNING -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
            case INFO -> 0;
        };
    }
    
    /**
     * Get formatted issue message
     */
    public String getFormattedMessage() {
        StringBuilder message = new StringBuilder();
        message.append("[").append(severity).append("] ");
        message.append(type.toString().replace("_", " "));
        
        if (location != null && !location.isEmpty()) {
            message.append(" at ").append(location);
        }
        
        message.append(": ").append(description);
        
        if (suggestedFix != null && !suggestedFix.isEmpty()) {
            message.append(" (Suggested fix: ").append(suggestedFix).append(")");
        }
        
        return message.toString();
    }
    
    /**
     * Builder pattern for creating ValidationIssue instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private IssueType type;
        private Severity severity = Severity.MEDIUM;
        private String description;
        private String location;
        private String fieldPath;
        private String suggestedFix;
        private String code;
        private LocalDateTime detectedAt;
        private Map<String, Object> context = new HashMap<>();
        private List<String> relatedIssues = new ArrayList<>();
        private double confidence = 1.0;
        
        public Builder type(IssueType type) {
            this.type = type;
            return this;
        }
        
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        public Builder fieldPath(String fieldPath) {
            this.fieldPath = fieldPath;
            return this;
        }
        
        public Builder suggestedFix(String suggestedFix) {
            this.suggestedFix = suggestedFix;
            return this;
        }
        
        public Builder code(String code) {
            this.code = code;
            return this;
        }
        
        public Builder detectedAt(LocalDateTime detectedAt) {
            this.detectedAt = detectedAt;
            return this;
        }
        
        public Builder addContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }
        
        public Builder context(Map<String, Object> context) {
            this.context = new HashMap<>(context);
            return this;
        }
        
        public Builder addRelatedIssue(String issueCode) {
            this.relatedIssues.add(issueCode);
            return this;
        }
        
        public Builder relatedIssues(List<String> relatedIssues) {
            this.relatedIssues = new ArrayList<>(relatedIssues);
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public ValidationIssue build() {
            validate();
            return new ValidationIssue(type, severity, description, location, fieldPath,
                    suggestedFix, code, detectedAt, context, relatedIssues, confidence);
        }
        
        private void validate() {
            if (type == null) {
                throw new IllegalArgumentException("Issue type is required");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Issue description is required");
            }
            if (severity == null) {
                throw new IllegalArgumentException("Issue severity is required");
            }
        }
    }
    
    /**
     * Predefined issue creators for common validation problems
     */
    public static class Common {
        
        public static ValidationIssue missingRequiredField(String fieldName) {
            return builder()
                    .type(IssueType.MISSING_REQUIRED_FIELD)
                    .severity(Severity.CRITICAL)
                    .description("Required field '" + fieldName + "' is missing")
                    .fieldPath(fieldName)
                    .suggestedFix("Add the required field: " + fieldName)
                    .code("MISSING_FIELD_" + fieldName.toUpperCase())
                    .build();
        }
        
        public static ValidationIssue invalidSchedule(String schedule) {
            return builder()
                    .type(IssueType.INVALID_SCHEDULE)
                    .severity(Severity.HIGH)
                    .description("Invalid cron expression: " + schedule)
                    .location("schedule")
                    .suggestedFix("Use valid cron expression format (e.g., '0 0 9 * * MON-FRI')")
                    .code("INVALID_CRON")
                    .addContext("invalidSchedule", schedule)
                    .build();
        }
        
        public static ValidationIssue performanceWarning(String component, long estimatedTimeMs) {
            return builder()
                    .type(IssueType.PERFORMANCE_WARNING)
                    .severity(Severity.MEDIUM)
                    .description("Component '" + component + "' may have performance issues")
                    .location(component)
                    .suggestedFix("Consider optimizing or simplifying this component")
                    .code("PERFORMANCE_WARNING")
                    .addContext("estimatedExecutionTimeMs", estimatedTimeMs)
                    .build();
        }
        
        public static ValidationIssue ruleConflict(String rule1Id, String rule2Id) {
            return builder()
                    .type(IssueType.RULE_CONFLICT)
                    .severity(Severity.HIGH)
                    .description("Rule conflict detected between '" + rule1Id + "' and '" + rule2Id + "'")
                    .suggestedFix("Review and resolve conflicting rules by adjusting conditions or priorities")
                    .code("RULE_CONFLICT")
                    .addContext("conflictingRule1", rule1Id)
                    .addContext("conflictingRule2", rule2Id)
                    .build();
        }
        
        public static ValidationIssue circularDependency(List<String> ruleIds) {
            return builder()
                    .type(IssueType.CIRCULAR_DEPENDENCY)
                    .severity(Severity.CRITICAL)
                    .description("Circular dependency detected in rules: " + String.join(" -> ", ruleIds))
                    .suggestedFix("Remove circular dependencies by adjusting rule dependencies")
                    .code("CIRCULAR_DEPENDENCY")
                    .addContext("circularPath", ruleIds)
                    .build();
        }
        
        public static ValidationIssue missingDependency(String ruleId, String missingDependency) {
            return builder()
                    .type(IssueType.MISSING_DEPENDENCY)
                    .severity(Severity.HIGH)
                    .description("Rule '" + ruleId + "' depends on non-existent rule '" + missingDependency + "'")
                    .suggestedFix("Create the missing dependency rule or remove the dependency reference")
                    .code("MISSING_DEPENDENCY")
                    .addContext("ruleId", ruleId)
                    .addContext("missingDependency", missingDependency)
                    .build();
        }
        
        public static ValidationIssue unrealisticTimeout(long timeoutMs) {
            return builder()
                    .type(IssueType.UNREALISTIC_TIMEOUT)
                    .severity(Severity.HIGH)
                    .description("Timeout value of " + timeoutMs + "ms may be too short for reliable execution")
                    .location("timeout")
                    .suggestedFix("Consider increasing timeout to at least 1000ms")
                    .code("UNREALISTIC_TIMEOUT")
                    .addContext("timeoutMs", timeoutMs)
                    .build();
        }
        
        public static ValidationIssue incompleteAction(String actionType, List<String> missingParams) {
            return builder()
                    .type(IssueType.INCOMPLETE_ACTION)
                    .severity(Severity.HIGH)
                    .description("Action '" + actionType + "' is missing required parameters: " + 
                               String.join(", ", missingParams))
                    .location("actions[" + actionType + "]")
                    .suggestedFix("Add the missing parameters: " + String.join(", ", missingParams))
                    .code("INCOMPLETE_ACTION")
                    .addContext("actionType", actionType)
                    .addContext("missingParameters", missingParams)
                    .build();
        }
        
        public static ValidationIssue validationTimeout(long timeoutMs) {
            return builder()
                    .type(IssueType.VALIDATION_TIMEOUT)
                    .severity(Severity.HIGH)
                    .description("Validation timed out after " + timeoutMs + "ms")
                    .suggestedFix("Simplify rule complexity or increase validation timeout")
                    .code("VALIDATION_TIMEOUT")
                    .addContext("timeoutMs", timeoutMs)
                    .build();
        }
        
        public static ValidationIssue simulationFailure(String error) {
            return builder()
                    .type(IssueType.SIMULATION_FAILURE)
                    .severity(Severity.MEDIUM)
                    .description("Rule simulation failed: " + error)
                    .suggestedFix("Review rule logic and test data for compatibility")
                    .code("SIMULATION_FAILURE")
                    .addContext("simulationError", error)
                    .build();
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationIssue that = (ValidationIssue) o;
        return Double.compare(that.confidence, confidence) == 0 &&
               type == that.type &&
               severity == that.severity &&
               Objects.equals(description, that.description) &&
               Objects.equals(location, that.location) &&
               Objects.equals(fieldPath, that.fieldPath) &&
               Objects.equals(code, that.code);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, severity, description, location, fieldPath, code, confidence);
    }
    
    @Override
    public String toString() {
        return "ValidationIssue{" +
                "type=" + type +
                ", severity=" + severity +
                ", description='" + description + '\'' +
                ", location='" + location + '\'' +
                ", code='" + code + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}