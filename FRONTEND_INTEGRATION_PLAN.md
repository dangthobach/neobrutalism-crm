# Frontend Integration Plan - Neobrutalism CRM
## Tích hợp API còn thiếu vào Frontend đã có

**Ngày tạo:** November 3, 2025  
**Phiên bản:** 1.0

---

## 📊 Phân tích hiện trạng

### ✅ Đã triển khai (Hoàn thành)

#### 1. **Foundation & Core**
- ✅ Next.js 14 với App Router
- ✅ TailwindCSS + Neobrutalism styling
- ✅ shadcn/ui components (40+ components)
- ✅ React Query (TanStack Query) setup
- ✅ Auth Context & Protected Routes
- ✅ API Client với token management
- ✅ Toast notifications (Sonner)
- ✅ Theme Provider (light/dark)

#### 2. **Authentication Module**
- ✅ Login page (`/login`)
- ✅ Auth Context với JWT
- ✅ Protected Route wrapper
- ✅ Token refresh mechanism
- ✅ Auth API client

#### 3. **Admin Module - User & Permission Management**
**Đã có pages:**
- ✅ `/admin/users` - User management
- ✅ `/admin/roles` - Role management
- ✅ `/admin/groups` - Group management
- ✅ `/admin/menus` - Menu management
- ✅ `/admin/menu-tabs` - Menu tabs
- ✅ `/admin/menu-screens` - Menu screens
- ✅ `/admin/api-endpoints` - API endpoints
- ✅ `/admin/organizations` - Organizations
- ✅ `/admin/permissions` - Permission sync

**Đã có API integrations:**
- ✅ `lib/api/auth.ts` - Authentication APIs
- ✅ `lib/api/users.ts` - User management
- ✅ `lib/api/roles.ts` - Role management
- ✅ `lib/api/groups.ts` - Group management
- ✅ `lib/api/menus.ts` - Menu management
- ✅ `lib/api/menu-tabs.ts` - Menu tabs
- ✅ `lib/api/menu-screens.ts` - Menu screens
- ✅ `lib/api/api-endpoints.ts` - API endpoints
- ✅ `lib/api/organizations.ts` - Organizations
- ✅ `lib/api/user-roles.ts` - User-role assignments
- ✅ `lib/api/user-groups.ts` - User-group assignments
- ✅ `lib/api/group-roles.ts` - Group-role assignments
- ✅ `lib/api/role-menus.ts` - Role-menu permissions

**Đã có React Query hooks:**
- ✅ `hooks/useUsers.ts`
- ✅ `hooks/useRoles.ts`
- ✅ `hooks/useGroups.ts`
- ✅ `hooks/useMenus.ts`
- ✅ `hooks/useMenuTabs.ts`
- ✅ `hooks/useMenuScreens.ts`
- ✅ `hooks/useApiEndpoints.ts`
- ✅ `hooks/useUserRoles.ts`
- ✅ `hooks/useUserGroups.ts`
- ✅ `hooks/useGroupRoles.ts`
- ✅ `hooks/useRoleMenus.ts`
- ✅ `hooks/usePermission.ts`

#### 4. **UI Components Library**
**Đã có 40+ components:**
- Forms: Input, Button, Checkbox, Select, Textarea, etc.
- Data Display: Table, Card, Badge, Avatar, etc.
- Feedback: Toast, Alert, Dialog, Sheet, etc.
- Navigation: Navbar, Sidebar, Breadcrumb, Tabs, etc.

---

## ❌ Chưa triển khai (Cần thêm)

### 1. **CRM Module - Customer & Contact Management**

#### 📦 API Clients cần tạo:
```typescript
// lib/api/customers.ts
// lib/api/contacts.ts
// lib/api/branches.ts (đã có organizations, cần thêm branches)
```

#### 🎣 React Query Hooks cần tạo:
```typescript
// hooks/useCustomers.ts
// hooks/useContacts.ts
// hooks/useBranches.ts
```

#### 📄 Pages cần tạo:
```
/admin/customers/             # Customer listing
/admin/customers/[id]         # Customer detail
/admin/customers/new          # Create customer
/admin/contacts/              # Contact listing
/admin/contacts/[id]          # Contact detail
/admin/branches/              # Branch management
```

