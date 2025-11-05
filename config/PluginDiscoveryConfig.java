package api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.Set;

/**
 * Configuration properties for the SmartPluginSuggestionService
 * 
 * Provides externalized configuration for plugin discovery, recommendation engine,
 * caching, performance monitoring, and other service-level settings.
 * 
 * @author Obvian Labs
 * @since Phase 26.2a
 */
@Configuration
@ConfigurationProperties(prefix = "obvian.plugin.discovery")
@Validated
public class PluginDiscoveryConfig {

    // Core recommendation settings
    @Min(value = 1, message = "Max recommendations must be at least 1")
    @Max(value = 100, message = "Max recommendations cannot exceed 100")
    private int maxRecommendations = 10;

    @DecimalMin(value = "0.0", message = "Min confidence threshold must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Min confidence threshold must be between 0.0 and 1.0")
    private double minConfidenceThreshold = 0.3;

    private boolean personalizationEnabled = true;
    private boolean explanationsEnabled = false;
    private boolean diversityControlEnabled = true;
    private double diversityThreshold = 0.7;

    // Async processing configuration
    private boolean asyncProcessingEnabled = true;
    
    @Min(value = 1, message = "Max concurrent requests must be at least 1")
    @Max(value = 100, message = "Max concurrent requests cannot exceed 100")
    private int maxConcurrentRequests = 10;

    @Min(value = 1, message = "Request timeout must be at least 1 second")
    @Max(value = 300, message = "Request timeout cannot exceed 300 seconds")
    private int requestTimeoutSeconds = 30;

    // Caching configuration
    private boolean cachingEnabled = true;
    
    @Min(value = 1, message = "Cache TTL must be at least 1 second")
    private int cacheTtlSeconds = 300;

    @Min(value = 1, message = "Cache max size must be at least 1")
    private int cacheMaxSize = 1000;

    private boolean cacheMetricsEnabled = true;
    private String cacheEvictionPolicy = "LRU";

    // Strategy configuration
    private String defaultStrategy = "CONTEXT_AWARE";
    private boolean hybridStrategyEnabled = true;
    private Map<String, Double> strategyWeights = Map.of(
        "POPULARITY_BASED", 0.3,
        "CONTEXT_AWARE", 0.4,
        "USER_PREFERENCE", 0.3
    );

    // Scoring weights for multi-factor scoring
    @DecimalMin(value = "0.0", message = "Weight must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Weight must be between 0.0 and 1.0")
    private double opportunityConfidenceWeight = 0.4;

    @DecimalMin(value = "0.0", message = "Weight must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Weight must be between 0.0 and 1.0")
    private double userHistoryWeight = 0.2;

    @DecimalMin(value = "0.0", message = "Weight must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Weight must be between 0.0 and 1.0")
    private double pluginHealthWeight = 0.15;

    @DecimalMin(value = "0.0", message = "Weight must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Weight must be between 0.0 and 1.0")
    private double contextFitWeight = 0.15;

    @DecimalMin(value = "0.0", message = "Weight must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Weight must be between 0.0 and 1.0")
    private double popularityWeight = 0.05;

    @DecimalMin(value = "0.0", message = "Weight must be between 0.0 and 1.0")
    @DecimalMax(value = "1.0", message = "Weight must be between 0.0 and 1.0")
    private double recencyWeight = 0.05;

    // Rate limiting configuration
    private boolean rateLimitingEnabled = true;
    
    @Min(value = 1, message = "Max requests per minute must be at least 1")
    private int maxRequestsPerMinute = 100;

    @Min(value = 1, message = "Max requests per hour must be at least 1")
    private int maxRequestsPerHour = 1000;

    private String rateLimitingStrategy = "TOKEN_BUCKET";

    // Performance monitoring
    private boolean metricsCollectionEnabled = true;
    private boolean performanceMonitoringEnabled = true;
    private boolean detailedLoggingEnabled = false;
    
    @Min(value = 1, message = "Metrics collection interval must be at least 1 second")
    private int metricsCollectionIntervalSeconds = 30;

    // WebSocket notifications
    private boolean webSocketNotificationsEnabled = true;
    private String webSocketDestinationPrefix = "/queue";
    private boolean broadcastUpdates = false;

