package com.chattrix.api.services.message;

import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.MessageReadReceipt;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BadRequestException;
import com.chattrix.api.exceptions.ResourceNotFoundException;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.MessageReadReceiptRepository;
import com.chattrix.api.repositories.MessageRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.ReadReceiptResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ReadReceiptService {

    @Inject
    private MessageReadReceiptRepository readReceiptRepository;

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Transactional
    public void markAsRead(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!participantRepository.isUserParticipant(message.getConversation().getId(), userId)) {
            throw new BadRequestException("You are not a participant in this conversation");
        }

        if (message.getSender().getId().equals(userId)) {
            return;
        }

        Optional<MessageReadReceipt> existing = readReceiptRepository.findByMessageIdAndUserId(messageId, userId);
        if (existing.isPresent()) {
            return;
        }

        MessageReadReceipt receipt = new MessageReadReceipt();
        receipt.setMessage(message);
        receipt.setUser(user);
        readReceiptRepository.save(receipt);
    }

    @Transactional
    public void markConversationAsRead(Long userId, Long conversationId, Long lastMessageId) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw new BadRequestException("You are not a participant in this conversation");
        }

        participantRepository.resetUnreadCount(conversationId, userId, lastMessageId);
    }

    public List<ReadReceiptResponse> getReadReceipts(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!participantRepository.isUserParticipant(message.getConversation().getId(), userId)) {
            throw new BadRequestException("You are not a participant in this conversation");
        }

        List<MessageReadReceipt> receipts = readReceiptRepository.findByMessageId(messageId);
        return receipts.stream()
                .map(this::mapToReadReceiptResponse)
                .toList();
    }

    public Long getUnreadCount(Long userId) {
        return participantRepository.getTotalUnreadCount(userId);
    }

    private ReadReceiptResponse mapToReadReceiptResponse(MessageReadReceipt receipt) {
        ReadReceiptResponse response = new ReadReceiptResponse();
        response.setUserId(receipt.getUser().getId());
        response.setUsername(receipt.getUser().getUsername());
        response.setFullName(receipt.getUser().getFullName());
        response.setAvatarUrl(receipt.getUser().getAvatarUrl());
        response.setReadAt(receipt.getReadAt());
        return response;
    }
}

