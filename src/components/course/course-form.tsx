'use client'

import { useEffect } from 'react'
import { UseFormReturn } from 'react-hook-form'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'
import { CourseLevel, CourseStatus, CreateCourseRequest, UpdateCourseRequest } from '@/types/course'

interface CourseFormProps {
  form: UseFormReturn<CreateCourseRequest | UpdateCourseRequest>
  categories?: Array<{ id: string; name: string }>
}

export function CourseForm({ form, categories = [] }: CourseFormProps) {
  const { register, watch, setValue } = form

  const title = watch('title')

  // Auto-generate slug from title
  useEffect(() => {
    if (title && !watch('slug')) {
      const slug = title
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-|-$/g, '')
      setValue('slug', slug)
    }
  }, [title, setValue, watch])

  const handleWhatYouWillLearnChange = (value: string, index: number) => {
    const currentItems = watch('whatYouWillLearn') || []
    const newItems = [...currentItems]
    newItems[index] = value
    setValue('whatYouWillLearn', newItems)
  }

  const addWhatYouWillLearnItem = () => {
    const currentItems = watch('whatYouWillLearn') || []
    setValue('whatYouWillLearn', [...currentItems, ''])
  }

  const removeWhatYouWillLearnItem = (index: number) => {
    const currentItems = watch('whatYouWillLearn') || []
    setValue(
      'whatYouWillLearn',
      currentItems.filter((_, i) => i !== index)
    )
  }

  const handlePrerequisiteChange = (value: string, index: number) => {
    const currentItems = watch('prerequisites') || []
    const newItems = [...currentItems]
    newItems[index] = value
    setValue('prerequisites', newItems)
  }

  const addPrerequisiteItem = () => {
    const currentItems = watch('prerequisites') || []
    setValue('prerequisites', [...currentItems, ''])
  }

  const removePrerequisiteItem = (index: number) => {
    const currentItems = watch('prerequisites') || []
    setValue(
      'prerequisites',
      currentItems.filter((_, i) => i !== index)
    )
  }

  return (
    <div className="space-y-6">
      {/* Section 1: Basic Information */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
          <h3 className="text-lg font-black">Basic Information</h3>
        </div>
        <div className="space-y-4 p-6">
          {/* Title */}
          <div>
            <Label htmlFor="title" className="font-bold">
              Course Title *
            </Label>
            <Input
              id="title"
              {...register('title')}
              placeholder="e.g., Complete Web Development Bootcamp"
              className="border-2 border-black"
            />
          </div>

          {/* Slug */}
          <div>
            <Label htmlFor="slug" className="font-bold">
              URL Slug *
            </Label>
            <Input
              id="slug"
              {...register('slug')}
              placeholder="e.g., complete-web-development-bootcamp"
              className="border-2 border-black font-mono"
            />
            <p className="mt-1 text-xs text-muted-foreground">
              Auto-generated from title. Used in the course URL.
            </p>
          </div>

          {/* Summary */}
          <div>
            <Label htmlFor="summary" className="font-bold">
              Summary
            </Label>
            <Textarea
              id="summary"
              {...register('summary')}
              placeholder="Brief one-liner about the course..."
              rows={2}
              className="border-2 border-black"
            />
          </div>

          {/* Level and Language */}
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <Label htmlFor="level" className="font-bold">
                Course Level *
              </Label>
              <Select
                value={watch('level')}
                onValueChange={(value) => setValue('level', value as CourseLevel)}
              >
                <SelectTrigger className="border-2 border-black">
                  <SelectValue placeholder="Select level" />
                </SelectTrigger>
                <SelectContent className="border-2 border-black">
                  <SelectItem value={CourseLevel.BEGINNER}>🌱 Beginner</SelectItem>
                  <SelectItem value={CourseLevel.INTERMEDIATE}>📚 Intermediate</SelectItem>
                  <SelectItem value={CourseLevel.ADVANCED}>🎯 Advanced</SelectItem>
                  <SelectItem value={CourseLevel.EXPERT}>🏆 Expert</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="language" className="font-bold">
                Language
              </Label>
              <Input
                id="language"
                {...register('language')}
                placeholder="e.g., English"
                className="border-2 border-black"
              />
            </div>
          </div>

          {/* Status and Category */}
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <Label htmlFor="status" className="font-bold">
                Status *
              </Label>
              <Select
                value={watch('status')}
                onValueChange={(value) => setValue('status', value as CourseStatus)}
              >
                <SelectTrigger className="border-2 border-black">
                  <SelectValue placeholder="Select status" />
                </SelectTrigger>
                <SelectContent className="border-2 border-black">
                  <SelectItem value={CourseStatus.DRAFT}>Draft</SelectItem>
                  <SelectItem value={CourseStatus.PUBLISHED}>Published</SelectItem>
                  <SelectItem value={CourseStatus.ARCHIVED}>Archived</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="categoryId" className="font-bold">
                Category
              </Label>
              <Select
                value={watch('categoryId')}
                onValueChange={(value) => setValue('categoryId', value)}
              >
                <SelectTrigger className="border-2 border-black">
                  <SelectValue placeholder="Select category" />
                </SelectTrigger>
                <SelectContent className="border-2 border-black">
                  {categories.map((category) => (
                    <SelectItem key={category.id} value={category.id}>
                      {category.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>
      </Card>

      {/* Section 2: Course Details */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-green-200 px-6 py-4">
          <h3 className="text-lg font-black">Course Details</h3>
        </div>
        <div className="space-y-4 p-6">
          {/* Description */}
          <div>
            <Label htmlFor="description" className="font-bold">
              Full Description
            </Label>
            <Textarea
              id="description"
              {...register('description')}
              placeholder="Detailed description of the course..."
              rows={6}
              className="border-2 border-black"
            />
          </div>

          {/* What You Will Learn */}
          <div>
            <Label className="font-bold">What You Will Learn</Label>
            <div className="mt-2 space-y-2">
              {(watch('whatYouWillLearn') || []).map((_, index) => (
                <div key={index} className="flex gap-2">
                  <Input
                    value={(watch('whatYouWillLearn') || [])[index]}
                    onChange={(e) => handleWhatYouWillLearnChange(e.target.value, index)}
                    placeholder="e.g., Build responsive websites"
                    className="border-2 border-black"
                  />
                  <button
                    type="button"
                    onClick={() => removeWhatYouWillLearnItem(index)}
                    className="rounded border-2 border-black bg-red-200 px-3 hover:bg-red-300"
                  >
                    ✕
                  </button>
                </div>
              ))}
              <button
                type="button"
                onClick={addWhatYouWillLearnItem}
                className="w-full rounded border-2 border-dashed border-black bg-green-50 py-2 font-bold hover:bg-green-100"
              >
                + Add Learning Objective
              </button>
            </div>
          </div>

          {/* Prerequisites */}
          <div>
            <Label className="font-bold">Prerequisites</Label>
            <div className="mt-2 space-y-2">
              {(watch('prerequisites') || []).map((_, index) => (
                <div key={index} className="flex gap-2">
                  <Input
                    value={(watch('prerequisites') || [])[index]}
                    onChange={(e) => handlePrerequisiteChange(e.target.value, index)}
                    placeholder="e.g., Basic HTML knowledge"
                    className="border-2 border-black"
                  />
                  <button
                    type="button"
                    onClick={() => removePrerequisiteItem(index)}
                    className="rounded border-2 border-black bg-red-200 px-3 hover:bg-red-300"
                  >
                    ✕
                  </button>
                </div>
              ))}
              <button
                type="button"
                onClick={addPrerequisiteItem}
                className="w-full rounded border-2 border-dashed border-black bg-yellow-50 py-2 font-bold hover:bg-yellow-100"
              >
                + Add Prerequisite
              </button>
            </div>
          </div>
        </div>
      </Card>

      {/* Section 3: Pricing & Access */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
          <h3 className="text-lg font-black">Pricing & Access</h3>
        </div>
        <div className="space-y-4 p-6">
          {/* Price and Currency */}
          <div className="grid gap-4 md:grid-cols-3">
            <div className="md:col-span-2">
              <Label htmlFor="price" className="font-bold">
                Price *
              </Label>
              <Input
                id="price"
                type="number"
                step="0.01"
                {...register('price', { valueAsNumber: true })}
                placeholder="0.00"
                className="border-2 border-black"
              />
              <p className="mt-1 text-xs text-muted-foreground">Set to 0 for free courses</p>
            </div>

            <div>
              <Label htmlFor="currency" className="font-bold">
                Currency
              </Label>
              <Input
                id="currency"
                {...register('currency')}
                placeholder="USD"
                maxLength={3}
                className="border-2 border-black uppercase"
              />
            </div>
          </div>

          {/* Duration */}
          <div>
            <Label htmlFor="duration" className="font-bold">
              Course Duration (minutes)
            </Label>
            <Input
              id="duration"
              type="number"
              {...register('duration', { valueAsNumber: true })}
              placeholder="e.g., 600 for 10 hours"
              className="border-2 border-black"
            />
          </div>

          {/* Certificate Enabled */}
          <div className="flex items-center space-x-2">
            <Checkbox
              id="certificateEnabled"
              checked={watch('certificateEnabled')}
              onCheckedChange={(checked) => setValue('certificateEnabled', checked as boolean)}
              className="border-2 border-black"
            />
            <Label htmlFor="certificateEnabled" className="font-bold">
              Issue certificate upon completion
            </Label>
          </div>
        </div>
      </Card>

      {/* Section 4: Media */}
      <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
          <h3 className="text-lg font-black">Media</h3>
        </div>
        <div className="space-y-4 p-6">
          {/* Thumbnail URL */}
          <div>
            <Label htmlFor="thumbnailUrl" className="font-bold">
              Thumbnail URL
            </Label>
            <Input
              id="thumbnailUrl"
              {...register('thumbnailUrl')}
              placeholder="https://example.com/thumbnail.jpg"
              className="border-2 border-black"
            />
            <p className="mt-1 text-xs text-muted-foreground">
              Recommended size: 1280x720 (16:9 ratio)
            </p>
          </div>

          {/* Tags */}
          <div>
            <Label htmlFor="tags" className="font-bold">
              Tags (comma-separated)
            </Label>
            <Input
              id="tags"
              placeholder="e.g., web development, javascript, react"
              value={(watch('tags') || []).join(', ')}
              onChange={(e) => {
                const tagsArray = e.target.value.split(',').map((tag) => tag.trim())
                setValue('tags', tagsArray)
              }}
              className="border-2 border-black"
            />
          </div>
        </div>
      </Card>
    </div>
  )
}
