package com.neobrutalism.crm.domain.attachment.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.common.security.UserPrincipal;
import com.neobrutalism.crm.domain.attachment.dto.AttachmentResponse;
import com.neobrutalism.crm.domain.attachment.dto.AttachmentUploadRequest;
import com.neobrutalism.crm.domain.attachment.model.Attachment;
import com.neobrutalism.crm.domain.attachment.model.AttachmentType;
import com.neobrutalism.crm.domain.attachment.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for Attachment management
 */
@Slf4j
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "File attachment management APIs")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload file", description = "Upload a file and create attachment record")
    public ApiResponse<AttachmentResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("attachmentType") AttachmentType attachmentType,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") UUID entityId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "isPublic", required = false, defaultValue = "false") Boolean isPublic,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        log.info("Upload file request: {} for entity: {}/{}", file.getOriginalFilename(), entityType, entityId);

        AttachmentUploadRequest request = new AttachmentUploadRequest();
        request.setAttachmentType(attachmentType);
        request.setEntityType(entityType);
        request.setEntityId(entityId);
        request.setDescription(description);
        request.setTags(tags);
        request.setIsPublic(isPublic);

        Attachment attachment = attachmentService.uploadFile(file, request, userPrincipal.getId());
        String downloadUrl = attachmentService.generateDownloadUrl(attachment.getId());

        return ApiResponse.success("File uploaded successfully",
                AttachmentResponse.fromWithDownloadUrl(attachment, downloadUrl));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download file", description = "Download file by attachment ID")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable UUID id) {
        log.info("Download file request: {}", id);

        Attachment attachment = attachmentService.findById(id);
        InputStream fileStream = attachmentService.downloadFile(id);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + attachment.getOriginalFilename() + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, attachment.getContentType());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(attachment.getFileSize())
                .body(new InputStreamResource(fileStream));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "Get download URL", description = "Get presigned download URL")
    public ApiResponse<String> getDownloadUrl(
            @PathVariable UUID id,
            @RequestParam(value = "expiryMinutes", required = false, defaultValue = "15") int expiryMinutes
    ) {
        log.info("Get download URL request: {}", id);
        String url = attachmentService.generateDownloadUrl(id, expiryMinutes);
        return ApiResponse.success("Download URL generated", url);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get attachment by ID", description = "Retrieve attachment metadata by ID")
    public ApiResponse<AttachmentResponse> getAttachment(@PathVariable UUID id) {
        Attachment attachment = attachmentService.findById(id);
        String downloadUrl = attachmentService.generateDownloadUrl(id);
        return ApiResponse.success(AttachmentResponse.fromWithDownloadUrl(attachment, downloadUrl));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get attachments by entity", description = "Retrieve all attachments for an entity")
    public ApiResponse<List<AttachmentResponse>> getAttachmentsByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId
    ) {
        List<Attachment> attachments = attachmentService.findByEntity(entityType, entityId);
        List<AttachmentResponse> responses = attachments.stream()
                .map(a -> AttachmentResponse.fromWithDownloadUrl(a, attachmentService.generateDownloadUrl(a.getId())))
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/entity/{entityType}/{entityId}/type/{attachmentType}")
    @Operation(summary = "Get attachments by entity and type", description = "Retrieve attachments by entity and type")
    public ApiResponse<List<AttachmentResponse>> getAttachmentsByEntityAndType(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @PathVariable AttachmentType attachmentType
    ) {
        List<Attachment> attachments = attachmentService.findByEntityAndType(entityType, entityId, attachmentType);
        List<AttachmentResponse> responses = attachments.stream()
                .map(a -> AttachmentResponse.fromWithDownloadUrl(a, attachmentService.generateDownloadUrl(a.getId())))
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get attachments by user", description = "Retrieve all attachments uploaded by a user")
    public ApiResponse<List<AttachmentResponse>> getAttachmentsByUser(@PathVariable UUID userId) {
        List<Attachment> attachments = attachmentService.findByUploader(userId);
        List<AttachmentResponse> responses = attachments.stream()
                .map(AttachmentResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/public")
    @Operation(summary = "Get public attachments", description = "Retrieve all public attachments")
    public ApiResponse<List<AttachmentResponse>> getPublicAttachments() {
        List<Attachment> attachments = attachmentService.findPublicAttachments();
        List<AttachmentResponse> responses = attachments.stream()
                .map(a -> AttachmentResponse.fromWithDownloadUrl(a, attachmentService.generateDownloadUrl(a.getId())))
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping
    @Operation(summary = "Get all attachments", description = "Retrieve all attachments with pagination")
    public ApiResponse<Page<AttachmentResponse>> getAllAttachments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Attachment> attachmentPage = attachmentService.findAll(pageable);
        Page<AttachmentResponse> responsePage = attachmentPage.map(AttachmentResponse::from);
        return ApiResponse.success(responsePage);
    }

    @GetMapping("/search")
    @Operation(summary = "Search attachments", description = "Search attachments by filename")
    public ApiResponse<List<AttachmentResponse>> searchAttachments(@RequestParam String keyword) {
        List<Attachment> attachments = attachmentService.searchByFilename(keyword);
        List<AttachmentResponse> responses = attachments.stream()
                .map(AttachmentResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @GetMapping("/tag/{tag}")
    @Operation(summary = "Get attachments by tag", description = "Retrieve attachments by tag")
    public ApiResponse<List<AttachmentResponse>> getAttachmentsByTag(@PathVariable String tag) {
        List<Attachment> attachments = attachmentService.findByTag(tag);
        List<AttachmentResponse> responses = attachments.stream()
                .map(AttachmentResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(responses);
    }

    @PutMapping("/{id}/metadata")
    @Operation(summary = "Update attachment metadata", description = "Update attachment description, tags, and visibility")
    public ApiResponse<AttachmentResponse> updateMetadata(
            @PathVariable UUID id,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Boolean isPublic
    ) {
        Attachment updated = attachmentService.updateMetadata(id, description, tags, isPublic);
        return ApiResponse.success("Metadata updated successfully", AttachmentResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete attachment", description = "Soft delete attachment")
    public ApiResponse<Void> deleteAttachment(@PathVariable UUID id) {
        attachmentService.deleteAttachment(id);
        return ApiResponse.success("Attachment deleted successfully");
    }

    @DeleteMapping("/{id}/permanent")
    @Operation(summary = "Delete attachment permanently", description = "Permanently delete attachment and file")
    public ApiResponse<Void> deleteAttachmentPermanently(@PathVariable UUID id) {
        attachmentService.deleteAttachmentPermanently(id);
        return ApiResponse.success("Attachment permanently deleted");
    }

    @GetMapping("/user/{userId}/storage")
    @Operation(summary = "Get user storage usage", description = "Get total storage used by user")
    public ApiResponse<Long> getUserStorageUsage(@PathVariable UUID userId) {
        Long totalSize = attachmentService.getTotalStorageByUser(userId);
        return ApiResponse.success(totalSize);
    }

    @GetMapping("/user/{userId}/count")
    @Operation(summary = "Get user attachment count", description = "Get total number of attachments by user")
    public ApiResponse<Long> getUserAttachmentCount(@PathVariable UUID userId) {
        Long count = attachmentService.getAttachmentCountByUser(userId);
        return ApiResponse.success(count);
    }

    @GetMapping("/entity/{entityType}/{entityId}/count")
    @Operation(summary = "Get entity attachment count", description = "Get total number of attachments for entity")
    public ApiResponse<Long> getEntityAttachmentCount(
            @PathVariable String entityType,
            @PathVariable UUID entityId
    ) {
        Long count = attachmentService.getAttachmentCountByEntity(entityType, entityId);
        return ApiResponse.success(count);
    }
}
