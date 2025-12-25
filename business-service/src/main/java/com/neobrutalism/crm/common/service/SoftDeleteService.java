package com.neobrutalism.crm.common.service;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import com.neobrutalism.crm.common.repository.SoftDeleteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for soft deletable entities
 */
@Slf4j
public abstract class SoftDeleteService<T extends SoftDeletableEntity>
        extends AuditableService<T> {

    @Override
    protected abstract SoftDeleteRepository<T> getRepository();

    /**
     * Find all active (non-deleted) entities
     */
    public List<T> findAllActive() {
        return getRepository().findAllActive();
    }

    /**
     * Find all active entities with pagination
     */
    public Page<T> findAllActive(Pageable pageable) {
        return getRepository().findAllActive(pageable);
    }

    /**
     * Find all deleted entities
     */
    public List<T> findAllDeleted() {
        return getRepository().findAllDeleted();
    }

    /**
     * Find all deleted entities with pagination
     */
    public Page<T> findAllDeleted(Pageable pageable) {
        return getRepository().findAllDeleted(pageable);
    }

    /**
     * Find active entity by ID
     */
    public Optional<T> findByIdActive(UUID id) {
        return getRepository().findByIdActive(id);
    }

    /**
     * Soft delete entity
     */
    @Transactional
    public void softDelete(UUID id) {
        T entity = findById(id);
        entity.softDelete(getCurrentUser());
        getRepository().save(entity);
        createAuditLog(entity, "SOFT_DELETE", null, null, null);
        log.debug("Soft deleted {} with id: {}", getEntityName(), id);
    }

    /**
     * Restore soft deleted entity
     */
    @Transactional
    public T restore(UUID id) {
        T entity = findById(id);
        if (!entity.isDeleted()) {
            log.warn("Entity {} with id {} is not deleted", getEntityName(), id);
            return entity;
        }
        entity.restore();
        T restored = getRepository().save(entity);
        createAuditLog(restored, "RESTORE", null, null, null);
        log.debug("Restored {} with id: {}", getEntityName(), id);
        return restored;
    }

    /**
     * Hard delete entity (permanent deletion)
     */
    @Transactional
    public void hardDelete(UUID id) {
        super.deleteById(id);
        log.debug("Hard deleted {} with id: {}", getEntityName(), id);
    }

    /**
     * Override default delete to use soft delete
     */
    @Override
    @Transactional
    public void deleteById(UUID id) {
        softDelete(id);
    }

    @Override
    @Transactional
    public void delete(T entity) {
        softDelete(entity.getId());
    }
}
