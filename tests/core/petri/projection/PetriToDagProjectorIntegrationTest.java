package core.petri.projection;

import core.DAG;
import core.TaskNode;
import core.petri.*;
import core.petri.validation.SimplePetriNetValidator;
import core.petri.simulation.PetriTokenSimulator;
import core.petri.simulation.SimulationConfig;
import core.petri.simulation.SimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for PetriToDagProjector with validation and simulation components
 * Tests the complete P3Net → validation → projection → DAG pipeline
 */
@DisplayName("PetriToDagProjector Integration Tests")
class PetriToDagProjectorIntegrationTest {

    private PetriToDagProjector projector;
    private SimplePetriNetValidator validator;
    private PetriTokenSimulator simulator;

    @BeforeEach
    void setUp() {
        projector = new PetriToDagProjector();
        validator = new SimplePetriNetValidator();
        simulator = new PetriTokenSimulator();
    }

    @Test
    @DisplayName("Should integrate with validation pipeline")
    void shouldIntegrateWithValidationPipeline() {
        // Create a valid DevOps workflow
        PetriNet petriNet = createDevOpsPipeline();

        // Step 1: Validate the Petri net
        PetriNetValidationResult validationResult = validator.validate(petriNet, PetriNetValidationResult.ValidationConfig.defaultConfig());
        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.getIssues()).isEmpty();

        // Step 2: Project to DAG
        DAG dag = projector.projectToDAG(petriNet);

        // Step 3: Verify projection results
        assertThat(dag).isNotNull();
        assertThat(dag.getDerivedFromPetriNetId()).isEqualTo(petriNet.getId());
        assertThat(dag.getNodes()).hasSize(4); // build, test, deploy, notify

        // Verify the projected workflow maintains logical order
        TaskNode buildNode = dag.getNode("build");
        TaskNode testNode = dag.getNode("test");
        TaskNode deployNode = dag.getNode("deploy");
        TaskNode notifyNode = dag.getNode("notify");

