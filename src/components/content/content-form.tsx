/**
 * Content Form Component
 * Form for creating/editing content with Neobrutalism styling
 */

'use client'

import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { UseFormReturn } from 'react-hook-form'
import {
  ContentType,
  ContentStatus,
  MemberTier,
  CreateContentRequest,
  UpdateContentRequest,
} from '@/types/content'
import { useActiveCategories } from '@/hooks/useContentCategories'
import { useTags } from '@/hooks/useContentTags'
import { useActiveSeries } from '@/hooks/useContentSeries'

interface ContentFormProps {
  form: UseFormReturn<CreateContentRequest | UpdateContentRequest>
  isEditing?: boolean
}

export function ContentForm({ form, isEditing = false }: ContentFormProps) {
  const { register, setValue, watch, formState: { errors } } = form
  
  const { data: categoriesData } = useActiveCategories()
  const { data: tagsData } = useTags()
  const { data: seriesData } = useActiveSeries()

  const categories = categoriesData || []
  const tags = tagsData?.content || []
  const series = seriesData || []

  const generateSlug = (title: string) => {
    return title
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '')
  }

  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const title = e.target.value
    if (!isEditing) {
      setValue('slug', generateSlug(title))
    }
  }

  return (
    <div className="space-y-6">
      {/* Basic Information */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
          <h2 className="text-xl font-black">Basic Information</h2>
        </div>
        <div className="space-y-4 p-6">
          <div className="space-y-2">
            <Label htmlFor="title">
              Title <span className="text-red-500">*</span>
            </Label>
            <Input
              id="title"
              {...register('title', { 
                required: 'Title is required',
                onChange: handleTitleChange,
              })}
              placeholder="Enter content title..."
              className={errors.title ? 'border-red-500' : ''}
            />
            {errors.title && (
              <p className="text-sm text-red-500">{errors.title.message}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="slug">
              Slug <span className="text-red-500">*</span>
            </Label>
            <Input
              id="slug"
              {...register('slug', { required: 'Slug is required' })}
              placeholder="content-url-slug"
              className={errors.slug ? 'border-red-500' : ''}
            />
            {errors.slug && (
              <p className="text-sm text-red-500">{errors.slug.message}</p>
            )}
            <p className="text-xs text-muted-foreground">
              URL-friendly version of the title
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="summary">Summary</Label>
            <Input
              id="summary"
              {...register('summary')}
              placeholder="Brief description..."
            />
            <p className="text-xs text-muted-foreground">
              Short summary for previews and cards
            </p>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="contentType">
                Content Type <span className="text-red-500">*</span>
              </Label>
              <Select
                value={watch('contentType')}
                onValueChange={(value) => setValue('contentType', value as ContentType)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select type..." />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ContentType.BLOG}>Blog Post</SelectItem>
                  <SelectItem value={ContentType.ARTICLE}>Article</SelectItem>
                  <SelectItem value={ContentType.TUTORIAL}>Tutorial</SelectItem>
                  <SelectItem value={ContentType.GUIDE}>Guide</SelectItem>
                  <SelectItem value={ContentType.NEWS}>News</SelectItem>
                  <SelectItem value={ContentType.PAGE}>Page</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="status">
                Status <span className="text-red-500">*</span>
              </Label>
              <Select
                value={watch('status')}
                onValueChange={(value) => setValue('status', value as ContentStatus)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select status..." />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={ContentStatus.DRAFT}>Draft</SelectItem>
                  <SelectItem value={ContentStatus.REVIEW}>In Review</SelectItem>
                  <SelectItem value={ContentStatus.PUBLISHED}>Published</SelectItem>
                  <SelectItem value={ContentStatus.ARCHIVED}>Archived</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>
      </Card>

      {/* Organization */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-green-200 px-6 py-4">
          <h2 className="text-xl font-black">Organization</h2>
        </div>
        <div className="space-y-4 p-6">
          <div className="space-y-2">
            <Label htmlFor="categoryIds">Categories</Label>
            <Select
              value={(watch('categoryIds') as string[] || [])[0] || ''}
              onValueChange={(value) => setValue('categoryIds', [value])}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select category..." />
              </SelectTrigger>
              <SelectContent>
                {categories.map((category) => (
                  <SelectItem key={category.id} value={category.id}>
                    {category.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="tagIds">Tags</Label>
            <div className="flex flex-wrap gap-2">
              {tags.slice(0, 10).map((tag) => (
                <button
                  key={tag.id}
                  type="button"
                  onClick={() => {
                    const currentTags = (watch('tagIds') as string[] || [])
                    const newTags = currentTags.includes(tag.id)
                      ? currentTags.filter(id => id !== tag.id)
                      : [...currentTags, tag.id]
                    setValue('tagIds', newTags)
                  }}
                  className={`rounded-full border-2 border-black px-3 py-1 text-xs font-bold transition-colors ${
                    (watch('tagIds') as string[] || []).includes(tag.id)
                      ? 'bg-yellow-300'
                      : 'bg-white hover:bg-gray-100'
                  }`}
                >
                  {tag.name}
                </button>
              ))}
            </div>
            <p className="text-xs text-muted-foreground">
              Click tags to select/deselect
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="seriesId">Series (Optional)</Label>
            <Select
              value={watch('seriesId') || ''}
              onValueChange={(value) => setValue('seriesId', value || undefined)}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select series..." />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">None</SelectItem>
                {series.map((s) => (
                  <SelectItem key={s.id} value={s.id}>
                    {s.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {watch('seriesId') && (
            <div className="space-y-2">
              <Label htmlFor="seriesOrder">Order in Series</Label>
              <Input
                id="seriesOrder"
                type="number"
                {...register('seriesOrder', { valueAsNumber: true })}
                placeholder="1"
                min="1"
              />
            </div>
          )}
        </div>
      </Card>

      {/* Access Control */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
          <h2 className="text-xl font-black">Access Control</h2>
        </div>
        <div className="space-y-4 p-6">
          <div className="space-y-2">
            <Label htmlFor="tierRequired">
              Required Member Tier <span className="text-red-500">*</span>
            </Label>
            <Select
              value={watch('tierRequired')}
              onValueChange={(value) => setValue('tierRequired', value as MemberTier)}
            >
              <SelectTrigger>
                <SelectValue placeholder="Select tier..." />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={MemberTier.FREE}>
                  <div className="flex items-center gap-2">
                    <span className="h-3 w-3 rounded-full bg-gray-400" />
                    Free
                  </div>
                </SelectItem>
                <SelectItem value={MemberTier.BASIC}>
                  <div className="flex items-center gap-2">
                    <span className="h-3 w-3 rounded-full bg-blue-400" />
                    Basic
                  </div>
                </SelectItem>
                <SelectItem value={MemberTier.PREMIUM}>
                  <div className="flex items-center gap-2">
                    <span className="h-3 w-3 rounded-full bg-yellow-400" />
                    Premium
                  </div>
                </SelectItem>
                <SelectItem value={MemberTier.ENTERPRISE}>
                  <div className="flex items-center gap-2">
                    <span className="h-3 w-3 rounded-full bg-purple-400" />
                    Enterprise
                  </div>
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="publishedAt">Schedule Publishing (Optional)</Label>
            <Input
              id="publishedAt"
              type="datetime-local"
              {...register('publishedAt')}
            />
            <p className="text-xs text-muted-foreground">
              Leave empty to publish immediately when status is set to Published
            </p>
          </div>
        </div>
      </Card>

      {/* SEO Settings */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
          <h2 className="text-xl font-black">SEO Settings</h2>
        </div>
        <div className="space-y-4 p-6">
          <div className="space-y-2">
            <Label htmlFor="seoTitle">SEO Title</Label>
            <Input
              id="seoTitle"
              {...register('seoTitle')}
              placeholder="Optimized title for search engines..."
              maxLength={60}
            />
            <p className="text-xs text-muted-foreground">
              {(watch('seoTitle')?.length || 0)}/60 characters
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="seoDescription">SEO Description</Label>
            <Input
              id="seoDescription"
              {...register('seoDescription')}
              placeholder="Brief description for search results..."
              maxLength={160}
            />
            <p className="text-xs text-muted-foreground">
              {(watch('seoDescription')?.length || 0)}/160 characters
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="seoKeywords">SEO Keywords</Label>
            <Input
              id="seoKeywords"
              {...register('seoKeywords')}
              placeholder="keyword1, keyword2, keyword3..."
            />
            <p className="text-xs text-muted-foreground">
              Comma-separated keywords for SEO
            </p>
          </div>
        </div>
      </Card>
    </div>
  )
}
