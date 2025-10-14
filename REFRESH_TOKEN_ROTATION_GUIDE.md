# Refresh Token & Rotation Implementation Guide

## Tổng quan

Hệ thống đã được triển khai với cơ chế **Refresh Token Rotation** - một best practice quan trọng trong bảo mật JWT để:
- Giảm thiểu rủi ro khi access token bị lộ
- Tự động vô hiệu hóa refresh token cũ khi tạo token mới
- Hỗ trợ logout từ tất cả các thiết bị
- Quản lý phiên làm việc dài hạn an toàn

## Các thay đổi chính

### 1. Entity: RefreshToken
**File:** `src/main/java/com/chattrix/api/entities/RefreshToken.java`

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    private Long id;
    private String token;          // UUID ngẫu nhiên
    private User user;             // Liên kết với user
    private Instant expiresAt;     // Thời hạn 7 ngày
    private Instant createdAt;     // Thời điểm tạo
    private boolean isRevoked;     // Đã bị vô hiệu hóa?
    private Instant revokedAt;     // Thời điểm revoke
}
```

**Đặc điểm:**
- Token được tạo bằng UUID ngẫu nhiên (không phải JWT)
- Lưu trữ trong database với relationship tới User
- Có thể bị revoke (vô hiệu hóa) khi cần

### 2. Repository: RefreshTokenRepository
**File:** `src/main/java/com/chattrix/api/repositories/RefreshTokenRepository.java`

**Chức năng:**
- `save()`: Lưu refresh token mới
- `findValidToken()`: Tìm token hợp lệ (chưa revoke, chưa hết hạn)
- `revokeAllByUser()`: Revoke tất cả tokens của user (logout all devices)
- `deleteExpiredTokens()`: Xóa tokens đã hết hạn
- `deleteRevokedTokens()`: Xóa tokens đã revoke > 7 ngày

### 3. TokenService
**File:** `src/main/java/com/chattrix/api/services/TokenService.java`

**Cấu hình:**
```java
ACCESS_TOKEN_VALIDITY = 15 minutes   // Giảm từ 1h xuống 15 phút
REFRESH_TOKEN_VALIDITY = 7 days      // Dài hạn
```

**Phương thức mới:**
- `generateAccessToken()`: Tạo JWT access token (15 phút)
- `generateRefreshToken()`: Tạo refresh token (7 ngày) và lưu vào DB

**Lý do giảm thời gian access token:**
- Refresh token rotation cho phép dùng access token ngắn hạn
- Tăng bảo mật: nếu bị đánh cắp, chỉ có hiệu lực 15 phút
- Refresh token an toàn hơn vì được kiểm tra qua database

### 4. AuthService
**File:** `src/main/java/com/chattrix/api/services/AuthService.java`

#### Login (Cập nhật)
```java
public AuthResponse login(LoginRequest request) {
    // Xác thực user...
    
    // Tạo cả access token và refresh token
    String accessToken = tokenService.generateAccessToken(user);
    RefreshToken refreshToken = tokenService.generateRefreshToken(user);
    
    return new AuthResponse(
        accessToken,           // JWT, 15 phút
        refreshToken.getToken(), // UUID, 7 ngày
        900                    // expiresIn (seconds)
    );
}
```

#### Refresh Token (Rotation)
```java
public AuthResponse refreshToken(String refreshTokenString) {
    // 1. Tìm và validate refresh token trong DB
    RefreshToken refreshToken = refreshTokenRepository
        .findValidToken(refreshTokenString)
        .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
    
    // 2. ROTATION: Revoke refresh token cũ ngay lập tức
    refreshToken.revoke();
    refreshTokenRepository.update(refreshToken);
    
    // 3. Tạo access token và refresh token MỚI
    String newAccessToken = tokenService.generateAccessToken(user);
    RefreshToken newRefreshToken = tokenService.generateRefreshToken(user);
    
    return new AuthResponse(newAccessToken, newRefreshToken.getToken(), 900);
}
```

**Ưu điểm của Rotation:**
- Mỗi refresh token chỉ dùng được 1 lần
- Nếu kẻ tấn công đánh cắp refresh token cũ → không dùng được (đã revoked)
- Phát hiện token reuse attack

#### Logout (Cập nhật)
```java
public void logout(String username, String accessToken) {
    // 1. Blacklist access token
    invalidatedTokenRepository.save(new InvalidatedToken(accessToken, ...));
    
    // 2. Revoke TẤT CẢ refresh tokens của user
    refreshTokenRepository.revokeAllByUser(user);
}
```

**Logout all devices:**
- Khi user logout, tất cả refresh tokens đều bị revoke
- Các thiết bị khác không thể refresh token nữa
- Phải login lại trên tất cả thiết bị

### 5. AuthResponse
**File:** `src/main/java/com/chattrix/api/dto/responses/AuthResponse.java`

```java
{
    "accessToken": "eyJhbGci...",  // JWT cho API calls
    "refreshToken": "uuid-string", // Để làm mới access token
    "tokenType": "Bearer",
    "expiresIn": 900               // Access token hết hạn sau 900s (15 phút)
}
```

### 6. TokenCleanupService
**File:** `src/main/java/com/chattrix/api/services/TokenCleanupService.java`

**Scheduled Tasks:**
1. **Mỗi giờ:** Xóa access tokens đã hết hạn khỏi blacklist
2. **Mỗi ngày (2 AM):** 
   - Xóa refresh tokens đã hết hạn
   - Xóa refresh tokens đã revoke > 7 ngày

## Cấu trúc Database

### Bảng: refresh_tokens
```sql
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) UNIQUE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

