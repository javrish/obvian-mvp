package api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration to allow access to Petri API endpoints for testing
 */
@Configuration
@EnableWebSecurity
public class PetriSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain petriApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/petri/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/petri/**").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}