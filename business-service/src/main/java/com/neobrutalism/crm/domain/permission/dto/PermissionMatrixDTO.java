package com.neobrutalism.crm.domain.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Permission Matrix Data Transfer Object
 *
 * Represents a matrix view of permissions showing which roles have access to which resources.
 * Used by the Permission Matrix UI for displaying and editing permissions.
 *
 * Structure:
 * - Rows: Roles
 * - Columns: Resources
 * - Cells: Permissions (READ, WRITE, DELETE, etc.)
 *
 * @author Neobrutalism CRM Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionMatrixDTO {

    /**
     * Organization/Tenant ID
     */
    private String tenantId;

    /**
     * List of roles in the matrix
     */
    private List<RoleInfo> roles;

    /**
     * List of resources in the matrix
     */
    private List<ResourceInfo> resources;

    /**
     * Permission matrix data
     * Map<roleCode, Map<resource, List<action>>>
     *
     * Example:
     * {
     *   "ROLE_ADMIN": {
     *     "/api/users": ["GET", "POST", "PUT", "DELETE"],
     *     "/api/tasks": ["GET", "POST"]
     *   },
     *   "ROLE_USER": {
     *     "/api/tasks": ["GET"]
     *   }
     * }
     */
    private Map<String, Map<String, List<String>>> matrix;

    /**
     * Role hierarchy information
     * Map<childRole, List<parentRole>>
     */
    private Map<String, List<String>> hierarchy;

    /**
     * Role information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        private String roleCode;
        private String roleName;
        private String description;
        private Integer priority;
        private String scope;
        private boolean inherited;
    }

    /**
     * Resource information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceInfo {
        private String path;
        private String name;
        private String category;
        private List<String> availableActions;
    }

    /**
     * Permission cell update request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionUpdate {
        private String roleCode;
        private String resource;
        private List<String> actions;
        private String reason;
    }

    /**
     * Bulk permission update request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkPermissionUpdate {
        private String tenantId;
        private List<PermissionUpdate> updates;
        private String reason;
    }
}
