package com.neobrutalism.crm.contract;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String url() { return "http://localhost:" + port + "/api-docs"; }

    @Test
    void openapi_should_expose_core_paths() {
        ResponseEntity<Map> resp = restTemplate.getForEntity(url(), Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        Map body = resp.getBody();
        assertThat(body).isNotNull();
        Map paths = (Map) body.get("paths");
        assertThat(paths).isNotNull();
        assertThat(paths.keySet())
                .anyMatch(p -> p.toString().contains("/api/users"))
                .anyMatch(p -> p.toString().contains("/api/customers"))
                .anyMatch(p -> p.toString().contains("/api/contacts"));
    }
}


