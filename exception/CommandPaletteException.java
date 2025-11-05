package api.exception;

/**
 * Custom exception for command palette operations.
 * 
 * This exception is thrown when specific errors occur during command palette
 * operations such as suggestion generation, learning, analytics, or configuration
 * management. It provides structured error information for proper error handling
 * and user feedback.
 * 
 * Patent Alignment: Supports error handling for Product Patent 25 (ContextualCommandPalette)
 * ensuring robust operation and proper error reporting.
 * 
 * @author Obvian Labs
 * @since Phase 26.1d
 */
public class CommandPaletteException extends RuntimeException {
    
    /**
     * Error types for different command palette operations.
     */
    public enum ErrorType {
        SUGGESTION_GENERATION_ERROR,
        LEARNING_ERROR,
        ANALYTICS_ERROR,
        CONFIGURATION_ERROR,
        RATE_LIMIT_EXCEEDED,
        INVALID_REQUEST,
        SERVICE_UNAVAILABLE,
        DATA_ACCESS_ERROR,
        PRIVACY_VIOLATION,
        VALIDATION_ERROR
    }
    
    private final ErrorType errorType;
    private final String userId;
    private final String operation;
    private final Object context;
    
    /**
     * Constructor with error type and message.
     * 
     * @param errorType The type of error
     * @param message The error message
     */
    public CommandPaletteException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.userId = null;
        this.operation = null;
        this.context = null;
    }
    
    /**
     * Constructor with error type, message, and cause.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param cause The underlying cause
     */
    public CommandPaletteException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.userId = null;
        this.operation = null;
        this.context = null;
    }
    
    /**
     * Constructor with full context information.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param userId The user ID associated with the error
     * @param operation The operation that failed
     * @param context Additional context information
     */
    public CommandPaletteException(ErrorType errorType, String message, String userId, 
                                 String operation, Object context) {
        super(message);
        this.errorType = errorType;
        this.userId = userId;
        this.operation = operation;
        this.context = context;
    }
    
    /**
     * Constructor with full context information and cause.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param cause The underlying cause
     * @param userId The user ID associated with the error
     * @param operation The operation that failed
     * @param context Additional context information
     */
    public CommandPaletteException(ErrorType errorType, String message, Throwable cause,
                                 String userId, String operation, Object context) {
        super(message, cause);
        this.errorType = errorType;
        this.userId = userId;
        this.operation = operation;
        this.context = context;
    }
    
    /**
     * Get the error type.
     * 
     * @return The error type
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Get the user ID associated with the error.
     * 
     * @return The user ID or null if not applicable
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * Get the operation that failed.
     * 
     * @return The operation name or null if not specified
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * Get additional context information.
     * 
     * @return The context object or null if not provided
     */
    public Object getContext() {
        return context;
    }
    
    /**
     * Check if this exception has user context.
     * 
     * @return true if user ID is available
     */
    public boolean hasUserContext() {
        return userId != null && !userId.trim().isEmpty();
    }
    
    /**
     * Check if this exception has operation context.
     * 
     * @return true if operation is specified
     */
    public boolean hasOperationContext() {
        return operation != null && !operation.trim().isEmpty();
    }
    
    /**
     * Create a suggestion generation error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param cause The underlying cause
     * @return CommandPaletteException instance
     */
    public static CommandPaletteException suggestionError(String message, String userId, Throwable cause) {
        return new CommandPaletteException(ErrorType.SUGGESTION_GENERATION_ERROR, message, cause, 
                                         userId, "suggestion_generation", null);
    }
    
    /**
     * Create a learning error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param command The command being learned
     * @return CommandPaletteException instance
     */
    public static CommandPaletteException learningError(String message, String userId, String command) {
        return new CommandPaletteException(ErrorType.LEARNING_ERROR, message, userId, 
                                         "command_learning", command);
    }
    
    /**
     * Create an analytics error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param cause The underlying cause
     * @return CommandPaletteException instance
     */
    public static CommandPaletteException analyticsError(String message, String userId, Throwable cause) {
        return new CommandPaletteException(ErrorType.ANALYTICS_ERROR, message, cause, 
                                         userId, "analytics_generation", null);
    }
    
    /**
     * Create a configuration error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @return CommandPaletteException instance
     */
    public static CommandPaletteException configurationError(String message, String userId) {
        return new CommandPaletteException(ErrorType.CONFIGURATION_ERROR, message, userId, 
                                         "configuration_update", null);
    }
    
    /**
     * Create a rate limit exceeded error.
     * 
     * @param userId The user ID
     * @param requestCount The current request count
     * @return CommandPaletteException instance
     */
    public static CommandPaletteException rateLimitExceeded(String userId, int requestCount) {
        return new CommandPaletteException(ErrorType.RATE_LIMIT_EXCEEDED, 
                                         "Rate limit exceeded for user", userId, 
                                         "rate_limiting", requestCount);
    }
    
    /**
     * Create an invalid request error.
     * 
     * @param message The error message
     * @param validationDetails The validation details
     * @return CommandPaletteException instance
     */
    public static CommandPaletteException invalidRequest(String message, Object validationDetails) {
        return new CommandPaletteException(ErrorType.INVALID_REQUEST, message, null, 
                                         "request_validation", validationDetails);
    }
    
    /**
     * Create a service unavailable error.
     * 
     * @param message The error message
     * @param cause The underlying cause
     * @return CommandPaletteException instance
     */
    public static CommandPaletteException serviceUnavailable(String message, Throwable cause) {
        return new CommandPaletteException(ErrorType.SERVICE_UNAVAILABLE, message, cause, 
                                         null, "service_health", null);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CommandPaletteException{");
        sb.append("errorType=").append(errorType);
        sb.append(", message='").append(getMessage()).append("'");
        if (userId != null) {
            sb.append(", userId='").append(userId).append("'");
        }
        if (operation != null) {
            sb.append(", operation='").append(operation).append("'");
        }
        if (context != null) {
            sb.append(", context=").append(context);
        }
        sb.append("}");
        return sb.toString();
    }
}