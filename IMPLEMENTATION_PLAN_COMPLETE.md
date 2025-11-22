# 📋 IMPLEMENTATION PLAN: Task, Notification & New Modules

**Date**: November 22, 2025
**Status**: 🟢 In Progress
**Timeline**: 8-12 weeks total

---

## 🎯 PHASE 1: COMPLETE TASK MANAGEMENT (Week 9)

### ✅ Already Completed (80%)
- [x] Backend: Task entity, service, controller
- [x] Frontend: Task page with Kanban board
- [x] Drag-and-drop functionality
- [x] Statistics cards
- [x] Filters (priority, sort)
- [x] Permission guards
- [x] API client & React Query hooks (17 hooks)

### 🔧 To Be Completed (20%)

#### 1.1 Task Edit Modal ✅ DONE
**File**: `src/components/tasks/task-edit-modal.tsx`

**Features**:
- [x] Full form with react-hook-form + zod validation
- [x] 4 sections: Basic Info, Status/Priority, Assignment/Dates, Related & Tags
- [x] Date pickers for start/due dates
- [x] Tag management (add/remove)
- [x] Customer/User selection dropdowns
- [x] Estimated hours input
- [x] Neobrutalism design with colored sections

**Integration**: Update `page.tsx` to use this modal in `handleEdit()` callback

---

#### 1.2 Task Detail Page
**File**: `src/app/admin/tasks/[taskId]/page.tsx`

**Layout**:
```
┌────────────────────────────────────────────────────────────┐
│ Header: Task Title + Status Badge + Actions               │
├────────────────────────────────────────────────────────────┤
│ ┌──────────────────────┬────────────────────────────────┐ │
│ │ Left Column (60%)    │ Right Sidebar (40%)            │ │
│ │                      │                                 │ │
│ │ 1. Description Card  │ 1. Details Card                │ │
│ │    - Full description│    - Priority, Status          │ │
│ │    - Created by/at   │    - Assigned to               │ │
│ │                      │    - Start/Due dates           │ │
│ │ 2. Checklist Card    │    - Est/Actual hours          │ │
│ │    - Items list      │    - Category                  │ │
│ │    - Add new item    │    - Tags                      │ │
│ │    - Progress bar    │                                 │ │
│ │                      │ 2. Related Entities Card       │ │
│ │ 3. Comments Card     │    - Customer link             │ │
│ │    - Comment list    │    - Contact link              │ │
│ │    - Add comment     │    - Activity link             │ │
│ │    - Attachments     │                                 │ │
│ │                      │ 3. Activity Timeline           │ │
│ │                      │    - Status changes            │ │
│ │                      │    - Assignments               │ │
│ │                      │    - Edits                     │ │
│ └──────────────────────┴────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

**Code Structure**:
```typescript
"use client"

import { useParams } from "next/navigation"
import { useTask, useTaskComments, useUpdateTask, useAddTaskComment } from "@/hooks/use-tasks"

export default function TaskDetailPage() {
  const params = useParams()
  const taskId = params.taskId as string

  const { data: task, isLoading } = useTask(taskId)
  const { data: comments } = useTaskComments(taskId)
  const updateMutation = useUpdateTask()
  const addCommentMutation = useAddTaskComment()

  // Components: TaskHeader, DescriptionCard, ChecklistCard, CommentsCard,
  // DetailsCard, RelatedEntitiesCard, ActivityTimelineCard
}
```

**Components to Create**:
- `src/components/tasks/task-header.tsx` - Title, badges, action buttons
- `src/components/tasks/task-checklist.tsx` - Checklist with progress
- `src/components/tasks/task-comments.tsx` - Comment list + form
- `src/components/tasks/task-activity-timeline.tsx` - Activity history

---

#### 1.3 Bulk Operations
**File**: Update `src/app/admin/tasks/page.tsx`

**Features**:
- [ ] Multi-select checkbox on task cards
- [ ] Bulk action toolbar (appears when items selected)
  - Assign to user (batch)
  - Change priority (batch)
  - Change status (batch)
  - Delete (batch)
- [ ] Confirmation dialog for bulk actions
- [ ] Optimistic updates for better UX

**UI Mockup**:
```
┌──────────────────────────────────────────────────────┐
│ [✓] 5 tasks selected                                 │
│ [Assign] [Priority] [Status] [Delete] [Clear]       │
└──────────────────────────────────────────────────────┘
```

**New API Endpoints Needed** (Backend):
```java
POST /api/tasks/bulk-assign
POST /api/tasks/bulk-priority
POST /api/tasks/bulk-status
DELETE /api/tasks/bulk-delete
```

---

#### 1.4 Advanced Filters
**File**: Update `src/app/admin/tasks/page.tsx`

**Add These Filters**:
- [ ] Assignee dropdown (multi-select)
- [ ] Date range picker (due date from/to)
- [ ] Customer filter
- [ ] Tag filter (multi-select)
- [ ] Overdue toggle
- [ ] Status multi-select (currently single)

**Component**: Use existing `AdvancedSearchDialog` pattern from other pages

---

### Summary: Task Module Completion

| Feature | Status | Files |
|---------|--------|-------|
| Edit Modal | ✅ Done | `task-edit-modal.tsx` |
| Detail Page | 📝 To Do | `[taskId]/page.tsx` + 4 components |
| Bulk Operations | 📝 To Do | Update `page.tsx` + backend |
| Advanced Filters | 📝 To Do | Update `page.tsx` |

**Estimated Time**: 3-4 days

---

## 🔔 PHASE 2: COMPLETE NOTIFICATION SYSTEM (Week 9-10)

### ✅ Already Completed (60%)
- [x] Backend: Notification entity, service, controller
- [x] Frontend: NotificationBell component
- [x] NotificationDropdown component
- [x] WebSocket integration (STOMP/SockJS)
- [x] Smart polling with exponential backoff
- [x] Batch mark-as-read
- [x] API client & hooks (11 hooks)

### 🔧 To Be Completed (40%)

#### 2.1 Full Notification Page
**File**: `src/app/admin/notifications/page.tsx`

**Layout**:
```
┌────────────────────────────────────────────────────────┐
│ Header: "Notifications" + Stats + Actions             │
├────────────────────────────────────────────────────────┤
│ ┌──────────────────────┬────────────────────────────┐ │
│ │ Filters Panel (25%)  │ Notification List (75%)    │ │
│ │                      │                             │ │
│ │ - All (123)          │ [Card] System notification  │ │
│ │ - Unread (45)        │ [Card] Task assigned        │ │
│ │ - Read (78)          │ [Card] Customer created     │ │
│ │ - Archived (12)      │ [Card] Comment added        │ │
│ │                      │ ...                         │ │
│ │ By Type:             │ [Pagination]                │ │
│ │ - System (23)        │                             │ │
│ │ - Task (56)          │                             │ │
│ │ - Customer (34)      │                             │ │
│ │ - Course (10)        │                             │ │
│ │                      │                             │ │
│ │ By Priority:         │                             │ │
│ │ - Urgent (5)         │                             │ │
│ │ - High (12)          │                             │ │
│ │ - Normal (106)       │                             │ │
│ └──────────────────────┴────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

**Features**:
- [ ] Paginated notification list (20 per page)
- [ ] Filter by status (all/unread/read/archived)
- [ ] Filter by type (19 notification types)
- [ ] Filter by priority (LOW/NORMAL/HIGH/URGENT)
- [ ] Search by keyword
- [ ] Bulk select & mark as read
- [ ] Archive notifications
- [ ] Click notification → navigate to actionUrl
- [ ] Real-time updates via WebSocket

**Components**:
- `src/components/notifications/notification-list.tsx`
- `src/components/notifications/notification-card.tsx`
- `src/components/notifications/notification-filters.tsx`

---

#### 2.2 Notification Preferences Page
**File**: `src/app/admin/notifications/preferences/page.tsx`

**Settings**:
```
┌────────────────────────────────────────────────────────┐
│ Email Notifications                                    │
├────────────────────────────────────────────────────────┤
│ [✓] Enable email notifications                        │
│ [✓] Task assigned to me                               │
│ [✓] Task due soon (24 hours before)                   │
│ [ ] Task completed                                     │
│ [✓] New customer created                              │
│ [ ] Comment on my task                                 │
│ [✓] Course enrollment                                  │
│                                                        │
│ Email frequency: [Instant ▼]                          │
│ Quiet hours: 22:00 - 08:00                            │
└────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────┐
│ Push Notifications (Browser)                           │
├────────────────────────────────────────────────────────┤
│ [✓] Enable push notifications                         │
│ [✓] Urgent priority only                              │
│ [ ] High priority and above                            │
│ [✓] All notifications                                  │
│                                                        │
│ [Request Permission] button                            │
└────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────┐
│ In-App Notifications                                   │
├────────────────────────────────────────────────────────┤
│ Toast duration: [5 seconds ▼]                         │
│ [✓] Show toast for urgent priority                    │
│ [✓] Play sound for urgent notifications               │
│ [ ] Desktop notifications (macOS/Windows)             │
└────────────────────────────────────────────────────────┘
```

