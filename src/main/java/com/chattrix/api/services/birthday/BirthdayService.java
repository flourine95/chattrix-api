package com.chattrix.api.services.birthday;

import com.chattrix.api.entities.Conversation;
import com.chattrix.api.entities.ConversationParticipant;
import com.chattrix.api.entities.User;
import com.chattrix.api.exceptions.BusinessException;
import com.chattrix.api.repositories.ConversationRepository;
import com.chattrix.api.repositories.UserRepository;
import com.chattrix.api.requests.ChatMessageRequest;
import com.chattrix.api.requests.SendBirthdayWishesRequest;
import com.chattrix.api.responses.BirthdayUserResponse;
import com.chattrix.api.services.message.MessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class BirthdayService {

    @Inject
    UserRepository userRepository;

    @Inject
    ConversationRepository conversationRepository;

    @Inject
    MessageService messageService;

    /**
     * Get users whose birthday is today (filtered by current user's contacts and conversations)
     */
    @Transactional
    public List<BirthdayUserResponse> getUsersWithBirthdayToday(Long currentUserId) {
        List<User> allUsers = userRepository.findUsersWithBirthdayToday();
        List<User> relevantUsers = filterRelevantUsers(allUsers, currentUserId);

        return relevantUsers.stream()
                .map(user -> toBirthdayUserResponse(user, 0))
                .collect(Collectors.toList());
    }

    /**
     * Get users whose birthday is within the next N days (filtered by current user's contacts and conversations)
     */
    @Transactional
    public List<BirthdayUserResponse> getUsersWithUpcomingBirthdays(int daysAhead, Long currentUserId) {
        List<User> allUsers = userRepository.findUsersWithUpcomingBirthdays(daysAhead);
        List<User> relevantUsers = filterRelevantUsers(allUsers, currentUserId);
        LocalDate today = LocalDate.now();

        List<BirthdayUserResponse> upcomingBirthdays = new ArrayList<>();

        for (User user : relevantUsers) {
            if (user.getDateOfBirth() == null) continue;

            LocalDate birthDate = user.getDateOfBirth().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate nextBirthday = getNextBirthday(birthDate, today);

            long daysUntil = ChronoUnit.DAYS.between(today, nextBirthday);

            if (daysUntil >= 0 && daysUntil <= daysAhead) {
                upcomingBirthdays.add(toBirthdayUserResponse(user, (int) daysUntil));
            }
        }

        upcomingBirthdays.sort((a, b) -> {
            int daysA = getDaysUntilBirthday(a.getBirthdayMessage());
            int daysB = getDaysUntilBirthday(b.getBirthdayMessage());
            return Integer.compare(daysA, daysB);
        });

        return upcomingBirthdays;
    }

    /**
     * Get birthdays in a specific conversation
     */
    @Transactional
    public List<BirthdayUserResponse> getBirthdaysInConversation(Long conversationId, int daysAhead) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> BusinessException.notFound("Conversation not found", "CONVERSATION_NOT_FOUND"));

        List<User> members = conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> user.getDateOfBirth() != null)
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        List<BirthdayUserResponse> birthdays = new ArrayList<>();

        for (User user : members) {
            LocalDate birthDate = user.getDateOfBirth().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate nextBirthday = getNextBirthday(birthDate, today);
            long daysUntil = ChronoUnit.DAYS.between(today, nextBirthday);

            if (daysUntil >= 0 && daysUntil <= daysAhead) {
                birthdays.add(toBirthdayUserResponse(user, (int) daysUntil));
            }
        }

        birthdays.sort((a, b) -> {
            int daysA = getDaysUntilBirthday(a.getBirthdayMessage());
            int daysB = getDaysUntilBirthday(b.getBirthdayMessage());
            return Integer.compare(daysA, daysB);
        });

        return birthdays;
    }

    /**
     * Filter users to only include those relevant to current user (contacts + conversation members)
     */
    private List<User> filterRelevantUsers(List<User> users, Long currentUserId) {
        // Get all conversations current user is in
        List<Conversation> userConversations = conversationRepository.findByUserId(currentUserId);

        // Get all user IDs from those conversations
        java.util.Set<Long> relevantUserIds = userConversations.stream()
                .flatMap(conv -> conv.getParticipants().stream())
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());

        // Filter users
        return users.stream()
                .filter(user -> relevantUserIds.contains(user.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Send birthday wishes to a user in specified conversations
     */
    @Transactional
    public void sendBirthdayWishes(SendBirthdayWishesRequest request, Long senderId) {
        // Validate user exists
        User birthdayUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> BusinessException.notFound("User not found", "USER_NOT_FOUND"));

        // Validate sender exists
        userRepository.findById(senderId)
                .orElseThrow(() -> BusinessException.notFound("Sender not found", "USER_NOT_FOUND"));

        // Generate birthday message
        String message = generateBirthdayMessage(birthdayUser, request.getCustomMessage());

        // Send message to each conversation
        for (Long conversationId : request.getConversationIds()) {
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> BusinessException.notFound("Conversation not found: " + conversationId, "CONVERSATION_NOT_FOUND"));

            // Verify sender is participant in conversation
            boolean isMember = conversation.getParticipants().stream()
                    .anyMatch(p -> p.getUser().getId().equals(senderId));

            if (!isMember) {
                throw BusinessException.forbidden("You are not a member of conversation: " + conversationId);
            }

            // Create message request with mention
            ChatMessageRequest messageRequest = new ChatMessageRequest(
                    message,
                    "TEXT",
                    null, null, null, null, null,
                    null, null, null,
                    null,
                    List.of(birthdayUser.getId())
            );

            // Send message using MessageService
            messageService.sendMessage(senderId, conversationId, messageRequest);
        }
    }

    /**
     * Auto-send birthday wishes to all group conversations where birthday user is a member
     * This is called by the scheduled job
     */
    @Transactional
    public void autoSendBirthdayWishes(User birthdayUser) {
        // Get all group conversations where this user is a member
        List<Conversation> conversations = conversationRepository.findByUserId(birthdayUser.getId())
                .stream()
                .filter(c -> c.getType() == Conversation.ConversationType.GROUP)
                .collect(Collectors.toList());

        if (conversations.isEmpty()) {
            return;
        }

        // Generate birthday message
        String message = generateAutoBirthdayMessage(birthdayUser);

        // Send message to each group conversation
        // Use first participant (not birthday user) as sender
        for (Conversation conversation : conversations) {
            // Get first participant as sender (could be improved to use a system user)
            User systemSender = conversation.getParticipants().stream()
                    .map(ConversationParticipant::getUser)
                    .filter(u -> !u.getId().equals(birthdayUser.getId()))
                    .findFirst()
                    .orElse(null);

            if (systemSender == null) continue;

            // IMPORTANT: Only mention the birthday user if they are actually in this conversation
            // Check if birthday user is a participant
            boolean isBirthdayUserInConversation = conversation.getParticipants().stream()
                    .anyMatch(p -> p.getUser().getId().equals(birthdayUser.getId()));

            // Create message request with mention ONLY if user is in conversation
            ChatMessageRequest messageRequest = new ChatMessageRequest(
                    message,
                    "SYSTEM",
                    null, null, null, null, null,
                    null, null, null,
                    null,
                    isBirthdayUserInConversation ? List.of(birthdayUser.getId()) : null
            );

            try {
                // Send message using MessageService
                messageService.sendMessage(systemSender.getId(), conversation.getId(), messageRequest);
            } catch (Exception e) {
                System.err.println("Failed to send birthday message to conversation " + 
                        conversation.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Check and send birthday wishes for all users with birthday today
     * Called by scheduled job at midnight
     */
    @Transactional
    public void checkAndSendBirthdayWishes() {
        List<User> birthdayUsers = userRepository.findUsersWithBirthdayToday();
        
        for (User user : birthdayUsers) {
            try {
                autoSendBirthdayWishes(user);
            } catch (Exception e) {
                // Log error but continue with other users
                System.err.println("Failed to send birthday wishes for user " + user.getId() + ": " + e.getMessage());
            }
        }
    }

    // Helper methods

    private BirthdayUserResponse toBirthdayUserResponse(User user, int daysUntil) {
        Integer age = calculateAge(user.getDateOfBirth());
        String birthdayMessage = getBirthdayMessage(daysUntil);

        return BirthdayUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .dateOfBirth(user.getDateOfBirth())
                .age(age)
                .birthdayMessage(birthdayMessage)
                .build();
    }

    private Integer calculateAge(Instant dateOfBirth) {
        if (dateOfBirth == null) return null;

        LocalDate birthDate = dateOfBirth.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();

        // Calculate age
        int age = Period.between(birthDate, today).getYears();

        // If birth date is in the future or today (age would be 0 or negative),
        // this likely means only month/day are significant (test data scenario)
        // Return 0 to indicate current year birth or invalid data
        if (age < 0) {
            return 0;
        }

        return age;
    }

    private String getBirthdayMessage(int daysUntil) {
        if (daysUntil == 0) {
            return "Hôm nay";
        } else if (daysUntil == 1) {
            return "Ngày mai";
        } else {
            return "Còn " + daysUntil + " ngày";
        }
    }

    private int getDaysUntilBirthday(String birthdayMessage) {
        if (birthdayMessage.equals("Hôm nay")) return 0;
        if (birthdayMessage.equals("Ngày mai")) return 1;
        
        // Extract number from "Còn X ngày"
        String[] parts = birthdayMessage.split(" ");
        if (parts.length >= 2) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.MAX_VALUE;
    }

    private LocalDate getNextBirthday(LocalDate birthDate, LocalDate today) {
        LocalDate nextBirthday = LocalDate.of(today.getYear(), birthDate.getMonth(), birthDate.getDayOfMonth());
        
        if (nextBirthday.isBefore(today) || nextBirthday.isEqual(today)) {
            nextBirthday = nextBirthday.plusYears(1);
        }
        
        return nextBirthday;
    }

    private String generateBirthdayMessage(User birthdayUser, String customMessage) {
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            return customMessage;
        }

        Integer age = calculateAge(birthdayUser.getDateOfBirth());
        String ageText = age != null ? " (" + age + " tuổi)" : "";

        return String.format("🎂 Chúc mừng sinh nhật @%s%s! 🎉", 
                birthdayUser.getUsername(), ageText);
    }

    private String generateAutoBirthdayMessage(User birthdayUser) {
        Integer age = calculateAge(birthdayUser.getDateOfBirth());
        String ageText = age != null ? " (" + age + " tuổi)" : "";

        return String.format("🎂 Hôm nay là sinh nhật của @%s%s! Hãy cùng chúc mừng nhé! 🎉🎈", 
                birthdayUser.getUsername(), ageText);
    }

    /**
     * Debug method to check timezone handling for a user
     */
    public java.util.Map<String, Object> debugUserBirthday(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found", "USER_NOT_FOUND"));

        if (user.getDateOfBirth() == null) {
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", "User has no date of birth set");
            return error;
        }

        Instant dob = user.getDateOfBirth();
        LocalDate dobLocal = dob.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate dobUTC = dob.atZone(ZoneId.of("UTC")).toLocalDate();
        LocalDate today = LocalDate.now();
        LocalDate todayUTC = LocalDate.now(ZoneId.of("UTC"));

        java.util.Map<String, Object> debug = new java.util.HashMap<>();
        debug.put("userId", user.getId());
        debug.put("username", user.getUsername());
        debug.put("dateOfBirth_instant", dob.toString());
        debug.put("dateOfBirth_localDate_systemTZ", dobLocal.toString());
        debug.put("dateOfBirth_localDate_UTC", dobUTC.toString());
        debug.put("today_systemTZ", today.toString());
        debug.put("today_UTC", todayUTC.toString());
        debug.put("systemTimezone", ZoneId.systemDefault().toString());
        debug.put("month_match", dobLocal.getMonthValue() == today.getMonthValue());
        debug.put("day_match", dobLocal.getDayOfMonth() == today.getDayOfMonth());
        debug.put("is_birthday_today", (dobLocal.getMonthValue() == today.getMonthValue() && 
                                        dobLocal.getDayOfMonth() == today.getDayOfMonth()));
        
        return debug;
    }
}
