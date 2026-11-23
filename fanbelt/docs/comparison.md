# Orion and Fanbelt: a comparison

[Orion](https://orion-tour.pages.dev/) is a TypeScript toolbelt, and Fanbelt is the same technique on http4k. Orion's source isn't public, so everything said about it here comes from its tour and concept pages as of November 2026, including whether a feature is built or only planned.

## In short

Both rest on the same idea: a change writes to the store and publishes a topic that carries no data, and every live region re-reads, re-renders and streams its HTML to Datastar over SSE.

**Orion is a toolbelt for one developer shipping a SaaS.** It's small, has no dependencies, and is copied into your source tree. It's broad (auth, jobs, webhooks, idempotency, flags, circuit breakers, tracing, a dev overlay and code generators) and opinionated about deployment: one process and one SQLite file, then Cloudflare.

**Fanbelt is a library for teams on the JVM.** It's narrower, leaving auth and policy to the app and to http4k. It's stricter about the loop itself: typed outcomes, liveness worked out from what a view read, a tenant you can't get around, three test drivers, and multi-node on Postgres or Redis, built and tested. Orion catches mistakes at run time, in dev mode. Fanbelt makes most of them impossible to write, or catches them in a fast test.

## Size

Orion claims "about 600 lines" for its core, with no dependencies. Fanbelt's core is a few thousand, with each database or broker in a module of its own.

They don't measure the same thing. Orion's belt lists a router, an SSE writer, a store, a bus, auth, authorisation, jobs, idempotency, webhooks, flags, a breaker, tracing, health checks and tenancy, which would be hard to fit in 600 lines, so the figure probably counts only the loop. Fanbelt leans on http4k for routing, SSE, servers, templating and JSON, and still carries more: typed states and outcomes, read tracking, a bus across nodes that heals itself, shared presence, paging, jobs and failure events.

"No dependencies" is an argument about supply chains for a solo developer on npm. In a JVM shop, depending on http4k is the point: it's already the platform, with OAuth, resilience, OpenTelemetry and the servers.

## The programming model

| | Orion | Fanbelt |
|---|---|---|
| A unit of code | a feature slice: `sql`, `view`, `commands`, `routes` | a feature (states, rules, operations and jobs) over a domain class |
| Writes | `command(…)`: validate, change, then `bus.publish("todos")` by hand | `operation { … }`, ending in `Changes.of(state.topic(key))`, `Rejected`, `Denied` or `Hidden`; published only after commit |
| What a region watches | declared by hand: `region({ topics: ["todos"], … })` | worked out from what its state read, on every read |
| Skipping a redraw | the model is compared per stream; `shared: true` renders once for identical models | the model is read once per tenant and key, and compared by equality; viewers with equal views share one render |
| A missing publish | a dev-mode check re-renders on every skip and throws on a difference | the test tenant fails a step that changes a state without publishing its topic |
| Validation | `shape()` | value types validate themselves; `FieldName.check` |
| Who may act | `authz.can(…)` over cached string grants | a typed `rule`, read in the operation's transaction |
| View state | a page's signals | the URL; only cosmetic state stays in the browser |
| Presence | `ephemeral()` stores in memory or a database | presence states with leases, in memory, a database or Redis, shared across nodes |
| Templates | an `html` tagged template | Handlebars templates beside their view models |
| Wiring | about 20 lines of `main.ts` | `App(runtime, infra)` returns a `Node`; `main` picks the technology |

The biggest difference is **what you have to keep right by hand**. In Orion you name each region's topics, call `publish` in each command, and rely on dev-mode checks to catch you when you forget. In Fanbelt the topics come from the reads, the publish comes from the outcome, the tenant can't be reached except through a `Scope`, and the test kit fails a missing publish. The cost is more concepts to learn before the first page works.

## Storage

| | Orion | Fanbelt |
|---|---|---|
| Model | plain SQL over a small synchronous `Store`; no ORM | JSON documents behind typed tables, with filters, ordering and paging; your own SQL tables alongside |
| Databases | SQLite; SQLite in Durable Objects on Cloudflare | in memory, Postgres |
| Postgres | "a documented redesign", because the render path is synchronous | built in |
| Schema | migrations per feature | your migrations; the library never creates schema |
| Tenancy | a database per tenant, or a shared one with generated `tenant_id` filters and an escape hatch | one shared database with a `tenant` column, and no way to reach another tenant's rows |

Orion's synchronous render path is a deliberate choice: every read is a local SQLite call. Fanbelt gets the same simplicity another way. Virtual threads make blocking JDBC reads cheap, so its render path is synchronous code over Postgres too. That's why Postgres is a module in Fanbelt and a redesign in Orion.

Documents are Fanbelt's weakest fit for existing company data. They're filtered by JSON field, with no joins or aggregates beyond `count`. An app over an existing relational schema will mostly use its own SQL tables, which works but means two ways of storing data.

## What each ships

| Area | Orion | Fanbelt |
|---|---|---|
| Authentication | sessions, passwords, cookies, origin checks, a rate limiter; generated sign-up and login | carries the http4k principal into pages, streams and presence; identity itself is http4k's |
| Authorisation | grants with wildcards, cached and watchable | rules shared by operations, pages and routes; an open page is sent away when its rule stops allowing the viewer |
| Background work | `jobs()`: an outbox, claims, backoff, a dead letter | a `job` per key, recorded by the operation that names it, locked to one node and retried every second; giving up is the app's |
| Webhooks, idempotency | `webhook(…)`, `idem.once(…)` | a recipe: input from another system |
| Flags, circuit breakers | built in | not in the library: http4k's resilience filters, or a state |
| Observability | tracing, a canonical log line, health checks, a live admin dashboard | typed failure and recovery events, and marble diagrams of each change; health checks are the app's |
| Dev experience | live reload, an error overlay, a region highlighter | none; tests are the feedback loop |
| Scaffolding | `orion new`, generators, a linter | none; the examples share one layout to copy |
| UI patterns | toasts, modals, comboboxes, inline editing, presence avatars | none; each example writes its own |
| Planned, not built | durable workflows, billing, product analytics | not in scope |

Orion is far broader, and most of that breadth serves a solo SaaS: sign-up, billing, analytics, webhooks. Fanbelt leaves those out on purpose. In a company, identity comes from the company's identity provider, flags from its flag service, and observability from its OpenTelemetry, so a framework with its own password table would be in the way.

One gap is real for everyone: **dev mode**. Live reload and an in-page error overlay shorten the loop more than anything else Orion has, and Fanbelt has nothing like them.

## Testing

| | Orion | Fanbelt |
|---|---|---|
| What's shown | "237 tests"; a test app that resets shared state; "open two windows" | one set of scenarios per example, run through the features, an emulated browser and, optionally, real Chromium |
| Determinism | not described | a test runtime with its own scheduler, clock and randomness, and outbound HTTP that fails until a fake is wired in |
| Several nodes | not described | the same scenarios with users on two nodes, including a node that dies |

Testing is where Fanbelt is furthest ahead, and it's what lets it need fewer run-time checks. Orion's dev-mode check finds a region that reads outside its model while you click around. Fanbelt's tests find a missing publish on every build, with no HTTP.

## Scale

Orion's tour describes two paths:

1. **Node:** one process and one SQLite file, then replicated with Litestream, then a bus adapter for several machines. The adapters are **planned**: the bus today is in process.
2. **Cloudflare:** a Durable Object per tenant, behind a stateless Worker.

Most of the tour's strongest claims (isolation by design, fan-out with no pub/sub) come from the second path. They hold because each tenant is one single-threaded object holding its own database and every open stream. That's a property of Cloudflare, not of the technique.

An app over company data (an existing database, an identity provider, a private network, a load balancer) can't take that path. Its fair comparison is **Orion on one node against Fanbelt on several**:

| | Orion, on Node | Fanbelt |
|---|---|---|
| Nodes | one; more are planned | many, behind any load balancer, with no sticky sessions |
| Fan-out across nodes | planned | Postgres `LISTEN/NOTIFY` or Redis pub/sub |
| Lost messages | can't happen on one node | everything re-reads on reconnect, and every five minutes |
| Load | not described | a cap on concurrent re-reads per node, with an event when it's reached |
| Ceiling | one SQLite writer | the database's primary; route by tenant at the load balancer past a few nodes |

On one machine, Orion's single SQLite writer is very fast and simpler to run than anything here. Past one machine, or when the data must live in the company's database, Fanbelt's path is built and tested, and Orion's is planned.

## Which to choose

**Orion** suits a solo developer or a very small team building a new SaaS from scratch, who owns the database, wants sign-up, billing and webhooks in the box, and is happy on one machine or on Cloudflare. Its dev loop is better, its surface smaller, and you can read the whole belt in an afternoon.

**Fanbelt** suits a team on the JVM building apps over company data and infrastructure, who already have an identity provider, a database, flags and telemetry, need several nodes, and want correctness in types and tests rather than dev-mode checks. It costs more concepts up front, has no scaffolding or live reload, and its document storage is awkward for existing schemas.

## What Fanbelt could take from Orion

1. **A dev mode:** live reload (http4k's hot reload, from a test `main`) and an error overlay. The cheapest win, and missing entirely.
2. **Backoff and dead letters for jobs:** a failed job retries every second, and giving up is each app's to build.

Orion's dev-mode check isn't worth porting: a Fanbelt view is given only its model and the request, so reading outside the model is mostly impossible already. The grant engine, passwords, flags, breakers and billing are better left to http4k and the company's own services; adding them would turn a library into a platform.
