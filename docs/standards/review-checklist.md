# AIStudyHub Review Checklist & Anti-Patterns

This is the quality bar for AIStudyHub code.
Use Part 1 as the last pass before opening a pull request, and as the first pass when reviewing one.
Use Part 2 when a submission *sounds* right and you cannot say why it isn't.
Use Part 3 to write the pull request description.

Sections are numbered (§01–§10). Other documents in this repo cite rules as "checklist §NN".

### Applying the checklist to this stack

AIStudyHub is Java 8 servlets + JSP + SQL Server on Tomcat, with some vanilla JavaScript in the pages. Some items are
written in JavaScript terms. Read them as follows:

| Item says | In this codebase |
|---|---|
| `??` not `||`; `0`, `''`, `false` survive | Don't treat `0`/empty as "missing" in Java (`Integer` null vs `0`), in EL (`empty` is true for `0`-length strings), or in JS |
| `Promise.all` / unbounded concurrency | Thread pools, loops of outbound HTTP calls, the chatbot's repeated Gemini calls |
| Middleware or guard | A servlet `Filter` registered in `web.xml` |
| Store action / component | Server-side service or DAO method / JSP page |
| Spread or `Object.assign` from request data | Copying request parameters into a DTO and saving it without a field whitelist |
| Raw-markup sink | `<%= %>`, un-escaped `${}`, `innerHTML`, `marked` output |
| Queue / message / consumer | `ScheduledExecutorService` jobs (e.g. the subscription scheduler) and any future background work |

---

## Part 1 — The one-page checklist

### §01 Interface state

- [ ] `loading` starts `true`; cleared in `finally`; the previous error cleared before each retry
- [ ] Loading / error / empty / data are **one exclusive chain**; error is a real branch, not only a toast
- [ ] A failed fetch never renders the empty state
- [ ] "No results for this filter" is distinct from "nothing exists yet", each with its own action
- [ ] Refetch does not blank existing data; mount and watcher do not double-fetch
- [ ] Mutating actions: re-entry guard, disabled control, visible reason when invalid; requirements shown before submit
- [ ] Dialog reference data fetched on open, reset between rows, failure closes the dialog
- [ ] Keystroke-driven calls debounced in the shared input with a stated value; filter change resets to page 1
- [ ] Request count on load and per interaction is known and stated; no request per row; stale responses ignored
- [ ] Application-wide data guarded by both "already fetched" and "currently fetching"
- [ ] Remembered client state validated against the server **and** parsed defensively with a clean fallback
- [ ] Derived values are derivations — no second copy kept in sync by hand; totals and counts are computed
- [ ] Invariants live in the store action, not only in the calling component; odd inputs have documented outcomes
- [ ] Lists keyed by a stable id, never the array index; identity is the id, not a display field
- [ ] **A newer request always wins** — requests aborted, keyed or sequenced
- [ ] `0`, `''` and `false` survive to the screen; `??` not `||`; numbers locale-formatted
- [ ] Client state classified route-scoped / session-scoped / preference; route-scoped state reset on navigation; sign-out clears everything
- [ ] View state and in-progress work are addressable by URL; URL values validated on read
- [ ] Every error has a fallback message; nothing technical on screen; a failed submit keeps the input
- [ ] Async actions handle six states — **timeout is not failure**
- [ ] Waiting: backoff, ceiling, **elapsed-time** deadline, explicit timeout outcome, cancellation on unmount
- [ ] Role-gated controls from server-supplied permissions; the server also rejects; client caps duplicated server-side

### §02 Failure handling

- [ ] No stack trace, query text, host, path, upstream payload or exception class in any response
- [ ] Expected failures: specific status, **specific actionable** message, logged at `warn`
- [ ] Unexpected failures: generic message, 500, error object logged, correlation id available
- [ ] One error envelope everywhere with a stable machine-readable `code`
- [ ] Out-of-band results declared pessimistically and narrowed to success
- [ ] Status codes match their meaning; **401 vs 403 correct**; 403 vs 404 choice deliberate
- [ ] The **status code** carries the outcome — no `200` with an error body
- [ ] Async: terminal failures recorded with a reason; unknown failures rethrown; no commented-out rethrow
- [ ] The waiting client always receives a terminal result — nothing stuck pending
- [ ] Batch mode chosen deliberately; empty input rejected; failure list itemised and capped
- [ ] Missing configuration fails closed; unknown values not coerced to `0`; `??` not `||`
- [ ] Receivers re-validate inbound messages, stored files and webhook bodies
- [ ] One error convention per layer; no ignorable error flag
- [ ] No empty catches, no dropped error objects, no unchecked `undefined` from a failed call

### §03 Observability

