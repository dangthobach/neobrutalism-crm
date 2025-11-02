'use client'

import { use, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { ArrowLeft, Share2, Facebook, Twitter, Linkedin } from 'lucide-react'
import {
  useContentBySlug,
  useIncrementContentViews,
  useContentBySeries,
} from '@/hooks/useContent'
import { ContentPreview } from '@/components/content/content-preview'

interface BlogPostPageProps {
  params: Promise<{ slug: string }>
}

export default function BlogPostPage({ params }: BlogPostPageProps) {
  const { slug } = use(params)
  const router = useRouter()
  
  const { data: content, isLoading } = useContentBySlug(slug)
  const incrementViews = useIncrementContentViews()
  const { data: seriesContent } = useContentBySeries(
    content?.seriesId || '',
    !!content?.seriesId
  )

  // Increment view count on mount
  useEffect(() => {
    if (content?.id) {
      incrementViews.mutate(content.id)
    }
  }, [content?.id, incrementViews])

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
          <p className="mt-4 font-bold">Loading...</p>
        </div>
      </div>
    )
  }

  if (!content) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <p className="text-2xl font-black">Post not found</p>
          <Button onClick={() => router.push('/blog')} className="mt-4">
            Back to Blog
          </Button>
        </div>
      </div>
    )
  }

  const shareUrl = typeof window !== 'undefined' ? window.location.href : ''

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="border-b-2 border-black bg-white">
        <div className="container mx-auto px-4 py-6">
          <Button
            variant="neutral"
            onClick={() => router.push('/blog')}
            className="gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Blog
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-4 py-12">
        <div className="mx-auto max-w-4xl space-y-8">
          {/* Main Content */}
          <ContentPreview content={content} variant="full" />

          {/* Share Buttons */}
          <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
              <h3 className="text-lg font-black">Share this post</h3>
            </div>
            <div className="flex flex-wrap gap-3 p-6">
              <Button
                variant="neutral"
                onClick={() => {
                  window.open(
                    `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(shareUrl)}`,
                    '_blank'
                  )
                }}
              >
                <Facebook className="mr-2 h-4 w-4" />
                Facebook
              </Button>
              <Button
                variant="neutral"
                onClick={() => {
                  window.open(
                    `https://twitter.com/intent/tweet?url=${encodeURIComponent(shareUrl)}&text=${encodeURIComponent(content.title)}`,
                    '_blank'
                  )
                }}
              >
                <Twitter className="mr-2 h-4 w-4" />
                Twitter
              </Button>
              <Button
                variant="neutral"
                onClick={() => {
                  window.open(
                    `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(shareUrl)}`,
                    '_blank'
                  )
                }}
              >
                <Linkedin className="mr-2 h-4 w-4" />
                LinkedIn
              </Button>
              <Button
                variant="neutral"
                onClick={() => {
                  navigator.clipboard.writeText(shareUrl)
                  alert('Link copied to clipboard!')
                }}
              >
                <Share2 className="mr-2 h-4 w-4" />
                Copy Link
              </Button>
            </div>
          </Card>

          {/* Series Navigation */}
          {content.seriesName && seriesContent?.content && seriesContent.content.length > 1 && (
            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
                <h3 className="text-lg font-black">
                  📚 More from &quot;{content.seriesName}&quot;
                </h3>
              </div>
              <div className="divide-y-2 divide-black">
                {seriesContent.content
                  .sort((a, b) => (a.seriesOrder || 0) - (b.seriesOrder || 0))
                  .map((item) => (
                    <div
                      key={item.id}
                      onClick={() => router.push(`/blog/${item.slug}`)}
                      className={`cursor-pointer p-4 ${
                        item.id === content.id
                          ? 'bg-yellow-200'
                          : 'hover:bg-gray-50'
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <span className="rounded-full border-2 border-black bg-white px-3 py-1 text-sm font-black">
                          {item.seriesOrder}
                        </span>
                        <div className="flex-1">
                          <h4 className="font-bold">{item.title}</h4>
                          {item.summary && (
                            <p className="mt-1 text-sm text-muted-foreground line-clamp-2">
                              {item.summary}
                            </p>
                          )}
                        </div>
                        {item.id === content.id && (
                          <span className="rounded border-2 border-black bg-yellow-300 px-2 py-1 text-xs font-black">
                            CURRENT
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
              </div>
            </Card>
          )}

          {/* Author Info */}
          <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="border-b-2 border-black bg-green-200 px-6 py-4">
              <h3 className="text-lg font-black">About the Author</h3>
            </div>
            <div className="p-6">
              <p className="text-lg font-bold">{content.authorName}</p>
              <p className="mt-2 text-sm text-muted-foreground">
                Published on {new Date(content.publishedAt || content.createdAt).toLocaleDateString('en-US', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </p>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
