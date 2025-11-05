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
 * Comprehensive test suite for advanced workflow patterns in Phase 2 Priority 2.
 *
 * Tests the enhanced AutomationGrammar and P3Net processing capabilities for:
 * - Nested conditional workflows with AND/OR logic
 * - Loop and iteration patterns (for-each, while, retry)
 * - Event-driven patterns (triggers, webhooks, time-based)
 * - Error handling & compensation (try-catch-finally, circuit breaker)
 * - Complex parallel patterns (fan-out/fan-in, pipeline stages)
 * - Resource-constrained execution
 *
 * @author Obvian Labs
 * @since Phase 2 Priority 2 - Advanced Workflow Patterns
 */
class AdvancedWorkflowPatternsTest {

  private AutomationGrammar automationGrammar;
  private IntentToPetriMapper mapper;
  private RuleEngine ruleEngine;

  @BeforeEach
  void setUp() {
    this.mapper = new IntentToPetriMapper();
    this.ruleEngine = new RuleEngine();
    this.automationGrammar = new AutomationGrammar(mapper, ruleEngine);
  }

  @Nested
  @DisplayName("Enterprise CI/CD Workflow Tests")
  class EnterpriseCiCdTests {

    @Test
    @DisplayName("Complex microservice CI/CD pipeline with advanced patterns")
    void testComplexMicroservicePipeline() throws AutomationGrammar.GrammarTransformationException {
      // "For each microservice, run unit tests in parallel; if all pass, run integration tests;
      // if integration passes, deploy to staging; after staging validation, get approval and deploy
      // to production"

      PetriIntentSpec complexCiCdSpec =
          PetriIntentSpec.builder()
              .name("Complex Microservice CI/CD Pipeline")
              .description("Advanced CI/CD with loops, conditionals, error handling, and approvals")
              .originalPrompt(
                  "For each microservice, run unit tests in parallel; if all pass, run integration"
                      + " tests; if integration passes, deploy to staging; after staging"
                      + " validation, get approval and deploy to production")

              // 1. Loop through microservices
              .addLoopStep(
                  "foreach_microservice",
                  "Iterate through each microservice",
                  "hasMoreMicroservices")

              // 2. Parallel unit tests for current microservice
              .addParallelStep("unit_tests", "Run unit tests in parallel")

              // 3. Nested conditional: Check if ALL tests pass
              .addNestedConditionalStep(
                  "check_unit_tests",
                  "Evaluate if all unit tests passed",
                  Map.of(
                      "allTestsPassed",
                      "unit_tests.allPassed",
                      "coverageThreshold",
                      "unit_tests.coverage >= 80",
                      "qualityGate",
                      "unit_tests.qualityGate == 'PASSED'"))

              // 4. Integration tests (only if unit tests pass)
              .addActionStep("integration_tests", "Run integration tests")

              // 5. Error handler for test failures
              .addErrorHandlerStep(
                  "test_failure_handler",
                  "Handle test failures and notification",
                  Map.of("notifyTeam", true, "createTicket", true),
                  Arrays.asList("rollback_changes", "notify_stakeholders"))

              // 6. Staging deployment with circuit breaker
              .addCircuitBreakerStep(
                  "deploy_staging",
                  "Deploy to staging with circuit breaker",
                  Map.of("maxRetries", 3, "backoffMultiplier", 2.0))

              // 7. Staging validation
              .addActionStep("validate_staging", "Run staging validation tests")

              // 8. Approval gate (event trigger)
              .addEventTriggerStep(
                  "approval_gate",
                  "Wait for production deployment approval",
                  Map.of(
                      "type", "webhook", "approvers", Arrays.asList("team_lead", "devops_manager")))

              // 9. Production deployment with resource constraints
              .addResourceConstrainedStep(
                  "deploy_production",
                  "Deploy to production with resource limits",
                  Map.of("maxConcurrency", 2, "resourceType", "deployment_slots"))

              // 10. Final validation
              .addActionStep("validate_production", "Run production smoke tests")
              .build();

      // Add dependencies to create the workflow flow
      List<PetriIntentSpec.IntentStep> steps = new ArrayList<>(complexCiCdSpec.getSteps());
      for (int i = 1; i < steps.size(); i++) {
        PetriIntentSpec.IntentStep currentStep = steps.get(i);
        PetriIntentSpec.IntentStep previousStep = steps.get(i - 1);
        currentStep.getDependencies().add(previousStep.getId());
      }

      // Transform the intent to PetriNet
      PetriNet petriNet = automationGrammar.transform(complexCiCdSpec);

      // Validate the transformation
      assertThat(petriNet).isNotNull();
      assertThat(petriNet.getPlaces()).hasSizeGreaterThan(10);
      assertThat(petriNet.getTransitions()).hasSizeGreaterThan(8);
      assertThat(petriNet.getArcs()).hasSizeGreaterThan(15);

      // Verify advanced patterns were created
      assertThat(petriNet.getMetadata())
          .containsKey("loopOptimization_foreach_microservice")
          .containsKey("circuitBreakerOptimization_deploy_staging")
          .containsKey("optimizedConditional_check_unit_tests");

      // Verify error handling structure
      List<Place> errorPlaces =
          petriNet.getPlaces().stream()
              .filter(
                  place ->
                      place.getMetadata().containsKey("stepType")
                          && "ERROR_HANDLER".equals(place.getMetadata().get("stepType")))
              .toList();
      assertThat(errorPlaces).isNotEmpty();

      // Verify resource constraints
      List<Place> resourcePools =
          petriNet.getPlaces().stream()
              .filter(place -> place.getName().contains("Resource Pool"))
              .toList();
      assertThat(resourcePools).isNotEmpty();
    }

