package api.repository;

import api.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity.
 */
@Repository
public interface UserRepository extends BaseRepository<User, String> {
    
    /**
     * Find user by email address.
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by Google ID.
     */
    Optional<User> findByGoogleId(String googleId);
    
    /**
     * Check if user exists by email.
     */
    boolean existsByEmail(String email);
    
    /**
     * Find all active users.
     */
    List<User> findByIsActiveTrue();
    
    /**
     * Find users who have completed onboarding.
     */
    List<User> findByOnboardingCompletedTrue();
    
    /**
     * Find users created after a specific date.
     */
    List<User> findByCreatedAtAfter(Instant date);
    
    /**
     * Find users by automation category.
     */
    List<User> findBySelectedAutomationCategory(String category);
    
    /**
     * Find users who haven't logged in recently.
     */
    @Query("SELECT u FROM User u WHERE u.lastLogin < :date")
    List<User> findInactiveUsers(@Param("date") Instant date);
    
    /**
     * Count users by verification status.
     */
    long countByIsVerified(boolean isVerified);
    
    /**
     * Find users with specific role.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.role = :roleName")
    List<User> findUsersByRole(@Param("roleName") String roleName);
    
    /**
     * Find recently active users.
     */
    @Query("SELECT u FROM User u WHERE u.lastLogin > :since ORDER BY u.lastLogin DESC")
    List<User> findRecentlyActiveUsers(@Param("since") Instant since);
    
    /**
     * Update user's last login time.
     */
    @Query("UPDATE User u SET u.lastLogin = :loginTime WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") String userId, @Param("loginTime") Instant loginTime);
}