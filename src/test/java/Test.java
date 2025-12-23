import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Test {

    // 🎨 Bảng màu Modern Soft - Không chói, dễ nhìn, phù hợp cả Dark/Light mode
    // Được thiết kế theo Material Design 3 và Messenger/WhatsApp
    private static final String[] THEME_COLORS = {
            "5B8FB9", // 0. Soft Blue - Xanh dương nhẹ nhàng
            "7B68A6", // 1. Soft Purple - Tím pastel
            "6B9080", // 2. Sage Green - Xanh lá nhạt
            "B85C5C", // 3. Dusty Rose - Hồng đất
            "8B7355", // 4. Warm Brown - Nâu ấm
            "5C8374", // 5. Forest Green - Xanh rêu
            "9B6B9E", // 6. Mauve - Tím hoa cà
            "6B8E9F", // 7. Steel Blue - Xanh thép
            "A67B5B", // 8. Caramel - Màu caramel
            "7A9D96", // 9. Teal - Xanh ngọc nhạt
            "9B7E7E", // 10. Taupe - Nâu xám
            "6B8BA4", // 11. Slate Blue - Xanh slate
            "8B8B7A", // 12. Olive - Ô liu
            "A67C8E", // 13. Dusty Pink - Hồng khói
            "6B9B9B", // 14. Aqua - Xanh nước biển
            "9B8B6B", // 15. Sand - Màu cát
            "7B7BA6", // 16. Periwinkle - Tím nhạt
            "8B9B7A", // 17. Moss - Rêu
            "A68B7B", // 18. Terracotta - Đất nung
            "7A8B9B", // 19. Denim - Xanh jean
    };

    // Class nội bộ để giữ thông tin User
    static class UserData {
        String id;
        String fullName;

        public UserData(String id, String fullName) {
            this.id = id;
            this.fullName = fullName;
        }
    }

    public static void main(String[] args) {
        // Cấu hình Cloudinary
        String CLOUDINARY_URL = "cloudinary://142117447527122:j45rpq-NJkqkwmv7_nOEIHAw_2I@dk3gud5kq";
        Cloudinary cloudinary = new Cloudinary(CLOUDINARY_URL);
        cloudinary.config.secure = true;

        // Danh sách 20 User
        List<UserData> users = new ArrayList<>();
        users.add(new UserData("1", "Nguyen Linh La"));
        users.add(new UserData("2", "Tran Van Binh"));
        users.add(new UserData("3", "Le Thi Hoa"));
        users.add(new UserData("4", "Pham Minh Tuan"));
        users.add(new UserData("5", "Vo Thu Thuy"));
        users.add(new UserData("6", "Dang Van Thanh"));
        users.add(new UserData("7", "Bui Thi Lan"));
        users.add(new UserData("8", "Do Quang Huy"));
        users.add(new UserData("9", "Hoang Minh Tri"));
        users.add(new UserData("10", "Ngo Bao Chau"));
        users.add(new UserData("11", "Duong Thuy Vi"));
        users.add(new UserData("12", "Ly Van Hung"));
        users.add(new UserData("13", "Cao Thai Son"));
        users.add(new UserData("14", "Vuong Dinh Vu"));
        users.add(new UserData("15", "Trinh Van Son"));
        users.add(new UserData("16", "Dao Thi Mai"));
        users.add(new UserData("17", "Phan Van Duc"));
        users.add(new UserData("18", "Lam Truong Giang"));
        users.add(new UserData("19", "Ha Tuan Anh"));
        users.add(new UserData("20", "Thai Thuy Linh"));

        System.out.println("🎨 Bắt đầu upload 20 avatars với màu Modern Soft...\n");

        for (UserData user : users) {
            try {
                // 1. Chọn màu dựa trên ID
                int colorIndex = (Integer.parseInt(user.id) - 1) % THEME_COLORS.length;
                String color = THEME_COLORS[colorIndex];

                // 2. Tạo URL từ UI Avatars
                // Tăng size lên 512px để có chất lượng tốt hơn
                // font-size=0.4 để chữ không quá to
                String avatarSourceUrl = "https://ui-avatars.com/api/?" +
                        "background=" + color +
                        "&color=ffffff" +
                        "&size=512" +
                        "&bold=true" +
                        "&font-size=0.4" +
                        "&length=2" +
                        "&rounded=false" + // Không bo tròn, để Flutter xử lý
                        "&name=" + URLEncoder.encode(user.fullName, StandardCharsets.UTF_8);

                // 3. Cấu hình upload
                Map params = ObjectUtils.asMap(
                        "public_id", "avatars/" + user.id,
                        "overwrite", true,
                        "resource_type", "image",
                        "quality", "auto:best", // Tự động tối ưu chất lượng
                        "fetch_format", "auto"  // Tự động chọn format tốt nhất (WebP nếu có thể)
                );

                // 4. Thực hiện Upload
                Map uploadResult = cloudinary.uploader().upload(avatarSourceUrl, params);

                System.out.printf("✅ [User %2s] %-20s -> Color: #%s -> %s%n",
                        user.id, user.fullName, color, uploadResult.get("secure_url"));

            } catch (Exception e) {
                System.err.println("❌ Lỗi upload user ID " + user.id + ": " + e.getMessage());
            }
        }

        System.out.println("\n🎉 Hoàn tất quá trình upload!");
        System.out.println("\n📊 Preview màu sắc:");
        System.out.println("┌─────┬──────────────────────┬──────────┐");
        System.out.println("│ ID  │ Màu                  │ Hex Code │");
        System.out.println("├─────┼──────────────────────┼──────────┤");
        for (int i = 0; i < THEME_COLORS.length; i++) {
            String colorName = getColorName(i);
            System.out.printf("│ %2d  │ %-20s │ #%s │%n", i + 1, colorName, THEME_COLORS[i]);
        }
        System.out.println("└─────┴──────────────────────┴──────────┘");
    }

    // Helper method để hiển thị tên màu
    private static String getColorName(int index) {
        String[] names = {
                "Soft Blue", "Soft Purple", "Sage Green", "Dusty Rose",
                "Warm Brown", "Forest Green", "Mauve", "Steel Blue",
                "Caramel", "Teal", "Taupe", "Slate Blue",
                "Olive", "Dusty Pink", "Aqua", "Sand",
                "Periwinkle", "Moss", "Terracotta", "Denim"
        };
        return names[index];
    }
}