**Backend**: Add `NotificationPreference` entity
```java
@Entity
public class NotificationPreference {
    @Id
    private UUID id;
    private UUID userId;

    // Email settings
    private boolean emailEnabled;
    private Set<NotificationType> emailTypes;
    private EmailFrequency frequency; // INSTANT, HOURLY, DAILY, WEEKLY
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;

    // Push settings
    private boolean pushEnabled;
    private NotificationPriority minPushPriority;

    // In-app settings
    private Integer toastDuration;
    private boolean soundEnabled;
    private boolean desktopEnabled;
}
```

---

#### 2.3 Email Integration
**Files**: Backend configuration

**Setup Spring Mail** (application.yml):
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**Email Templates** (Thymeleaf):
- `src/main/resources/templates/email/task-assigned.html`
- `src/main/resources/templates/email/task-due-soon.html`
- `src/main/resources/templates/email/notification-digest.html`

**Service**:
```java
@Service
public class NotificationEmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendTaskAssignedEmail(Task task, User assignee) {
        // Render template
        Context context = new Context();
        context.setVariable("task", task);
        context.setVariable("assignee", assignee);
        String html = templateEngine.process("email/task-assigned", context);

        // Send email
        MimeMessage message = mailSender.createMimeMessage();
        // ... configure message
        mailSender.send(message);
    }
}
```

---

#### 2.4 Push Notification (Browser API)
**File**: `src/lib/push-notifications.ts`

**Implementation**:
```typescript
export class PushNotificationManager {
  static async requestPermission(): Promise<boolean> {
    if (!('Notification' in window)) {
      console.error('This browser does not support notifications')
      return false
    }

    const permission = await Notification.requestPermission()
    return permission === 'granted'
  }

  static async subscribe(): Promise<PushSubscription | null> {
    const registration = await navigator.serviceWorker.ready
    const subscription = await registration.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY)
    })

    // Send subscription to backend
    await apiClient.post('/notifications/push/subscribe', {
      subscription: JSON.stringify(subscription)
    })

    return subscription
  }

  static showNotification(title: string, options: NotificationOptions) {
    if (Notification.permission === 'granted') {
      new Notification(title, options)
    }
  }
}
```

**Backend**: Add Web Push dependencies
```xml
<dependency>
    <groupId>nl.martijndwars</groupId>
    <artifactId>web-push</artifactId>
    <version>5.1.1</version>
</dependency>
```

---

### Summary: Notification Module Completion

| Feature | Status | Estimated Time |
|---------|--------|----------------|
| Full Notification Page | 📝 To Do | 1 day |
| Preferences Page | 📝 To Do | 1 day |
| Email Integration | 📝 To Do | 1 day |
| Push Notifications | 📝 To Do | 1 day |

**Total Estimated Time**: 4 days

---

## 💼 PHASE 3: OPPORTUNITY MANAGEMENT (Week 10-12)

### Data Model Design

#### 3.1 Opportunity Entity
```java
@Entity
@Table(name = "opportunities")
public class Opportunity extends TenantAwareAggregateRoot<OpportunityStatus> {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code; // OPP-2024-001

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStage stage; // LEAD, QUALIFIED, PROPOSAL, NEGOTIATION, CLOSED_WON, CLOSED_LOST

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStatus status; // ACTIVE, ON_HOLD, CANCELLED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_contact_id")
    private Contact primaryContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // Sales rep responsible

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount; // Deal value

    @Column(length = 3)
    private String currency; // USD, EUR, VND

    @Column(precision = 5, scale = 2)
    private BigDecimal probability; // 0-100% chance of closing

    @Column
    private LocalDate expectedCloseDate;

    @Column
    private LocalDate actualCloseDate;

    @Enumerated(EnumType.STRING)
    @Column
    private LeadSource source; // WEBSITE, REFERRAL, COLD_CALL, EVENT, PARTNER

    @Enumerated(EnumType.STRING)
    @Column
    private CompetitorType competitor; // NONE, COMPETITOR_A, COMPETITOR_B

    @Column(length = 500)
    private String competitorNotes;

    @Column(length = 1000)
    private String nextSteps;

    @Column
    private LocalDate lastContactDate;

    @Column
    private LocalDate nextFollowUpDate;

    @OneToMany(mappedBy = "opportunity", cascade = CascadeType.ALL)
    private Set<OpportunityLineItem> lineItems = new LinkedHashSet<>();

    @OneToMany(mappedBy = "opportunity", cascade = CascadeType.ALL)
    private Set<OpportunityStageHistory> stageHistory = new LinkedHashSet<>();

    @OneToMany(mappedBy = "opportunity")
    private Set<Task> tasks = new LinkedHashSet<>();

    @OneToMany(mappedBy = "opportunity")
    private Set<Activity> activities = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "opportunity_tags")
    private Set<String> tags = new HashSet<>();

    // Business methods
    public void advanceToStage(OpportunityStage newStage, String notes, User changedBy) {
        validateStageTransition(newStage);
        OpportunityStage oldStage = this.stage;
        this.stage = newStage;
        this.updatedBy = changedBy.getId();

        // Record history
        stageHistory.add(new OpportunityStageHistory(
            this, oldStage, newStage, notes, changedBy
        ));

        // Register domain event
        registerEvent(new OpportunityStageChangedEvent(
            this.id, oldStage, newStage, changedBy.getId()
        ));
    }

    public void win(BigDecimal finalAmount, LocalDate closeDate, User closedBy) {
        this.stage = OpportunityStage.CLOSED_WON;
        this.status = OpportunityStatus.CLOSED;
        this.amount = finalAmount;
        this.actualCloseDate = closeDate;
        this.probability = BigDecimal.valueOf(100);

        registerEvent(new OpportunityWonEvent(this.id, finalAmount, closedBy.getId()));
    }

    public void lose(String reason, User closedBy) {
        this.stage = OpportunityStage.CLOSED_LOST;
        this.status = OpportunityStatus.CLOSED;
        this.actualCloseDate = LocalDate.now();
        this.probability = BigDecimal.ZERO;

        registerEvent(new OpportunityLostEvent(this.id, reason, closedBy.getId()));
    }

    public BigDecimal getWeightedValue() {
        return amount.multiply(probability).divide(BigDecimal.valueOf(100));
    }
}

// Supporting enums
public enum OpportunityStage {
    LEAD,           // Initial contact
    QUALIFIED,      // Vetted as viable
    PROPOSAL,       // Proposal sent
    NEGOTIATION,    // Terms being discussed
    CLOSED_WON,     // Deal won
    CLOSED_LOST     // Deal lost
}

public enum OpportunityStatus {
    ACTIVE,
    ON_HOLD,
    CLOSED,
    CANCELLED
}

public enum LeadSource {
    WEBSITE,
    REFERRAL,
    COLD_CALL,
    EMAIL_CAMPAIGN,
    SOCIAL_MEDIA,
    EVENT,
    PARTNER,
    OTHER
}

public enum CompetitorType {
    NONE,
    COMPETITOR_A,
    COMPETITOR_B,
    COMPETITOR_C,
    OTHER
}
```

---

#### 3.2 OpportunityLineItem Entity
```java
@Entity
@Table(name = "opportunity_line_items")
public class OpportunityLineItem extends AuditableEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; // If you have product catalog

    @Column(nullable = false)
    private String productName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal taxRate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column
    private Integer displayOrder;

    // Calculated fields
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getDiscountedAmount() {
        BigDecimal subtotal = getSubtotal();
        if (discountPercent != null) {
            return subtotal.subtract(subtotal.multiply(discountPercent).divide(BigDecimal.valueOf(100)));
        } else if (discountAmount != null) {
            return subtotal.subtract(discountAmount);
        }
        return subtotal;
    }

    public void calculateTotalPrice() {
        BigDecimal discounted = getDiscountedAmount();
        if (taxRate != null) {
            this.totalPrice = discounted.add(discounted.multiply(taxRate).divide(BigDecimal.valueOf(100)));
        } else {
            this.totalPrice = discounted;
        }
    }
}
```

---

#### 3.3 OpportunityStageHistory Entity
```java
@Entity
@Table(name = "opportunity_stage_history")
public class OpportunityStageHistory {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpportunityStage toStage;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private Instant changedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Column
    private Integer daysInStage; // Calculated when moving to next stage
}
```

---

