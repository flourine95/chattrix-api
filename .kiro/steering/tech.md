---
inclusion: always
---

# Technology Stack & Architecture Standards

## Core Stack

- **Java 17** - Language version
- **Jakarta EE 10** - Enterprise platform (Web API)
- **WildFly 38** - Application server
- **Hibernate 6.6** - ORM and JPA implementation
- **Maven** - Build tool and dependency management

## Key Libraries

| Library | Purpose | Version |
|---------|---------|---------|
| **Lombok** | Reduce boilerplate (getters, setters, constructors) | 1.18.42 |
| **MapStruct** | Type-safe bean mapping between entities and DTOs | 1.5.5 |
| **Jackson** | JSON serialization/deserialization | 2.20.0 |
| **JJWT** | JWT token generation and validation | 0.12.6 |
| **jBCrypt** | Password hashing | 0.4 |
| **Hibernate Validator** | Bean validation (JSR 380) | 8.0.3 |
| **Jakarta Mail** | Email sending | 2.0.1 |

## Architecture Pattern

The project follows a **layered architecture** with clear separation of concerns:

```
resources/ (REST endpoints)
    ↓
services/ (business logic)
    ↓
repositories/ (data access)
    ↓
entities/ (JPA entities)
```

### Package Structure Rules

- **resources/** - JAX-RS REST endpoints. Use `@Path`, `@GET`, `@POST`, etc. Return Response objects.
- **services/** - Business logic layer. Inject repositories via CDI `@Inject`. Use `@Transactional` for database operations.
- **repositories/** - Data access layer. Extend or implement repository patterns. Use `EntityManager` for queries.
- **entities/** - JPA entities with `@Entity`, `@Table`, relationships. Use Lombok annotations.
- **requests/** - DTOs for incoming API requests. Use validation annotations (`@NotNull`, `@Email`, etc.).
- **responses/** - DTOs for API responses. Use Lombok and Jackson annotations.
- **mappers/** - MapStruct interfaces for entity ↔ DTO conversion. Annotate with `@Mapper(componentModel = "cdi")`.
- **exceptions/** - Custom exceptions and JAX-RS exception mappers.
- **filters/** - JAX-RS filters for cross-cutting concerns (auth, CORS, rate limiting).
- **websocket/** - WebSocket endpoints and related classes.
- **config/** - Application configuration classes.
- **validations/** - Custom validation annotations and validators.

## Coding Standards

### Dependency Injection
- Use CDI `@Inject` for dependency injection, NOT constructor injection unless necessary
- Mark services as `@ApplicationScoped` or `@RequestScoped` as appropriate
- Use `@Produces` for custom bean producers

### Entity Design
- ALL entities MUST use Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Use `@Builder` for complex entity construction
- Define relationships with proper fetch types (`LAZY` by default)
- Use `@JsonIgnore` on bidirectional relationships to prevent serialization loops

### DTO Mapping
- ALL mappers MUST be MapStruct interfaces with `@Mapper(componentModel = "cdi")`
- Define explicit mappings for complex transformations using `@Mapping`
- Inject mappers via `@Inject` in services
- NEVER manually map between entities and DTOs

### Transaction Management
- Use `@Transactional` on service methods that modify data
- Keep transactions as short as possible
- Handle exceptions properly to ensure rollback

### Error Handling
- Throw custom exceptions (`BusinessException`, `ResourceNotFoundException`, `UnauthorizedException`, `BadRequestException`)
- Use JAX-RS exception mappers to convert exceptions to proper HTTP responses
- Return structured error responses using `ErrorDetail` or `ApiResponse`

### Validation
- Use Bean Validation annotations on request DTOs (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, etc.)
- Create custom validators for complex validation logic (see `validations/` package)
- Validation errors are automatically handled by `ConstraintViolationExceptionMapper`

### REST API Design
- Use proper HTTP methods: GET (read), POST (create), PUT (update), DELETE (remove)
- Return appropriate status codes: 200 (OK), 201 (Created), 204 (No Content), 400 (Bad Request), 401 (Unauthorized), 404 (Not Found)
- Use `@Secured` annotation to protect endpoints requiring authentication
- Use `@RateLimited` annotation for rate-limiting sensitive endpoints
- Return `Response` objects with proper status and entity

### WebSocket
- WebSocket endpoints use Jakarta WebSocket API (`@ServerEndpoint`)
- Use custom encoders/decoders for message serialization
- Inject CDI beans using `CdiAwareConfigurator`
- Manage sessions in `ChatSessionService`

### Security
- JWT tokens for authentication (access + refresh token pattern)
- Use `@Secured` filter to validate JWT and inject `UserPrincipal`
- Hash passwords with jBCrypt before storing
- Invalidate tokens on logout
- Store refresh tokens in database with expiration

### Database
- Use JPA/Hibernate for all database operations
- Write JPQL or Criteria API queries in repositories
- Use named queries for complex, reusable queries
- Handle optimistic locking with `@Version` where needed

## Build & Deployment

- **Build**: `mvn clean package` produces `ROOT.war`
- **Deploy**: Use `wildfly-maven-plugin` with `mvn wildfly:deploy`
- **Annotation Processing**: Lombok and MapStruct run during compilation
- **Order matters**: Lombok must process before MapStruct (configured in `pom.xml`)

## Code Generation

When Lombok or MapStruct annotations are added/modified:
1. Run `mvn clean compile` to regenerate classes
2. Generated sources appear in `target/generated-sources/annotations/`
3. IDEs may need annotation processing enabled in settings