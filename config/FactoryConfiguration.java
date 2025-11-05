package api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// Core ServiceFactory fully qualified to avoid collision
import core.factory.DagExecutionServiceFactory;
import core.factory.PromptParserServiceFactory;
import core.factory.DagBuilderServiceFactory;
import core.factory.DagValidatorServiceFactory;
import core.interfaces.*;
import plugins.PluginRouter;
import memory.MemoryStoreInterface;
import core.MetricsCollector;
import core.TraceLogger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring Boot configuration for service factories.
 * 
 * Provides a compatibility layer and enhanced configuration for the refactored
 * ServiceFactory architecture. Creates factory wrapper beans that adapt the
 * core ServiceFactory to provide profile-specific configurations.
 * 
 * Patent Alignment: Supports Claim 1.3 (DAG construction) and
 * Claim 3.1 (Multi-user agent architecture) through factory pattern.
 * 
 * @author Obvian Engineering Team
 * @since 2.0.0
 */
@Configuration
@EnableConfigurationProperties
public class FactoryConfiguration {
    
    /**
     * Enhanced service factory wrapper that provides profile-specific configurations
     * and compatibility API for the refactored architecture.
     */
    @Bean(name = "serviceFactory")
    @Primary
    @Scope("singleton")
    public ServiceFactory serviceFactory(core.factory.ServiceFactory coreServiceFactory,
                                        DagExecutionServiceFactory dagExecutionServiceFactory) {
        return new ServiceFactory(coreServiceFactory, dagExecutionServiceFactory);
    }
    
    /**
     * DAG execution service factory bean.
     * Handles creation of various DAG executor implementations.
     */
    @Bean
    @Scope("singleton")
    public DagExecutionServiceFactory dagExecutionServiceFactory() {
        return DagExecutionServiceFactory.getInstance();
    }
    
    /**
     * Prompt parser service factory bean.
     * Creates prompt parsing service instances with different strategies.
     */
    @Bean
    @Scope("singleton")
    public PromptParserServiceFactory promptParserServiceFactory() {
        return new PromptParserServiceFactory();
    }
    
    /**
     * DAG builder service factory bean.
     * Creates DAG building service instances for different use cases.
     */
    @Bean
    @Scope("singleton")
    public DagBuilderServiceFactory dagBuilderServiceFactory() {
        return new DagBuilderServiceFactory();
    }
    
    /**
     * DAG validator service factory bean.
     * Creates validation service instances with configurable rules.
     */
    @Bean
    @Scope("singleton")
    public DagValidatorServiceFactory dagValidatorServiceFactory() {
        return new DagValidatorServiceFactory();
    }
    
    /**
     * Test-friendly service factory for integration tests.
     * Provides mocked or simplified service instances.
     */
    @Bean
    @Primary
    @Scope("singleton")
    @Profile("test")
    public ServiceFactory testServiceFactory(core.factory.ServiceFactory coreServiceFactory,
                                            DagExecutionServiceFactory dagExecutionServiceFactory) {
        ServiceFactory factory = new ServiceFactory(coreServiceFactory, dagExecutionServiceFactory);
        // Configure test-specific settings
        return factory;
    }
    
    /**
     * Development-specific service factory with enhanced debugging.
     */
    @Bean
    @Primary
    @Scope("singleton")
    @Profile("dev")
    public ServiceFactory developmentServiceFactory(core.factory.ServiceFactory coreServiceFactory,
                                                  DagExecutionServiceFactory dagExecutionServiceFactory) {
        ServiceFactory factory = new ServiceFactory(coreServiceFactory, dagExecutionServiceFactory);
        
        // Enable development-specific features
        factory.enableDebugMode();
        factory.enableVerboseLogging();
        
        return factory;
    }
    
    /**
     * Production-optimized service factory with performance tuning.
     */
    @Bean
    @Primary
    @Scope("singleton")
    @Profile("prod")
    public ServiceFactory productionServiceFactory(core.factory.ServiceFactory coreServiceFactory,
                                                 DagExecutionServiceFactory dagExecutionServiceFactory) {
        ServiceFactory factory = new ServiceFactory(coreServiceFactory, dagExecutionServiceFactory);
        
        // Enable production-specific optimizations
        factory.enablePerformanceOptimizations();
        factory.enableMetrics();
        factory.enableSecurityValidation();
        
        return factory;
    }
}

/**
 * Enhanced service factory wrapper that provides compatibility API and profile-specific
 * configurations for the refactored ServiceFactory architecture.
 * 
 * This class acts as an adapter between the test expectations and the actual core ServiceFactory.
 */
class ServiceFactory {
    
    private final core.factory.ServiceFactory coreServiceFactory;
    private final DagExecutionServiceFactory dagExecutionServiceFactory;
    private final PromptParserServiceFactory promptParserServiceFactory;
    private final DagBuilderServiceFactory dagBuilderServiceFactory;
    private final DagValidatorServiceFactory dagValidatorServiceFactory;
    
    private boolean debugMode = false;
    private boolean verboseLogging = false;
    private boolean performanceOptimizations = false;
    private boolean metricsEnabled = false;
    private boolean securityValidationEnabled = false;
    
