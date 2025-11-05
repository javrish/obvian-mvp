package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Summary DTO for rule execution history
 */
@Schema(description = "Summary of a business rule execution")
public class RuleExecutionSummary {
    
    @JsonProperty("executionId")
    @Schema(description = "Unique execution identifier")
    private String executionId;
    
    @JsonProperty("executedAt")
    @Schema(description = "Execution timestamp")
    private LocalDateTime executedAt;
    
    @JsonProperty("success")
    @Schema(description = "Whether the execution was successful")
    private boolean success;
    
    @JsonProperty("executionTimeMs")
    @Schema(description = "Execution time in milliseconds")
    private long executionTimeMs;
    
    @JsonProperty("trigger")
    @Schema(description = "What triggered the execution")
    private String trigger;
    
    @JsonProperty("result")
    @Schema(description = "Execution result summary")
    private String result;
    
    @JsonProperty("actionsExecuted")
    @Schema(description = "Number of actions executed")
    private int actionsExecuted;
    
    @JsonProperty("errorMessage")
    @Schema(description = "Error message if execution failed")
    private String errorMessage;
    
    // Constructors
    public RuleExecutionSummary() {}
    
    // Getters and setters
    public String getExecutionId() {
        return executionId;
    }
    
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    
    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
    
    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public String getTrigger() {
        return trigger;
    }
    
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public int getActionsExecuted() {
        return actionsExecuted;
    }
    
    public void setActionsExecuted(int actionsExecuted) {
        this.actionsExecuted = actionsExecuted;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .executionId(this.executionId)
            .executedAt(this.executedAt)
            .success(this.success)
            .executionTimeMs(this.executionTimeMs)
            .trigger(this.trigger)
            .result(this.result)
            .actionsExecuted(this.actionsExecuted)
            .errorMessage(this.errorMessage);
    }
    
    public static class Builder {
        private String executionId;
        private LocalDateTime executedAt;
        private boolean success;
        private long executionTimeMs;
        private String trigger;
        private String result;
        private int actionsExecuted;
        private String errorMessage;
        
        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }
        
        public Builder executedAt(LocalDateTime executedAt) {
            this.executedAt = executedAt;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }
        
        public Builder trigger(String trigger) {
            this.trigger = trigger;
            return this;
        }
        
        public Builder result(String result) {
            this.result = result;
            return this;
        }
        
        public Builder actionsExecuted(int actionsExecuted) {
            this.actionsExecuted = actionsExecuted;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public RuleExecutionSummary build() {
            RuleExecutionSummary summary = new RuleExecutionSummary();
            summary.setExecutionId(executionId);
            summary.setExecutedAt(executedAt);
            summary.setSuccess(success);
            summary.setExecutionTimeMs(executionTimeMs);
            summary.setTrigger(trigger);
            summary.setResult(result);
            summary.setActionsExecuted(actionsExecuted);
            summary.setErrorMessage(errorMessage);
            return summary;
        }
    }
}