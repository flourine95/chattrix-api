# Hướng Dẫn Refactor Hệ Thống Chattrix Backend

## Tổng Quan

Refactor này giảm số lượng bảng từ **~25 bảng xuống 7 bảng chính** bằng cách:
- Sử dụng **JSONB metadata** để phi chuẩn hóa dữ liệu
- Gộp các bảng phụ vào bảng chính
- Sử dụng **Caffeine Cache** cho dữ liệu thời gian thực (online status)
- Tối ưu hiệu năng với ít JOIN hơn

## Các Thay Đổi Chính

### 1. User Entity
**Thay đổi:**
- ❌ Xóa: `online` (boolean)
- ✅ Giữ: `lastSeen` (Instant)

**Lý do:** Trạng thái online được quản lý hoàn toàn bằng Caffeine Cache (UserId -> LastHeartbeat)

**Code:**
```java
// TRƯỚC
@Column(name = "online", nullable = false)
private boolean online = false;

@Column(name = "last_seen")
private Instant lastSeen;

// SAU
@Column(name = "last_seen")
private Instant lastSeen;
```

---

### 2. Conversation Entity
**Thay đổi:**
- ✅ Thêm: `metadata` (JSONB)
- ❌ Xóa bảng: `GroupInviteLink`

**Metadata chứa:**
```json
{
  "inviteLink": {
    "token": "abc123",
    "expiresAt": 1735574400,
    "maxUses": 100,
    "currentUses": 45,
    "createdBy": 123,
    "createdAt": 1735488000,
    "revoked": false,
    "revokedAt": null,
    "revokedBy": null
  }
}
```

**Code:**
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "metadata", columnDefinition = "jsonb")
@Builder.Default
private Map<String, Object> metadata = new HashMap<>();
```

---

### 3. ConversationParticipant Entity
**Thay đổi:**
- ✅ Gộp toàn bộ `ConversationSettings` vào đây
- ✅ Thêm: `unreadMarkerId` (Long) - cho tính năng "Đánh dấu chưa đọc"
- ❌ Xóa bảng: `ConversationSettings`, `MessageReadReceipt`

**Các field mới:**
```java
// Logic đọc tin nhắn
@Column(name = "last_read_message_id")
private Long lastReadMessageId;  // Tin nhắn thực sự đã đọc

@Column(name = "unread_marker_id")
private Long unreadMarkerId;  // Mốc "Đánh dấu chưa đọc"

// Từ ConversationSettings
@Column(name = "archived")
private boolean archived = false;

@Column(name = "pinned")
private boolean pinned = false;

@Column(name = "theme", length = 50)
private String theme;

@Column(name = "custom_nickname", length = 100)
private String customNickname;

@Column(name = "notifications_enabled")
private boolean notificationsEnabled = true;
```

**Logic đọc tin:**
```java
/**
 * Tính unread count từ lastReadMessageId hoặc unreadMarkerId
 */
public Long getEffectiveLastReadMessageId() {
    return unreadMarkerId != null ? unreadMarkerId : lastReadMessageId;
}
```

---

### 4. Message Entity
**Thay đổi:**
- ✅ Thêm: `metadata` (JSONB)
- ❌ Xóa: `poll_id`, `event_id` foreign keys
- ❌ Xóa bảng: `Poll`, `PollOption`, `PollVote`, `Event`, `EventRsvp`, `PinnedMessage`

**Metadata cho Poll:**
```json
{
  "poll": {
    "question": "Chọn địa điểm họp?",
    "allowMultipleVotes": false,
    "expiresAt": 1735660800,
    "closed": false,
    "createdAt": 1735488000,
    "options": [
      {
        "text": "Quán cafe A",
        "order": 0,
        "voterIds": [1, 2, 5, 8]
      },
      {
        "text": "Nhà hàng B",
        "order": 1,
        "voterIds": [3, 4, 6, 7, 9]
      }
    ]
  }
}
```

**Metadata cho Event:**
```json
{
  "event": {
    "title": "Team Building 2025",
    "description": "Sự kiện team building cuối năm",
    "startTime": 1735660800,
    "endTime": 1735747200,
    "location": "Vũng Tàu Beach Resort",
    "createdAt": 1735488000,
    "rsvps": [
      {"userId": 1, "status": "GOING", "createdAt": 1735490000},
      {"userId": 2, "status": "MAYBE", "createdAt": 1735491000},
      {"userId": 3, "status": "NOT_GOING", "createdAt": 1735492000}
    ]
  }
}
```

**Code:**
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "metadata", columnDefinition = "jsonb")
private Map<String, Object> metadata = new HashMap<>();
```

