package com.neobrutalism.crm.domain.attachment.model;

import com.neobrutalism.crm.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Attachment entity for file management
 */
@Getter
@Setter
@Entity
@Table(
    name = "attachments",
    indexes = {
        @Index(name = "idx_attachment_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_attachment_type", columnList = "attachment_type"),
        @Index(name = "idx_attachment_uploader", columnList = "uploaded_by"),
        @Index(name = "idx_attachment_filename", columnList = "original_filename"),
        @Index(name = "idx_attachment_deleted", columnList = "deleted")
    }
)
public class Attachment extends AuditableEntity {

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true, length = 500)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "minio_bucket", nullable = false, length = 100)
    private String minioBucket;

    @Column(name = "minio_object_name", nullable = false, length = 500)
    private String minioObjectName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 50)
    private AttachmentType attachmentType;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @Column(name = "checksum", length = 100)
    private String checksum;

    /**
     * Increment download count
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    /**
     * Get human-readable file size
     */
    public String getHumanReadableSize() {
        if (fileSize == null) return "0 B";

        long size = fileSize;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024.0));
        return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
    }
}
