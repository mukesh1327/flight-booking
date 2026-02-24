# Auth Service Plan (Keycloak + Spring Boot)

## 1. Objective
Design and implement auth for two user groups:
- `PUBLIC` customers: Google social login first.
- `CORP` airline staff: secure workforce login with strong MFA.

`authservice` acts as auth API facade/orchestrator for frontend and backend services, while Keycloak is the IAM core.

## 2. Target Architecture
- Keycloak: identity provider, realm policies, token issuance, user federation.
- authservice (Spring Boot):
  - Starts login flows and callback handling.
  - Adds business-specific checks (risk, profile-completion flags, customer linking).
  - Exposes stable APIs for UI and other services.
- profile-service: customer profile data (name, phone, preferences).
- corp-admin-service: corporate user lifecycle (roles, activation, manager mapping).

## 3. Keycloak Realm Design

### 3.1 Realms
- `skyfly-public`
  - Purpose: public customer authentication.
  - Identity provider: Google OIDC.
- `skyfly-corp`
  - Purpose: airline staff authentication.
  - Identity provider: enterprise IdP (OIDC/SAML) where available.

### 3.2 Clients
- Public web client: `skyfly-public-web`
  - Type: `public` (PKCE required), Standard Flow enabled.
  - Redirect URIs: `https://<public-ui>/auth/google/callback`.
  - Web Origins: public UI domains only.
- Corp web client: `skyfly-corp-web`
  - Type: `confidential` or `public+PKCE` based on architecture.
  - Redirect URIs: `https://<corp-ui>/corp/auth/callback`.
- Backend API client: `skyfly-auth-api`
  - Service account enabled for admin API operations.

### 3.3 Roles and Groups
- Realm roles:
  - Public: `CUSTOMER`.
  - Corporate: `CORP_ADMIN`, `OPS_AGENT`, `SUPPORT_AGENT`, `FINANCE_AGENT`.
- Groups (corp): `/ops`, `/support`, `/finance`, `/admin`.
- Map group -> role via realm role mappings.

### 3.4 Authentication Flows
- `skyfly-public`:
  - Browser flow with Google IdP redirect.
  - First broker login flow: create/link account.
  - Optional step-up (email/mobile OTP) only for sensitive actions.
- `skyfly-corp`:
  - Username/email + IdP/credential flow.
  - Required MFA:
    - Primary: WebAuthn/passkey.
    - Fallback: TOTP.
    - Break-glass: short-lived email OTP with alerting.

### 3.5 Token and Session Policies
- Access token TTL: 10-15 min (public), 5-10 min (corp).
- Refresh token:
  - Rotation enabled.
  - Reuse detection enabled.
  - Offline tokens only where explicitly needed.
- Session limits:
  - Corp admin: strict concurrent-session limit.
  - Forced re-auth for high-risk/admin actions.

### 3.6 Required Keycloak Configurations
- Enable SSL required (`external` or `all` in prod).
- Configure brute-force detection and temporary lockout.
- Configure events and admin events with retention export pipeline.
- Configure Google IdP:
  - `client_id`, `client_secret`.
  - Allowed hosted domain check only if needed.
  - Sync mode: import profile claims with controlled updates.
- Configure custom claims mappers:
  - `user_id`, `realm`, `roles`, `mfa_level`.

## 4. APIs to Develop in authservice
Base path: `/api/v1`

### 4.1 Public Auth APIs
1. `GET /auth/public/google/start`
- Purpose: build authorization URL + state + PKCE challenge.
- Response: `PublicGoogleStartResponse`.

2. `GET /auth/public/google/callback?code=&state=`
- Purpose: exchange code, validate state, create/link user, issue app session.
- Response: `AuthSessionResponse`.

3. `POST /auth/public/logout`
- Purpose: revoke refresh token/session.
- Request: `LogoutRequest`.
- Response: `204`.

4. `POST /auth/public/step-up/otp/request`
- Purpose: optional sensitive-action challenge.
- Request: `OtpRequest`.
- Response: `OtpChallengeResponse`.

