package api.config;

import api.model.CDNProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.util.*;

/**
 * Multi-provider CDN configuration
 * Supports AWS CloudFront, Cloudflare, and direct storage routing
 */
@Configuration
@ConfigurationProperties(prefix = "obvian.cdn")
@Validated
public class CDNConfiguration {
    
    /**
     * Primary CDN provider
     */
    @NotNull(message = "Primary CDN provider cannot be null")
    private CDNProvider primaryProvider = CDNProvider.CLOUDFLARE;
    
    /**
     * List of enabled CDN providers
     */
    @NotEmpty(message = "At least one CDN provider must be enabled")
    private List<CDNProvider> enabledProviders = Arrays.asList(
        CDNProvider.CLOUDFLARE, 
        CDNProvider.DIRECT_STORAGE
    );
    
    /**
     * Regional provider routing (region -> provider)
     */
    private Map<String, CDNProvider> regionalRouting = new HashMap<>();
    
    /**
     * Provider failover configuration
     */
    private Map<CDNProvider, List<CDNProvider>> failoverChains = new HashMap<>();
    
    /**
     * Load balancing strategy
     */
    private LoadBalancingStrategy loadBalancingStrategy = LoadBalancingStrategy.PERFORMANCE_BASED;
    
    /**
     * Whether to enable automatic failover
     */
    private boolean automaticFailoverEnabled = true;
    
    /**
     * Failover threshold (ms) - switch providers if latency exceeds this
     */
    @Positive(message = "Failover threshold must be positive")
    private Integer failoverThresholdMs = 5000; // 5 seconds
    
    /**
     * Health check interval in minutes
     */
    @Min(value = 1, message = "Health check interval must be at least 1 minute")
    private Integer healthCheckIntervalMinutes = 5;
    
    /**
     * Whether to enable health monitoring
     */
    private boolean healthMonitoringEnabled = true;
    
    /**
     * Provider weights for weighted load balancing (provider -> weight)
     */
    private Map<CDNProvider, Double> providerWeights = new HashMap<>();
    
    /**
     * Cache invalidation strategy
     */
    private CacheInvalidationStrategy cacheInvalidationStrategy = CacheInvalidationStrategy.IMMEDIATE_ALL_PROVIDERS;
    
    /**
     * Whether to enable intelligent routing based on request characteristics
     */
    private boolean intelligentRoutingEnabled = true;
    
    /**
     * Content type routing rules (content-type -> preferred provider)
     */
    private Map<String, CDNProvider> contentTypeRouting = new HashMap<>();
    
    /**
     * Geographic routing rules (country/region code -> provider)
     */
    private Map<String, CDNProvider> geographicRouting = new HashMap<>();
    
    /**
     * Provider-specific configurations
     */
    private CloudFlareProviderConfig cloudflareConfig = new CloudFlareProviderConfig();
    private CloudFrontProviderConfig cloudfrontConfig = new CloudFrontProviderConfig();
    private DirectStorageProviderConfig directStorageConfig = new DirectStorageProviderConfig();
    
    /**
     * Performance monitoring configuration
     */
    private boolean performanceMonitoringEnabled = true;
    private Integer performanceMetricsRetentionDays = 30;
    
    /**
     * Cost optimization settings
     */
    private boolean costOptimizationEnabled = true;
    private CostOptimizationStrategy costOptimizationStrategy = CostOptimizationStrategy.MINIMIZE_EGRESS;
    
    /**
     * Request routing configuration
     */
    private RequestRoutingConfig requestRouting = new RequestRoutingConfig();
    
