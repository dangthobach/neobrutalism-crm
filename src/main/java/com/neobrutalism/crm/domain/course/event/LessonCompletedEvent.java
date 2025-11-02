package com.neobrutalism.crm.domain.course.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a student completes a lesson
 */
@Getter
@Setter
public class LessonCompletedEvent extends DomainEvent {

    private UUID lessonProgressId;
    private UUID userId;
    private UUID lessonId;
    private String lessonTitle;
    private UUID courseId;
    private String courseTitle;
    private Instant completedAt;
    private Integer timeSpentSeconds;
    private String tenantId;

    public LessonCompletedEvent() {
        super();
    }

    public LessonCompletedEvent(UUID lessonProgressId, UUID userId, UUID lessonId, String lessonTitle,
                                UUID courseId, String courseTitle, Instant completedAt,
                                Integer timeSpentSeconds, String tenantId, String completedBy) {
        super("LESSON_COMPLETED", lessonProgressId.toString(), "LessonProgress", completedBy);
        this.lessonProgressId = lessonProgressId;
        this.userId = userId;
        this.lessonId = lessonId;
        this.lessonTitle = lessonTitle;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.completedAt = completedAt;
        this.timeSpentSeconds = timeSpentSeconds;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("lessonProgressId", lessonProgressId);
        payload.put("userId", userId);
        payload.put("lessonId", lessonId);
        payload.put("lessonTitle", lessonTitle);
        payload.put("courseId", courseId);
        payload.put("courseTitle", courseTitle);
        payload.put("completedAt", completedAt);
        payload.put("timeSpentSeconds", timeSpentSeconds);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
