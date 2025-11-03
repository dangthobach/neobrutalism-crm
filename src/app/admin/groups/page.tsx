"use client"

import { useMemo, useState, useCallback } from "react"
import { ColumnDef, ColumnFiltersState, getCoreRowModel, getFilteredRowModel, getPaginationRowModel, getSortedRowModel, SortingState, PaginationState, useReactTable, flexRender } from "@tanstack/react-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { ArrowUpDown, Trash2, Search, Loader2, PlayCircle, PauseCircle, Users, Shield, UserPlus } from "lucide-react"
import { useGroups, useCreateGroup, useUpdateGroup, useDeleteGroup, useActivateGroup, useSuspendGroup } from "@/hooks/useGroups"
import { Group, GroupStatus } from "@/lib/api/groups"
import { PermissionGuard } from "@/components/auth/permission-guard"
import { toast } from "sonner"

type GroupFormData = Omit<Group, 'id' | 'deleted' | 'createdAt' | 'updatedAt' | 'createdBy' | 'updatedBy' | 'path' | 'level'> & { id?: string }

export default function GroupsPage() {
  const [globalFilter, setGlobalFilter] = useState("")
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<GroupFormData | null>(null)
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [sorting, setSorting] = useState<SortingState>([{ id: "name", desc: false }])
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 10 })

  // Fetch groups with React Query
  const { data: groupsData, isLoading, error, refetch } = useGroups({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sortBy: sorting[0]?.id || "name",
    sortDirection: sorting[0]?.desc ? "DESC" : "ASC",
  })

  // Mutations
  const createMutation = useCreateGroup()
  const updateMutation = useUpdateGroup()
  const deleteMutation = useDeleteGroup()
  const activateMutation = useActivateGroup()
  const suspendMutation = useSuspendGroup()

  const groups = groupsData?.content || []
  const totalPages = groupsData?.totalPages || 0

  // Define handlers with useCallback before useMemo
  const onDelete = useCallback(async (id: string) => {
    if (!confirm("Are you sure you want to delete this group?")) return
    await deleteMutation.mutateAsync(id)
    refetch()
  }, [deleteMutation, refetch])

  const onActivate = useCallback(async (id: string) => {
    await activateMutation.mutateAsync(id)
    refetch()
  }, [activateMutation, refetch])

  const onSuspend = useCallback(async (id: string) => {
    await suspendMutation.mutateAsync(id)
    refetch()
  }, [suspendMutation, refetch])

  const columns = useMemo<ColumnDef<Group>[]>(
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
        accessorKey: "level",
        header: "Level",
        cell: ({ row }) => (
          <span className="px-2 py-1 border-2 border-black text-xs font-base bg-blue-100">
            {row.getValue("level")}
          </span>
        ),
      },
      {
        accessorKey: "status",
        header: "Status",
        cell: ({ row }) => {
          const status = row.getValue("status") as GroupStatus
          const colors = {
            [GroupStatus.ACTIVE]: "bg-green-500",
            [GroupStatus.INACTIVE]: "bg-yellow-500",
            [GroupStatus.SUSPENDED]: "bg-red-500",
          }
          return (
            <span className={`px-2 py-1 border-2 border-black text-xs font-base ${colors[status]} text-white`}>
              {status}
            </span>
          )
        },
      },
      {
        id: "actions",
        header: "Actions",
        cell: ({ row }) => {
          const group = row.original
          return (
            <div className="flex gap-2">
              <PermissionGuard routeOrCode="/groups" permission="canEdit">
                <Button
                  variant="noShadow"
                  size="sm"
                  onClick={() => onEdit(group)}
                  disabled={createMutation.isPending || updateMutation.isPending}
                >
                  Edit
                </Button>
              </PermissionGuard>
              <PermissionGuard routeOrCode="/groups" permission="canEdit">
                <Button
                  variant="noShadow"
                  size="sm"
                  onClick={() => window.location.href = `/admin/groups/${group.id}/members`}
                  title="Manage Members"
                >
                  <UserPlus className="h-3 w-3" />
                </Button>
              </PermissionGuard>
              <PermissionGuard routeOrCode="/groups" permission="canEdit">
                <Button
                  variant="noShadow"
                  size="sm"
                  onClick={() => window.location.href = `/admin/groups/${group.id}/roles`}
                  title="Manage Roles"
                >
                  <Shield className="h-3 w-3" />
                </Button>
              </PermissionGuard>
              <PermissionGuard routeOrCode="/groups" permission="canEdit">
                {group.status === GroupStatus.ACTIVE ? (
                  <Button
                    variant="noShadow"
                    size="sm"
                    onClick={() => onSuspend(group.id)}
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
                    onClick={() => onActivate(group.id)}
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
              </PermissionGuard>
              <PermissionGuard routeOrCode="/groups" permission="canDelete">
                <Button
                  variant="noShadow"
                  size="sm"
                  onClick={() => onDelete(group.id)}
                  disabled={deleteMutation.isPending}
                  className="bg-red-500 text-white border-2 border-black"
                  title="Delete"
                >
                  {deleteMutation.isPending ? (
                    <Loader2 className="h-3 w-3 animate-spin" />
                  ) : (
                    <Trash2 className="h-3 w-3" />
                  )}
                </Button>
              </PermissionGuard>
            </div>
          )
        },
      },
    ],
    [createMutation.isPending, updateMutation.isPending, deleteMutation.isPending, activateMutation.isPending, suspendMutation.isPending, onDelete, onActivate, onSuspend],
  )

  const table = useReactTable({
    data: groups,
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

  function onEdit(group: Group) {
    setEditing({
      id: group.id,
      code: group.code,
      name: group.name,
      description: group.description,
      parentId: group.parentId,
      organizationId: group.organizationId,
      status: group.status,
    })
    setOpen(true)
  }

  function onCreate() {
    setEditing({
      code: "",
      name: "",
      description: "",
      parentId: undefined,
      organizationId: "018e0010-0000-0000-0000-000000000001", // Default organization ID
      status: GroupStatus.ACTIVE,
    })
    setOpen(true)
  }

  async function saveGroup() {
    if (!editing) return

    if (editing.id) {
      // Update existing group
      await updateMutation.mutateAsync({
        id: editing.id,
        data: {
          code: editing.code,
          name: editing.name,
          description: editing.description,
          parentId: editing.parentId || undefined,
          organizationId: editing.organizationId,
        },
      })
    } else {
      // Create new group
      await createMutation.mutateAsync({
        code: editing.code,
        name: editing.name,
        description: editing.description,
        parentId: editing.parentId || undefined,
        organizationId: editing.organizationId,
      })
    }

    setOpen(false)
    setEditing(null)
    refetch()
  }

  if (error) {
    return (
      <div className="space-y-4">
        <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
          <h1 className="text-2xl font-heading">Groups</h1>
        </header>
        <div className="border-4 border-black bg-background p-12 shadow-[8px_8px_0_#000] text-center">
          <p className="text-lg font-base text-red-500">Error loading groups: {(error as Error).message}</p>
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
            <h1 className="text-2xl font-heading">Groups</h1>
            <p className="text-sm font-base mt-1 opacity-90">
              Manage organizational groups and teams
            </p>
          </div>
          <PermissionGuard routeOrCode="/groups" permission="canCreate">
            <Button onClick={onCreate} className="bg-background text-foreground border-2 border-black hover:translate-x-1 hover:translate-y-1 transition-all shadow-[4px_4px_0_#000]">
              <Users className="h-4 w-4 mr-2" />
              Add Group
            </Button>
          </PermissionGuard>
        </div>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <div className="flex flex-col sm:flex-row gap-3 sm:items-center mb-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-foreground/50" />
            <Input
              placeholder="Search groups..."
              value={globalFilter ?? ""}
              onChange={(e) => setGlobalFilter(e.target.value)}
              className="pl-10 border-2 border-black font-base"
            />
          </div>
        </div>

        {isLoading ? (
          <div className="text-center py-12">
            <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4" />
            <p className="text-lg font-base">Loading groups...</p>
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
                        <p className="font-base text-foreground/60">No groups found</p>
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
              {editing?.id ? "Edit Group" : "Create Group"}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="grid sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="code" className="font-base">Code *</Label>
                <Input
                  id="code"
                  placeholder="GROUP_CODE"
                  value={editing?.code ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as GroupFormData), code: e.target.value.toUpperCase() }))}
                  className="border-2 border-black"
                  disabled={!!editing?.id}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="name" className="font-base">Name *</Label>
                <Input
                  id="name"
                  placeholder="Group Name"
                  value={editing?.name ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as GroupFormData), name: e.target.value }))}
                  className="border-2 border-black"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description" className="font-base">Description</Label>
              <Input
                id="description"
                placeholder="Group description"
                value={editing?.description ?? ""}
                onChange={(e) => setEditing((u) => ({ ...(u as GroupFormData), description: e.target.value }))}
                className="border-2 border-black"
              />
            </div>
            <div className="grid sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="parentId" className="font-base">Parent Group ID (Optional)</Label>
                <Input
                  id="parentId"
                  placeholder="Leave empty for root group"
                  value={editing?.parentId ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as GroupFormData), parentId: e.target.value || undefined }))}
                  className="border-2 border-black"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="organizationId" className="font-base">Organization ID *</Label>
                <Input
                  id="organizationId"
                  placeholder="Organization ID"
                  value={editing?.organizationId ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as GroupFormData), organizationId: e.target.value }))}
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
              onClick={saveGroup}
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
