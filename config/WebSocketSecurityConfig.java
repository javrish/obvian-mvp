package api.config;

import api.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket security configuration for JWT-based authentication and channel authorization.
 * 
 * This configuration provides:
 * - JWT token validation for WebSocket connections
 * - Channel-level security for different message destinations
 * - User session management and access control
 * - Authorization checks for execution-specific channels
 * - Audit logging for security events
 * 
 * Security Model:
 * - All WebSocket connections require valid JWT authentication
 * - Users can only access their own execution channels
 * - Admin users have access to all channels
 * - Guest users have read-only access to public channels
 * 
 * Channel Security:
 * - /topic/visual-trace/{executionId} - Owner or shared access only
 * - /topic/playback/{executionId} - Owner or shared access only
 * - /topic/breakpoints/{executionId} - Owner or admin access only
 * - /queue/user/{username} - User-specific private channels
 * - /topic/public/* - Public channels (authenticated users only)
 * 
 * @author Obvian Labs
 * @since Phase 26.2
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSecurityConfig.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    // Session tracking for security auditing
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketChannelInterceptor());
    }
    
    /**
     * WebSocket channel interceptor for authentication and authorization
     */
    private class WebSocketChannelInterceptor implements ChannelInterceptor {
        
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            
            if (accessor != null) {
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    return handleConnect(message, accessor);
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    return handleSubscribe(message, accessor);
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    return handleSend(message, accessor);
                } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    return handleDisconnect(message, accessor);
                }
            }
            
            return message;
        }
        
        /**
         * Handle CONNECT command - authenticate user via JWT
         */
        private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
            try {
                String authToken = extractAuthToken(accessor);
                
                if (authToken == null) {
                    logger.warn("WebSocket connection attempt without authentication token");
                    throw new SecurityException("Authentication token required");
                }
                
                // Validate JWT token
                if (!jwtUtil.validateToken(authToken)) {
                    logger.warn("WebSocket connection attempt with invalid token");
                    throw new SecurityException("Invalid authentication token");
                }
                
                // Extract user information from token
                String username = jwtUtil.getUsernameFromToken(authToken);
                List<String> roles = jwtUtil.getRolesFromToken(authToken);
                List<GrantedAuthority> authorities = roles.stream()
                    .map(role -> (GrantedAuthority) () -> "ROLE_" + role)
                    .collect(java.util.stream.Collectors.toList());
                
                // Create authentication object
                Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Set user in accessor for session tracking
                accessor.setUser(authentication);
                
                // Track active session
                String sessionId = accessor.getSessionId();
                UserSession session = new UserSession(sessionId, username, authorities, System.currentTimeMillis());
                activeSessions.put(sessionId, session);
                
                logger.info("WebSocket connection authenticated: user={}, sessionId={}", username, sessionId);
                
                return message;
                
            } catch (Exception e) {
                logger.error("WebSocket authentication failed: {}", e.getMessage());
                throw new SecurityException("Authentication failed: " + e.getMessage());
            }
        }
        
        /**
         * Handle SUBSCRIBE command - authorize channel access
         */
        private Message<?> handleSubscribe(Message<?> message, StompHeaderAccessor accessor) {
            Principal user = accessor.getUser();
            if (user == null) {
                throw new SecurityException("User not authenticated");
            }
            
            String destination = accessor.getDestination();
            String username = user.getName();
            String sessionId = accessor.getSessionId();
            
            if (destination == null) {
                throw new SecurityException("Destination cannot be null");
            }
            
            // Check authorization for destination
            if (!isAuthorizedForDestination(username, destination, sessionId)) {
                logger.warn("User {} denied access to destination: {}", username, destination);
                throw new SecurityException("Access denied to destination: " + destination);
            }
            
            logger.debug("User {} authorized for destination: {}", username, destination);
            return message;
        }
        
        /**
         * Handle SEND command - authorize message sending
         */
        private Message<?> handleSend(Message<?> message, StompHeaderAccessor accessor) {
            Principal user = accessor.getUser();
            if (user == null) {
                throw new SecurityException("User not authenticated");
            }
            
            String destination = accessor.getDestination();
            String username = user.getName();
            
            if (destination == null) {
                throw new SecurityException("Destination cannot be null");
            }
            
            // Check if user can send to this destination
            if (!canSendToDestination(username, destination)) {
                logger.warn("User {} denied send access to destination: {}", username, destination);
                throw new SecurityException("Send access denied to destination: " + destination);
            }
            
            return message;
        }
        
        /**
         * Handle DISCONNECT command - clean up session
         */
        private Message<?> handleDisconnect(Message<?> message, StompHeaderAccessor accessor) {
            String sessionId = accessor.getSessionId();
            UserSession session = activeSessions.remove(sessionId);
            
            if (session != null) {
                logger.info("WebSocket session disconnected: user={}, sessionId={}, duration={}ms", 
                    session.getUsername(), sessionId, System.currentTimeMillis() - session.getConnectedAt());
            }
            
            return message;
        }
    }
    
    /**
     * Extract authentication token from STOMP headers
     */
    private String extractAuthToken(StompHeaderAccessor accessor) {
        // Try Authorization header first
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        
        // Try token parameter
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }
        
        // Try X-Auth-Token header
        List<String> xAuthTokenHeaders = accessor.getNativeHeader("X-Auth-Token");
        if (xAuthTokenHeaders != null && !xAuthTokenHeaders.isEmpty()) {
            return xAuthTokenHeaders.get(0);
        }
        
        return null;
    }
    
    /**
     * Check if user is authorized to subscribe to a destination
     */
    private boolean isAuthorizedForDestination(String username, String destination, String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session == null) {
            return false;
        }
        
        // Admin users have access to everything
        if (hasRole(session.getAuthorities(), "ADMIN")) {
            return true;
        }
        
        // Check specific destination patterns
        if (destination.startsWith("/topic/visual-trace/")) {
            String executionId = extractExecutionIdFromDestination(destination, "/topic/visual-trace/");
            return canAccessExecution(username, executionId, session.getAuthorities());
        }
        
        if (destination.startsWith("/topic/playback/")) {
            String executionId = extractExecutionIdFromDestination(destination, "/topic/playback/");
            return canAccessExecution(username, executionId, session.getAuthorities());
        }
        
        if (destination.startsWith("/topic/breakpoints/")) {
            String executionId = extractExecutionIdFromDestination(destination, "/topic/breakpoints/");
            // Breakpoints require owner or admin access
            return canModifyExecution(username, executionId, session.getAuthorities());
        }
        
        if (destination.startsWith("/queue/user/")) {
            String targetUser = destination.substring("/queue/user/".length());
            // Users can only access their own private queues
            return username.equals(targetUser) || hasRole(session.getAuthorities(), "ADMIN");
        }
        
        if (destination.startsWith("/topic/public/")) {
            // Public topics are accessible to all authenticated users
            return true;
        }
        
        if (destination.startsWith("/topic/system/")) {
            // System topics require admin access
            return hasRole(session.getAuthorities(), "ADMIN");
        }
        
        // Default: deny access to unknown destinations
        logger.warn("Unknown destination pattern: {}", destination);
        return false;
    }
    
    /**
     * Check if user can send messages to a destination
     */
    private boolean canSendToDestination(String username, String destination) {
        // Users can generally only send to /app/* destinations (handled by controllers)
        if (destination.startsWith("/app/")) {
            return true;
        }
        
        // Direct topic/queue sending is restricted
        if (destination.startsWith("/topic/") || destination.startsWith("/queue/")) {
            // Only allow sending to user's own queue
            if (destination.equals("/queue/user/" + username)) {
                return true;
            }
            // Otherwise deny direct topic/queue sends
            return false;
        }
        
        return false;
    }
    
    /**
     * Check if user can access execution data (read access)
     */
    private boolean canAccessExecution(String username, String executionId, List<GrantedAuthority> authorities) {
        // Admin users can access any execution
        if (hasRole(authorities, "ADMIN")) {
            return true;
        }
        
        // TODO: Implement proper execution ownership/sharing logic
        // For now, allow access for authenticated users
        // In production, this should check:
        // - Execution ownership (user created the execution)
        // - Shared access (execution shared with user's team/organization)
        // - Role-based access (user has viewer role for this execution)
        
        return true; // Temporary - replace with actual authorization logic
    }
    
    /**
     * Check if user can modify execution (write access)
     */
    private boolean canModifyExecution(String username, String executionId, List<GrantedAuthority> authorities) {
        // Admin users can modify any execution
        if (hasRole(authorities, "ADMIN")) {
            return true;
        }
        
        // TODO: Implement proper execution modification logic
        // For now, allow modification for authenticated users
        // In production, this should check:
        // - Execution ownership (user owns the execution)
        // - Modification permissions (user has edit role for this execution)
        
        return true; // Temporary - replace with actual authorization logic
    }
    
    /**
     * Extract execution ID from destination path
     */
    private String extractExecutionIdFromDestination(String destination, String prefix) {
        if (!destination.startsWith(prefix)) {
            return null;
        }
        
        String remaining = destination.substring(prefix.length());
        int slashIndex = remaining.indexOf('/');
        
        if (slashIndex == -1) {
            return remaining;
        } else {
            return remaining.substring(0, slashIndex);
        }
    }
    
    /**
     * Check if user has a specific role
     */
    private boolean hasRole(List<GrantedAuthority> authorities, String role) {
        if (authorities == null) {
            return false;
        }
        
        String roleWithPrefix = "ROLE_" + role;
        return authorities.stream()
            .anyMatch(auth -> role.equals(auth.getAuthority()) || roleWithPrefix.equals(auth.getAuthority()));
    }
    
    /**
     * Get active session count for monitoring
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Get session information for a user
     */
    public UserSession getUserSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Force disconnect a session (admin function)
     */
    public boolean disconnectSession(String sessionId) {
        UserSession session = activeSessions.remove(sessionId);
        if (session != null) {
            logger.info("Admin force disconnect: sessionId={}, user={}", sessionId, session.getUsername());
            return true;
        }
        return false;
    }
    
    /**
     * User session information for tracking and auditing
     */
    public static class UserSession {
        private final String sessionId;
        private final String username;
        private final List<GrantedAuthority> authorities;
        private final long connectedAt;
        private volatile long lastActivity;
        
        public UserSession(String sessionId, String username, List<GrantedAuthority> authorities, long connectedAt) {
            this.sessionId = sessionId;
            this.username = username;
            this.authorities = authorities;
            this.connectedAt = connectedAt;
            this.lastActivity = connectedAt;
        }
        
        public String getSessionId() { return sessionId; }
        public String getUsername() { return username; }
        public List<GrantedAuthority> getAuthorities() { return authorities; }
        public long getConnectedAt() { return connectedAt; }
        public long getLastActivity() { return lastActivity; }
        
        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        public boolean hasRole(String role) {
            String roleWithPrefix = "ROLE_" + role;
            return authorities.stream()
                .anyMatch(auth -> role.equals(auth.getAuthority()) || roleWithPrefix.equals(auth.getAuthority()));
        }
        
        public long getSessionDuration() {
            return System.currentTimeMillis() - connectedAt;
        }
    }
}