# java-jooq
Repos for the study of Java Object-Oriented Querying (JOOQ).
https://www.jooq.org/

Vídeo that I used to learn this: https://www.youtube.com/watch?v=6av6oUZK_zc

## What is jOOQ?
jOOQ is a popular Java library that provides a fluent API for building SQL queries in a type-safe manner. 
It allows developers to write SQL queries using Java code, which can help improve readability and reduce the likelihood 
of syntax errors. 
jOOQ generates Java classes based on the database schema, allowing developers to interact with the database using Java 
objects instead of raw SQL strings. This can lead to more maintainable and less error-prone code when working with 
databases in Java applications.

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
    │   │   │
    │   │   │   ── Domain Layer ──
    │   │   ├── UserDTO.java                  # Immutable record: userName, email, password
    │   │   ├── UserRepository.java           # Repository interface: findByUserName, create
    │   │   │
    │   │   │   ── Infrastructure Layer ──
    │   │   ├── UserRepositoryImpl.java       # jOOQ-backed repository implementation (DSLContext)
    │   │   └── UserMapper.java               # Maps UsersRecord → UserDTO
    │   │
    │   └── resources/
    │       ├── application.properties        # DataSource and jOOQ configuration
    │       └── schema.sql                    # DDL for the `users` table (used as reference)
    │
    └── test/
        └── java/com/jooq/renanmuniz/java_jooq/
            └── JavaJooqApplicationTests.java # Spring Boot context smoke test
```

### Key concepts

| Layer | Class / File | Responsibility |
|---|---|---|
| Controller | `UserController` | Handles HTTP requests; maps exceptions to HTTP status codes |
| DTO | `UserDTO` | Immutable response record: userName, email (password excluded) |
| Request | `CreateUserRequest` | Immutable input record for user creation: userName, email, password |
| Repository interface | `UserRepository` | Defines the data-access contract, decoupled from jOOQ |
| Repository impl | `UserRepositoryImpl` | Uses jOOQ `DSLContext` to run type-safe SQL against PostgreSQL |
| Mapper | `UserMapper` | Converts jOOQ-generated `UsersRecord` into `UserDTO` |
| Generated sources | `target/generated-sources/jooq/` | Type-safe table/record classes auto-generated from the live DB schema via `jooq-codegen-maven` |
| Infrastructure | `docker-compose.yml` | Spins up the PostgreSQL instance required for local development and code generation |

> **Note:** The jOOQ generated classes (`DefaultCatalog`, `Public`, `Tables`, `Users`, `UsersRecord`, etc.) live under
> `target/generated-sources/jooq/` and are **not** committed to source control — they are always regenerated from the
> database by running `./mvnw clean generate-sources`.

## Key Configuration

### 1. Maven Dependencies (`pom.xml`)

Two dependencies are needed:

| Dependency | Artifact | Purpose |
|---|---|---|
| jOOQ + Spring integration | `spring-boot-starter-jooq` | Auto-configures a `DSLContext` bean backed by the datasource |
| PostgreSQL JDBC driver | `postgresql` (runtime) | JDBC driver used both at runtime and by the code generator |

```xml
<!-- jOOQ auto-configuration (DSLContext, SQLDialect) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jooq</artifactId>
</dependency>

<!-- PostgreSQL JDBC driver (runtime scope — not needed at compile time) -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

### 2. jOOQ Code Generator plugin (`pom.xml`)

The `jooq-codegen-maven` plugin **connects to the live database** and generates type-safe Java classes for every table. 
It runs automatically during the `generate-sources` Maven lifecycle phase.

```xml
<plugin>
    <groupId>org.jooq</groupId>
    <artifactId>jooq-codegen-maven</artifactId>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- JDBC connection used only at code-generation time -->
        <jdbc>
            <driver>org.postgresql.Driver</driver>
            <url>jdbc:postgresql://localhost:5432/jooq_study</url>
            <user>dev</user>
            <password>dev123</password>
        </jdbc>
        <generator>
            <!-- Target package for generated classes -->
            <target>
                <packageName>com.jooq.renanmuniz.java_jooq</packageName>
            </target>
            <!-- Source database dialect + schema exclusions -->
            <database>
                <name>org.jooq.meta.postgres.PostgresDatabase</name>
                <excludes>pg_catalog.* | information_schema.*</excludes>
            </database>
        </generator>
    </configuration>
</plugin>
```

> **Important:** The database must be running (`docker compose up -d`) before executing `./mvnw clean generate-sources`, 
because the plugin makes a live JDBC connection to introspect the schema.

---

### 3. Application properties (`application.properties`)

```properties
# DataSource — tells Spring (and jOOQ) how to connect at runtime
spring.datasource.url=jdbc:postgresql://localhost:5432/jooq_study
spring.datasource.username=dev
spring.datasource.password=dev123
spring.datasource.driver-class-name=org.postgresql.Driver

# Tells the auto-configured DSLContext which SQL dialect to use
spring.jooq.sql-dialect=POSTGRES

# Expose error messages in HTTP responses (useful for development)
server.error.include-message=always
```

| Property | Why it matters |
|---|---|
| `spring.datasource.*` | Standard Spring Boot datasource; `spring-boot-starter-jooq` wraps it into a `DSLContext` bean automatically |
| `spring.jooq.sql-dialect=POSTGRES` | Instructs jOOQ to generate PostgreSQL-specific SQL (e.g. `RETURNING`, `ON CONFLICT`) |
| `server.error.include-message=always` | Surfaces exception messages (like `User not found`) in the JSON error response body |

---

### How the pieces connect at runtime

```
application.properties
        │  (DataSource config)
        ▼
Spring Boot Auto-Configuration
        │  creates
        ▼
   DSLContext bean  ◄──── spring.jooq.sql-dialect=POSTGRES
        │  injected into
        ▼
UserRepositoryImpl
        │  runs type-safe SQL using
        ▼
 Generated classes (Users, UsersRecord, …)
        │  mapped back to
        ▼
      UserDTO  ──►  HTTP response (JSON)
```

## How to run & connect:
1. **Start the database:**
`docker compose up -d`

2. **Generate jOOQ classes:**
RUN: `./mvnw clean generate-sources`.
The `generate-sources` goal will run the jOOQ code generator, which will connect to the database and generate Java
classes based on the database schema. These classes will be placed in the `target/generated-sources` directory and can 
be used in your application to interact with the database in a type-safe manner.

3. **Run the Spring Boot app:**
`./mvnw spring-boot:run`


***Optional:*** 
External DB connection details (for DBeaver, psql, etc.):
```
Host:     localhost
Port:     5432
Database: jooq_study
User:     dev
Password: dev123
```

--- 

**To Stop the DB:**
- `docker compose down`        keep data
- `docker compose down -v`     wipe data

---

## API Usage

### Create a user
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

**Response (201):**
```json
{
  "userName": "Renan",
  "email": "renan@email.com"
}
```

---

### Get a user by username
```bash
curl --request GET \
  --url http://localhost:8080/users/Renan
```

**Response (200):**
```json
{
  "userName": "Renan",
  "email": "renan@email.com"
}
```

**Response (404):** `User not found: Renan`

---

### Delete a user
```bash
curl --request DELETE \
  --url http://localhost:8080/users/Renan
```

**Response (200):**
```json
{
  "userName": "Renan",
  "email": "renan@email.com"
}
```

**Response (404):** `User not found: Renan`

---

