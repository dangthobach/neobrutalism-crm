package com.neobrutalism.crm.domain.attachment.repository;

import com.neobrutalism.crm.domain.attachment.model.Attachment;
import com.neobrutalism.crm.domain.attachment.model.AttachmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Attachment
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    Optional<Attachment> findByIdAndDeletedFalse(UUID id);

    Optional<Attachment> findByStoredFilenameAndDeletedFalse(String storedFilename);

    List<Attachment> findByEntityTypeAndEntityIdAndDeletedFalse(String entityType, UUID entityId);

    List<Attachment> findByEntityTypeAndEntityIdAndAttachmentTypeAndDeletedFalse(
            String entityType, UUID entityId, AttachmentType attachmentType);

    List<Attachment> findByUploadedByAndDeletedFalse(UUID uploadedBy);

    List<Attachment> findByAttachmentTypeAndDeletedFalse(AttachmentType attachmentType);

    Page<Attachment> findByDeletedFalse(Pageable pageable);

    @Query("SELECT a FROM Attachment a WHERE a.entityType = :entityType AND a.entityId = :entityId AND a.deleted = false ORDER BY a.createdAt DESC")
    List<Attachment> findByEntityOrderByCreatedAtDesc(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    @Query("SELECT a FROM Attachment a WHERE a.originalFilename LIKE %:keyword% AND a.deleted = false")
    List<Attachment> searchByFilename(@Param("keyword") String keyword);

    @Query("SELECT a FROM Attachment a WHERE a.tags LIKE %:tag% AND a.deleted = false")
    List<Attachment> findByTag(@Param("tag") String tag);

    @Query("SELECT SUM(a.fileSize) FROM Attachment a WHERE a.uploadedBy = :userId AND a.deleted = false")
    Long getTotalSizeByUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(a) FROM Attachment a WHERE a.uploadedBy = :userId AND a.deleted = false")
    Long countByUser(@Param("userId") UUID userId);

    @Query("SELECT a FROM Attachment a WHERE a.isPublic = true AND a.deleted = false")
    List<Attachment> findPublicAttachments();

    @Query("SELECT COUNT(a) FROM Attachment a WHERE a.entityType = :entityType AND a.entityId = :entityId AND a.deleted = false")
    Long countByEntity(@Param("entityType") String entityType, @Param("entityId") UUID entityId);
}
