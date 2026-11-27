# Remediation Tasks

Legend: `[ ]` todo · `[~]` in progress · `[x]` done and verified · `[-]` won't do (give the reason).
Ids are stable. Never renumber; add new tasks as `Tn.m+1` or with a letter suffix.
`F-xx` = finding in [FINDINGS.md](./FINDINGS.md). "Conv §n" = section n of [CONVENTIONS.md](./CONVENTIONS.md).

Order: phases run in order. Within a phase, follow the numbering unless the *Depends* line says otherwise.
Each task should fit in one session. If it doesn't, split it here first.

| Phase | Theme | Why this position |
|---|---|---|
| 0 | Containment: secrets, backdoors | Hours of work, removes the risk of someone simply logging in as admin |
| 1 | Authorization | One filter plus SQL scoping closes most critical holes |
| 2 | File storage | RCE and private-file exposure. Needs config from Phase 0 |
| 3 | Browser-side security | XSS/CSRF/session. Large but mechanical |
| 4 | Money & concurrency | Correctness of balances and Premium |
| 5 | Failure handling & observability | Touches every file. Do it after behaviour is correct to avoid conflicts |
| 6 | Data, config, operability | Pooling, Gemini hardening, pagination |
| 7 | Tests | Lock in the rules |
| 8 | UI state & accessibility | Checklist §01 and §10 pass |

---

## Phase 0 — Containment

