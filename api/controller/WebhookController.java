package api.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import plugins.PluginRegistry;
import plugins.PluginResult;
import plugins.WebhookPlugin;

/**
 * Generic webhook controller that routes webhook events to appropriate plugins.
 *
 * <p>Supports any plugin implementing WebhookPlugin interface:
 * - GitHubPlugin (github)
 * - GitLabPlugin (gitlab)
 * - BitbucketPlugin (bitbucket)
 * - etc.
 *
 * <p>Routes:
 * - POST /api/v1/webhooks/{pluginId} - Route to specific plugin
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

  private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

  private final PluginRegistry pluginRegistry;

  public WebhookController(PluginRegistry pluginRegistry) {
    this.pluginRegistry = pluginRegistry;
  }

  /**
   * Handle webhook for a specific plugin.
   *
   * @param pluginId Plugin identifier (e.g., "github", "gitlab")
   * @param rawPayload Raw JSON payload from webhook
   * @param headers All request headers
   * @return 200 OK if webhook accepted, 404 if plugin not found, 401 if signature invalid
   */
  @PostMapping("/{pluginId}")
  public ResponseEntity<Map<String, Object>> handleWebhook(
      @PathVariable String pluginId,
      @RequestBody String rawPayload,
      @RequestHeader Map<String, String> headers) {

    logger.info("Received webhook for plugin: {}, payloadLength={}", pluginId, rawPayload.length());

    // Get plugin from registry
    Optional<plugins.Plugin> pluginOpt = pluginRegistry.getPlugin(pluginId);

    if (pluginOpt.isEmpty()) {
      logger.warn("Plugin not found: {}", pluginId);
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(Map.of("error", "Plugin not found: " + pluginId));
    }

    plugins.Plugin plugin = pluginOpt.get();

    if (!(plugin instanceof WebhookPlugin)) {
      logger.warn("Plugin {} does not support webhooks", pluginId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(Map.of("error", "Plugin does not support webhooks"));
    }

    WebhookPlugin webhookPlugin = (WebhookPlugin) plugin;

    // Extract webhook metadata from headers
    String signature = extractSignatureHeader(headers, pluginId);
    String eventType = extractEventTypeHeader(headers, pluginId);
    String deliveryId = extractDeliveryIdHeader(headers, pluginId);

    logger.info(
        "Webhook metadata: plugin={}, event={}, delivery={}", pluginId, eventType, deliveryId);

    // Verify signature (plugin-specific logic)
    String webhookSecret = getWebhookSecret(pluginId);
    if (signature != null && !signature.isEmpty()) {
      boolean validSignature = webhookPlugin.verifyWebhookSignature(rawPayload, signature, webhookSecret);

      if (!validSignature) {
        logger.warn("Invalid webhook signature for plugin: {} (delivery={})", pluginId, deliveryId);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid webhook signature"));
      }
    }

    // Process webhook asynchronously via plugin
    try {
      PluginResult result = webhookPlugin.processWebhook(eventType, rawPayload, headers);

      Map<String, Object> response = new HashMap<>();
      response.put("status", result.getStatus().toString());
      response.put("message", result.getMessage());
      response.put("output", result.getOutput());

      if (result.isSuccess()) {
        logger.info("Webhook processed successfully: plugin={}, delivery={}", pluginId, deliveryId);
        return ResponseEntity.ok(response);
      } else {
        logger.error(
            "Webhook processing failed: plugin={}, delivery={}, error={}",
            pluginId,
            deliveryId,
            result.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
      }

    } catch (Exception e) {
      logger.error("Failed to process webhook: plugin={}, delivery={}", pluginId, deliveryId, e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Webhook processing failed: " + e.getMessage()));
    }
  }

  /**
   * Health check endpoint.
   *
   * @return 200 OK with registered plugins
   */
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> health = new HashMap<>();
    health.put("status", "healthy");
    health.put("pluginCount", pluginRegistry.getPluginCount());
    health.put("allPluginsHealthy", pluginRegistry.areAllPluginsHealthy());

    return ResponseEntity.ok(health);
  }

  /**
   * List all registered webhook plugins.
   *
   * @return List of plugin IDs and names
   */
  @GetMapping("/plugins")
  public ResponseEntity<Map<String, Object>> listPlugins() {
    Map<String, Object> response = new HashMap<>();

    pluginRegistry.getAllPlugins().stream()
        .filter(p -> p instanceof WebhookPlugin)
        .forEach(
            p -> {
              Map<String, String> pluginInfo = new HashMap<>();
              pluginInfo.put("name", p.getName());
              pluginInfo.put("version", p.getVersion());
              pluginInfo.put("healthy", String.valueOf(p.isHealthy()));

              WebhookPlugin wp = (WebhookPlugin) p;
              pluginInfo.put("supportedEvents", String.join(", ", wp.getSupportedEvents()));

              response.put(p.getId(), pluginInfo);
            });

    return ResponseEntity.ok(response);
  }

  /**
   * Extract signature header (plugin-specific format).
   */
  private String extractSignatureHeader(Map<String, String> headers, String pluginId) {
    return switch (pluginId) {
      case "github" -> headers.get("x-hub-signature-256");
      case "gitlab" -> headers.get("x-gitlab-token");
      case "bitbucket" -> headers.get("x-hub-signature");
      default -> null;
    };
  }

  /**
   * Extract event type header (plugin-specific format).
   */
  private String extractEventTypeHeader(Map<String, String> headers, String pluginId) {
    return switch (pluginId) {
      case "github" -> headers.get("x-github-event");
      case "gitlab" -> headers.get("x-gitlab-event");
      case "bitbucket" -> headers.get("x-event-key");
      default -> "unknown";
    };
  }

  /**
   * Extract delivery ID header (plugin-specific format).
   */
  private String extractDeliveryIdHeader(Map<String, String> headers, String pluginId) {
    return switch (pluginId) {
      case "github" -> headers.get("x-github-delivery");
      case "gitlab" -> headers.get("x-gitlab-event-uuid");
      case "bitbucket" -> headers.get("x-request-uuid");
      default -> "unknown";
    };
  }

  /**
   * Get webhook secret for plugin (would come from configuration).
   */
  private String getWebhookSecret(String pluginId) {
    // In production, this would come from secure configuration
    // For now, plugins handle their own secrets via @Value injection
    return null;
  }
}
