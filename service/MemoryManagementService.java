package api.service;

import api.model.MemoryEntryRequest;
import api.model.MemoryEntryResponse;
import api.model.MemorySearchResponse;
import memory.ExecutionMemoryEntry;
import memory.FileMemoryEntry;
import memory.MemoryStore;
import memory.UserContextMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing user memory through the API
 */
@Service
public class MemoryManagementService {
    
    private final MemoryStore memoryStore;
    private final Map<String, MemoryStore> userMemoryStores;
    
    // Configuration constants
    private static final int MAX_CONTENT_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_ENTRIES_PER_USER = 1000;
    private static final int DEFAULT_RETENTION_DAYS = 30;
    
    @Autowired
    public MemoryManagementService(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
        this.userMemoryStores = new ConcurrentHashMap<>();
    }
    
    /**
     * Search memory entries with filtering and pagination
     */
    public MemorySearchResponse searchMemoryEntries(String userId, MemorySearchCriteria criteria) {
        try {
            MemoryStore userStore = getUserMemoryStore(userId);
            
            // Get all entries based on type filter
            List<MemoryEntryResponse> allEntries = new ArrayList<>();
            
            if (criteria.type == null || "execution".equals(criteria.type)) {
                List<ExecutionMemoryEntry> executions = userStore.getAllExecutions();
                allEntries.addAll(executions.stream()
                    .map(MemoryEntryResponse::fromExecutionEntry)
                    .collect(Collectors.toList()));
            }
            
            if (criteria.type == null || "file".equals(criteria.type)) {
                List<FileMemoryEntry> files = userStore.getAllFiles();
                allEntries.addAll(files.stream()
                    .map(MemoryEntryResponse::fromFileEntry)
                    .collect(Collectors.toList()));
            }
            
            // Apply search filter
            if (criteria.search != null && !criteria.search.trim().isEmpty()) {
                allEntries = filterBySearch(allEntries, criteria.search);
            }
            
            // Apply date filters
            if (criteria.startDate != null || criteria.endDate != null) {
                allEntries = filterByDateRange(allEntries, criteria.startDate, criteria.endDate);
            }
            
            // Sort entries
            allEntries = sortEntries(allEntries, criteria.sortBy, criteria.sortOrder);
            
            // Apply pagination
            int total = allEntries.size();
            int startIndex = Math.min(criteria.offset, total);
            int endIndex = Math.min(startIndex + criteria.limit, total);
            List<MemoryEntryResponse> paginatedEntries = allEntries.subList(startIndex, endIndex);
            
            // Create pagination info
            MemorySearchResponse.PaginationInfo pagination = new MemorySearchResponse.PaginationInfo(
                total, criteria.limit, criteria.offset, endIndex < total);
            
            // Create search criteria map
            Map<String, Object> searchCriteriaMap = createSearchCriteriaMap(criteria);
            
            return MemorySearchResponse.success(paginatedEntries, pagination, searchCriteriaMap);
            
        } catch (Exception e) {
            return MemorySearchResponse.error(
                "Failed to search memory entries: " + e.getMessage(), "SEARCH_ERROR");
        }
    }
    
    /**
     * Get a specific memory entry by ID
     */
    public MemoryEntryResponse getMemoryEntry(String userId, String entryId) {
        try {
            MemoryStore userStore = getUserMemoryStore(userId);
            
            // Try to find as execution entry
            ExecutionMemoryEntry execution = userStore.getExecution(entryId);
            if (execution != null) {
                return MemoryEntryResponse.fromExecutionEntry(execution);
            }
            
            // Try to find as file entry
            FileMemoryEntry file = userStore.getFile(entryId);
            if (file != null) {
                return MemoryEntryResponse.fromFileEntry(file);
            }
            
            return MemoryEntryResponse.notFound(entryId);
            
        } catch (Exception e) {
            return MemoryEntryResponse.error(entryId, 
                "Failed to retrieve memory entry: " + e.getMessage(), "RETRIEVAL_ERROR");
        }
    }
    
