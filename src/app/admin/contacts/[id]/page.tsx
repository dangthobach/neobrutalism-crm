'use client'

import { use, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Card } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { ArrowLeft, Edit, Save, Trash2, X } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useContact, useUpdateContact, useDeleteContact } from '@/hooks/useContacts'
import { UpdateContactRequest, ContactRole, ContactStatus } from '@/types/contact'
import { ContactStatusBadge } from '@/components/contacts/contact-status-badge'

interface ContactDetailPageProps {
  params: Promise<{ id: string }>
}

export default function ContactDetailPage({ params }: ContactDetailPageProps) {
  const { id } = use(params)
  const router = useRouter()
  const [isEditing, setIsEditing] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)

  const { data: contact, isLoading } = useContact(id)
  const updateContact = useUpdateContact()
  const deleteContact = useDeleteContact()

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors },
  } = useForm<UpdateContactRequest>({
    values: contact
      ? {
          firstName: contact.firstName,
          lastName: contact.lastName,
          title: contact.title,
          department: contact.department,
          role: contact.role,
          status: contact.status,
          email: contact.email,
          phone: contact.phone,
          mobile: contact.mobile,
          isPrimary: contact.isPrimary,
          linkedinUrl: contact.linkedinUrl,
          notes: contact.notes,
          lastContactDate: contact.lastContactDate,
          nextFollowupDate: contact.nextFollowupDate,
          preferredContactMethod: contact.preferredContactMethod,
          birthday: contact.birthday,
        }
      : undefined,
  })

  const onSubmit = async (data: UpdateContactRequest) => {
    updateContact.mutate(
      { id, data },
      {
        onSuccess: () => {
          setIsEditing(false)
        },
      }
    )
  }

  const handleDelete = async () => {
    deleteContact.mutate(id, {
      onSuccess: () => {
        router.push('/admin/contacts')
      },
    })
  }

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-black border-t-transparent" />
          <p className="mt-4 font-bold">Loading contact...</p>
        </div>
      </div>
    )
  }

  if (!contact) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <p className="text-2xl font-black">Contact not found</p>
          <Button onClick={() => router.back()} className="mt-4">
            Go Back
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="neutral" size="icon" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-3xl font-black">{contact.fullName}</h1>
              <ContactStatusBadge status={contact.status} />
              {contact.isPrimary && (
                <span className="rounded-full border-2 border-black bg-yellow-300 px-3 py-1 text-xs font-black">
                  PRIMARY
                </span>
              )}
            </div>
            <p className="text-muted-foreground">
              {contact.title} {contact.department && `• ${contact.department}`}
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          {!isEditing ? (
            <>
              <Button variant="neutral" size="lg" onClick={() => setIsEditing(true)}>
                <Edit className="mr-2 h-4 w-4" />
                Edit
              </Button>
              <Button
                variant="reverse"
                size="lg"
                onClick={() => setShowDeleteDialog(true)}
                className="bg-red-500 hover:bg-red-600"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </Button>
            </>
          ) : (
            <>
              <Button variant="neutral" size="lg" onClick={() => {
                setIsEditing(false)
                reset()
              }}>
                <X className="mr-2 h-4 w-4" />
                Cancel
              </Button>
              <Button
                size="lg"
                onClick={handleSubmit(onSubmit)}
                disabled={updateContact.isPending}
              >
                <Save className="mr-2 h-4 w-4" />
                {updateContact.isPending ? 'Saving...' : 'Save Changes'}
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="overview" className="w-full">
        <TabsList className="grid w-full grid-cols-2">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="activities">Activities</TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Basic Information */}
            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
                <h2 className="text-xl font-black">Basic Information</h2>
              </div>
              <div className="grid gap-4 p-6 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="firstName">First Name</Label>
                  <Input
                    id="firstName"
                    {...register('firstName')}
                    disabled={!isEditing}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="lastName">Last Name</Label>
                  <Input
                    id="lastName"
                    {...register('lastName')}
                    disabled={!isEditing}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    {...register('email')}
                    disabled={!isEditing}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="phone">Phone</Label>
                  <Input id="phone" {...register('phone')} disabled={!isEditing} />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="mobile">Mobile</Label>
                  <Input id="mobile" {...register('mobile')} disabled={!isEditing} />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="role">Role</Label>
                  <Select
                    value={watch('role')}
                    onValueChange={(value) => setValue('role', value as ContactRole)}
                    disabled={!isEditing}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={ContactRole.DECISION_MAKER}>Decision Maker</SelectItem>
                      <SelectItem value={ContactRole.INFLUENCER}>Influencer</SelectItem>
                      <SelectItem value={ContactRole.CHAMPION}>Champion</SelectItem>
                      <SelectItem value={ContactRole.EVALUATOR}>Evaluator</SelectItem>
                      <SelectItem value={ContactRole.GATEKEEPER}>Gatekeeper</SelectItem>
                      <SelectItem value={ContactRole.USER}>User</SelectItem>
                      <SelectItem value={ContactRole.OTHER}>Other</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="status">Status</Label>
                  <Select
                    value={watch('status')}
                    onValueChange={(value) => setValue('status', value as ContactStatus)}
                    disabled={!isEditing}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value={ContactStatus.ACTIVE}>Active</SelectItem>
                      <SelectItem value={ContactStatus.INACTIVE}>Inactive</SelectItem>
                      <SelectItem value={ContactStatus.DO_NOT_CONTACT}>Do Not Contact</SelectItem>
                      <SelectItem value={ContactStatus.BOUNCED}>Bounced</SelectItem>
                      <SelectItem value={ContactStatus.UNSUBSCRIBED}>Unsubscribed</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="title">Job Title</Label>
                  <Input id="title" {...register('title')} disabled={!isEditing} />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="department">Department</Label>
                  <Input id="department" {...register('department')} disabled={!isEditing} />
                </div>
              </div>
            </Card>

            {/* Contact Settings */}
            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-green-200 px-6 py-4">
                <h2 className="text-xl font-black">Contact Settings</h2>
              </div>
              <div className="space-y-4 p-6">
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="isPrimary"
                    {...register('isPrimary')}
                    disabled={!isEditing}
                    className="h-4 w-4"
                  />
                  <Label htmlFor="isPrimary" className="cursor-pointer">
                    Set as primary contact for customer
                  </Label>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="preferredContactMethod">Preferred Contact Method</Label>
                  <Select
                    value={watch('preferredContactMethod') || ''}
                    onValueChange={(value) => setValue('preferredContactMethod', value)}
                    disabled={!isEditing}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select method..." />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="EMAIL">Email</SelectItem>
                      <SelectItem value="PHONE">Phone</SelectItem>
                      <SelectItem value="MOBILE">Mobile</SelectItem>
                      <SelectItem value="LINKEDIN">LinkedIn</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="linkedinUrl">LinkedIn URL</Label>
                  <Input
                    id="linkedinUrl"
                    {...register('linkedinUrl')}
                    disabled={!isEditing}
                  />
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="birthday">Birthday</Label>
                    <Input
                      id="birthday"
                      type="date"
                      {...register('birthday')}
                      disabled={!isEditing}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="lastContactDate">Last Contact Date</Label>
                    <Input
                      id="lastContactDate"
                      type="date"
                      {...register('lastContactDate')}
                      disabled={!isEditing}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="nextFollowupDate">Next Follow-up Date</Label>
                  <Input
                    id="nextFollowupDate"
                    type="date"
                    {...register('nextFollowupDate')}
                    disabled={!isEditing}
                  />
                </div>
              </div>
            </Card>

            {/* Additional Information */}
            <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
                <h2 className="text-xl font-black">Additional Information</h2>
              </div>
              <div className="space-y-4 p-6">
                <div className="space-y-2">
                  <Label htmlFor="notes">Notes</Label>
                  <Textarea
                    id="notes"
                    {...register('notes')}
                    rows={4}
                    disabled={!isEditing}
                  />
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label>Customer ID</Label>
                    <Input value={contact.customerId} disabled />
                  </div>

                  <div className="space-y-2">
                    <Label>Customer Name</Label>
                    <Input value={contact.customerName || 'N/A'} disabled />
                  </div>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label>Created</Label>
                    <Input
                      value={new Date(contact.createdAt).toLocaleString()}
                      disabled
                    />
                  </div>

                  <div className="space-y-2">
                    <Label>Last Updated</Label>
                    <Input
                      value={new Date(contact.updatedAt).toLocaleString()}
                      disabled
                    />
                  </div>
                </div>
              </div>
            </Card>
          </form>
        </TabsContent>

        <TabsContent value="activities">
          <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
            <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
              <h2 className="text-xl font-black">Recent Activities</h2>
            </div>
            <div className="p-6">
              <p className="text-center text-muted-foreground">
                Activity tracking coming soon...
              </p>
            </div>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent className="border-2 border-black shadow-[8px_8px_0px_0px_rgba(0,0,0,1)]">
          <AlertDialogHeader>
            <AlertDialogTitle>Are you sure?</AlertDialogTitle>
            <AlertDialogDescription>
              This will permanently delete the contact <strong>{contact.fullName}</strong>.
              This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-red-500 hover:bg-red-600">
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
