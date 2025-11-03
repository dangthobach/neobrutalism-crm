# Create Customer New Page
@"
/**
 * Create Customer Page
 */

'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { ChevronLeft } from 'lucide-react'
import { CustomerForm } from '@/components/customer'
import { useCreateCustomer } from '@/hooks/use-customers'
import { Button } from '@/components/ui/button'
import { toast } from 'sonner'
import type { CreateCustomerRequest } from '@/types/customer'

export default function NewCustomerPage() {
  const router = useRouter()
  const createMutation = useCreateCustomer()

  const handleSubmit = async (data: CreateCustomerRequest) => {
    try {
      await createMutation.mutateAsync(data)
      toast.success('Customer created successfully')
      router.push('/admin/customers')
    } catch (error) {
      toast.error('Failed to create customer')
      console.error(error)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button
          asChild
          variant="neutral"
          size="icon"
          className="border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
        >
          <Link href="/admin/customers">
            <ChevronLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div>
          <h1 className="text-3xl font-black uppercase tracking-tight">
            New Customer
          </h1>
          <p className="text-muted-foreground">
            Create a new customer record
          </p>
        </div>
      </div>

      <div className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white p-6">
        <CustomerForm
          onSubmit={handleSubmit}
          isLoading={createMutation.isPending}
        />
      </div>
    </div>
  )
}
"@ | Set-Content -Path "d:\project\neobrutalism-crm\src\app\admin\customers\new\page.tsx" -Encoding UTF8

Write-Host "Created customers/new/page.tsx"
