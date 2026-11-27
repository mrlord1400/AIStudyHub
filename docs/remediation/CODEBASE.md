# AIStudyHub Codebase Map

Snapshot as of **2026-11-26**, commit `a36e969`. It's meant to let an agent work without reading the whole codebase.
Line numbers drift. Search for names if they don't match. **Update this file when you change structure.**

## 1. What the product is

AIStudyHub is a student study-material hub. This is the SWP391 project at FPT University, and the UI text is in Vietnamese.
Users can:
- upload documents (pdf/docx/xlsx/pptx/txt/md/images) into folders;
- share documents as PRIVATE / FRIENDS_ONLY / PUBLIC;
- browse public and friends' documents ("Explore"), bookmark and download them;
- chat with a Gemini-powered AI that can search and read their documents;
- manage friends (request / accept / block);
- report documents (a score-based auto-flag);
- buy Premium with an internal coin wallet (admins approve deposits).

Admins manage users, transactions, reported documents, report-reason config and tier config.

## 2. Stack, build, run

| Item | Value |
|---|---|
| Language | Java, `javac.source=1.8` |
| Web | Servlet 3.1 + JSP + JSTL 1.2 (`web/WEB-INF/lib/jstl-1.2.jar`). No framework |
| Server | Apache Tomcat (`j2ee.server.type=Tomcat`). Context path **`/AIStudyHub`** (`web/META-INF/context.xml`) |
| Build | NetBeans Ant web project (`build.xml`, `nbproject/`). Build/run from NetBeans. `build/` and `dist/` are gitignored |
| DB | Microsoft SQL Server, database `ai_study_hub`. Schema + seed in **`ASH_Database.sql`**, the only schema source; there are no migrations yet |
| JDBC | `sqljdbc4` via `Utils.DBUtils` (DriverManager, no pool) |
| Libraries | gson 2.14, jbcrypt 0.4, pdfbox-app 3.0.3, poi/poi-ooxml 5.3.0, javax.mail 1.6.2 + activation, commons-compress/collections4, xmlbeans, log4j-api (unused) |
| Library paths | `nbproject/project.properties` references `C:\FPT\JavaLib\*.jar`, which is machine-specific. Some jars are also copied to `web/WEB-INF/lib` |
| Front end | Server-rendered JSP with Tailwind-style classes, inline `<script>`, `fetch` in chat/explore pages, `marked` from CDN for bot markdown |
| Tests | `test/` exists but is empty (JUnit comes in Phase 7). Until then `docs/remediation/verify/offline/` runs the real code against in-memory H2; since T3.1a `T31Test` also runs the real `web.xml` + JSPs in an embedded Tomcat 9 and checks the rendered HTML |
| External | Google Gemini REST (`Utils.GeminiService`), Gmail SMTP (`Utils.EmailUtil`) |

`nbproject/*` files show as modified in git because NetBeans rewrites them per machine. Don't commit those changes unless intended.

## 3. Directory layout