    public CDNConfiguration() {
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Initialize regional routing
        regionalRouting.put("US", CDNProvider.CLOUDFLARE);
        regionalRouting.put("EU", CDNProvider.CLOUDFLARE);
        regionalRouting.put("APAC", CDNProvider.CLOUDFLARE);
        regionalRouting.put("DEFAULT", CDNProvider.CLOUDFLARE);
        
        // Initialize failover chains
        failoverChains.put(CDNProvider.CLOUDFLARE, Arrays.asList(CDNProvider.DIRECT_STORAGE));
        failoverChains.put(CDNProvider.AWS_CLOUDFRONT, Arrays.asList(CDNProvider.CLOUDFLARE, CDNProvider.DIRECT_STORAGE));
        failoverChains.put(CDNProvider.DIRECT_STORAGE, Collections.emptyList());
        
        // Initialize provider weights
        providerWeights.put(CDNProvider.CLOUDFLARE, 0.7);
        providerWeights.put(CDNProvider.AWS_CLOUDFRONT, 0.2);
        providerWeights.put(CDNProvider.DIRECT_STORAGE, 0.1);
        
        // Initialize content type routing
        contentTypeRouting.put("application/java-archive", CDNProvider.CLOUDFLARE);
        contentTypeRouting.put("application/zip", CDNProvider.CLOUDFLARE);
        contentTypeRouting.put("image/*", CDNProvider.CLOUDFLARE);
        contentTypeRouting.put("application/json", CDNProvider.DIRECT_STORAGE);
        
        // Initialize geographic routing
        geographicRouting.put("CN", CDNProvider.DIRECT_STORAGE); // China - avoid international CDNs
        geographicRouting.put("RU", CDNProvider.DIRECT_STORAGE); // Russia - regulatory considerations
    }
    
    // Getters and setters
    
    public CDNProvider getPrimaryProvider() {
        return primaryProvider;
    }
    
    public void setPrimaryProvider(CDNProvider primaryProvider) {
        this.primaryProvider = primaryProvider;
    }
    
    public List<CDNProvider> getEnabledProviders() {
        return enabledProviders;
    }
    
    public void setEnabledProviders(List<CDNProvider> enabledProviders) {
        this.enabledProviders = enabledProviders;
    }
    
    public Map<String, CDNProvider> getRegionalRouting() {
        return regionalRouting;
    }
    
    public void setRegionalRouting(Map<String, CDNProvider> regionalRouting) {
        this.regionalRouting = regionalRouting;
    }
    
    public Map<CDNProvider, List<CDNProvider>> getFailoverChains() {
        return failoverChains;
    }
    
    public void setFailoverChains(Map<CDNProvider, List<CDNProvider>> failoverChains) {
        this.failoverChains = failoverChains;
    }
    
    public LoadBalancingStrategy getLoadBalancingStrategy() {
        return loadBalancingStrategy;
    }
    
    public void setLoadBalancingStrategy(LoadBalancingStrategy loadBalancingStrategy) {
        this.loadBalancingStrategy = loadBalancingStrategy;
    }
    
    public boolean isAutomaticFailoverEnabled() {
        return automaticFailoverEnabled;
    }
    
    public void setAutomaticFailoverEnabled(boolean automaticFailoverEnabled) {
        this.automaticFailoverEnabled = automaticFailoverEnabled;
    }
    
    public Integer getFailoverThresholdMs() {
        return failoverThresholdMs;
    }
    
    public void setFailoverThresholdMs(Integer failoverThresholdMs) {
        this.failoverThresholdMs = failoverThresholdMs;
    }
    
    public Integer getHealthCheckIntervalMinutes() {
        return healthCheckIntervalMinutes;
    }
    
    public void setHealthCheckIntervalMinutes(Integer healthCheckIntervalMinutes) {
        this.healthCheckIntervalMinutes = healthCheckIntervalMinutes;
    }
    
    public boolean isHealthMonitoringEnabled() {
        return healthMonitoringEnabled;
    }
    
    public void setHealthMonitoringEnabled(boolean healthMonitoringEnabled) {
        this.healthMonitoringEnabled = healthMonitoringEnabled;
    }
    
    public Map<CDNProvider, Double> getProviderWeights() {
        return providerWeights;
    }
    
    public void setProviderWeights(Map<CDNProvider, Double> providerWeights) {
        this.providerWeights = providerWeights;
    }
    
    public CacheInvalidationStrategy getCacheInvalidationStrategy() {
        return cacheInvalidationStrategy;
    }
    
    public void setCacheInvalidationStrategy(CacheInvalidationStrategy cacheInvalidationStrategy) {
        this.cacheInvalidationStrategy = cacheInvalidationStrategy;
    }
    
    public boolean isIntelligentRoutingEnabled() {
        return intelligentRoutingEnabled;
    }
    
