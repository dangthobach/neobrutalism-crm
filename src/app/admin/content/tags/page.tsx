'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { ArrowLeft } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useTags, useCreateTag, useUpdateTag, useDeleteTag } from '@/hooks/useContentTags'
import { TagCloud } from '@/components/content/tag-cloud'
import { ContentTag, CreateTagRequest, UpdateTagRequest } from '@/types/content'

export default function TagsPage() {
  const router = useRouter()
  const { data: tagsData, isLoading } = useTags()
  const createTag = useCreateTag()
  const updateTag = useUpdateTag()
  const deleteTag = useDeleteTag()

  const [showDialog, setShowDialog] = useState(false)
  const [editingTag, setEditingTag] = useState<ContentTag | null>(null)
  const [formData, setFormData] = useState<CreateTagRequest | UpdateTagRequest>({
    name: '',
    slug: '',
    description: '',
  })

  const tags = tagsData?.content || []

  const handleAdd = () => {
    setEditingTag(null)
    setFormData({ name: '', slug: '', description: '' })
    setShowDialog(true)
  }

  const handleEdit = (tag: ContentTag) => {
    setEditingTag(tag)
    setFormData({
      name: tag.name,
      slug: tag.slug,
      description: tag.description,
    })
    setShowDialog(true)
  }

  const handleDelete = async (tagId: string) => {
    if (confirm('Are you sure you want to delete this tag?')) {
      await deleteTag.mutateAsync(tagId)
    }
  }

  const handleSubmit = async () => {
    if (editingTag) {
      await updateTag.mutateAsync({ id: editingTag.id, data: formData })
    } else {
      const slug = formData.slug || (formData.name || '').toLowerCase().replace(/[^a-z0-9]+/g, '-')
      await createTag.mutateAsync({ ...formData as CreateTagRequest, slug })
    }
    setShowDialog(false)
  }

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
          <p className="mt-4 font-bold">Loading tags...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="neutral" size="icon" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-4xl font-black">Tag Management</h1>
          <p className="text-muted-foreground">
            Manage tags to help organize and find content
          </p>
        </div>
      </div>

      {/* Stats */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-pink-200 px-6 py-4">
          <h2 className="text-xl font-black">Statistics</h2>
        </div>
        <div className="grid gap-4 p-6 md:grid-cols-3">
          <div>
            <p className="text-3xl font-black">{tags.length}</p>
            <p className="text-sm text-muted-foreground">Total Tags</p>
          </div>
          <div>
            <p className="text-3xl font-black">
              {tags.reduce((sum, tag) => sum + tag.usageCount, 0)}
            </p>
            <p className="text-sm text-muted-foreground">Total Usage</p>
          </div>
          <div>
            <p className="text-3xl font-black">
              {tags.filter((t) => t.usageCount > 0).length}
            </p>
            <p className="text-sm text-muted-foreground">Active Tags</p>
          </div>
        </div>
      </Card>

      {/* Tag Cloud */}
      <TagCloud
        tags={tags}
        variant="admin"
        onAdd={handleAdd}
        onEdit={handleEdit}
        onDelete={handleDelete}
      />

      {/* Add/Edit Dialog */}
      <Dialog open={showDialog} onOpenChange={setShowDialog}>
        <DialogContent className="border-2 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
          <DialogHeader>
            <DialogTitle>{editingTag ? 'Edit Tag' : 'Add New Tag'}</DialogTitle>
            <DialogDescription>
              {editingTag
                ? 'Update the tag information.'
                : 'Create a new tag to organize content.'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Name *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) =>
                  setFormData({ ...formData, name: e.target.value })
                }
                placeholder="Tag name..."
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="slug">Slug</Label>
              <Input
                id="slug"
                value={formData.slug}
                onChange={(e) =>
                  setFormData({ ...formData, slug: e.target.value })
                }
                placeholder="tag-slug"
              />
              <p className="text-xs text-muted-foreground">
                Leave empty to auto-generate from name
              </p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Input
                id="description"
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                placeholder="Tag description..."
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="neutral" onClick={() => setShowDialog(false)}>
              Cancel
            </Button>
            <Button onClick={handleSubmit}>
              {editingTag ? 'Update' : 'Create'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
