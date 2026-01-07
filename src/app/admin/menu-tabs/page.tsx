"use client"

import { useMemo, useState } from "react"
import { ColumnDef } from "@tanstack/react-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Checkbox } from "@/components/ui/checkbox"
import { Badge } from "@/components/ui/badge"
import { Eye, EyeOff, ArrowUpDown } from "lucide-react"
import {
  useMenuTabs,
  useCreateMenuTab,
  useUpdateMenuTab,
  useDeleteMenuTab,
  MenuTab,
  CreateMenuTabRequest
} from "@/hooks/useMenuTabs"
import { useRootMenus } from "@/hooks/useMenus"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { DataTable } from "@/components/ui/data-table-reusable"
import { PermissionGuard } from "@/components/auth/permission-guard"

export default function MenuTabsPage() {
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<MenuTab | null>(null)
  const [formData, setFormData] = useState<Partial<CreateMenuTabRequest>>({
    isVisible: true,
    displayOrder: 0
  })

  const { data: menuTabsData, isLoading } = useMenuTabs()
  const { data: menus } = useRootMenus()
  const createMutation = useCreateMenuTab()
  const updateMutation = useUpdateMenuTab()
  const deleteMutation = useDeleteMenuTab()

  const menuTabs = menuTabsData?.content || []

  const getMenuName = (menuId: string) => {
    const menu = menus?.find(m => m.id === menuId)
    return menu?.name || menuId
  }

  const columns = useMemo<ColumnDef<MenuTab>[]>(
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
          <div className="flex items-center gap-2">
            {row.original.icon && <span>{row.original.icon}</span>}
            <span className="font-medium">{row.original.name}</span>
          </div>
        )
      },
      {
        accessorKey: "menuId",
        header: "Menu",
        cell: ({ row }) => (
          <Badge variant="neutral">
            {getMenuName(row.original.menuId)}
          </Badge>
        )
      },
      {
        accessorKey: "displayOrder",
        header: () => (
          <div className="flex items-center gap-1">
            <ArrowUpDown className="h-4 w-4" />
            Order
          </div>
        ),
        cell: ({ row }) => (
          <Badge variant="neutral">{row.original.displayOrder}</Badge>
        )
      },
      {
        accessorKey: "isVisible",
        header: "Visible",
        cell: ({ row }) => row.original.isVisible ? (
          <Eye className="h-4 w-4 text-green-600" />
        ) : (
          <EyeOff className="h-4 w-4 text-gray-400" />
        ),
      },
      {
        id: "actions",
        header: "Actions",
        cell: ({ row }) => (
          <div className="flex gap-2">
            <PermissionGuard routeOrCode="/menu-tabs" permission="canEdit">
              <Button variant="noShadow" size="sm" onClick={() => onEdit(row.original)}>
                Edit
              </Button>
            </PermissionGuard>
            <PermissionGuard routeOrCode="/menu-tabs" permission="canDelete">
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

  function onEdit(item: MenuTab) {
    setEditing(item)
    setFormData({
      code: item.code,
      name: item.name,
      menuId: item.menuId,
      icon: item.icon,
      displayOrder: item.displayOrder,
      isVisible: item.isVisible
    })
    setOpen(true)
  }

  function onDelete(id: string) {
    if (confirm('Are you sure you want to delete this menu tab?')) {
      deleteMutation.mutate(id)
    }
  }

  function onCreate() {
    setEditing(null)
    setFormData({
      code: '',
      name: '',
      menuId: '',
      displayOrder: 0,
      isVisible: true
    })
    setOpen(true)
  }

  function handleSave() {
    if (!formData.code || !formData.name || !formData.menuId) {
      alert('Code, Name, and Menu are required')
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
        formData as CreateMenuTabRequest,
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
        <h1 className="text-2xl font-heading">Menu Tabs</h1>
        <p className="text-sm mt-1">Manage tabs within menus for better organization</p>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <div className="flex justify-between items-center mb-4">
          <div className="text-sm">
            Total: <strong>{menuTabs.length}</strong> tabs
          </div>
          <Button variant="noShadow" onClick={onCreate}>
            New Menu Tab
          </Button>
        </div>

        <DataTable
          columns={columns}
          data={menuTabs}
        />
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="font-heading">
              {editing ? "Edit Menu Tab" : "Create Menu Tab"}
            </DialogTitle>
          </DialogHeader>

          <div className="grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Code *</Label>
                <Input
                  placeholder="TAB_GENERAL"
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
                  placeholder="General Information"
                  value={formData.name || ''}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label>Parent Menu *</Label>
              <Select
                value={formData.menuId}
                onValueChange={(value) => setFormData({ ...formData, menuId: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select a menu" />
                </SelectTrigger>
                <SelectContent>
                  {menus?.map(menu => (
                    <SelectItem key={menu.id} value={menu.id}>
                      {menu.icon && <span className="mr-2">{menu.icon}</span>}
                      {menu.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Icon (Emoji or Icon Name)</Label>
                <Input
                  placeholder="📋 or IconName"
                  value={formData.icon || ''}
                  onChange={(e) => setFormData({ ...formData, icon: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label>Display Order</Label>
                <Input
                  type="number"
                  min="0"
                  value={formData.displayOrder || 0}
                  onChange={(e) => setFormData({ ...formData, displayOrder: parseInt(e.target.value) })}
                />
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <Checkbox
                id="isVisible"
                checked={formData.isVisible}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, isVisible: checked as boolean })
                }
              />
              <Label htmlFor="isVisible" className="cursor-pointer">
                Visible to users
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
    </div>
  )
}
