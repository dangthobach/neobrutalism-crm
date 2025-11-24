package com.neobrutalism.crm.domain.notification.controller;

import com.neobrutalism.crm.common.dto.ApiResponse;
import com.neobrutalism.crm.domain.notification.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Email Test Controller
 * For testing email functionality with attachments
 */
@Slf4j
@RestController
@RequestMapping("/api/email/test")
@RequiredArgsConstructor
@Tag(name = "Email Testing", description = "Email testing APIs")
public class EmailTestController {

    private final EmailService emailService;

    @PostMapping("/send")
    @Operation(summary = "Send test email", description = "Send a test email to verify configuration")
    public ApiResponse<String> sendTestEmail(@RequestParam String toEmail) {
        log.info("Sending test email to: {}", toEmail);
        
        try {
            emailService.sendTestEmail(toEmail);
            return ApiResponse.success("Test email sent successfully to: " + toEmail);
        } catch (Exception e) {
            log.error("Failed to send test email", e);
            return ApiResponse.error("Failed to send test email: " + e.getMessage());
        }
    }

    @PostMapping("/send-with-file")
    @Operation(summary = "Send test email with attachment", 
               description = "Send test email with file attachment (max 10MB)")
    public ApiResponse<String> sendTestEmailWithFile(
        @RequestParam String toEmail,
        @RequestParam String subject,
        @RequestParam String body,
        @RequestParam String fileName,
        @RequestParam(required = false, defaultValue = "This is a test file content") String fileContent
    ) {
        log.info("Sending test email with attachment to: {}", toEmail);
        
        try {
            byte[] fileBytes = fileContent.getBytes();
            
            emailService.sendEmailWithFile(
                toEmail,
                subject,
                body,
                fileName,
                fileBytes,
                "text/plain"
            );
            
            return ApiResponse.success(
                String.format("Test email with attachment sent successfully to: %s (file: %s, size: %d bytes)", 
                    toEmail, fileName, fileBytes.length)
            );
        } catch (Exception e) {
            log.error("Failed to send test email with attachment", e);
            return ApiResponse.error("Failed to send test email with attachment: " + e.getMessage());
        }
    }

    @GetMapping("/config")
    @Operation(summary = "Get email configuration", description = "Get current email service configuration")
    public ApiResponse<String> getEmailConfig() {
        // This is just for debugging - in production, sensitive info should not be exposed
        return ApiResponse.success("Email service is configured. Check application.yml for details.");
    }
}
