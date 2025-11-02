'use client'

import { use, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { ArrowLeft, Search, FolderOpen } from 'lucide-react'
import { useCategoryBySlug } from '@/hooks/useContentCategories'
import { usePublishedContent } from '@/hooks/useContent'
import { ContentPreview } from '@/components/content/content-preview'

interface CategoryArchivePageProps {
  params: Promise<{ slug: string }>
}

export default function CategoryArchivePage({ params }: CategoryArchivePageProps) {
  const { slug } = use(params)
  const router = useRouter()
  const [keyword, setKeyword] = useState('')

  const { data: category, isLoading: categoryLoading } = useCategoryBySlug(slug)
  const { data: contentData, isLoading: contentLoading } = usePublishedContent()

  if (categoryLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
          <p className="mt-4 font-bold">Loading...</p>
        </div>
      </div>
    )
  }

  if (!category) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <p className="text-2xl font-black">Category not found</p>
          <Button onClick={() => router.push('/blog')} className="mt-4">
            Back to Blog
          </Button>
        </div>
      </div>
    )
  }

  // Filter content by category
  const filteredContent = contentData?.content.filter((item) => {
    const matchesCategory = item.categories?.some((cat) => cat.id === category.id)
    const matchesKeyword = keyword === '' || 
      item.title.toLowerCase().includes(keyword.toLowerCase()) ||
      item.summary?.toLowerCase().includes(keyword.toLowerCase())
    return matchesCategory && matchesKeyword
  }) || []

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Hero Section */}
      <div className="border-b-4 border-black bg-green-200">
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
            <FolderOpen className="h-12 w-12" />
            <div>
              <h1 className="text-5xl font-black">{category.name}</h1>
              {category.description && (
                <p className="mt-2 text-lg">{category.description}</p>
              )}
              {category.parentName && (
                <p className="mt-2 text-sm">
                  Parent: <span className="font-bold">{category.parentName}</span>
                </p>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-4 py-12">
        <div className="space-y-8">
          {/* Search */}
          <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
              <h3 className="text-lg font-black">Search in this category</h3>
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
                  No posts found in this category
                </p>
              </div>
            </Card>
          ) : (
            <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-3">
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

          {/* Results Count */}
          {!contentLoading && filteredContent.length > 0 && (
            <p className="text-center text-sm text-muted-foreground">
              Showing {filteredContent.length} {filteredContent.length === 1 ? 'post' : 'posts'}
            </p>
          )}
        </div>
      </div>
    </div>
  )
}
