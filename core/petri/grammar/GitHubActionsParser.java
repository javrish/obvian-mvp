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

      // Validate job names for reserved keywords
      validateJobNames(jobs.keySet());

      // Process each job
      for (Map.Entry<String, Object> jobEntry : jobs.entrySet()) {
        String jobId = jobEntry.getKey();
        @SuppressWarnings("unchecked")
        Map<String, Object> jobDef = (Map<String, Object>) jobEntry.getValue();

        processJob(specBuilder, jobId, jobDef);
      }

      // Validate dependencies after all jobs are processed
      validateDependencies(jobs, yamlContent);

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
   * Validates job names for reserved keywords.
   */
  private void validateJobNames(Set<String> jobNames) throws ParseException {
    Set<String> reserved = Set.of("on", "name", "jobs", "env", "defaults", "concurrency");
    for (String jobName : jobNames) {
      if (reserved.contains(jobName)) {
        throw new ParseException(
            "Job name '" + jobName + "' is a reserved keyword",
            0,
            "Rename the job to something like 'job-" + jobName + "' or '" + jobName + "-job'",
            null);
      }
    }
  }

  /**
   * Validates dependencies between jobs.
   */
  @SuppressWarnings("unchecked")
  private void validateDependencies(Map<String, Object> jobs, String yamlContent)
      throws ParseException {
    Map<String, List<String>> dependencies = new HashMap<>();

    // Extract all dependencies
    for (Map.Entry<String, Object> jobEntry : jobs.entrySet()) {
      String jobId = jobEntry.getKey();
      Map<String, Object> jobDef = (Map<String, Object>) jobEntry.getValue();
      List<String> deps = extractDependencies(jobDef);
      dependencies.put(jobId, deps);

      // Check for missing dependencies
      for (String dep : deps) {
        if (!jobs.containsKey(dep)) {
          String yamlContext = extractYamlContext(yamlContent, jobId);
          throw new ParseException(
              "Job '" + jobId + "' depends on '" + dep + "' which does not exist",
              findJobLineNumber(yamlContent, jobId),
              "Add job '" + dep + "' or remove it from the 'needs' list of '" + jobId + "'",
              yamlContext);
        }
      }
    }

    // Check for circular dependencies
    detectCircularDependencies(dependencies);
  }

  /**
   * Detects circular dependencies using DFS.
   */
  private void detectCircularDependencies(Map<String, List<String>> dependencies)
      throws ParseException {
    Set<String> visited = new HashSet<>();
    Set<String> recStack = new HashSet<>();
    List<String> cyclePath = new ArrayList<>();

    for (String job : dependencies.keySet()) {
      cyclePath.clear();
      if (hasCycle(job, dependencies, visited, recStack, cyclePath)) {
        String cycle = String.join(" → ", cyclePath);
        throw new ParseException(
            "Circular dependency detected in workflow: " + cycle,
            0,
            "Review the 'needs' relationships to remove the cycle",
            null);
      }
    }
  }

  private boolean hasCycle(
      String job,
      Map<String, List<String>> dependencies,
      Set<String> visited,
      Set<String> recStack,
      List<String> path) {
    if (recStack.contains(job)) {
      // Found cycle - add the job that completes the cycle
      path.add(job);
      return true;
    }
    if (visited.contains(job)) {
      return false;
    }

    visited.add(job);
    recStack.add(job);
    path.add(job);

    List<String> deps = dependencies.getOrDefault(job, Collections.emptyList());
    for (String dep : deps) {
      // Pass the same path list so we can track the cycle
      if (hasCycle(dep, dependencies, visited, recStack, path)) {
        return true;
      }
    }

    recStack.remove(job);
    // Only remove from path if no cycle found through this node
    if (!path.isEmpty() && path.get(path.size() - 1).equals(job)) {
      path.remove(path.size() - 1);
    }
    return false;
  }

  /**
   * Extracts YAML context around a specific job.
   */
  private String extractYamlContext(String yamlContent, String jobId) {
    String[] lines = yamlContent.split("\n");
    int jobLine = findJobLineNumber(yamlContent, jobId);

    if (jobLine < 0) {
      return "";
    }

    // Show 3 lines before and after
    int start = Math.max(0, jobLine - 3);
    int end = Math.min(lines.length, jobLine + 4);

    StringBuilder context = new StringBuilder();
    for (int i = start; i < end; i++) {
      String prefix = (i == jobLine) ? ">>> " : "    ";
      context.append(String.format("%s%3d | %s\n", prefix, i + 1, lines[i]));
    }

    return context.toString();
  }

  /**
   * Finds the line number where a job is defined.
   */
  private int findJobLineNumber(String yamlContent, String jobId) {
    String[] lines = yamlContent.split("\n");
    for (int i = 0; i < lines.length; i++) {
      // Look for job ID as a key at the jobs level
      if (lines[i].trim().startsWith(jobId + ":")) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Exception thrown when parsing fails.
   */
  public static class ParseException extends Exception {
    private final int lineNumber;
    private final String fixSuggestion;
    private final String yamlContext;

    public ParseException(String message, int lineNumber) {
      this(message, lineNumber, null, null, null);
    }

    public ParseException(String message, int lineNumber, Throwable cause) {
      this(message, lineNumber, null, null, cause);
    }

    public ParseException(
        String message, int lineNumber, String fixSuggestion, String yamlContext) {
      this(message, lineNumber, fixSuggestion, yamlContext, null);
    }

    public ParseException(
        String message,
        int lineNumber,
        String fixSuggestion,
        String yamlContext,
        Throwable cause) {
      super(formatMessage(message, lineNumber, fixSuggestion, yamlContext), cause);
      this.lineNumber = lineNumber;
      this.fixSuggestion = fixSuggestion != null ? fixSuggestion : "";
      this.yamlContext = yamlContext != null ? yamlContext : "";
    }

    private static String formatMessage(
        String message, int lineNumber, String fixSuggestion, String yamlContext) {
      StringBuilder sb = new StringBuilder();

      // Add line number prefix
      if (lineNumber > 0) {
        sb.append("Line ").append(lineNumber).append(": ");
      }
      sb.append(message);

      // Add YAML context
      if (yamlContext != null && !yamlContext.isEmpty()) {
        sb.append("\n\nContext:\n").append(yamlContext);
      }

      // Add fix suggestion
      if (fixSuggestion != null && !fixSuggestion.isEmpty()) {
        sb.append("\n\n💡 Suggestion: ").append(fixSuggestion);
      }

      return sb.toString();
    }

    public int getLineNumber() {
      return lineNumber;
    }

    public String getFixSuggestion() {
      return fixSuggestion;
    }

    public String getYamlContext() {
      return yamlContext;
    }
  }
}
