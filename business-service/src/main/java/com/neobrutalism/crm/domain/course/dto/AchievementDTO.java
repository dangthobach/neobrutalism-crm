package com.neobrutalism.crm.domain.course.dto;

import com.neobrutalism.crm.common.enums.AchievementType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Achievement DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDTO {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private AchievementType achievementType;
    private String iconUrl;
    private Integer points;
    private String tier;
    private Integer requirementValue;
    private String requirementDescription;
    private Boolean isHidden;
    private UUID courseId;
    private String courseName;

    // User-specific (when fetched for a user)
    private Boolean isEarned;
    private Instant earnedAt;
    private Integer progress;
}