#### 🔧 Components cần tạo:
- `CustomerTable` - Table with filters (status, type, owner, VIP)
- `CustomerForm` - Create/edit customer form
- `CustomerDetail` - Detail view with tabs (info, contacts, activities)
- `CustomerStatusBadge` - Status indicator
- `ContactTable` - Contact listing
- `ContactForm` - Create/edit contact
- `CustomerStats` - Dashboard cards (by status, type)
- `CustomerSearchBar` - Search with autocomplete

---

### 2. **CMS Module - Content Management**

#### 📦 API Clients cần tạo:
```typescript
// lib/api/content.ts
// lib/api/content-categories.ts
// lib/api/content-tags.ts
// lib/api/content-series.ts
```

#### 🎣 React Query Hooks cần tạo:
```typescript
// hooks/useContent.ts
// hooks/useContentCategories.ts
// hooks/useContentTags.ts
// hooks/useContentSeries.ts
```

#### 📄 Pages cần tạo:
```
/admin/content/               # Content listing (admin)
/admin/content/[id]           # Edit content
/admin/content/new            # Create content
/admin/content/categories     # Category management
/admin/content/tags           # Tag management
/admin/content/series         # Series management

/blog/                        # Public content listing
/blog/[slug]                  # Public content detail
/blog/category/[slug]         # Content by category
/blog/tag/[slug]              # Content by tag
/blog/series/[slug]           # Content series
```

#### 🔧 Components cần tạo:
- `ContentEditor` - Rich text editor (TipTap/Lexical)
- `ContentTable` - Admin content listing with filters
- `ContentForm` - Create/edit content form
- `ContentPreview` - Preview content before publish
- `ContentStatusBadge` - Status workflow (draft/review/published)
- `ContentCard` - Public content card
- `CategoryTree` - Hierarchical category tree
- `TagCloud` - Tag cloud component
- `ContentSearch` - Search with filters
- `TrendingContent` - Trending content widget
- `RelatedContent` - Related content section

---

### 3. **LMS Module - Learning Management System**

#### 📦 API Clients cần tạo:
```typescript
// lib/api/courses.ts
// lib/api/lessons.ts
// lib/api/quizzes.ts
// lib/api/enrollments.ts
// lib/api/certificates.ts
```

#### 🎣 React Query Hooks cần tạo:
```typescript
// hooks/useCourses.ts
// hooks/useLessons.ts
// hooks/useQuizzes.ts
// hooks/useEnrollments.ts
// hooks/useCertificates.ts
```

#### 📄 Pages cần tạo:
```
/admin/courses/               # Course management (admin)
/admin/courses/[id]           # Edit course
/admin/courses/new            # Create course
/admin/courses/[id]/modules   # Course modules
/admin/courses/[id]/lessons   # Course lessons
/admin/quizzes/               # Quiz management

/courses/                     # Public course catalog
/courses/[slug]               # Course detail & enroll
/my-courses/                  # Student dashboard
/my-courses/[id]              # Course player
/my-courses/[id]/lessons/[lessonId]  # Lesson viewer
/my-courses/[id]/quizzes/[quizId]    # Quiz taking
/certificates/[id]            # Certificate view/download
```

#### 🔧 Components cần tạo:
- `CourseCard` - Course card for catalog
- `CourseDetail` - Course detail with curriculum
- `CourseForm` - Create/edit course (multi-step wizard)
- `ModuleList` - Course modules list
- `LessonEditor` - Create/edit lessons
- `CoursePlayer` - Video/content player
- `ProgressBar` - Course progress indicator
- `QuizInterface` - Quiz taking UI
- `QuizResults` - Results display
- `CertificateDisplay` - Certificate viewer
- `EnrollmentButton` - Enroll action button
- `CourseFilters` - Filter by level, tier, etc.

---

### 4. **Notification Module**

#### 📦 API Clients cần tạo:
```typescript
// lib/api/notifications.ts
// lib/websocket/client.ts (WebSocket)
```

#### 🎣 React Query Hooks cần tạo:
```typescript
// hooks/useNotifications.ts
// hooks/useWebSocket.ts
```

