# Bzcom CRM

[![Backend CI](https://github.com/AnNguyen05092004/Customer-Request-Management-System-CRM-/actions/workflows/backend-ci.yml/badge.svg?branch=develop)](https://github.com/AnNguyen05092004/Customer-Request-Management-System-CRM-/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/AnNguyen05092004/Customer-Request-Management-System-CRM-/actions/workflows/frontend-ci.yml/badge.svg?branch=develop)](https://github.com/AnNguyen05092004/Customer-Request-Management-System-CRM-/actions/workflows/frontend-ci.yml)

> Customer Request Management System for Bzcom — a modular-monolith CRM that centralizes client requests, assignment, workflow status, history, alerts and LLM-assisted classification.

## Stack

- **Backend:** Java 21, Spring Boot 3.5, Spring Security/JWT, PostgreSQL 16, Flyway, MapStruct.
- **Frontend:** React 19, TypeScript, Vite, Ant Design, TanStack Query.
- **Quality and delivery:** JUnit 5, Testcontainers, Spotless, JaCoCo, Docker Compose and GitHub Actions.

## Quick start

Requirements: Docker Desktop with Docker Compose.

```bash
git clone https://github.com/AnNguyen05092004/Customer-Request-Management-System-CRM-.git
cd Customer-Request-Management-System-CRM-
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | <http://localhost:5173> |
| Swagger UI | <http://localhost:8080/swagger-ui/index.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| Backend health | <http://localhost:8080/actuator/health> |

Stop the stack with `docker compose down`.

## Demo accounts

All seeded demo accounts use `1234`.

| Email | Role |
|---|---|
| `admin@bzcom.com` | ADMIN |
| `dev1@bzcom.com` | DEVELOPER |
| `dev2@bzcom.com` | DEVELOPER |
| `client1@bzcom.com` | CLIENT |

## API contract

The canonical API specification is [openapi.yaml](./openapi.yaml). The rendered endpoint guide and examples are in [OPENAPI.md](./OPENAPI.md).

## Documentation

- [ANALYZE.md](./ANALYZE.md) — requirements, actors and acceptance criteria.
- [ARCHITECTURE.md](./ARCHITECTURE.md) — architecture and technical decisions.
- [ERD.md](./ERD.md) — database design and migrations.
- [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md) — workflow, assignment, alerts and statistics.
- [FRONTEND.md](./FRONTEND.md) — frontend structure and API integration.
- [TASKS.md](./TASKS.md) — implementation plan and ownership.
- [TEAM_DEVELOPMENT_GUIDE.md](./TEAM_DEVELOPMENT_GUIDE.md) — environment and team workflow.
- [GIT_WORKFLOW.md](./GIT_WORKFLOW.md) — branching, pull requests and CI.
- [BACKEND_CODING_RULES.md](./BACKEND_CODING_RULES.md) — Spring Boot coding conventions.

Before changing code or documentation, read the repository working agreement in [CLAUDE.md](../CLAUDE.md).

## Local verification

```bash
cd BE && ./mvnw verify
cd ../FE && npm ci && npm run build
```

The full backend verification uses Testcontainers and requires Docker.
