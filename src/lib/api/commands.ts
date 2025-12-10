/**
 * Commands API Service
 * Handles all command palette-related API calls
 */

import { apiClient } from './client'

export enum CommandCategory {
  CUSTOMER = 'CUSTOMER',
  CONTACT = 'CONTACT',
  TASK = 'TASK',
  ACTIVITY = 'ACTIVITY',
  USER = 'USER',
  ORGANIZATION = 'ORGANIZATION',
  REPORT = 'REPORT',
  SETTINGS = 'SETTINGS',
  NAVIGATION = 'NAVIGATION',
  SEARCH = 'SEARCH',
}

export interface Command {
  id: string
  commandId: string
  label: string
  description?: string
  category: CommandCategory
  icon?: string
  shortcutKey?: string
  actionType: string
  actionPayload?: string
  requiredPermission?: string
  executionCount?: number
  avgExecutionTimeMs?: number
}

export interface CommandSearchRequest {
  query?: string
  category?: CommandCategory
  page?: number
  size?: number
}

export interface CommandSearchResponse {
  commands: Command[]
  totalCount: number
  hasMore: boolean
}

export interface CommandExecutionRequest {
  commandId: string
  executionTimeMs: number
  contextData?: string
}

export class CommandApi {
  /**
   * Search commands
   */
  async searchCommands(params?: CommandSearchRequest): Promise<CommandSearchResponse> {
    return apiClient.get<CommandSearchResponse>('/commands/search', params)
  }

  /**
   * Get recent commands for current user
   */
  async getRecentCommands(): Promise<Command[]> {
    return apiClient.get<Command[]>('/commands/recent')
  }

  /**
   * Get favorite commands for current user
   */
  async getFavoriteCommands(): Promise<Command[]> {
    return apiClient.get<Command[]>('/commands/favorites')
  }

  /**
   * Add command to favorites
   */
  async addToFavorites(commandId: string): Promise<void> {
    return apiClient.post<void>(`/commands/favorites/${commandId}`)
  }

  /**
   * Remove command from favorites
   */
  async removeFromFavorites(commandId: string): Promise<void> {
    return apiClient.delete<void>(`/commands/favorites/${commandId}`)
  }

  /**
   * Record command execution
   */
  async recordExecution(request: CommandExecutionRequest): Promise<void> {
    return apiClient.post<void>('/commands/execute', request)
  }

  /**
   * Get suggested commands for current user
   */
  async getSuggestedCommands(limit?: number): Promise<Command[]> {
    return apiClient.get<Command[]>('/commands/suggestions', { limit })
  }
}

export const commandApi = new CommandApi()