    @Test
    @DisplayName("Multi-stage pipeline with fan-out/fan-in patterns")
    void testMultiStagePipelinePattern() throws AutomationGrammar.GrammarTransformationException {
      PetriIntentSpec pipelineSpec =
          PetriIntentSpec.builder()
              .name("Multi-Stage Pipeline")
              .description("Pipeline with multiple stages and fan-out/fan-in synchronization")

              // Stage 1: Build
              .addPipelineStageStep("build_stage", "Build all components")

              // Stage 2: Fan-out to multiple test types
              .addFanOutFanInStep("test_fanout", "Fan out to different test types")

              // Stage 3: Unit tests (parallel)
              .addParallelStep("unit_tests", "Run unit tests for all modules")

              // Stage 4: Integration tests (parallel)
              .addParallelStep("integration_tests", "Run integration tests")

              // Stage 5: Security tests (with timeout)
              .addTimedStep("security_tests", "Run security scans", 300000L) // 5 minutes

              // Stage 6: Fan-in synchronization
              .addSyncStep("test_sync", "Synchronize all test results")

              // Stage 7: Deploy
              .addPipelineStageStep("deploy_stage", "Deploy validated artifacts")
              .build();

      PetriNet petriNet = automationGrammar.transform(pipelineSpec);

      assertThat(petriNet).isNotNull();
      assertThat(petriNet.getMetadata())
          .containsKey("pipelineOptimization_build_stage")
          .containsKey("pipelineOptimization_deploy_stage")
          .containsKey("fanOutFanInOptimization_test_fanout");

      // Verify timeout management
      assertThat(petriNet.getMetadata()).containsKey("timeout_security_tests");
    }
  }

  @Nested
  @DisplayName("Data Pipeline Workflow Tests")
  class DataPipelineTests {

