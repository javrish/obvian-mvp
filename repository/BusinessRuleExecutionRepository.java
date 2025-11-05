package api.repository;

import api.entity.BusinessRuleEntity;
import api.entity.BusinessRuleExecution;
import api.entity.BusinessRuleExecution.TriggerType;
import api.entity.ExecutionStatus;
import api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for BusinessRuleExecution persistence operations.
 * Provides data access for business rule execution history and audit trail.
 */
@Repository
public interface BusinessRuleExecutionRepository extends BaseRepository<BusinessRuleExecution, String> {
    
    // Basic queries
    Optional<BusinessRuleExecution> findByExecutionId(String executionId);
    
    List<BusinessRuleExecution> findByBusinessRule(BusinessRuleEntity businessRule);
    
    Page<BusinessRuleExecution> findByBusinessRule(BusinessRuleEntity businessRule, Pageable pageable);
    
    // Status queries
    List<BusinessRuleExecution> findByStatus(ExecutionStatus status);
    
    Page<BusinessRuleExecution> findByStatus(ExecutionStatus status, Pageable pageable);
    
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.businessRule = :rule AND e.status = :status")
    List<BusinessRuleExecution> findByBusinessRuleAndStatus(@Param("rule") BusinessRuleEntity rule, 
                                                           @Param("status") ExecutionStatus status);
    
    // User queries
    List<BusinessRuleExecution> findByExecutedByUser(User user);
    
