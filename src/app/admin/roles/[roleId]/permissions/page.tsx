"use client"

import { useState, useMemo, useEffect } from "react"
import { useParams, useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Loader2, ArrowLeft, Save, Copy, ChevronRight } from "lucide-react"
import { useRole } from "@/hooks/useRoles"
import { useMenus } from "@/hooks/useMenus"
import { useMenuPermissionsByRole, useBulkUpdateMenuPermissions } from "@/hooks/useRoleMenus"
import { Menu } from "@/lib/api/menus"
import { RoleMenuRequest } from "@/lib/api/role-menus"
import { Checkbox } from "@/components/ui/checkbox"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useRoles } from "@/hooks/useRoles"
import { useCopyPermissionsFromRole } from "@/hooks/useRoleMenus"

type PermissionState = {
  menuId: string
  canView: boolean
  canCreate: boolean
  canEdit: boolean
  canDelete: boolean
  canExport: boolean
  canImport: boolean
}

export default function RolePermissionsPage() {
  const params = useParams()
  const router = useRouter()
  const roleId = params?.roleId as string

  const [permissions, setPermissions] = useState<Map<string, PermissionState>>(new Map())
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())
  const [copyDialogOpen, setCopyDialogOpen] = useState(false)
  const [sourceRoleId, setSourceRoleId] = useState<string>("")
  const [hasChanges, setHasChanges] = useState(false)

  // Fetch role details
  const { data: role, isLoading: roleLoading } = useRole(roleId)

  // Fetch all menus
  const { data: menusData, isLoading: menusLoading } = useMenus({ page: 0, size: 1000 })

  // Fetch role's current permissions
  const { data: roleMenus, isLoading: permsLoading, refetch } = useMenuPermissionsByRole(roleId)

  // Fetch all roles for copy function
  const { data: allRolesData } = useRoles({ page: 0, size: 100 })
  const allRoles = allRolesData?.content || []

  // Mutations
  const bulkUpdateMutation = useBulkUpdateMenuPermissions()
  const copyMutation = useCopyPermissionsFromRole()

  const menus = menusData?.content || []

  // Initialize permissions from API data
  useEffect(() => {
    if (roleMenus) {
      const newPerms = new Map<string, PermissionState>()
      roleMenus.forEach(rm => {
        newPerms.set(rm.menuId, {
          menuId: rm.menuId,
          canView: rm.canView,
          canCreate: rm.canCreate,
          canEdit: rm.canEdit,
          canDelete: rm.canDelete,
          canExport: rm.canExport,
          canImport: rm.canImport,
        })
      })
      setPermissions(newPerms)
      setHasChanges(false)
    }
  }, [roleMenus])

  // Build tree structure
  const menuTree = useMemo(() => {
    const menuMap = new Map<string, Menu & { children: Menu[] }>()
    const roots: (Menu & { children: Menu[] })[] = []

    menus.forEach(menu => {
      menuMap.set(menu.id, { ...menu, children: [] })
    })

    menus.forEach(menu => {
      const node = menuMap.get(menu.id)!
      if (menu.parentId && menuMap.has(menu.parentId)) {
        menuMap.get(menu.parentId)!.children.push(node)
      } else {
        roots.push(node)
      }
    })

    const sortChildren = (items: (Menu & { children: Menu[] })[]) => {
      items.sort((a, b) => a.displayOrder - b.displayOrder)
      items.forEach(item => {
        if (item.children && item.children.length > 0) {
          sortChildren(item.children as (Menu & { children: Menu[] })[])
        }
      })
    }
    sortChildren(roots)

    return roots
  }, [menus])

  function toggleExpand(id: string) {
    const newExpanded = new Set(expandedIds)
    if (newExpanded.has(id)) {
      newExpanded.delete(id)
    } else {
      newExpanded.add(id)
    }
    setExpandedIds(newExpanded)
  }

  function updatePermission(menuId: string, field: keyof Omit<PermissionState, 'menuId'>, value: boolean) {
    const current = permissions.get(menuId) || {
      menuId,
      canView: false,
      canCreate: false,
      canEdit: false,
      canDelete: false,
      canExport: false,
      canImport: false,
    }

    // Auto-enable canView when any other permission is enabled
    let newPerm = { ...current, [field]: value }
    if (value && field !== 'canView') {
      newPerm.canView = true
    }

    // Auto-disable all other permissions when canView is disabled
    if (!value && field === 'canView') {
      newPerm = {
        menuId,
        canView: false,
        canCreate: false,
        canEdit: false,
        canDelete: false,
        canExport: false,
        canImport: false,
      }
    }

    const newPermissions = new Map(permissions)
    newPermissions.set(menuId, newPerm)
    setPermissions(newPermissions)
    setHasChanges(true)
  }

  async function handleSave() {
    // Convert permissions map to array, only include menus with at least canView enabled
    const permissionsArray: RoleMenuRequest[] = Array.from(permissions.values())
      .filter(p => p.canView) // Only save if at least view permission is granted
      .map(p => ({
        roleId,
        menuId: p.menuId,
        canView: p.canView,
        canCreate: p.canCreate,
        canEdit: p.canEdit,
        canDelete: p.canDelete,
        canExport: p.canExport,
        canImport: p.canImport,
      }))

    await bulkUpdateMutation.mutateAsync({ roleId, permissions: permissionsArray })
    setHasChanges(false)
    refetch()
  }

  async function handleCopyPermissions() {
    if (!sourceRoleId) return

    await copyMutation.mutateAsync({ targetRoleId: roleId, sourceRoleId })
    setCopyDialogOpen(false)
    setSourceRoleId("")
    refetch()
  }

  function renderMenuRow(menu: Menu & { children: Menu[] }, depth: number = 0): React.ReactNode {
    const hasChildren = menu.children.length > 0
    const isExpanded = expandedIds.has(menu.id)
    const indent = depth * 24
    const perm = permissions.get(menu.id) || {
      menuId: menu.id,
      canView: false,
      canCreate: false,
      canEdit: false,
      canDelete: false,
      canExport: false,
      canImport: false,
    }

    return (
      <>
        <TableRow key={menu.id} className="border-b-2 border-black hover:bg-secondary-background/50">
          <TableCell className="font-base" style={{ paddingLeft: `${12 + indent}px` }}>
            <div className="flex items-center gap-2">
              {hasChildren && (
                <button
                  onClick={() => toggleExpand(menu.id)}
                  className="p-1 hover:bg-secondary-background rounded"
                >
                  <ChevronRight
                    className={`h-4 w-4 transition-transform ${isExpanded ? 'rotate-90' : ''}`}
                  />
                </button>
              )}
              {!hasChildren && <div className="w-6" />}
              {menu.icon && <span className="text-lg">{menu.icon}</span>}
              <div>
                <div className="font-bold">{menu.name}</div>
                <div className="text-sm text-foreground/60">{menu.code}</div>
              </div>
            </div>
          </TableCell>
          <TableCell className="text-center">
            <Checkbox
              checked={perm.canView}
              onCheckedChange={(checked) => updatePermission(menu.id, 'canView', checked as boolean)}
            />
          </TableCell>
          <TableCell className="text-center">
            <Checkbox
              checked={perm.canCreate}
              onCheckedChange={(checked) => updatePermission(menu.id, 'canCreate', checked as boolean)}
              disabled={!perm.canView}
            />
          </TableCell>
          <TableCell className="text-center">
            <Checkbox
              checked={perm.canEdit}
              onCheckedChange={(checked) => updatePermission(menu.id, 'canEdit', checked as boolean)}
              disabled={!perm.canView}
            />
          </TableCell>
          <TableCell className="text-center">
            <Checkbox
              checked={perm.canDelete}
              onCheckedChange={(checked) => updatePermission(menu.id, 'canDelete', checked as boolean)}
              disabled={!perm.canView}
            />
          </TableCell>
          <TableCell className="text-center">
            <Checkbox
              checked={perm.canExport}
              onCheckedChange={(checked) => updatePermission(menu.id, 'canExport', checked as boolean)}
              disabled={!perm.canView}
            />
          </TableCell>
          <TableCell className="text-center">
            <Checkbox
              checked={perm.canImport}
              onCheckedChange={(checked) => updatePermission(menu.id, 'canImport', checked as boolean)}
              disabled={!perm.canView}
            />
          </TableCell>
        </TableRow>
        {isExpanded && menu.children.map(child => renderMenuRow(child as Menu & { children: Menu[] }, depth + 1))}
      </>
    )
  }

  if (roleLoading || menusLoading || permsLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    )
  }

  // Filter out the current role from copy options
  const availableRoles = allRoles.filter(r => r.id !== roleId)

  return (
    <div className="space-y-4">
      <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
        <div className="flex items-center gap-4">
          <Button
            variant="noShadow"
            size="sm"
            onClick={() => router.push("/admin/roles")}
            className="bg-background text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div className="flex-1">
            <h1 className="text-2xl font-heading">Menu Permissions</h1>
            <p className="text-sm font-base mt-1 opacity-90">
              {role?.name} ({role?.code})
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              onClick={() => setCopyDialogOpen(true)}
              className="bg-background text-foreground border-2 border-black shadow-[4px_4px_0_#000]"
              disabled={availableRoles.length === 0}
            >
              <Copy className="h-4 w-4 mr-2" />
              Copy from Role
            </Button>
            <Button
              onClick={handleSave}
              disabled={!hasChanges || bulkUpdateMutation.isPending}
              className="bg-green-500 text-white border-2 border-black shadow-[4px_4px_0_#000]"
            >
              {bulkUpdateMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Saving...
                </>
              ) : (
                <>
                  <Save className="h-4 w-4 mr-2" />
                  Save Changes
                </>
              )}
            </Button>
          </div>
        </div>
      </header>

      {hasChanges && (
        <div className="border-4 border-yellow-500 bg-yellow-100 p-4 shadow-[8px_8px_0_#000]">
          <p className="font-base text-yellow-900">
            ⚠️ You have unsaved changes. Click "Save Changes" to apply them.
          </p>
        </div>
      )}

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <h2 className="text-lg font-heading mb-4">Permission Matrix</h2>
        <p className="text-sm font-base text-foreground/60 mb-4">
          Configure what actions this role can perform on each menu item. View permission is required for all other permissions.
        </p>

        <div className="overflow-auto">
          <Table>
            <TableHeader className="font-heading">
              <TableRow className="bg-secondary-background border-b-2 border-black">
                <TableHead className="font-heading">Menu</TableHead>
                <TableHead className="font-heading text-center">View</TableHead>
                <TableHead className="font-heading text-center">Create</TableHead>
                <TableHead className="font-heading text-center">Edit</TableHead>
                <TableHead className="font-heading text-center">Delete</TableHead>
                <TableHead className="font-heading text-center">Export</TableHead>
                <TableHead className="font-heading text-center">Import</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {menuTree.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-8">
                    <p className="font-base text-foreground/60">No menus found</p>
                  </TableCell>
                </TableRow>
              ) : (
                menuTree.map(menu => renderMenuRow(menu, 0))
              )}
            </TableBody>
          </Table>
        </div>
      </div>

      <Dialog open={copyDialogOpen} onOpenChange={setCopyDialogOpen}>
        <DialogContent className="border-4 border-black shadow-[8px_8px_0_#000]">
          <DialogHeader>
            <DialogTitle className="font-heading text-2xl">Copy Permissions from Role</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p className="text-sm font-base text-foreground/70">
              This will copy all menu permissions from the selected role to <strong>{role?.name}</strong>.
              Existing permissions will be overwritten.
            </p>
            <div className="space-y-2">
              <Select value={sourceRoleId} onValueChange={setSourceRoleId}>
                <SelectTrigger className="border-2 border-black">
                  <SelectValue placeholder="Select a role to copy from" />
                </SelectTrigger>
                <SelectContent>
                  {availableRoles.map((r) => (
                    <SelectItem key={r.id} value={r.id}>
                      {r.name} ({r.code})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter className="gap-2">
            <Button
              variant="noShadow"
              onClick={() => {
                setCopyDialogOpen(false)
                setSourceRoleId("")
              }}
              className="border-2 border-black"
            >
              Cancel
            </Button>
            <Button
              variant="noShadow"
              onClick={handleCopyPermissions}
              disabled={!sourceRoleId || copyMutation.isPending}
              className="border-2 border-black bg-main text-main-foreground"
            >
              {copyMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Copying...
                </>
              ) : (
                "Copy Permissions"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
