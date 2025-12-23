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

    // Danh sách Bio đa dạng, có Emoji
    private static final String[] RANDOM_BIOS = {
            "Always coding, never sleeping. 💻",
            "Music is my escape. 🎧",
            "Coffee lover & tech enthusiast. ☕",
            "Just a dreamer chasing stars. ✨",
            "Available for freelance work. 📩",
            "Life is short, make it sweet.",
            "Exploring the world, one city at a time. 🌍",
            "Gamer at heart. 🎮",
            "Silence is the best answer.",
            "Working hard in silence. 🚀",
            "Here for a good time, not a long time.",
            "Photography is my passion. 📸",
            "Foodie & Travel addict. 🍜",
            "Simplicity is the ultimate sophistication.",
            "Do what you love, love what you do. ❤️",
            "Catch flights, not feelings. ✈️",
            "Just another day in paradise. 🌴",
            "Trying to be a rainbow in someone's cloud. 🌈",
            "Less talk, more action. 💪",
            "Stay hungry, stay foolish."
    };

    static class UserData {
        String fullName;
        String username;
        String email;

        public UserData(String fullName, int index) {
            this.fullName = fullName;
            String slug = removeAccents(fullName).toLowerCase().replace(" ", "");
            this.username = slug + index;
            this.email = slug + index + "@example.com";
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

        // Header SQL
        String sqlHeader = "INSERT INTO users (full_name, username, email, password, avatar_url, phone, bio, gender, online, email_verified, profile_visibility, created_at, updated_at, last_seen) VALUES ";

        Random rand = new Random();
        StringBuilder sqlBuilder = new StringBuilder();

        System.out.println("⏳ Đang xử lý upload ảnh và tạo SQL... Vui lòng đợi!");

        for (int i = 0; i < users.size(); i++) {
            UserData user = users.get(i);
            int index = i + 1;

            try {
                // 1. Upload Avatar
                int colorIndex = (index - 1) % THEME_COLORS.length;
                String color = THEME_COLORS[colorIndex];

                String avatarSourceUrl = "https://ui-avatars.com/api/?" +
                        "background=" + color +
                        "&color=ffffff" +
                        "&size=512" +
                        "&bold=true" +
                        "&font-size=0.4" +
                        "&length=2" +
                        "&rounded=false" +
                        "&name=" + URLEncoder.encode(user.fullName, StandardCharsets.UTF_8);

                var params = ObjectUtils.asMap(
                        "public_id", "avatars/user_v2_" + index,
                        "overwrite", true,
                        "resource_type", "image"
                );
                var uploadResult = cloudinary.uploader().upload(avatarSourceUrl, params);
                String avatarUrl = (String) uploadResult.get("secure_url");

                // 2. Random Data
                String gender = rand.nextBoolean() ? "MALE" : "FEMALE";
                String phone = "09" + (10000000 + rand.nextInt(90000000));

                // Chọn Bio ngẫu nhiên
                String rawBio = RANDOM_BIOS[rand.nextInt(RANDOM_BIOS.length)];
                // QUAN TRỌNG: Escape dấu nháy đơn trong SQL (ví dụ: I'm -> I''m)
                String sqlBio = rawBio.replace("'", "''");

                // 3. Format SQL Row
                String valueRow = String.format(
                        "('%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', %b, %b, '%s', NOW(), NOW(), NOW())",
                        user.fullName,
                        user.username,
                        user.email,
                        hashedPassword,
                        avatarUrl,
                        phone,
                        sqlBio, // Đã xử lý escape
                        gender,
                        false, // online
                        true,  // email_verified
                        "PUBLIC"
                );

                sqlBuilder.append(valueRow);
                if (i < users.size() - 1) {
                    sqlBuilder.append(",\n");
                } else {
                    sqlBuilder.append(";");
                }

                System.out.println("-- ✅ Done: " + user.username);

            } catch (Exception e) {
                System.err.println("-- ❌ Error generating user " + user.username + ": " + e.getMessage());
            }
        }

        System.out.println("\n\n-- 👇👇👇 COPY ĐOẠN DƯỚI ĐÂY 👇👇👇");
        System.out.println("----------------------------------------------------------------");
        System.out.println(sqlHeader);
        System.out.println(sqlBuilder);
        System.out.println("----------------------------------------------------------------");
    }

    public static String removeAccents(String text) {
        String[] accents = new String[]{
                "aàáạảãâầấậẩẫăằắặẳẵ", "eèéẹẻẽêềếệểễ", "iìíịỉĩ",
                "oòóọỏõôồốộổỗơờớợởỡ", "uùúụủũưừứựửữ", "yỳýỵỷỹ", "dđ"
        };
        for (String str : accents) {
            for (int i = 1; i < str.length(); i++) {
                text = text.replace(str.charAt(i), str.charAt(0));
            }
        }
        return text;
    }
}