    public void setIntelligentRoutingEnabled(boolean intelligentRoutingEnabled) {
        this.intelligentRoutingEnabled = intelligentRoutingEnabled;
    }
    
    public Map<String, CDNProvider> getContentTypeRouting() {
        return contentTypeRouting;
    }
    
    public void setContentTypeRouting(Map<String, CDNProvider> contentTypeRouting) {
        this.contentTypeRouting = contentTypeRouting;
    }
    
    public Map<String, CDNProvider> getGeographicRouting() {
        return geographicRouting;
    }
    
    public void setGeographicRouting(Map<String, CDNProvider> geographicRouting) {
        this.geographicRouting = geographicRouting;
    }
    
    public CloudFlareProviderConfig getCloudflareConfig() {
        return cloudflareConfig;
    }
    
    public void setCloudflareConfig(CloudFlareProviderConfig cloudflareConfig) {
        this.cloudflareConfig = cloudflareConfig;
    }
    
    public CloudFrontProviderConfig getCloudfrontConfig() {
        return cloudfrontConfig;
    }
    
    public void setCloudfrontConfig(CloudFrontProviderConfig cloudfrontConfig) {
        this.cloudfrontConfig = cloudfrontConfig;
    }
    
    public DirectStorageProviderConfig getDirectStorageConfig() {
        return directStorageConfig;
    }
    
    public void setDirectStorageConfig(DirectStorageProviderConfig directStorageConfig) {
        this.directStorageConfig = directStorageConfig;
    }
    
    public boolean isPerformanceMonitoringEnabled() {
        return performanceMonitoringEnabled;
    }
    
    public void setPerformanceMonitoringEnabled(boolean performanceMonitoringEnabled) {
        this.performanceMonitoringEnabled = performanceMonitoringEnabled;
    }
    
    public Integer getPerformanceMetricsRetentionDays() {
        return performanceMetricsRetentionDays;
    }
    
    public void setPerformanceMetricsRetentionDays(Integer performanceMetricsRetentionDays) {
        this.performanceMetricsRetentionDays = performanceMetricsRetentionDays;
    }
    
    public boolean isCostOptimizationEnabled() {
        return costOptimizationEnabled;
    }
    
    public void setCostOptimizationEnabled(boolean costOptimizationEnabled) {
        this.costOptimizationEnabled = costOptimizationEnabled;
    }
    
    public CostOptimizationStrategy getCostOptimizationStrategy() {
        return costOptimizationStrategy;
    }
    
    public void setCostOptimizationStrategy(CostOptimizationStrategy costOptimizationStrategy) {
        this.costOptimizationStrategy = costOptimizationStrategy;
    }
    
    public RequestRoutingConfig getRequestRouting() {
        return requestRouting;
    }
    
    public void setRequestRouting(RequestRoutingConfig requestRouting) {
        this.requestRouting = requestRouting;
    }
    
    // Utility methods
    
    /**
     * Get preferred provider for a region
     */
    public CDNProvider getProviderForRegion(String region) {
        return regionalRouting.getOrDefault(region, 
               regionalRouting.getOrDefault("DEFAULT", primaryProvider));
    }
    
    /**
     * Get preferred provider for content type
     */
    public CDNProvider getProviderForContentType(String contentType) {
        if (contentType == null) return primaryProvider;
        
        // Check exact match first
        CDNProvider provider = contentTypeRouting.get(contentType);
        if (provider != null) return provider;
        
        // Check wildcard matches
        for (Map.Entry<String, CDNProvider> entry : contentTypeRouting.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.endsWith("/*") && contentType.startsWith(pattern.substring(0, pattern.length() - 2))) {
                return entry.getValue();
            }
        }
        
