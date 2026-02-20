# Flight booking

## Realm setup (`authservice`) - exact Keycloak settings

These are the exact default values wired in `application.yaml` and expected by code.

```yaml
realm: authservice
issuer-uri: http://localhost:8090/realms/authservice
authorization-url: http://localhost:8090/realms/authservice/protocol/openid-connect/auth
token-url: http://localhost:8090/realms/authservice/protocol/openid-connect/token
logout-url: http://localhost:8090/realms/authservice/protocol/openid-connect/logout
revocation-url: http://localhost:8090/realms/authservice/protocol/openid-connect/revoke
admin-users-url: http://localhost:8090/admin/realms/authservice/users
admin-token-url: http://localhost:8090/realms/authservice/protocol/openid-connect/token
client-id: authservice-client
client-secret: jXvZoiPpvazLf8VUl2qxbZHjBj66FSVN
admin-client-id: authservice-admin
admin-client-secret: 394YvP2YpNpt3ZUdfx5ZtWGeIGTK892V
default-redirect-uri: http://localhost:3000/auth/callback
default-scope: openid profile email
timeout-ms: 3000
```

Configure these in Keycloak Admin Console for realm `authservice`:

1. Realm
- Realm name: `authservice`
- Enabled: `ON`

2. Realm roles (must exist exactly)
- `customer`
- `support_agent`
- `airline_ops`
- `admin`

RHBK UI note:
- User role assignment is under `Users` -> select user -> `Role mapping` (or `Role mappings` in some versions).
- In some newer UI layouts, the same screen may be labeled `Associated roles`.
- Assign exactly one supported role: `customer`, `support_agent`, `airline_ops`, or `admin`.

3. Client: `authservice-client` (end-user OIDC)
- Client ID: `authservice-client`
- Protocol: `openid-connect`
- Client authentication:
  - `ON` if using client secret (matches current default config)
  - `OFF` only if you also set `AUTH_RHBK_CLIENT_SECRET` empty in authservice
- Standard flow: `ON` (required for Authorization Code + PKCE)
- Direct access grants: `OFF` (password grant is not used)
- Service accounts roles: `OFF`
- Valid redirect URIs: include `http://localhost:3000/auth/callback`
- Web origins: include `http://localhost:3000` (or `+` based on your Keycloak policy)

4. Client: `authservice-admin` (service-to-service admin API)
- Client ID: `authservice-admin`
- Protocol: `openid-connect`
- Client authentication: `ON`
- Service accounts roles: `ON`
- Standard flow: `OFF` (not required)
- Direct access grants: `OFF`

5. Service account roles for `authservice-admin`
- In `Clients` -> `authservice-admin` -> `Service account roles`:
  - Select client `realm-management`
  - Assign at least:
    - `view-users`
    - `query-users`
    - `manage-users`
    - `view-realm`

6. Token content requirements
- Access token must include role claims for supported roles:
  - `realm_access.roles` and/or `resource_access.authservice-client.roles`
- Strict isolation rule in this service:
  - user must have exactly one supported role among `customer`, `support_agent`, `airline_ops`, `admin`
  - mixed-role tokens are rejected

Dummy frontend for full role testing:
- Files:
  - `tools/pkce-test/index.html` (complete role test console)
  - `tools/pkce-test/callback.html` (simple callback viewer)
- Start server (Linux/macOS): `bash tools/pkce-test/start-server.sh`
- Start server (Windows PowerShell): `powershell -ExecutionPolicy Bypass -File tools/pkce-test/start-server.ps1`
- Open UI: `http://localhost:3000/index.html`
- Configure Keycloak valid redirect URIs to include:
  - `http://localhost:3000/index.html`
  - `http://localhost:3000/callback.html` (optional)
- Backend CORS is enabled for local UI origins:
  - `http://localhost:3000`, `http://127.0.0.1:3000`
  - `http://localhost:5173`, `http://127.0.0.1:5173`

### Complete role flow using dummy frontend

Real-time user login story (PKCE):
- User opens `http://localhost:3000/index.html`.
- User clicks `Start Login (PKCE)`.
- Frontend calls `GET /auth/login/authorize` and gets:
  - `authorizationUrl`
  - `codeVerifier`
  - `state`