    // Fallback and error handling
    private boolean fallbackRecommendationsEnabled = true;
    private int maxFallbackRecommendations = 3;
    private boolean circuitBreakerEnabled = true;
    private int circuitBreakerFailureThreshold = 5;
    private int circuitBreakerTimeoutSeconds = 60;

    // Health checks
    private boolean healthCheckEnabled = true;
    private int healthCheckIntervalSeconds = 30;
    private Set<String> healthCheckComponents = Set.of(
        "contextualAnalyzer", "recommendationEngine", "memoryStore", "pluginRegistry"
    );

    // Learning and adaptation
    private boolean learningEnabled = true;
    private boolean batchLearningEnabled = true;
    private int learningBatchSize = 100;
    private int learningIntervalMinutes = 60;

    // Experimental features
    private boolean experimentalFeaturesEnabled = false;
    private Set<String> enabledExperimentalFeatures = Set.of();
    
    // A/B testing
    private boolean abTestingEnabled = false;
    private Map<String, Double> abTestStrategies = Map.of();

    // Debug and development
    private boolean debugModeEnabled = false;
    private boolean requestResponseLoggingEnabled = false;
    private boolean performanceProfilingEnabled = false;

    // Getters and setters
    public int getMaxRecommendations() {
        return maxRecommendations;
    }

    public void setMaxRecommendations(int maxRecommendations) {
        this.maxRecommendations = maxRecommendations;
    }

    public double getMinConfidenceThreshold() {
        return minConfidenceThreshold;
    }

    public void setMinConfidenceThreshold(double minConfidenceThreshold) {
        this.minConfidenceThreshold = minConfidenceThreshold;
    }

    public boolean isPersonalizationEnabled() {
        return personalizationEnabled;
    }

    public void setPersonalizationEnabled(boolean personalizationEnabled) {
        this.personalizationEnabled = personalizationEnabled;
    }

    public boolean areExplanationsEnabled() {
        return explanationsEnabled;
    }

    public void setExplanationsEnabled(boolean explanationsEnabled) {
        this.explanationsEnabled = explanationsEnabled;
    }

    public boolean isDiversityControlEnabled() {
        return diversityControlEnabled;
    }

    public void setDiversityControlEnabled(boolean diversityControlEnabled) {
        this.diversityControlEnabled = diversityControlEnabled;
    }

    public double getDiversityThreshold() {
        return diversityThreshold;
    }

    public void setDiversityThreshold(double diversityThreshold) {
        this.diversityThreshold = diversityThreshold;
    }

    public boolean isAsyncProcessingEnabled() {
        return asyncProcessingEnabled;
    }

    public void setAsyncProcessingEnabled(boolean asyncProcessingEnabled) {
        this.asyncProcessingEnabled = asyncProcessingEnabled;
    }

    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public boolean isCachingEnabled() {
        return cachingEnabled;
    }

