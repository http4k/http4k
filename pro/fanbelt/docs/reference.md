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
- **The `Tables` you're handed is bound to one tenant.** Feature code never sees a tenant id.
- A `Table` has `page`, `count`, `get`, `first(where, order)`, `insert`, `update` and `delete`.
- **A write may run more than once,** so make no external calls inside one. A state's read is read-only; writing from it fails.

### Reading: `page`

```kotlin
tables.replies.page(where = Reply::ticket eq id).items
tables.journal.page(where = Fact::actor eq agent, order = Newest, after = before, limit = 10)
tables.tickets.count((Ticket::assignee eq agent) and (Ticket::status neq Resolved))
```

- `page(where, order, after, limit)` returns `Rows(items, next)`. With a `limit`, `next` is a `Cursor` when there's more.
- **Filters:** `eq`, `neq`, `oneOf` and `contains` (case-insensitive substring), combined with `and`. They work only on fields stored as JSON strings (ids, value types, enums), or null. Reach inside a composite key or nested class with `/`: `(Booking::id / ShowSeat::show) eq show`.
- **Order:** `Oldest` (the default), `Newest`, or `ascending(Field)` / `descending(Field)`. A field order takes a `limit` but has no cursor: it's for "the top N".
- **Cursors** are safe to put in a URL (`agents/{id}?before={cursor}`). Put the cursor in the region's key.
- **Across tenants,** `storage.read { it.having(table, where) }` returns the tenants with matching rows.
- **Gotcha:** every filtered field needs an index in your migrations. See [Production](#production).

## The domain class

Your queries and changes over the tables, built per transaction:

```kotlin
class Tickets(private val tables: HelpdeskTables, private val random: Random) {
    fun reply(id: TicketId, agent: ViewerId, body: ReplyBody) {
        tables.replies.insert(Reply(ReplyId.random(random), id, agent, body))
    }
}
```

It never checks permissions, publishes, or names a state. Take `Random` and `Clock` from the constructor, never `UUID.randomUUID()` or `Clock.systemUTC()`.

## Features

A feature is the states, rules and operations of one area of the app, over its domain:

```kotlin
class TicketsFeature(random: Random) : Feature<Tickets, Actor>({ tables -> Tickets(HelpdeskTables(tables), random) }) {
    val queue by state { it.queue() }
    val conversation by state(TicketId) { desk, id ->
        val found = desk.ticket(id)
        if (found == null) null else Conversation(found.customer, desk.replies(id))
    }
    val viewing by presence(TicketId, ViewerId)

    val agentOnTicket by rule { ctx, actor, id: TicketId ->
        if (actor is Actor.Agent && ctx.domain.ticket(id) != null) Allowed(actor) else Denied
    }

    val reply by operation(agentOnTicket, Pair<TicketId, String>::first) { desk, _, (id, raw), agent ->
        val body = replyBody(raw).orElse { return@operation it }
        desk.reply(id, agent.id, body)
        Changes.of(conversation.topic(id))
    }
}
```

A feature whose domain needs nothing from `App` can be an `object`.

### States

A state is named after its property, and that name is its topic. A value of `null` means its subject doesn't exist.

- **Stored:** `by state { domain -> … }` or `by state(Keys) { domain, key -> … }`. `conversation.topic(id)` is its topic, for `Changes.of(…)`; with no key, `queue.topic()`. A stored state's key is a value type.
- **Presence:** `by presence(Keys, Members)` is who's here, read as a map (take `.keys`). `viewing.join(scope, key, who)` returns a lease; close it to leave. `by presence(Members)` has no key. Joining and leaving publish, so operations never do.
- **Presence carrying a value:** `by presence(Members, CellRef::class)`, read as `Map<Member, CellRef?>`. Set it with `here.set(scope, key, who, value)`, or bind it to a route: `here.set(workspace, actor) { cellRef(it) }`. The value goes when the member's last lease ends.
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

**Checking input.** `FieldName.check(Values, raw, message)` turns text into a value type or a `Rejected`; `check(Enum.entries, raw, message)` does the same for an enum. `zip` combines checks and keeps every error. `.orElse { return@operation it }` takes the value or ends the operation:

```kotlin
val report = FieldName.of("status").check(BuildStatus.entries, posted.status, "A build is passing, failing or running")
    .zip(FieldName.of("commit").check(Commit, posted.commit, "A commit is 7 to 40 hex characters")) { status, commit -> BuildReport(board, status, commit) }
    .orElse { return@operation it }
```

**Gotcha:** a race is not a permission. Something gone since the page rendered is `Unavailable` from a rule, which reaches the viewer as a `Rejected`. Only "not yours" is `Denied`.

### Rules

A rule decides who may do something. Operations, regions and routes share it. It answers `Allowed(found)`, `Hidden`, `Denied` or `Unavailable(field, message)`:

```kotlin
val visibleChannel by rule { ctx, viewer, name: ChannelName ->
    val channel = channelNamed(ctx, name)
    if (channel?.visibleTo(viewer.id) == true) Allowed(channel) else Hidden
}

val post by operation(visibleChannel, Draft::channel) { chat, member, draft, channel -> … }
```

- **An operation names its rule** and how to get the key from its input, or just the rule when the input is the key. The block gets what the rule found. `operation { … }` with no rule is open.
- **A rule with no key narrows the actor:** `val fan by rule { _, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }`, then `operation(fan) { boxOffice, _, pick: ShowSeat, fan -> … }`.
- **Rules build on rules** with `then`, which carries on from `Allowed`: `read(visibleChannel, Attempt(viewer, ref.channel)).then { channel -> … }`.
- **A rule is a state,** keyed on `Attempt(actor, key)`. A route can ask `scope.read(chat.visibleChannel, Attempt(actor, channel)) is Allowed`, or take what it found with `.orNull()`.
- **Refusals are reported** as `OperationRefused` on `events`, with the operation and the rule. Like every event raised inside a scope, it carries its tenant as metadata, read with `eventTenant(event)`, and says nothing about who acted.
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
- **Absent:** a `null` model is a value. Render a "gone" view.
- **Fallback:** if a read or view throws, the region shows `fallback` (an empty element by default) and reports `RedrawFailed`. The rest of the page carries on.
- **Gotcha:** models and view models must be data classes, or unchanged models are re-sent and renders aren't shared.

**Several sources make a derived state,** not a second kind of region:

```kotlin
val viewers by derived { ctx, id: BoardId -> ViewersModel(ctx.read(here, id).keys, ctx.read(roles, id)) }

val nav by derived { ctx, _: Unit -> NavModel(ctx.read(counts), ctx.read(agents).open, ctx.read(online).keys) }
```

- The block gets a `ctx` (a `StateContext`) and the key; a rule's gets `ctx`, the actor and the key.
- `ctx.read(state, key)` reads any state, and the region re-renders when any of them changes.
- `ctx.watch(topic)` adds a topic that isn't a state.
- `ctx.staleAt(instant)` re-reads at that moment, with nothing published.
- `ctx.domain` reads what isn't a state. Nothing tracks it, so pair it with a `ctx.read` or `ctx.watch`.
- A derived state that needs something only `App` has takes it through its feature's constructor, as `MarketFeature(clock, random)` does.
- View arguments (a filter, a page size, a cursor) go in the key; the derived state `watch`es a coarse topic.
- **Gotcha:** a filtered view must read something that changes when its *membership* does. Read the coarse state and filter it, or have the operation publish a topic for the set.

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

A `Layout` is what the page draws, and a `Viewing` is how each browser's connection runs; a page without one gets the defaults. Mount it with `"/t/{ticket}" bind ticketPage`. That serves the first paint and one stream for every region, on the same route.

```html
<base href="{{shell.base}}">
<body data-init="{{shell.connect}}" data-signals="{{shell.signals}}">
{{{shell.regions.queue}}}
```

- **`shell.signals`** comes from the page's signals class: each field with its default, plus an `errors` entry for each.
- **`subject`** names the region the page is about. If its state is absent on first paint, the page renders the absent view and answers 404.
- **`onConnect`** runs when the stream opens and returns an `AutoCloseable`, closed when it ends.
- **URLs are relative to the tenant root** (`t/{{id}}/assign`).
- **Reading a page never writes.** If arriving should change something, post an operation on load: `<body data-init="@post('b/{{board}}/join'); {{shell.connect}}">`.
- **Gotcha:** open the stream with `{{shell.connect}}`, never a hand-written `@get`, or the page goes dead after a deploy.

### One-shot reads

A panel or modal that needn't stay live is a plain handler that reads a state once and patches it in:

```kotlin
"/t/{ticket}/customer" bind GET to CatchLensFailure().then { request ->
    val card = workspace.scope(request).read(tickets.customer, ticketId(request))
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
- **The app owns its tenants.** Each gets a row of its own when created, `exists` is one `get` of it, and `having(table, everything())` lists them. `App` creates none.
- **Membership ("not yours") is app policy:** a filter inside the tenant's routes. See [Recipe: authentication](recipes.md#authentication).

## Wiring: `App`

```kotlin
fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra): Node {
    val runtime = runtime.withEvents(EventFilters.AddTimestamp(runtime.clock).then(EventFilters.AddEventName()).then(runtime.events))
    val infra = infrastructure(runtime)
    // features, workspace, pages, routes …
    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, workspace.routes(identified.then(oneOrg))))
    return Node(app, runtime, infra, jobs)
}
```

- **`AppRuntime(clock, random, events, scheduler, http)`** is everything with side effects. `http` is the outbound client every adapter wraps; `withEvents` filters the events. Tests pass a `TestRuntime`.
- **`AppInfra(runtime, storage, transport, presenceBackend, env)`**, each defaulting to one node in memory:

| Part | Choices |
|---|---|
| storage | `InMemoryDocumentStorage()`, `SqliteDocumentStorage(file, json)`, `ExposedDocumentStorage(db, json, PostgresDocuments)` |
| transport | none (one node), `PostgresTransport(connect, scheduler, events)`, `RedisTransport(uri, events)` |
| presence | `InMemoryPresenceBackend(clock)`, `ExposedPresenceBackend(db, clock, json)`, `RedisPresenceBackend(uri, clock, json)` |
| env | the `Environment` that `FanbeltSettings` are read from; empty by default |

- **`main` picks the technology;** `App` reads no configuration and creates no data.
- **Gotcha:** every part with background work (the infra, your workers) goes in the `Node`, which starts and stops them. A part left out fails on first use.

## Background work and time

Fanbelt runs no background work. Use any scheduler or queue, and connect it to the loop two ways:

- **Record the intent in the same operation,** as a row (an outbox). It exists only if the operation committed.
- **Record outcomes by executing operations** as the system, so pages update like any change. A worker that writes through a read fails.

**Time that passes is a read, not a job.** `ctx.staleAt(instant)` in a derived state re-reads at that moment, with nothing published: the box office's seat map says `ctx.staleAt(booking.expiresAt)`. In tests, `tenant.tick(…)` moves the clock.

## Testing

Write scenarios once as a contract, against a driver interface of verbs (`adds`, `watches`, `sees`) that returns a `Verdict` (`Accepted`, `Rejected(reasons)`, `Denied`, `Hidden`). Make one user act and *another*, already watching, see. Wrap checks on other people's screens in `eventually { }`. The test kit is the `testing` module.

**Domain driver: `TestTenant()`.** One in-memory tenant on a deterministic `TestRuntime`; build features from `tenant.random` and `tenant.runtime.clock`.
- `execute(operation, actor, input)` returns a `Verdict`; `read(state[, key])`, `join(presence, key, who)`, `set(presence, key, who, value)` and `tick(duration)`.
- `tenant.events` records events, so a test can check a failure path.
- **Gotcha:** `read` fails if a state's value changed without its own topics being published. It can only catch that for a state read before, so watch first.

**UI driver: `browser(app, scheduler, cookie)`.** A headless Datastar browser over the real `App(TestRuntime(scheduler))`, one per user. `verdict(errorSelector)` reads the last action's result, and `close()` closes the tab.
- **Gotcha:** a control the page hides must still be proven refused: `node.provesServerRefuses(path, cookie, signals)` posts it and counts only a 403 as `Denied`.

**Real browser: `RealBrowser(uri)`,** opt-in with `FANBELT_TEST_BROWSER=1`. `open(path, cookie)` gives each user a context; `page.verdict`, `page.navigates`, `page.posting` and `page.provesServerRefuses` mirror the emulator.
- **Gotcha:** to move time, pass a `MovableClock` to the `App`, plus a `MovableScheduler(clock)` for anything that uses `staleAt`.

**Several nodes: `InMemoryCluster`.** Build each app with `App(cluster.runtime, cluster.node())`. `cluster.nodes[1].dies()` kills a node.

Pass fakes for external services to `App`, so the UI driver stays deterministic.

**Marble traces.** Every change also emits a trace on `events`: `Executed(operation, actor, outcome)`, `Published(topic)`, `Redrawn(region, key, changed)` and `Patched(region, actor, sent)`, all `FanbeltTrace`, each tagged with its tenant (`eventTenant(event)`). Register `@RegisterExtension val marbles = MarbleEvents()` and pass it as the app's events (`runtime.withEvents(marbles)`); after each test it draws the traces as text and HTML into `build/reports/marbles`. Give the page's `Viewing(asFan)` the routes' actor function to name lanes by person, `marbles.alias(key, name)` to name keys, and `marbles.record { … }` to draw only those steps. Approve one with `approver.assertApproved(AsciiMarbles().render(name, marbles.timeline()).content)`:

```
ann reserve @nyc             -✓--------------------------------------
bob reserve @nyc             -----------------------✗----------------
bookings:Midnight Owls @nyc  ---●------------------------------------
#seats(Midnight Owls) @nyc   ---------●------------------------------
#basket(ann) @nyc            -------------●--------------------------
#basket(bob) @nyc            -----------------○----------------------
ann #seats @nyc              -----------●----------------------------
bob #seats @nyc              ---------------------●------------------
```

`✓` changes, `✗` rejected, `⊘` denied or hidden, `●` happened, `○` nothing changed.

## Production

- **Schema:** your migrations create one document table per `TableName`, `PresenceTable()` (`fanbelt_presence`) if presence is in Postgres, and an expression index for every field you filter on: `CREATE INDEX ON journal ((doc ->> 'ticket'))` on Postgres, `CREATE INDEX journal_ticket ON journal (json_extract(doc, '$.ticket'))` on SQLite. Without one, every redraw scans the table.
- **Several nodes:** point every node at the same storage and the same transport. No sticky sessions.
- **Postgres behind a pooler:** `PostgresTransport` needs a direct or session-mode connection. A transaction-mode pooler (the default URL on Neon, Supabase, Heroku and others) silently delivers nothing.
- **Proxies:** every stream is pinged every `FANBELT_STREAM_KEEPALIVE` (default 2 s), which keeps it open through a proxy and notices a closed tab within about two intervals. Keep idle timeouts above 15 seconds, don't buffer `text/event-stream`, and serve browsers HTTP/2.
- **Redraws:** keep `FANBELT_REDRAW_LIMIT` (default 8) below your connection pool's size. `RedrawWaited` events show a node at its limit.
- **Past a few nodes:** use Redis for transport and presence, route requests by tenant at the load balancer, and don't send redraws to read replicas.
- **Health:** wrap the app in `ReportFailures(events)`, with no inner `CatchAll`. Failures and recoveries (`BackplaneDisconnected`/`BackplaneReconnected`, `PresenceRenewalFailed`/`PresenceRecovered`) arrive on `events`, for an http4k `ReadinessCheck`.
- **Deploys:** pages reconnect by themselves. Changing a stored row beyond adding a defaulted field is a migration: see [Recipe: evolving stored rows](recipes.md#evolving-stored-rows).

## Limitations

- `page()` has no ranges and sorts by one field. Beyond that, filter a bounded set in Kotlin, or use [your own SQL tables](recipes.md#your-own-sql-tables).
- Behind a path-rewriting proxy, `Workspace`'s default lookup needs the prefix added.
- Over HTTP/1.1, a browser stalls after about six open tabs of one app.
