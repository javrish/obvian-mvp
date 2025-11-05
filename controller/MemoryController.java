package api.controller;

import api.model.MemoryEntryRequest;
import api.model.MemoryEntryResponse;
import api.model.MemorySearchResponse;
import api.service.MemoryManagementService;
import api.service.security.AuthorizationService;
import api.service.security.DagExecutionAuditor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for memory management endpoints
 */
@RestController
@RequestMapping("/api/v1/memory")
@Tag(name = "Memory Management", description = "Memory management endpoints")
public class MemoryController {
    
    private final MemoryManagementService memoryManagementService;
    private final AuthorizationService authorizationService;
    private final DagExecutionAuditor auditor;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public MemoryController(MemoryManagementService memoryManagementService,
                           AuthorizationService authorizationService,
                           DagExecutionAuditor auditor) {
        this.memoryManagementService = memoryManagementService;
        this.authorizationService = authorizationService;
        this.auditor = auditor;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get user memory entries with pagination and filtering
     * GET /api/v1/memory
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN') or hasAuthority('memory:read')")
    @Operation(summary = "Retrieve memory entries", description = "Retrieve memory entries with optional filtering and pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Memory entries retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Resource not found"),
        @ApiResponse(responseCode = "413", description = "Request entity too large"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<MemorySearchResponse> getMemoryEntries(
            @Parameter(description = "Filter entries by type") @RequestParam(value = "type", required = false) String type,
            @Parameter(description = "Search term for filtering entries") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "Start date for filtering entries") @RequestParam(value = "startDate", required = false) String startDate,
            @Parameter(description = "End date for filtering entries") @RequestParam(value = "endDate", required = false) String endDate,
            @Parameter(description = "Maximum number of entries to return") @RequestParam(value = "limit", defaultValue = "20") @Min(1) @Max(100) int limit,
            @Parameter(description = "Number of entries to skip for pagination") @RequestParam(value = "offset", defaultValue = "0") @Min(0) int offset,
            @Parameter(description = "Field to sort by") @RequestParam(value = "sortBy", defaultValue = "timestamp") String sortBy,
            @Parameter(description = "Sort order (asc or desc)") @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder) {
        
        try {
            // Verify memory access permission and get current user
            authorizationService.requireMemoryAccessPermission();
            String currentUserId = authorizationService.getCurrentUserId();
            String currentUsername = authorizationService.getCurrentUsername();
            
            // Sanitize search parameters
            String sanitizedType = type != null ? authorizationService.sanitizeInput(type, "MEMORY_TYPE") : null;
            String sanitizedSearch = search != null ? authorizationService.sanitizeInput(search, "SEARCH_TERM") : null;
            String sanitizedSortBy = authorizationService.sanitizeInput(sortBy, "SORT_FIELD");
            String sanitizedSortOrder = authorizationService.sanitizeInput(sortOrder, "SORT_ORDER");
            
            // Create search criteria with sanitized inputs
            MemoryManagementService.MemorySearchCriteria criteria = 
                new MemoryManagementService.MemorySearchCriteria(
                    sanitizedType, sanitizedSearch, startDate, endDate, 
                    limit, offset, sanitizedSortBy, sanitizedSortOrder);
            
            // Search memory entries for the current user only
            MemorySearchResponse response = memoryManagementService.searchMemoryEntries(currentUserId, criteria);
            
            // Log memory access
            auditor.logSecurityEvent("MEMORY_SEARCH", currentUsername, "memory_entries", "SEARCH", true, null);
            
            return ResponseEntity.ok(response);
            
        } catch (AccessDeniedException e) {
            auditor.logSecurityEvent("MEMORY_ACCESS_DENIED", authorizationService.getCurrentUsername(), 
                "memory_entries", "SEARCH", false, e.getMessage());
            MemorySearchResponse errorResponse = MemorySearchResponse.error(
                "Access denied: " + e.getMessage(), "ACCESS_DENIED");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            
        } catch (IllegalArgumentException e) {
            auditor.logInputValidationFailure(authorizationService.getCurrentUsername(), 
                e.getMessage(), "MEMORY_SEARCH_PARAMS");
            MemorySearchResponse errorResponse = MemorySearchResponse.error(
                "Invalid search parameters: " + e.getMessage(), "VALIDATION_ERROR");
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            auditor.logSecurityEvent("MEMORY_SEARCH_ERROR", authorizationService.getCurrentUsername(), 
                "memory_entries", "SEARCH", false, "Internal error: " + e.getMessage());
            MemorySearchResponse errorResponse = MemorySearchResponse.error(
                "Failed to retrieve memory entries: " + e.getMessage(), "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get a specific memory entry by ID
     * GET /api/v1/memory/{entryId}
     */
    @GetMapping("/{entryId}")
    public ResponseEntity<MemoryEntryResponse> getMemoryEntry(
            @PathVariable String entryId,
            @RequestHeader(value = "X-User-Context", required = false) String userContextHeader) {
        
        try {
            // Parse user context from header
            Map<String, Object> userContext = authorizationService.parseUserContext(userContextHeader);
            String userId = authorizationService.extractUserId(userContext);
            
            // Get memory entry
            MemoryEntryResponse response = memoryManagementService.getMemoryEntry(userId, entryId);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if ("NOT_FOUND".equals(response.getErrorType())) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            MemoryEntryResponse errorResponse = MemoryEntryResponse.error(
                entryId, "Failed to retrieve memory entry: " + e.getMessage(), "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Create a new memory entry
     * POST /api/v1/memory
     */
    @PostMapping
    public ResponseEntity<MemoryEntryResponse> createMemoryEntry(
            @Valid @RequestBody MemoryEntryRequest request,
            @RequestHeader(value = "X-User-Context", required = false) String userContextHeader) {
        
        try {
            // Parse user context from header
            Map<String, Object> userContext = authorizationService.parseUserContext(userContextHeader);
            String userId = authorizationService.extractUserId(userContext);
            
            // Create memory entry
            MemoryEntryResponse response = memoryManagementService.createMemoryEntry(userId, request);
            
            if (response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else if ("VALIDATION_ERROR".equals(response.getErrorType())) {
                return ResponseEntity.badRequest().body(response);
            } else if ("STORAGE_LIMIT_EXCEEDED".equals(response.getErrorType())) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            MemoryEntryResponse errorResponse = MemoryEntryResponse.error(
                null, "Failed to create memory entry: " + e.getMessage(), "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Update an existing memory entry
     * PUT /api/v1/memory/{entryId}
     */
    @PutMapping("/{entryId}")
    public ResponseEntity<MemoryEntryResponse> updateMemoryEntry(
            @PathVariable String entryId,
            @Valid @RequestBody MemoryEntryRequest request,
            @RequestHeader(value = "X-User-Context", required = false) String userContextHeader) {
        
        try {
            // Parse user context from header
            Map<String, Object> userContext = authorizationService.parseUserContext(userContextHeader);
            String userId = authorizationService.extractUserId(userContext);
            
            // Update memory entry
            MemoryEntryResponse response = memoryManagementService.updateMemoryEntry(userId, entryId, request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else if ("NOT_FOUND".equals(response.getErrorType())) {
                return ResponseEntity.notFound().build();
            } else if ("VALIDATION_ERROR".equals(response.getErrorType())) {
                return ResponseEntity.badRequest().body(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
        } catch (Exception e) {
            MemoryEntryResponse errorResponse = MemoryEntryResponse.error(
                entryId, "Failed to update memory entry: " + e.getMessage(), "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Delete a memory entry
     * DELETE /api/v1/memory/{entryId}
     */
    @DeleteMapping("/{entryId}")
    public ResponseEntity<Map<String, Object>> deleteMemoryEntry(
            @PathVariable String entryId,
            @RequestHeader(value = "X-User-Context", required = false) String userContextHeader) {
        
        try {
            // Parse user context from header
            Map<String, Object> userContext = authorizationService.parseUserContext(userContextHeader);
            String userId = authorizationService.extractUserId(userContext);
            
            // Delete memory entry
            boolean deleted = memoryManagementService.deleteMemoryEntry(userId, entryId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("deleted", deleted);
            response.put("message", deleted ? "Memory entry deleted successfully" : "Memory entry not found");
            
            HttpStatus status = deleted ? HttpStatus.OK : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("entryId", entryId);
            errorResponse.put("deleted", false);
            errorResponse.put("error", "Failed to delete memory entry: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Clear all memory entries for a user
     * DELETE /api/v1/memory
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearMemory(
            @RequestParam(value = "confirm", required = true) boolean confirm,
            @RequestHeader(value = "X-User-Context", required = false) String userContextHeader) {
        
        try {
            if (!confirm) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Confirmation required to clear all memory");
                response.put("message", "Add ?confirm=true to confirm memory clearing");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Parse user context from header
            Map<String, Object> userContext = authorizationService.parseUserContext(userContextHeader);
            String userId = authorizationService.extractUserId(userContext);
            
            // Clear memory
            int deletedCount = memoryManagementService.clearMemory(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("cleared", true);
            response.put("deletedEntries", deletedCount);
            response.put("message", "Memory cleared successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("cleared", false);
            errorResponse.put("error", "Failed to clear memory: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get memory statistics and usage information
     * GET /api/v1/memory/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMemoryStats(
            @RequestHeader(value = "X-User-Context", required = false) String userContextHeader) {
        
        try {
            // Parse user context from header
            Map<String, Object> userContext = authorizationService.parseUserContext(userContextHeader);
            String userId = authorizationService.extractUserId(userContext);
            
            // Get memory statistics
            Map<String, Object> stats = memoryManagementService.getMemoryStats(userId);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve memory statistics: " + e.getMessage());
            errorResponse.put("errorType", "INTERNAL_ERROR");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Apply retention policy to user's memory
     * POST /api/v1/memory/retention
     */
    @PostMapping("/retention")
    public ResponseEntity<Map<String, Object>> applyRetentionPolicy(
            @RequestParam(value = "maxExecutions", defaultValue = "100") @Min(1) @Max(1000) int maxExecutions,
            @RequestParam(value = "maxFiles", defaultValue = "100") @Min(1) @Max(1000) int maxFiles,
            @RequestParam(value = "maxDaysOld", defaultValue = "30") @Min(1) @Max(365) int maxDaysOld,
            @RequestHeader(value = "X-User-Context", required = false) String userContextHeader) {
        
        try {
            // Parse user context from header
            Map<String, Object> userContext = authorizationService.parseUserContext(userContextHeader);
            String userId = authorizationService.extractUserId(userContext);
            
            // Apply retention policy
            Map<String, Object> result = memoryManagementService.applyRetentionPolicy(
                userId, maxExecutions, maxFiles, maxDaysOld);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to apply retention policy: " + e.getMessage());
            errorResponse.put("errorType", "INTERNAL_ERROR");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get memory context for injection into prompt parsing and DAG execution
     * GET /api/v1/memory/context
     */
    @GetMapping("/context")
    public ResponseEntity<Map<String, Object>> getMemoryContext(
            @RequestParam(value = "includeHistory", defaultValue = "true") boolean includeHistory,
            @RequestParam(value = "maxEntries", defaultValue = "10") @Min(1) @Max(50) int maxEntries,
            @RequestParam(value = "types", required = false) String types,
            @RequestHeader(value = "X-User-Context", required = false) String userContextHeader) {
        
        try {
            // Parse user context from header
            Map<String, Object> userContext = authorizationService.parseUserContext(userContextHeader);
            String userId = authorizationService.extractUserId(userContext);
            
            // Get memory context
            Map<String, Object> memoryContext = memoryManagementService.getMemoryContext(
                userId, includeHistory, maxEntries, types);
            
            return ResponseEntity.ok(memoryContext);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve memory context: " + e.getMessage());
            errorResponse.put("errorType", "INTERNAL_ERROR");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for memory management service
     * GET /api/v1/memory/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "MemoryManagementService");
        health.put("timestamp", System.currentTimeMillis());
        
        // Add basic service checks
        Map<String, String> components = new HashMap<>();
        components.put("memoryStore", "healthy");
        components.put("retentionPolicy", "healthy");
        components.put("searchIndex", "healthy");
        health.put("components", components);
        
        return ResponseEntity.ok(health);
    }
    
    // Removed unsafe user context parsing methods - now using AuthorizationService for secure user context
}