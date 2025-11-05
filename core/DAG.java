package core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Directed Acyclic Graph of tasks
 */
public class DAG {
    private String id = "unknown"; // Default DAG ID for metrics
    private String derivedFromPetriNetId; // ID of source Petri net if converted
    List<TaskNode> nodes;
    TaskNode rootNode;
    
    public DAG() {
        this.nodes = new ArrayList<>();
    }
    
    public DAG(String id) {
        this.id = id != null ? id : "unknown";
        this.nodes = new ArrayList<>();
    }
    
    public DAG(List<TaskNode> nodes) {
        this.nodes = new ArrayList<>(nodes != null ? nodes : new ArrayList<>());
        this.id = "test_dag_" + System.currentTimeMillis();
    }
    
    public List<TaskNode> getNodes() {
        return new ArrayList<>(nodes);
    }
    
    public void setNodes(List<TaskNode> nodes) {
        this.nodes = nodes;
    }
    
    public TaskNode getRootNode() {
        return rootNode;
    }
    
    public void setRootNode(TaskNode rootNode) {
        this.rootNode = rootNode;
        if (nodes != null && !nodes.contains(rootNode)) {
            nodes.add(rootNode);
        }
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id != null ? id : "unknown";
    }
    
    public String getDerivedFromPetriNetId() {
        return derivedFromPetriNetId;
    }
    
    public void setDerivedFromPetriNetId(String derivedFromPetriNetId) {
        this.derivedFromPetriNetId = derivedFromPetriNetId;
    }
    
    /**
     * Get the name of this DAG (for compatibility with PetriNet)
     */
    public String getName() {
        return (String) metadata.get("name");
    }
    
    /**
     * Set the name of this DAG
     */
    public void setName(String name) {
        metadata.put("name", name);
    }
    
    public void addNode(TaskNode node) {
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
    }
    
    public void removeNode(TaskNode node) {
        nodes.remove(node);
        if (rootNode == node) {
            rootNode = null;
        }
    }
    
    @JsonIgnore
    public boolean isEmpty() {
        return nodes == null || nodes.isEmpty();
    }
    
    /**
     * Prepare DAG for serialization by clearing transient circular references
     * This prevents Jackson serialization issues with circular references
     */
    public void prepareForSerialization() {
        if (nodes == null) return;
        
        // Clear all transient dependencies to prevent circular reference issues
        for (TaskNode node : nodes) {
            if (node != null) { // Add null check to prevent NPE
                if (node.resolvedDependencies != null) {
                    try {
                        node.resolvedDependencies.clear();
                    } catch (UnsupportedOperationException e) {
                        // Handle immutable collections by replacing with empty mutable list
                        node.resolvedDependencies = new java.util.ArrayList<>();
                    }
                }
                if (node.dependents != null) {
                    try {
                        node.dependents.clear();
                    } catch (UnsupportedOperationException e) {
                        // Handle immutable collections by replacing with empty mutable list
                        node.dependents = new java.util.ArrayList<>();
                    }
                }
            }
        }
    }
    
    /**
     * Rebuild transient dependencies after deserialization
     * This method reconstructs the resolvedDependencies and dependents lists
     * from the persistent dependencyIds to resolve circular reference issues
     */
    public void rebuildDependencies() {
        if (nodes == null) return;
        
        // Clear all transient dependencies first
        for (TaskNode node : nodes) {
            if (node == null) {
                continue; // Skip null nodes to avoid NullPointerException
            }
            if (node.resolvedDependencies == null) {
                node.resolvedDependencies = new ArrayList<>();
            } else {
                node.resolvedDependencies.clear();
            }
            if (node.dependents == null) {
                node.dependents = new ArrayList<>();
            } else {
                node.dependents.clear();
            }
        }
        
        // Create a map for fast node lookup
        Map<String, TaskNode> nodeMap = new HashMap<>();
        for (TaskNode node : nodes) {
            if (node != null) {
                nodeMap.put(node.getId(), node);
            }
        }
        
        // Rebuild dependencies
        for (TaskNode node : nodes) {
            if (node != null && node.getDependencyIds() != null) {
                for (String depId : node.getDependencyIds()) {
                    TaskNode dependency = nodeMap.get(depId);
                    if (dependency != null) {
                        node.resolvedDependencies.add(dependency);
                        dependency.dependents.add(node);
                    }
                }
            }
        }
    }
    
    // Additional methods for LiveDagPreview compatibility
    
    public TaskNode getNode(String nodeId) {
        if (nodes == null || nodeId == null) return null;
        return nodes.stream()
            .filter(node -> nodeId.equals(node.getId()))
            .findFirst()
            .orElse(null);
    }
    
    public boolean hasNode(String nodeId) {
        return getNode(nodeId) != null;
    }
    
    public Map<String, java.util.Set<String>> getDependencies() {
        Map<String, java.util.Set<String>> dependencies = new HashMap<>();
        if (nodes != null) {
            for (TaskNode node : nodes) {
                if (node.getDependencyIds() != null && !node.getDependencyIds().isEmpty()) {
                    dependencies.put(node.getId(), new java.util.HashSet<>(node.getDependencyIds()));
                }
            }
        }
        return dependencies;
    }
    
    public boolean hasDependency(String sourceNodeId, String targetNodeId) {
        TaskNode targetNode = getNode(targetNodeId);
        return targetNode != null && 
               targetNode.getDependencyIds() != null && 
               targetNode.getDependencyIds().contains(sourceNodeId);
    }
    
    public void addDependency(String sourceNodeId, String targetNodeId) {
        TaskNode targetNode = getNode(targetNodeId);
        if (targetNode != null) {
            if (targetNode.getDependencyIds() == null) {
                targetNode.setDependencyIds(new ArrayList<>());
            }
            if (!targetNode.getDependencyIds().contains(sourceNodeId)) {
                targetNode.getDependencyIds().add(sourceNodeId);
            }
        }
    }
    
    // Validation support fields and methods
    private java.util.List<String> validationWarnings = new ArrayList<>();
    private DagValidationResult validationResult;
    private Map<String, Object> metadata = new HashMap<>();
    private boolean livePreviewMode = false;
    
    public java.util.List<String> getValidationWarnings() { 
        return new ArrayList<>(validationWarnings); 
    }
    
    public void addValidationWarning(String warning) {
        validationWarnings.add(warning);
    }
    
    public DagValidationResult getValidationResult() {
        return validationResult;
    }
    
    public void setValidationResult(DagValidationResult validationResult) {
        this.validationResult = validationResult;
    }
    
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }
    
    public boolean isLivePreviewMode() {
        return livePreviewMode;
    }
    
    public void setLivePreviewMode(boolean livePreviewMode) {
        this.livePreviewMode = livePreviewMode;
    }
    
    @Override
    public String toString() {
        return "DAG{" +
                "nodes=" + (nodes == null ? 0 : nodes.size()) +
                ", rootNode=" + rootNode +
                '}';
    }
}