### Workflow Design: Sales Pipeline

#### Stage Transitions
```
LEAD → QUALIFIED → PROPOSAL → NEGOTIATION → CLOSED_WON
                                             ↘ CLOSED_LOST
```

**Validation Rules**:
- Cannot skip stages (must go sequentially)
- Can move backwards (e.g., NEGOTIATION → PROPOSAL if customer requests changes)
- Once CLOSED_WON or CLOSED_LOST, cannot change stage
- Probability auto-updates based on stage:
  - LEAD: 10%
  - QUALIFIED: 25%
  - PROPOSAL: 50%
  - NEGOTIATION: 75%
  - CLOSED_WON: 100%
  - CLOSED_LOST: 0%

#### Automated Actions
```java
@Service
public class OpportunityWorkflowService {
    public void onStageChanged(OpportunityStageChangedEvent event) {
        Opportunity opp = repository.findById(event.getOpportunityId());

        switch (opp.getStage()) {
            case QUALIFIED -> {
                // Create task: "Prepare proposal"
                taskService.createTask(CreateTaskRequest.builder()
                    .title("Prepare proposal for " + opp.getName())
                    .priority(TaskPriority.HIGH)
                    .assignedToId(opp.getOwner().getId())
                    .dueDate(LocalDate.now().plusDays(3))
                    .build());

                // Send notification to owner
                notificationService.send(
                    opp.getOwner().getId(),
                    "Opportunity qualified: " + opp.getName(),
                    "Prepare a proposal within 3 days"
                );
            }
            case PROPOSAL -> {
                // Create task: "Follow up on proposal"
                // Schedule reminder in 1 week
            }
            case NEGOTIATION -> {
                // Notify sales manager
                // Create task: "Finalize contract terms"
            }
            case CLOSED_WON -> {
                // Create onboarding tasks
                // Update customer status to ACTIVE
                // Send celebration email to team
                // Update revenue forecast
            }
            case CLOSED_LOST -> {
                // Request feedback
                // Create follow-up task for 6 months later
            }
        }
    }
}
```

---

### Frontend Design: Opportunity Module

#### 3.4 Opportunity List Page
**File**: `src/app/admin/opportunities/page.tsx`

**View Modes**:
1. **Pipeline View** (Default) - Kanban board by stage
2. **List View** - Table with all opportunities
3. **Forecast View** - Charts and revenue projections

**Pipeline View Layout**:
```
┌───────────────────────────────────────────────────────────────────┐
│ Revenue Forecast: $2.4M (weighted) | Win Rate: 32% | Avg: 45 days │
├───────────────────────────────────────────────────────────────────┤
│ ┌──────┬──────────┬──────────┬─────────────┬────────────┬──────┐│
│ │ LEAD │ QUALIFIED│ PROPOSAL │ NEGOTIATION │ CLOSED_WON │ LOST ││
│ │ 23   │   15     │   8      │     5       │     12     │  7   ││
│ │ $450K│  $680K   │  $520K   │   $750K     │   $1.2M    │$300K ││
│ ├──────┼──────────┼──────────┼─────────────┼────────────┼──────┤│
│ │[Card]│  [Card]  │  [Card]  │   [Card]    │   [Card]   │[Card]││
│ │[Card]│  [Card]  │  [Card]  │   [Card]    │   [Card]   │[Card]││
│ │[Card]│  [Card]  │  [Card]  │             │            │      ││
│ │...   │  ...     │  ...     │             │            │      ││
│ └──────┴──────────┴──────────┴─────────────┴────────────┴──────┘│
└───────────────────────────────────────────────────────────────────┘
```

**Opportunity Card** (in Pipeline):
```
┌─────────────────────────────────────┐
│ [ACME Corp - Website Redesign]     │
│ $45,000 • 50% • John Doe           │
│ Due: Dec 15, 2024                  │
│ Last contact: 3 days ago           │
│ [WEBSITE] [HIGH_PRIORITY]          │
└─────────────────────────────────────┘
```

---

#### 3.5 Opportunity Detail Page
**File**: `src/app/admin/opportunities/[opportunityId]/page.tsx`

**Sections**:
```
┌────────────────────────────────────────────────────────────┐
│ Header: [Name] [Stage Badge] [Actions: Edit | Win | Lose] │
├──────────────────────────┬─────────────────────────────────┤
│ Left Column (65%)        │ Right Sidebar (35%)             │
│                          │                                  │
│ 1. Overview Card         │ 1. Key Info Card                │
│    - Description         │    - Stage, Status, Probability │
│    - Amount, Currency    │    - Owner, Source              │
│    - Expected close date │    - Expected/Actual close date │
│    - Customer & Contact  │    - Created/Updated dates      │
│                          │                                  │
│ 2. Line Items Card       │ 2. Stage History Card           │
│    - Product table       │    - Timeline of stage changes  │
│    - Subtotal, Discount  │    - Days in each stage         │
│    - Tax, Total          │    - Notes per stage            │
│    - [Add Line Item]     │                                  │
│                          │ 3. Related Tasks Card           │
│ 3. Activities Card       │    - Active tasks (3)           │
│    - Activity timeline   │    - [Create Task]              │
│    - Calls, meetings     │                                  │
│    - Emails, notes       │ 4. Competitor Info Card         │
│    - [Log Activity]      │    - Competitor name            │
│                          │    - Competitive notes          │
│ 4. Next Steps Card       │                                  │
│    - Next steps text     │ 5. Files & Attachments          │
│    - Next follow-up date │    - Proposal.pdf               │
│    - [Update]            │    - Contract_Draft.docx        │
│                          │    - [Upload]                   │
└──────────────────────────┴─────────────────────────────────┘
```

---

#### 3.6 Opportunity Forecast Dashboard
**File**: `src/app/admin/opportunities/forecast/page.tsx`

**Charts & Metrics**:
1. **Revenue by Stage** (Horizontal Bar Chart)
   - Shows total amount in each stage
   - Weighted vs. Unweighted

2. **Win Rate Trend** (Line Chart)
   - Monthly win rate over time
   - Average deal size

3. **Sales Funnel** (Funnel Chart)
   - Conversion rates between stages
   - Identifies bottlenecks

4. **Top Opportunities** (Table)
   - 10 highest value active opportunities
   - Sorted by weighted value

5. **Aging Report** (Table)
   - Opportunities > 30 days in same stage
   - Needs attention

6. **Sales Rep Leaderboard** (Table)
   - Total value by owner
   - Win rate by owner

**Time Filters**: This Month | This Quarter | This Year | Custom Range

---

### Database Migration for Opportunity

**File**: `src/main/resources/db/migration/V116__create_opportunity_tables.sql`

```sql
-- Opportunity table
CREATE TABLE opportunities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    stage VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    primary_contact_id UUID REFERENCES contacts(id),
    owner_id UUID NOT NULL REFERENCES users(id),
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    probability DECIMAL(5, 2),
    expected_close_date DATE,
    actual_close_date DATE,
    source VARCHAR(50),
    competitor VARCHAR(50),
    competitor_notes VARCHAR(500),
    next_steps TEXT,
    last_contact_date DATE,
    next_follow_up_date DATE,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    tenant_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID NOT NULL REFERENCES users(id),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

-- Opportunity line items
CREATE TABLE opportunity_line_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    discount_percent DECIMAL(5, 2),
    discount_amount DECIMAL(19, 2),
    tax_rate DECIMAL(5, 2),
    total_price DECIMAL(19, 2) NOT NULL,
    display_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Opportunity stage history
CREATE TABLE opportunity_stage_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    from_stage VARCHAR(50) NOT NULL,
    to_stage VARCHAR(50) NOT NULL,
    notes TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by_id UUID NOT NULL REFERENCES users(id),
    days_in_stage INTEGER
);

-- Opportunity tags
CREATE TABLE opportunity_tags (
    opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (opportunity_id, tag)
);

-- Indexes for performance
CREATE INDEX idx_opportunity_customer ON opportunities(customer_id);
CREATE INDEX idx_opportunity_owner ON opportunities(owner_id);
CREATE INDEX idx_opportunity_stage ON opportunities(stage);
CREATE INDEX idx_opportunity_status ON opportunities(status);
CREATE INDEX idx_opportunity_expected_close ON opportunities(expected_close_date);
CREATE INDEX idx_opportunity_tenant_deleted ON opportunities(tenant_id, deleted);
CREATE INDEX idx_opportunity_stage_history_opp ON opportunity_stage_history(opportunity_id);
CREATE INDEX idx_opportunity_line_items_opp ON opportunity_line_items(opportunity_id);
```

---

### Summary: Opportunity Module

