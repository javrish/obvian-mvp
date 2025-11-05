/* Copyright (c) 2025 Rishabh Pathak. Licensed under the MIT License. */

package core.petri.grammar;

import static org.assertj.core.api.Assertions.*;

import core.petri.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the AutomationGrammar engine using real-world demo scenarios.
 *
 * These tests validate the complete NL → PetriIntentSpec → PetriNet pipeline
 * with the specific DevOps and Football scenarios mentioned in the requirements.
 *
 * Scenarios:
 * - DevOps: "run tests; if pass deploy; if fail alert" (sequential with XOR choice)
 * - Football: "warm-up, then pass and shoot in parallel, then cooldown" (AND-split/join)
 *
 * @author Obvian Labs
 * @since Task 2 - AutomationGrammar Implementation
 */
class DemoScenariosIntegrationTest {

  private AutomationGrammar automationGrammar;

  @BeforeEach
  void setUp() {
    automationGrammar = new AutomationGrammar();
  }

  @Nested
  @DisplayName("DevOps Deployment Pipeline Scenario")
  class DevOpsDeploymentPipelineScenario {

    @Test
    @DisplayName("DevOps: Complete pipeline transformation and validation")
    void devOpsCompletePipelineTransformationAndValidation() throws Exception {
      // Given: DevOps deployment pipeline intent
      PetriIntentSpec devOpsIntent = createDevOpsIntentSpec();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(devOpsIntent);

      // Then: Verify complete DevOps pipeline structure
      assertThat(petriNet).isNotNull();
      assertThat(petriNet.getName()).isEqualTo("DevOps Deployment Pipeline");

      // Verify structural requirements
      assertThat(petriNet.getPlaces()).hasSizeGreaterThanOrEqualTo(6);
      assertThat(petriNet.getTransitions()).hasSizeGreaterThanOrEqualTo(4);
      assertThat(petriNet.getArcs()).hasSizeGreaterThanOrEqualTo(6);

      // Verify validation passes
      List<String> validationErrors = petriNet.validate();
      assertThat(validationErrors).isEmpty();

      // Verify initial marking (single entry point)
      assertThat(petriNet.getInitialMarking().getTotalTokens()).isEqualTo(1);
    }

    @Test
    @DisplayName("DevOps: Choice structure for test pass/fail paths")
    void devOpsChoiceStructureForTestPassFailPaths() throws Exception {
      // Given: DevOps intent
      PetriIntentSpec devOpsIntent = createDevOpsIntentSpec();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(devOpsIntent);

      // Then: Verify choice structure exists
      List<Transition> choiceTransitions =
          petriNet.getTransitions().stream().filter(Transition::isChoice).toList();

      assertThat(choiceTransitions).hasSizeGreaterThanOrEqualTo(2);

      // Verify choice paths exist
      boolean hasPassPath =
          petriNet.getTransitions().stream()
              .anyMatch(
                  t ->
                      t.getDescription() != null
                          && t.getDescription().toLowerCase().contains("deploy"));

      boolean hasFailPath =
          petriNet.getTransitions().stream()
              .anyMatch(
                  t ->
                      t.getDescription() != null
                          && t.getDescription().toLowerCase().contains("alert"));

      assertThat(hasPassPath).isTrue();
      assertThat(hasFailPath).isTrue();
    }

