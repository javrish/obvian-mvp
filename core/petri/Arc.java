package core.petri;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an arc in a Petri net connecting places to transitions or vice versa.
 * Arcs have weights that determine how many tokens are consumed or produced.
 * 
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Arc {
    
    @JsonProperty("from")
    private final String from;
    
    @JsonProperty("to")
    private final String to;
    
    @JsonProperty("weight")
    private final int weight;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    @JsonCreator
    public Arc(
            @JsonProperty("from") String from,
            @JsonProperty("to") String to,
            @JsonProperty("weight") int weight,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        if (from == null || from.trim().isEmpty()) {
            throw new IllegalArgumentException("Arc source cannot be null or empty");
        }
        if (to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("Arc target cannot be null or empty");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("Arc weight must be positive");
        }
        
        this.from = from.trim();
        this.to = to.trim();
        this.weight = weight;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    // Convenience constructors
    public Arc(String from, String to) {
        this(from, to, 1, null);
    }
    
    public Arc(String from, String to, int weight) {
        this(from, to, weight, null);
    }
    
    // Getters
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public int getWeight() { return weight; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * Get metadata value by key
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Check if this is an inhibitor arc (prevents firing when place has tokens)
     */
    public boolean isInhibitor() {
        return Boolean.TRUE.equals(metadata.get("isInhibitor"));
    }
    
    /**
     * Check if this is a test arc (doesn't consume tokens)
     */
    public boolean isTest() {
        return Boolean.TRUE.equals(metadata.get("isTest"));
    }
    
    /**
     * Get the arc type for visualization
     */
    public String getArcType() {
        if (isInhibitor()) return "inhibitor";
        if (isTest()) return "test";
        return "normal";
    }
    
    /**
     * Builder pattern for creating Arc instances
     */
    public static Builder builder(String from, String to) {
        return new Builder(from, to);
    }
    
    public static class Builder {
        private final String from;
        private final String to;
        private int weight = 1;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder(String from, String to) {
            this.from = from;
            this.to = to;
        }
        
        public Builder weight(int weight) {
            this.weight = weight;
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
        
        public Builder asInhibitor() {
            this.metadata.put("isInhibitor", true);
            return this;
        }
        
        public Builder asTest() {
            this.metadata.put("isTest", true);
            return this;
        }
        
        public Arc build() {
            return new Arc(from, to, weight, metadata);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arc arc = (Arc) o;
        return weight == arc.weight &&
               Objects.equals(from, arc.from) &&
               Objects.equals(to, arc.to);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(from, to, weight);
    }
    
    @Override
    public String toString() {
        return "Arc{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", weight=" + weight +
                '}';
    }
}