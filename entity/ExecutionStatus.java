package api.entity;

/**
 * Enum representing the status of an execution.
 */
public enum ExecutionStatus {
    PENDING("Execution is pending"),
    QUEUED("Execution is queued for processing"),
    RUNNING("Execution is currently running"),
    PAUSED("Execution is paused"),
    COMPLETED("Execution completed successfully"),
    FAILED("Execution failed"),
    CANCELLED("Execution was cancelled"),
    RETRYING("Execution is being retried"),
    SKIPPED("Execution was skipped");

    private final String description;

    ExecutionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == SKIPPED;
    }

    public boolean isActive() {
        return this == RUNNING || this == PAUSED || this == RETRYING;
    }

    public boolean canBeRetried() {
        return this == FAILED;
    }

    public boolean canBeCancelled() {
        return this == PENDING || this == QUEUED || this == RUNNING || this == PAUSED || this == RETRYING;
    }
}