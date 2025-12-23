# LazyInitializationException Fix - Reply Message

## Vấn Đề

Khi reply message qua WebSocket, backend throw `LazyInitializationException`:

```
org.hibernate.LazyInitializationException: Could not initialize proxy [com.chattrix.api.entities.User#1] - no session
    at com.chattrix.api.mappers.MessageMapperImpl.messageSenderUsername(MessageMapperImpl.java:176)
    at com.chattrix.api.mappers.MessageMapperImpl.toReplyMessageResponse(MessageMapperImpl.java:125)
    at com.chattrix.api.mappers.WebSocketMapperImpl.toOutgoingMessageResponse(WebSocketMapperImpl.java:39)
    at com.chattrix.api.websocket.handlers.chat.ChatMessageHandler.processChatMessage(ChatMessageHandler.java:159)
```

### Root Cause

**File:** `ChatMessageHandler.java` (line 99)

```java
// ❌ BAD - findByIdSimple() không fetch sender
replyToMessage = messageRepository.findByIdSimple(chatMessageDto.getReplyToMessageId())
    .orElseThrow(() -> new IllegalArgumentException("Reply to message not found"));
```

**Vấn đề:**
1. `findByIdSimple()` chỉ fetch `Message` entity, không fetch relationships
2. `replyToMessage.sender` là lazy proxy (chưa được load)
3. Khi mapper cố access `replyToMessage.sender.getUsername()`, session đã đóng
4. → `LazyInitializationException`

### Flow Lỗi

```
1. WebSocket nhận message với replyToMessageId
2. ChatMessageHandler.processChatMessage() được gọi
3. Fetch replyToMessage bằng findByIdSimple() ❌
   → sender là lazy proxy, chưa được load
4. Save message mới với replyToMessage
5. Transaction commit, session đóng
6. webSocketMapper.toOutgoingMessageResponse(newMessage) được gọi
7. Mapper cố access replyToMessage.sender.getUsername()
8. → LazyInitializationException (session đã đóng)
```

---

## Giải Pháp

### Fix: Sử Dụng `findById()` Thay Vì `findByIdSimple()`

**File:** `ChatMessageHandler.java`

```java
// ✅ GOOD - findById() fetch sender với EntityGraph
replyToMessage = messageRepository.findById(chatMessageDto.getReplyToMessageId())
    .orElseThrow(() -> new IllegalArgumentException("Reply to message not found"));
```

### Tại Sao Fix Này Hoạt Động?

**`findById()` implementation:**

```java
public Optional<Message> findById(Long messageId) {
    try {
        // Use EntityGraph to fetch nested relationships
        EntityGraph<?> entityGraph = em.getEntityGraph("Message.withSenderAndConversation");

        Message message = em.createQuery(
                        "SELECT m FROM Message m " +
                                "WHERE m.id = :messageId",
                        Message.class
                )
                .setHint("jakarta.persistence.fetchgraph", entityGraph)
                .setParameter("messageId", messageId)
                .getSingleResult();
        return Optional.of(message);
    } catch (NoResultException e) {
        return Optional.empty();
    }
}
```

**EntityGraph `Message.withSenderAndConversation`:**

```java
@Entity
@Table(name = "messages")
@NamedEntityGraph(
    name = "Message.withSenderAndConversation",
    attributeNodes = {
        @NamedAttributeNode("sender"),
        @NamedAttributeNode("conversation")
    }
)
public class Message {
    // ...
}
```

**Kết quả:**
- ✅ `sender` được fetch EAGER trong cùng query
- ✅ `conversation` cũng được fetch EAGER
- ✅ Không có lazy proxy
- ✅ Không có LazyInitializationException

---

## So Sánh `findById()` vs `findByIdSimple()`

### findById() ✅

```java
public Optional<Message> findById(Long messageId) {
    EntityGraph<?> entityGraph = em.getEntityGraph("Message.withSenderAndConversation");
    
    Message message = em.createQuery(
            "SELECT m FROM Message m WHERE m.id = :messageId",
            Message.class
        )
        .setHint("jakarta.persistence.fetchgraph", entityGraph)
        .setParameter("messageId", messageId)
        .getSingleResult();
    
    return Optional.of(message);
}
```

**SQL Generated:**
```sql
SELECT m.*, s.*, c.*
FROM messages m
LEFT JOIN users s ON m.sender_id = s.id
LEFT JOIN conversations c ON m.conversation_id = c.id
WHERE m.id = ?
```

**Fetches:**
- ✅ Message
- ✅ Sender (EAGER)
- ✅ Conversation (EAGER)

### findByIdSimple() ❌

```java
public Optional<Message> findByIdSimple(Long messageId) {
    try {
        Message message = em.find(Message.class, messageId);
        return Optional.ofNullable(message);
    } catch (Exception e) {
        return Optional.empty();
    }
}
```

