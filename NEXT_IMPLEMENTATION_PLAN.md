# NEXT IMPLEMENTATION PLAN - Neobrutalism CRM

**Date**: 2025-11-03
**Phase**: Post-Authentication Implementation
**Current Progress**: 75% Complete

---

## EXECUTIVE SUMMARY

The CRM system has reached a solid foundation with:
- ✅ Core infrastructure (100%)
- ✅ Authentication & Authorization (100%)
- ✅ User Management & RBAC (100%)
- ✅ Basic CRM (Customer/Contact) (95%)
- ⚠️ Advanced CRM features (30%)
- ⚠️ Testing coverage (20%)

**Next Priority**: Complete CRM core functionality and establish testing foundation

---

## PHASE 1: COMPLETE CRM CORE MODULES (Weeks 1-3)

### 1.1 Activity & Task Management

#### **Entities to Implement**

##### **Activity** (High Priority)
```java
@Entity
@Table(name = "activities")
public class Activity extends TenantAwareAggregateRoot<ActivityStatus> {

    // Core fields
    private String subject;
    private String description;
    private ActivityType type;  // CALL, MEETING, EMAIL, TASK, NOTE
    private ActivityStatus status;  // PLANNED, IN_PROGRESS, COMPLETED, CANCELLED

    // Relationships
    private UUID ownerId;  // Assigned user
    private UUID relatedToType;  // CUSTOMER, CONTACT, OPPORTUNITY
    private UUID relatedToId;

    // Scheduling
    private Instant scheduledStartAt;
    private Instant scheduledEndAt;
    private Instant actualStartAt;
    private Instant actualEndAt;
    private Integer durationMinutes;

    // Details
    private String location;
    private ActivityPriority priority;  // LOW, MEDIUM, HIGH, URGENT
    private String outcome;
    private String nextSteps;

    // Recurrence
    private Boolean isRecurring;
    private String recurrencePattern;  // DAILY, WEEKLY, MONTHLY
    private UUID parentActivityId;  // For recurring instances
}

enum ActivityType {
    CALL, MEETING, EMAIL, TASK, NOTE, DEMO, PRESENTATION
}

enum ActivityStatus {
    PLANNED, IN_PROGRESS, COMPLETED, CANCELLED, RESCHEDULED
}

enum ActivityPriority {
    LOW, MEDIUM, HIGH, URGENT
}
```

##### **Task** (High Priority)
```java
@Entity
@Table(name = "tasks")
public class Task extends TenantAwareAggregateRoot<TaskStatus> {

    private String title;
    private String description;
    private TaskStatus status;  // TODO, IN_PROGRESS, DONE, CANCELLED
    private TaskPriority priority;

    // Assignment
    private UUID assignedToId;
    private UUID assignedById;
    private UUID relatedToType;  // CUSTOMER, CONTACT, OPPORTUNITY
    private UUID relatedToId;

    // Scheduling
    private Instant dueDate;
    private Instant completedAt;
    private Integer estimatedHours;
    private Integer actualHours;

    // Progress
    private Integer progressPercentage;  // 0-100

    // Checklist
    @Type(JsonType.class)
    private List<TaskChecklistItem> checklist;
}

enum TaskStatus {
    TODO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED
}

enum TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

@Embeddable
class TaskChecklistItem {
    private String description;
    private Boolean completed;
}
```

#### **Implementation Steps**

1. **Create Domain Structure**
   ```
   domain/activity/
     ├── model/Activity.java
     ├── repository/ActivityRepository.java
     ├── service/ActivityService.java
     ├── controller/ActivityController.java
     ├── dto/ActivityRequest.java
     ├── dto/ActivityResponse.java
     ├── event/ActivityCreatedEvent.java
     ├── event/ActivityCompletedEvent.java
     └── specification/ActivitySpecification.java

   domain/task/
     ├── model/Task.java
     ├── repository/TaskRepository.java
     ├── service/TaskService.java
     ├── controller/TaskController.java
     ├── dto/TaskRequest.java
     ├── dto/TaskResponse.java
     └── specification/TaskSpecification.java
   ```

