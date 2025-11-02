/**
 * Customer Detail Page
 * View and edit customer details
 */

'use client'

import { use } from 'react'
import { useRouter } from 'next/navigation'
import {
  useCustomer,
  useUpdateCustomer,
  useDeleteCustomer,
  useConvertToProspect,
  useConvertToActive,
  useDeactivateCustomer,
  useReactivateCustomer,
  useBlacklistCustomer,
} from '@/hooks/useCustomers'
import { useContactsByCustomer } from '@/hooks/useContacts'
import { CustomerForm } from '@/components/customers/customer-form'
import { CustomerStatusBadge } from '@/components/customers/customer-status-badge'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  ArrowLeft,
  Edit,
  Trash2,
  MoreVertical,
  Mail,
  Phone,
  Globe,
  MapPin,
  Building2,
  DollarSign,
  Users,
  Calendar,
  Star,
  Loader2,
} from 'lucide-react'
import { CustomerStatus, UpdateCustomerRequest } from '@/types/customer'
import { useState } from 'react'

export default function CustomerDetailPage({
  params,
}: {
  params: Promise<{ id: string }>
}) {
  const { id } = use(params)
  const router = useRouter()
  const [isEditing, setIsEditing] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)

  const { data: customer, isLoading } = useCustomer(id)
  const { data: contactsData } = useContactsByCustomer(id)
  const updateMutation = useUpdateCustomer()
  const deleteMutation = useDeleteCustomer()
  const convertToProspectMutation = useConvertToProspect()
  const convertToActiveMutation = useConvertToActive()
  const deactivateMutation = useDeactivateCustomer()
  const reactivateMutation = useReactivateCustomer()
  const blacklistMutation = useBlacklistCustomer()

  const handleUpdate = async (data: UpdateCustomerRequest) => {
    await updateMutation.mutateAsync({ id, data })
    setIsEditing(false)
  }

  const handleDelete = async () => {
    await deleteMutation.mutateAsync(id)
    router.push('/admin/customers')
  }

  const handleStatusChange = async (action: string) => {
    const reason = prompt('Please provide a reason for this action (optional):')
    
    switch (action) {
      case 'convert-to-prospect':
        await convertToProspectMutation.mutateAsync({ id, reason: reason || undefined })
        break
      case 'convert-to-active':
        await convertToActiveMutation.mutateAsync({ id, reason: reason || undefined })
        break
      case 'deactivate':
        await deactivateMutation.mutateAsync({ id, reason: reason || undefined })
        break
      case 'reactivate':
        await reactivateMutation.mutateAsync({ id, reason: reason || undefined })
        break
      case 'blacklist':
        await blacklistMutation.mutateAsync({ id, reason: reason || undefined })
        break
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center items-center p-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    )
  }

  if (!customer) {
    return (
      <div className="text-center p-12">
        <h2 className="text-2xl font-bold mb-2">Customer not found</h2>
        <Button onClick={() => router.back()}>Go Back</Button>
      </div>
    )
  }

  if (isEditing) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Button
            variant="neutral"
            onClick={() => setIsEditing(false)}
            className="border-2 border-black"
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-4xl font-black">Edit Customer</h1>
            <p className="text-gray-600 font-medium">Update customer information</p>
          </div>
        </div>
        <CustomerForm
          customer={customer}
          onSubmit={handleUpdate}
          isLoading={updateMutation.isPending}
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button
            variant="neutral"
            onClick={() => router.back()}
            className="border-2 border-black"
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <div className="flex items-center gap-3 mb-2">
              <h1 className="text-4xl font-black">{customer.companyName}</h1>
              {customer.isVip && (
                <Badge className="bg-amber-400 text-black border-2 border-black">
                  <Star className="h-3 w-3 mr-1 fill-black" />
                  VIP
                </Badge>
              )}
              <CustomerStatusBadge status={customer.status} />
            </div>
            <p className="text-gray-600 font-medium font-mono">{customer.code}</p>
          </div>
        </div>

        <div className="flex gap-2">
          <Button
            onClick={() => setIsEditing(true)}
            className="border-2 border-black"
          >
            <Edit className="h-4 w-4 mr-2" />
            Edit
          </Button>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="neutral" className="border-2 border-black">
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="border-4 border-black w-56">
              <DropdownMenuLabel className="font-bold">Status Actions</DropdownMenuLabel>
              <DropdownMenuSeparator className="bg-black h-0.5" />
              
              {customer.status === CustomerStatus.LEAD && (
                <DropdownMenuItem
                  onClick={() => handleStatusChange('convert-to-prospect')}
                  className="cursor-pointer"
                >
                  Convert to Prospect
                </DropdownMenuItem>
              )}
              
              {(customer.status === CustomerStatus.PROSPECT || customer.status === CustomerStatus.LEAD) && (
                <DropdownMenuItem
                  onClick={() => handleStatusChange('convert-to-active')}
                  className="cursor-pointer"
                >
                  Convert to Active
                </DropdownMenuItem>
              )}
              
              {customer.status === CustomerStatus.ACTIVE && (
                <DropdownMenuItem
                  onClick={() => handleStatusChange('deactivate')}
                  className="cursor-pointer"
                >
                  Deactivate
                </DropdownMenuItem>
              )}
              
              {customer.status === CustomerStatus.INACTIVE && (
                <DropdownMenuItem
                  onClick={() => handleStatusChange('reactivate')}
                  className="cursor-pointer"
                >
                  Reactivate
                </DropdownMenuItem>
              )}
              
              <DropdownMenuSeparator className="bg-black h-0.5" />
              <DropdownMenuItem
                onClick={() => handleStatusChange('blacklist')}
                className="text-red-600 cursor-pointer"
              >
                Blacklist Customer
              </DropdownMenuItem>
              
              <DropdownMenuSeparator className="bg-black h-0.5" />
              <DropdownMenuItem
                onClick={() => setShowDeleteDialog(true)}
                className="text-red-600 cursor-pointer"
              >
                <Trash2 className="h-4 w-4 mr-2" />
                Delete Customer
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* Quick Info Cards */}
      <div className="grid grid-cols-4 gap-4">
        <Card className="border-4 border-black">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-blue-300 border-2 border-black rounded-lg">
                <Building2 className="h-6 w-6" />
              </div>
              <div>
                <div className="text-sm text-gray-600 font-bold">Type</div>
                <div className="text-xl font-black">{customer.customerType}</div>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-4 border-black">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-green-300 border-2 border-black rounded-lg">
                <DollarSign className="h-6 w-6" />
              </div>
              <div>
                <div className="text-sm text-gray-600 font-bold">Revenue</div>
                <div className="text-xl font-black">
                  {customer.annualRevenue
                    ? `$${customer.annualRevenue.toLocaleString()}`
                    : '-'}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-4 border-black">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-purple-300 border-2 border-black rounded-lg">
                <Users className="h-6 w-6" />
              </div>
              <div>
                <div className="text-sm text-gray-600 font-bold">Employees</div>
                <div className="text-xl font-black">
                  {customer.employeeCount || '-'}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-4 border-black">
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-yellow-300 border-2 border-black rounded-lg">
                <Star className="h-6 w-6" />
              </div>
              <div>
                <div className="text-sm text-gray-600 font-bold">Rating</div>
                <div className="text-xl font-black">{customer.rating || '-'}/5</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList className="border-4 border-black bg-white p-1">
          <TabsTrigger value="overview" className="font-bold data-[state=active]:bg-yellow-300">
            Overview
          </TabsTrigger>
          <TabsTrigger value="contacts" className="font-bold data-[state=active]:bg-yellow-300">
            Contacts ({contactsData?.content.length || 0})
          </TabsTrigger>
          <TabsTrigger value="activities" className="font-bold data-[state=active]:bg-yellow-300">
            Activities
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          {/* Contact Information */}
          <Card className="border-4 border-black">
            <CardHeader className="bg-blue-300 border-b-4 border-black">
              <CardTitle className="text-2xl font-bold">Contact Information</CardTitle>
            </CardHeader>
            <CardContent className="pt-6 space-y-4">
              {customer.email && (
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-gray-600" />
                  <a
                    href={`mailto:${customer.email}`}
                    className="text-blue-600 hover:underline font-medium"
                  >
                    {customer.email}
                  </a>
                </div>
              )}
              {customer.phone && (
                <div className="flex items-center gap-3">
                  <Phone className="h-5 w-5 text-gray-600" />
                  <a
                    href={`tel:${customer.phone}`}
                    className="text-blue-600 hover:underline font-medium"
                  >
                    {customer.phone}
                  </a>
                </div>
              )}
              {customer.website && (
                <div className="flex items-center gap-3">
                  <Globe className="h-5 w-5 text-gray-600" />
                  <a
                    href={customer.website}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline font-medium"
                  >
                    {customer.website}
                  </a>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Address */}
          {(customer.billingAddress || customer.city) && (
            <Card className="border-4 border-black">
              <CardHeader className="bg-green-300 border-b-4 border-black">
                <CardTitle className="text-2xl font-bold">Address</CardTitle>
              </CardHeader>
              <CardContent className="pt-6">
                <div className="flex items-start gap-3">
                  <MapPin className="h-5 w-5 text-gray-600 mt-1" />
                  <div className="font-medium">
                    {customer.billingAddress && <div>{customer.billingAddress}</div>}
                    {customer.city && (
                      <div>
                        {customer.city}
                        {customer.state && `, ${customer.state}`}
                        {customer.postalCode && ` ${customer.postalCode}`}
                      </div>
                    )}
                    {customer.country && <div>{customer.country}</div>}
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Notes */}
          {customer.notes && (
            <Card className="border-4 border-black">
              <CardHeader className="bg-pink-300 border-b-4 border-black">
                <CardTitle className="text-2xl font-bold">Notes</CardTitle>
              </CardHeader>
              <CardContent className="pt-6">
                <p className="whitespace-pre-wrap font-medium">{customer.notes}</p>
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="contacts">
          <Card className="border-4 border-black">
            <CardHeader className="bg-purple-300 border-b-4 border-black flex flex-row items-center justify-between">
              <CardTitle className="text-2xl font-bold">Contacts</CardTitle>
              <Button
                onClick={() => router.push(`/admin/contacts/new?customerId=${id}`)}
                className="border-2 border-black"
              >
                Add Contact
              </Button>
            </CardHeader>
            <CardContent className="pt-6">
              {contactsData?.content && contactsData.content.length > 0 ? (
                <div className="space-y-4">
                  {contactsData.content.map((contact) => (
                    <div
                      key={contact.id}
                      className="p-4 border-2 border-black rounded-lg hover:bg-gray-50 cursor-pointer"
                      onClick={() => router.push(`/admin/contacts/${contact.id}`)}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="font-bold text-lg">
                            {contact.fullName}
                            {contact.isPrimary && (
                              <Badge className="ml-2 bg-yellow-300 text-black border-2 border-black">
                                Primary
                              </Badge>
                            )}
                          </div>
                          {contact.title && (
                            <div className="text-sm text-gray-600">{contact.title}</div>
                          )}
                          <div className="flex gap-4 mt-2 text-sm">
                            {contact.email && <span>{contact.email}</span>}
                            {contact.phone && <span>{contact.phone}</span>}
                          </div>
                        </div>
                        <Badge variant="neutral" className="border-2 border-black">
                          {contact.role}
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-gray-600">
                  No contacts found. Add a contact to get started.
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="activities">
          <Card className="border-4 border-black">
            <CardHeader className="bg-orange-300 border-b-4 border-black">
              <CardTitle className="text-2xl font-bold">Activities</CardTitle>
            </CardHeader>
            <CardContent className="pt-6">
              <div className="text-center py-8 text-gray-600">
                Activity tracking coming soon...
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Delete Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent className="border-4 border-black">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-2xl font-bold">
              Delete Customer?
            </AlertDialogTitle>
            <AlertDialogDescription>
              This action cannot be undone. This will permanently delete{' '}
              <strong>{customer.companyName}</strong> and all associated data.
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
