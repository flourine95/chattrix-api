package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.enums.MessageType;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.mappers.WebSocketMapper;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.services.cache.CacheManager;
import com.chattrix.api.services.cache.ConversationCache;
import com.chattrix.api.services.cache.MessageCache;
import com.chattrix.api.services.cache.MessageIdMappingCache;
import com.chattrix.api.services.conversation.GroupPermissionsService;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.WebSocketEventType;
import com.chattrix.api.websocket.dto.ConversationUpdateDto;
import com.chattrix.api.websocket.dto.MentionEventDto;
import com.chattrix.api.websocket.dto.OutgoingMessageDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized service for message creation logic.
 * Used by both REST API and WebSocket handlers to ensure consistency.
 */
@ApplicationScoped
@Slf4j
public class MessageCreationService {

    @Inject
    private MessageRepository messageRepository;
    @Inject
    private ConversationRepository conversationRepository;
    @Inject
    private UserRepository userRepository;
    @Inject
    private ConversationParticipantRepository participantRepository;
    @Inject
    private MessageMapper messageMapper;
    @Inject
    private WebSocketMapper webSocketMapper;
    @Inject
    private MessageCache messageCache;
    @Inject
    private ConversationCache conversationCache;
    @Inject
    private CacheManager cacheManager;
    @Inject
    private MessageBatchService messageBatchService;
    @Inject
    private ChatSessionService chatSessionService;
    @Inject
    private GroupPermissionsService groupPermissionsService;
    @Inject
    private MessageIdMappingCache idMappingCache;

