package com.neobrutalism.crm.common.specification;

import jakarta.persistence.criteria.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Generic specification for dynamic query building
 */
@Getter
@AllArgsConstructor
public class GenericSpecification<T> implements Specification<T> {

    private SearchCriteria criteria;

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        String operation = criteria.getOperation().toUpperCase();
        String key = criteria.getKey();
        Object value = criteria.getValue();

        return switch (operation) {
            case "EQUAL", ":" -> cb.equal(getPath(root, key), value);
            case "NOT_EQUAL", "!" -> cb.notEqual(getPath(root, key), value);
            case "LIKE", "~" -> cb.like(
                    cb.lower(getPath(root, key).as(String.class)),
                    "%" + value.toString().toLowerCase() + "%"
            );
            case "GREATER_THAN", ">" -> cb.greaterThan(getPath(root, key), (Comparable) value);
            case "GREATER_THAN_OR_EQUAL", ">=" -> cb.greaterThanOrEqualTo(getPath(root, key), (Comparable) value);
            case "LESS_THAN", "<" -> cb.lessThan(getPath(root, key), (Comparable) value);
            case "LESS_THAN_OR_EQUAL", "<=" -> cb.lessThanOrEqualTo(getPath(root, key), (Comparable) value);
            case "IN" -> getPath(root, key).in((List<?>) value);
            case "NOT_IN" -> cb.not(getPath(root, key).in((List<?>) value));
            case "IS_NULL" -> cb.isNull(getPath(root, key));
            case "IS_NOT_NULL" -> cb.isNotNull(getPath(root, key));
            case "IS_TRUE" -> cb.isTrue(getPath(root, key).as(Boolean.class));
            case "IS_FALSE" -> cb.isFalse(getPath(root, key).as(Boolean.class));
            default -> throw new IllegalArgumentException("Unsupported operation: " + operation);
        };
    }

    /**
     * Get path for nested properties (e.g., "address.city")
     */
    private Path<Comparable> getPath(Root<T> root, String key) {
        if (key.contains(".")) {
            String[] parts = key.split("\\.");
            Path<?> path = root;
            for (String part : parts) {
                path = path.get(part);
            }
            return (Path<Comparable>) path;
        }
        return root.get(key);
    }

    /**
     * Search criteria holder
     */
    @Getter
    @AllArgsConstructor
    public static class SearchCriteria {
        private String key;
        private String operation;
        private Object value;

        public static SearchCriteria of(String key, String operation, Object value) {
            return new SearchCriteria(key, operation, value);
        }
    }
}
