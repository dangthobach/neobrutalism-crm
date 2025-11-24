/**
 * Task Kanban Board Component
 * 3-column drag-and-drop board: TODO → IN_PROGRESS → COMPLETED
 */

"use client"

import React, { useState } from "react"
import { useRouter } from "next/navigation"
import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
  closestCorners,
} from "@dnd-kit/core"
import { SortableContext, verticalListSortingStrategy } from "@dnd-kit/sortable"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Task, TaskStatus, TaskPriority } from "@/types/task"
import { Calendar, User, AlertCircle, Clock, MoreVertical } from "lucide-react"
import { format } from "date-fns"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { useSortable } from "@dnd-kit/sortable"
import { CSS } from "@dnd-kit/utilities"

interface TaskBoardProps {
  tasks: Task[]
  onTaskStatusChange: (taskId: string, newStatus: TaskStatus) => void
  onTaskEdit?: (task: Task) => void
  onTaskDelete?: (taskId: string) => void
  isLoading?: boolean
  bulkMode?: boolean
  selectedTaskIds?: Set<string>
  onToggleSelect?: (taskId: string) => void
}

interface TaskCardProps {
  task: Task
  onEdit?: (task: Task) => void
  onDelete?: (taskId: string) => void
  bulkMode?: boolean
  isSelected?: boolean
  onToggleSelect?: (taskId: string) => void
}

/**
 * Get priority badge color and label
 */
function getPriorityBadge(priority: TaskPriority) {
  const variants: Record<TaskPriority, { color: string; label: string }> = {
    LOW: { color: "bg-gray-500", label: "Low" },
    MEDIUM: { color: "bg-blue-500", label: "Medium" },
    HIGH: { color: "bg-orange-500", label: "High" },
    URGENT: { color: "bg-red-500", label: "Urgent" },
    CRITICAL: { color: "bg-purple-500", label: "Critical" },
  }
  return variants[priority]
}

/**
 * Check if task is overdue
 */
function isOverdue(dueDate?: string): boolean {
  if (!dueDate) return false
  return new Date(dueDate) < new Date()
}

/**
 * Draggable Task Card
 */
