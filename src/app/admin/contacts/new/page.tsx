'use client'

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
import { ArrowLeft, Save } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { useCreateContact } from '@/hooks/useContacts'
import { CreateContactRequest, ContactRole, ContactStatus } from '@/types/contact'

export default function NewContactPage() {
  const router = useRouter()
  const createContact = useCreateContact()

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CreateContactRequest>({
    defaultValues: {
      role: ContactRole.OTHER,
      status: ContactStatus.ACTIVE,
      isPrimary: false,
      organizationId: '',
    },
  })

  const onSubmit = async (data: CreateContactRequest) => {
    createContact.mutate(data, {
      onSuccess: () => {
        router.push('/admin/contacts')
      },
    })
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button
            variant="neutral"
            size="icon"
            onClick={() => router.back()}
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-3xl font-black">New Contact</h1>
            <p className="text-muted-foreground">Create a new contact</p>
          </div>
        </div>
        <Button
          size="lg"
          onClick={handleSubmit(onSubmit)}
          disabled={createContact.isPending}
        >
          <Save className="mr-2 h-4 w-4" />
          {createContact.isPending ? 'Creating...' : 'Create Contact'}
        </Button>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* Basic Information */}
        <Card className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
          <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
            <h2 className="text-xl font-black">Basic Information</h2>
          </div>
          <div className="space-y-4 p-6">
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="firstName">
                  First Name <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="firstName"
                  {...register('firstName', { required: 'First name is required' })}
                  className={errors.firstName ? 'border-red-500' : ''}
                />
                {errors.firstName && (
                  <p className="text-sm text-red-500">{errors.firstName.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="lastName">
                  Last Name <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="lastName"
                  {...register('lastName', { required: 'Last name is required' })}
                  className={errors.lastName ? 'border-red-500' : ''}
                />
                {errors.lastName && (
                  <p className="text-sm text-red-500">{errors.lastName.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="email">
                  Email <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="email"
                  type="email"
                  {...register('email', {
                    required: 'Email is required',
                    pattern: {
                      value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                      message: 'Invalid email address',
                    },
                  })}
                  className={errors.email ? 'border-red-500' : ''}
                />
                {errors.email && (
                  <p className="text-sm text-red-500">{errors.email.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="phone">Phone</Label>
                <Input id="phone" {...register('phone')} />
              </div>

              <div className="space-y-2">
                <Label htmlFor="mobile">Mobile</Label>
                <Input id="mobile" {...register('mobile')} />
              </div>

              <div className="space-y-2">
                <Label htmlFor="role">
                  Role <span className="text-red-500">*</span>
                </Label>
                <Select
                  value={watch('role')}
                  onValueChange={(value) => setValue('role', value as ContactRole)}
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
                <Label htmlFor="customerId">
                  Customer ID <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="customerId"
                  {...register('customerId', { required: 'Customer ID is required' })}
                  placeholder="Enter customer ID"
                  className={errors.customerId ? 'border-red-500' : ''}
                />
                {errors.customerId && (
                  <p className="text-sm text-red-500">{errors.customerId.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="title">Job Title</Label>
                <Input id="title" {...register('title')} />
              </div>

              <div className="space-y-2">
                <Label htmlFor="department">Department</Label>
                <Input id="department" {...register('department')} />
              </div>
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
                placeholder="https://linkedin.com/in/..."
              />
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="birthday">Birthday</Label>
                <Input id="birthday" type="date" {...register('birthday')} />
              </div>

              <div className="space-y-2">
                <Label htmlFor="lastContactDate">Last Contact Date</Label>
                <Input id="lastContactDate" type="date" {...register('lastContactDate')} />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="nextFollowupDate">Next Follow-up Date</Label>
              <Input id="nextFollowupDate" type="date" {...register('nextFollowupDate')} />
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
                placeholder="Add any notes about this contact..."
              />
            </div>
          </div>
        </Card>
      </form>
    </div>
  )
}
