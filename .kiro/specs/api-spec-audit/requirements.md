# API Spec Audit & Fix - Requirements

## Introduction

File `api-spec.yaml` hiện tại có nhiều sai lệch so với implementation thực tế trong code. Cần audit toàn bộ và sửa lại cho đúng.

## Glossary

- **API Spec**: OpenAPI 3.0 specification file
- **Implementation**: Java code thực tế trong dự án
- **DTO**: Data Transfer Object (Request/Response classes)
- **Endpoint**: REST API endpoint
- **Schema**: OpenAPI schema definition

## Requirements

### Requirement 1: Fix Error Response Structure

**User Story:** As an API consumer, I want error responses to match the actual implementation, so that I can handle errors correctly.

#### Acceptance Criteria

1. WHEN the system returns an error THEN the response SHALL use `ApiResponse` structure with fields: `success`, `message`, `code`, `details`, `requestId`
2. WHEN validation fails THEN the response SHALL include `details` map with field-level errors
3. WHEN business error occurs THEN the response SHALL include `code` and `message` without nested `error` object
4. THE system SHALL NOT use nested `error` object structure as currently defined in spec

### Requirement 2: Fix Request/Response Schemas

**User Story:** As an API consumer, I want request and response schemas to match actual DTOs, so that integration works correctly.

#### Acceptance Criteria

1. WHEN defining schemas THEN they SHALL match Java DTO classes exactly
2. WHEN a field is nullable in code THEN it SHALL be marked nullable in spec BUT the field SHALL be omitted from response if null (due to `JsonInclude.Include.NON_NULL`)
3. WHEN a field has validation THEN spec SHALL document the validation rules
4. THE spec SHALL include all fields present in actual DTOs
5. THE spec SHALL document that null fields are NOT included in JSON responses (Jackson NON_NULL config)

### Requirement 3: Fix Endpoint Paths and Methods

**User Story:** As an API consumer, I want endpoint paths to match actual routes, so that API calls work correctly.

#### Acceptance Criteria

1. WHEN documenting endpoints THEN paths SHALL match `@Path` annotations in Resource classes
2. WHEN documenting HTTP methods THEN they SHALL match `@GET`, `@POST`, `@PUT`, `@DELETE` annotations
3. WHEN endpoint requires authentication THEN spec SHALL show `@Secured` requirement
4. WHEN endpoint has rate limiting THEN spec SHALL document `@RateLimited` settings

### Requirement 4: Fix Missing Endpoints

**User Story:** As an API consumer, I want all implemented endpoints documented, so that I know the complete API surface.

#### Acceptance Criteria

1. THE spec SHALL include all endpoints from Resource classes
2. THE spec SHALL document message edit history endpoint
3. THE spec SHALL document message forward endpoint
4. THE spec SHALL document pinned messages endpoints
5. THE spec SHALL document conversation search and media endpoints

### Requirement 5: Verify Response Status Codes

**User Story:** As an API consumer, I want correct HTTP status codes documented, so that I can handle responses properly.

#### Acceptance Criteria

1. WHEN endpoint creates resource THEN spec SHALL show 201 Created
2. WHEN endpoint returns data THEN spec SHALL show 200 OK
3. WHEN endpoint deletes resource THEN spec SHALL show 200 OK with success message
4. WHEN validation fails THEN spec SHALL show 400 Bad Request
5. WHEN authentication fails THEN spec SHALL show 401 Unauthorized
6. WHEN authorization fails THEN spec SHALL show 403 Forbidden
7. WHEN resource not found THEN spec SHALL show 404 Not Found
