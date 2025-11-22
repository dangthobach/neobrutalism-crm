/**
 * Task Edit/Create Modal
 * Full-featured form for creating and editing tasks
 */

"use client"

import React, { useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { X, Plus, Trash2, Calendar as CalendarIcon } from "lucide-react"
import { Task, TaskStatus, TaskPriority, TaskCategory } from "@/types/task"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Calendar } from "@/components/ui/calendar"
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover"
import { cn } from "@/lib/utils"
import { format } from "date-fns"

// Validation schema
const taskSchema = z.object({
  title: z.string().min(3, "Title must be at least 3 characters"),
  description: z.string().optional(),
  status: z.nativeEnum(TaskStatus),
  priority: z.nativeEnum(TaskPriority),
  category: z.nativeEnum(TaskCategory),
  customerId: z.string().optional(),
  contactId: z.string().optional(),
  assignedToId: z.string().optional(),
  dueDate: z.date().optional(),
  startDate: z.date().optional(),
  estimatedHours: z.number().min(0).optional(),
  tags: z.array(z.string()).optional(),
})

type TaskFormData = z.infer<typeof taskSchema>

interface TaskEditModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  task?: Task | null
  onSave: (data: TaskFormData) => void
  isLoading?: boolean
  customers?: Array<{ id: string; name: string }>
  users?: Array<{ id: string; name: string }>
}