#### 📄 Pages cần tạo:
```
/notifications/               # Notification center
```

#### 🔧 Components cần tạo:
- `NotificationBell` - Bell icon with unread count
- `NotificationDropdown` - Dropdown panel
- `NotificationList` - List of notifications
- `NotificationItem` - Single notification item
- `WebSocketProvider` - WebSocket context provider

---

### 5. **Attachment & File Management**

#### 📦 API Clients cần tạo:
```typescript
// lib/api/attachments.ts
```

#### 🎣 React Query Hooks cần tạo:
```typescript
// hooks/useAttachments.ts
```

#### 🔧 Components cần tạo:
- `FileUpload` - Drag & drop file upload
- `FilePreview` - File preview component
- `AttachmentList` - List of attachments
- `ImageGallery` - Image gallery viewer

---

### 6. **Dashboard & Analytics**

#### 📄 Pages cần tạo:
```
/admin/dashboard              # Main admin dashboard
/admin/analytics              # Analytics & reports
```

#### 🔧 Components cần tạo:
- `StatCard` - Metric card (total users, customers, etc.)
- `ChartCard` - Chart wrapper
- `SalesFunnel` - Sales funnel chart
- `CustomerAcquisition` - Customer acquisition chart
- `CourseEnrollments` - Enrollment trends
- `ContentViews` - Content view analytics

---

## 🎯 Ưu tiên triển khai (Priority Order)

### 🔴 Priority 1: CRM Module (Week 1-2)
**Business Value: Very High**  
**Complexity: Medium**

**Tasks:**
1. Create API clients (customers, contacts, branches)
2. Create React Query hooks
3. Build Customer pages (list, detail, create/edit)
4. Build Contact pages (list, create/edit)
5. Build Branch management page
6. Create reusable components (CustomerTable, ContactTable, forms)
7. Implement customer search & filters
8. Add customer analytics/stats

**Estimated Time:** 10-12 days

---

### 🟠 Priority 2: CMS Module (Week 3-4)
**Business Value: High**  
**Complexity: High (Rich text editor)**

**Tasks:**
1. Create API clients (content, categories, tags, series)
2. Create React Query hooks
3. Integrate rich text editor (TipTap recommended)
4. Build admin content management pages
5. Build public content pages (blog)
6. Create content components (editor, preview, cards)
7. Implement content workflow (draft → review → published)
8. Add content search & filters
9. Build category & tag management
10. Add trending/related content features

**Estimated Time:** 12-14 days

---

### 🟡 Priority 3: LMS Module (Week 5-7)
**Business Value: High**  
**Complexity: Very High (Video player, quiz engine)**

**Tasks:**
1. Create API clients (courses, lessons, quizzes, enrollments)
2. Create React Query hooks
3. Build admin course management pages
4. Build course creation wizard (multi-step form)
5. Build public course catalog
6. Build course player with lesson navigation
7. Create quiz taking interface
8. Implement progress tracking
9. Build certificate generation/display
10. Add enrollment management
11. Build student dashboard

**Estimated Time:** 18-21 days

---

### 🟢 Priority 4: Notification & Real-time (Week 8)
**Business Value: Medium**  
**Complexity: Medium (WebSocket integration)**

**Tasks:**
1. Create notification API client
2. Setup WebSocket client
3. Create notification context
4. Build notification bell component
5. Build notification dropdown
6. Build notification center page
7. Integrate real-time updates
8. Add browser notifications (optional)

**Estimated Time:** 5-6 days

---

### 🔵 Priority 5: Attachments & Files (Week 9)
**Business Value: Medium**  
**Complexity: Low-Medium**

**Tasks:**
1. Create attachment API client
2. Build file upload component (drag & drop)
3. Build file preview component
4. Build attachment list component
5. Integrate with customer/content/course modules

**Estimated Time:** 3-4 days

---

### ⚪ Priority 6: Dashboard & Analytics (Week 10)
**Business Value: Medium**  
**Complexity: Medium (Charts & data visualization)**

**Tasks:**
1. Design dashboard layout
2. Create stat cards
3. Integrate chart library (Recharts)
4. Build key metrics charts
5. Add filters & date ranges
6. Build reports page

