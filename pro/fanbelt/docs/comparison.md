# Orion (TypeScript) and Fanbelt: a comparison

The original is the TypeScript toolbelt described at https://orion-tour.pages.dev/ ("the original"). This repository is the same technique on http4k ("this one"). The original's source isn't public, so everything said about it comes from the tour and its concept pages as of November 2026, including whether a feature is built or planned. Where the tour says "planned", this document does too.

## In one paragraph

Both are the same idea: a change writes to the store and publishes a topic that carries no data, and every live region re-reads and re-renders, streaming HTML to Datastar over SSE. The original is a **toolbelt for one developer shipping a SaaS**. It is small, has zero dependencies and is vendored into your tree. It is broad (auth, jobs, webhooks, idempotency, flags, breakers, tracing, a dev overlay and generators) and opinionated about deployment: one process, one SQLite file, then Cloudflare. This one is a **library for teams on the JVM**. It is narrower, leaving auth, background work and policy to the app and to http4k. It is deeper and stricter in the loop itself: typed outcomes, liveness worked out from what a view read, a tenant you can't get around, three test drivers, and several nodes on Postgres or Redis that are built and tested. The original catches mistakes at run time in dev mode. This one makes most of them unrepresentable, or catches them in a millisecond test.

## Size

| | Original | This one |
|---|---|---|
| Language | TypeScript on Node (also Deno, Bun), no build step | Kotlin 2.3 on the JVM |
| Core | "about 600 lines", zero dependencies (as claimed; not verifiable) | `core` 2,194 lines |
| Everything else | patterns (UI) and recipes, copied in by generators | `exposed` 471, `testing` 545, `redis` 186, `sqlite` 84, `postgres` 78, `jooq` 75, `load` 603 |
| Library tests | "237 tests" in the prototype | 196 test methods in the library modules |
| Examples | one example app (todos, with admin, reports, billing, webhooks and journal named but not shown) | seven: todo 317, sheets 434, boxoffice 664, board 733, exchange 853, helpdesk 1,246, sl4k 1,614 lines of main code, with 195 test methods, most run once per driver |
| Docs | a tour of 20 sections plus concept pages | guide (878 lines), reference (988), glossary, presentation |
| Dependencies | none in the belt; recipes add theirs to your `package.json` | http4k (core, Datastar, Moshi, Handlebars), values4k, tx4k; adapters bring Exposed, jOOQ, Lettuce, JDBC drivers |

The 600-line claim and the size of this core aren't measuring the same thing. The belt's list (router, SSE writer, store, bus, auth, authz, jobs, idempotency, webhooks, flags, breaker, trace, health, stalls, canon, shape, tenancy) would be hard to fit in 600 lines, so it probably counts only the loop. This core leans on http4k for routing, SSE, servers, templating and JSON, and still comes to 2,200 lines, because it carries things the belt doesn't: typed states and outcomes, read tracking, a cross-node backplane with healing, shared presence with leases, paging, and failure events.

"Zero dependencies" is an argument about the supply chain for a solo developer on npm. On the JVM in a company, depending on http4k is the point, not a cost: it's already the platform, and it already has OAuth, resilience, OpenTelemetry and the servers.

## The programming model

| | Original | This one |
|---|---|---|
| A unit of code | a feature slice: `sql`, `view`, `commands`, `routes` | a feature (states and operations) over a domain class, with `store/`, `view/` and `web/` beside it |
| Writes | `command(async (ctx, respond) => …)`: validate, mutate, `bus.publish("todos", …)` by hand | `operation { domain, actor, input -> }` ending in `Changes.of(state.topic(key))`, `Rejected` or `Denied`; published only after commit |
| What a region watches | declared by hand: `region({ topics: ["todos"], watch: [presence, authz], model }, render)` | worked out from what its state read, again on every read; nobody keeps a topic list |
| Skipping a redraw | the model is serialized per stream and compared; `shared: true` renders once for identical models | the model is shared per tenant and key by every connection on a node, and compared by equality; the view runs per viewer, and viewers with equal views share one render |
| A missing or wrong publish | dev-mode **region oracle** re-renders on every skip and throws on a difference | `TestTenant.read` fails if a state's value changed without its topics being published |
| Validation | `shape()`, Standard-Schema-compatible, `{ ok, value, issues }` | value types own their validation; `FieldName.check`, `zip`, `orElse` |
| Who may act | `authz.can(store, subject, action, resource)` over cached string grants | a `rule` per feature, typed, in the operation's transaction: `Allowed(found)`, `Hidden`, `Denied`, `Unavailable` |
| View state | a page's signals | the URL; only cosmetic state stays in `_` signals |
| One-shot reads | `fragment()` | a plain handler that reads a state once and renders it |
| Presence | `ephemeral()` stores: `memory()`, `table()`, `database()` | `presence(…)` states with leases, carrying an optional value, on memory, JDBC or Redis, shared across nodes |
| Templating | an `html` tagged template | Handlebars templates beside their view models |
| Wiring | about 20 lines of `main.ts`, no DI | `App(runtime, infra)` returns a `Node`; `main` picks the technology |