## Luồng hoạt động

### 1. Login Flow
```
Client                    Server                     Database
  |                         |                           |
  |--POST /v1/auth/login--->|                           |
  |  {username, password}   |                           |
  |                         |--Verify credentials------>|
  |                         |                           |
  |                         |--Generate access token--->|
  |                         |  (JWT, 15 min)            |
  |                         |                           |
  |                         |--Generate refresh token-->|
  |                         |  (UUID, 7 days)           |
  |                         |--Save to DB-------------->|
  |                         |                           |
  |<--{accessToken,---------|                           |
  |    refreshToken,        |                           |
  |    expiresIn: 900}      |                           |
  |                         |                           |
```

**Client lưu:**
- `accessToken` → Memory hoặc sessionStorage (bảo mật hơn)
- `refreshToken` → httpOnly cookie hoặc secure storage

### 2. API Call Flow
```
Client                    Server
  |                         |
  |--GET /v1/conversations->|
  |  Authorization: Bearer  |
  |  {accessToken}          |
  |                         |
  |                         |--Validate access token
  |                         |  (JWT signature + blacklist)
  |                         |
  |<--200 OK----------------|
  |  {data}                 |
  |                         |
```

### 3. Token Refresh Flow (Rotation)
```
Client                    Server                     Database
  |                         |                           |
  |--POST /v1/auth/refresh->|                           |
  |  {refreshToken}         |                           |
  |                         |--Find token in DB-------->|
  |                         |  WHERE token = ? AND      |
  |                         |  isRevoked = false AND    |
  |                         |  expiresAt > now          |
  |                         |<--RefreshToken------------|
  |                         |                           |
  |                         |--REVOKE old token-------->|
  |                         |  SET isRevoked = true     |
  |                         |  SET revokedAt = now      |
  |                         |                           |
  |                         |--Generate NEW tokens----->|
  |                         |  - New access token       |
  |                         |  - New refresh token      |
  |                         |--Save new refresh-------->|
  |                         |                           |
  |<--{newAccessToken,------|                           |
  |    newRefreshToken,     |                           |
  |    expiresIn: 900}      |                           |
  |                         |                           |
```

**Quan trọng:** Old refresh token bị revoke ngay lập tức!

