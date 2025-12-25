package com.neobrutalism.crm.domain.user.specification;

import com.neobrutalism.crm.domain.user.dto.UserSearchRequest;
import com.neobrutalism.crm.domain.user.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification for User entity dynamic queries
 */
public class UserSpecification {

    /**
     * Build specification from search request
     */
    public static Specification<User> fromSearchRequest(UserSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword search (username, email, firstName, lastName)
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), keyword)
                );
                predicates.add(keywordPredicate);
            }

            // Username filter
            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("username")),
                        "%" + request.getUsername().toLowerCase() + "%"
                ));
            }

            // Email filter
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + request.getEmail().toLowerCase() + "%"
                ));
            }

            // First name filter
            if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("firstName")),
                        "%" + request.getFirstName().toLowerCase() + "%"
                ));
            }

            // Last name filter
            if (request.getLastName() != null && !request.getLastName().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("lastName")),
                        "%" + request.getLastName().toLowerCase() + "%"
                ));
            }

            // Organization filter
            if (request.getOrganizationId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("organizationId"), request.getOrganizationId()));
            }

            // Status filter
            if (request.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), request.getStatus()));
            }

            // Tenant filter
            if (request.getTenantId() != null && !request.getTenantId().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("tenantId"), request.getTenantId()));
            }

            // Deleted filter
            if (request.getIncludeDeleted() == null || !request.getIncludeDeleted()) {
                predicates.add(criteriaBuilder.equal(root.get("deleted"), false));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
