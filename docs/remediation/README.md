# AIStudyHub Remediation — Start Here

This folder is the hand-off kit for fixing AIStudyHub against the project review checklist
[`docs/standards/review-checklist.md`](../standards/review-checklist.md).
The work spans many sessions and many agents. **These documents replace re-reading the codebase.**

## Read order for a new session / agent

| # | File | What it gives you | When to read |
|---|---|---|---|
| 1 | [STATUS.md](./STATUS.md) | Where we are right now, what's in progress, blockers, open decisions | **Always, first** |
| 2 | [TASKS.md](./TASKS.md) | The full phased task list with acceptance criteria | Always — pick your task here |
| 3 | [CONVENTIONS.md](./CONVENTIONS.md) | The agreed *way* to fix things (auth helper, SQL scoping, escaping, logging…) | Before writing any code |
| 4 | [CODEBASE.md](./CODEBASE.md) | Architecture map: routing, controllers, DAOs, schema, session attributes, gotchas | When you need to find something |
| 5 | [FINDINGS.md](./FINDINGS.md) | The original audit: every defect with file:line evidence, by checklist section | When a task references `F-xx` |
| — | [app.properties.example](./app.properties.example) | Every config key the fixes introduce | When a task touches configuration |
| — | [verify/offline/](./verify/offline/README.md) | Offline check harness (`bash docs/remediation/verify/offline/run.sh`): real controllers/DAOs on in-memory H2, no Tomcat | After changing controllers, filters or DAOs; add a check per task until Phase 7 replaces it |
| 6 | [../standards/review-checklist.md](../standards/review-checklist.md) | The review checklist itself (rules referenced as "checklist §NN") | When you need the exact rule wording, and before opening a PR (use its Part 3) |

## Session protocol (every agent, every session)

0. Make sure you're on the right branch (see [`docs/BRANCHING.md`](../BRANCHING.md)): `git fetch origin`, then create
   `revamp/fix/f_<task_id>_<slug>` from `origin/revamp/d_dev`, or continue the existing branch listed in `STATUS.md`.
1. Read `STATUS.md` → note the current phase and anything marked **In progress** or **Blocked**.
2. Pick the **lowest-numbered unblocked task** in `TASKS.md` whose dependencies are done
   (unless the user assigns one). Mark it `[~]` in `TASKS.md` and add it under *In progress* in `STATUS.md`.
3. Re-read only the files named in the task. Line numbers in these docs were accurate on **2026-11-26**;
   they drift — search for the quoted code if a line number no longer matches.
4. Implement following `CONVENTIONS.md`. If you need to deviate, record why in the
   *Decisions log* in `CONVENTIONS.md`.
5. Verify using the task's **Verify** steps. Do not tick a task you have not verified; say what was skipped.
6. Before ending the session:
   - Tick the task `[x]` in `TASKS.md` (or leave `[~]` with a note of what remains).
   - Update `STATUS.md`: move the task, add a line to the *Session log*, record new blockers/decisions.
   - If you discovered a new defect, add it to `FINDINGS.md` (next free `F-` id) **and** a task to `TASKS.md`.
   - If code moved enough that `CODEBASE.md` is wrong, fix `CODEBASE.md`.

## Ground rules

- **Never commit secrets.** `src/java/Utils/GeminiService.java` currently holds a live key in the
  working copy — see T0.1.
- **Branching follows [`docs/BRANCHING.md`](../BRANCHING.md)**: work on `revamp/fix/f_<task_id>_<slug>` branched from
  `revamp/d_dev`, with the PR targeting `revamp/d_dev`. **Never touch `main`.** Commit messages are prefixed with the task id,
  e.g. `T1.2: scope document update/delete to owner`.
- Behaviour visible to users (Vietnamese UI text, page flow) should stay the same unless the task says otherwise.
- Prefer the smallest change that satisfies the acceptance criteria. This is a student project on
  NetBeans + Tomcat; do not introduce Maven/Spring/etc. without a recorded decision.
