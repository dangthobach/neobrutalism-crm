'use client'

import { useState, useEffect, useMemo } from 'react'
import { useRouter } from 'next/navigation'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Switch } from '@/components/ui/switch'
import { ArrowLeft, Save, Bell, Mail, MessageSquare, CheckCircle, Loader2, AlertCircle } from 'lucide-react'
import {
  useNotificationPreferences,
  useBatchUpdatePreferences,
  useEnableAllPreferences,
  useDisableAllPreferences,
  useResetPreferences,
} from '@/hooks/useNotificationPreferences'
import type { NotificationPreference as APIPreference } from '@/lib/api/notification-preferences'

interface NotificationPreference {
  type: string
  label: string
  description: string
  channels: {
    inApp: boolean
    email: boolean
    sms: boolean
  }
}

// Notification type labels and descriptions
const NOTIFICATION_TYPE_INFO: Record<string, { label: string; description: string }> = {
  SYSTEM: {
    label: 'System Notifications',
    description: 'Important system updates and announcements',
  },
  TASK_ASSIGNED: {
    label: 'Task Assigned',
    description: 'When you are assigned to a new task',
  },
  TASK_UPDATED: {
    label: 'Task Updated',
    description: 'When a task you\'re involved in is updated',
  },
  TASK_COMPLETED: {
    label: 'Task Completed',
    description: 'When a task you created is completed',
  },
  TASK_COMMENT: {
    label: 'Task Comments',
    description: 'When someone comments on your task',
  },
  CUSTOMER_CREATED: {
    label: 'Customer Created',
    description: 'When a new customer is added to the system',
  },
  CUSTOMER_UPDATED: {
    label: 'Customer Updated',
    description: 'When customer information is modified',
  },
  ACTIVITY_REMINDER: {
    label: 'Activity Reminders',
    description: 'Upcoming activity and event reminders',
  },
  ACTIVITY_OVERDUE: {
    label: 'Overdue Activities',
    description: 'When activities pass their due date',
  },
  MENTION: {
    label: 'Mentions',
    description: 'When someone mentions you in a comment',
  },
  APPROVAL_REQUEST: {
    label: 'Approval Requests',
    description: 'When you need to approve something',
  },
  APPROVAL_APPROVED: {
    label: 'Approvals Granted',
    description: 'When your request is approved',
  },
  APPROVAL_REJECTED: {
    label: 'Approvals Rejected',
    description: 'When your request is rejected',
  },
}

