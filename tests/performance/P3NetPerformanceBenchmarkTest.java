package performance;

import core.petri.*;
import core.petri.grammar.AutomationGrammar;
import core.petri.validation.PetriNetValidator;
import core.petri.simulation.PetriTokenSimulator;
import core.petri.simulation.SimulationConfig;
import core.petri.simulation.SimulationResult;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance benchmarking test suite for P3Net pipeline components.
 *
 * This test suite focuses on:
 * - Throughput benchmarks for each component
 * - Latency analysis under different loads
 * - Memory usage profiling
 * - Concurrent execution performance
 * - Scalability testing with varying workflow complexity
 * - Regression detection for performance degradation
 *
 * Target Performance Goals:
 * - Single workflow processing: <500ms per component
 * - End-to-end pipeline: <2000ms
 * - Concurrent processing: 10+ workflows/second
 * - Memory usage: <100MB per workflow
 *
 * @author Obvian Labs
 * @since P3Net Performance Testing Phase
 */
@SpringBootTest
@ActiveProfiles("performance-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class P3NetPerformanceBenchmarkTest {

    private AutomationGrammar automationGrammar;
    private PetriNetValidator petriNetValidator;
    private PetriTokenSimulator petriTokenSimulator;

    // Performance tracking
    private static final Map<String, List<Duration>> performanceMetrics = new HashMap<>();
    private static final Map<String, Long> memoryMetrics = new HashMap<>();

    // Performance targets
    private static final long COMPONENT_LATENCY_TARGET_MS = 500L;
    private static final long PIPELINE_LATENCY_TARGET_MS = 2000L;
    private static final double THROUGHPUT_TARGET_PER_SECOND = 10.0;
    private static final long MEMORY_TARGET_MB = 100L;

    @BeforeAll
    static void setupPerformanceTracking() {
        performanceMetrics.put("automationGrammar", new ArrayList<>());
        performanceMetrics.put("petriNetValidator", new ArrayList<>());
        performanceMetrics.put("petriTokenSimulator", new ArrayList<>());
        performanceMetrics.put("endToEndPipeline", new ArrayList<>());
    }

    @BeforeEach
    void setUp() {
        automationGrammar = new AutomationGrammar();
        petriNetValidator = new PetriNetValidator();
        petriTokenSimulator = new PetriTokenSimulator();

        // Warm up JVM for more accurate measurements
        performWarmup();
    }

    @Nested
    @DisplayName("Component Latency Benchmarks")
    @Order(1)
    class ComponentLatencyBenchmarks {

        @Test
        @DisplayName("AutomationGrammar transformation latency benchmark")
        void automationGrammarTransformationLatencyBenchmark() throws Exception {
            List<PetriIntentSpec> testWorkflows = createVariousComplexityWorkflows();

            for (PetriIntentSpec intent : testWorkflows) {
                // Multiple runs for statistical significance
                List<Duration> runTimes = new ArrayList<>();

                for (int run = 0; run < 10; run++) {
                    Instant start = Instant.now();
                    PetriNet petriNet = automationGrammar.transform(intent);
                    Duration elapsed = Duration.between(start, Instant.now());

                    runTimes.add(elapsed);

                    assertThat(petriNet).isNotNull();
                    assertThat(petriNet.getTransitions()).hasSizeGreaterThanOrEqualTo(1);
                }

                Duration averageTime = Duration.ofMillis(
                    runTimes.stream().mapToLong(Duration::toMillis).sum() / runTimes.size()
                );

                Duration maxTime = runTimes.stream().max(Duration::compareTo).orElse(Duration.ZERO);
                Duration minTime = runTimes.stream().min(Duration::compareTo).orElse(Duration.ZERO);

                performanceMetrics.get("automationGrammar").add(averageTime);

                assertThat(averageTime.toMillis())
                        .describedAs("AutomationGrammar average latency for %d-step workflow", intent.getSteps().size())
                        .isLessThan(COMPONENT_LATENCY_TARGET_MS);

                System.out.printf("✅ AutomationGrammar %d steps: avg=%dms, min=%dms, max=%dms%n",
                        intent.getSteps().size(), averageTime.toMillis(), minTime.toMillis(), maxTime.toMillis());
            }
        }

        @Test
        @DisplayName("PetriNetValidator validation latency benchmark")
        void petriNetValidatorValidationLatencyBenchmark() throws Exception {
            List<PetriNet> testPetriNets = createVariousComplexityPetriNets();

            for (PetriNet petriNet : testPetriNets) {
                List<Duration> runTimes = new ArrayList<>();

                for (int run = 0; run < 10; run++) {
                    PetriNetValidationResult.ValidationConfig config = PetriNetValidationResult.ValidationConfig.builder()
                            .kBound(100)
                            .maxTimeMs(3000L)
                            .enabledChecks(Set.of(
                                PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION,
                                PetriNetValidationResult.CheckType.DEADLOCK_DETECTION,
                                PetriNetValidationResult.CheckType.REACHABILITY_ANALYSIS
                            ))
                            .build();

                    Instant start = Instant.now();
                    PetriNetValidationResult result = petriNetValidator.validate(petriNet, config);
                    Duration elapsed = Duration.between(start, Instant.now());

                    runTimes.add(elapsed);

                    assertThat(result).isNotNull();
                }

                Duration averageTime = Duration.ofMillis(
                    runTimes.stream().mapToLong(Duration::toMillis).sum() / runTimes.size()
                );

                performanceMetrics.get("petriNetValidator").add(averageTime);

                assertThat(averageTime.toMillis())
                        .describedAs("PetriNetValidator average latency for %d-place PetriNet", petriNet.getPlaces().size())
                        .isLessThan(COMPONENT_LATENCY_TARGET_MS);

                System.out.printf("✅ PetriNetValidator %d places: avg=%dms%n",
                        petriNet.getPlaces().size(), averageTime.toMillis());
            }
        }

        @Test
        @DisplayName("PetriTokenSimulator simulation latency benchmark")
        void petriTokenSimulatorSimulationLatencyBenchmark() throws Exception {
            List<PetriNet> testPetriNets = createSimulatablePetriNets();

            for (PetriNet petriNet : testPetriNets) {
                List<Duration> runTimes = new ArrayList<>();

                for (int run = 0; run < 10; run++) {
                    SimulationConfig config = SimulationConfig.builder()
                            .mode(SimulationConfig.Mode.DETERMINISTIC)
                            .maxSteps(50)
                            .stepDelayMs(0)
                            .verbose(false)
                            .build();

                    Instant start = Instant.now();
                    SimulationResult result = petriTokenSimulator.simulate(petriNet, config);
                    Duration elapsed = Duration.between(start, Instant.now());

                    runTimes.add(elapsed);

                    assertThat(result).isNotNull();
                    assertThat(result.isSuccessful()).isTrue();
                }

                Duration averageTime = Duration.ofMillis(
                    runTimes.stream().mapToLong(Duration::toMillis).sum() / runTimes.size()
                );

                performanceMetrics.get("petriTokenSimulator").add(averageTime);

                assertThat(averageTime.toMillis())
                        .describedAs("PetriTokenSimulator average latency for %d-transition PetriNet", petriNet.getTransitions().size())
                        .isLessThan(COMPONENT_LATENCY_TARGET_MS);

                System.out.printf("✅ PetriTokenSimulator %d transitions: avg=%dms%n",
                        petriNet.getTransitions().size(), averageTime.toMillis());
            }
        }
    }

    @Nested
    @DisplayName("End-to-End Pipeline Benchmarks")
    @Order(2)
    class EndToEndPipelineBenchmarks {

        @Test
        @DisplayName("Complete pipeline latency benchmark")
        void completePipelineLatencyBenchmark() throws Exception {
            List<String> testPrompts = Arrays.asList(
                "simple task execution",
                "load data, process data, save results",
                "run tests; if pass deploy; if fail alert",
                "initialize, then process A and B in parallel, then finalize",
                "step1, step2, step3, step4, step5, step6, step7"
            );

            for (String prompt : testPrompts) {
                List<Duration> pipelineRunTimes = new ArrayList<>();

                for (int run = 0; run < 5; run++) {
                    Instant pipelineStart = Instant.now();

                    // Step 1: Parse to Intent
                    PetriIntentSpec intent = createIntentFromPrompt(prompt);

                    // Step 2: Transform to PetriNet
                    PetriNet petriNet = automationGrammar.transform(intent);

                    // Step 3: Validate
                    PetriNetValidationResult.ValidationConfig validationConfig =
                            PetriNetValidationResult.ValidationConfig.builder()
                                .kBound(50)
                                .maxTimeMs(2000L)
                                .enabledChecks(Set.of(
                                    PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION,
                                    PetriNetValidationResult.CheckType.DEADLOCK_DETECTION
                                ))
                                .build();
                    PetriNetValidationResult validationResult = petriNetValidator.validate(petriNet, validationConfig);

                    // Step 4: Simulate (only if validation passes)
                    if (validationResult.isValid()) {
                        SimulationConfig simConfig = SimulationConfig.builder()
                                .mode(SimulationConfig.Mode.DETERMINISTIC)
                                .maxSteps(30)
                                .stepDelayMs(0)
                                .verbose(false)
                                .build();
                        SimulationResult simResult = petriTokenSimulator.simulate(petriNet, simConfig);
                        assertThat(simResult.isSuccessful()).isTrue();
                    }

                    Duration pipelineTime = Duration.between(pipelineStart, Instant.now());
                    pipelineRunTimes.add(pipelineTime);
                }

                Duration averagePipelineTime = Duration.ofMillis(
                    pipelineRunTimes.stream().mapToLong(Duration::toMillis).sum() / pipelineRunTimes.size()
                );

                performanceMetrics.get("endToEndPipeline").add(averagePipelineTime);

                assertThat(averagePipelineTime.toMillis())
                        .describedAs("End-to-end pipeline latency for: %s", prompt)
                        .isLessThan(PIPELINE_LATENCY_TARGET_MS);

                System.out.printf("✅ Pipeline '%s': avg=%dms%n",
                        prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt,
                        averagePipelineTime.toMillis());
            }
        }
    }

    @Nested
    @DisplayName("Throughput Benchmarks")
    @Order(3)
    class ThroughputBenchmarks {

        @Test
        @DisplayName("Concurrent pipeline processing throughput")
        void concurrentPipelineProcessingThroughput() throws Exception {
            int concurrentWorkflows = 20;
            ExecutorService executor = Executors.newFixedThreadPool(10);

            List<PetriIntentSpec> workflows = createVariousComplexityWorkflows();

            Instant throughputStart = Instant.now();

            List<CompletableFuture<Duration>> futures = new ArrayList<>();

            for (int i = 0; i < concurrentWorkflows; i++) {
                PetriIntentSpec intent = workflows.get(i % workflows.size());

                CompletableFuture<Duration> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        Instant workflowStart = Instant.now();

                        PetriNet petriNet = automationGrammar.transform(intent);

                        PetriNetValidationResult.ValidationConfig config =
                                PetriNetValidationResult.ValidationConfig.builder()
                                    .kBound(30)
                                    .maxTimeMs(1000L)
                                    .enabledChecks(Set.of(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION))
                                    .build();

                        PetriNetValidationResult validationResult = petriNetValidator.validate(petriNet, config);

                        if (validationResult.isValid()) {
                            SimulationConfig simConfig = SimulationConfig.builder()
                                    .mode(SimulationConfig.Mode.DETERMINISTIC)
                                    .maxSteps(20)
                                    .stepDelayMs(0)
                                    .verbose(false)
                                    .build();
                            petriTokenSimulator.simulate(petriNet, simConfig);
                        }

                        return Duration.between(workflowStart, Instant.now());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all workflows to complete
            List<Duration> workflowTimes = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            Duration totalThroughputTime = Duration.between(throughputStart, Instant.now());
            executor.shutdown();

            // Calculate throughput
            double throughputPerSecond = (double) concurrentWorkflows / totalThroughputTime.toMillis() * 1000.0;

            assertThat(throughputPerSecond)
                    .describedAs("Concurrent pipeline throughput")
                    .isGreaterThan(THROUGHPUT_TARGET_PER_SECOND);

            Duration averageWorkflowTime = Duration.ofMillis(
                workflowTimes.stream().mapToLong(Duration::toMillis).sum() / workflowTimes.size()
            );

            System.out.printf("✅ Throughput: %.2f workflows/sec, avg workflow time: %dms, total time: %dms%n",
                    throughputPerSecond, averageWorkflowTime.toMillis(), totalThroughputTime.toMillis());
        }

        @Test
        @DisplayName("Component throughput under sustained load")
        void componentThroughputUnderSustainedLoad() throws Exception {
            int sustainedOperations = 100;

            // AutomationGrammar throughput test
            List<PetriIntentSpec> intents = createVariousComplexityWorkflows();
            Instant agStart = Instant.now();

            for (int i = 0; i < sustainedOperations; i++) {
                PetriIntentSpec intent = intents.get(i % intents.size());
                PetriNet result = automationGrammar.transform(intent);
                assertThat(result).isNotNull();
            }

            Duration agSustainedTime = Duration.between(agStart, Instant.now());
            double agThroughput = (double) sustainedOperations / agSustainedTime.toMillis() * 1000.0;

            // PetriNetValidator throughput test
            List<PetriNet> petriNets = createVariousComplexityPetriNets();
            Instant validatorStart = Instant.now();

            PetriNetValidationResult.ValidationConfig quickConfig =
                    PetriNetValidationResult.ValidationConfig.builder()
                        .kBound(20)
                        .maxTimeMs(500L)
                        .enabledChecks(Set.of(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION))
                        .build();

            for (int i = 0; i < sustainedOperations; i++) {
                PetriNet petriNet = petriNets.get(i % petriNets.size());
                PetriNetValidationResult result = petriNetValidator.validate(petriNet, quickConfig);
                assertThat(result).isNotNull();
            }

            Duration validatorSustainedTime = Duration.between(validatorStart, Instant.now());
            double validatorThroughput = (double) sustainedOperations / validatorSustainedTime.toMillis() * 1000.0;

            System.out.printf("✅ AutomationGrammar sustained throughput: %.2f ops/sec%n", agThroughput);
            System.out.printf("✅ PetriNetValidator sustained throughput: %.2f ops/sec%n", validatorThroughput);

            assertThat(agThroughput).isGreaterThan(20.0); // At least 20 transformations/sec
            assertThat(validatorThroughput).isGreaterThan(15.0); // At least 15 validations/sec
        }
    }

    @Nested
    @DisplayName("Memory Usage Benchmarks")
    @Order(4)
    class MemoryUsageBenchmarks {

        @Test
        @DisplayName("Memory usage per workflow processing")
        void memoryUsagePerWorkflowProcessing() throws Exception {
            // Force garbage collection before measurement
            System.gc();
            Thread.sleep(100);

            Runtime runtime = Runtime.getRuntime();
            long baselineMemory = runtime.totalMemory() - runtime.freeMemory();

            List<PetriIntentSpec> testWorkflows = createVariousComplexityWorkflows();

            for (PetriIntentSpec intent : testWorkflows) {
                long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

                // Process workflow
                PetriNet petriNet = automationGrammar.transform(intent);
                PetriNetValidationResult.ValidationConfig config =
                        PetriNetValidationResult.ValidationConfig.builder()
                            .kBound(50)
                            .maxTimeMs(1000L)
                            .enabledChecks(Set.of(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION))
                            .build();
                PetriNetValidationResult validationResult = petriNetValidator.validate(petriNet, config);

                if (validationResult.isValid()) {
                    SimulationConfig simConfig = SimulationConfig.builder()
                            .mode(SimulationConfig.Mode.DETERMINISTIC)
                            .maxSteps(20)
                            .stepDelayMs(0)
                            .verbose(false)
                            .build();
                    petriTokenSimulator.simulate(petriNet, simConfig);
                }

                long afterMemory = runtime.totalMemory() - runtime.freeMemory();
                long workflowMemoryUsage = afterMemory - beforeMemory;
                long workflowMemoryMB = workflowMemoryUsage / (1024 * 1024);

                memoryMetrics.put("workflow_" + intent.getSteps().size() + "_steps", workflowMemoryUsage);

                assertThat(workflowMemoryMB)
                        .describedAs("Memory usage for %d-step workflow should be reasonable", intent.getSteps().size())
                        .isLessThan(MEMORY_TARGET_MB);

                System.out.printf("✅ Memory usage %d steps: %dMB%n", intent.getSteps().size(), workflowMemoryMB);

                // Clean up references to allow GC
                petriNet = null;
                validationResult = null;
            }
        }

        @Test
        @DisplayName("Memory usage under sustained load")
        void memoryUsageUnderSustainedLoad() throws Exception {
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            Thread.sleep(100);

            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            List<Long> memorySnapshots = new ArrayList<>();

            // Process many workflows and track memory
            PetriIntentSpec testWorkflow = createIntentFromPrompt("sustained load test workflow");

            for (int i = 0; i < 50; i++) {
                PetriNet petriNet = automationGrammar.transform(testWorkflow);

                PetriNetValidationResult.ValidationConfig config =
                        PetriNetValidationResult.ValidationConfig.builder()
                            .kBound(20)
                            .maxTimeMs(500L)
                            .enabledChecks(Set.of(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION))
                            .build();
                petriNetValidator.validate(petriNet, config);

                if (i % 10 == 0) {
                    long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                    memorySnapshots.add(currentMemory - initialMemory);
                    System.out.printf("Memory after %d workflows: %dMB%n",
                            i, (currentMemory - initialMemory) / (1024 * 1024));
                }

                // Explicit cleanup to test memory management
                petriNet = null;

                if (i % 20 == 0) {
                    System.gc();
                    Thread.sleep(50);
                }
            }

            // Check that memory doesn't grow unboundedly
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long totalMemoryGrowth = finalMemory - initialMemory;
            long totalMemoryGrowthMB = totalMemoryGrowth / (1024 * 1024);

            assertThat(totalMemoryGrowthMB)
                    .describedAs("Memory growth under sustained load should be bounded")
                    .isLessThan(MEMORY_TARGET_MB * 2); // Allow 2x target for sustained operations

            System.out.printf("✅ Sustained load memory growth: %dMB%n", totalMemoryGrowthMB);
        }
    }

    @Nested
    @DisplayName("Scalability Analysis")
    @Order(5)
    class ScalabilityAnalysis {

        @Test
        @DisplayName("Performance scaling with workflow complexity")
        void performanceScalingWithWorkflowComplexity() throws Exception {
            int[] complexities = {2, 5, 10, 15, 20, 25};

            System.out.println("\n📊 Performance Scaling Analysis:");
            System.out.println("Complexity | Transform | Validate | Simulate | Total");
            System.out.println("-----------|-----------|----------|----------|-------");

            for (int complexity : complexities) {
                PetriIntentSpec intent = createSequentialWorkflow(complexity);

                // Measure AutomationGrammar
                Instant agStart = Instant.now();
                PetriNet petriNet = automationGrammar.transform(intent);
                Duration agTime = Duration.between(agStart, Instant.now());

                // Measure PetriNetValidator
                PetriNetValidationResult.ValidationConfig config =
                        PetriNetValidationResult.ValidationConfig.builder()
                            .kBound(Math.min(100, complexity * 5))
                            .maxTimeMs(Math.min(5000L, complexity * 200L))
                            .enabledChecks(Set.of(
                                PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION,
                                PetriNetValidationResult.CheckType.DEADLOCK_DETECTION
                            ))
                            .build();

                Instant validatorStart = Instant.now();
                PetriNetValidationResult validationResult = petriNetValidator.validate(petriNet, config);
                Duration validatorTime = Duration.between(validatorStart, Instant.now());

                // Measure PetriTokenSimulator (only if validation passes)
                Duration simulatorTime = Duration.ZERO;
                if (validationResult.isValid()) {
                    SimulationConfig simConfig = SimulationConfig.builder()
                            .mode(SimulationConfig.Mode.DETERMINISTIC)
                            .maxSteps(complexity * 3)
                            .stepDelayMs(0)
                            .verbose(false)
                            .build();

                    Instant simStart = Instant.now();
                    petriTokenSimulator.simulate(petriNet, simConfig);
                    simulatorTime = Duration.between(simStart, Instant.now());
                }

                Duration totalTime = agTime.plus(validatorTime).plus(simulatorTime);

                System.out.printf("%9d | %7dms | %6dms | %6dms | %5dms%n",
                        complexity,
                        agTime.toMillis(),
                        validatorTime.toMillis(),
                        simulatorTime.toMillis(),
                        totalTime.toMillis());

                // Performance should scale reasonably (not exponentially)
                if (complexity > 10) {
                    assertThat(totalTime.toMillis())
                            .describedAs("Performance should scale reasonably for complexity %d", complexity)
                            .isLessThan(complexity * 200L); // Allow linear scaling with some overhead
                }
            }

            System.out.println("-----------|-----------|----------|----------|-------");
            System.out.println("✅ Scalability analysis completed");
        }
    }

    // Helper methods

    private void performWarmup() {
        // Warm up JVM with a few quick operations
        try {
            PetriIntentSpec warmupIntent = createIntentFromPrompt("warmup task");
            PetriNet warmupNet = automationGrammar.transform(warmupIntent);

            PetriNetValidationResult.ValidationConfig warmupConfig =
                    PetriNetValidationResult.ValidationConfig.builder()
                        .kBound(10)
                        .maxTimeMs(100L)
                        .enabledChecks(Set.of(PetriNetValidationResult.CheckType.STRUCTURAL_VALIDATION))
                        .build();

            petriNetValidator.validate(warmupNet, warmupConfig);

            SimulationConfig warmupSimConfig = SimulationConfig.builder()
                    .mode(SimulationConfig.Mode.DETERMINISTIC)
                    .maxSteps(5)
                    .stepDelayMs(0)
                    .verbose(false)
                    .build();

            petriTokenSimulator.simulate(warmupNet, warmupSimConfig);
        } catch (Exception e) {
            // Ignore warmup failures
        }
    }

    private List<PetriIntentSpec> createVariousComplexityWorkflows() {
        return Arrays.asList(
            createSequentialWorkflow(3),
            createSequentialWorkflow(5),
            createSequentialWorkflow(8),
            createParallelWorkflow(),
            createChoiceWorkflow(),
            createComplexWorkflow()
        );
    }

    private List<PetriNet> createVariousComplexityPetriNets() throws Exception {
        return createVariousComplexityWorkflows().stream()
                .map(intent -> {
                    try {
                        return automationGrammar.transform(intent);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private List<PetriNet> createSimulatablePetriNets() throws Exception {
        // Create PetriNets that are known to be simulatable
        return Arrays.asList(
            createSequentialWorkflow(3),
            createSequentialWorkflow(5),
            createSimpleParallelWorkflow()
        ).stream()
                .map(intent -> {
                    try {
                        return automationGrammar.transform(intent);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    private PetriIntentSpec createIntentFromPrompt(String prompt) {
        return PetriIntentSpec.builder()
                .name("Generated from: " + prompt)
                .description(prompt)
                .originalPrompt(prompt)
                .templateId("generated")
                .addActionStep("task1", "Execute: " + prompt)
                .build();
    }

    private PetriIntentSpec createSequentialWorkflow(int steps) {
        PetriIntentSpec.Builder builder = PetriIntentSpec.builder()
                .name(String.format("Sequential Workflow %d Steps", steps))
                .description(String.format("Sequential workflow with %d steps", steps))
                .originalPrompt(String.format("%d sequential steps", steps))
                .templateId("sequential-" + steps);

        String previousStep = null;
        for (int i = 1; i <= steps; i++) {
            String stepId = "step" + i;
            if (previousStep != null) {
                builder.addActionStep(stepId, "Sequential step " + i, Arrays.asList(previousStep));
            } else {
                builder.addActionStep(stepId, "Sequential step " + i);
            }
            previousStep = stepId;
        }

        return builder.build();
    }

    private PetriIntentSpec createParallelWorkflow() {
        return PetriIntentSpec.builder()
                .name("Parallel Workflow")
                .description("workflow with parallel execution")
                .originalPrompt("init, then A and B in parallel, then sync")
                .templateId("parallel")
                .addActionStep("init", "Initialize")
                .addStep(new PetriIntentSpec.IntentStep(
                        "parallel_ab", PetriIntentSpec.StepType.PARALLEL,
                        "Execute A and B in parallel",
                        Arrays.asList("init"), new HashMap<>(), null, new HashMap<>()
                ))
                .addStep(new PetriIntentSpec.IntentStep(
                        "sync_ab", PetriIntentSpec.StepType.SYNC,
                        "Synchronize A and B",
                        Arrays.asList("parallel_ab"), new HashMap<>(), null, new HashMap<>()
                ))
                .addActionStep("finalize", "Finalize", Arrays.asList("sync_ab"))
                .build();
    }

    private PetriIntentSpec createSimpleParallelWorkflow() {
        return PetriIntentSpec.builder()
                .name("Simple Parallel")
                .description("simple parallel workflow")
                .originalPrompt("start, then A and B, then end")
                .templateId("simple-parallel")
                .addActionStep("start", "Start")
                .addActionStep("taskA", "Task A", Arrays.asList("start"))
                .addActionStep("taskB", "Task B", Arrays.asList("start"))
                .addActionStep("end", "End", Arrays.asList("taskA", "taskB"))
                .build();
    }

    private PetriIntentSpec createChoiceWorkflow() {
        return PetriIntentSpec.builder()
                .name("Choice Workflow")
                .description("workflow with decision points")
                .originalPrompt("analyze; if good continue; if bad retry")
                .templateId("choice")
                .addActionStep("analyze", "Analyze input")
                .addStep(new PetriIntentSpec.IntentStep(
                        "decision", PetriIntentSpec.StepType.CHOICE,
                        "Make decision",
                        Arrays.asList("analyze"), new HashMap<>(), null,
                        Map.of("paths", Arrays.asList("good", "bad"))
                ))
                .addStep(new PetriIntentSpec.IntentStep(
                        "continue", PetriIntentSpec.StepType.ACTION,
                        "Continue processing",
                        Arrays.asList("decision"), new HashMap<>(), "good", new HashMap<>()
                ))
                .addStep(new PetriIntentSpec.IntentStep(
                        "retry", PetriIntentSpec.StepType.ACTION,
                        "Retry processing",
                        Arrays.asList("decision"), new HashMap<>(), "bad", new HashMap<>()
                ))
                .build();
    }

    private PetriIntentSpec createComplexWorkflow() {
        return PetriIntentSpec.builder()
                .name("Complex Workflow")
                .description("complex workflow with multiple patterns")
                .originalPrompt("init, validate; if ok then process A and B in parallel and notify; if not ok then cleanup")
                .templateId("complex")
                .addActionStep("init", "Initialize")
                .addActionStep("validate", "Validate", Arrays.asList("init"))
                .addStep(new PetriIntentSpec.IntentStep(
                        "validation_choice", PetriIntentSpec.StepType.CHOICE,
                        "Check validation result",
                        Arrays.asList("validate"), new HashMap<>(), null,
                        Map.of("paths", Arrays.asList("valid", "invalid"))
                ))
                .addStep(new PetriIntentSpec.IntentStep(
                        "parallel_processing", PetriIntentSpec.StepType.PARALLEL,
                        "Process A and B in parallel",
                        Arrays.asList("validation_choice"), new HashMap<>(), "valid", new HashMap<>()
                ))
                .addStep(new PetriIntentSpec.IntentStep(
                        "sync_processing", PetriIntentSpec.StepType.SYNC,
                        "Sync parallel processing",
                        Arrays.asList("parallel_processing"), new HashMap<>(), null, new HashMap<>()
                ))
                .addActionStep("notify", "Send notification", Arrays.asList("sync_processing"))
                .addStep(new PetriIntentSpec.IntentStep(
                        "cleanup", PetriIntentSpec.StepType.ACTION,
                        "Cleanup after validation failure",
                        Arrays.asList("validation_choice"), new HashMap<>(), "invalid", new HashMap<>()
                ))
                .build();
    }

    @AfterAll
    static void printPerformanceSummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 P3Net Performance Benchmark Summary");
        System.out.println("=".repeat(80));

        performanceMetrics.forEach((component, times) -> {
            if (!times.isEmpty()) {
                OptionalDouble avgTime = times.stream().mapToLong(Duration::toMillis).average();
                long maxTime = times.stream().mapToLong(Duration::toMillis).max().orElse(0L);
                long minTime = times.stream().mapToLong(Duration::toMillis).min().orElse(0L);

                System.out.printf("📈 %s: avg=%.1fms, min=%dms, max=%dms, samples=%d%n",
                        component, avgTime.orElse(0.0), minTime, maxTime, times.size());
            }
        });

        System.out.println("\n🎯 Performance Targets:");
        System.out.printf("   Component Latency: <%dms%n", COMPONENT_LATENCY_TARGET_MS);
        System.out.printf("   Pipeline Latency: <%dms%n", PIPELINE_LATENCY_TARGET_MS);
        System.out.printf("   Throughput: >%.1f workflows/sec%n", THROUGHPUT_TARGET_PER_SECOND);
        System.out.printf("   Memory Usage: <%dMB per workflow%n", MEMORY_TARGET_MB);

        System.out.println("\n✅ Performance benchmark suite completed successfully");
        System.out.println("=".repeat(80));
    }
}