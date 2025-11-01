package com.neobrutalism.crm.domain.attachment.dto;

import com.neobrutalism.crm.domain.attachment.model.Attachment;
import com.neobrutalism.crm.domain.attachment.model.AttachmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Attachment Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private UUID id;
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private Long fileSize;
    private String humanReadableSize;
    private String contentType;
    private String fileExtension;
    private AttachmentType attachmentType;
    private String entityType;
    private UUID entityId;
    private UUID uploadedBy;
    private String description;
    private String tags;
    private Boolean isPublic;
    private Integer downloadCount;
    private String downloadUrl;
    private Instant createdAt;
    private String createdBy;

    public static AttachmentResponse from(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .originalFilename(attachment.getOriginalFilename())
                .storedFilename(attachment.getStoredFilename())
                .filePath(attachment.getFilePath())
                .fileSize(attachment.getFileSize())
                .humanReadableSize(attachment.getHumanReadableSize())
                .contentType(attachment.getContentType())
                .fileExtension(attachment.getFileExtension())
                .attachmentType(attachment.getAttachmentType())
                .entityType(attachment.getEntityType())
                .entityId(attachment.getEntityId())
                .uploadedBy(attachment.getUploadedBy())
                .description(attachment.getDescription())
                .tags(attachment.getTags())
                .isPublic(attachment.getIsPublic())
                .downloadCount(attachment.getDownloadCount())
                .createdAt(attachment.getCreatedAt())
                .createdBy(attachment.getCreatedBy())
                .build();
    }

    public static AttachmentResponse fromWithDownloadUrl(Attachment attachment, String downloadUrl) {
        AttachmentResponse response = from(attachment);
        response.setDownloadUrl(downloadUrl);
        return response;
    }
}
