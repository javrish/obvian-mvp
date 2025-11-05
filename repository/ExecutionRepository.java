package api.repository;

import api.entity.Execution;
import api.entity.ExecutionStatus;
import api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Execution entity.
 */
@Repository
public interface ExecutionRepository extends BaseRepository<Execution, String> {
    
    /**
     * Find executions by user.
     */
    Page<Execution> findByUser(User user, Pageable pageable);
    
    /**
     * Find executions by status.
     */
    List<Execution> findByStatus(ExecutionStatus status);
    
    /**
     * Find executions by user and status.
     */
    List<Execution> findByUserAndStatus(User user, ExecutionStatus status);
    
    /**
     * Find executions by type.
     */
    List<Execution> findByExecutionType(String executionType);
    
    /**
     * Find executions created after a specific date.
     */
    List<Execution> findByCreatedAtAfter(Instant date);
    
    /**
     * Find executions created between dates.
     */
    List<Execution> findByCreatedAtBetween(Instant startDate, Instant endDate);
    
    /**
     * Find child executions.
     */
    List<Execution> findByParentExecutionId(String parentExecutionId);
    
    /**
     * Find running executions that are stuck.
     */
    @Query("SELECT e FROM Execution e WHERE e.status = 'RUNNING' AND e.startedAt < :threshold")
    List<Execution> findStuckExecutions(@Param("threshold") Instant threshold);
    
    /**
     * Count executions by status for a user.
     */
    @Query("SELECT e.status, COUNT(e) FROM Execution e WHERE e.user = :user GROUP BY e.status")
    List<Object[]> countExecutionsByStatusForUser(@Param("user") User user);
    
    /**
     * Find recent executions for a user.
     */
    @Query("SELECT e FROM Execution e WHERE e.user = :user ORDER BY e.createdAt DESC")
    Page<Execution> findRecentExecutionsByUser(@Param("user") User user, Pageable pageable);
    
    /**
     * Calculate average execution time.
     */
    @Query("SELECT AVG(e.durationMs) FROM Execution e WHERE e.status = 'COMPLETED' AND e.executionType = :type")
    Double calculateAverageExecutionTime(@Param("type") String executionType);
    
    /**
     * Find failed executions for retry.
     */
    @Query("SELECT e FROM Execution e WHERE e.status = 'FAILED' AND e.retryCount < :maxRetries")
    List<Execution> findFailedExecutionsForRetry(@Param("maxRetries") int maxRetries);
    
    /**
     * Get execution statistics for a time period.
     */
    @Query("SELECT " +
           "COUNT(e), " +
           "COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END), " +
           "COUNT(CASE WHEN e.status = 'FAILED' THEN 1 END), " +
           "AVG(e.durationMs) " +
           "FROM Execution e " +
           "WHERE e.createdAt BETWEEN :startDate AND :endDate")
    Object[] getExecutionStatistics(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
    
    /**
     * Find latest execution by user and type.
     */
    Optional<Execution> findFirstByUserAndExecutionTypeOrderByCreatedAtDesc(User user, String executionType);
    
    /**
     * Delete old executions.
     */
    void deleteByCreatedAtBefore(Instant date);

    /**
     * Check if a user is the owner of an execution.
     */
    @Query("SELECT COUNT(e) > 0 FROM Execution e WHERE e.id = :executionId AND e.user.id = :userId")
    boolean isExecutionOwner(@Param("executionId") String executionId, @Param("userId") String userId);
}