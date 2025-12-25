package com.neobrutalism.crm.domain.course.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a student completes a course
 */
@Getter
@Setter
public class CourseCompletedEvent extends DomainEvent {

    private UUID enrollmentId;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID courseId;
    private String courseCode;
    private String courseTitle;
    private Instant completedAt;
    private Integer progressPercentage;
    private Long durationDays;
    private String tenantId;

    public CourseCompletedEvent() {
        super();
    }

    public CourseCompletedEvent(UUID enrollmentId, UUID userId, String userName, String userEmail,
                                UUID courseId, String courseCode, String courseTitle,
                                Instant completedAt, Integer progressPercentage, Long durationDays,
                                String tenantId, String completedBy) {
        super("COURSE_COMPLETED", enrollmentId.toString(), "Enrollment", completedBy);
        this.enrollmentId = enrollmentId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.completedAt = completedAt;
        this.progressPercentage = progressPercentage;
        this.durationDays = durationDays;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("enrollmentId", enrollmentId);
        payload.put("userId", userId);
        payload.put("userName", userName);
        payload.put("userEmail", userEmail);
        payload.put("courseId", courseId);
        payload.put("courseCode", courseCode);
        payload.put("courseTitle", courseTitle);
        payload.put("completedAt", completedAt);
        payload.put("progressPercentage", progressPercentage);
        payload.put("durationDays", durationDays);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