    @Test
    @DisplayName("Complex data processing pipeline with error handling")
    void testDataProcessingPipeline() throws AutomationGrammar.GrammarTransformationException {
      // "Process batch of files: for each file, validate format; if valid, transform and load to
      // database;
      // if any fail, retry once; if still failing, move to error queue and alert data team"

      PetriIntentSpec dataPipelineSpec =
          PetriIntentSpec.builder()
              .name("Data Processing Pipeline")
              .description(
                  "Advanced data pipeline with validation, transformation, error handling, and"
                      + " compensation")
              .originalPrompt(
                  "Process batch of files: for each file, validate format; if valid, transform and"
                      + " load to database; if any fail, retry once; if still failing, move to"
                      + " error queue and alert data team")

              // 1. Loop through files in batch
              .addLoopStep("foreach_file", "Process each file in the batch", "hasMoreFiles")

              // 2. Validate file format
              .addActionStep("validate_format", "Validate file format and structure")

              // 3. Conditional: Check if validation passed
              .addNestedConditionalStep(
                  "check_validation",
                  "Check if file validation passed",
                  Map.of(
                      "formatValid",
                      "validate_format.result == 'VALID'",
                      "sizeOk",
                      "validate_format.size <= MAX_FILE_SIZE",
                      "schemaMatch",
                      "validate_format.schema == 'EXPECTED'"))

              // 4. Transform data (if validation passed)
              .addActionStep("transform_data", "Transform data according to business rules")

              // 5. Load to database with circuit breaker
              .addCircuitBreakerStep(
                  "load_database",
                  "Load transformed data to database",
                  Map.of("maxRetries", 3, "timeoutMs", 30000, "failureThreshold", 5))

              // 6. Error handler for processing failures
              .addErrorHandlerStep(
                  "processing_error_handler",
                  "Handle processing errors with retry and compensation",
                  Map.of("retryOnce", true, "logError", true),
                  Arrays.asList("move_to_error_queue", "alert_data_team", "create_error_report"))

              // 7. Success notification
              .addActionStep("success_notification", "Log successful processing")

              // 8. Compensation: Move failed files to error queue
              .addCompensationStep(
                  "error_queue_handler",
                  "Handle failed files",
                  Arrays.asList("move_to_error_queue", "update_status", "notify_admin"))
              .build();

      PetriNet petriNet = automationGrammar.transform(dataPipelineSpec);

      assertThat(petriNet).isNotNull();

      // Verify loop optimization
      assertThat(petriNet.getMetadata()).containsKey("loopOptimization_foreach_file");

      // Verify error handling structures
      List<Place> errorHandlerPlaces =
          petriNet.getPlaces().stream()
              .filter(place -> place.getMetadata().containsValue("ERROR_HANDLER"))
              .toList();
      assertThat(errorHandlerPlaces).hasSizeGreaterThan(0);

      // Verify compensation structures
      List<Transition> compensationTransitions =
          petriNet.getTransitions().stream()
              .filter(transition -> transition.getMetadata().containsValue("COMPENSATION"))
              .toList();
      assertThat(compensationTransitions).hasSizeGreaterThan(0);

      // Verify circuit breaker optimization
      assertThat(petriNet.getMetadata()).containsKey("circuitBreakerOptimization_load_database");
    }

    @Test
    @DisplayName("Real-time event streaming with resource constraints")
    void testRealTimeEventStreaming() throws AutomationGrammar.GrammarTransformationException {
      PetriIntentSpec streamingSpec =
          PetriIntentSpec.builder()
              .name("Real-time Event Streaming")
              .description("High-throughput event processing with resource management")

              // 1. Event trigger for incoming data
              .addEventTriggerStep(
                  "incoming_events",
                  "Listen for incoming events from message queue",
                  Map.of("type", "message_queue", "queue", "events.fifo"))

              // 2. Resource-constrained processing
              .addResourceConstrainedStep(
                  "process_events",
                  "Process events with limited workers",
                  Map.of(
                      "maxConcurrency", 10, "resourceType", "event_processors", "queueSize", 1000))

              // 3. Parallel enrichment
              .addParallelStep("enrich_data", "Enrich events with additional data")

              // 4. Conditional routing based on event type
              .addNestedConditionalStep(
                  "route_events",
                  "Route events based on type and priority",
                  Map.of(
                      "highPriority",
                      "event.priority == 'HIGH'",
                      "requiresEnrichment",
                      "event.needsEnrichment == true",
                      "realTimeProcessing",
                      "event.type == 'REAL_TIME'"))

              // 5. Output to different sinks
              .addActionStep("output_processed", "Send processed events to output sinks")
              .build();

      PetriNet petriNet = automationGrammar.transform(streamingSpec);

      assertThat(petriNet).isNotNull();

      // Verify event trigger optimization
      assertThat(petriNet.getMetadata()).containsKey("triggerOptimization_message_queue");

      // Verify resource constraint handling
      List<Place> resourcePools =
          petriNet.getPlaces().stream()
              .filter(place -> place.getName().contains("Resource Pool"))
              .toList();
      assertThat(resourcePools).isNotEmpty();

      // Verify shared resource pool creation
      assertThat(petriNet.getMetadata()).containsKey("shared_pool_event_processors");
    }
  }

