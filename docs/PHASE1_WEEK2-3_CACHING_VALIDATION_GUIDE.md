# ✅ PHASE 1 WEEK 2-3: Caching & Form Validation - IMPLEMENTATION GUIDE

**Date:** 2025-11-04  
**Status:** 🔄 IN PROGRESS  
**Phase:** Week 2 (Caching) + Week 3 (Validation)

---

## 📋 Overview

This document provides comprehensive implementation guide for:
- **Week 2:** Redis caching with @Cacheable annotations + React Query migration
- **Week 3:** Zod schemas matching Jakarta validation + Reusable form components

---

## WEEK 2: CACHING STRATEGY

### ✅ Part 1: Redis Configuration (DONE)

#### Redis Cache Regions Added
Updated `RedisCacheConfig.java` with entity-specific caches:

```java
// Short-lived caches (5 minutes) - Frequently changing data
cacheConfigurations.put("branches", defaultConfig.entryTtl(Duration.ofMinutes(5)));
cacheConfigurations.put("customers", defaultConfig.entryTtl(Duration.ofMinutes(5)));
cacheConfigurations.put("contacts", defaultConfig.entryTtl(Duration.ofMinutes(5)));

// Medium-lived caches (10 minutes) - User data
cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(10)));
```

**Benefits:**
- Separate TTL per entity type
- Automatic cache invalidation after TTL
- JSON serialization for complex objects
- Graceful error handling (log but don't fail)

---

### 🔄 Part 2: @Cacheable Annotations (IN PROGRESS)

#### Implementation Pattern

**1. Read Operations - Add @Cacheable**

```java
@Cacheable(value = "branches", key = "#id")
public Branch findById(UUID id) {
    return branchRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + id));
}

@Cacheable(value = "branches", key = "'org:' + #organizationId")
public List<Branch> findByOrganizationId(UUID organizationId) {
    return branchRepository.findByOrganizationId(organizationId);
}
```

**2. Write Operations - Add @CacheEvict**

```java
@CacheEvict(value = "branches", allEntries = true)
public Branch create(Branch branch) {
    // ... creation logic
    return branchRepository.save(branch);
}

@CacheEvict(value = "branches", allEntries = true)
public Branch update(UUID id, Branch updatedBranch) {
    // ... update logic
    return branchRepository.save(existingBranch);
}

@CacheEvict(value = "branches", key = "#id")
public void delete(UUID id) {
    branchRepository.deleteById(id);
}
```

**3. Conditional Caching**

```java
@Cacheable(value = "branches", 
           key = "'search:' + #status + ':' + #organizationId",
           condition = "#status != null")
public List<Branch> findByStatus(BranchStatus status, UUID organizationId) {
    return branchRepository.findByStatusAndOrganizationId(status, organizationId);
}
```

#### Files to Update

**BranchService.java**
- ✅ Add `@Cacheable` to: `findById`, `findByOrganizationId`, `findByCode`, `getRootBranches`
- ✅ Add `@CacheEvict` to: `create`, `update`, `delete`, `updateParent`

**CustomerService.java**
- ✅ Add `@Cacheable` to: `findById`, `findByOrganizationId`, `findByStatus`, `findByType`, `findVipCustomers`
- ✅ Add `@CacheEvict` to: `create`, `update`, `delete`, `updateStatus`

**UserService.java**
- ✅ Add `@Cacheable` to: `findById`, `findByUsername`, `findByEmail`
- ✅ Add `@CacheEvict` to: `create`, `update`, `delete`

#### Cache Key Strategy

```java
// Single entity
key = "#id"

// List by organization
key = "'org:' + #organizationId"

// Filtered list
key = "'filter:' + #status + ':' + #type"

// Search with multiple params
key = "#root.methodName + ':' + #p0 + ':' + #p1"
```

---

### 🔄 Part 3: React Query Migration (TODO)

#### Setup React Query Provider

**1. Install Dependencies**
```bash
npm install @tanstack/react-query @tanstack/react-query-devtools
```

**2. Create Query Client**
```typescript
// src/lib/react-query/client.ts
import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
      refetchOnWindowFocus: false,
      retry: 1,
    },
    mutations: {
      retry: false,
    },
  },
})
```

**3. Add Provider to Root Layout**
```typescript
// src/app/layout.tsx
'use client'

import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { queryClient } from '@/lib/react-query/client'

export default function RootLayout({ children }) {
  return (
    <html>
      <body>
        <QueryClientProvider client={queryClient}>
          {children}
          <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
      </body>
    </html>
  )
}
```

#### Migration Pattern: Custom Hooks → React Query

**Before (Current Pattern):**
```typescript
// hooks/useBranches.ts
export function useBranches(organizationId: string) {
  const [data, setData] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  
  useEffect(() => {
    fetchBranches(organizationId).then(setData).finally(() => setIsLoading(false))
  }, [organizationId])
  
  return { data, isLoading }
}
```

**After (React Query):**
```typescript
// hooks/useBranches.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

export function useBranches(organizationId: string) {
  return useQuery({
    queryKey: ['branches', organizationId],
    queryFn: () => fetchBranches(organizationId),
    staleTime: 5 * 60 * 1000, // 5 minutes
  })
}

export function useCreateBranch() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: (data: CreateBranchRequest) => createBranch(data),
    onSuccess: () => {
      // Invalidate and refetch
      queryClient.invalidateQueries({ queryKey: ['branches'] })
      toast.success('Branch created successfully')
    },
    onError: (error: ApiError) => {
      ErrorHandler.toast(error)
    },
  })
}
```

#### Query Keys Strategy

```typescript
// lib/react-query/keys.ts
export const queryKeys = {
  // Branches
  branches: {
    all: ['branches'] as const,
    lists: () => [...queryKeys.branches.all, 'list'] as const,
    list: (filters: BranchFilters) => [...queryKeys.branches.lists(), filters] as const,
    details: () => [...queryKeys.branches.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.branches.details(), id] as const,
  },
  
  // Customers
  customers: {
    all: ['customers'] as const,
    lists: () => [...queryKeys.customers.all, 'list'] as const,
    list: (filters: CustomerFilters) => [...queryKeys.customers.lists(), filters] as const,
    details: () => [...queryKeys.customers.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.customers.details(), id] as const,
  },
}
```

#### Optimistic Updates

```typescript
export function useUpdateCustomer() {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCustomerRequest }) =>
      updateCustomer(id, data),
      
    // Optimistic update
    onMutate: async ({ id, data }) => {
      // Cancel outgoing refetches
      await queryClient.cancelQueries({ queryKey: queryKeys.customers.detail(id) })
      
      // Snapshot previous value
      const previousCustomer = queryClient.getQueryData(queryKeys.customers.detail(id))
      
      // Optimistically update
      queryClient.setQueryData(queryKeys.customers.detail(id), (old: Customer) => ({
        ...old,
        ...data,
      }))
      
      return { previousCustomer }
    },
    
    // Rollback on error
    onError: (err, variables, context) => {
      if (context?.previousCustomer) {
        queryClient.setQueryData(
          queryKeys.customers.detail(variables.id),
          context.previousCustomer
        )
      }
      ErrorHandler.toast(err as ApiError)
    },
    
    // Always refetch after error or success
    onSettled: (data, error, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.customers.detail(variables.id) })
    },
  })
}
```

#### Prefetching Strategy

```typescript
// Prefetch on hover
function CustomerListItem({ customer }: { customer: Customer }) {
  const queryClient = useQueryClient()
  
  const prefetchCustomer = () => {
    queryClient.prefetchQuery({
      queryKey: queryKeys.customers.detail(customer.id),
      queryFn: () => fetchCustomer(customer.id),
      staleTime: 5 * 60 * 1000,
    })
  }
  
  return (
    <div onMouseEnter={prefetchCustomer}>
      {customer.companyName}
    </div>
  )
}
```

---

## WEEK 3: FORM VALIDATION

### 🔄 Part 1: Zod Schemas (TODO)

#### Match Jakarta Validation

**Backend (Jakarta):**
```java
@Entity
public class Customer {
    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phone;
    
    @Min(value = 0, message = "Annual revenue must be positive")
    private BigDecimal annualRevenue;
}
```

**Frontend (Zod):**
```typescript
// lib/validation/customer.schema.ts
import { z } from 'zod'

export const customerSchema = z.object({
  companyName: z
    .string()
    .min(1, 'Company name is required')
    .max(255, 'Company name must not exceed 255 characters'),
    
  email: z
    .string()
    .email('Invalid email format')
    .optional()
    .or(z.literal('')),
    
  phone: z
    .string()
    .regex(/^\+?[1-9]\d{1,14}$/, 'Invalid phone number')
    .optional()
    .or(z.literal('')),
    
  annualRevenue: z
    .number()
    .min(0, 'Annual revenue must be positive')
    .optional(),
    
  customerType: z.enum(['B2B', 'B2C', 'B2B2C']),
  
  status: z.enum(['LEAD', 'PROSPECT', 'CUSTOMER', 'INACTIVE']),
})

export type CustomerFormData = z.infer<typeof customerSchema>
```

#### Validation Schema Files

**1. Create schemas for each entity**
```
src/lib/validation/
  ├── customer.schema.ts
  ├── contact.schema.ts
  ├── branch.schema.ts
  ├── user.schema.ts
  └── index.ts
```

**2. Common validation helpers**
```typescript
// lib/validation/helpers.ts
import { z } from 'zod'

export const phoneRegex = /^\+?[1-9]\d{1,14}$/
export const taxIdRegex = /^\d{2}-\d{7}$/

export const optionalString = z.string().optional().or(z.literal(''))
export const optionalNumber = z.number().optional().or(z.nan())
export const positiveNumber = z.number().min(0, 'Must be positive')
export const percentage = z.number().min(0).max(100, 'Must be between 0 and 100')
```

---

### 🔄 Part 2: Reusable Form Components (TODO)

#### Component Architecture

```
src/components/forms/
  ├── form-field.tsx       # Wrapper with label + error
  ├── form-input.tsx       # Text input with validation
  ├── form-select.tsx      # Select with validation
  ├── form-textarea.tsx    # Textarea with validation
  ├── form-checkbox.tsx    # Checkbox with validation
  ├── form-date-picker.tsx # Date picker with validation
  └── index.ts
```

#### FormField Component

```typescript
// components/forms/form-field.tsx
import { InlineError } from '@/components/errors/error-display'
import { Label } from '@/components/ui/label'

interface FormFieldProps {
  label: string
  name: string
  required?: boolean
  error?: string
  hint?: string
  children: React.ReactNode
}

export function FormField({
  label,
  name,
  required,
  error,
  hint,
  children,
}: FormFieldProps) {
  return (
    <div className="space-y-2">
      <Label htmlFor={name} className="font-bold">
        {label} {required && <span className="text-red-500">*</span>}
      </Label>
      {children}
      {hint && !error && (
        <p className="text-sm text-muted-foreground">{hint}</p>
      )}
      {error && <InlineError message={error} />}
    </div>
  )
}
```

#### FormInput Component

```typescript
// components/forms/form-input.tsx
import { forwardRef } from 'react'
import { Input } from '@/components/ui/input'
import { FormField } from './form-field'

interface FormInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string
  name: string
  required?: boolean
  error?: string
  hint?: string
}

export const FormInput = forwardRef<HTMLInputElement, FormInputProps>(
  ({ label, name, required, error, hint, ...props }, ref) => {
    return (
      <FormField
        label={label}
        name={name}
        required={required}
        error={error}
        hint={hint}
      >
        <Input
          id={name}
          ref={ref}
          className="border-2 border-black"
          aria-invalid={!!error}
          {...props}
        />
      </FormField>
    )
  }
)

FormInput.displayName = 'FormInput'
```

#### Usage with React Hook Form + Zod

```typescript
// components/customers/customer-form-v2.tsx
'use client'

import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { customerSchema, type CustomerFormData } from '@/lib/validation/customer.schema'
import { FormInput } from '@/components/forms/form-input'
import { FormSelect } from '@/components/forms/form-select'
import { Button } from '@/components/ui/button'

export function CustomerFormV2() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CustomerFormData>({
    resolver: zodResolver(customerSchema),
    defaultValues: {
      customerType: 'B2B',
      status: 'LEAD',
    },
  })

  const onSubmit = async (data: CustomerFormData) => {
    try {
      await createCustomer(data)
      toast.success('Customer created successfully')
    } catch (error) {
      ErrorHandler.toast(error as ApiError)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="grid grid-cols-2 gap-4">
        <FormInput
          label="Company Name"
          name="companyName"
          required
          error={errors.companyName?.message}
          {...register('companyName')}
        />
        
        <FormInput
          label="Email"
          name="email"
          type="email"
          error={errors.email?.message}
          hint="Optional"
          {...register('email')}
        />
      </div>
      
      <FormSelect
        label="Customer Type"
        name="customerType"
        required
        error={errors.customerType?.message}
        options={[
          { value: 'B2B', label: 'B2B' },
          { value: 'B2C', label: 'B2C' },
          { value: 'B2B2C', label: 'B2B2C' },
        ]}
        {...register('customerType')}
      />
      
      <Button
        type="submit"
        disabled={isSubmitting}
        className="border-2 border-black"
      >
        {isSubmitting ? 'Creating...' : 'Create Customer'}
      </Button>
    </form>
  )
}
```

---

## 🎯 Implementation Checklist

### Week 2: Caching Strategy

**Backend:**
- [x] Redis configuration with cache regions
- [ ] Add @Cacheable to BranchService read methods
- [ ] Add @CacheEvict to BranchService write methods
- [ ] Add @Cacheable to CustomerService read methods
- [ ] Add @CacheEvict to CustomerService write methods
- [ ] Add @Cacheable to UserService read methods
- [ ] Add @CacheEvict to UserService write methods
- [ ] Test cache hit/miss with Redis logs
- [ ] Performance benchmark: compare with/without cache

**Frontend:**
- [ ] Install @tanstack/react-query
- [ ] Create QueryClient configuration
- [ ] Add QueryClientProvider to root layout
- [ ] Create query keys structure
- [ ] Migrate useBranches to useQuery
- [ ] Migrate useCustomers to useQuery
- [ ] Migrate useUsers to useQuery
- [ ] Add mutation hooks with optimistic updates
- [ ] Add prefetching on hover
- [ ] Test cache invalidation

### Week 3: Form Validation

**Validation Schemas:**
- [ ] Create customer.schema.ts matching Jakarta validation
- [ ] Create contact.schema.ts matching Jakarta validation
- [ ] Create branch.schema.ts matching Jakarta validation
- [ ] Create user.schema.ts matching Jakarta validation
- [ ] Create validation helpers (phone, email, etc.)
- [ ] Test schemas with various inputs

**Form Components:**
- [ ] Create FormField wrapper component
- [ ] Create FormInput with validation
- [ ] Create FormSelect with validation
- [ ] Create FormTextarea with validation
- [ ] Create FormCheckbox with validation
- [ ] Create FormDatePicker with validation
- [ ] Test components in isolation
- [ ] Integrate with existing forms

**Form Migration:**
- [ ] Update CustomerForm to use new components
- [ ] Update ContactForm to use new components
- [ ] Update BranchForm to use new components (if exists)
- [ ] Test form submission with validation
- [ ] Test field-level errors display
- [ ] Test API error integration

---

## 📊 Expected Benefits

### Performance Improvements

**Backend Caching:**
- **80-90% reduction** in database queries for read-heavy operations
- **50-100ms faster** API response time for cached data
- **Reduced database load** - fewer connections, less CPU usage
- **Horizontal scaling** - shared cache across multiple backend instances

**Frontend Caching (React Query):**
- **Instant navigation** - data loaded from cache
- **Optimistic updates** - immediate UI feedback
- **Smart refetching** - only fetch when stale
- **Background updates** - keep data fresh without blocking UI
- **Reduced network traffic** - 50-70% fewer API calls

### Developer Experience

**Validation Benefits:**
- **Type-safe forms** - TypeScript errors for invalid data
- **Consistent validation** - same rules on frontend/backend
- **Better error messages** - user-friendly validation feedback
- **Faster development** - reusable form components
- **Easier testing** - isolated validation logic

**React Query Benefits:**
- **Less boilerplate** - no manual loading/error states
- **DevTools integration** - inspect queries in browser
- **Automatic retries** - resilient to network failures
- **Request deduplication** - prevent duplicate API calls
- **Garbage collection** - automatic cache cleanup

---

## 🚀 Deployment Strategy

### Phase 1: Backend Caching (Zero Downtime)
1. Deploy Redis configuration ✅
2. Add @Cacheable annotations to read methods
3. Test cache hit/miss in staging
4. Deploy to production with monitoring
5. Add @CacheEvict to write methods
6. Monitor cache invalidation

### Phase 2: Frontend Caching (Progressive)
1. Install React Query in development
2. Create QueryClient configuration
3. Migrate one hook at a time (branches → customers → users)
4. Test in development thoroughly
5. Deploy to staging for QA
6. Deploy to production with feature flag

### Phase 3: Form Validation (Incremental)
1. Create Zod schemas matching backend
2. Build reusable form components
3. Test components in isolation
4. Migrate one form at a time
5. A/B test old vs new forms
6. Full rollout after validation

---

## 📝 Testing Plan

### Cache Testing
```bash
# Start Redis
docker-compose up -d redis

# Monitor cache operations
docker exec -it crm-redis redis-cli -a redis_password_2024
> MONITOR

# Test cache hit
curl http://localhost:8080/api/branches/{id}  # First call - cache miss
curl http://localhost:8080/api/branches/{id}  # Second call - cache hit

# Check cache keys
> KEYS branches::*

# Check TTL
> TTL branches::123e4567-e89b-12d3-a456-426614174000
```

### React Query Testing
```typescript
// Test with React Query DevTools
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'

<QueryClientProvider client={queryClient}>
  <App />
  <ReactQueryDevtools initialIsOpen={false} />
</QueryClientProvider>

// Inspect queries in browser
// - See query status (loading, success, error)
// - View cached data
// - Manually trigger refetch
// - Clear cache
```

### Validation Testing
```typescript
// Test schema validation
import { customerSchema } from '@/lib/validation/customer.schema'

describe('Customer Schema', () => {
  it('should validate valid customer data', () => {
    const validData = {
      companyName: 'Acme Corp',
      email: 'contact@acme.com',
      customerType: 'B2B',
      status: 'LEAD',
    }
    
    expect(() => customerSchema.parse(validData)).not.toThrow()
  })
  
  it('should reject invalid email', () => {
    const invalidData = {
      companyName: 'Acme Corp',
      email: 'invalid-email',
      customerType: 'B2B',
      status: 'LEAD',
    }
    
    expect(() => customerSchema.parse(invalidData)).toThrow('Invalid email format')
  })
})
```

---

## 🎓 Key Learnings

### Caching Best Practices
1. **Short TTL for dynamic data** - branches, customers (5 min)
2. **Long TTL for static data** - roles, permissions (1 hour)
3. **Cache by tenant** - multi-tenant isolation
4. **Evict on write** - keep cache consistent
5. **Handle errors gracefully** - don't fail on cache errors

### React Query Best Practices
1. **Structured query keys** - easy invalidation
2. **Optimistic updates** - better UX
3. **Prefetching** - faster navigation
4. **Stale time = cache duration** - match backend TTL
5. **Error handling** - consistent with ErrorHandler

### Form Validation Best Practices
1. **Match backend rules** - consistent validation
2. **Reusable components** - DRY principle
3. **Accessible errors** - aria-invalid, role="alert"
4. **Progressive enhancement** - works without JS
5. **Type-safe schemas** - catch errors at compile time

---

**Next Steps:** Begin implementation following this guide, starting with backend @Cacheable annotations.