- [ ] `start`, a success `end`, and a **distinct** error end, per unit of work
- [ ] A correlation id on every line, bound once, honoured from inbound, echoed in the response, threaded across processes
- [ ] Lower layers receive a logger; they do not import the global one
- [ ] Every 4xx logs the rule, the submitted value, the limit, and the limit's source
- [ ] Permission denials log who, what resource, and which check failed
- [ ] Boundary crossings logged with their result; internal pure steps not
- [ ] **Structured fields with constant messages** — no string-built log messages
- [ ] **Counts, ids and decisions — never payloads, entities, bodies or file contents**; no per-row logging
- [ ] Level from configuration; expected rejections at `warn`; each exception logged once, with the object
- [ ] No secret, token, signed URL, connection string or credential anywhere; every line names what it prints
- [ ] Audit is a **table** with a closed schema plus `metadata`, written from shared constants
- [ ] Audit record created before the work, defaults to FAILED, promoted on success
- [ ] Audit write is last, isolated, switchable, and logs its own failure **with the error**
- [ ] Nothing counted or alerted on from a sampled log line

### §04 Data access & performance

- [ ] Every parameter: presence, type, integrality, range, **cap**, whitelist, length, format
- [ ] Cheap validation before permission checks before data access
- [ ] Queries parameterised; pattern metacharacters escaped; filterable fields are a whitelist
- [ ] Projection at the query; **allow-list** mapper at the boundary; correlates of hidden fields stripped
- [ ] Pagination: store-side paging, matching count filter, full envelope, deterministic sort with a tiebreaker
- [ ] `null` sort position defined; page clamped to the available range; filter change resets to page 1
- [ ] Money in integer minor units or an exact decimal type — never a float; currency stored with the amount
- [ ] Identifiers are strings; units named in field names; non-fractional quantities are integers
- [ ] No 1 + N — batched `IN`, prefetch and reuse, short-circuit on empty access sets, no per-row auth check
- [ ] Independent I/O parallelised; **no unbounded fan-out**; bulk operations for bulk writes
- [ ] Pool created once and reused; pool config from environment; connections released in `finally`
- [ ] Indexes match filter + sort shape; before/after plans captured; the count query is indexable
- [ ] Cache versus state store stated explicitly; a miss must not lose data
- [ ] Caches: justified, expiry from config, invalidated on named transitions, permission-scoped, **hit rate measured**
- [ ] Cache keys from one module, prefixed, owner-scoped, every entry with an expiry
- [ ] Response size bounded; streaming where size scales with data

### §05 Security & access

- [ ] Identity from a verified session or token only; user id derived, never accepted
- [ ] Auth enforced by a filter or guard, default-deny; every route's protection is stated
- [ ] The tenancy hierarchy is written down; roles attach to memberships, not to users
- [ ] **One** permission module — no forked copies
- [ ] Resolution order explicit, broadest first, deny as the fall-through, reason returned
- [ ] Role check **and** per-resource check; **scope embedded in the query**
- [ ] Stateful flows check "exists for this caller" **and** "is this the expected next operation"
- [ ] Escape hatches checked last, fail closed, server-side, batched
- [ ] Every request field classified asserted / claimed / derived; derived fields overwritten
- [ ] One `now` per request; deadlines server-enforced; client timezone validated
- [ ] **No bulk copy from request data into a persisted entity**
- [ ] Nothing shipped that may not be seen; exposure decided from server state
- [ ] Receivers re-validate data **and** authorization
- [ ] Quotas validated against the effective merged configuration; sentinels handled everywhere
- [ ] Issued credentials: single-object scope, minimal permissions, short expiry, never logged
- [ ] Uploads: server-side type, size and shape checks; server-chosen sanitised path
- [ ] Secrets only in configuration; absence fails closed; ignore rules defined **once, at the root**
- [ ] Passwords adaptively hashed; auth endpoints rate limited; failures indistinguishable
- [ ] Tokens: expiry, pinned algorithm, rotated revocable refresh, `httpOnly` cookie, real sign-out
- [ ] Untrusted content rendered as **text**; any raw-markup sink sanitised by a named library on a whitelist
- [ ] Redirect targets are validated paths; outbound URLs scheme-whitelisted; new-tab links carry `rel="noopener"`
- [ ] One validation rule set shared by both sides; server-only rules still produce field-level messages
- [ ] Normalisation stated once and applied on both sides; boundaries tested, not assumed

### §10 Accessibility & resilient UI

