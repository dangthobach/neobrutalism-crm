package com.neobrutalism.crm.domain.course.controller;

import com.neobrutalism.crm.domain.course.dto.EnrollmentDTO;
import com.neobrutalism.crm.domain.course.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for student enrollment management
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Student enrollment and progress tracking APIs")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * Get enrollment by ID
     */
    @GetMapping("/{enrollmentId}")
    @Operation(summary = "Get enrollment details", description = "Get detailed enrollment information")
    public ResponseEntity<EnrollmentDTO> getEnrollmentById(@PathVariable UUID enrollmentId) {
        EnrollmentDTO enrollment = enrollmentService.getEnrollmentById(enrollmentId);
        return ResponseEntity.ok(enrollment);
    }

    /**
     * Get my enrollments
     */
    @GetMapping("/my")
    @Operation(summary = "Get my enrollments", description = "Get all enrollments for current user")
    public ResponseEntity<Page<EnrollmentDTO>> getMyEnrollments(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        Page<EnrollmentDTO> enrollments = enrollmentService.getUserEnrollments(userId, pageable);
        return ResponseEntity.ok(enrollments);
    }

    /**
     * Get my active enrollments
     */
    @GetMapping("/my/active")
    @Operation(summary = "Get active enrollments", description = "Get all active enrollments for current user")
    public ResponseEntity<List<EnrollmentDTO>> getMyActiveEnrollments(
            @RequestHeader("X-User-Id") UUID userId) {
        List<EnrollmentDTO> enrollments = enrollmentService.getActiveEnrollments(userId);
        return ResponseEntity.ok(enrollments);
    }

    /**
     * Update enrollment progress
     */
    @PutMapping("/{enrollmentId}/progress")
    @Operation(summary = "Update progress", description = "Update enrollment progress percentage")
    public ResponseEntity<EnrollmentDTO> updateProgress(
            @PathVariable UUID enrollmentId,
            @RequestParam Integer progressPercentage) {
        EnrollmentDTO enrollment = enrollmentService.updateProgress(enrollmentId, progressPercentage);
        return ResponseEntity.ok(enrollment);
    }

    /**
     * Recalculate enrollment progress
     */
    @PostMapping("/{enrollmentId}/recalculate-progress")
    @Operation(summary = "Recalculate progress", description = "Recalculate enrollment progress based on lesson completion")
    public ResponseEntity<EnrollmentDTO> recalculateProgress(@PathVariable UUID enrollmentId) {
        EnrollmentDTO enrollment = enrollmentService.recalculateProgress(enrollmentId);
        return ResponseEntity.ok(enrollment);
    }

    /**
     * Drop enrollment
     */
    @PostMapping("/{enrollmentId}/drop")
    @Operation(summary = "Drop enrollment", description = "Drop/cancel a course enrollment")
    public ResponseEntity<EnrollmentDTO> dropEnrollment(@PathVariable UUID enrollmentId) {
        EnrollmentDTO enrollment = enrollmentService.dropEnrollment(enrollmentId);
        return ResponseEntity.ok(enrollment);
    }
}
