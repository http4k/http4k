# Fanbelt: recipes

Common patterns, each built from ordinary states, operations, regions and a few lines of template. The [reference](reference.md) has the API.

## Authentication

Authenticate with http4k (OAuth, OIDC, bearer tokens, your own session cookie), and let `Authenticated` put the principal in a `RequestKey` for the page and its stream alike:

```kotlin
val principal = RequestKey.required<Principal>("principal")

val identified = Authenticated(principal, onRefused = { Response(SEE_OTHER).location(Uri.of("/")) }) { request -> yourSessions.find(request) }

tickets.reply(workspace, actor = { request -> Actor.Agent(principal(request).id) }, …)
```

- **Someone it can't identify gets `onRefused`** (a 401 by default), on a page and its stream alike. Past it, pages, views and presence all read the same `principal`, and `onConnect` is given what the page's rule found.
- **Where it goes decides what a stranger learns.** Inside `workspace.routes(identified.then(oneOrg))`, an unknown tenant is a 404 for everyone, so someone signed out can tell which tenants exist. If tenant names are private, put it outside, `identified.then(workspace.routes(oneOrg))`, and everyone signed out gets the same refusal.
- **Machines get routes of their own** beside the people's, with no viewer: `workspace.routes(poly(BuildRoutes(…))), workspace.routes(identified.then(oneTeam))`.
- **Membership is the page's rule:** `Viewing(channelActorKey, chat.visibleChannel, …)` refuses both the page and its stream to anyone the rule refuses ([who may see a page](#who-may-see-a-page)). A 404 hides whether the thing exists; answer 403 only where that isn't secret. Chat does this for private channels.
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

An operation names the rule that guards it, and so does a page, in its `Viewing`. The helpdesk's pages are for the organisation's agents:

```kotlin
val anAgent by rule { ctx, actor ->
    ctx.stale(at = policy.recheckAt())
    if (actor is Actor.Agent && policy.isAgent(ctx.tenant, actor.id)) Allowed(actor) else Denied
}

val agentOnTicket by rule { ctx, actor, id: TicketId ->
    ctx.read(anAgent, ActorKey(actor, Unit)).flatMap { agent -> if (ctx.domain.ticket(id) != null) Allowed(agent) else Denied }
}
```

```kotlin
val agent = { request: Request -> Actor.Agent(principal(request).id) }

Page(workspace, layout, Viewing(agent, tickets.anAgent) { scope, request, me -> tickets.online.join(scope, me.id) })
```

| Path | Guarded by | When it asks |
| --- | --- | --- |
| An operation | naming the rule, `operation(agentOnTicket)` | every time it runs, in its transaction |
| A page's first paint and stream | naming the rule, `Viewing(agent, anAgent)` | as the request arrives |
| A page that is already open | the same rule, read again | when a topic it watched is published, or at its `ctx.stale` |
| Any other route that reads state | binding the rule as its handler, `customerOf(workspace, viewer) { card, request -> … }` | as the request arrives |

- **One rule, every path.** Operations and pages read `anAgent`, so who may see and who may act can't drift apart, and the domain tests, which read and execute through `TestTenant`, check it too.
- **An open page is sent away.** The page keeps reading the rule while its stream is open, and moves the viewer on once it stops allowing them. The helpdesk rechecks every 30 seconds (`OrgPolicy.recheckEvery`), so an agent the org removes is sent away within that.
- **Other routes that read state** bind a rule that finds what they show: the helpdesk's customer card binds `customerOf`, which reads `anAgent` and then the card, so the handler gets the card and a refused agent gets 403 or 404. A route that reads nothing, such as a redirect to the agent's own page, needs no rule beyond being signed in.

## Where a refused viewer goes

A viewer the rule refuses gets 403 for `Denied` and 404 for `Hidden`, and is sent to `Viewing`'s `leaveTo`, `/` by default. The same script that moves them on arrival is sent to a page they already have open, and that page stops changing. A refused stream gets only the status. Where that is is the app's choice. sl4k sends everyone a channel is hidden from, or who asks for one that doesn't exist, to the same page, so a private channel can't be told from a missing one:

```kotlin
Viewing(channelActorKey, chat.visibleChannel, { request -> workspace.uri(request).appendToPath("no-such-channel") }, enterChannel(chat))
```

- **Make the location absolute.** The refused answer has no `<base>`, so a relative one resolves against the page that refused.

## Create, then go there

The operation returns the new id as its result, and the route answers with `GoTo`:

