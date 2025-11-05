package api.config;

import api.security.JwtService;
import api.service.security.AuthorizationService;
import api.service.security.DagExecutionAuditor;
import api.service.security.DagSecurityService;
import api.service.security.DagExecutionValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enhanced security configuration for Obvian API.
 * Provides additional security services and configurations beyond the basic Spring Security setup.
 * 
 * This configuration implements defense-in-depth security patterns:
 * - Authorization service for centralized user context validation
 * - Comprehensive audit logging for security events
 * - Input validation and sanitization
 * - Resource ownership verification
 * - OWASP-compliant security controls
 * 
 * Security Features:
 * - JWT token validation with role-based access control
 * - Resource ownership verification for DAG executions and memory access
 * - Comprehensive audit logging for all security-sensitive operations
 * - Input sanitization to prevent injection attacks
 * - Rate limiting and request validation
 * - Plugin execution security boundaries
 * 
 * OWASP Top 10 2021 Mitigations:
 * - A01 Broken Access Control: Proper authorization checks
 * - A02 Cryptographic Failures: Secure JWT token handling
 * - A03 Injection: Input validation and sanitization
 * - A05 Security Misconfiguration: Secure defaults and configuration
 * - A06 Vulnerable Components: Security-focused component design
 * - A09 Security Logging Failures: Comprehensive audit logging
 * - A10 SSRF: Input validation for external requests
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityEnhancementConfig {
    
    /**
     * Central authorization service for user context validation and access control.
     * This service provides secure alternatives to header-based user context parsing.
     */
    @Bean
    @Primary
    public AuthorizationService authorizationService(DagExecutionAuditor auditor, JwtService jwtService) {
        return new AuthorizationService(auditor, jwtService);
    }
    
    /**
     * Security audit logger for comprehensive security event logging.
     * Logs all security-sensitive operations for monitoring and compliance.
     */
    @Bean
    @ConditionalOnMissingBean
    public DagExecutionAuditor dagExecutionAuditor() {
        return new DagExecutionAuditor();
    }
    
    /**
     * DAG security service for plugin permissions and execution security.
     * Provides authorization checks and security validation for DAG operations.
     */
    @Bean
    @ConditionalOnMissingBean
    public DagSecurityService dagSecurityService() {
        return new DagSecurityService();
    }
    
    /**
     * DAG execution validator for comprehensive request validation.
     * Validates DAG structure, size limits, and security constraints.
     */
    @Bean
    @ConditionalOnMissingBean
    public DagExecutionValidator dagExecutionValidator(DagSecurityService dagSecurityService) {
        return new DagExecutionValidator();
    }
    
    /**
     * Security configuration properties for customizing security behavior.
     * These properties can be overridden via application.properties.
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityConfigurationProperties securityConfigurationProperties() {
        return new SecurityConfigurationProperties();
    }
    
    /**
     * Configuration properties for security settings.
     */
    public static class SecurityConfigurationProperties {
        
        // Maximum allowed DAG nodes
        private int maxDagNodes = 100;
        
        // Maximum allowed context size in bytes
        private int maxContextSize = 1048576; // 1MB
        
        // Maximum execution timeout in milliseconds
        private long maxExecutionTimeMs = 300000; // 5 minutes
        
        // Maximum prompt length for security validation
        private int maxPromptLength = 5000;
        
        // Enable enhanced input sanitization
        private boolean enableEnhancedSanitization = true;
        
        // Enable comprehensive audit logging
        private boolean enableComprehensiveAuditLogging = true;
        
        // Enable rate limiting
        private boolean enableRateLimiting = true;
        
        // Default rate limit per user per minute
        private int defaultRateLimit = 60;
        
        // Enable plugin security boundaries
        private boolean enablePluginSecurity = true;
        
        // Enable memory access controls
        private boolean enableMemoryAccessControls = true;
        
        public int getMaxDagNodes() { return maxDagNodes; }
        public void setMaxDagNodes(int maxDagNodes) { this.maxDagNodes = maxDagNodes; }
        
        public int getMaxContextSize() { return maxContextSize; }
        public void setMaxContextSize(int maxContextSize) { this.maxContextSize = maxContextSize; }
        
        public long getMaxExecutionTimeMs() { return maxExecutionTimeMs; }
        public void setMaxExecutionTimeMs(long maxExecutionTimeMs) { this.maxExecutionTimeMs = maxExecutionTimeMs; }
        
        public int getMaxPromptLength() { return maxPromptLength; }
        public void setMaxPromptLength(int maxPromptLength) { this.maxPromptLength = maxPromptLength; }
        
        public boolean isEnableEnhancedSanitization() { return enableEnhancedSanitization; }
        public void setEnableEnhancedSanitization(boolean enableEnhancedSanitization) { 
            this.enableEnhancedSanitization = enableEnhancedSanitization; 
        }
        
        public boolean isEnableComprehensiveAuditLogging() { return enableComprehensiveAuditLogging; }
        public void setEnableComprehensiveAuditLogging(boolean enableComprehensiveAuditLogging) { 
            this.enableComprehensiveAuditLogging = enableComprehensiveAuditLogging; 
        }
        
        public boolean isEnableRateLimiting() { return enableRateLimiting; }
        public void setEnableRateLimiting(boolean enableRateLimiting) { 
            this.enableRateLimiting = enableRateLimiting; 
        }
        
        public int getDefaultRateLimit() { return defaultRateLimit; }
        public void setDefaultRateLimit(int defaultRateLimit) { 
            this.defaultRateLimit = defaultRateLimit; 
        }
        
        public boolean isEnablePluginSecurity() { return enablePluginSecurity; }
        public void setEnablePluginSecurity(boolean enablePluginSecurity) { 
            this.enablePluginSecurity = enablePluginSecurity; 
        }
        
        public boolean isEnableMemoryAccessControls() { return enableMemoryAccessControls; }
        public void setEnableMemoryAccessControls(boolean enableMemoryAccessControls) { 
            this.enableMemoryAccessControls = enableMemoryAccessControls; 
        }
    }
}