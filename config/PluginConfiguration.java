package api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import plugins.PluginRegistry;
import plugins.slack.SlackPluginConfigurable;
import plugins.email.EmailPlugin;
import plugins.file.FilePlugin;
import plugins.ConfigurablePlugin;
import plugins.Plugin;

/**
 * Configuration class for registering plugins with the system
 */
@Configuration
public class PluginConfiguration {
    
    @Bean
    public PluginRegistry pluginRegistry() {
        PluginRegistry registry = PluginRegistry.getInstance();
        
        // Register Slack plugin (configurable)
        SlackPluginConfigurable slackPlugin = new SlackPluginConfigurable();
        registry.registerPlugin("slack", slackPlugin);
        
        // Register Email plugin
        EmailPlugin emailPlugin = new EmailPlugin();
        registry.registerPlugin("email", emailPlugin);
        
        // Register File plugin
        FilePlugin filePlugin = new FilePlugin();
        registry.registerPlugin("file", filePlugin);
        
        // Add more plugins as needed
        
        return registry;
    }
}