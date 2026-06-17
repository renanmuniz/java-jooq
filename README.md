# java-jooq

Repo for the study of Java Object-Oriented Querying (jOOQ).  
https://www.jooq.org/

Video that I used to learn this: https://www.youtube.com/watch?v=6av6oUZK_zc

---

## What is jOOQ?

jOOQ is a Java library that lets you write SQL queries using a **type-safe, fluent Java API** instead of raw SQL strings.
It generates Java classes from your database schema so you interact with the database through Java objects, catching mistakes at compile time rather than runtime.

---

## Project Structure

```
java-jooq/
├── docker-compose.yml                  # PostgreSQL 18 (Alpine) container definition
├── pom.xml                             # Maven build: Spring Boot 3, jOOQ, PostgreSQL driver
└── src/
    ├── main/
    │   ├── java/com/jooq/renanmuniz/java_jooq/
    │   │   ├── JavaJooqApplication.java      # Spring Boot entry point
    │   │   │
    │   │   │   ── Application Layer ──
    │   │   ├── UserController.java           # REST controller: POST /users, GET /users/{userName}, DELETE /users/{userName}
    │   │   ├── AccessLogController.java      # REST controller: POST /access, GET /access/{userName}
    │   │   │
    │   │   │   ── Domain Layer ──
    │   │   ├── UserDTO.java                  # Immutable record: id, userName, email
    │   │   ├── AccessLogDTO.java             # Immutable record: userId, userName, accessTime
    │   │   ├── UserRepository.java           # Repository interface: findByUserName, create, delete
    │   │   ├── AccessLogRepository.java      # Repository interface: findByUserName, create
    │   │   │
    │   │   │   ── Infrastructure Layer ──
    │   │   ├── UserRepositoryImpl.java       # jOOQ-backed repository implementation (DSLContext)
    │   │   ├── AccessLogRepositoryImpl.java  # jOOQ-backed repository with JOIN example
    │   │   └── UserMapper.java               # Maps UsersRecord → UserDTO
    │   │
    │   └── resources/
    │       ├── application.properties        # DataSource and jOOQ configuration
    │       └── create_table.sql              # DDL mounted into the PostgreSQL container as the init script
    │
    └── test/
        └── java/com/jooq/renanmuniz/java_jooq/
            └── JavaJooqApplicationTests.java # Spring Boot context smoke test
```

### Key classes at a glance

| Layer | Class / File | Responsibility |
|---|---|---|
| Controller | `UserController` / `AccessLogController` | Handle HTTP requests; map exceptions to HTTP status codes |
| DTO | `UserDTO` / `AccessLogDTO` | Immutable response records (password excluded) |
| Request | `CreateUserRequest` / `CreateAccessLogRequest` | Immutable input records |
| Repository interface | `UserRepository` / `AccessLogRepository` | Data-access contracts, decoupled from jOOQ |
| Repository impl | `UserRepositoryImpl` / `AccessLogRepositoryImpl` | Use jOOQ `DSLContext` to run type-safe SQL against PostgreSQL |
| Mapper | `UserMapper` | Converts jOOQ-generated `UsersRecord` into `UserDTO` |
| Generated sources | `target/generated-sources/jooq/` | Type-safe table/record classes auto-generated from the live DB schema |
| Infrastructure | `docker-compose.yml` | Spins up the PostgreSQL instance required for local development and code generation |

> **Note:** The jOOQ generated classes (`DefaultCatalog`, `Public`, `Tables`, `Users`, `UsersRecord`, etc.) live under
> `target/generated-sources/jooq/` and are **not** committed to source control — they are always regenerated from the
> database by running `./mvnw clean generate-sources`.

---

## Quick Start

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Generate jOOQ classes from the live schema
./mvnw clean generate-sources

# 3. Run the application
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

---

## Documentation

For a detailed walkthrough of the project — including setup, configuration, jOOQ core concepts, CRUD examples, architecture, API reference, and common pitfalls — see **[JOOQ_GUIDE.md](./JOOQ_GUIDE.md)**.
