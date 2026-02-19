# Flight Booking - Polyglot Microservices + OTel Demo

## 1. Project Overview
This repository is a polyglot flight-booking demo intended to showcase end-to-end OpenTelemetry across multiple frameworks.

Target service landscape:

| Service | Framework | Primary DB | Key Integrations | Comm Mode | Source of Truth | Owner | SLO Target | OTel Readiness |
|---|---|---|---|---|---|---|---|---|
| Auth Service | Spring Boot | PostgreSQL | Keycloak, Gateway | Sync (HTTP) | User identity/profile | IAM Team | 99.9%, p95 < 200ms | Planned |
| Search Service | Node.js | Elasticsearch | Inventory, Schedule feed | Sync (HTTP) | Search index (derived) | Discovery Team | 99.9%, p95 < 300ms | Planned |
| Booking Service | Quarkus | MySQL | Auth, Inventory, Payment, Notification | Sync + Async | Booking lifecycle (PNR/status) | Booking Team | 99.95%, p95 < 400ms | Planned |
| Inventory Service | .NET 8 | SQL Server | Booking, Search | Sync (HTTP) | Seat availability | Inventory Team | 99.95%, p95 < 250ms | Planned |
| Payment Service | Go Gin | PostgreSQL | Booking, external payment gateway (mock for demo) | Sync (HTTP) | Payment transaction status | Payments Team | 99.95%, p95 < 300ms | Planned |
| Notification Service | Python FastAPI | Redis | Kafka, Booking, Payment | Async (Kafka) + Sync (HTTP) | Notification delivery status | Engagement Team | 99.9%, p95 < 500ms | Planned |

Optional supporting domain service:

| Service | Framework | Primary DB | Key Integrations | Comm Mode | Source of Truth | Owner | SLO Target | OTel Readiness |
|---|---|---|---|---|---|---|---|---|
| Schedule Service (Optional) | Node.js/Go | PostgreSQL | Search, Inventory | Async + Sync | Flight timetable/master schedule | Ops Platform Team | 99.9%, p95 < 300ms | Planned |

## 2. Current Repository State
Implemented code:
- `authservice` (Spring Boot)
- `bookingservice` (Quarkus)
- `inventoryservice` (.NET 8 minimal API)
- `infra` (shared infrastructure stack)

Planned/not fully implemented:
- `searchservice` (empty scaffold)
- `paymentservice`
- `notificationservice`
- End-to-end React UI
- Full OpenTelemetry integration

## 3. Repository Structure
- `authservice/` - Spring Boot auth APIs
- `bookingservice/` - Quarkus booking APIs
- `inventoryservice/` - .NET API (template + Swagger)
- `searchservice/` - placeholder for Node.js service
- `infra/` - infra compose (Kafka, DBs, Keycloak, Kong, Elasticsearch)

## 4. API Endpoints (Flight Booking Contract)

Base path strategy (recommended for Track 1):
- Gateway: `http://localhost:8000`
- Service direct URLs (dev):
  - Auth: `http://localhost:8080`
  - Booking: `http://localhost:8081`
  - Inventory: `http://localhost:8082` (target for Track 1)
  - Search: `http://localhost:8083` (target for Track 1)
  - Payment: `http://localhost:8084` (target for Track 1)
  - Notification: `http://localhost:8085` (target for Track 1)

Common headers:
- `Content-Type: application/json`
- `Authorization: Bearer <token>` (for protected endpoints)
- `x-correlation-id: <uuid>` (recommended)

### 4.1 Auth Service (`/auth`)
| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/auth/register` | Register new user |
| POST | `/auth/login` | Authenticate and return token |
| GET | `/auth/profile` | Get current user profile |
| PUT | `/auth/profile` | Update profile |
| POST | `/auth/logout` | Logout user session |

Register request:
```json
{ "name": "Mukesh", "email": "mukesh@test.com", "password": "Secret@123", "phone": "+91-9999999999" }
```

Login response:
```json
{ "accessToken": "jwt-token", "tokenType": "Bearer", "expiresIn": 3600, "userId": "u_1001" }
```

### 4.2 Search Service (`/search`)
| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/search/flights?from=BLR&to=DEL&date=2026-03-01&adults=1&cabin=ECONOMY` | Search flights |
| GET | `/search/flights/{flightId}` | Get flight details |
| GET | `/search/airports?query=ben` | Search airports |

