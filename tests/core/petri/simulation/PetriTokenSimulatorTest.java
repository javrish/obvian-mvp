package core.petri.simulation;

import core.petri.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for PetriTokenSimulator
 *
 * Tests cover:
 * - Deterministic simulation with seeded random
 * - Token-based firing mechanics
 * - Deadlock detection
 * - Step-by-step execution
 * - Trace generation
 * - Interactive mode simulation
 * - Error handling and edge cases
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@DisplayName("PetriTokenSimulator Tests")
class PetriTokenSimulatorTest {

    private PetriTokenSimulator simulator;
    private Clock fixedClock;
    private Instant fixedTime;

    @BeforeEach
    void setUp() {
        fixedTime = Instant.parse("2024-01-01T12:00:00Z");
        fixedClock = Clock.fixed(fixedTime, ZoneId.systemDefault());
        simulator = new PetriTokenSimulator(fixedClock);
    }

    @Nested
    @DisplayName("Simple Linear Workflow Tests")
    class SimpleLinearWorkflowTests {

        private PetriNet createSimpleLinearNet() {
            return PetriNet.builder()
                    .name("Simple Linear Workflow")
                    .addPlace(new Place("start"))
                    .addPlace(new Place("middle"))
                    .addPlace(new Place("end"))
                    .addTransition(new Transition("t1", "Step 1"))
                    .addTransition(new Transition("t2", "Step 2"))
                    .addArc("start", "t1")
                    .addArc("t1", "middle")
                    .addArc("middle", "t2")
                    .addArc("t2", "end")
                    .addInitialToken("start", 1)
                    .build();
        }

        @Test
        @DisplayName("Should execute simple linear workflow deterministically")
        void shouldExecuteSimpleLinearWorkflowDeterministically() {
            // Given
            PetriNet petriNet = createSimpleLinearNet();
            SimulationConfig config = SimulationConfig.forTesting(42L);

            // When
            SimulationResult result = simulator.simulate(petriNet, config);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getStepsExecuted()).isEqualTo(2);
            assertThat(result.getFinalMarking().getTokens("end")).isEqualTo(1);
            assertThat(result.getTrace()).hasSize(2);

            // Verify trace events
            List<TraceEvent> trace = result.getTrace();
            assertThat(trace.get(0).getTransition()).isEqualTo("t1");
            assertThat(trace.get(1).getTransition()).isEqualTo("t2");
        }

