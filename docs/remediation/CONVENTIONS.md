# Remediation Conventions

These rules make sure many agents across many sessions fix things **the same way**. If a task's
instructions conflict with this file, this file wins unless the task explicitly says otherwise. To change
a convention, add a row to the *Decisions log* at the bottom. Don't diverge quietly.

Stack constraints: **Java 8 source level**, Servlet 3.1 / JSP / JSTL 1.2, Tomcat, SQL Server, NetBeans Ant
project (no Maven). Keep new code in the existing packages and naming style: `Controller`, `Model.DAO`,
`Model.DTO`, `Model.Service`, `Utils`, `Filter`.

---

## 1. Configuration and secrets (T0.2)

- All secrets and environment-dependent values come from **`Utils.AppConfig`**. Nothing else reads env vars or literals.
- Lookup order: environment variable → JVM system property → properties file at
  `%USERPROFILE%\.aistudyhub\app.properties` (overridable with the env var `AISTUDYHUB_CONFIG`).
  The properties file lives **outside the repo**. `docs/remediation/app.properties.example` lists every key.
- Key naming: `UPPER_SNAKE` (e.g. `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SMTP_USER`, `SMTP_PASSWORD`,
  `GEMINI_API_KEY`, `REMEMBER_ME_SECRET`, `STORAGE_DIR`).
- `AppConfig.require("KEY")` throws `IllegalStateException("Missing required config: KEY")` → **fail closed**.
  `AppConfig.get("KEY", "default")` is only for non-secret values with a documented safe default.
- Booleans are parsed by **one** method, `AppConfig.getBoolean(key, default)` (accepts `true/false` only, case-insensitive; anything else throws).
- Required keys are validated once at startup (in a `ServletContextListener`), and the failure message names the key.

## 2. Authentication and authorization (Phase 1)

### 2.1 One gate, default-deny
- `Filter.AuthFilter` is the **only** place that decides "must be logged in" / "must be admin" per path.
- It must be registered in `web.xml`, not by annotation, so its order is defined:
  `SessionSyncFilter` first, then `AuthFilter`, then `CsrfFilter`.
- Dispatcher types are `REQUEST` **and** `FORWARD`. `MainController` forwards to other servlets, so a REQUEST-only filter
  would see `/MainController` and miss the real target. On FORWARD, `request.getServletPath()` returns the forward target.
- Paths are classified in three explicit lists (public, admin-only, authenticated), kept in one constants class.
  **Anything not listed counts as "authenticated".** The default is never "public".
- When access is denied:
  - Page request, not logged in: redirect to `login.jsp`.
  - Page request, logged in but not admin: respond `403`.
  - JSON/AJAX endpoint: respond `401` or `403` with the JSON envelope (§6).

### 2.2 One permission module
- `Utils.Access` holds the helpers. Controllers use these and never re-implement them:
  - `Integer Access.currentUserId(HttpServletRequest)` returns `null` if there is no session.
  - `boolean Access.isAdmin(HttpServletRequest)`
- `Model.Service.DocumentAccess` holds document-level decisions **in one place**:
  - `canView(Document d, Integer viewerId)`: owner, PUBLIC, or FRIENDS_ONLY **with** an ACCEPTED friendship. Admins can view everything.
  - `canEdit(Document d, Integer userId)`: owner only. Admins edit through admin endpoints.
  - Every place that serves or acts on a document calls these: `viewDoc`, `viewPublicPage`, `downloadDoc`,
    `toggleBookmark`, the ChatBot `VIEW/` command, and `createReport`.
- Never trust a user id, owner id or role from the request. The acting user comes **only** from `session.userId`.

### 2.3 Ownership enforced in SQL
- Every UPDATE or DELETE on user-owned rows includes the owner in the WHERE clause, and the code checks the affected row count:
  ```java
  String sql = "UPDATE documents SET title = ?, ... WHERE document_id = ? AND user_id = ?";
  int rows = ps.executeUpdate();
  return rows == 1;   // 0 => not found OR not yours; treat both as not-found
  ```
- Add a new owner-scoped DAO method, e.g. `updateDocumentInfoForOwner(docId, userId, ...)`, instead of changing the
  signature of an unscoped one that admin code still uses. Then remove the unscoped one when nothing calls it.
- Respond to "exists but isn't yours" exactly like "doesn't exist": the existing `error=not_found` redirect or a 404.
  Never say "forbidden" for another user's private resource, because that leaks that it exists.

### 2.4 Stateful flows
- Check both "does this row exist for this caller?" **and** "is this the expected next transition?" in the SQL itself.
  Example: `UPDATE friendships SET status='ACCEPTED' WHERE requester_id=? AND addressee_id=? AND status='PENDING'`,
  where the caller is the **addressee**.

## 3. Output encoding (T3.1)

