package api.config;

import core.BehavioralLearningEngine;
import core.LearningConfiguration;
import memory.MemoryStoreInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring configuration for the ContextualCommandPalette service components.
 * 
 * This configuration class provides Spring beans for the behavioral learning engine,
 * learning configuration, and related components used by the ContextualCommandPaletteService.
 * It supports configurable learning parameters through application properties.
 * 
 * Patent Alignment: Implements configuration management for Product Patent 25
 * (ContextualCommandPalette) enabling production-grade behavioral learning configuration.
 * 
 * @author Obvian Labs
 * @since Phase 26.1d
 */
@Configuration
@EnableAsync
public class CommandPaletteConfig {
    
    // Learning Configuration Properties
    @Value("${obvian.command-palette.temporal-decay-factor:0.9}")
    private double temporalDecayFactor;
    
    @Value("${obvian.command-palette.minimum-pattern-count:2}")
    private int minimumPatternCount;
    
    @Value("${obvian.command-palette.confidence-threshold:0.5}")
    private double confidenceThreshold;
    
    @Value("${obvian.command-palette.max-pattern-history:500}")
    private int maxPatternHistory;
    
    @Value("${obvian.command-palette.privacy-enabled:true}")
    private boolean privacyEnabled;
    
    @Value("${obvian.command-palette.cross-interface-learning:true}")
    private boolean crossInterfaceLearning;
    
    @Value("${obvian.command-palette.max-suggestions:10}")
    private int maxSuggestions;
    
    @Value("${obvian.command-palette.fuzzy-match-threshold:0.7}")
    private double fuzzyMatchThreshold;
    
    @Value("${obvian.command-palette.context-similarity-weight:0.3}")
    private double contextSimilarityWeight;
    
    @Value("${obvian.command-palette.frequency-weight:0.4}")
    private double frequencyWeight;
    
    @Value("${obvian.command-palette.temporal-weight:0.3}")
    private double temporalWeight;
    
    // Service Configuration Properties
    @Value("${obvian.command-palette.cache-ttl-minutes:30}")
    private int cacheTtlMinutes;
    
    @Value("${obvian.command-palette.async-learning-enabled:true}")
    private boolean asyncLearningEnabled;
    
    @Value("${obvian.command-palette.suggestion-timeout-ms:5000}")
    private long suggestionTimeoutMs;
    
    @Value("${obvian.command-palette.analytics-enabled:true}")
    private boolean analyticsEnabled;
    
    @Value("${obvian.command-palette.websocket-events-enabled:true}")
    private boolean websocketEventsEnabled;
    
    @Value("${obvian.command-palette.learning-warmup-commands:10}")
    private int learningWarmupCommands;
    
    @Value("${obvian.command-palette.data-retention-days:365}")
    private int dataRetentionDays;
    
    @Value("${obvian.command-palette.request-rate-limit:100}")
    private int requestRateLimit;
    
    @Value("${obvian.command-palette.rate-limit-window-minutes:60}")
    private int rateLimitWindowMinutes;
    
    /**
     * Create the default learning configuration bean.
     * 
     * @return Configured LearningConfiguration instance
     */
    @Bean
    @Scope("singleton")
    public LearningConfiguration defaultLearningConfiguration() {
        return LearningConfiguration.builder()
                .temporalDecayFactor(temporalDecayFactor)
                .minimumPatternCount(minimumPatternCount)
                .confidenceThreshold(confidenceThreshold)
                .maxPatternHistory(maxPatternHistory)
                .privacyEnabled(privacyEnabled)
                .crossInterfaceLearning(crossInterfaceLearning)
                .maxSuggestions(maxSuggestions)
                .fuzzyMatchThreshold(fuzzyMatchThreshold)
                .contextSimilarityWeight(contextSimilarityWeight)
                .frequencyWeight(frequencyWeight)
                .temporalWeight(temporalWeight)
                .build();
    }
    
    /**
     * Create the behavioral learning engine bean.
     * 
     * @param memoryStore The memory store for pattern persistence
     * @param learningConfiguration The learning configuration
     * @return Configured BehavioralLearningEngine instance
     */
    @Bean
    @Scope("singleton")
    public BehavioralLearningEngine behavioralLearningEngine(MemoryStoreInterface memoryStore,
                                                           LearningConfiguration learningConfiguration) {
        return new BehavioralLearningEngine(memoryStore, learningConfiguration);
    }
    
    /**
     * Create a configuration properties bean for service-level settings.
     * 
     * @return CommandPaletteProperties instance with current configuration
     */
    @Bean
    @Scope("singleton")
    public CommandPaletteProperties commandPaletteProperties() {
        return CommandPaletteProperties.builder()
                .cacheTtlMinutes(cacheTtlMinutes)
                .asyncLearningEnabled(asyncLearningEnabled)
                .suggestionTimeoutMs(suggestionTimeoutMs)
                .analyticsEnabled(analyticsEnabled)
                .websocketEventsEnabled(websocketEventsEnabled)
                .learningWarmupCommands(learningWarmupCommands)
                .dataRetentionDays(dataRetentionDays)
                .requestRateLimit(requestRateLimit)
                .rateLimitWindowMinutes(rateLimitWindowMinutes)
                .build();
    }
    
    /**
     * Configuration properties holder for command palette service settings.
     */
    public static class CommandPaletteProperties {
        
