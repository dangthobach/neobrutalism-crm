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
class UsersIntegrationTest {

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
    void list_create_update_delete_user_flow() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", bearer);

        // list
        ResponseEntity<String> listResp = restTemplate.exchange(url("/users?page=0&size=10"), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // create
        String createJson = "{\"username\":\"test.user\",\"email\":\"test.user@example.com\",\"firstName\":\"Test\",\"lastName\":\"User\",\"password\":\"Passw0rd!\"}";
        ResponseEntity<Map> createResp = restTemplate.exchange(url("/users"), HttpMethod.POST, new HttpEntity<>(createJson, headers), Map.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map created = (Map) createResp.getBody().get("data");
        String id = (String) created.get("id");

        // update
        String updateJson = "{\"firstName\":\"Updated\"}";
        ResponseEntity<Map> updateResp = restTemplate.exchange(url("/users/" + id), HttpMethod.PUT, new HttpEntity<>(updateJson, headers), Map.class);
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // delete
        ResponseEntity<Map> deleteResp = restTemplate.exchange(url("/users/" + id), HttpMethod.DELETE, new HttpEntity<>(headers), Map.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}


