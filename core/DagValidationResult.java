package core;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents the result of DAG validation operations.
 */
public class DagValidationResult {

    public enum ValidationStatus {
        VALID,
        INVALID,
        WARNING
    }

    private final ValidationStatus status;
    private final List<String> errors;
    private final List<String> warnings;
    private final boolean isValid;

    public DagValidationResult(boolean isValid, List<String> errors) {
        this.isValid = isValid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.status = isValid ? ValidationStatus.VALID : ValidationStatus.INVALID;
    }

    public DagValidationResult(boolean isValid, List<String> errors, List<String> warnings) {
        this.isValid = isValid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        this.status = determineStatus(isValid, this.errors, this.warnings);
    }

    private ValidationStatus determineStatus(boolean isValid, List<String> errors, List<String> warnings) {
        if (!isValid || !errors.isEmpty()) {
            return ValidationStatus.INVALID;
        }
        if (!warnings.isEmpty()) {
            return ValidationStatus.WARNING;
        }
        return ValidationStatus.VALID;
    }

    // Factory methods
    public static DagValidationResult valid() {
        return new DagValidationResult(true, null);
    }

    public static DagValidationResult valid(List<String> warnings) {
        return new DagValidationResult(true, null, warnings);
    }

    public static DagValidationResult invalid(List<String> errors) {
        return new DagValidationResult(false, errors);
    }

    public static DagValidationResult invalid(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new DagValidationResult(false, errors);
    }

    // Getters
    public ValidationStatus getStatus() {
        return status;
    }

    public boolean isValid() {
        return isValid;
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
}