**Estimated Time:** 5-6 days

---

## 📁 File Structure (Additions)

```
src/
├── app/
│   ├── (dashboard)/          # Main app layout (already exists)
│   │   ├── admin/
│   │   │   ├── customers/    # ✅ NEW
│   │   │   │   ├── page.tsx
│   │   │   │   ├── [id]/
│   │   │   │   │   └── page.tsx
│   │   │   │   └── new/
│   │   │   │       └── page.tsx
│   │   │   ├── contacts/     # ✅ NEW
│   │   │   ├── branches/     # ✅ NEW
│   │   │   ├── content/      # ✅ NEW
│   │   │   │   ├── page.tsx
│   │   │   │   ├── [id]/
│   │   │   │   ├── new/
│   │   │   │   ├── categories/
│   │   │   │   ├── tags/
│   │   │   │   └── series/
│   │   │   ├── courses/      # ✅ NEW
│   │   │   │   ├── page.tsx
│   │   │   │   ├── [id]/
│   │   │   │   └── new/
│   │   │   ├── quizzes/      # ✅ NEW
│   │   │   ├── dashboard/    # ✅ NEW
│   │   │   └── analytics/    # ✅ NEW
│   │   ├── blog/             # ✅ NEW (Public)
│   │   │   ├── page.tsx
│   │   │   ├── [slug]/
│   │   │   ├── category/
│   │   │   ├── tag/
│   │   │   └── series/
│   │   ├── courses/          # ✅ NEW (Public)
│   │   │   ├── page.tsx
│   │   │   └── [slug]/
│   │   ├── my-courses/       # ✅ NEW (Student)
│   │   │   ├── page.tsx
│   │   │   └── [id]/
│   │   └── notifications/    # ✅ NEW
│   │       └── page.tsx
├── components/
│   ├── customers/            # ✅ NEW
│   │   ├── customer-table.tsx
│   │   ├── customer-form.tsx
│   │   ├── customer-detail.tsx
│   │   ├── customer-status-badge.tsx
│   │   └── customer-search.tsx
│   ├── contacts/             # ✅ NEW
│   │   ├── contact-table.tsx
│   │   ├── contact-form.tsx
│   │   └── contact-card.tsx
│   ├── content/              # ✅ NEW
│   │   ├── content-editor.tsx
│   │   ├── content-table.tsx
│   │   ├── content-form.tsx
│   │   ├── content-card.tsx
│   │   ├── content-preview.tsx
│   │   ├── category-tree.tsx
│   │   └── tag-cloud.tsx
│   ├── courses/              # ✅ NEW
│   │   ├── course-card.tsx
│   │   ├── course-detail.tsx
│   │   ├── course-form.tsx
│   │   ├── course-player.tsx
│   │   ├── lesson-list.tsx
│   │   ├── quiz-interface.tsx
│   │   └── progress-bar.tsx
│   ├── notifications/        # ✅ NEW
│   │   ├── notification-bell.tsx
│   │   ├── notification-dropdown.tsx
│   │   └── notification-list.tsx
│   ├── attachments/          # ✅ NEW
│   │   ├── file-upload.tsx
│   │   ├── file-preview.tsx
│   │   └── attachment-list.tsx
│   └── dashboard/            # ✅ NEW
│       ├── stat-card.tsx
│       ├── chart-card.tsx
│       └── sales-funnel.tsx
├── lib/
│   ├── api/
│   │   ├── customers.ts      # ✅ NEW
│   │   ├── contacts.ts       # ✅ NEW
│   │   ├── branches.ts       # ✅ NEW
│   │   ├── content.ts        # ✅ NEW
│   │   ├── content-categories.ts  # ✅ NEW
│   │   ├── content-tags.ts   # ✅ NEW
│   │   ├── content-series.ts # ✅ NEW
│   │   ├── courses.ts        # ✅ NEW
│   │   ├── lessons.ts        # ✅ NEW
│   │   ├── quizzes.ts        # ✅ NEW
│   │   ├── enrollments.ts    # ✅ NEW
│   │   ├── certificates.ts   # ✅ NEW
│   │   ├── notifications.ts  # ✅ NEW
│   │   └── attachments.ts    # ✅ NEW
│   └── websocket/            # ✅ NEW
│       └── client.ts
├── hooks/
│   ├── useCustomers.ts       # ✅ NEW
│   ├── useContacts.ts        # ✅ NEW
│   ├── useBranches.ts        # ✅ NEW
│   ├── useContent.ts         # ✅ NEW
│   ├── useContentCategories.ts  # ✅ NEW
│   ├── useContentTags.ts     # ✅ NEW
│   ├── useContentSeries.ts   # ✅ NEW
│   ├── useCourses.ts         # ✅ NEW
│   ├── useLessons.ts         # ✅ NEW
│   ├── useQuizzes.ts         # ✅ NEW
│   ├── useEnrollments.ts     # ✅ NEW
│   ├── useCertificates.ts    # ✅ NEW
│   ├── useNotifications.ts   # ✅ NEW
│   ├── useWebSocket.ts       # ✅ NEW
│   └── useAttachments.ts     # ✅ NEW
└── types/
    ├── customer.ts           # ✅ NEW
    ├── contact.ts            # ✅ NEW
    ├── content.ts            # ✅ NEW
    ├── course.ts             # ✅ NEW
    ├── notification.ts       # ✅ NEW
    └── attachment.ts         # ✅ NEW
```

