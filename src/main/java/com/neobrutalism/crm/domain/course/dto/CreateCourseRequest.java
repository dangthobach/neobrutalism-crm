package com.neobrutalism.crm.domain.course.dto;

import com.neobrutalism.crm.common.enums.CourseLevel;
import com.neobrutalism.crm.common.enums.MemberTier;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a course
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {

    @NotBlank(message = "Course code is required")
    @Size(max = 50, message = "Course code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    @Size(max = 500, message = "Slug must not exceed 500 characters")
    private String slug;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;

    @NotNull(message = "Course level is required")
    private CourseLevel courseLevel;

    private MemberTier tierRequired = MemberTier.FREE;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @NotNull(message = "Instructor ID is required")
    private UUID instructorId;

    private UUID categoryId;

    private String thumbnailUrl;
    private String previewVideoUrl;
    private String learningObjectives;
    private String prerequisites;
    private String targetAudience;
}
