# KẾ HOẠCH TRIỂN KHAI TIẾP THEO - NEOBRUTALISM CRM

**Ngày đánh giá:** 24/11/2025
**Tiến độ hiện tại:** 85% hoàn thành
**Mục tiêu:** Đạt 95% và sẵn sàng production trong 4 tuần

---

## 📊 TÌNH TRẠNG HIỆN TẠI

### ✅ Hoàn thành 100%
- Core CRM (Customer, Contact, Organization, Branch)
- Content Management System (21 files, 3K LOC)
- Learning Management System (21 files, 5K LOC)
- User Management & JWT Authentication
- Role & Permission System (Casbin RBAC)
- Infrastructure (Redis, PostgreSQL, MinIO, WebSocket)

### 🟡 Hoàn thành 60-80%
- **Task Management: 80%**
  - ✅ Backend CRUD APIs
  - ✅ Kanban board view
  - ✅ Task edit modal
  - ⏳ Task detail page (Comments, Checklist, Activity)
  - ⏳ Bulk operations

- **Notification System: 60%**
  - ✅ Backend entities & APIs
  - ✅ WebSocket real-time delivery
  - ✅ Email notifications (with 10MB attachments)
  - ✅ Multi-channel preferences (in-app, email, SMS)
  - ⏳ Push notifications (Firebase)
  - ⏳ Quiet hours implementation
  - ⏳ Digest mode (daily summaries)

- **Excel Migration: 70%**
  - ✅ SAX parser for multi-sheet reading
  - ✅ Staging tables & validation
  - ✅ Contract/CIF transformation
  - ⏳ Reactive processing adapter

### ⚠️ VẤN ĐỀ NGHIÊM TRỌNG
1. **Testing Coverage: 0% Frontend, 30% Backend** 🔴
2. **Security: JWT secret trong config file** 🔴
3. **Error Handling: Thiếu error boundaries** 🟡
4. **Production Readiness: Chưa có load testing** 🟡

---

## 🎯 PHASE 1: STABILIZATION (Tuần 1-2) - 9 ngày

**Mục tiêu:** Hệ thống ổn định, sẵn sàng production

### TUẦN 1: TESTING INFRASTRUCTURE (5 ngày)

#### Day 1: Setup Testing Framework
```bash
# Frontend Testing Setup
□ Install dependencies
  npm install --save-dev jest @testing-library/react @testing-library/jest-dom
  npm install --save-dev @testing-library/user-event vitest
  npm install --save-dev @playwright/test

□ Configure Jest/Vitest
  - Create jest.config.js or vitest.config.ts
  - Setup test environment (jsdom)
  - Add test scripts to package.json

□ Configure Playwright E2E
  - npx playwright install
  - Create playwright.config.ts
  - Setup test database for E2E

□ Add test coverage reporting
  - Configure coverage thresholds (60% minimum)
  - Add coverage badges to README
```

**Files to create:**
- `jest.config.js` or `vitest.config.ts`
- `playwright.config.ts`
- `__tests__/setup.ts`

#### Day 2: Write Component Unit Tests
```typescript
// Priority components to test:
□ src/components/tasks/task-board.tsx
  - Test drag and drop functionality
  - Test status transitions
  - Test filtering logic

□ src/components/tasks/task-card.tsx
  - Test rendering with different props
  - Test action buttons (edit, delete)
  - Test priority/status badges

□ src/components/notifications/notification-item.tsx
  - Test mark as read
  - Test archive action
  - Test navigation on click

□ src/components/notifications/notification-list.tsx
  - Test empty state
  - Test loading skeleton
  - Test sorting logic
```

**Target:** 20+ component tests, 50% component coverage

#### Day 3: Write Hook Tests
```typescript
// Priority hooks to test:
□ src/hooks/useTasks.ts
  - Test fetching tasks
  - Test creating tasks
  - Test updating tasks
  - Test error handling

□ src/hooks/useNotifications.ts
  - Test pagination
  - Test filtering
  - Test mark as read mutation
  - Test cache invalidation

□ src/hooks/useCurrentUser.ts
  - Test user fetching
  - Test organization context
  - Test caching behavior
```

