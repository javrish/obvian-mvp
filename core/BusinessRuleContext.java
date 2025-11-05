package core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides execution context for business rule evaluation and action execution.
 * Contains business entities, variables, user information, and helper methods
 * for rule processing and variable interpolation.
 * 
 * This class is part of the Natural Language Business Rule Engine (Phase 26.2b)
 * which implements Product Patent 24 - Natural Language Business Rule Processing.
 * 
 * Patent Alignment: Implements context management that enables conversational
 * rule execution with business entity recognition and variable interpolation.
 * 
 * @author Obvian Labs
 * @since Phase 26.2b
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BusinessRuleContext {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    @JsonProperty("contextId")
    private final String contextId;
    
    @JsonProperty("sessionId")
    private final String sessionId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("entities")
    private final Map<String, Object> entities;
    
    @JsonProperty("variables")
    private final Map<String, Object> variables;
    
    @JsonProperty("executionHistory")
    private final Map<String, Object> executionHistory;
    
    @JsonProperty("userPreferences")
    private final Map<String, Object> userPreferences;
    
    @JsonProperty("securityContext")
    private final Map<String, Object> securityContext;
    
    @JsonProperty("createdAt")
    private final LocalDateTime createdAt;
    
    @JsonProperty("lastUpdatedAt")
    private LocalDateTime lastUpdatedAt;
    
    @JsonProperty("timezone")
    private String timezone;
    
    @JsonProperty("locale")
    private String locale;
    
    @JsonProperty("debugMode")
    private boolean debugMode;
    
    @JsonCreator
    private BusinessRuleContext(
            @JsonProperty("contextId") String contextId,
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("userId") String userId,
            @JsonProperty("entities") Map<String, Object> entities,
            @JsonProperty("variables") Map<String, Object> variables,
            @JsonProperty("executionHistory") Map<String, Object> executionHistory,
            @JsonProperty("userPreferences") Map<String, Object> userPreferences,
            @JsonProperty("securityContext") Map<String, Object> securityContext,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("lastUpdatedAt") LocalDateTime lastUpdatedAt,
            @JsonProperty("timezone") String timezone,
            @JsonProperty("locale") String locale,
            @JsonProperty("debugMode") boolean debugMode) {
        this.contextId = contextId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.entities = entities != null ? new HashMap<>(entities) : new HashMap<>();
        this.variables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        this.executionHistory = executionHistory != null ? new HashMap<>(executionHistory) : new HashMap<>();
        this.userPreferences = userPreferences != null ? new HashMap<>(userPreferences) : new HashMap<>();
        this.securityContext = securityContext != null ? new HashMap<>(securityContext) : new HashMap<>();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.lastUpdatedAt = lastUpdatedAt != null ? lastUpdatedAt : LocalDateTime.now();
        this.timezone = timezone != null ? timezone : "UTC";
        this.locale = locale != null ? locale : "en-US";
        this.debugMode = debugMode;
    }
    
    // Static factory methods
    public static BusinessRuleContext create(String sessionId) {
        return new BusinessRuleContext(
            UUID.randomUUID().toString(),
            sessionId,
            null,
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            new HashMap<>(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            "UTC",
            "en-US",
            false
        );
    }
    
    public static BusinessRuleContext create(String sessionId, String userId) {
        var context = create(sessionId);
        context.setUserId(userId);
        return context;
    }
    
    // Getters
    public String getContextId() { return contextId; }
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public Map<String, Object> getEntities() { return new HashMap<>(entities); }
    public Map<String, Object> getVariables() { return new HashMap<>(variables); }
    public Map<String, Object> getExecutionHistory() { return new HashMap<>(executionHistory); }
    public Map<String, Object> getUserPreferences() { return new HashMap<>(userPreferences); }
    public Map<String, Object> getSecurityContext() { return new HashMap<>(securityContext); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public String getTimezone() { return timezone; }
    public String getLocale() { return locale; }
    public boolean isDebugMode() { return debugMode; }
    
    // Setters for mutable fields
    public void setUserId(String userId) {
        this.userId = userId;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public void setLocale(String locale) {
        this.locale = locale;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    // Entity management methods
    public void addEntity(String entityType, Object entityData) {
        entities.put(entityType, entityData);
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public Object getEntity(String entityType) {
        return entities.get(entityType);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getEntity(String entityType, Class<T> type) {
        Object entity = entities.get(entityType);
        if (entity == null) return null;
        
        if (type.isInstance(entity)) {
            return (T) entity;
        }
        
        throw new ClassCastException("Entity '" + entityType + 
                                   "' cannot be cast to " + type.getSimpleName());
    }
    
    public boolean hasEntity(String entityType) {
        return entities.containsKey(entityType);
    }
    
    public void removeEntity(String entityType) {
        entities.remove(entityType);
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    /**
     * Get a nested value from an entity using dot notation (e.g., "customer.address.city")
     */
    public Object getEntityValue(String path) {
        String[] parts = path.split("\\.");
        Object current = entities.get(parts[0]);
        
        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                current = map.get(parts[i]);
            } else {
                // Try to access as object property via reflection if needed
                current = getObjectProperty(current, parts[i]);
            }
        }
        
        return current;
    }
    
    private Object getObjectProperty(Object obj, String propertyName) {
        try {
            // Simple property access - could be enhanced with reflection
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                return map.get(propertyName);
            }
            // For now, return null for non-map objects
            // In a full implementation, you might use reflection here
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    // Variable management methods
    public void addVariable(String name, Object value) {
        variables.put(name, value);
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public Object getVariable(String name) {
        return variables.get(name);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String name, Class<T> type) {
        Object value = variables.get(name);
        if (value == null) return null;
        
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        // Handle common type conversions
        if (type == String.class) {
            return (T) value.toString();
        } else if (type == Integer.class && value instanceof Number) {
            return (T) Integer.valueOf(((Number) value).intValue());
        } else if (type == Double.class && value instanceof Number) {
            return (T) Double.valueOf(((Number) value).doubleValue());
        } else if (type == Boolean.class && value instanceof String) {
            return (T) Boolean.valueOf((String) value);
        }
        
        throw new ClassCastException("Variable '" + name + 
                                   "' cannot be cast to " + type.getSimpleName());
    }
    
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }
    
    public void removeVariable(String name) {
        variables.remove(name);
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    // User preferences management
    public void setUserPreference(String key, Object value) {
        userPreferences.put(key, value);
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public Object getUserPreference(String key) {
        return userPreferences.get(key);
    }
    
    public <T> T getUserPreference(String key, Class<T> type, T defaultValue) {
        Object value = userPreferences.get(key);
        if (value == null) return defaultValue;
        
        try {
            if (type.isInstance(value)) {
                return type.cast(value);
            }
            // Simple type conversion for common cases
            if (type == String.class) {
                return type.cast(value.toString());
            }
        } catch (Exception e) {
            // Return default value if conversion fails
        }
        
        return defaultValue;
    }
    
    // Security context management
    public void setSecurityContext(String key, Object value) {
        securityContext.put(key, value);
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public Object getSecurityContext(String key) {
        return securityContext.get(key);
    }
    
    public boolean hasPermission(String permission) {
        Object permissions = securityContext.get("permissions");
        if (permissions instanceof java.util.Collection) {
            @SuppressWarnings("unchecked")
            java.util.Collection<String> permissionSet = (java.util.Collection<String>) permissions;
            return permissionSet.contains(permission);
        }
        return false;
    }
    
    // Execution history management
    public void addExecutionHistoryEntry(String key, Object value) {
        executionHistory.put(key, value);
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public Object getExecutionHistoryEntry(String key) {
        return executionHistory.get(key);
    }
    
    // Variable interpolation
    /**
     * Interpolate variables in a template string using ${variable} syntax.
     * Supports both variables and entity values using dot notation.
     */
    public String interpolateVariables(String template) {
        if (template == null) return null;
        
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = resolveVariable(variableName);
            String replacement = value != null ? value.toString() : "${" + variableName + "}";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Resolve a variable name to its value, supporting dot notation for entity access
     */
    private Object resolveVariable(String variableName) {
        // First check variables
        if (variables.containsKey(variableName)) {
            return variables.get(variableName);
        }
        
        // Then check entity values using dot notation
        if (variableName.contains(".")) {
            return getEntityValue(variableName);
        }
        
        // Finally check entities directly
        return entities.get(variableName);
    }
    
    // Utility methods
    public Map<String, Object> getAllData() {
        Map<String, Object> allData = new HashMap<>();
        allData.put("entities", entities);
        allData.put("variables", variables);
        allData.put("userPreferences", userPreferences);
        allData.put("executionHistory", executionHistory);
        return allData;
    }
    
    public BusinessRuleContext copy() {
        return new BusinessRuleContext(
            UUID.randomUUID().toString(), // New context ID
            sessionId,
            userId,
            new HashMap<>(entities),
            new HashMap<>(variables),
            new HashMap<>(executionHistory),
            new HashMap<>(userPreferences),
            new HashMap<>(securityContext),
            createdAt,
            LocalDateTime.now(),
            timezone,
            locale,
            debugMode
        );
    }
    
    public void merge(BusinessRuleContext other) {
        if (other == null) return;
        
        // Merge entities (other takes precedence)
        entities.putAll(other.entities);
        
        // Merge variables (other takes precedence)
        variables.putAll(other.variables);
        
        // Merge user preferences (other takes precedence)
        userPreferences.putAll(other.userPreferences);
        
        // Merge execution history (other takes precedence)
        executionHistory.putAll(other.executionHistory);
        
        // Update timestamp
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    public void clear() {
        entities.clear();
        variables.clear();
        executionHistory.clear();
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusinessRuleContext that = (BusinessRuleContext) o;
        return Objects.equals(contextId, that.contextId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(contextId);
    }
    
    @Override
    public String toString() {
        return "BusinessRuleContext{" +
                "contextId='" + contextId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", entitiesCount=" + entities.size() +
                ", variablesCount=" + variables.size() +
                ", createdAt=" + createdAt +
                ", lastUpdatedAt=" + lastUpdatedAt +
                '}';
    }
}