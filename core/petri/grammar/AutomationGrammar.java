package core.petri.grammar;

import core.petri.*;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Core AutomationGrammar engine that converts PetriIntentSpec objects to PetriNet structures
 * using formal transformation rules.
 *
 * This is the critical missing link in the NL → PetriIntentSpec → PetriNet → DAG pipeline.
 * Implements the formal transformation patterns:
 * - Sequential: A → B becomes place_A → transition_A → place_AB → transition_B → place_B
 * - Parallel: A ∥ B becomes AND-split/join with synchronization places
 * - Choice: A XOR B becomes single pre-place with conditional transitions
 *
 * @author Obvian Labs
 * @since Task 2 - AutomationGrammar Implementation
 */
@Component
public class AutomationGrammar {

    private static final Logger logger = LoggerFactory.getLogger(AutomationGrammar.class);

    private final IntentToPetriMapper mapper;
    private final RuleEngine ruleEngine;

    public AutomationGrammar(IntentToPetriMapper mapper, RuleEngine ruleEngine) {
        this.mapper = mapper;
        this.ruleEngine = ruleEngine;
    }

    /**
     * Default constructor with dependency injection
     */
    public AutomationGrammar() {
        this.mapper = new IntentToPetriMapper();
        this.ruleEngine = new RuleEngine();
    }

