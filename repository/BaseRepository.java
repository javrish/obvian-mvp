package api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Base repository interface that extends JpaRepository with common operations.
 * All entity repositories should extend this interface to ensure consistent
 * data access patterns across the application.
 * 
 * @param <T> The entity type
 * @param <ID> The entity ID type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID> {
    
    /**
     * Find all entities with pagination support.
     * This is already provided by JpaRepository but documented for clarity.
     * 
     * @param pageable pagination information
     * @return page of entities
     */
    Page<T> findAll(Pageable pageable);
    
    /**
     * Check if an entity exists by its ID.
     * This is already provided by JpaRepository but documented for clarity.
     * 
     * @param id entity ID
     * @return true if entity exists, false otherwise
     */
    boolean existsById(ID id);
    
    /**
     * Count total number of entities.
     * This is already provided by JpaRepository but documented for clarity.
     * 
     * @return total count of entities
     */
    long count();
    
    /**
     * Find entities by their IDs.
     * This is already provided by JpaRepository but documented for clarity.
     * 
     * @param ids collection of entity IDs
     * @return list of found entities
     */
    List<T> findAllById(Iterable<ID> ids);
    
    /**
     * Save an entity and flush changes immediately.
     * This is already provided by JpaRepository but documented for clarity.
     * 
     * @param entity entity to save
     * @return saved entity
     */
    <S extends T> S saveAndFlush(S entity);
    
    /**
     * Save multiple entities in a batch.
     * This is already provided by JpaRepository but documented for clarity.
     * 
     * @param entities entities to save
     * @return list of saved entities
     */
    <S extends T> List<S> saveAll(Iterable<S> entities);
    
    /**
     * Delete an entity by its ID.
     * This is already provided by JpaRepository but documented for clarity.
     * 
     * @param id entity ID
     */
    void deleteById(ID id);
    
    /**
     * Delete multiple entities.
     * This is already provided by JpaRepository but documented for clarity.
     * 
     * @param entities entities to delete
     */
    void deleteAll(Iterable<? extends T> entities);
    
    /**
     * Delete all entities of this type.
     * This is already provided by JpaRepository but documented for clarity.
     * WARNING: Use with caution in production environments.
     */
    void deleteAll();
}