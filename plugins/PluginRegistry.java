package plugins;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.Optional;

/**
 * Registry for managing plugin instances and discovery.
 *
 * This is a minimal implementation to resolve compilation dependencies.
 */
@Component
public class PluginRegistry {

    private static final Logger logger = LoggerFactory.getLogger(PluginRegistry.class);

    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();

    /**
     * Register a plugin in the registry
     */
    public void registerPlugin(Plugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        String pluginId = plugin.getId();
        if (pluginId == null || pluginId.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin ID cannot be null or empty");
        }

        plugins.put(pluginId, plugin);
        logger.info("Registered plugin: {} ({})", plugin.getName(), pluginId);
    }

    /**
     * Unregister a plugin from the registry
     */
    public void unregisterPlugin(String pluginId) {
        Plugin removed = plugins.remove(pluginId);
        if (removed != null) {
            logger.info("Unregistered plugin: {} ({})", removed.getName(), pluginId);
        }
    }

    /**
     * Get a plugin by ID
     */
    public Optional<Plugin> getPlugin(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }

    /**
     * Get all registered plugins
     */
    public Collection<Plugin> getAllPlugins() {
        return plugins.values();
    }

    /**
     * Check if a plugin is registered
     */
    public boolean isRegistered(String pluginId) {
        return plugins.containsKey(pluginId);
    }

    /**
     * Get the number of registered plugins
     */
    public int getPluginCount() {
        return plugins.size();
    }

    /**
     * Check health of all plugins
     */
    public boolean areAllPluginsHealthy() {
        return plugins.values().stream().allMatch(Plugin::isHealthy);
    }

    /**
     * Clear all registered plugins
     */
    public void clear() {
        int count = plugins.size();
        plugins.clear();
        logger.info("Cleared {} plugins from registry", count);
    }
}