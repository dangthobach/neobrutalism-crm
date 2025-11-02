"use client"

import { useState, useMemo } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Loader2, Trash2, Plus, Menu as MenuIcon, ChevronRight, Eye, EyeOff } from "lucide-react"
import { useMenus, useCreateMenu, useUpdateMenu, useDeleteMenu } from "@/hooks/useMenus"
import { Menu } from "@/lib/api/menus"
import { Checkbox } from "@/components/ui/checkbox"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

type MenuFormData = Omit<Menu, 'id' | 'deleted' | 'createdAt' | 'path' | 'level'> & { id?: string }

export default function MenusPage() {
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<MenuFormData | null>(null)
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())

  // Fetch all menus
  const { data: menusData, isLoading, error, refetch } = useMenus({ page: 0, size: 1000 })

  // Mutations
  const createMutation = useCreateMenu()
  const updateMutation = useUpdateMenu()
  const deleteMutation = useDeleteMenu()

  const menus = menusData?.content || []

  // Build tree structure
  const menuTree = useMemo(() => {
    const menuMap = new Map<string, Menu & { children: Menu[] }>()
    const roots: (Menu & { children: Menu[] })[] = []

    // First pass: create map
    menus.forEach(menu => {
      menuMap.set(menu.id, { ...menu, children: [] })
    })

    // Second pass: build tree
    menus.forEach(menu => {
      const node = menuMap.get(menu.id)!
      if (menu.parentId && menuMap.has(menu.parentId)) {
        menuMap.get(menu.parentId)!.children.push(node)
      } else {
        roots.push(node)
      }
    })

    // Sort by displayOrder
    const sortChildren = (items: (Menu & { children: Menu[] })[]) => {
      items.sort((a, b) => a.displayOrder - b.displayOrder)
      items.forEach(item => sortChildren(item.children))
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

  function onCreate(parentId?: string) {
    setEditing({
      code: "",
      name: "",
      icon: "",
      parentId: parentId || undefined,
      route: "",
      displayOrder: 0,
      isVisible: true,
      requiresAuth: true,
    })
    setOpen(true)
  }

  function onEdit(menu: Menu) {
    setEditing({
      id: menu.id,
      code: menu.code,
      name: menu.name,
      icon: menu.icon,
      parentId: menu.parentId,
      route: menu.route,
      displayOrder: menu.displayOrder,
      isVisible: menu.isVisible,
      requiresAuth: menu.requiresAuth,
    })
    setOpen(true)
  }

  async function saveMenu() {
    if (!editing) return

    if (editing.id) {
      await updateMutation.mutateAsync({
        id: editing.id,
        data: {
          code: editing.code,
          name: editing.name,
          icon: editing.icon || undefined,
          parentId: editing.parentId || undefined,
          route: editing.route || undefined,
          displayOrder: editing.displayOrder,
          isVisible: editing.isVisible,
          requiresAuth: editing.requiresAuth,
        },
      })
    } else {
      await createMutation.mutateAsync({
        code: editing.code,
        name: editing.name,
        icon: editing.icon || undefined,
        parentId: editing.parentId || undefined,
        route: editing.route || undefined,
        displayOrder: editing.displayOrder,
        isVisible: editing.isVisible,
        requiresAuth: editing.requiresAuth,
      })
    }

    setOpen(false)
    setEditing(null)
    refetch()
  }

  async function onDelete(id: string) {
    if (!confirm("Are you sure you want to delete this menu?")) return
    await deleteMutation.mutateAsync(id)
    refetch()
  }

  function renderMenuRow(menu: Menu & { children: Menu[] }, depth: number = 0) {
    const hasChildren = menu.children.length > 0
    const isExpanded = expandedIds.has(menu.id)
    const indent = depth * 24

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
          <TableCell className="font-base text-sm">{menu.route || '-'}</TableCell>
          <TableCell>
            <span className="px-2 py-1 border-2 border-black text-xs font-base bg-blue-100">
              {menu.displayOrder}
            </span>
          </TableCell>
          <TableCell>
            {menu.isVisible ? (
              <Eye className="h-4 w-4 text-green-600" />
            ) : (
              <EyeOff className="h-4 w-4 text-gray-400" />
            )}
          </TableCell>
          <TableCell>
            {menu.requiresAuth ? (
              <span className="px-2 py-1 border-2 border-black text-xs font-base bg-red-100">AUTH</span>
            ) : (
              <span className="px-2 py-1 border-2 border-black text-xs font-base bg-green-100">PUBLIC</span>
            )}
          </TableCell>
          <TableCell>
            <div className="flex gap-2">
              <Button variant="noShadow" size="sm" onClick={() => onCreate(menu.id)}>
                <Plus className="h-3 w-3" />
              </Button>
              <Button variant="noShadow" size="sm" onClick={() => onEdit(menu)}>
                Edit
              </Button>
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => onDelete(menu.id)}
                disabled={deleteMutation.isPending || hasChildren}
                className="bg-red-500 text-white border-2 border-black disabled:opacity-50"
                title={hasChildren ? "Cannot delete menu with children" : "Delete"}
              >
                <Trash2 className="h-3 w-3" />
              </Button>
            </div>
          </TableCell>
        </TableRow>
        {isExpanded && menu.children.map(child => renderMenuRow(child, depth + 1))}
      </>
    )
  }

  if (error) {
    return (
      <div className="space-y-4">
        <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
          <h1 className="text-2xl font-heading">Menus</h1>
        </header>
        <div className="border-4 border-black bg-background p-12 shadow-[8px_8px_0_#000] text-center">
          <p className="text-lg font-base text-red-500">Error loading menus: {(error as Error).message}</p>
          <Button onClick={() => refetch()} className="mt-4">Retry</Button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-heading">Menu Management</h1>
            <p className="text-sm font-base mt-1 opacity-90">
              Manage hierarchical menu structure
            </p>
          </div>
          <Button onClick={() => onCreate()} className="bg-background text-foreground border-2 border-black hover:translate-x-1 hover:translate-y-1 transition-all shadow-[4px_4px_0_#000]">
            <MenuIcon className="h-4 w-4 mr-2" />
            Add Root Menu
          </Button>
        </div>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        {isLoading ? (
          <div className="text-center py-12">
            <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
            <p className="text-lg font-base">Loading menus...</p>
          </div>
        ) : (
          <div className="overflow-auto">
            <Table>
              <TableHeader className="font-heading">
                <TableRow className="bg-secondary-background border-b-2 border-black">
                  <TableHead className="font-heading">Menu Name</TableHead>
                  <TableHead className="font-heading">Route</TableHead>
                  <TableHead className="font-heading">Order</TableHead>
                  <TableHead className="font-heading">Visible</TableHead>
                  <TableHead className="font-heading">Auth</TableHead>
                  <TableHead className="font-heading">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {menuTree.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8">
                      <MenuIcon className="h-12 w-12 mx-auto mb-4 text-foreground/30" />
                      <p className="font-base text-foreground/60">No menus found</p>
                    </TableCell>
                  </TableRow>
                ) : (
                  menuTree.map(menu => renderMenuRow(menu, 0))
                )}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="border-4 border-black shadow-[8px_8px_0_#000] max-w-2xl">
          <DialogHeader>
            <DialogTitle className="font-heading text-2xl">
              {editing?.id ? "Edit Menu" : "Create Menu"}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="code" className="font-base">Code *</Label>
                <Input
                  id="code"
                  placeholder="MENU_CODE"
                  value={editing?.code ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as MenuFormData), code: e.target.value.toUpperCase() }))}
                  className="border-2 border-black"
                  disabled={!!editing?.id}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="name" className="font-base">Name *</Label>
                <Input
                  id="name"
                  placeholder="Menu Name"
                  value={editing?.name ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as MenuFormData), name: e.target.value }))}
                  className="border-2 border-black"
                />
              </div>
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="icon" className="font-base">Icon (emoji)</Label>
                <Input
                  id="icon"
                  placeholder="📋"
                  value={editing?.icon ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as MenuFormData), icon: e.target.value }))}
                  className="border-2 border-black"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="route" className="font-base">Route</Label>
                <Input
                  id="route"
                  placeholder="/admin/example"
                  value={editing?.route ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as MenuFormData), route: e.target.value }))}
                  className="border-2 border-black"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="displayOrder" className="font-base">Display Order</Label>
              <Input
                id="displayOrder"
                type="number"
                placeholder="0"
                value={editing?.displayOrder ?? 0}
                onChange={(e) => setEditing((u) => ({ ...(u as MenuFormData), displayOrder: Number(e.target.value) }))}
                className="border-2 border-black"
              />
            </div>
            <div className="flex gap-4">
              <div className="flex items-center space-x-2">
                <Checkbox
                  id="isVisible"
                  checked={editing?.isVisible ?? true}
                  onCheckedChange={(checked) => setEditing((u) => ({ ...(u as MenuFormData), isVisible: checked as boolean }))}
                />
                <Label htmlFor="isVisible" className="font-base cursor-pointer">
                  Visible
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <Checkbox
                  id="requiresAuth"
                  checked={editing?.requiresAuth ?? true}
                  onCheckedChange={(checked) => setEditing((u) => ({ ...(u as MenuFormData), requiresAuth: checked as boolean }))}
                />
                <Label htmlFor="requiresAuth" className="font-base cursor-pointer">
                  Requires Authentication
                </Label>
              </div>
            </div>
          </div>
          <DialogFooter className="gap-2">
            <Button
              variant="noShadow"
              onClick={() => {
                setOpen(false)
                setEditing(null)
              }}
              className="border-2 border-black"
            >
              Cancel
            </Button>
            <Button
              variant="noShadow"
              onClick={saveMenu}
              disabled={createMutation.isPending || updateMutation.isPending}
              className="border-2 border-black bg-main text-main-foreground"
            >
              {(createMutation.isPending || updateMutation.isPending) ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Saving...
                </>
              ) : (
                "Save"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