**Target:** 15+ hook tests, 70% hook coverage

#### Day 4: Backend Integration Tests
```java
// Priority services to test:
□ TaskService.java
  - Test createTask with organizationId auto-set
  - Test updateTask with authorization
  - Test deleteTask cascade behavior
  - Test bulk operations

□ NotificationService.java
  - Test notification creation
  - Test preference filtering
  - Test email sending
  - Test WebSocket broadcasting

□ NotificationPreferenceService.java
  - Test cache hit/miss
  - Test default preferences creation
  - Test batch updates
  - Test quiet hours logic
```

**Files to create:**
- `src/test/java/com/neobrutalism/crm/domain/task/service/TaskServiceTest.java`
- `src/test/java/com/neobrutalism/crm/domain/notification/service/NotificationServiceTest.java`
- `src/test/java/com/neobrutalism/crm/domain/notification/service/NotificationPreferenceServiceTest.java`

**Target:** 30+ integration tests, 60% backend coverage

#### Day 5: E2E Critical Flows
```typescript
// Critical user journeys:
□ test/e2e/auth.spec.ts
  - User login flow
  - Token refresh
  - Logout

□ test/e2e/tasks.spec.ts
  - Create task
  - Move task between columns
  - Edit task
  - Delete task
  - Bulk operations

□ test/e2e/notifications.spec.ts
  - Receive notification
  - Mark as read
  - Filter notifications
  - Update preferences
```

**Target:** 5+ E2E scenarios, all critical paths covered

---

### TUẦN 2: SECURITY & ERROR HANDLING (4 ngày)

#### Day 1-2: Security Hardening

**Backend Security:**
```java
□ Move JWT secret to environment variables
  - Update application.yml to use ${JWT_SECRET:default}
  - Add JWT_SECRET to .env.example
  - Update deployment docs

□ Add CSRF protection
  - Configure CsrfTokenRepository
  - Add CSRF headers to API requests
  - Test CSRF protection

□ Security audit
  - Review authentication flows
  - Check authorization on all endpoints
  - Verify multi-tenant isolation
  - Test rate limiting

□ Add security headers
  - X-Content-Type-Options: nosniff
  - X-Frame-Options: DENY
  - Strict-Transport-Security
  - Content-Security-Policy
```

**Files to modify:**
- `src/main/resources/application.yml`
- `src/main/java/com/neobrutalism/crm/config/SecurityConfig.java`
- `.env.example`

**Frontend Security:**
```typescript
□ Implement XSS protection
  - Sanitize user inputs
  - Use DOMPurify for rich text
  - Escape HTML in notifications

□ Add CSP headers
  - Configure in next.config.js
  - Test with nonce-based scripts

□ Secure sensitive data
  - Never log tokens
  - Clear sensitive data on logout
  - Use secure cookies
```

#### Day 3-4: Error Handling

**Frontend Error Boundaries:**
```typescript
□ Create global error boundary
  // src/components/error-boundary.tsx
  - Catch React errors
  - Display user-friendly message
  - Log to error tracking service (Sentry)

□ Add error boundaries to critical components
  - Task board
  - Notification center
  - Course player

□ Improve API error handling
  - Standardize error responses
  - Show toast notifications on errors
  - Retry logic for network failures
```

**Files to create:**
- `src/components/error-boundary.tsx`
- `src/app/error.tsx` (Next.js global error)
- `src/lib/error-handler.ts`

**Backend Error Handling:**
```java
□ Create global exception handler
  // @ControllerAdvice
  - Handle all exceptions consistently
  - Return standardized error responses
  - Log errors with correlation IDs

□ Improve async error handling
  - Add @Async error handler
  - Dead letter queue for failed jobs
  - Retry logic with exponential backoff

□ Add validation error messages
  - User-friendly field validation messages
  - Internationalization support (i18n)
```

