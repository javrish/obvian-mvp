package api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

/**
 * Configuration properties for the Natural Language Business Rule Service.
 * Provides comprehensive configuration for all aspects of the service including
 * performance settings, security options, caching behavior, and operational limits.
 * 
 * @author Obvian Labs
 * @since Phase 26.2b-service
 */
@Configuration
@ConfigurationProperties(prefix = "obvian.business-rule-service")
@Validated
public class BusinessRuleServiceConfig {
    
    // Service operational settings
    @Min(value = 1, message = "Background thread pool size must be at least 1")
    @Max(value = 100, message = "Background thread pool size cannot exceed 100")
    private int backgroundThreadPoolSize = 10;
    
    @Min(value = 1, message = "Max rule text length must be at least 1")
    private int maxRuleTextLength = 10000;
    
    @Min(value = 1, message = "Default page size must be at least 1")
    @Max(value = 1000, message = "Default page size cannot exceed 1000")
    private int defaultPageSize = 20;
    
    @Min(value = 1, message = "Max page size must be at least 1")
    @Max(value = 1000, message = "Max page size cannot exceed 1000")
    private int maxPageSize = 100;
    
    // Rule lifecycle settings
    private boolean allowCascadingDeletes = false;
    private boolean archiveInsteadOfDelete = true;
    private int maxRuleVersionsToKeep = 10;
    private Duration ruleArchiveRetentionPeriod = Duration.ofDays(365);
    
    // Validation settings
    private boolean enableStrictValidation = true;
    private boolean enableConflictDetection = true;
    private boolean enableDependencyAnalysis = true;
    private boolean enableSecurityValidation = true;
    private boolean enablePerformanceValidation = true;
    
    // Simulation and testing settings
    private boolean enableSimulation = true;
    private int maxSimulationScenarios = 100;
    private Duration maxSimulationTimeout = Duration.ofMinutes(5);
    private boolean generateTestDataAutomatically = true;
    
    // Caching configuration
    private boolean enableCaching = true;
    private Duration cacheExpiration = Duration.ofHours(1);
    private int maxCacheSize = 1000;
    private boolean enableDistributedCache = false;
    
    // Async processing settings
    private boolean enableAsyncProcessing = true;
    private Duration asyncTimeout = Duration.ofMinutes(10);
    private int asyncRetryAttempts = 3;
    private Duration asyncRetryDelay = Duration.ofSeconds(5);
    
    // Security settings
    private boolean enableRoleBasedAccess = true;
    private boolean enableAuditLogging = true;
    private boolean enableEncryption = false;
    private String encryptionKey = null;
    
    // Performance settings
    private Duration operationTimeout = Duration.ofMinutes(2);
    private int maxConcurrentOperations = 50;
    private boolean enablePerformanceMonitoring = true;
    private Duration performanceMetricsRetention = Duration.ofDays(30);
    
    // Notification settings
    private boolean enableNotifications = false;
    private String notificationChannel = "email";
    private boolean notifyOnRuleFailure = true;
    private boolean notifyOnConflictDetection = true;
    
    // Integration settings
    private boolean enableWebhooks = false;
    private String webhookUrl = null;
    private Duration webhookTimeout = Duration.ofSeconds(30);
    private int webhookRetryAttempts = 3;
    
    // Development and debugging settings
    private boolean enableDebugMode = false;
    private boolean enableDetailedLogging = false;
    private boolean enableMetricsCollection = true;
    private String logLevel = "INFO";
    
    // Advanced configuration
    private Map<String, Object> customSettings = new HashMap<>();
    
    public BusinessRuleServiceConfig() {
        // Initialize with sensible defaults
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Add any default custom settings
        customSettings.put("enableExperimentalFeatures", false);
        customSettings.put("maxMemoryUsageMB", 512);
        customSettings.put("enableGarbageCollectionTuning", false);
    }
    
    // Getters and Setters
    public int getBackgroundThreadPoolSize() {
        return backgroundThreadPoolSize;
    }
    
    public void setBackgroundThreadPoolSize(int backgroundThreadPoolSize) {
        this.backgroundThreadPoolSize = backgroundThreadPoolSize;
    }
    
    public int getMaxRuleTextLength() {
        return maxRuleTextLength;
    }
    
    public void setMaxRuleTextLength(int maxRuleTextLength) {
        this.maxRuleTextLength = maxRuleTextLength;
    }
    
