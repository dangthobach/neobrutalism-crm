"use client"

import { useMemo, useState } from "react"
import { ColumnDef } from "@tanstack/react-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Checkbox } from "@/components/ui/checkbox"
import { Badge } from "@/components/ui/badge"
import {
  useApiEndpoints,
  useCreateApiEndpoint,
  useUpdateApiEndpoint,
  useDeleteApiEndpoint,
  ApiEndpoint,
  HttpMethod,
  CreateApiEndpointRequest
} from "@/hooks/useApiEndpoints"
import { DataTable } from "@/components/ui/data-table-reusable"

const HTTP_METHODS: HttpMethod[] = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS']

const METHOD_COLORS: Record<HttpMethod, string> = {
  GET: 'bg-blue-500',
  POST: 'bg-green-500',
  PUT: 'bg-yellow-500',
  DELETE: 'bg-red-500',
  PATCH: 'bg-purple-500',
  HEAD: 'bg-gray-500',
  OPTIONS: 'bg-pink-500'
}

export default function ApiEndpointsPage() {
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<ApiEndpoint | null>(null)
  const [formData, setFormData] = useState<Partial<CreateApiEndpointRequest>>({
    method: 'GET',
    requiresAuth: true,
    isPublic: false
  })

  const { data: apiEndpointsData, isLoading } = useApiEndpoints()
  const createMutation = useCreateApiEndpoint()
  const updateMutation = useUpdateApiEndpoint()
  const deleteMutation = useDeleteApiEndpoint()

  const apiEndpoints = apiEndpointsData?.content || []

  const columns = useMemo<ColumnDef<ApiEndpoint>[]>(
    () => [
      {
        accessorKey: "method",
        header: "Method",
        cell: ({ row }) => (
          <Badge className={`${METHOD_COLORS[row.original.method]} text-white border-2 border-black`}>
            {row.original.method}
          </Badge>
        ),
      },
      {
        accessorKey: "path",
        header: "Path",
        cell: ({ row }) => <code className="text-sm">{row.original.path}</code>
      },
      {
        accessorKey: "tag",
        header: "Tag",
        cell: ({ row }) => row.original.tag ? (
          <Badge variant="neutral">{row.original.tag}</Badge>
        ) : <span className="text-muted-foreground">-</span>
      },
      {
        accessorKey: "description",
        header: "Description",
        cell: ({ row }) => (
          <span className="text-sm">{row.original.description || '-'}</span>
        )
      },
      {
        accessorKey: "requiresAuth",
        header: "Auth",
        cell: ({ row }) => (
          <Badge variant={row.original.requiresAuth ? "default" : "neutral"}>
            {row.original.requiresAuth ? "Required" : "Optional"}
          </Badge>
        ),
      },
      {
        accessorKey: "isPublic",
        header: "Public",
        cell: ({ row }) => (
          <Badge variant={row.original.isPublic ? "default" : "neutral"}>
            {row.original.isPublic ? "Yes" : "No"}
          </Badge>
        ),
      },
      {
        id: "actions",
        header: "Actions",
        cell: ({ row }) => (
          <div className="flex gap-2">
            <Button variant="noShadow" size="sm" onClick={() => onEdit(row.original)}>
              Edit
            </Button>
            <Button
              variant="noShadow"
              size="sm"
              onClick={() => onDelete(row.original.id)}
              disabled={deleteMutation.isPending}
            >
              Delete
            </Button>
          </div>
        ),
      },
    ],
    [deleteMutation.isPending]
  )

  function onEdit(item: ApiEndpoint) {
    setEditing(item)
    setFormData({
      method: item.method,
      path: item.path,
      tag: item.tag,
      description: item.description,
      requiresAuth: item.requiresAuth,
      isPublic: item.isPublic
    })
    setOpen(true)
  }

  function onDelete(id: string) {
    if (confirm('Are you sure you want to delete this API endpoint?')) {
      deleteMutation.mutate(id)
    }
  }

  function onCreate() {
    setEditing(null)
    setFormData({
      method: 'GET',
      path: '',
      requiresAuth: true,
      isPublic: false
    })
    setOpen(true)
  }

  function handleSave() {
    if (!formData.method || !formData.path) {
      alert('Method and Path are required')
      return
    }

    if (editing) {
      updateMutation.mutate(
        { id: editing.id, data: formData },
        {
          onSuccess: () => {
            setOpen(false)
            setEditing(null)
          }
        }
      )
    } else {
      createMutation.mutate(
        formData as CreateApiEndpointRequest,
        {
          onSuccess: () => {
            setOpen(false)
          }
        }
      )
    }
  }

  return (
    <div className="space-y-4">
      <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
        <h1 className="text-2xl font-heading">API Endpoints</h1>
        <p className="text-sm mt-1">Manage REST API endpoints and their access control</p>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <div className="flex justify-between items-center mb-4">
          <div className="text-sm">
            Total: <strong>{apiEndpoints.length}</strong> endpoints
          </div>
          <Button variant="noShadow" onClick={onCreate}>
            New API Endpoint
          </Button>
        </div>

        <DataTable
          columns={columns}
          data={apiEndpoints}
          // searchColumn="path"
          // searchPlaceholder="Search by path..."
          // isLoading={isLoading}
        />
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="font-heading">
              {editing ? "Edit API Endpoint" : "Create API Endpoint"}
            </DialogTitle>
          </DialogHeader>

          <div className="grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>HTTP Method *</Label>
                <Select
                  value={formData.method}
                  onValueChange={(value: HttpMethod) => setFormData({ ...formData, method: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {HTTP_METHODS.map(method => (
                      <SelectItem key={method} value={method}>
                        {method}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Tag</Label>
                <Input
                  placeholder="e.g., Users, Organizations"
                  value={formData.tag || ''}
                  onChange={(e) => setFormData({ ...formData, tag: e.target.value })}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>Path *</Label>
              <Input
                placeholder="/api/users/{id}"
                value={formData.path || ''}
                onChange={(e) => setFormData({ ...formData, path: e.target.value })}
              />
              <p className="text-xs text-muted-foreground">
                Use path variables like {'{id}'} for dynamic segments
              </p>
            </div>

            <div className="space-y-2">
              <Label>Description</Label>
              <Input
                placeholder="Brief description of what this endpoint does"
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </div>

            <div className="flex gap-6">
              <div className="flex items-center space-x-2">
                <Checkbox
                  id="requiresAuth"
                  checked={formData.requiresAuth}
                  onCheckedChange={(checked) =>
                    setFormData({ ...formData, requiresAuth: checked as boolean })
                  }
                />
                <Label htmlFor="requiresAuth" className="cursor-pointer">
                  Requires Authentication
                </Label>
              </div>

              <div className="flex items-center space-x-2">
                <Checkbox
                  id="isPublic"
                  checked={formData.isPublic}
                  onCheckedChange={(checked) =>
                    setFormData({ ...formData, isPublic: checked as boolean })
                  }
                />
                <Label htmlFor="isPublic" className="cursor-pointer">
                  Public Access
                </Label>
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="noShadow" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="noShadow"
              onClick={handleSave}
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
