# Tailoring Workshop — Order Management System

A REST API backend for managing orders, scheduling, and client/employee communication in a tailoring workshop.

Originally built as an engineering thesis project — a full-stack Spring Boot application with a server-rendered frontend (Thymeleaf, Bootstrap, jQuery). This repository is a backend rewrite of that project: the same business domain and functionality, rebuilt as a standalone, tested REST API with improved separation of concerns, security, and maintainability.

> Original thesis version (full-stack): [Praca-dyplomowa-SpringBoot-old](https://github.com/LBolechow/Praca-dyplomowa-SpringBoot-old)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Security | Spring Security, JWT, OAuth2 (Google) |
| Persistence | Hibernate / JPA, PostgreSQL / H2 |
| Real-time | WebSocket (STOMP) |
| Validation | Bean Validation (Hibernate Validator) |
| Documentation | OpenAPI 3 (springdoc) |
| Testing | JUnit 5, Mockito, AssertJ |
| Build | Maven |
| Other | Lombok, Java Records (DTOs), SLF4J |

---

## Features

**Public**
- Registration and login (local account or Google OAuth2)
- Password recovery using one-time reset tokens
- Order status lookup by unique code
- Price list

**All authenticated users**
- Profile management
- Notifications
- Real-time messaging via WebSocket

**Client role**
- Full order history (in progress and completed)
- Direct messaging with employees

**Employee role**
- Order calendar — 30-day and daily view (own assignments only)
- Order creation with automatic scheduling — the system finds the next available time slot and assigns an available employee
- Order rescheduling to a new slot, same or different employee
- Materials checklist per order
- Messaging with clients and other employees

**Administrator role**
- Everything available to employees, across all staff (full schedule, all materials)
- Price list management
- User account management — create, update, delete, assign roles
- Notification broadcasting to selected employees

---

## What Changed From the Original

The original thesis project combined backend logic with server-rendered views. This version separates the API from the presentation layer and reorganizes the codebase around maintainability, testing, and clearer responsibilities.

**API design**
- Replaced server-rendered views and raw `Map<String, Object>` payloads with typed request and response DTOs, primarily using Java records
- Removed the Thymeleaf frontend; the application now exposes a standalone REST API
- Endpoints handling user, order, and conversation data return DTOs rather than JPA entities
- Request validation using Bean Validation, with constraint violations mapped to structured `400 Bad Request` responses

**Error handling**
- Centralized exception handling with `@RestControllerAdvice`
- Custom exception hierarchy (`ApplicationException`) mapped to consistent JSON responses and appropriate HTTP status codes
- Services throw domain exceptions rather than returning `ResponseEntity`
- Internal exception details are logged server-side instead of being included in generic error responses
- Dedicated handling for malformed request bodies and invalid input formats

**Security**
- JWT-based stateless authentication, with token invalidation on logout using a blacklist and scheduled cleanup of expired entries
- Google OAuth2 integration with externalized configuration and a short-lived, single-use code exchange mechanism
- Role-based endpoint authorization configured through Spring Security
- Object-level authorization checks for conversation access, verifying that the authenticated user belongs to the requested conversation
- Password recovery using cryptographically secure reset tokens instead of sending replacement passwords by email
- Reset tokens generated using `SecureRandom`, stored as SHA-256 hashes, and subject to expiration and single-use restrictions
- Centralized security path configuration and explicit access rules for clients, employees, and administrators
- Roles defined in a single `UserRole` enum, distinguishing role names from Spring Security authorities

**Code structure**
- Constructor injection (`@RequiredArgsConstructor`) throughout
- Thin REST controllers delegating business operations to services
- Shared functionality extracted into utility classes (`OrderUtils`, `UserUtils`)
- Domain-specific exceptions and centralized HTTP error handling
- User-facing and diagnostic messages organized through the `Messages` class
- Structured logging with SLF4J for authentication events, data modifications, and unexpected exceptions

**Testing**
- Unit tests covering services, scheduling logic, JWT utilities, and password recovery
- Security-focused regression tests for resource authorization, token validation, and OAuth2 code reuse
- Integration tests for persistence using H2 (`@DataJpaTest`), covering repository queries and JPA mappings
- Application context testing (`@SpringBootTest`) to detect configuration and dependency injection issues
- Dedicated scheduling tests covering weekend skipping, working-hour boundaries, overlapping orders, and invalid dates

---

## Project Structure

```text
src/main/java/pl/lukbol/dyplom/
├── classes/           # JPA entities
├── common/            # Shared constants (Messages, SecurityPaths, UserRole)
├── configs/           # Security, JWT, OAuth2, WebSocket, scheduled tasks
├── controllers/       # REST controllers
├── DTOs/              # Request/response records
├── exceptions/        # Custom exceptions and GlobalExceptionHandler
├── repositories/      # Spring Data JPA repositories
├── services/          # Business logic
└── utilities/         # OrderUtils, UserUtils, JwtUtil, DateUtils, ...

src/test/java/pl/lukbol/dyplom/
├── unitTests/         # Service and utility unit tests
└── integrationTests/  # Repository integration tests
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven
- PostgreSQL (optional — H2 file-based database is configured by default)

### Configuration

Copy the example configuration and fill in your own values:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

The following properties require configuration:

| Property | Notes |
|---|---|
| `jwt.secret` | Base64-encoded key containing at least 256 bits of random data |
| `spring.security.oauth2.client.registration.google.client-id` | Google Cloud Console → Credentials |
| `spring.security.oauth2.client.registration.google.client-secret` | Google OAuth2 client secret |
| `app.admin.*` | Initial administrator account configuration |

Generate a JWT signing key locally:

```bash
openssl rand -base64 32
```

The database configuration contains separate blocks for H2 and PostgreSQL. Enable the desired configuration and disable the other.

An administrator account is created automatically on first startup using the `app.admin.*` properties.

### Run

```bash
git clone https://github.com/LBolechow/tailoring-workshop-order-management
cd tailoring-workshop-order-management
mvn spring-boot:run
```

### API Documentation

With the application running, Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

The OpenAPI specification is exposed at:

```text
http://localhost:8080/v3/api-docs
```

The H2 console, when enabled, is available at:

```text
http://localhost:8080/h2-console
```

### Test

```bash
mvn test
```

---

## Author

**Łukasz Bolechów** — [GitHub](https://github.com/LBolechow)
