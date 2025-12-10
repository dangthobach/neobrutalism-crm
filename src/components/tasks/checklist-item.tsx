'use client'

import { useState } from 'react'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { GripVertical, Trash2, Edit2, Check, X } from 'lucide-react'
import { cn } from '@/lib/utils'

interface ChecklistItemProps {
  item: {
    id: string
    title: string
    completed: boolean
    position: number
  }
  onToggle: (itemId: string) => void
  onUpdate: (itemId: string, title: string) => void
  onDelete: (itemId: string) => void
  dragHandleProps?: any
  isDragging?: boolean
}

/**
 * Individual checklist item component
 * Features:
 * - Checkbox for completion toggle
 * - Inline editing on double-click
 * - Drag handle for reordering
 * - Delete button on hover
 * - Neobrutalism design with bold borders
 */
export function ChecklistItem({
  item,
  onToggle,
  onUpdate,
  onDelete,
  dragHandleProps,
  isDragging = false,
}: ChecklistItemProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [editTitle, setEditTitle] = useState(item.title)

  const handleSave = () => {
    if (editTitle.trim() && editTitle !== item.title) {
      onUpdate(item.id, editTitle)
    }
    setIsEditing(false)
  }

  const handleCancel = () => {
    setEditTitle(item.title)
    setIsEditing(false)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSave()
    } else if (e.key === 'Escape') {
      handleCancel()
    }
  }

  return (
    <div
      className={cn(
        "group flex items-center gap-3 p-3 bg-white border-2 border-black rounded-none",
        "hover:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all",
        item.completed && "bg-gray-50 opacity-70",
        isDragging && "opacity-50 shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]"
      )}
    >
      {/* Drag handle */}
      <div
        {...dragHandleProps}
        className="cursor-grab active:cursor-grabbing touch-none"
      >
        <GripVertical className="w-4 h-4 text-gray-400 hover:text-gray-600" />
      </div>

      {/* Checkbox */}
      <Checkbox
        checked={item.completed}
        onCheckedChange={() => onToggle(item.id)}
        className="border-2 border-black data-[state=checked]:bg-green-500 data-[state=checked]:border-black"
      />

      {/* Title (editable) */}
      {isEditing ? (
        <div className="flex-1 flex gap-2 items-center">
          <Input
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
            onKeyDown={handleKeyDown}
            onBlur={handleSave}
            className="flex-1 border-2 border-black shadow-none focus-visible:ring-0 focus-visible:ring-offset-0"
            autoFocus
          />
          <Button
            size="sm"
            onClick={handleSave}
            className="h-8 bg-green-500 hover:bg-green-600 border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5"
          >
            <Check className="w-4 h-4" />
          </Button>
          <Button
            size="sm"
            variant="neutral"
            onClick={handleCancel}
            className="h-8 border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-0.5 hover:translate-y-0.5"
          >
            <X className="w-4 h-4" />
          </Button>
        </div>
      ) : (
        <span
          className={cn(
            "flex-1 font-medium text-sm select-none",
            item.completed && "line-through text-gray-500"
          )}
          onDoubleClick={() => !item.completed && setIsEditing(true)}
          title="Double-click to edit"
        >
          {item.title}
        </span>
      )}

      {/* Actions (show on hover) */}
      {!isEditing && (
        <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <Button
            size="sm"
            variant="noShadow"
            onClick={() => setIsEditing(true)}
            className="h-8 w-8 p-0 hover:bg-gray-100"
            title="Edit"
          >
            <Edit2 className="w-3 h-3" />
          </Button>
          <Button
            size="sm"
            variant="noShadow"
            onClick={() => {
              if (confirm('Delete this checklist item?')) {
                onDelete(item.id)
              }
            }}
            className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
            title="Delete"
          >
            <Trash2 className="w-3 h-3" />
          </Button>
        </div>
      )}
    </div>
  )
}