```
ASH_Database.sql          schema + seed data
src/java/
  Controller/             17 servlets (one per feature area)
  Filter/                 package `Filter` (directory renamed from the "FIlter" typo in T0.4)
    SessionSyncFilter.java   registered in web.xml (no longer @WebFilter): runs first
    AuthFilter.java          default-deny gate (T1.1), REQUEST + FORWARD, runs second
  Listener/AppStartupListener.java   seeds dev test users only when DEV_SEED_TEST_USERS=true (T0.3)
  Listener/ConfigValidationListener.java   logs which required config keys are present/missing (T0.2)
  Model/DAO/              12 DAOs, plain JDBC
  Model/DTO/              10 POJOs
  Model/Service/SubscriptionRenewalService.java
  Model/Service/DocumentAccess.java   canView / canEdit: the one place that decides document visibility (T1.3)
  Utils/                  AppConfig, Access (who is calling), FileStorage + FileTypes (where uploads live, allowed types, T2), StorageMigration (one-off CLI, T2.2), AccessRules (public/admin/authenticated path lists), Html (`esc`, output escaping for scriptlets, T3.1),
                          Validation (whitelists, ranged numbers), DBUtils, PasswordUtil, CookieUtil, EmailUtil, GeminiService,
                          LinkUtil, SubscriptionScheduler
web/
  js/security.js          escapeHtml + renderMarkdown (DOMPurify) for every page that builds HTML in the browser (T3.1); public in AccessRules
  *.jsp                   30 pages (no WEB-INF protection: all directly reachable)
  WEB-INF/web.xml         maps SystemConfigController, declares the filter order (SessionSyncFilter, AuthFilter), session timeout 30 min
  WEB-INF/jspf/admin_error_banner.jspf   shared error banner included by admin_manageUser / admin_manageTrans / admin_report_config
  WEB-INF/tags/AdminLayout.tag   shared admin chrome
  WEB-INF/lib/            runtime jars
  (no uploads/ folder any more: files live in STORAGE_DIR outside the webapp, T2.1; /uploads/** answers 404)
docs/remediation/         these docs
docs/standards/           review checklist
```

## 4. Request flow

```
Browser
  └─> SessionSyncFilter (web.xml, REQUEST)   every 60 s re-reads user from DB: refreshes role/tier/balance,
  │                                          invalidates the session if the user is BANNED or deleted
  └─> AuthFilter (web.xml, REQUEST + FORWARD) classifies the path with Utils.AccessRules: PUBLIC / AUTHENTICATED / ADMIN.
  │                                          Not logged in -> redirect login.jsp (401 JSON/text for AJAX endpoints);
  │                                          logged in but not admin on an ADMIN path -> 403. Anything unlisted = AUTHENTICATED.
  ├─> /MainController?action=X  ── switch(action) ──> forward to /XController   (most JSP links/forms use this)
  └─> /XController?action=Y     (many JSPs and JS fetch calls hit controllers directly)
         └─> DAO (new DAO() per request, new Connection per method) ──> SQL Server
         └─> request.setAttribute(...) + forward to some.jsp   OR   sendRedirect(...?error=code)
```

Key facts:
- **`MainController` is public but is not the gate.** It only forwards; `AuthFilter` runs again on the FORWARD to the real controller
  (`getServletPath()` is then the target), so `/MainController?action=listUsers` is checked as `/AdminController`.
- Controllers mostly use `processRequest` for both GET and POST, so mutations are GET-able.
- Controllers keep their own login/role check as a second layer (`Utils.Access`), but the filter is what decides. The acting user always comes from the session.
- Errors travel as `?error=<code>` query params that the JSPs turn into Vietnamese messages.

## 5. Controllers, actions and current guards

Reviewed in T1.8 (2026-11-27). "Path gate" is `AuthFilter`/`AccessRules`. "In-controller" is the second layer plus SQL scoping.
✅ = closed in Phase 1. ⚠ = known gap, with the task that closes it. Nothing is left unaccounted for.

