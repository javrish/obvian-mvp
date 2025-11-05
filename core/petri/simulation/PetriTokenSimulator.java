package core.petri.simulation;

import core.petri.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Token simulator for Petri nets with deterministic and interactive execution modes.
 *
 * Implements step-by-step simulation with comprehensive trace logging, deadlock detection,
 * and real-time monitoring capabilities following Obvian execution patterns.
 *
 * Features:
 * - Deterministic simulation with seeded random selection
 * - Interactive mode with manual transition selection
 * - Comprehensive trace event generation
 * - Deadlock detection and diagnostic reporting
 * - Step-by-step execution with pause/resume
 * - Memory-efficient token tracking
 * - Production-grade error handling and logging
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@Component
public class PetriTokenSimulator {

    private static final Logger logger = LoggerFactory.getLogger(PetriTokenSimulator.class);
    private static final AtomicLong SIMULATION_ID_COUNTER = new AtomicLong(0);

    private final Clock simulationClock;
    private final Random random;

    // Simulation state
    private volatile boolean paused = false;
    private volatile boolean stopped = false;
    private String currentSimulationId;

    /**
     * Default constructor using system clock
     */
    public PetriTokenSimulator() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Constructor with custom clock for testing
     */
    public PetriTokenSimulator(Clock clock) {
        this.simulationClock = clock;
        this.random = new Random();
    }

