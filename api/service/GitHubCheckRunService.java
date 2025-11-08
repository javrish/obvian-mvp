package api.service;

import api.service.WorkflowVerificationService.WorkflowVerificationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.petri.grammar.GitHubActionsParser.ParseException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for creating GitHub Check Runs to display workflow verification results in PRs.
 *
 * <p>Posts verification results as check runs with:
 * - Pass/fail status
 * - Detailed summary with verification metrics
 * - Line-level annotations for errors
 * - Links to detailed logs
 *
 * @see <a href="https://docs.github.com/en/rest/checks/runs">GitHub Check Runs API</a>
 */
@Service
public class GitHubCheckRunService {

  private static final Logger logger = LoggerFactory.getLogger(GitHubCheckRunService.class);
  private static final String GITHUB_API_BASE = "https://api.github.com";
  private static final String CHECK_RUN_NAME = "Obvian Workflow Verification";

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String githubToken;

  public GitHubCheckRunService(
      ObjectMapper objectMapper, @Value("${obvian.github.token:}") String githubToken) {
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    this.objectMapper = objectMapper;
    this.githubToken = githubToken;
  }

  /**
   * Create a check run for workflow verification results.
   *
   * @param owner Repository owner
   * @param repo Repository name
   * @param sha Commit SHA
   * @param workflowPath Path to workflow file
   * @param result Verification result
   * @throws IOException if API request fails
   */
  public void createCheckRun(
      String owner,
      String repo,
      String sha,
      String workflowPath,
      WorkflowVerificationResult result)
      throws IOException {

    logger.info(
        "Creating check run: owner={}, repo={}, sha={}, workflow={}, passed={}",
        owner,
        repo,
        sha,
        workflowPath,
        result.isPassed());

    Map<String, Object> checkRun = buildCheckRunPayload(sha, workflowPath, result);

    String url = String.format("%s/repos/%s/%s/check-runs", GITHUB_API_BASE, owner, repo);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/vnd.github.v3+json")
            .header("Authorization", "Bearer " + githubToken)
            .header("User-Agent", "Obvian-Workflow-Verify")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(
                HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(checkRun)))
            .build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 201) {
        logger.info(
            "Check run created successfully: workflow={}, status={}",
            workflowPath,
            result.isPassed() ? "success" : "failure");
      } else {
        logger.error(
            "Failed to create check run: status={}, body={}",
            response.statusCode(),
            response.body());
        throw new IOException(
            String.format(
                "GitHub API error: status=%d, body=%s", response.statusCode(), response.body()));
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  /**
   * Create check run for parsing failure.
   *
   * @param owner Repository owner
   * @param repo Repository name
   * @param sha Commit SHA
   * @param workflowPath Path to workflow file
   * @param parseException Parse exception with error details
   * @throws IOException if API request fails
   */
  public void createCheckRunForParseFailure(
      String owner, String repo, String sha, String workflowPath, ParseException parseException)
      throws IOException {

    logger.info(
        "Creating check run for parse failure: owner={}, repo={}, sha={}, workflow={}",
        owner,
        repo,
        sha,
        workflowPath);

    Map<String, Object> checkRun = new HashMap<>();
    checkRun.put("name", CHECK_RUN_NAME);
    checkRun.put("head_sha", sha);
    checkRun.put("status", "completed");
    checkRun.put("conclusion", "failure");
    checkRun.put("started_at", Instant.now().toString());
    checkRun.put("completed_at", Instant.now().toString());

    // Build output with error details
    Map<String, Object> output = new HashMap<>();
    output.put("title", "Workflow Verification Failed");
    output.put(
        "summary",
        String.format(
            "**Workflow:** `%s`\n\n**Error:** %s",
            workflowPath, parseException.getMessage()));

    // Add annotation if we have line number
    if (parseException.getLineNumber() > 0) {
      List<Map<String, Object>> annotations = new ArrayList<>();
      Map<String, Object> annotation = new HashMap<>();
      annotation.put("path", workflowPath);
      annotation.put("start_line", parseException.getLineNumber());
      annotation.put("end_line", parseException.getLineNumber());
      annotation.put("annotation_level", "failure");
      annotation.put("message", parseException.getMessage());
      if (!parseException.getFixSuggestion().isEmpty()) {
        annotation.put("title", "💡 " + parseException.getFixSuggestion());
      }
      annotations.add(annotation);
      output.put("annotations", annotations);
    }

    checkRun.put("output", output);

    // Send request
    String url = String.format("%s/repos/%s/%s/check-runs", GITHUB_API_BASE, owner, repo);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/vnd.github.v3+json")
            .header("Authorization", "Bearer " + githubToken)
            .header("User-Agent", "Obvian-Workflow-Verify")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(
                HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(checkRun)))
            .build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 201) {
        logger.info("Check run created for parse failure: workflow={}", workflowPath);
      } else {
        logger.error(
            "Failed to create check run: status={}, body={}",
            response.statusCode(),
            response.body());
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  /**
   * Build check run payload from verification result.
   */
  private Map<String, Object> buildCheckRunPayload(
      String sha, String workflowPath, WorkflowVerificationResult result) {

    Map<String, Object> checkRun = new HashMap<>();
    checkRun.put("name", CHECK_RUN_NAME);
    checkRun.put("head_sha", sha);
    checkRun.put("status", "completed");
    checkRun.put("conclusion", result.isPassed() ? "success" : "failure");
    checkRun.put("started_at", Instant.now().toString());
    checkRun.put("completed_at", Instant.now().toString());

    // Build output
    Map<String, Object> output = new HashMap<>();
    output.put("title", result.isPassed() ? "Workflow Verification Passed ✅" : "Workflow Verification Failed ❌");

    // Build summary
    StringBuilder summary = new StringBuilder();
    summary.append(String.format("**Workflow:** `%s`\n\n", workflowPath));
    summary.append(String.format("**Status:** %s\n", result.getValidationResult().getPetriStatus()));
    summary.append(String.format("**Duration:** %dms\n\n", result.getVerificationDurationMs()));

    if (result.getIntentSpec() != null) {
      summary.append(String.format("**Jobs Found:** %d\n", result.getIntentSpec().getSteps().size()));
    }

    if (result.getPetriNet() != null) {
      summary.append(String.format("**Petri Net:** %d places, %d transitions\n",
          result.getPetriNet().getPlaces().size(),
          result.getPetriNet().getTransitions().size()));
    }

    if (!result.isPassed()) {
      summary.append("\n### Validation Issues\n\n");
      summary.append("The workflow failed formal verification. ");
      summary.append("This may indicate structural problems like deadlocks or unreachable states.\n");
    }

    output.put("summary", summary.toString());

    checkRun.put("output", output);

    return checkRun;
  }
}