**Files to create:**
- `src/main/java/com/neobrutalism/crm/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/neobrutalism/crm/config/AsyncExceptionHandler.java`

---

## 🎯 PHASE 2: FEATURE COMPLETION (Tuần 3-4) - 7.5 ngày

**Mục tiêu:** Hoàn thiện Task & Notification modules lên 100%

### TUẦN 3: TASK MODULE COMPLETION (3.5 ngày)

#### Day 1-2: Comments System

**Backend:**
```java
□ Create Comment entity
  // src/main/java/com/neobrutalism/crm/domain/task/model/Comment.java
  - id (UUID v7)
  - taskId (FK to Task)
  - userId (FK to User)
  - content (TEXT)
  - parentId (for threaded comments)
  - createdAt, updatedAt

□ Create CommentRepository
  - findByTaskId()
  - findByTaskIdOrderByCreatedAtDesc()

□ Create CommentService
  - addComment(taskId, userId, content, parentId)
  - updateComment(commentId, content)
  - deleteComment(commentId)
  - getComments(taskId)

□ Create CommentController
  - POST /api/tasks/{taskId}/comments
  - PUT /api/tasks/comments/{commentId}
  - DELETE /api/tasks/comments/{commentId}
  - GET /api/tasks/{taskId}/comments

□ WebSocket integration
  - Broadcast new comments in real-time
  - Topic: /topic/tasks/{taskId}/comments
```

**Frontend:**
```typescript
□ Create CommentList component
  // src/components/tasks/comment-list.tsx
  - Display comments with avatars
  - Threaded replies (indent child comments)
  - Timestamp with "2 hours ago" format
  - Edit/delete buttons (only for author)

□ Create AddComment component
  // src/components/tasks/add-comment.tsx
  - Textarea with auto-resize
  - @mention support (dropdown user list)
  - Submit button with loading state
  - Character counter

□ Create useComments hook
  // src/hooks/useComments.ts
  - useQuery for fetching comments
  - useMutation for add/edit/delete
  - WebSocket subscription for real-time updates

□ Integrate into task detail page
  // src/app/admin/tasks/[taskId]/page.tsx
  - Add Comments tab
  - Show comment count in tab label
```

**Database Migration:**
```sql
□ Create V117__Create_comments_table.sql
  - comments table with indexes
  - FK to tasks and users
  - Index on (task_id, created_at)
```

#### Day 2-3: Checklist System

**Backend:**
```java
□ Create ChecklistItem entity
  // src/main/java/com/neobrutalism/crm/domain/task/model/ChecklistItem.java
  - id (UUID v7)
  - taskId (FK to Task)
  - title (VARCHAR 255)
  - completed (BOOLEAN default false)
  - position (INT for ordering)
  - createdAt, updatedAt

□ Create ChecklistRepository
  - findByTaskIdOrderByPosition()
  - countByTaskIdAndCompleted()

□ Create ChecklistService
  - addItem(taskId, title)
  - updateItem(itemId, title, completed)
  - deleteItem(itemId)
  - reorderItems(taskId, List<itemId>)
  - calculateProgress(taskId) -> percentage

□ Create ChecklistController
  - POST /api/tasks/{taskId}/checklist
  - PUT /api/tasks/checklist/{itemId}
  - DELETE /api/tasks/checklist/{itemId}
  - PUT /api/tasks/{taskId}/checklist/reorder
  - GET /api/tasks/{taskId}/checklist
```