5. `POST /auth/public/step-up/otp/verify`
- Purpose: verify OTP challenge.
- Request: `OtpVerifyRequest`.
- Response: `StepUpVerifyResponse`.

### 4.2 Corporate Auth APIs
1. `POST /auth/corp/login/init`
- Purpose: start staff login and return allowed factors.
- Request: `CorpLoginInitRequest`.
- Response: `CorpLoginInitResponse`.

2. `POST /auth/corp/login/verify`
- Purpose: verify primary auth factor/assertion.
- Request: `CorpLoginVerifyRequest`.
- Response: `AuthSessionResponse` or challenge required.

3. `POST /auth/corp/mfa/challenge`
- Purpose: initiate MFA challenge.
- Request: `CorpMfaChallengeRequest`.
- Response: `OtpChallengeResponse` or `WebAuthnChallengeResponse`.

4. `POST /auth/corp/mfa/verify`
- Purpose: verify MFA and issue session.
- Request: `CorpMfaVerifyRequest`.
- Response: `AuthSessionResponse`.

5. `POST /auth/corp/logout`
- Purpose: terminate single/all staff sessions.
- Request: `LogoutRequest`.
- Response: `204`.

### 4.3 Shared Session/Profile Bridge APIs
1. `POST /auth/token/refresh`
- Request: `RefreshTokenRequest`.
- Response: `AuthSessionResponse`.

2. `GET /users/me`
- Purpose: return merged identity + profile view.
- Response: `MeResponse`.

3. `PATCH /users/me`
- Purpose: update mutable profile data.
- Request: `UpdateMeRequest`.
- Response: `MeResponse`.

4. `GET /sessions/me`
- Purpose: list active sessions/devices.
- Response: `SessionListResponse`.

5. `DELETE /sessions/me/{sessionId}`
- Purpose: revoke session.
- Response: `204`.

### 4.4 Corporate User Management APIs (Auth Scope)
1. `POST /corp/users`
2. `GET /corp/users`
3. `GET /corp/users/{id}`
4. `PATCH /corp/users/{id}`
5. `POST /corp/users/{id}/enable`
6. `POST /corp/users/{id}/disable`
7. `POST /corp/users/{id}/roles`
8. `DELETE /corp/users/{id}/roles/{roleId}`
9. `POST /corp/users/{id}/force-mfa-reset`
10. `POST /corp/users/{id}/session-revoke`

## 5. DTO Classes (Implementation Contract)
Use Java records for immutable API contracts and Bean Validation annotations for request validation.

### 5.1 Package Layout
- `com.cloudxplorer.authservice.api.dto.common`
- `com.cloudxplorer.authservice.api.dto.publicauth`
- `com.cloudxplorer.authservice.api.dto.corpauth`
- `com.cloudxplorer.authservice.api.dto.user`
- `com.cloudxplorer.authservice.api.dto.session`
- `com.cloudxplorer.authservice.api.dto.corpadmin`

### 5.2 Common DTOs
```java
package com.cloudxplorer.authservice.api.dto.common;

import java.util.List;
import java.util.Map;

public record ErrorResponse(
    String code,
    String message,
    String traceId,
    Map<String, Object> details
) {}

public record UserSummary(
    String userId,
    String email,
    String realm,
    String status,
    List<String> roles
) {}

public record TokenPair(
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
```

### 5.3 Public Auth DTOs
```java
package com.cloudxplorer.authservice.api.dto.publicauth;

import com.cloudxplorer.authservice.api.dto.common.TokenPair;
import com.cloudxplorer.authservice.api.dto.common.UserSummary;
import jakarta.validation.constraints.NotBlank;

public record PublicGoogleStartResponse(
    String authorizationUrl,
    String state,
    String codeChallengeMethod
) {}

public record AuthSessionResponse(
    TokenPair tokens,
    UserSummary user,
    boolean isNewUser,
    String profileStatus,
    String mfaLevel
) {}

public record OtpRequest(
    @NotBlank String purpose,
    @NotBlank String channel,
    @NotBlank String emailOrPhone
) {}

public record OtpChallengeResponse(
    String challengeId,
    long expiresIn,
    long resendAfter
) {}

public record OtpVerifyRequest(
    @NotBlank String challengeId,
    @NotBlank String otp
) {}

public record StepUpVerifyResponse(
    boolean verified,
    String expiresAt
) {}
```

