package api.config;

import core.interfaces.*;
import core.impl.*;
import core.factory.ServiceFactory;
import plugins.PluginRouter;
import memory.MemoryStoreInterface;
import memory.MemoryStore;
import core.MetricsCollector;
import core.NoOpMetricsCollector;
import core.TraceLogger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Spring configuration for core service interfaces and implementations.
 * Provides proper dependency injection for the refactored architecture.
 * 
 * @since 2.0.0
 */
@Configuration
public class ServiceConfiguration {
    
    /**
     * Configure the ServiceFactory with all required dependencies.
     */
    @Bean
    @Primary
    public ServiceFactory serviceFactory(PluginRouter pluginRouter, 
                                        MemoryStoreInterface memoryStore,
                                        MetricsCollector metricsCollector,
                                        TraceLogger traceLogger) {
        ServiceFactory.FactoryConfiguration config = 
            new ServiceFactory.FactoryConfiguration(pluginRouter, memoryStore)
                .withMetricsCollector(metricsCollector)
                .withTraceLogger(traceLogger);
                
        return ServiceFactory.getInstance().configure(config);
    }
    
    /**
     * Primary DAG executor service using the factory.
     */
    @Bean
    @Primary
    public DagExecutorService dagExecutorService(ServiceFactory serviceFactory) {
        return serviceFactory.createDagExecutorService();
    }
    
    /**
     * Primary prompt parser service using the factory.
     */
    @Bean
    @Primary
    public PromptParserService promptParserService(ServiceFactory serviceFactory) {
        return serviceFactory.createPromptParserService();
    }
    
    /**
     * Primary DAG builder service using the factory.
     */
    @Bean
    @Primary
    public DagBuilderService dagBuilderService(ServiceFactory serviceFactory) {
        return serviceFactory.createDagBuilderService();
    }
    
    /**
     * Primary DAG validator service using the factory.
     */
    @Bean
    @Primary
    public DagValidatorService dagValidatorService(ServiceFactory serviceFactory) {
        return serviceFactory.createDagValidatorService();
    }
    
    /**
     * Default metrics collector implementation.
     */
    @Bean
    @ConditionalOnMissingBean
    public MetricsCollector metricsCollector() {
        return new NoOpMetricsCollector();
    }
    
    /**
     * Default trace logger implementation.
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceLogger traceLogger() {
        return new TraceLogger();
    }
    
    /**
     * Memory store interface - use existing MemoryStore implementation.
     */
    @Bean
    @ConditionalOnMissingBean
    public MemoryStoreInterface memoryStore() {
        return new MemoryStore();
    }
    
    // Profile-specific configurations
    
    /**
     * Development profile configuration with enhanced logging.
     */
    @Configuration
    @Profile("dev")
    static class DevelopmentConfiguration {
        
        @Bean
        public MetricsCollector developmentMetricsCollector() {
            return new NoOpMetricsCollector();
        }
    }
    
    /**
     * Test profile configuration with mock-friendly setup.
     */
    @Configuration
    @Profile("test")
    static class TestConfiguration {
        
        @Bean
        @Primary
        public MemoryStoreInterface testMemoryStore() {
            return new MemoryStore();
        }
    }
    
    /**
     * Production profile configuration with optimized settings.
     */
    @Configuration
    @Profile("prod")
    static class ProductionConfiguration {
        
        @Bean
        public MetricsCollector productionMetricsCollector() {
            return new NoOpMetricsCollector();
        }
    }
    
    /**
     * Service properties configuration
     */
    @Component
    @ConfigurationProperties(prefix = "obvian.service")
    public static class ServiceProperties {
        
        private ExecutionProperties execution = new ExecutionProperties();
        private MemoryProperties memory = new MemoryProperties();
        private MetricsProperties metrics = new MetricsProperties();
        private ParsingProperties parsing = new ParsingProperties();
        private ValidationProperties validation = new ValidationProperties();
        private TracingProperties tracing = new TracingProperties();
        
        public ExecutionProperties getExecution() { return execution; }
        public void setExecution(ExecutionProperties execution) { this.execution = execution; }
        
        public MemoryProperties getMemory() { return memory; }
        public void setMemory(MemoryProperties memory) { this.memory = memory; }
        
        public MetricsProperties getMetrics() { return metrics; }
        public void setMetrics(MetricsProperties metrics) { this.metrics = metrics; }
        
        public ParsingProperties getParsing() { return parsing; }
        public void setParsing(ParsingProperties parsing) { this.parsing = parsing; }
        
        public ValidationProperties getValidation() { return validation; }
        public void setValidation(ValidationProperties validation) { this.validation = validation; }
        
        public TracingProperties getTracing() { return tracing; }
        public void setTracing(TracingProperties tracing) { this.tracing = tracing; }
        
