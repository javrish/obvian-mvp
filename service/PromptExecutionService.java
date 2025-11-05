package api.service;

import api.model.ExecutionStatusResponse;
import api.model.PromptExecutionRequest;
import api.model.PromptExecutionResponse;
import core.*;
import core.explainability.ExecutionTrace;
import memory.MemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import plugins.PluginRouter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for executing natural language prompts through DAG generation and execution
 */
@Service
public class PromptExecutionService {
    
    private final PromptParser promptParser;
    private final DagBuilder dagBuilder;
    private final DagExecutor dagExecutor;
    private final MemoryStore memoryStore;
    private final ResultConsolidator resultConsolidator;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StatusMonitoringService statusMonitoringService;
    private final StatusMonitoringProgressCallback progressCallback;
    private final ExecutorService asyncExecutor;
    private final Map<String, CompletableFuture<PromptExecutionResponse>> runningExecutions;
    private final PersonaService personaService;
    
    @Autowired(required = false)
    private ExplainabilityService explainabilityService;
    
    @Autowired
    public PromptExecutionService(MemoryStore memoryStore, RedisTemplate<String, Object> redisTemplate,
                                 StatusMonitoringService statusMonitoringService, PluginRouter pluginRouter,
                                 PersonaService personaService) {
        this.memoryStore = memoryStore;
        this.redisTemplate = redisTemplate;
        this.statusMonitoringService = statusMonitoringService;
        this.personaService = personaService;
        this.promptParser = new PromptParser(memoryStore);
        this.dagBuilder = new DagBuilder(memoryStore);
        
        // Initialize progress callback for real-time updates
        this.progressCallback = new StatusMonitoringProgressCallback(statusMonitoringService);
        
        // Validate that all required plugins are available
        if (pluginRouter != null) {
            plugins.PluginRouterFactory.validateExistingRouter(pluginRouter);
        }
        
        // Create DagExecutor with plugin router that has registered plugins
        this.dagExecutor = new DagExecutor(pluginRouter, new TraceLogger(), new NoOpMetricsCollector(), 
                                          null, null, memoryStore, progressCallback);
        this.resultConsolidator = new ResultConsolidator(memoryStore);
        this.asyncExecutor = Executors.newCachedThreadPool();
        this.runningExecutions = new ConcurrentHashMap<>();
    }
    
