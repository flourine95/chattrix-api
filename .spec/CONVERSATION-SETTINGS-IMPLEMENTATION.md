# Conversation Settings Implementation

## Overview

Đã implement các chức năng quản lý conversation settings cho user:
- Archive/Unarchive
- Mute/Unmute
- Pin/Unpin
- Delete (= Archive)

## API Endpoints

### 1. Delete Conversation (Archive)
```
DELETE /api/v1/conversations/{conversationId}
```
**Behavior:** Archive conversation cho user hiện tại (soft delete)
- User không thấy conversation trong list
- Conversation vẫn tồn tại trong DB
- Người khác vẫn thấy bình thường
- Có thể unarchive để restore

**Response:**
```json
{
  "success": true,
  "message": "Conversation archived successfully"
}
```

### 2. Archive Conversation
```
POST /api/v1/conversations/{conversationId}/archive
```
**Behavior:** Giống DELETE, archive conversation
- Set `archived = true`
- Set `archivedAt = now()`
- Invalidate cache

**Response:**
```json
{
  "success": true,
  "message": "Conversation archived successfully"
}
```

### 3. Unarchive Conversation
```
POST /api/v1/conversations/{conversationId}/unarchive
```
**Behavior:** Restore conversation từ archive
- Set `archived = false`
- Set `archivedAt = null`
- Invalidate cache

**Response:**
```json
{
  "success": true,
  "message": "Conversation unarchived successfully"
}
```

### 4. Mute Conversation
```
POST /api/v1/conversations/{conversationId}/mute?duration={minutes}
```
**Query Parameters:**
- `duration` (optional): Số phút mute. Nếu không có = mute vĩnh viễn

**Behavior:**
- Set `muted = true`
- Set `mutedAt = now()`
- Nếu có `duration`: Set `mutedUntil = now() + duration`
- Nếu không có `duration`: Set `mutedUntil = null` (permanent mute)
- Invalidate cache

**Examples:**
```bash
# Mute 60 phút
POST /api/v1/conversations/1/mute?duration=60

# Mute 24 giờ (1440 phút)
POST /api/v1/conversations/1/mute?duration=1440

# Mute vĩnh viễn
POST /api/v1/conversations/1/mute
```

**Response:**
```json
{
  "success": true,
  "message": "Conversation muted successfully"
}
```

### 5. Unmute Conversation
```
POST /api/v1/conversations/{conversationId}/unmute
```
**Behavior:**
- Set `muted = false`
- Set `mutedAt = null`
- Set `mutedUntil = null`
- Invalidate cache

**Response:**
```json
{
  "success": true,
  "message": "Conversation unmuted successfully"
}
```

### 6. Pin Conversation
```
POST /api/v1/conversations/{conversationId}/pin
```
**Behavior:**
- Check nếu đã pin → throw error
- Get max `pinOrder` của user
- Set `pinned = true`
- Set `pinnedAt = now()`
- Set `pinOrder = maxPinOrder + 1`
- Invalidate cache

**Response:**
```json
{
  "success": true,
  "message": "Conversation pinned successfully"
}
```

**Error (already pinned):**
```json
{
  "success": false,
  "message": "Conversation is already pinned",
  "errorCode": "ALREADY_PINNED"
}
```

### 7. Unpin Conversation
```
POST /api/v1/conversations/{conversationId}/unpin
```
**Behavior:**
- Check nếu chưa pin → throw error
- Set `pinned = false`
- Set `pinnedAt = null`
- Set `pinOrder = null`
- Invalidate cache

**Response:**
```json
{
  "success": true,
  "message": "Conversation unpinned successfully"
}
```

**Error (not pinned):**
```json
{
  "success": false,
  "message": "Conversation is not pinned",
  "errorCode": "NOT_PINNED"
}
```

## Database Schema

### ConversationParticipant Fields

```sql
-- Archive
archived BOOLEAN NOT NULL DEFAULT false
archived_at TIMESTAMP

-- Mute
muted BOOLEAN NOT NULL DEFAULT false
muted_at TIMESTAMP
muted_until TIMESTAMP  -- NULL = permanent mute

-- Pin
pinned BOOLEAN NOT NULL DEFAULT false
pinned_at TIMESTAMP
pin_order INTEGER  -- Order of pinned conversations

-- Notifications
notifications_enabled BOOLEAN NOT NULL DEFAULT true
```

## Business Logic

### Archive
- **Per-user setting**: Mỗi user có thể archive riêng
- **Soft delete**: Conversation không bị xóa khỏi DB
- **Reversible**: Có thể unarchive
- **Filter**: Có thể filter conversations để exclude archived

### Mute
- **Per-user setting**: Mỗi user có thể mute riêng
- **Temporary or permanent**: Có thể mute có thời hạn hoặc vĩnh viễn
- **Check expiry**: Method `isCurrentlyMuted()` trong entity check `mutedUntil`
- **Notifications**: Khi muted, không gửi push notifications

### Pin
- **Per-user setting**: Mỗi user có thể pin riêng
- **Ordered**: Dùng `pinOrder` để sort pinned conversations
- **Auto-increment**: `pinOrder` tự động tăng khi pin mới
- **Display**: Pinned conversations hiển thị đầu list

## Implementation Details

