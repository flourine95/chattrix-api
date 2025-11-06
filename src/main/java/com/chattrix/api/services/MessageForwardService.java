package com.chattrix.api.services;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.ForwardMessageRequest;
import com.chattrix.api.responses.MessageResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MessageForwardService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private UserRepository userRepository;

    @Transactional
    public List<MessageResponse> forwardMessage(Long userId, ForwardMessageRequest request) {
        Message originalMessage = messageRepository.findById(request.messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (originalMessage.isDeleted()) {
            throw new BadRequestException("Cannot forward deleted message");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<MessageResponse> forwardedMessages = new ArrayList<>();

        for (Long conversationId : request.conversationIds) {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

            if (!participantRepository.isUserParticipant(conversationId, userId)) {
                throw new BadRequestException("You are not a participant in conversation: " + conversationId);
            }

            Message forwardedMessage = new Message();
            forwardedMessage.setConversation(conversation);
            forwardedMessage.setSender(user);
            forwardedMessage.setContent(originalMessage.getContent());
            forwardedMessage.setType(originalMessage.getType());
            forwardedMessage.setForwarded(true);
            forwardedMessage.setOriginalMessage(originalMessage);

            if (originalMessage.getMediaUrl() != null) {
                forwardedMessage.setMediaUrl(originalMessage.getMediaUrl());
                forwardedMessage.setThumbnailUrl(originalMessage.getThumbnailUrl());
                forwardedMessage.setFileName(originalMessage.getFileName());
                forwardedMessage.setFileSize(originalMessage.getFileSize());
                forwardedMessage.setDuration(originalMessage.getDuration());
            }

            messageRepository.save(forwardedMessage);

            participantRepository.incrementUnreadCountForOthers(conversationId, userId);

            conversation.setUpdatedAt(Instant.now());
            conversationRepository.save(conversation);

            forwardedMessages.add(mapToMessageResponse(forwardedMessage));
        }

        originalMessage.setForwardCount(
                originalMessage.getForwardCount() != null
                        ? originalMessage.getForwardCount() + request.conversationIds.size()
                        : request.conversationIds.size()
        );
        messageRepository.save(originalMessage);

        return forwardedMessages;
    }

    private MessageResponse mapToMessageResponse(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversation().getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderUsername(message.getSender().getUsername());
        response.setSenderName(message.getSender().getFullName());
        response.setContent(message.getContent());
        response.setType(message.getType().name());
        response.setSentAt(message.getSentAt());
        response.setForwarded(message.isForwarded());
        response.setOriginalMessageId(message.getOriginalMessage() != null ? message.getOriginalMessage().getId() : null);
        return response;
    }
}

