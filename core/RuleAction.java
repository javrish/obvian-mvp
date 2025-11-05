package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents an action to be executed as part of a business rule.
 * Actions can be plugin executions, calculations, notifications, or other operations.
 * Supports conditional execution, priority ordering, and parameter templating.
 * 
 * This class is part of the Natural Language Business Rule Engine (Phase 26.2b)
 * which implements Product Patent 24 - Natural Language Business Rule Processing.
 * 
 * Patent Alignment: Implements action execution framework that enables conversational
 * rule definition with plugin-based action mapping and parameter interpolation.
 * 
 * @author Obvian Labs
 * @since Phase 26.2b
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleAction {
    
    /**
     * Common action types supported by the engine
     */
    public static final String SEND_EMAIL = "SEND_EMAIL";
    public static final String SEND_SLACK = "SEND_SLACK";
    public static final String SEND_SMS = "SEND_SMS";
    public static final String SEND_NOTIFICATION = "SEND_NOTIFICATION";
    public static final String APPLY_DISCOUNT = "APPLY_DISCOUNT";
    public static final String CALCULATE = "CALCULATE";
    public static final String VALIDATE_FIELD = "VALIDATE_FIELD";
    public static final String ESCALATE = "ESCALATE";
    public static final String EXECUTE_PLUGIN = "EXECUTE_PLUGIN";
    public static final String UPDATE_RECORD = "UPDATE_RECORD";
    public static final String CREATE_RECORD = "CREATE_RECORD";
    public static final String DELETE_RECORD = "DELETE_RECORD";
    public static final String RETRY = "RETRY";
    public static final String LOG_EVENT = "LOG_EVENT";
    public static final String TRIGGER_WORKFLOW = "TRIGGER_WORKFLOW";
    public static final String UPDATE_CONTEXT = "UPDATE_CONTEXT";
    
    @JsonProperty("actionType")
    private final String actionType;
    
    @JsonProperty("parameters")
    private final Map<String, Object> parameters;
    
    @JsonProperty("condition")
    private final RuleCondition condition; // Optional condition for this action
    
    @JsonProperty("priority")
    private final int priority;
    
    @JsonProperty("timeout")
    private final long timeout; // Action-specific timeout in milliseconds
    
    @JsonProperty("retryOnFailure")
    private final boolean retryOnFailure;
    
    @JsonProperty("maxRetries")
    private final int maxRetries;
    
    @JsonProperty("retryDelay")
    private final long retryDelay; // Delay between retries in milliseconds
    
    @JsonProperty("continueOnFailure")
    private final boolean continueOnFailure; // Whether to continue rule execution if this action fails
    
    @JsonProperty("async")
    private final boolean async; // Whether to execute asynchronously
    
    @JsonProperty("description")
    private final String description;
    
    @JsonCreator
    private RuleAction(
            @JsonProperty("actionType") String actionType,
            @JsonProperty("parameters") Map<String, Object> parameters,
            @JsonProperty("condition") RuleCondition condition,
            @JsonProperty("priority") int priority,
            @JsonProperty("timeout") long timeout,
            @JsonProperty("retryOnFailure") boolean retryOnFailure,
            @JsonProperty("maxRetries") int maxRetries,
            @JsonProperty("retryDelay") long retryDelay,
            @JsonProperty("continueOnFailure") boolean continueOnFailure,
            @JsonProperty("async") boolean async,
            @JsonProperty("description") String description) {
        this.actionType = actionType;
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
        this.condition = condition;
        this.priority = priority;
        this.timeout = timeout;
        this.retryOnFailure = retryOnFailure;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.continueOnFailure = continueOnFailure;
        this.async = async;
        this.description = description;
    }
    
    // Getters
    public String getActionType() { return actionType; }
    public Map<String, Object> getParameters() { return new HashMap<>(parameters); }
    public RuleCondition getCondition() { return condition; }
    public int getPriority() { return priority; }
    public long getTimeout() { return timeout; }
    public boolean isRetryOnFailure() { return retryOnFailure; }
    public int getMaxRetries() { return maxRetries; }
    public long getRetryDelay() { return retryDelay; }
    public boolean isContinueOnFailure() { return continueOnFailure; }
    public boolean isAsync() { return async; }
    public String getDescription() { return description; }
    
    /**
     * Check if this action has a condition
     */
    public boolean hasCondition() {
        return condition != null && !condition.isEmpty();
    }
    
    /**
     * Get a parameter value with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value == null) return null;
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        // Handle common type conversions
        if (type == String.class) {
            return (T) value.toString();
        } else if (type == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        } else if (type == Long.class && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        } else if (type == Double.class && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        } else if (type == Boolean.class) {
            if (value instanceof Boolean) {
                return (T) value;
            } else if (value instanceof String) {
                return (T) Boolean.valueOf((String) value);
            }
        }
        
        throw new ClassCastException("Cannot convert parameter '" + key + 
                                   "' from " + value.getClass().getSimpleName() + 
                                   " to " + type.getSimpleName());
    }
    
    /**
     * Get a parameter value as String
     */
    public String getParameterAsString(String key) {
        return getParameter(key, String.class);
    }
    
    /**
     * Get a parameter value as Integer
     */
    public Integer getParameterAsInt(String key) {
        return getParameter(key, Integer.class);
    }
    
    /**
     * Get a parameter value as Double
     */
    public Double getParameterAsDouble(String key) {
        return getParameter(key, Double.class);
    }
    
    /**
     * Get a parameter value as Boolean
     */
    public Boolean getParameterAsBoolean(String key) {
        return getParameter(key, Boolean.class);
    }
    
    /**
     * Builder pattern for creating RuleAction instances
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String actionType;
        private Map<String, Object> parameters = new HashMap<>();
        private RuleCondition condition;
        private int priority = 100;
        private long timeout = 30000; // 30 seconds default
        private boolean retryOnFailure = false;
        private int maxRetries = 0;
        private long retryDelay = 1000; // 1 second default
        private boolean continueOnFailure = true;
        private boolean async = false;
        private String description;
        
        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }
        
        public Builder addParameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = new HashMap<>(parameters);
            return this;
        }
        
        public Builder condition(RuleCondition condition) {
            this.condition = condition;
            return this;
        }
        
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder retryOnFailure(boolean retryOnFailure) {
            this.retryOnFailure = retryOnFailure;
            return this;
        }
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder retryDelay(long retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }
        
        public Builder continueOnFailure(boolean continueOnFailure) {
            this.continueOnFailure = continueOnFailure;
            return this;
        }
        
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        // Convenience methods for common action types
        public Builder sendEmail(String to, String subject, String template) {
            return actionType(SEND_EMAIL)
                    .addParameter("to", to)
                    .addParameter("subject", subject)
                    .addParameter("template", template);
        }
        
        public Builder sendSlack(String channel, String message) {
            return actionType(SEND_SLACK)
                    .addParameter("channel", channel)
                    .addParameter("message", message);
        }
        
        public Builder applyDiscount(double percentage) {
            return actionType(APPLY_DISCOUNT)
                    .addParameter("percentage", percentage);
        }
        
        public Builder calculate(String expression, String resultVariable) {
            return actionType(CALCULATE)
                    .addParameter("expression", expression)
                    .addParameter("resultVariable", resultVariable);
        }
        
        public Builder validateField(String field, String validationType) {
            return actionType(VALIDATE_FIELD)
                    .addParameter("field", field)
                    .addParameter("validationType", validationType);
        }
        
        public Builder escalate(String level, String reason) {
            return actionType(ESCALATE)
                    .addParameter("level", level)
                    .addParameter("reason", reason);
        }
        
        public Builder executePlugin(String pluginId) {
            return actionType(EXECUTE_PLUGIN)
                    .addParameter("pluginId", pluginId);
        }
        
        public Builder retry(int maxAttempts, String backoffStrategy) {
            return actionType(RETRY)
                    .addParameter("maxAttempts", maxAttempts)
                    .addParameter("backoffStrategy", backoffStrategy);
        }
        
        public Builder logEvent(String eventType, String message) {
            return actionType(LOG_EVENT)
                    .addParameter("eventType", eventType)
                    .addParameter("message", message);
        }
        
        public RuleAction build() {
            validate();
            return new RuleAction(actionType, parameters, condition, priority, timeout,
                                retryOnFailure, maxRetries, retryDelay, continueOnFailure,
                                async, description);
        }
        
        private void validate() {
            if (actionType == null || actionType.trim().isEmpty()) {
                throw new IllegalArgumentException("Action type is required");
            }
            
            if (timeout <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            
            if (maxRetries < 0) {
                throw new IllegalArgumentException("Max retries cannot be negative");
            }
            
            if (retryDelay < 0) {
                throw new IllegalArgumentException("Retry delay cannot be negative");
            }
            
            // Validate action-specific parameters
            validateActionParameters();
        }
        
        private void validateActionParameters() {
            switch (actionType) {
                case SEND_EMAIL:
                    if (!parameters.containsKey("to") || !parameters.containsKey("subject")) {
                        throw new IllegalArgumentException("Email action requires 'to' and 'subject' parameters");
                    }
                    break;
                case SEND_SLACK:
                    if (!parameters.containsKey("channel") || !parameters.containsKey("message")) {
                        throw new IllegalArgumentException("Slack action requires 'channel' and 'message' parameters");
                    }
                    break;
                case APPLY_DISCOUNT:
                    if (!parameters.containsKey("percentage")) {
                        throw new IllegalArgumentException("Discount action requires 'percentage' parameter");
                    }
                    break;
                case CALCULATE:
                    if (!parameters.containsKey("expression")) {
                        throw new IllegalArgumentException("Calculate action requires 'expression' parameter");
                    }
                    break;
                case VALIDATE_FIELD:
                    if (!parameters.containsKey("field")) {
                        throw new IllegalArgumentException("Validate field action requires 'field' parameter");
                    }
                    break;
                case EXECUTE_PLUGIN:
                    if (!parameters.containsKey("pluginId")) {
                        throw new IllegalArgumentException("Execute plugin action requires 'pluginId' parameter");
                    }
                    break;
            }
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleAction that = (RuleAction) o;
        return priority == that.priority &&
               timeout == that.timeout &&
               retryOnFailure == that.retryOnFailure &&
               maxRetries == that.maxRetries &&
               retryDelay == that.retryDelay &&
               continueOnFailure == that.continueOnFailure &&
               async == that.async &&
               Objects.equals(actionType, that.actionType) &&
               Objects.equals(parameters, that.parameters) &&
               Objects.equals(condition, that.condition) &&
               Objects.equals(description, that.description);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(actionType, parameters, condition, priority, timeout,
                          retryOnFailure, maxRetries, retryDelay, continueOnFailure, async);
    }
    
    @Override
    public String toString() {
        return "RuleAction{" +
                "actionType='" + actionType + '\'' +
                ", priority=" + priority +
                ", parametersCount=" + parameters.size() +
                (condition != null ? ", conditional=true" : "") +
                (async ? ", async=true" : "") +
                '}';
    }
}