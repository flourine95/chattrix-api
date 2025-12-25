package com.chattrix.api.mappers;

import com.chattrix.api.entities.Poll;
import com.chattrix.api.entities.PollOption;
import com.chattrix.api.entities.PollVote;
import com.chattrix.api.responses.PollOptionResponse;
import com.chattrix.api.responses.PollResponse;
import com.chattrix.api.responses.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi", uses = {UserMapper.class})
public interface PollMapper {
    
    @Mapping(target = "conversationId", source = "poll.conversation.id")
    @Mapping(target = "creator", source = "poll.creator")
    @Mapping(target = "expired", expression = "java(poll.isExpired())")
    @Mapping(target = "active", expression = "java(poll.isActive())")
    @Mapping(target = "totalVoters", ignore = true)
    @Mapping(target = "options", ignore = true)
    @Mapping(target = "currentUserVotedOptionIds", ignore = true)
    PollResponse toResponse(Poll poll);

    default PollResponse toResponseWithDetails(Poll poll, Long currentUserId, UserMapper userMapper) {
        PollResponse response = toResponse(poll);
        
        // Calculate total voters
        Long totalVoters = poll.getVotes().stream()
            .map(v -> v.getUser().getId())
            .distinct()
            .count();
        response.setTotalVoters(totalVoters.intValue());
        
        // Map options with vote details
        List<PollOptionResponse> optionResponses = poll.getOptions().stream()
            .map(option -> {
                List<PollVote> optionVotes = option.getVotes();
                int voteCount = optionVotes.size();
                double percentage = totalVoters > 0 ? (voteCount * 100.0 / totalVoters) : 0.0;
                
                List<UserResponse> voters = optionVotes.stream()
                    .map(vote -> userMapper.toResponse(vote.getUser()))
                    .collect(Collectors.toList());
                
                return PollOptionResponse.builder()
                    .id(option.getId())
                    .optionText(option.getOptionText())
                    .optionOrder(option.getOptionOrder())
                    .voteCount(voteCount)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .voters(voters)
                    .build();
            })
            .collect(Collectors.toList());
        response.setOptions(optionResponses);
        
        // Get current user's voted option IDs
        if (currentUserId != null) {
            List<Long> votedOptionIds = poll.getVotes().stream()
                .filter(v -> v.getUser().getId().equals(currentUserId))
                .map(v -> v.getPollOption().getId())
                .collect(Collectors.toList());
            response.setCurrentUserVotedOptionIds(votedOptionIds);
        }
        
        return response;
    }
}
