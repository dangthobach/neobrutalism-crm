"use client"

import { use } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft, Edit, Trash2, MoreHorizontal, Calendar, Clock, User } from 'lucide-react'
import { useTask, useDeleteTask } from '@/hooks/use-tasks'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { TaskStatusBadge } from '@/components/task/task-status-badge'
import { TaskPriorityBadge } from '@/components/task/task-priority-badge'
import { Checklist } from '@/components/tasks/checklist'
import { CommentList } from '@/components/tasks/comment-list'
import { ActivityTimeline } from '@/components/task/activity-timeline'
import { format } from 'date-fns'
import { toast } from 'sonner'

interface TaskDetailPageProps {
  params: Promise<{
    taskId: string
  }>
}

export default function TaskDetailPage({ params }: TaskDetailPageProps) {
  const { taskId } = use(params)
  const router = useRouter()

  const { data: task, isLoading, error } = useTask(taskId)
  const deleteMutation = useDeleteTask()

  // Calculate if task is overdue
  const isOverdue = task?.dueDate && new Date(task.dueDate) < new Date() && task.status !== 'COMPLETED'

  // Calculate progress from checklist items
  const progressPercentage = task?.checklistItems && task.checklistItems.length > 0
    ? Math.round((task.checklistItems.filter(item => item.isCompleted).length / task.checklistItems.length) * 100)
    : undefined

  const handleEdit = () => {
    toast.info('Edit functionality coming in next iteration')
    // TODO: Open edit modal or navigate to edit page
  }

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this task?')) return

    try {
      await deleteMutation.mutateAsync(taskId)
      toast.success('Task deleted successfully')
      router.push('/admin/tasks')
    } catch (error) {
      toast.error('Failed to delete task')
    }
  }

  if (isLoading) {
    return (
      <div className="container mx-auto py-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 w-48 bg-gray-200 rounded"></div>
          <div className="h-12 w-full bg-gray-200 rounded"></div>
          <div className="h-64 w-full bg-gray-200 rounded"></div>
        </div>
      </div>
    )
  }

  if (error || !task) {
    return (
      <div className="container mx-auto py-8">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-red-600">Task Not Found</h2>
          <p className="text-gray-600 mt-2">
            The task you're looking for doesn't exist or you don't have permission to view it.
          </p>
          <Button onClick={() => router.push('/admin/tasks')} className="mt-4">
            <ArrowLeft className="w-4 h-4 mr-2" />
            Back to Tasks
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto py-6 px-4">
        {/* Breadcrumb */}
        <div className="mb-4">
          <button
            onClick={() => router.push('/admin/tasks')}
            className="flex items-center text-sm text-gray-600 hover:text-black transition-colors font-medium"
          >
            <ArrowLeft className="w-4 h-4 mr-1" />
            Back to Tasks
          </button>
        </div>

        {/* Header */}
        <div className="bg-white rounded-lg border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] mb-6 p-6">
          <div className="flex items-start justify-between mb-4">
            <div className="flex-1">
              <div className="flex items-center gap-3 mb-3">
                <TaskStatusBadge status={task.status} />
                <TaskPriorityBadge priority={task.priority} />
                {task.category && (
                  <span className="px-3 py-1 text-sm font-bold bg-gray-100 border-2 border-black rounded">
                    {task.category}
                  </span>
                )}
              </div>

              <h1 className="text-3xl font-black mb-2">{task.title}</h1>

              {task.description && (
                <p className="text-gray-700 text-lg">{task.description}</p>
              )}
            </div>

            {/* Actions */}
            <div className="flex gap-2 ml-4">
              <Button
                variant="neutral"
                onClick={handleEdit}
                className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all"
              >
                <Edit className="w-4 h-4 mr-2" />
                Edit
              </Button>

              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="neutral"
                    size="icon"
                    className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all"
                  >
                    <MoreHorizontal className="w-4 h-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="border-2 border-black">
                  <DropdownMenuItem onClick={handleDelete} className="text-red-600">
                    <Trash2 className="w-4 h-4 mr-2" />
                    Delete Task
                  </DropdownMenuItem>
                  <DropdownMenuItem>Duplicate Task</DropdownMenuItem>
                  <DropdownMenuItem>Convert to Template</DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Content - 2/3 width */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] p-6">
              <Tabs defaultValue="overview" className="w-full">
                <TabsList className="grid w-full grid-cols-5 mb-6 bg-gray-100 p-1 border-2 border-black">
                  <TabsTrigger value="overview" className="data-[state=active]:bg-white data-[state=active]:border-2 data-[state=active]:border-black">
                    Overview
                  </TabsTrigger>
                  <TabsTrigger value="comments" className="data-[state=active]:bg-white data-[state=active]:border-2 data-[state=active]:border-black">
                    Comments
                  </TabsTrigger>
                  <TabsTrigger value="checklist" className="data-[state=active]:bg-white data-[state=active]:border-2 data-[state=active]:border-black">
                    Checklist
                  </TabsTrigger>
                  <TabsTrigger value="activity" className="data-[state=active]:bg-white data-[state=active]:border-2 data-[state=active]:border-black">
                    Activity
                  </TabsTrigger>
                  <TabsTrigger value="attachments" className="data-[state=active]:bg-white data-[state=active]:border-2 data-[state=active]:border-black">
                    Files
                  </TabsTrigger>
                </TabsList>

                <TabsContent value="overview" className="space-y-6">
                  <div>
                    <h3 className="font-black text-lg mb-4 flex items-center">
                      <Calendar className="w-5 h-5 mr-2" />
                      Task Details
                    </h3>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="p-3 bg-gray-50 rounded border-2 border-black">
                        <p className="text-sm text-gray-600 mb-1">Status</p>
                        <TaskStatusBadge status={task.status} />
                      </div>
                      <div className="p-3 bg-gray-50 rounded border-2 border-black">
                        <p className="text-sm text-gray-600 mb-1">Priority</p>
                        <TaskPriorityBadge priority={task.priority} />
                      </div>
                      {task.category && (
                        <div className="p-3 bg-gray-50 rounded border-2 border-black">
                          <p className="text-sm text-gray-600 mb-1">Category</p>
                          <p className="font-bold">{task.category}</p>
                        </div>
                      )}
                      {task.dueDate && (
                        <div className="p-3 bg-gray-50 rounded border-2 border-black">
                          <p className="text-sm text-gray-600 mb-1">Due Date</p>
                          <p className={`font-bold ${isOverdue ? 'text-red-600' : ''}`}>
                            {format(new Date(task.dueDate), 'MMM dd, yyyy')}
                            {isOverdue && ' ⚠️'}
                          </p>
                        </div>
                      )}
                    </div>
                  </div>

                  {task.description && (
                    <div>
                      <h3 className="font-black text-lg mb-3">Description</h3>
                      <div className="p-4 bg-gray-50 rounded border-2 border-black">
                        <p className="text-gray-700 whitespace-pre-wrap">{task.description}</p>
                      </div>
                    </div>
                  )}

                  {task.tags && task.tags.length > 0 && (
                    <div>
                      <h3 className="font-black text-lg mb-3">Tags</h3>
                      <div className="flex flex-wrap gap-2">
                        {task.tags.map((tag) => (
                          <span
                            key={tag}
                            className="px-3 py-1 text-sm font-bold bg-blue-100 text-blue-800 border-2 border-blue-800 rounded shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
                          >
                            #{tag}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </TabsContent>

                <TabsContent value="comments">
                  <CommentList
                    taskId={taskId}
                    currentUserId={task.assignedToId}
                    currentUserName={task.assignedToName || 'User'}
                  />
                </TabsContent>

                <TabsContent value="checklist">
                  <Checklist taskId={taskId} />
                </TabsContent>

                <TabsContent value="activity">
                  <ActivityTimeline taskId={taskId} />
                </TabsContent>

                <TabsContent value="attachments">
                  <div className="text-center py-16 border-2 border-dashed border-gray-300 rounded-lg">
                    <div className="text-6xl mb-4">📎</div>
                    <p className="text-lg font-bold text-gray-700">Attachments Coming Soon</p>
                    <p className="text-sm text-gray-500 mt-2">Upload and manage task attachments</p>
                  </div>
                </TabsContent>
              </Tabs>
            </div>
          </div>

          {/* Sidebar - 1/3 width */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] p-6 space-y-6">
              <div>
                <h3 className="font-black text-lg mb-4 flex items-center">
                  <User className="w-5 h-5 mr-2" />
                  Task Info
                </h3>

                {/* Assignee */}
                <div className="mb-5 pb-5 border-b-2 border-gray-200">
                  <p className="text-sm font-bold text-gray-600 mb-2">Assigned to</p>
                  {task.assignedToName ? (
                    <div className="flex items-center gap-2">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center text-white font-black text-lg border-2 border-black">
                        {task.assignedToName?.charAt(0) || 'U'}
                      </div>
                      <div>
                        <p className="font-bold">{task.assignedToName}</p>
                      </div>
                    </div>
                  ) : (
                    <div className="flex items-center gap-2 text-gray-400">
                      <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center border-2 border-gray-300">
                        ?
                      </div>
                      <span className="font-medium">Unassigned</span>
                    </div>
                  )}
                </div>

                {/* Due Date */}
                <div className="mb-5 pb-5 border-b-2 border-gray-200">
                  <p className="text-sm font-bold text-gray-600 mb-2 flex items-center">
                    <Calendar className="w-4 h-4 mr-1" />
                    Due Date
                  </p>
                  {task.dueDate ? (
                    <div>
                      <p className={`font-bold text-lg ${isOverdue ? 'text-red-600' : 'text-gray-900'}`}>
                        {format(new Date(task.dueDate), 'MMM dd, yyyy')}
                      </p>
                      <p className="text-sm text-gray-500">
                        {format(new Date(task.dueDate), 'HH:mm')}
                      </p>
                      {isOverdue && (
                        <p className="text-xs text-red-600 font-bold mt-1">⚠️ Overdue</p>
                      )}
                    </div>
                  ) : (
                    <span className="text-gray-400 font-medium">No due date set</span>
                  )}
                </div>

                {/* Time Tracking */}
                {(task.estimatedHours || task.actualHours) && (
                  <div className="mb-5 pb-5 border-b-2 border-gray-200">
                    <p className="text-sm font-bold text-gray-600 mb-2 flex items-center">
                      <Clock className="w-4 h-4 mr-1" />
                      Time Tracking
                    </p>
                    <div className="space-y-2">
                      {task.estimatedHours && (
                        <div className="flex justify-between items-center">
                          <span className="text-sm text-gray-600">Estimated</span>
                          <span className="font-bold text-blue-600">{task.estimatedHours}h</span>
                        </div>
                      )}
                      {task.actualHours && (
                        <div className="flex justify-between items-center">
                          <span className="text-sm text-gray-600">Logged</span>
                          <span className="font-bold text-green-600">{task.actualHours}h</span>
                        </div>
                      )}
                      {task.estimatedHours && task.actualHours && (
                        <div className="flex justify-between items-center pt-2 border-t border-gray-200">
                          <span className="text-sm text-gray-600">Remaining</span>
                          <span className={`font-bold ${task.actualHours > task.estimatedHours ? 'text-red-600' : 'text-gray-900'}`}>
                            {Math.max(0, task.estimatedHours - task.actualHours)}h
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Progress */}
                {progressPercentage !== undefined && (
                  <div className="mb-5 pb-5 border-b-2 border-gray-200">
                    <p className="text-sm font-bold text-gray-600 mb-2">Progress</p>
                    <div className="space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="text-2xl font-black">{progressPercentage}%</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-3 border-2 border-black overflow-hidden">
                        <div
                          className="bg-gradient-to-r from-blue-500 to-purple-500 h-full transition-all duration-300"
                          style={{ width: `${progressPercentage}%` }}
                        ></div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Metadata */}
              <div className="pt-4 border-t-2 border-gray-200">
                <h3 className="font-black text-sm mb-3">Metadata</h3>
                <div className="space-y-2 text-xs text-gray-600">
                  <div className="flex justify-between">
                    <span className="font-bold">Created:</span>
                    <span>{format(new Date(task.createdAt), 'MMM dd, yyyy')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="font-bold">Updated:</span>
                    <span>{format(new Date(task.updatedAt), 'MMM dd, yyyy')}</span>
                  </div>
                  {task.createdBy && (
                    <div className="flex justify-between">
                      <span className="font-bold">Created by:</span>
                      <span>{task.createdBy}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
