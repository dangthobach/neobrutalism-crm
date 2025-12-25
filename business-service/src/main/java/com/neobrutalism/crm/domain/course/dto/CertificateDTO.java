package com.neobrutalism.crm.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Certificate DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDTO {

    private UUID id;
    private String certificateNumber;
    private UUID userId;
    private String userName;
    private UUID courseId;
    private String courseCode;
    private String courseTitle;
    private Double finalScore;
    private Instant completionDate;
    private Instant issuedAt;
    private Instant expiresAt;
    private String pdfUrl;
    private String verificationUrl;
    private Boolean isVerified;
    private Boolean isValid;
}
