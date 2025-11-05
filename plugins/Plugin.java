package plugins;

import java.util.Map;

/**
 * Base interface for all plugins in the Obvian system.
 *
 * Plugins provide extensible task execution capabilities that can be
 * integrated into DAG workflows and Petri net executions.
 */
public interface Plugin {

    /**
     * Get the unique identifier for this plugin
     */
    String getId();

    /**
     * Get the human-readable name of this plugin
     */
    String getName();

    /**
     * Get the version of this plugin
     */
    String getVersion();

    /**
     * Execute the plugin with the given input parameters
     *
     * @param parameters Input parameters for plugin execution
     * @return Execution result containing output data and status
     */
    PluginResult execute(Map<String, Object> parameters);

    /**
     * Validate the input parameters for this plugin
     *
     * @param parameters Input parameters to validate
     * @return Validation result with any errors or warnings
     */
    PluginValidationResult validate(Map<String, Object> parameters);

    /**
     * Check if this plugin is healthy and ready to execute
     */
    boolean isHealthy();

    /**
     * Get the plugin configuration schema (JSON Schema format)
     */
    String getConfigurationSchema();
}