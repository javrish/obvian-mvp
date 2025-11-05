package api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Comprehensive Cloudflare configuration for CDN and R2 storage
 * Supports free tier optimization and enterprise features
 */
@Configuration
@ConfigurationProperties(prefix = "obvian.cloudflare")
@Validated
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "obvian.cloudflare.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class CloudflareConfiguration {
    
    // Cloudflare API Configuration
    
    /**
     * Cloudflare API token with necessary permissions
     */
    @NotBlank(message = "API token cannot be blank")
    private String apiToken = "dev-api-token";
    
    /**
     * Cloudflare zone ID for the domain
     */
    @NotBlank(message = "Zone ID cannot be blank")
    private String zoneId = "dev-zone-id";
    
    /**
     * Cloudflare account ID
     */
    @NotBlank(message = "Account ID cannot be blank")
    private String accountId = "dev-account-id";
    
    // CDN Configuration
    
    /**
     * CDN domain for serving content
     */
    private String cdnDomain = "cdn.obvian.com";
    
    /**
     * Whether CDN is enabled
     */
    private boolean cdnEnabled = true;
    
    /**
     * Cache TTL in seconds for different content types
     */
    private Map<String, Integer> cacheTtl = new HashMap<>();
    
    /**
     * Whether to enable image optimization
     */
    private boolean imageOptimizationEnabled = true;
    
    /**
     * Default image quality (1-100)
     */
    @Min(value = 1, message = "Image quality must be at least 1")
    @Max(value = 100, message = "Image quality cannot exceed 100")
    private Integer imageQuality = 85;
    
    /**
     * Supported image formats for optimization
     */
    private List<String> supportedImageFormats = List.of("webp", "avif", "jpeg", "png");
    
    /**
     * Maximum image dimensions for optimization
     */
    @Positive(message = "Max image width must be positive")
    private Integer maxImageWidth = 2048;
    
    @Positive(message = "Max image height must be positive")
    private Integer maxImageHeight = 2048;
    
    // R2 Storage Configuration
    
    /**
     * R2 bucket name for plugin storage
     */
    private String r2BucketName = "obvian-plugins";
    
    /**
     * R2 endpoint URL
     */
    private String r2Endpoint = "https://dev-account-id.r2.cloudflarestorage.com";
    
    /**
     * R2 access key ID
     */
    private String r2AccessKeyId = "dev-r2-access-key";
    
    /**
     * R2 secret access key
     */
    private String r2SecretAccessKey = "dev-r2-secret-key";
    
    /**
     * Whether to use R2 for primary storage
     */
    private boolean r2Enabled = true;
    
    /**
     * R2 storage class
     */
    private String r2StorageClass = "STANDARD";
    
    /**
     * Whether server-side encryption is enabled for R2
     */
    private boolean encryptionEnabled = true;
    
    // Performance and Optimization
    
    /**
     * Whether to enable Cloudflare's free tier optimizations
     */
    private boolean freeTierOptimizations = true;
    
    /**
     * Whether to enable Brotli compression
     */
    private boolean brotliCompressionEnabled = true;
    
    /**
     * Whether to enable minification
     */
    private boolean minificationEnabled = true;
    
    /**
     * Whether to enable mobile optimization
     */
    private boolean mobileOptimizationEnabled = true;
    
    /**
     * Whether to enable browser cache optimization
     */
    private boolean browserCacheOptimizationEnabled = true;
    
    /**
     * Browser cache TTL in seconds
     */
    @Positive(message = "Browser cache TTL must be positive")
    private Integer browserCacheTtl = 86400; // 24 hours
    
    // Security Configuration
    
    /**
     * Whether to enable DDoS protection
     */
    private boolean ddosProtectionEnabled = true;
    
    /**
     * Whether to enable Web Application Firewall
     */
    private boolean wafEnabled = true;
    
    /**
     * Whether to enforce HTTPS
     */
    private boolean httpsEnforcementEnabled = true;
    
    /**
     * SSL/TLS mode
     */
    private String sslMode = "full_strict";
    
    /**
     * Whether to enable HSTS
     */
    private boolean hstsEnabled = true;
    
    /**
     * HSTS max age in seconds
     */
    @Positive(message = "HSTS max age must be positive")
    private Integer hstsMaxAge = 31536000; // 1 year
    
    // API and Network Configuration
    
    /**
     * API request timeout in milliseconds
     */
    @Min(value = 1000, message = "API timeout must be at least 1000ms")
    private Integer apiTimeoutMs = 30000; // 30 seconds
    
    /**
     * Connection timeout in milliseconds
     */
    @Min(value = 1000, message = "Connection timeout must be at least 1000ms")
    private Integer connectionTimeoutMs = 10000; // 10 seconds
    
    /**
     * Read timeout in milliseconds
     */
    @Min(value = 1000, message = "Read timeout must be at least 1000ms")
    private Integer readTimeoutMs = 60000; // 60 seconds
    
    /**
     * Maximum retry attempts for API calls
     */
    @Min(value = 0, message = "Max retries cannot be negative")
    @Max(value = 5, message = "Max retries cannot exceed 5")
    private Integer maxRetries = 3;
    
    /**
     * Retry delay in milliseconds
     */
    @Positive(message = "Retry delay must be positive")
    private Integer retryDelayMs = 1000;
    
    // File Upload Configuration
    
    /**
     * Maximum file size for uploads (bytes)
     */
    @Positive(message = "Max file size must be positive")
    private Long maxFileSize = 100L * 1024 * 1024; // 100MB
    
    /**
     * Signed URL expiration in minutes
     */
    @Min(value = 1, message = "Signed URL expiration must be at least 1 minute")
    @Max(value = 1440, message = "Signed URL expiration cannot exceed 24 hours")
    private Integer signedUrlExpirationMinutes = 60; // 1 hour
    
    /**
     * Whether to enable multipart uploads
     */
    private boolean multipartUploadEnabled = true;
    
    /**
     * Multipart upload threshold in bytes
     */
    @Positive(message = "Multipart threshold must be positive")
    private Long multipartThreshold = 64L * 1024 * 1024; // 64MB
    
    /**
     * Multipart upload part size in bytes
     */
    @Positive(message = "Multipart part size must be positive")
    private Long multipartPartSize = 16L * 1024 * 1024; // 16MB
    
    // Monitoring and Analytics
    
    /**
     * Whether to enable analytics collection
     */
    private boolean analyticsEnabled = true;
    
    /**
     * Analytics data retention in days
     */
    @Min(value = 1, message = "Analytics retention must be at least 1 day")
    private Integer analyticsRetentionDays = 90;
    
    /**
     * Whether to enable detailed logging
     */
    private boolean detailedLoggingEnabled = false;
    
    /**
     * Log retention in days
     */
    @Min(value = 1, message = "Log retention must be at least 1 day")
    private Integer logRetentionDays = 30;
    
    // Cache Configuration
    
    /**
     * Whether caching is enabled
     */
    private boolean cacheEnabled = true;
    
    /**
     * Default cache level
     */
    private String cacheLevel = "aggressive";
    
    /**
     * Cache everything page rule
     */
    private boolean cacheEverythingEnabled = true;
    
    /**
     * Edge cache TTL in seconds
     */
    @Positive(message = "Edge cache TTL must be positive")
    private Integer edgeCacheTtl = 86400; // 24 hours
    
    /**
     * Custom cache rules
     */
    private List<CacheRule> cacheRules = new ArrayList<>();
    
    // Workers and Edge Computing
    
    /**
     * Whether Cloudflare Workers are enabled
     */
    private boolean workersEnabled = false;
    
    /**
     * Worker script name for custom logic
     */
    private String workerScriptName;
    
    /**
     * Worker routes for different patterns
     */
    private List<String> workerRoutes = new ArrayList<>();
    
    // Regional and Geographic Configuration
    
    /**
     * Preferred data center regions
     */
    private List<String> preferredRegions = new ArrayList<>();
    
    /**
     * Whether to enable Argo Smart Routing
     */
    private boolean argoSmartRoutingEnabled = false;
    
    /**
     * Whether to enable Load Balancing
     */
    private boolean loadBalancingEnabled = false;
    
    // Default constructor
    public CloudflareConfiguration() {
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        // Initialize default cache TTL values
        cacheTtl.put("jar", 86400);        // 24 hours for JAR files
        cacheTtl.put("image", 604800);     // 7 days for images  
        cacheTtl.put("css", 86400);        // 24 hours for CSS
        cacheTtl.put("js", 86400);         // 24 hours for JavaScript
        cacheTtl.put("html", 3600);        // 1 hour for HTML
        cacheTtl.put("api", 300);          // 5 minutes for API responses
        
        // Initialize default cache rules
        cacheRules.add(new CacheRule("*.jar", 86400, true));
        cacheRules.add(new CacheRule("*.zip", 86400, true));
        cacheRules.add(new CacheRule("/api/plugins/*/download", 3600, false));
        
        // Initialize preferred regions (global coverage)
        preferredRegions.add("US");
        preferredRegions.add("EU");
        preferredRegions.add("APAC");
    }
    
    // Getters and setters
    
    public String getApiToken() {
        return apiToken;
    }
    
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
    
    public String getZoneId() {
        return zoneId;
    }
    
    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
    
    public String getAccountId() {
        return accountId;
    }
    
    public void setAccountId(String accountId) {
        this.accountId = accountId;
        // Update R2 endpoint with account ID
        if (accountId != null) {
            this.r2Endpoint = String.format("https://%s.r2.cloudflarestorage.com", accountId);
        }
    }
    
    public String getCdnDomain() {
        return cdnDomain;
    }
    
    public void setCdnDomain(String cdnDomain) {
        this.cdnDomain = cdnDomain;
    }
    
    public boolean isCdnEnabled() {
        return cdnEnabled;
    }
    
    public void setCdnEnabled(boolean cdnEnabled) {
        this.cdnEnabled = cdnEnabled;
    }
    
    public Map<String, Integer> getCacheTtl() {
        return cacheTtl;
    }
    
    public void setCacheTtl(Map<String, Integer> cacheTtl) {
        this.cacheTtl = cacheTtl;
    }
    
    public boolean isImageOptimizationEnabled() {
        return imageOptimizationEnabled;
    }
    
    public void setImageOptimizationEnabled(boolean imageOptimizationEnabled) {
        this.imageOptimizationEnabled = imageOptimizationEnabled;
    }
    
    public Integer getImageQuality() {
        return imageQuality;
    }
    
    public void setImageQuality(Integer imageQuality) {
        this.imageQuality = imageQuality;
    }
    
    public List<String> getSupportedImageFormats() {
        return supportedImageFormats;
    }
    
    public void setSupportedImageFormats(List<String> supportedImageFormats) {
        this.supportedImageFormats = supportedImageFormats;
    }
    
    public Integer getMaxImageWidth() {
        return maxImageWidth;
    }
    
    public void setMaxImageWidth(Integer maxImageWidth) {
        this.maxImageWidth = maxImageWidth;
    }
    
    public Integer getMaxImageHeight() {
        return maxImageHeight;
    }
    
    public void setMaxImageHeight(Integer maxImageHeight) {
        this.maxImageHeight = maxImageHeight;
    }
    
    public String getR2BucketName() {
        return r2BucketName;
    }
    
    public void setR2BucketName(String r2BucketName) {
        this.r2BucketName = r2BucketName;
    }
    
    public String getR2Endpoint() {
        return r2Endpoint;
    }
    
    public void setR2Endpoint(String r2Endpoint) {
        this.r2Endpoint = r2Endpoint;
    }
    
    public String getR2AccessKeyId() {
        return r2AccessKeyId;
    }
    
    public void setR2AccessKeyId(String r2AccessKeyId) {
        this.r2AccessKeyId = r2AccessKeyId;
    }
    
    public String getR2SecretAccessKey() {
        return r2SecretAccessKey;
    }
    
    public void setR2SecretAccessKey(String r2SecretAccessKey) {
        this.r2SecretAccessKey = r2SecretAccessKey;
    }
    
    public boolean isR2Enabled() {
        return r2Enabled;
    }
    
    public void setR2Enabled(boolean r2Enabled) {
        this.r2Enabled = r2Enabled;
    }
    
    public String getR2StorageClass() {
        return r2StorageClass;
    }
    
    public void setR2StorageClass(String r2StorageClass) {
        this.r2StorageClass = r2StorageClass;
    }
    
    public String getStorageClass() {
        return r2StorageClass;
    }
    
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
    
    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }
    
    public boolean isFreeTierOptimizations() {
        return freeTierOptimizations;
    }
    
    public void setFreeTierOptimizations(boolean freeTierOptimizations) {
        this.freeTierOptimizations = freeTierOptimizations;
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
    
    public boolean isMobileOptimizationEnabled() {
        return mobileOptimizationEnabled;
    }
    
    public void setMobileOptimizationEnabled(boolean mobileOptimizationEnabled) {
        this.mobileOptimizationEnabled = mobileOptimizationEnabled;
    }
    
    public boolean isBrowserCacheOptimizationEnabled() {
        return browserCacheOptimizationEnabled;
    }
    
    public void setBrowserCacheOptimizationEnabled(boolean browserCacheOptimizationEnabled) {
        this.browserCacheOptimizationEnabled = browserCacheOptimizationEnabled;
    }
    
    public Integer getBrowserCacheTtl() {
        return browserCacheTtl;
    }
    
    public void setBrowserCacheTtl(Integer browserCacheTtl) {
        this.browserCacheTtl = browserCacheTtl;
    }
    
    public boolean isDdosProtectionEnabled() {
        return ddosProtectionEnabled;
    }
    
    public void setDdosProtectionEnabled(boolean ddosProtectionEnabled) {
        this.ddosProtectionEnabled = ddosProtectionEnabled;
    }
    
    public boolean isWafEnabled() {
        return wafEnabled;
    }
    
    public void setWafEnabled(boolean wafEnabled) {
        this.wafEnabled = wafEnabled;
    }
    
    public boolean isHttpsEnforcementEnabled() {
        return httpsEnforcementEnabled;
    }
    
    public void setHttpsEnforcementEnabled(boolean httpsEnforcementEnabled) {
        this.httpsEnforcementEnabled = httpsEnforcementEnabled;
    }
    
    public String getSslMode() {
        return sslMode;
    }
    
    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }
    
    public boolean isHstsEnabled() {
        return hstsEnabled;
    }
    
    public void setHstsEnabled(boolean hstsEnabled) {
        this.hstsEnabled = hstsEnabled;
    }
    
    public Integer getHstsMaxAge() {
        return hstsMaxAge;
    }
    
    public void setHstsMaxAge(Integer hstsMaxAge) {
        this.hstsMaxAge = hstsMaxAge;
    }
    
    public Integer getApiTimeoutMs() {
        return apiTimeoutMs;
    }
    
    public void setApiTimeoutMs(Integer apiTimeoutMs) {
        this.apiTimeoutMs = apiTimeoutMs;
    }
    
    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }
    
    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }
    
    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }
    
    public void setReadTimeoutMs(Integer readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public Integer getRetryDelayMs() {
        return retryDelayMs;
    }
    
    public void setRetryDelayMs(Integer retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
    
    public Long getMaxFileSize() {
        return maxFileSize;
    }
    
    public void setMaxFileSize(Long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    public Integer getSignedUrlExpirationMinutes() {
        return signedUrlExpirationMinutes;
    }
    
    public void setSignedUrlExpirationMinutes(Integer signedUrlExpirationMinutes) {
        this.signedUrlExpirationMinutes = signedUrlExpirationMinutes;
    }
    
    public boolean isMultipartUploadEnabled() {
        return multipartUploadEnabled;
    }
    
    public void setMultipartUploadEnabled(boolean multipartUploadEnabled) {
        this.multipartUploadEnabled = multipartUploadEnabled;
    }
    
    public Long getMultipartThreshold() {
        return multipartThreshold;
    }
    
    public void setMultipartThreshold(Long multipartThreshold) {
        this.multipartThreshold = multipartThreshold;
    }
    
    public Long getMultipartPartSize() {
        return multipartPartSize;
    }
    
    public void setMultipartPartSize(Long multipartPartSize) {
        this.multipartPartSize = multipartPartSize;
    }
    
    public boolean isAnalyticsEnabled() {
        return analyticsEnabled;
    }
    
    public void setAnalyticsEnabled(boolean analyticsEnabled) {
        this.analyticsEnabled = analyticsEnabled;
    }
    
    public Integer getAnalyticsRetentionDays() {
        return analyticsRetentionDays;
    }
    
    public void setAnalyticsRetentionDays(Integer analyticsRetentionDays) {
        this.analyticsRetentionDays = analyticsRetentionDays;
    }
    
    public boolean isDetailedLoggingEnabled() {
        return detailedLoggingEnabled;
    }
    
    public void setDetailedLoggingEnabled(boolean detailedLoggingEnabled) {
        this.detailedLoggingEnabled = detailedLoggingEnabled;
    }
    
    public Integer getLogRetentionDays() {
        return logRetentionDays;
    }
    
    public void setLogRetentionDays(Integer logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }
    
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }
    
    public String getCacheLevel() {
        return cacheLevel;
    }
    
    public void setCacheLevel(String cacheLevel) {
        this.cacheLevel = cacheLevel;
    }
    
    public boolean isCacheEverythingEnabled() {
        return cacheEverythingEnabled;
    }
    
    public void setCacheEverythingEnabled(boolean cacheEverythingEnabled) {
        this.cacheEverythingEnabled = cacheEverythingEnabled;
    }
    
    public Integer getEdgeCacheTtl() {
        return edgeCacheTtl;
    }
    
    public void setEdgeCacheTtl(Integer edgeCacheTtl) {
        this.edgeCacheTtl = edgeCacheTtl;
    }
    
    public List<CacheRule> getCacheRules() {
        return cacheRules;
    }
    
    public void setCacheRules(List<CacheRule> cacheRules) {
        this.cacheRules = cacheRules;
    }
    
    public boolean isWorkersEnabled() {
        return workersEnabled;
    }
    
    public void setWorkersEnabled(boolean workersEnabled) {
        this.workersEnabled = workersEnabled;
    }
    
    public String getWorkerScriptName() {
        return workerScriptName;
    }
    
    public void setWorkerScriptName(String workerScriptName) {
        this.workerScriptName = workerScriptName;
    }
    
    public List<String> getWorkerRoutes() {
        return workerRoutes;
    }
    
    public void setWorkerRoutes(List<String> workerRoutes) {
        this.workerRoutes = workerRoutes;
    }
    
    public List<String> getPreferredRegions() {
        return preferredRegions;
    }
    
    public void setPreferredRegions(List<String> preferredRegions) {
        this.preferredRegions = preferredRegions;
    }
    
    public boolean isArgoSmartRoutingEnabled() {
        return argoSmartRoutingEnabled;
    }
    
    public void setArgoSmartRoutingEnabled(boolean argoSmartRoutingEnabled) {
        this.argoSmartRoutingEnabled = argoSmartRoutingEnabled;
    }
    
    public boolean isLoadBalancingEnabled() {
        return loadBalancingEnabled;
    }
    
    public void setLoadBalancingEnabled(boolean loadBalancingEnabled) {
        this.loadBalancingEnabled = loadBalancingEnabled;
    }
    
    // Utility methods
    
    /**
     * Get cache TTL for specific content type
     */
    public int getCacheTtlForType(String contentType) {
        if (contentType == null) return 3600; // Default 1 hour
        
        String type = contentType.toLowerCase();
        if (type.contains("java-archive") || type.contains("zip")) {
            return cacheTtl.getOrDefault("jar", 86400);
        } else if (type.contains("image")) {
            return cacheTtl.getOrDefault("image", 604800);
        } else if (type.contains("css")) {
            return cacheTtl.getOrDefault("css", 86400);
        } else if (type.contains("javascript")) {
            return cacheTtl.getOrDefault("js", 86400);
        } else if (type.contains("html")) {
            return cacheTtl.getOrDefault("html", 3600);
        } else {
            return cacheTtl.getOrDefault("default", 3600);
        }
    }
    
    /**
     * Check if image format is supported for optimization
     */
    public boolean isImageFormatSupported(String format) {
        return format != null && supportedImageFormats.contains(format.toLowerCase());
    }
    
    /**
     * Validate image dimensions
     */
    public boolean areImageDimensionsValid(int width, int height) {
        return width > 0 && height > 0 && 
               width <= maxImageWidth && height <= maxImageHeight;
    }
    
    /**
     * Get human-readable max file size
     */
    public String getMaxFileSizeHumanReadable() {
        return formatBytes(maxFileSize);
    }
    
    /**
     * Check if file size is within limits
     */
    public boolean isFileSizeAllowed(long fileSize) {
        return fileSize > 0 && fileSize <= maxFileSize;
    }
    
    /**
     * Should use multipart upload for given file size
     */
    public boolean shouldUseMultipartUpload(long fileSize) {
        return multipartUploadEnabled && fileSize >= multipartThreshold;
    }
    
    /**
     * Validate the configuration
     */
    public void validate() {
        if (apiToken == null || apiToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloudflare API token cannot be null or empty");
        }
        if (zoneId == null || zoneId.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloudflare zone ID cannot be null or empty");
        }
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Cloudflare account ID cannot be null or empty");
        }
        if (r2Enabled) {
            if (r2AccessKeyId == null || r2AccessKeyId.trim().isEmpty()) {
                throw new IllegalArgumentException("R2 access key ID cannot be null or empty when R2 is enabled");
            }
            if (r2SecretAccessKey == null || r2SecretAccessKey.trim().isEmpty()) {
                throw new IllegalArgumentException("R2 secret access key cannot be null or empty when R2 is enabled");
            }
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    @Override
    public String toString() {
        return "CloudflareConfiguration{" +
                "cdnDomain='" + cdnDomain + '\'' +
                ", cdnEnabled=" + cdnEnabled +
                ", r2Enabled=" + r2Enabled +
                ", imageOptimizationEnabled=" + imageOptimizationEnabled +
                ", freeTierOptimizations=" + freeTierOptimizations +
                ", maxFileSize=" + getMaxFileSizeHumanReadable() +
                '}';
    }
    
    /**
     * Cache rule configuration
     */
    public static class CacheRule {
        private String pattern;
        private int ttlSeconds;
        private boolean cacheEnabled;
        
        public CacheRule() {
        }
        
        public CacheRule(String pattern, int ttlSeconds, boolean cacheEnabled) {
            this.pattern = pattern;
            this.ttlSeconds = ttlSeconds;
            this.cacheEnabled = cacheEnabled;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        
        public int getTtlSeconds() {
            return ttlSeconds;
        }
        
        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
        
        public boolean isCacheEnabled() {
            return cacheEnabled;
        }
        
        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }
        
        @Override
        public String toString() {
            return "CacheRule{" +
                    "pattern='" + pattern + '\'' +
                    ", ttlSeconds=" + ttlSeconds +
                    ", cacheEnabled=" + cacheEnabled +
                    '}';
        }
    }
}