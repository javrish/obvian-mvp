package core.petri;

import core.WorkflowModel;
import core.ModelType;

import java.util.List;

/**
 * Adapter that wraps a PetriNet to implement the WorkflowModel interface.
 * Provides unified access to Petri net workflows through the common interface.
 * 
 * @author Obvian Labs
 * @since Task 1 - Petri net DAG POC
 */
public class PetriWorkflow implements WorkflowModel {
    
    private final PetriNet petriNet;
    
    public PetriWorkflow(PetriNet petriNet) {
        if (petriNet == null) {
            throw new IllegalArgumentException("PetriNet cannot be null");
        }
        this.petriNet = petriNet;
    }
    
    @Override
    public ModelType type() {
        return ModelType.PETRI;
    }
    
    @Override
    public String getId() {
        return petriNet.getId();
    }
    
    @Override
    public String getName() {
        return petriNet.getName();
    }
    
    @Override
    public String getDescription() {
        return petriNet.getDescription();
    }
    
    @Override
    public List<String> validate() {
        return petriNet.validate();
    }
    
    /**
     * Get the underlying Petri net
     */
    public PetriNet getPetriNet() {
        return petriNet;
    }
    
    @Override
    public String toString() {
        return "PetriWorkflow{" +
                "petriNet=" + petriNet +
                '}';
    }
}