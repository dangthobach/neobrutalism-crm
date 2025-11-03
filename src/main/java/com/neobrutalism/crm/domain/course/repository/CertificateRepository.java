package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.domain.course.model.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Certificate entity
 */
@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    /**
     * Find by certificate number
     */
    Optional<Certificate> findByCertificateNumberAndDeletedFalse(String certificateNumber);

    /**
     * Find by enrollment
     */
    Optional<Certificate> findByEnrollmentIdAndDeletedFalse(UUID enrollmentId);

    /**
     * Find certificates by user
     */
    Page<Certificate> findByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    /**
     * Find certificates by course
     */
    Page<Certificate> findByCourseIdAndDeletedFalse(UUID courseId, Pageable pageable);

    /**
     * Find valid certificates by user
     */
    @Query("SELECT c FROM Certificate c WHERE c.user.id = :userId AND c.revokedAt IS NULL AND " +
           "(c.expiresAt IS NULL OR c.expiresAt > :now) AND c.deleted = false")
    List<Certificate> findValidByUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    /**
     * Find expired certificates
     */
    @Query("SELECT c FROM Certificate c WHERE c.expiresAt IS NOT NULL AND c.expiresAt < :now AND " +
           "c.revokedAt IS NULL AND c.deleted = false")
    List<Certificate> findExpired(@Param("now") LocalDateTime now);

    /**
     * Find certificates expiring soon
     */
    @Query("SELECT c FROM Certificate c WHERE c.expiresAt IS NOT NULL AND " +
           "c.expiresAt BETWEEN :now AND :expiryDate AND " +
           "c.revokedAt IS NULL AND c.deleted = false")
    List<Certificate> findExpiringSoon(
        @Param("now") LocalDateTime now,
        @Param("expiryDate") LocalDateTime expiryDate
    );

    /**
     * Find revoked certificates
     */
    @Query("SELECT c FROM Certificate c WHERE c.revokedAt IS NOT NULL AND c.deleted = false")
    Page<Certificate> findRevoked(Pageable pageable);

    /**
     * Find verified certificates
     */
    Page<Certificate> findByIsVerifiedTrueAndDeletedFalse(Pageable pageable);

    /**
     * Find certificates without PDF
     */
    @Query("SELECT c FROM Certificate c WHERE c.pdfUrl IS NULL AND c.deleted = false")
    List<Certificate> findWithoutPdf();

    /**
     * Count certificates by user
     */
    long countByUserIdAndDeletedFalse(UUID userId);

    /**
     * Count certificates by course
     */
    long countByCourseIdAndDeletedFalse(UUID courseId);

    /**
     * Count valid certificates
     */
    @Query("SELECT COUNT(c) FROM Certificate c WHERE c.revokedAt IS NULL AND " +
           "(c.expiresAt IS NULL OR c.expiresAt > :now) AND c.deleted = false")
    long countValid(@Param("now") LocalDateTime now);

    /**
     * Check if certificate number exists
     */
    boolean existsByCertificateNumberAndDeletedFalse(String certificateNumber);

    /**
     * Find certificates by date range
     */
    @Query("SELECT c FROM Certificate c WHERE c.issuedAt BETWEEN :startDate AND :endDate AND " +
           "c.deleted = false ORDER BY c.issuedAt DESC")
    Page<Certificate> findByIssuedDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Find recent certificates
     */
    @Query("SELECT c FROM Certificate c WHERE c.deleted = false ORDER BY c.issuedAt DESC")
    Page<Certificate> findRecent(Pageable pageable);

    /**
     * Verify certificate
     */
    @Query("SELECT c FROM Certificate c WHERE c.certificateNumber = :certificateNumber AND " +
           "c.isVerified = true AND c.revokedAt IS NULL AND " +
           "(c.expiresAt IS NULL OR c.expiresAt > :now) AND c.deleted = false")
    Optional<Certificate> verifyCertificate(
        @Param("certificateNumber") String certificateNumber,
        @Param("now") LocalDateTime now
    );
}
