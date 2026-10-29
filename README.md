# Tailoring Workshop — Order Management System

A REST API backend for managing orders, scheduling, and client/employee communication in a tailoring workshop.

Originally built as an engineering thesis project — a full-stack Spring Boot application with a server-rendered frontend (Thymeleaf, Bootstrap, jQuery). This repository is a ground-up backend rewrite of that project: the same business domain and functionality, rebuilt as a clean, tested, API-only backend following current Spring Boot practices.

> Thesis version (full-stack, original submission): [Praca-dyplomowa-SpringBoot-old](https://github.com/LBolechow/Praca-dyplomowa-SpringBoot-old)

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

The original thesis project worked, but the codebase reflected the pace of a first full-stack build under a deadline. This version is a rewrite of the backend focused on production-oriented practices:

**API design**
- Replaced server-rendered views and raw `Map<String, Object>` payloads with a typed DTO layer built on Java records — explicit, self-documenting request/response contracts
- Removed the Thymeleaf frontend entirely; the application is now a pure REST API
- Endpoints handling user, order and conversation data return DTOs rather than entities, so password hashes and lazy relations can't leak into responses
- Request validation via Bean Validation, with constraint violations mapped to structured `400` responses

**Error handling**
- Centralized exception handling with `@RestControllerAdvice`
- Custom exception hierarchy (`ApplicationException`) mapped to consistent, structured JSON error responses with correct HTTP status codes
- Services throw domain exceptions instead of returning `ResponseEntity` — the HTTP layer stays in the controllers

**Security**
- JWT-based stateless authentication with token invalidation on logout, backed by a blacklist and a scheduled cleanup of expired entries
- Rebuilt OAuth2 (Google) integration with externalized configuration — no hardcoded redirect URLs or credentials
- Reworked Spring Security route configuration around explicit per-role access rules
- Roles defined in a single `UserRole` enum, which also encodes the distinction between the Spring Security role name (`ADMIN`) and the stored authority (`ROLE_ADMIN`)

**Code structure**
- Constructor injection (`@RequiredArgsConstructor`) throughout — no field injection
- Thin controllers: `@RestController`, no business logic, pure delegation to services
- Business logic extracted into dedicated utility classes (`OrderUtils`, `UserUtils`) to keep services focused and testable
- User-facing and diagnostic messages centralized in a single `Messages` class rather than scattered string literals
- Structured logging with SLF4J on authentication events, data-modifying operations and unhandled exceptions

**Testing**
- Unit tests covering the services and the scheduling and JWT utilities (JUnit 5 + Mockito + AssertJ)
- Integration tests for the persistence layer on H2 (`@DataJpaTest`), which catch derived-query and `@EntityGraph` errors that mocked tests cannot
- Context test (`@SpringBootTest`) guarding against bean conflicts, circular dependencies and invalid configuration
- Business-critical logic — the availability-scheduling algorithm — covered with dedicated edge-case tests: weekend skipping, workday boundaries, overlapping orders, past-date handling

---

## Project Structure

```
src/main/java/pl/lukbol/dyplom/
├── classes/           # JPA entities
├── common/            # Shared constants (Messages, SecurityPaths, UserRole)
├── configs/           # Security config, JWT filter, OAuth2 handler, scheduled tasks
├── controllers/       # REST controllers
├── DTOs/              # Request/response records
├── exceptions/        # Custom exceptions + GlobalExceptionHandler
├── repositories/      # Spring Data JPA repositories
├── services/          # Business logic
└── utilities/         # OrderUtils, UserUtils, JwtUtil, DateUtils, ...

src/test/java/pl/lukbol/dyplom/
├── unitTests/         # Service and utility unit tests
└── integrationTests/  # Repository tests on H2
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

Three values must be set before the first run:

| Property | Notes |
|---|---|
| `jwt.secret` | Base64 string, at least 256 bits — e.g. `openssl rand -base64 32` |
| `spring.security.oauth2.client.registration.google.client-id` | From Google Cloud Console → Credentials |
| `spring.security.oauth2.client.registration.google.client-secret` | As above |

The database section contains two blocks — H2 (default) and PostgreSQL. Uncomment whichever you need and comment out the other. An administrator account is created automatically on first startup using the `app.admin.*` properties.

### Run

```bash
git clone https://github.com/LBolechow/tailoring-workshop-order-management
cd tailoring-workshop-order-management
mvn spring-boot:run
```

### API Documentation

With the application running, the interactive OpenAPI documentation is available at:

```
http://localhost:8080/swagger-ui/index.html
```

The H2 console (when H2 is enabled) is at `http://localhost:8080/h2-console`.

### Test

```bash
mvn test
```

---

## Roadmap

- [ ] Refresh token support
- [ ] Validation coverage extended to the remaining request DTOs
- [ ] Date fields typed as `LocalDate` instead of `String` in order DTOs

---

## Author

**Łukasz Bolechów** — [GitHub](https://github.com/LBolechow)