**Frontend:**
```typescript
□ Create ChecklistItem component
  // src/components/tasks/checklist-item.tsx
  - Checkbox with animation
  - Inline edit on double-click
  - Delete button (hover to show)
  - Drag handle for reordering

□ Create Checklist component
  // src/components/tasks/checklist.tsx
  - List of ChecklistItem
  - Add new item input
  - Progress bar (completed/total)
  - Drag-and-drop reordering (dnd-kit)

□ Create useChecklist hook
  // src/hooks/useChecklist.ts
  - useQuery for fetching items
  - useMutation for add/edit/delete/reorder
  - Optimistic updates for smooth UX

□ Integrate into task detail page
  // src/app/admin/tasks/[taskId]/page.tsx
  - Add Checklist tab
  - Show progress in sidebar
```

**Database Migration:**
```sql
□ Create V118__Create_checklist_items_table.sql
  - checklist_items table
  - Index on (task_id, position)
```

#### Day 3: Activity Timeline

**Backend:**
```java
□ Enhance existing Activity entity
  // Already exists: src/main/java/com/neobrutalism/crm/domain/activity/model/Activity.java
  - Verify it tracks task events

□ Create ActivityService methods
  - logTaskCreated(taskId, userId)
  - logTaskStatusChanged(taskId, oldStatus, newStatus, userId)
  - logTaskAssigned(taskId, assigneeId, assignerId)
  - logCommentAdded(taskId, commentId, userId)
  - logChecklistProgress(taskId, progress, userId)

□ Update TaskService to log activities
  - createTask() -> log "Task created"
  - updateTask() -> log field changes
  - assignTask() -> log "Assigned to {user}"

□ Create ActivityController endpoint
  - GET /api/tasks/{taskId}/activities
```

**Frontend:**
```typescript
□ Create ActivityTimeline component
  // src/components/tasks/activity-timeline.tsx
  - Vertical timeline with icons
  - Activity cards with user avatars
  - Timestamp formatting
  - Filter by activity type

□ Create useTaskActivities hook
  // src/hooks/useTaskActivities.ts
  - useQuery for fetching activities
  - Pagination (load more)

□ Integrate into task detail page
  // src/app/admin/tasks/[taskId]/page.tsx
  - Add Activity tab
  - Show last 3 activities in sidebar
```

#### Day 4: Bulk Operations

**Backend:**
```java
□ Add bulk endpoints to TaskController
  - POST /api/tasks/bulk/assign
    - Body: { taskIds: UUID[], assigneeId: UUID }
  - POST /api/tasks/bulk/status
    - Body: { taskIds: UUID[], status: TaskStatus }
  - DELETE /api/tasks/bulk
    - Body: { taskIds: UUID[] }

□ Add bulk methods to TaskService
  - bulkAssign(taskIds, assigneeId, userId)
  - bulkUpdateStatus(taskIds, status, userId)
  - bulkDelete(taskIds, userId)
  - Verify user permissions for each task
```

**Frontend:**
```typescript
□ Add multi-select to TaskBoard
  // src/components/tasks/task-board.tsx
  - Checkbox on each task card
  - "Select All" checkbox
  - Bulk action toolbar (shows when items selected)

□ Create BulkActionToolbar component
  // src/components/tasks/bulk-action-toolbar.tsx
  - "Assign to..." dropdown
  - "Change status to..." dropdown
  - "Delete selected" button
  - Confirmation dialogs

□ Create useBulkTasks hook
  // src/hooks/useBulkTasks.ts
  - useMutation for bulk operations
  - Optimistic updates
  - Toast notifications on success/error

□ Add keyboard shortcuts
  - Ctrl+A: Select all
  - Delete: Bulk delete confirmation
  - Escape: Clear selection
```

---

### TUẦN 4: NOTIFICATION MODULE COMPLETION (4 ngày)

#### Day 1-2: Push Notifications (Firebase)

