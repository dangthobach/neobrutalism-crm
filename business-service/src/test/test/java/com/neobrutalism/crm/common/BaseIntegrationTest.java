package com.neobrutalism.crm.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobrutalism.crm.common.multitenancy.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base integration test class with common setup
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    protected void setUp() {
        // Set default tenant for tests
        TenantContext.setCurrentTenant("test-tenant");
    }

    protected void setTenant(String tenantId) {
        TenantContext.setCurrentTenant(tenantId);
    }

    protected void clearTenant() {
        TenantContext.clear();
    }
}