```kotlin
"/channels" bind POST to chat.create(workspace, member, { signals: ChatSignals, _ -> NewChannel(signals.name, signals.private) }, feedback = GoTo { name -> Uri.of("c/$name") })
```

Only the person who acted moves; everyone else sees the new channel appear. A route can't redirect, because Datastar posts with `fetch`. In tests, `outcome.accepted()` reads the result.

## Per-viewer, short-lived state

Cursors, "typing…": carry them in presence, which already knows when a viewer leaves.

```kotlin
val here by presence(ViewerId, carrying = CellRef)

"/cursor/{cell}" bind POST to SheetFeature.here.set(workspace, actor) { cellRef(it) }
```

Only someone already present can set a value; anyone else's post is ignored, whichever node it lands on. Joining happens in the page's `onConnect`, after the page's rule has let the viewer in, and ends when it stops letting them in, so the page's rule guards the route too. The carried value is a value type. For several things that change separately, declare a presence state for each. For "typing", carry when it lapses (an `InstantValue`) and read it with `ctx.stale(at = until)`. A soft lock others must respect is stored data instead: a row with an expiry, checked in the operation.

## Validation as you type

An operation that checks and changes nothing:

```kotlin
val checkTitle by operation { todos, actor, raw: String ->
    todoTitle(raw).orElse { return@operation it }
    Changes.none()
}

"/todos/check" bind POST to todo.checkTitle(workspace, actor, { signals: TodoSignals, _ -> signals.title }),
```

```html
<input data-bind:title data-on:input__debounce.300ms="@post('todos/check')">
```

Share the check (`todoTitle`) with the real operation, so the two can't disagree.

## Growing lists

"Show more" is a link. Put the window in the URL and the region's key:

```kotlin
val show = Query.int().defaulted("show", 20)
Region("feed", renderer, { request -> FeedWindow(show(request)) }, FeedApp.feed) { … }
```

```html
<a href="?show={{more}}">Show more</a>
```

For true paging, use a cursor (`?before=…`) instead.

## Watch coarse, read narrow

An operation publishes what it knows: `react` knows the channel and the message, not which windows of history are open. So the stored state is keyed by channel. A region that shows a window reads it narrowly instead: a derived state keyed by the window watches the channel's topics and asks the domain for only the messages it shows.

```kotlin
val history by derived { ctx, window: Window ->
    ctx.watch(posted.topic(window.channel), reactions.topic(window.channel), replies.topic(window.channel))
    val shown = ctx.domain.latest(window.channel, window.show)
    val ids = shown.map { it.id }
    History(shown, ctx.domain.reactions(ids), ctx.domain.replyCounts(ids))
}
```

- **`ctx.read(reactions, channel)` would read every reaction the channel ever had.** The window's read grows with the window instead.
- **Every `ctx.domain` read is paired with a `ctx.watch`.** Miss one and the test tenant fails the step that changed it.
- **A filter on a field inside the id needs the field stored on its own.** `Cell::row eq number` runs in the database; filtering the id's text in Kotlin reads the whole table.

## Smooth navigation

Every view is a URL. Make full page loads seamless with View Transitions:

```css
@view-transition { navigation: auto; }
```

## Input from another system

Another system changing what people see is an operation with a non-human actor, behind a plain route that answers with status codes:

```kotlin
"/b/{board}/builds" bind POST to CatchLensFailure().then(ServerFilters.BearerAuth(ciToken)).then { request ->
    val posted = with(BoardJson) { request.json<BuildPosted>() }
    when (val outcome = workspace.scope(request).execute(ci.report, Ci, boardId(request) to posted)) {
        is Changes -> Response(ACCEPTED)
        is Rejected -> Response(BAD_REQUEST).body(outcome.errors.values.joinToString("\n"))
        Denied -> Response(FORBIDDEN)
        Hidden -> Response(NOT_FOUND)
    }
}
```

The operation returns its `Changes`, and every page showing the board's builds updates.

## Reading from another system

A state can show what an outside service says: call its client inside the read. The helpdesk's agents list keeps only those OpenFGA says are agents of the organisation:

```kotlin
class TicketsFeature(random: Random, private val policy: OrgPolicy) : Feature<Tickets, Actor>(…) {
    val agents by derived { ctx, _: Unit ->
        ctx.watch(queue.topic())
        val acted = ctx.domain.agents()
        val here = ctx.read(online).keys
        val agents = policy.agentsAmong(ctx.tenant, acted + here)   // one OpenFGA BatchCheck on org:{tenant}
        AgentsModel(acted intersect agents, here intersect agents, …)
    }
}
```

