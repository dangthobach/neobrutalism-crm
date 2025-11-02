/**
 * Customer Form Component
 * Create/Edit customer form with validation
 */

'use client'

import { useForm } from 'react-hook-form'
import { useRouter } from 'next/navigation'
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
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Loader2, Save, X } from 'lucide-react'
import {
  Customer,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  CustomerType,
  CustomerStatus,
} from '@/types/customer'

interface CustomerFormProps {
  customer?: Customer
  onSubmit: (data: CreateCustomerRequest | UpdateCustomerRequest) => Promise<void>
  isLoading?: boolean
}

export function CustomerForm({ customer, onSubmit, isLoading }: CustomerFormProps) {
  const router = useRouter()
  const isEdit = !!customer

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<CreateCustomerRequest | UpdateCustomerRequest>({
    defaultValues: customer
      ? {
          companyName: customer.companyName,
          legalName: customer.legalName,
          customerType: customer.customerType,
          status: customer.status,
          industry: customer.industry,
          taxId: customer.taxId,
          email: customer.email,
          phone: customer.phone,
          website: customer.website,
          billingAddress: customer.billingAddress,
          shippingAddress: customer.shippingAddress,
          city: customer.city,
          state: customer.state,
          country: customer.country,
          postalCode: customer.postalCode,
          annualRevenue: customer.annualRevenue,
          employeeCount: customer.employeeCount,
          creditLimit: customer.creditLimit,
          paymentTermsDays: customer.paymentTermsDays,
          leadSource: customer.leadSource,
          rating: customer.rating,
          notes: customer.notes,
          isVip: customer.isVip,
        }
      : {
          customerType: CustomerType.B2B,
          status: CustomerStatus.LEAD,
          isVip: false,
        },
  })

  const handleFormSubmit = async (data: any) => {
    await onSubmit(data)
  }

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
      {/* Basic Information */}
      <Card className="border-4 border-black">
        <CardHeader className="bg-yellow-300 border-b-4 border-black">
          <CardTitle className="text-2xl font-bold">Basic Information</CardTitle>
        </CardHeader>
        <CardContent className="pt-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            {!isEdit && (
              <div className="space-y-2">
                <Label htmlFor="code" className="font-bold">
                  Customer Code <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="code"
                  {...register('code' as any, { required: true })}
                  placeholder="CUST-001"
                  className="border-2 border-black"
                />
                {errors.companyName && (
                  <p className="text-sm text-red-500">Customer code is required</p>
                )}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="companyName" className="font-bold">
                Company Name <span className="text-red-500">*</span>
              </Label>
              <Input
                id="companyName"
                {...register('companyName', { required: true })}
                placeholder="Acme Corp"
                className="border-2 border-black"
              />
              {errors.companyName && (
                <p className="text-sm text-red-500">Company name is required</p>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="legalName" className="font-bold">
              Legal Name
            </Label>
            <Input
              id="legalName"
              {...register('legalName')}
              placeholder="Acme Corporation Inc."
              className="border-2 border-black"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="customerType" className="font-bold">
                Customer Type <span className="text-red-500">*</span>
              </Label>
              <Select
                value={watch('customerType')}
                onValueChange={(value) => setValue('customerType', value as CustomerType)}
              >
                <SelectTrigger className="border-2 border-black">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="border-4 border-black">
                  {Object.values(CustomerType).map((type) => (
                    <SelectItem key={type} value={type}>
                      {type}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="status" className="font-bold">
                Status <span className="text-red-500">*</span>
              </Label>
              <Select
                value={watch('status')}
                onValueChange={(value) => setValue('status', value as CustomerStatus)}
              >
                <SelectTrigger className="border-2 border-black">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="border-4 border-black">
                  {Object.values(CustomerStatus).map((status) => (
                    <SelectItem key={status} value={status}>
                      {status}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="industry" className="font-bold">
                Industry
              </Label>
              <Input
                id="industry"
                {...register('industry')}
                placeholder="Technology"
                className="border-2 border-black"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="taxId" className="font-bold">
                Tax ID
              </Label>
              <Input
                id="taxId"
                {...register('taxId')}
                placeholder="12-3456789"
                className="border-2 border-black"
              />
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <Checkbox
              id="isVip"
              checked={watch('isVip')}
              onCheckedChange={(checked) => setValue('isVip', checked as boolean)}
              className="border-2 border-black"
            />
            <Label htmlFor="isVip" className="font-bold cursor-pointer">
              VIP Customer
            </Label>
          </div>
        </CardContent>
      </Card>

      {/* Contact Information */}
      <Card className="border-4 border-black">
        <CardHeader className="bg-blue-300 border-b-4 border-black">
          <CardTitle className="text-2xl font-bold">Contact Information</CardTitle>
        </CardHeader>
        <CardContent className="pt-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="email" className="font-bold">
                Email
              </Label>
              <Input
                id="email"
                type="email"
                {...register('email')}
                placeholder="contact@acme.com"
                className="border-2 border-black"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="phone" className="font-bold">
                Phone
              </Label>
              <Input
                id="phone"
                {...register('phone')}
                placeholder="+1 234 567 8900"
                className="border-2 border-black"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="website" className="font-bold">
              Website
            </Label>
            <Input
              id="website"
              type="url"
              {...register('website')}
              placeholder="https://acme.com"
              className="border-2 border-black"
            />
          </div>
        </CardContent>
      </Card>

      {/* Address Information */}
      <Card className="border-4 border-black">
        <CardHeader className="bg-green-300 border-b-4 border-black">
          <CardTitle className="text-2xl font-bold">Address Information</CardTitle>
        </CardHeader>
        <CardContent className="pt-6 space-y-4">
          <div className="space-y-2">
            <Label htmlFor="billingAddress" className="font-bold">
              Billing Address
            </Label>
            <Textarea
              id="billingAddress"
              {...register('billingAddress')}
              placeholder="123 Main St, Suite 100"
              className="border-2 border-black"
              rows={2}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="shippingAddress" className="font-bold">
              Shipping Address
            </Label>
            <Textarea
              id="shippingAddress"
              {...register('shippingAddress')}
              placeholder="123 Main St, Suite 100"
              className="border-2 border-black"
              rows={2}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="city" className="font-bold">
                City
              </Label>
              <Input
                id="city"
                {...register('city')}
                placeholder="New York"
                className="border-2 border-black"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="state" className="font-bold">
                State/Province
              </Label>
              <Input
                id="state"
                {...register('state')}
                placeholder="NY"
                className="border-2 border-black"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="country" className="font-bold">
                Country
              </Label>
              <Input
                id="country"
                {...register('country')}
                placeholder="United States"
                className="border-2 border-black"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="postalCode" className="font-bold">
                Postal Code
              </Label>
              <Input
                id="postalCode"
                {...register('postalCode')}
                placeholder="10001"
                className="border-2 border-black"
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Financial Information */}
      <Card className="border-4 border-black">
        <CardHeader className="bg-purple-300 border-b-4 border-black">
          <CardTitle className="text-2xl font-bold">Financial Information</CardTitle>
        </CardHeader>
        <CardContent className="pt-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="annualRevenue" className="font-bold">
                Annual Revenue ($)
              </Label>
              <Input
                id="annualRevenue"
                type="number"
                {...register('annualRevenue', { valueAsNumber: true })}
                placeholder="1000000"
                className="border-2 border-black"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="employeeCount" className="font-bold">
                Employee Count
              </Label>
              <Input
                id="employeeCount"
                type="number"
                {...register('employeeCount', { valueAsNumber: true })}
                placeholder="50"
                className="border-2 border-black"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="creditLimit" className="font-bold">
                Credit Limit ($)
              </Label>
              <Input
                id="creditLimit"
                type="number"
                {...register('creditLimit', { valueAsNumber: true })}
                placeholder="50000"
                className="border-2 border-black"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="paymentTermsDays" className="font-bold">
                Payment Terms (Days)
              </Label>
              <Input
                id="paymentTermsDays"
                type="number"
                {...register('paymentTermsDays', { valueAsNumber: true })}
                placeholder="30"
                className="border-2 border-black"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="leadSource" className="font-bold">
                Lead Source
              </Label>
              <Input
                id="leadSource"
                {...register('leadSource')}
                placeholder="Website"
                className="border-2 border-black"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="rating" className="font-bold">
                Rating (1-5)
              </Label>
              <Input
                id="rating"
                type="number"
                min="1"
                max="5"
                {...register('rating', { valueAsNumber: true })}
                placeholder="5"
                className="border-2 border-black"
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Notes */}
      <Card className="border-4 border-black">
        <CardHeader className="bg-pink-300 border-b-4 border-black">
          <CardTitle className="text-2xl font-bold">Additional Notes</CardTitle>
        </CardHeader>
        <CardContent className="pt-6">
          <div className="space-y-2">
            <Label htmlFor="notes" className="font-bold">
              Notes
            </Label>
            <Textarea
              id="notes"
              {...register('notes')}
              placeholder="Any additional information..."
              className="border-2 border-black"
              rows={4}
            />
          </div>
        </CardContent>
      </Card>

      {/* Form Actions */}
      <div className="flex justify-end gap-4">
        <Button
          type="button"
          variant="neutral"
          onClick={() => router.back()}
          disabled={isLoading}
          className="border-2 border-black"
        >
          <X className="h-4 w-4 mr-2" />
          Cancel
        </Button>
        <Button
          type="submit"
          disabled={isLoading}
          className="border-2 border-black"
        >
          {isLoading ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              Saving...
            </>
          ) : (
            <>
              <Save className="h-4 w-4 mr-2" />
              {isEdit ? 'Update' : 'Create'} Customer
            </>
          )}
        </Button>
      </div>
    </form>
  )
}
