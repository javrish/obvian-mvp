/* Copyright (c) 2025 Rishabh Pathak. Licensed under the MIT License. */

package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Utility for generating and managing golden test outputs for P3Net regression testing.
 *
 * Golden outputs are reference results from known-good test executions that are used
 * to detect regressions in future test runs. This utility:
 *
 * - Captures structured test outputs (PetriNets, validation results, simulation traces, DAGs)
 * - Stores them in versioned JSON format with metadata
 * - Provides comparison capabilities for regression detection
 * - Manages golden output lifecycle (create, update, archive)
 *
 * Usage:
 * ```java
 * GoldenOutputGenerator generator = new GoldenOutputGenerator();
 *
 * // Capture golden output
 * generator.captureDevOpsWorkflow(parseResponse, petriNet, validationResult, simulationResult, dag);
 *
 * // Load for comparison
 * GoldenOutput golden = generator.loadGoldenOutput("devops-pipeline", "v1.0");
 * ```
 *
 * @author Obvian Labs
 * @since P3Net Golden Testing Phase
 */
public class GoldenOutputGenerator {

  private final ObjectMapper objectMapper;
  private final String goldenOutputsDir;
  private static final String GOLDEN_VERSION = "1.0";
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  public GoldenOutputGenerator() {
    this(System.getProperty("p3net.golden.outputs.dir", "target/test-reports/golden-outputs/"));
  }

  public GoldenOutputGenerator(String outputDirectory) {
    this.goldenOutputsDir = outputDirectory;
    this.objectMapper =
        new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Ensure output directory exists
    try {
      Files.createDirectories(Paths.get(goldenOutputsDir));
    } catch (IOException e) {
      throw new RuntimeException(
          "Failed to create golden outputs directory: " + goldenOutputsDir, e);
    }
  }

  /**
   * Capture golden output for DevOps deployment pipeline scenario.
   */
  public void captureDevOpsWorkflow(
      Map<String, Object> parseResponse,
      Map<String, Object> petriNet,
      Map<String, Object> validationReport,
      List<Map<String, Object>> simulationTrace,
      Map<String, Object> dag) {
    GoldenOutput goldenOutput = new GoldenOutput();
    goldenOutput.scenario = "DevOps Deployment Pipeline";
    goldenOutput.version = GOLDEN_VERSION;
    goldenOutput.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    goldenOutput.description = "DevOps workflow: run tests; if pass deploy; if fail alert";

    goldenOutput.parseResponse = parseResponse;
    goldenOutput.petriNet = petriNet;
    goldenOutput.validationReport = validationReport;
    goldenOutput.simulationTrace = simulationTrace;
    goldenOutput.dag = dag;

    // Add structural metadata
    goldenOutput.metadata = createMetadata(petriNet, validationReport, simulationTrace, dag);

    saveGoldenOutput("devops-pipeline", goldenOutput);
  }

  /**
   * Capture golden output for Football training session scenario.
   */
  public void captureFootballWorkflow(
      Map<String, Object> parseResponse,
      Map<String, Object> petriNet,
      Map<String, Object> validationReport,
      List<Map<String, Object>> simulationTrace,
      Map<String, Object> dag) {
    GoldenOutput goldenOutput = new GoldenOutput();
    goldenOutput.scenario = "Football Training Session";
    goldenOutput.version = GOLDEN_VERSION;
    goldenOutput.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    goldenOutput.description =
        "Football workflow: warm-up, then pass and shoot in parallel, then cooldown";

    goldenOutput.parseResponse = parseResponse;
    goldenOutput.petriNet = petriNet;
    goldenOutput.validationReport = validationReport;
    goldenOutput.simulationTrace = simulationTrace;
    goldenOutput.dag = dag;

    goldenOutput.metadata = createMetadata(petriNet, validationReport, simulationTrace, dag);

    saveGoldenOutput("football-training", goldenOutput);
  }

  /**
   * Capture golden output for a custom workflow scenario.
   */
  public void captureCustomWorkflow(
      String scenarioId,
      String scenarioName,
      String description,
      Map<String, Object> parseResponse,
      Map<String, Object> petriNet,
      Map<String, Object> validationReport,
      List<Map<String, Object>> simulationTrace,
      Map<String, Object> dag) {
    GoldenOutput goldenOutput = new GoldenOutput();
    goldenOutput.scenario = scenarioName;
    goldenOutput.version = GOLDEN_VERSION;
    goldenOutput.timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
    goldenOutput.description = description;

    goldenOutput.parseResponse = parseResponse;
    goldenOutput.petriNet = petriNet;
    goldenOutput.validationReport = validationReport;
    goldenOutput.simulationTrace = simulationTrace;
    goldenOutput.dag = dag;

    goldenOutput.metadata = createMetadata(petriNet, validationReport, simulationTrace, dag);

    saveGoldenOutput(scenarioId, goldenOutput);
  }

