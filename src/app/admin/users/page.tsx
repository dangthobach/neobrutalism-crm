"use client"

import { useMemo, useState } from "react"
import { ColumnDef, ColumnFiltersState, getCoreRowModel, getFilteredRowModel, getPaginationRowModel, getSortedRowModel, SortingState, PaginationState, useReactTable, flexRender } from "@tanstack/react-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Checkbox } from "@/components/ui/checkbox"
import { ArrowUpDown, Eye, Trash2, Search, Loader2, Shield, Users } from "lucide-react"
import { format } from "date-fns"
import { useUsers, useCreateUser, useUpdateUser, useDeleteUser } from "@/hooks/useUsers"
import { User, CreateUserRequest, UpdateUserRequest } from "@/lib/api/users"
import { Label } from "@/components/ui/label"
import { toast } from "sonner"

type UserFormData = {
  id?: string
  username: string
  email: string
  password?: string
  firstName: string
  lastName: string
  organizationId: string
}

export default function UsersPage() {
  const [globalFilter, setGlobalFilter] = useState("")
  const [tempGlobalFilter, setTempGlobalFilter] = useState("")
  const [open, setOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [editing, setEditing] = useState<UserFormData | null>(null)
  const [viewing, setViewing] = useState<User | null>(null)
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [tempColumnFilters, setTempColumnFilters] = useState({ username: "", email: "", firstName: "", lastName: "" })
  const [sorting, setSorting] = useState<SortingState>([{ id: "username", desc: false }])
  const [rowSelection, setRowSelection] = useState({})
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 10 })

  // Fetch users with React Query
  const { data: usersData, isLoading, error, refetch } = useUsers({
    page: pagination.pageIndex,
    size: pagination.pageSize,
    sortBy: sorting[0]?.id || "username",
    sortDirection: sorting[0]?.desc ? "DESC" : "ASC",
  })

  // Mutations
  const createMutation = useCreateUser()
  const updateMutation = useUpdateUser()
  const deleteMutation = useDeleteUser()

  const users = usersData?.content || []
  const totalPages = usersData?.totalPages || 0

  const columns = useMemo<ColumnDef<User>[]>(
    () => [
      {
        id: "select",
        header: ({ table }) => (
          <Checkbox
            checked={
              table.getIsAllPageRowsSelected() ||
              (table.getIsSomePageRowsSelected() && "indeterminate")
            }
            onCheckedChange={(value) => table.toggleAllPageRowsSelected(!!value)}
            aria-label="Select all"
          />
        ),
        cell: ({ row }) => (
          <Checkbox
            checked={row.getIsSelected()}
            onCheckedChange={(value) => row.toggleSelected(!!value)}
            aria-label="Select row"
          />
        ),
        enableSorting: false,
        enableHiding: false,
      },
      {
        accessorKey: "username",
        header: ({ column }) => {
          return (
            <button
              onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
              className="flex items-center hover:opacity-80 p-0 h-auto font-heading bg-transparent border-0 cursor-pointer"
            >
              Username
              <ArrowUpDown className="ml-2 h-4 w-4" />
            </button>
          )
        },
      },
      {
        accessorKey: "fullName",
        header: "Full Name",
      },
      {
        accessorKey: "email",
        header: ({ column }) => {
          return (
            <button
              onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
              className="flex items-center hover:opacity-80 p-0 h-auto font-heading bg-transparent border-0 cursor-pointer"
            >
              Email
              <ArrowUpDown className="ml-2 h-4 w-4" />
            </button>
          )
        },
      },
      {
        accessorKey: "status",
        header: "Status",
        cell: ({ row }) => {
          const status = row.original.status
          const statusColors: Record<string, string> = {
            ACTIVE: "bg-green-500",
            INACTIVE: "bg-gray-500",
            SUSPENDED: "bg-yellow-500",
            LOCKED: "bg-red-500",
            PENDING: "bg-blue-500",
          }
          return (
            <span className={`px-2 py-1 text-xs font-bold text-white ${statusColors[status] || "bg-gray-500"}`}>
              {status}
            </span>
          )
        },
      },
      {
        id: "actions",
        header: "Actions",
        cell: ({ row }) => (
          <div className="flex gap-2">
            <Button variant="noShadow" size="sm" onClick={() => onViewDetail(row.original)}>
              <Eye className="h-4 w-4" />
            </Button>
            <Button variant="noShadow" size="sm" onClick={() => onEdit(row.original)}>
              Edit
            </Button>
            <Button
              variant="noShadow"
              size="sm"
              onClick={() => window.location.href = `/admin/users/${row.original.id}/roles`}
              title="Manage Roles"
            >
              <Shield className="h-4 w-4" />
            </Button>
            <Button
              variant="noShadow"
              size="sm"
              onClick={() => window.location.href = `/admin/users/${row.original.id}/groups`}
              title="Manage Groups"
            >
              <Users className="h-4 w-4" />
            </Button>
            <Button variant="noShadow" size="sm" onClick={() => onDelete(row.original.id)}>
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        ),
      },
    ],
    [],
  )

  const table = useReactTable({
    data: users,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    manualPagination: true,
    manualSorting: true,
    pageCount: totalPages,
    state: { globalFilter, columnFilters, sorting, rowSelection, pagination },
    onGlobalFilterChange: setGlobalFilter,
    onColumnFiltersChange: setColumnFilters,
    onSortingChange: setSorting,
    onRowSelectionChange: setRowSelection,
    onPaginationChange: setPagination,
    globalFilterFn: "includesString",
  })

  function handleSearch() {
    refetch()
    setGlobalFilter(tempGlobalFilter)

    // Apply column filters
    const filters: ColumnFiltersState = []
    if (tempColumnFilters.username) filters.push({ id: "username", value: tempColumnFilters.username })
    if (tempColumnFilters.email) filters.push({ id: "email", value: tempColumnFilters.email })
    if (tempColumnFilters.firstName) filters.push({ id: "firstName", value: tempColumnFilters.firstName })
    if (tempColumnFilters.lastName) filters.push({ id: "lastName", value: tempColumnFilters.lastName })
    setColumnFilters(filters)
  }

  function handleClearFilters() {
    setTempGlobalFilter("")
    setGlobalFilter("")
    setTempColumnFilters({ username: "", email: "", firstName: "", lastName: "" })
    setColumnFilters([])
    setPagination({ pageIndex: 0, pageSize: 10 })
    refetch()
  }

  function onEdit(user: User) {
    setEditing({
      id: user.id,
      username: user.username,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      organizationId: user.organizationId,
    })
    setOpen(true)
  }

  function onViewDetail(user: User) {
    setViewing(user)
    setDetailOpen(true)
  }

  function onDelete(id: string) {
    if (confirm("Are you sure you want to delete this user?")) {
      deleteMutation.mutate(id)
    }
  }

  function onCreate() {
    setEditing({
      username: "",
      email: "",
      password: "",
      firstName: "",
      lastName: "",
      organizationId: "",
    })
    setOpen(true)
  }

  function saveUser() {
    if (!editing) return

    if (editing.id) {
      // Update existing user
      const updateData: UpdateUserRequest = {
        username: editing.username,
        email: editing.email,
        firstName: editing.firstName,
        lastName: editing.lastName,
        organizationId: editing.organizationId,
      }

      updateMutation.mutate(
        { id: editing.id, data: updateData },
        {
          onSuccess: () => {
            setOpen(false)
            setEditing(null)
          },
        }
      )
    } else {
      // Create new user
      if (!editing.password) {
        toast.error("Password is required for new users")
        return
      }

      const createData: CreateUserRequest = {
        username: editing.username,
        email: editing.email,
        password: editing.password,
        firstName: editing.firstName,
        lastName: editing.lastName,
        organizationId: editing.organizationId,
      }

      createMutation.mutate(createData, {
        onSuccess: () => {
          setOpen(false)
          setEditing(null)
        },
      })
    }
  }

  return (
    <div className="space-y-4">
      <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
        <h1 className="text-2xl font-heading">Users</h1>
      </header>

      {error && (
        <div className="border-4 border-black bg-red-100 p-4 shadow-[8px_8px_0_#000]">
          <p className="text-red-800 font-base">Error loading users: {error.message}</p>
          <Button variant="noShadow" size="sm" onClick={() => refetch()} className="mt-2">
            Retry
          </Button>
        </div>
      )}

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <div className="flex flex-col sm:flex-row gap-3 sm:items-center justify-between">
          <h2 className="text-lg font-heading">Search & Filters</h2>
          <Button variant="noShadow" onClick={onCreate} disabled={isLoading}>
            New User
          </Button>
        </div>

        <div className="mt-3 flex flex-col gap-3">
          <Input
            placeholder="Search users by any field..."
            value={tempGlobalFilter}
            onChange={(e) => setTempGlobalFilter(e.target.value)}
            className="w-full"
            disabled={isLoading}
          />
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-3">
            <Input
              placeholder="Filter username..."
              value={tempColumnFilters.username}
              onChange={(e) => setTempColumnFilters(prev => ({ ...prev, username: e.target.value }))}
              disabled={isLoading}
            />
            <Input
              placeholder="Filter email..."
              value={tempColumnFilters.email}
              onChange={(e) => setTempColumnFilters(prev => ({ ...prev, email: e.target.value }))}
              disabled={isLoading}
            />
            <Input
              placeholder="Filter first name..."
              value={tempColumnFilters.firstName}
              onChange={(e) => setTempColumnFilters(prev => ({ ...prev, firstName: e.target.value }))}
              disabled={isLoading}
            />
            <Input
              placeholder="Filter last name..."
              value={tempColumnFilters.lastName}
              onChange={(e) => setTempColumnFilters(prev => ({ ...prev, lastName: e.target.value }))}
              disabled={isLoading}
            />
          </div>

          <div className="flex justify-end gap-2">
            <Button variant="noShadow" size="sm" onClick={handleClearFilters} disabled={isLoading}>
              Clear
            </Button>
            <Button variant="noShadow" size="sm" onClick={handleSearch} disabled={isLoading}>
              <Search className="mr-2 h-4 w-4" />
              Search
            </Button>
          </div>
        </div>

        {Object.keys(rowSelection).length > 0 && (
          <div className="mt-3 p-2 bg-secondary-background border-2 border-black">
            <span className="text-sm font-base">{Object.keys(rowSelection).length} row(s) selected</span>
          </div>
        )}
      </div>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <h2 className="text-lg font-heading mb-3">Users Table</h2>

        {isLoading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-8 w-8 animate-spin" />
            <span className="ml-2 font-base">Loading users...</span>
          </div>
        ) : (
          <div className="overflow-auto">
            <Table>
              <TableHeader className="font-heading">
                {table.getHeaderGroups().map((hg) => (
                  <TableRow key={hg.id} className="bg-secondary-background">
                    {hg.headers.map((h) => (
                      <TableHead key={h.id}>
                        {h.isPlaceholder ? null : flexRender(h.column.columnDef.header, h.getContext())}
                      </TableHead>
                    ))}
                  </TableRow>
                ))}
              </TableHeader>
              <TableBody>
                {table.getRowModel().rows.map((r) => (
                  <TableRow key={r.id} className="bg-secondary-background">
                    {r.getVisibleCells().map((c) => (
                      <TableCell key={c.id}>{flexRender(c.column.columnDef.cell, c.getContext())}</TableCell>
                    ))}
                  </TableRow>
                ))}
                {!table.getRowModel().rows.length && !isLoading && (
                  <TableRow>
                    <TableCell colSpan={columns.length} className="text-center">No users found</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      <div className="flex items-center justify-between py-3">
        <div className="text-sm font-base">Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount() || 1}</div>
        <div className="flex items-center gap-2">
          <Button variant="noShadow" size="sm" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()}>Previous</Button>
          <Button variant="noShadow" size="sm" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>Next</Button>
          <select
            className="border-2 border-black px-2 py-1 bg-background"
            value={table.getState().pagination.pageSize}
            onChange={(e) => table.setPageSize(Number(e.target.value))}
          >
            {[10, 20, 30, 50].map((s) => (
              <option key={s} value={s}>{s} / page</option>
            ))}
          </select>
        </div>
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="font-heading">{editing?.id ? "Edit User" : "Create User"}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4">
            <div className="grid sm:grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="username">Username *</Label>
                <Input
                  id="username"
                  placeholder="Username"
                  value={editing?.username ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as UserFormData), username: e.target.value }))}
                  disabled={!!editing?.id}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email *</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="Email"
                  value={editing?.email ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as UserFormData), email: e.target.value }))}
                />
              </div>
            </div>

            {!editing?.id && (
              <div className="space-y-2">
                <Label htmlFor="password">Password *</Label>
                <Input
                  id="password"
                  type="password"
                  placeholder="Password"
                  value={editing?.password ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as UserFormData), password: e.target.value }))}
                />
              </div>
            )}

            <div className="grid sm:grid-cols-2 gap-3">
              <div className="space-y-2">
                <Label htmlFor="firstName">First Name *</Label>
                <Input
                  id="firstName"
                  placeholder="First Name"
                  value={editing?.firstName ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as UserFormData), firstName: e.target.value }))}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="lastName">Last Name *</Label>
                <Input
                  id="lastName"
                  placeholder="Last Name"
                  value={editing?.lastName ?? ""}
                  onChange={(e) => setEditing((u) => ({ ...(u as UserFormData), lastName: e.target.value }))}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="organizationId">Organization ID *</Label>
              <Input
                id="organizationId"
                placeholder="Organization ID (UUID)"
                value={editing?.organizationId ?? ""}
                onChange={(e) => setEditing((u) => ({ ...(u as UserFormData), organizationId: e.target.value }))}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="noShadow" onClick={() => setOpen(false)} disabled={createMutation.isPending || updateMutation.isPending}>
              Cancel
            </Button>
            <Button
              variant="noShadow"
              onClick={saveUser}
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              {createMutation.isPending || updateMutation.isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Saving...
                </>
              ) : (
                "Save"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={detailOpen} onOpenChange={setDetailOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="font-heading">User Details</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">ID:</span>
              <span className="col-span-2 font-base">{viewing?.id}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Username:</span>
              <span className="col-span-2 font-base">{viewing?.username}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Full Name:</span>
              <span className="col-span-2 font-base">{viewing?.fullName}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Email:</span>
              <span className="col-span-2 font-base">{viewing?.email}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Status:</span>
              <span className="col-span-2 font-base">{viewing?.status}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Organization ID:</span>
              <span className="col-span-2 font-base">{viewing?.organizationId}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Created At:</span>
              <span className="col-span-2 font-base">{viewing?.createdAt ? format(new Date(viewing.createdAt), "PPpp") : "N/A"}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Last Login:</span>
              <span className="col-span-2 font-base">{viewing?.lastLoginAt ? format(new Date(viewing.lastLoginAt), "PPpp") : "Never"}</span>
            </div>
          </div>
          <DialogFooter>
            <Button variant="noShadow" onClick={() => setDetailOpen(false)}>Close</Button>
            <Button variant="noShadow" onClick={() => {
              setDetailOpen(false)
              onEdit(viewing!)
            }}>Edit</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}


