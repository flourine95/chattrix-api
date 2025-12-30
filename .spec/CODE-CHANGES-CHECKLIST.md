# Checklist Thay Đổi Code Sau Refactor

## ✅ Entities Đã Refactor

- [x] `User.java` - Xóa field `online`
- [x] `Conversation.java` - Thêm `metadata` JSONB
- [x] `ConversationParticipant.java` - Gộp ConversationSettings, thêm `unreadMarkerId`
- [x] `Message.java` - Thêm `metadata` JSONB, xóa `poll_id`, `event_id`
- [x] `Call.java` - Comment về CallHistory logic
- [x] `UserToken.java` - Entity mới (VERIFY + RESET)
- [x] `TokenType.java` - Enum mới

## ✅ Services Đã Tạo

- [x] `OnlineStatusCache.java` - Caffeine cache cho online status

## ✅ Repositories Đã Tạo

- [x] `UserTokenRepository.java` - Repository cho UserToken

## ❌ Repositories Cần XÓA

Sau khi cập nhật tất cả services, xóa các repository này:

- [ ] `GroupInviteLinkRepository.java`
- [ ] `ConversationSettingsRepository.java`
- [ ] `MessageReadReceiptRepository.java`
- [ ] `PinnedMessageRepository.java`
- [ ] `PollRepository.java`
- [ ] `PollOptionRepository.java`
- [ ] `PollVoteRepository.java`
- [ ] `EventRepository.java`
- [ ] `EventRsvpRepository.java`
- [ ] `CallHistoryRepository.java`
- [ ] `VerificationTokenRepository.java`
- [ ] `PasswordResetTokenRepository.java`

## 📝 Services Cần CẬP NHẬT

### 1. UserService
- [ ] Inject `OnlineStatusCache`
- [ ] Thay thế `user.setOnline()` bằng `onlineStatusCache.markOnline(userId)`
- [ ] Thay thế `user.isOnline()` bằng `onlineStatusCache.isOnline(userId)`
- [ ] Cập nhật `lastSeen` định kỳ (không phải mỗi heartbeat)

**Ví dụ:**
```java
@Inject
private OnlineStatusCache onlineStatusCache;

public void updateHeartbeat(Long userId) {
    onlineStatusCache.markOnline(userId);
    // Chỉ cập nhật DB mỗi 5 phút
}

public boolean isUserOnline(Long userId) {
    return onlineStatusCache.isOnline(userId);
}
```

---

### 2. AuthService
- [ ] Thay `VerificationTokenRepository` bằng `UserTokenRepository`
- [ ] Thay `PasswordResetTokenRepository` bằng `UserTokenRepository`
- [ ] Sử dụng `TokenType.VERIFY` và `TokenType.RESET`

**Ví dụ:**
```java
// Tạo verification token
UserToken token = UserToken.builder()
    .user(user)
    .token(generateCode())
    .type(TokenType.VERIFY)
    .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
    .build();
userTokenRepository.save(token);

// Verify token
Optional<UserToken> tokenOpt = userTokenRepository.findByTokenAndType(code, TokenType.VERIFY);
if (tokenOpt.isPresent() && tokenOpt.get().isValid()) {
    tokenOpt.get().markAsUsed();
    userTokenRepository.save(tokenOpt.get());
}
```

---

### 3. ConversationService
- [ ] Xóa inject `GroupInviteLinkRepository`
- [ ] Tạo invite link: Lưu vào `conversation.metadata`
- [ ] Validate invite link: Đọc từ `conversation.metadata`
- [ ] Revoke invite link: Cập nhật `conversation.metadata`

