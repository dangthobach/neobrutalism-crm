/**
 * Comment Item Component
 * Displays a single comment with edit/delete actions and threading support
 */

'use client'

import { useState } from 'react'
import { formatDistanceToNow } from 'date-fns'
import { vi } from 'date-fns/locale'
import { MoreVertical, Edit2, Trash2, Reply, Check, X } from 'lucide-react'
import { Comment } from '@/hooks/use-comments'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'

interface CommentItemProps {
  comment: Comment
  onEdit?: (commentId: string, content: string) => void
  onDelete?: (commentId: string) => void
  onReply?: (parentId: string) => void
  isOwner?: boolean
  level?: number
  showReplyButton?: boolean
}

export function CommentItem({
  comment,
  onEdit,
  onDelete,
  onReply,
  isOwner = false,
  level = 0,
  showReplyButton = true,
}: CommentItemProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [editContent, setEditContent] = useState(comment.content)

  const handleSaveEdit = () => {
    if (editContent.trim() && onEdit) {
      onEdit(comment.id, editContent.trim())
      setIsEditing(false)
    }
  }

  const handleCancelEdit = () => {
    setEditContent(comment.content)
    setIsEditing(false)
  }

  if (comment.deleted) {
    return (
      <div
        className={`border-2 border-black bg-gray-100 p-3 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] ${
          level > 0 ? 'ml-12' : ''
        }`}
      >
        <p className="text-sm italic text-gray-500">[Comment đã bị xóa]</p>
      </div>
    )
  }

  return (
    <div
      className={`border-2 border-black bg-white p-4 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] ${
        level > 0 ? 'ml-12' : ''
      }`}
    >
      {/* Header */}
      <div className="mb-3 flex items-start justify-between">
        <div className="flex items-center gap-3">
          <Avatar className="h-8 w-8 border-2 border-black">
            <AvatarFallback className="bg-purple-200 text-xs font-black">
              {comment.userId.slice(0, 2).toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <div>
            <p className="text-sm font-black">User {comment.userId.slice(0, 8)}</p>
            <p className="text-xs font-medium text-gray-600">
              {formatDistanceToNow(new Date(comment.createdAt), {
                addSuffix: true,
                locale: vi,
              })}
              {comment.edited && <span className="ml-1 text-gray-500">(đã chỉnh sửa)</span>}
            </p>
          </div>
        </div>

        {isOwner && !isEditing && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="noShadow"
                size="sm"
                className="h-8 w-8 border-2 border-black p-0 hover:bg-gray-100"
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
                onClick={() => onDelete?.(comment.id)}
                className="font-bold text-red-600"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Xóa
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>

      {/* Content */}
      {isEditing ? (
        <div className="space-y-2">
          <Textarea
            value={editContent}
            onChange={(e) => setEditContent(e.target.value)}
            className="min-h-[80px] border-2 border-black font-medium shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] focus-visible:ring-0"
            maxLength={5000}
          />
          <div className="flex justify-end gap-2">
            <Button
              size="sm"
              variant="neutral"
              onClick={handleCancelEdit}
              className="border-2 border-black font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            >
              <X className="mr-1 h-4 w-4" />
              Hủy
            </Button>
            <Button
              size="sm"
              onClick={handleSaveEdit}
              disabled={!editContent.trim()}
              className="border-2 border-black bg-green-400 font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:bg-green-500"
            >
              <Check className="mr-1 h-4 w-4" />
              Lưu
            </Button>
          </div>
        </div>
      ) : (
        <>
          <p className="whitespace-pre-wrap text-sm font-medium leading-relaxed">
            {comment.content}
          </p>

          {/* Reply Button */}
          {showReplyButton && level < 2 && onReply && (
            <button
              onClick={() => onReply(comment.id)}
              className="mt-3 flex items-center gap-1 text-xs font-black text-purple-600 hover:text-purple-700"
            >
              <Reply className="h-3 w-3" />
              Trả lời
            </button>
          )}
        </>
      )}
    </div>
  )
}
