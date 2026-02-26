# SkyFly Flight Booking Platform

## Latest Implementation Update (Feb 26, 2026)

### UI Status (`skyfly-flight-booking`)
- Mock data removed from service layer and replaced with real HTTP integrations.
- UI is wired to live local services:
  - `auth-service` (`8081`)
  - `flight-service` (`8082`)
  - `booking-service` (`8083`)
  - `customer-service` (`8084`)
  - `payment-service` (`8085`)
- Vite proxy routes are configured in UI for local development:
  - `/auth-api`, `/flight-api`, `/booking-api`, `/customer-api`, `/payment-api`, `/gateway-api`

### Current Business Flow Available in UI
1. Search flights
2. Select flight
3. Login/Register (auth gating)
4. Enter passenger and contact details
5. Payment intent -> authorize -> capture
6. Booking confirmation page
7. My Bookings page (list + cancel)
8. Profile page (load + update)

### Run Order (Local)
1. Start services on configured ports (`8081`-`8085`, optional gateway on `8000`).
2. Start UI:
   - `cd skyfly-flight-booking`
   - `npm install`
   - `npm run dev`
3. Open `http://localhost:5173`

### Notes
- Flight search airport dropdown is now API-driven from live `flight-service` search response (unique `from/to` codes), not hardcoded options.
- Backend currently exposes airport codes in search payload; city/master metadata can be fully API-driven once a dedicated airport master endpoint is added.

## Short Description of SkyFly Flight Booking App
SkyFly is an airline booking platform for customers and airline staff. It supports user login, flight discovery, fare quote, booking, payment, and trip notifications.

## Polyglot Architecture Plan for All Services (Max 6)
| Service | Framework + DB | Purpose | Middleware / Platform Services |
|---|---|---|---|
| `api-gateway` | Kong or Traefik + N/A | Single entry point for routing, authentication enforcement, throttling, and API protection. | WAF, OAuth2/OIDC policy, rate limiter, service discovery |
| `auth-service` | Spring Boot + PostgreSQL | Handles customer/staff login, token lifecycle, session control, and access policies. | Keycloak, Redis, Kafka, Vault/KMS |
| `flight-service` | Quarkus + MySQL | Combines flight search, inventory availability, and pricing logic for low-latency shopping. | Redis cache, Kafka, OpenTelemetry |
| `booking-service` | .NET 8 + MSSQL | Owns reservation/PNR state, booking confirmation, cancel/change workflow orchestration. | Kafka, outbox pattern, saga orchestration |
| `customer-service` | Node.js + MongoDB | Manages customer profile/preferences and sends email/SMS/push notifications. | Kafka, SMTP/SMS providers, template engine |
| `payment-service` | Python FastAPI + PostgreSQL | Executes payment intent/authorize/capture/refund and settlement workflows. | Payment provider adapters, Kafka, idempotency middleware |

## Local Port Mapping (Verified)
| Service | App Port | Database | DB Port | Verified From |
|---|---|---|---|---|
| `auth-service` | `8081` | `postgres (auth)` | `5432` | `application.yaml` + app/infra compose |
| `flight-service` | `8082` | `mysql` | `3306` | `application.properties` + app/infra compose |
| `booking-service` | `8083` | `mssql` | `1433` | `Program.cs` + app/infra compose |
| `customer-service` | `8084` | `mongodb` | `27017` | `index.js` + app/infra compose |
| `payment-service` | `8085` | `postgres` (planned) | `5432` | app compose + README stack plan |
| `api-gateway` | `8000`, `8443`, `8001` | `postgres (kong)` | `5434` | infra compose |

### Shared Platform Ports
| Service | Port(s) |
|---|---|
| `kafka` | `9092`, `9093` |
| `keycloak` | `8090` (container `8080`) |
| `postgres (keycloak)` | `5433` |
| `redis` | `6379` |
| `elasticsearch` | `9200` |
| `kibana` | `5601` |

Coverage note:
- Frameworks covered: `Spring Boot`, `Quarkus`, `.NET 8`, `Node.js`, `Python FastAPI`.
- Databases covered: `PostgreSQL`, `MSSQL`, `MySQL`, `MongoDB`.

### Why This Stack (Framework + DB Combination)
| Service | Why This Combination |
|---|---|
| `auth-service` (`Spring Boot + PostgreSQL`) | Strong security ecosystem and ACID consistency for auth/session-critical operations. |
| `flight-service` (`Quarkus + MySQL`) | Quarkus gives low-latency runtime behavior; MySQL performs well for high-read commerce queries. |
| `booking-service` (`.NET 8 + MSSQL`) | Reliable async orchestration with strong enterprise transaction and governance capabilities. |
| `customer-service` (`Node.js + MongoDB`) | Rapid iteration for profile/notification features and flexible schema for evolving user data. |
| `payment-service` (`Python FastAPI + PostgreSQL`) | Fast development of payment workflows with dependable transactional persistence. |

