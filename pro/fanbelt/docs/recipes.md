# Fanbelt: recipes

Common patterns, each built from ordinary states, operations, regions and a few lines of template. The [reference](reference.md) has the API.

## Authentication

Authenticate with http4k (OAuth, OIDC, bearer tokens, your own session cookie), and let `Authenticated` put the principal in a `RequestKey` for the page and its stream alike:

```kotlin
val principal = RequestKey.required<Principal>("principal")

val identified = Authenticated(principal, refused = { Response(SEE_OTHER).location(Uri.of("/")) }) { request -> yourSessions.find(request) }

tickets.reply(workspace, actor = { request -> Actor.Agent(principal(request).id) }, …)
```

- **Someone it can't identify gets `refused`** (a 401 by default) on a page, and a 401 on a stream. Past it, pages, views, `onConnect` and presence all read the same `principal`.
- **Put it inside `workspace.routes(…)`:** `workspace.routes(identified.then(oneOrg))`, so an unknown tenant is a 404 for everyone.
- **Machines get routes of their own** beside the people's, with no viewer: `workspace.routes(poly(BuildRoutes(…))), workspace.routes(identified.then(oneTeam))`.
- **Membership is a `Guard`:** `Guard(NOT_FOUND) { request -> request.takeIf(::allowed) }` lets a request through or refuses both the page and its stream. Chat's `ChannelMembers` does this for private channels.
- **Gotcha:** Datastar posts with cookies. Reject cross-site POSTs (an `Origin` that isn't yours) and set cookies `SameSite=Lax`.

## Authorizing from a token's claims

Verify the token in the authentication filter, and let the claims be the actor:

```kotlin
data class Claims(val subject: UserId, val tenant: TenantId, val scopes: Set<String>, val expires: Instant)

val publishableReport by rule { ctx, actor, report: ReportId ->
    ctx.staleAt(actor.expires)
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
- **A stream outlives its token.** `ctx.staleAt(actor.expires)` re-reads the rule at expiry, and a [gate region](#revoking-access-with-a-gate-region) sends the viewer away.

## Revoking access with a gate region

A filter stops new requests, but not a stream that's already open. Give the page a small region over the rule, keyed on the viewer:

```kotlin
val gate = Region("gate", renderer, { request -> Attempt(principal(request), channelName(request)) }, chat.visibleChannel) { access, _ -> GateView(access is Allowed) }
```

```html
{{#if allowed}}<div id="gate"></div>{{else}}{{{leave}}}{{/if}}
```

`leave` is `goTo(Uri.of("c/general"), id = "gate")`, from `org.http4k.fanbelt.input`. When access changes, the removed viewer is sent away.

- **Gotcha:** make the script the region's root, not a child of a `<div>`, or the emulated browser won't run it.

## Create, then go there

The operation returns the new id as its result, and the route answers with `GoTo`:

```kotlin
"/channels" bind POST to chat.create(workspace, member, { signals: ChatSignals, _ -> NewChannel(signals.name, signals.private) }, feedback = GoTo { name -> Uri.of("c/$name") })
```

```html
<div id="goto"></div>
```

Only the person who acted moves; everyone else sees the new channel appear. A route can't redirect, because Datastar posts with `fetch`. In tests, `outcome.accepted()` reads the result.

## Per-viewer, short-lived state

Cursors, "typing…": carry them in presence, which already knows when a viewer leaves.

```kotlin
val here by presence(ViewerId, CellRef::class)

"/cursor/{cell}" bind POST to SheetFeature.here.set(workspace, actor) { cellRef(it) }
```

For several things, carry a data class and set it whole. For "typing", carry when it lapses and read it with `ctx.staleAt(until)`. A soft lock others must respect is stored data instead: a row with an expiry, checked in the operation.

## Validation as you type

An operation that checks and changes nothing:

```kotlin
val checkTitle by operation { _, _, raw: String ->
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

For joins, aggregates or a table another system owns, use your own tables inside the operation's transaction. `tables.sql { }` gives the tenant and the JDBC connection, on SQLite and Postgres storage only.

```kotlin
object TicketStats : TenantTable("ticket_stats") {
    val status = varchar("status", 16)
    val count = integer("count")
}

tables.exposed { query { TicketStats.selectAll() } }                              // filtered by tenant
tables.exposed { insert(TicketStats) { it[status] = "open"; it[count] = 1 } }     // tenant stamped

tables.jooq { it.selectFrom(TICKET_STATS).fetch() }                              // every table needs a tenant column
```

- **Exposed** covers `TenantTable`s in reads, joins, inserts, updates and deletes, but not subqueries or raw SQL. **jOOQ** covers subqueries too, and refuses an insert from a select.
- **Row-level security** on Postgres covers everything, raw SQL included:

  ```sql
  ALTER TABLE ticket_stats ENABLE ROW LEVEL SECURITY;
  CREATE POLICY tenant_rows ON ticket_stats USING (tenant = current_setting('fanbelt.tenant', true));
  ```

- **Gotcha:** the app's database user must not own the table or have `BYPASSRLS`, or the policy is skipped.

## Evolving stored rows

Rows are JSON, so the data class is the schema. Adding a field with a default needs no migration. Anything else is a data migration, rolled out as expand/contract because old and new nodes run side by side:

1. **Expand:** deploy code that reads both shapes and writes the new one.
2. **Migrate:** rewrite the old rows: `UPDATE tickets SET doc = jsonb_set(doc - 'title', '{subject}', doc -> 'title') WHERE doc ? 'title'` on Postgres, or `json_set` and `json_remove` on SQLite.
3. **Contract:** deploy code that reads only the new shape.

A row that fails to decode shows its region's fallback. Add a test that reads a row in the old shape. The same rule covers signals classes, region ids and outbox rows across a deploy.

## File uploads

Post the form as multipart, `@post('attachments', {contentType: 'form'})`, read the parts with http4k's multipart lenses in a request-only route, and pass the result to an operation. Store large files elsewhere and keep a reference in the row.

## Islands

A rich widget (a chart, a map) can be a web component inside a region, fed by attributes. When the region re-renders, the morph updates the attributes and the component redraws. Mark it `data-ignore-morph` if it owns its own children.
