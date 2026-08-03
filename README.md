# Tailoring Workshop Order Management Application (Refactored Version)

## Overview

This repository contains a refactored version of my original tailoring workshop order management application.

The initial version was created as an engineering thesis project. After reviewing the codebase, I identified areas that could be improved in terms of architecture, maintainability, security, and code organization. This version focuses on applying cleaner design practices and improving the overall structure of the application.

For the original implementation, see [Old version](https://github.com/LBolechow/Praca-dyplomowa-SpringBoot-old).

## Tech Stack

- Java
- Spring Boot
- Spring Security
- JWT Authentication
- OAuth2
- Hibernate / JPA
- PostgreSQL
- Maven

## Key Improvements

### DTO-based API

Replaced direct entity exposure with dedicated DTOs using Java records for request and response objects. This provides clearer API contracts and reduces coupling between the database layer and API layer.

### Global Exception Handling

Introduced centralized exception handling to provide consistent error responses and improve API readability.

### Improved Application Structure

Refactored controllers, services, and utility classes to improve separation of responsibilities, readability, and maintainability.

### Enhanced Security

Reworked authentication and authorization flow:
- Added JWT-based authentication
- Implemented token invalidation
- Improved Spring Security configuration
- Refactored OAuth2 integration

### Code Quality Improvements

- Removed duplicated logic
- Moved reusable constants and messages into dedicated classes
- Improved naming and organization across the codebase
- Prepared the project structure for easier testing and future development

## Planned Improvements

- Adding integration tests
- Expanding unit test coverage
- Further optimization and cleanup of application components
