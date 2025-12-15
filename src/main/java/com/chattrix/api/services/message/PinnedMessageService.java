package com.chattrix.api.services.message;
import com.chattrix.api.exceptions.BusinessException;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.PinnedMessage;
import com.chattrix.api.entities.User;
// Removed old exception import
// Removed old exception import
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.PinnedMessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.PinnedMessageResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PinnedMessageService {

    @Inject
    private PinnedMessageRepository pinnedMessageRepository;

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private UserRepository userRepository;

    private static final int MAX_PINNED_MESSAGES = 3;

    @Transactional
    public PinnedMessageResponse pinMessage(Long userId, Long conversationId, Long messageId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw BusinessException.badRequest("Message does not belong to this conversation", "BAD_REQUEST");
        }

        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You are not a participant in this conversation", "BAD_REQUEST");
        }

        if (conversation.getType() == Conversation.ConversationType.GROUP) {
            if (!participantRepository.isUserAdmin(conversationId, userId)) {
                throw BusinessException.badRequest("Only admins can pin messages in groups", "BAD_REQUEST");
            }
        }

        Optional<PinnedMessage> existing = pinnedMessageRepository.findByConversationIdAndMessageId(conversationId, messageId);
        if (existing.isPresent()) {
            throw BusinessException.badRequest("Message is already pinned", "BAD_REQUEST");
        }

        long pinnedCount = pinnedMessageRepository.countByConversationId(conversationId);
        if (pinnedCount >= MAX_PINNED_MESSAGES) {
            throw BusinessException.badRequest("Maximum " + MAX_PINNED_MESSAGES + " messages can be pinned", "BAD_REQUEST");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        Integer maxOrder = pinnedMessageRepository.getMaxPinOrder(conversationId);
        
        PinnedMessage pinnedMessage = new PinnedMessage();
        pinnedMessage.setConversation(conversation);
        pinnedMessage.setMessage(message);
        pinnedMessage.setPinnedBy(user);
        pinnedMessage.setPinOrder(maxOrder + 1);
        pinnedMessageRepository.save(pinnedMessage);

        return mapToPinnedMessageResponse(pinnedMessage);
    }

    @Transactional
    public void unpinMessage(Long userId, Long conversationId, Long messageId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You are not a participant in this conversation", "BAD_REQUEST");
        }

        if (conversation.getType() == Conversation.ConversationType.GROUP) {
            if (!participantRepository.isUserAdmin(conversationId, userId)) {
                throw BusinessException.badRequest("Only admins can unpin messages in groups", "BAD_REQUEST");
            }
        }

        PinnedMessage pinnedMessage = pinnedMessageRepository.findByConversationIdAndMessageId(conversationId, messageId)
                .orElseThrow(() -> BusinessException.notFound("Pinned message not found", "RESOURCE_NOT_FOUND"));

        pinnedMessageRepository.delete(pinnedMessage);
    }

    public List<PinnedMessageResponse> getPinnedMessages(Long userId, Long conversationId) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You are not a participant in this conversation", "BAD_REQUEST");
        }

        List<PinnedMessage> pinnedMessages = pinnedMessageRepository.findByConversationId(conversationId);
        return pinnedMessages.stream()
                .map(this::mapToPinnedMessageResponse)
                .toList();
    }

    private PinnedMessageResponse mapToPinnedMessageResponse(PinnedMessage pinnedMessage) {
        PinnedMessageResponse response = new PinnedMessageResponse();
        response.setId(pinnedMessage.getId());
        response.setMessageId(pinnedMessage.getMessage().getId());
        response.setContent(pinnedMessage.getMessage().getContent());
        response.setSenderId(pinnedMessage.getMessage().getSender().getId());
        response.setSenderUsername(pinnedMessage.getMessage().getSender().getUsername());
        response.setPinnedBy(pinnedMessage.getPinnedBy().getId());
        response.setPinnedByUsername(pinnedMessage.getPinnedBy().getUsername());
        response.setPinOrder(pinnedMessage.getPinOrder());
        response.setPinnedAt(pinnedMessage.getPinnedAt());
        response.setSentAt(pinnedMessage.getMessage().getSentAt());
        return response;
    }
}






