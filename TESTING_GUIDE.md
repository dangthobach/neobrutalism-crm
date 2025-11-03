# Testing Guide - Authentication & Users Module

## Prerequisites

### 1. Start Backend Server
```bash
mvn spring-boot:run
```

Verify backend is running:
- Check logs for: "Tomcat started on port(s): 8080"
- Access: http://localhost:8080/swagger-ui.html

### 2. Start Frontend Server
```bash
npm run dev
```

Verify frontend is running:
- Check logs for: "Local: http://localhost:3000"
- Access: http://localhost:3000

---

## Test Suite 1: Authentication Flow

### Test 1.1: Login Success
1. Navigate to http://localhost:3000/login
2. Enter valid credentials:
   - Username: `admin` (or your test user)
   - Password: `password`
3. Click "Login"

**Expected Results:**
- ✅ Loading spinner appears
- ✅ Success toast message
- ✅ Redirect to `/admin` dashboard
- ✅ User info displayed in sidebar
- ✅ Tokens stored in localStorage:
  - `access_token`
  - `refresh_token`
  - `access_token_expires_at`

**Verify in Browser DevTools:**
```javascript
// Check localStorage
localStorage.getItem('access_token')
localStorage.getItem('refresh_token')
localStorage.getItem('access_token_expires_at')
```

### Test 1.2: Login Failure
1. Navigate to http://localhost:3000/login
2. Enter invalid credentials
3. Click "Login"

**Expected Results:**
- ✅ Error toast message
- ✅ Stay on login page
- ✅ No tokens in localStorage

### Test 1.3: Protected Route Access (Unauthenticated)
1. Clear all tokens from localStorage
2. Navigate to http://localhost:3000/admin/users

**Expected Results:**
- ✅ Redirect to `/login?returnUrl=/admin/users`
- ✅ After login, redirect back to `/admin/users`

### Test 1.4: Logout
1. Login successfully
2. Click "Logout" button in sidebar

**Expected Results:**
- ✅ Redirect to `/login`
- ✅ All tokens cleared from localStorage
- ✅ Refresh timer cancelled

---

## Test Suite 2: Token Refresh

### Test 2.1: Automatic Token Refresh
1. Login successfully
2. Note the `access_token_expires_at` timestamp
3. Wait until 60 seconds before expiry

**Expected Results:**
- ✅ Token automatically refreshes before expiry
- ✅ New `access_token` in localStorage
- ✅ New `refresh_token` in localStorage
- ✅ New `access_token_expires_at` timestamp
- ✅ No interruption to user experience

**Verify in Console:**
```javascript
// Monitor token refresh
const expiresAt = Number(localStorage.getItem('access_token_expires_at'))
const now = Date.now()
const timeLeft = (expiresAt - now) / 1000 / 60 // minutes
console.log(`Token expires in ${timeLeft} minutes`)
```

### Test 2.2: 401 Error Token Refresh
1. Login successfully
2. Manually expire the access token (or wait for expiry)
3. Make an API call (e.g., navigate to Users page)

**Expected Results:**
- ✅ API returns 401 Unauthorized
- ✅ Client automatically attempts token refresh
- ✅ Original request retried with new token
- ✅ Data loads successfully
- ✅ No redirect to login

### Test 2.3: Token Refresh Failure
1. Login successfully
2. Manually invalidate the refresh token:
```javascript
localStorage.setItem('refresh_token', 'invalid_token')
```
3. Expire access token
4. Make an API call

**Expected Results:**
- ✅ Token refresh fails
- ✅ All tokens cleared
- ✅ Redirect to `/login`
- ✅ Return URL preserved

---

## Test Suite 3: Permission System

### Test 3.1: View Users List
1. Login with user having `users:read` or `canView` permission
2. Navigate to `/admin/users`

**Expected Results:**
- ✅ Users list visible
- ✅ Table shows user data
- ✅ "View Details" button visible

### Test 3.2: Create User Permission
1. Login with user having `users:create` or `canCreate` permission
2. Navigate to `/admin/users`

**Expected Results:**
- ✅ "New User" button visible
- ✅ Clicking button opens create dialog
- ✅ Can submit form successfully

**Without Permission:**
- ✅ "New User" button hidden

### Test 3.3: Edit User Permission
1. Login with user having `users:update` or `canEdit` permission
2. Navigate to `/admin/users`

**Expected Results:**
- ✅ "Edit" button visible for each user
- ✅ "Manage Roles" button visible
- ✅ "Manage Groups" button visible
- ✅ Clicking button opens edit dialog

**Without Permission:**
- ✅ All edit buttons hidden

### Test 3.4: Delete User Permission
1. Login with user having `users:delete` or `canDelete` permission
2. Navigate to `/admin/users`

**Expected Results:**
- ✅ "Delete" button visible for each user
- ✅ Clicking button shows confirmation
- ✅ Can delete successfully

**Without Permission:**
- ✅ "Delete" button hidden

---

## Test Suite 4: Users CRUD Operations

### Test 4.1: Create User
1. Login and navigate to `/admin/users`
2. Click "New User"
3. Fill in form:
   - Username: `testuser123`
   - Email: `test@example.com`
   - Password: `Password123!`
   - First Name: `Test`
   - Last Name: `User`
