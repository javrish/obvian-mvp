package api.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public class UpdatePluginRequest {
    
    @Size(min = 3, max = 100, message = "Plugin name must be between 3 and 100 characters")
    private String name;
    
    @Size(min = 10, max = 500, message = "Plugin description must be between 10 and 500 characters")
    private String description;
    
    private String category;
    private String code;
    private Map<String, Object> configuration;
    private String status;
    private List<String> capabilities;
    private List<String> tags;
    private Map<String, Object> metadata;
    private String changeDescription;

    public UpdatePluginRequest() {
    }

    public UpdatePluginRequest(String name, String description, String category, String code, Map<String, Object> configuration, String status, List<String> capabilities, List<String> tags, Map<String, Object> metadata, String changeDescription) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.code = code;
        this.configuration = configuration;
        this.status = status;
        this.capabilities = capabilities;
        this.tags = tags;
        this.metadata = metadata;
        this.changeDescription = changeDescription;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getChangeDescription() {
        return changeDescription;
    }

    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
}