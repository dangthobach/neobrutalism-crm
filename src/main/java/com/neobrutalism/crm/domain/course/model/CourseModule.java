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
 * Course Module entity - represents a module/section within a course
 */
@Entity
@Table(name = "course_modules", indexes = {
    @Index(name = "idx_modules_course", columnList = "course_id"),
    @Index(name = "idx_modules_sort", columnList = "course_id, sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseModule extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "is_preview", nullable = false)
    private Boolean isPreview = false;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<Lesson> lessons = new ArrayList<>();

    // Business methods

    /**
     * Get lesson count
     */
    public int getLessonCount() {
        return lessons != null ? lessons.size() : 0;
    }

    /**
     * Get total duration in minutes
     */
    public int getDurationMinutes() {
        if (lessons == null || lessons.isEmpty()) {
            return durationMinutes != null ? durationMinutes : 0;
        }
        return lessons.stream()
            .mapToInt(lesson -> lesson.getDurationMinutes() != null ? lesson.getDurationMinutes() : 0)
            .sum();
    }

    /**
     * Add lesson to module
     */
    public void addLesson(Lesson lesson) {
        this.lessons.add(lesson);
        lesson.setModule(this);
        lesson.setSortOrder(this.lessons.size());
    }

    /**
     * Remove lesson from module
     */
    public void removeLesson(Lesson lesson) {
        this.lessons.remove(lesson);
        lesson.setModule(null);
        reorderLessons();
    }

    /**
     * Reorder lessons after removal
     */
    private void reorderLessons() {
        for (int i = 0; i < this.lessons.size(); i++) {
            this.lessons.get(i).setSortOrder(i + 1);
        }
    }

    /**
     * Get lesson by index
     */
    public Lesson getLesson(int index) {
        if (index >= 0 && index < lessons.size()) {
            return lessons.get(index);
        }
        return null;
    }
}
