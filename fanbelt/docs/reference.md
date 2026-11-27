# Fanbelt: reference

What to call, with what, and the gotcha that bites. The [guide](guide.md) teaches the concepts in order; the [recipes](recipes.md) show common patterns. The snippets come from the **box office** (`examples/boxoffice`), the guide's app, and the **features** example (`examples/features`), which has one small feature per pattern.

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
| Feature | **States** (named, keyed reads), **operations**, **rules** and **jobs** | `Feature`, `State`, `Operation`, `Job` |
| Regions | Live parts of a page: an id, a key, a state and a view | `Region` |
| Pages | A route serving HTML plus one SSE stream for its regions | `Page` |
| Routes | POST routes binding signals or the request to an operation | `operation(workspace, actor, input)` |
| `App` | The function that wires everything and returns a `Node` | `AppRuntime`, `AppInfra`, `Node` |

## Values and rows

```kotlin
class OrderId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<OrderId>(::OrderId)
}

data class Order(override val id: OrderId, val show: ShowId, val fan: ViewerId, val seats: List<SeatRef>, val status: OrderStatus, val at: Instant) : Identified<OrderId>
```

An id is a value type, and two ids are the same row when their values are equal. `update` can't change a row's id: delete the row and insert another. An id made of parts (a seat at a show) is a value type built from them; store any part you filter on as a field of its own.

Register every value type stored in a row with one JSON object, and pass it to database storage: `object FeaturesJson : ConfigurableMoshi(standardConfig().withFanbeltMappings().value(RoomName)….done())`. A missing one only fails at runtime.

## Tables

```kotlin
class BoxOfficeTables(tables: Tables) {
    val orders = tables<OrderId, Order>(ORDERS)

    companion object {
        val ORDERS = TableName.of("orders")
    }
}
```

- **Your migrations create the tables:** `PostgresDocumentStorage.schema(name)` for each `TableName`. Fanbelt never creates schema.
- **The `Tables` you're handed is bound to one tenant.** Code can't choose another; a rule or derived state can read which one it is (`ctx.tenant`), to name it to a service outside the app.
- A `Table` has `page`, `count`, `get`, `first(where, order)`, `insert`, `update` and `delete`.
- **A write may run more than once,** so make no external calls with side effects inside one. A state's read is read-only; writing from it fails.

### Reading: `page`

```kotlin
tables.bookings.page(Booking::show eq show).items
tables.orders.page(Order::fan eq fan, Newest).items
notes.page(order = Newest, limit = shown.value + 1).items
lines.count(Line::room eq reader.room)
```

- `page(where, order, after, limit)` returns `Rows(items, next)`. With a `limit`, `next` is a `Cursor` when there's more.
- **Filters:** `eq`, `neq`, `oneOf` and `contains` (case-insensitive substring), combined with `and` and `or`. They work on strings, numbers, booleans, value types and null. Reach inside a nested class with `/`: `(Order::address / Address::city) eq city`.
- **Ranges:** `gt`, `lt` and `between(low, high)` (up to but not including `high`), on a field that sorts: `Order::at.between(monday, nextMonday)`. A missing or null field never matches.
- **Order:** `Oldest` (the default), `Newest`, or `ascending(field)` / `descending(field)`. A field order takes a `limit` but has no cursor: it's for "the top N".
- **How fields sort:** numbers and times as you'd expect, value types as what they wrap, and text by code point, so upper case comes before lower (`"Zebra" < "apple"`). A `ZonedDateTime` doesn't sort as a time, so store an `Instant` or `OffsetDateTime` instead.
- **Cursors** are safe to put in a URL (`notes?before={cursor}`). A cursor marks the next page, not "everything up to here": with `KeyLocking`, a row can commit after one written later, so fetching "everything since" a saved cursor can miss rows.
- **Gotchas:** a field is named by its Kotlin property, so `@Json(name = …)` doesn't rename it. Every filtered field needs an index ([production](recipes.md#production)), and on Postgres no index serves an `Instant` or `OffsetDateTime` range or sort: for "latest first", use `Newest`.