    public void setCachingEnabled(boolean cachingEnabled) {
        this.cachingEnabled = cachingEnabled;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public long getCacheTimeToLiveMs() {
        return cacheTtlSeconds * 1000L;
    }

    public int getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(int cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public boolean isCacheMetricsEnabled() {
        return cacheMetricsEnabled;
    }

    public void setCacheMetricsEnabled(boolean cacheMetricsEnabled) {
        this.cacheMetricsEnabled = cacheMetricsEnabled;
    }

    public String getCacheEvictionPolicy() {
        return cacheEvictionPolicy;
    }

    public void setCacheEvictionPolicy(String cacheEvictionPolicy) {
        this.cacheEvictionPolicy = cacheEvictionPolicy;
    }

    public String getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(String defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public boolean isHybridStrategyEnabled() {
        return hybridStrategyEnabled;
    }

    public void setHybridStrategyEnabled(boolean hybridStrategyEnabled) {
        this.hybridStrategyEnabled = hybridStrategyEnabled;
    }

    public Map<String, Double> getStrategyWeights() {
        return strategyWeights;
    }

    public void setStrategyWeights(Map<String, Double> strategyWeights) {
        this.strategyWeights = strategyWeights;
    }

    public double getOpportunityConfidenceWeight() {
        return opportunityConfidenceWeight;
    }

    public void setOpportunityConfidenceWeight(double opportunityConfidenceWeight) {
        this.opportunityConfidenceWeight = opportunityConfidenceWeight;
    }

    public double getUserHistoryWeight() {
        return userHistoryWeight;
    }

    public void setUserHistoryWeight(double userHistoryWeight) {
        this.userHistoryWeight = userHistoryWeight;
    }

    public double getPluginHealthWeight() {
        return pluginHealthWeight;
    }

    public void setPluginHealthWeight(double pluginHealthWeight) {
        this.pluginHealthWeight = pluginHealthWeight;
    }

    public double getContextFitWeight() {
        return contextFitWeight;
    }

    public void setContextFitWeight(double contextFitWeight) {
        this.contextFitWeight = contextFitWeight;
    }

    public double getPopularityWeight() {
        return popularityWeight;
    }

    public void setPopularityWeight(double popularityWeight) {
        this.popularityWeight = popularityWeight;
    }

    public double getRecencyWeight() {
        return recencyWeight;
    }

    public void setRecencyWeight(double recencyWeight) {
        this.recencyWeight = recencyWeight;
    }

    public boolean isRateLimitingEnabled() {
        return rateLimitingEnabled;
    }

    public void setRateLimitingEnabled(boolean rateLimitingEnabled) {
        this.rateLimitingEnabled = rateLimitingEnabled;
    }

    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }

    public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public int getMaxRequestsPerHour() {
        return maxRequestsPerHour;
    }

    public void setMaxRequestsPerHour(int maxRequestsPerHour) {
        this.maxRequestsPerHour = maxRequestsPerHour;
    }

    public String getRateLimitingStrategy() {
        return rateLimitingStrategy;
    }

    public void setRateLimitingStrategy(String rateLimitingStrategy) {
        this.rateLimitingStrategy = rateLimitingStrategy;
    }

    public boolean isMetricsCollectionEnabled() {
        return metricsCollectionEnabled;
    }

    public void setMetricsCollectionEnabled(boolean metricsCollectionEnabled) {
        this.metricsCollectionEnabled = metricsCollectionEnabled;
    }

    public boolean isPerformanceMonitoringEnabled() {
        return performanceMonitoringEnabled;
    }

    public void setPerformanceMonitoringEnabled(boolean performanceMonitoringEnabled) {
        this.performanceMonitoringEnabled = performanceMonitoringEnabled;
    }

    public boolean isDetailedLoggingEnabled() {
        return detailedLoggingEnabled;
    }

    public void setDetailedLoggingEnabled(boolean detailedLoggingEnabled) {
        this.detailedLoggingEnabled = detailedLoggingEnabled;
    }

    public int getMetricsCollectionIntervalSeconds() {
        return metricsCollectionIntervalSeconds;
    }

    public void setMetricsCollectionIntervalSeconds(int metricsCollectionIntervalSeconds) {
        this.metricsCollectionIntervalSeconds = metricsCollectionIntervalSeconds;
    }

    public boolean isWebSocketNotificationsEnabled() {
        return webSocketNotificationsEnabled;
    }

    public void setWebSocketNotificationsEnabled(boolean webSocketNotificationsEnabled) {
        this.webSocketNotificationsEnabled = webSocketNotificationsEnabled;
    }

    public String getWebSocketDestinationPrefix() {
        return webSocketDestinationPrefix;
    }

    public void setWebSocketDestinationPrefix(String webSocketDestinationPrefix) {
        this.webSocketDestinationPrefix = webSocketDestinationPrefix;
    }

    public boolean isBroadcastUpdates() {
        return broadcastUpdates;
    }

    public void setBroadcastUpdates(boolean broadcastUpdates) {
        this.broadcastUpdates = broadcastUpdates;
    }

    public boolean isFallbackRecommendationsEnabled() {
        return fallbackRecommendationsEnabled;
    }

    public void setFallbackRecommendationsEnabled(boolean fallbackRecommendationsEnabled) {
        this.fallbackRecommendationsEnabled = fallbackRecommendationsEnabled;
    }

    public int getMaxFallbackRecommendations() {
        return maxFallbackRecommendations;
    }

    public void setMaxFallbackRecommendations(int maxFallbackRecommendations) {
        this.maxFallbackRecommendations = maxFallbackRecommendations;
    }

    public boolean isCircuitBreakerEnabled() {
        return circuitBreakerEnabled;
    }

    public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) {
        this.circuitBreakerEnabled = circuitBreakerEnabled;
    }

    public int getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }

