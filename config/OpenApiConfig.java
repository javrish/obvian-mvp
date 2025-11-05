package api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for comprehensive API documentation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI obvianOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Obvian DAG Execution API")
                        .description("Comprehensive REST API for executing DAGs, natural language prompts, and business rules with the Obvian system. " +
                                   "This API provides endpoints for prompt execution, DAG execution, business rule management, status monitoring, memory management, " +
                                   "and plugin discovery with full authentication, rate limiting, and observability features.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Obvian Development Team")
                                .email("dev@obvian.com")
                                .url("https://obvian.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("https://api.obvian.com")
                                .description("Production server"),
                        new Server()
                                .url("https://staging-api.obvian.com")
                                .description("Staging server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth")
                        .addList("apiKeyAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token authentication"))
                        .addSecuritySchemes("apiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API key authentication")))
                .tags(List.of(
                        new Tag()
                                .name("Prompts")
                                .description("Natural language prompt execution endpoints"),
                        new Tag()
                                .name("DAGs")
                                .description("Structured DAG execution endpoints"),
                        new Tag()
                                .name("Command Palette")
                                .description("Contextual command palette and behavioral learning endpoints"),
                        new Tag()
                                .name("Executions")
                                .description("Execution monitoring and status endpoints"),
                        new Tag()
                                .name("Memory")
                                .description("User memory and context management endpoints"),
                        new Tag()
                                .name("Plugins")
                                .description("Plugin discovery and management endpoints"),
                        new Tag()
                                .name("Plugin Discovery")
                                .description("Intelligent plugin discovery and recommendation endpoints with AI-powered analysis, personalization, and learning capabilities"),
                        new Tag()
                                .name("Business Rules")
                                .description("Natural Language Business Rule Engine endpoints for parsing, creating, validating, and executing business rules with conflict detection"),
                        new Tag()
                                .name("Health")
                                .description("System health and monitoring endpoints")));
    }
}