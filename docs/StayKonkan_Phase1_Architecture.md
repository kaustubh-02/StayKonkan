# StayKonkan — Phase 1: Startup Planning & System Architecture

**Document Type:** Software Architecture Document (SAD)
**Product:** StayKonkan — Travel & Hotel Booking Platform for Alibag, Nagaon, Murud, Kashid & Konkan
**Prepared for:** Founding Team
**Status:** Draft v1.0 — Foundation Phase

---

## 1. Functional Requirements

### 1.1 Customer-Facing
- FR1: Search hotels by destination (Alibag, Nagaon, Murud, Kashid, etc.), date range, guest count, and price range.
- FR2: View hotel detail pages — photos, amenities, room types, pricing, location map, policies, reviews.
- FR3: Submit a **Booking Request** (not instant booking) specifying dates, room type, guests, contact info.
- FR4: Track booking request status: `PENDING → CONTACTED → CONFIRMED → ADVANCE_PAID → COMPLETED / CANCELLED`.
- FR5: Receive notifications (email/SMS/WhatsApp-ready) at each status change.
- FR6: Pay advance amount via a payment link (manual reconciliation in MVP, gateway-ready).
- FR7: View booking history and invoices/receipts.
- FR8: Leave a review after stay completion.
- FR9: User registration/login (email + OTP or password), guest checkout allowed for booking requests.
- FR10: Contact/support form and WhatsApp click-to-chat.

### 1.2 Admin-Facing
- FR11: View all incoming booking requests in a queue (sorted by urgency/date).
- FR12: Assign a booking request to self or another admin/staff member.
- FR13: Record communication log with hotel owner (call notes, confirmation status).
- FR14: Manually confirm/reject a booking on behalf of the hotel.
- FR15: Record advance payment received (amount, mode, reference).
- FR16: Mark booking as Confirmed, Completed, or Cancelled with reason codes.
- FR17: Manage hotel inventory: add/edit/deactivate hotels, rooms, pricing, images.
- FR18: Manage commission rules per hotel (percentage or flat).
- FR19: View commission earned reports (daily/weekly/monthly, per hotel).
- FR20: Manage users and roles (admin, staff, hotel owner future).

### 1.3 Hotel Owner-Facing (Phase 2, architected now)
- FR21: View/manage own hotel's bookings and calendar.
- FR22: Update room availability and pricing.
- FR23: View payout/commission statements.

### 1.4 Platform-Wide
- FR24: Full-text and filter-based hotel search with pagination.
- FR25: Image upload/management via Cloudinary (hotel photos, gallery).
- FR26: Audit trail for every booking status change (who changed what, when).
- FR27: Role-based access control across all modules.

---

## 2. Non-Functional Requirements

| Category | Requirement |
|---|---|
| **Performance** | API response time < 300ms (p95) for read endpoints; search results < 1s |
| **Scalability** | Support 10,000+ concurrent users without architecture change; horizontal scaling of stateless services |
| **Availability** | 99.5% uptime target for MVP; 99.9% post Phase 2 |
| **Security** | JWT-based auth, RBAC, OWASP Top 10 compliance, encrypted secrets, HTTPS-only |
| **Maintainability** | Clean Architecture, SOLID, modular monolith enabling future microservice extraction |
| **Portability** | Fully Dockerized; deployable to AWS, GCP, or on-prem with no code change |
| **Usability** | Mobile-first responsive UI; WCAG AA accessibility target |
| **Observability** | Centralized structured logging, request tracing, health checks, metrics-ready (Prometheus/Grafana future) |
| **Data Integrity** | ACID-compliant PostgreSQL, DB-level constraints, optimistic locking on booking state transitions |
| **Auditability** | Immutable audit log for all booking/commission transactions |
| **Extensibility** | New verticals (cabs, restaurants) addable as new modules/services without touching core booking domain |
| **Compliance** | GST-ready invoicing, data retention & privacy policy readiness (DPDP Act, India) |

---

## 3. Business Workflow

```
[Customer]
   │  Search hotels (destination, dates, guests)
   ▼
[Search Results] ──> [Hotel Detail Page]
   │  Submit Booking Request
   ▼
[Booking Request: PENDING]
   │  Admin picks up request
   ▼
[Admin Reviews Request] ──> [Admin Contacts Hotel Owner] (phone/manual)
   │
   ├── Hotel unavailable ──> [Booking: REJECTED] ──> Notify customer, suggest alternatives
   │
   ▼ Hotel confirms
[Admin Contacts Customer] ──> Shares price, advance amount, payment link
   │
   ▼ Customer pays advance
[Admin Records Payment] ──> [Booking: CONFIRMED]
   │
   ▼ Stay date arrives / completes
[Booking: COMPLETED] ──> Commission recorded ──> Review requested from customer
```