| Controller (URL) | Actions | Path gate | In-controller / SQL scoping |
|---|---|---|---|
| `MainController` | router; `guest` renders `guest_dashboard.jsp` | public (the forward target is re-checked) | ✅ nothing sensitive of its own |
| `AuthController` | GET `logout`; POST `register`, `login`, `processResetPassword` | public | ✅ reset needs session flag `ALLOW_RESET_<email>`. ⚠ session fixation, remember-me, rate limits: T3.3-T3.5 |
| `ForgotPasswordController` | `sendOTP`, `verifyOTP` (JSON) | public | ⚠ OTP generation, lockout, rate limit: T3.5 |
| `UserController` | `profile`, `updateProfile`, `deleteAccount` | authenticated | ✅ acts only on `session.userId`; `updateProfile` needs the current password and validates name/email. ⚠ `deleteAccount` and other mutations are GET-able: T3.2 |
| `DocumentController` | `explore` | authenticated | ✅ SQL limits to PUBLIC (non-flagged) / accepted-friends' FRIENDS_ONLY |
| | `viewPublicPage`, `viewDoc` | **public** (guests allowed, only these two actions) | ✅ `DocumentAccess.canView`; invisible = 404 / `not_found` |
| | `downloadDoc`, `toggleBookmark` | authenticated | ✅ `DocumentAccess.canView` (bookmark answers a 404 JSON) |
| | `viewPage`, `editDoc`, `updateDoc`, `deleteDoc`, `updatePermission` | authenticated | ✅ owner only (`canEdit`) + SQL `AND user_id = ?` (`*ForOwner` DAO methods); file deleted only after the DB delete matched 1 row; foreign `folderId` rejected. |
| `UploadController` (multipart, 100 MB) | `upload`, `confirm`, `replace`, `keepBoth`, `cancel` | authenticated | ✅ ids come from the session wizard, DB writes are owner-scoped, `folderId` must be the caller's. ✅ (T2) file saved to `STORAGE_DIR/<uid>/<uuid>.<ext>`, extension allow-list + magic bytes, per-file cap from the tier. ⚠ total-quota literal: T6.4 |
| `FolderController` | `createFolder`, `deleteFolder`, `viewFolder`, `editFolder`, `updateFolder` | authenticated | ✅ DAO SQL owner-scoped; parent/current/edit folder ids must be the caller's (T1.6); a folder cannot be its own parent. ⚠ deeper cycles (A under its own descendant): not prevented |
| `ChatBotController` | POST message (text/plain) | authenticated (401 text) | ✅ session must belong to the caller before quota/storage; `VIEW/<id>` uses `canView`; `GETLINK/<id>` needs an owned folder. ⚠ quota race, error text, Gemini hardening: T4.5, T5.3, T6.2 |
| `SessionController` | `chatMain`, `createSession`, `viewSession`, `deleteSession` | authenticated | ✅ owner-checked (as before) |
| | `updateSessionName`, `pinSession`, `deleteMessagesFrom`, `findSessionByName` (JSON) | authenticated (401 JSON) | ✅ SQL scoped to the session owner (messages via sub-select); not-yours = 404. ⚠ JSON envelope/status codes: T5.5 |
| `SearchController` | search own docs/folders | authenticated | ✅ both queries filter `user_id`. ⚠ LIKE wildcards: T6.5 |
| `FriendController` | `createFriendship`, `updateFriendshipStatus`, `deleteFriendship`, lists, `findUserByEmail` | authenticated | ✅ state machine in SQL (`acceptRequest` addressee-only on PENDING; `block` never overwrites a block; BLOCKED rows deletable only by the blocker); reverse-duplicate request refused; `returnPath` whitelisted. ⚠ DB-level unique for the pair: T4.6 |
| `ReportController` | `report`, `createReport`, `updateReport` | authenticated | ✅ reporting needs `canView`; one PENDING report per user per document; update scoped to the reporter. ⚠ score recalculation is read-modify-write: T4.7 |
| `TransactionController` | `createTransaction`, `listTransactions`, `buyPremium` | authenticated | ✅ `listTransactions` scoped to the caller. ⚠ type/amount unvalidated, balance races, non-atomic Premium: T4.1-T4.4 |
| | `adminListTransactions`, `adminUpdateTransaction` | **admin** (any `admin*` action on this URL) | ✅ also `Access.isAdmin`; status whitelist `PROCESSING/SUCCESS/CANCELLED`. ⚠ double approval: T4.2 |
| `AdminController` | all (`listUsers`, `listDashboard`, `listTransactions`, `profile`, `listPublicDocs`, `adminViewDoc`, `updateAdminProfile`, `createUser`, `updateUser`, `deleteUser`) | **admin** | ✅ also `Access.isAdmin`; role/status/tier/balance/name/email/password validated; an admin cannot demote, ban or delete themself |
| `AdminReportController` | `adminReportList`, `adminUpdateReportStatus`, `adminDeleteReport`, `adminDeleteDocument` | **admin** | ✅ also `Access.isAdmin`; status whitelisted; `adminDeleteDocument` removes the physical file after the DB delete (`Utils.FileStorage`). ⚠ mutations are GET-able: T3.2 |
| `ReportConfigController` (`/admin/report-config`, `/ReportConfigController`) | GET `reportConfigList`; POST `reportConfigAdd/Update/Delete` | **admin** | ✅ also `Access.isAdmin`; code/severity/scores validated; delete is POST-only (GET answers 405) |
| `SystemConfigController` (`/admin/system-config`, mapped in web.xml **and** by annotation) | GET list tiers; POST update tiers | **admin** | ✅ also `Access.isAdmin`; every row validated first, then one DB transaction |
| `/uploads/**` (legacy folder) | any URL | everyone | ✅ `AuthFilter` answers 404 for all callers (`AccessRules.isRetiredUploadsPath`); files are no longer stored there (T2.1) |

