import java.util.*;

/**
 * Seed conversations and messages for testing
 * Creates 5-10 conversations for user1 with multiple messages
 */
public class SeedConversations {

    private static final Random rand = new Random();

    // Sample message contents
    private static final String[] GREETINGS = {
            "Chào bạn!", "Hi!", "Hello!", "Hey!", "Xin chào!",
            "Chào buổi sáng!", "Good morning!", "Chào buổi chiều!"
    };

    private static final String[] QUESTIONS = {
            "Bạn khỏe không?", "Hôm nay thế nào?", "Có gì mới không?",
            "Bạn đang làm gì đấy?", "Cuối tuần có rảnh không?",
            "Bạn có thời gian không?", "Ăn cơm chưa?", "Đi đâu đấy?"
    };

    private static final String[] RESPONSES = {
            "Mình khỏe, cảm ơn bạn!", "Tốt lắm!", "Bình thường thôi",
            "Đang làm việc đây", "Rảnh đấy, sao?", "Có chứ!",
            "Ăn rồi!", "Đang ở nhà", "Chưa có kế hoạch gì"
    };

    private static final String[] CASUAL_MESSAGES = {
            "Haha đúng rồi 😂", "Oke nhé!", "Được đấy!", "Tuyệt vời!",
            "Cảm ơn bạn nhiều!", "Không sao đâu", "Để mình xem đã",
            "Mình nghĩ vậy cũng được", "Sounds good!", "Alright!",
            "👍", "😊", "🎉", "💯", "Chill thôi", "No problem!"
    };

    private static final String[] LONG_MESSAGES = {
            "Mình nghĩ là chúng ta nên thử cách tiếp cận mới này. Nó có vẻ hiệu quả hơn và tiết kiệm thời gian hơn nhiều so với cách cũ.",
            "Hôm qua mình có đi xem phim mới, hay lắm! Bạn nên đi xem thử, mình nghĩ bạn sẽ thích đấy.",
            "Project này deadline tuần sau rồi, chúng ta cần tăng tốc một chút. Bạn có thể giúp mình phần UI được không?",
            "Mình vừa đọc một bài viết rất hay về AI, nó nói về tương lai của công nghệ. Để mình gửi link cho bạn nhé.",
            "Cuối tuần này mình định tổ chức một buổi gặp mặt nhỏ, bạn có rảnh không? Sẽ có nhiều người quen đấy!",
            "Cảm ơn bạn đã giúp đỡ mình hôm qua. Nhờ bạn mà mình đã hoàn thành công việc đúng hạn. Mình nợ bạn một bữa cà phê!",
            "Mình đang học một khóa học mới về web development, khá thú vị đấy. Bạn có muốn học cùng không?"
    };

    public static void main(String[] args) {
        System.out.println("-- ========================================");
        System.out.println("-- SEED CONVERSATIONS & MESSAGES FOR USER1");
        System.out.println("-- ========================================\n");

        // Create 8 conversations (mix of DIRECT and GROUP)
        int numConversations = 8;
        List<ConversationData> conversations = new ArrayList<>();

        // 6 DIRECT conversations (user1 with user2-7)
        for (int i = 2; i <= 7; i++) {
            conversations.add(new ConversationData(i, "DIRECT", null));
        }

        // 2 GROUP conversations
        conversations.add(new ConversationData(0, "GROUP", "Team Project"));
        conversations.add(new ConversationData(0, "GROUP", "Friends Hangout"));

        // Generate SQL
        generateConversationSQL(conversations);
        generateParticipantSQL(conversations);
        generateMessageSQL(conversations);

        System.out.println("\n-- ========================================");
        System.out.println("-- SEED COMPLETED!");
        System.out.println("-- Total conversations: " + conversations.size());
        System.out.println("-- ========================================");
    }

