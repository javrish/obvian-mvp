package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.PetriNetValidationResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for Petri net validation results.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing Petri net validation results")
public class PetriValidateResponse {

    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0")
    private String schemaVersion;

    @JsonProperty("report")
    @Schema(description = "Validation report with detailed results")
    private PetriNetValidationResult report;

    // Constructors
    public PetriValidateResponse() {}

    public PetriValidateResponse(String schemaVersion, PetriNetValidationResult report) {
        this.schemaVersion = schemaVersion;
        this.report = report;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public PetriNetValidationResult getReport() { return report; }
    public void setReport(PetriNetValidationResult report) { this.report = report; }

    // Factory methods
    public static PetriValidateResponse success(String schemaVersion, PetriNetValidationResult report) {
        return new PetriValidateResponse(schemaVersion, report);
    }

    @Override
    public String toString() {
        return "PetriValidateResponse{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", report=" + report +
                '}';
    }
}