| Component | Estimated Time | Priority |
|-----------|---------------|----------|
| Backend Entities | 1 day | High |
| Controllers & Services | 1 day | High |
| Database Migration | 0.5 day | High |
| Workflow Service | 1 day | High |
| Pipeline Page (Kanban) | 2 days | High |
| Detail Page | 1.5 days | High |
| Forecast Dashboard | 1.5 days | Medium |
| Testing | 1 day | High |

**Total**: ~9-10 days (2 weeks)

---

## 📝 PHASE 4: QUIZ & ASSESSMENT SYSTEM (Week 12-14)

### Data Model Design

#### 4.1 Quiz Entity (Enhanced)
```java
@Entity
@Table(name = "quizzes")
public class Quiz extends TenantAwareAggregateRoot<QuizStatus> {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 1000)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizStatus status; // DRAFT, PUBLISHED, ARCHIVED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizType type; // PRACTICE, GRADED, CERTIFICATION, SURVEY

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course; // Optional: Quiz can be standalone

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson; // Or embedded in a lesson

    @Column(nullable = false)
    private Integer timeLimitMinutes; // 0 = no limit

    @Column(nullable = false)
    private Integer passingScore; // Percentage (e.g., 70)

    @Column(nullable = false)
    private Integer maxAttempts; // 0 = unlimited

    @Column(nullable = false)
    private Boolean shuffleQuestions;

    @Column(nullable = false)
    private Boolean shuffleAnswers;

    @Column(nullable = false)
    private Boolean showCorrectAnswers; // After submission

    @Column(nullable = false)
    private Boolean showScoreImmediately;

    @Column
    private LocalDateTime availableFrom;

    @Column
    private LocalDateTime availableUntil;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<QuizQuestion> questions = new ArrayList<>();

    @OneToMany(mappedBy = "quiz")
    private Set<QuizAttempt> attempts = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "quiz_tags")
    private Set<String> tags = new HashSet<>();

    // Calculated fields
    public Integer getTotalPoints() {
        return questions.stream()
            .mapToInt(QuizQuestion::getPoints)
            .sum();
    }

    public Double getAverageScore() {
        return attempts.stream()
            .filter(a -> a.getStatus() == AttemptStatus.COMPLETED)
            .mapToDouble(a -> a.getScorePercentage())
            .average()
            .orElse(0.0);
    }

    public Long getCompletionCount() {
        return attempts.stream()
            .filter(a -> a.getStatus() == AttemptStatus.COMPLETED)
            .count();
    }
}

public enum QuizStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}

public enum QuizType {
    PRACTICE,       // No impact on grade, unlimited attempts
    GRADED,         // Counts toward course grade
    CERTIFICATION,  // Issues certificate on pass
    SURVEY          // No right/wrong answers
}
```

---

#### 4.2 QuizQuestion Entity
```java
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion extends AuditableEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type; // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, ESSAY, MATCHING, FILL_BLANK

    @Column(nullable = false, length = 2000)
    private String questionText;

    @Column(length = 1000)
    private String explanation; // Shown after answer (if enabled)

    @Column(nullable = false)
    private Integer points; // Point value for this question

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean required; // Must be answered to submit

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<QuizAnswer> answers = new ArrayList<>();

    @Column(length = 500)
    private String imageUrl; // Optional image for question

    @Column(length = 500)
    private String codeSnippet; // For programming quizzes

    @Enumerated(EnumType.STRING)
    @Column
    private DifficultyLevel difficulty; // EASY, MEDIUM, HARD

    @ElementCollection
    @CollectionTable(name = "question_tags")
    private Set<String> tags = new HashSet<>();
}

public enum QuestionType {
    MULTIPLE_CHOICE,    // One correct answer from options
    MULTIPLE_SELECT,    // Multiple correct answers
    TRUE_FALSE,         // Boolean question
    SHORT_ANSWER,       // Text input (auto-graded by keyword)
    ESSAY,              // Long text (manual grading)
    MATCHING,           // Match items from two lists
    FILL_BLANK,         // Fill in the blank(s)
    ORDERING            // Put items in correct order
}

public enum DifficultyLevel {
    EASY,
    MEDIUM,
    HARD
}
```

---

#### 4.3 QuizAnswer Entity
```java
@Entity
@Table(name = "quiz_answers")
public class QuizAnswer {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(nullable = false, length = 1000)
    private String answerText;

    @Column(nullable = false)
    private Boolean isCorrect;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(length = 500)
    private String feedback; // Shown when this answer is selected

    // For matching questions
    @Column
    private Integer matchOrder; // What this should match to

    // For fill-in-the-blank
    @Column
    private Integer blankPosition; // Which blank (1, 2, 3...)
}
```

---

#### 4.4 QuizAttempt Entity
```java
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt extends AuditableEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private Integer attemptNumber; // 1, 2, 3, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status; // IN_PROGRESS, COMPLETED, ABANDONED, TIMED_OUT

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    @Column
    private Integer timeSpentSeconds;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
    private List<StudentAnswer> studentAnswers = new ArrayList<>();

    @Column
    private Integer totalPoints; // Points earned

    @Column
    private Integer maxPoints; // Total possible points

    @Column
    private Double scorePercentage; // (totalPoints / maxPoints) * 100

    @Column(nullable = false)
    private Boolean passed; // scorePercentage >= quiz.passingScore

    @Enumerated(EnumType.STRING)
    @Column
    private GradingStatus gradingStatus; // AUTO_GRADED, PENDING_REVIEW, MANUALLY_GRADED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by_id")
    private User gradedBy; // For manual grading

    @Column
    private Instant gradedAt;

    @Column(length = 2000)
    private String instructorFeedback;

    // Business methods
    public void submit() {
        this.status = AttemptStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.timeSpentSeconds = (int) Duration.between(startedAt, completedAt).getSeconds();

        // Auto-grade objective questions
        autoGrade();

        // Check if manual grading needed
        boolean hasEssayQuestions = studentAnswers.stream()
            .anyMatch(sa -> sa.getQuestion().getType() == QuestionType.ESSAY);

        this.gradingStatus = hasEssayQuestions
            ? GradingStatus.PENDING_REVIEW
            : GradingStatus.AUTO_GRADED;
    }

    private void autoGrade() {
        int earnedPoints = 0;

        for (StudentAnswer sa : studentAnswers) {
            if (sa.getQuestion().getType() == QuestionType.ESSAY) {
                continue; // Skip essays
            }

            if (isAnswerCorrect(sa)) {
                earnedPoints += sa.getQuestion().getPoints();
                sa.setPointsEarned(sa.getQuestion().getPoints());
            } else {
                sa.setPointsEarned(0);
            }
        }

        this.totalPoints = earnedPoints;
        this.maxPoints = quiz.getTotalPoints();
        this.scorePercentage = (totalPoints.doubleValue() / maxPoints) * 100;
        this.passed = scorePercentage >= quiz.getPassingScore();
    }

    private boolean isAnswerCorrect(StudentAnswer sa) {
        QuizQuestion q = sa.getQuestion();

        switch (q.getType()) {
            case MULTIPLE_CHOICE, TRUE_FALSE -> {
                return sa.getSelectedAnswers().stream()
                    .allMatch(QuizAnswer::getIsCorrect);
            }
            case MULTIPLE_SELECT -> {
                Set<UUID> correctIds = q.getAnswers().stream()
                    .filter(QuizAnswer::getIsCorrect)
                    .map(QuizAnswer::getId)
                    .collect(Collectors.toSet());

                Set<UUID> selectedIds = sa.getSelectedAnswers().stream()
                    .map(QuizAnswer::getId)
                    .collect(Collectors.toSet());

                return correctIds.equals(selectedIds);
            }
            case SHORT_ANSWER -> {
                // Simple keyword matching (can be enhanced with NLP)
                String studentText = sa.getTextAnswer().toLowerCase().trim();
                return q.getAnswers().stream()
                    .anyMatch(a -> a.getAnswerText().toLowerCase().contains(studentText));
            }
            case FILL_BLANK -> {
                // Match each blank to correct answer
                return sa.getFillBlankAnswers().entrySet().stream()
                    .allMatch(entry -> {
                        int blankNum = entry.getKey();
                        String answer = entry.getValue();
                        return q.getAnswers().stream()
                            .filter(a -> a.getBlankPosition() == blankNum)
                            .anyMatch(a -> a.getAnswerText().equalsIgnoreCase(answer.trim()));
                    });
            }
            default -> {
                return false; // ESSAY requires manual grading
            }
        }
    }
}

public enum AttemptStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
    TIMED_OUT
}

public enum GradingStatus {
    AUTO_GRADED,
    PENDING_REVIEW,
    MANUALLY_GRADED
}
```

---

