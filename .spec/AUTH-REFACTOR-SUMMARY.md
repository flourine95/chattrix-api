# Tóm Tắt Refactor Auth Module

## ✅ Hoàn Thành

### 1. Request DTOs (Tuân thủ Layered Architecture)

Tất cả Request DTOs đã được cập nhật với Lombok đầy đủ:

#### RegisterRequest.java
```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {
    @NotBlank @Size(min = 4, max = 20)
    @Pattern(regexp = "...")
    @UniqueUsername
    private String username;
    
    @NotBlank @Email
    @UniqueEmail
    private String email;
    
    @NotBlank @Size(min = 6, max = 100)
    private String password;
    
    @NotBlank @Size(min = 1, max = 100)
    private String fullName;
}
```

#### LoginRequest.java
```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LoginRequest {
    @NotBlank
    private String usernameOrEmail;
    
    @NotBlank
    private String password;
}
```

#### ChangePasswordRequest.java
```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;
    
    @NotBlank @Size(min = 6, max = 100)
    private String newPassword;
}
```

#### RefreshTokenRequest.java
```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshTokenRequest {
    @NotBlank
    private String refreshToken;
}
```

---

### 2. Response DTOs

#### AuthResponse.java
```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    
    @Builder.Default
    private String tokenType = "Bearer";
    
    private long expiresIn;
}
```

#### UserResponse.java
- ✅ Xóa field `online` (sử dụng OnlineStatusCache)
- ✅ Cập nhật enums: `User.Gender`, `User.ProfileVisibility`

```java
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private boolean emailVerified;
    private String fullName;
    private String avatarUrl;
    private User.Gender gender;
    private User.ProfileVisibility profileVisibility;
    private Instant lastSeen;  // Không có online
    private Instant createdAt;
    private Instant updatedAt;
}
```

---

### 3. AuthService (Tuân thủ Layered Architecture)

#### Cấu trúc mới:
```
Request → Service → Mapper → Entity → Repository → Database
                ↓
            Validation
                ↓
          Business Logic
                ↓
            Response DTO
```

#### Các method đã refactor:

**register(RegisterRequest)**
```java
Flow:
1. Map request to entity (Mapper)
2. Hash password (BCrypt)
3. Save user (Repository)
4. Generate avatar (AvatarService)
5. Send verification email (VerificationService)
```

**login(LoginRequest)**
```java
Flow:
1. Find user by username/email (Repository)
2. Validate password (BCrypt)
3. Check email verification
4. Update online status (OnlineStatusCache) ← Thay đổi
5. Update lastSeen (Repository)
6. Generate tokens (TokenService)
7. Build response (Builder pattern)
```

**logout(userId, accessToken)**
```java
Flow:
1. Find user (Repository)
2. Mark offline (OnlineStatusCache) ← Thay đổi
3. Update lastSeen (Repository)
4. Invalidate access token (InvalidatedTokenRepository)
5. Revoke refresh token (RefreshTokenRepository)
```

**logoutAllDevices(userId)**
```java
Flow:
1. Find user (Repository)
2. Mark offline (OnlineStatusCache) ← Thay đổi
3. Update lastSeen (Repository)
4. Revoke all refresh tokens (RefreshTokenRepository)
```

**refreshToken(refreshTokenString)**
```java
Flow:
1. Find and validate refresh token (Repository)
2. Invalidate old access token (InvalidatedTokenRepository)
3. Revoke old refresh token (Repository)
4. Generate new tokens (TokenService)
5. Build response (Builder pattern)
```

**changePassword(userId, request)**
```java
Flow:
1. Find user (Repository)
2. Validate current password (BCrypt)
3. Validate new password is different
4. Hash and save new password (Repository)
```

---

### 4. VerificationService (Refactor với UserToken)

#### Thay đổi chính:
- ❌ Xóa: `VerificationTokenRepository`, `PasswordResetTokenRepository`
- ✅ Sử dụng: `UserTokenRepository` với `TokenType.VERIFY` và `TokenType.RESET`

#### sendVerificationEmailByEmail(email)
```java
Flow:
1. Find user by email (Repository)
2. Validate email not verified
3. Delete old VERIFY tokens (UserTokenRepository)
4. Generate OTP (EmailService)
5. Create UserToken with type=VERIFY (Builder)
6. Save token (Repository)
7. Send email (EmailService)
```

#### verifyEmail(request)
```java
Flow:
1. Find user by email (Repository)
2. Validate email not verified
3. Find token by OTP and type=VERIFY (Repository)
4. Validate token belongs to user
5. Validate token is valid
6. Mark token as used (Repository)
7. Mark email as verified (Repository)
```

#### sendPasswordResetEmail(request)
```java
Flow:
1. Find user by email (Repository)
2. Delete old RESET tokens (UserTokenRepository)
3. Generate OTP (EmailService)
4. Create UserToken with type=RESET (Builder)
5. Save token (Repository)
6. Send email (EmailService)
```

#### resetPassword(request)
```java
Flow:
1. Find user by email (Repository)
2. Find token by OTP and type=RESET (Repository)
3. Validate token belongs to user
4. Validate token is valid
5. Mark token as used (Repository)
6. Hash and update password (Repository)
```

---

### 5. AuthResource (Controller - Không thay đổi logic)

Resource chỉ làm nhiệm vụ:
- Tiếp nhận request
- Validate với `@Valid`
- Gọi Service
- Trả về Response với status code

```java
@POST
@Path("/register")
@RateLimited(maxRequests = 3, windowSeconds = 300)
public Response register(@Valid RegisterRequest request) {
    authService.register(request);
    return Response.status(Response.Status.CREATED)
            .entity(ApiResponse.success(null, "Registration successful..."))
            .build();
}
```

