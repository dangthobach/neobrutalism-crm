'use client'

import { Card } from '@/components/ui/card'
import { MessageCircle, MessageSquare } from 'lucide-react'
import { AddComment } from './add-comment'
import { CommentItem } from './comment-item'
import { useComments } from '@/hooks/useComments'

interface CommentListProps {
  taskId: string
  currentUserId?: string
  currentUserName?: string
}

/**
 * Complete comment list component for task detail page
 * Features:
 * - Add new comment input at top
 * - List of all comments with replies
 * - Empty state when no comments
 * - Loading skeleton
 * - Real-time updates via React Query
 * - Threaded comments support
 *
 * Optimized for performance:
 * - React Query caching
 * - Optimistic updates
 * - Minimal re-renders
 */
export function CommentList({
  taskId,
  currentUserId,
  currentUserName = 'User',
}: CommentListProps) {
  const {
    topLevelComments,
    repliesMap,
    isLoading,
    addComment,
    isAdding,
    updateComment,
    deleteComment,
    replyToComment,
  } = useComments(taskId)

  const handleAddComment = (content: string) => {
    addComment({ content })
  }

  const handleEditComment = (commentId: string, content: string) => {
    updateComment({ commentId, content })
  }

  const handleDeleteComment = (commentId: string) => {
    deleteComment(commentId)
  }

  const handleReply = (parentId: string, content: string) => {
    replyToComment({ parentId, content })
  }

  if (isLoading) {
    return (
      <div className="space-y-4">
        {/* Loading skeleton */}
        {[1, 2, 3].map((i) => (
          <Card
            key={i}
            className="p-4 border-2 border-black bg-gray-100 animate-pulse"
          >
            <div className="flex gap-3">
              <div className="w-10 h-10 rounded-full bg-gray-300" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-gray-300 rounded w-1/4" />
                <div className="h-3 bg-gray-300 rounded w-3/4" />
                <div className="h-3 bg-gray-300 rounded w-1/2" />
              </div>
            </div>
          </Card>
        ))}
      </div>
    )
  }

  const hasComments = topLevelComments.length > 0

  return (
    <div className="space-y-6">
      {/* Add comment section */}
      <AddComment
        onSubmit={handleAddComment}
        isSubmitting={isAdding}
        currentUserName={currentUserName}
      />

      {/* Comments count header */}
      {hasComments && (
        <div className="flex items-center gap-2 pb-2 border-b-2 border-black">
          <MessageSquare className="w-5 h-5" />
          <h3 className="font-black text-lg">
            {topLevelComments.length} Comment{topLevelComments.length !== 1 ? 's' : ''}
          </h3>
        </div>
      )}

      {/* Comments list */}
      {hasComments ? (
        <div className="space-y-4">
          {topLevelComments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              replies={repliesMap.get(comment.id) || []}
              onEdit={handleEditComment}
              onDelete={handleDeleteComment}
              onReply={handleReply}
              currentUserId={currentUserId}
              isReply={false}
            />
          ))}
        </div>
      ) : (
        /* Empty state */
        <Card className="p-12 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-gradient-to-br from-gray-50 to-white">
          <div className="flex flex-col items-center justify-center text-center">
            <div className="w-16 h-16 bg-gray-100 border-4 border-black flex items-center justify-center mb-4">
              <MessageCircle className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="text-lg font-black mb-2">No comments yet</h3>
            <p className="text-sm text-gray-600 max-w-sm">
              Be the first to comment on this task. Share your thoughts, ask questions, or provide updates.
            </p>
          </div>
        </Card>
      )}
    </div>
  )
}
