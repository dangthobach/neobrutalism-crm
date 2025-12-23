/**
 * Task Board Component
 * Kanban board layout for task management
 */

'use client'

import { TaskBoard as TaskBoardType, Task, TaskStatus } from '@/types/task'
import { KanbanColumn } from './kanban-column'

interface TaskBoardProps {
  board: TaskBoardType
  onTaskClick?: (task: Task) => void
  onStatusChange?: (taskId: string, newStatus: TaskStatus) => void
  isLoading?: boolean
}

export function TaskBoard({ board, onTaskClick, onStatusChange, isLoading }: TaskBoardProps) {
  if (isLoading) {
    return (
      <div className="rounded border-2 border-black bg-white p-8 text-center">
        <p className="font-bold uppercase">Loading task board...</p>
      </div>
    )
  }

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
      <KanbanColumn
        title="To Do"
        status={TaskStatus.TODO}
        tasks={board.todo}
        color="yellow"
        onTaskClick={onTaskClick}
        onDrop={onStatusChange}
      />
      <KanbanColumn
        title="In Progress"
        status={TaskStatus.IN_PROGRESS}
        tasks={board.inProgress}
        color="blue"
        onTaskClick={onTaskClick}
        onDrop={onStatusChange}
      />
      <KanbanColumn
        title="In Review"
        status={TaskStatus.IN_REVIEW}
        tasks={board.inReview}
        color="purple"
        onTaskClick={onTaskClick}
        onDrop={onStatusChange}
      />
      <KanbanColumn
        title="Completed"
        status={TaskStatus.COMPLETED}
        tasks={board.completed}
        color="green"
        onTaskClick={onTaskClick}
        onDrop={onStatusChange}
      />
    </div>
  )
}
