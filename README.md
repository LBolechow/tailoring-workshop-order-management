# Tailoring Workshop — Order Management System

A Spring Boot REST API for managing tailoring workshop operations, including order scheduling, employee assignments, inventory materials, user management, and real-time communication.

The project is a backend rewrite of an earlier full-stack application developed as an engineering thesis. The original system used Spring Boot, Thymeleaf, Bootstrap, and jQuery. This version retains the business domain while replacing server-rendered views with a REST API, reorganizing the application architecture, and expanding automated testing and security controls.

**Original thesis implementation:** [Praca-dyplomowa-SpringBoot-old](https://github.com/LBolechow/Praca-dyplomowa-SpringBoot-old)

---

## Technology Stack

| Category          | Technologies                                |
| ----------------- | ------------------------------------------- |
| Language          | Java 17                                     |
| Framework         | Spring Boot 3, Spring Security              |
| Authentication    | JWT, OAuth2 (Google)                        |
| Persistence       | Spring Data JPA, Hibernate, PostgreSQL, H2  |
| Communication     | REST, WebSocket, STOMP                      |
| Validation        | Jakarta Bean Validation                     |
| API Documentation | OpenAPI 3, Springdoc                        |
| Testing           | JUnit 5, Mockito, AssertJ, Spring Boot Test |
| Build             | Maven                                       |
| Other             | Lombok, Java Records, SLF4J                 |

## Core Functionality

### Order Management

- Order creation, modification, cancellation, and status tracking.
- Automatic scheduling based on employee availability and working hours.
- Employee assignment and order rescheduling.
- Daily and monthly schedule views.
- Order history and individual order details.
- Materials tracking and checklists.

### Users and Access Control

The application supports three user roles with differentiated access to resources and operations.

| Role          | Capabilities                                                                           |
| ------------- | -------------------------------------------------------------------------------------- |
| Client        | Order history, profile management, communication with employees                        |
| Employee      | Order scheduling, assigned orders, materials management, client communication          |
| Administrator | User administration, role assignment, pricing management, organization-wide scheduling |

Selected functionality, including registration, authentication, pricing information, and order status lookup, is available without an authenticated session.

### Communication

- Real-time messaging using WebSocket and STOMP.
- Client–employee and employee–employee conversations.
- Conversation history and read-state tracking.
- User notifications and administrator broadcasts.

---

## Architecture and Implementation

The application follows a layered architecture separating HTTP request handling, business logic, and persistence.

**API layer**

REST controllers handle request validation and response mapping. Request and response contracts use dedicated DTOs, primarily Java records, rather than exposing persistence entities directly.

**Service layer**

Business operations are implemented in service classes, with shared functionality extracted into dedicated utilities. Domain exceptions are propagated to a centralized exception handler.

**Persistence layer**

Spring Data JPA repositories provide database access. Hibernate manages entity relationships and persistence operations. PostgreSQL is supported alongside H2 for local development and testing.

**Scheduling**

The order scheduling logic evaluates employee availability, working hours, existing assignments, and date constraints to determine valid appointment slots.

**Real-time communication**

WebSocket endpoints support asynchronous messaging and notifications, independently of the REST-based business operations.

### Error Handling

A centralized `@RestControllerAdvice` maps domain and framework exceptions to structured JSON responses.

Error handling distinguishes between validation failures, missing resources, authorization failures, and unexpected server errors. Internal exception details are retained in server logs rather than exposed through generic API error responses.

---

## Authentication and Application Security

Security is implemented primarily through Spring Security, with additional application-level authorization checks.

### Authentication

- JWT-based stateless authentication for REST endpoints.
- Password hashing through Spring Security's `PasswordEncoder`.
- JWT invalidation on logout using a token blacklist.
- Google OAuth2 integration with a short-lived, single-use code exchange mechanism.
- Role-based authorization using Spring Security authorities.

### Authorization

Endpoint access is restricted according to user roles.

Resource-level authorization is implemented where endpoint-level role checks are insufficient. Conversation access, for example, requires verification that the authenticated user belongs to the requested conversation.

This addresses an important distinction between authenticated access and authorization to individual resources, particularly in APIs that expose object identifiers.

### Password Recovery

Password recovery uses short-lived, single-use reset tokens instead of generating and emailing replacement passwords.

Reset tokens are generated using `SecureRandom`, stored as SHA-256 hashes, and expire after 15 minutes. Password changes require successful token verification.

### Security Assessment

The application is also used for practical application security testing and remediation.

Tools and techniques include:

- **OWASP ZAP (DAST):** active and passive testing of the running REST API.
- **OpenAPI integration:** importing endpoint definitions for automated security testing.
- **Authenticated scanning:** testing protected endpoints using JWT authorization headers.
- **Manual authorization testing:** verifying resource-level access restrictions using separate user identities.
- **Security regression testing:** validating authentication, authorization, token handling, and error responses after changes.

Security-related refactoring has included improvements to JWT key configuration, object-level authorization, OAuth2 token delivery, password recovery, and exception handling.

### Initial DAST Assessment

An initial OWASP ZAP assessment was performed on October 29, 2026, against a local application instance.

| Severity      | Alert categories |
| ------------- | ---------------: |
| High          |                0 |
| Medium        |                0 |
| Low           |                2 |
| Informational |                1 |

The reported findings concerned application error disclosure and debug error messages.

The assessment also produced a high proportion of HTTP 500 responses under automated input mutation. These responses are being investigated to distinguish input validation failures, exception handling issues, and actual application defects.

The results represent a point-in-time assessment rather than a comprehensive security certification.

---

## Automated Testing

The project uses JUnit 5, Mockito, AssertJ, and Spring Boot's testing infrastructure.

### Unit Tests

Unit tests cover service-layer behavior, scheduling rules, authentication utilities, and selected security-sensitive operations.

Scheduling tests include scenarios involving:

- Employee availability and overlapping appointments.
- Working-hour boundaries.
- Weekend handling.
- Invalid or historical dates.
- Appointment rescheduling.

Security-focused tests include cases involving:

- Unauthorized conversation access.
- Invalid, expired, and reused authentication or recovery tokens.
- Password reset behavior.
- OAuth2 authorization code reuse.
- Exception handling and response sanitization.

### Integration Tests

Persistence integration tests use `@DataJpaTest` with H2 to verify repository behavior, derived queries, and JPA mappings.

Spring application context tests check dependency injection and configuration initialization.

Run the test suite with:

```bash
mvn test
```

---

## Project Structure

```text
src/
├── main/
│   ├── java/pl/lukbol/dyplom/
│   │   ├── classes/         # JPA entities
│   │   ├── common/          # Shared constants and security paths
│   │   ├── configs/         # Security, JWT, OAuth2, WebSocket
│   │   ├── controllers/     # REST endpoints
│   │   ├── DTOs/            # Request and response models
│   │   ├── exceptions/      # Domain exceptions and handlers
│   │   ├── repositories/    # Spring Data JPA
│   │   ├── services/        # Business logic
│   │   └── utilities/       # Shared application utilities
│   └── resources/           # Application configuration
└── test/
    └── java/pl/lukbol/dyplom/
        ├── unitTests/
        └── integrationTests/
```

---

## Getting Started

### Requirements

- Java 17 or newer
- Maven
- PostgreSQL or H2

### Configuration

Clone the repository:

```bash
git clone https://github.com/LBolechow/tailoring-workshop-order-management.git
cd tailoring-workshop-order-management
```

Create the local application configuration:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Configure the required properties:

| Property                                                          | Description                                                         |
| ----------------------------------------------------------------- | ------------------------------------------------------------------- |
| `jwt.secret`                                                      | Base64-encoded HMAC key containing at least 256 bits of random data |
| `spring.security.oauth2.client.registration.google.client-id`     | Google OAuth2 client identifier                                     |
| `spring.security.oauth2.client.registration.google.client-secret` | Google OAuth2 client secret                                         |
| `app.admin.*`                                                     | Initial administrator account configuration                         |

A suitable JWT signing key can be generated locally:

```bash
openssl rand -base64 32
```

Select the appropriate database configuration in `application.properties`. H2 is available for local development, while PostgreSQL can be configured as the primary database.

Google OAuth2 credentials are required when using Google authentication.

### Run

```bash
mvn spring-boot:run
```

By default, the application runs at:

```text
http://localhost:8080
```

### API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

The API can be tested using Swagger UI, Postman, or other HTTP clients.

---

## Development Roadmap

- [ ] Complete request validation coverage across all endpoints.
- [ ] Improve handling of malformed requests and unexpected HTTP 500 responses.
- [ ] Extend object-level authorization checks to remaining conversation operations.
- [ ] Complete OAuth2 code exchange hardening and end-to-end verification.
- [ ] Review HTTP security headers, CORS, and Spring Security configuration.
- [ ] Introduce rate limiting for authentication and password recovery.
- [ ] Audit third-party dependencies using OWASP Dependency-Check.
- [ ] Extend security integration and regression testing.
- [ ] Repeat authenticated DAST assessment following remediation.
- [ ] Add refresh token support.

---

## Background

The original application was developed as an engineering thesis project at the University of Applied Sciences in Nysa.

The backend rewrite was undertaken to revisit the original implementation with a stronger emphasis on separation of concerns, testability, API design, and security.

The repository is maintained as an independent software engineering project and an environment for continued backend and application security development.

## Author

**Łukasz Bolechów**

[GitHub](https://github.com/LBolechow) · [LinkedIn](https://www.linkedin.com/in/lukaszbolechow99)
