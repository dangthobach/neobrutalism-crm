/**
 * Task Form Component
 * Form for creating and editing tasks
 */

'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { CreateTaskRequest, TaskStatus, TaskPriority, TaskCategory } from '@/types/task'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Checkbox } from '@/components/ui/checkbox'

interface TaskFormProps {
  task?: CreateTaskRequest
  onSubmit: (data: CreateTaskRequest) => void
  onCancel?: () => void
  isSubmitting?: boolean
}

export function TaskForm({ task, onSubmit, onCancel, isSubmitting }: TaskFormProps) {
  const { register, handleSubmit, formState: { errors }, setValue, watch } = useForm<any>({
    defaultValues: task || {
      status: TaskStatus.TODO,
      priority: TaskPriority.MEDIUM,
      category: TaskCategory.OTHER,
    },
  })

  const [checklistItems, setChecklistItems] = useState<Array<{ text: string; isCompleted: boolean }>>(
    task?.checklistItems?.map(item => ({ text: item.text, isCompleted: item.isCompleted })) || []
  )
  const [newChecklistItem, setNewChecklistItem] = useState('')

  const watchStatus = watch('status')
  const watchPriority = watch('priority')
  const watchCategory = watch('category')

  const addChecklistItem = () => {
    if (newChecklistItem.trim()) {
      setChecklistItems([...checklistItems, { text: newChecklistItem.trim(), isCompleted: false }])
      setNewChecklistItem('')
    }
  }

  const removeChecklistItem = (index: number) => {
    setChecklistItems(checklistItems.filter((_, i) => i !== index))
  }

  const handleFormSubmit = (data: any) => {
    onSubmit({
      ...data,
      checklistItems: checklistItems.length > 0 ? checklistItems : undefined,
    })
  }

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      {/* Section 1: Basic Information */}
      <div className="overflow-hidden rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
          <h2 className="font-black uppercase">Basic Information</h2>
        </div>
        <div className="grid gap-6 p-6">
          <div>
            <Label htmlFor="title" className="font-bold uppercase">
              Task Title *
            </Label>
            <Input
              id="title"
              {...register('title', { required: 'Title is required' })}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="e.g., Prepare proposal for Acme Corp"
            />
            {errors.title && (
              <p className="mt-1 text-sm font-bold text-red-600">
                {String(errors.title.message)}
              </p>
            )}
          </div>

          <div>
            <Label htmlFor="description" className="font-bold uppercase">
              Description
            </Label>
            <Textarea
              id="description"
              {...register('description')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="Task description..."
              rows={4}
            />
          </div>

          <div className="grid gap-6 md:grid-cols-3">
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
                  {Object.values(TaskStatus).map((status) => (
                    <SelectItem key={status} value={status}>
                      {status.replace('_', ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="priority" className="font-bold uppercase">
                Priority *
              </Label>
              <Select
                value={watchPriority}
                onValueChange={(value) => setValue('priority', value)}
              >
                <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  <SelectValue placeholder="Select priority" />
                </SelectTrigger>
                <SelectContent>
                  {Object.values(TaskPriority).map((priority) => (
                    <SelectItem key={priority} value={priority}>
                      {priority}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="category" className="font-bold uppercase">
                Category *
              </Label>
              <Select
                value={watchCategory}
                onValueChange={(value) => setValue('category', value)}
              >
                <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  <SelectValue placeholder="Select category" />
                </SelectTrigger>
                <SelectContent>
                  {Object.values(TaskCategory).map((category) => (
                    <SelectItem key={category} value={category}>
                      {category.replace('_', ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>
      </div>

      {/* Section 2: Assignment */}
      <div className="overflow-hidden rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
          <h2 className="font-black uppercase">Assignment</h2>
        </div>
        <div className="grid gap-6 p-6 md:grid-cols-2">
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

          <div>
            <Label htmlFor="relatedActivityId" className="font-bold uppercase">
              Related Activity ID
            </Label>
            <Input
              id="relatedActivityId"
              {...register('relatedActivityId')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="Activity ID"
            />
          </div>
        </div>
      </div>

      {/* Section 3: Schedule */}
      <div className="overflow-hidden rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-green-200 px-6 py-4">
          <h2 className="font-black uppercase">Schedule</h2>
        </div>
        <div className="grid gap-6 p-6 md:grid-cols-2">
          <div>
            <Label htmlFor="startDate" className="font-bold uppercase">
              Start Date
            </Label>
            <Input
              id="startDate"
              type="date"
              {...register('startDate')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>

          <div>
            <Label htmlFor="dueDate" className="font-bold uppercase">
              Due Date
            </Label>
            <Input
              id="dueDate"
              type="date"
              {...register('dueDate')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>

          <div>
            <Label htmlFor="estimatedHours" className="font-bold uppercase">
              Estimated Hours
            </Label>
            <Input
              id="estimatedHours"
              type="number"
              step="0.5"
              {...register('estimatedHours', { valueAsNumber: true })}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              placeholder="e.g., 4"
            />
          </div>

          <div>
            <Label htmlFor="reminderDate" className="font-bold uppercase">
              Reminder Date
            </Label>
            <Input
              id="reminderDate"
              type="datetime-local"
              {...register('reminderDate')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>
        </div>
      </div>

      {/* Section 4: Details */}
      <div className="overflow-hidden rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
          <h2 className="font-black uppercase">Details</h2>
        </div>
        <div className="grid gap-6 p-6">
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

          {/* Checklist Items */}
          <div>
            <Label className="font-bold uppercase">Checklist Items</Label>
            <div className="mt-2 space-y-2">
              {checklistItems.map((item, index) => (
                <div key={index} className="flex items-center gap-2 rounded border-2 border-black bg-gray-50 p-2">
                  <Checkbox
                    checked={item.isCompleted}
                    onCheckedChange={(checked) => {
                      const updated = [...checklistItems]
                      updated[index].isCompleted = checked === true
                      setChecklistItems(updated)
                    }}
                  />
                  <span className="flex-1 font-bold">{item.text}</span>
                  <Button
                    type="button"
                    variant="neutral"
                    size="sm"
                    onClick={() => removeChecklistItem(index)}
                    className="border-2 border-black p-1 text-xs shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none"
                  >
                    Remove
                  </Button>
                </div>
              ))}
              <div className="flex gap-2">
                <Input
                  value={newChecklistItem}
                  onChange={(e) => setNewChecklistItem(e.target.value)}
                  className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
                  placeholder="Add checklist item..."
                  onKeyPress={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      addChecklistItem()
                    }
                  }}
                />
                <Button
                  type="button"
                  onClick={addChecklistItem}
                  className="border-2 border-black bg-purple-400 font-bold uppercase shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:bg-purple-500 hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none"
                >
                  Add
                </Button>
              </div>
            </div>
          </div>

          {/* Recurring Pattern */}
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <Label htmlFor="recurrencePattern" className="font-bold uppercase">
                Recurring Pattern
              </Label>
              <Select
                defaultValue={task?.recurrencePattern || 'NONE'}
                onValueChange={(value) => setValue('recurrencePattern', value === 'NONE' ? undefined : value)}
              >
                <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  <SelectValue placeholder="Select pattern" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">None</SelectItem>
                  <SelectItem value="DAILY">Daily</SelectItem>
                  <SelectItem value="WEEKLY">Weekly</SelectItem>
                  <SelectItem value="MONTHLY">Monthly</SelectItem>
                  <SelectItem value="YEARLY">Yearly</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="recurrenceEndDate" className="font-bold uppercase">
                Recurring End Date
              </Label>
              <Input
                id="recurrenceEndDate"
                type="date"
                {...register('recurrenceEndDate')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Actions */}
      <div className="flex gap-4">
        <Button
          type="submit"
          disabled={isSubmitting}
          className="border-2 border-black bg-purple-400 px-8 py-3 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-purple-500 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
        >
          {isSubmitting ? 'Saving...' : task ? 'Update Task' : 'Create Task'}
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
