/**
 * Task Card Component
 * Displays task information in a card with Neobrutalism design
 * Can be draggable for Kanban board
 */

'use client'

import Link from 'next/link'
import { Calendar, Clock, User, Building2, UserCircle, CheckSquare, MessageSquare } from 'lucide-react'
import { Task } from '@/types/task'
import { TaskStatusBadge } from './task-status-badge'
import { TaskPriorityBadge } from './task-priority-badge'
import { formatDate } from '@/lib/utils'

interface TaskCardProps {
  task: Task
  onClick?: () => void
  isDraggable?: boolean
  onDragStart?: (e: React.DragEvent, task: Task) => void
  onDragEnd?: (e: React.DragEvent) => void
}

export function TaskCard({ task, onClick, isDraggable = false, onDragStart, onDragEnd }: TaskCardProps) {
  const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && task.status !== 'COMPLETED'
  const completedCount = task.checklistItems?.filter(item => item.isCompleted).length || 0
  const totalCount = task.checklistItems?.length || 0

  const content = (
    <>
      {/* Header */}
      <div className={`border-b-2 border-black px-4 py-3 ${
        isOverdue ? 'bg-red-200' : 'bg-purple-200'
      }`}>
        <div className="flex items-start justify-between gap-2">
          <h3 className="flex-1 font-black uppercase leading-tight">{task.title}</h3>
          <TaskPriorityBadge priority={task.priority} />
        </div>
        <div className="mt-2">
          <TaskStatusBadge status={task.status} />
        </div>
      </div>

      {/* Content */}
      <div className="space-y-3 p-4">
        {/* Description */}
        {task.description && (
          <p className="text-sm font-medium text-gray-700 line-clamp-2">{task.description}</p>
        )}

        {/* Category */}
        <div>
          <span className="rounded border border-black bg-purple-100 px-2 py-1 text-xs font-bold uppercase">
            {task.category}
          </span>
        </div>

        {/* Related To */}
        <div className="space-y-1">
          {task.customerName && (
            <div className="flex items-center gap-2 text-sm">
              <Building2 className="h-4 w-4 text-gray-500" />
              <span className="font-bold">{task.customerName}</span>
            </div>
          )}
          {task.contactName && (
            <div className="flex items-center gap-2 text-sm">
              <UserCircle className="h-4 w-4 text-gray-500" />
              <span className="font-medium">{task.contactName}</span>
            </div>
          )}
        </div>

        {/* Checklist Progress */}
        {totalCount > 0 && (
          <div className="flex items-center gap-2 text-sm">
            <CheckSquare className="h-4 w-4 text-gray-500" />
            <div className="flex-1">
              <div className="flex items-center justify-between text-xs font-bold">
                <span>{completedCount} / {totalCount}</span>
                <span>{Math.round((completedCount / totalCount) * 100)}%</span>
              </div>
              <div className="mt-1 h-2 overflow-hidden rounded-full border border-black bg-gray-200">
                <div
                  className="h-full bg-purple-400 transition-all"
                  style={{ width: `${(completedCount / totalCount) * 100}%` }}
                />
              </div>
            </div>
          </div>
        )}

        {/* Comments Count */}
        {task.comments && task.comments.length > 0 && (
          <div className="flex items-center gap-2 text-sm">
            <MessageSquare className="h-4 w-4 text-gray-500" />
            <span className="font-medium">{task.comments.length} comments</span>
          </div>
        )}

        {/* Due Date & Assigned To */}
        <div className="space-y-1 border-t-2 border-black pt-2">
          {task.dueDate && (
            <div className="flex items-center gap-2 text-sm">
              <Calendar className="h-4 w-4 text-gray-500" />
              <span className={`font-bold ${isOverdue ? 'text-red-600' : ''}`}>
                Due: {formatDate(task.dueDate)}
              </span>
            </div>
          )}
          {task.estimatedHours && (
            <div className="flex items-center gap-2 text-sm">
              <Clock className="h-4 w-4 text-gray-500" />
              <span className="font-medium">{task.estimatedHours}h estimated</span>
            </div>
          )}
          {task.assignedToName && (
            <div className="flex items-center gap-2 text-sm">
              <User className="h-4 w-4 text-gray-500" />
              <span className="font-bold">{task.assignedToName}</span>
            </div>
          )}
        </div>

        {/* Tags */}
        {task.tags && task.tags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {task.tags.map((tag) => (
              <span
                key={tag}
                className="rounded border border-black bg-gray-100 px-2 py-0.5 text-xs font-bold"
              >
                {tag}
              </span>
            ))}
          </div>
        )}
      </div>
    </>
  )

  const cardClasses = `group block transform border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all duration-200 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none ${
    isDraggable ? 'cursor-move' : ''
  }`

  if (isDraggable) {
    return (
      <div
        draggable
        onDragStart={(e) => onDragStart?.(e, task)}
        onDragEnd={onDragEnd}
        className={cardClasses}
        onClick={onClick}
      >
        {content}
      </div>
    )
  }

  if (onClick) {
    return (
      <div onClick={onClick} className={cardClasses + ' cursor-pointer'}>
        {content}
      </div>
    )
  }

  return (
    <Link href={`/admin/tasks/${task.id}`} className={cardClasses}>
      {content}
    </Link>
  )
}
