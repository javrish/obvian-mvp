package core.petri;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Petri net with places, transitions, arcs, and initial marking.
 * Extends/complements the existing DAG system for formal workflow validation.
 * 
 * This class implements the core Petri net data structure with stable ID generation
 * using SHA-1 hash of normalized JSON representation.
 * 
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetriNet {
    
    @JsonProperty("id")
    private final String id;
    
    @JsonProperty("name")
    private final String name;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("places")
    private final List<Place> places;
    
    @JsonProperty("transitions")
    private final List<Transition> transitions;
    
    @JsonProperty("arcs")
    private final List<Arc> arcs;
    
    @JsonProperty("initialMarking")
    private final Marking initialMarking;
    
    @JsonProperty("schemaVersion")
    private final String schemaVersion;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    @JsonProperty("derivedFromDagId")
    private final String derivedFromDagId;
    
    @JsonCreator
    public PetriNet(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("places") List<Place> places,
            @JsonProperty("transitions") List<Transition> transitions,
            @JsonProperty("arcs") List<Arc> arcs,
            @JsonProperty("initialMarking") Marking initialMarking,
            @JsonProperty("schemaVersion") String schemaVersion,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("derivedFromDagId") String derivedFromDagId) {
        this.places = places != null ? new ArrayList<>(places) : new ArrayList<>();
        this.transitions = transitions != null ? new ArrayList<>(transitions) : new ArrayList<>();
        this.arcs = arcs != null ? new ArrayList<>(arcs) : new ArrayList<>();
        this.initialMarking = initialMarking != null ? initialMarking : new Marking();
        this.name = name != null ? name : "Unnamed Petri Net";
        this.description = description;
        this.schemaVersion = schemaVersion != null ? schemaVersion : "1.0";
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.derivedFromDagId = derivedFromDagId;
        
        // Generate stable ID if not provided
        this.id = id != null ? id : generateStableId();
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<Place> getPlaces() { return new ArrayList<>(places); }
    public List<Transition> getTransitions() { return new ArrayList<>(transitions); }
    public List<Arc> getArcs() { return new ArrayList<>(arcs); }
    public Marking getInitialMarking() { return initialMarking; }
    public String getSchemaVersion() { return schemaVersion; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public String getDerivedFromDagId() { return derivedFromDagId; }
    
    /**
     * Get place by ID
     */
    public Optional<Place> getPlace(String placeId) {
        return places.stream()
                .filter(p -> p.getId().equals(placeId))
                .findFirst();
    }
    
    /**
     * Get transition by ID
     */
    public Optional<Transition> getTransition(String transitionId) {
        return transitions.stream()
                .filter(t -> t.getId().equals(transitionId))
                .findFirst();
    }
    
    /**
     * Get input places for a transition
     */
    public List<Place> getInputPlaces(String transitionId) {
        return arcs.stream()
                .filter(arc -> arc.getTo().equals(transitionId))
                .map(arc -> getPlace(arc.getFrom()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    /**
     * Get output places for a transition
     */
    public List<Place> getOutputPlaces(String transitionId) {
        return arcs.stream()
                .filter(arc -> arc.getFrom().equals(transitionId))
                .map(arc -> getPlace(arc.getTo()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    /**
     * Get input transitions for a place
     */
    public List<Transition> getInputTransitions(String placeId) {
        return arcs.stream()
                .filter(arc -> arc.getTo().equals(placeId))
                .map(arc -> getTransition(arc.getFrom()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    /**
     * Get output transitions for a place
     */
    public List<Transition> getOutputTransitions(String placeId) {
        return arcs.stream()
                .filter(arc -> arc.getFrom().equals(placeId))
                .map(arc -> getTransition(arc.getTo()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    /**
     * Get arc weight between two nodes
     */
    public int getArcWeight(String fromId, String toId) {
        return arcs.stream()
                .filter(arc -> arc.getFrom().equals(fromId) && arc.getTo().equals(toId))
                .findFirst()
                .map(Arc::getWeight)
                .orElse(0);
    }
    
    /**
     * Check if transition is enabled in given marking
     * Enable Rule: M(p) ≥ w(p→t) for all inputs; capacity check on outputs if cap(q) present
     */
    public boolean isEnabled(String transitionId, Marking marking) {
        List<Place> inputPlaces = getInputPlaces(transitionId);
        List<Place> outputPlaces = getOutputPlaces(transitionId);
        
        // Check input places have sufficient tokens
        for (Place place : inputPlaces) {
            int required = getArcWeight(place.getId(), transitionId);
            int available = marking.getTokens(place.getId());
            if (available < required) {
                return false;
            }
        }
        
        // Check output places have capacity
        for (Place place : outputPlaces) {
            if (place.getCapacity() != null) {
                int produced = getArcWeight(transitionId, place.getId());
                int current = marking.getTokens(place.getId());
                if (current + produced > place.getCapacity()) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Get all enabled transitions in given marking
     */
    public List<Transition> getEnabledTransitions(Marking marking) {
        return transitions.stream()
                .filter(t -> isEnabled(t.getId(), marking))
                .collect(Collectors.toList());
    }
    
    /**
     * Fire a transition and return new marking
     * Firing Effect: M' = M − Σ_in w + Σ_out w
     */
    public Marking fireTransition(String transitionId, Marking marking) {
        if (!isEnabled(transitionId, marking)) {
            throw new IllegalStateException("Transition " + transitionId + " is not enabled");
        }
        
        Map<String, Integer> newTokens = new HashMap<>(marking.getTokens());
        
        // Remove tokens from input places
        for (Place place : getInputPlaces(transitionId)) {
            int weight = getArcWeight(place.getId(), transitionId);
            int current = newTokens.getOrDefault(place.getId(), 0);
            newTokens.put(place.getId(), current - weight);
        }
        
        // Add tokens to output places
        for (Place place : getOutputPlaces(transitionId)) {
            int weight = getArcWeight(transitionId, place.getId());
            int current = newTokens.getOrDefault(place.getId(), 0);
            newTokens.put(place.getId(), current + weight);
        }
        
        return new Marking(newTokens);
    }
    
    /**
     * Check if marking is terminal (tokens in final places and no enabled transitions)
     */
    public boolean isTerminal(Marking marking) {
        // Check if any transitions are enabled
        if (!getEnabledTransitions(marking).isEmpty()) {
            return false;
        }
        
        // Check if tokens are in final places (places with no output transitions)
        for (Place place : places) {
            if (marking.getTokens(place.getId()) > 0) {
                List<Transition> outputs = getOutputTransitions(place.getId());
                if (!outputs.isEmpty()) {
                    // Tokens in non-final place with no enabled transitions = deadlock
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Generate stable netId using SHA-1 hash of normalized PetriNet JSON
     */
    @JsonIgnore
    private String generateStableId() {
        try {
            // Create normalized representation for hashing
            Map<String, Object> normalized = new TreeMap<>();
            normalized.put("name", name);
            normalized.put("places", places.stream()
                    .sorted(Comparator.comparing(Place::getId))
                    .collect(Collectors.toList()));
            normalized.put("transitions", transitions.stream()
                    .sorted(Comparator.comparing(Transition::getId))
                    .collect(Collectors.toList()));
            normalized.put("arcs", arcs.stream()
                    .sorted(Comparator.comparing(Arc::getFrom).thenComparing(Arc::getTo))
                    .collect(Collectors.toList()));
            normalized.put("initialMarking", initialMarking);
            
            // Convert to JSON string
            String jsonString = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(normalized);
            
            // Generate SHA-1 hash
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(jsonString.getBytes("UTF-8"));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return "petri_" + hexString.toString().substring(0, 12);
            
        } catch (Exception e) {
            // Fallback to timestamp-based ID
            return "petri_" + System.currentTimeMillis();
        }
    }
    
    /**
     * Validate Petri net structure
     */
    @JsonIgnore
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        // Check for empty places or transitions
        if (places.isEmpty()) {
            errors.add("Petri net must have at least one place");
        }
        if (transitions.isEmpty()) {
            errors.add("Petri net must have at least one transition");
        }
        
        // Check for duplicate IDs
        Set<String> placeIds = new HashSet<>();
        for (Place place : places) {
            if (!placeIds.add(place.getId())) {
                errors.add("Duplicate place ID: " + place.getId());
            }
        }
        
        Set<String> transitionIds = new HashSet<>();
        for (Transition transition : transitions) {
            if (!transitionIds.add(transition.getId())) {
                errors.add("Duplicate transition ID: " + transition.getId());
            }
        }
        
        // Check arc references
        Set<String> allIds = new HashSet<>();
        allIds.addAll(placeIds);
        allIds.addAll(transitionIds);
        
        for (Arc arc : arcs) {
            if (!allIds.contains(arc.getFrom())) {
                errors.add("Arc references unknown source: " + arc.getFrom());
            }
            if (!allIds.contains(arc.getTo())) {
                errors.add("Arc references unknown target: " + arc.getTo());
            }
            
            // Check that arcs connect places to transitions or vice versa
            boolean fromIsPlace = placeIds.contains(arc.getFrom());
            boolean toIsPlace = placeIds.contains(arc.getTo());
            
            if (fromIsPlace == toIsPlace) {
                errors.add("Arc must connect place to transition or transition to place: " 
                          + arc.getFrom() + " -> " + arc.getTo());
            }
        }
        
        // Check initial marking references valid places
        for (String placeId : initialMarking.getTokens().keySet()) {
            if (!placeIds.contains(placeId)) {
                errors.add("Initial marking references unknown place: " + placeId);
            }
        }
        
        return errors;
    }
    
    /**
     * Builder pattern for creating PetriNet instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private List<Place> places = new ArrayList<>();
        private List<Transition> transitions = new ArrayList<>();
        private List<Arc> arcs = new ArrayList<>();
        private Marking initialMarking = new Marking();
        private String schemaVersion = "1.0";
        private Map<String, Object> metadata = new HashMap<>();
        private String derivedFromDagId;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder addPlace(Place place) {
            this.places.add(place);
            return this;
        }
        
        public Builder addTransition(Transition transition) {
            this.transitions.add(transition);
            return this;
        }
        
        public Builder addArc(Arc arc) {
            this.arcs.add(arc);
            return this;
        }
        
        public Builder addArc(String from, String to, int weight) {
            this.arcs.add(new Arc(from, to, weight));
            return this;
        }
        
        public Builder addArc(String from, String to) {
            this.arcs.add(new Arc(from, to, 1));
            return this;
        }
        
        public Builder initialMarking(Marking marking) {
            this.initialMarking = marking;
            return this;
        }
        
        public Builder addInitialToken(String placeId, int tokens) {
            Map<String, Integer> current = new HashMap<>(this.initialMarking.getTokens());
            current.put(placeId, tokens);
            this.initialMarking = new Marking(current);
            return this;
        }
        
        public Builder schemaVersion(String version) {
            this.schemaVersion = version;
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder derivedFromDagId(String dagId) {
            this.derivedFromDagId = dagId;
            return this;
        }
        
        public PetriNet build() {
            return new PetriNet(id, name, description, places, transitions, arcs, 
                              initialMarking, schemaVersion, metadata, derivedFromDagId);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PetriNet petriNet = (PetriNet) o;
        return Objects.equals(id, petriNet.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "PetriNet{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", places=" + places.size() +
                ", transitions=" + transitions.size() +
                ", arcs=" + arcs.size() +
                '}';
    }
}