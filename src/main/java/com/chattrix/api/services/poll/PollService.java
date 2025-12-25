package com.chattrix.api.services.poll;

import com.chattrix.api.entities.*;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.mappers.PollMapper;
import com.chattrix.api.mappers.UserMapper;
import com.chattrix.api.repositories.*;
import com.chattrix.api.requests.CreatePollRequest;
import com.chattrix.api.requests.VotePollRequest;
import com.chattrix.api.responses.CursorPaginatedResponse;
import com.chattrix.api.responses.PollResponse;
import com.chattrix.api.services.notification.ChatSessionService;
import com.chattrix.api.websocket.dto.PollEventDto;
import com.chattrix.api.websocket.dto.WebSocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class PollService {
    @Inject
    private PollRepository pollRepository;

    @Inject
    private PollOptionRepository pollOptionRepository;

    @Inject
    private PollVoteRepository pollVoteRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private ConversationParticipantRepository conversationParticipantRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private MessageRepository messageRepository;

    @Inject
    private PollMapper pollMapper;

    @Inject
    private UserMapper userMapper;

    @Inject
    private ChatSessionService chatSessionService;

    @Transactional
    public PollResponse createPoll(Long conversationId, CreatePollRequest request, Long creatorId) {
        // Validate conversation exists
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));

        // Validate user is participant
        if (!conversationParticipantRepository.isUserParticipant(conversationId, creatorId)) {
            throw BusinessException.forbidden("You are not a participant of this conversation");
        }

        // Get creator
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "USER_NOT_FOUND"));

        // Create poll
        Poll poll = Poll.builder()
                .question(request.getQuestion())
                .conversation(conversation)
                .creator(creator)
                .allowMultipleVotes(request.getAllowMultipleVotes())
                .expiresAt(request.getExpiresAt())
                .closed(false)
                .build();

        poll = pollRepository.save(poll);

        // Create options
        for (int i = 0; i < request.getOptions().size(); i++) {
            PollOption option = PollOption.builder()
                    .poll(poll)
                    .optionText(request.getOptions().get(i))
                    .optionOrder(i)
                    .build();
            pollOptionRepository.save(option);
            poll.getOptions().add(option);
        }

        // Create a message for the poll
        Message pollMessage = new Message();
        pollMessage.setConversation(conversation);
        pollMessage.setSender(creator);
        pollMessage.setType(Message.MessageType.POLL);
        pollMessage.setPoll(poll);
        pollMessage.setContent(poll.getQuestion()); // Store question as content
        pollMessage.setSentAt(java.time.Instant.now());
        messageRepository.save(pollMessage);

        // Send WebSocket notification
        PollResponse pollResponse = pollMapper.toResponseWithDetails(poll, creatorId, userMapper);
        sendPollNotification(conversationId, "POLL_CREATED", pollResponse);

        return pollResponse;
    }

    @Transactional
    public PollResponse vote(Long conversationId, Long pollId, VotePollRequest request, Long userId) {
        // Get poll
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> BusinessException.notFound("Poll not found", "POLL_NOT_FOUND"));

        // Validate poll belongs to conversation
        if (!poll.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Poll not found in this conversation", "POLL_NOT_FOUND");
        }

        // Validate poll is active
        if (!poll.isActive()) {
            throw BusinessException.badRequest("Poll is not active", "POLL_NOT_ACTIVE");
        }

        // Validate user is participant
        if (!conversationParticipantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a participant of this conversation");
        }

        // Validate multiple votes
        if (!poll.getAllowMultipleVotes() && request.getOptionIds().size() > 1) {
            throw BusinessException.badRequest("This poll does not allow multiple votes", "MULTIPLE_VOTES_NOT_ALLOWED");
        }

        // Get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "USER_NOT_FOUND"));

        // Remove existing votes
        pollVoteRepository.deleteByPollIdAndUserId(pollId, userId);

        // Add new votes
        for (Long optionId : request.getOptionIds()) {
            PollOption option = pollOptionRepository.findById(optionId)
                    .orElseThrow(() -> BusinessException.notFound("Poll option not found", "POLL_OPTION_NOT_FOUND"));

            if (!option.getPoll().getId().equals(pollId)) {
                throw BusinessException.badRequest("Option does not belong to this poll", "INVALID_OPTION");
            }

            PollVote vote = PollVote.builder()
                    .poll(poll)
                    .pollOption(option)
                    .user(user)
                    .build();
            pollVoteRepository.save(vote);
        }

        // Refresh poll to get updated votes
        pollRepository.flushAndClear(); // Ensure votes are deleted/inserted before reloading
        poll = pollRepository.findById(pollId)
                .orElseThrow(() -> BusinessException.notFound("Poll not found", "POLL_NOT_FOUND"));

        // Send WebSocket notification
        PollResponse pollResponse = pollMapper.toResponseWithDetails(poll, userId, userMapper);
        sendPollNotification(poll.getConversation().getId(), "POLL_VOTED", pollResponse);

        return pollResponse;
    }

    @Transactional
    public PollResponse removeVote(Long conversationId, Long pollId, Long userId) {
        // Get poll
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> BusinessException.notFound("Poll not found", "POLL_NOT_FOUND"));

        // Validate poll belongs to conversation
        if (!poll.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Poll not found in this conversation", "POLL_NOT_FOUND");
        }

        // Validate user is participant
        if (!conversationParticipantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a participant of this conversation");
        }

        // Remove votes
        pollVoteRepository.deleteByPollIdAndUserId(pollId, userId);

        // Refresh poll
        pollRepository.flushAndClear();
        poll = pollRepository.findById(pollId)
                .orElseThrow(() -> BusinessException.notFound("Poll not found", "POLL_NOT_FOUND"));

        // Send WebSocket notification
        PollResponse pollResponse = pollMapper.toResponseWithDetails(poll, userId, userMapper);
        sendPollNotification(poll.getConversation().getId(), "POLL_VOTED", pollResponse);

        return pollResponse;
    }

    @Transactional
    public PollResponse getPoll(Long conversationId, Long pollId, Long userId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> BusinessException.notFound("Poll not found", "POLL_NOT_FOUND"));

        // Validate poll belongs to conversation
        if (!poll.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Poll not found in this conversation", "POLL_NOT_FOUND");
        }

        // Validate user is participant
        if (!conversationParticipantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a participant of this conversation");
        }

        return pollMapper.toResponseWithDetails(poll, userId, userMapper);
    }

    @Transactional
    public CursorPaginatedResponse<PollResponse> getConversationPolls(Long conversationId, Long userId, Long cursor, int limit) {
        if (limit < 1) {
            throw BusinessException.badRequest("Limit must be at least 1", "INVALID_LIMIT");
        }
        if (limit > 100) {
            limit = 100;
        }

        // Validate conversation exists
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));

        // Validate user is participant
        if (!conversationParticipantRepository.isUserParticipant(conversationId, userId)) {
            throw BusinessException.forbidden("You are not a participant of this conversation");
        }

        List<Poll> polls = pollRepository.findByConversationIdWithCursor(conversationId, cursor, limit);

        boolean hasMore = polls.size() > limit;
        if (hasMore) {
            polls = polls.subList(0, limit);
        }

        List<PollResponse> pollResponses = polls.stream()
                .map(poll -> pollMapper.toResponseWithDetails(poll, userId, userMapper))
                .collect(Collectors.toList());

        Long nextCursor = null;
        if (hasMore && !pollResponses.isEmpty()) {
            nextCursor = pollResponses.get(pollResponses.size() - 1).getId();
        }

        return new CursorPaginatedResponse<>(pollResponses, nextCursor, limit);
    }

    @Transactional
    public PollResponse closePoll(Long conversationId, Long pollId, Long userId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> BusinessException.notFound("Poll not found", "POLL_NOT_FOUND"));

        // Validate poll belongs to conversation
        if (!poll.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Poll not found in this conversation", "POLL_NOT_FOUND");
        }

        // Validate user is creator
        if (!poll.getCreator().getId().equals(userId)) {
            throw BusinessException.forbidden("Only the poll creator can close the poll");
        }

        poll.setClosed(true);
        poll = pollRepository.save(poll);

        // Send WebSocket notification
        PollResponse pollResponse = pollMapper.toResponseWithDetails(poll, userId, userMapper);
        sendPollNotification(poll.getConversation().getId(), "POLL_CLOSED", pollResponse);

        return pollResponse;
    }

    @Transactional
    public void deletePoll(Long conversationId, Long pollId, Long userId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> BusinessException.notFound("Poll not found", "POLL_NOT_FOUND"));

        // Validate poll belongs to conversation
        if (!poll.getConversation().getId().equals(conversationId)) {
            throw BusinessException.notFound("Poll not found in this conversation", "POLL_NOT_FOUND");
        }

        // Validate user is creator
        if (!poll.getCreator().getId().equals(userId)) {
            throw BusinessException.forbidden("Only the poll creator can delete the poll");
        }

        // Send WebSocket notification BEFORE deleting (while poll still exists)
        PollResponse pollResponse = PollResponse.builder()
                .id(pollId)
                .conversationId(conversationId)
                .build();
        sendPollNotification(conversationId, "POLL_DELETED", pollResponse);

        // Delete messages that reference this poll (to avoid FK constraint violation)
        messageRepository.deleteByPollId(pollId);

        // Delete poll (cascade will delete options and votes)
        pollRepository.delete(poll);
    }

    private void sendPollNotification(Long conversationId, String eventType, PollResponse pollResponse) {
        try {
            PollEventDto pollEvent = PollEventDto.builder()
                    .type(eventType)
                    .poll(pollResponse)
                    .build();

            WebSocketMessage<PollEventDto> message = new WebSocketMessage<>("poll.event", pollEvent);

            List<Long> participantIds = conversationParticipantRepository
                    .findByConversationId(conversationId)
                    .stream()
                    .map(cp -> cp.getUser().getId())
                    .toList();

            for (Long participantId : participantIds) {
                try {
                    chatSessionService.sendDirectMessage(participantId, message);
                } catch (Exception e) {
                    // Log but continue
                    System.err.println("Failed to send poll notification to user " + participantId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to send poll notification: " + e.getMessage());
        }
    }
}