**Key principle:** The booking state machine is the core domain object. Every future automation (instant booking, payment gateway, hotel owner self-service) simply changes *who or what triggers* a state transition — not the states themselves. This is why the state machine is designed as a standalone domain service from day one.

---

## 4. Use Case Diagram (Textual)

**Actors:** Customer, Admin/Staff, Hotel Owner (future), System (automated jobs)

```
                        ┌────────────────────────┐
                        │        StayKonkan       │
                        └────────────────────────┘
Customer
 ├── Search Hotels
 ├── View Hotel Details
 ├── Submit Booking Request
 ├── Track Booking Status
 ├── Make Advance Payment
 ├── View Booking History
 ├── Submit Review
 └── Register / Login

Admin / Staff
 ├── View Booking Queue
 ├── Assign Booking Request
 ├── Log Hotel Owner Communication
 ├── Confirm / Reject Booking
 ├── Record Advance Payment
 ├── Manage Hotel Inventory
 ├── Manage Commission Rules
 ├── View Commission Reports
 └── Manage Users & Roles

Hotel Owner (Phase 2)
 ├── View Own Bookings
 ├── Update Availability & Pricing
 └── View Payout Statements

System (Automated)
 ├── Send Notifications (email/SMS/WhatsApp)
 ├── Generate Invoices
 └── Run Scheduled Reports
```

---

## 5. System Architecture (Overview)

**Architecture Style:** **Modular Monolith** (see Section 13 for rationale) with clear bounded contexts, deployed as a single Spring Boot application behind Nginx, with a decoupled React SPA frontend.

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                          │
│   React 19 + Vite + TS SPA  (Web) — Mobile-responsive        │
└───────────────────────────┬───────────────────────────────────┘
                             │ HTTPS / REST (JSON) + JWT
┌───────────────────────────▼───────────────────────────────────┐
│                       EDGE / GATEWAY LAYER                     │
│      Nginx (Reverse Proxy, TLS termination, static assets,     │
│      rate limiting, gzip, load balancing entry point)          │
└───────────────────────────┬───────────────────────────────────┘
                             │
┌───────────────────────────▼───────────────────────────────────┐
│                     APPLICATION LAYER                          │
│         Spring Boot Modular Monolith (Java 21)                 │
│  ┌───────────┬───────────┬───────────┬───────────┬──────────┐ │
│  │  Identity │  Hotel    │  Booking  │ Commission│  Media    │ │
│  │  Module   │  Module   │  Module   │  Module   │  Module   │ │
│  └───────────┴───────────┴───────────┴───────────┴──────────┘ │
│  Cross-cutting: Security (Spring Security+JWT), Logging,       │
│  Exception Handling, Auditing, Notification Service             │
└───────┬───────────────────────────┬─────────────────────┬─────┘
        │                           │                     │
┌───────▼────────┐        ┌─────────▼────────┐   ┌────────▼───────┐
│  PostgreSQL     │        │   Cloudinary      │   │  Notification   │
│  (Primary DB)   │        │  (Image/Media CDN)│   │  Provider(s)     │
└─────────────────┘        └───────────────────┘   │ (Email/SMS/WA)   │
                                                     └──────────────────┘
