/**
 * ✅ PHASE 1.3: Generic Data Table Component
 * 
 * Features:
 * - @tanstack/react-table integration
 * - Sorting & filtering
 * - Row selection
 * - Column visibility
 * - Server-side pagination
 * - Loading states
 * - Empty states
 * - Responsive design
 */

"use client"

import React from "react"
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  useReactTable,
  SortingState,
  ColumnFiltersState,
  VisibilityState,
  Row,
} from "@tanstack/react-table"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { Input } from "@/components/ui/input"
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { DataTablePagination } from "@/components/ui/data-table-pagination"
import { ChevronDown } from "lucide-react"

export interface GenericDataTableProps<TData, TValue> {
  columns: ColumnDef<TData, TValue>[]
  data: TData[]
  loading?: boolean
  
  // Server-side pagination
  pageIndex?: number
  pageSize?: number
  totalItems?: number
  onPaginationChange?: (pageIndex: number, pageSize: number) => void
  
  // Sorting
  sorting?: SortingState
  onSortingChange?: (sorting: SortingState) => void
  
  // Column filters
  columnFilters?: ColumnFiltersState
  onColumnFiltersChange?: (filters: ColumnFiltersState) => void
  
  // Search
  searchColumn?: string
  searchPlaceholder?: string
  
  // Row selection
  rowSelection?: Record<string, boolean>
  onRowSelectionChange?: (selection: Record<string, boolean>) => void
  
  // Column visibility
  columnVisibility?: VisibilityState
  onColumnVisibilityChange?: (visibility: VisibilityState) => void
  
  // Actions
  onRowClick?: (row: Row<TData>) => void
  
  // Empty state
  emptyMessage?: string
  
  // Styling
  className?: string
}

export function GenericDataTable<TData, TValue>({
  columns,
  data,
  loading = false,
  pageIndex = 0,
  pageSize = 10,
  totalItems = 0,
  onPaginationChange,
  sorting = [],
  onSortingChange,
  columnFilters = [],
  onColumnFiltersChange,
  searchColumn,
  searchPlaceholder = "Search...",
  rowSelection = {},
  onRowSelectionChange,
  columnVisibility = {},
  onColumnVisibilityChange,
  onRowClick,
  emptyMessage = "No results.",
  className = "",
}: GenericDataTableProps<TData, TValue>) {
  const [internalSorting, setInternalSorting] = React.useState<SortingState>(sorting)
  const [internalColumnFilters, setInternalColumnFilters] = React.useState<ColumnFiltersState>(columnFilters)
  const [internalRowSelection, setInternalRowSelection] = React.useState(rowSelection)
  const [internalColumnVisibility, setInternalColumnVisibility] = React.useState<VisibilityState>(columnVisibility)

  const table = useReactTable({
    data,
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualPagination: true,
    pageCount: Math.ceil(totalItems / pageSize),
    state: {
      sorting: onSortingChange ? sorting : internalSorting,
      columnFilters: onColumnFiltersChange ? columnFilters : internalColumnFilters,
      rowSelection: onRowSelectionChange ? rowSelection : internalRowSelection,
      columnVisibility: onColumnVisibilityChange ? columnVisibility : internalColumnVisibility,
      pagination: {
        pageIndex,
        pageSize,
      },
    },
    onSortingChange: (updater) => {
      const newSorting = typeof updater === 'function' 
        ? updater(onSortingChange ? sorting : internalSorting) 
        : updater
      
      if (onSortingChange) {
        onSortingChange(newSorting)
      } else {
        setInternalSorting(newSorting)
      }
    },
    onColumnFiltersChange: (updater) => {
      const newFilters = typeof updater === 'function'
        ? updater(onColumnFiltersChange ? columnFilters : internalColumnFilters)
        : updater
      
      if (onColumnFiltersChange) {
        onColumnFiltersChange(newFilters)
      } else {
        setInternalColumnFilters(newFilters)
      }
    },
    onRowSelectionChange: (updater) => {
      const newSelection = typeof updater === 'function'
        ? updater(onRowSelectionChange ? rowSelection : internalRowSelection)
        : updater
      
      if (onRowSelectionChange) {
        onRowSelectionChange(newSelection)
      } else {
        setInternalRowSelection(newSelection)
      }
    },
    onColumnVisibilityChange: (updater) => {
      const newVisibility = typeof updater === 'function'
        ? updater(onColumnVisibilityChange ? columnVisibility : internalColumnVisibility)
        : updater
      
      if (onColumnVisibilityChange) {
        onColumnVisibilityChange(newVisibility)
      } else {
        setInternalColumnVisibility(newVisibility)
      }
    },
  })

  /**
   * Handle pagination change
   */
  const handlePaginationChange = (newPageIndex: number, newPageSize: number) => {
    if (onPaginationChange) {
      onPaginationChange(newPageIndex, newPageSize)
    }
  }

  return (
    <div className={`w-full space-y-4 ${className}`}>
      {/* Toolbar */}
      <div className="flex items-center justify-between">
        {/* Search */}
        {searchColumn && (
          <Input
            placeholder={searchPlaceholder}
            value={(table.getColumn(searchColumn)?.getFilterValue() as string) ?? ""}
            onChange={(event) =>
              table.getColumn(searchColumn)?.setFilterValue(event.target.value)
            }
            className="max-w-sm"
          />
        )}

        {/* Column visibility */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="noShadow" className="ml-auto">
              Columns <ChevronDown className="ml-2 h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            {table
              .getAllColumns()
              .filter((column) => column.getCanHide())
              .map((column) => {
                return (
                  <DropdownMenuCheckboxItem
                    key={column.id}
                    className="capitalize"
                    checked={column.getIsVisible()}
                    onCheckedChange={(value) =>
                      column.toggleVisibility(!!value)
                    }
                  >
                    {column.id}
                  </DropdownMenuCheckboxItem>
                )
              })}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      {/* Table */}
      <div className="rounded-md border">
        <Table>
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  return (
                    <TableHead key={header.id}>
                      {header.isPlaceholder
                        ? null
                        : flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                    </TableHead>
                  )
                })}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {loading ? (
              // Loading skeleton
              Array.from({ length: pageSize }).map((_, index) => (
                <TableRow key={index}>
                  {columns.map((_, colIndex) => (
                    <TableCell key={colIndex}>
                      <Skeleton className="h-4 w-full" />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : table.getRowModel().rows?.length ? (
              // Data rows
              table.getRowModel().rows.map((row) => (
                <TableRow
                  key={row.id}
                  data-state={row.getIsSelected() && "selected"}
                  onClick={() => onRowClick?.(row)}
                  className={onRowClick ? "cursor-pointer hover:bg-muted/50" : ""}
                >
                  {row.getVisibleCells().map((cell) => (
                    <TableCell key={cell.id}>
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : (
              // Empty state
              <TableRow>
                <TableCell
                  colSpan={columns.length}
                  className="h-24 text-center text-muted-foreground"
                >
                  {emptyMessage}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      <DataTablePagination
        page={pageIndex}
        size={pageSize}
        totalPages={Math.ceil(totalItems / pageSize)}
        totalElements={totalItems}
        onPageChange={(page: number) => handlePaginationChange(page, pageSize)}
        onSizeChange={(size: number) => handlePaginationChange(0, size)}
      />
    </div>
  )
}
