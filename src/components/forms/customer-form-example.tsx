/**
 * ✅ PHASE 1 WEEK 3: Example Form Component
 * 
 * Demonstrates usage of reusable form components with Zod validation
 */

'use client'

import React from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { CreateCustomerSchema, type CreateCustomer } from '@/lib/validations'
import { FormInput } from '@/components/ui/form-input'
import { FormSelect } from '@/components/ui/form-select'
import { FormTextarea } from '@/components/ui/form-textarea'
import { FormCheckbox } from '@/components/ui/form-checkbox'
import { Button } from '@/components/ui/button'

/**
 * Example: Customer Form using reusable components
 */
export function CustomerFormExample() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CreateCustomer>({
    resolver: zodResolver(CreateCustomerSchema),
    defaultValues: {
      customerType: 'ENTERPRISE',
      status: 'ACTIVE',
      isVip: false,
    },
  })

  const onSubmit = async (data: CreateCustomer) => {
    try {
      console.log('Form data:', data)
      // TODO: Call API to create customer
      // await createCustomer(data)
    } catch (error) {
      console.error('Failed to create customer:', error)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        {/* Customer Code */}
        <FormInput
          name="code"
          label="Customer Code"
          placeholder="Enter customer code"
          register={register}
          error={errors.code?.message}
          required
        />

        {/* Company Name */}
        <FormInput
          name="companyName"
          label="Company Name"
          placeholder="Enter company name"
          register={register}
          error={errors.companyName?.message}
          required
        />

        {/* Customer Type */}
        <FormSelect
          name="customerType"
          label="Customer Type"
          register={register}
          error={errors.customerType?.message}
          required
          options={[
            { value: 'ENTERPRISE', label: 'Enterprise' },
            { value: 'SME', label: 'SME' },
            { value: 'STARTUP', label: 'Startup' },
            { value: 'GOVERNMENT', label: 'Government' },
            { value: 'NONPROFIT', label: 'Non-Profit' },
            { value: 'INDIVIDUAL', label: 'Individual' },
          ]}
        />

        {/* Status */}
        <FormSelect
          name="status"
          label="Status"
          register={register}
          error={errors.status?.message}
          required
          options={[
            { value: 'ACTIVE', label: 'Active' },
            { value: 'INACTIVE', label: 'Inactive' },
            { value: 'PROSPECTIVE', label: 'Prospective' },
            { value: 'SUSPENDED', label: 'Suspended' },
            { value: 'CHURNED', label: 'Churned' },
            { value: 'BLACKLISTED', label: 'Blacklisted' },
          ]}
        />

        {/* Email */}
        <FormInput
          name="email"
          type="email"
          label="Email"
          placeholder="customer@example.com"
          register={register}
          error={errors.email?.message}
        />

        {/* Phone */}
        <FormInput
          name="phone"
          type="tel"
          label="Phone"
          placeholder="+1 234 567 8900"
          register={register}
          error={errors.phone?.message}
        />

        {/* Website */}
        <FormInput
          name="website"
          type="url"
          label="Website"
          placeholder="https://example.com"
          register={register}
          error={errors.website?.message}
        />

        {/* Industry */}
        <FormInput
          name="industry"
          label="Industry"
          placeholder="Enter industry"
          register={register}
          error={errors.industry?.message}
        />
      </div>

      {/* Notes */}
      <FormTextarea
        name="notes"
        label="Notes"
        placeholder="Enter additional notes"
        register={register}
        error={errors.notes?.message}
        rows={4}
      />

      {/* Checkboxes */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <FormCheckbox
          name="isVip"
          label="VIP Customer"
          register={register}
          error={errors.isVip?.message}
        />
      </div>

      {/* Submit Button */}
      <div className="flex justify-end space-x-4">
        <Button type="button" variant="neutral">
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Creating...' : 'Create Customer'}
        </Button>
      </div>
    </form>
  )
}
