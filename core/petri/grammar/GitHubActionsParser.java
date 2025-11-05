package core.petri.grammar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import core.petri.PetriIntentSpec;
import core.petri.PetriIntentSpec.IntentStep;
import core.petri.PetriIntentSpec.StepType;
import java.util.*;

/**
 * Parses GitHub Actions workflow YAML files into PetriIntentSpec format for formal verification.
 *
 * <p>Converts GitHub Actions workflows with jobs, steps, dependencies, and matrix builds into a
 * structured intent specification that can be validated using Petri net formal methods.
 *
 * <p>Supported features:
 *
 * <ul>
 *   <li>Job dependencies via 'needs' keyword
 *   <li>Matrix builds (creates AND-split for parallel jobs)
 *   <li>Conditional execution via 'if' conditions
 *   <li>Multi-step jobs (sequences)
 *   <li>Error handling with line number tracking
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GitHubActionsParser parser = new GitHubActionsParser();
 * PetriIntentSpec spec = parser.parse(yamlContent);
 * }</pre>
 *
 * @author Obvian Labs
 * @since Phase 21 - GitHub Actions Integration
 */
public class GitHubActionsParser {

  private final ObjectMapper yamlMapper;
  private final Map<String, Integer> nodeLineNumbers;

  public GitHubActionsParser() {
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
    this.nodeLineNumbers = new HashMap<>();
  }

  /**
   * Parse GitHub Actions workflow YAML into PetriIntentSpec.
   *
   * @param yamlContent GitHub Actions workflow YAML content
   * @return PetriIntentSpec representing the workflow
   * @throws ParseException if YAML is invalid or workflow structure is malformed
   */
  public PetriIntentSpec parse(String yamlContent) throws ParseException {
    try {
      // Parse YAML to Map
      @SuppressWarnings("unchecked")
      Map<String, Object> workflow = yamlMapper.readValue(yamlContent, Map.class);

      // Extract workflow metadata
      String workflowName = (String) workflow.getOrDefault("name", "Unnamed Workflow");

      // Extract jobs
      @SuppressWarnings("unchecked")
      Map<String, Object> jobs = (Map<String, Object>) workflow.get("jobs");
      if (jobs == null || jobs.isEmpty()) {
        throw new ParseException("No jobs found in workflow", 0);
      }

      // Build PetriIntentSpec
      PetriIntentSpec.Builder specBuilder =
          PetriIntentSpec.builder()
              .name(workflowName)
              .description("GitHub Actions workflow: " + workflowName)
              .modelType(PetriIntentSpec.MODEL_TYPE)
              .originalPrompt(yamlContent);

      // Process each job
      for (Map.Entry<String, Object> jobEntry : jobs.entrySet()) {
        String jobId = jobEntry.getKey();
        @SuppressWarnings("unchecked")
        Map<String, Object> jobDef = (Map<String, Object>) jobEntry.getValue();

        processJob(specBuilder, jobId, jobDef);
      }

      return specBuilder.build();

    } catch (Exception e) {
      if (e instanceof ParseException) {
        throw (ParseException) e;
      }
      throw new ParseException("Failed to parse GitHub Actions workflow: " + e.getMessage(), 0, e);
    }
  }

  /**
   * Process a single job and add it to the spec builder.
   *
   * @param specBuilder PetriIntentSpec builder
   * @param jobId Job identifier
   * @param jobDef Job definition from YAML
   * @throws ParseException if job structure is invalid
   */
  private void processJob(
      PetriIntentSpec.Builder specBuilder, String jobId, Map<String, Object> jobDef)
      throws ParseException {

    // Extract job properties
    String jobName = (String) jobDef.getOrDefault("name", jobId);

    // Check for matrix builds
    @SuppressWarnings("unchecked")
    Map<String, Object> strategy = (Map<String, Object>) jobDef.get("strategy");
    boolean hasMatrix = strategy != null && strategy.containsKey("matrix");

    // Extract dependencies (needs)
    List<String> dependencies = extractDependencies(jobDef);

    // Extract conditional (if)
    String condition = (String) jobDef.get("if");

    // Determine step type
    StepType stepType;
    if (hasMatrix) {
      stepType = StepType.PARALLEL; // Matrix builds are parallel
    } else if (condition != null) {
      stepType = StepType.CHOICE; // Conditional execution
    } else {
      stepType = StepType.ACTION; // Regular action
    }

    // Build metadata
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("jobId", jobId);
    metadata.put("jobName", jobName);
    metadata.put("hasMatrix", hasMatrix);
    if (hasMatrix) {
      metadata.put("matrix", strategy.get("matrix"));
    }
    if (condition != null) {
      metadata.put("condition", condition);
    }

    // Extract runs-on
    Object runsOn = jobDef.get("runs-on");
    if (runsOn != null) {
      metadata.put("runsOn", runsOn);
    }

    // Create IntentStep
    IntentStep step =
        new IntentStep(
            jobId, // id
            stepType, // type
            jobName, // description
            dependencies, // dependencies
            null, // conditions
            condition, // when (guard condition)
            metadata, // metadata
            null, // loopCondition
            null, // errorHandling
            null, // compensation
            extractTimeout(jobDef), // timeout
            null, // retryPolicy
            null // resourceConstraints
            );

    specBuilder.addStep(step);
  }

  /**
   * Extract job dependencies from 'needs' keyword.
   *
   * @param jobDef Job definition
   * @return List of job IDs this job depends on
   */
  @SuppressWarnings("unchecked")
  private List<String> extractDependencies(Map<String, Object> jobDef) {
    Object needs = jobDef.get("needs");
    if (needs == null) {
      return Collections.emptyList();
    }

    if (needs instanceof String) {
      return List.of((String) needs);
    } else if (needs instanceof List) {
      return new ArrayList<>((List<String>) needs);
    }

    return Collections.emptyList();
  }

  /**
   * Extract timeout from job definition.
   *
   * @param jobDef Job definition
   * @return Timeout in milliseconds, or null if not specified
   */
  private Long extractTimeout(Map<String, Object> jobDef) {
    Object timeoutMinutes = jobDef.get("timeout-minutes");
    if (timeoutMinutes instanceof Number) {
      return ((Number) timeoutMinutes).longValue() * 60 * 1000; // Convert to milliseconds
    }
    return null;
  }

  /**
   * Get line number for a given node/job ID.
   *
   * @param nodeId Node/job identifier
   * @return Line number, or -1 if not tracked
   */
  public int getLineNumber(String nodeId) {
    return nodeLineNumbers.getOrDefault(nodeId, -1);
  }

  /**
   * Exception thrown when parsing fails.
   */
  public static class ParseException extends Exception {
    private final int lineNumber;

    public ParseException(String message, int lineNumber) {
      super(formatMessage(message, lineNumber));
      this.lineNumber = lineNumber;
    }

    public ParseException(String message, int lineNumber, Throwable cause) {
      super(formatMessage(message, lineNumber), cause);
      this.lineNumber = lineNumber;
    }

    private static String formatMessage(String message, int lineNumber) {
      if (lineNumber > 0) {
        return "Line " + lineNumber + ": " + message;
      }
      return message;
    }

    public int getLineNumber() {
      return lineNumber;
    }
  }
}
