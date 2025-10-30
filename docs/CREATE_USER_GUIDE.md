# Guide: Create User

## Fix Applied

**File:** [UserController.java](../src/main/java/com/neobrutalism/crm/domain/user/controller/UserController.java#L92-L114)

**Change:** Made `organizationId` optional - if not provided or null, user will be assigned to "default" tenant.

## How to Create User

### Method 1: Without Organization (Recommended for Testing)

```bash
# Login first
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Extract token from response
TOKEN="<paste_access_token_here>"

# Create user WITHOUT organizationId
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecureP@ssw0rd",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1-234-567-8900"
  }'
```

**Result:** User created with `tenantId = "default"` and `organizationId = null`

### Method 2: With Organization UUID

First, get a valid organization UUID:

```bash
# Get organizations
curl -X GET http://localhost:8080/api/organizations \
  -H "Authorization: Bearer $TOKEN"
```

Response example:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "018e0011-1111-2222-3333-444444444444",
        "code": "ACME",
        "name": "ACME Corporation"
      }
    ]
  }
}
```

Then create user with that organization:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "SecureP@ssw0rd",
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1-234-567-8900",
    "organizationId": "018e0011-1111-2222-3333-444444444444"
  }'
```

**Result:** User created with specified organizationId and matching tenantId

## Frontend Usage (React)

### Without Organization

```typescript
const createUserMutation = useCreateUser();

const handleSubmit = async (data: UserFormData) => {
  await createUserMutation.mutateAsync({
    username: data.username,
    email: data.email,
    password: data.password,
    firstName: data.firstName,
    lastName: data.lastName,
    phone: data.phone,
    // Don't include organizationId field at all
  });
};
```

### With Organization

```typescript
const createUserMutation = useCreateUser();

const handleSubmit = async (data: UserFormData) => {
  await createUserMutation.mutateAsync({
    username: data.username,
    email: data.email,
    password: data.password,
    firstName: data.firstName,
    lastName: data.lastName,
    phone: data.phone,
    organizationId: selectedOrgId, // UUID from organization selector
  });
};
```

## Field Validation

### Required Fields
- ✅ username (3-50 chars)
- ✅ email (valid email format)
- ✅ password (8-100 chars)
- ✅ firstName (1-100 chars)
- ✅ lastName (1-100 chars)

### Optional Fields
- ⭕ phone (valid phone format if provided)
- ⭕ avatar (URL, max 500 chars)
- ⭕ **organizationId** (UUID or null/omitted)

## Error Handling

### 400 Bad Request - Validation Errors

**Example: Invalid email**
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email must be valid"
  }
}
```

**Example: Username too short**
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "username": "Username must be between 3 and 50 characters"
  }
}
```

### 409 Conflict - Duplicate

**Example: Username already exists**
```json
{
  "success": false,
  "message": "Username already exists. Please use a unique username.",
  "errorCode": "DUPLICATE_RESOURCE"
}
```

**Example: Email already exists**
```json
{
  "success": false,
  "message": "Email already exists. Please use a unique email.",
  "errorCode": "DUPLICATE_RESOURCE"
}
```

### 403 Forbidden - No Permission

```json
{
  "error": "Access denied",
  "message": "You do not have permission to access this resource"
}
```

**Solution:** Make sure you're logged in with SUPER_ADMIN role or have ROLE_ADMIN with proper permissions.

### 500 Internal Server Error - UUID Parse Error (OLD - NOW FIXED)

**Before Fix:**
```json
{
  "success": false,
  "message": "An unexpected error occurred",
  "errorCode": "INTERNAL_ERROR"
}
```

**After Fix:** organizationId can be null or omitted entirely!

## Testing

### Test 1: Create user without organization

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  jq -r '.data.accessToken')

curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "email": "test1@example.com",
    "password": "Test@1234",
    "firstName": "Test",
    "lastName": "User"
  }' | jq
```

**Expected Response:**
```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "id": "...",
    "username": "testuser1",
    "email": "test1@example.com",
    "firstName": "Test",
    "lastName": "User",
    "tenantId": "default",
    "organizationId": null,
    "status": "ACTIVE"
  }
}
```

### Test 2: List all users

```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer $TOKEN" | jq
```

Should see the newly created user in the list.

### Test 3: Get user by username

```bash
curl -X GET http://localhost:8080/api/users/username/testuser1 \
  -H "Authorization: Bearer $TOKEN" | jq
```

## Related Files

- [UserController.java](../src/main/java/com/neobrutalism/crm/domain/user/controller/UserController.java) - User API endpoints
- [UserRequest.java](../src/main/java/com/neobrutalism/crm/domain/user/dto/UserRequest.java) - Request DTO with validation
- [User.java](../src/main/java/com/neobrutalism/crm/domain/user/model/User.java) - User entity
- [UserService.java](../src/main/java/com/neobrutalism/crm/domain/user/service/UserService.java) - Business logic

## Next Steps

After creating a user, you can:

1. **Assign roles:** POST `/api/users/{userId}/roles/{roleId}`
2. **Add to groups:** POST `/api/groups/{groupId}/members/{userId}`
3. **Set permissions:** Configure role-menu permissions
4. **Login with new user:** POST `/api/auth/login` with new credentials

## Summary

✅ **Fixed:** organizationId is now optional
✅ **Default tenant:** Users without organization get "default" tenant
✅ **No more UUID parse errors:** Can omit or set null for organizationId
✅ **Backwards compatible:** Existing code with organizationId still works

Now you can easily create test users without needing to provide organization UUIDs!
