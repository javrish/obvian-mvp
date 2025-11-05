package api.repository;

import api.entity.PluginEntity;
import api.entity.PluginEntity.PluginStatus;
import api.entity.PluginEntity.HealthStatus;
import api.entity.PluginEntity.DiscoveryMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JPA repository for PluginEntity persistence operations.
 * Provides comprehensive data access methods for plugin management.
 */
@Repository
public interface PluginRepository extends BaseRepository<PluginEntity, String> {
    
    // Basic queries
    Optional<PluginEntity> findByName(String name);
    
    Optional<PluginEntity> findByNameAndVersion(String name, String version);
    
    List<PluginEntity> findByActiveTrue();
    
    List<PluginEntity> findByAutoLoadTrue();
    
    List<PluginEntity> findByStatus(PluginStatus status);
    
    Page<PluginEntity> findByStatus(PluginStatus status, Pageable pageable);
    
    // Category queries
    List<PluginEntity> findByCategory(String category);
    
    List<PluginEntity> findByCategoryAndActiveTrue(String category);
    
    @Query("SELECT DISTINCT p.category FROM PluginEntity p WHERE p.category IS NOT NULL")
    List<String> findAllCategories();
    
    // Health status queries
    List<PluginEntity> findByHealthStatus(HealthStatus healthStatus);
    
    @Query("SELECT p FROM PluginEntity p WHERE p.healthStatus = :status AND p.active = true")
    List<PluginEntity> findActiveByHealthStatus(@Param("status") HealthStatus healthStatus);
    
    @Query("SELECT p FROM PluginEntity p WHERE p.lastHealthCheck < :threshold OR p.lastHealthCheck IS NULL")
    List<PluginEntity> findPluginsNeedingHealthCheck(@Param("threshold") LocalDateTime threshold);
    