### 5.4 Corporate Auth DTOs
```java
package com.cloudxplorer.authservice.api.dto.corpauth;

import com.cloudxplorer.authservice.api.dto.publicauth.AuthSessionResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record DeviceInfo(
    String userAgent,
    String ip,
    String deviceId,
    String platform
) {}

public record CorpLoginInitRequest(
    @NotBlank String email,
    @NotNull DeviceInfo deviceInfo
) {}

public record CorpLoginInitResponse(
    String loginFlowId,
    List<String> allowedFactors,
    boolean requiresStepUp
) {}

public record CorpLoginVerifyRequest(
    @NotBlank String loginFlowId,
    @NotBlank String factorType,
    @NotBlank String assertion
) {}

public record CorpMfaChallengeRequest(
    @NotBlank String loginFlowId,
    @NotBlank String factorType
) {}

public record WebAuthnChallengeResponse(
    String challenge,
    String rpId,
    long timeout
) {}

public record CorpMfaVerifyRequest(
    @NotBlank String challengeId,
    @NotBlank String otpOrAssertion
) {}

public record CorpLoginVerifyResponse(
    AuthSessionResponse session,
    boolean challengeRequired,
    String challengeType,
    Map<String, Object> challengeMetadata
) {}
```

### 5.5 Session and User DTOs
```java
package com.cloudxplorer.authservice.api.dto.session;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RefreshTokenRequest(@NotBlank String refreshToken) {}

public record LogoutRequest(
    String refreshToken,
    boolean allSessions
) {}

public record SessionInfo(
    String sessionId,
    String device,
    String ip,
    String createdAt,
    String lastSeenAt,
    String riskLevel
) {}

public record SessionListResponse(List<SessionInfo> sessions) {}
```

```java
package com.cloudxplorer.authservice.api.dto.user;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record MeResponse(
    String userId,
    String email,
    String firstName,
    String lastName,
    String mobile,
    boolean mobileVerified,
    String realm,
    List<String> roles,
    String profileStatus
) {}

public record UpdateMeRequest(
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 20) String mobile,
    Map<String, Object> preferences
) {}
```

### 5.6 Corporate Admin DTOs
```java
package com.cloudxplorer.authservice.api.dto.corpadmin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CorpUserCreateRequest(
    @Email String email,
    @NotEmpty List<String> roleIds,
    String department,
    String managerId
) {}

public record CorpUserUpdateRequest(
    String status,
    String department,
    String managerId
) {}

public record RoleAssignmentRequest(@NotBlank String roleId) {}
```

## 6. Controller Interface Plan (Spring Web)
Create interface-first controllers (`*Api`) and implementation classes (`*Controller`) to keep HTTP contracts stable.