#### 4.5 StudentAnswer Entity
```java
@Entity
@Table(name = "student_answers")
public class StudentAnswer {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    // For multiple choice / true-false
    @ManyToMany
    @JoinTable(
        name = "student_answer_selections",
        joinColumns = @JoinColumn(name = "student_answer_id"),
        inverseJoinColumns = @JoinColumn(name = "quiz_answer_id")
    )
    private Set<QuizAnswer> selectedAnswers = new LinkedHashSet<>();

    // For short answer / essay
    @Column(length = 5000)
    private String textAnswer;

    // For fill-in-the-blank (key = blank number, value = student's text)
    @ElementCollection
    @CollectionTable(name = "fill_blank_answers")
    @MapKeyColumn(name = "blank_number")
    @Column(name = "answer_text")
    private Map<Integer, String> fillBlankAnswers = new HashMap<>();

    // For matching (key = left item id, value = right item id)
    @ElementCollection
    @CollectionTable(name = "matching_answers")
    private Map<UUID, UUID> matchingAnswers = new HashMap<>();

    // For ordering
    @ElementCollection
    @CollectionTable(name = "ordering_answers")
    @Column(name = "display_order")
    private List<UUID> orderingAnswers = new ArrayList<>();

    @Column
    private Integer pointsEarned;

    @Column(nullable = false)
    private Boolean isCorrect;

    @Column
    private Instant answeredAt;

    @Column(length = 1000)
    private String manualFeedback; // From instructor for essay questions
}
```

---

#### 4.6 QuestionBank Entity (Optional - Advanced)
```java
@Entity
@Table(name = "question_banks")
public class QuestionBank {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course; // Can be shared across courses or course-specific

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "question_bank_id")
    private Set<QuizQuestion> questions = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "question_bank_tags")
    private Set<String> tags = new HashSet<>();

    @Column(nullable = false)
    private Boolean isPublic; // Can other instructors use this?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;
}
```

---

### Workflow Design: Quiz System

#### Quiz Creation Workflow
```
1. Instructor creates Quiz
   ├─ Set title, description, type, passing score
   ├─ Configure settings (time limit, attempts, shuffle)
   └─ Set availability dates

2. Add Questions
   ├─ Manual creation
   │  ├─ Select question type
   │  ├─ Enter question text
   │  ├─ Add answer options
   │  └─ Mark correct answer(s)
   │
   ├─ Import from Question Bank
   │  ├─ Browse question bank
   │  ├─ Filter by tags/difficulty
   │  └─ Select questions to add
   │
   └─ Random selection
      ├─ Configure: "Pick 10 random from 'JavaScript Basics'"
      └─ Questions randomized per student

3. Preview & Test
   ├─ Take quiz as student
   ├─ Verify scoring logic
   └─ Check time limits

4. Publish
   ├─ Status: DRAFT → PUBLISHED
   ├─ Students can now access
   └─ Send notification to enrolled students
```

---

#### Student Quiz-Taking Workflow
```
1. Start Attempt
   ├─ Check prerequisites (max attempts, availability)
   ├─ Create QuizAttempt record
   ├─ Start timer (if timed)
   └─ Shuffle questions/answers (if enabled)

2. Answer Questions
   ├─ Save answers as student progresses (auto-save every 30s)
   ├─ Mark for review
   ├─ Navigate between questions
   └─ Warning if time running out

3. Submit Attempt
   ├─ Validate all required questions answered
   ├─ Confirm submission
   ├─ Stop timer
   ├─ Auto-grade objective questions
   └─ Set status to PENDING_REVIEW if has essay questions

4. View Results
   ├─ IF showScoreImmediately = true
   │  └─ Display score, percentage, pass/fail
   │
   ├─ IF showCorrectAnswers = true
   │  └─ Show which answers were correct/incorrect
   │     └─ Display explanations
   │
   └─ IF quiz type = CERTIFICATION && passed
      └─ Issue certificate
```

---

#### Manual Grading Workflow (for Essay Questions)
```
1. Instructor views "Pending Review" queue
   ├─ Filter by quiz, student, date
   └─ Sort by submission date

2. Review Student Answer
   ├─ Read essay response
   ├─ Compare to rubric (if available)
   ├─ Assign points (0 to max for that question)
   └─ Add feedback

3. Finalize Grade
   ├─ All essay questions graded
   ├─ Recalculate total score
   ├─ Update attempt.gradingStatus = MANUALLY_GRADED
   ├─ Update attempt.passed based on final score
   └─ Send notification to student
```

---

### Frontend Design: Quiz System

#### 4.7 Quiz List Page (Instructor)
**File**: `src/app/admin/quizzes/page.tsx`

**Features**:
- Table view of all quizzes
- Columns: Title, Type, Questions, Avg Score, Completion Rate, Status
- Actions: Edit, Duplicate, View Results, Delete
- Filter: By course, by type, by status
- Create button: Opens quiz creation wizard

---

#### 4.8 Quiz Editor Page (Instructor)
**File**: `src/app/admin/quizzes/[quizId]/edit/page.tsx`

**3-Step Wizard**:
```
Step 1: Quiz Settings
├─ Title, Description, Instructions
├─ Type, Status
├─ Time limit, Passing score, Max attempts
├─ Shuffle questions/answers
├─ Show correct answers, Show score immediately
└─ Availability dates

Step 2: Questions
├─ Question list (draggable for reorder)
├─ Add question modal
│  ├─ Question type selector
│  ├─ Question text (rich text editor)
│  ├─ Points
│  ├─ Answer options (dynamic based on type)
│  ├─ Mark correct answer(s)
│  ├─ Explanation
│  └─ Difficulty, Tags
├─ Edit/Delete question
└─ Import from Question Bank

Step 3: Preview & Publish
├─ Preview as student
├─ Validation checks
└─ Publish button
```

---

#### 4.9 Quiz Taking Page (Student)
**File**: `src/app/courses/[courseId]/quizzes/[quizId]/take/page.tsx`

**Layout**:
```
┌──────────────────────────────────────────────────────┐
│ Quiz: JavaScript Fundamentals                        │
│ Time Remaining: 45:23 | Question 5 of 20             │
├──────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────┐  │
│ │ Question 5                              [Mark] │  │
│ │                                                 │  │
│ │ What is the output of console.log(typeof NaN)?│  │
│ │                                                 │  │
│ │ ○ "number"                                     │  │
│ │ ○ "NaN"                                        │  │
│ │ ○ "undefined"                                  │  │
│ │ ○ "object"                                     │  │
│ └────────────────────────────────────────────────┘  │
│                                                      │
│ [Previous] [Next] [Submit Quiz]                     │
│                                                      │
│ Question Navigator:                                 │
│ [1✓] [2✓] [3✓] [4✓] [5] [6] [7] ... [20]           │
│  ✓ = Answered | Empty = Unanswered | ⭐ = Marked   │
└──────────────────────────────────────────────────────┘
```

**Features**:
- Auto-save answers every 30 seconds
- Countdown timer (if timed)
- Question navigator
- Mark for review
- Submit with confirmation
- Warning before leaving page

---

#### 4.10 Quiz Results Page (Student)
**File**: `src/app/courses/[courseId]/quizzes/[quizId]/results/[attemptId]/page.tsx`

**Layout**:
```
┌──────────────────────────────────────────────────────┐
│ Quiz Results: JavaScript Fundamentals                │
├──────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────┐  │
│ │ PASSED ✓                                       │  │
│ │                                                 │  │
│ │ Your Score: 85/100 (85%)                       │  │
│ │ Passing Score: 70%                             │  │
│ │                                                 │  │
│ │ Time Taken: 38 minutes                         │  │
│ │ Attempt: 1 of 3                                │  │
│ └────────────────────────────────────────────────┘  │
│                                                      │
│ Performance Breakdown:                              │
│ ┌────────────────────────────────────────────────┐  │
│ │ Multiple Choice: 90% (18/20)                   │  │
│ │ True/False: 100% (5/5)                         │  │
│ │ Short Answer: 60% (3/5)                        │  │
│ └────────────────────────────────────────────────┘  │
│                                                      │
│ Question Review:                                    │
│ ┌────────────────────────────────────────────────┐  │
│ │ Question 1 ✓ Correct (5 pts)                   │  │
│ │ What is the output of console.log(typeof NaN)?│  │
│ │                                                 │  │
│ │ Your Answer: "number" ✓                        │  │
│ │ Explanation: NaN is a special numeric value... │  │
│ ├────────────────────────────────────────────────┤  │
│ │ Question 2 ✗ Incorrect (0/5 pts)               │  │
│ │ ...                                            │  │
│ └────────────────────────────────────────────────┘  │
│                                                      │
│ [Retake Quiz] [Back to Course]                     │
└──────────────────────────────────────────────────────┘
```

---

