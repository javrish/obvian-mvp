package api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity for persisting plugin information in the database.
 * Tracks plugin metadata, status, and usage statistics.
 */
@Entity
@Table(name = "plugins", indexes = {
    @Index(name = "idx_plugin_name", columnList = "name"),
    @Index(name = "idx_plugin_category", columnList = "category"),
    @Index(name = "idx_plugin_status", columnList = "status"),
    @Index(name = "idx_plugin_active", columnList = "active"),
    @Index(name = "idx_plugin_version", columnList = "version")
})
public class PluginEntity {
    
    @Id
    @Column(name = "id", length = 100)
    private String id;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "version", nullable = false, length = 50)
    private String version;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @Column(name = "author", length = 255)
    private String author;
    
    @Column(name = "jar_path", length = 1024)
    private String jarPath;
    
    @Column(name = "class_name", length = 512)
    private String className;
    
    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PluginStatus status;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    @Column(name = "auto_load", nullable = false)
    private Boolean autoLoad = true;
    
    @Column(name = "priority", nullable = false)
    private Integer priority = 0;
    
    @ElementCollection
    @CollectionTable(name = "plugin_supported_actions", 
                     joinColumns = @JoinColumn(name = "plugin_id"))
    @Column(name = "action")
    private Set<String> supportedActions = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "plugin_dependencies", 
                     joinColumns = @JoinColumn(name = "plugin_id"))
    @Column(name = "dependency")
    private Set<String> dependencies = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "plugin_required_parameters", 
                     joinColumns = @JoinColumn(name = "plugin_id"))
    @Column(name = "parameter")
    private Set<String> requiredParameters = new HashSet<>();
    
    @Column(name = "configuration_json", columnDefinition = "TEXT")
    private String configurationJson;
    
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
    
    // Health and monitoring
    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;
    
    @Column(name = "health_status", length = 50)
    @Enumerated(EnumType.STRING)
    private HealthStatus healthStatus;
    
    @Column(name = "health_message", columnDefinition = "TEXT")
    private String healthMessage;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    // Usage statistics
    @Column(name = "load_count")
    private Long loadCount = 0L;
    
    @Column(name = "execution_count")
    private Long executionCount = 0L;
    
    @Column(name = "success_count")
    private Long successCount = 0L;
    
    @Column(name = "failure_count")
    private Long failureCount = 0L;
    
    @Column(name = "avg_execution_time_ms")
    private Long avgExecutionTimeMs;
    
    @Column(name = "total_execution_time_ms")
    private Long totalExecutionTimeMs = 0L;
    
    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    
    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;
    
    // Discovery and registration
    @Column(name = "discovered_at", nullable = false)
    private LocalDateTime discoveredAt;
    
    @Column(name = "registered_at")
    private LocalDateTime registeredAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "discovery_method", length = 50)
    @Enumerated(EnumType.STRING)
    private DiscoveryMethod discoveryMethod;
    
    @Column(name = "checksum", length = 128)
    private String checksum;
    
    // Security and validation
    @Column(name = "validated", nullable = false)
    private Boolean validated = false;
    
    @Column(name = "validated_at")
    private LocalDateTime validatedAt;
    
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
    
    @Column(name = "security_score")
    private Integer securityScore;
    
    @Column(name = "sandboxed", nullable = false)
    private Boolean sandboxed = false;
    
    // Relationships
    @OneToMany(mappedBy = "plugin", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PluginExecution> executions = new HashSet<>();
    
    @ManyToMany
    @JoinTable(
        name = "plugin_conflicts",
        joinColumns = @JoinColumn(name = "plugin_id"),
        inverseJoinColumns = @JoinColumn(name = "conflict_id")
    )
    private Set<PluginEntity> conflicts = new HashSet<>();
    
    public enum PluginStatus {
        DISCOVERED,
        LOADING,
        LOADED,
        ACTIVE,
        INACTIVE,
        ERROR,
        DISABLED,
        DEPRECATED,
        REMOVED
    }
    
    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN,
        CHECKING
    }
    
    public enum DiscoveryMethod {
        JAR_SCAN,
        SERVICE_LOADER,
        REFLECTION,
        MANIFEST,
        MANUAL,
        AUTO_DISCOVERY
    }
    
    @PrePersist
    protected void onCreate() {
        if (discoveredAt == null) {
            discoveredAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PluginStatus.DISCOVERED;
        }
        if (healthStatus == null) {
            healthStatus = HealthStatus.UNKNOWN;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getJarPath() {
        return jarPath;
    }
    
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public PluginStatus getStatus() {
        return status;
    }
    
    public void setStatus(PluginStatus status) {
        this.status = status;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public Boolean getAutoLoad() {
        return autoLoad;
    }
    
    public void setAutoLoad(Boolean autoLoad) {
        this.autoLoad = autoLoad;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    public Set<String> getSupportedActions() {
        return supportedActions;
    }
    
    public void setSupportedActions(Set<String> supportedActions) {
        this.supportedActions = supportedActions;
    }
    
    public Set<String> getDependencies() {
        return dependencies;
    }
    
    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }
    
    public Set<String> getRequiredParameters() {
        return requiredParameters;
    }
    
    public void setRequiredParameters(Set<String> requiredParameters) {
        this.requiredParameters = requiredParameters;
    }
    
    public String getConfigurationJson() {
        return configurationJson;
    }
    
    public void setConfigurationJson(String configurationJson) {
        this.configurationJson = configurationJson;
    }
    
    public String getMetadataJson() {
        return metadataJson;
    }
    
    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
    
    public LocalDateTime getLastHealthCheck() {
        return lastHealthCheck;
    }
    
    public void setLastHealthCheck(LocalDateTime lastHealthCheck) {
        this.lastHealthCheck = lastHealthCheck;
    }
    
    public HealthStatus getHealthStatus() {
        return healthStatus;
    }
    
    public void setHealthStatus(HealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }
    
    public String getHealthMessage() {
        return healthMessage;
    }
    
    public void setHealthMessage(String healthMessage) {
        this.healthMessage = healthMessage;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public Long getLoadCount() {
        return loadCount;
    }
    
    public void setLoadCount(Long loadCount) {
        this.loadCount = loadCount;
    }
    
    public Long getExecutionCount() {
        return executionCount;
    }
    
    public void setExecutionCount(Long executionCount) {
        this.executionCount = executionCount;
    }
    
    public Long getSuccessCount() {
        return successCount;
    }
    
    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }
    
    public Long getFailureCount() {
        return failureCount;
    }
    
    public void setFailureCount(Long failureCount) {
        this.failureCount = failureCount;
    }
    
    public Long getAvgExecutionTimeMs() {
        return avgExecutionTimeMs;
    }
    
    public void setAvgExecutionTimeMs(Long avgExecutionTimeMs) {
        this.avgExecutionTimeMs = avgExecutionTimeMs;
    }
    
    public Long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }
    
    public void setTotalExecutionTimeMs(Long totalExecutionTimeMs) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }
    
    public LocalDateTime getLastExecutedAt() {
        return lastExecutedAt;
    }
    
    public void setLastExecutedAt(LocalDateTime lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
    
    public LocalDateTime getLastErrorAt() {
        return lastErrorAt;
    }
    
    public void setLastErrorAt(LocalDateTime lastErrorAt) {
        this.lastErrorAt = lastErrorAt;
    }
    
    public LocalDateTime getDiscoveredAt() {
        return discoveredAt;
    }
    
    public void setDiscoveredAt(LocalDateTime discoveredAt) {
        this.discoveredAt = discoveredAt;
    }
    
    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
    
    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public DiscoveryMethod getDiscoveryMethod() {
        return discoveryMethod;
    }
    
    public void setDiscoveryMethod(DiscoveryMethod discoveryMethod) {
        this.discoveryMethod = discoveryMethod;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    public Boolean getValidated() {
        return validated;
    }
    
    public void setValidated(Boolean validated) {
        this.validated = validated;
    }
    
    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }
    
    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }
    
    public String getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(String validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public Integer getSecurityScore() {
        return securityScore;
    }
    
    public void setSecurityScore(Integer securityScore) {
        this.securityScore = securityScore;
    }
    
    public Boolean getSandboxed() {
        return sandboxed;
    }
    
    public void setSandboxed(Boolean sandboxed) {
        this.sandboxed = sandboxed;
    }
    
    public Set<PluginExecution> getExecutions() {
        return executions;
    }
    
    public void setExecutions(Set<PluginExecution> executions) {
        this.executions = executions;
    }
    
    public Set<PluginEntity> getConflicts() {
        return conflicts;
    }
    
    public void setConflicts(Set<PluginEntity> conflicts) {
        this.conflicts = conflicts;
    }
    
    // Helper methods
    public void incrementLoadCount() {
        this.loadCount = (this.loadCount != null ? this.loadCount : 0) + 1;
    }
    
    public void incrementExecutionCount() {
        this.executionCount = (this.executionCount != null ? this.executionCount : 0) + 1;
    }
    
    public void incrementSuccessCount() {
        this.successCount = (this.successCount != null ? this.successCount : 0) + 1;
    }
    
    public void incrementFailureCount() {
        this.failureCount = (this.failureCount != null ? this.failureCount : 0) + 1;
    }
    
    public void updateAverageExecutionTime(long executionTimeMs) {
        this.totalExecutionTimeMs = (this.totalExecutionTimeMs != null ? this.totalExecutionTimeMs : 0) + executionTimeMs;
        if (this.executionCount != null && this.executionCount > 0) {
            this.avgExecutionTimeMs = this.totalExecutionTimeMs / this.executionCount;
        }
    }
}