**Pinned Message:**
- Sử dụng field `pinned` (boolean) có sẵn trong Message
- Không cần bảng `PinnedMessage` riêng

---

### 5. Call Entity
**Thay đổi:**
- ❌ Xóa bảng: `CallHistory`
- ✅ Logic: Khi kết thúc cuộc gọi, cập nhật `status` và `durationSeconds` ngay trên bảng `Call`

**Lý do:** Service layer sẽ tạo lịch sử cho từng participant dựa trên thông tin Call

**Code:**
```java
public void end(CallStatus endStatus) {
    if (isFinished()) return;
    
    this.status = endStatus;  // ENDED, MISSED, REJECTED
    this.endTime = Instant.now();
    
    if (this.startTime != null && this.endTime != null) {
        this.durationSeconds = (int) Duration.between(this.startTime, this.endTime).getSeconds();
    } else {
        this.durationSeconds = 0;
    }
}
```

---

### 6. UserToken Entity (MỚI)
**Thay đổi:**
- ✅ Entity mới gộp: `VerificationToken` + `PasswordResetToken`
- ✅ Thêm: `type` (Enum: VERIFY, RESET)
- ❌ Xóa bảng: `VerificationToken`, `PasswordResetToken`

**Code:**
```java
@Entity
@Table(name = "user_tokens")
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class UserToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenType type;  // VERIFY hoặc RESET

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Builder.Default
    @Column(name = "used", nullable = false)
    private boolean used = false;

    public boolean isValid() {
        return !used && !isExpired();
    }
}
```

---

## Migration Steps

### Bước 1: Backup Database
```bash
docker compose exec postgres pg_dump -U postgres chattrix > backup_before_refactor.sql
```

### Bước 2: Chạy Migration Script
```bash
docker compose exec postgres psql -U postgres -d chattrix -f /path/to/migration-refactor.sql
```

### Bước 3: Rebuild Application
```bash
mvn clean compile
docker compose up -d --build
```

### Bước 4: Kiểm tra Logs
```bash
docker compose logs -f api
```

---

## Cập Nhật Code

### Services Cần Cập Nhật

#### 1. **UserService** - Online Status
```java
// TRƯỚC: Cập nhật online status trong DB
user.setOnline(true);
userRepository.save(user);

// SAU: Sử dụng Caffeine Cache
@Inject
private OnlineStatusCache onlineStatusCache;

public void updateOnlineStatus(Long userId) {
    onlineStatusCache.markOnline(userId);
    // Chỉ cập nhật lastSeen trong DB định kỳ
}

public boolean isUserOnline(Long userId) {
    return onlineStatusCache.isOnline(userId);
}
```

#### 2. **ConversationService** - Invite Link
```java
// TRƯỚC: Tạo entity GroupInviteLink
GroupInviteLink link = GroupInviteLink.builder()
    .conversation(conversation)
    .token(token)
    .expiresAt(expiresAt)
    .build();
groupInviteLinkRepository.save(link);

// SAU: Lưu vào metadata
Map<String, Object> inviteLink = new HashMap<>();
inviteLink.put("token", token);
inviteLink.put("expiresAt", expiresAt.getEpochSecond());
inviteLink.put("maxUses", maxUses);
inviteLink.put("currentUses", 0);
inviteLink.put("createdBy", userId);
inviteLink.put("createdAt", Instant.now().getEpochSecond());
inviteLink.put("revoked", false);

conversation.getMetadata().put("inviteLink", inviteLink);
conversationRepository.save(conversation);
```

