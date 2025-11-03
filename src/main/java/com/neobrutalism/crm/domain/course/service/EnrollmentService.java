package com.neobrutalism.crm.domain.course.service;

import com.neobrutalism.crm.common.enums.EnrollmentStatus;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.course.dto.EnrollmentDTO;
import com.neobrutalism.crm.domain.course.event.CourseCompletedEvent;
import com.neobrutalism.crm.domain.course.event.StudentEnrolledEvent;
import com.neobrutalism.crm.domain.course.model.Course;
import com.neobrutalism.crm.domain.course.model.Enrollment;
import com.neobrutalism.crm.domain.course.repository.CourseRepository;
import com.neobrutalism.crm.domain.course.repository.EnrollmentRepository;
import com.neobrutalism.crm.domain.course.repository.LessonProgressRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing course enrollments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseService courseService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Enroll a user in a course
     */
    @Transactional
    public EnrollmentDTO enrollUserInCourse(UUID userId, UUID courseId, BigDecimal pricePaid, String tenantId, String enrolledBy) {
        log.info("Enrolling user {} in course {}", userId, courseId);

        // Check if already enrolled
        if (enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(userId, courseId)) {
            throw new IllegalStateException("User is already enrolled in this course");
        }

        // Get user and course
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        // Validate course is published
        if (course.getStatus() != com.neobrutalism.crm.common.enums.CourseStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot enroll in unpublished course");
        }

        // Check tier access
        if (!user.getMemberTier().canAccess(course.getTierRequired())) {
            throw new IllegalStateException("User tier " + user.getMemberTier() +
                " cannot access course requiring tier " + course.getTierRequired());
        }

        // Create enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setPricePaid(pricePaid != null ? pricePaid : BigDecimal.ZERO);
        enrollment.setProgressPercentage(0);
        enrollment.setTenantId(tenantId);
        enrollment.setCreatedBy(enrolledBy);

        enrollment = enrollmentRepository.save(enrollment);
        log.info("Enrollment created: {}", enrollment.getId());

        // Update course enrollment count
        courseService.incrementEnrollmentCount(courseId);

        // Publish event
        StudentEnrolledEvent event = new StudentEnrolledEvent(
            enrollment.getId(),
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            course.getId(),
            course.getCode(),
            course.getTitle(),
            pricePaid,
            enrollment.getEnrolledAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
            tenantId,
            enrolledBy
        );
        eventPublisher.publishEvent(event);

        return mapToDTO(enrollment);
    }

    /**
     * Get enrollment by ID
     */
    @Transactional(readOnly = true)
    public EnrollmentDTO getEnrollmentById(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));
        return mapToDTO(enrollment);
    }

    /**
     * Get user enrollments
     */
    @Transactional(readOnly = true)
    public Page<EnrollmentDTO> getUserEnrollments(UUID userId, Pageable pageable) {
        return enrollmentRepository.findByUserIdAndDeletedFalse(userId, pageable)
            .map(this::mapToDTO);
    }

    /**
     * Get active enrollments for user
     */
    @Transactional(readOnly = true)
    public java.util.List<EnrollmentDTO> getActiveEnrollments(UUID userId) {
        return enrollmentRepository.findByUserIdAndStatusAndDeletedFalse(userId, EnrollmentStatus.ACTIVE)
            .stream()
            .map(this::mapToDTO)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Update enrollment progress
     */
    @Transactional
    public EnrollmentDTO updateProgress(UUID enrollmentId, Integer progressPercentage) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));

        enrollment.updateProgress(progressPercentage);
        enrollment.updateLastAccessed();

        enrollment = enrollmentRepository.save(enrollment);

        // If completed, fire event
        if (enrollment.isCompleted()) {
            CourseCompletedEvent event = new CourseCompletedEvent(
                enrollment.getId(),
                enrollment.getUser().getId(),
                enrollment.getUser().getFullName(),
                enrollment.getUser().getEmail(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getCode(),
                enrollment.getCourse().getTitle(),
                enrollment.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                enrollment.getProgressPercentage(),
                enrollment.getEnrollmentDurationDays(),
                enrollment.getTenantId(),
                enrollment.getUser().getId().toString()
            );
            eventPublisher.publishEvent(event);

            // Update course completion count
            courseService.incrementCompletionCount(enrollment.getCourse().getId());
        }

        return mapToDTO(enrollment);
    }

    /**
     * Calculate and update enrollment progress based on lesson completion
     */
    @Transactional
    public EnrollmentDTO recalculateProgress(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));

        // Calculate progress from lesson completion
        Integer progressPercentage = lessonProgressRepository.calculateCompletionPercentage(enrollmentId)
            .orElse(0);

        return updateProgress(enrollmentId, progressPercentage);
    }

    /**
     * Drop enrollment
     */
    @Transactional
    public EnrollmentDTO dropEnrollment(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));

        enrollment.drop();
        enrollment = enrollmentRepository.save(enrollment);

        return mapToDTO(enrollment);
    }

    /**
     * Check if user is enrolled in course
     */
    @Transactional(readOnly = true)
    public boolean isUserEnrolled(UUID userId, UUID courseId) {
        return enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(userId, courseId);
    }

    /**
     * Get enrollment by user and course
     */
    @Transactional(readOnly = true)
    public EnrollmentDTO getEnrollmentByUserAndCourse(UUID userId, UUID courseId) {
        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseIdAndDeletedFalse(userId, courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found for user and course"));
        return mapToDTO(enrollment);
    }

    /**
     * Map Enrollment to DTO
     */
    private EnrollmentDTO mapToDTO(Enrollment enrollment) {
        EnrollmentDTO dto = new EnrollmentDTO();
        dto.setId(enrollment.getId());
        dto.setUserId(enrollment.getUser().getId());
        dto.setUserName(enrollment.getUser().getFullName());
        dto.setCourseId(enrollment.getCourse().getId());
        dto.setCourseCode(enrollment.getCourse().getCode());
        dto.setCourseTitle(enrollment.getCourse().getTitle());
        dto.setCourseThumbnailUrl(enrollment.getCourse().getThumbnailUrl());
        dto.setStatus(enrollment.getStatus());
        dto.setEnrolledAt(enrollment.getEnrolledAt().atZone(java.time.ZoneId.systemDefault()).toInstant());

        if (enrollment.getCompletedAt() != null) {
            dto.setCompletedAt(enrollment.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
        }

        if (enrollment.getExpiresAt() != null) {
            dto.setExpiresAt(enrollment.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
        }

        dto.setProgressPercentage(enrollment.getProgressPercentage());

        if (enrollment.getLastAccessedAt() != null) {
            dto.setLastAccessedAt(enrollment.getLastAccessedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
        }

        dto.setPricePaid(enrollment.getPricePaid());

        if (enrollment.getCertificateIssuedAt() != null) {
            dto.setCertificateIssuedAt(enrollment.getCertificateIssuedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
        }

        dto.setNotes(enrollment.getNotes());

        // Add stats
        long completedLessons = lessonProgressRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
            enrollment.getId(),
            com.neobrutalism.crm.common.enums.LessonProgressStatus.COMPLETED
        );
        long totalLessons = lessonProgressRepository.countTotalLessonsByEnrollment(enrollment.getId());

        dto.setCompletedLessons((int) completedLessons);
        dto.setTotalLessons((int) totalLessons);

        Integer timeSpent = lessonProgressRepository.getTotalTimeSpent(
            enrollment.getUser().getId(),
            enrollment.getCourse().getId()
        );
        dto.setTimeSpentMinutes(timeSpent != null ? timeSpent / 60 : 0);

        return dto;
    }
}