`MainController` action → controller routing is the `switch` in `MainController.processRequest` (~line 42). Notable entries:
`listUsers/createUser/updateUser/deleteUser/listDashboard` → AdminController; `reportConfig*` → ReportConfigController;
`adminReport*`, `adminDeleteDocument` → AdminReportController; `wallet` → `CreditWallet.jsp` directly.

## 6. Session attributes (the de-facto "auth context")

| Attribute | Type | Set by | Meaning |
|---|---|---|---|
| `userId` | Integer | AuthController login, `login.jsp` remember-me | the acting user. **The only trusted identity** |
| `username`, `role` (`STUDENT`/`ADMIN`), `tierId`, `balance` | String/Integer | login; refreshed by SessionSyncFilter every 60 s | `balance`/`tierId` in the session can be ~60 s stale; re-read the DB for decisions |
| `lastSessionSync` | Long | SessionSyncFilter | sync throttle |
| `pendingDocumentId`, `pendingDocumentTitle`, `pendingFileExtension` | | UploadController | the upload wizard between steps |
| `conflictTitle`, `conflictFolderId`, `conflictSharingPermission`, `duplicateDocId` | | UploadController.confirm | duplicate-title resolution step |
| `returnChatSessionId`, `newAttachedDocTitle` | String | UploadController | return to chat after uploading from chat |
| `RECOVERY_OTP_<email>`, `RECOVERY_TIME_<email>`, `ALLOW_RESET_<email>` | | ForgotPasswordController | password-reset flow |

Cookie `remember_token` = Base64url(`email|expiry|hmac`) signed by `CookieUtil`. It's consumed by a scriptlet in `web/login.jsp`, which logs the user in.

## 7. Data model (`ASH_Database.sql`)

