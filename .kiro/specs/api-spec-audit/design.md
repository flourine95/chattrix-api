# API Spec Audit & Fix - Design Document

## Overview

Dự án cần audit và fix file `api-spec.yaml` để đảm bảo nó phản ánh chính xác implementation thực tế. File hiện tại có >6000 dòng với nhiều sai lệch về error responses, schemas, và missing endpoints.

## Architecture

### Audit Strategy

Sử dụng phương pháp **systematic section-by-section audit**:

1. **Schema Audit** - Verify tất cả schemas trong `components/schemas`
2. **Endpoint Audit** - Verify tất cả paths trong `paths`
3. **Example Audit** - Fix tất cả examples để match với actual responses
4. **Documentation Audit** - Ensure descriptions are accurate

### Source of Truth

- **Java Resource classes** (`src/main/java/com/chattrix/api/resources/**`) - Endpoint paths, methods, parameters
- **Request DTOs** (`src/main/java/com/chattrix/api/requests/**`) - Request body schemas
- **Response DTOs** (`src/main/java/com/chattrix/api/responses/**`) - Response body schemas
- **Exception Mappers** (`src/main/java/com/chattrix/api/exceptions/**`) - Error response structure
- **Jackson Config** (`src/main/java/com/chattrix/api/config/JacksonConfig.java`) - Serialization behavior

## Components and Interfaces

### 1. Error Response Structure

**Current (Wrong):**
```yaml
ErrorResponse:
  properties:
    success: boolean
    error:
      code: string
      message: string
      details: object
    requestId: string
```

**Correct:**
```yaml
ErrorResponse:
  properties:
    success: boolean (always false)
    message: string
    code: string
    details: object (nullable, only for validation errors)
    requestId: string
    data: null (always null for errors)
```

### 2. Success Response Structure

**Pattern:**
```yaml
ApiResponse_<Type>:
  properties:
    success: boolean (always true)
    message: string
    data: <Type> or array of <Type>
```

**Note:** `code`, `details`, `requestId` fields are NOT present in success responses.

### 3. Jackson NON_NULL Behavior

**Configuration:** `JsonInclude.Include.NON_NULL`

**Impact:**
- Nullable fields that are null will NOT appear in JSON response
- Spec should mark fields as `nullable: true` but document they won't appear if null
- Examples should NOT include null fields

## Data Models

### Key DTOs to Verify

1. **UserResponse** - User profile data
2. **MessageResponse** - Message with all metadata
3. **ConversationResponse** - Conversation with participants
4. **ContactResponse** - Contact information
5. **CallResponse** - Call information
6. **FriendRequestResponse** - Friend request data
7. **UserNoteResponse** - User note/status

### Schema Verification Checklist

For each schema:
- [ ] All fields from DTO are present
- [ ] Field types match (String, Long, Instant, etc.)
- [ ] Nullable fields marked with `nullable: true`
- [ ] Validation constraints documented
- [ ] Enums match Java enums
- [ ] Nested objects properly referenced

## Error Handling

### Error Response Patterns

**1. Validation Error (400):**
```json
{
  "success": false,
  "message": "Validation failed",
  "code": "VALIDATION_ERROR",
  "details": {
    "email": "Email already exists",
    "username": "Username must be 4-20 characters"
  },
  "requestId": "uuid"
}
```

**2. Business Error (400/404/403):**
```json
{
  "success": false,
  "message": "User not found",
  "code": "RESOURCE_NOT_FOUND",
  "requestId": "uuid"
}
```