## API Endpoint Plans for All Services
- Gateway:
  - `GET /api/v1/health`
- Auth:
  - `GET /api/v1/auth/public/google/start`
  - `GET /api/v1/auth/public/google/callback`
  - `POST /api/v1/auth/token/refresh`
  - `POST /api/v1/auth/logout`
  - `POST /api/v1/auth/corp/login/init`
  - `POST /api/v1/auth/corp/login/verify`
  - `POST /api/v1/auth/corp/mfa/challenge`
  - `POST /api/v1/auth/corp/mfa/verify`
- Flight:
  - `GET /api/v1/flights/search`
  - `GET /api/v1/flights/{flightId}`
  - `GET /api/v1/flights/{flightId}/availability`
  - `POST /api/v1/pricing/quote`
  - `POST /api/v1/inventory/hold`
  - `POST /api/v1/inventory/release`
  - `POST /api/v1/inventory/commit`
- Booking:
  - `POST /api/v1/bookings/reserve`
  - `POST /api/v1/bookings/{bookingId}/confirm`
  - `GET /api/v1/bookings/{bookingId}`
  - `GET /api/v1/bookings`
  - `POST /api/v1/bookings/{bookingId}/cancel`
  - `POST /api/v1/bookings/{bookingId}/change`
- Customer:
  - `GET /api/v1/users/me`
  - `PATCH /api/v1/users/me`
  - `POST /api/v1/users/me/mobile/verify/request`
  - `POST /api/v1/users/me/mobile/verify/confirm`
  - `POST /api/v1/notifications/email`
  - `POST /api/v1/notifications/sms`
  - `POST /api/v1/notifications/push`
- Payment:
  - `POST /api/v1/payments/intent`
  - `POST /api/v1/payments/{paymentId}/authorize`
  - `POST /api/v1/payments/{paymentId}/capture`
  - `POST /api/v1/payments/{paymentId}/refund`
  - `POST /api/v1/payments/webhooks/provider`

## End-to-End Workflow Details
- Customer booking flow:
  1. Login via `auth-service`.
  2. Search and quote via `flight-service`.
  3. Reserve booking via `booking-service` and hold seats through `flight-service`.
  4. Process payment via `payment-service`.
  5. Confirm booking and commit inventory.
  6. Send confirmation and manage profile updates via `customer-service`.
- Cancellation/refund flow:
  1. Cancel request in `booking-service`.
  2. Release seat/inventory in `flight-service`.
  3. Refund in `payment-service`.
  4. Notify customer via `customer-service`.
- Staff operations flow:
  1. Staff login with MFA in `auth-service`.
  2. Authorized operations on commerce and bookings through gateway-enforced roles.

## Security and Compliance Plan Across All Services
- Security baseline:
  - mTLS for service-to-service traffic.
  - OAuth2/OIDC JWT validation at gateway and service edges.
  - Centralized secret management via Vault/KMS.
  - WAF, DDoS controls, and adaptive rate limiting.
- Service controls:
  - Identity: brute-force protection, refresh-token rotation, session revocation.
  - Commerce/Booking: idempotency keys, optimistic locking, anti-tamper checks.
  - Payment: PCI-scope isolation, signed webhook validation, strict audit trails.
  - Engagement: PII protection, template allow-listing, communication abuse controls.
- Compliance:
  - GDPR/DPDP data export and erasure flows.
  - PCI-DSS controls for payment boundaries.
  - SOC 2 style logging, access governance, and incident response records.

## Additional Practical Suggestions and Phased Roadmap
### MVP vs Later Phase
| Capability | MVP (Initial Release) | Later Phase |
|---|---|---|
| Core 6 services | Required | Continue scaling and decomposition if needed |
| Kafka event backbone | Optional (limited events) | Required for full decoupling and analytics |
| Policy engine (`OPA`) | Optional | Recommended for centralized authz |
| Workflow/Saga engine | Optional | Recommended for advanced booking/payment orchestration |
| Dedicated audit analytics service | Optional | Add when regulatory/reporting needs grow |

- Practical suggestions:
  - Keep one immutable internal `userId` across all services.
  - Enforce OpenAPI-first contracts and shared error model.
  - Implement outbox + idempotent consumer pattern from day one.
  - Add contract tests and tracing before production rollout.
  - See detailed ownership split: `USER_DATA_OWNERSHIP_CONTRACT.md`.
- Phased roadmap:
  1. Build and stabilize the core 6 services.
  2. Add Kafka-driven async workflows and reliability hardening.
  3. Add advanced governance (OPA, compliance automation, deeper observability).
