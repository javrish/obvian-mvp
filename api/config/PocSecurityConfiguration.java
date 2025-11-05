/* Copyright (c) 2024 Obvian. All rights reserved. */
package api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * POC Security Configuration - Adds warning headers and security awareness
 * for the Petri Net DAG proof-of-concept implementation.
 *
 * <p>This configuration ensures all API responses include appropriate POC warnings
 * and security headers to make users aware this is a demonstration environment.
 */
@Configuration
public class PocSecurityConfiguration {

  /** POC Environment Warning Filter that adds security awareness headers */
  public static class PocWarningFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain)
        throws ServletException, IOException {

      // Add POC warning headers
      response.setHeader("X-POC-Environment", "true");
      response.setHeader("X-POC-Warning", "This is a proof-of-concept implementation");
      response.setHeader(
          "X-POC-Limitations", "Performance and features limited for demonstration purposes");
      response.setHeader("X-POC-Network-Limit", "Max 30 places, 30 transitions for optimal performance");

      // Security awareness headers
      response.setHeader("X-Content-Type-Options", "nosniff");
      response.setHeader("X-Frame-Options", "DENY");
      response.setHeader("X-XSS-Protection", "1; mode=block");
      response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

      // POC-specific CSP (more permissive for development)
      response.setHeader(
          "Content-Security-Policy",
          "default-src 'self' 'unsafe-inline' 'unsafe-eval' data: blob:; "
              + "connect-src 'self' ws: wss: http://localhost:* https://*.obvian.com; "
              + "img-src 'self' data: blob: https:; "
              + "font-src 'self' data: https://fonts.gstatic.com; "
              + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
              + "script-src 'self' 'unsafe-inline' 'unsafe-eval';");

      // Cache control for POC (prevent caching in production-like scenarios)
      response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
      response.setHeader("Pragma", "no-cache");
      response.setHeader("Expires", "0");

      // Accessibility headers
      response.setHeader("X-Accessibility-Compliance", "WCAG-2.1-AA");
      response.setHeader("X-Accessibility-Features", "screen-reader,keyboard-nav,high-contrast,color-blind-support");

      // Performance optimization headers for POC
      response.setHeader("X-Performance-Optimized", "network-size-30-limit");
      response.setHeader("X-Performance-Target", "demonstration-purposes");

      // Add CORS headers for POC environment
      response.setHeader("Access-Control-Allow-Origin", "*");
      response.setHeader(
          "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
      response.setHeader(
          "Access-Control-Allow-Headers",
          "Content-Type, Authorization, X-Requested-With, X-POC-Client-Version");
      response.setHeader("Access-Control-Expose-Headers",
          "X-POC-Environment, X-POC-Warning, X-POC-Limitations, X-Performance-Target");

      // Handle preflight requests
      if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        response.setStatus(HttpServletResponse.SC_OK);
        return;
      }

      filterChain.doFilter(request, response);
    }
  }

  /** Register the POC warning filter with highest priority */
  @Bean
  public FilterRegistrationBean<PocWarningFilter> pocWarningFilter() {
    FilterRegistrationBean<PocWarningFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new PocWarningFilter());
    registrationBean.addUrlPatterns("/api/*", "/v3/*", "/swagger-ui/*", "/actuator/*");
    registrationBean.setName("pocWarningFilter");
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registrationBean;
  }
}