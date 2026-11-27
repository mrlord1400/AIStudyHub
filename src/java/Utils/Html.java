package Utils;

/**
 * Escape giá trị khi in ra HTML (Conv §3). Escape lúc xuất, không escape trước khi lưu DB.
 *
 * Dùng trong scriptlet: {@code <%= Utils.Html.esc(doc.getTitle()) %>}. Escape đủ {@code & < > " '}
 * nên an toàn cả trong thân HTML lẫn trong thuộc tính đặt trong dấu nháy kép hoặc đơn.
 * Không dùng để chèn giá trị vào {@code <script>} hay {@code onclick}: chỗ đó phải dùng thuộc tính {@code data-*}.
 */
public final class Html {

    private Html() {
    }

    /** null thành chuỗi rỗng. */
    public static String esc(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        StringBuilder out = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String rep;
            switch (c) {
                case '&':
                    rep = "&amp;";
                    break;
                case '<':
                    rep = "&lt;";
                    break;
                case '>':
                    rep = "&gt;";
                    break;
                case '"':
                    rep = "&quot;";
                    break;
                case '\'':
                    rep = "&#39;";
                    break;
                default:
                    rep = null;
            }
            if (rep != null) {
                if (out == null) {
                    out = new StringBuilder(s.length() + 16);
                    out.append(s, 0, i);
                }
                out.append(rep);
            } else if (out != null) {
                out.append(c);
            }
        }
        return out == null ? s : out.toString();
    }

    /** Như {@link #esc} nhưng trả {@code fallback} (không escape) khi value null hoặc rỗng. */
    public static String escOr(Object value, String fallback) {
        String s = value == null ? "" : value.toString();
        return s.isEmpty() ? fallback : esc(s);
    }
}