4. Click "Save"

**Expected Results:**
- ✅ Success toast message
- ✅ Dialog closes
- ✅ User appears in list
- ✅ Table refreshes automatically

**API Call:**
```
POST /api/users
{
  "username": "testuser123",
  "email": "test@example.com",
  "password": "Password123!",
  "firstName": "Test",
  "lastName": "User"
}
```

### Test 4.2: View User Details
1. Click eye icon on any user
2. View details dialog opens

**Expected Results:**
- ✅ Dialog shows user information
- ✅ All fields populated correctly
- ✅ Status displayed with color coding
- ✅ Created/Updated timestamps shown

### Test 4.3: Update User
1. Click "Edit" on any user
2. Modify fields:
   - First Name: `Updated`
   - Last Name: `Name`
3. Click "Save"

**Expected Results:**
- ✅ Success toast message
- ✅ Dialog closes
- ✅ Changes reflected in list
- ✅ Table refreshes automatically

**API Call:**
```
PUT /api/users/{id}
{
  "firstName": "Updated",
  "lastName": "Name"
}
```

### Test 4.4: Delete User
1. Click delete button (trash icon) on any user
2. Confirm deletion

**Expected Results:**
- ✅ Confirmation prompt appears
- ✅ Success toast after confirmation
- ✅ User removed from list
- ✅ Table refreshes automatically

**API Call:**
```
DELETE /api/users/{id}
```

---

## Test Suite 5: User Status Management

### Test 5.1: Activate User
1. Find a user with status != ACTIVE
2. Click "Activate" button
3. Enter reason (optional)
4. Submit

**Expected Results:**
- ✅ Success toast message
- ✅ Status changes to ACTIVE (green)
- ✅ "Activate" button hidden
- ✅ "Suspend" button appears

**API Call:**
```
POST /api/users/{id}/activate
{
  "reason": "User verified"
}
```

### Test 5.2: Suspend User
1. Find a user with status = ACTIVE
2. Click "Suspend" button
3. Enter reason (required)
4. Submit

**Expected Results:**
- ✅ Success toast message
- ✅ Status changes to SUSPENDED (yellow)
- ✅ "Suspend" button hidden
- ✅ "Activate" button appears

**API Call:**
```
POST /api/users/{id}/suspend
{
  "reason": "Policy violation"
}
```

### Test 5.3: Lock User
1. Find a user with status != LOCKED
2. Click "Lock" button
3. Enter reason (required)
4. Submit

**Expected Results:**
- ✅ Success toast message
- ✅ Status changes to LOCKED (red)
- ✅ "Lock" button hidden
- ✅ "Unlock" button appears

**API Call:**
```
POST /api/users/{id}/lock
{
  "reason": "Security concern"
}
```

### Test 5.4: Unlock User
1. Find a user with status = LOCKED
2. Click "Unlock" button
3. Enter reason (optional)
4. Submit

**Expected Results:**
- ✅ Success toast message
- ✅ Status changes to ACTIVE (green)
- ✅ "Unlock" button hidden
- ✅ Other status buttons appear

**API Call:**
```
POST /api/users/{id}/unlock
{
  "reason": "Issue resolved"
}
```

---

## Test Suite 6: Search & Filters

### Test 6.1: Global Search
1. Navigate to `/admin/users`
2. Type in global search box: `admin`

**Expected Results:**
- ✅ List filters to show matching users
- ✅ Matches any field (username, email, name)
- ✅ Search is case-insensitive
- ✅ Results update as you type

### Test 6.2: Column Filters
1. Navigate to `/admin/users`
2. Enter filter in "Username" column: `test`

**Expected Results:**
- ✅ List filters to show matching usernames
- ✅ Other columns still visible
- ✅ Can combine with other filters
- ✅ Clear filter resets list

### Test 6.3: Sorting
1. Click column header "Username"

**Expected Results:**
- ✅ List sorts by username ascending
- ✅ Arrow icon shows sort direction
- ✅ Click again to sort descending
- ✅ Click third time to remove sort

### Test 6.4: Pagination
1. Navigate to page with >10 users
2. Use pagination controls

**Expected Results:**
- ✅ Shows correct page numbers
- ✅ "Next" button navigates forward
- ✅ "Previous" button navigates backward
- ✅ Can change page size (10, 20, 50, 100)
- ✅ Maintains filters when paginating

---

## Test Suite 7: Error Handling

### Test 7.1: Network Error
1. Stop backend server
2. Try to load users list

**Expected Results:**
- ✅ Error message displayed
- ✅ "Retry" button shown
- ✅ Clicking retry attempts reload
- ✅ No white screen of death

### Test 7.2: Validation Errors
1. Try to create user with invalid data:
   - Empty username
   - Invalid email format
   - Short password

**Expected Results:**
- ✅ Validation error messages displayed
- ✅ Form submission prevented
- ✅ Error messages clear on fix

### Test 7.3: Concurrent Operations
1. Start deleting a user
2. Immediately try to edit same user

