package api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT authentication entry point for handling authentication failures.
 * Returns structured error responses for unauthorized requests.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, Object> error = new HashMap<>();
        
        error.put("code", "AUTHENTICATION_REQUIRED");
        error.put("message", "Authentication required to access this resource");
        error.put("details", Map.of(
            "reason", "Missing or invalid authentication token",
            "suggestion", "Provide a valid JWT token in the Authorization header"
        ));
        error.put("correlationId", generateCorrelationId(request));
        error.put("timestamp", Instant.now().toString());
        
        errorResponse.put("error", error);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String generateCorrelationId(HttpServletRequest request) {
        String existingId = request.getHeader("X-Correlation-ID");
        if (existingId != null && !existingId.trim().isEmpty()) {
            return existingId;
        }
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}