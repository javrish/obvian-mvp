package core;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that wraps a DAG to implement the WorkflowModel interface.
 * Provides unified access to DAG workflows through the common interface.
 * 
 * @author Obvian Labs
 * @since Task 1 - Petri net DAG POC
 */
public class DagWorkflow implements WorkflowModel {
    
    private final DAG dag;
    
    public DagWorkflow(DAG dag) {
        if (dag == null) {
            throw new IllegalArgumentException("DAG cannot be null");
        }
        this.dag = dag;
    }
    
    @Override
    public ModelType type() {
        return ModelType.DAG;
    }
    
    @Override
    public String getId() {
        return dag.getId();
    }
    
    @Override
    public String getName() {
        return dag.getName();
    }
    
    @Override
    public String getDescription() {
        return (String) dag.getMetadata().get("description");
    }
    
    @Override
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        if (dag.getNodes() == null || dag.getNodes().isEmpty()) {
            errors.add("DAG must have at least one node");
        }
        
        if (dag.getRootNode() == null) {
            errors.add("DAG must have a root node");
        }
        
        // Add any existing validation warnings
        errors.addAll(dag.getValidationWarnings());
        
        return errors;
    }
    
    /**
     * Get the underlying DAG
     */
    public DAG getDag() {
        return dag;
    }
    
    @Override
    public String toString() {
        return "DagWorkflow{" +
                "dag=" + dag +
                '}';
    }
}