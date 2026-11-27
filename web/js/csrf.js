/*
 * CSRF phía trình duyệt (Conv §4). Trang nạp file này trong <head> cùng <meta name="csrf-token"> (csrf_head.jspf).
 *
 *  - Mọi <form method="post"> được gắn input ẩn _csrf lúc gửi (kể cả form tạo động và form.submit() bằng script).
 *    Form multipart còn có _csrf trên URL action vì servlet chỉ đọc được tham số trong body sau khi parse multipart.
 *  - fetch() không phải GET/HEAD tự thêm header X-CSRF-Token (+ X-Requested-With để server trả JSON khi bị từ chối).
 *  - <a data-post [data-confirm="..."]> gửi POST thay vì GET: dùng cho đăng xuất và các nút xóa. Tham số lấy từ query
 *    của href. Server trả 405 cho GET tới các action ghi, nên link không có data-post sẽ không chạy.
 */
(function () {
    'use strict';

    var meta = document.querySelector('meta[name="csrf-token"]');
    var token = meta ? meta.getAttribute('content') : '';

    function sameOrigin(url) {
        try {
            return new URL(url, window.location.href).origin === window.location.origin;
        } catch (e) {
            return false;
        }
    }

    function isPost(form) {
        return (form.getAttribute('method') || 'get').toLowerCase() === 'post';
    }

    function prepare(form) {
        if (!token || !isPost(form)) {
            return;
        }
        if (!sameOrigin(form.getAttribute('action') || window.location.href)) {
            return;
        }
        var input = form.querySelector('input[name="_csrf"]');
        if (!input) {
            input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            form.appendChild(input);
        }
        input.value = token;
        if ((form.getAttribute('enctype') || '').toLowerCase() === 'multipart/form-data') {
            var action = form.getAttribute('action') || window.location.href;
            if (action.indexOf('_csrf=') < 0) {
                form.setAttribute('action', action + (action.indexOf('?') < 0 ? '?' : '&') + '_csrf=' + encodeURIComponent(token));
            }
        }
    }

    // Người dùng bấm gửi (bắt ở capture để chạy trước các handler khác).
    document.addEventListener('submit', function (e) {
        if (e.target && e.target.tagName === 'FORM') {
            prepare(e.target);
        }
    }, true);

    // form.submit() bằng script không phát sinh sự kiện submit.
    var nativeSubmit = HTMLFormElement.prototype.submit;
    HTMLFormElement.prototype.submit = function () {
        prepare(this);
        return nativeSubmit.call(this);
    };

    // fetch
    if (window.fetch) {
        var nativeFetch = window.fetch;
        window.fetch = function (input, init) {
            var method = ((init && init.method) || (input && input.method) || 'GET').toUpperCase();
            var url = typeof input === 'string' ? input : (input && input.url) || '';
            if (token && method !== 'GET' && method !== 'HEAD' && sameOrigin(url)) {
                init = init || {};
                var headers = new Headers(init.headers || (input && input.headers) || {});
                headers.set('X-CSRF-Token', token);
                headers.set('X-Requested-With', 'fetch');
                init.headers = headers;
            }
            return nativeFetch.call(this, input, init);
        };
    }

    // Link gửi POST
    document.addEventListener('click', function (e) {
        var a = e.target && e.target.closest ? e.target.closest('a[data-post]') : null;
        if (!a) {
            return;
        }
        e.preventDefault();
        e.stopPropagation();
        var message = a.getAttribute('data-confirm');
        if (message && !window.confirm(message)) {
            return;
        }
        var url = new URL(a.getAttribute('href'), window.location.href);
        var form = document.createElement('form');
        form.method = 'post';
        form.action = url.origin + url.pathname;
        url.searchParams.forEach(function (value, name) {
            var field = document.createElement('input');
            field.type = 'hidden';
            field.name = name;
            field.value = value;
            form.appendChild(field);
        });
        form.style.display = 'none';
        document.body.appendChild(form);
        form.submit();
    }, true);
})();
