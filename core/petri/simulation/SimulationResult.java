package core.petri.simulation;

import core.petri.Marking;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of Petri net token simulation execution.
 *
 * Follows ExecutionResult patterns from DagExecutor for consistent error handling
 * and result management across the Obvian system.
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationResult {

    /**
     * Simulation status enumeration
     */
    public enum SimulationStatus {
        /** Simulation completed successfully reaching terminal marking */
        COMPLETED,
        /** Simulation stopped due to deadlock (no enabled transitions) */
        DEADLOCKED,
        /** Simulation stopped due to reaching maximum step limit */
        MAX_STEPS_REACHED,
        /** Simulation stopped due to user intervention */
        STOPPED,
        /** Simulation failed due to error */
        FAILED,
        /** Simulation is currently running (for async scenarios) */
        RUNNING
    }

    @JsonProperty("success")
    private final boolean success;

    @JsonProperty("status")
    private final SimulationStatus status;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("trace")
    private final List<TraceEvent> trace;

    @JsonProperty("finalMarking")
    private final Marking finalMarking;

    @JsonProperty("initialMarking")
    private final Marking initialMarking;

    @JsonProperty("stepsExecuted")
    private final int stepsExecuted;

    @JsonProperty("simulationStartTime")
    private final Instant simulationStartTime;

    @JsonProperty("simulationEndTime")
    private final Instant simulationEndTime;

    @JsonProperty("config")
    private final SimulationConfig config;

    @JsonProperty("diagnostics")
    private final Map<String, Object> diagnostics;

    @JsonProperty("error")
    private final Exception error;

    @JsonCreator
    public SimulationResult(
            @JsonProperty("success") boolean success,
            @JsonProperty("status") SimulationStatus status,
            @JsonProperty("message") String message,
            @JsonProperty("trace") List<TraceEvent> trace,
            @JsonProperty("finalMarking") Marking finalMarking,
            @JsonProperty("initialMarking") Marking initialMarking,
            @JsonProperty("stepsExecuted") Integer stepsExecuted,
            @JsonProperty("simulationStartTime") Instant simulationStartTime,
            @JsonProperty("simulationEndTime") Instant simulationEndTime,
            @JsonProperty("config") SimulationConfig config,
            @JsonProperty("diagnostics") Map<String, Object> diagnostics,
            @JsonProperty("error") Exception error) {
        this.success = success;
        this.status = status != null ? status : SimulationStatus.FAILED;
        this.message = message;
        this.trace = trace != null ? new ArrayList<>(trace) : new ArrayList<>();
        this.finalMarking = finalMarking;
        this.initialMarking = initialMarking;
        this.stepsExecuted = stepsExecuted != null ? stepsExecuted : 0;
        this.simulationStartTime = simulationStartTime;
        this.simulationEndTime = simulationEndTime;
        this.config = config;
        this.diagnostics = diagnostics != null ? new HashMap<>(diagnostics) : new HashMap<>();
        this.error = error;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public SimulationStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public List<TraceEvent> getTrace() { return new ArrayList<>(trace); }
    public Marking getFinalMarking() { return finalMarking; }
    public Marking getInitialMarking() { return initialMarking; }
    public int getStepsExecuted() { return stepsExecuted; }
    public Instant getSimulationStartTime() { return simulationStartTime; }
    public Instant getSimulationEndTime() { return simulationEndTime; }
    public SimulationConfig getConfig() { return config; }
    public Map<String, Object> getDiagnostics() { return new HashMap<>(diagnostics); }
    public Exception getError() { return error; }

    /**
     * Get simulation duration
     */
    public Duration getDuration() {
        if (simulationStartTime == null || simulationEndTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(simulationStartTime, simulationEndTime);
    }

    /**
     * Check if simulation ended in deadlock
     */
    public boolean isDeadlocked() {
        return status == SimulationStatus.DEADLOCKED;
    }

    /**
     * Check if simulation completed successfully
     */
    public boolean isCompleted() {
        return success && status == SimulationStatus.COMPLETED;
    }

    /**
     * Check if simulation reached maximum steps
     */
    public boolean reachedMaxSteps() {
        return status == SimulationStatus.MAX_STEPS_REACHED;
    }

    /**
     * Get diagnostic information
     */
    public Object getDiagnostic(String key) {
        return diagnostics.get(key);
    }

    /**
     * Get enabled transitions at final state
     */
    public List<String> getFinalEnabledTransitions() {
        Object enabled = diagnostics.get("finalEnabledTransitions");
        if (enabled instanceof List) {
            return new ArrayList<>((List<String>) enabled);
        }
        return new ArrayList<>();
    }

    // Static factory methods following ExecutionResult patterns

    /**
     * Create successful completed simulation result
     */
    public static SimulationResult completed(String message, List<TraceEvent> trace,
                                           Marking initialMarking, Marking finalMarking,
                                           int stepsExecuted, Instant startTime, Instant endTime,
                                           SimulationConfig config) {
        return new SimulationResult(true, SimulationStatus.COMPLETED, message, trace,
                                  finalMarking, initialMarking, stepsExecuted, startTime, endTime,
                                  config, null, null);
    }

    /**
     * Create deadlocked simulation result
     */
    public static SimulationResult deadlocked(String message, List<TraceEvent> trace,
                                            Marking initialMarking, Marking finalMarking,
                                            int stepsExecuted, Instant startTime, Instant endTime,
                                            SimulationConfig config, Map<String, Object> diagnostics) {
        return new SimulationResult(false, SimulationStatus.DEADLOCKED, message, trace,
                                  finalMarking, initialMarking, stepsExecuted, startTime, endTime,
                                  config, diagnostics, null);
    }

    /**
     * Create max steps reached result
     */
    public static SimulationResult maxStepsReached(String message, List<TraceEvent> trace,
                                                 Marking initialMarking, Marking finalMarking,
                                                 int stepsExecuted, Instant startTime, Instant endTime,
                                                 SimulationConfig config, Map<String, Object> diagnostics) {
        return new SimulationResult(false, SimulationStatus.MAX_STEPS_REACHED, message, trace,
                                  finalMarking, initialMarking, stepsExecuted, startTime, endTime,
                                  config, diagnostics, null);
    }

    /**
     * Create stopped simulation result
     */
    public static SimulationResult stopped(String message, List<TraceEvent> trace,
                                         Marking initialMarking, Marking finalMarking,
                                         int stepsExecuted, Instant startTime, Instant endTime,
                                         SimulationConfig config) {
        return new SimulationResult(false, SimulationStatus.STOPPED, message, trace,
                                  finalMarking, initialMarking, stepsExecuted, startTime, endTime,
                                  config, null, null);
    }

    /**
     * Create failed simulation result
     */
    public static SimulationResult failed(String message, Exception error,
                                        Marking initialMarking, int stepsExecuted,
                                        Instant startTime, Instant endTime,
                                        SimulationConfig config) {
        return new SimulationResult(false, SimulationStatus.FAILED, message, null,
                                  null, initialMarking, stepsExecuted, startTime, endTime,
                                  config, null, error);
    }

    /**
     * Create failed simulation result with trace
     */
    public static SimulationResult failed(String message, Exception error,
                                        List<TraceEvent> trace, Marking initialMarking,
                                        Marking finalMarking, int stepsExecuted,
                                        Instant startTime, Instant endTime,
                                        SimulationConfig config) {
        return new SimulationResult(false, SimulationStatus.FAILED, message, trace,
                                  finalMarking, initialMarking, stepsExecuted, startTime, endTime,
                                  config, null, error);
    }

    /**
     * Create running simulation result (for async tracking)
     */
    public static SimulationResult running(String message, List<TraceEvent> trace,
                                         Marking initialMarking, Marking currentMarking,
                                         int stepsExecuted, Instant startTime,
                                         SimulationConfig config) {
        return new SimulationResult(true, SimulationStatus.RUNNING, message, trace,
                                  currentMarking, initialMarking, stepsExecuted, startTime, null,
                                  config, null, null);
    }

    /**
     * Builder for complex simulation results
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success = true;
        private SimulationStatus status = SimulationStatus.COMPLETED;
        private String message;
        private List<TraceEvent> trace = new ArrayList<>();
        private Marking finalMarking;
        private Marking initialMarking;
        private int stepsExecuted = 0;
        private Instant simulationStartTime;
        private Instant simulationEndTime;
        private SimulationConfig config;
        private Map<String, Object> diagnostics = new HashMap<>();
        private Exception error;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder status(SimulationStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder trace(List<TraceEvent> trace) {
            this.trace = new ArrayList<>(trace);
            return this;
        }

        public Builder finalMarking(Marking finalMarking) {
            this.finalMarking = finalMarking;
            return this;
        }

        public Builder initialMarking(Marking initialMarking) {
            this.initialMarking = initialMarking;
            return this;
        }

        public Builder stepsExecuted(int stepsExecuted) {
            this.stepsExecuted = stepsExecuted;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.simulationStartTime = startTime;
            return this;
        }

        public Builder endTime(Instant endTime) {
            this.simulationEndTime = endTime;
            return this;
        }

        public Builder config(SimulationConfig config) {
            this.config = config;
            return this;
        }

        public Builder addDiagnostic(String key, Object value) {
            this.diagnostics.put(key, value);
            return this;
        }

        public Builder diagnostics(Map<String, Object> diagnostics) {
            this.diagnostics = new HashMap<>(diagnostics);
            return this;
        }

        public Builder error(Exception error) {
            this.error = error;
            this.success = false;
            if (this.status == SimulationStatus.COMPLETED) {
                this.status = SimulationStatus.FAILED;
            }
            return this;
        }

        public SimulationResult build() {
            return new SimulationResult(success, status, message, trace, finalMarking,
                                      initialMarking, stepsExecuted, simulationStartTime,
                                      simulationEndTime, config, diagnostics, error);
        }
    }

    @Override
    public String toString() {
        return "SimulationResult{" +
                "success=" + success +
                ", status=" + status +
                ", stepsExecuted=" + stepsExecuted +
                ", message='" + message + '\'' +
                ", duration=" + getDuration() +
                '}';
    }
}