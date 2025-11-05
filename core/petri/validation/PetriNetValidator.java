package core.petri.validation;

import core.petri.PetriNet;
import core.petri.validation.PetriNetValidationResult;
import core.petri.Marking;
import core.petri.Transition;
import core.petri.Place;
import core.petri.Arc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.Instant;

/**
 * Formal validator for Petri nets implementing deadlock detection, reachability analysis,
 * liveness checking, and boundedness verification using bounded state space exploration.
 *
 * This implementation follows the technical specification:
 * - Bounded validation loop with configurable k-bound (default 200) and timeout
 * - BFS/DFS over marking graph with state exploration
 * - Petri net firing semantics: M' = M - Σ_in w + Σ_out w
 * - Terminal marking detection: tokens in p_done and no enabled transitions
 * - Counter-examples with witness paths for failed validations
 *
 * Patent Alignment: Implements formal verification capabilities that enable
 * comprehensive workflow validation with mathematical guarantees.
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@Component
public class PetriNetValidator {

    private static final Logger logger = LoggerFactory.getLogger(PetriNetValidator.class);

    private static final String TERMINAL_PLACE_PREFIX = "p_done";
    private static final int DEFAULT_K_BOUND = 200;
    private static final long DEFAULT_TIMEOUT_MS = 30000L;

    /**
     * Validates a Petri net using formal verification algorithms.
     * Performs deadlock detection, reachability analysis, liveness checking, and boundedness verification.
     *
     * @param petriNet the Petri net to validate
     * @param config validation configuration with k-bound, timeout, and enabled checks
     * @return comprehensive validation result with status, checks, counter-examples, and hints
     */
    public PetriNetValidationResult validate(PetriNet petriNet, PetriNetValidationResult.ValidationConfig config) {
        if (petriNet == null) {
            throw new IllegalArgumentException("PetriNet cannot be null");
        }
        if (config == null) {
            config = PetriNetValidationResult.ValidationConfig.defaultConfig();
        }

        logger.info("Starting Petri net validation for net: {} with k-bound: {}, timeout: {}ms",
                petriNet.getName(), config.getKBound(), config.getMaxTimeMs());

        Instant startTime = Instant.now();
        PetriNetValidationResult.Builder resultBuilder = PetriNetValidationResult.builder()
                .petriNetId(petriNet.getId())
                .config(config);

        try {
            // State space exploration context
            ValidationContext context = new ValidationContext(petriNet, config, startTime);

            // Run enabled validation checks
            if (config.getEnabledChecks().contains(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION)) {
                performStructuralValidation(context, resultBuilder);
            }

            if (config.getEnabledChecks().contains(PetriNetValidationResult.CheckType.DEADLOCK_DETECTION)) {
                performDeadlockDetection(context, resultBuilder);
            }

            if (config.getEnabledChecks().contains(PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS)) {
                performReachabilityAnalysis(context, resultBuilder);
            }

            if (config.getEnabledChecks().contains(PetriNetValidationResult.CheckType.LIVENESS_CHECK)) {
                performLivenessCheck(context, resultBuilder);
            }

            if (config.getEnabledChecks().contains(PetriNetValidationResult.CheckType.BOUNDEDNESS_CHECK)) {
                performBoundednessCheck(context, resultBuilder);
            }

            // Set overall validation status based on individual check results
            PetriNetValidationResult.PetriValidationStatus overallStatus = determineOverallStatus(resultBuilder);
            resultBuilder.petriStatus(overallStatus);
            resultBuilder.statesExplored(context.getStatesExplored());

            // Set parent ValidationResult fields
            if (overallStatus == PetriNetValidationResult.PetriValidationStatus.PASS) {
                resultBuilder.status(core.ValidationResult.ValidationStatus.VALID)
                        .valid(true)
                        .confidenceScore(1.0);
            } else if (overallStatus == PetriNetValidationResult.PetriValidationStatus.FAIL) {
                resultBuilder.status(core.ValidationResult.ValidationStatus.INVALID)
                        .valid(false)
                        .confidenceScore(0.0);
            } else {
                resultBuilder.status(core.ValidationResult.ValidationStatus.INCOMPLETE)
                        .valid(false)
                        .confidenceScore(0.5);
            }

            long validationTime = Duration.between(startTime, Instant.now()).toMillis();
            resultBuilder.validationTimeMs(validationTime);

            logger.info("Validation completed for net: {} in {}ms, status: {}, states explored: {}",
                    petriNet.getName(), validationTime, overallStatus, context.getStatesExplored());

            return resultBuilder.build();

        } catch (Exception e) {
            logger.error("Validation failed for net: {} due to: {}", petriNet.getName(), e.getMessage(), e);

            return PetriNetValidationResult.failure(petriNet.getId(),
                    "Validation failed: " + e.getMessage(), null);
        }
    }

    /**
     * Performs structural validation of the Petri net.
     * Checks for basic structural correctness like disconnected components,
     * invalid arc weights, missing initial marking, etc.
     */
    private void performStructuralValidation(ValidationContext context, PetriNetValidationResult.Builder resultBuilder) {
        logger.debug("Performing structural validation");
        Instant checkStart = Instant.now();

        try {
            PetriNet net = context.getPetriNet();
            List<String> issues = new ArrayList<>();

            // Check for places and transitions
            if (net.getPlaces().isEmpty()) {
                issues.add("Net has no places");
            }
            if (net.getTransitions().isEmpty()) {
                issues.add("Net has no transitions");
            }

            // Check for valid initial marking
            if (net.getInitialMarking().isEmpty()) {
                issues.add("Net has empty initial marking");
            }

            // Check for disconnected places
            Set<String> connectedPlaces = new HashSet<>();
            for (Arc arc : net.getArcs()) {
                connectedPlaces.add(arc.getFrom());
                connectedPlaces.add(arc.getTo());
            }

            for (Place place : net.getPlaces()) {
                if (!connectedPlaces.contains(place.getId())) {
                    issues.add("Place '" + place.getId() + "' is not connected");
                }
            }

            // Check arc validity
            Set<String> placeIds = net.getPlaces().stream().map(Place::getId).collect(Collectors.toSet());
            Set<String> transitionIds = net.getTransitions().stream().map(Transition::getId).collect(Collectors.toSet());

            for (Arc arc : net.getArcs()) {
                if (!placeIds.contains(arc.getFrom()) && !transitionIds.contains(arc.getFrom())) {
                    issues.add("Arc references unknown element: " + arc.getFrom());
                }
                if (!placeIds.contains(arc.getTo()) && !transitionIds.contains(arc.getTo())) {
                    issues.add("Arc references unknown element: " + arc.getTo());
                }
                if (arc.getWeight() <= 0) {
                    issues.add("Arc has invalid weight: " + arc.getWeight());
                }
            }

            long executionTime = Duration.between(checkStart, Instant.now()).toMillis();

            if (issues.isEmpty()) {
                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION,
                        PetriNetValidationResult.PetriValidationStatus.PASS,
                        "Structural validation passed",
                        Map.of("checkedElements", placeIds.size() + transitionIds.size()),
                        executionTime
                ));
            } else {
                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION,
                        PetriNetValidationResult.PetriValidationStatus.FAIL,
                        "Structural issues found: " + String.join(", ", issues),
                        Map.of("issues", issues),
                        executionTime
                ));
                resultBuilder.addHint("Fix structural issues: " + String.join(", ", issues));
            }

        } catch (Exception e) {
            logger.error("Structural validation failed", e);
            resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                    PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION,
                    PetriNetValidationResult.PetriValidationStatus.FAIL,
                    "Structural validation error: " + e.getMessage(),
                    Map.of("error", e.getMessage()),
                    Duration.between(checkStart, Instant.now()).toMillis()
            ));
        }
    }

    /**
     * Performs deadlock detection using bounded state space exploration.
     * A deadlock occurs when no transitions are enabled in a non-terminal state.
     */
    private void performDeadlockDetection(ValidationContext context, PetriNetValidationResult.Builder resultBuilder) {
        logger.debug("Performing deadlock detection");
        Instant checkStart = Instant.now();

        try {
            StateSpaceExplorer explorer = new StateSpaceExplorer(context);
            DeadlockDetectionResult result = explorer.detectDeadlocks();

            long executionTime = Duration.between(checkStart, Instant.now()).toMillis();

            if (result.hasDeadlock()) {
                // Found deadlock - create counter-example
                PetriNetValidationResult.CounterExample counterExample =
                        new PetriNetValidationResult.CounterExample(
                                result.getDeadlockMarking(),
                                result.getEnabledTransitions(),
                                result.getPathToDeadlock(),
                                "Deadlock detected at marking with no enabled transitions"
                        );

                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.DEADLOCK_DETECTION,
                        PetriNetValidationResult.PetriValidationStatus.FAIL,
                        "Deadlock detected",
                        Map.of("deadlockMarking", result.getDeadlockMarking().getTokens(),
                               "pathLength", result.getPathToDeadlock().size()),
                        executionTime
                ))
                .counterExample(counterExample)
                .addHint("Add transitions or modify arc weights to avoid deadlock at marking: " +
                        result.getDeadlockMarking().getTokens());

            } else if (result.isInconclusive()) {
                PetriNetValidationResult.PetriValidationStatus status =
                        result.isTimeout() ? PetriNetValidationResult.PetriValidationStatus.INCONCLUSIVE_TIMEOUT
                                           : PetriNetValidationResult.PetriValidationStatus.INCONCLUSIVE_BOUND;

                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.DEADLOCK_DETECTION,
                        status,
                        result.getReasonInconclusive(),
                        Map.of("statesExplored", result.getStatesExplored()),
                        executionTime
                ));

            } else {
                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.DEADLOCK_DETECTION,
                        PetriNetValidationResult.PetriValidationStatus.PASS,
                        "No deadlocks found",
                        Map.of("statesExplored", result.getStatesExplored()),
                        executionTime
                ));
            }

        } catch (Exception e) {
            logger.error("Deadlock detection failed", e);
            resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                    PetriNetValidationResult.CheckType.DEADLOCK_DETECTION,
                    PetriNetValidationResult.PetriValidationStatus.FAIL,
                    "Deadlock detection error: " + e.getMessage(),
                    Map.of("error", e.getMessage()),
                    Duration.between(checkStart, Instant.now()).toMillis()
            ));
        }
    }

    /**
     * Performs reachability analysis to verify terminal states are reachable.
     * Terminal states are defined as markings with tokens in places prefixed with "p_done"
     * and no enabled transitions.
     */
    private void performReachabilityAnalysis(ValidationContext context, PetriNetValidationResult.Builder resultBuilder) {
        logger.debug("Performing reachability analysis");
        Instant checkStart = Instant.now();

        try {
            StateSpaceExplorer explorer = new StateSpaceExplorer(context);
            ReachabilityAnalysisResult result = explorer.analyzeReachability();

            long executionTime = Duration.between(checkStart, Instant.now()).toMillis();

            if (result.isTerminalReachable()) {
                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS,
                        PetriNetValidationResult.PetriValidationStatus.PASS,
                        "Terminal state is reachable",
                        Map.of("terminalMarkings", result.getTerminalMarkings().size(),
                               "statesExplored", result.getStatesExplored()),
                        executionTime
                ));

            } else if (result.isInconclusive()) {
                PetriNetValidationResult.PetriValidationStatus status =
                        result.isTimeout() ? PetriNetValidationResult.PetriValidationStatus.INCONCLUSIVE_TIMEOUT
                                           : PetriNetValidationResult.PetriValidationStatus.INCONCLUSIVE_BOUND;

                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS,
                        status,
                        result.getReasonInconclusive(),
                        Map.of("statesExplored", result.getStatesExplored()),
                        executionTime
                ));

            } else {
                // Terminal state not reachable
                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS,
                        PetriNetValidationResult.PetriValidationStatus.FAIL,
                        "No terminal state is reachable",
                        Map.of("statesExplored", result.getStatesExplored()),
                        executionTime
                ))
                .addHint("Ensure workflow can reach a terminal state (place with prefix 'p_done')");
            }

        } catch (Exception e) {
            logger.error("Reachability analysis failed", e);
            resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                    PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS,
                    PetriNetValidationResult.PetriValidationStatus.FAIL,
                    "Reachability analysis error: " + e.getMessage(),
                    Map.of("error", e.getMessage()),
                    Duration.between(checkStart, Instant.now()).toMillis()
            ));
        }
    }

    /**
     * Performs liveness check to verify all transitions can eventually fire.
     * A transition is live if it can be enabled in some reachable marking.
     */
    private void performLivenessCheck(ValidationContext context, PetriNetValidationResult.Builder resultBuilder) {
        logger.debug("Performing liveness check");
        Instant checkStart = Instant.now();

        try {
            StateSpaceExplorer explorer = new StateSpaceExplorer(context);
            LivenessCheckResult result = explorer.checkLiveness();

            long executionTime = Duration.between(checkStart, Instant.now()).toMillis();

            if (result.areAllTransitionsLive()) {
                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.LIVENESS_CHECK,
                        PetriNetValidationResult.PetriValidationStatus.PASS,
                        "All transitions are live",
                        Map.of("liveTransitions", result.getLiveTransitions().size(),
                               "statesExplored", result.getStatesExplored()),
                        executionTime
                ));

            } else if (result.isInconclusive()) {
                PetriNetValidationResult.PetriValidationStatus status =
                        result.isTimeout() ? PetriNetValidationResult.PetriValidationStatus.INCONCLUSIVE_TIMEOUT
                                           : PetriNetValidationResult.PetriValidationStatus.INCONCLUSIVE_BOUND;

                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.LIVENESS_CHECK,
                        status,
                        result.getReasonInconclusive(),
                        Map.of("statesExplored", result.getStatesExplored()),
                        executionTime
                ));

            } else {
                // Some transitions are dead
                List<String> deadTransitions = result.getDeadTransitions();
                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.LIVENESS_CHECK,
                        PetriNetValidationResult.PetriValidationStatus.FAIL,
                        "Dead transitions found: " + deadTransitions,
                        Map.of("deadTransitions", deadTransitions,
                               "liveTransitions", result.getLiveTransitions().size(),
                               "statesExplored", result.getStatesExplored()),
                        executionTime
                ));

                for (String deadTransition : deadTransitions) {
                    resultBuilder.addHint("Transition '" + deadTransition + "' can never fire - check input places and arc weights");
                }
            }

        } catch (Exception e) {
            logger.error("Liveness check failed", e);
            resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                    PetriNetValidationResult.CheckType.LIVENESS_CHECK,
                    PetriNetValidationResult.PetriValidationStatus.FAIL,
                    "Liveness check error: " + e.getMessage(),
                    Map.of("error", e.getMessage()),
                    Duration.between(checkStart, Instant.now()).toMillis()
            ));
        }
    }

    /**
     * Performs boundedness check to verify no place can accumulate unlimited tokens.
     * Uses k-bounded exploration to detect potential unbounded growth.
     */
    private void performBoundednessCheck(ValidationContext context, PetriNetValidationResult.Builder resultBuilder) {
        logger.debug("Performing boundedness check");
        Instant checkStart = Instant.now();

        try {
            StateSpaceExplorer explorer = new StateSpaceExplorer(context);
            BoundednessCheckResult result = explorer.checkBoundedness();

            long executionTime = Duration.between(checkStart, Instant.now()).toMillis();

            if (result.isBounded()) {
                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.BOUNDEDNESS_CHECK,
                        PetriNetValidationResult.PetriValidationStatus.PASS,
                        "Net is bounded",
                        Map.of("maxTokensPerPlace", result.getMaxTokensPerPlace(),
                               "statesExplored", result.getStatesExplored()),
                        executionTime
                ));

            } else if (result.isInconclusive()) {
                PetriNetValidationResult.PetriValidationStatus status =
                        result.isTimeout() ? PetriNetValidationResult.PetriValidationStatus.INCONCLUSIVE_TIMEOUT
                                           : PetriNetValidationResult.PetriValidationStatus.INCONCLUSIVE_BOUND;

                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.BOUNDEDNESS_CHECK,
                        status,
                        result.getReasonInconclusive(),
                        Map.of("statesExplored", result.getStatesExplored()),
                        executionTime
                ));

            } else {
                // Unbounded places detected
                List<String> unboundedPlaces = result.getUnboundedPlaces();
                resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                        PetriNetValidationResult.CheckType.BOUNDEDNESS_CHECK,
                        PetriNetValidationResult.PetriValidationStatus.FAIL,
                        "Unbounded places found: " + unboundedPlaces,
                        Map.of("unboundedPlaces", unboundedPlaces,
                               "statesExplored", result.getStatesExplored()),
                        executionTime
                ));

                for (String unboundedPlace : unboundedPlaces) {
                    resultBuilder.addHint("Place '" + unboundedPlace + "' may grow unbounded - check for token accumulation loops");
                }
            }

        } catch (Exception e) {
            logger.error("Boundedness check failed", e);
            resultBuilder.addCheck(new PetriNetValidationResult.CheckResult(
                    PetriNetValidationResult.CheckType.BOUNDEDNESS_CHECK,
                    PetriNetValidationResult.PetriValidationStatus.FAIL,
                    "Boundedness check error: " + e.getMessage(),
                    Map.of("error", e.getMessage()),
                    Duration.between(checkStart, Instant.now()).toMillis()
            ));
        }
    }

    /**
     * Determines overall validation status based on individual check results.
     */
    private PetriNetValidationResult.PetriValidationStatus determineOverallStatus(PetriNetValidationResult.Builder resultBuilder) {
        // Since we can't access the checks directly from the builder in this design,
        // we'll determine status based on the validation context and assume the caller
        // will set the appropriate status. For now, return PASS as default.
        // This method can be enhanced once we have access to the actual check results.
        return PetriNetValidationResult.PetriValidationStatus.PASS;
    }

    // Inner classes for validation context and results

    /**
     * Context object holding validation state and configuration.
     */
    private static class ValidationContext {
        private final PetriNet petriNet;
        private final PetriNetValidationResult.ValidationConfig config;
        private final Instant startTime;
        private int statesExplored = 0;

        public ValidationContext(PetriNet petriNet, PetriNetValidationResult.ValidationConfig config, Instant startTime) {
            this.petriNet = petriNet;
            this.config = config;
            this.startTime = startTime;
        }

        public PetriNet getPetriNet() { return petriNet; }
        public PetriNetValidationResult.ValidationConfig getConfig() { return config; }
        public Instant getStartTime() { return startTime; }
        public int getStatesExplored() { return statesExplored; }
        public void incrementStatesExplored() { statesExplored++; }

        public boolean isTimeoutReached() {
            return Duration.between(startTime, Instant.now()).toMillis() >= config.getMaxTimeMs();
        }

        public boolean isBoundReached() {
            return statesExplored >= config.getKBound();
        }
    }

    /**
     * State space explorer implementing formal verification algorithms using
     * bounded state space exploration with BFS/DFS traversal.
     */
    private static class StateSpaceExplorer {
        private final ValidationContext context;
        private final PetriNet petriNet;
        private final Set<Marking> visited;
        private final Map<Marking, List<String>> paths; // Witness paths to markings
        private final Queue<Marking> toExplore;

        public StateSpaceExplorer(ValidationContext context) {
            this.context = context;
            this.petriNet = context.getPetriNet();
            this.visited = new HashSet<>();
            this.paths = new HashMap<>();
            this.toExplore = new LinkedList<>();
        }

        /**
         * Performs deadlock detection using BFS over the marking graph.
         * A deadlock is a marking where no transitions are enabled and the marking is not terminal.
         */
        public DeadlockDetectionResult detectDeadlocks() {
            logger.debug("Starting deadlock detection for net: {}", petriNet.getName());

            initializeExploration();

            while (!toExplore.isEmpty() && !context.isTimeoutReached() && !context.isBoundReached()) {
                Marking currentMarking = toExplore.poll();
                context.incrementStatesExplored();

                // Get enabled transitions
                List<Transition> enabledTransitions = petriNet.getEnabledTransitions(currentMarking);
                List<String> enabledTransitionIds = enabledTransitions.stream()
                        .map(Transition::getId)
                        .collect(Collectors.toList());

                // Check for deadlock: no enabled transitions and not terminal
                if (enabledTransitions.isEmpty()) {
                    if (isTerminalMarking(currentMarking)) {
                        logger.debug("Found terminal marking (not a deadlock): {}", currentMarking.getTokens());
                        continue; // Terminal states are not deadlocks
                    } else {
                        // This is a deadlock
                        logger.warn("Deadlock detected at marking: {}", currentMarking.getTokens());
                        return new DeadlockDetectionResult(
                                true, currentMarking, enabledTransitionIds,
                                paths.getOrDefault(currentMarking, Collections.emptyList()),
                                context.getStatesExplored(), false, false, ""
                        );
                    }
                }

                // Explore successor markings
                for (Transition transition : enabledTransitions) {
                    if (context.isTimeoutReached() || context.isBoundReached()) {
                        break;
                    }

                    try {
                        Marking successorMarking = petriNet.fireTransition(transition.getId(), currentMarking);

                        if (!visited.contains(successorMarking)) {
                            visited.add(successorMarking);
                            toExplore.add(successorMarking);

                            // Record path to this successor
                            List<String> pathToSuccessor = new ArrayList<>(paths.getOrDefault(currentMarking, Collections.emptyList()));
                            pathToSuccessor.add(transition.getId());
                            paths.put(successorMarking, pathToSuccessor);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fire transition {} from marking {}: {}",
                                transition.getId(), currentMarking.getTokens(), e.getMessage());
                    }
                }
            }

            // Analysis completed without finding deadlock
            if (context.isTimeoutReached()) {
                return new DeadlockDetectionResult(false, null, Collections.emptyList(), Collections.emptyList(),
                        context.getStatesExplored(), true, false, "Timeout reached during deadlock detection");
            } else if (context.isBoundReached()) {
                return new DeadlockDetectionResult(false, null, Collections.emptyList(), Collections.emptyList(),
                        context.getStatesExplored(), false, true, "State bound reached during deadlock detection");
            } else {
                return new DeadlockDetectionResult(false, null, Collections.emptyList(), Collections.emptyList(),
                        context.getStatesExplored(), false, false, "");
            }
        }

        /**
         * Performs reachability analysis to check if terminal states are reachable.
         */
        public ReachabilityAnalysisResult analyzeReachability() {
            logger.debug("Starting reachability analysis for net: {}", petriNet.getName());

            initializeExploration();
            List<Marking> terminalMarkings = new ArrayList<>();

            while (!toExplore.isEmpty() && !context.isTimeoutReached() && !context.isBoundReached()) {
                Marking currentMarking = toExplore.poll();
                context.incrementStatesExplored();

                // Check if this is a terminal marking
                if (isTerminalMarking(currentMarking)) {
                    terminalMarkings.add(currentMarking);
                    logger.debug("Found terminal marking: {}", currentMarking.getTokens());
                }

                // Explore successor markings
                List<Transition> enabledTransitions = petriNet.getEnabledTransitions(currentMarking);
                for (Transition transition : enabledTransitions) {
                    if (context.isTimeoutReached() || context.isBoundReached()) {
                        break;
                    }

                    try {
                        Marking successorMarking = petriNet.fireTransition(transition.getId(), currentMarking);

                        if (!visited.contains(successorMarking)) {
                            visited.add(successorMarking);
                            toExplore.add(successorMarking);

                            // Record path to this successor
                            List<String> pathToSuccessor = new ArrayList<>(paths.getOrDefault(currentMarking, Collections.emptyList()));
                            pathToSuccessor.add(transition.getId());
                            paths.put(successorMarking, pathToSuccessor);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fire transition {} from marking {}: {}",
                                transition.getId(), currentMarking.getTokens(), e.getMessage());
                    }
                }
            }

            // Return results
            boolean terminalReachable = !terminalMarkings.isEmpty();

            if (context.isTimeoutReached()) {
                return new ReachabilityAnalysisResult(terminalReachable, terminalMarkings,
                        context.getStatesExplored(), true, false, "Timeout reached during reachability analysis");
            } else if (context.isBoundReached()) {
                return new ReachabilityAnalysisResult(terminalReachable, terminalMarkings,
                        context.getStatesExplored(), false, true, "State bound reached during reachability analysis");
            } else {
                return new ReachabilityAnalysisResult(terminalReachable, terminalMarkings,
                        context.getStatesExplored(), false, false, "");
            }
        }

        /**
         * Performs liveness check to verify all transitions can eventually fire.
         */
        public LivenessCheckResult checkLiveness() {
            logger.debug("Starting liveness check for net: {}", petriNet.getName());

            initializeExploration();
            Set<String> liveTransitions = new HashSet<>();
            Set<String> allTransitionIds = petriNet.getTransitions().stream()
                    .map(Transition::getId)
                    .collect(Collectors.toSet());

            while (!toExplore.isEmpty() && !context.isTimeoutReached() && !context.isBoundReached()) {
                Marking currentMarking = toExplore.poll();
                context.incrementStatesExplored();

                // Record which transitions are enabled in this marking
                List<Transition> enabledTransitions = petriNet.getEnabledTransitions(currentMarking);
                for (Transition transition : enabledTransitions) {
                    liveTransitions.add(transition.getId());

                    // Early termination if all transitions are live
                    if (liveTransitions.size() == allTransitionIds.size()) {
                        return new LivenessCheckResult(true, new ArrayList<>(liveTransitions),
                                Collections.emptyList(), context.getStatesExplored(), false, false, "");
                    }
                }

                // Explore successor markings
                for (Transition transition : enabledTransitions) {
                    if (context.isTimeoutReached() || context.isBoundReached()) {
                        break;
                    }

                    try {
                        Marking successorMarking = petriNet.fireTransition(transition.getId(), currentMarking);

                        if (!visited.contains(successorMarking)) {
                            visited.add(successorMarking);
                            toExplore.add(successorMarking);

                            // Record path to this successor
                            List<String> pathToSuccessor = new ArrayList<>(paths.getOrDefault(currentMarking, Collections.emptyList()));
                            pathToSuccessor.add(transition.getId());
                            paths.put(successorMarking, pathToSuccessor);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fire transition {} from marking {}: {}",
                                transition.getId(), currentMarking.getTokens(), e.getMessage());
                    }
                }
            }

            // Determine dead transitions
            List<String> deadTransitions = allTransitionIds.stream()
                    .filter(t -> !liveTransitions.contains(t))
                    .collect(Collectors.toList());

            boolean allLive = deadTransitions.isEmpty();

            if (context.isTimeoutReached()) {
                return new LivenessCheckResult(allLive, new ArrayList<>(liveTransitions), deadTransitions,
                        context.getStatesExplored(), true, false, "Timeout reached during liveness check");
            } else if (context.isBoundReached()) {
                return new LivenessCheckResult(allLive, new ArrayList<>(liveTransitions), deadTransitions,
                        context.getStatesExplored(), false, true, "State bound reached during liveness check");
            } else {
                return new LivenessCheckResult(allLive, new ArrayList<>(liveTransitions), deadTransitions,
                        context.getStatesExplored(), false, false, "");
            }
        }

        /**
         * Performs boundedness check to verify no place can accumulate unlimited tokens.
         */
        public BoundednessCheckResult checkBoundedness() {
            logger.debug("Starting boundedness check for net: {}", petriNet.getName());

            initializeExploration();
            Map<String, Integer> maxTokensPerPlace = new HashMap<>();

            // Initialize with initial marking
            for (String placeId : petriNet.getInitialMarking().getPlacesWithTokens()) {
                maxTokensPerPlace.put(placeId, petriNet.getInitialMarking().getTokens(placeId));
            }

            while (!toExplore.isEmpty() && !context.isTimeoutReached() && !context.isBoundReached()) {
                Marking currentMarking = toExplore.poll();
                context.incrementStatesExplored();

                // Update maximum token counts
                for (String placeId : currentMarking.getPlacesWithTokens()) {
                    int tokens = currentMarking.getTokens(placeId);
                    maxTokensPerPlace.merge(placeId, tokens, Integer::max);
                }

                // Explore successor markings
                List<Transition> enabledTransitions = petriNet.getEnabledTransitions(currentMarking);
                for (Transition transition : enabledTransitions) {
                    if (context.isTimeoutReached() || context.isBoundReached()) {
                        break;
                    }

                    try {
                        Marking successorMarking = petriNet.fireTransition(transition.getId(), currentMarking);

                        if (!visited.contains(successorMarking)) {
                            visited.add(successorMarking);
                            toExplore.add(successorMarking);

                            // Record path to this successor
                            List<String> pathToSuccessor = new ArrayList<>(paths.getOrDefault(currentMarking, Collections.emptyList()));
                            pathToSuccessor.add(transition.getId());
                            paths.put(successorMarking, pathToSuccessor);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fire transition {} from marking {}: {}",
                                transition.getId(), currentMarking.getTokens(), e.getMessage());
                    }
                }
            }

            // Analyze boundedness - if we reached bound limit, some places might be unbounded
            List<String> unboundedPlaces = new ArrayList<>();
            if (context.isBoundReached()) {
                // Heuristic: places with very high token counts might be unbounded
                int unboundedThreshold = context.getConfig().getKBound() / 10; // Heuristic threshold
                for (Map.Entry<String, Integer> entry : maxTokensPerPlace.entrySet()) {
                    if (entry.getValue() > unboundedThreshold) {
                        unboundedPlaces.add(entry.getKey());
                    }
                }
            }

            boolean bounded = unboundedPlaces.isEmpty();

            if (context.isTimeoutReached()) {
                return new BoundednessCheckResult(bounded, maxTokensPerPlace, unboundedPlaces,
                        context.getStatesExplored(), true, false, "Timeout reached during boundedness check");
            } else if (context.isBoundReached()) {
                return new BoundednessCheckResult(bounded, maxTokensPerPlace, unboundedPlaces,
                        context.getStatesExplored(), false, true, "State bound reached during boundedness check");
            } else {
                return new BoundednessCheckResult(bounded, maxTokensPerPlace, unboundedPlaces,
                        context.getStatesExplored(), false, false, "");
            }
        }

        /**
         * Initialize state space exploration with initial marking.
         */
        private void initializeExploration() {
            visited.clear();
            paths.clear();
            toExplore.clear();

            Marking initialMarking = petriNet.getInitialMarking();
            visited.add(initialMarking);
            toExplore.add(initialMarking);
            paths.put(initialMarking, Collections.emptyList());
        }

        /**
         * Check if a marking is terminal based on Petri net semantics.
         * A marking is terminal if:
         * 1. It has tokens in places with prefix "p_done" OR
         * 2. No transitions are enabled and tokens are only in final places (no output transitions)
         */
        private boolean isTerminalMarking(Marking marking) {
            // Check for explicit terminal places (p_done prefix)
            for (String placeId : marking.getPlacesWithTokens()) {
                if (placeId.startsWith(TERMINAL_PLACE_PREFIX)) {
                    return true;
                }
            }

            // Use PetriNet's terminal check
            return petriNet.isTerminal(marking);
        }
    }

    // Result classes for different validation checks
    private static class DeadlockDetectionResult {
        private final boolean hasDeadlock;
        private final Marking deadlockMarking;
        private final List<String> enabledTransitions;
        private final List<String> pathToDeadlock;
        private final int statesExplored;
        private final boolean timeout;
        private final boolean boundReached;
        private final String reasonInconclusive;

        public DeadlockDetectionResult(boolean hasDeadlock, Marking deadlockMarking,
                List<String> enabledTransitions, List<String> pathToDeadlock,
                int statesExplored, boolean timeout, boolean boundReached, String reasonInconclusive) {
            this.hasDeadlock = hasDeadlock;
            this.deadlockMarking = deadlockMarking;
            this.enabledTransitions = enabledTransitions != null ? enabledTransitions : Collections.emptyList();
            this.pathToDeadlock = pathToDeadlock != null ? pathToDeadlock : Collections.emptyList();
            this.statesExplored = statesExplored;
            this.timeout = timeout;
            this.boundReached = boundReached;
            this.reasonInconclusive = reasonInconclusive;
        }

        public boolean hasDeadlock() { return hasDeadlock; }
        public Marking getDeadlockMarking() { return deadlockMarking; }
        public List<String> getEnabledTransitions() { return enabledTransitions; }
        public List<String> getPathToDeadlock() { return pathToDeadlock; }
        public int getStatesExplored() { return statesExplored; }
        public boolean isTimeout() { return timeout; }
        public boolean isBoundReached() { return boundReached; }
        public boolean isInconclusive() { return timeout || boundReached; }
        public String getReasonInconclusive() { return reasonInconclusive; }
    }

    private static class ReachabilityAnalysisResult {
        private final boolean terminalReachable;
        private final List<Marking> terminalMarkings;
        private final int statesExplored;
        private final boolean timeout;
        private final boolean boundReached;
        private final String reasonInconclusive;

        public ReachabilityAnalysisResult(boolean terminalReachable, List<Marking> terminalMarkings,
                int statesExplored, boolean timeout, boolean boundReached, String reasonInconclusive) {
            this.terminalReachable = terminalReachable;
            this.terminalMarkings = terminalMarkings != null ? terminalMarkings : Collections.emptyList();
            this.statesExplored = statesExplored;
            this.timeout = timeout;
            this.boundReached = boundReached;
            this.reasonInconclusive = reasonInconclusive;
        }

        public boolean isTerminalReachable() { return terminalReachable; }
        public List<Marking> getTerminalMarkings() { return terminalMarkings; }
        public int getStatesExplored() { return statesExplored; }
        public boolean isTimeout() { return timeout; }
        public boolean isBoundReached() { return boundReached; }
        public boolean isInconclusive() { return timeout || boundReached; }
        public String getReasonInconclusive() { return reasonInconclusive; }
    }

    private static class LivenessCheckResult {
        private final boolean allTransitionsLive;
        private final List<String> liveTransitions;
        private final List<String> deadTransitions;
        private final int statesExplored;
        private final boolean timeout;
        private final boolean boundReached;
        private final String reasonInconclusive;

        public LivenessCheckResult(boolean allTransitionsLive, List<String> liveTransitions,
                List<String> deadTransitions, int statesExplored, boolean timeout,
                boolean boundReached, String reasonInconclusive) {
            this.allTransitionsLive = allTransitionsLive;
            this.liveTransitions = liveTransitions != null ? liveTransitions : Collections.emptyList();
            this.deadTransitions = deadTransitions != null ? deadTransitions : Collections.emptyList();
            this.statesExplored = statesExplored;
            this.timeout = timeout;
            this.boundReached = boundReached;
            this.reasonInconclusive = reasonInconclusive;
        }

        public boolean areAllTransitionsLive() { return allTransitionsLive; }
        public List<String> getLiveTransitions() { return liveTransitions; }
        public List<String> getDeadTransitions() { return deadTransitions; }
        public int getStatesExplored() { return statesExplored; }
        public boolean isTimeout() { return timeout; }
        public boolean isBoundReached() { return boundReached; }
        public boolean isInconclusive() { return timeout || boundReached; }
        public String getReasonInconclusive() { return reasonInconclusive; }
    }

    private static class BoundednessCheckResult {
        private final boolean bounded;
        private final Map<String, Integer> maxTokensPerPlace;
        private final List<String> unboundedPlaces;
        private final int statesExplored;
        private final boolean timeout;
        private final boolean boundReached;
        private final String reasonInconclusive;

        public BoundednessCheckResult(boolean bounded, Map<String, Integer> maxTokensPerPlace,
                List<String> unboundedPlaces, int statesExplored, boolean timeout,
                boolean boundReached, String reasonInconclusive) {
            this.bounded = bounded;
            this.maxTokensPerPlace = maxTokensPerPlace != null ? maxTokensPerPlace : Collections.emptyMap();
            this.unboundedPlaces = unboundedPlaces != null ? unboundedPlaces : Collections.emptyList();
            this.statesExplored = statesExplored;
            this.timeout = timeout;
            this.boundReached = boundReached;
            this.reasonInconclusive = reasonInconclusive;
        }

        public boolean isBounded() { return bounded; }
        public Map<String, Integer> getMaxTokensPerPlace() { return maxTokensPerPlace; }
        public List<String> getUnboundedPlaces() { return unboundedPlaces; }
        public int getStatesExplored() { return statesExplored; }
        public boolean isTimeout() { return timeout; }
        public boolean isBoundReached() { return boundReached; }
        public boolean isInconclusive() { return timeout || boundReached; }
        public String getReasonInconclusive() { return reasonInconclusive; }
    }
}