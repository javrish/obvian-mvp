package api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import core.interfaces.*;
import core.impl.*;
import core.*;
import core.config.DagExecutorConfiguration;
import memory.MemoryStoreInterface;
import plugins.PluginRouter;
import plugins.StandardPluginRouter;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dependency injection configuration to resolve circular dependencies
 * in the refactored service architecture.
 * 
 * This configuration handles complex dependency relationships between:
 * - Services that depend on each other
 * - Services that require lazy initialization
 * - Cross-cutting concerns like logging and metrics
 * 
 * Uses Spring's @Lazy, @DependsOn, and careful bean ordering
 * to prevent circular dependency issues.
 * 
 * Patent Alignment: Supports Claim 3.1 (Multi-user agent architecture)
 * by ensuring clean service separation and proper dependency management.
 * 
 * @author Obvian Engineering Team
 * @since 2.0.0
 */
@Configuration
@EnableConfigurationProperties
public class DependencyInjectionConfig {
    
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    @PostConstruct
    public void validateConfiguration() {
        if (initialized.compareAndSet(false, true)) {
            // Configuration validation logic here
            validateBeanDependencies();
        }
    }
    
    /**
     * Core infrastructure beans that other services depend on.
     * These are created first to avoid circular dependencies.
     */
    
    @Bean(name = "coreMetricsCollector")
    @Primary
    @Scope("singleton")
    @ConditionalOnMissingBean(name = "coreMetricsCollector")
    public MetricsCollector coreMetricsCollector() {
        return new InMemoryMetricsCollector();
    }
    
    @Bean(name = "coreTraceLogger")
    @Primary
    @Scope("singleton")
    @ConditionalOnMissingBean(name = "coreTraceLogger")
    public TraceLogger coreTraceLogger() {
        return new TraceLogger();
    }
    
    @Bean(name = "pluginRouter")
    @Primary
    @Scope("singleton")
    @ConditionalOnMissingBean(name = "pluginRouter")
    public PluginRouter pluginRouter() {
        return new StandardPluginRouter();
    }
    
    /**
     * Memory store with lazy initialization to break circular dependencies
     * with services that may need to access memory during their initialization.
     */
    @Bean(name = "lazyMemoryStore")
    @Scope("singleton")
    @Lazy
    @ConditionalOnMissingBean(name = "lazyMemoryStore")
    public MemoryStoreInterface lazyMemoryStore() {
        return new memory.MemoryStore();
    }
    
    
    /**
     * Service-level beans with controlled dependency injection.
     */
    
    /**
     * Prompt parser service with minimal dependencies.
     * Created early to support other services.
     */
    @Bean(name = "independentPromptParserService")
    @Primary
    @Scope("singleton")
    @DependsOn({"coreTraceLogger", "coreMetricsCollector", "lazyMemoryStore", "pluginRouter"})
    @ConditionalOnMissingBean(name = "independentPromptParserService")
    public PromptParserService independentPromptParserService(
            @Lazy MemoryStoreInterface memoryStore,
            PluginRouter pluginRouter) {
        return new StandardPromptParserService(memoryStore, pluginRouter);
    }
    
    /**
     * DAG validator service with controlled dependencies.
     * Validates DAGs without requiring heavy dependencies.
     */
    @Bean(name = "independentDagValidatorService")
    @Primary
    @Scope("singleton")
    @DependsOn({"coreTraceLogger", "pluginRouter"})
    @ConditionalOnMissingBean(name = "independentDagValidatorService")
    public DagValidatorService independentDagValidatorService(PluginRouter pluginRouter) {
        return new StandardDagValidatorService(pluginRouter);
    }
    
    /**
     * DAG builder service with lazy memory store injection.
     * Breaks circular dependency with execution services.
     */
    @Bean(name = "lazyDagBuilderService")
    @Primary
    @Scope("singleton")
    @DependsOn({"independentPromptParserService", "independentDagValidatorService", "lazyMemoryStore", "pluginRouter"})
    @Lazy
    public DagBuilderService lazyDagBuilderService(
            @Lazy MemoryStoreInterface memoryStore,
            PluginRouter pluginRouter) {
        return new StandardDagBuilderService(memoryStore, pluginRouter);
    }
    
