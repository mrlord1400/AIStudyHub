# AIStudyHub

Java 8 Servlet/JSP app on Tomcat + SQL Server, built with NetBeans Ant. Context path `/AIStudyHub`. UI text is Vietnamese.

## Ongoing remediation (multi-session)

A security/quality remediation is in progress against `docs/standards/review-checklist.md`.
**Before doing any work, read `docs/remediation/README.md`.** It explains the session protocol and points to:

- `docs/remediation/STATUS.md`: current phase, in-progress work, blockers, open decisions (read first)
- `docs/remediation/TASKS.md`: phased task list with verify steps
- `docs/remediation/CONVENTIONS.md`: the required way to implement fixes
- `docs/remediation/CODEBASE.md`: architecture map (use it instead of re-reading the whole codebase)
- `docs/remediation/FINDINGS.md`: audit findings `F-xx` with file:line evidence

Update `STATUS.md` and `TASKS.md` before ending a session.

## Hard rules

- **Branching: follow `docs/BRANCHING.md`.** `main` is frozen, so never commit, merge or push to it. Base all work on
  `revamp/d_dev`; use `revamp/fix/f_<name>` for fixes (remediation tasks: `f_<task_id>_<slug>`) and
  `revamp/feature/d_<name>` for features; PRs target `revamp/d_dev`. Don't merge into `revamp/d_dev` without the user's approval.
- Never commit secrets. Config goes through `Utils.AppConfig` once T0.2 lands; until then, don't add new literals.
- Don't commit machine-specific `nbproject/*` changes unless the task asks for it.
- Filters live in `src/java/Filter/` (package `Filter`).
