package com.neobrutalism.crm.domain.course.dto;

import com.neobrutalism.crm.common.enums.CourseLevel;
import com.neobrutalism.crm.common.enums.CourseStatus;
import com.neobrutalism.crm.common.enums.MemberTier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Course DTO for API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {

    private UUID id;
    private String code;
    private String title;
    private String slug;
    private String description;
    private String shortDescription;
    private CourseLevel courseLevel;
    private CourseStatus status;
    private MemberTier tierRequired;
    private BigDecimal price;
    private String thumbnailUrl;
    private String previewVideoUrl;

    // Instructor info
    private UUID instructorId;
    private String instructorName;

    // Category info
    private UUID categoryId;
    private String categoryName;

    // Stats
    private Integer enrollmentCount;
    private Integer completionCount;
    private BigDecimal ratingAverage;
    private Integer ratingCount;
    private Integer moduleCount;
    private Integer lessonCount;
    private Integer totalDurationMinutes;

    // Timestamps
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Detailed view
    private List<CourseModuleDTO> modules;
    private String learningObjectives;
    private String prerequisites;
    private String targetAudience;
}
