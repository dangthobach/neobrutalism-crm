'use client'

import { use, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { ArrowLeft, Edit, Save, Trash2, X, Eye, BarChart } from 'lucide-react'
import { useForm } from 'react-hook-form'
import {
  useContent,
  useUpdateContent,
  useDeleteContent,
  usePublishContent,
  useUnpublishContent,
} from '@/hooks/useContent'
import { UpdateContentRequest, ContentStatus } from '@/types/content'
import { ContentStatusBadge } from '@/components/content/content-status-badge'
import { ContentForm } from '@/components/content/content-form'
import { ContentEditor } from '@/components/content/content-editor'
import { ContentPreview } from '@/components/content/content-preview'

interface ContentDetailPageProps {
  params: Promise<{ id: string }>
}

export default function ContentDetailPage({ params }: ContentDetailPageProps) {
  const { id } = use(params)
  const router = useRouter()
  const [isEditing, setIsEditing] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)

  const { data: content, isLoading } = useContent(id)
  const updateContent = useUpdateContent()
  const deleteContent = useDeleteContent()
  const publishContent = usePublishContent()
  const unpublishContent = useUnpublishContent()

  const form = useForm<UpdateContentRequest>({
    values: content
      ? {
          title: content.title,
          slug: content.slug,
          summary: content.summary,
          body: content.body,
          featuredImageId: content.featuredImageId,
          contentType: content.contentType,
          status: content.status,
          publishedAt: content.publishedAt,
          tierRequired: content.tierRequired,
          seriesId: content.seriesId,
          seriesOrder: content.seriesOrder,
          seoTitle: content.seoTitle,
          seoDescription: content.seoDescription,
          seoKeywords: content.seoKeywords,
          categoryIds: content.categories.map(c => c.id),
          tagIds: content.tags.map(t => t.id),
        }
      : undefined,
  })

  const { handleSubmit, register, formState: { errors }, watch, reset } = form

  const onSubmit = async (data: UpdateContentRequest) => {
    updateContent.mutate(
      { id, data },
      {
        onSuccess: () => {
          setIsEditing(false)
        },
      }
    )
  }

  const handleDelete = async () => {
    deleteContent.mutate(id, {
      onSuccess: () => {
        router.push('/admin/content')
      },
    })
  }

  const handlePublish = async () => {
    publishContent.mutate(id)
  }

  const handleUnpublish = async () => {
    unpublishContent.mutate(id)
  }

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
          <p className="mt-4 font-bold">Loading content...</p>
        </div>
      </div>
    )
  }

  if (!content) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <p className="text-2xl font-black">Content not found</p>
          <Button onClick={() => router.back()} className="mt-4">
            Go Back
          </Button>
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
            <div className="flex items-center gap-3">
              <h1 className="text-3xl font-black line-clamp-1 max-w-2xl">{content.title}</h1>
              <ContentStatusBadge status={content.status} />
            </div>
            <p className="text-muted-foreground">
              {content.contentType} • {content.viewCount || 0} views
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          {!isEditing ? (
            <>
              {content.status === ContentStatus.DRAFT && (
                <Button onClick={handlePublish} size="lg">
                  Publish
                </Button>
              )}
              {content.status === ContentStatus.PUBLISHED && (
                <Button variant="neutral" onClick={handleUnpublish} size="lg">
                  Unpublish
                </Button>
              )}
              <Button variant="neutral" size="lg" onClick={() => setIsEditing(true)}>
                <Edit className="mr-2 h-4 w-4" />
                Edit
              </Button>
              <Button
                variant="reverse"
                size="lg"
                onClick={() => setShowDeleteDialog(true)}
                className="bg-red-500 hover:bg-red-600"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="neutral"
                size="lg"
                onClick={() => {
                  setIsEditing(false)
                  reset()
                }}
              >
                <X className="mr-2 h-4 w-4" />
                Cancel
              </Button>
              <Button
                size="lg"
                onClick={handleSubmit(onSubmit)}
                disabled={updateContent.isPending}
              >
                <Save className="mr-2 h-4 w-4" />
                {updateContent.isPending ? 'Saving...' : 'Save Changes'}
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="editor" className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="editor">
            <Edit className="mr-2 h-4 w-4" />
            Editor
          </TabsTrigger>
          <TabsTrigger value="preview">
            <Eye className="mr-2 h-4 w-4" />
            Preview
          </TabsTrigger>
          <TabsTrigger value="analytics">
            <BarChart className="mr-2 h-4 w-4" />
            Analytics
          </TabsTrigger>
        </TabsList>

        <TabsContent value="editor" className="space-y-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <ContentForm form={form as any} isEditing={!isEditing} />
            <ContentEditor
              register={register as any}
              errors={errors}
              value={watch('body')}
            />
          </form>
        </TabsContent>

        <TabsContent value="preview">
          <ContentPreview content={content} variant="full" />
        </TabsContent>

        <TabsContent value="analytics">
          <div className="grid gap-6 md:grid-cols-3">
            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
                <h3 className="font-black">Views</h3>
              </div>
              <div className="p-6">
                <p className="text-4xl font-black">{content.viewCount || 0}</p>
                <p className="text-sm text-muted-foreground">Total views</p>
              </div>
            </Card>

            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-green-200 px-6 py-4">
                <h3 className="font-black">Series</h3>
              </div>
              <div className="p-6">
                {content.seriesName ? (
                  <>
                    <p className="text-xl font-black">{content.seriesName}</p>
                    <p className="text-sm text-muted-foreground">
                      Part {content.seriesOrder} of series
                    </p>
                  </>
                ) : (
                  <p className="text-muted-foreground">Not in a series</p>
                )}
              </div>
            </Card>

            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
                <h3 className="font-black">Access</h3>
              </div>
              <div className="p-6">
                <p className="text-xl font-black">{content.tierRequired}</p>
                <p className="text-sm text-muted-foreground">Required tier</p>
              </div>
            </Card>
          </div>

          <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
              <h3 className="text-lg font-black">Metadata</h3>
            </div>
            <div className="grid gap-4 p-6 md:grid-cols-2">
              <div>
                <span className="font-bold">Created:</span>{' '}
                {new Date(content.createdAt).toLocaleString()}
              </div>
              <div>
                <span className="font-bold">Updated:</span>{' '}
                {new Date(content.updatedAt).toLocaleString()}
              </div>
              <div>
                <span className="font-bold">Author:</span> {content.authorName}
              </div>
              <div>
                <span className="font-bold">Published:</span>{' '}
                {content.publishedAt
                  ? new Date(content.publishedAt).toLocaleString()
                  : 'Not published'}
              </div>
            </div>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent className="border-2 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently delete <strong>{content.title}</strong>. This action cannot be
              undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-red-500 hover:bg-red-600">
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