**Expected Results:**
- ✅ Both operations handled correctly
- ✅ No race conditions
- ✅ UI state remains consistent

---

## Test Suite 8: Performance

### Test 8.1: Large Dataset
1. Load page with 1000+ users
2. Use search and filters

**Expected Results:**
- ✅ Page loads in <2 seconds
- ✅ Search results instant (<500ms)
- ✅ No UI lag when typing
- ✅ Pagination smooth

### Test 8.2: Multiple Tabs
1. Open `/admin/users` in multiple tabs
2. Make changes in one tab

**Expected Results:**
- ✅ Each tab maintains own state
- ✅ Token refresh works in all tabs
- ✅ No conflicts between tabs

---

## Debugging Tools

### Browser DevTools
```javascript
// Check authentication state
console.log('Access Token:', localStorage.getItem('access_token'))
console.log('Refresh Token:', localStorage.getItem('refresh_token'))
console.log('Expires At:', new Date(Number(localStorage.getItem('access_token_expires_at'))))

// Check API calls
// Go to Network tab → Filter by XHR
// Verify request headers include Authorization: Bearer <token>

// Check user permissions
// In React DevTools → Find AuthContext
// Check user.permissions array
```

### Backend Logs
```bash
# Monitor backend logs for:
# - Incoming API requests
# - Authentication/authorization decisions
# - Database queries
# - Error stack traces
```

### Common Issues & Solutions

**Issue:** 401 errors after login
**Solution:** Check token format, verify backend JWT secret

**Issue:** Permissions not working
**Solution:** Verify user has roles/permissions assigned, check menu permissions API

**Issue:** Token refresh loop
**Solution:** Check token expiry times, verify refresh endpoint

**Issue:** CORS errors
**Solution:** Check backend CORS configuration, verify allowed origins

---

## Manual Test Checklist

### Authentication
- [ ] Login with valid credentials
- [ ] Login with invalid credentials
- [ ] Logout functionality
- [ ] Protected route redirect
- [ ] Return URL after login
- [ ] Token stored in localStorage
- [ ] Token cleared on logout

### Token Refresh
- [ ] Automatic refresh before expiry
- [ ] 401 triggers refresh
- [ ] Refresh failure logs out
- [ ] No concurrent refresh calls
- [ ] Pending requests retry after refresh

### Permissions
- [ ] canView shows/hides view elements
- [ ] canCreate shows/hides create button
- [ ] canEdit shows/hides edit buttons
- [ ] canDelete shows/hides delete buttons
- [ ] Permission guards work on all actions

### Users CRUD
- [ ] Create new user
- [ ] View user details
- [ ] Update user info
- [ ] Delete user
- [ ] All operations show loading state
- [ ] Success/error messages appear

### Status Management
- [ ] Activate user
- [ ] Suspend user
- [ ] Lock user
- [ ] Unlock user
- [ ] Status colors correct
- [ ] Status buttons show/hide correctly

### Search & Filters
- [ ] Global search works
- [ ] Column filters work
- [ ] Sorting works
- [ ] Pagination works
- [ ] Clear filters resets list

### Error Handling
- [ ] Network errors handled
- [ ] Validation errors shown
- [ ] Retry button works
- [ ] No crashes or white screens

---

## Automated Testing (Future)

### Unit Tests
```typescript
// Example: Test usePermission hook
describe('usePermission', () => {
  it('should return true for user with permission', () => {
    // Test implementation
  })
})
```

### Integration Tests
```typescript
// Example: Test login flow
describe('Login Flow', () => {
  it('should login and redirect to admin', () => {
    // Test implementation
  })
})
```

### E2E Tests (Playwright/Cypress)
```typescript
// Example: Test full CRUD workflow
test('User CRUD operations', async ({ page }) => {
  // Test implementation
})
```

---

## Performance Benchmarks

### Target Metrics
- **Page Load:** <2 seconds
- **API Response:** <500ms
- **Search Results:** <300ms
- **Token Refresh:** <200ms
- **UI Interaction:** <100ms

### Monitoring
- Use Chrome DevTools Performance tab
- Monitor Network waterfall
- Check React DevTools Profiler
- Measure API endpoint response times

---

## Security Testing

### Checklist
- [ ] Tokens expire correctly
- [ ] Refresh token rotation works
- [ ] XSS protection in place
- [ ] CSRF tokens (if using cookies)
- [ ] SQL injection prevented
- [ ] Authorization checks on all endpoints
- [ ] Rate limiting works
- [ ] Audit logs capture events

---

## Next Steps After Testing

1. **Document Issues:** Create GitHub issues for any bugs found
2. **Performance Optimization:** Address any slow operations
3. **User Feedback:** Collect feedback from actual users
4. **Monitoring:** Set up error tracking (Sentry, etc.)
5. **Load Testing:** Test with realistic user loads
6. **Security Audit:** Professional security review

---

## Support & Resources

- **Backend API Docs:** http://localhost:8080/swagger-ui.html
- **Frontend Docs:** `/PHASE4_AUTHENTICATION_COMPLETE.md`
- **Permission System:** `/PERMISSION_SYSTEM_ROADMAP.md`
- **Backend Guide:** `/backend.md`
