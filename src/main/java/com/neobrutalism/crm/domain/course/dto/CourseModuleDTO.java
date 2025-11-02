package com.neobrutalism.crm.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Course Module DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseModuleDTO {

    private UUID id;
    private String title;
    private String description;
    private Integer sortOrder;
    private Integer durationMinutes;
    private Integer lessonCount;
    private Instant createdAt;
    private Instant updatedAt;

    // Lessons (optional, for detailed view)
    private List<LessonDTO> lessons;
}
