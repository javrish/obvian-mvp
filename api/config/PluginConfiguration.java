package api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import plugins.PluginRegistry;
import plugins.github.GitHubPlugin;

/**
 * Spring configuration for plugin registration.
 *
 * <p>Automatically registers all available plugins with the PluginRegistry on startup.
 */
@Configuration
public class PluginConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(PluginConfiguration.class);

  private final PluginRegistry pluginRegistry;
  private final GitHubPlugin githubPlugin;

  public PluginConfiguration(PluginRegistry pluginRegistry, GitHubPlugin githubPlugin) {
    this.pluginRegistry = pluginRegistry;
    this.githubPlugin = githubPlugin;
  }

  /**
   * Register all plugins on application startup.
   */
  @PostConstruct
  public void registerPlugins() {
    logger.info("Registering plugins...");

    // Register GitHub plugin
    pluginRegistry.registerPlugin(githubPlugin);
    logger.info(
        "Registered plugin: {} v{} (id={})",
        githubPlugin.getName(),
        githubPlugin.getVersion(),
        githubPlugin.getId());

    // Future plugins would be registered here:
    // pluginRegistry.registerPlugin(gitlabPlugin);
    // pluginRegistry.registerPlugin(bitbucketPlugin);

    logger.info("Plugin registration complete. Total plugins: {}", pluginRegistry.getPluginCount());
  }
}
