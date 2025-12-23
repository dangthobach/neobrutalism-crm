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
  useSortable,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Progress } from '@/components/ui/progress'
import { Card } from '@/components/ui/card'
import { Plus, CheckCircle2, Circle } from 'lucide-react'
import { ChecklistItem } from './checklist-item'
import { useChecklist, useChecklistProgress } from '@/hooks/useChecklist'
import type { ChecklistItem as ChecklistItemType } from '@/hooks/useChecklist'

interface SortableItemProps {
  item: ChecklistItemType
  onToggle: (id: string) => void
  onUpdate: (id: string, title: string) => void
  onDelete: (id: string) => void
}

/**
 * Sortable wrapper for ChecklistItem
 * Handles drag & drop functionality
 */
function SortableItem({ item, onToggle, onUpdate, onDelete }: SortableItemProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: item.id })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  }

  return (
    <div ref={setNodeRef} style={style}>
      <ChecklistItem
        item={item}
        onToggle={onToggle}
        onUpdate={onUpdate}
        onDelete={onDelete}
        dragHandleProps={{ ...attributes, ...listeners }}
        isDragging={isDragging}
      />
    </div>
  )
}

interface ChecklistProps {
  taskId: string
}

/**
 * Complete checklist component for task detail page
 * Features:
 * - Add new checklist items
 * - Drag & drop reordering
 * - Progress tracking with visual progress bar
 * - Inline editing and deletion
 * - Neobrutalism design
 *
 * Optimized for performance:
 * - React Query caching
 * - Optimistic updates
 * - Minimal re-renders
 */
export function Checklist({ taskId }: ChecklistProps) {
  const [newItemTitle, setNewItemTitle] = useState('')

  const {
    items,
    isLoading,
    addItem,
    isAdding,
    toggleItem,
    updateItem,
    deleteItem,
    reorderItems,
  } = useChecklist(taskId)

  const { data: progressData } = useChecklistProgress(taskId)

  // Configure drag & drop sensors
  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5, // Prevent accidental drags
      },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  )

  // Handle drag end event
  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event

    if (over && active.id !== over.id) {
      const oldIndex = items.findIndex((item) => item.id === active.id)
      const newIndex = items.findIndex((item) => item.id === over.id)

      if (oldIndex !== -1 && newIndex !== -1) {
        const reorderedItems = arrayMove(items, oldIndex, newIndex)
        const itemIds = reorderedItems.map((item) => item.id)
        reorderItems(itemIds)
      }
    }
  }

  // Add new checklist item
  const handleAddItem = () => {
    const trimmedTitle = newItemTitle.trim()
    if (trimmedTitle) {
      addItem(trimmedTitle)
      setNewItemTitle('')
    }
  }

  // Handle Enter key in input
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleAddItem()
    }
  }

  // Handle item update
  const handleUpdate = (itemId: string, title: string) => {
    updateItem({ itemId, title })
  }

  const progress = progressData?.data || { completed: 0, total: 0, percentage: 0, remaining: 0 }
  const hasItems = items.length > 0

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-4 border-black border-t-transparent"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Progress Section */}
      {hasItems && (
        <Card className="p-4 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-gradient-to-br from-blue-50 to-white">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-5 h-5 text-green-600" />
                <span className="font-bold text-sm">Progress</span>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-sm font-medium">
                  {progress.completed} / {progress.total} items
                </span>
                <span className="text-lg font-black text-green-600">
                  {progress.percentage}%
                </span>
              </div>
            </div>

            {/* Progress bar with neobrutalism style */}
            <div className="relative h-6 bg-gray-200 border-2 border-black">
              <div
                className="h-full bg-gradient-to-r from-green-400 to-green-600 border-r-2 border-black transition-all duration-300"
                style={{ width: `${progress.percentage}%` }}
              >
                {progress.percentage > 15 && (
                  <span className="absolute inset-0 flex items-center justify-center text-xs font-bold text-white">
                    {progress.percentage}%
                  </span>
                )}
              </div>
            </div>

            <div className="flex items-center gap-4 text-xs text-gray-600">
              <span className="flex items-center gap-1">
                <Circle className="w-3 h-3 fill-green-500 text-green-500" />
                {progress.completed} completed
              </span>
              <span className="flex items-center gap-1">
                <Circle className="w-3 h-3 fill-gray-300 text-gray-300" />
                {progress.remaining} remaining
              </span>
            </div>
          </div>
        </Card>
      )}

      {/* Add new item input */}
      <Card className="p-4 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white">
        <div className="flex gap-2">
          <Input
            value={newItemTitle}
            onChange={(e) => setNewItemTitle(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Add a new checklist item..."
            className="flex-1 border-2 border-black shadow-none focus-visible:ring-0 focus-visible:ring-offset-0"
            disabled={isAdding}
          />
          <Button
            onClick={handleAddItem}
            disabled={!newItemTitle.trim() || isAdding}
            className="bg-blue-500 hover:bg-blue-600 text-white font-bold border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Plus className="w-4 h-4 mr-2" />
            {isAdding ? 'Adding...' : 'Add'}
          </Button>
        </div>
      </Card>

      {/* Checklist items with drag & drop */}
      {hasItems ? (
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
                <SortableItem
                  key={item.id}
                  item={item}
                  onToggle={toggleItem}
                  onUpdate={handleUpdate}
                  onDelete={deleteItem}
                />
              ))}
            </div>
          </SortableContext>
        </DndContext>
      ) : (
        /* Empty state */
        <Card className="p-12 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-gradient-to-br from-gray-50 to-white">
          <div className="flex flex-col items-center justify-center text-center">
            <div className="w-16 h-16 bg-gray-100 border-4 border-black flex items-center justify-center mb-4">
              <CheckCircle2 className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-black mb-2">No checklist items yet</h3>
            <p className="text-sm text-gray-600 max-w-sm">
              Add checklist items above to break down this task into smaller steps and track your progress.
            </p>
          </div>
        </Card>
      )}
    </div>
  )
}
