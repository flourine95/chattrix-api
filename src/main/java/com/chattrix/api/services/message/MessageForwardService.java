package com.chattrix.api.services.message;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
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
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        if (originalMessage.isDeleted()) {
            throw BusinessException.badRequest("Cannot forward deleted message", "BAD_REQUEST");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        List<MessageResponse> forwardedMessages = new ArrayList<>();

        for (Long conversationId : request.conversationIds) {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> BusinessException.notFound("Conversation not found: " + conversationId, "RESOURCE_NOT_FOUND"));

            if (!participantRepository.isUserParticipant(conversationId, userId)) {
                throw BusinessException.badRequest("You are not a participant in conversation: " + conversationId, "BAD_REQUEST");
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
        response.setSenderFullName(message.getSender().getFullName());
        response.setContent(message.getContent());
        response.setType(message.getType().name());
        response.setSentAt(message.getSentAt());
        response.setForwarded(message.isForwarded());
        response.setOriginalMessageId(message.getOriginalMessage() != null ? message.getOriginalMessage().getId() : null);
        return response;
    }
}