| Context | Do this |
|---|---|
| JSP EL in HTML body/attribute | `${fn:escapeXml(doc.title)}` with `<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>` (or `<c:out value="${...}"/>`) |
| Scriptlet in HTML | `<%= Utils.Html.esc(doc.getTitle()) %>`. `Html.esc` escapes `& < > " '` |
| Value needed by JavaScript | Put it in an escaped `data-*` attribute and read `el.dataset.x`. **Never** interpolate into `onclick='...'` or `<script>` |
| Server → JS JSON | Build with Gson (`new Gson().toJson(obj)`), never string concatenation |
| Client-side DOM building | Use `textContent` / `createElement`. If a template must use `innerHTML`, wrap every value with `escapeHtml()` from `web/js/security.js`, which is defined once |
| Markdown from the bot | `DOMPurify.sanitize(marked.parse(text))`. Pin exact CDN versions and add SRI hashes |
| HTML email | Pass interpolated user values through `Html.esc` |

Escape on **output**. Don't HTML-escape before storing in the DB.

## 4. CSRF and HTTP methods (T3.2)

- Use a synchronizer token: `session.csrfToken` (32 random bytes from `SecureRandom`, Base64url), created at login or on first use.
- Forms include `<input type="hidden" name="_csrf" value="${fn:escapeXml(sessionScope.csrfToken)}">`.
- `fetch` calls send the header `X-CSRF-Token`, read from `<meta name="csrf-token" content="...">`.
- `Filter.CsrfFilter` rejects any POST without a valid token (constant-time compare) with **403**.
- **GET must never change state.** A state-changing action reached by GET responds `405` once it has been converted.

## 5. Logging (T5.1)

- Use `java.util.logging`, which needs no new dependency. One logger per class:
  `private static final Logger LOG = Logger.getLogger(DocumentController.class.getName());`
- Keep the message constant and pass values as parameters:
  `LOG.log(Level.WARNING, "upload rejected reason={0} userId={1} sizeMb={2} limitMb={3}", new Object[]{"quota", userId, size, limit});`
- Log an exception **once**, at the layer that handles it, with the exception object: `LOG.log(Level.SEVERE, "document delete failed docId=" + id, e);`
- Levels: expected rejections (validation, permission, not found) → `WARNING`; unexpected failures → `SEVERE`; start/end of significant work → `INFO`.
- **Never log** passwords, OTPs, tokens, cookies, API keys, file contents, chat message contents, or full request bodies.
  Log `userId`, not email.
- Every line carries the request id (§7) once T5.2 lands.
- Replace `System.out` / `System.err` / `printStackTrace` in any file you touch. Leave untouched files for T5.1.

## 6. Errors and status codes (T5.3–T5.5)

- **Page flows** (form POST → redirect) keep the existing pattern of redirecting with `?error=<code>`. The `<code>` is a
  stable lowercase token already used by the JSPs. Never put exception text in the URL or page.
- **JSON endpoints** return a real status code (400/401/403/404/409/429/500) and one envelope:
  `{"ok": false, "code": "NOT_FOUND", "message": "<Vietnamese user-facing text>"}`, or `{"ok": true, ...data}`.
  Build it with Gson.
- **DAO convention (new and modified methods):** throw `Model.DAO.DataAccessException` (an unchecked exception that wraps the
  `SQLException`) on failure. Return `null`/empty **only** for "not found". Return `boolean` only to mean "a row matched".
  Migrate old methods when you touch them; there's no big-bang rewrite.
- Users see a generic message for unexpected errors: "Đã xảy ra lỗi hệ thống. Vui lòng thử lại." plus the request id.

## 7. Request id (T5.2)

- `Filter.RequestIdFilter`, the first filter: honour an inbound `X-Request-Id` if it matches `[A-Za-z0-9-]{8,64}`, otherwise
  generate a UUID. Store it in a request attribute and a `ThreadLocal`, echo it in the response header, and include it in log
  lines and error pages.

## 8. Money and concurrency (Phase 4)

- Money is in **integer coins** (`int`/`long`), matching `users.balance INT`. Parse user amounts with
  `Long.parseLong` and validate range (min 1, max set by config `MAX_DEPOSIT`). Never use `double` for money.
- Balance changes are a single conditional statement, and the code checks the row count:
  ```sql
  UPDATE users SET balance = balance + ? WHERE user_id = ? AND balance + ? >= 0
  ```
- Status transitions are conditional:
  ```sql
  UPDATE transactions SET status = ? WHERE transaction_id = ? AND status = 'PENDING'
  ```
  Zero rows → someone else already acted → page flow `error=already_completed`, or 409 for JSON.
- Writes that must happen together (deduct + insert transaction + set tier + set expiry) happen in **one DB transaction**
  in a `Model.Service` method. The service owns the `Connection` (`setAutoCommit(false)`, `commit`, `rollback` in catch,
  close in `finally`) and passes it to DAO overloads `method(Connection conn, ...)`.
- Uniqueness rules get a named DB constraint (`UQ_<table>_<cols>`). The Java pre-check stays only to produce a friendly message.

## 9. File storage (Phase 2)

- Files live under `AppConfig.require("STORAGE_DIR")`, which is **outside** the webapp, in `<STORAGE_DIR>/<userId>/<uuid>.<ext>`.
  The on-disk name never contains user input.
- `documents.cloud_storage_url` stores the **storage key** `<userId>/<uuid>.<ext>`, not a URL. The display title stays in `title`.
- Files are only ever served through `DocumentController` (`viewDoc` / `downloadDoc`), after `DocumentAccess.canView`, with
  `X-Content-Type-Options: nosniff`.
