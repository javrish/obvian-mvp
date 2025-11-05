package api.security;

import java.util.Map;

/**
 * Configuration class for user rate limits.
 * Defines request quotas and execution limits per user.
 */
public class RateLimitConfig {
    
    private int requestsPerMinute;
    private int executionsPerHour;
    private int maxExecutionTimeSeconds;
    private int maxMemoryEntries;
    private String tier;

    public RateLimitConfig() {
        // Default constructor
    }

    public RateLimitConfig(int requestsPerMinute, int executionsPerHour, 
                          int maxExecutionTimeSeconds, int maxMemoryEntries, String tier) {
        this.requestsPerMinute = requestsPerMinute;
        this.executionsPerHour = executionsPerHour;
        this.maxExecutionTimeSeconds = maxExecutionTimeSeconds;
        this.maxMemoryEntries = maxMemoryEntries;
        this.tier = tier;
    }

    public static RateLimitConfig getDefault() {
        return new RateLimitConfig(60, 20, 60, 100, "free");
    }

    public static RateLimitConfig getFreeTier() {
        return new RateLimitConfig(60, 20, 60, 100, "free");
    }

    public static RateLimitConfig getProTier() {
        return new RateLimitConfig(300, 100, 300, 1000, "pro");
    }

    public static RateLimitConfig getEnterpriseTier() {
        return new RateLimitConfig(1000, 500, 1800, 10000, "enterprise");
    }

    public static RateLimitConfig fromMap(Map<String, Object> map) {
        RateLimitConfig config = new RateLimitConfig();
        config.requestsPerMinute = getIntValue(map, "requestsPerMinute", 60);
        config.executionsPerHour = getIntValue(map, "executionsPerHour", 20);
        config.maxExecutionTimeSeconds = getIntValue(map, "maxExecutionTimeSeconds", 60);
        config.maxMemoryEntries = getIntValue(map, "maxMemoryEntries", 100);
        config.tier = (String) map.getOrDefault("tier", "free");
        return config;
    }

    private static int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    // Getters and setters
    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public int getExecutionsPerHour() {
        return executionsPerHour;
    }

    public void setExecutionsPerHour(int executionsPerHour) {
        this.executionsPerHour = executionsPerHour;
    }

    public int getMaxExecutionTimeSeconds() {
        return maxExecutionTimeSeconds;
    }

    public void setMaxExecutionTimeSeconds(int maxExecutionTimeSeconds) {
        this.maxExecutionTimeSeconds = maxExecutionTimeSeconds;
    }

    public int getMaxMemoryEntries() {
        return maxMemoryEntries;
    }

    public void setMaxMemoryEntries(int maxMemoryEntries) {
        this.maxMemoryEntries = maxMemoryEntries;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }
}