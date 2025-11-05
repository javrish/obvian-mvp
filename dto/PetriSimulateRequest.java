package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.PetriNet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for Petri net simulation.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for simulating Petri net execution")
public class PetriSimulateRequest {

    @NotNull
    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0", required = true)
    private String schemaVersion;

    @Valid
    @NotNull
    @JsonProperty("petriNet")
    @Schema(description = "Petri net to simulate", required = true)
    private PetriNet petriNet;

    @JsonProperty("config")
    @Schema(description = "Simulation configuration")
    private SimulationConfig config;

    // Nested class for simulation configuration
    @Schema(description = "Simulation configuration parameters")
    public static class SimulationConfig {
        @JsonProperty("seed")
        @Schema(description = "Random seed for deterministic simulation", example = "42")
        private Long seed;

        @JsonProperty("mode")
        @Schema(description = "Simulation mode", example = "DETERMINISTIC", allowableValues = {"DETERMINISTIC", "INTERACTIVE"})
        private String mode;

        @JsonProperty("maxSteps")
        @Schema(description = "Maximum simulation steps", example = "1000")
        private Integer maxSteps;

        @JsonProperty("stepDelayMs")
        @Schema(description = "Delay between steps in milliseconds", example = "100")
        private Integer stepDelayMs;

        @JsonProperty("enableTrace")
        @Schema(description = "Enable detailed trace logging", example = "true")
        private Boolean enableTrace;

        // Constructors
        public SimulationConfig() {}

        // Getters and Setters
        public Long getSeed() { return seed; }
        public void setSeed(Long seed) { this.seed = seed; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public Integer getMaxSteps() { return maxSteps; }
        public void setMaxSteps(Integer maxSteps) { this.maxSteps = maxSteps; }

        public Integer getStepDelayMs() { return stepDelayMs; }
        public void setStepDelayMs(Integer stepDelayMs) { this.stepDelayMs = stepDelayMs; }

        public Boolean getEnableTrace() { return enableTrace; }
        public void setEnableTrace(Boolean enableTrace) { this.enableTrace = enableTrace; }
    }

    // Constructors
    public PetriSimulateRequest() {}

    public PetriSimulateRequest(String schemaVersion, PetriNet petriNet) {
        this.schemaVersion = schemaVersion;
        this.petriNet = petriNet;
    }

    public PetriSimulateRequest(String schemaVersion, PetriNet petriNet, SimulationConfig config) {
        this.schemaVersion = schemaVersion;
        this.petriNet = petriNet;
        this.config = config;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public PetriNet getPetriNet() { return petriNet; }
    public void setPetriNet(PetriNet petriNet) { this.petriNet = petriNet; }

    public SimulationConfig getConfig() { return config; }
    public void setConfig(SimulationConfig config) { this.config = config; }

    @Override
    public String toString() {
        return "PetriSimulateRequest{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", petriNet=" + petriNet +
                ", config=" + config +
                '}';
    }
}