/**
 * Comment List Component
 * Displays all comments for a task with real-time updates and threading
 */

'use client'

import { useState, useMemo } from 'react'
import { MessageSquare, Loader2 } from 'lucide-react'
import {
  useComments,
  useCreateComment,
  useUpdateComment,
  useDeleteComment,
  useCommentRealtime,
  Comment,
} from '@/hooks/use-comments'
import { CommentItem } from './comment-item'
import { AddComment } from './add-comment'
import { Skeleton } from '@/components/ui/skeleton'

interface CommentListProps {
  taskId: string
  currentUserId?: string
}

export function CommentList({ taskId, currentUserId }: CommentListProps) {
  const [replyingTo, setReplyingTo] = useState<string | null>(null)

  // Fetch comments and subscribe to real-time updates
  const { data: comments = [], isLoading } = useComments(taskId)
  useCommentRealtime(taskId)

  // Mutations
  const createMutation = useCreateComment(taskId)
  const updateMutation = useUpdateComment(taskId)
  const deleteMutation = useDeleteComment(taskId)

  // Organize comments into threads
  const { topLevelComments, repliesByParentId } = useMemo(() => {
    const topLevel: Comment[] = []
    const replies: Record<string, Comment[]> = {}

    comments.forEach((comment) => {
      if (!comment.parentId) {
        topLevel.push(comment)
      } else {
        if (!replies[comment.parentId]) {
          replies[comment.parentId] = []
        }
        replies[comment.parentId].push(comment)
      }
    })

    // Sort by creation date
    topLevel.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    Object.keys(replies).forEach((parentId) => {
      replies[parentId].sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
    })

    return { topLevelComments: topLevel, repliesByParentId: replies }
  }, [comments])

  const handleAddComment = (content: string) => {
    createMutation.mutate({ content })
  }

  const handleAddReply = (content: string, parentId: string) => {
    createMutation.mutate({ content, parentId }, {
      onSuccess: () => setReplyingTo(null),
    })
  }

  const handleEdit = (commentId: string, content: string) => {
    updateMutation.mutate({ commentId, request: { content } })
  }

  const handleDelete = (commentId: string) => {
    if (confirm('Bạn có chắc muốn xóa comment này?')) {
      deleteMutation.mutate(commentId)
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-32 w-full border-2 border-black" />
        <Skeleton className="h-24 w-full border-2 border-black" />
        <Skeleton className="h-24 w-full border-2 border-black" />
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <MessageSquare className="h-5 w-5" />
          <h3 className="text-lg font-black uppercase">
            Comments ({comments.length})
          </h3>
        </div>
        {(createMutation.isPending || updateMutation.isPending || deleteMutation.isPending) && (
          <Loader2 className="h-4 w-4 animate-spin text-purple-600" />
        )}
      </div>

      {/* Add Comment Form */}
      <AddComment
        onSubmit={handleAddComment}
        isSubmitting={createMutation.isPending}
      />

      {/* Comments List */}
      {comments.length === 0 ? (
        <div className="border-2 border-black bg-gray-50 p-8 text-center shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <MessageSquare className="mx-auto mb-2 h-12 w-12 text-gray-400" />
          <p className="font-bold text-gray-600">Chưa có comment nào</p>
          <p className="mt-1 text-sm text-gray-500">
            Hãy là người đầu tiên comment về task này
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {topLevelComments.map((comment) => (
            <div key={comment.id} className="space-y-3">
              {/* Top-level comment */}
              <CommentItem
                comment={comment}
                onEdit={handleEdit}
                onDelete={handleDelete}
                onReply={setReplyingTo}
                isOwner={comment.userId === currentUserId}
                level={0}
              />

              {/* Replies */}
              {repliesByParentId[comment.id]?.map((reply) => (
                <CommentItem
                  key={reply.id}
                  comment={reply}
                  onEdit={handleEdit}
                  onDelete={handleDelete}
                  isOwner={reply.userId === currentUserId}
                  level={1}
                  showReplyButton={false}
                />
              ))}

              {/* Reply Form */}
              {replyingTo === comment.id && (
                <AddComment
                  onSubmit={(content) => handleAddReply(content, comment.id)}
                  onCancel={() => setReplyingTo(null)}
                  placeholder="Viết câu trả lời..."
                  isReply
                  autoFocus
                  isSubmitting={createMutation.isPending}
                />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
