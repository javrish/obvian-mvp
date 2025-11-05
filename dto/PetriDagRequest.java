package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.PetriNet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for generating DAG representation from Petri net.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for generating DAG representation from Petri net")
public class PetriDagRequest {

    @NotNull
    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0", required = true)
    private String schemaVersion;

    @Valid
    @NotNull
    @JsonProperty("petriNet")
    @Schema(description = "Petri net to project as DAG", required = true)
    private PetriNet petriNet;

    @JsonProperty("includeMetadata")
    @Schema(description = "Include projection metadata and notes", example = "true")
    private Boolean includeMetadata;

    // Constructors
    public PetriDagRequest() {}

    public PetriDagRequest(String schemaVersion, PetriNet petriNet) {
        this.schemaVersion = schemaVersion;
        this.petriNet = petriNet;
    }

    public PetriDagRequest(String schemaVersion, PetriNet petriNet, Boolean includeMetadata) {
        this.schemaVersion = schemaVersion;
        this.petriNet = petriNet;
        this.includeMetadata = includeMetadata;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public PetriNet getPetriNet() { return petriNet; }
    public void setPetriNet(PetriNet petriNet) { this.petriNet = petriNet; }

    public Boolean getIncludeMetadata() { return includeMetadata; }
    public void setIncludeMetadata(Boolean includeMetadata) { this.includeMetadata = includeMetadata; }

    @Override
    public String toString() {
        return "PetriDagRequest{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", petriNet=" + petriNet +
                ", includeMetadata=" + includeMetadata +
                '}';
    }
}