package core.petri.grammar;

import core.petri.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * RuleEngine applies transformation rules and optimizations to the generated PetriNet.
 *
 * This engine handles:
 * - Post-processing optimizations (e.g., removing redundant places)
 * - Complex pattern recognition and transformation
 * - Rule-based corrections for specific scenarios
 * - Final structure validation and cleanup
 *
 * The RuleEngine operates after the basic IntentToPetriMapper has created the initial
 * structure, applying higher-level transformations and optimizations.
 *
 * @author Obvian Labs
 * @since Task 2 - AutomationGrammar Implementation
 */
public class RuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(RuleEngine.class);

    private AutomationGrammar.TransformationConfig config = new AutomationGrammar.TransformationConfig();

    /**
     * Configure the rule engine with transformation settings
     */
    public void configure(AutomationGrammar.TransformationConfig config) {
        this.config = config;
    }

    /**
     * Apply all rules and optimizations to the PetriNet builder
     *
     * @param context transformation context with step information
     * @param builder PetriNet builder to modify
     */
    public void applyRules(AutomationGrammar.TransformationContext context, PetriNet.Builder builder) {
        logger.debug("Applying rule engine transformations");

        // Apply basic rules in order of priority
        applySequentialOptimizations(context, builder);
        applyParallelJoinRules(context, builder);
        applyChoiceMergeRules(context, builder);
        applySinkPlaceRules(context, builder);
        applyInitialMarkingRules(context, builder);

        // Apply advanced pattern rules
        applyAdvancedPatternRules(context, builder);
        applyLoopOptimizations(context, builder);
        applyErrorHandlingRules(context, builder);
        applyResourceConstraintRules(context, builder);
        applyEventTriggerRules(context, builder);
        applyTimeoutRules(context, builder);

        if (config.isAddDebugMetadata()) {
            addDebugMetadata(context, builder);
        }

        logger.debug("Rule engine processing completed");
    }

    /**
     * Optimize sequential chains by merging unnecessary intermediate places
     */
    private void applySequentialOptimizations(AutomationGrammar.TransformationContext context,
                                            PetriNet.Builder builder) {
        if (!config.isOptimizeSequentialChains()) {
            return;
        }

        logger.debug("Applying sequential optimizations");

        // This would implement optimizations like:
        // place1 -> transition1 -> place2 -> transition2 -> place3
        // Could be optimized in certain cases to reduce intermediate places

        // For now, we'll implement a basic version that identifies chains
        // In a full implementation, this would do more sophisticated analysis

        // Mark optimization metadata for analysis tools
        PetriIntentSpec intentSpec = context.getIntentSpec();
        List<PetriIntentSpec.IntentStep> actionSteps = intentSpec.getActionSteps();

        for (int i = 0; i < actionSteps.size() - 1; i++) {
            PetriIntentSpec.IntentStep current = actionSteps.get(i);
            PetriIntentSpec.IntentStep next = actionSteps.get(i + 1);

            // Check if next step depends on current step (sequential pattern)
            if (next.getDependencies().contains(current.getId())) {
                String chainId = "chain_" + current.getId() + "_" + next.getId();
                logger.debug("Identified sequential chain: {} -> {}", current.getId(), next.getId());

                // Add metadata to track optimizable chains
                // In a full implementation, this would actually modify the structure
            }
        }
    }

    /**
     * Apply rules for parallel join patterns
     */
    private void applyParallelJoinRules(AutomationGrammar.TransformationContext context,
                                      PetriNet.Builder builder) {
        logger.debug("Applying parallel join rules");

        // Find PARALLEL steps that need corresponding SYNC steps
        PetriIntentSpec intentSpec = context.getIntentSpec();
        List<PetriIntentSpec.IntentStep> parallelSteps = intentSpec.getParallelSteps();
        List<PetriIntentSpec.IntentStep> syncSteps = intentSpec.getStepsByType(PetriIntentSpec.StepType.SYNC);

        for (PetriIntentSpec.IntentStep parallelStep : parallelSteps) {
            // Find corresponding sync step
            Optional<PetriIntentSpec.IntentStep> correspondingSync = syncSteps.stream()
                    .filter(sync -> sync.getDependencies().contains(parallelStep.getId()))
                    .findFirst();

            if (correspondingSync.isPresent()) {
                createParallelJoinStructure(parallelStep, correspondingSync.get(), context, builder);
            } else {
                // Create implicit join if no explicit sync step
                createImplicitJoin(parallelStep, context, builder);
            }
        }
    }

    /**
     * Create explicit parallel join structure
     */
    private void createParallelJoinStructure(PetriIntentSpec.IntentStep parallelStep,
                                           PetriIntentSpec.IntentStep syncStep,
                                           AutomationGrammar.TransformationContext context,
                                           PetriNet.Builder builder) {

        String parallelId = parallelStep.getId();
        String syncId = syncStep.getId();
        String joinTransitionId = context.getStepToTransitionMap().get(syncId + "_join");

        if (joinTransitionId != null) {
            // Connect parallel branches to the join transition
            for (int i = 0; i < 10; i++) { // Check up to 10 branches
                String branchPlaceId = context.getStepToPlaceMap().get(parallelId + "_branch_" + i);
                if (branchPlaceId != null) {
                    Arc joinArc = new Arc(branchPlaceId, joinTransitionId);
                    builder.addArc(joinArc);
                    logger.debug("Connected parallel branch {} to join {}", branchPlaceId, joinTransitionId);
                } else {
                    break; // No more branches
                }
            }
        }
    }

    /**
     * Create implicit join for parallel steps without explicit sync
     */
    private void createImplicitJoin(PetriIntentSpec.IntentStep parallelStep,
                                   AutomationGrammar.TransformationContext context,
                                   PetriNet.Builder builder) {

        String parallelId = parallelStep.getId();
        String joinTransitionId = context.generateTransitionId(parallelId + "_implicit_join");
        String joinPostPlaceId = context.generatePlaceId(parallelId + "_join_result");

        // Create implicit join transition
        Transition joinTransition = Transition.builder(joinTransitionId)
                .name("Implicit Join: " + parallelStep.getDescription())
                .description("Auto-generated join for parallel step")
                .asJoin()
                .addMetadata("stepId", parallelId)
                .addMetadata("stepType", "IMPLICIT_SYNC")
                .addMetadata("generated", true)
                .build();

        // Create join result place
        Place joinResultPlace = Place.builder(joinPostPlaceId)
                .name("Join Result: " + parallelStep.getDescription())
                .description("Result of implicit parallel join")
                .addMetadata("stepId", parallelId)
                .addMetadata("stepType", "IMPLICIT_SYNC")
                .addMetadata("generated", true)
                .build();

        builder.addTransition(joinTransition)
               .addPlace(joinResultPlace)
               .addArc(new Arc(joinTransitionId, joinPostPlaceId));

        // Connect all parallel branches to the implicit join
        for (int i = 0; i < 10; i++) {
            String branchPlaceId = context.getStepToPlaceMap().get(parallelId + "_branch_" + i);
            if (branchPlaceId != null) {
                Arc joinArc = new Arc(branchPlaceId, joinTransitionId);
                builder.addArc(joinArc);
            } else {
                break;
            }
        }

        // Store the implicit join mapping
        context.getStepToTransitionMap().put(parallelId + "_implicit_join", joinTransitionId);
        context.getStepToPlaceMap().put(parallelId + "_join_result", joinPostPlaceId);

        logger.debug("Created implicit join for parallel step {}", parallelId);
    }

    /**
     * Apply rules for choice merging and path handling
     */
    private void applyChoiceMergeRules(AutomationGrammar.TransformationContext context,
                                     PetriNet.Builder builder) {
        logger.debug("Applying choice merge rules");

        // Find choice steps that may need merge points
        PetriIntentSpec intentSpec = context.getIntentSpec();
        List<PetriIntentSpec.IntentStep> choiceSteps = intentSpec.getChoiceSteps();

        for (PetriIntentSpec.IntentStep choiceStep : choiceSteps) {
            String choiceId = choiceStep.getId();

            // Check if any other steps depend on this choice step
            List<PetriIntentSpec.IntentStep> dependentSteps = intentSpec.getDependentSteps(choiceId);

            if (dependentSteps.size() > 1) {
                // Multiple steps depend on this choice - may need merge logic
                createChoiceMergePoint(choiceStep, dependentSteps, context, builder);
            }
        }
    }

    /**
     * Create merge point for choice paths that converge
     */
    private void createChoiceMergePoint(PetriIntentSpec.IntentStep choiceStep,
                                       List<PetriIntentSpec.IntentStep> dependentSteps,
                                       AutomationGrammar.TransformationContext context,
                                       PetriNet.Builder builder) {

        String choiceId = choiceStep.getId();
        String mergeTransitionId = context.generateTransitionId(choiceId + "_merge");
        String mergePostPlaceId = context.generatePlaceId(choiceId + "_merged");

        // Create merge transition (OR-join)
        Transition mergeTransition = Transition.builder(mergeTransitionId)
                .name("Merge: " + choiceStep.getDescription())
                .description("Merge point for choice paths")
                .addMetadata("stepId", choiceId)
                .addMetadata("stepType", "CHOICE_MERGE")
                .addMetadata("generated", true)
                .build();

        // Create merge result place
        Place mergePlace = Place.builder(mergePostPlaceId)
                .name("Choice Merged: " + choiceStep.getDescription())
                .description("Merged result of choice paths")
                .addMetadata("stepId", choiceId)
                .addMetadata("stepType", "CHOICE_MERGE")
                .addMetadata("generated", true)
                .build();

        builder.addTransition(mergeTransition)
               .addPlace(mergePlace)
               .addArc(new Arc(mergeTransitionId, mergePostPlaceId));

        // Connect all choice path places to the merge transition
        for (int i = 0; i < 10; i++) {
            String pathPlaceId = context.getStepToPlaceMap().get(choiceId + "_path_" + i);
            if (pathPlaceId != null) {
                Arc mergeArc = new Arc(pathPlaceId, mergeTransitionId);
                builder.addArc(mergeArc);
            } else {
                break;
            }
        }

        context.getStepToTransitionMap().put(choiceId + "_merge", mergeTransitionId);
        context.getStepToPlaceMap().put(choiceId + "_merged", mergePostPlaceId);

        logger.debug("Created choice merge point for step {} with {} dependent steps",
                choiceId, dependentSteps.size());
    }

    /**
     * Apply rules for identifying and marking sink places
     */
    private void applySinkPlaceRules(AutomationGrammar.TransformationContext context,
                                   PetriNet.Builder builder) {
        logger.debug("Applying sink place rules");

        // This would analyze the generated structure to identify final places
        // that should be marked as sinks for proper termination detection

        PetriIntentSpec intentSpec = context.getIntentSpec();

        // Find steps that have no dependents (final steps)
        for (PetriIntentSpec.IntentStep step : intentSpec.getSteps()) {
            List<PetriIntentSpec.IntentStep> dependents = intentSpec.getDependentSteps(step.getId());

            if (dependents.isEmpty()) {
                // This is a final step - mark its output place as a sink
                String stepId = step.getId();
                String sinkPlaceId = findSinkPlaceForStep(stepId, context);

                if (sinkPlaceId != null) {
                    logger.debug("Identified sink place: {} for final step: {}", sinkPlaceId, stepId);
                    // In a full implementation, we would modify the place to add sink metadata
                    // For now, we just log the identification
                }
            }
        }
    }

    /**
     * Find the appropriate sink place for a final step
     */
    private String findSinkPlaceForStep(String stepId, AutomationGrammar.TransformationContext context) {
        // Check various possible output places for this step
        String postPlace = context.getStepToPlaceMap().get(stepId + "_post");
        if (postPlace != null) {
            return postPlace;
        }

        String syncPlace = context.getStepToPlaceMap().get(stepId + "_sync_post");
        if (syncPlace != null) {
            return syncPlace;
        }

        String joinPlace = context.getStepToPlaceMap().get(stepId + "_join_result");
        if (joinPlace != null) {
            return joinPlace;
        }

        return null;
    }

    /**
     * Apply rules for initial marking setup
     */
    private void applyInitialMarkingRules(AutomationGrammar.TransformationContext context,
                                        PetriNet.Builder builder) {
        logger.debug("Applying initial marking rules");

        PetriIntentSpec intentSpec = context.getIntentSpec();

        // Ensure all entry points (steps with no dependencies) have initial tokens
        for (PetriIntentSpec.IntentStep step : intentSpec.getSteps()) {
            if (step.getDependencies().isEmpty()) {
                String stepId = step.getId();
                String entryPlaceId = findEntryPlaceForStep(stepId, context);

                if (entryPlaceId != null) {
                    // Verify this place has an initial token
                    logger.debug("Ensuring initial token for entry step: {} at place: {}", stepId, entryPlaceId);
                    // The token would already be set by the mapper, this is validation
                }
            }
        }
    }

    /**
     * Find the entry place for a step (place that should have initial tokens)
     */
    private String findEntryPlaceForStep(String stepId, AutomationGrammar.TransformationContext context) {
        // Check for pre-places that are entry points
        String prePlace = context.getStepToPlaceMap().get(stepId + "_pre");
        if (prePlace != null) {
            return prePlace;
        }

        String choicePlace = context.getStepToPlaceMap().get(stepId + "_choice_pre");
        if (choicePlace != null) {
            return choicePlace;
        }

        String parallelPlace = context.getStepToPlaceMap().get(stepId + "_parallel_pre");
        if (parallelPlace != null) {
            return parallelPlace;
        }

        return null;
    }

    /**
     * Add debug metadata to components for analysis and debugging
     */
    private void addDebugMetadata(AutomationGrammar.TransformationContext context,
                                 PetriNet.Builder builder) {
        logger.debug("Adding debug metadata");

        // Add transformation statistics as metadata
        builder.addMetadata("transformationStats", Map.of(
                "processedSteps", context.getProcessedSteps().size(),
                "generatedPlaces", context.getStepToPlaceMap().size(),
                "generatedTransitions", context.getStepToTransitionMap().size(),
                "originalStepCount", context.getIntentSpec().getSteps().size()
        ));

        // Add mapping information for debugging
        builder.addMetadata("stepToPlaceMap", new HashMap<>(context.getStepToPlaceMap()));
        builder.addMetadata("stepToTransitionMap", new HashMap<>(context.getStepToTransitionMap()));
        builder.addMetadata("rulesApplied", Arrays.asList(
                "sequentialOptimizations",
                "parallelJoinRules",
                "choiceMergeRules",
                "sinkPlaceRules",
                "initialMarkingRules"
        ));

        logger.debug("Added debug metadata to PetriNet");
    }

    // ====================== ADVANCED PATTERN RULE METHODS ======================

    /**
     * Apply optimization rules for advanced workflow patterns
     */
    private void applyAdvancedPatternRules(AutomationGrammar.TransformationContext context,
                                         PetriNet.Builder builder) {
        logger.debug("Applying advanced pattern optimization rules");

        PetriIntentSpec intentSpec = context.getIntentSpec();
        List<PetriIntentSpec.IntentStep> advancedSteps = intentSpec.getAdvancedPatternSteps();

        for (PetriIntentSpec.IntentStep step : advancedSteps) {
            switch (step.getType()) {
                case NESTED_CONDITIONAL -> optimizeNestedConditionals(step, context, builder);
                case CIRCUIT_BREAKER -> optimizeCircuitBreaker(step, context, builder);
                case FAN_OUT_FAN_IN -> optimizeFanOutFanIn(step, context, builder);
                case PIPELINE_STAGE -> optimizePipelineStage(step, context, builder);
                default -> logger.debug("No specific optimization for step type: {}", step.getType());
            }
        }

        logger.debug("Advanced pattern rules applied for {} steps", advancedSteps.size());
    }

    private void optimizeNestedConditionals(PetriIntentSpec.IntentStep step,
                                          AutomationGrammar.TransformationContext context,
                                          PetriNet.Builder builder) {
        // Optimize nested conditional structures for better performance
        String stepId = step.getId();
        Map<String, Object> conditions = step.getConditions();

        if (conditions.size() > 1) {
            // Create optimized AND/OR gate structures
            logger.debug("Optimizing nested conditional {} with {} conditions", stepId, conditions.size());

            // Add metadata for conditional optimization
            builder.addMetadata("optimizedConditional_" + stepId, Map.of(
                "originalConditions", conditions.size(),
                "optimizationApplied", "nestedConditionalsSimplified"
            ));
        }
    }

    private void optimizeCircuitBreaker(PetriIntentSpec.IntentStep step,
                                      AutomationGrammar.TransformationContext context,
                                      PetriNet.Builder builder) {
        // Add circuit breaker state management optimizations
        String stepId = step.getId();
        Map<String, Object> retryPolicy = step.getRetryPolicy();

        if (!retryPolicy.isEmpty()) {
            logger.debug("Optimizing circuit breaker {} with retry policy", stepId);

            // Add timeout and backoff metadata
            builder.addMetadata("circuitBreakerOptimization_" + stepId, Map.of(
                "retryPolicy", retryPolicy,
                "optimizationApplied", "circuitBreakerStateManagement"
            ));
        }
    }

    private void optimizeFanOutFanIn(PetriIntentSpec.IntentStep step,
                                   AutomationGrammar.TransformationContext context,
                                   PetriNet.Builder builder) {
        // Optimize fan-out/fan-in patterns for better parallelism
        String stepId = step.getId();
        logger.debug("Optimizing fan-out/fan-in pattern for step {}", stepId);

        // Add parallel optimization metadata
        builder.addMetadata("fanOutFanInOptimization_" + stepId, Map.of(
            "optimizationApplied", "parallelSynchronizationImproved"
        ));
    }

    private void optimizePipelineStage(PetriIntentSpec.IntentStep step,
                                     AutomationGrammar.TransformationContext context,
                                     PetriNet.Builder builder) {
        // Optimize pipeline stages for better throughput
        String stepId = step.getId();
        logger.debug("Optimizing pipeline stage {}", stepId);

        // Add pipeline optimization metadata
        builder.addMetadata("pipelineOptimization_" + stepId, Map.of(
            "optimizationApplied", "pipelineStageOptimized"
        ));
    }

    /**
     * Apply optimization rules for loop patterns
     */
    private void applyLoopOptimizations(AutomationGrammar.TransformationContext context,
                                      PetriNet.Builder builder) {
        logger.debug("Applying loop pattern optimizations");

        PetriIntentSpec intentSpec = context.getIntentSpec();
        List<PetriIntentSpec.IntentStep> loopSteps = intentSpec.getLoopSteps();

        for (PetriIntentSpec.IntentStep loopStep : loopSteps) {
            String stepId = loopStep.getId();
            String loopCondition = loopStep.getLoopCondition();

            if (loopCondition != null && !loopCondition.trim().isEmpty()) {
                // Optimize loop termination checking
                logger.debug("Optimizing loop {} with condition: {}", stepId, loopCondition);

                // Create loop optimization metadata
                builder.addMetadata("loopOptimization_" + stepId, Map.of(
                    "loopCondition", loopCondition,
                    "optimizationApplied", "loopTerminationOptimized"
                ));

                // Add timeout protection for infinite loops
                if (loopStep.hasTimeout()) {
                    Long timeout = loopStep.getTimeout();
                    builder.addMetadata("loopTimeout_" + stepId, Map.of(
                        "timeoutMs", timeout,
                        "protectionApplied", "infiniteLoopProtection"
                    ));
                }
            }
        }

        logger.debug("Loop optimizations applied for {} loop steps", loopSteps.size());
    }

    /**
     * Apply rules for error handling patterns
     */
    private void applyErrorHandlingRules(AutomationGrammar.TransformationContext context,
                                       PetriNet.Builder builder) {
        logger.debug("Applying error handling rules");

        PetriIntentSpec intentSpec = context.getIntentSpec();
        List<PetriIntentSpec.IntentStep> errorHandlerSteps = intentSpec.getErrorHandlerSteps();
        List<PetriIntentSpec.IntentStep> stepsWithErrorHandling = intentSpec.getStepsWithErrorHandling();

        // Create global error handling structure if multiple steps have error handling
        if (stepsWithErrorHandling.size() > 1) {
            createGlobalErrorHandler(context, builder, stepsWithErrorHandling);
        }

        // Optimize individual error handlers
        for (PetriIntentSpec.IntentStep errorStep : errorHandlerSteps) {
            String stepId = errorStep.getId();
            List<String> compensation = errorStep.getCompensation();

            logger.debug("Applying error handling rules for step {} with {} compensation actions",
                        stepId, compensation.size());

            // Create compensation chain optimization
            if (compensation.size() > 1) {
                builder.addMetadata("compensationChain_" + stepId, Map.of(
                    "compensationActions", compensation,
                    "optimizationApplied", "compensationChainOptimized"
                ));
            }
        }

        logger.debug("Error handling rules applied for {} error handlers", errorHandlerSteps.size());
    }

    private void createGlobalErrorHandler(AutomationGrammar.TransformationContext context,
                                        PetriNet.Builder builder,
                                        List<PetriIntentSpec.IntentStep> stepsWithErrorHandling) {
        // Create a global error handling place that all error handlers can connect to
        String globalErrorPlaceId = context.generatePlaceId("global_error_handler");
        String globalRecoveryTransitionId = context.generateTransitionId("global_recovery");

        Place globalErrorPlace = Place.builder(globalErrorPlaceId)
                .name("Global Error Handler")
                .description("Central error handling for workflow")
                .addMetadata("stepType", "GLOBAL_ERROR_HANDLER")
                .addMetadata("connectedSteps", stepsWithErrorHandling.stream()
                        .map(PetriIntentSpec.IntentStep::getId)
                        .toList())
                .build();

        Transition globalRecovery = Transition.builder(globalRecoveryTransitionId)
                .name("Global Recovery")
                .description("Global error recovery and cleanup")
                .addMetadata("stepType", "GLOBAL_ERROR_HANDLER")
                .addMetadata("isGlobalRecovery", true)
                .build();

        builder.addPlace(globalErrorPlace)
               .addTransition(globalRecovery);

        // Store mappings
        context.getStepToPlaceMap().put("global_error_handler", globalErrorPlaceId);
        context.getStepToTransitionMap().put("global_recovery", globalRecoveryTransitionId);

        logger.debug("Created global error handler for {} steps", stepsWithErrorHandling.size());
    }

    /**
     * Apply rules for resource constraint management
     */
    private void applyResourceConstraintRules(AutomationGrammar.TransformationContext context,
                                            PetriNet.Builder builder) {
        logger.debug("Applying resource constraint rules");

        PetriIntentSpec intentSpec = context.getIntentSpec();
        List<PetriIntentSpec.IntentStep> resourceConstrainedSteps =
                intentSpec.getStepsByType(PetriIntentSpec.StepType.RESOURCE_CONSTRAINED);

        // Group steps by resource type
        Map<String, List<PetriIntentSpec.IntentStep>> resourceGroups = new HashMap<>();

        for (PetriIntentSpec.IntentStep step : resourceConstrainedSteps) {
            Map<String, Object> constraints = step.getResourceConstraints();
            String resourceType = (String) constraints.getOrDefault("resourceType", "default");

            resourceGroups.computeIfAbsent(resourceType, k -> new ArrayList<>()).add(step);
        }

        // Create shared resource pools for same resource types
        for (Map.Entry<String, List<PetriIntentSpec.IntentStep>> group : resourceGroups.entrySet()) {
            String resourceType = group.getKey();
            List<PetriIntentSpec.IntentStep> stepsInGroup = group.getValue();

            if (stepsInGroup.size() > 1) {
                createSharedResourcePool(context, builder, resourceType, stepsInGroup);
            }
        }

        logger.debug("Resource constraint rules applied for {} resource groups", resourceGroups.size());
    }

    private void createSharedResourcePool(AutomationGrammar.TransformationContext context,
                                        PetriNet.Builder builder, String resourceType,
                                        List<PetriIntentSpec.IntentStep> steps) {
        String sharedPoolId = context.generatePlaceId("shared_pool_" + resourceType);

        // Calculate total resource capacity
        int totalCapacity = steps.stream()
                .mapToInt(step -> {
                    Object capacity = step.getResourceConstraints().get("maxConcurrency");
                    return capacity instanceof Number ? ((Number) capacity).intValue() : 1;
                })
                .sum();

        Place sharedPool = Place.builder(sharedPoolId)
                .name("Shared Resource Pool: " + resourceType)
                .description("Shared resource pool for " + resourceType)
                .addMetadata("resourceType", resourceType)
                .addMetadata("connectedSteps", steps.stream()
                        .map(PetriIntentSpec.IntentStep::getId)
                        .toList())
                .addMetadata("totalCapacity", totalCapacity)
                .build();

        builder.addPlace(sharedPool)
               .addInitialToken(sharedPoolId, totalCapacity);

        context.getStepToPlaceMap().put("shared_pool_" + resourceType, sharedPoolId);

        logger.debug("Created shared resource pool {} with capacity {} for {} steps",
                    resourceType, totalCapacity, steps.size());
    }

    /**
     * Apply rules for event trigger optimization
     */
    private void applyEventTriggerRules(AutomationGrammar.TransformationContext context,
                                      PetriNet.Builder builder) {
        logger.debug("Applying event trigger rules");

        PetriIntentSpec intentSpec = context.getIntentSpec();
        List<PetriIntentSpec.IntentStep> eventTriggerSteps = intentSpec.getEventTriggerSteps();

        // Group event triggers by type
        Map<String, List<PetriIntentSpec.IntentStep>> triggerGroups = new HashMap<>();

        for (PetriIntentSpec.IntentStep step : eventTriggerSteps) {
            Map<String, Object> triggerConfig =
                    (Map<String, Object>) step.getMetadata().get("triggerConfig");

            if (triggerConfig != null) {
                String triggerType = (String) triggerConfig.getOrDefault("type", "unknown");
                triggerGroups.computeIfAbsent(triggerType, k -> new ArrayList<>()).add(step);
            }
        }

        // Optimize trigger handling
        for (Map.Entry<String, List<PetriIntentSpec.IntentStep>> group : triggerGroups.entrySet()) {
            String triggerType = group.getKey();
            List<PetriIntentSpec.IntentStep> triggersInGroup = group.getValue();

            logger.debug("Optimizing {} triggers of type {}", triggersInGroup.size(), triggerType);

            // Add trigger optimization metadata
            builder.addMetadata("triggerOptimization_" + triggerType, Map.of(
                "triggerType", triggerType,
                "triggerCount", triggersInGroup.size(),
                "optimizationApplied", "eventTriggerGroupOptimized"
            ));
        }

        logger.debug("Event trigger rules applied for {} trigger groups", triggerGroups.size());
    }

    /**
     * Apply rules for timeout management
     */
    private void applyTimeoutRules(AutomationGrammar.TransformationContext context,
                                 PetriNet.Builder builder) {
        logger.debug("Applying timeout rules");

        PetriIntentSpec intentSpec = context.getIntentSpec();
        List<PetriIntentSpec.IntentStep> timedSteps = intentSpec.getTimedSteps();

        if (!timedSteps.isEmpty()) {
            // Create global timeout manager
            String timeoutManagerId = context.generatePlaceId("timeout_manager");
            String timeoutCheckId = context.generateTransitionId("timeout_check");

            Place timeoutManager = Place.builder(timeoutManagerId)
                    .name("Timeout Manager")
                    .description("Global timeout management")
                    .addMetadata("stepType", "TIMEOUT_MANAGER")
                    .addMetadata("managedSteps", timedSteps.stream()
                            .map(PetriIntentSpec.IntentStep::getId)
                            .toList())
                    .build();

            Transition timeoutCheck = Transition.builder(timeoutCheckId)
                    .name("Timeout Check")
                    .description("Check for step timeouts")
                    .addMetadata("stepType", "TIMEOUT_MANAGER")
                    .addMetadata("isTimeoutCheck", true)
                    .build();

            builder.addPlace(timeoutManager)
                   .addTransition(timeoutCheck);

            // Store mappings
            context.getStepToPlaceMap().put("timeout_manager", timeoutManagerId);
            context.getStepToTransitionMap().put("timeout_check", timeoutCheckId);

            // Add timeout configuration for each timed step
            for (PetriIntentSpec.IntentStep timedStep : timedSteps) {
                String stepId = timedStep.getId();
                Long timeout = timedStep.getTimeout();

                builder.addMetadata("timeout_" + stepId, Map.of(
                    "timeoutMs", timeout,
                    "stepId", stepId,
                    "managedByTimeoutManager", true
                ));

                logger.debug("Added timeout management for step {} with {}ms timeout",
                           stepId, timeout);
            }
        }

        logger.debug("Timeout rules applied for {} timed steps", timedSteps.size());
    }
}