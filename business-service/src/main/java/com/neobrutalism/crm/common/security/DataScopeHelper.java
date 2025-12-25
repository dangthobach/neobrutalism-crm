package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.common.specification.DataScopeSpecification;
import com.neobrutalism.crm.domain.user.model.DataScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;
import java.util.UUID;

/**
 * Utility helper for applying data scope filtering to JPA queries
 *
 * Usage in repository methods:
 * <pre>
 * default List<Customer> findAllWithScope(Pageable pageable) {
 *     return findAll(DataScopeHelper.applyDataScope(), pageable);
 * }
 * </pre>
 *
 * Data Scope Levels:
 * - ALL_BRANCHES: See all records in organization (Management role)
 * - CURRENT_BRANCH: See current branch + child branches (ORC role)
 * - SELF_ONLY: See only own created records (Maker/Checker role)
 */
@Slf4j
public class DataScopeHelper {

    /**
     * Apply data scope filtering based on current user's context
     *
     * @param <T> Entity type
     * @return Specification that filters by data scope
     */
    public static <T> Specification<T> applyDataScope() {
        DataScopeContext scopeInfo = DataScopeContext.get();

        if (scopeInfo == null) {
            log.warn("DataScopeContext not set - allowing all data access. " +
                    "This may indicate a security gap. Please ensure JwtAuthenticationFilter " +
                    "is properly populating the context.");
            return Specification.where(null); // No filtering (admin/system operation)
        }

        log.debug("Applying data scope filter: userId={}, scope={}, branchId={}, accessibleBranchIds={}",
                DataScopeContext.getCurrentUserId(),
                DataScopeContext.getCurrentDataScope(),
                DataScopeContext.getCurrentBranchId(),
                DataScopeContext.getAccessibleBranchIds() != null ? DataScopeContext.getAccessibleBranchIds().size() : 0);

        return DataScopeSpecification.create();
    }

    /**
     * Apply data scope filtering with additional condition
     * Combines data scope with another specification using AND logic
     *
     * @param <T> Entity type
     * @param additionalSpec Additional specification to combine
     * @return Combined specification
     */
    public static <T> Specification<T> applyScopeWith(Specification<T> additionalSpec) {
        Specification<T> dataScope = applyDataScope();
        if (additionalSpec == null) {
            return dataScope;
        }
        return dataScope.and(additionalSpec);
    }

    /**
     * Create a specification for organizationId filtering
     * Used in combination with data scope
     *
     * @param <T> Entity type
     * @param organizationId Organization ID to filter by
     * @return Specification for organization filter
     */
    public static <T> Specification<T> byOrganization(UUID organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null) {
                return criteriaBuilder.conjunction(); // No filter
            }
            try {
                return criteriaBuilder.equal(root.get("organizationId"), organizationId);
            } catch (IllegalArgumentException e) {
                log.warn("Entity {} does not have organizationId field, skipping organization filter",
                        root.getJavaType().getSimpleName());
                return criteriaBuilder.conjunction();
            }
        };
    }

    /**
     * Bypass data scope filtering (admin/system operations only)
     *
     * USE WITH EXTREME CAUTION!
     * Only use for:
     * - System operations (background jobs, migrations)
     * - Admin operations that explicitly need to see all data
     * - Reports/analytics that aggregate across all scopes
     *
     * @param <T> Entity type
     * @return Empty specification (no filtering)
     */
    public static <T> Specification<T> bypassDataScope() {
        log.warn("Data scope bypassed! This should only be used for system/admin operations. " +
                "Stack trace: ", new Exception("Data scope bypass"));
        return Specification.where(null);
    }

    /**
     * Check if current user has ALL_BRANCHES scope
     *
     * @return true if user can see all data
     */
    public static boolean hasAllBranchesAccess() {
        DataScopeContext scopeInfo = DataScopeContext.get();
        return scopeInfo != null && DataScopeContext.getCurrentDataScope() == DataScope.ALL_BRANCHES;
    }

    /**
     * Check if current user has CURRENT_BRANCH scope
     *
     * @return true if user can see branch hierarchy
     */
    public static boolean hasCurrentBranchAccess() {
        DataScopeContext scopeInfo = DataScopeContext.get();
        return scopeInfo != null && DataScopeContext.getCurrentDataScope() == DataScope.CURRENT_BRANCH;
    }

    /**
     * Check if current user has SELF_ONLY scope
     *
     * @return true if user can only see own records
     */
    public static boolean hasSelfOnlyAccess() {
        DataScopeContext scopeInfo = DataScopeContext.get();
        return scopeInfo != null && DataScopeContext.getCurrentDataScope() == DataScope.SELF_ONLY;
    }

    /**
     * Get current user's accessible branch IDs
     *
     * @return Set of branch IDs the user can access
     */
    public static Set<UUID> getAccessibleBranchIds() {
        return DataScopeContext.getAccessibleBranchIds();
    }

    /**
     * Get current user's branch ID
     *
     * @return User's branch ID
     */
    public static UUID getCurrentBranchId() {
        return DataScopeContext.getCurrentBranchId();
    }

    /**
     * Get current user's ID
     *
     * @return User ID
     */
    public static UUID getCurrentUserId() {
        return DataScopeContext.getCurrentUserId();
    }

    /**
     * Check if data scope context is populated
     *
     * @return true if context is set
     */
    public static boolean isContextPopulated() {
        return DataScopeContext.hasContext();
    }

    /**
     * Get debug information about current data scope context
     *
     * @return Debug string
     */
    public static String getDebugInfo() {
        DataScopeContext scopeInfo = DataScopeContext.get();
        if (scopeInfo == null) {
            return "DataScopeContext: NOT SET";
        }
        return String.format("DataScopeContext: userId=%s, scope=%s, branchId=%s, accessibleBranches=%d, tenantId=%s",
                DataScopeContext.getCurrentUserId(),
                DataScopeContext.getCurrentDataScope(),
                DataScopeContext.getCurrentBranchId(),
                DataScopeContext.getAccessibleBranchIds() != null ? DataScopeContext.getAccessibleBranchIds().size() : 0,
                DataScopeContext.getCurrentTenantId());
    }
}
