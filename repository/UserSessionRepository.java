package api.repository;

import api.entity.User;
import api.entity.UserSession;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for UserSession entity.
 */
@Repository
public interface UserSessionRepository extends BaseRepository<UserSession, String> {
    
    /**
     * Find session by token.
     */
    Optional<UserSession> findBySessionToken(String sessionToken);
    
    /**
     * Find session by refresh token.
     */
    Optional<UserSession> findByRefreshToken(String refreshToken);
    
    /**
     * Find active sessions for a user.
     */
    List<UserSession> findByUserAndIsActiveTrue(User user);
    
    /**
     * Find all sessions for a user.
     */
    List<UserSession> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Find expired sessions.
     */
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now AND s.isActive = true")
    List<UserSession> findExpiredSessions(@Param("now") Instant now);
    
    /**
     * Revoke all active sessions for a user.
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.isActive = false, s.revokedAt = :now, s.revokedReason = :reason " +
           "WHERE s.user = :user AND s.isActive = true")
    int revokeAllUserSessions(@Param("user") User user, @Param("now") Instant now, @Param("reason") String reason);
    
    /**
     * Revoke session by token.
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.isActive = false, s.revokedAt = :now, s.revokedReason = :reason " +
           "WHERE s.sessionToken = :token")
    int revokeSessionByToken(@Param("token") String token, @Param("now") Instant now, @Param("reason") String reason);
    
    /**
     * Clean up old expired sessions.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :threshold AND s.isActive = false")
    int deleteExpiredSessions(@Param("threshold") Instant threshold);
    
    /**
     * Count active sessions.
     */
    long countByIsActiveTrue();
    
    /**
     * Count active sessions for a user.
     */
    long countByUserAndIsActiveTrue(User user);
    
    /**
     * Find sessions by IP address.
     */
    List<UserSession> findByIpAddress(String ipAddress);
    
    /**
     * Update last accessed time.
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.lastAccessedAt = :now WHERE s.sessionToken = :token")
    int updateLastAccessedTime(@Param("token") String token, @Param("now") Instant now);
    
    /**
     * Find inactive sessions.
     */
    @Query("SELECT s FROM UserSession s WHERE s.lastAccessedAt < :threshold AND s.isActive = true")
    List<UserSession> findInactiveSessions(@Param("threshold") Instant threshold);
    
    /**
     * Check if session token exists.
     */
    boolean existsBySessionToken(String sessionToken);
}