**Ví dụ:**
```java
public String createInviteLink(Long conversationId, Long userId, CreateInviteLinkRequest request) {
    Conversation conversation = findById(conversationId);
    
    String token = generateToken();
    Map<String, Object> inviteLink = new HashMap<>();
    inviteLink.put("token", token);
    inviteLink.put("expiresAt", request.getExpiresAt() != null ? request.getExpiresAt().getEpochSecond() : null);
    inviteLink.put("maxUses", request.getMaxUses());
    inviteLink.put("currentUses", 0);
    inviteLink.put("createdBy", userId);
    inviteLink.put("createdAt", Instant.now().getEpochSecond());
    inviteLink.put("revoked", false);
    
    conversation.getMetadata().put("inviteLink", inviteLink);
    conversationRepository.save(conversation);
    
    return token;
}

public void joinViaInviteLink(String token, Long userId) {
    Conversation conversation = conversationRepository.findByInviteToken(token)
        .orElseThrow(() -> new ResourceNotFoundException("Invalid invite link"));
    
    Map<String, Object> inviteLink = (Map<String, Object>) conversation.getMetadata().get("inviteLink");
    
    // Validate
    if ((Boolean) inviteLink.get("revoked")) {
        throw new BusinessException("Invite link has been revoked");
    }
    
    Long expiresAt = (Long) inviteLink.get("expiresAt");
    if (expiresAt != null && Instant.now().getEpochSecond() > expiresAt) {
        throw new BusinessException("Invite link has expired");
    }
    
    Integer maxUses = (Integer) inviteLink.get("maxUses");
    Integer currentUses = (Integer) inviteLink.get("currentUses");
    if (maxUses != null && currentUses >= maxUses) {
        throw new BusinessException("Invite link has reached maximum uses");
    }
    
    // Add member
    addMember(conversation.getId(), userId);
    
    // Increment currentUses
    inviteLink.put("currentUses", currentUses + 1);
    conversationRepository.save(conversation);
}
```

---

### 4. ConversationParticipantService / ConversationSettingsService
- [ ] Xóa inject `ConversationSettingsRepository`
- [ ] Gộp tất cả logic settings vào `ConversationParticipant`
- [ ] Cập nhật mute/archive/pin/theme trực tiếp trên participant

**Ví dụ:**
```java
public void updateSettings(Long userId, Long conversationId, UpdateConversationSettingsRequest request) {
    ConversationParticipant participant = findParticipant(userId, conversationId);
    
    if (request.getMuted() != null) {
        participant.setMuted(request.getMuted());
        participant.setMutedAt(request.getMuted() ? Instant.now() : null);
        participant.setMutedUntil(request.getMutedUntil());
    }
    
    if (request.getArchived() != null) {
        participant.setArchived(request.getArchived());
        participant.setArchivedAt(request.getArchived() ? Instant.now() : null);
    }
    
    if (request.getPinned() != null) {
        participant.setPinned(request.getPinned());
        participant.setPinnedAt(request.getPinned() ? Instant.now() : null);
    }
    
    if (request.getTheme() != null) {
        participant.setTheme(request.getTheme());
    }
    
    if (request.getCustomNickname() != null) {
        participant.setCustomNickname(request.getCustomNickname());
    }
    
    conversationParticipantRepository.save(participant);
}
```

---

### 5. MessageService - Poll
- [ ] Xóa inject `PollRepository`, `PollOptionRepository`, `PollVoteRepository`
- [ ] Tạo poll: Lưu vào `message.metadata`
- [ ] Vote poll: Cập nhật `voterIds` trong `message.metadata`
- [ ] Get poll results: Đọc từ `message.metadata`

