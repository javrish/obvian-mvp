package api.repository;

import api.entity.Role;
import api.entity.User;
import api.entity.UserRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserRole entity.
 */
@Repository
public interface UserRoleRepository extends BaseRepository<UserRole, Long> {
    
    /**
     * Find roles for a user.
     */
    List<UserRole> findByUser(User user);
    
    /**
     * Find active roles for a user.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.user = :user AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<UserRole> findActiveRolesByUser(@Param("user") User user, @Param("now") Instant now);
    
    /**
     * Find users with a specific role.
     */
    List<UserRole> findByRole(Role role);
    
    /**
     * Check if user has a specific role.
     */
    boolean existsByUserAndRole(User user, Role role);
    
    /**
     * Check if user has an active role.
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.user = :user AND ur.role = :role " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    boolean hasActiveRole(@Param("user") User user, @Param("role") Role role, @Param("now") Instant now);
    
    /**
     * Find user role by user and role.
     */
    Optional<UserRole> findByUserAndRole(User user, Role role);
    
    /**
     * Find expired roles.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt IS NOT NULL AND ur.expiresAt < :now")
    List<UserRole> findExpiredRoles(@Param("now") Instant now);
    
    /**
     * Count users by role.
     */
    @Query("SELECT ur.role, COUNT(DISTINCT ur.user) FROM UserRole ur GROUP BY ur.role")
    List<Object[]> countUsersByRole();
    
    /**
     * Find roles granted by a specific user.
     */
    List<UserRole> findByGrantedBy(String grantedBy);
    
    /**
     * Delete all roles for a user.
     */
    void deleteByUser(User user);
    
    /**
     * Find roles expiring soon.
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt BETWEEN :now AND :threshold")
    List<UserRole> findRolesExpiringSoon(@Param("now") Instant now, @Param("threshold") Instant threshold);
}