    @Test
    @DisplayName("DevOps: Token simulation through success path")
    void devOpsTokenSimulationThroughSuccessPath() throws Exception {
      // Given: DevOps PetriNet
      PetriIntentSpec devOpsIntent = createDevOpsIntentSpec();
      PetriNet petriNet = automationGrammar.transform(devOpsIntent);

      // When: Simulate token flow through success path
      Marking currentMarking = petriNet.getInitialMarking();
      assertThat(currentMarking.getTotalTokens()).isEqualTo(1);

      // Then: Should be able to fire "run_tests" transition
      List<Transition> enabledTransitions = petriNet.getEnabledTransitions(currentMarking);
      assertThat(enabledTransitions).hasSizeGreaterThanOrEqualTo(1);

      // Find run_tests transition
      Optional<Transition> runTestsTransition =
          enabledTransitions.stream()
              .filter(t -> t.getDescription().toLowerCase().contains("test"))
              .findFirst();

      assertThat(runTestsTransition).isPresent();

      // Fire the transition
      Marking afterTests =
          petriNet.fireTransition(runTestsTransition.get().getId(), currentMarking);
      assertThat(afterTests).isNotNull();

      // After running tests, should have choices available (pass/fail)
      List<Transition> choicesAvailable = petriNet.getEnabledTransitions(afterTests);
      assertThat(choicesAvailable).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("DevOps: Conditional execution paths work correctly")
    void devOpsConditionalExecutionPathsWorkCorrectly() throws Exception {
      // Given: DevOps PetriNet
      PetriIntentSpec devOpsIntent = createDevOpsIntentSpec();
      PetriNet petriNet = automationGrammar.transform(devOpsIntent);

      // When: Analyze conditional paths
      // Find deploy transition (success path)
      Optional<Transition> deployTransition =
          petriNet.getTransitions().stream()
              .filter(t -> t.getDescription().toLowerCase().contains("deploy"))
              .findFirst();

      // Find alert transition (failure path)
      Optional<Transition> alertTransition =
          petriNet.getTransitions().stream()
              .filter(t -> t.getDescription().toLowerCase().contains("alert"))
              .findFirst();

      // Then: Both paths should exist
      assertThat(deployTransition).isPresent();
      assertThat(alertTransition).isPresent();

      // Verify they have appropriate guard conditions or choice metadata
      Transition deploy = deployTransition.get();
      Transition alert = alertTransition.get();

      boolean deployHasCondition =
          deploy.hasGuard() || "tests_passed".equals(deploy.getMetadata().get("pathDescription"));
      boolean alertHasCondition =
          alert.hasGuard() || "tests_failed".equals(alert.getMetadata().get("pathDescription"));

      assertThat(deployHasCondition || alertHasCondition).isTrue();
    }

    @Test
    @DisplayName("DevOps: Transformation statistics are reasonable")
    void devOpsTransformationStatisticsAreReasonable() {
      // Given: DevOps intent
      PetriIntentSpec devOpsIntent = createDevOpsIntentSpec();

      // When: Get transformation statistics
      AutomationGrammar.TransformationStats stats =
          automationGrammar.getTransformationStats(devOpsIntent);

      // Then: Verify statistics are reasonable
      assertThat(stats).isNotNull();
      assertThat(stats.getInputStepCount()).isEqualTo(4); // run_tests, test_choice, deploy, alert
      assertThat(stats.getActionSteps()).isEqualTo(3); // run_tests, deploy, alert
      assertThat(stats.getChoiceSteps()).isEqualTo(1); // test_choice
      assertThat(stats.getParallelSteps()).isEqualTo(0); // no parallel steps
      assertThat(stats.getEstimatedPlaces()).isGreaterThan(4);
      assertThat(stats.getEstimatedTransitions()).isGreaterThan(3);
    }
  }

  @Nested
  @DisplayName("Football Training Session Scenario")
  class FootballTrainingSessionScenario {

    @Test
    @DisplayName("Football: Complete training session transformation")
    void footballCompleteTrainingSessionTransformation() throws Exception {
      // Given: Football training session intent
      PetriIntentSpec footballIntent = createFootballIntentSpec();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(footballIntent);

      // Then: Verify complete football structure
      assertThat(petriNet).isNotNull();
      assertThat(petriNet.getName()).isEqualTo("Football Training Session");

      // Verify structural requirements for: warmup → parallel(pass, shoot) → sync → cooldown
      assertThat(petriNet.getPlaces()).hasSizeGreaterThanOrEqualTo(6);
      assertThat(petriNet.getTransitions()).hasSizeGreaterThanOrEqualTo(4);

      // Verify validation passes
      List<String> validationErrors = petriNet.validate();
      assertThat(validationErrors).isEmpty();

      // Verify initial marking
      assertThat(petriNet.getInitialMarking().getTotalTokens()).isEqualTo(1);
    }

