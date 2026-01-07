"use client"

import { useMemo, useState } from "react"
import { ColumnDef } from "@tanstack/react-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Checkbox } from "@/components/ui/checkbox"
import { Badge } from "@/components/ui/badge"
import { Shield, Code2, FileCode } from "lucide-react"
import {
  useMenuScreens,
  useCreateMenuScreen,
  useUpdateMenuScreen,
  useDeleteMenuScreen,
  useScreenApiEndpoints,
  useAssignApiEndpoints,
  MenuScreen,
  CreateMenuScreenRequest
} from "@/hooks/useMenuScreens"
import { useRootMenus } from "@/hooks/useMenus"
import { useTabsByMenu } from "@/hooks/useMenuTabs"
import { useApiEndpoints } from "@/hooks/useApiEndpoints"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { DataTable } from "@/components/ui/data-table-reusable"
import { PermissionGuard } from "@/components/auth/permission-guard"

export default function MenuScreensPage() {
  const [open, setOpen] = useState(false)
  const [apiDialog, setApiDialog] = useState(false)
  const [editing, setEditing] = useState<MenuScreen | null>(null)
  const [selectedScreen, setSelectedScreen] = useState<string | null>(null)
  const [selectedApis, setSelectedApis] = useState<string[]>([])
  const [formData, setFormData] = useState<Partial<CreateMenuScreenRequest>>({
    requiresPermission: true
  })

  const { data: menuScreensData, isLoading } = useMenuScreens()
  const { data: menus } = useRootMenus()
  const { data: allApiEndpoints } = useApiEndpoints()
  const { data: screenApis } = useScreenApiEndpoints(selectedScreen || '')
  const createMutation = useCreateMenuScreen()
  const updateMutation = useUpdateMenuScreen()
  const deleteMutation = useDeleteMenuScreen()
  const assignApisMutation = useAssignApiEndpoints()

  // Get tabs for selected menu
  const { data: tabs } = useTabsByMenu(formData.menuId || '')

  const menuScreens = menuScreensData?.content || []
  const apiEndpoints = allApiEndpoints?.content || []

  const getMenuName = (menuId?: string) => {
    if (!menuId) return '-'
    const menu = menus?.find(m => m.id === menuId)
    return menu?.name || menuId
  }

  const columns = useMemo<ColumnDef<MenuScreen>[]>(
    () => [
      {
        accessorKey: "code",
        header: "Code",
        cell: ({ row }) => (
          <code className="text-sm font-mono bg-secondary px-2 py-1 rounded">
            {row.original.code}
          </code>
        )
      },
      {
        accessorKey: "name",
        header: "Name",
        cell: ({ row }) => (
          <span className="font-medium">{row.original.name}</span>
        )
      },
      {
        accessorKey: "menuId",
        header: "Menu",
        cell: ({ row }) => row.original.menuId ? (
          <Badge variant="neutral">{getMenuName(row.original.menuId)}</Badge>
        ) : <span className="text-muted-foreground">-</span>
      },
      {
        accessorKey: "route",
        header: "Route",
        cell: ({ row }) => row.original.route ? (
          <code className="text-xs">{row.original.route}</code>
        ) : <span className="text-muted-foreground">-</span>
      },
      {
        accessorKey: "component",
        header: "Component",
        cell: ({ row }) => row.original.component ? (
          <div className="flex items-center gap-1">
            <FileCode className="h-3 w-3" />
            <span className="text-xs">{row.original.component}</span>
          </div>
        ) : <span className="text-muted-foreground">-</span>
      },
      {
        accessorKey: "requiresPermission",
        header: "Permission",
        cell: ({ row }) => row.original.requiresPermission ? (
          <Shield className="h-4 w-4 text-green-600" />
        ) : (
          <span className="text-gray-400">-</span>
        ),
      },
      {
        id: "actions",
        header: "Actions",
        cell: ({ row }) => (
          <div className="flex gap-2">
            <PermissionGuard routeOrCode="/menu-screens" permission="canEdit">
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => openApiDialog(row.original)}
              >
                APIs
              </Button>
            </PermissionGuard>
            <PermissionGuard routeOrCode="/menu-screens" permission="canEdit">
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => onEdit(row.original)}
              >
                Edit
              </Button>
            </PermissionGuard>
            <PermissionGuard routeOrCode="/menu-screens" permission="canDelete">
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => onDelete(row.original.id)}
                disabled={deleteMutation.isPending}
              >
                Delete
              </Button>
            </PermissionGuard>
          </div>
        ),
      },
    ],
    [deleteMutation.isPending, menus, getMenuName, onDelete]
  )

  function openApiDialog(screen: MenuScreen) {
    setSelectedScreen(screen.id)
    setApiDialog(true)
  }

  // When screen APIs are loaded, update selected APIs
  useState(() => {
    if (screenApis) {
      setSelectedApis(screenApis)
    }
  })

  function onEdit(item: MenuScreen) {
    setEditing(item)
    setFormData({
      code: item.code,
      name: item.name,
      menuId: item.menuId,
      tabId: item.tabId,
      route: item.route,
      component: item.component,
      requiresPermission: item.requiresPermission
    })
    setOpen(true)
  }

  function onDelete(id: string) {
    if (confirm('Are you sure you want to delete this menu screen?')) {
      deleteMutation.mutate(id)
    }
  }

  function onCreate() {
    setEditing(null)
    setFormData({
      code: '',
      name: '',
      requiresPermission: true
    })
    setOpen(true)
  }

  function handleSave() {
    if (!formData.code || !formData.name) {
      alert('Code and Name are required')
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
        formData as CreateMenuScreenRequest,
        {
          onSuccess: () => {
            setOpen(false)
          }
        }
      )
    }
  }

  function handleSaveApis() {
    if (!selectedScreen) return

    assignApisMutation.mutate(
      { screenId: selectedScreen, apiEndpointIds: selectedApis },
      {
        onSuccess: () => {
          setApiDialog(false)
          setSelectedScreen(null)
          setSelectedApis([])
        }
      }
    )
  }

  function toggleApi(apiId: string) {
    setSelectedApis(prev =>
      prev.includes(apiId)
        ? prev.filter(id => id !== apiId)
        : [...prev, apiId]
    )
  }

  return (
    <div className="space-y-4">
      <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
        <h1 className="text-2xl font-heading">Menu Screens</h1>
        <p className="text-sm mt-1">Manage screens and their API endpoint mappings</p>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <div className="flex justify-between items-center mb-4">
          <div className="text-sm">
            Total: <strong>{menuScreens.length}</strong> screens
          </div>
          <Button variant="noShadow" onClick={onCreate}>
            New Menu Screen
          </Button>
        </div>

        <DataTable
          columns={columns}
          data={menuScreens}
        />
      </div>

      {/* Create/Edit Dialog */}
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle className="font-heading">
              {editing ? "Edit Menu Screen" : "Create Menu Screen"}
            </DialogTitle>
          </DialogHeader>

          <div className="grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Code *</Label>
                <Input
                  placeholder="SCREEN_USER_LIST"
                  value={formData.code || ''}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                  disabled={!!editing}
                />
                <p className="text-xs text-muted-foreground">
                  Uppercase, use underscores or hyphens
                </p>
              </div>

              <div className="space-y-2">
                <Label>Name *</Label>
                <Input
                  placeholder="User List"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Menu (Optional)</Label>
                <Select
                  value={formData.menuId || ''}
                  onValueChange={(value) => setFormData({ ...formData, menuId: value || undefined, tabId: undefined })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select a menu" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">None</SelectItem>
                    {menus?.map(menu => (
                      <SelectItem key={menu.id} value={menu.id}>
                        {menu.icon && <span className="mr-2">{menu.icon}</span>}
                        {menu.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Tab (Optional)</Label>
                <Select
                  value={formData.tabId || ''}
                  onValueChange={(value) => setFormData({ ...formData, tabId: value || undefined })}
                  disabled={!formData.menuId}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select a tab" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">None</SelectItem>
                    {tabs?.map(tab => (
                      <SelectItem key={tab.id} value={tab.id}>
                        {tab.icon && <span className="mr-2">{tab.icon}</span>}
                        {tab.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Route</Label>
                <Input
                  placeholder="/admin/users"
                  value={formData.route || ''}
                  onChange={(e) => setFormData({ ...formData, route: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label>Component</Label>
                <Input
                  placeholder="UsersPage"
                  value={formData.component || ''}
                  onChange={(e) => setFormData({ ...formData, component: e.target.value })}
                />
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <Checkbox
                id="requiresPermission"
                checked={formData.requiresPermission}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, requiresPermission: checked as boolean })
                }
              />
              <Label htmlFor="requiresPermission" className="cursor-pointer">
                Requires permission to access
              </Label>
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

      {/* API Assignment Dialog */}
      <Dialog open={apiDialog} onOpenChange={setApiDialog}>
        <DialogContent className="max-w-4xl max-h-[80vh]">
          <DialogHeader>
            <DialogTitle className="font-heading flex items-center gap-2">
              <Code2 className="h-5 w-5" />
              Assign API Endpoints
            </DialogTitle>
          </DialogHeader>

          <div className="space-y-4 overflow-y-auto max-h-[50vh]">
            <div className="text-sm text-muted-foreground">
              Select API endpoints that this screen needs to access
            </div>

            <div className="space-y-2">
              {apiEndpoints.map((api) => (
                <div
                  key={api.id}
                  className="flex items-center space-x-3 p-3 border-2 border-black bg-secondary-background hover:bg-secondary/50 transition-colors"
                >
                  <Checkbox
                    id={api.id}
                    checked={selectedApis.includes(api.id)}
                    onCheckedChange={() => toggleApi(api.id)}
                  />
                  <Label htmlFor={api.id} className="flex-1 cursor-pointer">
                    <div className="flex items-center gap-2">
                      <Badge className={`bg-${api.method === 'GET' ? 'blue' : api.method === 'POST' ? 'green' : api.method === 'PUT' ? 'yellow' : api.method === 'DELETE' ? 'red' : 'gray'}-500 text-white border-2 border-black`}>
                        {api.method}
                      </Badge>
                      <code className="text-sm">{api.path}</code>
                      {api.tag && (
                        <Badge variant="neutral" className="text-xs">
                          {api.tag}
                        </Badge>
                      )}
                    </div>
                    {api.description && (
                      <p className="text-xs text-muted-foreground mt-1">
                        {api.description}
                      </p>
                    )}
                  </Label>
                </div>
              ))}

              {apiEndpoints.length === 0 && (
                <div className="text-center py-8 text-muted-foreground">
                  No API endpoints available. Create some first.
                </div>
              )}
            </div>
          </div>

          <DialogFooter>
            <div className="flex justify-between items-center w-full">
              <div className="text-sm text-muted-foreground">
                Selected: <strong>{selectedApis.length}</strong> endpoints
              </div>
              <div className="flex gap-2">
                <Button variant="noShadow" onClick={() => setApiDialog(false)}>
                  Cancel
                </Button>
                <Button
                  variant="noShadow"
                  onClick={handleSaveApis}
                  disabled={assignApisMutation.isPending}
                >
                  {assignApisMutation.isPending ? 'Saving...' : 'Save'}
                </Button>
              </div>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