- Browser redirects to Keycloak login page.
- User signs in on Keycloak UI.
- Keycloak redirects back to frontend with `code` in URL.
- Frontend reads `code`, then user clicks `Exchange Code`.
- Frontend calls `POST /auth/login` with `code`, `codeVerifier`, `redirectUri`.
- Backend exchanges code with Keycloak token endpoint and returns:
  - `accessToken`, `refreshToken`, `expiresIn`.
- Frontend uses `accessToken` for role APIs.
- Later, user can:
  - click `Refresh` -> `POST /auth/token/refresh`
  - click `Logout` -> `POST /auth/logout`

In-UI guided demo:
- Set persona in `Story Guide (Real-time)` card (`customer`, `admin`, `support_agent`, `airline_ops`).
- Click `Run Guided Checks`.
- UI runs expected allowed and denied APIs for that role and logs all responses.

1. Start dependencies:
- Keycloak on `http://localhost:8090`
- Authservice on `http://localhost:8080`
- Test UI on `http://localhost:3000/index.html`

2. Register test users:
- Use `POST /auth/register` for a customer account.
- Use Keycloak admin console (or existing admin account) for `admin`.
- Use `POST /auth/admin/users` (as admin) to create `support_agent` and `airline_ops`.

3. Login and test each role:
- `customer`:
  - Click `Start Login (PKCE)` -> login as customer in Keycloak -> click `Exchange Code`.
  - Test `GET/PUT/DELETE /auth/profile` should pass.
  - Test admin/support/airline APIs should fail with `403`.
- `admin`:
  - Login as admin and exchange code.
  - Test `GET/POST/PUT/DELETE /auth/admin/users` should pass.
  - Attempt role change to `admin` via API should fail (blocked by service).
- `support_agent`:
  - Login and exchange code.
  - Test `PUT /auth/support/users` should pass.
  - Test `GET /auth/airline/users` should pass.
  - Test admin and customer-only APIs should fail.
- `airline_ops`:
  - Login and exchange code.
  - Test `GET /auth/airline/users` should pass.
  - Test support/admin/customer-only APIs should fail.

4. Token lifecycle checks:
- Click `Refresh` to test `POST /auth/token/refresh`.
- Click `Logout` to test token revocation + session end.
- Retry protected endpoint after logout; it should fail.

## Public endpoints

| Method | Endpoint | Description | Auth |
| ------ | -------- | ----------- | ---- |
| POST | /auth/register | Create Keycloak user + local profile | None |
| GET | /auth/login/authorize | Build Authorization Code + PKCE URL | None |
| POST | /auth/login | Exchange authorization code for tokens | None |
| POST | /auth/token/refresh | Refresh access token via refresh token | None |
| GET | /auth/health/live | Liveness probe | None |
| GET | /auth/health/ready | Readiness probe | None |
| GET | /auth/health | Service + dependency health | None |

## Customer endpoints

| Method | Endpoint | Description | Auth |
| ------ | -------- | ----------- | ---- |
| GET | /auth/profile | Read profile by token email | JWT (`customer`) |
| PUT | /auth/profile | Update profile | JWT (`customer`) |
| DELETE | /auth/profile | Delete own profile | JWT (`customer`) |
| POST | /auth/logout | Revoke tokens and end Keycloak session | JWT |

## Admin endpoints

| Method | Endpoint | Description | Auth |
| ------ | -------- | ----------- | ---- |
| GET | /auth/admin/users?email=... | Read any profile by email | JWT (`admin`) |
| POST | /auth/admin/users | Create user (role-based) | JWT (`admin`) |
| PUT | /auth/admin/users?email=... | Update user + optional role | JWT (`admin`) |
| DELETE | /auth/admin/users?email=... | Delete user | JWT (`admin`) |

## Support endpoints

| Method | Endpoint | Description | Auth |
| ------ | -------- | ----------- | ---- |
| PUT | /auth/support/users?email=... | Support updates customer profile | JWT (`support_agent`/`admin`) |

## Airline Ops endpoints

| Method | Endpoint | Description | Auth |
| ------ | -------- | ----------- | ---- |
| GET | /auth/airline/users?email=... | Airline ops read profile | JWT (`airline_ops`/`support_agent`/`admin`) |

## Current implementation notes

