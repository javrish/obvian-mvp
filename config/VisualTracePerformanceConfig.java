package api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Performance configuration for Visual Trace operations.
 * 
 * This configuration provides:
 * - Advanced caching strategies with Caffeine
 * - Response compression settings
 * - Cache control headers for HTTP responses
 * - Performance monitoring interceptors
 * - Pagination and batching configurations
 * 
 * @author Obvian Labs
 * @since Phase 26.2
 */
@Configuration
@ConditionalOnProperty(name = "obvian.features.visual-trace.enabled", havingValue = "true", matchIfMissing = false)
@ConfigurationProperties(prefix = "obvian.visual-trace.performance")
public class VisualTracePerformanceConfig implements WebMvcConfigurer {
    
    // Cache configuration properties
    private int maxCacheSize = 10000;
    private Duration cacheExpireAfterWrite = Duration.ofMinutes(30);
    private Duration cacheExpireAfterAccess = Duration.ofMinutes(15);
    private Duration cacheRefreshAfterWrite = Duration.ofMinutes(10);
    
    // Pagination configuration
    private int defaultPageSize = 50;
    private int maxPageSize = 1000;
    private int maxTimelineEvents = 10000;
    
    // Compression configuration
    private boolean enableCompression = true;
    private int compressionThreshold = 1024; // 1KB
    private String[] compressibleMimeTypes = {
        "application/json",
        "application/javascript",
        "text/html",
        "text/xml",
        "text/plain",
        "text/css"
    };
    
    // Response caching configuration
    private Duration maxAge = Duration.ofMinutes(5);
    private Duration staleWhileRevalidate = Duration.ofMinutes(10);
    private boolean enableEtag = true;
    
    /**
     * Configures high-performance cache manager with Caffeine.
     */
    @Bean("visualTraceCacheManager")
    public CacheManager visualTraceCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure Caffeine cache
        Caffeine<Object, Object> caffeineConfig = Caffeine.newBuilder()
            .maximumSize(maxCacheSize)
            .expireAfterWrite(cacheExpireAfterWrite.toMinutes(), TimeUnit.MINUTES)
            .expireAfterAccess(cacheExpireAfterAccess.toMinutes(), TimeUnit.MINUTES)
            .refreshAfterWrite(cacheRefreshAfterWrite.toMinutes(), TimeUnit.MINUTES)
            .recordStats() // Enable metrics collection
            .removalListener((key, value, cause) -> {
                // Log cache evictions for monitoring
                org.slf4j.LoggerFactory.getLogger(VisualTracePerformanceConfig.class)
                    .debug("Cache entry evicted: key={}, cause={}", key, cause);
            });
        
        cacheManager.setCaffeine(caffeineConfig);
        
