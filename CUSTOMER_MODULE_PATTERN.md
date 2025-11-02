# CRM Module Implementation Pattern Guide

## 📋 Overview

Đây là pattern hoàn chỉnh để triển khai **Customer Module** - một module CRM điển hình với đầy đủ CRUD operations, filtering, và status management. Pattern này có thể được replicate cho tất cả các modules khác (CMS, LMS, etc.).

---

## 🗂️ File Structure Pattern

```
src/
├── types/
│   └── customer.ts              # TypeScript types & interfaces
├── lib/api/
│   └── customers.ts             # API client functions
├── hooks/
│   └── useCustomers.ts          # React Query hooks
├── components/customers/
│   ├── customer-table.tsx       # List/table component
│   ├── customer-form.tsx        # Create/edit form
│   └── customer-status-badge.tsx # Status badge component
└── app/admin/customers/
    ├── page.tsx                 # List page
    ├── new/page.tsx             # Create page
    └── [id]/page.tsx            # Detail/edit page
```

---

## 📝 Implementation Steps

### Step 1: Define TypeScript Types

**File:** `src/types/customer.ts`

**Pattern:**
```typescript
// 1. Define Enums
export enum CustomerStatus {
  LEAD = 'LEAD',
  ACTIVE = 'ACTIVE',
  // ... other statuses
}

// 2. Define Main Interface (matches backend entity)
export interface Customer {
  id: string
  code: string
  companyName: string
  // ... all fields from backend
  createdAt: string
  updatedAt: string
  version: number
}

// 3. Define Request DTOs
export interface CreateCustomerRequest {
  code: string
  companyName: string
  // ... required fields
}

export interface UpdateCustomerRequest {
  companyName?: string
  // ... optional fields (all fields optional)
}

// 4. Define Search Parameters
export interface CustomerSearchParams {
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
  keyword?: string
  status?: CustomerStatus
  // ... filter fields
}
```

**Key Points:**
- ✅ Match backend entity exactly
- ✅ Use enums for fixed values
- ✅ Create separate interfaces for Create/Update
- ✅ All update fields should be optional

---

### Step 2: Create API Client

**File:** `src/lib/api/customers.ts`

**Pattern:**
```typescript
import { apiClient, PageResponse } from './client'
import type { Customer, CreateCustomerRequest, UpdateCustomerRequest } from '@/types/customer'

export const customerApi = {
  // Basic CRUD
  getAll: async (params?: CustomerSearchParams): Promise<PageResponse<Customer>> => {
    const response = await apiClient.get<PageResponse<Customer>>('/customers', params)
    return response.data!
  },

  getById: async (id: string): Promise<Customer> => {
    const response = await apiClient.get<Customer>(`/customers/${id}`)
    return response.data!
  },

  create: async (data: CreateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.post<Customer>('/customers', data)
    return response.data!
  },

  update: async (id: string, data: UpdateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.put<Customer>(`/customers/${id}`, data)
    return response.data!
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/customers/${id}`)
  },

  // Search & Filter
  search: async (keyword: string): Promise<Customer[]> => {
    const response = await apiClient.get<Customer[]>('/customers/search', { keyword })
    return response.data!
  },

  // Custom Actions (if any)
  convertToProspect: async (id: string, reason?: string): Promise<Customer> => {
    const endpoint = reason 
      ? `/customers/${id}/convert-to-prospect?reason=${encodeURIComponent(reason)}`
      : `/customers/${id}/convert-to-prospect`
    const response = await apiClient.post<Customer>(endpoint)
    return response.data!
  },
}
```

**Key Points:**
- ✅ Always return typed responses
- ✅ Use `PageResponse<T>` for paginated endpoints
- ✅ Add all backend endpoints (including custom actions)
- ✅ Handle query params properly

---

### Step 3: Create React Query Hooks

**File:** `src/hooks/useCustomers.ts`

**Pattern:**
```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { customerApi } from '@/lib/api/customers'
import { toast } from 'sonner'

