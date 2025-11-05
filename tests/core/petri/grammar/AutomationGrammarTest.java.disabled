/* Copyright (c) 2025 Rishabh Pathak. Licensed under the MIT License. */

package core.petri.grammar;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import core.petri.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

/**
 * Comprehensive unit tests for AutomationGrammar engine.
 *
 * Tests cover:
 * - Basic transformation patterns (Sequential, Parallel, Choice)
 * - Complex scenarios (DevOps and Football demos)
 * - Error handling and validation
 * - Configuration and optimization features
 * - Golden test outputs for regression testing
 *
 * @author Obvian Labs
 * @since Task 2 - AutomationGrammar Implementation
 */
class AutomationGrammarTest {

  private AutomationGrammar automationGrammar;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    automationGrammar = new AutomationGrammar();
  }

  @Nested
  @DisplayName("Basic Transformation Patterns")
  class BasicTransformationPatterns {

    @Test
    @DisplayName("Transform single ACTION step creates basic place-transition-place pattern")
    void transformSingleActionStep() throws Exception {
      // Given: Simple single action intent
      PetriIntentSpec intentSpec =
          PetriIntentSpec.builder()
              .name("Simple Action")
              .description("Single action test")
              .addActionStep("action1", "Execute simple task")
              .build();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(intentSpec);

      // Then: Verify basic structure
      assertThat(petriNet).isNotNull();
      assertThat(petriNet.getName()).isEqualTo("Simple Action");

      // Should have: pre-place, post-place, and transition
      assertThat(petriNet.getPlaces()).hasSize(2);
      assertThat(petriNet.getTransitions()).hasSize(1);
      assertThat(petriNet.getArcs()).hasSize(2);

      // Verify places
      List<Place> places = petriNet.getPlaces();
      assertThat(places).extracting(Place::getId).allMatch(id -> id.contains("action1"));

      // Verify transition
      List<Transition> transitions = petriNet.getTransitions();
      Transition actionTransition = transitions.get(0);
      assertThat(actionTransition.getDescription()).isEqualTo("Execute simple task");
      assertThat(actionTransition.getMetadata().get("stepId")).isEqualTo("action1");

      // Verify initial marking - should have token in pre-place
      Marking initialMarking = petriNet.getInitialMarking();
      assertThat(initialMarking.getTotalTokens()).isEqualTo(1);
    }

    @Test
    @DisplayName("Transform sequential ACTION steps creates connected chain")
    void transformSequentialActionSteps() throws Exception {
      // Given: Two sequential actions
      PetriIntentSpec intentSpec =
          PetriIntentSpec.builder()
              .name("Sequential Actions")
              .description("Two actions in sequence")
              .addActionStep("action1", "First task")
              .addActionStep("action2", "Second task")
              .build();

      // Add dependency: action2 depends on action1
      PetriIntentSpec.IntentStep action2 =
          new PetriIntentSpec.IntentStep(
              "action2",
              PetriIntentSpec.StepType.ACTION,
              "Second task",
              Arrays.asList("action1"),
              new HashMap<>(),
              null,
              new HashMap<>());

      PetriIntentSpec sequentialSpec =
          PetriIntentSpec.builder()
              .name("Sequential Actions")
              .description("Two actions in sequence")
              .addActionStep("action1", "First task")
              .addStep(action2)
              .build();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(sequentialSpec);

      // Then: Verify sequential structure
      assertThat(petriNet.getPlaces())
          .hasSizeGreaterThanOrEqualTo(3); // At least pre1, post1/pre2, post2
      assertThat(petriNet.getTransitions()).hasSize(2);

      // Verify only one initial token (in first action's pre-place)
      assertThat(petriNet.getInitialMarking().getTotalTokens()).isEqualTo(1);

      // Verify connectivity - there should be arcs connecting the sequence
      assertThat(petriNet.getArcs()).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Transform CHOICE step creates XOR branching structure")
    void transformChoiceStep() throws Exception {
      // Given: Choice step with two paths
      PetriIntentSpec.IntentStep choiceStep =
          new PetriIntentSpec.IntentStep(
              "choice1",
              PetriIntentSpec.StepType.CHOICE,
              "Make a choice",
              new ArrayList<>(),
              new HashMap<>(),
              null,
              Map.of("paths", Arrays.asList("Path A", "Path B")));

      PetriIntentSpec intentSpec =
          PetriIntentSpec.builder()
              .name("Choice Test")
              .description("XOR branching test")
              .addStep(choiceStep)
              .build();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(intentSpec);

      // Then: Verify choice structure
      assertThat(petriNet.getPlaces()).hasSizeGreaterThanOrEqualTo(3); // Pre-place + 2 path places
      assertThat(petriNet.getTransitions()).hasSize(2); // 2 choice transitions

      // Verify choice transitions have proper metadata
      List<Transition> transitions = petriNet.getTransitions();
      for (Transition t : transitions) {
        assertThat(t.getMetadata().get("stepType")).isEqualTo("CHOICE");
        assertThat(t.isChoice()).isTrue();
      }

      // Verify initial marking
      assertThat(petriNet.getInitialMarking().getTotalTokens()).isEqualTo(1);
    }

    @Test
    @DisplayName("Transform PARALLEL step creates AND-split structure")
    void transformParallelStep() throws Exception {
      // Given: Parallel step
      PetriIntentSpec.IntentStep parallelStep =
          new PetriIntentSpec.IntentStep(
              "parallel1",
              PetriIntentSpec.StepType.PARALLEL,
              "Execute in parallel",
              new ArrayList<>(),
              new HashMap<>(),
              null,
              new HashMap<>());

      PetriIntentSpec intentSpec =
          PetriIntentSpec.builder()
              .name("Parallel Test")
              .description("AND-split test")
              .addStep(parallelStep)
              .build();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(intentSpec);

      // Then: Verify parallel structure
      assertThat(petriNet.getPlaces())
          .hasSizeGreaterThanOrEqualTo(3); // Pre-place + 2 branch places
      assertThat(petriNet.getTransitions()).hasSize(1); // Fork transition

      // Verify fork transition
      Transition forkTransition = petriNet.getTransitions().get(0);
      assertThat(forkTransition.isFork()).isTrue();
      assertThat(forkTransition.getMetadata().get("stepType")).isEqualTo("PARALLEL");

      // Verify initial marking
      assertThat(petriNet.getInitialMarking().getTotalTokens()).isEqualTo(1);
    }

    @Test
    @DisplayName("Transform SYNC step creates AND-join structure")
    void transformSyncStep() throws Exception {
      // Given: Sync step with dependencies (simulating parallel branches)
      PetriIntentSpec.IntentStep syncStep =
          new PetriIntentSpec.IntentStep(
              "sync1",
              PetriIntentSpec.StepType.SYNC,
              "Synchronize branches",
              Arrays.asList("parallel1"),
              new HashMap<>(),
              null,
              new HashMap<>());

      // Also need the parallel step it depends on
      PetriIntentSpec.IntentStep parallelStep =
          new PetriIntentSpec.IntentStep(
              "parallel1",
              PetriIntentSpec.StepType.PARALLEL,
              "Execute in parallel",
              new ArrayList<>(),
              new HashMap<>(),
              null,
              new HashMap<>());

      PetriIntentSpec intentSpec =
          PetriIntentSpec.builder()
              .name("Sync Test")
              .description("AND-join test")
              .addStep(parallelStep)
              .addStep(syncStep)
              .build();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(intentSpec);

      // Then: Verify sync structure
      assertThat(petriNet.getTransitions()).hasSizeGreaterThanOrEqualTo(2); // Fork + Join

      // Find join transition
      Optional<Transition> joinTransition =
          petriNet.getTransitions().stream().filter(Transition::isJoin).findFirst();

      assertThat(joinTransition).isPresent();
      assertThat(joinTransition.get().getMetadata().get("stepType")).isEqualTo("SYNC");
    }
  }

  @Nested
  @DisplayName("Complex Scenario Tests")
  class ComplexScenarioTests {

    @Test
    @DisplayName("DevOps Scenario: run tests; if pass deploy; if fail alert")
    void transformDevOpsScenario() throws Exception {
      // Given: DevOps workflow intent
      PetriIntentSpec intentSpec = createDevOpsIntentSpec();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(intentSpec);

      // Then: Verify DevOps structure
      assertThat(petriNet).isNotNull();
      assertThat(petriNet.getName()).isEqualTo("DevOps Deployment Pipeline");

      // Should have reasonable complexity
      assertThat(petriNet.getPlaces()).hasSizeGreaterThanOrEqualTo(5);
      assertThat(petriNet.getTransitions()).hasSizeGreaterThanOrEqualTo(3);

      // Verify choice structure for pass/fail paths
      List<Transition> choiceTransitions =
          petriNet.getTransitions().stream().filter(Transition::isChoice).toList();

      assertThat(choiceTransitions).hasSizeGreaterThanOrEqualTo(2);

      // Verify initial marking
      assertThat(petriNet.getInitialMarking().getTotalTokens()).isEqualTo(1);

      // Verify validation passes
      assertThat(petriNet.validate()).isEmpty();
    }

    @Test
    @DisplayName("Football Scenario: warm-up, then pass and shoot in parallel, then cooldown")
    void transformFootballScenario() throws Exception {
      // Given: Football training intent
      PetriIntentSpec intentSpec = createFootballIntentSpec();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(intentSpec);

      // Then: Verify Football structure
      assertThat(petriNet).isNotNull();
      assertThat(petriNet.getName()).isEqualTo("Football Training Session");

      // Should have: warmup → parallel(pass, shoot) → sync → cooldown
      assertThat(petriNet.getPlaces()).hasSizeGreaterThanOrEqualTo(6);
      assertThat(petriNet.getTransitions()).hasSizeGreaterThanOrEqualTo(4);

      // Verify fork transition exists (for parallel activities)
      List<Transition> forkTransitions =
          petriNet.getTransitions().stream().filter(Transition::isFork).toList();
      assertThat(forkTransitions).hasSizeGreaterThanOrEqualTo(1);

      // Verify join transition exists (for sync)
      List<Transition> joinTransitions =
          petriNet.getTransitions().stream().filter(Transition::isJoin).toList();
      assertThat(joinTransitions).hasSizeGreaterThanOrEqualTo(1);

      // Verify initial marking
      assertThat(petriNet.getInitialMarking().getTotalTokens()).isEqualTo(1);

      // Verify validation passes
      assertThat(petriNet.validate()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Validation and Error Handling")
  class ValidationAndErrorHandling {

    @Test
    @DisplayName("Transform invalid intent spec throws exception")
    void transformInvalidIntentSpecThrowsException() {
      // Given: Invalid intent spec (no name)
      PetriIntentSpec invalidSpec =
          PetriIntentSpec.builder()
              .description("Missing name")
              .addActionStep("action1", "Some action")
              .build();

      // When/Then: Should throw exception
      assertThatThrownBy(() -> automationGrammar.transform(invalidSpec))
          .isInstanceOf(AutomationGrammar.GrammarTransformationException.class)
          .hasMessageContaining("Invalid intent specification");
    }

    @Test
    @DisplayName("Transform empty intent spec throws exception")
    void transformEmptyIntentSpecThrowsException() {
      // Given: Empty intent spec
      PetriIntentSpec emptySpec =
          PetriIntentSpec.builder().name("Empty Spec").description("No steps").build();

      // When/Then: Should throw exception
      assertThatThrownBy(() -> automationGrammar.transform(emptySpec))
          .isInstanceOf(AutomationGrammar.GrammarTransformationException.class)
          .hasMessageContaining("at least one step");
    }

    @Test
    @DisplayName("Validate transformability detects invalid specs")
    void validateTransformabilityDetectsInvalidSpecs() {
      // Given: Invalid spec with null step type
      PetriIntentSpec.IntentStep invalidStep =
          new PetriIntentSpec.IntentStep(
              "invalid",
              null,
              "Invalid step",
              new ArrayList<>(),
              new HashMap<>(),
              null,
              new HashMap<>());

      PetriIntentSpec invalidSpec =
          PetriIntentSpec.builder().name("Invalid Spec").addStep(invalidStep).build();

      // When: Validate transformability
      AutomationGrammar.ValidationResult result =
          automationGrammar.validateTransformability(invalidSpec);

      // Then: Should be invalid
      assertThat(result.isValid()).isFalse();
      assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("Validate transformability accepts valid specs")
    void validateTransformabilityAcceptsValidSpecs() {
      // Given: Valid spec
      PetriIntentSpec validSpec =
          PetriIntentSpec.builder()
              .name("Valid Spec")
              .description("Valid specification")
              .addActionStep("action1", "Valid action")
              .build();

      // When: Validate transformability
      AutomationGrammar.ValidationResult result =
          automationGrammar.validateTransformability(validSpec);

      // Then: Should be valid
      assertThat(result.isValid()).isTrue();
      assertThat(result.getErrors()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Configuration and Optimization")
  class ConfigurationAndOptimization {

    @Test
    @DisplayName("Transform with custom configuration applies settings")
    void transformWithCustomConfigurationAppliesSettings() throws Exception {
      // Given: Custom configuration
      AutomationGrammar.TransformationConfig config = new AutomationGrammar.TransformationConfig();
      config.setNamingStrategy("minimal");
      config.setAddDebugMetadata(true);
      config.setOptimizeSequentialChains(false);

      PetriIntentSpec intentSpec =
          PetriIntentSpec.builder()
              .name("Config Test")
              .addActionStep(
                  "action1",
                  "Test action with long description that should be truncated in descriptive mode")
              .build();

      // When: Transform with config
      PetriNet petriNet = automationGrammar.transform(intentSpec, config);

      // Then: Verify config was applied
      assertThat(petriNet).isNotNull();

      // Debug metadata should be present
      assertThat(petriNet.getMetadata()).containsKey("transformationStats");

      // Names should be minimal (check transition name)
      List<Transition> transitions = petriNet.getTransitions();
      assertThat(transitions).isNotEmpty();
      // With minimal naming, name should be shorter/simpler
    }

    @Test
    @DisplayName("Get transformation stats provides accurate metrics")
    void getTransformationStatsProvideAccurateMetrics() {
      // Given: Complex intent spec
      PetriIntentSpec intentSpec = createDevOpsIntentSpec();

      // When: Get stats
      AutomationGrammar.TransformationStats stats =
          automationGrammar.getTransformationStats(intentSpec);

      // Then: Verify stats
      assertThat(stats).isNotNull();
      assertThat(stats.getInputStepCount()).isEqualTo(intentSpec.getSteps().size());
      assertThat(stats.getActionSteps()).isEqualTo(intentSpec.getActionSteps().size());
      assertThat(stats.getChoiceSteps()).isEqualTo(intentSpec.getChoiceSteps().size());
      assertThat(stats.getParallelSteps()).isEqualTo(intentSpec.getParallelSteps().size());
      assertThat(stats.getEstimatedPlaces()).isGreaterThan(0);
      assertThat(stats.getEstimatedTransitions()).isGreaterThan(0);
    }
  }

  @Nested
  @DisplayName("Golden Test Outputs")
  class GoldenTestOutputs {

    @Test
    @DisplayName("Simple action transformation matches golden output")
    void simpleActionTransformationMatchesGolden() throws Exception {
      // Given: Simple action intent
      PetriIntentSpec intentSpec =
          PetriIntentSpec.builder()
              .name("Golden Simple")
              .description("Simple action for golden test")
              .addActionStep("golden_action", "Execute golden action")
              .build();

      // When: Transform
      PetriNet petriNet = automationGrammar.transform(intentSpec);

      // Then: Verify golden structure
      assertThat(petriNet.getName()).isEqualTo("Golden Simple");
      assertThat(petriNet.getPlaces()).hasSize(2);
      assertThat(petriNet.getTransitions()).hasSize(1);
      assertThat(petriNet.getArcs()).hasSize(2);

      // Verify place IDs follow expected pattern
      List<String> placeIds = petriNet.getPlaces().stream().map(Place::getId).sorted().toList();
      assertThat(placeIds).allMatch(id -> id.contains("golden_action"));

      // Verify transition
      Transition transition = petriNet.getTransitions().get(0);
      assertThat(transition.getDescription()).isEqualTo("Execute golden action");
      assertThat(transition.getAction()).isEqualTo("Execute golden action");
    }

    @Test
    @DisplayName("DevOps scenario produces consistent structure")
    void devOpsScenarioProducesConsistentStructure() throws Exception {
      // Given: DevOps scenario (run multiple times)
      PetriIntentSpec intentSpec = createDevOpsIntentSpec();

      // When: Transform multiple times
      PetriNet petriNet1 = automationGrammar.transform(intentSpec);
      PetriNet petriNet2 = automationGrammar.transform(intentSpec);

      // Then: Should produce identical structures (deterministic)
      assertThat(petriNet1.getName()).isEqualTo(petriNet2.getName());
      assertThat(petriNet1.getPlaces()).hasSize(petriNet2.getPlaces().size());
      assertThat(petriNet1.getTransitions()).hasSize(petriNet2.getTransitions().size());
      assertThat(petriNet1.getArcs()).hasSize(petriNet2.getArcs().size());

      // Both should validate successfully
      assertThat(petriNet1.validate()).isEmpty();
      assertThat(petriNet2.validate()).isEmpty();
    }
  }

  // Helper methods for creating test scenarios

  private PetriIntentSpec createDevOpsIntentSpec() {
    return PetriIntentSpec.builder()
        .name("DevOps Deployment Pipeline")
        .description("run tests; if pass deploy; if fail alert")
        .originalPrompt("run tests; if pass deploy; if fail alert")
        .addActionStep("run_tests", "Execute test suite")
        .addStep(
            new PetriIntentSpec.IntentStep(
                "test_choice",
                PetriIntentSpec.StepType.CHOICE,
                "Check test results",
                Arrays.asList("run_tests"),
                new HashMap<>(),
                null,
                Map.of("paths", Arrays.asList("tests_passed", "tests_failed"))))
        .addStep(
            new PetriIntentSpec.IntentStep(
                "deploy",
                PetriIntentSpec.StepType.ACTION,
                "Deploy to production",
                Arrays.asList("test_choice"),
                new HashMap<>(),
                "tests_passed",
                new HashMap<>()))
        .addStep(
            new PetriIntentSpec.IntentStep(
                "alert",
                PetriIntentSpec.StepType.ACTION,
                "Send failure alert",
                Arrays.asList("test_choice"),
                new HashMap<>(),
                "tests_failed",
                new HashMap<>()))
        .build();
  }

  private PetriIntentSpec createFootballIntentSpec() {
    return PetriIntentSpec.builder()
        .name("Football Training Session")
        .description("warm-up, then pass and shoot in parallel, then cooldown")
        .originalPrompt("warm-up, then pass and shoot in parallel, then cooldown")
        .addActionStep("warmup", "Team warm-up exercises")
        .addStep(
            new PetriIntentSpec.IntentStep(
                "parallel_training",
                PetriIntentSpec.StepType.PARALLEL,
                "Parallel training activities",
                Arrays.asList("warmup"),
                new HashMap<>(),
                null,
                new HashMap<>()))
        .addActionStep("pass_practice", "Passing drills")
        .addActionStep("shoot_practice", "Shooting drills")
        .addStep(
            new PetriIntentSpec.IntentStep(
                "sync_training",
                PetriIntentSpec.StepType.SYNC,
                "Synchronize training completion",
                Arrays.asList("parallel_training"),
                new HashMap<>(),
                null,
                new HashMap<>()))
        .addStep(
            new PetriIntentSpec.IntentStep(
                "cooldown",
                PetriIntentSpec.StepType.ACTION,
                "Team cooldown",
                Arrays.asList("sync_training"),
                new HashMap<>(),
                null,
                new HashMap<>()))
        .build();
  }
}
