package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.PetriIntentSpec;

public class PetriParseResponse {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("intentSpec")
    private PetriIntentSpec intentSpec;

    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("templateUsed")
    private String templateUsed;

    @JsonProperty("message")
    private String message;

    public PetriParseResponse() {}

    public PetriParseResponse(boolean success, PetriIntentSpec intentSpec, double confidence, String templateUsed, String message) {
        this.success = success;
        this.intentSpec = intentSpec;
        this.confidence = confidence;
        this.templateUsed = templateUsed;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public PetriIntentSpec getIntentSpec() { return intentSpec; }
    public void setIntentSpec(PetriIntentSpec intentSpec) { this.intentSpec = intentSpec; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getTemplateUsed() { return templateUsed; }
    public void setTemplateUsed(String templateUsed) { this.templateUsed = templateUsed; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    // Factory methods
    public static PetriParseResponse success(String schemaVersion, PetriIntentSpec intentSpec, String templateUsed, double confidence) {
        PetriParseResponse response = new PetriParseResponse();
        response.success = true;
        response.intentSpec = intentSpec;
        response.templateUsed = templateUsed;
        response.confidence = confidence;
        response.message = "Successfully parsed input";
        return response;
    }

    // For compatibility with the controller
    public String getSchemaVersion() { return "1.0"; }
}