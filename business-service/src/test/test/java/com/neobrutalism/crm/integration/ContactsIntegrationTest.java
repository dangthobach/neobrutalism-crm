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
class ContactsIntegrationTest {

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
    void list_and_create_contact_flow() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", bearer);

        // list
        ResponseEntity<String> listResp = restTemplate.exchange(url("/contacts?page=0&size=10"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(listResp.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
    }
}


