package com.neobrutalism.crm.domain.course.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a course is published
 */
@Getter
@Setter
public class CoursePublishedEvent extends DomainEvent {

    private UUID courseId;
    private String courseCode;
    private String courseTitle;
    private UUID instructorId;
    private String instructorName;
    private Instant publishedAt;
    private String tenantId;

    public CoursePublishedEvent() {
        super();
    }

    public CoursePublishedEvent(UUID courseId, String courseCode, String courseTitle,
                                UUID instructorId, String instructorName,
                                Instant publishedAt, String tenantId, String publishedBy) {
        super("COURSE_PUBLISHED", courseId.toString(), "Course", publishedBy);
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.publishedAt = publishedAt;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("courseId", courseId);
        payload.put("courseCode", courseCode);
        payload.put("courseTitle", courseTitle);
        payload.put("instructorId", instructorId);
        payload.put("instructorName", instructorName);
        payload.put("publishedAt", publishedAt);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
