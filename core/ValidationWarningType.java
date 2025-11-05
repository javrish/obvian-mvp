package core;

/**
 * Types of validation warnings
 */
public enum ValidationWarningType {
    /**
     * Node has no input parameters when they might be expected
     */
    MISSING_PARAMETERS,
    
    /**
     * Node has retry configuration but no fallback plugin
     */
    NO_FALLBACK_PLUGIN,
    
    /**
     * Plugin health check indicates potential issues
     */
    PLUGIN_HEALTH_WARNING,
    
    /**
     * Node configuration may cause performance issues
     */
    PERFORMANCE_WARNING
}