**Backend:**
```java
□ Add Firebase Admin SDK dependency
  // pom.xml
  <dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
  </dependency>

□ Create FirebaseConfig
  // src/main/java/com/neobrutalism/crm/config/FirebaseConfig.java
  - Initialize Firebase with service account
  - Bean for FirebaseMessaging

□ Create DeviceToken entity
  // src/main/java/com/neobrutalism/crm/domain/notification/model/DeviceToken.java
  - userId, token, platform (WEB/ANDROID/IOS)
  - createdAt, lastUsedAt

□ Create PushNotificationService
  // src/main/java/com/neobrutalism/crm/domain/notification/service/PushNotificationService.java
  - sendPush(userId, title, body, data)
  - registerDevice(userId, token, platform)
  - unregisterDevice(userId, token)
  - sendMulticast(userIds, notification)

□ Integrate with NotificationService
  - When creating notification, check if pushEnabled
  - Send push notification asynchronously
```

**Frontend:**
```typescript
□ Setup Firebase SDK
  // npm install firebase
  // src/lib/firebase.ts
  - Initialize Firebase app
  - Get FCM token
  - Request notification permission

□ Create service worker
  // public/firebase-messaging-sw.js
  - Listen for background messages
  - Show notification with actions

□ Create usePushNotifications hook
  // src/hooks/usePushNotifications.ts
  - requestPermission()
  - subscribeToNotifications()
  - unsubscribeFromNotifications()

□ Add notification permission prompt
  // src/components/notifications/push-permission-banner.tsx
  - Show banner if permission not granted
  - "Enable Notifications" button
  - Store preference in localStorage

□ Update notification preferences page
  // src/app/admin/notifications/preferences/page.tsx
  - Add "Push Notifications" toggle
  - Show device tokens list
  - "Test notification" button
```

**Database Migration:**
```sql
□ Create V119__Create_device_tokens_table.sql
  - device_tokens table
  - Index on user_id
  - Unique constraint on (user_id, token)
```

#### Day 3: Quiet Hours Implementation

**Backend:**
```java
□ Update NotificationService
  // src/main/java/com/neobrutalism/crm/domain/notification/service/NotificationService.java
  - isWithinQuietHours(userId, time) -> boolean
  - queueNotification(notification) -> save with scheduled_at
  - sendQueuedNotifications() -> @Scheduled method

□ Create NotificationQueue entity
  // src/main/java/com/neobrutalism/crm/domain/notification/model/NotificationQueue.java
  - notification (embedded)
  - scheduledAt (Instant)
  - status (QUEUED/SENT/FAILED)

□ Add scheduled job
  // @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
  - Check for queued notifications past scheduledAt
  - Send notifications
  - Mark as SENT

□ Update createNotification method
  - Check quiet hours
  - If within quiet hours, queue for later
  - Otherwise, send immediately
```

**Frontend:**
```typescript
□ Update preference form
  // src/app/admin/notifications/preferences/page.tsx
  - Add time pickers for quiet hours start/end
  - Show preview: "Notifications will be queued from 10:00 PM to 8:00 AM"
  - Validate: end time must be after start time

□ Add quiet hours status indicator
  // src/components/notifications/quiet-hours-status.tsx
  - Show badge if currently in quiet hours
  - "Notifications paused until 8:00 AM"
```

#### Day 4: Digest Mode

**Backend:**
```java
□ Create DigestService
  // src/main/java/com/neobrutalism/crm/domain/notification/service/DigestService.java
  - aggregateNotifications(userId, since) -> DigestEmail
  - sendDailyDigest(userId)
  - @Scheduled(cron = "0 0 9 * * *") -> sendAllDigests()

□ Create email template
  // src/main/resources/templates/email/notification-digest.html
  - Professional email layout
  - Group by notification type
  - Summary statistics
  - "View all" button

□ Update NotificationService
  - When digestMode enabled, don't send individual emails
  - Store notifications for digest

□ Add admin endpoint to trigger test digest
  - POST /api/notifications/preferences/me/test-digest
```

**Frontend:**
```typescript
□ Update preferences page
  // src/app/admin/notifications/preferences/page.tsx
  - Add "Digest Mode" toggle
  - Add time picker for digest delivery time
  - Show preview: "You'll receive 1 email per day at 9:00 AM"

□ Add "Send Test Digest" button
  - Trigger test digest email
  - Show success toast
```