#### 3. **ConversationParticipantService** - Settings
```java
// TRƯỚC: Tạo entity ConversationSettings
ConversationSettings settings = ConversationSettings.builder()
    .user(user)
    .conversation(conversation)
    .muted(true)
    .theme("dark")
    .build();
conversationSettingsRepository.save(settings);

// SAU: Cập nhật trực tiếp trên ConversationParticipant
ConversationParticipant participant = findParticipant(userId, conversationId);
participant.setMuted(true);
participant.setTheme("dark");
conversationParticipantRepository.save(participant);
```

#### 4. **MessageService** - Poll
```java
// TRƯỚC: Tạo entities Poll, PollOption, PollVote
Poll poll = Poll.builder()
    .question(question)
    .allowMultipleVotes(false)
    .build();
pollRepository.save(poll);

// SAU: Lưu vào metadata
Map<String, Object> pollData = new HashMap<>();
pollData.put("question", question);
pollData.put("allowMultipleVotes", false);
pollData.put("expiresAt", expiresAt.getEpochSecond());
pollData.put("closed", false);
pollData.put("createdAt", Instant.now().getEpochSecond());

List<Map<String, Object>> options = new ArrayList<>();
for (int i = 0; i < optionTexts.size(); i++) {
    Map<String, Object> option = new HashMap<>();
    option.put("text", optionTexts.get(i));
    option.put("order", i);
    option.put("voterIds", new ArrayList<Long>());
    options.add(option);
}
pollData.put("options", options);

message.getMetadata().put("poll", pollData);
messageRepository.save(message);
```

#### 5. **MessageService** - Vote Poll
```java
// TRƯỚC: Tạo entity PollVote
PollVote vote = PollVote.builder()
    .poll(poll)
    .pollOption(option)
    .user(user)
    .build();
pollVoteRepository.save(vote);

// SAU: Cập nhật voterIds trong metadata
Map<String, Object> pollData = (Map<String, Object>) message.getMetadata().get("poll");
List<Map<String, Object>> options = (List<Map<String, Object>>) pollData.get("options");

for (Map<String, Object> option : options) {
    if (option.get("order").equals(optionOrder)) {
        List<Long> voterIds = (List<Long>) option.get("voterIds");
        if (!voterIds.contains(userId)) {
            voterIds.add(userId);
        }
        break;
    }
}

messageRepository.save(message);
```

#### 6. **MessageService** - Event
```java
// TRƯỚC: Tạo entities Event, EventRsvp
Event event = Event.builder()
    .title(title)
    .startTime(startTime)
    .location(location)
    .build();
eventRepository.save(event);

// SAU: Lưu vào metadata
Map<String, Object> eventData = new HashMap<>();
eventData.put("title", title);
eventData.put("description", description);
eventData.put("startTime", startTime.getEpochSecond());
eventData.put("endTime", endTime.getEpochSecond());
eventData.put("location", location);
eventData.put("createdAt", Instant.now().getEpochSecond());
eventData.put("rsvps", new ArrayList<>());

message.getMetadata().put("event", eventData);
messageRepository.save(message);
```

#### 7. **MessageService** - Mark as Unread
```java
// MỚI: Đánh dấu chưa đọc
public void markAsUnread(Long userId, Long conversationId, Long messageId) {
    ConversationParticipant participant = findParticipant(userId, conversationId);
    participant.setUnreadMarkerId(messageId);
    conversationParticipantRepository.save(participant);
}

// Tính unread count
public int calculateUnreadCount(Long userId, Long conversationId) {
    ConversationParticipant participant = findParticipant(userId, conversationId);
    Long effectiveLastReadId = participant.getEffectiveLastReadMessageId();
    
    return messageRepository.countUnreadMessages(conversationId, effectiveLastReadId);
}
```

#### 8. **CallService** - Call History
```java
// TRƯỚC: Tạo entity CallHistory khi kết thúc
CallHistory history = CallHistory.builder()
    .userId(userId)
    .callId(callId)
    .status(status)
    .durationSeconds(durationSeconds)
    .build();
callHistoryRepository.save(history);

// SAU: Sử dụng thông tin từ Call entity
public void endCall(String callId, CallStatus endStatus) {
    Call call = callRepository.findById(callId)
        .orElseThrow(() -> new ResourceNotFoundException("Call not found"));
    
    call.end(endStatus);  // Cập nhật status và durationSeconds
    callRepository.save(call);
    
    // Service layer có thể tạo response cho từng participant
    // dựa trên thông tin từ call.getStatus(), call.getDurationSeconds()
}
```