| Table | Key columns / notes |
|---|---|
| `subscriptions` | `tier_id` IDENTITY: **1 Guest, 2 Free, 3 Premium** (seed order). `max_storage_mb`, `ai_prompt_limit_per_day`, `price DECIMAL(10,2)`, `total_storage_mb` (Free 5120, Premium 51200) |
| `users` | `role` CHECK GUEST/STUDENT/ADMIN; `status` CHECK ACTIVE/SUSPENDED/BANNED; `tier_id` FK, default 2; **`balance INT`**; `ai_prompts_today`, `last_prompt_reset`; `expires_at` (Premium expiry), `expiry_notified`, `downgrade_notice_pending`; `email` UNIQUE NULL |
| `folders` | `user_id`, self-FK `parent_folder_id`, `folder_name`, `sharing_permission`. No uniqueness constraint on name |
| `documents` | `user_id`, `folder_id`, `title`, `file_extension`, **`cloud_storage_url`** (a **storage key** `<uid>/<uuid>.<ext>` since T2; rows not yet migrated by T2.2 still hold `/AIStudyHub/uploads/...` and answer 404), `file_size_mb DECIMAL(5,2)`, `ai_parsing_status` PENDING/PROCESSING/READY/FAILED, `sharing_permission`, `share_link_token` UNIQUE, `total_report_score`, `is_flagged`, `bookmark_count`, `download_count` |
| `bookmarks` | `UQ_user_document (user_id, document_id)` |
| `chat_sessions` | `session_id`, `session_name`, `user_id`, `is_pinned` |
| `chat_messages` | `session_id`, `sender` USER/BOT, `message_content NVARCHAR(MAX)`, `display` bit (hidden system/tool messages have `display=0`) |
| `transactions` | `amount DECIMAL(10,2)` (negative for WITHDRAW), `type` DEPOSIT/WITHDRAW, `status` PENDING/PROCESSING/SUCCESS/CANCELLED, `started_at`, `completed_at` |
| `document_extracted_text` | one row per document (UNIQUE `document_id`), used by the chatbot |
| `friendships` | `requester_id`, `addressee_id`, `status` PENDING/ACCEPTED/BLOCKED, `blocker_id`. `UQ_friendship(requester_id, addressee_id)` does **not** stop the reverse pair (B,A) |
| `report_reason_configs` | PK `reason_code`; `severity_level`, `base_score`, `auto_flag_threshold` |
| `document_reports` | `document_id`, `reporter_id`, `reason_code`, `status` PENDING/REVIEWED/DISMISSED/ACTION_TAKEN, `resolved_by_admin_id` |

Triggers maintain `updated_at` on `users`, `documents` and `friendships`.

## 8. DAOs and helpers

| Class | Notes |
|---|---|
| `Utils.AppConfig` | All secrets/env settings: env var → system property → `~/.aistudyhub/app.properties`. `require(key)` fails closed with `Missing required config: KEY` |
| `Utils.DBUtils` | `getConnection()` (credentials from `AppConfig`: `DB_URL`, `DB_USER`, `DB_PASSWORD`), `closeConnection()` |
| `UserDAO` | register/login/get/update user, `updateBalance` (read-modify-write, F-20), `updatePassword(email,…)`, AI usage, premium expiry helpers, friend lists, `deleteUserAndAssociatedData` (**the one place that uses a real transaction**, a good pattern to copy), `seedTestUsers(password)` (dev only, T0.3) |
| `DocumentDAO` | ~780 lines: CRUD (mutations are `*ForOwner`: `updateDocumentInfoForOwner`, `updateSharingPermissionForOwner`, `replaceDocumentFileForOwner`, `deleteDocumentForOwner`; admin deletes via `deleteDocumentAndDependencies`), explore query (`getExploreDocuments`, builds ORDER BY from a whitelist, so it's safe), duplicates, bookmarks, storage totals, admin listing, `privatizePublicDocumentsByUserId` |
| `DocumentTextDAO` | extracted text CRUD |
| `FolderDAO` | owner-scoped CRUD, recursive CTE delete, `buildFolderTree` (text tree for the chatbot), `isOwnedBy(folderId, userId)` for ids that come from a request. `getFolderById` is **unscoped**: compare `getUserId()` or use `isOwnedBy` |
| `ChatSessionDAO` / `ChatMessageDAO` | sessions and messages (`updateSessionNameForOwner`, `pinSessionForOwner`, `findSessionByName(userId, ...)`, `deleteMessagesFromId(msg, session, userId)` are owner-scoped); `ChatMessageDAO` also builds the **system prompt** (~line 28) and attachment messages |
| `FriendshipDAO` | status by user pair, `areFriends`, `createFriendship` (refuses an existing pair in either direction), `acceptRequest`, `block`, guarded `deleteFriendshipByUsers` |
| `ReportDAO`, `ReportReasonDAO` | reports and reason configs |
| `TransactionDAO` | create, list, get, `updateTransactionStatus` (unconditional, F-21) |
| `SubscriptionDAO` | tiers, `getPremiumPrice()` (returns <0 on failure) |
| `AdminDAO` | `getAllUsers`, `createUser` |
| `Utils.PasswordUtil` | BCrypt hash/verify ✅ |
| `Utils.CookieUtil` | remember-me token sign/verify |
| `Utils.EmailUtil` | OTP email, premium warning/downgrade emails (HTML built by string concat) |
| `Utils.GeminiService` | single `getGeminiResponse(List<ChatMessage>)`, model `gemini-3.1-flash-lite` |
| `Utils.SubscriptionScheduler` | `@WebListener`, hourly: `processExpiryWarnings()` (72 h before expiry) + `processExpiredSubscriptions()` (renew if balance ≥ price, else downgrade to tier 2) |

## 9. Feature flows worth knowing

**Upload wizard** (`UploadController` + `document_upload.jsp`):
1. `upload` checks the extension (allow-list) and the per-file cap, saves the file as `STORAGE_DIR/<uid>/<uuid>.<ext>`, checks the magic bytes (deletes it again on a mismatch), and inserts a `documents` row (PRIVATE, PENDING) whose `cloud_storage_url` is that key. It extracts text inline (PDFBox/POI) into `document_extracted_text` and marks READY, then redirects to `?step=edit`.
2. `confirm` sets the title/folder/sharing. If a same-title document exists in that folder it goes to `?step=duplicate`.
3. `replace` points the old document at the new file (DB first), removes the pending row, and only then deletes the old file. `keepBoth` renames the title to `"Title (n)"`. `cancel` deletes the pending row, then its file. The file is never renamed: only the title changes.
4. Suspended users can't set PUBLIC (they're silently downgraded to PRIVATE).

