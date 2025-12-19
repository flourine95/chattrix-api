---
inclusion: always
---

# Technology Stack & Architecture Standards

## Core Stack

- Java 17
- Jakarta EE 10 (JAX-RS, CDI, JPA, Bean Validation, WebSocket)
- WildFly 38 application server
- Hibernate 6.6 (JPA provider)
- Maven build system

## Key Libraries

- **Lombok 1.18.42** - Eliminates boilerplate code (`@Getter`, `@Setter`, `@Builder`, etc.)
- **MapStruct 1.5.5** - Type-safe entity ↔ DTO mapping
- **Jackson 2.20.0** - JSON serialization
- **JJWT 0.12.6** - JWT token handling
- **jBCrypt 0.4** - Password hashing
- **Hibernate Validator 8.0.3** - Bean validation (JSR 380)
- **Jakarta Mail 2.0.1** - Email functionality

## Layered Architecture

This project uses strict layered architecture. Each layer has a specific responsibility:

```
REST Resources → Services → Repositories → Entities
```

**Critical Rule**: Never skip layers. Resources call services, services call repositories, repositories interact with entities.

## Package Structure & Responsibilities

### `resources/` - REST API Layer
- JAX-RS endpoints annotated with `@Path`
- HTTP method annotations: `@GET`, `@POST`, `@PUT`, `@DELETE`
- Return `Response` objects with proper status codes
- Inject services via `@Inject`
- Apply `@Secured` for authenticated endpoints
- Apply `@RateLimited` for rate-limited endpoints

### `services/` - Business Logic Layer
- Annotate with `@ApplicationScoped` or `@RequestScoped`
- Inject repositories and other services via `@Inject`
- Use `@Transactional` on methods that modify data
- Throw business exceptions (never return null for missing resources)
- Inject mappers to convert between entities and DTOs

### `repositories/` - Data Access Layer
- Inject `EntityManager` via `@PersistenceContext`
- Write JPQL or Criteria API queries
- Return entities or Optional<Entity>
- No business logic here

### `entities/` - JPA Entities
- Annotate with `@Entity`, `@Table`
- ALWAYS use Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- Use `@Builder` for complex construction
- Default to `LAZY` fetch for relationships
- Add `@JsonIgnore` on inverse side of bidirectional relationships

### `requests/` - Request DTOs
- POJOs for incoming API data
- Use Bean Validation annotations: `@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Pattern`
- Use Lombok for getters/setters

### `responses/` - Response DTOs
- POJOs for outgoing API data
- Use Lombok and Jackson annotations
- Never expose entities directly in API responses

### `mappers/` - MapStruct Mappers
- Interfaces annotated with `@Mapper(componentModel = "cdi")`
- Define methods like `EntityResponse toResponse(Entity entity)`
- Use `@Mapping` for custom field mappings
- Inject via `@Inject` in services

### `exceptions/` - Exception Handling
- Custom exceptions extend `RuntimeException`
- Exception mappers implement `ExceptionMapper<T>` and annotated with `@Provider`
- Return structured error responses

### `filters/` - JAX-RS Filters
- Implement `ContainerRequestFilter` or `ContainerResponseFilter`
- Annotate with `@Provider`
- Examples: authentication, CORS, rate limiting

### `websocket/` - WebSocket Layer
- Endpoints annotated with `@ServerEndpoint`
- Use `CdiAwareConfigurator` for CDI injection
- Custom encoders/decoders for message serialization

### `config/` - Configuration
- CDI beans for application configuration
- Use `@Produces` for custom bean producers

### `validations/` - Custom Validators
- Custom constraint annotations
- Validator classes implementing `ConstraintValidator`

## Mandatory Coding Patterns

### Dependency Injection
- Use field injection with `@Inject` (NOT constructor injection)
- Services: `@ApplicationScoped` (stateless) or `@RequestScoped` (stateful per request)
- Repositories: typically `@ApplicationScoped`
- Use `@Produces` for factory methods

### Entity Requirements
**Every entity MUST have:**
```java
@Entity
@Table(name = "table_name")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class EntityName {
    // fields
}
```
- Relationships default to `fetch = FetchType.LAZY`
- Add `@JsonIgnore` on inverse side of bidirectional relationships

### DTO Mapping (MapStruct)
**Never manually map entities to DTOs.** Always use MapStruct:
```java
@Mapper(componentModel = "cdi")
public interface EntityMapper {
    EntityResponse toResponse(Entity entity);
    Entity toEntity(EntityRequest request);
}
```
- Inject mappers in services: `@Inject EntityMapper mapper;`
- Use `@Mapping(source = "field", target = "otherField")` for custom mappings

### Transaction Management
- Annotate service methods with `@Transactional` when they modify data
- Keep transactions short - avoid long-running operations inside
- Exceptions automatically trigger rollback

