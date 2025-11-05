package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.PetriIntentSpec;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for natural language parsing results.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing parsed Petri net intent specification")
public class PetriParseResponse {

    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0")
    private String schemaVersion;

    @JsonProperty("intent")
    @Schema(description = "Parsed intent specification")
    private PetriIntentSpec intent;

    @JsonProperty("templateUsed")
    @Schema(description = "Template that was used for parsing", example = "devops-ci-cd")
    private String templateUsed;

    @JsonProperty("confidence")
    @Schema(description = "Parse confidence score", example = "0.95")
    private Double confidence;

    // Constructors
    public PetriParseResponse() {}

    public PetriParseResponse(String schemaVersion, PetriIntentSpec intent) {
        this.schemaVersion = schemaVersion;
        this.intent = intent;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public PetriIntentSpec getIntent() { return intent; }
    public void setIntent(PetriIntentSpec intent) { this.intent = intent; }

    public String getTemplateUsed() { return templateUsed; }
    public void setTemplateUsed(String templateUsed) { this.templateUsed = templateUsed; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    // Factory methods
    public static PetriParseResponse success(String schemaVersion, PetriIntentSpec intent, String templateUsed, double confidence) {
        PetriParseResponse response = new PetriParseResponse(schemaVersion, intent);
        response.setTemplateUsed(templateUsed);
        response.setConfidence(confidence);
        return response;
    }

    @Override
    public String toString() {
        return "PetriParseResponse{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", intent=" + intent +
                ", templateUsed='" + templateUsed + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}