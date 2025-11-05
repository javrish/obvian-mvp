package api.repository;

import api.entity.BusinessRuleEntity;
import api.entity.BusinessRuleEntity.RuleType;
import api.entity.BusinessRuleEntity.ValidationStatus;
import api.entity.User;
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
 * JPA repository for BusinessRuleEntity persistence operations.
 * Provides comprehensive data access methods for business rules.
 */
@Repository
public interface BusinessRuleRepository extends BaseRepository<BusinessRuleEntity, String> {
    
    // Basic queries
    Optional<BusinessRuleEntity> findByIdAndArchivedFalse(String id);
    
    List<BusinessRuleEntity> findByActiveTrue();
    
    List<BusinessRuleEntity> findByArchivedFalse();
    
    Page<BusinessRuleEntity> findByArchivedFalse(Pageable pageable);
    
    // User-specific queries
    List<BusinessRuleEntity> findByUserAndArchivedFalse(User user);
    
    Page<BusinessRuleEntity> findByUser(User user, Pageable pageable);
    
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.user = :user AND r.active = true AND r.archived = false")
    List<BusinessRuleEntity> findActiveRulesByUser(@Param("user") User user);
    
    // Name and type queries
    Optional<BusinessRuleEntity> findByNameAndArchivedFalse(String name);
    
    List<BusinessRuleEntity> findByRuleTypeAndActiveTrue(RuleType ruleType);
    
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.ruleType = :type AND r.archived = false")
    Page<BusinessRuleEntity> findByRuleType(@Param("type") RuleType ruleType, Pageable pageable);
    
