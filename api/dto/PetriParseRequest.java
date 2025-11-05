package api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for natural language parsing endpoint.
 */
public class PetriParseRequest {

    @NotBlank(message = "Text cannot be blank")
    @Size(max = 10000, message = "Text cannot exceed 10000 characters")
    private String text;

    private String templateHint;
    private java.util.Map<String, Object> metadata;

    public PetriParseRequest() {}

    public PetriParseRequest(String text) {
        this.text = text;
    }

    public PetriParseRequest(String text, String templateHint) {
        this.text = text;
        this.templateHint = templateHint;
    }

    // Getters and setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getTemplateHint() { return templateHint; }
    public void setTemplateHint(String templateHint) { this.templateHint = templateHint; }

    public java.util.Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(java.util.Map<String, Object> metadata) { this.metadata = metadata; }
}