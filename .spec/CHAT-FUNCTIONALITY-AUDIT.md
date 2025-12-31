# Chat Functionality Audit - Post Metadata & Cache Refactor

## Audit Scope
Kiểm tra toàn bộ chat functionality sau khi:
1. Refactor metadata từ separate fields → JSONB
2. Implement cache layer (Caffeine)
3. Refactor WebSocket DTOs và event types

---

## 1. MESSAGE FEATURES

### 1.1 Send Message (WebSocket)
**Path**: `ChatServerEndpoint.onMessage()` → `handleChatMessage()`
- [ ] **Request DTO**: `ChatMessageDto` - validate fields
- [ ] **Entity**: `Message` - check metadata JSONB mapping
- [ ] **Service**: Message creation logic
- [ ] **Repository**: Save message with metadata
- [ ] **Mapper**: `WebSocketMapper.toOutgoingMessageResponse()`
- [ ] **WebSocket**: Broadcast `CHAT_MESSAGE` event
- [ ] **Cache**: Invalidate conversation cache for all participants
- [ ] **Metadata fields**: location, media, linkPreview, voiceNote

**Status**: ⏳ PENDING

---

### 1.2 Get Messages (REST)
**Path**: `GET /api/conversations/{id}/messages`
**Resource**: `MessageResource.getMessages()`
- [ ] **Request**: Query params (page, size)
- [ ] **Response DTO**: `MessageResponse`
- [ ] **Service**: `MessageService.getMessages()`
- [ ] **Repository**: `MessageRepository.findByConversationId()`
- [ ] **Mapper**: `MessageMapper.toResponse()` - check metadata mapping
- [ ] **Cache**: Check `MessageCache.get()` first
- [ ] **Metadata**: Ensure all metadata fields mapped correctly

**Status**: ⏳ PENDING

---

### 1.3 Edit Message (REST)
**Path**: `PUT /api/messages/{id}`
**Resource**: `MessageResource.updateMessage()`
- [ ] **Request DTO**: `UpdateMessageRequest`
- [ ] **Response DTO**: `MessageResponse`
- [ ] **Service**: `MessageService.updateMessage()`
- [ ] **Repository**: Update message
- [ ] **WebSocket**: Broadcast `MESSAGE_UPDATED` with `MessageUpdateEventDto`
- [ ] **Cache**: Update `MessageCache`, invalidate `ConversationCache`
- [ ] **Validation**: Only sender can edit, time limit check

**Status**: ⏳ PENDING

---

### 1.4 Delete Message (REST)
**Path**: `DELETE /api/messages/{id}`
**Resource**: `MessageResource.deleteMessage()`
- [ ] **Service**: `MessageService.deleteMessage()`
- [ ] **Repository**: Delete message
- [ ] **WebSocket**: Broadcast `MESSAGE_DELETED` with `MessageDeleteEventDto`
- [ ] **Cache**: Remove from `MessageCache`, invalidate `ConversationCache`
- [ ] **Validation**: Only sender/admin can delete

**Status**: ⏳ PENDING

---

### 1.5 Reply to Message
**Path**: WebSocket `chat.message` with `replyToMessageId`
- [ ] **Request**: `ChatMessageDto.replyToMessageId`
- [ ] **Entity**: `Message.replyToMessage` relationship
- [ ] **Response**: `MessageResponse.replyToMessage` nested
- [ ] **Mapper**: Map reply relationship correctly
- [ ] **Validation**: Reply must be in same conversation

**Status**: ⏳ PENDING

---

### 1.6 Forward Message
**Path**: `POST /api/messages/{id}/forward`
**Resource**: `MessageResource.forwardMessage()`
- [ ] **Request DTO**: `ForwardMessageRequest`
- [ ] **Service**: `MessageService.forwardMessage()`
- [ ] **Entity**: `Message.originalMessage` relationship
- [ ] **Metadata**: Copy metadata from original
- [ ] **WebSocket**: Broadcast to target conversations
- [ ] **Cache**: Invalidate target conversation caches

**Status**: ⏳ PENDING

---

## 2. RICH MEDIA FEATURES

