package com.neobrutalism.crm.domain.attachment.service;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import com.neobrutalism.crm.common.storage.FileStorageService;
import com.neobrutalism.crm.domain.attachment.dto.AttachmentUploadRequest;
import com.neobrutalism.crm.domain.attachment.model.Attachment;
import com.neobrutalism.crm.domain.attachment.model.AttachmentType;
import com.neobrutalism.crm.domain.attachment.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing attachments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;

    @Value("${minio.default-bucket:crm-files}")
    private String defaultBucket;

    @Value("${file.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;

    /**
     * Upload file and create attachment record
     */
    @Transactional
    public Attachment uploadFile(
            MultipartFile file,
            AttachmentUploadRequest request,
            UUID uploadedBy
    ) {
        log.info("Uploading file: {} for entity: {}/{}", file.getOriginalFilename(), request.getEntityType(), request.getEntityId());

        // Validate file
        validateFile(file);

        // Generate storage filename
        String storedFilename = generateStoredFilename(file.getOriginalFilename());
        String objectName = buildObjectName(request.getEntityType(), request.getEntityId(), storedFilename);

        // Upload to MinIO
        fileStorageService.uploadFile(file, defaultBucket, objectName);

        // Create attachment record
        Attachment attachment = new Attachment();
        attachment.setOriginalFilename(file.getOriginalFilename());
        attachment.setStoredFilename(storedFilename);
        attachment.setFilePath(objectName);
        attachment.setMinioBucket(defaultBucket);
        attachment.setMinioObjectName(objectName);
        attachment.setFileSize(file.getSize());
        attachment.setContentType(file.getContentType());
        attachment.setFileExtension(getFileExtension(file.getOriginalFilename()));
        attachment.setAttachmentType(request.getAttachmentType());
        attachment.setEntityType(request.getEntityType());
        attachment.setEntityId(request.getEntityId());
        attachment.setUploadedBy(uploadedBy);
        attachment.setDescription(request.getDescription());
        attachment.setTags(request.getTags());
        attachment.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);

        Attachment saved = attachmentRepository.save(attachment);
        log.info("File uploaded successfully: {}", saved.getId());

        return saved;
    }

    /**
     * Download file
     */
    @Transactional
    public InputStream downloadFile(UUID attachmentId) {
        Attachment attachment = findById(attachmentId);

        // Increment download count
        attachment.incrementDownloadCount();
        attachmentRepository.save(attachment);

        // Get file from MinIO
        return fileStorageService.downloadFile(attachment.getMinioBucket(), attachment.getMinioObjectName());
    }

    /**
     * Get attachment by ID
     */
    public Attachment findById(UUID id) {
        return attachmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Attachment not found"));
    }

    /**
     * Get all attachments for an entity
     */
    public List<Attachment> findByEntity(String entityType, UUID entityId) {
        return attachmentRepository.findByEntityTypeAndEntityIdAndDeletedFalse(entityType, entityId);
    }

    /**
     * Get attachments by entity and type
     */
    public List<Attachment> findByEntityAndType(String entityType, UUID entityId, AttachmentType attachmentType) {
        return attachmentRepository.findByEntityTypeAndEntityIdAndAttachmentTypeAndDeletedFalse(
                entityType, entityId, attachmentType);
    }

    /**
     * Get all attachments uploaded by user
     */
    public List<Attachment> findByUploader(UUID uploaderId) {
        return attachmentRepository.findByUploadedByAndDeletedFalse(uploaderId);
    }

    /**
     * Get all attachments with pagination
     */
    public Page<Attachment> findAll(Pageable pageable) {
        return attachmentRepository.findByDeletedFalse(pageable);
    }

    /**
     * Search attachments by filename
     */
    public List<Attachment> searchByFilename(String keyword) {
        return attachmentRepository.searchByFilename(keyword);
    }

    /**
     * Find attachments by tag
     */
    public List<Attachment> findByTag(String tag) {
        return attachmentRepository.findByTag(tag);
    }

    /**
     * Get public attachments
     */
    public List<Attachment> findPublicAttachments() {
        return attachmentRepository.findPublicAttachments();
    }

    /**
     * Delete attachment (soft delete)
     */
    @Transactional
    public void deleteAttachment(UUID id) {
        Attachment attachment = findById(id);
        attachment.setDeleted(true);
        attachmentRepository.save(attachment);
        log.info("Attachment soft deleted: {}", id);
    }

    /**
     * Delete attachment permanently (hard delete)
     */
    @Transactional
    public void deleteAttachmentPermanently(UUID id) {
        Attachment attachment = findById(id);

        // Delete from MinIO
        try {
            fileStorageService.deleteFile(attachment.getMinioBucket(), attachment.getMinioObjectName());
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", e.getMessage());
        }

        // Delete from database
        attachmentRepository.delete(attachment);
        log.info("Attachment permanently deleted: {}", id);
    }

    /**
     * Update attachment metadata
     */
    @Transactional
    public Attachment updateMetadata(UUID id, String description, String tags, Boolean isPublic) {
        Attachment attachment = findById(id);

        if (description != null) {
            attachment.setDescription(description);
        }
        if (tags != null) {
            attachment.setTags(tags);
        }
        if (isPublic != null) {
            attachment.setIsPublic(isPublic);
        }

        return attachmentRepository.save(attachment);
    }

    /**
     * Generate presigned download URL
     */
    public String generateDownloadUrl(UUID id, int expiryMinutes) {
        Attachment attachment = findById(id);
        return fileStorageService.generatePresignedUrl(
                attachment.getMinioBucket(),
                attachment.getMinioObjectName(),
                expiryMinutes
        );
    }

    /**
     * Generate presigned download URL with default expiry (15 minutes)
     */
    public String generateDownloadUrl(UUID id) {
        return generateDownloadUrl(id, 15);
    }

    /**
     * Get total storage used by user
     */
    public Long getTotalStorageByUser(UUID userId) {
        Long total = attachmentRepository.getTotalSizeByUser(userId);
        return total != null ? total : 0L;
    }

    /**
     * Get attachment count by user
     */
    public Long getAttachmentCountByUser(UUID userId) {
        Long count = attachmentRepository.countByUser(userId);
        return count != null ? count : 0L;
    }

    /**
     * Get attachment count by entity
     */
    public Long getAttachmentCountByEntity(String entityType, UUID entityId) {
        Long count = attachmentRepository.countByEntity(entityType, entityId);
        return count != null ? count : 0L;
    }

    /**
     * Validate file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "File is required");
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    String.format("File size exceeds maximum allowed size: %d bytes", maxFileSize));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Filename is required");
        }
    }

    /**
     * Generate stored filename
     */
    private String generateStoredFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
    }

    /**
     * Build object name with path
     */
    private String buildObjectName(String entityType, UUID entityId, String storedFilename) {
        return String.format("%s/%s/%s", entityType.toLowerCase(), entityId.toString(), storedFilename);
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