    /**
     * Create a new memory entry
     */
    public MemoryEntryResponse createMemoryEntry(String userId, MemoryEntryRequest request) {
        try {
            // Validate request
            if (!request.isValid()) {
                return MemoryEntryResponse.validationError(null, "Invalid memory entry request");
            }
            
            // Check content size
            if (request.getContentSize() > MAX_CONTENT_SIZE) {
                return MemoryEntryResponse.validationError(null, 
                    "Content size exceeds maximum limit of " + MAX_CONTENT_SIZE + " bytes");
            }
            
            // Check storage limits
            if (!checkStorageLimit(userId)) {
                return MemoryEntryResponse.storageLimitExceeded(null);
            }
            
            MemoryStore userStore = getUserMemoryStore(userId);
            String entryId = generateEntryId();
            LocalDateTime timestamp = LocalDateTime.now();
            
            // Create entry based on type
            switch (request.getType().toLowerCase()) {
                case "file":
                    return createFileEntry(userStore, entryId, request, timestamp);
                    
                case "execution":
                    return createExecutionEntry(userStore, entryId, request, timestamp);
                    
                default:
                    return createGenericEntry(userStore, entryId, request, timestamp);
            }
            
        } catch (Exception e) {
            return MemoryEntryResponse.error(null, 
                "Failed to create memory entry: " + e.getMessage(), "CREATION_ERROR");
        }
    }
    
    /**
     * Update an existing memory entry
     */
    public MemoryEntryResponse updateMemoryEntry(String userId, String entryId, MemoryEntryRequest request) {
        try {
            // Check if entry exists
            MemoryEntryResponse existing = getMemoryEntry(userId, entryId);
            if (!existing.isSuccess()) {
                return existing; // Return the error response
            }
            
            // Validate request
            if (!request.isValid()) {
                return MemoryEntryResponse.validationError(entryId, "Invalid memory entry request");
            }
            
            // Check content size
            if (request.getContentSize() > MAX_CONTENT_SIZE) {
                return MemoryEntryResponse.validationError(entryId, 
                    "Content size exceeds maximum limit of " + MAX_CONTENT_SIZE + " bytes");
            }
            
            MemoryStore userStore = getUserMemoryStore(userId);
            LocalDateTime timestamp = LocalDateTime.now();
            
            // Update entry based on type
            switch (request.getType().toLowerCase()) {
                case "file":
                    return updateFileEntry(userStore, entryId, request, timestamp);
                    
                case "execution":
                    // Execution entries are typically read-only
                    return MemoryEntryResponse.validationError(entryId, 
                        "Execution entries cannot be updated");
                    
                default:
                    return updateGenericEntry(userStore, entryId, request, timestamp);
            }
            
        } catch (Exception e) {
            return MemoryEntryResponse.error(entryId, 
                "Failed to update memory entry: " + e.getMessage(), "UPDATE_ERROR");
        }
    }
    
