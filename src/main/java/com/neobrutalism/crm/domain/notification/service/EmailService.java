package com.neobrutalism.crm.domain.notification.service;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ErrorCode;
import com.neobrutalism.crm.domain.attachment.model.Attachment;
import com.neobrutalism.crm.domain.attachment.service.AttachmentService;
import com.neobrutalism.crm.domain.notification.model.Notification;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Email Service with Attachment Support
 * 
 * Features:
 * - Send notification emails using Thymeleaf templates
 * - Support file attachments (max 10MB)
 * - Async email sending for better performance
 * - Leverage existing AttachmentService for file handling
 * - Quiet hours support (configurable)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AttachmentService attachmentService;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.email.quiet-hours.enabled:false}")
    private boolean quietHoursEnabled;

    @Value("${notification.email.quiet-hours.start:22}")
    private int quietHoursStart;

    @Value("${notification.email.quiet-hours.end:7}")
    private int quietHoursEnd;

    @Value("${notification.email.quiet-hours.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @Value("${file.upload.max-size:10485760}") // 10MB
    private long maxAttachmentSize;

    /**
     * Send notification email
     * 
     * @param toEmail Recipient email
     * @param notification Notification object containing subject, message, etc.
     */
    @Async
    public void sendNotificationEmail(String toEmail, Notification notification) {
        sendNotificationEmail(toEmail, notification, null);
    }

    /**
     * Send notification email with attachment
     * 
     * @param toEmail Recipient email
     * @param notification Notification object
     * @param attachmentId Attachment UUID (from Attachment table)
     */
    @Async
    public void sendNotificationEmail(String toEmail, Notification notification, UUID attachmentId) {
        if (!emailEnabled) {
            log.debug("Email sending is disabled. Skipping email to: {}", toEmail);
            return;
        }

        if (isQuietHours()) {
            log.debug("Quiet hours active. Skipping email to: {}", toEmail);
            return;
        }

        try {
            log.info("Sending notification email to: {} for notification: {}", 
                toEmail, notification.getId());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email headers
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(notification.getTitle());

            // Build email body from Thymeleaf template
            String htmlContent = buildEmailContent(notification);
            helper.setText(htmlContent, true);

            // Add attachment if provided
            if (attachmentId != null) {
                addAttachment(helper, attachmentId);
            }

            // Send email
            mailSender.send(message);

            log.info("Email sent successfully to: {} for notification: {}", 
                toEmail, notification.getId());

        } catch (Exception e) {
            log.error("Failed to send email to: {} for notification: {}", 
                toEmail, notification.getId(), e);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED, 
                "Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Send custom email with attachment from MultipartFile
     * Used for ad-hoc emails outside notification system
     */
    @Async
    public void sendEmailWithFile(
        String toEmail,
        String subject,
        String body,
        String fileName,
        byte[] fileContent,
        String contentType
    ) {
        if (!emailEnabled) {
            log.debug("Email sending is disabled");
            return;
        }

        // Validate file size
        if (fileContent.length > maxAttachmentSize) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                String.format("Attachment size %d bytes exceeds maximum %d bytes (10MB)",
                    fileContent.length, maxAttachmentSize));
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);

            // Add file attachment
            DataSource dataSource = new ByteArrayDataSource(fileContent, contentType);
            helper.addAttachment(fileName, dataSource);

            mailSender.send(message);

            log.info("Email with attachment sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email with attachment to: {}", toEmail, e);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED,
                "Failed to send email with attachment: " + e.getMessage());
        }
    }

    /**
     * Build email content using Thymeleaf template
     */
    private String buildEmailContent(Notification notification) {
        Context context = new Context();
        
        // Add notification data to template context
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", notification.getTitle());
        variables.put("message", notification.getMessage());
        variables.put("type", notification.getNotificationType().name());
        variables.put("createdDate", notification.getCreatedAt());
        variables.put("metadata", notification.getMetadata());
        
        context.setVariables(variables);

        // Process template
        return templateEngine.process("email/notification", context);
    }

    /**
     * Add attachment to email from Attachment entity
     * Leverages existing AttachmentService to download file from MinIO
     */
    private void addAttachment(MimeMessageHelper helper, UUID attachmentId) 
        throws MessagingException {
        
        try {
            // Get attachment metadata
            Attachment attachment = attachmentService.findById(attachmentId);

            // Validate file size
            if (attachment.getFileSize() > maxAttachmentSize) {
                throw new BusinessException(ErrorCode.FILE_TOO_LARGE,
                    String.format("Attachment size %d bytes exceeds maximum %d bytes (10MB)",
                        attachment.getFileSize(), maxAttachmentSize));
            }

            // Download file from MinIO
            InputStream fileStream = attachmentService.downloadFile(attachmentId);
            
            // Read all bytes (for files < 10MB this is acceptable)
            byte[] fileBytes = fileStream.readAllBytes();
            fileStream.close();

            // Create data source and add attachment
            DataSource dataSource = new ByteArrayDataSource(
                fileBytes, 
                attachment.getContentType()
            );
            
            helper.addAttachment(attachment.getOriginalFilename(), dataSource);

            log.debug("Added attachment: {} ({} bytes) to email", 
                attachment.getOriginalFilename(), attachment.getFileSize());

        } catch (Exception e) {
            log.error("Failed to add attachment {} to email", attachmentId, e);
            throw new MessagingException("Failed to add attachment to email", e);
        }
    }

    /**
     * Check if current time is within quiet hours
     * During quiet hours, notification emails are not sent
     */
    private boolean isQuietHours() {
        if (!quietHoursEnabled) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));
        int currentHour = now.getHour();

        // Handle quiet hours that span midnight
        if (quietHoursStart > quietHoursEnd) {
            // e.g., 22:00 to 07:00 (10 PM to 7 AM)
            return currentHour >= quietHoursStart || currentHour < quietHoursEnd;
        } else {
            // e.g., 01:00 to 05:00 (1 AM to 5 AM)
            return currentHour >= quietHoursStart && currentHour < quietHoursEnd;
        }
    }

    /**
     * Send test email to verify configuration
     */
    public void sendTestEmail(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Test Email - Neobrutalism CRM");
            helper.setText("This is a test email from Neobrutalism CRM. Email service is working correctly!", false);

            mailSender.send(message);

            log.info("Test email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send test email to: {}", toEmail, e);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED,
                "Failed to send test email: " + e.getMessage());
        }
    }
}
