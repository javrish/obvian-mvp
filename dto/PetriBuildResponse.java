package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.PetriNet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for Petri net build results.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing built Petri net")
public class PetriBuildResponse {

    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0")
    private String schemaVersion;

    @JsonProperty("petriNet")
    @Schema(description = "Built Petri net")
    private PetriNet petriNet;

    @JsonProperty("buildNotes")
    @Schema(description = "Construction notes and warnings")
    private List<String> buildNotes;

    @JsonProperty("dag")
    @Schema(description = "Optional DAG representation if requested")
    private Object dag;

    // Constructors
    public PetriBuildResponse() {}

    public PetriBuildResponse(String schemaVersion, PetriNet petriNet) {
        this.schemaVersion = schemaVersion;
        this.petriNet = petriNet;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public PetriNet getPetriNet() { return petriNet; }
    public void setPetriNet(PetriNet petriNet) { this.petriNet = petriNet; }

    public List<String> getBuildNotes() { return buildNotes; }
    public void setBuildNotes(List<String> buildNotes) { this.buildNotes = buildNotes; }

    public Object getDag() { return dag; }
    public void setDag(Object dag) { this.dag = dag; }

    // Factory methods
    public static PetriBuildResponse success(String schemaVersion, PetriNet petriNet) {
        return new PetriBuildResponse(schemaVersion, petriNet);
    }

    public static PetriBuildResponse success(String schemaVersion, PetriNet petriNet, List<String> buildNotes) {
        PetriBuildResponse response = new PetriBuildResponse(schemaVersion, petriNet);
        response.setBuildNotes(buildNotes);
        return response;
    }

    @Override
    public String toString() {
        return "PetriBuildResponse{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", petriNet=" + petriNet +
                ", buildNotes=" + (buildNotes != null ? buildNotes.size() + " notes" : "no notes") +
                ", dag=" + (dag != null ? "included" : "not included") +
                '}';
    }
}