    public int getDefaultPageSize() {
        return defaultPageSize;
    }
    
    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }
    
    public int getMaxPageSize() {
        return maxPageSize;
    }
    
    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
    
    public boolean isAllowCascadingDeletes() {
        return allowCascadingDeletes;
    }
    
    public void setAllowCascadingDeletes(boolean allowCascadingDeletes) {
        this.allowCascadingDeletes = allowCascadingDeletes;
    }
    
    public boolean isArchiveInsteadOfDelete() {
        return archiveInsteadOfDelete;
    }
    
    public void setArchiveInsteadOfDelete(boolean archiveInsteadOfDelete) {
        this.archiveInsteadOfDelete = archiveInsteadOfDelete;
    }
    
    public int getMaxRuleVersionsToKeep() {
        return maxRuleVersionsToKeep;
    }
    
    public void setMaxRuleVersionsToKeep(int maxRuleVersionsToKeep) {
        this.maxRuleVersionsToKeep = maxRuleVersionsToKeep;
    }
    
    public Duration getRuleArchiveRetentionPeriod() {
        return ruleArchiveRetentionPeriod;
    }
    
    public void setRuleArchiveRetentionPeriod(Duration ruleArchiveRetentionPeriod) {
        this.ruleArchiveRetentionPeriod = ruleArchiveRetentionPeriod;
    }
    
    public boolean isEnableStrictValidation() {
        return enableStrictValidation;
    }
    
    public void setEnableStrictValidation(boolean enableStrictValidation) {
        this.enableStrictValidation = enableStrictValidation;
    }
    
    public boolean isEnableConflictDetection() {
        return enableConflictDetection;
    }
    
    public void setEnableConflictDetection(boolean enableConflictDetection) {
        this.enableConflictDetection = enableConflictDetection;
    }
    
    public boolean isEnableDependencyAnalysis() {
        return enableDependencyAnalysis;
    }
    
    public void setEnableDependencyAnalysis(boolean enableDependencyAnalysis) {
        this.enableDependencyAnalysis = enableDependencyAnalysis;
    }
    
    public boolean isEnableSecurityValidation() {
        return enableSecurityValidation;
    }
    
    public void setEnableSecurityValidation(boolean enableSecurityValidation) {
        this.enableSecurityValidation = enableSecurityValidation;
    }
    
    public boolean isEnablePerformanceValidation() {
        return enablePerformanceValidation;
    }
    
    public void setEnablePerformanceValidation(boolean enablePerformanceValidation) {
        this.enablePerformanceValidation = enablePerformanceValidation;
    }
    
    public boolean isEnableSimulation() {
        return enableSimulation;
    }
    
    public void setEnableSimulation(boolean enableSimulation) {
        this.enableSimulation = enableSimulation;
    }
    
    public int getMaxSimulationScenarios() {
        return maxSimulationScenarios;
    }
    
    public void setMaxSimulationScenarios(int maxSimulationScenarios) {
        this.maxSimulationScenarios = maxSimulationScenarios;
    }
    
    public Duration getMaxSimulationTimeout() {
        return maxSimulationTimeout;
    }
    
    public void setMaxSimulationTimeout(Duration maxSimulationTimeout) {
        this.maxSimulationTimeout = maxSimulationTimeout;
    }
    
    public boolean isGenerateTestDataAutomatically() {
        return generateTestDataAutomatically;
    }
    
    public void setGenerateTestDataAutomatically(boolean generateTestDataAutomatically) {
        this.generateTestDataAutomatically = generateTestDataAutomatically;
    }
    
    public boolean isEnableCaching() {
        return enableCaching;
    }
    
    public void setEnableCaching(boolean enableCaching) {
        this.enableCaching = enableCaching;
    }
    
    public Duration getCacheExpiration() {
        return cacheExpiration;
    }
    
    public void setCacheExpiration(Duration cacheExpiration) {
        this.cacheExpiration = cacheExpiration;
    }
    
    public int getMaxCacheSize() {
        return maxCacheSize;
    }
    
    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }
    
    public boolean isEnableDistributedCache() {
        return enableDistributedCache;
    }
    
    public void setEnableDistributedCache(boolean enableDistributedCache) {
        this.enableDistributedCache = enableDistributedCache;
    }
    
    public boolean isEnableAsyncProcessing() {
        return enableAsyncProcessing;
    }
    
    public void setEnableAsyncProcessing(boolean enableAsyncProcessing) {
        this.enableAsyncProcessing = enableAsyncProcessing;
    }
    
    public Duration getAsyncTimeout() {
        return asyncTimeout;
    }
    
    public void setAsyncTimeout(Duration asyncTimeout) {
        this.asyncTimeout = asyncTimeout;
    }
    
    public int getAsyncRetryAttempts() {
        return asyncRetryAttempts;
    }
    
    public void setAsyncRetryAttempts(int asyncRetryAttempts) {
        this.asyncRetryAttempts = asyncRetryAttempts;
    }
    
    public Duration getAsyncRetryDelay() {
        return asyncRetryDelay;
    }
    
    public void setAsyncRetryDelay(Duration asyncRetryDelay) {
        this.asyncRetryDelay = asyncRetryDelay;
    }
    
    public boolean isEnableRoleBasedAccess() {
        return enableRoleBasedAccess;
    }
    
    public void setEnableRoleBasedAccess(boolean enableRoleBasedAccess) {
        this.enableRoleBasedAccess = enableRoleBasedAccess;
    }
    
    public boolean isEnableAuditLogging() {
        return enableAuditLogging;
    }
    
    public void setEnableAuditLogging(boolean enableAuditLogging) {
        this.enableAuditLogging = enableAuditLogging;
    }
    
    public boolean isEnableEncryption() {
        return enableEncryption;
    }
    
    public void setEnableEncryption(boolean enableEncryption) {
        this.enableEncryption = enableEncryption;
    }
    
    public String getEncryptionKey() {
        return encryptionKey;
    }
    
    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    public Duration getOperationTimeout() {
        return operationTimeout;
    }
    
    public void setOperationTimeout(Duration operationTimeout) {
        this.operationTimeout = operationTimeout;
    }
    
    public int getMaxConcurrentOperations() {
        return maxConcurrentOperations;
    }
    
    public void setMaxConcurrentOperations(int maxConcurrentOperations) {
        this.maxConcurrentOperations = maxConcurrentOperations;
    }
    
    public boolean isEnablePerformanceMonitoring() {
        return enablePerformanceMonitoring;
    }
    
    public void setEnablePerformanceMonitoring(boolean enablePerformanceMonitoring) {
        this.enablePerformanceMonitoring = enablePerformanceMonitoring;
    }
    
    public Duration getPerformanceMetricsRetention() {
        return performanceMetricsRetention;
    }
    
    public void setPerformanceMetricsRetention(Duration performanceMetricsRetention) {
        this.performanceMetricsRetention = performanceMetricsRetention;
    }
    
    public boolean isEnableNotifications() {
        return enableNotifications;
    }
    
    public void setEnableNotifications(boolean enableNotifications) {
        this.enableNotifications = enableNotifications;
    }
    
    public String getNotificationChannel() {
        return notificationChannel;
    }
    
    public void setNotificationChannel(String notificationChannel) {
        this.notificationChannel = notificationChannel;
    }
    
    public boolean isNotifyOnRuleFailure() {
        return notifyOnRuleFailure;
    }
    
    public void setNotifyOnRuleFailure(boolean notifyOnRuleFailure) {
        this.notifyOnRuleFailure = notifyOnRuleFailure;
    }
    
    public boolean isNotifyOnConflictDetection() {
        return notifyOnConflictDetection;
    }
    
    public void setNotifyOnConflictDetection(boolean notifyOnConflictDetection) {
        this.notifyOnConflictDetection = notifyOnConflictDetection;
    }
    
    public boolean isEnableWebhooks() {
        return enableWebhooks;
    }
    
    public void setEnableWebhooks(boolean enableWebhooks) {
        this.enableWebhooks = enableWebhooks;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public Duration getWebhookTimeout() {
        return webhookTimeout;
    }
    
    public void setWebhookTimeout(Duration webhookTimeout) {
        this.webhookTimeout = webhookTimeout;
    }
    
    public int getWebhookRetryAttempts() {
        return webhookRetryAttempts;
    }
    
    public void setWebhookRetryAttempts(int webhookRetryAttempts) {
        this.webhookRetryAttempts = webhookRetryAttempts;
    }
    
    public boolean isEnableDebugMode() {
        return enableDebugMode;
    }
    
    public void setEnableDebugMode(boolean enableDebugMode) {
        this.enableDebugMode = enableDebugMode;
    }
    
    public boolean isEnableDetailedLogging() {
        return enableDetailedLogging;
    }
    
    public void setEnableDetailedLogging(boolean enableDetailedLogging) {
        this.enableDetailedLogging = enableDetailedLogging;
    }
    
    public boolean isEnableMetricsCollection() {
        return enableMetricsCollection;
    }
    
    public void setEnableMetricsCollection(boolean enableMetricsCollection) {
        this.enableMetricsCollection = enableMetricsCollection;
    }
    
    public String getLogLevel() {
        return logLevel;
    }
    
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
    
    public Map<String, Object> getCustomSettings() {
        return new HashMap<>(customSettings);
    }
    
    public void setCustomSettings(Map<String, Object> customSettings) {
        this.customSettings = new HashMap<>(customSettings);
    }
    
    public Object getCustomSetting(String key) {
        return customSettings.get(key);
    }
    
    public void setCustomSetting(String key, Object value) {
        this.customSettings.put(key, value);
    }
    
    // Utility methods for common configuration queries
    public boolean isProductionMode() {
        return !enableDebugMode && !enableDetailedLogging;
    }
    
    public boolean isHighPerformanceMode() {
        return enableCaching && enableAsyncProcessing && !enableDetailedLogging;
    }
    
    public boolean isSecureMode() {
        return enableRoleBasedAccess && enableAuditLogging && enableSecurityValidation;
    }
    
    public boolean isValidationEnabled() {
        return enableStrictValidation && enableConflictDetection && enableDependencyAnalysis;
    }
    
    public long getCacheExpirationMs() {
        return cacheExpiration.toMillis();
    }
    
    public long getOperationTimeoutMs() {
        return operationTimeout.toMillis();
    }
    
    public long getAsyncTimeoutMs() {
        return asyncTimeout.toMillis();
    }
    
    public long getMaxSimulationTimeoutMs() {
        return maxSimulationTimeout.toMillis();
    }
    
    // Configuration validation
    public boolean isValid() {
        return backgroundThreadPoolSize > 0 &&
               maxRuleTextLength > 0 &&
               defaultPageSize > 0 &&
               maxPageSize >= defaultPageSize &&
               maxSimulationScenarios > 0 &&
               maxCacheSize > 0 &&
               asyncRetryAttempts >= 0 &&
               webhookRetryAttempts >= 0;
    }
    
    public String getValidationError() {
        if (backgroundThreadPoolSize <= 0) {
            return "Background thread pool size must be positive";
        }
        if (maxRuleTextLength <= 0) {
            return "Max rule text length must be positive";
        }
        if (defaultPageSize <= 0) {
            return "Default page size must be positive";
        }
        if (maxPageSize < defaultPageSize) {
            return "Max page size must be greater than or equal to default page size";
        }
        if (maxSimulationScenarios <= 0) {
            return "Max simulation scenarios must be positive";
        }
        if (maxCacheSize <= 0) {
            return "Max cache size must be positive";
        }
        return null;
    }
    
    // Factory methods for common configurations
    public static BusinessRuleServiceConfig developmentConfig() {
        BusinessRuleServiceConfig config = new BusinessRuleServiceConfig();
        config.setEnableDebugMode(true);
        config.setEnableDetailedLogging(true);
        config.setLogLevel("DEBUG");
        config.setEnableStrictValidation(false);
        config.setMaxRuleTextLength(50000);
        config.setBackgroundThreadPoolSize(5);
        return config;
    }
    
    public static BusinessRuleServiceConfig productionConfig() {
        BusinessRuleServiceConfig config = new BusinessRuleServiceConfig();
        config.setEnableDebugMode(false);
        config.setEnableDetailedLogging(false);
        config.setLogLevel("WARN");
        config.setEnableStrictValidation(true);
        config.setEnableCaching(true);
        config.setEnableAsyncProcessing(true);
        config.setEnablePerformanceMonitoring(true);
        config.setBackgroundThreadPoolSize(20);
        config.setMaxConcurrentOperations(100);
        return config;
    }
    
    public static BusinessRuleServiceConfig testConfig() {
        BusinessRuleServiceConfig config = new BusinessRuleServiceConfig();
        config.setEnableDebugMode(true);
        config.setEnableDetailedLogging(false);
        config.setLogLevel("INFO");
        config.setEnableCaching(false);
        config.setEnableAsyncProcessing(false);
        config.setEnableNotifications(false);
        config.setEnableWebhooks(false);
        config.setBackgroundThreadPoolSize(2);
        config.setMaxConcurrentOperations(10);
        return config;
    }
    
    @Override
    public String toString() {
        return "BusinessRuleServiceConfig{" +
                "backgroundThreadPoolSize=" + backgroundThreadPoolSize +
                ", maxRuleTextLength=" + maxRuleTextLength +
                ", defaultPageSize=" + defaultPageSize +
                ", enableCaching=" + enableCaching +
                ", enableAsyncProcessing=" + enableAsyncProcessing +
                ", enableStrictValidation=" + enableStrictValidation +
                ", enableDebugMode=" + enableDebugMode +
                '}';
    }
}