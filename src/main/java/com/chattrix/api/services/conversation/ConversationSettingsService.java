package com.chattrix.api.services.conversation;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationSettings;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.responses.ConversationSettingsResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class ConversationSettingsService {

    @Inject
    private ConversationSettingsRepository settingsRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private UserRepository userRepository;

    @Transactional
    public ConversationSettingsResponse hideConversation(Long userId, Long conversationId) {
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        settings.setHidden(true);
        settings.setHiddenAt(Instant.now());
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    @Transactional
    public ConversationSettingsResponse unhideConversation(Long userId, Long conversationId) {
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        settings.setHidden(false);
        settings.setHiddenAt(null);
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    @Transactional
    public ConversationSettingsResponse archiveConversation(Long userId, Long conversationId) {
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        settings.setArchived(true);
        settings.setArchivedAt(Instant.now());
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    @Transactional
    public ConversationSettingsResponse unarchiveConversation(Long userId, Long conversationId) {
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        settings.setArchived(false);
        settings.setArchivedAt(null);
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    @Transactional
    public ConversationSettingsResponse pinConversation(Long userId, Long conversationId) {
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);

        if (settings.isPinned()) {
            throw BusinessException.badRequest("Conversation is already pinned", "BAD_REQUEST");
        }

        Integer maxPinOrder = settingsRepository.getMaxPinOrder(userId);

        settings.setPinned(true);
        settings.setPinOrder(maxPinOrder + 1);
        settings.setPinnedAt(Instant.now());
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    @Transactional
    public ConversationSettingsResponse unpinConversation(Long userId, Long conversationId) {
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        settings.setPinned(false);
        settings.setPinOrder(null);
        settings.setPinnedAt(null);
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    @Transactional
    public ConversationSettingsResponse muteConversation(Long userId, Long conversationId) {
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        settings.setMuted(true);
        settings.setMutedAt(Instant.now());
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    @Transactional
    public ConversationSettingsResponse unmuteConversation(Long userId, Long conversationId) {
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        settings.setMuted(false);
        settings.setMutedAt(null);
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    @Transactional
    public ConversationSettingsResponse blockUser(Long userId, Long conversationId) {
        validateDirectConversation(conversationId);
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        settings.setBlocked(true);
        settings.setBlockedAt(Instant.now());
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    @Transactional
    public ConversationSettingsResponse unblockUser(Long userId, Long conversationId) {
        validateDirectConversation(conversationId);
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        settings.setBlocked(false);
        settings.setBlockedAt(null);
        settingsRepository.save(settings);
        return toResponse(settings);
    }

    public ConversationSettingsResponse getSettings(Long userId, Long conversationId) {
        ConversationSettings settings = getOrCreateSettings(userId, conversationId);
        return toResponse(settings);
    }

    private ConversationSettings getOrCreateSettings(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));

        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.badRequest("You are not a participant in this conversation", "BAD_REQUEST");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "RESOURCE_NOT_FOUND"));

        Optional<ConversationSettings> existing = settingsRepository.findByConversationIdAndUserId(conversationId, userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        ConversationSettings settings = new ConversationSettings();
        settings.setConversation(conversation);
        settings.setUser(user);
        settingsRepository.save(settings);
        return settings;
    }

    private ConversationSettingsResponse toResponse(ConversationSettings settings) {
        return ConversationSettingsResponse.builder()
                .conversationId(settings.getConversation().getId())
                .muted(settings.isMuted())
                .mutedUntil(settings.getMutedUntil())
                .blocked(settings.isBlocked())
                .notificationsEnabled(settings.isNotificationsEnabled())
                .customNickname(settings.getCustomNickname())
                .theme(settings.getTheme())
                .pinned(settings.isPinned())
                .pinOrder(settings.getPinOrder())
                .archived(settings.isArchived())
                .hidden(settings.isHidden())
                .build();
    }

    private void validateDirectConversation(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "RESOURCE_NOT_FOUND"));
        if (conversation.getType() != Conversation.ConversationType.DIRECT) {
            throw BusinessException.badRequest("Action only available for direct conversations", "BAD_REQUEST");
        }
    }
}