**Chatbot** (`ChatBotController`):
- It checks the daily quota (`subscriptions.ai_prompt_limit_per_day`, reset 24 h after `last_prompt_reset`), stores the user message, sends the **whole session history** to Gemini, then loops up to `MAX_AI_LOOP` times:
  - `RESPONSE:<text>` → final answer;
  - `SEARCH…` → injects the user's folder/document tree as a system message and asks again;
  - `VIEW/<docId>` → injects that document's extracted text and asks again (no ownership check, F-06).
- Tool messages are stored with `display=0`. Bot replies are rendered client-side with `marked`.

**Reports**: the reporter picks a `reason_code`; the document's `total_report_score += base_score`. When the score is ≥ `auto_flag_threshold`, `is_flagged = 1`. Flagged documents sort last and are hidden from others in Explore. Admins resolve reports in `AdminReportController`.

**Wallet / Premium**: the user creates a DEPOSIT (PENDING). An admin sets SUCCESS → balance += amount. `buyPremium` deducts the price, sets tier 3 and `expires_at = now + 30 days`. The scheduler auto-renews or downgrades.

**Sessions**: the timeout is 30 min (`web.xml`). SessionSyncFilter refreshes role/tier/balance every 60 s and kicks banned or deleted users out.

## 10. Gotchas

