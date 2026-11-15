# Fanbelt: reference

What to call, with what, and the gotcha that bites. The [guide](guide.md) teaches the concepts in order; the [recipes](recipes.md) show common patterns. The worked example is the **helpdesk** (`examples/helpdesk`).

```
browser action ──▶ POST ──▶ scope.execute(operation) { domain writes; returns Changes.of(states) }
                                        │ commit, then publish
                                        ▼
browser ◀── SSE patch ◀── region re-renders ◀── bus: "topic changed" (on every node)
```

## Concepts

| You write | What it is | Library type |
|---|---|---|
| Values | A value type for every domain string or id | values4k `StringValue`/`UUIDValue` |
| Rows | Data classes stored as JSON documents, keyed by `id` | `Identified<ID>` |
| Tables object | Typed handles to the app's tables | `Tables`, `Table<ID, T>`, `TableName` |
| Domain class | The app's logic over its stored data, built per transaction | — |
| Feature | **States** (named, keyed reads), **operations** and **rules** | `Feature`, `State`, `Operation` |
| Regions | Live parts of a page: an id, a key, a state and a view | `Region` |
| Pages | A route serving HTML plus one SSE stream for its regions | `Page` |
| Routes | POST routes binding signals or the request to an operation | `operation(workspace, actor, input)` |
| `App` | The function that wires everything and returns a `Node` | `AppRuntime`, `AppInfra`, `Node` |

## Values and rows

```kotlin
class TicketId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<TicketId>(::TicketId)
}

data class Ticket(override val id: TicketId, val subject: Subject, val status: Status, val assignee: ViewerId?) : Identified<TicketId>
```

Register every value type stored in a row with one JSON object, and pass it to database storage: `object HelpdeskJson : ConfigurableMoshi(standardConfig().withFanbeltMappings().value(TicketId)….done())`. A missing one only fails at runtime.

## Tables

```kotlin
class HelpdeskTables(tables: Tables) {
    val tickets = tables<TicketId, Ticket>(TICKETS)

    companion object {
        val TICKETS = TableName.of("tickets")
    }
}
```

- **Your migrations create the tables:** `PostgresDocuments.schema(name)` or `SqliteDocuments.schema(name)` for each `TableName`. Fanbelt never creates schema.
- **The `Tables` you're handed is bound to one tenant.** Code can't choose another; a rule or derived state can read which one it is (`ctx.tenant`), to name it to a service outside the app.
- A `Table` has `page`, `count`, `get`, `first(where, order)`, `insert`, `update` and `delete`.
- **A write may run more than once,** so make no external calls with side effects inside one. A state's read is read-only; writing from it fails.

### Reading: `page`

```kotlin
tables.replies.page(where = Reply::ticket eq id).items
tables.journal.page(where = Fact::actor eq agent, order = Newest, after = before, limit = 10)
tables.tickets.count((Ticket::assignee eq agent) and (Ticket::status neq Resolved))
```

