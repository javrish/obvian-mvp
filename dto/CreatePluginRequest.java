package api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public class CreatePluginRequest {
    
    @NotBlank(message = "Plugin name is required")
    @Size(min = 3, max = 100, message = "Plugin name must be between 3 and 100 characters")
    private String name;
    
    @NotBlank(message = "Plugin description is required")
    @Size(min = 10, max = 500, message = "Plugin description must be between 10 and 500 characters")
    private String description;
    
    @NotBlank(message = "Plugin category is required")
    private String category;
    
    @NotBlank(message = "Plugin code is required")
    private String code;
    
    private Map<String, Object> configuration;
    private List<String> capabilities;
    private List<String> tags;
    private String author;
    private Map<String, Object> metadata;

    public CreatePluginRequest() {
    }

    public CreatePluginRequest(String name, String description, String category, String code, Map<String, Object> configuration, List<String> capabilities, List<String> tags, String author, Map<String, Object> metadata) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.code = code;
        this.configuration = configuration;
        this.capabilities = capabilities;
        this.tags = tags;
        this.author = author;
        this.metadata = metadata;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}