// Query Hooks (GET operations)
export function useCustomers(params?: CustomerSearchParams) {
  return useQuery({
    queryKey: ['customers', params],
    queryFn: () => customerApi.getAll(params),
  })
}

export function useCustomer(id: string) {
  return useQuery({
    queryKey: ['customers', id],
    queryFn: () => customerApi.getById(id),
    enabled: !!id,
  })
}

// Mutation Hooks (CREATE, UPDATE, DELETE)
export function useCreateCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: CreateCustomerRequest) => customerApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      toast.success('Customer created successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to create customer', {
        description: error.message,
      })
    },
  })
}

export function useUpdateCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCustomerRequest }) =>
      customerApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customers', variables.id] })
      toast.success('Customer updated successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to update customer', {
        description: error.message,
      })
    },
  })
}

export function useDeleteCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => customerApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      toast.success('Customer deleted successfully')
    },
    onError: (error: any) => {
      toast.error('Failed to delete customer', {
        description: error.message,
      })
    },
  })
}
```

**Key Points:**
- ✅ Use `useQuery` for GET operations
- ✅ Use `useMutation` for CREATE/UPDATE/DELETE
- ✅ Always invalidate related queries after mutations
- ✅ Show toast notifications for success/error
- ✅ Use descriptive hook names (useCustomers, useCustomer, etc.)

---

### Step 4: Create Table Component

**File:** `src/components/customers/customer-table.tsx`

**Features:**
- ✅ Paginated table display
- ✅ Row click to view details
- ✅ Actions dropdown (view, edit, delete)
- ✅ Delete confirmation dialog
- ✅ Loading state
- ✅ Empty state
- ✅ Neobrutalism styling

**Key Components Used:**
- `Table`, `TableHeader`, `TableBody`, `TableRow`, `TableCell`
- `DropdownMenu` for actions
- `AlertDialog` for delete confirmation
- `Button` for pagination
- `Loader2` for loading state

---

### Step 5: Create Form Component

**File:** `src/components/customers/customer-form.tsx`

**Features:**
- ✅ React Hook Form for validation
- ✅ Grouped fields in Cards
- ✅ Required field indicators (*)
- ✅ Select dropdowns for enums
- ✅ Checkbox for booleans
- ✅ Loading state during submission
- ✅ Cancel & Submit buttons

**Key Components Used:**
- `useForm` from react-hook-form
- `Card`, `CardHeader`, `CardTitle`, `CardContent`
- `Input`, `Select`, `Textarea`, `Checkbox`
- `Label` for form labels
- `Button` for actions

---

### Step 6: Create Pages

#### List Page (`app/admin/customers/page.tsx`)

**Features:**
- ✅ Stats cards (total, active, etc.)
- ✅ Search bar
- ✅ Filter dropdowns (status, type, etc.)
- ✅ Table with pagination
- ✅ Add button to create new

#### Create Page (`app/admin/customers/new/page.tsx`)

**Features:**
- ✅ Page header with back button
- ✅ Form component
- ✅ Submit handling
- ✅ Redirect after success

#### Detail Page (`app/admin/customers/[id]/page.tsx`)

**Features:**
- ✅ Header with status badge, edit button, actions dropdown
- ✅ Quick info cards (type, revenue, employees, rating)
- ✅ Tabs (overview, contacts, activities)
- ✅ Display all customer information
- ✅ Related data (contacts list)
- ✅ Status change actions
- ✅ Delete with confirmation
- ✅ Inline edit mode

---

## 🎨 Styling Pattern (Neobrutalism)

### Card Pattern
```typescript
<Card className="border-4 border-black">
  <CardHeader className="bg-yellow-300 border-b-4 border-black">
    <CardTitle className="text-2xl font-bold">Title</CardTitle>
  </CardHeader>
  <CardContent className="pt-6">
    {/* Content */}
  </CardContent>
</Card>
```

### Button Pattern
```typescript
<Button className="border-2 border-black font-bold">
  <Icon className="h-4 w-4 mr-2" />
  Button Text
