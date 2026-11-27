# Building live apps with Fanbelt: a step-by-step guide

This guide builds one app, a box office, from plain Kotlin values up to a live, multi-user page, one concept at a time. Each part adds one ring around the last: the domain, the feature, the web, and the app that runs it.

**Who it's for.** You know Kotlin and the web (HTTP, forms, cookies). You don't need to know http4k or Datastar, the small browser library Fanbelt uses: the web part introduces just enough of both.

**The example.** Fans reserve seats for a show. Everyone watching sees a seat go. When two fans click the same seat, one wins. A reservation lapses after five minutes, and checking out takes payment in the background. Every snippet is from [examples/boxoffice](../examples/boxoffice). The API of every concept is in the [reference](reference.md), and common patterns are in the [recipes](recipes.md).

**See it first.** Run `./gradlew :http4k-fanbelt-examples-boxoffice:run`, open `http://localhost:9000/` and pick a show. Open the same address in a second browser, or in a private window so it's a second fan, and pick the same show. Reserve a seat in one and watch it go in the other.

---

## The problem

Live, multi-user UIs usually get built twice. The browser has a state store, a cache, optimistic updates, an API client and a socket protocol to hear about other people's changes. The server has the database, an API shaped for the client, and push events. Each event carries enough data for the client to patch its copy. That's two copies of the state, kept in sync by hand.

Classic server rendering keeps one copy, in the database, and sends HTML. It's correct, but static: you see other people's changes when you refresh.

Fanbelt keeps the single copy and adds liveness: every screen updates when anything it shows changes, for everyone looking at it, with no second model in the browser.

## The idea

**A page is drawn once, then kept live.** The first paint is a GET:

```
browser ───────────▶ page ──────────────▶ regions ───────────▶ browser
opens the            checks who is        each reads its       shows the whole
show's URL           viewing              state, in one        page, as plain
                                          transaction          HTML
```

That's plain server rendering: one copy of the state, read and rendered as HTML. Then the browser asks the same URL for a stream, which adds the liveness:

```
browser ───────────▶ stream ────────────▶ regions ───────────▶ quiet
Datastar asks the    one connection;      listen for the       nothing is sent
same URL for a       sends what           topics their         until one is
stream               changed since        reads touched        published
                     the paint
```

**Then every change is this loop:**

```
browser ───────────▶ route ─────────────▶ operation ─────────▶ bus
a click posts        a POST, bound        changes the          after commit, a
                     to one operation     database, says       topic: the name
                                          which states         of what changed
                                          changed
                                                                   │
                                                                   ▼
browser ◀─────────── stream ◀──────────── region ◀──────────── subscribed
the new HTML         the page's one       a live part of a     every region
is swapped in        open connection      page: re-reads,      showing that
                                          re-renders           state
```

A POST never answers with new UI. It runs an **operation**, which writes to storage and says *which states it changed*. Every open page showing one of those states is subscribed to it. Each re-reads the state, re-renders that part of itself (a **region**), and sends the HTML down the page's one stream. You still write the domain, the views and the templates. The loop asks two things of that code: an operation names what changed, and a region reads through states, so its reads record what to listen for.

### The principles

The server owns the truth. A change says *what* changed and never *how*. Liveness comes from what a view read. Everything you can see has an address. The [theory](theory.md) states them as rules to argue with.

### The rings

```
the domain       plain Kotlin, no web                            step
  values           every id and name is a type                     1
  rows, tables     typed access to the database                    2
  domain class     your logic over your rows                       3
the feature      what people can see and do, still no web
  feature, actor   one area of the app, and who acts in it         4
  states           stored, derived, and who is here              5–7
  rules            who may act, and what they get                  8
  operations       one block, in a transaction                     9
  TestTenant       drives it, with no HTTP                        10
the web          http4k on the server, Datastar in the browser    11
  routes           an operation bound to a POST                   12
  regions, pages   live parts of a page, over one stream       13–14
  viewers          who is watching, and what is theirs         15–16
  liveness         what happens on a click, end to end            17
the app          runtime and infrastructure in, a node out     18–21
```

Each ring depends only on the rings inside it: the domain knows nothing of features, and a feature knows nothing of the web.

## Tenants

**Every request belongs to a tenant, and your code can't choose another.** A tenant is whatever your product calls its separate spaces; here, a theatre.

- **It's in the URL.** `/theatres/demo/…` belongs to `demo`. A theatre that doesn't exist is a 404, for every page, route and stream.
- **It has its own storage.** A feature is handed storage for one tenant, so the domain never names one.
- **Its writes don't interleave.** When two writes in one tenant overlap, the second sees what the first did.

Until step 12, where a route finds the tenant, everything runs inside one.

## Setting up

Fanbelt is split into modules the way http4k is: a technology-free `core`, one module per technology, and a test kit. The [README](../README.md) lists them all. The box office runs in memory, so its build is:

```kotlin
dependencies {
    implementation(project(":http4k-fanbelt"))
    implementation(project(":http4k-server-jetty"))
    implementation(project(":http4k-compression"))
    testImplementation(project(":http4k-fanbelt-testing"))
}
```

---

# Part 1: the domain

Start at the centre: the values, the rows and your logic. None of it depends on Fanbelt's web side, and most of it doesn't depend on Fanbelt at all.

## Step 1. Values

