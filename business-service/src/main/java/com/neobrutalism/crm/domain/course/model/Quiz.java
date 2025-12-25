package com.neobrutalism.crm.domain.course.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz entity for assessments
 */
@Entity
@Table(name = "quizzes", indexes = {
    @Index(name = "idx_quizzes_lesson", columnList = "lesson_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quiz extends SoftDeletableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "passing_score")
    private Integer passingScore = 70;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "max_attempts")
    private Integer maxAttempts;

    @Column(name = "is_randomized", nullable = false)
    private Boolean isRandomized = false;

    @Column(name = "show_correct_answers", nullable = false)
    private Boolean showCorrectAnswers = true;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    // Business methods

    /**
     * Get total points possible
     */
    public int getTotalPoints() {
        return questions.stream()
            .mapToInt(QuizQuestion::getPoints)
            .sum();
    }

    /**
     * Check if quiz has time limit
     */
    public boolean hasTimeLimit() {
        return timeLimitMinutes != null && timeLimitMinutes > 0;
    }

    /**
     * Check if attempts are limited
     */
    public boolean hasMaxAttempts() {
        return maxAttempts != null && maxAttempts > 0;
    }

    /**
     * Add question to quiz
     */
    public void addQuestion(QuizQuestion question) {
        this.questions.add(question);
        question.setQuiz(this);
        question.setSortOrder(this.questions.size());
    }

    /**
     * Remove question from quiz
     */
    public void removeQuestion(QuizQuestion question) {
        this.questions.remove(question);
        question.setQuiz(null);
        reorderQuestions();
    }

    /**
     * Reorder questions after removal
     */
    private void reorderQuestions() {
        for (int i = 0; i < this.questions.size(); i++) {
            this.questions.get(i).setSortOrder(i + 1);
        }
    }

    /**
     * Get question count
     */
    public int getQuestionCount() {
        return questions != null ? questions.size() : 0;
    }
}