**3. Authentication Error (401):**
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "code": "UNAUTHORIZED",
  "requestId": "uuid"
}
```

### Common Error Codes

- `VALIDATION_ERROR` - Bean validation failed
- `UNAUTHORIZED` - Authentication failed
- `FORBIDDEN` - Authorization failed
- `RESOURCE_NOT_FOUND` - Entity not found
- `BAD_REQUEST` - Business rule violation
- `CONFLICT` - Resource already exists
- `RATE_LIMIT_EXCEEDED` - Too many requests

## Testing Strategy

### Verification Approach

**Phase 1: Automated Checks**
- Grep search for nested `error` objects
- Grep search for `nullable` without documentation
- Count endpoints in spec vs Resource classes

**Phase 2: Manual Review**
- Compare each Resource class with spec paths
- Compare each DTO with spec schema
- Verify examples match actual responses

**Phase 3: Validation**
- Use OpenAPI validator tools
- Test with actual API calls
- Generate client code to verify usability

## Implementation Plan

### Section-by-Section Approach

**1. Schemas Section (components/schemas)**
- Fix ErrorResponse ✅ (Done)
- Fix all ApiResponse_* wrappers
- Verify all Request schemas
- Verify all Response schemas
- Add missing schemas

**2. Paths Section - Authentication (/v1/auth)**
- Fix error response examples
- Verify request/response schemas
- Check status codes
- Verify rate limiting documentation

**3. Paths Section - Users (/v1/users, /v1/profile)**
- Fix error response examples
- Verify schemas
- Add missing endpoints

**4. Paths Section - Conversations (/v1/conversations)**
- Fix error response examples
- Add missing endpoints (search, media)
- Verify schemas

**5. Paths Section - Messages (/v1/conversations/{id}/messages)**
- Fix error response examples
- Add missing endpoints ✅ (Done: history, forward, pin, search, media)
- Verify schemas

**6. Paths Section - Contacts & Social (/v1/contacts, /v1/friend-requests)**
- Fix error response examples
- Verify schemas
- Check all endpoints present

**7. Paths Section - Calls (/v1/calls)**
- Fix error response examples
- Verify schemas
- Check all endpoints present

**8. Paths Section - Notes (/v1/notes)**
- Fix error response examples
- Verify schemas
- Check all endpoints present

**9. Final Review**
- Run OpenAPI validator
- Check for consistency
- Verify all examples are realistic

## Correctness Properties

### Property 1: Error Response Consistency
*For any* error response example in the spec, it should use the flat structure with `success`, `message`, `code`, `requestId` fields and NOT have nested `error` object.
**Validates: Requirements 1.1, 1.3**

### Property 2: Schema Completeness
*For any* DTO class in the codebase, all its fields should be present in the corresponding schema in the spec.
**Validates: Requirements 2.1, 2.4**

### Property 3: Endpoint Completeness
*For any* public method in Resource classes, there should be a corresponding path in the spec with matching HTTP method and path.
**Validates: Requirements 3.1, 3.2, 4.1**

### Property 4: Nullable Field Documentation
*For any* field marked `nullable: true` in a schema, the description should mention that the field is omitted from response if null.
**Validates: Requirements 2.2, 2.5**

### Property 5: Status Code Accuracy
*For any* endpoint that creates a resource, the success response should have status code 201, not 200.
**Validates: Requirements 5.1**

## Common Patterns

### Pattern 1: Adding Missing Endpoint

1. Find Resource class method
2. Extract path from `@Path` annotation
3. Extract HTTP method from `@GET/@POST/@PUT/@DELETE`
4. Extract parameters from method signature
5. Extract request body from method parameter
6. Extract response from return type
7. Add to spec with proper structure

### Pattern 2: Fixing Error Response Example

**Before:**
```yaml
example:
  success: false
  error:
    code: "UNAUTHORIZED"
    message: "Invalid token"
  requestId: "req-123"
```

**After:**
```yaml
example:
  success: false
  message: "Invalid token"
  code: "UNAUTHORIZED"
  requestId: "req-123"
```

### Pattern 3: Documenting Nullable Field

```yaml
avatarUrl:
  type: string
  format: uri
  nullable: true
  description: "User avatar URL. Omitted from response if null (NON_NULL serialization)."
```

## Notes

- File size: >6000 lines - need systematic approach
- Many error examples still use old nested structure
- Some endpoints missing entirely
- Jackson NON_NULL config affects all responses
- All timestamps use `Instant` type (ISO-8601 format)
