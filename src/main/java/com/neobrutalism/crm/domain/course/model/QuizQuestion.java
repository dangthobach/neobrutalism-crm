package com.neobrutalism.crm.domain.course.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import com.neobrutalism.crm.common.enums.QuestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz question entity
 */
@Entity
@Table(name = "quiz_questions", indexes = {
    @Index(name = "idx_quiz_questions_quiz", columnList = "quiz_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestion extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 50)
    private QuestionType questionType;

    @Column(name = "points", nullable = false)
    private Integer points = 1;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @ElementCollection
    @CollectionTable(
        name = "quiz_question_options",
        joinColumns = @JoinColumn(name = "question_id"),
        indexes = @Index(name = "idx_question_options", columnList = "question_id")
    )
    @OrderColumn(name = "option_order")
    @Column(name = "option_text", length = 1000)
    private List<String> options = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "quiz_question_correct_answers",
        joinColumns = @JoinColumn(name = "question_id"),
        indexes = @Index(name = "idx_question_answers", columnList = "question_id")
    )
    @Column(name = "answer", length = 1000)
    private List<String> correctAnswers = new ArrayList<>();

    // Business methods

    /**
     * Check if answer is correct
     */
    public boolean isCorrectAnswer(String answer) {
        return correctAnswers.stream()
            .anyMatch(correct -> correct.trim().equalsIgnoreCase(answer.trim()));
    }

    /**
     * Check if multiple answers are correct
     */
    public boolean isCorrectAnswers(List<String> answers) {
        if (answers == null || answers.size() != correctAnswers.size()) {
            return false;
        }
        return answers.stream()
            .allMatch(this::isCorrectAnswer);
    }

    /**
     * Add option to question
     */
    public void addOption(String option) {
        this.options.add(option);
    }

    /**
     * Remove option from question
     */
    public void removeOption(String option) {
        this.options.remove(option);
    }

    /**
     * Add correct answer
     */
    public void addCorrectAnswer(String answer) {
        this.correctAnswers.add(answer);
    }

    /**
     * Check if question has multiple correct answers
     */
    public boolean hasMultipleCorrectAnswers() {
        return correctAnswers.size() > 1;
    }

    /**
     * Check if question requires text input
     */
    public boolean requiresTextInput() {
        return questionType == QuestionType.SHORT_ANSWER ||
               questionType == QuestionType.ESSAY;
    }

    /**
     * Get option count
     */
    public int getOptionCount() {
        return options != null ? options.size() : 0;
    }
}
