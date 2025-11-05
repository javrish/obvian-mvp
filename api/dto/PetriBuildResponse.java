package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.PetriNet;

public class PetriBuildResponse {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("petriNet")
    private PetriNet petriNet;

    @JsonProperty("message")
    private String message;

    @JsonProperty("buildTime")
    private long buildTime;

    public PetriBuildResponse() {}

    public PetriBuildResponse(boolean success, PetriNet petriNet, String message, long buildTime) {
        this.success = success;
        this.petriNet = petriNet;
        this.message = message;
        this.buildTime = buildTime;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public PetriNet getPetriNet() { return petriNet; }
    public void setPetriNet(PetriNet petriNet) { this.petriNet = petriNet; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getBuildTime() { return buildTime; }
    public void setBuildTime(long buildTime) { this.buildTime = buildTime; }
}