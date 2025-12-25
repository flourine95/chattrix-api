package com.chattrix.api.services.conversation;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationParticipantRepository;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.MuteMemberRequest;
import com.chattrix.api.responses.MuteMemberResponse;
import com.chattrix.api.services.message.SystemMessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class MemberMuteService {
    
    @Inject
    private ConversationRepository conversationRepository;
    
    @Inject
    private ConversationParticipantRepository participantRepository;
    
    @Inject
    private UserRepository userRepository;
    
    @Inject
    private SystemMessageService systemMessageService;
    
    /**
     * Mute a member in a group conversation (admin only)
     */
    @Transactional
    public MuteMemberResponse muteMember(Long adminUserId, Long conversationId, Long memberUserId, MuteMemberRequest request) {
        // Validate conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));
        
        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw BusinessException.badRequest("Can only mute members in group conversations", "INVALID_CONVERSATION_TYPE");
        }
        
        // Validate admin is admin
        if (!participantRepository.isUserAdmin(conversationId, adminUserId)) {
            throw BusinessException.forbidden("Only admins can mute members");
        }
        
        // Cannot mute yourself
        if (adminUserId.equals(memberUserId)) {
            throw BusinessException.badRequest("Cannot mute yourself", "INVALID_OPERATION");
        }
        
        // Get member participant
        ConversationParticipant memberParticipant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> BusinessException.notFound("Member not found in conversation", "MEMBER_NOT_FOUND"));
        
        // Cannot mute another admin
        if (memberParticipant.getRole() == ConversationParticipant.Role.ADMIN) {
            throw BusinessException.forbidden("Cannot mute another admin");
        }
        
        // Get admin user
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> BusinessException.notFound("Admin user not found", "USER_NOT_FOUND"));
        
        // Set mute
        memberParticipant.setMuted(true);
        memberParticipant.setMutedAt(Instant.now());
        memberParticipant.setMutedBy(adminUser);
        
        // Set mute duration
        Integer duration = request.getDuration();
        if (duration == null || duration == -1) {
            // Permanent mute
            memberParticipant.setMutedUntil(null);
        } else {
            // Temporary mute
            memberParticipant.setMutedUntil(Instant.now().plusSeconds(duration));
        }
        
        participantRepository.save(memberParticipant);
        
        // Create system message
        systemMessageService.createMemberMutedMessage(conversationId, memberUserId, adminUserId, memberParticipant.getMutedUntil());
        
        return toResponse(memberParticipant);
    }
    
    /**
     * Unmute a member in a group conversation (admin only)
     */
    @Transactional
    public MuteMemberResponse unmuteMember(Long adminUserId, Long conversationId, Long memberUserId) {
        // Validate conversation exists and is a group
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));
        
        if (conversation.getType() != Conversation.ConversationType.GROUP) {
            throw BusinessException.badRequest("Can only unmute members in group conversations", "INVALID_CONVERSATION_TYPE");
        }
        
        // Validate admin is admin
        if (!participantRepository.isUserAdmin(conversationId, adminUserId)) {
            throw BusinessException.forbidden("Only admins can unmute members");
        }
        
        // Get member participant
        ConversationParticipant memberParticipant = participantRepository
                .findByConversationIdAndUserId(conversationId, memberUserId)
                .orElseThrow(() -> BusinessException.notFound("Member not found in conversation", "MEMBER_NOT_FOUND"));
        
        // Check if member is muted
        if (!memberParticipant.isMuted()) {
            throw BusinessException.badRequest("Member is not muted", "NOT_MUTED");
        }
        
        // Unmute
        memberParticipant.setMuted(false);
        memberParticipant.setMutedUntil(null);
        memberParticipant.setMutedBy(null);
        memberParticipant.setMutedAt(null);
        
        participantRepository.save(memberParticipant);
        
        // Create system message
        systemMessageService.createMemberUnmutedMessage(conversationId, memberUserId, adminUserId);
        
        return toResponse(memberParticipant);
    }
    
    /**
     * Check if a member is currently muted
     */
    public boolean isMemberMuted(Long conversationId, Long userId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElse(null);
        
        if (participant == null) {
            return false;
        }
        
        return participant.isCurrentlyMuted();
    }
    
    private MuteMemberResponse toResponse(ConversationParticipant participant) {
        return MuteMemberResponse.builder()
                .userId(participant.getUser().getId())
                .username(participant.getUser().getUsername())
                .fullName(participant.getUser().getFullName())
                .muted(participant.isMuted())
                .mutedUntil(participant.getMutedUntil())
                .mutedAt(participant.getMutedAt())
                .mutedBy(participant.getMutedBy() != null ? participant.getMutedBy().getId() : null)
                .build();
    }
}
