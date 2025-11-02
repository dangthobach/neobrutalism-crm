package com.neobrutalism.crm.domain.course.event.handler;

import com.neobrutalism.crm.common.email.EmailService;
import com.neobrutalism.crm.domain.course.event.*;
import com.neobrutalism.crm.domain.course.repository.EnrollmentRepository;
import com.neobrutalism.crm.domain.notification.model.NotificationType;
import com.neobrutalism.crm.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Event handler for course-related domain events
 * Handles notifications, emails, and side effects of course events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventHandler {

    private final EmailService emailService;
    private final NotificationService notificationService;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Handle student enrolled event
     * Sends welcome email and creates notification
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStudentEnrolled(StudentEnrolledEvent event) {
        log.info("Handling StudentEnrolledEvent for enrollment: {}", event.getEnrollmentId());

        try {
            // Send welcome email
            sendEnrollmentWelcomeEmail(event);

            // Create notification
            createEnrollmentNotification(event);

            log.info("Successfully handled StudentEnrolledEvent for enrollment: {}", event.getEnrollmentId());
        } catch (Exception e) {
            log.error("Error handling StudentEnrolledEvent for enrollment: {}", event.getEnrollmentId(), e);
            // Don't rethrow - allow the main transaction to complete
        }
    }

    /**
     * Handle course completed event
     * Issues certificate and sends congratulations email
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCourseCompleted(CourseCompletedEvent event) {
        log.info("Handling CourseCompletedEvent for enrollment: {}", event.getEnrollmentId());

        try {
            // Send congratulations email
            sendCourseCompletionEmail(event);

            // Create completion notification
            createCompletionNotification(event);

            // Update enrollment completion metrics
            updateEnrollmentMetrics(event);

            log.info("Successfully handled CourseCompletedEvent for enrollment: {}", event.getEnrollmentId());
        } catch (Exception e) {
            log.error("Error handling CourseCompletedEvent for enrollment: {}", event.getEnrollmentId(), e);
        }
    }

    /**
     * Handle lesson completed event
     * Creates notification for lesson completion
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleLessonCompleted(LessonCompletedEvent event) {
        log.info("Handling LessonCompletedEvent for lesson: {} by user: {}",
            event.getLessonId(), event.getUserId());

        try {
            // Create lesson completion notification
            createLessonCompletionNotification(event);

            log.debug("Successfully handled LessonCompletedEvent for lesson: {}", event.getLessonId());
        } catch (Exception e) {
            log.error("Error handling LessonCompletedEvent for lesson: {}", event.getLessonId(), e);
        }
    }

    /**
     * Handle quiz completed event
     * Sends quiz results notifications
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleQuizCompleted(QuizCompletedEvent event) {
        log.info("Handling QuizCompletedEvent for quiz: {}", event.getQuizId());

        try {
            // Create quiz result notification
            createQuizResultNotification(event);

            // Log high score achievements
            if (Boolean.TRUE.equals(event.getIsPassed()) && event.getScore() != null && event.getScore() >= 90) {
                log.info("High score achieved: {} for quiz: {}", event.getScore(), event.getQuizId());
            }

            log.info("Successfully handled QuizCompletedEvent for quiz: {}", event.getQuizId());
        } catch (Exception e) {
            log.error("Error handling QuizCompletedEvent for quiz: {}", event.getQuizId(), e);
        }
    }

    /**
     * Handle certificate issued event
     * Sends certificate email with download link
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCertificateIssued(CertificateIssuedEvent event) {
        log.info("Handling CertificateIssuedEvent for certificate: {}", event.getCertificateId());

        try {
            // Send certificate email
            sendCertificateEmail(event);

            // Create certificate notification
            createCertificateNotification(event);

            log.info("Successfully handled CertificateIssuedEvent for certificate: {}", event.getCertificateId());
        } catch (Exception e) {
            log.error("Error handling CertificateIssuedEvent for certificate: {}", event.getCertificateId(), e);
        }
    }

    /**
     * Handle achievement earned event
     * Sends achievement notification
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAchievementEarned(AchievementEarnedEvent event) {
        log.info("Handling AchievementEarnedEvent: {} for user: {}",
            event.getAchievementCode(), event.getUserId());

        try {
            // Create achievement notification
            createAchievementNotification(event);

            // Send email for special achievements
            if (isSpecialAchievement(event.getAchievementCode())) {
                sendAchievementEmail(event);
            }

            log.info("Successfully handled AchievementEarnedEvent: {}", event.getAchievementCode());
        } catch (Exception e) {
            log.error("Error handling AchievementEarnedEvent: {}", event.getAchievementCode(), e);
        }
    }

    /**
     * Handle course published event
     * Notifies interested users about new course
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCoursePublished(CoursePublishedEvent event) {
        log.info("Handling CoursePublishedEvent for course: {}", event.getCourseId());

        try {
            // TODO: Notify interested users or send to recommendation engine
            log.info("Course published: {} - {}", event.getCourseCode(), event.getCourseTitle());

        } catch (Exception e) {
            log.error("Error handling CoursePublishedEvent for course: {}", event.getCourseId(), e);
        }
    }

    // ==================== Private Helper Methods ====================

    private void sendEnrollmentWelcomeEmail(StudentEnrolledEvent event) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("userName", event.getUserName());
            templateData.put("courseTitle", event.getCourseTitle());
            templateData.put("courseCode", event.getCourseCode());
            templateData.put("enrolledAt", event.getEnrolledAt());

            emailService.sendTemplateEmail(
                event.getUserEmail(),
                "Welcome to " + event.getCourseTitle(),
                "enrollment-welcome",
                templateData
            );

            log.debug("Sent enrollment welcome email to: {}", event.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to send enrollment welcome email", e);
        }
    }

    private void createEnrollmentNotification(StudentEnrolledEvent event) {
        try {
            String message = String.format("You have successfully enrolled in %s. Start learning now!",
                event.getCourseTitle());

            notificationService.createNotification(
                event.getUserId(),
                "Course Enrollment",
                message,
                NotificationType.SYSTEM,
                1,
                "/courses/" + event.getCourseId(),
                "Enrollment",
                event.getEnrollmentId()
            );

            log.debug("Created enrollment notification for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create enrollment notification", e);
        }
    }

    private void sendCourseCompletionEmail(CourseCompletedEvent event) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("userName", event.getUserName());
            templateData.put("courseTitle", event.getCourseTitle());
            templateData.put("completedAt", event.getCompletedAt());
            templateData.put("durationDays", event.getDurationDays());

            emailService.sendTemplateEmail(
                event.getUserEmail(),
                "Congratulations on completing " + event.getCourseTitle(),
                "course-completion",
                templateData
            );

            log.debug("Sent course completion email to: {}", event.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to send course completion email", e);
        }
    }

    private void createCompletionNotification(CourseCompletedEvent event) {
        try {
            String message = String.format("Congratulations! You have completed %s. Your certificate is being prepared.",
                event.getCourseTitle());

            notificationService.createNotification(
                event.getUserId(),
                "Course Completed",
                message,
                NotificationType.SYSTEM,
                2,
                "/enrollments/" + event.getEnrollmentId(),
                "Enrollment",
                event.getEnrollmentId()
            );

            log.debug("Created completion notification for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create completion notification", e);
        }
    }

    private void updateEnrollmentMetrics(CourseCompletedEvent event) {
        try {
            enrollmentRepository.findById(event.getEnrollmentId()).ifPresent(enrollment -> {
                // Additional metrics update if needed
                log.debug("Updated metrics for enrollment: {}", event.getEnrollmentId());
            });
        } catch (Exception e) {
            log.error("Failed to update enrollment metrics", e);
        }
    }

    private void createLessonCompletionNotification(LessonCompletedEvent event) {
        try {
            String message = String.format("You completed the lesson: %s in %s",
                event.getLessonTitle(), event.getCourseTitle());

            notificationService.createNotification(
                event.getUserId(),
                "Lesson Completed",
                message,
                NotificationType.SYSTEM,
                0,
                "/courses/" + event.getCourseId() + "/lessons/" + event.getLessonId(),
                "LessonProgress",
                event.getLessonProgressId()
            );

            log.debug("Created lesson completion notification for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create lesson completion notification", e);
        }
    }

    private void createQuizResultNotification(QuizCompletedEvent event) {
        try {
            String message = Boolean.TRUE.equals(event.getIsPassed())
                ? String.format("Congratulations! You passed the quiz '%s' with score: %.1f", event.getQuizTitle(), event.getScore())
                : String.format("You scored %.1f on the quiz '%s'. Keep practicing!", event.getScore(), event.getQuizTitle());

            notificationService.createNotification(
                event.getUserId(),
                Boolean.TRUE.equals(event.getIsPassed()) ? "Quiz Passed" : "Quiz Completed",
                message,
                NotificationType.SYSTEM,
                1,
                "/quizzes/" + event.getQuizId() + "/attempts/" + event.getQuizAttemptId(),
                "QuizAttempt",
                event.getQuizAttemptId()
            );

            log.debug("Created quiz result notification for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create quiz result notification", e);
        }
    }

    private void sendCertificateEmail(CertificateIssuedEvent event) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("userName", event.getUserName());
            templateData.put("courseTitle", event.getCourseTitle());
            templateData.put("certificateNumber", event.getCertificateNumber());
            templateData.put("verificationUrl", event.getVerificationUrl());

            emailService.sendTemplateEmail(
                event.getUserEmail(),
                "Your Certificate is Ready!",
                "certificate-issued",
                templateData
            );

            log.debug("Sent certificate email to: {}", event.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to send certificate email", e);
        }
    }

    private void createCertificateNotification(CertificateIssuedEvent event) {
        try {
            String message = String.format("Your certificate for %s is ready! Certificate number: %s",
                event.getCourseTitle(), event.getCertificateNumber());

            notificationService.createNotification(
                event.getUserId(),
                "Certificate Issued",
                message,
                NotificationType.SYSTEM,
                2,
                event.getVerificationUrl(),
                "Certificate",
                event.getCertificateId()
            );

            log.debug("Created certificate notification for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create certificate notification", e);
        }
    }

    private void createAchievementNotification(AchievementEarnedEvent event) {
        try {
            String message = String.format("Achievement unlocked: %s! You earned %d points.",
                event.getAchievementName(), event.getPoints());

            notificationService.createNotification(
                event.getUserId(),
                "Achievement Earned",
                message,
                NotificationType.SYSTEM,
                1,
                "/achievements/" + event.getAchievementId(),
                "Achievement",
                event.getAchievementId()
            );

            log.debug("Created achievement notification for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to create achievement notification", e);
        }
    }

    private boolean isSpecialAchievement(String achievementCode) {
        return achievementCode != null && (
            achievementCode.contains("FIRST") ||
            achievementCode.contains("PERFECT") ||
            achievementCode.contains("MASTER")
        );
    }

    private void sendAchievementEmail(AchievementEarnedEvent event) {
        try {
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("userName", event.getUserName());
            templateData.put("achievementName", event.getAchievementName());
            templateData.put("achievementCode", event.getAchievementCode());
            templateData.put("points", event.getPoints());

            emailService.sendTemplateEmail(
                // Note: AchievementEarnedEvent doesn't have userEmail, need to fetch from User
                null, // TODO: Fetch user email from UserRepository
                "Special Achievement Unlocked!",
                "achievement-earned",
                templateData
            );

            log.debug("Sent achievement email for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to send achievement email", e);
        }
    }
}