        private final int cacheTtlMinutes;
        private final boolean asyncLearningEnabled;
        private final long suggestionTimeoutMs;
        private final boolean analyticsEnabled;
        private final boolean websocketEventsEnabled;
        private final int learningWarmupCommands;
        private final int dataRetentionDays;
        private final int requestRateLimit;
        private final int rateLimitWindowMinutes;
        
        private CommandPaletteProperties(Builder builder) {
            this.cacheTtlMinutes = builder.cacheTtlMinutes;
            this.asyncLearningEnabled = builder.asyncLearningEnabled;
            this.suggestionTimeoutMs = builder.suggestionTimeoutMs;
            this.analyticsEnabled = builder.analyticsEnabled;
            this.websocketEventsEnabled = builder.websocketEventsEnabled;
            this.learningWarmupCommands = builder.learningWarmupCommands;
            this.dataRetentionDays = builder.dataRetentionDays;
            this.requestRateLimit = builder.requestRateLimit;
            this.rateLimitWindowMinutes = builder.rateLimitWindowMinutes;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        
        public int getCacheTtlMinutes() {
            return cacheTtlMinutes;
        }
        
        public boolean isAsyncLearningEnabled() {
            return asyncLearningEnabled;
        }
        
        public long getSuggestionTimeoutMs() {
            return suggestionTimeoutMs;
        }
        
        public boolean isAnalyticsEnabled() {
            return analyticsEnabled;
        }
        
        public boolean isWebsocketEventsEnabled() {
            return websocketEventsEnabled;
        }
        
        public int getLearningWarmupCommands() {
            return learningWarmupCommands;
        }
        
        public int getDataRetentionDays() {
            return dataRetentionDays;
        }
        
        public int getRequestRateLimit() {
            return requestRateLimit;
        }
        
        public int getRateLimitWindowMinutes() {
            return rateLimitWindowMinutes;
        }
        
        /**
         * Check if user has enough commands for meaningful learning.
         * 
         * @param commandCount Current command count
         * @return true if above warmup threshold
         */
        public boolean isAboveWarmupThreshold(int commandCount) {
            return commandCount >= learningWarmupCommands;
        }
        
        /**
         * Check if WebSocket events should be sent for suggestions.
         * 
         * @return true if WebSocket events are enabled
         */
        public boolean shouldSendWebSocketEvents() {
            return websocketEventsEnabled;
        }
        
        /**
         * Check if analytics data should be collected.
         * 
         * @return true if analytics are enabled
         */
        public boolean shouldCollectAnalytics() {
            return analyticsEnabled;
        }
        
        @Override
        public String toString() {
            return String.format(
                    "CommandPaletteProperties{cacheTtlMinutes=%d, asyncLearningEnabled=%s, " +
                    "suggestionTimeoutMs=%d, analyticsEnabled=%s, websocketEventsEnabled=%s, " +
                    "learningWarmupCommands=%d, dataRetentionDays=%d, requestRateLimit=%d, " +
                    "rateLimitWindowMinutes=%d}",
                    cacheTtlMinutes, asyncLearningEnabled, suggestionTimeoutMs, analyticsEnabled,
                    websocketEventsEnabled, learningWarmupCommands, dataRetentionDays,
                    requestRateLimit, rateLimitWindowMinutes
            );
        }
        
        /**
         * Builder for CommandPaletteProperties.
         */
        public static class Builder {
            private int cacheTtlMinutes = 30;
            private boolean asyncLearningEnabled = true;
            private long suggestionTimeoutMs = 5000;
            private boolean analyticsEnabled = true;
            private boolean websocketEventsEnabled = true;
            private int learningWarmupCommands = 10;
            private int dataRetentionDays = 365;
            private int requestRateLimit = 100;
            private int rateLimitWindowMinutes = 60;
            
            public Builder cacheTtlMinutes(int cacheTtlMinutes) {
                this.cacheTtlMinutes = cacheTtlMinutes;
                return this;
            }
            
            public Builder asyncLearningEnabled(boolean asyncLearningEnabled) {
                this.asyncLearningEnabled = asyncLearningEnabled;
                return this;
            }
            
            public Builder suggestionTimeoutMs(long suggestionTimeoutMs) {
                this.suggestionTimeoutMs = suggestionTimeoutMs;
                return this;
            }
            
            public Builder analyticsEnabled(boolean analyticsEnabled) {
                this.analyticsEnabled = analyticsEnabled;
                return this;
            }
            
            public Builder websocketEventsEnabled(boolean websocketEventsEnabled) {
                this.websocketEventsEnabled = websocketEventsEnabled;
                return this;
            }
            
            public Builder learningWarmupCommands(int learningWarmupCommands) {
                this.learningWarmupCommands = learningWarmupCommands;
                return this;
            }
            
            public Builder dataRetentionDays(int dataRetentionDays) {
                this.dataRetentionDays = dataRetentionDays;
                return this;
            }
            
            public Builder requestRateLimit(int requestRateLimit) {
                this.requestRateLimit = requestRateLimit;
                return this;
            }
            
            public Builder rateLimitWindowMinutes(int rateLimitWindowMinutes) {
                this.rateLimitWindowMinutes = rateLimitWindowMinutes;
                return this;
            }
            
            public CommandPaletteProperties build() {
                return new CommandPaletteProperties(this);
            }
        }
    }
}