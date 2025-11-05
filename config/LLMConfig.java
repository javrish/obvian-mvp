package api.config;

import core.EnhancedPromptParser;
// Groq removed - using local LLM only
import core.llm.OllamaLLMService;
import core.llm.PatternLearningService;
import memory.MemoryStore;
import plugins.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Spring configuration for LLM services including Groq integration,
 * caching, and prompt parsing strategies.
 */
@Configuration
@EnableCaching
public class LLMConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(LLMConfig.class);
    
    // Groq removed - no API keys needed
    
    @Value("${llm.parsing.enabled:true}")
    private boolean llmParsingEnabled;
    
    @Value("${llm.cache.enabled:true}")
    private boolean cacheEnabled;
    
    // Groq service removed - using local Ollama only
    
    /**
     * Ollama LLM Service bean - local LLM with no rate limits
     */
    @Bean
    @ConditionalOnProperty(name = "ollama.enabled", havingValue = "true", matchIfMissing = true)
    public OllamaLLMService ollamaLLMService(@Autowired(required = false) PluginRegistry pluginRegistry,
                                             @Autowired(required = false) PatternLearningService patternLearner) {
        logger.info("Initializing Ollama LLM Service for local inference");
        OllamaLLMService service = new OllamaLLMService();
        if (pluginRegistry != null) {
            service.setPluginRegistry(pluginRegistry);
        }
        if (patternLearner != null) {
            service.setPatternLearner(patternLearner);
        }
        return service;
    }
    
    /**
     * Pattern Learning Service - learns from LLM outputs
     */
    @Bean
    public PatternLearningService patternLearningService() {
        logger.info("Initializing Pattern Learning Service");
        return new PatternLearningService();
    }
    
    /**
     * Enhanced Prompt Parser - primary parser with LLM capabilities
     */
    @Bean
    @Primary
    public EnhancedPromptParser enhancedPromptParser(MemoryStore memoryStore) {
        logger.info("Initializing Enhanced Prompt Parser with LLM parsing enabled: {}", llmParsingEnabled);
        return new EnhancedPromptParser(memoryStore);
    }
    
    /**
     * Cache manager for LLM responses and parsed prompts
     */
    @Bean("llmCacheManager")
    public CacheManager llmCacheManager() {
        logger.info("Initializing LLM cache manager with caching enabled: {}", cacheEnabled);
        
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        if (cacheEnabled) {
            // Configure cache names for different types of cached data
            cacheManager.setCacheNames(java.util.List.of(
                "ollama-intents",    // Cached Ollama responses
                "parsed-prompts",    // Cached parse results
                "learned-patterns",  // Cached learned patterns
                "memory-resolutions" // Cached memory reference resolutions
            ));
        }
        
        return cacheManager;
    }
    
    /**
     * Executor for async LLM operations
     */
    @Bean("llmExecutor")
    public Executor llmExecutor() {
        logger.info("Initializing LLM executor with 4 threads");
        return Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "llm-worker");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * LLM Configuration Properties
     */
    @Bean
    @ConfigurationProperties(prefix = "llm")
    public LLMProperties llmProperties() {
        return new LLMProperties();
    }
    
    /**
     * Configuration properties for LLM services
     */
    public static class LLMProperties {
        // Groq removed - local processing only
        private Ollama ollama = new Ollama();
        private Cache cache = new Cache();
        private Parsing parsing = new Parsing();
        
        public Ollama getOllama() { return ollama; }
        public void setOllama(Ollama ollama) { this.ollama = ollama; }
        
        public Cache getCache() { return cache; }
        public void setCache(Cache cache) { this.cache = cache; }
        
        public Parsing getParsing() { return parsing; }
        public void setParsing(Parsing parsing) { this.parsing = parsing; }
        
        // Groq configuration class removed
        
        public static class Ollama {
            private boolean enabled = true;  // Default to enabled for local processing
            private String baseUrl = "http://localhost:11434";
            private String model = "llama3.1:70b";
            private int timeoutSeconds = 60;
            private int maxTokens = 1000;
            private double temperature = 0.1;
            private int maxRetries = 2;
            private Duration retryDelay = Duration.ofSeconds(2);
            
            // Getters and setters
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
            
            public int getTimeoutSeconds() { return timeoutSeconds; }
            public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
            
            public int getMaxTokens() { return maxTokens; }
            public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
            
            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
            
            public int getMaxRetries() { return maxRetries; }
            public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
            
            public Duration getRetryDelay() { return retryDelay; }
            public void setRetryDelay(Duration retryDelay) { this.retryDelay = retryDelay; }
        }
        
        public static class Cache {
            private boolean enabled = true;
            private int maxSize = 1000;
            private Duration ttl = Duration.ofMinutes(30);
            private boolean persistToDisk = false;
            private String diskCachePath = "./cache/llm";
            
            // Getters and setters
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public int getMaxSize() { return maxSize; }
            public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
            
            public Duration getTtl() { return ttl; }
            public void setTtl(Duration ttl) { this.ttl = ttl; }
            
            public boolean isPersistToDisk() { return persistToDisk; }
            public void setPersistToDisk(boolean persistToDisk) { this.persistToDisk = persistToDisk; }
            
            public String getDiskCachePath() { return diskCachePath; }
            public void setDiskCachePath(String diskCachePath) { this.diskCachePath = diskCachePath; }
        }
        
        public static class Parsing {
            private String strategy = "local"; // local, patterns-only, regex-only
            private double confidenceThreshold = 0.7;
            private boolean enhanceWithMemory = true;
            private boolean validateResults = true;
            private int maxIntentsPerPrompt = 10;
            private Duration maxParsingTime = Duration.ofSeconds(30);
            
            // Getters and setters
            public String getStrategy() { return strategy; }
            public void setStrategy(String strategy) { this.strategy = strategy; }
            
            public double getConfidenceThreshold() { return confidenceThreshold; }
            public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
            
            public boolean isEnhanceWithMemory() { return enhanceWithMemory; }
            public void setEnhanceWithMemory(boolean enhanceWithMemory) { this.enhanceWithMemory = enhanceWithMemory; }
            
            public boolean isValidateResults() { return validateResults; }
            public void setValidateResults(boolean validateResults) { this.validateResults = validateResults; }
            
            public int getMaxIntentsPerPrompt() { return maxIntentsPerPrompt; }
            public void setMaxIntentsPerPrompt(int maxIntentsPerPrompt) { this.maxIntentsPerPrompt = maxIntentsPerPrompt; }
            
            public Duration getMaxParsingTime() { return maxParsingTime; }
            public void setMaxParsingTime(Duration maxParsingTime) { this.maxParsingTime = maxParsingTime; }
        }
    }
}