The two differ most in **what the developer has to keep right by hand**. In the original you name a region's topics, call `publish` in each command, put every per-viewer value into the model, and rely on the dev oracle and a heuristic `tenantGuard` to catch it when you forget. In this one the topics come from the reads, the publish comes from the outcome type, the tenant can't be reached except through a `Scope`, and the test kit fails a missing publish. The cost is more concepts to learn before the first page works. The reference's concept budget has eleven, against roughly six in the original (`command`, `region`, `view`, `fragment`, `resource`, the `store` and `bus` seams).

## Storage

| | Original | This one |
|---|---|---|
| Model | plain SQL over a five-method synchronous `Store`; named query functions per feature; no ORM | JSON documents (`tenant, id, seq, doc`) behind typed `Table`s, with `page` and `count`; a SQL hatch, `tables.exposed { }` and `tables.jooq { }` for your own tables |
| Databases | SQLite (WAL, single writer, optional group commit); DO SQLite on Cloudflare | in memory, SQLite, Postgres |
| Postgres | "not a drop-in", "a documented redesign", because the render path is synchronous | built in: per-tenant write lock, `REPEATABLE READ READ ONLY` reads, row-level security on `fanbelt.tenant` |
| Schema | per-feature migrations; a migration that touches another feature's table throws at boot | the library never creates schema; your migrations do |
| Tenancy | db-per-tenant (a file or a DO each), or shared-db with generated `tenant_id` predicates; `withoutTenant(…)` as the escape hatch | one shared database with a `tenant` column; `Tenants.tables(tenant)` is the only way in; `Tenants.having(…)` is the one cross-tenant read |

The original's synchronous render path is a real design choice, not an accident. Every read is a local SQLite call with no `await`, which is why it can say that "a stale render has one main suspect". This one gets the same simplicity a different way: JVM virtual threads make blocking JDBC reads cheap, so the render path is synchronous code over Postgres too. That's the reason Postgres is a module here and a redesign there.

The document model is this one's weakest fit for existing company data. Rows are JSON, filtered by JSON field, with one sort field and no ranges (see Rough edges in the reference). An app over an existing relational schema will live in the SQL hatch or in `TenantTable`. That works, but it means two ways of storing data.

## Breadth: what each ships