  /**
   * Load golden output for comparison in regression tests.
   */
  public GoldenOutput loadGoldenOutput(String scenarioId) {
    String fileName = String.format("%s-golden-v%s.json", scenarioId, GOLDEN_VERSION);
    Path goldenFilePath = Paths.get(goldenOutputsDir, fileName);

    if (!Files.exists(goldenFilePath)) {
      throw new RuntimeException("Golden output not found: " + goldenFilePath);
    }

    try {
      return objectMapper.readValue(goldenFilePath.toFile(), GoldenOutput.class);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load golden output: " + goldenFilePath, e);
    }
  }

  /**
   * Compare current test results against golden output.
   */
  public ComparisonResult compareAgainstGolden(
      String scenarioId,
      Map<String, Object> currentPetriNet,
      Map<String, Object> currentValidationReport,
      List<Map<String, Object>> currentSimulationTrace,
      Map<String, Object> currentDag) {
    try {
      GoldenOutput golden = loadGoldenOutput(scenarioId);
      return compareResults(
          golden, currentPetriNet, currentValidationReport, currentSimulationTrace, currentDag);
    } catch (Exception e) {
      return new ComparisonResult(
          false, "Failed to load golden output: " + e.getMessage(), new ArrayList<>());
    }
  }

  /**
   * List all available golden outputs.
   */
  public List<String> listGoldenOutputs() {
    List<String> outputs = new ArrayList<>();
    File dir = new File(goldenOutputsDir);

    if (dir.exists() && dir.isDirectory()) {
      File[] files =
          dir.listFiles((d, name) -> name.endsWith("-golden-v" + GOLDEN_VERSION + ".json"));
      if (files != null) {
        for (File file : files) {
          String scenarioId = file.getName().replace("-golden-v" + GOLDEN_VERSION + ".json", "");
          outputs.add(scenarioId);
        }
      }
    }

    outputs.sort(String::compareTo);
    return outputs;
  }