### 2.1 Send Media Message
**Path**: WebSocket with media metadata
- [ ] **Request**: `ChatMessageDto` with `mediaUrl`, `mediaType`, `thumbnailUrl`
- [ ] **Metadata**: Store in `metadata.media` JSONB field
- [ ] **Response**: `MessageResponse` with media fields
- [ ] **Mapper**: Map media metadata correctly
- [ ] **Types**: IMAGE, VIDEO, AUDIO, DOCUMENT

**Status**: ⏳ PENDING

---

### 2.2 Send Voice Note
**Path**: WebSocket with voice note metadata
- [ ] **Request**: `ChatMessageDto` with `voiceNoteUrl`, `voiceNoteDuration`
- [ ] **Metadata**: Store in `metadata.voiceNote` JSONB field
- [ ] **Response**: Include voice note data
- [ ] **Mapper**: Map voice note metadata

**Status**: ⏳ PENDING

---

### 2.3 Send Location
**Path**: WebSocket with location metadata
- [ ] **Request**: `ChatMessageDto` with `latitude`, `longitude`, `locationName`
- [ ] **Metadata**: Store in `metadata.location` JSONB field
- [ ] **Response**: Include location data
- [ ] **Mapper**: Map location metadata

**Status**: ⏳ PENDING

---

### 2.4 Link Preview
**Path**: Automatic link detection
- [ ] **Metadata**: Store in `metadata.linkPreview` JSONB field
- [ ] **Response**: Include link preview data
- [ ] **Mapper**: Map link preview metadata

**Status**: ⏳ PENDING

---

## 3. REACTION FEATURES

### 3.1 Add Reaction
**Path**: `POST /api/messages/{id}/reactions`
**Resource**: `ReactionResource.addReaction()`
- [ ] **Request DTO**: `AddReactionRequest`
- [ ] **Response DTO**: `ReactionResponse`
- [ ] **Service**: `ReactionService.addReaction()`
- [ ] **Entity**: `Message.reactions` JSONB Map<String, List<Long>>
- [ ] **WebSocket**: Broadcast `MESSAGE_REACTION` with `ReactionEventDto`
- [ ] **Cache**: Update `MessageCache`
- [ ] **Validation**: User is participant

**Status**: ⏳ PENDING

---

### 3.2 Remove Reaction
**Path**: `DELETE /api/messages/{id}/reactions`
**Resource**: `ReactionResource.removeReaction()`
- [ ] **Service**: `ReactionService.removeReaction()`
- [ ] **Entity**: Update reactions JSONB
- [ ] **WebSocket**: Broadcast reaction removal
- [ ] **Cache**: Update `MessageCache`

**Status**: ⏳ PENDING

---

## 4. MENTION FEATURES

### 4.1 Send Message with Mentions
**Path**: WebSocket with mentions
- [ ] **Request**: `ChatMessageDto.mentions` List<Long>
- [ ] **Entity**: `Message.mentions` JSONB array
- [ ] **Response**: `MessageResponse.mentionedUsers`
- [ ] **WebSocket**: Send `MESSAGE_MENTION` to mentioned users
- [ ] **Mapper**: Map mentioned users
- [ ] **Cache**: Use `UserProfileCache` for mentioned users

**Status**: ⏳ PENDING

---

## 5. PIN MESSAGE FEATURES

### 5.1 Pin Message
**Path**: `POST /api/conversations/{id}/messages/{messageId}/pin`
**Resource**: `PinnedMessageResource.pinMessage()`
- [ ] **Service**: `PinnedMessageService.pinMessage()`
- [ ] **Entity**: `Message.isPinned` boolean
- [ ] **WebSocket**: Broadcast `MESSAGE_PIN` with `MessagePinEventDto`
- [ ] **Cache**: Invalidate `ConversationCache`
- [ ] **Validation**: User has pin permission

**Status**: ⏳ PENDING

---

### 5.2 Unpin Message
**Path**: `DELETE /api/conversations/{id}/messages/{messageId}/pin`
- [ ] **Service**: `PinnedMessageService.unpinMessage()`
- [ ] **WebSocket**: Broadcast unpin event
- [ ] **Cache**: Invalidate `ConversationCache`

**Status**: ⏳ PENDING

---

### 5.3 Get Pinned Messages
**Path**: `GET /api/conversations/{id}/pinned-messages`
- [ ] **Service**: `PinnedMessageService.getPinnedMessages()`
- [ ] **Repository**: Query pinned messages
- [ ] **Response**: List of `MessageResponse`
- [ ] **Mapper**: Map with metadata

