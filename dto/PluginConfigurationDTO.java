package api.dto;

import java.util.Map;

/**
 * DTO for plugin configuration.
 */
public class PluginConfigurationDTO {
    private Map<String, String> apiKeys;
    private Map<String, Object> settings;
    private Map<String, String> webhooks;
    private Map<String, Boolean> features;

    public void encryptSensitiveData() {
        // Encrypt API keys before storage
        if (apiKeys != null) {
            apiKeys.replaceAll((k, v) -> encryptValue(v));
        }
    }

    private String encryptValue(String value) {
        // TODO: Implement actual encryption
        return "encrypted:" + value;
    }

    // Getters and setters
    public Map<String, String> getApiKeys() { return apiKeys; }
    public void setApiKeys(Map<String, String> apiKeys) { this.apiKeys = apiKeys; }
    
    public Map<String, Object> getSettings() { return settings; }
    public void setSettings(Map<String, Object> settings) { this.settings = settings; }

    public Map<String, String> getWebhooks() { return webhooks; }
    public void setWebhooks(Map<String, String> webhooks) { this.webhooks = webhooks; }

    public Map<String, Boolean> getFeatures() { return features; }
    public void setFeatures(Map<String, Boolean> features) { this.features = features; }
}