export default function NotificationPreferencesPage() {
  const router = useRouter()
  
  // Fetch preferences from API
  const { data: apiPreferences, isLoading, error, refetch } = useNotificationPreferences()
  
  // Mutations
  const batchUpdateMutation = useBatchUpdatePreferences()
  const enableAllMutation = useEnableAllPreferences()
  const disableAllMutation = useDisableAllPreferences()
  const resetMutation = useResetPreferences()
  
  // Local state for form
  const [preferences, setPreferences] = useState<NotificationPreference[]>([])

  // Transform API preferences to UI format
  useEffect(() => {
    if (apiPreferences) {
      const transformed = apiPreferences.map((pref: APIPreference) => {
        const info = NOTIFICATION_TYPE_INFO[pref.notificationType] || {
          label: pref.notificationType,
          description: 'Notification setting',
        }
        
        return {
          type: pref.notificationType,
          label: info.label,
          description: info.description,
          channels: {
            inApp: pref.inAppEnabled,
            email: pref.emailEnabled,
            sms: pref.smsEnabled,
          },
        }
      })
      
      setPreferences(transformed)
    }
  }, [apiPreferences])

  const handleToggle = (type: string, channel: 'inApp' | 'email' | 'sms') => {
    setPreferences(prev =>
      prev.map(pref =>
        pref.type === type
          ? {
              ...pref,
              channels: {
                ...pref.channels,
                [channel]: !pref.channels[channel]
              }
            }
          : pref
      )
    )
  }

  const handleSave = async () => {
    try {
      // Transform preferences to API format
      const updates = preferences.map(pref => ({
        notificationType: pref.type,
        inAppEnabled: pref.channels.inApp,
        emailEnabled: pref.channels.email,
        smsEnabled: pref.channels.sms,
      }))
      
      await batchUpdateMutation.mutateAsync({ preferences: updates })
      await refetch()
    } catch (error) {
      // Error already handled by mutation
    }
  }

  const handleEnableAll = async () => {
    try {
      await enableAllMutation.mutateAsync()
      await refetch()
    } catch (error) {
      // Error already handled by mutation
    }
  }

  const handleDisableAll = async () => {
    try {
      await disableAllMutation.mutateAsync()
      await refetch()
    } catch (error) {
      // Error already handled by mutation
    }
  }
  
  const handleReset = async () => {
    try {
      await resetMutation.mutateAsync()
      await refetch()
    } catch (error) {
      // Error already handled by mutation
    }
  }
  
  const isSaving = batchUpdateMutation.isPending || 
                   enableAllMutation.isPending || 
                   disableAllMutation.isPending ||
                   resetMutation.isPending

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto py-6 px-4 max-w-5xl">
        {/* Header */}
        <div className="mb-6">
          <Button
            variant="ghost"
            onClick={() => router.back()}
            className="mb-4 hover:bg-gray-100"
          >
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Notifications
          </Button>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-purple-500 border-4 border-black rounded-lg shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <Bell className="w-8 h-8 text-white" />
              </div>
              <div>
                <h1 className="text-4xl font-black">Notification Preferences</h1>
                <p className="text-gray-600 font-medium">
                  Manage how you receive notifications
                </p>
              </div>
            </div>

            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={handleDisableAll}
                className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all"
              >
                Disable All
              </Button>
              <Button
                variant="outline"
                onClick={handleEnableAll}
                className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all"
              >
                Enable All
              </Button>
              <Button
                onClick={handleSave}
                disabled={isSaving}
                className="bg-green-500 hover:bg-green-600 text-white border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all"
              >
                <Save className="w-4 h-4 mr-2" />
                {isSaving ? 'Saving...' : 'Save Preferences'}
              </Button>
            </div>
          </div>
        </div>

        {/* Info Card */}
        <Card className="p-4 mb-6 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-blue-50">
          <div className="flex gap-3">
            <CheckCircle className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
            <div>
              <h3 className="font-bold text-sm mb-1">Choose Your Notification Channels</h3>
              <p className="text-sm text-gray-700">
                For each notification type, you can choose to receive alerts via in-app notifications,
                email, or SMS. You can customize each channel independently.
              </p>
            </div>
          </div>
        </Card>

        {/* Preferences Table */}
        <Card className="border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] bg-white overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-gray-100 border-b-4 border-black">
                  <th className="text-left p-4 font-black">Notification Type</th>
                  <th className="text-center p-4 font-black w-32">
                    <div className="flex flex-col items-center gap-1">
                      <Bell className="w-5 h-5" />
                      <span className="text-xs">In-App</span>
                    </div>
                  </th>
                  <th className="text-center p-4 font-black w-32">
                    <div className="flex flex-col items-center gap-1">
                      <Mail className="w-5 h-5" />
                      <span className="text-xs">Email</span>
                    </div>
                  </th>
                  <th className="text-center p-4 font-black w-32">
                    <div className="flex flex-col items-center gap-1">
                      <MessageSquare className="w-5 h-5" />
                      <span className="text-xs">SMS</span>
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                {preferences.map((pref, index) => (
                  <tr
                    key={pref.type}
                    className={`
                      border-b-2 border-gray-200 hover:bg-gray-50 transition-colors
                      ${index === preferences.length - 1 ? 'border-b-0' : ''}
                    `}
                  >
                    <td className="p-4">
                      <div>
                        <div className="font-bold text-sm">{pref.label}</div>
                        <div className="text-xs text-gray-600 mt-0.5">{pref.description}</div>
                      </div>
                    </td>
                    <td className="p-4 text-center">
                      <div className="flex justify-center">
                        <Switch
                          checked={pref.channels.inApp}
                          onCheckedChange={() => handleToggle(pref.type, 'inApp')}
                          className="data-[state=checked]:bg-blue-500"
                        />
                      </div>
                    </td>
                    <td className="p-4 text-center">
                      <div className="flex justify-center">
                        <Switch
                          checked={pref.channels.email}
                          onCheckedChange={() => handleToggle(pref.type, 'email')}
                          className="data-[state=checked]:bg-green-500"
                        />
                      </div>
                    </td>
                    <td className="p-4 text-center">
                      <div className="flex justify-center">
                        <Switch
                          checked={pref.channels.sms}
                          onCheckedChange={() => handleToggle(pref.type, 'sms')}
                          className="data-[state=checked]:bg-purple-500"
                        />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>

        {/* Additional Settings */}
        <Card className="p-6 mt-6 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white">
          <h3 className="font-black text-lg mb-4">Additional Settings</h3>

          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-gray-50 border-2 border-black">
              <div>
                <div className="font-bold text-sm">Quiet Hours</div>
                <div className="text-xs text-gray-600 mt-0.5">
                  Disable notifications during specific hours (Coming soon)
                </div>
              </div>
              <Switch disabled />
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 border-2 border-black">
              <div>
                <div className="font-bold text-sm">Digest Mode</div>
                <div className="text-xs text-gray-600 mt-0.5">
                  Receive a daily summary instead of individual notifications (Coming soon)
                </div>
              </div>
              <Switch disabled />
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 border-2 border-black">
              <div>
                <div className="font-bold text-sm">Sound Notifications</div>
                <div className="text-xs text-gray-600 mt-0.5">
                  Play a sound when new notifications arrive (Coming soon)
                </div>
              </div>
              <Switch disabled />
            </div>
          </div>
        </Card>
      </div>
    </div>
  )
}