**Status**: ⏳ PENDING

---

## 6. SCHEDULED MESSAGE FEATURES

### 6.1 Schedule Message
**Path**: `POST /api/messages/schedule`
**Resource**: `ScheduledMessageResource.scheduleMessage()`
- [ ] **Request DTO**: `ScheduleMessageRequest`
- [ ] **Service**: `ScheduledMessageService.scheduleMessage()`
- [ ] **Entity**: `Message.scheduledFor` timestamp
- [ ] **Metadata**: Store scheduling info
- [ ] **Validation**: Future timestamp

**Status**: ⏳ PENDING

---

### 6.2 Send Scheduled Messages (Cron)
**Path**: `ScheduledMessageService.sendScheduledMessages()`
- [ ] **Service**: Query and send due messages
- [ ] **WebSocket**: Broadcast `SCHEDULED_MESSAGE_SENT` with `ScheduledMessageSentEventDto`
- [ ] **Cache**: Invalidate conversation caches
- [ ] **Error handling**: Broadcast `SCHEDULED_MESSAGE_FAILED` on error

**Status**: ⏳ PENDING

---

## 7. ANNOUNCEMENT FEATURES

### 7.1 Create Announcement
**Path**: `POST /api/conversations/{id}/announcements`
**Resource**: `AnnouncementResource.createAnnouncement()`
- [ ] **Request DTO**: `CreateAnnouncementRequest`
- [ ] **Service**: `AnnouncementService.createAnnouncement()`
- [ ] **Entity**: `Message` with type ANNOUNCEMENT
- [ ] **WebSocket**: Broadcast `ANNOUNCEMENT_CREATED` with `AnnouncementEventDto`
- [ ] **Cache**: Invalidate `ConversationCache`
- [ ] **Validation**: User is admin

**Status**: ⏳ PENDING

---

### 7.2 Delete Announcement
**Path**: `DELETE /api/conversations/{id}/announcements/{messageId}`
- [ ] **Service**: `AnnouncementService.deleteAnnouncement()`
- [ ] **WebSocket**: Broadcast `ANNOUNCEMENT_DELETED` with `AnnouncementDeleteEventDto`
- [ ] **Cache**: Invalidate caches
- [ ] **Validation**: Admin or sender only

**Status**: ⏳ PENDING

---

## 8. CONVERSATION FEATURES

### 8.1 Get Conversations
**Path**: `GET /api/conversations`
**Resource**: `ConversationResource.getConversations()`
- [ ] **Service**: `ConversationService.getConversations()`
- [ ] **Repository**: Query with participants
- [ ] **Response**: `ConversationResponse` with lastMessage
- [ ] **Cache**: Check `ConversationCache` first
- [ ] **Mapper**: Map lastMessage metadata correctly

**Status**: ⏳ PENDING

---

### 8.2 Create Conversation
**Path**: `POST /api/conversations`
- [ ] **Request DTO**: `CreateConversationRequest`
- [ ] **Service**: `ConversationService.createConversation()`
- [ ] **Entity**: Create conversation and participants
- [ ] **WebSocket**: Notify participants
- [ ] **Cache**: No cache yet (new conversation)

**Status**: ⏳ PENDING

---

### 8.3 Update Conversation
**Path**: `PUT /api/conversations/{id}`
- [ ] **Service**: `ConversationService.updateConversation()`
- [ ] **WebSocket**: Broadcast `CONVERSATION_UPDATE` with `ConversationUpdateDto`
- [ ] **Cache**: Invalidate `ConversationCache` for all participants

**Status**: ⏳ PENDING

---

## 9. TYPING INDICATOR

### 9.1 Typing Start/Stop
**Path**: WebSocket `typing.start` / `typing.stop`
- [ ] **Request**: `TypingIndicatorDto`
- [ ] **Service**: Track typing users in memory
- [ ] **WebSocket**: Broadcast `TYPING_INDICATOR` with `TypingIndicatorResponseDto`
- [ ] **Cache**: Use `UserProfileCache` for typing user info

**Status**: ⏳ PENDING

---

## 10. ONLINE STATUS & HEARTBEAT

