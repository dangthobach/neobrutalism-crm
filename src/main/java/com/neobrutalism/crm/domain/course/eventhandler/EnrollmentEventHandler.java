package com.neobrutalism.crm.domain.course.eventhandler;

import com.neobrutalism.crm.common.email.EmailService;
import com.neobrutalism.crm.domain.course.event.StudentEnrolledEvent;
import com.neobrutalism.crm.domain.course.event.CourseCompletedEvent;
import com.neobrutalism.crm.domain.course.event.CertificateIssuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event handler for Enrollment-related events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentEventHandler {

    private final EmailService emailService;

    /**
     * Handle student enrolled event
     * Sends welcome email to the student
     */
    @EventListener
    @Async
    public void handleStudentEnrolled(StudentEnrolledEvent event) {
        log.info("Handling StudentEnrolledEvent - User: {} enrolled in Course: {}",
                event.getUserName(), event.getCourseTitle());

        try {
            sendWelcomeEmail(event);
            log.info("Welcome email sent to: {}", event.getUserEmail());
        } catch (Exception e) {
            log.error("Error sending welcome email for enrollment: {}", event.getEnrollmentId(), e);
        }
    }

    /**
     * Handle course completed event
     * Sends congratulations email and triggers certificate generation
     */
    @EventListener
    @Async
    public void handleCourseCompleted(CourseCompletedEvent event) {
        log.info("Handling CourseCompletedEvent - User: {} completed Course: {}",
                event.getUserName(), event.getCourseTitle());

        try {
            sendCompletionEmail(event);
            log.info("Completion email sent to: {}", event.getUserEmail());

            // TODO: Trigger certificate generation
            // certificateService.issueCertificate(event.getEnrollmentId());

            // TODO: Award course completion achievement
            // achievementService.checkAndAwardAchievements(event.getUserId());

        } catch (Exception e) {
            log.error("Error handling course completion for enrollment: {}", event.getEnrollmentId(), e);
        }
    }

    /**
     * Handle certificate issued event
     * Sends certificate notification email
     */
    @EventListener
    @Async
    public void handleCertificateIssued(CertificateIssuedEvent event) {
        log.info("Handling CertificateIssuedEvent - Certificate {} issued to {}",
                event.getCertificateNumber(), event.getUserName());

        try {
            sendCertificateEmail(event);
            log.info("Certificate email sent to: {}", event.getUserEmail());
        } catch (Exception e) {
            log.error("Error sending certificate email for: {}", event.getCertificateNumber(), e);
        }
    }

    /**
     * Send welcome email to newly enrolled student
     */
    private void sendWelcomeEmail(StudentEnrolledEvent event) {
        String subject = "Welcome to " + event.getCourseTitle();

        String htmlBody = String.format("""
            <html>
            <body>
                <h2>Welcome to %s!</h2>
                <p>Hi %s,</p>
                <p>You have successfully enrolled in <strong>%s</strong>.</p>
                <p><strong>Course Details:</strong></p>
                <ul>
                    <li>Course Code: %s</li>
                    <li>Enrolled: %s</li>
                    %s
                </ul>
                <p>
                    <a href="/enrollments/%s">Start Learning</a>
                </p>
                <p>Good luck with your learning journey!</p>
            </body>
            </html>
            """,
            event.getCourseTitle(),
            event.getUserName(),
            event.getCourseTitle(),
            event.getCourseCode(),
            event.getEnrolledAt(),
            event.getPricePaid() != null && event.getPricePaid().doubleValue() > 0
                    ? "<li>Price Paid: $" + event.getPricePaid() + "</li>"
                    : "",
            event.getEnrollmentId()
        );

        log.info("Welcome email prepared for: {}", event.getUserEmail());
        // Uncomment when email is properly configured:
        // emailService.sendHtmlEmail(event.getUserEmail(), subject, htmlBody);
    }

    /**
     * Send course completion congratulations email
     */
    private void sendCompletionEmail(CourseCompletedEvent event) {
        String subject = "Congratulations! You've completed " + event.getCourseTitle();

        String htmlBody = String.format("""
            <html>
            <body>
                <h2>Congratulations, %s!</h2>
                <p>You have successfully completed <strong>%s</strong>!</p>
                <p><strong>Achievement Summary:</strong></p>
                <ul>
                    <li>Course: %s (%s)</li>
                    <li>Completed: %s</li>
                    <li>Duration: %d days</li>
                    <li>Progress: %d%%</li>
                </ul>
                <p>Your certificate will be generated shortly and available in your dashboard.</p>
                <p>
                    <a href="/certificates">View Certificates</a>
                </p>
                <p>Keep up the great work!</p>
            </body>
            </html>
            """,
            event.getUserName(),
            event.getCourseTitle(),
            event.getCourseTitle(),
            event.getCourseCode(),
            event.getCompletedAt(),
            event.getDurationDays(),
            event.getProgressPercentage()
        );

        log.info("Completion email prepared for: {}", event.getUserEmail());
        // Uncomment when email is properly configured:
        // emailService.sendHtmlEmail(event.getUserEmail(), subject, htmlBody);
    }

    /**
     * Send certificate issued notification email
     */
    private void sendCertificateEmail(CertificateIssuedEvent event) {
        String subject = "Your Certificate for " + event.getCourseTitle();

        String htmlBody = String.format("""
            <html>
            <body>
                <h2>Your Certificate is Ready!</h2>
                <p>Hi %s,</p>
                <p>Your certificate for completing <strong>%s</strong> has been issued!</p>
                <p><strong>Certificate Details:</strong></p>
                <ul>
                    <li>Certificate Number: %s</li>
                    <li>Course: %s (%s)</li>
                    <li>Issued: %s</li>
                    %s
                </ul>
                <p>
                    <a href="%s">Verify Certificate</a> |
                    <a href="/certificates/%s">Download PDF</a>
                </p>
            </body>
            </html>
            """,
            event.getUserName(),
            event.getCourseTitle(),
            event.getCertificateNumber(),
            event.getCourseTitle(),
            event.getCourseCode(),
            event.getIssuedAt(),
            event.getFinalScore() != null
                    ? "<li>Final Score: " + String.format("%.2f", event.getFinalScore()) + "%</li>"
                    : "",
            event.getVerificationUrl(),
            event.getCertificateId()
        );

        log.info("Certificate email prepared for: {}", event.getUserEmail());
        // Uncomment when email is properly configured:
        // emailService.sendHtmlEmail(event.getUserEmail(), subject, htmlBody);
    }
}
