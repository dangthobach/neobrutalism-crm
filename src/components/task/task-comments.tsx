/**
 * Task Comments Component
 * Display and manage task comments
 */

'use client'

import { useState } from 'react'
import { TaskComment } from '@/types/task'
import { formatDateTime } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { MessageSquare, Trash2 } from 'lucide-react'

interface TaskCommentsProps {
  taskId: string
  comments: TaskComment[]
  onAddComment?: (content: string) => void
  onDeleteComment?: (commentId: string) => void
  isSubmitting?: boolean
}

export function TaskComments({ taskId, comments, onAddComment, onDeleteComment, isSubmitting }: TaskCommentsProps) {
  const [newComment, setNewComment] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (newComment.trim() && onAddComment) {
      onAddComment(newComment.trim())
      setNewComment('')
    }
  }

  return (
    <div className="space-y-4">
      {/* Add Comment Form */}
      {onAddComment && (
        <form onSubmit={handleSubmit} className="rounded border-2 border-black bg-white p-4 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="flex items-start gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-full border-2 border-black bg-purple-200">
              <MessageSquare className="h-5 w-5" />
            </div>
            <div className="flex-1 space-y-2">
              <Textarea
                value={newComment}
                onChange={(e) => setNewComment(e.target.value)}
                placeholder="Add a comment..."
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
                rows={3}
              />
              <Button
                type="submit"
                disabled={!newComment.trim() || isSubmitting}
                className="border-2 border-black bg-purple-400 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-purple-500 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
              >
                {isSubmitting ? 'Adding...' : 'Add Comment'}
              </Button>
            </div>
          </div>
        </form>
      )}

      {/* Comments List */}
      <div className="space-y-3">
        {comments.length === 0 ? (
          <div className="rounded border-2 border-black bg-white p-8 text-center">
            <p className="font-bold uppercase text-gray-500">No comments yet</p>
          </div>
        ) : (
          comments.map((comment) => (
            <div
              key={comment.id}
              className="rounded border-2 border-black bg-white p-4 shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1">
                  <div className="mb-2 flex items-center gap-2">
                    <span className="font-black uppercase">{comment.createdByName || 'Unknown'}</span>
                    <span className="text-xs font-bold text-gray-500">
                      {formatDateTime(comment.createdAt)}
                    </span>
                  </div>
                  <p className="font-medium text-gray-700">{comment.content}</p>
                  {comment.attachments && comment.attachments.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {comment.attachments.map((attachment, index) => (
                        <span
                          key={index}
                          className="rounded border border-black bg-gray-100 px-2 py-1 text-xs font-bold"
                        >
                          📎 {attachment}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
                {onDeleteComment && (
                  <Button
                    variant="neutral"
                    size="sm"
                    onClick={() => onDeleteComment(comment.id)}
                    className="border-2 border-black p-2 shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
