package com.cloudxplorer.authservice.api.controller;

import com.cloudxplorer.authservice.api.dto.health.HealthResponse;
import com.cloudxplorer.authservice.infrastructure.config.AuthServiceProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final AuthServiceProperties properties;

    public HealthController(AuthServiceProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(buildResponse("UP", "health"));
    }

    @GetMapping("/ready")
    public ResponseEntity<HealthResponse> readiness() {
        return ResponseEntity.ok(buildResponse("UP", "readiness"));
    }

    @GetMapping("/live")
    public ResponseEntity<HealthResponse> liveness() {
        return ResponseEntity.ok(buildResponse("UP", "liveness"));
    }

    private HealthResponse buildResponse(String status, String description) {
        Map<String, String> details = Map.of(
            "appBaseUrl", nullSafe(properties.appBaseUrl(), "unset"),
            "keycloakBaseUrl", nullSafe(properties.keycloak() != null ? properties.keycloak().baseUrl() : null, "unset"),
            "publicClientId", nullSafe(properties.publicClient() != null ? properties.publicClient().clientId() : null, "unset"),
            "corpClientId", nullSafe(properties.corp() != null ? properties.corp().clientId() : null, "unset"),
            "mode", description
        );

        return new HealthResponse(
            status,
            Instant.now().toString(),
            nullSafe(properties.activeEnv(), "unknown"),
            details
        );
    }

    private static String nullSafe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