- Keycloak is used for authorization-code authentication and token lifecycle (`/auth/login/authorize`, `/auth/login`, `/auth/token/refresh`, `/auth/logout`).
- Bearer-token validation uses Spring Security JWT Resource Server (no per-request UserInfo calls).
- Role checks are enforced from JWT roles:
  - `customer`: `/auth/profile` GET/PUT/DELETE
  - `admin`: full user management (`/auth/admin/users` GET/POST/PUT/DELETE)
  - Note: `admin` role assignment is blocked via service APIs; admin users must be provisioned directly in Keycloak.
  - `support_agent`: assisted update (`/auth/support/users`)
  - `airline_ops`: read-only operations endpoint (`/auth/airline/users`)
- Local DB (`public.flightapp` by default) stores domain profile fields (`name`, `username`, `email`, `phone`).
- `/auth/register` creates `customer` in Keycloak first, then persists local DB profile.
- `/auth/logout` revokes access/refresh tokens and ends Keycloak session.
- Login flow uses Authorization Code + PKCE against Keycloak token endpoint.
- Strict role isolation is enforced: a user must have exactly one supported role (`customer`, `support_agent`, `airline_ops`, `admin`) for protected APIs; mixed-role tokens are denied.

## Status

Completed:
- Keycloak-backed register/login/profile flow is implemented.
- Role-based authorization is implemented for `customer`, `admin`, `support_agent`, `airline_ops`.
- Admin APIs support create/read/update/delete user lifecycle.
- Validation implemented for:
  - unique username
  - unique email
  - unique phone
  - username format and `username != email`
  - strong password policy
- DB schema updated for `username` and uniqueness constraints.
- Health endpoints are implemented (`live`, `ready`, `health`).

Production hardening status:
- None.

## CRUD by role

- `customer`
  - Create: `POST /auth/register`
  - Read: `GET /auth/profile`
  - Update: `PUT /auth/profile`
  - Delete: `DELETE /auth/profile`

- `admin`
  - Create: `POST /auth/admin/users`
  - Read: `GET /auth/admin/users?email=...`
  - Update: `PUT /auth/admin/users?email=...`
  - Delete: `DELETE /auth/admin/users?email=...`

- `support_agent`
  - Read: via `GET /auth/airline/users?email=...`
  - Update: `PUT /auth/support/users?email=...`
  - Create/Delete: Not allowed

- `airline_ops`
  - Read: `GET /auth/airline/users?email=...`
  - Create/Update/Delete: Not allowed

## Role activities

| Activity | Public (no JWT) | customer | support_agent | airline_ops | admin |
| -------- | ---------------- | -------- | ------------- | ----------- | ----- |
| Register new customer account | Yes (`POST /auth/register`) | No | No | No | No |
| Start PKCE login | Yes (`GET /auth/login/authorize`) | Yes | Yes | Yes | Yes |
| Exchange auth code for tokens | Yes (`POST /auth/login`) | Yes | Yes | Yes | Yes |
| Refresh access token | Yes (`POST /auth/token/refresh`) | Yes | Yes | Yes | Yes |
| Read own profile | No | Yes (`GET /auth/profile`) | No | No | No |
| Update own profile | No | Yes (`PUT /auth/profile`) | No | No | No |
| Delete own profile | No | Yes (`DELETE /auth/profile`) | No | No | No |
| Logout / revoke session | No | Yes (`POST /auth/logout`) | Yes (`POST /auth/logout`) | Yes (`POST /auth/logout`) | Yes (`POST /auth/logout`) |
| Read any user profile by email | No | No | No (except airline view endpoint) | Yes (`GET /auth/airline/users`) | Yes (`GET /auth/admin/users`) |
| Update another user's profile | No | No | Yes (`PUT /auth/support/users`) | No | Yes (`PUT /auth/admin/users`) |
| Create users with role | No | No | No | No | Yes (`POST /auth/admin/users`) |
| Delete users | No | No | No | No | Yes (`DELETE /auth/admin/users`) |
| Change user's role | No | No | No | No | Yes (`PUT /auth/admin/users` with `role`, except `admin`) |

Per-role activity summary:
- `customer`
  - Sign up as customer, log in via PKCE, refresh token, manage own profile, logout.
- `support_agent`
  - Log in via PKCE, refresh token, logout, update customer profile via support endpoint, read customer profile via airline view endpoint.
- `airline_ops`
  - Log in via PKCE, refresh token, logout, read customer profile via airline view endpoint.