Search response:
```json
{
  "flights": [
    {
      "flightId": "AI202",
      "airline": "Air India",
      "from": "BLR",
      "to": "DEL",
      "date": "2026-03-01",
      "departureTime": "10:30",
      "arrivalTime": "13:15",
      "durationMinutes": 165,
      "fare": { "amount": 6200, "currency": "INR" },
      "availableSeats": 14
    }
  ]
}
```

### 4.3 Inventory Service (`/inventory`)
| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/inventory/flights/{flightId}/seats` | Get current seat availability |
| POST | `/inventory/hold` | Hold seats before payment |
| POST | `/inventory/confirm-hold` | Convert hold to booked seats |
| POST | `/inventory/release` | Release held/booked seats |

Hold request:
```json
{ "flightId": "AI202", "seatCount": 1, "cabin": "ECONOMY", "ttlSeconds": 600 }
```

Hold response:
```json
{ "holdId": "hold_9001", "flightId": "AI202", "status": "HELD", "expiresAt": "2026-03-01T10:40:00Z" }
```

### 4.4 Booking Service (`/booking`)
| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/booking/reserve` | Create reservation + seat hold |
| POST | `/booking/confirm/{pnr}` | Confirm booking after payment |
| PUT | `/booking/{pnr}/cancel` | Cancel booking and trigger refund/release |
| GET | `/booking/my-bookings` | List user bookings |
| GET | `/booking/{pnr}` | Get booking by PNR |

Reserve request:
```json
{
  "userId": "u_1001",
  "flightId": "AI202",
  "travelDate": "2026-03-01",
  "passengers": [
    { "firstName": "Mukesh", "lastName": "K", "age": 30, "gender": "M" }
  ],
  "contact": { "email": "mukesh@test.com", "phone": "+91-9999999999" }
}
```

Reserve response:
```json
{ "pnr": "PNR12345", "bookingStatus": "RESERVED", "holdId": "hold_9001", "expiresIn": 600 }
```

Confirm response:
```json
{
  "pnr": "PNR12345",
  "bookingStatus": "CONFIRMED",
  "paymentStatus": "CAPTURED",
  "ticketNumbers": ["0987654321123"]
}
```

### 4.5 Payment Service (`/payment`)
| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/payment/authorize` | Authorize amount before confirmation |
| POST | `/payment/capture` | Capture payment for confirmed booking |
| POST | `/payment/refund` | Refund cancelled booking |
| GET | `/payment/transactions/{transactionId}` | Check payment transaction status |

Authorize request:
```json
{
  "pnr": "PNR12345",
  "amount": 6200,
  "currency": "INR",
  "method": "CARD",
  "cardLast4": "4242"
}
```

Capture response:
```json
{ "transactionId": "txn_7001", "status": "CAPTURED", "capturedAmount": 6200 }
```

### 4.6 Notification Service (`/notifications`)
| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/notifications/send` | Send booking-related notification |
| GET | `/notifications/{notificationId}` | Check notification status |
| POST | `/notifications/preferences` | Save user notification preferences |

Send request:
```json
{
  "eventType": "BOOKING_CONFIRMED",
  "channel": "EMAIL",
  "recipient": "mukesh@test.com",
  "templateId": "booking-confirmed-v1",
  "data": { "pnr": "PNR12345", "flightId": "AI202", "travelDate": "2026-03-01" }
}
```

Send response:
```json
{ "notificationId": "n_3001", "status": "QUEUED" }
```

### 4.7 Health Endpoints (All Services)
| Service | Endpoint |
|---|---|
| Auth | `/auth/health` |
| Search | `/search/health` |
| Booking | `/booking/health` |
| Inventory | `/inventory/health` |
| Payment | `/payment/health` |
| Notification | `/notifications/health` |