2. **Create Migration**
   ```sql
   -- V112__Create_activity_task_tables.sql
   CREATE TABLE activities (
       id UUID PRIMARY KEY,
       subject VARCHAR(255) NOT NULL,
       description TEXT,
       activity_type VARCHAR(50) NOT NULL,
       status VARCHAR(50) NOT NULL,
       owner_id UUID NOT NULL REFERENCES users(id),
       related_to_type VARCHAR(50),
       related_to_id UUID,
       scheduled_start_at TIMESTAMP,
       scheduled_end_at TIMESTAMP,
       actual_start_at TIMESTAMP,
       actual_end_at TIMESTAMP,
       duration_minutes INTEGER,
       location VARCHAR(255),
       priority VARCHAR(50),
       outcome TEXT,
       next_steps TEXT,
       is_recurring BOOLEAN DEFAULT FALSE,
       recurrence_pattern VARCHAR(100),
       parent_activity_id UUID REFERENCES activities(id),
       -- Audit fields
       tenant_id VARCHAR(50) NOT NULL,
       created_at TIMESTAMP NOT NULL,
       created_by VARCHAR(100),
       updated_at TIMESTAMP,
       updated_by VARCHAR(100),
       version BIGINT
   );

   CREATE TABLE tasks (
       id UUID PRIMARY KEY,
       title VARCHAR(255) NOT NULL,
       description TEXT,
       status VARCHAR(50) NOT NULL,
       priority VARCHAR(50),
       assigned_to_id UUID REFERENCES users(id),
       assigned_by_id UUID REFERENCES users(id),
       related_to_type VARCHAR(50),
       related_to_id UUID,
       due_date TIMESTAMP,
       completed_at TIMESTAMP,
       estimated_hours INTEGER,
       actual_hours INTEGER,
       progress_percentage INTEGER DEFAULT 0,
       checklist JSONB,
       -- Audit fields
       tenant_id VARCHAR(50) NOT NULL,
       created_at TIMESTAMP NOT NULL,
       created_by VARCHAR(100),
       updated_at TIMESTAMP,
       updated_by VARCHAR(100),
       version BIGINT
   );

   -- Indexes
   CREATE INDEX idx_activities_owner ON activities(owner_id);
   CREATE INDEX idx_activities_tenant ON activities(tenant_id);
   CREATE INDEX idx_activities_related ON activities(related_to_type, related_to_id);
   CREATE INDEX idx_activities_scheduled ON activities(scheduled_start_at);
   CREATE INDEX idx_activities_status ON activities(status);

   CREATE INDEX idx_tasks_assigned ON tasks(assigned_to_id);
   CREATE INDEX idx_tasks_tenant ON tasks(tenant_id);
   CREATE INDEX idx_tasks_related ON tasks(related_to_type, related_to_id);
   CREATE INDEX idx_tasks_due ON tasks(due_date);
   CREATE INDEX idx_tasks_status ON tasks(status);
   ```

3. **Implement Services**
   - ActivityService with scheduling logic
   - TaskService with assignment logic
   - Activity-Task linking
   - Notification triggers

4. **REST API Endpoints**
   ```
   Activities:
   GET    /api/activities
   GET    /api/activities/{id}
   POST   /api/activities
   PUT    /api/activities/{id}
   DELETE /api/activities/{id}
   POST   /api/activities/{id}/complete
   POST   /api/activities/{id}/cancel
   POST   /api/activities/{id}/reschedule
   GET    /api/activities/calendar  (calendar view)
   GET    /api/activities/upcoming  (next 7 days)

   Tasks:
   GET    /api/tasks
   GET    /api/tasks/{id}
   POST   /api/tasks
   PUT    /api/tasks/{id}
   DELETE /api/tasks/{id}
   POST   /api/tasks/{id}/assign
   POST   /api/tasks/{id}/complete
   GET    /api/tasks/my-tasks
   GET    /api/tasks/assigned-by-me
   ```

5. **Add Domain Events**
   - ActivityCreatedEvent
   - ActivityCompletedEvent
   - ActivityCancelledEvent
   - TaskCreatedEvent
   - TaskAssignedEvent
   - TaskCompletedEvent

---

### 1.2 Opportunity & Deal Management

#### **Entities to Implement**