**Ví dụ:**
```java
public MessageResponse createPoll(Long conversationId, Long userId, CreatePollRequest request) {
    Message message = Message.builder()
        .conversation(findConversation(conversationId))
        .sender(findUser(userId))
        .content(request.getQuestion())
        .type(Message.MessageType.POLL)
        .build();
    
    // Tạo poll metadata
    Map<String, Object> pollData = new HashMap<>();
    pollData.put("question", request.getQuestion());
    pollData.put("allowMultipleVotes", request.getAllowMultipleVotes());
    pollData.put("expiresAt", request.getExpiresAt() != null ? request.getExpiresAt().getEpochSecond() : null);
    pollData.put("closed", false);
    pollData.put("createdAt", Instant.now().getEpochSecond());
    
    List<Map<String, Object>> options = new ArrayList<>();
    for (int i = 0; i < request.getOptions().size(); i++) {
        Map<String, Object> option = new HashMap<>();
        option.put("text", request.getOptions().get(i));
        option.put("order", i);
        option.put("voterIds", new ArrayList<Long>());
        options.add(option);
    }
    pollData.put("options", options);
    
    message.getMetadata().put("poll", pollData);
    messageRepository.save(message);
    
    return messageMapper.toResponse(message);
}

public void votePoll(Long messageId, Long userId, VotePollRequest request) {
    Message message = findById(messageId);
    
    if (message.getType() != Message.MessageType.POLL) {
        throw new BusinessException("Message is not a poll");
    }
    
    Map<String, Object> pollData = (Map<String, Object>) message.getMetadata().get("poll");
    
    // Check if closed
    if ((Boolean) pollData.get("closed")) {
        throw new BusinessException("Poll is closed");
    }
    
    // Check if expired
    Long expiresAt = (Long) pollData.get("expiresAt");
    if (expiresAt != null && Instant.now().getEpochSecond() > expiresAt) {
        throw new BusinessException("Poll has expired");
    }
    
    List<Map<String, Object>> options = (List<Map<String, Object>>) pollData.get("options");
    boolean allowMultiple = (Boolean) pollData.get("allowMultipleVotes");
    
    // Remove previous votes if not allowing multiple
    if (!allowMultiple) {
        for (Map<String, Object> option : options) {
            List<Long> voterIds = (List<Long>) option.get("voterIds");
            voterIds.remove(userId);
        }
    }
    
    // Add new vote
    for (Map<String, Object> option : options) {
        if (option.get("order").equals(request.getOptionOrder())) {
            List<Long> voterIds = (List<Long>) option.get("voterIds");
            if (!voterIds.contains(userId)) {
                voterIds.add(userId);
            }
            break;
        }
    }
    
    messageRepository.save(message);
}
```

---

### 6. MessageService - Event
- [ ] Xóa inject `EventRepository`, `EventRsvpRepository`
- [ ] Tạo event: Lưu vào `message.metadata`
- [ ] RSVP event: Cập nhật `rsvps` trong `message.metadata`
- [ ] Get event details: Đọc từ `message.metadata`

**Ví dụ:**
```java
public MessageResponse createEvent(Long conversationId, Long userId, CreateEventRequest request) {
    Message message = Message.builder()
        .conversation(findConversation(conversationId))
        .sender(findUser(userId))
        .content(request.getTitle())
        .type(Message.MessageType.EVENT)
        .build();
    
    // Tạo event metadata
    Map<String, Object> eventData = new HashMap<>();
    eventData.put("title", request.getTitle());
    eventData.put("description", request.getDescription());
    eventData.put("startTime", request.getStartTime().getEpochSecond());
    eventData.put("endTime", request.getEndTime() != null ? request.getEndTime().getEpochSecond() : null);
    eventData.put("location", request.getLocation());
    eventData.put("createdAt", Instant.now().getEpochSecond());
    eventData.put("rsvps", new ArrayList<>());
    
    message.getMetadata().put("event", eventData);
    messageRepository.save(message);
    
    return messageMapper.toResponse(message);
}

public void rsvpEvent(Long messageId, Long userId, EventRsvpRequest request) {
    Message message = findById(messageId);
    
    if (message.getType() != Message.MessageType.EVENT) {
        throw new BusinessException("Message is not an event");
    }
    
    Map<String, Object> eventData = (Map<String, Object>) message.getMetadata().get("event");
    List<Map<String, Object>> rsvps = (List<Map<String, Object>>) eventData.get("rsvps");
    
    // Remove existing RSVP
    rsvps.removeIf(rsvp -> rsvp.get("userId").equals(userId));
    
    // Add new RSVP
    Map<String, Object> newRsvp = new HashMap<>();
    newRsvp.put("userId", userId);
    newRsvp.put("status", request.getStatus().name());
    newRsvp.put("createdAt", Instant.now().getEpochSecond());
    rsvps.add(newRsvp);
    
    messageRepository.save(message);
}
```