#### 4.11 Grading Queue Page (Instructor)
**File**: `src/app/admin/quizzes/grading-queue/page.tsx`

**Features**:
- List of attempts pending review
- Filter: By quiz, by student, by date
- Sort: By submission date, by student name
- Click to open grading modal
- Bulk actions: Assign to grader

**Grading Modal**:
```
┌──────────────────────────────────────────────────────┐
│ Grade Essay Question                                 │
├──────────────────────────────────────────────────────┤
│ Student: John Doe                                    │
│ Quiz: Final Exam                                     │
│ Question 15: Explain the concept of closures in JS  │
│                                                      │
│ Student Answer:                                      │
│ ┌────────────────────────────────────────────────┐  │
│ │ A closure is when a function retains access    │  │
│ │ to its lexical scope even after the outer      │  │
│ │ function has returned. This allows the inner   │  │
│ │ function to access variables from the outer... │  │
│ └────────────────────────────────────────────────┘  │
│                                                      │
│ Points Earned: [8] / 10                            │  │
│                                                      │
│ Feedback:                                            │
│ ┌────────────────────────────────────────────────┐  │
│ │ Good explanation of lexical scope. Consider    │  │
│ │ adding an example to strengthen your answer.   │  │
│ └────────────────────────────────────────────────┘  │
│                                                      │
│ [Previous Question] [Next Question] [Save & Close] │
└──────────────────────────────────────────────────────┘
```

---

### Database Migration for Quiz System

**File**: `src/main/resources/db/migration/V117__enhance_quiz_tables.sql`

```sql
-- Quizzes table (enhanced)
ALTER TABLE quizzes ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT 'PRACTICE';
ALTER TABLE quizzes ADD COLUMN time_limit_minutes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE quizzes ADD COLUMN passing_score INTEGER NOT NULL DEFAULT 70;
ALTER TABLE quizzes ADD COLUMN max_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE quizzes ADD COLUMN shuffle_questions BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE quizzes ADD COLUMN shuffle_answers BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE quizzes ADD COLUMN show_correct_answers BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE quizzes ADD COLUMN show_score_immediately BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE quizzes ADD COLUMN available_from TIMESTAMP;
ALTER TABLE quizzes ADD COLUMN available_until TIMESTAMP;
ALTER TABLE quizzes ADD COLUMN instructions TEXT;

-- Quiz questions table
ALTER TABLE quiz_questions ADD COLUMN type VARCHAR(50) NOT NULL DEFAULT 'MULTIPLE_CHOICE';
ALTER TABLE quiz_questions ADD COLUMN explanation TEXT;
ALTER TABLE quiz_questions ADD COLUMN points INTEGER NOT NULL DEFAULT 1;
ALTER TABLE quiz_questions ADD COLUMN display_order INTEGER NOT NULL DEFAULT 0;
ALTER TABLE quiz_questions ADD COLUMN required BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE quiz_questions ADD COLUMN image_url VARCHAR(500);
ALTER TABLE quiz_questions ADD COLUMN code_snippet TEXT;
ALTER TABLE quiz_questions ADD COLUMN difficulty VARCHAR(50);

-- Quiz answers table
CREATE TABLE quiz_answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    answer_text VARCHAR(1000) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    display_order INTEGER NOT NULL,
    feedback VARCHAR(500),
    match_order INTEGER,
    blank_position INTEGER
);

-- Quiz attempts table
ALTER TABLE quiz_attempts ADD COLUMN attempt_number INTEGER NOT NULL DEFAULT 1;
ALTER TABLE quiz_attempts ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS';
ALTER TABLE quiz_attempts ADD COLUMN started_at TIMESTAMP;
ALTER TABLE quiz_attempts ADD COLUMN completed_at TIMESTAMP;
ALTER TABLE quiz_attempts ADD COLUMN time_spent_seconds INTEGER;
ALTER TABLE quiz_attempts ADD COLUMN total_points INTEGER;
ALTER TABLE quiz_attempts ADD COLUMN max_points INTEGER;
ALTER TABLE quiz_attempts ADD COLUMN score_percentage DECIMAL(5, 2);
ALTER TABLE quiz_attempts ADD COLUMN passed BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE quiz_attempts ADD COLUMN grading_status VARCHAR(50);
ALTER TABLE quiz_attempts ADD COLUMN graded_by_id UUID REFERENCES users(id);
ALTER TABLE quiz_attempts ADD COLUMN graded_at TIMESTAMP;
ALTER TABLE quiz_attempts ADD COLUMN instructor_feedback TEXT;

-- Student answers table
CREATE TABLE student_answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    attempt_id UUID NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES quiz_questions(id),
    text_answer TEXT,
    points_earned INTEGER,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    answered_at TIMESTAMP,
    manual_feedback TEXT
);

-- Student answer selections (many-to-many)
CREATE TABLE student_answer_selections (
    student_answer_id UUID NOT NULL REFERENCES student_answers(id) ON DELETE CASCADE,
    quiz_answer_id UUID NOT NULL REFERENCES quiz_answers(id),
    PRIMARY KEY (student_answer_id, quiz_answer_id)
);

-- Fill blank answers
CREATE TABLE fill_blank_answers (
    student_answer_id UUID NOT NULL REFERENCES student_answers(id) ON DELETE CASCADE,
    blank_number INTEGER NOT NULL,
    answer_text VARCHAR(500),
    PRIMARY KEY (student_answer_id, blank_number)
);

-- Matching answers
CREATE TABLE matching_answers (
    student_answer_id UUID NOT NULL REFERENCES student_answers(id) ON DELETE CASCADE,
    left_item_id UUID NOT NULL,
    right_item_id UUID NOT NULL,
    PRIMARY KEY (student_answer_id, left_item_id)
);

-- Ordering answers
CREATE TABLE ordering_answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    student_answer_id UUID NOT NULL REFERENCES student_answers(id) ON DELETE CASCADE,
    answer_id UUID NOT NULL,
    display_order INTEGER NOT NULL
);

-- Question bank
CREATE TABLE question_banks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    course_id UUID REFERENCES courses(id),
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_by_id UUID NOT NULL REFERENCES users(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_quiz_type ON quizzes(type);
CREATE INDEX idx_quiz_status ON quizzes(status);
CREATE INDEX idx_quiz_course ON quizzes(course_id);
CREATE INDEX idx_question_quiz ON quiz_questions(quiz_id);
CREATE INDEX idx_answer_question ON quiz_answers(question_id);
CREATE INDEX idx_attempt_quiz ON quiz_attempts(quiz_id);
CREATE INDEX idx_attempt_student ON quiz_attempts(student_id);
CREATE INDEX idx_attempt_status ON quiz_attempts(status);
CREATE INDEX idx_attempt_grading_status ON quiz_attempts(grading_status);
CREATE INDEX idx_student_answer_attempt ON student_answers(attempt_id);
CREATE INDEX idx_question_bank_course ON question_banks(course_id);
```

---

### Summary: Quiz System

| Component | Estimated Time | Priority |
|-----------|---------------|----------|
| Backend Entities | 2 days | High |
| Auto-Grading Logic | 1 day | High |
| Manual Grading Service | 0.5 day | High |
| Database Migration | 0.5 day | High |
| Quiz Editor (Instructor) | 2 days | High |
| Quiz Taking Page (Student) | 1.5 days | High |
| Results Page | 1 day | High |
| Grading Queue Page | 1 day | Medium |
| Question Bank (Advanced) | 1.5 days | Low |
| Testing | 1 day | High |

**Total**: ~11-12 days (2.5 weeks)

---

## 💬 PHASE 5: DISCUSSION FORUM SYSTEM (Week 14-16)

### Data Model Design

#### 5.1 Forum Entity
```java
@Entity
@Table(name = "forums")
public class Forum extends TenantAwareAggregateRoot<ForumStatus> {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForumType type; // COURSE, GENERAL, ANNOUNCEMENT, Q_AND_A

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForumStatus status; // ACTIVE, ARCHIVED, LOCKED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course; // Optional: Forum can be global or course-specific

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(nullable = false)
    private Boolean allowReplies; // Can users reply to threads?

    @Column(nullable = false)
    private Boolean requireModeration; // Posts need approval?

    @Column(nullable = false)
    private Boolean allowAnonymous; // Allow anonymous posting?

    @OneToMany(mappedBy = "forum", cascade = CascadeType.ALL)
    @OrderBy("isPinned DESC, lastActivityAt DESC")
    private List<ForumThread> threads = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "forum_moderators")
    @Column(name = "user_id")
    private Set<UUID> moderatorIds = new HashSet<>();

    // Statistics
    @Column(nullable = false)
    private Long threadCount = 0L;

    @Column(nullable = false)
    private Long postCount = 0L;

    @Column
    private Instant lastActivityAt;
}

public enum ForumType {
    COURSE,         // Course-specific discussions
    GENERAL,        // General topic discussions
    ANNOUNCEMENT,   // Read-only announcements
    Q_AND_A         // Question & Answer format
}

public enum ForumStatus {
    ACTIVE,
    ARCHIVED,
    LOCKED
}
```

