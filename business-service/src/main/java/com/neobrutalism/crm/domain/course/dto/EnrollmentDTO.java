package com.neobrutalism.crm.domain.course.dto;

import com.neobrutalism.crm.common.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Enrollment DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {

    private UUID id;
    private UUID userId;
    private String userName;
    private UUID courseId;
    private String courseCode;
    private String courseTitle;
    private String courseThumbnailUrl;
    private EnrollmentStatus status;
    private Instant enrolledAt;
    private Instant completedAt;
    private Instant expiresAt;
    private Integer progressPercentage;
    private Instant lastAccessedAt;
    private BigDecimal pricePaid;
    private Instant certificateIssuedAt;
    private String notes;

    // Stats
    private Integer completedLessons;
    private Integer totalLessons;
    private Integer timeSpentMinutes;
}
