package com.neobrutalism.crm.domain.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neobrutalism.crm.common.BaseIntegrationTest;
import com.neobrutalism.crm.domain.organization.dto.OrganizationRequest;
import com.neobrutalism.crm.domain.organization.model.Organization;
import com.neobrutalism.crm.domain.organization.model.OrganizationStatus;
import com.neobrutalism.crm.domain.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrganizationControllerTest extends BaseIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization testOrganization;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        
        // Create test organization
        testOrganization = new Organization();
        testOrganization.setName("Test Organization");
        testOrganization.setCode("TEST_ORG");
        testOrganization.setDescription("Test description");
        testOrganization.setEmail("test@example.com");
        testOrganization.setPhone("+1234567890");
        testOrganization.setStatus(OrganizationStatus.ACTIVE);
        
        testOrganization = organizationRepository.save(testOrganization);
    }

    @Test
    void getAllOrganizations_ShouldReturnPagedResults() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/organizations")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].name").value("Test Organization"));
    }

    @Test
    void getOrganizationById_ShouldReturnOrganization() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/organizations/{id}", testOrganization.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Organization"))
                .andExpect(jsonPath("$.data.code").value("TEST_ORG"));
    }

    @Test
    void getOrganizationById_WithInvalidId_ShouldReturn404() throws Exception {
        UUID invalidId = UUID.randomUUID();
        mockMvc.perform(MockMvcRequestBuilders.get("/api/organizations/{id}", invalidId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrganization_ShouldCreateSuccessfully() throws Exception {
        OrganizationRequest request = new OrganizationRequest();
        request.setName("New Organization");
        request.setCode("NEW_ORG");
        request.setDescription("New description");
        request.setEmail("new@example.com");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Organization"));
    }

    @Test
    void createOrganization_WithInvalidData_ShouldReturn400() throws Exception {
        OrganizationRequest request = new OrganizationRequest();
        // Missing required fields

        mockMvc.perform(MockMvcRequestBuilders.post("/api/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrganization_ShouldUpdateSuccessfully() throws Exception {
        OrganizationRequest request = new OrganizationRequest();
        request.setName("Updated Organization");
        request.setCode("UPDATED_ORG");
        request.setDescription("Updated description");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/organizations/{id}", testOrganization.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Organization"));
    }

    @Test
    void deleteOrganization_ShouldDeleteSuccessfully() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/organizations/{id}", testOrganization.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void activateOrganization_ShouldChangeStatus() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/organizations/{id}/activate", testOrganization.getId())
                        .param("reason", "Testing activation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void getOrganizationsByStatus_ShouldReturnFilteredResults() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/organizations/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }
}
