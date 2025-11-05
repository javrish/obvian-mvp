package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Comprehensive validation result containing all aspects of business rule validation.
 * Includes structural validation, conflict detection, dependency analysis, performance metrics,
 * and simulation results with detailed issue reporting and confidence scoring.
 * 
 * This class is part of the BusinessRuleValidator system (Phase 26.2c)
 * which implements comprehensive rule validation with advanced conflict detection
 * and performance analysis capabilities.
 * 
 * Patent Alignment: Implements validation result representation that enables
 * comprehensive business rule validation with detailed feedback and simulation results.
 * 
 * @author Obvian Labs
 * @since Phase 26.2c
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationResult {
    
    /**
     * Overall validation status
     */
    public enum ValidationStatus {
        VALID,              // Rule passed all validations
        INVALID,            // Rule failed critical validations
        WARNING,            // Rule has warnings but is usable
        INCOMPLETE,         // Validation could not be completed
        TIMEOUT             // Validation timed out
    }
    
    /**
     * Performance metrics for rule validation and execution
     */
    public static class PerformanceMetrics {
        @JsonProperty("estimatedExecutionTimeMs")
        private final long estimatedExecutionTimeMs;
        
        @JsonProperty("complexityScore")
        private final double complexityScore;
        
        @JsonProperty("memoryUsageEstimateMB")
        private final double memoryUsageEstimateMB;
        
        @JsonProperty("cpuIntensityScore")
        private final double cpuIntensityScore;
        
        @JsonProperty("bottlenecks")
        private final List<String> bottlenecks;
        
        @JsonProperty("optimizationSuggestions")
        private final List<String> optimizationSuggestions;
        
        @JsonCreator
        public PerformanceMetrics(
                @JsonProperty("estimatedExecutionTimeMs") long estimatedExecutionTimeMs,
                @JsonProperty("complexityScore") double complexityScore,
                @JsonProperty("memoryUsageEstimateMB") double memoryUsageEstimateMB,
                @JsonProperty("cpuIntensityScore") double cpuIntensityScore,
                @JsonProperty("bottlenecks") List<String> bottlenecks,
                @JsonProperty("optimizationSuggestions") List<String> optimizationSuggestions) {
            this.estimatedExecutionTimeMs = estimatedExecutionTimeMs;
            this.complexityScore = complexityScore;
            this.memoryUsageEstimateMB = memoryUsageEstimateMB;
            this.cpuIntensityScore = cpuIntensityScore;
            this.bottlenecks = bottlenecks != null ? new ArrayList<>(bottlenecks) : new ArrayList<>();
            this.optimizationSuggestions = optimizationSuggestions != null ? 
                    new ArrayList<>(optimizationSuggestions) : new ArrayList<>();
        }
        
        public long getEstimatedExecutionTimeMs() { return estimatedExecutionTimeMs; }
        public double getComplexityScore() { return complexityScore; }
        public double getMemoryUsageEstimateMB() { return memoryUsageEstimateMB; }
        public double getCpuIntensityScore() { return cpuIntensityScore; }
        public List<String> getBottlenecks() { return new ArrayList<>(bottlenecks); }
        public List<String> getOptimizationSuggestions() { return new ArrayList<>(optimizationSuggestions); }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private long estimatedExecutionTimeMs = 0;
            private double complexityScore = 0.0;
            private double memoryUsageEstimateMB = 0.0;
            private double cpuIntensityScore = 0.0;
            private List<String> bottlenecks = new ArrayList<>();
            private List<String> optimizationSuggestions = new ArrayList<>();
            
            public Builder estimatedExecutionTimeMs(long time) {
                this.estimatedExecutionTimeMs = time;
                return this;
            }
            
            public Builder complexityScore(double score) {
                this.complexityScore = score;
                return this;
            }
            
            public Builder memoryUsageEstimateMB(double usage) {
                this.memoryUsageEstimateMB = usage;
                return this;
            }
            
            public Builder cpuIntensityScore(double score) {
                this.cpuIntensityScore = score;
                return this;
            }
            
            public Builder addBottleneck(String bottleneck) {
                this.bottlenecks.add(bottleneck);
                return this;
            }
            
            public Builder addOptimizationSuggestion(String suggestion) {
                this.optimizationSuggestions.add(suggestion);
                return this;
            }
            
            public PerformanceMetrics build() {
                return new PerformanceMetrics(estimatedExecutionTimeMs, complexityScore,
                        memoryUsageEstimateMB, cpuIntensityScore, bottlenecks, optimizationSuggestions);
            }
        }
    }
    
    /**
     * Witness for validation failures - provides counter-example or proof
     */
    public static class Witness {
        @JsonProperty("type")
        private final String type;
        
        @JsonProperty("description")
        private final String description;
        
        @JsonProperty("marking")
        private final Map<String, Integer> marking;
        
        @JsonProperty("path")
        private final List<String> path;
        
        @JsonProperty("enabledTransitions")
        private final List<String> enabledTransitions;
        
        @JsonProperty("metadata")
        private final Map<String, Object> metadata;
        
        @JsonCreator
        public Witness(
                @JsonProperty("type") String type,
                @JsonProperty("description") String description,
                @JsonProperty("marking") Map<String, Integer> marking,
                @JsonProperty("path") List<String> path,
                @JsonProperty("enabledTransitions") List<String> enabledTransitions,
                @JsonProperty("metadata") Map<String, Object> metadata) {
            this.type = type;
            this.description = description;
            this.marking = marking != null ? new HashMap<>(marking) : new HashMap<>();
            this.path = path != null ? new ArrayList<>(path) : new ArrayList<>();
            this.enabledTransitions = enabledTransitions != null ? new ArrayList<>(enabledTransitions) : new ArrayList<>();
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }
        
        public String getType() { return type; }
        public String getDescription() { return description; }
        public Map<String, Integer> getMarking() { return new HashMap<>(marking); }
        public List<String> getPath() { return new ArrayList<>(path); }
        public List<String> getEnabledTransitions() { return new ArrayList<>(enabledTransitions); }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String type;
            private String description;
            private Map<String, Integer> marking = new HashMap<>();
            private List<String> path = new ArrayList<>();
            private List<String> enabledTransitions = new ArrayList<>();
            private Map<String, Object> metadata = new HashMap<>();
            
            public Builder type(String type) {
                this.type = type;
                return this;
            }
            
            public Builder description(String description) {
                this.description = description;
                return this;
            }
            
            public Builder marking(Map<String, Integer> marking) {
                this.marking = new HashMap<>(marking);
                return this;
            }
            
            public Builder path(List<String> path) {
                this.path = new ArrayList<>(path);
                return this;
            }
            
            public Builder enabledTransitions(List<String> enabledTransitions) {
                this.enabledTransitions = new ArrayList<>(enabledTransitions);
                return this;
            }
            
            public Builder addMetadata(String key, Object value) {
                this.metadata.put(key, value);
                return this;
            }
            
            public Witness build() {
                return new Witness(type, description, marking, path, enabledTransitions, metadata);
            }
        }
    }

    /**
     * Simulation result for rule execution testing
     */
    public static class SimulationResult {
        @JsonProperty("executionSuccessful")
        private final boolean executionSuccessful;
        
        @JsonProperty("executedActions")
        private final List<String> executedActions;
        
        @JsonProperty("executionTimeMs")
        private final long executionTimeMs;
        
        @JsonProperty("testData")
        private final Map<String, Object> testData;
        
        @JsonProperty("executionError")
        private final String executionError;
        
        @JsonProperty("scenarioResults")
        private final List<ScenarioResult> scenarioResults;
        
        @JsonCreator
        public SimulationResult(
                @JsonProperty("executionSuccessful") boolean executionSuccessful,
                @JsonProperty("executedActions") List<String> executedActions,
                @JsonProperty("executionTimeMs") long executionTimeMs,
                @JsonProperty("testData") Map<String, Object> testData,
                @JsonProperty("executionError") String executionError,
                @JsonProperty("scenarioResults") List<ScenarioResult> scenarioResults) {
            this.executionSuccessful = executionSuccessful;
            this.executedActions = executedActions != null ? new ArrayList<>(executedActions) : new ArrayList<>();
            this.executionTimeMs = executionTimeMs;
            this.testData = testData != null ? new HashMap<>(testData) : new HashMap<>();
            this.executionError = executionError;
            this.scenarioResults = scenarioResults != null ? new ArrayList<>(scenarioResults) : new ArrayList<>();
        }
        
        public boolean isExecutionSuccessful() { return executionSuccessful; }
        public List<String> getExecutedActions() { return new ArrayList<>(executedActions); }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public Map<String, Object> getTestData() { return new HashMap<>(testData); }
        public String getExecutionError() { return executionError; }
        public List<ScenarioResult> getScenarioResults() { return new ArrayList<>(scenarioResults); }
        
        public static class ScenarioResult {
            @JsonProperty("scenarioName")
            private final String scenarioName;
            
            @JsonProperty("successful")
            private final boolean successful;
            
            @JsonProperty("executionTimeMs")
            private final long executionTimeMs;
            
            @JsonProperty("testData")
            private final Map<String, Object> testData;
            
            @JsonProperty("error")
            private final String error;
            
            @JsonCreator
            public ScenarioResult(
                    @JsonProperty("scenarioName") String scenarioName,
                    @JsonProperty("successful") boolean successful,
                    @JsonProperty("executionTimeMs") long executionTimeMs,
                    @JsonProperty("testData") Map<String, Object> testData,
                    @JsonProperty("error") String error) {
                this.scenarioName = scenarioName;
                this.successful = successful;
                this.executionTimeMs = executionTimeMs;
                this.testData = testData != null ? new HashMap<>(testData) : new HashMap<>();
                this.error = error;
            }
            
            public String getScenarioName() { return scenarioName; }
            public boolean isSuccessful() { return successful; }
            public long getExecutionTimeMs() { return executionTimeMs; }
            public Map<String, Object> getTestData() { return new HashMap<>(testData); }
            public String getError() { return error; }
        }
    }
    
    @JsonProperty("status")
    private final ValidationStatus status;
    
    @JsonProperty("valid")
    private final boolean valid;
    
    @JsonProperty("modelType")
    private final ModelType modelType;
    
    @JsonProperty("witness")
    private final Witness witness;
    
    @JsonProperty("confidenceScore")
    private final double confidenceScore;
    
    @JsonProperty("validationTimeMs")
    private final long validationTimeMs;
    
    @JsonProperty("validatedAt")
    private final LocalDateTime validatedAt;
    
    @JsonProperty("issues")
    private final List<ValidationIssue> issues;
    
    @JsonProperty("conflicts")
    private final List<RuleConflict> conflicts;
    
    @JsonProperty("dependencies")
    private final List<RuleDependency> dependencies;
    
    @JsonProperty("executionOrder")
    private final List<BusinessRule> executionOrder;
    
    @JsonProperty("performanceMetrics")
    private final PerformanceMetrics performanceMetrics;
    
    @JsonProperty("simulationResult")
    private final SimulationResult simulationResult;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    @JsonProperty("suggestions")
    private final List<String> suggestions;
    
    @JsonCreator
    protected ValidationResult(
            @JsonProperty("status") ValidationStatus status,
            @JsonProperty("valid") boolean valid,
            @JsonProperty("modelType") ModelType modelType,
            @JsonProperty("witness") Witness witness,
            @JsonProperty("confidenceScore") double confidenceScore,
            @JsonProperty("validationTimeMs") long validationTimeMs,
            @JsonProperty("validatedAt") LocalDateTime validatedAt,
            @JsonProperty("issues") List<ValidationIssue> issues,
            @JsonProperty("conflicts") List<RuleConflict> conflicts,
            @JsonProperty("dependencies") List<RuleDependency> dependencies,
            @JsonProperty("executionOrder") List<BusinessRule> executionOrder,
            @JsonProperty("performanceMetrics") PerformanceMetrics performanceMetrics,
            @JsonProperty("simulationResult") SimulationResult simulationResult,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("suggestions") List<String> suggestions) {
        this.status = status;
        this.valid = valid;
        this.modelType = modelType != null ? modelType : ModelType.DAG;
        this.witness = witness;
        this.confidenceScore = confidenceScore;
        this.validationTimeMs = validationTimeMs;
        this.validatedAt = validatedAt != null ? validatedAt : LocalDateTime.now();
        this.issues = issues != null ? new ArrayList<>(issues) : new ArrayList<>();
        this.conflicts = conflicts != null ? new ArrayList<>(conflicts) : new ArrayList<>();
        this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
        this.executionOrder = executionOrder != null ? new ArrayList<>(executionOrder) : new ArrayList<>();
        this.performanceMetrics = performanceMetrics;
        this.simulationResult = simulationResult;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.suggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
    }
    
    // Getters
    public ValidationStatus getStatus() { return status; }
    public boolean isValid() { return valid; }
    public ModelType getModelType() { return modelType; }
    public Witness getWitness() { return witness; }
    public double getConfidenceScore() { return confidenceScore; }
    public long getValidationTimeMs() { return validationTimeMs; }
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public List<ValidationIssue> getIssues() { return new ArrayList<>(issues); }
    public List<RuleConflict> getConflicts() { return new ArrayList<>(conflicts); }
    public List<RuleDependency> getDependencies() { return new ArrayList<>(dependencies); }
    public List<BusinessRule> getExecutionOrder() { return new ArrayList<>(executionOrder); }
    public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
    public SimulationResult getSimulationResult() { return simulationResult; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public List<String> getSuggestions() { return new ArrayList<>(suggestions); }

    /**
     * Get error messages from validation issues (for compatibility)
     */
    public List<String> getErrors() {
        return issues.stream()
                .map(ValidationIssue::getDescription)
                .toList();
    }
    
    /**
     * Check if validation has critical issues
     */
    public boolean hasCriticalIssues() {
        return issues.stream().anyMatch(issue -> 
                issue.getSeverity() == ValidationIssue.Severity.CRITICAL);
    }
    
    /**
     * Check if validation has warnings
     */
    public boolean hasWarnings() {
        return issues.stream().anyMatch(issue -> 
                issue.getSeverity() == ValidationIssue.Severity.MEDIUM ||
                issue.getSeverity() == ValidationIssue.Severity.HIGH);
    }
    
    /**
     * Get issues by severity level
     */
    public List<ValidationIssue> getIssuesBySeverity(ValidationIssue.Severity severity) {
        return issues.stream()
                .filter(issue -> issue.getSeverity() == severity)
                .toList();
    }
    
    /**
     * Get issues by type
     */
    public List<ValidationIssue> getIssuesByType(ValidationIssue.IssueType type) {
        return issues.stream()
                .filter(issue -> issue.getType() == type)
                .toList();
    }
    
    /**
     * Check if validation was successful (no critical issues)
     */
    public boolean isSuccessful() {
        return valid && !hasCriticalIssues();
    }
    
    /**
     * Get summary statistics
     */
    public Map<String, Object> getSummaryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIssues", issues.size());
        stats.put("criticalIssues", getIssuesBySeverity(ValidationIssue.Severity.CRITICAL).size());
        stats.put("highSeverityIssues", getIssuesBySeverity(ValidationIssue.Severity.HIGH).size());
        stats.put("mediumSeverityIssues", getIssuesBySeverity(ValidationIssue.Severity.MEDIUM).size());
        stats.put("lowSeverityIssues", getIssuesBySeverity(ValidationIssue.Severity.LOW).size());
        stats.put("conflictsFound", conflicts.size());
        stats.put("dependenciesAnalyzed", dependencies.size());
        stats.put("executionOrderDetermined", !executionOrder.isEmpty());
        stats.put("performanceAnalyzed", performanceMetrics != null);
        stats.put("simulationExecuted", simulationResult != null);
        return stats;
    }
    
    /**
     * Builder pattern for creating ValidationResult instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a successful validation result
     */
    public static ValidationResult success() {
        return builder()
                .status(ValidationStatus.VALID)
                .valid(true)
                .confidenceScore(1.0)
                .build();
    }
    
    /**
     * Create a validation result with success status and suggestions
     */
    public static ValidationResult success(List<String> suggestions) {
        return builder()
                .status(ValidationStatus.VALID)
                .valid(true)
                .confidenceScore(1.0)
                .suggestions(suggestions != null ? suggestions : new ArrayList<>())
                .build();
    }
    
    /**
     * Create a failed validation result with a single error message
     */
    public static ValidationResult failure(String errorMessage) {
        ValidationIssue issue = ValidationIssue.builder()
                .type(ValidationIssue.IssueType.VALIDATION_TIMEOUT)
                .severity(ValidationIssue.Severity.CRITICAL)
                .description(errorMessage != null ? errorMessage : "Validation failed")
                .build();
        
        return builder()
                .status(ValidationStatus.INVALID)
                .valid(false)
                .confidenceScore(0.0)
                .addIssue(issue)
                .build();
    }
    
    /**
     * Create a failed validation result with validation issues
     */
    public static ValidationResult failure(List<ValidationIssue> issues) {
        return builder()
                .status(ValidationStatus.INVALID)
                .valid(false)
                .confidenceScore(0.0)
                .issues(issues != null ? issues : new ArrayList<>())
                .build();
    }
    
    public static class Builder {
        private ValidationStatus status = ValidationStatus.VALID;
        private boolean valid = true;
        private ModelType modelType = ModelType.DAG;
        private Witness witness;
        private double confidenceScore = 1.0;
        private long validationTimeMs = 0;
        private LocalDateTime validatedAt;
        private List<ValidationIssue> issues = new ArrayList<>();
        private List<RuleConflict> conflicts = new ArrayList<>();
        private List<RuleDependency> dependencies = new ArrayList<>();
        private List<BusinessRule> executionOrder = new ArrayList<>();
        private PerformanceMetrics performanceMetrics;
        private SimulationResult simulationResult;
        private Map<String, Object> metadata = new HashMap<>();
        private List<String> suggestions = new ArrayList<>();
        
        public Builder status(ValidationStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }
        
        public Builder modelType(ModelType modelType) {
            this.modelType = modelType;
            return this;
        }
        
        public Builder witness(Witness witness) {
            this.witness = witness;
            return this;
        }
        
        public Builder confidenceScore(double score) {
            this.confidenceScore = Math.max(0.0, Math.min(1.0, score));
            return this;
        }
        
        public Builder validationTimeMs(long timeMs) {
            this.validationTimeMs = timeMs;
            return this;
        }
        
        public Builder validatedAt(LocalDateTime validatedAt) {
            this.validatedAt = validatedAt;
            return this;
        }
        
        public Builder addIssue(ValidationIssue issue) {
            this.issues.add(issue);
            updateStatusFromIssues();
            return this;
        }
        
        public Builder addIssue(ValidationIssue.IssueType type, String description, 
                              ValidationIssue.Severity severity) {
            return addIssue(ValidationIssue.builder()
                    .type(type)
                    .description(description)
                    .severity(severity)
                    .build());
        }
        
        public Builder issues(List<ValidationIssue> issues) {
            this.issues = new ArrayList<>(issues);
            updateStatusFromIssues();
            return this;
        }
        
        public Builder addConflict(RuleConflict conflict) {
            this.conflicts.add(conflict);
            return this;
        }
        
        public Builder conflicts(List<RuleConflict> conflicts) {
            this.conflicts = new ArrayList<>(conflicts);
            return this;
        }
        
        public Builder addDependency(RuleDependency dependency) {
            this.dependencies.add(dependency);
            return this;
        }
        
        public Builder dependencies(List<RuleDependency> dependencies) {
            this.dependencies = new ArrayList<>(dependencies);
            return this;
        }
        
        public Builder executionOrder(List<BusinessRule> executionOrder) {
            this.executionOrder = new ArrayList<>(executionOrder);
            return this;
        }
        
        public Builder performanceMetrics(PerformanceMetrics performanceMetrics) {
            this.performanceMetrics = performanceMetrics;
            return this;
        }
        
        public Builder simulationResult(SimulationResult simulationResult) {
            this.simulationResult = simulationResult;
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
        
        public Builder addSuggestion(String suggestion) {
            this.suggestions.add(suggestion);
            return this;
        }
        
        public Builder suggestions(List<String> suggestions) {
            this.suggestions = new ArrayList<>(suggestions);
            return this;
        }
        
        private void updateStatusFromIssues() {
            boolean hasCritical = issues.stream().anyMatch(issue -> 
                    issue.getSeverity() == ValidationIssue.Severity.CRITICAL);
            boolean hasHighOrMedium = issues.stream().anyMatch(issue -> 
                    issue.getSeverity() == ValidationIssue.Severity.HIGH ||
                    issue.getSeverity() == ValidationIssue.Severity.MEDIUM);
            
            if (hasCritical) {
                this.status = ValidationStatus.INVALID;
                this.valid = false;
            } else if (hasHighOrMedium) {
                this.status = ValidationStatus.WARNING;
                this.valid = true;
            } else {
                this.status = ValidationStatus.VALID;
                this.valid = true;
            }
        }
        
        public ValidationResult build() {
            if (validatedAt == null) {
                validatedAt = LocalDateTime.now();
            }
            
            return new ValidationResult(status, valid, modelType, witness, confidenceScore, validationTimeMs,
                    validatedAt, issues, conflicts, dependencies, executionOrder,
                    performanceMetrics, simulationResult, metadata, suggestions);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid &&
               Double.compare(that.confidenceScore, confidenceScore) == 0 &&
               validationTimeMs == that.validationTimeMs &&
               status == that.status &&
               Objects.equals(validatedAt, that.validatedAt) &&
               Objects.equals(issues, that.issues) &&
               Objects.equals(conflicts, that.conflicts) &&
               Objects.equals(dependencies, that.dependencies) &&
               Objects.equals(performanceMetrics, that.performanceMetrics) &&
               Objects.equals(simulationResult, that.simulationResult);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(status, valid, confidenceScore, validationTimeMs, validatedAt,
                issues, conflicts, dependencies, performanceMetrics, simulationResult);
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "status=" + status +
                ", valid=" + valid +
                ", confidenceScore=" + confidenceScore +
                ", validationTimeMs=" + validationTimeMs +
                ", issuesCount=" + issues.size() +
                ", conflictsCount=" + conflicts.size() +
                ", dependenciesCount=" + dependencies.size() +
                (performanceMetrics != null ? ", hasPerformanceMetrics=true" : "") +
                (simulationResult != null ? ", hasSimulationResult=true" : "") +
                '}';
    }
}