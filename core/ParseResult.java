package core;

import core.petri.PetriIntentSpec;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents the result of parsing operations, typically from natural language input.
 */
public class ParseResult {

    private final boolean success;
    private final PetriIntentSpec intentSpec;
    private final String templateId;
    private final double confidence;
    private final List<String> errors;
    private final List<String> warnings;

    private ParseResult(boolean success, PetriIntentSpec intentSpec, String templateId,
                       double confidence, List<String> errors, List<String> warnings) {
        this.success = success;
        this.intentSpec = intentSpec;
        this.templateId = templateId;
        this.confidence = confidence;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
    }

    // Factory methods for success cases
    public static ParseResult success(PetriIntentSpec intentSpec, String templateId, double confidence) {
        return new ParseResult(true, intentSpec, templateId, confidence, null, null);
    }

    public static ParseResult success(PetriIntentSpec intentSpec, String templateId, double confidence, List<String> warnings) {
        return new ParseResult(true, intentSpec, templateId, confidence, null, warnings);
    }

    // Factory methods for failure cases
    public static ParseResult failure(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new ParseResult(false, null, null, 0.0, errors, null);
    }

    public static ParseResult failure(List<String> errors) {
        return new ParseResult(false, null, null, 0.0, errors, null);
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public PetriIntentSpec getIntentSpec() {
        return intentSpec;
    }

    public String getTemplateId() {
        return templateId;
    }

    public double getConfidence() {
        return confidence;
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