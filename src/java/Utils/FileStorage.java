package Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Nơi duy nhất biến "storage key" thành file trên đĩa (Conv §9).
 *
 * File nằm dưới {@code STORAGE_DIR} (ngoài webapp) tại {@code <STORAGE_DIR>/<userId>/<uuid>.<ext>}.
 * {@code documents.cloud_storage_url} lưu key {@code <userId>/<uuid>.<ext>}; tên trên đĩa không bao giờ chứa
 * dữ liệu người dùng. Mọi key đều được kiểm tra bằng regex nghiêm ngặt và phải nằm trong thư mục gốc.
 */
public final class FileStorage {

    private static final Logger LOG = Logger.getLogger(FileStorage.class.getName());

    public static final String CONFIG_KEY = "STORAGE_DIR";

    private static final Pattern KEY = Pattern.compile(
            "^[0-9]{1,10}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]{1,10}$");

    private FileStorage() {
    }

    // ───────────────────────── thư mục gốc ─────────────────────────

    /** Thư mục gốc từ cấu hình (bắt buộc, phải là đường dẫn tuyệt đối). Thiếu thì ném "Missing required config: STORAGE_DIR". */
    public static Path root() {
        Path root = Paths.get(AppConfig.require(CONFIG_KEY));
        if (!root.isAbsolute()) {
            throw new IllegalStateException("Config " + CONFIG_KEY + " must be an absolute path");
        }
        return root.normalize();
    }

    /**
     * Kiểm tra lúc khởi động: tạo được thư mục, ghi được, và KHÔNG nằm trong webapp.
     * Trả về danh sách vấn đề (rỗng nếu ổn). Không ném lỗi.
     */
    public static List<String> validateRoot(String webappRealPath) {
        List<String> problems = new ArrayList<String>();
        Path root;
        try {
            root = root();
        } catch (IllegalStateException e) {
            problems.add(e.getMessage());
            return problems;
        }
        try {
            Files.createDirectories(root);
            if (!Files.isWritable(root)) {
                problems.add(CONFIG_KEY + " is not writable");
            }
        } catch (IOException e) {
            problems.add(CONFIG_KEY + " cannot be created: " + e.getClass().getSimpleName());
        }
        if (webappRealPath != null && !webappRealPath.isEmpty()) {
            Path web = Paths.get(webappRealPath).toAbsolutePath().normalize();
            if (root.startsWith(web) || web.startsWith(root)) {
                problems.add(CONFIG_KEY + " must be outside the web application folder");
            }
        }
        return problems;
    }

    // ───────────────────────── key ─────────────────────────

    public static boolean isValidKey(String key) {
        return key != null && KEY.matcher(key).matches();
    }

    /** Key → đường dẫn trong thư mục gốc; ném IllegalArgumentException nếu key sai dạng hoặc thoát khỏi thư mục gốc. */
    public static Path resolve(String key) {
        return resolve(root(), key);
    }

    /** Như {@link #resolve(String)} với thư mục gốc cho trước (dùng để test và cho công cụ migrate). */
    public static Path resolve(Path root, String key) {
        if (!isValidKey(key)) {
            throw new IllegalArgumentException("invalid storage key");
        }
        Path base = root.toAbsolutePath().normalize();
        Path resolved = base.resolve(key).normalize();
        if (!resolved.startsWith(base) || resolved.equals(base)) {
            throw new IllegalArgumentException("storage key escapes the storage root");
        }
        return resolved;
    }

    // ───────────────────────── thao tác ─────────────────────────

    /**
     * Ghi stream thành file mới của userId và trả về key. Đuôi phải nằm trong {@link FileTypes#ALLOWED_EXTENSIONS}.
     * Việc kiểm tra nội dung ({@link FileTypes#contentMatches}) là việc của người gọi, sau khi lưu.
     */
    public static String save(int userId, InputStream in, String ext) throws IOException {
        return save(root(), userId, in, ext);
    }

    public static String save(Path root, int userId, InputStream in, String ext) throws IOException {
        if (userId < 0 || !FileTypes.isAllowedExtension(ext)) {
            throw new IllegalArgumentException("file type not allowed");
        }
        String key = userId + "/" + UUID.randomUUID() + "." + ext;
        Path target = resolve(root, key);
        Files.createDirectories(target.getParent());
        try {
            Files.copy(in, target);   // không ghi đè: tên là UUID mới
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        return key;
    }

    /** Bản sao file có sẵn vào kho (dùng khi migrate). Trả về key mới. */
    public static String importFile(Path root, int userId, Path source, String ext) throws IOException {
        try (InputStream in = Files.newInputStream(source)) {
            return save(root, userId, in, ext);
        }
    }

    public static InputStream open(String key) throws IOException {
        return Files.newInputStream(resolve(key));
    }

    /** File của key nếu key hợp lệ và file tồn tại; ngược lại null (key kiểu cũ/đã hỏng cũng cho null). */
    public static File fileOrNull(String key) {
        if (!isValidKey(key)) {
            return null;
        }
        File f = resolve(key).toFile();
        return f.isFile() ? f : null;
    }

    /** Xóa file của key. Gọi SAU KHI bản ghi DB đã xóa thành công. Không ném lỗi; true nếu đã xóa. */
    public static boolean delete(String key) {
        if (!isValidKey(key)) {
            if (key != null && !key.trim().isEmpty()) {
                LOG.log(Level.WARNING, "delete skipped: value is not a storage key (legacy row?)");
            }
            return false;
        }
        try {
            return Files.deleteIfExists(resolve(key));
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "could not delete stored file", e);
            return false;
        }
    }

    /** Xóa toàn bộ thư mục file của một user (sau khi xóa tài khoản). Trả về số file đã xóa. */
    public static int deleteUserFiles(int userId) {
        if (userId < 0) {
            return 0;
        }
        int deleted = 0;
        try {
            Path root = root().toAbsolutePath().normalize();
            Path dir = root.resolve(Integer.toString(userId)).normalize();
            if (!dir.startsWith(root) || dir.equals(root) || !Files.isDirectory(dir)) {
                return 0;
            }
            try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
                for (Path p : files) {
                    if (Files.isRegularFile(p) && Files.deleteIfExists(p)) {
                        deleted++;
                    }
                }
            }
            Files.deleteIfExists(dir);   // chỉ xóa được khi đã rỗng
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "could not delete the stored files of userId=" + userId, e);
        }
        return deleted;
    }
}
