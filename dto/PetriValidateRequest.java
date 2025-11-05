package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.PetriNet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for Petri net validation.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request for validating Petri net")
public class PetriValidateRequest {

    @NotNull
    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0", required = true)
    private String schemaVersion;

    @Valid
    @NotNull
    @JsonProperty("petriNet")
    @Schema(description = "Petri net to validate", required = true)
    private PetriNet petriNet;

    @JsonProperty("config")
    @Schema(description = "Validation configuration")
    private ValidationConfig config;

    // Nested class for validation configuration
    @Schema(description = "Validation configuration parameters")
    public static class ValidationConfig {
        @JsonProperty("kBound")
        @Schema(description = "State space exploration bound", example = "200")
        private Integer kBound;

        @JsonProperty("maxMillis")
        @Schema(description = "Maximum validation time in milliseconds", example = "30000")
        private Long maxMillis;

        @JsonProperty("enableDeadlockCheck")
        @Schema(description = "Enable deadlock detection", example = "true")
        private Boolean enableDeadlockCheck;

        @JsonProperty("enableReachabilityCheck")
        @Schema(description = "Enable reachability analysis", example = "true")
        private Boolean enableReachabilityCheck;

        @JsonProperty("enableLivenessCheck")
        @Schema(description = "Enable liveness checking", example = "true")
        private Boolean enableLivenessCheck;

        @JsonProperty("enableBoundednessCheck")
        @Schema(description = "Enable boundedness verification", example = "true")
        private Boolean enableBoundednessCheck;

        // Constructors
        public ValidationConfig() {}

        // Getters and Setters
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

    // Constructors
    public PetriValidateRequest() {}

    public PetriValidateRequest(String schemaVersion, PetriNet petriNet) {
        this.schemaVersion = schemaVersion;
        this.petriNet = petriNet;
    }

    public PetriValidateRequest(String schemaVersion, PetriNet petriNet, ValidationConfig config) {
        this.schemaVersion = schemaVersion;
        this.petriNet = petriNet;
        this.config = config;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public PetriNet getPetriNet() { return petriNet; }
    public void setPetriNet(PetriNet petriNet) { this.petriNet = petriNet; }

    public ValidationConfig getConfig() { return config; }
    public void setConfig(ValidationConfig config) { this.config = config; }

    @Override
    public String toString() {
        return "PetriValidateRequest{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", petriNet=" + petriNet +
                ", config=" + config +
                '}';
    }
}