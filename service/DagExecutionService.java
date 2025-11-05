package api.service;

import api.model.DagExecutionRequest;
import api.model.DagExecutionResponse;
import api.model.ExecutionStatusResponse;
import api.entity.Execution;
import api.entity.ExecutionStatus;
import api.entity.User;
import api.repository.ExecutionRepository;
import api.repository.UserRepository;
import core.*;
import core.explainability.ExecutionTrace;
import core.interfaces.DagExecutorService;
import core.interfaces.DagValidatorService;
import core.factory.ServiceFactory;
import memory.MemoryStoreInterface;
import plugins.PluginRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for executing structured DAGs through the API
 */
@Service
public class DagExecutionService {
    
    private final DagExecutorService dagExecutorService;
    private final DagValidatorService dagValidatorService;
    private final PluginRouter pluginRouter;
    private final MemoryStoreInterface memoryStore;
    private final ServiceFactory serviceFactory;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StatusMonitoringService statusMonitoringService;
    private final ExecutionQueueService queueService;
    private final ExecutorService asyncExecutor;
    private final Map<String, CompletableFuture<DagExecutionResponse>> runningExecutions;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final ExplainabilityService explainabilityService;
    private final RealtimeExecutionService realtimeExecutionService;
    private final ExecutionRepository executionRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public DagExecutionService(MemoryStoreInterface memoryStore, 
                              RedisTemplate<String, Object> redisTemplate,
                              StatusMonitoringService statusMonitoringService,
                              @Lazy ExecutionQueueService queueService, 
                              PluginRouter pluginRouter,
                              ServiceFactory serviceFactory,
                              @Autowired(required = false) ExplainabilityService explainabilityService,
                              @Autowired(required = false) RealtimeExecutionService realtimeExecutionService,
                              @Autowired(required = false) ExecutionRepository executionRepository,
                              @Autowired(required = false) UserRepository userRepository) {
        this.memoryStore = memoryStore;
        this.redisTemplate = redisTemplate;
        this.statusMonitoringService = statusMonitoringService;
        this.queueService = queueService;
        this.pluginRouter = pluginRouter;
        this.serviceFactory = serviceFactory;
        this.explainabilityService = explainabilityService;
        this.realtimeExecutionService = realtimeExecutionService;
        this.executionRepository = executionRepository;
        this.userRepository = userRepository;
        
        // Validate that all required plugins are available (skip for testing with null router)
        if (this.pluginRouter != null) {
            plugins.PluginRouterFactory.validateExistingRouter(this.pluginRouter);
        }
        
        // Use ServiceFactory to create interface-based services
        this.dagValidatorService = serviceFactory.createDagValidatorService();
        
        // Create DagExecutorService with progress callback that integrates with ExplainabilityService
        ProgressCallback progressCallback = createProgressCallback();
        ServiceFactory.ExecutorConfiguration executorConfig = new ServiceFactory.ExecutorConfiguration()
            .withPluginRouter(pluginRouter)
            .withMemoryStore(memoryStore)
            .withProgressCallback(progressCallback);
        this.dagExecutorService = serviceFactory.createCustomDagExecutorService(executorConfig);
        
        this.asyncExecutor = Executors.newCachedThreadPool();
        this.runningExecutions = new ConcurrentHashMap<>();
    }
    
    // Plugin registration is now handled by PluginRouterFactory for consistency
    
