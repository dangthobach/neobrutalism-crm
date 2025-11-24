'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { MessageCircle, Edit2, Trash2, Reply, Check, X } from 'lucide-react'
import { formatDistanceToNow } from 'date-fns'
import type { Comment } from '@/hooks/useComments'

interface CommentItemProps {
  comment: Comment
  replies?: Comment[]
  onEdit: (commentId: string, content: string) => void
  onDelete: (commentId: string) => void
  onReply: (parentId: string, content: string) => void
  currentUserId?: string
  isReply?: boolean
}

/**
 * Individual comment component with edit/delete/reply actions
 * Supports threaded replies
 */
export function CommentItem({
  comment,
  replies = [],
  onEdit,
  onDelete,
  onReply,
  currentUserId,
  isReply = false,
}: CommentItemProps) {
  const [isEditing, setIsEditing] = useState(false)
  const [isReplying, setIsReplying] = useState(false)
  const [editContent, setEditContent] = useState(comment.content)
  const [replyContent, setReplyContent] = useState('')

  const isAuthor = currentUserId === comment.userId
  const timeAgo = formatDistanceToNow(new Date(comment.createdAt), { addSuffix: true })

  const handleSaveEdit = () => {
    if (editContent.trim() && editContent !== comment.content) {
      onEdit(comment.id, editContent)
    }
    setIsEditing(false)
  }

  const handleCancelEdit = () => {
    setEditContent(comment.content)
    setIsEditing(false)
  }

  const handleSaveReply = () => {
    if (replyContent.trim()) {
      onReply(comment.id, replyContent)
      setReplyContent('')
      setIsReplying(false)
    }
  }

  const handleDelete = () => {
    if (confirm('Are you sure you want to delete this comment?')) {
      onDelete(comment.id)
    }
  }

  return (
    <div className={`space-y-3 ${isReply ? 'ml-12' : ''}`}>
      <div className="bg-white border-2 border-black p-4 rounded-none shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
        {/* Header */}
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-3">
            {/* Avatar */}
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-white font-black text-sm border-2 border-black">
              {comment.authorName?.charAt(0)?.toUpperCase() || 'U'}
            </div>

            {/* Author info */}
            <div>
              <p className="font-bold text-sm">{comment.authorName || 'Unknown User'}</p>
              <div className="flex items-center gap-2 text-xs text-gray-500">
                <span>{timeAgo}</span>
                {comment.edited && <span className="text-orange-600">• edited</span>}
              </div>
            </div>
          </div>

          {/* Actions for comment author */}
          {isAuthor && !isEditing && (
            <div className="flex gap-1">
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setIsEditing(true)}
                className="h-8 w-8 p-0"
              >
                <Edit2 className="w-3 h-3" />
              </Button>
              <Button
                size="sm"
                variant="ghost"
                onClick={handleDelete}
                className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
              >
                <Trash2 className="w-3 h-3" />
              </Button>
            </div>
          )}
        </div>

        {/* Content */}
        {isEditing ? (
          <div className="space-y-2">
            <Textarea
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
              className="min-h-[80px] border-2 border-black resize-none"
              autoFocus
            />
            <div className="flex gap-2">
              <Button
                size="sm"
                onClick={handleSaveEdit}
                className="bg-green-500 hover:bg-green-600 border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              >
                <Check className="w-4 h-4 mr-1" />
                Save
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={handleCancelEdit}
                className="border-2 border-black"
              >
                <X className="w-4 h-4 mr-1" />
                Cancel
              </Button>
            </div>
          </div>
        ) : (
          <div className="text-sm text-gray-700 whitespace-pre-wrap mb-3">
            {comment.content}
          </div>
        )}

        {/* Reply button */}
        {!isEditing && !isReply && (
          <Button
            size="sm"
            variant="ghost"
            onClick={() => setIsReplying(!isReplying)}
            className="text-blue-600 hover:text-blue-700 hover:bg-blue-50 h-7 px-2"
          >
            <Reply className="w-3 h-3 mr-1" />
            Reply
          </Button>
        )}

        {/* Reply input */}
        {isReplying && (
          <div className="mt-3 space-y-2 border-t-2 border-gray-200 pt-3">
            <Textarea
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
              placeholder="Write a reply..."
              className="min-h-[60px] border-2 border-black resize-none text-sm"
              autoFocus
            />
            <div className="flex gap-2">
              <Button
                size="sm"
                onClick={handleSaveReply}
                disabled={!replyContent.trim()}
                className="bg-blue-500 hover:bg-blue-600 border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] h-7"
              >
                <MessageCircle className="w-3 h-3 mr-1" />
                Reply
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  setIsReplying(false)
                  setReplyContent('')
                }}
                className="border-2 border-black h-7"
              >
                Cancel
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Nested replies */}
      {replies.length > 0 && (
        <div className="space-y-3">
          {replies.map((reply) => (
            <CommentItem
              key={reply.id}
              comment={reply}
              replies={[]}
              onEdit={onEdit}
              onDelete={onDelete}
              onReply={onReply}
              currentUserId={currentUserId}
              isReply={true}
            />
          ))}
        </div>
      )}
    </div>
  )
}
