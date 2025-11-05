package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

/**
 * Configuration class for business rule validation settings.
 * Controls which validation features are enabled, performance parameters,
 * and customization options for the validation process.
 * 
 * This class is part of the BusinessRuleValidator system (Phase 26.2c)
 * which implements comprehensive rule validation with configurable
 * validation capabilities and performance tuning options.
 * 
 * Patent Alignment: Implements validation configuration that enables
 * flexible and customizable business rule validation with performance
 * optimization and feature toggling capabilities.
 * 
 * @author Obvian Labs
 * @since Phase 26.2c
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationConfig {
    
    /**
     * Validation modes
     */
    public enum ValidationMode {
        STRICT,         // All validations enabled, strict checking
        STANDARD,       // Standard validation set
        PERFORMANCE,    // Performance-optimized validation
        DEVELOPMENT,    // Development mode with extra checks
        PRODUCTION      // Production mode with optimized settings
    }
    
    /**
     * Conflict detection strategies
     */
    public enum ConflictDetectionStrategy {
        COMPREHENSIVE,  // Detect all possible conflicts
        FAST,          // Quick conflict detection
        SMART,         // Intelligent conflict detection based on rules
        MINIMAL        // Only detect critical conflicts
    }
    
    /**
     * Simulation strategies
     */
    public enum SimulationStrategy {
        FULL,          // Full rule simulation with all scenarios
        TARGETED,      // Targeted simulation of specific cases
        SAMPLE,        // Sample-based simulation
        MINIMAL,       // Minimal simulation for basic validation
        DISABLED       // No simulation
    }
    
    // Core validation features
    @JsonProperty("enableStructuralValidation")
    private final boolean enableStructuralValidation;
    
    @JsonProperty("enableConflictDetection")
    private final boolean enableConflictDetection;
    
    @JsonProperty("enableDependencyAnalysis")
    private final boolean enableDependencyAnalysis;
    
    @JsonProperty("enablePerformanceValidation")
    private final boolean enablePerformanceValidation;
    
    @JsonProperty("enableSimulation")
    private final boolean enableSimulation;
    
    @JsonProperty("enableSemanticValidation")
    private final boolean enableSemanticValidation;
    
    @JsonProperty("enableSecurityValidation")
    private final boolean enableSecurityValidation;
    
    // Validation modes and strategies
    @JsonProperty("validationMode")
    private final ValidationMode validationMode;
    
    @JsonProperty("conflictDetectionStrategy")
    private final ConflictDetectionStrategy conflictDetectionStrategy;
    
    @JsonProperty("simulationStrategy")
    private final SimulationStrategy simulationStrategy;
    
    // Performance settings
    @JsonProperty("maxValidationTimeMs")
    private final long maxValidationTimeMs;
    
    @JsonProperty("maxRuleSetSize")
    private final int maxRuleSetSize;
    
    @JsonProperty("maxDependencyDepth")
    private final int maxDependencyDepth;
    
    @JsonProperty("maxConflictsToDetect")
    private final int maxConflictsToDetect;
    
    @JsonProperty("enableParallelValidation")
    private final boolean enableParallelValidation;
    
    @JsonProperty("validationThreadPoolSize")
    private final int validationThreadPoolSize;
    
    // Validation thresholds
    @JsonProperty("minimumConfidenceThreshold")
    private final double minimumConfidenceThreshold;
    
    @JsonProperty("performanceWarningThresholdMs")
    private final long performanceWarningThresholdMs;
    
    @JsonProperty("complexityWarningThreshold")
    private final double complexityWarningThreshold;
    
    @JsonProperty("maximumTimeoutMs")
    private final long maximumTimeoutMs;
    
    @JsonProperty("minimumTimeoutMs")
    private final long minimumTimeoutMs;
    
    // Simulation settings
    @JsonProperty("simulationSampleSize")
    private final int simulationSampleSize;
    
    @JsonProperty("generateTestData")
    private final boolean generateTestData;
    
    @JsonProperty("maxSimulationTimeMs")
    private final long maxSimulationTimeMs;
    
    @JsonProperty("simulationRetryCount")
    private final int simulationRetryCount;
    
    // Caching settings
    @JsonProperty("enableValidationCache")
    private final boolean enableValidationCache;
    
    @JsonProperty("cacheExpirationTimeMs")
    private final long cacheExpirationTimeMs;
    
    @JsonProperty("maxCacheSize")
    private final int maxCacheSize;
    
    // Reporting settings
    @JsonProperty("enableDetailedReporting")
    private final boolean enableDetailedReporting;
    
    @JsonProperty("includeWarningsInResults")
    private final boolean includeWarningsInResults;
    
    @JsonProperty("includeSuggestionsInResults")
    private final boolean includeSuggestionsInResults;
    
    @JsonProperty("enableMetricsCollection")
    private final boolean enableMetricsCollection;
    
    // Custom settings
    @JsonProperty("customValidators")
    private final List<String> customValidators;
    
    @JsonProperty("ignoredIssueTypes")
    private final Set<ValidationIssue.IssueType> ignoredIssueTypes;
    
    @JsonProperty("customSettings")
    private final Map<String, Object> customSettings;
    
    @JsonCreator
    private ValidationConfig(
            @JsonProperty("enableStructuralValidation") boolean enableStructuralValidation,
            @JsonProperty("enableConflictDetection") boolean enableConflictDetection,
            @JsonProperty("enableDependencyAnalysis") boolean enableDependencyAnalysis,
            @JsonProperty("enablePerformanceValidation") boolean enablePerformanceValidation,
            @JsonProperty("enableSimulation") boolean enableSimulation,
            @JsonProperty("enableSemanticValidation") boolean enableSemanticValidation,
            @JsonProperty("enableSecurityValidation") boolean enableSecurityValidation,
            @JsonProperty("validationMode") ValidationMode validationMode,
            @JsonProperty("conflictDetectionStrategy") ConflictDetectionStrategy conflictDetectionStrategy,
            @JsonProperty("simulationStrategy") SimulationStrategy simulationStrategy,
            @JsonProperty("maxValidationTimeMs") long maxValidationTimeMs,
            @JsonProperty("maxRuleSetSize") int maxRuleSetSize,
            @JsonProperty("maxDependencyDepth") int maxDependencyDepth,
            @JsonProperty("maxConflictsToDetect") int maxConflictsToDetect,
            @JsonProperty("enableParallelValidation") boolean enableParallelValidation,
            @JsonProperty("validationThreadPoolSize") int validationThreadPoolSize,
            @JsonProperty("minimumConfidenceThreshold") double minimumConfidenceThreshold,
            @JsonProperty("performanceWarningThresholdMs") long performanceWarningThresholdMs,
            @JsonProperty("complexityWarningThreshold") double complexityWarningThreshold,
            @JsonProperty("maximumTimeoutMs") long maximumTimeoutMs,
            @JsonProperty("minimumTimeoutMs") long minimumTimeoutMs,
            @JsonProperty("simulationSampleSize") int simulationSampleSize,
            @JsonProperty("generateTestData") boolean generateTestData,
            @JsonProperty("maxSimulationTimeMs") long maxSimulationTimeMs,
            @JsonProperty("simulationRetryCount") int simulationRetryCount,
            @JsonProperty("enableValidationCache") boolean enableValidationCache,
            @JsonProperty("cacheExpirationTimeMs") long cacheExpirationTimeMs,
            @JsonProperty("maxCacheSize") int maxCacheSize,
            @JsonProperty("enableDetailedReporting") boolean enableDetailedReporting,
            @JsonProperty("includeWarningsInResults") boolean includeWarningsInResults,
            @JsonProperty("includeSuggestionsInResults") boolean includeSuggestionsInResults,
            @JsonProperty("enableMetricsCollection") boolean enableMetricsCollection,
            @JsonProperty("customValidators") List<String> customValidators,
            @JsonProperty("ignoredIssueTypes") Set<ValidationIssue.IssueType> ignoredIssueTypes,
            @JsonProperty("customSettings") Map<String, Object> customSettings) {
        this.enableStructuralValidation = enableStructuralValidation;
        this.enableConflictDetection = enableConflictDetection;
        this.enableDependencyAnalysis = enableDependencyAnalysis;
        this.enablePerformanceValidation = enablePerformanceValidation;
        this.enableSimulation = enableSimulation;
        this.enableSemanticValidation = enableSemanticValidation;
        this.enableSecurityValidation = enableSecurityValidation;
        this.validationMode = validationMode;
        this.conflictDetectionStrategy = conflictDetectionStrategy;
        this.simulationStrategy = simulationStrategy;
        this.maxValidationTimeMs = maxValidationTimeMs;
        this.maxRuleSetSize = maxRuleSetSize;
        this.maxDependencyDepth = maxDependencyDepth;
        this.maxConflictsToDetect = maxConflictsToDetect;
        this.enableParallelValidation = enableParallelValidation;
        this.validationThreadPoolSize = validationThreadPoolSize;
        this.minimumConfidenceThreshold = minimumConfidenceThreshold;
        this.performanceWarningThresholdMs = performanceWarningThresholdMs;
        this.complexityWarningThreshold = complexityWarningThreshold;
        this.maximumTimeoutMs = maximumTimeoutMs;
        this.minimumTimeoutMs = minimumTimeoutMs;
        this.simulationSampleSize = simulationSampleSize;
        this.generateTestData = generateTestData;
        this.maxSimulationTimeMs = maxSimulationTimeMs;
        this.simulationRetryCount = simulationRetryCount;
        this.enableValidationCache = enableValidationCache;
        this.cacheExpirationTimeMs = cacheExpirationTimeMs;
        this.maxCacheSize = maxCacheSize;
        this.enableDetailedReporting = enableDetailedReporting;
        this.includeWarningsInResults = includeWarningsInResults;
        this.includeSuggestionsInResults = includeSuggestionsInResults;
        this.enableMetricsCollection = enableMetricsCollection;
        this.customValidators = customValidators != null ? new ArrayList<>(customValidators) : new ArrayList<>();
        this.ignoredIssueTypes = ignoredIssueTypes != null ? new HashSet<>(ignoredIssueTypes) : new HashSet<>();
        this.customSettings = customSettings != null ? new HashMap<>(customSettings) : new HashMap<>();
    }
    
    // Getters
    public boolean isStructuralValidationEnabled() { return enableStructuralValidation; }
    public boolean isConflictDetectionEnabled() { return enableConflictDetection; }
    public boolean isDependencyAnalysisEnabled() { return enableDependencyAnalysis; }
    public boolean isPerformanceValidationEnabled() { return enablePerformanceValidation; }
    public boolean isSimulationEnabled() { return enableSimulation; }
    public boolean isSemanticValidationEnabled() { return enableSemanticValidation; }
    public boolean isSecurityValidationEnabled() { return enableSecurityValidation; }
    public ValidationMode getValidationMode() { return validationMode; }
    public ConflictDetectionStrategy getConflictDetectionStrategy() { return conflictDetectionStrategy; }
    public SimulationStrategy getSimulationStrategy() { return simulationStrategy; }
    public long getMaxValidationTimeMs() { return maxValidationTimeMs; }
    public int getMaxRuleSetSize() { return maxRuleSetSize; }
    public int getMaxDependencyDepth() { return maxDependencyDepth; }
    public int getMaxConflictsToDetect() { return maxConflictsToDetect; }
    public boolean isParallelValidationEnabled() { return enableParallelValidation; }
    public int getValidationThreadPoolSize() { return validationThreadPoolSize; }
    public double getMinimumConfidenceThreshold() { return minimumConfidenceThreshold; }
    public long getPerformanceWarningThresholdMs() { return performanceWarningThresholdMs; }
    public double getComplexityWarningThreshold() { return complexityWarningThreshold; }
    public long getMaximumTimeoutMs() { return maximumTimeoutMs; }
    public long getMinimumTimeoutMs() { return minimumTimeoutMs; }
    public int getSimulationSampleSize() { return simulationSampleSize; }
    public boolean isGenerateTestData() { return generateTestData; }
    public long getMaxSimulationTimeMs() { return maxSimulationTimeMs; }
    public int getSimulationRetryCount() { return simulationRetryCount; }
    public boolean isValidationCacheEnabled() { return enableValidationCache; }
    public long getCacheExpirationTimeMs() { return cacheExpirationTimeMs; }
    public int getMaxCacheSize() { return maxCacheSize; }
    public boolean isDetailedReportingEnabled() { return enableDetailedReporting; }
    public boolean isIncludeWarningsInResults() { return includeWarningsInResults; }
    public boolean isIncludeSuggestionsInResults() { return includeSuggestionsInResults; }
    public boolean isMetricsCollectionEnabled() { return enableMetricsCollection; }
    public List<String> getCustomValidators() { return new ArrayList<>(customValidators); }
    public Set<ValidationIssue.IssueType> getIgnoredIssueTypes() { return new HashSet<>(ignoredIssueTypes); }
    public Map<String, Object> getCustomSettings() { return new HashMap<>(customSettings); }
    
    /**
     * Check if an issue type should be ignored
     */
    public boolean shouldIgnoreIssueType(ValidationIssue.IssueType issueType) {
        return ignoredIssueTypes.contains(issueType);
    }
    
    /**
     * Get custom setting value
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomSetting(String key, Class<T> type, T defaultValue) {
        Object value = customSettings.get(key);
        if (value == null) return defaultValue;
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        // Handle common type conversions
        if (type == String.class) {
            return (T) value.toString();
        } else if (type == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        } else if (type == Long.class && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        } else if (type == Double.class && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        } else if (type == Boolean.class) {
            if (value instanceof Boolean) {
                return (T) value;
            } else if (value instanceof String) {
                return (T) Boolean.valueOf((String) value);
            }
        }
        
        return defaultValue;
    }
    
    /**
     * Check if validation is comprehensive
     */
    public boolean isComprehensiveValidation() {
        return validationMode == ValidationMode.STRICT ||
               validationMode == ValidationMode.DEVELOPMENT;
    }
    
    /**
     * Check if validation is performance-optimized
     */
    public boolean isPerformanceOptimized() {
        return validationMode == ValidationMode.PERFORMANCE ||
               validationMode == ValidationMode.PRODUCTION;
    }
    
    /**
     * Get effective thread pool size
     */
    public int getEffectiveThreadPoolSize() {
        if (!enableParallelValidation) {
            return 1;
        }
        return validationThreadPoolSize > 0 ? validationThreadPoolSize : 
               Runtime.getRuntime().availableProcessors();
    }
    
    /**
     * Builder pattern for creating ValidationConfig instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        // Core validation features
        private boolean enableStructuralValidation = true;
        private boolean enableConflictDetection = true;
        private boolean enableDependencyAnalysis = true;
        private boolean enablePerformanceValidation = true;
        private boolean enableSimulation = true;
        private boolean enableSemanticValidation = false;
        private boolean enableSecurityValidation = false;
        
        // Validation modes and strategies
        private ValidationMode validationMode = ValidationMode.STANDARD;
        private ConflictDetectionStrategy conflictDetectionStrategy = ConflictDetectionStrategy.SMART;
        private SimulationStrategy simulationStrategy = SimulationStrategy.TARGETED;
        
        // Performance settings
        private long maxValidationTimeMs = 30000L; // 30 seconds
        private int maxRuleSetSize = 1000;
        private int maxDependencyDepth = 10;
        private int maxConflictsToDetect = 100;
        private boolean enableParallelValidation = true;
        private int validationThreadPoolSize = 0; // Auto-detect
        
        // Validation thresholds
        private double minimumConfidenceThreshold = 0.7;
        private long performanceWarningThresholdMs = 1000L;
        private double complexityWarningThreshold = 0.8;
        private long maximumTimeoutMs = 300000L; // 5 minutes
        private long minimumTimeoutMs = 100L;
        
        // Simulation settings
        private int simulationSampleSize = 10;
        private boolean generateTestData = true;
        private long maxSimulationTimeMs = 10000L; // 10 seconds
        private int simulationRetryCount = 3;
        
        // Caching settings
        private boolean enableValidationCache = true;
        private long cacheExpirationTimeMs = 3600000L; // 1 hour
        private int maxCacheSize = 1000;
        
        // Reporting settings
        private boolean enableDetailedReporting = true;
        private boolean includeWarningsInResults = true;
        private boolean includeSuggestionsInResults = true;
        private boolean enableMetricsCollection = true;
        
        // Custom settings
        private List<String> customValidators = new ArrayList<>();
        private Set<ValidationIssue.IssueType> ignoredIssueTypes = new HashSet<>();
        private Map<String, Object> customSettings = new HashMap<>();
        
        public Builder enableStructuralValidation(boolean enable) {
            this.enableStructuralValidation = enable;
            return this;
        }
        
        public Builder enableConflictDetection(boolean enable) {
            this.enableConflictDetection = enable;
            return this;
        }
        
        public Builder enableDependencyAnalysis(boolean enable) {
            this.enableDependencyAnalysis = enable;
            return this;
        }
        
        public Builder enablePerformanceValidation(boolean enable) {
            this.enablePerformanceValidation = enable;
            return this;
        }
        
        public Builder enableSimulation(boolean enable) {
            this.enableSimulation = enable;
            return this;
        }
        
        public Builder enableSemanticValidation(boolean enable) {
            this.enableSemanticValidation = enable;
            return this;
        }
        
        public Builder enableSecurityValidation(boolean enable) {
            this.enableSecurityValidation = enable;
            return this;
        }
        
        public Builder validationMode(ValidationMode mode) {
            this.validationMode = mode;
            applyModeDefaults(mode);
            return this;
        }
        
        public Builder conflictDetectionStrategy(ConflictDetectionStrategy strategy) {
            this.conflictDetectionStrategy = strategy;
            return this;
        }
        
        public Builder simulationStrategy(SimulationStrategy strategy) {
            this.simulationStrategy = strategy;
            return this;
        }
        
        public Builder maxValidationTimeMs(long timeMs) {
            this.maxValidationTimeMs = timeMs;
            return this;
        }
        
        public Builder maxRuleSetSize(int size) {
            this.maxRuleSetSize = size;
            return this;
        }
        
        public Builder maxDependencyDepth(int depth) {
            this.maxDependencyDepth = depth;
            return this;
        }
        
        public Builder maxConflictsToDetect(int maxConflicts) {
            this.maxConflictsToDetect = maxConflicts;
            return this;
        }
        
        public Builder enableParallelValidation(boolean enable) {
            this.enableParallelValidation = enable;
            return this;
        }
        
        public Builder validationThreadPoolSize(int size) {
            this.validationThreadPoolSize = size;
            return this;
        }
        
        public Builder minimumConfidenceThreshold(double threshold) {
            this.minimumConfidenceThreshold = threshold;
            return this;
        }
        
        public Builder performanceWarningThresholdMs(long thresholdMs) {
            this.performanceWarningThresholdMs = thresholdMs;
            return this;
        }
        
        public Builder complexityWarningThreshold(double threshold) {
            this.complexityWarningThreshold = threshold;
            return this;
        }
        
        public Builder maximumTimeoutMs(long timeoutMs) {
            this.maximumTimeoutMs = timeoutMs;
            return this;
        }
        
        public Builder minimumTimeoutMs(long timeoutMs) {
            this.minimumTimeoutMs = timeoutMs;
            return this;
        }
        
        public Builder simulationSampleSize(int size) {
            this.simulationSampleSize = size;
            return this;
        }
        
        public Builder generateTestData(boolean generate) {
            this.generateTestData = generate;
            return this;
        }
        
        public Builder maxSimulationTimeMs(long timeMs) {
            this.maxSimulationTimeMs = timeMs;
            return this;
        }
        
        public Builder simulationRetryCount(int retryCount) {
            this.simulationRetryCount = retryCount;
            return this;
        }
        
        public Builder enableValidationCache(boolean enable) {
            this.enableValidationCache = enable;
            return this;
        }
        
        public Builder cacheExpirationTimeMs(long timeMs) {
            this.cacheExpirationTimeMs = timeMs;
            return this;
        }
        
        public Builder maxCacheSize(int size) {
            this.maxCacheSize = size;
            return this;
        }
        
        public Builder enableDetailedReporting(boolean enable) {
            this.enableDetailedReporting = enable;
            return this;
        }
        
        public Builder includeWarningsInResults(boolean include) {
            this.includeWarningsInResults = include;
            return this;
        }
        
        public Builder includeSuggestionsInResults(boolean include) {
            this.includeSuggestionsInResults = include;
            return this;
        }
        
        public Builder enableMetricsCollection(boolean enable) {
            this.enableMetricsCollection = enable;
            return this;
        }
        
        public Builder addCustomValidator(String validatorClass) {
            this.customValidators.add(validatorClass);
            return this;
        }
        
        public Builder customValidators(List<String> validators) {
            this.customValidators = new ArrayList<>(validators);
            return this;
        }
        
        public Builder ignoreIssueType(ValidationIssue.IssueType issueType) {
            this.ignoredIssueTypes.add(issueType);
            return this;
        }
        
        public Builder ignoredIssueTypes(Set<ValidationIssue.IssueType> issueTypes) {
            this.ignoredIssueTypes = new HashSet<>(issueTypes);
            return this;
        }
        
        public Builder addCustomSetting(String key, Object value) {
            this.customSettings.put(key, value);
            return this;
        }
        
        public Builder customSettings(Map<String, Object> settings) {
            this.customSettings = new HashMap<>(settings);
            return this;
        }
        
        private void applyModeDefaults(ValidationMode mode) {
            switch (mode) {
                case STRICT:
                    enableStructuralValidation = true;
                    enableConflictDetection = true;
                    enableDependencyAnalysis = true;
                    enablePerformanceValidation = true;
                    enableSimulation = true;
                    enableSemanticValidation = true;
                    enableSecurityValidation = true;
                    conflictDetectionStrategy = ConflictDetectionStrategy.COMPREHENSIVE;
                    simulationStrategy = SimulationStrategy.FULL;
                    break;
                case PERFORMANCE:
                    conflictDetectionStrategy = ConflictDetectionStrategy.FAST;
                    simulationStrategy = SimulationStrategy.MINIMAL;
                    enableParallelValidation = true;
                    maxValidationTimeMs = 10000L; // 10 seconds
                    break;
                case DEVELOPMENT:
                    enableDetailedReporting = true;
                    includeWarningsInResults = true;
                    includeSuggestionsInResults = true;
                    enableMetricsCollection = true;
                    break;
                case PRODUCTION:
                    enableParallelValidation = true;
                    enableValidationCache = true;
                    conflictDetectionStrategy = ConflictDetectionStrategy.SMART;
                    simulationStrategy = SimulationStrategy.TARGETED;
                    break;
                case STANDARD:
                    // Use default settings
                    break;
            }
        }
        
        public ValidationConfig build() {
            validate();
            return new ValidationConfig(
                    enableStructuralValidation, enableConflictDetection, enableDependencyAnalysis,
                    enablePerformanceValidation, enableSimulation, enableSemanticValidation,
                    enableSecurityValidation, validationMode, conflictDetectionStrategy,
                    simulationStrategy, maxValidationTimeMs, maxRuleSetSize, maxDependencyDepth,
                    maxConflictsToDetect, enableParallelValidation, validationThreadPoolSize,
                    minimumConfidenceThreshold, performanceWarningThresholdMs, complexityWarningThreshold,
                    maximumTimeoutMs, minimumTimeoutMs, simulationSampleSize, generateTestData,
                    maxSimulationTimeMs, simulationRetryCount, enableValidationCache,
                    cacheExpirationTimeMs, maxCacheSize, enableDetailedReporting,
                    includeWarningsInResults, includeSuggestionsInResults, enableMetricsCollection,
                    customValidators, ignoredIssueTypes, customSettings);
        }
        
        private void validate() {
            if (maxValidationTimeMs <= 0) {
                throw new IllegalArgumentException("Max validation time must be positive");
            }
            if (maxRuleSetSize <= 0) {
                throw new IllegalArgumentException("Max rule set size must be positive");
            }
            if (maxDependencyDepth <= 0) {
                throw new IllegalArgumentException("Max dependency depth must be positive");
            }
            if (minimumConfidenceThreshold < 0.0 || minimumConfidenceThreshold > 1.0) {
                throw new IllegalArgumentException("Minimum confidence threshold must be between 0.0 and 1.0");
            }
            if (minimumTimeoutMs > maximumTimeoutMs) {
                throw new IllegalArgumentException("Minimum timeout cannot be greater than maximum timeout");
            }
            if (simulationSampleSize <= 0) {
                throw new IllegalArgumentException("Simulation sample size must be positive");
            }
        }
    }
    
    /**
     * Predefined configurations for common use cases
     */
    public static class Presets {
        
        public static ValidationConfig strict() {
            return builder()
                    .validationMode(ValidationMode.STRICT)
                    .build();
        }
        
        public static ValidationConfig standard() {
            return builder()
                    .validationMode(ValidationMode.STANDARD)
                    .build();
        }
        
        public static ValidationConfig performance() {
            return builder()
                    .validationMode(ValidationMode.PERFORMANCE)
                    .build();
        }
        
        public static ValidationConfig development() {
            return builder()
                    .validationMode(ValidationMode.DEVELOPMENT)
                    .build();
        }
        
        public static ValidationConfig production() {
            return builder()
                    .validationMode(ValidationMode.PRODUCTION)
                    .build();
        }
        
        public static ValidationConfig minimal() {
            return builder()
                    .enableStructuralValidation(true)
                    .enableConflictDetection(false)
                    .enableDependencyAnalysis(false)
                    .enablePerformanceValidation(false)
                    .enableSimulation(false)
                    .maxValidationTimeMs(5000L)
                    .build();
        }
        
        public static ValidationConfig testing() {
            return builder()
                    .enableSimulation(true)
                    .simulationStrategy(SimulationStrategy.FULL)
                    .generateTestData(true)
                    .simulationSampleSize(20)
                    .enableDetailedReporting(true)
                    .build();
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationConfig that = (ValidationConfig) o;
        return enableStructuralValidation == that.enableStructuralValidation &&
               enableConflictDetection == that.enableConflictDetection &&
               enableDependencyAnalysis == that.enableDependencyAnalysis &&
               enablePerformanceValidation == that.enablePerformanceValidation &&
               enableSimulation == that.enableSimulation &&
               maxValidationTimeMs == that.maxValidationTimeMs &&
               maxRuleSetSize == that.maxRuleSetSize &&
               validationMode == that.validationMode &&
               conflictDetectionStrategy == that.conflictDetectionStrategy &&
               simulationStrategy == that.simulationStrategy;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enableStructuralValidation, enableConflictDetection, enableDependencyAnalysis,
                enablePerformanceValidation, enableSimulation, validationMode, conflictDetectionStrategy,
                simulationStrategy, maxValidationTimeMs, maxRuleSetSize);
    }
    
    @Override
    public String toString() {
        return "ValidationConfig{" +
                "validationMode=" + validationMode +
                ", structural=" + enableStructuralValidation +
                ", conflicts=" + enableConflictDetection +
                ", dependencies=" + enableDependencyAnalysis +
                ", performance=" + enablePerformanceValidation +
                ", simulation=" + enableSimulation +
                ", maxTimeMs=" + maxValidationTimeMs +
                ", maxRuleSetSize=" + maxRuleSetSize +
                '}';
    }
}