    /**
     * Create and send a message (used by both REST and WebSocket)
     * 
     * @param senderId User ID of sender
     * @param conversationId Conversation ID
     * @param content Message content
     * @param type Message type (TEXT, IMAGE, etc.)
     * @param metadata Additional metadata (media URLs, location, etc.)
     * @param replyToMessageId Optional reply to message ID
     * @param mentions Optional list of mentioned user IDs
     * @param useWriteBehind If true, use write-behind pattern (buffer before DB insert)
     * @return MessageResponse
     */
    @Transactional
    public MessageResponse createMessage(
            Long senderId,
            Long conversationId,
            String content,
            String type,
            Map<String, Object> metadata,
            Long replyToMessageId,
            List<Long> mentions,
            boolean useWriteBehind
    ) {
        log.debug("Creating message: senderId={}, conversationId={}, type={}, useWriteBehind={}", 
                senderId, conversationId, type, useWriteBehind);

        // 1. Validate and load entities
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (!conversation.isUserParticipant(senderId))
            throw BusinessException.forbidden("You are not a participant of this conversation");

        // Check if user is muted (for group conversations)
        if (conversation.isGroupConversation()) {
            ConversationParticipant participant = conversation.getParticipant(senderId).orElse(null);
            if (participant != null && participant.isCurrentlyMuted())
                throw BusinessException.forbidden("You are muted in this conversation");
            
            // Check send_messages permission
            if (!groupPermissionsService.hasPermission(conversationId, senderId, "send_messages"))
                throw BusinessException.forbidden("You don't have permission to send messages in this group");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        // 2. Validate reply to message if provided
        Message replyToMessage = null;
        if (replyToMessageId != null) {
            log.debug("Validating reply to message ID: {}", replyToMessageId);
            
            // Check if it's a temporary ID (negative) - look in cache first
            if (replyToMessageId < 0) {
                // Step 1: Check if temp ID has been mapped to real ID
                Long realId = idMappingCache.getRealId(conversationId, replyToMessageId);
                if (realId != null) {
                    log.debug("Found ID mapping: temp {} -> real {}", replyToMessageId, realId);
                    // Use the real ID to fetch from database
                    replyToMessage = messageRepository.findByIdSimple(realId)
                            .orElse(null);
                    
                    if (replyToMessage != null) {
                        log.debug("Successfully loaded reply message from DB using mapped ID: {}", realId);
                    }
                }
                
                // Step 2: If not mapped yet, check in unflushed cache (O(1) lookup)
                if (replyToMessage == null) {
                    MessageResponse cachedReply = messageCache.getUnflushedById(conversationId, replyToMessageId);
                    
                    if (cachedReply != null) {
                        // Create a temporary Message entity from cached response
                        replyToMessage = new Message();
                        replyToMessage.setId(cachedReply.getId());
                        replyToMessage.setContent(cachedReply.getContent());
                        replyToMessage.setType(MessageType.valueOf(cachedReply.getType()));
                        replyToMessage.setConversation(conversation);
                        replyToMessage.setCreatedAt(cachedReply.getCreatedAt());
                        
                        // Set sender from cached response
                        User replySender = userRepository.findById(cachedReply.getSenderId())
                                .orElse(sender); // Fallback to current sender if not found
                        replyToMessage.setSender(replySender);
                        
                        log.debug("Found reply message in cache with temp ID: {}", replyToMessageId);
                    } else {
                        log.warn("Reply message with temp ID {} not found in unflushed cache", replyToMessageId);
                    }
                }
                
                // Step 3: If still not found, check in message buffer (last resort)
                if (replyToMessage == null) {
                    log.debug("Checking message buffer for temp ID: {}", replyToMessageId);
                    Message bufferedMessage = messageBatchService.getBufferedMessage(replyToMessageId);
                    if (bufferedMessage != null) {
                        // Create a detached copy to avoid OptimisticLockException
                        // Don't use the buffered entity directly as it may cause conflicts during flush
                        replyToMessage = new Message();
                        replyToMessage.setId(bufferedMessage.getId());
                        replyToMessage.setContent(bufferedMessage.getContent());
                        replyToMessage.setType(bufferedMessage.getType());
                        replyToMessage.setConversation(bufferedMessage.getConversation());
                        replyToMessage.setCreatedAt(bufferedMessage.getCreatedAt());
                        replyToMessage.setSender(bufferedMessage.getSender());
                        
                        log.debug("Found reply message in buffer with temp ID: {} (created detached copy)", replyToMessageId);
                    }
                }
                
                // Step 4: If still not found, throw error
                if (replyToMessage == null) {
                    log.error("Reply message with temp ID {} not found in cache, mappings, or buffer", replyToMessageId);
                    throw BusinessException.notFound("Reply to message not found - message may have been deleted");
                }
            } else {
                // For real IDs, look in database
                replyToMessage = messageRepository.findByIdSimple(replyToMessageId)
                        .orElseThrow(() -> BusinessException.notFound("Reply to message not found"));

                if (!replyToMessage.getConversation().getId().equals(conversationId))
                    throw BusinessException.badRequest("Cannot reply to message from different conversation");
            }
        }

        // 3. Validate mentions if provided
        if (mentions != null && !mentions.isEmpty()) {
            Set<Long> participantIds = conversation.getParticipantIds();
            for (Long mentionedUserId : mentions) {
                if (!participantIds.contains(mentionedUserId))
                    throw BusinessException.badRequest("Cannot mention user who is not in this conversation");
            }
        }

        // 4. Build message entity
        Message message = new Message();
        message.setContent(content);
        message.setSender(sender);
        message.setConversation(conversation);
        message.setSentAt(Instant.now());

        // Set message type with auto-detection
        MessageType messageType = MessageType.TEXT;
        if (type != null) {
            try {
                messageType = MessageType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid message type: {}, defaulting to TEXT", type);
            }
        }
        
        // Auto-detect type from metadata and content if not explicitly set or set as TEXT
        if ((type == null || messageType == MessageType.TEXT) && (metadata != null || content != null)) {
            messageType = detectMessageType(metadata, content);
            log.debug("Auto-detected message type: {}", messageType);
        }
        
        message.setType(messageType);

        // Set metadata
        if (metadata != null)
            message.setMetadata(new HashMap<>(metadata));
        else
            message.setMetadata(new HashMap<>());

        // For write-behind: Don't set replyToMessage entity (causes OptimisticLockException)
        // Instead, store replyToMessageId in metadata and set entity during flush
        if (useWriteBehind && replyToMessage != null) {
            message.getMetadata().put("_replyToMessageId", replyToMessage.getId());
            log.debug("Stored replyToMessageId {} in metadata for write-behind", replyToMessage.getId());
        } else {
            // For direct save: Set entity normally
            message.setReplyToMessage(replyToMessage);
        }
        
        message.setMentions(mentions);

        // 5. Save message (Write-Behind or Direct)
        if (useWriteBehind) {
            // Buffer message for batch insert
            Long tempId = messageBatchService.bufferMessage(message);
            message.setId(tempId);
            log.debug("Message buffered with temp ID: {}", tempId);
        } else {
            // Direct DB insert
            messageRepository.save(message);
            log.debug("Message saved directly to DB with ID: {}", message.getId());
        }

        // 6. Map to response
        MessageResponse response = messageMapper.toResponse(message);
        
        // For write-behind: Manually set reply data from metadata (since entity wasn't set)
        if (useWriteBehind && replyToMessage != null) {
            response.setReplyToMessageId(replyToMessage.getId());
            // Map replyToMessage entity to ReplyMessageResponse
            response.setReplyToMessage(messageMapper.toReplyMessageResponse(replyToMessage));
            log.debug("Manually set replyToMessage data in response for write-behind");
        }

        // 7. Update cache
        if (useWriteBehind) {
            log.debug("Adding message {} to unflushed cache for conversation {}", message.getId(), conversationId);
            messageCache.addUnflushed(conversationId, response);
            log.debug("Message {} added to unflushed cache successfully", message.getId());
        } else {
            messageCache.invalidate(conversationId);
        }

        // 8. Update conversation (outside transaction for write-behind)
        if (!useWriteBehind) {
            conversation.setLastMessage(message);
        }
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        // 9. Auto-unarchive for all participants
        conversation.getParticipants().forEach(participant -> {
            if (participant.isArchived()) {
                participant.setArchived(false);
                participant.setArchivedAt(null);
                participantRepository.save(participant);
            }
        });

        // 10. Invalidate caches (but NOT message cache for write-behind)
        Set<Long> participantIds = conversation.getParticipantIds();
        conversationCache.invalidateForAllParticipants(conversationId, participantIds);
        // Note: messageCache is NOT invalidated for write-behind pattern
        // Messages are in unflushed cache and will be synced after flush

        log.debug("Message created successfully: messageId={}, conversationId={}, senderId={}", 
                message.getId(), conversationId, senderId);

        return response;
    }

    /**
     * Broadcast message to all participants via WebSocket
     * Should be called OUTSIDE transaction to avoid holding DB locks
     */
    public void broadcastMessage(Message message, Conversation conversation) {
        log.debug("Broadcasting message: messageId={}, conversationId={}", 
                message.getId(), conversation.getId());

        OutgoingMessageDto outgoingDto = webSocketMapper.toOutgoingMessageResponse(message);
        WebSocketMessage<OutgoingMessageDto> wsMessage = 
                new WebSocketMessage<>(WebSocketEventType.CHAT_MESSAGE, outgoingDto);

        conversation.getParticipants().forEach(p ->
                chatSessionService.sendMessageToUser(p.getUser().getId(), wsMessage)
        );
    }

    /**
     * Broadcast conversation update to all participants
     * Should be called OUTSIDE transaction
     */
    public void broadcastConversationUpdate(Conversation conversation) {
        log.debug("Broadcasting conversation update: conversationId={}", conversation.getId());

        ConversationUpdateDto updateDto = new ConversationUpdateDto();
        updateDto.setConversationId(conversation.getId());
        updateDto.setUpdatedAt(conversation.getUpdatedAt());

        if (conversation.getLastMessage() != null) {
            Message lastMsg = conversation.getLastMessage();
            ConversationUpdateDto.LastMessageDto lastMessageDto = new ConversationUpdateDto.LastMessageDto();
            lastMessageDto.setId(lastMsg.getId());
            lastMessageDto.setContent(lastMsg.getContent());
            lastMessageDto.setSenderId(lastMsg.getSender().getId());
            lastMessageDto.setSenderUsername(lastMsg.getSender().getUsername());
            lastMessageDto.setSentAt(lastMsg.getSentAt());
            lastMessageDto.setType(lastMsg.getType().name());
            updateDto.setLastMessage(lastMessageDto);
        }

        WebSocketMessage<ConversationUpdateDto> message =
                new WebSocketMessage<>(WebSocketEventType.CONVERSATION_UPDATE, updateDto);

        conversation.getParticipants().forEach(participant ->
                chatSessionService.sendMessageToUser(participant.getUser().getId(), message)
        );
    }

    /**
     * Broadcast conversation update with temporary message (before DB flush)
     * This allows clients to see lastMessage immediately with temp ID
     * Will be updated again after flush with real ID
     */
    public void broadcastConversationUpdateWithTempMessage(Conversation conversation, Message tempMessage) {
        log.debug("Broadcasting conversation update with temp message: conversationId={}, tempMessageId={}", 
                conversation.getId(), tempMessage.getId());

        ConversationUpdateDto updateDto = new ConversationUpdateDto();
        updateDto.setConversationId(conversation.getId());
        updateDto.setUpdatedAt(conversation.getUpdatedAt());

        // Use temp message as lastMessage
        ConversationUpdateDto.LastMessageDto lastMessageDto = new ConversationUpdateDto.LastMessageDto();
        lastMessageDto.setId(tempMessage.getId());  // Temp ID (negative)
        lastMessageDto.setContent(tempMessage.getContent());
        lastMessageDto.setSenderId(tempMessage.getSender().getId());
        lastMessageDto.setSenderUsername(tempMessage.getSender().getUsername());
        lastMessageDto.setSentAt(tempMessage.getSentAt());
        lastMessageDto.setType(tempMessage.getType().name());
        updateDto.setLastMessage(lastMessageDto);

        WebSocketMessage<ConversationUpdateDto> message =
                new WebSocketMessage<>(WebSocketEventType.CONVERSATION_UPDATE, updateDto);

        conversation.getParticipants().forEach(participant ->
                chatSessionService.sendMessageToUser(participant.getUser().getId(), message)
        );
    }

    /**
     * Send mention notifications to mentioned users
     * Should be called OUTSIDE transaction
     */
    public void sendMentionNotifications(Message message, List<Long> mentions) {
        if (mentions == null || mentions.isEmpty()) return;

        log.debug("Sending mention notifications: messageId={}, mentions={}", message.getId(), mentions);

        User sender = message.getSender();
        Conversation conversation = message.getConversation();

        for (Long mentionedUserId : mentions) {
            MentionEventDto mentionEvent = new MentionEventDto();
            mentionEvent.setMessageId(message.getId());
            mentionEvent.setConversationId(conversation.getId());
            mentionEvent.setSenderId(sender.getId());
            mentionEvent.setSenderName(sender.getFullName());
            mentionEvent.setContent(message.getContent());
            mentionEvent.setMentionedUserId(mentionedUserId);
            mentionEvent.setCreatedAt(message.getCreatedAt());

            WebSocketMessage<MentionEventDto> mentionMessage =
                    new WebSocketMessage<>(WebSocketEventType.MESSAGE_MENTION, mentionEvent);
            chatSessionService.sendMessageToUser(mentionedUserId, mentionMessage);
        }
    }
    
    /**
     * Auto-detect message type from metadata and content
     * Handles all MessageType enum values with proper priority
     * 
     * Priority order:
     * 1. POLL - has poll data
     * 2. EVENT - has event data
     * 3. LOCATION - has coordinates
     * 4. AUDIO - has duration + mediaUrl
     * 5. STICKER - has sticker data
     * 6. Media types - detect from mediaUrl extension
     * 7. FILE - has mediaUrl but unknown type
     * 8. LINK - has URL in content or linkPreview in metadata
     * 9. TEXT - default
     * 
     * Note: CALL, SYSTEM, ANNOUNCEMENT should be set explicitly by backend
     */
    private MessageType detectMessageType(Map<String, Object> metadata, String content) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        
        // Priority 1: POLL (has poll data)
        if (metadata.containsKey("poll") || metadata.containsKey("pollId") || 
            metadata.containsKey("options") || metadata.containsKey("pollQuestion")) {
            log.debug("Detected POLL type from poll data");
            return MessageType.POLL;
        }
        
        // Priority 2: EVENT (has event data)
        if (metadata.containsKey("event") || metadata.containsKey("eventId") ||
            metadata.containsKey("eventTitle") || metadata.containsKey("eventDate")) {
            log.debug("Detected EVENT type from event data");
            return MessageType.EVENT;
        }
        
        // Priority 3: LOCATION (has coordinates)
        if (metadata.containsKey("latitude") && metadata.containsKey("longitude")) {
            log.debug("Detected LOCATION type from coordinates");
            return MessageType.LOCATION;
        }
        
        // Priority 4: AUDIO (has duration + mediaUrl - voice/audio messages)
        if (metadata.containsKey("duration") && metadata.containsKey("mediaUrl")) {
            log.debug("Detected AUDIO type from duration + mediaUrl");
            return MessageType.AUDIO;
        }
        
        // Priority 5: STICKER (has sticker data)
        if (metadata.containsKey("stickerId") || metadata.containsKey("stickerUrl") ||
            metadata.containsKey("stickerPackId")) {
            log.debug("Detected STICKER type from sticker data");
            return MessageType.STICKER;
        }
        
        // Priority 6: Detect from mediaUrl extension
        if (metadata.containsKey("mediaUrl")) {
            String mediaUrl = metadata.get("mediaUrl").toString().toLowerCase();
            
            // Audio extensions (music files, voice recordings)
            if (mediaUrl.contains(".mp3") || mediaUrl.contains(".m4a") || 
                mediaUrl.contains(".wav") || mediaUrl.contains(".aac") ||
                mediaUrl.contains(".ogg") || mediaUrl.contains(".flac") ||
                mediaUrl.contains(".wma") || mediaUrl.contains("/audio/")) {
                log.debug("Detected AUDIO type from mediaUrl extension");
                return MessageType.AUDIO;
            }
            
            // Video extensions
            if (mediaUrl.contains(".mp4") || mediaUrl.contains(".mov") || 
                mediaUrl.contains(".avi") || mediaUrl.contains(".mkv") ||
                mediaUrl.contains(".webm") || mediaUrl.contains(".flv") ||
                mediaUrl.contains(".wmv") || mediaUrl.contains("/video/")) {
                log.debug("Detected VIDEO type from mediaUrl extension");
                return MessageType.VIDEO;
            }
            
            // Image extensions
            if (mediaUrl.contains(".jpg") || mediaUrl.contains(".jpeg") || 
                mediaUrl.contains(".png") || mediaUrl.contains(".gif") ||
                mediaUrl.contains(".webp") || mediaUrl.contains(".bmp") ||
                mediaUrl.contains(".svg") || mediaUrl.contains("/image/")) {
                log.debug("Detected IMAGE type from mediaUrl extension");
                return MessageType.IMAGE;
            }
            
            // Document/File extensions
            if (mediaUrl.contains(".pdf") || mediaUrl.contains(".doc") || 
                mediaUrl.contains(".docx") || mediaUrl.contains(".xls") ||
                mediaUrl.contains(".xlsx") || mediaUrl.contains(".ppt") ||
                mediaUrl.contains(".pptx") || mediaUrl.contains(".txt") ||
                mediaUrl.contains(".zip") || mediaUrl.contains(".rar") ||
                mediaUrl.contains(".7z") || mediaUrl.contains("/file/") ||
                mediaUrl.contains("/document/")) {
                log.debug("Detected FILE type from document extension");
                return MessageType.FILE;
            }
            
            // Generic file (has mediaUrl but unknown extension)
            log.debug("Detected FILE type from mediaUrl (unknown extension)");
            return MessageType.FILE;
        }
        
        // Priority 7: LINK (has linkPreview in metadata or URL in content)
        if (metadata.containsKey("linkPreview") || metadata.containsKey("url") ||
            (content != null && containsUrl(content))) {
            log.debug("Detected LINK type from URL in content or linkPreview");
            return MessageType.LINK;
        }
        
        // Default to TEXT
        return MessageType.TEXT;
    }
    
    /**
     * Check if content contains URL
     * Simple regex to detect http/https URLs
     */
    private boolean containsUrl(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        // Simple URL pattern: http(s)://... or www....
        String urlPattern = "(?i)\\b(https?://|www\\.)\\S+";
        return content.matches(".*" + urlPattern + ".*");
    }
}