---

## 🎯 PHASE 3: NEW FEATURES (Tuần 5-10)

### TUẦN 5-6: OPPORTUNITY MANAGEMENT (10 ngày)

#### Day 1-3: Backend Implementation

**Entities:**
```java
□ Opportunity entity
  - id, name, description
  - customerId (FK to Customer)
  - contactId (FK to Contact)
  - ownerId (FK to User)
  - stage (LEAD/QUALIFIED/PROPOSAL/NEGOTIATION/CLOSED_WON/CLOSED_LOST)
  - amount, currency
  - probability (%)
  - expectedCloseDate
  - actualCloseDate
  - lostReason
  - organizationId

□ OpportunityLineItem entity (products/services in opportunity)
  - opportunityId
  - productName, quantity, unitPrice
  - discount, totalPrice

□ OpportunityStage entity (customizable pipeline stages)
  - organizationId, name, order, probability
  - color (for UI)
```

**Services:**
```java
□ OpportunityService
  - CRUD operations
  - moveToStage(opportunityId, newStage)
  - calculateForecast(organizationId) -> revenue projection
  - getWinLossReport(organizationId, dateRange)
  - getLeaderboard(organizationId, metric) -> top salespeople

□ ForecastService
  - calculateWeightedForecast(organizationId)
  - calculateByStage(organizationId)
  - getTrend(organizationId, period)
```

**Controllers:**
```java
□ OpportunityController
  - Standard CRUD endpoints
  - PUT /api/opportunities/{id}/stage
  - GET /api/opportunities/forecast
  - GET /api/opportunities/reports/win-loss

□ OpportunityStageController
  - CRUD for custom stages
  - PUT /api/opportunity-stages/reorder
```

**Database Migration:**
```sql
□ V120__Create_opportunity_tables.sql
  - opportunities table
  - opportunity_line_items table
  - opportunity_stages table
  - Indexes on organizationId, stage, ownerId
```

#### Day 4-7: Frontend Implementation

**Components:**
```typescript
□ Opportunity Kanban Board
  // src/app/admin/opportunities/page.tsx
  - Drag-and-drop pipeline view
  - Cards show amount, close date, probability
  - Quick actions (edit, delete, mark won/lost)

□ Opportunity Detail Page
  // src/app/admin/opportunities/[id]/page.tsx
  - Header with amount, stage, probability
  - Sidebar: Contact, Customer, Owner
  - Tabs: Overview, Line Items, Activities, Notes
  - Edit inline

□ Forecast Dashboard
  // src/app/admin/opportunities/forecast/page.tsx
  - Weighted revenue forecast
  - Forecast by stage (chart)
  - Win/loss rate
  - Conversion funnel

□ Opportunity Form Modal
  // src/components/opportunities/opportunity-form.tsx
  - All fields with validation
  - Customer/Contact dropdowns
  - Line items table (add/remove products)
  - Probability slider
```

**Hooks:**
```typescript
□ useOpportunities hook
□ useOpportunity hook
□ useForecast hook
□ useOpportunityStages hook
```

#### Day 8-10: Integration & Testing

```typescript
□ Integration with Contact/Customer
  - Show opportunities on Contact detail
  - Show opportunities on Customer detail

□ Email tracking
  - Log emails sent to contact
  - Track email opens (optional pixel tracking)

□ Activity logging
  - Log stage changes
  - Log won/lost reasons
  - Log amount changes

□ Unit tests
  - OpportunityService tests
  - ForecastService tests
  - Component tests

□ E2E tests
  - Create opportunity flow
  - Move through pipeline
  - Win/loss flow
```

---

### TUẦN 7-8: TESTING & HARDENING (10 ngày)

#### Day 1-5: Comprehensive Testing

