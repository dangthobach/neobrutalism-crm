/**
 * Add Comment Component
 * Input form for adding new comments or replies
 */

'use client'

import { useState } from 'react'
import { Send, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'

interface AddCommentProps {
  onSubmit: (content: string) => void
  onCancel?: () => void
  placeholder?: string
  isReply?: boolean
  autoFocus?: boolean
  isSubmitting?: boolean
}

export function AddComment({
  onSubmit,
  onCancel,
  placeholder = 'Viết comment...',
  isReply = false,
  autoFocus = false,
  isSubmitting = false,
}: AddCommentProps) {
  const [content, setContent] = useState('')

  const handleSubmit = () => {
    if (content.trim()) {
      onSubmit(content.trim())
      setContent('')
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      e.preventDefault()
      handleSubmit()
    }
  }

  return (
    <div
      className={`border-2 border-black bg-white p-4 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] ${
        isReply ? 'ml-12' : ''
      }`}
    >
      <Textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        autoFocus={autoFocus}
        className="min-h-[100px] border-2 border-black font-medium shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] focus-visible:ring-0"
        maxLength={5000}
        disabled={isSubmitting}
      />
      
      <div className="mt-3 flex items-center justify-between">
        <p className="text-xs font-medium text-gray-600">
          {content.length}/5000 ký tự
          {content.length > 0 && (
            <span className="ml-2 text-gray-500">
              • Ctrl+Enter để gửi
            </span>
          )}
        </p>
        
        <div className="flex gap-2">
          {isReply && onCancel && (
            <Button
              size="sm"
              variant="outline"
              onClick={onCancel}
              disabled={isSubmitting}
              className="border-2 border-black font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            >
              <X className="mr-1 h-4 w-4" />
              Hủy
            </Button>
          )}
          
          <Button
            size="sm"
            onClick={handleSubmit}
            disabled={!content.trim() || isSubmitting}
            className="border-2 border-black bg-purple-400 font-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:bg-purple-500"
          >
            <Send className="mr-1 h-4 w-4" />
            {isSubmitting ? 'Đang gửi...' : 'Gửi'}
          </Button>
        </div>
      </div>
    </div>
  )
}
