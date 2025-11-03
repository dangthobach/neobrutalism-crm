package com.neobrutalism.crm.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Quiz Attempt DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptDTO {

    private UUID id;
    private UUID quizId;
    private String quizTitle;
    private UUID userId;
    private Integer attemptNumber;
    private Instant startedAt;
    private Instant submittedAt;
    private Double score;
    private Integer pointsEarned;
    private Integer totalPoints;
    private Boolean isPassed;
    private Integer timeSpentSeconds;
    private Integer timeRemainingSeconds;
    private String feedback;

    // Quiz details
    private Integer passingScore;
    private Integer timeLimitMinutes;
}