Required health endpoint contract (all services):
- Method: `GET`
- Authentication: none (internal network/gateway restricted)
- Response `Content-Type`: `application/json`
- Must return:
  - Overall status: `UP` or `DOWN`
  - Service name and version
  - Environment (`dev`, `staging`, `prod`)
  - Timestamp (UTC)
  - Uptime seconds
  - Dependency checks with per-dependency status and latency

Required dependency checks by service:
- Auth:
  - PostgreSQL connectivity
  - Keycloak reachability
- Search:
  - Elasticsearch connectivity
- Booking:
  - MySQL connectivity
  - Reachability checks to Inventory and Payment (lightweight ping)
- Inventory:
  - SQL Server connectivity
- Payment:
  - PostgreSQL connectivity
  - Payment provider/mock gateway reachability
- Notification:
  - Redis connectivity
  - Kafka broker connectivity

Status code rules:
- `200 OK`: service healthy and critical dependencies healthy
- `503 Service Unavailable`: service degraded/unhealthy

Recommended split (platform readiness):
- Liveness endpoint: `GET /<service>/health/live`
  - Checks process is running only (no external dependencies)
- Readiness endpoint: `GET /<service>/health/ready`
  - Includes critical dependency checks
- Keep `GET /<service>/health` as aggregate summary for humans/dashboards

Standard health response example:
```json
{
  "service": "bookingservice",
  "version": "1.0.0",
  "environment": "dev",
  "status": "UP",
  "timestamp": "2026-02-15T12:00:00Z",
  "uptimeSeconds": 1842,
  "checks": [
    { "name": "mysql", "status": "UP", "latencyMs": 12 },
    { "name": "inventory-api", "status": "UP", "latencyMs": 24 },
    { "name": "payment-api", "status": "UP", "latencyMs": 31 }
  ]
}
```

### 4.8 Standard Error Model (Use for all services)
```json
{
  "timestamp": "2026-02-15T12:00:00Z",
  "path": "/booking/confirm/PNR12345",
  "code": "PAYMENT_FAILED",
  "message": "Payment authorization failed",
  "correlationId": "a3c4d6f2-9b11-4d8a-9d95-9f82ea2c5f1f"
}
```

## 5. Quick Start (Current State)

Run auth service:
```bash
cd authservice
./mvnw spring-boot:run
```

Run booking service:
```bash
cd bookingservice
./mvnw quarkus:dev
```

Run inventory service:
```bash
cd inventoryservice
dotnet run
```

Run infra stack (optional):
```bash
docker compose -f infra/infra-dockercompose.yaml up -d
```

## 6. Kong Setup (Current Commands)

Register auth service:
```bash
curl -i -X POST http://localhost:8001/services \
  --data "name=auth-service" \
  --data "url=http://auth-service:8080"

SERVICE_ID=$(curl -s http://localhost:8001/services/auth-service | jq -r .id)
curl -i -X POST http://localhost:8001/services/$SERVICE_ID/routes \
  --data "paths[]=/auth" \
  --data "methods[]=GET" \
  --data "methods[]=POST" \
  --data "methods[]=PUT" \
  --data "strip_path=false"
```

Register booking service:
```bash
curl -i -X POST http://localhost:8001/services \
  --data "name=flightservice-booking" \
  --data "url=http://flightservice-booking:8081"

SERVICE_ID=$(curl -s http://localhost:8001/services/flightservice-booking | jq -r .id)
curl -i -X POST http://localhost:8001/services/$SERVICE_ID/routes \
  --data "paths[]=/booking" \
  --data "methods[]=GET" \
  --data "methods[]=POST" \
  --data "methods[]=PUT" \
  --data "strip_path=false"
```

Inspect Kong:
```bash
curl -X GET http://localhost:8001/services
curl -X GET http://localhost:8001/routes
```

## 7. Curl Checks (Via Kong Proxy)

Register + login:
```bash
curl -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Mukesh","email":"mukesh@test.com","password":"Secret@123"}'

curl -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"mukesh@test.com","password":"Secret@123"}'
```

Search flights:
```bash
curl "http://localhost:8000/search/flights?from=BLR&to=DEL&date=2026-03-01&adults=1&cabin=ECONOMY"
```