```

**Future-ready seams (no core rewrite needed):**
- Booking Module already models `bookingSource: MANUAL | INSTANT` → enables Instant Booking later.
- Hotel Module already models `ownerId` → enables Hotel Owner Dashboard later.
- New verticals (Cab, Restaurant) become **new modules** implementing the same `Bookable` domain interface, reusing Booking, Commission, Identity, and Notification modules.
- Payment is abstracted behind a `PaymentGateway` interface — manual reconciliation today, Razorpay/Stripe tomorrow.
- Recommendation Module can be added as a read-only consumer of Booking + Search data (no write coupling).

---

## 6. High Level Architecture

**Layers (top to bottom):**

1. **Presentation Layer** — React SPA: pages, components, state (Redux Toolkit + React Query), forms (RHF + Zod), 3D/motion (R3F, GSAP, Framer Motion) for landing/marketing experience.
2. **API Gateway Layer** — Nginx: TLS, routing, caching static assets, rate limiting, reverse proxy to backend.
3. **Application/Service Layer** — Spring Boot REST Controllers → Service Layer (business logic, orchestration, state machine transitions).
4. **Domain Layer** — Core entities (Hotel, Room, BookingRequest, Commission, User), value objects, domain events (e.g., `BookingConfirmedEvent`).
5. **Persistence Layer** — Spring Data JPA / Hibernate Repositories → PostgreSQL.
6. **Integration Layer** — Cloudinary SDK (media), Notification providers, future Payment Gateway SDKs.
7. **Cross-Cutting Concerns** — Security (JWT filters), Logging (structured, correlation IDs), Exception Handling (global advice), Auditing (interceptors), Caching (Redis-ready).

**High-Level Component Diagram:**

```
React SPA ──REST/JSON──> Nginx ──> Spring Boot App
                                        │
                       ┌────────────────┼─────────────────┐
                       │                │                 │
                Controller Layer   Security Layer    Exception Layer
                       │
                 Service Layer (business rules, state machine)
                       │
                 Repository Layer (Spring Data JPA)
                       │
                   PostgreSQL
```

---

## 7. Low Level Architecture

### 7.1 Booking State Machine (Core Domain Logic)
```
Enum BookingStatus:
  PENDING → CONTACTED → OWNER_CONFIRMED → CUSTOMER_CONFIRMED →
  ADVANCE_PAID → CONFIRMED → COMPLETED
  (with REJECTED / CANCELLED reachable from PENDING through CONFIRMED)

BookingStateMachine (Service):
  - validateTransition(current, next): boolean
  - transition(bookingId, next, actor, metadata): BookingRequest
  - emits domain event on every transition (for audit + notification)
```

### 7.2 Entity Relationship (Core Tables)
```
User (id, name, email, phone, passwordHash, role, createdAt)
Hotel (id, ownerId[nullable-FK future], name, destination, description,
       address, geoLat, geoLng, amenities[], status, commissionRuleId)
Room (id, hotelId, type, capacity, basePrice, totalUnits)
BookingRequest (id, customerId, hotelId, roomId, checkIn, checkOut,
       guests, status, assignedAdminId, advanceAmount, totalAmount,
       source[MANUAL/INSTANT], createdAt, updatedAt)
BookingStatusHistory (id, bookingId, fromStatus, toStatus, actorId, note, timestamp)
CommissionRule (id, hotelId, type[PERCENT/FLAT], value)
CommissionLedger (id, bookingId, hotelId, amount, status, settledAt)
Payment (id, bookingId, amount, mode, referenceNo, recordedBy, recordedAt)
Review (id, bookingId, customerId, hotelId, rating, comment, createdAt)
MediaAsset (id, ownerType, ownerId, cloudinaryUrl, type, order)
AuditLog (id, entityType, entityId, action, actorId, diff, timestamp)
```

### 7.3 Sequence — Booking Request to Confirmation
```
Customer → API: POST /bookings (create request) → status=PENDING
API → DB: persist BookingRequest + BookingStatusHistory
API → NotificationService: notify Admin (new request)

Admin → API: PATCH /bookings/{id}/assign
Admin → API: PATCH /bookings/{id}/status (CONTACTED, OWNER_CONFIRMED ...)
   each call → StateMachine.validateTransition() → persist → audit → notify