```bash
□ Increase test coverage to 70%+
  - Add missing unit tests
  - Integration tests for all services
  - E2E tests for all critical flows

□ Performance testing
  - Load test with JMeter (50K CCU simulation)
  - Database query performance analysis
  - Redis cache hit rate analysis
  - Memory leak detection

□ Security penetration testing
  - OWASP Top 10 vulnerability scan
  - SQL injection testing
  - XSS testing
  - CSRF testing
  - Authentication bypass attempts

□ Browser compatibility testing
  - Chrome, Firefox, Safari, Edge
  - Mobile responsive testing
  - Accessibility (WCAG 2.1 AA)
```

#### Day 6-10: Production Preparation

```bash
□ Docker containerization
  - Create Dockerfile for backend
  - Create Dockerfile for frontend
  - docker-compose.yml for full stack
  - Multi-stage builds for optimization

□ CI/CD pipeline
  - GitHub Actions workflow
  - Automated tests on PR
  - Build and push Docker images
  - Deploy to staging on merge

□ Monitoring setup
  - Prometheus metrics
  - Grafana dashboards
  - Application logs (ELK stack)
  - Error tracking (Sentry)
  - Uptime monitoring

□ Backup/restore procedures
  - Database backup scripts
  - Redis backup configuration
  - MinIO backup strategy
  - Restore testing

□ Documentation update
  - Deployment guide
  - Operations manual
  - API documentation (Swagger)
  - User guide
```

---

### TUẦN 9-10: QUIZ SYSTEM (12 ngày)

#### Day 1-4: Backend (8 Question Types)

**Entities:**
```java
□ Quiz entity
  - courseId, title, description
  - timeLimit (minutes)
  - passingScore (percentage)
  - shuffleQuestions, shuffleAnswers
  - allowReview, showCorrectAnswers

□ Question entity (polymorphic)
  - quizId, questionText, type
  - points, explanation
  - order

□ QuestionType enum
  - MULTIPLE_CHOICE
  - TRUE_FALSE
  - SHORT_ANSWER
  - ESSAY
  - MATCHING
  - FILL_IN_BLANK
  - ORDERING
  - FILE_UPLOAD

□ QuizAttempt entity
  - userId, quizId
  - startedAt, submittedAt
  - score, passed
  - answers (JSONB)

□ ManualGradingQueue entity
  - attemptId, questionId
  - submittedAnswer
  - gradedBy, gradedAt
  - pointsAwarded, feedback
```

**Services:**
```java
□ QuizService
  - CRUD operations
  - startAttempt(userId, quizId) -> QuizAttempt
  - submitAttempt(attemptId, answers)
  - gradeAttempt(attemptId) -> auto-grade MCQ, TF, etc.

□ GradingService
  - getGradingQueue(instructorId) -> manual grading needed
  - gradeQuestion(attemptId, questionId, points, feedback)
  - calculateFinalScore(attemptId)
```

#### Day 5-9: Frontend

**Components:**
```typescript
□ Quiz Editor
  // src/app/admin/courses/[courseId]/quizzes/[quizId]/edit/page.tsx
  - Rich question builder for each type
  - Drag-and-drop question ordering
  - Preview mode

□ Quiz Taking Page
  // src/app/admin/courses/[courseId]/quizzes/[quizId]/take/page.tsx
  - Timer countdown
  - Question navigation sidebar
  - Auto-save answers
  - Submit confirmation

□ Results Page
  // src/app/admin/courses/[courseId]/quizzes/[quizId]/results/[attemptId]/page.tsx
  - Score display
  - Correct/incorrect breakdown
  - Review answers (if allowed)
  - Certificate download (if passed)

□ Grading Queue
  // src/app/admin/grading/page.tsx
  - List of essay/file upload questions
  - Side-by-side grading interface
  - Rubric support
  - Bulk grading actions
```

#### Day 10-12: Integration & Testing

