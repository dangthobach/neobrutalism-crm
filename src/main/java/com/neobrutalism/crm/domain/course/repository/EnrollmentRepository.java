package com.neobrutalism.crm.domain.course.repository;

import com.neobrutalism.crm.common.enums.EnrollmentStatus;
import com.neobrutalism.crm.domain.course.model.Enrollment;
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
 * Repository for Enrollment entity
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    /**
     * Find enrollment by user and course
     */
    Optional<Enrollment> findByUserIdAndCourseIdAndDeletedFalse(UUID userId, UUID courseId);

    /**
     * Find enrollments by user
     */
    Page<Enrollment> findByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    /**
     * Find enrollments by course
     */
    Page<Enrollment> findByCourseIdAndDeletedFalse(UUID courseId, Pageable pageable);

    /**
     * Find active enrollments by user
     */
    List<Enrollment> findByUserIdAndStatusAndDeletedFalse(UUID userId, EnrollmentStatus status);

    /**
     * Find active enrollments by course
     */
    List<Enrollment> findByCourseIdAndStatusAndDeletedFalse(UUID courseId, EnrollmentStatus status);

    /**
     * Find enrollments by status
     */
    Page<Enrollment> findByStatusAndDeletedFalse(EnrollmentStatus status, Pageable pageable);

    /**
     * Check if user is enrolled in course
     */
    boolean existsByUserIdAndCourseIdAndDeletedFalse(UUID userId, UUID courseId);

    /**
     * Count enrollments by course
     */
    long countByCourseIdAndDeletedFalse(UUID courseId);

    /**
     * Count active enrollments by course
     */
    long countByCourseIdAndStatusAndDeletedFalse(UUID courseId, EnrollmentStatus status);

    /**
     * Count enrollments by user
     */
    long countByUserIdAndDeletedFalse(UUID userId);

    /**
     * Find expired enrollments
     */
    @Query("SELECT e FROM Enrollment e WHERE e.expiresAt IS NOT NULL AND e.expiresAt < :now AND " +
           "e.status = 'ACTIVE' AND e.deleted = false")
    List<Enrollment> findExpiredEnrollments(@Param("now") LocalDateTime now);

    /**
     * Find enrollments expiring soon
     */
    @Query("SELECT e FROM Enrollment e WHERE e.expiresAt IS NOT NULL AND " +
           "e.expiresAt BETWEEN :now AND :expiryDate AND " +
           "e.status = 'ACTIVE' AND e.deleted = false")
    List<Enrollment> findEnrollmentsExpiringSoon(
        @Param("now") LocalDateTime now,
        @Param("expiryDate") LocalDateTime expiryDate
    );

    /**
     * Find completed enrollments without certificates
     */
    @Query("SELECT e FROM Enrollment e WHERE e.status = 'COMPLETED' AND " +
           "e.certificateIssuedAt IS NULL AND e.deleted = false")
    List<Enrollment> findCompletedWithoutCertificates();

    /**
     * Find enrollments by progress range
     */
    @Query("SELECT e FROM Enrollment e WHERE e.progressPercentage BETWEEN :minProgress AND :maxProgress AND " +
           "e.deleted = false")
    Page<Enrollment> findByProgressRange(
        @Param("minProgress") Integer minProgress,
        @Param("maxProgress") Integer maxProgress,
        Pageable pageable
    );

    /**
     * Find recently accessed enrollments
     */
    @Query("SELECT e FROM Enrollment e WHERE e.lastAccessedAt >= :since AND e.deleted = false " +
           "ORDER BY e.lastAccessedAt DESC")
    List<Enrollment> findRecentlyAccessed(@Param("since") LocalDateTime since);

    /**
     * Find inactive enrollments
     */
    @Query("SELECT e FROM Enrollment e WHERE e.lastAccessedAt < :inactiveSince AND " +
           "e.status = 'ACTIVE' AND e.deleted = false")
    List<Enrollment> findInactiveEnrollments(@Param("inactiveSince") LocalDateTime inactiveSince);
}
