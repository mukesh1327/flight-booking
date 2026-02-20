package com.cloudxplorer.authservice.service;

import com.cloudxplorer.authservice.client.KeycloakClient;
import com.cloudxplorer.authservice.config.AuthProperties;
import com.cloudxplorer.authservice.exception.ApiException;
import com.cloudxplorer.authservice.model.*;
import com.cloudxplorer.authservice.repository.UserRepository;
import com.cloudxplorer.authservice.util.RoleUtils;
import com.cloudxplorer.authservice.util.TokenUtils;
import com.cloudxplorer.authservice.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final String ADMIN_ROLE = "admin";

    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final KeycloakClient keycloakClient;
    private final TokenUtils tokenUtils;

    public AuthService(
            AuthProperties authProperties,
            UserRepository userRepository,
            KeycloakClient keycloakClient,
            TokenUtils tokenUtils) {
        this.authProperties = authProperties;
        this.userRepository = userRepository;
        this.keycloakClient = keycloakClient;
        this.tokenUtils = tokenUtils;
    }

    public RegisterResponse register(User request, String correlationId) {
        requireRegisterFields(request);
        String username = normalizeUsername(request.getUsername());
        String email = normalizeEmail(request.getEmail());
        String phone = request.getPhone().trim();
        validateIdentityFields(username, email, request.getPassword(), phone);

        try {
            if (userRepository.findByEmail(email) != null) {
                throw conflict("USER_ALREADY_EXISTS", "user already exists");
            }
            if (userRepository.findByPhone(phone) != null) {
                throw conflict("PHONE_ALREADY_EXISTS", "phone already exists");
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw internal("DB_ERROR", "failed to register user");
        }

        String keycloakUserId;
        try {
            if (keycloakClient.userExistsByUsername(username)) {
                throw conflict("USERNAME_ALREADY_EXISTS", "username already exists");
            }
            if (keycloakClient.userExistsByEmail(email)) {
                throw conflict("USER_ALREADY_EXISTS", "user already exists");
            }
            keycloakUserId = keycloakClient.createUser(
                    username, email, request.getPassword(), extractFirstName(request.getName()), extractLastName(request.getName()), phone, "customer");
        } catch (KeycloakClient.KeycloakConflictException ex) {
            throw conflict("USER_ALREADY_EXISTS", "user already exists");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable("KEYCLOAK_UNAVAILABLE", "failed to create user in keycloak");
        }

        try {
            userRepository.insert(keycloakUserId, request.getName(), username, email, phone);
            return new RegisterResponse(keycloakUserId, "registered");
        } catch (Exception ex) {
            try {
                keycloakClient.deleteUser(keycloakUserId);
            } catch (Exception ignored) {
                log.warn("register rollback keycloak delete failed correlationId={} userId={}", correlationId, keycloakUserId);
            }
            throw internal("DB_ERROR", "failed to register user");
        }
    }

    public Map<String, String> buildAuthorizeUrl(String redirectUri, String scope) {
        String resolvedRedirectUri = ValidationUtils.isBlank(redirectUri)
                ? authProperties.getRhbk().getDefaultRedirectUri()
                : redirectUri;
        String resolvedScope = ValidationUtils.isBlank(scope)
                ? authProperties.getRhbk().getDefaultScope()
                : scope;
        String state = randomUrlToken(24);
        String codeVerifier = randomUrlToken(64);
        String codeChallenge = sha256Base64Url(codeVerifier);
        String authorizationUrl = keycloakClient.buildAuthorizeUrl(resolvedRedirectUri, resolvedScope, state, codeChallenge);
        return Map.of(
                "authorizationUrl", authorizationUrl,
                "state", state,
                "codeVerifier", codeVerifier,
                "redirectUri", resolvedRedirectUri,
                "scope", resolvedScope);
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null || ValidationUtils.isBlank(request.getCode()) || ValidationUtils.isBlank(request.getCodeVerifier())) {
            throw badRequest("INVALID_REQUEST", "authorization code and code_verifier are required");
        }
        String redirectUri = ValidationUtils.isBlank(request.getRedirectUri())
                ? authProperties.getRhbk().getDefaultRedirectUri()
                : request.getRedirectUri();
        try {
            KeycloakClient.TokenResult token = keycloakClient.exchangeAuthorizationCode(
                    request.getCode(), request.getCodeVerifier(), redirectUri);
            return toAuthResponseWithRoleValidation(token);
        } catch (KeycloakClient.KeycloakTokenException ex) {
            throw mapTokenError(ex);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw unavailable("KEYCLOAK_UNAVAILABLE", "failed to connect to keycloak");
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (request == null || ValidationUtils.isBlank(request.getRefreshToken())) {
            throw badRequest("INVALID_REQUEST", "refresh_token is required");
        }
        try {
            KeycloakClient.TokenResult token = keycloakClient.refreshToken(request.getRefreshToken());
            return toAuthResponse(token);
        } catch (KeycloakClient.KeycloakTokenException ex) {
            throw mapTokenError(ex);
        } catch (Exception ex) {
            throw unavailable("KEYCLOAK_UNAVAILABLE", "failed to refresh access token");
        }
    }

    public MessageResponse logout(Jwt jwt, LogoutRequest request) {
        requireAuthenticated(jwt);
        if (request == null || ValidationUtils.isBlank(request.getRefreshToken())) {
            throw badRequest("INVALID_REQUEST", "refresh_token is required");
        }
        try {
            keycloakClient.revokeToken(request.getRefreshToken(), "refresh_token");
            if (!ValidationUtils.isBlank(request.getAccessToken())) {
                keycloakClient.revokeToken(request.getAccessToken(), "access_token");
            }
            keycloakClient.endSession(request.getRefreshToken());
            return new MessageResponse("logged out");
        } catch (Exception ex) {
            throw unavailable("KEYCLOAK_UNAVAILABLE", "failed to revoke refresh token");
        }
    }

    public UserProfileResponse profile(Jwt jwt) {
        PrincipalInfo principal = requireCustomerPrincipal(jwt);
        UserRecord user = getUserByEmailOrThrow(principal.getEmail());
        return toUserProfile(user);
    }

    public MessageResponse updateProfile(Jwt jwt, ProfileUpdateRequest request) {
        PrincipalInfo principal = requireCustomerPrincipal(jwt);
        UserRecord existing = getUserByEmailOrThrow(principal.getEmail());
        String updatedName = request == null || ValidationUtils.isBlank(request.getName()) ? existing.getName() : request.getName();
        String updatedPhone = request == null || ValidationUtils.isBlank(request.getPhone()) ? existing.getPhone() : request.getPhone();
        if (!ValidationUtils.isValidPhone(updatedPhone)) {
            throw badRequest("INVALID_REQUEST", "phone must contain 10 to 15 digits");
        }
        try {
            UserRecord existingWithPhone = userRepository.findByPhone(updatedPhone);
            if (existingWithPhone != null && !existingWithPhone.getUserId().equals(existing.getUserId())) {
                throw conflict("PHONE_ALREADY_EXISTS", "phone already exists");
            }
            userRepository.updateProfile(existing.getUserId(), updatedName, updatedPhone);
            return new MessageResponse("profile updated");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw internal("DB_ERROR", "failed to update profile");
        }
    }

    public MessageResponse deleteProfile(Jwt jwt) {
        PrincipalInfo principal = requireCustomerPrincipal(jwt);
        UserRecord existing = getUserByEmailOrThrow(principal.getEmail());
        try {
            keycloakClient.deleteUser(existing.getUserId());
            userRepository.deleteByUserId(existing.getUserId());
            return new MessageResponse("profile deleted");
        } catch (Exception ex) {
            throw unavailable("KEYCLOAK_UNAVAILABLE", "failed to delete user in keycloak");
        }
    }

    public UserProfileResponse adminGetUser(Jwt jwt, String email) {
        requireExactRole(jwt, ADMIN_ROLE, "admin role only is required");
        if (ValidationUtils.isBlank(email)) {
            throw badRequest("INVALID_REQUEST", "email is required");
        }
        return toUserProfile(getUserByEmailOrThrow(normalizeEmail(email)));
    }

    public RegisterResponse adminCreateUser(Jwt jwt, AdminUserRequest request) {
        requireExactRole(jwt, ADMIN_ROLE, "admin role only is required");
        if (request == null || ValidationUtils.isBlank(request.getName()) || ValidationUtils.isBlank(request.getUsername())
                || ValidationUtils.isBlank(request.getEmail()) || ValidationUtils.isBlank(request.getPassword()) || ValidationUtils.isBlank(request.getPhone())) {
            throw badRequest("INVALID_REQUEST", "name, username, email, password and phone are required");
        }
        String role = normalizeRole(request.getRole());
        if (ValidationUtils.isBlank(role)) {
            role = "customer";
        }
        if (ADMIN_ROLE.equals(role)) {
            throw badRequest("INVALID_ROLE", "admin role cannot be assigned via this endpoint");
        }
        if (!RoleUtils.SUPPORTED_USER_ROLES.contains(role)) {
            throw badRequest("INVALID_ROLE", "unsupported role");
        }
        String email = normalizeEmail(request.getEmail());
        String username = normalizeUsername(request.getUsername());
        String phone = request.getPhone().trim();
        validateIdentityFields(username, email, request.getPassword(), phone);

        try {
            if (userRepository.findByEmail(email) != null) {
                throw conflict("USER_ALREADY_EXISTS", "user already exists");
            }
            if (userRepository.findByPhone(phone) != null) {
                throw conflict("PHONE_ALREADY_EXISTS", "phone already exists");
            }
            if (keycloakClient.userExistsByUsername(username)) {
                throw conflict("USERNAME_ALREADY_EXISTS", "username already exists");
            }
            if (keycloakClient.userExistsByEmail(email)) {
                throw conflict("USER_ALREADY_EXISTS", "user already exists");
            }
            String keycloakUserId = keycloakClient.createUser(
                    username, email, request.getPassword(), extractFirstName(request.getName()), extractLastName(request.getName()), phone, role);
            userRepository.insert(keycloakUserId, request.getName(), username, email, phone);
            return new RegisterResponse(keycloakUserId, "created");
        } catch (ApiException ex) {
            throw ex;
        } catch (KeycloakClient.KeycloakConflictException ex) {
            throw conflict("USER_ALREADY_EXISTS", "user already exists");
        } catch (Exception ex) {
            throw unavailable("KEYCLOAK_UNAVAILABLE", "failed to create user in keycloak");
        }
    }

    public MessageResponse adminUpdateUser(Jwt jwt, String email, AdminUserUpdateRequest request) {
        requireExactRole(jwt, ADMIN_ROLE, "admin role only is required");
        if (ValidationUtils.isBlank(email)) {
            throw badRequest("INVALID_REQUEST", "email is required");
        }
        String normalizedEmail = normalizeEmail(email);
        UserRecord existing = getUserByEmailOrThrow(normalizedEmail);
        String newName = request == null || ValidationUtils.isBlank(request.getName()) ? existing.getName() : request.getName();
        String newPhone = request == null || ValidationUtils.isBlank(request.getPhone()) ? existing.getPhone() : request.getPhone();
        if (!ValidationUtils.isValidPhone(newPhone)) {
            throw badRequest("INVALID_REQUEST", "phone must contain 10 to 15 digits");
        }
        try {
            UserRecord existingWithPhone = userRepository.findByPhone(newPhone);
            if (existingWithPhone != null && !existingWithPhone.getUserId().equals(existing.getUserId())) {
                throw conflict("PHONE_ALREADY_EXISTS", "phone already exists");
            }
            userRepository.updateProfile(existing.getUserId(), newName, newPhone);
            String role = normalizeRole(request == null ? null : request.getRole());
            if (!ValidationUtils.isBlank(role)) {
                if (ADMIN_ROLE.equals(role)) {
                    throw badRequest("INVALID_ROLE", "admin role cannot be assigned via this endpoint");
                }
                if (!RoleUtils.SUPPORTED_USER_ROLES.contains(role)) {
                    throw badRequest("INVALID_ROLE", "unsupported role");
                }
                keycloakClient.assignExclusiveRealmRole(existing.getUserId(), role);
            }
            return new MessageResponse("user updated");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw internal("DB_ERROR", "failed to update user");
        }
    }

    public MessageResponse adminDeleteUser(Jwt jwt, String email) {
        requireExactRole(jwt, ADMIN_ROLE, "admin role only is required");
        if (ValidationUtils.isBlank(email)) {
            throw badRequest("INVALID_REQUEST", "email is required");
        }
        UserRecord existing = getUserByEmailOrThrow(normalizeEmail(email));
        try {
            keycloakClient.deleteUser(existing.getUserId());
            userRepository.deleteByUserId(existing.getUserId());
            return new MessageResponse("user deleted");
        } catch (Exception ex) {
            throw unavailable("KEYCLOAK_UNAVAILABLE", "failed to delete user in keycloak");
        }
    }

    public MessageResponse supportUpdateUser(Jwt jwt, String email, ProfileUpdateRequest request) {
        PrincipalInfo principal = requirePrincipal(jwt);
        if (!RoleUtils.hasExactlyOneOfRoles(principal.getRoles(), Set.of("support_agent", "admin"))) {
            throw forbidden("ACCESS_DENIED", "exactly one of support_agent or admin role is required");
        }
        if (ValidationUtils.isBlank(email)) {
            throw badRequest("INVALID_REQUEST", "email is required");
        }
        UserRecord existing = getUserByEmailOrThrow(normalizeEmail(email));
        String newName = request == null || ValidationUtils.isBlank(request.getName()) ? existing.getName() : request.getName();
        String newPhone = request == null || ValidationUtils.isBlank(request.getPhone()) ? existing.getPhone() : request.getPhone();
        if (!ValidationUtils.isValidPhone(newPhone)) {
            throw badRequest("INVALID_REQUEST", "phone must contain 10 to 15 digits");
        }
        try {
            UserRecord existingWithPhone = userRepository.findByPhone(newPhone);
            if (existingWithPhone != null && !existingWithPhone.getUserId().equals(existing.getUserId())) {
                throw conflict("PHONE_ALREADY_EXISTS", "phone already exists");
            }
            userRepository.updateProfile(existing.getUserId(), newName, newPhone);
            return new MessageResponse("user updated by support");
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw internal("DB_ERROR", "failed to update user");
        }
    }

    public UserProfileResponse airlineViewUser(Jwt jwt, String email) {
        PrincipalInfo principal = requirePrincipal(jwt);
        if (!RoleUtils.hasExactlyOneOfRoles(principal.getRoles(), Set.of("airline_ops", "support_agent", "admin"))) {
            throw forbidden("ACCESS_DENIED", "exactly one of airline_ops, support_agent, or admin role is required");
        }
        if (ValidationUtils.isBlank(email)) {
            throw badRequest("INVALID_REQUEST", "email is required");
        }
        return toUserProfile(getUserByEmailOrThrow(normalizeEmail(email)));
    }

    private AuthResponse toAuthResponseWithRoleValidation(KeycloakClient.TokenResult token) {
        if (ValidationUtils.isBlank(token.accessToken())) {
            throw unauthorized("INVALID_CREDENTIALS", "invalid credentials");
        }
        Set<String> roles = tokenUtils.extractRolesFromAccessToken(token.accessToken());
        if (!RoleUtils.hasExactlyOneSupportedRole(roles)) {
            throw forbidden("ACCESS_DENIED", "user must have exactly one supported role");
        }
        return toAuthResponse(token);
    }

    private AuthResponse toAuthResponse(KeycloakClient.TokenResult token) {
        String userId = tokenUtils.extractClaimFromAccessToken(token.accessToken(), "sub");
        if (ValidationUtils.isBlank(userId)) {
            userId = "unknown";
        }
        String tokenType = ValidationUtils.isBlank(token.tokenType()) ? TOKEN_TYPE_BEARER : token.tokenType();
        return new AuthResponse(token.accessToken(), tokenType, token.expiresIn(), userId, token.refreshToken());
    }

    private ApiException mapTokenError(KeycloakClient.KeycloakTokenException ex) {
        KeycloakClient.KeycloakTokenError tokenError = keycloakClient.parseTokenError(ex.getBody());
        String description = tokenError.description().toLowerCase(Locale.ROOT);
        if ("invalid_grant".equals(tokenError.code()) && description.contains("not fully set up")) {
            return forbidden("ACCOUNT_NOT_READY", "account is not fully set up");
        }
        if ("invalid_grant".equals(tokenError.code()) && description.contains("invalid user credentials")) {
            return unauthorized("INVALID_CREDENTIALS", "invalid credentials");
        }
        if ("invalid_grant".equals(tokenError.code()) && description.contains("code")) {
            return unauthorized("INVALID_AUTHORIZATION_CODE", "authorization code is invalid or expired");
        }
        if ("invalid_client".equals(tokenError.code())) {
            return unavailable("AUTH_CLIENT_INVALID", "keycloak client configuration is invalid");
        }
        if (ex.getStatusCode() == 401) {
            return unauthorized("INVALID_CREDENTIALS", "invalid credentials");
        }
        return unavailable("KEYCLOAK_AUTH_ERROR", "keycloak rejected login request");
    }

    private PrincipalInfo requirePrincipal(Jwt jwt) {
        PrincipalInfo principal = tokenUtils.resolvePrincipal(jwt);
        if (principal == null || ValidationUtils.isBlank(principal.getEmail())) {
            throw unauthorized("AUTH_TOKEN_INVALID", "invalid or missing token");
        }
        if (!RoleUtils.hasExactlyOneSupportedRole(principal.getRoles())) {
            throw forbidden("ACCESS_DENIED", "user must have exactly one supported role");
        }
        return principal;
    }

    private PrincipalInfo requireCustomerPrincipal(Jwt jwt) {
        PrincipalInfo principal = requirePrincipal(jwt);
        if (!RoleUtils.hasExactlyRole(principal.getRoles(), "customer")) {
            throw forbidden("ACCESS_DENIED", "customer role only is required");
        }
        return principal;
    }

    private void requireExactRole(Jwt jwt, String role, String message) {
        PrincipalInfo principal = requirePrincipal(jwt);
        if (!RoleUtils.hasExactlyRole(principal.getRoles(), role)) {
            throw forbidden("ACCESS_DENIED", message);
        }
    }

    private void requireAuthenticated(Jwt jwt) {
        if (jwt == null) {
            throw unauthorized("AUTH_TOKEN_INVALID", "invalid or missing token");
        }
    }

    private UserRecord getUserByEmailOrThrow(String email) {
        try {
            UserRecord user = userRepository.findByEmail(email);
            if (user == null) {
                throw notFound("USER_NOT_FOUND", "user profile not found");
            }
            return user;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw internal("DB_ERROR", "failed to fetch profile");
        }
    }

    private void requireRegisterFields(User request) {
        if (request == null || ValidationUtils.isBlank(request.getName()) || ValidationUtils.isBlank(request.getUsername())
                || ValidationUtils.isBlank(request.getEmail()) || ValidationUtils.isBlank(request.getPassword()) || ValidationUtils.isBlank(request.getPhone())) {
            throw badRequest("INVALID_REQUEST", "name, username, email, password and phone are required");
        }
    }

    private void validateIdentityFields(String username, String email, String password, String phone) {
        if (username.equalsIgnoreCase(email)) {
            throw badRequest("INVALID_REQUEST", "username cannot be same as email");
        }
        if (!ValidationUtils.isValidUsername(username)) {
            throw badRequest("INVALID_REQUEST", "username must be 4-32 chars and contain only letters, digits, ., _, -");
        }
        if (!ValidationUtils.isValidEmail(email)) {
            throw badRequest("INVALID_REQUEST", "email format is invalid");
        }
        if (!ValidationUtils.isStrongPassword(password)) {
            throw badRequest("INVALID_REQUEST", "password must be 8+ chars with upper, lower, digit and special character");
        }
        if (!ValidationUtils.isValidPhone(phone)) {
            throw badRequest("INVALID_REQUEST", "phone must contain 10 to 15 digits");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        return role == null ? null : role.trim().toLowerCase(Locale.ROOT);
    }

    private String extractFirstName(String fullName) {
        if (ValidationUtils.isBlank(fullName)) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }

    private String extractLastName(String fullName) {
        if (ValidationUtils.isBlank(fullName)) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length < 2) {
            return parts[0];
        }
        return String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
    }

    private UserProfileResponse toUserProfile(UserRecord user) {
        return new UserProfileResponse(user.getUserId(), user.getName(), user.getEmail(), user.getPhone());
    }

    private String randomUrlToken(int size) {
        byte[] bytes = new byte[size];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Base64Url(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("failed to build code challenge", ex);
        }
    }

    private ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    private ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }

    private ApiException forbidden(String code, String message) {
        return new ApiException(HttpStatus.FORBIDDEN, code, message);
    }

    private ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    private ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    private ApiException internal(String code, String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, code, message);
    }

    private ApiException unavailable(String code, String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }
}
