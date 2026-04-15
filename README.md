# experimentKS

`experimentKS` is a single-service Kotlin/Spring backend for the `experimentKmp` mobile app. It is structured as a modular monolith with feature packages for auth, finance data, dashboard aggregation, and shared platform concerns.

## Architecture Summary

- `auth`: signup, login, refresh-token rotation, logout, password hashing, JWT issuing
- `finance.category`: user-owned categories, default category bootstrap, validation
- `finance.transaction`: user-owned transactions, filtering, pagination, sorting, soft deletion
- `dashboard`: backend-computed summary, chart data, category breakdown, recent transactions
- `shared`: error handling, security helpers, persistence base classes, page responses

Key tradeoffs:

- A package-by-feature monolith keeps the codebase easy to grow without premature microservice boundaries.
- Access tokens are stateless JWTs, while refresh tokens are persisted and revocable for safer session management.
- Transactions and categories carry `updatedAt` and `deletedAt` so the API is ready for offline-first sync later.
- Dashboard aggregation is computed in the backend service layer for predictable mobile clients, even if some queries are not yet heavily optimized.

## Package Structure

```text
com.erkan.experimentks
├── auth
│   ├── api
│   ├── application
│   └── domain
├── config
├── dashboard
│   ├── api
│   └── application
├── finance
│   ├── category
│   │   ├── api
│   │   ├── application
│   │   └── domain
│   ├── transaction
│   │   ├── api
│   │   ├── application
│   │   └── domain
│   └── TransactionType.kt
└── shared
    ├── api
    ├── domain
    ├── pagination
    └── security
```

## Implemented Endpoints

Public:

- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /actuator/health`
- `GET /swagger-ui.html`
- `GET /v3/api-docs`

Authenticated:

- `GET /api/v1/users/me`
- `GET /api/v1/categories`
- `POST /api/v1/categories`
- `GET /api/v1/transactions`
- `POST /api/v1/transactions`
- `GET /api/v1/transactions/{id}`
- `PUT /api/v1/transactions/{id}`
- `DELETE /api/v1/transactions/{id}`
- `GET /api/v1/dashboard?period=MONTH`

## Environment Variables

Required in production:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`

Optional with defaults:

- `PORT=8080`
- `JWT_ACCESS_TOKEN_TTL=PT15M`
- `JWT_REFRESH_TOKEN_TTL=P30D`
- `APP_BASE_URL`
- `LOGGING_LEVEL_COM_ERKAN_EXPERIMENTKS=INFO`

## Local Run

1. Copy the environment template:

```bash
cp .env.example .env
```

2. Start PostgreSQL:

```bash
docker compose up -d postgres
```

3. Run the backend locally:

```bash
export $(grep -v '^#' .env | xargs)
./gradlew bootRun
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Test Commands

Integration tests use PostgreSQL Testcontainers:

```bash
./gradlew test
```

## Docker Commands

Build the container:

```bash
docker build -t experimentks:local .
```

Run the full app stack:

```bash
docker compose up --build
```

## Render Deploy

The repository includes `render.yaml` for a Render Blueprint deploy with:

- one Docker web service
- one managed PostgreSQL instance
- `SPRING_DATASOURCE_URL` wired from the database connection string
- `/actuator/health` configured as the health check

Manual deploy steps:

1. Push the repository to GitHub, GitLab, or Bitbucket.
2. In Render, create a new Blueprint and select the repo.
3. Confirm the `experimentks` web service and `experimentks-db` database.
4. Provide a strong `JWT_SECRET` when prompted.
5. Deploy the Blueprint.
6. After the first deploy, open the service URL and verify `/actuator/health` and `/swagger-ui.html`.

The app reads Render’s `PORT` environment variable automatically through Spring Boot configuration.

Reference docs used for the Blueprint shape and Postgres connection wiring:

- [Render Blueprint YAML Reference](https://render.com/docs/blueprint-spec)
- [Render Postgres Docs](https://render.com/docs/postgresql)

## Recommended Next Steps for v2

- recurring transactions and scheduled expense generation
- sync endpoints that filter by `updatedAt` / `deletedAt`
- budget envelopes and savings goals
- CSV import/export
- audit trail for sensitive finance mutations
- household/shared-account support
