package core.petri;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

/**
 * Intent specification for Petri net-based workflows.
 * Represents a structured interpretation of natural language prompts
 * that will be converted into formal Petri net representations.
 *
 * This extends the concept of IntentSpec with Petri net-specific semantics
 * and maintains compatibility with existing template infrastructure.
 *
 * @author Obvian Labs
 * @since Task 2 - Petri-specific templates
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetriIntentSpec {

    /**
     * Model type identifier for Petri net workflows
     */
    public static final String MODEL_TYPE = "PETRI";

    /**
     * Types of intent steps supported in Petri net workflows
     */
    public enum StepType {
        ACTION,              // Single action/task -> becomes transition
        SEQUENCE,            // Sequential flow -> connected places/transitions
        CHOICE,              // XOR branching -> choice transition with guards
        PARALLEL,            // AND-split -> fork/join places
        SYNC,                // Synchronization -> join transition

        // Advanced workflow patterns
        NESTED_CONDITIONAL,  // Nested if/then/else with AND/OR logic
        LOOP,                // For-each, while, retry loops with termination conditions
        EVENT_TRIGGER,       // Event-driven initiation (webhooks, time-based, etc.)
        ERROR_HANDLER,       // Try-catch-finally error handling patterns
        COMPENSATION,        // Compensation actions for failed operations
        CIRCUIT_BREAKER,     // Circuit breaker pattern for resilience
        FAN_OUT_FAN_IN,      // Complex parallel patterns with different sync strategies
        PIPELINE_STAGE,      // Pipeline parallelism with stage dependencies
        RESOURCE_CONSTRAINED // Resource-limited parallel execution
    }

    /**
     * Represents a single step in the intent workflow
     */
    public static class IntentStep {
        @JsonProperty("id")
        private final String id;

        @JsonProperty("type")
        private final StepType type;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("dependencies")
        private final List<String> dependencies;

        @JsonProperty("conditions")
        private final Map<String, Object> conditions;

        @JsonProperty("when")
        private final String when;  // Guard condition for choice steps

        @JsonProperty("metadata")
        private final Map<String, Object> metadata;

        // Advanced pattern fields
        @JsonProperty("loopCondition")
        private final String loopCondition;  // Termination condition for loops

        @JsonProperty("errorHandling")
        private final Map<String, Object> errorHandling;  // Error handling configuration

        @JsonProperty("compensation")
        private final List<String> compensation;  // Compensation actions

        @JsonProperty("timeout")
        private final Long timeout;  // Timeout in milliseconds

        @JsonProperty("retryPolicy")
        private final Map<String, Object> retryPolicy;  // Retry configuration

        @JsonProperty("resourceConstraints")
        private final Map<String, Object> resourceConstraints;  // Resource limits

        @JsonCreator
        public IntentStep(
                @JsonProperty("id") String id,
                @JsonProperty("type") StepType type,
                @JsonProperty("description") String description,
                @JsonProperty("dependencies") List<String> dependencies,
                @JsonProperty("conditions") Map<String, Object> conditions,
                @JsonProperty("when") String when,
                @JsonProperty("metadata") Map<String, Object> metadata,
                @JsonProperty("loopCondition") String loopCondition,
                @JsonProperty("errorHandling") Map<String, Object> errorHandling,
                @JsonProperty("compensation") List<String> compensation,
                @JsonProperty("timeout") Long timeout,
                @JsonProperty("retryPolicy") Map<String, Object> retryPolicy,
                @JsonProperty("resourceConstraints") Map<String, Object> resourceConstraints) {
            this.id = id;
            this.type = type;
            this.description = description;
            this.dependencies = dependencies != null ? new ArrayList<>(dependencies) : new ArrayList<>();
            this.conditions = conditions != null ? new HashMap<>(conditions) : new HashMap<>();
            this.when = when;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            this.loopCondition = loopCondition;
            this.errorHandling = errorHandling != null ? new HashMap<>(errorHandling) : new HashMap<>();
            this.compensation = compensation != null ? new ArrayList<>(compensation) : new ArrayList<>();
            this.timeout = timeout;
            this.retryPolicy = retryPolicy != null ? new HashMap<>(retryPolicy) : new HashMap<>();
            this.resourceConstraints = resourceConstraints != null ? new HashMap<>(resourceConstraints) : new HashMap<>();
        }

        // Getters
        public String getId() { return id; }
        public StepType getType() { return type; }
        public String getDescription() { return description; }
        public List<String> getDependencies() { return new ArrayList<>(dependencies); }
        public Map<String, Object> getConditions() { return new HashMap<>(conditions); }
        public String getWhen() { return when; }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
        public String getLoopCondition() { return loopCondition; }
        public Map<String, Object> getErrorHandling() { return new HashMap<>(errorHandling); }
        public List<String> getCompensation() { return new ArrayList<>(compensation); }
        public Long getTimeout() { return timeout; }
        public Map<String, Object> getRetryPolicy() { return new HashMap<>(retryPolicy); }
        public Map<String, Object> getResourceConstraints() { return new HashMap<>(resourceConstraints); }

        /**
         * Check if this step has guard conditions
         */
        public boolean hasGuard() {
            return when != null && !when.trim().isEmpty();
        }

        /**
         * Check if this step is conditional on specific outcomes
         */
        public boolean isConditional() {
            return hasGuard() || !conditions.isEmpty();
        }

        /**
         * Check if this step is a loop-based step
         */
        public boolean isLoop() {
            return type == StepType.LOOP && (loopCondition != null || !conditions.isEmpty());
        }

        /**
         * Check if this step has error handling
         */
        public boolean hasErrorHandling() {
            return !errorHandling.isEmpty() || !compensation.isEmpty();
        }

        /**
         * Check if this step has timeout configuration
         */
        public boolean hasTimeout() {
            return timeout != null && timeout > 0;
        }

        /**
         * Check if this step has retry policy
         */
        public boolean hasRetryPolicy() {
            return !retryPolicy.isEmpty();
        }

        /**
         * Check if this step has resource constraints
         */
        public boolean hasResourceConstraints() {
            return !resourceConstraints.isEmpty();
        }

        /**
         * Check if this is an advanced workflow pattern step
         */
        public boolean isAdvancedPattern() {
            return type == StepType.NESTED_CONDITIONAL ||
                   type == StepType.LOOP ||
                   type == StepType.EVENT_TRIGGER ||
                   type == StepType.ERROR_HANDLER ||
                   type == StepType.COMPENSATION ||
                   type == StepType.CIRCUIT_BREAKER ||
                   type == StepType.FAN_OUT_FAN_IN ||
                   type == StepType.PIPELINE_STAGE ||
                   type == StepType.RESOURCE_CONSTRAINED;
        }

        @Override
        public String toString() {
            return "IntentStep{" +
                    "id='" + id + '\'' +
                    ", type=" + type +
                    ", description='" + description + '\'' +
                    (when != null ? ", when='" + when + '\'' : "") +
                    '}';
        }
    }

    @JsonProperty("modelType")
    private final String modelType;

    @JsonProperty("name")
    private final String name;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("steps")
    private final List<IntentStep> steps;

    @JsonProperty("metadata")
    private final Map<String, Object> metadata;

    @JsonProperty("schemaVersion")
    private final String schemaVersion;

    @JsonProperty("originalPrompt")
    private final String originalPrompt;

    @JsonProperty("templateId")
    private final String templateId;

    @JsonCreator
    public PetriIntentSpec(
            @JsonProperty("modelType") String modelType,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("steps") List<IntentStep> steps,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("schemaVersion") String schemaVersion,
            @JsonProperty("originalPrompt") String originalPrompt,
            @JsonProperty("templateId") String templateId) {
        this.modelType = modelType != null ? modelType : MODEL_TYPE;
        this.name = name;
        this.description = description;
        this.steps = steps != null ? new ArrayList<>(steps) : new ArrayList<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.schemaVersion = schemaVersion != null ? schemaVersion : "1.0";
        this.originalPrompt = originalPrompt;
        this.templateId = templateId;
    }

    // Getters
    public String getModelType() { return modelType; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<IntentStep> getSteps() { return new ArrayList<>(steps); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public String getSchemaVersion() { return schemaVersion; }
    public String getOriginalPrompt() { return originalPrompt; }
    public String getTemplateId() { return templateId; }

    /**
     * Check if this intent spec is for Petri net models
     */
    public boolean isPetriModel() {
        return MODEL_TYPE.equals(modelType);
    }

    /**
     * Get steps by type
     */
    public List<IntentStep> getStepsByType(StepType type) {
        return steps.stream()
                .filter(step -> step.getType() == type)
                .toList();
    }

    /**
     * Get step by ID
     */
    public Optional<IntentStep> getStep(String stepId) {
        return steps.stream()
                .filter(step -> step.getId().equals(stepId))
                .findFirst();
    }

    /**
     * Get all action steps (will become transitions)
     */
    public List<IntentStep> getActionSteps() {
        return getStepsByType(StepType.ACTION);
    }

    /**
     * Get all choice steps (will become XOR branching)
     */
    public List<IntentStep> getChoiceSteps() {
        return getStepsByType(StepType.CHOICE);
    }

    /**
     * Get all parallel steps (will become AND-split/join)
     */
    public List<IntentStep> getParallelSteps() {
        return getStepsByType(StepType.PARALLEL);
    }

    /**
     * Get steps that depend on a given step
     */
    public List<IntentStep> getDependentSteps(String stepId) {
        return steps.stream()
                .filter(step -> step.getDependencies().contains(stepId))
                .toList();
    }

    /**
     * Get all advanced pattern steps
     */
    public List<IntentStep> getAdvancedPatternSteps() {
        return steps.stream()
                .filter(IntentStep::isAdvancedPattern)
                .toList();
    }

    /**
     * Get all loop steps
     */
    public List<IntentStep> getLoopSteps() {
        return getStepsByType(StepType.LOOP);
    }

    /**
     * Get all nested conditional steps
     */
    public List<IntentStep> getNestedConditionalSteps() {
        return getStepsByType(StepType.NESTED_CONDITIONAL);
    }

    /**
     * Get all event trigger steps
     */
    public List<IntentStep> getEventTriggerSteps() {
        return getStepsByType(StepType.EVENT_TRIGGER);
    }

    /**
     * Get all error handler steps
     */
    public List<IntentStep> getErrorHandlerSteps() {
        return getStepsByType(StepType.ERROR_HANDLER);
    }

    /**
     * Get all compensation steps
     */
    public List<IntentStep> getCompensationSteps() {
        return getStepsByType(StepType.COMPENSATION);
    }

    /**
     * Get all circuit breaker steps
     */
    public List<IntentStep> getCircuitBreakerSteps() {
        return getStepsByType(StepType.CIRCUIT_BREAKER);
    }

    /**
     * Get steps with error handling configured
     */
    public List<IntentStep> getStepsWithErrorHandling() {
        return steps.stream()
                .filter(IntentStep::hasErrorHandling)
                .toList();
    }

    /**
     * Get steps with timeouts
     */
    public List<IntentStep> getTimedSteps() {
        return steps.stream()
                .filter(IntentStep::hasTimeout)
                .toList();
    }

    /**
     * Get steps with retry policies
     */
    public List<IntentStep> getRetryableSteps() {
        return steps.stream()
                .filter(IntentStep::hasRetryPolicy)
                .toList();
    }

    /**
     * Validate the intent specification
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add("Intent name cannot be null or empty");
        }

        if (steps.isEmpty()) {
            errors.add("Intent must have at least one step");
        }

        // Check for duplicate step IDs
        Set<String> stepIds = new HashSet<>();
        for (IntentStep step : steps) {
            if (step.getId() == null || step.getId().trim().isEmpty()) {
                errors.add("Step ID cannot be null or empty");
                continue;
            }
            if (!stepIds.add(step.getId())) {
                errors.add("Duplicate step ID: " + step.getId());
            }
        }

        // Check dependency references
        for (IntentStep step : steps) {
            for (String dependency : step.getDependencies()) {
                if (!stepIds.contains(dependency)) {
                    errors.add("Step " + step.getId() + " references unknown dependency: " + dependency);
                }
            }
        }

        // Check for circular dependencies (basic cycle detection)
        for (String stepId : stepIds) {
            if (hasCircularDependency(stepId, new HashSet<>())) {
                errors.add("Circular dependency detected involving step: " + stepId);
                break;
            }
        }

        return errors;
    }

    private boolean hasCircularDependency(String stepId, Set<String> visited) {
        if (visited.contains(stepId)) {
            return true;
        }

        Optional<IntentStep> step = getStep(stepId);
        if (step.isEmpty()) {
            return false;
        }

        visited.add(stepId);

        for (String dependency : step.get().getDependencies()) {
            if (hasCircularDependency(dependency, visited)) {
                return true;
            }
        }

        visited.remove(stepId);
        return false;
    }

    /**
     * Builder pattern for creating PetriIntentSpec instances
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String modelType = MODEL_TYPE;
        private String name;
        private String description;
        private List<IntentStep> steps = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();
        private String schemaVersion = "1.0";
        private String originalPrompt;
        private String templateId;

        public Builder modelType(String modelType) {
            this.modelType = modelType;
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

        public Builder addStep(IntentStep step) {
            this.steps.add(step);
            return this;
        }

        public Builder addActionStep(String id, String description) {
            this.steps.add(new IntentStep(id, StepType.ACTION, description,
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>(),
                    null, new HashMap<>(), new ArrayList<>(), null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder addChoiceStep(String id, String description, List<String> paths) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paths", paths);
            this.steps.add(new IntentStep(id, StepType.CHOICE, description,
                    new ArrayList<>(), new HashMap<>(), null, metadata,
                    null, new HashMap<>(), new ArrayList<>(), null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder addConditionalActionStep(String id, String description, String when) {
            this.steps.add(new IntentStep(id, StepType.ACTION, description,
                    new ArrayList<>(), new HashMap<>(), when, new HashMap<>(),
                    null, new HashMap<>(), new ArrayList<>(), null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder addParallelStep(String id, String description) {
            this.steps.add(new IntentStep(id, StepType.PARALLEL, description,
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>(),
                    null, new HashMap<>(), new ArrayList<>(), null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder addSyncStep(String id, String description) {
            this.steps.add(new IntentStep(id, StepType.SYNC, description,
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>(),
                    null, new HashMap<>(), new ArrayList<>(), null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        // Advanced pattern builder methods
        public Builder addLoopStep(String id, String description, String loopCondition) {
            this.steps.add(new IntentStep(id, StepType.LOOP, description,
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>(),
                    loopCondition, new HashMap<>(), new ArrayList<>(), null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder addNestedConditionalStep(String id, String description, Map<String, Object> conditions) {
            this.steps.add(new IntentStep(id, StepType.NESTED_CONDITIONAL, description,
                    new ArrayList<>(), conditions, null, new HashMap<>(),
                    null, new HashMap<>(), new ArrayList<>(), null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder addEventTriggerStep(String id, String description, Map<String, Object> triggerConfig) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("triggerConfig", triggerConfig);
            this.steps.add(new IntentStep(id, StepType.EVENT_TRIGGER, description,
                    new ArrayList<>(), new HashMap<>(), null, metadata,
                    null, new HashMap<>(), new ArrayList<>(), null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder addErrorHandlerStep(String id, String description, Map<String, Object> errorHandling, List<String> compensation) {
            this.steps.add(new IntentStep(id, StepType.ERROR_HANDLER, description,
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>(),
                    null, errorHandling, compensation, null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder addCompensationStep(String id, String description, List<String> compensationActions) {
            this.steps.add(new IntentStep(id, StepType.COMPENSATION, description,
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>(),
                    null, new HashMap<>(), compensationActions, null, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder addCircuitBreakerStep(String id, String description, Map<String, Object> retryPolicy) {
            this.steps.add(new IntentStep(id, StepType.CIRCUIT_BREAKER, description,
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>(),
                    null, new HashMap<>(), new ArrayList<>(), null, retryPolicy, new HashMap<>()));
            return this;
        }

        public Builder addResourceConstrainedStep(String id, String description, Map<String, Object> resourceConstraints) {
            this.steps.add(new IntentStep(id, StepType.RESOURCE_CONSTRAINED, description,
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>(),
                    null, new HashMap<>(), new ArrayList<>(), null, new HashMap<>(), resourceConstraints));
            return this;
        }

        public Builder addTimedStep(String id, String description, Long timeout) {
            this.steps.add(new IntentStep(id, StepType.ACTION, description,
                    new ArrayList<>(), new HashMap<>(), null, new HashMap<>(),
                    null, new HashMap<>(), new ArrayList<>(), timeout, new HashMap<>(), new HashMap<>()));
            return this;
        }

        public Builder steps(List<IntentStep> steps) {
            this.steps = new ArrayList<>(steps);
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

        public Builder schemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder originalPrompt(String originalPrompt) {
            this.originalPrompt = originalPrompt;
            return this;
        }

        public Builder templateId(String templateId) {
            this.templateId = templateId;
            return this;
        }

        public PetriIntentSpec build() {
            return new PetriIntentSpec(modelType, name, description, steps,
                    metadata, schemaVersion, originalPrompt, templateId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PetriIntentSpec that = (PetriIntentSpec) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(steps, that.steps) &&
               Objects.equals(modelType, that.modelType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelType, name, steps);
    }

    @Override
    public String toString() {
        return "PetriIntentSpec{" +
                "modelType='" + modelType + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", steps=" + steps.size() +
                ", templateId='" + templateId + '\'' +
                '}';
    }
}