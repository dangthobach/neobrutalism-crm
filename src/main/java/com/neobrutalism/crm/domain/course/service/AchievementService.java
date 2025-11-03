package com.neobrutalism.crm.domain.course.service;

import com.neobrutalism.crm.common.enums.AchievementType;
import com.neobrutalism.crm.common.exception.ResourceNotFoundException;
import com.neobrutalism.crm.domain.course.event.AchievementEarnedEvent;
import com.neobrutalism.crm.domain.course.model.Achievement;
import com.neobrutalism.crm.domain.course.model.UserAchievement;
import com.neobrutalism.crm.domain.course.repository.AchievementRepository;
import com.neobrutalism.crm.domain.course.repository.UserAchievementRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
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
 * Service for managing achievements and user achievements
 * Handles achievement awarding, tracking, and gamification
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== Achievement Management ====================

    /**
     * Get all achievements
     */
    @Transactional(readOnly = true)
    public List<Achievement> getAllAchievements() {
        return achievementRepository.findAll();
    }

    /**
     * Get visible achievements only
     */
    @Transactional(readOnly = true)
    public Page<Achievement> getVisibleAchievements(Pageable pageable) {
        return achievementRepository.findVisible(pageable);
    }

    /**
     * Get achievements by type
     */
    @Transactional(readOnly = true)
    public List<Achievement> getAchievementsByType(AchievementType type) {
        return achievementRepository.findByAchievementTypeAndDeletedFalse(type);
    }

    /**
     * Get achievement by code
     */
    @Transactional(readOnly = true)
    public Optional<Achievement> getAchievementByCode(String code) {
        return achievementRepository.findByCodeAndDeletedFalse(code);
    }

    /**
     * Get achievement by ID
     */
    @Transactional(readOnly = true)
    public Achievement getAchievementById(UUID achievementId) {
        return achievementRepository.findById(achievementId)
            .orElseThrow(() -> new ResourceNotFoundException("Achievement not found: " + achievementId));
    }

    /**
     * Get achievements for a course
     */
    @Transactional(readOnly = true)
    public List<Achievement> getAchievementsByCourse(UUID courseId) {
        return achievementRepository.findByCourse(courseId);
    }

    /**
     * Get global achievements
     */
    @Transactional(readOnly = true)
    public List<Achievement> getGlobalAchievements() {
        return achievementRepository.findGlobalAchievements();
    }

    // ==================== User Achievement Management ====================

    /**
     * Award achievement to user
     *
     * @param userId The user ID
     * @param achievementCode The achievement code
     * @return The user achievement
     */
    @Transactional
    public UserAchievement awardAchievement(UUID userId, String achievementCode) {
        log.info("Awarding achievement {} to user {}", achievementCode, userId);

        // Get user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Get achievement
        Achievement achievement = achievementRepository.findByCodeAndDeletedFalse(achievementCode)
            .orElseThrow(() -> new ResourceNotFoundException("Achievement not found: " + achievementCode));

        // Check if user already has this achievement
        Optional<UserAchievement> existing = userAchievementRepository
            .findByUserIdAndAchievementIdAndDeletedFalse(userId, achievement.getId());

        if (existing.isPresent()) {
            log.warn("User {} already has achievement {}", userId, achievementCode);
            return existing.get();
        }

        // Create user achievement
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUser(user);
        userAchievement.setAchievement(achievement);
        userAchievement.setEarnedAt(LocalDateTime.now());
        userAchievement.setProgress(100); // Fully earned
        userAchievement.setCreatedBy(userId.toString());

        userAchievement = userAchievementRepository.save(userAchievement);

        // Fire achievement earned event
        fireAchievementEarnedEvent(userAchievement);

        log.info("Achievement {} awarded to user {}", achievementCode, userId);

        return userAchievement;
    }

    /**
     * Award achievement by ID
     */
    @Transactional
    public UserAchievement awardAchievementById(UUID userId, UUID achievementId) {
        Achievement achievement = getAchievementById(achievementId);
        return awardAchievement(userId, achievement.getCode());
    }

    /**
     * Update achievement progress
     *
     * @param userId The user ID
     * @param achievementCode The achievement code
     * @param progress The progress percentage (0-100)
     */
    @Transactional
    public UserAchievement updateAchievementProgress(UUID userId, String achievementCode, int progress) {
        log.debug("Updating achievement progress: user={}, achievement={}, progress={}",
            userId, achievementCode, progress);

        Achievement achievement = achievementRepository.findByCodeAndDeletedFalse(achievementCode)
            .orElseThrow(() -> new ResourceNotFoundException("Achievement not found: " + achievementCode));

        // Get or create user achievement
        UserAchievement userAchievement = userAchievementRepository
            .findByUserIdAndAchievementIdAndDeletedFalse(userId, achievement.getId())
            .orElseGet(() -> createUserAchievement(userId, achievement.getId()));

        // Update progress
        userAchievement.setProgress(Math.min(progress, 100));

        // If progress reaches 100%, mark as earned
        if (progress >= 100 && userAchievement.getEarnedAt() == null) {
            userAchievement.setEarnedAt(LocalDateTime.now());
            userAchievement = userAchievementRepository.save(userAchievement);

            // Fire event when achievement is earned
            fireAchievementEarnedEvent(userAchievement);

            log.info("Achievement {} earned by user {}", achievementCode, userId);
        } else {
            userAchievement = userAchievementRepository.save(userAchievement);
        }

        return userAchievement;
    }

    /**
     * Get user achievements
     */
    @Transactional(readOnly = true)
    public Page<UserAchievement> getUserAchievements(UUID userId, Pageable pageable) {
        return userAchievementRepository.findByUserIdAndDeletedFalse(userId, pageable);
    }

    /**
     * Get earned achievements for user (progress = 100)
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getEarnedAchievements(UUID userId) {
        return userAchievementRepository.findByUserAndProgress(userId, 100);
    }

    /**
     * Get achievements in progress for user (progress < 100)
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getAchievementsInProgress(UUID userId) {
        // Get all achievements with progress >= 1 and filter those < 100
        return userAchievementRepository.findByUserAndProgress(userId, 1).stream()
            .filter(ua -> ua.getProgress() < 100)
            .toList();
    }

    /**
     * Get user achievement by achievement code
     */
    @Transactional(readOnly = true)
    public Optional<UserAchievement> getUserAchievementByCode(UUID userId, String achievementCode) {
        return achievementRepository.findByCodeAndDeletedFalse(achievementCode)
            .flatMap(achievement -> userAchievementRepository
                .findByUserIdAndAchievementIdAndDeletedFalse(userId, achievement.getId()));
    }

    /**
     * Check if user has achievement
     */
    @Transactional(readOnly = true)
    public boolean hasAchievement(UUID userId, String achievementCode) {
        return getUserAchievementByCode(userId, achievementCode)
            .map(ua -> ua.getEarnedAt() != null)
            .orElse(false);
    }

    /**
     * Get total points for user
     */
    @Transactional(readOnly = true)
    public int getTotalPoints(UUID userId) {
        return userAchievementRepository.getTotalPointsByUser(userId);
    }

    /**
     * Get achievement count for user
     */
    @Transactional(readOnly = true)
    public long getAchievementCount(UUID userId) {
        return userAchievementRepository.countByUserIdAndDeletedFalse(userId);
    }

    /**
     * Get user achievement statistics
     */
    @Transactional(readOnly = true)
    public UserAchievementStats getUserStats(UUID userId) {
        long totalAchievements = achievementRepository.count();
        long earnedCount = getEarnedAchievements(userId).size();
        int totalPoints = getTotalPoints(userId);
        long inProgressCount = getAchievementsInProgress(userId).size();

        return new UserAchievementStats(
            earnedCount,
            totalAchievements,
            totalPoints,
            inProgressCount
        );
    }

    /**
     * Get recent achievements for user
     */
    @Transactional(readOnly = true)
    public List<UserAchievement> getRecentAchievements(UUID userId, int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        return userAchievementRepository.findRecentByUser(userId, pageable).getContent();
    }

    /**
     * Get leaderboard (top users by achievement points)
     * Note: This is a simplified implementation
     */
    @Transactional(readOnly = true)
    public List<UserLeaderboardEntry> getLeaderboard(int limit) {
        // TODO: Implement custom repository method for full leaderboard functionality
        // For now, return empty list - would need GROUP BY and ORDER BY total points
        log.warn("Leaderboard not fully implemented yet");
        return List.of();
    }

    // ==================== Achievement Tracking ====================

    /**
     * Track course completion achievement
     */
    @Transactional
    public void trackCourseCompletion(UUID userId, UUID courseId) {
        log.info("Tracking course completion achievement for user {} and course {}", userId, courseId);

        // Award course completion achievement
        String achievementCode = "COURSE_COMPLETE_" + courseId;
        Optional<Achievement> achievement = getAchievementByCode(achievementCode);

        if (achievement.isPresent()) {
            awardAchievement(userId, achievementCode);
        } else {
            log.debug("No specific achievement found for course {}", courseId);
        }

        // Check for "first course" achievement
        long completedCourses = getAchievementCount(userId);
        if (completedCourses == 1) {
            awardAchievementIfExists(userId, "FIRST_COURSE");
        }

        // Check for milestones
        checkCourseMilestones(userId, completedCourses);
    }

    /**
     * Track quiz performance
     */
    @Transactional
    public void trackQuizPerformance(UUID userId, double score) {
        log.debug("Tracking quiz performance for user {}: score={}", userId, score);

        // Perfect score achievement
        if (score == 100.0) {
            awardAchievementIfExists(userId, "PERFECT_QUIZ");
        }

        // High score achievement
        if (score >= 90.0) {
            awardAchievementIfExists(userId, "HIGH_ACHIEVER");
        }
    }

    /**
     * Track learning streak
     */
    @Transactional
    public void trackLearningStreak(UUID userId, int streakDays) {
        log.debug("Tracking learning streak for user {}: {} days", userId, streakDays);

        if (streakDays >= 7) {
            awardAchievementIfExists(userId, "WEEK_STREAK");
        }
        if (streakDays >= 30) {
            awardAchievementIfExists(userId, "MONTH_STREAK");
        }
        if (streakDays >= 100) {
            awardAchievementIfExists(userId, "CENTURY_STREAK");
        }
    }

    /**
     * Track lesson completion
     */
    @Transactional
    public void trackLessonCompletion(UUID userId, int totalLessons) {
        log.debug("Tracking lesson completion for user {}: {} lessons", userId, totalLessons);

        if (totalLessons >= 10) {
            awardAchievementIfExists(userId, "LESSON_10");
        }
        if (totalLessons >= 50) {
            awardAchievementIfExists(userId, "LESSON_50");
        }
        if (totalLessons >= 100) {
            awardAchievementIfExists(userId, "LESSON_100");
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * Create a new user achievement entry
     */
    private UserAchievement createUserAchievement(UUID userId, UUID achievementId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Achievement achievement = getAchievementById(achievementId);

        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUser(user);
        userAchievement.setAchievement(achievement);
        userAchievement.setProgress(0);
        userAchievement.setCreatedBy(userId.toString());

        return userAchievementRepository.save(userAchievement);
    }

    /**
     * Award achievement if it exists (doesn't throw if not found)
     */
    private void awardAchievementIfExists(UUID userId, String achievementCode) {
        try {
            Optional<Achievement> achievement = getAchievementByCode(achievementCode);
            if (achievement.isPresent()) {
                awardAchievement(userId, achievementCode);
            }
        } catch (Exception e) {
            log.debug("Achievement {} not found or could not be awarded: {}", achievementCode, e.getMessage());
        }
    }

    /**
     * Check and award course milestone achievements
     */
    private void checkCourseMilestones(UUID userId, long completedCourses) {
        if (completedCourses >= 5) {
            awardAchievementIfExists(userId, "COURSE_5");
        }
        if (completedCourses >= 10) {
            awardAchievementIfExists(userId, "COURSE_10");
        }
        if (completedCourses >= 25) {
            awardAchievementIfExists(userId, "COURSE_25");
        }
        if (completedCourses >= 50) {
            awardAchievementIfExists(userId, "COURSE_50");
        }
    }

    /**
     * Fire achievement earned event
     */
    private void fireAchievementEarnedEvent(UserAchievement userAchievement) {
        User user = userAchievement.getUser();
        Achievement achievement = userAchievement.getAchievement();

        AchievementEarnedEvent event = new AchievementEarnedEvent(
            userAchievement.getId(),
            user.getId(),
            user.getFullName(),
            achievement.getId(),
            achievement.getCode(),
            achievement.getName(),
            achievement.getPoints(),
            userAchievement.getEarnedAt().atZone(ZoneId.systemDefault()).toInstant(),
            user.getTenantId(),
            user.getId().toString()
        );

        eventPublisher.publishEvent(event);
        log.info("AchievementEarnedEvent published for achievement: {}", achievement.getCode());
    }

    // ==================== DTOs ====================

    /**
     * User achievement statistics
     */
    public record UserAchievementStats(
        long earnedCount,
        long totalAvailable,
        int totalPoints,
        long inProgressCount
    ) {
        public double getCompletionPercentage() {
            return totalAvailable > 0 ? (double) earnedCount / totalAvailable * 100 : 0;
        }
    }

    /**
     * Leaderboard entry
     */
    public record UserLeaderboardEntry(
        UUID userId,
        String userName,
        int totalPoints,
        long achievementCount
    ) {
    }
}
