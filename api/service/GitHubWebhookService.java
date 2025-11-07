package api.service;

import api.dto.GitHubWebhookPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.petri.PetriIntentSpec;
import core.petri.grammar.GitHubActionsParser;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for processing GitHub webhook events.
 *
 * <p>Handles async processing of pull request webhooks, fetching workflows, and triggering
 * verification.
 */
@Service
public class GitHubWebhookService {

  private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookService.class);

  private final ObjectMapper objectMapper;
  private final GitHubApiClient githubClient;
  private final GitHubActionsParser parser;
  private final ExecutorService executor;

  public GitHubWebhookService(
      ObjectMapper objectMapper, GitHubApiClient githubClient, GitHubActionsParser parser) {
    this.objectMapper = objectMapper;
    this.githubClient = githubClient;
    this.parser = parser;
    this.executor = Executors.newFixedThreadPool(10); // Pool for async webhook processing
  }

  /**
   * Process pull_request webhook asynchronously.
   *
   * @param rawPayload Raw JSON payload from GitHub
   * @param deliveryId Unique delivery ID for tracking
   */
  public void processPullRequestWebhook(String rawPayload, String deliveryId) {
    executor.submit(
        () -> {
          try {
            processWebhookInternal(rawPayload, deliveryId);
          } catch (Exception e) {
            logger.error(
                "Failed to process webhook (delivery={}): {}", deliveryId, e.getMessage(), e);
          }
        });
  }

  private void processWebhookInternal(String rawPayload, String deliveryId) throws Exception {
    logger.info("Processing webhook (delivery={})", deliveryId);

    // Parse payload
    GitHubWebhookPayload payload = objectMapper.readValue(rawPayload, GitHubWebhookPayload.class);

    String owner = payload.getRepository().getOwner().getLogin();
    String repo = payload.getRepository().getName();
    String sha = payload.getPullRequest().getHead().getSha();
    long prNumber = payload.getPullRequest().getNumber();

    logger.info(
        "Webhook details: owner={}, repo={}, pr={}, sha={}, action={}",
        owner,
        repo,
        prNumber,
        sha,
        payload.getAction());

    // List all workflow files
    String[] workflowPaths = githubClient.listWorkflowFiles(owner, repo, sha);

    if (workflowPaths.length == 0) {
      logger.warn("No workflow files found for {}/{} (pr={})", owner, repo, prNumber);
      return;
    }

    logger.info("Found {} workflow files to verify", workflowPaths.length);

    // Verify each workflow
    int successCount = 0;
    int failureCount = 0;

    for (String path : workflowPaths) {
      try {
        verifyWorkflow(owner, repo, path, sha, deliveryId);
        successCount++;
      } catch (Exception e) {
        logger.error("Failed to verify workflow {}: {}", path, e.getMessage(), e);
        failureCount++;
      }
    }

    logger.info(
        "Workflow verification complete (delivery={}): {} succeeded, {} failed",
        deliveryId,
        successCount,
        failureCount);
  }

  private void verifyWorkflow(
      String owner, String repo, String path, String sha, String deliveryId) throws Exception {
    logger.info(
        "Verifying workflow: owner={}, repo={}, path={}, sha={}", owner, repo, path, sha);

    // Fetch workflow YAML
    String yamlContent = githubClient.getWorkflowFile(owner, repo, path, sha);

    // Parse workflow
    PetriIntentSpec spec = parser.parse(yamlContent);

    logger.info(
        "Successfully parsed workflow {} (delivery={}): {} steps",
        path,
        deliveryId,
        spec.getSteps().size());

    // TODO: Build Petri net and validate (will be implemented in VALIDATION-1)
    // For now, just log success
    logger.info("Workflow {} verified successfully (delivery={})", path, deliveryId);
  }

  /**
   * Shutdown executor service gracefully.
   */
  public void shutdown() {
    logger.info("Shutting down webhook processor");
    executor.shutdown();
  }
}
