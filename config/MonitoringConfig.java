package api.config;

import io.opentelemetry.api.trace.Tracer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Compatibility wrapper for MonitoringConfig that delegates to UnifiedMonitoringConfig.
 * This allows existing code to continue working while we migrate to the unified configuration.
 * 
 * @deprecated Use UnifiedMonitoringConfig directly
 */
@Configuration
@ConditionalOnProperty(name = "obvian.monitoring.use-legacy", havingValue = "true", matchIfMissing = false)
@Deprecated
public class MonitoringConfig {
    
    @Autowired
    private UnifiedMonitoringConfig unifiedMonitoringConfig;
    
    @Autowired(required = false)
    private UnifiedMonitoringConfig.MonitoringService monitoringService;
    
    /**
     * Provides monitoring service bean for backward compatibility.
     */
    @Bean
    @Primary
    public MonitoringService monitoringService() {
        if (monitoringService != null) {
            return new MonitoringServiceWrapper(monitoringService);
        }
        // Return a dummy implementation if unified config is not available
        return new DummyMonitoringService();
    }
    
    /**
     * Wrapper class to adapt UnifiedMonitoringConfig.MonitoringService to the old interface.
     */
    public static class MonitoringServiceWrapper extends MonitoringService {
        private final UnifiedMonitoringConfig.MonitoringService delegate;
        
        public MonitoringServiceWrapper(UnifiedMonitoringConfig.MonitoringService delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public Tracer getTracer() { return delegate.getTracer(); }
        
        @Override
        public MeterRegistry getMeterRegistry() { return delegate.getMeterRegistry(); }
        
        @Override
        public void incrementRequests() { delegate.incrementRequests(); }
        
        @Override
        public void incrementErrors() { delegate.incrementErrors(); }
        
        @Override
        public Timer.Sample startExecutionTimer() { return delegate.startExecutionTimer(); }
        
        @Override
        public void recordExecutionTime(Timer.Sample sample) { delegate.recordExecutionTime(sample); }
        
        @Override
        public Timer.Sample startResponseTimer() { return delegate.startResponseTimer(); }
        
        @Override
        public void recordResponseTime(Timer.Sample sample) { delegate.recordResponseTime(sample); }
        
        @Override
        public void incrementActiveExecutions() { delegate.incrementActiveExecutions(); }
        
        @Override
        public void decrementActiveExecutions() { delegate.decrementActiveExecutions(); }
        
        @Override
        public void setQueueDepth(int depth) { delegate.setQueueDepth(depth); }
        
        @Override
        public int getActiveExecutions() { return delegate.getActiveExecutions(); }
        
        @Override
        public int getQueueDepth() { return delegate.getQueueDepth(); }
        
        @Override
        public long getTotalRequests() { return delegate.getTotalRequests(); }
        
        @Override
        public long getTotalErrors() { return delegate.getTotalErrors(); }
    }
    
    /**
     * Abstract base class for monitoring service to match the old interface.
     */
    public static abstract class MonitoringService {
        public abstract Tracer getTracer();
        public abstract MeterRegistry getMeterRegistry();
        public abstract void incrementRequests();
        public abstract void incrementErrors();
        public abstract Timer.Sample startExecutionTimer();
        public abstract void recordExecutionTime(Timer.Sample sample);
        public abstract Timer.Sample startResponseTimer();
        public abstract void recordResponseTime(Timer.Sample sample);
        public abstract void incrementActiveExecutions();
        public abstract void decrementActiveExecutions();
        public abstract void setQueueDepth(int depth);
        public abstract int getActiveExecutions();
        public abstract int getQueueDepth();
        public abstract long getTotalRequests();
        public abstract long getTotalErrors();
    }
    
    /**
     * Dummy implementation for when unified config is not available.
     */
    private static class DummyMonitoringService extends MonitoringService {
        private final AtomicInteger activeExecutions = new AtomicInteger(0);
        private final AtomicInteger queueDepth = new AtomicInteger(0);
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong totalErrors = new AtomicLong(0);
        private final MeterRegistry meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        private final Tracer tracer = io.opentelemetry.api.OpenTelemetry.noop().getTracer("obvian-api");
        
        @Override
        public Tracer getTracer() { return tracer; }
        
        @Override
        public MeterRegistry getMeterRegistry() { return meterRegistry; }
        
        @Override
        public void incrementRequests() { totalRequests.incrementAndGet(); }
        
        @Override
        public void incrementErrors() { totalErrors.incrementAndGet(); }
        
        @Override
        public Timer.Sample startExecutionTimer() { return Timer.start(meterRegistry); }
        
        @Override
        public void recordExecutionTime(Timer.Sample sample) {
            if (sample != null) {
                sample.stop(Timer.builder("execution.duration").register(meterRegistry));
            }
        }
        
        @Override
        public Timer.Sample startResponseTimer() { return Timer.start(meterRegistry); }
        
        @Override
        public void recordResponseTime(Timer.Sample sample) {
            if (sample != null) {
                sample.stop(Timer.builder("response.time").register(meterRegistry));
            }
        }
        
        @Override
        public void incrementActiveExecutions() { activeExecutions.incrementAndGet(); }
        
        @Override
        public void decrementActiveExecutions() { activeExecutions.decrementAndGet(); }
        
        @Override
        public void setQueueDepth(int depth) { queueDepth.set(depth); }
        
        @Override
        public int getActiveExecutions() { return activeExecutions.get(); }
        
        @Override
        public int getQueueDepth() { return queueDepth.get(); }
        
        @Override
        public long getTotalRequests() { return totalRequests.get(); }
        
        @Override
        public long getTotalErrors() { return totalErrors.get(); }
    }
}