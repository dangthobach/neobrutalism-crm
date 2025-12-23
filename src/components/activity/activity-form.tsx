/**
 * Activity Form Component
 * Form for creating and editing activities
 */

'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { CreateActivityRequest, ActivityType, ActivityStatus } from '@/types/activity'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

interface ActivityFormProps {
  activity?: CreateActivityRequest
  onSubmit: (data: CreateActivityRequest) => void
  onCancel?: () => void
  isSubmitting?: boolean
}

export function ActivityForm({ activity, onSubmit, onCancel, isSubmitting }: ActivityFormProps) {
  const { register, handleSubmit, formState: { errors }, setValue, watch } = useForm<any>({
    defaultValues: activity || {
      type: ActivityType.CALL,
      status: ActivityStatus.SCHEDULED,
    },
  })

  const watchType = watch('type')
  const watchStatus = watch('status')

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Section 1: Basic Information */}
      <div className="overflow-hidden rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
          <h2 className="font-black uppercase">Basic Information</h2>
        </div>
        <div className="grid gap-6 p-6 md:grid-cols-2">
          <div>
            <Label htmlFor="type" className="font-bold uppercase">
              Activity Type *
            </Label>
            <Select
              value={watchType}
              onValueChange={(value) => setValue('type', value)}
            >
              <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                <SelectValue placeholder="Select type" />
              </SelectTrigger>
              <SelectContent>
                {Object.values(ActivityType).map((type) => (
                  <SelectItem key={type} value={type}>
                    {type}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div>
            <Label htmlFor="status" className="font-bold uppercase">
              Status *
            </Label>
            <Select
              value={watchStatus}
              onValueChange={(value) => setValue('status', value)}
            >
              <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                <SelectValue placeholder="Select status" />
              </SelectTrigger>
              <SelectContent>
                {Object.values(ActivityStatus).map((status) => (
                  <SelectItem key={status} value={status}>
                    {status}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="md:col-span-2">
            <Label htmlFor="subject" className="font-bold uppercase">
              Subject *
            </Label>
            <Input
              id="subject"
              {...register('subject', { required: 'Subject is required' })}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="e.g., Follow-up call with John"
            />
            {errors.subject && (
              <p className="mt-1 text-sm font-bold text-red-600">
                {String(errors.subject.message)}
              </p>
            )}
          </div>

          <div className="md:col-span-2">
            <Label htmlFor="description" className="font-bold uppercase">
              Description
            </Label>
            <Textarea
              id="description"
              {...register('description')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="Activity description..."
              rows={4}
            />
          </div>
        </div>
      </div>

      {/* Section 2: Scheduling */}
      <div className="overflow-hidden rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-green-200 px-6 py-4">
          <h2 className="font-black uppercase">Scheduling</h2>
        </div>
        <div className="grid gap-6 p-6 md:grid-cols-2">
          <div>
            <Label htmlFor="scheduledDate" className="font-bold uppercase">
              Scheduled Date
            </Label>
            <Input
              id="scheduledDate"
              type="date"
              {...register('scheduledDate')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>

          <div>
            <Label htmlFor="scheduledTime" className="font-bold uppercase">
              Scheduled Time
            </Label>
            <Input
              id="scheduledTime"
              type="time"
              {...register('scheduledTime')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>

          <div>
            <Label htmlFor="duration" className="font-bold uppercase">
              Duration (minutes)
            </Label>
            <Input
              id="duration"
              type="number"
              {...register('duration', { valueAsNumber: true })}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="e.g., 30"
            />
          </div>

          <div>
            <Label htmlFor="location" className="font-bold uppercase">
              Location
            </Label>
            <Input
              id="location"
              {...register('location')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="e.g., Conference Room A"
            />
          </div>

          <div className="md:col-span-2">
            <Label htmlFor="meetingUrl" className="font-bold uppercase">
              Meeting URL
            </Label>
            <Input
              id="meetingUrl"
              type="url"
              {...register('meetingUrl')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="https://meet.example.com/..."
            />
          </div>
        </div>
      </div>

      {/* Section 3: Related To */}
      <div className="overflow-hidden rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
          <h2 className="font-black uppercase">Related To</h2>
        </div>
        <div className="grid gap-6 p-6 md:grid-cols-3">
          <div>
            <Label htmlFor="customerId" className="font-bold uppercase">
              Customer ID
            </Label>
            <Input
              id="customerId"
              {...register('customerId')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="Customer ID"
            />
          </div>

          <div>
            <Label htmlFor="contactId" className="font-bold uppercase">
              Contact ID
            </Label>
            <Input
              id="contactId"
              {...register('contactId')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="Contact ID"
            />
          </div>

          <div>
            <Label htmlFor="assignedToId" className="font-bold uppercase">
              Assigned To ID
            </Label>
            <Input
              id="assignedToId"
              {...register('assignedToId')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="User ID"
            />
          </div>
        </div>
      </div>

      {/* Section 4: Outcome */}
      <div className="overflow-hidden rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
          <h2 className="font-black uppercase">Outcome</h2>
        </div>
        <div className="grid gap-6 p-6">
          <div>
            <Label htmlFor="outcome" className="font-bold uppercase">
              Outcome
            </Label>
            <Textarea
              id="outcome"
              {...register('outcome')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="What was the outcome of this activity?"
              rows={4}
            />
          </div>

          <div>
            <Label htmlFor="nextSteps" className="font-bold uppercase">
              Next Steps
            </Label>
            <Textarea
              id="nextSteps"
              {...register('nextSteps')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="What are the next steps?"
              rows={3}
            />
          </div>

          <div>
            <Label htmlFor="tags" className="font-bold uppercase">
              Tags
            </Label>
            <Input
              id="tags"
              {...register('tags')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="Comma-separated tags"
            />
          </div>

          <div>
            <Label htmlFor="attachments" className="font-bold uppercase">
              Attachments
            </Label>
            <Input
              id="attachments"
              {...register('attachments')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="Comma-separated attachment URLs"
            />
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-4">
        <Button
          type="submit"
          disabled={isSubmitting}
          className="border-2 border-black bg-blue-400 px-8 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-blue-500 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
        >
          {isSubmitting ? 'Saving...' : activity ? 'Update Activity' : 'Create Activity'}
        </Button>
        {onCancel && (
          <Button
            type="button"
            variant="neutral"
            onClick={onCancel}
            className="border-2 border-black px-8 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          >
            Cancel
          </Button>
        )}
      </div>
    </form>
  )
}