        public static class ExecutionProperties {
            private long timeoutMs = 30000L;
            private int maxRetries = 3;
            private boolean enableMetrics = true;
            private boolean enableTracing = true;
            private boolean failFast = false;
            
            public long getTimeoutMs() { return timeoutMs; }
            public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
            
            public int getMaxRetries() { return maxRetries; }
            public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
            
            public boolean isEnableMetrics() { return enableMetrics; }
            public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
            
            public boolean isEnableTracing() { return enableTracing; }
            public void setEnableTracing(boolean enableTracing) { this.enableTracing = enableTracing; }
            
            public boolean isFailFast() { return failFast; }
            public void setFailFast(boolean failFast) { this.failFast = failFast; }
        }
        
        public static class MemoryProperties {
            private int maxEntries = 10000;
            private long ttlMinutes = 60L;
            private boolean persistToDisk = false;
            
            public int getMaxEntries() { return maxEntries; }
            public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }
            
            public long getTtlMinutes() { return ttlMinutes; }
            public void setTtlMinutes(long ttlMinutes) { this.ttlMinutes = ttlMinutes; }
            
            public boolean isPersistToDisk() { return persistToDisk; }
            public void setPersistToDisk(boolean persistToDisk) { this.persistToDisk = persistToDisk; }
        }
        
        public static class MetricsProperties {
            private boolean enabled = true;
            private String exporterType = "prometheus";
            private int reportingIntervalSeconds = 60;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public String getExporterType() { return exporterType; }
            public void setExporterType(String exporterType) { this.exporterType = exporterType; }
            
            public int getReportingIntervalSeconds() { return reportingIntervalSeconds; }
            public void setReportingIntervalSeconds(int reportingIntervalSeconds) { 
                this.reportingIntervalSeconds = reportingIntervalSeconds; 
            }
        }
        
        public static class ParsingProperties {
            private String strategy = "hybrid";
            private boolean enableCache = true;
            private int maxCacheSize = 1000;
            private int cacheTtlMinutes = 30;
            private double confidenceThreshold = 0.7;
            
            public String getStrategy() { return strategy; }
            public void setStrategy(String strategy) { this.strategy = strategy; }
            
            public boolean isEnableCache() { return enableCache; }
            public void setEnableCache(boolean enableCache) { this.enableCache = enableCache; }
            
            public int getMaxCacheSize() { return maxCacheSize; }
            public void setMaxCacheSize(int maxCacheSize) { this.maxCacheSize = maxCacheSize; }
            
            public int getCacheTtlMinutes() { return cacheTtlMinutes; }
            public void setCacheTtlMinutes(int cacheTtlMinutes) { this.cacheTtlMinutes = cacheTtlMinutes; }
            
            public double getConfidenceThreshold() { return confidenceThreshold; }
            public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
        }
        
        public static class ValidationProperties {
            private boolean strictMode = false;
            private boolean enableSecurityChecks = true;
            private boolean enableSecurityValidation = true;
            private boolean enableComplianceChecks = true;
            private int maxDagSize = 1000;
            private int maxDagNodes = 100;
            private int maxDagDepth = 10;
            
            public boolean isStrictMode() { return strictMode; }
            public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; }
            
            public boolean isEnableSecurityChecks() { return enableSecurityChecks; }
            public void setEnableSecurityChecks(boolean enableSecurityChecks) { this.enableSecurityChecks = enableSecurityChecks; }
            
            public int getMaxDagSize() { return maxDagSize; }
            public void setMaxDagSize(int maxDagSize) { this.maxDagSize = maxDagSize; }
            
            public boolean isEnableSecurityValidation() { return enableSecurityValidation; }
            public void setEnableSecurityValidation(boolean enableSecurityValidation) { this.enableSecurityValidation = enableSecurityValidation; }
            
            public boolean isEnableComplianceChecks() { return enableComplianceChecks; }
            public void setEnableComplianceChecks(boolean enableComplianceChecks) { this.enableComplianceChecks = enableComplianceChecks; }
            
            public int getMaxDagNodes() { return maxDagNodes; }
            public void setMaxDagNodes(int maxDagNodes) { this.maxDagNodes = maxDagNodes; }
            
            public int getMaxDagDepth() { return maxDagDepth; }
            public void setMaxDagDepth(int maxDagDepth) { this.maxDagDepth = maxDagDepth; }
        }
        
        public static class TracingProperties {
            private boolean enabled = false;
            private String exporterEndpoint = "http://localhost:4317";
            private double samplingRate = 0.1;
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public String getExporterEndpoint() { return exporterEndpoint; }
            public void setExporterEndpoint(String exporterEndpoint) { this.exporterEndpoint = exporterEndpoint; }
            
            public double getSamplingRate() { return samplingRate; }
            public void setSamplingRate(double samplingRate) { this.samplingRate = samplingRate; }
        }
    }
}