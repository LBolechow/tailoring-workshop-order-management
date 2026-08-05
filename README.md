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
| Persistence | Hibernate / JPA, PostgreSQL |
| Real-time | WebSocket (STOMP) |
| Testing | JUnit 5, Mockito, AssertJ |
| Build | Maven |
| Other | Lombok, Java Records (DTOs) |

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

**Error handling**
- Centralized exception handling with `@RestControllerAdvice`
- Custom exception hierarchy (`ApplicationException`) mapped to consistent, structured JSON error responses with correct HTTP status codes

**Security**
- JWT-based stateless authentication with token blacklisting on logout
- Rebuilt OAuth2 (Google) integration with externalized, environment-based configuration — no hardcoded redirect URLs or credentials
- Reworked Spring Security route configuration around explicit per-role access rules

**Code structure**
- Constructor injection (`@RequiredArgsConstructor`) throughout — no field injection
- Thin controllers: `@RestController`, no business logic, pure delegation to services
- Business logic extracted into dedicated utility classes (`OrderUtils`, `UserUtils`) to keep services focused and testable
- User-facing and diagnostic messages centralized in a single `Messages` class rather than scattered string literals
- Fixed logic bugs uncovered during the rewrite (null-handling in scheduling, silently-dead exception handlers, inconsistent transactional boundaries)

**Testing**
- Full unit test suite covering every service and utility class (JUnit 5 + Mockito + AssertJ)
- Business-critical logic — like the availability-scheduling algorithm — covered with dedicated edge-case tests (weekend skipping, workday boundaries, overlapping orders)

---

## Project Structure

```
src/main/java/pl/lukbol/dyplom/
├── classes/          # JPA entities
├── common/           # Shared constants (Messages, SecurityPaths)
├── configs/          # Security config, JWT filter, OAuth2 handler
├── controllers/      # REST controllers
├── DTOs/             # Request/response records
├── exceptions/       # Custom exceptions + GlobalExceptionHandler
├── repositories/     # Spring Data JPA repositories
├── services/         # Business logic
└── utilities/        # OrderUtils, UserUtils, JwtUtil, DateUtils, ...

src/test/java/pl/lukbol/dyplom/
├── services/          # Unit tests per service
└── utilities/         # Unit tests per utility class
```

---

## Getting Started

### Prerequisites
- Java 17+
- PostgreSQL
- Maven

### Configuration

Set the following in `application.yml` (or via environment variables):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/your_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

app:
  oauth2:
    redirect-url: ${OAUTH2_REDIRECT_URL}
```

### Run

```bash
git clone https://github.com/LBolechow/tailoring-workshop-order-management
cd tailoring-workshop-order-management
mvn spring-boot:run
```

### Test

```bash
mvn test
```

---

## Roadmap

- [ ] Integration tests (`@SpringBootTest` + H2)
- [ ] Request validation (Bean Validation)
- [ ] OpenAPI / Swagger documentation
- [ ] Refresh token support
- [ ] Structured logging (SLF4J)

---

## Author

**Łukasz Bolechów** — [GitHub](https://github.com/LBolechow)