```java
package com.cloudxplorer.authservice.api;

import com.cloudxplorer.authservice.api.dto.corpauth.*;
import com.cloudxplorer.authservice.api.dto.publicauth.*;
import com.cloudxplorer.authservice.api.dto.session.*;
import com.cloudxplorer.authservice.api.dto.user.*;
import com.cloudxplorer.authservice.api.dto.corpadmin.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface PublicAuthApi {
    @GetMapping("/api/v1/auth/public/google/start")
    ResponseEntity<PublicGoogleStartResponse> startGoogleLogin();

    @GetMapping("/api/v1/auth/public/google/callback")
    ResponseEntity<AuthSessionResponse> googleCallback(
        @RequestParam("code") String code,
        @RequestParam("state") String state
    );

    @PostMapping("/api/v1/auth/public/logout")
    ResponseEntity<Void> publicLogout(@Valid @RequestBody LogoutRequest request);

    @PostMapping("/api/v1/auth/public/step-up/otp/request")
    ResponseEntity<OtpChallengeResponse> requestStepUpOtp(@Valid @RequestBody OtpRequest request);

    @PostMapping("/api/v1/auth/public/step-up/otp/verify")
    ResponseEntity<StepUpVerifyResponse> verifyStepUpOtp(@Valid @RequestBody OtpVerifyRequest request);
}

public interface CorpAuthApi {
    @PostMapping("/api/v1/auth/corp/login/init")
    ResponseEntity<CorpLoginInitResponse> initCorpLogin(@Valid @RequestBody CorpLoginInitRequest request);

    @PostMapping("/api/v1/auth/corp/login/verify")
    ResponseEntity<CorpLoginVerifyResponse> verifyCorpLogin(@Valid @RequestBody CorpLoginVerifyRequest request);

    @PostMapping("/api/v1/auth/corp/mfa/challenge")
    ResponseEntity<Object> challengeCorpMfa(@Valid @RequestBody CorpMfaChallengeRequest request);

    @PostMapping("/api/v1/auth/corp/mfa/verify")
    ResponseEntity<AuthSessionResponse> verifyCorpMfa(@Valid @RequestBody CorpMfaVerifyRequest request);

    @PostMapping("/api/v1/auth/corp/logout")
    ResponseEntity<Void> corpLogout(@Valid @RequestBody LogoutRequest request);
}

public interface SessionApi {
    @PostMapping("/api/v1/auth/token/refresh")
    ResponseEntity<AuthSessionResponse> refresh(@Valid @RequestBody RefreshTokenRequest request);

    @GetMapping("/api/v1/sessions/me")
    ResponseEntity<SessionListResponse> getMySessions();

    @DeleteMapping("/api/v1/sessions/me/{sessionId}")
    ResponseEntity<Void> revokeSession(@PathVariable("sessionId") String sessionId);
}

public interface UserApi {
    @GetMapping("/api/v1/users/me")
    ResponseEntity<MeResponse> getMe();

    @PatchMapping("/api/v1/users/me")
    ResponseEntity<MeResponse> updateMe(@Valid @RequestBody UpdateMeRequest request);
}

public interface CorpUserAdminApi {
    @PostMapping("/api/v1/corp/users")
    ResponseEntity<Void> createCorpUser(@Valid @RequestBody CorpUserCreateRequest request);

    @PatchMapping("/api/v1/corp/users/{id}")
    ResponseEntity<Void> updateCorpUser(@PathVariable("id") String id, @Valid @RequestBody CorpUserUpdateRequest request);

    @PostMapping("/api/v1/corp/users/{id}/enable")
    ResponseEntity<Void> enableCorpUser(@PathVariable("id") String id);

    @PostMapping("/api/v1/corp/users/{id}/disable")
    ResponseEntity<Void> disableCorpUser(@PathVariable("id") String id);

    @PostMapping("/api/v1/corp/users/{id}/roles")
    ResponseEntity<Void> assignRole(@PathVariable("id") String id, @Valid @RequestBody RoleAssignmentRequest request);

    @DeleteMapping("/api/v1/corp/users/{id}/roles/{roleId}")
    ResponseEntity<Void> removeRole(@PathVariable("id") String id, @PathVariable("roleId") String roleId);
}
```

## 7. Clean Architecture Plan (Spring Boot)

### 7.1 Layered Structure
- `api` (presentation): controllers, DTOs, request validation, response mapping.
- `application` (use cases): orchestration services, transaction boundaries, policy checks.
- `domain` (core): entities, value objects, domain services, business rules.
- `infrastructure` (adapters): Keycloak client, repositories, Redis, DB, messaging.

### 7.2 Package Blueprint
- `com.cloudxplorer.authservice.api`
- `com.cloudxplorer.authservice.application`
- `com.cloudxplorer.authservice.domain`
- `com.cloudxplorer.authservice.infrastructure`
- `com.cloudxplorer.authservice.config`

### 7.3 Dependency Rule
- `api` -> `application`
- `application` -> `domain`
- `infrastructure` -> `application` and `domain`
- `domain` -> no dependency on Spring or infrastructure

