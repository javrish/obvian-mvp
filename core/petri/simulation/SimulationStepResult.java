package core.petri.simulation;

import java.util.List;

/**
 * Result of a single simulation step.
 * Used for step-by-step simulation control and monitoring.
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
public class SimulationStepResult {

    public enum StepStatus {
        SUCCESS,
        DEADLOCK,
        MAX_STEPS_REACHED,
        ERROR
    }

    private final StepStatus status;
    private final SimulationState state;
    private final TraceEvent event;
    private final List<String> enabledTransitions;
    private final String message;
    private final Exception error;

    private SimulationStepResult(StepStatus status, SimulationState state, TraceEvent event,
                               List<String> enabledTransitions, String message, Exception error) {
        this.status = status;
        this.state = state;
        this.event = event;
        this.enabledTransitions = enabledTransitions;
        this.message = message;
        this.error = error;
    }

    // Getters
    public StepStatus getStatus() { return status; }
    public SimulationState getState() { return state; }
    public TraceEvent getEvent() { return event; }
    public List<String> getEnabledTransitions() { return enabledTransitions; }
    public String getMessage() { return message; }
    public Exception getError() { return error; }

    // Status checks
    public boolean isSuccess() { return status == StepStatus.SUCCESS; }
    public boolean isDeadlock() { return status == StepStatus.DEADLOCK; }
    public boolean isMaxStepsReached() { return status == StepStatus.MAX_STEPS_REACHED; }
    public boolean isError() { return status == StepStatus.ERROR; }

    // Factory methods
    public static SimulationStepResult success(SimulationState state, TraceEvent event, List<String> enabledTransitions) {
        return new SimulationStepResult(StepStatus.SUCCESS, state, event, enabledTransitions, null, null);
    }

    public static SimulationStepResult deadlock(SimulationState state, List<String> enabledTransitions) {
        return new SimulationStepResult(StepStatus.DEADLOCK, state, null, enabledTransitions,
                                      "No enabled transitions - deadlock detected", null);
    }

    public static SimulationStepResult maxStepsReached(SimulationState state) {
        return new SimulationStepResult(StepStatus.MAX_STEPS_REACHED, state, null, null,
                                      "Maximum steps limit reached", null);
    }

    public static SimulationStepResult error(SimulationState state, String message, Exception error) {
        return new SimulationStepResult(StepStatus.ERROR, state, null, null, message, error);
    }

    @Override
    public String toString() {
        return "SimulationStepResult{" +
                "status=" + status +
                ", message='" + message + '\'' +
                (event != null ? ", transition=" + event.getTransition() : "") +
                '}';
    }
}