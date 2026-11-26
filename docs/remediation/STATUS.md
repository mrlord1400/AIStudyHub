# Remediation Status

**Last updated:** 2026-11-26 by the Phase 0 session. T0.2–T0.4 are implemented on `revamp/fix/f_t0_containment` and compile; none is ticked
yet because the runtime checks need Tomcat + SQL Server + real keys, and T0.1 needs the user's rotation.
**Current phase:** 0 — Containment (code done, awaiting user actions and runtime verification)
**Next task:** user finishes T0.1 and runs the Phase 0 verify steps, then Phase 1 starting with T1.1 (T0.4 is already in place)

> Update this file at the end of every session (see README → Session protocol). Keep it short. History goes
> in the *Session log*; detail goes in TASKS.md.

## Snapshot

| Phase | Done / total | State |
|---|---|---|
| 0 Containment | 0 / 4 (4 implemented, awaiting verification) | in progress |
| 1 Authorization | 0 / 8 | not started |
| 2 File storage | 0 / 3 | not started |
| 3 Browser security | 0 / 5 (T3.1 has 4 parts) | not started |
| 4 Money & concurrency | 0 / 6 | not started |
| 5 Failure & observability | 0 / 6 | not started |
| 6 Data, config, ops | 0 / 8 | not started |
| 7 Tests | 0 / 3 | not started |
| 8 UI & accessibility | 0 / 2 | not started |

## Risk right now (what is still exploitable)

Every critical finding is still open. The worst, in order:
1. Anyone can create an admin or change any user via `/AdminController` (F-01).
2. ~~Every deployment has `admin@gmail.com` / `123` (F-02).~~ New starts no longer seed it, but **existing databases still hold those rows** until the user removes them (T0.3 step 4).
3. Uploading a `.jsp` gives remote code execution, and private files are downloadable by URL (F-05).
4. Any user can edit or delete any document, or read any chat (F-03, F-06).
5. Stored XSS on admin pages (F-07). Secrets are no longer in the source tree (F-08) but **remain in git history and are still live until rotated** (T0.1).

## In progress

- **T0.1–T0.4** on branch `revamp/fix/f_t0_containment` (one branch for the four tightly related Phase 0 tasks; one commit per task). Code is done and compiles
  (`javac --release 8` over `src/java`, with a downloaded `javax.servlet-api-3.1.0.jar` outside the repo). See the per-task notes in `TASKS.md` for what was and
  wasn't verified.

## Blocked / waiting on the user

- **T0.1:** rotating the Gmail app password, Gemini key and DB password has to be done by the user.
- **Before the app starts on this branch the user must create `%USERPROFILE%\.aistudyhub\app.properties`** (template: `docs/remediation/app.properties.example`)
  with the **new** credentials. Without it the app deploys, logs `Missing required config: <KEY>`, and DB/mail/Gemini/remember-me are unusable (fail closed).
- **T0.3 step 4:** remove or re-password `admin@gmail.com` and `user@gmail.com` in every existing database (SQL in `CODEBASE.md` §11).
- **Runtime verification** of T0.2–T0.4 with Tomcat and SQL Server (listed in `TASKS.md`).
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
| `revamp/fix/f_t0_containment` | T0.1–T0.4 (Phase 0) | implemented, in review; PR to `revamp/d_dev` not merged |

## Session log

| Date | Agent/session | Did | Left for next |
|---|---|---|---|
| 2026-11-26 | audit | Audited the codebase against the review checklist. Wrote `docs/remediation/*` (README, STATUS, TASKS, FINDINGS, CONVENTIONS, CODEBASE, app.properties.example), `docs/standards/review-checklist.md` and the root `CLAUDE.md` pointer. Added `docs/BRANCHING.md`; created `revamp/d_dev` from `origin/main` and committed the docs there | Start T0.1/T0.2 on `revamp/fix/f_t0_2_app_config`. User to answer the open decisions |
| 2026-11-26 | Phase 0 | Created `revamp/fix/f_t0_containment`. T0.4 renamed `FIlter`→`Filter`. T0.2 added `AppConfig` and `ConfigValidationListener` and moved the DB, SMTP, Gemini and remember-me secrets into config. T0.3 gated seeding behind `DEV_SEED_TEST_USERS` and removed the stored-hash password fallback. Updated CODEBASE (§3, §8, §10, new §11), CONVENTIONS (D-08) and the example properties | User: rotate secrets, create `app.properties`, delete seeded admin rows, run runtime checks, then tick T0.1–T0.4. Next code task: T1.1 |
