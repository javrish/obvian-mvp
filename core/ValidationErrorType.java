package core;

/**
 * Types of validation errors that can occur during DAG validation
 */
public enum ValidationErrorType {
    /**
     * DAG structure is invalid (null, empty when expected to have nodes)
     */
    INVALID_STRUCTURE,
    
    /**
     * Missing root node when DAG has nodes
     */
    MISSING_ROOT_NODE,
    
    /**
     * Node has invalid or missing ID
     */
    INVALID_NODE_ID,
    
    /**
     * Node has invalid or missing action
     */
    INVALID_NODE_ACTION,
    
    /**
     * Node references dependencies that don't exist in the DAG
     */
    MISSING_DEPENDENCY,
    
    /**
     * Circular dependency detected in the DAG
     */
    CIRCULAR_DEPENDENCY,
    
    /**
     * Nodes exist that are not reachable from the root node
     */
    ORPHANED_NODES,
    
    /**
     * Node has invalid parameter configuration
     */
    INVALID_PARAMETERS,
    
    /**
     * Plugin required by node is not available
     */
    PLUGIN_UNAVAILABLE,
    
    /**
     * Node has invalid retry configuration
     */
    INVALID_RETRY_CONFIG,
    
    /**
     * Node references invalid fallback plugin
     */
    INVALID_FALLBACK_PLUGIN
}