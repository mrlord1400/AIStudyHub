package Utils;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Giới hạn tần suất trong bộ nhớ, cửa sổ cố định, có chặn trên số khóa (D-07: một instance Tomcat, mất khi restart).
 *
 * Mỗi khóa có tối đa {@code max} lần trong {@code windowMs} kể từ lần đầu của cửa sổ. Khi số khóa chạm
 * {@code maxKeys}, các khóa đã hết cửa sổ bị dọn trước, nếu vẫn đầy thì bỏ khóa cũ nhất, nên bộ nhớ không phình ra
 * khi bị quét nhiều khóa khác nhau. Đồng hồ được tiêm vào để test không phải chờ.
 */
public final class RateLimiter {

    private static final class Entry {

        long windowStart;
        int count;
    }

    private final int max;
    private final long windowMs;
    private final int maxKeys;
    private final LongSupplier clock;
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<String, Entry>();

    public RateLimiter(int max, long windowMs, int maxKeys, LongSupplier clock) {
        if (max < 1 || windowMs < 1 || maxKeys < 1) {
            throw new IllegalArgumentException("max, windowMs and maxKeys must be positive");
        }
        this.max = max;
        this.windowMs = windowMs;
        this.maxKeys = maxKeys;
        this.clock = clock;
    }

    public RateLimiter(int max, long windowMs, int maxKeys) {
        this(max, windowMs, maxKeys, System::currentTimeMillis);
    }

    /** true nếu khóa đã dùng hết lượt trong cửa sổ hiện tại (không đếm thêm). */
    public synchronized boolean isBlocked(String key) {
        Entry e = live(key);
        return e != null && e.count >= max;
    }

    /** Đếm một lượt (ví dụ một lần đăng nhập sai). */
    public synchronized void hit(String key) {
        Entry e = live(key);
        if (e == null) {
            makeRoom();
            e = new Entry();
            e.windowStart = clock.getAsLong();
            entries.put(key, e);
        }
        e.count++;
    }

    /** Nếu còn lượt thì đếm và trả true, hết lượt thì trả false và không đếm. */
    public synchronized boolean tryAcquire(String key) {
        if (isBlocked(key)) {
            return false;
        }
        hit(key);
        return true;
    }

    /** Còn bao nhiêu ms nữa thì cửa sổ của khóa này mở lại (0 nếu không bị chặn). */
    public synchronized long retryAfterMs(String key) {
        Entry e = live(key);
        if (e == null || e.count < max) {
            return 0;
        }
        return Math.max(0, e.windowStart + windowMs - clock.getAsLong());
    }

    /** Gỡ một lượt đã đếm (ví dụ đăng nhập thành công không được tính vào hạn mức theo IP). */
    public synchronized void release(String key) {
        Entry e = live(key);
        if (e != null && e.count > 0) {
            e.count--;
            if (e.count == 0) {
                entries.remove(key);
            }
        }
    }

    public synchronized void clear(String key) {
        entries.remove(key);
    }

    public synchronized int size() {
        return entries.size();
    }

    /** Dòng còn trong cửa sổ, hoặc null (dòng hết hạn bị xóa). */
    private Entry live(String key) {
        Entry e = entries.get(key);
        if (e == null) {
            return null;
        }
        if (clock.getAsLong() - e.windowStart >= windowMs) {
            entries.remove(key);
            return null;
        }
        return e;
    }

    private void makeRoom() {
        if (entries.size() < maxKeys) {
            return;
        }
        long now = clock.getAsLong();
        Iterator<Map.Entry<String, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().windowStart >= windowMs) {
                it.remove();
            }
        }
        // vẫn đầy: bỏ khóa cũ nhất (thứ tự chèn)
        it = entries.entrySet().iterator();
        while (entries.size() >= maxKeys && it.hasNext()) {
            it.next();
            it.remove();
        }
    }
}