        return primaryProvider;
    }
    
    /**
     * Get preferred provider for country/region
     */
    public CDNProvider getProviderForGeography(String countryCode) {
        return geographicRouting.getOrDefault(countryCode, primaryProvider);
    }
    
    /**
     * Get failover chain for a provider
     */
    public List<CDNProvider> getFailoverChain(CDNProvider provider) {
        return failoverChains.getOrDefault(provider, Collections.emptyList());
    }
    
    /**
     * Get weight for a provider
     */
    public double getProviderWeight(CDNProvider provider) {
        return providerWeights.getOrDefault(provider, 1.0);
    }
    
    /**
     * Validate configuration
     */
    public void validate() {
        if (primaryProvider == null) {
            throw new IllegalArgumentException("Primary CDN provider cannot be null");
        }
        if (enabledProviders == null || enabledProviders.isEmpty()) {
            throw new IllegalArgumentException("At least one CDN provider must be enabled");
        }
        if (!enabledProviders.contains(primaryProvider)) {
            throw new IllegalArgumentException("Primary provider must be in enabled providers list");
        }
        
        // Validate weights sum to reasonable value
        double totalWeight = providerWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight <= 0 || totalWeight > 2.0) {
            throw new IllegalArgumentException("Provider weights must sum to a positive value <= 2.0");
        }
    }
    
    @Override
    public String toString() {
        return "CDNConfiguration{" +
                "primaryProvider=" + primaryProvider +
                ", enabledProviders=" + enabledProviders +
                ", loadBalancingStrategy=" + loadBalancingStrategy +
                ", automaticFailoverEnabled=" + automaticFailoverEnabled +
                ", intelligentRoutingEnabled=" + intelligentRoutingEnabled +
                '}';
    }
    
    // Enums and inner classes
    
    public enum LoadBalancingStrategy {
        ROUND_ROBIN,
        WEIGHTED_ROUND_ROBIN,
        PERFORMANCE_BASED,
        COST_OPTIMIZED,
        GEOGRAPHIC_PROXIMITY
    }
    
    public enum CacheInvalidationStrategy {
        IMMEDIATE_ALL_PROVIDERS,
        IMMEDIATE_PRIMARY_ONLY,
        LAZY_INVALIDATION,
        SCHEDULED_BATCH
    }
    
    public enum CostOptimizationStrategy {
        MINIMIZE_EGRESS,
        MINIMIZE_REQUESTS,
        BALANCED_COST_PERFORMANCE
    }
    
    public static class CloudFlareProviderConfig {
        private boolean freeTierOptimizations = true;
        private boolean imageOptimizationEnabled = true;
        private boolean brotliCompressionEnabled = true;
        private boolean minificationEnabled = true;
        private int defaultCacheTtl = 86400; // 24 hours
        
        // Getters and setters
        
        public boolean isFreeTierOptimizations() {
            return freeTierOptimizations;
        }
        
        public void setFreeTierOptimizations(boolean freeTierOptimizations) {
            this.freeTierOptimizations = freeTierOptimizations;
        }
        
        public boolean isImageOptimizationEnabled() {
            return imageOptimizationEnabled;
        }
        
        public void setImageOptimizationEnabled(boolean imageOptimizationEnabled) {
            this.imageOptimizationEnabled = imageOptimizationEnabled;
        }
        
        public boolean isBrotliCompressionEnabled() {
            return brotliCompressionEnabled;
        }
        
        public void setBrotliCompressionEnabled(boolean brotliCompressionEnabled) {
            this.brotliCompressionEnabled = brotliCompressionEnabled;
        }
        
        public boolean isMinificationEnabled() {
            return minificationEnabled;
        }
        
        public void setMinificationEnabled(boolean minificationEnabled) {
            this.minificationEnabled = minificationEnabled;
        }
        
        public int getDefaultCacheTtl() {
            return defaultCacheTtl;
        }
        
        public void setDefaultCacheTtl(int defaultCacheTtl) {
            this.defaultCacheTtl = defaultCacheTtl;
        }
    }
    
    public static class CloudFrontProviderConfig {
        private String distributionId;
        private boolean gzipCompressionEnabled = true;
        private boolean originShieldEnabled = false;
        private int defaultCacheTtl = 86400; // 24 hours
        private String priceClass = "PriceClass_All";
        
        // Getters and setters
        
        public String getDistributionId() {
            return distributionId;
        }
        
        public void setDistributionId(String distributionId) {
            this.distributionId = distributionId;
        }
        
        public boolean isGzipCompressionEnabled() {
            return gzipCompressionEnabled;
        }
        
        public void setGzipCompressionEnabled(boolean gzipCompressionEnabled) {
            this.gzipCompressionEnabled = gzipCompressionEnabled;
        }
        
        public boolean isOriginShieldEnabled() {
            return originShieldEnabled;
        }
        
        public void setOriginShieldEnabled(boolean originShieldEnabled) {
            this.originShieldEnabled = originShieldEnabled;
        }
        
        public int getDefaultCacheTtl() {
            return defaultCacheTtl;
        }
        
        public void setDefaultCacheTtl(int defaultCacheTtl) {
            this.defaultCacheTtl = defaultCacheTtl;
        }
        
        public String getPriceClass() {
            return priceClass;
        }
        
        public void setPriceClass(String priceClass) {
            this.priceClass = priceClass;
        }
    }
    
    public static class DirectStorageProviderConfig {
        private boolean signedUrlsEnabled = true;
        private int signedUrlExpirationMinutes = 60;
        private boolean compressionEnabled = false;
        private String corsPolicy = "permissive";
        
        // Getters and setters
        
        public boolean isSignedUrlsEnabled() {
            return signedUrlsEnabled;
        }
        
        public void setSignedUrlsEnabled(boolean signedUrlsEnabled) {
            this.signedUrlsEnabled = signedUrlsEnabled;
        }
        
        public int getSignedUrlExpirationMinutes() {
            return signedUrlExpirationMinutes;
        }
        
        public void setSignedUrlExpirationMinutes(int signedUrlExpirationMinutes) {
            this.signedUrlExpirationMinutes = signedUrlExpirationMinutes;
        }
        
        public boolean isCompressionEnabled() {
            return compressionEnabled;
        }
        
        public void setCompressionEnabled(boolean compressionEnabled) {
            this.compressionEnabled = compressionEnabled;
        }
        
        public String getCorsPolicy() {
            return corsPolicy;
        }
        
        public void setCorsPolicy(String corsPolicy) {
            this.corsPolicy = corsPolicy;
        }
    }
    
    public static class RequestRoutingConfig {
        private boolean enableRequestSizeRouting = true;
        private long largeFileThreshold = 100 * 1024 * 1024; // 100MB
        private CDNProvider largeFileProvider = CDNProvider.CLOUDFLARE;
        
        private boolean enableLatencyBasedRouting = true;
        private int latencyThresholdMs = 1000;
        
        private boolean enableBandwidthOptimization = true;
        private boolean enableCostRouting = true;
        
        // Getters and setters
        
        public boolean isEnableRequestSizeRouting() {
            return enableRequestSizeRouting;
        }
        
        public void setEnableRequestSizeRouting(boolean enableRequestSizeRouting) {
            this.enableRequestSizeRouting = enableRequestSizeRouting;
        }
        
        public long getLargeFileThreshold() {
            return largeFileThreshold;
        }
        
        public void setLargeFileThreshold(long largeFileThreshold) {
            this.largeFileThreshold = largeFileThreshold;
        }
        
        public CDNProvider getLargeFileProvider() {
            return largeFileProvider;
        }
        
        public void setLargeFileProvider(CDNProvider largeFileProvider) {
            this.largeFileProvider = largeFileProvider;
        }
        
        public boolean isEnableLatencyBasedRouting() {
            return enableLatencyBasedRouting;
        }
        
        public void setEnableLatencyBasedRouting(boolean enableLatencyBasedRouting) {
            this.enableLatencyBasedRouting = enableLatencyBasedRouting;
        }
        
        public int getLatencyThresholdMs() {
            return latencyThresholdMs;
        }
        
        public void setLatencyThresholdMs(int latencyThresholdMs) {
            this.latencyThresholdMs = latencyThresholdMs;
        }
        
        public boolean isEnableBandwidthOptimization() {
            return enableBandwidthOptimization;
        }
        
        public void setEnableBandwidthOptimization(boolean enableBandwidthOptimization) {
            this.enableBandwidthOptimization = enableBandwidthOptimization;
        }
        
        public boolean isEnableCostRouting() {
            return enableCostRouting;
        }
        
        public void setEnableCostRouting(boolean enableCostRouting) {
            this.enableCostRouting = enableCostRouting;
        }
    }
}