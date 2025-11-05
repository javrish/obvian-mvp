package api.dto;

import core.petri.PetriIntentSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for P3Net end-to-end execution endpoint.
 * Includes intent specification and execution configuration.
 */
public class PetriExecuteRequest {

    @NotNull(message = "Intent specification cannot be null")
    @Valid
    private PetriIntentSpec intentSpec;

    // Validation configuration
    private Boolean validationEnabled;
    private Boolean strictValidation;
    private Integer kBound;
    private Long validationTimeoutMs;

    // Simulation configuration
    private Boolean simulationEnabled;
    private Integer maxSimulationSteps;
    private Long simulationSeed;

    // Execution configuration
    private Boolean executionEnabled;

    public PetriExecuteRequest() {}

    public PetriExecuteRequest(PetriIntentSpec intentSpec) {
        this.intentSpec = intentSpec;
    }

    // Getters and setters
    public PetriIntentSpec getIntentSpec() { return intentSpec; }
    public void setIntentSpec(PetriIntentSpec intentSpec) { this.intentSpec = intentSpec; }

    public Boolean isValidationEnabled() { return validationEnabled; }
    public void setValidationEnabled(Boolean validationEnabled) { this.validationEnabled = validationEnabled; }

    public Boolean isStrictValidation() { return strictValidation; }
    public void setStrictValidation(Boolean strictValidation) { this.strictValidation = strictValidation; }

    public Integer getKBound() { return kBound; }
    public void setKBound(Integer kBound) { this.kBound = kBound; }

    public Long getValidationTimeoutMs() { return validationTimeoutMs; }
    public void setValidationTimeoutMs(Long validationTimeoutMs) { this.validationTimeoutMs = validationTimeoutMs; }

    public Boolean isSimulationEnabled() { return simulationEnabled; }
    public void setSimulationEnabled(Boolean simulationEnabled) { this.simulationEnabled = simulationEnabled; }

    public Integer getMaxSimulationSteps() { return maxSimulationSteps; }
    public void setMaxSimulationSteps(Integer maxSimulationSteps) { this.maxSimulationSteps = maxSimulationSteps; }

    public Long getSimulationSeed() { return simulationSeed; }
    public void setSimulationSeed(Long simulationSeed) { this.simulationSeed = simulationSeed; }

    public Boolean isExecutionEnabled() { return executionEnabled; }
    public void setExecutionEnabled(Boolean executionEnabled) { this.executionEnabled = executionEnabled; }
}