        // Pre-configure cache names
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "visualTraceData",
            "criticalPath", 
            "timelineEvents",
            "renderingCache",
            "metricsCache"
        ));
        
        return cacheManager;
    }
    
    /**
     * Specialized cache for large trace data with different eviction policy.
     */
    @Bean("largeTraceCacheManager")
    public CacheManager largeTraceCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure for large data with size-based eviction
        Caffeine<Object, Object> caffeineConfig = Caffeine.newBuilder()
            .maximumWeight(100 * 1024 * 1024) // 100MB max cache size
            .weigher((Object key, Object value) -> estimateObjectSize(value))
            .expireAfterWrite(10, TimeUnit.MINUTES) // Shorter expiry for large objects
            .recordStats()
            .removalListener((Object key, Object value, com.github.benmanes.caffeine.cache.RemovalCause cause) -> {
                org.slf4j.LoggerFactory.getLogger(VisualTracePerformanceConfig.class)
                    .info("Large cache entry evicted: key={}, cause={}, estimatedSize={}KB", 
                        key, cause, estimateObjectSize(value) / 1024);
            });
        
        cacheManager.setCaffeine(caffeineConfig);
        
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "largeVisualTraceData",
            "compressedTimelineData"
        ));
        
        return cacheManager;
    }
    
    /**
     * Cache configuration for WebSocket/SSE event caching.
     */
    @Bean("eventStreamCacheManager")
    public CacheManager eventStreamCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure for high-frequency, short-lived data
        Caffeine<Object, Object> caffeineConfig = Caffeine.newBuilder()
            .maximumSize(50000) // Higher capacity for events
            .expireAfterWrite(2, TimeUnit.MINUTES) // Very short expiry
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .recordStats();
        
        cacheManager.setCaffeine(caffeineConfig);
        
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "webSocketEvents",
            "sseEvents",
            "realtimeMetrics"
        ));
        
        return cacheManager;
    }
    
    /**
     * Performance monitoring interceptor for HTTP requests.
     */
    @Bean
    public VisualTracePerformanceInterceptor visualTracePerformanceInterceptor() {
        return new VisualTracePerformanceInterceptor();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visualTracePerformanceInterceptor())
            .addPathPatterns("/api/visual-trace/**")
            .addPathPatterns("/api/timeline/**")
            .addPathPatterns("/api/playback/**");
    }
    
    /**
     * Cache control configuration for different types of responses.
     */
    @Bean
    public CacheControlConfig cacheControlConfig() {
        CacheControlConfig config = new CacheControlConfig();
        
        // Static trace data - can be cached longer
        config.addCacheControl("/api/visual-trace/*/data", 
            CacheControl.maxAge(maxAge)
                .staleWhileRevalidate(staleWhileRevalidate)
                .cachePrivate());
        
        // Timeline events - shorter cache duration
        config.addCacheControl("/api/timeline/*/events", 
            CacheControl.maxAge(Duration.ofMinutes(2))
                .staleWhileRevalidate(Duration.ofMinutes(5))
                .cachePrivate());
        
        // Real-time data - no caching
        config.addCacheControl("/api/playback/**", 
            CacheControl.noCache().noStore());
        
        return config;
    }
    
    /**
     * Pagination configuration bean.
     */
    @Bean
    public PaginationConfig paginationConfig() {
        PaginationConfig config = new PaginationConfig();
        config.setDefaultPageSize(defaultPageSize);
        config.setMaxPageSize(maxPageSize);
        config.setMaxTimelineEvents(maxTimelineEvents);
        return config;
    }
    
    /**
     * Compression configuration bean.
     */
    @Bean
    public CompressionConfig compressionConfig() {
        CompressionConfig config = new CompressionConfig();
        config.setEnabled(enableCompression);
        config.setThreshold(compressionThreshold);
        config.setMimeTypes(compressibleMimeTypes);
        return config;
    }
    
    // ===== Helper Methods =====
    
    /**
     * Estimates object size for cache weighing (rough approximation).
     */
    private int estimateObjectSize(Object obj) {
        if (obj == null) return 0;
        
        // Simple heuristic based on object type
        String className = obj.getClass().getSimpleName();
        switch (className) {
            case "VisualTraceData":
                return 50 * 1024; // 50KB estimate
            case "TimelineVisualization":
                return 30 * 1024; // 30KB estimate
            case "CriticalPathAnalysis":
                return 10 * 1024; // 10KB estimate
            case "String":
                return ((String) obj).length() * 2; // 2 bytes per char
            default:
                return 1024; // 1KB default
        }
    }
    
    // ===== Configuration Data Classes =====
    
    public static class CacheControlConfig {
        private final java.util.Map<String, CacheControl> cacheControls = new java.util.HashMap<>();
        
        public void addCacheControl(String pattern, CacheControl cacheControl) {
            cacheControls.put(pattern, cacheControl);
        }
        
        public CacheControl getCacheControl(String path) {
            return cacheControls.entrySet().stream()
                .filter(entry -> path.matches(entry.getKey().replace("*", ".*")))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(CacheControl.noCache());
        }
        
        public java.util.Map<String, CacheControl> getAllCacheControls() {
            return new java.util.HashMap<>(cacheControls);
        }
    }
    
    public static class PaginationConfig {
        private int defaultPageSize;
        private int maxPageSize;
        private int maxTimelineEvents;
        
        // Getters and setters
        public int getDefaultPageSize() { return defaultPageSize; }
        public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }
        
        public int getMaxPageSize() { return maxPageSize; }
        public void setMaxPageSize(int maxPageSize) { this.maxPageSize = maxPageSize; }
        
        public int getMaxTimelineEvents() { return maxTimelineEvents; }
        public void setMaxTimelineEvents(int maxTimelineEvents) { this.maxTimelineEvents = maxTimelineEvents; }
    }
    
    public static class CompressionConfig {
        private boolean enabled;
        private int threshold;
        private String[] mimeTypes;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getThreshold() { return threshold; }
        public void setThreshold(int threshold) { this.threshold = threshold; }
        
        public String[] getMimeTypes() { return mimeTypes; }
        public void setMimeTypes(String[] mimeTypes) { this.mimeTypes = mimeTypes; }
    }
    
    // ===== Property Getters and Setters =====
    
    public int getMaxCacheSize() { return maxCacheSize; }
    public void setMaxCacheSize(int maxCacheSize) { this.maxCacheSize = maxCacheSize; }
    
    public Duration getCacheExpireAfterWrite() { return cacheExpireAfterWrite; }
    public void setCacheExpireAfterWrite(Duration cacheExpireAfterWrite) { 
        this.cacheExpireAfterWrite = cacheExpireAfterWrite; 
    }
    
    public Duration getCacheExpireAfterAccess() { return cacheExpireAfterAccess; }
    public void setCacheExpireAfterAccess(Duration cacheExpireAfterAccess) { 
        this.cacheExpireAfterAccess = cacheExpireAfterAccess; 
    }
    
    public Duration getCacheRefreshAfterWrite() { return cacheRefreshAfterWrite; }
    public void setCacheRefreshAfterWrite(Duration cacheRefreshAfterWrite) { 
        this.cacheRefreshAfterWrite = cacheRefreshAfterWrite; 
    }
    
    public int getDefaultPageSize() { return defaultPageSize; }
    public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }
    
    public int getMaxPageSize() { return maxPageSize; }
    public void setMaxPageSize(int maxPageSize) { this.maxPageSize = maxPageSize; }
    
    public int getMaxTimelineEvents() { return maxTimelineEvents; }
    public void setMaxTimelineEvents(int maxTimelineEvents) { this.maxTimelineEvents = maxTimelineEvents; }
    
    public boolean isEnableCompression() { return enableCompression; }
    public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }
    
    public int getCompressionThreshold() { return compressionThreshold; }
    public void setCompressionThreshold(int compressionThreshold) { this.compressionThreshold = compressionThreshold; }
    
    public String[] getCompressibleMimeTypes() { return compressibleMimeTypes; }
    public void setCompressibleMimeTypes(String[] compressibleMimeTypes) { 
        this.compressibleMimeTypes = compressibleMimeTypes; 
    }
    
    public Duration getMaxAge() { return maxAge; }
    public void setMaxAge(Duration maxAge) { this.maxAge = maxAge; }
    
    public Duration getStaleWhileRevalidate() { return staleWhileRevalidate; }
    public void setStaleWhileRevalidate(Duration staleWhileRevalidate) { 
        this.staleWhileRevalidate = staleWhileRevalidate; 
    }
    
    public boolean isEnableEtag() { return enableEtag; }
    public void setEnableEtag(boolean enableEtag) { this.enableEtag = enableEtag; }
}