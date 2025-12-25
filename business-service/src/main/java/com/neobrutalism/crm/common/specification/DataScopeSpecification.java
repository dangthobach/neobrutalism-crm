package com.neobrutalism.crm.common.specification;

import com.neobrutalism.crm.common.security.DataScopeContext;
import com.neobrutalism.crm.domain.user.model.DataScope;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Data Scope Specification - JPA Specification để filter dữ liệu theo data scope
 *
 * Sử dụng:
 * <pre>
 * Specification<User> spec = DataScopeSpecification.<User>create()
 *     .and(UserSpecifications.hasStatus(UserStatus.ACTIVE));
 *
 * List<User> users = userRepository.findAll(spec);
 * </pre>
 */
public class DataScopeSpecification {

    private DataScopeSpecification() {
        // Private constructor
    }

    /**
     * Tạo specification để filter theo data scope
     * Áp dụng cho entities có branchId và createdBy
     */
    public static <T> Specification<T> create() {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (!DataScopeContext.hasContext()) {
                // Không có context, không filter
                return cb.conjunction();
            }

            DataScope dataScope = DataScopeContext.getCurrentDataScope();
            List<Predicate> predicates = new ArrayList<>();

            switch (dataScope) {
                case ALL_BRANCHES:
                    // Management role - Không filter, xem tất cả
                    break;

                case CURRENT_BRANCH:
                    // ORC role - Filter theo branch và các branch con
                    Set<UUID> accessibleBranchIds = DataScopeContext.getAccessibleBranchIds();
                    if (!accessibleBranchIds.isEmpty()) {
                        try {
                            predicates.add(root.get("branchId").in(accessibleBranchIds));
                        } catch (IllegalArgumentException e) {
                            // Entity không có branchId field, skip
                        }
                    }
                    break;

                case SELF_ONLY:
                    // Maker/Checker role - Chỉ xem bản ghi của mình
                    UUID currentUserId = DataScopeContext.getCurrentUserId();
                    if (currentUserId != null) {
                        try {
                            predicates.add(cb.equal(root.get("createdBy"), currentUserId.toString()));
                        } catch (IllegalArgumentException e) {
                            // Entity không có createdBy field, skip
                        }
                    }
                    break;
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Tạo specification với custom field name cho branch
     */
    public static <T> Specification<T> createWithBranchField(String branchFieldName) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (!DataScopeContext.hasContext()) {
                return cb.conjunction();
            }

            DataScope dataScope = DataScopeContext.getCurrentDataScope();
            List<Predicate> predicates = new ArrayList<>();

            switch (dataScope) {
                case ALL_BRANCHES:
                    break;

                case CURRENT_BRANCH:
                    Set<UUID> accessibleBranchIds = DataScopeContext.getAccessibleBranchIds();
                    if (!accessibleBranchIds.isEmpty()) {
                        try {
                            predicates.add(root.get(branchFieldName).in(accessibleBranchIds));
                        } catch (IllegalArgumentException e) {
                            // Field not found, skip
                        }
                    }
                    break;

                case SELF_ONLY:
                    UUID currentUserId = DataScopeContext.getCurrentUserId();
                    if (currentUserId != null) {
                        try {
                            predicates.add(cb.equal(root.get("createdBy"), currentUserId.toString()));
                        } catch (IllegalArgumentException e) {
                            // Field not found, skip
                        }
                    }
                    break;
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Tạo specification với custom creator field name
     */
    public static <T> Specification<T> createWithCreatorField(String creatorFieldName) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (!DataScopeContext.hasContext()) {
                return cb.conjunction();
            }

            DataScope dataScope = DataScopeContext.getCurrentDataScope();

            if (dataScope == DataScope.SELF_ONLY) {
                UUID currentUserId = DataScopeContext.getCurrentUserId();
                if (currentUserId != null) {
                    try {
                        return cb.equal(root.get(creatorFieldName), currentUserId.toString());
                    } catch (IllegalArgumentException e) {
                        // Field not found
                    }
                }
            }

            return cb.conjunction();
        };
    }
}
