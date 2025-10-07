package com.chattrix.api.resources;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.services.TokenService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Path("/test")
public class TestSetupResource {

    @Inject
    private UserRepository userRepository;

    @Inject
    private ConversationRepository conversationRepository;

    @Inject
    private TokenService tokenService;

    @POST
    @Path("/setup")
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public Response setupTestData() {
        // 1. Create two users if they don't exist
        User user1 = userRepository.findByUsername("testuser1").orElseGet(() -> {
            User u = new User();
            u.setUsername("testuser1");
            u.setDisplayName("Test User 1");
            u.setPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
            userRepository.save(u);
            return u;
        });

        User user2 = userRepository.findByUsername("testuser2").orElseGet(() -> {
            User u = new User();
            u.setUsername("testuser2");
            u.setDisplayName("Test User 2");
            u.setPassword(BCrypt.hashpw("password", BCrypt.gensalt()));
            userRepository.save(u);
            return u;
        });

        // 2. Create a conversation between them
        Conversation conversation = new Conversation();
        conversation.setType(Conversation.ConversationType.DIRECT);

        ConversationParticipant participant1 = new ConversationParticipant();
        participant1.setUser(user1);
        participant1.setConversation(conversation);
        participant1.setRole(ConversationParticipant.Role.MEMBER);

        ConversationParticipant participant2 = new ConversationParticipant();
        participant2.setUser(user2);
        participant2.setConversation(conversation);
        participant2.setRole(ConversationParticipant.Role.MEMBER);

        conversation.setParticipants(Set.of(participant1, participant2));
        conversationRepository.save(conversation);

        // 3. Generate a token for user1
        String token = tokenService.generateToken(user1);

        // 4. Return the necessary data
        Map<String, String> responseData = new HashMap<>();
        responseData.put("message", "Test data created successfully.");
        responseData.put("conversationId", conversation.getId().toString());
        responseData.put("tokenForUser1", token);
        responseData.put("user1_username", user1.getUsername());
        responseData.put("user2_username", user2.getUsername());

        return Response.ok(responseData).build();
    }
}

