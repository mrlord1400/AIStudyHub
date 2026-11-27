package Utils;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * T2.2: một lần chạy để chuyển file cũ trong webapp ({@code <webapp>/uploads/<uid>/<tên>.<ext>}, cột
 * {@code cloud_storage_url} dạng {@code /AIStudyHub/uploads/...}) sang kho mới ({@code STORAGE_DIR}, key
 * {@code <uid>/<uuid>.<ext>}).
 *
 * Mặc định là chạy thử (dry-run), không ghi gì. Có thể chạy lại nhiều lần: dòng đã là key hợp lệ thì bỏ qua.
 * File cũ chỉ bị xóa khi có {@code --delete-old}, sau khi bản sao đã được kiểm tra kích thước và DB đã cập nhật.
 *
 * <pre>
 *   java -cp "build/web/WEB-INF/classes;web/WEB-INF/lib/*" Utils.StorageMigration --legacy-webroot=&lt;thư mục webapp cũ&gt; [--apply] [--delete-old]
 * </pre>
 * DB và STORAGE_DIR đọc từ cấu hình như ứng dụng (AppConfig). {@code --legacy-webroot} là thư mục CHỨA thư mục
 * {@code uploads} (vd. {@code C:\...\apache-tomcat\webapps\AIStudyHub} hoặc {@code build\web}).
 */
public final class StorageMigration {

    /** Kết quả từng dòng. Tổng các trạng thái luôn bằng số dòng trong bảng documents. */
    public enum Status {
        ALREADY_MIGRATED, MIGRATED, MISSING_FILE, BAD_EXTENSION, BAD_CONTENT, UNSAFE_PATH, FAILED
    }

    public static final class Report {

        public final boolean apply;
        public int total;
        public final int[] counts = new int[Status.values().length];
        /** Dòng bị bỏ qua cần người xem lại: "documentId: lý do". */
        public final List<String> problems = new ArrayList<String>();
        /** File dưới uploads/ cũ không có dòng DB nào trỏ tới (có thể là file nguy hiểm, vd .jsp). */
        public final List<String> orphanFiles = new ArrayList<String>();
        public int legacyFilesDeleted;
        final List<Path> legacyToDelete = new ArrayList<Path>();
        /** Đuôi không được phép (vd. doc, zip) -> số dòng bị bỏ qua. Các tài liệu này sẽ không mở được nữa cho tới khi có quyết định (D-17). */
        public final java.util.Map<String, Integer> skippedExtensions = new java.util.TreeMap<String, Integer>();

        Report(boolean apply) {
            this.apply = apply;
        }

        public int count(Status s) {
            return counts[s.ordinal()];
        }

        public int sum() {
            int n = 0;
            for (int c : counts) {
                n += c;
            }
            return n;
        }
    }

    private StorageMigration() {
    }

