package api.config;

import core.*;
import core.explainability.CausalTraceLogger;
import core.testing.AITestGenerator;
import memory.MemoryStore;
import memory.MemoryStoreInterface;
import plugins.PluginRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

/**
 * Core Spring configuration for Obvian DAG execution engine.
 * Provides centralized bean definitions for all core execution components.
 * 
 * Patent Alignment: Implements Claim 1.1 (AI-native task orchestration)
 * and Claim 2.1 (Natural language prompt processing) through proper DI.
 */
@Configuration
public class CoreConfig {

    /**
     * Primary MemoryStore bean for persistent execution context and state management.
     * Configured with default settings for production use.
     */
    @Bean
    @Primary
    public MemoryStore memoryStore() {
        System.out.println("=== CREATING MEMORYSTORE BEAN ===");
        return new MemoryStore();
    }

    /**
     * PromptParser bean for natural language processing.
     * Injected with MemoryStore for contextual prompt analysis.
     */
    @Bean
    public PromptParser promptParser(MemoryStore memoryStore) {
        System.out.println("=== CREATING PROMPTPARSER BEAN ===");
        return new PromptParser(memoryStore);
    }

    /**
     * DagBuilder bean for converting parsed intents to executable DAGs.
     * Requires MemoryStore for contextual DAG construction.
     */
    @Bean
    public DagBuilder dagBuilder(MemoryStore memoryStore) {
        System.out.println("=== CREATING DAGBUILDER BEAN ===");
        return new DagBuilder(memoryStore);
    }

    /**
     * DagValidator bean for validating DAG structure and dependencies.
     * Requires PluginRouter for plugin availability validation.
     */
    @Bean
    public DagValidator dagValidator(PluginRouter pluginRouter) {
        System.out.println("=== CREATING DAGVALIDATOR BEAN ===");
        return new DagValidator(pluginRouter);
    }

    /**
     * TraceLogger bean for execution logging and debugging.
     * Singleton scope for consistent trace correlation across executions.
     */
    @Bean
    @Scope("singleton")
    public TraceLogger traceLogger() {
        System.out.println("=== CREATING TRACELOGGER BEAN ===");
        return new TraceLogger();
    }

    /**
     * CausalTraceLogger bean for explainability and causal analysis.
     * Provides detailed execution trace for debugging and compliance.
     */
    @Bean
    public CausalTraceLogger causalTraceLogger() {
        System.out.println("=== CREATING CAUSALTRACELOGGER BEAN ===");
        return new CausalTraceLogger();
    }

    /**
     * MetricsCollector bean for performance and execution metrics.
     * Uses NoOpMetricsCollector for lightweight production deployment.
     */
    @Bean
    public MetricsCollector metricsCollector() {
        System.out.println("=== CREATING METRICSCOLLECTOR BEAN ===");
        return new NoOpMetricsCollector();
    }
    
    // Agent permission beans temporarily disabled to avoid dependency chain
    // TODO: Re-enable when all security dependencies are properly configured

    /**
     * InMemoryMetricsCollector bean for development and testing.
     * Provides detailed metrics collection for debugging.
     */
    @Bean("inMemoryMetricsCollector")
    public MetricsCollector inMemoryMetricsCollector() {
        System.out.println("=== CREATING INMEMORYMETRICSCOLLECTOR BEAN ===");
        return new InMemoryMetricsCollector();
    }

    /**
     * SecureTokenSubstitutor bean for secure parameter substitution.
     * Prevents token leakage and enforces security boundaries.
     */
    @Bean
    public SecureTokenSubstitutor secureTokenSubstitutor() {
        System.out.println("=== CREATING SECURETOKENSUBSTITUTOR BEAN ===");
        return new SecureTokenSubstitutor();
    }

    /**
     * ExecutionContextValidator bean for context validation and security.
     * Ensures execution context meets security and validation requirements.
     */
    @Bean
    public ExecutionContextValidator executionContextValidator() {
        System.out.println("=== CREATING EXECUTIONCONTEXTVALIDATOR BEAN ===");
        return new ExecutionContextValidator();
    }

    /**
     * EnhancedPluginExecutor bean for advanced plugin execution.
     * Provides retry logic, fallback handling, and enhanced error management.
     */
    @Bean
    public EnhancedPluginExecutor enhancedPluginExecutor(PluginRouter pluginRouter) {
        System.out.println("=== CREATING ENHANCEDPLUGINEXECUTOR BEAN ===");
        return new EnhancedPluginExecutor(pluginRouter);
    }

