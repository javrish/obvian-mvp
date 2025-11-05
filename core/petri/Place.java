package core.petri;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a place in a Petri net.
 * Places can hold tokens and have optional capacity constraints.
 * 
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Place {
    
    @JsonProperty("id")
    private final String id;
    
    @JsonProperty("name")
    private final String name;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("capacity")
    private final Integer capacity;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    @JsonCreator
    public Place(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("capacity") Integer capacity,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Place ID cannot be null or empty");
        }
        if (capacity != null && capacity <= 0) {
            throw new IllegalArgumentException("Place capacity must be positive if specified");
        }
        
        this.id = id.trim();
        this.name = name != null ? name.trim() : id;
        this.description = description;
        this.capacity = capacity;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    // Convenience constructors
    public Place(String id) {
        this(id, null, null, null, null);
    }
    
    public Place(String id, String name) {
        this(id, name, null, null, null);
    }
    
    public Place(String id, String name, Integer capacity) {
        this(id, name, null, capacity, null);
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getCapacity() { return capacity; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * Check if place is bounded (has capacity constraint)
     */
    public boolean isBounded() {
        return capacity != null;
    }
    
    /**
     * Check if place can accept additional tokens
     */
    public boolean canAcceptTokens(int currentTokens, int additionalTokens) {
        if (capacity == null) {
            return true; // Unbounded
        }
        return currentTokens + additionalTokens <= capacity;
    }
    
    /**
     * Get metadata value by key
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Check if this is a source place (typically has no input transitions)
     */
    public boolean isSource() {
        return Boolean.TRUE.equals(metadata.get("isSource"));
    }
    
    /**
     * Check if this is a sink place (typically has no output transitions)
     */
    public boolean isSink() {
        return Boolean.TRUE.equals(metadata.get("isSink"));
    }
    
    /**
     * Builder pattern for creating Place instances
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private String name;
        private String description;
        private Integer capacity;
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder(String id) {
            this.id = id;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder capacity(Integer capacity) {
            this.capacity = capacity;
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
        
        public Builder asSource() {
            this.metadata.put("isSource", true);
            return this;
        }
        
        public Builder asSink() {
            this.metadata.put("isSink", true);
            return this;
        }
        
        public Place build() {
            return new Place(id, name, description, capacity, metadata);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return Objects.equals(id, place.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Place{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                (capacity != null ? ", capacity=" + capacity : "") +
                '}';
    }
}