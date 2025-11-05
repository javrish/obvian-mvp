package core;

/**
 * Categorizes the types of errors that can occur during DAG execution
 */
public enum ExecutionErrorType {
    /**
     * Errors related to DAG structure validation
     * Examples: circular dependencies, missing nodes, etc.
     */
    VALIDATION_ERROR,
    
    /**
     * Errors that occur during task execution
     * Examples: plugin failures, task timeouts, etc.
     */
    EXECUTION_ERROR,
    
    /**
     * System-level errors not directly related to tasks
     * Examples: logging failures, resource exhaustion, etc.
     */
    SYSTEM_ERROR,
    
    // Enhanced error types for robustness framework
    /**
     * Network connectivity or communication errors
     */
    NETWORK_ERROR,
    
    /**
     * Authentication or authorization failures
     */
    AUTHENTICATION_ERROR,
    
    /**
     * Rate limiting or quota exceeded errors
     */
    RATE_LIMITED,
    
    /**
     * Resource exhaustion (memory, disk, etc.)
     */
    RESOURCE_EXHAUSTED,
    
    /**
     * Operation timeout errors
     */
    TIMEOUT,
    
    /**
     * Operation was cancelled
     */
    CANCELLED,
    
    /**
     * Circuit breaker is open
     */
    CIRCUIT_BREAKER_OPEN,
    
    /**
     * Unknown or unspecified error
     */
    UNKNOWN,
    
    /**
     * Input/output errors (file operations, etc.)
     */
    IO_ERROR
}