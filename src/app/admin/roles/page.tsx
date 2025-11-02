"use client"

import { useMemo, useState } from "react"
import { ColumnDef, ColumnFiltersState, getCoreRowModel, getFilteredRowModel, getPaginationRowModel, getSortedRowModel, SortingState, PaginationState, useReactTable, flexRender } from "@tanstack/react-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { ArrowUpDown, Trash2, Search, Loader2, PlayCircle, PauseCircle, Shield, Settings } from "lucide-react"
import { useRoles, useCreateRole, useUpdateRole, useDeleteRole, useActivateRole, useSuspendRole } from "@/hooks/useRoles"
import { Role, RoleStatus } from "@/lib/api/roles"

type RoleFormData = Omit<Role, 'id' | 'deleted' | 'createdAt' | 'updatedAt' | 'createdBy' | 'updatedBy'> & { id?: string }

export default function RolesPage() {
  const [globalFilter, setGlobalFilter] = useState("")
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<RoleFormData | null>(null)
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [sorting, setSorting] = useState<SortingState>([{ id: "name", desc: false }])
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 10 })

  // Fetch roles with React Query
  const { data: rolesData, isLoading, error, refetch } = useRoles({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sortBy: sorting[0]?.id || "name",
    sortDirection: sorting[0]?.desc ? "DESC" : "ASC",
  })

  // Mutations
  const createMutation = useCreateRole()
  const updateMutation = useUpdateRole()
  const deleteMutation = useDeleteRole()
  const activateMutation = useActivateRole()
  const suspendMutation = useSuspendRole()

  const roles = rolesData?.content || []
  const totalPages = rolesData?.totalPages || 0

  const columns = useMemo<ColumnDef<Role>[]>(
    () => [
      {
        accessorKey: "code",
        header: ({ column }) => {
          return (
            <button
              onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
              className="flex items-center hover:opacity-80 p-0 h-auto font-heading bg-transparent border-0 cursor-pointer"
            >
              Code
              <ArrowUpDown className="ml-2 h-4 w-4" />
            </button>
          )
        },
      },
      {
        accessorKey: "name",
        header: ({ column }) => {
          return (
            <button
              onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
              className="flex items-center hover:opacity-80 p-0 h-auto font-heading bg-transparent border-0 cursor-pointer"
            >
              Name
              <ArrowUpDown className="ml-2 h-4 w-4" />
            </button>
          )
        },
      },
      {
        accessorKey: "description",
        header: "Description",
      },
      {
        accessorKey: "priority",
        header: ({ column }) => {
          return (
            <button
              onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
              className="flex items-center hover:opacity-80 p-0 h-auto font-heading bg-transparent border-0 cursor-pointer"
            >
              Priority
              <ArrowUpDown className="ml-2 h-4 w-4" />
            </button>
          )
        },
      },
      {
        accessorKey: "status",
        header: "Status",
        cell: ({ row }) => {
          const status = row.getValue("status") as RoleStatus
          const colors = {
            [RoleStatus.ACTIVE]: "bg-green-500",
            [RoleStatus.INACTIVE]: "bg-yellow-500",
            [RoleStatus.SUSPENDED]: "bg-red-500",
          }
          return (
            <span className={`px-2 py-1 border-2 border-black text-xs font-base ${colors[status]} text-white`}>
              {status}
            </span>
          )
        },
      },
      {
        accessorKey: "isSystem",
        header: "System",
        cell: ({ row }) => (
          <div className="flex justify-center">
            {row.getValue("isSystem") ? (
              <Shield className="h-4 w-4 text-main" />
            ) : null}
          </div>
        ),
      },
      {
        id: "actions",
        header: "Actions",
        cell: ({ row }) => {
          const role = row.original
          return (
            <div className="flex gap-2">
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => onEdit(role)}
                disabled={createMutation.isPending || updateMutation.isPending}
              >
                Edit
              </Button>
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => window.location.href = `/admin/roles/${role.id}/permissions`}
                title="Manage Permissions"
                className="bg-blue-500 text-white border-2 border-black"
              >
                <Settings className="h-3 w-3" />
              </Button>
              {role.status === RoleStatus.ACTIVE ? (
                <Button
                  variant="noShadow"
                  size="sm"
                  onClick={() => onSuspend(role.id)}
                  disabled={suspendMutation.isPending}
                  className="bg-yellow-500 text-white border-2 border-black"
                  title="Suspend"
                >
                  {suspendMutation.isPending ? (
                    <Loader2 className="h-3 w-3 animate-spin" />
                  ) : (
                    <PauseCircle className="h-3 w-3" />
                  )}
                </Button>
              ) : (
                <Button
                  variant="noShadow"
                  size="sm"
                  onClick={() => onActivate(role.id)}
                  disabled={activateMutation.isPending}
                  className="bg-green-500 text-white border-2 border-black"
                  title="Activate"
                >
                  {activateMutation.isPending ? (
                    <Loader2 className="h-3 w-3 animate-spin" />
                  ) : (
                    <PlayCircle className="h-3 w-3" />
                  )}
                </Button>
              )}
              <Button
                variant="noShadow"
                size="sm"
                onClick={() => onDelete(role.id)}
                disabled={deleteMutation.isPending || role.isSystem}
                className="bg-red-500 text-white border-2 border-black disabled:opacity-50"
                title={role.isSystem ? "Cannot delete system role" : "Delete"}
              >
                {deleteMutation.isPending ? (
                  <Loader2 className="h-3 w-3 animate-spin" />
                ) : (
                  <Trash2 className="h-3 w-3" />
                )}
              </Button>
            </div>
          )
        },
      },
    ],
    [createMutation.isPending, updateMutation.isPending, deleteMutation.isPending, activateMutation.isPending, suspendMutation.isPending],
  )

  const table = useReactTable({
    data: roles,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    state: { globalFilter, columnFilters, sorting, pagination },
    onGlobalFilterChange: setGlobalFilter,
    onColumnFiltersChange: setColumnFilters,
    onSortingChange: setSorting,
    onPaginationChange: setPagination,
    globalFilterFn: "includesString",
    manualPagination: true,
    pageCount: totalPages,
  })

  function onEdit(role: Role) {
    setEditing({
      id: role.id,
      code: role.code,
      name: role.name,
      description: role.description,
      organizationId: role.organizationId,
      isSystem: role.isSystem,
      priority: role.priority,
      status: role.status,
    })
    setOpen(true)
  }

  function onCreate() {
    setEditing({
      code: "",
      name: "",
      description: "",
      organizationId: "018e0010-0000-0000-0000-000000000001", // Default organization ID
      isSystem: false,
      priority: 50,
      status: RoleStatus.ACTIVE,
    })
    setOpen(true)
  }

  async function saveRole() {
    if (!editing) return

    if (editing.id) {
      // Update existing role
      await updateMutation.mutateAsync({
        id: editing.id,
        data: {
          code: editing.code,
          name: editing.name,
          description: editing.description,
          organizationId: editing.organizationId,
          isSystem: editing.isSystem,
          priority: editing.priority,
        },
      })
    } else {
      // Create new role
      await createMutation.mutateAsync({
        code: editing.code,
        name: editing.name,
        description: editing.description,
        organizationId: editing.organizationId,
        isSystem: editing.isSystem,
        priority: editing.priority,
      })
    }

    setOpen(false)
    setEditing(null)
    refetch()
  }

  async function onDelete(id: string) {
    if (!confirm("Are you sure you want to delete this role?")) return
    await deleteMutation.mutateAsync(id)
    refetch()
  }

  async function onActivate(id: string) {
    await activateMutation.mutateAsync(id)
    refetch()
  }

  async function onSuspend(id: string) {
    await suspendMutation.mutateAsync(id)
    refetch()
  }

  if (error) {
    return (
      <div className="space-y-4">
        <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
          <h1 className="text-2xl font-heading">Roles</h1>
        </header>
        <div className="border-4 border-black bg-background p-12 shadow-[8px_8px_0_#000] text-center">
          <p className="text-lg font-base text-red-500">Error loading roles: {(error as Error).message}</p>
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
            <h1 className="text-2xl font-heading">Roles</h1>
            <p className="text-sm font-base mt-1 opacity-90">
              Manage user roles and permissions
            </p>
          </div>
          <Button onClick={onCreate} className="bg-background text-foreground border-2 border-black hover:translate-x-1 hover:translate-y-1 transition-all shadow-[4px_4px_0_#000]">
            <Shield className="h-4 w-4 mr-2" />
            Add Role
          </Button>
        </div>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <div className="flex flex-col sm:flex-row gap-3 sm:items-center mb-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-foreground/50" />
            <Input
              placeholder="Search roles..."
              value={globalFilter ?? ""}
              onChange={(e) => setGlobalFilter(e.target.value)}
              className="pl-10 border-2 border-black font-base"
            />
          </div>
        </div>

        {isLoading ? (
          <div className="text-center py-12">
            <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
            <p className="text-lg font-base">Loading roles...</p>
          </div>
        ) : (
          <>
            <div className="overflow-auto">
              <Table>
                <TableHeader className="font-heading">
                  {table.getHeaderGroups().map((hg) => (
                    <TableRow key={hg.id} className="bg-secondary-background border-b-2 border-black">
                      {hg.headers.map((h) => (
                        <TableHead key={h.id} className="font-heading">
                          {h.isPlaceholder ? null : flexRender(h.column.columnDef.header, h.getContext())}
                        </TableHead>
                      ))}
                    </TableRow>
                  ))}
                </TableHeader>
                <TableBody>
                  {table.getRowModel().rows.map((r) => (
                    <TableRow key={r.id} className="border-b-2 border-black hover:bg-secondary-background/50">
                      {r.getVisibleCells().map((c) => (
                        <TableCell key={c.id} className="font-base">
                          {flexRender(c.column.columnDef.cell, c.getContext())}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                  {!table.getRowModel().rows.length && (
                    <TableRow>
                      <TableCell colSpan={columns.length} className="text-center py-8">
                        <p className="font-base text-foreground/60">No roles found</p>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>

            <div className="flex items-center justify-between mt-4">
              <div className="text-sm font-base text-foreground/70">
                Page {pagination.pageIndex + 1} of {totalPages || 1}
              </div>
              <div className="flex items-center gap-2">
                <Button variant="noShadow" size="sm" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()}>
                  Previous
                </Button>
                <Button variant="noShadow" size="sm" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>
                  Next
                </Button>
                <select
                  className="border-2 border-black px-2 py-1 bg-background font-base"
                  value={pagination.pageSize}
                  onChange={(e) => setPagination({ ...pagination, pageSize: Number(e.target.value) })}
                >
                  {[10, 20, 30, 50].map((s) => (
                    <option key={s} value={s}>{s} / page</option>
                  ))}
                </select>
              </div>
            </div>
          </>
        )}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="border-4 border-black shadow-[8px_8px_0_#000] max-w-2xl">
          <DialogHeader>
            <DialogTitle className="font-heading text-2xl">
              {editing?.id ? "Edit Role" : "Create Role"}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="code" className="font-base">Code *</Label>
                <Input
                  id="code"
                  placeholder="ROLE_CODE"
                  value={editing?.code ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as RoleFormData), code: e.target.value.toUpperCase() }))}
                  className="border-2 border-black"
                  disabled={!!editing?.id}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="name" className="font-base">Name *</Label>
                <Input
                  id="name"
                  placeholder="Role Name"
                  value={editing?.name ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as RoleFormData), name: e.target.value }))}
                  className="border-2 border-black"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description" className="font-base">Description</Label>
              <Input
                id="description"
                placeholder="Role description"
                value={editing?.description ?? ""}
                onChange={(e) => setEditing((u) => ({ ...(u as RoleFormData), description: e.target.value }))}
                className="border-2 border-black"
              />
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="priority" className="font-base">Priority</Label>
                <Input
                  id="priority"
                  type="number"
                  placeholder="50"
                  value={editing?.priority ?? 50}
                  onChange={(e) => setEditing((u) => ({ ...(u as RoleFormData), priority: Number(e.target.value) }))}
                  className="border-2 border-black"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="organizationId" className="font-base">Organization ID *</Label>
                <Input
                  id="organizationId"
                  placeholder="Organization ID"
                  value={editing?.organizationId ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as RoleFormData), organizationId: e.target.value }))}
                  className="border-2 border-black"
                />
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
              onClick={saveRole}
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