    private static void generateConversationSQL(List<ConversationData> conversations) {
        System.out.println("-- ========================================");
        System.out.println("-- 1. CREATE CONVERSATIONS");
        System.out.println("-- ========================================\n");

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO conversations (name, type, avatar_url, description, created_at, updated_at, metadata) VALUES\n");

        for (int i = 0; i < conversations.size(); i++) {
            ConversationData conv = conversations.get(i);
            conv.id = i + 1; // Assign conversation ID

            String name = conv.type.equals("DIRECT") ? "NULL" : "'" + conv.name + "'";
            String avatarUrl = conv.type.equals("GROUP") ? "'https://ui-avatars.com/api/?background=4DB6AC&color=ffffff&name=" + conv.name.replace(" ", "+") + "'" : "NULL";
            String description = conv.type.equals("GROUP") ? "'Group chat for " + conv.name + "'" : "NULL";

            sql.append(String.format("  (%s, '%s', %s, %s, NOW() - INTERVAL '%d days', NOW() - INTERVAL '%d hours', '{}'::jsonb)",
                    name, conv.type, avatarUrl, description,
                    rand.nextInt(30) + 1,  // Created 1-30 days ago
                    rand.nextInt(24)       // Updated 0-23 hours ago
            ));

            if (i < conversations.size() - 1) {
                sql.append(",\n");
            } else {
                sql.append(";\n");
            }
        }

        System.out.println(sql.toString());
    }

    private static void generateParticipantSQL(List<ConversationData> conversations) {
        System.out.println("-- ========================================");
        System.out.println("-- 2. CREATE CONVERSATION PARTICIPANTS");
        System.out.println("-- ========================================\n");

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO conversation_participants (conversation_id, user_id, role, joined_at, unread_count, last_read_at, archived, muted, pinned, notifications_enabled) VALUES\n");

        List<String> rows = new ArrayList<>();

        for (ConversationData conv : conversations) {
            if (conv.type.equals("DIRECT")) {
                // DIRECT: user1 and one other user
                rows.add(String.format("  (%d, 1, 'ADMIN', NOW() - INTERVAL '%d days', %d, NOW() - INTERVAL '%d hours', false, false, false, true)",
                        conv.id, rand.nextInt(30) + 1, rand.nextInt(5), rand.nextInt(24)));
                rows.add(String.format("  (%d, %d, 'MEMBER', NOW() - INTERVAL '%d days', 0, NOW(), false, false, false, true)",
                        conv.id, conv.otherUserId, rand.nextInt(30) + 1));
            } else {
                // GROUP: user1 (admin) + 3-5 random users
                rows.add(String.format("  (%d, 1, 'ADMIN', NOW() - INTERVAL '%d days', %d, NOW() - INTERVAL '%d hours', false, false, false, true)",
                        conv.id, rand.nextInt(30) + 1, rand.nextInt(8), rand.nextInt(24)));

                int numMembers = 3 + rand.nextInt(3); // 3-5 members
                Set<Integer> usedUsers = new HashSet<>();
                usedUsers.add(1); // user1 already added

                for (int i = 0; i < numMembers; i++) {
                    int userId;
                    do {
                        userId = 2 + rand.nextInt(19); // user2-20
                    } while (usedUsers.contains(userId));
                    usedUsers.add(userId);

                    String role = i == 0 ? "ADMIN" : "MEMBER";
                    rows.add(String.format("  (%d, %d, '%s', NOW() - INTERVAL '%d days', 0, NOW(), false, false, false, true)",
                            conv.id, userId, role, rand.nextInt(30) + 1));
                }
            }
        }

        for (int i = 0; i < rows.size(); i++) {
            sql.append(rows.get(i));
            if (i < rows.size() - 1) {
                sql.append(",\n");
            } else {
                sql.append(";\n");
            }
        }

        System.out.println(sql.toString());
    }

