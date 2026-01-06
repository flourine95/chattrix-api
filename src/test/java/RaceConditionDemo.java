
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RaceConditionDemo {

    // Kẻ tội đồ: HashMap thường (Không an toàn)
    private static final Map<Integer, String> unsafeMap = new HashMap<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("==========================================");
        System.out.println("   CHƯƠNG TRÌNH VẠCH TRẦN LỖI HASHMAP   ");
        System.out.println("==========================================\n");

        // --- MÀN 1: ẢO THUẬT BIẾN MẤT (DATA LOSS) ---
        simulateDataLoss();

        System.out.println("\n------------------------------------------");
        System.out.println("Chuẩn bị sang Màn 2 trong 3 giây...");
        Thread.sleep(3000);
        System.out.println("------------------------------------------\n");

        // --- MÀN 2: NỔ TUNG (CRASH) ---
        unsafeMap.clear(); // Xóa dữ liệu cũ để test mới
        simulateCrash();
    }

    /**
     * Kịch bản: 2 luồng cùng tranh nhau ghi vào Map.
     * Hậu quả: Dữ liệu của luồng này đè lên luồng kia -> Mất dữ liệu.
     */
    private static void simulateDataLoss() throws InterruptedException {
        System.out.println("▶ MÀN 1: Test mất dữ liệu (Data Loss)");
        int totalUsers = 10000;
        ExecutorService executor = Executors.newFixedThreadPool(10);

        long start = System.currentTimeMillis();

        try {
            for (int i = 0; i < totalUsers; i++) {
                final int userId = i;
                executor.submit(() -> {
                    // Cố tình ghi đè liên tục
                    unsafeMap.put(userId, "User " + userId);
                });
            }
        } finally {
            executor.shutdown();
        }

        executor.awaitTermination(1, TimeUnit.MINUTES);
        long end = System.currentTimeMillis();

        int actualSize = unsafeMap.size();
        System.out.println("   - Kỳ vọng: " + totalUsers + " users");
        System.out.println("   - Thực tế: " + actualSize + " users");
        System.out.println("   - Thời gian: " + (end - start) + "ms");

        if (actualSize < totalUsers) {
            System.err.println("❌ KẾT QUẢ: THẤT BẠI! Đã bị mất " + (totalUsers - actualSize) + " users.");
            System.out.print("🔍 Soi vài user bị mất tích: ");
            int count = 0;
            for (int i = 0; i < totalUsers; i++) {
                if (!unsafeMap.containsKey(i)) {
                    System.out.print(i + ", ");
                    count++;
                    if (count >= 10) {
                        System.out.print("...");
                        break;
                    }
                }
            }
            System.out.println();
        } else {
            System.out.println("✅ May mắn: Không mất dữ liệu (Chạy lại vài lần sẽ thấy mất)");
        }
    }

    /**
     * Kịch bản: Một luồng đang đọc (duyệt for), một luồng khác nhảy vào xóa/sửa.
     * Hậu quả: Ứng dụng sập ngay lập tức.
     */
    private static void simulateCrash() {
        System.out.println("▶ MÀN 2: Test sập nguồn (ConcurrentModificationException)");
        System.out.println("   (Đang chạy... Hãy chờ dòng lỗi đỏ hiện ra)");

        // Tạo sẵn ít dữ liệu
        for (int i = 0; i < 100; i++) unsafeMap.put(i, "User " + i);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // LUỒNG 1: Kẻ phá hoại (Liên tục thêm/xóa dữ liệu)
        executor.submit(() -> {
            int i = 1000;
            while (true) {
                unsafeMap.put(i++, "User Mới"); // Gây nhiễu
                try {
                    Thread.sleep(1);
                } catch (Exception e) {
                }
            }
        });

        // LUỒNG 2: Nạn nhân (Đang cố gắng duyệt danh sách để in ra)
        executor.submit(() -> {
            try {
                while (true) {
                    // Vừa duyệt vừa run...
                    for (Integer key : unsafeMap.keySet()) {
                        // Chỉ cần đọc thôi là đủ chết rồi
                        String val = unsafeMap.get(key);
                    }
                }
            } catch (Exception e) {
                System.out.println("\n🔥 BÙM! ỨNG DỤNG ĐÃ CRASH 🔥");
                System.err.println("Lỗi bắt được: " + e); // In ra lỗi
                System.out.println("Lý do: Đang duyệt (Iterator) thì bị luồng khác sửa đổi Map.");
                System.exit(1); // Dừng chương trình
            }
        });
    }
}