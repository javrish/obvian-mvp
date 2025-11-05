package core.petri;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a marking (token distribution) in a Petri net.
 * A marking defines how many tokens are in each place at a given time.
 * 
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Marking {
    
    @JsonProperty("tokens")
    private final Map<String, Integer> tokens;
    
    @JsonCreator
    public Marking(@JsonProperty("tokens") Map<String, Integer> tokens) {
        this.tokens = new HashMap<>();
        if (tokens != null) {
            // Only store places with positive token counts
            for (Map.Entry<String, Integer> entry : tokens.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    this.tokens.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }
    
    // Convenience constructors
    public Marking() {
        this(new HashMap<>());
    }
    
    public Marking(String placeId, int tokenCount) {
        this();
        if (tokenCount > 0) {
            this.tokens.put(placeId, tokenCount);
        }
    }
    
    // Getters
    public Map<String, Integer> getTokens() { 
        return new HashMap<>(tokens); 
    }
    
    /**
     * Get number of tokens in a specific place
     */
    public int getTokens(String placeId) {
        return tokens.getOrDefault(placeId, 0);
    }
    
    /**
     * Get all places that have tokens
     */
    public Set<String> getPlacesWithTokens() {
        return tokens.keySet();
    }
    
    /**
     * Get total number of tokens across all places
     */
    public int getTotalTokens() {
        return tokens.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Check if marking is empty (no tokens anywhere)
     */
    public boolean isEmpty() {
        return tokens.isEmpty();
    }
    
    /**
     * Check if a place has any tokens
     */
    public boolean hasTokens(String placeId) {
        return getTokens(placeId) > 0;
    }
    
    /**
     * Check if a place has at least the specified number of tokens
     */
    public boolean hasTokens(String placeId, int minTokens) {
        return getTokens(placeId) >= minTokens;
    }
    
    /**
     * Create a new marking with additional tokens in a place
     */
    public Marking addTokens(String placeId, int additionalTokens) {
        if (additionalTokens <= 0) {
            return this;
        }
        
        Map<String, Integer> newTokens = new HashMap<>(tokens);
        int currentTokens = newTokens.getOrDefault(placeId, 0);
        newTokens.put(placeId, currentTokens + additionalTokens);
        
        return new Marking(newTokens);
    }
    
    /**
     * Create a new marking with tokens removed from a place
     */
    public Marking removeTokens(String placeId, int tokensToRemove) {
        if (tokensToRemove <= 0) {
            return this;
        }
        
        Map<String, Integer> newTokens = new HashMap<>(tokens);
        int currentTokens = newTokens.getOrDefault(placeId, 0);
        int remainingTokens = Math.max(0, currentTokens - tokensToRemove);
        
        if (remainingTokens > 0) {
            newTokens.put(placeId, remainingTokens);
        } else {
            newTokens.remove(placeId);
        }
        
        return new Marking(newTokens);
    }
    
    /**
     * Create a new marking by setting exact token count for a place
     */
    public Marking setTokens(String placeId, int tokenCount) {
        Map<String, Integer> newTokens = new HashMap<>(tokens);
        
        if (tokenCount > 0) {
            newTokens.put(placeId, tokenCount);
        } else {
            newTokens.remove(placeId);
        }
        
        return new Marking(newTokens);
    }
    
    /**
     * Check if this marking is reachable from another marking
     * (simple check - this marking has <= tokens in each place)
     */
    public boolean isReachableFrom(Marking other) {
        for (Map.Entry<String, Integer> entry : tokens.entrySet()) {
            if (other.getTokens(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Calculate the difference between this marking and another
     * Returns a map of place -> token difference (positive = more tokens, negative = fewer)
     */
    public Map<String, Integer> difference(Marking other) {
        Map<String, Integer> diff = new HashMap<>();
        
        // Check all places in this marking
        for (Map.Entry<String, Integer> entry : tokens.entrySet()) {
            String placeId = entry.getKey();
            int thisTokens = entry.getValue();
            int otherTokens = other.getTokens(placeId);
            int difference = thisTokens - otherTokens;
            
            if (difference != 0) {
                diff.put(placeId, difference);
            }
        }
        
        // Check places only in other marking
        for (Map.Entry<String, Integer> entry : other.tokens.entrySet()) {
            String placeId = entry.getKey();
            if (!tokens.containsKey(placeId)) {
                diff.put(placeId, -entry.getValue());
            }
        }
        
        return diff;
    }
    
    /**
     * Builder pattern for creating Marking instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Map<String, Integer> tokens = new HashMap<>();
        
        public Builder addTokens(String placeId, int tokenCount) {
            if (tokenCount > 0) {
                tokens.put(placeId, tokens.getOrDefault(placeId, 0) + tokenCount);
            }
            return this;
        }
        
        public Builder setTokens(String placeId, int tokenCount) {
            if (tokenCount > 0) {
                tokens.put(placeId, tokenCount);
            } else {
                tokens.remove(placeId);
            }
            return this;
        }
        
        public Builder tokens(Map<String, Integer> tokens) {
            this.tokens = new HashMap<>(tokens);
            return this;
        }
        
        public Marking build() {
            return new Marking(tokens);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Marking marking = (Marking) o;
        return Objects.equals(tokens, marking.tokens);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tokens);
    }
    
    @Override
    public String toString() {
        if (tokens.isEmpty()) {
            return "Marking{}";
        }
        
        StringBuilder sb = new StringBuilder("Marking{");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : tokens.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}