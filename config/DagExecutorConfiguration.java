package core.config;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration for DAG executor
 */
public class DagExecutorConfiguration {
    private int maxConcurrentExecutions = 10;
    private Duration defaultTimeout = Duration.ofMinutes(30);
    private int maxRetryAttempts = 3;
    private Duration retryDelay = Duration.ofSeconds(5);
    private boolean enableCircuitBreaker = true;
    private boolean enableMetrics = true;
    private boolean enableTracing = true;
    private String executorPoolSize = "fixed";
    private Map<String, Object> customSettings;

    public DagExecutorConfiguration() {}

    // Getters and setters
    public int getMaxConcurrentExecutions() { return maxConcurrentExecutions; }
    public void setMaxConcurrentExecutions(int maxConcurrentExecutions) { 
        this.maxConcurrentExecutions = maxConcurrentExecutions; 
    }

    public Duration getDefaultTimeout() { return defaultTimeout; }
    public void setDefaultTimeout(Duration defaultTimeout) { this.defaultTimeout = defaultTimeout; }

    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }

    public Duration getRetryDelay() { return retryDelay; }
    public void setRetryDelay(Duration retryDelay) { this.retryDelay = retryDelay; }

    public boolean isEnableCircuitBreaker() { return enableCircuitBreaker; }
    public void setEnableCircuitBreaker(boolean enableCircuitBreaker) { this.enableCircuitBreaker = enableCircuitBreaker; }

    public boolean isEnableMetrics() { return enableMetrics; }
    public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }

    public boolean isEnableTracing() { return enableTracing; }
    public void setEnableTracing(boolean enableTracing) { this.enableTracing = enableTracing; }

    public String getExecutorPoolSize() { return executorPoolSize; }
    public void setExecutorPoolSize(String executorPoolSize) { this.executorPoolSize = executorPoolSize; }

    public Map<String, Object> getCustomSettings() { return customSettings; }
    public void setCustomSettings(Map<String, Object> customSettings) { this.customSettings = customSettings; }

    @Override
    public String toString() {
        return "DagExecutorConfiguration{" +
                "maxConcurrentExecutions=" + maxConcurrentExecutions +
                ", defaultTimeout=" + defaultTimeout +
                ", maxRetryAttempts=" + maxRetryAttempts +
                '}';
    }
}