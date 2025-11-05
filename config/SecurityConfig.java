package api.config;

import api.middleware.RateLimitingFilter;
import api.middleware.RequestValidationFilter;
import api.security.JwtAuthenticationEntryPoint;
import api.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the Obvian API.
 * Configures JWT authentication, CORS, and endpoint security.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!minimal-startup & !development")
public class SecurityConfig {

    @Value("${spring.profiles.active:development}")
    private String activeProfile;

    @Autowired
    private Environment environment;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private RequestValidationFilter requestValidationFilter;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // Debug logging to understand profile detection
        boolean devMode = isDevProfile();
        System.out.println("=== SECURITY CONFIG DEBUG ===");
        System.out.println("Active profiles: " + String.join(",", environment.getActiveProfiles()));
        System.out.println("Default profiles: " + String.join(",", environment.getDefaultProfiles()));
        System.out.println("Dev mode detected: " + devMode);
        System.out.println("==============================");

        // Development mode - completely disable security for frontend development
        // Check for dev profiles using Spring's Environment API
        if (devMode) {
            System.out.println("CONFIGURING DEVELOPMENT SECURITY (completely disabled)");
            http.authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            )
            .anonymous(anonymous -> anonymous.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        } else {
            System.out.println("CONFIGURING PRODUCTION SECURITY (authentication required)");
            // Production mode - full authentication required
            http.authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/health", "/metrics", "/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/prompts/health", "/api/v1/prompts/actions").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .requestMatchers("/ws/**", "/ws/execution/**", "/api/v1/executions/ws/**").permitAll() // Allow WebSocket endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            // Add filters in correct order (only in production)
            http.addFilterBefore(requestValidationFilter, UsernamePasswordAuthenticationFilter.class);
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            http.addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);
        }

        return http.build();
    }

    @Autowired
    private CorsConfig corsConfig;

    private org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        return corsConfig.corsConfigurationSource();
    }
    
    private boolean isDevProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        String[] defaultProfiles = environment.getDefaultProfiles();
        
        System.out.println("DEBUG: Checking profiles for dev mode");
        System.out.println("DEBUG: Active profiles: " + String.join(",", activeProfiles));
        System.out.println("DEBUG: Default profiles: " + String.join(",", defaultProfiles));
        
        // Check active profiles first
        for (String profile : activeProfiles) {
            System.out.println("DEBUG: Checking active profile: " + profile);
            if ("development".equals(profile) || "dev".equals(profile) || "test".equals(profile) ||
                profile.contains("dev") || profile.contains("local") || profile.contains("test")) {
                System.out.println("DEBUG: Found dev/test profile: " + profile);
                return true;
            }
        }
        
        // Also check @Value property for additional safety
        System.out.println("DEBUG: activeProfile from @Value: " + activeProfile);
        if (activeProfile != null && (activeProfile.contains("dev") || activeProfile.contains("local") || activeProfile.contains("test"))) {
            System.out.println("DEBUG: Found dev/test in @Value property");
            return true;
        }
        
        System.out.println("DEBUG: No dev/test profiles found, defaulting to production mode");
        return false;
    }
}