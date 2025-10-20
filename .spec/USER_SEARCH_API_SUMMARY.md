# User Search API - Tóm tắt Implementation

## Mô tả
API tìm kiếm người dùng để tạo đoạn chat mới. Cho phép tìm kiếm theo username, email hoặc full name.

## Endpoint
```
GET /api/v1/users/search?query={keyword}&limit={number}
```

## Các file đã tạo

### 1. Request DTO
**File:** `src/main/java/com/chattrix/api/requests/SearchUserRequest.java`
- Chứa validation cho query tìm kiếm
- Query phải có từ 1-100 ký tự

### 2. Response DTO
**File:** `src/main/java/com/chattrix/api/responses/UserSearchResponse.java`
- Chứa thông tin user cơ bản (id, username, email, fullName, avatarUrl, isOnline, lastSeen)
- Thông tin bổ sung:
  - `contact`: User có phải là contact không
  - `hasConversation`: Đã có conversation trực tiếp chưa
  - `conversationId`: ID của conversation (nếu có)

### 3. Mapper
**File:** `src/main/java/com/chattrix/api/mappers/UserSearchMapper.java`
- MapStruct mapper để chuyển đổi User entity sang UserSearchResponse
- Sử dụng CDI component model

### 4. Repository Updates
**File:** `src/main/java/com/chattrix/api/repositories/UserRepository.java`
- Thêm method `searchUsers(String query, Long excludeUserId, int limit)`
- Tìm kiếm theo username, email, fullName (không phân biệt hoa thường)
- Sắp xếp kết quả theo độ liên quan:
  1. Khớp chính xác với username
  2. Khớp chính xác với full name
  3. Username bắt đầu bằng từ khóa
  4. Full name bắt đầu bằng từ khóa
  5. Các kết quả khác

**File:** `src/main/java/com/chattrix/api/repositories/ConversationRepository.java`
- Thêm method `findDirectConversationBetweenUsers(Long userId1, Long userId2)`
- Tìm conversation DIRECT giữa 2 users

### 5. Service
**File:** `src/main/java/com/chattrix/api/services/UserSearchService.java`
- Method `searchUsers(Long currentUserId, String query, int limit)`
- Logic:
  1. Tìm kiếm users theo query
  2. Chuyển đổi sang response DTO
  3. Kiểm tra từng user xem có phải contact không
  4. Kiểm tra xem đã có conversation trực tiếp chưa
  5. Trả về danh sách kết quả với đầy đủ thông tin

### 6. Resource (Controller)
**File:** `src/main/java/com/chattrix/api/resources/UserSearchResource.java`
- Endpoint: `GET /v1/users/search`
- Query params:
  - `query`: String (required) - Từ khóa tìm kiếm
  - `limit`: Integer (optional, default: 20, max: 50)
- Validation:
  - Query không được rỗng
  - Limit phải từ 1-50
- Yêu cầu authentication (@Secured)

## Cấu trúc tuân theo
✅ **Request**: SearchUserRequest.java với validation
✅ **Response**: UserSearchResponse.java với đầy đủ thông tin
✅ **Mapper**: UserSearchMapper.java sử dụng MapStruct
✅ **Repository**: Cập nhật UserRepository và ConversationRepository
✅ **Service**: UserSearchService.java xử lý business logic
✅ **Resource**: UserSearchResource.java làm REST controller

## Ví dụ sử dụng

### Request
```bash
curl -X GET "http://localhost:8080/api/v1/users/search?query=john&limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Response
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

## Tính năng
- ✅ Tìm kiếm theo username, email, full name
- ✅ Không phân biệt hoa thường
- ✅ Sắp xếp theo độ liên quan
- ✅ Loại trừ user hiện tại khỏi kết quả
- ✅ Hiển thị trạng thái contact (field: `contact`)
- ✅ Hiển thị conversation đã có (field: `hasConversation`, `conversationId`)
- ✅ Giới hạn số lượng kết quả
- ✅ Yêu cầu authentication
- ✅ Validation đầy đủ

## Testing
Để test API này:
1. Đăng nhập để lấy JWT token
2. Gọi endpoint với query parameter
3. Kiểm tra kết quả trả về có đúng format không
4. Kiểm tra các trường `contact` và `hasConversation`
5. Test với các trường hợp: query rỗng, limit vượt quá, không có kết quả

## Documentation
Đã cập nhật file `REST_API_SPECIFICATION.md` với đầy đủ thông tin về endpoint mới.

