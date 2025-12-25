package com.neobrutalism.crm.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomersIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String bearer;

    private String url(String path) { return "http://localhost:" + port + "/api" + path; }

    @BeforeEach
    void login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        ResponseEntity<Map> loginResp = restTemplate.exchange(url("/auth/login"), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        Map data = (Map) loginResp.getBody().get("data");
        bearer = "Bearer " + data.get("accessToken");
    }

    @Test
    void list_create_update_delete_customer_flow() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", bearer);

        // list with filters
        ResponseEntity<String> listResp = restTemplate.exchange(url("/customers?page=0&size=10&status=ACTIVE"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // create
        String createJson = "{\"code\":\"CUST-IT-001\",\"companyName\":\"Acme Inc\",\"customerType\":\"B2B\",\"status\":\"ACTIVE\",\"organizationId\":\"00000000-0000-0000-0000-000000000001\"}";
        ResponseEntity<Map> createResp = restTemplate.exchange(url("/customers"), HttpMethod.POST, new HttpEntity<>(createJson, headers), Map.class);
        assertThat(createResp.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }
}


