"use client"

import { useMemo, useState, useEffect, useCallback } from "react"
import { ColumnDef, ColumnFiltersState, getCoreRowModel, getFilteredRowModel, getPaginationRowModel, PaginationState, useReactTable } from "@tanstack/react-table"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"

type Permission = {
  id: string
  key: string
  description?: string
}

import { generatePermissions } from "@/lib/mock"

export default function PermissionsPage() {
  const [rows, setRows] = useState<Permission[]>([])
  useEffect(() => {
    setRows(generatePermissions())
  }, [])
  const [globalFilter, setGlobalFilter] = useState("")
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Permission | null>(null)
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 10 })

  const onEdit = useCallback((item: Permission) => {
    setEditing(item)
    setOpen(true)
  }, [])

  const onDelete = useCallback((id: string) => {
    setRows((r) => r.filter((x) => x.id !== id))
  }, [])

  const columns = useMemo<ColumnDef<Permission>[]>(
    () => [
      { accessorKey: "key", header: "Key" },
      { accessorKey: "description", header: "Description" },
      {
        id: "actions",
        header: "Actions",
        cell: ({ row }) => (
          <div className="flex gap-2">
            <Button variant="noShadow" size="sm" onClick={() => onEdit(row.original)}>Edit</Button>
            <Button variant="noShadow" size="sm" onClick={() => onDelete(row.original.id)}>Delete</Button>
          </div>
        ),
      },
    ],
    [onDelete, onEdit],
  )

  const table = useReactTable({
    data: rows,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    state: { globalFilter, columnFilters, pagination },
    onGlobalFilterChange: setGlobalFilter,
    onColumnFiltersChange: setColumnFilters,
    onPaginationChange: setPagination,
    globalFilterFn: "includesString",
  })

  function onCreate() {
    setEditing({ id: "", key: "", description: "" })
    setOpen(true)
  }

  function savePermission() {
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
        <h1 className="text-2xl font-heading">Permissions</h1>
      </header>

      <div className="border-4 border-black bg-background p-4 shadow-[8px_8px_0_#000]">
        <div className="flex flex-col sm:flex-row gap-3 sm:items-center">
          <Input
            placeholder="Search permissions by any field..."
            value={globalFilter ?? ""}
            onChange={(e) => setGlobalFilter(e.target.value)}
            className="max-w-md"
          />
          <div className="sm:ml-auto">
            <Button variant="noShadow" onClick={onCreate}>New Permission</Button>
          </div>
        </div>

        <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
          <Input placeholder="Filter key..." value={(table.getColumn("key")?.getFilterValue() as string) ?? ""} onChange={(e) => table.getColumn("key")?.setFilterValue(e.target.value)} />
          <Input placeholder="Filter description..." value={(table.getColumn("description")?.getFilterValue() as string) ?? ""} onChange={(e) => table.getColumn("description")?.setFilterValue(e.target.value)} />
        </div>

        <div className="mt-4 overflow-auto">
          <Table>
            <TableHeader className="font-heading">
              {table.getHeaderGroups().map((hg) => (
                <TableRow key={hg.id} className="bg-secondary-background">
                  {hg.headers.map((h) => (
                    <TableHead key={h.id}>
                      {h.isPlaceholder ? null : h.column.columnDef.header as any}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {table.getRowModel().rows.map((r) => (
                <TableRow key={r.id} className="bg-secondary-background">
                  {r.getVisibleCells().map((c) => (
                    <TableCell key={c.id}>{c.renderValue() as any}</TableCell>
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
            <DialogTitle className="font-heading">{editing?.id ? "Edit Permission" : "Create Permission"}</DialogTitle>
          </DialogHeader>
          <div className="grid sm:grid-cols-2 gap-3">
            <Input placeholder="Key" value={editing?.key ?? ""} onChange={(e) => setEditing((u) => ({ ...(u as Permission), key: e.target.value }))} />
            <Input placeholder="Description" value={editing?.description ?? ""} onChange={(e) => setEditing((u) => ({ ...(u as Permission), description: e.target.value }))} />
          </div>
          <DialogFooter>
            <Button variant="noShadow" onClick={() => setOpen(false)}>Cancel</Button>
            <Button variant="noShadow" onClick={savePermission}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}