</Button>
```

### Input Pattern
```typescript
<Input className="border-2 border-black" />
<Select>
  <SelectTrigger className="border-2 border-black">
    <SelectValue />
  </SelectTrigger>
  <SelectContent className="border-4 border-black">
    {/* Options */}
  </SelectContent>
</Select>
```

### Badge Pattern
```typescript
<Badge className="border-2 border-black bg-green-100 text-green-800">
  Status
</Badge>
```

---

## 📦 Replication Checklist for New Modules

### For Contact Module (Already have backend)
- [ ] Create `types/contact.ts`
- [ ] Create `lib/api/contacts.ts`
- [ ] Create `hooks/useContacts.ts`
- [ ] Create `components/contacts/contact-table.tsx`
- [ ] Create `components/contacts/contact-form.tsx`
- [ ] Create `app/admin/contacts/page.tsx`
- [ ] Create `app/admin/contacts/new/page.tsx`
- [ ] Create `app/admin/contacts/[id]/page.tsx`

### For Content Module (CMS)
- [ ] Create `types/content.ts`
- [ ] Create `lib/api/content.ts`
- [ ] Create `hooks/useContent.ts`
- [ ] Create `components/content/content-editor.tsx` (Rich text editor)
- [ ] Create `components/content/content-table.tsx`
- [ ] Create `app/admin/content/page.tsx`
- [ ] Create `app/admin/content/new/page.tsx`
- [ ] Create `app/admin/content/[id]/page.tsx`
- [ ] Create `app/blog/page.tsx` (Public listing)
- [ ] Create `app/blog/[slug]/page.tsx` (Public detail)

### For Course Module (LMS)
- [ ] Create `types/course.ts`
- [ ] Create `lib/api/courses.ts`
- [ ] Create `hooks/useCourses.ts`
- [ ] Create `components/courses/course-card.tsx`
- [ ] Create `components/courses/course-form.tsx`
- [ ] Create `components/courses/course-player.tsx`
- [ ] Create `app/admin/courses/page.tsx`
- [ ] Create `app/admin/courses/new/page.tsx`
- [ ] Create `app/courses/page.tsx` (Public catalog)
- [ ] Create `app/courses/[slug]/page.tsx` (Public detail)

---

## 🚀 Testing the Implementation

### 1. Start Backend
```bash
mvn spring-boot:run
```

### 2. Start Frontend
```bash
npm run dev
```

### 3. Navigate to
```
http://localhost:3000/admin/customers
```

### 4. Test Features
- ✅ View customer list
- ✅ Search customers
- ✅ Filter by status/type
- ✅ Create new customer
- ✅ Edit customer
- ✅ Delete customer
- ✅ View customer details
- ✅ Change customer status

---

## 📚 Key Takeaways

1. **Consistent Structure**: Follow the same file structure for all modules
2. **Type Safety**: Always use TypeScript types from backend entities
3. **API Client**: Centralize all API calls in one file per module
4. **React Query**: Use hooks for data fetching and mutations
5. **Neobrutalism**: Use bold borders, bright colors, and strong shadows
6. **User Feedback**: Always show toast notifications for actions
7. **Loading States**: Show loaders during async operations
8. **Error Handling**: Display error messages to users
9. **Validation**: Use react-hook-form for form validation
10. **Responsive**: Make sure components work on all screen sizes

---

## 🎯 Next Steps

Now that you have the **Customer Module** complete, you can:

1. **Test thoroughly** to ensure everything works
2. **Replicate pattern** for Contact module
3. **Continue with CMS** module (Content, Categories, Tags)
4. **Then LMS** module (Courses, Lessons, Quizzes)
5. **Add Notifications** with WebSocket
6. **Add File Attachments**
7. **Build Dashboard** with analytics

---

**Pattern Created By:** AI Assistant  
**Date:** November 3, 2025  
**Status:** ✅ Customer Module Complete - Ready to Replicate
