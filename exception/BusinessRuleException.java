package api.exception;

/**
 * Exception thrown for business rule related errors
 */
public class BusinessRuleException extends RuntimeException {
    
    private final String errorCode;
    private final String details;
    
    public BusinessRuleException(String message) {
        super(message);
        this.errorCode = "BUSINESS_RULE_ERROR";
        this.details = null;
    }
    
    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_RULE_ERROR";
        this.details = null;
    }
    
    public BusinessRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public BusinessRuleException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public BusinessRuleException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public BusinessRuleException(String errorCode, String message, String details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getDetails() {
        return details;
    }
    
    // Static factory methods for common exceptions
    public static BusinessRuleException parseError(String message) {
        return new BusinessRuleException("RULE_PARSE_ERROR", "Failed to parse business rule: " + message);
    }
    
    public static BusinessRuleException validationError(String message) {
        return new BusinessRuleException("RULE_VALIDATION_ERROR", "Rule validation failed: " + message);
    }
    
    public static BusinessRuleException conflictError(String message) {
        return new BusinessRuleException("RULE_CONFLICT_ERROR", "Rule conflict detected: " + message);
    }
    
    public static BusinessRuleException executionError(String message) {
        return new BusinessRuleException("RULE_EXECUTION_ERROR", "Rule execution failed: " + message);
    }
    
    public static BusinessRuleException notFoundError(String ruleId) {
        return new BusinessRuleException("RULE_NOT_FOUND", "Business rule not found: " + ruleId);
    }
    
    public static BusinessRuleException permissionError(String message) {
        return new BusinessRuleException("RULE_PERMISSION_ERROR", "Insufficient permissions: " + message);
    }
}