package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowExecutionDto {
    private String id;
    private String workflowId;
    private String status; // RUNNING, COMPLETED, FAILED, CANCELLED
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, Object> parameters;
    private Map<String, Object> result;
    private String error;
    private Long duration; // in milliseconds
    private Map<String, Object> executionTrace;

    public WorkflowExecutionDto() {
    }

    public WorkflowExecutionDto(String id, String workflowId, String status, LocalDateTime startTime, LocalDateTime endTime, Map<String, Object> parameters, Map<String, Object> result, String error, Long duration, Map<String, Object> executionTrace) {
        this.id = id;
        this.workflowId = workflowId;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.parameters = parameters;
        this.result = result;
        this.error = error;
        this.duration = duration;
        this.executionTrace = executionTrace;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public void setResult(Map<String, Object> result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Map<String, Object> getExecutionTrace() {
        return executionTrace;
    }

    public void setExecutionTrace(Map<String, Object> executionTrace) {
        this.executionTrace = executionTrace;
    }
}