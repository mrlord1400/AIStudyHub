# Branching Model

All work on the AIStudyHub revamp follows this model. It applies to people and AI agents alike.

```
main                          frozen. Do not commit, merge, rebase or push to it
revamp/
├── d_dev                     the integration branch, i.e. the new "main" for all current work
├── feature/d_<feature_name>  new functionality for the system
└── fix/f_<fix_name>          fixes, revamps, refactors (all remediation tasks)
```

## Rules

1. **`main` stays exactly as it is.** Never use it as a base, merge target or push target. Treat it as an archive
   of the pre-revamp system.
2. **`revamp/d_dev` is the base and the target.** Every `feature/` and `fix/` branch is created from the latest
   `revamp/d_dev` and merges back into `revamp/d_dev`. Pull requests target `revamp/d_dev`, **never `main`**.
3. **Don't commit directly to `revamp/d_dev`**, except for documentation-only changes to `docs/` or `CLAUDE.md`
   (e.g. updating `STATUS.md` after a merge). Code always goes through a `feature/` or `fix/` branch.
4. **Naming**: lowercase snake_case after the prefix.
   - Features: `revamp/feature/d_<feature_name>`, e.g. `revamp/feature/d_document_comments`
   - Fixes and remediation: `revamp/fix/f_<fix_name>`. For remediation tasks, start the name with the task id
     from `docs/remediation/TASKS.md` (dots become underscores), e.g.
     `revamp/fix/f_t0_2_app_config`, `revamp/fix/f_t1_1_auth_filter`, `revamp/fix/f_t3_1a_escape_admin_pages`.
5. **Scope**: one remediation task per `fix/` branch (or a few tightly related tasks; list them in the PR).
   Keep branches short-lived and rebase or merge `revamp/d_dev` into them if `d_dev` moves.
6. **Commits**: prefix the message with the task id when there is one, e.g. `T1.2: scope document update/delete to owner`.
   Never commit secrets or machine-specific `nbproject/*` changes.
7. **Merging into `revamp/d_dev`** happens through a pull request that uses the PR template in
   `docs/standards/review-checklist.md` (Part 3). Agents push their branch and open the PR when asked, but
   **don't merge into `revamp/d_dev` without the user's approval**.
8. **After a merge**, update `docs/remediation/STATUS.md` (and tick the task in `TASKS.md`) on `revamp/d_dev`.
   Delete the merged branch.

## Quick reference

```bash
# start a fix
git fetch origin
git switch -c revamp/fix/f_t1_1_auth_filter origin/revamp/d_dev

# keep it current
git fetch origin && git merge origin/revamp/d_dev     # or: git rebase origin/revamp/d_dev (before pushing)

# publish
git push -u origin revamp/fix/f_t1_1_auth_filter
gh pr create --base revamp/d_dev --title "T1.1: single auth gate" --body "..."
```

## Pre-revamp branches

The many older branches in the repo (`fix-folder-error`, `Long-BE`, `ai-*`, …) belong to the pre-revamp workflow.
Don't base new work on them. If something from one of them is still needed, bring it over through a
`revamp/fix/` or `revamp/feature/` branch.
