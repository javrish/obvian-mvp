package core.petri;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

/**
 * Represents a transition in a Petri net.
 * Transitions represent actions or events that can fire when enabled.
 * 
 * @author Obvian Labs
 * @since POC Phase 1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transition {
    
    @JsonProperty("id")
    private final String id;
    
    @JsonProperty("name")
    private final String name;
    
    @JsonProperty("description")
    private final String description;
    
    @JsonProperty("action")
    private final String action;
    
    @JsonProperty("guard")
    private final String guard;

    @JsonProperty("metadata")
    private final Map<String, Object> metadata;

    // Advanced control flow fields
    @JsonProperty("timeoutMs")
    private final Long timeoutMs;

    @JsonProperty("delayMs")
    private final Long delayMs;

    @JsonProperty("retryPolicy")
    private final Map<String, Object> retryPolicy;

    @JsonProperty("inhibitorConditions")
    private final Map<String, Object> inhibitorConditions;
    
    @JsonCreator
    public Transition(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("action") String action,
            @JsonProperty("guard") String guard,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("timeoutMs") Long timeoutMs,
            @JsonProperty("delayMs") Long delayMs,
            @JsonProperty("retryPolicy") Map<String, Object> retryPolicy,
            @JsonProperty("inhibitorConditions") Map<String, Object> inhibitorConditions) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Transition ID cannot be null or empty");
        }
        
        this.id = id.trim();
        this.name = name != null ? name.trim() : id;
        this.description = description;
        this.action = action;
        this.guard = guard;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.timeoutMs = timeoutMs;
        this.delayMs = delayMs;
        this.retryPolicy = retryPolicy != null ? new HashMap<>(retryPolicy) : new HashMap<>();
        this.inhibitorConditions = inhibitorConditions != null ? new HashMap<>(inhibitorConditions) : new HashMap<>();
    }
    
    // Convenience constructors
    public Transition(String id) {
        this(id, null, null, null, null, null, null, null, null, null);
    }

    public Transition(String id, String name) {
        this(id, name, null, null, null, null, null, null, null, null);
    }

    public Transition(String id, String name, String action) {
        this(id, name, null, action, null, null, null, null, null, null);
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAction() { return action; }
    public String getGuard() { return guard; }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public Long getTimeoutMs() { return timeoutMs; }
    public Long getDelayMs() { return delayMs; }
    public Map<String, Object> getRetryPolicy() { return new HashMap<>(retryPolicy); }
    public Map<String, Object> getInhibitorConditions() { return new HashMap<>(inhibitorConditions); }
    
    /**
     * Check if transition has a guard condition
     */
    public boolean hasGuard() {
        return guard != null && !guard.trim().isEmpty();
    }
    
    /**
     * Check if transition has an associated action
     */
    public boolean hasAction() {
        return action != null && !action.trim().isEmpty();
    }
    
    /**
     * Get metadata value by key
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Get priority for deterministic firing order
     */
    public int getPriority() {
        Object priority = metadata.get("priority");
        if (priority instanceof Number) {
            return ((Number) priority).intValue();
        }
        return 0; // Default priority
    }
    
    /**
     * Check if this is a choice transition (XOR-split)
     */
    public boolean isChoice() {
        return Boolean.TRUE.equals(metadata.get("isChoice"));
    }
    
    /**
     * Check if this is a fork transition (AND-split)
     */
    public boolean isFork() {
        return Boolean.TRUE.equals(metadata.get("isFork"));
    }
    
    /**
     * Check if this is a join transition (AND-join)
     */
    public boolean isJoin() {
        return Boolean.TRUE.equals(metadata.get("isJoin"));
    }
    
    /**
     * Get the choice condition for XOR transitions
     */
    public String getChoiceCondition() {
        Object condition = metadata.get("choiceCondition");
        return condition != null ? condition.toString() : null;
    }

    // ====================== ADVANCED CONTROL FLOW METHODS ======================

    /**
     * Check if transition has timeout configured
     */
    public boolean hasTimeout() {
        return timeoutMs != null && timeoutMs > 0;
    }

    /**
     * Check if transition has delay configured
     */
    public boolean hasDelay() {
        return delayMs != null && delayMs > 0;
    }

    /**
     * Check if transition has retry policy configured
     */
    public boolean hasRetryPolicy() {
        return !retryPolicy.isEmpty();
    }

    /**
     * Check if transition has inhibitor conditions
     */
    public boolean hasInhibitorConditions() {
        return !inhibitorConditions.isEmpty();
    }

    /**
     * Check if this is a timed transition
     */
    public boolean isTimed() {
        return hasTimeout() || hasDelay() || Boolean.TRUE.equals(metadata.get("isTimed"));
    }

    /**
     * Check if this is an immediate transition (fires as soon as enabled)
     */
    public boolean isImmediate() {
        return Boolean.TRUE.equals(metadata.get("isImmediate")) && !hasDelay();
    }

    /**
     * Get retry count from retry policy
     */
    public int getMaxRetries() {
        Object maxRetries = retryPolicy.get("maxRetries");
        return maxRetries instanceof Number ? ((Number) maxRetries).intValue() : 0;
    }

    /**
     * Get retry backoff multiplier
     */
    public double getBackoffMultiplier() {
        Object backoff = retryPolicy.get("backoffMultiplier");
        return backoff instanceof Number ? ((Number) backoff).doubleValue() : 1.0;
    }

    /**
     * Get initial retry delay
     */
    public long getInitialRetryDelayMs() {
        Object initialDelay = retryPolicy.get("initialDelayMs");
        return initialDelay instanceof Number ? ((Number) initialDelay).longValue() : 1000L;
    }

    /**
     * Get maximum retry delay
     */
    public long getMaxRetryDelayMs() {
        Object maxDelay = retryPolicy.get("maxDelayMs");
        return maxDelay instanceof Number ? ((Number) maxDelay).longValue() : 30000L;
    }

    /**
     * Check if transition should be retried on specific exception types
     */
    @SuppressWarnings("unchecked")
    public boolean shouldRetryOnException(String exceptionType) {
        Object retryOn = retryPolicy.get("retryOnExceptions");
        if (retryOn instanceof List) {
            return ((List<String>) retryOn).contains(exceptionType);
        }
        return true; // Default: retry on any exception
    }

    /**
     * Evaluate inhibitor conditions against current state
     */
    public boolean isInhibited(Map<String, Object> currentState) {
        if (!hasInhibitorConditions()) {
            return false; // No inhibitor conditions
        }

        for (Map.Entry<String, Object> condition : inhibitorConditions.entrySet()) {
            String conditionKey = condition.getKey();
            Object conditionValue = condition.getValue();

            if (evaluateInhibitorCondition(conditionKey, conditionValue, currentState)) {
                return true; // At least one inhibitor condition is met
            }
        }

        return false; // No inhibitor conditions met
    }

    /**
     * Evaluate a single inhibitor condition
     */
    private boolean evaluateInhibitorCondition(String conditionKey, Object conditionValue,
                                              Map<String, Object> currentState) {
        if (currentState == null) {
            return false;
        }

        Object stateValue = currentState.get(conditionKey);

        if (stateValue == null && conditionValue == null) {
            return true;
        }

        if (stateValue == null || conditionValue == null) {
            return false;
        }

        // Handle different value types
        if (conditionValue instanceof Number && stateValue instanceof Number) {
            return ((Number) stateValue).doubleValue() >= ((Number) conditionValue).doubleValue();
        }

        if (conditionValue instanceof Boolean) {
            return Boolean.parseBoolean(stateValue.toString()) == (Boolean) conditionValue;
        }

        return Objects.equals(stateValue.toString(), conditionValue.toString());
    }

    /**
     * Calculate effective firing delay considering base delay and retry attempts
     */
    public long calculateEffectiveDelay(int retryAttempt) {
        long baseDelay = delayMs != null ? delayMs : 0L;

        if (retryAttempt > 0 && hasRetryPolicy()) {
            long retryDelay = getInitialRetryDelayMs();
            double multiplier = getBackoffMultiplier();

            // Apply exponential backoff
            for (int i = 1; i < retryAttempt; i++) {
                retryDelay = Math.round(retryDelay * multiplier);
            }

            // Respect maximum retry delay
            retryDelay = Math.min(retryDelay, getMaxRetryDelayMs());
            return baseDelay + retryDelay;
        }

        return baseDelay;
    }

    /**
     * Check if transition can fire considering all constraints
     */
    public boolean canFire(Map<String, Object> currentState, Map<String, Integer> marking) {
        // Check basic guard condition
        if (hasGuard() && !evaluateGuardCondition(currentState)) {
            return false;
        }

        // Check inhibitor conditions
        if (isInhibited(currentState)) {
            return false;
        }

        // Additional checks can be added here for colored token compatibility
        return true;
    }

    /**
     * Evaluate guard condition with context
     */
    private boolean evaluateGuardCondition(Map<String, Object> context) {
        if (!hasGuard()) {
            return true;
        }

        // This is a simplified implementation
        // In a full implementation, this would parse and evaluate complex guard expressions
        try {
            // Handle simple variable conditions
            if (context != null && context.containsKey(guard)) {
                Object value = context.get(guard);
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
                return Boolean.parseBoolean(value.toString());
            }

            // Handle simple comparison expressions
            if (guard.contains("==") || guard.contains(">=") || guard.contains("<=") ||
                guard.contains(">") || guard.contains("<") || guard.contains("!=")) {
                return evaluateComparisonExpression(guard, context);
            }

            return true; // Default to true for unknown expressions

        } catch (Exception e) {
            // Guard evaluation failed, default to false for safety
            return false;
        }
    }

    /**
     * Evaluate comparison expressions in guards
     */
    private boolean evaluateComparisonExpression(String expression, Map<String, Object> context) {
        // This is a very simplified implementation
        // A full implementation would use a proper expression parser

        String[] operators = {"==", "!=", ">=", "<=", ">", "<"};
        for (String op : operators) {
            if (expression.contains(op)) {
                String[] parts = expression.split(op, 2);
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();

                    Object leftValue = getValueFromContext(left, context);
                    Object rightValue = getValueFromContext(right, context);

                    return compareValues(leftValue, rightValue, op);
                }
            }
        }
        return true;
    }

    /**
     * Get value from context or parse as literal
     */
    private Object getValueFromContext(String valueStr, Map<String, Object> context) {
        if (context != null && context.containsKey(valueStr)) {
            return context.get(valueStr);
        }

        // Try to parse as number
        try {
            return Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            // Try to parse as boolean
            if ("true".equalsIgnoreCase(valueStr) || "false".equalsIgnoreCase(valueStr)) {
                return Boolean.parseBoolean(valueStr);
            }
            // Return as string (remove quotes if present)
            return valueStr.replaceAll("^['\"]|['\"]$", "");
        }
    }

    /**
     * Compare two values using the specified operator
     */
    private boolean compareValues(Object left, Object right, String operator) {
        if (left == null || right == null) {
            return "==".equals(operator) ? Objects.equals(left, right) :
                   "!=".equals(operator) ? !Objects.equals(left, right) : false;
        }

        if (left instanceof Number && right instanceof Number) {
            double leftNum = ((Number) left).doubleValue();
            double rightNum = ((Number) right).doubleValue();

            return switch (operator) {
                case "==" -> leftNum == rightNum;
                case "!=" -> leftNum != rightNum;
                case ">" -> leftNum > rightNum;
                case ">=" -> leftNum >= rightNum;
                case "<" -> leftNum < rightNum;
                case "<=" -> leftNum <= rightNum;
                default -> false;
            };
        }

        // String comparison
        String leftStr = left.toString();
        String rightStr = right.toString();

        return switch (operator) {
            case "==" -> leftStr.equals(rightStr);
            case "!=" -> !leftStr.equals(rightStr);
            case ">" -> leftStr.compareTo(rightStr) > 0;
            case ">=" -> leftStr.compareTo(rightStr) >= 0;
            case "<" -> leftStr.compareTo(rightStr) < 0;
            case "<=" -> leftStr.compareTo(rightStr) <= 0;
            default -> false;
        };
    }
    
    /**
     * Builder pattern for creating Transition instances
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private String name;
        private String description;
        private String action;
        private String guard;
        private Map<String, Object> metadata = new HashMap<>();
        private Long timeoutMs;
        private Long delayMs;
        private Map<String, Object> retryPolicy = new HashMap<>();
        private Map<String, Object> inhibitorConditions = new HashMap<>();
        
        public Builder(String id) {
            this.id = id;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder action(String action) {
            this.action = action;
            return this;
        }
        
        public Builder guard(String guard) {
            this.guard = guard;
            return this;
        }
        
        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }
        
        public Builder priority(int priority) {
            this.metadata.put("priority", priority);
            return this;
        }
        
        public Builder asChoice(String condition) {
            this.metadata.put("isChoice", true);
            if (condition != null) {
                this.metadata.put("choiceCondition", condition);
            }
            return this;
        }
        
        public Builder asFork() {
            this.metadata.put("isFork", true);
            return this;
        }
        
        public Builder asJoin() {
            this.metadata.put("isJoin", true);
            return this;
        }

        // Advanced control flow builder methods
        public Builder timeout(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder delay(long delayMs) {
            this.delayMs = delayMs;
            return this;
        }

        public Builder retryPolicy(int maxRetries, double backoffMultiplier) {
            this.retryPolicy.put("maxRetries", maxRetries);
            this.retryPolicy.put("backoffMultiplier", backoffMultiplier);
            return this;
        }

        public Builder retryPolicy(int maxRetries, double backoffMultiplier, long initialDelayMs, long maxDelayMs) {
            this.retryPolicy.put("maxRetries", maxRetries);
            this.retryPolicy.put("backoffMultiplier", backoffMultiplier);
            this.retryPolicy.put("initialDelayMs", initialDelayMs);
            this.retryPolicy.put("maxDelayMs", maxDelayMs);
            return this;
        }

        public Builder retryOnExceptions(List<String> exceptionTypes) {
            this.retryPolicy.put("retryOnExceptions", new ArrayList<>(exceptionTypes));
            return this;
        }

        public Builder inhibitorCondition(String key, Object value) {
            this.inhibitorConditions.put(key, value);
            return this;
        }

        public Builder inhibitorConditions(Map<String, Object> conditions) {
            this.inhibitorConditions.putAll(conditions);
            return this;
        }

        public Builder asTimed() {
            this.metadata.put("isTimed", true);
            return this;
        }

        public Builder asImmediate() {
            this.metadata.put("isImmediate", true);
            return this;
        }

        public Transition build() {
            return new Transition(id, name, description, action, guard, metadata,
                                timeoutMs, delayMs, retryPolicy, inhibitorConditions);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transition that = (Transition) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Transition{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');

        if (action != null) {
            sb.append(", action='").append(action).append('\'');
        }

        if (guard != null) {
            sb.append(", guard='").append(guard).append('\'');
        }

        if (hasTimeout()) {
            sb.append(", timeout=").append(timeoutMs).append("ms");
        }

        if (hasDelay()) {
            sb.append(", delay=").append(delayMs).append("ms");
        }

        if (hasRetryPolicy()) {
            sb.append(", retries=").append(getMaxRetries());
        }

        if (hasInhibitorConditions()) {
            sb.append(", inhibitors=").append(inhibitorConditions.size());
        }

        if (isTimed()) {
            sb.append(", timed=true");
        }

        if (isImmediate()) {
            sb.append(", immediate=true");
        }

        sb.append('}');
        return sb.toString();
    }
}