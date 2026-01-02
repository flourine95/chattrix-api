import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

public class MergeCode {

    private static final String ROOT_DIR = "D:\\Projects\\chattrix\\chattrix-api\\src\\main\\java\\com\\chattrix\\api";

    private static final String OUTPUT_FILE = "D:\\Projects\\chattrix\\chattrix-api\\merged_code.txt";

    private static final List<String> EXTENSIONS = List.of(".java");

    // CẤU HÌNH: Các thư mục/file cần BỎ QUA (để tránh lấy code rác/generated)
    private static final List<String> IGNORED_PATHS = List.of(
            "target", ".git", ".idea", "build", "node_modules", ".mvn",
            "test" // Bỏ qua test nếu chỉ muốn gửi code chính (tùy chọn)
    );

    public static void main(String[] args) {
        Path startPath = Paths.get(ROOT_DIR);
        Path outputPath = Paths.get(OUTPUT_FILE);

        if (!Files.exists(startPath)) {
            System.err.println("❌ Không tìm thấy thư mục: " + startPath.toAbsolutePath());
            System.err.println("Hãy đặt file CodeMerger.java ở thư mục gốc dự án.");
            return;
        }

        System.out.println("🚀 Đang quét code từ: " + startPath.toAbsolutePath());

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
             Stream<Path> stream = Files.walk(startPath)) {

            long count = stream
                    .filter(path -> !Files.isDirectory(path)) // Chỉ lấy file
                    .filter(MergeCode::isAllowedFile)        // Kiểm tra đuôi file và thư mục cấm
                    .sorted()                                 // Sắp xếp theo tên cho gọn
                    .map(path -> writeContent(writer, path))  // Ghi nội dung
                    .filter(success -> success)               // Đếm số file thành công
                    .count();

            System.out.println("✅ Đã gộp thành công " + count + " files vào: " + OUTPUT_FILE);
            System.out.println("👉 Bạn hãy mở file '" + OUTPUT_FILE + "' và copy toàn bộ nội dung gửi cho AI.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isAllowedFile(Path path) {
        String pathString = path.toString();

        // 1. Kiểm tra nếu nằm trong thư mục bị ignore
        for (String ignored : IGNORED_PATHS) {
            if (pathString.contains(FileSystems.getDefault().getSeparator() + ignored + FileSystems.getDefault().getSeparator())
                    || pathString.startsWith(ignored)) {
                return false;
            }
        }

        // 2. Kiểm tra đuôi file
        for (String ext : EXTENSIONS) {
            if (pathString.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static boolean writeContent(BufferedWriter writer, Path path) {
        try {
            // Tạo Header đẹp để phân biệt các file
            String header = String.format("%n%n// =======================================================%n" +
                    "// FILE: %s%n" +
                    "// =======================================================%n", path.toString());

            writer.write(header);

            // Đọc và ghi nội dung file
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            System.err.println("⚠️ Không đọc được file: " + path);
            return false;
        }
    }
}