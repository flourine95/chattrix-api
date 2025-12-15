package com.chattrix.api.services.common;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Service xử lý tạo và quản lý avatar trên Cloudinary
 */
@ApplicationScoped
public class AvatarService {

    private static final Logger logger = LoggerFactory.getLogger(AvatarService.class);

    // Bảng màu Deep Muted cho avatar
    private static final String[] THEME_COLORS = {
            "333333", // Dark Grey
            "2E4053", // Dark Slate Blue
            "1C2833", // Charcoal
            "4A235A", // Deep Purple
            "0E6251", // Deep Teal
            "78281F", // Dark Red
            "154360", // Dark Blue
            "515A5A", // Grey
    };

    @Inject
    private Cloudinary cloudinary;

    /**
     * Tạo avatar từ tên người dùng và upload lên Cloudinary
     *
     * @param userId   ID người dùng (dùng làm public_id)
     * @param fullName Tên đầy đủ (dùng để tạo chữ cái)
     * @return URL của avatar trên Cloudinary
     */
    public String generateAndUploadAvatar(String userId, String fullName) {
        try {
            // Chọn màu ngẫu nhiên dựa trên userId để đảm bảo consistent
            String color = selectColorForUser(userId);

            // Tạo URL avatar từ ui-avatars.com với size 512px (chất lượng cao)
            String avatarSourceUrl = buildAvatarUrl(fullName, color);

            // Upload lên Cloudinary
            Map uploadParams = ObjectUtils.asMap(
                    "public_id", "avatars/" + userId,
                    "overwrite", true,
                    "resource_type", "image",
                    "folder", "avatars"
            );

            Map uploadResult = cloudinary.uploader().upload(avatarSourceUrl, uploadParams);

            // Trả về secure URL
            String avatarUrl = (String) uploadResult.get("secure_url");
            logger.info("Avatar created successfully for user {}: {}", userId, avatarUrl);

            return avatarUrl;

        } catch (IOException e) {
            logger.error("Failed to generate avatar for user {}", userId, e);
            throw new RuntimeException("Failed to generate avatar", e);
        }
    }

    /**
     * Xóa avatar của người dùng trên Cloudinary
     *
     * @param userId ID người dùng
     */
    public void deleteAvatar(String userId) {
        try {
            String publicId = "avatars/" + userId;
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            logger.info("Avatar deleted for user {}: {}", userId, result.get("result"));
        } catch (Exception e) {
            logger.error("Failed to delete avatar for user {}", userId, e);
            // Không throw exception để không ảnh hưởng đến flow chính
        }
    }

    /**
     * Upload avatar từ file/URL khác
     *
     * @param userId      ID người dùng
     * @param imageSource File path hoặc URL của ảnh
     * @return URL của avatar trên Cloudinary
     */
    public String uploadCustomAvatar(String userId, Object imageSource) {
        try {
            Map uploadParams = ObjectUtils.asMap(
                    "public_id", "avatars/" + userId,
                    "overwrite", true,
                    "resource_type", "image",
                    "folder", "avatars",
                    "transformation", ObjectUtils.asMap(
                            "width", 512,
                            "height", 512,
                            "crop", "fill",
                            "gravity", "face"
                    )
            );

            Map uploadResult = cloudinary.uploader().upload(imageSource, uploadParams);
            String avatarUrl = (String) uploadResult.get("secure_url");

            logger.info("Custom avatar uploaded for user {}: {}", userId, avatarUrl);
            return avatarUrl;

        } catch (IOException e) {
            logger.error("Failed to upload custom avatar for user {}", userId, e);
            throw new RuntimeException("Failed to upload custom avatar", e);
        }
    }

    /**
     * Tạo URL avatar từ ui-avatars.com
     */
    private String buildAvatarUrl(String fullName, String color) {
        try {
            return "https://ui-avatars.com/api/?" +
                    "background=" + color +
                    "&color=ffffff" +
                    "&size=512" +
                    "&bold=true" +
                    "&font-size=0.45" +
                    "&length=2" +
                    "&name=" + URLEncoder.encode(fullName, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to encode full name: {}", fullName, e);
            throw new RuntimeException("Failed to build avatar URL", e);
        }
    }

    /**
     * Chọn màu dựa trên userId để đảm bảo mỗi user có màu consistent
     */
    private String selectColorForUser(String userId) {
        int hash = Math.abs(userId.hashCode());
        int index = hash % THEME_COLORS.length;
        return THEME_COLORS[index];
    }
}

