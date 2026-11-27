package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Danh sách đuôi file được phép tải lên (một hằng số duy nhất, Conv §9) và kiểm tra nội dung (magic bytes).
 * Không đụng servlet API nên test độc lập được.
 */
public final class FileTypes {

    public static final Set<String> ALLOWED_EXTENSIONS = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
            "pdf", "docx", "xlsx", "pptx", "txt", "md", "png", "jpg", "jpeg", "gif")));

    private static final int HEAD_BYTES = 1024;
    private static final byte[] ZIP = {'P', 'K', 3, 4};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87 = {'G', 'I', 'F', '8', '7', 'a'};
    private static final byte[] GIF89 = {'G', 'I', 'F', '8', '9', 'a'};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};

    private FileTypes() {
    }

    /** Đuôi file (chữ thường, không dấu chấm) của tên file gốc; "" nếu không có. Chỉ lấy phần sau dấu chấm cuối cùng. */
    public static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        String name = fileName.trim();
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT).trim();
    }

    public static boolean isAllowedExtension(String ext) {
        return ext != null && ALLOWED_EXTENSIONS.contains(ext);
    }

    /** Chuỗi cho thông báo lỗi: "pdf, docx, ...". */
    public static String allowedList() {
        StringBuilder sb = new StringBuilder();
        for (String e : ALLOWED_EXTENSIONS) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(e);
        }
        return sb.toString();
    }

    /** MIME type để phục vụ file (không tin servlet container đoán). */
    public static String mimeType(String ext) {
        if (ext == null) {
            return "application/octet-stream";
        }
        switch (ext) {
            case "pdf":
                return "application/pdf";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "txt":
            case "md":
                return "text/plain; charset=UTF-8";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Nội dung file có khớp với đuôi khai báo không. Đuôi không nằm trong danh sách cho phép thì luôn false.
     * pdf: "%PDF-" trong 1024 byte đầu; docx/xlsx/pptx: chữ ký ZIP; png/jpg/gif: chữ ký ảnh;
     * txt/md: không chứa byte NUL.
     */
    public static boolean contentMatches(String ext, Path file) throws IOException {
        if (!isAllowedExtension(ext)) {
            return false;
        }
        switch (ext) {
            case "txt":
            case "md":
                return !containsNul(file);
            default:
                break;
        }
        byte[] head = new byte[HEAD_BYTES];
        int n;
        try (InputStream in = Files.newInputStream(file)) {
            n = readFully(in, head);
        }
        switch (ext) {
            case "pdf":
                return indexOf(head, n, PDF) >= 0;
            case "docx":
            case "xlsx":
            case "pptx":
                return startsWith(head, n, ZIP);
            case "png":
                return startsWith(head, n, PNG);
            case "jpg":
            case "jpeg":
                return startsWith(head, n, JPEG);
            case "gif":
                return startsWith(head, n, GIF87) || startsWith(head, n, GIF89);
            default:
                return false;
        }
    }

    private static boolean containsNul(Path file) throws IOException {
        byte[] buf = new byte[8192];
        try (InputStream in = Files.newInputStream(file)) {
            int n;
            while ((n = in.read(buf)) != -1) {
                for (int i = 0; i < n; i++) {
                    if (buf[i] == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int readFully(InputStream in, byte[] buf) throws IOException {
        int total = 0;
        while (total < buf.length) {
            int n = in.read(buf, total, buf.length - total);
            if (n < 0) {
                break;
            }
            total += n;
        }
        return total;
    }

    private static boolean startsWith(byte[] data, int len, byte[] prefix) {
        if (len < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] data, int len, byte[] needle) {
        for (int i = 0; i + needle.length <= len; i++) {
            int j = 0;
            while (j < needle.length && data[i + j] == needle[j]) {
                j++;
            }
            if (j == needle.length) {
                return i;
            }
        }
        return -1;
    }
}
