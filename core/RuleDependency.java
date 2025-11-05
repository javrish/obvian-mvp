package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a dependency relationship between business rules including
 * dependency type, strength, and ordering requirements.
 * 
 * This class is part of the BusinessRuleValidator system (Phase 26.2c)
 * which implements comprehensive dependency analysis for business rules
 * with intelligent ordering and cycle detection capabilities.
 * 
 * Patent Alignment: Implements rule dependency analysis and representation
 * that enables intelligent business rule orchestration with dependency-aware
 * execution ordering and cycle detection.
 * 
 * @author Obvian Labs
 * @since Phase 26.2c
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleDependency {
    
    /**
     * Types of rule dependencies
     */
    public enum DependencyType {
        EXPLICIT,           // Explicitly declared dependency
        IMPLICIT,           // Inferred from rule logic
        DATA,              // Dependency on data produced by another rule
        TEMPORAL,          // Time-based dependency
        CONDITIONAL,       // Conditional dependency based on rule outcomes
        RESOURCE,          // Dependency on shared resources
        SEMANTIC,          // Semantic dependency based on business logic
        EXECUTION_ORDER,   // Strict execution order requirement
        WEAK,              // Soft dependency that can be violated if needed
        CYCLIC             // Part of a circular dependency chain
    }
    
    /**
     * Dependency strength levels
     */
    public enum DependencyStrength {
        MANDATORY,         // Must be satisfied for correct execution
        RECOMMENDED,       // Should be satisfied for optimal execution
        OPTIONAL,          // Can be satisfied for enhanced functionality
        WEAK              // Minor preference, can be ignored
    }
    
    /**
     * Dependency validation status
     */
    public enum ValidationStatus {
        VALID,             // Dependency is valid and can be satisfied
        INVALID,           // Dependency cannot be satisfied
        CIRCULAR,          // Dependency creates a circular reference
        MISSING,           // Required dependency is missing
        CONFLICTING,       // Dependency conflicts with other dependencies
        UNRESOLVED         // Dependency resolution is pending
    }
    
    @JsonProperty("dependencyId")
    private final String dependencyId;
    
    @JsonProperty("dependentRule")
    private final BusinessRule dependentRule;
    
    @JsonProperty("dependsOnRule")
    private final BusinessRule dependsOnRule;
    
    @JsonProperty("type")
    private final DependencyType type;
    
    @JsonProperty("strength")
    private final DependencyStrength strength;
    
    @JsonProperty("validationStatus")
    private final ValidationStatus validationStatus;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("dependencyContext")
    private final Map<String, Object> dependencyContext;
    
    @JsonProperty("detectedAt")
    private final LocalDateTime detectedAt;
    
    @JsonProperty("confidence")
    private final double confidence;
    
    @JsonProperty("executionOrder")
    private final int executionOrder;
    
    @JsonProperty("conditions")
    private final List<String> conditions;
    
    @JsonProperty("metadata")
    private final Map<String, Object> metadata;
    
    @JsonCreator
    private RuleDependency(
            @JsonProperty("dependencyId") String dependencyId,
            @JsonProperty("dependentRule") BusinessRule dependentRule,
            @JsonProperty("dependsOnRule") BusinessRule dependsOnRule,
            @JsonProperty("type") DependencyType type,
            @JsonProperty("strength") DependencyStrength strength,
            @JsonProperty("validationStatus") ValidationStatus validationStatus,
            @JsonProperty("description") String description,
            @JsonProperty("dependencyContext") Map<String, Object> dependencyContext,
            @JsonProperty("detectedAt") LocalDateTime detectedAt,
            @JsonProperty("confidence") double confidence,
            @JsonProperty("executionOrder") int executionOrder,
            @JsonProperty("conditions") List<String> conditions,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.dependencyId = dependencyId != null ? dependencyId : UUID.randomUUID().toString();
        this.dependentRule = dependentRule;
        this.dependsOnRule = dependsOnRule;
        this.type = type;
        this.strength = strength;
        this.validationStatus = validationStatus;
        this.description = description;
        this.dependencyContext = dependencyContext != null ? new HashMap<>(dependencyContext) : new HashMap<>();
        this.detectedAt = detectedAt != null ? detectedAt : LocalDateTime.now();
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.executionOrder = executionOrder;
        this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    // Getters
    public String getDependencyId() { return dependencyId; }
    public BusinessRule getDependentRule() { return dependentRule; }
    public BusinessRule getDependsOnRule() { return dependsOnRule; }
    public String getDependentRuleId() { return dependentRule != null ? dependentRule.getId() : null; }
    public String getDependsOnRuleId() { return dependsOnRule != null ? dependsOnRule.getId() : null; }
    public DependencyType getType() { return type; }
    public DependencyStrength getStrength() { return strength; }
    public ValidationStatus getValidationStatus() { return validationStatus; }
    public String getDescription() { return description; }
    public Map<String, Object> getDependencyContext() { return new HashMap<>(dependencyContext); }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public double getConfidence() { return confidence; }
    public int getExecutionOrder() { return executionOrder; }
    public List<String> getConditions() { return new ArrayList<>(conditions); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    
    /**
     * Check if this is a mandatory dependency
     */
    public boolean isMandatory() {
        return strength == DependencyStrength.MANDATORY;
    }
    
    /**
     * Check if this dependency is blocking
     */
    public boolean isBlocking() {
        return strength == DependencyStrength.MANDATORY && 
               validationStatus != ValidationStatus.VALID;
    }
    
    /**
     * Check if this dependency is valid
     */
    public boolean isValid() {
        return validationStatus == ValidationStatus.VALID;
    }
    
    /**
     * Check if this dependency creates a circular reference
     */
    public boolean isCircular() {
        return validationStatus == ValidationStatus.CIRCULAR ||
               type == DependencyType.CYCLIC;
    }
    
    /**
     * Check if this is a data dependency
     */
    public boolean isDataDependency() {
        return type == DependencyType.DATA;
    }
    
    /**
     * Check if this is a temporal dependency
     */
    public boolean isTemporalDependency() {
        return type == DependencyType.TEMPORAL;
    }
    
    /**
     * Get the dependency relationship string
     */
    public String getDependencyRelationship() {
        String dependent = dependentRule != null ? dependentRule.getId() : "unknown";
        String dependsOn = dependsOnRule != null ? dependsOnRule.getId() : "unknown";
        return dependent + " -> " + dependsOn;
    }
    
    /**
     * Get strength as numeric score (higher = stronger)
     */
    public int getStrengthScore() {
        return switch (strength) {
            case MANDATORY -> 4;
            case RECOMMENDED -> 3;
            case OPTIONAL -> 2;
            case WEAK -> 1;
        };
    }
    
    /**
     * Check if dependency has conditions
     */
    public boolean hasConditions() {
        return !conditions.isEmpty();
    }
    
    /**
     * Get formatted dependency summary
     */
    public String getFormattedSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("[").append(strength).append("] ");
        summary.append(type.toString().replace("_", " "));
        summary.append(" dependency: ");
        summary.append(getDependencyRelationship());
        
        if (description != null && !description.isEmpty()) {
            summary.append(" - ").append(description);
        }
        
        if (validationStatus != ValidationStatus.VALID) {
            summary.append(" (").append(validationStatus).append(")");
        }
        
        return summary.toString();
    }
    
    /**
     * Builder pattern for creating RuleDependency instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String dependencyId;
        private BusinessRule dependentRule;
        private BusinessRule dependsOnRule;
        private DependencyType type;
        private DependencyStrength strength = DependencyStrength.RECOMMENDED;
        private ValidationStatus validationStatus = ValidationStatus.VALID;
        private String description;
        private Map<String, Object> dependencyContext = new HashMap<>();
        private LocalDateTime detectedAt;
        private double confidence = 1.0;
        private int executionOrder = 0;
        private List<String> conditions = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();
        
        public Builder dependencyId(String dependencyId) {
            this.dependencyId = dependencyId;
            return this;
        }
        
        public Builder dependentRule(BusinessRule dependentRule) {
            this.dependentRule = dependentRule;
            return this;
        }
        
        public Builder dependsOnRule(BusinessRule dependsOnRule) {
            this.dependsOnRule = dependsOnRule;
            return this;
        }
        
        public Builder type(DependencyType type) {
            this.type = type;
            return this;
        }
        
        public Builder strength(DependencyStrength strength) {
            this.strength = strength;
            return this;
        }
        
        public Builder validationStatus(ValidationStatus status) {
            this.validationStatus = status;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder addDependencyContext(String key, Object value) {
            this.dependencyContext.put(key, value);
            return this;
        }
        
        public Builder dependencyContext(Map<String, Object> context) {
            this.dependencyContext = new HashMap<>(context);
            return this;
        }
        
        public Builder detectedAt(LocalDateTime detectedAt) {
            this.detectedAt = detectedAt;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder executionOrder(int order) {
            this.executionOrder = order;
            return this;
        }
        
        public Builder addCondition(String condition) {
            this.conditions.add(condition);
            return this;
        }
        
        public Builder conditions(List<String> conditions) {
            this.conditions = new ArrayList<>(conditions);
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
        
        public RuleDependency build() {
            validate();
            generateDefaultDescription();
            return new RuleDependency(dependencyId, dependentRule, dependsOnRule, type, strength,
                    validationStatus, description, dependencyContext, detectedAt, confidence,
                    executionOrder, conditions, metadata);
        }
        
        private void validate() {
            if (dependentRule == null) {
                throw new IllegalArgumentException("Dependent rule is required");
            }
            if (dependsOnRule == null) {
                throw new IllegalArgumentException("Depends-on rule is required");
            }
            if (type == null) {
                throw new IllegalArgumentException("Dependency type is required");
            }
            if (dependentRule.getId().equals(dependsOnRule.getId())) {
                throw new IllegalArgumentException("Rule cannot depend on itself");
            }
        }
        
        private void generateDefaultDescription() {
            if (description == null || description.trim().isEmpty()) {
                description = generateDefaultDescription(type, dependentRule, dependsOnRule);
            }
        }
        
        private String generateDefaultDescription(DependencyType type, BusinessRule dependent, BusinessRule dependsOn) {
            return switch (type) {
                case EXPLICIT -> "Explicit dependency: " + dependent.getName() + " depends on " + dependsOn.getName();
                case IMPLICIT -> "Implicit dependency detected between " + dependent.getName() + " and " + dependsOn.getName();
                case DATA -> "Data dependency: " + dependent.getName() + " requires data from " + dependsOn.getName();
                case TEMPORAL -> "Temporal dependency: " + dependent.getName() + " must execute after " + dependsOn.getName();
                case CONDITIONAL -> "Conditional dependency: " + dependent.getName() + " depends on outcome of " + dependsOn.getName();
                case RESOURCE -> "Resource dependency: " + dependent.getName() + " and " + dependsOn.getName() + " share resources";
                case SEMANTIC -> "Semantic dependency: " + dependent.getName() + " has business logic dependency on " + dependsOn.getName();
                case EXECUTION_ORDER -> "Execution order dependency: " + dependent.getName() + " must execute after " + dependsOn.getName();
                case WEAK -> "Weak dependency: " + dependent.getName() + " has minor dependency on " + dependsOn.getName();
                case CYCLIC -> "Circular dependency: " + dependent.getName() + " and " + dependsOn.getName() + " are mutually dependent";
            };
        }
    }
    
    /**
     * Dependency analysis utilities
     */
    public static class Analysis {
        
        /**
         * Check if two dependencies form a cycle
         */
        public static boolean formsCycle(RuleDependency dep1, RuleDependency dep2) {
            return dep1.getDependentRule().getId().equals(dep2.getDependsOnRule().getId()) &&
                   dep1.getDependsOnRule().getId().equals(dep2.getDependentRule().getId());
        }
        
        /**
         * Calculate dependency chain length
         */
        public static int calculateChainLength(List<RuleDependency> dependencies, String startRuleId) {
            Set<String> visited = new HashSet<>();
            return calculateChainLengthRecursive(dependencies, startRuleId, visited);
        }
        
        private static int calculateChainLengthRecursive(List<RuleDependency> dependencies, 
                                                       String ruleId, Set<String> visited) {
            if (visited.contains(ruleId)) {
                return 0; // Cycle detected or already processed
            }
            
            visited.add(ruleId);
            int maxLength = 0;
            
            for (RuleDependency dep : dependencies) {
                if (dep.getDependentRule().getId().equals(ruleId)) {
                    int chainLength = 1 + calculateChainLengthRecursive(dependencies, 
                            dep.getDependsOnRule().getId(), visited);
                    maxLength = Math.max(maxLength, chainLength);
                }
            }
            
            return maxLength;
        }
        
        /**
         * Find all dependencies of a rule
         */
        public static List<RuleDependency> findDependencies(List<RuleDependency> allDependencies, String ruleId) {
            return allDependencies.stream()
                    .filter(dep -> dep.getDependentRule().getId().equals(ruleId))
                    .toList();
        }
        
        /**
         * Find all dependents of a rule
         */
        public static List<RuleDependency> findDependents(List<RuleDependency> allDependencies, String ruleId) {
            return allDependencies.stream()
                    .filter(dep -> dep.getDependsOnRule().getId().equals(ruleId))
                    .toList();
        }
        
        /**
         * Detect circular dependencies
         */
        public static List<List<String>> detectCircularDependencies(List<RuleDependency> dependencies) {
            List<List<String>> cycles = new ArrayList<>();
            Set<String> visited = new HashSet<>();
            Set<String> recursionStack = new HashSet<>();
            
            // Build adjacency list
            Map<String, List<String>> graph = new HashMap<>();
            for (RuleDependency dep : dependencies) {
                String dependent = dep.getDependentRule().getId();
                String dependsOn = dep.getDependsOnRule().getId();
                graph.computeIfAbsent(dependent, k -> new ArrayList<>()).add(dependsOn);
            }
            
            // DFS to detect cycles
            for (String node : graph.keySet()) {
                if (!visited.contains(node)) {
                    List<String> cycle = new ArrayList<>();
                    if (hasCycleDFS(graph, node, visited, recursionStack, cycle)) {
                        cycles.add(new ArrayList<>(cycle));
                    }
                }
            }
            
            return cycles;
        }
        
        private static boolean hasCycleDFS(Map<String, List<String>> graph, String node,
                                         Set<String> visited, Set<String> recursionStack,
                                         List<String> cycle) {
            visited.add(node);
            recursionStack.add(node);
            cycle.add(node);
            
            List<String> neighbors = graph.getOrDefault(node, new ArrayList<>());
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    if (hasCycleDFS(graph, neighbor, visited, recursionStack, cycle)) {
                        return true;
                    }
                } else if (recursionStack.contains(neighbor)) {
                    // Found a cycle - trim cycle to start from the back-edge
                    int cycleStart = cycle.indexOf(neighbor);
                    if (cycleStart >= 0) {
                        cycle.subList(0, cycleStart).clear();
                    }
                    return true;
                }
            }
            
            recursionStack.remove(node);
            cycle.remove(cycle.size() - 1);
            return false;
        }
        
        /**
         * Perform topological sort on dependencies
         */
        public static List<String> topologicalSort(List<RuleDependency> dependencies) {
            Map<String, List<String>> graph = new HashMap<>();
            Map<String, Integer> inDegree = new HashMap<>();
            
            // Build graph and calculate in-degrees
            for (RuleDependency dep : dependencies) {
                String dependent = dep.getDependentRule().getId();
                String dependsOn = dep.getDependsOnRule().getId();
                
                graph.computeIfAbsent(dependsOn, k -> new ArrayList<>()).add(dependent);
                inDegree.put(dependent, inDegree.getOrDefault(dependent, 0) + 1);
                inDegree.putIfAbsent(dependsOn, 0);
            }
            
            // Kahn's algorithm
            Queue<String> queue = new LinkedList<>();
            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() == 0) {
                    queue.offer(entry.getKey());
                }
            }
            
            List<String> result = new ArrayList<>();
            while (!queue.isEmpty()) {
                String node = queue.poll();
                result.add(node);
                
                for (String neighbor : graph.getOrDefault(node, new ArrayList<>())) {
                    inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                    if (inDegree.get(neighbor) == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
            
            return result;
        }
    }
    
    /**
     * Predefined dependency creators for common dependency types
     */
    public static class Common {
        
        public static RuleDependency explicitDependency(BusinessRule dependent, BusinessRule dependsOn) {
            return builder()
                    .dependentRule(dependent)
                    .dependsOnRule(dependsOn)
                    .type(DependencyType.EXPLICIT)
                    .strength(DependencyStrength.MANDATORY)
                    .build();
        }
        
        public static RuleDependency dataDependency(BusinessRule dependent, BusinessRule dependsOn, String dataField) {
            return builder()
                    .dependentRule(dependent)
                    .dependsOnRule(dependsOn)
                    .type(DependencyType.DATA)
                    .strength(DependencyStrength.MANDATORY)
                    .description("Data dependency on field: " + dataField)
                    .addDependencyContext("dataField", dataField)
                    .build();
        }
        
        public static RuleDependency temporalDependency(BusinessRule dependent, BusinessRule dependsOn) {
            return builder()
                    .dependentRule(dependent)
                    .dependsOnRule(dependsOn)
                    .type(DependencyType.TEMPORAL)
                    .strength(DependencyStrength.RECOMMENDED)
                    .build();
        }
        
        public static RuleDependency conditionalDependency(BusinessRule dependent, BusinessRule dependsOn, 
                                                          String condition) {
            return builder()
                    .dependentRule(dependent)
                    .dependsOnRule(dependsOn)
                    .type(DependencyType.CONDITIONAL)
                    .strength(DependencyStrength.RECOMMENDED)
                    .addCondition(condition)
                    .build();
        }
        
        public static RuleDependency resourceDependency(BusinessRule dependent, BusinessRule dependsOn, String resource) {
            return builder()
                    .dependentRule(dependent)
                    .dependsOnRule(dependsOn)
                    .type(DependencyType.RESOURCE)
                    .strength(DependencyStrength.MANDATORY)
                    .description("Shared resource dependency: " + resource)
                    .addDependencyContext("sharedResource", resource)
                    .build();
        }
        
        public static RuleDependency weakDependency(BusinessRule dependent, BusinessRule dependsOn) {
            return builder()
                    .dependentRule(dependent)
                    .dependsOnRule(dependsOn)
                    .type(DependencyType.WEAK)
                    .strength(DependencyStrength.OPTIONAL)
                    .build();
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleDependency that = (RuleDependency) o;
        return Double.compare(that.confidence, confidence) == 0 &&
               executionOrder == that.executionOrder &&
               Objects.equals(dependencyId, that.dependencyId) &&
               Objects.equals(dependentRule, that.dependentRule) &&
               Objects.equals(dependsOnRule, that.dependsOnRule) &&
               type == that.type &&
               strength == that.strength;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(dependencyId, dependentRule, dependsOnRule, type, strength, confidence, executionOrder);
    }
    
    @Override
    public String toString() {
        return "RuleDependency{" +
                "dependencyId='" + dependencyId + '\'' +
                ", type=" + type +
                ", strength=" + strength +
                ", relationship='" + getDependencyRelationship() + '\'' +
                ", validationStatus=" + validationStatus +
                ", confidence=" + confidence +
                '}';
    }
}