    /**
     * Main transformation method: converts PetriIntentSpec to PetriNet
     *
     * @param intentSpec the intent specification to transform
     * @return the generated PetriNet structure
     * @throws GrammarTransformationException if transformation fails
     */
    public PetriNet transform(PetriIntentSpec intentSpec) throws GrammarTransformationException {
        logger.info("Starting AutomationGrammar transformation for intent: {}", intentSpec.getName());

        // Validate input
        List<String> validationErrors = intentSpec.validate();
        if (!validationErrors.isEmpty()) {
            throw new GrammarTransformationException("Invalid intent specification: " + validationErrors);
        }

        try {
            // Create transformation context
            TransformationContext context = new TransformationContext(intentSpec);

            // Apply transformation rules based on intent steps
            PetriNet.Builder builder = PetriNet.builder()
                    .name(intentSpec.getName())
                    .description(intentSpec.getDescription())
                    .addMetadata("originalPrompt", intentSpec.getOriginalPrompt())
                    .addMetadata("templateId", intentSpec.getTemplateId())
                    .addMetadata("transformedAt", System.currentTimeMillis());

            // Transform each step using the mapper
            for (PetriIntentSpec.IntentStep step : intentSpec.getSteps()) {
                logger.debug("Transforming step: {} (type: {})", step.getId(), step.getType());
                mapper.mapStep(step, context, builder);
            }

            // Apply rule engine for complex patterns
            ruleEngine.applyRules(context, builder);

            // Build the final PetriNet
            PetriNet petriNet = builder.build();

            // Validate the generated PetriNet
            List<String> petriErrors = petriNet.validate();
            if (!petriErrors.isEmpty()) {
                throw new GrammarTransformationException("Generated PetriNet validation failed: " + petriErrors);
            }

            logger.info("Successfully transformed intent '{}' to PetriNet with {} places, {} transitions, {} arcs",
                    intentSpec.getName(), petriNet.getPlaces().size(),
                    petriNet.getTransitions().size(), petriNet.getArcs().size());

            return petriNet;

        } catch (Exception e) {
            logger.error("AutomationGrammar transformation failed for intent: {}", intentSpec.getName(), e);
            throw new GrammarTransformationException("Transformation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Transform with custom configuration
     *
     * @param intentSpec the intent specification
     * @param config transformation configuration
     * @return the generated PetriNet
     */
    public PetriNet transform(PetriIntentSpec intentSpec, TransformationConfig config)
            throws GrammarTransformationException {
        logger.info("Starting transformation with custom config for intent: {}", intentSpec.getName());

        // Apply custom configuration to mapper and rule engine
        mapper.configure(config);
        ruleEngine.configure(config);

        return transform(intentSpec);
    }

    /**
     * Validate that an intent can be transformed
     *
     * @param intentSpec the intent to validate
     * @return validation result with any issues
     */
    public ValidationResult validateTransformability(PetriIntentSpec intentSpec) {
        try {
            List<String> errors = new ArrayList<>(intentSpec.validate());

            // Check for unsupported step combinations
            if (intentSpec.getSteps().stream().anyMatch(step -> step.getType() == null)) {
                errors.add("All steps must have a valid type");
            }

            // Check for complex dependency cycles that can't be represented
            if (hasUnsupportedCycles(intentSpec)) {
                errors.add("Intent contains dependency cycles that cannot be mapped to Petri nets");
            }

            // Check for unsupported step type combinations
            if (hasUnsupportedStepCombinations(intentSpec)) {
                errors.add("Intent contains step type combinations that are not supported");
            }

            return new ValidationResult(errors.isEmpty(), errors);

        } catch (Exception e) {
            return new ValidationResult(false, Arrays.asList("Validation failed: " + e.getMessage()));
        }
    }

    /**
     * Get transformation statistics for debugging
     *
     * @param intentSpec the intent to analyze
     * @return transformation statistics
     */
    public TransformationStats getTransformationStats(PetriIntentSpec intentSpec) {
        TransformationStats stats = new TransformationStats();
        stats.setInputStepCount(intentSpec.getSteps().size());
        stats.setActionSteps(intentSpec.getActionSteps().size());
        stats.setChoiceSteps(intentSpec.getChoiceSteps().size());
        stats.setParallelSteps(intentSpec.getParallelSteps().size());

        // Estimate output complexity
        int estimatedPlaces = intentSpec.getSteps().size() * 2; // Rough estimate
        int estimatedTransitions = intentSpec.getActionSteps().size() +
                                  intentSpec.getChoiceSteps().size() +
                                  intentSpec.getParallelSteps().size();

        stats.setEstimatedPlaces(estimatedPlaces);
        stats.setEstimatedTransitions(estimatedTransitions);

        return stats;
    }

    private boolean hasUnsupportedCycles(PetriIntentSpec intentSpec) {
        // This would implement cycle detection that can't be represented in Petri nets
        // For now, basic implementation - can be enhanced later
        return false;
    }

    private boolean hasUnsupportedStepCombinations(PetriIntentSpec intentSpec) {
        // Check for specific combinations that are hard to map
        // For now, we support all current types
        return false;
    }

    /**
     * Context object that tracks the transformation state
     */
    public static class TransformationContext {
        private final PetriIntentSpec intentSpec;
        private final Map<String, String> stepToPlaceMap = new HashMap<>();
        private final Map<String, String> stepToTransitionMap = new HashMap<>();
        private final Set<String> processedSteps = new HashSet<>();
        private int placeCounter = 0;
        private int transitionCounter = 0;

        public TransformationContext(PetriIntentSpec intentSpec) {
            this.intentSpec = intentSpec;
        }

        public PetriIntentSpec getIntentSpec() { return intentSpec; }
        public Map<String, String> getStepToPlaceMap() { return stepToPlaceMap; }
        public Map<String, String> getStepToTransitionMap() { return stepToTransitionMap; }
        public Set<String> getProcessedSteps() { return processedSteps; }

        public String generatePlaceId(String stepId) {
            return "place_" + stepId + "_" + (++placeCounter);
        }

        public String generateTransitionId(String stepId) {
            return "transition_" + stepId + "_" + (++transitionCounter);
        }

        public void markStepProcessed(String stepId) {
            processedSteps.add(stepId);
        }

        public boolean isStepProcessed(String stepId) {
            return processedSteps.contains(stepId);
        }
    }

    /**
     * Configuration for transformation behavior
     */
    public static class TransformationConfig {
        private boolean enableGuardConditions = true;
        private boolean optimizeSequentialChains = true;
        private boolean addDebugMetadata = false;
        private String namingStrategy = "descriptive"; // or "minimal"

        // Getters and setters
        public boolean isEnableGuardConditions() { return enableGuardConditions; }
        public void setEnableGuardConditions(boolean enableGuardConditions) {
            this.enableGuardConditions = enableGuardConditions;
        }

        public boolean isOptimizeSequentialChains() { return optimizeSequentialChains; }
        public void setOptimizeSequentialChains(boolean optimizeSequentialChains) {
            this.optimizeSequentialChains = optimizeSequentialChains;
        }

        public boolean isAddDebugMetadata() { return addDebugMetadata; }
        public void setAddDebugMetadata(boolean addDebugMetadata) {
            this.addDebugMetadata = addDebugMetadata;
        }

        public String getNamingStrategy() { return namingStrategy; }
        public void setNamingStrategy(String namingStrategy) {
            this.namingStrategy = namingStrategy;
        }
    }

    /**
     * Validation result for transformability checks
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return new ArrayList<>(errors); }

        @Override
        public String toString() {
            return "ValidationResult{valid=" + valid + ", errors=" + errors + "}";
        }
    }

    /**
     * Statistics about the transformation
     */
    public static class TransformationStats {
        private int inputStepCount;
        private int actionSteps;
        private int choiceSteps;
        private int parallelSteps;
        private int estimatedPlaces;
        private int estimatedTransitions;

        // Getters and setters
        public int getInputStepCount() { return inputStepCount; }
        public void setInputStepCount(int inputStepCount) { this.inputStepCount = inputStepCount; }

        public int getActionSteps() { return actionSteps; }
        public void setActionSteps(int actionSteps) { this.actionSteps = actionSteps; }

        public int getChoiceSteps() { return choiceSteps; }
        public void setChoiceSteps(int choiceSteps) { this.choiceSteps = choiceSteps; }

        public int getParallelSteps() { return parallelSteps; }
        public void setParallelSteps(int parallelSteps) { this.parallelSteps = parallelSteps; }

        public int getEstimatedPlaces() { return estimatedPlaces; }
        public void setEstimatedPlaces(int estimatedPlaces) { this.estimatedPlaces = estimatedPlaces; }

        public int getEstimatedTransitions() { return estimatedTransitions; }
        public void setEstimatedTransitions(int estimatedTransitions) {
            this.estimatedTransitions = estimatedTransitions;
        }

        @Override
        public String toString() {
            return "TransformationStats{" +
                    "inputSteps=" + inputStepCount +
                    ", actionSteps=" + actionSteps +
                    ", choiceSteps=" + choiceSteps +
                    ", parallelSteps=" + parallelSteps +
                    ", estimatedPlaces=" + estimatedPlaces +
                    ", estimatedTransitions=" + estimatedTransitions +
                    '}';
        }
    }

    /**
     * Exception thrown during grammar transformation
     */
    public static class GrammarTransformationException extends Exception {
        public GrammarTransformationException(String message) {
            super(message);
        }

        public GrammarTransformationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}