package com.neobrutalism.crm.domain.course.service;

import com.neobrutalism.crm.common.enums.LessonProgressStatus;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.course.dto.LessonDTO;
import com.neobrutalism.crm.domain.course.event.LessonCompletedEvent;
import com.neobrutalism.crm.domain.course.model.Enrollment;
import com.neobrutalism.crm.domain.course.model.Lesson;
import com.neobrutalism.crm.domain.course.model.LessonProgress;
import com.neobrutalism.crm.domain.course.repository.EnrollmentRepository;
import com.neobrutalism.crm.domain.course.repository.LessonProgressRepository;
import com.neobrutalism.crm.domain.course.repository.LessonRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing lesson progress
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LessonProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentService enrollmentService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Start a lesson
     */
    @Transactional
    public LessonProgress startLesson(UUID userId, UUID lessonId, UUID enrollmentId) {
        log.info("Starting lesson {} for user {}", lessonId, userId);

        // Get or create progress
        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, lessonId)
            .orElseGet(() -> createNewProgress(userId, lessonId, enrollmentId));

        if (progress.getStatus() == LessonProgressStatus.NOT_STARTED) {
            progress.start();
            progress = lessonProgressRepository.save(progress);
            log.info("Lesson started: {}", progress.getId());
        }

        return progress;
    }

    /**
     * Complete a lesson
     */
    @Transactional
    public LessonProgress completeLesson(UUID userId, UUID lessonId) {
        log.info("Completing lesson {} for user {}", lessonId, userId);

        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, lessonId)
            .orElseThrow(() -> new ResourceNotFoundException("Lesson progress not found"));

        if (!progress.isCompleted()) {
            progress.complete();
            progress = lessonProgressRepository.save(progress);

            // Fire event
            Lesson lesson = progress.getLesson();
            User user = progress.getUser();

            LessonCompletedEvent event = new LessonCompletedEvent(
                progress.getId(),
                user.getId(),
                lesson.getId(),
                lesson.getTitle(),
                lesson.getModule().getCourse().getId(),
                lesson.getModule().getCourse().getTitle(),
                progress.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                progress.getTimeSpentSeconds(),
                user.getTenantId(),
                user.getId().toString()
            );
            eventPublisher.publishEvent(event);

            // Recalculate enrollment progress
            enrollmentService.recalculateProgress(progress.getEnrollment().getId());

            log.info("Lesson completed: {}", progress.getId());
        }

        return progress;
    }

    /**
     * Update video position
     */
    @Transactional
    public LessonProgress updateVideoPosition(UUID userId, UUID lessonId, int seconds) {
        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, lessonId)
            .orElseThrow(() -> new ResourceNotFoundException("Lesson progress not found"));

        progress.updateVideoPosition(seconds);
        return lessonProgressRepository.save(progress);
    }

    /**
     * Update completion percentage
     */
    @Transactional
    public LessonProgress updateCompletionPercentage(UUID userId, UUID lessonId, int percentage) {
        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, lessonId)
            .orElseThrow(() -> new ResourceNotFoundException("Lesson progress not found"));

        progress.updateCompletionPercentage(percentage);
        progress = lessonProgressRepository.save(progress);

        // If 100%, trigger completion
        if (percentage == 100 && !progress.isCompleted()) {
            return completeLesson(userId, lessonId);
        }

        return progress;
    }

    /**
     * Add time spent
     */
    @Transactional
    public LessonProgress addTimeSpent(UUID userId, UUID lessonId, int seconds) {
        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, lessonId)
            .orElseThrow(() -> new ResourceNotFoundException("Lesson progress not found"));

        progress.addTimeSpent(seconds);
        progress.getEnrollment().updateLastAccessed();

        return lessonProgressRepository.save(progress);
    }

    /**
     * Get lesson progress for user
     */
    @Transactional(readOnly = true)
    public LessonProgress getLessonProgress(UUID userId, UUID lessonId) {
        return lessonProgressRepository.findByUserIdAndLessonIdAndDeletedFalse(userId, lessonId)
            .orElse(null);
    }

    /**
     * Get progress for enrollment
     */
    @Transactional(readOnly = true)
    public List<LessonProgress> getProgressForEnrollment(UUID enrollmentId) {
        return lessonProgressRepository.findByEnrollmentIdAndDeletedFalse(enrollmentId);
    }

    /**
     * Get progress for user and course
     */
    @Transactional(readOnly = true)
    public List<LessonProgress> getProgressForCourse(UUID userId, UUID courseId) {
        return lessonProgressRepository.findByUserAndCourse(userId, courseId);
    }

    /**
     * Get completed lessons count
     */
    @Transactional(readOnly = true)
    public long getCompletedLessonsCount(UUID enrollmentId) {
        return lessonProgressRepository.countByEnrollmentIdAndStatusAndDeletedFalse(
            enrollmentId,
            LessonProgressStatus.COMPLETED
        );
    }

    /**
     * Get total time spent on course
     */
    @Transactional(readOnly = true)
    public int getTotalTimeSpent(UUID userId, UUID courseId) {
        Integer timeSpent = lessonProgressRepository.getTotalTimeSpent(userId, courseId);
        return timeSpent != null ? timeSpent : 0;
    }

    /**
     * Check if lesson is completed
     */
    @Transactional(readOnly = true)
    public boolean isLessonCompleted(UUID userId, UUID lessonId) {
        return lessonProgressRepository.isLessonCompleted(userId, lessonId);
    }

    /**
     * Create new lesson progress
     */
    private LessonProgress createNewProgress(UUID userId, UUID lessonId, UUID enrollmentId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));

        LessonProgress progress = new LessonProgress();
        progress.setUser(user);
        progress.setLesson(lesson);
        progress.setEnrollment(enrollment);
        progress.setStatus(LessonProgressStatus.NOT_STARTED);
        progress.setCompletionPercentage(0);
        progress.setTimeSpentSeconds(0);
        progress.setAttemptsCount(0);
        progress.setTenantId(user.getTenantId());
        progress.setCreatedBy(user.getId().toString());

        return lessonProgressRepository.save(progress);
    }

    /**
     * Map LessonProgress to DTO (with progress info)
     */
    public LessonDTO mapToDTOWithProgress(Lesson lesson, LessonProgress progress) {
        LessonDTO dto = new LessonDTO();
        dto.setId(lesson.getId());
        dto.setTitle(lesson.getTitle());
        dto.setDescription(lesson.getContent());  // Use content as description
        dto.setLessonType(lesson.getLessonType());
        dto.setContent(lesson.getContent());
        dto.setSortOrder(lesson.getSortOrder());
        dto.setDurationMinutes(lesson.getDurationMinutes());
        dto.setVideoUrl(lesson.getVideoUrl());
        dto.setVideoDurationSeconds(lesson.getVideoDurationSeconds());
        // Document URL is from attachment if present
        if (lesson.getAttachment() != null) {
            dto.setDocumentUrl(lesson.getAttachment().getFilePath());
        }

        if (lesson.getQuiz() != null) {
            dto.setQuizId(lesson.getQuiz().getId());
            dto.setQuizTitle(lesson.getQuiz().getTitle());
        }

        dto.setCreatedAt(lesson.getCreatedAt());
        dto.setUpdatedAt(lesson.getUpdatedAt());

        // Add progress info
        if (progress != null) {
            dto.setIsCompleted(progress.isCompleted());
            dto.setCompletionPercentage(progress.getCompletionPercentage());
            dto.setLastPositionSeconds(progress.getLastPositionSeconds());
        } else {
            dto.setIsCompleted(false);
            dto.setCompletionPercentage(0);
        }

        // Calculate duration in minutes
        if (lesson.getVideoDurationSeconds() != null) {
            dto.setDurationMinutes(lesson.getVideoDurationSeconds() / 60);
        }

        return dto;
    }
}
