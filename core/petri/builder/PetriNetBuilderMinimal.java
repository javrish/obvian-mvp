package core.petri.builder;

import core.petri.*;

import java.util.*;

/**
 * Minimal PetriNetBuilder implementation for Task 3 - PetriNetBuilder
 *
 * This version implements the core functionality without external dependencies
 * to demonstrate the PetriNetBuilder architecture and patterns.
 *
 * Key capabilities demonstrated:
 * - Build Petri nets directly from PetriIntentSpec
 * - Map IntentStep types to appropriate Petri net structures
 * - Support sequence, branch, parallel, and sync patterns
 * - Handle choice points and synchronization properly
 * - Fluent API for manual construction
 *
 * This follows existing DagBuilder patterns and integrates with the established architecture.
 *
 * @author Obvian Labs
 * @since Task 3 - PetriNetBuilder implementation
 */
public class PetriNetBuilderMinimal {

    // Mapping of intent actions to plugin names (following DagBuilder pattern)
    private static final Map<String, String> ACTION_TO_PLUGIN = Map.of(
        "send_email", "EmailPlugin",
        "create_file", "FilePlugin",
        "send_slack", "SlackPlugin",
        "set_reminder", "ReminderPlugin",
        "analyze_text", "LLMPlugin",
        "generate_text", "LLMPlugin",
        "consciousness", "LLMPlugin",
        "generic", "EchoPlugin"
    );

    // Fluent API state for manual construction
    private String currentName;
    private String currentDescription;
    private List<Place> currentPlaces;
    private List<Transition> currentTransitions;
    private List<Arc> currentArcs;
    private Map<String, Integer> currentInitialTokens;
    private Map<String, Object> currentMetadata;

    public PetriNetBuilderMinimal() {
        resetFluentState();
    }