### Error Handling Pattern
**Never return null for missing resources.** Throw exceptions:
- `BusinessException` - business rule violations
- `ResourceNotFoundException` - entity not found (404)
- `UnauthorizedException` - authentication failure (401)
- `BadRequestException` - invalid input (400)

Exception mappers convert these to proper HTTP responses automatically.

### Validation
- Add validation annotations to request DTOs: `@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Min`, `@Max`, `@Pattern`
- `ConstraintViolationExceptionMapper` handles validation errors automatically
- For complex validation, create custom validators in `validations/` package

### REST API Conventions
**HTTP Methods:**
- `GET` - retrieve resources (no body modification)
- `POST` - create new resources
- `PUT` - update existing resources
- `DELETE` - remove resources

**Status Codes:**
- `200 OK` - successful GET/PUT
- `201 Created` - successful POST
- `204 No Content` - successful DELETE
- `400 Bad Request` - validation error
- `401 Unauthorized` - authentication required
- `404 Not Found` - resource doesn't exist

**Response Pattern:**
```java
return Response.status(Status.OK).entity(responseDto).build();
```

### Security
- JWT-based authentication (access token + refresh token)
- Protected endpoints: add `@Secured` annotation
- Rate-limited endpoints: add `@RateLimited` annotation
- Password hashing: use jBCrypt before storing
- Current user access: inject `@Context SecurityContext` in resources, call `UserContext.getCurrentUserId()`

### WebSocket
- Endpoints: `@ServerEndpoint(value = "/path", configurator = CdiAwareConfigurator.class)`
- Use custom encoders/decoders for JSON serialization
- Session management via `ChatSessionService`

### Database Operations
- Use `EntityManager` for all database operations
- Write JPQL queries in repositories: `entityManager.createQuery("SELECT e FROM Entity e WHERE ...", Entity.class)`
- Use `Optional<Entity>` for single results that might not exist
- Named queries for complex, reusable queries

## Docker Development Environment

**This project runs in Docker containers.** All development and testing should be done using Docker.

### Container Architecture
- **postgres** - PostgreSQL 16 database (port 5432)
- **api** - WildFly 38 application server with deployed application (ports 8080, 9990)

### Essential Docker Commands

**Start the application:**
```bash
docker compose up -d
```

**View logs:**
```bash
docker compose logs -f api          # Application logs
docker compose logs -f postgres     # Database logs
```

**Rebuild after code changes:**
```bash
docker compose up -d --build
```

**Stop containers:**
```bash
docker compose down
```

**Reset everything (including database):**
```bash
docker compose down -v
docker compose up -d --build
```

**Execute commands inside container:**
```bash
docker compose exec api bash        # Access API container shell
docker compose exec postgres psql -U postgres -d chattrix  # Access database
```

### Environment Configuration
- Copy `.env.example` to `.env` and configure required variables
- Required: `JWT_SECRET`, `AGORA_APP_ID`, `AGORA_APP_CERTIFICATE`
- Optional: Email settings, Cloudinary for file uploads

### Accessing Services
- **API**: http://localhost:8080
- **WildFly Admin Console**: http://localhost:9990 (admin/admin123)
- **Database**: localhost:5432 (postgres/postgres123)

### Development Workflow
1. Make code changes in `src/`
2. Run `docker compose up -d --build` to rebuild and deploy
3. Check logs with `docker compose logs -f api`
4. Test endpoints at http://localhost:8080

**Important**: The application automatically deploys `ROOT.war` to WildFly on container startup.

## Build & Annotation Processing

**Important**: Lombok generates code before MapStruct runs. This order is configured in `pom.xml`.

**After adding/modifying Lombok or MapStruct annotations:**
```bash
mvn clean compile
```

Generated sources appear in `target/generated-sources/annotations/`.

**Build commands (inside Docker):**
- `mvn clean package` - produces `ROOT.war`
- Application auto-deploys on container restart

## Common Patterns

### Creating a New Entity
1. Create entity class in `entities/` with Lombok annotations
2. Create repository in `repositories/`
3. Create request/response DTOs in `requests/`/`responses/`
4. Create MapStruct mapper in `mappers/`
5. Create service in `services/` with `@Transactional` methods
6. Create REST resource in `resources/`

### Adding a New Endpoint
1. Add method to resource class with `@GET/@POST/@PUT/@DELETE`
2. Define `@Path` if needed
3. Add `@Secured` if authentication required
4. Validate input using request DTO with validation annotations
5. Call service method
6. Return `Response` with appropriate status and DTO

### Handling Relationships
- Use `@ManyToOne`, `@OneToMany`, `@ManyToMany`, `@OneToOne`
- Always specify `mappedBy` on inverse side
- Add `@JsonIgnore` on inverse side to prevent infinite loops
- Use `@JoinColumn` to specify foreign key column name