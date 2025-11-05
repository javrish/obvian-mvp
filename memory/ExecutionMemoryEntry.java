package memory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents an execution memory entry in the memory store.
 */
public class ExecutionMemoryEntry {

    private final String executionId;
    private final String status;
    private final Map<String, Object> results;
    private final LocalDateTime started;
    private final LocalDateTime completed;

    public ExecutionMemoryEntry(String executionId, String status, Map<String, Object> results) {
        this.executionId = executionId;
        this.status = status;
        this.results = results != null ? new HashMap<>(results) : new HashMap<>();
        this.started = LocalDateTime.now();
        this.completed = "COMPLETED".equals(status) ? LocalDateTime.now() : null;
    }

    // Getters
    public String getExecutionId() { return executionId; }
    public String getStatus() { return status; }
    public Map<String, Object> getResults() { return new HashMap<>(results); }
    public LocalDateTime getStarted() { return started; }
    public LocalDateTime getCompleted() { return completed; }
}