# Fanbelt: recipes

Common patterns, each built from ordinary states, operations, regions and a few lines of template. The [reference](reference.md) has the API.

## Authentication

Authenticate with http4k (OAuth, OIDC, bearer tokens, your own session cookie), and let `Authenticated` put the principal in a `RequestKey` for the page and its stream alike:

```kotlin
val principal = RequestKey.required<Principal>("principal")

val identified = Authenticated(principal, onRefused = { Response(SEE_OTHER).location(Uri.of("/")) }) { request -> yourSessions.find(request) }

rooms.say(workspace, actor = { request -> principal(request).id }, …)
```

- **Someone it can't identify gets `onRefused`** (a 401 by default), on a page and its stream alike. Past it, pages, views and presence all read the same `principal`, and the view and `onConnect` are given who the page admitted.
- **Where it goes decides what a stranger learns.** Inside `workspace.routes(identified.then(oneSpace))`, an unknown tenant is a 404 for everyone, so someone signed out can tell which tenants exist. If tenant names are private, put it outside, `identified.then(workspace.routes(oneSpace))`, and everyone signed out gets the same refusal.
- **Machines get routes of their own** beside the people's, with no viewer: `workspace.routes(poly(BuildsWebhook(…))), workspace.routes(identified.then(oneSpace))`.
- **Membership is the page's rule:** `Viewing(viewer, roomName, rooms.visibleRoom, …)` refuses both the page and its stream to anyone the rule refuses ([who may see a page](#who-may-see-a-page)). A 404 hides whether the thing exists; answer 403 only where that isn't secret. Rooms do this for private rooms.
- **Gotcha:** Datastar posts with cookies. Reject cross-site POSTs (an `Origin` that isn't yours) and set cookies `SameSite=Lax`.

## Authorizing from a token's claims

Verify the token in the authentication filter, and let the claims be the actor:

```kotlin
data class Claims(val subject: UserId, val tenant: TenantId, val scopes: Set<String>, val expires: Instant)

val publishableReport by rule { ctx, actor, report: ReportId ->
    ctx.stale(at = actor.expires)
    val found = ctx.domain.report(report)
    when {
        clock.instant() >= actor.expires -> Hidden
        found == null || found.tenant != actor.tenant -> Hidden
        "reports:write" !in actor.scopes -> Denied
        else -> Allowed(found)
    }
}
```

- **Keep the actor small and stable.** It's part of the rule's key: leave out `iat`, `jti` and the raw token.
- **A stream outlives its token.** `ctx.stale(at = actor.expires)` re-reads the rule at expiry, and a page that [names the rule](#who-may-see-a-page) sends the viewer away.

## Who may see a page

An operation names the rule that guards it, and so does a page, in its `Viewing`. In the features example, a private room is its owner's alone, and only the owner may rename a room:

```kotlin
val visibleRoom by rule { ctx, viewer, name: RoomName ->
    val room = room.read(ctx, name)
    if (room?.visibleTo(viewer) == true) Allowed(room) else Hidden
}

val yourRoom by rule { ctx, viewer, name: RoomName ->
    visibleRoom.read(ctx, viewer, name).flatMap { room -> if (room.owner == viewer) Allowed(room) else Denied }
}
```

```kotlin
Viewing(viewer, roomName, rooms.visibleRoom, { workspace.uri(it).appendToPath("rooms") }, rooms.enter)
```

| Path | Guarded by | When it asks |
| --- | --- | --- |
| An operation | naming the rule, `operation(visibleRoom, Said::room)` | every time it runs, in its transaction |
| A page's first paint and stream | naming the rule, `Viewing(viewer, roomName, visibleRoom)` | as the request arrives |
| A page that is already open | the same rule, read again | when a topic it watched is published, or at its `ctx.stale` |
| Any other route that reads state | binding the rule as its handler, `grid.cells(workspace, viewer) { cells -> … }` | as the request arrives |

- **One rule, every path.** Saying something and seeing the room both read `visibleRoom`, so who may see and who may act can't drift apart, and the domain tests, which read and execute through `TestTenant`, check it too.
- **An open page is sent away.** The page keeps reading the rule while its stream is open, and moves the viewer on once it stops allowing them. A rule that asks a service outside the app rechecks with `ctx.stale(at = …)`, as a token's expiry does above.
- **Other routes that read state** bind a rule that finds what they show: the grid's CSV download binds `cells`, so the handler gets the cells and a refused viewer gets 403 or 404. A route that reads nothing, such as a redirect, needs no rule beyond being signed in.

## Where a refused viewer goes

A viewer the rule refuses gets 403 for `Denied` and 404 for `Hidden`, and is sent to `Viewing`'s `leaveTo`, `/` by default. The same script that moves them on arrival is sent to a page they already have open, and that page stops changing. A refused stream gets only the status. Where that is is the app's choice. The features example sends everyone a room is hidden from, or who asks for one that doesn't exist, back to the room list, so a private room can't be told from a missing one:

```kotlin
Viewing(viewer, roomName, rooms.visibleRoom, { request -> workspace.uri(request).appendToPath("rooms") }, rooms.enter)
```

- **Make the location absolute.** The refused answer has no `<base>`, so a relative one resolves against the page that refused.

## Create, then go there

The operation returns the new id as its result, and the route answers with `GoTo`:

```kotlin
"/rooms" bind POST to rooms.create(workspace, viewer, feedback = GoTo { name -> Uri.of("rooms/$name") }) { signals: NewRoomSignals -> NewRoom(signals.name, signals.private) }
```

Only the person who acted moves; everyone else sees the new room appear. A route can't redirect, because Datastar posts with `fetch`. In tests, `outcome.accepted()` reads the result.

## Per-viewer, short-lived state

Cursors, "typing…": carry them in presence, which already knows when a viewer leaves.

```kotlin
val here by presence(ViewerId, carrying = CellRef)

"/cursor/{cell}" bind POST to grid.here.carry(workspace, viewer) { cellRef(it) }
```

- **The page's rule guards the route too.** Only someone already present can carry a value, and a viewer joins in the page's `onConnect`, after its rule has let them in. Anyone else's post is refused with a 404 and reported as `CarryRefused`, whichever node it lands on.
- **One value per presence state.** For several things that change separately, declare a presence state for each.
- **For "typing…",** carry when it lapses (an `InstantValue`), and read it with `ctx.stale(at = until)`.
- **A soft lock others must respect is stored data,** not presence: a row with an expiry, checked in the operation.

## Validation as you type

An operation that checks and changes nothing:

```kotlin
val checkTitle by operation { todos, actor, raw: String ->
    todoTitle(raw).orElse { return@operation it }
    Changes.none()
}

"/todos/check" bind POST to todo.checkTitle(workspace) { signals: TodoSignals -> signals.title },
```

```html
<input data-bind:title data-on:input__debounce.300ms="@post('todos/check')">
```

Share the check (`todoTitle`) with the real operation, so the two can't disagree.

## Growing lists

"Show more" is a link. Put the window in the URL and the region's key:

```kotlin
val show = Query.value(Shown).defaulted("show", Shown.of(20))
Region("notes", renderer, { show(it) }, notes.newest) { window, shown -> … }
```

```html
{{#if more}}<a href="notes?show={{more}}">Show more</a>{{/if}}
```

For true paging, use a cursor (`?before=…`) instead.

## Watch coarse, read narrow

An operation publishes what it knows: `post` knows a note was added, not which windows of the board are open. So the stored state is coarse, the board's count. A region that shows a window reads narrowly instead: a derived state keyed by the window watches the count and asks the domain for only the notes it shows.

```kotlin
val count by state { it.count() }

val newest by derived { ctx, shown: Shown ->
    ctx.watch(count.topic())
    ctx.domain.newest(shown)
}
```

- **A stored state of every note would read the whole board** on every post. The window's read grows with the window instead.
- **Every `ctx.domain` read is paired with a `ctx.watch`.** Miss one and the test tenant fails the step that changed it.
- **A filter on a field inside the id needs the field stored on its own.** `Cell::row eq number` runs in the database; filtering the id's text in Kotlin reads the whole table.

## Smooth navigation

Every view is a URL. Make full page loads seamless with View Transitions:

```css
@view-transition { navigation: auto; }
```

## Input from another system

Another system changing what people see is an operation with a non-human actor, bound with `Statuses()` as its feedback so it answers with status codes: `202` when accepted, `400` with the errors as text when rejected, and `403` or `404` when a rule refuses:

```kotlin
"/builds" bind POST to CatchLensFailure().then(ServerFilters.BearerAuth(ciToken)).then(
    builds.report(workspace, { Ci }, Statuses()) { it.json<BuildPosted>() }
)
```

The operation returns its `Changes`, and every page showing the builds updates.

## Reading from another system

A state can show what an outside service says: call its client inside the read. The features example's status page shows what a status service says about the space:

```kotlin
class StatusFeature(private val http: HttpHandler, private val clock: Clock) : Feature<Unit, Unit>({ }) {
    val current by derived { ctx, _: Unit ->
        ctx.stale(at = clock.instant() + ofMinutes(1))
        val response = http(Request(GET, "/status/${ctx.tenant}"))
        if (response.status.successful) ServiceStatus(response.bodyString()) else ServiceStatus.UNKNOWN
    }
}
```

`App` builds the client over `AppRuntime.http`, so tests answer with a fake service. A `Region` over `current` is written like any other, and a rule can ask a service the same way, to guard operations and pages ([who may see a page](#who-may-see-a-page)).

- **`ctx.tenant` names the tenant to the service.** Code can read its tenant, never choose one.
- **There's no cache.** Every read calls the service, but a region's read is shared by everyone watching it, so calls follow redraws, not viewers. An operation's rule always decides on a fresh answer.
- **A change made in the service publishes nothing.** The region re-reads when a topic it watches is published, or at `ctx.stale(at = …)` (here, a minute on); otherwise it shows the old answer. A `read` inside a `TestTenant` fails a test if a state changes without either.
- **Decide what a failure means.** Here a failed call shows as `Unknown`. A rule should fail closed, and a derived state that throws shows its region's fallback and reports `RedrawFailed`.
- **Gotcha:** the call runs inside the transaction. A region's read holds a read transaction while it waits; an operation's rule holds the write lock, which on Postgres's default locking queues every other write in the tenant. Keep the call fast, and give the client a timeout.

## Showing work in progress

A job's lock keeps other runners away, not other writers: a fan can cancel an order while its charge is with the provider. When the domain should know a job has started, the body says so with an operation before it calls out:

```kotlin
val charge by job(OrderId) { scope, id ->
    val order = order.read(scope, id)?.takeIf { it.status == Pending || it.status == Charging } ?: return@job
    if (order.status == Pending) charging.execute(scope, Actor.System, id)
    take(scope, order)
}
```

- **`Charging` is a fact,** so pages show it and rules use it: `cancel` refuses a charging order, and `paid` and `declined` accept only a charging one.
- **A run that dies mid-charge isn't stuck.** Its recorded key stays, so it runs again at start-up or the next heal, finds `Charging`, and carries on. The key's lock means no other runner is charging it, so there's no lease or timeout to keep; the provider call must be idempotent, as for any job.
- **Without it,** the operation that records the result decides from current state: `paid` for an order cancelled meanwhile is the domain's to settle, with a refund, say.

## Stamping events with request context

An edge filter puts the context in a `ThreadLocal`, and an `EventFilter` adds it to every event:

```kotlin
private val actingUser = ThreadLocal<ViewerId>()

val RememberActingUser = Filter { next -> { request ->
    actingUser.set(principal(request).id)
    try { next(request) } finally { actingUser.remove() }
} }

val AddActingUser = EventFilter { next -> { event ->
    val user = actingUser.get()
    next(if (user == null) event else event + ("user" to user.value))
} }
```

Add `AddActingUser` in `runtime.withEvents(…)`, and put `RememberActingUser` outside `ReportFailures`.

- **Gotcha:** it only reaches events raised on the request's thread. Redraws, background work and the bus don't carry it.

## Browser-only UI state

Signals whose names start with `_` stay in the browser:

```html
<body data-signals="{_palette: false}" data-on:keydown__window="evt.metaKey && evt.key === 'k' && ($_palette = !$_palette)">
  <dialog data-show="$_palette">…</dialog>
```

Anything the server renders from goes in the URL instead.

## Your own SQL tables

For joins, aggregates or a table another system owns, use your own tables inside the operation's transaction. `tables.sql { }` gives the tenant and the JDBC connection, on Postgres storage only.

```kotlin
object RoomStats : TenantTable("room_stats") {
    val status = varchar("status", 16)
    val count = integer("count")
}

tables.exposed { query { RoomStats.selectAll() } }                                // filtered by tenant
tables.exposed { insert(RoomStats) { it[status] = "private"; it[count] = 1 } }    // tenant stamped

tables.jooq { it.selectFrom(ROOM_STATS).fetch() }                              // every table needs a tenant column
```

- **Exposed** covers `TenantTable`s in reads, joins (outer ones included), subqueries, inserts, updates and deletes, but not raw SQL. It can't filter an aliased `TenantTable`, so `query { }` refuses one in its FROM or joins (self-joins included); an alias inside a subquery isn't caught. **jOOQ** covers aliases and self-joins too, and refuses an insert from a select.
- **Make tenant columns `NOT NULL`.** To keep an outer join's unmatched rows, jOOQ lets a NULL tenant through.
- **Row-level security** on Postgres covers everything, raw SQL included:

  ```sql
  ALTER TABLE room_stats ENABLE ROW LEVEL SECURITY;
  CREATE POLICY tenant_rows ON room_stats USING (tenant = current_setting('fanbelt.tenant', true));
  ```

- **Gotcha:** the app's database user must not own the table or have `BYPASSRLS`, or the policy is skipped.

## Evolving stored rows

Rows are JSON, so the data class is the schema. Adding a field with a default needs no migration, unless you filter or sort on it: rows stored before it don't have it, so on Postgres they compare as missing, not as the default. Backfill it first. Anything else, renaming a field included, is a data migration (filters name fields by Kotlin property, so `@Json(name = …)` can't keep the old name for them). Old and new nodes run side by side during a deploy, so roll it out in three steps:

1. **Expand:** deploy code that reads both shapes and writes the new one.
2. **Migrate:** rewrite the old rows: `UPDATE notes SET doc = jsonb_set(doc - 'text', '{body}', doc -> 'text') WHERE doc ? 'text'` on Postgres.
3. **Contract:** deploy code that reads only the new shape.

A row that fails to decode shows its region's fallback, so add a test that reads a row in the old shape. The same care applies to signals classes, region ids and job keys.

## File uploads

Post the form as multipart, `@post('attachments', {contentType: 'form'})`, read the parts with http4k's multipart lenses in a request-only route, and pass the result to an operation. Store large files elsewhere and keep a reference in the row.

## Islands

A rich widget (a chart, a map) can be a web component inside a region, fed by attributes. When the region re-renders, the morph updates the attributes and the component redraws. Mark it `data-ignore-morph` if it owns its own children.

## Marble traces

Every step of a change is an event on `events`: `Executed(operation, actor, outcome)`, `Published(topic)`, `Redrawn(region, key, changed)` and `Patched(region, actor, sent)`. A marble diagram draws them as one lane each, to show how a change spread: who acted, what was published, what was read again and who was sent what.

- **Record them:** `@RegisterExtension val marbles = MarbleEvents()`, passed as the app's events with `runtime.withEvents(marbles)`. After each test, it draws them as text and HTML in `build/reports/marbles`.
- **Name the lanes:** give the page's `Viewing` the same actor function as the routes, to name lanes by person, and use `marbles.alias(key, name)` to name keys.
- **Narrow it:** `marbles.record { … }` draws only the steps inside it.
- **Pin one down** with an approval test: `approver.assertApproved(AsciiMarbles().render(name, marbles.timeline()).content)`.

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

- **Gotcha:** to move time under a real browser, give the `App` a `DeterministicScheduler()` and its `toClock()`, start `scheduler.pumping()` so work queued by the server runs without a tick, and move time with `scheduler.tick(by)`. A closed tab is only noticed when a keepalive ping fails, so tick past `FANBELT_STREAM_KEEPALIVE` a few times after closing one.

## Production

- **JSON:** database storage writes rows as JSON, so register every value type the rows use in one JSON object (`object TodoJson : ConfigurableMoshi(standardConfig().withFanbeltMappings().value(TodoId).value(TodoTitle).done())`), and pass it to `PostgresDocumentStorage`. A missing one only fails at runtime.
- **Schema:** your migrations create one document table per `TableName` (`PostgresDocumentStorage.schema(name)`), `fanbelt_jobs` if you run jobs, `PresenceTable()` (`fanbelt_presence`) if presence is in Postgres, and an expression index for every field you filter on: `CREATE INDEX ON journal ((doc -> 'ticket'))` (`eq`, `neq` and `oneOf` compare `doc -> 'field'`; a nested field is `doc -> 'address' -> 'city'`). A text field you sort or range on needs `CREATE INDEX ON people (((doc ->> 'name') COLLATE "C"))`. Without one, every redraw scans the table. Fanbelt never creates schema.
- **Several nodes:** point every node at the same storage and the same transport. No sticky sessions.
- **Postgres behind a pooler:** `PostgresTransport` needs a direct or session-mode connection. A transaction-mode pooler (the default URL on Neon, Supabase, Heroku and others) silently delivers nothing.
- **Proxies:** every stream is pinged every `FANBELT_STREAM_KEEPALIVE` (default 2 s), which keeps it open through a proxy and notices a closed tab within about two intervals. Keep idle timeouts above 15 seconds, don't buffer `text/event-stream`, and serve browsers HTTP/2.
- **Redraws:** keep `FANBELT_REDRAW_LIMIT` (default 8) below your connection pool's size. It caps how many live re-reads a node runs at once, and `RedrawWaited` and `AccessWaited` events show a node at its limit. All the settings are in the [reference](reference.md#settings).
- **Presence leases:** a node renews its leases on a heartbeat. If it can't reach the presence backend for longer than the lease, its members drop out for everyone, and come back once it can, republished to every node.
- **A busy tenant:** on Postgres, a tenant's writes queue behind one lock (`TenantLocking`, the default). When one tenant has many independent things being written at once, such as rooms in a busy space, use `PostgresDocumentStorage(db, json, KeyLocking)`. Each operation then locks its rule's key instead, so writes to different keys run side by side; one with no key still locks the whole tenant. The costs:
  - writes run `SERIALIZABLE`, so clashing writes are retried, and fail with a 500 if the retries run out;
  - writes to the same key wait for each other and then retry once, so a key many people write at once is better on `TenantLocking`;
  - without an index on every filtered field, one tenant's scan can make another tenant's writes retry;
  - a cursor saved as "everything up to here" can miss a row that commits late.
- **Your own locking:** implement `WriteLocking` and pass it as `PostgresDocumentStorage(db, json, locking)`. Anything that locks less than the whole tenant needs `TRANSACTION_SERIALIZABLE`, or concurrent writes silently lose updates.
- **Past a few nodes:** use Redis for transport and presence, route requests by tenant at the load balancer, and keep every read on the primary: Fanbelt has no replica reads, because a redraw would read a replica before it has the change it was told about.
- **Health:** wrap the app in `ReportFailures(events)`, with no inner `CatchAll`. Failures and recoveries arrive on `events` (`BackplaneDisconnected` then `BackplaneReconnected`, `PresenceRenewalFailed` then `PresenceRecovered`, and so on), for an http4k `ReadinessCheck`. Every event raised inside a tenant carries it, read with `eventTenant(event)`.
- **Deploys:** pages reconnect by themselves. Changing a stored row beyond adding a defaulted field you don't filter or sort on is a migration: see [evolving stored rows](#evolving-stored-rows).