    /**
     * Delete a memory entry
     */
    public boolean deleteMemoryEntry(String userId, String entryId) {
        try {
            MemoryStore userStore = getUserMemoryStore(userId);
            
            // Try to delete as execution entry
            ExecutionMemoryEntry execution = userStore.getExecution(entryId);
            if (execution != null) {
                // Remove from internal storage (this would need to be implemented in MemoryStore)
                return removeExecutionEntry(userStore, entryId);
            }
            
            // Try to delete as file entry
            FileMemoryEntry file = userStore.getFile(entryId);
            if (file != null) {
                // Remove from internal storage (this would need to be implemented in MemoryStore)
                return removeFileEntry(userStore, entryId);
            }
            
            return false; // Entry not found
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Clear all memory entries for a user
     */
    public int clearMemory(String userId) {
        try {
            MemoryStore userStore = getUserMemoryStore(userId);
            
            // Count entries before clearing
            int executionCount = userStore.getAllExecutions().size();
            int fileCount = userStore.getAllFiles().size();
            int totalCount = executionCount + fileCount;
            
            // Clear memory
            userStore.clearMemory();
            
            return totalCount;
            
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get memory statistics for a user
     */
    public Map<String, Object> getMemoryStats(String userId) {
        try {
            MemoryStore userStore = getUserMemoryStore(userId);
            
            Map<String, Object> stats = new HashMap<>();
            
            // Basic counts
            int executionCount = userStore.getAllExecutions().size();
            int fileCount = userStore.getAllFiles().size();
            int totalEntries = executionCount + fileCount;
            
            stats.put("totalEntries", totalEntries);
            stats.put("executionEntries", executionCount);
            stats.put("fileEntries", fileCount);
            
            // Storage usage
            long totalSize = calculateTotalStorageSize(userStore);
            stats.put("totalSizeBytes", totalSize);
            stats.put("totalSizeMB", totalSize / (1024.0 * 1024.0));
            
            // Limits and usage
            stats.put("maxEntries", MAX_ENTRIES_PER_USER);
            stats.put("maxContentSizeBytes", MAX_CONTENT_SIZE);
            stats.put("usagePercentage", (double) totalEntries / MAX_ENTRIES_PER_USER * 100);
            
            // Recent activity
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            long recentEntries = userStore.getAllExecutions().stream()
                .filter(entry -> entry.getTimestamp().isAfter(cutoff))
                .count() +
                userStore.getAllFiles().stream()
                .filter(entry -> entry.getTimestamp().isAfter(cutoff))
                .count();
            stats.put("recentEntriesLast7Days", recentEntries);
            
            // Oldest and newest entries
            Optional<LocalDateTime> oldestTimestamp = getAllTimestamps(userStore).stream().min(LocalDateTime::compareTo);
            Optional<LocalDateTime> newestTimestamp = getAllTimestamps(userStore).stream().max(LocalDateTime::compareTo);
            
            oldestTimestamp.ifPresent(timestamp -> stats.put("oldestEntry", timestamp));
            newestTimestamp.ifPresent(timestamp -> stats.put("newestEntry", timestamp));
            
            return stats;
            
        } catch (Exception e) {
            Map<String, Object> errorStats = new HashMap<>();
            errorStats.put("error", "Failed to calculate memory statistics: " + e.getMessage());
            return errorStats;
        }
    }
    
    /**
     * Apply retention policy to user's memory
     */
    public Map<String, Object> applyRetentionPolicy(String userId, int maxExecutions, int maxFiles, int maxDaysOld) {
        try {
            MemoryStore userStore = getUserMemoryStore(userId);
            
            // Count entries before cleanup
            int executionsBefore = userStore.getAllExecutions().size();
            int filesBefore = userStore.getAllFiles().size();
            
            // Apply retention policy
            userStore.applyRetentionPolicy(maxExecutions, maxFiles, maxDaysOld);
            
            // Count entries after cleanup
            int executionsAfter = userStore.getAllExecutions().size();
            int filesAfter = userStore.getAllFiles().size();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("executionsRemoved", executionsBefore - executionsAfter);
            result.put("filesRemoved", filesBefore - filesAfter);
            result.put("totalRemoved", (executionsBefore - executionsAfter) + (filesBefore - filesAfter));
            result.put("executionsRemaining", executionsAfter);
            result.put("filesRemaining", filesAfter);
            result.put("retentionPolicy", Map.of(
                "maxExecutions", maxExecutions,
                "maxFiles", maxFiles,
                "maxDaysOld", maxDaysOld
            ));
            
            return result;
            
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Failed to apply retention policy: " + e.getMessage());
            return errorResult;
        }
    }
    
    /**
     * Get memory context for injection into prompt parsing and DAG execution
     */
    public Map<String, Object> getMemoryContext(String userId, boolean includeHistory, int maxEntries, String types) {
        try {
            MemoryStore userStore = getUserMemoryStore(userId);
            Map<String, Object> context = new HashMap<>();
            
            if (includeHistory) {
                // Get recent executions
                List<ExecutionMemoryEntry> recentExecutions = userStore.getAllExecutions().stream()
                    .limit(maxEntries / 2)
                    .collect(Collectors.toList());
                
                // Get recent files
                List<FileMemoryEntry> recentFiles = userStore.getAllFiles().stream()
                    .limit(maxEntries / 2)
                    .collect(Collectors.toList());
                
                // Filter by types if specified
                if (types != null && !types.trim().isEmpty()) {
                    Set<String> typeSet = Set.of(types.split(","));
                    
                    if (!typeSet.contains("execution")) {
                        recentExecutions = List.of();
                    }
                    if (!typeSet.contains("file")) {
                        recentFiles = List.of();
                    }
                }
                
                context.put("recentExecutions", recentExecutions.stream()
                    .map(this::createContextEntry)
                    .collect(Collectors.toList()));
                
                context.put("recentFiles", recentFiles.stream()
                    .map(this::createContextEntry)
                    .collect(Collectors.toList()));
            }
            
            // Add user preferences
            UserContextMemory userContextMemory = userStore.getUserContext();
            context.put("userPreferences", userContextMemory.getPreferences());
            context.put("recentActions", userContextMemory.getRecentActions());
            
            // Add summary statistics
            context.put("memoryStats", Map.of(
                "totalExecutions", userStore.getAllExecutions().size(),
                "totalFiles", userStore.getAllFiles().size(),
                "lastActivity", getLastActivityTimestamp(userStore)
            ));
            
            return context;
            
        } catch (Exception e) {
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("error", "Failed to retrieve memory context: " + e.getMessage());
            return errorContext;
        }
    }
    
    // Helper methods
    
    private MemoryStore getUserMemoryStore(String userId) {
        // For now, return the shared memory store
        // In a real implementation, this would return user-specific stores
        return memoryStore;
    }
    
    private boolean checkStorageLimit(String userId) {
        MemoryStore userStore = getUserMemoryStore(userId);
        int totalEntries = userStore.getAllExecutions().size() + userStore.getAllFiles().size();
        return totalEntries < MAX_ENTRIES_PER_USER;
    }
    
    private String generateEntryId() {
        return "mem_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    private MemoryEntryResponse createFileEntry(MemoryStore store, String entryId, 
                                               MemoryEntryRequest request, LocalDateTime timestamp) {
        // Store file in memory store
        store.storeFile(request.getFilename(), request.getContentAsString(), 
                       request.getFilePath(), request.getExecutionId());
        
        return MemoryEntryResponse.created(entryId, "file", timestamp);
    }
    
    private MemoryEntryResponse createExecutionEntry(MemoryStore store, String entryId, 
                                                    MemoryEntryRequest request, LocalDateTime timestamp) {
        // Execution entries are typically created by the system, not manually
        return MemoryEntryResponse.validationError(entryId, 
            "Execution entries cannot be created manually");
    }
    
    private MemoryEntryResponse createGenericEntry(MemoryStore store, String entryId, 
                                                  MemoryEntryRequest request, LocalDateTime timestamp) {
        // For generic entries, we'll store them as user context preferences
        UserContextMemory userContext = store.getUserContext();
        userContext.setPreference(entryId, request.getContentAsString());
        
        return MemoryEntryResponse.created(entryId, request.getType(), timestamp);
    }
    
    private MemoryEntryResponse updateFileEntry(MemoryStore store, String entryId, 
                                               MemoryEntryRequest request, LocalDateTime timestamp) {
        // Update file in memory store
        store.storeFile(request.getFilename(), request.getContentAsString(), 
                       request.getFilePath(), request.getExecutionId());
        
        return MemoryEntryResponse.updated(entryId, "file", timestamp);
    }
    
    private MemoryEntryResponse updateGenericEntry(MemoryStore store, String entryId, 
                                                  MemoryEntryRequest request, LocalDateTime timestamp) {
        // Update generic entry in user context
        UserContextMemory userContext = store.getUserContext();
        userContext.setPreference(entryId, request.getContentAsString());
        
        return MemoryEntryResponse.updated(entryId, request.getType(), timestamp);
    }
    
    private boolean removeExecutionEntry(MemoryStore store, String entryId) {
        // This would need to be implemented in MemoryStore
        // For now, return false as execution entries are typically read-only
        return false;
    }
    
    private boolean removeFileEntry(MemoryStore store, String entryId) {
        // This would need to be implemented in MemoryStore
        // For now, return false as we don't have a remove method
        return false;
    }
    
    private List<MemoryEntryResponse> filterBySearch(List<MemoryEntryResponse> entries, String search) {
        String lowerSearch = search.toLowerCase();
        return entries.stream()
            .filter(entry -> {
                if (entry.getContent() != null && 
                    entry.getContent().toString().toLowerCase().contains(lowerSearch)) {
                    return true;
                }
                if (entry.getFilename() != null && 
                    entry.getFilename().toLowerCase().contains(lowerSearch)) {
                    return true;
                }
                if (entry.getDescription() != null && 
                    entry.getDescription().toLowerCase().contains(lowerSearch)) {
                    return true;
                }
                return false;
            })
            .collect(Collectors.toList());
    }
    
    private List<MemoryEntryResponse> filterByDateRange(List<MemoryEntryResponse> entries, 
                                                       String startDate, String endDate) {
        try {
            LocalDateTime start = startDate != null ? 
                LocalDateTime.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
            LocalDateTime end = endDate != null ? 
                LocalDateTime.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
            
            return entries.stream()
                .filter(entry -> {
                    LocalDateTime timestamp = entry.getTimestamp();
                    if (timestamp == null) return true;
                    
                    if (start != null && timestamp.isBefore(start)) return false;
                    if (end != null && timestamp.isAfter(end)) return false;
                    
                    return true;
                })
                .collect(Collectors.toList());
                
        } catch (DateTimeParseException e) {
            // If date parsing fails, return all entries
            return entries;
        }
    }
    
    private List<MemoryEntryResponse> sortEntries(List<MemoryEntryResponse> entries, 
                                                 String sortBy, String sortOrder) {
        Comparator<MemoryEntryResponse> comparator;
        
        switch (sortBy.toLowerCase()) {
            case "type":
                comparator = Comparator.comparing(entry -> entry.getType() != null ? entry.getType() : "");
                break;
            case "size":
                comparator = Comparator.comparing(entry -> entry.getSize() != null ? entry.getSize() : 0);
                break;
            case "filename":
                comparator = Comparator.comparing(entry -> entry.getFilename() != null ? entry.getFilename() : "");
                break;
            default: // timestamp
                comparator = Comparator.comparing(entry -> entry.getTimestamp() != null ? entry.getTimestamp() : LocalDateTime.MIN);
                break;
        }
        
        if ("asc".equalsIgnoreCase(sortOrder)) {
            return entries.stream().sorted(comparator).collect(Collectors.toList());
        } else {
            return entries.stream().sorted(comparator.reversed()).collect(Collectors.toList());
        }
    }
    
    private Map<String, Object> createSearchCriteriaMap(MemorySearchCriteria criteria) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", criteria.type);
        map.put("search", criteria.search);
        map.put("startDate", criteria.startDate);
        map.put("endDate", criteria.endDate);
        map.put("limit", criteria.limit);
        map.put("offset", criteria.offset);
        map.put("sortBy", criteria.sortBy);
        map.put("sortOrder", criteria.sortOrder);
        return map;
    }
    
    private long calculateTotalStorageSize(MemoryStore store) {
        long totalSize = 0;
        
        // Calculate size of execution entries
        for (ExecutionMemoryEntry entry : store.getAllExecutions()) {
            if (entry.getOutputs() != null) {
                totalSize += entry.getOutputs().toString().length();
            }
        }
        
        // Calculate size of file entries
        for (FileMemoryEntry entry : store.getAllFiles()) {
            totalSize += entry.getSize();
        }
        
        return totalSize;
    }
    
    private List<LocalDateTime> getAllTimestamps(MemoryStore store) {
        List<LocalDateTime> timestamps = new ArrayList<>();
        
        store.getAllExecutions().forEach(entry -> timestamps.add(entry.getTimestamp()));
        store.getAllFiles().forEach(entry -> timestamps.add(entry.getTimestamp()));
        
        return timestamps;
    }
    
    private Map<String, Object> createContextEntry(ExecutionMemoryEntry entry) {
        Map<String, Object> contextEntry = new HashMap<>();
        contextEntry.put("id", entry.getExecutionId());
        contextEntry.put("type", "execution");
        contextEntry.put("timestamp", entry.getTimestamp());
        contextEntry.put("successful", entry.isSuccessful());
        contextEntry.put("outputs", entry.getOutputs());
        return contextEntry;
    }
    
    private Map<String, Object> createContextEntry(FileMemoryEntry entry) {
        Map<String, Object> contextEntry = new HashMap<>();
        contextEntry.put("id", entry.getFilename());
        contextEntry.put("type", "file");
        contextEntry.put("timestamp", entry.getTimestamp());
        contextEntry.put("filename", entry.getFilename());
        contextEntry.put("content", entry.getContent());
        contextEntry.put("size", entry.getSize());
        return contextEntry;
    }
    
    private LocalDateTime getLastActivityTimestamp(MemoryStore store) {
        List<LocalDateTime> timestamps = getAllTimestamps(store);
        return timestamps.stream().max(LocalDateTime::compareTo).orElse(null);
    }
    
    /**
     * Search criteria class for memory entries
     */
    public static class MemorySearchCriteria {
        public final String type;
        public final String search;
        public final String startDate;
        public final String endDate;
        public final int limit;
        public final int offset;
        public final String sortBy;
        public final String sortOrder;
        
        public MemorySearchCriteria(String type, String search, String startDate, String endDate,
                                   int limit, int offset, String sortBy, String sortOrder) {
            this.type = type;
            this.search = search;
            this.startDate = startDate;
            this.endDate = endDate;
            this.limit = limit;
            this.offset = offset;
            this.sortBy = sortBy;
            this.sortOrder = sortOrder;
        }
    }
}