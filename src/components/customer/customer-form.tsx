/**
 * Customer Form Component
 * Form for creating/editing customers with 4-section layout
 */

'use client'

import { useForm } from 'react-hook-form'
import { Customer, CreateCustomerRequest, UpdateCustomerRequest, CustomerType, CustomerStatus } from '@/types/customer'
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

interface CustomerFormProps {
  customer?: Customer
  onSubmit: (data: CreateCustomerRequest | UpdateCustomerRequest) => void
  isSubmitting?: boolean
}

export function CustomerForm({ customer, onSubmit, isSubmitting }: CustomerFormProps) {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<any>({
    defaultValues: customer || {
      customerType: CustomerType.B2B,
      status: CustomerStatus.LEAD,
      isVip: false,
    },
  })

  const watchIsVip = watch('isVip')

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Section 1: Basic Information */}
      <div className="rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-yellow-200 px-6 py-4">
          <h2 className="font-black uppercase">Basic Information</h2>
        </div>
        <div className="space-y-4 p-6">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="code" className="font-bold uppercase">
                Customer Code *
              </Label>
              <Input
                id="code"
                {...register('code', { required: 'Code is required' })}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
                disabled={!!customer}
              />
              {errors.code && (
                <p className="mt-1 text-sm font-bold text-red-600">{String(errors.code.message)}</p>
              )}
            </div>
            <div>
              <Label htmlFor="companyName" className="font-bold uppercase">
                Company Name *
              </Label>
              <Input
                id="companyName"
                {...register('companyName', { required: 'Company name is required' })}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
              {errors.companyName && (
                <p className="mt-1 text-sm font-bold text-red-600">{String(errors.companyName.message)}</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="customerType" className="font-bold uppercase">
                Customer Type *
              </Label>
              <Select
                defaultValue={customer?.customerType || CustomerType.B2B}
                onValueChange={(value) => setValue('customerType', value as CustomerType)}
              >
                <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="border-2 border-black">
                  {Object.values(CustomerType).map((type) => (
                    <SelectItem key={type} value={type} className="font-bold uppercase">
                      {type}
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
                defaultValue={customer?.status || CustomerStatus.LEAD}
                onValueChange={(value) => setValue('status', value as CustomerStatus)}
              >
                <SelectTrigger className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="border-2 border-black">
                  {Object.values(CustomerStatus).map((status) => (
                    <SelectItem key={status} value={status} className="font-bold uppercase">
                      {status}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div>
            <Label htmlFor="legalName" className="font-bold uppercase">
              Legal Name
            </Label>
            <Input
              id="legalName"
              {...register('legalName')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox
              id="isVip"
              checked={watchIsVip}
              onCheckedChange={(checked) => setValue('isVip', checked as boolean)}
              className="border-2 border-black"
            />
            <Label htmlFor="isVip" className="font-bold uppercase cursor-pointer">
              ⭐ VIP Customer
            </Label>
          </div>
        </div>
      </div>

      {/* Section 2: Contact Information */}
      <div className="rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-green-200 px-6 py-4">
          <h2 className="font-black uppercase">Contact Information</h2>
        </div>
        <div className="space-y-4 p-6">
          <div className="grid grid-cols-2 gap-4">
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
          </div>

          <div>
            <Label htmlFor="website" className="font-bold uppercase">
              Website
            </Label>
            <Input
              id="website"
              {...register('website')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>

          <div>
            <Label htmlFor="billingAddress" className="font-bold uppercase">
              Billing Address
            </Label>
            <Textarea
              id="billingAddress"
              {...register('billingAddress')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              rows={2}
            />
          </div>

          <div>
            <Label htmlFor="shippingAddress" className="font-bold uppercase">
              Shipping Address
            </Label>
            <Textarea
              id="shippingAddress"
              {...register('shippingAddress')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              rows={2}
            />
          </div>

          <div className="grid grid-cols-4 gap-4">
            <div>
              <Label htmlFor="city" className="font-bold uppercase">
                City
              </Label>
              <Input
                id="city"
                {...register('city')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
            <div>
              <Label htmlFor="state" className="font-bold uppercase">
                State
              </Label>
              <Input
                id="state"
                {...register('state')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
            <div>
              <Label htmlFor="country" className="font-bold uppercase">
                Country
              </Label>
              <Input
                id="country"
                {...register('country')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
            <div>
              <Label htmlFor="postalCode" className="font-bold uppercase">
                Postal Code
              </Label>
              <Input
                id="postalCode"
                {...register('postalCode')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Section 3: Business Details */}
      <div className="rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-blue-200 px-6 py-4">
          <h2 className="font-black uppercase">Business Details</h2>
        </div>
        <div className="space-y-4 p-6">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="industry" className="font-bold uppercase">
                Industry
              </Label>
              <Input
                id="industry"
                {...register('industry')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
            <div>
              <Label htmlFor="taxId" className="font-bold uppercase">
                Tax ID
              </Label>
              <Input
                id="taxId"
                {...register('taxId')}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="annualRevenue" className="font-bold uppercase">
                Annual Revenue
              </Label>
              <Input
                id="annualRevenue"
                type="number"
                {...register('annualRevenue', { valueAsNumber: true })}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
            <div>
              <Label htmlFor="employeeCount" className="font-bold uppercase">
                Employee Count
              </Label>
              <Input
                id="employeeCount"
                type="number"
                {...register('employeeCount', { valueAsNumber: true })}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="creditLimit" className="font-bold uppercase">
                Credit Limit
              </Label>
              <Input
                id="creditLimit"
                type="number"
                {...register('creditLimit', { valueAsNumber: true })}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
            <div>
              <Label htmlFor="paymentTermsDays" className="font-bold uppercase">
                Payment Terms (Days)
              </Label>
              <Input
                id="paymentTermsDays"
                type="number"
                {...register('paymentTermsDays', { valueAsNumber: true })}
                className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
              />
            </div>
          </div>

          <div>
            <Label htmlFor="leadSource" className="font-bold uppercase">
              Lead Source
            </Label>
            <Input
              id="leadSource"
              {...register('leadSource')}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>
        </div>
      </div>

      {/* Section 4: Additional Information */}
      <div className="rounded border-2 border-black bg-white shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        <div className="border-b-2 border-black bg-purple-200 px-6 py-4">
          <h2 className="font-black uppercase">Additional Information</h2>
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

          <div>
            <Label htmlFor="tags" className="font-bold uppercase">
              Tags (comma-separated)
            </Label>
            <Input
              id="tags"
              {...register('tags')}
              placeholder="e.g., enterprise, high-priority, tech"
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            />
          </div>

          <div>
            <Label htmlFor="rating" className="font-bold uppercase">
              Rating (1-5)
            </Label>
            <Input
              id="rating"
              type="number"
              min="1"
              max="5"
              {...register('rating', { valueAsNumber: true, min: 1, max: 5 })}
              className="border-2 border-black font-bold shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
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
          className="border-2 border-black bg-yellow-400 font-bold uppercase shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] hover:bg-yellow-500 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Saving...' : customer ? 'Update Customer' : 'Create Customer'}
        </Button>
      </div>
    </form>
  )
}