---

#### 5.2 ForumThread Entity
```java
@Entity
@Table(name = "forum_threads")
public class ForumThread extends TenantAwareAggregateRoot<ThreadStatus> {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forum_id", nullable = false)
    private Forum forum;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // Original post content (Markdown)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreadStatus status; // OPEN, ANSWERED, CLOSED, DELETED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private Boolean isPinned; // Stick to top

    @Column(nullable = false)
    private Boolean isLocked; // Cannot add new replies

    @Column(nullable = false)
    private Boolean isAnonymous;

    @Enumerated(EnumType.STRING)
    @Column
    private ThreadType type; // DISCUSSION, QUESTION, POLL, ANNOUNCEMENT

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<ForumPost> posts = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_answer_id")
    private ForumPost acceptedAnswer; // For Q&A threads

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private Long replyCount = 0L;

    @Column(nullable = false)
    private Integer upvotes = 0;

    @Column(nullable = false)
    private Integer downvotes = 0;

    @Column
    private Instant lastActivityAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_post_by_id")
    private User lastPostBy;

    @ElementCollection
    @CollectionTable(name = "thread_tags")
    private Set<String> tags = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "thread_watchers")
    @Column(name = "user_id")
    private Set<UUID> watcherIds = new HashSet<>(); // Users following this thread

    // Business methods
    public void markAsAnswered(ForumPost answer, User markedBy) {
        validateCanMarkAnswer(markedBy);
        this.acceptedAnswer = answer;
        this.status = ThreadStatus.ANSWERED;

        registerEvent(new ThreadAnsweredEvent(
            this.id, answer.getId(), markedBy.getId()
        ));
    }

    public void pin(User pinnedBy) {
        this.isPinned = true;
        registerEvent(new ThreadPinnedEvent(this.id, pinnedBy.getId()));
    }

    public void lock(User lockedBy, String reason) {
        this.isLocked = true;
        registerEvent(new ThreadLockedEvent(this.id, lockedBy.getId(), reason));
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void addWatcher(UUID userId) {
        this.watcherIds.add(userId);
    }

    private void validateCanMarkAnswer(User user) {
        boolean isAuthor = this.author.getId().equals(user.getId());
        boolean isModerator = forum.getModeratorIds().contains(user.getId());
        boolean isInstructor = user.hasRole("INSTRUCTOR");

        if (!isAuthor && !isModerator && !isInstructor) {
            throw new ForbiddenException("Only thread author, moderator, or instructor can mark answer");
        }
    }
}

public enum ThreadStatus {
    OPEN,
    ANSWERED,
    CLOSED,
    DELETED
}

public enum ThreadType {
    DISCUSSION,
    QUESTION,
    POLL,
    ANNOUNCEMENT
}
```

---

#### 5.3 ForumPost Entity
```java
@Entity
@Table(name = "forum_posts")
public class ForumPost extends TenantAwareAggregateRoot<PostStatus> {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private ForumThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_post_id")
    private ForumPost parentPost; // For nested replies

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // Markdown

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status; // PUBLISHED, PENDING_MODERATION, FLAGGED, DELETED

    @Column(nullable = false)
    private Boolean isAnonymous;

    @Column(nullable = false)
    private Boolean isEdited;

    @Column
    private Instant editedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by_id")
    private User editedBy;

    @Column(nullable = false)
    private Integer upvotes = 0;

    @Column(nullable = false)
    private Integer downvotes = 0;

    @OneToMany(mappedBy = "parentPost", cascade = CascadeType.ALL)
    private List<ForumPost> replies = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "post_attachments")
    @Column(name = "attachment_url")
    private List<String> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private Set<PostVote> votes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private Set<PostFlag> flags = new LinkedHashSet<>();

    // Business methods
    public void edit(String newContent, User editedBy) {
        validateCanEdit(editedBy);
        this.content = newContent;
        this.isEdited = true;
        this.editedAt = Instant.now();
        this.editedBy = editedBy;

        registerEvent(new PostEditedEvent(this.id, editedBy.getId()));
    }

    public void upvote(User voter) {
        PostVote existingVote = findVoteByUser(voter);
        if (existingVote != null) {
            if (existingVote.getVoteType() == VoteType.UPVOTE) {
                // Remove upvote
                votes.remove(existingVote);
                this.upvotes--;
            } else {
                // Change downvote to upvote
                existingVote.setVoteType(VoteType.UPVOTE);
                this.downvotes--;
                this.upvotes++;
            }
        } else {
            votes.add(new PostVote(this, voter, VoteType.UPVOTE));
            this.upvotes++;
        }
    }

    public void downvote(User voter) {
        // Similar to upvote logic
    }

    public void flag(User flagger, FlagReason reason, String description) {
        flags.add(new PostFlag(this, flagger, reason, description));

        if (flags.size() >= 3) { // Auto-flag threshold
            this.status = PostStatus.FLAGGED;
            registerEvent(new PostAutoFlaggedEvent(this.id));
        }
    }

    public Integer getScore() {
        return upvotes - downvotes;
    }

    private void validateCanEdit(User user) {
        boolean isAuthor = this.author.getId().equals(user.getId());
        boolean isModerator = thread.getForum().getModeratorIds().contains(user.getId());

        if (!isAuthor && !isModerator) {
            throw new ForbiddenException("Only author or moderator can edit post");
        }

        // Can only edit within 24 hours (unless moderator)
        if (isAuthor && !isModerator) {
            Duration timeSinceCreated = Duration.between(createdAt, Instant.now());
            if (timeSinceCreated.toHours() > 24) {
                throw new ForbiddenException("Cannot edit post after 24 hours");
            }
        }
    }
}

public enum PostStatus {
    PUBLISHED,
    PENDING_MODERATION,
    FLAGGED,
    DELETED
}
```

---

#### 5.4 PostVote Entity
```java
@Entity
@Table(name = "post_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "voter_id"})
})
public class PostVote {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteType voteType;

    @Column(nullable = false)
    private Instant votedAt;
}

public enum VoteType {
    UPVOTE,
    DOWNVOTE
}
```

---

#### 5.5 PostFlag Entity
```java
@Entity
@Table(name = "post_flags")
public class PostFlag {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flagger_id", nullable = false)
    private User flagger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlagReason reason;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Instant flaggedAt;

    @Enumerated(EnumType.STRING)
    @Column
    private FlagResolution resolution; // DISMISSED, REMOVED, WARNING_ISSUED, USER_BANNED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;

    @Column
    private Instant resolvedAt;

    @Column(length = 1000)
    private String resolutionNotes;
}

public enum FlagReason {
    SPAM,
    INAPPROPRIATE,
    HARASSMENT,
    OFF_TOPIC,
    PLAGIARISM,
    OTHER
}

public enum FlagResolution {
    DISMISSED,
    REMOVED,
    WARNING_ISSUED,
    USER_BANNED
}
```

---

### Workflow Design: Forum System

#### Thread Creation Workflow
```
1. User creates thread
   ├─ Select forum
   ├─ Choose thread type (Discussion/Question)
   ├─ Enter title & content (Markdown editor)
   ├─ Add tags
   ├─ Upload attachments (optional)
   └─ Toggle anonymous (if allowed)

2. Moderation check
   ├─ IF requireModeration = true
   │  ├─ Set status = PENDING_MODERATION
   │  └─ Notify moderators
   └─ ELSE
      └─ Set status = PUBLISHED

3. Notifications
   ├─ Notify forum watchers
   ├─ IF course forum → notify enrolled students
   └─ IF mentioned users (@username) → notify them
```

---

#### Post Reply Workflow
```
1. User replies to thread
   ├─ Check if thread is locked
   ├─ Enter reply content (Markdown)
   ├─ Optional: Quote parent post
   └─ Submit

2. Update thread
   ├─ Increment thread.replyCount
   ├─ Update thread.lastActivityAt
   ├─ Update thread.lastPostBy
   └─ Notify thread watchers

3. Notifications
   ├─ Notify thread author
   ├─ Notify mentioned users
   └─ Notify watchers (except the poster)
```

---

#### Voting Workflow
```
1. User upvotes/downvotes post
   ├─ Check if user already voted
   │  ├─ IF same vote → remove vote
   │  └─ IF opposite vote → change vote
   └─ ELSE → add new vote

2. Update post scores
   ├─ Recalculate upvotes/downvotes
   └─ Rank posts by score

3. Gamification (Optional)
   ├─ Award reputation points to post author
   └─ Track user's voting activity
```