    /**
     * FORCE plugin registration to fix "Plugin not found" issue.
     * This directly registers all plugins regardless of Spring configuration.
     */
    private void forceRegisterPlugins(PluginRouter pluginRouter) {
        // Check if pluginRouter is null (can happen in test scenarios)
        if (pluginRouter == null) {
            System.out.println("Warning: PluginRouter is null, skipping plugin registration");
            return;
        }
        
        // Always register plugins to ensure they're available
        try {
            pluginRouter.registerPlugin("EchoPlugin", new EchoPlugin());
            pluginRouter.registerPlugin("EmailPlugin", new plugins.email.EmailPlugin());
            pluginRouter.registerPlugin("FilePlugin", new plugins.file.FilePlugin());
            pluginRouter.registerPlugin("ReminderPlugin", new plugins.reminder.ReminderPlugin());
            pluginRouter.registerPlugin("SlackPlugin", new plugins.slack.SlackPlugin());
        } catch (Exception e) {
            System.err.println("Failed to register plugins: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Simple echo plugin for generic actions
     */
    private static class EchoPlugin implements plugins.Plugin {
        @Override
        public core.ExecutionResult execute(java.util.Map<String, Object> inputParams) {
            String msg = String.valueOf(inputParams.getOrDefault("message", ""));
            return core.ExecutionResult.success("Echo: " + msg, msg);
        }
        
        @Override
        public String getName() { 
            return "EchoPlugin"; 
        }
        
        @Override
        public String getDescription() { 
            return "Echoes back the input message for generic actions and testing."; 
        }
    }

    /**
     * Execute a prompt synchronously or asynchronously
     */
    public PromptExecutionResponse executePrompt(PromptExecutionRequest request, Map<String, Object> userContext) {
        String executionId = generateExecutionId();
        
        try {
            // Validate request
            if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
                return PromptExecutionResponse.failure(executionId, "Prompt cannot be empty", "VALIDATION_ERROR");
            }
            
            // Extract userId from request or userContext
            String userId = extractUserId(request, userContext);
            
            // Trigger persona analysis if userId is available
            if (userId != null && personaService != null) {
                try {
                    // Create UserContext for persona analysis
                    UserContext userContextForPersona = createUserContextForPersonaAnalysis(userContext, request);
                    
                    // Trigger persona analysis and potential switching
                    personaService.analyzeAndSwitchPersona(userId, userContextForPersona);
                    
                    // Add current persona information to execution context
                    PersonaManager.PersonaType currentPersona = personaService.getCurrentPersona(userId);
                    if (userContext == null) userContext = new HashMap<>();
                    userContext.put("userId", userId);
                    userContext.put("currentPersona", currentPersona.getValue());
                } catch (Exception e) {
                    // Log error but don't fail execution due to persona analysis issues
                    System.err.println("Persona analysis failed for user " + userId + ": " + e.getMessage());
                }
            }
            
            // Merge user context from header and request body
            Map<String, Object> mergedContext = mergeContexts(userContext, request.getContext());
            
            // Handle dry run mode
            if (request.isDryRun()) {
                return handleDryRun(executionId, request.getPrompt(), mergedContext);
            }
            
            // Handle async execution
            if (request.isAsync()) {
                return handleAsyncExecution(executionId, request, mergedContext);
            }
            
            // Execute synchronously
            return executeSynchronously(executionId, request, mergedContext);
            
        } catch (Exception e) {
            return PromptExecutionResponse.failure(executionId, 
                "Execution failed: " + e.getMessage(), "EXECUTION_ERROR");
        }
    }
    
    /**
     * Get execution status for async executions
     */
    public ExecutionStatusResponse getExecutionStatus(String executionId) {
        // Check if execution is still running
        CompletableFuture<PromptExecutionResponse> future = runningExecutions.get(executionId);
        if (future != null) {
            if (future.isDone()) {
                try {
                    PromptExecutionResponse result = future.get();
                    runningExecutions.remove(executionId);
                    
                    if (result.isSuccess()) {
                        return ExecutionStatusResponse.completed(executionId, result);
                    } else {
                        return ExecutionStatusResponse.failed(executionId, result);
                    }
                } catch (Exception e) {
                    runningExecutions.remove(executionId);
                    PromptExecutionResponse errorResult = PromptExecutionResponse.failure(
                        executionId, "Async execution failed: " + e.getMessage(), "EXECUTION_ERROR");
                    return ExecutionStatusResponse.failed(executionId, errorResult);
                }
            } else {
                // Still running - get progress from Redis if available
                ExecutionStatusResponse.ExecutionProgress progress = getExecutionProgress(executionId);
                return ExecutionStatusResponse.running(executionId, progress);
            }
        }
        
        // Check Redis for completed executions
        Object cachedResult = redisTemplate.opsForValue().get("execution:" + executionId);
        if (cachedResult instanceof PromptExecutionResponse) {
            PromptExecutionResponse result = (PromptExecutionResponse) cachedResult;
            if (result.isSuccess()) {
                return ExecutionStatusResponse.completed(executionId, result);
            } else {
                return ExecutionStatusResponse.failed(executionId, result);
            }
        }
        
        // Execution not found
        PromptExecutionResponse notFoundResult = PromptExecutionResponse.failure(
            executionId, "Execution not found", "NOT_FOUND");
        return ExecutionStatusResponse.failed(executionId, notFoundResult);
    }
    
    /**
     * Cancel an async execution
     */
    public boolean cancelExecution(String executionId) {
        CompletableFuture<PromptExecutionResponse> future = runningExecutions.get(executionId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                runningExecutions.remove(executionId);
                // Store cancellation status in Redis
                redisTemplate.opsForValue().set("execution:" + executionId + ":status", "CANCELLED");
            }
            return cancelled;
        }
        return false;
    }
    
    private PromptExecutionResponse handleDryRun(String executionId, String prompt, Map<String, Object> context) {
        try {
            // Parse the prompt to understand structure
            PromptParser.CompoundParseResult parseResult = promptParser.parseCompoundPrompt(prompt);
            
            // Generate DAG structure preview
            Map<String, Object> dagStructure = dagBuilder.previewDagStructure(prompt);
            
            // Create execution plan
            List<Map<String, Object>> executionPlan = createExecutionPlan(parseResult);
            
            return PromptExecutionResponse.dryRun(executionId, dagStructure, executionPlan);
            
        } catch (Exception e) {
            return PromptExecutionResponse.failure(executionId, 
                "Dry run failed: " + e.getMessage(), "DRY_RUN_ERROR");
        }
    }
    
    private PromptExecutionResponse handleAsyncExecution(String executionId, PromptExecutionRequest request, 
                                                        Map<String, Object> context) {
        String statusUrl = "/api/v1/executions/" + executionId + "/status";
        
        // Track execution start
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "prompt");
        metadata.put("async", true);
        metadata.put("prompt", request.getPrompt());
        statusMonitoringService.trackExecution(executionId, "prompt", metadata);
        
        // Start async execution
        CompletableFuture<PromptExecutionResponse> future = CompletableFuture.supplyAsync(() -> {
            try {
                return executeSynchronously(executionId, request, context);
            } catch (Exception e) {
                return PromptExecutionResponse.failure(executionId, 
                    "Async execution failed: " + e.getMessage(), "EXECUTION_ERROR");
            }
        }, asyncExecutor);
        
        // Store future for status tracking
        runningExecutions.put(executionId, future);
        
        // Handle completion
        future.whenComplete((result, throwable) -> {
            if (throwable == null) {
                // Store result in Redis for later retrieval
                redisTemplate.opsForValue().set("prompt_execution:" + executionId, result);
                
                // Update status monitoring
                statusMonitoringService.completeExecution(executionId, result.isSuccess(), result);
            } else {
                PromptExecutionResponse errorResult = PromptExecutionResponse.failure(
                    executionId, "Execution failed: " + throwable.getMessage(), "EXECUTION_ERROR");
                redisTemplate.opsForValue().set("prompt_execution:" + executionId, errorResult);
                
                // Update status monitoring
                statusMonitoringService.completeExecution(executionId, false, errorResult);
            }
        });
        
        return PromptExecutionResponse.async(executionId, statusUrl);
    }
    
