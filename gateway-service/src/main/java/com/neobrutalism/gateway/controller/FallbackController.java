package com.neobrutalism.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Fallback controller for circuit breaker scenarios
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/business-service")
    public Mono<ResponseEntity<Map<String, Object>>> businessServiceFallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Service Unavailable",
                        "message", "Business service is currently unavailable. Please try again later.",
                        "status", 503
                )));
    }
}

