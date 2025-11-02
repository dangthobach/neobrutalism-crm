'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Search } from 'lucide-react'
import { usePublishedContent, useTrendingContent } from '@/hooks/useContent'
import { useActiveCategories } from '@/hooks/useContentCategories'
import { usePopularTags } from '@/hooks/useContentTags'
import { ContentPreview } from '@/components/content/content-preview'
import { TagCloud } from '@/components/content/tag-cloud'
import { ContentType } from '@/types/content'

export default function BlogPage() {
  const router = useRouter()
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [contentType, setContentType] = useState<ContentType | ''>('')

  const { data: contentsData, isLoading } = usePublishedContent()
  const { data: trendingContent } = useTrendingContent(3)
  const { data: categories = [] } = useActiveCategories()
  const { data: tags = [] } = usePopularTags(20)

  const contents = contentsData?.content || []

  // Filter contents
  const filteredContents = contents.filter((content) => {
    if (keyword && !content.title.toLowerCase().includes(keyword.toLowerCase())) {
      return false
    }
    if (categoryId && !content.categories.some(c => c.id === categoryId)) {
      return false
    }
    if (contentType && content.contentType !== contentType) {
      return false
    }
    return true
  })

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Hero Section */}
      <div className="border-b-4 border-black bg-yellow-300 py-16">
        <div className="container mx-auto px-4">
          <h1 className="mb-4 text-6xl font-black">Blog</h1>
          <p className="text-xl font-bold">
            Discover articles, tutorials, and insights
          </p>
        </div>
      </div>

      <div className="container mx-auto px-4 py-12">
        <div className="grid gap-8 lg:grid-cols-3">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-8">
            {/* Filters */}
            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
                <h2 className="text-xl font-black">Search & Filter</h2>
              </div>
              <div className="grid gap-4 p-6 md:grid-cols-3">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    placeholder="Search..."
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    className="pl-10"
                  />
                </div>

                <Select value={categoryId} onValueChange={setCategoryId}>
                  <SelectTrigger>
                    <SelectValue placeholder="All Categories" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">All Categories</SelectItem>
                    {categories.map((cat) => (
                      <SelectItem key={cat.id} value={cat.id}>
                        {cat.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                <Select
                  value={contentType}
                  onValueChange={(value) => setContentType(value as ContentType | '')}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="All Types" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">All Types</SelectItem>
                    <SelectItem value={ContentType.BLOG}>Blog</SelectItem>
                    <SelectItem value={ContentType.ARTICLE}>Article</SelectItem>
                    <SelectItem value={ContentType.TUTORIAL}>Tutorial</SelectItem>
                    <SelectItem value={ContentType.GUIDE}>Guide</SelectItem>
                    <SelectItem value={ContentType.NEWS}>News</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </Card>

            {/* Content Grid */}
            {isLoading ? (
              <div className="flex h-64 items-center justify-center">
                <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
              </div>
            ) : filteredContents.length === 0 ? (
              <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <div className="flex h-64 items-center justify-center">
                  <p className="text-lg font-bold text-muted-foreground">
                    No content found. Try adjusting your filters.
                  </p>
                </div>
              </Card>
            ) : (
              <div className="space-y-6">
                {filteredContents.map((content) => (
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
            {/* Trending */}
            {trendingContent && trendingContent.length > 0 && (
              <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
                <div className="border-b-2 border-black bg-red-300 px-6 py-4">
                  <h2 className="text-xl font-black">🔥 Trending</h2>
                </div>
                <div className="divide-y-2 divide-black">
                  {trendingContent.map((content) => (
                    <div
                      key={content.id}
                      onClick={() => router.push(`/blog/${content.slug}`)}
                      className="cursor-pointer p-4 hover:bg-gray-50"
                    >
                      <h3 className="font-bold line-clamp-2">{content.title}</h3>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {content.viewCount || 0} views
                      </p>
                    </div>
                  ))}
                </div>
              </Card>
            )}

            {/* Categories */}
            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-green-200 px-6 py-4">
                <h2 className="text-xl font-black">Categories</h2>
              </div>
              <div className="p-4 space-y-2">
                {categories.map((cat) => (
                  <button
                    key={cat.id}
                    onClick={() => router.push(`/blog/category/${cat.slug}`)}
                    className="block w-full rounded border-2 border-black bg-white px-4 py-2 text-left font-bold hover:bg-yellow-200"
                  >
                    {cat.name}
                  </button>
                ))}
              </div>
            </Card>

            {/* Popular Tags */}
            {tags.length > 0 && (
              <TagCloud tags={tags} variant="public" maxTags={15} />
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
