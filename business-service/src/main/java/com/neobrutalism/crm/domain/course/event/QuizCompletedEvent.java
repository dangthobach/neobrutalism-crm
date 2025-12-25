package com.neobrutalism.crm.domain.course.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a student completes a quiz
 */
@Getter
@Setter
public class QuizCompletedEvent extends DomainEvent {

    private UUID quizAttemptId;
    private UUID userId;
    private UUID quizId;
    private String quizTitle;
    private UUID courseId;
    private String courseTitle;
    private Double score;
    private Boolean isPassed;
    private Integer attemptNumber;
    private Instant submittedAt;
    private String tenantId;

    public QuizCompletedEvent() {
        super();
    }

    public QuizCompletedEvent(UUID quizAttemptId, UUID userId, UUID quizId, String quizTitle,
                             UUID courseId, String courseTitle, Double score, Boolean isPassed,
                             Integer attemptNumber, Instant submittedAt,
                             String tenantId, String submittedBy) {
        super("QUIZ_COMPLETED", quizAttemptId.toString(), "QuizAttempt", submittedBy);
        this.quizAttemptId = quizAttemptId;
        this.userId = userId;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.score = score;
        this.isPassed = isPassed;
        this.attemptNumber = attemptNumber;
        this.submittedAt = submittedAt;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("quizAttemptId", quizAttemptId);
        payload.put("userId", userId);
        payload.put("quizId", quizId);
        payload.put("quizTitle", quizTitle);
        payload.put("courseId", courseId);
        payload.put("courseTitle", courseTitle);
        payload.put("score", score);
        payload.put("isPassed", isPassed);
        payload.put("attemptNumber", attemptNumber);
        payload.put("submittedAt", submittedAt);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
