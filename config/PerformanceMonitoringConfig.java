package api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Compatibility wrapper for PerformanceMonitoringConfig.
 * This configuration has been merged into UnifiedMonitoringConfig.
 * 
 * @deprecated Use UnifiedMonitoringConfig directly
 */
@Configuration
@ConditionalOnProperty(name = "obvian.monitoring.performance.enabled", havingValue = "true", matchIfMissing = true)
@Deprecated
public class PerformanceMonitoringConfig {
    // All functionality has been moved to UnifiedMonitoringConfig
    // This empty class exists only for backward compatibility
}