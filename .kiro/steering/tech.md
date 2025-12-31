---
inclusion: always
---

# Technology Stack & Architecture Standards

## Core Stack

- **Java 17** with Jakarta EE 10 (JAX-RS, CDI, JPA, Bean Validation, WebSocket)
- **WildFly 38** application server
- **Hibernate 6.6** (JPA provider)
- **PostgreSQL 16** database
- **Maven** build system

## Key Libraries

- **Lombok 1.18.42** - Boilerplate elimination (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`)
- **MapStruct 1.5.5** - Type-safe entity ↔ DTO mapping (REQUIRED - never map manually)
- **Jackson 2.20.0** - JSON serialization
- **JJWT 0.12.6** - JWT authentication
- **jBCrypt 0.4** - Password hashing
- **Hibernate Validator 8.0.3** - Bean validation
- **SLF4J** - Logging framework (provided by WildFly)

## Architecture Rules

**Strict Layered Architecture** - NEVER skip layers:
```
Resources → Services → Repositories → Entities
```

- Resources handle HTTP, inject services
- Services contain business logic, inject repositories and mappers
- Repositories handle data access, inject EntityManager
- Entities are JPA models

## Package Structure

### `resources/` - REST API Layer
- `@Path` endpoints with `@GET/@POST/@PUT/@DELETE`
- Return `Response` with status codes
- `@Inject` services
- `@Secured` for auth, `@RateLimited` for rate limiting
- Get current user: `UserContext.getCurrentUserId()`
- Add `@Slf4j` for logging

### `services/` - Business Logic
- `@ApplicationScoped` (stateless) or `@RequestScoped` (stateful)
- `@Inject` repositories, mappers, other services
- `@Transactional` on methods that modify data
- **NEVER return null** - throw exceptions for missing resources
- Use MapStruct mappers for entity ↔ DTO conversion
- Add `@Slf4j` for logging

### `repositories/` - Data Access
- `@PersistenceContext EntityManager`
- JPQL queries: `entityManager.createQuery("SELECT e FROM Entity e WHERE ...", Entity.class)`
- Return `Optional<Entity>` for single results
- NO business logic

### `entities/` - JPA Models
**MANDATORY template:**
```java
@Entity
@Table(name = "table_name")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // other fields
}
```
- Relationships: `fetch = FetchType.LAZY` (default)
- Bidirectional: `@JsonIgnore` on inverse side

### `requests/` - Request DTOs
- Bean Validation: `@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Pattern`
- Lombok: `@Getter`, `@Setter`

### `responses/` - Response DTOs
- **NEVER expose entities directly**
- Lombok + Jackson annotations

### `mappers/` - MapStruct Interfaces
**MANDATORY for entity ↔ DTO conversion:**
```java
@Mapper(componentModel = "cdi")
public interface EntityMapper {
    EntityResponse toResponse(Entity entity);
    Entity toEntity(EntityRequest request);
}
```
- `@Inject` in services
- `@Mapping(source = "field", target = "otherField")` for custom mappings

### `exceptions/` - Error Handling
- Custom exceptions extend `RuntimeException`
- `@Provider` exception mappers implement `ExceptionMapper<T>`
- Standard exceptions: `BusinessException`, `ResourceNotFoundException`, `UnauthorizedException`, `BadRequestException`

### `filters/` - JAX-RS Filters
- `@Provider` on `ContainerRequestFilter` or `ContainerResponseFilter`
- Used for: authentication, CORS, rate limiting

### `websocket/` - WebSocket
- `@ServerEndpoint(configurator = CdiAwareConfigurator.class)`
- Custom encoders/decoders for JSON
- Session management via `ChatSessionService`

### `config/` - Configuration
- CDI beans with `@Produces` for custom bean factories

### `validations/` - Custom Validators
- Custom constraint annotations + `ConstraintValidator` implementations

## Critical Patterns

### Dependency Injection
- **Field injection only**: `@Inject` (NOT constructor injection)
- Services: `@ApplicationScoped` or `@RequestScoped`
- Repositories: `@ApplicationScoped`

### Transaction Management
- `@Transactional` on service methods that modify data
- Keep transactions short
- Exceptions trigger automatic rollback

### Error Handling
**NEVER return null for missing resources.** Always throw:
- `BusinessException` - business rule violations
- `ResourceNotFoundException` - 404
- `UnauthorizedException` - 401
- `BadRequestException` - 400

### REST Conventions
**Status codes:**
- `200 OK` - successful GET/PUT
- `201 Created` - successful POST
- `204 No Content` - successful DELETE
- `400 Bad Request` - validation error
- `401 Unauthorized` - auth required
- `404 Not Found` - resource missing

**Response pattern:**
```java
return Response.status(Status.OK).entity(responseDto).build();
```

### Security
- JWT authentication (access + refresh tokens)
- `@Secured` on protected endpoints
- `@RateLimited` on rate-limited endpoints
- jBCrypt for password hashing
- Current user: `UserContext.getCurrentUserId()`

### JPA Relationships
- Default: `fetch = FetchType.LAZY`
- Bidirectional: specify `mappedBy` on inverse side
- Bidirectional: add `@JsonIgnore` on inverse side
- `@JoinColumn` for FK column names

### Logging with SLF4J
**MANDATORY logging pattern with Lombok:**
```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceName {
    public void method() {
        log.debug("Debug message with param: {}", param);
        log.info("Info message");
        log.warn("Warning message");
        log.error("Error message", exception);
    }
}
```

**Logging levels:**
- `DEBUG` - Detailed flow information (method entry/exit, parameters)
- `INFO` - Important business events (user registered, order created)
- `WARN` - Recoverable issues (validation failed, retry attempt)
- `ERROR` - Exceptions and critical errors

**Best practices:**
- Use `@Slf4j` annotation from Lombok - no need to declare logger manually
- Use parameterized logging: `log.info("User {} logged in", username)` (NOT string concatenation)
- Log at method entry: `log.debug("Method called with params: {}", params)`
- Log business events: `log.info("User registered: {}", userId)`
- Log exceptions: `log.error("Failed to process", exception)`
- Never log sensitive data (passwords, tokens)
- Use appropriate log levels

## Docker Development

**All development uses Docker.** Do not run commands outside containers.

### Container Services
- `postgres` - PostgreSQL 16 (port 5432)
- `api` - WildFly 38 (ports 8080, 9990)

### Essential Commands
```bash
# Start
docker compose up -d