##### **Opportunity** (High Priority)
```java
@Entity
@Table(name = "opportunities")
public class Opportunity extends TenantAwareAggregateRoot<OpportunityStatus> {

    // Core fields
    private String name;
    private String description;
    private UUID customerId;  // Required
    private UUID contactId;   // Primary contact

    // Sales info
    private OpportunityStatus status;  // LEAD, QUALIFIED, PROPOSAL, NEGOTIATION, WON, LOST
    private OpportunityStage stage;
    private BigDecimal amount;
    private String currency;
    private Integer probability;  // 0-100
    private BigDecimal expectedRevenue;  // amount * probability

    // Timeline
    private LocalDate expectedCloseDate;
    private LocalDate actualCloseDate;
    private Integer salesCycleDays;

    // Assignment
    private UUID ownerId;
    private UUID accountManagerId;

    // Source & Competition
    private String leadSource;
    private String competitors;
    private String lossReason;  // If lost

    // Products
    @OneToMany(mappedBy = "opportunityId")
    private Set<OpportunityProduct> products;

    // State machine transitions
    public void qualify() { ... }
    public void moveToProposal() { ... }
    public void moveToNegotiation() { ... }
    public void win() { ... }
    public void lose(String reason) { ... }
}

enum OpportunityStatus {
    LEAD,           // Initial stage
    QUALIFIED,      // Qualified lead
    PROPOSAL,       // Proposal sent
    NEGOTIATION,    // In negotiation
    WON,            // Deal won
    LOST,           // Deal lost
    ABANDONED       // Opportunity abandoned
}

enum OpportunityStage {
    DISCOVERY, QUALIFICATION, NEEDS_ANALYSIS, VALUE_PROPOSITION,
    PROPOSAL, NEGOTIATION, CLOSED_WON, CLOSED_LOST
}
```

##### **OpportunityProduct** (Medium Priority)
```java
@Entity
@Table(name = "opportunity_products")
public class OpportunityProduct extends TenantAwareEntity {

    private UUID opportunityId;
    private UUID productId;  // Reference to Product entity (if exists)
    private String productName;
    private String description;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;  // Percentage or amount
    private BigDecimal totalPrice;

    private Integer displayOrder;
}
```

##### **Product** (Optional - can defer)
```java
@Entity
@Table(name = "products")
public class Product extends TenantAwareAggregateRoot<ProductStatus> {

    private String code;  // SKU
    private String name;
    private String description;
    private ProductCategory category;

    private BigDecimal listPrice;
    private String currency;

    private ProductStatus status;  // ACTIVE, INACTIVE, DISCONTINUED
    private Boolean isRecurring;
    private String billingFrequency;  // MONTHLY, YEARLY
}
```

#### **Implementation Steps**

1. **Create Domain Structure** (similar to Activity)

2. **Create Migration**
   ```sql
   -- V113__Create_opportunity_tables.sql
   CREATE TABLE opportunities (
       id UUID PRIMARY KEY,
       name VARCHAR(255) NOT NULL,
       description TEXT,
       customer_id UUID NOT NULL REFERENCES customers(id),
       contact_id UUID REFERENCES contacts(id),
       status VARCHAR(50) NOT NULL,
       stage VARCHAR(50),
       amount DECIMAL(15,2),
       currency VARCHAR(3) DEFAULT 'USD',
       probability INTEGER CHECK (probability >= 0 AND probability <= 100),
       expected_revenue DECIMAL(15,2),
       expected_close_date DATE,
       actual_close_date DATE,
       sales_cycle_days INTEGER,
       owner_id UUID NOT NULL REFERENCES users(id),
       account_manager_id UUID REFERENCES users(id),
       lead_source VARCHAR(100),
       competitors TEXT,
       loss_reason TEXT,
       -- Audit
       tenant_id VARCHAR(50) NOT NULL,
       created_at TIMESTAMP NOT NULL,
       created_by VARCHAR(100),
       updated_at TIMESTAMP,
       updated_by VARCHAR(100),
       version BIGINT
   );

   CREATE TABLE opportunity_products (
       id UUID PRIMARY KEY,
       opportunity_id UUID NOT NULL REFERENCES opportunities(id) ON DELETE CASCADE,
       product_id UUID,
       product_name VARCHAR(255) NOT NULL,
       description TEXT,
       quantity INTEGER NOT NULL DEFAULT 1,
       unit_price DECIMAL(15,2) NOT NULL,
       discount DECIMAL(15,2) DEFAULT 0,
       total_price DECIMAL(15,2) NOT NULL,
       display_order INTEGER,
       -- Audit
       tenant_id VARCHAR(50) NOT NULL,
       created_at TIMESTAMP NOT NULL,
       created_by VARCHAR(100),
       updated_at TIMESTAMP,
       updated_by VARCHAR(100),
       version BIGINT
   );

   -- Indexes
   CREATE INDEX idx_opportunities_customer ON opportunities(customer_id);
   CREATE INDEX idx_opportunities_owner ON opportunities(owner_id);
   CREATE INDEX idx_opportunities_status ON opportunities(status);
   CREATE INDEX idx_opportunities_close_date ON opportunities(expected_close_date);
   CREATE INDEX idx_opportunity_products_opp ON opportunity_products(opportunity_id);
   ```

3. **Implement Services**
   - OpportunityService with state machine
   - Sales pipeline calculations
   - Revenue forecasting
   - Probability calculations

