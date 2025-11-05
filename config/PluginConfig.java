package api.config;

import plugins.Plugin;
import plugins.PluginRegistry;
import plugins.PluginRouter;
import plugins.email.EmailPlugin;
import plugins.file.FilePlugin;
import plugins.reminder.ReminderPlugin;
import plugins.slack.SlackPlugin;
import core.ExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

/**
 * Centralized Spring configuration for plugin management.
 * Provides a single PluginRouter instance with all plugins registered.
 * Designed to support future dynamic plugin loading.
 */
@Configuration
public class PluginConfig {
    
    /**
     * PluginRegistry bean for centralized plugin management.
     * Handles plugin discovery, registration, and lifecycle management.
     */
    @Bean
    public PluginRegistry pluginRegistry() {
        System.out.println("=== CREATING PLUGINREGISTRY BEAN ===");
        return new PluginRegistry();
    }
    
    /**
     * Create and configure the central PluginRouter with all available plugins.
     * Uses the standardized factory to ensure consistent initialization.
     */
    @Bean
    @Primary
    public PluginRouter pluginRouter(PluginRegistry pluginRegistry) {
        System.out.println("=== CREATING PLUGINROUTER BEAN ===");
        PluginRouter router = plugins.PluginRouterFactory.createFromRegistry(pluginRegistry);
        
        logRegisteredPlugins(router);
        
        return router;
    }
    
    // Plugin registration is now handled by PluginRouterFactory for consistency
    
    /**
     * Log all registered plugins at startup for debugging and validation.
     */
    private void logRegisteredPlugins(PluginRouter router) {
        System.out.println("=== PluginRouter Configuration Complete ===");
        System.out.println("EchoPlugin registered: " + router.hasPlugin("EchoPlugin"));
        System.out.println("EmailPlugin registered: " + router.hasPlugin("EmailPlugin"));
        System.out.println("FilePlugin registered: " + router.hasPlugin("FilePlugin"));
        System.out.println("ReminderPlugin registered: " + router.hasPlugin("ReminderPlugin"));
        System.out.println("SlackPlugin registered: " + router.hasPlugin("SlackPlugin"));
        System.out.println("=== Total plugins ready for execution ===");
    }
    
    /**
     * Define core plugins as Spring beans for future auto-discovery.
     * TODO: Move these to separate configuration classes for better organization.
     */
    
    // EchoPlugin commented out - create as proper plugin class if needed
    // @Bean
    // public Plugin echoPlugin() {
    //     return new EchoPlugin();
    // }
    
    @Bean
    public Plugin emailPlugin() {
        return new EmailPlugin();
    }
    
    @Bean
    public Plugin filePlugin() {
        return new FilePlugin();
    }
    
    @Bean
    public Plugin reminderPlugin() {
        return new ReminderPlugin();
    }
    
    @Bean
    public Plugin slackPlugin() {
        return new SlackPlugin();
    }
    
    // EchoPlugin is now provided by PluginRouterFactory for consistency
}