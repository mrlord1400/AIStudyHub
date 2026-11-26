# Remediation Status

**Last updated:** 2026-11-26 by the audit session. Docs were created; no code has changed yet.
**Current phase:** 0 — Containment (not started)
**Next task:** T0.1 (needs the user to rotate secrets), then T0.2

> Update this file at the end of every session (see README → Session protocol). Keep it short. History goes
> in the *Session log*; detail goes in TASKS.md.

## Snapshot

| Phase | Done / total | State |
|---|---|---|
| 0 Containment | 0 / 4 | not started |
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
2. Every deployment has `admin@gmail.com` / `123` (F-02).
3. Uploading a `.jsp` gives remote code execution, and private files are downloadable by URL (F-05).
4. Any user can edit or delete any document, or read any chat (F-03, F-06).
5. Stored XSS on admin pages (F-07). Secrets are in the repo (F-08).

## In progress

_None._

## Blocked / waiting on the user

- **T0.1:** rotating the Gmail app password, Gemini key and DB password has to be done by the user.
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
| _next:_ `revamp/fix/f_t0_2_app_config` | T0.2 | not created yet |

## Session log

| Date | Agent/session | Did | Left for next |
|---|---|---|---|
| 2026-11-26 | audit | Audited the codebase against the review checklist. Wrote `docs/remediation/*` (README, STATUS, TASKS, FINDINGS, CONVENTIONS, CODEBASE, app.properties.example), `docs/standards/review-checklist.md` and the root `CLAUDE.md` pointer. Added `docs/BRANCHING.md`; created `revamp/d_dev` from `origin/main` and committed the docs there | Start T0.1/T0.2 on `revamp/fix/f_t0_2_app_config`. User to answer the open decisions |
