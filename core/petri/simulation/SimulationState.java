package core.petri.simulation;

import core.petri.Marking;
import core.petri.PetriNet;

import java.time.Instant;

/**
 * Represents the current state of a Petri net simulation.
 * Used for step-by-step simulation and state tracking.
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
public class SimulationState {

    public final String simulationId;
    public final PetriNet petriNet;
    public final Marking initialMarking;
    public final SimulationConfig config;
    public final Instant startTime;

    // Mutable state
    public Marking currentMarking;
    public int stepsExecuted;

    private SimulationState(String simulationId, PetriNet petriNet, Marking initialMarking,
                           Marking currentMarking, SimulationConfig config, int stepsExecuted,
                           Instant startTime) {
        this.simulationId = simulationId;
        this.petriNet = petriNet;
        this.initialMarking = initialMarking;
        this.currentMarking = currentMarking;
        this.config = config;
        this.stepsExecuted = stepsExecuted;
        this.startTime = startTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String simulationId;
        private PetriNet petriNet;
        private Marking initialMarking;
        private Marking currentMarking;
        private SimulationConfig config;
        private int stepsExecuted = 0;
        private Instant startTime;

        public Builder simulationId(String simulationId) {
            this.simulationId = simulationId;
            return this;
        }

        public Builder petriNet(PetriNet petriNet) {
            this.petriNet = petriNet;
            return this;
        }

        public Builder initialMarking(Marking initialMarking) {
            this.initialMarking = initialMarking;
            return this;
        }

        public Builder currentMarking(Marking currentMarking) {
            this.currentMarking = currentMarking;
            return this;
        }

        public Builder config(SimulationConfig config) {
            this.config = config;
            return this;
        }

        public Builder stepsExecuted(int stepsExecuted) {
            this.stepsExecuted = stepsExecuted;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public SimulationState build() {
            return new SimulationState(simulationId, petriNet, initialMarking,
                                     currentMarking, config, stepsExecuted, startTime);
        }
    }

    @Override
    public String toString() {
        return "SimulationState{" +
                "simulationId='" + simulationId + '\'' +
                ", stepsExecuted=" + stepsExecuted +
                ", currentMarking=" + currentMarking +
                '}';
    }
}