# Rebuild after code changes
docker compose up -d --build

# View logs
docker compose logs -f api

# Stop
docker compose down

# Reset (including DB)
docker compose down -v && docker compose up -d --build

# Shell access
docker compose exec api bash
docker compose exec postgres psql -U postgres -d chattrix
```

### Access Points
- API: http://localhost:8080
- WildFly Admin: http://localhost:9990 (admin/admin123)
- Database: localhost:5432 (postgres/postgres123)

### Environment Setup
- Copy `.env.example` to `.env`
- Required: `JWT_SECRET`, `AGORA_APP_ID`, `AGORA_APP_CERTIFICATE`

### Build & Deploy
**When asked to build or deploy:** Run `build-and-deploy.ps1`

Application auto-deploys `ROOT.war` to WildFly on container startup.

### Annotation Processing
Lombok generates code before MapStruct. After modifying Lombok/MapStruct annotations:
```bash
mvn clean compile
```
Generated sources: `target/generated-sources/annotations/`

## Common Workflows

### Creating a New Feature (Full Stack)
1. **Entity**: Create in `entities/` with Lombok annotations
2. **Repository**: Create in `repositories/` with `@PersistenceContext EntityManager`
3. **DTOs**: Create request/response in `requests/`/`responses/`
4. **Mapper**: Create MapStruct interface in `mappers/` with `@Mapper(componentModel = "cdi")`
5. **Service**: Create in `services/` with `@ApplicationScoped` and `@Transactional`
6. **Resource**: Create REST endpoint in `resources/` with `@Path` and `@Secured`

### Adding a REST Endpoint
1. Add method to resource with `@GET/@POST/@PUT/@DELETE`
2. Add `@Path` if needed
3. Add `@Secured` if auth required
4. Validate input with request DTO + Bean Validation
5. Call service method
6. Return `Response.status(...).entity(responseDto).build()`

### Modifying Existing Code
- **Adding fields to entity**: Update entity, mapper, DTOs, then run `mvn clean compile`
- **Changing business logic**: Modify service methods (keep in service layer)
- **Adding validation**: Add annotations to request DTOs
- **New query**: Add method to repository with JPQL

### Testing Changes
1. Rebuild: `docker compose up -d --build`
2. Check logs: `docker compose logs -f api`
3. Test endpoint at http://localhost:8080