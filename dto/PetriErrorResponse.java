package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Standard error response for Petri net API endpoints.
 *
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error response for Petri net API operations")
public class PetriErrorResponse {

    @JsonProperty("schemaVersion")
    @Schema(description = "Schema version", example = "1.0")
    private String schemaVersion;

    @JsonProperty("error")
    @Schema(description = "Error information")
    private ErrorInfo error;

    // Nested class for error information
    @Schema(description = "Detailed error information")
    public static class ErrorInfo {
        @JsonProperty("code")
        @Schema(description = "Error code", example = "VALIDATION_INCONCLUSIVE")
        private String code;

        @JsonProperty("message")
        @Schema(description = "Human-readable error message", example = "Bound 200 reached during reachability analysis")
        private String message;

        @JsonProperty("details")
        @Schema(description = "Additional error details")
        private Map<String, Object> details;

        // Constructors
        public ErrorInfo() {}

        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorInfo(String code, String message, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        // Getters and Setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }

    // Constructors
    public PetriErrorResponse() {}

    public PetriErrorResponse(String schemaVersion, ErrorInfo error) {
        this.schemaVersion = schemaVersion;
        this.error = error;
    }

    // Getters and Setters
    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public ErrorInfo getError() { return error; }
    public void setError(ErrorInfo error) { this.error = error; }

    // Factory methods
    public static PetriErrorResponse create(String schemaVersion, String code, String message) {
        return new PetriErrorResponse(schemaVersion, new ErrorInfo(code, message));
    }

    public static PetriErrorResponse create(String schemaVersion, String code, String message, Map<String, Object> details) {
        return new PetriErrorResponse(schemaVersion, new ErrorInfo(code, message, details));
    }

    // Common error responses
    public static PetriErrorResponse invalidInput(String schemaVersion, String message) {
        return create(schemaVersion, "INVALID_INPUT", message);
    }

    public static PetriErrorResponse constructionConflict(String schemaVersion, String message) {
        return create(schemaVersion, "CONSTRUCTION_CONFLICT", message);
    }

    public static PetriErrorResponse validationInconclusive(String schemaVersion, String message, Map<String, Object> details) {
        return create(schemaVersion, "VALIDATION_INCONCLUSIVE", message, details);
    }

    public static PetriErrorResponse engineError(String schemaVersion, String message) {
        return create(schemaVersion, "ENGINE_ERROR", message);
    }

    public static PetriErrorResponse parseError(String schemaVersion, String message) {
        return create(schemaVersion, "PARSE_ERROR", message);
    }

    public static PetriErrorResponse buildError(String schemaVersion, String message) {
        return create(schemaVersion, "BUILD_ERROR", message);
    }

    public static PetriErrorResponse validationError(String schemaVersion, String message) {
        return create(schemaVersion, "VALIDATION_ERROR", message);
    }

    public static PetriErrorResponse simulationError(String schemaVersion, String message) {
        return create(schemaVersion, "SIMULATION_ERROR", message);
    }

    public static PetriErrorResponse dagProjectionError(String schemaVersion, String message) {
        return create(schemaVersion, "DAG_PROJECTION_ERROR", message);
    }

    @Override
    public String toString() {
        return "PetriErrorResponse{" +
                "schemaVersion='" + schemaVersion + '\'' +
                ", error=" + error +
                '}';
    }
}