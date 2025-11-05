package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PetriErrorResponse {
    @JsonProperty("success")
    private boolean success = false;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("errorCode")
    private String errorCode;

    public PetriErrorResponse() {}

    public PetriErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public PetriErrorResponse(String error, String message, String errorCode) {
        this.error = error;
        this.message = message;
        this.errorCode = errorCode;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
}