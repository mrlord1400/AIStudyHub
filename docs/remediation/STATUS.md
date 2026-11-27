# Remediation Status

**Last updated:** 2026-11-27 — Phase 0 closed (finished and approved by the user; merged to `revamp/d_dev` as PR #130). Phase 1 (T1.1-T1.8) implemented on `revamp/fix/f_t1_authorization`,
checked offline (230 checks, `docs/remediation/verify/offline/run.sh`), not yet run against Tomcat + SQL Server.
**Current phase:** 1 — Authorization (code done, awaiting the user's runtime verification and PR approval)
**Next task:** after Phase 1 is approved: Phase 2 starting with T2.1 (file storage). Phase 3 (T3.1+) can start in parallel only on files Phase 1 did not touch.

> Update this file at the end of every session (see README → Session protocol). Keep it short. History goes
> in the *Session log*; detail goes in TASKS.md.

## Snapshot

| Phase | Done / total | State |
|---|---|---|
| 0 Containment | 4 / 4 | **done**, approved by the user 2026-11-27 |
| 1 Authorization | 0 / 8 (8 implemented, `[~]`, awaiting runtime verification) | in review |
| 2 File storage | 0 / 3 | not started |
| 3 Browser security | 0 / 5 (T3.1 has 4 parts) | not started |
| 4 Money & concurrency | 0 / 6 | not started |
| 5 Failure & observability | 0 / 6 | not started |
| 6 Data, config, ops | 0 / 8 | not started |
| 7 Tests | 0 / 3 | not started |
| 8 UI & accessibility | 0 / 2 | not started |

## Risk right now (what is still exploitable)

Every critical finding is still open. The worst, in order:
1. ~~Anyone can create an admin or change any user via `/AdminController` (F-01).~~ Closed in code by T1.1 (path gate + in-controller check); needs the curl check on a real instance.
2. ~~Every deployment has `admin@gmail.com` / `123` (F-02).~~ Closed with Phase 0 (no seeding by default; cleaning existing databases was the user's T0.3 step 4, covered by the Phase 0 approval).
3. Uploading a `.jsp` gives remote code execution, and private files are downloadable by URL (F-05).
4. ~~Any user can edit or delete any document, or read any chat (F-03, F-06).~~ Closed in code by T1.2-T1.4 (F-04 FRIENDS_ONLY and F-10 friendship too). Still true: **any logged-in user can read any uploaded file by URL** (F-05, T2.1).
5. Stored XSS on admin pages (F-07). Secrets are no longer in the source tree (F-08); they remain in git history, which is acceptable once rotated (D-05, T0.1 closed).

## In progress

- **Phase 1 (T1.1–T1.8)** on branch `revamp/fix/f_t1_authorization` (one branch for the eight Phase 1 tasks, one commit per task, branched from `origin/revamp/d_dev`
  @ `17f4bde`). All eight are `[~]`: implemented, compiled with `javac --release 8`, and checked offline by `bash docs/remediation/verify/offline/run.sh`
  (real filter/controllers/DAOs; in-memory H2 in SQL Server mode; 230 checks pass). The per-task notes in `TASKS.md` say what was **not** verified. An independent review (Opus agent) is recorded in the session log.

## Blocked / waiting on the user

- **Runtime verification of Phase 1** with Tomcat and SQL Server: the curl checks in each task's **Verify** step (Conv §11), then tick T1.1-T1.8. Highest value first:
  (1) log in as admin, student and guest and click through every menu entry to catch a public page missing from `Utils/AccessRules.java` (symptom: redirect loop to `login.jsp`);
  (2) `/MainController?action=listUsers` as a student must be 403 (this proves the FORWARD dispatch is filtered under real Tomcat);
  (3) the friend request/accept/block buttons in `friendMain.jsp` (the `INSERT ... SELECT ... WHERE NOT EXISTS` in `FriendshipDAO.createFriendship` was only run on H2);
  (4) chat: rename, pin, delete-from-message, search, send;
  (5) admin: create user (both account types), edit user, transactions status select, report-config add/edit/delete, system-config save.
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
| Q-1 | Meaning of `subscriptions.max_storage_mb` vs `total_storage_mb` | `max_storage_mb` = per-file cap, `total_storage_mb` = total quota (to confirm before T6.4) |
| Q-2 | Buying Premium while already Premium: extend or refuse? | Extend by 30 days (to confirm before T4.3) |
| Q-3 | Refund the AI prompt when Gemini fails? | Yes (to confirm before T4.5) |

## Migrations applied

| # | File | Applied to | Date | By |
|---|---|---|---|---|
| — | none yet | | | |

## Branches

Model: [`docs/BRANCHING.md`](../BRANCHING.md). `main` is frozen. All work branches from and merges into `revamp/d_dev`.

| Branch | Purpose | State |
|---|---|---|
| `main` | pre-revamp archive | frozen at `09176ec`. Never touch |
| `revamp/d_dev` | integration branch (the new main) | created from `origin/main` @ `09176ec`; holds these docs |
| `fix-folder-error` | user's pre-revamp work | has uncommitted local changes (see Blocked); not part of the revamp |
| `revamp/fix/f_t0_containment` | T0.1–T0.4 (Phase 0) | merged to `revamp/d_dev` (PR #130) and approved by the user 2026-11-27 |
| `revamp/fix/f_t1_authorization` | T1.1–T1.8 (Phase 1) | implemented, checked offline and independently reviewed; pushed to origin; PR to `revamp/d_dev` not opened/merged |

## Session log

| Date | Agent/session | Did | Left for next |
|---|---|---|---|
| 2026-11-26 | audit | Audited the codebase against the review checklist. Wrote `docs/remediation/*` (README, STATUS, TASKS, FINDINGS, CONVENTIONS, CODEBASE, app.properties.example), `docs/standards/review-checklist.md` and the root `CLAUDE.md` pointer. Added `docs/BRANCHING.md`; created `revamp/d_dev` from `origin/main` and committed the docs there | Start T0.1/T0.2 on `revamp/fix/f_t0_2_app_config`. User to answer the open decisions |
| 2026-11-26 | Phase 0 | Created `revamp/fix/f_t0_containment`. T0.4 renamed `FIlter`→`Filter`. T0.2 added `AppConfig` and `ConfigValidationListener` and moved the DB, SMTP, Gemini and remember-me secrets into config. T0.3 gated seeding behind `DEV_SEED_TEST_USERS` and removed the stored-hash password fallback. Updated CODEBASE (§3, §8, §10, new §11), CONVENTIONS (D-08) and the example properties | User: rotate secrets, create `app.properties`, delete seeded admin rows, run runtime checks, then tick T0.1–T0.4. Next code task: T1.1 |
| 2026-11-27 | Phase 1 | Closed Phase 0 in the docs. Created `revamp/fix/f_t1_authorization` from `origin/revamp/d_dev` (17f4bde). T1.1 `Access`/`AccessRules`/`AuthFilter` + `web.xml` filter order; T1.2 owner-scoped document mutations; T1.3 `DocumentAccess` + friendship check; T1.4 chat scoping; T1.5 friendship state machine in SQL; T1.6 folder-id ownership (also `FolderController`); T1.7 `Validation` + admin input checks; T1.8 sweep and the rewritten CODEBASE §5. Added `docs/remediation/verify/offline/` (229 checks). New findings F-74..F-76, new tasks T4.7 and T8.3, decisions D-09..D-13 | User: run the curl/click-through checks (see *Blocked*), answer D-12 / F-76, approve the PR. Next: T2.1 |
| 2026-11-27 | Phase 1 review | Independent review of the branch by a separate Opus agent: no authorization bypass found; harness re-run (229 then, 230 now). Four fixes made from it: (1) the admin error banner fragment was read as ISO-8859-1 (garbled Vietnamese; confirmed with Tomcat's Jasper) -> `pageEncoding` + a `*.jspf` property group in `web.xml`; (2) admin delete from `document_view.jsp` no longer removed the file -> `AdminReportController.adminDeleteDocument` deletes it after the DB delete via `Utils.StoredFiles`; (3) the new email regex rejected addresses registration accepts (`o'neil@x.com`, `dev@localhost`) -> made deliberately loose; (4) `FileExplore.jsp` bookmark JS now checks `response.ok`. Three doc inaccuracies corrected. Left as the reviewer noted: `createFriendship`'s `INSERT ... SELECT ... WHERE NOT EXISTS` is valid T-SQL by inspection but only ran on H2 | User: runtime checks (see *Blocked*), D-12 / F-76 answers, PR approval |
