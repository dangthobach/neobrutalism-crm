/**
 * ✅ PHASE 1.3: Reusable Pagination Component
 * 
 * Features:
 * - Page navigation (First, Previous, Next, Last)
 * - Page size selection (10, 20, 50, 100)
 * - Total results display
 * - Responsive design
 * - Keyboard navigation
 */

import React from 'react'
import { Button } from '@/components/ui/button'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react'

export interface PaginationProps {
  page: number
  size: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
  onSizeChange: (size: number) => void
  className?: string
}

export function DataTablePagination({
  page,
  size,
  totalPages,
  totalElements,
  onPageChange,
  onSizeChange,
  className = '',
}: PaginationProps) {
  // Calculate range display
  const startIndex = page * size + 1
  const endIndex = Math.min((page + 1) * size, totalElements)

  // Page size options
  const pageSizeOptions = [10, 20, 50, 100]

  return (
    <div className={`flex items-center justify-between px-2 py-4 ${className}`}>
      {/* Results Info */}
      <div className="flex-1 text-sm text-muted-foreground">
        {totalElements > 0 ? (
          <>
            Showing <span className="font-medium">{startIndex}</span> to{' '}
            <span className="font-medium">{endIndex}</span> of{' '}
            <span className="font-medium">{totalElements}</span> results
          </>
        ) : (
          'No results found'
        )}
      </div>

      {/* Pagination Controls */}
      <div className="flex items-center space-x-6 lg:space-x-8">
        {/* Page Size Selector */}
        <div className="flex items-center space-x-2">
          <p className="text-sm font-medium">Rows per page</p>
          <Select
            value={size.toString()}
            onValueChange={(value) => {
              onSizeChange(Number(value))
              onPageChange(0) // Reset to first page when changing size
            }}
          >
            <SelectTrigger className="h-8 w-[70px]">
              <SelectValue placeholder={size.toString()} />
            </SelectTrigger>
            <SelectContent side="top">
              {pageSizeOptions.map((pageSize) => (
                <SelectItem key={pageSize} value={pageSize.toString()}>
                  {pageSize}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Page Info */}
        <div className="flex w-[100px] items-center justify-center text-sm font-medium">
          Page {page + 1} of {totalPages || 1}
        </div>

        {/* Navigation Buttons */}
        <div className="flex items-center space-x-2">
          {/* First Page */}
          <Button
            variant="default"
            className="hidden h-8 w-8 p-0 lg:flex"
            onClick={() => onPageChange(0)}
            disabled={page === 0 || totalElements === 0}
            aria-label="Go to first page"
          >
            <ChevronsLeft className="h-4 w-4" />
          </Button>

          {/* Previous Page */}
          <Button
            variant="default"
            className="h-8 w-8 p-0"
            onClick={() => onPageChange(page - 1)}
            disabled={page === 0 || totalElements === 0}
            aria-label="Go to previous page"
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>

          {/* Next Page */}
          <Button
            variant="default"
            className="h-8 w-8 p-0"
            onClick={() => onPageChange(page + 1)}
            disabled={page >= totalPages - 1 || totalElements === 0}
            aria-label="Go to next page"
          >
            <ChevronRight className="h-4 w-4" />
          </Button>

          {/* Last Page */}
          <Button
            variant="default"
            className="hidden h-8 w-8 p-0 lg:flex"
            onClick={() => onPageChange(totalPages - 1)}
            disabled={page >= totalPages - 1 || totalElements === 0}
            aria-label="Go to last page"
          >
            <ChevronsRight className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  )
}

/**
 * Simple pagination for mobile/compact views
 */
export function SimplePagination({
  page,
  totalPages,
  onPageChange,
  className = '',
}: Omit<PaginationProps, 'size' | 'totalElements' | 'onSizeChange'>) {
  return (
    <div className={`flex items-center justify-center space-x-2 py-4 ${className}`}>
      <Button
        variant="default"
        size="sm"
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
      >
        <ChevronLeft className="h-4 w-4 mr-1" />
        Previous
      </Button>

      <span className="text-sm">
        Page {page + 1} of {totalPages || 1}
      </span>

      <Button
        variant="default"
        size="sm"
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
      >
        Next
        <ChevronRight className="h-4 w-4 ml-1" />
      </Button>
    </div>
  )
}