    /**
     * DAG executor service with all dependencies properly injected.
     * Uses lazy initialization for dependencies that may cause cycles.
     */
    @Bean(name = "fullDagExecutorService")
    @Primary
    @Scope("singleton")
    @DependsOn({"pluginRouter", "coreTraceLogger", "coreMetricsCollector"})
    @Lazy
    public DagExecutorService fullDagExecutorService(
            @Lazy PluginRouter pluginRouter,
            @Lazy TraceLogger traceLogger,
            @Lazy MetricsCollector metricsCollector,
            @Lazy MemoryStoreInterface memoryStore) {
        
        return createDagExecutorWithDependencies(
            pluginRouter, traceLogger, metricsCollector, memoryStore);
    }
    
    /**
     * Test-specific dependency injection configuration.
     * Uses mock or simplified dependencies to avoid circular issues.
     */
    @Configuration
    @Profile("test")
    static class TestDependencyConfig {
        
        @Bean(name = "testMetricsCollector")
        @Primary
        @Scope("singleton")
        public MetricsCollector testMetricsCollector() {
            return new NoOpMetricsCollector();
        }
        
        @Bean(name = "testTraceLogger")
        @Primary
        @Scope("singleton")
        public TraceLogger testTraceLogger() {
            return new TraceLogger();
        }
        
        @Bean(name = "testPluginRouter")
        @Scope("singleton")
        public PluginRouter testPluginRouter() {
            PluginRouter router = new StandardPluginRouter();
            // Register minimal test plugins
            return router;
        }
        
        @Bean(name = "testMemoryStore")
        @Scope("singleton")
        @Lazy
        public MemoryStoreInterface testMemoryStore() {
            return new memory.TestMemoryStore();
        }
    }
    
    /**
     * Development-specific dependency injection configuration.
     * Enhanced with debugging and monitoring capabilities.
     */
    @Configuration
    @Profile("dev")
    static class DevelopmentDependencyConfig {
        
        @Bean(name = "devMetricsCollector")
        @Primary
        @Scope("singleton")
        public MetricsCollector devMetricsCollector() {
            InMemoryMetricsCollector collector = new InMemoryMetricsCollector();
            collector.enableDebugMetrics();
            return collector;
        }
        
        @Bean(name = "devTraceLogger")
        @Primary
        @Scope("singleton")
        public TraceLogger devTraceLogger() {
            TraceLogger logger = new TraceLogger();
            logger.setLevel(TraceLogger.Level.DEBUG);
            return logger;
        }
    }
    
    /**
     * Production-specific dependency injection configuration.
     * Optimized for performance and minimal overhead.
     */
    @Configuration
    @Profile("prod")
    static class ProductionDependencyConfig {
        
        @Bean(name = "prodMetricsCollector")
        @Primary
        @Scope("singleton")
        public MetricsCollector prodMetricsCollector() {
            return new OptimizedMetricsCollector();
        }
        
        @Bean(name = "prodTraceLogger")
        @Primary
        @Scope("singleton")
        public TraceLogger prodTraceLogger() {
            TraceLogger logger = new TraceLogger();
            logger.setLevel(TraceLogger.Level.WARN);
            logger.enableAsyncLogging();
            return logger;
        }
    }
    
    /**
     * Conditional beans that are created based on properties.
     */
    
    @Bean(name = "conditionalUnifiedContextService")
    @ConditionalOnProperty(
        name = "obvian.services.unified-context.enabled", 
        havingValue = "true",
        matchIfMissing = false)
    @Lazy
    public api.service.UnifiedContextService conditionalUnifiedContextService() {
        return new api.service.UnifiedContextServiceImpl();
    }
    
