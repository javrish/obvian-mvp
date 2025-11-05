package api.controller;

import api.service.StatusMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket controller for real-time execution updates
 */
@Controller
public class ExecutionWebSocketController {
    
    private final StatusMonitoringService statusMonitoringService;
    
    @Autowired
    public ExecutionWebSocketController(StatusMonitoringService statusMonitoringService) {
        this.statusMonitoringService = statusMonitoringService;
    }
    
    /**
     * Handle subscription to execution updates
     */
    @SubscribeMapping("/topic/executions/{executionId}")
    public Map<String, Object> subscribeToExecution(@DestinationVariable String executionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "subscription_confirmed");
        response.put("executionId", executionId);
        response.put("message", "Subscribed to execution updates");
        response.put("timestamp", System.currentTimeMillis());
        
        // Send current status if available
        try {
            var currentStatus = statusMonitoringService.getExecutionStatus(executionId);
            response.put("currentStatus", currentStatus);
        } catch (Exception e) {
            response.put("error", "Could not retrieve current status");
        }
        
        return response;
    }
    
    /**
     * Handle subscription to all execution updates
     */
    @SubscribeMapping("/topic/executions/all")
    public Map<String, Object> subscribeToAllExecutions() {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "subscription_confirmed");
        response.put("scope", "all_executions");
        response.put("message", "Subscribed to all execution updates");
        response.put("timestamp", System.currentTimeMillis());
        
        // Add current metrics
        response.put("activeExecutions", statusMonitoringService.getActiveExecutionCount());
        response.put("totalExecutions", statusMonitoringService.getTotalExecutionCount());
        
        return response;
    }
    
    /**
     * Handle ping messages for connection health
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, Object> handlePing(Map<String, Object> message) {
        Map<String, Object> pong = new HashMap<>();
        pong.put("type", "pong");
        pong.put("timestamp", System.currentTimeMillis());
        pong.put("originalMessage", message);
        return pong;
    }
    
    /**
     * Handle execution control messages (pause, resume, cancel)
     */
    @MessageMapping("/executions/{executionId}/control")
    @SendTo("/topic/executions/{executionId}")
    public Map<String, Object> handleExecutionControl(
            @DestinationVariable String executionId,
            Map<String, Object> controlMessage) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "control_response");
        response.put("executionId", executionId);
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            String action = (String) controlMessage.get("action");
            
            switch (action) {
                case "cancel":
                    boolean cancelled = statusMonitoringService.cancelExecution(executionId);
                    response.put("action", "cancel");
                    response.put("success", cancelled);
                    response.put("message", cancelled ? 
                        "Execution cancelled successfully" : 
                        "Failed to cancel execution");
                    break;
                    
                case "status":
                    var status = statusMonitoringService.getExecutionStatus(executionId);
                    response.put("action", "status");
                    response.put("success", true);
                    response.put("status", status);
                    break;
                    
                case "resume":
                    boolean canResume = statusMonitoringService.canResumeExecution(executionId);
                    if (canResume) {
                        boolean resumed = statusMonitoringService.resumeExecution(executionId);
                        response.put("action", "resume");
                        response.put("success", resumed);
                        response.put("message", resumed ? 
                            "Execution resumed successfully" : 
                            "Failed to resume execution");
                    } else {
                        response.put("action", "resume");
                        response.put("success", false);
                        response.put("message", "Execution cannot be resumed");
                    }
                    break;
                    
                case "recovery_check":
                    boolean recoverable = statusMonitoringService.canResumeExecution(executionId);
                    response.put("action", "recovery_check");
                    response.put("success", true);
                    response.put("recoverable", recoverable);
                    
                    if (recoverable) {
                        var recoveredState = statusMonitoringService.recoverExecutionState(executionId);
                        response.put("recoveredState", recoveredState);
                    }
                    break;
                    
                default:
                    response.put("action", action);
                    response.put("success", false);
                    response.put("message", "Unknown action: " + action);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Control action failed: " + e.getMessage());
        }
        
        return response;
    }
}