## The domain class

Your queries and changes over the tables, built per transaction ([guide step 3](guide.md#step-3-the-domain-class)). It never checks permissions, publishes, or names a state, and takes `Random` and `Clock` from its constructor.

## Features

A feature is the states, rules and operations of one area of the app, over its domain:

```kotlin
class BoxOfficeFeature(clock: Clock) : Feature<BoxOffice, Actor>({ tables -> BoxOffice(BoxOfficeTables(tables), clock) }) {
    val shows by state { boxOffice -> boxOffice.shows() }
    val bookings by state { boxOffice, id: ShowId -> boxOffice.bookings(id) }
    val watching by presence(ShowId, ViewerId)

    val fan by rule { ctx, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }

    val reserve by operation(fan) { boxOffice, actor, pick: ShowSeat, fan ->
        val show = boxOffice.show(pick.show) ?: return@operation Rejected(seatField, "No such show")
        …
        boxOffice.reserve(pick, fan.id)
        Changes.of(bookings.topic(pick.show))
    }
}
```

### States

A state is named after its property, and that name is its topic. A value of `null` means its subject doesn't exist.

- **Stored:** `by state { domain -> … }` or `by state { domain, key: Key -> … }`, where the key is a value type. `bookings.topic(id)` is its topic, for `Changes.of(…)`; with no key, `shows.topic()`. **Gotcha:** every feature in a tenant shares one set of topics, so give states different names across features. Two states called `list` would redraw each other's regions.
- **Presence:** `by presence(Keys, Members)` is who's here, read as a map (take `.keys`). `watching.join(scope, fan, show)` returns a lease; close it to leave. `by presence(Members)` has no key. Joining and leaving publish, so operations never do.
- **Presence carrying a value:** `by presence(Members, carrying = CellRef)`, or per key `presence(Keys, Members, CellRef)`, where `CellRef` is a value type, read as `Map<Member, CellRef?>`. Carry one with `here.carry(scope, actor, key, value)`, or bind it to a route: `here.carry(workspace, actor) { cellRef(it) }`. Only someone present can carry a value: anyone else's post answers 404 and is reported as `CarryRefused`, with the presence, who and the key. The value goes when the member's last lease ends. See [Recipe: per-viewer, short-lived state](recipes.md#per-viewer-short-lived-state).
- **Derived:** `by derived { ctx, key -> … ctx.read(a, key) … }` reads other states, in one transaction.

**A derived state's block** ([guide step 6](guide.md#step-6-derived-states)) gets a `ctx` (a `StateContext`) and the key; a rule's gets `ctx`, the actor and the key:

- `ctx.read(state, key)` reads any state, and takes on its topics.
- `ctx.watch(topic)` adds a topic that isn't a state.
- `ctx.stale(at = instant)` re-reads at that moment, with nothing published.
- `ctx.tenant` is the tenant being read, for asking a service outside the app ([recipe](recipes.md#reading-from-another-system)).
- `ctx.domain` reads what isn't a state. Nothing tracks it, so pair it with a `ctx.read` or `ctx.watch`.

### Operations

```kotlin
val reply by operation(rule, Input::key) { domain, actor, input: Input, found -> … }
val clear by operation(actorRule) { domain, actor, found -> … }
```

The second form takes no input, guarded by a rule on the actor alone.

An operation runs in one write transaction, which never interleaves with the tenant's other writes, and ends in one of four outcomes:

- **`Changes`:** accepted. `Changes.of(topic)`, `Changes.of(setOf(a, b), result)` or `Changes.none()`. Topics publish only after commit. The optional `result` goes back to the caller.
- **`Rejected(field, message)`:** the input or current state won't do. It is shown on that field, to this viewer only.
- **`Denied`:** not allowed, a 403.
- **`Hidden`:** as far as you know, it doesn't exist, a 404.

A refusal rolls back everything written before it, and nothing publishes.

**Checking input.** `FieldName.check(Values, raw, message)` (or `check<SomeEnum>`) turns input into a value or a `Rejected`; `zip` keeps every error, and `.orElse { return@operation it }` ends the operation. Keep typed text in a String signal: a typed signal that doesn't parse is a 400, not a field error.

```kotlin
val (status, commit) = FieldName.of("status").check<BuildStatus>(posted.status, "A build is passing, failing or running")
    .zip(FieldName.of("commit").check(Commit, posted.commit, "A commit is 7 to 40 hex characters")) { status, commit -> status to commit }
    .orElse { return@operation it }
```

**Rules are the security boundary.** A hidden button protects nothing; the rule on the server does. Every operation, page and route runs its rule.

**Gotcha:** a race is not a permission. Something gone since the page rendered is `Rejected` by a rule, with a reason the viewer sees. Only "not yours" is `Denied`.

### Rules

A rule decides who may do something. Operations, pages and routes share it. It answers `Allowed(found)`, or refuses with the same outcome an operation would: `Hidden`, `Denied` or `Rejected(field, message)`:

```kotlin
val visibleRoom by rule { ctx, viewer, name: RoomName ->
    val room = ctx.read(room, name)
    if (room?.visibleTo(viewer) == true) Allowed(room) else Hidden
}

val say by operation(visibleRoom, Said::room) { rooms, viewer, said, room -> … }
```

- **An operation names its rule** and how to get the key from its input, or just the rule when the input is the key. The block gets what the rule found. `operation { … }` with no rule is open.
- **A rule with no key narrows the actor:** `val fan by rule { ctx, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }`, then `operation(fan) { boxOffice, actor, pick: ShowSeat, fan -> … }`.
- **Rules build on rules** with `flatMap`, which carries on from `Allowed` and may refuse: `ctx.read(visibleRoom, viewer, name).flatMap { room -> if (room.owner == viewer) Allowed(room) else Denied }`. `map` changes what was found without refusing.
- **A rule is a state,** so other rules and states can read it: `ctx.read(rule, actor, key)`, or `ctx.read(rule, actor)` for a rule with no key, and `tenant { rule.read(actor, key) }` in a test.
- **Pages and routes name rules too:** `Viewing(actor, key, rule)` guards a page. A route that reads state binds a rule as its handler, with the actor and key worked out from the request as for a page: `chat.yourMessage(workspace, principal, messageRef) { found, ref -> … }`, or `grid.cells(workspace, viewer) { cells -> … }` for a rule with no key. The handler gets what the rule found, then the actor and the key if it names them, in the order a region's view gets them: `{ found -> }`, `{ found, actor -> }` for a rule with no key, `{ found, key -> }` and `{ found, actor, key -> }`. It never gets the request: anything else it needs belongs in the rule's key. A refusal answers 403 (404 for `Hidden`), and a key that doesn't parse answers 400. See [Recipe: who may see a page](recipes.md#who-may-see-a-page).
- **Refusals are reported** as `OperationRefused` on `events`, with the operation, the actor, the key the rule was checked on and the rule; a value carried by someone not present, as `CarryRefused`, with the presence, the actor and the key. The actor and key are held as they are, and the key is `null` when there is none, as in `Redrawn`. So the event log shows who is sending what they shouldn't, and about what.
- **Gotcha:** a rule an operation uses can't read presence. It fails rather than seeing nobody.

### Driving it directly

`scope.execute(boxOffice.reserve, Actor.Fan(ann), ShowSeat(show, a1))` returns the operation's `Outcome` once it has committed and published. `scope.read(boxOffice.shows)` and `scope.read(boxOffice.seatMap, show)` read states. Background workers and webhooks work this way, with no HTTP.

### Several features

An app holds any number of features over the same storage, bus and presence. A feature is given the features it reads (`OrdersFeature(boxOffice, runtime)`), so dependencies run one way. A derived state can read any feature's states.

- **An operation can't run another feature's operation.** When one change spans two features, one feature's domain writes it over the tenant's shared tables, and its `Changes` names both features' topics, as the box office's checkout does.
- **A feature has one actor type.** Changes made by someone else, such as the system recording what a job did, either go in a feature with its own actor (the features example's `AnnouncementsFeature` acts as `Announcer`), or get a case in the app's actor type (the box office's `Actor.System`).
- **An operation that makes work for a job must be able to see the job.** If the job's feature builds on the operation's, flip the dependency: rooms are given `AnnouncementsFeature`, whose `opened` job announces each room that `create` makes.

### Jobs

```kotlin
val order by state { orders, id: OrderId -> orders.order(id) }

val charge by job(OrderId) { scope, id ->
    scope.read(order, id)?.takeIf { it.status == OrderStatus.Pending }?.let { take(scope, it) }
}
```

A job is work on one key ([guide step 19](guide.md#step-19-jobs)), a value type whose factory it is given. Its body gets the tenant's `scope` and the key, with no transaction open, reads what it needs (here the order), and records what it did by executing operations.

- **Named by an operation:** `Changes.of(charge.topic(order.id))`, with as many keys as you like. That records a row for the job and key in the tenant's `fanbelt_jobs` table, in the operation's own transaction, and after commit wakes every node running `Jobs`, which runs it at once. If the wake is lost, the work is late, never lost.
- **Done when the body returns:** the row is deleted.
- **A body that throws** is one `JobFailed(job, cause)` event. The row stays, and the key runs again a second later, or at once after a restart.
- **No backoff or giving up in Fanbelt.** An app that wants to give up records its attempts in its own domain, and the body does nothing once it has given up.
- **Recorded work is found again:** at start-up and once per heal (`FANBELT_BACKPLANE_HEAL`, five minutes by default), `Jobs` reads every recorded row and runs it.
- **One node per key:** each run takes a lock on its job, tenant and key, without waiting. A node that can't take it leaves the key to the holder, which runs it again if it was named during the run. A key named just as the run finishes waits for the next heal.
- **The lock keeps runners apart, and nothing else.** Operations never take it, so the key's state can change while the body runs (a fan cancels mid-charge), and the operation that records the result decides from the state as it is then. To make "in progress" something others can see, see [showing work in progress](recipes.md#showing-work-in-progress).
- **Locks come from the storage.** Postgres storage takes advisory locks, which hold across nodes and go with a dead node's connection. If a live node's lock connection fails, its running jobs carry on while another node may start the same keys, so a key can run twice at once. In-memory storage locks within the process.
- **Run them:** add `Jobs(infra, orders.charge)` to the `Node`. A node runs only the jobs it's given, so a worker process can run them over the same storage and transport while the web nodes leave them out.

**Gotchas:**

- **At least once.** A node can make the outside call and die before the row is deleted, and the key runs again. Make outside calls idempotent, with a key the far end checks.
- **A body can run when its work is already done,** so it reads current state first, as `charge` checks the order is still `Pending`.
- **The key is a value type, and the job is given its factory** (`job(OrderId)`), which writes the key as text and turns the recorded text back into a key.
- **SQL storage needs the `fanbelt_jobs` table.** Create it in your migrations like any other document table. If a large backlog is likely, index it: `CREATE INDEX ON fanbelt_jobs ((doc -> 'key'))`.
- **A body waiting on a slow call holds one of the scheduler's threads** until it returns. Set a timeout on the client.
- **A job's name is its property name,** unless given one: `job(OrderId, name = JobName.of("charge")) { … }`. `Jobs` refuses two jobs with the same name.
- **Recorded rows hold the job's name and key as text.** So renaming a job's property, or narrowing its key's type, strands the rows waiting at deploy. To rename the property safely, keep the old name with `name =`.

## Regions

A region shows one state: a **key** from the request, the state's value as its **model**, and a **view**.

```kotlin
Region("seats", renderer, showId, boxOffice.seatMap) { map, show ->
    SeatMapView(map.show.id, map.seats.groupBy { it.seat.row }.map { … })
}
```

- **Key:** a function of the request, such as a path lens. A state with no key takes none: `Region("todos", renderer, todo.list) { … }`. Key on the viewer only when the *read* differs per viewer; when only the look differs, do it in the view.
- **View:** `{ model, key -> … }`, or `{ model, viewer, key -> … }` when the look differs per viewer; a state with no key drops the key: `{ model -> … }`, `{ model, viewer -> … }`. The viewer is whoever the page's `Viewing` admitted, so a region that uses it only fits a page whose actor is that type. The view reads those and nothing else: no request, no storage, no presence. A look that differs per *page* rather than per viewer, such as which channel in a sidebar is the current one, goes in the browser: the page sets a local signal and the template's `data-class` reads it.
- **Template:** renders exactly one root element carrying the region's id.
- **Absent and failing:** a `null` model renders a "gone" view; a read or view that throws shows `fallback` and reports `RedrawFailed` ([guide step 13](guide.md#absence-is-a-value)).
- **Gotcha:** models and view models must be data classes, or unchanged models are re-sent and renders aren't shared.

## Pages

```kotlin
Page(
    workspace = workspace,
    layout = Layout(
        regions = listOf(header, seats, basket),
        renderer = renderer,
        subject = seats,
        signals = ShowSignals(),
    ) { shell, (_, show, fan) -> ShowPageView(shell, show, fan.id) },
    viewing = Viewing(asFan, showId, boxOffice.fan, onConnect = boxOffice.watch),
)
```

- **`Viewing(actor, key, rule, leaveTo, onConnect)`** is who may view it, and what about. `actor` and `key` are worked out from the request once; `rule` is the rule they must pass, on the key or on the actor alone, and `leaveTo` is where a refused viewer is sent (`/` by default, on arrival or while watching). Leave out what a page doesn't have: the theatre page's `Viewing(asFan, boxOffice.fan)` has no key, and the grid's `Viewing(principal, grid.arrive { it.actor.id })` neither key nor rule.
- **The view and `onConnect` are handed an `Admitted(actor, key, found)`:** the viewer and key the rule checked. It destructures: `{ shell, (_, show, fan) -> … }`. With no rule, `found` is `Unit`; with no key, `key` is.
- **`Layout(regions, renderer, subject, signals) { shell, admitted -> … }`** is what the page draws; one that needs only the shell takes `{ shell -> … }`. What the page shows frozen as it was on arrival, not live, the view reads once from the scope it can be handed, `{ scope, shell, admitted -> … }`: a room's "new since you were last here" is `scope.read(rooms.unseen, Reader(name, me))`, read before entering marks the room seen.
- **`onConnect` runs as their stream opens,** given the tenant's scope and who was admitted, and returns an `AutoCloseable` that's closed when the stream ends.
- **What arriving means is declared on the feature as a `visit`,** and bound as `onConnect`. Its body is given the tenant's scope and returns what leaving closes: `val enter by visit { scope, viewer, name: RoomName -> scope.execute(markSeen, viewer, name); val lease = here.join(scope, viewer, name); AutoCloseable { scope.execute(markSeen, viewer, name); lease.close() } }`, and a visit whose actor and input are the page's actor and key is an `onConnect` as it stands: `Viewing(viewer, roomName, rooms.visibleRoom, leaveTo, rooms.enter)`. One that isn't maps who was admitted: `grid.arrive { it.actor.id }`, for one with no input. A test opens it inside the tenant: `tenant { rooms.enter.open(ann, plans) }`.
- **A visit can be guarded by a rule,** with the same forms as an operation, and is then handed what the rule found: `val watch by visit(fan) { scope, _, show: ShowId, fan -> watching.join(scope, fan.id, show) }` needs no `when` on box office's sealed `Actor`, because the `fan` rule narrowed it. A refusal opens nothing, and Denied or Hidden is reported as `VisitRefused`, with the visit, the actor, the key and the rule, so a test that opens it for someone the page would refuse is refused too.
- **A visit is a domain fact, not a page's load script.** `rooms.enter` means "in this room, and all of it seen" whichever page binds it. A page can still join one presence inline in `onConnect`, but a visit can be opened in a test and guarded by the same rule as the page, so prefer one.
- **With no rule,** a page is open to the tenant, and `Page(workspace, layout)` has no viewer at all.
- **A key that doesn't parse is a 400,** for the page and its stream, as a region's is.
- **The viewer is worked out for the first paint and the stream alike,** so the view and `onConnect` are given the same one.
- **Mount it** with `"/shows/{show}" bind ShowPage(…)`: the first paint and one stream for every region, on one route.

```html
<base href="{{shell.base}}">
<body data-init="{{shell.connect}}" data-signals="{{shell.signals}}">
{{{shell.regions.seats}}}
```

- **`shell.signals`** comes from the page's signals class: each field with its default, plus an `errors` entry for each.
- **`subject`** names the region the page is about. If its state is absent on first paint, the page renders the absent view and answers 404.
- **URLs are relative to the tenant root** (`shows/{{show}}/checkout`).
- **Reading a page never writes.** If arriving should change something, declare it as a `visit` and bind it as `onConnect`.
- **Gotcha:** open the stream with `{{shell.connect}}`, never a hand-written `@get`, or the page goes dead after a deploy.

### One-shot reads

Something that needn't stay live, such as a download, is a route that binds a rule and reads once:

```kotlin
val cells by rule { ctx, _ -> Allowed(ctx.domain.all()) }

"/grid.csv" bind GET to grid.cells(workspace, viewer) { cells ->
    Response(OK).header("content-type", "text/csv").body("cell,text\n" + cells.joinToString("") { … })
}
```

The rule finds what may be served, `Hidden` when it's missing. Later changes don't show until it's fetched again. A panel or modal binds the same way, answering with `datastarElements` into an empty placeholder outside the page's regions.

## Routes

Invoke an operation with the workspace, the actor and where its input comes from:

```kotlin
val asFan = { request: Request -> Actor.Fan(principal(request).id) }

"/shows/{show}/seats/{seat}/reserve" bind POST to boxOffice.reserve(workspace, asFan) { ShowSeat(showId(it), seat(it)) },
"/shows/{show}/checkout" bind POST to orders.checkout(workspace, asFan, input = showId),
"/rooms/{room}/say" bind POST to rooms.say(workspace, viewer, roomName, reset = setOf(RoomSignals::line)) { signals: RoomSignals, room ->
    Said(room, signals.line)
},
```

- **With signals,** the input function gets the parsed signals class, and the key when the binding names one (`roomName`, worked out from the request as `Viewing`'s key is); it never gets the request. **Without signals,** the input is worked out from the request alone: a lens (`input = showId`) or a function (`{ ShowSeat(showId(it), seat(it)) }`).
- **A feature with no actors** (`Feature<Domain, Unit>`) binds without one: `todo.toggle(workspace, input = todoId)`.

| Outcome | Answer |
|---|---|
| `Changes` | Resets the `reset` signals and clears the page's errors. The result goes to the `Feedback`. |
| `Rejected` | Sets the `errors` signal for each rejected field and clears the page's other errors, to this viewer only. |
| `Denied` | 403 |
| `Hidden` | 404 |

The default `Feedback` is `ErrorSignals`; `GoTo` moves the browser. A route never publishes and never returns HTML. A hand-written route that reads a lens wraps itself in `CatchLensFailure()`.

Mount the tenant's routes and pages with `workspace.routes(…)` beside the app's own:

```kotlin
val oneTheatre = poly(listOf(BoxOfficeRoutes(boxOffice, orders, workspace)), "/shows/{show}" bind ShowPage(workspace, boxOffice, orders, renderer))
poly(home, workspace.routes(identified.then(oneTheatre)))
```

## Tenancy

```kotlin
val workspace = Workspace(infra, "/theatres/{theatre}", theatres::exists)
```

- **The tenant is the template's one path variable.** For subdomain or token tenancy, pass both functions: `Workspace(infra, template, exists, tenant = { request -> … }, root = { tenant -> Uri })`.
- **`workspace.routes(…)` answers 404** for a tenant that `exists` rejects, for pages, routes and streams alike. `exists` runs on every request; cache it if tenants are never deleted.
- **Deleting a tenant** is the app's to do: delete all its rows, `fanbelt_jobs` included, or the jobs it still owes keep running at every heal. A stream that's already open keeps running, because `exists` is checked only when it connects: to send its viewers away, put the tenant's existence in the page's rule, as for [who may see a page](recipes.md#who-may-see-a-page).
- **A scope is only ever handed to you,** never fetched: a job's and a visit's body are given one, as are a Viewing's `onConnect` and a Layout's view, to read once what it shows frozen. Routes bind operations, rules and presence instead. `workspace.uri(request)` is the tenant's root.
- **The app owns its tenants;** `App` creates none ([guide step 18](guide.md#step-18-the-app)).
- **Membership ("not yours") is a rule:** a page names it in its `Viewing`, and a route that reads state binds it. See [Recipe: who may see a page](recipes.md#who-may-see-a-page).

## Wiring: `App`

`fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra): Node`, as in [guide step 18](guide.md#step-18-the-app).

- **`AppRuntime(clock, random, events, scheduler, http)`** is everything with side effects. `http` is the outbound client every adapter wraps; `withEvents` filters the events. Tests pass a `TestRuntime`.
- **`AppInfra(runtime, storage, transport, presenceBackend, env)`**, each defaulting to one node in memory:

| Part | Choices |
|---|---|
| storage | `InMemoryDocumentStorage()`, `PostgresDocumentStorage(db, json)` (or `PostgresDocumentStorage(db, json, KeyLocking)`); each brings the locks `Jobs` takes |
| transport | none (one node), `PostgresTransport(connect, scheduler, events)`, `RedisTransport(uri, events)` |
| presence | `InMemoryPresenceBackend(clock)`, `ExposedPresenceBackend(db, clock)`, `RedisPresenceBackend(uri, clock)` |
| env | the `Environment` that `FanbeltSettings` are read from; empty by default |

- **`main` picks the technology and the starting data;** `App` creates none, so tests start empty.
- **Gotcha:** every part with background work (the infra, your workers) goes in the `Node`, which starts and stops them. A part left out fails on first use.
- **Jobs run in the `Node`:** `Node(app, runtime, infra, Jobs(infra, orders.charge))`. See [Jobs](#jobs).

## Testing

Write scenarios once as a contract, against a driver interface of verbs (`adds`, `watches`, `sees`) that returns a `Verdict` (`Accepted`, `Rejected(reasons)`, `Denied`, `Hidden`, or `Unavailable` when the page offered no way to try). Make one user act and *another*, already watching, see; the domain driver reads the same states, with the same keys, as the regions show. Wrap checks on other people's screens in `eventually { }`. The test kit is the `testing` module.

**Domain driver: `TestTenant()`.** One tenant on a deterministic `TestRuntime`, in memory unless given `storage` (`TestTenant(storage = postgres)`); build features from `tenant.clock` and `tenant.random`.
- **Steps run inside the tenant,** with the operation or state first: `tenant { boxOffice.reserve.execute(actor, seat) }` returns a `Verdict`; `seatMap.read(show)`, `watching.join(actor, show)` and `here.carry(actor, key, value)` (no key: `read()`, `join(actor)`, `carry(actor, value)`). Inside the block only these verbs are added, so the tenant's own fields can't shadow yours.
- **Arrange with `operation.given(actor, input)`:** it returns the operation's result, and fails the test if it is refused. Seed with `tenant.storage` and `tenant.id`. Inside the tenant, `tables { … }` reads rows no state shows, `tick(duration)` moves the clock and `settle()` runs what's pending.
- `tenant.runtime.events` records events, so a test can check a failure path.
- `tenant.runs(orders.charge)` runs those jobs on the tenant's node: a named job has run by the time `execute` returns, and its retries run as `tick` moves the clock. Build the features from the tenant first, so they can be given to it.
- **Missed publishes fail the step that missed them.** After each `execute`, `given`, `join`, `carry`, `tick` and `settle`, and before each `read`, every state the test has read is read again, and one that changed without its topics being published fails the step. It only checks states the test has read, so read what a page would show before acting. A visit opens inside the block as a page's would on connecting (`val visit = tenant { rooms.enter.open(ann, plans) }`), settling after arriving and again when it's closed.

**UI driver: `browser(app, scheduler, cookie)`.** A headless Datastar browser over the real `App(TestRuntime(scheduler))`, one per user. `verdictAt(errorSelector)` reads the last action's result, and `close()` closes the tab.
- **Gotcha:** a control the page hides is `Unavailable` in a UI driver, so assert refusals with `refusedAs(Denied)` or `refusedAs(Hidden)`. The domain driver never answers `Unavailable`, so it proves the exact refusal: rules are the security boundary, not the page.

**Real browser: `Http4kBrowser(chromium, uri)`,** from `http4k-testing-playwright`. `browser.newPage(cookie)` gives each user a page in a context of its own; close the `Playwright` when done; `page.verdictAt`, `page.navigates` and `page.posting` mirror the emulator.

**Several nodes: `InMemoryCluster`.** Build each app with `App(cluster.runtime, cluster.node())`. Its nodes share one storage, in memory unless given (`InMemoryCluster(storage = postgres)`). `node.dies()` kills one.

Pass fakes for external services to `App`, so the UI driver stays deterministic.

**Marbles: `MarbleEvents()`.** Pass it as the app's events and it draws each test's steps (operations, topics, re-reads, patches) one lane each, as text and HTML in `build/reports/marbles`: how a change spread, for when a test fails or a page redraws more than it should. A `○` is a read that found nothing new. See [Recipe: marble traces](recipes.md#marble-traces).

## Production

- **Schema:** your migrations create every table and an index for each filtered field.
- **Several nodes:** every node uses the same storage and the same transport. No sticky sessions.
- **Health:** wrap the app in `ReportFailures(events)`, with no inner `CatchAll`.

The rest (indexes, poolers, proxies, locking, presence leases) is in [Recipe: production](recipes.md#production).

### Settings

`AppInfra` reads these from the `Environment` it's given (each example's `main` passes `ENV`):

| Setting | Default | What it does |
|---|---|---|
| `FANBELT_REDRAW_LIMIT` | 8 | How many live re-reads a node runs at once. Keep it below your connection pool's size. |
| `FANBELT_STREAM_KEEPALIVE` | 2 s | How often each stream is pinged, which keeps it open through proxies and notices a closed tab. |
| `FANBELT_PRESENCE_HEARTBEAT` | 10 s | How often a node renews its presence leases. |
| `FANBELT_PRESENCE_TTL` | 30 s | How long a lease lasts without renewal. Must be more than twice the heartbeat. |
| `FANBELT_BACKPLANE_HEAL` | 5 min | How often every view re-reads, and every recorded job runs, to catch anything a lost message missed. |

## Limitations

- `page()` sorts by one field, and has no joins or aggregates beyond `count`. Beyond that, filter a bounded set in Kotlin, or use [your own SQL tables](recipes.md#your-own-sql-tables).
- Behind a path-rewriting proxy, `Workspace`'s default lookup needs the prefix added.
- Over HTTP/1.1, a browser stalls after about six open tabs of one app.
