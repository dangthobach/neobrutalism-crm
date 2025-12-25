package com.neobrutalism.crm.domain.attachment.dto;

import com.neobrutalism.crm.domain.attachment.model.AttachmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Attachment Upload Request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUploadRequest {

    @NotNull(message = "Attachment type is required")
    private AttachmentType attachmentType;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    private String description;

    private String tags;

    private Boolean isPublic = false;
}
