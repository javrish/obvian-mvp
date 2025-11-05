package core;

/**
 * Common interface for workflow models in the Obvian system.
 * Provides a unified abstraction over different modeling approaches (DAG, Petri net).
 * 
 * @author Obvian Labs
 * @since Task 1 - Petri net DAG POC
 */
public interface WorkflowModel {
    
    /**
     * Get the model type (DAG or PETRI)
     */
    ModelType type();
    
    /**
     * Get the unique identifier for this workflow model
     */
    String getId();
    
    /**
     * Get the human-readable name for this workflow model
     */
    String getName();
    
    /**
     * Get the description of this workflow model
     */
    String getDescription();
    
    /**
     * Validate the workflow model structure
     * @return list of validation errors, empty if valid
     */
    java.util.List<String> validate();
}