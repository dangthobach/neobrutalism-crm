"use client"

import { useMemo, useState, useEffect } from "react"
import { ColumnDef, ColumnFiltersState, getCoreRowModel, getFilteredRowModel, getPaginationRowModel, getSortedRowModel, SortingState, PaginationState, useReactTable, flexRender } from "@tanstack/react-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Checkbox } from "@/components/ui/checkbox"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { ArrowUpDown, Eye, Trash2, CalendarIcon, Search } from "lucide-react"
import { format } from "date-fns"
import { DateRange } from "react-day-picker"

type User = {
  id: string
  name: string
  email: string
  role: string
  createdAt: Date
}

import { generateUsers } from "@/lib/mock"

export default function UsersPage() {
  const [rows, setRows] = useState<User[]>([])
  const [filteredRows, setFilteredRows] = useState<User[]>([])
  useEffect(() => {
    const users = generateUsers(75)
    setRows(users)
    setFilteredRows(users)
  }, [])
  const [globalFilter, setGlobalFilter] = useState("")
  const [tempGlobalFilter, setTempGlobalFilter] = useState("")
  const [open, setOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [viewing, setViewing] = useState<User | null>(null)
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [tempColumnFilters, setTempColumnFilters] = useState({ name: "", email: "", role: "" })
  const [sorting, setSorting] = useState<SortingState>([])
  const [rowSelection, setRowSelection] = useState({})
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 10 })
  const [dateRange, setDateRange] = useState<DateRange | undefined>(undefined)

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
        accessorKey: "role",
        header: ({ column }) => {
          return (
            <button
              onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
              className="flex items-center hover:opacity-80 p-0 h-auto font-heading bg-transparent border-0 cursor-pointer"
            >
              Role
              <ArrowUpDown className="ml-2 h-4 w-4" />
            </button>
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
            <Button variant="noShadow" size="sm" onClick={() => onDelete(row.original.id)}>
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        ),
      },
    ],
    [rows],
  )

  const table = useReactTable({
    data: filteredRows,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    state: { globalFilter, columnFilters, sorting, rowSelection, pagination },
    onGlobalFilterChange: setGlobalFilter,
    onColumnFiltersChange: setColumnFilters,
    onSortingChange: setSorting,
    onRowSelectionChange: setRowSelection,
    onPaginationChange: setPagination,
    globalFilterFn: "includesString",
  })

  function handleSearch() {
    let filtered = [...rows]

    // Apply date range filter
    if (dateRange?.from) {
      const fromDate = dateRange.from
      const toDate = dateRange.to
      filtered = filtered.filter((user) => {
        const userDate = new Date(user.createdAt)
        if (toDate) {
          return userDate >= fromDate && userDate <= toDate
        }
        return userDate >= fromDate
      })
    }

    setFilteredRows(filtered)
    setGlobalFilter(tempGlobalFilter)

    // Apply column filters
    const filters: ColumnFiltersState = []
    if (tempColumnFilters.name) filters.push({ id: "name", value: tempColumnFilters.name })
    if (tempColumnFilters.email) filters.push({ id: "email", value: tempColumnFilters.email })
    if (tempColumnFilters.role) filters.push({ id: "role", value: tempColumnFilters.role })
    setColumnFilters(filters)
  }

  function handleClearFilters() {
    setDateRange(undefined)
    setTempGlobalFilter("")
    setGlobalFilter("")
    setTempColumnFilters({ name: "", email: "", role: "" })
    setColumnFilters([])
    setFilteredRows(rows)
  }

  function onEdit(user: User) {
    setEditing(user)
    setOpen(true)
  }

  function onViewDetail(user: User) {
    setViewing(user)
    setDetailOpen(true)
  }

  function onDelete(id: string) {
    if (confirm("Are you sure you want to delete this user?")) {
      setRows((r) => r.filter((x) => x.id !== id))
    }
  }

  function onCreate() {
    setEditing({ id: "", name: "", email: "", role: "User", createdAt: new Date() })
    setOpen(true)
  }

  function saveUser() {
    if (!editing) return
    if (editing.id) {
      setRows((r) => r.map((x) => (x.id === editing.id ? editing : x)))
    } else {
      setRows((r) => [{ ...editing, id: crypto.randomUUID() }, ...r])
    }
    setOpen(false)
  }

  return (
    <div className="space-y-4">
      <header className="border-4 border-black bg-main text-main-foreground p-4 shadow-[8px_8px_0_#000]">
        <h1 className="text-2xl font-heading">Users</h1>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <div className="flex flex-col sm:flex-row gap-3 sm:items-center justify-between">
          <h2 className="text-lg font-heading">Search & Filters</h2>
          <Button variant="noShadow" onClick={onCreate}>New User</Button>
        </div>

        <div className="mt-3 flex flex-col gap-3">
          <Input
            placeholder="Search users by any field..."
            value={tempGlobalFilter}
            onChange={(e) => setTempGlobalFilter(e.target.value)}
            className="w-full"
          />
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <Input
              placeholder="Filter name..."
              value={tempColumnFilters.name}
              onChange={(e) => setTempColumnFilters(prev => ({ ...prev, name: e.target.value }))}
            />
            <Input
              placeholder="Filter email..."
              value={tempColumnFilters.email}
              onChange={(e) => setTempColumnFilters(prev => ({ ...prev, email: e.target.value }))}
            />
            <Input
              placeholder="Filter role..."
              value={tempColumnFilters.role}
              onChange={(e) => setTempColumnFilters(prev => ({ ...prev, role: e.target.value }))}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Popover>
              <PopoverTrigger asChild>
                <Button
                  variant="noShadow"
                  className="justify-start text-left font-base"
                >
                  <CalendarIcon className="mr-2 h-4 w-4" />
                  {dateRange?.from ? (
                    dateRange.to ? (
                      <>
                        {format(dateRange.from, "LLL dd, y")} - {format(dateRange.to, "LLL dd, y")}
                      </>
                    ) : (
                      format(dateRange.from, "LLL dd, y")
                    )
                  ) : (
                    <span>Pick a date range</span>
                  )}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-auto p-0" align="start">
                <Calendar
                  initialFocus
                  mode="range"
                  defaultMonth={dateRange?.from}
                  selected={dateRange}
                  onSelect={setDateRange}
                  numberOfMonths={2}
                />
              </PopoverContent>
            </Popover>
          </div>

          <div className="flex justify-end gap-2">
            <Button variant="noShadow" size="sm" onClick={handleClearFilters}>
              Clear
            </Button>
            <Button variant="noShadow" size="sm" onClick={handleSearch}>
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
              {!table.getRowModel().rows.length && (
                <TableRow>
                  <TableCell colSpan={columns.length} className="text-center">No results</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
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
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="font-heading">{editing?.id ? "Edit User" : "Create User"}</DialogTitle>
          </DialogHeader>
          <div className="grid sm:grid-cols-2 gap-3">
            <Input placeholder="Name" value={editing?.name ?? ""} onChange={(e) => setEditing((u) => ({ ...(u as User), name: e.target.value }))} />
            <Input placeholder="Email" value={editing?.email ?? ""} onChange={(e) => setEditing((u) => ({ ...(u as User), email: e.target.value }))} />
            <Input placeholder="Role" value={editing?.role ?? ""} onChange={(e) => setEditing((u) => ({ ...(u as User), role: e.target.value }))} />
          </div>
          <DialogFooter>
            <Button variant="noShadow" onClick={() => setOpen(false)}>Cancel</Button>
            <Button variant="noShadow" onClick={saveUser}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={detailOpen} onOpenChange={setDetailOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="font-heading">User Details</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">ID:</span>
              <span className="col-span-2 font-base">{viewing?.id}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Name:</span>
              <span className="col-span-2 font-base">{viewing?.name}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Email:</span>
              <span className="col-span-2 font-base">{viewing?.email}</span>
            </div>
            <div className="grid grid-cols-3 gap-2 p-3 border-2 border-black bg-secondary-background">
              <span className="font-heading">Role:</span>
              <span className="col-span-2 font-base">{viewing?.role}</span>
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