- `page(where, order, after, limit)` returns `Rows(items, next)`. With a `limit`, `next` is a `Cursor` when there's more.
- **Filters:** `eq`, `neq`, `oneOf` and `contains` (case-insensitive substring), combined with `and`. They work only on fields stored as JSON strings (ids, value types, enums), or null. Reach inside a composite key or nested class with `/`: `(Booking::id / ShowSeat::show) eq show`.
- **Order:** `Oldest` (the default), `Newest`, or `ascending(Field)` / `descending(Field)`. A field order takes a `limit` but has no cursor: it's for "the top N".
- **Cursors** are safe to put in a URL (`agents/{id}?before={cursor}`).
- **Gotcha:** every filtered field needs an index in your migrations. See [Production](#production).

## The domain class

Your queries and changes over the tables, built per transaction ([guide step 3](guide.md#step-3-the-domain-class)). It never checks permissions, publishes, or names a state, and takes `Random` and `Clock` from its constructor.

## Features

A feature is the states, rules and operations of one area of the app, over its domain:

```kotlin
class TicketsFeature(random: Random) : Feature<Tickets, Actor>({ tables -> Tickets(HelpdeskTables(tables), random) }) {
    val queue by state { it.queue() }
    val conversation by state { desk, id: TicketId ->
        val found = desk.ticket(id)
        if (found == null) null else Conversation(found.customer, desk.replies(id))
    }
    val viewing by presence(TicketId, ViewerId)

    val agentOnTicket by rule { ctx, actor, id: TicketId ->
        if (actor is Actor.Agent && ctx.domain.ticket(id) != null) Allowed(actor) else Denied
    }

    val reply by operation(agentOnTicket, Pair<TicketId, String>::first) { desk, actor, (id, raw), agent ->
        val body = replyBody(raw).orElse { return@operation it }
        desk.reply(id, agent.id, body)
        Changes.of(conversation.topic(id))
    }
}
```

A feature whose domain needs nothing from `App` can be an `object`.

### States

A state is named after its property, and that name is its topic. A value of `null` means its subject doesn't exist.

- **Stored:** `by state { domain -> … }` or `by state { domain, key: Key -> … }`. `conversation.topic(id)` is its topic, for `Changes.of(…)`; with no key, `queue.topic()`. A stored state's key is a value type.
- **Presence:** `by presence(Keys, Members)` is who's here, read as a map (take `.keys`). `viewing.join(scope, key, who)` returns a lease; close it to leave. `by presence(Members)` has no key. Joining and leaving publish, so operations never do.
- **Presence carrying a value:** `by presence(Members, carrying = CellRef)`, or per key `presence(Keys, Members, CellRef)`, where `CellRef` is a value type, read as `Map<Member, CellRef?>`. Set it with `here.set(scope, key, who, value)`, or bind it to a route: `here.set(workspace, actor) { cellRef(it) }`. The value goes when the member's last lease ends. See [Recipe: per-viewer, short-lived state](recipes.md#per-viewer-short-lived-state).
- **Derived:** `by derived { ctx, key -> … ctx.read(a, key) … }` reads other states. See [Regions](#regions).

### Operations

```kotlin
val reply by operation(rule, Input::key) { domain, actor, input: Input, found -> … }
```

An operation runs in a write transaction, one at a time per tenant, and ends in one of four outcomes:

- **`Changes`:** accepted. `Changes.of(topic)`, `Changes.of(setOf(a, b), result)` or `Changes.none()`. Topics publish only after commit. The optional `result` goes back to the caller.
- **`Rejected(field, message)`:** the input or current state won't do. It is shown on that field, to this viewer only.
- **`Denied`:** not allowed, a 403.
- **`Hidden`:** as far as you know, it doesn't exist, a 404. Only a rule answers it.

A refusal rolls back everything written before it, and nothing publishes.

**Checking input.** `FieldName.check(Values, raw, message)` (or `check<SomeEnum>`) turns input into a value or a `Rejected`; `zip` keeps every error, and `.orElse { return@operation it }` ends the operation. Keep typed text in a String signal: a typed signal that doesn't parse is a 400, not a field error.

```kotlin
val report = FieldName.of("status").check<BuildStatus>(posted.status, "A build is passing, failing or running")
    .zip(FieldName.of("commit").check(Commit, posted.commit, "A commit is 7 to 40 hex characters")) { status, commit -> BuildReport(board, status, commit) }
    .orElse { return@operation it }
```

**Gotcha:** a race is not a permission. Something gone since the page rendered is `Rejected` by a rule, with a reason the viewer sees. Only "not yours" is `Denied`.

### Rules

A rule decides who may do something. Operations, regions and routes share it. It answers `Allowed(found)`, or refuses with the same outcome an operation would: `Hidden`, `Denied` or `Rejected(field, message)`:

```kotlin
val visibleChannel by rule { ctx, viewer, name: ChannelName ->
    val channel = channelNamed(ctx, name)
    if (channel?.visibleTo(viewer.id) == true) Allowed(channel) else Hidden
}

val post by operation(visibleChannel, Draft::channel) { chat, member, draft, channel -> … }
```

- **An operation names its rule** and how to get the key from its input, or just the rule when the input is the key. The block gets what the rule found. `operation { … }` with no rule is open.
- **A rule with no key narrows the actor:** `val fan by rule { ctx, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }`, then `operation(fan) { boxOffice, _, pick: ShowSeat, fan -> … }`.
- **Rules build on rules** with `flatMap`, which carries on from `Allowed` and may refuse: `read(visibleChannel, ActorKey(viewer, ref.channel)).flatMap { channel -> … }`. `map` changes what was found without refusing.
- **A rule is a state,** keyed on `ActorKey(actor, key)`, so other rules and states can read it.
- **Pages and routes name rules too:** `Viewing(viewer, rule)` guards a page, and a route that reads state binds a rule as its handler, `chat.visibleChannel(workspace, { ActorKey(principal(it), channelName(it)) }) { channel, request -> … }`, where a refusal answers 403 (404 for `Hidden`) and a key that doesn't parse answers 400. See [Recipe: who may see a page](recipes.md#who-may-see-a-page).
- **Refusals are reported** as `OperationRefused` on `events`, with the operation and the rule.
- **Gotcha:** a rule an operation uses can't read presence. It fails rather than seeing nobody.

### Driving it directly

`scope.execute(tickets.reply, Actor.Agent(sam), ticket to "Hello")` returns the operation's `Outcome` once it has committed and published. `scope.read(tickets.queue)` and `scope.read(tickets.conversation, id)` read states, and `scope.read(tickets) { desk -> … }` reads the domain directly. Background workers and webhooks work this way, with no HTTP.

### Several features

An app holds any number of features over the same storage, bus and presence. A feature is given the features it reads (`ActivityFeature(tickets)`), and dependencies run one way. A derived state can `read` and `watch` any feature's states in one transaction. **Gotcha:** an operation changes only its own feature's data. If two features need one atomic change, they're one feature.

## Regions

A region shows one state: a **key** from the request, the state's value as its **model**, and a **view**.

```kotlin
Region("conversation", renderer, ticketId, tickets.conversation) { conversation, _ ->
    ConversationView(conversation.replies.map { … })
}
```

- **Key:** a function of the request, such as a path lens. A state with no key takes none: `Region("todos", renderer, todo.list) { … }`. Key on the viewer only when the *read* differs per viewer; when only the look differs, do it in the view.
- **View:** `{ model, request -> … }`. It reads the model and the request, nothing else: no storage, no presence.
- **Template:** renders exactly one root element carrying the region's id.
- **Absent and failing:** a `null` model renders a "gone" view; a read or view that throws shows `fallback` and reports `RedrawFailed` ([guide step 16](guide.md#step-16-the-url-holds-view-state)).
- **Gotcha:** models and view models must be data classes, or unchanged models are re-sent and renders aren't shared.

**Several sources make a derived state** ([guide steps 14–15](guide.md#step-14-derived-states)). Its block gets a `ctx` (a `StateContext`) and the key; a rule's gets `ctx`, the actor and the key:

- `ctx.read(state, key)` reads any state, and the region re-renders when any of them changes.
- `ctx.watch(topic)` adds a topic that isn't a state.
- `ctx.staleAt(instant)` re-reads at that moment, with nothing published.
- `ctx.tenant` is the tenant being read, for asking a service outside the app (OpenFGA's `org:{tenant}`, say).
- `ctx.domain` reads what isn't a state. Nothing tracks it, so pair it with a `ctx.read` or `ctx.watch`.

## Pages

```kotlin
Page(
    workspace = workspace,
    layout = Layout(
        regions = listOf(nav, queue, header, conversation),
        renderer = renderer,
        subject = header,
        signals = ReplySignals(),
    ) { request, shell -> TicketPageView(shell, principal(request).name) },
    viewing = Viewing { scope, request -> tickets.viewing.join(scope, ticketId(request), principal(request).id) },
)
```

A `Layout` is what the page draws, and a `Viewing` is who is viewing it: `Viewing(viewer, rule, leaveTo, onConnect)` names the rule they must pass (for a rule with no key, `viewer` returns just the actor, as in `Viewing(asFan, boxOffice.fan)`), where a refused viewer is sent (`/` by default, on arrival or while watching) and what runs when their stream opens (`onConnect`, returning an `AutoCloseable` closed when it ends). A page without one is open to the tenant. Mount it with `"/t/{ticket}" bind ticketPage`. That serves the first paint and one stream for every region, on the same route.

```html
<base href="{{shell.base}}">
<body data-init="{{shell.connect}}" data-signals="{{shell.signals}}">
{{{shell.regions.queue}}}
```

- **`shell.signals`** comes from the page's signals class: each field with its default, plus an `errors` entry for each.
- **`subject`** names the region the page is about. If its state is absent on first paint, the page renders the absent view and answers 404.
- **URLs are relative to the tenant root** (`t/{{id}}/assign`).
- **Reading a page never writes.** If arriving should change something, post an operation on load: `<body data-init="@post('b/{{board}}/join'); {{shell.connect}}">`.
- **Gotcha:** open the stream with `{{shell.connect}}`, never a hand-written `@get`, or the page goes dead after a deploy.

### One-shot reads

A panel or modal that needn't stay live is a route that binds a rule, reads once and patches it in:

```kotlin
"/t/{ticket}/customer" bind GET to tickets.customerOf(workspace, { ActorKey(agent(it), ticketId(it)) }) { card, _ ->
    Response(OK).datastarElements(Element.of(renderer(viewOf(card))), morphMode = outer)
}
```

The page holds an empty placeholder (`<div id="customer-card"></div>`) outside its regions, and a close route patches it back. Later changes don't show until it's opened again.

## Routes

Invoke an operation with the workspace, the actor and where its input comes from:

```kotlin
private val agent = { request: Request -> Actor.Agent(principal(request).id) }

"/t/{ticket}/reply" bind POST to tickets.reply(
    workspace, agent, { signals: ReplySignals, request -> ticketId(request) to signals.reply },
    reset = setOf(ReplySignals::reply),
),
"/t/{ticket}/assign" bind POST to tickets.take(workspace, agent, input = ticketId),
```

- **With signals,** the input function gets the parsed signals class and the request; **without,** just the request.
- **A feature with no actors** (`Feature<Domain, Unit>`) binds without one: `todo.toggle(workspace, input = todoId)`.

| Outcome | Answer |
|---|---|
| `Changes` | Resets the `reset` signals and clears the page's errors. The result goes to the `Feedback`. |
| `Rejected` | Sets the `errors` signal for that field, to this viewer only. |
| `Denied` | 403 |
| `Hidden` | 404 |

The default `Feedback` is `ErrorSignals`; `GoTo` moves the browser. A route never publishes and never returns HTML. A hand-written route that reads a lens wraps itself in `CatchLensFailure()`.

Mount the tenant's routes and pages with `workspace.routes(…)` beside the app's own:

```kotlin
val oneOrg = poly(listOf(TicketRoutes(tickets, workspace)), "/t/{ticket}" bind ticketPage)
poly(home, workspace.routes(oneOrg))
```

## Tenancy

```kotlin
val workspace = Workspace(infra, "/orgs/{org}", orgs::exists)
```

- **The tenant is the template's one path variable.** For subdomain or token tenancy, pass both functions: `Workspace(infra, template, exists, tenant = { request -> … }, root = { tenant -> Uri })`.
- **`workspace.routes(…)` answers 404** for a tenant that `exists` rejects, for pages, routes and streams alike. `exists` runs on every request; cache it if tenants are never deleted.
- `workspace.scope(request)` is the tenant's `Scope`; `workspace.uri(request)` is its root.
- **The app owns its tenants;** `App` creates none ([guide step 20](guide.md#step-20-tenancy)).
- **Membership ("not yours") is a rule:** a page names it in its `Viewing`, and a route that reads state binds it. See [Recipe: who may see a page](recipes.md#who-may-see-a-page).

## Wiring: `App`

`fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra): Node`, wired in full in [guide step 22](guide.md#step-22-wiring-app-and-main).

- **`AppRuntime(clock, random, events, scheduler, http)`** is everything with side effects. `http` is the outbound client every adapter wraps; `withEvents` filters the events. Tests pass a `TestRuntime`.
- **`AppInfra(runtime, storage, transport, presenceBackend, env)`**, each defaulting to one node in memory:

| Part | Choices |
|---|---|
| storage | `InMemoryDocumentStorage()`, `SqliteDocumentStorage(file, json)`, `ExposedDocumentStorage(db, json, PostgresDocuments)` (or `PostgresDocuments(KeyLocking)`) |
| transport | none (one node), `PostgresTransport(connect, scheduler, events)`, `RedisTransport(uri, events)` |
| presence | `InMemoryPresenceBackend(clock)`, `ExposedPresenceBackend(db, clock)`, `RedisPresenceBackend(uri, clock)` |
| env | the `Environment` that `FanbeltSettings` are read from; empty by default |

- **`main` picks the technology;** `App` reads no configuration and creates no data.
- **Gotcha:** every part with background work (the infra, your workers) goes in the `Node`, which starts and stops them. A part left out fails on first use.
- **Background work is yours:** record the intent in an operation (an outbox) and the outcome by executing one as the system; time passing is `ctx.staleAt(instant)`, and `storage.read { it.having(table, where) }` finds the tenants with matching rows. See [guide step 21](guide.md#step-21-background-work-and-time).

## Testing

Write scenarios once as a contract, against a driver interface of verbs (`adds`, `watches`, `sees`) that returns a `Verdict` (`Accepted`, `Rejected(reasons)`, `Denied`, `Hidden`). Make one user act and *another*, already watching, see. Wrap checks on other people's screens in `eventually { }`. The test kit is the `testing` module.

**Domain driver: `TestTenant()`.** One in-memory tenant on a deterministic `TestRuntime`; build features from `tenant.random` and `tenant.runtime.clock`.
- `execute(operation, actor, input)` returns a `Verdict`; `read(state[, key])`, `join(presence, key, who)`, `set(presence, key, who, value)` and `tick(duration)`.
- `tenant.events` records events, so a test can check a failure path.
- **Gotcha:** `read` fails if a state's value changed without its own topics being published. It can only catch that for a state read before, so watch first.

**UI driver: `browser(app, scheduler, cookie)`.** A headless Datastar browser over the real `App(TestRuntime(scheduler))`, one per user. `verdict(errorSelector)` reads the last action's result, and `close()` closes the tab.
- **Gotcha:** a control the page hides must still be proven refused: `node.provesServerRefuses(path, cookie, signals)` posts it and counts only a 403 as `Denied`.

**Real browser: `RealBrowser(uri)`,** opt-in with `FANBELT_TEST_BROWSER=1`. `open(path, cookie)` gives each user a context; `page.verdict`, `page.navigates`, `page.posting` and `page.provesServerRefuses` mirror the emulator.

**Several nodes: `InMemoryCluster`.** Build each app with `App(cluster.runtime, cluster.node())`. `cluster.nodes[1].dies()` kills a node.

Pass fakes for external services to `App`, so the UI driver stays deterministic.

Marble traces of a test can be approved: see [Recipe: approving marble traces](recipes.md#approving-marble-traces).

## Production

- **Schema:** your migrations create every table and an index for each filtered field.
- **Several nodes:** every node uses the same storage and the same transport. No sticky sessions.
- **Health:** wrap the app in `ReportFailures(events)`, with no inner `CatchAll`.

The rest (indexes, poolers, proxies, locking, presence leases) is in [Recipe: production](recipes.md#production).

## Limitations

- `page()` has no ranges and sorts by one field. Beyond that, filter a bounded set in Kotlin, or use [your own SQL tables](recipes.md#your-own-sql-tables).
- Behind a path-rewriting proxy, `Workspace`'s default lookup needs the prefix added.
- Over HTTP/1.1, a browser stalls after about six open tabs of one app.
