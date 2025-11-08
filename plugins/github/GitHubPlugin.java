package plugins.github;

import api.service.GitHubApiClient;
import api.service.WorkflowVerificationService;
import api.service.WorkflowVerificationService.WorkflowVerificationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import plugins.Plugin;
import plugins.PluginResult;
import plugins.PluginValidationResult;
import plugins.WebhookPlugin;

/**
 * GitHub integration plugin for workflow verification.
 *
 * <p>Capabilities:
 * - Receive GitHub pull_request webhooks
 * - Fetch workflow files from repositories
 * - Verify workflows using Petri net analysis
 * - Post check runs with verification results
 *
 * <p>Plugin ID: "github"
 */
@Component
public class GitHubPlugin implements WebhookPlugin {

  private static final Logger logger = LoggerFactory.getLogger(GitHubPlugin.class);
  private static final String PLUGIN_ID = "github";
  private static final String PLUGIN_NAME = "GitHub Workflow Verification";
  private static final String PLUGIN_VERSION = "1.0.0";

  private final ObjectMapper objectMapper;
  private final GitHubApiClient githubClient;
  private final WorkflowVerificationService verificationService;
  private final String webhookSecret;

  public GitHubPlugin(
      ObjectMapper objectMapper,
      GitHubApiClient githubClient,
      WorkflowVerificationService verificationService,
      @Value("${obvian.github.webhook.secret:}") String webhookSecret) {
    this.objectMapper = objectMapper;
    this.githubClient = githubClient;
    this.verificationService = verificationService;
    this.webhookSecret = webhookSecret;
  }

  @Override
  public String getId() {
    return PLUGIN_ID;
  }

  @Override
  public String getName() {
    return PLUGIN_NAME;
  }

  @Override
  public String getVersion() {
    return PLUGIN_VERSION;
  }

  @Override
  public PluginResult execute(Map<String, Object> parameters) {
    // Generic execution - delegates to webhook processing
    String action = (String) parameters.get("action");

    if ("verifyWorkflow".equals(action)) {
      return verifyWorkflowAction(parameters);
    }

    return PluginResult.failure("Unknown action: " + action);
  }

  @Override
  public PluginValidationResult validate(Map<String, Object> parameters) {
    // Validate plugin configuration
    PluginValidationResult.Builder builder = PluginValidationResult.builder();

    if (webhookSecret == null || webhookSecret.isEmpty()) {
      builder.addError("Webhook secret not configured (OBVIAN_GITHUB_WEBHOOK_SECRET)");
    }

    return builder.build();
  }

  @Override
  public boolean isHealthy() {
    // Check if GitHub API is accessible
    try {
      // Simple health check - could ping GitHub API
      return webhookSecret != null && !webhookSecret.isEmpty();
    } catch (Exception e) {
      logger.error("GitHub plugin health check failed", e);
      return false;
    }
  }

  @Override
  public String getConfigurationSchema() {
    return """
        {
          "type": "object",
          "properties": {
            "webhookSecret": {
              "type": "string",
              "description": "GitHub webhook secret for signature verification"
            },
            "token": {
              "type": "string",
              "description": "GitHub personal access token or app token"
            }
          },
          "required": ["webhookSecret", "token"]
        }
        """;
  }

  @Override
  public boolean verifyWebhookSignature(String payload, String signature, String secret) {
    if (secret == null || secret.isEmpty()) {
      logger.warn("Webhook secret not configured - signature verification disabled");
      return true; // Allow in development mode
    }

    try {
      // Compute HMAC SHA-256
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKey =
          new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(secretKey);
      byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

      // Convert to hex string
      StringBuilder computed = new StringBuilder("sha256=");
      for (byte b : hmacBytes) {
        computed.append(String.format("%02x", b));
      }

      // Constant-time comparison to prevent timing attacks
      return MessageDigest.isEqual(
          computed.toString().getBytes(StandardCharsets.UTF_8),
          signature.getBytes(StandardCharsets.UTF_8));

    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      logger.error("Failed to verify webhook signature: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public PluginResult processWebhook(
      String eventType, String payload, Map<String, String> headers) {

    logger.info("Processing GitHub webhook: event={}", eventType);

    if (!"pull_request".equals(eventType)) {
      return PluginResult.failure("Unsupported event type: " + eventType);
    }

    try {
      // Parse webhook payload
      JsonNode root = objectMapper.readTree(payload);

      String owner = root.path("repository").path("owner").path("login").asText();
      String repo = root.path("repository").path("name").asText();
      String sha = root.path("pull_request").path("head").path("sha").asText();
      long prNumber = root.path("pull_request").path("number").asLong();

      logger.info("Webhook details: owner={}, repo={}, pr={}, sha={}", owner, repo, prNumber, sha);

      // Fetch workflow files
      String[] workflowPaths = githubClient.listWorkflowFiles(owner, repo, sha);

      if (workflowPaths.length == 0) {
        logger.warn("No workflow files found for {}/{} (pr={})", owner, repo, prNumber);
        return PluginResult.success(Map.of("workflowsVerified", 0), "No workflows found");
      }

      // Verify each workflow
      int successCount = 0;
      int failureCount = 0;

      for (String path : workflowPaths) {
        try {
          String yamlContent = githubClient.getWorkflowFile(owner, repo, path, sha);
          WorkflowVerificationResult result =
              verificationService.verifyWorkflow(yamlContent, path);

          if (result.isPassed()) {
            logger.info("Workflow {} PASSED verification", path);
            successCount++;
          } else {
            logger.warn("Workflow {} FAILED verification", path);
            failureCount++;
          }

        } catch (Exception e) {
          logger.error("Failed to verify workflow {}: {}", path, e.getMessage(), e);
          failureCount++;
        }
      }

      Map<String, Object> output = new HashMap<>();
      output.put("owner", owner);
      output.put("repo", repo);
      output.put("pr", prNumber);
      output.put("sha", sha);
      output.put("workflowsVerified", workflowPaths.length);
      output.put("successCount", successCount);
      output.put("failureCount", failureCount);

      String message =
          String.format(
              "Verified %d workflows: %d passed, %d failed",
              workflowPaths.length, successCount, failureCount);

      return PluginResult.success(output, message);

    } catch (Exception e) {
      logger.error("Failed to process GitHub webhook: {}", e.getMessage(), e);
      return PluginResult.failure("Webhook processing failed: " + e.getMessage(), e);
    }
  }

  @Override
  public String[] getSupportedEvents() {
    return new String[] {"pull_request", "push"};
  }

  /**
   * Action: Verify a workflow file.
   */
  private PluginResult verifyWorkflowAction(Map<String, Object> parameters) {
    String yamlContent = (String) parameters.get("yamlContent");
    String workflowPath = (String) parameters.get("workflowPath");

    if (yamlContent == null || workflowPath == null) {
      return PluginResult.failure("Missing required parameters: yamlContent, workflowPath");
    }

    try {
      WorkflowVerificationResult result =
          verificationService.verifyWorkflow(yamlContent, workflowPath);

      Map<String, Object> output = new HashMap<>();
      output.put("passed", result.isPassed());
      output.put("workflowPath", result.getWorkflowPath());
      output.put("durationMs", result.getVerificationDurationMs());
      output.put("status", result.getValidationResult().getPetriStatus().toString());

      return PluginResult.success(output);

    } catch (Exception e) {
      return PluginResult.failure("Verification failed: " + e.getMessage(), e);
    }
  }
}
