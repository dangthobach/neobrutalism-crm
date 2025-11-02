'use client'

import { useRouter } from 'next/navigation'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ArrowLeft, Save } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useCreateContent } from '@/hooks/useContent'
import { CreateContentRequest, ContentType, ContentStatus, MemberTier } from '@/types/content'
import { ContentForm } from '@/components/content/content-form'
import { ContentEditor } from '@/components/content/content-editor'

export default function NewContentPage() {
  const router = useRouter()
  const createContent = useCreateContent()

  const form = useForm<CreateContentRequest>({
    defaultValues: {
      title: '',
      slug: '',
      summary: '',
      body: '',
      contentType: ContentType.BLOG,
      status: ContentStatus.DRAFT,
      tierRequired: MemberTier.FREE,
      categoryIds: [],
      tagIds: [],
    },
  })

  const { handleSubmit, register, formState: { errors }, watch } = form

  const onSubmit = async (data: CreateContentRequest) => {
    createContent.mutate(data, {
      onSuccess: (content) => {
        router.push(`/admin/content/${content.id}`)
      },
    })
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
            <h1 className="text-3xl font-black">New Content</h1>
            <p className="text-muted-foreground">Create a new blog post, article, or page</p>
          </div>
        </div>
        <div className="flex gap-2">
          <Button
            variant="neutral"
            size="lg"
            onClick={handleSubmit((data) => {
              data.status = ContentStatus.DRAFT
              onSubmit(data)
            })}
            disabled={createContent.isPending}
          >
            Save as Draft
          </Button>
          <Button
            size="lg"
            onClick={handleSubmit((data) => {
              data.status = ContentStatus.PUBLISHED
              onSubmit(data)
            })}
            disabled={createContent.isPending}
          >
            <Save className="mr-2 h-4 w-4" />
            {createContent.isPending ? 'Publishing...' : 'Publish'}
          </Button>
        </div>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <ContentForm form={form as any} />
        
        <ContentEditor 
          register={register as any} 
          errors={errors}
          value={watch('body')}
        />
      </form>
    </div>
  )
}
