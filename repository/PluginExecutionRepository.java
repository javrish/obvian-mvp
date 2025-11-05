package api.repository;

import api.entity.PluginEntity;
import api.entity.PluginExecution;
import api.entity.ExecutionStatus;
import api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA repository for PluginExecution persistence operations.
 * Provides data access for plugin execution history and audit trail.
 */
@Repository
public interface PluginExecutionRepository extends BaseRepository<PluginExecution, String> {
    
    // Basic queries
    List<PluginExecution> findByPlugin(PluginEntity plugin);
    
    Page<PluginExecution> findByPlugin(PluginEntity plugin, Pageable pageable);
    
    List<PluginExecution> findByDagExecutionId(String dagExecutionId);
    
    List<PluginExecution> findByNodeId(String nodeId);
    
    // Status queries
    List<PluginExecution> findByStatus(ExecutionStatus status);
    
    Page<PluginExecution> findByStatus(ExecutionStatus status, Pageable pageable);
    
    @Query("SELECT e FROM PluginExecution e WHERE e.plugin = :plugin AND e.status = :status")
    List<PluginExecution> findByPluginAndStatus(@Param("plugin") PluginEntity plugin, 
                                               @Param("status") ExecutionStatus status);
    
    // Time-based queries
    @Query("SELECT e FROM PluginExecution e WHERE e.executedAt >= :startDate AND e.executedAt <= :endDate")
    List<PluginExecution> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT e FROM PluginExecution e WHERE e.plugin = :plugin AND e.executedAt >= :since")
    List<PluginExecution> findRecentExecutions(@Param("plugin") PluginEntity plugin, 
                                              @Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM PluginExecution e ORDER BY e.executedAt DESC")
    Page<PluginExecution> findMostRecent(Pageable pageable);
    
    // User queries
    List<PluginExecution> findByExecutedByUser(User user);
    
    Page<PluginExecution> findByExecutedByUser(User user, Pageable pageable);
    
    // Action queries
    List<PluginExecution> findByAction(String action);
    
    @Query("SELECT e FROM PluginExecution e WHERE e.plugin = :plugin AND e.action = :action")
    List<PluginExecution> findByPluginAndAction(@Param("plugin") PluginEntity plugin, 
                                               @Param("action") String action);
    
    // Error queries
    @Query("SELECT e FROM PluginExecution e WHERE e.status = 'FAILED' AND e.errorMessage IS NOT NULL")
    Page<PluginExecution> findFailedExecutions(Pageable pageable);
    
    @Query("SELECT e FROM PluginExecution e WHERE e.plugin = :plugin AND e.status = 'FAILED'")
    List<PluginExecution> findFailedExecutionsForPlugin(@Param("plugin") PluginEntity plugin);
    
    @Query("SELECT e FROM PluginExecution e WHERE e.errorMessage LIKE %:errorPattern%")
    List<PluginExecution> findByErrorPattern(@Param("errorPattern") String errorPattern);
    
    // Performance queries
    @Query("SELECT e FROM PluginExecution e WHERE e.durationMs > :threshold ORDER BY e.durationMs DESC")
    List<PluginExecution> findSlowExecutions(@Param("threshold") Long threshold);
    
    @Query("SELECT AVG(e.durationMs) FROM PluginExecution e WHERE e.plugin = :plugin AND e.status = 'COMPLETED'")
    Double getAverageExecutionTime(@Param("plugin") PluginEntity plugin);
    
    @Query("SELECT e FROM PluginExecution e WHERE e.plugin = :plugin ORDER BY e.durationMs DESC")
    Page<PluginExecution> findSlowestExecutions(@Param("plugin") PluginEntity plugin, Pageable pageable);
    
    // Retry queries
    @Query("SELECT e FROM PluginExecution e WHERE e.retryCount > 0")
    List<PluginExecution> findRetriedExecutions();
    
    @Query("SELECT e FROM PluginExecution e WHERE e.plugin = :plugin AND e.retryCount > 0")
    List<PluginExecution> findRetriedExecutionsForPlugin(@Param("plugin") PluginEntity plugin);
    
    // Fallback queries
    @Query("SELECT e FROM PluginExecution e WHERE e.isFallback = true")
    List<PluginExecution> findFallbackExecutions();
    
    @Query("SELECT COUNT(e) FROM PluginExecution e WHERE e.plugin = :plugin AND e.isFallback = true")
    long countFallbackExecutions(@Param("plugin") PluginEntity plugin);
    
    // Resource usage queries
    @Query("SELECT e FROM PluginExecution e WHERE e.memoryUsageBytes > :threshold")
    List<PluginExecution> findHighMemoryExecutions(@Param("threshold") Long threshold);
    
    @Query("SELECT e FROM PluginExecution e WHERE e.cpuTimeMs > :threshold")
    List<PluginExecution> findHighCpuExecutions(@Param("threshold") Long threshold);
    
    // Trace queries
    List<PluginExecution> findByTraceId(String traceId);
    
    List<PluginExecution> findBySpanId(String spanId);
    
    // Statistics queries
    @Query("SELECT COUNT(e) FROM PluginExecution e WHERE e.plugin = :plugin")
    long countExecutionsForPlugin(@Param("plugin") PluginEntity plugin);
    
    @Query("SELECT COUNT(e) FROM PluginExecution e WHERE e.plugin = :plugin AND e.status = :status")
    long countExecutionsByStatus(@Param("plugin") PluginEntity plugin, @Param("status") ExecutionStatus status);
    
    @Query("SELECT e.status, COUNT(e) FROM PluginExecution e WHERE e.plugin = :plugin GROUP BY e.status")
    List<Object[]> getExecutionStatusDistribution(@Param("plugin") PluginEntity plugin);
    
    @Query("SELECT DATE(e.executedAt), COUNT(e) FROM PluginExecution e WHERE e.plugin = :plugin " +
           "AND e.executedAt >= :since GROUP BY DATE(e.executedAt) ORDER BY DATE(e.executedAt)")
    List<Object[]> getExecutionCountByDay(@Param("plugin") PluginEntity plugin, @Param("since") LocalDateTime since);
    
    @Query("SELECT e.action, COUNT(e), AVG(e.durationMs) FROM PluginExecution e WHERE e.plugin = :plugin " +
           "GROUP BY e.action")
    List<Object[]> getActionStatistics(@Param("plugin") PluginEntity plugin);
    
    // Aggregate queries
    @Query("SELECT e.plugin, COUNT(e), AVG(e.durationMs), " +
           "SUM(CASE WHEN e.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN e.status = 'FAILED' THEN 1 ELSE 0 END) " +
           "FROM PluginExecution e WHERE e.executedAt >= :since GROUP BY e.plugin")
    List<Object[]> getExecutionSummaryByPlugin(@Param("since") LocalDateTime since);
    
    @Query("SELECT e.executedByUser, COUNT(e), AVG(e.durationMs) FROM PluginExecution e " +
           "WHERE e.executedAt >= :since GROUP BY e.executedByUser")
    List<Object[]> getExecutionSummaryByUser(@Param("since") LocalDateTime since);
    
    @Query("SELECT e.dagExecutionId, COUNT(e), AVG(e.durationMs) FROM PluginExecution e " +
           "WHERE e.executedAt >= :since GROUP BY e.dagExecutionId")
    List<Object[]> getExecutionSummaryByDag(@Param("since") LocalDateTime since);
    
    // Cleanup queries
    @Query("DELETE FROM PluginExecution e WHERE e.executedAt < :cutoffDate")
    int deleteOldExecutions(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("DELETE FROM PluginExecution e WHERE e.executedAt < :cutoffDate AND e.status = 'COMPLETED'")
    int deleteOldSuccessfulExecutions(@Param("cutoffDate") LocalDateTime cutoffDate);
}