---

### 7. MessageService - Pinned Messages
- [ ] Xóa inject `PinnedMessageRepository`
- [ ] Pin message: Cập nhật `message.pinned = true`
- [ ] Unpin message: Cập nhật `message.pinned = false`
- [ ] Get pinned messages: Query `WHERE pinned = true`

**Ví dụ:**
```java
public void pinMessage(Long messageId, Long userId) {
    Message message = findById(messageId);
    
    if (message.isPinned()) {
        throw new BusinessException("Message is already pinned");
    }
    
    message.setPinned(true);
    message.setPinnedAt(Instant.now());
    message.setPinnedBy(findUser(userId));
    
    messageRepository.save(message);
}

public void unpinMessage(Long messageId) {
    Message message = findById(messageId);
    
    if (!message.isPinned()) {
        throw new BusinessException("Message is not pinned");
    }
    
    message.setPinned(false);
    message.setPinnedAt(null);
    message.setPinnedBy(null);
    
    messageRepository.save(message);
}

public List<MessageResponse> getPinnedMessages(Long conversationId) {
    List<Message> messages = messageRepository.findPinnedByConversationId(conversationId);
    return messages.stream()
        .map(messageMapper::toResponse)
        .collect(Collectors.toList());
}
```

---

### 8. MessageService - Read Receipts & Unread Count
- [ ] Xóa inject `MessageReadReceiptRepository`
- [ ] Mark as read: Cập nhật `participant.lastReadMessageId`
- [ ] Mark as unread: Cập nhật `participant.unreadMarkerId`
- [ ] Calculate unread: Sử dụng `getEffectiveLastReadMessageId()`

**Ví dụ:**
```java
public void markAsRead(Long userId, Long conversationId, Long messageId) {
    ConversationParticipant participant = findParticipant(userId, conversationId);
    participant.setLastReadMessageId(messageId);
    participant.setLastReadAt(Instant.now());
    participant.setUnreadMarkerId(null); // Clear unread marker
    conversationParticipantRepository.save(participant);
}

public void markAsUnread(Long userId, Long conversationId, Long messageId) {
    ConversationParticipant participant = findParticipant(userId, conversationId);
    participant.setUnreadMarkerId(messageId);
    conversationParticipantRepository.save(participant);
}

public int getUnreadCount(Long userId, Long conversationId) {
    ConversationParticipant participant = findParticipant(userId, conversationId);
    Long effectiveLastReadId = participant.getEffectiveLastReadMessageId();
    
    if (effectiveLastReadId == null) {
        return messageRepository.countByConversationId(conversationId);
    }
    
    return messageRepository.countUnreadMessages(conversationId, effectiveLastReadId);
}
```

---

### 9. CallService
- [ ] Xóa inject `CallHistoryRepository`
- [ ] End call: Sử dụng `call.end(status)` để cập nhật status và duration
- [ ] Get call history: Query từ `Call` entity với status ENDED/MISSED/REJECTED

**Ví dụ:**
```java
public void endCall(String callId, Long userId, EndCallRequest request) {
    Call call = findById(callId);
    
    if (!call.isParticipant(userId)) {
        throw new UnauthorizedException("Not a participant");
    }
    
    call.end(request.getStatus());
    callRepository.save(call);
    
    // Không cần tạo CallHistory riêng
    // Service layer có thể tạo response cho từng participant
}

public List<CallResponse> getCallHistory(Long userId) {
    // Query calls where user is caller or participant
    List<Call> calls = callRepository.findByUserIdAndFinished(userId);
    return calls.stream()
        .map(callMapper::toResponse)
        .collect(Collectors.toList());
}
```

---

## 📝 Repositories Cần CẬP NHẬT

### MessageRepository
- [ ] Thêm method: `findPinnedByConversationId(Long conversationId)`
- [ ] Thêm method: `countUnreadMessages(Long conversationId, Long lastReadMessageId)`