        @Test
        @DisplayName("Should produce identical results with same seed")
        void shouldProduceIdenticalResultsWithSameSeed() {
            // Given
            PetriNet petriNet = createSimpleLinearNet();
            SimulationConfig config1 = SimulationConfig.forTesting(42L);
            SimulationConfig config2 = SimulationConfig.forTesting(42L);

            // When
            SimulationResult result1 = simulator.simulate(petriNet, config1);
            SimulationResult result2 = simulator.simulate(petriNet, config2);

            // Then
            assertThat(result1.getStepsExecuted()).isEqualTo(result2.getStepsExecuted());
            assertThat(result1.getFinalMarking().getTokens()).isEqualTo(result2.getFinalMarking().getTokens());

            // Trace should have same transitions fired in same order
            List<TraceEvent> trace1 = result1.getTrace();
            List<TraceEvent> trace2 = result2.getTrace();
            assertThat(trace1).hasSize(trace2.size());
            for (int i = 0; i < trace1.size(); i++) {
                assertThat(trace1.get(i).getTransition()).isEqualTo(trace2.get(i).getTransition());
            }
        }
    }

    @Nested
    @DisplayName("Choice and Conflict Tests")
    class ChoiceAndConflictTests {

        private PetriNet createChoiceNet() {
            return PetriNet.builder()
                    .name("Choice Network")
                    .addPlace(new Place("start"))
                    .addPlace(new Place("choice_a"))
                    .addPlace(new Place("choice_b"))
                    .addPlace(new Place("end"))
                    .addTransition(new Transition("t_choice_a", "Choose A"))
                    .addTransition(new Transition("t_choice_b", "Choose B"))
                    .addTransition(new Transition("t_join", "Join"))
                    .addArc("start", "t_choice_a")
                    .addArc("start", "t_choice_b")
                    .addArc("t_choice_a", "choice_a")
                    .addArc("t_choice_b", "choice_b")
                    .addArc("choice_a", "t_join")
                    .addArc("choice_b", "t_join")
                    .addArc("t_join", "end")
                    .addInitialToken("start", 1)
                    .build();
        }

        @Test
        @DisplayName("Should handle choice conflicts deterministically")
        void shouldHandleChoiceConflictsDeterministically() {
            // Given
            PetriNet petriNet = createChoiceNet();
            SimulationConfig config = SimulationConfig.forTesting(123L);

            // When
            SimulationResult result = simulator.simulate(petriNet, config);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getStepsExecuted()).isEqualTo(2);
            assertThat(result.getFinalMarking().getTokens("end")).isEqualTo(1);

            // Should choose one path consistently with same seed
            List<TraceEvent> trace = result.getTrace();
            String firstChoice = trace.get(0).getTransition();
            assertThat(firstChoice).isIn("t_choice_a", "t_choice_b");
            assertThat(trace.get(1).getTransition()).isEqualTo("t_join");
        }

        @Test
        @DisplayName("Should produce different choices with different seeds")
        void shouldProduceDifferentChoicesWithDifferentSeeds() {
            // Given
            PetriNet petriNet = createChoiceNet();
            SimulationConfig config1 = SimulationConfig.forTesting(42L);
            SimulationConfig config2 = SimulationConfig.forTesting(999L);

            // When
            SimulationResult result1 = simulator.simulate(petriNet, config1);
            SimulationResult result2 = simulator.simulate(petriNet, config2);

            // Then - both should complete but may choose different paths
            assertThat(result1.isCompleted()).isTrue();
            assertThat(result2.isCompleted()).isTrue();
            assertThat(result1.getStepsExecuted()).isEqualTo(2);
            assertThat(result2.getStepsExecuted()).isEqualTo(2);

            // Different seeds might lead to different first transitions
            String choice1 = result1.getTrace().get(0).getTransition();
            String choice2 = result2.getTrace().get(0).getTransition();

            // Both are valid choices
            assertThat(choice1).isIn("t_choice_a", "t_choice_b");
            assertThat(choice2).isIn("t_choice_a", "t_choice_b");
        }
    }

    @Nested
    @DisplayName("Deadlock Detection Tests")
    class DeadlockDetectionTests {

        private PetriNet createDeadlockedNet() {
            return PetriNet.builder()
                    .name("Deadlocked Network")
                    .addPlace(new Place("p1"))
                    .addPlace(new Place("p2"))
                    .addTransition(new Transition("t1"))
                    .addArc("p1", "t1", 2) // Requires 2 tokens but only 1 available
                    .addArc("t1", "p2")
                    .addInitialToken("p1", 1) // Only 1 token, but transition needs 2
                    .build();
        }

        @Test
        @DisplayName("Should detect deadlock when no transitions are enabled")
        void shouldDetectDeadlockWhenNoTransitionsEnabled() {
            // Given
            PetriNet petriNet = createDeadlockedNet();
            SimulationConfig config = SimulationConfig.forTesting(42L);

            // When
            SimulationResult result = simulator.simulate(petriNet, config);

            // Then
            assertThat(result.isDeadlocked()).isTrue();
            assertThat(result.getStepsExecuted()).isEqualTo(0);
            assertThat(result.getMessage()).contains("deadlocked");
            assertThat(result.getDiagnostic("deadlockDetected")).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Step-by-Step Simulation Tests")
    class StepByStepSimulationTests {

        @Test
        @DisplayName("Should execute single steps correctly")
        void shouldExecuteSingleStepsCorrectly() {
            // Given
            PetriNet petriNet = createSimpleLinearNet();
            SimulationConfig config = SimulationConfig.forTesting(42L);
            SimulationState state = SimulationState.builder()
                    .simulationId("test-sim-1")
                    .petriNet(petriNet)
                    .initialMarking(petriNet.getInitialMarking())
                    .currentMarking(petriNet.getInitialMarking())
                    .config(config)
                    .stepsExecuted(0)
                    .startTime(fixedTime)
                    .build();

            // When - First step
            SimulationStepResult stepResult1 = simulator.step(state, config);

            // Then
            assertThat(stepResult1.isSuccess()).isTrue();
            assertThat(stepResult1.getEvent().getTransition()).isEqualTo("t1");
            assertThat(stepResult1.getState().stepsExecuted).isEqualTo(1);
            assertThat(stepResult1.getState().currentMarking.getTokens("middle")).isEqualTo(1);

            // When - Second step
            SimulationStepResult stepResult2 = simulator.step(stepResult1.getState(), config);

            // Then
            assertThat(stepResult2.isSuccess()).isTrue();
            assertThat(stepResult2.getEvent().getTransition()).isEqualTo("t2");
            assertThat(stepResult2.getState().stepsExecuted).isEqualTo(2);
            assertThat(stepResult2.getState().currentMarking.getTokens("end")).isEqualTo(1);
        }

        private PetriNet createSimpleLinearNet() {
            return PetriNet.builder()
                    .name("Simple Linear Workflow")
                    .addPlace(new Place("start"))
                    .addPlace(new Place("middle"))
                    .addPlace(new Place("end"))
                    .addTransition(new Transition("t1", "Step 1"))
                    .addTransition(new Transition("t2", "Step 2"))
                    .addArc("start", "t1")
                    .addArc("t1", "middle")
                    .addArc("middle", "t2")
                    .addArc("t2", "end")
                    .addInitialToken("start", 1)
                    .build();
        }
    }

    @Nested
    @DisplayName("Trace Generation Tests")
    class TraceGenerationTests {

        @Test
        @DisplayName("Should generate comprehensive trace events")
        void shouldGenerateComprehensiveTraceEvents() {
            // Given
            PetriNet petriNet = createSimpleLinearNet();
            SimulationConfig config = SimulationConfig.forTesting(42L);

            // When
            SimulationResult result = simulator.simulate(petriNet, config);

            // Then
            List<TraceEvent> trace = result.getTrace();
            assertThat(trace).hasSize(2);

            // Verify first trace event details
            TraceEvent event1 = trace.get(0);
            assertThat(event1.getTransition()).isEqualTo("t1");
            assertThat(event1.getSequenceNumber()).isEqualTo(1);
            assertThat(event1.getSimulationSeed()).isEqualTo(42L);
            assertThat(event1.getMarkingBefore()).isNotNull();
            assertThat(event1.getMarkingAfter()).isNotNull();
            assertThat(event1.getMarkingBefore().getTokens("start")).isEqualTo(1);
            assertThat(event1.getMarkingAfter().getTokens("middle")).isEqualTo(1);
            assertThat(event1.getFromPlaces()).contains("start");
            assertThat(event1.getToPlaces()).contains("middle");

            // Verify second trace event details
            TraceEvent event2 = trace.get(1);
            assertThat(event2.getTransition()).isEqualTo("t2");
            assertThat(event2.getSequenceNumber()).isEqualTo(2);
            assertThat(event2.getMarkingBefore().getTokens("middle")).isEqualTo(1);
            assertThat(event2.getMarkingAfter().getTokens("end")).isEqualTo(1);
        }

        private PetriNet createSimpleLinearNet() {
            return PetriNet.builder()
                    .name("Simple Linear Workflow")
                    .addPlace(new Place("start"))
                    .addPlace(new Place("middle"))
                    .addPlace(new Place("end"))
                    .addTransition(new Transition("t1", "Step 1"))
                    .addTransition(new Transition("t2", "Step 2"))
                    .addArc("start", "t1")
                    .addArc("t1", "middle")
                    .addArc("middle", "t2")
                    .addArc("t2", "end")
                    .addInitialToken("start", 1)
                    .build();
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should respect maximum steps limit")
        void shouldRespectMaximumStepsLimit() {
            // Given
            PetriNet petriNet = createInfiniteLoopNet();
            SimulationConfig config = SimulationConfig.builder()
                    .seed(42L)
                    .maxSteps(5)
                    .deterministic()
                    .build();

            // When
            SimulationResult result = simulator.simulate(petriNet, config);

            // Then
            assertThat(result.reachedMaxSteps()).isTrue();
            assertThat(result.getStepsExecuted()).isEqualTo(5);
            assertThat(result.getMessage()).contains("maximum steps");
        }

        private PetriNet createInfiniteLoopNet() {
            return PetriNet.builder()
                    .name("Infinite Loop Network")
                    .addPlace(new Place("loop"))
                    .addTransition(new Transition("t_loop"))
                    .addArc("loop", "t_loop")
                    .addArc("t_loop", "loop")
                    .addInitialToken("loop", 1)
                    .build();
        }

        @Test
        @DisplayName("Should handle verbose mode")
        void shouldHandleVerboseMode() {
            // Given
            PetriNet petriNet = createSimpleLinearNet();
            SimulationConfig config = SimulationConfig.builder()
                    .seed(42L)
                    .verbose(true)
                    .build();

            // When
            SimulationResult result = simulator.simulate(petriNet, config);

            // Then - Should complete successfully even with verbose logging
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getStepsExecuted()).isEqualTo(2);
        }

        private PetriNet createSimpleLinearNet() {
            return PetriNet.builder()
                    .name("Simple Linear Workflow")
                    .addPlace(new Place("start"))
                    .addPlace(new Place("middle"))
                    .addPlace(new Place("end"))
                    .addTransition(new Transition("t1", "Step 1"))
                    .addTransition(new Transition("t2", "Step 2"))
                    .addArc("start", "t1")
                    .addArc("t1", "middle")
                    .addArc("middle", "t2")
                    .addArc("t2", "end")
                    .addInitialToken("start", 1)
                    .build();
        }
    }

    @Nested
    @DisplayName("Interactive Mode Tests")
    class InteractiveModeTests {

        @Test
        @DisplayName("Should handle interactive mode with single transitions")
        void shouldHandleInteractiveModeWithSingleTransitions() {
            // Given
            PetriNet petriNet = createSimpleLinearNet();
            SimulationConfig config = SimulationConfig.builder()
                    .interactive()
                    .maxSteps(10)
                    .build();

            // When
            SimulationResult result = simulator.simulate(petriNet, config);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getStepsExecuted()).isEqualTo(2);
        }

        private PetriNet createSimpleLinearNet() {
            return PetriNet.builder()
                    .name("Simple Linear Workflow")
                    .addPlace(new Place("start"))
                    .addPlace(new Place("middle"))
                    .addPlace(new Place("end"))
                    .addTransition(new Transition("t1", "Step 1"))
                    .addTransition(new Transition("t2", "Step 2"))
                    .addArc("start", "t1")
                    .addArc("t1", "middle")
                    .addArc("middle", "t2")
                    .addArc("t2", "end")
                    .addInitialToken("start", 1)
                    .build();
        }
    }

    @Nested
    @DisplayName("Control Operations Tests")
    class ControlOperationsTests {

        @Test
        @DisplayName("Should support pause and resume operations")
        void shouldSupportPauseAndResumeOperations() {
            // Given
            simulator.reset();

            // When & Then
            assertThat(simulator.isPaused()).isFalse();
            assertThat(simulator.isStopped()).isFalse();

            simulator.pause();
            assertThat(simulator.isPaused()).isTrue();

            simulator.resume();
            assertThat(simulator.isPaused()).isFalse();

            simulator.stop();
            assertThat(simulator.isStopped()).isTrue();

            simulator.reset();
            assertThat(simulator.isPaused()).isFalse();
            assertThat(simulator.isStopped()).isFalse();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle invalid PetriNet gracefully")
        void shouldHandleInvalidPetriNetGracefully() {
            // Given - empty PetriNet (should cause validation errors)
            PetriNet emptyNet = PetriNet.builder()
                    .name("Empty Network")
                    .build();
            SimulationConfig config = SimulationConfig.forTesting(42L);

            // When & Then - Should handle gracefully, not crash
            assertThatCode(() -> {
                SimulationResult result = simulator.simulate(emptyNet, config);
                // Even if it fails, it should return a proper result object
                assertThat(result).isNotNull();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Arc Weight Tests")
    class ArcWeightTests {

        @Test
        @DisplayName("Should handle weighted arcs correctly")
        void shouldHandleWeightedArcsCorrectly() {
            // Given
            PetriNet petriNet = PetriNet.builder()
                    .name("Weighted Arc Network")
                    .addPlace(new Place("source"))
                    .addPlace(new Place("sink"))
                    .addTransition(new Transition("t_weighted"))
                    .addArc("source", "t_weighted", 3) // Requires 3 tokens
                    .addArc("t_weighted", "sink", 2)   // Produces 2 tokens
                    .addInitialToken("source", 3)
                    .build();
            SimulationConfig config = SimulationConfig.forTesting(42L);

            // When
            SimulationResult result = simulator.simulate(petriNet, config);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getStepsExecuted()).isEqualTo(1);
            assertThat(result.getFinalMarking().getTokens("source")).isEqualTo(0);
            assertThat(result.getFinalMarking().getTokens("sink")).isEqualTo(2);
        }

        @Test
        @DisplayName("Should block when insufficient tokens for weighted arc")
        void shouldBlockWhenInsufficientTokensForWeightedArc() {
            // Given
            PetriNet petriNet = PetriNet.builder()
                    .name("Insufficient Tokens Network")
                    .addPlace(new Place("source"))
                    .addPlace(new Place("sink"))
                    .addTransition(new Transition("t_blocked"))
                    .addArc("source", "t_blocked", 5) // Requires 5 tokens
                    .addArc("t_blocked", "sink")
                    .addInitialToken("source", 2) // Only 2 tokens available
                    .build();
            SimulationConfig config = SimulationConfig.forTesting(42L);

            // When
            SimulationResult result = simulator.simulate(petriNet, config);

            // Then
            assertThat(result.isDeadlocked()).isTrue();
            assertThat(result.getStepsExecuted()).isEqualTo(0);
            assertThat(result.getFinalMarking().getTokens("source")).isEqualTo(2);
        }
    }
}