    // Search queries
    @Query("SELECT r FROM BusinessRuleEntity r WHERE " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.naturalLanguageText) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND r.archived = false")
    Page<BusinessRuleEntity> searchRules(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Tag queries
    @Query("SELECT DISTINCT r FROM BusinessRuleEntity r JOIN r.tags t WHERE t IN :tags AND r.archived = false")
    List<BusinessRuleEntity> findByTagsIn(@Param("tags") Set<String> tags);
    
    @Query("SELECT DISTINCT r FROM BusinessRuleEntity r JOIN r.tags t WHERE t = :tag AND r.archived = false")
    List<BusinessRuleEntity> findByTag(@Param("tag") String tag);
    
    // Priority queries
    List<BusinessRuleEntity> findByPriorityGreaterThanAndActiveTrueOrderByPriorityDesc(Integer priority);
    
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.active = true AND r.archived = false ORDER BY r.priority DESC")
    List<BusinessRuleEntity> findAllActiveOrderedByPriority();
    
    // Temporal queries
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.schedule IS NOT NULL AND r.active = true AND r.archived = false")
    List<BusinessRuleEntity> findScheduledRules();
    
    @Query("SELECT r FROM BusinessRuleEntity r WHERE " +
           "r.effectiveFrom <= :now AND (r.effectiveUntil IS NULL OR r.effectiveUntil > :now) " +
           "AND r.active = true AND r.archived = false")
    List<BusinessRuleEntity> findEffectiveRules(@Param("now") LocalDateTime now);
    
    // Validation queries
    List<BusinessRuleEntity> findByValidationStatus(ValidationStatus status);
    
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.validationStatus = :status AND r.archived = false")
    Page<BusinessRuleEntity> findByValidationStatus(@Param("status") ValidationStatus status, Pageable pageable);
    
    @Query("SELECT r FROM BusinessRuleEntity r WHERE " +
           "(r.lastValidatedAt IS NULL OR r.lastValidatedAt < :threshold) " +
           "AND r.archived = false")
    List<BusinessRuleEntity> findRulesNeedingValidation(@Param("threshold") LocalDateTime threshold);
    
    // Execution statistics queries
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.lastExecutedAt IS NOT NULL ORDER BY r.lastExecutedAt DESC")
    Page<BusinessRuleEntity> findRecentlyExecuted(Pageable pageable);
    
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.executionCount > :minCount ORDER BY r.executionCount DESC")
    List<BusinessRuleEntity> findFrequentlyExecuted(@Param("minCount") Long minCount);
    
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.failureCount > 0 AND r.archived = false ORDER BY r.failureCount DESC")
    List<BusinessRuleEntity> findRulesWithFailures();
    
    // Dependency queries
    @Query("SELECT r.dependencies FROM BusinessRuleEntity r WHERE r.id = :ruleId")
    Set<BusinessRuleEntity> findDependencies(@Param("ruleId") String ruleId);
    
    @Query("SELECT r FROM BusinessRuleEntity r JOIN r.dependencies d WHERE d.id = :dependencyId")
    List<BusinessRuleEntity> findRulesDependingOn(@Param("dependencyId") String dependencyId);
    
    // Conflict queries
    @Query("SELECT r.conflicts FROM BusinessRuleEntity r WHERE r.id = :ruleId")
    Set<BusinessRuleEntity> findConflicts(@Param("ruleId") String ruleId);
    
    @Query("SELECT r FROM BusinessRuleEntity r JOIN r.conflicts c WHERE c.id = :conflictId")
    List<BusinessRuleEntity> findRulesConflictingWith(@Param("conflictId") String conflictId);
    
    // Archival queries
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.archived = true")
    Page<BusinessRuleEntity> findArchivedRules(Pageable pageable);
    
    @Query("SELECT r FROM BusinessRuleEntity r WHERE r.archived = true AND r.archivedAt > :since")
    List<BusinessRuleEntity> findRecentlyArchived(@Param("since") LocalDateTime since);
    
    // Update operations
    @Modifying
    @Query("UPDATE BusinessRuleEntity r SET r.active = :active WHERE r.id = :id")
    void updateActiveStatus(@Param("id") String id, @Param("active") Boolean active);
    
    @Modifying
    @Query("UPDATE BusinessRuleEntity r SET r.lastExecutedAt = :executedAt, " +
           "r.executionCount = r.executionCount + 1 WHERE r.id = :id")
    void updateExecutionStats(@Param("id") String id, @Param("executedAt") LocalDateTime executedAt);
    
    @Modifying
    @Query("UPDATE BusinessRuleEntity r SET r.validationStatus = :status, " +
           "r.validationErrors = :errors, r.lastValidatedAt = :validatedAt WHERE r.id = :id")
    void updateValidationStatus(@Param("id") String id, 
                               @Param("status") ValidationStatus status,
                               @Param("errors") String errors,
                               @Param("validatedAt") LocalDateTime validatedAt);
    
    @Modifying
    @Query("UPDATE BusinessRuleEntity r SET r.archived = true, " +
           "r.archivedAt = :archivedAt, r.archivedBy = :archivedBy WHERE r.id = :id")
    void archiveRule(@Param("id") String id, 
                    @Param("archivedAt") LocalDateTime archivedAt,
                    @Param("archivedBy") String archivedBy);
    
    // Bulk operations
    @Modifying
    @Query("UPDATE BusinessRuleEntity r SET r.active = false WHERE r.effectiveUntil < :now")
    int deactivateExpiredRules(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE BusinessRuleEntity r SET r.active = true WHERE r.effectiveFrom <= :now " +
           "AND (r.effectiveUntil IS NULL OR r.effectiveUntil > :now) AND r.archived = false")
    int activateEffectiveRules(@Param("now") LocalDateTime now);
    
    // Count queries
    long countByActiveTrue();
    
    long countByArchivedFalse();
    
    long countByRuleType(RuleType ruleType);
    
    long countByUser(User user);
    
    long countByValidationStatus(ValidationStatus status);
    
    @Query("SELECT COUNT(r) FROM BusinessRuleEntity r WHERE r.failureCount > 0")
    long countRulesWithFailures();
    
    // Existence checks
    boolean existsByNameAndArchivedFalse(String name);
    
    boolean existsByIdAndActiveTrue(String id);
}