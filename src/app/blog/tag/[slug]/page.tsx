'use client'

import { use, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { ArrowLeft, Search, Hash } from 'lucide-react'
import { useTagBySlug, usePopularTags } from '@/hooks/useContentTags'
import { usePublishedContent } from '@/hooks/useContent'
import { ContentPreview } from '@/components/content/content-preview'

interface TagArchivePageProps {
  params: Promise<{ slug: string }>
}

export default function TagArchivePage({ params }: TagArchivePageProps) {
  const { slug } = use(params)
  const router = useRouter()
  const [keyword, setKeyword] = useState('')

  const { data: tag, isLoading: tagLoading } = useTagBySlug(slug)
  const { data: contentData, isLoading: contentLoading } = usePublishedContent()
  const { data: popularTagsData } = usePopularTags(10)

  if (tagLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
          <p className="mt-4 font-bold">Loading...</p>
        </div>
      </div>
    )
  }

  if (!tag) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <p className="text-2xl font-black">Tag not found</p>
          <Button onClick={() => router.push('/blog')} className="mt-4">
            Back to Blog
          </Button>
        </div>
      </div>
    )
  }

  // Filter content by tag
  const filteredContent = contentData?.content.filter((item) => {
    const matchesTag = item.tags?.some((t) => t.id === tag.id)
    const matchesKeyword = keyword === '' || 
      item.title.toLowerCase().includes(keyword.toLowerCase()) ||
      item.summary?.toLowerCase().includes(keyword.toLowerCase())
    return matchesTag && matchesKeyword
  }) || []

  // Get related tags (excluding current tag)
  const relatedTags = (popularTagsData || []).filter((t) => t.id !== tag.id).slice(0, 10)

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Hero Section */}
      <div className="border-b-4 border-black bg-pink-200">
        <div className="container mx-auto px-4 py-12">
          <Button
            variant="neutral"
            onClick={() => router.push('/blog')}
            className="mb-6 gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Blog
          </Button>
          <div className="flex items-center gap-4">
            <Hash className="h-12 w-12" />
            <div>
              <h1 className="text-5xl font-black">#{tag.name}</h1>
              {tag.description && (
                <p className="mt-2 text-lg">{tag.description}</p>
              )}
              <p className="mt-2 text-sm">
                {filteredContent.length} {filteredContent.length === 1 ? 'post' : 'posts'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-4 py-12">
        <div className="grid gap-8 lg:grid-cols-3">
          {/* Main Content */}
          <div className="space-y-8 lg:col-span-2">
            {/* Search */}
            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
                <h3 className="text-lg font-black">Search in this tag</h3>
              </div>
              <div className="p-6">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2" />
                  <Input
                    placeholder="Search posts..."
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    className="pl-10"
                  />
                </div>
              </div>
            </Card>

            {/* Content Grid */}
            {contentLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-center">
                  <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
                  <p className="mt-4 font-bold">Loading content...</p>
                </div>
              </div>
            ) : filteredContent.length === 0 ? (
              <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <div className="p-12 text-center">
                  <p className="text-lg font-bold text-muted-foreground">
                    No posts found with this tag
                  </p>
                </div>
              </Card>
            ) : (
              <div className="space-y-6">
                {filteredContent.map((content) => (
                  <div
                    key={content.id}
                    onClick={() => router.push(`/blog/${content.slug}`)}
                    className="cursor-pointer"
                  >
                    <ContentPreview content={content} variant="card" />
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Related Tags */}
            {relatedTags.length > 0 && (
              <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
                  <h3 className="text-lg font-black">Related Tags</h3>
                </div>
                <div className="flex flex-wrap gap-2 p-6">
                  {relatedTags.map((relatedTag) => (
                    <Button
                      key={relatedTag.id}
                      variant="neutral"
                      size="sm"
                      onClick={() => router.push(`/blog/tag/${relatedTag.slug}`)}
                    >
                      #{relatedTag.name}
                    </Button>
                  ))}
                </div>
              </Card>
            )}

            {/* Tag Info */}
            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-green-200 px-6 py-4">
                <h3 className="text-lg font-black">Tag Information</h3>
              </div>
              <div className="space-y-4 p-6">
                <div>
                  <p className="text-sm text-muted-foreground">Slug</p>
                  <p className="font-mono text-sm font-bold">{tag.slug}</p>
                </div>
                {tag.description && (
                  <div>
                    <p className="text-sm text-muted-foreground">Description</p>
                    <p className="text-sm">{tag.description}</p>
                  </div>
                )}
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}
