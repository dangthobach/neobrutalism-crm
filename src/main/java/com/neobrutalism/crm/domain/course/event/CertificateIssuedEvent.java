package com.neobrutalism.crm.domain.course.event;

import com.neobrutalism.crm.common.event.DomainEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event fired when a certificate is issued
 */
@Getter
@Setter
public class CertificateIssuedEvent extends DomainEvent {

    private UUID certificateId;
    private String certificateNumber;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID courseId;
    private String courseCode;
    private String courseTitle;
    private Double finalScore;
    private Instant issuedAt;
    private String verificationUrl;
    private String tenantId;

    public CertificateIssuedEvent() {
        super();
    }

    public CertificateIssuedEvent(UUID certificateId, String certificateNumber,
                                 UUID userId, String userName, String userEmail,
                                 UUID courseId, String courseCode, String courseTitle,
                                 Double finalScore, Instant issuedAt, String verificationUrl,
                                 String tenantId, String issuedBy) {
        super("CERTIFICATE_ISSUED", certificateId.toString(), "Certificate", issuedBy);
        this.certificateId = certificateId;
        this.certificateNumber = certificateNumber;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.finalScore = finalScore;
        this.issuedAt = issuedAt;
        this.verificationUrl = verificationUrl;
        this.tenantId = tenantId;
    }

    @Override
    public Object getPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("certificateId", certificateId);
        payload.put("certificateNumber", certificateNumber);
        payload.put("userId", userId);
        payload.put("userName", userName);
        payload.put("userEmail", userEmail);
        payload.put("courseId", courseId);
        payload.put("courseCode", courseCode);
        payload.put("courseTitle", courseTitle);
        payload.put("finalScore", finalScore);
        payload.put("issuedAt", issuedAt);
        payload.put("verificationUrl", verificationUrl);
        payload.put("tenantId", tenantId);
        return payload;
    }
}
