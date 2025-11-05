package api.config;

import api.security.JwtService;
import api.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * OAuth2 configuration for Google authentication.
 * Enables seamless sign-in with Google accounts for test users.
 */
@Configuration
@EnableWebSecurity
@Profile("oauth2")
public class OAuth2Config {

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private final JwtService jwtService;
    private final UserService userService;

    public OAuth2Config(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(this.oidcUserService())
                )
                .successHandler(this.authenticationSuccessHandler())
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();
        
        return (OidcUserRequest userRequest) -> {
            // Delegate to the default implementation for loading user
            OidcUser oidcUser = delegate.loadUser(userRequest);
            
            // Extract user information from Google
            String email = oidcUser.getEmail();
            String name = oidcUser.getFullName();
            String googleId = oidcUser.getSubject();
            String picture = oidcUser.getPicture();
            
            // Create or update user in our database
            userService.findOrCreateByGoogle(googleId, email, name, picture);
            
            return oidcUser;
        };
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            
            // Extract user info
            String email = oAuth2User.getAttribute("email");
            String googleId = oAuth2User.getAttribute("sub");
            
            // Get user from database
            var user = userService.findByGoogleId(googleId);
            
            // Generate JWT token
            String token = jwtService.generateToken(user);
            
            // Check if user needs onboarding
            boolean needsOnboarding = !user.hasCompletedOnboarding();
            
            // Create cookie with JWT token
            Cookie jwtCookie = new Cookie("auth_token", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(request.isSecure());
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(jwtCookie);
            
            // Redirect to frontend with onboarding flag
            String redirectUrl = needsOnboarding 
                ? frontendUrl + "/onboarding" 
                : frontendUrl + "/dashboard";
            
            response.sendRedirect(redirectUrl);
        };
    }
}