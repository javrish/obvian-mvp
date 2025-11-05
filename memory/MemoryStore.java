package memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal memory store implementation for storing execution context and state.
 *
 * This is a stub implementation to resolve compilation dependencies.
 * In a full implementation, this would integrate with Redis or other persistent storage.
 */
@Component
public class MemoryStore {

    private final Map<String, Object> memoryMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Store a value in memory with the given key
     */
    public void store(String key, Object value) {
        memoryMap.put(key, value);
    }

    /**
     * Retrieve a value from memory by key
     */
    public Object retrieve(String key) {
        return memoryMap.get(key);
    }

    /**
     * Retrieve a value from memory by key with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> T retrieve(String key, Class<T> type) {
        Object value = memoryMap.get(key);
        if (value == null) return null;

        if (type.isInstance(value)) {
            return (T) value;
        }

        // Try to convert using ObjectMapper for complex objects
        try {
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if a key exists in memory
     */
    public boolean contains(String key) {
        return memoryMap.containsKey(key);
    }

    /**
     * Remove a value from memory
     */
    public void remove(String key) {
        memoryMap.remove(key);
    }

    /**
     * Clear all memory
     */
    public void clear() {
        memoryMap.clear();
    }

    /**
     * Get the current size of memory store
     */
    public int size() {
        return memoryMap.size();
    }

    /**
     * Get the most recently stored file (stub implementation)
     */
    public FileMemoryEntry getLastFile() {
        // Stub: In full implementation, would query file entries by timestamp
        return retrieve("last_file", FileMemoryEntry.class);
    }

    /**
     * Store a file memory entry
     */
    public void storeFile(FileMemoryEntry entry) {
        store("last_file", entry);
        store("file_" + entry.getId(), entry);
    }

    /**
     * Get the most recent execution (stub implementation)
     */
    public ExecutionMemoryEntry getLastExecution() {
        // Stub: In full implementation, would query execution entries by timestamp
        return retrieve("last_execution", ExecutionMemoryEntry.class);
    }

    /**
     * Store an execution memory entry
     */
    public void storeExecution(ExecutionMemoryEntry entry) {
        store("last_execution", entry);
        store("exec_" + entry.getExecutionId(), entry);
    }
}