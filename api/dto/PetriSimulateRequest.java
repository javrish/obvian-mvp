package api.dto;

import core.petri.PetriNet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for Petri net simulation endpoint.
 */
public class PetriSimulateRequest {

    @NotNull(message = "Petri net cannot be null")
    @Valid
    private PetriNet petriNet;

    private SimulationConfig config;

    public PetriSimulateRequest() {}

    public PetriSimulateRequest(PetriNet petriNet) {
        this.petriNet = petriNet;
    }

    public PetriSimulateRequest(PetriNet petriNet, SimulationConfig config) {
        this.petriNet = petriNet;
        this.config = config;
    }

    // Getters and setters
    public PetriNet getPetriNet() { return petriNet; }
    public void setPetriNet(PetriNet petriNet) { this.petriNet = petriNet; }

    public SimulationConfig getConfig() { return config; }
    public void setConfig(SimulationConfig config) { this.config = config; }

    /**
     * Configuration for simulation parameters.
     */
    public static class SimulationConfig {
        private Long seed;
        private String mode;
        private Integer maxSteps;
        private Integer stepDelayMs;
        private Boolean enableTrace;

        // Getters and setters
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
}