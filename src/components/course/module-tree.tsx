/**
 * Module Tree Component
 * Hierarchical module/lesson display with Neobrutalism styling
 */

'use client'

import { useState } from 'react'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  ChevronRight,
  ChevronDown,
  Plus,
  Edit,
  Trash2,
  FolderOpen,
  Folder,
  Clock,
  BookOpen,
  Lock,
} from 'lucide-react'
import type { CourseModule } from '@/types/course'
import {
  useCreateModule,
  useUpdateModule,
  useDeleteModule,
} from '@/hooks/useCourseModules'

interface ModuleTreeProps {
  modules: CourseModule[]
  courseId: string
}

export function ModuleTree({ modules, courseId }: ModuleTreeProps) {
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editingTitle, setEditingTitle] = useState('')
  const [editingDescription, setEditingDescription] = useState('')
  const [isAdding, setIsAdding] = useState(false)
  const [newTitle, setNewTitle] = useState('')
  const [newDescription, setNewDescription] = useState('')

  const createModule = useCreateModule()
  const updateModule = useUpdateModule()
  const deleteModule = useDeleteModule()

  const sortedModules = [...modules].sort((a, b) => a.displayOrder - b.displayOrder)

  const toggleExpand = (id: string) => {
    const newExpanded = new Set(expandedIds)
    if (newExpanded.has(id)) {
      newExpanded.delete(id)
    } else {
      newExpanded.add(id)
    }
    setExpandedIds(newExpanded)
  }

  const handleEdit = (module: CourseModule) => {
    setEditingId(module.id)
    setEditingTitle(module.title)
    setEditingDescription(module.description || '')
  }

  const handleSaveEdit = async (id: string) => {
    if (editingTitle.trim()) {
      await updateModule.mutateAsync({
        id,
        data: {
          title: editingTitle,
          description: editingDescription || undefined,
        },
      })
      setEditingId(null)
      setEditingTitle('')
      setEditingDescription('')
    }
  }

  const handleCancelEdit = () => {
    setEditingId(null)
    setEditingTitle('')
    setEditingDescription('')
  }

  const handleAdd = () => {
    setIsAdding(true)
    setNewTitle('')
    setNewDescription('')
  }

  const handleSaveAdd = async () => {
    if (newTitle.trim()) {
      await createModule.mutateAsync({
        courseId,
        title: newTitle,
        description: newDescription || undefined,
        displayOrder: modules.length,
        isLocked: false,
      })
      setIsAdding(false)
      setNewTitle('')
      setNewDescription('')
    }
  }

  const handleCancelAdd = () => {
    setIsAdding(false)
    setNewTitle('')
    setNewDescription('')
  }

  const handleDelete = async (module: CourseModule) => {
    if (window.confirm(`Delete module "${module.title}"? This will also delete all lessons in this module.`)) {
      await deleteModule.mutateAsync({
        id: module.id,
        courseId: module.courseId,
      })
    }
  }

  const formatDuration = (minutes: number) => {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    if (hours === 0) return `${mins}m`
    if (mins === 0) return `${hours}h`
    return `${hours}h ${mins}m`
  }

  const renderModule = (module: CourseModule) => {
    const isExpanded = expandedIds.has(module.id)
    const isEditing = editingId === module.id

    return (
      <div key={module.id} className="border-b-2 border-black last:border-b-0">
        <div className="group flex items-start gap-3 p-4 hover:bg-gray-50">
          {/* Expand/Collapse Icon */}
          <button
            onClick={() => toggleExpand(module.id)}
            className="mt-1 flex-shrink-0 transition-transform"
          >
            {isExpanded ? (
              <ChevronDown className="h-5 w-5" />
            ) : (
              <ChevronRight className="h-5 w-5" />
            )}
          </button>

          {/* Folder Icon */}
          {isExpanded ? (
            <FolderOpen className="mt-1 h-5 w-5 flex-shrink-0 text-yellow-600" />
          ) : (
            <Folder className="mt-1 h-5 w-5 flex-shrink-0 text-yellow-600" />
          )}

          {/* Module Content */}
          <div className="flex-1">
            {isEditing ? (
              /* Edit Mode */
              <div className="space-y-2">
                <Input
                  value={editingTitle}
                  onChange={(e) => setEditingTitle(e.target.value)}
                  className="border-2 border-black font-bold"
                  placeholder="Module title"
                  autoFocus
                />
                <Input
                  value={editingDescription}
                  onChange={(e) => setEditingDescription(e.target.value)}
                  className="border-2 border-black text-sm"
                  placeholder="Module description (optional)"
                />
                <div className="flex gap-2">
                  <Button
                    variant="default"
                    size="sm"
                    onClick={() => handleSaveEdit(module.id)}
                  >
                    Save
                  </Button>
                  <Button variant="neutral" size="sm" onClick={handleCancelEdit}>
                    Cancel
                  </Button>
                </div>
              </div>
            ) : (
              /* Display Mode */
              <div>
                <div className="flex items-start gap-2">
                  <div className="flex-1">
                    <h4 className="font-bold">{module.title}</h4>
                    {module.description && (
                      <p className="mt-1 text-sm text-muted-foreground">{module.description}</p>
                    )}
                  </div>

                  {/* Module Info Badges */}
                  <div className="flex flex-wrap gap-2">
                    <span className="rounded border-2 border-black bg-blue-100 px-2 py-1 font-mono text-xs font-bold">
                      #{module.displayOrder + 1}
                    </span>
                    {module.isLocked && (
                      <span className="flex items-center gap-1 rounded border-2 border-black bg-red-200 px-2 py-1 text-xs font-bold">
                        <Lock className="h-3 w-3" />
                        Locked
                      </span>
                    )}
                    <span className="flex items-center gap-1 rounded border-2 border-black bg-green-100 px-2 py-1 text-xs font-bold">
                      <BookOpen className="h-3 w-3" />
                      {module.lessonCount} lessons
                    </span>
                    {module.duration > 0 && (
                      <span className="flex items-center gap-1 rounded border-2 border-black bg-purple-100 px-2 py-1 text-xs font-bold">
                        <Clock className="h-3 w-3" />
                        {formatDuration(module.duration)}
                      </span>
                    )}
                  </div>
                </div>

                {/* Action Buttons (Show on Hover) */}
                <div className="mt-2 flex gap-2 opacity-0 transition-opacity group-hover:opacity-100">
                  <Button
                    variant="neutral"
                    size="sm"
                    onClick={() => handleEdit(module)}
                    className="gap-1"
                  >
                    <Edit className="h-3 w-3" />
                    Edit
                  </Button>
                  <Button
                    variant="neutral"
                    size="sm"
                    onClick={() => handleDelete(module)}
                    className="gap-1 bg-red-200 hover:bg-red-300"
                  >
                    <Trash2 className="h-3 w-3" />
                    Delete
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Expanded Content (Placeholder for lessons) */}
        {isExpanded && (
          <div className="border-t-2 border-black bg-gray-50 p-4">
            <p className="text-sm text-muted-foreground">
              Lessons for this module will be displayed here.
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              Manage lessons from the course curriculum page.
            </p>
          </div>
        )}
      </div>
    )
  }

  return (
    <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
      <div className="border-b-2 border-black bg-green-200 px-6 py-4">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-black">Course Modules</h3>
          <Button variant="default" size="sm" onClick={handleAdd} className="gap-1">
            <Plus className="h-4 w-4" />
            Add Module
          </Button>
        </div>
      </div>

      <div>
        {/* Add New Module Form */}
        {isAdding && (
          <div className="border-b-2 border-dashed border-black bg-yellow-50 p-4">
            <div className="space-y-2">
              <Input
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                className="border-2 border-black font-bold"
                placeholder="New module title"
                autoFocus
              />
              <Input
                value={newDescription}
                onChange={(e) => setNewDescription(e.target.value)}
                className="border-2 border-black text-sm"
                placeholder="Module description (optional)"
              />
              <div className="flex gap-2">
                <Button variant="default" size="sm" onClick={handleSaveAdd}>
                  Create Module
                </Button>
                <Button variant="neutral" size="sm" onClick={handleCancelAdd}>
                  Cancel
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Modules List */}
        {sortedModules.length === 0 ? (
          <div className="p-8 text-center">
            <p className="text-muted-foreground">No modules yet. Add your first module!</p>
          </div>
        ) : (
          <div>{sortedModules.map(renderModule)}</div>
        )}
      </div>
    </Card>
  )
}
