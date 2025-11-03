package com.neobrutalism.crm.domain.course.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a student enrolls in a course
 */
@Getter
@Setter
public class StudentEnrolledEvent extends DomainEvent {

    private UUID enrollmentId;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID courseId;
    private String courseCode;
    private String courseTitle;
    private BigDecimal pricePaid;
    private Instant enrolledAt;
    private String tenantId;

    public StudentEnrolledEvent() {
        super();
    }

    public StudentEnrolledEvent(UUID enrollmentId, UUID userId, String userName, String userEmail,
                                UUID courseId, String courseCode, String courseTitle,
                                BigDecimal pricePaid, Instant enrolledAt,
                                String tenantId, String enrolledBy) {
        super("STUDENT_ENROLLED", enrollmentId.toString(), "Enrollment", enrolledBy);
        this.enrollmentId = enrollmentId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.pricePaid = pricePaid;
        this.enrolledAt = enrolledAt;
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
        payload.put("pricePaid", pricePaid);
        payload.put("enrolledAt", enrolledAt);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