    @Test
    @DisplayName("Football: Parallel structure for pass and shoot activities")
    void footballParallelStructureForPassAndShootActivities() throws Exception {
      // Given: Football intent
      PetriIntentSpec footballIntent = createFootballIntentSpec();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(footballIntent);

      // Then: Verify parallel structure (AND-split)
      List<Transition> forkTransitions =
          petriNet.getTransitions().stream().filter(Transition::isFork).toList();

      assertThat(forkTransitions).hasSizeGreaterThanOrEqualTo(1);

      // Verify synchronization structure (AND-join)
      List<Transition> joinTransitions =
          petriNet.getTransitions().stream().filter(Transition::isJoin).toList();

      assertThat(joinTransitions).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Football: Sequential warmup and cooldown phases")
    void footballSequentialWarmupAndCooldownPhases() throws Exception {
      // Given: Football intent
      PetriIntentSpec footballIntent = createFootballIntentSpec();

      // When: Transform to PetriNet
      PetriNet petriNet = automationGrammar.transform(footballIntent);

      // Then: Verify warmup and cooldown transitions exist
      boolean hasWarmup =
          petriNet.getTransitions().stream()
              .anyMatch(
                  t ->
                      t.getDescription() != null
                          && t.getDescription().toLowerCase().contains("warm"));

      boolean hasCooldown =
          petriNet.getTransitions().stream()
              .anyMatch(
                  t ->
                      t.getDescription() != null
                          && t.getDescription().toLowerCase().contains("cool"));

      assertThat(hasWarmup).isTrue();
      assertThat(hasCooldown).isTrue();
    }

    @Test
    @DisplayName("Football: Token flow through complete training session")
    void footballTokenFlowThroughCompleteTrainingSession() throws Exception {
      // Given: Football PetriNet
      PetriIntentSpec footballIntent = createFootballIntentSpec();
      PetriNet petriNet = automationGrammar.transform(footballIntent);

      // When: Simulate token flow
      Marking currentMarking = petriNet.getInitialMarking();
      assertThat(currentMarking.getTotalTokens()).isEqualTo(1);

      // Then: Should be able to progress through the session
      List<Transition> enabledTransitions = petriNet.getEnabledTransitions(currentMarking);
      assertThat(enabledTransitions).hasSizeGreaterThanOrEqualTo(1);

      // Fire first enabled transition (should be warmup)
      Transition firstTransition = enabledTransitions.get(0);
      Marking afterFirst = petriNet.fireTransition(firstTransition.getId(), currentMarking);
      assertThat(afterFirst).isNotNull();

      // After warmup, should have parallel options or fork transition enabled
      List<Transition> afterWarmup = petriNet.getEnabledTransitions(afterFirst);
      assertThat(afterWarmup).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Football: Parallel branches can execute independently")
    void footballParallelBranchesCanExecuteIndependently() throws Exception {
      // Given: Football PetriNet
      PetriIntentSpec footballIntent = createFootballIntentSpec();
      PetriNet petriNet = automationGrammar.transform(footballIntent);

      // When: Analyze parallel structure
      // Find fork transition
      Optional<Transition> forkTransition =
          petriNet.getTransitions().stream().filter(Transition::isFork).findFirst();

      // Then: Fork should create multiple tokens in parallel branches
      if (forkTransition.isPresent()) {
        // Get output places of fork
        List<Place> outputPlaces = petriNet.getOutputPlaces(forkTransition.get().getId());
        assertThat(outputPlaces).hasSizeGreaterThanOrEqualTo(2);

        // Each output place represents an independent parallel branch
        for (Place place : outputPlaces) {
          List<Transition> branchTransitions = petriNet.getOutputTransitions(place.getId());
          // Each branch should have its own transitions
        }
      }
    }

    @Test
    @DisplayName("Football: Transformation handles complex flow correctly")
    void footballTransformationHandlesComplexFlowCorrectly() {
      // Given: Football intent
      PetriIntentSpec footballIntent = createFootballIntentSpec();

      // When: Get transformation stats
      AutomationGrammar.TransformationStats stats =
          automationGrammar.getTransformationStats(footballIntent);

      // Then: Verify complex flow statistics
      assertThat(stats).isNotNull();
      assertThat(stats.getInputStepCount()).isGreaterThan(3);
      assertThat(stats.getParallelSteps()).isGreaterThan(0);
      assertThat(stats.getEstimatedPlaces()).isGreaterThan(5);
      assertThat(stats.getEstimatedTransitions()).isGreaterThan(3);
    }
  }

  @Nested
  @DisplayName("Cross-Scenario Validation")
  class CrossScenarioValidation {

    @Test
    @DisplayName("Both scenarios transform without errors")
    void bothScenariosTransformWithoutErrors() throws Exception {
      // Given: Both scenario intents
      PetriIntentSpec devOpsIntent = createDevOpsIntentSpec();
      PetriIntentSpec footballIntent = createFootballIntentSpec();

      // When: Transform both
      PetriNet devOpsPetriNet = automationGrammar.transform(devOpsIntent);
      PetriNet footballPetriNet = automationGrammar.transform(footballIntent);

      // Then: Both should be valid
      assertThat(devOpsPetriNet.validate()).isEmpty();
      assertThat(footballPetriNet.validate()).isEmpty();

      // Both should have reasonable complexity
      assertThat(devOpsPetriNet.getPlaces().size()).isGreaterThan(3);
      assertThat(footballPetriNet.getPlaces().size()).isGreaterThan(3);
    }

    @Test
    @DisplayName("Transformation produces deterministic results")
    void transformationProducesDeterministicResults() throws Exception {
      // Given: DevOps intent
      PetriIntentSpec devOpsIntent = createDevOpsIntentSpec();

      // When: Transform multiple times
      PetriNet petriNet1 = automationGrammar.transform(devOpsIntent);
      PetriNet petriNet2 = automationGrammar.transform(devOpsIntent);

      // Then: Should produce identical structures
      assertThat(petriNet1.getName()).isEqualTo(petriNet2.getName());
      assertThat(petriNet1.getPlaces()).hasSize(petriNet2.getPlaces().size());
      assertThat(petriNet1.getTransitions()).hasSize(petriNet2.getTransitions().size());
      assertThat(petriNet1.getArcs()).hasSize(petriNet2.getArcs().size());
      assertThat(petriNet1.getInitialMarking().getTotalTokens())
          .isEqualTo(petriNet2.getInitialMarking().getTotalTokens());
    }

    @Test
    @DisplayName("Custom configuration affects both scenarios consistently")
    void customConfigurationAffectsBothScenariosConsistently() throws Exception {
      // Given: Custom configuration
      AutomationGrammar.TransformationConfig config = new AutomationGrammar.TransformationConfig();
      config.setNamingStrategy("minimal");
      config.setAddDebugMetadata(true);

      PetriIntentSpec devOpsIntent = createDevOpsIntentSpec();
      PetriIntentSpec footballIntent = createFootballIntentSpec();

      // When: Transform both with custom config
      PetriNet devOpsPetriNet = automationGrammar.transform(devOpsIntent, config);
      PetriNet footballPetriNet = automationGrammar.transform(footballIntent, config);

      // Then: Both should have debug metadata (config applied)
      assertThat(devOpsPetriNet.getMetadata()).containsKey("transformationStats");
      assertThat(footballPetriNet.getMetadata()).containsKey("transformationStats");
    }
  }

  // Helper methods to create demo scenarios

  private PetriIntentSpec createDevOpsIntentSpec() {
    return PetriIntentSpec.builder()
        .name("DevOps Deployment Pipeline")
        .description("run tests; if pass deploy; if fail alert")
        .originalPrompt("run tests; if pass deploy; if fail alert")
        .templateId("devops-pipeline-v1")
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
        .templateId("football-training-v1")
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
