package com.neobrutalism.crm.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    @Test
    void login_refresh_logout_flow_should_work() {
        // login
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        ResponseEntity<Map> loginResp = restTemplate.exchange(url("/auth/login"), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map data = (Map) loginResp.getBody().get("data");
        assertThat(data.get("accessToken")).isNotNull();
        assertThat(data.get("refreshToken")).isNotNull();

        String accessToken = (String) data.get("accessToken");
        String refreshToken = (String) data.get("refreshToken");

        // refresh
        String refreshBody = String.format("{\"refreshToken\":\"%s\"}", refreshToken);
        ResponseEntity<Map> refreshResp = restTemplate.exchange(url("/auth/refresh"), HttpMethod.POST, new HttpEntity<>(refreshBody, headers), Map.class);
        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map refreshData = (Map) refreshResp.getBody().get("data");
        assertThat(refreshData.get("accessToken")).isNotNull();
        assertThat(refreshData.get("refreshToken")).isNotNull();
        String rotatedRefresh = (String) refreshData.get("refreshToken");
        assertThat(rotatedRefresh).isNotEqualTo(refreshToken);

        // call protected with access token
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);
        ResponseEntity<String> meResp = restTemplate.exchange(url("/auth/me"), HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);
        assertThat(meResp.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.UNAUTHORIZED);

        // logout
        ResponseEntity<Map> logoutResp = restTemplate.exchange(url("/auth/logout"), HttpMethod.POST, new HttpEntity<>(authHeaders), Map.class);
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}


