/**
 * Contact Table Component
 * Displays contacts in a table with actions
 */

'use client'

import Link from 'next/link'
import { MoreHorizontal, Eye, Edit, Trash2, Mail, Phone, Star } from 'lucide-react'
import { Contact } from '@/types/contact'
import { ContactRoleBadge } from './contact-role-badge'
import { ContactStatusBadge } from './contact-status-badge'
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

interface ContactTableProps {
  contacts: Contact[]
  onDelete?: (id: string) => void
  onSetPrimary?: (id: string) => void
  isLoading?: boolean
  showCustomer?: boolean
}

export function ContactTable({ contacts, onDelete, onSetPrimary, isLoading, showCustomer = true }: ContactTableProps) {
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
        <p className="font-bold uppercase">Loading contacts...</p>
      </div>
    )
  }

  if (contacts.length === 0) {
    return (
      <div className="rounded border-2 border-black bg-white p-8 text-center">
        <p className="font-bold uppercase text-gray-500">No contacts found</p>
      </div>
    )
  }

  return (
    <>
      <div className="overflow-hidden rounded border-2 border-black bg-white">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b-2 border-black bg-green-200">
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Contact
                </th>
                {showCustomer && (
                  <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                    Customer
                  </th>
                )}
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Role
                </th>
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Status
                </th>
                <th className="border-r-2 border-black px-4 py-3 text-left font-black uppercase">
                  Contact Info
                </th>
                <th className="px-4 py-3 text-center font-black uppercase">Actions</th>
              </tr>
            </thead>
            <tbody>
              {contacts.map((contact, index) => (
                <tr
                  key={contact.id}
                  className={`border-b-2 border-black transition-colors hover:bg-green-50 ${
                    index === contacts.length - 1 ? 'border-b-0' : ''
                  }`}
                >
                  <td className="border-r-2 border-black px-4 py-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <Link
                          href={`/admin/contacts/${contact.id}`}
                          className="font-black uppercase hover:underline"
                        >
                          {contact.fullName}
                        </Link>
                        {contact.isPrimary && (
                          <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                        )}
                      </div>
                      {contact.title && (
                        <p className="text-xs font-bold text-gray-600">{contact.title}</p>
                      )}
                      {contact.department && (
                        <p className="text-xs font-medium text-gray-500">{contact.department}</p>
                      )}
                    </div>
                  </td>
                  {showCustomer && (
                    <td className="border-r-2 border-black px-4 py-3">
                      <span className="font-bold">{contact.customerName || '—'}</span>
                    </td>
                  )}
                  <td className="border-r-2 border-black px-4 py-3">
                    <ContactRoleBadge role={contact.role} />
                  </td>
                  <td className="border-r-2 border-black px-4 py-3">
                    <ContactStatusBadge status={contact.status} />
                  </td>
                  <td className="border-r-2 border-black px-4 py-3">
                    <div className="space-y-1 text-sm">
                      {contact.email && (
                        <div className="flex items-center gap-1">
                          <Mail className="h-3 w-3" />
                          <span className="font-medium">{contact.email}</span>
                        </div>
                      )}
                      {contact.phone && (
                        <div className="flex items-center gap-1">
                          <Phone className="h-3 w-3" />
                          <span className="font-medium">{contact.phone}</span>
                        </div>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <DropdownMenu>
                      <DropdownMenuTrigger className="rounded border-2 border-black bg-white p-1 shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all hover:translate-x-[1px] hover:translate-y-[1px] hover:shadow-none">
                        <MoreHorizontal className="h-4 w-4" />
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="border-2 border-black">
                        <DropdownMenuItem asChild>
                          <Link
                            href={`/admin/contacts/${contact.id}`}
                            className="flex items-center gap-2 font-bold"
                          >
                            <Eye className="h-4 w-4" />
                            View
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link
                            href={`/admin/contacts/${contact.id}/edit`}
                            className="flex items-center gap-2 font-bold"
                          >
                            <Edit className="h-4 w-4" />
                            Edit
                          </Link>
                        </DropdownMenuItem>
                        {onSetPrimary && !contact.isPrimary && (
                          <DropdownMenuItem
                            onClick={() => onSetPrimary(contact.id)}
                            className="flex items-center gap-2 font-bold"
                          >
                            <Star className="h-4 w-4" />
                            Set as Primary
                          </DropdownMenuItem>
                        )}
                        {onDelete && (
                          <DropdownMenuItem
                            onClick={() => setDeleteId(contact.id)}
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
            <AlertDialogTitle className="font-black uppercase">Delete Contact</AlertDialogTitle>
            <AlertDialogDescription className="font-bold">
              Are you sure you want to delete this contact? This action cannot be undone.
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
