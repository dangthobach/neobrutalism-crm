package com.neobrutalism.crm.domain.course.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a student reviews a course
 */
@Getter
@Setter
public class CourseReviewedEvent extends DomainEvent {

    private UUID reviewId;
    private UUID userId;
    private String userName;
    private UUID courseId;
    private String courseCode;
    private String courseTitle;
    private Integer rating;
    private String reviewTitle;
    private Boolean isVerifiedPurchase;
    private Instant reviewedAt;
    private String tenantId;

    public CourseReviewedEvent() {
        super();
    }

    public CourseReviewedEvent(UUID reviewId, UUID userId, String userName,
                              UUID courseId, String courseCode, String courseTitle,
                              Integer rating, String reviewTitle, Boolean isVerifiedPurchase,
                              Instant reviewedAt, String tenantId, String reviewedBy) {
        super("COURSE_REVIEWED", reviewId.toString(), "CourseReview", reviewedBy);
        this.reviewId = reviewId;
        this.userId = userId;
        this.userName = userName;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.rating = rating;
        this.reviewTitle = reviewTitle;
        this.isVerifiedPurchase = isVerifiedPurchase;
        this.reviewedAt = reviewedAt;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("reviewId", reviewId);
        payload.put("userId", userId);
        payload.put("userName", userName);
        payload.put("courseId", courseId);
        payload.put("courseCode", courseCode);
        payload.put("courseTitle", courseTitle);
        payload.put("rating", rating);
        payload.put("reviewTitle", reviewTitle);
        payload.put("isVerifiedPurchase", isVerifiedPurchase);
        payload.put("reviewedAt", reviewedAt);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
