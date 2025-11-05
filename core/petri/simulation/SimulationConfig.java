package core.petri.simulation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration settings for Petri net token simulation.
 * Defines simulation behavior, timing, and execution parameters.
 *
 * Based on Obvian execution patterns for configuration management.
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationConfig {

    /**
     * Simulation execution modes
     */
    public enum SimulationMode {
        /** Deterministic mode with seeded random selection for conflicts */
        DETERMINISTIC,
        /** Interactive mode allowing manual transition selection */
        INTERACTIVE
    }

    @JsonProperty("seed")
    private final long seed;

    @JsonProperty("mode")
    private final SimulationMode mode;

    @JsonProperty("maxSteps")
    private final int maxSteps;

    @JsonProperty("stepDelayMs")
    private final int stepDelayMs;

    @JsonProperty("enableTracing")
    private final boolean enableTracing;

    @JsonProperty("enableAnimation")
    private final boolean enableAnimation;

    @JsonProperty("pauseOnDeadlock")
    private final boolean pauseOnDeadlock;

    @JsonProperty("verbose")
    private final boolean verbose;

    @JsonCreator
    public SimulationConfig(
            @JsonProperty("seed") Long seed,
            @JsonProperty("mode") SimulationMode mode,
            @JsonProperty("maxSteps") Integer maxSteps,
            @JsonProperty("stepDelayMs") Integer stepDelayMs,
            @JsonProperty("enableTracing") Boolean enableTracing,
            @JsonProperty("enableAnimation") Boolean enableAnimation,
            @JsonProperty("pauseOnDeadlock") Boolean pauseOnDeadlock,
            @JsonProperty("verbose") Boolean verbose) {
        this.seed = seed != null ? seed : System.currentTimeMillis();
        this.mode = mode != null ? mode : SimulationMode.DETERMINISTIC;
        this.maxSteps = maxSteps != null && maxSteps > 0 ? maxSteps : 1000;
        this.stepDelayMs = stepDelayMs != null && stepDelayMs >= 0 ? stepDelayMs : 0;
        this.enableTracing = enableTracing != null ? enableTracing : true;
        this.enableAnimation = enableAnimation != null ? enableAnimation : false;
        this.pauseOnDeadlock = pauseOnDeadlock != null ? pauseOnDeadlock : true;
        this.verbose = verbose != null ? verbose : false;
    }

    // Getters
    public long getSeed() { return seed; }
    public SimulationMode getMode() { return mode; }
    public int getMaxSteps() { return maxSteps; }
    public int getStepDelayMs() { return stepDelayMs; }
    public boolean isEnableTracing() { return enableTracing; }
    public boolean isEnableAnimation() { return enableAnimation; }
    public boolean isPauseOnDeadlock() { return pauseOnDeadlock; }
    public boolean isVerbose() { return verbose; }

    /**
     * Check if simulation is in deterministic mode
     */
    public boolean isDeterministic() {
        return mode == SimulationMode.DETERMINISTIC;
    }

    /**
     * Check if simulation is in interactive mode
     */
    public boolean isInteractive() {
        return mode == SimulationMode.INTERACTIVE;
    }

    /**
     * Create default deterministic configuration
     */
    public static SimulationConfig defaultDeterministic() {
        return new SimulationConfig(42L, SimulationMode.DETERMINISTIC, 1000, 0, true, false, true, false);
    }

    /**
     * Create default interactive configuration
     */
    public static SimulationConfig defaultInteractive() {
        return new SimulationConfig(null, SimulationMode.INTERACTIVE, 1000, 100, true, true, true, true);
    }

    /**
     * Create configuration for testing with fixed seed
     */
    public static SimulationConfig forTesting(long seed) {
        return new SimulationConfig(seed, SimulationMode.DETERMINISTIC, 500, 0, true, false, false, false);
    }

    /**
     * Create configuration for fast batch processing
     */
    public static SimulationConfig fastBatch() {
        return new SimulationConfig(System.currentTimeMillis(), SimulationMode.DETERMINISTIC, 10000, 0, false, false, false, false);
    }

    /**
     * Builder pattern for configuration creation
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long seed;
        private SimulationMode mode = SimulationMode.DETERMINISTIC;
        private Integer maxSteps = 1000;
        private Integer stepDelayMs = 0;
        private Boolean enableTracing = true;
        private Boolean enableAnimation = false;
        private Boolean pauseOnDeadlock = true;
        private Boolean verbose = false;

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder mode(SimulationMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder stepDelayMs(int stepDelayMs) {
            this.stepDelayMs = stepDelayMs;
            return this;
        }

        public Builder enableTracing(boolean enableTracing) {
            this.enableTracing = enableTracing;
            return this;
        }

        public Builder enableAnimation(boolean enableAnimation) {
            this.enableAnimation = enableAnimation;
            return this;
        }

        public Builder pauseOnDeadlock(boolean pauseOnDeadlock) {
            this.pauseOnDeadlock = pauseOnDeadlock;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder deterministic() {
            this.mode = SimulationMode.DETERMINISTIC;
            return this;
        }

        public Builder interactive() {
            this.mode = SimulationMode.INTERACTIVE;
            this.enableAnimation = true;
            this.verbose = true;
            this.stepDelayMs = 100;
            return this;
        }

        public SimulationConfig build() {
            return new SimulationConfig(seed, mode, maxSteps, stepDelayMs,
                                      enableTracing, enableAnimation, pauseOnDeadlock, verbose);
        }
    }

    @Override
    public String toString() {
        return "SimulationConfig{" +
                "seed=" + seed +
                ", mode=" + mode +
                ", maxSteps=" + maxSteps +
                ", stepDelayMs=" + stepDelayMs +
                ", enableTracing=" + enableTracing +
                ", enableAnimation=" + enableAnimation +
                ", pauseOnDeadlock=" + pauseOnDeadlock +
                ", verbose=" + verbose +
                '}';
    }
}