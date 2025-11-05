package api.controller;

import api.exception.BusinessRuleException;
import api.exception.CommandPaletteException;
import api.exception.PluginDiscoveryException;
import api.model.ApiResponse;
import api.model.PromptExecutionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for API controllers
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<PromptExecutionResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        // Create a validation error response in our standard format
        String errorMessage = errors.values().iterator().next(); // Get first error message
        PromptExecutionResponse response = PromptExecutionResponse.failure(
            "validation_error", errorMessage, "VALIDATION_ERROR");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle missing request parameter errors
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Missing required parameter: " + ex.getParameterName());
        response.put("parameter", ex.getParameterName());
        response.put("type", ex.getParameterType());
        response.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * Handle CommandPaletteException errors
     */
    @ExceptionHandler(CommandPaletteException.class)
    public ResponseEntity<Map<String, Object>> handleCommandPaletteException(CommandPaletteException ex) {
        logger.error("Command palette error: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", ex.getMessage());
        response.put("errorType", ex.getErrorType().toString());
        response.put("timestamp", Instant.now());
        
        if (ex.hasUserContext()) {
            response.put("userId", ex.getUserId());
        }
        
        if (ex.hasOperationContext()) {
            response.put("operation", ex.getOperation());
        }
        
        if (ex.getContext() != null) {
            response.put("context", ex.getContext());
        }
        
        // Map error types to HTTP status codes
        HttpStatus status = mapCommandPaletteErrorTypeToHttpStatus(ex.getErrorType());
        
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Handle PluginDiscoveryException errors
     */
    @ExceptionHandler(PluginDiscoveryException.class)
    public ResponseEntity<Map<String, Object>> handlePluginDiscoveryException(PluginDiscoveryException ex) {
        logger.error("Plugin discovery error: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", ex.getMessage());
        response.put("errorType", ex.getErrorType().toString());
        response.put("timestamp", Instant.now());
        
        if (ex.hasUserContext()) {
            response.put("userId", ex.getUserId());
        }
        
        if (ex.hasOperationContext()) {
            response.put("operation", ex.getOperation());
        }
        
        if (ex.hasRequestId()) {
            response.put("requestId", ex.getRequestId());
        }
        
        if (ex.getContext() != null) {
            response.put("context", ex.getContext());
        }
        
        // Add specific error handling information
        response.put("isClientError", ex.isClientError());
        response.put("isServerError", ex.isServerError());
        
        if (ex.isRateLimitError()) {
            response.put("retryAfter", "60"); // seconds
        }
        
        if (ex.isCircuitBreakerError()) {
            response.put("serviceStatus", "DEGRADED");
        }
        
        // Map error types to HTTP status codes
        HttpStatus status = mapPluginDiscoveryErrorTypeToHttpStatus(ex.getErrorType());
        
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Handle BusinessRuleException errors
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessRuleException(BusinessRuleException ex) {
        logger.error("Business rule error: {}", ex.getMessage(), ex);
        
        // Map error codes to HTTP status codes
        HttpStatus status = mapBusinessRuleErrorCodeToHttpStatus(ex.getErrorCode());
        
        // Create standardized error response
        ApiResponse<Object> response = ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        response.setCorrelationId(java.util.UUID.randomUUID().toString());
        
        // Add additional details if available
        if (ex.getDetails() != null) {
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("details", ex.getDetails());
            errorDetails.put("timestamp", Instant.now());
            response.setData(errorDetails);
        }
        
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Map BusinessRuleException error codes to HTTP status codes
     */
    private HttpStatus mapBusinessRuleErrorCodeToHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "RULE_PARSE_ERROR", "RULE_VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "RULE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "RULE_CONFLICT_ERROR" -> HttpStatus.CONFLICT;
            case "RULE_PERMISSION_ERROR" -> HttpStatus.FORBIDDEN;
            case "RULE_EXECUTION_ERROR", "BUSINESS_RULE_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    /**
     * Map CommandPaletteException error types to HTTP status codes
     */
    private HttpStatus mapCommandPaletteErrorTypeToHttpStatus(CommandPaletteException.ErrorType errorType) {
        return switch (errorType) {
            case INVALID_REQUEST, VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case PRIVACY_VIOLATION -> HttpStatus.FORBIDDEN;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case SUGGESTION_GENERATION_ERROR, LEARNING_ERROR, ANALYTICS_ERROR, 
                 CONFIGURATION_ERROR, DATA_ACCESS_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    /**
     * Map PluginDiscoveryException error types to HTTP status codes
     */
    private HttpStatus mapPluginDiscoveryErrorTypeToHttpStatus(PluginDiscoveryException.ErrorType errorType) {
        return switch (errorType) {
            // Client errors (4xx)
            case INVALID_REQUEST, VALIDATION_ERROR, PROMPT_VALIDATION_ERROR,
                 FEEDBACK_VALIDATION_ERROR, PREFERENCES_VALIDATION_ERROR,
                 DATA_VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            
            case AUTHENTICATION_ERROR -> HttpStatus.UNAUTHORIZED;
            case AUTHORIZATION_ERROR, PRIVACY_VIOLATION -> HttpStatus.FORBIDDEN;
            
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            
            // Server errors (5xx)
            case SERVICE_UNAVAILABLE, CIRCUIT_BREAKER_OPEN -> HttpStatus.SERVICE_UNAVAILABLE;
            
            case TIMEOUT_EXCEEDED -> HttpStatus.REQUEST_TIMEOUT;
            
            case ANALYSIS_FAILED, RECOMMENDATION_FAILED, FEEDBACK_FAILED,
                 PREFERENCES_UPDATE_FAILED, ANALYTICS_ERROR, METRICS_COLLECTION_ERROR,
                 METRICS_CALCULATION_ERROR, DATA_ACCESS_ERROR, DATA_CORRUPTION_ERROR,
                 CONFIGURATION_ERROR, INVALID_CONFIGURATION, MISSING_CONFIGURATION,
                 BATCH_PROCESSING_FAILED, ASYNC_PROCESSING_FAILED, CACHE_ERROR,
                 UNKNOWN_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            
            // Specific processing errors
            case CONTEXT_ANALYSIS_ERROR, RECOMMENDATION_GENERATION_ERROR,
                 FEEDBACK_STORAGE_ERROR, PREFERENCES_STORAGE_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            
            case INSUFFICIENT_DATA -> HttpStatus.UNPROCESSABLE_ENTITY;
            
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}