### 4. Logout Flow
```
Client                    Server                     Database
  |                         |                           |
  |--POST /v1/auth/logout-->|                           |
  |  Authorization: Bearer  |                           |
  |  {accessToken}          |                           |
  |                         |--Blacklist access token-->|
  |                         |                           |
  |                         |--Revoke ALL refresh------->|
  |                         |  tokens của user          |
  |                         |  UPDATE refresh_tokens    |
  |                         |  SET isRevoked = true     |
  |                         |  WHERE user_id = ?        |
  |                         |                           |
  |<--200 OK----------------|                           |
  |                         |                           |
```

**Kết quả:**
- Access token hiện tại → Blacklist (không dùng được nữa)
- Tất cả refresh tokens → Revoked (logout all devices)

## API Endpoints

### 1. POST /v1/auth/login
**Request:**
```json
{
  "username": "user1",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

### 2. POST /v1/auth/refresh
**Request:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "660e8400-e29b-41d4-a716-446655440001",
    "tokenType": "Bearer",
    "expiresIn": 900
  }
}
```

**Lưu ý:** Refresh token mới khác với token cũ (rotation)!

### 3. POST /v1/auth/logout
**Request:**
```http
POST /v1/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Response:**
```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

## Client Implementation Example

### JavaScript/TypeScript
```typescript
class AuthService {
  private accessToken: string | null = null;
  private refreshToken: string | null = null;
  private refreshTimer: NodeJS.Timeout | null = null;

  async login(username: string, password: string) {
    const response = await fetch('/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    
    const { data } = await response.json();
    this.setTokens(data.accessToken, data.refreshToken, data.expiresIn);
  }

  private setTokens(accessToken: string, refreshToken: string, expiresIn: number) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    
    // Lưu refresh token vào localStorage (hoặc httpOnly cookie tốt hơn)
    localStorage.setItem('refreshToken', refreshToken);
    
    // Tự động refresh trước khi hết hạn (refresh ở 80% thời gian)
    this.scheduleRefresh(expiresIn * 0.8 * 1000);
  }

  private scheduleRefresh(delay: number) {
    if (this.refreshTimer) clearTimeout(this.refreshTimer);
    
    this.refreshTimer = setTimeout(() => {
      this.refreshAccessToken();
    }, delay);
  }

  private async refreshAccessToken() {
    try {
      const response = await fetch('/v1/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: this.refreshToken })
      });
      
      if (response.ok) {
        const { data } = await response.json();
        this.setTokens(data.accessToken, data.refreshToken, data.expiresIn);
      } else {
        // Refresh token không hợp lệ -> redirect to login
        this.logout();
        window.location.href = '/login';
      }
    } catch (error) {
      console.error('Failed to refresh token', error);
      this.logout();
    }
  }

  async apiCall(url: string, options: RequestInit = {}) {
    // Thêm access token vào mọi request
    const headers = {
      ...options.headers,
      'Authorization': `Bearer ${this.accessToken}`
    };
    
    let response = await fetch(url, { ...options, headers });
    
    // Nếu 401 (token hết hạn), thử refresh và retry
    if (response.status === 401) {
      await this.refreshAccessToken();
      headers.Authorization = `Bearer ${this.accessToken}`;
      response = await fetch(url, { ...options, headers });
    }
    
    return response;
  }

  async logout() {
    if (this.accessToken) {
      await fetch('/v1/auth/logout', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${this.accessToken}` }
      });
    }
    
    this.accessToken = null;
    this.refreshToken = null;
    localStorage.removeItem('refreshToken');
    if (this.refreshTimer) clearTimeout(this.refreshTimer);
  }
}
```

## Security Best Practices

### ✅ Đã triển khai

1. **Short-lived access tokens (15 phút)**
   - Giảm thiểu thiệt hại nếu bị đánh cắp
   - Force refresh thường xuyên

2. **Refresh token rotation**
   - Mỗi refresh token chỉ dùng 1 lần
   - Tự động vô hiệu hóa token cũ

3. **Token blacklist**
   - Access tokens bị logout được blacklist
   - Không thể sử dụng lại

4. **Logout all devices**
   - Revoke tất cả refresh tokens khi logout
   - Bảo vệ tài khoản khỏi thiết bị bị mất

5. **Automatic cleanup**
   - Xóa tokens hết hạn định kỳ
   - Tối ưu hiệu suất database

### 🔐 Khuyến nghị bổ sung

1. **HttpOnly Cookies cho refresh token**
   ```java
   // Thay vì trả JSON, set cookie
   Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
   refreshCookie.setHttpOnly(true);
   refreshCookie.setSecure(true);  // HTTPS only
   refreshCookie.setPath("/v1/auth/refresh");
   refreshCookie.setMaxAge(7 * 24 * 60 * 60);  // 7 days
   ```

2. **Fingerprinting**
   - Lưu User-Agent, IP trong refresh token
   - Validate khi refresh để phát hiện token theft

3. **Rate limiting refresh endpoint**
   - Giới hạn số lần refresh/phút
   - Ngăn chặn brute force

4. **Refresh token reuse detection**
   ```java
   // Nếu phát hiện dùng lại token đã revoke
   // -> Revoke ALL tokens của user (suspicious activity)
   if (refreshToken.isRevoked()) {
       refreshTokenRepository.revokeAllByUser(refreshToken.getUser());
       throw new SecurityException("Token reuse detected!");
   }
   ```

## Testing

### Test Case 1: Login → Access API → Refresh → Access API
```bash
# 1. Login
POST /v1/auth/login
{
  "username": "user1",
  "password": "password123"
}
# Save accessToken1 and refreshToken1

