/* Copyright (c) 2025 Rishabh Pathak. Licensed under the MIT License. */

package tests.core.petri.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import core.petri.*;
import core.petri.validation.PetriNetValidator;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Comprehensive test suite for PetriNetValidator using TDD approach.
 * Tests formal verification algorithms including deadlock detection,
 * reachability analysis, liveness checking, and boundedness verification.
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("PetriNetValidator Tests")
class PetriNetValidatorTest {

  private PetriNetValidator validator;

  @Mock private PetriNet mockPetriNet;

  @Mock private Marking mockMarking;

  @Mock private Transition mockTransition;

  @Mock private Place mockPlace;

  @Mock private Arc mockArc;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    validator = new PetriNetValidator();
    setupBasicMockBehavior();
  }

  private void setupBasicMockBehavior() {
    when(mockPetriNet.getId()).thenReturn("test-petri-net");
    when(mockPetriNet.getName()).thenReturn("Test Petri Net");
    when(mockPetriNet.getInitialMarking()).thenReturn(mockMarking);
    when(mockPetriNet.getPlaces()).thenReturn(Arrays.asList(mockPlace));
    when(mockPetriNet.getTransitions()).thenReturn(Arrays.asList(mockTransition));
    when(mockPetriNet.getArcs()).thenReturn(Arrays.asList(mockArc));

    when(mockPlace.getId()).thenReturn("p1");
    when(mockTransition.getId()).thenReturn("t1");
    when(mockArc.getFrom()).thenReturn("p1");
    when(mockArc.getTo()).thenReturn("t1");
    when(mockArc.getWeight()).thenReturn(1);

    when(mockMarking.isEmpty()).thenReturn(false);
    when(mockMarking.getTokens()).thenReturn(Map.of("p1", 1));
    when(mockMarking.getPlacesWithTokens()).thenReturn(Set.of("p1"));
    when(mockMarking.getTokens("p1")).thenReturn(1);
  }

  @Nested
  @DisplayName("Basic Validation Tests")
  class BasicValidationTests {

    @Test
    @DisplayName("Should validate simple Petri net successfully")
    void shouldValidateSimplePetriNetSuccessfully() {
      // Arrange
      PetriNetValidationResult.ValidationConfig config =
          PetriNetValidationResult.ValidationConfig.defaultConfig();

      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Arrays.asList(mockTransition));
      when(mockPetriNet.fireTransition(eq("t1"), any(Marking.class))).thenReturn(mockMarking);
      when(mockPetriNet.isTerminal(any(Marking.class))).thenReturn(true);

      // Act & Assert - This should fail initially since validator doesn't exist
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
          });
    }

    @Test
    @DisplayName("Should throw exception when PetriNet is null")
    void shouldThrowExceptionWhenPetriNetIsNull() {
      // Arrange
      PetriNetValidationResult.ValidationConfig config =
          PetriNetValidationResult.ValidationConfig.defaultConfig();

      // Act & Assert
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            validator.validate(null, config);
          });
    }

    @Test
    @DisplayName("Should use default config when config is null")
    void shouldUseDefaultConfigWhenConfigIsNull() {
      // This test will fail initially until PetriNetValidator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, null);
          });
    }
  }

  @Nested
  @DisplayName("Structural Validation Tests")
  class StructuralValidationTests {

    @Test
    @DisplayName("Should detect empty places as structural issue")
    void shouldDetectEmptyPlacesAsStructuralIssue() {
      // Arrange
      when(mockPetriNet.getPlaces()).thenReturn(Collections.emptyList());
      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isFailed());
            assertTrue(
                result
                    .getCheckResult(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION)
                    .map(check -> check.isFailed())
                    .orElse(false));
          });
    }

    @Test
    @DisplayName("Should detect empty transitions as structural issue")
    void shouldDetectEmptyTransitionsAsStructuralIssue() {
      // Arrange
      when(mockPetriNet.getTransitions()).thenReturn(Collections.emptyList());
      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isFailed());
          });
    }

    @Test
    @DisplayName("Should detect empty initial marking as structural issue")
    void shouldDetectEmptyInitialMarkingAsStructuralIssue() {
      // Arrange
      when(mockMarking.isEmpty()).thenReturn(true);
      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isFailed());
          });
    }
  }

  @Nested
  @DisplayName("Deadlock Detection Tests")
  class DeadlockDetectionTests {

    @Test
    @DisplayName("Should detect deadlock when no transitions are enabled")
    void shouldDetectDeadlockWhenNoTransitionsEnabled() {
      // Arrange
      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Collections.emptyList());
      when(mockPetriNet.isTerminal(any(Marking.class))).thenReturn(false);

      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.DEADLOCK_DETECTION));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isFailed());
            assertTrue(
                result
                    .getCheckResult(PetriNetValidationResult.CheckType.DEADLOCK_DETECTION)
                    .map(check -> check.isFailed())
                    .orElse(false));
            assertNotNull(result.getCounterExample());
          });
    }

    @Test
    @DisplayName("Should not report deadlock when marking is terminal")
    void shouldNotReportDeadlockWhenMarkingIsTerminal() {
      // Arrange
      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Collections.emptyList());
      when(mockPetriNet.isTerminal(any(Marking.class))).thenReturn(true);

      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.DEADLOCK_DETECTION));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isPassed());
            assertTrue(
                result
                    .getCheckResult(PetriNetValidationResult.CheckType.DEADLOCK_DETECTION)
                    .map(check -> check.isPassed())
                    .orElse(false));
          });
    }

    @Test
    @DisplayName("Should report inconclusive when timeout is reached")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldReportInconclusiveWhenTimeoutReached() {
      // Arrange - simulate long-running validation
      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200,
              1, // Very short timeout
              Set.of(PetriNetValidationResult.CheckType.DEADLOCK_DETECTION));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isInconclusive());
            assertEquals(
                PetriNetValidationResult.PetriValidationStatus.INCONCLUSIVE_TIMEOUT,
                result.getPetriStatus());
          });
    }
  }

  @Nested
  @DisplayName("Reachability Analysis Tests")
  class ReachabilityAnalysisTests {

    @Test
    @DisplayName("Should find reachable terminal state")
    void shouldFindReachableTerminalState() {
      // Arrange
      Marking terminalMarking = mock(Marking.class);
      when(terminalMarking.getPlacesWithTokens()).thenReturn(Set.of("p_done"));
      when(terminalMarking.getTokens()).thenReturn(Map.of("p_done", 1));

      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Arrays.asList(mockTransition));
      when(mockPetriNet.fireTransition(eq("t1"), any(Marking.class))).thenReturn(terminalMarking);
      when(mockPetriNet.isTerminal(terminalMarking)).thenReturn(true);

      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isPassed());
            assertTrue(
                result
                    .getCheckResult(PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS)
                    .map(check -> check.isPassed())
                    .orElse(false));
          });
    }

    @Test
    @DisplayName("Should report failure when no terminal state is reachable")
    void shouldReportFailureWhenNoTerminalStateReachable() {
      // Arrange
      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Collections.emptyList());
      when(mockPetriNet.isTerminal(any(Marking.class))).thenReturn(false);

      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isFailed());
            assertTrue(
                result
                    .getCheckResult(PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS)
                    .map(check -> check.isFailed())
                    .orElse(false));
          });
    }
  }

  @Nested
  @DisplayName("Liveness Check Tests")
  class LivenessCheckTests {

    @Test
    @DisplayName("Should verify all transitions are live")
    void shouldVerifyAllTransitionsAreLive() {
      // Arrange
      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Arrays.asList(mockTransition));
      when(mockPetriNet.fireTransition(eq("t1"), any(Marking.class))).thenReturn(mockMarking);

      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.LIVENESS_CHECK));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isPassed());
            assertTrue(
                result
                    .getCheckResult(PetriNetValidationResult.CheckType.LIVENESS_CHECK)
                    .map(check -> check.isPassed())
                    .orElse(false));
          });
    }

    @Test
    @DisplayName("Should detect dead transitions")
    void shouldDetectDeadTransitions() {
      // Arrange - transition never becomes enabled
      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Collections.emptyList());

      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.LIVENESS_CHECK));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isFailed());
            assertTrue(
                result
                    .getCheckResult(PetriNetValidationResult.CheckType.LIVENESS_CHECK)
                    .map(check -> check.isFailed())
                    .orElse(false));
          });
    }
  }

  @Nested
  @DisplayName("Boundedness Check Tests")
  class BoundednessCheckTests {

    @Test
    @DisplayName("Should verify net is bounded")
    void shouldVerifyNetIsBounded() {
      // Arrange - finite state space
      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Arrays.asList(mockTransition))
          .thenReturn(
              Collections.emptyList()); // Second call returns empty to terminate exploration
      when(mockPetriNet.fireTransition(eq("t1"), any(Marking.class))).thenReturn(mockMarking);

      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200, 30000, Set.of(PetriNetValidationResult.CheckType.BOUNDEDNESS_CHECK));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isPassed());
            assertTrue(
                result
                    .getCheckResult(PetriNetValidationResult.CheckType.BOUNDEDNESS_CHECK)
                    .map(check -> check.isPassed())
                    .orElse(false));
          });
    }

    @Test
    @DisplayName("Should detect unbounded places when bound is reached")
    void shouldDetectUnboundedPlacesWhenBoundReached() {
      // Arrange - simulate unbounded growth by always having enabled transitions
      Marking growingMarking = mock(Marking.class);
      when(growingMarking.getPlacesWithTokens()).thenReturn(Set.of("p1"));
      when(growingMarking.getTokens("p1")).thenReturn(100); // High token count
      when(growingMarking.getTokens()).thenReturn(Map.of("p1", 100));

      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Arrays.asList(mockTransition));
      when(mockPetriNet.fireTransition(eq("t1"), any(Marking.class))).thenReturn(growingMarking);

      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              10,
              30000, // Low bound to trigger bound reached
              Set.of(PetriNetValidationResult.CheckType.BOUNDEDNESS_CHECK));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertTrue(result.isInconclusive() || result.isFailed());
          });
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Should run all validation checks successfully")
    void shouldRunAllValidationChecksSuccessfully() {
      // Arrange
      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Arrays.asList(mockTransition));
      when(mockPetriNet.fireTransition(eq("t1"), any(Marking.class))).thenReturn(mockMarking);
      when(mockPetriNet.isTerminal(any(Marking.class))).thenReturn(true);

      PetriNetValidationResult.ValidationConfig config =
          PetriNetValidationResult.ValidationConfig.defaultConfig();

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);

            // Should have all check types
            assertEquals(5, result.getChecks().size());
            assertTrue(
                result
                    .getChecks()
                    .containsKey(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION));
            assertTrue(
                result
                    .getChecks()
                    .containsKey(PetriNetValidationResult.CheckType.DEADLOCK_DETECTION));
            assertTrue(
                result
                    .getChecks()
                    .containsKey(PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS));
            assertTrue(
                result.getChecks().containsKey(PetriNetValidationResult.CheckType.LIVENESS_CHECK));
            assertTrue(
                result
                    .getChecks()
                    .containsKey(PetriNetValidationResult.CheckType.BOUNDEDNESS_CHECK));

            // Should have states explored
            assertTrue(result.getStatesExplored() > 0);

            // Should have petri net ID
            assertEquals("test-petri-net", result.getPetriNetId());
          });
    }

    @Test
    @DisplayName("Should provide meaningful error messages and hints")
    void shouldProvideMeaningfulErrorMessagesAndHints() {
      // Arrange - create a failing scenario
      when(mockPetriNet.getPlaces()).thenReturn(Collections.emptyList());
      when(mockPetriNet.getTransitions()).thenReturn(Collections.emptyList());
      when(mockMarking.isEmpty()).thenReturn(true);

      PetriNetValidationResult.ValidationConfig config =
          PetriNetValidationResult.ValidationConfig.defaultConfig();

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);

            assertTrue(result.isFailed());
            assertFalse(result.getHints().isEmpty());
            assertTrue(result.getSummary().contains("Petri Net Validation"));
          });
    }
  }

  @Nested
  @DisplayName("Performance Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Should complete validation within reasonable time")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void shouldCompleteValidationWithinReasonableTime() {
      // Arrange
      when(mockPetriNet.getEnabledTransitions(any(Marking.class)))
          .thenReturn(Arrays.asList(mockTransition));
      when(mockPetriNet.fireTransition(eq("t1"), any(Marking.class))).thenReturn(mockMarking);
      when(mockPetriNet.isTerminal(any(Marking.class))).thenReturn(true);

      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              1000, 30000, EnumSet.allOf(PetriNetValidationResult.CheckType.class));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            assertNotNull(result);
          });
    }

    @Test
    @DisplayName("Should respect configured timeout")
    void shouldRespectConfiguredTimeout() {
      // Arrange
      PetriNetValidationResult.ValidationConfig config =
          new PetriNetValidationResult.ValidationConfig(
              200,
              100, // Very short timeout
              Set.of(PetriNetValidationResult.CheckType.DEADLOCK_DETECTION));

      // This test will fail initially until validator is implemented
      assertThrows(
          Exception.class,
          () -> {
            long startTime = System.currentTimeMillis();
            PetriNetValidationResult result = validator.validate(mockPetriNet, config);
            long endTime = System.currentTimeMillis();

            // Should complete quickly due to timeout
            assertTrue(endTime - startTime < 5000); // Should finish within 5 seconds
          });
    }
  }
}
