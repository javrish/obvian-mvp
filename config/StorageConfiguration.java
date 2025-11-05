package api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Configuration properties for plugin storage infrastructure
 * Manages S3/compatible storage settings, CDN configuration, and security policies
 */
@Configuration
@ConfigurationProperties(prefix = "obvian.plugin.storage")
@Validated
public class StorageConfiguration {
    
    /**
     * S3 bucket name for storing plugin JARs
     */
    @NotBlank(message = "Bucket name cannot be blank")
    private String bucketName = "obvian-plugin-storage";
    
    /**
     * AWS region for S3 bucket
     */
    @NotBlank(message = "Region cannot be blank")
    private String region = "us-west-2";
    
    /**
     * CDN domain for serving plugin downloads
     */
    private String cdnDomain = "cdn.obvian.com";
    
    /**
     * Maximum allowed file size for plugin uploads (in bytes)
     */
    @Positive(message = "Max file size must be positive")
    private Long maxFileSize = 50L * 1024 * 1024; // 50MB default
    
    /**
     * Minimum allowed file size for plugin uploads (in bytes)
     */
    @Positive(message = "Min file size must be positive")
    private Long minFileSize = 1024L; // 1KB minimum
    
    /**
     * Expiration time for signed download URLs (in minutes)
     */
    @Min(value = 1, message = "Signed URL expiration must be at least 1 minute")
    @Max(value = 1440, message = "Signed URL expiration cannot exceed 24 hours")
    private Integer signedUrlExpirationMinutes = 15;
    
    /**
     * Whether virus scanning is enabled for uploaded files
     */
    private boolean virusScanningEnabled = true;

    /**
     * Whether comprehensive security scanning is enabled for uploaded files
     */
    private boolean securityScanningEnabled = true;
    
    /**
     * Whether CDN is enabled for file serving
     */
    private boolean cdnEnabled = false;
    
    /**
     * Whether to enable server-side encryption
     */
    private boolean encryptionEnabled = true;
    
    /**
     * S3 storage class for uploaded files
     */
    private String storageClass = "STANDARD";
    
    /**
     * Allowed file extensions for plugin uploads
     */
    private List<String> allowedExtensions = List.of(".jar");
    
    /**
     * Allowed MIME types for plugin uploads
     */
    private List<String> allowedMimeTypes = List.of(
        "application/java-archive",
        "application/zip",
        "application/octet-stream"
    );
    
    /**
     * Maximum number of versions to keep per plugin
     */
    @Min(value = 1, message = "Max versions must be at least 1")
    private Integer maxVersionsPerPlugin = 10;
    
    /**
     * Whether to enable automatic cleanup of old versions
     */
    private boolean autoCleanupEnabled = true;
    
    /**
     * Number of days after which unused versions are cleaned up
     */
    @Min(value = 1, message = "Cleanup days must be at least 1")
    private Integer cleanupAfterDays = 90;
    
    /**
     * S3 endpoint URL for custom/compatible storage solutions (MinIO, etc.)
     */
    private String endpointUrl;
    
    /**
     * Whether to use path-style access for S3 API calls
     */
    private boolean pathStyleAccessEnabled = false;
    
    /**
     * Connection timeout for S3 operations (in milliseconds)
     */
    @Min(value = 1000, message = "Connection timeout must be at least 1000ms")
    private Integer connectionTimeoutMs = 30000; // 30 seconds
    
    /**
     * Read timeout for S3 operations (in milliseconds)
     */
    @Min(value = 1000, message = "Read timeout must be at least 1000ms")
    private Integer readTimeoutMs = 60000; // 60 seconds
    
    /**
     * Maximum number of retry attempts for S3 operations
     */
    @Min(value = 0, message = "Max retries cannot be negative")
    @Max(value = 10, message = "Max retries cannot exceed 10")
    private Integer maxRetries = 3;
    
    /**
     * Whether to enable multipart uploads for large files
     */
    private boolean multipartUploadEnabled = true;
    
    /**
     * Minimum file size threshold for multipart uploads (in bytes)
     */
    @Positive(message = "Multipart threshold must be positive")
    private Long multipartThreshold = 16L * 1024 * 1024; // 16MB
    
