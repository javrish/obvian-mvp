package api.exception;

/**
 * Custom exception for plugin discovery operations.
 * 
 * This exception is thrown when specific errors occur during plugin discovery
 * operations such as prompt analysis, recommendation generation, feedback recording,
 * preference updates, and analytics generation. It provides structured error information
 * for proper error handling, user feedback, and monitoring.
 * 
 * Patent Alignment: Supports error handling for Claim 19 (Intelligent Plugin Discovery)
 * ensuring robust operation and proper error reporting for AI-powered plugin
 * recommendation systems.
 * 
 * @author Obvian Labs
 * @since Phase 26.2a
 */
public class PluginDiscoveryException extends RuntimeException {
    
    /**
     * Error types for different plugin discovery operations.
     */
    public enum ErrorType {
        // Analysis errors
        ANALYSIS_FAILED,
        PROMPT_VALIDATION_ERROR,
        CONTEXT_ANALYSIS_ERROR,
        
        // Recommendation errors
        RECOMMENDATION_FAILED,
        RECOMMENDATION_GENERATION_ERROR,
        INSUFFICIENT_DATA,
        
        // Feedback errors
        FEEDBACK_FAILED,
        FEEDBACK_VALIDATION_ERROR,
        FEEDBACK_STORAGE_ERROR,
        
        // Preference errors
        PREFERENCES_UPDATE_FAILED,
        PREFERENCES_VALIDATION_ERROR,
        PREFERENCES_STORAGE_ERROR,
        
        // Analytics errors
        ANALYTICS_ERROR,
        METRICS_COLLECTION_ERROR,
        METRICS_CALCULATION_ERROR,
        
        // Service errors
        SERVICE_UNAVAILABLE,
        CIRCUIT_BREAKER_OPEN,
        RATE_LIMIT_EXCEEDED,
        TIMEOUT_EXCEEDED,
        
        // Data errors
        DATA_ACCESS_ERROR,
        DATA_VALIDATION_ERROR,
        DATA_CORRUPTION_ERROR,
        
        // Configuration errors
        CONFIGURATION_ERROR,
        INVALID_CONFIGURATION,
        MISSING_CONFIGURATION,
        
        // Security errors
        AUTHENTICATION_ERROR,
        AUTHORIZATION_ERROR,
        PRIVACY_VIOLATION,
        
        // Processing errors
        BATCH_PROCESSING_FAILED,
        ASYNC_PROCESSING_FAILED,
        CACHE_ERROR,
        
        // General errors
        INVALID_REQUEST,
        VALIDATION_ERROR,
        UNKNOWN_ERROR
    }
    
    private final ErrorType errorType;
    private final String userId;
    private final String operation;
    private final Object context;
    private final String requestId;
    
