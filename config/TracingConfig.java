package api.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Compatibility wrapper for TracingConfig that delegates to UnifiedMonitoringConfig.
 * This allows existing code to continue working while we migrate to the unified configuration.
 * 
 * @deprecated Use UnifiedMonitoringConfig directly
 */
@Configuration
@ConditionalOnProperty(name = "obvian.tracing.enabled", havingValue = "true", matchIfMissing = true)
@Deprecated
public class TracingConfig {
    
    @Autowired
    private UnifiedMonitoringConfig unifiedMonitoringConfig;
    
    @Autowired(required = false)
    private OpenTelemetry openTelemetry;
    
    @Autowired(required = false)
    private Tracer tracer;
    
    /**
     * Provides a tracer bean with "visualTraceTracer" qualifier for backward compatibility.
     */
    @Bean(name = "visualTraceTracer")
    @Primary
    public Tracer visualTraceTracer() {
        return tracer != null ? tracer : OpenTelemetry.noop().getTracer("obvian-api");
    }
    
    /**
     * Gets tracing metrics for backward compatibility.
     */
    public TracingMetrics getTracingMetrics() {
        return new TracingMetrics();
    }
    
    /**
     * Backward compatibility class for tracing metrics.
     */
    public static class TracingMetrics {
        private final String serviceName = "obvian-api";
        private final String serviceVersion = "0.5.0";
        private final String serviceEnvironment = "development";
        private final double samplingRatio = 1.0;
        private final boolean lowOverheadMode = false;
        private final Duration exportTimeout = Duration.ofSeconds(30);
        private final int maxExportBatchSize = 512;
        
        public String getServiceName() { return serviceName; }
        public String getServiceVersion() { return serviceVersion; }
        public String getServiceEnvironment() { return serviceEnvironment; }
        public double getSamplingRatio() { return samplingRatio; }
        public boolean isLowOverheadMode() { return lowOverheadMode; }
        public Duration getExportTimeout() { return exportTimeout; }
        public int getMaxExportBatchSize() { return maxExportBatchSize; }
    }
}