    public void setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
    }

    public int getCircuitBreakerTimeoutSeconds() {
        return circuitBreakerTimeoutSeconds;
    }

    public void setCircuitBreakerTimeoutSeconds(int circuitBreakerTimeoutSeconds) {
        this.circuitBreakerTimeoutSeconds = circuitBreakerTimeoutSeconds;
    }

    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    public int getHealthCheckIntervalSeconds() {
        return healthCheckIntervalSeconds;
    }

    public void setHealthCheckIntervalSeconds(int healthCheckIntervalSeconds) {
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
    }

    public Set<String> getHealthCheckComponents() {
        return healthCheckComponents;
    }

    public void setHealthCheckComponents(Set<String> healthCheckComponents) {
        this.healthCheckComponents = healthCheckComponents;
    }

    public boolean isLearningEnabled() {
        return learningEnabled;
    }

    public void setLearningEnabled(boolean learningEnabled) {
        this.learningEnabled = learningEnabled;
    }

    public boolean isBatchLearningEnabled() {
        return batchLearningEnabled;
    }

    public void setBatchLearningEnabled(boolean batchLearningEnabled) {
        this.batchLearningEnabled = batchLearningEnabled;
    }

    public int getLearningBatchSize() {
        return learningBatchSize;
    }

    public void setLearningBatchSize(int learningBatchSize) {
        this.learningBatchSize = learningBatchSize;
    }

    public int getLearningIntervalMinutes() {
        return learningIntervalMinutes;
    }

    public void setLearningIntervalMinutes(int learningIntervalMinutes) {
        this.learningIntervalMinutes = learningIntervalMinutes;
    }

    public boolean isExperimentalFeaturesEnabled() {
        return experimentalFeaturesEnabled;
    }

    public void setExperimentalFeaturesEnabled(boolean experimentalFeaturesEnabled) {
        this.experimentalFeaturesEnabled = experimentalFeaturesEnabled;
    }

    public Set<String> getEnabledExperimentalFeatures() {
        return enabledExperimentalFeatures;
    }

    public void setEnabledExperimentalFeatures(Set<String> enabledExperimentalFeatures) {
        this.enabledExperimentalFeatures = enabledExperimentalFeatures;
    }

    public boolean isAbTestingEnabled() {
        return abTestingEnabled;
    }

    public void setAbTestingEnabled(boolean abTestingEnabled) {
        this.abTestingEnabled = abTestingEnabled;
    }

    public Map<String, Double> getAbTestStrategies() {
        return abTestStrategies;
    }

    public void setAbTestStrategies(Map<String, Double> abTestStrategies) {
        this.abTestStrategies = abTestStrategies;
    }

    public boolean isDebugModeEnabled() {
        return debugModeEnabled;
    }

    public void setDebugModeEnabled(boolean debugModeEnabled) {
        this.debugModeEnabled = debugModeEnabled;
    }

    public boolean isRequestResponseLoggingEnabled() {
        return requestResponseLoggingEnabled;
    }

    public void setRequestResponseLoggingEnabled(boolean requestResponseLoggingEnabled) {
        this.requestResponseLoggingEnabled = requestResponseLoggingEnabled;
    }

    public boolean isPerformanceProfilingEnabled() {
        return performanceProfilingEnabled;
    }

    public void setPerformanceProfilingEnabled(boolean performanceProfilingEnabled) {
        this.performanceProfilingEnabled = performanceProfilingEnabled;
    }

    // Convenience methods
    public boolean isFeatureEnabled(String featureName) {
        return experimentalFeaturesEnabled && enabledExperimentalFeatures.contains(featureName);
    }

    public double getStrategyWeight(String strategy) {
        return strategyWeights.getOrDefault(strategy, 0.0);
    }

    @Override
    public String toString() {
        return "PluginDiscoveryConfig{" +
                "maxRecommendations=" + maxRecommendations +
                ", minConfidenceThreshold=" + minConfidenceThreshold +
                ", asyncProcessingEnabled=" + asyncProcessingEnabled +
                ", cachingEnabled=" + cachingEnabled +
                ", rateLimitingEnabled=" + rateLimitingEnabled +
                ", defaultStrategy='" + defaultStrategy + '\'' +
                '}';
    }
}