Admin → API: POST /bookings/{id}/payments (advance recorded)
API → Service: recalculate → status=CONFIRMED
API → NotificationService: notify Customer (confirmed)
API → CommissionService: create CommissionLedger entry
```

---

## 8. Folder Structure (Conceptual — see Section 27 for full tree)

- `staykonkan-backend/` — Spring Boot modular monolith, package-by-feature
- `staykonkan-frontend/` — React + Vite + TS SPA, feature-based structure
- `infra/` — Docker, Nginx configs, deployment scripts
- `docs/` — Architecture docs, ADRs (Architecture Decision Records), API specs

---

## 9. Module Breakdown

| Module | Responsibility |
|---|---|
| **Identity** | Registration, login, JWT issuance/refresh, RBAC, user profile |
| **Hotel** | Hotel & room CRUD, availability, pricing, amenities, search/filter |
| **Booking** | Booking request lifecycle, state machine, assignment, history |
| **Commission** | Commission rules, ledger, settlement reports |
| **Payment** | Manual payment recording (MVP); gateway abstraction for future |
| **Media** | Cloudinary integration, image upload/management |
| **Notification** | Email/SMS/WhatsApp dispatch, templated messages, event listeners |
| **Review** | Post-stay review capture and display |
| **Admin/Reporting** | Dashboards, queues, commission reports, audit views |
| **Audit** | Cross-cutting change tracking for compliance |
| **(Future) Cab, Restaurant, Recommendation** | New modules reusing Booking/Commission/Identity contracts |

Each module follows: `controller → service → repository → domain` internally, with a well-defined public interface (facade) for cross-module calls — this is what allows clean extraction into microservices later if needed.

---

## 10. UI Navigation Flow

```
Landing Page (marketing, hero, destinations showcase w/ motion)
   │
   ├── Search Bar → Search Results Page (filters: destination, dates, price, guests)
   │        └── Hotel Detail Page (gallery, amenities, rooms, map)
   │                └── Booking Request Form → Confirmation Screen
   │                        └── My Bookings (status tracker) → Payment Link → Invoice
   │
   ├── Login/Register → OTP/Password → Redirect to previous intent
   │
   └── Admin Login → Admin Dashboard
            ├── Booking Queue → Booking Detail (assign, log, transition status)
            ├── Hotel Management (CRUD, image upload)
            ├── Commission Reports
            └── User Management
