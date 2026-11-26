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
| Tests | `test/` exists but is empty |
| External | Google Gemini REST (`Utils.GeminiService`), Gmail SMTP (`Utils.EmailUtil`) |

`nbproject/*` files show as modified in git because NetBeans rewrites them per machine. Don't commit those changes unless intended.

## 3. Directory layout

```
ASH_Database.sql          schema + seed data
src/java/
  Controller/             17 servlets (one per feature area)
  FIlter/                 <- directory name has a typo ("FIlter"), but the package is `Filter`
    SessionSyncFilter.java
  Listener/AppStartupListener.java   seeds test users at startup (F-02)
  Model/DAO/              12 DAOs, plain JDBC
  Model/DTO/              10 POJOs
  Model/Service/SubscriptionRenewalService.java
  Utils/                  DBUtils, PasswordUtil, CookieUtil, EmailUtil, GeminiService, LinkUtil, SubscriptionScheduler
web/
  *.jsp                   30 pages (no WEB-INF protection: all directly reachable)
  WEB-INF/web.xml         only maps SystemConfigController + session timeout 30 min
  WEB-INF/tags/AdminLayout.tag   shared admin chrome
  WEB-INF/lib/            runtime jars
  uploads/<userId>/...    created at runtime inside the exploded WAR (F-05)
docs/remediation/         these docs
docs/standards/           review checklist
```

## 4. Request flow

```
Browser
  └─> SessionSyncFilter (@WebFilter "/*")   every 60 s re-reads user from DB: refreshes role/tier/balance,
  │                                          invalidates the session if the user is BANNED or deleted
  ├─> /MainController?action=X  ── switch(action) ──> forward to /XController   (most JSP links/forms use this)
  └─> /XController?action=Y     (many JSPs and JS fetch calls hit controllers directly)
         └─> DAO (new DAO() per request, new Connection per method) ──> SQL Server
         └─> request.setAttribute(...) + forward to some.jsp   OR   sendRedirect(...?error=code)
```

Key facts:
- **`MainController` is not a security boundary.** Every controller has its own URL and can be called directly.
- Controllers mostly use `processRequest` for both GET and POST, so mutations are GET-able.
- Each controller does its own `session.getAttribute("userId") == null` check, or doesn't (see §5).
- Errors travel as `?error=<code>` query params that the JSPs turn into Vietnamese messages.

## 5. Controllers, actions and current guards

"Guard" means the check as of the audit. ❌ = missing (see the finding).

| Controller (URL) | Actions | Guard today |
|---|---|---|
| `MainController` (`/MainController`) | router only; `guest` action renders `guest_dashboard.jsp` itself | none needed |
| `AuthController` | GET `logout`; POST `register`, `login`, `processResetPassword` | public (correct); reset requires session flag `ALLOW_RESET_<email>` |
| `ForgotPasswordController` | `sendOTP`, `verifyOTP` (JSON) | public; in-memory lockout (F-09) |
| `UserController` | `profile`, `updateProfile`, `deleteAccount` | login check |
| `DocumentController` | `explore`, `editDoc`❌, `viewPage`, `viewPublicPage`, `deleteDoc`❌, `updateDoc`❌, `updatePermission`, `viewDoc`, `downloadDoc`, `toggleBookmark`❌ | login (guests allowed for `viewPublicPage`, `viewDoc`). Owner check only in `viewPage`/`updatePermission`. FRIENDS_ONLY unchecked (F-03, F-04) |
| `UploadController` (multipart, 100 MB) | `upload`, `confirm`, `replace`, `keepBoth`, `cancel` | login; folderId unchecked (F-05) |
| `FolderController` | `createFolder`, `deleteFolder`, `viewFolder`, `editFolder`, `updateFolder` | login; **FolderDAO SQL is owner-scoped** (good reference implementation) |
| `ChatBotController` | POST message → Gemini loop (text/plain response) | login; sessionId ownership ❌ (F-06) |
| `SessionController` | `chatMain`, `createSession`, `viewSession`, `updateSessionName`❌, `deleteSession`, `findSessionByName`❌, `pinSession`❌, `deleteMessagesFrom`❌ | login; partial ownership (F-06) |
| `SearchController` | search own docs/folders | login |
| `FriendController` | `createFriendship`, `updateFriendshipStatus`❌, `deleteFriendship`, `friendList`, `pendingList`, `blockedList`, `findUserByEmail` | login; state machine ❌ (F-10) |
| `ReportController` | `report`, `createReport`, `updateReport` | login; update is scoped to reporter |
| `TransactionController` | `createTransaction`, `listTransactions`, `buyPremium`, `adminListTransactions`, `adminUpdateTransaction` | login; admin role on the two admin actions |
| `AdminController` (`/AdminController`) | GET `listUsers`, `listDashboard`, `listTransactions`, `profile`, `listPublicDocs`, `adminViewDoc`; POST `updateAdminProfile`, `createUser`, `updateUser`, `deleteUser` | **❌ none** (F-01) |
| `AdminReportController` | `adminReportList`, `adminUpdateReportStatus`, `adminDeleteReport`, `adminDeleteDocument` | login + ADMIN ✅ |
| `ReportConfigController` (`/admin/report-config`, `/ReportConfigController`) | GET `reportConfigList`, `reportConfigDelete`; POST `reportConfigAdd`, `reportConfigUpdate` | **❌ none** (F-01) |
| `SystemConfigController` (`/admin/system-config`, mapped in web.xml **and** by annotation) | GET list tiers; POST update tiers | **❌ none** (F-01) |

