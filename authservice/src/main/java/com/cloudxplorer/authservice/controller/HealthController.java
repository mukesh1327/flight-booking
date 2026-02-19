package com.cloudxplorer.authservice.controller;

import com.cloudxplorer.authservice.config.AuthProperties;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private static final String SERVICE_NAME = "authservice";
    private static final String SERVICE_VERSION = "1.0.0";
    private static final Instant STARTED_AT = Instant.now();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    private final AuthProperties authProperties;

    public HealthController(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("Received health check request");
        long uptimeSeconds = Duration.between(STARTED_AT, Instant.now()).getSeconds();
        DbCheckResult dbCheck = checkDatabase();
        DependencyCheckResult rhbkCheck = checkRhbk();
        String tableCheckName = "auth-schema-" + authProperties.getDb().getHealthTable();
        boolean isUp = dbCheck.status().equals("UP") && rhbkCheck.status().equals("UP");
        String overallStatus = isUp ? "UP" : "DOWN";
        log.info("Health result status={} dbStatus={} rhbkStatus={}", overallStatus, dbCheck.status(), rhbkCheck.status());
        HttpStatus httpStatus = isUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(Map.of(
                "service", SERVICE_NAME,
                "version", SERVICE_VERSION,
                "environment", "dev",
                "status", overallStatus,
                "timestamp", Instant.now().toString(),
                "uptimeSeconds", uptimeSeconds,
                "checks", List.of(
                        Map.of("name", "postgres", "status", dbCheck.status(), "latencyMs", dbCheck.latencyMs()),
                        Map.of("name", tableCheckName, "status", dbCheck.tableStatus(), "latencyMs", dbCheck.latencyMs()),
                        Map.of("name", "rhbk", "status", rhbkCheck.status(), "latencyMs", rhbkCheck.latencyMs())
                )
        ));
    }

    @GetMapping("/health/live")
    public ResponseEntity<Map<String, Object>> healthLive() {
        log.info("Received liveness check request");
        long uptimeSeconds = Duration.between(STARTED_AT, Instant.now()).getSeconds();
        return ResponseEntity.ok(Map.of(
                "service", SERVICE_NAME,
                "version", SERVICE_VERSION,
                "environment", "dev",
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "uptimeSeconds", uptimeSeconds
        ));
    }

    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> healthReady() {
        log.info("Received readiness check request");
        long uptimeSeconds = Duration.between(STARTED_AT, Instant.now()).getSeconds();
        DbCheckResult dbCheck = checkDatabase();
        DependencyCheckResult rhbkCheck = checkRhbk();
        String tableCheckName = "auth-schema-" + authProperties.getDb().getHealthTable();
        boolean isUp = dbCheck.status().equals("UP") && rhbkCheck.status().equals("UP");
        String overallStatus = isUp ? "UP" : "DOWN";
        log.info("Readiness result status={} dbStatus={} rhbkStatus={}", overallStatus, dbCheck.status(), rhbkCheck.status());
        HttpStatus httpStatus = isUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(Map.of(
                "service", SERVICE_NAME,
                "version", SERVICE_VERSION,
                "environment", "dev",
                "status", overallStatus,
                "timestamp", Instant.now().toString(),
                "uptimeSeconds", uptimeSeconds,
                "checks", List.of(
                        Map.of("name", "postgres", "status", dbCheck.status(), "latencyMs", dbCheck.latencyMs()),
                        Map.of("name", tableCheckName, "status", dbCheck.tableStatus(), "latencyMs", dbCheck.latencyMs()),
                        Map.of("name", "rhbk", "status", rhbkCheck.status(), "latencyMs", rhbkCheck.latencyMs())
                )
        ));
    }

    private DbCheckResult checkDatabase() {
        long start = System.nanoTime();
        try (Connection conn = DriverManager.getConnection(
                authProperties.getDb().getUrl(),
                authProperties.getDb().getUsername(),
                authProperties.getDb().getPassword())) {
            try (PreparedStatement one = conn.prepareStatement("SELECT 1")) {
                one.executeQuery();
            }

            String healthTable = authProperties.getDb().getHealthTable();
            boolean tableExists;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT to_regclass(?)")) {
                stmt.setString(1, healthTable);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    tableExists = rs.getString(1) != null;
                }
            }

            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            log.debug("Database health check successful table={} latencyMs={}", healthTable, latencyMs);
            return new DbCheckResult("UP", tableExists ? "UP" : "DOWN", latencyMs);
        } catch (Exception ex) {
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            log.warn("Database health check failed latencyMs={} message={}", latencyMs, ex.getMessage());
            return new DbCheckResult("DOWN", "DOWN", latencyMs);
        }
    }

    private DependencyCheckResult checkRhbk() {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(authProperties.getRhbk().getHealthUrl()))
                    .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                    .GET()
                    .build();
            HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            boolean isUp = response.statusCode() >= 200 && response.statusCode() < 400;
            if (!isUp) {
                log.warn("RHBK health check returned non-success statusCode={} latencyMs={}", response.statusCode(), latencyMs);
            } else {
                log.debug("RHBK health check successful statusCode={} latencyMs={}", response.statusCode(), latencyMs);
            }
            return new DependencyCheckResult(isUp ? "UP" : "DOWN", latencyMs);
        } catch (Exception ex) {
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            log.warn("RHBK health check failed latencyMs={} message={}", latencyMs, ex.getMessage());
            return new DependencyCheckResult("DOWN", latencyMs);
        }
    }

    private record DbCheckResult(String status, String tableStatus, long latencyMs) {
    }

    private record DependencyCheckResult(String status, long latencyMs) {
    }
}
