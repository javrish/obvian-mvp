package core;

import java.util.Map;

/**
 * Interface for collecting and publishing execution metrics.
 * Supports pluggable backends for different metrics systems.
 */
public interface MetricsCollector {
    
    /**
     * Record a DAG execution start event
     * 
     * @param executionId Unique execution identifier
     * @param dagId DAG identifier
     * @param nodeCount Number of nodes in the DAG
     */
    void recordExecutionStart(String executionId, String dagId, int nodeCount);
    
    /**
     * Record a DAG execution completion event
     * 
     * @param executionId Unique execution identifier
     * @param dagId DAG identifier
     * @param success Whether the execution was successful
     * @param durationMs Execution duration in milliseconds
     * @param nodesExecuted Number of nodes that were executed
     * @param nodesSucceeded Number of nodes that succeeded
     * @param nodesFailed Number of nodes that failed
     * @param nodesSkipped Number of nodes that were skipped
     */
    void recordExecutionComplete(String executionId, String dagId, boolean success, 
                               long durationMs, int nodesExecuted, int nodesSucceeded, 
                               int nodesFailed, int nodesSkipped);
    
    /**
     * Record a node execution start event
     * 
     * @param executionId Unique execution identifier
     * @param nodeId Node identifier
     * @param pluginId Plugin identifier
     */
    void recordNodeStart(String executionId, String nodeId, String pluginId);
    
    /**
     * Record a node execution completion event
     * 
     * @param executionId Unique execution identifier
     * @param nodeId Node identifier
     * @param pluginId Plugin identifier
     * @param success Whether the node execution was successful
     * @param durationMs Node execution duration in milliseconds
     * @param retryCount Number of retries attempted
     * @param usedFallback Whether fallback plugin was used
     */
    void recordNodeComplete(String executionId, String nodeId, String pluginId, 
                          boolean success, long durationMs, int retryCount, boolean usedFallback);
    
    /**
     * Record a plugin execution event
     * 
     * @param pluginId Plugin identifier
     * @param success Whether the plugin execution was successful
     * @param durationMs Plugin execution duration in milliseconds
     */
    void recordPluginExecution(String pluginId, boolean success, long durationMs);
    
    /**
     * Record a custom metric with tags
     * 
     * @param metricName Name of the metric
     * @param value Metric value
     * @param tags Additional tags for the metric
     */
    void recordCustomMetric(String metricName, double value, Map<String, String> tags);
    
    /**
     * Increment a counter metric
     * 
     * @param counterName Name of the counter
     * @param tags Additional tags for the counter
     */
    void incrementCounter(String counterName, Map<String, String> tags);
    
    /**
     * Record a timing metric
     * 
     * @param timerName Name of the timer
     * @param durationMs Duration in milliseconds
     * @param tags Additional tags for the timer
     */
    void recordTiming(String timerName, long durationMs, Map<String, String> tags);
    
    /**
     * Get current metrics snapshot (for testing and monitoring)
     * 
     * @return Map of metric names to their current values
     */
    Map<String, Object> getMetricsSnapshot();
    
    /**
     * Reset all metrics (primarily for testing)
     */
    void reset();
    
    /**
     * Flush any pending metrics to the backend
     */
    void flush();
    
    /**
     * Record a generic metric with arbitrary data
     * 
     * @param metricName Name of the metric
     * @param data Metric data as key-value pairs
     */
    void recordMetric(String metricName, Map<String, Object> data);
}