**Ví dụ:**
```java
public List<Message> findPinnedByConversationId(Long conversationId) {
    return entityManager.createQuery(
        "SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.pinned = true " +
        "ORDER BY m.pinnedAt DESC", Message.class)
        .setParameter("conversationId", conversationId)
        .getResultList();
}

public int countUnreadMessages(Long conversationId, Long lastReadMessageId) {
    return entityManager.createQuery(
        "SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
        "AND m.id > :lastReadMessageId", Long.class)
        .setParameter("conversationId", conversationId)
        .setParameter("lastReadMessageId", lastReadMessageId)
        .getSingleResult()
        .intValue();
}
```

### ConversationRepository
- [ ] Thêm method: `findByInviteToken(String token)` - Query JSONB

**Ví dụ:**
```java
public Optional<Conversation> findByInviteToken(String token) {
    try {
        Conversation conversation = entityManager.createQuery(
            "SELECT c FROM Conversation c WHERE c.metadata -> 'inviteLink' ->> 'token' = :token", 
            Conversation.class)
            .setParameter("token", token)
            .getSingleResult();
        return Optional.of(conversation);
    } catch (NoResultException e) {
        return Optional.empty();
    }
}
```

### CallRepository
- [ ] Thêm method: `findByUserIdAndFinished(Long userId)`

**Ví dụ:**
```java
public List<Call> findByUserIdAndFinished(Long userId) {
    return entityManager.createQuery(
        "SELECT c FROM Call c WHERE (c.callerId = :userId OR " +
        "EXISTS (SELECT p FROM CallParticipant p WHERE p.call = c AND p.userId = :userId)) " +
        "AND c.status IN ('ENDED', 'MISSED', 'REJECTED') " +
        "ORDER BY c.createdAt DESC", Call.class)
        .setParameter("userId", userId)
        .getResultList();
}
```

---

## 📝 Resources Cần CẬP NHẬT

Tất cả resources sử dụng các services đã refactor cần kiểm tra lại:

- [ ] `UserResource.java` - Online status endpoints
- [ ] `AuthResource.java` - Verify/reset password endpoints
- [ ] `ConversationResource.java` - Invite link endpoints
- [ ] `MessageResource.java` - Poll, Event, Pin, Read receipts endpoints
- [ ] `CallResource.java` - Call history endpoints

---

## 📝 Mappers Cần CẬP NHẬT

- [ ] `MessageMapper.java` - Map metadata cho Poll/Event
- [ ] `ConversationMapper.java` - Map metadata cho InviteLink
- [ ] `ConversationParticipantMapper.java` - Map settings fields
- [ ] `UserMapper.java` - Xóa online field mapping

---

## 📝 Responses Cần CẬP NHẬT

- [ ] `UserResponse.java` / `UserStatusResponse.java` - Xóa `online` field
- [ ] `ConversationSettingsResponse.java` - Có thể xóa hoặc gộp vào `ConversationMemberResponse`
- [ ] `PollResponse.java` - Đọc từ metadata thay vì entity
- [ ] `EventResponse.java` - Đọc từ metadata thay vì entity
- [ ] `InviteLinkResponse.java` - Đọc từ metadata thay vì entity

---

## 🧪 Testing

Sau khi cập nhật code:

1. [ ] Chạy migration: `migration-refactor.sql`
2. [ ] Compile: `mvn clean compile`
3. [ ] Build: `docker compose up -d --build`
4. [ ] Test từng endpoint đã thay đổi
5. [ ] Kiểm tra logs: `docker compose logs -f api`
6. [ ] Test performance với JSONB queries

---

## 📊 Performance Monitoring

Sau khi deploy:

- [ ] Monitor Caffeine Cache hit rate
- [ ] Monitor JSONB query performance
- [ ] Check database size reduction
- [ ] Verify GIN indexes được sử dụng

---

## 🔄 Rollback Plan

Nếu có vấn đề:

1. Restore database backup
2. Revert code changes
3. Rebuild application

```bash
docker compose exec postgres psql -U postgres -d chattrix < backup_before_refactor.sql
git checkout <commit-before-refactor>
docker compose up -d --build
```
