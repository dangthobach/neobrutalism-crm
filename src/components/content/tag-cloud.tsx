/**
 * Tag Cloud Component
 * Visual tag display with usage-based sizing and Neobrutalism styling
 */

'use client'

import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ContentTag } from '@/types/content'
import { Edit, Trash2, Plus } from 'lucide-react'
import { useRouter } from 'next/navigation'

interface TagCloudProps {
  tags: ContentTag[]
  onTagClick?: (tag: ContentTag) => void
  onEdit?: (tag: ContentTag) => void
  onDelete?: (tagId: string) => void
  onAdd?: () => void
  variant?: 'default' | 'admin' | 'public'
  maxTags?: number
}

export function TagCloud({
  tags,
  onTagClick,
  onEdit,
  onDelete,
  onAdd,
  variant = 'default',
  maxTags,
}: TagCloudProps) {
  const router = useRouter()

  // Calculate tag sizes based on usage
  const getTagSize = (usageCount: number, maxUsage: number) => {
    const ratio = usageCount / maxUsage
    if (ratio > 0.7) return 'text-2xl'
    if (ratio > 0.4) return 'text-xl'
    if (ratio > 0.2) return 'text-lg'
    return 'text-base'
  }

  const getTagColor = (usageCount: number, maxUsage: number) => {
    const ratio = usageCount / maxUsage
    if (ratio > 0.7) return 'bg-yellow-300 hover:bg-yellow-400'
    if (ratio > 0.4) return 'bg-blue-300 hover:bg-blue-400'
    if (ratio > 0.2) return 'bg-green-300 hover:bg-green-400'
    return 'bg-gray-200 hover:bg-gray-300'
  }

  const displayTags = maxTags ? tags.slice(0, maxTags) : tags
  const maxUsage = Math.max(...tags.map((t) => t.usageCount), 1)

  const handleTagClick = (tag: ContentTag) => {
    if (onTagClick) {
      onTagClick(tag)
    } else if (variant === 'public') {
      router.push(`/blog/tag/${tag.slug}`)
    }
  }

  if (variant === 'admin') {
    return (
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="flex items-center justify-between border-b-2 border-black bg-pink-200 px-6 py-4">
          <h2 className="text-xl font-black">Tags</h2>
          {onAdd && (
            <Button onClick={onAdd} size="sm">
              <Plus className="mr-2 h-4 w-4" />
              Add Tag
            </Button>
          )}
        </div>
        <div className="p-6">
          {displayTags.length === 0 ? (
            <div className="flex h-32 items-center justify-center text-muted-foreground">
              <p>No tags yet. Add one to get started!</p>
            </div>
          ) : (
            <div className="space-y-2">
              {displayTags.map((tag) => (
                <div
                  key={tag.id}
                  className="group flex items-center justify-between rounded border-2 border-black bg-white p-3 hover:bg-gray-50"
                >
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => handleTagClick(tag)}
                      className="rounded-full border-2 border-black bg-yellow-200 px-4 py-2 font-bold hover:bg-yellow-300"
                    >
                      #{tag.name}
                    </button>
                    {tag.description && (
                      <span className="text-sm text-muted-foreground">
                        {tag.description}
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="rounded border-2 border-black bg-blue-100 px-3 py-1 font-mono text-sm font-bold">
                      {tag.usageCount}
                    </span>
                    <div className="flex gap-1 opacity-0 group-hover:opacity-100">
                      {onEdit && (
                        <Button
                          size="sm"
                          variant="neutral"
                          onClick={() => onEdit(tag)}
                        >
                          <Edit className="h-3 w-3" />
                        </Button>
                      )}
                      {onDelete && (
                        <Button
                          size="sm"
                          variant="neutral"
                          onClick={() => onDelete(tag.id)}
                        >
                          <Trash2 className="h-3 w-3" />
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </Card>
    )
  }

  // Public or default variant - cloud style
  return (
    <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
      <div className="border-b-2 border-black bg-pink-200 px-6 py-4">
        <h2 className="text-xl font-black">Popular Tags</h2>
      </div>
      <div className="p-6">
        {displayTags.length === 0 ? (
          <div className="flex h-32 items-center justify-center text-muted-foreground">
            <p>No tags available</p>
          </div>
        ) : (
          <div className="flex flex-wrap gap-3">
            {displayTags.map((tag) => (
              <button
                key={tag.id}
                onClick={() => handleTagClick(tag)}
                className={`group relative rounded-full border-2 border-black px-4 py-2 font-bold transition-all ${getTagSize(
                  tag.usageCount,
                  maxUsage
                )} ${getTagColor(tag.usageCount, maxUsage)}`}
              >
                <span>#{tag.name}</span>
                <span className="ml-2 rounded-full bg-black px-2 py-0.5 text-xs text-white">
                  {tag.usageCount}
                </span>
                {tag.description && (
                  <span className="absolute -top-12 left-1/2 z-10 hidden -translate-x-1/2 rounded border-2 border-black bg-white px-3 py-2 text-sm font-normal shadow-lg group-hover:block">
                    {tag.description}
                  </span>
                )}
              </button>
            ))}
          </div>
        )}
        {maxTags && tags.length > maxTags && (
          <div className="mt-4 text-center">
            <Button variant="neutral" onClick={() => router.push('/blog/tags')}>
              View All {tags.length} Tags
            </Button>
          </div>
        )}
      </div>
    </Card>
  )
}