4. **REST API Endpoints**
   ```
   GET    /api/opportunities
   GET    /api/opportunities/{id}
   POST   /api/opportunities
   PUT    /api/opportunities/{id}
   DELETE /api/opportunities/{id}
   POST   /api/opportunities/{id}/qualify
   POST   /api/opportunities/{id}/move-to-proposal
   POST   /api/opportunities/{id}/move-to-negotiation
   POST   /api/opportunities/{id}/win
   POST   /api/opportunities/{id}/lose
   GET    /api/opportunities/pipeline  (sales pipeline view)
   GET    /api/opportunities/forecast  (revenue forecast)

   Products:
   GET    /api/opportunities/{id}/products
   POST   /api/opportunities/{id}/products
   PUT    /api/opportunities/{id}/products/{productId}
   DELETE /api/opportunities/{id}/products/{productId}
   ```

---

### 1.3 Note & Comment System

#### **Entities to Implement**

##### **Note** (Medium Priority)
```java
@Entity
@Table(name = "notes")
public class Note extends TenantAwareEntity {

    private String title;
    private String content;  // Rich text

    // Relationship (polymorphic)
    private String relatedToType;  // CUSTOMER, CONTACT, OPPORTUNITY, ACTIVITY
    private UUID relatedToId;

    private UUID createdById;
    private Boolean isPinned;
    private Boolean isPrivate;  // Only visible to creator

    @ElementCollection
    private Set<String> tags;
}
```

##### **Comment** (Low Priority - can defer)
```java
@Entity
@Table(name = "comments")
public class Comment extends TenantAwareEntity {

    private String content;

    private String relatedToType;
    private UUID relatedToId;

    private UUID createdById;
    private UUID parentCommentId;  // For threading

    @ElementCollection
    private Set<UUID> mentions;  // @mentioned users
}
```

---

## PHASE 2: TESTING FOUNDATION (Weeks 4-5)

### 2.1 Unit Testing Strategy

#### **Test Structure**
```
src/test/java/
  ├── com/neobrutalism/crm/
      ├── common/
      │   ├── entity/BaseEntityTest.java
      │   ├── service/BaseServiceTest.java
      │   └── specification/SpecificationTest.java
      │
      ├── domain/
      │   ├── user/
      │   │   ├── service/UserServiceTest.java
      │   │   ├── repository/UserRepositoryTest.java
      │   │   └── controller/UserControllerTest.java
      │   │
      │   ├── customer/
      │   │   ├── service/CustomerServiceTest.java
      │   │   └── ...
      │   │
      │   └── [repeat for all domains]
      │
      └── security/
          ├── JwtTokenProviderTest.java
          ├── PermissionServiceTest.java
          └── UserSessionServiceTest.java
```