| Area | Original | This one |
|---|---|---|
| Authentication | belt: session tokens hashed with SHA-256, scrypt passwords, cookies, `checkOrigin`, a rate limiter; slice (generated): users, sessions, login, signup | `Authenticated` carries a principal into page, stream and presence; identity itself is http4k (OAuth/OIDC, tokens, your cookie); recAppIipes for origin checks, membership and masquerade |
| Authorization | `authz` grants with wildcards, cached, invalidated, watchable; `live({ authorize, revoked })` | rules shared by operations, regions and routes; revocation through a gate region (sl4k) |
| Background work | `jobs()`: outbox INSERT, atomic claim, backoff, dead letter | not in the library: an operation records intent as a row, the app's worker acts as `Actor.System` (helpdesk, boxoffice) |
| Durable workflows | planned | not in scope |
| Webhooks, idempotency | `webhook({ verify, handle })`, `idem.once(key, …)` | recipe: input from another system (the board's CI endpoint) |
| Flags, breakers | `flags()` with rollouts and kill switches; `breaker()` with a registry; both watchable | not in the library; http4k's resilience filters, or a state |
| Entitlements, billing | planned (Stripe to capabilities) | not in scope |
| Observability | `trace()`, canonical log line, `health()`, stall attribution, a live admin dashboard | typed failure and recovery events on `AppRuntime.events` (`RedrawFailed`, `BackplaneDisconnected`, `RedrawWaited`, …); health checks are the app's |
| Product analytics, journal | planned | an append-only journal table (helpdesk) |
| Dev experience | live reload, error overlay, stall banners, region highlighter and debug panel | none in the library; tests are the feedback loop |
| Scaffolding | `orion new`, `orion gen feature/pattern/recipe`, `orion routes`, `orion lint` | none; the examples share one layout to copy from |
| UI patterns | vendored toasts, modals, comboboxes, inline edit, presence avatars | none; each example writes its own |

The original is far broader. Most of that breadth is the SaaS-for-one list: signup, billing, entitlements, analytics, webhooks. This one deliberately leaves those out ("no background work, auth or policy: those are your app's, wired with ordinary http4k"). For a company app that's mostly right. Identity is the company's IdP, flags are the company's flag service, and observability is the company's OpenTelemetry. A framework that brought its own scrypt password table and `_orion_flags` table would be in the way.

Two gaps are real whoever the user is:

- **Dev mode.** Live reload and an in-page error overlay shorten the loop more than anything else in the original, and this one has no equivalent.
- **Jobs.** Every example that does background work rewrites the same sweeper around its own outbox, claiming and retry. Jobs were taken out of the library on purpose (ticket 113): owning queue machinery had cost a production bug and a scale ticket. But the sweeper is the one thing in the original's belt that this one's examples keep rebuilding.

## Testing

| | Original | This one |
|---|---|---|
| What the tour shows | "237 tests"; `testApp()` resets shared state; the demo is "open two windows" | one contract per example, run against a domain driver (`TestTenant`), an emulated Datastar browser and, opt-in, real Chromium through Playwright |
| Determinism | not described | `TestRuntime`: deterministic scheduler, clock and random, recorded events, outbound HTTP that answers 503 until a fake is wired |
| Several nodes | not described | `InMemoryCluster`: the same contract with users on two nodes, including a node that dies |
| Load | not described | `load` module: fan-out, hot tenant (about 12,600 accepted reservations a second in memory, no double bookings), presence churn, memory |
| Swarm | none | the exchange is checked by a Playwright bot swarm trading through the real UI |

Testing is where this one is furthest ahead, and it's what allows the smaller set of runtime guards. The original's oracle finds a region that reads outside its model while you click around in dev. This one's contract finds a missing publish on every build, with no HTTP.

## Scale, and whether it's comparable

The tour's scaling story has two paths:

1. **Node.** One process, one SQLite file, systemd (rung 0), then Litestream (rung 1), then a `Bus` adapter for several machines (rung 2). Rung 2's Redis and NATS adapters are **planned**: the bus today is an in-process `Map`.
2. **Cloudflare.** One Durable Object per tenant, a Directory DO as the gate, and a stateless Worker in front.

Most of the tour's strongest claims are about the second path: tenant isolation by architecture, refusals before any data object is touched, fan-out with no pub/sub, about 10 GB per tenant. They depend on each tenant being one single-threaded object that holds its own database and every open stream. That's a Cloudflare property, not a property of the technique.

For a company whose apps sit on internal data (an existing Postgres or SQL Server, an IdP, a VPC, a load balancer), that path isn't available. Path 1 then means one node and one SQLite file the app owns, with multi-node not yet built and Postgres described as a redesign. So for that reader the honest comparison is **the original on one node against this one on N nodes**.

| | Original, Node | This one |
|---|---|---|
| Nodes | one; a bus adapter is planned | many, behind any load balancer, no sticky sessions |
| Fan-out across nodes | planned | Postgres `LISTEN/NOTIFY` or Redis pub/sub; a channel per tenant on Redis |
| Lost messages | n/a on one node | everything re-reads on reconnect; everything heals every five minutes |
| Back-pressure | not described | `RedrawLimit` caps concurrent redraw reads per node and reports `RedrawWaited` |
| Ceiling | one SQLite writer (the tour cites a third-party "100,000 TPS" for SQLite, not an Orion benchmark) | the database primary, for writes and redraws; tenant routing at the load balancer past a few nodes |
| Operational notes | systemd, journald, Litestream | pooler pitfalls for `LISTEN`, proxy idle timeouts against the 15 s heartbeat, HTTP/2 for many tabs, hidden pages closing their streams |

On one box, the original's single SQLite writer with group commit is very fast, and simpler to run than anything here. Past one box, or when the data has to live in the company's database, this one has a path that's built and tested, and the original's is planned.

## Where each is the better choice

**The original** fits a solo developer or a very small team building a new multi-tenant SaaS from scratch. It suits someone who owns the database, wants signup, billing and webhooks in the box, and is happy on one node or on Cloudflare. Its dev loop is better, its surface is smaller, and you can read the belt in an afternoon.

**This one** fits a team on the JVM building apps over company data and infrastructure. They already have an IdP, a database, a flag service and telemetry, need several nodes behind a load balancer, and want correctness pushed into types and tests rather than dev-mode guards. It costs more concepts up front and has no tooling to scaffold or reload, and the document storage is awkward for existing schemas until you reach for the SQL hatch.

## What this one could take from the original

In order of value to a company team:

1. **A dev mode:** live reload (http4k's hot reload, from a test main) and an error overlay. These are the cheapest wins and are missing entirely.
2. **A sweeper:** the part of each example's worker that finds tenants with work and reports failures. Claiming and retries stay the app's operations, as ticket 113 decided.

The region oracle isn't worth porting. A view here is given only its model (and the request), so a view that reads outside its model is mostly prevented by construction.

What it should leave alone: the grant engine (see the authorization discussion: rules already do it, typed and reactive), password auth, flags, breakers and billing. These are better served by http4k and the company's existing services, and adding them would turn a library into a platform.