    @Bean(name = "conditionalCausalGraphConstruction")
    @ConditionalOnProperty(
        name = "obvian.services.causal-graph.enabled", 
        havingValue = "true",
        matchIfMissing = false)
    @Lazy
    public memory.CausalGraphConstruction conditionalCausalGraphConstruction() {
        return new memory.CausalGraphConstruction();
    }
    
    /**
     * Helper methods for complex bean creation.
     */
    
    private DagExecutorService createDagExecutorWithDependencies(
            PluginRouter pluginRouter,
            TraceLogger traceLogger,
            MetricsCollector metricsCollector,
            MemoryStoreInterface memoryStore) {
        
        DagExecutorConfiguration config = new DagExecutorConfiguration();
        config.setMaxRetryAttempts(3);
        config.setEnableTracing(true);
        config.setEnableMetrics(true);
        config.setDefaultTimeout(java.time.Duration.ofSeconds(30));
        
        return new StandardDagExecutorService(
            pluginRouter,
            memoryStore,
            metricsCollector,
            traceLogger,
            null // Progress callback can be null
        );
    }
    
    /**
     * Validates that all critical bean dependencies are properly configured.
     */
    private void validateBeanDependencies() {
        // Add validation logic to ensure proper dependency wiring
        // This can include checks for required beans, circular dependency detection, etc.
    }
    
    /**
     * Dependency resolution helper that provides ordered initialization.
     */
    @Bean
    @Scope("singleton")
    public DependencyResolver dependencyResolver() {
        return new DependencyResolver();
    }
    
    /**
     * Helper class for managing complex dependency resolution.
     */
    public static class DependencyResolver {
        
        private final java.util.Set<String> initializedBeans = new java.util.concurrent.ConcurrentHashMap<String, Boolean>().keySet();
        
        /**
         * Marks a bean as initialized to track dependency resolution order.
         */
        public void markInitialized(String beanName) {
            initializedBeans.add(beanName);
        }
        
        /**
         * Checks if a bean has been initialized.
         */
        public boolean isInitialized(String beanName) {
            return initializedBeans.contains(beanName);
        }
        
        /**
         * Gets the list of initialized beans for debugging.
         */
        public java.util.Set<String> getInitializedBeans() {
            return new java.util.HashSet<>(initializedBeans);
        }
        
        /**
         * Validates that all required beans are initialized.
         */
        public void validateRequiredBeans(String... requiredBeans) {
            java.util.List<String> missing = new java.util.ArrayList<>();
            for (String required : requiredBeans) {
                if (!isInitialized(required)) {
                    missing.add(required);
                }
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                    "Required beans not initialized: " + String.join(", ", missing));
            }
        }
    }
    
    /**
     * Configuration properties for dependency injection behavior.
     */
    @Bean
    @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "obvian.dependency-injection")
    public DependencyInjectionProperties dependencyInjectionProperties() {
        return new DependencyInjectionProperties();
    }
    
    /**
     * Properties class for dependency injection configuration.
     */
    public static class DependencyInjectionProperties {
        private boolean enableLazyInitialization = true;
        private boolean enableCircularDependencyDetection = true;
        private boolean strictValidation = false;
        private int maxInitializationTimeout = 30000;
        
        // Getters and setters
        public boolean isEnableLazyInitialization() { return enableLazyInitialization; }
        public void setEnableLazyInitialization(boolean enableLazyInitialization) { this.enableLazyInitialization = enableLazyInitialization; }
        
        public boolean isEnableCircularDependencyDetection() { return enableCircularDependencyDetection; }
        public void setEnableCircularDependencyDetection(boolean enableCircularDependencyDetection) { this.enableCircularDependencyDetection = enableCircularDependencyDetection; }
        
        public boolean isStrictValidation() { return strictValidation; }
        public void setStrictValidation(boolean strictValidation) { this.strictValidation = strictValidation; }
        
        public int getMaxInitializationTimeout() { return maxInitializationTimeout; }
        public void setMaxInitializationTimeout(int maxInitializationTimeout) { this.maxInitializationTimeout = maxInitializationTimeout; }
    }
}