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
import { Plus, Search, FileText, Eye, Archive, Trash2 } from 'lucide-react'
import { ContentTable } from '@/components/content/content-table'
import { useContents, useContentStats } from '@/hooks/useContent'
import { ContentType, ContentStatus } from '@/types/content'

export default function ContentListPage() {
  const router = useRouter()
  const [page, setPage] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<ContentStatus | ''>('')
  const [contentType, setContentType] = useState<ContentType | ''>('')

  const { data: contentsData, isLoading } = useContents({
    page,
    size: 20,
    keyword: keyword || undefined,
    status: status || undefined,
    contentType: contentType || undefined,
  })

  const { data: stats } = useContentStats()

  const contents = contentsData?.content || []
  const totalPages = contentsData?.totalPages || 0

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-4xl font-black">Content Management</h1>
          <p className="text-muted-foreground">
            Manage your blog posts, articles, and pages
          </p>
        </div>
        <Button size="lg" onClick={() => router.push('/admin/content/new')}>
          <Plus className="mr-2 h-4 w-4" />
          New Content
        </Button>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid gap-4 md:grid-cols-4">
          <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="border-b-2 border-black bg-blue-200 p-4">
              <div className="flex items-center gap-2">
                <FileText className="h-5 w-5" />
                <h3 className="font-black">Total Content</h3>
              </div>
            </div>
            <div className="p-6">
              <p className="text-4xl font-black">{stats.totalContent}</p>
            </div>
          </Card>

          <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="border-b-2 border-black bg-green-200 p-4">
              <div className="flex items-center gap-2">
                <Eye className="h-5 w-5" />
                <h3 className="font-black">Published</h3>
              </div>
            </div>
            <div className="p-6">
              <p className="text-4xl font-black">
                {stats.byStatus[ContentStatus.PUBLISHED] || 0}
              </p>
              <p className="text-sm text-muted-foreground">
                {stats.totalViews} total views
              </p>
            </div>
          </Card>

          <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="border-b-2 border-black bg-yellow-200 p-4">
              <div className="flex items-center gap-2">
                <FileText className="h-5 w-5" />
                <h3 className="font-black">Drafts</h3>
              </div>
            </div>
            <div className="p-6">
              <p className="text-4xl font-black">
                {stats.byStatus[ContentStatus.DRAFT] || 0}
              </p>
            </div>
          </Card>

          <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="border-b-2 border-black bg-purple-200 p-4">
              <div className="flex items-center gap-2">
                <Archive className="h-5 w-5" />
                <h3 className="font-black">Archived</h3>
              </div>
            </div>
            <div className="p-6">
              <p className="text-4xl font-black">
                {stats.byStatus[ContentStatus.ARCHIVED] || 0}
              </p>
            </div>
          </Card>
        </div>
      )}

      {/* Filters */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
          <h2 className="text-xl font-black">Filters</h2>
        </div>
        <div className="grid gap-4 p-6 md:grid-cols-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search content..."
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value)
                setPage(0)
              }}
              className="pl-10"
            />
          </div>

          <Select
            value={status}
            onValueChange={(value) => {
              setStatus(value as ContentStatus | '')
              setPage(0)
            }}
          >
            <SelectTrigger>
              <SelectValue placeholder="All Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">All Status</SelectItem>
              <SelectItem value={ContentStatus.DRAFT}>Draft</SelectItem>
              <SelectItem value={ContentStatus.REVIEW}>In Review</SelectItem>
              <SelectItem value={ContentStatus.PUBLISHED}>Published</SelectItem>
              <SelectItem value={ContentStatus.ARCHIVED}>Archived</SelectItem>
            </SelectContent>
          </Select>

          <Select
            value={contentType}
            onValueChange={(value) => {
              setContentType(value as ContentType | '')
              setPage(0)
            }}
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
              <SelectItem value={ContentType.PAGE}>Page</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </Card>

      {/* Content Table */}
      <ContentTable contents={contents} isLoading={isLoading} />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <Button
            variant="neutral"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            Previous
          </Button>
          <span className="rounded border-2 border-black bg-white px-4 py-2 font-bold">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="neutral"
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  )
}