### 10.1 Heartbeat
**Path**: WebSocket `heartbeat`
- [ ] **Handler**: `handleHeartbeat()`
- [ ] **Service**: `HeartbeatMonitorService.recordHeartbeat()`
- [ ] **Cache**: `OnlineStatusCache.markOnline()`
- [ ] **WebSocket**: Send `HEARTBEAT_ACK` with `HeartbeatAckDto`
- [ ] **Batch**: Queue lastSeen update

**Status**: ⏳ PENDING

---

### 10.2 User Status Broadcast
**Path**: `UserStatusBroadcastService.broadcastUserStatusChange()`
- [ ] **Cache**: Check `OnlineStatusCache.isOnline()`
- [ ] **WebSocket**: Broadcast `USER_STATUS` with `UserStatusEventDto`
- [ ] **Recipients**: Friends and conversation participants

**Status**: ⏳ PENDING

---

## 11. CACHE INTEGRATION

### 11.1 UserProfileCache
- [ ] **Usage**: Message sender info, mentioned users, typing users
- [ ] **Invalidation**: On user profile update
- [ ] **TTL**: 1 hour
- [ ] **Max size**: 50,000

**Status**: ⏳ PENDING

---

### 11.2 ConversationCache
- [ ] **Usage**: Conversation list with lastMessage
- [ ] **Invalidation**: On new message, conversation update
- [ ] **TTL**: 10 minutes
- [ ] **Key**: userId_conversationId

**Status**: ⏳ PENDING

---

### 11.3 MessageCache
- [ ] **Usage**: Recent 50 messages per conversation
- [ ] **Invalidation**: On message update/delete
- [ ] **TTL**: 5 minutes
- [ ] **Operations**: add, update, remove message

**Status**: ⏳ PENDING

---

### 11.4 OnlineStatusCache
- [ ] **Usage**: User online/offline status
- [ ] **Update**: On heartbeat, connect, disconnect
- [ ] **TTL**: 5 minutes
- [ ] **Threshold**: 2 minutes for online

**Status**: ⏳ PENDING

---

## 12. METADATA MAPPING

### 12.1 MessageMetadataMapper
- [ ] **Entity → JSONB**: `toJsonb()` method
- [ ] **JSONB → DTO**: `fromJsonb()` method
- [ ] **Fields**: media, location, voiceNote, linkPreview, poll, event
- [ ] **Null handling**: Empty objects vs null

**Status**: ⏳ PENDING

---

### 12.2 MessageMapper
- [ ] **Entity → Response**: Include metadata fields
- [ ] **Ignore fields**: pollId, eventId (data in metadata)
- [ ] **Nested objects**: replyToMessage, originalMessage
- [ ] **Collections**: reactions, mentions, mentionedUsers

**Status**: ⏳ PENDING

---

## 13. WEBSOCKET EVENTS

### 13.1 Event DTOs
- [ ] **HeartbeatAckDto**: userId, timestamp
- [ ] **MessageUpdateEventDto**: messageId, conversationId, content, isEdited, updatedAt
- [ ] **MessageDeleteEventDto**: messageId, conversationId
- [ ] **FriendRequestRejectEventDto**: requestId, rejectedBy
- [ ] **FriendRequestCancelEventDto**: requestId, cancelledBy
- [ ] **AnnouncementEventDto**: announcement, conversationId
- [ ] **AnnouncementDeleteEventDto**: messageId, conversationId, type
- [ ] **ScheduledMessageSentEventDto**: scheduledMessageId, message
- [ ] **ScheduledMessageFailedEventDto**: scheduledMessageId, error, failedAt
- [ ] **MessagePinEventDto**: action, message
- [ ] **UserStatusEventDto**: userId, status, lastSeen

**Status**: ⏳ PENDING

---

### 13.2 Event Type Constants
- [ ] All string literals replaced with `WebSocketEventType` constants
- [ ] No hardcoded strings in WebSocket broadcasts
- [ ] Consistent event naming

**Status**: ⏳ PENDING

---

## AUDIT SUMMARY

**Total Checks**: 0 / 100+
**Completed**: 0
**Failed**: 0
**Pending**: 100+

---

## NEXT STEPS
1. Start with WebSocket endpoint (ChatServerEndpoint)
2. Check message send flow end-to-end
3. Verify metadata mapping
4. Test cache integration
5. Validate all WebSocket events
6. Check REST endpoints
7. Verify mapper logic
8. Test error scenarios
