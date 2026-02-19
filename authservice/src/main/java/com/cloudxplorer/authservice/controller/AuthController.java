package com.cloudxplorer.authservice.controller;

import com.cloudxplorer.authservice.config.AuthProperties;
import com.cloudxplorer.authservice.model.AuthResponse;
import com.cloudxplorer.authservice.model.AdminUserRequest;
import com.cloudxplorer.authservice.model.AdminUserUpdateRequest;
import com.cloudxplorer.authservice.model.ErrorResponse;
import com.cloudxplorer.authservice.model.LoginRequest;
import com.cloudxplorer.authservice.model.MessageResponse;
import com.cloudxplorer.authservice.model.ProfileUpdateRequest;
import com.cloudxplorer.authservice.model.RefreshTokenRequest;
import com.cloudxplorer.authservice.model.RegisterResponse;
import com.cloudxplorer.authservice.model.User;
import com.cloudxplorer.authservice.model.UserProfileResponse;
import com.cloudxplorer.authservice.model.LogoutRequest;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final JsonParser JSON_PARSER = JsonParserFactory.getJsonParser();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern JSON_STRING_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern JSON_NUMBER_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");
    private static final Pattern LAST_PATH_SEGMENT_PATTERN = Pattern.compile(".*/([^/]+)$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{4,32}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,15}$");
    private static final Set<String> SUPPORTED_USER_ROLES = Set.of("customer", "admin", "support_agent", "airline_ops");
    private static final String ADMIN_ROLE = "admin";

    private final AuthProperties authProperties;

    public AuthController(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody User request,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        log.info("Register request received path={} correlationId={}", httpRequest.getRequestURI(), correlationId);
        if (request == null || isBlank(request.getName()) || isBlank(request.getUsername())
                || isBlank(request.getEmail()) || isBlank(request.getPassword()) || isBlank(request.getPhone())) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "name, username, email, password and phone are required");
        }

        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone().trim();
        if (username.equalsIgnoreCase(email)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "username cannot be same as email");
        }
        if (!isValidUsername(username)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "username must be 4-32 chars and contain only letters, digits, ., _, -");
        }
        if (!isValidEmail(email)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "email format is invalid");
        }
        if (!isStrongPassword(request.getPassword())) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "password must be 8+ chars with upper, lower, digit and special character");
        }
        if (!isValidPhone(phone)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "phone must contain 10 to 15 digits");
        }
        String keycloakUserId;

        try (Connection conn = openConnection()) {
            if (findUserByEmail(conn, email) != null) {
                log.warn("Register failed duplicate email={} correlationId={}", email, correlationId);
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "USER_ALREADY_EXISTS", "user already exists");
            }
            if (findUserByPhone(conn, phone) != null) {
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "PHONE_ALREADY_EXISTS", "phone already exists");
            }
        } catch (SQLException ex) {
            log.error("Register failed DB pre-check error email={} correlationId={} message={}", email, correlationId, ex.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to register user");
        }

        try {
            if (keycloakUserExistsByUsername(username)) {
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "USERNAME_ALREADY_EXISTS", "username already exists");
            }
            if (keycloakUserExistsByEmail(email)) {
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "USER_ALREADY_EXISTS", "user already exists");
            }
            keycloakUserId = createKeycloakUser(request, username, email, "customer", correlationId);
        } catch (KeycloakConflictException ex) {
            log.warn("Register failed duplicate keycloak user email={} correlationId={}", email, correlationId);
            return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                    "USER_ALREADY_EXISTS", "user already exists");
        } catch (Exception ex) {
            log.error("Register failed keycloak error email={} correlationId={} message={}", email, correlationId, ex.getMessage());
            return error(HttpStatus.SERVICE_UNAVAILABLE, httpRequest, correlationId,
                    "KEYCLOAK_UNAVAILABLE", "failed to create user in keycloak");
        }

        String userId = keycloakUserId;
        try (Connection conn = openConnection()) {
            String sql = "INSERT INTO " + userTable() + " (user_id, name, username, email, phone) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                stmt.setString(2, request.getName());
                stmt.setString(3, username);
                stmt.setString(4, email);
                stmt.setString(5, phone);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            log.error("Register failed DB error email={} correlationId={} message={}", email, correlationId, ex.getMessage());
            try {
                deleteKeycloakUser(keycloakUserId, correlationId);
            } catch (Exception cleanupEx) {
                log.error("Register rollback failed keycloakUserId={} correlationId={} message={}",
                        keycloakUserId, correlationId, cleanupEx.getMessage());
            }
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to register user");
        }

        log.info("Register successful email={} userId={} correlationId={}", email, userId, correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(userId, "registered"));
    }

    @GetMapping("/login/authorize")
    public ResponseEntity<?> authorizeUrl(
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam(value = "redirectUri", required = false) String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        String resolvedRedirectUri = isBlank(redirectUri) ? authProperties.getRhbk().getDefaultRedirectUri() : redirectUri;
        String resolvedScope = isBlank(scope) ? authProperties.getRhbk().getDefaultScope() : scope;
        String state = randomUrlToken(24);
        String codeVerifier = randomUrlToken(64);
        String codeChallenge = sha256Base64Url(codeVerifier);

        String authorizeUrl = authProperties.getRhbk().getAuthorizationUrl()
                + "?response_type=code"
                + "&client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                + "&redirect_uri=" + urlEncode(resolvedRedirectUri)
                + "&scope=" + urlEncode(resolvedScope)
                + "&state=" + urlEncode(state)
                + "&code_challenge=" + urlEncode(codeChallenge)
                + "&code_challenge_method=S256";

        log.info("Authorize URL generated correlationId={} redirectUri={}", correlationId, resolvedRedirectUri);
        return ResponseEntity.ok(Map.of(
                "authorizationUrl", authorizeUrl,
                "state", state,
                "codeVerifier", codeVerifier,
                "redirectUri", resolvedRedirectUri,
                "scope", resolvedScope
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        log.info("Login request received path={} correlationId={}", httpRequest.getRequestURI(), correlationId);
        if (request == null || isBlank(request.getCode()) || isBlank(request.getCodeVerifier())) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "authorization code and code_verifier are required");
        }

        String redirectUri = isBlank(request.getRedirectUri())
                ? authProperties.getRhbk().getDefaultRedirectUri()
                : request.getRedirectUri();
        try {
            String form = "grant_type=authorization_code"
                    + "&client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                    + "&code=" + urlEncode(request.getCode())
                    + "&code_verifier=" + urlEncode(request.getCodeVerifier())
                    + "&redirect_uri=" + urlEncode(redirectUri);
            if (!isBlank(authProperties.getRhbk().getClientSecret())) {
                form = form + "&client_secret=" + urlEncode(authProperties.getRhbk().getClientSecret());
            }

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create(authProperties.getRhbk().getTokenUrl()))
                    .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> tokenResponse = HTTP_CLIENT.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
                KeycloakTokenError tokenError = parseKeycloakTokenError(tokenResponse.body());
                log.warn("Login rejected by keycloak statusCode={} error={} description={} correlationId={}",
                        tokenResponse.statusCode(), tokenError.code(), tokenError.description(), correlationId);
                return mapLoginFailureResponse(tokenResponse.statusCode(), tokenError, httpRequest, correlationId);
            }

            String body = tokenResponse.body();
            String accessToken = extractStringField(body, "access_token");
            String tokenType = extractStringField(body, "token_type");
            int expiresIn = extractIntField(body, "expires_in", 3600);
            String refreshToken = extractStringField(body, "refresh_token");

            if (isBlank(accessToken)) {
                log.warn("Login response missing access token correlationId={}", correlationId);
                return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                        "INVALID_CREDENTIALS", "invalid credentials");
            }

            String userId = extractJwtClaim(accessToken, "sub");
            if (isBlank(userId)) {
                userId = "unknown";
            }
            Set<String> roles = extractRolesFromAccessToken(accessToken);
            if (!hasExactlyOneSupportedRole(roles)) {
                log.warn("Login denied due to role isolation violation roles={} correlationId={}", roles, correlationId);
                return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                        "ACCESS_DENIED", "user must have exactly one supported role");
            }
            String responseTokenType = isBlank(tokenType) ? TOKEN_TYPE_BEARER : tokenType;
            log.info("Login successful userId={} roles={} correlationId={}", userId, roles, correlationId);
            return ResponseEntity.ok(new AuthResponse(accessToken, responseTokenType, expiresIn, userId, refreshToken));
        } catch (Exception ex) {
            log.error("Login failed due to keycloak connectivity correlationId={} message={}", correlationId, ex.getMessage());
            return error(HttpStatus.SERVICE_UNAVAILABLE, httpRequest, correlationId,
                    "KEYCLOAK_UNAVAILABLE", "failed to connect to keycloak");
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        log.info("Profile request received path={} correlationId={}", httpRequest.getRequestURI(), correlationId);
        KeycloakPrincipal principal;
        principal = resolvePrincipal(jwt);
        if (principal == null || isBlank(principal.email())) {
            log.warn("Profile request unauthorized correlationId={}", correlationId);
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!hasExactlyRole(principal, "customer")) {
            return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                    "ACCESS_DENIED", "customer role only is required");
        }

        UserRecord user;
        try (Connection conn = openConnection()) {
            user = findUserByEmail(conn, principal.email().toLowerCase());
        } catch (SQLException ex) {
            log.error("Profile fetch DB error email={} correlationId={} message={}",
                    principal.email(), correlationId, ex.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to fetch profile");
        }

        if (user == null) {
            log.warn("Profile not found email={} correlationId={}", principal.email(), correlationId);
            return error(HttpStatus.NOT_FOUND, httpRequest, correlationId,
                    "USER_NOT_FOUND", "user profile not found");
        }

        log.info("Profile fetch successful email={} correlationId={}", principal.email(), correlationId);
        return ResponseEntity.ok(new UserProfileResponse(user.userId(), user.name(), user.email(), user.phone()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody ProfileUpdateRequest request,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        log.info("Profile update request received path={} correlationId={}", httpRequest.getRequestURI(), correlationId);
        KeycloakPrincipal principal;
        principal = resolvePrincipal(jwt);
        if (principal == null || isBlank(principal.email())) {
            log.warn("Profile update unauthorized correlationId={}", correlationId);
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!hasExactlyRole(principal, "customer")) {
            return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                    "ACCESS_DENIED", "customer role only is required");
        }

        try (Connection conn = openConnection()) {
            UserRecord existing = findUserByEmail(conn, principal.email().toLowerCase());
            if (existing == null) {
                log.warn("Profile update user not found email={} correlationId={}", principal.email(), correlationId);
                return error(HttpStatus.NOT_FOUND, httpRequest, correlationId,
                        "USER_NOT_FOUND", "user profile not found");
            }

            String updatedName = request == null || isBlank(request.getName()) ? existing.name() : request.getName();
            String updatedPhone = request == null || isBlank(request.getPhone()) ? existing.phone() : request.getPhone();

            String sql = "UPDATE " + userTable() + " SET name = ?, phone = ?, updated_at = NOW() WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, updatedName);
                stmt.setString(2, updatedPhone);
                stmt.setString(3, existing.userId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            log.error("Profile update DB error email={} correlationId={} message={}",
                    principal.email(), correlationId, ex.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to update profile");
        }

        log.info("Profile update successful email={} correlationId={}", principal.email(), correlationId);
        return ResponseEntity.ok(new MessageResponse("profile updated"));
    }

    @DeleteMapping("/profile")
    public ResponseEntity<?> deleteProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        log.info("Profile delete request received path={} correlationId={}", httpRequest.getRequestURI(), correlationId);
        KeycloakPrincipal principal;
        principal = resolvePrincipal(jwt);
        if (principal == null || isBlank(principal.email())) {
            log.warn("Profile delete unauthorized correlationId={}", correlationId);
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!hasExactlyRole(principal, "customer")) {
            return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                    "ACCESS_DENIED", "customer role only is required");
        }

        try (Connection conn = openConnection()) {
            UserRecord existing = findUserByEmail(conn, principal.email().toLowerCase(Locale.ROOT));
            if (existing == null) {
                log.warn("Profile delete user not found email={} correlationId={}", principal.email(), correlationId);
                return error(HttpStatus.NOT_FOUND, httpRequest, correlationId,
                        "USER_NOT_FOUND", "user profile not found");
            }
            deleteKeycloakUser(existing.userId(), correlationId);
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + userTable() + " WHERE user_id = ?")) {
                stmt.setString(1, existing.userId());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            log.error("Profile delete DB error email={} correlationId={} message={}",
                    principal.email(), correlationId, ex.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to delete profile");
        } catch (Exception ex) {
            log.error("Profile delete keycloak error email={} correlationId={} message={}",
                    principal.email(), correlationId, ex.getMessage());
            return error(HttpStatus.SERVICE_UNAVAILABLE, httpRequest, correlationId,
                    "KEYCLOAK_UNAVAILABLE", "failed to delete user in keycloak");
        }

        log.info("Profile delete successful email={} correlationId={}", principal.email(), correlationId);
        return ResponseEntity.ok(new MessageResponse("profile deleted"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        log.info("Logout request received path={} correlationId={}", httpRequest.getRequestURI(), correlationId);
        if (jwt == null) {
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (request == null || isBlank(request.getRefreshToken())) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "refresh_token is required");
        }
        try {
            revokeToken(request.getRefreshToken(), "refresh_token");
            if (!isBlank(request.getAccessToken())) {
                revokeToken(request.getAccessToken(), "access_token");
            }
            endSession(request.getRefreshToken());
            log.info("Logout successful correlationId={}", correlationId);
            return ResponseEntity.ok(new MessageResponse("logged out"));
        } catch (Exception ex) {
            log.error("Logout failed keycloak error correlationId={} message={}", correlationId, ex.getMessage());
            return error(HttpStatus.SERVICE_UNAVAILABLE, httpRequest, correlationId,
                    "KEYCLOAK_UNAVAILABLE", "failed to revoke refresh token");
        }
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        if (request == null || isBlank(request.getRefreshToken())) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "refresh_token is required");
        }
        try {
            String form = "grant_type=refresh_token"
                    + "&client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                    + "&refresh_token=" + urlEncode(request.getRefreshToken());
            if (!isBlank(authProperties.getRhbk().getClientSecret())) {
                form = form + "&client_secret=" + urlEncode(authProperties.getRhbk().getClientSecret());
            }

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create(authProperties.getRhbk().getTokenUrl()))
                    .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> tokenResponse = HTTP_CLIENT.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300) {
                KeycloakTokenError tokenError = parseKeycloakTokenError(tokenResponse.body());
                return mapLoginFailureResponse(tokenResponse.statusCode(), tokenError, httpRequest, correlationId);
            }

            String body = tokenResponse.body();
            String accessToken = extractStringField(body, "access_token");
            String tokenType = extractStringField(body, "token_type");
            int expiresIn = extractIntField(body, "expires_in", 3600);
            String refreshedToken = extractStringField(body, "refresh_token");
            String userId = extractJwtClaim(accessToken, "sub");
            if (isBlank(userId)) {
                userId = "unknown";
            }
            String responseTokenType = isBlank(tokenType) ? TOKEN_TYPE_BEARER : tokenType;
            return ResponseEntity.ok(new AuthResponse(accessToken, responseTokenType, expiresIn, userId, refreshedToken));
        } catch (Exception ex) {
            log.error("Refresh token failed correlationId={} message={}", correlationId, ex.getMessage());
            return error(HttpStatus.SERVICE_UNAVAILABLE, httpRequest, correlationId,
                    "KEYCLOAK_UNAVAILABLE", "failed to refresh access token");
        }
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> adminGetUserByEmail(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        KeycloakPrincipal principal;
        principal = resolvePrincipal(jwt);
        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!hasExactlyRole(principal, "admin")) {
            return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                    "ACCESS_DENIED", "admin role only is required");
        }
        if (isBlank(email)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "email is required");
        }

        try (Connection conn = openConnection()) {
            UserRecord user = findUserByEmail(conn, email.toLowerCase(Locale.ROOT));
            if (user == null) {
                return error(HttpStatus.NOT_FOUND, httpRequest, correlationId,
                        "USER_NOT_FOUND", "user profile not found");
            }
            return ResponseEntity.ok(new UserProfileResponse(user.userId(), user.name(), user.email(), user.phone()));
        } catch (SQLException ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to fetch profile");
        }
    }

    @PostMapping("/admin/users")
    public ResponseEntity<?> adminCreateUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestBody AdminUserRequest request,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        KeycloakPrincipal principal;
        principal = resolvePrincipal(jwt);
        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!hasExactlyRole(principal, "admin")) {
            return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                    "ACCESS_DENIED", "admin role only is required");
        }
        if (request == null || isBlank(request.getName()) || isBlank(request.getUsername())
                || isBlank(request.getEmail()) || isBlank(request.getPassword()) || isBlank(request.getPhone())) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "name, username, email, password and phone are required");
        }

        String role = normalizeRole(request.getRole());
        if (isBlank(role)) {
            role = "customer";
        }
        if (ADMIN_ROLE.equals(role)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_ROLE", "admin role cannot be assigned via this endpoint");
        }
        if (!SUPPORTED_USER_ROLES.contains(role)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_ROLE", "unsupported role");
        }

        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        String phone = request.getPhone().trim();
        if (username.equalsIgnoreCase(email)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "username cannot be same as email");
        }
        if (!isValidUsername(username)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "username must be 4-32 chars and contain only letters, digits, ., _, -");
        }
        if (!isValidEmail(email)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "email format is invalid");
        }
        if (!isStrongPassword(request.getPassword())) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "password must be 8+ chars with upper, lower, digit and special character");
        }
        if (!isValidPhone(phone)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "phone must contain 10 to 15 digits");
        }
        String keycloakUserId;

        try (Connection conn = openConnection()) {
            if (findUserByEmail(conn, email) != null) {
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "USER_ALREADY_EXISTS", "user already exists");
            }
            if (findUserByPhone(conn, phone) != null) {
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "PHONE_ALREADY_EXISTS", "phone already exists");
            }
        } catch (SQLException ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to create user");
        }

        User user = new User();
        user.setName(request.getName());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());

        try {
            if (keycloakUserExistsByUsername(username)) {
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "USERNAME_ALREADY_EXISTS", "username already exists");
            }
            if (keycloakUserExistsByEmail(email)) {
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "USER_ALREADY_EXISTS", "user already exists");
            }
            keycloakUserId = createKeycloakUser(user, username, email, role, correlationId);
        } catch (KeycloakConflictException ex) {
            return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                    "USER_ALREADY_EXISTS", "user already exists");
        } catch (Exception ex) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, httpRequest, correlationId,
                    "KEYCLOAK_UNAVAILABLE", "failed to create user in keycloak");
        }

        try (Connection conn = openConnection()) {
            String sql = "INSERT INTO " + userTable() + " (user_id, name, username, email, phone) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, keycloakUserId);
                stmt.setString(2, request.getName());
                stmt.setString(3, username);
                stmt.setString(4, email);
                stmt.setString(5, phone);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            try {
                deleteKeycloakUser(keycloakUserId, correlationId);
            } catch (Exception ignore) {
            }
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to create user");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(keycloakUserId, "created"));
    }

    @PutMapping("/admin/users")
    public ResponseEntity<?> adminUpdateUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            @RequestBody AdminUserUpdateRequest request,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        KeycloakPrincipal principal;
        principal = resolvePrincipal(jwt);
        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!hasExactlyRole(principal, "admin")) {
            return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                    "ACCESS_DENIED", "admin role only is required");
        }
        if (isBlank(email)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "email is required");
        }

        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        try (Connection conn = openConnection()) {
            UserRecord existing = findUserByEmail(conn, normalizedEmail);
            if (existing == null) {
                return error(HttpStatus.NOT_FOUND, httpRequest, correlationId,
                        "USER_NOT_FOUND", "user profile not found");
            }
            String newName = request == null || isBlank(request.getName()) ? existing.name() : request.getName();
            String newPhone = request == null || isBlank(request.getPhone()) ? existing.phone() : request.getPhone();
            if (!isValidPhone(newPhone)) {
                return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                        "INVALID_REQUEST", "phone must contain 10 to 15 digits");
            }
            UserRecord existingWithPhone = findUserByPhone(conn, newPhone);
            if (existingWithPhone != null && !existingWithPhone.userId().equals(existing.userId())) {
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "PHONE_ALREADY_EXISTS", "phone already exists");
            }
            String sql = "UPDATE " + userTable() + " SET name = ?, phone = ?, updated_at = NOW() WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newName);
                stmt.setString(2, newPhone);
                stmt.setString(3, existing.userId());
                stmt.executeUpdate();
            }

            String role = normalizeRole(request == null ? null : request.getRole());
            if (!isBlank(role)) {
                if (ADMIN_ROLE.equals(role)) {
                    return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                            "INVALID_ROLE", "admin role cannot be assigned via this endpoint");
                }
                if (!SUPPORTED_USER_ROLES.contains(role)) {
                    return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                            "INVALID_ROLE", "unsupported role");
                }
                assignRealmRoleToUser(existing.userId(), role);
            }
            return ResponseEntity.ok(new MessageResponse("user updated"));
        } catch (SQLException ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to update user");
        } catch (Exception ex) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, httpRequest, correlationId,
                    "KEYCLOAK_UNAVAILABLE", "failed to update user role in keycloak");
        }
    }

    @DeleteMapping("/admin/users")
    public ResponseEntity<?> adminDeleteUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        KeycloakPrincipal principal;
        principal = resolvePrincipal(jwt);
        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!hasExactlyRole(principal, "admin")) {
            return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                    "ACCESS_DENIED", "admin role only is required");
        }
        if (isBlank(email)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "email is required");
        }

        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        try (Connection conn = openConnection()) {
            UserRecord existing = findUserByEmail(conn, normalizedEmail);
            if (existing == null) {
                return error(HttpStatus.NOT_FOUND, httpRequest, correlationId,
                        "USER_NOT_FOUND", "user profile not found");
            }
            deleteKeycloakUser(existing.userId(), correlationId);
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM " + userTable() + " WHERE user_id = ?")) {
                stmt.setString(1, existing.userId());
                stmt.executeUpdate();
            }
            return ResponseEntity.ok(new MessageResponse("user deleted"));
        } catch (SQLException ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to delete user");
        } catch (Exception ex) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, httpRequest, correlationId,
                    "KEYCLOAK_UNAVAILABLE", "failed to delete user in keycloak");
        }
    }

    @PutMapping("/support/users")
    public ResponseEntity<?> supportUpdateUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            @RequestBody ProfileUpdateRequest request,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        KeycloakPrincipal principal;
        principal = resolvePrincipal(jwt);
        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!hasExactlyOneOfRoles(principal, Set.of("support_agent", "admin"))) {
            return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                    "ACCESS_DENIED", "exactly one of support_agent or admin role is required");
        }
        if (isBlank(email)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "email is required");
        }
        try (Connection conn = openConnection()) {
            UserRecord existing = findUserByEmail(conn, email.toLowerCase(Locale.ROOT));
            if (existing == null) {
                return error(HttpStatus.NOT_FOUND, httpRequest, correlationId,
                        "USER_NOT_FOUND", "user profile not found");
            }
            String newName = request == null || isBlank(request.getName()) ? existing.name() : request.getName();
            String newPhone = request == null || isBlank(request.getPhone()) ? existing.phone() : request.getPhone();
            if (!isValidPhone(newPhone)) {
                return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                        "INVALID_REQUEST", "phone must contain 10 to 15 digits");
            }
            UserRecord existingWithPhone = findUserByPhone(conn, newPhone);
            if (existingWithPhone != null && !existingWithPhone.userId().equals(existing.userId())) {
                return error(HttpStatus.CONFLICT, httpRequest, correlationId,
                        "PHONE_ALREADY_EXISTS", "phone already exists");
            }
            String sql = "UPDATE " + userTable() + " SET name = ?, phone = ?, updated_at = NOW() WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newName);
                stmt.setString(2, newPhone);
                stmt.setString(3, existing.userId());
                stmt.executeUpdate();
            }
            return ResponseEntity.ok(new MessageResponse("user updated by support"));
        } catch (SQLException ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to update user");
        }
    }

    @GetMapping("/airline/users")
    public ResponseEntity<?> airlineViewUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestParam("email") String email,
            HttpServletRequest httpRequest) {
        correlationId = resolveCorrelationId(correlationId, httpRequest);
        KeycloakPrincipal principal;
        principal = resolvePrincipal(jwt);
        if (principal == null) {
            return error(HttpStatus.UNAUTHORIZED, httpRequest, correlationId,
                    "AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!hasExactlyOneOfRoles(principal, Set.of("airline_ops", "support_agent", "admin"))) {
            return error(HttpStatus.FORBIDDEN, httpRequest, correlationId,
                    "ACCESS_DENIED", "exactly one of airline_ops, support_agent, or admin role is required");
        }
        if (isBlank(email)) {
            return error(HttpStatus.BAD_REQUEST, httpRequest, correlationId,
                    "INVALID_REQUEST", "email is required");
        }
        try (Connection conn = openConnection()) {
            UserRecord user = findUserByEmail(conn, email.toLowerCase(Locale.ROOT));
            if (user == null) {
                return error(HttpStatus.NOT_FOUND, httpRequest, correlationId,
                        "USER_NOT_FOUND", "user profile not found");
            }
            return ResponseEntity.ok(new UserProfileResponse(user.userId(), user.name(), user.email(), user.phone()));
        } catch (SQLException ex) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, httpRequest, correlationId,
                    "DB_ERROR", "failed to fetch profile");
        }
    }

    private KeycloakPrincipal resolvePrincipal(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String email = extractEmailFromJwt(jwt);
        return new KeycloakPrincipal(jwt.getSubject(), email, extractRolesFromJwt(jwt));
    }

    private String createKeycloakUser(User request, String username, String email, String role, String correlationId) throws Exception {
        String adminToken = fetchAdminAccessToken();
        String firstName = extractFirstName(request.getName());
        String lastName = extractLastName(request.getName());
        String payload = "{"
                + "\"username\":\"" + jsonEscape(username) + "\","
                + "\"email\":\"" + jsonEscape(email) + "\","
                + "\"enabled\":true,"
                + "\"emailVerified\":true,"
                + "\"firstName\":\"" + jsonEscape(firstName) + "\","
                + "\"lastName\":\"" + jsonEscape(lastName) + "\","
                + "\"attributes\":{\"phone\":[\"" + jsonEscape(defaultString(request.getPhone())) + "\"]},"
                + "\"credentials\":[{\"type\":\"password\",\"value\":\"" + jsonEscape(request.getPassword()) + "\",\"temporary\":false}]"
                + "}";

        HttpRequest createUserRequest = HttpRequest.newBuilder()
                .uri(URI.create(authProperties.getRhbk().getUsersUrl()))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(createUserRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 409) {
            throw new KeycloakConflictException();
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak create user failed status=" + response.statusCode());
        }

        String location = response.headers().firstValue("Location").orElse("");
        Matcher matcher = LAST_PATH_SEGMENT_PATTERN.matcher(location);
        if (matcher.matches()) {
            String keycloakUserId = matcher.group(1);
            markKeycloakUserReadyForLogin(keycloakUserId);
            assignRealmRoleToUser(keycloakUserId, role);
            return keycloakUserId;
        }
        String fallbackUserId = "kc_" + UUID.randomUUID();
        markKeycloakUserReadyForLogin(fallbackUserId);
        assignRealmRoleToUser(fallbackUserId, role);
        return fallbackUserId;
    }

    private void deleteKeycloakUser(String keycloakUserId, String correlationId) throws Exception {
        if (isBlank(keycloakUserId)) {
            return;
        }
        String adminToken = fetchAdminAccessToken();
        HttpRequest deleteUserRequest = HttpRequest.newBuilder()
                .uri(URI.create(authProperties.getRhbk().getUsersUrl() + "/" + urlEncode(keycloakUserId)))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + adminToken)
                .DELETE()
                .build();
        HttpResponse<Void> deleteResponse = HTTP_CLIENT.send(deleteUserRequest, HttpResponse.BodyHandlers.discarding());
        log.info("Keycloak rollback delete user status={} keycloakUserId={} correlationId={}",
                deleteResponse.statusCode(), keycloakUserId, correlationId);
    }

    private void revokeToken(String token, String tokenTypeHint) throws Exception {
        String revocationUrl = authProperties.getRhbk().getRevocationUrl();
        if (isBlank(revocationUrl)) {
            return;
        }
        String form = "client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                + "&token=" + urlEncode(token)
                + "&token_type_hint=" + urlEncode(tokenTypeHint);
        if (!isBlank(authProperties.getRhbk().getClientSecret())) {
            form = form + "&client_secret=" + urlEncode(authProperties.getRhbk().getClientSecret());
        }

        HttpRequest revokeRequest = HttpRequest.newBuilder()
                .uri(URI.create(revocationUrl))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(revokeRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak revoke token failed status=" + response.statusCode());
        }
    }

    private void endSession(String refreshToken) throws Exception {
        String form = "client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                + "&refresh_token=" + urlEncode(refreshToken);
        if (!isBlank(authProperties.getRhbk().getClientSecret())) {
            form = form + "&client_secret=" + urlEncode(authProperties.getRhbk().getClientSecret());
        }

        HttpRequest logoutRequest = HttpRequest.newBuilder()
                .uri(URI.create(authProperties.getRhbk().getLogoutUrl()))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(logoutRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak logout failed status=" + response.statusCode());
        }
    }

    private String fetchAdminAccessToken() throws Exception {
        String adminClientId = isBlank(authProperties.getRhbk().getAdminClientId())
                ? authProperties.getRhbk().getClientId()
                : authProperties.getRhbk().getAdminClientId();
        String adminClientSecret = authProperties.getRhbk().getAdminClientSecret();
        if (isBlank(adminClientSecret)) {
            adminClientSecret = authProperties.getRhbk().getClientSecret();
        }

        String form = "grant_type=client_credentials"
                + "&client_id=" + urlEncode(adminClientId);
        if (!isBlank(adminClientSecret)) {
            form = form + "&client_secret=" + urlEncode(adminClientSecret);
        }

        HttpRequest adminTokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(authProperties.getRhbk().getAdminTokenUrl()))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(adminTokenRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak admin token failed status=" + response.statusCode());
        }

        String accessToken = extractStringField(response.body(), "access_token");
        if (isBlank(accessToken)) {
            throw new IllegalStateException("keycloak admin token missing access_token");
        }
        return accessToken;
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                authProperties.getDb().getUrl(),
                authProperties.getDb().getUsername(),
                authProperties.getDb().getPassword());
    }

    private UserRecord findUserByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT user_id, name, email, phone FROM " + userTable() + " WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new UserRecord(
                        rs.getString("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"));
            }
        }
    }

    private UserRecord findUserByPhone(Connection conn, String phone) throws SQLException {
        String sql = "SELECT user_id, name, email, phone FROM " + userTable() + " WHERE phone = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new UserRecord(
                        rs.getString("user_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"));
            }
        }
    }

    private String userTable() {
        return authProperties.getDb().getUserTable();
    }

    private String resolveCorrelationId(String correlationId, HttpServletRequest request) {
        if (!isBlank(correlationId)) {
            return correlationId;
        }
        Object fromRequest = request.getAttribute("x-correlation-id");
        if (fromRequest instanceof String value && !value.isBlank()) {
            return value;
        }
        return UUID.randomUUID().toString();
    }

    private String randomUrlToken(int size) {
        byte[] bytes = new byte[size];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Base64Url(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to build code challenge", ex);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String extractFirstName(String fullName) {
        if (isBlank(fullName)) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    private String extractLastName(String fullName) {
        if (isBlank(fullName)) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length < 2) {
            return parts[0];
        }
        return String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
    }

    private String normalizeRole(String role) {
        return role == null ? null : role.trim().toLowerCase(Locale.ROOT);
    }

    private boolean keycloakUserExistsByUsername(String username) throws Exception {
        String adminToken = fetchAdminAccessToken();
        String url = authProperties.getRhbk().getUsersUrl() + "?username=" + urlEncode(username) + "&exact=true";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak user search failed status=" + response.statusCode());
        }
        return jsonArrayHasObjects(response.body());
    }

    private boolean keycloakUserExistsByEmail(String email) throws Exception {
        String adminToken = fetchAdminAccessToken();
        String url = authProperties.getRhbk().getUsersUrl() + "?email=" + urlEncode(email);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak user search failed status=" + response.statusCode());
        }
        return jsonArrayHasObjects(response.body());
    }

    private void assignRealmRoleToUser(String keycloakUserId, String role) throws Exception {
        if (isBlank(keycloakUserId) || isBlank(role)) {
            return;
        }
        String adminToken = fetchAdminAccessToken();
        String roleMappingsUrl = authProperties.getRhbk().getUsersUrl() + "/" + urlEncode(keycloakUserId) + "/role-mappings/realm";

        String clearPayload = buildRolePayload(fetchSupportedRoleRepresentations(adminToken));
        if (!isBlank(clearPayload) && !"[]".equals(clearPayload)) {
            HttpRequest clearRolesRequest = HttpRequest.newBuilder()
                    .uri(URI.create(roleMappingsUrl))
                    .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(clearPayload))
                    .build();
            HttpResponse<Void> clearRolesResponse = HTTP_CLIENT.send(clearRolesRequest, HttpResponse.BodyHandlers.discarding());
            if (clearRolesResponse.statusCode() < 200 || clearRolesResponse.statusCode() >= 300) {
                throw new IllegalStateException("keycloak role clear failed status=" + clearRolesResponse.statusCode());
            }
        }

        String payload = buildRolePayload(List.of(fetchRoleRepresentation(adminToken, role)));
        HttpRequest assignRoleRequest = HttpRequest.newBuilder()
                .uri(URI.create(roleMappingsUrl))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<Void> assignRoleResponse = HTTP_CLIENT.send(assignRoleRequest, HttpResponse.BodyHandlers.discarding());
        if (assignRoleResponse.statusCode() < 200 || assignRoleResponse.statusCode() >= 300) {
            throw new IllegalStateException("keycloak role assignment failed status=" + assignRoleResponse.statusCode());
        }
    }

    private List<Map<String, String>> fetchSupportedRoleRepresentations(String adminToken) throws Exception {
        List<Map<String, String>> roles = new java.util.ArrayList<>();
        for (String role : SUPPORTED_USER_ROLES) {
            roles.add(fetchRoleRepresentation(adminToken, role));
        }
        return roles;
    }

    private Map<String, String> fetchRoleRepresentation(String adminToken, String role) throws Exception {
        String adminRealmBaseUrl = authProperties.getRhbk().getUsersUrl().replaceAll("/users$", "");
        HttpRequest getRoleRequest = HttpRequest.newBuilder()
                .uri(URI.create(adminRealmBaseUrl + "/roles/" + urlEncode(role)))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();
        HttpResponse<String> getRoleResponse = HTTP_CLIENT.send(getRoleRequest, HttpResponse.BodyHandlers.ofString());
        if (getRoleResponse.statusCode() < 200 || getRoleResponse.statusCode() >= 300) {
            throw new IllegalStateException("keycloak role fetch failed status=" + getRoleResponse.statusCode());
        }

        String roleBody = getRoleResponse.body();
        String roleId = extractStringField(roleBody, "id");
        String roleName = extractStringField(roleBody, "name");
        if (isBlank(roleId) || isBlank(roleName)) {
            throw new IllegalStateException("keycloak role payload invalid");
        }
        return Map.of("id", roleId, "name", roleName);
    }

    private String buildRolePayload(List<Map<String, String>> roles) {
        StringBuilder payload = new StringBuilder("[");
        for (int i = 0; i < roles.size(); i++) {
            Map<String, String> role = roles.get(i);
            if (i > 0) {
                payload.append(",");
            }
            payload.append("{\"id\":\"")
                    .append(jsonEscape(role.getOrDefault("id", "")))
                    .append("\",\"name\":\"")
                    .append(jsonEscape(role.getOrDefault("name", "")))
                    .append("\"}");
        }
        payload.append("]");
        return payload.toString();
    }

    private void markKeycloakUserReadyForLogin(String keycloakUserId) throws Exception {
        if (isBlank(keycloakUserId)) {
            return;
        }
        String adminToken = fetchAdminAccessToken();
        String payload = "{"
                + "\"enabled\":true,"
                + "\"emailVerified\":true,"
                + "\"requiredActions\":[]"
                + "}";
        HttpRequest updateUserRequest = HttpRequest.newBuilder()
                .uri(URI.create(authProperties.getRhbk().getUsersUrl() + "/" + urlEncode(keycloakUserId)))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .method("PUT", HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<Void> updateUserResponse = HTTP_CLIENT.send(updateUserRequest, HttpResponse.BodyHandlers.discarding());
        if (updateUserResponse.statusCode() < 200 || updateUserResponse.statusCode() >= 300) {
            throw new IllegalStateException("keycloak user update failed status=" + updateUserResponse.statusCode());
        }
    }

    private Set<String> extractRolesFromAccessToken(String token) {
        Set<String> roles = new HashSet<>();
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length < 2) {
            return roles;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = JSON_PARSER.parseMap(payload);
            roles.addAll(extractRolesFromClaims(claims));
        } catch (Exception ex) {
            return roles;
        }
        return roles;
    }

    private Set<String> extractRolesFromJwt(Jwt jwt) {
        Set<String> roles = new HashSet<>();
        if (jwt == null) {
            return roles;
        }
        Object realmAccessObj = jwt.getClaim("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> roleList) {
                for (Object role : roleList) {
                    if (role != null) {
                        roles.add(role.toString());
                    }
                }
            }
        }

        Object resourceAccessObj = jwt.getClaim("resource_access");
        if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
            Object clientAccessObj = resourceAccess.get(authProperties.getRhbk().getClientId());
            if (clientAccessObj instanceof Map<?, ?> clientAccess) {
                Object rolesObj = clientAccess.get("roles");
                if (rolesObj instanceof List<?> roleList) {
                    for (Object role : roleList) {
                        if (role != null) {
                            roles.add(role.toString());
                        }
                    }
                }
            }
        }
        return roles;
    }

    private Set<String> extractRolesFromClaims(Map<String, Object> claims) {
        Set<String> roles = new HashSet<>();
        if (claims == null) {
            return roles;
        }
        Object realmAccessObj = claims.get("realm_access");
        if (realmAccessObj instanceof Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> roleList) {
                for (Object role : roleList) {
                    if (role != null) {
                        roles.add(role.toString());
                    }
                }
            }
        }

        Object resourceAccessObj = claims.get("resource_access");
        if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
            Object clientAccessObj = resourceAccess.get(authProperties.getRhbk().getClientId());
            if (clientAccessObj instanceof Map<?, ?> clientAccess) {
                Object rolesObj = clientAccess.get("roles");
                if (rolesObj instanceof List<?> roleList) {
                    for (Object role : roleList) {
                        if (role != null) {
                            roles.add(role.toString());
                        }
                    }
                }
            }
        }
        return roles;
    }

    private String extractEmailFromJwt(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String email = jwt.getClaimAsString("email");
        if (!isBlank(email)) {
            return email;
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (!isBlank(preferredUsername) && preferredUsername.contains("@")) {
            return preferredUsername;
        }
        return null;
    }

    private String extractJwtClaim(String token, String field) {
        if (isBlank(token) || isBlank(field)) {
            return null;
        }
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length < 2) {
            return null;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
            return extractStringField(payload, field);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private KeycloakTokenError parseKeycloakTokenError(String jsonBody) {
        String code = extractStringField(jsonBody, "error");
        String description = extractStringField(jsonBody, "error_description");
        return new KeycloakTokenError(isBlank(code) ? "unknown_error" : code, isBlank(description) ? "unknown" : description);
    }

    private ResponseEntity<ErrorResponse> mapLoginFailureResponse(
            int statusCode,
            KeycloakTokenError tokenError,
            HttpServletRequest request,
            String correlationId) {
        String description = tokenError.description().toLowerCase(Locale.ROOT);
        if ("invalid_grant".equals(tokenError.code()) && description.contains("not fully set up")) {
            return error(HttpStatus.FORBIDDEN, request, correlationId,
                    "ACCOUNT_NOT_READY", "account is not fully set up");
        }
        if ("invalid_grant".equals(tokenError.code()) && description.contains("invalid user credentials")) {
            return error(HttpStatus.UNAUTHORIZED, request, correlationId,
                    "INVALID_CREDENTIALS", "invalid credentials");
        }
        if ("invalid_grant".equals(tokenError.code()) && description.contains("code")) {
            return error(HttpStatus.UNAUTHORIZED, request, correlationId,
                    "INVALID_AUTHORIZATION_CODE", "authorization code is invalid or expired");
        }
        if ("invalid_client".equals(tokenError.code())) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, request, correlationId,
                    "AUTH_CLIENT_INVALID", "keycloak client configuration is invalid");
        }
        if (statusCode == 401) {
            return error(HttpStatus.UNAUTHORIZED, request, correlationId,
                    "INVALID_CREDENTIALS", "invalid credentials");
        }
        return error(HttpStatus.SERVICE_UNAVAILABLE, request, correlationId,
                "KEYCLOAK_AUTH_ERROR", "keycloak rejected login request");
    }

    private boolean hasExactlyRole(KeycloakPrincipal principal, String requiredRole) {
        Set<String> supportedRoles = supportedRoles(principal.roles());
        return supportedRoles.size() == 1 && supportedRoles.contains(requiredRole);
    }

    private boolean hasExactlyOneOfRoles(KeycloakPrincipal principal, Set<String> requiredRoles) {
        Set<String> supportedRoles = supportedRoles(principal.roles());
        if (supportedRoles.size() != 1) {
            return false;
        }
        String role = supportedRoles.iterator().next();
        return requiredRoles.contains(role);
    }

    private boolean hasExactlyOneSupportedRole(Set<String> roles) {
        return supportedRoles(roles).size() == 1;
    }

    private Set<String> supportedRoles(Set<String> roles) {
        Set<String> result = new HashSet<>();
        for (String role : roles) {
            if (SUPPORTED_USER_ROLES.contains(role)) {
                result.add(role);
            }
        }
        return result;
    }

    private boolean isValidEmail(String email) {
        return !isBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidUsername(String username) {
        return !isBlank(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    private boolean isValidPhone(String phone) {
        return !isBlank(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    private boolean isStrongPassword(String password) {
        if (isBlank(password) || password.length() < 8) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private boolean jsonArrayHasObjects(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("[") && trimmed.length() > 2 && trimmed.contains("{");
    }

    private String extractStringField(String json, String field) {
        Matcher matcher = JSON_STRING_FIELD_PATTERN.matcher(json == null ? "" : json);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return null;
    }

    private int extractIntField(String json, String field, int fallback) {
        Matcher matcher = JSON_NUMBER_FIELD_PATTERN.matcher(json == null ? "" : json);
        while (matcher.find()) {
            if (field.equals(matcher.group(1))) {
                try {
                    return Integer.parseInt(matcher.group(2));
                } catch (NumberFormatException ex) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            HttpServletRequest request,
            String correlationId,
            String code,
            String message) {
        String resolvedCorrelationId = resolveCorrelationId(correlationId, request);
        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                request.getRequestURI(),
                code,
                message,
                resolvedCorrelationId);
        return ResponseEntity.status(status).body(body);
    }

    private record UserRecord(String userId, String name, String email, String phone) {
    }

    private record KeycloakPrincipal(String subject, String email, Set<String> roles) {
    }

    private record KeycloakTokenError(String code, String description) {
    }

    private static class KeycloakConflictException extends RuntimeException {
    }
}

