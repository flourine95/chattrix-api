package com.chattrix.api.services.user;

import com.chattrix.api.entities.*;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.*;
import com.chattrix.api.requests.ChatMessageRequest;
import com.chattrix.api.requests.UserNoteRequest;
import com.chattrix.api.responses.MessageResponse;
import com.chattrix.api.responses.UserNoteResponse;
import com.chattrix.api.services.message.MessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserNoteService {

    @Inject
    private UserNoteRepository noteRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ContactRepository contactRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private MessageService messageService;

    @Inject
    private MessageRepository messageRepository;

    /**
     * Create or update user's note (status)
     * Only one active note per user at a time
     */
    @Transactional
    public UserNoteResponse createOrUpdateNote(Long userId, UserNoteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        // Delete existing note if any
        noteRepository.deleteByUserId(userId);

        // Create new note
        UserNote note = new UserNote();
        note.setUser(user);
        note.setNoteText(request.noteText());
        note.setMusicUrl(request.musicUrl());
        note.setMusicTitle(request.musicTitle());
        note.setEmoji(request.emoji());

        noteRepository.save(note);
        return mapToResponse(note);
    }

    /**
     * Get current user's active note
     */
    public Optional<UserNoteResponse> getMyNote(Long userId) {
        return noteRepository.findByUserId(userId)
                .map(this::mapToResponse);
    }

    /**
     * Get a specific user's note
     */
    public Optional<UserNoteResponse> getUserNote(Long currentUserId, Long targetUserId) {
        // Check if users are contacts
        boolean areContacts = contactRepository.areUsersConnected(currentUserId, targetUserId);
        if (!areContacts && !currentUserId.equals(targetUserId)) {
            throw BusinessException.badRequest("You can only view notes from your contacts", "BAD_REQUEST");
        }

        return noteRepository.findByUserId(targetUserId)
                .map(this::mapToResponse);
    }

    /**
     * Get all active notes from user's contacts
     * Displayed in the inbox/chat list
     */
    public List<UserNoteResponse> getContactsNotes(Long userId) {
        // Get user's contacts
        List<Contact> contacts = contactRepository.findByUserId(userId);
        List<Long> contactIds = contacts.stream()
                .map(c -> c.getContactUser().getId())
                .collect(Collectors.toList());

        if (contactIds.isEmpty()) {
            return List.of();
        }

        // Get active notes from contacts
        List<UserNote> notes = noteRepository.findActiveNotesByUserIds(contactIds);

        return notes.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete user's note
     */
    @Transactional
    public void deleteNote(Long userId) {
        noteRepository.deleteByUserId(userId);
    }

    /**
     * Reply to a note
     * Creates a TEXT message in direct conversation with replyToNote reference
     */
    @Transactional
    public MessageResponse replyToNote(Long userId, Long noteId, String replyText) {
        UserNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> BusinessException.notFound("Note not found or expired", "RESOURCE_NOT_FOUND"));

        // Cannot reply to own note
        if (note.getUser().getId().equals(userId)) {
            throw BusinessException.badRequest("You cannot reply to your own note", "BAD_REQUEST");
        }

        // Check if users are contacts
        boolean areContacts = contactRepository.areUsersConnected(userId, note.getUser().getId());
        if (!areContacts) {
            throw BusinessException.badRequest("You can only reply to notes from your contacts", "BAD_REQUEST");
        }

        // Find or create direct conversation
        Conversation conversation = findOrCreateDirectConversation(userId, note.getUser().getId());

        // Send message (TEXT type)
        ChatMessageRequest messageRequest = new ChatMessageRequest(
                replyText,
                "TEXT",
                null, null, null, null, null,
                null, null, null,
                null, null
        );

        return messageService.sendMessage(userId, conversation.getId(), messageRequest);
        // Note: Message sẽ có field replyToNote được set bởi frontend khi gọi API
    }

    /**
     * React to a note with emoji
     * Creates a TEXT message with just the emoji in direct conversation
     */
    @Transactional
    public MessageResponse reactToNote(Long userId, Long noteId, String emoji) {
        UserNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> BusinessException.notFound("Note not found or expired", "RESOURCE_NOT_FOUND"));

        // Check if users are contacts
        boolean areContacts = contactRepository.areUsersConnected(userId, note.getUser().getId());
        if (!areContacts && !note.getUser().getId().equals(userId)) {
            throw BusinessException.badRequest("You can only react to notes from your contacts", "BAD_REQUEST");
        }

        // Find or create direct conversation
        Conversation conversation = findOrCreateDirectConversation(userId, note.getUser().getId());

        // Send emoji as TEXT message
        ChatMessageRequest messageRequest = new ChatMessageRequest(
                emoji,  // Just the emoji: "👍", "❤️", etc.
                "TEXT",
                null, null, null, null, null,
                null, null, null,
                null, null
        );

        return messageService.sendMessage(userId, conversation.getId(), messageRequest);
    }

    /**
     * Get all messages (replies + reactions) related to a note
     * Returns raw Message entities - frontend will format them
     */
    public List<Message> getNoteMessages(Long userId, Long noteId) {
        UserNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> BusinessException.notFound("Note not found or expired", "RESOURCE_NOT_FOUND"));

        // Only note owner can see messages
        if (!note.getUser().getId().equals(userId)) {
            throw BusinessException.badRequest("You can only view messages to your own notes", "BAD_REQUEST");
        }

        return messageRepository.findByReplyToNoteId(noteId);
    }

    /**
     * Cleanup expired notes (scheduled task)
     */
    @Transactional
    public int cleanupExpiredNotes() {
        return noteRepository.deleteExpiredNotes();
    }

    /**
     * Find existing direct conversation or create new one
     */
    private Conversation findOrCreateDirectConversation(Long userId1, Long userId2) {
        // Try to find existing conversation
        Optional<Conversation> existing = conversationRepository.findDirectConversationBetweenUsers(userId1, userId2);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new direct conversation
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        Conversation conversation = new Conversation();
        conversation.setType(Conversation.ConversationType.DIRECT);
        conversation.setName(null); // Direct conversations don't have names

        Set<ConversationParticipant> participants = new HashSet<>();

        ConversationParticipant participant1 = new ConversationParticipant();
        participant1.setUser(user1);
        participant1.setConversation(conversation);
        participant1.setRole(ConversationParticipant.Role.MEMBER);
        participants.add(participant1);

        ConversationParticipant participant2 = new ConversationParticipant();
        participant2.setUser(user2);
        participant2.setConversation(conversation);
        participant2.setRole(ConversationParticipant.Role.MEMBER);
        participants.add(participant2);

        conversation.setParticipants(participants);
        return conversationRepository.save(conversation);
    }

    private UserNoteResponse mapToResponse(UserNote note) {
        UserNoteResponse response = new UserNoteResponse();
        response.setId(note.getId());
        response.setUserId(note.getUser().getId());
        response.setUsername(note.getUser().getUsername());
        response.setFullName(note.getUser().getFullName());
        response.setAvatarUrl(note.getUser().getAvatarUrl());
        response.setNoteText(note.getNoteText());
        response.setMusicUrl(note.getMusicUrl());
        response.setMusicTitle(note.getMusicTitle());
        response.setEmoji(note.getEmoji());
        response.setCreatedAt(note.getCreatedAt());
        response.setExpiresAt(note.getExpiresAt());

        // Count all messages (replies + reactions) related to this note
        Long replyCount = messageRepository.countByReplyToNoteId(note.getId());
        response.setReplyCount(replyCount);

        return response;
    }
}






