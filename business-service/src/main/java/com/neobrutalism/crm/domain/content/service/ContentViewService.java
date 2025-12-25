package com.neobrutalism.crm.domain.content.service;

import com.neobrutalism.crm.domain.content.dto.TrackViewRequest;
import com.neobrutalism.crm.domain.content.event.ContentViewedEvent;
import com.neobrutalism.crm.domain.content.model.Content;
import com.neobrutalism.crm.domain.content.model.ContentView;
import com.neobrutalism.crm.domain.content.repository.ContentRepository;
import com.neobrutalism.crm.domain.content.repository.ContentViewRepository;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for tracking content views and engagement
 */
@Service
@Slf4j
public class ContentViewService {

    private final ContentViewRepository contentViewRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final ContentService contentService;
    private final ApplicationEventPublisher eventPublisher;

    public ContentViewService(ContentViewRepository contentViewRepository,
                             ContentRepository contentRepository,
                             UserRepository userRepository,
                             ContentService contentService,
                             ApplicationEventPublisher eventPublisher) {
        this.contentViewRepository = contentViewRepository;
        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.contentService = contentService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Track content view
     */
    @Transactional
    public ContentView trackView(UUID contentId, UUID userId, TrackViewRequest request,
                                 HttpServletRequest httpRequest) {
        log.info("Tracking view for content: {} by user: {}", contentId, userId);

        Content content = contentRepository.findById(contentId)
            .orElseThrow(() -> new IllegalArgumentException("Content not found"));

        // Only track views for published content
        if (!content.isPublished()) {
            log.warn("Attempted to track view for non-published content: {}", contentId);
            throw new IllegalStateException("Cannot track view for non-published content");
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // Create view record
        ContentView view = new ContentView();
        view.setContent(content);
        view.setUser(user);
        view.setSessionId(request.getSessionId());
        view.setTimeSpentSeconds(request.getTimeSpentSeconds() != null ? request.getTimeSpentSeconds() : 0);
        view.setScrollPercentage(request.getScrollPercentage() != null ? request.getScrollPercentage() : 0);
        view.setViewedAt(Instant.now());

        // Extract HTTP request info
        if (httpRequest != null) {
            view.setIpAddress(getClientIp(httpRequest));
            view.setUserAgent(httpRequest.getHeader("User-Agent"));
            view.setReferrer(request.getReferrer() != null ? request.getReferrer() : httpRequest.getHeader("Referer"));
        }

        ContentView saved = contentViewRepository.save(view);

        // Increment view count in content
        contentService.incrementViewCount(contentId);

        // Publish event for engagement scoring
        eventPublisher.publishEvent(new ContentViewedEvent(
            contentId,
            content.getTitle(),
            userId,
            request.getSessionId(),
            request.getTimeSpentSeconds(),
            request.getScrollPercentage(),
            content.getTenantId()
        ));

        log.info("Content view tracked: {} - engagement score: {}", saved.getId(), view.calculateEngagementScore());
        return saved;
    }

    /**
     * Get content view history
     */
    @Transactional(readOnly = true)
    public Page<ContentView> getContentViewHistory(UUID contentId, Pageable pageable) {
        return contentViewRepository.findByContentId(contentId, pageable);
    }

    /**
     * Get user view history
     */
    @Transactional(readOnly = true)
    public Page<ContentView> getUserViewHistory(UUID userId, Pageable pageable) {
        return contentViewRepository.findUserViewHistory(userId, pageable);
    }

    /**
     * Get view statistics for content
     */
    @Transactional(readOnly = true)
    public ContentViewStats getContentViewStats(UUID contentId) {
        long totalViews = contentViewRepository.countByContentId(contentId);
        long uniqueUsers = contentViewRepository.countUniqueUsersByContentId(contentId);
        Double avgTimeSpent = contentViewRepository.getAverageTimeSpent(contentId);
        Double avgScrollPercentage = contentViewRepository.getAverageScrollPercentage(contentId);

        ContentViewStats stats = new ContentViewStats();
        stats.setTotalViews(totalViews);
        stats.setUniqueUsers(uniqueUsers);
        stats.setAverageTimeSpentSeconds(avgTimeSpent != null ? avgTimeSpent.intValue() : 0);
        stats.setAverageScrollPercentage(avgScrollPercentage != null ? avgScrollPercentage.intValue() : 0);

        return stats;
    }

    /**
     * Check if user has viewed content
     */
    @Transactional(readOnly = true)
    public boolean hasUserViewedContent(UUID contentId, UUID userId) {
        return contentViewRepository.existsByContentIdAndUserId(contentId, userId);
    }

    /**
     * Get most viewed content
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getMostViewedContent(Instant since, Pageable pageable) {
        return contentViewRepository.findMostViewedContent(since, pageable);
    }

    /**
     * Extract client IP from HTTP request
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Get first IP if multiple
                if (ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                return ip.trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * DTO for view statistics
     */
    public static class ContentViewStats {
        private long totalViews;
        private long uniqueUsers;
        private int averageTimeSpentSeconds;
        private int averageScrollPercentage;

        public long getTotalViews() { return totalViews; }
        public void setTotalViews(long totalViews) { this.totalViews = totalViews; }

        public long getUniqueUsers() { return uniqueUsers; }
        public void setUniqueUsers(long uniqueUsers) { this.uniqueUsers = uniqueUsers; }

        public int getAverageTimeSpentSeconds() { return averageTimeSpentSeconds; }
        public void setAverageTimeSpentSeconds(int averageTimeSpentSeconds) {
            this.averageTimeSpentSeconds = averageTimeSpentSeconds;
        }

        public int getAverageScrollPercentage() { return averageScrollPercentage; }
        public void setAverageScrollPercentage(int averageScrollPercentage) {
            this.averageScrollPercentage = averageScrollPercentage;
        }
    }
}
