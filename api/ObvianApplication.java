package api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot application for Obvian backend services.
 *
 * This is a minimal implementation to get the server running.
 * SpringDoc OpenAPI is temporarily excluded to avoid version conflicts.
 */
@SpringBootApplication(exclude = {
    org.springdoc.core.configuration.SpringDocConfiguration.class,
    org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration.class
})
@ComponentScan(basePackages = {"api", "core", "memory", "plugins"})
public class ObvianApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObvianApplication.class, args);
    }
}