# 2. Use access token
GET /v1/auth/me
Authorization: Bearer {accessToken1}
# Expected: 200 OK

# 3. Wait 15 minutes (or mock time), then refresh
POST /v1/auth/refresh
{
  "refreshToken": "{refreshToken1}"
}
# Get accessToken2 and refreshToken2

# 4. Old refresh token không dùng được nữa
POST /v1/auth/refresh
{
  "refreshToken": "{refreshToken1}"
}
# Expected: 401 Unauthorized - "Invalid or expired refresh token"

# 5. Use new access token
GET /v1/auth/me
Authorization: Bearer {accessToken2}
# Expected: 200 OK
```

### Test Case 2: Logout → Tokens không dùng được
```bash
# 1. Login
POST /v1/auth/login
# Save accessToken and refreshToken

# 2. Logout
POST /v1/auth/logout
Authorization: Bearer {accessToken}

# 3. Try to use old access token
GET /v1/auth/me
Authorization: Bearer {accessToken}
# Expected: 401 Unauthorized

# 4. Try to use old refresh token
POST /v1/auth/refresh
{
  "refreshToken": "{refreshToken}"
}
# Expected: 401 Unauthorized
```

### Test Case 3: Refresh token rotation
```bash
# 1. Login và lưu refreshToken1
# 2. Refresh lần 1 → Nhận refreshToken2
# 3. Refresh lần 2 với refreshToken2 → Nhận refreshToken3
# 4. Thử dùng refreshToken1 hoặc refreshToken2 → FAIL
# 5. Chỉ refreshToken3 (mới nhất) mới hoạt động
```

## Monitoring & Logs

Các sự kiện nên log:
```
INFO: User 'user1' logged in, refresh token created
INFO: User 'user1' refreshed access token
INFO: User 'user1' logged out, 3 refresh tokens revoked
INFO: Cleaned up 150 expired refresh tokens
WARN: Token reuse detected for user 'user1'
ERROR: Failed to revoke refresh tokens for user 'user1'
```

## Migration từ hệ thống cũ

Nếu đang có hệ thống chỉ dùng access token:

1. Deploy code mới
2. Database sẽ tự động tạo bảng `refresh_tokens`
3. User cũ phải login lại để nhận refresh token
4. Từ từ tất cả users sẽ chuyển sang hệ thống mới

## Kết luận

Hệ thống refresh token rotation đã được triển khai đầy đủ với:
- ✅ Access token ngắn hạn (15 phút)
- ✅ Refresh token dài hạn (7 ngày)
- ✅ Automatic rotation (mỗi lần refresh)
- ✅ Logout all devices
- ✅ Token blacklist
- ✅ Automatic cleanup

Đây là một giải pháp bảo mật hiện đại, tuân thủ OAuth 2.0 best practices!

