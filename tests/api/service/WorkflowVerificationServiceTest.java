package tests.api.service;

import static org.assertj.core.api.Assertions.*;

import api.service.WorkflowVerificationService;
import api.service.WorkflowVerificationService.WorkflowVerificationException;
import api.service.WorkflowVerificationService.WorkflowVerificationResult;
import core.petri.grammar.AutomationGrammar;
import core.petri.grammar.GitHubActionsParser;
import core.petri.validation.PetriNetValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end integration tests for workflow verification pipeline.
 *
 * <p>Tests the complete flow: YAML → PetriIntentSpec → PetriNet → Validation → Result
 */
class WorkflowVerificationServiceTest {

  private WorkflowVerificationService verificationService;

  @BeforeEach
  void setUp() {
    GitHubActionsParser parser = new GitHubActionsParser();
    AutomationGrammar automationGrammar = new AutomationGrammar();
    PetriNetValidator validator = new PetriNetValidator();

    verificationService =
        new WorkflowVerificationService(parser, automationGrammar, validator);
  }

  @Test
  @DisplayName("Should verify simple linear workflow successfully")
  void shouldVerifySimpleWorkflow() throws Exception {
    // Given: Simple workflow with linear dependencies
    String yaml =
        """
                name: Simple CI
                on: [push]
                jobs:
                  test:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm test
                  build:
                    runs-on: ubuntu-latest
                    needs: test
                    steps:
                      - run: npm run build
                  deploy:
                    runs-on: ubuntu-latest
                    needs: build
                    steps:
                      - run: npm run deploy
                """;

    // When: Verify workflow
    WorkflowVerificationResult result =
        verificationService.verifyWorkflow(yaml, ".github/workflows/ci.yml");

    // Then: Should pass verification
    assertThat(result).isNotNull();
    assertThat(result.isPassed()).isTrue();
    assertThat(result.getIntentSpec()).isNotNull();
    assertThat(result.getIntentSpec().getSteps()).hasSize(3);
    assertThat(result.getPetriNet()).isNotNull();
    assertThat(result.getValidationResult()).isNotNull();
    assertThat(result.getVerificationDurationMs()).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should verify workflow with parallel jobs")
  void shouldVerifyParallelWorkflow() throws Exception {
    // Given: Workflow with parallel matrix build
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
                    steps:
                      - run: npm test
                  deploy:
                    runs-on: ubuntu-latest
                    needs: test
                    steps:
                      - run: npm run deploy
                """;

    // When: Verify workflow
    WorkflowVerificationResult result =
        verificationService.verifyWorkflow(yaml, ".github/workflows/matrix.yml");

    // Then: Should pass verification
    assertThat(result.isPassed()).isTrue();
    assertThat(result.getIntentSpec().getSteps()).hasSize(2);
  }

  @Test
  @DisplayName("Should verify workflow with conditional execution")
  void shouldVerifyConditionalWorkflow() throws Exception {
    // Given: Workflow with conditional job
    String yaml =
        """
                name: Conditional Deploy
                on: [push]
                jobs:
                  test:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm test
                  deploy:
                    runs-on: ubuntu-latest
                    needs: test
                    if: github.ref == 'refs/heads/main'
                    steps:
                      - run: npm run deploy
                """;

    // When: Verify workflow
    WorkflowVerificationResult result =
        verificationService.verifyWorkflow(yaml, ".github/workflows/conditional.yml");

    // Then: Should pass verification
    assertThat(result.isPassed()).isTrue();
    assertThat(result.getIntentSpec().getSteps()).hasSize(2);
  }

  @Test
  @DisplayName("Should fail verification for workflow with missing dependency")
  void shouldFailForMissingDependency() {
    // Given: Workflow with missing dependency
    String yaml =
        """
                name: Missing Dependency
                on: [push]
                jobs:
                  deploy:
                    runs-on: ubuntu-latest
                    needs: build
                    steps:
                      - run: npm run deploy
                """;

    // When/Then: Should throw ParseException
    assertThatThrownBy(
            () ->
                verificationService.verifyWorkflow(
                    yaml, ".github/workflows/missing-dep.yml"))
        .isInstanceOf(WorkflowVerificationException.class)
        .hasMessageContaining("depends on 'build' which does not exist")
        .satisfies(
            e -> {
              WorkflowVerificationException wve = (WorkflowVerificationException) e;
              assertThat(wve.getFailureStage())
                  .isEqualTo(WorkflowVerificationException.FailureStage.PARSING);
            });
  }

  @Test
  @DisplayName("Should fail verification for circular dependency")
  void shouldFailForCircularDependency() {
    // Given: Workflow with circular dependency
    String yaml =
        """
                name: Circular Dependency
                on: [push]
                jobs:
                  job-a:
                    runs-on: ubuntu-latest
                    needs: job-c
                    steps:
                      - run: echo "A"
                  job-b:
                    runs-on: ubuntu-latest
                    needs: job-a
                    steps:
                      - run: echo "B"
                  job-c:
                    runs-on: ubuntu-latest
                    needs: job-b
                    steps:
                      - run: echo "C"
                """;

    // When/Then: Should throw ParseException
    assertThatThrownBy(
            () ->
                verificationService.verifyWorkflow(yaml, ".github/workflows/circular.yml"))
        .isInstanceOf(WorkflowVerificationException.class)
        .hasMessageContaining("Circular dependency detected");
  }

  @Test
  @DisplayName("Should handle malformed YAML gracefully")
  void shouldHandleMalformedYaml() {
    // Given: Invalid YAML syntax
    String yaml =
        """
                name: Malformed
                on: [push
                jobs:
                  test:
                    runs-on: ubuntu-latest
                """;

    // When/Then: Should throw ParseException
    assertThatThrownBy(
            () -> verificationService.verifyWorkflow(yaml, ".github/workflows/malformed.yml"))
        .isInstanceOf(WorkflowVerificationException.class)
        .satisfies(
            e -> {
              WorkflowVerificationException wve = (WorkflowVerificationException) e;
              assertThat(wve.getFailureStage())
                  .isEqualTo(WorkflowVerificationException.FailureStage.PARSING);
            });
  }

  @Test
  @DisplayName("Should verify real-world Next.js workflow")
  void shouldVerifyNextJsWorkflow() throws Exception {
    // Given: Real Next.js CI/CD workflow
    String yaml =
        """
                name: Next.js CI/CD
                on:
                  push:
                    branches: [main]
                  pull_request:
                    branches: [main]
                jobs:
                  lint:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@v3
                      - run: npm ci
                      - run: npm run lint
                  test:
                    runs-on: ubuntu-latest
                    needs: lint
                    steps:
                      - uses: actions/checkout@v3
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
                      - uses: actions/checkout@v3
                      - run: npm run deploy
                """;

    // When: Verify workflow
    WorkflowVerificationResult result =
        verificationService.verifyWorkflow(yaml, ".github/workflows/nextjs.yml");

    // Then: Should pass verification
    assertThat(result.isPassed()).isTrue();
    assertThat(result.getIntentSpec().getSteps()).hasSize(4);
    assertThat(result.getPetriNet().getPlaces()).isNotEmpty();
    assertThat(result.getPetriNet().getTransitions()).isNotEmpty();
  }

  @Test
  @DisplayName("Should include validation details in result")
  void shouldIncludeValidationDetails() throws Exception {
    // Given: Simple workflow
    String yaml =
        """
                name: Details Test
                on: [push]
                jobs:
                  test:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm test
                """;

    // When: Verify workflow
    WorkflowVerificationResult result =
        verificationService.verifyWorkflow(yaml, ".github/workflows/details.yml");

    // Then: Should include all pipeline artifacts
    assertThat(result.getWorkflowPath()).isEqualTo(".github/workflows/details.yml");
    assertThat(result.getIntentSpec().getName()).isEqualTo("Details Test");
    assertThat(result.getPetriNet().getName()).isEqualTo("Details Test");
    assertThat(result.getValidationResult().getPetriNetId()).isEqualTo(result.getPetriNet().getId());
  }
}