---

#### Moderation Workflow
```
1. Post flagged by users
   ├─ User clicks "Flag" button
   ├─ Select reason
   ├─ Add description
   └─ Submit flag

2. Auto-flagging threshold
   ├─ IF flags.size() >= 3
   │  ├─ Set post.status = FLAGGED
   │  └─ Notify moderators immediately
   └─ ELSE
      └─ Add to moderation queue

3. Moderator reviews
   ├─ View flagged post + context
   ├─ Decide action:
   │  ├─ Dismiss (no action)
   │  ├─ Remove post
   │  ├─ Edit post (remove offensive part)
   │  ├─ Warn user
   │  └─ Ban user (if severe)
   └─ Record resolution

4. Notify flagger & author
   ├─ Flagger: "Your flag was reviewed"
   └─ Author: "Your post was removed for..."
```

---

### Frontend Design: Forum System

#### 5.6 Forum List Page
**File**: `src/app/forums/page.tsx`

**Layout**:
```
┌──────────────────────────────────────────────────────┐
│ Discussion Forums                                    │
├──────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────┐  │
│ │ Forum Name        Threads  Posts  Last Activity│  │
│ ├────────────────────────────────────────────────┤  │
│ │ 📢 Announcements     12      45   2 hours ago  │  │
│ │ 💬 General          234    1,234  5 mins ago   │  │
│ │ ❓ Q&A              156      678   1 hour ago  │  │
│ │ 📚 Course: React    89       456   Today 3pm   │  │
│ │ 📚 Course: Node.js  67       345   Yesterday   │  │
│ └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

---

#### 5.7 Thread List Page
**File**: `src/app/forums/[forumId]/page.tsx`

**Layout**:
```
┌──────────────────────────────────────────────────────┐
│ General Discussion                   [New Thread]    │
├──────────────────────────────────────────────────────┤
│ Sort: [Latest Activity ▼] Filter: [All ▼]          │
├──────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────┐  │
│ │ 📌 PINNED: Forum Rules (Please Read)           │  │
│ │ Started by Admin • 123 views • 5 replies       │  │
│ ├────────────────────────────────────────────────┤  │
│ │ ✅ [ANSWERED] How to deploy Next.js app?       │  │
│ │ Started by JohnDoe • 45 views • 12 replies     │  │
│ │ Tags: [nextjs] [deployment] [vercel]           │  │
│ │ Last: Sarah replied 2 hours ago                │  │
│ ├────────────────────────────────────────────────┤  │
│ │ 💬 Best practices for state management?        │  │
│ │ Started by Alice • 89 views • 23 replies       │  │
│ │ Tags: [react] [state] [redux]                  │  │
│ │ Last: Bob replied 5 mins ago                   │  │
│ └────────────────────────────────────────────────┘  │
│                                                      │
│ [Load More]                                         │
└──────────────────────────────────────────────────────┘
```

---

#### 5.8 Thread Detail Page
**File**: `src/app/forums/[forumId]/threads/[threadId]/page.tsx`

**Layout**:
```
┌──────────────────────────────────────────────────────┐
│ How to deploy Next.js app to Vercel?                │
│ Started by JohnDoe • 45 views • 12 replies           │
│ Tags: [nextjs] [deployment] [vercel]                │
├──────────────────────────────────────────────────────┤
│ ┌────────────────────────────────────────────────┐  │
│ │ 👤 JohnDoe                  [Edit] [Flag]      │  │
│ │ Posted 3 hours ago                             │  │
│ │                                                 │  │
│ │ I'm trying to deploy my Next.js app to Vercel │  │
│ │ but getting this error:                        │  │
│ │                                                 │  │
│ │ ```                                            │  │
│ │ Error: Module not found                       │  │
│ │ ```                                            │  │
│ │                                                 │  │
│ │ Has anyone faced this before?                  │  │
│ │                                                 │  │
│ │ [👍 5] [👎 0] [Reply] [Watch Thread]          │  │
│ └────────────────────────────────────────────────┘  │
│                                                      │
│ ┌────────────────────────────────────────────────┐  │
│ │ 👤 Sarah        [✓ ACCEPTED ANSWER] [Edit]    │  │
│ │ Posted 2 hours ago                             │  │
│ │                                                 │  │
│ │ This is likely a dependency issue. Try:       │  │
│ │                                                 │  │
│ │ ```bash                                        │  │
│ │ npm install --legacy-peer-deps                │  │
│ │ ```                                            │  │
│ │                                                 │  │
│ │ Then redeploy. Let me know if it works!       │  │
│ │                                                 │  │
│ │ [👍 12] [👎 0] [Reply]                        │  │
│ └────────────────────────────────────────────────┘  │
│                                                      │
│ ┌────────────────────────────────────────────────┐  │
│ │ 💬 Add your reply                              │  │
│ │ ┌──────────────────────────────────────────┐  │  │
│ │ │ Markdown editor...                        │  │  │
│ │ └──────────────────────────────────────────┘  │  │
│ │ [Attach] [Preview] [Submit Reply]             │  │
│ └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

---

### Summary: Forum System

| Component | Estimated Time | Priority |
|-----------|---------------|----------|
| Backend Entities | 1.5 days | High |
| Controllers & Services | 1.5 days | High |
| Voting & Moderation Logic | 1 day | High |
| Database Migration | 0.5 day | High |
| Forum List Page | 0.5 day | High |
| Thread List Page | 1 day | High |
| Thread Detail Page | 2 days | High |
| Markdown Editor Integration | 0.5 day | High |
| Moderation Queue | 1 day | Medium |
| Testing | 1 day | High |

**Total**: ~10-11 days (2 weeks)

---

## 📅 OVERALL TIMELINE SUMMARY

| Phase | Module | Duration | Dependencies |
|-------|--------|----------|--------------|
| Phase 1 | Complete Task Management | 3-4 days | None |
| Phase 2 | Complete Notification System | 4 days | Task completion |
| Phase 3 | Opportunity Management | 2 weeks | None (parallel) |
| Phase 4 | Quiz & Assessment System | 2.5 weeks | LMS completion |
| Phase 5 | Discussion Forum System | 2 weeks | None (parallel) |

**Total Estimated Time**: 8-10 weeks

**Parallel Execution Possible**:
- Opportunity (Phase 3) + Quiz (Phase 4) can run in parallel (different domains)
- Forum (Phase 5) can start after Phase 2 completes

**Optimized Timeline**: ~6-7 weeks with parallel development

---

## 🎯 SUCCESS CRITERIA

### Task Module
- [ ] Edit modal functional with all fields
- [ ] Detail page with comments
- [ ] Bulk operations working
- [ ] Advanced filters implemented
- [ ] 0 TypeScript errors
- [ ] Permission guards tested

### Notification Module
- [ ] Full notification page with pagination
- [ ] Preferences page saving settings
- [ ] Email notifications sending
- [ ] Push notifications working
- [ ] Real-time updates via WebSocket

### Opportunity Module
- [ ] Pipeline view (Kanban) functional
- [ ] Detail page with line items
- [ ] Stage transitions validated
- [ ] Forecast dashboard with charts
- [ ] Workflow automation triggering

### Quiz Module
- [ ] Quiz editor with 8 question types
- [ ] Quiz taking page with timer
- [ ] Auto-grading working correctly
- [ ] Manual grading queue functional
- [ ] Results page showing detailed feedback
- [ ] Certificate issuance on pass

### Forum Module
- [ ] Thread creation and replies working
- [ ] Voting system functional
- [ ] Accepted answer marking
- [ ] Moderation queue and flagging
- [ ] Markdown rendering correctly
- [ ] Anonymous posting (if enabled)

---

## 🚀 DEPLOYMENT CHECKLIST

### Before Production
- [ ] All unit tests passing (60%+ coverage)
- [ ] All integration tests passing
- [ ] E2E tests for critical flows
- [ ] Performance testing (1M users, 50K CCU)
- [ ] Security audit completed
- [ ] Database migration tested on staging
- [ ] Environment variables documented
- [ ] Monitoring & alerts configured
- [ ] Backup strategy in place
- [ ] Rollback plan documented

### Production Deployment
- [ ] Database backup before migration
- [ ] Run Flyway migrations
- [ ] Deploy backend (blue-green deployment)
- [ ] Deploy frontend (incremental rollout)
- [ ] Smoke tests pass
- [ ] Monitor error rates
- [ ] Monitor performance metrics
- [ ] Customer communication sent

---

**End of Implementation Plan**

**Last Updated**: November 22, 2025
**Version**: 1.0
**Status**: Ready for Implementation
