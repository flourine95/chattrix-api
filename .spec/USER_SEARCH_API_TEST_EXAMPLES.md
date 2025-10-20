# User Search API - Test Examples

## Endpoint
```
GET /api/v1/users/search
```

## Prerequisites
1. Server đang chạy (thường là `http://localhost:8080`)
2. Đã có JWT token (lấy từ login API)
3. Đã có một số users trong database để test

---

## Test Case 1: Tìm kiếm cơ bản

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=john" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Expected Response (200 OK)
```json
{
  "success": true,
  "message": "Users found successfully",
  "data": [
    {
      "id": 5,
      "username": "john_doe",
      "email": "john@example.com",
      "fullName": "John Doe",
      "avatarUrl": "https://example.com/avatar.jpg",
      "isOnline": true,
      "lastSeen": "2025-10-20T10:30:00Z",
      "contact": true,
      "hasConversation": true,
      "conversationId": 15
    }
  ],
  "errors": null
}
```

---

## Test Case 2: Tìm kiếm với limit

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=a&limit=5" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Expected Response (200 OK)
Trả về tối đa 5 users có chứa chữ "a" trong username, email hoặc fullName

---

## Test Case 3: Query rỗng (Error Case)

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Expected Response (400 BAD REQUEST)
```json
{
  "success": false,
  "message": "Search query cannot be empty",
  "data": null,
  "errors": [
    {
      "field": null,
      "code": "INVALID_QUERY",
      "message": "Search query cannot be empty"
    }
  ]
}
```

---

## Test Case 4: Limit vượt quá giới hạn (Error Case)

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=test&limit=100" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Expected Response (400 BAD REQUEST)
```json
{
  "success": false,
  "message": "Limit must be between 1 and 50",
  "data": null,
  "errors": [
    {
      "field": null,
      "code": "INVALID_LIMIT",
      "message": "Limit must be between 1 and 50"
    }
  ]
}
```

---

## Test Case 5: Không có token (Error Case)

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=john" \
  -H "Content-Type: application/json"
```

### Expected Response (401 UNAUTHORIZED)
```json
{
  "success": false,
  "message": "Unauthorized",
  "data": null,
  "errors": [
    {
      "field": null,
      "code": "UNAUTHORIZED",
      "message": "Authentication required"
    }
  ]
}
```

---

## Test Case 6: Tìm kiếm theo email

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=example.com" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Expected Response (200 OK)
Trả về tất cả users có email chứa "example.com"

---

## Test Case 7: Không tìm thấy kết quả

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=zzzzzzzzz" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Expected Response (200 OK)
```json
{
  "success": true,
  "message": "Users found successfully",
  "data": [],
  "errors": null
}
```

---

## Test Case 8: Kiểm tra contact và conversation status

### Scenario
- User A (current user) đã add User B làm contact
- User A đã có conversation với User C
- User A chưa có contact và conversation với User D

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=user" \
  -H "Authorization: Bearer USER_A_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Expected Response (200 OK)
```json
{
  "success": true,
  "message": "Users found successfully",
  "data": [
    {
      "id": 2,
      "username": "user_b",
      "fullName": "User B",
      "contact": true,
      "hasConversation": false,
      "conversationId": null
    },
    {
      "id": 3,
      "username": "user_c",
      "fullName": "User C",
      "contact": false,
      "hasConversation": true,
      "conversationId": 20
    },
    {
      "id": 4,
      "username": "user_d",
      "fullName": "User D",
      "contact": false,
      "hasConversation": false,
      "conversationId": null
    }
  ],
  "errors": null
}
```

---

## Using Postman

### Setup
1. Tạo một request mới trong Postman
2. Method: `GET`
3. URL: `http://localhost:8080/api/v1/users/search`
4. Params:
   - Key: `query`, Value: `john`
   - Key: `limit`, Value: `20` (optional)
5. Headers:
   - Key: `Authorization`, Value: `Bearer YOUR_JWT_TOKEN`
   - Key: `Content-Type`, Value: `application/json`

### Save as Collection
Lưu request này vào collection "Chattrix API" để dễ dàng test lại sau này.

---

## Response Field Meanings

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | ID của user |
| `username` | String | Tên đăng nhập |
| `email` | String | Email của user |
| `fullName` | String | Tên đầy đủ |
| `avatarUrl` | String | URL ảnh đại diện (có thể null) |
| `isOnline` | Boolean | Trạng thái online hiện tại |
| `lastSeen` | Instant | Thời gian online lần cuối |
| `contact` | Boolean | User này có phải là contact của bạn không |
| `hasConversation` | Boolean | Đã có conversation trực tiếp với user này chưa |
| `conversationId` | Long | ID của conversation (null nếu chưa có) |

---

## Notes

1. **Search Priority**: Kết quả được sắp xếp theo độ liên quan:
   - Exact match với username (cao nhất)
   - Exact match với full name
   - Username starts with query
   - Full name starts with query
   - Contains query (thấp nhất)

2. **Case Insensitive**: Tìm kiếm không phân biệt hoa thường

3. **Current User Excluded**: User hiện tại sẽ không xuất hiện trong kết quả

4. **Conversation Check**: Chỉ kiểm tra DIRECT conversation, không kiểm tra GROUP conversation

5. **Performance**: Với database lớn, nên sử dụng limit nhỏ (10-20) để tối ưu performance

