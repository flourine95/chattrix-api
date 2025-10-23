package com.chattrix.api.services;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.mappers.MessageMapper;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.ChatMessageRequest;
import com.chattrix.api.responses.MessageResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class MessageService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private MessageMapper messageMapper;

    @Inject
    private UserRepository userRepository;

    public List<MessageResponse> getMessages(Long userId, Long conversationId, int page, int size, String sort) {
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        List<Message> messages = messageRepository.findByConversationIdWithSort(conversationId, page, size, sort);
        return messages.stream()
                .map(messageMapper::toResponse)
                .toList();
    }

    public MessageResponse getMessage(Long userId, Long conversationId, Long messageId) {
        // Check if conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        // Get specific message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        // Verify message belongs to this conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw new ResourceNotFoundException("Message not found");
        }

        return messageMapper.toResponse(message);
    }

    @Transactional
    public MessageResponse sendMessage(Long userId, Long conversationId, ChatMessageRequest request) {
        // Check if conversation exists and user is participant
        Conversation conversation = conversationRepository.findByIdWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));

        if (!isParticipant) {
            throw new BadRequestException("You do not have access to this conversation");
        }

        // Get sender
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create and save message
        Message message = new Message();
        message.setContent(request.content());
        message.setSender(sender);
        message.setConversation(conversation);
        message.setType(Message.MessageType.TEXT);
        messageRepository.save(message);

        // Update conversation's lastMessage and updatedAt
        conversation.setLastMessage(message);
        conversationRepository.save(conversation);

        return messageMapper.toResponse(message);
    }
}
