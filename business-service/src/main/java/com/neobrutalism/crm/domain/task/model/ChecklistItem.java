package com.neobrutalism.crm.domain.task.model;

import com.neobrutalism.crm.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Checklist item entity for task checklists
 * Supports ordering and progress tracking
 */
@Entity
@Table(
    name = "checklist_items",
    indexes = {
        @Index(name = "idx_checklist_task", columnList = "task_id"),
        @Index(name = "idx_checklist_position", columnList = "task_id, position")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ChecklistItem extends TenantAwareEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @NotBlank
    @Size(max = 500)
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "position", nullable = false)
    private Integer position = 0;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * Toggle completion status
     */
    public void toggleCompleted() {
        this.completed = !this.completed;
    }

    /**
     * Mark as completed
     */
    public void markCompleted() {
        this.completed = true;
    }

    /**
     * Mark as incomplete
     */
    public void markIncomplete() {
        this.completed = false;
    }

    /**
     * Soft delete
     */
    public void softDelete() {
        this.deleted = true;
    }
}
