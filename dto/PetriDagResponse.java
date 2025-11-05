package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for DAG projection results.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing DAG representation projected from Petri net")
public class PetriDagResponse {

    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0")
    private String schemaVersion;

    @JsonProperty("dag")
    @Schema(description = "DAG representation with nodes and edges")
    private DAGRepresentation dag;

    @JsonProperty("derivedFromPetriNetId")
    @Schema(description = "Source Petri net ID", example = "petri_abc123def456")
    private String derivedFromPetriNetId;

    @JsonProperty("projectionNotes")
    @Schema(description = "Notes about the projection process")
    private List<String> projectionNotes;

    // Nested class for DAG representation
    @Schema(description = "DAG representation with nodes and edges")
    public static class DAGRepresentation {
        @JsonProperty("nodes")
        @Schema(description = "DAG nodes representing transitions")
        private List<DAGNode> nodes;

        @JsonProperty("edges")
        @Schema(description = "DAG edges representing dependencies")
        private List<DAGEdge> edges;

        // Constructors
        public DAGRepresentation() {}

        public DAGRepresentation(List<DAGNode> nodes, List<DAGEdge> edges) {
            this.nodes = nodes;
            this.edges = edges;
        }

        // Getters and Setters
        public List<DAGNode> getNodes() { return nodes; }
        public void setNodes(List<DAGNode> nodes) { this.nodes = nodes; }

        public List<DAGEdge> getEdges() { return edges; }
        public void setEdges(List<DAGEdge> edges) { this.edges = edges; }
    }

    @Schema(description = "DAG node representing a transition")
    public static class DAGNode {
        @JsonProperty("id")
        @Schema(description = "Node ID", example = "t_run_tests")
        private String id;

        @JsonProperty("name")
        @Schema(description = "Node name", example = "Run Tests")
        private String name;

        @JsonProperty("type")
        @Schema(description = "Node type", example = "ACTION")
        private String type;

        @JsonProperty("metadata")
        @Schema(description = "Additional node metadata")
        private Object metadata;

        // Constructors
        public DAGNode() {}

        public DAGNode(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Object getMetadata() { return metadata; }
        public void setMetadata(Object metadata) { this.metadata = metadata; }
    }

    @Schema(description = "DAG edge representing dependency")
    public static class DAGEdge {
        @JsonProperty("from")
        @Schema(description = "Source node ID", example = "t_run_tests")
        private String from;

        @JsonProperty("to")
        @Schema(description = "Target node ID", example = "t_deploy")
        private String to;

        @JsonProperty("condition")
        @Schema(description = "Optional condition for edge", example = "pass")
        private String condition;

        @JsonProperty("metadata")
        @Schema(description = "Additional edge metadata")
        private Object metadata;

        // Constructors
        public DAGEdge() {}

        public DAGEdge(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public DAGEdge(String from, String to, String condition) {
            this.from = from;
            this.to = to;
            this.condition = condition;
        }

        // Getters and Setters
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public Object getMetadata() { return metadata; }
        public void setMetadata(Object metadata) { this.metadata = metadata; }
    }

    // Constructors
    public PetriDagResponse() {}

    public PetriDagResponse(String schemaVersion, DAGRepresentation dag, String derivedFromPetriNetId) {
        this.schemaVersion = schemaVersion;
        this.dag = dag;
        this.derivedFromPetriNetId = derivedFromPetriNetId;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public DAGRepresentation getDag() { return dag; }
    public void setDag(DAGRepresentation dag) { this.dag = dag; }

    public String getDerivedFromPetriNetId() { return derivedFromPetriNetId; }
    public void setDerivedFromPetriNetId(String derivedFromPetriNetId) { this.derivedFromPetriNetId = derivedFromPetriNetId; }

    public List<String> getProjectionNotes() { return projectionNotes; }
    public void setProjectionNotes(List<String> projectionNotes) { this.projectionNotes = projectionNotes; }

    // Factory methods
    public static PetriDagResponse success(String schemaVersion, DAGRepresentation dag, String derivedFromPetriNetId) {
        return new PetriDagResponse(schemaVersion, dag, derivedFromPetriNetId);
    }

    @Override
    public String toString() {
        return "PetriDagResponse{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", dag=" + dag +
                ", derivedFromPetriNetId='" + derivedFromPetriNetId + '\'' +
                ", projectionNotes=" + (projectionNotes != null ? projectionNotes.size() + " notes" : "no notes") +
                '}';
    }
}