- [ ] Everything clickable is a `<button>` or a link; click-handler `<div>`s converted
- [ ] Every control has an accessible name identifying **which** item it acts on; icon-only controls named
- [ ] Form controls have associated labels; meaningful images have alternative text
- [ ] State never conveyed by colour alone; the current item carries a machine-readable state
- [ ] Visible focus indicator; tab order follows reading order; fixed chrome does not obscure focus
- [ ] Overlays: focus moves in, is trapped, Escape closes, focus returns to the trigger
- [ ] A keyboard equivalent exists for drag, hover-reveal and swipe; **the whole flow works mouse-free**
- [ ] Exactly one main region; headings descend without skipping; lists and tables semantic
- [ ] Layout survives long strings, large numbers, empty sections, oversized collections, missing assets
- [ ] No horizontal page scroll at the narrowest supported width; overflowing containers scroll themselves
- [ ] Presentation values from tokens; no hardcoded colours in feature pages; shared chrome defined once
- [ ] Reduced motion respected; nothing essential conveyed only by a disappearing message

### §06 Asynchronous work

- [ ] Sync versus async justified per operation
- [ ] Entry point does auth, validation, authorization, a cheap pre-check — and no business write
- [ ] `202` plus a pollable handle, with `Retry-After` while non-terminal
- [ ] One **random** correlation id across log, message, result, audit row and status URL
- [ ] The result record is created **before** the response is returned
- [ ] Destination names from one shared constant, asserted at startup
- [ ] One pessimistic result envelope for the whole product
- [ ] The handler is idempotent under redelivery, and the technique is named
- [ ] The result is written before follow-on work; follow-on work is isolated
- [ ] Control messages recognised first and side-effect free
- [ ] Ordering per entity key only where a race exists, with the same key at every producer
- [ ] Progress on a bounded cadence; `status` / `startedAt` / `expectedBy` recorded **and read**
- [ ] Delivery cap and dead-letter destination configured, with a way to inspect and requeue
- [ ] Client deadline plus stale-job sweeper, so nothing waits forever
- [ ] Decision logic pure and testable without network or clock

### §07 Concurrency & versioning

- [ ] Every uniqueness rule backed by a **named constraint**; the pre-check is only for the message
- [ ] Derived ids where the key is immutable, random elsewhere; trade-off stated
- [ ] The store's duplicate-key error translated in one place; everything else rethrown
- [ ] The data layer does not erase the difference between "rejected" and "failed"
- [ ] Updates conditional on version or token, in a **single statement**; row count inspected
- [ ] Optimistic versus pessimistic chosen deliberately, with the reason
- [ ] Records read by users and edited by operators are versioned, not edited in place
- [ ] References point at the stable root id; every read filters on `isCurrent`
- [ ] Paired writes are transactional, or the compensating action is written **and implemented**
- [ ] Conflicts return **409** with a stable code, the current state, and a next action
- [ ] Optimistic interface state rolled back on every failure branch
- [ ] Lifecycle is a column; transitions are explicit; guards enforced server-side

### §08 External dependencies

- [ ] Fan-out capped by configuration; the cap is **total**, not per nested loop; trailing batch flushed
- [ ] Unbounded input chunked along a meaningful unit to a configured limit
- [ ] Every outbound call has a timeout; retries backed off, capped, jittered, idempotent-only
- [ ] Per-call cost captured, summed, attributed to a user, persisted; extraction cannot break the call
- [ ] Per-item failures collected, capped, phrased in the user's terms
- [ ] Dependency responses parsed in a dedicated layer, schema-validated, size-bounded, never passed through raw
- [ ] Request templates (including AI prompts) in versioned source
- [ ] Optional subsystems have a kill switch, default on, compound-guarded, one boolean parser
- [ ] Degradation strategy chosen in advance and visible in the response

### §09 Configuration & operability

- [ ] No environment-dependent literal in code; safe defaults documented; secrets undefaulted
- [ ] Values parsed once at the edge; client-exposed values visibly distinct; all documented in one place
- [ ] Configuration validated at startup; failure names the key
- [ ] Development conveniences opt-in, loud, fenced by a startup assertion
- [ ] The build artifact contains what runtime imports; production start command; non-root; healthcheck
- [ ] Migrations have a tested rollback; backfills have a dry run, counts and re-runnability
- [ ] Bulk deletes and updates are paged
- [ ] Config cascade: one implementation, deep merge, `0`/`false`/`''` preserved
- [ ] Build version visible; health endpoint checks dependencies; startup logs effective config; graceful shutdown
- [ ] Platform defaults — sampling, delivery limits, timeouts, cold starts — known and written down
- [ ] Tests run without network or clock; failure paths tested

---

## Part 2 — The anti-pattern gallery

### What *sounds* senior and is not

