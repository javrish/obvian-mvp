package api.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.ConsoleAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Configuration for structured logging with correlation IDs.
 */
@Configuration
public class LoggingConfig {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String EXECUTION_ID_MDC_KEY = "executionId";

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    /**
     * Filter to add correlation IDs to all requests.
     */
    public static class CorrelationIdFilter extends OncePerRequestFilter {

        @Override
        public void doFilterInternal(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
            
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            }

            // Add correlation ID to MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            
            // Add correlation ID to response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            try {
                filterChain.doFilter(request, response);
            } finally {
                // Clean up MDC
                MDC.clear();
            }
        }
    }

    /**
     * Utility class for managing logging context.
     */
    public static class LoggingContext {
        
        public static void setUserId(String userId) {
            if (userId != null) {
                MDC.put(USER_ID_MDC_KEY, userId);
            } else {
                MDC.remove(USER_ID_MDC_KEY);
            }
        }

        public static void setExecutionId(String executionId) {
            if (executionId != null) {
                MDC.put(EXECUTION_ID_MDC_KEY, executionId);
            } else {
                MDC.remove(EXECUTION_ID_MDC_KEY);
            }
        }

        public static String getCorrelationId() {
            return MDC.get(CORRELATION_ID_MDC_KEY);
        }

        public static String getUserId() {
            return MDC.get(USER_ID_MDC_KEY);
        }

        public static String getExecutionId() {
            return MDC.get(EXECUTION_ID_MDC_KEY);
        }

        public static void clear() {
            MDC.clear();
        }

        public static void clearExecutionId() {
            MDC.remove(EXECUTION_ID_MDC_KEY);
        }
    }
}