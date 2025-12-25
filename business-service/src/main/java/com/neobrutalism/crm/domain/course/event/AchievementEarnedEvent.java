package com.neobrutalism.crm.domain.course.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a user earns an achievement
 */
@Getter
@Setter
public class AchievementEarnedEvent extends DomainEvent {

    private UUID userAchievementId;
    private UUID userId;
    private String userName;
    private UUID achievementId;
    private String achievementCode;
    private String achievementName;
    private Integer points;
    private Instant earnedAt;
    private String tenantId;

    public AchievementEarnedEvent() {
        super();
    }

    public AchievementEarnedEvent(UUID userAchievementId, UUID userId, String userName,
                                 UUID achievementId, String achievementCode, String achievementName,
                                 Integer points, Instant earnedAt,
                                 String tenantId, String earnedBy) {
        super("ACHIEVEMENT_EARNED", userAchievementId.toString(), "UserAchievement", earnedBy);
        this.userAchievementId = userAchievementId;
        this.userId = userId;
        this.userName = userName;
        this.achievementId = achievementId;
        this.achievementCode = achievementCode;
        this.achievementName = achievementName;
        this.points = points;
        this.earnedAt = earnedAt;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userAchievementId", userAchievementId);
        payload.put("userId", userId);
        payload.put("userName", userName);
        payload.put("achievementId", achievementId);
        payload.put("achievementCode", achievementCode);
        payload.put("achievementName", achievementName);
        payload.put("points", points);
        payload.put("earnedAt", earnedAt);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
