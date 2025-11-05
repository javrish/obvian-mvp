package api.controller;

import api.config.UnifiedMonitoringConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import plugins.PluginRegistry;
import org.springframework.context.annotation.Lazy;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced health check controller with comprehensive monitoring.
 */
@RestController
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Lazy
    private UnifiedMonitoringConfig.MonitoringService monitoringService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private Map<String, HealthIndicator> healthIndicators;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Tracer tracer = monitoringService.getTracer();
        Span span = tracer.spanBuilder("health_check").startSpan();
        
        try {
            Map<String, Object> health = new HashMap<>();
            Map<String, Object> components = new HashMap<>();
            boolean overallHealthy = true;
            
            health.put("status", "healthy");
            health.put("timestamp", Instant.now().toString());
            health.put("version", "1.0.0");
            
            // Check Redis connectivity
            try {
                redisTemplate.opsForValue().set("health_check", "ok");
                String result = (String) redisTemplate.opsForValue().get("health_check");
                boolean redisHealthy = "ok".equals(result);
                components.put("redis", redisHealthy ? "healthy" : "unhealthy");
                redisTemplate.delete("health_check");
                overallHealthy &= redisHealthy;
                
                span.addEvent("redis_check_completed");
            } catch (Exception e) {
                components.put("redis", "unhealthy");
                overallHealthy = false;
                span.recordException(e);
                logger.error("Redis health check failed", e);
            }
            
            // Check plugin registry
            try {
                int pluginCount = pluginRegistry.getAllPlugins().size();
                boolean pluginRegistryHealthy = pluginCount > 0;
                components.put("pluginRegistry", pluginRegistryHealthy ? "healthy" : "unhealthy");
                components.put("pluginCount", pluginCount);
                overallHealthy &= pluginRegistryHealthy;
                
                span.addEvent("plugin_registry_check_completed");
            } catch (Exception e) {
                components.put("pluginRegistry", "unhealthy");
                overallHealthy = false;
                span.recordException(e);
                logger.error("Plugin registry health check failed", e);
            }
            
            // Check custom health indicators
            for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
                try {
                    Status status = entry.getValue().health().getStatus();
                    boolean indicatorHealthy = Status.UP.equals(status);
                    components.put(entry.getKey(), indicatorHealthy ? "healthy" : "unhealthy");
                    overallHealthy &= indicatorHealthy;
                } catch (Exception e) {
                    components.put(entry.getKey(), "unhealthy");
                    overallHealthy = false;
                    logger.error("Health indicator {} failed", entry.getKey(), e);
                }
            }
            
            health.put("components", components);
            health.put("status", overallHealthy ? "healthy" : "unhealthy");
            
            // Add system metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("activeExecutions", monitoringService.getActiveExecutions());
            metrics.put("queueDepth", monitoringService.getQueueDepth());
            metrics.put("totalRequests", monitoringService.getTotalRequests());
            metrics.put("totalErrors", monitoringService.getTotalErrors());
            
            // Add JVM metrics
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            
            Map<String, Object> jvmMetrics = new HashMap<>();
            jvmMetrics.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
            jvmMetrics.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
            jvmMetrics.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
            jvmMetrics.put("uptime", runtimeBean.getUptime());
            
            metrics.put("jvm", jvmMetrics);
            health.put("metrics", metrics);
            
            span.addEvent("health_check_completed");
            return ResponseEntity.ok(health);
            
        } finally {
            span.end();
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<String> metrics() {
        // Return Prometheus format metrics
        StringBuilder prometheus = new StringBuilder();
        
        // API request metrics
        prometheus.append("# HELP obvian_api_requests_total Total number of API requests\n");
        prometheus.append("# TYPE obvian_api_requests_total counter\n");
        prometheus.append("obvian_api_requests_total ").append(monitoringService.getTotalRequests()).append("\n");
        
        // Error metrics
        prometheus.append("# HELP obvian_api_errors_total Total number of API errors\n");
        prometheus.append("# TYPE obvian_api_errors_total counter\n");
        prometheus.append("obvian_api_errors_total ").append(monitoringService.getTotalErrors()).append("\n");
        
        // Active executions
        prometheus.append("# HELP obvian_active_executions Number of currently active executions\n");
        prometheus.append("# TYPE obvian_active_executions gauge\n");
        prometheus.append("obvian_active_executions ").append(monitoringService.getActiveExecutions()).append("\n");
        
        // Queue depth
        prometheus.append("# HELP obvian_queue_depth Current execution queue depth\n");
        prometheus.append("# TYPE obvian_queue_depth gauge\n");
        prometheus.append("obvian_queue_depth ").append(monitoringService.getQueueDepth()).append("\n");
        
        // JVM metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        prometheus.append("# HELP jvm_memory_used_bytes Used memory in bytes\n");
        prometheus.append("# TYPE jvm_memory_used_bytes gauge\n");
        prometheus.append("jvm_memory_used_bytes{area=\"heap\"} ").append(memoryBean.getHeapMemoryUsage().getUsed()).append("\n");
        prometheus.append("jvm_memory_used_bytes{area=\"nonheap\"} ").append(memoryBean.getNonHeapMemoryUsage().getUsed()).append("\n");
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                .body(prometheus.toString());
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        Map<String, Object> readiness = new HashMap<>();
        boolean ready = true;
        
        // Check critical components for readiness
        try {
            redisTemplate.opsForValue().set("readiness_check", "ok");
            redisTemplate.delete("readiness_check");
        } catch (Exception e) {
            ready = false;
            logger.warn("Readiness check failed - Redis unavailable", e);
        }
        
        try {
            pluginRegistry.getAllPlugins();
        } catch (Exception e) {
            ready = false;
            logger.warn("Readiness check failed - Plugin registry unavailable", e);
        }
        
        readiness.put("status", ready ? "ready" : "not_ready");
        readiness.put("timestamp", Instant.now().toString());
        
        return ready ? ResponseEntity.ok(readiness) : ResponseEntity.status(503).body(readiness);
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "alive");
        liveness.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(liveness);
    }
}