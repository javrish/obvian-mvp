package core.petri.projection;

// DAG and TaskNode are in the root package
import core.DAG;
import core.TaskNode;
import core.petri.PetriNet;
import core.petri.Place;
import core.petri.Transition;
import core.petri.Arc;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Projects a validated Petri net to an executable DAG for integration with existing infrastructure.
 *
 * Implements the projection algorithm specified in requirements:
 * - Edge u→v iff ∃ place p with u→p and p→v and producers(p)=={u} and consumers(p)=={v}
 * - Transitive reduction: Remove redundant edges, tie-break by lexicographic transition ID
 * - Cross-highlighting: Transition IDs become DAG node IDs, edges carry meta.places for UI sync
 * - Traceability: DAG stamped with derivedFromPetriNetId
 *
 * This enables the complete steel thread: PetriNet → validate → project to DAG → execute
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@Component
public class PetriToDagProjector {

    /**
     * Project a validated Petri net to an executable DAG
     *
     * @param petriNet The validated Petri net to project
     * @return The projected DAG ready for execution
     * @throws IllegalArgumentException if petriNet is null or invalid
     * @throws ProjectionException if projection fails due to structural issues
     */
    public DAG projectToDAG(PetriNet petriNet) {
        if (petriNet == null) {
            throw new IllegalArgumentException("PetriNet cannot be null");
        }

        // Validate basic structure
        List<String> errors = petriNet.validate();
        if (!errors.isEmpty()) {
            throw new ProjectionException("Cannot project invalid Petri net: " + errors);
        }

        DAG dag = new DAG();
        dag.setDerivedFromPetriNetId(petriNet.getId());
        dag.setName(petriNet.getName());

        // Copy metadata
        Map<String, Object> metadata = new HashMap<>(petriNet.getMetadata());
        metadata.put("projectedFrom", "PetriNet");
        metadata.put("projectionAlgorithm", "single-producer-consumer");
        dag.setMetadata(metadata);

        // Step 1: Create TaskNodes from transitions
        Map<String, TaskNode> nodeMap = createTaskNodesFromTransitions(petriNet);

        // Step 2: Apply projection algorithm to find edges
        List<EdgeInfo> projectedEdges = applyProjectionAlgorithm(petriNet);

        // Step 3: Apply transitive reduction with lexicographic tie-breaking
        List<EdgeInfo> reducedEdges = applyTransitiveReduction(projectedEdges);

        // Step 4: Build DAG structure
        buildDagStructure(dag, nodeMap, reducedEdges);

        // Step 5: Identify root nodes (transitions with no input places or only initial places)
        identifyRootNodes(dag, petriNet);

        return dag;
    }

    /**
     * Create TaskNodes from Petri net transitions
     * Transition IDs become DAG node IDs for cross-highlighting
     */
    private Map<String, TaskNode> createTaskNodesFromTransitions(PetriNet petriNet) {
        Map<String, TaskNode> nodeMap = new HashMap<>();

        for (Transition transition : petriNet.getTransitions()) {
            TaskNode node = new TaskNode();
            node.setId(transition.getId()); // Direct mapping for cross-highlighting

            // Map action from transition
            String action = transition.getAction();
            if (action == null || action.trim().isEmpty()) {
                action = transition.getName(); // Fallback to name
            }
            if (action == null || action.trim().isEmpty()) {
                action = "execute"; // Default action
            }
            node.setAction(action);

            // Set input parameters from transition metadata
            Map<String, Object> inputParams = new HashMap<>();
            Map<String, Object> transitionMeta = transition.getMetadata();
            if (transitionMeta.containsKey("inputParams")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) transitionMeta.get("inputParams");
                inputParams.putAll(params);
            }

            // Add transition properties as parameters
            if (transition.getDescription() != null) {
                inputParams.put("description", transition.getDescription());
            }
            if (transition.getGuard() != null) {
                inputParams.put("guard", transition.getGuard());
            }

            node.setInputParams(inputParams);

            // Copy metadata for cross-highlighting and execution context
            Map<String, Object> nodeMetadata = new HashMap<>(transitionMeta);
            nodeMetadata.put("petriTransitionId", transition.getId());
            nodeMetadata.put("petriTransitionName", transition.getName());

            // Add structural properties for execution hints
            if (transition.isChoice()) {
                nodeMetadata.put("executionType", "choice");
                nodeMetadata.put("choiceCondition", transition.getChoiceCondition());
            } else if (transition.isFork()) {
                nodeMetadata.put("executionType", "fork");
            } else if (transition.isJoin()) {
                nodeMetadata.put("executionType", "join");
            }

            node.setMetadata(nodeMetadata);
            nodeMap.put(transition.getId(), node);
        }

