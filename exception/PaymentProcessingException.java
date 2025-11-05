package api.exception;

/**
 * Exception for payment processing errors
 * 
 * Custom exception for handling payment-related errors including:
 * - Payment validation failures
 * - Stripe API errors
 * - Security violations
 * - Rate limiting
 * - Business rule violations
 */
public class PaymentProcessingException extends RuntimeException {
    
    private String errorCode;
    private String userMessage;
    
    public PaymentProcessingException(String message) {
        super(message);
        this.userMessage = message;
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.userMessage = message;
    }
    
    public PaymentProcessingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = message;
    }
    
    public PaymentProcessingException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public PaymentProcessingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = message;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getUserMessage() {
        return userMessage != null ? userMessage : getMessage();
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
}