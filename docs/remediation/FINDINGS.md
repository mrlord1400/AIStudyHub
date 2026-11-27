# Audit Findings

Audit date: **2026-11-26**, against [`docs/standards/review-checklist.md`](../standards/review-checklist.md).
"Checklist §NN" refers to the section headings in Part 1 of that file. Baseline commit `a36e969` (branch `fix-folder-error`).
Paths are relative to the repo root. Line numbers drift — search for the quoted code if they don't match.

Severity: **C** = critical / exploitable now · **H** = high (data/money corruption) · **M** = medium (robustness, ops) · **L** = low.

Status of each finding lives in `TASKS.md` (the task that fixes it), not here. Add new findings at the bottom with the next free id.

---

## Security & access (checklist §05)

### F-01 (C) Admin controllers have no authentication or authorization
- `src/java/Controller/AdminController.java` — `doGet` (line ~22) and `doPost` (line ~102) never check session or role.
  Anyone, **including unauthenticated visitors**, can POST `action=createUser&role=admin`, `action=updateUser`
  (set any user's role/balance/tier/status), `action=deleteUser`, or GET `listUsers`/`listDashboard`.
- `src/java/Controller/SystemConfigController.java` (`/admin/system-config`) — no check; anyone can change tier prices, storage and AI limits.
- `src/java/Controller/ReportConfigController.java` (`/admin/report-config`, `/ReportConfigController`) — no check; anyone can add/update/delete report reasons. Delete is a **GET**.
- Admin JSPs (`web/admin_*.jsp`) are directly reachable by URL.
- Only `AdminReportController` and two `TransactionController` actions check `role == ADMIN`. Each is a hand-written copy.

### F-02 (C) Hard-coded admin account seeded on every startup
- `src/java/Listener/AppStartupListener.java` calls `UserDAO.seedTestUsers()` (`UserDAO.java:453`) which inserts
  `admin@gmail.com` / password `123` with role ADMIN (and `user@gmail.com` / `123`). Any deployment has a known admin login.
- Related: `AdminController.java:138` accepts the **stored hash itself** as a valid "current password" (plaintext fallback).

### F-03 (C) Document IDOR: any logged-in user can edit/delete any document
- `src/java/Controller/DocumentController.java`: `editDoc` (~87), `deleteDoc` (~135, also deletes the physical file), `updateDoc` (~177) — no owner check.
- SQL is unscoped: `DocumentDAO.updateDocumentInfo` (`WHERE document_id = ?`, ~79), `DocumentDAO.deleteDocument` (~150).
- Exploit: `updateDoc` with `sharingPermission=PUBLIC` on someone's private document, then view it.
- `updatePermission` and `viewPage` *do* check ownership — the pattern exists but isn't applied consistently.

### F-04 (C) FRIENDS_ONLY documents readable by every logged-in user
- `DocumentController.java` `viewPublicPage` (~118), `viewDoc` (~269), `downloadDoc` (~543): `"FRIENDS_ONLY".equals(permission)` grants access with no friendship check.
- `toggleBookmark` (~596) works on any document id, including private ones (leaks existence + count).

### F-05 (C) Uploaded files stored inside the web root with user-controlled extension
- **Status (2026-11-27): fixed in code by T2.1-T2.3, not yet run against Tomcat.** Files now live in `STORAGE_DIR` outside the webapp under UUID names, are served only through `DocumentController` after `canView`, only allow-listed extensions with matching content are accepted, `/uploads/**` answers 404, and `T2.2` migrates old files. The bullets below describe the original defect.
- `src/java/Controller/UploadController.java:143-157` writes to `getRealPath("") + "/uploads/<userId>/"`; the extension comes from the submitted filename (`:136-141`, `renameToFinalName` `:505`).
- Uploading `x.jsp` → Tomcat's JSP servlet executes it → **remote code execution**.
- Every file (including PRIVATE) is publicly downloadable at `/AIStudyHub/uploads/<userId>/<title>.<ext>`, bypassing `viewDoc`/`downloadDoc` checks. User ids are sequential; titles are guessable.
- Files in the exploded WAR are wiped on redeploy.
- `folderId` from the request is not checked to belong to the user (`UploadController.java:162-169`, `:251-258`; also `DocumentController.updateDoc`).

### F-06 (C) Chat session / message IDOR
- `src/java/Controller/ChatBotController.java:89,170` — `sessionId` from the request is never checked against the caller. An attacker posts into another user's session; Gemini receives that user's history and the reply is returned to the attacker.
- `ChatBotController.java:207` — the AI's `VIEW/<id>` command loads **any** document by id (`documentDAO.findById`) and feeds its extracted text to the model. Prompt injection → read any private document.
- `src/java/Controller/SessionController.java`: `updateSessionName` (~184), `pinSession` (~291), `deleteMessagesFrom` (~336) pass no user id; `findSessionByName` (~254) searches **all users'** sessions.
- SQL unscoped: `ChatSessionDAO.java:53` (rename), `:70` (pin), `:146` (search); `ChatMessageDAO.java:223` (delete from id).
- `viewSession` and `deleteSession` *do* check ownership.

### F-07 (C) Stored XSS everywhere — no output escaping
- 0 uses of `c:out` / `fn:escapeXml` across 30 JSPs. EL `${...}` and scriptlet `<%= %>` do not escape.
- Highest risk (attacker-controlled data shown to admins or other users):
  `web/admin_manageUser.jsp:131,132,168` (username/email, also inside an `onclick='...'` JS string),
  `web/admin_manageDocument.jsp:143`, `web/admin_report_config.jsp:211,214`,
  `web/document_view.jsp:56,207,262,383`, `web/publicDocument_view.jsp:38,93,156`,
  `web/friendMain.jsp:243-245`, `web/chat_main.jsp:375,396,421`, `web/chat_session.jsp:46,375,401,413,439,570,598,623`.
- DOM XSS via `innerHTML` + template literals: `web/FileExplore.jsp:418,454,457`, `web/guest_dashboard.jsp:212,215`, `user_dashboard.jsp`, `friendMain.jsp`, `chat_session.jsp`.
- Bot replies rendered with `marked` (unpinned CDN `cdn.jsdelivr.net/npm/marked/marked.min.js`) with no sanitizer.
- `SessionController.handleFindSessionByName` (~256) builds JSON by string concatenation (only `"` escaped).
- `EmailUtil.java:65,77` puts `username` unescaped into HTML email.

### F-08 (C) Secrets in source and git history
- `src/java/Utils/EmailUtil.java:15` Gmail app password — **in git history since commit `4d5f0f0`**.
- `src/java/Utils/DBUtils.java:16-18` — DB URL, the `sa` login with a trivial password, `trustServerCertificate=true`.
- `src/java/Utils/CookieUtil.java:24` — remember-me HMAC key.
- `src/java/Utils/GeminiService.java:15` — **live API key in the uncommitted working copy** (committed version is `""`). Key also sent in the URL query string (`:16`), so it ends up in proxy/access logs.

### F-09 (C) Session / authentication weaknesses
- No CSRF protection anywhere. `MainController` and most controllers route GET and POST identically, so every mutation is GET-able. Logout is GET (`AuthController.java:23`).
- Session fixation: login reuses the pre-login session (`AuthController.java:173`), no `changeSessionId()`.
- Remember-me (`CookieUtil.java`): stateless, 10-year expiry (`:30`), cannot be revoked (logout only deletes the browser cookie; password reset doesn't invalidate), no `Secure` flag, non-constant-time compare (`:65`). Consumed by a scriptlet in `web/login.jsp`.
- No rate limit on login.
- OTP (`ForgotPasswordController.java`): `java.util.Random` (`:61`); `nextInt(999999)` never yields 999999; lockout keyed on **email only** (`:42,110`) → anyone can lock any user out of password reset for 12 h; `sendOTP` unthrottled → mail bombing; maps are in-memory and unbounded.
- Banned check happens after password verification (`AuthController.java:166`) → confirms password validity to banned users (L).
- `reset_password.jsp?email=` + raw email in redirect (`AuthController.java:235,243`) — unencoded.

### F-10 (C) Friendship state machine not enforced
- `FriendController.updateFriendshipStatus` (~72) → `FriendshipDAO.updateFriendStatusByUsers` (`:127`) updates the row in either direction with no check of who is acting or current status:
  the **requester can accept their own request** (become "friends" with anyone → FRIENDS_ONLY docs), and a **blocked user can set status to ACCEPTED** to unblock themselves.
- `deleteFriendship` does check blocker for BLOCKED rows. `returnPath` param is concatenated into a redirect (`:88-96`) — open-redirect-ish within the app (L).

### F-11 (M) Admin input not validated
- `AdminController.updateUser` (~209): `newRole`, `newStatus`, `newTierId` written verbatim (no whitelist); balance edits leave no transaction record. `createUser` `accountType.toLowerCase()` NPE if missing.
- `SystemConfigController.doPost`: no ranges/caps; one bad row aborts midway (partial update).
- `TransactionController.handleUpdateTransactionStatus`: `newStatus` not whitelisted.

---

## Concurrency, money & versioning (checklist §07, §04)

### F-20 (H) Balance updates are read-modify-write (lost updates)
- `UserDAO.updateBalance` (`:403-422`) reads the user in Java, adds, writes the absolute value. Concurrent calls lose updates. Also opens a second connection via `getUserById`, and leaks the `PreparedStatement`.

### F-21 (H) Deposit approval can be applied twice
- `TransactionController.java:197-209` checks status in Java, then `updateTransactionStatus` unconditionally, then credits. Two concurrent approvals → double credit. Status update and credit are not in one DB transaction.

### F-22 (H) Premium purchase / renewal not atomic
- `TransactionController.handleBuyPremium` (`:220-284`): balance check → insert WITHDRAW → deduct → set tier → set expiry; 4 separate statements, no transaction. Double-submit → double charge; partial failure → Premium without payment or payment without Premium.
- `Model/Service/SubscriptionRenewalService.java:59-72`: same pattern in the scheduler.

### F-23 (H) Money types inconsistent / lossy
- `users.balance` is `INT`; `transactions.amount` and `subscriptions.price` are `DECIMAL(10,2)`; Java uses `double` and casts to `int` (`TransactionController.java:87,209,259`).
- User-supplied `type` on `createTransaction` is not whitelisted; amount has no cap.
- Premium price lookup failure silently falls back to 99000 (`TransactionController.java:233`, `SubscriptionRenewalService.java:25`) — fail-open.

### F-24 (M) AI daily quota race
- `ChatBotController.java:116-148` reads `ai_prompts_today`, increments in Java, writes. Parallel requests exceed the limit.

### F-25 (M) Uniqueness enforced only by pre-checks
- Folder name per parent (`FolderDAO.isFolderNameExists`) and document title per folder (`DocumentDAO.findDuplicateByTitle`) have no DB constraint.

---

## Failure handling (checklist §02)

### F-30 (M) Errors swallowed; failure indistinguishable from "no result"
- DAOs catch `SQLException`, print `e.getMessage()`, return `false` / `null` / empty list. Callers cannot tell "not found/rejected" from "DB down".
- `MainController.java:142` catches everything and forwards to `login.jsp`.
- Many controllers' catch-all redirects to `login.jsp?error=system_error` or `user_dashboard.jsp?error=system_error`.
- `catch (InterruptedException ignored)` (`ForgotPasswordController.java:76`), empty catches in `UploadController.java:167,256,598`.

### F-31 (M) Technical details shown to users
- `UploadController.java:219` — `"Hệ thống trục trặc: " + e.getMessage()` (can include server paths).
- `UploadController.java:151` — IOException message contains the absolute upload path.
- No `<error-page>` in `web/WEB-INF/web.xml` → Tomcat default error pages with stack traces.

### F-32 (M) Status codes don't carry outcomes
- JSON endpoints (`ForgotPasswordController`, `SessionController`) return 200 with `{"status":"error"}` / `{"success":false}`. Redirect-with-query-string is the error channel everywhere else.

---

## Observability (checklist §03)

### F-40 (M) No logging framework, no correlation, no audit
- 119 `System.out/err.println`, 66 `printStackTrace`, 0 loggers. (`log4j-api` jar is present but has no `log4j-core`, so it's unused.)
- PII in logs: `UserDAO.login` logs email on invalid password (`:103`); `ForgotPasswordController.java:70` logs email.
- No request/correlation id. No audit trail for admin actions (ban, delete user, balance edit, transaction approval, config change).

---

## Data access & performance (checklist §04)

### F-50 (M) No connection pool
- `DBUtils.getConnection()` (`:34`) → `DriverManager.getConnection` per call. Some methods open 2 connections per operation. Some don't close statements (`UserDAO.updateBalance`, `seedTestUsers`).

### F-51 (M) No pagination / unbounded responses
- `DocumentDAO.getExploreDocuments` (`:406`), `AdminDAO.getAllUsers`, `TransactionDAO.getAllTransactions`, `FolderDAO.getPublicFolders`.
- `AdminController.listDashboard` counts with `list.size()` (`:45-46`).

### F-52 (M) Search inputs not escaped for LIKE
- `ChatSessionDAO.java:151`, `FolderDAO.java:411`, `DocumentDAO.java:365,743` — `%`/`_` in user input act as wildcards (parameterised, so not injection). No length cap.

---

## External dependencies (checklist §08)

### F-60 (M) Gemini call has no timeout, no error handling, unbounded loop cost
- `src/java/Utils/GeminiService.java:20-78`: no connect/read timeout (a hung upstream holds a Tomcat thread forever); on HTTP 4xx/5xx `getInputStream()` throws and the error body is never read; response parsed by blind chaining (`candidates[0].content.parts[0].text`) — safety blocks or empty candidates throw NPE/IndexOutOfBounds.
- `ChatBotController` calls Gemini up to `MAX_AI_LOOP` times synchronously per user message; entire session history is sent every call (unbounded size/cost); no per-call cost tracking.
- Model name hard-coded in the URL.

### F-61 (M) Synchronous text extraction of up to 100 MB uploads
- `UploadController.extractTextFromFile` runs PDFBox/POI inline in the request.

---

## Configuration & operability (checklist §09)

### F-70 (M) Environment-dependent literals in code
- Storage quota hard-coded 5 GB (`UploadController.java:113`) although `subscriptions.total_storage_mb` / `max_storage_mb` exist; quota uses `tierId` from session.
- Tier ids `2` (Free) and `3` (Premium) are magic numbers across `AuthController`, `AdminController`, `TransactionController`, `UserDAO.downgradeToFree`, `UploadController`. Seed data uses tier `1` for a student.
- Session timeout, OTP durations, lock durations, remember-me duration, scheduler period — all literals.
- Library paths in `nbproject/project.properties` point to `C:\FPT\JavaLib\...` (machine-specific).

### F-71 (M) No tests
- `test/` is empty. No automated verification of any rule.

### F-72 (L) Scheduler assumptions undocumented
- `Utils/SubscriptionScheduler.java` — single-threaded hourly job; would double-charge if two app instances ran. Exceptions logged without stack.

### F-73 (L) Filter ordering undefined
- `Filter.SessionSyncFilter` is registered by `@WebFilter("/*")`. Adding a second annotated filter gives **unspecified order**; ordering must be declared in `web.xml`.

---

## Accessibility & resilient UI (checklist §10) and Interface state (§01)

Not audited in depth. Spot observations: clickable `<div>`s and icon-only buttons with only `title` exist; no visible error branch for failed `fetch` calls in `chat_session.jsp`. Schedule a dedicated pass (T8.x) after security work.

---

## Found during remediation

### F-74 (M) Folder ids from requests were trusted (fixed in T1.6)
- `FolderController.createFolder` / `updateFolder` accepted any `currentFolderId` / `parentFolderId` (a folder could be hung under another user's folder), and `viewFolder` / `editFolder` loaded another user's folder by id (`FolderDAO.getFolderById` is unscoped). Fixed with `FolderDAO.isOwnedBy` checks. Deeper cycles (folder under its own descendant) are still not prevented.

### F-75 (M) Report abuse: unlimited reports and non-atomic scoring
- `ReportController.createReport` had no "one PENDING report per user per document" check (only the form view called `checkValid`), so a user could repeat the POST to push any visible document over its auto-flag threshold. Duplicate guard fixed in T1.8.
- Still open: the score recomputation is read-modify-write in Java in `createReport`, `updateReport` and `AdminReportController.adminDeleteReport` (lost updates under concurrency) -> T4.7.

### F-76 (L) Admin user forms mislead
- `admin_manageUser.jsp` create form: option `STUDENT` is labelled "Premium (VIP)" but creates a Free (tier 2) account. The edit form offered role `GUEST` (removed in T1.7). `admin_manageTrans.jsp` offered PENDING as a target status (now disabled) -> T8.3 for the label.
