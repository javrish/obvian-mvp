/* Copyright (c) 2025 Rishabh Pathak. Licensed under the MIT License. */

package core.petri.simulation;

import static org.assertj.core.api.Assertions.*;

import core.petri.Marking;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for TraceEvent
 *
 * Tests cover:
 * - Builder pattern functionality
 * - Backward compatibility constructors
 * - Factory methods
 * - JSON serialization support
 * - Timestamp handling
 * - Marking integration
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@DisplayName("TraceEvent Tests")
class TraceEventTest {

  @Nested
  @DisplayName("Builder Pattern Tests")
  class BuilderPatternTests {

    @Test
    @DisplayName("Should build complete trace event with all fields")
    void shouldBuildCompleteTraceEventWithAllFields() {
      // Given
      Instant timestamp = Instant.parse("2024-01-01T12:00:00Z");
      Marking markingBefore = new Marking("place1", 2);
      Marking markingAfter = new Marking("place2", 1);

      // When
      TraceEvent event =
          TraceEvent.builder()
              .type(TraceEvent.EventType.TRANSITION_FIRED)
              .timestamp(timestamp)
              .sequenceNumber(5)
              .transition("t1")
              .fromPlaces(Arrays.asList("place1", "place2"))
              .toPlaces(Arrays.asList("place3"))
              .tokenId("token-123")
              .simulationSeed(42L)
              .enabled(Arrays.asList("t1", "t2"))
              .markingBefore(markingBefore)
              .markingAfter(markingAfter)
              .simulationMode("deterministic")
              .reason("Transition fired successfully")
              .description("Test transition firing")
              .build();

      // Then
      assertThat(event.getType()).isEqualTo(TraceEvent.EventType.TRANSITION_FIRED);
      assertThat(event.getTimestamp()).isEqualTo(timestamp);
      assertThat(event.getSequenceNumber()).isEqualTo(5);
      assertThat(event.getTransition()).isEqualTo("t1");
      assertThat(event.getTransitionId()).isEqualTo("t1");
      assertThat(event.getFromPlaces()).containsExactly("place1", "place2");
      assertThat(event.getToPlaces()).containsExactly("place3");
      assertThat(event.getTokenId()).isEqualTo("token-123");
      assertThat(event.getSimulationSeed()).isEqualTo(42L);
      assertThat(event.getEnabled()).containsExactly("t1", "t2");
      assertThat(event.getMarkingBefore()).isEqualTo(markingBefore);
      assertThat(event.getMarkingAfter()).isEqualTo(markingAfter);
      assertThat(event.getSimulationMode()).isEqualTo("deterministic");
      assertThat(event.getReason()).isEqualTo("Transition fired successfully");
      assertThat(event.getDescription()).isEqualTo("Test transition firing");
    }

    @Test
    @DisplayName("Should use defaults for optional fields")
    void shouldUseDefaultsForOptionalFields() {
      // When
      TraceEvent event = TraceEvent.builder().transition("t1").build();

      // Then
      assertThat(event.getType()).isEqualTo(TraceEvent.EventType.TRANSITION_FIRED);
      assertThat(event.getTimestamp()).isNotNull();
      assertThat(event.getTransition()).isEqualTo("t1");
      assertThat(event.getTransitionId()).isEqualTo("t1");
      assertThat(event.getFromPlaces()).isEmpty();
      assertThat(event.getToPlaces()).isEmpty();
      assertThat(event.getEnabled()).isEmpty();
      assertThat(event.getMarking()).isEmpty();
    }

    @Test
    @DisplayName("Should handle transition ID mapping correctly")
    void shouldHandleTransitionIdMappingCorrectly() {
      // When using transition()
      TraceEvent event1 = TraceEvent.builder().transition("t1").build();

      // Then
      assertThat(event1.getTransition()).isEqualTo("t1");
      assertThat(event1.getTransitionId()).isEqualTo("t1");

      // When using transitionId()
      TraceEvent event2 = TraceEvent.builder().transitionId("t2").build();

      // Then
      assertThat(event2.getTransition()).isEqualTo("t2");
      assertThat(event2.getTransitionId()).isEqualTo("t2");

      // When using both (transitionId should take precedence for ID)
      TraceEvent event3 = TraceEvent.builder().transition("t3").transitionId("t3_id").build();

      // Then
      assertThat(event3.getTransition()).isEqualTo("t3");
      assertThat(event3.getTransitionId()).isEqualTo("t3_id");
    }
  }