    // Search queries
    @Query("SELECT p FROM PluginEntity p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<PluginEntity> searchPlugins(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Action queries
    @Query("SELECT DISTINCT p FROM PluginEntity p JOIN p.supportedActions a WHERE a IN :actions")
    List<PluginEntity> findBySupportedActionsIn(@Param("actions") Set<String> actions);
    
    @Query("SELECT DISTINCT p FROM PluginEntity p JOIN p.supportedActions a WHERE a = :action AND p.active = true")
    List<PluginEntity> findActiveByAction(@Param("action") String action);
    
    // Dependency queries
    @Query("SELECT DISTINCT p FROM PluginEntity p JOIN p.dependencies d WHERE d = :dependency")
    List<PluginEntity> findByDependency(@Param("dependency") String dependency);
    
    @Query("SELECT p FROM PluginEntity p WHERE SIZE(p.dependencies) = 0")
    List<PluginEntity> findPluginsWithNoDependencies();
    
    // Priority queries
    List<PluginEntity> findByActiveTrueOrderByPriorityDesc();
    
    @Query("SELECT p FROM PluginEntity p WHERE p.category = :category AND p.active = true ORDER BY p.priority DESC")
    List<PluginEntity> findByCategoryOrderByPriority(@Param("category") String category);
    
    // Discovery queries
    List<PluginEntity> findByDiscoveryMethod(DiscoveryMethod method);
    
    @Query("SELECT p FROM PluginEntity p WHERE p.discoveredAt >= :since")
    List<PluginEntity> findRecentlyDiscovered(@Param("since") LocalDateTime since);
    
    @Query("SELECT p FROM PluginEntity p WHERE p.registeredAt IS NULL")
    List<PluginEntity> findUnregisteredPlugins();
    
    // Validation queries
    List<PluginEntity> findByValidatedFalse();
    
    @Query("SELECT p FROM PluginEntity p WHERE p.validated = false AND p.active = true")
    List<PluginEntity> findActiveUnvalidatedPlugins();
    
    @Query("SELECT p FROM PluginEntity p WHERE p.validationErrors IS NOT NULL")
    List<PluginEntity> findPluginsWithValidationErrors();
    
    // Security queries
    List<PluginEntity> findBySandboxedTrue();
    
    @Query("SELECT p FROM PluginEntity p WHERE p.securityScore < :threshold")
    List<PluginEntity> findLowSecurityPlugins(@Param("threshold") Integer threshold);
    
    // JAR path queries
    List<PluginEntity> findByJarPath(String jarPath);
    
    @Query("SELECT p FROM PluginEntity p WHERE p.jarPath LIKE :pattern")
    List<PluginEntity> findByJarPathPattern(@Param("pattern") String pattern);
    
    // Execution statistics queries
    @Query("SELECT p FROM PluginEntity p WHERE p.executionCount > :minCount ORDER BY p.executionCount DESC")
    List<PluginEntity> findFrequentlyUsed(@Param("minCount") Long minCount);
    
    @Query("SELECT p FROM PluginEntity p WHERE p.failureCount > 0 ORDER BY p.failureCount DESC")
    List<PluginEntity> findPluginsWithFailures();
    
    @Query("SELECT p FROM PluginEntity p WHERE p.lastExecutedAt IS NOT NULL ORDER BY p.lastExecutedAt DESC")
    Page<PluginEntity> findRecentlyExecuted(Pageable pageable);
    
    @Query("SELECT p FROM PluginEntity p WHERE p.avgExecutionTimeMs > :threshold ORDER BY p.avgExecutionTimeMs DESC")
    List<PluginEntity> findSlowPlugins(@Param("threshold") Long threshold);
    
    // Error queries
    @Query("SELECT p FROM PluginEntity p WHERE p.lastError IS NOT NULL ORDER BY p.lastErrorAt DESC")
    Page<PluginEntity> findPluginsWithErrors(Pageable pageable);
    
    @Query("SELECT p FROM PluginEntity p WHERE p.lastErrorAt >= :since")
    List<PluginEntity> findRecentErrors(@Param("since") LocalDateTime since);
    
    // Update operations
    @Modifying
    @Query("UPDATE PluginEntity p SET p.status = :status WHERE p.id = :id")
    void updateStatus(@Param("id") String id, @Param("status") PluginStatus status);
    
    @Modifying
    @Query("UPDATE PluginEntity p SET p.active = :active WHERE p.id = :id")
    void updateActiveStatus(@Param("id") String id, @Param("active") Boolean active);
    
    @Modifying
    @Query("UPDATE PluginEntity p SET p.healthStatus = :healthStatus, p.healthMessage = :message, " +
           "p.lastHealthCheck = :checkTime, p.responseTimeMs = :responseTime WHERE p.id = :id")
    void updateHealthStatus(@Param("id") String id, 
                          @Param("healthStatus") HealthStatus healthStatus,
                          @Param("message") String message,
                          @Param("checkTime") LocalDateTime checkTime,
                          @Param("responseTime") Long responseTime);
    
    @Modifying
    @Query("UPDATE PluginEntity p SET p.executionCount = p.executionCount + 1, " +
           "p.lastExecutedAt = :executedAt WHERE p.id = :id")
    void incrementExecutionCount(@Param("id") String id, @Param("executedAt") LocalDateTime executedAt);
    
    @Modifying
    @Query("UPDATE PluginEntity p SET p.successCount = p.successCount + 1 WHERE p.id = :id")
    void incrementSuccessCount(@Param("id") String id);
    
    @Modifying
    @Query("UPDATE PluginEntity p SET p.failureCount = p.failureCount + 1, " +
           "p.lastError = :error, p.lastErrorAt = :errorAt WHERE p.id = :id")
    void recordError(@Param("id") String id, @Param("error") String error, @Param("errorAt") LocalDateTime errorAt);
    
    // Bulk operations
    @Modifying
    @Query("UPDATE PluginEntity p SET p.active = false WHERE p.status = 'ERROR'")
    int deactivateErrorPlugins();
    
    @Modifying
    @Query("UPDATE PluginEntity p SET p.status = 'DEPRECATED' WHERE p.version < :version")
    int deprecateOldVersions(@Param("version") String version);
    
    // Count queries
    long countByActiveTrue();
    
    long countByStatus(PluginStatus status);
    
    long countByHealthStatus(HealthStatus healthStatus);
    
    long countByCategory(String category);
    
    long countByValidatedTrue();
    
    @Query("SELECT COUNT(p) FROM PluginEntity p WHERE p.failureCount > 0")
    long countPluginsWithFailures();
    
    // Existence checks
    boolean existsByName(String name);
    
    boolean existsByNameAndVersion(String name, String version);
    
    boolean existsByIdAndActiveTrue(String id);
    
    // Conflict queries
    @Query("SELECT p.conflicts FROM PluginEntity p WHERE p.id = :pluginId")
    Set<PluginEntity> findConflicts(@Param("pluginId") String pluginId);
    
    @Query("SELECT p FROM PluginEntity p JOIN p.conflicts c WHERE c.id = :conflictId")
    List<PluginEntity> findPluginsConflictingWith(@Param("conflictId") String conflictId);
}