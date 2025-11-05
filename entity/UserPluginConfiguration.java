package api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity for persisting user-specific plugin configurations.
 * Stores encrypted API keys and settings per user per plugin.
 */
@Entity
@Table(name = "user_plugin_configurations", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "plugin_id"}))
public class UserPluginConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "plugin_id", nullable = false)
    private String pluginId;
    
    @Column(name = "plugin_name")
    private String pluginName;
    
    @Column(name = "plugin_version")
    private String pluginVersion;
    
    @JsonIgnore
    @Column(name = "encrypted_api_keys", columnDefinition = "TEXT")
    private Map<String, String> encryptedApiKeys = new HashMap<>();
    
    @Column(name = "settings", columnDefinition = "TEXT")
    private Map<String, Object> settings = new HashMap<>();
    
    @Column(name = "webhooks", columnDefinition = "TEXT")
    private Map<String, String> webhooks = new HashMap<>();
    
    @Column(name = "enabled")
    private boolean enabled = true;
    
    @Column(name = "configured")
    private boolean configured = false;
    
    @Column(name = "sandbox_mode")
    private boolean sandboxMode = true;
    
    @Column(name = "test_mode")
    private boolean testMode = false;
    
    @Column(name = "installation_date")
    private LocalDateTime installationDate;
    
    @Column(name = "last_configured_date")
    private LocalDateTime lastConfiguredDate;
    
    @Column(name = "last_used_date")
    private LocalDateTime lastUsedDate;
    
    @Column(name = "last_test_date")
    private LocalDateTime lastTestDate;
    
    @Column(name = "last_test_status")
    private String lastTestStatus;
    
    @Column(name = "execution_count")
    private int executionCount = 0;
    
    @Column(name = "success_count")
    private int successCount = 0;
    
    @Column(name = "failure_count")
    private int failureCount = 0;
    
    @Column(name = "rate_limit_remaining")
    private Integer rateLimitRemaining;
    
    @Column(name = "rate_limit_reset_at")
    private LocalDateTime rateLimitResetAt;
    
    @Column(name = "subscription_tier")
    private String subscriptionTier;
    
    @Column(name = "subscription_expires_at")
    private LocalDateTime subscriptionExpiresAt;
    
    @Column(name = "custom_endpoint")
    private String customEndpoint;
    
    @Column(name = "proxy_settings", columnDefinition = "TEXT")
    private Map<String, String> proxySettings = new HashMap<>();
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private Map<String, Object> metadata = new HashMap<>();
    
    @PrePersist
    protected void onCreate() {
        installationDate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        if (configured && lastConfiguredDate == null) {
            lastConfiguredDate = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }
    
    public String getPluginName() {
        return pluginName;
    }
    
    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
    
    public String getPluginVersion() {
        return pluginVersion;
    }
    
    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }
    
    public Map<String, String> getEncryptedApiKeys() {
        return encryptedApiKeys;
    }
    
    public void setEncryptedApiKeys(Map<String, String> encryptedApiKeys) {
        this.encryptedApiKeys = encryptedApiKeys;
    }
    
    public Map<String, Object> getSettings() {
        return settings;
    }
    
    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
    
    public Map<String, String> getWebhooks() {
        return webhooks;
    }
    
    public void setWebhooks(Map<String, String> webhooks) {
        this.webhooks = webhooks;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isConfigured() {
        return configured;
    }
    
    public void setConfigured(boolean configured) {
        this.configured = configured;
    }
    
    public boolean isSandboxMode() {
        return sandboxMode;
    }
    
    public void setSandboxMode(boolean sandboxMode) {
        this.sandboxMode = sandboxMode;
    }
    
    public boolean isTestMode() {
        return testMode;
    }
    
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }
    
    public LocalDateTime getInstallationDate() {
        return installationDate;
    }
    
    public void setInstallationDate(LocalDateTime installationDate) {
        this.installationDate = installationDate;
    }
    
    public LocalDateTime getLastConfiguredDate() {
        return lastConfiguredDate;
    }
    
    public void setLastConfiguredDate(LocalDateTime lastConfiguredDate) {
        this.lastConfiguredDate = lastConfiguredDate;
    }
    
    public LocalDateTime getLastUsedDate() {
        return lastUsedDate;
    }
    
    public void setLastUsedDate(LocalDateTime lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }
    
    public LocalDateTime getLastTestDate() {
        return lastTestDate;
    }
    
    public void setLastTestDate(LocalDateTime lastTestDate) {
        this.lastTestDate = lastTestDate;
    }
    
    public String getLastTestStatus() {
        return lastTestStatus;
    }
    
    public void setLastTestStatus(String lastTestStatus) {
        this.lastTestStatus = lastTestStatus;
    }
    
    public int getExecutionCount() {
        return executionCount;
    }
    
    public void setExecutionCount(int executionCount) {
        this.executionCount = executionCount;
    }
    
    public void incrementExecutionCount() {
        this.executionCount++;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }
    
    public void incrementSuccessCount() {
        this.successCount++;
    }
    
    public int getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }
    
    public void incrementFailureCount() {
        this.failureCount++;
    }
    
    public Integer getRateLimitRemaining() {
        return rateLimitRemaining;
    }
    
    public void setRateLimitRemaining(Integer rateLimitRemaining) {
        this.rateLimitRemaining = rateLimitRemaining;
    }
    
    public LocalDateTime getRateLimitResetAt() {
        return rateLimitResetAt;
    }
    
    public void setRateLimitResetAt(LocalDateTime rateLimitResetAt) {
        this.rateLimitResetAt = rateLimitResetAt;
    }
    
    public String getSubscriptionTier() {
        return subscriptionTier;
    }
    
    public void setSubscriptionTier(String subscriptionTier) {
        this.subscriptionTier = subscriptionTier;
    }
    
    public LocalDateTime getSubscriptionExpiresAt() {
        return subscriptionExpiresAt;
    }
    
    public void setSubscriptionExpiresAt(LocalDateTime subscriptionExpiresAt) {
        this.subscriptionExpiresAt = subscriptionExpiresAt;
    }
    
    public String getCustomEndpoint() {
        return customEndpoint;
    }
    
    public void setCustomEndpoint(String customEndpoint) {
        this.customEndpoint = customEndpoint;
    }
    
    public Map<String, String> getProxySettings() {
        return proxySettings;
    }
    
    public void setProxySettings(Map<String, String> proxySettings) {
        this.proxySettings = proxySettings;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    // Helper methods
    
    public void updateLastUsed() {
        this.lastUsedDate = LocalDateTime.now();
        this.executionCount++;
    }
    
    public void recordSuccess() {
        updateLastUsed();
        this.successCount++;
    }
    
    public void recordFailure() {
        updateLastUsed();
        this.failureCount++;
    }
    
    public boolean isExpired() {
        return subscriptionExpiresAt != null && 
               subscriptionExpiresAt.isBefore(LocalDateTime.now());
    }
    
    public boolean isRateLimited() {
        return rateLimitRemaining != null && 
               rateLimitRemaining <= 0 && 
               rateLimitResetAt != null &&
               rateLimitResetAt.isAfter(LocalDateTime.now());
    }
}