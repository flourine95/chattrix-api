package com.chattrix.api.services.message;

import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.MessageEditHistory;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.repositories.MessageEditHistoryRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.EditMessageRequest;
import com.chattrix.api.responses.MessageEditHistoryResponse;
import com.chattrix.api.responses.MessageResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class MessageEditService {

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private MessageEditHistoryRepository editHistoryRepository;

    @Inject
    private UserRepository userRepository;

    @Transactional
    public MessageResponse editMessage(Long userId, Long conversationId, Long messageId, EditMessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        // Validate message belongs to the specified conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw new ResourceNotFoundException("Message not found in this conversation");
        }

        if (!message.getSender().getId().equals(userId)) {
            throw new BadRequestException("You can only edit your own messages");
        }

        if (message.isDeleted()) {
            throw new BadRequestException("Cannot edit deleted message");
        }

        if (message.getType() != Message.MessageType.TEXT) {
            throw new BadRequestException("Can only edit text messages");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        MessageEditHistory history = new MessageEditHistory();
        history.setMessage(message);
        history.setPreviousContent(message.getContent());
        history.setEditedBy(user);
        editHistoryRepository.save(history);

        message.setContent(request.content());
        message.setEdited(true);
        message.setEditedAt(Instant.now());
        messageRepository.save(message);

        return mapToMessageResponse(message);
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own messages");
        }

        if (message.isDeleted()) {
            throw new BadRequestException("Message already deleted");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        message.setDeleted(true);
        message.setDeletedAt(Instant.now());
        message.setDeletedBy(user);
        messageRepository.save(message);
    }

    public List<MessageEditHistoryResponse> getEditHistory(Long userId, Long conversationId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        // Validate message belongs to the specified conversation
        if (conversationId != null && !message.getConversation().getId().equals(conversationId)) {
            throw new ResourceNotFoundException("Message not found in this conversation");
        }

        List<MessageEditHistory> history = editHistoryRepository.findByMessageId(messageId);
        return history.stream()
                .map(this::mapToEditHistoryResponse)
                .toList();
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
        response.setEdited(message.isEdited());
        response.setEditedAt(message.getEditedAt());
        response.setDeleted(message.isDeleted());
        response.setDeletedAt(message.getDeletedAt());
        return response;
    }

    private MessageEditHistoryResponse mapToEditHistoryResponse(MessageEditHistory history) {
        MessageEditHistoryResponse response = new MessageEditHistoryResponse();
        response.setId(history.getId());
        response.setPreviousContent(history.getPreviousContent());
        response.setEditedBy(history.getEditedBy().getId());
        response.setEditedByUsername(history.getEditedBy().getUsername());
        response.setEditedAt(history.getEditedAt());
        return response;
    }
}

