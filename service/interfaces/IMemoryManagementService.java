package api.service.interfaces;

import api.model.MemoryEntryRequest;
import api.model.MemoryEntryResponse;
import api.model.MemorySearchResponse;

import java.util.Map;

/**
 * Interface for user memory management service.
 * 
 * This service provides comprehensive memory management capabilities for maintaining
 * user context, execution history, and file storage across sessions:
 * - Persistent memory storage with user isolation
 * - Advanced search and filtering capabilities
 * - Memory lifecycle management with retention policies
 * - Context injection for prompt parsing and DAG execution
 * - Storage quota management and optimization
 * 
 * Patent Claims Coverage:
 * - Claim 4: Memory-aware execution context management
 * - Claim 11: Persistent memory storage with contextual retrieval
 * - Claim 13: User-isolated memory management with security boundaries
 * - Claim 15: Memory retention policies and lifecycle management
 * 
 * @author Obvian Engineering Team
 * @since 1.0.0
 */
public interface IMemoryManagementService {

    /**
     * Search memory entries with advanced filtering and pagination.
     * 
     * Provides comprehensive memory search capabilities:
     * - Multi-type filtering (execution, file, context entries)
     * - Full-text search across content and metadata
     * - Date range filtering and temporal sorting
     * - Pagination with configurable limits
     * - Security-aware results filtering
     * 
     * Patent Coverage: Contextual memory retrieval per Claim 11
     * 
     * @param userId User identifier for memory isolation
     * @param criteria Search criteria including filters, pagination, and sort options
     * @return MemorySearchResponse containing matched entries, pagination info, and search metadata
     * @throws SecurityException if user lacks access to requested memory scope
     */
    MemorySearchResponse searchMemoryEntries(String userId, MemorySearchCriteria criteria);

    /**
     * Retrieve a specific memory entry by ID.
     * 
     * Provides secure access to individual memory entries with:
     * - User ownership validation
     * - Content access control
     * - Metadata enrichment
     * - Related entry suggestions
     * 
     * Patent Coverage: Memory isolation per Claim 13
     * 
     * @param userId User identifier for security validation
     * @param entryId Unique identifier for the memory entry
     * @return MemoryEntryResponse containing entry data and metadata
     * @throws NotFoundException if entry does not exist
     * @throws SecurityException if user lacks access to the entry
     */
    MemoryEntryResponse getMemoryEntry(String userId, String entryId);

    /**
     * Create a new memory entry.
     * 
     * Provides secure memory entry creation with:
     * - Content validation and size limits
     * - Storage quota enforcement
     * - Automatic metadata generation
     * - Entry type-specific processing
     * 
     * Patent Coverage: Memory management per Claim 15
     * 
     * @param userId User identifier for ownership assignment
     * @param request Memory entry creation request with content and metadata
     * @return MemoryEntryResponse containing created entry information
     * @throws ValidationException if entry content is invalid
     * @throws QuotaExceededException if storage limits are exceeded
     */
    MemoryEntryResponse createMemoryEntry(String userId, MemoryEntryRequest request);

    /**
     * Update an existing memory entry.
     * 
     * Provides controlled memory entry modification with:
     * - Ownership validation
     * - Version tracking
     * - Content validation
     * - Immutability enforcement for execution entries
     * 
     * Patent Coverage: Memory lifecycle per Claim 15
     * 
     * @param userId User identifier for security validation
     * @param entryId Unique identifier for the entry to update
     * @param request Updated entry content and metadata
     * @return MemoryEntryResponse containing updated entry information
     * @throws NotFoundException if entry does not exist
     * @throws SecurityException if user lacks modification rights
     * @throws ImmutabilityException if entry type does not support updates
     */
    MemoryEntryResponse updateMemoryEntry(String userId, String entryId, MemoryEntryRequest request);

    /**
     * Delete a memory entry.
     * 
     * Provides secure entry deletion with:
     * - Ownership validation
     * - Dependency checking
     * - Soft deletion options
     * - Audit trail maintenance
     * 
     * Patent Coverage: Memory lifecycle per Claim 15
     * 
     * @param userId User identifier for security validation
     * @param entryId Unique identifier for the entry to delete
     * @return true if deletion was successful, false if entry was not found
     * @throws SecurityException if user lacks deletion rights
     * @throws DependencyException if entry has active dependencies
     */
    boolean deleteMemoryEntry(String userId, String entryId);

    /**
     * Clear all memory entries for a user.
     * 
     * Provides bulk memory cleanup with:
     * - Complete user memory reset
     * - Dependency validation
     * - Audit logging
     * - Recovery options
     * 
     * Patent Coverage: Memory management per Claim 15
     * 
     * @param userId User identifier for memory scope
     * @return Number of entries cleared
     * @throws SecurityException if user lacks clear permission
     */
    int clearMemory(String userId);

    /**
     * Get memory statistics for a user.
     * 
     * Provides comprehensive memory usage analytics:
     * - Storage utilization and quotas
     * - Entry counts by type and status
     * - Activity patterns and trends
     * - Optimization recommendations
     * 
     * Patent Coverage: Memory analytics per Claim 13
     * 
     * @param userId User identifier for statistics scope
     * @return Map containing detailed memory statistics and analytics
     * @throws SecurityException if user lacks access to memory statistics
     */
    Map<String, Object> getMemoryStats(String userId);

    /**
     * Apply retention policy to user's memory.
     * 
     * Provides automated memory lifecycle management:
     * - Configurable retention rules
     * - Age-based and count-based cleanup
     * - Selective retention by importance
     * - Impact analysis and reporting
     * 
     * Patent Coverage: Memory lifecycle per Claim 15
     * 
     * @param userId User identifier for policy application
     * @param maxExecutions Maximum execution entries to retain
     * @param maxFiles Maximum file entries to retain
     * @param maxDaysOld Maximum age in days for entry retention
     * @return Map containing policy application results and statistics
     * @throws SecurityException if user lacks policy management rights
     */
    Map<String, Object> applyRetentionPolicy(String userId, int maxExecutions, int maxFiles, int maxDaysOld);

    /**
     * Get memory context for injection into prompt parsing and DAG execution.
     * 
     * Provides contextual memory integration:
     * - Recent activity filtering
     * - Context-aware entry selection
     * - Memory-based parameter resolution
     * - User preference integration
     * 
     * Patent Coverage: Context-aware execution per Claim 4
     * 
     * @param userId User identifier for context scope
     * @param includeHistory Whether to include execution history in context
     * @param maxEntries Maximum number of entries to include
     * @param types Comma-separated list of entry types to include
     * @return Map containing contextual memory data for execution
     * @throws SecurityException if user lacks context access rights
     */
    Map<String, Object> getMemoryContext(String userId, boolean includeHistory, int maxEntries, String types);

    /**
     * Search criteria class for memory entries with comprehensive filtering options.
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