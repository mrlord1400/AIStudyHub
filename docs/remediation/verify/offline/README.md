# Offline verification harness

A stopgap until Phase 7 (JUnit, T7.1-T7.3). It runs the **real** controllers, filters and DAOs from `src/java`
without Tomcat or SQL Server, so a remediation task can be checked on any machine with a JDK 9+ and `curl`.

```bash
bash docs/remediation/verify/offline/run.sh          # everything
bash docs/remediation/verify/offline/run.sh T18Test  # one check
```

It downloads the servlet API, H2, javax.mail, javax.activation and (for `T31Test`) the Tomcat-embed, ECJ and `javax.annotation` jars into `$AISH_LIBS`
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
- Full Tomcat behaviour. `T31Test` does run the real `web.xml` and compiles/renders the JSPs on embedded Tomcat 9, but the other checks still use the servlet stub, and no check covers a production Tomcat's own configuration (`context.xml`, connectors, the default servlet in front of `/uploads`).
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
| `T31Test` | T3.1 (25 pages with hostile data, owner/friend/guest/admin views, plus every JSP compiled by Jasper): `Web.java` starts the real `web.xml`, filters, controllers and JSPs (Jasper) in an **embedded Tomcat 9** on the same H2, logs in as an admin through `AuthController`, loads every admin page with hostile data (`"><img src=x onerror=alert(1)>`, `'</script><script>…`, `x" autofocus onfocus="…`) and fails on any raw payload; it also requires the escaped form to be present so the check cannot pass on an empty page. Also unit checks for `Utils.Html.esc`. Add a page here whenever you convert one |
| `T32Test` | T3.2 (39 checks): `CsrfFilter` + `csrf.js` + the token meta tag on the real chain: POST without/with a wrong/with another session's token -> 403 and nothing changes; with the token (field or header) it works; GET on mutating actions -> 405 for `deleteDoc`, `deleteFolder`, `logout`, `deleteSession`, `deleteFriendship`; AJAX rejections are JSON, chat is text; multipart; logout by POST; every JSP that posts includes `csrf_head.jspf`. Also `Utils.Csrf` unit checks |
| `T33Test` | T3.3: security headers on pages, redirects, errors and static files; session cookie HttpOnly + SameSite=Lax; `SessionCookieListener` Secure logic; JSESSIONID and CSRF token change at login; a banned account answers the same for a right and a wrong password |
| `T34Test` | T3.4: remember-me through raw requests: cookie flags and lifetime, only the hash stored, rotation, replay revokes, logout, password change, ban, expiry, tampering, garbage values, the old HMAC cookie format |
| `T35Test` | T3.5: `RateLimiter` on a fake clock (window, retry-after, key bound), login throttle per (IP, email) and per IP, OTP send cooldown/hourly cap, unknown email gets the same answer, per-code attempt counting (another browser cannot lock the victim out), no email-wide lock |
| `T18Test` | T1.2-T1.6 and T1.8 end to end through `DocumentController`, `ReportController`, `ChatBotController`, `SessionController`, `FriendController`, `FolderController`, `UserController`, `AdminReportController` |

`Web.java` keeps the cookie jar of the JDK client for page walks (`Web.login`, `Web.csrf`) and has `Web.raw(...)` for checks that need to see every `Set-Cookie` line and send exact `Cookie` headers. Tomcat's class loader keeps its own copies of the app classes, so state in `Utils.*` / controllers must be reached with `Web.invokeInApp(...)`.

To add a check: drop a class with a `main` here that uses `Db.init()`, `Db.check(...)`, `Http.call(...)` and ends with `Db.done()`, then add its name to `TESTS` in `run.sh`.
Keep each check meaningful: run it against the previous commit's sources and confirm that it **fails** there.