- Allowed extensions (one constant): `pdf, docx, xlsx, pptx, txt, md, png, jpg, jpeg, gif`. Check the magic bytes
  for binary types.

## 10. Database schema changes

- `ASH_Database.sql` builds a **fresh** database. Every schema change also needs a migration so that existing databases
  can catch up:
  - `db/migrations/NNN_<slug>.sql`: idempotent, guarded with `IF NOT EXISTS` / `IF COL_LENGTH(...) IS NULL`.
  - `db/migrations/NNN_<slug>.rollback.sql`: undoes it.
- Apply the same change to `ASH_Database.sql` in the same commit.
- Data backfills print counts before and after, and must be safe to run twice.
- Record each applied migration in `STATUS.md` → *Migrations*.

## 11. Verifying a fix manually

There are no automated tests yet (Phase 7). Every security task's **Verify** step uses curl against a running instance:

```bash
BASE=http://localhost:8080/AIStudyHub
# log in as user A, keep cookies
curl -s -c a.txt -b a.txt -d "action=login&email=A@x.com&password=..." "$BASE/AuthController" -o /dev/null -w "%{http_code} %{redirect_url}\n"
# try to act on user B's resource with A's cookies
curl -s -b a.txt -d "action=deleteDoc&docId=<B's doc id>" "$BASE/DocumentController" -o /dev/null -w "%{http_code} %{redirect_url}\n"
# then confirm in the DB that B's row is untouched
```

After T3.2 (CSRF) lands, first GET a page to obtain `csrfToken`, then send `_csrf=` with each POST.
Record the exact commands and results in the task's notes in `TASKS.md`.

---

## Decisions log

| Id | Date | Decision | Why | Status |
|---|---|---|---|---|
| D-01 | 2026-11-26 | Stay on NetBeans Ant + Tomcat; no Maven/Spring | Team toolchain; scope is fixing, not re-platforming | Proposed — confirm with user |
| D-02 | 2026-11-26 | Config via `AppConfig`: env → sysprop → `~/.aistudyhub/app.properties` | Works in NetBeans on Windows without editing system env; keeps secrets out of the repo | Proposed — confirm with user |
| D-03 | 2026-11-26 | `java.util.logging`, not log4j/slf4j | Zero new jars; `log4j-api` alone is unusable without `log4j-core` | Proposed |
| D-04 | 2026-11-26 | Money is integer coins | Matches `users.balance INT`; avoids a schema change to DECIMAL | Proposed — confirm with user |
| D-05 | 2026-11-26 | Rotate leaked secrets; git-history rewrite **optional** | Rotation is what removes the risk. A rewrite disrupts every teammate's clone | Proposed — user decides on rewrite |
| D-06 | 2026-11-26 | Other users' private resources answer as "not found" | Avoids leaking existence (checklist 403 vs 404) | Adopted |
| D-07 | 2026-11-26 | Rate limiting and lockouts stay in-memory | Single Tomcat instance; documented assumption. Revisit if scaled out | Proposed |
| D-08 | 2026-11-26 | A missing required config key is logged at startup (`ConfigValidationListener`) and fails closed on first use via `AppConfig.require`; it does not abort deployment | The app stays deployable so the admin can read the log, while DB/SMTP/Gemini/remember-me stay unusable. Keeps seeding and the scheduler from taking the context down | Adopted in T0.2 |
| D-09 | 2026-11-27 | `DocumentAccess.canView(doc, viewerId, viewerIsAdmin)` is the real signature; the 2-arg form is "not admin" | Conv §2.2 says admins can view everything, and the service must not depend on the servlet API, so the caller passes `Access.isAdmin(request)` | Adopted in T1.3 |
| D-10 | 2026-11-27 | `AccessRules` is stricter than the T1.1 text: `/DocumentController` is public only for `viewPublicPage`/`viewDoc`; any `admin*` action on `/TransactionController` is admin-only; admin paths match case-insensitively | Default deny; Windows file systems are case-insensitive, so a wrong guess toward "admin" is the safe direction | Adopted in T1.1 |
| D-11 | 2026-11-27 | A denial on a JSON endpoint carries both the legacy `"success": false` and the envelope fields `ok`/`code`/`message`; `ChatBotController` denials stay `text/plain` | The current client JS reads `data.success` / the response text; T5.5 converts the clients and drops the legacy flag | Adopted in T1.1, revisit in T5.5 |
| D-12 | 2026-11-27 | An admin may set a transaction to `PROCESSING` as well as `SUCCESS`/`CANCELLED`; nothing can be set back to `PENDING` | The admin UI exposes PROCESSING as a real intermediate state; T1.7's text listed only SUCCESS/CANCELLED | Adopted in T1.7 (confirm with the user) |
| D-13 | 2026-11-27 | A flagged document is visible only to its owner and admins, for every access path (view, download, bookmark, report, chat `VIEW/`) | Matches what Explore already did; before, a guest with a direct link could still open a flagged PUBLIC document | Adopted in T1.3 |
