package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a conflict between business rules including detailed analysis
 * of the conflict nature, severity, and resolution suggestions.
 * 
 * This class is part of the BusinessRuleValidator system (Phase 26.2c)
 * which implements comprehensive conflict detection between business rules
 * with sophisticated analysis and resolution recommendations.
 * 
 * Patent Alignment: Implements rule conflict detection and representation
 * that enables intelligent business rule conflict resolution with detailed
 * analysis and automated suggestion generation.
 * 
 * @author Obvian Labs
 * @since Phase 26.2c
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleConflict {
    
    /**
     * Types of rule conflicts
     */
    public enum ConflictType {
        DIRECT_CONFLICT,        // Rules have contradictory actions for same conditions
        CONDITIONAL_CONFLICT,   // Rules may conflict under certain conditions
        TEMPORAL_CONFLICT,      // Time-based conflicts between scheduled rules
        PRIORITY_CONFLICT,      // Rules with same priority that could interfere
        RESOURCE_CONFLICT,      // Rules competing for same resources
        DEPENDENCY_CONFLICT,    // Conflicts in rule dependency chains
        SEMANTIC_CONFLICT,      // Semantic conflicts in business logic
        DATA_CONFLICT,          // Conflicts in data access or modification
        EXECUTION_CONFLICT,     // Conflicts in execution ordering or timing
        SCOPE_CONFLICT          // Conflicts in rule scope or applicability
    }
    
    /**
     * Severity levels for conflicts
     */
    public enum ConflictSeverity {
        CRITICAL,   // Complete contradiction - rules cannot coexist
        HIGH,       // Significant conflict requiring immediate attention
        MEDIUM,     // Moderate conflict that should be addressed
        LOW,        // Minor conflict with potential issues
        INFO        // Informational - rules overlap but may work together
    }
    
    /**
     * Resolution strategies for conflicts
     */
    public enum ResolutionStrategy {
        MODIFY_CONDITIONS,      // Adjust rule conditions to avoid conflict
        ADJUST_PRIORITIES,      // Change rule priorities to resolve conflicts
        MERGE_RULES,           // Combine conflicting rules into one
        SPLIT_RULES,           // Split rules to separate conflicting logic
        ADD_DEPENDENCIES,      // Add dependencies to control execution order
        TEMPORAL_SEPARATION,   // Separate rules in time to avoid conflicts
        SCOPE_REFINEMENT,      // Refine rule scopes to avoid overlap
        CONDITIONAL_LOGIC,     // Add conditional logic to handle conflicts
        FALLBACK_MECHANISM,    // Implement fallback for conflict resolution
        MANUAL_REVIEW          // Requires manual review and resolution
    }
    
    @JsonProperty("conflictId")
    private final String conflictId;
    
    @JsonProperty("type")
    private final ConflictType type;
    
    @JsonProperty("severity")
    private final ConflictSeverity severity;
    
    @JsonProperty("conflictingRules")
    private final List<BusinessRule> conflictingRules;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("conflictDetails")
    private final String conflictDetails;
    
    @JsonProperty("conflictContext")
    private final Map<String, Object> conflictContext;
    
    @JsonProperty("detectedAt")
    private final LocalDateTime detectedAt;
    
    @JsonProperty("confidence")
    private final double confidence;
    
    @JsonProperty("resolutionStrategies")
    private final List<ResolutionStrategy> resolutionStrategies;
    
    @JsonProperty("resolutionSuggestions")
    private final List<String> resolutionSuggestions;
    
    @JsonProperty("impactAnalysis")
    private final ConflictImpactAnalysis impactAnalysis;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    /**
     * Analysis of conflict impact on rule execution
     */
    public static class ConflictImpactAnalysis {
        @JsonProperty("affectedRules")
        private final List<String> affectedRules;
        
        @JsonProperty("potentialFailures")
        private final List<String> potentialFailures;
        
        @JsonProperty("performanceImpact")
        private final String performanceImpact;
        
        @JsonProperty("dataConsistencyRisk")
        private final boolean dataConsistencyRisk;
        
        @JsonProperty("businessImpact")
        private final String businessImpact;
        
        @JsonProperty("riskScore")
        private final double riskScore;
        
        @JsonCreator
        public ConflictImpactAnalysis(
                @JsonProperty("affectedRules") List<String> affectedRules,
                @JsonProperty("potentialFailures") List<String> potentialFailures,
                @JsonProperty("performanceImpact") String performanceImpact,
                @JsonProperty("dataConsistencyRisk") boolean dataConsistencyRisk,
                @JsonProperty("businessImpact") String businessImpact,
                @JsonProperty("riskScore") double riskScore) {
            this.affectedRules = affectedRules != null ? new ArrayList<>(affectedRules) : new ArrayList<>();
            this.potentialFailures = potentialFailures != null ? new ArrayList<>(potentialFailures) : new ArrayList<>();
            this.performanceImpact = performanceImpact;
            this.dataConsistencyRisk = dataConsistencyRisk;
            this.businessImpact = businessImpact;
            this.riskScore = Math.max(0.0, Math.min(1.0, riskScore));
        }
        
        public List<String> getAffectedRules() { return new ArrayList<>(affectedRules); }
        public List<String> getPotentialFailures() { return new ArrayList<>(potentialFailures); }
        public String getPerformanceImpact() { return performanceImpact; }
        public boolean isDataConsistencyRisk() { return dataConsistencyRisk; }
        public String getBusinessImpact() { return businessImpact; }
        public double getRiskScore() { return riskScore; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private List<String> affectedRules = new ArrayList<>();
            private List<String> potentialFailures = new ArrayList<>();
            private String performanceImpact;
            private boolean dataConsistencyRisk = false;
            private String businessImpact;
            private double riskScore = 0.0;
            
            public Builder addAffectedRule(String ruleId) {
                this.affectedRules.add(ruleId);
                return this;
            }
            
            public Builder addPotentialFailure(String failure) {
                this.potentialFailures.add(failure);
                return this;
            }
            
            public Builder performanceImpact(String impact) {
                this.performanceImpact = impact;
                return this;
            }
            
            public Builder dataConsistencyRisk(boolean risk) {
                this.dataConsistencyRisk = risk;
                return this;
            }
            
            public Builder businessImpact(String impact) {
                this.businessImpact = impact;
                return this;
            }
            
            public Builder riskScore(double score) {
                this.riskScore = score;
                return this;
            }
            
            public ConflictImpactAnalysis build() {
                return new ConflictImpactAnalysis(affectedRules, potentialFailures,
                        performanceImpact, dataConsistencyRisk, businessImpact, riskScore);
            }
        }
    }
    
    @JsonCreator
    private RuleConflict(
            @JsonProperty("conflictId") String conflictId,
            @JsonProperty("type") ConflictType type,
            @JsonProperty("severity") ConflictSeverity severity,
            @JsonProperty("conflictingRules") List<BusinessRule> conflictingRules,
            @JsonProperty("description") String description,
            @JsonProperty("conflictDetails") String conflictDetails,
            @JsonProperty("conflictContext") Map<String, Object> conflictContext,
            @JsonProperty("detectedAt") LocalDateTime detectedAt,
            @JsonProperty("confidence") double confidence,
            @JsonProperty("resolutionStrategies") List<ResolutionStrategy> resolutionStrategies,
            @JsonProperty("resolutionSuggestions") List<String> resolutionSuggestions,
            @JsonProperty("impactAnalysis") ConflictImpactAnalysis impactAnalysis,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.conflictId = conflictId != null ? conflictId : UUID.randomUUID().toString();
        this.type = type;
        this.severity = severity;
        this.conflictingRules = conflictingRules != null ? new ArrayList<>(conflictingRules) : new ArrayList<>();
        this.description = description;
        this.conflictDetails = conflictDetails;
        this.conflictContext = conflictContext != null ? new HashMap<>(conflictContext) : new HashMap<>();
        this.detectedAt = detectedAt != null ? detectedAt : LocalDateTime.now();
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.resolutionStrategies = resolutionStrategies != null ? new ArrayList<>(resolutionStrategies) : new ArrayList<>();
        this.resolutionSuggestions = resolutionSuggestions != null ? new ArrayList<>(resolutionSuggestions) : new ArrayList<>();
        this.impactAnalysis = impactAnalysis;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    // Getters
    public String getConflictId() { return conflictId; }
    public ConflictType getType() { return type; }
    public ConflictSeverity getSeverity() { return severity; }
    public List<BusinessRule> getConflictingRules() { return new ArrayList<>(conflictingRules); }
    public String getDescription() { return description; }
    public String getConflictDetails() { return conflictDetails; }
    public Map<String, Object> getConflictContext() { return new HashMap<>(conflictContext); }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public double getConfidence() { return confidence; }
    public List<ResolutionStrategy> getResolutionStrategies() { return new ArrayList<>(resolutionStrategies); }
    public List<String> getResolutionSuggestions() { return new ArrayList<>(resolutionSuggestions); }
    public ConflictImpactAnalysis getImpactAnalysis() { return impactAnalysis; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * Check if this is a critical conflict
     */
    public boolean isCritical() {
        return severity == ConflictSeverity.CRITICAL;
    }
    
    /**
     * Check if this conflict blocks rule execution
     */
    public boolean isBlocking() {
        return severity == ConflictSeverity.CRITICAL || severity == ConflictSeverity.HIGH;
    }
    
    /**
     * Get the primary conflicting rule (first in list)
     */
    public BusinessRule getPrimaryRule() {
        return conflictingRules.isEmpty() ? null : conflictingRules.get(0);
    }
    
    /**
     * Get the secondary conflicting rule (second in list)
     */
    public BusinessRule getSecondaryRule() {
        return conflictingRules.size() < 2 ? null : conflictingRules.get(1);
    }
    
    /**
     * Get all rule IDs involved in the conflict
     */
    public List<String> getConflictingRuleIds() {
        return conflictingRules.stream()
                .map(BusinessRule::getId)
                .toList();
    }
    
    /**
     * Get severity as numeric score (higher = more severe)
     */
    public int getSeverityScore() {
        return switch (severity) {
            case CRITICAL -> 5;
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case INFO -> 1;
        };
    }
    
    /**
     * Check if conflict involves temporal scheduling
     */
    public boolean isTemporalConflict() {
        return type == ConflictType.TEMPORAL_CONFLICT ||
               type == ConflictType.EXECUTION_CONFLICT;
    }
    
    /**
     * Check if conflict involves data access
     */
    public boolean isDataConflict() {
        return type == ConflictType.DATA_CONFLICT ||
               type == ConflictType.RESOURCE_CONFLICT;
    }
    
    /**
     * Get formatted conflict summary
     */
    public String getFormattedSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("[").append(severity).append("] ");
        summary.append(type.toString().replace("_", " "));
        summary.append(": ").append(description);
        
        if (!conflictingRules.isEmpty()) {
            summary.append(" (Rules: ");
            summary.append(conflictingRules.stream()
                    .map(BusinessRule::getId)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
            summary.append(")");
        }
        
        return summary.toString();
    }
    
    /**
     * Builder pattern for creating RuleConflict instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String conflictId;
        private ConflictType type;
        private ConflictSeverity severity = ConflictSeverity.MEDIUM;
        private List<BusinessRule> conflictingRules = new ArrayList<>();
        private String description;
        private String conflictDetails;
        private Map<String, Object> conflictContext = new HashMap<>();
        private LocalDateTime detectedAt;
        private double confidence = 1.0;
        private List<ResolutionStrategy> resolutionStrategies = new ArrayList<>();
        private List<String> resolutionSuggestions = new ArrayList<>();
        private ConflictImpactAnalysis impactAnalysis;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder conflictId(String conflictId) {
            this.conflictId = conflictId;
            return this;
        }
        
        public Builder type(ConflictType type) {
            this.type = type;
            return this;
        }
        
        public Builder severity(ConflictSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder addConflictingRule(BusinessRule rule) {
            this.conflictingRules.add(rule);
            return this;
        }
        
        public Builder conflictingRules(List<BusinessRule> rules) {
            this.conflictingRules = new ArrayList<>(rules);
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder conflictDetails(String details) {
            this.conflictDetails = details;
            return this;
        }
        
        public Builder addConflictContext(String key, Object value) {
            this.conflictContext.put(key, value);
            return this;
        }
        
        public Builder conflictContext(Map<String, Object> context) {
            this.conflictContext = new HashMap<>(context);
            return this;
        }
        
        public Builder detectedAt(LocalDateTime detectedAt) {
            this.detectedAt = detectedAt;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder addResolutionStrategy(ResolutionStrategy strategy) {
            this.resolutionStrategies.add(strategy);
            return this;
        }
        
        public Builder resolutionStrategies(List<ResolutionStrategy> strategies) {
            this.resolutionStrategies = new ArrayList<>(strategies);
            return this;
        }
        
        public Builder addResolutionSuggestion(String suggestion) {
            this.resolutionSuggestions.add(suggestion);
            return this;
        }
        
        public Builder resolutionSuggestions(List<String> suggestions) {
            this.resolutionSuggestions = new ArrayList<>(suggestions);
            return this;
        }
        
        public Builder impactAnalysis(ConflictImpactAnalysis analysis) {
            this.impactAnalysis = analysis;
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
        
        public RuleConflict build() {
            validate();
            generateDefaultStrategiesAndSuggestions();
            return new RuleConflict(conflictId, type, severity, conflictingRules,
                    description, conflictDetails, conflictContext, detectedAt,
                    confidence, resolutionStrategies, resolutionSuggestions,
                    impactAnalysis, metadata);
        }
        
        private void validate() {
            if (type == null) {
                throw new IllegalArgumentException("Conflict type is required");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Conflict description is required");
            }
            if (conflictingRules.size() < 2) {
                throw new IllegalArgumentException("At least two conflicting rules are required");
            }
        }
        
        private void generateDefaultStrategiesAndSuggestions() {
            if (resolutionStrategies.isEmpty()) {
                generateDefaultStrategies();
            }
            if (resolutionSuggestions.isEmpty()) {
                generateDefaultSuggestions();
            }
        }
        
        private void generateDefaultStrategies() {
            switch (type) {
                case DIRECT_CONFLICT:
                    resolutionStrategies.addAll(Arrays.asList(
                            ResolutionStrategy.MODIFY_CONDITIONS,
                            ResolutionStrategy.ADJUST_PRIORITIES,
                            ResolutionStrategy.MERGE_RULES));
                    break;
                case TEMPORAL_CONFLICT:
                    resolutionStrategies.addAll(Arrays.asList(
                            ResolutionStrategy.TEMPORAL_SEPARATION,
                            ResolutionStrategy.ADJUST_PRIORITIES));
                    break;
                case PRIORITY_CONFLICT:
                    resolutionStrategies.addAll(Arrays.asList(
                            ResolutionStrategy.ADJUST_PRIORITIES,
                            ResolutionStrategy.ADD_DEPENDENCIES));
                    break;
                case RESOURCE_CONFLICT:
                    resolutionStrategies.addAll(Arrays.asList(
                            ResolutionStrategy.TEMPORAL_SEPARATION,
                            ResolutionStrategy.SCOPE_REFINEMENT,
                            ResolutionStrategy.FALLBACK_MECHANISM));
                    break;
                case DEPENDENCY_CONFLICT:
                    resolutionStrategies.addAll(Arrays.asList(
                            ResolutionStrategy.ADD_DEPENDENCIES,
                            ResolutionStrategy.SPLIT_RULES));
                    break;
                default:
                    resolutionStrategies.add(ResolutionStrategy.MANUAL_REVIEW);
                    break;
            }
        }
        
        private void generateDefaultSuggestions() {
            String rule1Id = conflictingRules.get(0).getId();
            String rule2Id = conflictingRules.get(1).getId();
            
            switch (type) {
                case DIRECT_CONFLICT:
                    resolutionSuggestions.addAll(Arrays.asList(
                            "Review conditions in rules " + rule1Id + " and " + rule2Id + " to eliminate overlap",
                            "Consider merging the conflicting rules into a single rule with combined logic",
                            "Adjust rule priorities to ensure proper execution order"));
                    break;
                case TEMPORAL_CONFLICT:
                    resolutionSuggestions.addAll(Arrays.asList(
                            "Modify schedules to avoid temporal overlap",
                            "Stagger execution times to prevent conflicts",
                            "Consider using different time zones or execution windows"));
                    break;
                case PRIORITY_CONFLICT:
                    resolutionSuggestions.addAll(Arrays.asList(
                            "Assign different priorities to rules " + rule1Id + " and " + rule2Id,
                            "Add dependencies to control execution order",
                            "Consider business importance when setting priorities"));
                    break;
                case RESOURCE_CONFLICT:
                    resolutionSuggestions.addAll(Arrays.asList(
                            "Implement resource locking or queuing mechanisms",
                            "Separate resource access by time or conditions",
                            "Consider using different resources or fallback options"));
                    break;
                default:
                    resolutionSuggestions.add("Manual review required to resolve this conflict type");
                    break;
            }
        }
    }
    
    /**
     * Predefined conflict creators for common conflict scenarios
     */
    public static class Common {
        
        public static RuleConflict directConflict(BusinessRule rule1, BusinessRule rule2, String reason) {
            return builder()
                    .type(ConflictType.DIRECT_CONFLICT)
                    .severity(ConflictSeverity.HIGH)
                    .addConflictingRule(rule1)
                    .addConflictingRule(rule2)
                    .description("Direct conflict between rules: " + reason)
                    .conflictDetails("Rules have contradictory actions for the same conditions")
                    .addConflictContext("reason", reason)
                    .build();
        }
        
        public static RuleConflict temporalConflict(BusinessRule rule1, BusinessRule rule2) {
            return builder()
                    .type(ConflictType.TEMPORAL_CONFLICT)
                    .severity(ConflictSeverity.MEDIUM)
                    .addConflictingRule(rule1)
                    .addConflictingRule(rule2)
                    .description("Temporal conflict: rules have overlapping schedules")
                    .conflictDetails("Rules are scheduled to execute at the same time or overlapping periods")
                    .build();
        }
        
        public static RuleConflict priorityConflict(List<BusinessRule> rules, int priority) {
            return builder()
                    .type(ConflictType.PRIORITY_CONFLICT)
                    .severity(ConflictSeverity.MEDIUM)
                    .conflictingRules(rules)
                    .description("Priority conflict: multiple rules with same priority " + priority)
                    .conflictDetails("Rules with identical priority may have unpredictable execution order")
                    .addConflictContext("conflictingPriority", priority)
                    .build();
        }
        
        public static RuleConflict resourceConflict(BusinessRule rule1, BusinessRule rule2, String resource) {
            return builder()
                    .type(ConflictType.RESOURCE_CONFLICT)
                    .severity(ConflictSeverity.HIGH)
                    .addConflictingRule(rule1)
                    .addConflictingRule(rule2)
                    .description("Resource conflict: both rules access resource '" + resource + "'")
                    .conflictDetails("Rules may interfere with each other when accessing the same resource")
                    .addConflictContext("conflictingResource", resource)
                    .build();
        }
        
        public static RuleConflict dependencyConflict(BusinessRule rule1, BusinessRule rule2, String dependencyChain) {
            return builder()
                    .type(ConflictType.DEPENDENCY_CONFLICT)
                    .severity(ConflictSeverity.HIGH)
                    .addConflictingRule(rule1)
                    .addConflictingRule(rule2)
                    .description("Dependency conflict in chain: " + dependencyChain)
                    .conflictDetails("Rules have conflicting dependency requirements")
                    .addConflictContext("dependencyChain", dependencyChain)
                    .build();
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleConflict that = (RuleConflict) o;
        return Double.compare(that.confidence, confidence) == 0 &&
               Objects.equals(conflictId, that.conflictId) &&
               type == that.type &&
               severity == that.severity &&
               Objects.equals(conflictingRules, that.conflictingRules) &&
               Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(conflictId, type, severity, conflictingRules, description, confidence);
    }
    
    @Override
    public String toString() {
        return "RuleConflict{" +
                "conflictId='" + conflictId + '\'' +
                ", type=" + type +
                ", severity=" + severity +
                ", conflictingRulesCount=" + conflictingRules.size() +
                ", description='" + description + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}