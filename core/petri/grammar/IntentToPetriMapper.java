package core.petri.grammar;

import core.petri.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * IntentToPetriMapper handles the core transformation logic from IntentSteps to PetriNet components.
 *
 * Implements the formal transformation patterns:
 * - Sequential: A → B becomes place_A → transition_A → place_AB → transition_B → place_B
 * - Parallel: A ∥ B becomes AND-split/join with synchronization places
 * - Choice: A XOR B becomes single pre-place with conditional transitions
 *
 * This mapper applies the specific grammar rules for each IntentStep type and builds
 * the corresponding Places, Transitions, and Arcs in the target PetriNet.
 *
 * @author Obvian Labs
 * @since Task 2 - AutomationGrammar Implementation
 */
public class IntentToPetriMapper {

    private static final Logger logger = LoggerFactory.getLogger(IntentToPetriMapper.class);

    private AutomationGrammar.TransformationConfig config = new AutomationGrammar.TransformationConfig();

    /**
     * Configure the mapper with transformation settings
     */
    public void configure(AutomationGrammar.TransformationConfig config) {
        this.config = config;
    }

    /**
     * Map a single IntentStep to PetriNet components
     *
     * @param step the intent step to map
     * @param context transformation context
     * @param builder PetriNet builder to add components to
     */
    public void mapStep(PetriIntentSpec.IntentStep step,
                       AutomationGrammar.TransformationContext context,
                       PetriNet.Builder builder) {

        logger.debug("Mapping step {} of type {}", step.getId(), step.getType());

        switch (step.getType()) {
            case ACTION -> mapActionStep(step, context, builder);
            case SEQUENCE -> mapSequenceStep(step, context, builder);
            case CHOICE -> mapChoiceStep(step, context, builder);
            case PARALLEL -> mapParallelStep(step, context, builder);
            case SYNC -> mapSyncStep(step, context, builder);

            // Advanced workflow patterns
            case NESTED_CONDITIONAL -> mapNestedConditionalStep(step, context, builder);
            case LOOP -> mapLoopStep(step, context, builder);
            case EVENT_TRIGGER -> mapEventTriggerStep(step, context, builder);
            case ERROR_HANDLER -> mapErrorHandlerStep(step, context, builder);
            case COMPENSATION -> mapCompensationStep(step, context, builder);
            case CIRCUIT_BREAKER -> mapCircuitBreakerStep(step, context, builder);
            case FAN_OUT_FAN_IN -> mapFanOutFanInStep(step, context, builder);
            case PIPELINE_STAGE -> mapPipelineStageStep(step, context, builder);
            case RESOURCE_CONSTRAINED -> mapResourceConstrainedStep(step, context, builder);

            default -> throw new IllegalArgumentException("Unsupported step type: " + step.getType());
        }

        context.markStepProcessed(step.getId());
    }

