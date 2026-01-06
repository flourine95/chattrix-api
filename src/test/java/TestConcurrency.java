
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class TestConcurrency {

    // Kịch bản 1: Cách cũ (Synchronized Map) - Khóa toàn bộ
    private static final Map<Integer, String> syncMap = Collections.synchronizedMap(new HashMap<>());

    // Kịch bản 2: Cách mới (ConcurrentHashMap) - Khóa từng phần & Không khóa khi đọc
    private static final Map<Integer, String> concurrentMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Cấu hình bài test hạng nặng
        int nThreads = 100;           // 100 luồng chạy song song (Giả lập 100 user spam cùng lúc)
        int nOperations = 100_0000;    // Mỗi luồng thực hiện 100.000 thao tác
        // Tổng cộng: 10 triệu thao tác lên Server

        System.out.println("=== BẮT ĐẦU TEST HIỆU NĂNG (90% Read, 10% Write) ===");
        System.out.println("Threads: " + nThreads + " | Ops/Thread: " + nOperations);
        System.out.println("------------------------------------------------");

        // --- TEST 1: SYNCHRONIZED MAP ---
        long durationSync = runTest("Collections.synchronizedMap", syncMap, nThreads, nOperations);

        // --- TEST 2: CONCURRENT HASHMAP ---
        long durationConcurrent = runTest("ConcurrentHashMap", concurrentMap, nThreads, nOperations);

        // --- SO SÁNH ---
        System.out.println("\n=== KẾT QUẢ CUỐI CÙNG ===");
        System.out.println("Synchronized Map: " + durationSync + " ms");
        System.out.println("ConcurrentHashMap: " + durationConcurrent + " ms");

        double improvement = (double) durationSync / durationConcurrent;
        System.out.printf("🚀 ConcurrentHashMap nhanh hơn gấp %.2f lần!%n", improvement);
    }

    private static long runTest(String mapName, Map<Integer, String> map, int nThreads, int nOps) {
        // Pre-fill dữ liệu để có cái mà đọc
        for (int i = 0; i < 1000; i++) map.put(i, "User " + i);

        long startTime = System.currentTimeMillis();

        try (var executor = Executors.newFixedThreadPool(nThreads)) {
            for (int i = 0; i < nThreads; i++) {
                executor.submit(() -> {
                    // Mỗi luồng thực hiện nOps thao tác hỗn hợp
                    for (int j = 0; j < nOps; j++) {
                        // Random key từ 0-1000
                        int key = ThreadLocalRandom.current().nextInt(1000);

                        // Mô phỏng tỷ lệ thực tế: 90% Đọc, 10% Ghi
                        // Trong App Chat: Bạn nhận tin nhắn (Read) nhiều hơn là bạn login (Write)
                        if (ThreadLocalRandom.current().nextInt(10) < 9) {
                            // 90% là ĐỌC (GET)
                            map.get(key);
                        } else {
                            // 10% là GHI (PUT)
                            map.put(key, "Updated " + j);
                        }
                    }
                });
            }
        } // Tự động chờ xong hết mới chạy xuống dưới

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("[" + mapName + "] Hoàn thành trong: " + duration + " ms");
        return duration;
    }
}