function DraggableTaskCard({ task, onEdit, onDelete, bulkMode, isSelected, onToggleSelect }: TaskCardProps) {
  const router = useRouter()
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: task.id, disabled: bulkMode })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  }

  const priority = getPriorityBadge(task.priority)
  const overdue = isOverdue(task.dueDate)

  const handleCardClick = (e: React.MouseEvent) => {
    if (bulkMode && onToggleSelect) {
      e.stopPropagation()
      onToggleSelect(task.id)
    } else if (!bulkMode) {
      // Navigate to task detail page
      router.push(`/admin/tasks/${task.id}`)
    }
  }

  return (
    <div ref={setNodeRef} style={style} {...(bulkMode ? {} : attributes)} {...(bulkMode ? {} : listeners)}>
      <Card
        className={`p-4 mb-3 border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all ${bulkMode ? 'cursor-pointer' : 'cursor-pointer'} ${isSelected ? 'ring-2 ring-blue-500' : ''}`}
        onClick={handleCardClick}
      >
        <div className="flex items-start justify-between mb-2">
          {bulkMode && onToggleSelect && (
            <input
              type="checkbox"
              checked={isSelected}
              onChange={(e) => {
                e.stopPropagation()
                onToggleSelect(task.id)
              }}
              className="mt-1 h-4 w-4 rounded border-2 border-black mr-2"
              onClick={(e) => e.stopPropagation()}
            />
          )}
          <h3 className="font-bold text-sm line-clamp-2 flex-1 mr-2">
            {task.title}
          </h3>
          {!bulkMode && (onEdit || onDelete) && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                <Button variant="noShadow" size="sm" className="h-6 w-6 p-0">
                  <MoreVertical className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuLabel>Actions</DropdownMenuLabel>
                <DropdownMenuSeparator />
                {onEdit && (
                  <DropdownMenuItem onClick={(e) => {
                    e.stopPropagation()
                    onEdit(task)
                  }}>
                    Edit Task
                  </DropdownMenuItem>
                )}
                {onDelete && (
                  <DropdownMenuItem 
                    className="text-red-600"
                    onClick={(e) => {
                      e.stopPropagation()
                      onDelete(task.id)
                    }}
                  >
                    Delete Task
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>

        {task.description && (
          <p className="text-xs text-gray-600 line-clamp-2 mb-3">
            {task.description}
          </p>
        )}

        <div className="flex items-center gap-2 mb-3">
          <Badge className={priority.color}>
            {priority.label}
          </Badge>
          {task.category && (
            <Badge variant="neutral" className="text-xs">
              {task.category}
            </Badge>
          )}
        </div>

        <div className="space-y-2 text-xs text-gray-600">
          {task.assignedToName && (
            <div className="flex items-center gap-2">
              <User className="h-3 w-3" />
              <span>{task.assignedToName}</span>
            </div>
          )}

          {task.dueDate && (
            <div className={`flex items-center gap-2 ${overdue ? 'text-red-600 font-bold' : ''}`}>
              {overdue ? <AlertCircle className="h-3 w-3" /> : <Calendar className="h-3 w-3" />}
              <span>
                {format(new Date(task.dueDate), "MMM d, yyyy")}
                {overdue && " (Overdue)"}
              </span>
            </div>
          )}

          {task.estimatedHours && (
            <div className="flex items-center gap-2">
              <Clock className="h-3 w-3" />
              <span>{task.estimatedHours}h estimated</span>
            </div>
          )}
        </div>

        {task.tags && task.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-3">
            {task.tags.slice(0, 3).map((tag, index) => (
              <Badge key={index} variant="neutral" className="text-xs">
                {tag}
              </Badge>
            ))}
            {task.tags.length > 3 && (
              <Badge variant="neutral" className="text-xs">
                +{task.tags.length - 3}
              </Badge>
            )}
          </div>
        )}
      </Card>
    </div>
  )
}

/**
 * Kanban Column
 */
interface ColumnProps {
  title: string
  tasks: Task[]
  status: TaskStatus
  color: string
  onEdit?: (task: Task) => void
  onDelete?: (taskId: string) => void
  bulkMode?: boolean
  selectedTaskIds?: Set<string>
  onToggleSelect?: (taskId: string) => void
}

function KanbanColumn({ title, tasks, status, color, onEdit, onDelete, bulkMode, selectedTaskIds, onToggleSelect }: ColumnProps) {
  return (
    <div className="flex-1 min-w-[300px]">
      <div className={`p-3 mb-4 border-2 border-black ${color} font-bold text-center uppercase`}>
        {title} ({tasks.length})
      </div>
      <SortableContext items={tasks.map(t => t.id)} strategy={verticalListSortingStrategy}>
        <div className="min-h-[400px]">
          {tasks.map((task) => (
            <DraggableTaskCard
              key={task.id}
              task={task}
              onEdit={onEdit}
              onDelete={onDelete}
              bulkMode={bulkMode}
              isSelected={selectedTaskIds?.has(task.id)}
              onToggleSelect={onToggleSelect}
            />
          ))}
          {tasks.length === 0 && (
            <div className="text-center text-gray-400 py-8">
              No tasks
            </div>
          )}
        </div>
      </SortableContext>
    </div>
  )
}

/**
 * Main Task Board Component
 */
export function TaskBoard({
  tasks,
  onTaskStatusChange,
  onTaskEdit,
  onTaskDelete,
  isLoading,
  bulkMode,
  selectedTaskIds,
  onToggleSelect,
}: TaskBoardProps) {
  const [activeTask, setActiveTask] = useState<Task | null>(null)

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    })
  )

  // Group tasks by status
  const todoTasks = tasks.filter((t) => t.status === TaskStatus.TODO)
  const inProgressTasks = tasks.filter((t) => t.status === TaskStatus.IN_PROGRESS)
  const completedTasks = tasks.filter((t) => t.status === TaskStatus.COMPLETED)

  const handleDragStart = (event: DragStartEvent) => {
    const task = tasks.find((t) => t.id === event.active.id)
    setActiveTask(task || null)
  }

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    setActiveTask(null)

    if (!over) return

    const taskId = active.id as string
    const task = tasks.find((t) => t.id === taskId)
    if (!task) return

    // Determine new status based on drop zone
    let newStatus: TaskStatus | null = null

    // Check if dropped over another task
    const overTask = tasks.find((t) => t.id === over.id)
    if (overTask) {
      newStatus = overTask.status
    }

    // If status changed, update task
    if (newStatus && newStatus !== task.status) {
      onTaskStatusChange(taskId, newStatus)
    }
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-gray-500">Loading tasks...</div>
      </div>
    )
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
    >
      <div className="flex gap-6 overflow-x-auto pb-4">
        <KanbanColumn
          title="To Do"
          tasks={todoTasks}
          status={TaskStatus.TODO}
          color="bg-yellow-200"
          onEdit={onTaskEdit}
          onDelete={onTaskDelete}
          bulkMode={bulkMode}
          selectedTaskIds={selectedTaskIds}
          onToggleSelect={onToggleSelect}
        />
        <KanbanColumn
          title="In Progress"
          tasks={inProgressTasks}
          status={TaskStatus.IN_PROGRESS}
          color="bg-blue-200"
          onEdit={onTaskEdit}
          onDelete={onTaskDelete}
          bulkMode={bulkMode}
          selectedTaskIds={selectedTaskIds}
          onToggleSelect={onToggleSelect}
        />
        <KanbanColumn
          title="Completed"
          tasks={completedTasks}
          status={TaskStatus.COMPLETED}
          color="bg-green-200"
          onEdit={onTaskEdit}
          onDelete={onTaskDelete}
          bulkMode={bulkMode}
          selectedTaskIds={selectedTaskIds}
          onToggleSelect={onToggleSelect}
        />
      </div>

      <DragOverlay>
        {activeTask && (
          <Card className="p-4 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] opacity-90">
            <h3 className="font-bold text-sm">{activeTask.title}</h3>
          </Card>
        )}
      </DragOverlay>
    </DndContext>
  )
}
