package api.dto;

import core.petri.PetriNet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for Petri net validation endpoint.
 */
public class PetriValidateRequest {

    @NotNull(message = "Petri net cannot be null")
    @Valid
    private PetriNet petriNet;

    private ValidationConfig config;

    public PetriValidateRequest() {}

    public PetriValidateRequest(PetriNet petriNet) {
        this.petriNet = petriNet;
    }

    public PetriValidateRequest(PetriNet petriNet, ValidationConfig config) {
        this.petriNet = petriNet;
        this.config = config;
    }

    // Getters and setters
    public PetriNet getPetriNet() { return petriNet; }
    public void setPetriNet(PetriNet petriNet) { this.petriNet = petriNet; }

    public ValidationConfig getConfig() { return config; }
    public void setConfig(ValidationConfig config) { this.config = config; }

    /**
     * Configuration for validation parameters.
     */
    public static class ValidationConfig {
        private Integer kBound;
        private Long maxMillis;
        private Boolean enableDeadlockCheck;
        private Boolean enableReachabilityCheck;
        private Boolean enableLivenessCheck;
        private Boolean enableBoundednessCheck;

        // Getters and setters
        public Integer getKBound() { return kBound; }
        public void setKBound(Integer kBound) { this.kBound = kBound; }

        public Long getMaxMillis() { return maxMillis; }
        public void setMaxMillis(Long maxMillis) { this.maxMillis = maxMillis; }

        public Boolean getEnableDeadlockCheck() { return enableDeadlockCheck; }
        public void setEnableDeadlockCheck(Boolean enableDeadlockCheck) { this.enableDeadlockCheck = enableDeadlockCheck; }

        public Boolean getEnableReachabilityCheck() { return enableReachabilityCheck; }
        public void setEnableReachabilityCheck(Boolean enableReachabilityCheck) { this.enableReachabilityCheck = enableReachabilityCheck; }

        public Boolean getEnableLivenessCheck() { return enableLivenessCheck; }
        public void setEnableLivenessCheck(Boolean enableLivenessCheck) { this.enableLivenessCheck = enableLivenessCheck; }

        public Boolean getEnableBoundednessCheck() { return enableBoundednessCheck; }
        public void setEnableBoundednessCheck(Boolean enableBoundednessCheck) { this.enableBoundednessCheck = enableBoundednessCheck; }
    }
}