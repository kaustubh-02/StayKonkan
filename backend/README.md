# StayKonkan Backend

Enterprise Spring Boot foundation for **StayKonkan** — a travel & hotel booking
platform for Alibag, Nagaon, Murud, Kashid and nearby Konkan destinations.

This repository currently contains **Phase 3: Backend Foundation only**.
Business modules (Hotel, Booking, Restaurant, Cab, Payment, Commission) are
intentionally not present yet — they are built on top of this foundation in
later phases without changing anything documented here.

Consistent with:
- **Phase 1** — Startup Planning & System Architecture
- **Phase 2** — PostgreSQL Database Design (`staykonkan_schema.sql`)

---

## 1. Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 24 |
| Framework | Spring Boot 4.1.0 (Spring Framework 7, Jakarta EE) |
| Security | Spring Security 7, JWT (jjwt 0.12.6) |
| Persistence | Spring Data JPA / Hibernate, PostgreSQL 16 |
| Build | Maven |
| Docs | springdoc-openapi 3.0.3 (Swagger UI) |
| Codegen | Lombok, MapStruct |
| Media (config only) | Cloudinary |
| Containerization | Docker (multi-stage build) |

---

## 2. Complete Backend Folder Structure

```
backend/
├── pom.xml
├── Dockerfile
├── .dockerignore
├── docker-compose.yml
├── .gitignore
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/staykonkan/
│   │   │   ├── StayKonkanApplication.java        # entry point
│   │   │   │
│   │   │   ├── config/                            # all @Configuration classes
│   │   │   │   ├── SecurityConfig.java             # security filter chain, auth manager/provider
│   │   │   │   ├── CorsConfig.java                 # CORS allow-list
│   │   │   │   ├── PasswordEncoderConfig.java       # BCrypt bean
│   │   │   │   ├── OpenApiConfig.java              # Swagger metadata + bearer auth scheme
│   │   │   │   ├── JpaAuditingConfig.java          # enables @CreatedDate/@CreatedBy etc.
│   │   │   │   └── MapperConfig.java               # shared MapStruct @MapperConfig
│   │   │   │
│   │   │   ├── security/                           # JWT + auth plumbing
│   │   │   │   ├── JwtProperties.java              # binds app.jwt.* properties
│   │   │   │   ├── JwtTokenProvider.java           # JWT Utility — issue/parse/validate
│   │   │   │   ├── JwtAuthenticationFilter.java    # reads Bearer token per request
│   │   │   │   ├── JwtAuthenticationEntryPoint.java # 401 handler (ApiError JSON)
│   │   │   │   ├── CustomAccessDeniedHandler.java   # 403 handler (ApiError JSON)
│   │   │   │   └── DefaultUserDetailsService.java  # TEMPORARY — replaced in Phase 4
│   │   │   │
│   │   │   ├── audit/
│   │   │   │   └── AuditorAwareImpl.java           # resolves "current user" for auditing
│   │   │   │
│   │   │   ├── entity/                              # JPA base classes only (no business entities yet)
│   │   │   │   ├── BaseEntity.java                  # id, @Version (optimistic locking)
│   │   │   │   └── AuditableEntity.java             # + createdAt/updatedAt/createdBy/updatedBy
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── BaseDTO.java                     # id/createdAt/updatedAt for response DTOs
│   │   │   │   ├── PageRequestDTO.java              # standard pagination request params
│   │   │   │   └── PageResponseDTO.java             # standard pagination response envelope
│   │   │   │
│   │   │   ├── response/
│   │   │   │   ├── ApiResponse.java                 # success envelope
│   │   │   │   └── ApiError.java                    # error envelope
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   ├── StayKonkanException.java         # base exception (carries ErrorCode)
│   │   │   │   ├── ResourceNotFoundException.java   # 404
│   │   │   │   ├── ValidationException.java         # 400
│   │   │   │   ├── UnauthorizedException.java       # 401
│   │   │   │   ├── ForbiddenException.java          # 403
│   │   │   │   ├── InvalidStateTransitionException.java # 409 (booking state machine, Phase 4+)
│   │   │   │   ├── ExternalServiceException.java    # 502 (Cloudinary/SMS/payment gateway)
│   │   │   │   └── GlobalExceptionHandler.java       # @RestControllerAdvice, maps everything to ApiError
│   │   │   │
│   │   │   ├── constant/
│   │   │   │   ├── SecurityConstants.java           # header names, claim keys, public endpoints
│   │   │   │   ├── AppConstants.java                # pagination defaults, API base path
│   │   │   │   └── ErrorCode.java                   # enum -> HttpStatus mapping
│   │   │   │
│   │   │   ├── validation/
│   │   │   │   ├── StrongPassword.java / StrongPasswordValidator.java
│   │   │   │   └── ValidPhoneNumber.java / PhoneNumberValidator.java
│   │   │   │
│   │   │   └── web/
│   │   │       └── CorrelationIdFilter.java          # X-Correlation-Id + MDC trace ID
│   │   │
│   │   └── resources/
│   │       ├── application.properties                # common config (all profiles)
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── logback-spring.xml                     # structured logging w/ trace ID
│   │
│   └── test/
│       └── java/com/staykonkan/
│           └── StayKonkanApplicationTests.java        # context-load smoke test
```

