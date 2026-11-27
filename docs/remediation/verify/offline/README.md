# Offline verification harness

A stopgap until Phase 7 (JUnit, T7.1-T7.3). It runs the **real** controllers, filters and DAOs from `src/java`
without Tomcat or SQL Server, so a remediation task can be checked on any machine with a JDK 9+ and `curl`.

```bash
bash docs/remediation/verify/offline/run.sh          # everything
bash docs/remediation/verify/offline/run.sh T18Test  # one check
```

It downloads the servlet API, H2, javax.mail and javax.activation jars into `$AISH_LIBS`
(default `$TMPDIR/aish-libs`), compiles `src/java` with `--release 8` into a temp directory, then runs each check.
Nothing is written into the repo.

## What is simulated

| Piece | How |
|---|---|
| Servlet container | `Http.java`: proxy `HttpServletRequest` / `Response` / `Session`. It supports parameters, session attributes (`Http.sharedSession` keeps one across calls), one multipart file (`Http.part`), `sendRedirect`, `sendError`, `setStatus`, forwards (recorded, not executed), the writer and the output stream |
| Database | in-memory **H2 in `MODE=MSSQLServer`** with a simplified copy of `ASH_Database.sql` (`Db.java`). The real DAO classes run against it. `DB_URL`/`DB_USER`/`DB_PASSWORD` are passed as system properties, which `AppConfig` already honours |
| Filters | `FilterHarness.java` drives `Filter.AuthFilter` directly with a dispatcher type of `FORWARD` |

## What it does **not** prove

- Real SQL Server behaviour: H2 accepts some SQL that T-SQL does not and the reverse (for example the `INSERT ... SELECT ... WHERE NOT EXISTS` in `FriendshipDAO.createFriendship`).
- Tomcat behaviour: filter mapping and dispatcher handling in `web.xml`, `getServletPath()` on a real FORWARD, welcome-file handling, JSP compilation and rendering.
- Anything in the browser (JS, CSS, redirects as the user sees them).

So a task is only ticked `[x]` after its curl **Verify** steps have been run against a real instance (Conv §11).

## Checks

| Class | Covers |
|---|---|
| `FilterHarness` | T1.1: `AuthFilter` outcome for anonymous / student / admin over public, admin, guest and default-deny paths |
| `T12Test` | T1.2: owner-scoped DAO methods |
| `T13Test` | T1.3: `DocumentAccess.canView` / `canEdit` matrix (guest, stranger, friend in both directions, pending, blocked, flagged, admin) |
| `T14Test` | T1.4: chat DAO scoping, `FolderDAO.isOwnedBy` |
| `T15Test` | T1.5: friendship state machine in SQL |
| `T17Test` | T1.7: `AdminController`, `TransactionController`, `ReportConfigController`, `SystemConfigController` validation, self-protection, all-or-nothing config write |
| `T2Test` | T2.1-T2.3: `FileStorage`/`FileTypes` rules, the real `UploadController` (multipart stub) through upload/confirm/keepBoth/replace/cancel, view/download headers, every delete path, `StorageMigration` dry run / apply / re-run on a fake legacy tree. Needs `AISH_VERBOSE=1` to see the (expected) PDFBox warnings for the tiny fake PDF |
| `T18Test` | T1.2-T1.6 and T1.8 end to end through `DocumentController`, `ReportController`, `ChatBotController`, `SessionController`, `FriendController`, `FolderController`, `UserController`, `AdminReportController` |

To add a check: drop a class with a `main` here that uses `Db.init()`, `Db.check(...)`, `Http.call(...)` and ends with `Db.done()`, then add its name to `TESTS` in `run.sh`.
Keep each check meaningful: run it against the previous commit's sources and confirm that it **fails** there.
