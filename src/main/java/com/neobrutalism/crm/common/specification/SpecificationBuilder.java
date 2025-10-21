package com.neobrutalism.crm.common.specification;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent API for building complex specifications
 */
public class SpecificationBuilder<T> {

    private final List<Specification<T>> specifications = new ArrayList<>();

    public static <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }

    /**
     * Add a specification
     */
    public SpecificationBuilder<T> with(Specification<T> specification) {
        if (specification != null) {
            specifications.add(specification);
        }
        return this;
    }

    /**
     * Add equal specification
     */
    public SpecificationBuilder<T> equal(String field, Object value) {
        if (value != null) {
            specifications.add(BaseSpecification.equal(field, value));
        }
        return this;
    }

    /**
     * Add not equal specification
     */
    public SpecificationBuilder<T> notEqual(String field, Object value) {
        if (value != null) {
            specifications.add(BaseSpecification.notEqual(field, value));
        }
        return this;
    }

    /**
     * Add like specification
     */
    public SpecificationBuilder<T> like(String field, String value) {
        if (value != null && !value.isEmpty()) {
            specifications.add(BaseSpecification.like(field, value));
        }
        return this;
    }

    /**
     * Add in specification
     */
    public SpecificationBuilder<T> in(String field, List<?> values) {
        if (values != null && !values.isEmpty()) {
            specifications.add(BaseSpecification.in(field, values));
        }
        return this;
    }

    /**
     * Add greater than specification
     */
    public <V extends Comparable<V>> SpecificationBuilder<T> greaterThan(String field, V value) {
        if (value != null) {
            specifications.add(BaseSpecification.greaterThan(field, value));
        }
        return this;
    }

    /**
     * Add greater than or equal specification
     */
    public <V extends Comparable<V>> SpecificationBuilder<T> greaterThanOrEqual(String field, V value) {
        if (value != null) {
            specifications.add(BaseSpecification.greaterThanOrEqual(field, value));
        }
        return this;
    }

    /**
     * Add less than specification
     */
    public <V extends Comparable<V>> SpecificationBuilder<T> lessThan(String field, V value) {
        if (value != null) {
            specifications.add(BaseSpecification.lessThan(field, value));
        }
        return this;
    }

    /**
     * Add less than or equal specification
     */
    public <V extends Comparable<V>> SpecificationBuilder<T> lessThanOrEqual(String field, V value) {
        if (value != null) {
            specifications.add(BaseSpecification.lessThanOrEqual(field, value));
        }
        return this;
    }

    /**
     * Add between specification
     */
    public <V extends Comparable<V>> SpecificationBuilder<T> between(String field, V start, V end) {
        if (start != null || end != null) {
            specifications.add(BaseSpecification.between(field, start, end));
        }
        return this;
    }

    /**
     * Add date between specification
     */
    public SpecificationBuilder<T> dateBetween(String field, Instant start, Instant end) {
        if (start != null || end != null) {
            specifications.add(BaseSpecification.dateBetween(field, start, end));
        }
        return this;
    }

    /**
     * Add is null specification
     */
    public SpecificationBuilder<T> isNull(String field) {
        specifications.add(BaseSpecification.isNull(field));
        return this;
    }

    /**
     * Add is not null specification
     */
    public SpecificationBuilder<T> isNotNull(String field) {
        specifications.add(BaseSpecification.isNotNull(field));
        return this;
    }

    /**
     * Add is true specification
     */
    public SpecificationBuilder<T> isTrue(String field) {
        specifications.add(BaseSpecification.isTrue(field));
        return this;
    }

    /**
     * Add is false specification
     */
    public SpecificationBuilder<T> isFalse(String field) {
        specifications.add(BaseSpecification.isFalse(field));
        return this;
    }

    /**
     * Add active entities specification
     */
    public SpecificationBuilder<T> isActive() {
        specifications.add(BaseSpecification.isActive());
        return this;
    }

    /**
     * Add deleted entities specification
     */
    public SpecificationBuilder<T> isDeleted() {
        specifications.add(BaseSpecification.isDeleted());
        return this;
    }

    /**
     * Build with AND logic
     */
    public Specification<T> buildAnd() {
        return BaseSpecification.and(specifications);
    }

    /**
     * Build with OR logic
     */
    public Specification<T> buildOr() {
        return BaseSpecification.or(specifications);
    }

    /**
     * Build (default is AND)
     */
    public Specification<T> build() {
        return buildAnd();
    }
}
