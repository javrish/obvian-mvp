package api.config;

import core.*;
import memory.MemoryStore;
import memory.MemoryStoreInterface;
import plugins.PluginRegistry;
import plugins.PluginRouter;
import plugins.StandardPluginRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import java.util.Map;
import java.util.HashMap;

/**
 * Minimal configuration for debugging startup issues and circular dependencies.
 * This configuration provides only the essential beans needed for the application to start.
 */
@Configuration
@Profile("minimal-startup")
@ConditionalOnProperty(name = "obvian.minimal-startup", havingValue = "true", matchIfMissing = false)
public class MinimalStartupConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    public MemoryStoreInterface memoryStore() {
        return new MemoryStore();
    }

    @Bean
    @Primary
    public PluginRegistry pluginRegistry() {
        return new PluginRegistry();
    }

    @Bean
    @Primary
    public PluginRouter pluginRouter(PluginRegistry pluginRegistry) {
        return new StandardPluginRouter(pluginRegistry);
    }

    @Bean
    @Primary
    public DagValidator dagValidator(PluginRouter pluginRouter) {
        return new DagValidator(pluginRouter);
    }

    @Bean
    @Primary
    public DagExecutor dagExecutor(PluginRouter pluginRouter) {
        return new DagExecutor(pluginRouter);
    }

    @Bean
    @Primary
    public PromptParser promptParser() {
        return new PromptParser();
    }

    @Bean
    @Primary
    public DagBuilder dagBuilder() {
        return new DagBuilder();
    }
}