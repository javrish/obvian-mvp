/* Copyright (c) 2025 Rishabh Pathak. Licensed under the MIT License. */

package core.petri.simulation;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for SimulationConfig
 *
 * Tests cover:
 * - Builder pattern functionality
 * - Default configurations
 * - Validation of configuration parameters
 * - JSON serialization/deserialization
 * - Configuration factory methods
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@DisplayName("SimulationConfig Tests")
class SimulationConfigTest {

  @Nested
  @DisplayName("Builder Pattern Tests")
  class BuilderPatternTests {

    @Test
    @DisplayName("Should build config with all parameters")
    void shouldBuildConfigWithAllParameters() {
      // When
      SimulationConfig config =
          SimulationConfig.builder()
              .seed(123L)
              .mode(SimulationConfig.SimulationMode.INTERACTIVE)
              .maxSteps(500)
              .stepDelayMs(100)
              .enableTracing(true)
              .enableAnimation(true)
              .pauseOnDeadlock(false)
              .verbose(true)
              .build();

      // Then
      assertThat(config.getSeed()).isEqualTo(123L);
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.INTERACTIVE);
      assertThat(config.getMaxSteps()).isEqualTo(500);
      assertThat(config.getStepDelayMs()).isEqualTo(100);
      assertThat(config.isEnableTracing()).isTrue();
      assertThat(config.isEnableAnimation()).isTrue();
      assertThat(config.isPauseOnDeadlock()).isFalse();
      assertThat(config.isVerbose()).isTrue();
      assertThat(config.isInteractive()).isTrue();
      assertThat(config.isDeterministic()).isFalse();
    }