    /**
     * Constructor with error type and message.
     * 
     * @param errorType The type of error
     * @param message The error message
     */
    public PluginDiscoveryException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
        this.userId = null;
        this.operation = null;
        this.context = null;
        this.requestId = null;
    }
    
    /**
     * Constructor with error type, message, and cause.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param cause The underlying cause
     */
    public PluginDiscoveryException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.userId = null;
        this.operation = null;
        this.context = null;
        this.requestId = null;
    }
    
    /**
     * Constructor with user and operation context.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param userId The user ID associated with the error
     * @param operation The operation that failed
     */
    public PluginDiscoveryException(ErrorType errorType, String message, String userId, String operation) {
        super(message);
        this.errorType = errorType;
        this.userId = userId;
        this.operation = operation;
        this.context = null;
        this.requestId = null;
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
    public PluginDiscoveryException(ErrorType errorType, String message, String userId, 
                                  String operation, Object context) {
        super(message);
        this.errorType = errorType;
        this.userId = userId;
        this.operation = operation;
        this.context = context;
        this.requestId = null;
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
    public PluginDiscoveryException(ErrorType errorType, String message, Throwable cause,
                                  String userId, String operation, Object context) {
        super(message, cause);
        this.errorType = errorType;
        this.userId = userId;
        this.operation = operation;
        this.context = context;
        this.requestId = null;
    }
    
    /**
     * Constructor with error cause and request ID.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param cause The underlying cause
     * @param userId The user ID associated with the error
     * @param operation The operation that failed
     * @param context Additional context information
     * @param requestId The request ID for tracking
     */
    public PluginDiscoveryException(ErrorType errorType, String message, Throwable cause,
                                  String userId, String operation, Object context, String requestId) {
        super(message, cause);
        this.errorType = errorType;
        this.userId = userId;
        this.operation = operation;
        this.context = context;
        this.requestId = requestId;
    }
    
    /**
     * Constructor with request ID tracking.
     * 
     * @param errorType The type of error
     * @param message The error message
     * @param userId The user ID associated with the error
     * @param operation The operation that failed
     * @param context Additional context information
     * @param requestId The request ID for tracking
     */
    public PluginDiscoveryException(ErrorType errorType, String message, String userId, 
                                  String operation, Object context, String requestId) {
        super(message);
        this.errorType = errorType;
        this.userId = userId;
        this.operation = operation;
        this.context = context;
        this.requestId = requestId;
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
     * Get the request ID for tracking.
     * 
     * @return The request ID or null if not provided
     */
    public String getRequestId() {
        return requestId;
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
     * Check if this exception has request tracking.
     * 
     * @return true if request ID is available
     */
    public boolean hasRequestId() {
        return requestId != null && !requestId.trim().isEmpty();
    }
    
    /**
     * Check if this is a client error (4xx equivalent).
     * 
     * @return true if this is a client-side error
     */
    public boolean isClientError() {
        return switch (errorType) {
            case INVALID_REQUEST, VALIDATION_ERROR, PROMPT_VALIDATION_ERROR,
                 FEEDBACK_VALIDATION_ERROR, PREFERENCES_VALIDATION_ERROR,
                 DATA_VALIDATION_ERROR, AUTHENTICATION_ERROR, AUTHORIZATION_ERROR -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this is a server error (5xx equivalent).
     * 
     * @return true if this is a server-side error
     */
    public boolean isServerError() {
        return switch (errorType) {
            case SERVICE_UNAVAILABLE, DATA_ACCESS_ERROR, CONFIGURATION_ERROR,
                 ANALYTICS_ERROR, METRICS_COLLECTION_ERROR, BATCH_PROCESSING_FAILED,
                 ASYNC_PROCESSING_FAILED, CACHE_ERROR, UNKNOWN_ERROR -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this is a rate limiting error.
     * 
     * @return true if this is a rate limiting error
     */
    public boolean isRateLimitError() {
        return errorType == ErrorType.RATE_LIMIT_EXCEEDED;
    }
    
    /**
     * Check if this is a circuit breaker error.
     * 
     * @return true if this is a circuit breaker error
     */
    public boolean isCircuitBreakerError() {
        return errorType == ErrorType.CIRCUIT_BREAKER_OPEN;
    }
    
    // Static factory methods for common error scenarios
    
    /**
     * Create a prompt analysis error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param cause The underlying cause
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException analysisError(String message, String userId, Throwable cause) {
        return new PluginDiscoveryException(ErrorType.ANALYSIS_FAILED, message, cause, 
                                          userId, "prompt_analysis", null);
    }
    
    /**
     * Create a recommendation generation error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param prompt The prompt being processed
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException recommendationError(String message, String userId, String prompt) {
        return new PluginDiscoveryException(ErrorType.RECOMMENDATION_FAILED, message, userId, 
                                          "recommendation_generation", prompt);
    }
    
    /**
     * Create a feedback recording error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param recommendationId The recommendation ID
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException feedbackError(String message, String userId, String recommendationId) {
        return new PluginDiscoveryException(ErrorType.FEEDBACK_FAILED, message, userId, 
                                          "feedback_recording", recommendationId);
    }
    
    /**
     * Create a preferences update error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param cause The underlying cause
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException preferencesError(String message, String userId, Throwable cause) {
        return new PluginDiscoveryException(ErrorType.PREFERENCES_UPDATE_FAILED, message, cause,
                                          userId, "preferences_update", null);
    }
    
    /**
     * Create an analytics error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param cause The underlying cause
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException analyticsError(String message, String userId, Throwable cause) {
        return new PluginDiscoveryException(ErrorType.ANALYTICS_ERROR, message, cause, 
                                          userId, "analytics_generation", null);
    }
    
    /**
     * Create a rate limit exceeded error.
     * 
     * @param userId The user ID
     * @param operation The operation being rate limited
     * @param requestCount The current request count
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException rateLimitExceeded(String userId, String operation, int requestCount) {
        return new PluginDiscoveryException(ErrorType.RATE_LIMIT_EXCEEDED, 
                                          "Rate limit exceeded for user", userId, 
                                          operation, requestCount);
    }
    
    /**
     * Create a circuit breaker open error.
     * 
     * @param userId The user ID
     * @param operation The operation being circuit broken
     * @param failureCount The current failure count
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException circuitBreakerOpen(String userId, String operation, int failureCount) {
        return new PluginDiscoveryException(ErrorType.CIRCUIT_BREAKER_OPEN, 
                                          "Circuit breaker is open", userId, 
                                          operation, failureCount);
    }
    
    /**
     * Create a validation error.
     * 
     * @param message The error message
     * @param validationDetails The validation details
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException validationError(String message, Object validationDetails) {
        return new PluginDiscoveryException(ErrorType.VALIDATION_ERROR, message, null, 
                                          "input_validation", validationDetails);
    }
    
    /**
     * Create a service unavailable error.
     * 
     * @param message The error message
     * @param cause The underlying cause
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException serviceUnavailable(String message, Throwable cause) {
        return new PluginDiscoveryException(ErrorType.SERVICE_UNAVAILABLE, message, cause, 
                                          null, "service_health", null);
    }
    
    /**
     * Create a batch processing error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param batchSize The batch size being processed
     * @param cause The underlying cause
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException batchProcessingError(String message, String userId, 
                                                              int batchSize, Throwable cause) {
        return new PluginDiscoveryException(ErrorType.BATCH_PROCESSING_FAILED, message, cause,
                                          userId, "batch_processing", batchSize);
    }
    
    /**
     * Create an async processing error.
     * 
     * @param message The error message
     * @param userId The user ID
     * @param requestId The request ID
     * @param cause The underlying cause
     * @return PluginDiscoveryException instance
     */
    public static PluginDiscoveryException asyncProcessingError(String message, String userId, 
                                                              String requestId, Throwable cause) {
        return new PluginDiscoveryException(ErrorType.ASYNC_PROCESSING_FAILED, 
                                           message, cause, userId, "async_processing", null, requestId);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PluginDiscoveryException{");
        sb.append("errorType=").append(errorType);
        sb.append(", message='").append(getMessage()).append("'");
        if (userId != null) {
            sb.append(", userId='").append(userId).append("'");
        }
        if (operation != null) {
            sb.append(", operation='").append(operation).append("'");
        }
        if (requestId != null) {
            sb.append(", requestId='").append(requestId).append("'");
        }
        if (context != null) {
            sb.append(", context=").append(context);
        }
        sb.append("}");
        return sb.toString();
    }
}