/**
 * Checklist Component
 * Displays task checklist with progress bar, drag-and-drop reordering, and add/edit/delete functionality
 */

'use client'

import { useState } from 'react'
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from '@dnd-kit/core'
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CheckSquare, Plus, Loader2 } from 'lucide-react'
import {
  useChecklist,
  useChecklistProgress,
  useCreateChecklistItem,
  useUpdateChecklistItem,
  useToggleChecklistItem,
  useReorderChecklist,
  useDeleteChecklistItem,
} from '@/hooks/use-checklist'
import { ChecklistItem } from './checklist-item'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Progress } from '@/components/ui/progress'
import { Skeleton } from '@/components/ui/skeleton'

interface ChecklistProps {
  taskId: string
}

export function Checklist({ taskId }: ChecklistProps) {
  const [isAdding, setIsAdding] = useState(false)
  const [newItemTitle, setNewItemTitle] = useState('')

  // Fetch data
  const { data: items = [], isLoading } = useChecklist(taskId)
  const { data: progress } = useChecklistProgress(taskId)

  // Mutations
  const createMutation = useCreateChecklistItem(taskId)
  const updateMutation = useUpdateChecklistItem(taskId)
  const toggleMutation = useToggleChecklistItem(taskId)
  const reorderMutation = useReorderChecklist(taskId)
  const deleteMutation = useDeleteChecklistItem(taskId)

  // Drag and drop sensors
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  )

  const handleAddItem = () => {
    if (newItemTitle.trim()) {
      createMutation.mutate(
        { title: newItemTitle.trim() },
        {
          onSuccess: () => {
            setNewItemTitle('')
            setIsAdding(false)
          },
        }
      )
    }
  }

  const handleToggle = (itemId: string) => {
    toggleMutation.mutate(itemId)
  }

  const handleUpdate = (itemId: string, title: string) => {
    updateMutation.mutate({ itemId, request: { title } })
  }

  const handleDelete = (itemId: string) => {
    if (confirm('Bạn có chắc muốn xóa mục này?')) {
      deleteMutation.mutate(itemId)
    }
  }

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event

    if (over && active.id !== over.id) {
      const oldIndex = items.findIndex((item) => item.id === active.id)
      const newIndex = items.findIndex((item) => item.id === over.id)

      const reorderedItems = arrayMove(items, oldIndex, newIndex)
      const itemIds = reorderedItems.map((item) => item.id)

      reorderMutation.mutate({ itemIds })
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      handleAddItem()
    } else if (e.key === 'Escape') {
      setIsAdding(false)
      setNewItemTitle('')
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-full border-2 border-black" />
        <Skeleton className="h-16 w-full border-2 border-black" />
        <Skeleton className="h-16 w-full border-2 border-black" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <CheckSquare className="h-5 w-5" />
          <h3 className="text-lg font-black uppercase">
            Checklist ({progress?.completed || 0}/{progress?.total || 0})
          </h3>
        </div>
        {(createMutation.isPending ||
          updateMutation.isPending ||
          toggleMutation.isPending ||
          deleteMutation.isPending ||
          reorderMutation.isPending) && (
          <Loader2 className="h-4 w-4 animate-spin text-purple-600" />
        )}
      </div>

      {/* Progress Bar */}
      {progress && progress.total > 0 && (
        <div className="border-2 border-black bg-white p-4 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="mb-2 flex items-center justify-between text-sm font-bold">
            <span>Tiến độ hoàn thành</span>
            <span className="text-purple-600">{progress.percentage}%</span>
          </div>
          <Progress
            value={progress.percentage}
            className="h-3 border-2 border-black"
          />
        </div>
      )}

      {/* Checklist Items */}
      {items.length === 0 && !isAdding ? (
        <div className="border-2 border-black bg-gray-50 p-8 text-center shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <CheckSquare className="mx-auto mb-2 h-12 w-12 text-gray-400" />
          <p className="font-bold text-gray-600">Chưa có checklist</p>
          <p className="mt-1 text-sm text-gray-500">
            Thêm các công việc cần hoàn thành
          </p>
        </div>
      ) : (
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={handleDragEnd}
        >
          <SortableContext
            items={items.map((item) => item.id)}
            strategy={verticalListSortingStrategy}
          >
            <div className="space-y-2">
              {items.map((item) => (
                <ChecklistItem
                  key={item.id}
                  item={item}
                  onToggle={handleToggle}
                  onUpdate={handleUpdate}
                  onDelete={handleDelete}
                />
              ))}
            </div>
          </SortableContext>
        </DndContext>
      )}

      {/* Add Item Form */}
      {isAdding ? (
        <div className="flex gap-2 border-2 border-black bg-white p-3 shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
          <Input
            value={newItemTitle}
            onChange={(e) => setNewItemTitle(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Nhập tên công việc..."
            autoFocus
            className="flex-1 border-2 border-black font-medium shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] focus-visible:ring-0"
            maxLength={500}
            disabled={createMutation.isPending}
          />
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setIsAdding(false)
              setNewItemTitle('')
            }}
            className="border-2 border-black font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            disabled={createMutation.isPending}
          >
            Hủy
          </Button>
          <Button
            size="sm"
            onClick={handleAddItem}
            disabled={!newItemTitle.trim() || createMutation.isPending}
            className="border-2 border-black bg-green-400 font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:bg-green-500"
          >
            Thêm
          </Button>
        </div>
      ) : (
        <Button
          onClick={() => setIsAdding(true)}
          variant="outline"
          className="w-full border-2 border-black font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:bg-purple-50"
        >
          <Plus className="mr-2 h-4 w-4" />
          Thêm công việc
        </Button>
      )}
    </div>
  )
}
