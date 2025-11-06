# Chat Info API Implementation

## Tổng quan

Tài liệu này mô tả việc triển khai các API cho tính năng Chat Info trong ứng dụng Chattrix. Các API này cho phép quản lý thông tin hội thoại, thành viên, cài đặt, tìm kiếm tin nhắn và truy xuất media files.

## Các thành phần đã triển khai

### 1. Entities

#### ConversationSettings
- **File**: `src/main/java/com/chattrix/api/entities/ConversationSettings.java`
- **Mô tả**: Entity lưu trữ cài đặt cá nhân của user cho mỗi conversation
- **Các trường**:
  - `isMuted`: Conversation có bị tắt tiếng không
  - `mutedUntil`: Thời gian tắt tiếng đến (null = vô thời hạn)
  - `isBlocked`: User có bị chặn không (chỉ DIRECT conversation)
  - `blockedAt`: Thời gian bị chặn
  - `notificationsEnabled`: Bật/tắt thông báo
  - `customNickname`: Biệt danh tùy chỉnh
  - `theme`: Theme tùy chỉnh

#### Conversation (Updated)
- **File**: `src/main/java/com/chattrix/api/entities/Conversation.java`
- **Thay đổi**: Thêm field `avatarUrl` để lưu avatar của group conversation

### 2. Repositories

#### ConversationParticipantRepository
- **File**: `src/main/java/com/chattrix/api/repositories/ConversationParticipantRepository.java`
- **Methods**:
  - `findByConversationIdAndUserId()`: Tìm participant theo conversation và user
  - `delete()`: Xóa participant
  - `countByConversationId()`: Đếm số participant trong conversation
  - `isUserParticipant()`: Kiểm tra user có phải participant không
  - `isUserAdmin()`: Kiểm tra user có phải admin không

#### ConversationSettingsRepository
- **File**: `src/main/java/com/chattrix/api/repositories/ConversationSettingsRepository.java`
- **Methods**:
  - `findByUserIdAndConversationId()`: Tìm settings của user cho conversation

#### MessageRepository (Updated)
- **File**: `src/main/java/com/chattrix/api/repositories/MessageRepository.java`
- **Methods mới**:
  - `searchMessages()`: Tìm kiếm messages với filters
  - `countSearchMessages()`: Đếm số messages tìm được
  - `findMediaByConversationId()`: Lấy media files theo type
  - `countMediaByConversationId()`: Đếm số media files

### 3. Request DTOs

- `UpdateConversationRequest`: Cập nhật tên và avatar của conversation
- `AddMembersRequest`: Thêm members vào group
- `UpdateMemberRoleRequest`: Cập nhật role của member (ADMIN/MEMBER)
- `MuteConversationRequest`: Tắt/bật tiếng conversation
- `UpdateConversationSettingsRequest`: Cập nhật settings

### 4. Response DTOs

- `ConversationSettingsResponse`: Thông tin settings của conversation
- `AddMembersResponse`: Danh sách members đã thêm
- `MuteConversationResponse`: Trạng thái mute
- `BlockUserResponse`: Trạng thái block
- `MediaResponse`: Thông tin media file

### 5. Services

#### ConversationService (Extended)
- **File**: `src/main/java/com/chattrix/api/services/ConversationService.java`
- **Methods mới**:
  - `updateConversation()`: Cập nhật thông tin conversation (GROUP only, ADMIN only)
  - `deleteConversation()`: Xóa conversation (soft delete)
  - `leaveConversation()`: Rời khỏi group conversation
  - `addMembers()`: Thêm members vào group (ADMIN only)
  - `removeMember()`: Xóa member khỏi group (ADMIN only)
  - `updateMemberRole()`: Cập nhật role của member (ADMIN only)
  - `getConversationSettings()`: Lấy settings của user
  - `updateConversationSettings()`: Cập nhật settings
  - `muteConversation()`: Tắt/bật tiếng conversation
  - `blockUser()`: Chặn user (DIRECT only)
  - `unblockUser()`: Bỏ chặn user (DIRECT only)

#### MessageService (Extended)
- **File**: `src/main/java/com/chattrix/api/services/MessageService.java`
- **Methods mới**:
  - `searchMessages()`: Tìm kiếm messages với filters (query, type, senderId)
  - `getMediaFiles()`: Lấy media files theo type (IMAGE, VIDEO, AUDIO, DOCUMENT)

