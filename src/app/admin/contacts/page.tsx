/**
 * Contacts List Page
 * Main page for viewing and managing contacts
 */

'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useContacts, useDeleteContact } from '@/hooks/useContacts'
import { ContactStatusBadge } from '@/components/contacts/contact-status-badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
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
import { Plus, Search, Users, Loader2, MoreHorizontal, Edit, Trash2, Eye, Star } from 'lucide-react'
import { ContactSearchParams, ContactStatus, ContactRole } from '@/types/contact'

export default function ContactsPage() {
  const router = useRouter()
  const [searchParams, setSearchParams] = useState<ContactSearchParams>({})
  const [page, setPage] = useState(0)
  const [deleteId, setDeleteId] = useState<string | null>(null)

  const { data, isLoading } = useContacts({ ...searchParams, page, size: 20 })
  const deleteMutation = useDeleteContact()

  const handleSearch = (keyword: string) => {
    setSearchParams({ ...searchParams, keyword })
    setPage(0)
  }

  const handleStatusFilter = (status: string) => {
    if (status === 'all') {
      const { status: _, ...rest } = searchParams
      setSearchParams(rest)
    } else {
      setSearchParams({ ...searchParams, status: status as ContactStatus })
    }
    setPage(0)
  }

  const handleRoleFilter = (role: string) => {
    if (role === 'all') {
      const { role: _, ...rest } = searchParams
      setSearchParams(rest)
    } else {
      setSearchParams({ ...searchParams, role: role as ContactRole })
    }
    setPage(0)
  }

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

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-4xl font-black mb-2">Contacts</h1>
          <p className="text-gray-600 font-medium">
            Manage your customer contacts and relationships
          </p>
        </div>
        <Button
          onClick={() => router.push('/admin/contacts/new')}
          className="border-2 border-black"
        >
          <Plus className="h-4 w-4 mr-2" />
          Add Contact
        </Button>
      </div>

      {/* Stats Card */}
      {data && (
        <div className="grid grid-cols-3 gap-4">
          <div className="border-4 border-black rounded-lg p-6 bg-white">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-blue-300 border-2 border-black rounded-lg">
                <Users className="h-6 w-6" />
              </div>
              <div>
                <div className="text-sm text-gray-600 font-bold">Total Contacts</div>
                <div className="text-3xl font-black">{data.totalElements.toLocaleString()}</div>
              </div>
            </div>
          </div>

          <div className="border-4 border-black rounded-lg p-6 bg-white">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-green-300 border-2 border-black rounded-lg">
                <Star className="h-6 w-6" />
              </div>
              <div>
                <div className="text-sm text-gray-600 font-bold">Primary Contacts</div>
                <div className="text-3xl font-black">
                  {data.content.filter(c => c.isPrimary).length}
                </div>
              </div>
            </div>
          </div>

          <div className="border-4 border-black rounded-lg p-6 bg-white">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-yellow-300 border-2 border-black rounded-lg">
                <Users className="h-6 w-6" />
              </div>
              <div>
                <div className="text-sm text-gray-600 font-bold">Active</div>
                <div className="text-3xl font-black">
                  {data.content.filter(c => c.status === ContactStatus.ACTIVE).length}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="border-4 border-black rounded-lg p-4 bg-white">
        <div className="flex gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-500" />
            <Input
              placeholder="Search contacts..."
              onChange={(e) => handleSearch(e.target.value)}
              className="pl-10 border-2 border-black"
            />
          </div>

          <Select onValueChange={handleStatusFilter} defaultValue="all">
            <SelectTrigger className="w-[180px] border-2 border-black">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent className="border-4 border-black">
              <SelectItem value="all">All Status</SelectItem>
              {Object.values(ContactStatus).map((status) => (
                <SelectItem key={status} value={status}>
                  {status.replace(/_/g, ' ')}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Select onValueChange={handleRoleFilter} defaultValue="all">
            <SelectTrigger className="w-[180px] border-2 border-black">
              <SelectValue placeholder="Role" />
            </SelectTrigger>
            <SelectContent className="border-4 border-black">
              <SelectItem value="all">All Roles</SelectItem>
              {Object.values(ContactRole).map((role) => (
                <SelectItem key={role} value={role}>
                  {role.replace(/_/g, ' ')}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Table */}
      <div className="border-4 border-black rounded-lg overflow-hidden bg-white">
        <Table>
          <TableHeader>
            <TableRow className="bg-purple-300 hover:bg-purple-300 border-b-4 border-black">
              <TableHead className="font-bold text-black">Name</TableHead>
              <TableHead className="font-bold text-black">Customer</TableHead>
              <TableHead className="font-bold text-black">Title</TableHead>
              <TableHead className="font-bold text-black">Role</TableHead>
              <TableHead className="font-bold text-black">Status</TableHead>
              <TableHead className="font-bold text-black">Contact Info</TableHead>
              <TableHead className="font-bold text-black text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {data?.content.map((contact) => (
              <TableRow
                key={contact.id}
                className="hover:bg-gray-50 cursor-pointer border-b-2 border-black"
                onClick={() => router.push(`/admin/contacts/${contact.id}`)}
              >
                <TableCell className="font-bold">
                  <div className="flex items-center gap-2">
                    {contact.fullName}
                    {contact.isPrimary && (
                      <Badge className="bg-yellow-300 text-black border-2 border-black">
                        <Star className="h-3 w-3 mr-1 fill-black" />
                        Primary
                      </Badge>
                    )}
                  </div>
                </TableCell>
                <TableCell>{contact.customerName || '-'}</TableCell>
                <TableCell>{contact.title || '-'}</TableCell>
                <TableCell>
                  <Badge variant="neutral" className="border-2 border-black">
                    {contact.role.replace(/_/g, ' ')}
                  </Badge>
                </TableCell>
                <TableCell>
                  <ContactStatusBadge status={contact.status} />
                </TableCell>
                <TableCell>
                  <div className="text-sm">
                    {contact.email && <div>{contact.email}</div>}
                    {contact.phone && <div className="text-gray-600">{contact.phone}</div>}
                  </div>
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
                        onClick={() => router.push(`/admin/contacts/${contact.id}`)}
                        className="cursor-pointer"
                      >
                        <Eye className="h-4 w-4 mr-2" />
                        View Details
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        onClick={() => router.push(`/admin/contacts/${contact.id}/edit`)}
                        className="cursor-pointer"
                      >
                        <Edit className="h-4 w-4 mr-2" />
                        Edit
                      </DropdownMenuItem>
                      <DropdownMenuSeparator className="bg-black h-0.5" />
                      <DropdownMenuItem
                        onClick={() => setDeleteId(contact.id)}
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
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="text-sm text-gray-700">
            Showing <span className="font-bold">{data.content.length}</span> of{' '}
            <span className="font-bold">{data.totalElements}</span> contacts
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
      )}

      {/* Delete Dialog */}
      <AlertDialog open={!!deleteId} onOpenChange={() => setDeleteId(null)}>
        <AlertDialogContent className="border-4 border-black">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-2xl font-bold">Delete Contact?</AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete the contact.
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
    </div>
  )
}
