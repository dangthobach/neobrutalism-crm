package com.neobrutalism.crm.common.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Service for sending emails
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@crm.local}")
    private String fromEmail;

    @Value("${app.name:Neobrutalism CRM}")
    private String appName;

    /**
     * Send simple text email
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send HTML email
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send email using Thymeleaf template
     */
    @Async
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // Create Thymeleaf context with variables
            Context context = new Context();
            context.setVariables(variables);
            context.setVariable("appName", appName);

            // Process template
            String htmlContent = templateEngine.process(templateName, context);

            // Send email
            sendHtmlEmail(to, subject, htmlContent);
            log.info("Template email sent successfully to: {} using template: {}", to, templateName);

        } catch (Exception e) {
            log.error("Failed to send template email to: {} with template: {}", to, templateName, e);
            throw new RuntimeException("Failed to send template email", e);
        }
    }

    /**
     * Send welcome email
     */
    public void sendWelcomeEmail(String to, String username, String password) {
        Map<String, Object> variables = Map.of(
                "username", username,
                "password", password,
                "loginUrl", "http://localhost:8080/login"
        );

        sendTemplateEmail(to, "Welcome to " + appName, "email/welcome", variables);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String to, String resetToken, String resetUrl) {
        Map<String, Object> variables = Map.of(
                "resetToken", resetToken,
                "resetUrl", resetUrl + "?token=" + resetToken
        );

        sendTemplateEmail(to, "Password Reset Request - " + appName, "email/password-reset", variables);
    }

    /**
     * Send notification email
     */
    public void sendNotificationEmail(String to, String title, String message, String actionUrl) {
        Map<String, Object> variables = Map.of(
                "title", title,
                "message", message,
                "actionUrl", actionUrl != null ? actionUrl : "",
                "hasAction", actionUrl != null
        );

        sendTemplateEmail(to, title + " - " + appName, "email/notification", variables);
    }

    /**
     * Send email with attachment
     */
    @Async
    public void sendEmailWithAttachment(String to, String subject, String text, String attachmentPath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);

            // Add attachment
            if (attachmentPath != null && !attachmentPath.isEmpty()) {
                helper.addAttachment(attachmentPath.substring(attachmentPath.lastIndexOf("/") + 1),
                        new java.io.File(attachmentPath));
            }

            mailSender.send(message);
            log.info("Email with attachment sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email with attachment to: {}", to, e);
            throw new RuntimeException("Failed to send email with attachment", e);
        }
    }

    /**
     * Send bulk emails
     */
    @Async
    public void sendBulkEmails(String[] recipients, String subject, String htmlContent) {
        for (String recipient : recipients) {
            try {
                sendHtmlEmail(recipient, subject, htmlContent);
            } catch (Exception e) {
                log.error("Failed to send bulk email to: {}", recipient, e);
            }
        }
        log.info("Bulk emails sent to {} recipients", recipients.length);
    }
}
