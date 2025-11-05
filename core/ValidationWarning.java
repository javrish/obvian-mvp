package core;

/**
 * Represents a validation warning that doesn't prevent execution but indicates potential issues
 */
public class ValidationWarning {
    private final ValidationWarningType type;
    private final String message;
    private final String nodeId;
    private final String suggestion;
    
    public ValidationWarning(ValidationWarningType type, String message) {
        this(type, message, null, null);
    }
    
    public ValidationWarning(ValidationWarningType type, String message, String nodeId) {
        this(type, message, nodeId, null);
    }
    
    public ValidationWarning(ValidationWarningType type, String message, String nodeId, String suggestion) {
        this.type = type;
        this.message = message;
        this.nodeId = nodeId;
        this.suggestion = suggestion;
    }
    
    public ValidationWarningType getType() {
        return type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    /**
     * Get a detailed warning message including suggestions if available
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(message);
        
        if (nodeId != null) {
            sb.append(" (Node: ").append(nodeId).append(")");
        }
        
        if (suggestion != null) {
            sb.append(" - Suggestion: ").append(suggestion);
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "ValidationWarning{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", suggestion='" + suggestion + '\'' +
                '}';
    }
}