`MainController` action → controller routing is the `switch` in `MainController.processRequest` (~line 42). Notable entries:
`listUsers/createUser/updateUser/deleteUser/listDashboard` → AdminController; `reportConfig*` → ReportConfigController;
`adminReport*`, `adminDeleteDocument` → AdminReportController; `wallet` → `CreditWallet.jsp` directly.

## 6. Session attributes (the de-facto "auth context")

| Attribute | Type | Set by | Meaning |
|---|---|---|---|
| `userId` | Integer | AuthController login, `login.jsp` remember-me | the acting user. **The only trusted identity** |
| `username`, `role` (`STUDENT`/`ADMIN`), `tierId`, `balance` | String/Integer | login; refreshed by SessionSyncFilter every 60 s | `balance`/`tierId` in the session can be ~60 s stale; re-read the DB for decisions |
| `lastSessionSync` | Long | SessionSyncFilter | sync throttle |
| `pendingDocumentId`, `pendingDocumentPath`, `pendingDocumentTitle`, `pendingFileExtension` | | UploadController | the upload wizard between steps |
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
| `documents` | `user_id`, `folder_id`, `title`, `file_extension`, **`cloud_storage_url`** (currently `/AIStudyHub/uploads/<uid>/<name>.<ext>`), `file_size_mb DECIMAL(5,2)`, `ai_parsing_status` PENDING/PROCESSING/READY/FAILED, `sharing_permission`, `share_link_token` UNIQUE, `total_report_score`, `is_flagged`, `bookmark_count`, `download_count` |
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
| `Utils.DBUtils` | `getConnection()` (hard-coded creds, F-08), `closeConnection()` |
| `UserDAO` | register/login/get/update user, `updateBalance` (read-modify-write, F-20), `updatePassword(email,…)`, AI usage, premium expiry helpers, friend lists, `deleteUserAndAssociatedData` (**the one place that uses a real transaction**, a good pattern to copy), `seedTestUsers` (F-02) |
| `DocumentDAO` | ~780 lines: CRUD, explore query (`getExploreDocuments`, builds ORDER BY from a whitelist, so it's safe), duplicates, bookmarks, storage totals, admin listing, `privatizePublicDocumentsByUserId` |
| `DocumentTextDAO` | extracted text CRUD |
| `FolderDAO` | owner-scoped CRUD, recursive CTE delete, `buildFolderTree` (text tree for the chatbot) |
| `ChatSessionDAO` / `ChatMessageDAO` | sessions and messages; `ChatMessageDAO` also builds the **system prompt** (~line 28) and attachment messages |
| `FriendshipDAO` | status by user pair, create/update/delete |
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
1. `upload` saves `temp_<uuid8>_<sanitized name>` and inserts a `documents` row (PRIVATE, PENDING). It extracts text inline (PDFBox/POI) into `document_extracted_text` and marks READY, then redirects to `?step=edit`.
2. `confirm` sets the title/folder/sharing. If a same-title document exists in that folder it goes to `?step=duplicate`.
3. `replace` overwrites the old document's file and metadata and deletes the pending row. `keepBoth` renames to `"Title (n)"`. `cancel` deletes the pending row and file.
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

- `src/java/FIlter/` vs package `Filter`. This works on Windows only because the filesystem is case-insensitive. Put new filters in the same directory and package, or fix the directory name first with `git mv` (tracked in TASKS).
- Annotated `@WebFilter`s have **no defined order**. Declare filters in `web.xml` when order matters.
- `SystemConfigController` is mapped twice (annotation + `web.xml`). That works because the mappings are identical, but keep it in mind.
- `DocumentController` derives the on-disk path from `cloud_storage_url` by stripping the context path. Any change to storage must update `viewDoc`, `downloadDoc`, `deleteDoc`, `UploadController.deletePhysicalFile`/`renameToFinalName`, `AdminReportController.adminDeleteDocument`, and `UserDAO.deleteUserAndAssociatedData` (verify the last two when you get there).
- Comments are a mix of Vietnamese and English. User-facing strings are Vietnamese, so keep them Vietnamese.
- Many JSPs contain scriptlets that call DAOs directly (e.g. `user_dashboard.jsp`, `login.jsp`). Grep JSPs too when you change a DAO signature.
- The seeded `user@gmail.com` uses tier 1 (Guest), but registration uses tier 2. The tier ids are magic numbers (F-70).
