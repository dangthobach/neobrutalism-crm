package com.neobrutalism.crm.domain.course.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import com.neobrutalism.crm.common.enums.LessonType;
import com.neobrutalism.crm.domain.attachment.model.Attachment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

/**
 * Lesson entity - individual learning unit within a module
 */
@Entity
@Table(name = "lessons", indexes = {
    @Index(name = "idx_lessons_module", columnList = "module_id"),
    @Index(name = "idx_lessons_type", columnList = "lesson_type"),
    @Index(name = "idx_lessons_sort", columnList = "module_id, sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lesson extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private CourseModule module;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 50)
    private LessonType lessonType = LessonType.TEXT;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "video_duration_seconds")
    private Integer videoDurationSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private Attachment attachment;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "is_preview", nullable = false)
    private Boolean isPreview = false;

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private Quiz quiz;

    // Business methods

    /**
     * Check if lesson is a video
     */
    public boolean isVideo() {
        return this.lessonType == LessonType.VIDEO;
    }

    /**
     * Check if lesson is a quiz
     */
    public boolean isQuiz() {
        return this.lessonType == LessonType.QUIZ;
    }

    /**
     * Check if lesson has quiz
     */
    public boolean hasQuiz() {
        return this.quiz != null;
    }

    /**
     * Get video duration in minutes
     */
    public int getVideoDurationMinutes() {
        if (videoDurationSeconds != null) {
            return (int) Math.ceil(videoDurationSeconds / 60.0);
        }
        return 0;
    }

    /**
     * Set quiz for this lesson
     */
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        if (quiz != null) {
            quiz.setLesson(this);
        }
    }
}
