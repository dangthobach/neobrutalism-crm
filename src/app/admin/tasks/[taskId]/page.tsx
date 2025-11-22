"use client"

import { useState, useCallback } from "react"
import { useParams, useRouter } from "next/navigation"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { format, parseISO } from "date-fns"
import { vi } from "date-fns/locale"
import { 
  ArrowLeft, 
  Edit, 
  Trash2, 
  Calendar, 
  User, 
  Tag, 
  Clock,
  CheckSquare,
  MessageSquare,
  Activity,
  Plus,
  Check,
  X
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Textarea } from "@/components/ui/textarea"
import { Input } from "@/components/ui/input"
import { Checkbox } from "@/components/ui/checkbox"
import { TaskEditModal } from "@/components/tasks/task-edit-modal"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import {
  taskApi,
} from "@/lib/api/tasks"
import type {
  Task,
  UpdateTaskRequest,
  TaskComment,
  TaskChecklistItem,
  TaskActivity,
} from "@/types/task"

export default function TaskDetailPage() {
  const params = useParams()
  const router = useRouter()
  const queryClient = useQueryClient()
  const taskId = params.taskId as string

  const [isEditModalOpen, setIsEditModalOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [newComment, setNewComment] = useState("")
  const [newChecklistItem, setNewChecklistItem] = useState("")

  // Fetch task details
  const { data: task, isLoading } = useQuery({
    queryKey: ["task", taskId],
    queryFn: () => taskApi.getById(taskId),
  })

  // Fetch task comments
  const { data: comments = [] } = useQuery<TaskComment[]>({
    queryKey: ["task-comments", taskId],
    queryFn: () => taskApi.getComments(taskId),
    enabled: !!taskId,
  })

  // Fetch task activities
  const { data: activities = [] } = useQuery<TaskActivity[]>({
    queryKey: ["task-activities", taskId],
    queryFn: async () => {
      // TODO: Implement activities API endpoint
      return []
    },
    enabled: !!taskId,
  })

  // Update task mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTaskRequest }) =>
      taskApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["task", taskId] })
      queryClient.invalidateQueries({ queryKey: ["tasks"] })
    },
  })

  // Delete task mutation
  const deleteMutation = useMutation({
    mutationFn: taskApi.delete,
    onSuccess: () => {
      router.push("/admin/tasks")
    },
  })

  // Add comment mutation
  const addCommentMutation = useMutation({
    mutationFn: (content: string) => taskApi.addComment({ taskId, content }),
    onSuccess: () => {
      setNewComment("")
      queryClient.invalidateQueries({ queryKey: ["task-comments", taskId] })
    },
  })

  // Toggle checklist item
  const toggleChecklistItem = useCallback(
    (itemId: string) => {
      if (!task) return

      const updatedChecklist = (task.checklistItems || []).map((item) =>
        item.id === itemId ? { ...item, isCompleted: !item.isCompleted } : item
      )

      updateMutation.mutate({
        id: taskId,
        data: { checklistItems: updatedChecklist },
      })
    },
    [task, taskId, updateMutation]
  )

  // Add checklist item
  const addChecklistItem = useCallback(() => {
    if (!task || !newChecklistItem.trim()) return

    const newItem: TaskChecklistItem = {
      id: crypto.randomUUID(),
      text: newChecklistItem,
      isCompleted: false,
    }

    updateMutation.mutate({
      id: taskId,
      data: { checklistItems: [...(task.checklistItems || []), newItem] },
    })

    setNewChecklistItem("")
  }, [task, newChecklistItem, taskId, updateMutation])

  // Delete checklist item
  const deleteChecklistItem = useCallback(
    (itemId: string) => {
      if (!task) return

      const updatedChecklist = (task.checklistItems || []).filter((item) => item.id !== itemId)

      updateMutation.mutate({
        id: taskId,
        data: { checklistItems: updatedChecklist },
      })
    },
    [task, taskId, updateMutation]
  )

  // Add comment
  const handleAddComment = useCallback(() => {
    if (!newComment.trim()) return
    addCommentMutation.mutate(newComment)
  }, [newComment, addCommentMutation])

  // Handle task update from modal
  const handleSaveTask = useCallback(
    (data: any) => {
      const formattedData = {
        ...data,
        dueDate: data.dueDate ? format(data.dueDate, "yyyy-MM-dd'T'HH:mm:ss") : undefined,
        startDate: data.startDate ? format(data.startDate, "yyyy-MM-dd'T'HH:mm:ss") : undefined,
      }

      updateMutation.mutate(
        { id: taskId, data: formattedData as UpdateTaskRequest },
        {
          onSuccess: () => {
            setIsEditModalOpen(false)
          },
        }
      )
    },
    [taskId, updateMutation]
  )

  // Handle delete
  const handleDelete = useCallback(() => {
    deleteMutation.mutate(taskId)
  }, [taskId, deleteMutation])

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-lg">Đang tải...</div>
      </div>
    )
  }

  if (!task) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-4">
        <div className="text-lg">Không tìm thấy công việc</div>
        <Button onClick={() => router.push("/admin/tasks")}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Quay lại danh sách
        </Button>
      </div>
    )
  }

  const priorityColors: Record<string, string> = {
    LOW: "bg-blue-500 text-white",
    MEDIUM: "bg-yellow-500 text-black",
    HIGH: "bg-orange-500 text-white",
    URGENT: "bg-orange-600 text-white",
    CRITICAL: "bg-red-500 text-white",
  }

  const statusColors: Record<string, string> = {
    TODO: "bg-gray-500 text-white",
    IN_PROGRESS: "bg-blue-500 text-white",
    IN_REVIEW: "bg-purple-500 text-white",
    COMPLETED: "bg-green-500 text-white",
    CANCELLED: "bg-red-500 text-white",
    ON_HOLD: "bg-yellow-500 text-black",
  }

  const statusLabels: Record<string, string> = {
    TODO: "Chưa làm",
    IN_PROGRESS: "Đang làm",
    IN_REVIEW: "Đang xem xét",
    COMPLETED: "Hoàn thành",
    CANCELLED: "Đã hủy",
    ON_HOLD: "Tạm dừng",
  }

  const priorityLabels: Record<string, string> = {
    LOW: "Thấp",
    MEDIUM: "Trung bình",
    HIGH: "Cao",
    URGENT: "Gấp",
    CRITICAL: "Khẩn cấp",
  }

  return (
    <div className="container mx-auto p-6 max-w-6xl">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button
            variant="noShadow"
            onClick={() => router.push("/admin/tasks")}
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Quay lại
          </Button>
          <h1 className="text-3xl font-black">{task.title}</h1>
        </div>
        <div className="flex gap-2">
          <Button onClick={() => setIsEditModalOpen(true)}>
            <Edit className="mr-2 h-4 w-4" />
            Sửa
          </Button>
          <Button
            variant="reverse"
            className="bg-red-500"
            onClick={() => setIsDeleteDialogOpen(true)}
          >
            <Trash2 className="mr-2 h-4 w-4" />
            Xóa
          </Button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Content - Left Column */}
        <div className="lg:col-span-2 space-y-6">
          {/* Description */}
          <Card className="p-6">
            <h2 className="text-xl font-bold mb-4">Mô tả</h2>
            <p className="text-gray-700 whitespace-pre-wrap">
              {task.description || "Không có mô tả"}
            </p>
          </Card>

          {/* Checklist */}
          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <CheckSquare className="h-5 w-5" />
                <h2 className="text-xl font-bold">Danh sách công việc</h2>
                <Badge variant="neutral">
                  {(task.checklistItems || []).filter((item) => item.isCompleted).length}/
                  {(task.checklistItems || []).length}
                </Badge>
              </div>
            </div>

            <div className="space-y-2 mb-4">
              {(task.checklistItems || []).map((item) => (
                <div
                  key={item.id}
                  className="flex items-center gap-3 p-3 border-2 border-black rounded-md hover:bg-gray-50 group"
                >
                  <Checkbox
                    checked={item.isCompleted}
                    onCheckedChange={() => toggleChecklistItem(item.id)}
                  />
                  <span
                    className={`flex-1 ${
                      item.isCompleted ? "line-through text-gray-500" : ""
                    }`}
                  >
                    {item.text}
                  </span>
                  <Button
                    variant="noShadow"
                    size="sm"
                    className="opacity-0 group-hover:opacity-100 transition-opacity"
                    onClick={() => deleteChecklistItem(item.id)}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>
              ))}
            </div>

            {/* Add new checklist item */}
            <div className="flex gap-2">
              <Input
                placeholder="Thêm mục mới..."
                value={newChecklistItem}
                onChange={(e) => setNewChecklistItem(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault()
                    addChecklistItem()
                  }
                }}
              />
              <Button onClick={addChecklistItem}>
                <Plus className="h-4 w-4" />
              </Button>
            </div>
          </Card>

          {/* Comments */}
          <Card className="p-6">
            <div className="flex items-center gap-2 mb-4">
              <MessageSquare className="h-5 w-5" />
              <h2 className="text-xl font-bold">Bình luận</h2>
              <Badge variant="neutral">{comments.length}</Badge>
            </div>

            {/* Comments list */}
            <div className="space-y-4 mb-4">
              {comments.length === 0 ? (
                <p className="text-gray-500 text-center py-4">
                  Chưa có bình luận nào
                </p>
              ) : (
                comments.map((comment) => (
                  <div key={comment.id} className="border-2 border-black rounded-md p-4">
                    <div className="flex items-center gap-2 mb-2">
                      <User className="h-4 w-4" />
                      <span className="font-bold">{comment.author || comment.createdByName || 'Unknown'}</span>
                      <span className="text-sm text-gray-500">
                        {format(parseISO(comment.createdAt), "dd/MM/yyyy HH:mm", {
                          locale: vi,
                        })}
                      </span>
                    </div>
                    <p className="text-gray-700">{comment.content}</p>
                  </div>
                ))
              )}
            </div>

            {/* Add comment */}
            <div className="space-y-2">
              <Textarea
                placeholder="Viết bình luận..."
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                rows={3}
              />
              <div className="flex justify-end">
                <Button onClick={handleAddComment}>
                  <MessageSquare className="mr-2 h-4 w-4" />
                  Gửi bình luận
                </Button>
              </div>
            </div>
          </Card>

          {/* Activity Timeline */}
          <Card className="p-6">
            <div className="flex items-center gap-2 mb-4">
              <Activity className="h-5 w-5" />
              <h2 className="text-xl font-bold">Lịch sử hoạt động</h2>
            </div>

            <div className="space-y-4">
              {activities.length === 0 ? (
                <p className="text-gray-500 text-center py-4">
                  Chưa có hoạt động nào
                </p>
              ) : (
                activities.map((activity, index) => (
                  <div
                    key={activity.id}
                    className="flex gap-4 relative"
                  >
                    {index !== activities.length - 1 && (
                      <div className="absolute left-2 top-6 bottom-0 w-0.5 bg-gray-300" />
                    )}
                    <div className="flex-shrink-0 w-4 h-4 rounded-full bg-blue-500 border-2 border-white mt-1 z-10" />
                    <div className="flex-1">
                      <p className="font-medium">{activity.description}</p>
                      <p className="text-sm text-gray-500">
                        {format(parseISO(activity.createdAt), "dd/MM/yyyy HH:mm", {
                          locale: vi,
                        })}
                      </p>
                    </div>
                  </div>
                ))
              )}
            </div>
          </Card>
        </div>

        {/* Sidebar - Right Column */}
        <div className="space-y-6">
          {/* Status & Priority */}
          <Card className="p-6">
            <h2 className="text-xl font-bold mb-4">Chi tiết</h2>
            
            <div className="space-y-4">
              <div>
                <label className="text-sm font-medium text-gray-600">Trạng thái</label>
                <Badge className={`${statusColors[task.status]} mt-1 w-full justify-center`}>
                  {statusLabels[task.status]}
                </Badge>
              </div>

              <div>
                <label className="text-sm font-medium text-gray-600">Độ ưu tiên</label>
                <Badge className={`${priorityColors[task.priority]} mt-1 w-full justify-center`}>
                  {priorityLabels[task.priority]}
                </Badge>
              </div>

              {task.category && (
                <div>
                  <label className="text-sm font-medium text-gray-600">Danh mục</label>
                  <p className="mt-1 font-medium">{task.category}</p>
                </div>
              )}
            </div>
          </Card>

          {/* Dates */}
          <Card className="p-6">
            <div className="flex items-center gap-2 mb-4">
              <Calendar className="h-5 w-5" />
              <h2 className="text-xl font-bold">Thời gian</h2>
            </div>

            <div className="space-y-3">
              {task.startDate && (
                <div>
                  <label className="text-sm font-medium text-gray-600">Ngày bắt đầu</label>
                  <p className="mt-1">
                    {format(parseISO(task.startDate), "dd/MM/yyyy", { locale: vi })}
                  </p>
                </div>
              )}

              {task.dueDate && (
                <div>
                  <label className="text-sm font-medium text-gray-600">Hạn chót</label>
                  <p className="mt-1">
                    {format(parseISO(task.dueDate), "dd/MM/yyyy", { locale: vi })}
                  </p>
                </div>
              )}

              {task.completedDate && (
                <div>
                  <label className="text-sm font-medium text-gray-600">Hoàn thành lúc</label>
                  <p className="mt-1">
                    {format(parseISO(task.completedDate), "dd/MM/yyyy HH:mm", {
                      locale: vi,
                    })}
                  </p>
                </div>
              )}
            </div>
          </Card>

          {/* Assignee */}
          {task.assignedToName && (
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <User className="h-5 w-5" />
                <h2 className="text-xl font-bold">Người phụ trách</h2>
              </div>
              <p className="font-medium">{task.assignedToName}</p>
            </Card>
          )}

          {/* Tags */}
          {task.tags && task.tags.length > 0 && (
            <Card className="p-6">
              <div className="flex items-center gap-2 mb-4">
                <Tag className="h-5 w-5" />
                <h2 className="text-xl font-bold">Nhãn</h2>
              </div>
              <div className="flex flex-wrap gap-2">
                {task.tags.map((tag, index) => (
                  <Badge key={index} variant="neutral">
                    {tag}
                  </Badge>
                ))}
              </div>
            </Card>
          )}

          {/* Metadata */}
          <Card className="p-6">
            <div className="flex items-center gap-2 mb-4">
              <Clock className="h-5 w-5" />
              <h2 className="text-xl font-bold">Thông tin</h2>
            </div>

            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Tạo lúc:</span>
                <span className="font-medium">
                  {format(parseISO(task.createdAt), "dd/MM/yyyy HH:mm", {
                    locale: vi,
                  })}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Cập nhật:</span>
                <span className="font-medium">
                  {format(parseISO(task.updatedAt), "dd/MM/yyyy HH:mm", {
                    locale: vi,
                  })}
                </span>
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* Edit Modal */}
      {isEditModalOpen && (
        <TaskEditModal
          open={isEditModalOpen}
          onOpenChange={setIsEditModalOpen}
          onSave={handleSaveTask}
          task={task}
        />
      )}

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xác nhận xóa</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc chắn muốn xóa công việc "{task.title}"? Hành động này không
              thể hoàn tác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete}>Xóa</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