    /**
     * Part size for multipart uploads (in bytes)
     */
    @Positive(message = "Multipart part size must be positive")
    private Long multipartPartSize = 8L * 1024 * 1024; // 8MB
    
    /**
     * Whether to enable transfer acceleration
     */
    private boolean transferAccelerationEnabled = false;
    
    /**
     * Custom metadata to add to all uploaded files
     */
    private List<MetadataEntry> customMetadata = new ArrayList<>();
    
    /**
     * Whether to enable detailed logging for storage operations
     */
    private boolean detailedLoggingEnabled = false;
    
    /**
     * Whether to enable metrics collection for storage operations
     */
    private boolean metricsEnabled = true;
    
    // Default constructor
    public StorageConfiguration() {
    }
    
    // Getters and setters
    
    public String getBucketName() {
        return bucketName;
    }
    
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getCdnDomain() {
        return cdnDomain;
    }
    
    public void setCdnDomain(String cdnDomain) {
        this.cdnDomain = cdnDomain;
    }
    
    public Long getMaxFileSize() {
        return maxFileSize;
    }
    
    public void setMaxFileSize(Long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    public Long getMinFileSize() {
        return minFileSize;
    }
    
    public void setMinFileSize(Long minFileSize) {
        this.minFileSize = minFileSize;
    }
    
    public Integer getSignedUrlExpirationMinutes() {
        return signedUrlExpirationMinutes;
    }
    
    public void setSignedUrlExpirationMinutes(Integer signedUrlExpirationMinutes) {
        this.signedUrlExpirationMinutes = signedUrlExpirationMinutes;
    }
    
    public boolean isVirusScanningEnabled() {
        return virusScanningEnabled;
    }
    
    public void setVirusScanningEnabled(boolean virusScanningEnabled) {
        this.virusScanningEnabled = virusScanningEnabled;
    }

    public boolean isSecurityScanningEnabled() {
        return securityScanningEnabled;
    }

    public void setSecurityScanningEnabled(boolean securityScanningEnabled) {
        this.securityScanningEnabled = securityScanningEnabled;
    }
    
    public boolean isCdnEnabled() {
        return cdnEnabled;
    }
    
    public void setCdnEnabled(boolean cdnEnabled) {
        this.cdnEnabled = cdnEnabled;
    }
    
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
    
    public void setEncryptionEnabled(boolean encryptionEnabled) {
        this.encryptionEnabled = encryptionEnabled;
    }
    
    public String getStorageClass() {
        return storageClass;
    }
    
    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }
    
    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }
    
    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
    
    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }
    
    public void setAllowedMimeTypes(List<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }
    
    public Integer getMaxVersionsPerPlugin() {
        return maxVersionsPerPlugin;
    }
    
    public void setMaxVersionsPerPlugin(Integer maxVersionsPerPlugin) {
        this.maxVersionsPerPlugin = maxVersionsPerPlugin;
    }
    
    public boolean isAutoCleanupEnabled() {
        return autoCleanupEnabled;
    }
    
    public void setAutoCleanupEnabled(boolean autoCleanupEnabled) {
        this.autoCleanupEnabled = autoCleanupEnabled;
    }
    
    public Integer getCleanupAfterDays() {
        return cleanupAfterDays;
    }
    
    public void setCleanupAfterDays(Integer cleanupAfterDays) {
        this.cleanupAfterDays = cleanupAfterDays;
    }
    
    public String getEndpointUrl() {
        return endpointUrl;
    }
    
    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }
    
    public boolean isPathStyleAccessEnabled() {
        return pathStyleAccessEnabled;
    }
    
    public void setPathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
        this.pathStyleAccessEnabled = pathStyleAccessEnabled;
    }
    
    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }
    
    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }
    
    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }
    
    public void setReadTimeoutMs(Integer readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
    
    public Integer getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public boolean isMultipartUploadEnabled() {
        return multipartUploadEnabled;
    }
    
    public void setMultipartUploadEnabled(boolean multipartUploadEnabled) {
        this.multipartUploadEnabled = multipartUploadEnabled;
    }
    
    public Long getMultipartThreshold() {
        return multipartThreshold;
    }
    
    public void setMultipartThreshold(Long multipartThreshold) {
        this.multipartThreshold = multipartThreshold;
    }
    
    public Long getMultipartPartSize() {
        return multipartPartSize;
    }
    
    public void setMultipartPartSize(Long multipartPartSize) {
        this.multipartPartSize = multipartPartSize;
    }
    
    public boolean isTransferAccelerationEnabled() {
        return transferAccelerationEnabled;
    }
    
    public void setTransferAccelerationEnabled(boolean transferAccelerationEnabled) {
        this.transferAccelerationEnabled = transferAccelerationEnabled;
    }
    
    public List<MetadataEntry> getCustomMetadata() {
        return customMetadata;
    }
    
    public void setCustomMetadata(List<MetadataEntry> customMetadata) {
        this.customMetadata = customMetadata;
    }
    
    public boolean isDetailedLoggingEnabled() {
        return detailedLoggingEnabled;
    }
    
    public void setDetailedLoggingEnabled(boolean detailedLoggingEnabled) {
        this.detailedLoggingEnabled = detailedLoggingEnabled;
    }
    
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }
    
    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }
    
    // Utility methods
    
    /**
     * Check if the given file extension is allowed
     */
    public boolean isExtensionAllowed(String extension) {
        if (extension == null || allowedExtensions == null) {
            return false;
        }
        return allowedExtensions.contains(extension.toLowerCase());
    }
    
    /**
     * Check if the given MIME type is allowed
     */
    public boolean isMimeTypeAllowed(String mimeType) {
        if (mimeType == null || allowedMimeTypes == null) {
            return false;
        }
        return allowedMimeTypes.contains(mimeType.toLowerCase());
    }
    
    /**
     * Check if file size is within allowed limits
     */
    public boolean isFileSizeAllowed(long fileSize) {
        return fileSize >= minFileSize && fileSize <= maxFileSize;
    }
    
    /**
     * Get human-readable max file size
     */
    public String getMaxFileSizeHumanReadable() {
        return formatBytes(maxFileSize);
    }
    
    /**
     * Get human-readable min file size
     */
    public String getMinFileSizeHumanReadable() {
        return formatBytes(minFileSize);
    }
    
    /**
     * Check if multipart upload should be used for the given file size
     */
    public boolean shouldUseMultipartUpload(long fileSize) {
        return multipartUploadEnabled && fileSize >= multipartThreshold;
    }
    
    /**
     * Validate the configuration
     */
    public void validate() {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("Bucket name cannot be null or empty");
        }
        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Max file size must be positive");
        }
        if (minFileSize <= 0) {
            throw new IllegalArgumentException("Min file size must be positive");
        }
        if (minFileSize >= maxFileSize) {
            throw new IllegalArgumentException("Min file size must be less than max file size");
        }
        if (signedUrlExpirationMinutes <= 0) {
            throw new IllegalArgumentException("Signed URL expiration must be positive");
        }
        if (multipartThreshold <= 0) {
            throw new IllegalArgumentException("Multipart threshold must be positive");
        }
        if (multipartPartSize <= 0) {
            throw new IllegalArgumentException("Multipart part size must be positive");
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    @Override
    public String toString() {
        return "StorageConfiguration{" +
                "bucketName='" + bucketName + '\'' +
                ", region='" + region + '\'' +
                ", maxFileSize=" + getMaxFileSizeHumanReadable() +
                ", cdnEnabled=" + cdnEnabled +
                ", virusScanningEnabled=" + virusScanningEnabled +
                ", securityScanningEnabled=" + securityScanningEnabled +
                ", encryptionEnabled=" + encryptionEnabled +
                ", multipartUploadEnabled=" + multipartUploadEnabled +
                '}';
    }
    
    /**
     * Inner class for custom metadata entries
     */
    public static class MetadataEntry {
        private String key;
        private String value;
        
        public MetadataEntry() {
        }
        
        public MetadataEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }
        
        public String getKey() {
            return key;
        }
        
        public void setKey(String key) {
            this.key = key;
        }
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return "MetadataEntry{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}