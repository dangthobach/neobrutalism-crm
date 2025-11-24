/**
 * Checklist Item Component
 * Individual checklist item with checkbox, edit, delete, and drag handle
 */

'use client'

import { useState } from 'react'
import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { GripVertical, MoreVertical, Edit2, Trash2, Check, X } from 'lucide-react'
import { ChecklistItem as ChecklistItemType } from '@/hooks/use-checklist'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

interface ChecklistItemProps {
  item: ChecklistItemType
  onToggle: (itemId: string) => void
  onUpdate: (itemId: string, title: string) => void
  onDelete: (itemId: string) => void
  isDragging?: boolean
}

export function ChecklistItem({
  item,
  onToggle,
  onUpdate,
  onDelete,
  isDragging = false,
}: ChecklistItemProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [editTitle, setEditTitle] = useState(item.title)

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging: isSortableDragging,
  } = useSortable({ id: item.id })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isSortableDragging ? 0.5 : 1,
  }

  const handleSaveEdit = () => {
    if (editTitle.trim()) {
      onUpdate(item.id, editTitle.trim())
      setIsEditing(false)
    }
  }

  const handleCancelEdit = () => {
    setEditTitle(item.title)
    setIsEditing(false)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      handleSaveEdit()
    } else if (e.key === 'Escape') {
      handleCancelEdit()
    }
  }

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`group flex items-center gap-2 border-2 border-black bg-white p-3 shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] ${
        isDragging || isSortableDragging ? 'cursor-grabbing' : ''
      }`}
    >
      {/* Drag Handle */}
      <button
        {...attributes}
        {...listeners}
        className="cursor-grab p-1 opacity-0 transition-opacity hover:bg-gray-100 group-hover:opacity-100"
        aria-label="Kéo để sắp xếp"
      >
        <GripVertical className="h-4 w-4 text-gray-400" />
      </button>

      {/* Checkbox */}
      <Checkbox
        checked={item.completed}
        onCheckedChange={() => onToggle(item.id)}
        className="border-2 border-black data-[state=checked]:bg-green-400 data-[state=checked]:text-black"
        disabled={isEditing}
      />

      {/* Title */}
      {isEditing ? (
        <div className="flex flex-1 items-center gap-2">
          <Input
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
            onKeyDown={handleKeyDown}
            autoFocus
            className="flex-1 border-2 border-black font-medium shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] focus-visible:ring-0"
            maxLength={500}
          />
          <Button
            size="sm"
            variant="ghost"
            onClick={handleCancelEdit}
            className="h-8 w-8 border-2 border-black p-0"
          >
            <X className="h-4 w-4" />
          </Button>
          <Button
            size="sm"
            onClick={handleSaveEdit}
            disabled={!editTitle.trim()}
            className="h-8 border-2 border-black bg-green-400 font-black hover:bg-green-500"
          >
            <Check className="h-4 w-4" />
          </Button>
        </div>
      ) : (
        <>
          <span
            className={`flex-1 font-medium ${
              item.completed ? 'text-gray-500 line-through' : 'text-gray-900'
            }`}
          >
            {item.title}
          </span>

          {/* Actions Menu */}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="sm"
                className="h-8 w-8 border-2 border-black p-0 opacity-0 transition-opacity hover:bg-gray-100 group-hover:opacity-100"
              >
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              align="end"
              className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]"
            >
              <DropdownMenuItem
                onClick={() => setIsEditing(true)}
                className="font-bold"
              >
                <Edit2 className="mr-2 h-4 w-4" />
                Chỉnh sửa
              </DropdownMenuItem>
              <DropdownMenuItem
                onClick={() => onDelete(item.id)}
                className="font-bold text-red-600"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Xóa
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </>
      )}
    </div>
  )
}
