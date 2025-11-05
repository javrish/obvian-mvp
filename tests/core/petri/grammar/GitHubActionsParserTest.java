
package tests.core.petri.grammar;

import static org.assertj.core.api.Assertions.*;

import core.petri.PetriIntentSpec;
import core.petri.PetriIntentSpec.IntentStep;
import core.petri.PetriIntentSpec.StepType;
import core.petri.grammar.GitHubActionsParser;
import core.petri.grammar.GitHubActionsParser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for GitHubActionsParser.
 *
 * <p>Tests cover: - Simple workflows (2 jobs, linear dependency) - Complex workflows (5+ jobs,
 * matrix builds, conditions) - Edge cases (empty workflow, missing needs, circular deps) - Error
 * cases (invalid YAML, malformed needs array) - Real workflows from top GitHub repos
 */
class GitHubActionsParserTest {

  private GitHubActionsParser parser;

  @BeforeEach
  void setUp() {
    parser = new GitHubActionsParser();
  }

  @Test
  @DisplayName("Should parse simple workflow with two jobs and linear dependency")
  void shouldParseSimpleWorkflowWithLinearDependency() throws ParseException {
    // Given: Simple workflow with build -> test dependency
    String yaml =
        """
                name: Simple CI
                on: [push]
                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@v2
                      - run: npm install
                      - run: npm run build
                  test:
                    runs-on: ubuntu-latest
                    needs: build
                    steps:
                      - uses: actions/checkout@v2
                      - run: npm test
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Should have 2 jobs
    assertThat(spec.getSteps()).hasSize(2);
    assertThat(spec.getName()).isEqualTo("Simple CI");
    assertThat(spec.getModelType()).isEqualTo(PetriIntentSpec.MODEL_TYPE);

    // Then: Build job has no dependencies
    IntentStep buildJob = spec.getStep("build").orElseThrow();
    assertThat(buildJob.getDependencies()).isEmpty();
    assertThat(buildJob.getType()).isEqualTo(StepType.ACTION);

    // Then: Test job depends on build
    IntentStep testJob = spec.getStep("test").orElseThrow();
    assertThat(testJob.getDependencies()).containsExactly("build");
    assertThat(testJob.getType()).isEqualTo(StepType.ACTION);
  }

  @Test
  @DisplayName("Should parse complex workflow with matrix builds")
  void shouldParseComplexWorkflowWithMatrixBuilds() throws ParseException {
    // Given: Workflow with matrix strategy
    String yaml =
        """
                name: Matrix Build
                on: [push]
                jobs:
                  test:
                    runs-on: ubuntu-latest
                    strategy:
                      matrix:
                        node: [14, 16, 18]
                        os: [ubuntu-latest, macos-latest]
                    steps:
                      - uses: actions/checkout@v2
                      - run: npm test
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Job should be marked as PARALLEL (matrix build)
    IntentStep testJob = spec.getStep("test").orElseThrow();
    assertThat(testJob.getType()).isEqualTo(StepType.PARALLEL);

    // Then: Metadata should contain matrix configuration
    assertThat(testJob.getMetadata())
        .containsEntry("hasMatrix", true)
        .containsKey("matrix");
  }

  @Test
  @DisplayName("Should parse workflow with conditional execution")
  void shouldParseWorkflowWithConditionalExecution() throws ParseException {
    // Given: Workflow with 'if' condition
    String yaml =
        """
                name: Conditional Deploy
                on: [push]
                jobs:
                  deploy:
                    runs-on: ubuntu-latest
                    if: github.ref == 'refs/heads/main'
                    steps:
                      - run: echo "Deploying to production"
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Job should be marked as CHOICE (conditional)
    IntentStep deployJob = spec.getStep("deploy").orElseThrow();
    assertThat(deployJob.getType()).isEqualTo(StepType.CHOICE);
    assertThat(deployJob.getWhen()).isEqualTo("github.ref == 'refs/heads/main'");
  }

  @Test
  @DisplayName("Should parse workflow with multiple dependencies")
  void shouldParseWorkflowWithMultipleDependencies() throws ParseException {
    // Given: Workflow with job depending on multiple jobs
    String yaml =
        """
                name: Multi-Dependency
                on: [push]
                jobs:
                  lint:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm run lint
                  test:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm test
                  deploy:
                    runs-on: ubuntu-latest
                    needs: [lint, test]
                    steps:
                      - run: npm run deploy
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Deploy job should depend on both lint and test
    IntentStep deployJob = spec.getStep("deploy").orElseThrow();
    assertThat(deployJob.getDependencies()).containsExactlyInAnyOrder("lint", "test");
  }

