package plugins;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents the result of plugin parameter validation.
 */
public class PluginValidationResult {

    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    public PluginValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
    }

    // Factory methods
    public static PluginValidationResult valid() {
        return new PluginValidationResult(true, null, null);
    }

    public static PluginValidationResult valid(List<String> warnings) {
        return new PluginValidationResult(true, null, warnings);
    }

    public static PluginValidationResult invalid(List<String> errors) {
        return new PluginValidationResult(false, errors, null);
    }

    public static PluginValidationResult invalid(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new PluginValidationResult(false, errors, null);
    }

    // Getters
    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public Builder addError(String error) {
            this.errors.add(error);
            return this;
        }

        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        public PluginValidationResult build() {
            boolean valid = errors.isEmpty();
            return new PluginValidationResult(valid, errors, warnings);
        }
    }
}