### Service Layer
```java
@Transactional
public void archiveConversation(Long userId, Long conversationId) {
    ConversationParticipant participant = participantRepository
            .findByConversationIdAndUserId(conversationId, userId)
            .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

    participant.setArchived(true);
    participant.setArchivedAt(Instant.now());
    participantRepository.save(participant);

    conversationCache.invalidate(userId, conversationId);
}
```

### Repository Layer
```java
public Integer getMaxPinOrder(Long userId) {
    Integer maxOrder = em.createQuery(
            "SELECT MAX(cp.pinOrder) FROM ConversationParticipant cp " +
            "WHERE cp.user.id = :userId AND cp.pinned = true",
            Integer.class)
        .setParameter("userId", userId)
        .getSingleResult();
    return maxOrder != null ? maxOrder : 0;
}
```

### Entity Layer
```java
@PrePersist
protected void onPrePersist() {
    if (this.joinedAt == null) {
        this.joinedAt = Instant.now();
    }
    // Set default values for boolean fields
    if (!archived && archivedAt == null) {
        this.archived = false;
    }
    if (!muted && mutedAt == null && mutedUntil == null) {
        this.muted = false;
    }
    if (!pinned && pinnedAt == null && pinOrder == null) {
        this.pinned = false;
    }
    if (!notificationsEnabled && this.id == null) {
        this.notificationsEnabled = true;
    }
}

public boolean isCurrentlyMuted() {
    if (!muted) return false;
    if (mutedUntil == null) return true;  // Permanent mute
    return Instant.now().isBefore(mutedUntil);
}
```

## Cache Strategy

Tất cả operations đều invalidate cache:
```java
conversationCache.invalidate(userId, conversationId);
```

## Error Handling

### Common Errors
- `404 RESOURCE_NOT_FOUND`: Conversation không tồn tại hoặc user không phải participant
- `400 ALREADY_PINNED`: Conversation đã được pin
- `400 NOT_PINNED`: Conversation chưa được pin

## Testing

### Test Scenarios

1. **Archive/Unarchive**
   ```bash
   # Archive
   curl -X POST http://localhost:8080/api/v1/conversations/1/archive \
     -H "Authorization: Bearer $TOKEN"
   
   # Verify không thấy trong list
   curl http://localhost:8080/api/v1/conversations \
     -H "Authorization: Bearer $TOKEN"
   
   # Unarchive
   curl -X POST http://localhost:8080/api/v1/conversations/1/unarchive \
     -H "Authorization: Bearer $TOKEN"
   ```

2. **Mute/Unmute**
   ```bash
   # Mute 60 phút
   curl -X POST "http://localhost:8080/api/v1/conversations/1/mute?duration=60" \
     -H "Authorization: Bearer $TOKEN"
   
   # Mute vĩnh viễn
   curl -X POST http://localhost:8080/api/v1/conversations/1/mute \
     -H "Authorization: Bearer $TOKEN"
   
   # Unmute
   curl -X POST http://localhost:8080/api/v1/conversations/1/unmute \
     -H "Authorization: Bearer $TOKEN"
   ```

3. **Pin/Unpin**
   ```bash
   # Pin
   curl -X POST http://localhost:8080/api/v1/conversations/1/pin \
     -H "Authorization: Bearer $TOKEN"
   
   # Pin thêm (pinOrder tự động tăng)
   curl -X POST http://localhost:8080/api/v1/conversations/2/pin \
     -H "Authorization: Bearer $TOKEN"
   
   # Unpin
   curl -X POST http://localhost:8080/api/v1/conversations/1/unpin \
     -H "Authorization: Bearer $TOKEN"
   ```

4. **Delete (Archive)**
   ```bash
   # Delete = Archive
   curl -X DELETE http://localhost:8080/api/v1/conversations/1 \
     -H "Authorization: Bearer $TOKEN"
   
   # Verify archived
   curl http://localhost:8080/api/v1/conversations/1 \
     -H "Authorization: Bearer $TOKEN"
   ```

## Frontend Integration

### State Management
```javascript
// Archive
const archiveConversation = async (conversationId) => {
  await api.post(`/conversations/${conversationId}/archive`);
  // Remove from list
  setConversations(prev => prev.filter(c => c.id !== conversationId));
};

// Mute
const muteConversation = async (conversationId, duration) => {
  const url = duration 
    ? `/conversations/${conversationId}/mute?duration=${duration}`
    : `/conversations/${conversationId}/mute`;
  await api.post(url);
  // Update UI
  updateConversation(conversationId, { muted: true });
};

// Pin
const pinConversation = async (conversationId) => {
  await api.post(`/conversations/${conversationId}/pin`);
  // Move to top of list
  setConversations(prev => {
    const conv = prev.find(c => c.id === conversationId);
    const others = prev.filter(c => c.id !== conversationId);
    return [{ ...conv, pinned: true }, ...others];
  });
};
```

## Future Enhancements

1. **Bulk operations**
   - Archive multiple conversations
   - Mute multiple conversations

2. **Smart mute**
   - Mute until next morning
   - Mute for 1 hour, 8 hours, 1 day, 1 week

3. **Pin limit**
   - Max 3-5 pinned conversations
   - Show warning when limit reached

4. **Archive folder**
   - Separate view for archived conversations
   - Search in archived

5. **Mute exceptions**
   - Mute all except mentions
   - Mute all except from specific users