---

## 🛠️ Implementation Guidelines

### 1. API Client Pattern (Đã có sẵn - Follow pattern này)

```typescript
// Example: lib/api/customers.ts
import { apiClient, ApiResponse, PageResponse } from './client'

export interface Customer {
  id: string
  code: string
  companyName: string
  // ... other fields
}

export interface CreateCustomerRequest {
  code: string
  companyName: string
  // ... other fields
}

export const customerApi = {
  getAll: async (params: {
    page?: number
    size?: number
    sortBy?: string
    sortDirection?: string
  }): Promise<PageResponse<Customer>> => {
    const response = await apiClient.get<PageResponse<Customer>>(
      '/customers',
      params
    )
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

  update: async (id: string, data: Partial<Customer>): Promise<Customer> => {
    const response = await apiClient.put<Customer>(`/customers/${id}`, data)
    return response.data!
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/customers/${id}`)
  },

  // Status transitions
  convertToProspect: async (id: string, reason?: string): Promise<Customer> => {
    const response = await apiClient.post<Customer>(
      `/customers/${id}/convert-to-prospect`,
      null,
      { params: { reason } }
    )
    return response.data!
  },

  // Search
  search: async (keyword: string): Promise<Customer[]> => {
    const response = await apiClient.get<Customer[]>('/customers/search', {
      keyword,
    })
    return response.data!
  },
}
```

### 2. React Query Hook Pattern (Đã có sẵn - Follow pattern này)

```typescript
// Example: hooks/useCustomers.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { customerApi } from '@/lib/api/customers'
import { toast } from 'sonner'

export function useCustomers(page = 0, size = 20) {
  return useQuery({
    queryKey: ['customers', page, size],
    queryFn: () => customerApi.getAll({ page, size }),
  })
}

export function useCustomer(id: string) {
  return useQuery({
    queryKey: ['customers', id],
    queryFn: () => customerApi.getById(id),
    enabled: !!id,
  })
}

export function useCreateCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: customerApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      toast.success('Customer created successfully')
    },
    onError: (error) => {
      toast.error('Failed to create customer', {
        description: error.message,
      })
    },
  })
}

export function useUpdateCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) =>
      customerApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customers', variables.id] })
      toast.success('Customer updated successfully')
    },
    onError: (error) => {
      toast.error('Failed to update customer', {
        description: error.message,
      })
    },
  })
}

export function useDeleteCustomer() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: customerApi.delete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      toast.success('Customer deleted successfully')
    },
    onError: (error) => {
      toast.error('Failed to delete customer', {
        description: error.message,
      })
    },
  })
}
```

### 3. Component Pattern (Sử dụng components có sẵn)

```typescript
// Example: components/customers/customer-table.tsx
'use client'

