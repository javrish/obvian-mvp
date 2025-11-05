package core;

import java.util.List;

/**
 * Represents a validation error with detailed information
 */
public class ValidationError {
    private final ValidationErrorType type;
    private final String message;
    private final String nodeId;
    private final List<String> affectedNodes;
    private final String suggestion;
    
    public ValidationError(ValidationErrorType type, String message) {
        this(type, message, null, null, null);
    }
    
    public ValidationError(ValidationErrorType type, String message, String nodeId) {
        this(type, message, nodeId, null, null);
    }
    
    public ValidationError(ValidationErrorType type, String message, String nodeId, List<String> affectedNodes) {
        this(type, message, nodeId, affectedNodes, null);
    }
    
    public ValidationError(ValidationErrorType type, String message, String nodeId, List<String> affectedNodes, String suggestion) {
        this.type = type;
        this.message = message;
        this.nodeId = nodeId;
        this.affectedNodes = affectedNodes;
        this.suggestion = suggestion;
    }
    
    public ValidationErrorType getType() {
        return type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public List<String> getAffectedNodes() {
        return affectedNodes;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    /**
     * Get a detailed error message including suggestions if available
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(message);
        
        if (nodeId != null) {
            sb.append(" (Node: ").append(nodeId).append(")");
        }
        
        if (affectedNodes != null && !affectedNodes.isEmpty()) {
            sb.append(" (Affected nodes: ").append(String.join(", ", affectedNodes)).append(")");
        }
        
        if (suggestion != null) {
            sb.append(" - Suggestion: ").append(suggestion);
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "ValidationError{" +
                "type=" + type +
                ", message='" + message + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", affectedNodes=" + affectedNodes +
                ", suggestion='" + suggestion + '\'' +
                '}';
    }
}