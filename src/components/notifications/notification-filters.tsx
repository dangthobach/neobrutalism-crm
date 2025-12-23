'use client'

import React from 'react'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Card } from '@/components/ui/card'
import { Filter } from 'lucide-react'

interface NotificationFiltersProps {
  filterType: string
  filterPriority: string
  onTypeChange: (value: string) => void
  onPriorityChange: (value: string) => void
}

export function NotificationFilters({
  filterType,
  filterPriority,
  onTypeChange,
  onPriorityChange,
}: NotificationFiltersProps) {
  return (
    <Card className="p-4 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white">
      <div className="flex items-center gap-4 flex-wrap">
        <div className="flex items-center gap-2">
          <Filter className="w-5 h-5" />
          <span className="font-bold text-sm">Filters:</span>
        </div>

        {/* Type Filter */}
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium">Type:</label>
          <Select value={filterType} onValueChange={onTypeChange}>
            <SelectTrigger className="w-[180px] border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] h-9">
              <SelectValue placeholder="All types" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Types</SelectItem>
              <SelectItem value="TASK_ASSIGNED">Task Assigned</SelectItem>
              <SelectItem value="TASK_UPDATED">Task Updated</SelectItem>
              <SelectItem value="TASK_COMPLETED">Task Completed</SelectItem>
              <SelectItem value="REMINDER">Reminder</SelectItem>
              <SelectItem value="DEADLINE">Deadline</SelectItem>
              <SelectItem value="TEAM_INVITATION">Team Invitation</SelectItem>
              <SelectItem value="USER_MENTION">User Mention</SelectItem>
              <SelectItem value="SYSTEM">System</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Priority Filter */}
        <div className="flex items-center gap-2">
          <label className="text-sm font-medium">Priority:</label>
          <Select value={filterPriority} onValueChange={onPriorityChange}>
            <SelectTrigger className="w-[150px] border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] h-9">
              <SelectValue placeholder="All priorities" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All Priorities</SelectItem>
              <SelectItem value="URGENT">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 bg-red-500 border border-black" />
                  Urgent
                </div>
              </SelectItem>
              <SelectItem value="HIGH">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 bg-orange-500 border border-black" />
                  High
                </div>
              </SelectItem>
              <SelectItem value="MEDIUM">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 bg-yellow-500 border border-black" />
                  Medium
                </div>
              </SelectItem>
              <SelectItem value="LOW">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 bg-gray-500 border border-black" />
                  Low
                </div>
              </SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
    </Card>
  )
}
