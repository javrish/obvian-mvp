package api.util;

import api.security.JwtUtil;
import api.security.RateLimitConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;

/**
 * Utility to generate JWT tokens for testing and development
 */
@SpringBootApplication
@ComponentScan(basePackages = {"api.security"})
public class TokenGenerator implements CommandLineRunner {

    private final JwtUtil jwtUtil;

    public TokenGenerator(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public static void main(String[] args) {
        SpringApplication.run(TokenGenerator.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Obvian API JWT Token Generator ===\n");
        
        // Generate admin token
        String adminToken = jwtUtil.generateToken(
            "admin-001", 
            "admin", 
            Arrays.asList("ADMIN"), 
            Arrays.asList("prompts:execute", "dags:execute", "memory:read", "memory:write", "admin:all"),
            RateLimitConfig.getDefault()
        );
        
        // Generate user token
        String userToken = jwtUtil.generateToken(
            "user-001", 
            "testuser", 
            Arrays.asList("USER"), 
            Arrays.asList("prompts:execute", "dags:execute", "memory:read", "memory:write"),
            RateLimitConfig.getDefault()
        );
        
        System.out.println("Admin Token (full permissions):");
        System.out.println("Bearer " + adminToken);
        System.out.println();
        
        System.out.println("User Token (standard permissions):");
        System.out.println("Bearer " + userToken);
        System.out.println();
        
        System.out.println("Usage:");
        System.out.println("curl -H \"Authorization: Bearer " + userToken + "\" http://localhost:8080/api/v1/health");
        System.out.println();
        System.out.println("These tokens are valid for 24 hours.");
    }
}