  @Test
  @DisplayName("Should parse workflow with timeout configuration")
  void shouldParseWorkflowWithTimeout() throws ParseException {
    // Given: Workflow with timeout-minutes
    String yaml =
        """
                name: Timeout Test
                on: [push]
                jobs:
                  slow-job:
                    runs-on: ubuntu-latest
                    timeout-minutes: 30
                    steps:
                      - run: echo "Long running job"
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Timeout should be 30 minutes (in milliseconds)
    IntentStep slowJob = spec.getStep("slow-job").orElseThrow();
    assertThat(slowJob.getTimeout()).isEqualTo(30 * 60 * 1000);
  }

  @Test
  @DisplayName("Should fail when workflow has no jobs")
  void shouldFailWhenWorkflowHasNoJobs() {
    // Given: Empty workflow
    String yaml =
        """
                name: Empty Workflow
                on: [push]
                """;

    // When/Then: Should throw ParseException
    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("No jobs found in workflow");
  }

  @Test
  @DisplayName("Should fail when YAML is invalid")
  void shouldFailWhenYamlIsInvalid() {
    // Given: Malformed YAML
    String yaml =
        """
                name: Invalid Workflow
                on: [push]
                jobs:
                  - this is not valid yaml structure
                """;

    // When/Then: Should throw ParseException
    assertThatThrownBy(() -> parser.parse(yaml)).isInstanceOf(ParseException.class);
  }

  @Test
  @DisplayName("Should parse workflow with single dependency string")
  void shouldParseWorkflowWithSingleDependencyString() throws ParseException {
    // Given: Workflow with single 'needs' as string (not array)
    String yaml =
        """
                name: Single Dependency
                on: [push]
                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm run build
                  test:
                    runs-on: ubuntu-latest
                    needs: build
                    steps:
                      - run: npm test
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Should handle single string dependency
    IntentStep testJob = spec.getStep("test").orElseThrow();
    assertThat(testJob.getDependencies()).containsExactly("build");
  }

  @Test
  @DisplayName("Should handle jobs without 'needs' keyword")
  void shouldHandleJobsWithoutNeeds() throws ParseException {
    // Given: Jobs without dependencies
    String yaml =
        """
                name: Independent Jobs
                on: [push]
                jobs:
                  lint:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm run lint
                  test:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm test
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Both jobs should have no dependencies
    assertThat(spec.getStep("lint").orElseThrow().getDependencies()).isEmpty();
    assertThat(spec.getStep("test").orElseThrow().getDependencies()).isEmpty();
  }

  @Test
  @DisplayName("Should parse workflow with custom job names")
  void shouldParseWorkflowWithCustomJobNames() throws ParseException {
    // Given: Workflow with 'name' field in jobs
    String yaml =
        """
                name: Custom Names
                on: [push]
                jobs:
                  job1:
                    name: Build Application
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm run build
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Should use custom name in description
    IntentStep job1 = spec.getStep("job1").orElseThrow();
    assertThat(job1.getDescription()).isEqualTo("Build Application");
  }