import { useState } from 'react'
import { useCustomers, useDeleteCustomer } from '@/hooks/useCustomers'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow 
} from '@/components/ui/table'
import { Loader2, Edit, Trash2 } from 'lucide-react'

export function CustomerTable() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useCustomers(page)
  const deleteMutation = useDeleteCustomer()

  if (isLoading) {
    return <div className="flex justify-center p-8">
      <Loader2 className="h-8 w-8 animate-spin" />
    </div>
  }

  return (
    <div className="space-y-4">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Company Name</TableHead>
            <TableHead>Code</TableHead>
            <TableHead>Type</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Owner</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data?.content.map((customer) => (
            <TableRow key={customer.id}>
              <TableCell className="font-medium">
                {customer.companyName}
                {customer.isVip && (
                  <Badge variant="default" className="ml-2">VIP</Badge>
                )}
              </TableCell>
              <TableCell>{customer.code}</TableCell>
              <TableCell>{customer.customerType}</TableCell>
              <TableCell>
                <Badge variant={getStatusVariant(customer.status)}>
                  {customer.status}
                </Badge>
              </TableCell>
              <TableCell>{customer.ownerName}</TableCell>
              <TableCell className="text-right space-x-2">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => router.push(`/admin/customers/${customer.id}`)}
                >
                  <Edit className="h-4 w-4" />
                </Button>
                <Button
                  size="sm"
                  variant="destructive"
                  onClick={() => deleteMutation.mutate(customer.id)}
                  disabled={deleteMutation.isPending}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>

      {/* Pagination */}
      <div className="flex justify-between items-center">
        <Button
          onClick={() => setPage(page - 1)}
          disabled={page === 0}
        >
          Previous
        </Button>
        <span>Page {page + 1} of {data?.totalPages}</span>
        <Button
          onClick={() => setPage(page + 1)}
          disabled={page >= (data?.totalPages || 0) - 1}
        >
          Next
        </Button>
      </div>
    </div>
  )
}

function getStatusVariant(status: string) {
  switch (status) {
    case 'ACTIVE': return 'success'
    case 'LEAD': return 'secondary'
    case 'PROSPECT': return 'default'
    case 'INACTIVE': return 'outline'
    default: return 'default'
  }
}
```

---

## 📝 TypeScript Types (Cần tạo)

### Customer Types
```typescript
// types/customer.ts
export enum CustomerType {
  B2B = 'B2B',
  B2C = 'B2C',
  PARTNER = 'PARTNER',
  RESELLER = 'RESELLER',
  VENDOR = 'VENDOR',
  PROSPECT = 'PROSPECT',
}

export enum CustomerStatus {
  LEAD = 'LEAD',
  PROSPECT = 'PROSPECT',
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  CHURNED = 'CHURNED',
  BLACKLISTED = 'BLACKLISTED',
}

export interface Customer {
  id: string
  code: string
  companyName: string
  legalName?: string
  customerType: CustomerType
  status: CustomerStatus
  industry?: string
  taxId?: string
  email?: string
  phone?: string
  website?: string
  billingAddress?: string
  shippingAddress?: string
  city?: string
  state?: string
  country?: string
  postalCode?: string
  ownerId?: string
  ownerName?: string
  branchId?: string
  branchName?: string
  organizationId: string
  annualRevenue?: number
  employeeCount?: number
  acquisitionDate?: string
  lastContactDate?: string
  nextFollowupDate?: string
  leadSource?: string
  creditLimit?: number
  paymentTermsDays?: number
  tags?: string[]
  notes?: string
  rating?: number
  isVip: boolean
  createdAt: string
  updatedAt: string
  createdBy: string
  updatedBy: string
}
```

### Content Types
```typescript
// types/content.ts
export enum ContentType {
  BLOG = 'BLOG',
  ARTICLE = 'ARTICLE',
  TUTORIAL = 'TUTORIAL',
  GUIDE = 'GUIDE',
  NEWS = 'NEWS',
  PAGE = 'PAGE',
}

export enum ContentStatus {
  DRAFT = 'DRAFT',
  REVIEW = 'REVIEW',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED',
  DELETED = 'DELETED',
}

