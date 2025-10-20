# Resource Refactoring Summary

## Mục tiêu
Refactor tất cả Resource classes để tuân theo chuẩn của **AuthResource**:

## Các thay đổi đã áp dụng

### ✅ 1. Loại bỏ try-catch INTERNAL_SERVER_ERROR
**Trước:**
```java
try {
    // business logic
} catch (Exception e) {
    ApiResponse<Void> errorResponse = ApiResponse.error("Error...", "INTERNAL_SERVER_ERROR");
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
}
```

**Sau:**
```java
// business logic trực tiếp - GlobalExceptionMapper sẽ xử lý
```

**Lý do:** Đã có `GlobalExceptionMapper`, `BusinessExceptionMapper` để xử lý exceptions globally.

---

### ✅ 2. Dùng SecurityContext thay vì HttpHeaders
**Trước:**
```java
@GET
public Response getData(@Context HttpHeaders headers) {
    String token = extractTokenFromHeaders(headers);
    if (token == null || !tokenService.validateToken(token)) {
        throw new UnauthorizedException();
    }
    Long userId = tokenService.getUserIdFromToken(token);
    User user = userRepository.findById(userId).orElseThrow();
    // ...
}
```

**Sau:**
```java
@GET
public Response getData(@Context SecurityContext securityContext) {
    User currentUser = getCurrentUser(securityContext);
    // ...
}

private User getCurrentUser(SecurityContext securityContext) {
    UserPrincipal userPrincipal = (UserPrincipal) securityContext.getUserPrincipal();
    return userPrincipal.user();
}
```

**Lý do:** `AuthenticationFilter` đã parse JWT và inject User vào SecurityContext rồi.

---

### ✅ 3. Thêm @Secured annotation
**Trước:**
```java
@Path("/v1/conversations")
public class ConversationResource {
```

**Sau:**
```java
@Path("/v1/conversations")
@Secured
public class ConversationResource {
```

**Lý do:** Để `AuthenticationFilter` tự động xác thực mọi request đến resource này.

---

### ✅ 4. Inline ApiResponse vào Response
**Trước:**
```java
ApiResponse<List<ConversationResponse>> response = ApiResponse.success(conversations, "Success");
return Response.ok(response).build();
```

**Sau:**
```java
return Response.ok(ApiResponse.success(conversations, "Success")).build();
```

**Lý do:** Code gọn gàng hơn, không cần biến trung gian.

---

### ✅ 5. Tách Service Layer
**Trước (Resource chứa business logic):**
```java
@POST
public Response createConversation(@Context HttpHeaders headers, @Valid CreateConversationRequest request) {
    // Authenticate manually
    String token = extractTokenFromHeaders(headers);
    Long userId = tokenService.getUserIdFromToken(token);
    
    // Business logic
    Conversation conversation = new Conversation();
    conversation.setName(request.getName());
    // ... nhiều logic
    conversationRepository.save(conversation);
    
    return Response.ok(...).build();
}
```

**Sau (Resource chỉ gọi Service):**
```java
// ConversationResource.java
@POST
public Response createConversation(@Context SecurityContext securityContext, @Valid CreateConversationRequest request) {
    User currentUser = getCurrentUser(securityContext);
    ConversationResponse conversation = conversationService.createConversation(currentUser.getId(), request);
    return Response.status(Response.Status.CREATED)
            .entity(ApiResponse.success(conversation, "Conversation created successfully"))
            .build();
}

// ConversationService.java
@Transactional
public ConversationResponse createConversation(Long currentUserId, CreateConversationRequest request) {
    // Tất cả business logic ở đây
    // Validation
    // Create entities
    // Save to DB
    return ConversationResponse.fromEntity(conversation);
}
```

---

### ✅ 6. Truyền Request object thay vì từng thuộc tính
**Trước:**
```java
contactService.addContact(userId, contactUserId, nickname);
```

**Sau:**
```java
contactService.addContact(userId, request); // request chứa contactUserId, nickname
```

**Lý do:** 
- Dễ mở rộng (thêm field mới không cần sửa method signature)
- Code sạch hơn
- Request class có validation annotations

---

## Các Resource đã được refactor

| Resource | Service | Status |
|----------|---------|--------|
| ✅ AuthResource | AuthService | Chuẩn mẫu |
| ✅ ContactResource | ContactService | Đã refactor |
| ✅ ConversationResource | ConversationService | Đã refactor |
| ✅ MessageResource | MessageService | Đã refactor |
| ✅ UserStatusResource | UserStatusService | Đã refactor |
| ✅ TypingIndicatorResource | TypingIndicatorService | Đã refactor |

---

## Kiến trúc sau khi refactor

```
┌─────────────────┐
│   HTTP Request  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ AuthenticationFilter │ ◄─── @Secured annotation
│ - Parse JWT      │
│ - Load User      │
│ - Inject vào SecurityContext │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Resource      │
│ - Get User từ SecurityContext │
│ - Validate input │
│ - Call Service   │
│ - Return Response│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│    Service      │
│ - Business Logic │
│ - Transaction    │
│ - Call Repository│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   Repository    │
│ - Database ops   │
└─────────────────┘
```

---

## Exception Handling Flow

```
Exception → GlobalExceptionMapper
            ├─ BusinessException → BusinessExceptionMapper
            │  ├─ BadRequestException (400)
            │  ├─ UnauthorizedException (401)
            │  └─ ResourceNotFoundException (404)
            ├─ ConstraintViolationException → ConstraintViolationExceptionMapper (400)
            ├─ JsonParseException → JsonParseExceptionMapper (400)
            ├─ JsonMappingException → JsonMappingExceptionMapper (400)
            └─ Any other Exception → GlobalExceptionMapper (500)
```

**Vì vậy Resource KHÔNG CẦN try-catch nữa!**

---

## Best Practices đã áp dụng

1. ✅ **Single Responsibility**: Resource chỉ lo HTTP, Service lo business logic
2. ✅ **DRY (Don't Repeat Yourself)**: Authentication logic chỉ ở AuthenticationFilter
3. ✅ **Fail Fast**: Validation ở đầu service method
4. ✅ **Explicit is better than implicit**: Throw exception rõ ràng thay vì return error response
5. ✅ **Consistent API**: Tất cả resources theo cùng một pattern
6. ✅ **Testability**: Service có thể test độc lập không cần HTTP context

---

## Các file mới được tạo

- `ContactService.java` - Business logic cho contacts
- `ConversationService.java` - Business logic cho conversations  
- `MessageService.java` - Business logic cho messages
- `UpdateContactRequest.java` - Request DTO cho update contact

---

## Migration checklist cho Resource mới

Khi tạo Resource mới, hãy làm theo:

- [ ] Thêm `@Secured` annotation ở class level
- [ ] Inject Service, KHÔNG inject Repository trực tiếp
- [ ] Dùng `@Context SecurityContext` thay vì `HttpHeaders`
- [ ] Tạo `getCurrentUser(securityContext)` helper method
- [ ] KHÔNG dùng try-catch cho business exceptions
- [ ] Inline `ApiResponse` vào `Response.ok()` hoặc `Response.status()`
- [ ] Truyền Request object vào Service, không truyền từng field
- [ ] Service methods có `@Transactional` nếu cần
- [ ] Throw `BusinessException` (hoặc subclass) thay vì return error response


