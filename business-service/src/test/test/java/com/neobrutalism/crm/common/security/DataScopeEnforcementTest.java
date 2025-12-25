package com.neobrutalism.crm.common.security;

import com.neobrutalism.crm.domain.customer.model.Customer;
import com.neobrutalism.crm.domain.customer.repository.CustomerRepository;
import com.neobrutalism.crm.domain.task.model.Task;
import com.neobrutalism.crm.domain.task.repository.TaskRepository;
import com.neobrutalism.crm.domain.user.model.DataScope;
import com.neobrutalism.crm.domain.user.model.User;
import com.neobrutalism.crm.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Data Scope Enforcement
 * Tests that row-level security properly filters data based on user's data scope
 *
 * Test Scenarios:
 * 1. ALL_BRANCHES: User sees all records in organization
 * 2. CURRENT_BRANCH: User sees current branch + child branches
 * 3. SELF_ONLY: User sees only own created records
 * 4. Cross-tenant isolation: Users cannot see other tenants' data
 * 5. No context: When context not set, no filtering occurs (admin/system operation)
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Data Scope Enforcement Tests")
class DataScopeEnforcementTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    // Test data IDs
    private UUID tenantId;
    private UUID organizationId;
    private UUID branch1Id;
    private UUID branch2Id;
    private UUID user1Id; // ALL_BRANCHES
    private UUID user2Id; // CURRENT_BRANCH
    private UUID user3Id; // SELF_ONLY
    private UUID customer1Id;
    private UUID customer2Id;
    private UUID customer3Id;
    private UUID task1Id;
    private UUID task2Id;
    private UUID task3Id;

    @BeforeEach
    void setUp() {
        // Initialize test data IDs
        tenantId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        branch1Id = UUID.randomUUID();
        branch2Id = UUID.randomUUID();
        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        user3Id = UUID.randomUUID();

        // Create test users with different data scopes
        User user1 = createTestUser(user1Id, "user1", DataScope.ALL_BRANCHES, branch1Id);
        User user2 = createTestUser(user2Id, "user2", DataScope.CURRENT_BRANCH, branch1Id);
        User user3 = createTestUser(user3Id, "user3", DataScope.SELF_ONLY, branch2Id);

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        // Create test customers
        customer1Id = UUID.randomUUID();
        customer2Id = UUID.randomUUID();
        customer3Id = UUID.randomUUID();

        Customer customer1 = createTestCustomer(customer1Id, "Customer 1", branch1Id, user1Id);
        Customer customer2 = createTestCustomer(customer2Id, "Customer 2", branch1Id, user2Id);
        Customer customer3 = createTestCustomer(customer3Id, "Customer 3", branch2Id, user3Id);

        customerRepository.save(customer1);
        customerRepository.save(customer2);
        customerRepository.save(customer3);

        // Create test tasks
        task1Id = UUID.randomUUID();
        task2Id = UUID.randomUUID();
        task3Id = UUID.randomUUID();

        Task task1 = createTestTask(task1Id, "Task 1", branch1Id, user1Id, user1Id);
        Task task2 = createTestTask(task2Id, "Task 2", branch1Id, user2Id, user2Id);
        Task task3 = createTestTask(task3Id, "Task 3", branch2Id, user3Id, user3Id);

        taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(task3);
    }

    @AfterEach
    void tearDown() {
        // Clear DataScopeContext after each test
        DataScopeContext.clear();
    }

    // ========================================
    // Test Scenario 1: ALL_BRANCHES Scope
    // ========================================

    @Test
    @DisplayName("ALL_BRANCHES user should see all records in organization")
    void testAllBranchesScope_ShouldSeeAllRecords() {
        // Given: User with ALL_BRANCHES scope
        setupDataScopeContext(user1Id, DataScope.ALL_BRANCHES, branch1Id, Set.of(branch1Id, branch2Id));

        // When: Query customers
        List<Customer> customers = customerRepository.findAllWithScope();

        // Then: Should see all 3 customers
        assertThat(customers).hasSize(3);
        assertThat(customers).extracting(Customer::getId)
                .containsExactlyInAnyOrder(customer1Id, customer2Id, customer3Id);
    }

    @Test
    @DisplayName("ALL_BRANCHES user should see all records with pagination")
    void testAllBranchesScope_WithPagination() {
        // Given: User with ALL_BRANCHES scope
        setupDataScopeContext(user1Id, DataScope.ALL_BRANCHES, branch1Id, Set.of(branch1Id, branch2Id));

        // When: Query with pagination
        Page<Customer> customersPage = customerRepository.findAllWithScope(PageRequest.of(0, 10));

        // Then: Should see all 3 customers
        assertThat(customersPage.getTotalElements()).isEqualTo(3);
        assertThat(customersPage.getContent()).hasSize(3);
    }

    // ========================================
    // Test Scenario 2: CURRENT_BRANCH Scope
    // ========================================

    @Test
    @DisplayName("CURRENT_BRANCH user should see only their branch + accessible branches")
    void testCurrentBranchScope_ShouldSeeAccessibleBranches() {
        // Given: User with CURRENT_BRANCH scope (only has access to branch1)
        setupDataScopeContext(user2Id, DataScope.CURRENT_BRANCH, branch1Id, Set.of(branch1Id));

        // When: Query customers
        List<Customer> customers = customerRepository.findAllWithScope();

        // Then: Should see only customers from branch1 (2 customers)
        assertThat(customers).hasSize(2);
        assertThat(customers).extracting(Customer::getId)
                .containsExactlyInAnyOrder(customer1Id, customer2Id);
    }

    @Test
    @DisplayName("CURRENT_BRANCH user should see branch hierarchy")
    void testCurrentBranchScope_WithBranchHierarchy() {
        // Given: User with CURRENT_BRANCH scope with access to multiple branches
        setupDataScopeContext(user2Id, DataScope.CURRENT_BRANCH, branch1Id, Set.of(branch1Id, branch2Id));

        // When: Query customers
        List<Customer> customers = customerRepository.findAllWithScope();

        // Then: Should see customers from all accessible branches
        assertThat(customers).hasSize(3);
    }

    // ========================================
    // Test Scenario 3: SELF_ONLY Scope
    // ========================================

    @Test
    @DisplayName("SELF_ONLY user should see only their own created records")
    void testSelfOnlyScope_ShouldSeeOwnRecordsOnly() {
        // Given: User with SELF_ONLY scope
        setupDataScopeContext(user3Id, DataScope.SELF_ONLY, branch2Id, Set.of(branch2Id));

        // When: Query customers
        List<Customer> customers = customerRepository.findAllWithScope();

        // Then: Should see only their own customer (customer3)
        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).getId()).isEqualTo(customer3Id);
        assertThat(customers.get(0).getCreatedBy()).isEqualTo(user3Id);
    }

    @Test
    @DisplayName("SELF_ONLY user should see only their own tasks")
    void testSelfOnlyScope_ForTasks() {
        // Given: User with SELF_ONLY scope
        setupDataScopeContext(user3Id, DataScope.SELF_ONLY, branch2Id, Set.of(branch2Id));

        // When: Query tasks
        List<Task> tasks = taskRepository.findAllWithScope();

        // Then: Should see only their own task (task3)
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getId()).isEqualTo(task3Id);
        assertThat(tasks.get(0).getCreatedBy()).isEqualTo(user3Id);
    }

    // ========================================
    // Test Scenario 4: Cross-Tenant Isolation
    // ========================================

    @Test
    @DisplayName("Users should not see data from other tenants")
    void testCrossTenantIsolation() {
        // Given: Create customer for different tenant
        UUID otherTenantId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        Customer otherCustomer = createTestCustomer(otherCustomerId, "Other Tenant Customer", branch1Id, user1Id);
        otherCustomer.setTenantId(otherTenantId.toString());
        customerRepository.save(otherCustomer);

        // Given: User with ALL_BRANCHES scope for their tenant
        setupDataScopeContext(user1Id, DataScope.ALL_BRANCHES, branch1Id, Set.of(branch1Id, branch2Id));

        // When: Query customers
        List<Customer> customers = customerRepository.findAllWithScope();

        // Then: Should NOT see customer from other tenant
        assertThat(customers).hasSize(3); // Only customers from their tenant
        assertThat(customers).extracting(Customer::getId)
                .doesNotContain(otherCustomerId);
    }

    // ========================================
    // Test Scenario 5: No Context (Admin/System Operations)
    // ========================================

    @Test
    @DisplayName("When no context set, no filtering should occur (admin/system operation)")
    void testNoContext_ShouldAllowAllAccess() {
        // Given: No DataScopeContext set (cleared in @AfterEach, not set here)
        // This simulates system operations or admin bypass

        // When: Query customers
        List<Customer> customers = customerRepository.findAllWithScope();

        // Then: Should see all customers (no filtering)
        assertThat(customers).hasSize(3);
    }

    // ========================================
    // Test Scenario 6: Combined Filters
    // ========================================

    @Test
    @DisplayName("Data scope should work with additional filters")
    void testDataScopeWithAdditionalFilters() {
        // Given: User with CURRENT_BRANCH scope
        setupDataScopeContext(user2Id, DataScope.CURRENT_BRANCH, branch1Id, Set.of(branch1Id));

        // When: Query customers for specific organization (combined filter)
        List<Customer> customers = customerRepository.findByOrganizationIdWithScope(organizationId);

        // Then: Should see only customers from their accessible branches in that organization
        assertThat(customers).hasSize(2);
        assertThat(customers).extracting(Customer::getId)
                .containsExactlyInAnyOrder(customer1Id, customer2Id);
    }

    @Test
    @DisplayName("Data scope should work with pagination and additional filters")
    void testDataScopeWithPaginationAndFilters() {
        // Given: User with ALL_BRANCHES scope
        setupDataScopeContext(user1Id, DataScope.ALL_BRANCHES, branch1Id, Set.of(branch1Id, branch2Id));

        // When: Query with organization filter and pagination
        Page<Customer> customersPage = customerRepository.findByOrganizationIdWithScope(
                organizationId, PageRequest.of(0, 10));

        // Then: Should see all customers from organization
        assertThat(customersPage.getTotalElements()).isEqualTo(3);
    }

    // ========================================
    // Test Scenario 7: Helper Methods
    // ========================================

    @Test
    @DisplayName("DataScopeHelper should correctly identify scope levels")
    void testDataScopeHelperUtilities() {
        // Test ALL_BRANCHES detection
        setupDataScopeContext(user1Id, DataScope.ALL_BRANCHES, branch1Id, Set.of(branch1Id, branch2Id));
        assertThat(DataScopeHelper.hasAllBranchesAccess()).isTrue();
        assertThat(DataScopeHelper.hasCurrentBranchAccess()).isFalse();
        assertThat(DataScopeHelper.hasSelfOnlyAccess()).isFalse();

        // Test CURRENT_BRANCH detection
        setupDataScopeContext(user2Id, DataScope.CURRENT_BRANCH, branch1Id, Set.of(branch1Id));
        assertThat(DataScopeHelper.hasAllBranchesAccess()).isFalse();
        assertThat(DataScopeHelper.hasCurrentBranchAccess()).isTrue();
        assertThat(DataScopeHelper.hasSelfOnlyAccess()).isFalse();

        // Test SELF_ONLY detection
        setupDataScopeContext(user3Id, DataScope.SELF_ONLY, branch2Id, Set.of(branch2Id));
        assertThat(DataScopeHelper.hasAllBranchesAccess()).isFalse();
        assertThat(DataScopeHelper.hasCurrentBranchAccess()).isFalse();
        assertThat(DataScopeHelper.hasSelfOnlyAccess()).isTrue();
    }

    @Test
    @DisplayName("DataScopeHelper should provide context information")
    void testDataScopeHelperContextInfo() {
        // Given: User context
        setupDataScopeContext(user1Id, DataScope.ALL_BRANCHES, branch1Id, Set.of(branch1Id, branch2Id));

        // When: Get context info
        UUID userId = DataScopeHelper.getCurrentUserId();
        UUID branchId = DataScopeHelper.getCurrentBranchId();
        Set<UUID> accessibleBranches = DataScopeHelper.getAccessibleBranchIds();
        boolean hasContext = DataScopeHelper.isContextPopulated();
        String debugInfo = DataScopeHelper.getDebugInfo();

        // Then: Should return correct values
        assertThat(userId).isEqualTo(user1Id);
        assertThat(branchId).isEqualTo(branch1Id);
        assertThat(accessibleBranches).containsExactlyInAnyOrder(branch1Id, branch2Id);
        assertThat(hasContext).isTrue();
        assertThat(debugInfo).contains(user1Id.toString(), "ALL_BRANCHES");
    }

    // ========================================
    // Helper Methods
    // ========================================

    private void setupDataScopeContext(UUID userId, DataScope dataScope, UUID branchId, Set<UUID> accessibleBranchIds) {
        DataScopeContext context = DataScopeContext.builder()
                .userId(userId)
                .tenantId(tenantId.toString())
                .dataScope(dataScope)
                .branchId(branchId)
                .accessibleBranchIds(accessibleBranchIds)
                .build();
        DataScopeContext.set(context);
    }

    private User createTestUser(UUID id, String username, DataScope dataScope, UUID branchId) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setFirstName("Test");
        user.setLastName(username);
        user.setDataScope(dataScope);
        user.setBranchId(branchId);
        user.setOrganizationId(organizationId);
        user.setTenantId(tenantId.toString());
        user.setDeleted(false);
        return user;
    }

    private Customer createTestCustomer(UUID id, String companyName, UUID branchId, UUID createdBy) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setCode("CUST-" + id.toString().substring(0, 8));
        customer.setCompanyName(companyName);
        customer.setBranchId(branchId);
        customer.setOrganizationId(organizationId);
        customer.setTenantId(tenantId.toString());
        customer.setCreatedBy(createdBy);
        customer.setDeleted(false);
        return customer;
    }

    private Task createTestTask(UUID id, String title, UUID branchId, UUID assignedToId, UUID createdBy) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setBranchId(branchId);
        task.setOrganizationId(organizationId);
        task.setTenantId(tenantId.toString());
        task.setAssignedToId(assignedToId);
        task.setCreatedBy(createdBy);
        task.setDeleted(false);
        return task;
    }
}
