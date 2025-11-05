package api.repository;

import api.entity.UserPluginConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for user plugin configurations.
 * Handles persistence of user-specific plugin settings and API keys.
 */
@Repository
public interface UserPluginConfigurationRepository extends JpaRepository<UserPluginConfiguration, String> {
    
    /**
     * Find all plugin configurations for a specific user.
     */
    List<UserPluginConfiguration> findByUserId(String userId);
    
    /**
     * Find all enabled plugin configurations for a specific user.
     */
    List<UserPluginConfiguration> findByUserIdAndEnabled(String userId, boolean enabled);
    
    /**
     * Find a specific plugin configuration for a user.
     */
    Optional<UserPluginConfiguration> findByUserIdAndPluginId(String userId, String pluginId);
    
    /**
     * Check if a user has a specific plugin installed.
     */
    boolean existsByUserIdAndPluginId(String userId, String pluginId);
    
    /**
     * Find all configured plugins for a user.
     */
    List<UserPluginConfiguration> findByUserIdAndConfigured(String userId, boolean configured);
    
    /**
     * Delete a specific plugin configuration.
     */
    void deleteByUserIdAndPluginId(String userId, String pluginId);
    
    /**
     * Find plugins that haven't been used recently.
     */
    @Query("SELECT upc FROM UserPluginConfiguration upc WHERE upc.userId = :userId " +
           "AND upc.lastUsedDate < :beforeDate ORDER BY upc.lastUsedDate DESC")
    List<UserPluginConfiguration> findUnusedPlugins(@Param("userId") String userId, 
                                                    @Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Find plugins with high failure rates.
     */
    @Query("SELECT upc FROM UserPluginConfiguration upc WHERE upc.userId = :userId " +
           "AND upc.failureCount > upc.successCount AND upc.executionCount > :minExecutions")
    List<UserPluginConfiguration> findProblematicPlugins(@Param("userId") String userId,
                                                         @Param("minExecutions") int minExecutions);
    
    /**
     * Get plugin usage statistics for a user.
     */
    @Query("SELECT upc.pluginName as plugin, SUM(upc.executionCount) as count " +
           "FROM UserPluginConfiguration upc WHERE upc.userId = :userId " +
           "GROUP BY upc.pluginName ORDER BY count DESC")
    List<Object[]> getPluginUsageStats(@Param("userId") String userId);
    
    /**
     * Find all users who have a specific plugin installed.
     */
    List<UserPluginConfiguration> findByPluginId(String pluginId);
    
    /**
     * Count total installations of a plugin.
     */
    long countByPluginId(String pluginId);
    
    /**
     * Find expired plugin subscriptions.
     */
    @Query("SELECT upc FROM UserPluginConfiguration upc " +
           "WHERE upc.subscriptionExpiresAt IS NOT NULL " +
           "AND upc.subscriptionExpiresAt < :now AND upc.enabled = true")
    List<UserPluginConfiguration> findExpiredSubscriptions(@Param("now") LocalDateTime now);
    
    /**
     * Update last used timestamp.
     */
    @Query("UPDATE UserPluginConfiguration upc SET upc.lastUsedDate = :now, " +
           "upc.executionCount = upc.executionCount + 1 " +
           "WHERE upc.id = :id")
    void updateLastUsed(@Param("id") String id, @Param("now") LocalDateTime now);
}