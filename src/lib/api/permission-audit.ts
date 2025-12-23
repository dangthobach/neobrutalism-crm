import { apiClient } from './client';
import type { ApiResponse, PaginatedResponse } from './client';

/**
 * Permission Audit Log API
 */

export interface PermissionAuditLog {
  id: string;
  actionType: string;
  changedByUserId: string;
  changedByUsername: string;
  targetUserId?: string;
  targetUsername?: string;
  targetRoleCode?: string;
  resource?: string;
  action?: string;
  dataScope?: string;
  branchId?: string;
  oldValue?: string;
  newValue?: string;
  reason?: string;
  ipAddress?: string;
  userAgent?: string;
  changedAt: string;
  success: boolean;
  errorMessage?: string;
  tenantId: string;
  organizationId?: string;
  sessionId?: string;
  metadata?: string;
}

export interface AuditStatistic {
  actionType: string;
  count: number;
}

/**
 * Get all audit logs (paginated)
 */
export const getAllAuditLogs = async (
  page: number = 0,
  size: number = 20
): Promise<PaginatedResponse<PermissionAuditLog>> => {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<PermissionAuditLog>>>(
    `/permission-audit?page=${page}&size=${size}`
  );
  return response.data.data;
};

/**
 * Get audit logs for a specific user
 */
export const getAuditLogsForUser = async (
  userId: string,
  page: number = 0,
  size: number = 20
): Promise<PaginatedResponse<PermissionAuditLog>> => {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<PermissionAuditLog>>>(
    `/permission-audit/user/${userId}?page=${page}&size=${size}`
  );
  return response.data.data;
};

/**
 * Get audit logs by action type
 */
export const getAuditLogsByActionType = async (
  actionType: string,
  page: number = 0,
  size: number = 20
): Promise<PaginatedResponse<PermissionAuditLog>> => {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<PermissionAuditLog>>>(
    `/permission-audit/action-type/${actionType}?page=${page}&size=${size}`
  );
  return response.data.data;
};

/**
 * Get audit logs within date range
 */
export const getAuditLogsByDateRange = async (
  startDate: string,
  endDate: string,
  page: number = 0,
  size: number = 20
): Promise<PaginatedResponse<PermissionAuditLog>> => {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<PermissionAuditLog>>>(
    `/permission-audit/date-range?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
  );
  return response.data.data;
};

/**
 * Get critical security events
 */
export const getCriticalEvents = async (
  page: number = 0,
  size: number = 20
): Promise<PaginatedResponse<PermissionAuditLog>> => {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<PermissionAuditLog>>>(
    `/permission-audit/critical-events?page=${page}&size=${size}`
  );
  return response.data.data;
};

/**
 * Get failed permission attempts
 */
export const getFailedAttempts = async (
  page: number = 0,
  size: number = 20
): Promise<PaginatedResponse<PermissionAuditLog>> => {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<PermissionAuditLog>>>(
    `/permission-audit/failed-attempts?page=${page}&size=${size}`
  );
  return response.data.data;
};

/**
 * Search audit logs
 */
export const searchAuditLogs = async (
  searchTerm: string,
  page: number = 0,
  size: number = 20
): Promise<PaginatedResponse<PermissionAuditLog>> => {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<PermissionAuditLog>>>(
    `/permission-audit/search?searchTerm=${encodeURIComponent(searchTerm)}&page=${page}&size=${size}`
  );
  return response.data.data;
};

/**
 * Get audit statistics
 */
export const getAuditStatistics = async (
  startDate?: string,
  endDate?: string
): Promise<AuditStatistic[]> => {
  let url = '/permission-audit/statistics';
  if (startDate && endDate) {
    url += `?startDate=${startDate}&endDate=${endDate}`;
  }
  const response = await apiClient.get<ApiResponse<AuditStatistic[]>>(url);
  return response.data.data;
};

/**
 * Get current user's recent activity
 */
export const getMyActivity = async (
  page: number = 0,
  size: number = 20
): Promise<PaginatedResponse<PermissionAuditLog>> => {
  const response = await apiClient.get<ApiResponse<PaginatedResponse<PermissionAuditLog>>>(
    `/permission-audit/my-activity?page=${page}&size=${size}`
  );
  return response.data.data;
};

/**
 * Check for suspicious activity for a user
 */
export const checkSuspiciousActivity = async (userId: string): Promise<boolean> => {
  const response = await apiClient.get<ApiResponse<boolean>>(
    `/permission-audit/suspicious-activity/${userId}`
  );
  return response.data.data;
};

/**
 * Get audit logs by session ID
 */
export const getAuditLogsBySession = async (
  sessionId: string
): Promise<PermissionAuditLog[]> => {
  const response = await apiClient.get<ApiResponse<PermissionAuditLog[]>>(
    `/permission-audit/session/${sessionId}`
  );
  return response.data.data;
};

/**
 * Clean up old audit logs
 */
export const cleanupOldAuditLogs = async (
  retentionDays: number = 365
): Promise<number> => {
  const response = await apiClient.delete<ApiResponse<number>>(
    `/permission-audit/cleanup?retentionDays=${retentionDays}`
  );
  return response.data.data;
};