`controller/`, `service/`, `repository/`, and business `entity/`/`dto/`/`mapper/`
subpackages (e.g. `hotel/`, `booking/`) are intentionally absent — they arrive
module-by-module starting Phase 4, each following the same layered pattern
this foundation establishes.

---

## 3. Architecture Explanation (Phase 3 Scope)

- **Stateless JWT security.** No server-side sessions. `JwtAuthenticationFilter`
  runs after `CorrelationIdFilter`, validates the access token, and loads a
  `UserDetails` via the standard Spring Security `UserDetailsService`
  interface — **not** a concrete `User` entity. This is deliberate: it lets
  the whole security layer be built and tested now, and the Identity module
  (Phase 4) simply registers its real `UserDetailsService` implementation,
  which Spring auto-wires in place of the temporary `DefaultUserDetailsService`
  (via `@ConditionalOnMissingBean`) with zero changes here.
- **One response contract.** Every success response is `ApiResponse<T>`;
  every error is `ApiError`, produced centrally by `GlobalExceptionHandler`.
  Business modules never format their own error JSON.
- **One exception hierarchy.** All custom exceptions extend
  `StayKonkanException` and carry an `ErrorCode`, so adding a new failure
  case never means touching `GlobalExceptionHandler`.
- **Auditing is structural, not manual.** Any future entity extending
  `AuditableEntity` gets `createdAt/updatedAt/createdBy/updatedBy` for free
  via Spring Data JPA auditing — no service method ever sets these fields
  by hand.
- **MapStruct + Lombok coexist safely.** `pom.xml` pins the annotation
  processor order (Lombok → `lombok-mapstruct-binding` → MapStruct) so
  Lombok-generated getters/builders are visible when MapStruct generates
  mapping code — a very common source of silent build failures if omitted.
- **Secrets never live in the repo.** `JWT_SECRET`, DB credentials, and
  Cloudinary keys are all `${ENV_VAR}` placeholders — see Section 6.

---

## 4. Project Setup Instructions

### Prerequisites
- JDK 24
- Maven 3.9+ (or use the Docker build, which needs no local Maven/JDK)
- PostgreSQL 16 (or Docker, which provisions it for you)
- Git

### Clone & configure
```bash
git clone <your-repo-url> staykonkan
cd staykonkan/backend

# Generate a JWT secret — required, the app will refuse to start without it
export JWT_SECRET=$(openssl rand -base64 64)

# Local Postgres credentials (match application-dev.properties defaults,
# or override via env vars DB_USERNAME / DB_PASSWORD)
createdb staykonkan_dev   # if not using Docker for Postgres
```

---

## 5. Run Instructions

### Option A — Docker Compose (recommended, zero local Java/Maven/Postgres needed)
```bash
cd backend
export JWT_SECRET=$(openssl rand -base64 64)
docker compose up --build
```
Backend will be available at `http://localhost:8080`, Postgres at `localhost:5432`.

### Option B — Local Maven
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Option C — Build the jar and run it directly
```bash
mvn clean package -DskipTests
java -jar target/staykonkan-backend.jar --spring.profiles.active=dev
```

### Verify it's running
```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```
Swagger UI (dev only): `http://localhost:8080/swagger-ui.html`

### Run tests
```bash
mvn test
```

---

## 6. Environment Variables Reference

| Variable | Required | Used In | Purpose |
|---|---|---|---|
| `JWT_SECRET` | Yes (all envs) | all profiles | HMAC signing key for JWTs. Generate: `openssl rand -base64 64` |
| `DB_USERNAME` / `DB_PASSWORD` | Yes | dev/prod | PostgreSQL credentials |
| `DB_URL` | Yes (prod) | prod | Full JDBC URL |
| `CORS_ALLOWED_ORIGINS` | Yes (prod) | prod | Comma-separated allowed frontend origins |
| `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET` | Later phase | all | Media module (Phase 5+) |

---

## 7. GitHub Commit Instructions

```bash
# From the repository root (one level above backend/, if this is part of
# the larger staykonkan monorepo alongside staykonkan-frontend/):

git init                                  # only if not already a repo
git add backend/
git status                                # confirm target/, .env, secrets are NOT staged
                                           # (.gitignore already excludes them)

git commit -m "Phase 3: Spring Boot backend foundation

- Security: JWT auth (access + refresh architecture), Spring Security
  filter chain, BCrypt, secure headers, RBAC-ready
- Global exception handling with consistent ApiResponse/ApiError contract
- Base entity/DTO classes with JPA auditing
- CORS, Swagger/OpenAPI, structured logging with correlation IDs
- Docker multi-stage build + docker-compose for local dev
- No business modules yet — foundation only, per Phase 3 scope"

git branch -M main
git remote add origin <your-repo-url>     # only if remote not already set
git push -u origin main
```

