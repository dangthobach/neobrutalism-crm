package com.neobrutalism.crm.domain.course.service;

import com.neobrutalism.crm.common.enums.CourseStatus;
import com.neobrutalism.crm.common.enums.MemberTier;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.content.model.ContentCategory;
import com.neobrutalism.crm.domain.content.repository.ContentCategoryRepository;
import com.neobrutalism.crm.domain.course.dto.CourseDTO;
import com.neobrutalism.crm.domain.course.dto.CreateCourseRequest;
import com.neobrutalism.crm.domain.course.event.CoursePublishedEvent;
import com.neobrutalism.crm.domain.course.model.Course;
import com.neobrutalism.crm.domain.course.repository.CourseRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Course management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ContentCategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create a new course
     */
    @Transactional
    public CourseDTO createCourse(CreateCourseRequest request, String tenantId, String createdBy) {
        log.info("Creating course: {} for tenant: {}", request.getCode(), tenantId);

        // Validate code uniqueness
        if (courseRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new IllegalArgumentException("Course code already exists: " + request.getCode());
        }

        // Validate slug uniqueness
        if (courseRepository.existsBySlugAndDeletedFalse(request.getSlug())) {
            throw new IllegalArgumentException("Course slug already exists: " + request.getSlug());
        }

        // Get instructor
        User instructor = userRepository.findById(request.getInstructorId())
            .orElseThrow(() -> new ResourceNotFoundException("Instructor not found: " + request.getInstructorId()));

        // Get category if specified
        ContentCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
        }

        // Create course
        Course course = new Course();
        course.setCode(request.getCode());
        course.setTitle(request.getTitle());
        course.setSlug(request.getSlug());
        course.setDescription(request.getDescription());
        course.setShortDescription(request.getShortDescription());
        course.setCourseLevel(request.getCourseLevel());
        course.setStatus(CourseStatus.DRAFT);
        course.setTierRequired(request.getTierRequired() != null ? request.getTierRequired() : MemberTier.FREE);
        course.setPrice(request.getPrice());
        course.setInstructor(instructor);
        course.setCategory(category);
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setPreviewVideoUrl(request.getPreviewVideoUrl());
        course.setLearningObjectives(request.getLearningObjectives());
        course.setPrerequisites(request.getPrerequisites());
        course.setTargetAudience(request.getTargetAudience());
        course.setTenantId(tenantId);
        course.setCreatedBy(createdBy);

        course = courseRepository.save(course);
        log.info("Course created successfully: {}", course.getId());

        return mapToDTO(course);
    }

    /**
     * Get course by ID
     */
    @Transactional(readOnly = true)
    public CourseDTO getCourseById(UUID courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));
        return mapToDTO(course);
    }

    /**
     * Get course by slug
     */
    @Transactional(readOnly = true)
    public CourseDTO getCourseBySlug(String slug) {
        Course course = courseRepository.findBySlugAndDeletedFalse(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + slug));
        return mapToDTO(course);
    }

    /**
     * Get all published courses
     */
    @Transactional(readOnly = true)
    public Page<CourseDTO> getPublishedCourses(Pageable pageable) {
        return courseRepository.findByStatusAndDeletedFalse(CourseStatus.PUBLISHED, pageable)
            .map(this::mapToDTO);
    }

    /**
     * Get courses by instructor
     */
    @Transactional(readOnly = true)
    public Page<CourseDTO> getCoursesByInstructor(UUID instructorId, Pageable pageable) {
        return courseRepository.findByInstructorIdAndDeletedFalse(instructorId, pageable)
            .map(this::mapToDTO);
    }

    /**
     * Get courses accessible to user's tier
     */
    @Transactional(readOnly = true)
    public Page<CourseDTO> getCoursesForUserTier(MemberTier userTier, Pageable pageable) {
        // Get all tiers accessible to user (e.g., GOLD can access FREE, SILVER, GOLD)
        List<MemberTier> accessibleTiers = MemberTier.getAccessibleTiers(userTier);
        return courseRepository.findByAccessibleTiers(accessibleTiers, CourseStatus.PUBLISHED, pageable)
            .map(this::mapToDTO);
    }

    /**
     * Search courses
     */
    @Transactional(readOnly = true)
    public Page<CourseDTO> searchCourses(String keyword, Pageable pageable) {
        return courseRepository.searchCourses(keyword, CourseStatus.PUBLISHED, pageable)
            .map(this::mapToDTO);
    }

    /**
     * Publish course
     */
    @Transactional
    public CourseDTO publishCourse(UUID courseId, String publishedBy) {
        log.info("Publishing course: {}", courseId);

        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        // Validate course can be published
        if (course.getStatus() == CourseStatus.PUBLISHED) {
            throw new IllegalStateException("Course is already published");
        }

        // Transition to published state
        course.publish(publishedBy, "Course published");
        course.setUpdatedBy(publishedBy);

        course = courseRepository.save(course);
        log.info("Course published successfully: {}", courseId);

        // Publish event
        CoursePublishedEvent event = new CoursePublishedEvent(
            course.getId(),
            course.getCode(),
            course.getTitle(),
            course.getInstructor().getId(),
            course.getInstructor().getFullName(),
            course.getPublishedAt(),
            course.getTenantId(),
            publishedBy
        );
        eventPublisher.publishEvent(event);

        return mapToDTO(course);
    }

    /**
     * Update course rating (called by review service)
     */
    @Transactional
    public void updateCourseRating(UUID courseId, int rating) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        course.updateRating(rating);
        courseRepository.save(course);
    }

    /**
     * Increment enrollment count
     */
    @Transactional
    public void incrementEnrollmentCount(UUID courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        course.enroll();
        courseRepository.save(course);
    }

    /**
     * Increment completion count
     */
    @Transactional
    public void incrementCompletionCount(UUID courseId) {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + courseId));

        Integer currentCount = course.getCompletionCount();
        course.setCompletionCount(currentCount != null ? currentCount + 1 : 1);
        courseRepository.save(course);
    }

    /**
     * Map Course entity to DTO
     */
    private CourseDTO mapToDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setCode(course.getCode());
        dto.setTitle(course.getTitle());
        dto.setSlug(course.getSlug());
        dto.setDescription(course.getDescription());
        dto.setShortDescription(course.getShortDescription());
        dto.setCourseLevel(course.getCourseLevel());
        dto.setStatus(course.getStatus());
        dto.setTierRequired(course.getTierRequired());
        dto.setPrice(course.getPrice());
        dto.setThumbnailUrl(course.getThumbnailUrl());
        dto.setPreviewVideoUrl(course.getPreviewVideoUrl());

        // Instructor
        if (course.getInstructor() != null) {
            dto.setInstructorId(course.getInstructor().getId());
            dto.setInstructorName(course.getInstructor().getFullName());
        }

        // Category
        if (course.getCategory() != null) {
            dto.setCategoryId(course.getCategory().getId());
            dto.setCategoryName(course.getCategory().getName());
        }

        // Stats
        dto.setEnrollmentCount(course.getEnrollmentCount());
        dto.setCompletionCount(course.getCompletionCount());
        dto.setRatingAverage(course.getRatingAverage());
        dto.setRatingCount(course.getRatingCount());
        dto.setModuleCount(course.getModules() != null ? course.getModules().size() : 0);
        dto.setLessonCount(course.getTotalLessons());

        // Timestamps
        dto.setPublishedAt(course.getPublishedAt());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setUpdatedAt(course.getUpdatedAt());

        // Additional fields
        dto.setLearningObjectives(course.getLearningObjectives());
        dto.setPrerequisites(course.getPrerequisites());
        dto.setTargetAudience(course.getTargetAudience());

        return dto;
    }
}
