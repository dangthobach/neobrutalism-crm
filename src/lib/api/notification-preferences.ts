/**
 * Notification Preferences API
 * API client for managing user notification preferences
 */

import { apiClient } from './client'

export interface NotificationPreference {
  id?: string
  userId?: string
  organizationId?: string
  notificationType: string
  inAppEnabled: boolean
  emailEnabled: boolean
  smsEnabled: boolean
  quietHoursStart?: string
  quietHoursEnd?: string
  digestModeEnabled: boolean
  digestTime?: string
  createdDate?: string
  lastModifiedDate?: string
}

export interface UpdatePreferenceRequest {
  notificationType: string
  inAppEnabled: boolean
  emailEnabled: boolean
  smsEnabled: boolean
  quietHoursStart?: string
  quietHoursEnd?: string
  digestModeEnabled?: boolean
  digestTime?: string
}

export interface BatchUpdatePreferencesRequest {
  preferences: UpdatePreferenceRequest[]
}

export class NotificationPreferenceApi {
  /**
   * Get all notification preferences for current user
   */
  async getMyPreferences(): Promise<NotificationPreference[]> {
    return apiClient.get<NotificationPreference[]>('/notifications/preferences/my')
  }

  /**
   * Get preference for specific notification type
   */
  async getPreferenceByType(type: string): Promise<NotificationPreference> {
    return apiClient.get<NotificationPreference>(`/notifications/preferences/my/${type}`)
  }

  /**
   * Update preference for specific notification type
   */
  async updatePreference(type: string, data: UpdatePreferenceRequest): Promise<NotificationPreference> {
    return apiClient.put<NotificationPreference>(`/notifications/preferences/my/${type}`, data)
  }

  /**
   * Batch update multiple preferences
   */
  async batchUpdatePreferences(data: BatchUpdatePreferencesRequest): Promise<NotificationPreference[]> {
    return apiClient.post<NotificationPreference[]>('/notifications/preferences/my/batch', data)
  }

  /**
   * Reset preferences to defaults
   */
  async resetToDefaults(): Promise<NotificationPreference[]> {
    return apiClient.post<NotificationPreference[]>('/notifications/preferences/my/reset', {})
  }

  /**
   * Enable all notification channels
   */
  async enableAll(): Promise<NotificationPreference[]> {
    const preferences = await this.getMyPreferences()
    const updates = preferences.map(p => ({
      notificationType: p.notificationType,
      inAppEnabled: true,
      emailEnabled: true,
      smsEnabled: true,
    }))
    
    return this.batchUpdatePreferences({ preferences: updates })
  }

  /**
   * Disable all notification channels
   */
  async disableAll(): Promise<NotificationPreference[]> {
    const preferences = await this.getMyPreferences()
    const updates = preferences.map(p => ({
      notificationType: p.notificationType,
      inAppEnabled: false,
      emailEnabled: false,
      smsEnabled: false,
    }))
    
    return this.batchUpdatePreferences({ preferences: updates })
  }
}

export const notificationPreferenceApi = new NotificationPreferenceApi()
