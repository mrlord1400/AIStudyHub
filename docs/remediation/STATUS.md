# Remediation Status

**Last updated:** 2026-11-27 — Phase 2 merged to `revamp/d_dev` (PR #132; the runtime checks and the T2.2 migration are still the user's). Phase 3 started: T3.1a (escaping helpers + the eight admin pages) implemented on `revamp/fix/f_t3_1a_escape_admin_pages`,
checked offline (327 checks, `docs/remediation/verify/offline/run.sh`, now including real JSP rendering on embedded Tomcat), not yet run against a real Tomcat + SQL Server.
**Current phase:** 3 — Browser security (T3.1a done in code, awaiting review/merge)
**Next task:** T3.1b (chat pages), then T3.1c, T3.1d, T3.2 CSRF. T3.3 (session/headers) and T3.5 (rate limiting) do not depend on T3.1 and can go earlier. Phase 4 (T4.1) is independent and can run in parallel.

> Update this file at the end of every session (see README → Session protocol). Keep it short. History goes
> in the *Session log*; detail goes in TASKS.md.

## Snapshot

| Phase | Done / total | State |
|---|---|---|
| 0 Containment | 4 / 4 | **done**, approved by the user 2026-11-27 |
| 1 Authorization | 8 / 8 | **done**, approved by the user 2026-11-27 (PR #131) |
| 2 File storage | 0 / 3 (3 implemented, `[~]`, awaiting runtime verification) | merged (PR #132); runtime checks + T2.2 migration pending |
| 3 Browser security | 0 / 5 (T3.1 has 4 parts; T3.1a implemented `[~]`) | in progress |
| 4 Money & concurrency | 0 / 7 | not started |
| 5 Failure & observability | 0 / 6 | not started |
| 6 Data, config, ops | 0 / 8 | not started |
| 7 Tests | 0 / 3 | not started |
| 8 UI & accessibility | 0 / 3 | not started |

## Risk right now (what is still exploitable)

Every critical finding is still open. The worst, in order:
1. ~~Anyone can create an admin or change any user via `/AdminController` (F-01).~~ Closed in code by T1.1 (path gate + in-controller check); needs the curl check on a real instance.
2. ~~Every deployment has `admin@gmail.com` / `123` (F-02).~~ Closed with Phase 0 (no seeding by default; cleaning existing databases was the user's T0.3 step 4, covered by the Phase 0 approval).
3. ~~Uploading a `.jsp` gives remote code execution, and private files are downloadable by URL (F-05).~~ Closed in code by T2.1-T2.3 (files outside the web root, allow-list + content check, `/uploads/**` = 404). Needs the checks on a real instance **and the T2.2 migration run**, otherwise old documents answer 404.
4. ~~Any user can edit or delete any document, or read any chat (F-03, F-06).~~ Closed in code by T1.2-T1.4 (F-04 FRIENDS_ONLY and F-10 friendship too). The "any logged-in user can read any file by URL" gap is closed by T2.1.
5. Stored XSS (F-07): the admin pages are closed in code by T3.1a; chat, document/explore and the remaining user pages are still open (T3.1b-d). Secrets are no longer in the source tree (F-08); they remain in git history, which is acceptable once rotated (D-05, T0.1 closed).

## In progress

- **T3.1a** on branch `revamp/fix/f_t3_1a_escape_admin_pages` (from `origin/revamp/d_dev` @ `c56f587`, the merge of PR #132). `[~]`: implemented and checked offline by `T31Test`; the click-through (edit-user modal, report-reason edit/delete buttons) is not done. See the T3.1a notes in `TASKS.md`.
- **Phase 2 (T2.1–T2.3)** on branch `revamp/fix/f_t2_file_storage` (one branch, branched from `origin/revamp/d_dev` @ `6110934`, the merge of PR #131). All three are `[~]`: implemented, compiled with `javac --release 8`, and checked offline by
  `bash docs/remediation/verify/offline/run.sh` (308 checks pass, 76 of them new in `T2Test`). The per-task notes in `TASKS.md` say what was **not** verified.

## Blocked / waiting on the user

- **Before the first run of this branch: add `STORAGE_DIR` to your `app.properties`** (absolute path outside the web app, e.g. `C:/aistudyhub-data/uploads`; see the example file). Without it uploads fail closed and the startup log says `Missing required config: STORAGE_DIR`.
- **Run the T2.2 migration** (commands in the T2.2 notes in `TASKS.md`): dry run first, read the counts, the per-extension list of documents that will NOT be migrated (decision D-17) and the "needs a human look" list, then `--apply`. Until then, existing documents answer 404 (their rows still hold `/AIStudyHub/uploads/...`). Record the run under *Migrations* below. The old `uploads/` folder is dead (404) but still on disk: delete it after you have opened a sample of old documents.
- **Runtime verification of Phase 2** with Tomcat and SQL Server (Conv §11), highest value first:
  (1) upload one file of each allowed type, then `x.jsp`, `x.html`, `x.exe` and a `.pdf` that is really HTML: the last three must be rejected with a message and leave nothing in `STORAGE_DIR`;
  (2) `GET /AIStudyHub/uploads/<anything>` must be 404 (for anonymous and logged-in);
  (3) view, download, replace, keep-both, cancel and delete a document; delete a folder with nested documents (this is the one path whose success branch H2 cannot run: the recursive CTE delete) and confirm the files vanish;
  (4) delete an account (own and via admin): `<STORAGE_DIR>/<uid>/` is gone;
  (5) a Free user uploading above 50 MB is refused (D-15 / Q-1: please confirm `max_storage_mb` is the per-file cap).
- The `app.properties` file (template: `docs/remediation/app.properties.example`) is still required to start the app; see `CODEBASE.md` §11.
- If your DB/Tomcat start still reads old values from somewhere (a `-D` flag, a service env var), those override the properties file.
- **Uncommitted working copy** on the user's local branch `fix-folder-error`: `GeminiService.java` contains a **live API key**. Don't commit it.
  Other uncommitted edits there (`UploadController`, `SubscriptionScheduler` period 60 s→60 min, `report_create.jsp`,
  `nbproject/*`) belong to the user's in-flight work and aren't on `revamp/d_dev`. The user decides whether to bring
  any of them over via a `revamp/fix/` branch. The scheduler period change is probably wanted.

## Open decisions (need the user's answer; the proposed default applies until answered)

| Id | Question | Proposed default |
|---|---|---|
| D-01 | Stay on NetBeans Ant + Tomcat? | Yes |
| D-02 | Where do secrets live locally? | `%USERPROFILE%\.aistudyhub\app.properties`, env vars override |
| D-04 | Money unit | Integer coins (matches `users.balance INT`) |
| D-05 | Rewrite git history to purge the old Gmail password? | No; rotation is enough |
| D-08 | Missing config key: log at startup and fail closed on use, rather than aborting deployment | Adopted in T0.2 (see CONVENTIONS) |
| D-12 | Admin may set a transaction to `PROCESSING` in addition to `SUCCESS`/`CANCELLED` (T1.7 text listed only the last two) | Adopted because the UI offers it; **please confirm** |
| D-13 | Flagged documents are visible only to their owner and admins on every access path | Adopted in T1.3 (matches Explore) |
| F-76 | The create-user form labels the `STUDENT` option "Premium (VIP)" but creates a Free account | Left as is; T8.3. **Which is intended?** |
| D-14 | `/uploads/**` answers 404 for everyone | Adopted in T2.1 |
| D-15 | Per-file upload cap = tier's `max_storage_mb` (read from the DB), at most 100 MB; 0 = no uploads | Adopted in T2.3, **please confirm** (this is Q-1) |
| D-16 | The T2.2 migration copies and never deletes unless `--delete-old`; suspicious rows are reported, not migrated | Adopted in T2.2 |
| D-17 | Documents with a file type outside the new allow-list (old `.doc`, `.xls`, `.zip`, ...) are not migrated and answer 404; alternative: migrate them as download-only attachments | **Open, please decide**; the T2.2 dry run prints how many per extension |
| Q-1 | Meaning of `subscriptions.max_storage_mb` vs `total_storage_mb` | `max_storage_mb` = per-file cap (now enforced, D-15), `total_storage_mb` = total quota (to confirm before T6.4) |
| Q-2 | Buying Premium while already Premium: extend or refuse? | Extend by 30 days (to confirm before T4.3) |
| Q-3 | Refund the AI prompt when Gemini fails? | Yes (to confirm before T4.5) |

## Migrations applied

| # | File | Applied to | Date | By |
|---|---|---|---|---|
| T2.2 | `Utils.StorageMigration` (CLI, commands in TASKS T2.2). Not a SQL file: it rewrites `documents.cloud_storage_url` | not run yet | | |

## Branches

Model: [`docs/BRANCHING.md`](../BRANCHING.md). `main` is frozen. All work branches from and merges into `revamp/d_dev`.

| Branch | Purpose | State |
|---|---|---|
| `main` | pre-revamp archive | frozen at `09176ec`. Never touch |
| `revamp/d_dev` | integration branch (the new main) | created from `origin/main` @ `09176ec`; holds these docs |
| `fix-folder-error` | user's pre-revamp work | has uncommitted local changes (see Blocked); not part of the revamp |
| `revamp/fix/f_t0_containment` | T0.1–T0.4 (Phase 0) | merged to `revamp/d_dev` (PR #130) and approved by the user 2026-11-27 |
| `revamp/fix/f_t1_authorization` | T1.1–T1.8 (Phase 1) | merged to `revamp/d_dev` (PR #131) and approved by the user 2026-11-27 |
| `revamp/fix/f_t2_file_storage` | T2.1–T2.3 (Phase 2) | merged to `revamp/d_dev` (PR #132) |
| `revamp/fix/f_t3_1a_escape_admin_pages` | T3.1a (Phase 3) | implemented, checked offline; independent review and push: see the session log |

## Session log

| Date | Agent/session | Did | Left for next |
|---|---|---|---|
| 2026-11-26 | audit | Audited the codebase against the review checklist. Wrote `docs/remediation/*` (README, STATUS, TASKS, FINDINGS, CONVENTIONS, CODEBASE, app.properties.example), `docs/standards/review-checklist.md` and the root `CLAUDE.md` pointer. Added `docs/BRANCHING.md`; created `revamp/d_dev` from `origin/main` and committed the docs there | Start T0.1/T0.2 on `revamp/fix/f_t0_2_app_config`. User to answer the open decisions |
| 2026-11-26 | Phase 0 | Created `revamp/fix/f_t0_containment`. T0.4 renamed `FIlter`→`Filter`. T0.2 added `AppConfig` and `ConfigValidationListener` and moved the DB, SMTP, Gemini and remember-me secrets into config. T0.3 gated seeding behind `DEV_SEED_TEST_USERS` and removed the stored-hash password fallback. Updated CODEBASE (§3, §8, §10, new §11), CONVENTIONS (D-08) and the example properties | User: rotate secrets, create `app.properties`, delete seeded admin rows, run runtime checks, then tick T0.1–T0.4. Next code task: T1.1 |
| 2026-11-27 | Phase 1 | Closed Phase 0 in the docs. Created `revamp/fix/f_t1_authorization` from `origin/revamp/d_dev` (17f4bde). T1.1 `Access`/`AccessRules`/`AuthFilter` + `web.xml` filter order; T1.2 owner-scoped document mutations; T1.3 `DocumentAccess` + friendship check; T1.4 chat scoping; T1.5 friendship state machine in SQL; T1.6 folder-id ownership (also `FolderController`); T1.7 `Validation` + admin input checks; T1.8 sweep and the rewritten CODEBASE §5. Added `docs/remediation/verify/offline/` (229 checks). New findings F-74..F-76, new tasks T4.7 and T8.3, decisions D-09..D-13 | User: run the curl/click-through checks (see *Blocked*), answer D-12 / F-76, approve the PR. Next: T2.1 |
| 2026-11-27 | Phase 1 review | Independent review of the branch by a separate Opus agent: no authorization bypass found; harness re-run (229 then, 230 now). Four fixes made from it: (1) the admin error banner fragment was read as ISO-8859-1 (garbled Vietnamese; confirmed with Tomcat's Jasper) -> `pageEncoding` + a `*.jspf` property group in `web.xml`; (2) admin delete from `document_view.jsp` no longer removed the file -> `AdminReportController.adminDeleteDocument` deletes it after the DB delete via `Utils.StoredFiles`; (3) the new email regex rejected addresses registration accepts (`o'neil@x.com`, `dev@localhost`) -> made deliberately loose; (4) `FileExplore.jsp` bookmark JS now checks `response.ok`. Three doc inaccuracies corrected. Left as the reviewer noted: `createFriendship`'s `INSERT ... SELECT ... WHERE NOT EXISTS` is valid T-SQL by inspection but only ran on H2 | User: runtime checks (see *Blocked*), D-12 / F-76 answers, PR approval |
| 2026-11-27 | Phase 2 | Closed Phase 1 in the docs. Created `revamp/fix/f_t2_file_storage` from `origin/revamp/d_dev` (6110934). T2.1 `Utils.FileStorage` + `FileTypes`: uploads in `STORAGE_DIR/<uid>/<uuid>.<ext>`, `cloud_storage_url` holds the key, served only by `DocumentController` with `nosniff`, `/uploads/**` = 404 in `AuthFilter`, every delete path deletes the DB row before the file, account deletion removes the user's directory, `STORAGE_DIR` now required and validated at startup, `StoredFiles` removed. T2.2 `Utils.StorageMigration` (dry run default, copy not move). T2.3 extension allow-list, magic-byte checks, per-file cap from the tier, generic error text instead of exception messages in `UploadController`. Added `T2Test` (76 checks) and extended `Http` (shared session, multipart part) and `Db.str`. Decisions D-14..D-16; F-05 marked fixed in code | User: add `STORAGE_DIR`, run the T2.2 migration, run the checks in *Blocked*, confirm D-15/Q-1, approve the PR. Next: T3.1a (or T4.1 in parallel). Note: the user's `fix-folder-error` has uncommitted edits to `UploadController` that will conflict with this branch |
| 2026-11-27 | Phase 2 review | Independent review by a separate Opus agent: no blocker, three should-fix items, all fixed: (1) the migration tool advised `--delete-old` after a plain `--apply`, which cannot work (old paths are gone) -> advice corrected, `--delete-old` now deletes after all rows and only files no unmigrated row needs; (2) old files with types outside the allow-list silently become unreachable -> reported per extension, recorded as open decision D-17; (3) `confirm`/`keepBoth`/`cancel` used a file key kept in the session, so overlapping uploads could make two rows share a file -> the key now comes from the owner-checked DB row (session attribute removed), `replace` no longer deletes the old file when the pending row could not be removed. Also from the review: `0` in `max_storage_mb` now means "no uploads", stray files are cleaned up if an upload fails after saving, configuration errors are no longer reported as "file too large", `STORAGE_DIR` under Tomcat `webapps/` is flagged at startup. Known and left: `FolderController.deleteFolder` reads the tree's documents and deletes in two steps (a failed listing would orphan files); the Office preview in `viewDoc` writes document text unescaped (already T3.1d) | Re-review, then push |
| 2026-11-27 | Phase 3 (T3.1a) | Created `revamp/fix/f_t3_1a_escape_admin_pages` from `origin/revamp/d_dev` (c56f587). Added `Utils.Html`, `web/js/security.js`, pinned marked/DOMPurify tags with SRI (Conv §3), `/js/security.js` public (D-18). Escaped every dynamic value on the eight `admin_*.jsp` pages and `AdminLayout.tag`; the inline `onclick` handlers that interpolated values now read `data-*`. New harness piece `Web.java` + `T31Test`: the real `web.xml` and JSPs on embedded Tomcat 9 (the first JSP compile/render in the offline harness), 17 checks; 8 of its 9 page checks fail on the pre-change JSPs | Review, then push. Next: T3.1b (chat). `profile.jsp` and other user pages still render hostile names raw (T3.1d) |