  /**
   * Generate regression test report comparing all golden outputs.
   */
  public void generateRegressionReport(Map<String, Map<String, Object>> currentResults) {
    StringBuilder report = new StringBuilder();
    report.append("# P3Net Golden Output Regression Test Report\n\n");
    report
        .append("Generated: ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .append("\n");
    report.append("Golden Version: ").append(GOLDEN_VERSION).append("\n\n");

    List<String> scenarios = listGoldenOutputs();
    int totalScenarios = scenarios.size();
    int passedScenarios = 0;

    for (String scenarioId : scenarios) {
      report.append("## ").append(scenarioId).append("\n\n");

      if (currentResults.containsKey(scenarioId)) {
        Map<String, Object> current = currentResults.get(scenarioId);
        // Extract components for comparison
        Map<String, Object> petriNet = (Map<String, Object>) current.get("petriNet");
        Map<String, Object> validation = (Map<String, Object>) current.get("validationReport");
        List<Map<String, Object>> simulation =
            (List<Map<String, Object>>) current.get("simulationTrace");
        Map<String, Object> dag = (Map<String, Object>) current.get("dag");

        ComparisonResult result =
            compareAgainstGolden(scenarioId, petriNet, validation, simulation, dag);

        if (result.passed) {
          report.append("✅ **PASS** - Results match golden output\n\n");
          passedScenarios++;
        } else {
          report.append("❌ **FAIL** - ").append(result.message).append("\n\n");
          for (String difference : result.differences) {
            report.append("- ").append(difference).append("\n");
          }
          report.append("\n");
        }
      } else {
        report.append("⚠️ **SKIP** - No current results provided\n\n");
      }
    }

    report.append("## Summary\n\n");
    report.append("- Total scenarios: ").append(totalScenarios).append("\n");
    report.append("- Passed: ").append(passedScenarios).append("\n");
    report.append("- Failed: ").append(totalScenarios - passedScenarios).append("\n");
    report
        .append("- Success rate: ")
        .append(String.format("%.1f%%", (double) passedScenarios / totalScenarios * 100))
        .append("\n");

    // Write report
    String reportFileName =
        "regression-report-" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".md";
    Path reportPath = Paths.get(goldenOutputsDir, reportFileName);

    try (FileWriter writer = new FileWriter(reportPath.toFile())) {
      writer.write(report.toString());
    } catch (IOException e) {
      throw new RuntimeException("Failed to write regression report: " + reportPath, e);
    }

    System.out.println("Regression report generated: " + reportPath);
  }

  // Private helper methods

  private void saveGoldenOutput(String scenarioId, GoldenOutput goldenOutput) {
    String fileName = String.format("%s-golden-v%s.json", scenarioId, GOLDEN_VERSION);
    Path goldenFilePath = Paths.get(goldenOutputsDir, fileName);

    try {
      objectMapper.writeValue(goldenFilePath.toFile(), goldenOutput);
      System.out.println("Golden output saved: " + goldenFilePath);
    } catch (IOException e) {
      throw new RuntimeException("Failed to save golden output: " + goldenFilePath, e);
    }
  }

  private Map<String, Object> createMetadata(
      Map<String, Object> petriNet,
      Map<String, Object> validationReport,
      List<Map<String, Object>> simulationTrace,
      Map<String, Object> dag) {
    Map<String, Object> metadata = new HashMap<>();

    // PetriNet metadata
    if (petriNet != null) {
      metadata.put("petriNetPlaces", ((List<?>) petriNet.get("places")).size());
      metadata.put("petriNetTransitions", ((List<?>) petriNet.get("transitions")).size());
      metadata.put("petriNetArcs", ((List<?>) petriNet.get("arcs")).size());
    }

    // Validation metadata
    if (validationReport != null) {
      metadata.put("validationStatus", validationReport.get("status"));
      metadata.put("statesExplored", validationReport.get("statesExplored"));
    }

    // Simulation metadata
    if (simulationTrace != null) {
      metadata.put("simulationSteps", simulationTrace.size());
    }

    // DAG metadata
    if (dag != null) {
      metadata.put("dagNodes", ((List<?>) dag.get("nodes")).size());
      metadata.put("dagEdges", ((List<?>) dag.get("edges")).size());
    }

    metadata.put("generatedBy", "GoldenOutputGenerator");
    metadata.put("javaVersion", System.getProperty("java.version"));
    metadata.put("hostname", System.getProperty("user.name") + "@" + getHostname());

    return metadata;
  }

  private ComparisonResult compareResults(
      GoldenOutput golden,
      Map<String, Object> currentPetriNet,
      Map<String, Object> currentValidationReport,
      List<Map<String, Object>> currentSimulationTrace,
      Map<String, Object> currentDag) {
    List<String> differences = new ArrayList<>();

    // Compare structural elements
    if (!compareStructuralElements(golden.petriNet, currentPetriNet, "PetriNet", differences)) {
      return new ComparisonResult(false, "PetriNet structure mismatch", differences);
    }

    if (!compareValidationResults(golden.validationReport, currentValidationReport, differences)) {
      return new ComparisonResult(false, "Validation results mismatch", differences);
    }

    if (!compareSimulationResults(golden.simulationTrace, currentSimulationTrace, differences)) {
      return new ComparisonResult(false, "Simulation results mismatch", differences);
    }

    if (!compareStructuralElements(golden.dag, currentDag, "DAG", differences)) {
      return new ComparisonResult(false, "DAG structure mismatch", differences);
    }

    if (differences.isEmpty()) {
      return new ComparisonResult(true, "All results match golden output", differences);
    } else {
      return new ComparisonResult(false, "Minor differences found", differences);
    }
  }

  private boolean compareStructuralElements(
      Map<String, Object> golden,
      Map<String, Object> current,
      String elementType,
      List<String> differences) {
    if (golden == null && current == null) return true;
    if (golden == null || current == null) {
      differences.add(elementType + ": null mismatch");
      return false;
    }

    // Compare key structural properties
    String[] structuralKeys = {"places", "transitions", "arcs", "nodes", "edges"};
    for (String key : structuralKeys) {
      if (golden.containsKey(key) && current.containsKey(key)) {
        List<?> goldenList = (List<?>) golden.get(key);
        List<?> currentList = (List<?>) current.get(key);

        if (goldenList.size() != currentList.size()) {
          differences.add(
              String.format(
                  "%s.%s: size mismatch (golden=%d, current=%d)",
                  elementType, key, goldenList.size(), currentList.size()));
          return false;
        }
      }
    }

    return true;
  }

  private boolean compareValidationResults(
      Map<String, Object> golden, Map<String, Object> current, List<String> differences) {
    if (golden == null && current == null) return true;
    if (golden == null || current == null) {
      differences.add("ValidationReport: null mismatch");
      return false;
    }

    // Compare validation status
    String goldenStatus = (String) golden.get("status");
    String currentStatus = (String) current.get("status");

    if (!Objects.equals(goldenStatus, currentStatus)) {
      differences.add(
          String.format("Validation status: golden=%s, current=%s", goldenStatus, currentStatus));
      return false;
    }

    return true;
  }

  private boolean compareSimulationResults(
      List<Map<String, Object>> golden,
      List<Map<String, Object>> current,
      List<String> differences) {
    if (golden == null && current == null) return true;
    if (golden == null || current == null) {
      differences.add("SimulationTrace: null mismatch");
      return false;
    }

    // Compare trace length (allowing some variance for non-deterministic simulations)
    int goldenLength = golden.size();
    int currentLength = current.size();

    if (Math.abs(goldenLength - currentLength) > 2) { // Allow 2-step variance
      differences.add(
          String.format(
              "Simulation trace length: golden=%d, current=%d", goldenLength, currentLength));
      return false;
    }

    return true;
  }

  private String getHostname() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      return "unknown";
    }
  }

  // Data classes

  public static class GoldenOutput {
    public String scenario;
    public String version;
    public String timestamp;
    public String description;
    public Map<String, Object> parseResponse;
    public Map<String, Object> petriNet;
    public Map<String, Object> validationReport;
    public List<Map<String, Object>> simulationTrace;
    public Map<String, Object> dag;
    public Map<String, Object> metadata;

    // Default constructor for Jackson
    public GoldenOutput() {}
  }

  public static class ComparisonResult {
    public final boolean passed;
    public final String message;
    public final List<String> differences;

    public ComparisonResult(boolean passed, String message, List<String> differences) {
      this.passed = passed;
      this.message = message;
      this.differences = differences;
    }
  }
}