`OrgPolicy` wraps the http4k-connect OpenFGA client, built over `AppRuntime.http`, so tests wire in `FakeOpenFga`. A `Region` over `agents` is written like any other, and the rule `anAgent` asks the same policy to guard the operations and the pages ([who may see a page](#who-may-see-a-page)).

- **`ctx.tenant` names the tenant to the service.** Code can read its tenant, never choose one.
- **There's no cache.** Every read calls the service, but a region's read is shared by everyone watching it, so calls follow redraws, not viewers. An operation's rule always decides on a fresh answer.
- **A change made in the service publishes nothing.** The region re-reads when a topic it watches is published (here `queue`), or at `ctx.stale(at = …)`; otherwise it shows the old answer. `TestTenant.read` fails a test if a state changes without either.
- **Decide what a failure means.** Here a rule fails closed (`isAgent` is false if the call fails), and a derived state throws, so the region shows its fallback and reports `RedrawFailed`.
- **Gotcha:** the call runs inside the transaction. A region's read holds a read transaction while it waits; an operation's rule holds the write lock, which on Postgres's default locking queues every other write in the tenant. Keep the call fast, and give the client a timeout.

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
object TicketStats : TenantTable("ticket_stats") {
    val status = varchar("status", 16)
    val count = integer("count")
}

tables.exposed { query { TicketStats.selectAll() } }                              // filtered by tenant
tables.exposed { insert(TicketStats) { it[status] = "open"; it[count] = 1 } }     // tenant stamped

tables.jooq { it.selectFrom(TICKET_STATS).fetch() }                              // every table needs a tenant column
```

- **Exposed** covers `TenantTable`s in reads, joins (outer ones included), subqueries, inserts, updates and deletes, but not raw SQL. It can't filter an aliased `TenantTable`, so `query { }` refuses one in its FROM or joins (self-joins included); an alias inside a subquery isn't caught. **jOOQ** covers aliases and self-joins too, and refuses an insert from a select.
- **Tenant columns are `NOT NULL`.** jOOQ keeps an outer join's unmatched rows by letting a NULL tenant through, which only padding can produce.
- **Row-level security** on Postgres covers everything, raw SQL included:

  ```sql
  ALTER TABLE ticket_stats ENABLE ROW LEVEL SECURITY;
  CREATE POLICY tenant_rows ON ticket_stats USING (tenant = current_setting('fanbelt.tenant', true));
  ```

- **Gotcha:** the app's database user must not own the table or have `BYPASSRLS`, or the policy is skipped.

## Evolving stored rows

Rows are JSON, so the data class is the schema. Adding a field with a default needs no migration. Anything else is a data migration, rolled out as expand/contract because old and new nodes run side by side:

1. **Expand:** deploy code that reads both shapes and writes the new one.
2. **Migrate:** rewrite the old rows: `UPDATE tickets SET doc = jsonb_set(doc - 'title', '{subject}', doc -> 'title') WHERE doc ? 'title'` on Postgres.
3. **Contract:** deploy code that reads only the new shape.

A row that fails to decode shows its region's fallback. Add a test that reads a row in the old shape. The same rule covers signals classes, region ids and outbox rows across a deploy.

## File uploads

Post the form as multipart, `@post('attachments', {contentType: 'form'})`, read the parts with http4k's multipart lenses in a request-only route, and pass the result to an operation. Store large files elsewhere and keep a reference in the row.

## Islands

A rich widget (a chart, a map) can be a web component inside a region, fed by attributes. When the region re-renders, the morph updates the attributes and the component redraws. Mark it `data-ignore-morph` if it owns its own children.

## Marble traces

Every change also emits a trace on `events`: `Executed(operation, actor, outcome)`, `Published(topic)`, `Redrawn(region, key, changed)` and `Patched(region, actor, sent)`, all `FanbeltTrace`, each tagged with its tenant (`eventTenant(event)`). Register `@RegisterExtension val marbles = MarbleEvents()` and pass it as the app's events (`runtime.withEvents(marbles)`); after each test it draws the traces as text and HTML into `build/reports/marbles`. Give the page's `Viewing(asFan)` the routes' actor function to name lanes by person, `marbles.alias(key, name)` to name keys, and `marbles.record { … }` to draw only those steps. A drawing reads as how the change spread: who acted, what was published, what was read again and who was sent what. To pin one down, approve its text with `approver.assertApproved(AsciiMarbles().render(name, marbles.timeline()).content)`:

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

- **Gotcha:** to move time under a real browser, pass a `MovableClock` to the `App`, plus a `MovableScheduler(clock)` for anything that uses `ctx.stale`.

## Production

- **JSON:** database storage writes rows as JSON, so register every value type the rows use in one JSON object (`object TodoJson : ConfigurableMoshi(standardConfig().withFanbeltMappings().value(TodoId).value(TodoTitle).done())`), and pass it to `PostgresDocumentStorage` and to shared presence. A missing one only fails at runtime.
- **Schema:** your migrations create one document table per `TableName` (`PostgresDocumentStorage.schema(name)`), `fanbelt_jobs` if you run jobs, `PresenceTable()` (`fanbelt_presence`) if presence is in Postgres, and an expression index for every field you filter on: `CREATE INDEX ON journal ((doc -> 'ticket'))` (`eq`, `neq` and `oneOf` compare `doc -> 'field'`; a nested field is `doc -> 'id' -> 'show'`). A text field you sort or range on needs `CREATE INDEX ON people (((doc ->> 'name') COLLATE "C"))`. Without one, every redraw scans the table. Fanbelt never creates schema.
- **Several nodes:** point every node at the same storage and the same transport. No sticky sessions.
- **Postgres behind a pooler:** `PostgresTransport` needs a direct or session-mode connection. A transaction-mode pooler (the default URL on Neon, Supabase, Heroku and others) silently delivers nothing.
- **Proxies:** every stream is pinged every `FANBELT_STREAM_KEEPALIVE` (default 2 s), which keeps it open through a proxy and notices a closed tab within about two intervals. Keep idle timeouts above 15 seconds, don't buffer `text/event-stream`, and serve browsers HTTP/2.
- **Redraws:** keep `FANBELT_REDRAW_LIMIT` (default 8) below your connection pool's size. It bounds every live re-read on a node: regions, and open pages rechecking their `Viewing` rule. `RedrawWaited` (a region) and `AccessWaited` (a page's rule) events show a node at its limit.
- **Presence leases:** a node renews its leases on a heartbeat. If it can't reach the presence backend for longer than the lease, its members drop out for everyone, and come back once it can, republished to every node.
- **A busy tenant:** on Postgres a tenant's writes queue behind one lock and run at `READ COMMITTED`. When one tenant has many independent things being written at once (channels in a big workspace), use `PostgresDocumentStorage(db, json, KeyLocking)`: an operation locks its rule's key instead, so writes to different keys run side by side, and one with no key (no rule, or a rule on the actor alone) still locks the whole tenant. Writes then run `SERIALIZABLE`, so an operation that reads beyond its key is retried rather than losing an update. The cost: clashing writes retry, and fail with a 500 once retries run out. Writes to one key wait for each other and then still retry once, because a `SERIALIZABLE` transaction sees the rows as they were before it waited, so a key written by many people at once costs two attempts per write: keep hot keys on `TenantLocking`; a scan by one tenant can make another tenant's write retry unless every filtered field has an index; and an `Oldest` cursor can skip a row written under another key that commits late.
- **Your own locking:** implement `WriteLocking` (the lock a write takes, and its isolation) and pass it as `PostgresDocumentStorage(db, json, locking)`. Locking less than the tenant needs `TRANSACTION_SERIALIZABLE`, or concurrent writes lose updates silently.
- **Past a few nodes:** use Redis for transport and presence, route requests by tenant at the load balancer, and keep every read on the primary: Fanbelt has no replica reads, because a redraw would read a replica before it has the change it was told about.
- **Health:** wrap the app in `ReportFailures(events)`, with no inner `CatchAll`. Failures and recoveries (`BackplaneDisconnected`/`BackplaneReconnected`, `PresenceRenewalFailed`/`PresenceRecovered`) arrive on `events`, for an http4k `ReadinessCheck`. Every event raised inside a scope, `OperationRefused` included, carries its tenant as metadata, read with `eventTenant(event)`, and says nothing about who acted.
- **Configuration:** each example's `main` uses one technology and reads only its keys: the box office, chat, todo and sheets run in memory; the board uses Postgres (`POSTGRES_URL`); the helpdesk, Postgres with Redis (`POSTGRES_URL`, `REDIS_URL`); the exchange, in memory (or Postgres with `POSTGRES_URL`) with Redis. Tests run every example in memory.
- **Deploys:** pages reconnect by themselves. Changing a stored row beyond adding a defaulted field is a migration: see [evolving stored rows](#evolving-stored-rows).