export enum MemberTier {
  FREE = 'FREE',
  BASIC = 'BASIC',
  PREMIUM = 'PREMIUM',
  ENTERPRISE = 'ENTERPRISE',
}

export interface Content {
  id: string
  tenantId: string
  title: string
  slug: string
  summary?: string
  body: string
  featuredImageId?: string
  featuredImageUrl?: string
  contentType: ContentType
  status: ContentStatus
  publishedAt?: string
  viewCount: number
  tierRequired: MemberTier
  authorId: string
  authorName: string
  seriesId?: string
  seriesName?: string
  seriesOrder?: number
  seoTitle?: string
  seoDescription?: string
  seoKeywords?: string
  categories: ContentCategory[]
  tags: ContentTag[]
  createdAt: string
  updatedAt: string
}
```

### Course Types
```typescript
// types/course.ts
export enum CourseLevel {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  ADVANCED = 'ADVANCED',
  EXPERT = 'EXPERT',
}

export enum CourseStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED',
}

export interface Course {
  id: string
  title: string
  slug: string
  description: string
  thumbnailUrl?: string
  courseLevel: CourseLevel
  status: CourseStatus
  price?: number
  tierRequired: MemberTier
  duration?: number
  instructorId: string
  instructorName: string
  enrollmentCount: number
  completionRate: number
  ratingAverage?: number
  ratingCount: number
  publishedAt?: string
  createdAt: string
  updatedAt: string
}

export interface Enrollment {
  id: string
  userId: string
  courseId: string
  courseName: string
  enrolledAt: string
  completedAt?: string
  progress: number
  certificateId?: string
  pricePaid?: number
}
```

---

## 🎯 Success Metrics

### Technical Metrics
- [ ] All backend APIs integrated (300+ endpoints)
- [ ] 100% TypeScript coverage
- [ ] Response time < 100ms for data fetching
- [ ] Error rate < 1%
- [ ] Test coverage > 70%

### UX Metrics
- [ ] Page load < 2s
- [ ] Form submission < 1s
- [ ] Search results < 500ms
- [ ] Mobile responsive (all pages)
- [ ] Accessibility score > 90 (Lighthouse)

### Business Metrics
- [ ] All 8 modules fully functional
- [ ] End-to-end workflows working
- [ ] User can complete key tasks without errors
- [ ] Admin can manage all entities
- [ ] Public users can access content/courses

---

## 📅 Timeline Summary

| Week | Focus | Status |
|------|-------|--------|
| Week 1-2 | CRM Module | 🔴 Not Started |
| Week 3-4 | CMS Module | 🔴 Not Started |
| Week 5-7 | LMS Module | 🔴 Not Started |
| Week 8 | Notifications | 🔴 Not Started |
| Week 9 | Attachments | 🔴 Not Started |
| Week 10 | Dashboard | 🔴 Not Started |
| Week 11 | Testing & Bug Fixes | 🔴 Not Started |
| Week 12 | Polish & Documentation | 🔴 Not Started |

**Total Estimated Time:** 12 weeks (60 working days)

---

## 🚀 Getting Started

### Step 1: Setup Environment
```bash
cd neobrutalism-crm
npm install
```

### Step 2: Configure Backend URL
```env
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### Step 3: Start Development Server
```bash
npm run dev
```

### Step 4: Start Backend
```bash
cd ../backend
mvn spring-boot:run
```

---

## 📚 Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [TanStack Query](https://tanstack.com/query/latest)
- [shadcn/ui Components](https://ui.shadcn.com/)
- [Tailwind CSS](https://tailwindcss.com/)
- [Backend API Docs](http://localhost:8080/swagger-ui.html)

---

## ✅ Next Immediate Actions

1. ✅ **Review this plan** với team
2. 🔴 **Start Priority 1**: Tạo Customer API client (`lib/api/customers.ts`)
3. 🔴 **Create types**: Định nghĩa TypeScript types (`types/customer.ts`)
4. 🔴 **Create hooks**: React Query hooks (`hooks/useCustomers.ts`)
5. 🔴 **Build pages**: Customer listing page (`app/admin/customers/page.tsx`)

---

**Prepared By:** AI Assistant  
**Last Updated:** November 3, 2025  
**Version:** 1.0
