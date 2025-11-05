package api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for Server-Sent Events (SSE) support in the Obvian API.
 * 
 * This configuration ensures proper async support, CORS handling, 
 * and resource management for SSE connections in the visual trace system.
 * 
 * Features:
 * - Async request processing configuration for SSE
 * - CORS support for cross-origin SSE connections
 * - Thread pool management for SSE operations
 * - Timeout and resource cleanup configuration
 * 
 * @author Obvian Labs
 * @since Phase 26.2d
 */
@Configuration
public class SseConfig implements WebMvcConfigurer {

    /**
     * Configure async support for SSE endpoints.
     * This is essential for proper SSE functionality in Spring Boot.
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(300000L); // 5 minutes default timeout
        configurer.setTaskExecutor(sseAsyncTaskExecutor());
    }

    /**
     * Configure CORS to allow SSE connections from different origins.
     * This is important for browser-based SSE clients.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/traces/{executionId}/events")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Cache-Control", "Connection", "X-Accel-Buffering")
                .maxAge(3600); // 1 hour preflight cache

        registry.addMapping("/api/traces/sse/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Task scheduler specifically for SSE operations.
     * Separate from the main WebSocket heartbeat scheduler to avoid conflicts.
     */
    @Bean(name = "sseTaskScheduler")
    public TaskScheduler sseTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4); // Dedicated threads for SSE operations
        scheduler.setThreadNamePrefix("sse-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.setRejectedExecutionHandler((r, executor) -> {
            // Log rejected executions for monitoring
            System.err.println("SSE task rejected: " + r.toString());
        });
        return scheduler;
    }
    
    /**
     * Async task executor for SSE operations.
     */
    @Bean(name = "sseAsyncTaskExecutor")
    public AsyncTaskExecutor sseAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sse-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}