### [x] T0.1 Rotate leaked secrets and keep the Gemini key out of git
- **Findings:** F-08
- **Owner:** the **user** does the rotation (an agent can't). The agent prepares the commit hygiene.
- **Steps:**
  1. User: revoke the Gmail app password for `aistudyhub.noreply@gmail.com` and create a new one; revoke and re-issue the
     Gemini API key; change the SQL Server `sa` password (better: create a dedicated `aistudyhub_app` login with rights on `ai_study_hub` only).
  2. Agent: make sure `src/java/Utils/GeminiService.java` is **not** committed with a key. Run `git diff` to check. Until T0.2
     lands, keep the key out of the file and pass it some other way, or keep the change unstaged.
  3. Decide on a git-history rewrite (decision D-05). The default is no rewrite, since rotation is enough.
- **Verify:** the old Gmail password fails SMTP login; the old Gemini key returns 400/403; `git log -p -S "<old key prefix>"` shows nothing new.
- **Depends:** none.
- **Notes (2026-11-26):** agent part done: `revamp/d_dev` and this branch contain no key (`GeminiService` is read from config). The
  user still has to rotate the Gmail app password, the Gemini key and the DB password, and keep the real `GeminiService.java` edit on
  `fix-folder-error` uncommitted. Still unticked until the user confirms the old values are dead.
- **Closed (2026-11-27):** Phase 0 was reported finished and approved by the user; merged to `revamp/d_dev` as PR #130. The "not verified" runtime items above were the user's to run and are covered by that approval.

### [x] T0.2 Introduce `Utils.AppConfig` and move all secrets and env literals into it
- **Findings:** F-08, F-70 (partly)
- **Files:** new `src/java/Utils/AppConfig.java`; `DBUtils.java`, `EmailUtil.java`, `GeminiService.java`, `CookieUtil.java`;
  new `docs/remediation/app.properties.example` (already drafted, so keep it in sync); a startup validator (new `Listener/ConfigValidationListener.java`, or extend `AppStartupListener`).
- **Steps:** implement Conv §1. Replace the literals with `AppConfig.require(...)`. Send the Gemini key in the
  `x-goog-api-key` header instead of the URL. Validate all required keys at startup and log the **names** of present keys (never values).
- **Verify:** with a key missing, startup logs `Missing required config: <KEY>` and the app refuses DB/Gemini/SMTP use. With a
  complete properties file: login, upload, chat and the OTP email all work. grepping `src/` for the old literal values of the DB password, SMTP password, Gemini key and remember-me secret (take them from `git show main:src/java/Utils/DBUtils.java` etc., and don't paste them into docs or commits) → nothing; no `private static final String` holds a credential.
- **Depends:** T0.1 step 1, or at least new values available locally.
- **Notes (2026-11-26):** `Utils.AppConfig` + `Listener.ConfigValidationListener` added; `DBUtils`, `EmailUtil`, `GeminiService`
  (key now in `x-goog-api-key`), `CookieUtil` read from config. `SMTP_HOST`/`SMTP_PORT` default to Gmail. See decision D-08.
  *Verified:* full `javac --release 8` of `src/java` compiles; with no config DB/Cookie/Gemini/Mail each throw `Missing required config: <KEY>`;
  file / env / `-D` precedence, blank-as-missing and strict booleans behave; no old DB password, SMTP password, Gemini key or remember-me secret
  remains in `src/` (checked against `git show main:`). *Not verified (needs Tomcat, SQL Server and real keys):* login, upload, chat and the OTP
  email with a complete properties file, and the startup log lines. Left for later: `STORAGE_DIR` is not validated until T2.1; `System.out`
  remains in other `UserDAO` methods (T5.1).
- **Closed (2026-11-27):** Phase 0 was reported finished and approved by the user; merged to `revamp/d_dev` as PR #130. The "not verified" runtime items above were the user's to run and are covered by that approval.

### [x] T0.3 Remove the seeded admin backdoor and the plaintext-password fallback
- **Findings:** F-02
- **Files:** `src/java/Listener/AppStartupListener.java`, `UserDAO.seedTestUsers`, `AdminController.java` (~138).
- **Steps:**
  1. Seeding runs only when `AppConfig.getBoolean("DEV_SEED_TEST_USERS", false)` is true, and logs a loud WARNING when it does.
     Seeded passwords come from config, not literals.
  2. Delete the "plain text" fallback in `updateAdminProfile`.
  3. Document how to create the first admin (a SQL snippet with a BCrypt hash generated locally) in `CODEBASE.md` §10.
  4. User: delete or re-password `admin@gmail.com` and `user@gmail.com` in every existing DB.
- **Verify:** fresh start with default config → no `admin@gmail.com` row. Logging in with `admin@gmail.com`/`123` fails.
- **Depends:** T0.2.
- **Notes (2026-11-26):** seeding now runs only with `DEV_SEED_TEST_USERS=true` (WARNING logged, password from `DEV_SEED_PASSWORD`);
  `UserDAO.seedTestUsers(String)`; the fallback that accepted the stored hash as the current password is gone from `updateAdminProfile`;
  first-admin procedure is in `CODEBASE.md` §11. *Verified:* compiles; `AppConfig.getBoolean("DEV_SEED_TEST_USERS", false)` is false with no config.
  *Not verified:* a fresh start against a real DB (no `admin@gmail.com` row, `admin@gmail.com`/`123` rejected). *Step 4 is the user's:* remove or
  re-password `admin@gmail.com` and `user@gmail.com` in every existing database.
- **Closed (2026-11-27):** Phase 0 was reported finished and approved by the user; merged to `revamp/d_dev` as PR #130. The "not verified" runtime items above were the user's to run and are covered by that approval.

### [x] T0.4 Fix the `src/java/FIlter` directory name
- **Findings:** CODEBASE §10 gotcha
- **Steps:** `git mv src/java/FIlter src/java/FilterTmp && git mv src/java/FilterTmp src/java/Filter` (two steps, because Windows is case-insensitive). Clean and rebuild in NetBeans.
- **Verify:** clean build succeeds; the app starts; SessionSyncFilter still runs (banning a user logs them out within 60 s).
- **Depends:** none. Do it before Phase 1 adds filters.
- **Notes (2026-11-26):** done with two `git mv` steps; `git ls-files` shows `src/java/Filter/SessionSyncFilter.java`. *Verified:* the whole source tree
  compiles. *Not verified:* NetBeans clean build, app start, and the ban-logs-out-within-60-s check (needs Tomcat). Stale `nbproject/private` entries
  pointing at `FIlter` are gitignored and harmless.
- **Closed (2026-11-27):** Phase 0 was reported finished and approved by the user; merged to `revamp/d_dev` as PR #130. The "not verified" runtime items above were the user's to run and are covered by that approval.

---

## Phase 1 — Authorization

> **Closed (2026-11-27):** Phase 1 was reported finished and approved by the user; merged to `revamp/d_dev` as PR #131. The "not verified (needs Tomcat + SQL Server)" items in the notes below were the user's to run and are covered by that approval.

### [x] T1.1 Single auth gate: `Utils.Access` + `Filter.AuthFilter`, filters ordered in `web.xml`
- **Findings:** F-01, F-73
- **Files:** new `Utils/Access.java`, new `Filter/AuthFilter.java`, `web/WEB-INF/web.xml`; remove `@WebFilter` from `SessionSyncFilter` and register it in `web.xml` instead.
- **Steps:** implement Conv §2.1–2.2.
  - *Public*: `/login.jsp`, `/register.jsp`, `/forgot_password.jsp`, `/reset_password.jsp`, `/index.jsp`, `/guest_dashboard.jsp`,
    `/publicDocument_view.jsp` (verify), `/AuthController`, `/ForgotPasswordController`, `/MainController` (the router itself;
    its forwards are checked on FORWARD), `/DocumentController` (the controller keeps its guest rules for `viewPublicPage`/`viewDoc`), static assets if any.
  - *Admin*: `/AdminController`, `/AdminReportController`, `/ReportConfigController`, `/admin/*`, `/admin_*.jsp`, plus the `TransactionController`
    actions `adminListTransactions` and `adminUpdateTransaction` (check the `action` param for this servlet).
  - *Everything else*: login required.
  - Replace the hand-written role checks in `AdminReportController` and `TransactionController` with `Access.isAdmin` (keep them as defence in depth).
  - Grep every JSP for where users are sent on login/logout so the public list is complete. Missing a public page shows up as a redirect loop.
- **Verify (curl, Conv §11):**
  - Anonymous `POST /AdminController action=createUser…` → redirect to login, no row created.
  - Student session → 403 for `/AdminController?action=listUsers`, `/admin/system-config`, `/ReportConfigController?action=reportConfigDelete&reasonCode=SPAM`, `/admin_manageUser.jsp`, and `/MainController?action=listUsers` (the FORWARD path).
  - Admin session → all admin pages work. Guest → `guest_dashboard`, login, register, forgot-password still work.
- **Depends:** T0.4.
- **Notes (2026-11-27):** added `Utils.Access` (who is calling), `Utils.AccessRules` (the three path lists as a pure function) and `Filter.AuthFilter`; `web.xml` now declares `SessionSyncFilter` then `AuthFilter` (REQUEST + FORWARD) and the `@WebFilter` is gone. `AdminController`, `ReportConfigController`, `SystemConfigController`, `AdminReportController` and the two admin actions of `TransactionController` keep an `Access.isAdmin` check as the second layer. Deviations (D-10, D-11): `/DocumentController` is public **only** for `viewPublicPage`/`viewDoc` (stricter than the task text); `TransactionController` actions starting with `admin` are admin-only; admin path matching is case-insensitive; denials on AJAX endpoints answer 401/403 with a body that has both the legacy `success:false` and the `ok/code/message` envelope; `AuthFilter` sets UTF-8 before reading `action`. `/uploads/**` is now login-only by default, but any logged-in user can still fetch any file (T2.1). *Verified offline:* compiles; `FilterHarness` drives the real `AuthFilter` through 44 anonymous/student/admin scenarios (every item of the Verify list, public pages, default deny, case variants, AJAX 401s). *Not verified (needs Tomcat + SQL Server, none in the agent's environment):* the curl **Verify** steps above against a real instance. Offline checks are in `docs/remediation/verify/offline/` (`bash docs/remediation/verify/offline/run.sh`). Specifically still open: the real FORWARD from `MainController` (servletPath on forward, welcome file `/`), and walking the real pages for redirect loops.

### [x] T1.2 Document edit/delete scoped to the owner
- **Findings:** F-03
- **Files:** `DocumentController.java` (`editDoc`, `updateDoc`, `deleteDoc`), `DocumentDAO.java` (new owner-scoped methods), and `UploadController` callers of `updateDocumentInfo`/`deleteDocument` (they use session-held ids, but switch them to owner-scoped too).
- **Steps:** Conv §2.3. Delete the physical file **only after** the owner-scoped DB delete returns 1 row. The `isAdmin` branches in
  `DocumentController` suggest admins once used these actions. Grep the `admin_*.jsp` files (none were found at audit time). Admin deletion goes through `AdminReportController.adminDeleteDocument`.
- **Verify:** user A cannot `editDoc`/`updateDoc`/`deleteDoc` user B's document (they get the `not_found` redirect, and B's row and file are unchanged). A can still do all three on their own documents.
- **Depends:** T1.1.
- **Notes (2026-11-27):** `DocumentDAO` mutations are now `updateDocumentInfoForOwner`, `updateSharingPermissionForOwner`, `replaceDocumentFileForOwner`, `deleteDocumentForOwner` (`WHERE ... AND user_id = ?`, must hit exactly 1 row); the unscoped versions are deleted. `DocumentController.deleteDoc` deletes the DB row first and the physical file only if it matched; not-yours answers the `not_found` redirect; the `isAdmin` branches are gone (admins delete through `AdminReportController`). `UploadController` uses the scoped methods and `handleReplace` also checks both documents belong to the caller. `document_view.jsp`: an admin viewing someone else's document no longer gets the edit button and the delete button targets `adminDeleteDocument` (which, after the independent review, now also removes the physical file via the new `Utils.StoredFiles`; the post-delete redirect is `listDashboard`, not `listPublicDocs` as before). *Verified offline:* `T12Test` (DAO level, 14 checks) and `T18Test` (controller level: A cannot edit/update/delete/re-permission B's document, row **and file** survive, admin cannot use `deleteDoc` on someone else's document, owner can do all of it). Run against the pre-Phase-1 sources (`revamp/d_dev` @ `17f4bde`) `T18Test` fails 18 of its first 24 checks (A could edit and delete B's document and file, strangers could read FRIENDS_ONLY files), so the checks are not vacuous. *Not verified (needs Tomcat + SQL Server, none in the agent's environment):* the curl **Verify** steps above against a real instance. Offline checks are in `docs/remediation/verify/offline/` (`bash docs/remediation/verify/offline/run.sh`).

### [x] T1.3 `DocumentAccess.canView` with a real friendship check, used everywhere
- **Findings:** F-04
- **Files:** new `Model/Service/DocumentAccess.java`; `DocumentController` (`viewPublicPage`, `viewDoc`, `downloadDoc`, `toggleBookmark`), `ChatBotController` (`VIEW/`, to be finished in T1.4), `ReportController.createReport`.
- **Steps:** Conv §2.2. Friendship = an ACCEPTED row in either direction (`FriendshipDAO.getFriendshipStatus`). A flagged document is visible to its owner only, matching Explore's behaviour (verify).
- **Verify:** C (not a friend of B) gets "not found" for B's FRIENDS_ONLY document on view, download and bookmark. B's accepted friend A can view it. Public documents still work for guests.
- **Depends:** T1.1.
- **Notes (2026-11-27):** new `Model.Service.DocumentAccess` (`canView(doc, viewerId[, isAdmin])`, `canEdit`) backed by the new `FriendshipDAO.areFriends` (an ACCEPTED row in either direction). Used by `viewPublicPage`, `viewDoc`, `downloadDoc`, `toggleBookmark`, `ReportController.report/createReport`; `viewDoc`/`downloadDoc` now answer 404 instead of 403 for anything not visible (D-06). Flagged documents are visible to their owner and admins only (D-13), which also closes the guest direct-link path. The ChatBot `VIEW/` use is in T1.4. *Verified offline:* `T13Test` (20 checks: guest/stranger/friend both directions/pending/blocked/flagged/admin/null) and `T18Test` (stranger gets 404 with no body for FRIENDS_ONLY view/download/bookmark; the friend gets the file and the download counter moves only for allowed requests). *Not verified (needs Tomcat + SQL Server, none in the agent's environment):* the curl **Verify** steps above against a real instance. Offline checks are in `docs/remediation/verify/offline/` (`bash docs/remediation/verify/offline/run.sh`).

### [x] T1.4 Chat sessions and messages scoped to the owner
- **Findings:** F-06
- **Files:** `ChatBotController.java`, `SessionController.java`, `ChatSessionDAO.java`, `ChatMessageDAO.java`.
- **Steps:**
  - `ChatBotController`: after parsing `sessionId`, load the session and require `getUserId() == userId` **before** using any quota or storing anything. `VIEW/<id>`: allow only if `DocumentAccess.canView(doc, userId)`; otherwise reply "not found".
  - `SessionController`: pass `userId` to rename, pin, delete-messages and search, and add `AND user_id = ?` in the DAO SQL (for messages: `AND session_id IN (SELECT session_id FROM chat_sessions WHERE user_id = ?)`).
  - Replace the hand-built JSON in `handleFindSessionByName` with Gson.
- **Verify:** A posting to B's `sessionId` → 403/404 and no message row inserted. A cannot rename, pin or delete B's session or messages. Search returns only A's sessions. Asking the bot to "VIEW/<B's private doc id>" gets "not found".
- **Depends:** T1.3.
- **Notes (2026-11-27):** `ChatBotController` loads the session and requires ownership before quota/storage (404 text otherwise); `VIEW/<id>` goes through `canView` and `GETLINK/<id>` through the new `FolderDAO.isOwnedBy`. `SessionController` passes the caller to rename/pin/delete-messages/search (DAO: `updateSessionNameForOwner`, `pinSessionForOwner`, `findSessionByName(userId, ...)`, `deleteMessagesFromId(msg, session, userId)` with a sub-select); not-yours = 404 with the legacy `{"success":false,...}` body; the search JSON is built with Gson. The chat JS still reads `data.success`; the envelope change is T5.5. *Verified offline:* `T14Test` (DAO) and `T18Test` (A posting to B's session gets 404 and neither B's messages nor A's quota change; rename/pin/delete-messages denied; search returns only A's sessions and a name containing quotes round-trips as valid JSON). The `VIEW/` branch of the AI loop needs Gemini and was **not** exercised. *Not verified (needs Tomcat + SQL Server, none in the agent's environment):* the curl **Verify** steps above against a real instance. Offline checks are in `docs/remediation/verify/offline/` (`bash docs/remediation/verify/offline/run.sh`).

### [x] T1.5 Enforce the friendship state machine
- **Findings:** F-10
- **Files:** `FriendController.java`, `FriendshipDAO.java`.
- **Steps:** Conv §2.4. Split `updateFriendStatusByUsers` into:
  - `accept(addresseeId=me, requesterId=target)`: requires `status='PENDING'`;
  - `block(me, target)`: from any state, sets `blocker_id = me`;
  - unblock: goes through delete, and only the blocker may do it (that check exists today).
  Validate `returnPath` against a whitelist (`friendList`/`pendingList`/`blockedList`). Also consider preventing a reverse-pair duplicate row (B→A when A→B exists). `createFriendship` checks status in both directions; verify that.
- **Verify:** a requester who tries to accept their own request → no change. A blocked user trying `status=ACCEPTED` → no change. Normal accept and block flows still work.
- **Depends:** T1.1.
- **Notes (2026-11-27):** `FriendshipDAO.acceptRequest` (addressee only, `status='PENDING'`), `block` (from PENDING/ACCEPTED, never overwrites an existing block, caller becomes `blocker_id`), `deleteFriendshipByUsers` (a BLOCKED row only deletable by `blocker_id`, checked in SQL), `createFriendship` refuses a pair that exists in either direction (`INSERT ... SELECT ... WHERE NOT EXISTS`); `updateFriendStatus`, `deleteFriendship(int)` and `updateFriendStatusByUsers` are removed. `FriendController` uses them, rejects `targetUserId == self` and whitelists `returnPath`. *Verified offline:* `T15Test` (25 checks) and `T18Test` through the controller. *Not verified:* the `INSERT ... SELECT ... WHERE NOT EXISTS` with `RETURN_GENERATED_KEYS` on real SQL Server (H2 accepted it); the race window is only closed by the T4.6 unique constraint. *Not verified (needs Tomcat + SQL Server, none in the agent's environment):* the curl **Verify** steps above against a real instance. Offline checks are in `docs/remediation/verify/offline/` (`bash docs/remediation/verify/offline/run.sh`).

### [x] T1.6 Folder ids from requests must belong to the caller
- **Findings:** F-05 (last bullet)
- **Files:** `UploadController` (`upload`, `confirm`), `DocumentController.updateDoc`.
- **Steps:** add `FolderDAO.isOwnedBy(folderId, userId)`. If the id isn't owned, treat it as root (`null`) or reject with `error=invalid_folder`. Choose rejection and keep it consistent.
- **Verify:** uploading or moving a document into B's folder id is rejected, and B's folder view doesn't show A's document.
- **Depends:** T1.2.
- **Notes (2026-11-27):** `FolderDAO.isOwnedBy`; `UploadController.upload/confirm` and `DocumentController.updateDoc` reject a foreign `folderId` with the page's `errorMessage` "Thư mục không hợp lệ." (the upload pages show the request attribute, not the `error` param; `upload` now validates before writing the file). **Extended beyond the task text** because the same hole existed in `FolderController`: `createFolder`/`updateFolder` parent ids, `viewFolder` and `editFolder` read or wrote another user's folder by id; all now require ownership (`error=invalid_folder` / `not_found`), and a folder cannot become its own parent. `user_dashboard.jsp` shows toasts for those two codes. *Verified offline:* `T18Test`. *Not exercised:* the multipart `UploadController` itself (no multipart support in the harness); only `isOwnedBy` and the wiring were reviewed. *Not verified (needs Tomcat + SQL Server, none in the agent's environment):* the curl **Verify** steps above against a real instance. Offline checks are in `docs/remediation/verify/offline/` (`bash docs/remediation/verify/offline/run.sh`).

### [x] T1.7 Validate admin inputs
- **Findings:** F-11
- **Files:** `AdminController` (`createUser`, `updateUser`), `SystemConfigController.doPost`, `TransactionController.handleUpdateTransactionStatus`, `ReportConfigController.doPost`.
- **Steps:** whitelist role (`STUDENT`/`ADMIN`), status (`ACTIVE`/`SUSPENDED`/`BANNED`), tier (existing ids), transaction status (`SUCCESS`/`CANCELLED`), severity. Add numeric ranges with caps. Make `SystemConfigController` validate every row **before** writing any, inside one DB transaction. Block an admin from demoting or banning themselves (avoids lock-out). Make `ReportConfig` delete POST-only.
- **Verify:** invalid values → `error=invalid_input` and nothing written. Valid edits still work.
- **Depends:** T1.1.
- **Notes (2026-11-27):** new `Utils.Validation` and `PasswordUtil.isValidPassword` (the same rule as register/profile; the two private copies in `AuthController`/`UserController` are left for later). `AdminController`: role `STUDENT/ADMIN`, status `ACTIVE/SUSPENDED/BANNED`, tier must exist, balance `0..1e9`, username <= 50, email format, password strength; an admin cannot demote, ban or delete themself (`error=self_modify`); `createUser` accepts the form's `STUDENT`/`ADMIN` (and the old `free`/`premium`). `TransactionController.adminUpdateTransaction`: `PROCESSING/SUCCESS/CANCELLED` (D-12: the UI offers PROCESSING; PENDING can't be restored and is now a disabled option). `SystemConfigController`: every row validated first, then `SubscriptionDAO.updateSubscriptions` in one transaction. `ReportConfigController`: code `[A-Za-z0-9_-]{1,50}`, severity whitelist, scores `0..999.99` (a parse failure is now an error, not a silent 0), delete is POST-only (GET answers 405) and `admin_report_config.jsp` uses a POST form. Admin pages show a shared banner (`WEB-INF/jspf/admin_error_banner.jspf`); the edit-user role select no longer offers `GUEST`. *Verified offline:* `T17Test` (50 checks, incl. nothing written on any invalid input and the all-or-nothing tier write). The email check is deliberately loose (no whitespace/`<>"`, exactly one `@`) because registration does no server-side email check and browsers accept `o'neil@x.com` / `dev@localhost`; a stricter regex locked such users out of profile edits. Note for the user: the create-user form labels the `STUDENT` option "Premium (VIP)" but it creates a Free (tier 2) account, as before (F-76, T8.3). *Not verified (needs Tomcat + SQL Server, none in the agent's environment):* the curl **Verify** steps above against a real instance. Offline checks are in `docs/remediation/verify/offline/` (`bash docs/remediation/verify/offline/run.sh`).

### [x] T1.8 Sweep: confirm the remaining endpoints
- **Files:** `UserController` (`updateProfile`, `deleteAccount`), `SearchController`, `ReportController` (all actions), `AdminReportController`, `DocumentController.explore`, `MainController` `guest`.
- **Steps:** for each action, record in `CODEBASE.md` §5: who may call it, what is scoped, and what's left. Fix small gaps here; open new tasks for big ones.
- **Verify:** the `CODEBASE.md` §5 table has no ❌ left.
- **Depends:** T1.2–T1.7.
- **Notes (2026-11-27):** the `CODEBASE.md` §5 table is rewritten per controller/action (path gate, in-controller check, SQL scoping, and which later task closes each remaining ⚠); it has no ❌ left. Small gaps fixed here: `ReportController.createReport` now refuses a second PENDING report by the same user for the same document (F-75), `AdminReportController.adminUpdateReportStatus` whitelists the status, `UserController.updateProfile` validates username/email. Reviewed with no change: `SearchController` (both queries filter `user_id`), `DocumentController.explore` (SQL-limited), `MainController` guest. Deferred and tracked: GET-able mutations incl. `deleteAccount` (T3.2), score recalculation races (T4.7), `/uploads/**` (T2.1). *Verified offline:* `T18Test`. *Not verified (needs Tomcat + SQL Server, none in the agent's environment):* the curl **Verify** steps above against a real instance. Offline checks are in `docs/remediation/verify/offline/` (`bash docs/remediation/verify/offline/run.sh`).

---

## Phase 2 — File storage

### [~] T2.1 Store uploads outside the web root and serve them only through the controller
- **Findings:** F-05
- **Files:** `UploadController.java`, `DocumentController.java` (`viewDoc`, `downloadDoc`, `deleteDoc`), `AdminReportController.adminDeleteDocument`, `UserDAO.deleteUserAndAssociatedData` (and anything else that turns `cloud_storage_url` into a path; grep for `getRealPath`).
- **Steps:** Conv §9. Add `Utils.FileStorage` with `save(userId, InputStream, ext) → key`, `open(key)`, `delete(key)`, `resolve(key)` (rejects `..` and absolute paths). Every path computation goes through it. Add `X-Content-Type-Options: nosniff` when serving.
- **Verify:** a new upload lands in `STORAGE_DIR/<uid>/<uuid>.<ext>`. `GET /AIStudyHub/uploads/...` → 404. View, download, replace, keep-both, cancel and delete all still work. Text extraction still works.
- **Depends:** T0.2, T1.3.
- **Notes (2026-11-27):** new `Utils.FileStorage` (`save`, `open`, `fileOrNull`, `delete`, `deleteUserFiles`, `resolve`, `isValidKey`, `validateRoot`) and `Utils.FileTypes`. The key regex is strict (`<digits>/<uuid>.<ext>`), so `..`, absolute paths, backslashes and old `/AIStudyHub/uploads/...` URLs are all invalid, and `resolve` also checks the result stays under the root. `STORAGE_DIR` is now **required** (must be absolute; `ConfigValidationListener` logs a missing key and checks the directory is writable and not inside the webapp). `UploadController` writes `<STORAGE_DIR>/<uid>/<uuid>.<ext>` and stores the key; the upload wizard no longer renames the file (the session no longer holds a path or key: `confirm`, `keepBoth` and `cancel` read the key from the owner-checked pending row), `replace` switches the old document to the new key, removes the pending row and only then deletes the old file (and not when the pending row could not be removed), `cancel` deletes the file only after the row. `DocumentController.viewDoc/downloadDoc` read through `FileStorage`, set `X-Content-Type-Options: nosniff` and a MIME type taken from the allowed extension (not guessed by the container). Every delete path now deletes the DB row first and the file after: `deleteDoc`, `adminDeleteDocument`, `FolderController.deleteFolder` (it used to delete files first), and account deletion (`UserController.deleteAccount`, `AdminController.deleteUser`) now also removes `<STORAGE_DIR>/<uid>/` (before, those files stayed forever). `Utils.StoredFiles` (T1.8 stopgap) is gone. `AuthFilter` answers **404 for everything under `/uploads`** for every caller (`AccessRules.isRetiredUploadsPath`), so files left in an old webapp stay unreachable until deleted. A row that still holds an old URL (not migrated yet) answers 404 / `error=file_not_found`; **run T2.2 before users need those documents**. *Verified offline:* `T2Test` (76 checks: key/traversal rules, save/open/delete, real `UploadController` through upload/confirm/keepBoth/replace/cancel with a multipart stub, view/download headers, delete paths, account deletion) and `T18Test`/`FilterHarness` updated to the new storage; the whole harness is 308 checks, all passing. *Not verified (needs Tomcat + SQL Server):* the curl **Verify** steps, a real multipart upload, `GET /AIStudyHub/uploads/...` → 404 through Tomcat's default servlet, and the success path of `FolderController.deleteFolder` (the recursive `WITH ... DELETE` is T-SQL that H2 cannot run; the harness only checks that files survive when the DB delete fails). Not done: files are still streamed through the servlet (no `X-Accel`-style offload, not needed).

### [~] T2.2 Migrate existing files and `cloud_storage_url` values
- **Steps:** a one-off migration (Java main class or SQL + script) that, for each document:
  - finds the file under the old webapp path;
  - copies it to `STORAGE_DIR` under a uuid name;
  - updates the row to the new key.
  It needs a dry-run mode, counts, re-runnability, and a report of missing files. Record the run in `STATUS.md` → *Migrations*.
- **Verify:** dry-run counts match `SELECT COUNT(*) FROM documents`. After the real run, a sample of old documents opens.
- **Depends:** T2.1.
- **Notes (2026-11-27):** `Utils.StorageMigration` (a `main` class, compiled with the app). **Dry run is the default.**
  ```
  # from the repo root after a NetBeans build; --legacy-webroot is the folder that CONTAINS uploads/ (the old exploded WAR)
  java -cp "build/web/WEB-INF/classes;web/WEB-INF/lib/*" Utils.StorageMigration --legacy-webroot="C:\path\to\apache-tomcat\webapps\AIStudyHub"            # dry run
  java -cp "build/web/WEB-INF/classes;web/WEB-INF/lib/*" Utils.StorageMigration --legacy-webroot="C:\path\to\apache-tomcat\webapps\AIStudyHub" --apply    # copy + update rows
  ```
  It reads DB and `STORAGE_DIR` from the same config as the app, so run it with the same `app.properties`. For each `documents` row it: skips rows that already hold a valid key; resolves the old URL under `<legacy-webroot>/uploads/` (rejects anything that escapes it); copies the file to `<STORAGE_DIR>/<owner id from the row>/<uuid>.<ext>`; checks the copy's size; then `UPDATE ... WHERE document_id=? AND cloud_storage_url=<old value>`. It **copies, never moves**, unless `--delete-old` (needs `--apply`, and only works in the same run: old files are deleted after all rows are processed and only if no unmigrated row still points at them; after a plain `--apply` the old paths are no longer recorded, so delete the old `uploads/` folder by hand). Rows it will not touch are reported with a reason: `MISSING_FILE`, `BAD_EXTENSION` (e.g. an old `.jsp` upload, but also every old `.doc`, `.xls`, `.zip`, ... which the old code accepted; the summary lists these by extension; **they become unreachable, decision D-17**), `BAD_CONTENT` (extension and bytes disagree), `UNSAFE_PATH`, `FAILED`. The summary prints the row count and the sum of all categories (they must match), plus the files under `uploads/` that no row points at (look at those by hand, they are the dangerous ones), then tells you to delete the old `uploads/` folder after checking a sample. Re-running is safe (migrated rows count as `ALREADY_MIGRATED`). *Verified offline:* `T2Test` runs it on H2 against a fake legacy tree: dry run changes nothing and counts add up, apply copies and updates, dangerous/missing/traversal rows are left alone, re-run is a no-op, `--delete-old` removes only migrated files. *Not run on a real database* (the user must; then record the run in STATUS → Migrations). After `--apply`, give the new keys a test: open an old document in the app.
- **Depends:** T2.1.

### [~] T2.3 Allow-list file types and check content
- **Findings:** F-05
- **Files:** `UploadController.handleUpload`.
- **Steps:** keep one constant set of allowed extensions (Conv §9). Check magic bytes: `%PDF` for pdf; ZIP `PK\x03\x04` for docx/xlsx/pptx; PNG, JPEG and GIF signatures; reject NUL bytes in txt/md. Use the per-file size cap from `subscriptions.max_storage_mb` (verify what that column means; see T6.4). Show a clear Vietnamese rejection message, and never echo exception text (F-31).
- **Verify:** uploading `x.jsp`, `x.html`, `x.exe`, and a `.pdf` that is really HTML → rejected with a message. Valid files are accepted.
- **Depends:** T2.1.
- **Notes (2026-11-27):** `Utils.FileTypes` holds the one allow-list (`pdf docx xlsx pptx txt md png jpg jpeg gif`) and `contentMatches`: `%PDF-` within the first 1024 bytes; ZIP signature for docx/xlsx/pptx; PNG/JPEG/GIF signatures; no NUL byte anywhere in txt/md. `UploadController.handleUpload` checks the extension first (a file with none, e.g. `noext`, used to be stored as `unknown` and is now rejected), then the size, saves, then checks the content and deletes the file again on a mismatch. Messages are Vietnamese and never carry exception text (F-31 for this controller: `IllegalStateException` from Tomcat's multipart limit becomes "File quá lớn", everything else the generic system-error text; details go to the log). `FileStorage.save` refuses a disallowed extension on its own as defence in depth. **Per-file size cap** = `subscriptions.max_storage_mb` of the user's *current* tier read from the DB, capped at 100 MB (the `@MultipartConfig` limit). A tier value of `0` (or a missing tier) means "uploads not allowed" and is refused with its own message. That follows the dashboard, which already shows `max_storage_mb` as the per-file limit, but **Q-1 is still unconfirmed**. This enforces something that was only displayed before: Free = 50 MB, Premium = 100 MB with the seed data. The total quota (literal 5 GB for tier < 3) is untouched (T6.4). `chat_session.jsp`'s picker no longer offers `.doc`. *Verified offline:* `T2Test` (jsp/html/exe/svg/doc/no-extension/double extension rejected with nothing stored; HTML as `.pdf`, NUL in `.txt`, empty file rejected; real files accepted; limit read from the DB and raising it applies immediately). *Not verified:* a real browser upload through Tomcat's multipart handling, and the 100 MB multipart rejection path (only the message text is reviewed).

---

## Phase 3 — Browser-side security

### [~] T3.1 Escape all output (split across sessions)
- **Findings:** F-07
- **Shared first step (T3.1a):** create `Utils.Html.esc`, `web/js/security.js` (`escapeHtml`), pinned `marked` + `DOMPurify` with SRI. Do this before the page groups.
- [~] **T3.1a** Helpers + admin pages: `admin_manageUser.jsp`, `admin_manageDocument.jsp`, `admin_report_config.jsp`, `admin_manageTrans.jsp`, `admin_document_reports.jsp`, `admin_dashboard.jsp`, `admin_profile.jsp`, `admin_system_config.jsp`, `WEB-INF/tags/AdminLayout.tag`. Convert `onclick='fn("<%= ... %>")'` to `data-*` attributes.
- [~] **T3.1b** Chat: `chat_main.jsp`, `chat_session.jsp` (server-rendered messages, session names, the JS that renders bot markdown → DOMPurify).
- [~] **T3.1c** Documents/explore: `FileExplore.jsp`, `guest_dashboard.jsp`, `user_dashboard.jsp`, `document_view.jsp`, `publicDocument_view.jsp`, `document_edit.jsp`, `document_upload.jsp`, `search_results.jsp`, `share_document.jsp` (innerHTML templates → `escapeHtml`/`textContent`).
- [~] **T3.1d** The rest: `friendMain.jsp`, `profile.jsp`, `folder_edit.jsp`, `report_create.jsp`, `report_edit.jsp`, `CreditWallet.jsp`, `Membership.jsp`, `login.jsp`, `register.jsp`, `forgot_password.jsp`, `reset_password.jsp`, `index.jsp`; `EmailUtil` HTML; the `DocumentController.viewDoc` HTML writer (Office→HTML conversion output: escape the text it pulls from documents).
- **T3.1a notes (2026-11-27):** `Utils.Html` (`esc`, `escOr`), `web/js/security.js` (`escapeHtml`, `renderMarkdown` = `DOMPurify.sanitize(marked.parse())` with an escaped-text fallback) and the pinned `marked@12.0.2` / `dompurify@3.5.0` tags with SRI hashes (Conv §3; not used by a page yet, T3.1b/c do). `/js/security.js` is public in `AccessRules` (D-18). All eight `admin_*.jsp` pages: usernames, emails, titles, reason codes/descriptions, report details, tier names and the system-config messages are escaped (`Html.esc` in scriptlets, `<c:out>` in EL; the sidebar name on every page; the `displayName` EL attribute is escaped where it is set). The two `onclick` handlers that interpolated values (`openEditModal(...)` in `admin_manageUser.jsp`, `enterEditMode(...)` and the delete `confirm('...[${reasonCode}]...')` in `admin_report_config.jsp`) now read `data-*` attributes from listeners. `AdminLayout.tag` (not used by any page) escapes its `title`. What is left on those pages is numeric/boolean EL, `contextPath`, and fixed strings. *Verified:* new `T31Test` (embedded Tomcat 9 + Jasper + H2, the real `web.xml`, admin login through `AuthController`): 8 admin pages with hostile data, no raw payload and the escaped text present; against the pre-change JSPs 8 of those 9 checks fail. Whole offline harness: 327 checks pass. *Not verified:* a real browser (the data-attribute listeners run in JS the harness does not execute: edit-user modal fill and the report-reason edit/delete buttons need a click-through), SQL Server, and a production Tomcat. An ad-hoc render of `profile.jsp` (user side) with the same data showed the payload raw; that is T3.1d and no committed check covers it yet.
- **T3.1b-d notes (2026-11-27):** every page in the three groups is converted; the only raw output left is numeric/boolean/`contextPath`/fixed text. **Server data placed in JS** (`FileExplore`, `guest_dashboard`, `user_dashboard`) is now built as a list of maps and printed with Gson (which escapes `< > & '` and U+2028/9); the hand-written `escapeJs` helpers (which also leaked into HTML attributes) are deleted. Client templates wrap every value in `escapeHtml`, toasts use `textContent`, and `chat_session.jsp` renders bot markdown through `renderMarkdown` (marked 12.0.2 + DOMPurify 3.5.0, SRI) and user text through `escapeHtml`; the attachment name and the avatar initial reach JS through an escaped `data-*` element instead of `"${...}"` in a script. Session names in `onclick="deleteSession('id','name')"` became `data-name`. `EmailUtil` escapes the username and OTP. `DocumentController.viewDoc`: the Office-to-HTML preview uses `Html.esc`, no longer prints `poiEx.getMessage()` (F-31 for this spot), and sends `Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'`, so even a missed escape cannot run script. `T31Test` now renders 25 pages as the owner, a friend, a guest and an admin with `"><img src=x onerror=alert(1)>`, `'</script><script>alert(2)</script>`, `x" autofocus onfocus="alert(9)` in usernames, emails, folder/document/session names, chat messages, report reasons and tier names, and compiles every JSP; against the pre-change JSPs 14 of the user-page checks fail (the admin ones 8 of 8). *Not verified (needs a real Tomcat + SQL Server and a browser):* a click-through of the pages whose JS changed (explore, dashboard, guest, chat send/edit, admin edit modals), the `reset_password.jsp` page (needs an OTP session; only reviewed), and `Membership.jsp` (reached through a controller the harness does not drive).
- **Method:** Conv §3. Grep each file for `<%=`, `${`, `innerHTML`, `insertAdjacentHTML`, `onclick=`.
- **Verify per group:** create a user, document, folder, chat session and report reason whose names are `"><img src=x onerror=alert(1)>` and `'</script><script>alert(2)</script>`, then open every page in the group as both the owner and an admin. No alert fires and the text displays literally.
- **Depends:** T1.1.

### [~] T3.2 CSRF protection and POST-only mutations
- **Findings:** F-09
- **Files:** new `Filter/CsrfFilter.java`, `web.xml`, every JSP form and `fetch` call, controllers' GET handlers.
- **Steps:** Conv §4. Expose the token via `<meta name="csrf-token">` in a shared include. List every mutating action. For each one, make GET return 405, and convert links that trigger mutations (logout, delete buttons) into small POST forms.
- **Verify:** a POST without `_csrf` → 403. Every form in the app still works (walk through each feature). A GET to `deleteDoc` → 405.
- **Depends:** T3.1 (the page edits overlap, so it's less merge pain after it). Can start after T3.1a if needed.
- **Notes (2026-11-27):** `Utils.Csrf` (token, constant-time compare, `MUTATING_ACTIONS`), `Filter.CsrfFilter` (web.xml, last), `web/js/csrf.js` + `WEB-INF/jspf/csrf_head.jspf` included by all 31 pages (decision D-20: the script adds the token to forms, multipart actions and `fetch`, and turns `<a data-post data-confirm>` into POSTs). Logout links (19 pages), the delete-folder / delete-document links on the dashboard, the friend actions and the admin delete-document button are POSTs now; GET on any mutating action answers 405. Real bug found on the way: `AuthController` handled `logout` only in `doGet`, so a POST logout redirected without ending the session; it is handled in `doPost` now. `T32Test` (34 checks on embedded Tomcat) covers 403 without/with a wrong or foreign token, success with the token as field or header, 405 for GET, JSON/text rejections, multipart, logout, and a scan that every JSP that posts includes the token include. `downloadDoc` stays GET (it only increments a counter). *Not verified (needs a real Tomcat + SQL Server and a browser):* walking every form and button in a browser (the harness runs no JavaScript, so the `csrf.js` injection and the `data-post` click handler are covered only by review and by the server-side checks), `@MultipartConfig` upload with a real file picker.

### [~] T3.3 Session hardening and security headers
- **Findings:** F-09
- **Steps:** call `request.changeSessionId()` on successful login and on password reset. In `context.xml`, set the session cookie to HttpOnly, SameSite=Lax, and Secure when `APP_HTTPS=true`. Add a small filter setting `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: same-origin`. Check BANNED status before verifying the password, or return the same generic message either way.
- **Verify:** JSESSIONID differs before and after login. The response headers are present.
- **Depends:** T1.1.
- **Notes (2026-11-27):** `Filter.SecurityHeadersFilter` (first filter, REQUEST + ERROR): `nosniff`, `X-Frame-Options: SAMEORIGIN` (D-19: the document page iframes itself), `Referrer-Policy: same-origin`. `Listener.SessionCookieListener`: HttpOnly always, Secure when `APP_HTTPS=true`; `META-INF/context.xml` sets `SameSite=Lax` through Tomcat's `CookieProcessor`. Login calls `request.changeSessionId()` and renews the CSRF token; so do a verified OTP and a completed reset. The BANNED check runs before the password check (a right and a wrong password get the same `error=banned`). The `reset_password.jsp?email=` redirects URL-encode the email. `T33Test` (14 checks). *Not verified (needs a real Tomcat + SQL Server and a browser):* the Secure flag on a real HTTPS deployment (only the listener logic is unit-checked), and whether a front proxy strips or duplicates the headers.

### [~] T3.4 Revocable remember-me
- **Findings:** F-09
- **Files:** `CookieUtil`, `AuthController`, `login.jsp` (scriptlet auto-login → move into a filter or `AuthController`), new table `remember_tokens` (migration per Conv §10).
- **Steps:** use the selector/validator pattern. Store the SHA-256 of the validator, give it a 30-day expiry (config), rotate it on use, delete it on logout, delete all of a user's tokens on password change/reset and on ban. Set the cookie HttpOnly, SameSite=Lax, and Secure when on HTTPS.
- **Verify:** logout → the old cookie value no longer logs in. Password reset → the token from another browser is invalid.
- **Depends:** T3.3, T0.2.
- **Notes (2026-11-27):** `remember_tokens` (`db/migrations/001_remember_tokens.sql` + rollback, same DDL appended to `ASH_Database.sql`), `RememberTokenDAO`, `Model.Service.RememberMeService`, `Filter.RememberMeFilter` (replaces the scriptlet in `login.jsp`, which now only redirects a signed-in visitor). Selector/validator, only `SHA-256(validator)` stored, 30 days (`REMEMBER_ME_DAYS`), rotated on every use, HttpOnly + SameSite=Lax + Secure on HTTPS, path = the context path. Revoked on logout, `UserDAO.updatePassword` (change and reset), `UserDAO.updateUser` when the status is not ACTIVE, and user deletion (FK cascade). A wrong validator for a known selector deletes that one token (D-22). `CookieUtil`, the 10-year HMAC cookie and `REMEMBER_ME_SECRET` are removed (**existing remember-me cookies stop working: everyone signs in once more**). `T34Test` (32 checks, raw requests). **The user must run `db/migrations/001_remember_tokens.sql`** (the app logs and fails closed for remember-me if the table is missing; password login still works). *Not verified (needs a real Tomcat + SQL Server and a browser):* the migration on SQL Server (only H2 ran the DDL; `ASH_Database.sql` and the migration were not executed), and the browser checkbox end to end.

### [~] T3.5 Rate limiting and OTP hardening
- **Findings:** F-09
- **Files:** `ForgotPasswordController`, `AuthController`, new `Utils/RateLimiter.java` (in-memory, bounded, D-07).
- **Steps:**
  - Generate the OTP with `SecureRandom`.
  - Limit login failures per (IP+email) and per IP. Limit `sendOTP` per email (e.g. 1/min, 5/h) and per IP.
  - Count OTP verify attempts per **issued OTP** (in the session); 3 failures invalidate that OTP, with no 12-hour email-wide lockout.
  - Limits come from config.
  - Make the maps bounded (evict expired entries).
- **Verify:** the 6th login failure within a minute → throttled message. Another browser can't lock out a victim's reset. Sending the OTP twice within 60 s → the second is refused politely.
- **Depends:** T0.2.
- **Notes (2026-11-27):** `Utils.RateLimiter` (fixed window, bounded key map, injectable clock) and `Utils.AuthLimits` (all numbers from config; keys listed in `app.properties.example`). Login: 5 failures per (IP, email) and 20 per IP per minute, then `error=too_many_attempts` without checking the password; a success clears the pair. OTP: `SecureRandom` over the full `000000..999999`; send cooldown 60 s and 5/hour per (IP, email), 10/hour per email (aggregate), 20/hour per IP, counted before the email lookup so an unknown email behaves the same; wrong guesses counted per issued code in the requester's session (3, then the code is destroyed); the 12-hour email-wide lockout and its static maps are gone, so another browser cannot lock a victim out. Decision D-21: throttled replies stay HTTP 200 with `throttled:true` until T5.5. `T35Test` (29 checks). Left: the limiters are in memory and keyed on `getRemoteAddr()` (behind a reverse proxy every user would share the proxy's IP unless Tomcat's `RemoteIpValve` is configured; not done), `register` is not throttled, and `ForgotPasswordController` still answers a JSON body with HTTP 200 (T5.5). *Not verified (needs a real Tomcat + SQL Server and a browser):* the real SMTP send (the sender is stubbed in the harness) and the timing of the 60 s cooldown in the OTP page countdown.

---

## Phase 4 — Money & concurrency

### [ ] T4.1 Atomic balance changes
- **Findings:** F-20
- **Files:** `UserDAO.updateBalance` → replace with `adjustBalance(Connection, userId, delta)` per Conv §8 (returns false when the balance would go negative). Update all callers.
- **Verify:** fire 20 concurrent +1 adjustments (a small Java test or parallel curl via approved deposits) → the balance increases by exactly 20.
- **Depends:** none in Phase 4. Can run any time after Phase 1.

### [ ] T4.2 Conditional, transactional deposit approval
- **Findings:** F-21
- **Files:** `TransactionController.handleUpdateTransactionStatus`, `TransactionDAO`, new `Model/Service/WalletService.java`.
- **Steps:** in one DB transaction: `UPDATE transactions SET status=?, completed_at=SYSUTCDATETIME() WHERE transaction_id=? AND status='PENDING'`. If 1 row and SUCCESS+DEPOSIT, call `adjustBalance`. Commit. 0 rows → `already_completed`.
- **Verify:** approve the same transaction from two browser tabs simultaneously → the balance is credited once.
- **Depends:** T4.1.

### [ ] T4.3 Atomic Premium purchase and renewal
- **Findings:** F-22
- **Files:** `TransactionController.handleBuyPremium`, `SubscriptionRenewalService.renew`, new `Model/Service/PremiumService.java`.
- **Steps:** `purchase(userId)` and `renew(userId)` each run in one transaction: conditional deduct → insert the WITHDRAW transaction → set tier 3 and expiry. Any failure → rollback. The scheduler uses the same `renew`. If already Premium, decide whether buying extends the expiry or is refused, and record that in the Decisions log.
- **Verify:** double-clicking Buy → one deduction. Forcing a failure after the deduct (temporarily throw) → balance unchanged and tier unchanged.
- **Depends:** T4.1.

### [ ] T4.4 Money parsing, type whitelist, fail-closed price
- **Findings:** F-23
- **Steps:** amounts parsed as integer coins with min/max from config (`MAX_DEPOSIT`). Users can only create `DEPOSIT` transactions. If the price lookup fails → error, never the 99000 fallback. Follow decision D-04 (integer coins).
- **Verify:** `amount=1.5`, `amount=-5`, `amount=1e12`, `type=WITHDRAW` → rejected.
- **Depends:** T4.2.

### [ ] T4.5 Atomic AI quota
- **Findings:** F-24
- **Files:** `ChatBotController`, `UserDAO.updateAiUsage`.
- **Steps:** reset the counter if 24 h have passed (a conditional UPDATE), then `UPDATE users SET ai_prompts_today = ai_prompts_today + 1 WHERE user_id = ? AND ai_prompts_today < ?`. 0 rows → 429. Consider refunding the prompt when Gemini fails (decide, record).
- **Verify:** with a limit of 3, 10 parallel requests → exactly 3 go to Gemini.
- **Depends:** T1.4.

### [ ] T4.6 Named uniqueness constraints
- **Findings:** F-25
- **Steps:** migration adding filtered unique indexes:
  - `UQ_folders_user_parent_name` (one for `parent_folder_id IS NULL`, one for NOT NULL);
  - `UQ_documents_user_folder_title` (same split);
  - prevent reverse duplicate friendships (computed columns least/greatest + unique).
  Clean up existing duplicates first, with a dry-run report. Translate duplicate-key errors (SQL Server error 2601/2627) in one helper.
- **Verify:** concurrently creating the same folder name twice → one succeeds, the other gets the friendly duplicate message.
- **Depends:** T5.4 helps but isn't required.

### [ ] T4.7 Atomic report score update
- **Findings:** F-75 (second half)
- **Files:** `ReportController.createReport/updateReport`, `AdminReportController.adminDeleteReport`, `DocumentDAO.updateReportMetrics`, `ReportDAO`.
- **Steps:** insert the report and adjust `documents.total_report_score` / `is_flagged` in one DB transaction using `SET total_report_score = total_report_score + ?` (and the flag recomputed from the new value in SQL) instead of read-modify-write in Java. Same for the update-report and admin-delete-report paths.
- **Verify:** 10 users reporting the same document concurrently add exactly the sum of their scores; deleting a report subtracts exactly its score.
- **Depends:** T4.1 (pattern), T1.8.

---

## Phase 5 — Failure handling & observability

### [ ] T5.1 Replace `System.out`/`printStackTrace` with `java.util.logging`
- **Findings:** F-40, F-30
- **Steps:** Conv §5, file by file. Remove PII from logs (`UserDAO.login` email, `ForgotPasswordController` email). Add a `logging.properties` note in `CODEBASE.md` describing how to set levels in Tomcat.
- **Verify:** `grep -rn "System.out\|System.err\|printStackTrace" src/java` → 0.
- **Depends:** best after Phases 1–4 to avoid conflicts.

### [ ] T5.2 Request id filter
- **Steps:** Conv §7. It's the first filter in `web.xml`. Logging includes the id (a custom JUL `Formatter` reading the ThreadLocal).
- **Verify:** the response has `X-Request-Id`; the log lines for that request carry it.
- **Depends:** T5.1.

### [ ] T5.3 Generic error pages and no leaked internals
- **Findings:** F-31
- **Steps:** add `<error-page>` entries in `web.xml` for 403/404/405/500/`java.lang.Throwable` → new `error.jsp` (generic Vietnamese text + request id). Remove `e.getMessage()` from every user-visible string. Make `MainController`'s catch log and show the error page instead of `login.jsp`.
- **Verify:** force an exception (e.g. stop SQL Server) → generic page with the request id, and the full stack trace is in the log.
- **Depends:** T5.2.

### [ ] T5.4 DAO error convention
- **Findings:** F-30
- **Steps:** add `Model/DAO/DataAccessException`. Migrate DAOs one at a time (one commit per DAO): throw on `SQLException`, return null only for not-found. Update callers to let it propagate to the error page, or handle it deliberately.
- **Verify:** with the DB stopped, pages show the error page rather than "empty list" or "not found".
- **Depends:** T5.3.

### [ ] T5.5 JSON endpoints: status codes and one envelope
- **Findings:** F-32
- **Files:** `ForgotPasswordController`, `SessionController` JSON handlers, `ChatBotController` (text/plain today; decide whether to keep it), `DocumentController.toggleBookmark`, and the JS callers in the JSPs.
- **Steps:** Conv §6. Update the client JS to branch on `response.ok` / `code`.
- **Verify:** each endpoint's error path returns a non-200 status with the envelope, and the UI shows the message.
- **Depends:** T5.3.

### [ ] T5.6 Audit table for admin and money actions
- **Findings:** F-40
- **Steps:** migration `audit_log(audit_id, occurred_at, actor_user_id, action, target_type, target_id, outcome DEFAULT 'FAILED', request_id, metadata NVARCHAR(MAX))`. Keep action names in one constants class. Write it for: create/update/delete user, ban/suspend, balance edit, transaction approve/cancel, tier config change, report-reason change, admin document delete, Premium purchase/renewal. Audit failures must be logged without breaking the main action.
- **Verify:** each action produces exactly one row with the correct outcome.
- **Depends:** T5.2.

---

## Phase 6 — Data, config, operability

### [ ] T6.1 Connection pool via Tomcat JNDI
- **Findings:** F-50
- **Steps:** a `Resource` in `context.xml` (pool sizes from config). Credentials can't sit in the repo's `context.xml`: either use a Tomcat-side `context.xml` override or build the pool in code from `AppConfig` with `org.apache.tomcat.jdbc.pool` (choose and record). `DBUtils.getConnection()` returns from the pool. Fix statement leaks (`updateBalance`, `seedTestUsers`) with try-with-resources.
- **Verify:** load-test a page (e.g. 200 requests) → no connection errors; the SQL Server connection count stays at or below the pool max.
- **Depends:** T0.2.

### [ ] T6.2 Harden `GeminiService`
- **Findings:** F-60
- **Steps:**
  - Connect/read timeouts from config.
  - Read the error stream on non-2xx and map it to typed outcomes (`TIMEOUT`, `UPSTREAM_ERROR`, `BLOCKED`, `EMPTY`).
  - Parse defensively and cap the response size.
  - Model name from config.
  - Cap the history sent (last N messages / M characters, from config).
  - Log the call duration plus token counts (`usageMetadata`) per user (§08 cost capture).
  - Add a kill switch `AI_ENABLED`.
  - In `ChatBotController`, show a distinct user message for timeout vs failure.
- **Verify:** set a 1 ms read timeout → the user sees the "AI đang bận/timeout" message and the thread returns quickly. A bad key → generic error, with the details in the log.
- **Depends:** T0.2, T4.5.

### [ ] T6.3 Pagination
- **Findings:** F-51
- **Steps:** server-side `OFFSET … FETCH` with a deterministic tiebreaker (`, document_id DESC`) for Explore, admin users, admin transactions, admin documents and the report list. Page size from config with a cap. Clamp the page number. Compute dashboard counts with `COUNT(*)`.
- **Verify:** with 1000 seeded documents, Explore returns 1 page, and next/prev work while preserving filters.
- **Depends:** T5.4 helps.

### [ ] T6.4 Quotas and tiers from data, not literals
- **Findings:** F-70
- **Steps:**
  - Confirm the meaning of `max_storage_mb` (per-file?) vs `total_storage_mb` (total) with the user.
  - Enforce both from the user's tier read from the DB, not the session.
  - Replace the magic tier ids with constants (`Tier.FREE`, `Tier.PREMIUM`) loaded/validated at startup.
  - Move the session timeout, OTP and lock durations, and the scheduler period into config.
- **Verify:** changing Free's `total_storage_mb` in admin config immediately changes the upload limit.
- **Depends:** T1.7.

### [ ] T6.5 LIKE escaping and input caps
- **Findings:** F-52
- **Steps:** add a helper that escapes `[`, `%`, `_` with `ESCAPE '\'`, and cap search input length (e.g. 100) in `ChatSessionDAO`, `FolderDAO`, `DocumentDAO`.
- **Verify:** searching `%` returns only titles containing `%`.

### [ ] T6.6 Scheduler hardening
- **Findings:** F-72
- **Steps:** log start/end/counts and errors with the exception. Document the single-instance assumption in `CODEBASE.md`. Make renewals idempotent by relying on the T4.3 conditional updates (`WHERE expires_at <= now AND tier_id = 3`).
- **Depends:** T4.3, T5.1.

### [ ] T6.7 Reproducible build: library references
- **Findings:** F-70 (`C:\FPT\JavaLib`)
- **Steps:** point `nbproject/project.properties` at jars inside the repo (`lib/` or `web/WEB-INF/lib`), so a fresh clone builds on any machine. Add the missing `javax.mail`/`activation` jars. Coordinate with the team, since everyone's NetBeans is affected.
- **Verify:** a fresh clone in a new folder builds in NetBeans without editing properties.

### [ ] T6.8 Move text extraction off the request thread (optional)
- **Findings:** F-61
- **Steps:** only if uploads are measured to be slow. Use a bounded executor and rely on `ai_parsing_status` PENDING→PROCESSING→READY/FAILED (the column exists), with a stale-job sweeper. Record the measurement first (checklist Part 2: "Made it asynchronous").

---

## Phase 7 — Tests

### [ ] T7.1 Test infrastructure
- **Steps:** JUnit 4 in the NetBeans `test/` folder. Tests must run without the network, the DB or the clock (inject `Clock`).
- **Verify:** "Test Project" in NetBeans runs one sample test green.

### [ ] T7.2 Unit tests for decision logic
- **Steps:** `DocumentAccess.canView`/`canEdit`, AuthFilter path classification, CSRF token compare, password rules, money amount parsing, the rate limiter, `Html.esc`, `FileStorage.resolve` (path traversal), the Gemini response parser (fixtures for blocked/empty/normal).
- **Depends:** the corresponding tasks.

### [ ] T7.3 Security regression script
- **Steps:** `docs/remediation/verify/security-regression.sh`, which collects the curl checks from the Phase 1–3 Verify steps (two users, an admin, a guest). Run it against a local instance; every check prints PASS/FAIL.
- **Depends:** Phase 3.

---

## Phase 8 — UI state & accessibility (checklist §01, §10)

### [ ] T8.1 Interface-state pass on the AJAX pages
- **Scope:** `chat_session.jsp`, `chat_main.jsp`, `FileExplore.jsp`, `forgot_password.jsp`: loading/error/empty as one chain, re-entry guards on send buttons, stale-response handling, timeout vs failure messages.

### [ ] T8.2 Accessibility pass
- **Scope:** clickable `div`s → buttons, names for icon-only buttons, form labels, modal focus management, keyboard-only walkthrough of upload, chat and admin user edit.

### [ ] T8.3 Admin user forms: labels that match what they do
- **Findings:** F-76
- **Scope:** `admin_manageUser.jsp`. The create form's account-type option `STUDENT` is labelled "Premium (VIP)" but creates a Free (tier 2) account; either relabel it "Học viên (Free)" and add a real Premium option (role STUDENT, tier 3, which `AdminController.createUser` already supports as `premium`), or decide it is intentional. Remove the stale `free`/`premium` string aliases once the form is settled.
- **Verify:** creating each account type produces the role and tier its label says.