  @Nested
  @DisplayName("Backward Compatibility Tests")
  class BackwardCompatibilityTests {

    @Test
    @DisplayName("Should support legacy constructor")
    void shouldSupportLegacyConstructor() {
      // Given
      Map<String, Integer> marking = new HashMap<>();
      marking.put("place1", 3);
      marking.put("place2", 1);

      // When
      TraceEvent event =
          new TraceEvent(
              TraceEvent.EventType.TRANSITION_FIRED,
              "t1",
              null,
              marking,
              "Legacy transition firing");

      // Then
      assertThat(event.getType()).isEqualTo(TraceEvent.EventType.TRANSITION_FIRED);
      assertThat(event.getTransitionId()).isEqualTo("t1");
      assertThat(event.getDescription()).isEqualTo("Legacy transition firing");
      assertThat(event.getMarking()).containsEntry("place1", 3);
      assertThat(event.getMarking()).containsEntry("place2", 1);
      assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should support factory methods")
    void shouldSupportFactoryMethods() {
      // Given
      Map<String, Integer> marking = new HashMap<>();
      marking.put("place1", 2);

      // When - transitionFired factory
      TraceEvent transitionEvent = TraceEvent.transitionFired("t1", marking);

      // Then
      assertThat(transitionEvent.getType()).isEqualTo(TraceEvent.EventType.TRANSITION_FIRED);
      assertThat(transitionEvent.getTransitionId()).isEqualTo("t1");
      assertThat(transitionEvent.getDescription()).isEqualTo("Transition t1 fired");
      assertThat(transitionEvent.getMarking()).containsEntry("place1", 2);

      // When - tokenAdded factory
      TraceEvent tokenEvent = TraceEvent.tokenAdded("place2", marking);

      // Then
      assertThat(tokenEvent.getType()).isEqualTo(TraceEvent.EventType.TOKEN_ADDED);
      assertThat(tokenEvent.getPlaceId()).isEqualTo("place2");
      assertThat(tokenEvent.getDescription()).isEqualTo("Token added to place place2");

      // When - simulationStarted factory
      TraceEvent startEvent = TraceEvent.simulationStarted(marking);

      // Then
      assertThat(startEvent.getType()).isEqualTo(TraceEvent.EventType.SIMULATION_STARTED);
      assertThat(startEvent.getDescription()).isEqualTo("Simulation started");

      // When - simulationCompleted factory
      TraceEvent endEvent = TraceEvent.simulationCompleted(marking);

      // Then
      assertThat(endEvent.getType()).isEqualTo(TraceEvent.EventType.SIMULATION_COMPLETED);
      assertThat(endEvent.getDescription()).isEqualTo("Simulation completed");
    }

    @Test
    @DisplayName("Should provide LocalDateTime backward compatibility")
    void shouldProvideLocalDateTimeBackwardCompatibility() {
      // When
      TraceEvent event = TraceEvent.builder().transition("t1").build();

      // Then
      LocalDateTime localTimestamp = event.getLocalTimestamp();
      assertThat(localTimestamp).isNotNull();

      // The LocalDateTime should be converted from the Instant
      Instant instant = event.getTimestamp();
      assertThat(localTimestamp)
          .isEqualTo(LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()));
    }
  }

  @Nested
  @DisplayName("Marking Integration Tests")
  class MarkingIntegrationTests {

    @Test
    @DisplayName("Should automatically extract marking from markingAfter")
    void shouldAutomaticallyExtractMarkingFromMarkingAfter() {
      // Given
      Marking markingAfter =
          Marking.builder().addTokens("place1", 3).addTokens("place2", 1).build();

      // When
      TraceEvent event = TraceEvent.builder().transition("t1").markingAfter(markingAfter).build();

      // Then
      assertThat(event.getMarkingAfter()).isEqualTo(markingAfter);
      assertThat(event.getMarking()).containsEntry("place1", 3);
      assertThat(event.getMarking()).containsEntry("place2", 1);
    }

    @Test
    @DisplayName("Should prefer explicit marking over markingAfter")
    void shouldPreferExplicitMarkingOverMarkingAfter() {
      // Given
      Marking markingAfter = new Marking("place1", 3);
      Map<String, Integer> explicitMarking = new HashMap<>();
      explicitMarking.put("place2", 5);

      // When
      TraceEvent event =
          TraceEvent.builder()
              .transition("t1")
              .markingAfter(markingAfter)
              .marking(explicitMarking)
              .build();

      // Then
      assertThat(event.getMarkingAfter()).isEqualTo(markingAfter);
      assertThat(event.getMarking()).containsEntry("place2", 5);
      assertThat(event.getMarking()).doesNotContainKey("place1");
    }
  }

