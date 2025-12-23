/**
 * Kanban Column Component
 * Droppable column for Kanban board
 */

'use client'

import { Task, TaskStatus } from '@/types/task'
import { TaskCard } from './task-card'

interface KanbanColumnProps {
  title: string
  status: TaskStatus
  tasks: Task[]
  color: string
  onTaskClick?: (task: Task) => void
  onDrop?: (taskId: string, newStatus: TaskStatus) => void
}

export function KanbanColumn({ title, status, tasks, color, onTaskClick, onDrop }: KanbanColumnProps) {
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    const taskId = e.dataTransfer.getData('taskId')
    if (taskId && onDrop) {
      onDrop(taskId, status)
    }
  }

  const handleDragStart = (e: React.DragEvent, task: Task) => {
    e.dataTransfer.setData('taskId', task.id)
  }

  const bgColor = {
    yellow: 'bg-yellow-50',
    blue: 'bg-blue-50',
    purple: 'bg-purple-50',
    green: 'bg-green-50',
    gray: 'bg-gray-50',
    red: 'bg-red-50',
  }[color] || 'bg-gray-50'

  const headerColor = {
    yellow: 'bg-yellow-200',
    blue: 'bg-blue-200',
    purple: 'bg-purple-200',
    green: 'bg-green-200',
    gray: 'bg-gray-200',
    red: 'bg-red-200',
  }[color] || 'bg-gray-200'

  return (
    <div
      className="flex h-full min-h-[600px] flex-col rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]"
      onDragOver={handleDragOver}
      onDrop={handleDrop}
    >
      {/* Header */}
      <div className={`border-b-2 border-black ${headerColor} px-4 py-3`}>
        <div className="flex items-center justify-between">
          <h3 className="font-black uppercase">{title}</h3>
          <span className="rounded-full border-2 border-black bg-white px-3 py-1 text-sm font-black">
            {tasks.length}
          </span>
        </div>
      </div>

      {/* Tasks */}
      <div className={`flex-1 space-y-3 overflow-y-auto p-4 ${bgColor}`}>
        {tasks.length === 0 ? (
          <div className="rounded border-2 border-dashed border-gray-300 p-4 text-center">
            <p className="text-sm font-bold uppercase text-gray-400">No tasks</p>
          </div>
        ) : (
          tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onClick={onTaskClick ? () => onTaskClick(task) : undefined}
              isDraggable={!!onDrop}
              onDragStart={handleDragStart}
            />
          ))
        )}
      </div>
    </div>
  )
}
