package com.neobrutalism.crm.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Quiz DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizDTO {

    private UUID id;
    private UUID lessonId;
    private String title;
    private String description;
    private Integer passingScore;
    private Integer timeLimitMinutes;
    private Integer maxAttempts;
    private Boolean showCorrectAnswers;
    private Boolean randomizeQuestions;
    private Integer questionCount;
    private Integer totalPoints;
    private Instant createdAt;
    private Instant updatedAt;

    // Questions (optional, for detailed view)
    private List<QuizQuestionDTO> questions;

    // User-specific data (when fetched for a user)
    private Integer attemptCount;
    private Double bestScore;
    private Boolean hasPassed;
}