**SQL Generated:**
```sql
SELECT m.*
FROM messages m
WHERE m.id = ?
```

**Fetches:**
- ✅ Message
- ❌ Sender (LAZY proxy)
- ❌ Conversation (LAZY proxy)

---

## Khi Nào Dùng Method Nào?

### Dùng `findById()` ✅

**Khi:**
- Cần access relationships (sender, conversation, replyToMessage)
- Sẽ serialize entity thành DTO/JSON
- Sẽ pass entity qua transaction boundary
- Sẽ broadcast qua WebSocket

**Examples:**
```java
// ✅ Reply message - cần sender info
Message replyToMessage = messageRepository.findById(replyToMessageId)
    .orElseThrow();

// ✅ Broadcast message - cần sender info
Message message = messageRepository.findById(messageId)
    .orElseThrow();
OutgoingMessageDto dto = mapper.toOutgoingMessageResponse(message);
```

### Dùng `findByIdSimple()` ✅

**Khi:**
- Chỉ cần check existence
- Chỉ cần ID hoặc simple fields
- Không access relationships
- Trong cùng transaction

**Examples:**
```java
// ✅ Check if message exists
boolean exists = messageRepository.findByIdSimple(messageId).isPresent();

// ✅ Get message ID only
Long messageId = messageRepository.findByIdSimple(id)
    .map(Message::getId)
    .orElseThrow();

// ✅ Update simple field (trong transaction)
Message message = messageRepository.findByIdSimple(id).orElseThrow();
message.setContent("Updated");
messageRepository.save(message);
```

---

## Testing

### Test 1: Reply Message Qua WebSocket

```javascript
// Client code
const ws = new WebSocket('ws://localhost:8080/ws/chat');

ws.onopen = () => {
  // Send reply message
  ws.send(JSON.stringify({
    type: 'chat.message',
    data: {
      conversationId: 1,
      content: 'This is a reply',
      type: 'TEXT',
      replyToMessageId: 123  // Reply to existing message
    }
  }));
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
  
  // ✅ Should receive message with replyToMessage populated
  if (message.type === 'chat.message') {
    console.log('Reply to:', message.data.replyToMessage);
    console.log('Reply sender:', message.data.replyToMessage.senderUsername);
  }
};
```

### Test 2: Verify No LazyInitializationException

```bash
# Monitor logs
docker compose logs -f api

# Send reply message from client
# ✅ Should NOT see LazyInitializationException
# ✅ Should see successful message broadcast
```

---

## Best Practices

### 1. Always Use EntityGraph for DTOs

```java
// ❌ BAD - Will cause LazyInitializationException
Message message = em.find(Message.class, id);
MessageResponse dto = mapper.toResponse(message);

// ✅ GOOD - Fetch with EntityGraph
EntityGraph<?> graph = em.getEntityGraph("Message.withSenderAndReply");
Message message = em.createQuery("SELECT m FROM Message m WHERE m.id = :id", Message.class)
    .setHint("jakarta.persistence.fetchgraph", graph)
    .setParameter("id", id)
    .getSingleResult();
MessageResponse dto = mapper.toResponse(message);
```

### 2. Define EntityGraphs for Common Use Cases

```java
@Entity
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "Message.withSenderAndConversation",
        attributeNodes = {
            @NamedAttributeNode("sender"),
            @NamedAttributeNode("conversation")
        }
    ),
    @NamedEntityGraph(
        name = "Message.withSenderAndReply",
        attributeNodes = {
            @NamedAttributeNode("sender"),
            @NamedAttributeNode(value = "replyToMessage", subgraph = "replySubgraph")
        },
        subgraphs = {
            @NamedSubgraph(
                name = "replySubgraph",
                attributeNodes = {
                    @NamedAttributeNode("sender")
                }
            )
        }
    )
})
public class Message {
    // ...
}
```

### 3. Use @Transactional for WebSocket Handlers

```java
@ApplicationScoped
public class ChatMessageHandler implements MessageHandler {
    
    @Transactional  // ✅ Ensure transaction is active
    public void handle(WebSocketMessage<?> message, Long userId) {
        // Process message
        // All lazy relationships will be accessible
    }
}
```

---

## Summary

### Problem
- `LazyInitializationException` when replying to messages via WebSocket
- Caused by using `findByIdSimple()` which doesn't fetch relationships

### Solution
- Changed `findByIdSimple()` to `findById()` in `ChatMessageHandler.java`
- `findById()` uses EntityGraph to fetch `sender` and `conversation` eagerly
- No more lazy proxies, no more exceptions

### Files Modified
- `src/main/java/com/chattrix/api/websocket/handlers/chat/ChatMessageHandler.java`
  - Line 99: Changed `findByIdSimple()` to `findById()`

### Status
✅ **DEPLOYED** - Fix applied and tested successfully
✅ **VERIFIED** - Reply messages now work without LazyInitializationException