- `admin`
  - Log in via PKCE, refresh token, logout, full user lifecycle (create/read/update/delete), and role assignment.
  - Cannot assign `admin` role via this service API.

## Login user stories

1. Valid user login (customer/admin/support_agent/airline_ops)
- Input: valid authorization `code` + `codeVerifier` (+ matching `redirectUri`)
- Result: `200 OK`
- Response: `accessToken`, `tokenType`, `expiresIn`, `userId`
  - Response includes `refreshToken` for logout/revocation.

2. Invalid authorization code
- Input: invalid/expired authorization code or verifier mismatch
- Result: `401 Unauthorized`
- Response code: `INVALID_AUTHORIZATION_CODE`

3. Account exists but not fully set up in Keycloak
- Input: user has pending setup requirements (for example required actions/profile policy)
- Result: `403 Forbidden`
- Response code: `ACCOUNT_NOT_READY`

4. User authenticated but no supported flight-booking role
- Input: valid token but no role among `customer`, `admin`, `support_agent`, `airline_ops`
- Result: `403 Forbidden`
- Response code: `ACCESS_DENIED`

5. Keycloak client misconfiguration
- Input: invalid client id/secret configuration
- Result: `503 Service Unavailable`
- Response code: `AUTH_CLIENT_INVALID`

6. Keycloak unavailable or unexpected auth error
- Input: Keycloak down/network issue/unknown token endpoint rejection
- Result: `503 Service Unavailable`
- Response code: `KEYCLOAK_UNAVAILABLE` or `KEYCLOAK_AUTH_ERROR`

## Curl commands by role

### 0) Common setup

```bash
BASE_URL="http://localhost:8080"
CORRELATION_ID="curl-check-001"
USERNAME="test_user_$(date +%s)"
EMAIL="<keycloak-user-email>"
PASSWORD="Test@1234"
REDIRECT_URI="http://localhost:3000/auth/callback"
KC_BASE_URL="http://localhost:8090"
KC_REALM="authservice"
KC_CLIENT_ID="authservice-client"
KC_CLIENT_SECRET="<client-secret-or-empty-for-public-client>"
KC_ADMIN_CLIENT_ID="authservice-admin"
KC_ADMIN_CLIENT_SECRET="<admin-client-secret>"
```

Prerequisites:
- Keycloak realm/client/user are up and reachable by authservice.
- For `POST /auth/register`, authservice must be able to call Keycloak Admin API with a client that can manage users in realm.
- `KC_ADMIN_CLIENT_ID`/`KC_ADMIN_CLIENT_SECRET` must map to `AUTH_RHBK_ADMIN_CLIENT_ID`/`AUTH_RHBK_ADMIN_CLIENT_SECRET`.
- For confidential client, set real `KC_CLIENT_SECRET`.
- For public client, keep `KC_CLIENT_SECRET=""` (authservice now skips `client_secret` when empty).
- Validation rules:
  - username must be 4-32 chars and use only letters, digits, `.`, `_`, `-`
  - username must be different from email
  - password must be 8+ chars with upper, lower, digit, special
  - phone must be 10-15 digits and unique

### 1) Health checks

```bash
curl -i "$BASE_URL/auth/health/live"
```

```bash
curl -i "$BASE_URL/auth/health/ready"
```

```bash
curl -i "$BASE_URL/auth/health"
```

### 2) PKCE login flow (used by customer/admin/support/airline users)

Start auth and get `authorizationUrl` + `codeVerifier`:

```bash
AUTH_START=$(curl -s "$BASE_URL/auth/login/authorize?redirectUri=$(printf %s "$REDIRECT_URI" | sed 's/:/%3A/g; s/\//%2F/g')")

AUTH_URL=$(echo "$AUTH_START" | tr -d '\n' | sed -n 's/.*"authorizationUrl"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
CODE_VERIFIER=$(echo "$AUTH_START" | tr -d '\n' | sed -n 's/.*"codeVerifier"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')

echo "$AUTH_URL"
echo "$CODE_VERIFIER"
```

Open `AUTH_URL` in browser, login with the intended account, and copy `code` from callback URL:

```text
http://localhost:3000/auth/callback?code=<AUTH_CODE>&state=<STATE>
```

Exchange code for tokens:

