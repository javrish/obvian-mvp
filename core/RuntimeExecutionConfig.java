package core;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime configuration options for DAG execution that can be passed at execution time
 * to control various aspects of the execution behavior.
 */
public class RuntimeExecutionConfig {
    
    // Debug mode configuration
    private boolean debugMode = false;
    
    // Global retry configuration (can be overridden per node)
    private int defaultMaxRetries = 0;
    private int defaultRetryDelayMs = 1000;
    private double defaultBackoffMultiplier = 1.0;
    
    // Timeout configuration
    private long executionTimeoutMs = 0; // 0 = no timeout
    private long nodeTimeoutMs = 0; // 0 = no timeout per node
    
    // Logging and tracing configuration
    private boolean verboseLogging = false;
    private boolean enableTracing = true;
    private boolean enableMetrics = true;
    
    // Hook configuration
    private long hookTimeoutMs = 1000; // 1 second default
    private boolean enableHooks = true;
    
    // Plugin configuration
    private boolean enableFallbackPlugins = true;
    private long pluginTimeoutMs = 0; // 0 = no timeout per plugin
    
    // Memory and performance configuration
    private boolean enableMemoryStore = true;
    private int maxContextSize = 1024 * 1024; // 1MB default
    
    // Causal learning configuration
    private boolean enableCausalLearning = true; // Enable by default for Patent Claim 12.4 compliance
    
    // Visual trace logging configuration
    private boolean enableRealTimeVisualUpdates = false; // Disabled by default for performance
    
    // Custom configuration properties
    private Map<String, Object> customProperties = new HashMap<>();
    
    /**
     * Create a default runtime configuration
     */
    public RuntimeExecutionConfig() {
        // Use defaults
    }
    
    /**
     * Create a runtime configuration with debug mode enabled
     */
    public static RuntimeExecutionConfig debugMode() {
        RuntimeExecutionConfig config = new RuntimeExecutionConfig();
        config.setDebugMode(true);
        config.setVerboseLogging(true);
        return config;
    }
    
    /**
     * Create a runtime configuration with custom timeout
     */
    public static RuntimeExecutionConfig withTimeout(long timeoutMs) {
        RuntimeExecutionConfig config = new RuntimeExecutionConfig();
        config.setExecutionTimeoutMs(timeoutMs);
        return config;
    }
    
    /**
     * Create a runtime configuration with custom retry settings
     */
    public static RuntimeExecutionConfig withRetries(int maxRetries, int delayMs, double backoffMultiplier) {
        RuntimeExecutionConfig config = new RuntimeExecutionConfig();
        config.setDefaultMaxRetries(maxRetries);
        config.setDefaultRetryDelayMs(delayMs);
        config.setDefaultBackoffMultiplier(backoffMultiplier);
        return config;
    }
    
