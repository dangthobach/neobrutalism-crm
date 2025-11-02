/**
 * Category Tree Component
 * Hierarchical category display with Neobrutalism styling
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
} from 'lucide-react'
import { ContentCategory } from '@/types/content'
import {
  useCreateCategory,
  useUpdateCategory,
  useDeleteCategory,
} from '@/hooks/useContentCategories'

interface CategoryTreeProps {
  categories: ContentCategory[]
}

interface TreeNode extends ContentCategory {
  children: TreeNode[]
}

export function CategoryTree({ categories }: CategoryTreeProps) {
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editingName, setEditingName] = useState('')
  const [addingParentId, setAddingParentId] = useState<string | null>(null)
  const [newName, setNewName] = useState('')

  const createCategory = useCreateCategory()
  const updateCategory = useUpdateCategory()
  const deleteCategory = useDeleteCategory()

  // Build tree structure
  const buildTree = (cats: ContentCategory[]): TreeNode[] => {
    const map = new Map<string, TreeNode>()
    const roots: TreeNode[] = []

    // Initialize nodes
    cats.forEach((cat) => {
      map.set(cat.id, { ...cat, children: [] })
    })

    // Build tree
    cats.forEach((cat) => {
      const node = map.get(cat.id)!
      if (cat.parentId) {
        const parent = map.get(cat.parentId)
        if (parent) {
          parent.children.push(node)
        } else {
          roots.push(node)
        }
      } else {
        roots.push(node)
      }
    })

    return roots
  }

  const tree = buildTree(categories)

  const toggleExpand = (id: string) => {
    const newExpanded = new Set(expandedIds)
    if (newExpanded.has(id)) {
      newExpanded.delete(id)
    } else {
      newExpanded.add(id)
    }
    setExpandedIds(newExpanded)
  }

  const handleEdit = (category: ContentCategory) => {
    setEditingId(category.id)
    setEditingName(category.name)
  }

  const handleSaveEdit = async (id: string) => {
    if (editingName.trim()) {
      await updateCategory.mutateAsync({
        id,
        data: { name: editingName },
      })
      setEditingId(null)
      setEditingName('')
    }
  }

  const handleCancelEdit = () => {
    setEditingId(null)
    setEditingName('')
  }

  const handleAdd = (parentId: string | null) => {
    setAddingParentId(parentId)
    setNewName('')
  }

  const handleSaveAdd = async () => {
    if (newName.trim()) {
      const slug = newName.toLowerCase().replace(/[^a-z0-9]+/g, '-')
      await createCategory.mutateAsync({
        name: newName,
        slug,
        parentId: addingParentId || undefined,
        displayOrder: 0,
        isActive: true,
      })
      setAddingParentId(null)
      setNewName('')
    }
  }

  const handleCancelAdd = () => {
    setAddingParentId(null)
    setNewName('')
  }

  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to delete this category?')) {
      await deleteCategory.mutateAsync(id)
    }
  }

  const renderNode = (node: TreeNode, level: number = 0) => {
    const isExpanded = expandedIds.has(node.id)
    const hasChildren = node.children.length > 0
    const isEditing = editingId === node.id
    const isAddingChild = addingParentId === node.id

    return (
      <div key={node.id} className="mb-1">
        <div
          className="group flex items-center gap-2 rounded border-2 border-black bg-white p-2 hover:bg-gray-50"
          style={{ marginLeft: `${level * 24}px` }}
        >
          {/* Expand/Collapse */}
          {hasChildren ? (
            <button
              onClick={() => toggleExpand(node.id)}
              className="hover:bg-gray-200 rounded p-1"
            >
              {isExpanded ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronRight className="h-4 w-4" />
              )}
            </button>
          ) : (
            <div className="w-6" />
          )}

          {/* Icon */}
          {isExpanded ? (
            <FolderOpen className="h-4 w-4 text-yellow-600" />
          ) : (
            <Folder className="h-4 w-4 text-yellow-600" />
          )}

          {/* Name */}
          {isEditing ? (
            <div className="flex flex-1 items-center gap-2">
              <Input
                value={editingName}
                onChange={(e) => setEditingName(e.target.value)}
                className="h-8"
                autoFocus
              />
              <Button size="sm" onClick={() => handleSaveEdit(node.id)}>
                Save
              </Button>
              <Button size="sm" variant="neutral" onClick={handleCancelEdit}>
                Cancel
              </Button>
            </div>
          ) : (
            <>
              <span className="flex-1 font-bold">{node.name}</span>
              {node.parentName && (
                <span className="text-xs text-muted-foreground">
                  in {node.parentName}
                </span>
              )}
              <span className="rounded border border-black bg-blue-100 px-2 py-0.5 text-xs font-mono">
                {node.displayOrder}
              </span>
              {!node.isActive && (
                <span className="rounded border border-black bg-red-200 px-2 py-0.5 text-xs font-bold">
                  Inactive
                </span>
              )}
            </>
          )}

          {/* Actions */}
          {!isEditing && (
            <div className="flex gap-1 opacity-0 group-hover:opacity-100">
              <Button
                size="sm"
                variant="neutral"
                onClick={() => handleAdd(node.id)}
              >
                <Plus className="h-3 w-3" />
              </Button>
              <Button
                size="sm"
                variant="neutral"
                onClick={() => handleEdit(node)}
              >
                <Edit className="h-3 w-3" />
              </Button>
              <Button
                size="sm"
                variant="neutral"
                onClick={() => handleDelete(node.id)}
              >
                <Trash2 className="h-3 w-3" />
              </Button>
            </div>
          )}
        </div>

        {/* Add child form */}
        {isAddingChild && (
          <div
            className="mt-1 flex items-center gap-2 rounded border-2 border-dashed border-black bg-green-50 p-2"
            style={{ marginLeft: `${(level + 1) * 24}px` }}
          >
            <Folder className="h-4 w-4 text-gray-400" />
            <Input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="New category name..."
              className="h-8"
              autoFocus
            />
            <Button size="sm" onClick={handleSaveAdd}>
              Add
            </Button>
            <Button size="sm" variant="neutral" onClick={handleCancelAdd}>
              Cancel
            </Button>
          </div>
        )}

        {/* Children */}
        {isExpanded && hasChildren && (
          <div className="mt-1">
            {node.children.map((child) => renderNode(child, level + 1))}
          </div>
        )}
      </div>
    )
  }

  return (
    <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
      <div className="flex items-center justify-between border-b-2 border-black bg-green-200 px-6 py-4">
        <h2 className="text-xl font-black">Category Tree</h2>
        <Button onClick={() => handleAdd(null)} size="sm">
          <Plus className="mr-2 h-4 w-4" />
          Add Root Category
        </Button>
      </div>
      <div className="p-4 space-y-1">
        {/* Add root form */}
        {addingParentId === null && (
          <div className="mb-2 flex items-center gap-2 rounded border-2 border-dashed border-black bg-yellow-50 p-2">
            <Folder className="h-4 w-4 text-gray-400" />
            <Input
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="New root category name..."
              className="h-8"
              autoFocus
            />
            <Button size="sm" onClick={handleSaveAdd}>
              Add
            </Button>
            <Button size="sm" variant="neutral" onClick={handleCancelAdd}>
              Cancel
            </Button>
          </div>
        )}

        {tree.length === 0 ? (
          <div className="flex h-32 items-center justify-center text-muted-foreground">
            <p>No categories yet. Add one to get started!</p>
          </div>
        ) : (
          tree.map((node) => renderNode(node))
        )}
      </div>
    </Card>
  )
}
