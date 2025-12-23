import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as permissionAuditApi from '@/lib/api/permission-audit';
import type { PermissionAuditLog } from '@/lib/api/permission-audit';

/**
 * Hook for fetching all audit logs
 */
export function useAuditLogs(page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['audit-logs', page, size],
    queryFn: () => permissionAuditApi.getAllAuditLogs(page, size),
  });
}

/**
 * Hook for fetching audit logs for a specific user
 */
export function useUserAuditLogs(userId: string, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['audit-logs', 'user', userId, page, size],
    queryFn: () => permissionAuditApi.getAuditLogsForUser(userId, page, size),
    enabled: !!userId,
  });
}

/**
 * Hook for fetching audit logs by action type
 */
export function useAuditLogsByActionType(actionType: string, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['audit-logs', 'action-type', actionType, page, size],
    queryFn: () => permissionAuditApi.getAuditLogsByActionType(actionType, page, size),
    enabled: !!actionType,
  });
}

/**
 * Hook for fetching audit logs within date range
 */
export function useAuditLogsByDateRange(
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 20
) {
  return useQuery({
    queryKey: ['audit-logs', 'date-range', startDate, endDate, page, size],
    queryFn: () => permissionAuditApi.getAuditLogsByDateRange(startDate, endDate, page, size),
    enabled: !!startDate && !!endDate,
  });
}

/**
 * Hook for fetching critical security events
 */
export function useCriticalEvents(page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['audit-logs', 'critical-events', page, size],
    queryFn: () => permissionAuditApi.getCriticalEvents(page, size),
  });
}

/**
 * Hook for fetching failed permission attempts
 */
export function useFailedAttempts(page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['audit-logs', 'failed-attempts', page, size],
    queryFn: () => permissionAuditApi.getFailedAttempts(page, size),
  });
}

/**
 * Hook for searching audit logs
 */
export function useSearchAuditLogs(searchTerm: string, page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['audit-logs', 'search', searchTerm, page, size],
    queryFn: () => permissionAuditApi.searchAuditLogs(searchTerm, page, size),
    enabled: searchTerm.length >= 2,
  });
}

/**
 * Hook for fetching audit statistics
 */
export function useAuditStatistics(startDate?: string, endDate?: string) {
  return useQuery({
    queryKey: ['audit-statistics', startDate, endDate],
    queryFn: () => permissionAuditApi.getAuditStatistics(startDate, endDate),
  });
}

/**
 * Hook for fetching current user's activity
 */
export function useMyActivity(page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: ['audit-logs', 'my-activity', page, size],
    queryFn: () => permissionAuditApi.getMyActivity(page, size),
  });
}

/**
 * Hook for checking suspicious activity
 */
export function useSuspiciousActivity(userId: string) {
  return useQuery({
    queryKey: ['suspicious-activity', userId],
    queryFn: () => permissionAuditApi.checkSuspiciousActivity(userId),
    enabled: !!userId,
  });
}

/**
 * Hook for fetching audit logs by session
 */
export function useAuditLogsBySession(sessionId: string) {
  return useQuery({
    queryKey: ['audit-logs', 'session', sessionId],
    queryFn: () => permissionAuditApi.getAuditLogsBySession(sessionId),
    enabled: !!sessionId,
  });
}

/**
 * Hook for cleaning up old audit logs
 */
export function useCleanupAuditLogs() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (retentionDays: number = 365) =>
      permissionAuditApi.cleanupOldAuditLogs(retentionDays),
    onSuccess: () => {
      // Invalidate all audit log queries
      queryClient.invalidateQueries({ queryKey: ['audit-logs'] });
    },
  });
}
