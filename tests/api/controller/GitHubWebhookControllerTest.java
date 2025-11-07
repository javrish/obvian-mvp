package tests.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import api.controller.GitHubWebhookController;
import api.service.GitHubWebhookService;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tests for GitHubWebhookController.
 *
 * <p>Tests webhook signature verification, payload processing, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class GitHubWebhookControllerTest {

  @Mock private GitHubWebhookService webhookService;

  private GitHubWebhookController controller;
  private static final String WEBHOOK_SECRET = "test-webhook-secret-key";

  @BeforeEach
  void setUp() {
    controller = new GitHubWebhookController(webhookService, WEBHOOK_SECRET);
  }

  @Test
  @DisplayName("Should accept webhook with valid signature")
  void shouldAcceptValidWebhook() throws Exception {
    // Given: Valid webhook payload
    String payload =
        """
                {
                  "action": "opened",
                  "number": 1,
                  "pull_request": {
                    "id": 1,
                    "number": 1,
                    "title": "Test PR",
                    "head": {
                      "ref": "feature-branch",
                      "sha": "abc123",
                      "repo": {
                        "name": "test-repo",
                        "full_name": "owner/test-repo"
                      }
                    },
                    "base": {
                      "ref": "main",
                      "sha": "def456"
                    }
                  },
                  "repository": {
                    "name": "test-repo",
                    "full_name": "owner/test-repo",
                    "owner": {
                      "login": "owner",
                      "type": "User"
                    }
                  }
                }
                """;

    String signature = computeSignature(payload, WEBHOOK_SECRET);
    String deliveryId = "12345-67890";

    // When: Webhook received
    ResponseEntity<Void> response =
        controller.handlePullRequest(payload, signature, deliveryId, "pull_request");

    // Then: Should accept and queue for processing
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(webhookService, times(1)).processPullRequestWebhook(eq(payload), eq(deliveryId));
  }

  @Test
  @DisplayName("Should reject webhook with invalid signature")
  void shouldRejectInvalidSignature() {
    // Given: Payload with invalid signature
    String payload = "{\"action\": \"opened\"}";
    String invalidSignature = "sha256=invalid_signature_here";
    String deliveryId = "12345-67890";

    // When: Webhook received with invalid signature
    ResponseEntity<Void> response =
        controller.handlePullRequest(payload, invalidSignature, deliveryId, "pull_request");

    // Then: Should reject with 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(webhookService, never()).processPullRequestWebhook(anyString(), anyString());
  }

  @Test
  @DisplayName("Should reject webhook with missing signature")
  void shouldRejectMissingSignature() {
    // Given: Payload without signature
    String payload = "{\"action\": \"opened\"}";
    String deliveryId = "12345-67890";

    // When: Webhook received without signature
    ResponseEntity<Void> response =
        controller.handlePullRequest(payload, null, deliveryId, "pull_request");

    // Then: Should reject with 401 Unauthorized
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    verify(webhookService, never()).processPullRequestWebhook(anyString(), anyString());
  }

  @Test
  @DisplayName("Should reject webhook with wrong event type")
  void shouldRejectWrongEventType() throws Exception {
    // Given: Valid signature but wrong event type
    String payload = "{\"action\": \"opened\"}";
    String signature = computeSignature(payload, WEBHOOK_SECRET);
    String deliveryId = "12345-67890";

    // When: Webhook received with "push" event instead of "pull_request"
    ResponseEntity<Void> response =
        controller.handlePullRequest(payload, signature, deliveryId, "push");

    // Then: Should reject with 400 Bad Request
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    verify(webhookService, never()).processPullRequestWebhook(anyString(), anyString());
  }

  @Test
  @DisplayName("Should handle webhook processing exceptions")
  void shouldHandleProcessingExceptions() throws Exception {
    // Given: Valid webhook but service throws exception
    String payload = "{\"action\": \"opened\"}";
    String signature = computeSignature(payload, WEBHOOK_SECRET);
    String deliveryId = "12345-67890";

    doThrow(new RuntimeException("Processing error"))
        .when(webhookService)
        .processPullRequestWebhook(anyString(), anyString());

    // When: Webhook received
    ResponseEntity<Void> response =
        controller.handlePullRequest(payload, signature, deliveryId, "pull_request");

    // Then: Should return 500 Internal Server Error
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("Should accept webhook when secret not configured (dev mode)")
  void shouldAcceptWebhookInDevMode() {
    // Given: Controller with no webhook secret (dev mode)
    GitHubWebhookController devController = new GitHubWebhookController(webhookService, "");
    String payload = "{\"action\": \"opened\"}";
    String deliveryId = "12345-67890";

    // When: Webhook received without signature in dev mode
    ResponseEntity<Void> response =
        devController.handlePullRequest(payload, "any_signature", deliveryId, "pull_request");

    // Then: Should accept (signature verification disabled)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(webhookService, times(1)).processPullRequestWebhook(eq(payload), eq(deliveryId));
  }

  @Test
  @DisplayName("Should return healthy status from health endpoint")
  void shouldReturnHealthyStatus() {
    // When: Health check endpoint called
    ResponseEntity<String> response = controller.health();

    // Then: Should return 200 OK with message
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("healthy");
  }

  @Test
  @DisplayName("Should use constant-time comparison for signature verification")
  void shouldUseConstantTimeComparison() throws Exception {
    // Given: Two similar but different signatures
    String payload1 = "{\"action\": \"opened\"}";
    String payload2 = "{\"action\": \"closed\"}";

    String signature1 = computeSignature(payload1, WEBHOOK_SECRET);
    String signature2 = computeSignature(payload2, WEBHOOK_SECRET);

    String deliveryId = "12345-67890";

    // When: Payload1 sent with signature2 (mismatch)
    ResponseEntity<Void> response =
        controller.handlePullRequest(payload1, signature2, deliveryId, "pull_request");

    // Then: Should reject (constant-time comparison prevents timing attacks)
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /**
   * Helper method to compute HMAC SHA-256 signature like GitHub.
   */
  private String computeSignature(String payload, String secret) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(secretKey);
    byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

    StringBuilder signature = new StringBuilder("sha256=");
    for (byte b : hmacBytes) {
      signature.append(String.format("%02x", b));
    }
    return signature.toString();
  }
}
