package com.chattrix.api.services.conversation;

import com.chattrix.api.config.AppConfig;
import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.enums.ConversationType;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.requests.CreateInviteLinkRequest;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.InviteLinkHistoryResponse;
import com.chattrix.api.responses.InviteLinkInfoResponse;
import com.chattrix.api.responses.InviteLinkResponse;
import com.chattrix.api.responses.JoinViaInviteResponse;
import com.chattrix.api.responses.UserBasicResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class InviteLinkService {

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationParticipantRepository participantRepository;

    @Inject
    private AppConfig appConfig;

    @Transactional
    public InviteLinkResponse createInviteLink(Long userId, Long conversationId, CreateInviteLinkRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        if (conversation.getType() != ConversationType.GROUP) {
            throw BusinessException.badRequest("Invite links are only available for group conversations");
        }

        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.forbidden("You are not a member of this conversation"));
        
        if (participant.getRole() != ConversationParticipant.Role.ADMIN) {
            throw BusinessException.forbidden("Only admins can create invite links");
        }

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Instant expiresAt = null;
        if (request.getExpiresIn() != null) {
            expiresAt = Instant.now().plusSeconds(request.getExpiresIn());
        }

        Instant now = Instant.now();

        Map<String, Object> newLink = new HashMap<>();
        newLink.put("token", token);
        newLink.put("expiresAt", expiresAt != null ? expiresAt.toString() : null);
        newLink.put("maxUses", request.getMaxUses());
        newLink.put("currentUses", 0);
        newLink.put("createdBy", userId);
        newLink.put("createdAt", now.toString());
        newLink.put("revoked", false);
        newLink.put("revokedAt", null);
        newLink.put("revokedBy", null);
        newLink.put("active", true);

        Map<String, Object> metadata = conversation.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inviteLinks = (List<Map<String, Object>>) metadata.get("inviteLinks");
        if (inviteLinks == null) {
            inviteLinks = new ArrayList<>();
        }

        inviteLinks.add(0, newLink);
        metadata.put("inviteLinks", inviteLinks);
        conversation.setMetadata(metadata);

        conversationRepository.save(conversation);

        log.info("Created invite link for conversation {} by user {}", conversationId, userId);

        return buildInviteLinkResponse(conversationId, newLink);
    }

    public InviteLinkResponse getCurrentInviteLink(Long userId, Long conversationId) {
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this conversation");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        Map<String, Object> metadata = conversation.getMetadata();
        if (metadata == null) {
            throw BusinessException.notFound("No active invite link found", "NO_ACTIVE_INVITE_LINK");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inviteLinks = (List<Map<String, Object>>) metadata.get("inviteLinks");
        
        if (inviteLinks == null || inviteLinks.isEmpty()) {
            throw BusinessException.notFound("No active invite link found", "NO_ACTIVE_INVITE_LINK");
        }

        Map<String, Object> activeLink = inviteLinks.stream()
                .filter(link -> {
                    Boolean active = (Boolean) link.get("active");
                    Boolean revoked = (Boolean) link.get("revoked");
                    return (active != null && active) && (revoked == null || !revoked);
                })
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("No active invite link found", "NO_ACTIVE_INVITE_LINK"));

        return buildInviteLinkResponse(conversationId, activeLink);
    }

    @Transactional
    public CursorPaginatedResponse<InviteLinkHistoryResponse> getInviteLinkHistory(
            Long userId, Long conversationId, Long cursor, int limit) {
        
        if (!participantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a member of this conversation");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        Map<String, Object> metadata = conversation.getMetadata();
        List<Map<String, Object>> allLinks = new ArrayList<>();

        if (metadata != null && metadata.containsKey("inviteLinks")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> inviteLinks = (List<Map<String, Object>>) metadata.get("inviteLinks");
            if (inviteLinks != null) {
                allLinks.addAll(inviteLinks);
            }
        }

        int startIndex = 0;
        if (cursor != null) {
            for (int i = 0; i < allLinks.size(); i++) {
                Instant createdAt = getInstantValue(allLinks.get(i).get("createdAt"));
                if (createdAt != null && createdAt.toEpochMilli() == cursor) {
                    startIndex = i + 1;
                    break;
                }
            }
        }

        int endIndex = Math.min(startIndex + limit, allLinks.size());
        List<Map<String, Object>> paginatedLinks = allLinks.subList(startIndex, endIndex);

        List<InviteLinkHistoryResponse> responses = paginatedLinks.stream()
                .map(link -> buildHistoryResponse(conversationId, link))
                .collect(Collectors.toList());

        Long nextCursor = null;
        if (endIndex < allLinks.size()) {
            Instant nextCreatedAt = getInstantValue(allLinks.get(endIndex).get("createdAt"));
            if (nextCreatedAt != null) {
                nextCursor = nextCreatedAt.toEpochMilli();
            }
        }

        log.info("Retrieved {} invite links for conversation {} (cursor: {}, total: {})", 
                responses.size(), conversationId, cursor, allLinks.size());

        return new CursorPaginatedResponse<>(responses, nextCursor, limit);
    }

    @Transactional
    public InviteLinkInfoResponse getInviteLinkInfo(String token) {
        Conversation conversation = conversationRepository.findByInviteToken(token)
                .orElseThrow(() -> BusinessException.notFound("Invite link not found or has been revoked", "INVITE_LINK_NOT_FOUND"));

        Map<String, Object> metadata = conversation.getMetadata();
        if (metadata == null) {
            throw BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inviteLinks = (List<Map<String, Object>>) metadata.get("inviteLinks");
        
        if (inviteLinks == null) {
            throw BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND");
        }

        Map<String, Object> inviteLink = inviteLinks.stream()
                .filter(link -> token.equals(link.get("token")))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND"));

        boolean valid = isInviteLinkValid(inviteLink);
        if (!valid) {
            String reason = getInvalidReason(inviteLink);
            throw BusinessException.gone(reason, getInvalidCode(inviteLink));
        }

        long memberCount = participantRepository.countByConversationId(conversation.getId());

        Long createdBy = getLongValue(inviteLink.get("createdBy"));
        ConversationParticipant creator = participantRepository.findByConversationIdAndUserId(
                conversation.getId(), createdBy).orElse(null);

        return InviteLinkInfoResponse.builder()
                .token(token)
                .groupId(conversation.getId())
                .groupName(conversation.getName())
                .groupAvatar(conversation.getAvatarUrl())
                .memberCount((int) memberCount)
                .valid(true)
                .expiresAt(getInstantValue(inviteLink.get("expiresAt")))
                .createdBy(createdBy)
                .createdByUsername(creator != null ? creator.getUser().getUsername() : null)
                .createdByFullName(creator != null ? creator.getUser().getFullName() : null)
                .build();
    }

    @Transactional
    public JoinViaInviteResponse joinViaInviteLink(Long userId, String token) {
        Conversation conversation = conversationRepository.findByInviteToken(token)
                .orElseThrow(() -> BusinessException.notFound("Invite link not found or has been revoked", "INVITE_LINK_NOT_FOUND"));

        if (participantRepository.isUserParticipant(conversation.getId(), userId)) {
            throw BusinessException.badRequest("You are already a member of this group", "ALREADY_MEMBER");
        }

        Map<String, Object> metadata = conversation.getMetadata();
        if (metadata == null) {
            throw BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inviteLinks = (List<Map<String, Object>>) metadata.get("inviteLinks");
        
        if (inviteLinks == null) {
            throw BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND");
        }

        Map<String, Object> inviteLink = inviteLinks.stream()
                .filter(link -> token.equals(link.get("token")))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND"));

        if (!isInviteLinkValid(inviteLink)) {
            String reason = getInvalidReason(inviteLink);
            throw BusinessException.gone(reason, getInvalidCode(inviteLink));
        }

        User user = new User();
        user.setId(userId);

        ConversationParticipant participant = ConversationParticipant.builder()
                .conversation(conversation)
                .user(user)
                .role(ConversationParticipant.Role.MEMBER)
                .joinedAt(Instant.now())
                .build();

        participantRepository.save(participant);

        Integer currentUses = getIntegerValue(inviteLink.get("currentUses"));
        inviteLink.put("currentUses", currentUses + 1);
        conversation.setMetadata(metadata);
        conversationRepository.save(conversation);

        log.info("User {} joined conversation {} via invite link", userId, conversation.getId());

        return JoinViaInviteResponse.builder()
                .success(true)
                .conversationId(conversation.getId())
                .message("You have joined the group")
                .build();
    }

    @Transactional
    public void revokeInviteLink(Long userId, Long conversationId, String token) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found"));

        Map<String, Object> metadata = conversation.getMetadata();
        if (metadata == null) {
            throw BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inviteLinks = (List<Map<String, Object>>) metadata.get("inviteLinks");
        
        if (inviteLinks == null) {
            throw BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND");
        }

        Map<String, Object> inviteLink = inviteLinks.stream()
                .filter(link -> token.equals(link.get("token")))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Invite link not found", "INVITE_LINK_NOT_FOUND"));

        Long createdBy = getLongValue(inviteLink.get("createdBy"));

        ConversationParticipant participant = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> BusinessException.forbidden("You are not a member of this conversation"));

        if (participant.getRole() != ConversationParticipant.Role.ADMIN && !userId.equals(createdBy)) {
            throw BusinessException.forbidden("Only admins or the link creator can revoke invite links");
        }

        inviteLink.put("revoked", true);
        inviteLink.put("revokedAt", Instant.now().toString());
        inviteLink.put("revokedBy", userId);
        inviteLink.put("active", false);
        conversation.setMetadata(metadata);

        conversationRepository.save(conversation);

        log.info("Invite link {} revoked for conversation {} by user {}", token, conversationId, userId);
    }

    private String buildInviteUrl(String token) {
        String baseUrl = appConfig.get("app.base.url");
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = "http://localhost:8080";
        }
        
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        return baseUrl + "/api/v1/invite/" + token;
    }

    private boolean isInviteLinkValid(Map<String, Object> inviteLink) {
        Boolean active = (Boolean) inviteLink.get("active");
        if (active == null || !active) {
            return false;
        }

        Boolean revoked = (Boolean) inviteLink.get("revoked");
        if (revoked != null && revoked) {
            return false;
        }

        Instant expiresAt = getInstantValue(inviteLink.get("expiresAt"));
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return false;
        }

        Integer maxUses = getIntegerValue(inviteLink.get("maxUses"));
        Integer currentUses = getIntegerValue(inviteLink.get("currentUses"));
        if (maxUses != null && maxUses > 0 && currentUses >= maxUses) {
            return false;
        }

        return true;
    }

    private String getInvalidReason(Map<String, Object> inviteLink) {
        Boolean revoked = (Boolean) inviteLink.get("revoked");
        if (revoked != null && revoked) {
            return "This invite link has been revoked";
        }

        Instant expiresAt = getInstantValue(inviteLink.get("expiresAt"));
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return "This invite link has expired";
        }

        Integer maxUses = getIntegerValue(inviteLink.get("maxUses"));
        Integer currentUses = getIntegerValue(inviteLink.get("currentUses"));
        if (maxUses != null && currentUses >= maxUses) {
            return "This invite link has reached its maximum number of uses";
        }

        return "This invite link is no longer valid";
    }

    private String getInvalidCode(Map<String, Object> inviteLink) {
        Boolean revoked = (Boolean) inviteLink.get("revoked");
        if (revoked != null && revoked) {
            return "INVITE_LINK_REVOKED";
        }

        Instant expiresAt = getInstantValue(inviteLink.get("expiresAt"));
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return "INVITE_LINK_EXPIRED";
        }

        Integer maxUses = getIntegerValue(inviteLink.get("maxUses"));
        Integer currentUses = getIntegerValue(inviteLink.get("currentUses"));
        if (maxUses != null && currentUses >= maxUses) {
            return "INVITE_LINK_MAX_USES_REACHED";
        }

        return "INVITE_LINK_INVALID";
    }

    private String determineStatus(Map<String, Object> inviteLink) {
        Boolean revoked = (Boolean) inviteLink.get("revoked");
        if (revoked != null && revoked) {
            return "revoked";
        }

        Instant expiresAt = getInstantValue(inviteLink.get("expiresAt"));
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            return "expired";
        }

        Integer maxUses = getIntegerValue(inviteLink.get("maxUses"));
        Integer currentUses = getIntegerValue(inviteLink.get("currentUses"));
        if (maxUses != null && currentUses >= maxUses) {
            return "max_uses_reached";
        }

        Boolean active = (Boolean) inviteLink.get("active");
        if (active != null && active) {
            return "active";
        }

        return "inactive";
    }

    private InviteLinkResponse buildInviteLinkResponse(Long conversationId, Map<String, Object> inviteLink) {
        String token = (String) inviteLink.get("token");
        return InviteLinkResponse.builder()
                .id(conversationId)
                .token(token)
                .inviteUrl(buildInviteUrl(token))
                .conversationId(conversationId)
                .createdBy(getLongValue(inviteLink.get("createdBy")))
                .createdByUsername(null)
                .createdAt(getInstantValue(inviteLink.get("createdAt")))
                .expiresAt(getInstantValue(inviteLink.get("expiresAt")))
                .maxUses(getIntegerValue(inviteLink.get("maxUses")))
                .currentUses(getIntegerValue(inviteLink.get("currentUses")))
                .revoked((Boolean) inviteLink.get("revoked"))
                .revokedAt(getInstantValue(inviteLink.get("revokedAt")))
                .revokedBy(getLongValue(inviteLink.get("revokedBy")))
                .valid(isInviteLinkValid(inviteLink))
                .build();
    }

    private InviteLinkHistoryResponse buildHistoryResponse(Long conversationId, Map<String, Object> inviteLink) {
        Long createdById = getLongValue(inviteLink.get("createdBy"));
        Long revokedById = getLongValue(inviteLink.get("revokedBy"));

        UserBasicResponse createdBy = null;
        UserBasicResponse revokedBy = null;

        if (createdById != null) {
            ConversationParticipant creator = participantRepository
                    .findByConversationIdAndUserId(conversationId, createdById).orElse(null);
            if (creator != null) {
                createdBy = UserBasicResponse.builder()
                        .id(creator.getUser().getId())
                        .username(creator.getUser().getUsername())
                        .fullName(creator.getUser().getFullName())
                        .avatarUrl(creator.getUser().getAvatarUrl())
                        .build();
            }
        }

        if (revokedById != null) {
            ConversationParticipant revoker = participantRepository
                    .findByConversationIdAndUserId(conversationId, revokedById).orElse(null);
            if (revoker != null) {
                revokedBy = UserBasicResponse.builder()
                        .id(revoker.getUser().getId())
                        .username(revoker.getUser().getUsername())
                        .fullName(revoker.getUser().getFullName())
                        .avatarUrl(revoker.getUser().getAvatarUrl())
                        .build();
            }
        }

        Instant expiresAt = getInstantValue(inviteLink.get("expiresAt"));
        boolean isExpired = expiresAt != null && Instant.now().isAfter(expiresAt);

        return InviteLinkHistoryResponse.builder()
                .id(getLongValue(inviteLink.get("id")))
                .token((String) inviteLink.get("token"))
                .createdBy(createdBy)
                .createdAt(getInstantValue(inviteLink.get("createdAt")))
                .expiresAt(expiresAt)
                .maxUses(getIntegerValue(inviteLink.get("maxUses")))
                .currentUses(getIntegerValue(inviteLink.get("currentUses")))
                .isActive((Boolean) inviteLink.get("active"))
                .isRevoked((Boolean) inviteLink.get("revoked"))
                .isExpired(isExpired)
                .revokedAt(getInstantValue(inviteLink.get("revokedAt")))
                .revokedBy(revokedBy)
                .status(determineStatus(inviteLink))
                .build();
    }

    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return null;
    }

    private Integer getIntegerValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    private Instant getInstantValue(Object value) {
        if (value == null) return null;
        if (value instanceof Instant) return (Instant) value;
        if (value instanceof String) return Instant.parse((String) value);
        if (value instanceof Number) {
            double timestamp = ((Number) value).doubleValue();
            long seconds = (long) timestamp;
            long nanos = (long) ((timestamp - seconds) * 1_000_000_000);
            return Instant.ofEpochSecond(seconds, nanos);
        }
        return null;
    }
}
