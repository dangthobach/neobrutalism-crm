package com.neobrutalism.crm.domain.command.model;

import com.neobrutalism.crm.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Aggregate Root for Command Palette functionality.
 *
 * Represents a user action that can be invoked via keyboard shortcuts
 * or command palette search.
 *
 * @author Admin
 * @version 1.0
 * @since Phase 1
 */
@Entity
@Table(
    name = "commands",
    indexes = {
        @Index(name = "idx_command_tenant_category", columnList = "tenant_id, category"),
        @Index(name = "idx_command_shortcut", columnList = "shortcut_key"),
        @Index(name = "idx_command_active", columnList = "is_active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Command extends TenantAwareEntity {

    @Column(name = "command_id", nullable = false, unique = true, length = 100)
    private String commandId; // e.g., "customer.create", "task.assign"

    @Column(name = "label", nullable = false, length = 255)
    private String label; // User-visible label

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private CommandCategory category;

    @Column(name = "icon", length = 100)
    private String icon; // Icon name from icon library

    @Column(name = "shortcut_key", length = 50)
    private String shortcutKey; // e.g., "Ctrl+Shift+N"

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // NAVIGATION, API_CALL, MODAL, EXTERNAL

    @Column(name = "action_payload", columnDefinition = "TEXT")
    private String actionPayload; // JSON payload for action

    @Column(name = "required_permission", length = 100)
    private String requiredPermission; // Casbin permission code

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "execution_count", nullable = false)
    @Builder.Default
    private Long executionCount = 0L;

    @Column(name = "avg_execution_time_ms")
    private Long avgExecutionTimeMs;

    @Column(name = "search_keywords", columnDefinition = "TEXT")
    private String searchKeywords; // Space-separated keywords for search

    /**
     * Increment execution count and update average execution time.
     */
    public void recordExecution(long executionTimeMs) {
        if (this.executionCount == null) {
            this.executionCount = 0L;
        }
        if (this.avgExecutionTimeMs == null) {
            this.avgExecutionTimeMs = 0L;
        }

        // Running average calculation
        long totalTime = this.avgExecutionTimeMs * this.executionCount;
        this.executionCount++;
        this.avgExecutionTimeMs = (totalTime + executionTimeMs) / this.executionCount;
    }

    /**
     * Check if user has permission to execute this command.
     */
    public boolean isPermittedFor(String userId, String permission) {
        if (requiredPermission == null || requiredPermission.isBlank()) {
            return true; // No permission required
        }
        return requiredPermission.equals(permission);
    }

    /**
     * Check if command matches search query.
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String lowerQuery = query.toLowerCase();
        return label.toLowerCase().contains(lowerQuery) ||
               commandId.toLowerCase().contains(lowerQuery) ||
               (description != null && description.toLowerCase().contains(lowerQuery)) ||
               (searchKeywords != null && searchKeywords.toLowerCase().contains(lowerQuery));
    }
}
