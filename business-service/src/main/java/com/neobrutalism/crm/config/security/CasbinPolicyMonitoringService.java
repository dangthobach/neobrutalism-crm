package com.neobrutalism.crm.config.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Casbin Policy Monitoring Service
 *
 * Purpose: Monitor and alert on Casbin policy growth to prevent performance degradation
 *
 * Features:
 * 1. Real-time policy count tracking per tenant
 * 2. Automatic alerts when policy thresholds are exceeded
 * 3. Statistics collection for capacity planning
 * 4. Policy explosion prevention (100k CCU requirement)
 *
 * Performance Impact:
 * - Below 10,000 policies: ~0.01ms per permission check (100,000 ops/sec)
 * - 10,000-50,000 policies: ~0.1ms per permission check (10,000 ops/sec)
 * - Above 50,000 policies: ~1-5ms per permission check (200-1000 ops/sec)
 * - Above 100,000 policies: ~10-100ms per permission check (10-100 ops/sec) ⚠️ CRITICAL
 *
 * Alert Thresholds:
 * - WARNING: 10,000 policies (recommend role hierarchy)
 * - CRITICAL: 50,000 policies (performance degradation)
 * - EMERGENCY: 100,000 policies (system at risk)
 *
 * @author Neobrutalism CRM Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CasbinPolicyMonitoringService {

    private final Enforcer enforcer;

    @Value("${casbin.policy.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${casbin.policy.threshold.warning:10000}")
    private int warningThreshold;

    @Value("${casbin.policy.threshold.critical:50000}")
    private int criticalThreshold;

    @Value("${casbin.policy.threshold.emergency:100000}")
    private int emergencyThreshold;

    @Value("${casbin.policy.max-policies-per-role:1000}")
    private int maxPoliciesPerRole;

    @Value("${casbin.policy.max-policies-per-tenant:10000}")
    private int maxPoliciesPerTenant;

    // Statistics tracking
    private final Map<String, PolicyStats> tenantStats = new ConcurrentHashMap<>();
    private volatile int totalPolicyCount = 0;
    private volatile long lastCheckTimestamp = 0;
    private volatile String currentAlertLevel = "NORMAL";

    @PostConstruct
    public void init() {
        if (monitoringEnabled) {
            log.info("Casbin Policy Monitoring Service initialized");
            log.info("Thresholds - WARNING: {}, CRITICAL: {}, EMERGENCY: {}",
                    warningThreshold, criticalThreshold, emergencyThreshold);
            log.info("Limits - Max per role: {}, Max per tenant: {}",
                    maxPoliciesPerRole, maxPoliciesPerTenant);

            // Initial check
            checkPolicyHealth();
        } else {
            log.warn("Casbin Policy Monitoring is DISABLED - policy growth not monitored");
        }
    }

    /**
     * Scheduled health check - runs every 5 minutes
     */
    @Scheduled(fixedDelayString = "${casbin.policy.monitoring.check-interval-ms:300000}")
    public void scheduledHealthCheck() {
        if (monitoringEnabled) {
            checkPolicyHealth();
        }
    }

    /**
     * Check policy health and trigger alerts if needed
     */
    public void checkPolicyHealth() {
        try {
            List<List<String>> allPolicies = enforcer.getPolicy();
            totalPolicyCount = allPolicies.size();
            lastCheckTimestamp = System.currentTimeMillis();

            log.debug("Policy health check: {} total policies", totalPolicyCount);

            // Calculate per-tenant statistics
            updateTenantStats(allPolicies);

            // Check thresholds and trigger alerts
            String previousAlertLevel = currentAlertLevel;
            currentAlertLevel = determineAlertLevel(totalPolicyCount);

            if (!currentAlertLevel.equals(previousAlertLevel)) {
                logAlertLevelChange(previousAlertLevel, currentAlertLevel);
            }

            // Log detailed stats if above warning threshold
            if (totalPolicyCount >= warningThreshold) {
                logDetailedStats();
            }

        } catch (Exception e) {
            log.error("Error during policy health check: {}", e.getMessage(), e);
        }
    }

    /**
     * Update per-tenant statistics
     */
    private void updateTenantStats(List<List<String>> allPolicies) {
        Map<String, PolicyStats> newStats = new HashMap<>();

        for (List<String> policy : allPolicies) {
            if (policy.size() >= 4) {
                // Policy format: [subject, domain/tenant, resource, action]
                String tenant = policy.get(1);

                PolicyStats stats = newStats.computeIfAbsent(tenant, k -> new PolicyStats());
                stats.incrementPolicyCount();

                // Track policies per role
                String subject = policy.get(0);
                stats.incrementRolePolicyCount(subject);
            }
        }

        tenantStats.clear();
        tenantStats.putAll(newStats);
    }

    /**
     * Determine alert level based on policy count
     */
    private String determineAlertLevel(int policyCount) {
        if (policyCount >= emergencyThreshold) {
            return "EMERGENCY";
        } else if (policyCount >= criticalThreshold) {
            return "CRITICAL";
        } else if (policyCount >= warningThreshold) {
            return "WARNING";
        } else {
            return "NORMAL";
        }
    }

    /**
     * Log alert level changes
     */
    private void logAlertLevelChange(String previous, String current) {
        switch (current) {
            case "EMERGENCY":
                log.error("🚨 EMERGENCY: Policy count ({}) exceeded emergency threshold ({}). " +
                                "System performance critically degraded. IMMEDIATE ACTION REQUIRED: " +
                                "1) Implement role hierarchy, 2) Archive old policies, 3) Split tenants",
                        totalPolicyCount, emergencyThreshold);
                break;
            case "CRITICAL":
                log.error("⚠️ CRITICAL: Policy count ({}) exceeded critical threshold ({}). " +
                                "Performance degradation expected. ACTION REQUIRED: " +
                                "1) Review and consolidate policies, 2) Plan role hierarchy implementation",
                        totalPolicyCount, criticalThreshold);
                break;
            case "WARNING":
                log.warn("⚠️ WARNING: Policy count ({}) exceeded warning threshold ({}). " +
                                "Consider implementing role hierarchy to prevent performance issues. " +
                                "Current performance: ~0.1ms per check (acceptable but monitor closely)",
                        totalPolicyCount, warningThreshold);
                break;
            case "NORMAL":
                log.info("✅ Policy count ({}) back to NORMAL range. Performance optimal (~0.01ms per check)",
                        totalPolicyCount);
                break;
        }
    }

    /**
     * Log detailed statistics
     */
    private void logDetailedStats() {
        log.info("=== Casbin Policy Statistics ===");
        log.info("Total Policies: {}", totalPolicyCount);
        log.info("Alert Level: {}", currentAlertLevel);
        log.info("Number of Tenants: {}", tenantStats.size());

        // Find top tenants by policy count
        List<Map.Entry<String, PolicyStats>> topTenants = tenantStats.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getPolicyCount(), e1.getValue().getPolicyCount()))
                .limit(5)
                .collect(Collectors.toList());

        log.info("Top 5 Tenants by Policy Count:");
        for (Map.Entry<String, PolicyStats> entry : topTenants) {
            String tenant = entry.getKey();
            PolicyStats stats = entry.getValue();
            log.info("  - Tenant: {}, Policies: {}, Roles with most policies: {}",
                    tenant, stats.getPolicyCount(), stats.getTopRoles(3));

            // Check if tenant exceeds limit
            if (stats.getPolicyCount() > maxPoliciesPerTenant) {
                log.warn("    ⚠️ Tenant {} exceeds max policies per tenant ({} > {})",
                        tenant, stats.getPolicyCount(), maxPoliciesPerTenant);
            }

            // Check if any role exceeds limit
            stats.getRolePolicyCounts().forEach((role, count) -> {
                if (count > maxPoliciesPerRole) {
                    log.warn("    ⚠️ Role {} in tenant {} exceeds max policies per role ({} > {})",
                            role, tenant, count, maxPoliciesPerRole);
                }
            });
        }

        log.info("=================================");
    }

    /**
     * Get current monitoring statistics
     */
    public Map<String, Object> getMonitoringStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("monitoring_enabled", monitoringEnabled);
        stats.put("total_policy_count", totalPolicyCount);
        stats.put("alert_level", currentAlertLevel);
        stats.put("last_check_timestamp", lastCheckTimestamp);
        stats.put("tenant_count", tenantStats.size());
        stats.put("thresholds", Map.of(
                "warning", warningThreshold,
                "critical", criticalThreshold,
                "emergency", emergencyThreshold
        ));
        stats.put("limits", Map.of(
                "max_policies_per_role", maxPoliciesPerRole,
                "max_policies_per_tenant", maxPoliciesPerTenant
        ));

        // Tenant stats summary
        Map<String, Object> tenantSummary = new HashMap<>();
        tenantStats.forEach((tenant, policyStats) -> {
            tenantSummary.put(tenant, Map.of(
                    "policy_count", policyStats.getPolicyCount(),
                    "role_count", policyStats.getRolePolicyCounts().size(),
                    "top_roles", policyStats.getTopRoles(3)
            ));
        });
        stats.put("tenant_stats", tenantSummary);

        return stats;
    }

    /**
     * Validate if adding new policies is safe
     */
    public boolean canAddPolicies(String tenant, String role, int count) {
        PolicyStats stats = tenantStats.getOrDefault(tenant, new PolicyStats());

        // Check tenant limit
        if (stats.getPolicyCount() + count > maxPoliciesPerTenant) {
            log.warn("Cannot add {} policies to tenant {}: would exceed limit ({} > {})",
                    count, tenant, stats.getPolicyCount() + count, maxPoliciesPerTenant);
            return false;
        }

        // Check role limit
        int currentRolePolicies = stats.getRolePolicyCounts().getOrDefault(role, 0);
        if (currentRolePolicies + count > maxPoliciesPerRole) {
            log.warn("Cannot add {} policies to role {} in tenant {}: would exceed limit ({} > {})",
                    count, role, tenant, currentRolePolicies + count, maxPoliciesPerRole);
            return false;
        }

        return true;
    }

    /**
     * Inner class to track policy statistics per tenant
     */
    private static class PolicyStats {
        private int policyCount = 0;
        private final Map<String, Integer> rolePolicyCounts = new HashMap<>();

        public void incrementPolicyCount() {
            policyCount++;
        }

        public void incrementRolePolicyCount(String role) {
            rolePolicyCounts.merge(role, 1, Integer::sum);
        }

        public int getPolicyCount() {
            return policyCount;
        }

        public Map<String, Integer> getRolePolicyCounts() {
            return rolePolicyCounts;
        }

        public List<String> getTopRoles(int limit) {
            return rolePolicyCounts.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .limit(limit)
                    .map(e -> e.getKey() + "(" + e.getValue() + ")")
                    .collect(Collectors.toList());
        }
    }
}
