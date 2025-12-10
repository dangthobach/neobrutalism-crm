package com.neobrutalism.crm.domain.task.model;

import com.neobrutalism.crm.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Task Activity entity - stores activity timeline entries for tasks
 * Tracks all events related to a task (status changes, assignments, comments, etc.)
 */
@Entity
@Table(name = "task_activities",
        indexes = {
                @Index(name = "idx_task_activity_task_id", columnList = "task_id"),
                @Index(name = "idx_task_activity_user_id", columnList = "user_id"),
                @Index(name = "idx_task_activity_created_at", columnList = "created_at"),
                @Index(name = "idx_task_activity_type", columnList = "activity_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskActivity extends AuditableEntity {

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType; // CREATED, STATUS_CHANGED, ASSIGNED, COMMENT_ADDED, CHECKLIST_UPDATED, ATTACHMENT_ADDED

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "metadata", columnDefinition = "TEXT")
    @Convert(converter = MetadataConverter.class)
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /**
     * JPA converter for storing metadata as JSON
     */
    @Converter
    public static class MetadataConverter implements AttributeConverter<Map<String, Object>, String> {

        @Override
        public String convertToDatabaseColumn(Map<String, Object> attribute) {
            if (attribute == null || attribute.isEmpty()) {
                return null;
            }
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(attribute);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert metadata to JSON", e);
            }
        }

        @Override
        public Map<String, Object> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isEmpty()) {
                return new HashMap<>();
            }
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(dbData, Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert JSON to metadata", e);
            }
        }
    }

    // Builder methods for fluent API

    public static TaskActivity create(UUID taskId, String activityType, String description, 
                                     UUID userId, String username, UUID organizationId) {
        TaskActivity activity = new TaskActivity();
        activity.setTaskId(taskId);
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setUserId(userId);
        activity.setUsername(username);
        activity.setOrganizationId(organizationId);
        return activity;
    }

    public TaskActivity withMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    public TaskActivity withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }
}