export function TaskEditModal({
  open,
  onOpenChange,
  task,
  onSave,
  isLoading,
  customers = [],
  users = [],
}: TaskEditModalProps) {
  const isEditing = !!task

  const form = useForm<TaskFormData>({
    resolver: zodResolver(taskSchema),
    defaultValues: {
      title: "",
      description: "",
      status: TaskStatus.TODO,
      priority: TaskPriority.MEDIUM,
      category: TaskCategory.OTHER,
      tags: [],
    },
  })

  // Reset form when task changes
  useEffect(() => {
    if (task) {
      form.reset({
        title: task.title,
        description: task.description || "",
        status: task.status,
        priority: task.priority,
        category: task.category,
        customerId: task.customerId,
        contactId: task.contactId,
        assignedToId: task.assignedToId,
        dueDate: task.dueDate ? new Date(task.dueDate) : undefined,
        startDate: task.startDate ? new Date(task.startDate) : undefined,
        estimatedHours: task.estimatedHours,
        tags: task.tags || [],
      })
    } else {
      form.reset({
        title: "",
        description: "",
        status: TaskStatus.TODO,
        priority: TaskPriority.MEDIUM,
        category: TaskCategory.OTHER,
        tags: [],
      })
    }
  }, [task, form])

  const handleSubmit = (data: TaskFormData) => {
    onSave(data)
  }

  // Tag management
  const [tagInput, setTagInput] = React.useState("")
  const tags = form.watch("tags") || []

  const handleAddTag = () => {
    if (tagInput.trim() && !tags.includes(tagInput.trim())) {
      form.setValue("tags", [...tags, tagInput.trim()])
      setTagInput("")
    }
  }

  const handleRemoveTag = (tag: string) => {
    form.setValue(
      "tags",
      tags.filter((t) => t !== tag)
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto border-2 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
        <DialogHeader className="border-b-2 border-black pb-4 -mt-2 -mx-6 px-6 py-4 bg-yellow-200">
          <DialogTitle className="text-2xl font-black uppercase">
            {isEditing ? "Edit Task" : "Create New Task"}
          </DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Update task details and assignment"
              : "Fill in the task information below"}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6 mt-4">
            {/* Section 1: Basic Information */}
            <div className="space-y-4 border-2 border-black p-4 bg-yellow-50">
              <h3 className="font-black uppercase text-sm border-b-2 border-black pb-2">
                Basic Information
              </h3>

              <FormField
                control={form.control}
                name="title"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="font-bold">Title *</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="Enter task title"
                        className="border-2 border-black"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="description"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="font-bold">Description</FormLabel>
                    <FormControl>
                      <Textarea
                        placeholder="Enter task description"
                        className="border-2 border-black min-h-[100px]"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Section 2: Status & Priority */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-4 border-2 border-black p-4 bg-green-50">
                <h3 className="font-black uppercase text-sm border-b-2 border-black pb-2">
                  Status & Priority
                </h3>

                <FormField
                  control={form.control}
                  name="status"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="font-bold">Status *</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        defaultValue={field.value}
                      >
                        <FormControl>
                          <SelectTrigger className="border-2 border-black">
                            <SelectValue placeholder="Select status" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value={TaskStatus.TODO}>To Do</SelectItem>
                          <SelectItem value={TaskStatus.IN_PROGRESS}>
                            In Progress
                          </SelectItem>
                          <SelectItem value={TaskStatus.IN_REVIEW}>
                            In Review
                          </SelectItem>
                          <SelectItem value={TaskStatus.COMPLETED}>
                            Completed
                          </SelectItem>
                          <SelectItem value={TaskStatus.ON_HOLD}>
                            On Hold
                          </SelectItem>
                          <SelectItem value={TaskStatus.CANCELLED}>
                            Cancelled
                          </SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="priority"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="font-bold">Priority *</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        defaultValue={field.value}
                      >
                        <FormControl>
                          <SelectTrigger className="border-2 border-black">
                            <SelectValue placeholder="Select priority" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value={TaskPriority.LOW}>Low</SelectItem>
                          <SelectItem value={TaskPriority.MEDIUM}>
                            Medium
                          </SelectItem>
                          <SelectItem value={TaskPriority.HIGH}>High</SelectItem>
                          <SelectItem value={TaskPriority.URGENT}>
                            Urgent
                          </SelectItem>
                          <SelectItem value={TaskPriority.CRITICAL}>
                            Critical
                          </SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="category"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="font-bold">Category *</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        defaultValue={field.value}
                      >
                        <FormControl>
                          <SelectTrigger className="border-2 border-black">
                            <SelectValue placeholder="Select category" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value={TaskCategory.SALES}>Sales</SelectItem>
                          <SelectItem value={TaskCategory.SUPPORT}>
                            Support
                          </SelectItem>
                          <SelectItem value={TaskCategory.ONBOARDING}>
                            Onboarding
                          </SelectItem>
                          <SelectItem value={TaskCategory.FOLLOW_UP}>
                            Follow Up
                          </SelectItem>
                          <SelectItem value={TaskCategory.MEETING}>
                            Meeting
                          </SelectItem>
                          <SelectItem value={TaskCategory.RESEARCH}>
                            Research
                          </SelectItem>
                          <SelectItem value={TaskCategory.PROPOSAL}>
                            Proposal
                          </SelectItem>
                          <SelectItem value={TaskCategory.CONTRACT}>
                            Contract
                          </SelectItem>
                          <SelectItem value={TaskCategory.OTHER}>Other</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* Section 3: Assignment & Dates */}
              <div className="space-y-4 border-2 border-black p-4 bg-blue-50">
                <h3 className="font-black uppercase text-sm border-b-2 border-black pb-2">
                  Assignment & Dates
                </h3>

                <FormField
                  control={form.control}
                  name="assignedToId"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="font-bold">Assign To</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        defaultValue={field.value}
                      >
                        <FormControl>
                          <SelectTrigger className="border-2 border-black">
                            <SelectValue placeholder="Select user" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="unassigned">Unassigned</SelectItem>
                          {users.map((user) => (
                            <SelectItem key={user.id} value={user.id}>
                              {user.name}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="startDate"
                  render={({ field }) => (
                    <FormItem className="flex flex-col">
                      <FormLabel className="font-bold">Start Date</FormLabel>
                      <Popover>
                        <PopoverTrigger asChild>
                          <FormControl>
                            <Button
                              variant="noShadow"
                              className={cn(
                                "w-full border-2 border-black justify-start text-left font-normal",
                                !field.value && "text-muted-foreground"
                              )}
                            >
                              <CalendarIcon className="mr-2 h-4 w-4" />
                              {field.value ? (
                                format(field.value, "PPP")
                              ) : (
                                <span>Pick a date</span>
                              )}
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            mode="single"
                            selected={field.value}
                            onSelect={field.onChange}
                            initialFocus
                          />
                        </PopoverContent>
                      </Popover>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="dueDate"
                  render={({ field }) => (
                    <FormItem className="flex flex-col">
                      <FormLabel className="font-bold">Due Date</FormLabel>
                      <Popover>
                        <PopoverTrigger asChild>
                          <FormControl>
                            <Button
                              variant="noShadow"
                              className={cn(
                                "w-full border-2 border-black justify-start text-left font-normal",
                                !field.value && "text-muted-foreground"
                              )}
                            >
                              <CalendarIcon className="mr-2 h-4 w-4" />
                              {field.value ? (
                                format(field.value, "PPP")
                              ) : (
                                <span>Pick a date</span>
                              )}
                            </Button>
                          </FormControl>
                        </PopoverTrigger>
                        <PopoverContent className="w-auto p-0" align="start">
                          <Calendar
                            mode="single"
                            selected={field.value}
                            onSelect={field.onChange}
                            initialFocus
                          />
                        </PopoverContent>
                      </Popover>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="estimatedHours"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="font-bold">Estimated Hours</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min="0"
                          step="0.5"
                          placeholder="0"
                          className="border-2 border-black"
                          {...field}
                          onChange={(e) =>
                            field.onChange(
                              e.target.value ? parseFloat(e.target.value) : undefined
                            )
                          }
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </div>

            {/* Section 4: Related Entities & Tags */}
            <div className="space-y-4 border-2 border-black p-4 bg-purple-50">
              <h3 className="font-black uppercase text-sm border-b-2 border-black pb-2">
                Related Entities & Tags
              </h3>

              <FormField
                control={form.control}
                name="customerId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="font-bold">Related Customer</FormLabel>
                    <Select
                      onValueChange={field.onChange}
                      defaultValue={field.value}
                    >
                      <FormControl>
                        <SelectTrigger className="border-2 border-black">
                          <SelectValue placeholder="Select customer" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="none">None</SelectItem>
                        {customers.map((customer) => (
                          <SelectItem key={customer.id} value={customer.id}>
                            {customer.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <div>
                <FormLabel className="font-bold">Tags</FormLabel>
                <div className="flex gap-2 mt-2">
                  <Input
                    placeholder="Add tag and press Enter"
                    className="border-2 border-black"
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault()
                        handleAddTag()
                      }
                    }}
                  />
                  <Button
                    type="button"
                    variant="success"
                    onClick={handleAddTag}
                    className="shrink-0"
                  >
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
                {tags.length > 0 && (
                  <div className="flex flex-wrap gap-2 mt-3">
                    {tags.map((tag) => (
                      <Badge
                        key={tag}
                        variant="neutral"
                        className="text-sm flex items-center gap-1"
                      >
                        {tag}
                        <button
                          type="button"
                          onClick={() => handleRemoveTag(tag)}
                          className="ml-1 hover:text-red-600"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </Badge>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Actions */}
            <div className="flex justify-end gap-3 pt-4 border-t-2 border-black">
              <Button
                type="button"
                variant="neutral"
                onClick={() => onOpenChange(false)}
                disabled={isLoading}
              >
                Cancel
              </Button>
              <Button type="submit" variant="primary" disabled={isLoading}>
                {isLoading ? "Saving..." : isEditing ? "Update Task" : "Create Task"}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