### 6. REST Endpoints

#### ConversationResource (Extended)
- **File**: `src/main/java/com/chattrix/api/resources/ConversationResource.java`
- **Base Path**: `/v1/conversations`

**Endpoints mới**:

1. **PUT /{conversationId}** - Cập nhật conversation
2. **DELETE /{conversationId}** - Xóa conversation
3. **POST /{conversationId}/leave** - Rời khỏi group
4. **POST /{conversationId}/members** - Thêm members
5. **DELETE /{conversationId}/members/{userId}** - Xóa member
6. **PUT /{conversationId}/members/{userId}/role** - Cập nhật role
7. **GET /{conversationId}/settings** - Lấy settings
8. **PUT /{conversationId}/settings** - Cập nhật settings
9. **POST /{conversationId}/mute** - Tắt/bật tiếng
10. **POST /{conversationId}/block** - Chặn user
11. **POST /{conversationId}/unblock** - Bỏ chặn user
12. **GET /{conversationId}/messages/search** - Tìm kiếm messages
13. **GET /{conversationId}/media** - Lấy media files

## Database Migration

**File**: `src/main/resources/db/migration/chat_info_feature.sql`

Chạy script SQL này để:
1. Thêm cột `avatar_url` vào bảng `conversations`
2. Tạo bảng `conversation_settings`
3. Tạo các indexes để tối ưu hóa tìm kiếm

## Cách sử dụng

### 1. Chạy Database Migration

```bash
psql -U your_username -d your_database -f src/main/resources/db/migration/chat_info_feature.sql
```

### 2. Build và Deploy

```bash
mvn clean package
mvn wildfly:deploy
```

### 3. Test APIs

Sử dụng Postman hoặc curl để test các endpoints. Ví dụ:

```bash
# Update conversation
curl -X PUT http://localhost:8080/v1/conversations/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "New Group Name", "avatarUrl": "https://example.com/avatar.jpg"}'

# Search messages
curl -X GET "http://localhost:8080/v1/conversations/1/messages/search?query=hello&page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"

# Get media files
curl -X GET "http://localhost:8080/v1/conversations/1/media?type=IMAGE&page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Quyền hạn (Permissions)

### Admin-only operations (GROUP conversations):
- Update conversation info
- Add members
- Remove members
- Update member roles

### All participants:
- Leave conversation
- Get/update personal settings
- Mute/unmute conversation
- Search messages
- Get media files

### DIRECT conversations only:
- Block/unblock user

## Validation Rules

1. **Update Conversation**: Chỉ GROUP conversations, chỉ ADMIN
2. **Add/Remove Members**: Chỉ GROUP conversations, chỉ ADMIN
3. **Update Member Role**: Chỉ GROUP conversations, chỉ ADMIN
4. **Leave Conversation**: Chỉ GROUP conversations
5. **Block/Unblock**: Chỉ DIRECT conversations
6. **Mute Duration**:
   - `null` hoặc `0`: Unmute
   - `-1`: Mute vô thời hạn
   - Số dương: Mute trong N giây

## Error Handling

Tất cả các endpoints đều sử dụng exception handling thống nhất:
- `BadRequestException`: Lỗi validation hoặc business logic
- `ResourceNotFoundException`: Không tìm thấy resource
- `UnauthorizedException`: Không có quyền truy cập

## Pagination

Các endpoints search và media hỗ trợ pagination:
- `page`: Số trang (default: 0)
- `size`: Số items mỗi trang (default: 20)
- Response bao gồm:
  - `data`: Danh sách items
  - `pagination`: Thông tin phân trang (page, size, totalElements, totalPages)

## Next Steps

1. Viết unit tests cho các services
2. Viết integration tests cho các endpoints
3. Thêm WebSocket events cho real-time updates khi có thay đổi
4. Thêm push notifications cho các events quan trọng
5. Tối ưu hóa performance với caching

## Tài liệu tham khảo

- `.spec/CHAT_INFO_FEATURE_README.md`: Yêu cầu tính năng
- `.spec/CHAT_INFO_API_PROPOSAL.md`: Thiết kế API

