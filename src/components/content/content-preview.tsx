/**
 * Content Preview Component
 * Preview content card with Neobrutalism styling
 */

'use client'

import { Card } from '@/components/ui/card'
import { Content } from '@/types/content'
import { ContentStatusBadge } from './content-status-badge'
import { Calendar, Eye, User } from 'lucide-react'

interface ContentPreviewProps {
  content: Content
  variant?: 'full' | 'card'
}

export function ContentPreview({ content, variant = 'card' }: ContentPreviewProps) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    })
  }

  if (variant === 'card') {
    return (
      <Card className="group overflow-hidden border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] transition-all hover:shadow-[6px_6px_0px_0px_rgba(0,0,0,1)]">
        {content.featuredImageUrl && (
          <div className="relative h-48 overflow-hidden border-b-2 border-black">
            <img
              src={content.featuredImageUrl}
              alt={content.title}
              className="h-full w-full object-cover transition-transform group-hover:scale-105"
            />
          </div>
        )}
        
        <div className="p-6">
          <div className="mb-3 flex items-center gap-2">
            <span className="rounded border-2 border-black bg-blue-200 px-2 py-1 text-xs font-bold">
              {content.contentType}
            </span>
            <ContentStatusBadge status={content.status} />
            {content.seriesName && (
              <span className="rounded border-2 border-black bg-purple-200 px-2 py-1 text-xs font-bold">
                {content.seriesName}
              </span>
            )}
          </div>

          <h3 className="mb-2 text-2xl font-black line-clamp-2 group-hover:text-blue-600">
            {content.title}
          </h3>

          {content.summary && (
            <p className="mb-4 text-sm text-muted-foreground line-clamp-3">
              {content.summary}
            </p>
          )}

          <div className="flex flex-wrap gap-2 mb-4">
            {content.categories.map((category) => (
              <span
                key={category.id}
                className="rounded-full border-2 border-black bg-yellow-200 px-3 py-1 text-xs font-bold"
              >
                {category.name}
              </span>
            ))}
          </div>

          <div className="flex items-center gap-4 text-xs text-muted-foreground">
            <div className="flex items-center gap-1">
              <User className="h-3 w-3" />
              <span>{content.authorName}</span>
            </div>
            <div className="flex items-center gap-1">
              <Calendar className="h-3 w-3" />
              <span>{formatDate(content.createdAt)}</span>
            </div>
            <div className="flex items-center gap-1">
              <Eye className="h-3 w-3" />
              <span>{content.viewCount || 0} views</span>
            </div>
          </div>

          {content.tags.length > 0 && (
            <div className="mt-4 flex flex-wrap gap-2 border-t-2 border-black pt-4">
              {content.tags.slice(0, 3).map((tag) => (
                <span
                  key={tag.id}
                  className="rounded-full border border-black bg-white px-2 py-0.5 text-xs"
                >
                  #{tag.name}
                </span>
              ))}
              {content.tags.length > 3 && (
                <span className="rounded-full border border-black bg-white px-2 py-0.5 text-xs">
                  +{content.tags.length - 3} more
                </span>
              )}
            </div>
          )}
        </div>
      </Card>
    )
  }

  // Full preview
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="space-y-4">
        <div className="flex flex-wrap gap-2">
          <span className="rounded border-2 border-black bg-blue-200 px-3 py-1 text-sm font-bold">
            {content.contentType}
          </span>
          <ContentStatusBadge status={content.status} />
          {content.seriesName && (
            <span className="rounded border-2 border-black bg-purple-200 px-3 py-1 text-sm font-bold">
              📚 {content.seriesName} #{content.seriesOrder}
            </span>
          )}
        </div>

        <h1 className="text-5xl font-black leading-tight">{content.title}</h1>

        {content.summary && (
          <p className="text-xl text-muted-foreground">{content.summary}</p>
        )}

        <div className="flex flex-wrap items-center gap-6 text-sm">
          <div className="flex items-center gap-2">
            <User className="h-4 w-4" />
            <span className="font-bold">{content.authorName}</span>
          </div>
          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4" />
            <span>{formatDate(content.publishedAt || content.createdAt)}</span>
          </div>
          <div className="flex items-center gap-2">
            <Eye className="h-4 w-4" />
            <span>{content.viewCount || 0} views</span>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          {content.categories.map((category) => (
            <span
              key={category.id}
              className="rounded-full border-2 border-black bg-yellow-200 px-4 py-2 text-sm font-bold"
            >
              {category.name}
            </span>
          ))}
        </div>
      </div>

      {/* Featured Image */}
      {content.featuredImageUrl && (
        <div className="overflow-hidden rounded-lg border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <img
            src={content.featuredImageUrl}
            alt={content.title}
            className="w-full"
          />
        </div>
      )}

      {/* Content Body */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="prose max-w-none p-8">
          {/* Render markdown/HTML content */}
          <div dangerouslySetInnerHTML={{ __html: content.body }} />
        </div>
      </Card>

      {/* Tags */}
      {content.tags.length > 0 && (
        <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="border-b-2 border-black bg-green-200 px-6 py-4">
            <h3 className="text-lg font-black">Tags</h3>
          </div>
          <div className="flex flex-wrap gap-2 p-6">
            {content.tags.map((tag) => (
              <span
                key={tag.id}
                className="rounded-full border-2 border-black bg-white px-4 py-2 text-sm font-bold hover:bg-gray-100"
              >
                #{tag.name}
              </span>
            ))}
          </div>
        </Card>
      )}

      {/* SEO Info (Admin view only) */}
      {(content.seoTitle || content.seoDescription) && (
        <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
            <h3 className="text-lg font-black">SEO Information</h3>
          </div>
          <div className="space-y-3 p-6 text-sm">
            {content.seoTitle && (
              <div>
                <span className="font-bold">Title:</span> {content.seoTitle}
              </div>
            )}
            {content.seoDescription && (
              <div>
                <span className="font-bold">Description:</span> {content.seoDescription}
              </div>
            )}
            {content.seoKeywords && (
              <div>
                <span className="font-bold">Keywords:</span> {content.seoKeywords}
              </div>
            )}
          </div>
        </Card>
      )}
    </div>
  )
}