    /**
     * Create a progress callback that integrates with ExplainabilityService and RealtimeExecutionService
     */
    private ProgressCallback createProgressCallback() {
        return new ProgressCallback() {
            @Override
            public void onExecutionStart(String executionId, int totalNodes, String dagId) {
                // Start real-time tracking
                if (realtimeExecutionService != null) {
                    realtimeExecutionService.startExecution(executionId);
                }
            }
            
            @Override
            public void onExecutionComplete(String executionId, boolean success, int completedNodes, 
                                          int totalNodes, Object executionResult) {
                // Store completed trace in ExplainabilityService
                if (explainabilityService != null && dagExecutorService != null) {
                    ExecutionTrace trace = dagExecutorService.getExecutionTrace(executionId);
                    if (trace != null) {
                        explainabilityService.storeExecutionTrace(executionId, trace);
                    }
                }
                
                // Complete real-time tracking
                if (realtimeExecutionService != null) {
                    ExecutionStatus status = success ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED;
                    String result = executionResult != null ? executionResult.toString() : null;
                    realtimeExecutionService.completeExecution(executionId, status, result, null);
                }
            }
            
            @Override
            public void onNodeStart(String executionId, String nodeId, String action, 
                                  int completedNodes, int totalNodes) {
                // Broadcast node start via WebSocket
                if (realtimeExecutionService != null) {
                    Map<String, Object> nodeData = new HashMap<>();
                    nodeData.put("action", action);
                    nodeData.put("progress", (double) completedNodes / totalNodes);
                    realtimeExecutionService.updateNodeStatus(executionId, nodeId, ExecutionStatus.RUNNING, nodeData);
                }
            }
            
            @Override
            public void onNodeComplete(String executionId, String nodeId, String action, boolean success,
                                     int completedNodes, int totalNodes, Object result) {
                // Broadcast node completion via WebSocket
                if (realtimeExecutionService != null) {
                    ExecutionStatus status = success ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED;
                    Map<String, Object> nodeData = new HashMap<>();
                    nodeData.put("action", action);
                    nodeData.put("progress", (double) completedNodes / totalNodes);
                    realtimeExecutionService.updateNodeStatus(executionId, nodeId, status, nodeData);
                    
                    // Send node output if available
                    if (result != null) {
                        realtimeExecutionService.broadcastNodeOutput(executionId, nodeId, result);
                    }
                }
            }
            
            @Override
            public void onNodeSkipped(String executionId, String nodeId, String action, String reason,
                                    int completedNodes, int totalNodes) {
                // Broadcast node skip via WebSocket
                if (realtimeExecutionService != null) {
                    Map<String, Object> nodeData = new HashMap<>();
                    nodeData.put("action", action);
                    nodeData.put("skipReason", reason);
                    nodeData.put("progress", (double) completedNodes / totalNodes);
                    realtimeExecutionService.updateNodeStatus(executionId, nodeId, ExecutionStatus.SKIPPED, nodeData);
                }
            }
            
            @Override
            public void onExecutionError(String executionId, Exception error, String nodeId) {
                // Broadcast error via WebSocket
                if (realtimeExecutionService != null) {
                    String errorMessage = error.getMessage();
                    String stackTrace = Arrays.toString(error.getStackTrace());
                    realtimeExecutionService.broadcastNodeError(executionId, nodeId, errorMessage, stackTrace);
                }
            }
        };
    }
    
    // EchoPlugin is now provided by PluginRouterFactory for consistency

    /**
     * Execute a DAG synchronously or asynchronously
     */
    public DagExecutionResponse executeDag(DagExecutionRequest request, Map<String, Object> userContext) {
        String executionId = generateExecutionId();
        
        try {
            // Validate request
            if (request.getDag() == null) {
                return DagExecutionResponse.failure(executionId, "DAG cannot be null", "VALIDATION_ERROR");
            }
            
            // Merge user context from header and request body
            Map<String, Object> mergedContext = mergeContexts(userContext, request.getContext());
            
            // Validate DAG structure
            ValidationResult validationResult = dagValidatorService.validate(request.getDag());
            DagValidationResult dagValidationResult = adaptValidationResult(validationResult);
            
            // Handle dry run mode
            if (request.isDryRun()) {
                return handleDryRun(executionId, request, dagValidationResult);
            }
            
            // Return validation errors if DAG is invalid
            if (!dagValidationResult.isValid()) {
                return DagExecutionResponse.validationFailure(executionId, dagValidationResult);
            }
            
            // Validate plugin dependencies if requested
            if (request.isValidatePlugins()) {
                DagValidationResult pluginValidation = validatePluginDependencies(request.getDag());
                if (!pluginValidation.isValid()) {
                    return DagExecutionResponse.validationFailure(executionId, pluginValidation);
                }
            }
            
            // Handle async execution via queue
            if (request.isAsync()) {
                return handleQueuedAsyncExecution(executionId, request, mergedContext);
            }
            
            // Execute synchronously
            return executeSynchronously(executionId, request, mergedContext);
            
        } catch (Exception e) {
            return DagExecutionResponse.failure(executionId, 
                "DAG execution failed: " + e.getMessage(), "EXECUTION_ERROR");
        }
    }
    