Reserve and confirm booking:
```bash
curl -X POST http://localhost:8000/booking/reserve \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: demo-12345" \
  -d '{"userId":"u_1001","flightId":"AI202","travelDate":"2026-03-01","passengers":[{"firstName":"Mukesh","lastName":"K","age":30}],"contact":{"email":"mukesh@test.com","phone":"+91-9999999999"}}'

curl -X POST http://localhost:8000/booking/confirm/PNR12345 \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: demo-12345" \
  -d '{"paymentMethod":"CARD","amount":6200,"currency":"INR"}'
```

Cancel booking:
```bash
curl -X PUT http://localhost:8000/booking/PNR12345/cancel \
  -H "Content-Type: application/json" \
  -H "x-correlation-id: demo-12345"
```

## 8. OpenTelemetry Status
Current status:
- No explicit OTel SDK/exporter dependency in service manifests.
- No collector config in repo yet.
- No OTLP env wiring in current service configs.

Conclusion:
- This repo is currently a polyglot microservice bootstrap, not yet a complete OTel demo.

## 9. Master Implementation Plan

Goal:
- Build a full demo with 6 services + React UI + distributed tracing/metrics/logs.

Phases:
1. Baseline and scaffolding
- Add missing services (`searchservice`, `paymentservice`, `notificationservice`).
- Add unified compose startup profile.

2. Functional MVP APIs
- Implement minimal domain endpoints for all services.
- Ensure one happy-path booking flow works.

3. Persistence and messaging
- Wire DBs and Kafka topics.
- Add basic data initialization.

4. OTel instrumentation
- Spring Boot, Quarkus, .NET, Node, Go, Python instrumentation.
- Add OpenTelemetry Collector + backend (Jaeger/Tempo + Prometheus + Grafana).

5. Demo hardening
- Add smoke tests, failure scenarios, runbook documentation.

Acceptance criteria:
- All 6 services run.
- React UI drives complete booking journey.
- One distributed trace spans sync + async hops.

## 10. Track 1 Plan (Working Demo + React UI)

Track 1 objective:
- Deliver working end-to-end flow with React UI and minimal service integrations.

In scope:
- React app with login/search/reserve/confirm pages.
- Missing service stubs (Node/Go/Python) with in-memory logic.
- Booking orchestration across inventory/payment/notification.
- Compose run + smoke validation.

Execution order:
1. Freeze API contracts.
2. Scaffold missing services.
3. Implement booking orchestration.
4. Build React screens against fixed contracts.
5. Integrate and run via compose.
6. Add smoke script and docs.

Track 1 exit criteria:
- UI can complete happy-path booking.
- All 6 services participate.
- Startup/demo steps are documented and reproducible.

## 11. Track 1 User Stories + Task Status

Status legend:
- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

| ID | User Story | Status |
|---|---|---|
| US-101 | User can open React app and view working home page | Not Started |
| US-102 | User can register/login from UI | Not Started |
| US-103 | User can search flights from UI | Not Started |
| US-104 | User can reserve/confirm booking from UI | Not Started |
| US-201 | Search service returns flights (`GET /search/flights`) | Not Started |
| US-202 | Payment service supports authorize/capture | Not Started |
| US-203 | Notification service supports send endpoint | Not Started |
| US-204 | Inventory service supports seat hold endpoint | Not Started |
| US-205 | Booking service orchestrates inventory/payment/notification | Not Started |
| US-301 | All services run together in Docker Compose | Not Started |
| US-302 | Full demo flow works end-to-end from UI | Not Started |
| US-303 | README + smoke validation script complete | Not Started |

Progress summary:
- Total stories: `12`
- Done: `0`
- In Progress: `0`
- Blocked: `0`
- Not Started: `12`

Daily update format:
```md
Date: YYYY-MM-DD
- US-101: Done
- US-201: In Progress
- US-205: Blocked (waiting for inventory hold contract)
```

## 12. Next Action
Start Track 1 Phase A:
1. Scaffold `ui/` (Vite + React + TypeScript).
2. Add base routing and API client.
3. Verify UI can call `authservice` and `bookingservice` endpoints.
