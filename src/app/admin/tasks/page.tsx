/**
 * ✅ WEEK 7: Task Management Page
 * 
 * Features:
 * - Kanban board with drag-and-drop
 * - Task statistics cards
 * - Filters by priority, status, assignee, due date
 * - Create/Edit/Delete with permissions
 */

"use client"

import React, { useState, useCallback } from "react"
import Link from "next/link"
import { Plus, ListTodo, CheckCircle2, AlertCircle, TrendingUp, Filter, X, Users, Calendar } from "lucide-react"
import {
  useTasks,
  useTaskStats,
  useChangeTaskStatus,
  useDeleteTask,
  useCreateTask,
  useUpdateTask,
} from "@/hooks/use-tasks"
import { TaskBoard } from "@/components/tasks/task-board"
import { TaskEditModal } from "@/components/tasks/task-edit-modal"
import { Task, TaskStatus, TaskPriority, CreateTaskRequest, UpdateTaskRequest } from "@/types/task"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Calendar as CalendarComponent } from "@/components/ui/calendar"
import { PermissionGuard } from "@/components/auth/permission-guard"
import { usePermission } from "@/hooks/usePermission"
import { toast } from "sonner"
import { format } from "date-fns"
import { cn } from "@/lib/utils"

export default function TasksPage() {
  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingTask, setEditingTask] = useState<Task | null>(null)
  
  // Filter states
  const [priority, setPriority] = useState<TaskPriority | "ALL">("ALL")
  const [assignedToId, setAssignedToId] = useState<string | "ALL">("ALL")
  const [dueDateFrom, setDueDateFrom] = useState<Date | undefined>(undefined)
  const [dueDateTo, setDueDateTo] = useState<Date | undefined>(undefined)
  const [selectedTags, setSelectedTags] = useState<string[]>([])
  const [sortBy, setSortBy] = useState<string>("dueDate")
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("asc")
  
  // Bulk operations state
  const [selectedTaskIds, setSelectedTaskIds] = useState<Set<string>>(new Set())
  const [bulkMode, setBulkMode] = useState(false)

  // Check permissions
  const { canCreate, canEdit, canDelete } = usePermission()

  // Fetch tasks with all filters
  const { data: tasksData, isLoading, error, refetch } = useTasks({
    priority: priority !== "ALL" ? priority : undefined,
    assignedToId: assignedToId !== "ALL" ? assignedToId : undefined,
    dueDateFrom: dueDateFrom ? format(dueDateFrom, "yyyy-MM-dd") : undefined,
    dueDateTo: dueDateTo ? format(dueDateTo, "yyyy-MM-dd") : undefined,
    sortBy,
    sortDirection,
  })

  // Fetch statistics
  const { data: stats } = useTaskStats()

  // Mutations
  const changeStatusMutation = useChangeTaskStatus()
  const deleteMutation = useDeleteTask()
  const createMutation = useCreateTask()
  const updateMutation = useUpdateTask()

  const tasks = tasksData?.content || []

  const handleStatusChange = useCallback(
    (taskId: string, newStatus: TaskStatus) => {
      changeStatusMutation.mutate(
        { id: taskId, status: newStatus },
        {
          onSuccess: () => {
            refetch()
          },
        }
      )
    },
    [changeStatusMutation, refetch]
  )

  // Open modal for creating new task
  const handleCreate = useCallback(() => {
    setEditingTask(null)
    setIsModalOpen(true)
  }, [])

  // Open modal for editing existing task
  const handleEdit = useCallback((task: Task) => {
    setEditingTask(task)
    setIsModalOpen(true)
  }, [])

  // Handle task save (create or update)
  const handleSaveTask = useCallback(
    (data: any) => {
      // Convert Date objects to ISO strings
      const formattedData = {
        ...data,
        dueDate: data.dueDate ? format(data.dueDate, "yyyy-MM-dd'T'HH:mm:ss") : undefined,
        startDate: data.startDate ? format(data.startDate, "yyyy-MM-dd'T'HH:mm:ss") : undefined,
      }
      
      if (editingTask) {
        // Update existing task
        updateMutation.mutate(
          { id: editingTask.id, data: formattedData as UpdateTaskRequest },
          {
            onSuccess: () => {
              setIsModalOpen(false)
              setEditingTask(null)
              refetch()
            },
          }
        )
      } else {
        // Create new task - need to add organizationId
        const createData = {
          ...formattedData,
          organizationId: "default", // TODO: Get from current user context
        } as CreateTaskRequest
        
        createMutation.mutate(createData, {
          onSuccess: () => {
            setIsModalOpen(false)
            refetch()
          },
        })
      }
    },
    [editingTask, createMutation, updateMutation, refetch]
  )

  const handleDelete = useCallback(
    (taskId: string) => {
      if (confirm("Are you sure you want to delete this task?")) {
        deleteMutation.mutate(taskId, {
          onSuccess: () => {
            refetch()
          },
        })
      }
    },
    [deleteMutation, refetch]
  )

  // Bulk operations
  const handleToggleSelect = useCallback((taskId: string) => {
    setSelectedTaskIds(prev => {
      const newSet = new Set(prev)
      if (newSet.has(taskId)) {
        newSet.delete(taskId)
      } else {
        newSet.add(taskId)
      }
      return newSet
    })
  }, [])

  const handleSelectAll = useCallback(() => {
    if (selectedTaskIds.size === tasks.length) {
      setSelectedTaskIds(new Set())
    } else {
      setSelectedTaskIds(new Set(tasks.map(t => t.id)))
    }
  }, [tasks, selectedTaskIds])

  const handleBulkDelete = useCallback(() => {
    if (selectedTaskIds.size === 0) return
    if (confirm(`Delete ${selectedTaskIds.size} selected tasks?`)) {
      Promise.all(
        Array.from(selectedTaskIds).map(id => deleteMutation.mutateAsync(id))
      ).then(() => {
        setSelectedTaskIds(new Set())
        setBulkMode(false)
        refetch()
        toast.success(`Deleted ${selectedTaskIds.size} tasks`)
      })
    }
  }, [selectedTaskIds, deleteMutation, refetch])

  const handleBulkChangeStatus = useCallback((newStatus: TaskStatus) => {
    if (selectedTaskIds.size === 0) return
    Promise.all(
      Array.from(selectedTaskIds).map(id =>
        changeStatusMutation.mutateAsync({ id, status: newStatus })
      )
    ).then(() => {
      setSelectedTaskIds(new Set())
      refetch()
      toast.success(`Updated ${selectedTaskIds.size} tasks to ${newStatus}`)
    })
  }, [selectedTaskIds, changeStatusMutation, refetch])

  const handleBulkAssign = useCallback((userId: string) => {
    if (selectedTaskIds.size === 0) return
    Promise.all(
      Array.from(selectedTaskIds).map(id =>
        updateMutation.mutateAsync({ id, data: { assignedToId: userId } })
      )
    ).then(() => {
      setSelectedTaskIds(new Set())
      refetch()
      toast.success(`Assigned ${selectedTaskIds.size} tasks`)
    })
  }, [selectedTaskIds, updateMutation, refetch])

  const handleClearFilters = () => {
    setPriority("ALL")
    setAssignedToId("ALL")
    setDueDateFrom(undefined)
    setDueDateTo(undefined)
    setSelectedTags([])
    setSortBy("dueDate")
    setSortDirection("asc")
  }

  // Calculate statistics
  const totalTasks = stats?.total || 0
  const completedCount = stats?.byStatus?.[TaskStatus.COMPLETED] || 0
  const inProgressCount = stats?.byStatus?.[TaskStatus.IN_PROGRESS] || 0
  const overdueCount = stats?.overdueCount || 0

  return (
    <div className="container mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-heading text-4xl font-black uppercase">
            Task Management
          </h1>
          <p className="text-gray-600 mt-1">
            Manage tasks with Kanban board{bulkMode && selectedTaskIds.size > 0 && ` - ${selectedTaskIds.size} selected`}
          </p>
        </div>
        <div className="flex gap-2">
          {!bulkMode && (
            <Button
              variant="neutral"
              size="lg"
              onClick={() => setBulkMode(true)}
            >
              Select Multiple
            </Button>
          )}
          {bulkMode && (
            <Button
              variant="reverse"
              size="lg"
              onClick={() => {
                setBulkMode(false)
                setSelectedTaskIds(new Set())
              }}
            >
              Cancel Selection
            </Button>
          )}
          <PermissionGuard routeOrCode="/tasks" permission="canCreate">
            <Button size="lg" onClick={handleCreate}>
              <Plus className="mr-2 h-5 w-5" />
              Create Task
            </Button>
          </PermissionGuard>
        </div>
      </div>

      {/* Bulk Operations Toolbar */}
      {bulkMode && selectedTaskIds.size > 0 && (
        <Card className="p-4 border-2 border-black bg-yellow-100 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <span className="font-bold">{selectedTaskIds.size} tasks selected</span>
              <Button
                variant="neutral"
                size="sm"
                onClick={handleSelectAll}
              >
                {selectedTaskIds.size === tasks.length ? "Deselect All" : "Select All"}
              </Button>
            </div>
            <div className="flex items-center gap-2">
              <Select onValueChange={handleBulkChangeStatus}>
                <SelectTrigger className="w-[160px] border-2 border-black">
                  <SelectValue placeholder="Change Status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={TaskStatus.TODO}>To Do</SelectItem>
                  <SelectItem value={TaskStatus.IN_PROGRESS}>In Progress</SelectItem>
                  <SelectItem value={TaskStatus.IN_REVIEW}>In Review</SelectItem>
                  <SelectItem value={TaskStatus.COMPLETED}>Completed</SelectItem>
                  <SelectItem value={TaskStatus.ON_HOLD}>On Hold</SelectItem>
                </SelectContent>
              </Select>
              
              <Button
                variant="reverse"
                size="sm"
                onClick={handleBulkDelete}
                className="bg-red-500 hover:bg-red-600"
              >
                Delete Selected
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold uppercase text-gray-600">
                Total Tasks
              </p>
              <p className="text-3xl font-black mt-2">{totalTasks}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-blue-200 border-2 border-black flex items-center justify-center">
              <ListTodo className="h-6 w-6" />
            </div>
          </div>
        </Card>

        <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold uppercase text-gray-600">
                In Progress
              </p>
              <p className="text-3xl font-black mt-2">{inProgressCount}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-yellow-200 border-2 border-black flex items-center justify-center">
              <TrendingUp className="h-6 w-6" />
            </div>
          </div>
        </Card>

        <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold uppercase text-gray-600">
                Completed
              </p>
              <p className="text-3xl font-black mt-2">{completedCount}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-green-200 border-2 border-black flex items-center justify-center">
              <CheckCircle2 className="h-6 w-6" />
            </div>
          </div>
        </Card>

        <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-bold uppercase text-gray-600">
                Overdue
              </p>
              <p className="text-3xl font-black mt-2 text-red-600">{overdueCount}</p>
            </div>
            <div className="h-12 w-12 rounded-full bg-red-200 border-2 border-black flex items-center justify-center">
              <AlertCircle className="h-6 w-6" />
            </div>
          </div>
        </Card>
      </div>

      {/* Advanced Filters */}
      <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-heading text-xl font-black uppercase flex items-center">
              <Filter className="mr-2 h-5 w-5" />
              Advanced Filters
            </h2>
            <Button
              variant="neutral"
              size="sm"
              onClick={handleClearFilters}
              className="font-bold"
            >
              <X className="mr-1 h-4 w-4" />
              Clear All
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Priority Filter */}
            <div className="space-y-2">
              <Label className="font-bold text-sm">Priority</Label>
              <Select
                value={priority}
                onValueChange={(value) => setPriority(value as TaskPriority | "ALL")}
              >
                <SelectTrigger className="h-11 border-2 border-black">
                  <SelectValue placeholder="All Priorities" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Priorities</SelectItem>
                  <SelectItem value={TaskPriority.LOW}>Low</SelectItem>
                  <SelectItem value={TaskPriority.MEDIUM}>Medium</SelectItem>
                  <SelectItem value={TaskPriority.HIGH}>High</SelectItem>
                  <SelectItem value={TaskPriority.URGENT}>Urgent</SelectItem>
                  <SelectItem value={TaskPriority.CRITICAL}>Critical</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Assignee Filter */}
            <div className="space-y-2">
              <Label className="font-bold text-sm flex items-center">
                <Users className="mr-1 h-4 w-4" />
                Assignee
              </Label>
              <Select
                value={assignedToId}
                onValueChange={setAssignedToId}
              >
                <SelectTrigger className="h-11 border-2 border-black">
                  <SelectValue placeholder="All Users" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ALL">All Users</SelectItem>
                  <SelectItem value="unassigned">Unassigned</SelectItem>
                  <SelectItem value="me">My Tasks</SelectItem>
                  {/* TODO: Load users from API */}
                </SelectContent>
              </Select>
            </div>

            {/* Due Date From */}
            <div className="space-y-2">
              <Label className="font-bold text-sm flex items-center">
                <Calendar className="mr-1 h-4 w-4" />
                Due Date From
              </Label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button
                    variant="noShadow"
                    className={cn(
                      "w-full h-11 justify-start text-left font-normal border-2 border-black",
                      !dueDateFrom && "text-muted-foreground"
                    )}
                  >
                    <Calendar className="mr-2 h-4 w-4" />
                    {dueDateFrom ? format(dueDateFrom, "PPP") : "Pick a date"}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0" align="start">
                  <CalendarComponent
                    mode="single"
                    selected={dueDateFrom}
                    onSelect={setDueDateFrom}
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
            </div>

            {/* Due Date To */}
            <div className="space-y-2">
              <Label className="font-bold text-sm flex items-center">
                <Calendar className="mr-1 h-4 w-4" />
                Due Date To
              </Label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button
                    variant="noShadow"
                    className={cn(
                      "w-full h-11 justify-start text-left font-normal border-2 border-black",
                      !dueDateTo && "text-muted-foreground"
                    )}
                  >
                    <Calendar className="mr-2 h-4 w-4" />
                    {dueDateTo ? format(dueDateTo, "PPP") : "Pick a date"}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0" align="start">
                  <CalendarComponent
                    mode="single"
                    selected={dueDateTo}
                    onSelect={setDueDateTo}
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
            </div>
          </div>

          {/* Row 2: Sort options */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2 border-t-2 border-gray-200">
            {/* Sort By */}
            <div className="space-y-2">
              <Label className="font-bold text-sm">Sort By</Label>
              <Select value={sortBy} onValueChange={setSortBy}>
                <SelectTrigger className="h-11 border-2 border-black">
                  <SelectValue placeholder="Sort By" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="dueDate">Due Date</SelectItem>
                  <SelectItem value="priority">Priority</SelectItem>
                  <SelectItem value="title">Title</SelectItem>
                  <SelectItem value="createdAt">Created Date</SelectItem>
                  <SelectItem value="updatedAt">Updated Date</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Sort Direction */}
            <div className="space-y-2">
              <Label className="font-bold text-sm">Order</Label>
              <Select
                value={sortDirection}
                onValueChange={(value) => setSortDirection(value as "asc" | "desc")}
              >
                <SelectTrigger className="h-11 border-2 border-black">
                  <SelectValue placeholder="Order" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="asc">Ascending</SelectItem>
                  <SelectItem value="desc">Descending</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>
      </Card>

      {/* Kanban Board */}
      <Card className="p-6 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <TaskBoard
          tasks={tasks}
          onTaskStatusChange={handleStatusChange}
          onTaskEdit={canEdit("/tasks") ? handleEdit : undefined}
          onTaskDelete={canDelete("/tasks") ? handleDelete : undefined}
          isLoading={isLoading}
          bulkMode={bulkMode}
          selectedTaskIds={selectedTaskIds}
          onToggleSelect={handleToggleSelect}
        />
      </Card>

      {/* Error State */}
      {error && (
        <Card className="p-6 border-2 border-red-500 bg-red-50">
          <p className="text-red-600 font-bold">
            Error loading tasks: {error.message}
          </p>
        </Card>
      )}

      {/* Task Edit/Create Modal */}
      <TaskEditModal
        open={isModalOpen}
        onOpenChange={setIsModalOpen}
        task={editingTask}
        onSave={handleSaveTask}
        isLoading={createMutation.isPending || updateMutation.isPending}
      />
    </div>
  )
}
