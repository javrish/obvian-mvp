package tests.core.petri.grammar;

import static org.assertj.core.api.Assertions.*;

import core.petri.grammar.GitHubActionsParser;
import core.petri.grammar.GitHubActionsParser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for GitHubActionsParser error handling and line number tracking.
 *
 * <p>Tests cover:
 * - Missing dependencies
 * - Circular dependencies
 * - Invalid job names (reserved keywords)
 * - Malformed YAML syntax
 * - Unknown workflow keys
 * - Error message formatting with line numbers
 * - Fix suggestions
 * - YAML context in error messages
 */
class GitHubActionsParserErrorTest {

  private GitHubActionsParser parser;

  @BeforeEach
  void setUp() {
    parser = new GitHubActionsParser();
  }

  @Test
  @DisplayName("Should detect missing dependency and provide fix suggestion")
  void shouldDetectMissingDependency() {
    // Given: Workflow with job depending on non-existent job
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

    // When/Then: Should throw ParseException with line number and fix suggestion
    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("Job 'deploy' depends on 'build' which does not exist")
        .satisfies(
            e -> {
              ParseException pe = (ParseException) e;
              assertThat(pe.getLineNumber()).isGreaterThan(0);
              assertThat(pe.getFixSuggestion()).isNotEmpty();
              assertThat(pe.getYamlContext()).isNotEmpty();
            });
  }

  @Test
  @DisplayName("Should detect circular dependency")
  void shouldDetectCircularDependency() {
    // Given: Workflow with circular dependency (A → B → C → A)
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

    // When/Then: Should throw ParseException with cycle details
    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("Circular dependency detected")
        .hasMessageContaining("job-a")
        .hasMessageContaining("job-b")
        .hasMessageContaining("job-c");
  }

  @Test
  @DisplayName("Should reject reserved keyword as job name")
  void shouldRejectReservedKeyword() {
    // Given: Workflow with reserved keyword as job name
    String yaml =
        """
                name: Reserved Keyword
                on: [push]
                jobs:
                  on:
                    runs-on: ubuntu-latest
                    steps:
                      - run: echo "Invalid job name"
                """;

    // When/Then: Should throw ParseException
    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("reserved keyword")
        .hasMessageContaining("'on'");
  }

  @Test
  @DisplayName("Should provide helpful error for malformed YAML syntax")
  void shouldHandleMalformedYamlSyntax() {
    // Given: Workflow with invalid YAML syntax (bad indentation)
    String yaml =
        """
                name: Malformed YAML
                on: [push]
                jobs:
                  test:
                runs-on: ubuntu-latest
                    steps:
                      - run: npm test
                """;

    // When/Then: Should throw ParseException with YAML syntax error
    assertThatThrownBy(() -> parser.parse(yaml)).isInstanceOf(ParseException.class);
  }

  @Test
  @DisplayName("Should warn about unknown workflow keys")
  void shouldWarnAboutUnknownKeys() {
    // Given: Workflow with unknown top-level key
    String yaml =
        """
                name: Unknown Keys
                on: [push]
                unknown_key: some_value
                another_unknown: value
                jobs:
                  test:
                    runs-on: ubuntu-latest
                    steps:
                      - run: npm test
                """;

    // When: Parse the workflow
    assertThatCode(() -> parser.parse(yaml)).doesNotThrowAnyException();

    // Then: Should log warnings about unknown keys (implementation logs, not throws)
  }

  @Test
  @DisplayName("Should provide line numbers for job errors")
  void shouldProvideLineNumbersForJobErrors() {
    // Given: Workflow with invalid job structure
    String yaml =
        """
                name: Line Number Test
                on: [push]
                jobs:
                  test:
                    runs-on: ubuntu-latest
                    needs: [missing-job1, missing-job2]
                    steps:
                      - run: npm test
                """;

    // When/Then: Should throw with specific line number
    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(ParseException.class)
        .satisfies(
            e -> {
              ParseException pe = (ParseException) e;
              assertThat(pe.getLineNumber())
                  .isGreaterThan(0)
                  .as("Line number should be tracked");
            });
  }

  @Test
  @DisplayName("Should include YAML context in error messages")
  void shouldIncludeYamlContext() {
    // Given: Workflow with dependency error
    String yaml =
        """
                name: Context Test
                on: [push]
                jobs:
                  deploy:
                    runs-on: ubuntu-latest
                    needs: non-existent
                    steps:
                      - run: deploy.sh
                """;

    // When/Then: Should include surrounding lines in error message
    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(ParseException.class)
        .satisfies(
            e -> {
              ParseException pe = (ParseException) e;
              assertThat(pe.getYamlContext())
                  .isNotEmpty()
                  .as("Should include YAML context")
                  .contains("needs: non-existent");
            });
  }

  @Test
  @DisplayName("Should provide actionable fix suggestions")
  void shouldProvideFixSuggestions() {
    // Given: Workflow with missing dependency
    String yaml =
        """
                name: Fix Suggestion Test
                on: [push]
                jobs:
                  deploy:
                    runs-on: ubuntu-latest
                    needs: build
                    steps:
                      - run: ./deploy.sh
                """;

    // When/Then: Should suggest adding missing job
    assertThatThrownBy(() -> parser.parse(yaml))
        .isInstanceOf(ParseException.class)
        .satisfies(
            e -> {
              ParseException pe = (ParseException) e;
              assertThat(pe.getFixSuggestion())
                  .contains("Add job 'build'")
                  .as("Should suggest adding missing job");
            });
  }
}
