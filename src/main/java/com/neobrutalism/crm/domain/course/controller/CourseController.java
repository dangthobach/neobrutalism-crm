package com.neobrutalism.crm.domain.course.controller;

import com.neobrutalism.crm.common.enums.MemberTier;
import com.neobrutalism.crm.domain.course.dto.CourseDTO;
import com.neobrutalism.crm.domain.course.dto.CreateCourseRequest;
import com.neobrutalism.crm.domain.course.dto.EnrollmentDTO;
import com.neobrutalism.crm.domain.course.service.CourseService;
import com.neobrutalism.crm.domain.course.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST Controller for Course management and enrollment
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management and enrollment APIs")
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    /**
     * Create a new course
     */
    @PostMapping
    @Operation(summary = "Create a new course", description = "Create a new course (instructor/admin only)")
    public ResponseEntity<CourseDTO> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            Authentication authentication) {

        String createdBy = authentication.getName();
        CourseDTO course = courseService.createCourse(request, tenantId, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }

    /**
     * Get course by ID
     */
    @GetMapping("/{courseId}")
    @Operation(summary = "Get course details", description = "Get detailed information about a course")
    public ResponseEntity<CourseDTO> getCourseById(@PathVariable UUID courseId) {
        CourseDTO course = courseService.getCourseById(courseId);
        return ResponseEntity.ok(course);
    }

    /**
     * Get course by slug
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get course by slug", description = "Get course details by URL slug")
    public ResponseEntity<CourseDTO> getCourseBySlug(@PathVariable String slug) {
        CourseDTO course = courseService.getCourseBySlug(slug);
        return ResponseEntity.ok(course);
    }

    /**
     * Get all published courses
     */
    @GetMapping
    @Operation(summary = "Get published courses", description = "Get all published courses with pagination")
    public ResponseEntity<Page<CourseDTO>> getPublishedCourses(Pageable pageable) {
        Page<CourseDTO> courses = courseService.getPublishedCourses(pageable);
        return ResponseEntity.ok(courses);
    }

    /**
     * Search courses
     */
    @GetMapping("/search")
    @Operation(summary = "Search courses", description = "Search courses by keyword")
    public ResponseEntity<Page<CourseDTO>> searchCourses(
            @RequestParam String keyword,
            Pageable pageable) {
        Page<CourseDTO> courses = courseService.searchCourses(keyword, pageable);
        return ResponseEntity.ok(courses);
    }

    /**
     * Get courses by instructor
     */
    @GetMapping("/instructor/{instructorId}")
    @Operation(summary = "Get courses by instructor", description = "Get all courses created by an instructor")
    public ResponseEntity<Page<CourseDTO>> getCoursesByInstructor(
            @PathVariable UUID instructorId,
            Pageable pageable) {
        Page<CourseDTO> courses = courseService.getCoursesByInstructor(instructorId, pageable);
        return ResponseEntity.ok(courses);
    }

    /**
     * Get courses for user's tier
     */
    @GetMapping("/my-tier")
    @Operation(summary = "Get courses for my tier", description = "Get courses accessible with current user's membership tier")
    public ResponseEntity<Page<CourseDTO>> getCoursesForMyTier(
            @RequestParam(required = false, defaultValue = "FREE") MemberTier tier,
            Pageable pageable) {
        Page<CourseDTO> courses = courseService.getCoursesForUserTier(tier, pageable);
        return ResponseEntity.ok(courses);
    }

    /**
     * Publish a course
     */
    @PostMapping("/{courseId}/publish")
    @Operation(summary = "Publish course", description = "Publish a course to make it available to students")
    public ResponseEntity<CourseDTO> publishCourse(
            @PathVariable UUID courseId,
            Authentication authentication) {
        String publishedBy = authentication.getName();
        CourseDTO course = courseService.publishCourse(courseId, publishedBy);
        return ResponseEntity.ok(course);
    }

    /**
     * Enroll in a course
     */
    @PostMapping("/{courseId}/enroll")
    @Operation(summary = "Enroll in course", description = "Enroll current user in a course")
    public ResponseEntity<EnrollmentDTO> enrollInCourse(
            @PathVariable UUID courseId,
            @RequestParam(required = false) BigDecimal pricePaid,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestHeader("X-User-Id") UUID userId,
            Authentication authentication) {

        String enrolledBy = authentication.getName();
        EnrollmentDTO enrollment = enrollmentService.enrollUserInCourse(
            userId,
            courseId,
            pricePaid,
            tenantId,
            enrolledBy
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    /**
     * Check if user is enrolled
     */
    @GetMapping("/{courseId}/enrollment/check")
    @Operation(summary = "Check enrollment", description = "Check if user is enrolled in a course")
    public ResponseEntity<Boolean> checkEnrollment(
            @PathVariable UUID courseId,
            @RequestHeader("X-User-Id") UUID userId) {
        boolean isEnrolled = enrollmentService.isUserEnrolled(userId, courseId);
        return ResponseEntity.ok(isEnrolled);
    }

    /**
     * Get user's enrollment for a course
     */
    @GetMapping("/{courseId}/enrollment")
    @Operation(summary = "Get enrollment", description = "Get user's enrollment details for a course")
    public ResponseEntity<EnrollmentDTO> getEnrollment(
            @PathVariable UUID courseId,
            @RequestHeader("X-User-Id") UUID userId) {
        EnrollmentDTO enrollment = enrollmentService.getEnrollmentByUserAndCourse(userId, courseId);
        return ResponseEntity.ok(enrollment);
    }
}
