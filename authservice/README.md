# Flight booking

## Public endpoints

| Method | Endpoint | Description | Auth |
| ------ | -------- | ----------- | ---- |
| POST | /auth/register | Create Keycloak user + local profile | None |
| POST | /auth/login | Login via Keycloak token API | None |
| GET | /auth/health/live | Liveness probe | None |
| GET | /auth/health/ready | Readiness probe | None |
| GET | /auth/health | Service + dependency health | None |

## Customer endpoints

| Method | Endpoint | Description | Auth |
| ------ | -------- | ----------- | ---- |
| GET | /auth/profile | Read profile by token email | JWT (`customer`) |
| PUT | /auth/profile | Update profile | JWT (`customer`) |
| POST | /auth/logout | Stateless logout response | JWT |

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

- Keycloak is used for authentication (`/auth/login`) and bearer-token validation (`/auth/profile`, `/auth/profile` PUT).
- Role checks are enforced from JWT roles:
  - `customer`: `/auth/profile`, `/auth/profile` PUT
  - `admin`: full user management (`/auth/admin/users` GET/POST/PUT/DELETE)
  - `support_agent`: assisted update (`/auth/support/users`)
  - `airline_ops`: read-only operations endpoint (`/auth/airline/users`)
- Local DB (`public.flightapp` by default) stores domain profile fields (`name`, `email`, `phone`).
- `/auth/register` creates `customer` in Keycloak first, then persists local DB profile.
- `/auth/logout` currently does not revoke Keycloak token.
- Current login flow uses password grant against Keycloak token endpoint.

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

Pending (before production hardening):
- Replace password grant with Authorization Code + PKCE for end-user login.
- Token revocation/real logout support.
- Global correlation-id filter with auto-generation for all success/error logs.
- Replace regex-based role parsing with Spring Security Resource Server JWT claim mapping.
- Add automated integration tests for full role lifecycle and failure paths.

## CRUD by role

- `customer`
  - Create: `POST /auth/register`
  - Read: `GET /auth/profile`
  - Update: `PUT /auth/profile`
  - Delete: Not allowed directly (admin only)

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

## Login user stories

1. Valid user login (customer/admin/support_agent/airline_ops)
- Input: correct `username` + `password`
- Result: `200 OK`
- Response: `accessToken`, `tokenType`, `expiresIn`, `userId`

2. Invalid credentials
- Input: wrong password or unknown username
- Result: `401 Unauthorized`
- Response code: `INVALID_CREDENTIALS`

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

```bash
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}" \
  | tr -d '\n' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')

echo "$TOKEN"
```

Negative login (wrong password):

```bash
curl -i -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"WrongPassword123\"}"
```

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
  -H "x-correlation-id: $CORRELATION_ID"
```

```bash
# Verify token after logout (should fail)
curl -i "$BASE_URL/auth/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "x-correlation-id: $CORRELATION_ID"
```

### Admin (`admin`)

Admin login:

```bash
ADMIN_EMAIL="<keycloak-admin-email>"
ADMIN_PASSWORD="<keycloak-admin-password>"

ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"username\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" \
  | tr -d '\n' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
```

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

```bash
SUPPORT_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"username\":\"$SUPPORT_USERNAME\",\"password\":\"Support@123\"}" \
  | tr -d '\n' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
```

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

```bash
AIRLINE_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: $CORRELATION_ID" \
  -d "{\"username\":\"$SUPPORT_USERNAME\",\"password\":\"Support@123\"}" \
  | tr -d '\n' | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
```

Read customer profile:

```bash
curl -i "$BASE_URL/auth/airline/users?email=$EMAIL" \
  -H "Authorization: Bearer $AIRLINE_TOKEN" \
  -H "x-correlation-id: $CORRELATION_ID"
```
