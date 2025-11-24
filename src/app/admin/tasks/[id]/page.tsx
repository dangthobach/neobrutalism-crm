/**
 * Task Detail Page
 * Displays comprehensive task information with tabs for comments, checklist, and activity timeline
 */

'use client'

import { useParams } from 'next/navigation'
import { useTask } from '@/hooks/use-tasks'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Card } from '@/components/ui/card'
import {
  CommentList,
  Checklist,
  ActivityTimeline,
  TaskStatusBadge,
  TaskPriorityBadge,
  TaskCategoryBadge,
} from '@/components/task'
import { Skeleton } from '@/components/ui/skeleton'
import { Button } from '@/components/ui/button'
import { ArrowLeft, Edit, Clock, User, Calendar } from 'lucide-react'
import Link from 'next/link'
import { format, parseISO } from 'date-fns'
import { vi } from 'date-fns/locale'

export default function TaskDetailPage() {
  const params = useParams()
  const taskId = params.id as string
  const { data: task, isLoading } = useTask(taskId)

  // TODO: Get current user from auth context
  const currentUserId = 'current-user-id'

  if (isLoading) {
    return (
      <div className="container max-w-7xl space-y-6 py-8">
        <Skeleton className="h-12 w-full border-2 border-black" />
        <Skeleton className="h-48 w-full border-2 border-black" />
        <Skeleton className="h-96 w-full border-2 border-black" />
      </div>
    )
  }

  if (!task) {
    return (
      <div className="container max-w-7xl py-8">
        <Card className="border-4 border-black p-12 text-center shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
          <p className="mb-4 text-2xl font-black">Task không tồn tại</p>
          <Link href="/admin/tasks">
            <Button className="border-2 border-black bg-purple-400 font-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-purple-500">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Quay lại danh sách
            </Button>
          </Link>
        </Card>
      </div>
    )
  }

  return (
    <div className="container max-w-7xl space-y-6 py-8">
      {/* Back Button */}
      <Link href="/admin/tasks">
        <Button
          variant="outline"
          className="border-2 border-black font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:bg-gray-50"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại
        </Button>
      </Link>

      {/* Task Header Card */}
      <Card className="border-4 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
        {/* Title Section */}
        <div className="border-b-4 border-black bg-gradient-to-r from-purple-200 via-pink-200 to-yellow-200 p-6">
          <div className="mb-4 flex items-start justify-between">
            <div className="flex-1">
              <h1 className="mb-2 text-4xl font-black uppercase leading-tight">
                {task.title}
              </h1>
              {task.description && (
                <p className="text-lg font-medium text-gray-800">
                  {task.description}
                </p>
              )}
            </div>
            <Link href={`/admin/tasks/${taskId}/edit`}>
              <Button
                size="sm"
                className="border-2 border-black bg-blue-400 font-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-blue-500"
              >
                <Edit className="mr-1 h-4 w-4" />
                Sửa
              </Button>
            </Link>
          </div>

          {/* Badges */}
          <div className="flex flex-wrap gap-2">
            <TaskStatusBadge status={task.status} />
            <TaskPriorityBadge priority={task.priority} />
            <TaskCategoryBadge category={task.category} />
          </div>
        </div>

        {/* Info Grid */}
        <div className="grid grid-cols-1 divide-y-2 divide-black md:grid-cols-3 md:divide-x-2 md:divide-y-0">
          {/* Assignee */}
          <div className="p-6">
            <div className="mb-2 flex items-center gap-2">
              <User className="h-5 w-5 text-purple-600" />
              <p className="text-xs font-bold uppercase tracking-wider text-gray-600">
                Người làm
              </p>
            </div>
            <p className="text-xl font-black">
              {task.assignedToName || (
                <span className="text-gray-400">Chưa giao</span>
              )}
            </p>
          </div>

          {/* Due Date */}
          <div className="p-6">
            <div className="mb-2 flex items-center gap-2">
              <Calendar className="h-5 w-5 text-blue-600" />
              <p className="text-xs font-bold uppercase tracking-wider text-gray-600">
                Hạn chót
              </p>
            </div>
            <p className="text-xl font-black">
              {task.dueDate ? (
                <>
                  {format(parseISO(task.dueDate), 'dd/MM/yyyy', {
                    locale: vi,
                  })}
                  {new Date(task.dueDate) < new Date() && (
                    <span className="ml-2 text-base text-red-600">(Quá hạn)</span>
                  )}
                </>
              ) : (
                <span className="text-gray-400">Không có</span>
              )}
            </p>
          </div>

          {/* Estimated Hours */}
          <div className="p-6">
            <div className="mb-2 flex items-center gap-2">
              <Clock className="h-5 w-5 text-green-600" />
              <p className="text-xs font-bold uppercase tracking-wider text-gray-600">
                Thời gian ước tính
              </p>
            </div>
            <p className="text-xl font-black">
              {task.estimatedHours ? (
                <>
                  {task.estimatedHours}h
                  {task.actualHours && (
                    <span className="ml-2 text-base text-gray-600">
                      / {task.actualHours}h thực tế
                    </span>
                  )}
                </>
              ) : (
                <span className="text-gray-400">Không có</span>
              )}
            </p>
          </div>
        </div>

        {/* Additional Info */}
        {(task.customerName || task.contactName || task.tags) && (
          <div className="border-t-4 border-black bg-gray-50 p-6">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
              {task.customerName && (
                <div>
                  <p className="mb-1 text-xs font-bold uppercase text-gray-600">
                    Khách hàng
                  </p>
                  <p className="font-black">{task.customerName}</p>
                </div>
              )}
              {task.contactName && (
                <div>
                  <p className="mb-1 text-xs font-bold uppercase text-gray-600">
                    Liên hệ
                  </p>
                  <p className="font-black">{task.contactName}</p>
                </div>
              )}
              {task.tags && task.tags.length > 0 && (
                <div>
                  <p className="mb-1 text-xs font-bold uppercase text-gray-600">
                    Tags
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {task.tags.map((tag) => (
                      <span
                        key={tag}
                        className="border-2 border-black bg-yellow-200 px-2 py-1 text-xs font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
                      >
                        #{tag}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </Card>

      {/* Tabs */}
      <Tabs defaultValue="overview" className="space-y-6">
        <TabsList className="grid w-full grid-cols-4 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <TabsTrigger
            value="overview"
            className="border-r-2 border-black font-black data-[state=active]:bg-purple-300"
          >
            TỔNG QUAN
          </TabsTrigger>
          <TabsTrigger
            value="checklist"
            className="border-r-2 border-black font-black data-[state=active]:bg-green-300"
          >
            CHECKLIST
          </TabsTrigger>
          <TabsTrigger
            value="comments"
            className="border-r-2 border-black font-black data-[state=active]:bg-blue-300"
          >
            COMMENTS
          </TabsTrigger>
          <TabsTrigger
            value="activity"
            className="font-black data-[state=active]:bg-yellow-300"
          >
            LỊCH SỬ
          </TabsTrigger>
        </TabsList>

        {/* Overview Tab */}
        <TabsContent value="overview" className="space-y-6">
          <Card className="border-4 border-black p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <h2 className="mb-4 border-b-2 border-black pb-2 text-2xl font-black uppercase">
              Chi tiết Task
            </h2>

            <div className="space-y-4">
              {/* Description */}
              <div>
                <p className="mb-2 text-sm font-bold uppercase text-gray-600">
                  Mô tả
                </p>
                <p className="text-base font-medium">
                  {task.description || (
                    <span className="text-gray-400">Không có mô tả</span>
                  )}
                </p>
              </div>

              {/* Dates */}
              <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <div>
                  <p className="mb-2 text-sm font-bold uppercase text-gray-600">
                    Ngày bắt đầu
                  </p>
                  <p className="font-black">
                    {task.startDate
                      ? format(parseISO(task.startDate), 'dd/MM/yyyy HH:mm', {
                          locale: vi,
                        })
                      : 'Chưa có'}
                  </p>
                </div>
                <div>
                  <p className="mb-2 text-sm font-bold uppercase text-gray-600">
                    Ngày hoàn thành
                  </p>
                  <p className="font-black">
                    {task.completedDate
                      ? format(parseISO(task.completedDate), 'dd/MM/yyyy HH:mm', {
                          locale: vi,
                        })
                      : 'Chưa hoàn thành'}
                  </p>
                </div>
                <div>
                  <p className="mb-2 text-sm font-bold uppercase text-gray-600">
                    Ngày nhắc nhở
                  </p>
                  <p className="font-black">
                    {task.reminderDate
                      ? format(parseISO(task.reminderDate), 'dd/MM/yyyy HH:mm', {
                          locale: vi,
                        })
                      : 'Không có'}
                  </p>
                </div>
              </div>

              {/* Recurring */}
              {task.isRecurring && (
                <div>
                  <p className="mb-2 text-sm font-bold uppercase text-gray-600">
                    Task lặp lại
                  </p>
                  <p className="font-black">
                    {task.recurrencePattern || 'Có (chưa cấu hình)'}
                  </p>
                </div>
              )}

              {/* Created & Updated Info */}
              <div className="mt-6 border-t-2 border-black pt-4">
                <div className="grid grid-cols-1 gap-2 text-sm">
                  <p className="font-medium">
                    <span className="font-bold text-gray-600">Tạo bởi:</span>{' '}
                    {task.createdBy} -{' '}
                    {format(parseISO(task.createdAt), 'dd/MM/yyyy HH:mm', {
                      locale: vi,
                    })}
                  </p>
                  <p className="font-medium">
                    <span className="font-bold text-gray-600">Cập nhật:</span>{' '}
                    {task.updatedBy} -{' '}
                    {format(parseISO(task.updatedAt), 'dd/MM/yyyy HH:mm', {
                      locale: vi,
                    })}
                  </p>
                </div>
              </div>
            </div>
          </Card>
        </TabsContent>

        {/* Checklist Tab */}
        <TabsContent value="checklist">
          <Card className="border-4 border-black p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <Checklist taskId={taskId} />
          </Card>
        </TabsContent>

        {/* Comments Tab */}
        <TabsContent value="comments">
          <Card className="border-4 border-black p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <CommentList taskId={taskId} currentUserId={currentUserId} />
          </Card>
        </TabsContent>

        {/* Activity Tab */}
        <TabsContent value="activity">
          <Card className="border-4 border-black p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <ActivityTimeline taskId={taskId} />
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  )
}
