package core.petri.simulation;

import core.petri.Marking;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Represents a single event in a Petri net simulation trace.
 * Enhanced to support comprehensive token simulation logging and diagnostics.
 *
 * Features:
 * - Detailed transition firing events with before/after markings
 * - Token movement tracking
 * - Simulation metadata and execution context
 * - Deterministic trace reproduction capabilities
 *
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceEvent {

    public enum EventType {
        TRANSITION_FIRED,
        TOKEN_ADDED,
        TOKEN_REMOVED,
        SIMULATION_STARTED,
        SIMULATION_COMPLETED,
        SIMULATION_FAILED
    }

    @JsonProperty("type")
    private final EventType type;

    @JsonProperty("timestamp")
    private final Instant timestamp;

    @JsonProperty("sequenceNumber")
    private final Integer sequenceNumber;

    @JsonProperty("transition")
    private final String transition;

    @JsonProperty("transitionId")
    private final String transitionId;

    @JsonProperty("placeId")
    private final String placeId;

    @JsonProperty("fromPlaces")
    private final List<String> fromPlaces;

    @JsonProperty("toPlaces")
    private final List<String> toPlaces;

    @JsonProperty("tokenId")
    private final String tokenId;

    @JsonProperty("simulationSeed")
    private final Long simulationSeed;

    @JsonProperty("enabled")
    private final List<String> enabled;

    @JsonProperty("markingBefore")
    private final Marking markingBefore;

    @JsonProperty("markingAfter")
    private final Marking markingAfter;

    @JsonProperty("marking")
    private final Map<String, Integer> marking;

    @JsonProperty("simulationMode")
    private final String simulationMode;

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("description")
    private final String description;

    @JsonCreator
    public TraceEvent(
            @JsonProperty("type") EventType type,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sequenceNumber") Integer sequenceNumber,
            @JsonProperty("transition") String transition,
            @JsonProperty("transitionId") String transitionId,
            @JsonProperty("placeId") String placeId,
            @JsonProperty("fromPlaces") List<String> fromPlaces,
            @JsonProperty("toPlaces") List<String> toPlaces,
            @JsonProperty("tokenId") String tokenId,
            @JsonProperty("simulationSeed") Long simulationSeed,
            @JsonProperty("enabled") List<String> enabled,
            @JsonProperty("markingBefore") Marking markingBefore,
            @JsonProperty("markingAfter") Marking markingAfter,
            @JsonProperty("marking") Map<String, Integer> marking,
            @JsonProperty("simulationMode") String simulationMode,
            @JsonProperty("reason") String reason,
            @JsonProperty("description") String description) {

        this.type = type != null ? type : EventType.TRANSITION_FIRED;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.sequenceNumber = sequenceNumber;
        this.transition = transition;
        this.transitionId = transitionId != null ? transitionId : transition;
        this.placeId = placeId;
        this.fromPlaces = fromPlaces != null ? new ArrayList<>(fromPlaces) : new ArrayList<>();
        this.toPlaces = toPlaces != null ? new ArrayList<>(toPlaces) : new ArrayList<>();
        this.tokenId = tokenId;
        this.simulationSeed = simulationSeed;
        this.enabled = enabled != null ? new ArrayList<>(enabled) : new ArrayList<>();
        this.markingBefore = markingBefore;
        this.markingAfter = markingAfter;
        this.marking = marking != null ? new HashMap<>(marking) :
                      (markingAfter != null ? markingAfter.getTokens() : new HashMap<>());
        this.simulationMode = simulationMode;
        this.reason = reason;
        this.description = description != null ? description : reason;
    }

    // Backward compatibility constructor
    public TraceEvent(EventType type, String transitionId, String placeId, Map<String, Integer> marking, String description) {
        this(type, Instant.now(), null, transitionId, transitionId, placeId, null, null, null, null, null,
             null, null, marking, null, null, description);
    }

    // Factory methods for backward compatibility
    public static TraceEvent transitionFired(String transitionId, Map<String, Integer> marking) {
        return new TraceEvent(EventType.TRANSITION_FIRED, transitionId, null, marking,
            "Transition " + transitionId + " fired");
    }

    public static TraceEvent tokenAdded(String placeId, Map<String, Integer> marking) {
        return new TraceEvent(EventType.TOKEN_ADDED, null, placeId, marking,
            "Token added to place " + placeId);
    }

    public static TraceEvent simulationStarted(Map<String, Integer> initialMarking) {
        return new TraceEvent(EventType.SIMULATION_STARTED, null, null, initialMarking,
            "Simulation started");
    }

    public static TraceEvent simulationCompleted(Map<String, Integer> finalMarking) {
        return new TraceEvent(EventType.SIMULATION_COMPLETED, null, null, finalMarking,
            "Simulation completed");
    }

    // Getters
    public EventType getType() { return type; }

    public Instant getTimestamp() { return timestamp; }

    /**
     * Get timestamp as LocalDateTime for backward compatibility
     */
    public LocalDateTime getLocalTimestamp() {
        return LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
    }

    public Integer getSequenceNumber() { return sequenceNumber; }
    public String getTransition() { return transition; }
    public String getTransitionId() { return transitionId != null ? transitionId : transition; }
    public String getPlaceId() { return placeId; }
    public List<String> getFromPlaces() { return new ArrayList<>(fromPlaces); }
    public List<String> getToPlaces() { return new ArrayList<>(toPlaces); }
    public String getTokenId() { return tokenId; }
    public Long getSimulationSeed() { return simulationSeed; }
    public List<String> getEnabled() { return new ArrayList<>(enabled); }
    public Marking getMarkingBefore() { return markingBefore; }
    public Marking getMarkingAfter() { return markingAfter; }
    public Map<String, Integer> getMarking() { return new HashMap<>(marking); }
    public String getSimulationMode() { return simulationMode; }
    public String getReason() { return reason; }
    public String getDescription() { return description; }

    /**
     * Builder pattern for creating TraceEvent instances
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EventType type = EventType.TRANSITION_FIRED;
        private Instant timestamp = Instant.now();
        private Integer sequenceNumber;
        private String transition;
        private String transitionId;
        private String placeId;
        private List<String> fromPlaces = new ArrayList<>();
        private List<String> toPlaces = new ArrayList<>();
        private String tokenId;
        private Long simulationSeed;
        private List<String> enabled = new ArrayList<>();
        private Marking markingBefore;
        private Marking markingAfter;
        private Map<String, Integer> marking = new HashMap<>();
        private String simulationMode;
        private String reason;
        private String description;

        public Builder type(EventType type) {
            this.type = type;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder sequenceNumber(Integer sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public Builder transition(String transition) {
            this.transition = transition;
            this.transitionId = transition;
            return this;
        }

        public Builder transitionId(String transitionId) {
            this.transitionId = transitionId;
            if (this.transition == null) {
                this.transition = transitionId;
            }
            return this;
        }

        public Builder placeId(String placeId) {
            this.placeId = placeId;
            return this;
        }

        public Builder fromPlaces(List<String> fromPlaces) {
            this.fromPlaces = fromPlaces != null ? new ArrayList<>(fromPlaces) : new ArrayList<>();
            return this;
        }

        public Builder toPlaces(List<String> toPlaces) {
            this.toPlaces = toPlaces != null ? new ArrayList<>(toPlaces) : new ArrayList<>();
            return this;
        }

        public Builder tokenId(String tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        public Builder simulationSeed(Long simulationSeed) {
            this.simulationSeed = simulationSeed;
            return this;
        }

        public Builder enabled(List<String> enabled) {
            this.enabled = enabled != null ? new ArrayList<>(enabled) : new ArrayList<>();
            return this;
        }

        public Builder markingBefore(Marking markingBefore) {
            this.markingBefore = markingBefore;
            return this;
        }

        public Builder markingAfter(Marking markingAfter) {
            this.markingAfter = markingAfter;
            return this;
        }

        public Builder marking(Map<String, Integer> marking) {
            this.marking = marking != null ? new HashMap<>(marking) : new HashMap<>();
            return this;
        }

        public Builder simulationMode(String simulationMode) {
            this.simulationMode = simulationMode;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public TraceEvent build() {
            return new TraceEvent(type, timestamp, sequenceNumber, transition, transitionId, placeId,
                                fromPlaces, toPlaces, tokenId, simulationSeed, enabled, markingBefore,
                                markingAfter, marking, simulationMode, reason, description);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TraceEvent{");
        sb.append("type=").append(type);
        if (sequenceNumber != null) {
            sb.append(", seq=").append(sequenceNumber);
        }
        if (transition != null) {
            sb.append(", transition='").append(transition).append('\'');
        }
        if (placeId != null) {
            sb.append(", place='").append(placeId).append('\'');
        }
        if (description != null) {
            sb.append(", desc='").append(description).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}