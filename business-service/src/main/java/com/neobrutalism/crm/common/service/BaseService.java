package com.neobrutalism.crm.common.service;

import com.neobrutalism.crm.common.entity.BaseEntity;
import com.neobrutalism.crm.common.exception.DatabaseExceptionHandler;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.common.exception.ValidationException;
import com.neobrutalism.crm.common.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Base service with CRUD operations and lifecycle hooks
 */
@Slf4j
public abstract class BaseService<T extends BaseEntity> {

    protected abstract BaseRepository<T> getRepository();
    protected abstract String getEntityName();

    /**
     * Lifecycle hooks - override in subclasses for custom behavior
     */
    protected void beforeCreate(T entity) {}
    protected void afterCreate(T entity) {}
    protected void beforeUpdate(T entity) {}
    protected void afterUpdate(T entity) {}
    protected void beforeDelete(T entity) {}
    protected void afterDelete(T entity) {}

    /**
     * Find by ID
     */
    public T findById(UUID id) {
        return getRepository().findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forResource(getEntityName(), id));
    }

    /**
     * Find by ID optional
     */
    public Optional<T> findByIdOptional(UUID id) {
        return getRepository().findById(id);
    }

    /**
     * Find all
     */
    public List<T> findAll() {
        return getRepository().findAll();
    }

    /**
     * Find all with pagination
     */
    public Page<T> findAll(Pageable pageable) {
        return getRepository().findAll(pageable);
    }

    /**
     * Find all by specification
     */
    public List<T> findAll(Specification<T> spec) {
        return getRepository().findAll(spec);
    }

    /**
     * Find all by specification with pagination
     */
    public Page<T> findAll(Specification<T> spec, Pageable pageable) {
        return getRepository().findAll(spec, pageable);
    }

    /**
     * Count all
     */
    public long count() {
        return getRepository().count();
    }

    /**
     * Count by specification
     */
    public long count(Specification<T> spec) {
        return getRepository().count(spec);
    }

    /**
     * Check if entity exists by ID
     */
    public boolean existsById(UUID id) {
        return getRepository().existsById(id);
    }

    /**
     * Create new entity
     */
    @Transactional
    public T create(T entity) {
        beforeCreate(entity);
        T saved = getRepository().save(entity);
        afterCreate(saved);
        return saved;
    }

    /**
     * Update existing entity
     */
    @Transactional
    public T update(UUID id, T entity) {
        T existing = findById(id);
        beforeUpdate(entity);
        T updated = getRepository().save(entity);
        afterUpdate(updated);
        return updated;
    }

    /**
     * Save entity (create or update)
     */
    @Transactional
    public T save(T entity) {
        if (entity.isNew()) {
            return create(entity);
        } else {
            return update(entity.getId(), entity);
        }
    }

    /**
     * Delete by ID
     */
    @Transactional
    public void deleteById(UUID id) {
        T entity = findById(id);
        delete(entity);
    }

    /**
     * Delete entity
     */
    @Transactional
    public void delete(T entity) {
        beforeDelete(entity);
        getRepository().delete(entity);
        afterDelete(entity);
    }

    /**
     * Delete all by IDs
     */
    @Transactional
    public void deleteAllById(Iterable<UUID> ids) {
        ids.forEach(this::deleteById);
    }

    // ==================== Database Integrity Check Methods ====================

    /**
     * Save entity with database integrity constraint checking
     * Converts DataIntegrityViolationException to user-friendly ValidationException
     *
     * @param entity The entity to save
     * @param constraintMessages Map of constraint/field names to user-friendly error messages
     * @return Saved entity
     * @throws ValidationException If constraint violation occurs
     */
    protected T saveWithIntegrityCheck(T entity, Map<String, String> constraintMessages) {
        try {
            return getRepository().save(entity);
        } catch (DataIntegrityViolationException ex) {
            throw DatabaseExceptionHandler.handleDataIntegrityViolation(
                ex, getEntityName(), constraintMessages
            );
        }
    }

    /**
     * Create entity with database integrity constraint checking
     *
     * @param entity The entity to create
     * @param constraintMessages Map of constraint/field names to user-friendly error messages
     * @return Created entity
     * @throws ValidationException If constraint violation occurs
     */
    @Transactional
    protected T createWithIntegrityCheck(T entity, Map<String, String> constraintMessages) {
        try {
            beforeCreate(entity);
            T saved = getRepository().save(entity);
            afterCreate(saved);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw DatabaseExceptionHandler.handleDataIntegrityViolation(
                ex, getEntityName(), constraintMessages
            );
        }
    }

    /**
     * Update entity with database integrity constraint checking
     *
     * @param id The entity ID
     * @param entity The entity to update
     * @param constraintMessages Map of constraint/field names to user-friendly error messages
     * @return Updated entity
     * @throws ValidationException If constraint violation occurs
     */
    @Transactional
    protected T updateWithIntegrityCheck(UUID id, T entity, Map<String, String> constraintMessages) {
        try {
            T existing = findById(id);
            beforeUpdate(entity);
            T updated = getRepository().save(entity);
            afterUpdate(updated);
            return updated;
        } catch (DataIntegrityViolationException ex) {
            throw DatabaseExceptionHandler.handleDataIntegrityViolation(
                ex, getEntityName(), constraintMessages
            );
        }
    }

    /**
     * Handle unique constraint violation for a specific field
     *
     * @param entity The entity
     * @param fieldName The field name that should be unique
     * @param fieldValue The duplicate value
     * @throws ValidationException With specific message about duplicate field
     */
    protected void handleUniqueConstraintViolation(T entity, String fieldName, Object fieldValue) {
        throw DatabaseExceptionHandler.handleUniqueConstraintViolation(
            null, getEntityName(), fieldName, fieldValue
        );
    }

    /**
     * Delete entity with foreign key constraint checking
     *
     * @param entity The entity to delete
     * @param referencedEntity The entity type that might reference this entity
     * @throws ValidationException If foreign key constraint violation occurs
     */
    @Transactional
    protected void deleteWithIntegrityCheck(T entity, String referencedEntity) {
        try {
            beforeDelete(entity);
            getRepository().delete(entity);
            afterDelete(entity);
        } catch (DataIntegrityViolationException ex) {
            if (DatabaseExceptionHandler.isForeignKeyViolation(ex)) {
                throw DatabaseExceptionHandler.handleForeignKeyViolation(
                    ex, getEntityName(), referencedEntity
                );
            }
            throw DatabaseExceptionHandler.handleDataIntegrityViolation(
                ex, getEntityName(), Map.of()
            );
        }
    }
}