    private PromptExecutionResponse executeSynchronously(String executionId, PromptExecutionRequest request, 
                                                        Map<String, Object> context) {
        try {
            // Parse the prompt
            PromptParser.CompoundParseResult parseResult = promptParser.parseCompoundPrompt(request.getPrompt());
            
            // Build DAG from parsed intents
            DAG dag;
            if (parseResult.isCompound()) {
                dag = dagBuilder.buildFromCompoundPrompt(parseResult);
            } else {
                dag = dagBuilder.buildFromParsedIntent(parseResult.getPrimaryIntent());
            }
            
            // Execute DAG
            long timeoutMs = request.getTimeoutMs() != null ? request.getTimeoutMs() : 0;
            ExecutionResult result = dagExecutor.execute(dag, context, timeoutMs);
            
            // Get trace from explainability service if available
            ExecutionTrace executionTrace = null;
            if (explainabilityService != null) {
                executionTrace = explainabilityService.getExecutionTrace(executionId);
            }
            
            // Store execution in memory and consolidate results
            if (result instanceof DagExecutionResult) {
                DagExecutionResult dagResult = (DagExecutionResult) result;
                memoryStore.storeExecution(executionId, dag, dagResult, executionTrace);
                
                // Consolidate terminal node results
                String userContext = extractUserContext(context);
                ResultConsolidator.ConsolidatedResult consolidatedResult = 
                    resultConsolidator.consolidateResults(dagResult, dag, userContext);
                
                // Create response with consolidated results
                PromptExecutionResponse response = PromptExecutionResponse.fromExecutionResult(executionId, result);
                response.setConsolidatedResult(consolidatedResult);
                
                // Set the DAG structure for frontend compatibility
                Map<String, Object> dagMap = createFrontendDAG(dag, "completed", executionId);
                response.setDag(dagMap);
                
                return response;
            }
            
            // Create response and set DAG structure
            PromptExecutionResponse response = PromptExecutionResponse.fromExecutionResult(executionId, result);
            Map<String, Object> dagMap = createFrontendDAG(dag, result.isSuccess() ? "completed" : "failed", executionId);
            response.setDag(dagMap);
            
            return response;
            
        } catch (Exception e) {
            return PromptExecutionResponse.failure(executionId, 
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
    
    private List<Map<String, Object>> createExecutionPlan(PromptParser.CompoundParseResult parseResult) {
        List<Map<String, Object>> plan = new ArrayList<>();
        
        for (int i = 0; i < parseResult.getIntents().size(); i++) {
            PromptParser.ParsedIntent intent = parseResult.getIntents().get(i);
            Map<String, Object> step = new HashMap<>();
            step.put("stepNumber", i + 1);
            step.put("taskId", "task_" + i);
            step.put("action", intent.getAction());
            step.put("parameters", intent.getParameters());
            step.put("plugin", getPluginForAction(intent.getAction()));
            step.put("estimatedDuration", "1-5 seconds");
            plan.add(step);
        }
        
        return plan;
    }
    
    private String getPluginForAction(String action) {
        Map<String, String> actionToPlugin = Map.of(
            "send_email", "EmailPlugin",
            "create_file", "FilePlugin",
            "set_reminder", "ReminderPlugin",
            "generic", "EchoPlugin"
        );
        return actionToPlugin.getOrDefault(action, "EchoPlugin");
    }
    
    private ExecutionStatusResponse.ExecutionProgress getExecutionProgress(String executionId) {
        // Try to get progress from Redis
        Object progressData = redisTemplate.opsForValue().get("execution:" + executionId + ":progress");
        if (progressData instanceof ExecutionStatusResponse.ExecutionProgress) {
            return (ExecutionStatusResponse.ExecutionProgress) progressData;
        }
        
        // Return default progress if not available
        return new ExecutionStatusResponse.ExecutionProgress(1, 0, "unknown");
    }
    
    private String generateExecutionId() {
        return "exec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
    
    /**
     * Extract user context string for memory key generation
     */
    private String extractUserContext(Map<String, Object> context) {
        if (context == null) return null;
        
        // Try common user identifier keys
        Object userId = context.get("userId");
        if (userId != null) return userId.toString();
        
        Object username = context.get("username");
        if (username != null) return username.toString();
        
        Object user = context.get("user");
        if (user != null) return user.toString();
        
        return null;
    }
    
    /**
     * Create a complete DAG structure for the frontend
     */
    private Map<String, Object> createFrontendDAG(DAG dag, String status, String executionId) {
        Map<String, Object> dagMap = new HashMap<>();
        
        // Core DAG fields - use executionId for frontend compatibility
        dagMap.put("id", executionId);
        dagMap.put("name", "Generated Workflow");
        dagMap.put("description", "Auto-generated workflow from natural language prompt");
        dagMap.put("nodes", convertNodesToMap(dag.getNodes()));
        dagMap.put("edges", generateEdgesFromDependencies(dag.getNodes()));
        dagMap.put("status", status);
        dagMap.put("createdAt", java.time.Instant.now().toString());
        
        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("executionId", executionId);
        metadata.put("originalDagId", dag.getId());
        metadata.put("nodeCount", dag.getNodes().size());
        metadata.put("generatedBy", "Obvian Engine");
        dagMap.put("metadata", metadata);
        
        return dagMap;
    }
    
    /**
     * Generate edges array from node dependencies for frontend visualization
     */
    private List<Map<String, Object>> generateEdgesFromDependencies(List<TaskNode> nodes) {
        List<Map<String, Object>> edges = new ArrayList<>();
        
        for (TaskNode node : nodes) {
            for (String depId : node.getDependencyIds()) {
                Map<String, Object> edge = new HashMap<>();
                edge.put("id", depId + "->" + node.getId());
                edge.put("source", depId);
                edge.put("target", node.getId());
                edge.put("type", "default");
                edges.add(edge);
            }
        }
        
        return edges;
    }
    
    /**
     * Convert DAG nodes to a format expected by the frontend
     * Transforms backend TaskNode structure to match frontend TypeScript interface
     */
    private List<Map<String, Object>> convertNodesToMap(List<TaskNode> nodes) {
        List<Map<String, Object>> nodeList = new ArrayList<>();
        
        for (TaskNode node : nodes) {
            Map<String, Object> nodeMap = new HashMap<>();
            
            // Core fields expected by frontend
            nodeMap.put("id", node.getId());
            nodeMap.put("type", node.getAction()); // Map 'action' to 'type'
            nodeMap.put("name", generateNodeName(node)); // Generate readable name
            nodeMap.put("description", generateNodeDescription(node));
            nodeMap.put("status", "pending"); // Default status for new nodes
            nodeMap.put("plugin", node.getAction()); // Plugin name from action
            nodeMap.put("config", node.getInputParams()); // Map inputParams to config
            nodeMap.put("dependencies", node.getDependencyIds()); // Map dependencyIds to dependencies
            nodeMap.put("retryCount", 0);
            nodeMap.put("maxRetries", node.getMaxRetries());
            
            // Optional backend-specific fields
            nodeMap.put("retryDelayMs", node.getRetryDelayMs());
            nodeMap.put("backoffMultiplier", node.getBackoffMultiplier());
            nodeMap.put("fallbackPluginId", node.getFallbackPluginId());
            
            // Metadata container for additional info
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("originalAction", node.getAction());
            metadata.put("inputParams", node.getInputParams());
            nodeMap.put("metadata", metadata);
            
            nodeList.add(nodeMap);
        }
        
        return nodeList;
    }
    
    /**
     * Generate a human-readable name for a node based on its action and parameters
     */
    private String generateNodeName(TaskNode node) {
        String action = node.getAction();
        Map<String, Object> params = node.getInputParams();
        
        switch (action) {
            case "FilePlugin":
                String filename = (String) params.get("filename");
                return filename != null ? "Create " + filename : "File Operation";
            case "EmailPlugin":
                String recipient = (String) params.get("recipient");
                return recipient != null ? "Email to " + recipient : "Send Email";
            case "EchoPlugin":
                return "Echo Task";
            case "SlackPlugin":
                return "Slack Message";
            case "ReminderPlugin":
                return "Set Reminder";
            default:
                return action.replace("Plugin", " Task");
        }
    }
    
    /**
     * Generate a description for a node based on its action and parameters
     */
    private String generateNodeDescription(TaskNode node) {
        String action = node.getAction();
        Map<String, Object> params = node.getInputParams();
        
        switch (action) {
            case "FilePlugin":
                String filename = (String) params.get("filename");
                String content = (String) params.get("content");
                return "Create file '" + filename + "'" + 
                       (content != null && !content.isEmpty() ? " with content" : " (empty)");
            case "EmailPlugin":
                String recipient = (String) params.get("recipient");
                String subject = (String) params.get("subject");
                return "Send email to " + recipient + 
                       (subject != null ? " - " + subject : "");
            case "EchoPlugin":
                String message = (String) params.get("message");
                return "Echo: " + (message != null ? message : "No message");
            default:
                return "Execute " + action.replace("Plugin", "").toLowerCase() + " operation";
        }
    }
    
    /**
     * Extract userId from request or userContext
     * 
     * @param request The prompt execution request
     * @param userContext The user context from headers
     * @return userId or null if not found
     */
    private String extractUserId(PromptExecutionRequest request, Map<String, Object> userContext) {
        // First check the request body for userId
        if (request.getUserId() != null && !request.getUserId().trim().isEmpty()) {
            return request.getUserId().trim();
        }
        
        // Then check userContext from headers
        if (userContext != null) {
            Object userIdObj = userContext.get("userId");
            if (userIdObj instanceof String && !((String) userIdObj).trim().isEmpty()) {
                return ((String) userIdObj).trim();
            }
            
            // Also check common variations
            userIdObj = userContext.get("user_id");
            if (userIdObj instanceof String && !((String) userIdObj).trim().isEmpty()) {
                return ((String) userIdObj).trim();
            }
            
            userIdObj = userContext.get("id");
            if (userIdObj instanceof String && !((String) userIdObj).trim().isEmpty()) {
                return ((String) userIdObj).trim();
            }
        }
        
        return null; // No userId found
    }
    
    /**
     * Create UserContext for persona analysis from available data
     * 
     * @param userContext The user context from headers
     * @param request The prompt execution request
     * @return UserContext for persona analysis
     */
    private UserContext createUserContextForPersonaAnalysis(Map<String, Object> userContext, PromptExecutionRequest request) {
        String userId = extractUserId(request, userContext);
        if (userId == null) {
            userId = "anonymous_" + System.currentTimeMillis();
        }
        
        // Create maps for recent activities and suggestions
        Map<String, Instant> recentActivities = new HashMap<>();
        Map<String, Instant> recentSuggestions = new HashMap<>();
        
        // Add prompt execution as a recent activity
        Instant currentTime = Instant.now();
        recentActivities.put("prompt_execution", currentTime);
        
        // Extract additional context if available
        if (userContext != null) {
            // Look for activity data in user context
            Object activitiesObj = userContext.get("recentActivities");
            if (activitiesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> activities = (Map<String, Object>) activitiesObj;
                for (Map.Entry<String, Object> entry : activities.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        long timestamp = ((Number) entry.getValue()).longValue();
                        recentActivities.put(entry.getKey(), Instant.ofEpochMilli(timestamp));
                    }
                }
            }
            
            // Look for suggestion data
            Object suggestionsObj = userContext.get("recentSuggestions");
            if (suggestionsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> suggestions = (Map<String, Object>) suggestionsObj;
                for (Map.Entry<String, Object> entry : suggestions.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        long timestamp = ((Number) entry.getValue()).longValue();
                        recentSuggestions.put(entry.getKey(), Instant.ofEpochMilli(timestamp));
                    }
                }
            }
        }
        
        // Determine last activity type based on request
        String lastActivityType = "prompt_execution";
        if (request.isDryRun()) {
            lastActivityType = "dry_run_prompt";
        } else if (request.isAsync()) {
            lastActivityType = "async_prompt";
        }
        
        return new UserContext(userId, recentActivities, recentSuggestions, lastActivityType, currentTime);
    }
}