package com.neobrutalism.crm.common.specification;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Base specification with common query patterns
 */
public class BaseSpecification {

    /**
     * Create specification that combines multiple specs with AND
     */
    public static <T> Specification<T> and(List<Specification<T>> specifications) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Specification<T> spec : specifications) {
                if (spec != null) {
                    Predicate predicate = spec.toPredicate(root, query, cb);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create specification that combines multiple specs with OR
     */
    public static <T> Specification<T> or(List<Specification<T>> specifications) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Specification<T> spec : specifications) {
                if (spec != null) {
                    Predicate predicate = spec.toPredicate(root, query, cb);
                    if (predicate != null) {
                        predicates.add(predicate);
                    }
                }
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Equal specification
     */
    public static <T> Specification<T> equal(String fieldName, Object value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(fieldName), value);
    }

    /**
     * Not equal specification
     */
    public static <T> Specification<T> notEqual(String fieldName, Object value) {
        return (root, query, cb) -> value == null ? null : cb.notEqual(root.get(fieldName), value);
    }

    /**
     * Like specification (case-insensitive)
     */
    public static <T> Specification<T> like(String fieldName, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get(fieldName)), "%" + value.toLowerCase() + "%");
        };
    }

    /**
     * In specification
     */
    public static <T> Specification<T> in(String fieldName, List<?> values) {
        return (root, query, cb) -> {
            if (values == null || values.isEmpty()) {
                return null;
            }
            return root.get(fieldName).in(values);
        };
    }

    /**
     * Greater than specification
     */
    public static <T, V extends Comparable<V>> Specification<T> greaterThan(String fieldName, V value) {
        return (root, query, cb) -> value == null ? null : cb.greaterThan(root.get(fieldName), value);
    }

    /**
     * Greater than or equal specification
     */
    public static <T, V extends Comparable<V>> Specification<T> greaterThanOrEqual(String fieldName, V value) {
        return (root, query, cb) -> value == null ? null : cb.greaterThanOrEqualTo(root.get(fieldName), value);
    }

    /**
     * Less than specification
     */
    public static <T, V extends Comparable<V>> Specification<T> lessThan(String fieldName, V value) {
        return (root, query, cb) -> value == null ? null : cb.lessThan(root.get(fieldName), value);
    }

    /**
     * Less than or equal specification
     */
    public static <T, V extends Comparable<V>> Specification<T> lessThanOrEqual(String fieldName, V value) {
        return (root, query, cb) -> value == null ? null : cb.lessThanOrEqualTo(root.get(fieldName), value);
    }

    /**
     * Between specification
     */
    public static <T, V extends Comparable<V>> Specification<T> between(String fieldName, V start, V end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return null;
            }
            if (start == null) {
                return cb.lessThanOrEqualTo(root.get(fieldName), end);
            }
            if (end == null) {
                return cb.greaterThanOrEqualTo(root.get(fieldName), start);
            }
            return cb.between(root.get(fieldName), start, end);
        };
    }

    /**
     * Is null specification
     */
    public static <T> Specification<T> isNull(String fieldName) {
        return (root, query, cb) -> cb.isNull(root.get(fieldName));
    }

    /**
     * Is not null specification
     */
    public static <T> Specification<T> isNotNull(String fieldName) {
        return (root, query, cb) -> cb.isNotNull(root.get(fieldName));
    }

    /**
     * Is true specification
     */
    public static <T> Specification<T> isTrue(String fieldName) {
        return (root, query, cb) -> cb.isTrue(root.get(fieldName));
    }

    /**
     * Is false specification
     */
    public static <T> Specification<T> isFalse(String fieldName) {
        return (root, query, cb) -> cb.isFalse(root.get(fieldName));
    }

    /**
     * Date between specification
     */
    public static <T> Specification<T> dateBetween(String fieldName, Instant start, Instant end) {
        return between(fieldName, start, end);
    }

    /**
     * Active entities (not deleted)
     */
    public static <T> Specification<T> isActive() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    /**
     * Deleted entities
     */
    public static <T> Specification<T> isDeleted() {
        return (root, query, cb) -> cb.isTrue(root.get("deleted"));
    }
}
