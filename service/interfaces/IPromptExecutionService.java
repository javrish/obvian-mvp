package api.service.interfaces;

import api.model.ExecutionStatusResponse;
import api.model.PromptExecutionRequest;
import api.model.PromptExecutionResponse;

import java.util.Map;

/**
 * Interface for natural language prompt execution service.
 * 
 * This service transforms natural language instructions into executable DAGs and manages
 * their execution lifecycle with comprehensive context awareness and persona management:
 * - Natural language parsing and intent extraction
 * - Dynamic DAG generation from parsed intents
 * - Persona-aware execution context management
 * - Memory integration for contextual understanding
 * - Real-time execution monitoring and result consolidation
 * 
 * Patent Claims Coverage:
 * - Claim 1: Natural language to DAG transformation
 * - Claim 3: Context-aware prompt parsing with memory integration
 * - Claim 5: Persona-driven execution customization
 * - Claim 7: Multi-step prompt consolidation and result aggregation
 * - Claim 9: Real-time prompt execution monitoring
 * - Claim 11: Memory-aware contextual prompt resolution
 * 
 * @author Obvian Engineering Team
 * @since 1.0.0
 */
public interface IPromptExecutionService {

    /**
     * Execute a natural language prompt through DAG generation and execution.
     * 
     * This method provides the core natural language processing capability:
     * - Prompt parsing and compound intent extraction
     * - Dynamic persona analysis and context switching
     * - DAG construction from parsed intents with dependency resolution
     * - Context-aware execution with memory integration
     * - Result consolidation and frontend-compatible response generation
     * 
     * Patent Coverage: Core NL-to-DAG transformation per Claims 1, 3, 5, 7
     * 
     * @param request The prompt execution request containing natural language instruction and execution options
     * @param userContext User authentication context and session state from headers
     * @return PromptExecutionResponse containing execution results, generated DAG, and consolidated outputs
     * @throws IllegalArgumentException if prompt is empty or invalid
     * @throws ParsingException if natural language parsing fails
     * @throws ExecutionException if DAG generation or execution encounters errors
     */
    PromptExecutionResponse executePrompt(PromptExecutionRequest request, Map<String, Object> userContext);

    /**
     * Get execution status for asynchronous prompt executions.
     * 
     * Provides real-time tracking of prompt-based executions with:
     * - Generated DAG structure and execution progress
     * - Intermediate node results and consolidated outputs
     * - Persona context and memory integration status
     * - Error diagnostics and recovery recommendations
     * 
     * Patent Coverage: Real-time monitoring per Claim 9
     * 
     * @param executionId Unique identifier for the prompt execution instance
     * @return ExecutionStatusResponse with current status, progress, and intermediate results
     * @throws ExecutionNotFoundException if execution ID is not found
     */
    ExecutionStatusResponse getExecutionStatus(String executionId);

    /**
     * Cancel an active asynchronous prompt execution.
     * 
     * Provides graceful cancellation with:
     * - Termination of ongoing DAG execution
     * - Preservation of partial results and generated DAG structure
     * - Memory context cleanup and state consistency
     * - Persona state restoration to pre-execution state
     * 
     * Patent Coverage: Execution control per Claim 12
     * 
     * @param executionId Unique identifier for the execution to cancel
     * @return true if cancellation was successful, false if execution was not found or already completed
     * @throws SecurityException if user lacks permission to cancel the execution
     */
    boolean cancelExecution(String executionId);
}