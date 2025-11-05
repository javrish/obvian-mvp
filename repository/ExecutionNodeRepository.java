package api.repository;

import api.entity.Execution;
import api.entity.ExecutionNode;
import api.entity.ExecutionStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ExecutionNode entity.
 */
@Repository
public interface ExecutionNodeRepository extends BaseRepository<ExecutionNode, Long> {
    
    /**
     * Find nodes by execution.
     */
    List<ExecutionNode> findByExecution(Execution execution);
    
    /**
     * Find nodes by execution ordered by start time.
     */
    List<ExecutionNode> findByExecutionOrderByStartedAt(Execution execution);
    
    /**
     * Find node by execution and node ID.
     */
    Optional<ExecutionNode> findByExecutionAndNodeId(Execution execution, String nodeId);
    
    /**
     * Find nodes by status.
     */
    List<ExecutionNode> findByStatus(ExecutionStatus status);
    
    /**
     * Find nodes by plugin name.
     */
    List<ExecutionNode> findByPluginName(String pluginName);
    
    /**
     * Find failed nodes for an execution.
     */
    List<ExecutionNode> findByExecutionAndStatus(Execution execution, ExecutionStatus status);
    
    /**
     * Count nodes by status for an execution.
     */
    @Query("SELECT n.status, COUNT(n) FROM ExecutionNode n WHERE n.execution = :execution GROUP BY n.status")
    List<Object[]> countNodesByStatusForExecution(@Param("execution") Execution execution);
    
    /**
     * Find nodes that can be retried.
     */
    @Query("SELECT n FROM ExecutionNode n WHERE n.status = 'FAILED' AND n.retryCount < n.maxRetries")
    List<ExecutionNode> findRetriableNodes();
    
    /**
     * Calculate average duration by plugin.
     */
    @Query("SELECT n.pluginName, AVG(n.durationMs) FROM ExecutionNode n " +
           "WHERE n.status = 'COMPLETED' GROUP BY n.pluginName")
    List<Object[]> calculateAverageDurationByPlugin();
    
    /**
     * Find slowest nodes.
     */
    @Query("SELECT n FROM ExecutionNode n WHERE n.durationMs IS NOT NULL ORDER BY n.durationMs DESC")
    List<ExecutionNode> findSlowestNodes();
    
    /**
     * Count successful executions by plugin.
     */
    @Query("SELECT n.pluginName, COUNT(n) FROM ExecutionNode n " +
           "WHERE n.status = 'COMPLETED' GROUP BY n.pluginName")
    List<Object[]> countSuccessfulExecutionsByPlugin();
    
    /**
     * Find nodes with errors.
     */
    @Query("SELECT n FROM ExecutionNode n WHERE n.errorMessage IS NOT NULL")
    List<ExecutionNode> findNodesWithErrors();
    
    /**
     * Delete nodes by execution.
     */
    void deleteByExecution(Execution execution);
}