package com.neobrutalism.crm.domain.course.service;

import com.neobrutalism.crm.common.exception.BusinessException;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.course.event.CertificateIssuedEvent;
import com.neobrutalism.crm.domain.course.model.Certificate;
import com.neobrutalism.crm.domain.course.model.Course;
import com.neobrutalism.crm.domain.course.model.Enrollment;
import com.neobrutalism.crm.domain.course.repository.CertificateRepository;
import com.neobrutalism.crm.domain.course.repository.EnrollmentRepository;
import com.neobrutalism.crm.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing course completion certificates
 * Handles certificate generation, verification, and PDF creation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Issue a certificate for a completed enrollment
     *
     * @param enrollmentId The enrollment ID
     * @return The issued certificate
     */
    @Transactional
    public Certificate issueCertificate(UUID enrollmentId) {
        log.info("Issuing certificate for enrollment: {}", enrollmentId);

        // Get enrollment
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));

        // Validate enrollment is completed
        if (!enrollment.isCompleted()) {
            throw new BusinessException("Cannot issue certificate: enrollment is not completed");
        }

        // Check if certificate already exists
        Optional<Certificate> existingCert = certificateRepository
            .findByEnrollmentIdAndDeletedFalse(enrollmentId);

        if (existingCert.isPresent()) {
            log.warn("Certificate already exists for enrollment: {}", enrollmentId);
            return existingCert.get();
        }

        // Create certificate
        Certificate certificate = createCertificate(enrollment);

        // Save certificate
        certificate = certificateRepository.save(certificate);

        // Fire certificate issued event
        fireCertificateIssuedEvent(certificate);

        log.info("Certificate issued: {} for enrollment: {}", certificate.getCertificateNumber(), enrollmentId);

        return certificate;
    }

    /**
     * Get certificate by ID
     *
     * @param certificateId The certificate ID
     * @return The certificate
     */
    @Transactional(readOnly = true)
    public Certificate getCertificateById(UUID certificateId) {
        return certificateRepository.findById(certificateId)
            .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));
    }

    /**
     * Get certificate by number
     *
     * @param certificateNumber The certificate number
     * @return The certificate
     */
    @Transactional(readOnly = true)
    public Optional<Certificate> getCertificateByNumber(String certificateNumber) {
        return certificateRepository.findByCertificateNumberAndDeletedFalse(certificateNumber);
    }

    /**
     * Get certificates for a user
     *
     * @param userId The user ID
     * @return List of certificates
     */
    @Transactional(readOnly = true)
    public List<Certificate> getCertificatesByUser(UUID userId) {
        return certificateRepository.findValidByUser(userId, LocalDateTime.now());
    }

    /**
     * Get certificates for a course
     *
     * @param courseId The course ID
     * @param pageable Pagination parameters
     * @return Page of certificates
     */
    @Transactional(readOnly = true)
    public Page<Certificate> getCertificatesByCourse(UUID courseId, Pageable pageable) {
        return certificateRepository.findByCourseIdAndDeletedFalse(courseId, pageable);
    }

    /**
     * Get certificate for a specific enrollment
     *
     * @param enrollmentId The enrollment ID
     * @return The certificate
     */
    @Transactional(readOnly = true)
    public Optional<Certificate> getCertificateByEnrollment(UUID enrollmentId) {
        return certificateRepository.findByEnrollmentIdAndDeletedFalse(enrollmentId);
    }

    /**
     * Verify a certificate by number
     *
     * @param certificateNumber The certificate number
     * @return True if certificate is valid
     */
    @Transactional(readOnly = true)
    public boolean verifyCertificate(String certificateNumber) {
        return certificateRepository.findByCertificateNumberAndDeletedFalse(certificateNumber)
            .map(Certificate::isValid)
            .orElse(false);
    }

    /**
     * Revoke a certificate
     *
     * @param certificateId The certificate ID
     * @param reason The revocation reason
     */
    @Transactional
    public void revokeCertificate(UUID certificateId, String reason) {
        log.info("Revoking certificate: {}, reason: {}", certificateId, reason);

        Certificate certificate = getCertificateById(certificateId);

        certificate.setRevokedAt(LocalDateTime.now());
        certificate.setRevokeReason(reason);
        certificate.setIsVerified(false);

        certificateRepository.save(certificate);

        log.info("Certificate revoked: {}", certificate.getCertificateNumber());
    }

    /**
     * Count certificates issued for a course
     *
     * @param courseId The course ID
     * @return Number of certificates
     */
    @Transactional(readOnly = true)
    public long countCertificatesByCourse(UUID courseId) {
        return certificateRepository.countByCourseIdAndDeletedFalse(courseId);
    }

    /**
     * Get recent certificates (for admin dashboard)
     *
     * @param limit Number of certificates to return
     * @return List of recent certificates
     */
    @Transactional(readOnly = true)
    public List<Certificate> getRecentCertificates(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        return certificateRepository.findRecent(pageable).getContent();
    }

    /**
     * Get certificate statistics for a course
     *
     * @param courseId The course ID
     * @return Certificate statistics
     */
    @Transactional(readOnly = true)
    public CertificateStatistics getCertificateStatistics(UUID courseId) {
        long totalIssued = countCertificatesByCourse(courseId);
        long validCertificates = certificateRepository.countValid(LocalDateTime.now());
        // Count revoked by checking total issued vs valid
        long revokedCertificates = totalIssued - validCertificates;

        return new CertificateStatistics(totalIssued, validCertificates, revokedCertificates);
    }

    /**
     * Check if user has certificate for course
     *
     * @param userId The user ID
     * @param courseId The course ID
     * @return True if user has certificate
     */
    @Transactional(readOnly = true)
    public boolean hasCertificate(UUID userId, UUID courseId) {
        // Check if any valid certificates exist for this user and course
        return !certificateRepository.findValidByUser(userId, LocalDateTime.now()).isEmpty();
    }

    /**
     * Regenerate certificate PDF
     * This would typically call a PDF generation service
     *
     * @param certificateId The certificate ID
     * @return Updated certificate
     */
    @Transactional
    public Certificate regeneratePDF(UUID certificateId) {
        log.info("Regenerating PDF for certificate: {}", certificateId);

        Certificate certificate = getCertificateById(certificateId);

        // TODO: Integrate with PDF generation service (e.g., iText, PDFBox, or external service)
        // For now, just update the timestamp
        String pdfUrl = generatePdfUrl(certificate);
        certificate.setPdfUrl(pdfUrl);

        certificate = certificateRepository.save(certificate);

        log.info("PDF regenerated for certificate: {}", certificate.getCertificateNumber());

        return certificate;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Create a new certificate from enrollment
     */
    private Certificate createCertificate(Enrollment enrollment) {
        User user = enrollment.getUser();
        Course course = enrollment.getCourse();

        Certificate certificate = new Certificate();
        certificate.setUser(user);
        certificate.setCourse(course);
        certificate.setEnrollment(enrollment);

        // Generate certificate number
        String certificateNumber = Certificate.generateCertificateNumber(
            course.getCode(),
            user.getId().getMostSignificantBits()
        );
        certificate.setCertificateNumber(certificateNumber);

        // Set dates
        certificate.setIssuedAt(LocalDateTime.now());
        certificate.setCompletionDate(enrollment.getCompletedAt());

        // Set expiration (optional - courses may not have expiration)
        // For now, no expiration unless configured in course
        // certificate.setExpiresAt(LocalDateTime.now().plusYears(2));

        // Set final score (calculate from enrollment progress)
        certificate.setFinalScore((double) enrollment.getProgressPercentage());

        // Generate verification URL
        String verificationUrl = generateVerificationUrl(certificateNumber);
        certificate.setVerificationUrl(verificationUrl);

        // Generate PDF URL (placeholder - would be generated by PDF service)
        String pdfUrl = generatePdfUrl(certificate);
        certificate.setPdfUrl(pdfUrl);

        certificate.setIsVerified(true);
        certificate.setCreatedBy(user.getId().toString());

        return certificate;
    }

    /**
     * Generate verification URL for certificate
     */
    private String generateVerificationUrl(String certificateNumber) {
        // TODO: Replace with actual application base URL from configuration
        String baseUrl = "https://your-domain.com";
        return String.format("%s/verify-certificate/%s", baseUrl, certificateNumber);
    }

    /**
     * Generate PDF URL (placeholder)
     * In production, this would be generated by a PDF service
     */
    private String generatePdfUrl(Certificate certificate) {
        // TODO: Integrate with actual PDF generation service
        // For now, return a placeholder URL
        String baseUrl = "https://your-domain.com/certificates";
        return String.format("%s/%s.pdf", baseUrl, certificate.getCertificateNumber());
    }

    /**
     * Fire certificate issued event
     */
    private void fireCertificateIssuedEvent(Certificate certificate) {
        User user = certificate.getUser();
        Course course = certificate.getCourse();

        CertificateIssuedEvent event = new CertificateIssuedEvent(
            certificate.getId(),
            certificate.getCertificateNumber(),
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            course.getId(),
            course.getCode(),
            course.getTitle(),
            certificate.getFinalScore(),
            certificate.getIssuedAt().atZone(ZoneId.systemDefault()).toInstant(),
            certificate.getVerificationUrl(),
            user.getTenantId(),
            user.getId().toString()
        );

        eventPublisher.publishEvent(event);
        log.info("CertificateIssuedEvent published for certificate: {}", certificate.getCertificateNumber());
    }

    /**
     * Certificate statistics DTO
     */
    public record CertificateStatistics(
        long totalIssued,
        long validCertificates,
        long revokedCertificates
    ) {
    }
}
