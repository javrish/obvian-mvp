package api.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for plugin configuration schema.
 */
public class PluginSchemaDTO {
    private String pluginId;
    private Map<String, FieldSchema> apiKeyFields = new HashMap<>();
    private Map<String, FieldSchema> settingFields = new HashMap<>();
    private List<String> requiredFields;
    private Map<String, String> examples = new HashMap<>();

    public static class FieldSchema {
        private String name;
        private String type;
        private String description;
        private boolean required;
        private String defaultValue;
        private List<String> allowedValues;
        private String validation;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        
        public List<String> getAllowedValues() { return allowedValues; }
        public void setAllowedValues(List<String> allowedValues) { this.allowedValues = allowedValues; }
        
        public String getValidation() { return validation; }
        public void setValidation(String validation) { this.validation = validation; }
    }

    // Getters and setters
    public String getPluginId() { return pluginId; }
    public void setPluginId(String pluginId) { this.pluginId = pluginId; }
    
    public Map<String, FieldSchema> getApiKeyFields() { return apiKeyFields; }
    public void setApiKeyFields(Map<String, FieldSchema> apiKeyFields) { 
        this.apiKeyFields = apiKeyFields; 
    }
    
    public Map<String, FieldSchema> getSettingFields() { return settingFields; }
    public void setSettingFields(Map<String, FieldSchema> settingFields) { 
        this.settingFields = settingFields; 
    }
    
    public List<String> getRequiredFields() { return requiredFields; }
    public void setRequiredFields(List<String> requiredFields) { 
        this.requiredFields = requiredFields; 
    }
    
    public Map<String, String> getExamples() { return examples; }
    public void setExamples(Map<String, String> examples) { 
        this.examples = examples; 
    }
}