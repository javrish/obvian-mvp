/* Copyright (c) 2025 Rishabh Pathak. Licensed under the MIT License. */

package core.petri.projection;

import static org.assertj.core.api.Assertions.*;

import core.DAG;
import core.TaskNode;
import core.petri.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for PetriToDagProjector
 * Tests the core P3Net → DAG lowering functionality with various scenarios
 */
@DisplayName("PetriToDagProjector")
class PetriToDagProjectorTest {

  private PetriToDagProjector projector;

  @BeforeEach
  void setUp() {
    projector = new PetriToDagProjector();
  }

  @Nested
  @DisplayName("Basic Projection Tests")
  class BasicProjectionTests {

    @Test
    @DisplayName("Should reject null PetriNet")
    void shouldRejectNullPetriNet() {
      assertThatThrownBy(() -> projector.projectToDAG(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("PetriNet cannot be null");
    }

    @Test
    @DisplayName("Should reject invalid PetriNet")
    void shouldRejectInvalidPetriNet() {
      // Create invalid Petri net (no transitions)
      PetriNet invalidNet =
          PetriNet.builder()
              .name("Invalid")
              .addPlace(new Place("p1", "Start", null, null, new HashMap<>()))
              .build();

      assertThatThrownBy(() -> projector.projectToDAG(invalidNet))
          .isInstanceOf(PetriToDagProjector.ProjectionException.class)
          .hasMessageContaining("Cannot project invalid Petri net");
    }

    @Test
    @DisplayName("Should project simple linear workflow")
    void shouldProjectSimpleLinearWorkflow() {
      // Create simple linear Petri net: t1 -> p1 -> t2
      PetriNet petriNet =
          PetriNet.builder()
              .name("Linear Workflow")
              .addPlace(new Place("p1", "Intermediate", null, null, new HashMap<>()))
              .addTransition(
                  new Transition("t1", "Start Task", null, "start", null, new HashMap<>()))
              .addTransition(new Transition("t2", "End Task", null, "end", null, new HashMap<>()))
              .addArc("t1", "p1", 1)
              .addArc("p1", "t2", 1)
              .build();

      DAG dag = projector.projectToDAG(petriNet);

      assertThat(dag).isNotNull();
      assertThat(dag.getDerivedFromPetriNetId()).isEqualTo(petriNet.getId());
      assertThat(dag.getName()).isEqualTo("Linear Workflow");
      assertThat(dag.getNodes()).hasSize(2);

      // Check TaskNode mapping
      TaskNode t1Node = dag.getNode("t1");
      TaskNode t2Node = dag.getNode("t2");

      assertThat(t1Node).isNotNull();
      assertThat(t1Node.getAction()).isEqualTo("start");
      assertThat(t2Node).isNotNull();
      assertThat(t2Node.getAction()).isEqualTo("end");

      // Check dependency: t2 depends on t1
      assertThat(t2Node.getDependencyIds()).containsExactly("t1");
      assertThat(t1Node.getDependencyIds()).isEmpty();

      // Check cross-highlighting metadata
      assertThat(t1Node.getMetadata()).containsKeys("petriTransitionId", "petriTransitionName");
      assertThat(t2Node.getMetadata()).containsKey("incomingEdges");
    }

    @Test
    @DisplayName("Should handle places with multiple producers or consumers")
    void shouldHandlePlacesWithMultipleProducersOrConsumers() {
      // Create Petri net with fork: t1 -> p1 -> {t2, t3}
      // Only the p1 -> t2 connection should be projected (single consumer rule)
      PetriNet petriNet =
          PetriNet.builder()
              .name("Fork Workflow")
              .addPlace(new Place("p1", "Fork Place", null, null, new HashMap<>()))
              .addTransition(new Transition("t1", "Source", null, "source", null, new HashMap<>()))
              .addTransition(
                  new Transition("t2", "Target1", null, "target1", null, new HashMap<>()))
              .addTransition(
                  new Transition("t3", "Target2", null, "target2", null, new HashMap<>()))
              .addArc("t1", "p1", 1)
              .addArc("p1", "t2", 1)
              .addArc("p1", "t3", 1)
              .build();

      DAG dag = projector.projectToDAG(petriNet);

      // No edges should be projected because p1 has multiple consumers
      assertThat(dag.getNodes()).hasSize(3);
      assertThat(dag.getNode("t1").getDependencyIds()).isEmpty();
      assertThat(dag.getNode("t2").getDependencyIds()).isEmpty();
      assertThat(dag.getNode("t3").getDependencyIds()).isEmpty();
    }

    @Test
    @DisplayName("Should apply transitive reduction")
    void shouldApplyTransitiveReduction() {
      // Create Petri net with transitive edge: t1 -> p1 -> t2 -> p2 -> t3, and t1 -> p3 -> t3
      PetriNet petriNet =
          PetriNet.builder()
              .name("Transitive Workflow")
              .addPlace(new Place("p1", "Step1", null, null, new HashMap<>()))
              .addPlace(new Place("p2", "Step2", null, null, new HashMap<>()))
              .addPlace(new Place("p3", "Shortcut", null, null, new HashMap<>()))
              .addTransition(new Transition("t1", "Start", null, "start", null, new HashMap<>()))
              .addTransition(new Transition("t2", "Middle", null, "middle", null, new HashMap<>()))
              .addTransition(new Transition("t3", "End", null, "end", null, new HashMap<>()))
              .addArc("t1", "p1", 1)
              .addArc("p1", "t2", 1)
              .addArc("t2", "p2", 1)
              .addArc("p2", "t3", 1)
              .addArc("t1", "p3", 1)
              .addArc("p3", "t3", 1)
              .build();

      DAG dag = projector.projectToDAG(petriNet);

      // Should have edges: t1->t2, t2->t3, t1->t3
      // Transitive reduction should remove t1->t3 (since t1->t2->t3 exists)
      TaskNode t1 = dag.getNode("t1");
      TaskNode t2 = dag.getNode("t2");
      TaskNode t3 = dag.getNode("t3");

      assertThat(t1.getDependencyIds()).isEmpty();
      assertThat(t2.getDependencyIds()).containsExactly("t1");
      // t3 should depend on both t1 and t2 since there are two distinct paths
      assertThat(t3.getDependencyIds()).contains("t2");
    }
  }

  @Nested
  @DisplayName("DevOps Scenario Tests")
  class DevOpsScenarioTests {

    @Test
    @DisplayName("Should project CI/CD pipeline workflow")
    void shouldProjectCiCdPipelineWorkflow() {
      // DevOps workflow: commit -> build -> test -> deploy
      Map<String, Object> buildParams = new HashMap<>();
      buildParams.put("dockerfile", "Dockerfile");
      buildParams.put("context", ".");

      Map<String, Object> testParams = new HashMap<>();
      testParams.put("testSuite", "unit-tests");
      testParams.put("coverage", true);

      Map<String, Object> deployParams = new HashMap<>();
      deployParams.put("environment", "staging");
      deployParams.put("replicas", 3);

      PetriNet cicdNet =
          PetriNet.builder()
              .name("DevOps CI/CD Pipeline")
              .description("Automated deployment pipeline")
              // Places (intermediate states)
              .addPlace(new Place("source_ready", "Source Code Ready", null, null, new HashMap<>()))
              .addPlace(
                  new Place("build_complete", "Build Artifacts Ready", null, null, new HashMap<>()))
              .addPlace(new Place("tests_passed", "Tests Passed", null, null, new HashMap<>()))
              // Transitions (tasks)
              .addTransition(createDevOpsTransition("commit", "Code Commit", "git_push", null))
              .addTransition(
                  createDevOpsTransition("build", "Build Application", "docker_build", buildParams))
              .addTransition(createDevOpsTransition("test", "Run Tests", "pytest", testParams))
              .addTransition(
                  createDevOpsTransition(
                      "deploy", "Deploy to Staging", "kubernetes_deploy", deployParams))
              // Arcs (workflow connections)
              .addArc("commit", "source_ready", 1)
              .addArc("source_ready", "build", 1)
              .addArc("build", "build_complete", 1)
              .addArc("build_complete", "test", 1)
              .addArc("test", "tests_passed", 1)
              .addArc("tests_passed", "deploy", 1)
              // Initial marking
              .addInitialToken("source_ready", 0) // Pipeline starts when code is committed
              .build();

      DAG dag = projector.projectToDAG(cicdNet);

      assertThat(dag).isNotNull();
      assertThat(dag.getNodes()).hasSize(4);
      assertThat(dag.getName()).isEqualTo("DevOps CI/CD Pipeline");

      // Verify workflow structure: commit -> build -> test -> deploy
      TaskNode commitNode = dag.getNode("commit");
      TaskNode buildNode = dag.getNode("build");
      TaskNode testNode = dag.getNode("test");
      TaskNode deployNode = dag.getNode("deploy");

      assertThat(commitNode.getDependencyIds()).isEmpty(); // Root node
      assertThat(buildNode.getDependencyIds()).containsExactly("commit");
      assertThat(testNode.getDependencyIds()).containsExactly("build");
      assertThat(deployNode.getDependencyIds()).containsExactly("test");

      // Verify DevOps-specific parameters are preserved
      assertThat(buildNode.getInputParams()).containsEntry("dockerfile", "Dockerfile");
      assertThat(testNode.getInputParams()).containsEntry("testSuite", "unit-tests");
      assertThat(deployNode.getInputParams()).containsEntry("environment", "staging");

      // Verify cross-highlighting metadata
      assertThat(buildNode.getMetadata()).containsEntry("petriTransitionId", "build");
      assertThat(testNode.getMetadata()).containsKey("incomingEdges");

      // Verify projection metadata
      assertThat(dag.getMetadata()).containsEntry("projectedFrom", "PetriNet");
      assertThat(dag.getMetadata())
          .containsEntry("projectionAlgorithm", "single-producer-consumer");
    }

    @Test
    @DisplayName("Should project infrastructure deployment workflow")
    void shouldProjectInfrastructureDeploymentWorkflow() {
      // Infrastructure workflow: plan -> validate -> apply -> verify
      PetriNet infraNet =
          PetriNet.builder()
              .name("Infrastructure Deployment")
              .addPlace(
                  new Place("plan_ready", "Terraform Plan Ready", null, null, new HashMap<>()))
              .addPlace(new Place("validated", "Plan Validated", null, null, new HashMap<>()))
              .addPlace(new Place("applied", "Infrastructure Applied", null, null, new HashMap<>()))
              .addTransition(
                  createDevOpsTransition("plan", "Terraform Plan", "terraform_plan", null))
              .addTransition(
                  createDevOpsTransition("validate", "Validate Plan", "terraform_validate", null))
              .addTransition(
                  createDevOpsTransition("apply", "Apply Infrastructure", "terraform_apply", null))
              .addTransition(
                  createDevOpsTransition("verify", "Verify Deployment", "health_check", null))
              .addArc("plan", "plan_ready", 1)
              .addArc("plan_ready", "validate", 1)
              .addArc("validate", "validated", 1)
              .addArc("validated", "apply", 1)
              .addArc("apply", "applied", 1)
              .addArc("applied", "verify", 1)
              .build();

      DAG dag = projector.projectToDAG(infraNet);

      assertThat(dag.getNodes()).hasSize(4);

      // Verify linear dependency chain
      assertThat(dag.getNode("plan").getDependencyIds()).isEmpty();
      assertThat(dag.getNode("validate").getDependencyIds()).containsExactly("plan");
      assertThat(dag.getNode("apply").getDependencyIds()).containsExactly("validate");
      assertThat(dag.getNode("verify").getDependencyIds()).containsExactly("apply");
    }

    private Transition createDevOpsTransition(
        String id, String name, String action, Map<String, Object> params) {
      Map<String, Object> metadata = new HashMap<>();
      if (params != null) {
        metadata.put("inputParams", params);
      }
      metadata.put("category", "devops");
      return new Transition(id, name, null, action, null, metadata);
    }
  }

  @Nested
  @DisplayName("Football Management Scenario Tests")
  class FootballScenarioTests {

    @Test
    @DisplayName("Should project match preparation workflow")
    void shouldProjectMatchPreparationWorkflow() {
      // Football workflow: scout -> plan -> train -> select_team -> match
      Map<String, Object> scoutParams = new HashMap<>();
      scoutParams.put("opponent", "Liverpool FC");
      scoutParams.put("analysisDepth", "full");

      Map<String, Object> planParams = new HashMap<>();
      planParams.put("formation", "4-3-3");
      planParams.put("strategy", "possession");

      Map<String, Object> trainParams = new HashMap<>();
      trainParams.put("focus", "tactics");
      trainParams.put("intensity", "high");

      PetriNet footballNet =
          PetriNet.builder()
              .name("Match Preparation Workflow")
              .description("Complete match preparation process")
              // Places
              .addPlace(
                  new Place(
                      "opponent_analyzed",
                      "Opponent Analysis Complete",
                      null,
                      null,
                      new HashMap<>()))
              .addPlace(
                  new Place("tactics_planned", "Tactical Plan Ready", null, null, new HashMap<>()))
              .addPlace(
                  new Place("team_trained", "Team Training Complete", null, null, new HashMap<>()))
              .addPlace(
                  new Place("squad_selected", "Starting XI Selected", null, null, new HashMap<>()))
              // Transitions
              .addTransition(
                  createFootballTransition(
                      "scout", "Scout Opponent", "opponent_analysis", scoutParams))
              .addTransition(
                  createFootballTransition("plan", "Plan Tactics", "tactical_planning", planParams))
              .addTransition(
                  createFootballTransition(
                      "train", "Team Training", "training_session", trainParams))
              .addTransition(
                  createFootballTransition("select", "Team Selection", "squad_selection", null))
              .addTransition(
                  createFootballTransition("match", "Play Match", "match_execution", null))
              // Workflow arcs
              .addArc("scout", "opponent_analyzed", 1)
              .addArc("opponent_analyzed", "plan", 1)
              .addArc("plan", "tactics_planned", 1)
              .addArc("tactics_planned", "train", 1)
              .addArc("train", "team_trained", 1)
              .addArc("team_trained", "select", 1)
              .addArc("select", "squad_selected", 1)
              .addArc("squad_selected", "match", 1)
              .build();

      DAG dag = projector.projectToDAG(footballNet);

      assertThat(dag).isNotNull();
      assertThat(dag.getNodes()).hasSize(5);
      assertThat(dag.getName()).isEqualTo("Match Preparation Workflow");

      // Verify sequential workflow structure
      TaskNode scoutNode = dag.getNode("scout");
      TaskNode planNode = dag.getNode("plan");
      TaskNode trainNode = dag.getNode("train");
      TaskNode selectNode = dag.getNode("select");
      TaskNode matchNode = dag.getNode("match");

      assertThat(scoutNode.getDependencyIds()).isEmpty(); // Root
      assertThat(planNode.getDependencyIds()).containsExactly("scout");
      assertThat(trainNode.getDependencyIds()).containsExactly("plan");
      assertThat(selectNode.getDependencyIds()).containsExactly("train");
      assertThat(matchNode.getDependencyIds()).containsExactly("select");

      // Verify football-specific parameters
      assertThat(scoutNode.getInputParams()).containsEntry("opponent", "Liverpool FC");
      assertThat(planNode.getInputParams()).containsEntry("formation", "4-3-3");
      assertThat(trainNode.getInputParams()).containsEntry("focus", "tactics");

      // Verify metadata categorization
      assertThat(scoutNode.getMetadata()).containsEntry("category", "football");
    }

    @Test
    @DisplayName("Should project player transfer workflow")
    void shouldProjectPlayerTransferWorkflow() {
      // Transfer workflow: identify -> negotiate -> medical -> announce
      PetriNet transferNet =
          PetriNet.builder()
              .name("Player Transfer Process")
              .addPlace(
                  new Place(
                      "target_identified",
                      "Transfer Target Identified",
                      null,
                      null,
                      new HashMap<>()))
              .addPlace(
                  new Place("terms_agreed", "Contract Terms Agreed", null, null, new HashMap<>()))
              .addPlace(
                  new Place(
                      "medical_passed", "Medical Examination Passed", null, null, new HashMap<>()))
              .addTransition(
                  createFootballTransition("identify", "Identify Target", "player_scouting", null))
              .addTransition(
                  createFootballTransition(
                      "negotiate", "Negotiate Transfer", "contract_negotiation", null))
              .addTransition(
                  createFootballTransition("medical", "Medical Check", "medical_examination", null))
              .addTransition(
                  createFootballTransition(
                      "announce", "Announce Transfer", "media_announcement", null))
              .addArc("identify", "target_identified", 1)
              .addArc("target_identified", "negotiate", 1)
              .addArc("negotiate", "terms_agreed", 1)
              .addArc("terms_agreed", "medical", 1)
              .addArc("medical", "medical_passed", 1)
              .addArc("medical_passed", "announce", 1)
              .build();

      DAG dag = projector.projectToDAG(transferNet);

      assertThat(dag.getNodes()).hasSize(4);

      // Verify dependency chain
      assertThat(dag.getNode("identify").getDependencyIds()).isEmpty();
      assertThat(dag.getNode("negotiate").getDependencyIds()).containsExactly("identify");
      assertThat(dag.getNode("medical").getDependencyIds()).containsExactly("negotiate");
      assertThat(dag.getNode("announce").getDependencyIds()).containsExactly("medical");
    }

    private Transition createFootballTransition(
        String id, String name, String action, Map<String, Object> params) {
      Map<String, Object> metadata = new HashMap<>();
      if (params != null) {
        metadata.put("inputParams", params);
      }
      metadata.put("category", "football");
      return new Transition(id, name, null, action, null, metadata);
    }
  }

  @Nested
  @DisplayName("Complex Workflow Tests")
  class ComplexWorkflowTests {

    @Test
    @DisplayName("Should handle choice transitions")
    void shouldHandleChoiceTransitions() {
      // Create workflow with choice: review -> {approve, reject}
      Transition reviewTransition =
          new Transition("review", "Review Code", null, "code_review", null, new HashMap<>());
      Transition approveTransition =
          Transition.builder("approve")
              .name("Approve PR")
              .action("approve_merge")
              .asChoice("status == 'approved'")
              .build();
      Transition rejectTransition =
          Transition.builder("reject")
              .name("Reject PR")
              .action("reject_merge")
              .asChoice("status == 'rejected'")
              .build();

      PetriNet choiceNet =
          PetriNet.builder()
              .name("Code Review Workflow")
              .addPlace(new Place("reviewed", "Code Reviewed", null, null, new HashMap<>()))
              .addTransition(reviewTransition)
              .addTransition(approveTransition)
              .addTransition(rejectTransition)
              .addArc("review", "reviewed", 1)
              .addArc("reviewed", "approve", 1)
              .addArc("reviewed", "reject", 1)
              .build();

      DAG dag = projector.projectToDAG(choiceNet);

      // Should have all transitions but no projected edges (multiple consumers)
      assertThat(dag.getNodes()).hasSize(3);

      TaskNode reviewNode = dag.getNode("review");
      TaskNode approveNode = dag.getNode("approve");
      TaskNode rejectNode = dag.getNode("reject");

      // Check choice metadata is preserved
      assertThat(approveNode.getMetadata()).containsEntry("executionType", "choice");
      assertThat(approveNode.getMetadata())
          .containsEntry("choiceCondition", "status == 'approved'");
    }

    @Test
    @DisplayName("Should handle fork-join patterns")
    void shouldHandleForkJoinPatterns() {
      // Create parallel workflow: start -> {task1, task2} -> join -> end
      PetriNet parallelNet =
          PetriNet.builder()
              .name("Parallel Workflow")
              .addPlace(new Place("started", "Workflow Started", null, null, new HashMap<>()))
              .addPlace(new Place("task1_done", "Task 1 Complete", null, null, new HashMap<>()))
              .addPlace(new Place("task2_done", "Task 2 Complete", null, null, new HashMap<>()))
              .addPlace(new Place("all_done", "All Tasks Complete", null, null, new HashMap<>()))
              .addTransition(
                  Transition.builder("start")
                      .name("Start Work")
                      .action("initialize")
                      .asFork()
                      .build())
              .addTransition(
                  new Transition("task1", "Task 1", null, "execute_task1", null, new HashMap<>()))
              .addTransition(
                  new Transition("task2", "Task 2", null, "execute_task2", null, new HashMap<>()))
              .addTransition(
                  Transition.builder("join")
                      .name("Join Results")
                      .action("combine_results")
                      .asJoin()
                      .build())
              .addTransition(
                  new Transition("end", "Complete", null, "finalize", null, new HashMap<>()))
              .addArc("start", "started", 1)
              .addArc("started", "task1", 1)
              .addArc("started", "task2", 1)
              .addArc("task1", "task1_done", 1)
              .addArc("task2", "task2_done", 1)
              .addArc("task1_done", "join", 1)
              .addArc("task2_done", "join", 1)
              .addArc("join", "all_done", 1)
              .addArc("all_done", "end", 1)
              .build();

      DAG dag = projector.projectToDAG(parallelNet);

      assertThat(dag.getNodes()).hasSize(5);

      // Check fork and join metadata
      TaskNode startNode = dag.getNode("start");
      TaskNode joinNode = dag.getNode("join");
      TaskNode endNode = dag.getNode("end");

      assertThat(startNode.getMetadata()).containsEntry("executionType", "fork");
      assertThat(joinNode.getMetadata()).containsEntry("executionType", "join");
      assertThat(endNode.getDependencyIds())
          .containsExactly("join"); // Only join -> end edge projected
    }

    @Test
    @DisplayName("Should preserve original transition metadata")
    void shouldPreserveOriginalTransitionMetadata() {
      Map<String, Object> customMetadata = new HashMap<>();
      customMetadata.put("priority", "high");
      customMetadata.put("timeout", 300);
      customMetadata.put("retries", 3);

      Transition transition =
          new Transition(
              "custom",
              "Custom Task",
              "Custom description",
              "custom_action",
              "guard_condition",
              customMetadata);

      PetriNet simpleNet = PetriNet.builder().name("Simple Net").addTransition(transition).build();

      DAG dag = projector.projectToDAG(simpleNet);

      TaskNode customNode = dag.getNode("custom");
      assertThat(customNode.getMetadata()).containsEntry("priority", "high");
      assertThat(customNode.getMetadata()).containsEntry("timeout", 300);
      assertThat(customNode.getMetadata()).containsEntry("retries", 3);

      // Check additional metadata added by projector
      assertThat(customNode.getMetadata()).containsEntry("petriTransitionId", "custom");
      assertThat(customNode.getMetadata()).containsEntry("petriTransitionName", "Custom Task");

      // Check input parameters include guard and description
      assertThat(customNode.getInputParams()).containsEntry("description", "Custom description");
      assertThat(customNode.getInputParams()).containsEntry("guard", "guard_condition");
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesAndErrorHandling {

    @Test
    @DisplayName("Should handle empty workflow gracefully")
    void shouldHandleEmptyWorkflowGracefully() {
      PetriNet emptyNet =
          PetriNet.builder()
              .name("Empty Workflow")
              .addPlace(new Place("lonely", "Lonely Place", null, null, new HashMap<>()))
              .addTransition(
                  new Transition(
                      "orphan", "Orphan Transition", null, "orphan_action", null, new HashMap<>()))
              .build(); // No arcs

      DAG dag = projector.projectToDAG(emptyNet);

      assertThat(dag.getNodes()).hasSize(1);
      assertThat(dag.getNode("orphan").getDependencyIds()).isEmpty();
    }

    @Test
    @DisplayName("Should handle transitions with null or empty actions")
    void shouldHandleTransitionsWithNullOrEmptyActions() {
      PetriNet actionTestNet =
          PetriNet.builder()
              .name("Action Test")
              .addTransition(
                  new Transition("null_action", "Null Action", null, null, null, new HashMap<>()))
              .addTransition(
                  new Transition("empty_action", "Empty Action", null, "", null, new HashMap<>()))
              .addTransition(
                  new Transition(
                      "whitespace_action", "Whitespace Action", null, "  ", null, new HashMap<>()))
              .build();

      DAG dag = projector.projectToDAG(actionTestNet);

      // Should fallback to transition name, then to "execute"
      assertThat(dag.getNode("null_action").getAction()).isEqualTo("Null Action");
      assertThat(dag.getNode("empty_action").getAction()).isEqualTo("Empty Action");
      assertThat(dag.getNode("whitespace_action").getAction()).isEqualTo("Whitespace Action");
    }

    @Test
    @DisplayName("Should handle transitions with null names")
    void shouldHandleTransitionsWithNullNames() {
      PetriNet nullNameNet =
          PetriNet.builder()
              .name("Null Name Test")
              .addTransition(new Transition("test_id", null, null, null, null, new HashMap<>()))
              .build();

      DAG dag = projector.projectToDAG(nullNameNet);

      // Should fallback to default action
      assertThat(dag.getNode("test_id").getAction()).isEqualTo("execute");
    }

    @Test
    @DisplayName("Should maintain deterministic ordering with lexicographic sorting")
    void shouldMaintainDeterministicOrderingWithLexicographicSorting() {
      // Create multiple edges and verify consistent ordering
      PetriNet orderingNet =
          PetriNet.builder()
              .name("Ordering Test")
              .addPlace(new Place("p1", "P1", null, null, new HashMap<>()))
              .addPlace(new Place("p2", "P2", null, null, new HashMap<>()))
              .addPlace(new Place("p3", "P3", null, null, new HashMap<>()))
              .addTransition(
                  new Transition("z_task", "Z Task", null, "z_action", null, new HashMap<>()))
              .addTransition(
                  new Transition("a_task", "A Task", null, "a_action", null, new HashMap<>()))
              .addTransition(
                  new Transition("m_task", "M Task", null, "m_action", null, new HashMap<>()))
              .addTransition(
                  new Transition("final", "Final", null, "final_action", null, new HashMap<>()))
              .addArc("z_task", "p1", 1)
              .addArc("p1", "a_task", 1)
              .addArc("a_task", "p2", 1)
              .addArc("p2", "m_task", 1)
              .addArc("m_task", "p3", 1)
              .addArc("p3", "final", 1)
              .build();

      DAG dag1 = projector.projectToDAG(orderingNet);
      DAG dag2 = projector.projectToDAG(orderingNet);

      // Should produce identical results across multiple runs
      assertThat(dag1.getNode("a_task").getDependencyIds())
          .isEqualTo(dag2.getNode("a_task").getDependencyIds());
      assertThat(dag1.getNode("m_task").getDependencyIds())
          .isEqualTo(dag2.getNode("m_task").getDependencyIds());
      assertThat(dag1.getNode("final").getDependencyIds())
          .isEqualTo(dag2.getNode("final").getDependencyIds());
    }
  }
}
