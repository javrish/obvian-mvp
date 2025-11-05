package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginValidationResult {
    private String pluginId;
    private boolean valid;
    private List<String> errors;
    private List<String> warnings;
    private LocalDateTime timestamp;

    public PluginValidationResult() {
    }

    public PluginValidationResult(String pluginId, boolean valid, List<String> errors, List<String> warnings, LocalDateTime timestamp) {
        this.pluginId = pluginId;
        this.valid = valid;
        this.errors = errors;
        this.warnings = warnings;
        this.timestamp = timestamp;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}