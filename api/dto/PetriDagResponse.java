package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PetriDagResponse {
    @JsonProperty("success")
    private boolean success;

    @JsonProperty("dag")
    private DAGRepresentation dag;

    @JsonProperty("message")
    private String message;

    @JsonProperty("conversionTime")
    private long conversionTime;

    public PetriDagResponse() {}

    public PetriDagResponse(boolean success, DAGRepresentation dag, String message, long conversionTime) {
        this.success = success;
        this.dag = dag;
        this.message = message;
        this.conversionTime = conversionTime;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public DAGRepresentation getDag() { return dag; }
    public void setDag(DAGRepresentation dag) { this.dag = dag; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getConversionTime() { return conversionTime; }
    public void setConversionTime(long conversionTime) { this.conversionTime = conversionTime; }

    // Factory methods
    public static PetriDagResponse success(String schemaVersion, DAGRepresentation dag, String derivedFromPetriNetId) {
        PetriDagResponse response = new PetriDagResponse();
        response.success = true;
        response.dag = dag;
        response.message = "Successfully generated DAG representation";
        return response;
    }

    // For controller compatibility
    public void setProjectionNotes(List<String> notes) {
        // Store in metadata if needed
    }

    /**
     * Represents a DAG structure for API responses
     */
    public static class DAGRepresentation {
        @JsonProperty("nodes")
        private List<DAGNode> nodes;

        @JsonProperty("edges")
        private List<DAGEdge> edges;

        public DAGRepresentation() {}

        public DAGRepresentation(List<DAGNode> nodes, List<DAGEdge> edges) {
            this.nodes = nodes;
            this.edges = edges;
        }

        public List<DAGNode> getNodes() { return nodes; }
        public void setNodes(List<DAGNode> nodes) { this.nodes = nodes; }

        public List<DAGEdge> getEdges() { return edges; }
        public void setEdges(List<DAGEdge> edges) { this.edges = edges; }
    }

    /**
     * Represents a node in the DAG
     */
    public static class DAGNode {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("type")
        private String type;

        public DAGNode() {}

        public DAGNode(String id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    /**
     * Represents an edge in the DAG
     */
    public static class DAGEdge {
        @JsonProperty("from")
        private String from;

        @JsonProperty("to")
        private String to;

        public DAGEdge() {}

        public DAGEdge(String from, String to) {
            this.from = from;
            this.to = to;
        }

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }
}