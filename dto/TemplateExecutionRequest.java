package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for template execution operations.
 * Contains parameters and configuration for executing a template.
 * 
 * @author Obvian Labs
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to execute a template with specific parameters")
public class TemplateExecutionRequest {

    @NotBlank(message = "Template ID is required")
    @Schema(description = "Template identifier to execute", example = "daily-standup-report", required = true)
    private String templateId;

    @NotNull(message = "Parameters map is required (can be empty)")
    @Schema(description = "Parameter values for template execution", required = true)
    private Map<String, Object> parameters;

    @Schema(description = "Execution context variables")
    private Map<String, Object> context;

    @Schema(description = "Whether to perform dry-run (validate without executing)")
    private boolean dryRun = false;

    @Schema(description = "Whether to enable detailed tracing")
    private boolean trace = false;

    @Schema(description = "Whether to execute asynchronously")
    private boolean async = false;

    @Schema(description = "Execution timeout in milliseconds")
    private Long timeoutMs;

    @Schema(description = "Whether to validate plugins before execution")
    private boolean validatePlugins = true;

    @Schema(description = "Webhook URL for execution status notifications")
    private String webhookUrl;

    @Schema(description = "Execution priority level")
    private ExecutionPriority priority = ExecutionPriority.NORMAL;

    @Schema(description = "Maximum number of concurrent nodes to execute")
    private Integer maxConcurrency;

    @Schema(description = "Whether to fail fast on first error")
    private boolean failFast = true;

    @Schema(description = "Whether to enable debug mode")
    private boolean debugMode = false;

    @Schema(description = "Custom execution options")
    private ExecutionOptions executionOptions;

    // Constructors
    public TemplateExecutionRequest() {}

    public TemplateExecutionRequest(String templateId, Map<String, Object> parameters) {
        this.templateId = templateId;
        this.parameters = parameters;
    }

    // Getters and Setters
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public boolean isTrace() { return trace; }
    public void setTrace(boolean trace) { this.trace = trace; }

    public boolean isAsync() { return async; }
    public void setAsync(boolean async) { this.async = async; }

    public Long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Long timeoutMs) { this.timeoutMs = timeoutMs; }

    public boolean isValidatePlugins() { return validatePlugins; }
    public void setValidatePlugins(boolean validatePlugins) { this.validatePlugins = validatePlugins; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public ExecutionPriority getPriority() { return priority; }
    public void setPriority(ExecutionPriority priority) { this.priority = priority; }

    public Integer getMaxConcurrency() { return maxConcurrency; }
    public void setMaxConcurrency(Integer maxConcurrency) { this.maxConcurrency = maxConcurrency; }

    public boolean isFailFast() { return failFast; }
    public void setFailFast(boolean failFast) { this.failFast = failFast; }

    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }

    public ExecutionOptions getExecutionOptions() { return executionOptions; }
    public void setExecutionOptions(ExecutionOptions executionOptions) { this.executionOptions = executionOptions; }

    // Nested classes

    @Schema(description = "Execution priority levels")
    public enum ExecutionPriority {
        @Schema(description = "Low priority execution")
        LOW,
        @Schema(description = "Normal priority execution")
        NORMAL,
        @Schema(description = "High priority execution")
        HIGH,
        @Schema(description = "Critical priority execution")
        CRITICAL
    }

    @Schema(description = "Advanced execution options")
    public static class ExecutionOptions {
        @Schema(description = "Retry policy for failed nodes")
        private RetryPolicy retryPolicy;

        @Schema(description = "Resource allocation preferences")
        private ResourcePreferences resourcePreferences;

        @Schema(description = "Notification preferences")
        private NotificationPreferences notificationPreferences;

        @Schema(description = "Custom metadata for execution")
        private Map<String, Object> metadata;

        // Constructors, getters, and setters
        public ExecutionOptions() {}

        public RetryPolicy getRetryPolicy() { return retryPolicy; }
        public void setRetryPolicy(RetryPolicy retryPolicy) { this.retryPolicy = retryPolicy; }

        public ResourcePreferences getResourcePreferences() { return resourcePreferences; }
        public void setResourcePreferences(ResourcePreferences resourcePreferences) { this.resourcePreferences = resourcePreferences; }

        public NotificationPreferences getNotificationPreferences() { return notificationPreferences; }
        public void setNotificationPreferences(NotificationPreferences notificationPreferences) { this.notificationPreferences = notificationPreferences; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    @Schema(description = "Retry policy configuration")
    public static class RetryPolicy {
        @Schema(description = "Maximum number of retry attempts")
        private int maxAttempts = 3;

        @Schema(description = "Initial delay between retries in milliseconds")
        private long initialDelayMs = 1000;

        @Schema(description = "Multiplier for exponential backoff")
        private double backoffMultiplier = 2.0;

        @Schema(description = "Maximum delay between retries in milliseconds")
        private long maxDelayMs = 30000;

        // Constructors, getters, and setters
        public RetryPolicy() {}

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public long getInitialDelayMs() { return initialDelayMs; }
        public void setInitialDelayMs(long initialDelayMs) { this.initialDelayMs = initialDelayMs; }

        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }

        public long getMaxDelayMs() { return maxDelayMs; }
        public void setMaxDelayMs(long maxDelayMs) { this.maxDelayMs = maxDelayMs; }
    }

    @Schema(description = "Resource allocation preferences")
    public static class ResourcePreferences {
        @Schema(description = "Preferred CPU allocation")
        private String cpuPreference;

        @Schema(description = "Preferred memory allocation")
        private String memoryPreference;

        @Schema(description = "Preferred execution environment")
        private String environment;

        // Constructors, getters, and setters
        public ResourcePreferences() {}

        public String getCpuPreference() { return cpuPreference; }
        public void setCpuPreference(String cpuPreference) { this.cpuPreference = cpuPreference; }

        public String getMemoryPreference() { return memoryPreference; }
        public void setMemoryPreference(String memoryPreference) { this.memoryPreference = memoryPreference; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
    }

    @Schema(description = "Notification preferences")
    public static class NotificationPreferences {
        @Schema(description = "Whether to notify on completion")
        private boolean notifyOnCompletion = true;

        @Schema(description = "Whether to notify on failure")
        private boolean notifyOnFailure = true;

        @Schema(description = "Email addresses for notifications")
        private java.util.List<String> emailNotifications;

        @Schema(description = "Slack channels for notifications")
        private java.util.List<String> slackNotifications;

        // Constructors, getters, and setters
        public NotificationPreferences() {}

        public boolean isNotifyOnCompletion() { return notifyOnCompletion; }
        public void setNotifyOnCompletion(boolean notifyOnCompletion) { this.notifyOnCompletion = notifyOnCompletion; }

        public boolean isNotifyOnFailure() { return notifyOnFailure; }
        public void setNotifyOnFailure(boolean notifyOnFailure) { this.notifyOnFailure = notifyOnFailure; }

        public java.util.List<String> getEmailNotifications() { return emailNotifications; }
        public void setEmailNotifications(java.util.List<String> emailNotifications) { this.emailNotifications = emailNotifications; }

        public java.util.List<String> getSlackNotifications() { return slackNotifications; }
        public void setSlackNotifications(java.util.List<String> slackNotifications) { this.slackNotifications = slackNotifications; }
    }
}