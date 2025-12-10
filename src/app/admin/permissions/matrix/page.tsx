'use client'

import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Checkbox } from '@/components/ui/checkbox'
import { permissionMatrixApi, PermissionMatrixDTO, PermissionUpdate } from '@/lib/api/permission-matrix'
import { RefreshCw, Save, Filter, Download } from 'lucide-react'
import { toast } from 'sonner'

export default function PermissionMatrixPage() {
  const [tenantId, setTenantId] = useState('default')
  const [selectedRole, setSelectedRole] = useState<string | null>(null)
  const [selectedResource, setSelectedResource] = useState<string | null>(null)
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false)
  const [pendingUpdate, setPendingUpdate] = useState<PermissionUpdate | null>(null)
  const [filterRole, setFilterRole] = useState('')
  const [filterResource, setFilterResource] = useState('')
  const [reason, setReason] = useState('')
  const queryClient = useQueryClient()

  // Fetch permission matrix
  const { data: matrix, isLoading, error } = useQuery({
    queryKey: ['permission-matrix', tenantId],
    queryFn: () => permissionMatrixApi.getPermissionMatrix(tenantId),
    enabled: !!tenantId,
  })

  // Refresh mutation
  const refreshMutation = useMutation({
    mutationFn: () => permissionMatrixApi.refreshMatrix(tenantId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['permission-matrix', tenantId] })
      toast.success('Permission matrix cache refreshed')
    },
    onError: () => {
      toast.error('Failed to refresh cache')
    },
  })

  // Update permission mutation
  const updateMutation = useMutation({
    mutationFn: (update: PermissionUpdate) =>
      permissionMatrixApi.updatePermission(tenantId, update),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['permission-matrix', tenantId] })
      setIsEditDialogOpen(false)
      setPendingUpdate(null)
      setReason('')
      toast.success('Permission updated successfully')
    },
    onError: () => {
      toast.error('Failed to update permission')
    },
  })

  // Filtered roles and resources
  const filteredRoles = useMemo(() => {
    if (!matrix) return []
    return matrix.roles.filter((role) =>
      role.roleCode.toLowerCase().includes(filterRole.toLowerCase()) ||
      role.roleName.toLowerCase().includes(filterRole.toLowerCase())
    )
  }, [matrix, filterRole])

  const filteredResources = useMemo(() => {
    if (!matrix) return []
    return matrix.resources.filter((resource) =>
      resource.path.toLowerCase().includes(filterResource.toLowerCase()) ||
      resource.name.toLowerCase().includes(filterResource.toLowerCase()) ||
      resource.category.toLowerCase().includes(filterResource.toLowerCase())
    )
  }, [matrix, filterResource])

  // Handle cell click
  const handleCellClick = (roleCode: string, resource: string) => {
    const currentActions = matrix?.matrix[roleCode]?.[resource] || []
    setPendingUpdate({
      roleCode,
      resource,
      actions: [...currentActions],
    })
    setSelectedRole(roleCode)
    setSelectedResource(resource)
    setIsEditDialogOpen(true)
  }

  // Handle save
  const handleSave = () => {
    if (!pendingUpdate) return
    updateMutation.mutate({
      ...pendingUpdate,
      reason: reason || 'Permission update via Permission Matrix UI',
    })
  }

  // Get available actions for a resource
  const getAvailableActions = (resourcePath: string): string[] => {
    const resource = matrix?.resources.find((r) => r.path === resourcePath)
    return resource?.availableActions || ['GET', 'POST', 'PUT', 'DELETE']
  }

  // Check if role has permission for resource/action
  const hasPermission = (roleCode: string, resource: string, action: string): boolean => {
    return matrix?.matrix[roleCode]?.[resource]?.includes(action) || false
  }

  if (error) {
    return (
      <div className="container mx-auto p-6">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center text-destructive">
              Failed to load permission matrix. Please try again.
            </div>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="container mx-auto p-6 space-y-6">
      {/* Header */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Permission Matrix</CardTitle>
              <p className="text-sm text-muted-foreground mt-1">
                View and manage permissions for all roles and resources
              </p>
            </div>
            <div className="flex gap-2">
              <Button
                variant="neutral"
                size="sm"
                onClick={() => refreshMutation.mutate()}
                disabled={refreshMutation.isPending}
              >
                <RefreshCw className={`h-4 w-4 mr-2 ${refreshMutation.isPending ? 'animate-spin' : ''}`} />
                Refresh Cache
              </Button>
              <Button variant="neutral" size="sm">
                <Download className="h-4 w-4 mr-2" />
                Export
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4">
            <div className="flex-1">
              <Label htmlFor="tenantId">Tenant ID</Label>
              <Input
                id="tenantId"
                value={tenantId}
                onChange={(e) => setTenantId(e.target.value)}
                placeholder="default"
              />
            </div>
            <div className="flex-1">
              <Label htmlFor="filterRole">Filter Role</Label>
              <Input
                id="filterRole"
                value={filterRole}
                onChange={(e) => setFilterRole(e.target.value)}
                placeholder="Search roles..."
              />
            </div>
            <div className="flex-1">
              <Label htmlFor="filterResource">Filter Resource</Label>
              <Input
                id="filterResource"
                value={filterResource}
                onChange={(e) => setFilterResource(e.target.value)}
                placeholder="Search resources..."
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Matrix Table */}
      {isLoading ? (
        <Card>
          <CardContent className="pt-6">
            <Skeleton className="h-[600px] w-full" />
          </CardContent>
        </Card>
      ) : matrix ? (
        <Card>
          <CardHeader>
            <CardTitle>Permission Matrix</CardTitle>
            <p className="text-sm text-muted-foreground">
              {filteredRoles.length} roles × {filteredResources.length} resources
            </p>
          </CardHeader>
          <CardContent>
            <ScrollArea className="h-[600px] w-full">
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader className="sticky top-0 bg-background z-10">
                    <TableRow>
                      <TableHead className="w-[200px] sticky left-0 bg-background z-20 border-r-2">
                        Role / Resource
                      </TableHead>
                      {filteredResources.map((resource) => (
                        <TableHead
                          key={resource.path}
                          className="text-center min-w-[150px]"
                          title={resource.path}
                        >
                          <div className="flex flex-col">
                            <span className="font-semibold">{resource.name}</span>
                            <span className="text-xs text-muted-foreground font-normal">
                              {resource.category}
                            </span>
                          </div>
                        </TableHead>
                      ))}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredRoles.map((role) => (
                      <TableRow key={role.roleCode}>
                        <TableCell className="sticky left-0 bg-background z-10 border-r-2 font-medium">
                          <div>
                            <div>{role.roleName}</div>
                            <div className="text-xs text-muted-foreground">{role.roleCode}</div>
                            {role.inherited && (
                              <Badge variant="outline" className="text-xs mt-1">
                                Inherited
                              </Badge>
                            )}
                          </div>
                        </TableCell>
                        {filteredResources.map((resource) => {
                          const permissions = matrix.matrix[role.roleCode]?.[resource.path] || []
                          const hasAnyPermission = permissions.length > 0

                          return (
                            <TableCell
                              key={`${role.roleCode}-${resource.path}`}
                              className={`text-center cursor-pointer hover:bg-secondary ${
                                hasAnyPermission ? 'bg-primary/5' : ''
                              }`}
                              onClick={() => handleCellClick(role.roleCode, resource.path)}
                            >
                              {hasAnyPermission ? (
                                <div className="flex flex-wrap gap-1 justify-center">
                                  {permissions.map((action) => (
                                    <Badge key={action} variant="default" className="text-xs">
                                      {action}
                                    </Badge>
                                  ))}
                                </div>
                              ) : (
                                <span className="text-muted-foreground">-</span>
                              )}
                            </TableCell>
                          )
                        })}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </ScrollArea>
          </CardContent>
        </Card>
      ) : null}

      {/* Edit Dialog */}
      <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Edit Permissions</DialogTitle>
            <DialogDescription>
              {selectedRole && selectedResource && (
                <>
                  Update permissions for <strong>{matrix?.roles.find((r) => r.roleCode === selectedRole)?.roleName}</strong> on{' '}
                  <strong>{selectedResource}</strong>
                </>
              )}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            {pendingUpdate && (
              <>
                <div>
                  <Label>Available Actions</Label>
                  <div className="grid grid-cols-4 gap-2 mt-2">
                    {getAvailableActions(pendingUpdate.resource).map((action) => {
                      const isChecked = pendingUpdate.actions.includes(action)
                      return (
                        <div key={action} className="flex items-center space-x-2">
                          <Checkbox
                            id={`action-${action}`}
                            checked={isChecked}
                            onCheckedChange={(checked) => {
                              if (checked) {
                                setPendingUpdate({
                                  ...pendingUpdate,
                                  actions: [...pendingUpdate.actions, action],
                                })
                              } else {
                                setPendingUpdate({
                                  ...pendingUpdate,
                                  actions: pendingUpdate.actions.filter((a) => a !== action),
                                })
                              }
                            }}
                          />
                          <Label
                            htmlFor={`action-${action}`}
                            className="cursor-pointer font-normal"
                          >
                            {action}
                          </Label>
                        </div>
                      )
                    })}
                  </div>
                </div>
                <div>
                  <Label htmlFor="reason">Reason (Optional)</Label>
                  <Textarea
                    id="reason"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Enter reason for this permission change..."
                    rows={3}
                  />
                </div>
              </>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleSave} disabled={updateMutation.isPending}>
              {updateMutation.isPending ? 'Saving...' : 'Save Changes'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

