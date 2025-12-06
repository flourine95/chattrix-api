package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.PinnedMessage;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new BadRequestException("Message does not belong to this conversation");
        }

        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You are not a participant in this conversation");
        }

        if (conversation.getType() == Conversation.ConversationType.GROUP) {
            if (!participantRepository.isUserAdmin(conversationId, userId)) {
                throw new BadRequestException("Only admins can pin messages in groups");
            }
        }

        Optional<PinnedMessage> existing = pinnedMessageRepository.findByConversationIdAndMessageId(conversationId, messageId);
        if (existing.isPresent()) {
            throw new BadRequestException("Message is already pinned");
        }

        long pinnedCount = pinnedMessageRepository.countByConversationId(conversationId);
        if (pinnedCount >= MAX_PINNED_MESSAGES) {
            throw new BadRequestException("Maximum " + MAX_PINNED_MESSAGES + " messages can be pinned");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You are not a participant in this conversation");
        }

        if (conversation.getType() == Conversation.ConversationType.GROUP) {
            if (!participantRepository.isUserAdmin(conversationId, userId)) {
                throw new BadRequestException("Only admins can unpin messages in groups");
            }
        }

        PinnedMessage pinnedMessage = pinnedMessageRepository.findByConversationIdAndMessageId(conversationId, messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Pinned message not found"));

        pinnedMessageRepository.delete(pinnedMessage);
    }

    public List<PinnedMessageResponse> getPinnedMessages(Long userId, Long conversationId) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You are not a participant in this conversation");
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