    /**
     * Get execution status for async executions
     */
    public ExecutionStatusResponse getExecutionStatus(String executionId) {
        // Check if execution is still running
        CompletableFuture<DagExecutionResponse> future = runningExecutions.get(executionId);
        if (future != null) {
            if (future.isDone()) {
                try {
                    DagExecutionResponse result = future.get();
                    runningExecutions.remove(executionId);
                    
                    if (result.isSuccess()) {
                        return ExecutionStatusResponse.completed(executionId, convertToPromptResponse(result));
                    } else {
                        return ExecutionStatusResponse.failed(executionId, convertToPromptResponse(result));
                    }
                } catch (Exception e) {
                    runningExecutions.remove(executionId);
                    DagExecutionResponse errorResult = DagExecutionResponse.failure(
                        executionId, "Async execution failed: " + e.getMessage(), "EXECUTION_ERROR");
                    return ExecutionStatusResponse.failed(executionId, convertToPromptResponse(errorResult));
                }
            } else {
                // Still running - get progress from Redis if available
                ExecutionStatusResponse.ExecutionProgress progress = getExecutionProgress(executionId);
                return ExecutionStatusResponse.running(executionId, progress);
            }
        }
        
        // Check Redis for completed executions
        Object cachedResult = redisTemplate.opsForValue().get("dag_execution:" + executionId);
        if (cachedResult instanceof DagExecutionResponse) {
            DagExecutionResponse result = (DagExecutionResponse) cachedResult;
            if (result.isSuccess()) {
                return ExecutionStatusResponse.completed(executionId, convertToPromptResponse(result));
            } else {
                return ExecutionStatusResponse.failed(executionId, convertToPromptResponse(result));
            }
        }
        
        // Execution not found
        DagExecutionResponse notFoundResult = DagExecutionResponse.failure(
            executionId, "Execution not found", "NOT_FOUND");
        return ExecutionStatusResponse.failed(executionId, convertToPromptResponse(notFoundResult));
    }
    