    Page<BusinessRuleExecution> findByExecutedByUser(User user, Pageable pageable);
    
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.executedByUser = :user " +
           "ORDER BY e.executedAt DESC")
    Page<BusinessRuleExecution> findRecentByUser(@Param("user") User user, Pageable pageable);
    
    // Time-based queries
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.executedAt >= :startDate " +
           "AND e.executedAt <= :endDate ORDER BY e.executedAt DESC")
    List<BusinessRuleExecution> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.businessRule = :rule " +
           "AND e.executedAt >= :since ORDER BY e.executedAt DESC")
    List<BusinessRuleExecution> findRecentExecutions(@Param("rule") BusinessRuleEntity rule, 
                                                    @Param("since") LocalDateTime since);
    
    @Query("SELECT e FROM BusinessRuleExecution e ORDER BY e.executedAt DESC")
    Page<BusinessRuleExecution> findMostRecent(Pageable pageable);
    
    // Trigger type queries
    List<BusinessRuleExecution> findByTriggerType(TriggerType triggerType);
    
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.businessRule = :rule " +
           "AND e.triggerType = :triggerType")
    List<BusinessRuleExecution> findByBusinessRuleAndTriggerType(@Param("rule") BusinessRuleEntity rule,
                                                                @Param("triggerType") TriggerType triggerType);
    
    // Error queries
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.status = 'FAILED' " +
           "AND e.errorMessage IS NOT NULL ORDER BY e.executedAt DESC")
    Page<BusinessRuleExecution> findFailedExecutions(Pageable pageable);
    
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.businessRule = :rule " +
           "AND e.status = 'FAILED' ORDER BY e.executedAt DESC")
    List<BusinessRuleExecution> findFailedExecutionsForRule(@Param("rule") BusinessRuleEntity rule);
    
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.errorMessage LIKE %:errorPattern% " +
           "ORDER BY e.executedAt DESC")
    List<BusinessRuleExecution> findByErrorPattern(@Param("errorPattern") String errorPattern);
    
    // Performance queries
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.durationMs > :threshold " +
           "ORDER BY e.durationMs DESC")
    List<BusinessRuleExecution> findSlowExecutions(@Param("threshold") Long threshold);
    
    @Query("SELECT AVG(e.durationMs) FROM BusinessRuleExecution e WHERE e.businessRule = :rule " +
           "AND e.status = 'COMPLETED'")
    Double getAverageExecutionTime(@Param("rule") BusinessRuleEntity rule);
    
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.businessRule = :rule " +
           "ORDER BY e.durationMs DESC")
    Page<BusinessRuleExecution> findSlowestExecutions(@Param("rule") BusinessRuleEntity rule, Pageable pageable);
    
    // Retry queries
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.parentExecution IS NOT NULL " +
           "AND e.triggerType = 'RETRY'")
    List<BusinessRuleExecution> findRetryExecutions();
    
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.parentExecution = :parent")
    List<BusinessRuleExecution> findChildExecutions(@Param("parent") BusinessRuleExecution parent);
    
    @Query("SELECT COUNT(e) FROM BusinessRuleExecution e WHERE e.parentExecution = :parent " +
           "AND e.triggerType = 'RETRY'")
    long countRetries(@Param("parent") BusinessRuleExecution parent);
    
    // Simulation and dry-run queries
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.isDryRun = true " +
           "ORDER BY e.executedAt DESC")
    Page<BusinessRuleExecution> findDryRunExecutions(Pageable pageable);
    
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.isSimulation = true " +
           "ORDER BY e.executedAt DESC")
    Page<BusinessRuleExecution> findSimulationExecutions(Pageable pageable);
    
    // Statistics queries
    @Query("SELECT COUNT(e) FROM BusinessRuleExecution e WHERE e.businessRule = :rule")
    long countExecutionsForRule(@Param("rule") BusinessRuleEntity rule);
    
    @Query("SELECT COUNT(e) FROM BusinessRuleExecution e WHERE e.businessRule = :rule " +
           "AND e.status = :status")
    long countExecutionsByStatus(@Param("rule") BusinessRuleEntity rule, 
                                @Param("status") ExecutionStatus status);
    
    @Query("SELECT e.status, COUNT(e) FROM BusinessRuleExecution e WHERE e.businessRule = :rule " +
           "GROUP BY e.status")
    List<Object[]> getExecutionStatusDistribution(@Param("rule") BusinessRuleEntity rule);
    
    @Query("SELECT DATE(e.executedAt), COUNT(e) FROM BusinessRuleExecution e " +
           "WHERE e.businessRule = :rule AND e.executedAt >= :since " +
           "GROUP BY DATE(e.executedAt) ORDER BY DATE(e.executedAt)")
    List<Object[]> getExecutionCountByDay(@Param("rule") BusinessRuleEntity rule, 
                                         @Param("since") LocalDateTime since);
    
    // Plugin usage queries
    @Query("SELECT e FROM BusinessRuleExecution e WHERE e.pluginsUsed LIKE %:pluginName%")
    List<BusinessRuleExecution> findByPluginUsage(@Param("pluginName") String pluginName);
    
    @Query("SELECT e.pluginsUsed, COUNT(e) FROM BusinessRuleExecution e " +
           "WHERE e.pluginsUsed IS NOT NULL GROUP BY e.pluginsUsed")
    List<Object[]> getPluginUsageStatistics();
    
    // Cleanup queries
    @Query("DELETE FROM BusinessRuleExecution e WHERE e.executedAt < :cutoffDate " +
           "AND e.isDryRun = true")
    int deleteOldDryRuns(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("DELETE FROM BusinessRuleExecution e WHERE e.executedAt < :cutoffDate " +
           "AND e.isSimulation = true")
    int deleteOldSimulations(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Aggregate queries
    @Query("SELECT e.businessRule, COUNT(e), AVG(e.durationMs), " +
           "SUM(CASE WHEN e.status = 'COMPLETED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN e.status = 'FAILED' THEN 1 ELSE 0 END) " +
           "FROM BusinessRuleExecution e WHERE e.executedAt >= :since " +
           "GROUP BY e.businessRule")
    List<Object[]> getExecutionSummaryByRule(@Param("since") LocalDateTime since);
    
    @Query("SELECT e.executedByUser, COUNT(e), AVG(e.durationMs) " +
           "FROM BusinessRuleExecution e WHERE e.executedAt >= :since " +
           "GROUP BY e.executedByUser")
    List<Object[]> getExecutionSummaryByUser(@Param("since") LocalDateTime since);
}