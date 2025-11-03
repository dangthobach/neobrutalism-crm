# Auth API & Customer API Fix Summary

**Date:** November 4, 2025  
**Status:** ✅ FIXED

## 🔴 Issues Fixed

### 1. Login API Response Mismatch (Frontend ↔ Backend)

#### **Problem:**
Frontend expected nested `user` object with `organizationId` and `permissions`, but backend returned flat structure.

**Frontend Expected:**
```typescript
{
  user: {
    id: string
    username: string
    email: string
    organizationId: string  // ❌ Not in backend
    permissions: string[]   // ❌ Not in backend
  }
}
```

**Backend Actually Returns:**
```java
{
  userId: UUID
  username: string
  email: string
  firstName: string
  lastName: string
  fullName: string
  tenantId: string
  roles: Set<String>
  // ❌ No organizationId or permissions
}
```

#### **Solution:**
✅ Updated Frontend types and auth context to match backend response structure

**Files Changed:**
- `src/lib/api/auth.ts` - Updated LoginResponse interface
- `src/contexts/auth-context.tsx` - Fixed user state mapping

---

### 2. Customer API Routing Conflict

#### **Problem 1: `/api/customers/stats` matched `/{id}` pattern**
```
GET /api/customers/stats/by-status
❌ Routed to: getCustomerById(id="stats")
❌ Error: Invalid UUID string: stats
```

#### **Solution:**
✅ **Moved stats endpoints BEFORE `/{id}` endpoint** to ensure specific routes match first

**Route Order (Fixed):**
```java
@GetMapping                           // /api/customers
@GetMapping("/stats/by-status")      // /api/customers/stats/by-status  ✅ BEFORE /{id}
@GetMapping("/stats/by-type")        // /api/customers/stats/by-type    ✅ BEFORE /{id}
@GetMapping("/{id}")                 // /api/customers/{id}             ✅ AFTER stats
```

---

#### **Problem 2: Invalid `sortBy` field name**
```
GET /api/customers?sortBy=name
❌ Error: No property 'name' found for type 'Customer'
```

Customer entity has `companyName`, not `name`.

#### **Solution:**
✅ **Added field name validation and mapping** in `getAllCustomers()`

**Field Mapping:**
```java
"name" → "companyName"
"company" → "companyName"
"type" → "customerType"
"owner" → "ownerId"
"createdAt" → "createdAt"
// ... and more
```

**Files Changed:**
- `src/main/java/com/neobrutalism/crm/domain/customer/controller/CustomerController.java`
  - Moved stats endpoints before `/{id}`
  - Added `validateAndMapSortField()` method
  - **Fixed duplicate case statements** (compilation error)

---

## 📊 Test Results

### Before Fix:
```
❌ POST /api/auth/login → Error: No data returned from API
❌ GET /api/customers/stats/by-status → 500: Invalid UUID string: stats
❌ GET /api/customers?sortBy=name → 500: No property 'name' found
```

### After Fix:
```
✅ POST /api/auth/login → Returns correct user data
✅ GET /api/customers/stats/by-status → Returns count by status
✅ GET /api/customers?sortBy=name → Sorts by companyName
✅ GET /api/customers/{uuid} → Returns customer by ID
```

---

## 🔧 Technical Details

### API Client Enhancement
The API client already auto-unwraps `ApiResponse<T>` wrapper:
```typescript
// Backend returns: { success: true, data: {...}, message: "..." }
// API client returns: {...} (unwrapped data)
```

### Route Specificity in Spring MVC
Spring MVC matches routes **in declaration order** for same HTTP method. More specific patterns should be declared **before** generic patterns:

```java
// ✅ CORRECT ORDER
@GetMapping("/stats")         // More specific
@GetMapping("/{id}")          // Less specific (catches everything)

// ❌ WRONG ORDER  
@GetMapping("/{id}")          // Would match /api/customers/stats
@GetMapping("/stats")         // Never reached!
```

### Field Name Validation
Always validate user input for `sortBy` parameter to prevent:
- SQL injection via entity field names
- PropertyReferenceException for non-existent fields
- Accidental exposure of internal field names

---

## ✅ Next Steps

1. **Apply same patterns to other controllers:**
   - Organizations
   - Users
   - Roles
   - Groups

2. **Add integration tests:**
   - Test route specificity
   - Test field name validation
   - Test all stats endpoints

3. **Document API conventions:**
   - Stats endpoints always use `/stats/` prefix
   - ID-based endpoints use `/{id}` pattern
   - List all valid `sortBy` field names in API docs

---

## 📝 Lessons Learned

1. ✅ **Always align Frontend types with Backend DTOs**
2. ✅ **Declare specific routes before generic patterns**
3. ✅ **Validate and sanitize user input for dynamic queries**
4. ✅ **Use API client response unwrapping consistently**
5. ✅ **Test all route patterns to avoid conflicts**

---

**Status:** All issues resolved ✨  
**Server:** Running on port 8080  
**TypeScript Errors:** 0  
**Backend Errors:** Fixed