```

---

## 11. User Journey

**Customer Journey:**
1. Discovers StayKonkan via search/social/ads → lands on destination-rich homepage.
2. Searches "Nagaon Beach" for a weekend → browses filtered results.
3. Opens a hotel, likes the photos & price → submits Booking Request (name, phone, dates).
4. Receives WhatsApp/SMS: "We're checking availability with the hotel."
5. Gets a call/message: "Confirmed! Please pay ₹2,000 advance to lock your stay."
6. Pays via link → receives confirmation + invoice.
7. Enjoys stay → receives review request → leaves feedback.

**Admin Journey:**
1. Logs into dashboard, sees new booking request in queue.
2. Assigns to self, calls hotel owner, logs the outcome.
3. Calls/messages customer with confirmed price and payment link.
4. Marks payment received, booking auto-moves to Confirmed.
5. Reviews weekly commission report to track earnings per hotel.

---

## 12. Role Based Access Control (RBAC)

| Role | Permissions |
|---|---|
| **CUSTOMER** | Search, view, book, pay, review, manage own profile/bookings |
| **ADMIN** | Full access: hotels, bookings, commissions, users, reports |
| **STAFF** (sub-admin) | Manage assigned bookings, log communication, no financial/report access (configurable) |
| **HOTEL_OWNER** *(future)* | View/manage only own hotel's bookings, availability, payouts |
| **SUPER_ADMIN** *(future)* | Platform config, role management, module toggles |

Implementation: Spring Security with role + permission-based method security (`@PreAuthorize`), JWT claims carry role + userId; frontend route guards mirror backend permissions (never trust client-side only).

---

## 13. Microservice vs Monolith Recommendation

**Recommendation: Start with a Modular Monolith.**

**Why:**
- Team size and MVP stage don't justify microservice operational overhead (service discovery, distributed tracing, network latency, data consistency across services).
- A **well-modularized monolith** (package-by-feature, enforced module boundaries, facade interfaces) gives 90% of the benefits of microservices (separation of concerns, testability, independent module evolution) with 10% of the complexity.
- PostgreSQL with clean schema boundaries per module allows straightforward extraction later — each module already owns its own tables.
- Deployment, monitoring, and debugging are dramatically simpler for a small team.

**When to split into microservices (future triggers):**
- A specific module (e.g., Search or Notification) needs independent scaling due to load.
- Separate teams start owning separate modules and need independent deploy cycles.
- New verticals (Cabs, Restaurants) grow large enough to warrant their own service and database.

**Migration path:** Because modules communicate only through service-layer interfaces (never direct repository access across modules), any module can be lifted into its own Spring Boot service + database with minimal refactor — this is the core architectural insurance policy.

---

## 14. Design Patterns to Use

| Pattern | Where Used |
|---|---|
| **State Pattern** | Booking status lifecycle transitions |
| **Strategy Pattern** | Commission calculation (percent vs flat), Notification channel selection (email/SMS/WhatsApp) |
| **Factory Pattern** | Notification message creation, DTO/Entity mapping |
| **Facade Pattern** | Module-level public service interfaces (e.g., `BookingFacade`, `HotelFacade`) |
| **Repository Pattern** | Data access abstraction via Spring Data JPA |
| **Observer/Event-Driven** | Domain events (e.g., `BookingConfirmedEvent`) trigger notifications, commission ledger entry, audit log — using Spring's `ApplicationEventPublisher` |
| **DTO Pattern** | All API boundaries use DTOs, never expose JPA entities directly |
| **Builder Pattern** | Complex object construction (e.g., booking search criteria, notification payloads) |
| **Singleton (via Spring beans)** | Services, configuration beans |
| **Adapter Pattern** | Payment gateway abstraction, media provider abstraction (Cloudinary today, S3 tomorrow) |
| **Specification Pattern** | Dynamic hotel search filters (JPA Criteria/Specification API) |

---

## 15. SOLID Principles — Application

- **S — Single Responsibility:** Each service handles one concern (e.g., `BookingStateMachineService` only handles transitions; `NotificationService` only handles dispatch).
- **O — Open/Closed:** New commission strategies or notification channels are added via new Strategy implementations, not by modifying existing classes.
- **L — Liskov Substitution:** Payment gateway implementations (Manual, future Razorpay) are interchangeable behind `PaymentGateway` interface.
- **I — Interface Segregation:** Module facades expose only what other modules need (e.g., `HotelFacade.getHotelSummary()`), not full internal entities.
- **D — Dependency Inversion:** Services depend on repository/interface abstractions, not concrete implementations; enables easy testing/mocking.

---

## 16. Clean Architecture

```
┌───────────────────────────────────────────┐
│              Frameworks & Drivers          │  ← Spring Web, JPA, Cloudinary SDK, Nginx
│  ┌───────────────────────────────────────┐ │
│  │        Interface Adapters              │ │  ← Controllers, DTOs, Mappers, Repository impls
│  │  ┌───────────────────────────────────┐ │ │
│  │  │       Application/Use Cases        │ │ │  ← Services: CreateBooking, ConfirmBooking, etc.
│  │  │  ┌───────────────────────────────┐ │ │ │
│  │  │  │         Domain / Entities      │ │ │ │  ← BookingRequest, Hotel, CommissionRule
│  │  │  └───────────────────────────────┘ │ │ │
│  │  └───────────────────────────────────┘ │ │
│  └───────────────────────────────────────┘ │
└───────────────────────────────────────────┘
```

**Dependency Rule:** Inner layers (Domain, Use Cases) never depend on outer layers (frameworks, DB, web). Domain entities are POJOs free of JPA annotations where feasible, or isolated via mapper layers — enabling the domain logic to be tested without Spring context and portable if persistence technology changes.

---

## 17. Naming Conventions

**Backend (Java):**
- Packages: `com.staykonkan.<module>.<layer>` e.g. `com.staykonkan.booking.service`
- Classes: `PascalCase` — `BookingRequestService`, `HotelController`
- REST DTOs: suffix `Request`/`Response` — `CreateBookingRequest`, `BookingResponse`
- Entities: singular noun — `Hotel`, `BookingRequest`
- Interfaces: no `I` prefix; implementation suffixed if needed — `PaymentGateway` / `ManualPaymentGateway`
- Constants: `UPPER_SNAKE_CASE`
- REST endpoints: kebab-case, plural nouns — `/api/v1/booking-requests`

**Frontend (React/TS):**
- Components: `PascalCase` — `HotelCard.tsx`
- Hooks: `useCamelCase` — `useBookingStatus.ts`
- Files/folders (non-component): `kebab-case` or `camelCase` per feature convention
- Redux slices: `<feature>Slice.ts`
- Zod schemas: `<entity>Schema.ts`
- Types/interfaces: `PascalCase`, prefixed with `T`/`I` only if project convention requires (recommend no prefix, rely on file naming)

**Database:**
- Tables: `snake_case`, plural — `booking_requests`
- Columns: `snake_case`
- Foreign keys: `<referenced_table_singular>_id`

---

## 18. API Versioning Strategy

- URI-based versioning: `/api/v1/...` — simplest, explicit, cache-friendly, easiest for frontend and third-party integrators to reason about.
- Breaking changes → new version (`/api/v2/...`); non-breaking additive changes stay within `v1`.
- Deprecation policy: old versions supported for minimum 6 months post new version release, with `Deprecation` and `Sunset` HTTP headers.
- Internal module facade interfaces are versioned independently via semantic versioning if/when extracted to microservices.

---

## 19. Security Architecture

- **Authentication:** JWT (access token short-lived ~15min + refresh token rotation, stored as httpOnly secure cookie).
- **Authorization:** Spring Security method-level (`@PreAuthorize`) + role/permission model (Section 12).
- **Password Storage:** BCrypt hashing, never plaintext.
- **Transport Security:** HTTPS enforced end-to-end (Nginx TLS termination + HSTS).
- **Input Validation:** Bean Validation (JSR-380) on backend DTOs; Zod schema validation on frontend forms — defense in depth.
- **CORS:** Explicit allow-list of frontend origins.
- **Rate Limiting:** Nginx-level + application-level (e.g., Bucket4j) on auth and booking-creation endpoints to prevent abuse.
- **SQL Injection:** Prevented via JPA parameterized queries; no raw string concatenation.
- **XSS/CSRF:** React auto-escapes output; CSRF mitigated via SameSite cookies + stateless JWT design.
- **Secrets Management:** Environment variables / AWS Secrets Manager (future), never committed to repo.
- **Audit Logging:** All privileged actions (status changes, payment recording, role changes) logged immutably.
- **File Upload Security:** Cloudinary signed uploads, file-type/size validation before accepting.
- **Dependency Security:** Regular `mvn dependency-check` / `npm audit` scans in CI.

---

## 20. Logging Strategy

- **Structured JSON logging** (e.g., via Logback + Logstash encoder) for machine parseability.
- **Correlation/Trace ID** injected per request (MDC) — propagated from Nginx → Spring Boot, visible in every log line for a request, essential for debugging distributed issues later.
- **Log Levels:** `ERROR` (failures needing attention), `WARN` (recoverable anomalies), `INFO` (business events: booking created/confirmed), `DEBUG` (dev-only detail).
- **Sensitive Data Masking:** Never log passwords, tokens, full card/payment details.
- **Centralized Aggregation (future-ready):** Logs shippable to ELK/CloudWatch/Grafana Loki without code change (SLF4J abstraction).
- **Business Event Logging:** Key domain events (booking status transitions, payments, commission entries) logged distinctly from technical logs for audit/analytics.

---

## 21. Exception Handling Strategy

- **Global Exception Handler:** `@ControllerAdvice` + `@ExceptionHandler` mapping domain exceptions to consistent HTTP responses.
- **Custom Exception Hierarchy:**
  - `StayKonkanException` (base)
    - `ResourceNotFoundException` → 404
    - `InvalidStateTransitionException` → 409
    - `ValidationException` → 400
    - `UnauthorizedException` → 401 / `ForbiddenException` → 403
    - `ExternalServiceException` (Cloudinary/Notification failures) → 502/503
- **Consistent Error Response Contract:**
  ```json
  { "timestamp": "...", "status": 409, "error": "INVALID_STATE_TRANSITION",
    "message": "Cannot move booking from COMPLETED to PENDING",
    "path": "/api/v1/booking-requests/123/status", "traceId": "..." }
  ```
- **Frontend:** Centralized Axios/React Query error interceptor maps error codes to user-friendly toasts; retries idempotent GET requests automatically.
- **Fail-safe defaults:** External integration failures (e.g., SMS provider down) never block core booking flow — queued/retried asynchronously.

---

## 22. Future Scalability Plan

1. **Caching Layer:** Introduce Redis for search results, session/token blacklist, and hot hotel data — no architectural change, just a new integration.
2. **Read Replicas:** PostgreSQL read replicas for search-heavy read traffic once volume grows.
3. **CDN:** Cloudinary already provides CDN for media; extend to static frontend assets via CloudFront.
4. **Horizontal Scaling:** Stateless Spring Boot instances behind a load balancer (Nginx/ALB); JWT statelessness makes this trivial.
5. **Async Processing:** Move notification dispatch, report generation, and commission settlement to a message queue (RabbitMQ/SQS) as volume grows.
6. **Search Scaling:** Introduce Elasticsearch/OpenSearch for hotel search once catalog size and query complexity outgrow PostgreSQL full-text search.
7. **Microservice Extraction:** Per Section 13, extract high-load modules (Booking, Search) first when justified.
8. **Multi-region readiness:** AWS-ready containerized deployment allows future multi-AZ/region expansion.
9. **New Verticals:** Cab Booking, Restaurant, AI Recommendations plug in as new modules/services consuming shared Identity, Booking-core, and Commission contracts.

---

## 23. Development Roadmap

**Phase 1 (Current):** Planning & Architecture — this document.
**Phase 2:** Core backend — Identity, Hotel, Booking modules + DB schema + API contracts.
**Phase 3:** Core frontend — Search, Hotel Detail, Booking Request flow, Auth.
**Phase 4:** Admin Dashboard — Booking queue, hotel management, commission tracking.
**Phase 5:** Notifications, Payments (manual), Reviews.
**Phase 6:** Polish — UI/UX motion (Framer Motion/GSAP/R3F), performance, security hardening.
**Phase 7:** Deployment — Dockerize, Nginx, CI/CD, AWS deployment.
**Phase 8:** Post-MVP — Hotel Owner Dashboard, Instant Booking, Online Payments, Coupons, Cab Booking, AI Recommendations.

---

## 24. Complete Development Timeline (Indicative, Small Team)

| Phase | Duration | Deliverable |
|---|---|---|
| Planning & Architecture | Week 1–2 | This document, ERD, API contracts, wireframes |
| Backend Core Setup | Week 3–5 | Identity, Hotel, Booking modules, DB, security |
| Frontend Core Setup | Week 4–7 | Search, Hotel Detail, Booking flow, Auth (parallel to backend) |
| Admin Dashboard | Week 7–9 | Booking queue, hotel CRUD, commission module |
| Notifications & Payments (manual) | Week 9–10 | Email/SMS integration, payment recording |
| Reviews & Polish | Week 10–11 | Review flow, UI motion, responsive QA |
| Testing & Security Hardening | Week 11–12 | Unit/integration tests, security review, load test |
| Dockerization & Deployment | Week 12–13 | CI/CD, staging + production on AWS |
| **MVP Launch** | **~Week 13–14** | Public beta in Alibag/Nagaon/Murud/Kashid |
| Post-MVP Iterations | Ongoing | Owner Dashboard, Instant Booking, Payments gateway, Coupons, Cabs, AI Recs |

*(Timeline assumes 1–2 full-stack engineers + 1 designer; adjust based on actual team size.)*

---

## 25. Professional UI/UX Plan

**Design Philosophy:** "Coastal Konkan modern" — warm, sun-lit, trustworthy, mobile-first, fast.

- **Visual Language:** Earthy coastal palette (sea blue, sand beige, sunset orange accents), generous imagery, minimal chrome.
- **Typography:** A clean geometric sans for UI (e.g., Inter/Satoshi-style) + a warm display serif/script for hero headlines to evoke a boutique travel feel.
- **Homepage:** Full-bleed hero with destination imagery, subtle parallax/GSAP scroll animation, quick-search widget above the fold, curated "Popular this weekend" destination cards.
- **Micro-interactions:** Framer Motion for card hover/transition states, page transitions; React Three Fiber reserved for a signature hero element (e.g., subtle 3D wave/coastline visual) — used sparingly to avoid performance cost on mobile.
- **Search & Results:** Card-based hotel listings, sticky filter bar, skeleton loaders (React Query), map + list toggle.
- **Booking Flow:** Progressive disclosure form (React Hook Form + Zod validation), clear status tracker (stepper UI) mirroring the state machine so customers always know "where" their booking is.
- **Admin Dashboard:** Data-dense but calm — ShadCN UI components (tables, badges, drawers) for booking queue, kanban-style status columns optional.
- **Accessibility:** Semantic HTML, keyboard navigability, color-contrast AA compliance, alt text enforced on all hotel images.
- **Responsiveness:** Mobile-first breakpoints (most Konkan weekend-trip searches will be mobile); Tailwind utility-driven consistency.
- **Trust Signals:** Verified badges for hotels, transparent "how booking works" explainer (matches the manual flow honestly, builds trust rather than hiding it).

---

## 26. Suggested Brand Name and Logo Concept

**Name:** **StayKonkan** (confirmed — strong, descriptive, SEO-friendly for "stay in Konkan" searches)

**Tagline options:**
- "Your Konkan Getaway, Sorted."
- "Konkan Stays, Curated by Locals."
- "Beaches. Booked. Beautifully."

**Logo Concept:**
- Mark: a minimal wave/palm-leaf motif merged into a "S/K" monogram, or a stylized sun-over-coastline icon — simple enough to work as a small app icon/favicon.
- Color palette: Deep sea blue (#0B4F6C or similar), warm sand (#F4E1C1), sunset coral accent (#F76C5E).
- Wordmark: rounded-geometric sans for "Stay", contrasted with a slightly warmer weight/color for "Konkan" to separate the generic word from the destination brand.
- Usage: Horizontal lockup for web header, icon-only mark for favicon/app icon/social avatar.

*(Actual logo file production is a design task for Phase 2+; this is the creative brief.)*

---

## 27. Project Folder Structure

### Backend — `staykonkan-backend/` (package-by-feature, modular monolith)
```
staykonkan-backend/
├── src/main/java/com/staykonkan/
│   ├── identity/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── domain/
│   │   ├── dto/
│   │   └── security/           (JWT filters, providers)
│   ├── hotel/
│   │   ├── controller/  service/  repository/  domain/  dto/
│   ├── booking/
│   │   ├── controller/  service/  repository/  domain/  dto/
│   │   └── statemachine/
│   ├── commission/
│   │   ├── controller/  service/  repository/  domain/  dto/
│   ├── payment/
│   │   ├── controller/  service/  gateway/ (interface + impls)
│   ├── media/
│   │   ├── controller/  service/  cloudinary/
│   ├── notification/
│   │   ├── service/  strategy/  templates/
│   ├── review/
│   │   ├── controller/  service/  repository/  domain/
│   ├── audit/
│   │   ├── aspect/  service/  domain/
│   ├── common/
│   │   ├── exception/  response/  validation/  util/
│   ├── config/
│   │   ├── SecurityConfig  CorsConfig  SwaggerConfig  JpaConfig
│   └── StayKonkanApplication.java
├── src/main/resources/
│   ├── application.yml (+ -dev/-prod profiles)
│   └── db/migration/    (Flyway SQL migrations)
├── src/test/java/...    (mirrors main structure)
├── Dockerfile
└── pom.xml
```

### Frontend — `staykonkan-frontend/` (feature-based)
```
staykonkan-frontend/
├── src/
│   ├── app/                 (store setup, root providers, router)
│   ├── features/
│   │   ├── auth/             (components, hooks, slice, api)
│   │   ├── search/
│   │   ├── hotel/
│   │   ├── booking/
│   │   ├── admin/
│   │   │   ├── booking-queue/  hotel-management/  commission/
│   │   ├── review/
│   │   └── notification/
│   ├── components/ui/        (ShadCN-based shared components)
│   ├── components/motion/    (Framer Motion / GSAP wrappers)
│   ├── components/three/     (React Three Fiber scenes)
│   ├── lib/                  (axios instance, query client, zod schemas)
│   ├── hooks/                (shared hooks)
│   ├── routes/                (React Router route definitions, guards)
│   ├── types/
│   ├── styles/                (Tailwind config, globals)
│   └── main.tsx
├── public/
├── index.html
├── vite.config.ts
├── tailwind.config.ts
└── package.json
```

### Infra
```
infra/
├── docker-compose.yml         (local: backend + frontend + postgres + nginx)
├── nginx/
│   └── default.conf
├── docker/
│   ├── backend.Dockerfile
│   └── frontend.Dockerfile
└── aws/                        (future: ECS/EKS task defs, terraform-ready)
```

---

## 28. Feature Prioritization — MVP vs Future

### MVP (Launch-Critical)
- Hotel search & listing (destination/date/guest filters)
- Hotel detail pages with gallery (Cloudinary)
- Manual booking request flow + status tracking
- Admin booking queue, assignment, status transitions
- Manual advance payment recording
- Basic commission tracking & reporting
- Email/SMS notifications at key status changes
- Auth (customer + admin), RBAC
- Responsive, polished marketing-grade UI

### Post-MVP / Future (Architecturally Reserved, Not Built Yet)
- Instant Booking (auto-confirm without admin step)
- Hotel Owner Dashboard (self-service inventory/pricing)
- Online Payment Gateway integration (Razorpay/Stripe)
- Coupons & Offers engine
- Cab Booking module
- Restaurant Booking/Dashboard module
- AI-based recommendations (personalized destination/hotel suggestions)
- Review moderation & rich media reviews
- Multi-language support (Marathi/Hindi/English)
- Loyalty/referral program
- Advanced analytics dashboard for admin

---

**End of Phase 1.**

Waiting for your instruction: **"Next Phase"** to proceed.
