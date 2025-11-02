'use client'

import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Card } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { ArrowLeft, Plus, Edit, Trash2, Star } from 'lucide-react'
import { useRouter } from 'next/navigation'
import {
  useSeries,
  useCreateSeries,
  useUpdateSeries,
  useDeleteSeries,
} from '@/hooks/useContentSeries'
import { ContentSeries, CreateSeriesRequest, UpdateSeriesRequest } from '@/types/content'

export default function SeriesPage() {
  const router = useRouter()
  const { data: seriesData, isLoading } = useSeries()
  const createSeries = useCreateSeries()
  const updateSeries = useUpdateSeries()
  const deleteSeries = useDeleteSeries()

  const [showDialog, setShowDialog] = useState(false)
  const [editingSeries, setEditingSeries] = useState<ContentSeries | null>(null)
  const [formData, setFormData] = useState<CreateSeriesRequest | UpdateSeriesRequest>({
    name: '',
    slug: '',
    description: '',
    isActive: true,
  })

  const series = seriesData?.content || []

  const handleAdd = () => {
    setEditingSeries(null)
    setFormData({ name: '', slug: '', description: '', isActive: true })
    setShowDialog(true)
  }

  const handleEdit = (s: ContentSeries) => {
    setEditingSeries(s)
    setFormData({
      name: s.name,
      slug: s.slug,
      description: s.description,
      isActive: s.isActive,
    })
    setShowDialog(true)
  }

  const handleDelete = async (seriesId: string) => {
    if (confirm('Are you sure you want to delete this series?')) {
      await deleteSeries.mutateAsync(seriesId)
    }
  }

  const handleSubmit = async () => {
    if (editingSeries) {
      await updateSeries.mutateAsync({ id: editingSeries.id, data: formData })
    } else {
      const slug = formData.slug || (formData.name || '').toLowerCase().replace(/[^a-z0-9]+/g, '-')
      await createSeries.mutateAsync({ ...formData as CreateSeriesRequest, slug })
    }
    setShowDialog(false)
  }

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
          <p className="mt-4 font-bold">Loading series...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="neutral" size="icon" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-4xl font-black">Series Management</h1>
            <p className="text-muted-foreground">
              Group related content into series
            </p>
          </div>
        </div>
        <Button onClick={handleAdd} size="lg">
          <Plus className="mr-2 h-4 w-4" />
          New Series
        </Button>
      </div>

      {/* Stats */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
          <h2 className="text-xl font-black">Statistics</h2>
        </div>
        <div className="grid gap-4 p-6 md:grid-cols-3">
          <div>
            <p className="text-3xl font-black">{series.length}</p>
            <p className="text-sm text-muted-foreground">Total Series</p>
          </div>
          <div>
            <p className="text-3xl font-black">
              {series.reduce((sum, s) => sum + s.contentCount, 0)}
            </p>
            <p className="text-sm text-muted-foreground">Total Content</p>
          </div>
          <div>
            <p className="text-3xl font-black">
              {series.filter((s) => s.isActive).length}
            </p>
            <p className="text-sm text-muted-foreground">Active Series</p>
          </div>
        </div>
      </Card>

      {/* Series List */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-green-200 px-6 py-4">
          <h2 className="text-xl font-black">All Series</h2>
        </div>
        <div className="p-6">
          {series.length === 0 ? (
            <div className="flex h-32 items-center justify-center text-muted-foreground">
              <p>No series yet. Create one to get started!</p>
            </div>
          ) : (
            <div className="space-y-3">
              {series.map((s) => (
                <div
                  key={s.id}
                  className="group flex items-start justify-between rounded border-2 border-black bg-white p-4 hover:bg-gray-50"
                >
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="text-lg font-black">{s.name}</h3>
                      {!s.isActive && (
                        <span className="rounded border-2 border-black bg-red-200 px-2 py-0.5 text-xs font-bold">
                          Inactive
                        </span>
                      )}
                    </div>
                    {s.description && (
                      <p className="mt-1 text-sm text-muted-foreground">
                        {s.description}
                      </p>
                    )}
                    <div className="mt-2 flex items-center gap-4 text-xs text-muted-foreground">
                      <span className="rounded border border-black bg-blue-100 px-2 py-1 font-mono">
                        {s.contentCount} articles
                      </span>
                      <span className="font-mono">{s.slug}</span>
                    </div>
                  </div>
                  <div className="flex gap-1 opacity-0 group-hover:opacity-100">
                    <Button
                      size="sm"
                      variant="neutral"
                      onClick={() => handleEdit(s)}
                    >
                      <Edit className="h-3 w-3" />
                    </Button>
                    <Button
                      size="sm"
                      variant="neutral"
                      onClick={() => handleDelete(s.id)}
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </Card>

      {/* Add/Edit Dialog */}
      <Dialog open={showDialog} onOpenChange={setShowDialog}>
        <DialogContent className="border-2 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
          <DialogHeader>
            <DialogTitle>
              {editingSeries ? 'Edit Series' : 'Add New Series'}
            </DialogTitle>
            <DialogDescription>
              {editingSeries
                ? 'Update the series information.'
                : 'Create a new series to group related content.'}
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
                placeholder="Series name..."
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
                placeholder="series-slug"
              />
              <p className="text-xs text-muted-foreground">
                Leave empty to auto-generate from name
              </p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                placeholder="Series description..."
                rows={3}
              />
            </div>
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="isActive"
                checked={formData.isActive}
                onChange={(e) =>
                  setFormData({ ...formData, isActive: e.target.checked })
                }
                className="h-4 w-4"
              />
              <Label htmlFor="isActive" className="cursor-pointer">
                Active
              </Label>
            </div>
          </div>
          <DialogFooter>
            <Button variant="neutral" onClick={() => setShowDialog(false)}>
              Cancel
            </Button>
            <Button onClick={handleSubmit}>
              {editingSeries ? 'Update' : 'Create'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
