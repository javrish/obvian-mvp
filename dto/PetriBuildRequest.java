package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.PetriIntentSpec;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for building Petri net from intent specification.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for building Petri net from intent specification")
public class PetriBuildRequest {

    @NotNull
    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0", required = true)
    private String schemaVersion;

    @Valid
    @NotNull
    @JsonProperty("intent")
    @Schema(description = "Intent specification to build Petri net from", required = true)
    private PetriIntentSpec intent;

    @JsonProperty("generateDag")
    @Schema(description = "Whether to also generate DAG representation", example = "false")
    private Boolean generateDag;

    // Constructors
    public PetriBuildRequest() {}

    public PetriBuildRequest(String schemaVersion, PetriIntentSpec intent) {
        this.schemaVersion = schemaVersion;
        this.intent = intent;
    }

    public PetriBuildRequest(String schemaVersion, PetriIntentSpec intent, Boolean generateDag) {
        this.schemaVersion = schemaVersion;
        this.intent = intent;
        this.generateDag = generateDag;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public PetriIntentSpec getIntent() { return intent; }
    public void setIntent(PetriIntentSpec intent) { this.intent = intent; }

    public Boolean getGenerateDag() { return generateDag; }
    public void setGenerateDag(Boolean generateDag) { this.generateDag = generateDag; }

    @Override
    public String toString() {
        return "PetriBuildRequest{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", intent=" + intent +
                ", generateDag=" + generateDag +
                '}';
    }
}