- `src/java/Filter/` was once misspelled `FIlter` (fixed in T0.4). On a clone that still has the old directory name in an IDE cache, clean and rebuild.
- Annotated `@WebFilter`s have **no defined order**. Filters are declared in `web.xml` (SessionSyncFilter, then AuthFilter); add `CsrfFilter` after them (T3.2). Do not add `@WebFilter`.
- **A new page or controller is "authenticated" by default.** To make it public or admin-only add it to the lists in `Utils/AccessRules.java`. A missing public page shows up as a redirect loop to `login.jsp`.
- `AuthFilter` sets request encoding to UTF-8 before reading `action`. Do not read request parameters in a filter before that, or Vietnamese form values get mangled.
- On a FORWARD, `request.getServletPath()` is the forward target. The `action` parameter is the original one unless the forward URL has its own query string (e.g. `DocumentController` forwards to `/MainController?action=editDoc&docId=...`), in which case the forward's value wins; `AuthFilter` re-reads it on every dispatch, so it classifies what the target will actually see.
- `SystemConfigController` is mapped twice (annotation + `web.xml`). That works because the mappings are identical, but keep it in mind.
- `getFolderById`, `DocumentDAO.findById`, `ChatSessionDAO.getSessionById` are **unscoped lookups**. Always follow them with an ownership/`DocumentAccess` check.
- **Files are only touched through `Utils.FileStorage`** (`resolve`/`fileOrNull`/`delete`/`deleteUserFiles`). Never build a path from `cloud_storage_url` yourself, never call `getRealPath` for uploads. Delete the DB row first and the file after (`deleteDoc`, `adminDeleteDocument`, `deleteFolder`, account deletion all do). New code that stores a file must use `FileStorage.save` and `FileTypes` (extension allow-list + `contentMatches`).
- **Escape every value you print** (Conv §3): `<%= Utils.Html.esc(x) %>` in scriptlets, `<c:out>`/`fn:escapeXml` in EL, `data-*` attributes instead of `onclick='fn("...")'`. `T31Test` renders the admin pages with hostile data and fails on any raw payload; add the new page to it when you convert one.
- Comments are a mix of Vietnamese and English. User-facing strings are Vietnamese, so keep them Vietnamese.
- Many JSPs contain scriptlets that call DAOs directly (e.g. `user_dashboard.jsp`, `login.jsp`). Grep JSPs too when you change a DAO signature.
- The seeded `user@gmail.com` uses tier 1 (Guest), but registration uses tier 2. The tier ids are magic numbers (F-70).

## 11. Configuration and the first admin (T0.2 / T0.3)

**Local setup.** Copy `docs/remediation/app.properties.example` to `%USERPROFILE%\.aistudyhub\app.properties` and fill in the
required keys (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `SMTP_USER`, `SMTP_PASSWORD`, `REMEMBER_ME_SECRET`, and `GEMINI_API_KEY` while
`AI_ENABLED` is not `false`). Environment variables and `-D` system properties override the file. If Tomcat runs as a service its
`user.home` differs, so point `AISTUDYHUB_CONFIG` at the file instead. At startup `ConfigValidationListener` logs which required
key names are missing; any code path that needs a missing key throws `Missing required config: KEY`.

**There is no default admin.** The app no longer seeds `admin@gmail.com` / `user@gmail.com` unless `DEV_SEED_TEST_USERS=true`
(plus `DEV_SEED_PASSWORD`), which is for a local machine only and logs a WARNING.

**Creating the first admin on a real database:**

1. Generate a BCrypt hash locally (JDK 9+, run from the repo root; the password never leaves your machine):
   ```
   jshell --class-path web/WEB-INF/lib/jbcrypt-0.4.jar
   jshell> org.mindrot.jbcrypt.BCrypt.hashpw("<choose a strong password>", org.mindrot.jbcrypt.BCrypt.gensalt())
   ```
2. Insert the user (tier 3 matches what the old seed used; adjust to your tier ids):
   ```sql
   INSERT INTO users (username, email, password_hash, role, tier_id, status)
   VALUES (N'<admin name>', N'<admin email>', N'<bcrypt hash from step 1>', 'ADMIN', 3, 'ACTIVE');
   ```
3. Existing databases that were started with an older build still hold the seeded rows. Delete them or give them a new password:
   ```sql
   SELECT user_id, email, role FROM users WHERE email IN ('admin@gmail.com', 'user@gmail.com');
   -- then either DELETE them (check their documents first) or UPDATE users SET password_hash = '<new bcrypt hash>' WHERE email = '...';
   ```
