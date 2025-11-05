package api.dto;

import core.petri.PetriNet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for DAG projection endpoint.
 */
public class PetriDagRequest {

    @NotNull(message = "Petri net cannot be null")
    @Valid
    private PetriNet petriNet;

    private Boolean includeMetadata = false;
    private java.util.Map<String, Object> projectionOptions;

    public PetriDagRequest() {}

    public PetriDagRequest(PetriNet petriNet) {
        this.petriNet = petriNet;
    }

    public PetriDagRequest(PetriNet petriNet, Boolean includeMetadata) {
        this.petriNet = petriNet;
        this.includeMetadata = includeMetadata;
    }

    // Getters and setters
    public PetriNet getPetriNet() { return petriNet; }
    public void setPetriNet(PetriNet petriNet) { this.petriNet = petriNet; }

    public Boolean getIncludeMetadata() { return includeMetadata; }
    public void setIncludeMetadata(Boolean includeMetadata) { this.includeMetadata = includeMetadata; }

    public java.util.Map<String, Object> getProjectionOptions() { return projectionOptions; }
    public void setProjectionOptions(java.util.Map<String, Object> projectionOptions) { this.projectionOptions = projectionOptions; }
}