    private static void generateMessageSQL(List<ConversationData> conversations) {
        System.out.println("-- ========================================");
        System.out.println("-- 3. CREATE MESSAGES");
        System.out.println("-- ========================================\n");

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO messages (conversation_id, sender_id, content, type, sent_at, created_at, updated_at, edited, deleted, metadata, reactions, mentions, forwarded, forward_count, pinned, scheduled, scheduled_status) VALUES\n");

        List<String> rows = new ArrayList<>();

        for (ConversationData conv : conversations) {
            // Generate 15-30 messages per conversation
            int numMessages = 15 + rand.nextInt(16);

            for (int i = 0; i < numMessages; i++) {
                // Determine sender (alternate between user1 and others)
                int senderId;
                if (conv.type.equals("DIRECT")) {
                    senderId = (i % 2 == 0) ? 1 : conv.otherUserId;
                } else {
                    // For groups, randomly pick from participants
                    senderId = (i % 3 == 0) ? 1 : (2 + rand.nextInt(5));
                }

                // Pick message content
                String content = getRandomMessage(i);
                content = content.replace("'", "''"); // Escape quotes

                // Calculate timestamp (spread over last 7 days)
                int daysAgo = rand.nextInt(7);
                int hoursAgo = rand.nextInt(24);
                int minutesAgo = rand.nextInt(60);

                // Some messages have reactions
                String reactions = "'{}'::jsonb";
                if (rand.nextInt(100) < 20) { // 20% chance of reactions
                    reactions = "'{\"👍\": [" + senderId + "], \"❤️\": [" + (senderId == 1 ? 2 : 1) + "]}'::jsonb";
                }

                rows.add(String.format(
                        "  (%d, %d, '%s', 'TEXT', NOW() - INTERVAL '%d days %d hours %d minutes', NOW() - INTERVAL '%d days %d hours %d minutes', NOW() - INTERVAL '%d days %d hours %d minutes', false, false, '{}'::jsonb, %s, NULL, false, 0, false, false, NULL)",
                        conv.id, senderId, content,
                        daysAgo, hoursAgo, minutesAgo,
                        daysAgo, hoursAgo, minutesAgo,
                        daysAgo, hoursAgo, minutesAgo,
                        reactions
                ));
            }
        }

        for (int i = 0; i < rows.size(); i++) {
            sql.append(rows.get(i));
            if (i < rows.size() - 1) {
                sql.append(",\n");
            } else {
                sql.append(";\n");
            }
        }

        System.out.println(sql.toString());

        // Update last_message_id for conversations
        System.out.println("\n-- ========================================");
        System.out.println("-- 4. UPDATE LAST MESSAGE REFERENCES");
        System.out.println("-- ========================================\n");

        System.out.println("-- Update last_message_id for each conversation");
        System.out.println("UPDATE conversations c");
        System.out.println("SET last_message_id = (");
        System.out.println("    SELECT m.id");
        System.out.println("    FROM messages m");
        System.out.println("    WHERE m.conversation_id = c.id");
        System.out.println("    ORDER BY m.sent_at DESC");
        System.out.println("    LIMIT 1");
        System.out.println(");\n");
    }

    private static String getRandomMessage(int index) {
        // Vary message types for natural conversation
        if (index == 0) {
            return GREETINGS[rand.nextInt(GREETINGS.length)];
        } else if (index % 5 == 1) {
            return QUESTIONS[rand.nextInt(QUESTIONS.length)];
        } else if (index % 5 == 2) {
            return RESPONSES[rand.nextInt(RESPONSES.length)];
        } else if (index % 10 == 0) {
            return LONG_MESSAGES[rand.nextInt(LONG_MESSAGES.length)];
        } else {
            return CASUAL_MESSAGES[rand.nextInt(CASUAL_MESSAGES.length)];
        }
    }

    static class ConversationData {
        int id;
        int otherUserId; // For DIRECT conversations
        String type; // DIRECT or GROUP
        String name; // For GROUP conversations

        public ConversationData(int otherUserId, String type, String name) {
            this.otherUserId = otherUserId;
            this.type = type;
            this.name = name;
        }
    }
}
