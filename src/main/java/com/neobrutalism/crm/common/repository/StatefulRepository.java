package com.neobrutalism.crm.common.repository;

import com.neobrutalism.crm.common.entity.StatefulEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Repository for stateful entities
 */
@NoRepositoryBean
public interface StatefulRepository<T extends StatefulEntity<S>, S extends Enum<S>>
        extends SoftDeleteRepository<T> {

    /**
     * Find all active entities by status
     */
    default List<T> findByStatus(S status) {
        return findAll((Specification<T>) (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("status"), status),
                        cb.isFalse(root.get("deleted"))
                ));
    }

    /**
     * Find all active entities by status with pagination
     */
    default Page<T> findByStatus(S status, Pageable pageable) {
        return findAll((Specification<T>) (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("status"), status),
                        cb.isFalse(root.get("deleted"))
                ), pageable);
    }

    /**
     * Find all active entities by multiple statuses
     */
    default List<T> findByStatusIn(List<S> statuses) {
        return findAll((Specification<T>) (root, query, cb) ->
                cb.and(
                        root.get("status").in(statuses),
                        cb.isFalse(root.get("deleted"))
                ));
    }

    /**
     * Find all active entities by multiple statuses with pagination
     */
    default Page<T> findByStatusIn(List<S> statuses, Pageable pageable) {
        return findAll((Specification<T>) (root, query, cb) ->
                cb.and(
                        root.get("status").in(statuses),
                        cb.isFalse(root.get("deleted"))
                ), pageable);
    }

    /**
     * Count by status
     */
    default long countByStatus(S status) {
        return count((Specification<T>) (root, query, cb) ->
                cb.and(
                        cb.equal(root.get("status"), status),
                        cb.isFalse(root.get("deleted"))
                ));
    }
}
