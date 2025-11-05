package core.petri;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;

/**
 * Represents a trace event during Petri net token simulation.
 * Contains information about transition firing, token movement, and simulation state.
 * 
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceEvent {
    
    @JsonProperty("timestamp")
    private final Instant timestamp;
    
    @JsonProperty("sequenceNumber")
    private final long sequenceNumber;
    
    @JsonProperty("transition")
    private final String transition;
    
    @JsonProperty("fromPlaces")
    private final List<String> fromPlaces;
    
    @JsonProperty("toPlaces")
    private final List<String> toPlaces;
    
    @JsonProperty("tokenId")
    private final String tokenId;
    
    @JsonProperty("simulationSeed")
    private final long simulationSeed;
    
    @JsonProperty("enabled")
    private final List<String> enabled;
    
    @JsonProperty("markingBefore")
    private final Marking markingBefore;
    
    @JsonProperty("markingAfter")
    private final Marking markingAfter;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    @JsonCreator
    public TraceEvent(
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sequenceNumber") long sequenceNumber,
            @JsonProperty("transition") String transition,
            @JsonProperty("fromPlaces") List<String> fromPlaces,
            @JsonProperty("toPlaces") List<String> toPlaces,
            @JsonProperty("tokenId") String tokenId,
            @JsonProperty("simulationSeed") long simulationSeed,
            @JsonProperty("enabled") List<String> enabled,
            @JsonProperty("markingBefore") Marking markingBefore,
            @JsonProperty("markingAfter") Marking markingAfter,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.sequenceNumber = sequenceNumber;
        this.transition = transition;
        this.fromPlaces = fromPlaces != null ? new ArrayList<>(fromPlaces) : new ArrayList<>();
        this.toPlaces = toPlaces != null ? new ArrayList<>(toPlaces) : new ArrayList<>();
        this.tokenId = tokenId;
        this.simulationSeed = simulationSeed;
        this.enabled = enabled != null ? new ArrayList<>(enabled) : new ArrayList<>();
        this.markingBefore = markingBefore;
        this.markingAfter = markingAfter;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    // Getters
    public Instant getTimestamp() { return timestamp; }
    public long getSequenceNumber() { return sequenceNumber; }
    public String getTransition() { return transition; }
    public List<String> getFromPlaces() { return new ArrayList<>(fromPlaces); }
    public List<String> getToPlaces() { return new ArrayList<>(toPlaces); }
    public String getTokenId() { return tokenId; }
    public long getSimulationSeed() { return simulationSeed; }
    public List<String> getEnabled() { return new ArrayList<>(enabled); }
    public Marking getMarkingBefore() { return markingBefore; }
    public Marking getMarkingAfter() { return markingAfter; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * Get metadata value by key
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Get simulation mode from metadata
     */
    public String getSimulationMode() {
        Object mode = metadata.get("simulationMode");
        return mode != null ? mode.toString() : "deterministic";
    }
    
    /**
     * Get reason for transition selection from metadata
     */
    public String getReason() {
        Object reason = metadata.get("reason");
        return reason != null ? reason.toString() : "Transition fired";
    }
    
    /**
     * Check if this was a deterministic choice
     */
    public boolean isDeterministic() {
        return "deterministic".equals(getSimulationMode());
    }
    
    /**
     * Check if this was an interactive choice
     */
    public boolean isInteractive() {
        return "interactive".equals(getSimulationMode());
    }
    
    /**
     * Get the number of tokens moved
     */
    public int getTokensMoved() {
        if (markingBefore == null || markingAfter == null) {
            return 0;
        }
        
        int moved = 0;
        Map<String, Integer> diff = markingAfter.difference(markingBefore);
        for (int change : diff.values()) {
            moved += Math.abs(change);
        }
        return moved / 2; // Divide by 2 because each token move creates both a removal and addition
    }
    
    /**
     * Get places that gained tokens
     */
    public List<String> getPlacesGainedTokens() {
        if (markingBefore == null || markingAfter == null) {
            return new ArrayList<>();
        }
        
        List<String> gained = new ArrayList<>();
        Map<String, Integer> diff = markingAfter.difference(markingBefore);
        for (Map.Entry<String, Integer> entry : diff.entrySet()) {
            if (entry.getValue() > 0) {
                gained.add(entry.getKey());
            }
        }
        return gained;
    }
    
    /**
     * Get places that lost tokens
     */
    public List<String> getPlacesLostTokens() {
        if (markingBefore == null || markingAfter == null) {
            return new ArrayList<>();
        }
        
        List<String> lost = new ArrayList<>();
        Map<String, Integer> diff = markingAfter.difference(markingBefore);
        for (Map.Entry<String, Integer> entry : diff.entrySet()) {
            if (entry.getValue() < 0) {
                lost.add(entry.getKey());
            }
        }
        return lost;
    }
    
    /**
     * Convert to ND-JSON format for export
     */
    public String toNdJson() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.findAndRegisterModules(); // For Java 8 time support
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{\"error\":\"Failed to serialize TraceEvent: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Builder pattern for creating TraceEvent instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Instant timestamp;
        private long sequenceNumber;
        private String transition;
        private List<String> fromPlaces = new ArrayList<>();
        private List<String> toPlaces = new ArrayList<>();
        private String tokenId;
        private long simulationSeed;
        private List<String> enabled = new ArrayList<>();
        private Marking markingBefore;
        private Marking markingAfter;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder sequenceNumber(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }
        
        public Builder transition(String transition) {
            this.transition = transition;
            return this;
        }
        
        public Builder fromPlaces(List<String> fromPlaces) {
            this.fromPlaces = new ArrayList<>(fromPlaces);
            return this;
        }
        
        public Builder addFromPlace(String place) {
            this.fromPlaces.add(place);
            return this;
        }
        
        public Builder toPlaces(List<String> toPlaces) {
            this.toPlaces = new ArrayList<>(toPlaces);
            return this;
        }
        
        public Builder addToPlace(String place) {
            this.toPlaces.add(place);
            return this;
        }
        
        public Builder tokenId(String tokenId) {
            this.tokenId = tokenId;
            return this;
        }
        
        public Builder simulationSeed(long seed) {
            this.simulationSeed = seed;
            return this;
        }
        
        public Builder enabled(List<String> enabled) {
            this.enabled = new ArrayList<>(enabled);
            return this;
        }
        
        public Builder markingBefore(Marking marking) {
            this.markingBefore = marking;
            return this;
        }
        
        public Builder markingAfter(Marking marking) {
            this.markingAfter = marking;
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }
        
        public Builder simulationMode(String mode) {
            this.metadata.put("simulationMode", mode);
            return this;
        }
        
        public Builder reason(String reason) {
            this.metadata.put("reason", reason);
            return this;
        }
        
        public TraceEvent build() {
            return new TraceEvent(timestamp, sequenceNumber, transition, fromPlaces, toPlaces,
                                tokenId, simulationSeed, enabled, markingBefore, markingAfter, metadata);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraceEvent that = (TraceEvent) o;
        return sequenceNumber == that.sequenceNumber &&
               simulationSeed == that.simulationSeed &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(transition, that.transition) &&
               Objects.equals(tokenId, that.tokenId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, sequenceNumber, transition, tokenId, simulationSeed);
    }
    
    @Override
    public String toString() {
        return "TraceEvent{" +
                "seq=" + sequenceNumber +
                ", transition='" + transition + '\'' +
                ", from=" + fromPlaces +
                ", to=" + toPlaces +
                ", tokenId='" + tokenId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}