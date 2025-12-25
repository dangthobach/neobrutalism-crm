package com.neobrutalism.crm.domain.course.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for submitting quiz answers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitQuizRequest {

    @NotNull(message = "Attempt ID is required")
    private UUID attemptId;

    @NotEmpty(message = "Answers are required")
    private Map<String, String> answers; // questionId -> answer
}
