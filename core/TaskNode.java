package core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a node in the DAG that contains a task to be executed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskNode {
    String id;
    String action;
    public List<String> dependencyIds;
    @JsonIgnore
    transient List<TaskNode> resolvedDependencies;
    @JsonIgnore
    transient List<TaskNode> dependents;
    /**
     * Structured input parameters for this node (may contain tokens for substitution)
     */
    Map<String, Object> inputParams;
    /**
     * Optional hook to run before plugin execution (may mutate or inspect context)
     */
    @JsonIgnore
    Consumer<ExecutionContext> beforeHook;
    /**
     * Optional hook to run after plugin execution (may mutate or inspect context)
     */
    @JsonIgnore
    Consumer<ExecutionContext> afterHook;
    /**
     * Maximum number of plugin retry attempts (0 = no retry)
     */
    int maxRetries;
    /**
     * Delay in milliseconds between retries
     */
    int retryDelayMs;
    /**
     * Backoff multiplier for retry delay (1.0 = constant delay)
     */
    double backoffMultiplier;
    /**
     * Optional fallback plugin action to execute if primary plugin fails after retries
     */
    String fallbackPluginId;
    
    /**
     * Metadata for storing additional node information (CADR insights, temporal constraints, etc.)
     */
    Map<String, Object> metadata;
    
    /**
     * Temporal constraints for this node
     */
    List<Object> temporalConstraints;
    
    public TaskNode() {
        this.dependencyIds = new ArrayList<>();
        this.resolvedDependencies = new ArrayList<>();
        this.dependents = new ArrayList<>();
        // Initialize retry configuration with sensible defaults
        this.maxRetries = 0; // No retries by default
        this.retryDelayMs = 1000; // 1 second default delay
        this.backoffMultiplier = 1.0; // No backoff by default
        this.metadata = new HashMap<>();
        this.temporalConstraints = new ArrayList<>();
    }
    
    public TaskNode(String id) {
        this();
        this.id = id;
    }
    
    public TaskNode(String id, String action) {
        this();
        this.id = id;
        this.action = action;
    }

    public TaskNode(String id, String action, Map<String, Object> inputParams) {
        this(id, action);
        this.inputParams = inputParams;
    }

    public TaskNode(String id, String action, Map<String, Object> inputParams, Consumer<ExecutionContext> beforeHook, Consumer<ExecutionContext> afterHook) {
        this(id, action, inputParams);
        this.beforeHook = beforeHook;
        this.afterHook = afterHook;
    }

    /**
     * Create a TaskNode with input parameters, lifecycle hooks, retry config, and fallback plugin
     * @param id Node ID
     * @param action Plugin action
     * @param inputParams Input parameters (may be null)
     * @param beforeHook Hook to run before plugin execution (may be null)
     * @param afterHook Hook to run after plugin execution (may be null)
     * @param maxRetries Maximum number of plugin retry attempts (0 = no retry)
     * @param retryDelayMs Delay in ms between retries
     * @param backoffMultiplier Backoff multiplier for delay (1.0 = constant)
     * @param fallbackPluginId Fallback plugin action to use if primary fails (may be null)
     */
    public TaskNode(String id, String action, Map<String, Object> inputParams, Consumer<ExecutionContext> beforeHook, Consumer<ExecutionContext> afterHook, int maxRetries, int retryDelayMs, double backoffMultiplier, String fallbackPluginId) {
        this(id, action, inputParams, beforeHook, afterHook);
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.fallbackPluginId = fallbackPluginId;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public List<String> getDependencyIds() { return dependencyIds; }
    public void setDependencyIds(List<String> dependencyIds) { this.dependencyIds = dependencyIds; }

    /**
     * Get dependencies (alias for getDependencyIds for compatibility)
     */
    public List<String> getDependencies() { return dependencyIds; }
    @JsonIgnore
    public List<TaskNode> getResolvedDependencies() { return resolvedDependencies; }
    public void setResolvedDependencies(List<TaskNode> resolvedDependencies) { this.resolvedDependencies = resolvedDependencies; }
    @JsonIgnore
    public List<TaskNode> getDependents() { return dependents; }
    public void setDependents(List<TaskNode> dependents) { this.dependents = dependents; }
    /**
     * Get the input parameters for this node (never null)
     */
    public Map<String, Object> getInputParams() { return inputParams; }
    public void setInputParams(Map<String, Object> inputParams) { this.inputParams = inputParams; }
    /**
     * Get the before-execution hook (may be null)
     */
    @JsonIgnore
    public Consumer<ExecutionContext> getBeforeHook() { return beforeHook; }
    public void setBeforeHook(Consumer<ExecutionContext> beforeHook) { this.beforeHook = beforeHook; }
    /**
     * Get the after-execution hook (may be null)
     */
    @JsonIgnore
    public Consumer<ExecutionContext> getAfterHook() { return afterHook; }
    public void setAfterHook(Consumer<ExecutionContext> afterHook) { this.afterHook = afterHook; }
    /**
     * Get the maximum number of plugin retry attempts
     */
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    /**
     * Get the delay in ms between plugin retries
     */
    public int getRetryDelayMs() { return retryDelayMs; }
    public void setRetryDelayMs(int retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    /**
     * Get the backoff multiplier for retry delay
     */
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
    /**
     * Get the fallback plugin action ID (may be null)
     */
    public String getFallbackPluginId() { return fallbackPluginId; }
    public void setFallbackPluginId(String fallbackPluginId) { this.fallbackPluginId = fallbackPluginId; }
    /**
     * Get the metadata map for this node (never null)
     */
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata != null ? metadata : new HashMap<>(); }
    /**
     * Get the temporal constraints for this node (may be null)
     */
    public List<Object> getTemporalConstraints() { return temporalConstraints; }
    public void setTemporalConstraints(List<Object> temporalConstraints) { this.temporalConstraints = temporalConstraints; }
    /**
     * Get plugin name (alias for action for compatibility)
     */
    public String getPluginName() { return action; }
    public void setPluginName(String pluginName) { this.action = pluginName; }
    
    /**
     * Get the task type (derived from action for compatibility)
     */
    public String getTaskType() { 
        return action; 
    }
    /**
     * Get parameters (alias for inputParams for compatibility)
     */
    public Map<String, Object> getParameters() { return inputParams; }
    
    // Resolve dependencyIds to TaskNode references
    public void resolveDependencies(Map<String, TaskNode> nodeMap) {
        resolvedDependencies = new ArrayList<>();
        if (dependencyIds != null) {
            for (String depId : dependencyIds) {
                TaskNode dep = nodeMap.get(depId);
                if (dep == null) {
                    throw new IllegalArgumentException("Dependency ID '" + depId + "' not found for node '" + id + "'.");
                }
                resolvedDependencies.add(dep);
            }
        }
    }

    public void addDependency(TaskNode dependency) {
        if (!resolvedDependencies.contains(dependency)) {
            resolvedDependencies.add(dependency);
            dependency.dependents.add(this);
            // Ensure dependencyIds is synchronized for serialization
            if (!dependencyIds.contains(dependency.getId())) {
                dependencyIds.add(dependency.getId());
            }
        }
    }
    
    public void removeDependency(TaskNode dependency) {
        resolvedDependencies.remove(dependency);
        dependency.dependents.remove(this);
    }
    
    @Override
    public String toString() {
        return "TaskNode{" +
                "id='" + id + '\'' +
                ", action='" + action + '\'' +
                (inputParams != null && !inputParams.isEmpty() ? ", inputParams=" + inputParams : "") +
                (beforeHook != null ? ", beforeHook=present" : "") +
                (afterHook != null ? ", afterHook=present" : "") +
                (maxRetries > 0 ? ", maxRetries=" + maxRetries : "") +
                (retryDelayMs > 0 ? ", retryDelayMs=" + retryDelayMs : "") +
                (backoffMultiplier != 1.0 ? ", backoffMultiplier=" + backoffMultiplier : "") +
                (fallbackPluginId != null ? ", fallbackPluginId='" + fallbackPluginId + '\'' : "") +
                '}';
    }
    
    // Convenience methods for template DAG building
    
    /**
     * Set description in metadata.
     * @param description the description to store
     */
    public void setDescription(String description) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put("description", description);
    }
    
    /**
     * Get description from metadata.
     * @return the description or null if not set
     */
    public String getDescription() {
        if (this.metadata != null) {
            Object desc = this.metadata.get("description");
            return desc != null ? desc.toString() : null;
        }
        return null;
    }
    
    /**
     * Set inputs (alias for setInputParams).
     * @param inputs the input parameters
     */
    public void setInputs(Map<String, Object> inputs) {
        this.setInputParams(inputs);
    }
    
    /**
     * Get inputs (alias for getInputParams).
     * @return the input parameters
     */
    public Map<String, Object> getInputs() {
        return this.getInputParams();
    }
}