    /**
     * Map ACTION step: Creates a simple transition with input and output places
     * Pattern: place_pre → transition_action → place_post
     */
    private void mapActionStep(PetriIntentSpec.IntentStep step,
                              AutomationGrammar.TransformationContext context,
                              PetriNet.Builder builder) {

        String stepId = step.getId();
        String prePlaceId = context.generatePlaceId(stepId + "_pre");
        String transitionId = context.generateTransitionId(stepId);
        String postPlaceId = context.generatePlaceId(stepId + "_post");

        // Create places
        Place prePlace = Place.builder(prePlaceId)
                .name(getPlaceName(step, "pre"))
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ACTION")
                .build();

        Place postPlace = Place.builder(postPlaceId)
                .name(getPlaceName(step, "post"))
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ACTION")
                .build();

        // Create transition
        Transition.Builder transitionBuilder = Transition.builder(transitionId)
                .name(getTransitionName(step))
                .description(step.getDescription())
                .action(step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ACTION");

        // Add guard condition if present
        if (step.hasGuard() && config.isEnableGuardConditions()) {
            transitionBuilder.guard(step.getWhen());
        }

        Transition transition = transitionBuilder.build();

        // Create arcs
        Arc inputArc = new Arc(prePlaceId, transitionId);
        Arc outputArc = new Arc(transitionId, postPlaceId);

        // Add components to builder
        builder.addPlace(prePlace)
               .addPlace(postPlace)
               .addTransition(transition)
               .addArc(inputArc)
               .addArc(outputArc);

        // Handle dependencies by connecting to predecessor steps
        connectDependencies(step, context, builder, prePlaceId);

        // Store mappings for future reference
        context.getStepToPlaceMap().put(stepId + "_pre", prePlaceId);
        context.getStepToPlaceMap().put(stepId + "_post", postPlaceId);
        context.getStepToTransitionMap().put(stepId, transitionId);

        // Set initial marking for steps with no dependencies (start places)
        if (step.getDependencies().isEmpty()) {
            builder.addInitialToken(prePlaceId, 1);
        }

        logger.debug("Mapped ACTION step {} to transition {} with pre-place {} and post-place {}",
                stepId, transitionId, prePlaceId, postPlaceId);
    }

    /**
     * Map SEQUENCE step: Creates a chain of connected places and transitions
     * Pattern: Connects multiple actions in sequence based on dependencies
     */
    private void mapSequenceStep(PetriIntentSpec.IntentStep step,
                                AutomationGrammar.TransformationContext context,
                                PetriNet.Builder builder) {

        // SEQUENCE steps are typically handled by dependency analysis
        // This creates a meta-structure for sequential execution
        String stepId = step.getId();
        String syncPlaceId = context.generatePlaceId(stepId + "_sync");

        Place syncPlace = Place.builder(syncPlaceId)
                .name(getPlaceName(step, "sync"))
                .description("Synchronization point for sequence: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "SEQUENCE")
                .build();

        builder.addPlace(syncPlace);
        context.getStepToPlaceMap().put(stepId + "_sync", syncPlaceId);

        logger.debug("Mapped SEQUENCE step {} to sync place {}", stepId, syncPlaceId);
    }

    /**
     * Map CHOICE step: Creates XOR branching with conditional transitions
     * Pattern: place_pre → [transition_choice1|guard1, transition_choice2|guard2] → [place_path1, place_path2]
     */
    private void mapChoiceStep(PetriIntentSpec.IntentStep step,
                              AutomationGrammar.TransformationContext context,
                              PetriNet.Builder builder) {

        String stepId = step.getId();
        String prePlaceId = context.generatePlaceId(stepId + "_choice_pre");

        // Create pre-place for the choice
        Place prePlace = Place.builder(prePlaceId)
                .name(getPlaceName(step, "choice"))
                .description("Choice point: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "CHOICE")
                .build();

        builder.addPlace(prePlace);

        // Get choice paths from metadata
        @SuppressWarnings("unchecked")
        List<String> paths = (List<String>) step.getMetadata().getOrDefault("paths", new ArrayList<>());

        // Create transition and post-place for each choice path
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            String transitionId = context.generateTransitionId(stepId + "_choice_" + i);
            String postPlaceId = context.generatePlaceId(stepId + "_path_" + i);

            // Create choice transition
            Transition choiceTransition = Transition.builder(transitionId)
                    .name("Choice: " + path)
                    .description("Choice branch: " + path)
                    .action(path)
                    .asChoice("condition_" + i)
                    .addMetadata("stepId", stepId)
                    .addMetadata("stepType", "CHOICE")
                    .addMetadata("pathIndex", i)
                    .addMetadata("pathDescription", path)
                    .build();

            // Create post-place for this path
            Place postPlace = Place.builder(postPlaceId)
                    .name("Path " + i + ": " + path)
                    .description("Result of choice path: " + path)
                    .addMetadata("stepId", stepId)
                    .addMetadata("stepType", "CHOICE")
                    .addMetadata("pathIndex", i)
                    .build();

            // Create arcs
            Arc inputArc = new Arc(prePlaceId, transitionId);
            Arc outputArc = new Arc(transitionId, postPlaceId);

            builder.addTransition(choiceTransition)
                   .addPlace(postPlace)
                   .addArc(inputArc)
                   .addArc(outputArc);

            // Store path mappings
            context.getStepToTransitionMap().put(stepId + "_choice_" + i, transitionId);
            context.getStepToPlaceMap().put(stepId + "_path_" + i, postPlaceId);
        }

        // Handle dependencies
        connectDependencies(step, context, builder, prePlaceId);
        context.getStepToPlaceMap().put(stepId + "_choice_pre", prePlaceId);

        // Set initial marking if no dependencies
        if (step.getDependencies().isEmpty()) {
            builder.addInitialToken(prePlaceId, 1);
        }

        logger.debug("Mapped CHOICE step {} with {} paths", stepId, paths.size());
    }

    /**
     * Map PARALLEL step: Creates AND-split/join pattern
     * Pattern: place_pre → transition_fork → [place_branch1, place_branch2] → transition_join → place_post
     */
    private void mapParallelStep(PetriIntentSpec.IntentStep step,
                                AutomationGrammar.TransformationContext context,
                                PetriNet.Builder builder) {

        String stepId = step.getId();
        String prePlaceId = context.generatePlaceId(stepId + "_parallel_pre");
        String forkTransitionId = context.generateTransitionId(stepId + "_fork");

        // Create pre-place
        Place prePlace = Place.builder(prePlaceId)
                .name(getPlaceName(step, "parallel"))
                .description("Parallel entry: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "PARALLEL")
                .build();

        // Create fork transition (AND-split)
        Transition forkTransition = Transition.builder(forkTransitionId)
                .name("Fork: " + step.getDescription())
                .description("AND-split for parallel execution")
                .asFork()
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "PARALLEL")
                .build();

        builder.addPlace(prePlace)
               .addTransition(forkTransition)
               .addArc(new Arc(prePlaceId, forkTransitionId));

        // Create parallel branches - for demo, create 2 branches
        // In real implementation, this would be derived from dependencies or metadata
        int branchCount = 2; // This could be configurable or derived from metadata
        List<String> branchPlaces = new ArrayList<>();

        for (int i = 0; i < branchCount; i++) {
            String branchPlaceId = context.generatePlaceId(stepId + "_branch_" + i);

            Place branchPlace = Place.builder(branchPlaceId)
                    .name("Branch " + i + ": " + step.getDescription())
                    .description("Parallel branch " + i)
                    .addMetadata("stepId", stepId)
                    .addMetadata("stepType", "PARALLEL")
                    .addMetadata("branchIndex", i)
                    .build();

            builder.addPlace(branchPlace)
                   .addArc(new Arc(forkTransitionId, branchPlaceId));

            branchPlaces.add(branchPlaceId);
            context.getStepToPlaceMap().put(stepId + "_branch_" + i, branchPlaceId);
        }

        // Handle dependencies
        connectDependencies(step, context, builder, prePlaceId);
        context.getStepToPlaceMap().put(stepId + "_parallel_pre", prePlaceId);
        context.getStepToTransitionMap().put(stepId + "_fork", forkTransitionId);

        // Set initial marking if no dependencies
        if (step.getDependencies().isEmpty()) {
            builder.addInitialToken(prePlaceId, 1);
        }

        logger.debug("Mapped PARALLEL step {} with {} branches", stepId, branchCount);
    }

    /**
     * Map SYNC step: Creates synchronization point (AND-join)
     * Pattern: [place_branch1, place_branch2] → transition_join → place_post
     */
    private void mapSyncStep(PetriIntentSpec.IntentStep step,
                            AutomationGrammar.TransformationContext context,
                            PetriNet.Builder builder) {

        String stepId = step.getId();
        String joinTransitionId = context.generateTransitionId(stepId + "_join");
        String postPlaceId = context.generatePlaceId(stepId + "_sync_post");

        // Create join transition (AND-join)
        Transition joinTransition = Transition.builder(joinTransitionId)
                .name("Join: " + step.getDescription())
                .description("AND-join for synchronization")
                .asJoin()
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "SYNC")
                .build();

        // Create post-place
        Place postPlace = Place.builder(postPlaceId)
                .name(getPlaceName(step, "sync_result"))
                .description("Synchronization result: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "SYNC")
                .build();

        builder.addTransition(joinTransition)
               .addPlace(postPlace)
               .addArc(new Arc(joinTransitionId, postPlaceId));

        // Connect dependencies (multiple input branches)
        connectDependencies(step, context, builder, joinTransitionId);

        context.getStepToTransitionMap().put(stepId + "_join", joinTransitionId);
        context.getStepToPlaceMap().put(stepId + "_sync_post", postPlaceId);

        logger.debug("Mapped SYNC step {} to join transition {}", stepId, joinTransitionId);
    }

    /**
     * Connect step dependencies by creating arcs from predecessor steps
     * Ensures proper Petri net semantics: place → transition → place
     */
    private void connectDependencies(PetriIntentSpec.IntentStep step,
                                   AutomationGrammar.TransformationContext context,
                                   PetriNet.Builder builder,
                                   String targetNodeId) {

        for (String dependencyId : step.getDependencies()) {
            // Find the output place of the dependency
            String sourceNodeId = findSourceNodeForDependency(dependencyId, context);

            if (sourceNodeId != null) {
                // Check if target is a place or transition
                boolean targetIsPlace = targetNodeId.startsWith("place_");

                if (targetIsPlace) {
                    // Need to create intermediate transition: sourcePlace → intermediateTransition → targetPlace
                    String intermediateTransitionId = context.generateTransitionId(step.getId() + "_dep_" + dependencyId);

                    Transition intermediateTransition = Transition.builder(intermediateTransitionId)
                            .name("Dependency: " + dependencyId + " → " + step.getId())
                            .description("Connects " + dependencyId + " to " + step.getId())
                            .addMetadata("isDependencyConnector", true)
                            .addMetadata("sourceStep", dependencyId)
                            .addMetadata("targetStep", step.getId())
                            .build();

                    builder.addTransition(intermediateTransition)
                           .addArc(new Arc(sourceNodeId, intermediateTransitionId))
                           .addArc(new Arc(intermediateTransitionId, targetNodeId));

                    logger.debug("Connected dependency {} → {} → {} via intermediate transition",
                                sourceNodeId, intermediateTransitionId, targetNodeId);
                } else {
                    // Target is a transition, can connect directly from place
                    Arc dependencyArc = new Arc(sourceNodeId, targetNodeId);
                    builder.addArc(dependencyArc);

                    logger.debug("Connected dependency {} → {} via direct arc", sourceNodeId, targetNodeId);
                }
            } else {
                logger.warn("Could not find source node for dependency: {}", dependencyId);
            }
        }
    }

    /**
     * Find the output node (place or transition) for a dependency step
     */
    private String findSourceNodeForDependency(String dependencyId,
                                             AutomationGrammar.TransformationContext context) {

        // Look for post-place first (most common case)
        String postPlace = context.getStepToPlaceMap().get(dependencyId + "_post");
        if (postPlace != null) {
            return postPlace;
        }

        // Look for sync result place
        String syncPlace = context.getStepToPlaceMap().get(dependencyId + "_sync_post");
        if (syncPlace != null) {
            return syncPlace;
        }

        // Look for choice/parallel branches - use the first available
        for (int i = 0; i < 10; i++) { // Reasonable upper bound
            String pathPlace = context.getStepToPlaceMap().get(dependencyId + "_path_" + i);
            if (pathPlace != null) {
                return pathPlace;
            }

            String branchPlace = context.getStepToPlaceMap().get(dependencyId + "_branch_" + i);
            if (branchPlace != null) {
                return branchPlace;
            }
        }

        return null;
    }

    /**
     * Generate place names based on naming strategy
     */
    private String getPlaceName(PetriIntentSpec.IntentStep step, String suffix) {
        if ("minimal".equals(config.getNamingStrategy())) {
            return step.getId() + "_" + suffix;
        } else {
            // Descriptive, human-readable naming
            String desc = step.getDescription();
            if (desc == null || desc.trim().isEmpty()) {
                desc = step.getId();
            }

            // Truncate if too long
            if (desc.length() > 35) {
                desc = desc.substring(0, 32) + "...";
            }

            // Create natural language names
            if ("pre".equals(suffix)) {
                return "Ready: " + desc;
            } else if ("post".equals(suffix)) {
                return "Done: " + desc;
            } else if ("waiting".equals(suffix)) {
                return "Waiting: " + desc;
            } else if ("complete".equals(suffix)) {
                return "Completed: " + desc;
            } else {
                return desc + " (" + suffix + ")";
            }
        }
    }

    /**
     * Generate transition names based on naming strategy
     */
    private String getTransitionName(PetriIntentSpec.IntentStep step) {
        if ("minimal".equals(config.getNamingStrategy())) {
            return step.getId();
        } else {
            // Descriptive, human-readable naming
            String desc = step.getDescription();
            if (desc == null || desc.trim().isEmpty()) {
                desc = step.getId();
            }

            // Truncate if too long
            if (desc.length() > 45) {
                desc = desc.substring(0, 42) + "...";
            }

            // Capitalize first letter for professional appearance
            if (desc.length() > 0 && Character.isLowerCase(desc.charAt(0))) {
                desc = Character.toUpperCase(desc.charAt(0)) + desc.substring(1);
            }

            return desc;
        }
    }

    // ====================== ADVANCED PATTERN MAPPING METHODS ======================

    /**
     * Map NESTED_CONDITIONAL step: Creates complex decision trees with AND/OR logic
     * Pattern: Nested if/then/else structures with multiple condition evaluation
     */
    private void mapNestedConditionalStep(PetriIntentSpec.IntentStep step,
                                         AutomationGrammar.TransformationContext context,
                                         PetriNet.Builder builder) {

        String stepId = step.getId();
        String rootPlaceId = context.generatePlaceId(stepId + "_conditional_root");

        // Create root place for the nested conditional
        Place rootPlace = Place.builder(rootPlaceId)
                .name(getPlaceName(step, "conditional_root"))
                .description("Root of nested conditional: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "NESTED_CONDITIONAL")
                .build();

        builder.addPlace(rootPlace);

        // Parse conditions to create nested structure
        Map<String, Object> conditions = step.getConditions();

        // Create conditional evaluation transitions
        createNestedConditionalStructure(stepId, conditions, context, builder, rootPlaceId);

        // Handle dependencies
        connectDependencies(step, context, builder, rootPlaceId);
        context.getStepToPlaceMap().put(stepId + "_conditional_root", rootPlaceId);

        // Set initial marking if no dependencies
        if (step.getDependencies().isEmpty()) {
            builder.addInitialToken(rootPlaceId, 1);
        }

        logger.debug("Mapped NESTED_CONDITIONAL step {} with {} conditions", stepId, conditions.size());
    }

    private void createNestedConditionalStructure(String stepId, Map<String, Object> conditions,
                                                 AutomationGrammar.TransformationContext context,
                                                 PetriNet.Builder builder, String inputPlaceId) {
        // Implementation for complex AND/OR logic structures
        int conditionIndex = 0;

        for (Map.Entry<String, Object> condition : conditions.entrySet()) {
            String conditionId = condition.getKey();
            Object conditionValue = condition.getValue();

            String transitionId = context.generateTransitionId(stepId + "_condition_" + conditionIndex);
            String resultPlaceId = context.generatePlaceId(stepId + "_result_" + conditionIndex);

            // Create condition evaluation transition
            Transition conditionTransition = Transition.builder(transitionId)
                    .name("Evaluate: " + conditionId)
                    .description("Condition evaluation: " + conditionValue.toString())
                    .guard(conditionId)
                    .addMetadata("stepId", stepId)
                    .addMetadata("stepType", "NESTED_CONDITIONAL")
                    .addMetadata("conditionKey", conditionId)
                    .addMetadata("conditionValue", conditionValue.toString())
                    .build();

            // Create result place
            Place resultPlace = Place.builder(resultPlaceId)
                    .name("Result: " + conditionId)
                    .description("Result of condition: " + conditionId)
                    .addMetadata("stepId", stepId)
                    .addMetadata("stepType", "NESTED_CONDITIONAL")
                    .addMetadata("conditionKey", conditionId)
                    .build();

            // Create arcs
            Arc inputArc = new Arc(inputPlaceId, transitionId);
            Arc outputArc = new Arc(transitionId, resultPlaceId);

            builder.addTransition(conditionTransition)
                   .addPlace(resultPlace)
                   .addArc(inputArc)
                   .addArc(outputArc);

            // Store mappings
            context.getStepToTransitionMap().put(stepId + "_condition_" + conditionIndex, transitionId);
            context.getStepToPlaceMap().put(stepId + "_result_" + conditionIndex, resultPlaceId);

            conditionIndex++;
        }
    }

    /**
     * Map LOOP step: Creates loop structures with termination conditions
     * Pattern: For-each, while, retry loops with proper termination handling
     */
    private void mapLoopStep(PetriIntentSpec.IntentStep step,
                            AutomationGrammar.TransformationContext context,
                            PetriNet.Builder builder) {

        String stepId = step.getId();
        String loopEntryId = context.generatePlaceId(stepId + "_loop_entry");
        String loopBodyId = context.generateTransitionId(stepId + "_loop_body");
        String loopCheckId = context.generateTransitionId(stepId + "_loop_check");
        String loopExitId = context.generatePlaceId(stepId + "_loop_exit");

        // Create loop entry place
        Place loopEntry = Place.builder(loopEntryId)
                .name(getPlaceName(step, "loop_entry"))
                .description("Loop entry point: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "LOOP")
                .build();

        // Create loop body transition
        Transition loopBody = Transition.builder(loopBodyId)
                .name("Loop Body: " + step.getDescription())
                .description("Execute loop iteration")
                .action(step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "LOOP")
                .addMetadata("loopCondition", step.getLoopCondition())
                .build();

        // Create loop condition check transition
        Transition loopCheck = Transition.builder(loopCheckId)
                .name("Loop Check: " + step.getLoopCondition())
                .description("Check loop termination condition")
                .guard(step.getLoopCondition())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "LOOP")
                .addMetadata("isLoopCheck", true)
                .build();

        // Create loop exit place
        Place loopExit = Place.builder(loopExitId)
                .name(getPlaceName(step, "loop_exit"))
                .description("Loop termination: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "LOOP")
                .build();

        // Create loop arcs - entry to body, body back to entry (with check), check to exit
        Arc entryToBody = new Arc(loopEntryId, loopBodyId);
        Arc bodyToEntry = new Arc(loopBodyId, loopEntryId);  // Continue loop
        Arc entryToCheck = new Arc(loopEntryId, loopCheckId);
        Arc checkToExit = new Arc(loopCheckId, loopExitId);  // Exit loop

        builder.addPlace(loopEntry)
               .addPlace(loopExit)
               .addTransition(loopBody)
               .addTransition(loopCheck)
               .addArc(entryToBody)
               .addArc(bodyToEntry)
               .addArc(entryToCheck)
               .addArc(checkToExit);

        // Handle dependencies
        connectDependencies(step, context, builder, loopEntryId);

        // Store mappings
        context.getStepToPlaceMap().put(stepId + "_loop_entry", loopEntryId);
        context.getStepToPlaceMap().put(stepId + "_loop_exit", loopExitId);
        context.getStepToTransitionMap().put(stepId + "_loop_body", loopBodyId);
        context.getStepToTransitionMap().put(stepId + "_loop_check", loopCheckId);

        // Set initial marking if no dependencies
        if (step.getDependencies().isEmpty()) {
            builder.addInitialToken(loopEntryId, 1);
        }

        logger.debug("Mapped LOOP step {} with condition: {}", stepId, step.getLoopCondition());
    }

    /**
     * Map EVENT_TRIGGER step: Creates event-driven workflow initiation
     * Pattern: Webhook, time-based, or external event triggers
     */
    private void mapEventTriggerStep(PetriIntentSpec.IntentStep step,
                                    AutomationGrammar.TransformationContext context,
                                    PetriNet.Builder builder) {

        String stepId = step.getId();
        String triggerPlaceId = context.generatePlaceId(stepId + "_trigger_wait");
        String triggerTransitionId = context.generateTransitionId(stepId + "_trigger_fire");
        String triggeredPlaceId = context.generatePlaceId(stepId + "_triggered");

        // Get trigger configuration from metadata
        Map<String, Object> triggerConfig = (Map<String, Object>) step.getMetadata().get("triggerConfig");

        // Create trigger waiting place
        Place triggerWait = Place.builder(triggerPlaceId)
                .name(getPlaceName(step, "waiting_for_trigger"))
                .description("Waiting for trigger: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "EVENT_TRIGGER")
                .addMetadata("triggerConfig", triggerConfig)
                .build();

        // Create trigger transition
        Transition triggerFire = Transition.builder(triggerTransitionId)
                .name("Trigger: " + step.getDescription())
                .description("Event trigger fired")
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "EVENT_TRIGGER")
                .addMetadata("triggerConfig", triggerConfig)
                .addMetadata("isEventTrigger", true)
                .build();

        // Create triggered place
        Place triggered = Place.builder(triggeredPlaceId)
                .name(getPlaceName(step, "triggered"))
                .description("Event triggered: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "EVENT_TRIGGER")
                .build();

        // Create arcs
        Arc waitToFire = new Arc(triggerPlaceId, triggerTransitionId);
        Arc fireToTriggered = new Arc(triggerTransitionId, triggeredPlaceId);

        builder.addPlace(triggerWait)
               .addPlace(triggered)
               .addTransition(triggerFire)
               .addArc(waitToFire)
               .addArc(fireToTriggered);

        // Handle dependencies
        connectDependencies(step, context, builder, triggerPlaceId);

        // Store mappings
        context.getStepToPlaceMap().put(stepId + "_trigger_wait", triggerPlaceId);
        context.getStepToPlaceMap().put(stepId + "_triggered", triggeredPlaceId);
        context.getStepToTransitionMap().put(stepId + "_trigger_fire", triggerTransitionId);

        // Set initial marking if no dependencies
        if (step.getDependencies().isEmpty()) {
            builder.addInitialToken(triggerPlaceId, 1);
        }

        logger.debug("Mapped EVENT_TRIGGER step {} with config: {}", stepId, triggerConfig);
    }

    /**
     * Map ERROR_HANDLER step: Creates try-catch-finally patterns
     * Pattern: Error handling with compensation and recovery actions
     */
    private void mapErrorHandlerStep(PetriIntentSpec.IntentStep step,
                                    AutomationGrammar.TransformationContext context,
                                    PetriNet.Builder builder) {

        String stepId = step.getId();
        String tryPlaceId = context.generatePlaceId(stepId + "_try");
        String tryTransitionId = context.generateTransitionId(stepId + "_execute");
        String successPlaceId = context.generatePlaceId(stepId + "_success");
        String errorPlaceId = context.generatePlaceId(stepId + "_error");
        String catchTransitionId = context.generateTransitionId(stepId + "_catch");
        String finallyTransitionId = context.generateTransitionId(stepId + "_finally");
        String completePlaceId = context.generatePlaceId(stepId + "_complete");

        // Create try place
        Place tryPlace = Place.builder(tryPlaceId)
                .name(getPlaceName(step, "try"))
                .description("Try block: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ERROR_HANDLER")
                .build();

        // Create execution transition
        Transition tryTransition = Transition.builder(tryTransitionId)
                .name("Execute: " + step.getDescription())
                .description("Execute with error handling")
                .action(step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ERROR_HANDLER")
                .addMetadata("errorHandling", step.getErrorHandling())
                .build();

        // Create success and error places
        Place successPlace = Place.builder(successPlaceId)
                .name(getPlaceName(step, "success"))
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ERROR_HANDLER")
                .build();

        Place errorPlace = Place.builder(errorPlaceId)
                .name(getPlaceName(step, "error"))
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ERROR_HANDLER")
                .build();

        // Create catch transition
        Transition catchTransition = Transition.builder(catchTransitionId)
                .name("Catch: " + step.getDescription())
                .description("Error handling and compensation")
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ERROR_HANDLER")
                .addMetadata("compensation", step.getCompensation())
                .build();

        // Create finally transition
        Transition finallyTransition = Transition.builder(finallyTransitionId)
                .name("Finally: " + step.getDescription())
                .description("Cleanup and finalization")
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ERROR_HANDLER")
                .addMetadata("isFinally", true)
                .build();

        // Create completion place
        Place completePlace = Place.builder(completePlaceId)
                .name(getPlaceName(step, "complete"))
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "ERROR_HANDLER")
                .build();

        // Create arcs for try-catch-finally pattern
        Arc tryToExecute = new Arc(tryPlaceId, tryTransitionId);
        Arc executeToSuccess = new Arc(tryTransitionId, successPlaceId);  // Success path
        Arc executeToError = new Arc(tryTransitionId, errorPlaceId);      // Error path
        Arc errorToCatch = new Arc(errorPlaceId, catchTransitionId);
        Arc successToFinally = new Arc(successPlaceId, finallyTransitionId);
        Arc catchToFinally = new Arc(catchTransitionId, finallyTransitionId);
        Arc finallyToComplete = new Arc(finallyTransitionId, completePlaceId);

        builder.addPlace(tryPlace)
               .addPlace(successPlace)
               .addPlace(errorPlace)
               .addPlace(completePlace)
               .addTransition(tryTransition)
               .addTransition(catchTransition)
               .addTransition(finallyTransition)
               .addArc(tryToExecute)
               .addArc(executeToSuccess)
               .addArc(executeToError)
               .addArc(errorToCatch)
               .addArc(successToFinally)
               .addArc(catchToFinally)
               .addArc(finallyToComplete);

        // Handle dependencies
        connectDependencies(step, context, builder, tryPlaceId);

        // Store mappings
        context.getStepToPlaceMap().put(stepId + "_try", tryPlaceId);
        context.getStepToPlaceMap().put(stepId + "_complete", completePlaceId);
        context.getStepToTransitionMap().put(stepId + "_execute", tryTransitionId);
        context.getStepToTransitionMap().put(stepId + "_catch", catchTransitionId);

        // Set initial marking if no dependencies
        if (step.getDependencies().isEmpty()) {
            builder.addInitialToken(tryPlaceId, 1);
        }

        logger.debug("Mapped ERROR_HANDLER step {} with {} compensation actions",
                     stepId, step.getCompensation().size());
    }

    /**
     * Map COMPENSATION step: Creates compensation patterns for failed operations
     */
    private void mapCompensationStep(PetriIntentSpec.IntentStep step,
                                    AutomationGrammar.TransformationContext context,
                                    PetriNet.Builder builder) {

        String stepId = step.getId();
        String compensationEntryId = context.generatePlaceId(stepId + "_compensation_entry");

        // Create individual compensation transitions for each action
        for (int i = 0; i < step.getCompensation().size(); i++) {
            String compensationAction = step.getCompensation().get(i);
            String compensationTransitionId = context.generateTransitionId(stepId + "_compensation_" + i);
            String compensationResultId = context.generatePlaceId(stepId + "_compensation_result_" + i);

            // Create compensation transition
            Transition compensationTransition = Transition.builder(compensationTransitionId)
                    .name("Compensate: " + compensationAction)
                    .description("Compensation action: " + compensationAction)
                    .action(compensationAction)
                    .addMetadata("stepId", stepId)
                    .addMetadata("stepType", "COMPENSATION")
                    .addMetadata("compensationIndex", i)
                    .addMetadata("compensationAction", compensationAction)
                    .build();

            // Create result place
            Place compensationResult = Place.builder(compensationResultId)
                    .name("Compensated: " + compensationAction)
                    .addMetadata("stepId", stepId)
                    .addMetadata("stepType", "COMPENSATION")
                    .build();

            builder.addTransition(compensationTransition)
                   .addPlace(compensationResult);

            // Store mappings
            context.getStepToTransitionMap().put(stepId + "_compensation_" + i, compensationTransitionId);
            context.getStepToPlaceMap().put(stepId + "_compensation_result_" + i, compensationResultId);
        }

        logger.debug("Mapped COMPENSATION step {} with {} actions", stepId, step.getCompensation().size());
    }

    /**
     * Map CIRCUIT_BREAKER step: Creates circuit breaker resilience patterns
     */
    private void mapCircuitBreakerStep(PetriIntentSpec.IntentStep step,
                                      AutomationGrammar.TransformationContext context,
                                      PetriNet.Builder builder) {

        String stepId = step.getId();
        String closedStateId = context.generatePlaceId(stepId + "_circuit_closed");
        String halfOpenStateId = context.generatePlaceId(stepId + "_circuit_half_open");
        String openStateId = context.generatePlaceId(stepId + "_circuit_open");
        String executeTransitionId = context.generateTransitionId(stepId + "_execute");
        String failTransitionId = context.generateTransitionId(stepId + "_fail");
        String resetTransitionId = context.generateTransitionId(stepId + "_reset");

        // Create circuit breaker state places
        Place closedState = Place.builder(closedStateId)
                .name("Circuit Closed")
                .description("Circuit breaker in closed state (normal operation)")
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "CIRCUIT_BREAKER")
                .addMetadata("circuitState", "CLOSED")
                .build();

        Place halfOpenState = Place.builder(halfOpenStateId)
                .name("Circuit Half-Open")
                .description("Circuit breaker testing recovery")
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "CIRCUIT_BREAKER")
                .addMetadata("circuitState", "HALF_OPEN")
                .build();

        Place openState = Place.builder(openStateId)
                .name("Circuit Open")
                .description("Circuit breaker in open state (failure mode)")
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "CIRCUIT_BREAKER")
                .addMetadata("circuitState", "OPEN")
                .build();

        // Create transitions
        Transition executeTransition = Transition.builder(executeTransitionId)
                .name("Execute: " + step.getDescription())
                .description("Execute operation through circuit breaker")
                .action(step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "CIRCUIT_BREAKER")
                .addMetadata("retryPolicy", step.getRetryPolicy())
                .build();

        builder.addPlace(closedState)
               .addPlace(halfOpenState)
               .addPlace(openState)
               .addTransition(executeTransition);

        // Store mappings
        context.getStepToPlaceMap().put(stepId + "_circuit_closed", closedStateId);
        context.getStepToPlaceMap().put(stepId + "_circuit_open", openStateId);
        context.getStepToTransitionMap().put(stepId + "_execute", executeTransitionId);

        // Set initial marking (start in closed state)
        if (step.getDependencies().isEmpty()) {
            builder.addInitialToken(closedStateId, 1);
        }

        logger.debug("Mapped CIRCUIT_BREAKER step {} with retry policy", stepId);
    }

    /**
     * Map FAN_OUT_FAN_IN step: Creates complex parallel patterns with different sync strategies
     */
    private void mapFanOutFanInStep(PetriIntentSpec.IntentStep step,
                                   AutomationGrammar.TransformationContext context,
                                   PetriNet.Builder builder) {

        String stepId = step.getId();
        // Similar to parallel but with more sophisticated synchronization
        mapParallelStep(step, context, builder);

        // Add fan-out/fan-in specific metadata
        logger.debug("Mapped FAN_OUT_FAN_IN step {} (enhanced parallel pattern)", stepId);
    }

    /**
     * Map PIPELINE_STAGE step: Creates pipeline parallelism with stage dependencies
     */
    private void mapPipelineStageStep(PetriIntentSpec.IntentStep step,
                                     AutomationGrammar.TransformationContext context,
                                     PetriNet.Builder builder) {

        String stepId = step.getId();
        String stageInputId = context.generatePlaceId(stepId + "_stage_input");
        String stageProcessId = context.generateTransitionId(stepId + "_stage_process");
        String stageOutputId = context.generatePlaceId(stepId + "_stage_output");

        // Create pipeline stage structure
        Place stageInput = Place.builder(stageInputId)
                .name("Pipeline Input: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "PIPELINE_STAGE")
                .build();

        Transition stageProcess = Transition.builder(stageProcessId)
                .name("Pipeline Process: " + step.getDescription())
                .action(step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "PIPELINE_STAGE")
                .build();

        Place stageOutput = Place.builder(stageOutputId)
                .name("Pipeline Output: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "PIPELINE_STAGE")
                .build();

        builder.addPlace(stageInput)
               .addPlace(stageOutput)
               .addTransition(stageProcess)
               .addArc(new Arc(stageInputId, stageProcessId))
               .addArc(new Arc(stageProcessId, stageOutputId));

        // Handle dependencies
        connectDependencies(step, context, builder, stageInputId);

        // Store mappings
        context.getStepToPlaceMap().put(stepId + "_stage_input", stageInputId);
        context.getStepToPlaceMap().put(stepId + "_stage_output", stageOutputId);
        context.getStepToTransitionMap().put(stepId + "_stage_process", stageProcessId);

        logger.debug("Mapped PIPELINE_STAGE step {}", stepId);
    }

    /**
     * Map RESOURCE_CONSTRAINED step: Creates resource-limited parallel execution
     */
    private void mapResourceConstrainedStep(PetriIntentSpec.IntentStep step,
                                           AutomationGrammar.TransformationContext context,
                                           PetriNet.Builder builder) {

        String stepId = step.getId();
        Map<String, Object> resourceConstraints = step.getResourceConstraints();

        // Create semaphore-like structure for resource constraints
        String resourcePoolId = context.generatePlaceId(stepId + "_resource_pool");
        String acquireResourceId = context.generateTransitionId(stepId + "_acquire_resource");
        String executeWithResourceId = context.generateTransitionId(stepId + "_execute");
        String releaseResourceId = context.generateTransitionId(stepId + "_release_resource");
        String completePlaceId = context.generatePlaceId(stepId + "_complete");

        // Create resource pool (semaphore)
        Place resourcePool = Place.builder(resourcePoolId)
                .name("Resource Pool: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "RESOURCE_CONSTRAINED")
                .addMetadata("resourceConstraints", resourceConstraints)
                .build();

        // Create transitions
        Transition acquireResource = Transition.builder(acquireResourceId)
                .name("Acquire Resource")
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "RESOURCE_CONSTRAINED")
                .build();

        Transition executeWithResource = Transition.builder(executeWithResourceId)
                .name("Execute: " + step.getDescription())
                .action(step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "RESOURCE_CONSTRAINED")
                .build();

        Place completePlace = Place.builder(completePlaceId)
                .name("Complete: " + step.getDescription())
                .addMetadata("stepId", stepId)
                .addMetadata("stepType", "RESOURCE_CONSTRAINED")
                .build();

        builder.addPlace(resourcePool)
               .addPlace(completePlace)
               .addTransition(acquireResource)
               .addTransition(executeWithResource);

        // Set resource limit as initial tokens in pool
        Object maxConcurrency = resourceConstraints.get("maxConcurrency");
        if (maxConcurrency instanceof Number) {
            int limit = ((Number) maxConcurrency).intValue();
            builder.addInitialToken(resourcePoolId, limit);
        } else {
            builder.addInitialToken(resourcePoolId, 1); // Default to 1
        }

        // Store mappings
        context.getStepToPlaceMap().put(stepId + "_resource_pool", resourcePoolId);
        context.getStepToPlaceMap().put(stepId + "_complete", completePlaceId);
        context.getStepToTransitionMap().put(stepId + "_execute", executeWithResourceId);

        logger.debug("Mapped RESOURCE_CONSTRAINED step {} with constraints: {}", stepId, resourceConstraints);
    }
}