#### **Testing Tools**
```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers for integration tests -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### **Sample Test Template**

##### **Service Test**
```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("Should create customer with valid data")
    void shouldCreateCustomer() {
        // Given
        CustomerRequest request = CustomerRequest.builder()
            .companyName("Test Company")
            .email("test@example.com")
            .build();

        Customer customer = new Customer();
        customer.setCompanyName(request.getCompanyName());

        when(customerRepository.save(any(Customer.class)))
            .thenReturn(customer);

        // When
        Customer result = customerService.create(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompanyName()).isEqualTo("Test Company");
        verify(eventPublisher).publish(any(CustomerCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when duplicate email")
    void shouldThrowExceptionWhenDuplicateEmail() {
        // Given
        CustomerRequest request = CustomerRequest.builder()
            .companyName("Test Company")
            .email("duplicate@example.com")
            .build();

        when(customerRepository.existsByEmailAndDeletedFalse("duplicate@example.com"))
            .thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> customerService.create(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Email already exists");
    }

    @Test
    @DisplayName("Should convert lead to active customer")
    void shouldConvertLeadToActive() {
        // Given
        Customer customer = new Customer();
        customer.setStatus(CustomerStatus.LEAD);

        when(customerRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(customer));

        // When
        customerService.convertToActive(customer.getId());

        // Then
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        verify(eventPublisher).publish(any(CustomerStatusChangedEvent.class));
    }
}
```

##### **Repository Test**
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class CustomerRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("Should find customer by email")
    void shouldFindByEmail() {
        // Given
        Customer customer = new Customer();
        customer.setCompanyName("Test Company");
        customer.setEmail("test@example.com");
        customerRepository.save(customer);

        // When
        Optional<Customer> found = customerRepository.findByEmailAndDeletedFalse("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCompanyName()).isEqualTo("Test Company");
    }
}
```

##### **Controller Test**
```java
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /api/customers should return customer list")
    void shouldReturnCustomerList() throws Exception {
        // Given
        List<Customer> customers = Arrays.asList(
            createCustomer("Company A"),
            createCustomer("Company B")
        );

        when(customerService.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(customers));

        // When & Then
        mockMvc.perform(get("/api/customers")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/customers should create customer")
    void shouldCreateCustomer() throws Exception {
        // Given
        CustomerRequest request = CustomerRequest.builder()
            .companyName("New Company")
            .email("new@example.com")
            .build();

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCompanyName(request.getCompanyName());

        when(customerService.create(any(CustomerRequest.class)))
            .thenReturn(customer);

        // When & Then
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.companyName").value("New Company"));
    }
}
```

#### **Test Coverage Goals**
- **Services**: 90% coverage
- **Controllers**: 80% coverage
- **Repositories**: 70% coverage
- **Entities**: 60% coverage (getter/setter excluded)
- **Overall**: 80% coverage

---

### 2.2 Integration Testing

#### **Integration Test Setup**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String getAuthToken(String username, String password) {
        // Login and return JWT token
        LoginRequest loginRequest = new LoginRequest(username, password);
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
            "/api/auth/login",
            loginRequest,
            LoginResponse.class
        );
        return response.getBody().getAccessToken();
    }
}
```

#### **Sample Integration Test**
```java
class CustomerIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should complete customer lifecycle")
    void shouldCompleteCustomerLifecycle() {
        // 1. Login as admin
        String token = getAuthToken("admin", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        // 2. Create customer
        CustomerRequest createRequest = CustomerRequest.builder()
            .companyName("Integration Test Company")
            .email("integration@test.com")
            .build();

        HttpEntity<CustomerRequest> createEntity = new HttpEntity<>(createRequest, headers);
        ResponseEntity<CustomerResponse> createResponse = restTemplate.exchange(
            "/api/customers",
            HttpMethod.POST,
            createEntity,
            CustomerResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID customerId = createResponse.getBody().getId();

        // 3. Get customer
        HttpEntity<?> getEntity = new HttpEntity<>(headers);
        ResponseEntity<CustomerResponse> getResponse = restTemplate.exchange(
            "/api/customers/" + customerId,
            HttpMethod.GET,
            getEntity,
            CustomerResponse.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getStatus()).isEqualTo(CustomerStatus.LEAD);

        // 4. Convert to active
        ResponseEntity<Void> convertResponse = restTemplate.exchange(
            "/api/customers/" + customerId + "/convert-to-active",
            HttpMethod.POST,
            getEntity,
            Void.class
        );

        assertThat(convertResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 5. Verify status changed
        ResponseEntity<CustomerResponse> verifyResponse = restTemplate.exchange(
            "/api/customers/" + customerId,
            HttpMethod.GET,
            getEntity,
            CustomerResponse.class
        );

        assertThat(verifyResponse.getBody().getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }
}
```

---

## PHASE 3: ANALYTICS & REPORTING (Weeks 6-7)

### 3.1 Dashboard Metrics

#### **Metrics to Track**

##### **Sales Metrics**
```java
@Service
public class SalesMetricsService {

    public SalesMetrics getDashboardMetrics(UUID userId, LocalDate startDate, LocalDate endDate) {
        return SalesMetrics.builder()
            // Pipeline
            .totalOpportunities(countOpportunities())
            .totalPipelineValue(calculatePipelineValue())
            .averageDealSize(calculateAverageDealSize())
            .winRate(calculateWinRate())

            // Activities
            .activitiesCompleted(countCompletedActivities())
            .activitiesPlanned(countPlannedActivities())
            .meetingsThisWeek(countMeetings())
            .callsThisWeek(countCalls())

            // Customers
            .newCustomers(countNewCustomers())
            .activeCustomers(countActiveCustomers())
            .churnRate(calculateChurnRate())

            // Revenue
            .actualRevenue(calculateActualRevenue())
            .forecastedRevenue(calculateForecastedRevenue())
            .revenueByStage(groupRevenueByStage())

            .build();
    }
}
```

##### **Dashboard Endpoints**
```
GET /api/dashboard/metrics
GET /api/dashboard/pipeline
GET /api/dashboard/activities/calendar
GET /api/dashboard/opportunities/forecast
GET /api/dashboard/customers/stats
```

### 3.2 Reporting System

#### **Report Types**
1. **Sales Pipeline Report**
   - Opportunities by stage
   - Win/loss analysis
   - Sales cycle length

2. **Activity Report**
   - Activity volume by type
   - Completion rate
   - User productivity

3. **Customer Report**
   - Customer acquisition
   - Customer lifecycle
   - Churn analysis

4. **Revenue Report**
   - Actual vs forecasted
   - Revenue by product
   - Revenue by customer

#### **Report API**
```
GET /api/reports/sales-pipeline?startDate=2025-01-01&endDate=2025-12-31
GET /api/reports/activities?userId={id}&period=monthly
GET /api/reports/customers?filter=active&segment=enterprise
GET /api/reports/revenue?groupBy=product
```

---

## PHASE 4: COMPLETE DOMAIN EVENTS & CQRS (Weeks 8-9)

### 4.1 Add Missing Domain Events

#### **Priority Order**
1. **Customer Events** (High Priority)
   - CustomerCreatedEvent
   - CustomerUpdatedEvent
   - CustomerStatusChangedEvent
   - CustomerConvertedEvent

2. **Contact Events** (High Priority)
   - ContactCreatedEvent
   - ContactUpdatedEvent
   - ContactLinkedEvent

3. **Opportunity Events** (High Priority)
   - OpportunityCreatedEvent
   - OpportunityUpdatedEvent
   - OpportunityStageChangedEvent
   - OpportunityWonEvent
   - OpportunityLostEvent

4. **Activity Events** (Medium Priority)
   - ActivityScheduledEvent
   - ActivityCompletedEvent
   - ActivityCancelledEvent

5. **Task Events** (Medium Priority)
   - TaskCreatedEvent
   - TaskAssignedEvent
   - TaskCompletedEvent

### 4.2 Implement Event Handlers

#### **Example: Notification on Task Assignment**
```java
@Component
@RequiredArgsConstructor
public class TaskEventHandler {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @EventListener
    public void handleTaskAssigned(TaskAssignedEvent event) {
        // Send in-app notification
        notificationService.createNotification(
            event.getAssignedToId(),
            "New Task Assigned",
            "You have been assigned a new task: " + event.getTaskTitle(),
            NotificationType.INFO,
            "/tasks/" + event.getTaskId()
        );

        // Send email notification
        emailService.sendTaskAssignmentEmail(
            event.getAssignedToId(),
            event.getTaskTitle(),
            event.getDueDate()
        );
    }

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        // Notify assigner
        notificationService.createNotification(
            event.getAssignedById(),
            "Task Completed",
            event.getAssignedToName() + " completed the task: " + event.getTaskTitle(),
            NotificationType.SUCCESS,
            "/tasks/" + event.getTaskId()
        );
    }
}
```

### 4.3 CQRS Read Models

#### **Create Read Models**
```java
@Entity
@Table(name = "customer_read_model")
public class CustomerReadModel {

    @Id
    private UUID customerId;

    // Denormalized fields
    private String companyName;
    private String email;
    private String phone;
    private CustomerStatus status;

    // Aggregated data
    private Integer totalOpportunities;
    private BigDecimal totalRevenue;
    private BigDecimal pipelineValue;
    private Integer openActivities;
    private LocalDate lastActivityDate;

    // Owner info (denormalized)
    private UUID ownerId;
    private String ownerName;

    // Branch info (denormalized)
    private UUID branchId;
    private String branchName;
}

@Component
public class CustomerReadModelUpdater {

    @EventListener
    @Transactional
    public void handle(CustomerCreatedEvent event) {
        CustomerReadModel readModel = new CustomerReadModel();
        readModel.setCustomerId(event.getCustomerId());
        readModel.setCompanyName(event.getCompanyName());
        // ... set other fields
        customerReadModelRepository.save(readModel);
    }

    @EventListener
    @Transactional
    public void handle(OpportunityCreatedEvent event) {
        // Update customer's total opportunities and pipeline value
        customerReadModelRepository.incrementOpportunityCount(event.getCustomerId());
        customerReadModelRepository.addToPipelineValue(
            event.getCustomerId(),
            event.getAmount()
        );
    }
}
```

---

## PHASE 5: DEVOPS & OBSERVABILITY (Weeks 10-11)

### 5.1 CI/CD Pipeline

#### **GitHub Actions Workflow**
```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop, feature/* ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Run tests
        run: mvn clean verify

      - name: Generate test report
        run: mvn surefire-report:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: mvn sonar:sonar

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v3

      - name: Build Docker image
        run: docker build -t neobrutalism-crm:${{ github.sha }} .

      - name: Push to registry
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push neobrutalism-crm:${{ github.sha }}
```

### 5.2 Monitoring & Observability

#### **Add Micrometer Metrics**
```java
@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
public class CustomerService {

    private final Counter customerCreatedCounter;
    private final Timer customerCreationTimer;

    public CustomerService(MeterRegistry registry) {
        this.customerCreatedCounter = Counter.builder("customers.created")
            .description("Total customers created")
            .tag("type", "customer")
            .register(registry);

        this.customerCreationTimer = Timer.builder("customers.creation.time")
            .description("Time to create customer")
            .register(registry);
    }

    @Timed(value = "customers.create", description = "Time to create customer")
    public Customer create(CustomerRequest request) {
        return customerCreationTimer.record(() -> {
            Customer customer = // ... create logic
            customerCreatedCounter.increment();
            return customer;
        });
    }
}
```

#### **Prometheus Configuration**
```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active}
```

#### **Grafana Dashboards**
- Application metrics (requests, latency, errors)
- Business metrics (customers, opportunities, revenue)
- JVM metrics (heap, threads, GC)
- Database metrics (connections, query time)

---

## PHASE 6: ADVANCED FEATURES (Weeks 12+)

### 6.1 Email Integration

#### **Email Templates**
```html
<!-- templates/email/task-assignment.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
</head>
<body>
    <h2>New Task Assigned</h2>
    <p>Hi <span th:text="${assigneeName}"></span>,</p>
    <p>You have been assigned a new task:</p>
    <h3 th:text="${taskTitle}"></h3>
    <p th:text="${taskDescription}"></p>
    <p><strong>Due Date:</strong> <span th:text="${dueDate}"></span></p>
    <a th:href="${taskUrl}">View Task</a>
</body>
</html>
```

#### **Email Service**
```java
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendTaskAssignmentEmail(UUID userId, String taskTitle, Instant dueDate) {
        User user = userRepository.findById(userId).orElseThrow();

        Context context = new Context();
        context.setVariable("assigneeName", user.getFullName());
        context.setVariable("taskTitle", taskTitle);
        context.setVariable("dueDate", dueDate);
        context.setVariable("taskUrl", "https://crm.example.com/tasks/...");

        String htmlContent = templateEngine.process("email/task-assignment", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(user.getEmail());
        helper.setSubject("New Task Assigned: " + taskTitle);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
```

### 6.2 Real-time Notifications

#### **WebSocket Implementation**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("*")
            .withSockJS();
    }
}

@Controller
public class NotificationWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendNotificationToUser(UUID userId, Notification notification) {
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            notification
        );
    }
}
```

### 6.3 Import/Export

#### **CSV Export**
```java
@Service
public class CustomerExportService {

    public byte[] exportToCsv(List<Customer> customers) {
        StringWriter writer = new StringWriter();
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
            .withHeader("ID", "Company Name", "Email", "Phone", "Status", "Created At"));

        for (Customer customer : customers) {
            printer.printRecord(
                customer.getId(),
                customer.getCompanyName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStatus(),
                customer.getCreatedAt()
            );
        }

        return writer.toString().getBytes();
    }
}
```

#### **Excel Export**
```java
@Service
public class CustomerExportService {

    public byte[] exportToExcel(List<Customer> customers) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Customers");

        // Header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Company Name");
        headerRow.createCell(1).setCellValue("Email");
        // ...

        // Data rows
        int rowNum = 1;
        for (Customer customer : customers) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(customer.getCompanyName());
            row.createCell(1).setCellValue(customer.getEmail());
            // ...
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }
}
```

---

## IMPLEMENTATION CHECKLIST

### **Phase 1: CRM Core Modules** ✅ Priority 1
- [ ] Activity entity and state machine
- [ ] Task entity and assignment logic
- [ ] Activity/Task services and controllers
- [ ] Activity/Task migrations
- [ ] Activity/Task REST APIs
- [ ] Activity/Task domain events
- [ ] Opportunity entity and state machine
- [ ] OpportunityProduct linking
- [ ] Opportunity services and controllers
- [ ] Opportunity migrations
- [ ] Opportunity REST APIs
- [ ] Sales pipeline calculations
- [ ] Note entity
- [ ] Note service and controller

### **Phase 2: Testing Foundation** ✅ Priority 1
- [ ] Set up testing dependencies
- [ ] Create BaseTest classes
- [ ] Write UserService tests
- [ ] Write CustomerService tests
- [ ] Write OpportunityService tests
- [ ] Write controller tests
- [ ] Write repository tests with Testcontainers
- [ ] Integration tests for complete workflows
- [ ] Achieve 80% code coverage

### **Phase 3: Analytics & Reporting** ⚠️ Priority 2
- [ ] Sales metrics service
- [ ] Dashboard API
- [ ] Sales pipeline report
- [ ] Activity report
- [ ] Customer report
- [ ] Revenue report

### **Phase 4: Domain Events & CQRS** ⚠️ Priority 2
- [ ] Add Customer domain events
- [ ] Add Contact domain events
- [ ] Add Opportunity domain events
- [ ] Add Activity domain events
- [ ] Add Task domain events
- [ ] Implement event handlers
- [ ] Create CustomerReadModel
- [ ] Create OpportunityReadModel
- [ ] Implement read model updaters

### **Phase 5: DevOps & Observability** ⚠️ Priority 2
- [ ] GitHub Actions CI/CD pipeline
- [ ] Docker image builds
- [ ] SonarQube integration
- [ ] Micrometer metrics
- [ ] Prometheus configuration
- [ ] Grafana dashboards
- [ ] Distributed tracing

### **Phase 6: Advanced Features** ⚠️ Priority 3
- [ ] Email integration
- [ ] Email templates
- [ ] Real-time notifications via WebSocket
- [ ] CSV export
- [ ] Excel export
- [ ] PDF report generation
- [ ] Calendar integration
- [ ] Workflow automation

---

## ESTIMATED TIMELINE

| Phase | Duration | Key Deliverables | Status |
|-------|----------|------------------|--------|
| **Phase 1** | 3 weeks | Activity, Task, Opportunity modules | 🔵 Not Started |
| **Phase 2** | 2 weeks | 80% test coverage | 🔵 Not Started |
| **Phase 3** | 2 weeks | Dashboard & Reports | 🔵 Not Started |
| **Phase 4** | 2 weeks | Complete event system | 🔵 Not Started |
| **Phase 5** | 2 weeks | CI/CD & Monitoring | 🔵 Not Started |
| **Phase 6** | 2+ weeks | Email, Export, Advanced | 🔵 Not Started |
| **Total** | **13+ weeks** | **Production-ready CRM** | |

---

## SUCCESS CRITERIA

### **Completion Criteria for Each Phase**

#### **Phase 1: CRM Core Modules**
- ✅ All entities created with proper state machines
- ✅ Services implement business logic
- ✅ Controllers expose REST APIs
- ✅ Migrations run successfully
- ✅ Manual testing passes
- ✅ OpenAPI documentation complete

#### **Phase 2: Testing Foundation**
- ✅ 80%+ code coverage
- ✅ All service tests pass
- ✅ All controller tests pass
- ✅ Integration tests for key workflows
- ✅ CI pipeline runs tests automatically

#### **Phase 3: Analytics & Reporting**
- ✅ Dashboard shows real-time metrics
- ✅ Reports generate correctly
- ✅ Performance is acceptable (<2s)

#### **Phase 4: Domain Events & CQRS**
- ✅ All domains publish events
- ✅ Event handlers process correctly
- ✅ Read models stay synchronized
- ✅ Event sourcing provides audit trail

#### **Phase 5: DevOps & Observability**
- ✅ CI/CD pipeline deploys automatically
- ✅ Metrics visible in Grafana
- ✅ Alerts configured
- ✅ Logs aggregated

#### **Phase 6: Advanced Features**
- ✅ Emails send correctly
- ✅ Real-time notifications work
- ✅ Export generates valid files
- ✅ Performance acceptable

---

## NEXT IMMEDIATE STEPS (Week 1)

### **Day 1-2: Activity Module**
1. Create Activity entity with state machine
2. Create ActivityRepository
3. Create ActivityService with business logic
4. Create ActivityController with REST endpoints
5. Create migration V112__Create_activity_tables.sql
6. Test Activity CRUD operations

### **Day 3-4: Task Module**
1. Create Task entity
2. Create TaskRepository
3. Create TaskService with assignment logic
4. Create TaskController
5. Add tasks table to migration V112
6. Test Task CRUD and assignment

### **Day 5: Integration & Testing**
1. Link Activity and Task to Customer/Contact/Opportunity
2. Test complete workflows
3. Add basic unit tests
4. Update OpenAPI documentation
5. Commit and push to git

---

## CONCLUSION

This implementation plan provides a **clear roadmap** for completing the Neobrutalism CRM system. The plan follows **industry best practices** and ensures:

- **Complete CRM functionality** (Activity, Task, Opportunity)
- **High code quality** (80%+ test coverage)
- **Production readiness** (CI/CD, monitoring)
- **Scalability** (CQRS, caching, event-driven)
- **Maintainability** (consistent patterns, documentation)

By following this plan phase by phase, the CRM system will reach **production-ready status** in approximately **13 weeks**, with core functionality available in the first **3 weeks**.

---

**Next Action**: Start implementing Phase 1 - Activity module

**Questions?**: Review the repository analysis in the exploration report for detailed current state.
