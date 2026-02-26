# User Data Ownership Contract

This document defines ownership boundaries between `auth-service` and `customer-service` to avoid duplicated authority and conflicting updates.

## Scope
- `auth-service` (Spring Boot + PostgreSQL): identity, credentials, sessions, auth state.
- `customer-service` (Node.js + MongoDB): profile, preferences, communication settings, notification history.

## Canonical Identity Rules
- `userId` is immutable and globally unique across all services.
- `auth-service` is the source of truth for identity lifecycle.
- `customer-service` stores `userId` as reference key only (never re-keys or regenerates).

## Data Ownership Matrix
| Data Domain | Owner Service | Storage | Notes |
|---|---|---|---|
| Login identifier (`email`/username for login) | `auth-service` | PostgreSQL | Used for authentication and account recovery |
| Password/MFA secrets | `auth-service` | PostgreSQL/IdP | Never copied to other services |
| Session/refresh token state | `auth-service` | PostgreSQL/Redis | Includes revocation and rotation status |
| Account status (`ACTIVE`, `LOCKED`, `DISABLED`) | `auth-service` | PostgreSQL | Authorization-critical flag |
| User profile (`firstName`, `lastName`, `dob`, etc.) | `customer-service` | MongoDB | Business profile fields for customer experience |
| Preferences (`seat`, `meal`, `language`, etc.) | `customer-service` | MongoDB | Flexible schema |
| Communication preferences (`emailOptIn`, `smsOptIn`, push settings) | `customer-service` | MongoDB | Consent and channel policy |
| Notification records | `customer-service` | MongoDB | Delivery logs, template usage, provider IDs |
| Verification state for business profile contactability | Shared (see below) | Split | Auth owns identity verification trust; customer owns channel usability state |

## Shared Fields With Authority
- `email`:
  - Auth authority: login identity and verification trust for sign-in/security.
  - Customer copy: display/contact value for communication only.
- `mobile`:
  - Auth authority: if used for MFA/authentication challenges.
  - Customer authority: preferred reachable phone for notifications.
- Conflict rule:
  - If a field is auth-critical, auth wins.
  - If a field is engagement-only, customer wins.

## API Boundary Rules
- Other services must not update identity/security fields through `customer-service`.
- Other services must not update profile/preferences through `auth-service`.
- `GET /api/v1/users/me` should compose:
  - Identity-safe subset from `auth-service` (id, account status, verified flags as needed).
  - Profile/preferences from `customer-service`.
- Write paths:
  - Auth updates: auth endpoints only.
  - Profile updates: customer endpoints only.

## Event Contract (Recommended)
- Publisher: `auth-service`
  - `user.created`
  - `user.identity.updated`
  - `user.account.status.changed`
  - `user.deleted`
- Publisher: `customer-service`
  - `user.profile.updated`
  - `user.preferences.updated`
  - `user.notification.preference.updated`

### Minimum event envelope
```json
{
  "eventId": "uuid",
  "eventType": "user.profile.updated",
  "occurredAt": "2026-02-25T00:00:00Z",
  "userId": "U-123",
  "version": 1,
  "source": "customer-service",
  "payload": {}
}
```

## Consistency and Idempotency
- Use outbox pattern in each owner service for event publishing.
- Consumers must be idempotent on `eventId`.
- Use per-user versioning (`version`) to reject stale writes.
- Do not perform distributed cross-service DB transactions.

## Deletion and Compliance
- `auth-service` initiates hard/legal identity deletion events when required.
- `customer-service` performs profile erasure/anonymization based on `user.deleted`.
- Retention exceptions (audit/legal hold) must be explicit per domain.

## Anti-Patterns to Avoid
- Storing password hashes or refresh tokens in `customer-service`.
- Letting profile service toggle account lock/unlock.
- Duplicating mutable identity state in multiple services as co-authoritative.
- Synchronous call chains for every read without cache/composition strategy.
