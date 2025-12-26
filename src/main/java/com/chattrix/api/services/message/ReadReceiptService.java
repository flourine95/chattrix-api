package com.chattrix.api.services.message;

import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.Message;
import com.chattrix.api.entities.MessageReadReceipt;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
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
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        if (!participantRepository.isUserParticipant(message.getConversation().getId(), userId)) {
            throw BusinessException.badRequest("You are not a participant in this conversation", "BAD_REQUEST");
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
            throw BusinessException.badRequest("You are not a participant in this conversation", "BAD_REQUEST");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // Get all unread messages in this conversation up to lastMessageId
        List<Message> unreadMessages;
        if (lastMessageId != null) {
            unreadMessages = messageRepository.findUnreadMessagesUpTo(conversationId, userId, lastMessageId);
        } else {
            unreadMessages = messageRepository.findUnreadMessages(conversationId, userId);
        }

        // Create read receipts for all unread messages (except user's own messages)
        for (Message message : unreadMessages) {
            // Skip if user is the sender
            if (message.getSender().getId().equals(userId)) {
                continue;
            }

            // Skip if already read
            if (readReceiptRepository.existsByMessageIdAndUserId(message.getId(), userId)) {
                continue;
            }

            // Create read receipt
            MessageReadReceipt receipt = new MessageReadReceipt();
            receipt.setMessage(message);
            receipt.setUser(user);
            readReceiptRepository.save(receipt);
        }

        // Reset unread count in participant table
        participantRepository.resetUnreadCount(conversationId, userId, lastMessageId);
    }

    @Transactional
    public void markConversationAsUnread(Long userId, Long conversationId) {
        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.badRequest("You are not a participant in this conversation", "BAD_REQUEST"));

        // If it's already unread (count > 0), we don't need to do anything
        // But usually "Mark as unread" in chat apps sets a visual indicator (like unreadCount = 1)
        if (participant.getUnreadCount() == 0) {
            participant.setUnreadCount(1);
            participantRepository.save(participant);
        }
    }

    public List<ReadReceiptResponse> getReadReceipts(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> BusinessException.notFound("Message not found", "RESOURCE_NOT_FOUND"));

        if (!participantRepository.isUserParticipant(message.getConversation().getId(), userId)) {
            throw BusinessException.badRequest("You are not a participant in this conversation", "BAD_REQUEST");
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
