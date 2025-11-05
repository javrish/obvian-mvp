package api.service.interfaces;

import api.model.DagExecutionRequest;
import api.model.DagExecutionResponse;
import api.model.ExecutionStatusResponse;
import core.DAG;
import core.DagValidationResult;

import java.util.Map;

/**
 * Interface for DAG execution service.
 * 
 * This service provides enterprise-grade DAG execution capabilities including:
 * - Synchronous and asynchronous execution modes
 * - Real-time execution tracking and monitoring
 * - Comprehensive validation and error handling
 * - Memory-aware execution with persistent context
 * - Plugin-based node execution with retry logic
 * 
 * Patent Claims Coverage:
 * - Claim 2: Multi-threaded DAG execution with dependency tracking
 * - Claim 4: Memory-aware execution context management
 * - Claim 6: Plugin-based task orchestration
 * - Claim 8: Real-time execution monitoring and status tracking
 * - Claim 12: Asynchronous execution with cancellation support
 * 
 * @author Obvian Engineering Team
 * @since 1.0.0
 */
public interface IDagExecutionService {

    /**
     * Execute a DAG synchronously or asynchronously based on request configuration.
     * 
     * This method orchestrates the complete DAG execution lifecycle including:
     * - Request validation and context merging
     * - DAG structure validation with plugin dependency checking
     * - Execution routing (sync/async/queued) based on request parameters
     * - Real-time progress tracking and explainability trace collection
     * - Result consolidation and memory storage
     * 
     * Patent Coverage: Implements core orchestration logic per Claims 2, 4, 6
     * 
     * @param request The DAG execution request containing DAG definition, execution options, and context
     * @param userContext Additional user context from authentication headers and session state
     * @return DagExecutionResponse containing execution results, traces, and status information
     * @throws IllegalArgumentException if request validation fails
     * @throws ExecutionException if DAG execution encounters unrecoverable errors
     */
    DagExecutionResponse executeDag(DagExecutionRequest request, Map<String, Object> userContext);

    /**
     * Get execution status for asynchronous executions.
     * 
     * Provides real-time status tracking for long-running DAG executions with:
     * - Progress information including completed/total nodes
     * - Intermediate results and node outputs
     * - Error details and recovery suggestions
     * - Execution trace data for explainability
     * 
     * Patent Coverage: Real-time monitoring per Claim 8
     * 
     * @param executionId Unique identifier for the execution instance
     * @return ExecutionStatusResponse with current status, progress, and intermediate results
     * @throws ExecutionNotFoundException if execution ID is not found
     */
    ExecutionStatusResponse getExecutionStatus(String executionId);

    /**
     * Cancel an active asynchronous execution.
     * 
     * Provides graceful cancellation with:
     * - Immediate termination of running nodes
     * - Resource cleanup and connection management
     * - Partial result preservation for analysis
     * - Status update notification to monitoring systems
     * 
     * Patent Coverage: Cancellation control per Claim 12
     * 
     * @param executionId Unique identifier for the execution to cancel
     * @return true if cancellation was successful, false if execution was not found or already completed
     * @throws SecurityException if user lacks permission to cancel the execution
     */
    boolean cancelExecution(String executionId);

    /**
     * Validate plugin dependencies for all nodes in a DAG.
     * 
     * Performs comprehensive validation including:
     * - Plugin availability and version compatibility
     * - Input/output schema validation
     * - Security permission checks
     * - Resource requirement analysis
     * 
     * Patent Coverage: Plugin validation per Claim 6
     * 
     * @param dag The DAG structure to validate
     * @return DagValidationResult containing validation status, errors, and warnings
     * @throws ValidationException if critical validation issues prevent execution
     */
    DagValidationResult validatePluginDependencies(DAG dag);
}