    /**
     * Build a Petri net from a PetriIntentSpec using IntentToPetriMapper
     * This is the main entry point for intent-based Petri net construction
     */
    public PetriNet buildFromIntent(PetriIntentSpec intentSpec) {
        if (intentSpec == null) {
            throw new IllegalArgumentException("PetriIntentSpec cannot be null");
        }

        // Validate the intent spec first
        List<String> validationErrors = intentSpec.validate();
        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Intent validation failed: " +
                String.join(", ", validationErrors));
        }

        IntentToPetriMapper mapper = new IntentToPetriMapper();
        return mapper.mapToPetriNet(intentSpec);
    }

    // Fluent API for manual Petri net construction (following DagBuilder pattern)

    /**
     * Reset the builder for a new Petri net
     */
    public PetriNetBuilderMinimal reset() {
        resetFluentState();
        return this;
    }

    private void resetFluentState() {
        currentName = null;
        currentDescription = null;
        currentPlaces = new ArrayList<>();
        currentTransitions = new ArrayList<>();
        currentArcs = new ArrayList<>();
        currentInitialTokens = new HashMap<>();
        currentMetadata = new HashMap<>();
    }

    /**
     * Set the name for the current Petri net being built
     */
    public PetriNetBuilderMinimal withName(String name) {
        this.currentName = name;
        return this;
    }

    /**
     * Set the description for the current Petri net being built
     */
    public PetriNetBuilderMinimal withDescription(String description) {
        this.currentDescription = description;
        return this;
    }

    /**
     * Add a place to the current Petri net
     */
    public PetriNetBuilderMinimal addPlace(String id, String name) {
        currentPlaces.add(new Place(id, name));
        return this;
    }

    /**
     * Add a place with capacity to the current Petri net
     */
    public PetriNetBuilderMinimal addPlace(String id, String name, Integer capacity) {
        currentPlaces.add(new Place(id, name, capacity));
        return this;
    }

    /**
     * Add a transition to the current Petri net
     */
    public PetriNetBuilderMinimal addTransition(String id, String name, String action) {
        currentTransitions.add(Transition.builder(id)
                .name(name)
                .action(action)
                .build());
        return this;
    }

    /**
     * Add a transition with guard condition
     */
    public PetriNetBuilderMinimal addTransition(String id, String name, String action, String guard) {
        currentTransitions.add(Transition.builder(id)
                .name(name)
                .action(action)
                .guard(guard)
                .build());
        return this;
    }

    /**
     * Connect a place to a transition
     */
    public PetriNetBuilderMinimal connectPlaceToTransition(String placeId, String transitionId) {
        currentArcs.add(new Arc(placeId, transitionId, 1));
        return this;
    }

    /**
     * Connect a transition to a place
     */
    public PetriNetBuilderMinimal connectTransitionToPlace(String transitionId, String placeId) {
        currentArcs.add(new Arc(transitionId, placeId, 1));
        return this;
    }

    /**
     * Set initial tokens for a place
     */
    public PetriNetBuilderMinimal setInitialToken(String placeId, int tokens) {
        currentInitialTokens.put(placeId, tokens);
        return this;
    }

    /**
     * Add metadata to the current Petri net
     */
    public PetriNetBuilderMinimal addMetadata(String key, Object value) {
        currentMetadata.put(key, value);
        return this;
    }

    /**
     * Build the Petri net from the current fluent state
     */
    public PetriNet build() {
        String name = currentName != null ? currentName : "built_petri_net_" + System.currentTimeMillis();

        PetriNet.Builder builder = PetriNet.builder()
                .name(name)
                .description(currentDescription)
                .initialMarking(new Marking(currentInitialTokens));

        // Add places individually
        for (Place place : currentPlaces) {
            builder.addPlace(place);
        }

        // Add transitions individually
        for (Transition transition : currentTransitions) {
            builder.addTransition(transition);
        }

        // Add arcs individually
        for (Arc arc : currentArcs) {
            builder.addArc(arc);
        }

        // Add metadata
        for (Map.Entry<String, Object> entry : currentMetadata.entrySet()) {
            builder.addMetadata(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    /**
     * IntentToPetriMapper - Maps PetriIntentSpec to PetriNet
     * This implements the core mapping logic from requirements 2.1-2.5
     */
    private static class IntentToPetriMapper {

        public PetriNet mapToPetriNet(PetriIntentSpec intentSpec) {
            PetriNet.Builder netBuilder = PetriNet.builder()
                    .name(intentSpec.getName())
                    .description(intentSpec.getDescription());

            // Add metadata from intent spec
            for (Map.Entry<String, Object> entry : intentSpec.getMetadata().entrySet()) {
                netBuilder.addMetadata(entry.getKey(), entry.getValue());
            }

            List<PetriIntentSpec.IntentStep> steps = intentSpec.getSteps();

            // Process each intent step according to its type
            for (PetriIntentSpec.IntentStep step : steps) {
                processIntentStep(step, netBuilder, steps);
            }

            // Handle step dependencies and create sequence arcs
            processStepDependencies(steps, netBuilder);

            // Set initial marking (start places get tokens)
            setInitialMarking(steps, netBuilder);

            return netBuilder.build();
        }

        /**
         * Process individual intent steps based on their type
         * Implements requirements:
         * - 2.1 ACTION steps → Transition with pre/post places
         * - 2.2 SEQUENCE steps → Connected via dependencies
         * - 2.3 CHOICE steps → XOR-split with guards
         * - 2.4 PARALLEL steps → AND-split/join
         * - 2.5 SYNC steps → Join transition
         */
        private void processIntentStep(PetriIntentSpec.IntentStep step, PetriNet.Builder netBuilder,
                                     List<PetriIntentSpec.IntentStep> allSteps) {
            switch (step.getType()) {
                case ACTION -> processActionStep(step, netBuilder);
                case CHOICE -> processChoiceStep(step, netBuilder);
                case PARALLEL -> processParallelStep(step, netBuilder);
                case SYNC -> processSyncStep(step, netBuilder);
                case SEQUENCE -> {
                    // SEQUENCE steps are handled via dependencies
                    System.out.println("SEQUENCE step " + step.getId() + " processed via dependencies");
                }
                default -> {
                    System.out.println("Unknown step type: " + step.getType() + ", defaulting to ACTION");
                    processActionStep(step, netBuilder); // Fallback to action
                }
            }
        }

        /**
         * Process ACTION step: Create transition with pre/post places
         * Requirement 2.1: ACTION → Transition with pre/post places
         */
        private void processActionStep(PetriIntentSpec.IntentStep step, PetriNet.Builder netBuilder) {
            // Create transition for the action
            String transitionId = "t_" + step.getId();
            String pluginName = getPluginForAction(step.getDescription());

            Transition.Builder transitionBuilder = Transition.builder(transitionId)
                    .name(step.getDescription())
                    .action(pluginName);

            // Add guard condition if present
            if (step.hasGuard()) {
                transitionBuilder.guard(step.getWhen());
            }

            netBuilder.addTransition(transitionBuilder.build());

            // Create pre and post places
            String prePlace = "p_pre_" + step.getId();
            String postPlace = "p_post_" + step.getId();

            netBuilder.addPlace(Place.builder(prePlace)
                    .name("Pre-condition for " + step.getDescription())
                    .build());
            netBuilder.addPlace(Place.builder(postPlace)
                    .name("Post-condition for " + step.getDescription())
                    .build());

            // Connect places to transition: prePlace → transition → postPlace
            netBuilder.addArc(prePlace, transitionId);
            netBuilder.addArc(transitionId, postPlace);
        }

        /**
         * Process CHOICE step: Create XOR-split pattern with guards
         * Requirement 2.3: CHOICE → XOR branching with guards
         */
        private void processChoiceStep(PetriIntentSpec.IntentStep step, PetriNet.Builder netBuilder) {
            // XOR-split: Create multiple transitions for different choice paths
            @SuppressWarnings("unchecked")
            List<String> paths = (List<String>) step.getMetadata().get("paths");

            if (paths == null || paths.isEmpty()) {
                System.out.println("Choice step " + step.getId() + " has no paths defined, defaulting to ACTION");
                processActionStep(step, netBuilder); // Fallback
                return;
            }

            String inputPlace = "p_choice_input_" + step.getId();
            netBuilder.addPlace(Place.builder(inputPlace)
                    .name("Choice input for " + step.getDescription())
                    .build());

            for (String path : paths) {
                String transitionId = "t_" + step.getId() + "_" + path;
                String outputPlace = "p_choice_output_" + step.getId() + "_" + path;

                // Create choice transition with guard
                Transition transition = Transition.builder(transitionId)
                        .name(step.getDescription() + " (" + path + ")")
                        .asChoice(path)
                        .guard("choice == '" + path + "'")
                        .build();

                netBuilder.addTransition(transition);
                netBuilder.addPlace(Place.builder(outputPlace)
                        .name("Choice output (" + path + ") for " + step.getDescription())
                        .build());

                // Connect input → transition → output
                netBuilder.addArc(inputPlace, transitionId);
                netBuilder.addArc(transitionId, outputPlace);
            }
        }

        /**
         * Process PARALLEL step: Create AND-split/join pattern
         * Requirement 2.4: PARALLEL → AND-split/join with synchronization
         */
        private void processParallelStep(PetriIntentSpec.IntentStep step, PetriNet.Builder netBuilder) {
            // AND-split/join: Create fork and join transitions
            String forkTransitionId = "t_fork_" + step.getId();
            String joinTransitionId = "t_join_" + step.getId();

            String inputPlace = "p_parallel_input_" + step.getId();
            String outputPlace = "p_parallel_output_" + step.getId();
            String intermediatePlace1 = "p_parallel_branch1_" + step.getId();
            String intermediatePlace2 = "p_parallel_branch2_" + step.getId();

            // Create places
            netBuilder.addPlace(Place.builder(inputPlace).name("Parallel input").build());
            netBuilder.addPlace(Place.builder(outputPlace).name("Parallel output").build());
            netBuilder.addPlace(Place.builder(intermediatePlace1).name("Parallel branch 1").build());
            netBuilder.addPlace(Place.builder(intermediatePlace2).name("Parallel branch 2").build());

            // Create fork transition (AND-split)
            Transition forkTransition = Transition.builder(forkTransitionId)
                    .name("Fork for " + step.getDescription())
                    .asFork()
                    .build();
            netBuilder.addTransition(forkTransition);

            // Create join transition (AND-join)
            Transition joinTransition = Transition.builder(joinTransitionId)
                    .name("Join for " + step.getDescription())
                    .asJoin()
                    .build();
            netBuilder.addTransition(joinTransition);

            // Connect the AND-split/join structure
            netBuilder.addArc(inputPlace, forkTransitionId);
            netBuilder.addArc(forkTransitionId, intermediatePlace1);
            netBuilder.addArc(forkTransitionId, intermediatePlace2);
            netBuilder.addArc(intermediatePlace1, joinTransitionId);
            netBuilder.addArc(intermediatePlace2, joinTransitionId);
            netBuilder.addArc(joinTransitionId, outputPlace);
        }

        /**
         * Process SYNC step: Create join transition with multiple inputs
         * Requirement 2.5: SYNC → Join transition with multiple inputs
         */
        private void processSyncStep(PetriIntentSpec.IntentStep step, PetriNet.Builder netBuilder) {
            // Sync step: Create a join transition that waits for multiple inputs
            String transitionId = "t_" + step.getId();
            String outputPlace = "p_sync_output_" + step.getId();

            Transition syncTransition = Transition.builder(transitionId)
                    .name(step.getDescription())
                    .asJoin()
                    .build();

            netBuilder.addTransition(syncTransition);
            netBuilder.addPlace(Place.builder(outputPlace)
                    .name("Sync output for " + step.getDescription())
                    .build());

            netBuilder.addArc(transitionId, outputPlace);
        }

        /**
         * Process step dependencies to create sequence arcs
         * Requirement 2.2: SEQUENCE A→B → Arc from post(A) to pre(B)
         */
        private void processStepDependencies(List<PetriIntentSpec.IntentStep> steps, PetriNet.Builder netBuilder) {
            for (PetriIntentSpec.IntentStep step : steps) {
                if (!step.getDependencies().isEmpty()) {
                    // For sequence A→B: connect post(A) to pre(B)
                    for (String depId : step.getDependencies()) {
                        String sharedPlaceId = "p_shared_" + depId + "_" + step.getId();

                        // Create shared place that connects dependency output to current step input
                        netBuilder.addPlace(Place.builder(sharedPlaceId)
                                .name("Shared place between " + depId + " and " + step.getId())
                                .build());

                        // Connect dependency transition to shared place, then shared place to current step
                        if (step.getType() == PetriIntentSpec.StepType.ACTION) {
                            String depTransitionId = "t_" + depId;
                            String currentTransitionId = "t_" + step.getId();

                            // Dependency transition → shared place → current transition
                            netBuilder.addArc(depTransitionId, sharedPlaceId);
                            netBuilder.addArc(sharedPlaceId, currentTransitionId);
                        }
                    }
                }
            }
        }

        /**
         * Set initial marking for root steps (no dependencies)
         */
        private void setInitialMarking(List<PetriIntentSpec.IntentStep> steps, PetriNet.Builder netBuilder) {
            // Find steps with no dependencies (root steps) and give their pre-places tokens
            for (PetriIntentSpec.IntentStep step : steps) {
                if (step.getDependencies().isEmpty()) {
                    if (step.getType() == PetriIntentSpec.StepType.ACTION) {
                        netBuilder.addInitialToken("p_pre_" + step.getId(), 1);
                    } else if (step.getType() == PetriIntentSpec.StepType.CHOICE) {
                        netBuilder.addInitialToken("p_choice_input_" + step.getId(), 1);
                    } else if (step.getType() == PetriIntentSpec.StepType.PARALLEL) {
                        netBuilder.addInitialToken("p_parallel_input_" + step.getId(), 1);
                    }
                }
            }
        }

        /**
         * Map action descriptions to plugin names (following DagBuilder pattern)
         */
        private String getPluginForAction(String description) {
            // Try to infer plugin from description
            String lowerDesc = description.toLowerCase();
            if (lowerDesc.contains("email")) return "EmailPlugin";
            if (lowerDesc.contains("file") || lowerDesc.contains("create")) return "FilePlugin";
            if (lowerDesc.contains("slack")) return "SlackPlugin";
            if (lowerDesc.contains("remind")) return "ReminderPlugin";
            if (lowerDesc.contains("analyze") || lowerDesc.contains("generate")) return "LLMPlugin";

            return "EchoPlugin"; // Default fallback
        }
    }

    /**
     * Get available plugin mappings (following DagBuilder pattern)
     */
    public Map<String, String> getActionToPluginMappings() {
        return new HashMap<>(ACTION_TO_PLUGIN);
    }

    /**
     * Check if an action is supported
     */
    public boolean isActionSupported(String action) {
        return action != null && ACTION_TO_PLUGIN.containsKey(action);
    }
}