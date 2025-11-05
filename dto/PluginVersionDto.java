package api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginVersionDto {
    private String version;
    private LocalDateTime createdAt;
    private String createdBy;
    private String changes;
    private String code;
    private String commitHash;

    public PluginVersionDto() {
    }

    public PluginVersionDto(String version, LocalDateTime createdAt, String createdBy, String changes, String code, String commitHash) {
        this.version = version;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.changes = changes;
        this.code = code;
        this.commitHash = commitHash;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }
}