### 7.4 Core Use Cases
- `StartPublicGoogleLoginUseCase`
- `CompletePublicGoogleLoginUseCase`
- `InitCorpLoginUseCase`
- `VerifyCorpPrimaryFactorUseCase`
- `ChallengeCorpMfaUseCase`
- `VerifyCorpMfaUseCase`
- `RefreshTokenUseCase`
- `GetMeUseCase`
- `UpdateMeUseCase`
- `RevokeSessionUseCase`

### 7.5 Ports and Adapters
- Outbound ports:
  - `IdentityProviderPort` (Keycloak interactions)
  - `UserProfilePort`
  - `SessionStorePort`
  - `OtpChallengePort`
  - `AuditEventPort`
- Inbound adapters:
  - REST controllers implementing `*Api` interfaces.
- Outbound adapters:
  - `KeycloakIdentityProviderAdapter`
  - `PostgresSessionStoreAdapter`
  - `RedisOtpChallengeAdapter`
  - `KafkaAuditEventAdapter` or DB audit adapter.

### 7.6 Cross-Cutting Standards
- Global exception handler maps domain/application errors to `ErrorResponse`.
- Correlation ID filter for all inbound requests.
- OpenAPI contracts generated from interface annotations.
- Structured logging (JSON) and PII masking.
- Unit tests for use-cases, integration tests for adapters/controllers.

## 8. Is Database Required for authservice?
Yes. At least one database is required in the auth domain.

### 6.1 Mandatory: Keycloak Database
- Keycloak always needs its own persistent DB (typically PostgreSQL).
- Stores users, credentials, federated identity links, realm/client settings, sessions, consents, tokens metadata.
- Without this DB, Keycloak cannot operate reliably across restarts/clusters.

### 6.2 authservice Database (Recommended)
`authservice` can be stateless, but a service-side DB is strongly recommended for business auth extensions:
- Login state/nonce/state tracking (if not fully delegated).
- Step-up challenge metadata.
- Audit trails and security events.
- Idempotency keys for callback/login completion.

Suggested storage:
- PostgreSQL: durable auth business records and audit metadata.
- Redis: short-lived challenge cache, rate-limit counters, anti-replay state.

### 6.3 How It Works End-to-End
1. UI calls authservice start endpoint.
2. authservice redirects to Keycloak/Google flow.
3. Keycloak validates identity and issues tokens.
4. authservice validates callback, enriches with business checks/profile status.
5. authservice returns app session response and sets secure refresh token cookie.
6. Business services trust JWT via gateway/JWKS validation and use internal `userId` for domain data (bookings, payments, notifications).

## 9. Security Controls Checklist
- Separate realms for `PUBLIC` and `CORP`.
- Enforce PKCE + state + nonce for browser flows.
- Enable brute-force protection and rate limits.
- Use HttpOnly + Secure + SameSite cookies for refresh tokens.
- Rotate refresh tokens and detect reuse.
- Enforce MFA for all corp users; passkey-first for admins.
- Emit immutable auth/admin audit events.
- Keep secrets in vault; never hardcode provider secrets.

## 10. Implementation Phases
1. Realm setup + Google IdP + public login APIs.
2. Corp login + MFA flows + role mapping.
3. Session management + audit APIs + admin operations.
4. Step-up auth for sensitive customer actions + risk rules.
5. SCIM/HR sync and advanced conditional access.

## 11. Minimum Environment Variables
- `KEYCLOAK_BASE_URL`
- `KEYCLOAK_PUBLIC_REALM=skyfly-public`
- `KEYCLOAK_CORP_REALM=skyfly-corp`
- `KEYCLOAK_CLIENT_ID_PUBLIC`
- `KEYCLOAK_CLIENT_ID_CORP`
- `KEYCLOAK_CLIENT_SECRET_CORP` (if confidential client)
- `GOOGLE_IDP_ALIAS=google`
- `JWT_JWKS_URI`
- `REDIS_URL`
- `AUTH_DB_URL`
- `AUTH_DB_USER`
- `AUTH_DB_PASSWORD`

---
This plan should be treated as the implementation contract for authservice before coding controllers, DTOs, and security configuration.
