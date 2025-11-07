package api.controller;

import api.dto.GitHubWebhookPayload;
import api.service.GitHubWebhookService;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling GitHub webhook events.
 *
 * <p>Receives GitHub pull_request webhooks, verifies signatures, and triggers workflow
 * verification.
 *
 * @see <a href="https://docs.github.com/webhooks">GitHub Webhooks Documentation</a>
 */
@RestController
@RequestMapping("/api/v1/github/webhooks")
public class GitHubWebhookController {

  private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookController.class);

  private final GitHubWebhookService webhookService;
  private final String webhookSecret;

  public GitHubWebhookController(
      GitHubWebhookService webhookService,
      @Value("${obvian.github.webhook.secret:}") String webhookSecret) {
    this.webhookService = webhookService;
    this.webhookSecret = webhookSecret;
  }

  /**
   * Handle pull_request webhook events.
   *
   * @param rawPayload Raw JSON payload from GitHub
   * @param signature HMAC SHA-256 signature from X-Hub-Signature-256 header
   * @param deliveryId Unique delivery ID from X-GitHub-Delivery header
   * @param event Event type from X-GitHub-Event header
   * @return 200 OK if webhook accepted, 401 Unauthorized if signature invalid, 400 Bad Request if
   *     payload invalid
   */
  @PostMapping("/pull_request")
  public ResponseEntity<Void> handlePullRequest(
      @RequestBody String rawPayload,
      @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
      @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
      @RequestHeader(value = "X-GitHub-Event", required = false) String event) {

    logger.info(
        "Received GitHub webhook: event={}, delivery={}, payloadLength={}",
        event,
        deliveryId,
        rawPayload != null ? rawPayload.length() : 0);

    // Verify signature
    if (signature == null || signature.isEmpty()) {
      logger.warn("Webhook rejected: Missing X-Hub-Signature-256 header (delivery={})", deliveryId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    if (!verifySignature(rawPayload, signature)) {
      logger.warn("Webhook rejected: Invalid signature (delivery={})", deliveryId);
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Verify event type
    if (!"pull_request".equals(event)) {
      logger.warn(
          "Webhook rejected: Unexpected event type '{}' (expected 'pull_request', delivery={})",
          event,
          deliveryId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    // Process webhook asynchronously
    try {
      webhookService.processPullRequestWebhook(rawPayload, deliveryId);
      logger.info("Webhook accepted and queued for processing (delivery={})", deliveryId);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      logger.error("Failed to process webhook (delivery={}): {}", deliveryId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Verify GitHub webhook signature using HMAC SHA-256.
   *
   * @param payload Raw payload string
   * @param signature Signature from X-Hub-Signature-256 header (format: "sha256=<hex>")
   * @return true if signature is valid, false otherwise
   */
  private boolean verifySignature(String payload, String signature) {
    if (webhookSecret == null || webhookSecret.isEmpty()) {
      logger.warn("Webhook secret not configured - signature verification disabled");
      return true; // Allow in development mode
    }

    try {
      // Compute HMAC SHA-256
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
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

  /**
   * Health check endpoint for webhook receiver.
   *
   * @return 200 OK with status message
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("GitHub webhook receiver is healthy");
  }
}
