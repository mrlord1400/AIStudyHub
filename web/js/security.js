/*
 * Helper dùng chung để chống XSS ở phía trình duyệt (Conv §3). Định nghĩa MỘT lần ở đây.
 *
 *   el.innerHTML = '<b>' + escapeHtml(userValue) + '</b>';   // template bắt buộc dùng innerHTML
 *   el.textContent = userValue;                              // ưu tiên cách này
 *   renderMarkdown(botText)                                  // markdown từ bot -> HTML đã lọc
 *
 * Giá trị cần cho JS thì đặt trong thuộc tính data-* đã escape rồi đọc bằng el.dataset.x,
 * không nối chuỗi vào onclick='...' hoặc <script>.
 */
(function (global) {
    'use strict';

    var MAP = {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'};

    /** Escape & < > " ' ; null/undefined thành chuỗi rỗng. */
    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value).replace(/[&<>"']/g, function (c) {
            return MAP[c];
        });
    }

    /**
     * Markdown -> HTML an toàn. Cần marked và DOMPurify đã được nạp; nếu thiếu một trong hai thì
     * hiện văn bản thuần (đã escape), không bao giờ trả HTML chưa lọc.
     */
    function renderMarkdown(text) {
        var src = text === null || text === undefined ? '' : String(text);
        var parse = global.marked && (typeof global.marked.parse === 'function' ? global.marked.parse : global.marked);
        if (typeof parse === 'function' && global.DOMPurify && typeof global.DOMPurify.sanitize === 'function') {
            return global.DOMPurify.sanitize(parse.call(global.marked, src));
        }
        return escapeHtml(src).replace(/\n/g, '<br>');
    }

    global.escapeHtml = escapeHtml;
    global.renderMarkdown = renderMarkdown;
})(window);
