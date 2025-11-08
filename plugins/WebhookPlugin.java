package plugins;

import java.util.Map;

/**
 * Extension of Plugin interface for plugins that handle webhook events.
 *
 * <p>WebhookPlugins can:
 * - Verify webhook signatures from external services
 * - Process webhook payloads asynchronously
 * - Route webhook events to appropriate handlers
 *
 * <p>Examples: GitHubPlugin, GitLabPlugin, BitbucketPlugin
 */
public interface WebhookPlugin extends Plugin {

  /**
   * Verify webhook signature to ensure authenticity.
   *
   * @param payload Raw webhook payload
   * @param signature Signature from webhook header
   * @param secret Webhook secret for verification
   * @return true if signature is valid
   */
  boolean verifyWebhookSignature(String payload, String signature, String secret);

  /**
   * Process a webhook event.
   *
   * @param eventType Type of webhook event (e.g., "pull_request", "push")
   * @param payload Raw webhook payload
   * @param headers Webhook headers
   * @return Result of webhook processing
   */
  PluginResult processWebhook(String eventType, String payload, Map<String, String> headers);

  /**
   * Get supported webhook event types.
   *
   * @return Array of supported event types
   */
  String[] getSupportedEvents();
}