**Không có logic nghiệp vụ trong Resource!**

---

## 📊 Thay Đổi Chính

### Online Status Management
**TRƯỚC:**
```java
// Trong AuthService.login()
user.setOnline(true);
userRepository.save(user);

// Trong AuthService.logoutAllDevices()
user.setOnline(false);
userRepository.save(user);
```

**SAU:**
```java
// Trong AuthService.login()
onlineStatusCache.markOnline(user.getId());
user.setLastSeen(Instant.now());
userRepository.save(user);

// Trong AuthService.logout()
onlineStatusCache.markOffline(userId);
user.setLastSeen(Instant.now());
userRepository.save(user);
```

### Token Management
**TRƯỚC:**
```java
// 2 repositories riêng biệt
@Inject VerificationTokenRepository verificationTokenRepository;
@Inject PasswordResetTokenRepository passwordResetTokenRepository;

// Tạo token
VerificationToken token = new VerificationToken();
token.setToken(otp);
token.setUser(user);
verificationTokenRepository.save(token);
```

**SAU:**
```java
// 1 repository duy nhất
@Inject UserTokenRepository userTokenRepository;

// Tạo token với Builder
UserToken token = UserToken.builder()
    .token(otp)
    .user(user)
    .type(UserToken.TokenType.VERIFY)  // hoặc RESET
    .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
    .build();
userTokenRepository.save(token);
```

### Response Building
**TRƯỚC:**
```java
return new AuthResponse(
    accessToken,
    refreshToken.getToken(),
    tokenService.getAccessTokenValidityInSeconds()
);
```

**SAU:**
```java
return AuthResponse.builder()
    .accessToken(accessToken)
    .refreshToken(refreshToken.getToken())
    .tokenType("Bearer")
    .expiresIn(tokenService.getAccessTokenValidityInSeconds())
    .build();
```

---

## 🎯 Tuân Thủ Layered Architecture

### Data Flow
```
Client Request
    ↓
[Resource Layer]
    - Validate with @Valid
    - Call Service
    - Return Response
    ↓
[Service Layer]
    - Business Logic
    - Validation Logic
    - Use Mapper
    - Call Repository
    ↓
[Repository Layer]
    - Data Access
    - JPQL Queries
    - Return Optional<Entity>
    ↓
[Database]
```

### Separation of Concerns

#### Resource (Controller)
- ✅ HTTP handling only
- ✅ No business logic
- ✅ Validate with `@Valid`
- ✅ Return proper status codes

#### Service
- ✅ Business logic
- ✅ Validation logic
- ✅ Use Mapper for DTO ↔ Entity
- ✅ Call Repository
- ✅ Throw exceptions for errors

#### Repository
- ✅ Data access only
- ✅ JPQL queries
- ✅ Return `Optional<Entity>`
- ✅ No business logic

#### Mapper
- ✅ MapStruct interface
- ✅ Entity ↔ DTO conversion
- ✅ Handle complex mappings

---

## 📝 Code Quality Improvements

### 1. Lombok Usage
- ✅ DTOs: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- ✅ Entities: `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- ✅ `@Builder.Default` cho giá trị mặc định

### 2. Builder Pattern
- ✅ Tất cả DTOs sử dụng Builder
- ✅ Entities sử dụng Builder
- ✅ Dễ đọc và maintain

### 3. Comments & Documentation
- ✅ Javadoc cho mỗi method
- ✅ Flow comments trong code
- ✅ Giải thích từng bước

### 4. Exception Handling
- ✅ Sử dụng `BusinessException` với message rõ ràng
- ✅ Không return null
- ✅ Throw exception cho missing resources

---

## 🧪 Testing Checklist

- [ ] Register new user
- [ ] Verify email with OTP
- [ ] Resend verification email
- [ ] Login with username
- [ ] Login with email
- [ ] Get current user
- [ ] Logout from current device
- [ ] Logout from all devices
- [ ] Refresh token
- [ ] Change password
- [ ] Forgot password
- [ ] Reset password with OTP
- [ ] Online status in cache
- [ ] Token expiration
- [ ] Invalid credentials
- [ ] Email already verified
- [ ] Expired OTP

---

## 📚 Files Modified

### Requests
- ✅ `RegisterRequest.java`
- ✅ `LoginRequest.java`
- ✅ `ChangePasswordRequest.java`
- ✅ `RefreshTokenRequest.java`

### Responses
- ✅ `AuthResponse.java`
- ✅ `UserResponse.java`

### Services
- ✅ `AuthService.java`
- ✅ `VerificationService.java`

### Resources
- ✅ `AuthResource.java`

---

## 🔄 Migration Impact

### Database
- ✅ Không cần migration (đã có trong `migration-refactor.sql`)
- ✅ Sử dụng `user_tokens` thay vì `verification_tokens` và `password_reset_tokens`

### Code
- ✅ Cập nhật imports cho inner enums
- ✅ Xóa references đến `VerificationTokenRepository`, `PasswordResetTokenRepository`
- ✅ Sử dụng `OnlineStatusCache` thay vì `user.online`

---

## ✅ Best Practices Followed

1. ✅ **Layered Architecture** - Strict separation of concerns
2. ✅ **Lombok** - Reduce boilerplate
3. ✅ **Builder Pattern** - Readable object creation
4. ✅ **MapStruct** - Type-safe mapping
5. ✅ **Bean Validation** - Validate at DTO level
6. ✅ **Exception Handling** - Never return null
7. ✅ **Comments** - Document flow and logic
8. ✅ **Naming** - Clear and descriptive
9. ✅ **Single Responsibility** - Each layer has one job
10. ✅ **DRY** - Don't repeat yourself

---

**Refactor hoàn tất!** 🎉
