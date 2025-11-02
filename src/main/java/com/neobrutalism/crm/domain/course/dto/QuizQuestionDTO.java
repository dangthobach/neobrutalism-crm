package com.neobrutalism.crm.domain.course.dto;

import com.neobrutalism.crm.common.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Quiz Question DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDTO {

    private UUID id;
    private String questionText;
    private QuestionType questionType;
    private Integer points;
    private Integer sortOrder;
    private String explanation;
    private List<String> options;

    // Correct answers are NOT included in responses for students
    // Only included for instructors or after quiz completion with showCorrectAnswers=true
    private List<String> correctAnswers;
}