        return nodeMap;
    }

    /**
     * Apply the core projection algorithm:
     * Edge u→v iff ∃ place p with u→p and p→v and producers(p)=={u} and consumers(p)=={v}
     */
    private List<EdgeInfo> applyProjectionAlgorithm(PetriNet petriNet) {
        List<EdgeInfo> edges = new ArrayList<>();

        for (Place place : petriNet.getPlaces()) {
            String placeId = place.getId();

            // Find all transitions that produce tokens to this place (producers)
            List<String> producers = petriNet.getInputTransitions(placeId)
                    .stream()
                    .map(Transition::getId)
                    .collect(Collectors.toList());

            // Find all transitions that consume tokens from this place (consumers)
            List<String> consumers = petriNet.getOutputTransitions(placeId)
                    .stream()
                    .map(Transition::getId)
                    .collect(Collectors.toList());

            // Apply projection rule: single producer and single consumer
            if (producers.size() == 1 && consumers.size() == 1) {
                String producer = producers.get(0);
                String consumer = consumers.get(0);

                // Create edge from producer to consumer through this place
                EdgeInfo edge = new EdgeInfo(producer, consumer, Arrays.asList(placeId));
                edges.add(edge);
            }
        }

        return edges;
    }

    /**
     * Apply transitive reduction to remove redundant edges
     * Tie-break by lexicographic transition ID ordering
     */
    private List<EdgeInfo> applyTransitiveReduction(List<EdgeInfo> edges) {
        if (edges.isEmpty()) {
            return new ArrayList<>(edges);
        }

        // Build adjacency representation for transitive reduction
        Set<String> allNodes = new HashSet<>();
        edges.forEach(edge -> {
            allNodes.add(edge.getFrom());
            allNodes.add(edge.getTo());
        });

        // Create adjacency matrix for reachability analysis
        Map<String, Set<String>> adjacency = new HashMap<>();
        Map<String, Set<String>> reachability = new HashMap<>();

        for (String node : allNodes) {
            adjacency.put(node, new HashSet<>());
            reachability.put(node, new HashSet<>());
        }

        // Build initial adjacency and track edge metadata
        Map<String, EdgeInfo> edgeMap = new HashMap<>();
        for (EdgeInfo edge : edges) {
            String key = edge.getFrom() + "->" + edge.getTo();
            adjacency.get(edge.getFrom()).add(edge.getTo());
            edgeMap.put(key, edge);
        }

        // Compute transitive closure using Floyd-Warshall algorithm
        for (String k : allNodes) {
            for (String i : allNodes) {
                for (String j : allNodes) {
                    if (adjacency.get(i).contains(k) && adjacency.get(k).contains(j)) {
                        reachability.get(i).add(j);
                    }
                }
            }
        }

        // Remove transitive edges (keep only direct edges that are not transitively reachable)
        List<EdgeInfo> reducedEdges = new ArrayList<>();

        for (EdgeInfo edge : edges) {
            String from = edge.getFrom();
            String to = edge.getTo();

            // Check if there's an alternative path from 'from' to 'to'
            boolean isTransitive = false;
            for (String intermediate : allNodes) {
                if (!intermediate.equals(from) && !intermediate.equals(to)) {
                    if (adjacency.get(from).contains(intermediate) &&
                        reachability.get(intermediate).contains(to)) {
                        isTransitive = true;
                        break;
                    }
                }
            }

            if (!isTransitive) {
                reducedEdges.add(edge);
            }
        }

        // Apply lexicographic tie-breaking for deterministic ordering
        reducedEdges.sort((e1, e2) -> {
            int cmp = e1.getFrom().compareTo(e2.getFrom());
            if (cmp != 0) return cmp;
            return e1.getTo().compareTo(e2.getTo());
        });

        return reducedEdges;
    }

    /**
     * Build the DAG structure from nodes and edges
     */
    private void buildDagStructure(DAG dag, Map<String, TaskNode> nodeMap, List<EdgeInfo> edges) {
        // Add all nodes to DAG
        for (TaskNode node : nodeMap.values()) {
            dag.addNode(node);
        }

        // Build dependencies from edges
        for (EdgeInfo edge : edges) {
            TaskNode fromNode = nodeMap.get(edge.getFrom());
            TaskNode toNode = nodeMap.get(edge.getTo());

            if (fromNode != null && toNode != null) {
                // Add dependency: toNode depends on fromNode
                if (toNode.getDependencyIds() == null) {
                    toNode.setDependencyIds(new ArrayList<>());
                }
                if (!toNode.getDependencyIds().contains(fromNode.getId())) {
                    toNode.getDependencyIds().add(fromNode.getId());
                }

                // Add cross-highlighting metadata: places this edge represents
                Map<String, Object> edgeMetadata = new HashMap<>();
                edgeMetadata.put("places", edge.getPlaces());

                // Store edge metadata in the target node for UI cross-highlighting
                Map<String, Object> nodeMetadata = toNode.getMetadata();
                if (!nodeMetadata.containsKey("incomingEdges")) {
                    nodeMetadata.put("incomingEdges", new ArrayList<>());
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> incomingEdges =
                    (List<Map<String, Object>>) nodeMetadata.get("incomingEdges");

                Map<String, Object> edgeInfo = new HashMap<>();
                edgeInfo.put("from", fromNode.getId());
                edgeInfo.put("places", edge.getPlaces());
                incomingEdges.add(edgeInfo);
            }
        }

        // Rebuild transient dependencies
        dag.rebuildDependencies();
    }

    /**
     * Identify root nodes - transitions with no dependencies or only dependencies from initial marking
     */
    private void identifyRootNodes(DAG dag, PetriNet petriNet) {
        List<TaskNode> rootCandidates = new ArrayList<>();

        for (TaskNode node : dag.getNodes()) {
            if (node.getDependencyIds() == null || node.getDependencyIds().isEmpty()) {
                rootCandidates.add(node);
            }
        }

        // If no clear root, check for transitions enabled by initial marking
        if (rootCandidates.isEmpty()) {
            List<Transition> enabledByInitial = petriNet.getEnabledTransitions(petriNet.getInitialMarking());
            for (Transition t : enabledByInitial) {
                TaskNode node = dag.getNode(t.getId());
                if (node != null) {
                    rootCandidates.add(node);
                }
            }
        }

        // Set the first root candidate as the root node (lexicographic order for determinism)
        if (!rootCandidates.isEmpty()) {
            rootCandidates.sort(Comparator.comparing(TaskNode::getId));
            dag.setRootNode(rootCandidates.get(0));
        }
    }

    /**
     * Information about a projected edge between transitions
     */
    private static class EdgeInfo {
        private final String from;
        private final String to;
        private final List<String> places;

        public EdgeInfo(String from, String to, List<String> places) {
            this.from = from;
            this.to = to;
            this.places = new ArrayList<>(places);
        }

        public String getFrom() { return from; }
        public String getTo() { return to; }
        public List<String> getPlaces() { return new ArrayList<>(places); }

        @Override
        public String toString() {
            return from + " -> " + to + " [places: " + places + "]";
        }
    }

    /**
     * Exception thrown when projection fails
     */
    public static class ProjectionException extends RuntimeException {
        public ProjectionException(String message) {
            super(message);
        }

        public ProjectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}