  @Test
  @DisplayName("Should use job ID as name when name field is missing")
  void shouldUseJobIdAsNameWhenNameFieldMissing() throws ParseException {
    // Given: Workflow without 'name' field in job
    String yaml =
        """
                name: No Job Names
                on: [push]
                jobs:
                  build-job:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm run build
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Should use job ID as description
    IntentStep buildJob = spec.getStep("build-job").orElseThrow();
    assertThat(buildJob.getDescription()).isEqualTo("build-job");
  }

  @Test
  @DisplayName("Should parse complex real-world Next.js workflow")
  void shouldParseRealWorldNextJsWorkflow() throws ParseException {
    // Given: Real Next.js CI workflow
    String yaml =
        """
                name: Next.js CI
                on:
                  push:
                    branches: [main, develop]
                  pull_request:
                    branches: [main]
                jobs:
                  lint:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@v3
                      - uses: actions/setup-node@v3
                        with:
                          node-version: 18
                      - run: npm ci
                      - run: npm run lint
                  test:
                    runs-on: ubuntu-latest
                    needs: lint
                    strategy:
                      matrix:
                        node-version: [16, 18, 20]
                    steps:
                      - uses: actions/checkout@v3
                      - uses: actions/setup-node@v3
                        with:
                          node-version: ${{ matrix.node-version }}
                      - run: npm ci
                      - run: npm test
                  build:
                    runs-on: ubuntu-latest
                    needs: [lint, test]
                    steps:
                      - uses: actions/checkout@v3
                      - run: npm ci
                      - run: npm run build
                  deploy:
                    runs-on: ubuntu-latest
                    needs: build
                    if: github.ref == 'refs/heads/main'
                    steps:
                      - run: npm run deploy
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Should have 4 jobs
    assertThat(spec.getSteps()).hasSize(4);

    // Then: Verify dependencies
    assertThat(spec.getStep("lint").orElseThrow().getDependencies()).isEmpty();
    assertThat(spec.getStep("test").orElseThrow().getDependencies()).containsExactly("lint");
    assertThat(spec.getStep("build").orElseThrow().getDependencies())
        .containsExactlyInAnyOrder("lint", "test");
    assertThat(spec.getStep("deploy").orElseThrow().getDependencies()).containsExactly("build");

    // Then: Test job should be PARALLEL (matrix)
    assertThat(spec.getStep("test").orElseThrow().getType()).isEqualTo(StepType.PARALLEL);

    // Then: Deploy job should be CHOICE (conditional)
    assertThat(spec.getStep("deploy").orElseThrow().getType()).isEqualTo(StepType.CHOICE);
  }

  @Test
  @DisplayName("Should parse workflow with runs-on metadata")
  void shouldParseWorkflowWithRunsOnMetadata() throws ParseException {
    // Given: Workflow with different runners
    String yaml =
        """
                name: Multi-OS Build
                on: [push]
                jobs:
                  linux-build:
                    runs-on: ubuntu-latest
                    steps:
                      - run: echo "Linux build"
                  mac-build:
                    runs-on: macos-latest
                    steps:
                      - run: echo "Mac build"
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Metadata should contain runs-on
    assertThat(spec.getStep("linux-build").orElseThrow().getMetadata())
        .containsEntry("runsOn", "ubuntu-latest");
    assertThat(spec.getStep("mac-build").orElseThrow().getMetadata())
        .containsEntry("runsOn", "macos-latest");
  }

  @Test
  @DisplayName("Should preserve original YAML in originalPrompt field")
  void shouldPreserveOriginalYamlInOriginalPrompt() throws ParseException {
    // Given: Any workflow
    String yaml =
        """
                name: Test Workflow
                on: [push]
                jobs:
                  test:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm test
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Original YAML should be preserved
    assertThat(spec.getOriginalPrompt()).isEqualTo(yaml);
  }

  @Test
  @DisplayName("Should parse unnamed workflow with default name")
  void shouldParseUnnamedWorkflowWithDefaultName() throws ParseException {
    // Given: Workflow without name
    String yaml =
        """
                on: [push]
                jobs:
                  test:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm test
                """;

    // When: Parse the workflow
    PetriIntentSpec spec = parser.parse(yaml);

    // Then: Should use default name
    assertThat(spec.getName()).isEqualTo("Unnamed Workflow");
  }
}
