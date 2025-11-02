package com.neobrutalism.crm.domain.course.model;

import com.neobrutalism.crm.common.entity.SoftDeletableEntity;
import com.neobrutalism.crm.common.enums.AchievementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Achievement entity - gamification badges and achievements
 */
@Entity
@Table(name = "achievements", indexes = {
    @Index(name = "idx_achievements_type", columnList = "achievement_type"),
    @Index(name = "idx_achievements_code", columnList = "code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Achievement extends SoftDeletableEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "achievement_type", nullable = false, length = 50)
    private AchievementType achievementType;

    @Column(name = "icon_url", length = 1000)
    private String iconUrl;

    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @Column(name = "requirement_value")
    private Integer requirementValue;

    @Column(name = "requirement_description", columnDefinition = "TEXT")
    private String requirementDescription;

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // Business methods

    /**
     * Check if achievement is course-specific
     */
    public boolean isCourseSpecific() {
        return course != null;
    }

    /**
     * Check if achievement is global
     */
    public boolean isGlobal() {
        return course == null;
    }

    /**
     * Hide achievement
     */
    public void hide() {
        this.isHidden = true;
    }

    /**
     * Show achievement
     */
    public void show() {
        this.isHidden = false;
    }

    /**
     * Check if user meets requirements
     */
    public boolean meetsRequirement(Integer userValue) {
        if (requirementValue == null) {
            return true;
        }
        return userValue != null && userValue >= requirementValue;
    }

    /**
     * Get achievement tier based on points
     */
    public String getTier() {
        if (points >= 100) {
            return "LEGENDARY";
        } else if (points >= 50) {
            return "GOLD";
        } else if (points >= 25) {
            return "SILVER";
        } else {
            return "BRONZE";
        }
    }

    /**
     * Check if achievement is rare
     */
    public boolean isRare() {
        return points >= 50;
    }
}