```typescript
□ LMS integration
  - Quizzes show in course modules
  - Progress tracking
  - Grade synchronization

□ Certificate generation on pass
  - Auto-generate certificate
  - Store in user's achievements
  - Email certificate PDF

□ Leaderboards
  - Quiz-specific leaderboards
  - Course-level leaderboards
  - Organization-level leaderboards

□ Tests
  - QuizService tests
  - GradingService tests
  - Component tests
  - E2E quiz taking flow
```

---

## 📊 PROGRESS TRACKING

### Daily Checklist Template
```markdown
## [Date] - [Day X of Phase Y]

### Morning Stand-up (9:00 AM)
- [ ] Review yesterday's work
- [ ] Plan today's tasks
- [ ] Check for blockers

### Tasks
- [ ] Task 1
- [ ] Task 2
- [ ] Task 3

### Evening Review (6:00 PM)
- [ ] Commit and push code
- [ ] Update documentation
- [ ] Update this checklist

### Blockers
- None / [Describe blocker]

### Notes
[Any important notes or decisions]
```

### Weekly Progress Report Template
```markdown
## Week [X] Progress Report

### Completed
- [List completed features]

### In Progress
- [List ongoing work]

### Blockers
- [List any blockers]

### Next Week Plan
- [List next week's priorities]

### Metrics
- Code coverage: X%
- Tests written: X
- Bugs fixed: X
- Features completed: X
```

---

## 🎯 SUCCESS CRITERIA

### Phase 1 Completion Checklist
- [ ] Testing coverage: 60%+ backend, 50%+ frontend
- [ ] All E2E critical flows passing
- [ ] JWT secret in environment variables
- [ ] Security audit completed
- [ ] Error boundaries implemented
- [ ] Global error handling working

### Phase 2 Completion Checklist
- [ ] Task detail page 100% complete
- [ ] Comments system working
- [ ] Checklist with progress tracking
- [ ] Activity timeline showing all events
- [ ] Bulk operations functional
- [ ] Push notifications working
- [ ] Quiet hours implemented
- [ ] Digest mode sending emails

### Phase 3 Completion Checklist
- [ ] Opportunity management functional
- [ ] Forecast dashboard accurate
- [ ] Win/loss reporting complete
- [ ] Production deployment successful
- [ ] Monitoring dashboards configured
- [ ] Quiz system with 8 question types
- [ ] Manual grading queue working

---

## 📞 DAILY COMMUNICATION

### Status Update Format
```
📊 Daily Status Update - [Date]

✅ Completed Today:
- [Feature/task completed]

🚧 In Progress:
- [Current work]

📅 Tomorrow's Plan:
- [Planned tasks]

⚠️ Blockers:
- [Any blockers or concerns]

📈 Metrics:
- Tests written: X
- Coverage: X%
- Commits: X
```

---

## 🔄 ITERATION CYCLE

```
Week N:
  Monday:    Plan week, assign tasks
  Tue-Thu:   Development
  Friday:    Code review, testing, deploy to staging

Week N+1:
  Monday:    Review staging, plan next sprint
  Repeat
```

---

## 📚 RESOURCES

### Documentation References
- [PLAN_WEEK1_TASK_MODULE.md](PLAN_WEEK1_TASK_MODULE.md)
- [PLAN_WEEK3_NOTIFICATION_MODULE.md](PLAN_WEEK3_NOTIFICATION_MODULE.md)
- [DAY3_NOTIFICATION_MODULE_SUMMARY.md](DAY3_NOTIFICATION_MODULE_SUMMARY.md)
- [ARCHITECTURE_DIAGRAM.md](ARCHITECTURE_DIAGRAM.md)

### Technical References
- Spring Boot 3.5: https://docs.spring.io/spring-boot/docs/3.5.x/reference/htmlsingle/
- React 19: https://react.dev/
- Next.js 15: https://nextjs.org/docs
- PostgreSQL 16: https://www.postgresql.org/docs/16/
- Redis 7: https://redis.io/docs/

---

**Created:** 2025-01-24
**Last Updated:** 2025-01-24
**Version:** 1.0
**Status:** Active Development Plan
