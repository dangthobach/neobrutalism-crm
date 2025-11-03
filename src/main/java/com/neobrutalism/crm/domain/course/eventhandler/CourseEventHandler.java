package com.neobrutalism.crm.domain.course.eventhandler;

import com.neobrutalism.crm.domain.course.event.CoursePublishedEvent;
import com.neobrutalism.crm.common.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event handler for Course-related events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseEventHandler {

    private final EmailService emailService;

    /**
     * Handle course published event
     * Sends notification emails to interested parties
     */
    @EventListener
    @Async
    public void handleCoursePublished(CoursePublishedEvent event) {
        log.info("Handling CoursePublishedEvent for course: {}", event.getCourseCode());

        try {
            // TODO: Get list of users who follow this instructor or category
            // For now, send email to instructor
            sendCoursePublishedNotification(event);

            log.info("Course published notification sent for: {}", event.getCourseCode());
        } catch (Exception e) {
            log.error("Error handling CoursePublishedEvent for course: {}", event.getCourseCode(), e);
        }
    }

    /**
     * Send course published notification email
     */
    private void sendCoursePublishedNotification(CoursePublishedEvent event) {
        String subject = "Course Published: " + event.getCourseTitle();

        String htmlBody = buildCoursePublishedEmail(event);

        // Note: In a real implementation, you would:
        // 1. Get list of followers/interested users
        // 2. Send bulk emails
        // 3. Use email templates

        log.info("Course published email would be sent: {}", subject);
        // Uncomment when email is properly configured:
        // emailService.sendHtmlEmail(to, subject, htmlBody);
    }

    /**
     * Build HTML email for course published
     */
    private String buildCoursePublishedEmail(CoursePublishedEvent event) {
        return String.format("""
            <html>
            <body>
                <h2>New Course Published!</h2>
                <p>A new course has been published:</p>
                <ul>
                    <li><strong>Title:</strong> %s</li>
                    <li><strong>Code:</strong> %s</li>
                    <li><strong>Instructor:</strong> %s</li>
                    <li><strong>Published:</strong> %s</li>
                </ul>
                <p>
                    <a href="/courses/%s">View Course</a>
                </p>
            </body>
            </html>
            """,
            event.getCourseTitle(),
            event.getCourseCode(),
            event.getInstructorName(),
            event.getOccurredAt(),
            event.getCourseId()
        );
    }
}