  @Nested
  @DisplayName("Incident Response Workflow Tests")
  class IncidentResponseTests {

    @Test
    @DisplayName("Automated incident response with escalation patterns")
    void testIncidentResponseWorkflow() throws AutomationGrammar.GrammarTransformationException {
      // "When alert triggers: create incident ticket; notify on-call engineer; if no response in 15
      // minutes,
      // escalate to team lead; if critical severity, also notify manager immediately"

      PetriIntentSpec incidentSpec =
          PetriIntentSpec.builder()
              .name("Incident Response Workflow")
              .description("Automated incident response with escalation and notifications")
              .originalPrompt(
                  "When alert triggers: create incident ticket; notify on-call engineer; if no"
                      + " response in 15 minutes, escalate to team lead; if critical severity, also"
                      + " notify manager immediately")

              // 1. Alert trigger event
              .addEventTriggerStep(
                  "alert_trigger",
                  "Monitor for system alerts",
                  Map.of("type", "monitoring_webhook", "source", "prometheus"))

              // 2. Create incident ticket immediately
              .addActionStep("create_ticket", "Create incident ticket in system")

              // 3. Parallel notification paths
              .addParallelStep("initial_notifications", "Send initial notifications")

              // 4. Notify on-call engineer
              .addActionStep("notify_oncall", "Notify on-call engineer via phone/SMS")

              // 5. Critical severity check
              .addNestedConditionalStep(
                  "severity_check",
                  "Check incident severity for immediate escalation",
                  Map.of(
                      "isCritical",
                      "alert.severity == 'CRITICAL'",
                      "isP1",
                      "alert.priority == 'P1'",
                      "affectsProduction",
                      "alert.environment == 'PRODUCTION'"))

              // 6. Immediate manager notification for critical issues
              .addActionStep("notify_manager_critical", "Notify manager for critical incidents")

              // 7. Timed escalation trigger (15 minutes)
              .addEventTriggerStep(
                  "escalation_timer",
                  "15-minute escalation timer",
                  Map.of("type", "timer", "delayMs", 900000)) // 15 minutes

              // 8. Check for response
              .addNestedConditionalStep(
                  "response_check",
                  "Check if engineer has responded",
                  Map.of(
                      "hasResponded",
                      "ticket.status != 'NEW'",
                      "isAcknowledged",
                      "ticket.acknowledged == true"))

              // 9. Escalation to team lead
              .addActionStep("escalate_teamlead", "Escalate to team lead")

              // 10. Error handler for notification failures
              .addErrorHandlerStep(
                  "notification_failure_handler",
                  "Handle notification delivery failures",
                  Map.of("retryNotifications", true),
                  Arrays.asList("try_alternate_channels", "log_failure", "use_backup_contacts"))

              // 11. Resolution tracking
              .addActionStep("track_resolution", "Track incident resolution")
              .build();

      PetriNet petriNet = automationGrammar.transform(incidentSpec);

      assertThat(petriNet).isNotNull();

      // Verify event trigger handling
      assertThat(petriNet.getMetadata()).containsKey("triggerOptimization_monitoring_webhook");
      assertThat(petriNet.getMetadata()).containsKey("triggerOptimization_timer");

      // Verify parallel notification structure
      List<Place> parallelPlaces =
          petriNet.getPlaces().stream()
              .filter(place -> place.getMetadata().containsValue("PARALLEL"))
              .toList();
      assertThat(parallelPlaces).isNotEmpty();

      // Verify nested conditional optimization
      assertThat(petriNet.getMetadata()).containsKey("optimizedConditional_severity_check");
      assertThat(petriNet.getMetadata()).containsKey("optimizedConditional_response_check");

      // Verify error handling
      List<Place> errorHandlingPlaces =
          petriNet.getPlaces().stream()
              .filter(place -> place.getMetadata().containsValue("ERROR_HANDLER"))
              .toList();
      assertThat(errorHandlingPlaces).isNotEmpty();
    }

