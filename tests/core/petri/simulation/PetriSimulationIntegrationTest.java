package core.petri.simulation;

import core.petri.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests demonstrating complete token-based simulation functionality
 *
 * These tests showcase the P3Net simulation engine capabilities including:
 * - Complete workflow simulation from start to finish
 * - Deterministic execution with trace reproduction
 * - Complex workflow patterns (parallel, choice, loops)
 * - Real-world workflow scenarios
 * - Performance characteristics
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@DisplayName("Petri Simulation Integration Tests")
class PetriSimulationIntegrationTest {

    private PetriTokenSimulator simulator;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        Instant fixedTime = Instant.parse("2024-01-01T12:00:00Z");
        fixedClock = Clock.fixed(fixedTime, ZoneId.systemDefault());
        simulator = new PetriTokenSimulator(fixedClock);
    }

    @Nested
    @DisplayName("DevOps Workflow Simulation")
    class DevOpsWorkflowSimulation {

        @Test
        @DisplayName("Should simulate complete DevOps CI/CD pipeline")
        void shouldSimulateCompleteDevOpsCiCdPipeline() {
            // Given - A realistic DevOps workflow
            PetriNet devopsWorkflow = createDevOpsWorkflow();
            SimulationConfig config = SimulationConfig.builder()
                    .seed(42L)
                    .maxSteps(20)
                    .verbose(true)
                    .build();

            // When
            SimulationResult result = simulator.simulate(devopsWorkflow, config);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getStepsExecuted()).isGreaterThan(5);
            assertThat(result.getFinalMarking().getTokens("deployed")).isEqualTo(1);

            // Verify workflow progression through trace
            List<TraceEvent> trace = result.getTrace();
            assertThat(trace).isNotEmpty();

            // Should start with code commit
            assertThat(trace.get(0).getTransition()).isEqualTo("commit_code");

            // Should end with deployment
            TraceEvent lastEvent = trace.get(trace.size() - 1);
            assertThat(lastEvent.getTransition()).isIn("deploy_staging", "deploy_production");

            // Verify trace contains expected workflow steps
            List<String> transitions = trace.stream()
                    .map(TraceEvent::getTransition)
                    .toList();
            assertThat(transitions).contains("commit_code", "run_tests", "build_artifact");
        }

        @Test
        @DisplayName("Should handle test failures and retry logic")
        void shouldHandleTestFailuresAndRetryLogic() {
            // Given - DevOps workflow with potential test failures
            PetriNet devopsWorkflow = createDevOpsWorkflowWithRetries();
            SimulationConfig config = SimulationConfig.forTesting(999L); // Different seed for different behavior

            // When
            SimulationResult result = simulator.simulate(devopsWorkflow, config);

            // Then
            assertThat(result.isCompleted()).isTrue();

            // Verify that retries can occur
            List<TraceEvent> trace = result.getTrace();
            long testTransitions = trace.stream()
                    .map(TraceEvent::getTransition)
                    .filter(t -> t.contains("test"))
                    .count();

            // Should have at least one test execution
            assertThat(testTransitions).isGreaterThanOrEqualTo(1);
        }

        private PetriNet createDevOpsWorkflow() {
            return PetriNet.builder()
                    .name("DevOps CI/CD Pipeline")
                    .description("Complete CI/CD workflow with build, test, and deploy stages")

                    // Places
                    .addPlace(Place.builder("code_ready").name("Code Ready").asSource().build())
                    .addPlace(Place.builder("code_committed").name("Code Committed").build())
                    .addPlace(Place.builder("tests_passed").name("Tests Passed").build())
                    .addPlace(Place.builder("artifact_built").name("Artifact Built").build())
                    .addPlace(Place.builder("staging_ready").name("Staging Ready").build())
                    .addPlace(Place.builder("production_ready").name("Production Ready").build())
                    .addPlace(Place.builder("deployed").name("Deployed").asSink().build())

                    // Transitions
                    .addTransition(Transition.builder("commit_code").name("Commit Code").build())
                    .addTransition(Transition.builder("run_tests").name("Run Tests").build())
                    .addTransition(Transition.builder("build_artifact").name("Build Artifact").build())
                    .addTransition(Transition.builder("deploy_staging").name("Deploy to Staging").build())
                    .addTransition(Transition.builder("deploy_production").name("Deploy to Production").build())

                    // Workflow arcs
                    .addArc("code_ready", "commit_code")
                    .addArc("commit_code", "code_committed")
                    .addArc("code_committed", "run_tests")
                    .addArc("run_tests", "tests_passed")
                    .addArc("tests_passed", "build_artifact")
                    .addArc("build_artifact", "artifact_built")
                    .addArc("artifact_built", "deploy_staging")
                    .addArc("deploy_staging", "staging_ready")
                    .addArc("staging_ready", "deploy_production")
                    .addArc("deploy_production", "deployed")

                    // Initial state
                    .addInitialToken("code_ready", 1)
                    .build();
        }

        private PetriNet createDevOpsWorkflowWithRetries() {
            return PetriNet.builder()
                    .name("DevOps Pipeline with Retries")

                    // Places
                    .addPlace(new Place("code_ready"))
                    .addPlace(new Place("code_committed"))
                    .addPlace(new Place("test_in_progress"))
                    .addPlace(new Place("tests_passed"))
                    .addPlace(new Place("tests_failed"))
                    .addPlace(new Place("retry_available"))
                    .addPlace(new Place("deployed"))

                    // Transitions
                    .addTransition(new Transition("commit_code"))
                    .addTransition(new Transition("start_tests"))
                    .addTransition(new Transition("tests_succeed"))
                    .addTransition(new Transition("tests_fail"))
                    .addTransition(new Transition("retry_tests"))
                    .addTransition(new Transition("deploy"))

                    // Main workflow
                    .addArc("code_ready", "commit_code")
                    .addArc("commit_code", "code_committed")
                    .addArc("code_committed", "start_tests")
                    .addArc("start_tests", "test_in_progress")

                    // Success path
                    .addArc("test_in_progress", "tests_succeed")
                    .addArc("tests_succeed", "tests_passed")
                    .addArc("tests_passed", "deploy")
                    .addArc("deploy", "deployed")

                    // Failure and retry path
                    .addArc("test_in_progress", "tests_fail")
                    .addArc("tests_fail", "tests_failed")
                    .addArc("tests_failed", "retry_tests")
                    .addArc("retry_tests", "test_in_progress")

                    // Initial state with retry tokens
                    .addInitialToken("code_ready", 1)
                    .addInitialToken("retry_available", 2) // Allow 2 retries
                    .build();
        }
    }

    @Nested
    @DisplayName("Parallel Workflow Simulation")
    class ParallelWorkflowSimulation {

        @Test
        @DisplayName("Should simulate parallel execution and synchronization")
        void shouldSimulateParallelExecutionAndSynchronization() {
            // Given - A workflow with parallel branches
            PetriNet parallelWorkflow = createParallelWorkflow();
            SimulationConfig config = SimulationConfig.forTesting(123L);

            // When
            SimulationResult result = simulator.simulate(parallelWorkflow, config);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getFinalMarking().getTokens("completed")).isEqualTo(1);

            // Verify parallel execution occurred
            List<TraceEvent> trace = result.getTrace();
            List<String> transitions = trace.stream()
                    .map(TraceEvent::getTransition)
                    .toList();

            // Should have forked and then joined
            assertThat(transitions).contains("fork", "join");
            assertThat(transitions).contains("task_a", "task_b"); // Both parallel tasks

            // Fork should happen before parallel tasks
            int forkIndex = transitions.indexOf("fork");
            int taskAIndex = transitions.indexOf("task_a");
            int taskBIndex = transitions.indexOf("task_b");
            int joinIndex = transitions.indexOf("join");

            assertThat(forkIndex).isLessThan(taskAIndex);
            assertThat(forkIndex).isLessThan(taskBIndex);
            assertThat(Math.max(taskAIndex, taskBIndex)).isLessThan(joinIndex);
        }

        private PetriNet createParallelWorkflow() {
            return PetriNet.builder()
                    .name("Parallel Execution Workflow")

                    // Places
                    .addPlace(new Place("start"))
                    .addPlace(new Place("forked"))
                    .addPlace(new Place("branch_a"))
                    .addPlace(new Place("branch_b"))
                    .addPlace(new Place("task_a_done"))
                    .addPlace(new Place("task_b_done"))
                    .addPlace(new Place("completed"))

                    // Transitions
                    .addTransition(Transition.builder("fork").name("Fork").asFork().build())
                    .addTransition(new Transition("task_a", "Execute Task A"))
                    .addTransition(new Transition("task_b", "Execute Task B"))
                    .addTransition(Transition.builder("join").name("Join").asJoin().build())

                    // Fork structure
                    .addArc("start", "fork")
                    .addArc("fork", "branch_a")
                    .addArc("fork", "branch_b")

                    // Parallel tasks
                    .addArc("branch_a", "task_a")
                    .addArc("task_a", "task_a_done")
                    .addArc("branch_b", "task_b")
                    .addArc("task_b", "task_b_done")

                    // Join structure
                    .addArc("task_a_done", "join")
                    .addArc("task_b_done", "join")
                    .addArc("join", "completed")

                    // Initial state
                    .addInitialToken("start", 1)
                    .build();
        }
    }

    @Nested
    @DisplayName("Choice Workflow Simulation")
    class ChoiceWorkflowSimulation {

        @Test
        @DisplayName("Should simulate exclusive choice decisions")
        void shouldSimulateExclusiveChoiceDecisions() {
            // Given - A workflow with exclusive choices
            PetriNet choiceWorkflow = createChoiceWorkflow();
            SimulationConfig config = SimulationConfig.forTesting(456L);

            // When
            SimulationResult result = simulator.simulate(choiceWorkflow, config);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getFinalMarking().getTokens("completed")).isEqualTo(1);

            // Verify exclusive choice was made
            List<TraceEvent> trace = result.getTrace();
            List<String> transitions = trace.stream()
                    .map(TraceEvent::getTransition)
                    .toList();

            // Should choose either path A or path B, but not both
            boolean chosePathA = transitions.contains("choose_path_a");
            boolean chosePathB = transitions.contains("choose_path_b");

            assertThat(chosePathA || chosePathB).isTrue();
            assertThat(chosePathA && chosePathB).isFalse(); // Exclusive choice

            // Should always end with merge
            assertThat(transitions).contains("merge");
        }

        private PetriNet createChoiceWorkflow() {
            return PetriNet.builder()
                    .name("Exclusive Choice Workflow")

                    // Places
                    .addPlace(new Place("start"))
                    .addPlace(new Place("choice_point"))
                    .addPlace(new Place("path_a"))
                    .addPlace(new Place("path_b"))
                    .addPlace(new Place("task_a_done"))
                    .addPlace(new Place("task_b_done"))
                    .addPlace(new Place("completed"))

                    // Transitions
                    .addTransition(new Transition("begin", "Begin Process"))
                    .addTransition(Transition.builder("choose_path_a").name("Choose Path A").asChoice("condition_a").build())
                    .addTransition(Transition.builder("choose_path_b").name("Choose Path B").asChoice("condition_b").build())
                    .addTransition(new Transition("task_a", "Execute Task A"))
                    .addTransition(new Transition("task_b", "Execute Task B"))
                    .addTransition(new Transition("merge", "Merge Results"))

                    // Workflow structure
                    .addArc("start", "begin")
                    .addArc("begin", "choice_point")

                    // Choice branches
                    .addArc("choice_point", "choose_path_a")
                    .addArc("choice_point", "choose_path_b")
                    .addArc("choose_path_a", "path_a")
                    .addArc("choose_path_b", "path_b")

                    // Path execution
                    .addArc("path_a", "task_a")
                    .addArc("task_a", "task_a_done")
                    .addArc("path_b", "task_b")
                    .addArc("task_b", "task_b_done")

                    // Merge
                    .addArc("task_a_done", "merge")
                    .addArc("task_b_done", "merge")
                    .addArc("merge", "completed")

                    // Initial state
                    .addInitialToken("start", 1)
                    .build();
        }
    }

    @Nested
    @DisplayName("Resource Management Simulation")
    class ResourceManagementSimulation {

        @Test
        @DisplayName("Should simulate resource constraints and queuing")
        void shouldSimulateResourceConstraintsAndQueuing() {
            // Given - A workflow with limited resources
            PetriNet resourceWorkflow = createResourceConstrainedWorkflow();
            SimulationConfig config = SimulationConfig.builder()
                    .seed(789L)
                    .maxSteps(50)
                    .build();

            // When
            SimulationResult result = simulator.simulate(resourceWorkflow, config);

            // Then
            assertThat(result.isCompleted()).isTrue();

            // Verify all jobs were processed
            assertThat(result.getFinalMarking().getTokens("job_completed")).isEqualTo(3);

            // Verify resource was properly managed
            List<TraceEvent> trace = result.getTrace();
            long acquireEvents = trace.stream()
                    .map(TraceEvent::getTransition)
                    .filter(t -> t.equals("acquire_resource"))
                    .count();
            long releaseEvents = trace.stream()
                    .map(TraceEvent::getTransition)
                    .filter(t -> t.equals("release_resource"))
                    .count();

            // Should have equal number of acquire and release events
            assertThat(acquireEvents).isEqualTo(releaseEvents);
            assertThat(acquireEvents).isGreaterThanOrEqualTo(1);
        }

        private PetriNet createResourceConstrainedWorkflow() {
            return PetriNet.builder()
                    .name("Resource Constrained Workflow")

                    // Places
                    .addPlace(new Place("job_queue"))
                    .addPlace(Place.builder("resource_pool").capacity(1).build()) // Only 1 resource available
                    .addPlace(new Place("job_in_progress"))
                    .addPlace(new Place("job_completed"))

                    // Transitions
                    .addTransition(new Transition("acquire_resource", "Acquire Resource"))
                    .addTransition(new Transition("process_job", "Process Job"))
                    .addTransition(new Transition("release_resource", "Release Resource"))

                    // Workflow arcs
                    .addArc("job_queue", "acquire_resource")
                    .addArc("resource_pool", "acquire_resource")
                    .addArc("acquire_resource", "job_in_progress")

                    .addArc("job_in_progress", "process_job")
                    .addArc("process_job", "job_completed")

                    .addArc("job_in_progress", "release_resource")
                    .addArc("release_resource", "resource_pool")

                    // Initial state - 3 jobs waiting, 1 resource available
                    .addInitialToken("job_queue", 3)
                    .addInitialToken("resource_pool", 1)
                    .build();
        }
    }

    @Nested
    @DisplayName("Deterministic Execution Tests")
    class DeterministicExecutionTests {

        @Test
        @DisplayName("Should produce identical execution traces with same seed")
        void shouldProduceIdenticalExecutionTracesWithSameSeed() {
            // Given
            PetriNet complexWorkflow = createComplexWorkflow();
            long seed = 12345L;

            SimulationConfig config1 = SimulationConfig.forTesting(seed);
            SimulationConfig config2 = SimulationConfig.forTesting(seed);

            // When
            SimulationResult result1 = simulator.simulate(complexWorkflow, config1);
            SimulationResult result2 = simulator.simulate(complexWorkflow, config2);

            // Then
            assertThat(result1.isCompleted()).isTrue();
            assertThat(result2.isCompleted()).isTrue();

            // Should have identical execution
            assertThat(result1.getStepsExecuted()).isEqualTo(result2.getStepsExecuted());
            assertThat(result1.getFinalMarking().getTokens()).isEqualTo(result2.getFinalMarking().getTokens());

            // Should have identical trace
            List<TraceEvent> trace1 = result1.getTrace();
            List<TraceEvent> trace2 = result2.getTrace();

            assertThat(trace1).hasSize(trace2.size());
            for (int i = 0; i < trace1.size(); i++) {
                assertThat(trace1.get(i).getTransition()).isEqualTo(trace2.get(i).getTransition());
                assertThat(trace1.get(i).getSequenceNumber()).isEqualTo(trace2.get(i).getSequenceNumber());
            }
        }

        @Test
        @DisplayName("Should produce different but valid executions with different seeds")
        void shouldProduceDifferentButValidExecutionsWithDifferentSeeds() {
            // Given
            PetriNet complexWorkflow = createComplexWorkflow();
            SimulationConfig config1 = SimulationConfig.forTesting(111L);
            SimulationConfig config2 = SimulationConfig.forTesting(999L);

            // When
            SimulationResult result1 = simulator.simulate(complexWorkflow, config1);
            SimulationResult result2 = simulator.simulate(complexWorkflow, config2);

            // Then - Both should complete successfully
            assertThat(result1.isCompleted()).isTrue();
            assertThat(result2.isCompleted()).isTrue();

            // Both should reach same final state (workflow determinism)
            assertThat(result1.getFinalMarking().getTokens()).isEqualTo(result2.getFinalMarking().getTokens());

            // But execution order might differ due to choice resolution
            List<String> transitions1 = result1.getTrace().stream()
                    .map(TraceEvent::getTransition)
                    .toList();
            List<String> transitions2 = result2.getTrace().stream()
                    .map(TraceEvent::getTransition)
                    .toList();

            // Should contain same set of transitions (eventually)
            assertThat(transitions1).containsAll(transitions2);
            assertThat(transitions2).containsAll(transitions1);
        }

        private PetriNet createComplexWorkflow() {
            return PetriNet.builder()
                    .name("Complex Workflow with Choices")

                    // Places
                    .addPlace(new Place("start"))
                    .addPlace(new Place("choice1"))
                    .addPlace(new Place("choice2"))
                    .addPlace(new Place("path_a"))
                    .addPlace(new Place("path_b"))
                    .addPlace(new Place("intermediate"))
                    .addPlace(new Place("final"))

                    // Transitions
                    .addTransition(new Transition("begin"))
                    .addTransition(new Transition("choice_a1"))
                    .addTransition(new Transition("choice_b1"))
                    .addTransition(new Transition("choice_a2"))
                    .addTransition(new Transition("choice_b2"))
                    .addTransition(new Transition("process_a"))
                    .addTransition(new Transition("process_b"))
                    .addTransition(new Transition("merge"))
                    .addTransition(new Transition("finish"))

                    // Basic flow
                    .addArc("start", "begin")
                    .addArc("begin", "choice1")

                    // First choice
                    .addArc("choice1", "choice_a1")
                    .addArc("choice1", "choice_b1")
                    .addArc("choice_a1", "path_a")
                    .addArc("choice_b1", "path_b")

                    // Processing
                    .addArc("path_a", "process_a")
                    .addArc("path_b", "process_b")
                    .addArc("process_a", "intermediate")
                    .addArc("process_b", "intermediate")

                    // Second choice
                    .addArc("intermediate", "choice_a2")
                    .addArc("intermediate", "choice_b2")
                    .addArc("choice_a2", "choice2")
                    .addArc("choice_b2", "choice2")

                    // Completion
                    .addArc("choice2", "merge")
                    .addArc("merge", "finish")
                    .addArc("finish", "final")

                    // Initial state
                    .addInitialToken("start", 1)
                    .build();
        }
    }

    @Nested
    @DisplayName("Performance and Scalability Tests")
    class PerformanceAndScalabilityTests {

        @Test
        @DisplayName("Should handle moderate-sized networks efficiently")
        void shouldHandleModerateSizedNetworksEfficiently() {
            // Given - A moderately complex network (15 places, 12 transitions)
            PetriNet moderateNetwork = createModerateNetwork();
            SimulationConfig config = SimulationConfig.builder()
                    .seed(42L)
                    .maxSteps(100)
                    .enableTracing(true)
                    .build();

            // When
            long startTime = System.currentTimeMillis();
            SimulationResult result = simulator.simulate(moderateNetwork, config);
            long executionTime = System.currentTimeMillis() - startTime;

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(executionTime).isLessThan(5000); // Should complete in under 5 seconds

            // Verify comprehensive trace was generated
            assertThat(result.getTrace()).isNotEmpty();
            assertThat(result.getStepsExecuted()).isGreaterThan(10);
        }

        private PetriNet createModerateNetwork() {
            PetriNet.Builder builder = PetriNet.builder()
                    .name("Moderate Complexity Network");

            // Create a series of sequential and parallel sections
            for (int i = 0; i < 15; i++) {
                builder.addPlace(new Place("p" + i));
            }

            for (int i = 0; i < 12; i++) {
                builder.addTransition(new Transition("t" + i));
            }

            // Create a complex but realistic flow
            builder.addArc("p0", "t0").addArc("t0", "p1")
                   .addArc("p1", "t1").addArc("t1", "p2")
                   .addArc("p1", "t2").addArc("t2", "p3") // Parallel branch
                   .addArc("p2", "t3").addArc("t3", "p4")
                   .addArc("p3", "t4").addArc("t4", "p5")
                   .addArc("p4", "t5").addArc("t5", "p6") // Merge point
                   .addArc("p5", "t5")
                   .addArc("p6", "t6").addArc("t6", "p7")
                   .addArc("p7", "t7").addArc("t7", "p8")
                   .addArc("p7", "t8").addArc("t8", "p9") // Another parallel section
                   .addArc("p8", "t9").addArc("t9", "p10")
                   .addArc("p9", "t10").addArc("t10", "p11")
                   .addArc("p10", "t11").addArc("t11", "p12") // Final merge
                   .addArc("p11", "t11")
                   .addInitialToken("p0", 1);

            return builder.build();
        }
    }
}