| Sounds senior | What it usually means | What to ask |
|---|---|---|
| "Added caching" | A map nobody measured | What is the hit rate? What happens with two replicas? What invalidates it? |
| "Made it asynchronous" | A queue in front of a 30 ms insert | What is the p99 of the work you moved? What was timing out? |
| "Made the handler idempotent" | Nothing; the word was in an article | What is the idempotency key, and what happens on the second delivery? |
| "Added correlation ids" | One id, in one place, in one log line | Show me the id on a line in **each** process |
| "Parallelised it for performance" | Unbounded concurrency | How many calls with the largest input a user can upload? |
| "Handled the error" | `catch (Exception e) { e.printStackTrace(); }` | What does the user see? Read me the sentence |
| "Used optimistic locking" | A `version` column nobody compares | Show me the `WHERE version = ?` and what happens when it matches zero rows |
| "Secured the endpoint" | A role check copied from the endpoint above | Who exactly can call it? What about a user who was banned a minute ago? |
| "Added an index" | An index on a column nothing filters by | Show me the plan before and after |
| "Validated the input" | A presence check | Where is the **cap**? |
| "Used a background thread / worker to process the messages" | A technology with no problem attached | What is the measured block you are removing? |
| "Used a message broker for the async pattern" | The transport, not the idea | Describe the same design with a database row and a status endpoint |
| "Used a distributed cache" | Infrastructure for a single-process service | How many replicas do you run? |
| "It's accessible" | Some `aria-` attributes were added | Do the exercise with the mouse unplugged. Show me |
| "Made it responsive" | Breakpoints exist | Render it with a 300-character title and a 10-digit number at the narrowest width |
| "Sanitised the input" | A regular expression that strips `<script>` | Which library, which whitelist, and does it run on output too? |
| "Shared the validation" | Two rule sets that already disagree | Which file defines the rule, and what fails when they diverge? |

The test in every row is the same: **ask what problem it solves in this codebase, and expect a number.**

### Defects that get past review

Each of these has shipped in real, reviewed production systems, written by someone who knew better and
was in a hurry. **Knowing what a rule looks like when it is broken is more useful than knowing the rule.**

| The defect | Why review missed it | Rule |
|---|---|---|
| The rethrow for unexpected failures **commented out** in a background consumer — every transient error silently discarded the user's work | Two characters, in a catch block, at the bottom of a long file | §02 — unknown failures rethrown |
| A correlation handle derived from `(queue name + current millisecond)` — two simultaneous requests shared a result record | It reads like a normal id derivation, and it works in testing | §06 — one **random** correlation id |
| A `delete(key)` immediately before the matching `get(key)` — a cache with a 0% hit rate | Nothing fails. Nothing logs. No test breaks. The only symptom is a latency number nobody watches | §04 — cache hit rate measured |
| An error-as-value wrapper whose flag was checked by **none** of its four callers — provider error text stored as content | Both halves are individually reasonable patterns | §08 — dependency responses parsed in a dedicated layer |
| A shared request helper returning the same "nothing" for both "no data" and "it failed" | It looks like tidy error handling. It makes the four-state rule impossible to implement | §02 — no unchecked "nothing" from a failed call |
| `loading` initialised `false`, no `catch`, and the empty state **commented out** — a permanently blank page on failure | The symptom (a flash of "no data") was silenced instead of diagnosed | §01 — loading/error/empty/data as one chain |
| A **forked** permission helper whose two copies disagreed on who may access a resource | Both files are correct in isolation; only the *pair* is wrong | §05 — one permission module |
| A "only published content may be used" guard implemented **only** in the page that renders the button | The feature demonstrably works; the endpoint was never tested directly | §01 — the server also rejects |
| A production credential committed, because one service's ignore list omitted a filename another's included | The rule was enforced per directory, and a directory was added | §05 — ignore rules defined once, at the root |
| A consumer that re-validated every field of its message and then trusted the message's identity field as proof of permission | The careful half is visible; the missing half is not | §05 — receivers re-validate data **and** authorization |
| A response logged in full while debugging — writing confidential content into the log store on every request | One line, added under pressure, never revisited | §03 — never payloads or bodies |
| Three boolean configuration switches parsed three different ways in one codebase | Each is correct where it was written | §09 — one boolean parser |

**The pattern across all of them**: none of them break a test, none of them log an error, and none of
them are visible in a feature demo. Production quality is not what runs — it is **what survives the day
it does not.**

---

## Part 3 — How to use this in a pull request

State, in the description, in a few lines:

1. **The request count** on load and per interaction, and why each one is needed.
2. **What happens when it fails** — which branch, what the user sees, what the log records.
3. **What happens when two people do it at once** — the constraint, the version check, or "this
   operation cannot conflict, because…".
4. **What is configurable** and what the defaults are.
5. **Any rule here you decided does not apply**, and why.

A stated, reasoned exception is a sign of understanding. Silence is not.