```bash
AUTH_CODE="<paste-code-from-callback>"

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"code\":\"$AUTH_CODE\",\"codeVerifier\":\"$CODE_VERIFIER\",\"redirectUri\":\"$REDIRECT_URI\"}")

TOKEN=$(echo "$LOGIN_RESPONSE" | tr -d '\n' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | tr -d '\n' | sed -n 's/.*"refreshToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')

echo "$TOKEN"
echo "$REFRESH_TOKEN"
```

Refresh token:

```bash
REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/token/refresh" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")

NEW_TOKEN=$(echo "$REFRESH_RESPONSE" | tr -d '\n' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
NEW_REFRESH_TOKEN=$(echo "$REFRESH_RESPONSE" | tr -d '\n' | sed -n 's/.*"refreshToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
```

Negative login (invalid authorization code):

```bash
curl -i -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"code\":\"invalid-code\",\"codeVerifier\":\"$CODE_VERIFIER\",\"redirectUri\":\"$REDIRECT_URI\"}"
```

### Customer (`customer`)

Register:

```bash
REGISTER_PAYLOAD=$(cat <<EOF
{
  "name": "Test User",
  "username": "$USERNAME",
  "email": "$EMAIL",
  "password": "$PASSWORD",
  "phone": "9999999999"
}
EOF
)

curl -i -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "$REGISTER_PAYLOAD"
```

Login:
- Use the PKCE flow in section `2)` with customer credentials.

Read own profile:

```bash
curl -i "$BASE_URL/auth/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "x-correlation-id: $CORRELATION_ID"
```

Update own profile:

```bash
curl -i -X PUT "$BASE_URL/auth/profile" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d '{
    "name": "Test User Updated",
    "phone": "8888888888"
  }'
```

Logout:

```bash
curl -i -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\",\"accessToken\":\"$TOKEN\"}"
```

```bash
# Verify token after logout (should fail)
curl -i "$BASE_URL/auth/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "x-correlation-id: $CORRELATION_ID"
```

### Admin (`admin`)

Admin login:

- Use the PKCE flow in section `2)` with admin account.
- Set `ADMIN_TOKEN="$TOKEN"` after successful admin login.

Read user by email:

```bash
curl -i "$BASE_URL/auth/admin/users?email=$EMAIL" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "x-correlation-id: $CORRELATION_ID"
```

Create support user:

```bash
SUPPORT_EMAIL="support.user.$(date +%s)@local.com"
SUPPORT_USERNAME="support_user_$(date +%s)"
SUPPORT_PHONE="$(date +%s%N | tr -d '\n' | cut -c1-10)"

curl -i -X POST "$BASE_URL/auth/admin/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"name\":\"Support User\",\"username\":\"$SUPPORT_USERNAME\",\"email\":\"$SUPPORT_EMAIL\",\"password\":\"Support@123\",\"phone\":\"$SUPPORT_PHONE\",\"role\":\"support_agent\"}"
```

Update support user:

```bash
curl -i -X PUT "$BASE_URL/auth/admin/users?email=$SUPPORT_EMAIL" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d '{
    "name": "Support User Updated",
    "phone": "7666666666",
    "role": "airline_ops"
  }'
```

Delete user:

```bash
curl -i -X DELETE "$BASE_URL/auth/admin/users?email=$SUPPORT_EMAIL" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "x-correlation-id: $CORRELATION_ID"
```

### Support Agent (`support_agent`)

Support login:

- Use the PKCE flow in section `2)` with support user credentials.
- Set `SUPPORT_TOKEN="$TOKEN"` after successful support login.

Update customer profile:

```bash
curl -i -X PUT "$BASE_URL/auth/support/users?email=$EMAIL" \
  -H "Authorization: Bearer $SUPPORT_TOKEN" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d '{
    "name": "Customer Updated By Support",
    "phone": "7000000000"
  }'
```

### Airline Ops (`airline_ops`)

Airline ops login:

- Use the PKCE flow in section `2)` with airline ops user credentials.
- Set `AIRLINE_TOKEN="$TOKEN"` after successful airline ops login.

Read customer profile:

```bash
curl -i "$BASE_URL/auth/airline/users?email=$EMAIL" \
  -H "Authorization: Bearer $AIRLINE_TOKEN" \
  -H "x-correlation-id: $CORRELATION_ID"
```
