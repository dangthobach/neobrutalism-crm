'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Card } from '@/components/ui/card'
import { MessageCircle, Send } from 'lucide-react'

interface AddCommentProps {
  onSubmit: (content: string) => void
  isSubmitting?: boolean
  placeholder?: string
  currentUserName?: string
}

/**
 * Component for adding new comments
 * Features:
 * - Textarea with character counter
 * - Submit button with loading state
 * - User avatar display
 * - Auto-resize textarea
 * - Neobrutalism design
 */
export function AddComment({
  onSubmit,
  isSubmitting = false,
  placeholder = 'Write a comment...',
  currentUserName = 'User',
}: AddCommentProps) {
  const [content, setContent] = useState('')
  const maxLength = 2000
  const remainingChars = maxLength - content.length

  const handleSubmit = () => {
    if (content.trim()) {
      onSubmit(content)
      setContent('')
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    // Ctrl+Enter or Cmd+Enter to submit
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      handleSubmit()
    }
  }

  return (
    <Card className="p-4 border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white">
      <div className="space-y-3">
        {/* Header with avatar */}
        <div className="flex items-center gap-3">
          {/* Avatar */}
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center text-white font-black text-sm border-2 border-black">
            {currentUserName.charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="font-bold text-sm">{currentUserName}</p>
            <p className="text-xs text-gray-500">Add a comment</p>
          </div>
        </div>

        {/* Textarea */}
        <Textarea
          value={content}
          onChange={(e) => setContent(e.target.value.slice(0, maxLength))}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          className="min-h-[100px] border-2 border-black resize-none focus-visible:ring-0 focus-visible:ring-offset-0 focus:border-blue-500 transition-colors"
          disabled={isSubmitting}
        />

        {/* Footer with character counter and submit button */}
        <div className="flex items-center justify-between">
          <div className="text-xs text-gray-500">
            <span className={remainingChars < 100 ? 'text-orange-600 font-medium' : ''}>
              {remainingChars} characters remaining
            </span>
            <span className="ml-3 text-gray-400">Ctrl+Enter to submit</span>
          </div>
          <Button
            onClick={handleSubmit}
            disabled={!content.trim() || isSubmitting}
            className="bg-blue-500 hover:bg-blue-600 text-white font-bold border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:shadow-none hover:translate-x-1 hover:translate-y-1 transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] disabled:hover:translate-x-0 disabled:hover:translate-y-0"
          >
            {isSubmitting ? (
              <>
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2" />
                Posting...
              </>
            ) : (
              <>
                <Send className="w-4 h-4 mr-2" />
                Post Comment
              </>
            )}
          </Button>
        </div>
      </div>
    </Card>
  )
}
