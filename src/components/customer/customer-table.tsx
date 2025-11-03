/**
 * Customer Table Component
 * Displays customers in a table with actions
 */

'use client'

import Link from 'next/link'
import { MoreHorizontal, Eye, Edit, Trash2, Mail, Phone } from 'lucide-react'
import { Customer } from '@/types/customer'
import { CustomerStatusBadge } from './customer-status-badge'
import { formatCurrency, formatDate } from '@/lib/utils'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
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
import { useState } from 'react'

interface CustomerTableProps {
  customers: Customer[]
  onDelete?: (id: string) => void
  isLoading?: boolean
}

export function CustomerTable({ customers, onDelete, isLoading }: CustomerTableProps) {
  const [deleteId, setDeleteId] = useState<string | null>(null)

  const handleDelete = () => {
    if (deleteId && onDelete) {
      onDelete(deleteId)
      setDeleteId(null)
    }
  }

  if (isLoading) {
    return (
      <div className="rounded border-2 border-black bg-white p-8 text-center">
        <p className="font-bold uppercase">Loading customers...</p>
      </div>
    )
  }

  if (customers.length === 0) {
    return (
      <div className="rounded border-2 border-black bg-white p-8 text-center">
        <p className="font-bold uppercase text-gray-500">No customers found</p>
      </div>
    )
  }

  return (
    <>
      <div className="overflow-hidden rounded border-2 border-black bg-white">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b-2 border-black bg-yellow-200">
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Customer
                </th>
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Type
                </th>
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Status
                </th>
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Contact
                </th>
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Revenue
                </th>
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Owner
                </th>
                <th className="px-4 py-3 text-center font-black uppercase">Actions</th>
              </tr>
            </thead>
            <tbody>
              {customers.map((customer, index) => (
                <tr
                  key={customer.id}
                  className={`border-b-2 border-black transition-colors hover:bg-yellow-50 ${
                    index === customers.length - 1 ? 'border-b-0' : ''
                  }`}
                >
                  <td className="border-r-2 border-black px-4 py-3">
                    <div>
                      <Link
                        href={`/admin/customers/${customer.id}`}
                        className="font-black uppercase hover:underline"
                      >
                        {customer.companyName}
                      </Link>
                      <p className="text-xs font-bold text-gray-600">{customer.code}</p>
                      {customer.isVip && (
                        <span className="mt-1 inline-block rounded-full border border-black bg-yellow-400 px-2 py-0.5 text-xs font-black">
                          ⭐ VIP
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="border-r-2 border-black px-4 py-3">
                    <span className="rounded-full border border-black px-2 py-1 text-xs font-bold uppercase">
                      {customer.customerType}
                    </span>
                  </td>
                  <td className="border-r-2 border-black px-4 py-3">
                    <CustomerStatusBadge status={customer.status} />
                  </td>
                  <td className="border-r-2 border-black px-4 py-3">
                    <div className="space-y-1 text-sm">
                      {customer.email && (
                        <div className="flex items-center gap-1">
                          <Mail className="h-3 w-3" />
                          <span className="font-medium">{customer.email}</span>
                        </div>
                      )}
                      {customer.phone && (
                        <div className="flex items-center gap-1">
                          <Phone className="h-3 w-3" />
                          <span className="font-medium">{customer.phone}</span>
                        </div>
                      )}
                    </div>
                  </td>
                  <td className="border-r-2 border-black px-4 py-3">
                    {customer.annualRevenue ? (
                      <span className="font-black text-green-700">
                        {formatCurrency(customer.annualRevenue)}
                      </span>
                    ) : (
                      <span className="text-gray-400">—</span>
                    )}
                  </td>
                  <td className="border-r-2 border-black px-4 py-3">
                    <span className="font-bold">{customer.ownerName || '—'}</span>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <DropdownMenu>
                      <DropdownMenuTrigger className="rounded border-2 border-black bg-white p-1 shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none">
                        <MoreHorizontal className="h-4 w-4" />
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="border-2 border-black">
                        <DropdownMenuItem asChild>
                          <Link
                            href={`/admin/customers/${customer.id}`}
                            className="flex items-center gap-2 font-bold"
                          >
                            <Eye className="h-4 w-4" />
                            View
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link
                            href={`/admin/customers/${customer.id}/edit`}
                            className="flex items-center gap-2 font-bold"
                          >
                            <Edit className="h-4 w-4" />
                            Edit
                          </Link>
                        </DropdownMenuItem>
                        {onDelete && (
                          <DropdownMenuItem
                            onClick={() => setDeleteId(customer.id)}
                            className="flex items-center gap-2 font-bold text-red-600"
                          >
                            <Trash2 className="h-4 w-4" />
                            Delete
                          </DropdownMenuItem>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <AlertDialogContent className="border-2 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
          <AlertDialogHeader>
            <AlertDialogTitle className="font-black uppercase">Delete Customer</AlertDialogTitle>
            <AlertDialogDescription className="font-bold">
              Are you sure you want to delete this customer? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-2 border-black font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none">
              Cancel
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="border-2 border-black bg-red-400 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-red-500 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
