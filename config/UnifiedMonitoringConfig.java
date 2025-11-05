package api.config;

import api.middleware.MonitoringInterceptor;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import io.opentelemetry.api.common.Attributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unified monitoring configuration that consolidates all monitoring, tracing, and performance configurations.
 * This replaces MonitoringConfig, TracingConfig, and PerformanceMonitoringConfig to avoid conflicts.
 * 
 * @author Obvian Labs
 * @since Phase 26.3
 */
@Configuration
@ConditionalOnProperty(name = "obvian.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class UnifiedMonitoringConfig implements WebMvcConfigurer {
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Value("${obvian.service.name:obvian-api}")
    private String serviceName;
    
    @Value("${obvian.service.version:0.5.0}")
    private String serviceVersion;
    
    @Value("${obvian.service.environment:development}")
    private String serviceEnvironment;
    
    @Value("${monitoring.jaeger.endpoint:http://localhost:14250}")
    private String jaegerEndpoint;
    
    @Value("${obvian.monitoring.jvm.enabled:true}")
    private boolean enableJvmMetrics;
    
    @Value("${obvian.monitoring.system.enabled:true}")
    private boolean enableSystemMetrics;
    
    @Autowired
    @Lazy
    private MonitoringInterceptor monitoringInterceptor;
    
    // MeterRegistry will be injected in methods that need it, not as a field to avoid circular dependency
    
    // Metrics tracking
    private final AtomicInteger activeExecutions = new AtomicInteger(0);
    private final AtomicInteger queueDepth = new AtomicInteger(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (monitoringInterceptor != null) {
            registry.addInterceptor(monitoringInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns("/health", "/metrics", "/ready", "/live");
        }
    }
    
    /**
     * Configure Prometheus metrics registry if not already present.
     * Uses @ConditionalOnMissingBean pattern to avoid conflicts.
     */
    @Bean
    @ConditionalOnProperty(name = "obvian.monitoring.prometheus.enabled", havingValue = "true", matchIfMissing = false)
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        // Add common tags to all metrics
        registry.config()
            .commonTags(
                "application", serviceName,
                "version", serviceVersion,
                "environment", serviceEnvironment
            )
            // Configure metric filters
            .meterFilter(MeterFilter.deny(id -> {
                String name = id.getName();
                // Filter out noisy metrics
                return name.startsWith("jvm.classes") || 
                       name.startsWith("process.files") ||
                       name.startsWith("logback");
            }));
        
        return registry;
    }
    
    /**
     * Configure OpenTelemetry for distributed tracing.
     * This is the ONLY place where OpenTelemetry bean should be defined.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "obvian.tracing.enabled", havingValue = "true", matchIfMissing = true)
    public OpenTelemetry openTelemetry() {
        // Create resource with service information
        Resource resource = Resource.create(
            Attributes.builder()
                .put(ResourceAttributes.SERVICE_NAME, serviceName)
                .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, serviceEnvironment)
                .build()
        );
        
        // Configure Jaeger exporter
        SpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint(jaegerEndpoint)
                .build();
        
        // Create tracer provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
                .build();
        
        // Build OpenTelemetry SDK
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
    }
    
    /**
     * Create tracer bean from OpenTelemetry.
     */
    @Bean
    public Tracer tracer(@Autowired(required = false) OpenTelemetry openTelemetry) {
        if (openTelemetry == null) {
            // Return a no-op tracer if OpenTelemetry is disabled
            return OpenTelemetry.noop().getTracer(serviceName);
        }
        return openTelemetry.getTracer(serviceName);
    }
    
    /**
     * Initialize JVM and system metrics after bean creation.
     * This avoids circular dependencies.
     */
    @PostConstruct
    public void initializeMetrics() {
        if (meterRegistry == null) {
            return; // Metrics not available, skip setup
        }
        
        // Add JVM metrics if enabled
        if (enableJvmMetrics) {
            new JvmMemoryMetrics().bindTo(meterRegistry);
            new JvmGcMetrics().bindTo(meterRegistry);
            new JvmThreadMetrics().bindTo(meterRegistry);
        }
        
        // Add system metrics if enabled
        if (enableSystemMetrics) {
            new ProcessorMetrics().bindTo(meterRegistry);
            new UptimeMetrics().bindTo(meterRegistry);
        }
        
        // Register custom gauges
        Gauge.builder("obvian.active_executions", activeExecutions, AtomicInteger::get)
                .description("Number of currently active executions")
                .register(meterRegistry);
        
        Gauge.builder("obvian.queue_depth", queueDepth, AtomicInteger::get)
                .description("Current execution queue depth")
                .register(meterRegistry);
        
        Gauge.builder("obvian.total_requests", totalRequests, AtomicLong::get)
                .description("Total number of API requests")
                .register(meterRegistry);
        
        Gauge.builder("obvian.total_errors", totalErrors, AtomicLong::get)
                .description("Total number of API errors")
                .register(meterRegistry);
    }
    
    /**
     * Unified monitoring service that provides all monitoring capabilities.
     */
    @Bean
    public MonitoringService monitoringService(
            @Autowired(required = false) MeterRegistry injectedMeterRegistry,
            @Autowired(required = false) Tracer tracer) {
        // Use injected registry or create a simple one if not available
        MeterRegistry registry = injectedMeterRegistry != null ? injectedMeterRegistry : new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        return new MonitoringService(
            registry,
            tracer != null ? tracer : OpenTelemetry.noop().getTracer(serviceName),
            activeExecutions, queueDepth, totalRequests, totalErrors
        );
    }
    
    /**
     * Health indicator for execution queue.
     */
    @Bean
    public HealthIndicator executionQueueHealthIndicator() {
        return () -> {
            int currentQueueDepth = queueDepth.get();
            if (currentQueueDepth > 100) {
                return Health.down()
                        .withDetail("queueDepth", currentQueueDepth)
                        .withDetail("reason", "Queue depth exceeds threshold")
                        .build();
            }
            return Health.up()
                    .withDetail("queueDepth", currentQueueDepth)
                    .build();
        };
    }
    
    /**
     * Health indicator for active executions.
     */
    @Bean
    public HealthIndicator activeExecutionsHealthIndicator() {
        return () -> {
            int currentActiveExecutions = activeExecutions.get();
            if (currentActiveExecutions > 50) {
                return Health.down()
                        .withDetail("activeExecutions", currentActiveExecutions)
                        .withDetail("reason", "Too many active executions")
                        .build();
            }
            return Health.up()
                    .withDetail("activeExecutions", currentActiveExecutions)
                    .build();
        };
    }
    
    /**
     * Unified monitoring service that provides all monitoring capabilities.
     */
    public static class MonitoringService {
        private final MeterRegistry meterRegistry;
        private final Tracer tracer;
        private final Counter requestCounter;
        private final Counter errorCounter;
        private final Timer executionTimer;
        private final Timer responseTimer;
        private final AtomicInteger activeExecutions;
        private final AtomicInteger queueDepth;
        private final AtomicLong totalRequests;
        private final AtomicLong totalErrors;
        
        public MonitoringService(MeterRegistry meterRegistry, Tracer tracer,
                               AtomicInteger activeExecutions, AtomicInteger queueDepth,
                               AtomicLong totalRequests, AtomicLong totalErrors) {
            this.meterRegistry = meterRegistry;
            this.tracer = tracer;
            this.activeExecutions = activeExecutions;
            this.queueDepth = queueDepth;
            this.totalRequests = totalRequests;
            this.totalErrors = totalErrors;
            
            // Initialize metrics
            this.requestCounter = Counter.builder("obvian.api.requests.total")
                    .description("Total number of API requests")
                    .register(meterRegistry);
            
            this.errorCounter = Counter.builder("obvian.api.errors.total")
                    .description("Total number of API errors")
                    .register(meterRegistry);
            
            this.executionTimer = Timer.builder("obvian.execution.duration")
                    .description("DAG execution duration")
                    .register(meterRegistry);
            
            this.responseTimer = Timer.builder("obvian.api.response.time")
                    .description("API response time")
                    .register(meterRegistry);
        }
        
        // Getters and operational methods
        public Tracer getTracer() { return tracer; }
        public MeterRegistry getMeterRegistry() { return meterRegistry; }
        
        public void incrementRequests() {
            requestCounter.increment();
            totalRequests.incrementAndGet();
        }
        
        public void incrementErrors() {
            errorCounter.increment();
            totalErrors.incrementAndGet();
        }
        
        public Timer.Sample startExecutionTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordExecutionTime(Timer.Sample sample) {
            sample.stop(executionTimer);
        }
        
        public Timer.Sample startResponseTimer() {
            return Timer.start(meterRegistry);
        }
        
        public void recordResponseTime(Timer.Sample sample) {
            sample.stop(responseTimer);
        }
        
        public void incrementActiveExecutions() {
            activeExecutions.incrementAndGet();
        }
        
        public void decrementActiveExecutions() {
            activeExecutions.decrementAndGet();
        }
        
        public void setQueueDepth(int depth) {
            queueDepth.set(depth);
        }
        
        public int getActiveExecutions() { return activeExecutions.get(); }
        public int getQueueDepth() { return queueDepth.get(); }
        public long getTotalRequests() { return totalRequests.get(); }
        public long getTotalErrors() { return totalErrors.get(); }
    }
}