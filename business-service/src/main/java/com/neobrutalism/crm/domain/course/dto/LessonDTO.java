package com.neobrutalism.crm.domain.course.dto;

import com.neobrutalism.crm.common.enums.LessonType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Lesson DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonDTO {

    private UUID id;
    private String title;
    private String description;
    private LessonType lessonType;
    private String content;
    private Integer sortOrder;
    private Integer durationMinutes;

    // Video-specific
    private String videoUrl;
    private Integer videoDurationSeconds;

    // Document-specific
    private String documentUrl;

    // Quiz-specific
    private UUID quizId;
    private String quizTitle;

    private Instant createdAt;
    private Instant updatedAt;

    // Progress (when fetched for a specific user)
    private Boolean isCompleted;
    private Integer completionPercentage;
    private Integer lastPositionSeconds;
}
