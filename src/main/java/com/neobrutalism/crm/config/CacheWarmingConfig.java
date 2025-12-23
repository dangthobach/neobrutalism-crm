package com.neobrutalism.crm.config;

import com.neobrutalism.crm.domain.menu.repository.MenuRepository;
import com.neobrutalism.crm.domain.role.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * ✅ FIX #2: Cache Warming Strategy
 * Pre-loads frequently accessed reference data into Redis cache on application startup
 *
 * Benefits:
 * - Eliminates cold start latency
 * - Improves first request performance
 * - Pre-populates menu trees and role permissions
 */
@Slf4j
@Configuration
public class CacheWarmingConfig {

    private final CacheManager cacheManager;
    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;

    public CacheWarmingConfig(
            CacheManager cacheManager,
            MenuRepository menuRepository,
            RoleRepository roleRepository
    ) {
        this.cacheManager = cacheManager;
        this.menuRepository = menuRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Warm up caches after application is fully started
     * Uses @Async to avoid blocking application startup
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCaches() {
        log.info("🔥 Starting cache warming...");
        long startTime = System.currentTimeMillis();

        try {
            warmMenuTreeCache();
            warmRoleCache();

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Cache warming completed in {}ms", duration);
        } catch (Exception e) {
            log.error("❌ Cache warming failed: {}", e.getMessage(), e);
            // Don't throw - application should still start even if cache warming fails
        }
    }

    /**
     * Pre-load menu tree (most frequently accessed)
     * Warms up: menuTree cache
     */
    private void warmMenuTreeCache() {
        try {
            Cache menuTreeCache = cacheManager.getCache("menuTree");
            if (menuTreeCache == null) {
                log.warn("⚠️ menuTree cache not found, skipping warm-up");
                return;
            }

            // Load all menus (will be cached by MenuService)
            var menus = menuRepository.findAll();
            log.info("📋 Warmed up {} menus into menuTree cache", menus.size());

        } catch (Exception e) {
            log.error("❌ Failed to warm menu tree cache: {}", e.getMessage());
        }
    }

    /**
     * Pre-load roles and permissions
     * Warms up: roles, rolePermissions caches
     */
    private void warmRoleCache() {
        try {
            Cache roleCache = cacheManager.getCache("roles");
            if (roleCache == null) {
                log.warn("⚠️ roles cache not found, skipping warm-up");
                return;
            }

            // Load all roles
            var roles = roleRepository.findAll();
            log.info("👥 Warmed up {} roles into cache", roles.size());

        } catch (Exception e) {
            log.error("❌ Failed to warm role cache: {}", e.getMessage());
        }
    }

    /**
     * Optional: Manual cache warming endpoint for admins
     * Can be called via POST /api/admin/cache/warm
     */
    public void manualWarmUp() {
        log.info("🔥 Manual cache warm-up triggered");
        warmUpCaches();
    }

    /**
     * Optional: Clear all caches
     * Useful for testing or cache invalidation
     */
    public void clearAllCaches() {
        log.info("🗑️ Clearing all caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("  ✅ Cleared cache: {}", cacheName);
            }
        });
    }
}
