/**
 * Contact Form Component
 * Form for creating/editing contacts with 4-section layout
 */

'use client'

import { useForm } from 'react-hook-form'
import { Contact, CreateContactRequest, UpdateContactRequest, ContactRole, ContactStatus } from '@/types/contact'
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
import { Checkbox } from '@/components/ui/checkbox'

interface ContactFormProps {
  contact?: Contact
  customerId?: string
  onSubmit: (data: CreateContactRequest | UpdateContactRequest) => void
  isSubmitting?: boolean
}

export function ContactForm({ contact, customerId, onSubmit, isSubmitting }: ContactFormProps) {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<any>({
    defaultValues: contact || {
      role: ContactRole.USER,
      status: ContactStatus.ACTIVE,
      isPrimary: false,
    },
  })

  const watchIsPrimary = watch('isPrimary')

  const onFormSubmit = (data: any) => {
    if (customerId && !contact) {
      data.customerId = customerId
    }
    onSubmit(data)
  }

  return (
    <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-6">
      {/* Section 1: Basic Information */}
      <div className="rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-green-200 px-6 py-4">
          <h2 className="font-black uppercase">Basic Information</h2>
        </div>
        <div className="space-y-4 p-6">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="firstName" className="font-bold uppercase">
                First Name *
              </Label>
              <Input
                id="firstName"
                {...register('firstName', { required: 'First name is required' })}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
              {errors.firstName && (
                <p className="mt-1 text-sm font-bold text-red-600">{String(errors.firstName.message)}</p>
              )}
            </div>
            <div>
              <Label htmlFor="lastName" className="font-bold uppercase">
                Last Name *
              </Label>
              <Input
                id="lastName"
                {...register('lastName', { required: 'Last name is required' })}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
              {errors.lastName && (
                <p className="mt-1 text-sm font-bold text-red-600">{String(errors.lastName.message)}</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="title" className="font-bold uppercase">
                Job Title
              </Label>
              <Input
                id="title"
                {...register('title')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
            <div>
              <Label htmlFor="department" className="font-bold uppercase">
                Department
              </Label>
              <Input
                id="department"
                {...register('department')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="role" className="font-bold uppercase">
                Role *
              </Label>
              <Select
                defaultValue={contact?.role || ContactRole.USER}
                onValueChange={(value) => setValue('role', value as ContactRole)}
              >
                <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="border-2 border-black">
                  {Object.values(ContactRole).map((role) => (
                    <SelectItem key={role} value={role} className="font-bold uppercase">
                      {role.replace(/_/g, ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label htmlFor="status" className="font-bold uppercase">
                Status *
              </Label>
              <Select
                defaultValue={contact?.status || ContactStatus.ACTIVE}
                onValueChange={(value) => setValue('status', value as ContactStatus)}
              >
                <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="border-2 border-black">
                  {Object.values(ContactStatus).map((status) => (
                    <SelectItem key={status} value={status} className="font-bold uppercase">
                      {status.replace(/_/g, ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox
              id="isPrimary"
              checked={watchIsPrimary}
              onCheckedChange={(checked) => setValue('isPrimary', checked as boolean)}
              className="border-2 border-black"
            />
            <Label htmlFor="isPrimary" className="font-bold uppercase cursor-pointer">
              ⭐ Primary Contact
            </Label>
          </div>
        </div>
      </div>

      {/* Section 2: Contact Information */}
      <div className="rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
          <h2 className="font-black uppercase">Contact Information</h2>
        </div>
        <div className="space-y-4 p-6">
          <div>
            <Label htmlFor="email" className="font-bold uppercase">
              Email
            </Label>
            <Input
              id="email"
              type="email"
              {...register('email')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="phone" className="font-bold uppercase">
                Phone
              </Label>
              <Input
                id="phone"
                {...register('phone')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
            <div>
              <Label htmlFor="mobile" className="font-bold uppercase">
                Mobile
              </Label>
              <Input
                id="mobile"
                {...register('mobile')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
          </div>

          <div>
            <Label htmlFor="preferredContactMethod" className="font-bold uppercase">
              Preferred Contact Method
            </Label>
            <Select
              defaultValue={contact?.preferredContactMethod}
              onValueChange={(value) => setValue('preferredContactMethod', value)}
            >
              <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                <SelectValue placeholder="Select method" />
              </SelectTrigger>
              <SelectContent className="border-2 border-black">
                <SelectItem value="email" className="font-bold">Email</SelectItem>
                <SelectItem value="phone" className="font-bold">Phone</SelectItem>
                <SelectItem value="mobile" className="font-bold">Mobile</SelectItem>
                <SelectItem value="linkedin" className="font-bold">LinkedIn</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div>
            <Label htmlFor="linkedinUrl" className="font-bold uppercase">
              LinkedIn URL
            </Label>
            <Input
              id="linkedinUrl"
              {...register('linkedinUrl')}
              placeholder="https://linkedin.com/in/..."
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>
        </div>
      </div>

      {/* Section 3: Additional Details */}
      <div className="rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
          <h2 className="font-black uppercase">Additional Details</h2>
        </div>
        <div className="space-y-4 p-6">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="birthday" className="font-bold uppercase">
                Birthday
              </Label>
              <Input
                id="birthday"
                type="date"
                {...register('birthday')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
            <div>
              <Label htmlFor="lastContactDate" className="font-bold uppercase">
                Last Contact Date
              </Label>
              <Input
                id="lastContactDate"
                type="date"
                {...register('lastContactDate')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
          </div>

          <div>
            <Label htmlFor="nextFollowupDate" className="font-bold uppercase">
              Next Follow-up Date
            </Label>
            <Input
              id="nextFollowupDate"
              type="date"
              {...register('nextFollowupDate')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>
        </div>
      </div>

      {/* Section 4: Notes */}
      <div className="rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
          <h2 className="font-black uppercase">Notes</h2>
        </div>
        <div className="space-y-4 p-6">
          <div>
            <Label htmlFor="notes" className="font-bold uppercase">
              Notes
            </Label>
            <Textarea
              id="notes"
              {...register('notes')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              rows={4}
            />
          </div>
        </div>
      </div>

      {/* Submit Button */}
      <div className="flex justify-end gap-4">
        <Button
          type="button"
          variant="neutral"
          className="border-2 border-black font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          disabled={isSubmitting}
        >
          Cancel
        </Button>
        <Button
          type="submit"
          className="border-2 border-black bg-green-400 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-green-500 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Saving...' : contact ? 'Update Contact' : 'Create Contact'}
        </Button>
      </div>
    </form>
  )
}
