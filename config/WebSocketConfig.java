package api.config;

import api.websocket.ExecutionWebSocketHandler;
import api.websocket.MemorySyncWebSocketHandler;
import api.websocket.AgentPolicyWebSocketHandler;
import api.websocket.PetriNetWebSocketHandler;
import api.websocket.WebSocketSecurityInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * Enhanced WebSocket configuration for live trace streaming and real-time execution updates
 * 
 * Features:
 * - Dedicated endpoints for trace streaming, playback control, and breakpoint management
 * - Multi-tenant security with proper authentication and authorization
 * - Connection lifecycle management with error recovery
 * - Scalable message broker configuration
 * - Performance optimized connection limits and buffer sizes
 * 
 * Endpoints:
 * - /ws/traces/{executionId}/events - Real-time execution events
 * - /ws/traces/{executionId}/playback - Playback state updates
 * - /ws/traces/{executionId}/breakpoints - Breakpoint notifications
 * - /ws/memory-sync - Memory synchronization for agent coordination
 * - /ws/policy - Agent policy updates
 * - /ws/petri - Real-time P3Net simulation and validation
 * 
 * @author Obvian Labs
 * @since Phase 26.2
 */
@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {
    
    @Autowired
    private MemorySyncWebSocketHandler memorySyncHandler;
    
    @Autowired
    private ExecutionWebSocketHandler executionHandler;
    
    @Autowired(required = false)
    private AgentPolicyWebSocketHandler policyHandler;

    @Autowired
    private PetriNetWebSocketHandler petriNetHandler;
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker with heartbeat configuration
        config.enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(new long[]{10000, 10000}) // 10 second heartbeat
            .setTaskScheduler(heartbeatTaskScheduler());
        
        // Designate the "/app" prefix for messages that are bound
        // for @MessageMapping-annotated methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for private messaging
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(getAllowedOrigins())
                .addInterceptors(new WebSocketSecurityInterceptor())
                .withSockJS()
                .setHeartbeatTime(25000) // 25 second heartbeat for SockJS
                .setDisconnectDelay(30000) // 30 second disconnect delay
                .setStreamBytesLimit(128 * 1024) // 128KB stream limit
                .setHttpMessageCacheSize(1000);
        
        // Native WebSocket endpoint (no SockJS)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(getAllowedOrigins())
                .addInterceptors(new WebSocketSecurityInterceptor());
        
        // Visual trace specific endpoints
        registry.addEndpoint("/ws/visual-trace")
                .setAllowedOriginPatterns(getAllowedOrigins())
                .addInterceptors(new WebSocketSecurityInterceptor())
                .withSockJS();
        
        // SSE fallback endpoint for browsers that don't support WebSockets properly
        registry.addEndpoint("/ws/sse-fallback")
                .setAllowedOriginPatterns(getAllowedOrigins())
                .addInterceptors(new WebSocketSecurityInterceptor())
                .withSockJS()
                .setStreamBytesLimit(256 * 1024) // 256KB for SSE-like streaming
                .setHttpMessageCacheSize(100);
        
        // Legacy endpoints for backward compatibility
        registry.addEndpoint("/ws/execution")
                .setAllowedOriginPatterns(getAllowedOrigins())
                .withSockJS();
        
        registry.addEndpoint("/api/v1/executions/ws")
                .setAllowedOriginPatterns(getAllowedOrigins())
                .withSockJS();
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register legacy execution WebSocket endpoint for backward compatibility
        registry.addHandler(executionHandler, "/ws/execution")
                .setAllowedOrigins("*");
        
        // Register new trace streaming endpoints
        registry.addHandler(executionHandler, "/ws/traces/{executionId}/events")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketSecurityInterceptor());
        
        registry.addHandler(executionHandler, "/ws/traces/{executionId}/playback")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketSecurityInterceptor());
        
        registry.addHandler(executionHandler, "/ws/traces/{executionId}/breakpoints")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketSecurityInterceptor());
        
        // Register memory sync WebSocket endpoint for real-time agent coordination
        registry.addHandler(memorySyncHandler, "/ws/memory-sync")
                .setAllowedOrigins("*") // Configure origins as needed for security
                .withSockJS(); // Enable SockJS fallback for broader browser support
        
        // Register memory sync endpoint without SockJS for native WebSocket clients
        registry.addHandler(memorySyncHandler, "/ws/memory-sync/native")
                .setAllowedOrigins("*");
        
        // Register agent policy WebSocket endpoints (if available)
        if (policyHandler != null) {
            registry.addHandler(policyHandler, "/ws/policy")
                    .setAllowedOrigins("*")
                    .addInterceptors(new WebSocketSecurityInterceptor())
                    .withSockJS();
            
            registry.addHandler(policyHandler, "/ws/policy/native")
                    .setAllowedOrigins("*")
                    .addInterceptors(new WebSocketSecurityInterceptor());
        }

        // Register P3Net WebSocket endpoints for real-time simulation and validation
        registry.addHandler(petriNetHandler, "/ws/petri")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketSecurityInterceptor())
                .withSockJS(); // Enable SockJS fallback for broader browser support

        registry.addHandler(petriNetHandler, "/ws/petri/native")
                .setAllowedOrigins("*")
                .addInterceptors(new WebSocketSecurityInterceptor());
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Configure thread pool for processing incoming messages
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(8)
            .keepAliveSeconds(60);
    }
    
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Configure thread pool for sending messages to clients
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(8)
            .keepAliveSeconds(60);
    }
    
    /**
     * Task scheduler for WebSocket heartbeats and periodic tasks
     */
    @Bean
    public TaskScheduler heartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
    
    /**
     * Configure WebSocket container settings
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192); // 8KB text message buffer
        container.setMaxBinaryMessageBufferSize(8192); // 8KB binary message buffer
        container.setMaxSessionIdleTimeout(300000L); // 5 minute idle timeout
        return container;
    }
    
    @Value("${websocket.allowed-origins:http://localhost:3000,http://localhost:8080,http://127.0.0.1:3000,http://127.0.0.1:8080}")
    private String allowedOriginsConfig;
    
    private String[] getAllowedOrigins() {
        if (isDevProfile()) {
            return new String[]{"*"}; // Allow all origins in development
        }
        return allowedOriginsConfig.split(",");
    }
    
    @Autowired
    private org.springframework.core.env.Environment environment;
    
    private boolean isDevProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("development".equals(profile) || "dev".equals(profile) || "test".equals(profile) ||
                profile.contains("dev") || profile.contains("local") || profile.contains("test")) {
                return true;
            }
        }
        return false;
    }
}