# Gideon Heritage Bank — Identity Service

The **Identity Service** is the identity, authentication and staff/customer identity-management component of the Gideon Heritage Bank backend. It is a Spring Boot REST service built with Java 21, Spring MVC, Spring Security, Spring Data JPA and PostgreSQL.

This README describes the implementation present in this repository snapshot. Configuration values supplied by the external Spring Cloud Config Server are intentionally not fabricated; see [Configuration](#configuration).

## Contents

- [Responsibilities](#responsibilities)
- [Technology stack](#technology-stack)
- [Architecture and package structure](#architecture-and-package-structure)
- [Implemented capabilities](#implemented-capabilities)
- [API and controller groups](#api-and-controller-groups)
- [API response and error handling](#api-response-and-error-handling)
- [Authentication and authorization](#authentication-and-authorization)
- [OpenAPI / Swagger UI](#openapi--swagger-ui)
- [Persistence and database migrations](#persistence-and-database-migrations)
- [Caching and idempotency](#caching-and-idempotency)
- [Messaging and integration events](#messaging-and-integration-events)
- [Profile image storage](#profile-image-storage)
- [Configuration](#configuration)
- [Running locally](#running-locally)
- [Operational notes](#operational-notes)
- [Current boundaries](#current-boundaries)

## Responsibilities

The service owns identity-related data and workflows, including:

- Customer registration and email verification.
- Customer and staff authentication.
- JWT access-token issuance and refresh-token lifecycle.
- Password change, forgotten-password and reset flows.
- Customer profile and address management.
- Staff identity creation and onboarding.
- Staff profiles, roles, employment assignments and employment history.
- Staff lifecycle operations and closure history.
- Department and branch administration.
- Customer closure requests and customer-to-staff conversion.
- Profile-image upload and retrieval.
- Publishing identity-related notification messages through RabbitMQ.
- Publishing a JSON Web Key Set (JWKS) for consumers that verify JWT signatures.

It is not the service responsible for delivering email/SMS/push messages. The messaging publishers send work to RabbitMQ for a downstream notification component.

## Technology stack

| Area | Technology visible in this project |
|---|---|
| Language | Java 21 |
| Build | Maven |
| Framework | Spring Boot 4.1.2 |
| HTTP | Spring MVC (`spring-boot-starter-webmvc`) |
| Security | Spring Security, OAuth2 Resource Server, JWT |
| API documentation | springdoc-openapi 3.2.0, Swagger UI |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| Schema migrations | Flyway |
| Caching | Spring Cache with Redis |
| Messaging | Spring AMQP / RabbitMQ |
| Object storage | MinIO |
| Service discovery | Spring Cloud Netflix Eureka Client |
| Centralized configuration | Spring Cloud Config Client |
| HTTP clients | Spring Cloud OpenFeign, Apache HttpClient 5 |
| Object mapping | MapStruct 1.6.3 |
| Boilerplate reduction | Lombok |
| Validation | Jakarta Bean Validation |

## Architecture and package structure

Base package:

`com.gideonheritagebank.IDENTITY_SERVICE`

Main packages:

| Package | Responsibility |
|---|---|
| `controller` | REST endpoints and HTTP request/response handling |
| `service` | Business workflows and transaction boundaries |
| `entity` | JPA persistence entities |
| `repository` | Spring Data repository interfaces and database queries |
| `dto.request` | Request payloads accepted by the API |
| `dto.response` | API response payloads, pagination and standard response envelope |
| `dto.internal` | Internal service-layer result objects |
| `mapper` | MapStruct mappings between entities and DTOs |
| `config` | Spring Security, JWT keys, RabbitMQ, Redis, MinIO, Feign and async configuration |
| `security` | JWT service and token-related logic |
| `event` | Identity event payload types |
| `messaging` | RabbitMQ publishers, listeners and message payloads |
| `cache` | Cache names and cache-key helpers |
| `storage` | Profile-image storage abstractions and MinIO implementation |
| `exception` | Domain exceptions and global exception handling |
| `enums` | Domain enumerations |
| `utils` | Trace IDs and idempotency support |

The application entry point is under the base package. Spring's component scanning therefore discovers the subpackages listed above.

## Implemented capabilities

### Customer identity

- Customer registration.
- Email verification and resend-verification workflows.
- Customer profile retrieval and updates.
- Customer address management, including primary-address operations.
- Customer search and administrative identity controls.
- Customer suspension and unsuspension.
- Customer closure request and administrative review workflows.
- Customer retention-related operations.
- Customer-to-staff conversion workflow.

### Authentication and credentials

- Login with failed-attempt tracking and account-locking behaviour.
- JWT access tokens and refresh tokens.
- Refresh-token rotation and logout/session revocation workflows.
- Cookie-based access/refresh token support in addition to Bearer-token support.
- Password change, forgot-password and reset-password flows.
- CSRF token endpoint and CSRF protection for applicable browser-based requests.
- Public JWKS endpoint at `/.well-known/jwks.json`.

### Staff identity and administration

- Staff identity creation.
- Staff onboarding and initial credential setup.
- Staff profile retrieval and update.
- Staff address management.
- Staff role assignment, change, removal and role history.
- Staff employment updates, current employment retrieval and employment history.
- Dedicated employment termination operation.
- Staff lifecycle status, enable/disable, suspend/unsuspend, lock/unlock, deactivate and close operations.
- Staff closure history.
- Department and branch administration.
- Branch manager assignment/removal.
- Branch employment listing and employment counts.

### Profile images

Profile images are handled through a storage abstraction. The configured implementation uses MinIO. The identity database stores image references/URLs rather than binary image content in the `BankUser` row.

## API and controller groups

The main API prefix is `/api/v1/identity`. The following groups are derived from the controllers in this repository.

| Controller | Base route(s) | Purpose |
|---|---|---|
| `IdentityController` | `/api/v1/identity`, `/api/v1/identity/auth`, `/auth` | Registration and email verification |
| `AuthenticationController` | `/api/v1/identity/auth`, `/api/v1/identity`, `/auth` | Login, refresh, logout, password workflows and CSRF |
| `JwkController` | `/.well-known/jwks.json` | Public signing keys |
| `CustomerController` | `/api/v1/identity/customers` | Customer self-service profile and addresses |
| `CustomerClosureController` | `/api/v1/identity/customers/me/closure-requests` | Customer closure requests |
| `AdminCustomerController` | `/api/v1/identity/admin/customers` | Customer administration and search |
| `AdminCustomerClosureController` | `/api/v1/identity/admin/customer-closure-requests` | Administrative closure review |
| `CustomerStaffConversionController` | `/api/v1/identity/admin/customers` | Customer-to-staff conversion |
| `StaffIdentityController` | `/api/v1/identity/staff`, `/staff` | Staff identity creation |
| `StaffOnboardingController` | `/api/v1/identity/staff/onboarding` | Staff onboarding completion |
| `AdminStaffController` | `/api/v1/identity/admin/staff` | Staff directory and lookup |
| `StaffProfileController` | `/api/v1/identity/admin/staff-profiles` | Staff profile management |
| `StaffAddressController` | `/api/v1/identity/admin/staff/{staffUserId}/addresses` | Staff addresses |
| `StaffRoleManagementController` | `/api/v1/identity/admin/staff/{staffUserId}/role` | Staff role operations and history |
| `StaffEmploymentController` | `/api/v1/identity/admin/staff/{staffProfileId}/employment` | Employment update, history and termination |
| `StaffLifecycleController` | `/api/v1/identity/admin/staff/{staffUserId}/lifecycle` | Staff lifecycle operations |
| `StaffClosureHistoryController` | `/api/v1/identity/admin/staff/closure-history` | Staff closure history |
| `DepartmentController` | `/api/v1/identity/admin/departments` | Department administration |
| `BranchController` | `/api/v1/identity/admin/branches` | Branch administration |
| `BranchEmploymentController` | `/api/v1/identity/admin/branches` | Current branch employment and staffing count |
| `ProfileImageController` | `/api/v1/identity/profile-image` | Authenticated profile-image operations |

The exact HTTP methods, path variables, query parameters, request DTOs and response DTOs are exposed in the generated OpenAPI document and Swagger UI. Access control is enforced by Spring Security and method-level `@PreAuthorize` rules; the presence of a route in Swagger does not grant permission to call it.

## API response and error handling

### Standard response envelope

Successful controller responses use `dto.response.ApiResponse<T>`. The global exception handler also returns `ApiResponse<Void>` for handled application errors.

The envelope contains:

| Field | Meaning |
|---|---|
| `timestamp` | UTC response timestamp |
| `success` | Whether the operation succeeded |
| `status` | HTTP status code |
| `message` | Human-readable message |
| `data` | Success payload; its shape depends on the endpoint |
| `errors` | Field-level or machine-readable error map |
| `path` | Request path |
| `traceId` | Request trace identifier |

A typical success response:

```json
{
  "timestamp": "2026-11-18T12:00:00Z",
  "success": true,
  "status": 200,
  "message": "Request completed successfully.",
  "data": {},
  "path": "/api/v1/identity/customers/me",
  "traceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

A typical handled error response:

```json
{
  "timestamp": "2026-11-18T12:00:00Z",
  "success": false,
  "status": 400,
  "message": "Request validation failed",
  "errors": {
    "email": "Please provide a valid email address"
  },
  "path": "/api/v1/identity/register",
  "traceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

The `GlobalExceptionHandler` handles validation failures, malformed request bodies, domain exceptions, missing resources, conflicts, and other application exceptions. `SecurityApiErrorHandler` uses the same `ApiResponse` envelope for authentication and authorization failures generated by the Spring Security filter chain.

`ApiErrorResponse` is present as a separate DTO in the source tree, but the current global exception and security handlers serialize `ApiResponse<Void>`; therefore `ApiResponse` is the documented standard error envelope.

### Pagination

Paged endpoints use `PagedResponse<T>`. The response includes `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last` and `empty`.

The project's response factory converts Spring Data's zero-based page index to a one-based page number in the API response. Follow the pagination behaviour documented by the individual controller/service implementation when supplying request parameters.

## Authentication and authorization

### JWT Bearer authentication

Protected endpoints accept JWT access tokens through the standard header:

```http
Authorization: Bearer <access-token>
```

The service also has a configurable access-token cookie resolver. If an Authorization Bearer header is supplied, it takes precedence over cookie-based authentication.

The OpenAPI security scheme is named `bearerAuth`. Use Swagger UI's **Authorize** control and paste the raw access token; Swagger UI supplies the Bearer prefix.

### Roles and authorization

The JWT authentication converter reads the `roles` claim and maps values such as `ADMIN` to Spring Security authorities such as `ROLE_ADMIN`. Controllers/services use method security such as `hasRole('ADMIN')` where applicable.

### CSRF

The service is stateless but retains CSRF protection for applicable cookie/browser flows. Specific public authentication and onboarding routes are excluded from CSRF checks in `SecurityConfig`. Swagger UI and OpenAPI resources are also excluded from CSRF checks.

### Public JWKS

The public signing-key document is exposed at:

`GET /.well-known/jwks.json`

It publishes the public portion of the configured RSA signing key. Private signing material must remain outside source control.

## OpenAPI / Swagger UI

The project uses `springdoc-openapi-starter-webmvc-ui` version `3.2.0`, compatible with the Spring Boot 4.1.x line used in this project.

When the service is running locally on port `8081`:

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`
- OpenAPI YAML: `http://localhost:8081/v3/api-docs.yaml`

Swagger UI is configured to scan the Identity Service controller package and the versioned Identity API routes, plus the public JWKS route. Protected controller groups declare the `bearerAuth` scheme. Request/response DTOs use schema descriptions and examples where applicable.

For production, disable Swagger or restrict access to it according to the deployment's security policy. Swagger documentation is not a replacement for endpoint authorization.

## Persistence and database migrations

The service uses Spring Data JPA with PostgreSQL. Flyway owns schema evolution. Migration scripts are under:

`src/main/resources/db/migration`

The migration set in this repository includes versions V1 through V14, covering the identity schema and subsequent additions such as customer identifiers, customer suspension/closure/retention, staff onboarding, staff employment, staff role history, staff identity/customer number, customer-to-staff conversion, staff closure history, branch tables and the branch country-code type correction.

Do not use Hibernate schema auto-update as a substitute for Flyway migrations in this project. When changing an already-applied migration, create a new migration with a version greater than the latest applied version.

## Caching and idempotency

The service uses Spring Cache backed by Redis. Cache names and cache-key helpers are centralized in the `cache` package (`CacheNames` and `CacheKeys`). Services use cache annotations and evict affected caches when underlying identity, staff, branch, department or employment data changes.

Write operations that use the project's `@Idempotent` annotation are protected against duplicate processing using the idempotency infrastructure backed by Redis/persistence components in the project. Follow the endpoint's required idempotency-key behaviour when calling such operations.

## Messaging and integration events

RabbitMQ is used to publish identity workflows that require downstream notification delivery. Event payloads are in the `event` package; publishers, listeners and message records are in `messaging`.

The source contains these identity event types:

- `EmailVerificationRequestedEvent`
- `PasswordResetRequestedEvent`
- `StaffOnboardingRequestedEvent`

The RabbitMQ configuration defines an exchange and queues/routing keys for email verification, password reset and staff onboarding. Defaults visible in the code include:

| Purpose | Default queue | Default routing key |
|---|---|---|
| Email verification | `identity.email-verification` | `notification.email-verification` |
| Password reset | `identity.password-reset` | `notification.password-reset` |
| Staff onboarding | `identity.staff-onboarding` | `notification.staff-onboarding` |

Default exchange: `bank.notifications.exchange`.

These values are configurable. The downstream notification service is responsible for actual message delivery; Identity Service publishes the requests and owns the identity workflow.

## Profile image storage

The project defines a profile-image storage abstraction and a MinIO implementation. MinIO connection credentials, bucket and related storage settings are supplied through configuration properties. Do not commit access keys or secret keys.

The profile-image controller exposes authenticated upload, retrieval and deletion operations under `/api/v1/identity/profile-image`.

## Configuration

The checked-in `src/main/resources/application.properties` identifies the application and imports centralized configuration:

```properties
spring.application.name=IDENTITY-SERVICE
spring.config.import=configserver:http://localhost:8888
```

This repository snapshot does not include the external Config Server's `IDENTITY-SERVICE` configuration file. Database credentials, Redis, RabbitMQ, MinIO, JWT keystore, service-discovery and other environment-specific values must therefore be supplied by the Config Server or an approved local override.

Configuration keys referenced by the source include:

| Concern | Property keys referenced in code |
|---|---|
| PostgreSQL / JPA / Flyway | Spring datasource, JPA and Flyway properties |
| Redis | Spring Redis connection properties |
| RabbitMQ | Spring RabbitMQ connection properties and `identity.messaging.*` |
| JWT | `identity.jwt.keystore-path`, `identity.jwt.keystore-password`, `identity.jwt.keystore-alias`, `identity.jwt.issuer`, `identity.jwt.audience`, `identity.jwt.access-token-ttl`, `identity.jwt.refresh-token-ttl` |
| Cookie security | `identity.security.cookies.access-name`, `refresh-name`, `access-path`, `refresh-path`, `secure`, `same-site` |
| Login protection | `identity.security.login.max-failed-attempts`, `identity.security.login.lock-duration` |
| Email verification | `identity.verification.token-ttl`, `identity.verification.frontend-url` |
| Password reset | `identity.password-reset.token-ttl`, `identity.password-reset.frontend-url` |
| Staff onboarding | `identity.staff-onboarding.token-ttl`, `identity.staff-onboarding.frontend-url` |
| Customer retention | `identity.retention.closed-customer-review-period`, `identity.retention.auth-token-record-period` |
| MinIO | `storage.minio.access-key`, `storage.minio.secret-key`, `storage.minio.bucket` |

Some properties have defaults in the code; others are required and must be configured. Keep secrets in environment variables or a secrets manager/config server rather than committing them.

## Running locally

### Prerequisites

- JDK 21.
- Maven compatible with the project's Spring Boot build.
- PostgreSQL.
- Redis.
- RabbitMQ.
- MinIO if profile-image storage is to be exercised.
- A running Spring Cloud Config Server at `http://localhost:8888`, unless the application is configured to use an alternative local configuration source.
- A configured RSA keystore for JWT signing.

### Start dependencies

Start PostgreSQL, Redis, RabbitMQ and MinIO using your preferred local installation or development environment. Create the application database and database user, then supply the corresponding datasource properties through Config Server/local configuration.

Ensure RabbitMQ's exchange and queues can be declared by the application. Ensure the MinIO bucket exists or that the configured storage setup creates it as expected by the implementation.

### Start the service

From the directory containing `pom.xml`:

```bash
mvn clean compile
mvn spring-boot:run
```

Or run the main application class from IntelliJ IDEA using JDK 21 and the Maven project configuration.

The actual HTTP port is configuration-driven and is not declared in the checked-in `application.properties`. The development setup discussed for this project uses port `8081`; confirm the effective Config Server configuration if the service starts on a different port.

### Verify

1. Confirm the application starts and Flyway completes its migrations.
2. Open `http://localhost:8081/swagger-ui.html` (adjust the port if configured differently).
3. Retrieve `http://localhost:8081/v3/api-docs`.
4. Use the Swagger **Authorize** control with a valid access token for protected endpoints.
5. Check `/.well-known/jwks.json` for the public signing-key set.
6. Check the health endpoint if Actuator health is enabled and exposed by the effective configuration.

## Operational notes

- Use HTTPS outside local development.
- Never commit JWT keystores, keystore passwords, database credentials, Redis/RabbitMQ credentials or MinIO secrets.
- Keep Flyway migrations append-only after they have been applied to shared environments.
- Keep API error responses consistent through `ApiResponse` and the global/security error handlers.
- Ensure RabbitMQ consumers are idempotent in downstream services; message delivery may be retried.
- Do not log passwords, raw refresh tokens, verification tokens or other credentials.
- Review Swagger exposure before production deployment.
- Keep branch/staff employment operations transactional and preserve employment-history snapshots when assignments change.

## Current boundaries

This README intentionally documents only functionality and infrastructure represented in the uploaded project snapshot. The external Config Server configuration is not included in the ZIP, so its exact database URLs, ports, secrets, token lifetimes beyond code defaults, and deployment-specific settings cannot be asserted here. The downstream risk-assessment and full notification-delivery service is a separate planned component, not implemented inside this Identity Service.
