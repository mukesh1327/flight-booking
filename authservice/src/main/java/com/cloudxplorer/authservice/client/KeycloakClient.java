package com.cloudxplorer.authservice.client;

import com.cloudxplorer.authservice.config.AuthProperties;
import com.cloudxplorer.authservice.util.RoleUtils;
import com.cloudxplorer.authservice.util.ValidationUtils;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class KeycloakClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();
    private static final Pattern JSON_STRING_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern JSON_NUMBER_FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");
    private static final Pattern LAST_PATH_SEGMENT_PATTERN = Pattern.compile(".*/([^/]+)$");

    private final AuthProperties authProperties;

    public KeycloakClient(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String buildAuthorizeUrl(String redirectUri, String scope, String state, String codeChallenge) {
        return authProperties.getRhbk().getAuthorizationUrl()
                + "?response_type=code"
                + "&client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode(scope)
                + "&state=" + urlEncode(state)
                + "&code_challenge=" + urlEncode(codeChallenge)
                + "&code_challenge_method=S256";
    }

    public TokenResult exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri) throws Exception {
        String form = "grant_type=authorization_code"
                + "&client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                + "&code=" + urlEncode(code)
                + "&code_verifier=" + urlEncode(codeVerifier)
                + "&redirect_uri=" + urlEncode(redirectUri);
        if (!ValidationUtils.isBlank(authProperties.getRhbk().getClientSecret())) {
            form = form + "&client_secret=" + urlEncode(authProperties.getRhbk().getClientSecret());
        }
        return requestToken(form);
    }

    public TokenResult refreshToken(String refreshToken) throws Exception {
        String form = "grant_type=refresh_token"
                + "&client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                + "&refresh_token=" + urlEncode(refreshToken);
        if (!ValidationUtils.isBlank(authProperties.getRhbk().getClientSecret())) {
            form = form + "&client_secret=" + urlEncode(authProperties.getRhbk().getClientSecret());
        }
        return requestToken(form);
    }

    public void revokeToken(String token, String tokenTypeHint) throws Exception {
        if (ValidationUtils.isBlank(authProperties.getRhbk().getRevocationUrl())) {
            return;
        }
        String form = "client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                + "&token=" + urlEncode(token)
                + "&token_type_hint=" + urlEncode(tokenTypeHint);
        if (!ValidationUtils.isBlank(authProperties.getRhbk().getClientSecret())) {
            form = form + "&client_secret=" + urlEncode(authProperties.getRhbk().getClientSecret());
        }
        postForm(authProperties.getRhbk().getRevocationUrl(), form, "keycloak revoke token failed");
    }

    public void endSession(String refreshToken) throws Exception {
        String form = "client_id=" + urlEncode(authProperties.getRhbk().getClientId())
                + "&refresh_token=" + urlEncode(refreshToken);
        if (!ValidationUtils.isBlank(authProperties.getRhbk().getClientSecret())) {
            form = form + "&client_secret=" + urlEncode(authProperties.getRhbk().getClientSecret());
        }
        postForm(authProperties.getRhbk().getLogoutUrl(), form, "keycloak logout failed");
    }

    public boolean userExistsByUsername(String username) throws Exception {
        String token = fetchAdminAccessToken();
        String url = authProperties.getRhbk().getUsersUrl() + "?username=" + urlEncode(username) + "&exact=true";
        HttpResponse<String> response = getWithBearer(url, token);
        return jsonArrayHasObjects(response.body());
    }

    public boolean userExistsByEmail(String email) throws Exception {
        String token = fetchAdminAccessToken();
        String url = authProperties.getRhbk().getUsersUrl() + "?email=" + urlEncode(email);
        HttpResponse<String> response = getWithBearer(url, token);
        return jsonArrayHasObjects(response.body());
    }

    public String createUser(String username, String email, String password, String firstName, String lastName, String phone, String role) throws Exception {
        String adminToken = fetchAdminAccessToken();
        String payload = "{"
                + "\"username\":\"" + jsonEscape(username) + "\","
                + "\"email\":\"" + jsonEscape(email) + "\","
                + "\"enabled\":true,"
                + "\"emailVerified\":true,"
                + "\"firstName\":\"" + jsonEscape(firstName) + "\","
                + "\"lastName\":\"" + jsonEscape(lastName) + "\","
                + "\"attributes\":{\"phone\":[\"" + jsonEscape(phone == null ? "" : phone) + "\"]},"
                + "\"credentials\":[{\"type\":\"password\",\"value\":\"" + jsonEscape(password) + "\",\"temporary\":false}]"
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
        if (!matcher.matches()) {
            throw new IllegalStateException("keycloak create user response missing location user id");
        }
        String keycloakUserId = matcher.group(1);
        markUserReady(keycloakUserId);
        assignExclusiveRealmRole(keycloakUserId, role);
        return keycloakUserId;
    }

    public void deleteUser(String keycloakUserId) throws Exception {
        if (ValidationUtils.isBlank(keycloakUserId)) {
            return;
        }
        String token = fetchAdminAccessToken();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authProperties.getRhbk().getUsersUrl() + "/" + urlEncode(keycloakUserId)))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public void assignExclusiveRealmRole(String keycloakUserId, String role) throws Exception {
        if (ValidationUtils.isBlank(keycloakUserId) || ValidationUtils.isBlank(role)) {
            return;
        }
        String adminToken = fetchAdminAccessToken();
        String roleMappingsUrl = authProperties.getRhbk().getUsersUrl() + "/" + urlEncode(keycloakUserId) + "/role-mappings/realm";

        String clearPayload = buildRolePayload(fetchSupportedRoleRepresentations(adminToken));
        if (!"[]".equals(clearPayload)) {
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

    public KeycloakTokenError parseTokenError(String jsonBody) {
        String code = extractStringField(jsonBody, "error");
        String description = extractStringField(jsonBody, "error_description");
        return new KeycloakTokenError(
                ValidationUtils.isBlank(code) ? "unknown_error" : code,
                ValidationUtils.isBlank(description) ? "unknown" : description);
    }

    private TokenResult requestToken(String form) throws Exception {
        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(authProperties.getRhbk().getTokenUrl()))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new KeycloakTokenException(response.statusCode(), response.body());
        }
        String body = response.body();
        return new TokenResult(
                extractStringField(body, "access_token"),
                extractStringField(body, "token_type"),
                extractIntField(body, "expires_in", 3600),
                extractStringField(body, "refresh_token"));
    }

    private void postForm(String url, String form, String errorPrefix) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(errorPrefix + " status=" + response.statusCode());
        }
    }

    private HttpResponse<String> getWithBearer(String url, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak request failed status=" + response.statusCode());
        }
        return response;
    }

    private String fetchAdminAccessToken() throws Exception {
        String adminClientId = ValidationUtils.isBlank(authProperties.getRhbk().getAdminClientId())
                ? authProperties.getRhbk().getClientId()
                : authProperties.getRhbk().getAdminClientId();
        String adminClientSecret = authProperties.getRhbk().getAdminClientSecret();
        if (ValidationUtils.isBlank(adminClientSecret)) {
            adminClientSecret = authProperties.getRhbk().getClientSecret();
        }
        String form = "grant_type=client_credentials"
                + "&client_id=" + urlEncode(adminClientId);
        if (!ValidationUtils.isBlank(adminClientSecret)) {
            form = form + "&client_secret=" + urlEncode(adminClientSecret);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authProperties.getRhbk().getAdminTokenUrl()))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak admin token failed status=" + response.statusCode());
        }
        String accessToken = extractStringField(response.body(), "access_token");
        if (ValidationUtils.isBlank(accessToken)) {
            throw new IllegalStateException("keycloak admin token missing access_token");
        }
        return accessToken;
    }

    private void markUserReady(String keycloakUserId) throws Exception {
        String token = fetchAdminAccessToken();
        String payload = "{\"enabled\":true,\"emailVerified\":true,\"requiredActions\":[]}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authProperties.getRhbk().getUsersUrl() + "/" + urlEncode(keycloakUserId)))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PUT", HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<Void> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak user update failed status=" + response.statusCode());
        }
    }

    private List<Map<String, String>> fetchSupportedRoleRepresentations(String adminToken) throws Exception {
        List<Map<String, String>> roles = new java.util.ArrayList<>();
        for (String role : RoleUtils.SUPPORTED_USER_ROLES) {
            roles.add(fetchRoleRepresentation(adminToken, role));
        }
        return roles;
    }

    private Map<String, String> fetchRoleRepresentation(String adminToken, String role) throws Exception {
        String adminRealmBaseUrl = authProperties.getRhbk().getUsersUrl().replaceAll("/users$", "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(adminRealmBaseUrl + "/roles/" + urlEncode(role)))
                .timeout(Duration.ofMillis(authProperties.getRhbk().getTimeoutMs()))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("keycloak role fetch failed status=" + response.statusCode());
        }
        String roleId = extractStringField(response.body(), "id");
        String roleName = extractStringField(response.body(), "name");
        if (ValidationUtils.isBlank(roleId) || ValidationUtils.isBlank(roleName)) {
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

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
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

    private boolean jsonArrayHasObjects(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return trimmed.startsWith("[") && trimmed.length() > 2 && trimmed.contains("{");
    }

    public static class KeycloakConflictException extends RuntimeException {
    }

    public static class KeycloakTokenException extends RuntimeException {
        private final int statusCode;
        private final String body;

        public KeycloakTokenException(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }
    }

    public record TokenResult(String accessToken, String tokenType, int expiresIn, String refreshToken) {
    }

    public record KeycloakTokenError(String code, String description) {
    }
}
