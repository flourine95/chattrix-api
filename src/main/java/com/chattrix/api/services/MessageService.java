package com.chattrix.api.services;

import com.chattrix.api.dto.responses.MessageDto;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class MessageService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private UserRepository userRepository;

    public List<MessageDto> getConversationMessages(UUID conversationId, UUID userId, int page, int size) {
        // Verify conversation exists
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        // Verify user is a participant in the conversation
        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(participant -> participant.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new ForbiddenException("You are not a participant in this conversation");
        }

        // Get messages
        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, page, size);

        return messages.stream()
                .map(MessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    public long getConversationMessageCount(UUID conversationId, UUID userId) {
        // Verify conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(participant -> participant.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new ForbiddenException("You are not a participant in this conversation");
        }

        return messageRepository.countByConversationId(conversationId);
    }
}
