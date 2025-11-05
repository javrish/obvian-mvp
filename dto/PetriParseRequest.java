package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for parsing natural language into PetriIntentSpec.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for parsing natural language into Petri net intent specification")
public class PetriParseRequest {

    @NotNull
    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0", required = true)
    private String schemaVersion;

    @NotBlank
    @JsonProperty("text")
    @Schema(description = "Natural language workflow description",
            example = "Every time I push code: run tests; if pass deploy to staging; if fail alert Slack",
            required = true)
    private String text;

    @JsonProperty("templateHint")
    @Schema(description = "Optional template hint to guide parsing", example = "devops")
    private String templateHint;

    // Constructors
    public PetriParseRequest() {}

    public PetriParseRequest(String schemaVersion, String text) {
        this.schemaVersion = schemaVersion;
        this.text = text;
    }

    public PetriParseRequest(String schemaVersion, String text, String templateHint) {
        this.schemaVersion = schemaVersion;
        this.text = text;
        this.templateHint = templateHint;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getTemplateHint() { return templateHint; }
    public void setTemplateHint(String templateHint) { this.templateHint = templateHint; }

    @Override
    public String toString() {
        return "PetriParseRequest{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", text='" + (text != null ? text.substring(0, Math.min(text.length(), 50)) + "..." : "null") + '\'' +
                ", templateHint='" + templateHint + '\'' +
                '}';
    }
}