**Recommended branch strategy going forward:**
```bash
git checkout -b phase-4-identity-module
# ... build the Identity module (User entity, auth endpoints, real
#     UserDetailsService replacing DefaultUserDetailsService) ...
git push -u origin phase-4-identity-module
# open a PR into main
```

---

## 8. Coding Standards (enforced by this foundation)

- **Constructor injection only** — every class in this codebase uses
  `final` fields set via constructor; no `@Autowired` on fields.
- **DTOs at every API boundary** — controllers (from Phase 4 on) never
  accept or return JPA entities directly.
- **SOLID** — see Phase 1, Section 15 for the full mapping; this
  foundation's `PaymentGateway`-style abstractions arrive with the
  Payment module, but the same principle already governs
  `UserDetailsService` usage here (interface, not concrete class).
- **No placeholder/TODO business logic** — the only intentional
  placeholder in this phase is `DefaultUserDetailsService`, explicitly
  marked `TEMPORARY` and self-documenting about when/how it's replaced.

---

## 9. Module 1: Authentication + User — Delivered

New packages (package-by-feature, additive to the flat foundation):
- `com.staykonkan.auth` — `dto/` (RegisterRequest, LoginRequest, RefreshTokenRequest, AuthResponse), `service/` + `service/impl/` (AuthService, AuthServiceImpl), `controller/` (AuthController)
- `com.staykonkan.user` — `entity/` (User, Role, UserStatus), `repository/` (UserRepository), `dto/` (UserProfileResponse, UpdateProfileRequest), `mapper/` (UserMapper), `service/` + `service/impl/` (UserService, UserServiceImpl), `controller/` (UserController)
- `com.staykonkan.util.PageUtils` — new foundation utility (the `util` package existed in the plan but had no classes yet); converts `PageRequestDTO` → Spring's `Pageable`, reused by every future module's list endpoints
- `com.staykonkan.security.SecurityUserPrincipal` / `UserDetailsServiceImpl` — new files in the existing `security` package
- `com.staykonkan.exception.DuplicateResourceException` — new file in the existing `exception` package

**Endpoints now live:**
```
POST   /api/v1/auth/register     (public)
POST   /api/v1/auth/login        (public)
POST   /api/v1/auth/refresh      (public)
GET    /api/v1/users/me          (any authenticated user)
PUT    /api/v1/users/me          (any authenticated user)
GET    /api/v1/users             (ADMIN)
GET    /api/v1/users/{id}        (ADMIN)
DELETE /api/v1/users/{id}        (ADMIN — soft delete)
```

**Required modifications to existing files, and why (nothing else was touched):**
1. **Deleted** `security/DefaultUserDetailsService.java` — a second `UserDetailsService` bean alongside the new real one would fail context startup with `NoUniqueBeanDefinitionException`. Not optional.
2. **`config/SecurityConfig.java`** — added `@EnableMethodSecurity` (one annotation + one import). Without it, every `@PreAuthorize` in the new controllers is silently ignored and admin-only endpoints would be unprotected. No existing logic changed.
3. **`audit/AuditorAwareImpl.java`** — replaced the `Optional.empty()` placeholder (a `TODO` I left here in Phase 3 for this exact moment) with real logic reading `SecurityUserPrincipal.getUserId()`. One method body, same signature.
4. **`exception/GlobalExceptionHandler.java`** — added a handler for the general `AuthenticationException` (catches `DisabledException` etc., thrown when a SUSPENDED/DELETED user tries to log in). Without it, that case fell through to the generic 500 handler instead of a proper 401. The existing `BadCredentialsException` handler is untouched and still wins for wrong-password cases since it's more specific.

`pom.xml`, all `application*.properties`, JWT configuration, and CORS config were not touched.

**Design notes:**
- Role is a single enum column on `User` (ADMIN/OWNER/USER), not the join-table RBAC model — a deliberate simplification appropriate for this scope; revisit only if a user genuinely needs multiple simultaneous roles.
- `ADMIN` can never be self-registered (`AuthServiceImpl.register` rejects it outright) — admin accounts are provisioned out-of-band.
- Login is by email; the JWT `subject` claim is always the email — documented once, in `UserDetailsServiceImpl`.
- User deletion is a soft delete (`status = DELETED`), preserving referential integrity for that user's future properties/bookings/reviews.

## 10. What's Next — Property Module (not built yet)

`Property`, `PropertyImage` entities; owner-only create/update/delete; public search/filter by location & price; Cloudinary upload wired to the existing `CloudinaryConfig` bean; admin approval workflow feeding the Admin module's dashboard.

