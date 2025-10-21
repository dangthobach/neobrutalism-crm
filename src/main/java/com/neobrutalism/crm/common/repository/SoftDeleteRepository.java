package com.neobrutalism.crm.common.repository;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for soft deletable entities
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T extends SoftDeletableEntity>
        extends BaseRepository<T> {

    /**
     * Find all non-deleted entities
     */
    default List<T> findAllActive() {
        return findAll((Specification<T>) (root, query, cb) ->
                cb.isFalse(root.get("deleted")));
    }

    /**
     * Find all non-deleted entities with pagination
     */
    default Page<T> findAllActive(Pageable pageable) {
        return findAll((Specification<T>) (root, query, cb) ->
                cb.isFalse(root.get("deleted")), pageable);
    }

    /**
     * Find all deleted entities
     */
    default List<T> findAllDeleted() {
        return findAll((Specification<T>) (root, query, cb) ->
                cb.isTrue(root.get("deleted")));
    }

    /**
     * Find all deleted entities with pagination
     */
    default Page<T> findAllDeleted(Pageable pageable) {
        return findAll((Specification<T>) (root, query, cb) ->
                cb.isTrue(root.get("deleted")), pageable);
    }

    /**
     * Find by id if not deleted
     */
    default Optional<T> findByIdActive(UUID id) {
        return findOne((Specification<T>) (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("id"), id),
                        cb.isFalse(root.get("deleted"))
                ));
    }
}