    @Test
    @DisplayName("Multi-team incident coordination with resource constraints")
    void testMultiTeamIncidentCoordination()
        throws AutomationGrammar.GrammarTransformationException {
      PetriIntentSpec coordinationSpec =
          PetriIntentSpec.builder()
              .name("Multi-Team Incident Coordination")
              .description(
                  "Coordinate incident response across multiple teams with resource management")

              // 1. Major incident detection
              .addEventTriggerStep(
                  "major_incident_trigger",
                  "Detect major incident requiring multi-team response",
                  Map.of("type", "alert_correlation", "threshold", "5_alerts_in_5_minutes"))

              // 2. Form incident response team (resource constrained)
              .addResourceConstrainedStep(
                  "form_response_team",
                  "Form incident response team from available engineers",
                  Map.of(
                      "maxConcurrency",
                      1,
                      "resourceType",
                      "incident_commanders",
                      "requiredSkills",
                      Arrays.asList("networking", "database", "frontend")))

              // 3. Parallel team notifications
              .addFanOutFanInStep("notify_teams", "Notify all relevant teams simultaneously")

              // 4. Network team response
              .addPipelineStageStep("network_team_response", "Network team investigation")

              // 5. Database team response
              .addPipelineStageStep("database_team_response", "Database team investigation")

              // 6. Application team response
              .addPipelineStageStep("app_team_response", "Application team investigation")

              // 7. Coordination sync point
              .addSyncStep("team_coordination_sync", "Synchronize findings from all teams")

              // 8. Decision making with timeout
              .addTimedStep(
                  "coordination_decision",
                  "Make coordination decision based on team findings",
                  600000L) // 10 minutes timeout

              // 9. Resolution execution
              .addActionStep("execute_resolution", "Execute coordinated resolution plan")
              .build();

      PetriNet petriNet = automationGrammar.transform(coordinationSpec);

      assertThat(petriNet).isNotNull();

      // Verify resource constraint optimization
      assertThat(petriNet.getMetadata()).containsKey("shared_pool_incident_commanders");

      // Verify pipeline stage optimization
      assertThat(petriNet.getMetadata())
          .containsKey("pipelineOptimization_network_team_response")
          .containsKey("pipelineOptimization_database_team_response")
          .containsKey("pipelineOptimization_app_team_response");

      // Verify fan-out/fan-in optimization
      assertThat(petriNet.getMetadata()).containsKey("fanOutFanInOptimization_notify_teams");

      // Verify timeout management
      assertThat(petriNet.getMetadata()).containsKey("timeout_coordination_decision");
    }
  }

  @Nested
  @DisplayName("Advanced Pattern Integration Tests")
  class AdvancedPatternIntegrationTests {