    /**
     * Cancel an async execution
     */
    public boolean cancelExecution(String executionId) {
        CompletableFuture<DagExecutionResponse> future = runningExecutions.get(executionId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                runningExecutions.remove(executionId);
                // Store cancellation status in Redis
                redisTemplate.opsForValue().set("dag_execution:" + executionId + ":status", "CANCELLED");
            }
            return cancelled;
        }
        return false;
    }
    
    /**
     * Validate plugin dependencies for all nodes in the DAG
     */
    public DagValidationResult validatePluginDependencies(DAG dag) {
        ValidationResult validationResult = dagValidatorService.validate(dag);
        return adaptValidationResult(validationResult);
    }
    
    private DagExecutionResponse handleDryRun(String executionId, DagExecutionRequest request, 
                                             DagValidationResult validationResult) {
        try {
            // Create DAG structure representation
            Map<String, Object> dagStructure = createDagStructureRepresentation(request.getDag());
            
            return DagExecutionResponse.dryRun(executionId, dagStructure, validationResult);
            
        } catch (Exception e) {
            return DagExecutionResponse.failure(executionId, 
                "Dry run failed: " + e.getMessage(), "DRY_RUN_ERROR");
        }
    }
    
    private DagExecutionResponse handleAsyncExecution(String executionId, DagExecutionRequest request, 
                                                     Map<String, Object> context) {
        String statusUrl = "/api/v1/executions/" + executionId + "/status";
        
        // Track execution start
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "dag");
        metadata.put("async", true);
        statusMonitoringService.trackExecution(executionId, "dag", metadata);
        
        // Start async execution
        CompletableFuture<DagExecutionResponse> future = CompletableFuture.supplyAsync(() -> {
            try {
                return executeSynchronously(executionId, request, context);
            } catch (Exception e) {
                return DagExecutionResponse.failure(executionId, 
                    "Async execution failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }, asyncExecutor);
        
        // Store future for status tracking
        runningExecutions.put(executionId, future);
        
        // Handle completion
        future.whenComplete((result, throwable) -> {
            if (throwable == null) {
                // Store result in Redis for later retrieval
                redisTemplate.opsForValue().set("dag_execution:" + executionId, result);
                
                // Update status monitoring
                statusMonitoringService.completeExecution(executionId, result.isSuccess(), result);
            } else {
                DagExecutionResponse errorResult = DagExecutionResponse.failure(
                    executionId, "Execution failed: " + throwable.getMessage(), "EXECUTION_ERROR");
                redisTemplate.opsForValue().set("dag_execution:" + executionId, errorResult);
                
                // Update status monitoring
                statusMonitoringService.completeExecution(executionId, false, errorResult);
            }
        });
        
        return DagExecutionResponse.async(executionId, statusUrl);
    }
    
    private DagExecutionResponse handleQueuedAsyncExecution(String executionId, DagExecutionRequest request, 
                                                           Map<String, Object> context) {
        String statusUrl = "/api/v1/dags/status/" + executionId;
        
        // Determine priority based on request characteristics
        ExecutionQueueService.ExecutionPriority priority = determinePriority(request);
        
        // Submit to queue
        CompletableFuture<DagExecutionResponse> future = queueService.submitDagExecution(request, context, priority);
        
        // Store future for status tracking
        runningExecutions.put(executionId, future);
        
        // Handle completion
        future.whenComplete((result, throwable) -> {
            runningExecutions.remove(executionId);
            if (throwable == null) {
                // Store result in Redis for later retrieval
                redisTemplate.opsForValue().set("dag_execution:" + executionId, result);
            } else {
                DagExecutionResponse errorResult = DagExecutionResponse.failure(
                    executionId, "Queued execution failed: " + throwable.getMessage(), "EXECUTION_ERROR");
                redisTemplate.opsForValue().set("dag_execution:" + executionId, errorResult);
            }
        });
        
        return DagExecutionResponse.async(executionId, statusUrl);
    }
    
    private ExecutionQueueService.ExecutionPriority determinePriority(DagExecutionRequest request) {
        // Determine priority based on request characteristics
        if (request.getExecutionOptions() != null) {
            String priority = request.getExecutionOptions().getPriority();
            if (priority != null) {
                try {
                    return ExecutionQueueService.ExecutionPriority.valueOf(priority.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Fall through to default logic
                }
            }
        }
        
        // Default priority logic
        if (request.isDryRun()) {
            return ExecutionQueueService.ExecutionPriority.HIGH; // Dry runs are fast
        }
        
        if (request.getDag() != null && request.getDag().getNodes().size() > 10) {
            return ExecutionQueueService.ExecutionPriority.LOW; // Large DAGs get lower priority
        }
        
        return ExecutionQueueService.ExecutionPriority.NORMAL;
    }
    
    private DagExecutionResponse executeSynchronously(String executionId, DagExecutionRequest request, 
                                                     Map<String, Object> context) {
        try {
            // Create database execution record if available
            if (executionRepository != null) {
                createExecutionRecord(executionId, request, context);
            }
            
            // Apply execution options to context
            if (request.getExecutionOptions() != null) {
                applyExecutionOptions(context, request.getExecutionOptions());
            }
            
            // Execute DAG
            long timeoutMs = request.getTimeoutMs() != null ? request.getTimeoutMs() : 0;
            
            // Convert Map<String, Object> context to ExecutionContext for interface compatibility
            ExecutionContext executionContext = new ExecutionContext(executionId, context);
            
            // Use interface-based execution - convert back to ExecutionResult for backward compatibility
            DagExecutionResult dagResult = dagExecutorService.execute(request.getDag(), executionContext);
            ExecutionResult result = convertDagExecutionResultToExecutionResult(dagResult);
            
            // Get trace from explainability service
            ExecutionTrace executionTrace = null;
            if (explainabilityService != null) {
                executionTrace = explainabilityService.getExecutionTrace(executionId);
            }
            
            // Store execution in memory with trace
            if (result instanceof DagExecutionResult) {
                memoryStore.storeExecution(executionId, request.getDag(), (DagExecutionResult) result, executionTrace);
            }
            
            // Update database execution record if available
            if (executionRepository != null) {
                updateExecutionRecord(executionId, result);
            }
            
            // Get trace for response if requested
            Object trace = null;
            if (request.isTrace()) {
                trace = executionTrace != null ? executionTrace : getExecutionTrace(executionId);
            }
            
            return DagExecutionResponse.fromExecutionResult(executionId, result, trace);
            
        } catch (Exception e) {
            // Update database with error if available
            if (executionRepository != null) {
                markExecutionFailed(executionId, e.getMessage());
            }
            
            return DagExecutionResponse.failure(executionId, 
                "Execution failed: " + e.getMessage(), "EXECUTION_ERROR");
        }
    }
    
    private Map<String, Object> mergeContexts(Map<String, Object> userContext, Map<String, Object> requestContext) {
        Map<String, Object> merged = new HashMap<>();
        
        if (userContext != null) {
            merged.putAll(userContext);
        }
        
        if (requestContext != null) {
            merged.putAll(requestContext);
        }
        
        return merged;
    }
    
    private void applyExecutionOptions(Map<String, Object> context, DagExecutionRequest.ExecutionOptions options) {
        if (options.getMaxConcurrency() != null) {
            context.put("maxConcurrency", options.getMaxConcurrency());
        }
        
        if (options.getRetryPolicy() != null) {
            context.put("retryPolicy", options.getRetryPolicy());
        }
        
        context.put("failFast", options.isFailFast());
        context.put("debugMode", options.isDebugMode());
    }
    
    private Map<String, Object> createDagStructureRepresentation(DAG dag) {
        Map<String, Object> structure = new HashMap<>();
        
        // Basic DAG info
        structure.put("totalNodes", dag.getNodes().size());
        structure.put("rootNodeId", dag.getRootNode() != null ? dag.getRootNode().getId() : null);
        
        // Node details
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (TaskNode node : dag.getNodes()) {
            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("id", node.getId());
            nodeInfo.put("action", node.getAction());
            nodeInfo.put("inputParams", node.getInputParams());
            
            // Dependency information
            List<String> dependencies = new ArrayList<>();
            for (TaskNode dep : node.getResolvedDependencies()) {
                dependencies.add(dep.getId());
            }
            nodeInfo.put("dependencies", dependencies);
            
            // Retry configuration
            Map<String, Object> retryConfig = new HashMap<>();
            retryConfig.put("maxRetries", node.getMaxRetries());
            retryConfig.put("retryDelayMs", node.getRetryDelayMs());
            retryConfig.put("backoffMultiplier", node.getBackoffMultiplier());
            retryConfig.put("fallbackPluginId", node.getFallbackPluginId());
            nodeInfo.put("retryConfig", retryConfig);
            
            nodes.add(nodeInfo);
        }
        structure.put("nodes", nodes);
        
        // Execution plan
        try {
            List<String> executionOrder = calculateExecutionOrder(dag);
            structure.put("executionOrder", executionOrder);
        } catch (Exception e) {
            structure.put("executionOrderError", e.getMessage());
        }
        
        return structure;
    }
    
    private List<String> calculateExecutionOrder(DAG dag) {
        // Simple topological sort to determine execution order
        List<String> order = new ArrayList<>();
        Set<TaskNode> visited = new HashSet<>();
        Set<TaskNode> visiting = new HashSet<>();
        
        for (TaskNode node : dag.getNodes()) {
            if (!visited.contains(node)) {
                topologicalSortUtil(node, visited, visiting, order);
            }
        }
        
        Collections.reverse(order);
        return order;
    }
    
    private void topologicalSortUtil(TaskNode node, Set<TaskNode> visited, Set<TaskNode> visiting, List<String> order) {
        if (visiting.contains(node)) {
            throw new IllegalStateException("Circular dependency detected involving node: " + node.getId());
        }
        
        if (visited.contains(node)) {
            return;
        }
        
        visiting.add(node);
        
        for (TaskNode dependency : node.getResolvedDependencies()) {
            topologicalSortUtil(dependency, visited, visiting, order);
        }
        
        visiting.remove(node);
        visited.add(node);
        order.add(node.getId());
    }
    
    private Object getExecutionTrace(String executionId) {
        // Try to get trace from TraceLogger
        // This would typically read from the logs directory
        // For now, return a placeholder
        Map<String, Object> trace = new HashMap<>();
        trace.put("executionId", executionId);
        trace.put("traceAvailable", false);
        trace.put("message", "Trace logging implementation pending");
        return trace;
    }
    
    private ExecutionStatusResponse.ExecutionProgress getExecutionProgress(String executionId) {
        // Try to get progress from Redis
        Object progressData = redisTemplate.opsForValue().get("dag_execution:" + executionId + ":progress");
        if (progressData instanceof ExecutionStatusResponse.ExecutionProgress) {
            return (ExecutionStatusResponse.ExecutionProgress) progressData;
        }
        
        // Return default progress if not available
        return new ExecutionStatusResponse.ExecutionProgress(1, 0, "unknown");
    }
    
    private api.model.PromptExecutionResponse convertToPromptResponse(DagExecutionResponse dagResponse) {
        // Convert DagExecutionResponse to PromptExecutionResponse for status API compatibility
        api.model.PromptExecutionResponse promptResponse = new api.model.PromptExecutionResponse();
        promptResponse.setExecutionId(dagResponse.getExecutionId());
        promptResponse.setSuccess(dagResponse.isSuccess());
        promptResponse.setMessage(dagResponse.getMessage());
        promptResponse.setData(dagResponse.getData());
        promptResponse.setTimestamp(dagResponse.getTimestamp());
        promptResponse.setAsync(dagResponse.isAsync());
        promptResponse.setDryRun(dagResponse.isDryRun());
        promptResponse.setNodeResults(dagResponse.getNodeResults());
        promptResponse.setErrorType(dagResponse.getErrorType());
        promptResponse.setStatusUrl(dagResponse.getStatusUrl());
        return promptResponse;
    }
    
    private String generateExecutionId() {
        return "dag_exec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    /**
     * Create database execution record
     */
    private void createExecutionRecord(String executionId, DagExecutionRequest request, Map<String, Object> context) {
        try {
            Execution execution = new Execution();
            execution.setId(executionId);
            execution.setExecutionType("DAG");
            execution.setStatus(ExecutionStatus.PENDING);
            execution.setCreatedAt(Instant.now());
            
            // Set DAG definition as JSON
            if (request.getDag() != null) {
                String dagJson = objectMapper.writeValueAsString(request.getDag());
                execution.setDagDefinition(dagJson);
            }
            
            // Set context as metadata
            if (context != null && !context.isEmpty()) {
                Map<String, String> metadata = new HashMap<>();
                for (Map.Entry<String, Object> entry : context.entrySet()) {
                    metadata.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
                execution.setMetadata(metadata);
            }
            
            // Try to get current user from context
            if (context != null && context.containsKey("userId")) {
                String userId = (String) context.get("userId");
                userRepository.findById(userId).ifPresent(execution::setUser);
            }
            
            executionRepository.save(execution);
        } catch (Exception e) {
            // Log error but don't fail execution
            System.err.println("Failed to create execution record: " + e.getMessage());
        }
    }
    
    /**
     * Update database execution record with result
     */
    private void updateExecutionRecord(String executionId, ExecutionResult result) {
        try {
            executionRepository.findById(executionId).ifPresent(execution -> {
                if (result.isSuccess()) {
                    execution.setStatus(ExecutionStatus.COMPLETED);
                    if (result.getData() != null) {
                        try {
                            String resultJson = objectMapper.writeValueAsString(result.getData());
                            execution.setExecutionResult(resultJson);
                        } catch (Exception e) {
                            execution.setExecutionResult(result.getData().toString());
                        }
                    }
                } else {
                    execution.setStatus(ExecutionStatus.FAILED);
                    execution.setErrorMessage(result.getMessage());
                }
                
                execution.setCompletedAt(Instant.now());
                if (execution.getStartedAt() != null) {
                    long duration = execution.getCompletedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli();
                    execution.setDurationMs(duration);
                }
                
                executionRepository.save(execution);
            });
        } catch (Exception e) {
            // Log error but don't fail
            System.err.println("Failed to update execution record: " + e.getMessage());
        }
    }
    
    /**
     * Mark execution as failed in database
     */
    private void markExecutionFailed(String executionId, String errorMessage) {
        try {
            executionRepository.findById(executionId).ifPresent(execution -> {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setErrorMessage(errorMessage);
                execution.setCompletedAt(Instant.now());
                
                if (execution.getStartedAt() != null) {
                    long duration = execution.getCompletedAt().toEpochMilli() - execution.getStartedAt().toEpochMilli();
                    execution.setDurationMs(duration);
                }
                
                executionRepository.save(execution);
            });
        } catch (Exception e) {
            // Log error but don't fail
            System.err.println("Failed to mark execution as failed: " + e.getMessage());
        }
    }
    
    /**
     * Adapter method to convert ValidationResult to DagValidationResult
     * This maintains backward compatibility while using the new interface-based architecture
     */
    private DagValidationResult adaptValidationResult(ValidationResult validationResult) {
        if (validationResult.isValid()) {
            return DagValidationResult.success();
        } else {
            // Convert ValidationResult errors to DagValidationResult errors
            List<ValidationError> errors = new ArrayList<>();
            validationResult.getErrors().forEach(error -> {
                errors.add(new ValidationError(ValidationErrorType.INVALID_STRUCTURE, error));
            });
            return DagValidationResult.failure(errors);
        }
    }
    
    /**
     * Converts DagExecutionResult to ExecutionResult for backward compatibility
     * This adapter method maintains compatibility with legacy code expecting ExecutionResult
     */
    private ExecutionResult convertDagExecutionResultToExecutionResult(DagExecutionResult dagResult) {
        if (dagResult.isSuccess()) {
            return ExecutionResult.success(dagResult.getMessage(), dagResult.getData());
        } else {
            return ExecutionResult.failure(
                dagResult.getMessage(), 
                dagResult.getError(), 
                dagResult.getData(),
                dagResult.getErrorType() != null ? dagResult.getErrorType() : ExecutionErrorType.EXECUTION_ERROR
            );
        }
    }
}