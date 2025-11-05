package api.controller;

import api.model.ExecutionStatusResponse;
import api.service.StatusMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for execution monitoring and status endpoints
 */
@RestController
@RequestMapping("/api/v1/executions")
public class StatusController {
    
    private final StatusMonitoringService statusMonitoringService;
    
    @Autowired
    public StatusController(StatusMonitoringService statusMonitoringService) {
        this.statusMonitoringService = statusMonitoringService;
    }
    
    /**
     * Get execution status for any execution (prompt or DAG)
     * GET /api/v1/executions/{id}/status
     */
    @GetMapping("/{executionId}/status")
    public ResponseEntity<ExecutionStatusResponse> getExecutionStatus(
            @PathVariable String executionId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        
        try {
            ExecutionStatusResponse response = statusMonitoringService.getExecutionStatus(executionId);
            
            // Handle ETag for efficient polling
            String etag = statusMonitoringService.generateETag(response);
            if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
            
            HttpStatus status = determineHttpStatus(response);
            return ResponseEntity.status(status)
                    .header("ETag", etag)
                    .header("Cache-Control", "no-cache")
                    .body(response);
            
        } catch (Exception e) {
            ExecutionStatusResponse errorResponse = ExecutionStatusResponse.failed(
                executionId, 
                createErrorPromptResponse(executionId, "Failed to get status: " + e.getMessage())
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get execution history with pagination and filtering
     * GET /api/v1/executions/status-history
     */
    @GetMapping("/status-history")
    public ResponseEntity<Map<String, Object>> getExecutionHistory(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        try {
            // Validate pagination parameters
            if (limit > 100) {
                limit = 100; // Max limit
            }
            if (limit <= 0) {
                limit = 20; // Default limit
            }
            if (offset < 0) {
                offset = 0;
            }
            
            Map<String, Object> filters = new HashMap<>();
            if (status != null) filters.put("status", status);
            if (type != null) filters.put("type", type);
            if (userId != null) filters.put("userId", userId);
            if (startDate != null) filters.put("startDate", startDate);
            if (endDate != null) filters.put("endDate", endDate);
            
            Map<String, Object> response = statusMonitoringService.getExecutionHistory(
                offset, limit, filters);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve execution history: " + e.getMessage());
            errorResponse.put("errorType", "INTERNAL_ERROR");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Cancel an execution
     * DELETE /api/v1/executions/{id}
     */
    @DeleteMapping("/{executionId}")
    public ResponseEntity<Map<String, Object>> cancelExecution(@PathVariable String executionId) {
        try {
            boolean cancelled = statusMonitoringService.cancelExecution(executionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("executionId", executionId);
            response.put("cancelled", cancelled);
            response.put("message", cancelled ? 
                "Execution cancelled successfully" : 
                "Execution not found or already completed");
            
            HttpStatus status = cancelled ? HttpStatus.OK : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("executionId", executionId);
            errorResponse.put("cancelled", false);
            errorResponse.put("error", "Failed to cancel execution: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get detailed execution trace
     * GET /api/v1/executions/{id}/trace
     */
    @GetMapping("/{executionId}/trace")
    public ResponseEntity<Map<String, Object>> getExecutionTrace(@PathVariable String executionId) {
        try {
            Map<String, Object> trace = statusMonitoringService.getExecutionTrace(executionId);
            
            if (trace == null || trace.isEmpty()) {
                Map<String, Object> notFoundResponse = new HashMap<>();
                notFoundResponse.put("executionId", executionId);
                notFoundResponse.put("error", "Trace not found");
                notFoundResponse.put("errorType", "NOT_FOUND");
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundResponse);
            }
            
            return ResponseEntity.ok(trace);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("executionId", executionId);
            errorResponse.put("error", "Failed to retrieve trace: " + e.getMessage());
            errorResponse.put("errorType", "INTERNAL_ERROR");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get execution metrics and statistics
     * GET /api/v1/executions/metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getExecutionMetrics(
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String userId) {
        
        try {
            Map<String, Object> filters = new HashMap<>();
            if (timeRange != null) filters.put("timeRange", timeRange);
            if (userId != null) filters.put("userId", userId);
            
            Map<String, Object> metrics = statusMonitoringService.getExecutionMetrics(filters);
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve metrics: " + e.getMessage());
            errorResponse.put("errorType", "INTERNAL_ERROR");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Register webhook for execution completion notifications
     * POST /api/v1/executions/{id}/webhook
     */
    @PostMapping("/{executionId}/webhook")
    public ResponseEntity<Map<String, Object>> registerWebhook(
            @PathVariable String executionId,
            @RequestBody Map<String, Object> webhookRequest) {
        
        try {
            String webhookUrl = (String) webhookRequest.get("url");
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Webhook URL is required");
                errorResponse.put("errorType", "VALIDATION_ERROR");
                
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) webhookRequest.get("headers");
            String secret = (String) webhookRequest.get("secret");
            
            boolean registered = statusMonitoringService.registerWebhook(
                executionId, webhookUrl, headers, secret);
            
            Map<String, Object> response = new HashMap<>();
            response.put("executionId", executionId);
            response.put("webhookUrl", webhookUrl);
            response.put("registered", registered);
            response.put("message", registered ? 
                "Webhook registered successfully" : 
                "Failed to register webhook");
            
            HttpStatus status = registered ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("executionId", executionId);
            errorResponse.put("error", "Failed to register webhook: " + e.getMessage());
            errorResponse.put("errorType", "INTERNAL_ERROR");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check for status monitoring service
     * GET /api/v1/executions/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "StatusMonitoringService");
        health.put("timestamp", System.currentTimeMillis());
        
        // Add component health checks
        Map<String, String> components = statusMonitoringService.getComponentHealth();
        health.put("components", components);
        
        // Add service metrics
        Map<String, Object> serviceMetrics = new HashMap<>();
        serviceMetrics.put("activeExecutions", statusMonitoringService.getActiveExecutionCount());
        serviceMetrics.put("totalExecutions", statusMonitoringService.getTotalExecutionCount());
        serviceMetrics.put("webhookSubscriptions", statusMonitoringService.getWebhookSubscriptionCount());
        health.put("metrics", serviceMetrics);
        
        return ResponseEntity.ok(health);
    }
    
    private HttpStatus determineHttpStatus(ExecutionStatusResponse response) {
        switch (response.getStatus()) {
            case PENDING:
            case RUNNING:
                return HttpStatus.ACCEPTED;
            case COMPLETED:
                return HttpStatus.OK;
            case FAILED:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case CANCELLED:
                return HttpStatus.OK;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
    
    private api.model.PromptExecutionResponse createErrorPromptResponse(String executionId, String message) {
        return api.model.PromptExecutionResponse.failure(executionId, message, "INTERNAL_ERROR");
    }
}