    public ServiceFactory(core.factory.ServiceFactory coreServiceFactory,
                         DagExecutionServiceFactory dagExecutionServiceFactory) {
        
        this.coreServiceFactory = coreServiceFactory;
        this.dagExecutionServiceFactory = dagExecutionServiceFactory;
        this.promptParserServiceFactory = new PromptParserServiceFactory();
        this.dagBuilderServiceFactory = new DagBuilderServiceFactory();
        this.dagValidatorServiceFactory = new DagValidatorServiceFactory();
    }
    
    // Factory getters
    public DagExecutionServiceFactory getDagExecutionServiceFactory() {
        return dagExecutionServiceFactory;
    }
    
    public PromptParserServiceFactory getPromptParserServiceFactory() {
        return promptParserServiceFactory;
    }
    
    public DagBuilderServiceFactory getDagBuilderServiceFactory() {
        return dagBuilderServiceFactory;
    }
    
    public DagValidatorServiceFactory getDagValidatorServiceFactory() {
        return dagValidatorServiceFactory;
    }
    
    // Configuration methods
    public void enableDebugMode() {
        this.debugMode = true;
    }
    
    public void enableVerboseLogging() {
        this.verboseLogging = true;
    }
    
    public void enablePerformanceOptimizations() {
        this.performanceOptimizations = true;
    }
    
    public void enableMetrics() {
        this.metricsEnabled = true;
    }
    
    public void enableSecurityValidation() {
        this.securityValidationEnabled = true;
    }
    
    // Status getters
    public boolean isDebugMode() { return debugMode; }
    public boolean isVerboseLogging() { return verboseLogging; }
    public boolean isPerformanceOptimizations() { return performanceOptimizations; }
    public boolean isMetricsEnabled() { return metricsEnabled; }
    public boolean isSecurityValidationEnabled() { return securityValidationEnabled; }
    
    /**
     * Creates a fully configured service stack for the given profile.
     * 
     * @param profile The spring profile (dev, test, prod)
     * @return ServiceStack containing all configured services
     */
    public ServiceStack createServiceStack(String profile) {
        ServiceStack.Builder builder = ServiceStack.builder();
        
        // Configure based on profile using core factory
        switch (profile.toLowerCase()) {
            case "dev":
                builder.withDagExecutorService(coreServiceFactory.createDagExecutorService())
                      .withPromptParserService(coreServiceFactory.createPromptParserService())
                      .withDagBuilderService(coreServiceFactory.createDagBuilderService())
                      .withDagValidatorService(coreServiceFactory.createDagValidatorService());
                break;
                
            case "test":
                builder.withDagExecutorService(dagExecutionServiceFactory.createTestService())
                      .withPromptParserService(coreServiceFactory.createPromptParserService())
                      .withDagBuilderService(coreServiceFactory.createDagBuilderService())
                      .withDagValidatorService(coreServiceFactory.createDagValidatorService());
                break;
                
            case "prod":
                builder.withDagExecutorService(coreServiceFactory.createDagExecutorService())
                      .withPromptParserService(coreServiceFactory.createPromptParserService())
                      .withDagBuilderService(coreServiceFactory.createDagBuilderService())
                      .withDagValidatorService(coreServiceFactory.createDagValidatorService());
                break;
                
            default:
                builder.withDagExecutorService(coreServiceFactory.createDagExecutorService())
                      .withPromptParserService(coreServiceFactory.createPromptParserService())
                      .withDagBuilderService(coreServiceFactory.createDagBuilderService())
                      .withDagValidatorService(coreServiceFactory.createDagValidatorService());
        }
        
        return builder.build();
    }
}

/**
 * Container for all configured services.
 * Provides type-safe access to service instances.
 */
class ServiceStack {
    private final core.interfaces.DagExecutorService dagExecutorService;
    private final core.interfaces.PromptParserService promptParserService;
    private final core.interfaces.DagBuilderService dagBuilderService;
    private final core.interfaces.DagValidatorService dagValidatorService;
    
    private ServiceStack(Builder builder) {
        this.dagExecutorService = builder.dagExecutorService;
        this.promptParserService = builder.promptParserService;
        this.dagBuilderService = builder.dagBuilderService;
        this.dagValidatorService = builder.dagValidatorService;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    // Getters
    public core.interfaces.DagExecutorService getDagExecutorService() { return dagExecutorService; }
    public core.interfaces.PromptParserService getPromptParserService() { return promptParserService; }
    public core.interfaces.DagBuilderService getDagBuilderService() { return dagBuilderService; }
    public core.interfaces.DagValidatorService getDagValidatorService() { return dagValidatorService; }
    
    public static class Builder {
        private core.interfaces.DagExecutorService dagExecutorService;
        private core.interfaces.PromptParserService promptParserService;
        private core.interfaces.DagBuilderService dagBuilderService;
        private core.interfaces.DagValidatorService dagValidatorService;
        
        public Builder withDagExecutorService(core.interfaces.DagExecutorService dagExecutorService) {
            this.dagExecutorService = dagExecutorService;
            return this;
        }
        
        public Builder withPromptParserService(core.interfaces.PromptParserService promptParserService) {
            this.promptParserService = promptParserService;
            return this;
        }
        
        public Builder withDagBuilderService(core.interfaces.DagBuilderService dagBuilderService) {
            this.dagBuilderService = dagBuilderService;
            return this;
        }
        
        public Builder withDagValidatorService(core.interfaces.DagValidatorService dagValidatorService) {
            this.dagValidatorService = dagValidatorService;
            return this;
        }
        
        public ServiceStack build() {
            return new ServiceStack(this);
        }
    }
}