    /**
     * Execute full simulation of Petri net
     */
    public SimulationResult simulate(PetriNet petriNet, SimulationConfig config) {
        // Generate unique simulation ID for tracing
        currentSimulationId = "sim-" + SIMULATION_ID_COUNTER.incrementAndGet();
        setupMDCContext(currentSimulationId, config);

        logger.info("Starting Petri net simulation: {}", currentSimulationId);

        Instant startTime = simulationClock.instant();
        List<TraceEvent> trace = new ArrayList<>();
        Map<String, Object> diagnostics = new HashMap<>();

        try {
            // Initialize simulation state
            SimulationState state = initializeSimulation(petriNet, config);

            // Configure random seed for deterministic mode
            if (config.isDeterministic()) {
                random.setSeed(config.getSeed());
                logger.debug("Set random seed to {} for deterministic simulation", config.getSeed());
            }

            // Main simulation loop
            while (!isTerminalState(state, config)) {
                if (stopped) {
                    logger.info("Simulation stopped by user intervention");
                    return createStoppedResult(state, trace, startTime, config);
                }

                // Handle pause in interactive mode
                if (paused && config.isInteractive()) {
                    waitForResume();
                }

                // Check for deadlock
                List<String> enabledTransitions = findEnabledTransitions(state);
                if (enabledTransitions.isEmpty()) {
                    logger.warn("Simulation deadlocked - no enabled transitions");
                    return createDeadlockedResult(state, trace, startTime, config, diagnostics, enabledTransitions);
                }

                // Select and fire transition
                String selectedTransition = selectTransition(enabledTransitions, config);
                TraceEvent event = fireTransition(state, selectedTransition, config, enabledTransitions);
                trace.add(event);

                // Apply step delay if configured
                if (config.getStepDelayMs() > 0) {
                    try {
                        Thread.sleep(config.getStepDelayMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Simulation interrupted during step delay");
                        return createStoppedResult(state, trace, startTime, config);
                    }
                }

                // Log progress
                if (config.isVerbose() && state.stepsExecuted % 10 == 0) {
                    logger.info("Simulation progress: {} steps executed, current marking: {}",
                               state.stepsExecuted, state.currentMarking);
                }
            }

            Instant endTime = simulationClock.instant();

            // Check termination reason
            if (state.stepsExecuted >= config.getMaxSteps()) {
                logger.warn("Simulation terminated due to max steps limit: {}", config.getMaxSteps());
                return createMaxStepsResult(state, trace, startTime, endTime, config, diagnostics);
            }

            logger.info("Simulation completed successfully in {} steps", state.stepsExecuted);
            return createCompletedResult(state, trace, startTime, endTime, config);

        } catch (Exception e) {
            logger.error("Simulation failed with error: {}", e.getMessage(), e);
            return createFailedResult(e, trace, petriNet.getInitialMarking(),
                                    startTime, simulationClock.instant(), config);
        } finally {
            clearMDCContext();
        }
    }

    /**
     * Execute single simulation step
     */
    public SimulationStepResult step(SimulationState state, SimulationConfig config) {
        setupMDCContext(state.simulationId, config);

        try {
            List<String> enabledTransitions = findEnabledTransitions(state);

            if (enabledTransitions.isEmpty()) {
                return SimulationStepResult.deadlock(state, enabledTransitions);
            }

            if (state.stepsExecuted >= config.getMaxSteps()) {
                return SimulationStepResult.maxStepsReached(state);
            }

            String selectedTransition = selectTransition(enabledTransitions, config);
            TraceEvent event = fireTransition(state, selectedTransition, config, enabledTransitions);

            return SimulationStepResult.success(state, event, enabledTransitions);

        } finally {
            clearMDCContext();
        }
    }

    /**
     * Initialize simulation state from Petri net
     */
    private SimulationState initializeSimulation(PetriNet petriNet, SimulationConfig config) {
        return SimulationState.builder()
                .simulationId(currentSimulationId)
                .petriNet(petriNet)
                .currentMarking(petriNet.getInitialMarking())
                .initialMarking(petriNet.getInitialMarking())
                .config(config)
                .stepsExecuted(0)
                .startTime(simulationClock.instant())
                .build();
    }

    /**
     * Find all enabled transitions at current marking
     */
    private List<String> findEnabledTransitions(SimulationState state) {
        List<String> enabled = new ArrayList<>();

        for (Transition transition : state.petriNet.getTransitions()) {
            if (isTransitionEnabled(transition, state.currentMarking, state.petriNet)) {
                enabled.add(transition.getId());
            }
        }

        // Sort for deterministic ordering
        Collections.sort(enabled);

        if (logger.isDebugEnabled()) {
            logger.debug("Found {} enabled transitions: {}", enabled.size(), enabled);
        }

        return enabled;
    }

    /**
     * Check if a transition is enabled at current marking
     */
    private boolean isTransitionEnabled(Transition transition, Marking marking, PetriNet petriNet) {
        // Get input arcs for this transition (arcs from places to this transition)
        List<Arc> inputArcs = petriNet.getArcs().stream()
                .filter(arc -> arc.getTo().equals(transition.getId()))
                .collect(Collectors.toList());

        // Check if all input places have sufficient tokens
        for (Arc arc : inputArcs) {
            int requiredTokens = arc.getWeight();
            int availableTokens = marking.getTokens(arc.getFrom());

            if (availableTokens < requiredTokens) {
                return false;
            }
        }

        // Check capacity constraints for output places (arcs from this transition to places)
        List<Arc> outputArcs = petriNet.getArcs().stream()
                .filter(arc -> arc.getFrom().equals(transition.getId()))
                .collect(Collectors.toList());

        for (Arc arc : outputArcs) {
            Optional<Place> place = petriNet.getPlace(arc.getTo());
            if (place.isPresent() && place.get().getCapacity() != null && place.get().getCapacity() > 0) {
                int currentTokens = marking.getTokens(arc.getTo());
                int addedTokens = arc.getWeight();

                if (currentTokens + addedTokens > place.get().getCapacity()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Select transition to fire based on configuration
     */
    private String selectTransition(List<String> enabledTransitions, SimulationConfig config) {
        if (enabledTransitions.isEmpty()) {
            throw new IllegalStateException("Cannot select from empty transition list");
        }

        if (enabledTransitions.size() == 1) {
            return enabledTransitions.get(0);
        }

        switch (config.getMode()) {
            case DETERMINISTIC:
                // Use seeded random selection for conflict resolution
                return enabledTransitions.get(random.nextInt(enabledTransitions.size()));

            case INTERACTIVE:
                // In real implementation, this would prompt user or wait for UI input
                // For now, return first transition (sorted alphabetically)
                logger.info("Interactive mode: selecting first enabled transition: {}", enabledTransitions.get(0));
                return enabledTransitions.get(0);

            default:
                throw new IllegalArgumentException("Unsupported simulation mode: " + config.getMode());
        }
    }

    /**
     * Fire selected transition and update marking
     */
    private TraceEvent fireTransition(SimulationState state, String transitionId,
                                    SimulationConfig config, List<String> enabledTransitions) {

        logger.debug("Firing transition: {}", transitionId);

        Transition transition = state.petriNet.getTransition(transitionId)
                .orElseThrow(() -> new IllegalArgumentException("Transition not found: " + transitionId));

        // Capture state before firing
        Marking markingBefore = state.currentMarking;

        // Calculate new marking after firing
        Marking markingAfter = calculateMarkingAfterFiring(transition, markingBefore, state.petriNet);

        // Update simulation state
        state.currentMarking = markingAfter;
        state.stepsExecuted++;

        // Generate trace event
        TraceEvent event = TraceEvent.builder()
                .timestamp(simulationClock.instant())
                .sequenceNumber(state.stepsExecuted)
                .transition(transitionId)
                .fromPlaces(getInputPlaces(transition, state.petriNet))
                .toPlaces(getOutputPlaces(transition, state.petriNet))
                .tokenId("token-" + state.stepsExecuted) // Simple token ID for POC
                .simulationSeed(config.getSeed())
                .enabled(enabledTransitions)
                .markingBefore(markingBefore)
                .markingAfter(markingAfter)
                .simulationMode(config.getMode().name().toLowerCase())
                .reason("Transition " + transitionId + " fired in " + config.getMode().name().toLowerCase() + " mode")
                .build();

        if (config.isVerbose()) {
            logger.info("Fired transition {}: {} -> {}", transitionId,
                       markingBefore.getTokens(), markingAfter.getTokens());
        }

        return event;
    }

    /**
     * Calculate marking after firing transition
     */
    private Marking calculateMarkingAfterFiring(Transition transition, Marking currentMarking, PetriNet petriNet) {
        Map<String, Integer> newTokens = new HashMap<>(currentMarking.getTokens());

        // Remove tokens from input places
        List<Arc> inputArcs = petriNet.getArcs().stream()
                .filter(arc -> arc.getTo().equals(transition.getId()))
                .collect(Collectors.toList());

        for (Arc arc : inputArcs) {
            String placeId = arc.getFrom();
            int currentCount = newTokens.getOrDefault(placeId, 0);
            int newCount = currentCount - arc.getWeight();

            if (newCount <= 0) {
                newTokens.remove(placeId);
            } else {
                newTokens.put(placeId, newCount);
            }
        }

        // Add tokens to output places
        List<Arc> outputArcs = petriNet.getArcs().stream()
                .filter(arc -> arc.getFrom().equals(transition.getId()))
                .collect(Collectors.toList());

        for (Arc arc : outputArcs) {
            String placeId = arc.getTo();
            int currentCount = newTokens.getOrDefault(placeId, 0);
            newTokens.put(placeId, currentCount + arc.getWeight());
        }

        return new Marking(newTokens);
    }

    /**
     * Get input place IDs for transition
     */
    private List<String> getInputPlaces(Transition transition, PetriNet petriNet) {
        return petriNet.getArcs().stream()
                .filter(arc -> arc.getTo().equals(transition.getId()))
                .map(Arc::getFrom)
                .collect(Collectors.toList());
    }

    /**
     * Get output place IDs for transition
     */
    private List<String> getOutputPlaces(Transition transition, PetriNet petriNet) {
        return petriNet.getArcs().stream()
                .filter(arc -> arc.getFrom().equals(transition.getId()))
                .map(Arc::getTo)
                .collect(Collectors.toList());
    }

    /**
     * Check if simulation has reached terminal state
     */
    private boolean isTerminalState(SimulationState state, SimulationConfig config) {
        // Check step limit
        if (state.stepsExecuted >= config.getMaxSteps()) {
            return true;
        }

        // Check for deadlock (will be handled separately)
        List<String> enabled = findEnabledTransitions(state);
        return enabled.isEmpty();
    }

    // Result creation methods

    private SimulationResult createCompletedResult(SimulationState state, List<TraceEvent> trace,
                                                 Instant startTime, Instant endTime, SimulationConfig config) {
        return SimulationResult.completed(
                "Simulation completed successfully after " + state.stepsExecuted + " steps",
                trace, state.initialMarking, state.currentMarking, state.stepsExecuted,
                startTime, endTime, config);
    }

    private SimulationResult createDeadlockedResult(SimulationState state, List<TraceEvent> trace,
                                                  Instant startTime, SimulationConfig config,
                                                  Map<String, Object> diagnostics, List<String> enabledTransitions) {
        diagnostics.put("finalEnabledTransitions", enabledTransitions);
        diagnostics.put("deadlockDetected", true);
        diagnostics.put("deadlockStep", state.stepsExecuted);

        return SimulationResult.deadlocked(
                "Simulation deadlocked - no enabled transitions at step " + state.stepsExecuted,
                trace, state.initialMarking, state.currentMarking, state.stepsExecuted,
                startTime, simulationClock.instant(), config, diagnostics);
    }

    private SimulationResult createMaxStepsResult(SimulationState state, List<TraceEvent> trace,
                                                Instant startTime, Instant endTime, SimulationConfig config,
                                                Map<String, Object> diagnostics) {
        diagnostics.put("maxStepsReached", true);
        diagnostics.put("finalEnabledTransitions", findEnabledTransitions(state));

        return SimulationResult.maxStepsReached(
                "Simulation terminated due to maximum steps limit: " + config.getMaxSteps(),
                trace, state.initialMarking, state.currentMarking, state.stepsExecuted,
                startTime, endTime, config, diagnostics);
    }

    private SimulationResult createStoppedResult(SimulationState state, List<TraceEvent> trace,
                                               Instant startTime, SimulationConfig config) {
        return SimulationResult.stopped(
                "Simulation stopped by user intervention at step " + state.stepsExecuted,
                trace, state.initialMarking, state.currentMarking, state.stepsExecuted,
                startTime, simulationClock.instant(), config);
    }

    private SimulationResult createFailedResult(Exception error, List<TraceEvent> trace,
                                              Marking initialMarking, Instant startTime, Instant endTime,
                                              SimulationConfig config) {
        return SimulationResult.failed(
                "Simulation failed: " + error.getMessage(), error,
                trace, initialMarking, null, 0, startTime, endTime, config);
    }

    // Control methods for interactive simulation

    /**
     * Pause simulation (interactive mode)
     */
    public void pause() {
        this.paused = true;
        logger.info("Simulation paused");
    }

    /**
     * Resume simulation (interactive mode)
     */
    public void resume() {
        this.paused = false;
        synchronized (this) {
            this.notifyAll();
        }
        logger.info("Simulation resumed");
    }

    /**
     * Stop simulation
     */
    public void stop() {
        this.stopped = true;
        resume(); // Wake up if paused
        logger.info("Simulation stop requested");
    }

    /**
     * Reset simulator state
     */
    public void reset() {
        this.paused = false;
        this.stopped = false;
        logger.info("Simulator reset");
    }

    /**
     * Wait for resume signal
     */
    private void waitForResume() {
        synchronized (this) {
            while (paused && !stopped) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Wait for resume interrupted");
                    break;
                }
            }
        }
    }

    // MDC context management

    private void setupMDCContext(String simulationId, SimulationConfig config) {
        MDC.put("simulationId", simulationId);
        MDC.put("simulationMode", config.getMode().toString());
        MDC.put("simulationSeed", String.valueOf(config.getSeed()));
    }

    private void clearMDCContext() {
        MDC.remove("simulationId");
        MDC.remove("simulationMode");
        MDC.remove("simulationSeed");
    }

    // Getters for state inspection

    public boolean isPaused() { return paused; }
    public boolean isStopped() { return stopped; }
    public String getCurrentSimulationId() { return currentSimulationId; }
}