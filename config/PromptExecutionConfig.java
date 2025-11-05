package api.config;

import core.MarkdownDagRenderer;
import core.BidirectionalDagParser;
import core.LiveDagPreview;
import core.DagValidator;
import core.explainability.CausalTraceLogger;
import memory.MemoryStore;
import memory.CausalGraphConstruction;
import memory.TemporalConstraintProcessor;
import memory.DynamicCausalModelUpdates;
import plugins.PluginRegistry;
import plugins.PluginRouter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for prompt execution components.
 * Plugin configuration has been moved to PluginConfig.java for centralized management.
 */
@Configuration
public class PromptExecutionConfig {
    
    // MemoryStore bean is now provided by CoreConfig
    // PluginRegistry bean is now provided by PluginConfig
    
    @Bean
    public CausalGraphConstruction causalGraphConstruction() {
        return new CausalGraphConstruction();
    }
    
    @Bean
    public TemporalConstraintProcessor temporalConstraintProcessor() {
        return new TemporalConstraintProcessor();
    }
    
    @Bean
    public DynamicCausalModelUpdates dynamicCausalModelUpdates() {
        return new DynamicCausalModelUpdates();
    }
    
    @Bean
    public MarkdownDagRenderer markdownDagRenderer(
            CausalGraphConstruction causalGraphConstruction,
            TemporalConstraintProcessor temporalConstraintProcessor,
            DynamicCausalModelUpdates dynamicCausalModelUpdates) {
        return new MarkdownDagRenderer(causalGraphConstruction, temporalConstraintProcessor, dynamicCausalModelUpdates);
    }
    
    @Bean
    public BidirectionalDagParser bidirectionalDagParser(
            CausalGraphConstruction causalGraphConstruction,
            TemporalConstraintProcessor temporalConstraintProcessor,
            DynamicCausalModelUpdates dynamicCausalModelUpdates) {
        return new BidirectionalDagParser(causalGraphConstruction, temporalConstraintProcessor, dynamicCausalModelUpdates);
    }
    
    // CausalTraceLogger bean is now provided by CoreConfig
    
    @Bean
    public LiveDagPreview liveDagPreview(
            MemoryStore memoryStore,
            CausalTraceLogger causalTraceLogger,
            CausalGraphConstruction causalGraphConstruction,
            DynamicCausalModelUpdates dynamicCausalModelUpdates,
            BidirectionalDagParser dagParser,
            MarkdownDagRenderer dagRenderer) {
        return new LiveDagPreview(memoryStore, causalTraceLogger, causalGraphConstruction, 
                                dynamicCausalModelUpdates, dagParser, dagRenderer);
    }
    
    // DagValidator bean is now provided by CoreConfig
}