    @Test
    @DisplayName("Complex workflow combining all advanced patterns")
    void testComplexWorkflowIntegration() throws AutomationGrammar.GrammarTransformationException {
      PetriIntentSpec complexSpec =
          PetriIntentSpec.builder()
              .name("Complex Integrated Workflow")
              .description("Workflow demonstrating all advanced patterns working together")

              // Event-driven initiation
              .addEventTriggerStep(
                  "workflow_trigger",
                  "External system triggers workflow",
                  Map.of("type", "api_webhook", "authentication", "jwt"))

              // Loop with nested conditionals
              .addLoopStep(
                  "main_processing_loop",
                  "Main processing loop with complex logic",
                  "processingComplete")

              // Nested conditional within loop
              .addNestedConditionalStep(
                  "complex_decision",
                  "Complex multi-factor decision making",
                  Map.of(
                      "factorA",
                      "data.score > 0.8",
                      "factorB",
                      "data.category == 'HIGH_VALUE'",
                      "factorC",
                      "data.timestamp > cutoff"))

              // Fan-out to parallel processing
              .addFanOutFanInStep("parallel_processing", "Fan out to multiple processing paths")

              // Resource-constrained operations
              .addResourceConstrainedStep(
                  "heavy_computation",
                  "CPU-intensive computation with resource limits",
                  Map.of("maxConcurrency", 4, "resourceType", "compute_workers"))

              // Circuit breaker for external calls
              .addCircuitBreakerStep(
                  "external_api_call",
                  "Call external API with circuit breaker",
                  Map.of("maxRetries", 5, "backoffMs", 1000))

              // Error handling with compensation
              .addErrorHandlerStep(
                  "comprehensive_error_handler",
                  "Handle all types of errors with full compensation",
                  Map.of("handleAllErrors", true),
                  Arrays.asList(
                      "rollback_state", "notify_admin", "cleanup_resources", "log_incident"))

              // Pipeline stages for final processing
              .addPipelineStageStep("final_validation", "Final validation stage")
              .addPipelineStageStep("final_output", "Final output generation")

              // Synchronization point
              .addSyncStep("workflow_completion", "Synchronize workflow completion")
              .build();

      PetriNet petriNet = automationGrammar.transform(complexSpec);

      assertThat(petriNet).isNotNull();

      // Verify that all advanced pattern optimizations were applied
      assertThat(petriNet.getMetadata())
          .containsKey("triggerOptimization_api_webhook")
          .containsKey("loopOptimization_main_processing_loop")
          .containsKey("optimizedConditional_complex_decision")
          .containsKey("fanOutFanInOptimization_parallel_processing")
          .containsKey("shared_pool_compute_workers")
          .containsKey("circuitBreakerOptimization_external_api_call")
          .containsKey("compensationChain_comprehensive_error_handler")
          .containsKey("pipelineOptimization_final_validation")
          .containsKey("pipelineOptimization_final_output");

      // Verify global optimization structures
      assertThat(petriNet.getMetadata()).containsKey("global_error_handler");
      assertThat(petriNet.getMetadata()).containsKey("timeout_manager");

      // Verify complex structure size
      assertThat(petriNet.getPlaces()).hasSizeGreaterThan(20);
      assertThat(petriNet.getTransitions()).hasSizeGreaterThan(15);
      assertThat(petriNet.getArcs()).hasSizeGreaterThan(30);
    }

    @Test
    @DisplayName("Performance validation for complex workflows")
    void testComplexWorkflowPerformance() throws AutomationGrammar.GrammarTransformationException {
      // Create a large workflow to test performance
      PetriIntentSpec.Builder builder =
          PetriIntentSpec.builder()
              .name("Large Performance Test Workflow")
              .description("Large workflow to test transformation performance");

      // Add 50 steps of various advanced patterns
      for (int i = 0; i < 10; i++) {
        builder
            .addLoopStep("loop_" + i, "Loop step " + i, "condition_" + i)
            .addNestedConditionalStep(
                "conditional_" + i,
                "Conditional step " + i,
                Map.of("cond1", "value > " + i, "cond2", "status == 'OK'"))
            .addCircuitBreakerStep("circuit_" + i, "Circuit breaker " + i, Map.of("maxRetries", 3))
            .addResourceConstrainedStep(
                "resource_" + i, "Resource constrained " + i, Map.of("maxConcurrency", 2))
            .addErrorHandlerStep(
                "error_" + i,
                "Error handler " + i,
                Map.of("retry", true),
                Arrays.asList("compensate_" + i));
      }

      PetriIntentSpec largeSpec = builder.build();

      long startTime = System.currentTimeMillis();
      PetriNet petriNet = automationGrammar.transform(largeSpec);
      long endTime = System.currentTimeMillis();

      assertThat(petriNet).isNotNull();
      assertThat(endTime - startTime).isLessThan(5000); // Should complete in under 5 seconds

      // Verify all patterns were processed
      assertThat(petriNet.getPlaces()).hasSizeGreaterThan(100);
      assertThat(petriNet.getTransitions()).hasSizeGreaterThan(50);

      // Verify optimization metadata is present
      long optimizationCount =
          petriNet.getMetadata().entrySet().stream()
              .filter(entry -> entry.getKey().contains("Optimization"))
              .count();
      assertThat(optimizationCount).isGreaterThan(10);
    }
  }
}
