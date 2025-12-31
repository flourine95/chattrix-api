import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SeedUser {

    private static final String CLOUDINARY_URL = "cloudinary://142117447527122:j45rpq-NJkqkwmv7_nOEIHAw_2I@dk3gud5kq";

    private static final String[] THEME_COLORS = {
            "E57373", "BA68C8", "7986CB", "4DB6AC", "4DD0E1",
            "9575CD", "F06292", "64B5F6", "4FC3F7", "81C784",
            "A1887F", "90A4AE", "FF8A65", "D4E157", "FFD54F",
            "FFB74D", "AED581", "E0E0E0", "78909C", "5C6BC0"
    };

    private static final String[] RANDOM_BIOS = {
            "Always coding, never sleeping. 💻", "Music is my escape. 🎧",
            "Coffee lover & tech enthusiast. ☕", "Just a dreamer chasing stars. ✨",
            "Available for freelance work. 📩", "Life is short, make it sweet.",
            "Exploring the world, one city at a time. 🌍", "Gamer at heart. 🎮",
            "Silence is the best answer.", "Working hard in silence. 🚀",
            "Photography is my passion. 📸", "Foodie & Travel addict. 🍜",
            "Do what you love, love what you do. ❤️", "Catch flights, not feelings. ✈️"
    };

    private static final String[] LOCATIONS = {
            "Hanoi, Vietnam", "Ho Chi Minh City, Vietnam", "Da Nang, Vietnam",
            "Can Tho, Vietnam", "Hai Phong, Vietnam", "Da Lat, Vietnam"
    };

    static class UserData {
        String fullName;
        String username;
        String email;

        public UserData(String fullName, int index) {
            this.fullName = fullName;
            this.username = "user" + index;
            this.email = "user" + index + "@example.com";
        }
    }

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    public static void main(String[] args) {
        Cloudinary cloudinary = new Cloudinary(CLOUDINARY_URL);
        cloudinary.config.secure = true;

        String rawPassword = "password";
        String hashedPassword = hashPassword(rawPassword);

        String[] names = {
                "Nguyen Linh La", "Tran Van Binh", "Le Thi Hoa", "Pham Minh Tuan", "Vo Thu Thuy",
                "Dang Van Thanh", "Bui Thi Lan", "Do Quang Huy", "Hoang Minh Tri", "Ngo Bao Chau",
                "Duong Thuy Vi", "Ly Van Hung", "Cao Thai Son", "Vuong Dinh Vu", "Trinh Van Son",
                "Dao Thi Mai", "Phan Van Duc", "Lam Truong Giang", "Ha Tuan Anh", "Thai Thuy Linh"
        };

        List<UserData> users = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            users.add(new UserData(names[i], i + 1));
        }

        // Header SQL sắp xếp theo đúng thứ tự bạn yêu cầu (không bao gồm id vì auto-increment)
        String sqlHeader = "INSERT INTO users (avatar_url, bio, created_at, date_of_birth, email, email_verified, full_name, gender, last_seen, location, note_metadata, password, phone, profile_visibility, updated_at, username) VALUES ";

        Random rand = new Random();
        StringBuilder sqlBuilder = new StringBuilder();

        for (int i = 0; i < users.size(); i++) {
            UserData user = users.get(i);
            try {
                int colorIndex = i % THEME_COLORS.length;
                String color = THEME_COLORS[colorIndex];

                String avatarSourceUrl = "https://ui-avatars.com/api/?background=" + color + "&color=ffffff&name=" + URLEncoder.encode(user.fullName, StandardCharsets.UTF_8);

                var params = ObjectUtils.asMap("public_id", "avatars/" + user.username, "overwrite", true, "resource_type", "image");
                var uploadResult = cloudinary.uploader().upload(avatarSourceUrl, params);
                String avatarUrl = (String) uploadResult.get("secure_url");

                String gender = rand.nextBoolean() ? "MALE" : "FEMALE";
                String phone = "09" + (10000000 + rand.nextInt(90000000));
                String sqlBio = RANDOM_BIOS[rand.nextInt(RANDOM_BIOS.length)].replace("'", "''");

                int year = 1990 + rand.nextInt(16);
                String dob = String.format("%d-12-25 06:25:17.861000 +00:00", year);

                String location = LOCATIONS[rand.nextInt(LOCATIONS.length)];

                // Format row theo đúng thứ tự cột:
                // avatar_url, bio, created_at, date_of_birth, email, email_verified, full_name, gender, last_seen, location, note_metadata, password, phone, profile_visibility, updated_at, username
                String valueRow = String.format(
                        "('%s', '%s', NOW(), '%s', '%s', %b, '%s', '%s', NOW(), '%s', %s, '%s', '%s', '%s', NOW(), '%s')",
                        avatarUrl,          // avatar_url
                        sqlBio,             // bio
                        dob,                // date_of_birth
                        user.email,         // email
                        true,               // email_verified
                        user.fullName,      // full_name
                        gender,             // gender
                        location,           // location
                        "NULL",             // note_metadata (để NULL nếu không có data)
                        hashedPassword,     // password
                        phone,              // phone
                        "PUBLIC",           // profile_visibility
                        user.username       // username
                );

                sqlBuilder.append(valueRow);
                if (i < users.size() - 1) sqlBuilder.append(",\n"); else sqlBuilder.append(";");

                System.out.println("-- ✅ Processed: " + user.username);
            } catch (Exception e) {
                System.err.println("-- ❌ Error: " + user.username + " - " + e.getMessage());
            }
        }

        System.out.println("\n-- SQL SCRIPT --\n" + sqlHeader + "\n" + sqlBuilder);
    }
}