    // Getters and setters
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public RuntimeExecutionConfig setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }
    
    public int getDefaultMaxRetries() {
        return defaultMaxRetries;
    }
    
    public RuntimeExecutionConfig setDefaultMaxRetries(int defaultMaxRetries) {
        this.defaultMaxRetries = defaultMaxRetries;
        return this;
    }
    
    public int getDefaultRetryDelayMs() {
        return defaultRetryDelayMs;
    }
    
    public RuntimeExecutionConfig setDefaultRetryDelayMs(int defaultRetryDelayMs) {
        this.defaultRetryDelayMs = defaultRetryDelayMs;
        return this;
    }
    
    public double getDefaultBackoffMultiplier() {
        return defaultBackoffMultiplier;
    }
    
    public RuntimeExecutionConfig setDefaultBackoffMultiplier(double defaultBackoffMultiplier) {
        this.defaultBackoffMultiplier = defaultBackoffMultiplier;
        return this;
    }
    
    public long getExecutionTimeoutMs() {
        return executionTimeoutMs;
    }
    
    public RuntimeExecutionConfig setExecutionTimeoutMs(long executionTimeoutMs) {
        this.executionTimeoutMs = executionTimeoutMs;
        return this;
    }
    
    public long getNodeTimeoutMs() {
        return nodeTimeoutMs;
    }
    
    public RuntimeExecutionConfig setNodeTimeoutMs(long nodeTimeoutMs) {
        this.nodeTimeoutMs = nodeTimeoutMs;
        return this;
    }
    
    public boolean isVerboseLogging() {
        return verboseLogging;
    }
    
    public RuntimeExecutionConfig setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
        return this;
    }
    
    public boolean isEnableTracing() {
        return enableTracing;
    }
    
    public RuntimeExecutionConfig setEnableTracing(boolean enableTracing) {
        this.enableTracing = enableTracing;
        return this;
    }
    
    public boolean isEnableMetrics() {
        return enableMetrics;
    }
    
    public RuntimeExecutionConfig setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
        return this;
    }
    
    public long getHookTimeoutMs() {
        return hookTimeoutMs;
    }
    
    public RuntimeExecutionConfig setHookTimeoutMs(long hookTimeoutMs) {
        this.hookTimeoutMs = hookTimeoutMs;
        return this;
    }
    
    public boolean isEnableHooks() {
        return enableHooks;
    }
    
    public RuntimeExecutionConfig setEnableHooks(boolean enableHooks) {
        this.enableHooks = enableHooks;
        return this;
    }
    
    public boolean isEnableFallbackPlugins() {
        return enableFallbackPlugins;
    }
    
    public RuntimeExecutionConfig setEnableFallbackPlugins(boolean enableFallbackPlugins) {
        this.enableFallbackPlugins = enableFallbackPlugins;
        return this;
    }
    
    public long getPluginTimeoutMs() {
        return pluginTimeoutMs;
    }
    
    public RuntimeExecutionConfig setPluginTimeoutMs(long pluginTimeoutMs) {
        this.pluginTimeoutMs = pluginTimeoutMs;
        return this;
    }
    
    public boolean isEnableMemoryStore() {
        return enableMemoryStore;
    }
    
    public RuntimeExecutionConfig setEnableMemoryStore(boolean enableMemoryStore) {
        this.enableMemoryStore = enableMemoryStore;
        return this;
    }
    
    public int getMaxContextSize() {
        return maxContextSize;
    }
    
    public RuntimeExecutionConfig setMaxContextSize(int maxContextSize) {
        this.maxContextSize = maxContextSize;
        return this;
    }
    
    public boolean isEnableCausalLearning() {
        return enableCausalLearning;
    }
    
    public RuntimeExecutionConfig setEnableCausalLearning(boolean enableCausalLearning) {
        this.enableCausalLearning = enableCausalLearning;
        return this;
    }
    
    public boolean isEnableRealTimeVisualUpdates() {
        return enableRealTimeVisualUpdates;
    }
    
    public RuntimeExecutionConfig setEnableRealTimeVisualUpdates(boolean enableRealTimeVisualUpdates) {
        this.enableRealTimeVisualUpdates = enableRealTimeVisualUpdates;
        return this;
    }
    
    public Map<String, Object> getCustomProperties() {
        return new HashMap<>(customProperties);
    }
    
    public RuntimeExecutionConfig setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = new HashMap<>(customProperties);
        return this;
    }
    
    public RuntimeExecutionConfig setCustomProperty(String key, Object value) {
        this.customProperties.put(key, value);
        return this;
    }
    
    public Object getCustomProperty(String key) {
        return customProperties.get(key);
    }
    
    public Object getCustomProperty(String key, Object defaultValue) {
        return customProperties.getOrDefault(key, defaultValue);
    }
    
    /**
     * Apply default retry configuration to a node if it doesn't have retry settings
     */
    public void applyDefaultRetryConfig(TaskNode node) {
        if (node.getMaxRetries() == 0 && defaultMaxRetries > 0) {
            node.setMaxRetries(defaultMaxRetries);
        }
        if (node.getRetryDelayMs() == 1000 && defaultRetryDelayMs != 1000) {
            node.setRetryDelayMs(defaultRetryDelayMs);
        }
        if (node.getBackoffMultiplier() == 1.0 && defaultBackoffMultiplier != 1.0) {
            node.setBackoffMultiplier(defaultBackoffMultiplier);
        }
    }
    
    public RuntimeExecutionConfig copy() {
        RuntimeExecutionConfig copy = new RuntimeExecutionConfig();
        copy.debugMode = this.debugMode;
        copy.defaultMaxRetries = this.defaultMaxRetries;
        copy.defaultRetryDelayMs = this.defaultRetryDelayMs;
        copy.defaultBackoffMultiplier = this.defaultBackoffMultiplier;
        copy.executionTimeoutMs = this.executionTimeoutMs;
        copy.nodeTimeoutMs = this.nodeTimeoutMs;
        copy.verboseLogging = this.verboseLogging;
        copy.enableTracing = this.enableTracing;
        copy.enableMetrics = this.enableMetrics;
        copy.hookTimeoutMs = this.hookTimeoutMs;
        copy.enableHooks = this.enableHooks;
        copy.enableFallbackPlugins = this.enableFallbackPlugins;
        copy.pluginTimeoutMs = this.pluginTimeoutMs;
        copy.enableMemoryStore = this.enableMemoryStore;
        copy.maxContextSize = this.maxContextSize;
        copy.enableCausalLearning = this.enableCausalLearning;
        copy.enableRealTimeVisualUpdates = this.enableRealTimeVisualUpdates;
        copy.customProperties = new HashMap<>(this.customProperties);
        return copy;
    }
    
    @Override
    public String toString() {
        return "RuntimeExecutionConfig{" +
                "debugMode=" + debugMode +
                ", defaultMaxRetries=" + defaultMaxRetries +
                ", defaultRetryDelayMs=" + defaultRetryDelayMs +
                ", defaultBackoffMultiplier=" + defaultBackoffMultiplier +
                ", executionTimeoutMs=" + executionTimeoutMs +
                ", nodeTimeoutMs=" + nodeTimeoutMs +
                ", verboseLogging=" + verboseLogging +
                ", enableTracing=" + enableTracing +
                ", enableMetrics=" + enableMetrics +
                ", hookTimeoutMs=" + hookTimeoutMs +
                ", enableHooks=" + enableHooks +
                ", enableFallbackPlugins=" + enableFallbackPlugins +
                ", pluginTimeoutMs=" + pluginTimeoutMs +
                ", enableMemoryStore=" + enableMemoryStore +
                ", maxContextSize=" + maxContextSize +
                ", enableCausalLearning=" + enableCausalLearning +
                ", enableRealTimeVisualUpdates=" + enableRealTimeVisualUpdates +
                ", customProperties=" + customProperties +
                '}';
    }
}