    @Test
    @DisplayName("Should use defaults when not specified")
    void shouldUseDefaultsWhenNotSpecified() {
      // When
      SimulationConfig config = SimulationConfig.builder().build();

      // Then
      assertThat(config.getSeed()).isNotNull();
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.DETERMINISTIC);
      assertThat(config.getMaxSteps()).isEqualTo(1000);
      assertThat(config.getStepDelayMs()).isEqualTo(0);
      assertThat(config.isEnableTracing()).isTrue();
      assertThat(config.isEnableAnimation()).isFalse();
      assertThat(config.isPauseOnDeadlock()).isTrue();
      assertThat(config.isVerbose()).isFalse();
      assertThat(config.isDeterministic()).isTrue();
      assertThat(config.isInteractive()).isFalse();
    }

    @Test
    @DisplayName("Should support deterministic mode shortcut")
    void shouldSupportDeterministicModeShortcut() {
      // When
      SimulationConfig config = SimulationConfig.builder().deterministic().seed(42L).build();

      // Then
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.DETERMINISTIC);
      assertThat(config.isDeterministic()).isTrue();
      assertThat(config.isInteractive()).isFalse();
      assertThat(config.getSeed()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Should support interactive mode shortcut")
    void shouldSupportInteractiveModeShortcut() {
      // When
      SimulationConfig config = SimulationConfig.builder().interactive().build();

      // Then
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.INTERACTIVE);
      assertThat(config.isInteractive()).isTrue();
      assertThat(config.isDeterministic()).isFalse();
      assertThat(config.isEnableAnimation()).isTrue();
      assertThat(config.isVerbose()).isTrue();
      assertThat(config.getStepDelayMs()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should create default deterministic config")
    void shouldCreateDefaultDeterministicConfig() {
      // When
      SimulationConfig config = SimulationConfig.defaultDeterministic();

      // Then
      assertThat(config.getSeed()).isEqualTo(42L);
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.DETERMINISTIC);
      assertThat(config.getMaxSteps()).isEqualTo(1000);
      assertThat(config.getStepDelayMs()).isEqualTo(0);
      assertThat(config.isEnableTracing()).isTrue();
      assertThat(config.isEnableAnimation()).isFalse();
      assertThat(config.isPauseOnDeadlock()).isTrue();
      assertThat(config.isVerbose()).isFalse();
    }

    @Test
    @DisplayName("Should create default interactive config")
    void shouldCreateDefaultInteractiveConfig() {
      // When
      SimulationConfig config = SimulationConfig.defaultInteractive();

      // Then
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.INTERACTIVE);
      assertThat(config.getMaxSteps()).isEqualTo(1000);
      assertThat(config.getStepDelayMs()).isEqualTo(100);
      assertThat(config.isEnableTracing()).isTrue();
      assertThat(config.isEnableAnimation()).isTrue();
      assertThat(config.isPauseOnDeadlock()).isTrue();
      assertThat(config.isVerbose()).isTrue();
    }

    @Test
    @DisplayName("Should create testing config with fixed seed")
    void shouldCreateTestingConfigWithFixedSeed() {
      // When
      SimulationConfig config = SimulationConfig.forTesting(999L);

      // Then
      assertThat(config.getSeed()).isEqualTo(999L);
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.DETERMINISTIC);
      assertThat(config.getMaxSteps()).isEqualTo(500);
      assertThat(config.getStepDelayMs()).isEqualTo(0);
      assertThat(config.isEnableTracing()).isTrue();
      assertThat(config.isEnableAnimation()).isFalse();
      assertThat(config.isPauseOnDeadlock()).isFalse();
      assertThat(config.isVerbose()).isFalse();
    }

    @Test
    @DisplayName("Should create fast batch config")
    void shouldCreateFastBatchConfig() {
      // When
      SimulationConfig config = SimulationConfig.fastBatch();

      // Then
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.DETERMINISTIC);
      assertThat(config.getMaxSteps()).isEqualTo(10000);
      assertThat(config.getStepDelayMs()).isEqualTo(0);
      assertThat(config.isEnableTracing()).isFalse();
      assertThat(config.isEnableAnimation()).isFalse();
      assertThat(config.isPauseOnDeadlock()).isFalse();
      assertThat(config.isVerbose()).isFalse();
    }
  }

  @Nested
  @DisplayName("JSON Constructor Tests")
  class JsonConstructorTests {

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
      // When
      SimulationConfig config =
          new SimulationConfig(null, null, null, null, null, null, null, null);

      // Then - Should use defaults
      assertThat(config.getSeed()).isNotNull();
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.DETERMINISTIC);
      assertThat(config.getMaxSteps()).isEqualTo(1000);
      assertThat(config.getStepDelayMs()).isEqualTo(0);
      assertThat(config.isEnableTracing()).isTrue();
      assertThat(config.isEnableAnimation()).isFalse();
      assertThat(config.isPauseOnDeadlock()).isTrue();
      assertThat(config.isVerbose()).isFalse();
    }

    @Test
    @DisplayName("Should validate and correct invalid values")
    void shouldValidateAndCorrectInvalidValues() {
      // When
      SimulationConfig config =
          new SimulationConfig(
              123L,
              SimulationConfig.SimulationMode.INTERACTIVE,
              -5, // Invalid maxSteps
              -100, // Invalid stepDelayMs
              true,
              false,
              true,
              false);

      // Then - Should correct invalid values
      assertThat(config.getMaxSteps()).isEqualTo(1000); // Corrected to default
      assertThat(config.getStepDelayMs()).isEqualTo(0); // Corrected to default
      assertThat(config.getSeed()).isEqualTo(123L);
      assertThat(config.getMode()).isEqualTo(SimulationConfig.SimulationMode.INTERACTIVE);
    }
  }

  @Nested
  @DisplayName("String Representation Tests")
  class StringRepresentationTests {

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void shouldProvideMeaningfulToStringRepresentation() {
      // Given
      SimulationConfig config =
          SimulationConfig.builder().seed(42L).maxSteps(500).verbose(true).build();

      // When
      String toString = config.toString();

      // Then
      assertThat(toString).contains("SimulationConfig");
      assertThat(toString).contains("seed=42");
      assertThat(toString).contains("maxSteps=500");
      assertThat(toString).contains("verbose=true");
      assertThat(toString).contains("mode=DETERMINISTIC");
    }
  }

  @Nested
  @DisplayName("Convenience Method Tests")
  class ConvenienceMethodTests {

    @Test
    @DisplayName("isDeterministic should return correct value")
    void isDeterministicShouldReturnCorrectValue() {
      SimulationConfig deterministicConfig =
          SimulationConfig.builder().mode(SimulationConfig.SimulationMode.DETERMINISTIC).build();

      SimulationConfig interactiveConfig =
          SimulationConfig.builder().mode(SimulationConfig.SimulationMode.INTERACTIVE).build();

      assertThat(deterministicConfig.isDeterministic()).isTrue();
      assertThat(interactiveConfig.isDeterministic()).isFalse();
    }

    @Test
    @DisplayName("isInteractive should return correct value")
    void isInteractiveShouldReturnCorrectValue() {
      SimulationConfig deterministicConfig =
          SimulationConfig.builder().mode(SimulationConfig.SimulationMode.DETERMINISTIC).build();

      SimulationConfig interactiveConfig =
          SimulationConfig.builder().mode(SimulationConfig.SimulationMode.INTERACTIVE).build();

      assertThat(deterministicConfig.isInteractive()).isFalse();
      assertThat(interactiveConfig.isInteractive()).isTrue();
    }
  }
}