#### 9. **AuthService** - User Tokens
```java
// TRƯỚC: Tạo VerificationToken
VerificationToken token = new VerificationToken();
token.setUser(user);
token.setToken(code);
token.setType(TokenType.VERIFY);
verificationTokenRepository.save(token);

// SAU: Sử dụng UserToken
UserToken token = UserToken.builder()
    .user(user)
    .token(code)
    .type(TokenType.VERIFY)  // hoặc TokenType.RESET
    .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
    .build();
userTokenRepository.save(token);
```

---

## Repositories Cần Xóa

```java
// ❌ XÓA các repository này:
- GroupInviteLinkRepository
- ConversationSettingsRepository
- MessageReadReceiptRepository
- PinnedMessageRepository
- PollRepository
- PollOptionRepository
- PollVoteRepository
- EventRepository
- EventRsvpRepository
- CallHistoryRepository
- VerificationTokenRepository
- PasswordResetTokenRepository
```

---

## Repositories Cần Tạo Mới

```java
// ✅ TẠO repository mới:
- UserTokenRepository
```

---

## Caffeine Cache Configuration

Tạo `OnlineStatusCache` service:

```java
@ApplicationScoped
public class OnlineStatusCache {
    
    private final Cache<Long, Instant> onlineUsers = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(100_000)
        .build();
    
    public void markOnline(Long userId) {
        onlineUsers.put(userId, Instant.now());
    }
    
    public boolean isOnline(Long userId) {
        Instant lastSeen = onlineUsers.getIfPresent(userId);
        if (lastSeen == null) return false;
        
        // Online nếu heartbeat trong vòng 2 phút
        return Duration.between(lastSeen, Instant.now()).toMinutes() < 2;
    }
    
    public Instant getLastSeen(Long userId) {
        return onlineUsers.getIfPresent(userId);
    }
    
    public void markOffline(Long userId) {
        onlineUsers.invalidate(userId);
    }
}
```

---

## Testing Checklist

- [ ] User online status hoạt động với Caffeine Cache
- [ ] Conversation invite link lưu/đọc từ metadata
- [ ] ConversationParticipant settings (mute, archive, pin, theme)
- [ ] Message unread count với unreadMarkerId
- [ ] Poll tạo/vote từ metadata
- [ ] Event tạo/RSVP từ metadata
- [ ] Pinned messages sử dụng field `pinned`
- [ ] Call history từ Call entity
- [ ] UserToken cho verify và reset password
- [ ] Migration script chạy thành công
- [ ] Tất cả endpoints hoạt động bình thường

---

## Performance Benefits

1. **Giảm JOIN queries**: Ít bảng hơn = ít JOIN hơn
2. **JSONB indexing**: PostgreSQL hỗ trợ GIN index cho JSONB
3. **Caffeine Cache**: Online status không cần query DB
4. **Denormalization**: Truy vấn nhanh hơn với metadata
5. **Ít foreign keys**: Giảm overhead constraint checking

---

## Rollback Plan

Nếu cần rollback:

```bash
# Restore backup
docker compose exec postgres psql -U postgres -d chattrix < backup_before_refactor.sql

# Rebuild với code cũ
git checkout <commit-before-refactor>
docker compose up -d --build
```

---

## Lưu Ý Quan Trọng

1. **JSONB Queries**: Sử dụng PostgreSQL JSONB operators (`->`, `->>`, `@>`, `?`)
2. **Validation**: Validate metadata structure trong service layer
3. **Migration**: Test migration trên staging trước khi production
4. **Indexes**: Tạo GIN indexes cho JSONB columns
5. **Cache**: Monitor Caffeine Cache memory usage

---

## Hỗ Trợ

Nếu gặp vấn đề, kiểm tra:
1. Logs: `docker compose logs -f api`
2. Database: `docker compose exec postgres psql -U postgres -d chattrix`
3. Migration script: Xem `migration-refactor.sql`
