/**
 * Customer Table Component
 * Displays paginated list of customers with actions
 */

'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useCustomers, useDeleteCustomer } from '@/hooks/useCustomers'
import { CustomerStatusBadge } from './customer-status-badge'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Loader2, MoreHorizontal, Edit, Trash2, Eye, Star, Building2 } from 'lucide-react'
import { CustomerSearchParams, CustomerStatus, CustomerType } from '@/types/customer'

interface CustomerTableProps {
  searchParams?: CustomerSearchParams
}

export function CustomerTable({ searchParams }: CustomerTableProps) {
  const router = useRouter()
  const [page, setPage] = useState(searchParams?.page || 0)
  const [deleteId, setDeleteId] = useState<string | null>(null)

  const { data, isLoading } = useCustomers({ ...searchParams, page, size: 20 })
  const deleteMutation = useDeleteCustomer()

  const handleDelete = async () => {
    if (deleteId) {
      await deleteMutation.mutateAsync(deleteId)
      setDeleteId(null)
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center p-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    )
  }

  if (!data || data.content.length === 0) {
    return (
      <div className="text-center p-12 border-4 border-black rounded-lg bg-white">
        <Building2 className="h-12 w-12 mx-auto mb-4 text-gray-400" />
        <h3 className="text-lg font-bold mb-2">No customers found</h3>
        <p className="text-gray-600 mb-4">Get started by creating your first customer.</p>
        <Button onClick={() => router.push('/admin/customers/new')}>
          Add Customer
        </Button>
      </div>
    )
  }

  return (
    <>
      <div className="border-4 border-black rounded-lg overflow-hidden bg-white">
        <Table>
          <TableHeader>
            <TableRow className="bg-yellow-300 hover:bg-yellow-300 border-b-4 border-black">
              <TableHead className="font-bold text-black">Company</TableHead>
              <TableHead className="font-bold text-black">Code</TableHead>
              <TableHead className="font-bold text-black">Type</TableHead>
              <TableHead className="font-bold text-black">Status</TableHead>
              <TableHead className="font-bold text-black">Owner</TableHead>
              <TableHead className="font-bold text-black">Revenue</TableHead>
              <TableHead className="font-bold text-black text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.content.map((customer) => (
              <TableRow 
                key={customer.id} 
                className="hover:bg-gray-50 cursor-pointer border-b-2 border-black"
                onClick={() => router.push(`/admin/customers/${customer.id}`)}
              >
                <TableCell className="font-bold">
                  <div className="flex items-center gap-2">
                    {customer.companyName}
                    {customer.isVip && (
                      <Badge className="bg-amber-400 text-black border-2 border-black">
                        <Star className="h-3 w-3 mr-1 fill-black" />
                        VIP
                      </Badge>
                    )}
                  </div>
                  {customer.industry && (
                    <div className="text-xs text-gray-600 mt-1">{customer.industry}</div>
                  )}
                </TableCell>
                <TableCell className="font-mono font-bold">{customer.code}</TableCell>
                <TableCell>
                  <Badge variant="neutral" className="border-2 border-black">
                    {customer.customerType}
                  </Badge>
                </TableCell>
                <TableCell>
                  <CustomerStatusBadge status={customer.status} />
                </TableCell>
                <TableCell>{customer.ownerName || '-'}</TableCell>
                <TableCell>
                  {customer.annualRevenue
                    ? `$${customer.annualRevenue.toLocaleString()}`
                    : '-'}
                </TableCell>
                <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="neutral" size="sm" className="border-2 border-black">
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end" className="border-4 border-black">
                      <DropdownMenuLabel className="font-bold">Actions</DropdownMenuLabel>
                      <DropdownMenuSeparator className="bg-black h-0.5" />
                      <DropdownMenuItem
                        onClick={() => router.push(`/admin/customers/${customer.id}`)}
                        className="cursor-pointer"
                      >
                        <Eye className="h-4 w-4 mr-2" />
                        View Details
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        onClick={() => router.push(`/admin/customers/${customer.id}/edit`)}
                        className="cursor-pointer"
                      >
                        <Edit className="h-4 w-4 mr-2" />
                        Edit
                      </DropdownMenuItem>
                      <DropdownMenuSeparator className="bg-black h-0.5" />
                      <DropdownMenuItem
                        onClick={() => setDeleteId(customer.id)}
                        className="text-red-600 cursor-pointer"
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        Delete
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      <div className="flex items-center justify-between mt-6">
        <div className="text-sm text-gray-700">
          Showing <span className="font-bold">{data.content.length}</span> of{' '}
          <span className="font-bold">{data.totalElements}</span> customers
        </div>
        <div className="flex gap-2">
          <Button
            variant="neutral"
            onClick={() => setPage(page - 1)}
            disabled={page === 0}
            className="border-2 border-black font-bold"
          >
            Previous
          </Button>
          <div className="flex items-center gap-2 px-4">
            <span className="text-sm font-bold">
              Page {page + 1} of {data.totalPages}
            </span>
          </div>
          <Button
            variant="neutral"
            onClick={() => setPage(page + 1)}
            disabled={page >= data.totalPages - 1}
            className="border-2 border-black font-bold"
          >
            Next
          </Button>
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <AlertDialogContent className="border-4 border-black">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-2xl font-bold">Delete Customer?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the customer
              and all associated data.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-2 border-black font-bold">
              Cancel
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              disabled={deleteMutation.isPending}
              className="bg-red-500 hover:bg-red-600 border-2 border-black font-bold"
            >
              {deleteMutation.isPending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Deleting...
                </>
              ) : (
                'Delete'
              )}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