    public static Report migrate(Connection conn, Path legacyWebroot, Path storageRoot, boolean apply, boolean deleteOld)
            throws SQLException, IOException {

        Report report = new Report(apply);
        Path legacyUploads = legacyWebroot.toAbsolutePath().normalize().resolve("uploads").normalize();
        Set<Path> referenced = new HashSet<Path>();
        Set<Path> keep = new HashSet<Path>();

        List<long[]> ids = new ArrayList<long[]>();   // {document_id, user_id}
        List<String[]> rows = new ArrayList<String[]>();   // {extension, url}
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT document_id, user_id, file_extension, cloud_storage_url FROM documents ORDER BY document_id");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(new long[]{rs.getLong(1), rs.getLong(2)});
                rows.add(new String[]{rs.getString(3), rs.getString(4)});
            }
        }
        report.total = ids.size();

        for (int i = 0; i < ids.size(); i++) {
            long docId = ids.get(i)[0];
            int userId = (int) ids.get(i)[1];
            String ext = rows.get(i)[0] == null ? "" : rows.get(i)[0].trim().toLowerCase(Locale.ROOT);
            String url = rows.get(i)[1];

            Status st = migrateRow(conn, legacyUploads, storageRoot, apply, deleteOld, report, docId, userId, ext, url, referenced);
            report.counts[st.ordinal()]++;
            Path lp = legacyPathOf(legacyUploads, url);
            if (st != Status.MIGRATED && lp != null) {
                keep.add(lp);   // một dòng chưa migrate được vẫn có thể cần file này
            }
        }

        for (Path p : report.legacyToDelete) {
            if (!keep.contains(p) && Files.deleteIfExists(p)) {
                report.legacyFilesDeleted++;
            }
        }

        if (Files.isDirectory(legacyUploads)) {
            try (java.util.stream.Stream<Path> walk = Files.walk(legacyUploads)) {
                for (Path p : (Iterable<Path>) walk.filter(Files::isRegularFile)::iterator) {
                    if (!referenced.contains(p.toAbsolutePath().normalize())) {
                        report.orphanFiles.add(legacyUploads.relativize(p).toString());
                    }
                }
            }
        }
        return report;
    }

    private static Status migrateRow(Connection conn, Path legacyUploads, Path storageRoot, boolean apply, boolean deleteOld,
            Report report, long docId, int userId, String ext, String url, Set<Path> referenced) {

        if (FileStorage.isValidKey(url)) {
            if (!Files.isRegularFile(FileStorage.resolve(storageRoot, url))) {
                report.problems.add(docId + ": already a storage key but the file is missing from the storage directory");
            }
            return Status.ALREADY_MIGRATED;
        }

        Path legacyFile = legacyPathOf(legacyUploads, url);
        if (legacyFile == null) {
            report.problems.add(docId + ": cloud_storage_url is not a path under /uploads/ (left unchanged)");
            return Status.UNSAFE_PATH;
        }
        referenced.add(legacyFile);

        if (!Files.isRegularFile(legacyFile)) {
            report.problems.add(docId + ": file not found under the legacy uploads folder");
            return Status.MISSING_FILE;
        }
        if (!FileTypes.isAllowedExtension(ext)) {
            report.problems.add(docId + ": extension '" + ext + "' is not allowed (not copied, the document stays unreachable until D-17 is decided)");
            Integer seen = report.skippedExtensions.get(ext.isEmpty() ? "(none)" : ext);
            report.skippedExtensions.put(ext.isEmpty() ? "(none)" : ext, seen == null ? 1 : seen + 1);
            return Status.BAD_EXTENSION;
        }
        try {
            if (!FileTypes.contentMatches(ext, legacyFile)) {
                report.problems.add(docId + ": content does not match ." + ext + " (not copied; review and delete by hand)");
                return Status.BAD_CONTENT;
            }
        } catch (IOException e) {
            report.problems.add(docId + ": could not read the file (" + e.getClass().getSimpleName() + ")");
            return Status.FAILED;
        }

        if (!apply) {
            return Status.MIGRATED;   // "would migrate"
        }

        String key = null;
        try {
            key = FileStorage.importFile(storageRoot, userId, legacyFile, ext);
            Path copy = FileStorage.resolve(storageRoot, key);
            if (Files.size(copy) != Files.size(legacyFile)) {
                throw new IOException("copy size differs");
            }
            // Điều kiện theo giá trị cũ: nếu dòng đã đổi trong lúc chạy thì không ghi đè
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE documents SET cloud_storage_url = ? WHERE document_id = ? AND cloud_storage_url = ?")) {
                ps.setString(1, key);
                ps.setLong(2, docId);
                ps.setString(3, url);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("row changed during migration");
                }
            }
            if (deleteOld) {
                report.legacyToDelete.add(legacyFile);   // chỉ xóa sau khi MỌI dòng đã xử lý (nhiều dòng có thể trỏ cùng một file cũ)
            }
            return Status.MIGRATED;
        } catch (IOException | SQLException e) {
            if (key != null) {
                try {
                    Files.deleteIfExists(FileStorage.resolve(storageRoot, key));   // không để bản sao mồ côi khi DB chưa trỏ tới nó
                } catch (IOException | RuntimeException ignored) {
                    report.problems.add(docId + ": could not remove the partial copy " + key);
                }
            }
            report.problems.add(docId + ": failed (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            return Status.FAILED;
        }
    }

    /**
     * "/AIStudyHub/uploads/12/abc.pdf" → &lt;legacyUploads&gt;/12/abc.pdf, hoặc null nếu không nằm dưới uploads/.
     * Chỉ lấy phần sau "/uploads/" đầu tiên và chặn mọi đường dẫn thoát khỏi thư mục uploads.
     */
    static Path legacyPathOf(Path legacyUploads, String url) {
        if (url == null) {
            return null;
        }
        int idx = url.indexOf("/uploads/");
        if (idx < 0) {
            return null;
        }
        String rel = url.substring(idx + "/uploads/".length());
        if (rel.isEmpty() || rel.indexOf('\0') >= 0) {
            return null;
        }
        try {
            Path p = legacyUploads.resolve(rel).normalize();
            return p.startsWith(legacyUploads) && !p.equals(legacyUploads) ? p : null;
        } catch (RuntimeException e) {
            return null;   // InvalidPathException
        }
    }

    // ───────────────────────── CLI ─────────────────────────

    public static void main(String[] args) throws Exception {
        // Đầu ra của công cụ dòng lệnh: không đi qua logger (người vận hành đọc trực tiếp), UTF-8 cố định
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8");

        String legacy = null;
        boolean apply = false;
        boolean deleteOld = false;
        for (String a : args) {
            if (a.startsWith("--legacy-webroot=")) {
                legacy = a.substring("--legacy-webroot=".length());
            } else if (a.equals("--apply")) {
                apply = true;
            } else if (a.equals("--delete-old")) {
                deleteOld = true;
            } else {
                out.println("Unknown argument: " + a);
                legacy = null;
                break;
            }
        }
        if (legacy == null || legacy.trim().isEmpty() || (deleteOld && !apply)) {
            out.println("Usage: Utils.StorageMigration --legacy-webroot=<webapp folder that contains uploads/> [--apply] [--delete-old]");
            out.println("  default is a dry run; --delete-old needs --apply");
            System.exit(2);
            return;
        }

        Path storage = FileStorage.root();
        Path legacyRoot = Paths.get(legacy);
        out.println("Mode: " + (apply ? "APPLY" : "DRY RUN (nothing is written)") + (deleteOld ? ", delete old files" : ""));
        out.println("Legacy webapp : " + legacyRoot.toAbsolutePath());
        out.println("Storage dir   : " + storage);

        Report r;
        try (Connection conn = DBUtils.getConnection()) {
            r = migrate(conn, legacyRoot, storage, apply, deleteOld);
        }

        out.println();
        out.println("documents in the database : " + r.total);
        for (Status s : Status.values()) {
            String label = (s == Status.MIGRATED && !apply) ? "WOULD_MIGRATE" : s.name();
            out.println(String.format("  %-18s %d", label, r.count(s)));
        }
        out.println("  sum of the above          " + r.sum() + (r.sum() == r.total ? "  (matches)" : "  (MISMATCH)"));
        if (apply && deleteOld) {
            out.println("legacy files deleted        : " + r.legacyFilesDeleted);
        }
        if (!r.skippedExtensions.isEmpty()) {
            out.println();
            out.println("Documents with a file type that is no longer accepted (NOT migrated, they will answer 404, decision D-17): " + r.skippedExtensions);
        }
        if (!r.problems.isEmpty()) {
            out.println();
            out.println("Needs a human look (" + r.problems.size() + "):");
            for (String p : r.problems) {
                out.println("  " + p);
            }
        }
        out.println();
        out.println("Files under uploads/ that no document row points at: " + r.orphanFiles.size());
        for (int i = 0; i < Math.min(20, r.orphanFiles.size()); i++) {
            out.println("  " + r.orphanFiles.get(i));
        }
        if (!apply) {
            out.println();
            out.println("Dry run only. Re-run with --apply to copy files and update the rows.");
        } else {
            out.println();
            out.println("Next: open a sample of old documents in the app, then delete the old uploads/ folder by hand.");
            out.println("(--delete-old only works in the same run as the migration; after a plain --apply the old paths are no longer recorded.)");
        }
    }
}
