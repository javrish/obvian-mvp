package core.petri.validation;

import core.petri.PetriNet;
import core.petri.Marking;
import core.ValidationResult;
import core.ValidationIssue;
import core.RuleConflict;
import core.RuleDependency;
import core.BusinessRule;
import core.ModelType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Extends ValidationResult for Petri net specific validation outcomes.
 * Includes formal verification results like deadlock detection, reachability analysis,
 * liveness checking, and boundedness verification.
 * 
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetriNetValidationResult extends ValidationResult {
    
    /**
     * Petri net specific validation status
     */
    public enum PetriValidationStatus {
        PASS,                    // All validations passed
        FAIL,                    // Critical validation failures
        INCONCLUSIVE_BOUND,      // Bound reached during analysis
        INCONCLUSIVE_TIMEOUT     // Timeout during analysis
    }
    
    /**
     * Types of Petri net validation checks
     */
    public enum CheckType {
        DEADLOCK_DETECTION,
        REACHABILITY_ANALYSIS,
        LIVENESS_CHECK,
        BOUNDEDNESS_CHECK,
        STRUCTURAL_VALIDATION
    }
    
    /**
     * Result of a specific validation check
     */
    public static class CheckResult {
        @JsonProperty("type")
        private final CheckType type;
        
        @JsonProperty("status")
        private final PetriValidationStatus status;
        
        @JsonProperty("message")
        private final String message;
        
        @JsonProperty("details")
        private final Map<String, Object> details;
        
        @JsonProperty("executionTimeMs")
        private final long executionTimeMs;
        
        @JsonCreator
        public CheckResult(
                @JsonProperty("type") CheckType type,
                @JsonProperty("status") PetriValidationStatus status,
                @JsonProperty("message") String message,
                @JsonProperty("details") Map<String, Object> details,
                @JsonProperty("executionTimeMs") long executionTimeMs) {
            this.type = type;
            this.status = status;
            this.message = message;
            this.details = details != null ? new HashMap<>(details) : new HashMap<>();
            this.executionTimeMs = executionTimeMs;
        }
        
        // Getters
        public CheckType getType() { return type; }
        public PetriValidationStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return new HashMap<>(details); }
        public long getExecutionTimeMs() { return executionTimeMs; }
        
        public boolean isPassed() { return status == PetriValidationStatus.PASS; }
        public boolean isFailed() { return status == PetriValidationStatus.FAIL; }
        public boolean isInconclusive() { 
            return status == PetriValidationStatus.INCONCLUSIVE_BOUND || 
                   status == PetriValidationStatus.INCONCLUSIVE_TIMEOUT; 
        }
    }
    
    /**
     * Counter-example for failed validations
     */
    public static class CounterExample {
        @JsonProperty("failingMarking")
        private final Marking failingMarking;
        
        @JsonProperty("enabledTransitions")
        private final List<String> enabledTransitions;
        
        @JsonProperty("pathToFailure")
        private final List<String> pathToFailure;
        
        @JsonProperty("description")
        private final String description;
        
        @JsonCreator
        public CounterExample(
                @JsonProperty("failingMarking") Marking failingMarking,
                @JsonProperty("enabledTransitions") List<String> enabledTransitions,
                @JsonProperty("pathToFailure") List<String> pathToFailure,
                @JsonProperty("description") String description) {
            this.failingMarking = failingMarking;
            this.enabledTransitions = enabledTransitions != null ? 
                    new ArrayList<>(enabledTransitions) : new ArrayList<>();
            this.pathToFailure = pathToFailure != null ? 
                    new ArrayList<>(pathToFailure) : new ArrayList<>();
            this.description = description;
        }
        
        // Getters
        public Marking getFailingMarking() { return failingMarking; }
        public List<String> getEnabledTransitions() { return new ArrayList<>(enabledTransitions); }
        public List<String> getPathToFailure() { return new ArrayList<>(pathToFailure); }
        public String getDescription() { return description; }
    }
    
    /**
     * Configuration used for validation
     */
    public static class ValidationConfig {
        @JsonProperty("kBound")
        private final int kBound;
        
        @JsonProperty("maxTimeMs")
        private final long maxTimeMs;
        
        @JsonProperty("enabledChecks")
        private final Set<CheckType> enabledChecks;
        
        @JsonCreator
        public ValidationConfig(
                @JsonProperty("kBound") int kBound,
                @JsonProperty("maxTimeMs") long maxTimeMs,
                @JsonProperty("enabledChecks") Set<CheckType> enabledChecks) {
            this.kBound = kBound > 0 ? kBound : 200; // Default bound
            this.maxTimeMs = maxTimeMs > 0 ? maxTimeMs : 30000; // Default 30s timeout
            this.enabledChecks = enabledChecks != null ? 
                    new HashSet<>(enabledChecks) : EnumSet.allOf(CheckType.class);
        }
        
        // Getters
        public int getKBound() { return kBound; }
        public long getMaxTimeMs() { return maxTimeMs; }
        public Set<CheckType> getEnabledChecks() { return new HashSet<>(enabledChecks); }
        
        public static ValidationConfig defaultConfig() {
            return new ValidationConfig(200, 30000, EnumSet.allOf(CheckType.class));
        }
    }
    
    @JsonProperty("petriStatus")
    private final PetriValidationStatus petriStatus;
    
    @JsonProperty("checks")
    private final Map<CheckType, CheckResult> checks;
    
    @JsonProperty("counterExample")
    private final CounterExample counterExample;
    
    @JsonProperty("hints")
    private final List<String> hints;
    
    @JsonProperty("config")
    private final ValidationConfig config;
    
    @JsonProperty("statesExplored")
    private final int statesExplored;
    
    @JsonProperty("petriNetId")
    private final String petriNetId;
    
    @JsonCreator
    private PetriNetValidationResult(
            // Parent ValidationResult fields
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
            @JsonProperty("suggestions") List<String> suggestions,
            // Petri net specific fields
            @JsonProperty("petriStatus") PetriValidationStatus petriStatus,
            @JsonProperty("checks") Map<CheckType, CheckResult> checks,
            @JsonProperty("counterExample") CounterExample counterExample,
            @JsonProperty("hints") List<String> hints,
            @JsonProperty("config") ValidationConfig config,
            @JsonProperty("statesExplored") int statesExplored,
            @JsonProperty("petriNetId") String petriNetId) {
        super(status, valid, modelType != null ? modelType : ModelType.PETRI, witness,
              confidenceScore, validationTimeMs, validatedAt, issues, conflicts,
              dependencies, executionOrder, performanceMetrics, simulationResult, metadata, suggestions);
        
        this.petriStatus = petriStatus != null ? petriStatus : PetriValidationStatus.PASS;
        this.checks = checks != null ? new HashMap<>(checks) : new HashMap<>();
        this.counterExample = counterExample;
        this.hints = hints != null ? new ArrayList<>(hints) : new ArrayList<>();
        this.config = config != null ? config : ValidationConfig.defaultConfig();
        this.statesExplored = statesExplored;
        this.petriNetId = petriNetId;
    }
    
    // Getters for Petri net specific fields
    public PetriValidationStatus getPetriStatus() { return petriStatus; }
    public Map<CheckType, CheckResult> getChecks() { return new HashMap<>(checks); }
    public CounterExample getCounterExample() { return counterExample; }
    public List<String> getHints() { return new ArrayList<>(hints); }
    public ValidationConfig getConfig() { return config; }
    public int getStatesExplored() { return statesExplored; }
    public String getPetriNetId() { return petriNetId; }
    
    /**
     * Check if validation passed all checks
     */
    public boolean isPassed() {
        return petriStatus == PetriValidationStatus.PASS && 
               checks.values().stream().allMatch(CheckResult::isPassed);
    }
    
    /**
     * Check if validation failed any critical checks
     */
    public boolean isFailed() {
        return petriStatus == PetriValidationStatus.FAIL ||
               checks.values().stream().anyMatch(CheckResult::isFailed);
    }
    
    /**
     * Check if validation was inconclusive
     */
    public boolean isInconclusive() {
        return petriStatus == PetriValidationStatus.INCONCLUSIVE_BOUND ||
               petriStatus == PetriValidationStatus.INCONCLUSIVE_TIMEOUT ||
               checks.values().stream().anyMatch(CheckResult::isInconclusive);
    }
    
    /**
     * Get specific check result
     */
    public Optional<CheckResult> getCheckResult(CheckType type) {
        return Optional.ofNullable(checks.get(type));
    }
    
    /**
     * Get failed checks
     */
    public List<CheckResult> getFailedChecks() {
        return checks.values().stream()
                .filter(CheckResult::isFailed)
                .toList();
    }
    
    /**
     * Get inconclusive checks
     */
    public List<CheckResult> getInconclusiveChecks() {
        return checks.values().stream()
                .filter(CheckResult::isInconclusive)
                .toList();
    }
    
    /**
     * Get summary of validation results
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Petri Net Validation: ").append(petriStatus);
        summary.append(" (").append(statesExplored).append(" states explored)");
        
        long passedChecks = checks.values().stream().mapToLong(c -> c.isPassed() ? 1 : 0).sum();
        summary.append(", Checks: ").append(passedChecks).append("/").append(checks.size()).append(" passed");
        
        if (counterExample != null) {
            summary.append(", Counter-example available");
        }
        
        if (!hints.isEmpty()) {
            summary.append(", ").append(hints.size()).append(" hints");
        }
        
        return summary.toString();
    }
    
    /**
     * Builder pattern for creating PetriNetValidationResult instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Create a successful validation result
     */
    public static PetriNetValidationResult success(String petriNetId, ValidationConfig config) {
        Builder builder = builder()
                .petriStatus(PetriValidationStatus.PASS)
                .petriNetId(petriNetId)
                .config(config);

        builder.status(ValidationStatus.VALID)
                .valid(true)
                .confidenceScore(1.0);

        return builder.build();
    }
    
    /**
     * Create a failed validation result
     */
    public static PetriNetValidationResult failure(String petriNetId, String errorMessage,
                                                  CounterExample counterExample) {
        Builder builder = builder()
                .petriStatus(PetriValidationStatus.FAIL)
                .petriNetId(petriNetId)
                .counterExample(counterExample);

        builder.status(ValidationStatus.INVALID)
                .valid(false)
                .confidenceScore(0.0);

        if (errorMessage != null) {
            builder.addHint(errorMessage);
        }

        return builder.build();
    }
    
    /**
     * Create an inconclusive validation result
     */
    public static PetriNetValidationResult inconclusive(String petriNetId, String reason,
                                                       int statesExplored, ValidationConfig config) {
        PetriValidationStatus petriStatus = reason.contains("bound") ?
                PetriValidationStatus.INCONCLUSIVE_BOUND : PetriValidationStatus.INCONCLUSIVE_TIMEOUT;

        Builder builder = builder()
                .petriStatus(petriStatus)
                .petriNetId(petriNetId)
                .statesExplored(statesExplored)
                .config(config);

        builder.status(ValidationStatus.INCOMPLETE)
                .valid(false)
                .confidenceScore(0.5);

        if (reason != null) {
            builder.addHint(reason);
        }

        return builder.build();
    }
    
    public static class Builder extends ValidationResult.Builder {
        private PetriValidationStatus petriStatus = PetriValidationStatus.PASS;
        private Map<CheckType, CheckResult> checks = new HashMap<>();
        private CounterExample counterExample;
        private List<String> hints = new ArrayList<>();
        private ValidationConfig config = ValidationConfig.defaultConfig();
        private int statesExplored = 0;
        private String petriNetId;
        
        public Builder petriStatus(PetriValidationStatus status) {
            this.petriStatus = status;
            return this;
        }
        
        public Builder addCheck(CheckResult check) {
            this.checks.put(check.getType(), check);
            return this;
        }
        
        public Builder addCheck(CheckType type, PetriValidationStatus status, String message) {
            this.checks.put(type, new CheckResult(type, status, message, new HashMap<>(), 0));
            return this;
        }
        
        public Builder checks(Map<CheckType, CheckResult> checks) {
            this.checks = new HashMap<>(checks);
            return this;
        }
        
        public Builder counterExample(CounterExample counterExample) {
            this.counterExample = counterExample;
            return this;
        }
        
        public Builder addHint(String hint) {
            this.hints.add(hint);
            return this;
        }
        
        public Builder hints(List<String> hints) {
            this.hints = new ArrayList<>(hints);
            return this;
        }
        
        public Builder config(ValidationConfig config) {
            this.config = config;
            return this;
        }
        
        public Builder statesExplored(int states) {
            this.statesExplored = states;
            return this;
        }
        
        public Builder petriNetId(String petriNetId) {
            this.petriNetId = petriNetId;
            return this;
        }
        
        @Override
        public PetriNetValidationResult build() {
            // Set ModelType to PETRI for Petri net validation results
            modelType(ModelType.PETRI);

            // Convert counterExample to Witness if present
            Witness witnessObj = null;
            if (counterExample != null && counterExample.getFailingMarking() != null) {
                // Convert Marking to Map<String, Integer> for Witness
                Marking marking = counterExample.getFailingMarking();
                Map<String, Integer> markingMap = marking.getTokens(); // Already returns Map<String, Integer>

                witnessObj = Witness.builder()
                        .type("counterexample")
                        .description(counterExample.getDescription() != null ?
                                counterExample.getDescription() : "Validation failure")
                        .marking(markingMap)
                        .path(counterExample.getPathToFailure() != null ?
                                counterExample.getPathToFailure() : new ArrayList<>())
                        .build();
            }
            witness(witnessObj);

            ValidationResult parent = super.build();
            return new PetriNetValidationResult(
                    parent.getStatus(), parent.isValid(), parent.getModelType(), parent.getWitness(),
                    parent.getConfidenceScore(), parent.getValidationTimeMs(), parent.getValidatedAt(),
                    parent.getIssues(), parent.getConflicts(), parent.getDependencies(), parent.getExecutionOrder(),
                    parent.getPerformanceMetrics(), parent.getSimulationResult(),
                    parent.getMetadata(), parent.getSuggestions(),
                    petriStatus, checks, counterExample, hints, config, statesExplored, petriNetId);
        }
    }
    
    @Override
    public String toString() {
        return "PetriNetValidationResult{" +
                "petriStatus=" + petriStatus +
                ", checks=" + checks.size() +
                ", statesExplored=" + statesExplored +
                ", petriNetId='" + petriNetId + '\'' +
                ", valid=" + isValid() +
                '}';
    }
}