    /**
     * Primary DagExecutor bean for DAG execution with all dependencies.
     * Configured with full dependency injection for production use.
     */
    @Bean
    @Primary
    public DagExecutor dagExecutor(PluginRouter pluginRouter,
                                   TraceLogger traceLogger,
                                   MetricsCollector metricsCollector,
                                   MemoryStore memoryStore) {
        System.out.println("=== CREATING DAGEXECUTOR BEAN ===");
        
        // Create progress callback that can be extended by services
        ProgressCallback progressCallback = new ProgressCallback() {
            @Override
            public void onExecutionStart(String executionId, int totalNodes, String dagId) {
                System.out.println("DAG execution started: " + executionId + " (" + totalNodes + " nodes)");
            }
            
            @Override
            public void onExecutionComplete(String executionId, boolean success, int completedNodes, 
                                          int totalNodes, Object executionResult) {
                System.out.println("DAG execution completed: " + executionId + " (success: " + success + ")");
            }
            
            @Override
            public void onNodeStart(String executionId, String nodeId, String action, 
                                  int completedNodes, int totalNodes) {
                // Default implementation - can be overridden by services
            }
            
            @Override
            public void onNodeComplete(String executionId, String nodeId, String action, boolean success,
                                     int completedNodes, int totalNodes, Object result) {
                // Default implementation - can be overridden by services
            }
            
            @Override
            public void onNodeSkipped(String executionId, String nodeId, String action, String reason,
                                    int completedNodes, int totalNodes) {
                // Default implementation - can be overridden by services
            }
            
            @Override
            public void onExecutionError(String executionId, Exception error, String nodeId) {
                System.err.println("DAG execution error in " + executionId + " at node " + nodeId + ": " + error.getMessage());
            }
        };
        
        return new DagExecutor(pluginRouter, traceLogger, metricsCollector, 
                             null, null, memoryStore, progressCallback);
    }

    /**
     * TokenSubstitutor bean for basic token replacement.
     * Used for non-security-critical token substitution scenarios.
     */
    @Bean
    public TokenSubstitutor tokenSubstitutor() {
        System.out.println("=== CREATING TOKENSUBSTITUTOR BEAN ===");
        return new TokenSubstitutor();
    }

    /**
     * MemoryReconciliationService bean for cross-session memory management.
     * Handles memory consistency and reconciliation across multiple sessions.
     */
    @Bean
    public MemoryReconciliationService memoryReconciliationService() {
        System.out.println("=== CREATING MEMORYRECONCILIATIONSERVICE BEAN ===");
        return new MemoryReconciliationService();
    }

    /**
     * DeviceCapabilityDiscovery bean for device profiling and capability detection.
     * Requires DeviceSessionRegistry for device registry access.
     */
    @Bean
    public DeviceCapabilityDiscovery deviceCapabilityDiscovery(DeviceSessionRegistry deviceRegistry) {
        System.out.println("=== CREATING DEVICECAPABILITYDISCOVERY BEAN ===");
        return new DeviceCapabilityDiscovery(deviceRegistry);
    }

    /**
     * ResourceMonitor bean for resource usage tracking.
     * Basic implementation for monitoring system resources.
     */
    @Bean
    public ResourceMonitor resourceMonitor() {
        System.out.println("=== CREATING RESOURCEMONITOR BEAN ===");
        return new ResourceMonitor();
    }

    /**
     * SystemResourcePool bean for managing system-wide resource pools.
     * Configured with default resource limits suitable for development.
     */
    @Bean
    public SystemResourcePool systemResourcePool() {
        System.out.println("=== CREATING SYSTEMRESOURCEPOOL BEAN ===");
        // Default pool size: 8GB memory, 1000 operations, 100 CPU units
        return new SystemResourcePool(8_000_000_000L, 1000, 100);
    }

    /**
     * TenantResourceManager bean for tenant resource management.
     * Manages quotas and limits for multi-tenant resource allocation.
     */
    @Bean
    public TenantResourceManager tenantResourceManager(ResourceMonitor resourceMonitor) {
        System.out.println("=== CREATING TENANTRESOURCEMANAGER BEAN ===");
        return new TenantResourceManager(resourceMonitor);
    }

    /**
     * ResourceAllocationEngine bean for intelligent resource allocation.
     * Provides core resource allocation algorithms and strategies.
     */
    @Bean
    public ResourceAllocationEngine resourceAllocationEngine(
            SystemResourcePool systemResourcePool, 
            ResourceMonitor resourceMonitor) {
        System.out.println("=== CREATING RESOURCEALLOCATIONENGINE BEAN ===");
        return new ResourceAllocationEngine(systemResourcePool, resourceMonitor);
    }

    /**
     * MultiTenantResourceArbitrator bean for multi-tenant resource arbitration.
     * Manages resource allocation across tenants with tier-based prioritization.
     */
    @Bean
    public MultiTenantResourceArbitrator multiTenantResourceArbitrator(
            ResourceMonitor resourceMonitor,
            TenantResourceManager tenantResourceManager,
            ResourceAllocationEngine resourceAllocationEngine) {
        System.out.println("=== CREATING MULTITENANTRESOURCEARBITRATOR BEAN ===");
        return new MultiTenantResourceArbitrator(resourceMonitor, tenantResourceManager, resourceAllocationEngine);
    }

    /**
     * AdaptiveCoordinationManager bean for adaptive workload coordination.
     * Orchestrates intelligent workload distribution across devices.
     */
    @Bean
    public AdaptiveCoordinationManager adaptiveCoordinationManager(
            DeviceCapabilityDiscovery capabilityDiscovery,
            DeviceSessionRegistry deviceRegistry,
            MultiTenantResourceArbitrator resourceArbitrator) {
        System.out.println("=== CREATING ADAPTIVECOORDINATIONMANAGER BEAN ===");
        return new AdaptiveCoordinationManager(capabilityDiscovery, deviceRegistry, resourceArbitrator);
    }

    /**
     * AITestGenerator bean for AI-powered test generation.
     * Converts natural language descriptions to executable test scenarios.
     */
    @Bean
    public AITestGenerator aiTestGenerator(PromptParser promptParser) {
        System.out.println("=== CREATING AITESTGENERATOR BEAN ===");
        return new AITestGenerator(promptParser);
    }
}