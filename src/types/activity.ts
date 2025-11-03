/**
 * Activity Types
 * For tracking customer interactions and activities
 */

export enum ActivityType {
  CALL = 'CALL',
  EMAIL = 'EMAIL',
  MEETING = 'MEETING',
  NOTE = 'NOTE',
  TASK = 'TASK',
  DEMO = 'DEMO',
  PROPOSAL = 'PROPOSAL',
  CONTRACT = 'CONTRACT',
  SUPPORT = 'SUPPORT',
  OTHER = 'OTHER',
}

export enum ActivityStatus {
  SCHEDULED = 'SCHEDULED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  OVERDUE = 'OVERDUE',
}

export enum ActivityOutcome {
  SUCCESSFUL = 'SUCCESSFUL',
  NO_ANSWER = 'NO_ANSWER',
  LEFT_MESSAGE = 'LEFT_MESSAGE',
  NEED_FOLLOWUP = 'NEED_FOLLOWUP',
  NOT_INTERESTED = 'NOT_INTERESTED',
  INTERESTED = 'INTERESTED',
  QUALIFIED = 'QUALIFIED',
  OTHER = 'OTHER',
}

export interface Activity {
  id: string
  type: ActivityType
  subject: string
  description?: string
  status: ActivityStatus
  outcome?: ActivityOutcome
  customerId?: string
  customerName?: string
  contactId?: string
  contactName?: string
  assignedToId?: string
  assignedToName?: string
  scheduledAt?: string
  startedAt?: string
  completedAt?: string
  duration?: number // in minutes
  location?: string
  meetingUrl?: string
  participants?: string[]
  attachments?: string[]
  tags?: string[]
  nextSteps?: string
  organizationId: string
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
  version: number
}

export interface CreateActivityRequest {
  type: ActivityType
  subject: string
  description?: string
  status: ActivityStatus
  outcome?: ActivityOutcome
  customerId?: string
  contactId?: string
  assignedToId?: string
  scheduledAt?: string
  startedAt?: string
  completedAt?: string
  duration?: number
  location?: string
  meetingUrl?: string
  participants?: string[]
  attachments?: string[]
  tags?: string[]
  nextSteps?: string
  organizationId: string
}

export interface UpdateActivityRequest {
  subject?: string
  description?: string
  status?: ActivityStatus
  outcome?: ActivityOutcome
  customerId?: string
  contactId?: string
  assignedToId?: string
  scheduledAt?: string
  startedAt?: string
  completedAt?: string
  duration?: number
  location?: string
  meetingUrl?: string
  participants?: string[]
  attachments?: string[]
  tags?: string[]
  nextSteps?: string
}

export interface ActivitySearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  keyword?: string
  type?: ActivityType
  status?: ActivityStatus
  outcome?: ActivityOutcome
  customerId?: string
  contactId?: string
  assignedToId?: string
  startDate?: string
  endDate?: string
  organizationId?: string
}

export interface ActivityStats {
  total: number
  byType: Record<ActivityType, number>
  byStatus: Record<ActivityStatus, number>
  byOutcome: Record<ActivityOutcome, number>
  totalDuration: number
  averageDuration: number
  todayCount: number
  thisWeekCount: number
  overdueCount: number
}
