package core.petri.builder;

import core.petri.*;
import core.TaskNode;
import core.DAG;
import memory.MemoryStore;
import memory.FileMemoryEntry;
import memory.ExecutionMemoryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PetriNetBuilder - Converts intents and DAGs to Petri net representations
 *
 * This class implements the core functionality for Task 3 of the Petri net DAG POC,
 * following existing DagBuilder patterns and integrating with the established architecture.
 *
 * Key capabilities:
 * - Build Petri nets directly from PetriIntentSpec
 * - Convert existing DAG structures to Petri nets (bidirectional)
 * - Map TaskNode actions to Petri net transitions with proper place generation
 * - Support sequence, branch, parallel, and loop patterns
 * - Handle choice points and synchronization properly
 *
 * @author Obvian Labs
 * @since Task 3 - PetriNetBuilder implementation
 */
public class PetriNetBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PetriNetBuilder.class);

    private final MemoryStore memoryStore;

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

    public PetriNetBuilder() {
        this.memoryStore = null;
        resetFluentState();
    }

    public PetriNetBuilder(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
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

        logger.debug("Building Petri net from intent: {}", intentSpec.getName());

        IntentToPetriMapper mapper = new IntentToPetriMapper(memoryStore);
        return mapper.mapToPetriNet(intentSpec);
    }

    /**
     * Convert existing DAG structure to Petri net (bidirectional capability)
     */
    public PetriNet buildFromDAG(DAG dag) {
        if (dag == null) {
            throw new IllegalArgumentException("DAG cannot be null");
        }

        logger.debug("Converting DAG to Petri net: {}", dag.getName());

        DagToPetriMapper mapper = new DagToPetriMapper();
        return mapper.mapToPetriNet(dag);
    }

    /**
     * Convert Petri net to DAG structure (bidirectional capability)
     */
    public DAG convertToDAG(PetriNet petriNet) {
        if (petriNet == null) {
            throw new IllegalArgumentException("PetriNet cannot be null");
        }

        logger.debug("Converting Petri net to DAG: {}", petriNet.getName());

        PetriToDagMapper mapper = new PetriToDagMapper();
        return mapper.mapToDAG(petriNet);
    }

    // Fluent API for manual Petri net construction (following DagBuilder pattern)

    /**
     * Reset the builder for a new Petri net
     */
    public PetriNetBuilder reset() {
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
    public PetriNetBuilder withName(String name) {
        this.currentName = name;
        return this;
    }

    /**
     * Set the description for the current Petri net being built
     */
    public PetriNetBuilder withDescription(String description) {
        this.currentDescription = description;
        return this;
    }

    /**
     * Add a place to the current Petri net
     */
    public PetriNetBuilder addPlace(String id, String name) {
        currentPlaces.add(new Place(id, name));
        return this;
    }

    /**
     * Add a place with capacity to the current Petri net
     */
    public PetriNetBuilder addPlace(String id, String name, Integer capacity) {
        currentPlaces.add(new Place(id, name, capacity));
        return this;
    }

    /**
     * Add a transition to the current Petri net
     */
    public PetriNetBuilder addTransition(String id, String name, String action) {
        currentTransitions.add(Transition.builder(id)
                .name(name)
                .action(action)
                .build());
        return this;
    }

    /**
     * Add a transition with guard condition
     */
    public PetriNetBuilder addTransition(String id, String name, String action, String guard) {
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
    public PetriNetBuilder connectPlaceToTransition(String placeId, String transitionId) {
        currentArcs.add(new Arc(placeId, transitionId, 1));
        return this;
    }

    /**
     * Connect a transition to a place
     */
    public PetriNetBuilder connectTransitionToPlace(String transitionId, String placeId) {
        currentArcs.add(new Arc(transitionId, placeId, 1));
        return this;
    }

    /**
     * Set initial tokens for a place
     */
    public PetriNetBuilder setInitialToken(String placeId, int tokens) {
        currentInitialTokens.put(placeId, tokens);
        return this;
    }

    /**
     * Add metadata to the current Petri net
     */
    public PetriNetBuilder addMetadata(String key, Object value) {
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
     */
    private static class IntentToPetriMapper {
        private final MemoryStore memoryStore;

        public IntentToPetriMapper(MemoryStore memoryStore) {
            this.memoryStore = memoryStore;
        }

        public PetriNet mapToPetriNet(PetriIntentSpec intentSpec) {
            PetriNet.Builder netBuilder = PetriNet.builder()
                    .name(intentSpec.getName())
                    .description(intentSpec.getDescription());

            // Add metadata entries
            for (Map.Entry<String, Object> entry : intentSpec.getMetadata().entrySet()) {
                netBuilder.addMetadata(entry.getKey(), entry.getValue());
            }

            List<PetriIntentSpec.IntentStep> steps = intentSpec.getSteps();

            // Create places and transitions for each step
            for (PetriIntentSpec.IntentStep step : steps) {
                processIntentStep(step, netBuilder, steps);
            }

            // Handle step dependencies and create sequence arcs
            processStepDependencies(steps, netBuilder);

            // Set initial marking (start places get tokens)
            setInitialMarking(steps, netBuilder);

            return netBuilder.build();
        }

        private void processIntentStep(PetriIntentSpec.IntentStep step, PetriNet.Builder netBuilder,
                                     List<PetriIntentSpec.IntentStep> allSteps) {
            switch (step.getType()) {
                case ACTION -> processActionStep(step, netBuilder);
                case CHOICE -> processChoiceStep(step, netBuilder);
                case PARALLEL -> processParallelStep(step, netBuilder);
                case SYNC -> processSyncStep(step, netBuilder);
                case SEQUENCE -> {
                    // SEQUENCE steps are handled via dependencies
                    logger.debug("SEQUENCE step {} will be processed via dependencies", step.getId());
                }
                default -> {
                    logger.warn("Unknown step type: {}", step.getType());
                    processActionStep(step, netBuilder); // Fallback to action
                }
            }
        }

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

            // Connect places to transition
            netBuilder.addArc(prePlace, transitionId);
            netBuilder.addArc(transitionId, postPlace);
        }

        private void processChoiceStep(PetriIntentSpec.IntentStep step, PetriNet.Builder netBuilder) {
            // XOR-split: Create multiple transitions for different choice paths
            @SuppressWarnings("unchecked")
            List<String> paths = (List<String>) step.getMetadata().get("paths");

            if (paths == null || paths.isEmpty()) {
                logger.warn("Choice step {} has no paths defined", step.getId());
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

                // Create choice transition
                Transition transition = Transition.builder(transitionId)
                        .name(step.getDescription() + " (" + path + ")")
                        .asChoice(path)
                        .guard("choice == '" + path + "'")
                        .build();

                netBuilder.addTransition(transition);
                netBuilder.addPlace(Place.builder(outputPlace)
                        .name("Choice output (" + path + ") for " + step.getDescription())
                        .build());

                // Connect input -> transition -> output
                netBuilder.addArc(inputPlace, transitionId);
                netBuilder.addArc(transitionId, outputPlace);
            }
        }

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

        private void processStepDependencies(List<PetriIntentSpec.IntentStep> steps, PetriNet.Builder netBuilder) {
            for (PetriIntentSpec.IntentStep step : steps) {
                if (!step.getDependencies().isEmpty()) {
                    // For sequence A->B: connect post(A) to pre(B)
                    for (String depId : step.getDependencies()) {
                        String sharedPlaceId = "p_shared_" + depId + "_" + step.getId();

                        // Create shared place that connects dependency output to current step input
                        netBuilder.addPlace(Place.builder(sharedPlaceId)
                                .name("Shared place between " + depId + " and " + step.getId())
                                .build());

                        // Connect dependency transition to shared place
                        if (step.getType() == PetriIntentSpec.StepType.ACTION) {
                            String depTransitionId = "t_" + depId;
                            String currentPrePlace = "p_pre_" + step.getId();

                            // Replace the original arc from dependency to its post-place
                            // with arc to shared place, and shared place to current pre-place
                            netBuilder.addArc(depTransitionId, sharedPlaceId);
                            netBuilder.addArc(sharedPlaceId, "t_" + step.getId());
                        }
                    }
                }
            }
        }

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
     * DagToPetriMapper - Maps DAG to PetriNet
     */
    private static class DagToPetriMapper {
        public PetriNet mapToPetriNet(DAG dag) {
            PetriNet.Builder netBuilder = PetriNet.builder()
                    .name(dag.getName() != null ? dag.getName() : "dag_to_petri")
                    .derivedFromDagId(dag.getId());

            List<TaskNode> nodes = dag.getNodes();
            if (nodes == null || nodes.isEmpty()) {
                return netBuilder.build(); // Empty Petri net
            }

            // Create transition and places for each TaskNode
            for (TaskNode node : nodes) {
                processTaskNode(node, netBuilder);
            }

            // Handle dependencies between TaskNodes
            for (TaskNode node : nodes) {
                processDependencies(node, netBuilder);
            }

            // Set initial marking for root nodes
            TaskNode rootNode = dag.getRootNode();
            if (rootNode != null) {
                netBuilder.addInitialToken("p_pre_" + rootNode.getId(), 1);
            }

            return netBuilder.build();
        }

        private void processTaskNode(TaskNode node, PetriNet.Builder netBuilder) {
            String transitionId = "t_" + node.getId();
            String prePlace = "p_pre_" + node.getId();
            String postPlace = "p_post_" + node.getId();

            // Create transition from TaskNode
            Transition.Builder transitionBuilder = Transition.builder(transitionId)
                    .name(node.getId())
                    .action(node.getAction());

            // Add metadata from TaskNode
            if (node.getInputParams() != null) {
                transitionBuilder.addMetadata("inputParams", node.getInputParams());
            }
            if (node.getMaxRetries() > 0) {
                transitionBuilder.addMetadata("maxRetries", node.getMaxRetries());
            }

            netBuilder.addTransition(transitionBuilder.build());

            // Create pre and post places
            netBuilder.addPlace(Place.builder(prePlace)
                    .name("Pre-condition for " + node.getId())
                    .build());
            netBuilder.addPlace(Place.builder(postPlace)
                    .name("Post-condition for " + node.getId())
                    .build());

            // Connect places to transition
            netBuilder.addArc(prePlace, transitionId);
            netBuilder.addArc(transitionId, postPlace);
        }

        private void processDependencies(TaskNode node, PetriNet.Builder netBuilder) {
            if (node.getResolvedDependencies() != null) {
                for (TaskNode dependency : node.getResolvedDependencies()) {
                    // Connect dependency's post-place to current node's pre-place
                    String depPostPlace = "p_post_" + dependency.getId();
                    String nodePrePlace = "p_pre_" + node.getId();

                    // Create a shared place for the connection
                    String sharedPlace = "p_shared_" + dependency.getId() + "_" + node.getId();
                    netBuilder.addPlace(Place.builder(sharedPlace)
                            .name("Connection between " + dependency.getId() + " and " + node.getId())
                            .build());

                    // Connect: dependency_transition -> shared_place -> node_transition
                    netBuilder.addArc("t_" + dependency.getId(), sharedPlace);
                    netBuilder.addArc(sharedPlace, "t_" + node.getId());
                }
            }
        }
    }

    /**
     * PetriToDagMapper - Maps PetriNet to DAG
     */
    private static class PetriToDagMapper {
        public DAG mapToDAG(PetriNet petriNet) {
            DAG dag = new DAG(petriNet.getName());
            dag.setId(petriNet.getDerivedFromDagId());

            List<Transition> transitions = petriNet.getTransitions();
            if (transitions.isEmpty()) {
                return dag; // Empty DAG
            }

            Map<String, TaskNode> nodeMap = new HashMap<>();
            List<TaskNode> nodes = new ArrayList<>();

            // Create TaskNode for each transition
            for (Transition transition : transitions) {
                TaskNode taskNode = createTaskNodeFromTransition(transition);
                nodeMap.put(transition.getId(), taskNode);
                nodes.add(taskNode);
            }

            // Rebuild dependencies from Petri net structure
            for (Transition transition : transitions) {
                TaskNode taskNode = nodeMap.get(transition.getId());
                List<TaskNode> dependencies = findDependencies(transition, petriNet, nodeMap);
                taskNode.setResolvedDependencies(dependencies);
            }

            dag.setNodes(nodes);

            // Find root node (transition with tokens in input places)
            TaskNode rootNode = findRootNode(petriNet, nodeMap);
            dag.setRootNode(rootNode);

            return dag;
        }

        private TaskNode createTaskNodeFromTransition(Transition transition) {
            String nodeId = transition.getId().startsWith("t_") ?
                    transition.getId().substring(2) : transition.getId();

            TaskNode taskNode = new TaskNode(nodeId, transition.getAction());

            // Restore metadata as input parameters
            @SuppressWarnings("unchecked")
            Map<String, Object> inputParams = (Map<String, Object>) transition.getMetadata("inputParams");
            if (inputParams != null) {
                taskNode.setInputParams(inputParams);
            }

            // Restore retry configuration
            Object maxRetries = transition.getMetadata("maxRetries");
            if (maxRetries instanceof Number) {
                taskNode.setMaxRetries(((Number) maxRetries).intValue());
            }

            return taskNode;
        }

        private List<TaskNode> findDependencies(Transition transition, PetriNet petriNet,
                                               Map<String, TaskNode> nodeMap) {
            List<TaskNode> dependencies = new ArrayList<>();

            // Find transitions that have places connecting to current transition's input places
            List<Place> inputPlaces = petriNet.getInputPlaces(transition.getId());

            for (Place inputPlace : inputPlaces) {
                List<Transition> inputTransitions = petriNet.getInputTransitions(inputPlace.getId());
                for (Transition inputTransition : inputTransitions) {
                    TaskNode depNode = nodeMap.get(inputTransition.getId());
                    if (depNode != null && !dependencies.contains(depNode)) {
                        dependencies.add(depNode);
                    }
                }
            }

            return dependencies;
        }

        private TaskNode findRootNode(PetriNet petriNet, Map<String, TaskNode> nodeMap) {
            // Find transition whose input place has tokens in initial marking
            Marking initialMarking = petriNet.getInitialMarking();

            for (Transition transition : petriNet.getTransitions()) {
                List<Place> inputPlaces = petriNet.getInputPlaces(transition.getId());
                for (Place place : inputPlaces) {
                    if (initialMarking.getTokens(place.getId()) > 0) {
                        return nodeMap.get(transition.getId());
                    }
                }
            }

            // Fallback: return first transition
            if (!petriNet.getTransitions().isEmpty()) {
                return nodeMap.get(petriNet.getTransitions().get(0).getId());
            }

            return null;
        }
    }

    // Helper method to resolve memory references (following DagBuilder pattern)
    private Object resolveMemoryReference(String memoryRef, String action) {
        if (memoryRef == null || memoryStore == null) return null;
        switch (memoryRef) {
            case "last_file":
                FileMemoryEntry fileEntry = memoryStore.getLastFile();
                if (fileEntry != null) {
                    if ("send_email".equals(action)) {
                        return fileEntry.getFileName();
                    } else if ("create_file".equals(action)) {
                        return fileEntry.getContent();
                    }
                    return fileEntry.getFileName();
                }
                break;
            case "last_execution":
                ExecutionMemoryEntry execEntry = memoryStore.getLastExecution();
                if (execEntry != null) {
                    Map<String, Object> results = execEntry.getResults();
                    return results != null ? results.toString() : "execution completed";
                }
                break;
            default:
                return null;
        }
        return null;
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