**Every id and name is a type.** A value type validates its own shape, so "12F" can never become a `SeatRef`, and nothing downstream handles raw text. Fanbelt uses [values4k](https://github.com/fork-handles/forkhandles/tree/trunk/values4k):

```kotlin
class SeatRef private constructor(value: String) : StringValue(value) {
    val row get() = value.first()
    val number get() = value.drop(1).toInt()

    companion object : StringValueFactory<SeatRef>(::SeatRef, { it.matches(Regex("[A-Z][0-9]{1,2}")) })
}

class ShowId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<ShowId>(::ShowId)
}

class ShowTitle private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<ShowTitle, String>(::ShowTitle, (1..80).length.and { it.isNotBlank() }, String::trim)
}

class ShowSeat private constructor(value: String) : StringValue(value) {
    val show get() = ShowId.parse(value.substringBefore(':'))
    val seat get() = SeatRef.of(value.substringAfter(':'))

    companion object : StringValueFactory<ShowSeat>(::ShowSeat, { it.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:[A-Z][0-9]{1,2}")) })
}

fun ShowSeat(show: ShowId, seat: SeatRef) = ShowSeat.of("$show:$seat")
```

A `SeatRef` only says that "F12" is well formed. Whether a show *has* an F12 is the domain's question (step 9). `ShowSeat` is the seat at a show, a value type built from the two so it can be a row's id, and most of the box office works on it.

**You have:** a vocabulary that can't hold an invalid value.

## Step 2. Rows and tables

**Rows are data classes; tables are typed handles to them.** A row knows its id:

```kotlin
data class Layout(val rows: Int, val seatsPerRow: Int) {
    val seats get() = ('A'..'Z').take(rows).flatMap { row -> (1..seatsPerRow).map { SeatRef.of("$row$it") } }

    operator fun contains(seat: SeatRef) = seat.row < 'A' + rows && seat.number in 1..seatsPerRow
}

data class Show(override val id: ShowId, val title: ShowTitle, val layout: Layout) : Identified<ShowId>

data class Booking(override val id: ShowSeat, val fan: ViewerId, val expiresAt: Instant, val order: OrderId? = null, val show: ShowId = id.show) : Identified<ShowSeat> {
    fun holdsSeatAt(now: Instant) = order != null || now < expiresAt
}
```

A booking is identified by the seat at the show, a `ShowSeat` value type, and keeps its `show` beside it to filter on. It's reserved by a fan until it expires, or sold once it has an order.

You don't write SQL. Fanbelt stores every row as a JSON document, and you name the tables in a **tables object**:

```kotlin
class BoxOfficeTables(tables: Tables) {
    val shows = tables<ShowId, Show>(SHOWS)
    val bookings = tables<ShowSeat, Booking>(BOOKINGS)
    // orders, and the theatre's own row …

    companion object {
        val SHOWS = TableName.of("shows")
        val BOOKINGS = TableName.of("bookings")
        // …
    }
}
```

The `tables` handed in are locked to one theatre: they can't see another's rows, so your code never names one.

A `Table<ID, T>` has `get`, `insert`, `update`, `delete`, `count`, and one query, `page`:

```kotlin
tables.bookings.page(Booking::show eq show).items   // the bookings for one show
```

The filter runs in the database; `eq` and the others are in the [reference](reference.md#reading-page). A page with a limit comes with a cursor for the next. When documents don't fit, say for joins or aggregates, use your own SQL tables in the same transaction: see [Recipe: your own SQL tables](recipes.md#your-own-sql-tables).

**You have:** typed access to storage.

## Step 3. The domain class

**The domain class is your logic over your rows.** It's built for each transaction, from that transaction's tables:

```kotlin
class BoxOffice(private val tables: BoxOfficeTables, private val clock: Clock) {
    fun shows() = tables.shows.page().items

    fun show(id: ShowId) = tables.shows.get(id)

    fun bookings(show: ShowId) = tables.bookings.page(Booking::show eq show).items

    fun stillHeld(bookings: List<Booking>) = bookings.filter { it.holdsSeatAt(clock.instant()) }

    fun booking(pick: ShowSeat) = tables.bookings.get(pick)?.takeIf { it.holdsSeatAt(clock.instant()) }

    fun reserve(pick: ShowSeat, fan: ViewerId) {
        val booking = Booking(pick, fan, clock.instant() + RESERVED_FOR)
        if (tables.bookings.get(booking.id) == null) tables.bookings.insert(booking) else tables.bookings.update(booking.id) { booking }
    }

    fun release(booking: Booking) = tables.bookings.delete(booking.id)
}
```

Three rules:

- **No HTTP, no permissions, no publishing.** It works on rows.
- **Time and randomness are injected.** It takes a `Clock` (and a `Random` when it makes ids, with `ShowId.random(random)`), never `Clock.systemUTC()` or `UUID.randomUUID()`, so tests can move time and repeat themselves.
- **A transaction may run more than once,** so there are no outside calls with side effects here. Payment is taken by a job (step 19).

**You have:** the business rules, in a plain class.

---

# Part 2: the feature

One ring out: what people can see and do with that domain. There's still no web, and everything here runs in a test with no server.

## Step 4. The feature, and who acts

**A feature is one area of the app: what people can see and do.** It owns its domain, built for each transaction, and declares who acts:

```kotlin
sealed interface Actor {
    data class Fan(val id: ViewerId) : Actor
    data object System : Actor
}

class BoxOfficeFeature(clock: Clock) : Feature<BoxOffice, Actor>({ tables -> BoxOffice(BoxOfficeTables(tables), clock) }) {
    // states, rules and operations: steps 5 to 9
}
```

- **The function builds the domain for each transaction.** The feature is a class because its domain needs a `Clock`, which the app hands it. A feature whose domain needs nothing can be an `object`.
- **The actor is whoever is acting, and not every actor is a person.** A fan reserves seats; the job that takes payment acts as `System`. A feature with nobody to tell apart (the [todo example](../examples/todo)) uses `Unit`, and then nothing passes an actor at all.
- **An app can have several, which build on each other one way.** Orders turns reservations into paid tickets: it's given the box office and reads its states, and the box office doesn't know it exists. Features share the tenant's tables, so checkout writes the order and marks its seats sold in one transaction.

**You have:** an empty feature, ready for what people see, who may act, and what they do.

## Step 5. States

**A state is a named read, and its name is its topic.** It's what part of a page shows:

```kotlin
val shows by state { boxOffice -> boxOffice.shows() }
val bookings by state { boxOffice, id: ShowId -> boxOffice.bookings(id) }
```

- **It's named after its property.** `shows.topic()` is the topic "shows": the name used to announce that this state changed, and to listen for that.
- **Most states are about one thing, so they take a key.** `bookings.topic(id)` is "bookings:<id>", so the bookings for show A and show B are different topics, and a reservation on one redraws only that one. A stored state's key is a value type, because it's part of the topic.

These are **stored** states, read from the domain. The other two kinds, derived and presence, come next, and that's all.

**You have:** what people can see, by name.

## Step 6. Derived states

**A derived state is built from other states, in one snapshot.** The seat map needs the show's layout and the bookings still held:

```kotlin
val seatMap by derived { ctx, id: ShowId ->
    val held = ctx.domain.stillHeld(ctx.read(bookings, id))
    held.filter { it.order == null }.forEach { ctx.stale(at = it.expiresAt) }
    val holders = held.associate { it.id.seat to it.fan }
    val show = ctx.domain.show(id)
    if (show == null) null else SeatMap(show, show.layout.seats.map { seat -> SeatView(seat, holders[seat]) })
}
```

- **`ctx.read(bookings, id)` reads another state, and takes on its topic:** when the bookings change, so does the seat map. It can read another feature's states too: orders' basket reads the box office's bookings.
- **`ctx.stale` says when it goes stale by the clock.** Nobody acts when a reservation lapses; time just passes. The seat map is read again at the earliest of those moments, with nothing published, and an unchanged model sends nothing.
- **All the reads happen in one transaction,** so the model is a consistent snapshot.
- **`ctx.domain` is an escape hatch** for reads that aren't states, and nothing tracks it. Pair it with a `read`, or `ctx.watch(topic)`, of whatever changes it. Here the show itself never changes, so nothing needs to.
- **Absence is a value.** With no such show, the seat map is `null`, and the page shows that (step 13).

**You have:** composite views that can't drift from their sources.

## Step 7. Presence

**Presence is who's here, while their page is open.** It's the third kind of state:

```kotlin
val watching by presence(ShowId, ViewerId)
```

- **Read it like any other state.** `ctx.read(watching, show)` is each viewer and the value they carry, if any, and its topic says when it changed.
- **It isn't an operation:** nothing to validate and no transaction. It's a lease, held while someone's page is open, and joining and leaving publish like any change. A page joins it as it connects (step 15).
- **A member can carry a value,** such as a cursor ([per-viewer, short-lived state](recipes.md#per-viewer-short-lived-state)).

The show's header, how many seats are left and who is watching, is a derived state over the seat map and presence:

```kotlin
val header by derived { ctx, id: ShowId ->
    val map = ctx.read(seatMap, id)
    if (map == null) null else Header(map.show.title, map.seats.count { it.reservedBy == null }, ctx.read(watching, id).keys)
}
```

**You have:** what people can see: what's stored, what's built from it, and who's here.

## Step 8. Rules: who may act

**A rule decides who may do something, and hands down what it found.** It's given a `ctx`, the actor and a key, and answers `Allowed(found)`, or refuses:

```kotlin
val fan by rule { ctx, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }

val yourBooking by rule { ctx, actor, pick: ShowSeat ->
    ctx.read(fan, ActorKey(actor, Unit)).flatMap { fan ->
        val booking = ctx.domain.booking(pick)
        when {
            booking == null -> Rejected(seatField, "You have no reservation of ${pick.seat}")
            booking.fan != fan.id -> Denied
            else -> Allowed(booking)
        }
    }
}
```

(`seatField` is `FieldName.of("seat")`, the field the page shows an error against.)

- **It's named for what it hands down:** `fan` finds an `Actor.Fan`, and `yourBooking` finds the booking, so whatever it guards needn't load it again.
- **Rules build on rules.** `yourBooking` starts from `fan`, and `flatMap` carries on only from `Allowed`, so anyone who isn't a fan stops there.
- **There are three ways to refuse.** `Rejected` carries a reason the page shows, such as having no reservation of that seat. `Denied` means this actor may not, and there's nothing to show them: the page never offers them that button. `Hidden` is a 404, for something they can't know about, so someone else's private thing and a missing thing look alike.
- **Rules are the security boundary.** Hiding a button is a courtesy, not a defence: anyone can send the POST by hand. Every operation, page and route runs its rule on the server, so refuse in the rule and let the page follow.
- **A rule guards whatever names it:** an operation (next), a page (step 14) and a route that only reads (step 12), so they can't disagree. Roles are just conditions on the actor and the data: see [Recipe: who may see a page](recipes.md#who-may-see-a-page).

**You have:** who may act, decided in one place.

## Step 9. Operations: one block, in a transaction

**An operation is a change.** It names its rule, and is given the domain, the actor, a typed input and what the rule found:

```kotlin
val reserve by operation(fan) { boxOffice, actor, pick: ShowSeat, fan ->
    val show = boxOffice.show(pick.show) ?: return@operation Rejected(seatField, "No such show")
    if (pick.seat !in show.layout) return@operation Rejected(seatField, "No such seat at this show")
    val reservedBy = boxOffice.booking(pick)?.fan
    if (reservedBy != null && reservedBy != fan.id) return@operation Rejected(seatField, "Someone just took ${pick.seat}")
    boxOffice.reserve(pick, fan.id)
    Changes.of(bookings.topic(pick.show))
}
```

- **If `fan` refuses, the block never runs,** and the refusal is the operation's answer. `release` names `yourBooking` instead, and is handed the booking. `operation { … }` with no rule is open to anyone.
- **It ends in one of four ways:** `Changes`, `Rejected`, `Denied` or `Hidden`.
- **It runs in one transaction with its rule,** so nothing can change between the check and the write. It may run more than once, so it makes no outside calls with side effects; a job makes those (step 19).

### Changes: what changed, not how

`Changes.of(topics…)` says it's done and names what changed; `Changes.none()` changes nothing visible.

- **A topic carries no data.** Whoever hears it re-reads the state. Duplicates and reordering do no harm, so there's nothing to version or merge.
- **It's published after commit.** A refusal rolls back, and nothing is published.
- **Only an operation's `Changes` publish.** The domain and the HTTP layer never do.

An operation can also hand a result to its caller, such as the id of what it made: see [Recipe: create, then go there](recipes.md#create-then-go-there).

### Two fans, one seat

Ann and Bob click F12 at the same moment. A tenant's writes don't interleave, so whichever runs second sees the first reservation, and is told "Someone just took F12".

Input that arrives as text is checked the same way: `FieldName.check` turns it into a value type, or a `Rejected` on that field ([reference](reference.md#operations)).

**You have:** what people can do, with refusals they can understand.

## Step 10. Test it, with no HTTP

**A test tenant is the app, minus HTTP.** The feature has no web dependency, so the `testing` module's `TestTenant` drives it directly:

```kotlin
val tenant = TestTenant()
val boxOffice = BoxOfficeFeature(tenant.clock)

tenant {
    boxOffice.reserve.given(Actor.Fan(bob), ShowSeat(show, a1))      // setup: must be accepted, returns its result
    boxOffice.watching.join(bob, show)                               // Bob opens the page
    boxOffice.reserve.execute(Actor.Fan(ann), ShowSeat(show, f12))   // Accepted, Rejected(reasons), Denied or Hidden
    boxOffice.seatMap.read(show)                                     // read as a page watching it would
    tick(Duration.ofMinutes(5))                                      // the clock moves; the reservation lapses
}
```

- **Real:** the storage, bus and presence a node runs, in memory, for one tenant, and as many features as you give it.
- **Controlled:** the clock, randomness and outbound HTTP (`TestTenant(TestRuntime(http = …))`). Jobs you give it run when an operation names them, and again as the clock moves.
- **Watching:** a `read` inside the tenant remembers each state's value and the topics it read. If the value changes and none of those topics was published, the next read fails.

The likeliest bug in this design is a change that doesn't say what changed, and this catches it in milliseconds. Step 20 runs the same scenarios through a browser.

**You have:** fast tests of every behaviour, including "everyone watching sees it".

---

# Part 3: the web

Out of process: two new things, the browser and HTTP.

## Step 11. Just enough http4k and Datastar

**http4k is the server side.** A server is a function from `Request` to `Response` (an `HttpHandler`), and `routes` composes them:

```kotlin
routes("/shows/{show}" bind GET to { request: Request -> Response(OK).body("…") })
```

Everything Fanbelt gives you to mount is one of those functions. A `Filter` wraps a handler (`filter.then(handler)`). A **lens** reads a typed value from a request: `Path.value(ShowId).of("show")` is a `(Request) -> ShowId`, and answers 400 if it doesn't parse. A `PolyHandler` (`poly(…)`) serves HTTP and server-sent events (SSE) together. Templates are Handlebars, given a `ViewModel` data class.

**[Datastar](https://data-star.dev) is the browser side:** a small script that gives HTML attributes behaviour. An attribute posts to the server, and the response is HTML to swap in, or a signal.

**Signals** are the browser's small piece of state: the text in a field, an error message. In attributes, `$errors.seat` reads one and `$title = 'x'` writes one. In Fanbelt you never declare them yourself; the page renders them from a Kotlin class (step 14).

| Attribute | Meaning |
|---|---|
| `data-on:click="…"`, `data-on:submit`, `data-on:change` | Runs the expression on that event. `evt` is the event. |
| `data-bind:title` | Two-way binds an input's value to `$title`. |
| `data-show="$errors.seat"` | Shows the element only while the expression is truthy. |
| `data-text="$errors.seat"` | Sets the element's text. |
| `data-init="…"` | Runs the expression when the element appears. |

**Actions** talk to the server: `@post('…')` sends every signal as JSON, and `@get(url)` makes a GET. A response patches **signals**, or **elements**: HTML that Datastar matches to the page *by id* and morphs in, changing only what differs. An action whose response is an event stream keeps it open and applies each patch as it arrives. That's how a page stays live.

That's all of either that this guide needs.

## Step 12. Routes

**An operation is bound to a POST route by invoking it** with the workspace, the actor and where its input comes from:

```kotlin
val workspace = Workspace(infra, "/theatres/{theatre}", theatres::exists)
val asFan = { request: Request -> Actor.Fan(principal(request).id) }
val showId = Path.value(ShowId).of("show")
val seat = Path.value(SeatRef).of("seat")

"/shows/{show}/seats/{seat}/reserve" bind POST to boxOffice.reserve(workspace, asFan) { ShowSeat(showId(it), seat(it)) },
"/shows/{show}/seats/{seat}/release" bind POST to boxOffice.release(workspace, asFan) { ShowSeat(showId(it), seat(it)) },
```

```html
<button id="seat-{{seat}}" class="seat" data-on:click="@post('shows/{{../../show}}/seats/{{seat}}/reserve')">{{number}}</button>
```

- **The workspace finds the tenant** from the URL, and the operation runs in that tenant. `theatres::exists` says which theatres there are.
- **The actor is a function of the request.** The request says who the *user* is (here, an anonymous cookie the app sets on its home page); this function says which *actor* they act as. A real app authenticates with http4k: see [Recipe: authentication](recipes.md#authentication).
- **The input is any `(Request) -> Input`:** path lenses here, so "12F" is a 400 before the operation runs. It can also come from the signals Datastar posts, as the [todo example](../examples/todo)'s routes do.

What the route answers:

| Outcome | Answer |
|---|---|
| Changes | A signals patch clearing any error the page was showing. |
| Rejected | A signals patch setting `errors.seat`, to this viewer only. The page shows it: `<p class="error" data-show="$errors.seat" data-text="$errors.seat"></p>`. |
| Denied | 403, and nothing is shown. |
| Hidden | 404, and nothing is shown. |

The route never answers with new UI and never publishes. The page changes because regions re-render, for everyone at once, the person who clicked included. The operation is the behaviour, and step 10 ran it with no route at all.

**A route that only reads binds a rule the same way,** for something that isn't live, such as the features example's CSV download. It's handed what the rule found, and a refusal answers 403 or 404.

**You have:** a way for the browser to act.

## Step 13. Regions

**A region is a live part of a page, showing one state.**

```kotlin
data class ShowsView(val shows: List<Show>) : ViewModel

val renderer = HandlebarsTemplates().CachingClasspath()
val shows = Region("shows", renderer, boxOffice.shows) { shows -> ShowsView(shows) }
```

```html
<div id="shows">
  <ul>{{#each shows}}<li><a href="shows/{{id}}">{{title}}</a></li>{{/each}}</ul>
</div>
```

- **Model: read once.** The state's value, shared by everyone watching. If it's equal to the last one, nothing is sent, which is why models are data classes.
- **View: per viewer.** `(model, key) -> ViewModel`, or `(model, viewer, key)` when the look differs per viewer, where the viewer is whoever the page admitted. It reads those and nothing else, not even the request. A state with no key drops the key: `(model)` or `(model, viewer)`.
- **Template: the view as HTML.** http4k finds it by the view model's name, so `ShowsView` renders `ShowsView.hbs`. Its root element carries the region's id; that's how Datastar knows where to morph the next version in.

### Keys: which view

The seat map is one per show, so its region takes the key from the request:

```kotlin
val seats = Region("seats", renderer, showId, boxOffice.seatMap, fallback = ShowGoneView) { map, _ ->
    if (map == null) return@Region ShowGoneView
    SeatMapView(map.show.id, map.seats.groupBy { it.seat.row }.map { (row, seats) -> SeatRow(row, …) })
}
```

- **The key is *which view*; the topic is *what changed*.** `showId` is the lens the routes use. Everyone watching one show shares one model, and a reservation on one show redraws that show's seat map and no other.
- **If the server renders from it, it's in the URL.** Changing what you see is following a link, and the new page is live. Filters, page sizes and paging cursors belong in the key, read from the URL; only cosmetic state, such as whether a menu is open, stays in the browser.
- **The topic stays coarse,** so operations never need to know which views are open. A filtered view reads the coarse state and filters it; when nothing it shows changed, nothing is sent.
- **A filtered view must read something that changes when its *membership* does.** "My tickets" built from each ticket's own topic misses one newly assigned to you, because it never read it. Read the coarse state, or publish a topic for the set.

### Absence is a value

- **A missing show is a `null` model,** and the view renders a "gone" view that carries the region's id too. A show removed while someone watches is just another change.
- **Arriving at a missing one is a 404,** when the region is the page's `subject` (step 15).
- **A read or render that throws** shows the region's `fallback` and reports `RedrawFailed`. The rest of the page carries on.

A region is re-read on every change it watches, so read only what's on screen: see [Recipe: growing lists](recipes.md#growing-lists).

**You have:** one state, rendered, for each thing it's about.

## Step 14. Pages

**A page is one route for the first render and the stream.**

```kotlin
data class TheatrePageView(val shell: PageShell) : ViewModel

val layout = Layout(listOf(shows), renderer) { shell -> TheatrePageView(shell) }
val theatrePage = Page(workspace, layout, Viewing(asFan, boxOffice.fan))

"/" bind theatrePage
```

- **A `Layout` is what the page draws:** its regions, how to render them, and a view of the whole page, built from the `PageShell`.
- **A `Viewing` is who may view it:** how to get the actor from the request, and the rule they must pass. A viewer it refuses gets 403, or 404 if `Hidden`. They're sent to `/` on arrival, or as soon as the rule stops allowing them.
- **The browser loads the page, then asks the same URL for a stream:** every region, live, over one connection. (http4k picks between the two by the request's `Accept` header.)

The page template is given the `PageShell`:

```html
<head>
  <base href="{{shell.base}}">
  <script type="module" src="https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.4/bundles/datastar.js"></script>
</head>
<body data-init="{{shell.connect}}">
  <h1>What's on</h1>
  {{{shell.regions.shows}}}
</body>
```

- **`shell.regions.shows`** is the region's first render; the triple braces stop Handlebars escaping it.
- **`shell.connect`** opens the stream when the page loads, and reopens it whenever it drops, deploys included.
- **`shell.base`** is the tenant's root, such as `/theatres/demo/`. Every URL in a template is relative to it (`shows/{{id}}`), so templates never mention tenants.
- **`shell.signals`**, put in `data-signals`, declares the page's signals from its signals class: each field with its default, plus an `errors` entry for each.

**You have:** a page that keeps itself up to date, for every viewer.

## Step 15. Who's watching

**A page can join presence as it connects.** The show page joins `watching`:

```kotlin
val showPage = Page(
    workspace,
    Layout(listOf(header, seats, basket), renderer, subject = seats, signals = ShowSignals()) { shell, (_, show, fan) -> ShowPageView(shell, show, fan.id) },
    Viewing(asFan, showId, boxOffice.fan) { scope, (_, show, fan) -> boxOffice.watching.join(scope, fan.id, show) },
)
```

- **The `Viewing` names the page's key, `showId`, alongside the actor.** Both are worked out from the request once, and handed on with what the rule found, here the fan, as an `Admitted(actor, key, found)`.
- **`Viewing` can take a lambda, run when someone's page connects.** It's given the tenant's `scope` and who was admitted.
- **What it returns is closed when the page disconnects.** `join` returns the lease, so leaving the page leaves the show.
- **The view is given the same `Admitted`,** so the page knows whose seats are whose (next step).
- **A page mixes regions from any features** over its one stream: the header and seat map from the box office, and the basket from orders.
- **`subject = seats`** makes the page a 404 if the seat map is absent at first paint. It still renders, so one set of templates covers both.

Tests join without a page, as step 10 did.

## Step 16. Per-viewer detail

**Share the model; vary the view.** Your seats are amber, but the seat map is rendered once and shared. Each seat says who reserved it, and one CSS rule on each viewer's page colours theirs:

```html
<!-- shared seat map: SeatButton(seat, number, reservedBy) -->
<button id="seat-E7" data-reservedBy="{{reservedBy}}">7</button>

<!-- each viewer's page: ShowPageView(shell, show, viewer) -->
<style>[data-reservedBy="{{viewer}}"] { background: var(--mine) }</style>
```

Before varying the view, ask whether the browser already knows the difference.

**When what is read differs per viewer, key the region on them too.** "Reserved for you" is keyed on the show and the fan:

```kotlin
val basket = Region("basket", renderer, { request -> Shopper(showId(request), principal(request).id) }, orders.basket) { basket, shopper ->
    BasketView(shopper.show, basket.reserved, …)
}
```

Those renders aren't shared, so keep it for when the data really differs. The [features example](../examples/features)'s grid pushes sharing furthest: a region per row, your own cursor kept in a browser signal, and everyone else's in one shared `<style>` region.

## Step 17. How a page stays live

**A region re-renders when something it read changes.** Each read records the topics it touched, and the region subscribes to them. When one is published, the region re-reads once, renders for each viewer, and sends HTML only to the screens where it changed. There's no subscription list to maintain.

### Bob opens the show: every concept, in order

1. **Open.** Bob's browser asks for `/theatres/demo/shows/<show>`.
2. **Read.** In one transaction, the page checks Bob may view it, and each region reads its state. The seat map reads this show's bookings, and so records the topic `bookings:<show>`.
3. **Paint.** The page's template renders every region, and Bob sees the seats.
4. **Connect.** Datastar asks the same URL for an event stream: one connection for the whole page. Bob joins `watching`, and every header on the show counts him.
5. **Catch up.** The page carries a digest of each region it painted, so a region that changed since is sent now, and one that didn't sends nothing.
6. **Listen.** The seat map now listens for `bookings:<show>`. Nothing more is sent to Bob until it's published.

### Ann reserves F12: every concept, in order

1. **Click.** Datastar posts to `shows/<show>/seats/F12/reserve`; the page's `<base>` makes that `/theatres/demo/shows/<show>/seats/F12/reserve`.
2. **Parse.** The lenses turn the path into a `ShowSeat`, and the cookie becomes `Actor.Fan(ann)`.
3. **Decide.** In one transaction, `reserve` finds F12 free, and Ann reserves it.
4. **Announce.** After commit, `bookings:<show>` is published.
5. **Answer.** Ann's POST gets a signals patch clearing any error. No HTML.
6. **Re-read.** Every region on this show that read its bookings re-reads, once, however many people are watching.
7. **Patch.** Each screen whose HTML changed, Ann's and Bob's alike, gets the new region down its stream, and Datastar morphs it into place.

**You have:** every concept on the show page.

---

# Part 4: the app

The outer ring: what the features run on, the work they leave for later, and the node that serves it all.

## Step 18. The app

**Runtime and infrastructure in, a node out.** One function wires everything:

```kotlin
fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra): Node {
    val infra = infrastructure(runtime)
    val theatres = Theatres(infra.storage)
    val boxOffice = BoxOfficeFeature(runtime.clock)
    val orders = OrdersFeature(boxOffice, runtime)
    val workspace = Workspace(infra, "/theatres/{theatre}", theatres::exists)
    val renderer = HandlebarsTemplates().CachingClasspath()

    val oneTheatre = poly(
        listOf(BoxOfficeRoutes(boxOffice, orders, workspace)),
        "/shows/{show}" bind ShowPage(workspace, boxOffice, orders, renderer),
        "/" bind TheatrePage(workspace, boxOffice, renderer),
    )
    val app = …   // http4k filters around workspace.routes(identified.then(oneTheatre))

    return Node(app, runtime, infra, Jobs(infra, orders.charge))
}
```

- **`AppRuntime`** is everything with side effects: the clock, randomness, the scheduler, the events sink and outbound HTTP. Tests pass a `TestRuntime`.
- **`AppInfra`** is storage, the bus and presence, shared by every feature; each defaults to in memory.
- **`workspace.routes(…)`** mounts a theatre's pages and routes under `/theatres/{theatre}`. The app owns its theatres: each gets a row of its own when it's created, which `theatres::exists` looks for. `App` creates none.
- **`Node`** is the handler, plus everything with background work (the bus, presence, `Jobs`), in start order. `node.asServer(…).start()` starts them, then serves; `stop()` reverses it.
- **The wrapping is yours.** Compression, an event per request, the viewer cookie, auth and health checks are ordinary http4k filters around it.

`main` picks the runtime, the infrastructure and the starting data: here, storage in memory with one theatre, and a fake payment provider over HTTP. Tests pass test ones.

### Many nodes

**Swap in a shared bus and presence.** To run several nodes behind any load balancer, with no sticky sessions, `main` gives `AppInfra` a shared transport and presence backend. The features, the pages and `App` don't change. The [features example](../examples/features)'s `runDistributed` keeps storage in Postgres, with Redis for the bus and presence:

```kotlin
val storage = PostgresDocumentStorage(database(), FeaturesJson)
App(runtime, { runtime ->
    AppInfra(runtime, storage, RedisTransport(redis, runtime.events), RedisPresenceBackend(redis, runtime.clock), ENV)
}, statusService = statusService.uri())
```

- **The bus becomes a backplane.** Each node tells the others which topics changed: names only, no data. If a message is lost, it's caught up: every view re-reads every five minutes (`FANBELT_BACKPLANE_HEAL`), and sends nothing if nothing changed.
- **Presence leases live in shared storage,** so a dead node's viewers expire.
- **Each comes from its technology's module** (`postgres`, `redis`, `exposed`), imported only by `main`. Postgres can carry the bus and presence too: `PostgresTransport` and `ExposedPresenceBackend`.

**You have:** a runnable app: `./gradlew :http4k-fanbelt-examples-boxoffice:run`.

## Step 19. Jobs

**A job is work on one key, done outside any transaction.** Payment is the box office's job: checkout records a `Pending` order, and a job charges the card for that order and records what happened.

```kotlin
val order by state { orders, id: OrderId -> orders.order(id) }

val charge by job(OrderId) { scope, id ->
    scope.read(order, id)?.takeIf { it.status == OrderStatus.Pending }?.let { take(scope, it) }
}
```

- **It's declared per key,** a value type given by its factory, here `job(OrderId)`. Its body gets the tenant's `scope` and the key, and reads the current order itself: the job has nothing to do unless it's still `Pending`.
- **An operation that makes work names the key,** with `charge.topic(order.id)` in its `Changes`, as checkout does. That records the key in the tenant's `fanbelt_jobs` table, in checkout's own transaction, and the job runs at once.
- **It records what happened through operations, as `System`,** so pages update like any other change. `take` executes `paid` or `declined`, or throws when the provider fails.
- **It's done when the body returns,** and the key's row is deleted. A body that throws is reported as `JobFailed`, and runs again a second later. The box office retries forever; an app that wants to give up records its attempts in its domain and stops.
- **Work survives a restart.** As a node starts, and once per heal, `Jobs` runs every recorded key.

**Every node hears the wake, and one runs the key.** Each run takes a lock on the job, tenant and key. A node that can't take it leaves the key to the node that has it. Postgres storage takes the lock in Postgres, so it holds across nodes.

**So a charge can happen twice,** when a node charges and dies before the row is deleted. Make the outside call idempotent: here the provider is sent the order's id. And read the current state first: a body can run on an order that's already paid.

**The lock keeps other runners away, not other writers.** Operations never take it, so a fan can change an order while its charge is with the provider, and the operation that records the result decides from the order as it is then. When the app should show that a charge is under way, it records that as a fact ([showing work in progress](recipes.md#showing-work-in-progress)).

Add `Jobs(infra, orders.charge)` to the `Node`. A worker process can run it alone, with the same storage and bus, while the web nodes leave it out. On SQL storage, create the `fanbelt_jobs` table in your migrations. In tests, `tenant.runs(orders.charge)` runs a named job before `execute` returns, and its retries as the clock ticks.

## Step 20. One test, three depths

**Write the tests as a contract:** an interface of what a fan does and sees, and the scenarios written against it. Each area of the app gets its own contract interface, and the app's contract extends them all, so a test class runs everything by implementing one interface and naming its driver:

```kotlin
interface Fan {
    fun watches(show: ShowTitle)
    fun reserved(show: ShowTitle, seat: SeatRef): Verdict
    fun sees(show: ShowTitle): ShowSeen
    // releases, checksOut, basket …
}

interface SeatingContract {
    val driver: BoxOfficeDriver

    @Test
    fun `a seat reserved by one fan shows as taken for everyone watching`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        bob.watches(SHOW)

        assertThat(ann.reserved(SHOW, F12), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.sees(SHOW), equalTo(ShowSeen(mine = emptySet(), taken = setOf(F12)))) }
    }
}

interface CheckoutContract {
    val driver: BoxOfficeDriver
    // checking out, a declined payment …
}

interface BoxOfficeContract : SeatingContract, CheckoutContract

class BoxOfficeDomainTest : BoxOfficeContract {
    override val driver = DomainBoxOfficeDriver()
}
```

- **Contracts are interfaces, not classes.** A scenario is a `@Test` default method, and the one abstract member is `val driver`. Split by area of the app (seating, checkout), one file each, so a contract stays small enough to read.
- **Contracts and drivers' interfaces live in `testFixtures`.** The tests that implement them, and the drivers themselves, stay in `test`.
- **Ann acts, and Bob, already watching, sees.** Checking through the person who acted proves nothing about everyone else.
- **`watches` matters even with no page,** because a test tenant only catches a missing publish for a state it has read before. From then on, any step that changes it without publishing fails.
- **The domain driver reads what the regions show:** the same states, with the same keys. A driver that reads a convenience state instead leaves the region's own state unchecked.
- **A missed publish is a missing scenario.** Topics are named by hand in `Changes`; the contract is what proves them. Specify what everyone else sees after each change, and the tests say which topics an operation forgot.
- **`eventually { }`** retries a check until it passes. Only the real browser needs it; it passes first time everywhere else.

Each driver runs the contract at a different depth:

- **The features:** a test tenant (step 10). No HTTP; fast, and catches a missing publish.
- **The app:** the real `App` in memory, with an emulated browser. `browser(node, scheduler, cookie)`, from `testing`, runs `data-init`, follows `@post` and `@get`, holds the stream open and morphs in patches, and a deterministic scheduler drives every redraw: no server and no sleeps.
- **A real browser:** the app served and driven by Playwright (`Http4kBrowser`, from `http4k-testing-playwright`), for what only a browser shows, such as morphing and disconnects.

**A button the page hides is `Unavailable`.** Release on someone else's seat has no button, and a user can't tell a 403 from a 404 by looking, so the UI drivers return `Unavailable` and the contract asserts `refusedAs(Denied)`. The domain driver never answers `Unavailable`, so it must give the exact `Denied`: the rule, the security boundary, is proven there, and every route runs the same rule.

**An app's contract runs on one node.** Changes reaching viewers on other nodes, and a node dying with its viewers, are fanbelt's job, and fanbelt's own tests prove them over `InMemoryCluster`, from `testing`. Reach for it only to test something your app adds across nodes.

**See how a change spreads.** Every step of a change is an event: the operation and its outcome, each topic published, each region re-read and each browser patched. `MarbleEvents`, from `testing`, draws a scenario's steps as one lane each:

```
ann reserve @nyc             -✓--------------------------------------
bob reserve @nyc             -----------------------✗----------------
bookings:Midnight Owls @nyc  ---●------------------------------------
#seats(Midnight Owls) @nyc   ---------●------------------------------
#basket(bob) @nyc            -----------------○----------------------
bob #seats @nyc              ---------------------●------------------
```

A contract says what each person ends up seeing; a marble shows how the app got there. Read one when a test fails, to see who was sent what and why. Read one when a page redraws too often: a `○` is a read that found nothing new, and a lane you didn't expect is a topic that reaches too far. See [Recipe: marble traces](recipes.md#marble-traces) for setting it up.

## Step 21. Running it for real

Your migrations create the schema. Database storage needs a JSON object that registers every stored value type. Every failure and recovery is a typed event on `events`, to build health checks from. The checklist, including poolers, proxies and locking for a busy tenant, is in [Recipe: production](recipes.md#production).

## What a change costs

A publish costs one read for each key that watches it, on each node, and then a render for each viewer whose view differs. Re-rendering a whole region is cheap; what makes an app slow is the shape of its states:

- **A model is bounded by its key.** A window of notes reads its window, not the whole board: [watch coarse, read narrow](recipes.md#watch-coarse-read-narrow).
- **Keep shared and per-viewer apart.** An attribute of a shared row (who reserved a seat, who owns a room) goes in the shared model, and the view picks by viewer. A highlight goes in a small overlay region (step 16). Only data that differs per viewer gets a per-viewer key.
- **A state with no key that everyone watches must be cheap.** Don't gather every member's view of the tenant into one: key it per member, so it's read again only when what that member sees changes.
- **Arriving touches only the one arriving.** A stream that writes as it opens or closes publishes to everyone watching what it wrote, and reconnects and deploys multiply it. Entering a room marks it seen, and that changes only the viewer's own `seen`.
- **Every filtered field has an index, and every list read a limit.**

---

## What Fanbelt does not do

- **No client state.** No store, no cache, no optimistic UI. Rich widgets can be islands, web components fed by attributes, but nothing on the client owns state: see [Recipe: islands](recipes.md#islands).
- **No sync protocol.** No event payloads, no CRDTs. Topics name what to re-read.
- **No API to design.** Pages post what the user did and receive HTML.
- **No auth or policy.** Those are your app's, wired with ordinary http4k.

## Every app has the same shape

Every example keeps the same parts in the same places, so you can find anything in any of them. The folders follow the rings:

```
boxoffice/
  main.kt     picks the technology and the starting data        step 18
  App.kt      wires the parts together, and nothing else        step 18
  domain/     values, rows, read models, the actor              steps 1–4
  store/      tables, the list of tenants, JSON for stored rows steps 2, 18
  feature/    domain classes, features and their jobs           steps 3–9, 19
  view/       view models and signals                           steps 13–16
  web/        pages, regions, routes, lenses, the viewer cookie steps 12–16
  fakes/      stand-ins for outside systems                     step 19
```

- **Dependencies point inward.** `store` and `view` use `domain`, `feature` uses `store`, and `web` uses them all. Nothing in `domain` knows about pages.
- **A page has two halves with one name.** `web/ShowPage.kt` builds the page from its regions, and `view.ShowPageView`, with `ShowPageView.hbs` beside it in resources, is what it renders.
- **Contracts live in `testFixtures`.** `*Contract` interfaces are the scenarios, one per area, composed into the app's contract, beside the `*Driver` interfaces they are written against. `test` holds the drivers and the classes that run the contract through each.

It's a convention, not a rule: Fanbelt doesn't look at your folders.

## Where to next

- **[The reference](reference.md)** has the API of each concept, the production checklist and the limitations.
- **[The recipes](recipes.md)** have common patterns: authentication, create-then-go-there, revoking access, per-viewer state, validation as you type, and more.
- **The examples** are each self-contained:
  - [boxoffice](../examples/boxoffice): this guide;
  - [todo](../examples/todo): the smallest app, in memory;
  - [features](../examples/features): a software team's project space, with one small tool per pattern the [reference](reference.md) and [recipes](recipes.md) quote: a planning sheet with live cursors and a CSV download, rooms to talk in with private access, visits and a job, a noticeboard that grows, CI builds posted by webhook, and a status read from another service. It also starts two nodes in one process (`runCluster`), and runs over Postgres with Redis (`runDistributed`);
  - [sl4k](../examples/sl4k): a team chat with everything at once.

### The concepts, in the order you met them

| Step | Concept | Library type (module, if not `core`) |
|---|---|---|
| – | Tenants | `Workspace`, `Scope` |
| – | Modules (Setting up) | `core`, adapters, `testing` |
| 1 | Values | values4k |
| 2 | Rows and tables | `Identified`, `Tables`, `Table`, `Rows`, `Cursor` |
| 3 | Domain class | (yours) |
| 4 | Features and actors | `Feature` |
| 5 | States and topics | `StoredState` |
| 6 | Derived states | `derived`, `ctx.read`, `ctx.watch`, `ctx.stale` |
| 7 | Presence | `PresenceState`, `join`, `carry` |
| 8 | Rules | `rule`, `Allowed`, `Rejected`, `Denied`, `Hidden` |
| 9 | Operations and their outcomes | `Operation`, `Changes`, `Rejected`, `FieldName.check` |
| 10 | Testing without HTTP | `TestTenant`, `Verdict` (`testing`) |
| 12 | Routes | `operation(workspace, actor, input)`, `Workspace` |
| 13 | Regions, keys and absence | `Region`, keys from lenses, nullable models, `fallback`, `RedrawFailed` |
| 14 | Pages | `Page`, `Layout`, `Viewing`, `PageShell` |
| 15 | Who's watching | `Viewing`'s connect lambda, `subject` |
| 16 | Per-viewer detail | the view function, viewer-keyed regions |
| 18 | The app, and many nodes | `AppRuntime`, `AppInfra`, `Node`, `Transport`, `PostgresTransport` (`postgres`), `RedisTransport` (`redis`) |
| 19 | Jobs | `job`, `topic(key)`, `Jobs`, the storage's `locks` |
| 20 | Contract tests, and marbles | `browser`, `newPage(cookie)`, `InMemoryCluster`, `MarbleEvents` (`testing`) |
