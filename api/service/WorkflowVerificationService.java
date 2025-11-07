package api.service;

import core.petri.PetriIntentSpec;
import core.petri.PetriNet;
import core.petri.grammar.AutomationGrammar;
import core.petri.grammar.GitHubActionsParser;
import core.petri.validation.PetriNetValidationResult;
import core.petri.validation.PetriNetValidator;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for end-to-end workflow verification.
 *
 * <p>Orchestrates the complete verification pipeline: YAML → PetriIntentSpec → PetriNet →
 * Validation → Result
 */
@Service
public class WorkflowVerificationService {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowVerificationService.class);

  private final GitHubActionsParser parser;
  private final AutomationGrammar automationGrammar;
  private final PetriNetValidator validator;

  public WorkflowVerificationService(
      GitHubActionsParser parser,
      AutomationGrammar automationGrammar,
      PetriNetValidator validator) {
    this.parser = parser;
    this.automationGrammar = automationGrammar;
    this.validator = validator;
  }

  /**
   * Verify a GitHub Actions workflow YAML.
   *
   * @param yamlContent Workflow YAML content
   * @param workflowPath Path to workflow file (for logging)
   * @return Validation result with pass/fail status and details
   * @throws WorkflowVerificationException if verification fails
   */
  public WorkflowVerificationResult verifyWorkflow(String yamlContent, String workflowPath)
      throws WorkflowVerificationException {
    logger.info("Starting workflow verification for: {}", workflowPath);

    long startTime = System.currentTimeMillis();
    WorkflowVerificationResult.Builder resultBuilder =
        WorkflowVerificationResult.builder().workflowPath(workflowPath);

    try {
      // Step 1: Parse YAML to PetriIntentSpec
      logger.debug("Step 1: Parsing workflow YAML");
      PetriIntentSpec intentSpec = parser.parse(yamlContent);
      resultBuilder.intentSpec(intentSpec);
      logger.info(
          "Parsed workflow with {} steps (path={})", intentSpec.getSteps().size(), workflowPath);

      // Step 2: Transform to PetriNet
      logger.debug("Step 2: Transforming to Petri net");
      PetriNet petriNet = automationGrammar.transform(intentSpec);
      resultBuilder.petriNet(petriNet);
      logger.info(
          "Built Petri net: {} places, {} transitions (path={})",
          petriNet.getPlaces().size(),
          petriNet.getTransitions().size(),
          workflowPath);

      // Step 3: Validate PetriNet
      logger.debug("Step 3: Validating Petri net");
      PetriNetValidationResult.ValidationConfig config =
          PetriNetValidationResult.ValidationConfig.defaultConfig();
      PetriNetValidationResult validationResult = validator.validate(petriNet, config);
      resultBuilder.validationResult(validationResult);

      // Determine overall status
      boolean passed = validationResult.getPetriStatus() == PetriNetValidationResult.PetriValidationStatus.PASS;
      resultBuilder.passed(passed);

      long duration = System.currentTimeMillis() - startTime;
      resultBuilder.verificationDurationMs(duration);

      logger.info(
          "Workflow verification {}: {} (path={}, duration={}ms)",
          passed ? "PASSED" : "FAILED",
          validationResult.getPetriStatus(),
          workflowPath,
          duration);

      return resultBuilder.build();

    } catch (GitHubActionsParser.ParseException e) {
      logger.error("Failed to parse workflow (path={}): {}", workflowPath, e.getMessage());
      throw new WorkflowVerificationException(
          "YAML parsing failed: " + e.getMessage(), e, WorkflowVerificationException.FailureStage.PARSING);
    } catch (Exception e) {
      logger.error(
          "Failed to verify workflow (path={}): {}", workflowPath, e.getMessage(), e);
      throw new WorkflowVerificationException(
          "Verification failed: " + e.getMessage(), e, WorkflowVerificationException.FailureStage.VALIDATION);
    }
  }

  /**
   * Result of workflow verification.
   */
  public static class WorkflowVerificationResult {
    private final String workflowPath;
    private final boolean passed;
    private final PetriIntentSpec intentSpec;
    private final PetriNet petriNet;
    private final PetriNetValidationResult validationResult;
    private final long verificationDurationMs;

    private WorkflowVerificationResult(Builder builder) {
      this.workflowPath = builder.workflowPath;
      this.passed = builder.passed;
      this.intentSpec = builder.intentSpec;
      this.petriNet = builder.petriNet;
      this.validationResult = builder.validationResult;
      this.verificationDurationMs = builder.verificationDurationMs;
    }

    public String getWorkflowPath() {
      return workflowPath;
    }

    public boolean isPassed() {
      return passed;
    }

    public PetriIntentSpec getIntentSpec() {
      return intentSpec;
    }

    public PetriNet getPetriNet() {
      return petriNet;
    }

    public PetriNetValidationResult getValidationResult() {
      return validationResult;
    }

    public long getVerificationDurationMs() {
      return verificationDurationMs;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static class Builder {
      private String workflowPath;
      private boolean passed;
      private PetriIntentSpec intentSpec;
      private PetriNet petriNet;
      private PetriNetValidationResult validationResult;
      private long verificationDurationMs;

      public Builder workflowPath(String workflowPath) {
        this.workflowPath = workflowPath;
        return this;
      }

      public Builder passed(boolean passed) {
        this.passed = passed;
        return this;
      }

      public Builder intentSpec(PetriIntentSpec intentSpec) {
        this.intentSpec = intentSpec;
        return this;
      }

      public Builder petriNet(PetriNet petriNet) {
        this.petriNet = petriNet;
        return this;
      }

      public Builder validationResult(PetriNetValidationResult validationResult) {
        this.validationResult = validationResult;
        return this;
      }

      public Builder verificationDurationMs(long verificationDurationMs) {
        this.verificationDurationMs = verificationDurationMs;
        return this;
      }

      public WorkflowVerificationResult build() {
        return new WorkflowVerificationResult(this);
      }
    }
  }

  /**
   * Exception thrown when workflow verification fails.
   */
  public static class WorkflowVerificationException extends Exception {
    private final FailureStage failureStage;

    public WorkflowVerificationException(String message, Throwable cause, FailureStage stage) {
      super(message, cause);
      this.failureStage = stage;
    }

    public FailureStage getFailureStage() {
      return failureStage;
    }

    public enum FailureStage {
      PARSING,
      TRANSFORMATION,
      VALIDATION
    }
  }
}