  @Nested
  @DisplayName("JSON Constructor Tests")
  class JsonConstructorTests {

    @Test
    @DisplayName("Should handle null values gracefully in JSON constructor")
    void shouldHandleNullValuesGracefullyInJsonConstructor() {
      // When
      TraceEvent event =
          new TraceEvent(
              null, // type
              null, // timestamp
              null, // sequenceNumber
              null, // transition
              "t1", // transitionId
              null, // placeId
              null, // fromPlaces
              null, // toPlaces
              null, // tokenId
              null, // simulationSeed
              null, // enabled
              null, // markingBefore
              null, // markingAfter
              null, // marking
              null, // simulationMode
              null, // reason
              null // description
              );

      // Then - Should use defaults
      assertThat(event.getType()).isEqualTo(TraceEvent.EventType.TRANSITION_FIRED);
      assertThat(event.getTimestamp()).isNotNull();
      assertThat(event.getTransitionId()).isEqualTo("t1");
      assertThat(event.getFromPlaces()).isEmpty();
      assertThat(event.getToPlaces()).isEmpty();
      assertThat(event.getEnabled()).isEmpty();
      assertThat(event.getMarking()).isEmpty();
    }

    @Test
    @DisplayName("Should fallback description to reason")
    void shouldFallbackDescriptionToReason() {
      // When
      TraceEvent event =
          new TraceEvent(
              TraceEvent.EventType.TRANSITION_FIRED,
              Instant.now(),
              1,
              "t1",
              "t1",
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              "This is the reason",
              null // description is null
              );

      // Then
      assertThat(event.getReason()).isEqualTo("This is the reason");
      assertThat(event.getDescription()).isEqualTo("This is the reason");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("Should return defensive copies of mutable fields")
    void shouldReturnDefensiveCopiesOfMutableFields() {
      // Given
      TraceEvent event =
          TraceEvent.builder()
              .transition("t1")
              .fromPlaces(Arrays.asList("place1", "place2"))
              .toPlaces(Arrays.asList("place3"))
              .enabled(Arrays.asList("t1", "t2"))
              .build();

      // When & Then - Modifying returned lists should not affect the event
      event.getFromPlaces().add("place4");
      assertThat(event.getFromPlaces()).containsExactly("place1", "place2");

      event.getToPlaces().clear();
      assertThat(event.getToPlaces()).containsExactly("place3");

      event.getEnabled().add("t3");
      assertThat(event.getEnabled()).containsExactly("t1", "t2");

      event.getMarking().put("newPlace", 5);
      assertThat(event.getMarking()).isEmpty();
    }
  }

  @Nested
  @DisplayName("String Representation Tests")
  class StringRepresentationTests {

    @Test
    @DisplayName("Should provide meaningful toString")
    void shouldProvideMeaningfulToString() {
      // Given
      TraceEvent event =
          TraceEvent.builder()
              .type(TraceEvent.EventType.TRANSITION_FIRED)
              .sequenceNumber(3)
              .transition("t1")
              .description("Test transition")
              .build();

      // When
      String toString = event.toString();

      // Then
      assertThat(toString).contains("TraceEvent");
      assertThat(toString).contains("type=TRANSITION_FIRED");
      assertThat(toString).contains("seq=3");
      assertThat(toString).contains("transition='t1'");
      assertThat(toString).contains("desc='Test transition'");
    }

    @Test
    @DisplayName("Should handle minimal event in toString")
    void shouldHandleMinimalEventInToString() {
      // Given
      TraceEvent event = TraceEvent.builder().build();

      // When
      String toString = event.toString();

      // Then
      assertThat(toString).contains("TraceEvent");
      assertThat(toString).contains("type=TRANSITION_FIRED");
      // Should not contain null references or cause exceptions
    }

    @Test
    @DisplayName("Should handle place events in toString")
    void shouldHandlePlaceEventsInToString() {
      // Given
      TraceEvent event =
          TraceEvent.builder()
              .type(TraceEvent.EventType.TOKEN_ADDED)
              .placeId("place1")
              .description("Token added")
              .build();

      // When
      String toString = event.toString();

      // Then
      assertThat(toString).contains("type=TOKEN_ADDED");
      assertThat(toString).contains("place='place1'");
      assertThat(toString).contains("desc='Token added'");
    }
  }
}