        assertThat(buildNode.getDependencyIds()).isEmpty(); // Root
        assertThat(testNode.getDependencyIds()).containsExactly("build");
        assertThat(deployNode.getDependencyIds()).containsExactly("test");
        assertThat(notifyNode.getDependencyIds()).containsExactly("deploy");
    }

    @Test
    @DisplayName("Should handle validation failures gracefully")
    void shouldHandleValidationFailuresGracefully() {
        // Create an invalid Petri net (duplicate transition IDs)
        PetriNet invalidNet = PetriNet.builder()
                .name("Invalid Workflow")
                .addTransition(new Transition("duplicate", "First", null, "action1", null, new HashMap<>()))
                .addTransition(new Transition("duplicate", "Second", null, "action2", null, new HashMap<>()))
                .build();

        // Validation should fail
        PetriNetValidationResult validationResult = validator.validate(invalidNet, PetriNetValidationResult.ValidationConfig.defaultConfig());
        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getIssues()).isNotEmpty();

        // Projection should reject invalid nets
        assertThatThrownBy(() -> projector.projectToDAG(invalidNet))
                .isInstanceOf(PetriToDagProjector.ProjectionException.class)
                .hasMessageContaining("Cannot project invalid Petri net");
    }

    @Test
    @DisplayName("Should integrate with simulation and maintain execution semantics")
    void shouldIntegrateWithSimulationAndMaintainExecutionSemantics() {
        // Create a workflow with parallel branches
        PetriNet parallelWorkflow = createParallelWorkflow();

        // Validate the workflow
        PetriNetValidationResult validationResult = validator.validate(parallelWorkflow, PetriNetValidationResult.ValidationConfig.defaultConfig());
        assertThat(validationResult.isValid()).isTrue();

        // Simulate the Petri net to understand execution behavior
        SimulationConfig config = SimulationConfig.builder().maxSteps(100).enableTracing(true).enableAnimation(true).build();
        SimulationResult simulationResult = simulator.simulate(parallelWorkflow, config);

        assertThat(simulationResult.isSuccess()).isTrue();
        assertThat(simulationResult.getTrace()).isNotEmpty();

        // Project to DAG
        DAG dag = projector.projectToDAG(parallelWorkflow);

        // Verify that the DAG structure reflects the parallel execution semantics
        assertThat(dag.getNodes()).hasSize(5); // start, task1, task2, join, end

        TaskNode startNode = dag.getNode("start");
        TaskNode task1Node = dag.getNode("task1");
        TaskNode task2Node = dag.getNode("task2");
        TaskNode joinNode = dag.getNode("join");
        TaskNode endNode = dag.getNode("end");

        // Parallel tasks should not depend on each other
        assertThat(task1Node.getDependencyIds()).containsExactly("start");
        assertThat(task2Node.getDependencyIds()).containsExactly("start");

        // Join should be projected correctly (single producer per input)
        assertThat(joinNode.getDependencyIds()).contains("task1", "task2");
        assertThat(endNode.getDependencyIds()).containsExactly("join");

        // Verify metadata preservation for execution hints
        assertThat(startNode.getMetadata()).containsEntry("executionType", "fork");
        assertThat(joinNode.getMetadata()).containsEntry("executionType", "join");
    }

    @Test
    @DisplayName("Should preserve traceability between PetriNet and DAG")
    void shouldPreserveTraceabilityBetweenPetriNetAndDag() {
        PetriNet originalNet = createFootballWorkflow();

        // Project to DAG
        DAG dag = projector.projectToDAG(originalNet);

        // Verify complete traceability chain
        assertThat(dag.getDerivedFromPetriNetId()).isEqualTo(originalNet.getId());
        assertThat(dag.getMetadata()).containsEntry("projectedFrom", "PetriNet");
        assertThat(dag.getMetadata()).containsEntry("projectionAlgorithm", "single-producer-consumer");

        // Each TaskNode should maintain connection to original transition
        for (TaskNode node : dag.getNodes()) {
            assertThat(node.getMetadata()).containsKey("petriTransitionId");
            assertThat(node.getMetadata()).containsKey("petriTransitionName");

            String transitionId = (String) node.getMetadata().get("petriTransitionId");
            Optional<Transition> originalTransition = originalNet.getTransition(transitionId);
            assertThat(originalTransition).isPresent();
            assertThat(originalTransition.get().getId()).isEqualTo(node.getId());
        }

        // Cross-highlighting metadata should be present for connected nodes
        long nodesWithIncomingEdges = dag.getNodes().stream()
                .mapToLong(node -> node.getDependencyIds() != null ? node.getDependencyIds().size() : 0)
                .sum();

        long nodesWithEdgeMetadata = dag.getNodes().stream()
                .mapToLong(node -> node.getMetadata().containsKey("incomingEdges") ? 1 : 0)
                .sum();

        assertThat(nodesWithEdgeMetadata).isEqualTo(nodesWithIncomingEdges);
    }

    @Test
    @DisplayName("Should handle complex workflow with choice and join patterns")
    void shouldHandleComplexWorkflowWithChoiceAndJoinPatterns() {
        PetriNet complexNet = createComplexWorkflowWithChoicesAndJoins();

        // Validate first
        PetriNetValidationResult validationResult = validator.validate(complexNet, PetriNetValidationResult.ValidationConfig.defaultConfig());
        assertThat(validationResult.isValid()).isTrue();

        // Project to DAG
        DAG dag = projector.projectToDAG(complexNet);

        assertThat(dag.getNodes()).hasSize(6); // review, approve, reject, merge, rollback, notify

        // Verify choice transitions maintain their metadata
        TaskNode approveNode = dag.getNode("approve");
        TaskNode rejectNode = dag.getNode("reject");

        assertThat(approveNode.getMetadata()).containsEntry("executionType", "choice");
        assertThat(rejectNode.getMetadata()).containsEntry("executionType", "choice");

        // Verify cross-highlighting places are tracked
        TaskNode mergeNode = dag.getNode("merge");
        if (mergeNode.getMetadata().containsKey("incomingEdges")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> incomingEdges =
                (List<Map<String, Object>>) mergeNode.getMetadata().get("incomingEdges");

            assertThat(incomingEdges).isNotEmpty();
            for (Map<String, Object> edge : incomingEdges) {
                assertThat(edge).containsKey("places");
                assertThat(edge).containsKey("from");
            }
        }
    }

    @Test
    @DisplayName("Should maintain execution order invariants after projection")
    void shouldMaintainExecutionOrderInvariantsAfterProjection() {
        PetriNet sequentialNet = createSequentialWorkflow();

        // Simulate to establish baseline execution order
        SimulationConfig config = SimulationConfig.builder().maxSteps(50).enableTracing(true).enableAnimation(false).build();
        SimulationResult simulationResult = simulator.simulate(sequentialNet, config);
        assertThat(simulationResult.isSuccess()).isTrue();

        // Project to DAG
        DAG dag = projector.projectToDAG(sequentialNet);

        // Verify that DAG dependencies maintain the same logical order as Petri net execution
        List<String> expectedOrder = Arrays.asList("init", "process", "validate", "finalize");

        for (int i = 1; i < expectedOrder.size(); i++) {
            String currentTask = expectedOrder.get(i);
            String previousTask = expectedOrder.get(i - 1);

            TaskNode currentNode = dag.getNode(currentTask);
            assertThat(currentNode.getDependencyIds()).contains(previousTask);
        }

        // Root node should have no dependencies
        TaskNode initNode = dag.getNode("init");
        assertThat(initNode.getDependencyIds()).isEmpty();
    }

    // Helper methods to create test scenarios

    private PetriNet createDevOpsPipeline() {
        return PetriNet.builder()
                .name("DevOps CI/CD Pipeline")
                .addPlace(new Place("code_ready", "Code Ready", null, null, new HashMap<>()))
                .addPlace(new Place("built", "Built", null, null, new HashMap<>()))
                .addPlace(new Place("tested", "Tested", null, null, new HashMap<>()))
                .addPlace(new Place("deployed", "Deployed", null, null, new HashMap<>()))
                .addTransition(new Transition("build", "Build App", null, "docker_build", null, new HashMap<>()))
                .addTransition(new Transition("test", "Run Tests", null, "pytest", null, new HashMap<>()))
                .addTransition(new Transition("deploy", "Deploy to Prod", null, "k8s_deploy", null, new HashMap<>()))
                .addTransition(new Transition("notify", "Send Notification", null, "slack_notify", null, new HashMap<>()))
                .addArc("build", "code_ready", 1)
                .addArc("code_ready", "test", 1)
                .addArc("test", "built", 1)
                .addArc("built", "deploy", 1)
                .addArc("deploy", "tested", 1)
                .addArc("tested", "notify", 1)
                .addInitialToken("code_ready", 1)
                .build();
    }

    private PetriNet createParallelWorkflow() {
        return PetriNet.builder()
                .name("Parallel Processing Workflow")
                .addPlace(new Place("ready", "Ready", null, null, new HashMap<>()))
                .addPlace(new Place("task1_done", "Task1 Done", null, null, new HashMap<>()))
                .addPlace(new Place("task2_done", "Task2 Done", null, null, new HashMap<>()))
                .addPlace(new Place("joined", "Results Joined", null, null, new HashMap<>()))
                .addTransition(Transition.builder("start").name("Start Parallel").action("init_parallel").asFork().build())
                .addTransition(new Transition("task1", "Execute Task 1", null, "task1_exec", null, new HashMap<>()))
                .addTransition(new Transition("task2", "Execute Task 2", null, "task2_exec", null, new HashMap<>()))
                .addTransition(Transition.builder("join").name("Join Results").action("join_results").asJoin().build())
                .addTransition(new Transition("end", "Complete", null, "finalize", null, new HashMap<>()))
                .addArc("start", "ready", 1)
                .addArc("ready", "task1", 1)
                .addArc("ready", "task2", 1)
                .addArc("task1", "task1_done", 1)
                .addArc("task2", "task2_done", 1)
                .addArc("task1_done", "join", 1)
                .addArc("task2_done", "join", 1)
                .addArc("join", "joined", 1)
                .addArc("joined", "end", 1)
                .addInitialToken("ready", 1)
                .build();
    }

    private PetriNet createFootballWorkflow() {
        return PetriNet.builder()
                .name("Match Preparation")
                .addPlace(new Place("scouted", "Opponent Scouted", null, null, new HashMap<>()))
                .addPlace(new Place("planned", "Tactics Planned", null, null, new HashMap<>()))
                .addPlace(new Place("trained", "Team Trained", null, null, new HashMap<>()))
                .addTransition(new Transition("scout", "Scout Opponent", null, "analysis", null, new HashMap<>()))
                .addTransition(new Transition("plan", "Plan Tactics", null, "tactical_planning", null, new HashMap<>()))
                .addTransition(new Transition("train", "Train Team", null, "training", null, new HashMap<>()))
                .addTransition(new Transition("match", "Play Match", null, "match_execution", null, new HashMap<>()))
                .addArc("scout", "scouted", 1)
                .addArc("scouted", "plan", 1)
                .addArc("plan", "planned", 1)
                .addArc("planned", "train", 1)
                .addArc("train", "trained", 1)
                .addArc("trained", "match", 1)
                .build();
    }

    private PetriNet createComplexWorkflowWithChoicesAndJoins() {
        return PetriNet.builder()
                .name("Code Review Workflow")
                .addPlace(new Place("reviewed", "Code Reviewed", null, null, new HashMap<>()))
                .addPlace(new Place("approved", "PR Approved", null, null, new HashMap<>()))
                .addPlace(new Place("rejected", "PR Rejected", null, null, new HashMap<>()))
                .addPlace(new Place("merged", "Code Merged", null, null, new HashMap<>()))
                .addPlace(new Place("rolled_back", "Changes Rolled Back", null, null, new HashMap<>()))
                .addTransition(new Transition("review", "Review PR", null, "code_review", null, new HashMap<>()))
                .addTransition(Transition.builder("approve").name("Approve PR").action("approve").asChoice("decision == 'approve'").build())
                .addTransition(Transition.builder("reject").name("Reject PR").action("reject").asChoice("decision == 'reject'").build())
                .addTransition(new Transition("merge", "Merge Code", null, "merge", null, new HashMap<>()))
                .addTransition(new Transition("rollback", "Rollback Changes", null, "rollback", null, new HashMap<>()))
                .addTransition(new Transition("notify", "Send Notification", null, "notify", null, new HashMap<>()))
                .addArc("review", "reviewed", 1)
                .addArc("reviewed", "approve", 1)
                .addArc("reviewed", "reject", 1)
                .addArc("approve", "approved", 1)
                .addArc("reject", "rejected", 1)
                .addArc("approved", "merge", 1)
                .addArc("rejected", "rollback", 1)
                .addArc("merge", "merged", 1)
                .addArc("rollback", "rolled_back", 1)
                .addArc("merged", "notify", 1)
                .addArc("rolled_back", "notify", 1)
                .build();
    }

    private PetriNet createSequentialWorkflow() {
        return PetriNet.builder()
                .name("Sequential Data Processing")
                .addPlace(new Place("initialized", "System Initialized", null, null, new HashMap<>()))
                .addPlace(new Place("processed", "Data Processed", null, null, new HashMap<>()))
                .addPlace(new Place("validated", "Data Validated", null, null, new HashMap<>()))
                .addTransition(new Transition("init", "Initialize System", null, "system_init", null, new HashMap<>()))
                .addTransition(new Transition("process", "Process Data", null, "data_processing", null, new HashMap<>()))
                .addTransition(new Transition("validate", "Validate Results", null, "data_validation", null, new HashMap<>()))
                .addTransition(new Transition("finalize", "Finalize Process", null, "finalization", null, new HashMap<>()))
                .addArc("init", "initialized", 1)
                .addArc("initialized", "process", 1)
                .addArc("process", "processed", 1)
                .addArc("processed", "validate", 1)
                .addArc("validate", "validated", 1)
                .addArc("validated", "finalize", 1)
                .build();
    }
}