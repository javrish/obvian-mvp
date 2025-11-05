package api.dto;

import core.petri.PetriIntentSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for Petri net building endpoint.
 */
public class PetriBuildRequest {

    @NotNull(message = "Intent specification cannot be null")
    @Valid
    private PetriIntentSpec intent;

    private Boolean generateDag = false;
    private java.util.Map<String, Object> metadata;

    public PetriBuildRequest() {}

    public PetriBuildRequest(PetriIntentSpec intent) {
        this.intent = intent;
    }

    public PetriBuildRequest(PetriIntentSpec intent, Boolean generateDag) {
        this.intent = intent;
        this.generateDag = generateDag;
    }

    // Getters and setters
    public PetriIntentSpec getIntent() { return intent; }
    public void setIntent(PetriIntentSpec intent) { this.intent = intent; }

    public Boolean getGenerateDag() { return generateDag; }
    public void setGenerateDag(Boolean generateDag) { this.generateDag = generateDag; }

    public java.util.Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }
}