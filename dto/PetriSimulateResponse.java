package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import core.petri.Marking;
import core.petri.simulation.TraceEvent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for Petri net simulation results.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing Petri net simulation results")
public class PetriSimulateResponse {

    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0")
    private String schemaVersion;

    @JsonProperty("trace")
    @Schema(description = "Execution trace events")
    private List<TraceEvent> trace;

    @JsonProperty("finalMarking")
    @Schema(description = "Final token marking after simulation")
    private Marking finalMarking;

    @JsonProperty("status")
    @Schema(description = "Simulation status", example = "COMPLETED", allowableValues = {"COMPLETED", "DEADLOCK", "MAX_STEPS_REACHED", "TIMEOUT", "ERROR"})
    private String status;

    @JsonProperty("stepsExecuted")
    @Schema(description = "Number of simulation steps executed", example = "5")
    private Integer stepsExecuted;

    @JsonProperty("executionTimeMs")
    @Schema(description = "Total execution time in milliseconds", example = "250")
    private Long executionTimeMs;

    // Constructors
    public PetriSimulateResponse() {}

    public PetriSimulateResponse(String schemaVersion, List<TraceEvent> trace, Marking finalMarking, String status) {
        this.schemaVersion = schemaVersion;
        this.trace = trace;
        this.finalMarking = finalMarking;
        this.status = status;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public List<TraceEvent> getTrace() { return trace; }
    public void setTrace(List<TraceEvent> trace) { this.trace = trace; }

    public Marking getFinalMarking() { return finalMarking; }
    public void setFinalMarking(Marking finalMarking) { this.finalMarking = finalMarking; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getStepsExecuted() { return stepsExecuted; }
    public void setStepsExecuted(Integer stepsExecuted) { this.stepsExecuted = stepsExecuted; }

    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    // Factory methods
    public static PetriSimulateResponse success(String schemaVersion, List<TraceEvent> trace, Marking finalMarking, String status) {
        return new PetriSimulateResponse(schemaVersion, trace, finalMarking, status);
    }

    @Override
    public String toString() {
        return "PetriSimulateResponse{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", trace=" + (trace != null ? trace.size() + " events" : "no trace") +
                ", finalMarking=" + finalMarking +
                ", status='" + status + '\'' +
                ", stepsExecuted=" + stepsExecuted +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}