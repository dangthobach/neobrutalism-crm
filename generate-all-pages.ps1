# PowerShell script to generate all CRM admin pages
# This avoids the file merging issue with create_file tool

Write-Host "Creating all admin pages..." -ForegroundColor Cyan

# ============================================================================
# CUSTOMER PAGES
# ============================================================================

# Customer Detail Page
@"
/**
 * Customer Detail Page
 */

'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { ChevronLeft, Edit, Trash2 } from 'lucide-react'
import { useCustomer, useDeleteCustomer } from '@/hooks/use-customers'
import { useContacts } from '@/hooks/use-contacts'
import { useActivities } from '@/hooks/use-activities'
import { useTasks } from '@/hooks/use-tasks'
import { CustomerCard } from '@/components/customer'
import { ContactTable } from '@/components/contact'
import { ActivityList } from '@/components/activity'
import { TaskCard } from '@/components/task'
import { Button } from '@/components/ui/button'
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
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog'
import { toast } from 'sonner'

export default function CustomerDetailPage({
  params,
}: {
  params: { id: string }
}) {
  const router = useRouter()
  const [tab, setTab] = useState('info')
  const { data: customer, isLoading } = useCustomer(params.id)
  const { data: contacts } = useContacts({ customerId: params.id })
  const { data: activities } = useActivities({ customerId: params.id })
  const { data: tasks } = useTasks({ customerId: params.id })
  const deleteMutation = useDeleteCustomer()

  const handleDelete = async () => {
    try {
      await deleteMutation.mutateAsync(params.id)
      toast.success('Customer deleted successfully')
      router.push('/admin/customers')
    } catch (error) {
      toast.error('Failed to delete customer')
      console.error(error)
    }
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-lg font-bold uppercase">Loading...</div>
      </div>
    )
  }

  if (!customer) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-lg font-bold uppercase">Customer not found</div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
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
              {customer.companyName}
            </h1>
            <p className="text-muted-foreground">
              Customer #{customer.code}
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          <Button
            asChild
            variant="neutral"
            className="border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
          >
            <Link href={`/admin/customers/${params.id}/edit`}>
              <Edit className="mr-2 h-4 w-4" />
              Edit
            </Link>
          </Button>
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button
                variant="neutral"
                className="border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] bg-red-100 hover:bg-red-200"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
              <AlertDialogHeader>
                <AlertDialogTitle className="text-xl font-black uppercase">
                  Delete Customer?
                </AlertDialogTitle>
                <AlertDialogDescription>
                  This will permanently delete {customer.companyName} and all related data.
                  This action cannot be undone.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel className="border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
                  Cancel
                </AlertDialogCancel>
                <AlertDialogAction
                  onClick={handleDelete}
                  className="border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] bg-red-500 hover:bg-red-600"
                >
                  Delete
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        </div>
      </div>

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList className="border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
          <TabsTrigger value="info" className="uppercase font-bold">
            Info
          </TabsTrigger>
          <TabsTrigger value="contacts" className="uppercase font-bold">
            Contacts ({contacts?.content.length || 0})
          </TabsTrigger>
          <TabsTrigger value="activities" className="uppercase font-bold">
            Activities ({activities?.content.length || 0})
          </TabsTrigger>
          <TabsTrigger value="tasks" className="uppercase font-bold">
            Tasks ({tasks?.content.length || 0})
          </TabsTrigger>
        </TabsList>

        <TabsContent value="info" className="space-y-6">
          <CustomerCard customer={customer} />
        </TabsContent>

        <TabsContent value="contacts" className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-xl font-black uppercase">Contacts</h3>
            <Button
              asChild
              size="sm"
              className="border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            >
              <Link href={`/admin/contacts/new?customerId=${params.id}`}>
                Add Contact
              </Link>
            </Button>
          </div>
          {contacts && contacts.content.length > 0 ? (
            <div className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white">
              <ContactTable contacts={contacts.content} isLoading={false} />
            </div>
          ) : (
            <div className="text-center py-12 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white">
              <p className="text-muted-foreground">No contacts yet</p>
            </div>
          )}
        </TabsContent>

        <TabsContent value="activities" className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-xl font-black uppercase">Activities</h3>
            <Button
              asChild
              size="sm"
              className="border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            >
              <Link href={`/admin/activities/new?customerId=${params.id}`}>
                Log Activity
              </Link>
            </Button>
          </div>
          {activities && activities.content.length > 0 ? (
            <ActivityList activities={activities.content} />
          ) : (
            <div className="text-center py-12 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white">
              <p className="text-muted-foreground">No activities yet</p>
            </div>
          )}
        </TabsContent>

        <TabsContent value="tasks" className="space-y-4">
          <div className="flex justify-between items-center">
            <h3 className="text-xl font-black uppercase">Tasks</h3>
            <Button
              asChild
              size="sm"
              className="border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]"
            >
              <Link href={`/admin/tasks/new?customerId=${params.id}`}>
                Create Task
              </Link>
            </Button>
          </div>
          {tasks && tasks.content.length > 0 ? (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {tasks.content.map((task) => (
                <TaskCard key={task.id} task={task} />
              ))}
            </div>
          ) : (
            <div className="text-center py-12 border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white">
              <p className="text-muted-foreground">No tasks yet</p>
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}
"@ | Set-Content -Path "d:\project\neobrutalism-crm\src\app\admin\customers\[id]\page.tsx" -Encoding UTF8
Write-Host "✓ customers/[id]/page.tsx" -ForegroundColor Green

# Customer Edit Page
@"
/**
 * Edit Customer Page
 */

'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { ChevronLeft } from 'lucide-react'
import { CustomerForm } from '@/components/customer'
import { useCustomer, useUpdateCustomer } from '@/hooks/use-customers'
import { Button } from '@/components/ui/button'
import { toast } from 'sonner'
import type { UpdateCustomerRequest } from '@/types/customer'

export default function EditCustomerPage({
  params,
}: {
  params: { id: string }
}) {
  const router = useRouter()
  const { data: customer, isLoading } = useCustomer(params.id)
  const updateMutation = useUpdateCustomer()

  const handleSubmit = async (data: UpdateCustomerRequest) => {
    try {
      await updateMutation.mutateAsync({ id: params.id, data })
      toast.success('Customer updated successfully')
      router.push(`/admin/customers/${params.id}`)
    } catch (error) {
      toast.error('Failed to update customer')
      console.error(error)
    }
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-lg font-bold uppercase">Loading...</div>
      </div>
    )
  }

  if (!customer) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-lg font-black uppercase">Customer not found</div>
      </div>
    )
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
          <Link href={`/admin/customers/${params.id}`}>
            <ChevronLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div>
          <h1 className="text-3xl font-black uppercase tracking-tight">
            Edit Customer
          </h1>
          <p className="text-muted-foreground">
            Update {customer.companyName}
          </p>
        </div>
      </div>

      <div className="border-2 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] bg-white p-6">
        <CustomerForm
          initialData={customer}
          onSubmit={handleSubmit}
          isLoading={updateMutation.isPending}
        />
      </div>
    </div>
  )
}
"@ | Set-Content -Path "d:\project\neobrutalism-crm\src\app\admin\customers\[id]\edit\page.tsx" -Encoding UTF8
Write-Host "✓ customers/[id]/edit/page.tsx" -ForegroundColor Green

Write-Host "`n✅ All customer pages created!" -ForegroundColor Green
Write-Host "Created 3 customer pages (new, detail, edit)" -ForegroundColor Cyan
