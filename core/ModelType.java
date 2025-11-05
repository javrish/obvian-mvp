package core;

/**
 * Enumeration of supported workflow model types in the Obvian system.
 * Used to distinguish between different modeling approaches for validation and execution.
 * 
 * @author Obvian Labs
 * @since Task 1 - Petri net DAG POC
 */
public enum ModelType {
    /**
     * Directed Acyclic Graph model - traditional task-based workflows
     */
    DAG,
    
    /**
     * Petri net model - formal workflow validation with token-based semantics
     */
    PETRI
}