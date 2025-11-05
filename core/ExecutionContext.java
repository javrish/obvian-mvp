package core;

import memory.MemoryStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Execution context for DAG and task execution.
 *
 * Contains execution state, memory references, and metadata.
 */
public class ExecutionContext {

    private final String executionId;
    private final Map<String, Object> variables;
    private final MemoryStore memoryStore;
    private final Map<String, Object> metadata;
    private final long startTime;

    public ExecutionContext() {
        this.executionId = "exec_" + UUID.randomUUID().toString().replace("-", "");
        this.variables = new ConcurrentHashMap<>();
        this.memoryStore = new MemoryStore();
        this.metadata = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }

    public ExecutionContext(MemoryStore memoryStore) {
        this.executionId = "exec_" + UUID.randomUUID().toString().replace("-", "");
        this.variables = new ConcurrentHashMap<>();
        this.memoryStore = memoryStore != null ? memoryStore : new MemoryStore();
        this.metadata = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }

    // Variable management
    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, Class<T> type) {
        Object value = variables.get(key);
        if (value == null) return null;

        if (type.isInstance(value)) {
            return (T) value;
        }

        // Try basic type conversions
        if (type == String.class) {
            return (T) value.toString();
        }

        return null;
    }

    public boolean hasVariable(String key) {
        return variables.containsKey(key);
    }

    public void removeVariable(String key) {
        variables.remove(key);
    }

    public Map<String, Object> getAllVariables() {
        return new ConcurrentHashMap<>(variables);
    }

    // Memory store access
    public MemoryStore getMemoryStore() {
        return memoryStore;
    }

    // Metadata management
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public Map<String, Object> getAllMetadata() {
        return new ConcurrentHashMap<>(metadata);
    }

    // Execution info
    public String getExecutionId() {
        return executionId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}