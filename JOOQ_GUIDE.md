# jOOQ — A Beginner's Guide

> This guide uses the **java-jooq** project as a hands-on reference.  
> All code examples are taken directly from the working codebase.

---

## Table of Contents

1. [What is jOOQ?](#1-what-is-jooq)
2. [Core Concepts](#2-core-concepts)
3. [Project Setup](#3-project-setup)
   - [Maven Dependencies](#31-maven-dependencies)
   - [Code Generator Plugin](#32-code-generator-plugin)
   - [Application Properties](#33-application-properties)
4. [Database Schema](#4-database-schema)
5. [Code Generation](#5-code-generation)
6. [Using DSLContext](#6-using-dslcontext)
7. [CRUD Operations — Real Examples](#7-crud-operations--real-examples)
   - [SELECT — Find a record](#71-select--find-a-record)
   - [INSERT — Create a record](#72-insert--create-a-record)
   - [DELETE — Remove a record with RETURNING](#73-delete--remove-a-record-with-returning)
   - [JOIN — Query across tables](#74-join--query-across-tables)
8. [Mapping Records to DTOs](#8-mapping-records-to-dtos)
9. [Project Architecture](#9-project-architecture)
10. [How to Run the Project](#10-how-to-run-the-project)
11. [API Reference](#11-api-reference)
12. [Common Pitfalls](#12-common-pitfalls)
13. [Quick Reference Cheatsheet](#13-quick-reference-cheatsheet)

---

## 1. What is jOOQ?

**jOOQ** (Java Object-Oriented Querying) is a Java library that lets you write SQL queries using a **type-safe, fluent Java API** instead of raw SQL strings.

### The core problem it solves

Without jOOQ, you might write SQL like this:
```java
// Raw SQL — no compile-time safety. Typos only blow up at runtime.
String sql = "SELECT * FROM usres WHERE userneme = ?"; // typos are silent
```

With jOOQ:
```java
// Type-safe — typos are caught at compile time.
ctx.selectFrom(Users.USERS)
   .where(Users.USERS.USERNAME.eq(username))
   .fetchOne();
```

### Key benefits

| Benefit | Description |
|---|---|
| **Type safety** | Column names, types, and table references are Java classes — the compiler catches mistakes |
| **SQL-first** | You write real SQL, not an ORM abstraction. jOOQ just wraps it in Java |
| **Code generation** | Java classes are generated directly from your live database schema |
| **Database-specific features** | Supports `RETURNING`, `ON CONFLICT`, window functions, and other dialect-specific SQL |
| **No magic** | Queries are predictable and readable — you know exactly what SQL is generated |

### jOOQ vs JPA/Hibernate

| | jOOQ | JPA / Hibernate |
|---|---|---|
| Approach | SQL-first (write SQL in Java) | Object-first (Java objects mapped to tables) |
| Complex queries | Excellent (native SQL support) | Cumbersome (JPQL/Criteria API) |
| Generated SQL | Transparent and predictable | Can be surprising / N+1 prone |
| Learning curve | Low if you know SQL | Higher (lots of annotations, magic) |

---

## 2. Core Concepts

Before writing code, understand these five building blocks:

### `DSLContext`
The **entry point** for all jOOQ operations. It holds the database connection and SQL dialect.  
Think of it as the object you use to "talk" to the database.

```java
// You inject it via Spring — never create it manually
private final DSLContext ctx;
```

### Generated Table Classes
jOOQ reads your database schema and generates a Java class for each table.  
Example: the `users` table becomes the `Users` class with a static field `Users.USERS`.

```java
Users.USERS          // the table itself
Users.USERS.USERNAME // a specific column — type-safe!
Users.USERS.EMAIL
Users.USERS.ID
```

### `Record`
A **row** from a database table, represented as a Java object.  
jOOQ generates a `UsersRecord` class with getters/setters for every column.

```java
UsersRecord record = ctx.selectFrom(Users.USERS).fetchOne();
record.getUsername(); // strongly typed getter
record.getEmail();
```

### `Result`
A **list of records** returned by a query. Think of it as `List<Record>`.

### SQL Dialect
Tells jOOQ which database you're using so it can generate the right SQL syntax.  
This project uses `POSTGRES`, which enables PostgreSQL-specific features like `RETURNING`.

---

## 3. Project Setup

### 3.1 Maven Dependencies

Add these two dependencies to your `pom.xml`:

```xml
<!-- jOOQ + Spring Boot auto-configuration -->
<!-- Automatically creates and configures the DSLContext bean -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jooq</artifactId>
</dependency>

<!-- PostgreSQL JDBC driver -->
<!-- runtime scope: not needed to compile, only to run -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

> `spring-boot-starter-jooq` does the heavy lifting: it reads your `spring.datasource.*` properties and automatically creates a `DSLContext` bean ready to inject.

---

### 3.2 Code Generator Plugin

This Maven plugin **connects to your live database** and generates Java classes from the schema.  
Add it inside `<build><plugins>`:

```xml
<plugin>
    <groupId>org.jooq</groupId>
    <artifactId>jooq-codegen-maven</artifactId>
    <executions>
        <execution>
            <!-- Runs automatically during the generate-sources phase -->
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- JDBC connection used ONLY at code-generation time (not at runtime) -->
        <jdbc>
            <driver>org.postgresql.Driver</driver>
            <url>jdbc:postgresql://localhost:5432/jooq_study</url>
            <user>dev</user>
            <password>dev123</password>
        </jdbc>
        <generator>
            <!-- Package where generated classes will be placed -->
            <target>
                <packageName>com.jooq.renanmuniz.java_jooq</packageName>
            </target>
            <!-- Which database dialect + what to exclude -->
            <database>
                <name>org.jooq.meta.postgres.PostgresDatabase</name>
                <excludes>pg_catalog.* | information_schema.*</excludes>
            </database>
        </generator>
    </configuration>
</plugin>
```

**What gets generated?** For every table in your schema, jOOQ creates:

| Generated class | What it is |
|---|---|
| `Users` | Represents the `users` table and all its columns |
| `UsersRecord` | Represents one row of the `users` table, with getters/setters |
| `AccessLog` | Represents the `access_log` table |
| `AccessLogRecord` | Represents one row of the `access_log` table |
| `Tables` | A single class with static references to every table |
| `Keys` | Foreign key and primary key definitions |
| `Public` | The default PostgreSQL schema |

> ⚠️ **Important:** The database must be running **before** you generate sources.  
> These classes land in `target/generated-sources/jooq/` and are **not** committed to git.

---

### 3.3 Application Properties

```properties
# DataSource — how Spring (and jOOQ) connect at runtime
spring.datasource.url=jdbc:postgresql://localhost:5432/jooq_study
spring.datasource.username=dev
spring.datasource.password=dev123
spring.datasource.driver-class-name=org.postgresql.Driver

# Tells the auto-configured DSLContext which SQL dialect to use
spring.jooq.sql-dialect=POSTGRES

# Shows error messages in HTTP responses (useful during development)
server.error.include-message=always
```

| Property | Why it matters |
|---|---|
| `spring.datasource.*` | Standard Spring Boot datasource configuration |
| `spring.jooq.sql-dialect=POSTGRES` | Enables PostgreSQL-specific SQL features like `RETURNING` |
| `server.error.include-message=always` | Surfaces exception messages in the HTTP error body |

---

## 4. Database Schema

The schema is defined in `src/main/resources/create_table.sql` and is automatically loaded by the PostgreSQL Docker container on first start.

```sql
CREATE TABLE IF NOT EXISTS users (
  id       INT          NOT NULL GENERATED BY DEFAULT AS IDENTITY,
  username VARCHAR(50)  NOT NULL UNIQUE,
  email    VARCHAR(50)  NOT NULL,
  password VARCHAR(50)  NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS access_log (
  id           INT          NOT NULL GENERATED BY DEFAULT AS IDENTITY,
  user_id      INT          NOT NULL REFERENCES users(id),
  logged_in_at TIMESTAMPTZ  DEFAULT NOW()
);
```

**Why does the schema file matter for jOOQ?**  
jOOQ does **not** read `.sql` files — it introspects a **live database**. The schema file initialises the database the first time the Docker container starts, so the tables exist when the code generator runs.

### How Docker wires the schema

```yaml
# docker-compose.yml
volumes:
  - ./src/main/resources/create_table.sql:/docker-entrypoint-initdb.d/init.sql
```

PostgreSQL's official Docker image automatically executes any `.sql` file placed in `/docker-entrypoint-initdb.d/` on the first container start.

---

## 5. Code Generation

### The flow

```
Database (running)
       │
       │  jooq-codegen-maven reads the live schema via JDBC
       ▼
target/generated-sources/jooq/
       ├── Users.java           ← table class
       ├── UsersRecord.java     ← row class
       ├── AccessLog.java
       ├── AccessLogRecord.java
       └── ...
       │
       │  You import these in your repositories
       ▼
UserRepositoryImpl.java
```

### Running code generation

```bash
# 1. Start the database first
docker compose up -d

# 2. Generate jOOQ classes
./mvnw clean generate-sources
```

After this command, the classes appear in `target/generated-sources/jooq/` and your IDE will recognise them as source roots.

---

## 6. Using `DSLContext`

`DSLContext` is the main jOOQ object. In a Spring Boot project, you simply inject it — Spring creates it automatically from your datasource.

```java
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final DSLContext ctx;  // injected by Spring

    public UserRepositoryImpl(DSLContext ctx) {
        this.ctx = ctx;
    }

    // Now use ctx.select(...), ctx.insertInto(...), etc.
}
```

All query operations start from `ctx`:

| Method | What it does |
|---|---|
| `ctx.selectFrom(table)` | SELECT all columns from a table |
| `ctx.select(fields...)` | SELECT specific columns |
| `ctx.newRecord(table)` | Create a new in-memory record (for INSERT) |
| `ctx.deleteFrom(table)` | Build a DELETE query |
| `ctx.insertInto(table)` | Build an INSERT query (alternative approach) |

---

## 7. CRUD Operations — Real Examples

### 7.1 SELECT — Find a record

**Goal:** Find a user by their username.

```java
// From: UserRepositoryImpl.java
public UsersRecord findByUserName(String username) {
    return ctx.selectFrom(Users.USERS)          // SELECT * FROM users
              .where(Users.USERS.USERNAME.eq(username)) // WHERE username = ?
              .fetchOne();                       // returns 1 row or null
}
```

**Breaking it down:**

| Part | SQL equivalent | Explanation |
|---|---|---|
| `ctx.selectFrom(Users.USERS)` | `SELECT * FROM users` | Selects all columns from `users` |
| `.where(Users.USERS.USERNAME.eq(username))` | `WHERE username = ?` | Type-safe filter — `eq()` maps to `=` |
| `.fetchOne()` | — | Executes the query, returns one `UsersRecord` or `null` |

> **`fetchOne()` vs `fetch()`**  
> - `fetchOne()` — returns one record or `null`. Throws if more than one result.  
> - `fetch()` — returns a `Result` (list of records).  
> - `fetchAny()` — returns the first record or `null`, no exception for multiple results.

---

### 7.2 INSERT — Create a record

**Goal:** Insert a new user into the database.

```java
// From: UserRepositoryImpl.java
public UsersRecord create(CreateUserRequest request) {
    var usersRecord = ctx.newRecord(Users.USERS);   // create an empty record
    usersRecord.setUsername(request.userName());    // set the columns
    usersRecord.setEmail(request.email());
    usersRecord.setPassword(request.password());
    int stored = usersRecord.store();               // executes INSERT

    if (stored != 1) {
        throw new RuntimeException("Failed to create user");
    }

    return usersRecord;  // the record now has the auto-generated id
}
```

**Breaking it down:**

| Step | What happens |
|---|---|
| `ctx.newRecord(Users.USERS)` | Creates a `UsersRecord` in memory — nothing goes to the DB yet |
| `usersRecord.setUsername(...)` | Sets column values using generated setters |
| `usersRecord.store()` | Executes `INSERT INTO users ...` and returns the number of affected rows |
| `stored != 1` | A guard: if no row was inserted, something went wrong |

> After `store()`, jOOQ automatically refreshes the record — the auto-generated `id` is now populated on `usersRecord`.

---

### 7.3 DELETE — Remove a record with `RETURNING`

**Goal:** Delete a user and return the deleted data in a single query.

```java
// From: UserRepositoryImpl.java
public UsersRecord delete(String username) {
    UsersRecord deleted = ctx.deleteFrom(Users.USERS)          // DELETE FROM users
                             .where(Users.USERS.USERNAME.eq(username)) // WHERE username = ?
                             .returning()                      // RETURNING * (PostgreSQL-specific!)
                             .fetchOne();                      // get the deleted row

    if (deleted == null) {
        throw new RuntimeException("User not found: " + username);
    }

    return deleted;
}
```

**The `RETURNING` clause:**  
This is a PostgreSQL-specific feature that returns the deleted (or inserted/updated) rows in the same statement — no extra `SELECT` needed.  
jOOQ enables this via `.returning()` because we set `spring.jooq.sql-dialect=POSTGRES`.

---

### 7.4 JOIN — Query across tables

**Goal:** Get all access logs for a user, including the username (which lives in a different table).

```java
// From: AccessLogRepositoryImpl.java
public List<AccessLogDTO> findByUserName(String userName) {
    return ctx.select(
                  AccessLog.ACCESS_LOG.USER_ID,    // select specific columns
                  Users.USERS.USERNAME,
                  AccessLog.ACCESS_LOG.LOGGED_IN_AT
              )
              .from(AccessLog.ACCESS_LOG)           // FROM access_log
              .join(Users.USERS)                    // JOIN users
                  .on(AccessLog.ACCESS_LOG.USER_ID.eq(Users.USERS.ID)) // ON access_log.user_id = users.id
              .where(Users.USERS.USERNAME.eq(userName)) // WHERE users.username = ?
              .orderBy(AccessLog.ACCESS_LOG.LOGGED_IN_AT.desc()) // ORDER BY logged_in_at DESC
              .fetch(r -> new AccessLogDTO(         // map each row to a DTO
                  r.get(AccessLog.ACCESS_LOG.USER_ID),
                  r.get(Users.USERS.USERNAME),
                  r.get(AccessLog.ACCESS_LOG.LOGGED_IN_AT)
              ));
}
```

**Breaking it down:**

| Part | SQL equivalent |
|---|---|
| `ctx.select(col1, col2, col3)` | `SELECT user_id, username, logged_in_at` |
| `.from(AccessLog.ACCESS_LOG)` | `FROM access_log` |
| `.join(Users.USERS).on(...)` | `JOIN users ON access_log.user_id = users.id` |
| `.where(Users.USERS.USERNAME.eq(userName))` | `WHERE users.username = ?` |
| `.orderBy(...desc())` | `ORDER BY logged_in_at DESC` |
| `.fetch(r -> new AccessLogDTO(...))` | Execute and map each row to an `AccessLogDTO` |

> **`.fetch(mapper)`** is a powerful overload: it executes the query and transforms each row using the lambda you provide. This avoids dealing with raw `Record` objects in the rest of your code.

---

## 8. Mapping Records to DTOs

jOOQ records contain **all** database columns, including sensitive ones like `password`. It's a best practice to map them to DTOs (Data Transfer Objects) before returning data to the API layer.

### The DTO

```java
// UserDTO.java — immutable Java record, no password field
public record UserDTO(int id, String userName, String email) {}
```

### The Mapper

```java
// UserMapper.java
public class UserMapper {
    public static UserDTO toUserDTO(UsersRecord user) {
        return new UserDTO(
            user.getId(),       // maps id column
            user.getUsername(), // maps username column
            user.getEmail()     // maps email column — password is intentionally excluded
        );
    }
}
```

### Using the mapper in the controller

```java
// UserController.java
@GetMapping("{userName}")
public UserDTO getUser(@PathVariable String userName) {
    var userRecord = userRepository.findByUserName(userName); // UsersRecord (has password)
    if (userRecord == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userName);
    }
    return toUserDTO(userRecord); // UserDTO (no password) ← safe to return
}
```

---

## 9. Project Architecture

This project follows a clean layered architecture:

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────┐
│  Controller Layer                   │
│  UserController / AccessLogController│
│  Maps HTTP → method calls           │
│  Maps exceptions → HTTP status codes│
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  Domain Layer                       │
│  UserRepository (interface)         │
│  Defines the data-access contract   │
│  No jOOQ here — just plain Java     │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  Infrastructure Layer               │
│  UserRepositoryImpl                 │
│  Uses DSLContext + generated classes│
│  Runs actual SQL against PostgreSQL │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  Generated Sources                  │
│  target/generated-sources/jooq/     │
│  Users, UsersRecord, AccessLog...   │
│  Auto-generated from live DB schema │
└─────────────────────────────────────┘
```

### Key classes at a glance

| Class | Type | Role |
|---|---|---|
| `UserController` | Spring `@RestController` | Receives HTTP requests, returns JSON responses |
| `AccessLogController` | Spring `@RestController` | Same for access log endpoints |
| `UserDTO` | Java `record` | Immutable response — id, userName, email (no password) |
| `AccessLogDTO` | Java `record` | Immutable response — userId, userName, accessTime |
| `CreateUserRequest` | Java `record` | Input for user creation |
| `CreateAccessLogRequest` | Java `record` | Input for access log creation |
| `UserRepository` | Interface | Data access contract — decoupled from jOOQ |
| `UserRepositoryImpl` | `@Repository` | jOOQ implementation of `UserRepository` |
| `AccessLogRepository` | Interface | Data access contract for access logs |
| `AccessLogRepositoryImpl` | `@Repository` | jOOQ implementation with JOIN example |
| `UserMapper` | Plain class | Converts `UsersRecord` → `UserDTO` |

---

## 10. How to Run the Project

### Prerequisites
- Java 21
- Maven (or use `./mvnw`)
- Docker

### Step-by-step

```bash
# 1. Start PostgreSQL in a Docker container
docker compose up -d

# 2. Generate jOOQ classes from the live schema
./mvnw clean generate-sources

# 3. Run the Spring Boot application
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

### Stopping

```bash
docker compose down       # stop the container, keep data
docker compose down -v    # stop the container AND wipe the volume (fresh start)
```

> ⚠️ If you wipe the volume with `-v`, the schema will be recreated next time the container starts (from `create_table.sql`), but all data will be lost. You also need to re-run `./mvnw clean generate-sources`.

### Optional: Connect with a DB client (DBeaver, psql, etc.)

```
Host:     localhost
Port:     5432
Database: jooq_study
User:     dev
Password: dev123
```

---

## 11. API Reference

### Users

#### Create a user — `POST /users`

```bash
curl --request POST \
  --url http://localhost:8080/users \
  --header 'content-type: application/json' \
  --data '{
  "userName": "Renan",
  "email": "renan@email.com",
  "password": "def456"
}'
```

**Response 201:**
```json
{ "id": 1, "userName": "Renan", "email": "renan@email.com" }
```

#### Get a user — `GET /users/{userName}`

```bash
curl http://localhost:8080/users/Renan
```

**Response 200:** same as above  
**Response 404:** `User not found: Renan`

#### Delete a user — `DELETE /users/{userName}`

```bash
curl --request DELETE http://localhost:8080/users/Renan
```

**Response 200:** returns the deleted user  
**Response 404:** `User not found: Renan`

---

### Access Logs

#### Create an access log — `POST /access`

```bash
curl --request POST \
  --url http://localhost:8080/access \
  --header 'content-type: application/json' \
  --data '{ "userName": "Renan" }'
```

**Response 200:**
```json
{ "userId": 1, "userName": "Renan", "accessTime": "2026-06-16T14:30:00Z" }
```

#### Get access logs — `GET /access/{userName}`

```bash
curl http://localhost:8080/access/Renan
```

**Response 200:** list of access logs, ordered by most recent first  
**Response 404:** `No access logs found for user: Renan`

---

## 12. Common Pitfalls

### "Table not found" during code generation

**Cause:** The database is not running when you run `./mvnw clean generate-sources`.  
**Fix:** Always run `docker compose up -d` first.

---

### Generated classes are missing / IDE shows errors

**Cause:** You haven't run the code generator yet, or it's out of date.  
**Fix:** Run `./mvnw clean generate-sources` and then refresh your IDE's project structure.

---

### `RETURNING` clause doesn't work

**Cause:** `RETURNING` is PostgreSQL-specific. If `spring.jooq.sql-dialect` is missing or wrong, jOOQ won't generate it.  
**Fix:** Make sure `spring.jooq.sql-dialect=POSTGRES` is in `application.properties`.

---

### `store()` returns 0

**Cause:** The INSERT or UPDATE did not affect any rows (e.g., a constraint violation was silently swallowed by a misconfigured exception handler).  
**Fix:** Always check the return value of `store()`:
```java
int stored = usersRecord.store();
if (stored != 1) {
    throw new RuntimeException("Failed to create user");
}
```

---

### Volume already exists — init script didn't run

**Cause:** `create_table.sql` only runs once, on the first container start with an empty volume.  
**Fix:** `docker compose down -v` to wipe the volume, then `docker compose up -d`.

---

## 13. Quick Reference Cheatsheet

```java
// --- Inject DSLContext ---
private final DSLContext ctx;

// --- SELECT all from a table ---
ctx.selectFrom(Users.USERS).fetch();

// --- SELECT with a WHERE clause ---
ctx.selectFrom(Users.USERS)
   .where(Users.USERS.USERNAME.eq("Renan"))
   .fetchOne(); // null if not found

// --- SELECT specific columns ---
ctx.select(Users.USERS.USERNAME, Users.USERS.EMAIL)
   .from(Users.USERS)
   .fetch();

// --- INSERT via newRecord + store() ---
var record = ctx.newRecord(Users.USERS);
record.setUsername("Renan");
record.setEmail("renan@email.com");
record.setPassword("secret");
record.store(); // executes INSERT

// --- DELETE with RETURNING (PostgreSQL) ---
ctx.deleteFrom(Users.USERS)
   .where(Users.USERS.USERNAME.eq("Renan"))
   .returning()
   .fetchOne();

// --- JOIN ---
ctx.select(AccessLog.ACCESS_LOG.LOGGED_IN_AT, Users.USERS.USERNAME)
   .from(AccessLog.ACCESS_LOG)
   .join(Users.USERS)
       .on(AccessLog.ACCESS_LOG.USER_ID.eq(Users.USERS.ID))
   .where(Users.USERS.USERNAME.eq("Renan"))
   .orderBy(AccessLog.ACCESS_LOG.LOGGED_IN_AT.desc())
   .fetch();

// --- Common fetch methods ---
.fetchOne()          // one record or null (throws if > 1 result)
.fetchAny()          // first record or null
.fetch()             // Result<Record> — a list
.fetch(r -> dto)     // list, each row mapped via lambda
.fetchInto(MyClass.class) // list, auto-mapped to a POJO
```

---

> **Official documentation:** https://www.jooq.org/doc/latest/manual/  
> **Project source:** https://github.com/renanmuniz/java-jooq

