/**
 * Create Customer Page
 * Form for creating a new customer
 */

'use client'

import { useRouter } from 'next/navigation'
import { CustomerForm } from '@/components/customers/customer-form'
import { useCreateCustomer } from '@/hooks/useCustomers'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { CreateCustomerRequest } from '@/types/customer'

export default function NewCustomerPage() {
  const router = useRouter()
  const createMutation = useCreateCustomer()

  const handleSubmit = async (data: CreateCustomerRequest | any) => {
    await createMutation.mutateAsync(data as CreateCustomerRequest)
    router.push('/admin/customers')
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center gap-4">
        <Button
          variant="neutral"
          onClick={() => router.back()}
          className="border-2 border-black"
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-4xl font-black">Create Customer</h1>
          <p className="text-gray-600 font-medium">Add a new customer to your CRM</p>
        </div>
      </div>

      {/* Form */}
      <CustomerForm onSubmit={handleSubmit} isLoading={createMutation.isPending} />
    </div>
  )
}
