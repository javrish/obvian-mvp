package api.config;

import core.*;
import memory.MemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Spring configuration for the Autonomous Suggestion Engine components
 */
@Configuration
public class SuggestionEngineConfig {
    
    /**
     * Create PatternAnalyzer bean
     */
    @Bean
    @Scope("prototype") // Create new instance for each injection
    public PatternAnalyzer patternAnalyzer(MemoryStore memoryStore) {
        return new PatternAnalyzer(memoryStore);
    }
    
    /**
     * Create OpportunityDetector bean
     */
    @Bean
    @Scope("prototype") // Create new instance for each injection
    public OpportunityDetector opportunityDetector(PatternAnalyzer patternAnalyzer, MemoryStore memoryStore) {
        return new OpportunityDetector(patternAnalyzer, memoryStore);
    }
    
    /**
     * Create SuggestionScheduler bean
     */
    @Bean
    @Scope("prototype") // Create new instance for each injection
    public SuggestionScheduler suggestionScheduler(PatternAnalyzer patternAnalyzer, 
                                                   OpportunityDetector opportunityDetector,
                                                   MemoryStore memoryStore,
                                                   ConsoleSuggestionDeliveryService deliveryService) {
        return new SuggestionScheduler(patternAnalyzer, opportunityDetector, memoryStore, deliveryService);
    }
    
    /**
     * Create ConsoleSuggestionDeliveryService bean
     */
    @Bean
    public ConsoleSuggestionDeliveryService consoleSuggestionDeliveryService() {
        return new ConsoleSuggestionDeliveryService();
    }
}