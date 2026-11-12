# Building live apps with Fanbelt: a step-by-step guide

This guide builds one app, a box office, from plain Kotlin values up to a live, multi-user page, one concept at a time. Each step adds one ring around the last: first the domain, then the feature, then the web, then the app that runs it.

**Who it's for.** You know Kotlin and the web (HTTP, forms, cookies). You don't need to know http4k or Datastar, the small browser library Fanbelt uses: the web part introduces just enough of both, at the point you need them.

**The example.** Fans reserve seats for a show. Everyone watching sees a seat go, two fans clicking the same seat get one winner, a reservation lapses after five minutes, and checking out takes payment in the background. Every snippet is from [examples/boxoffice](../examples/boxoffice). Where the box office doesn't need a concept (paging, fragments, many nodes), the guide points at the example that does. The API of every concept is in the [reference](reference.md), and common patterns are in the [recipes](recipes.md).

**See it first.** Run `./gradlew :http4k-fanbelt-examples-boxoffice:run`, open `http://localhost:9000/`, pick a show, and open `http://localhost:9000/` in a second browser (or a private window, so it's a second fan) and pick the same show. Reserve a seat in one and watch it go in the other.

---

## The problem

Live, multi-user UIs usually get built twice. The browser has a state store, a cache, optimistic updates, an API client and a socket protocol to hear about other people's changes. The server has the database, an API shaped for the client, and push events that each carry enough data for the client to patch its copy. That's two copies of the state, kept in sync by hand.

Classic server rendering keeps one copy, in the database, and sends HTML. It's correct, but static: you see other people's changes when you refresh.

Fanbelt keeps the single copy and adds liveness: every screen updates when anything it shows changes, for everyone looking at it, with no second model in the browser.

## The idea

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

A POST never answers with new UI. It runs an **operation**, which writes to storage and says *which states it changed*. Every open page showing one of those states is subscribed to it. Each re-reads the state, re-renders that part of itself (a **region**), and sends the HTML down the page's one stream. You still write the domain, the views and the templates. The loop asks two things of that code: name what changed, and read fresh.

### The principles

1. **The server owns the truth.** It's in the database. The browser shows HTML it was sent and posts what the user wants to do.
2. **A change says *what* changed, never *how*.** A topic carries no data; it only means "re-read this". Every render reads the current truth, so a repeated or reordered message does no harm.
3. **Multiplayer is the default.** Your own change reaches you the same way as everyone else's.
4. **Liveness comes from what a view read.** A region re-renders when something it read changes, and the library works out what it read, so there's no subscription list to maintain.
5. **Everything you can see has an address.** If the server renders from it, it's in the URL. Changing what you see is following a link.
6. **An operation and its route are declared separately.** The operation is the behaviour, and runs from tests and background work as well as pages. The route is transport.
7. **Nodes are stateless.** Any node can serve any request, because the truth is in the database and "something changed" is on a shared bus.

### The rings

```
the domain       plain Kotlin, no web                           step
  values           every id and name is a type                    1
  rows, tables     typed access to the database                   2
  domain class     your logic over your rows                      3
the feature      what people can see and do, still no web
  feature, actor   one area of the app, and who acts in it        4
  states           named reads; a name is a topic                 5
  operations       one block, in a transaction                    6
  TestTenant       drives it, with no HTTP                        7
the web          http4k on the server, Datastar in the browser    8
  regions, pages   live parts of a page, one stream             9–10
  routes           an operation bound to a POST                  11
  derived states, keys, the URL, per-viewer detail, presence  13–17
the app          several features, tenants, work in the background
  features, tenancy, background work, wiring, browser tests   18–22
running it       many nodes, and production                   23–24
```

### Every app has the same shape

**Every Fanbelt app is built from the same parts, so every example keeps them in the same places.** Learn the layout once, and you can find anything in any of them. The folders follow the rings:

```
boxoffice/
  main.kt     picks the technology and the starting data        step 21
  App.kt      wires the parts together, and nothing else        step 21
  domain/     values, rows, read models, the actor              steps 1–4
  store/      tables, the list of tenants, JSON for stored rows steps 2, 19, 24
  feature/    domain classes and features                       steps 3–6, 13
  view/       view models and signals                           steps 9–10
  web/        pages, regions, routes, lenses, the viewer cookie steps 9–17
  jobs/       background work                                   step 20
  fakes/      stand-ins for outside systems                     step 20
```

- **Dependencies point inward.** `store` and `view` use `domain`, `feature` uses `store`, and `web` uses them all. Nothing in `domain` knows about pages.
- **A template sits with its view model.** http4k finds a template from the view model's package and name, so `boxoffice.view.ShowPageView` renders `resources/boxoffice/view/ShowPageView.hbs`.
- **A page has two halves with one name.** `web/ShowPage.kt` builds the page from its regions, and `view.ShowPageView` is what it renders.
- **`App.kt` holds no routes and no view models.** If you're looking for one, it's in `web/` or `view/`.
- **A folder with nothing to hold is left out.** Todo has no background work, so it has no `jobs/` or `fakes/`.
- **Tests stay flat.** `*Contract` is the scenarios, and `*Driver` the ways of running them ([step 22](#step-22-one-contract-three-drivers)).

This is a convention, not a rule: Fanbelt doesn't look at your folders. It's worth following for the same reason it is in Rails: nobody has to ask where something goes.

---

## Setting up

Fanbelt is split into modules the way http4k is: a technology-free `core`, one module per technology, and a test kit. The [README](../README.md) lists them all. The box office runs in memory, so its build is:

```kotlin
dependencies {
    implementation(project(":http4k-fanbelt-core"))
    implementation(project(":http4k-server-jetty"))
    implementation(project(":http4k-compression"))       // the compression the app serves (step 21)
    testImplementation(project(":http4k-fanbelt-testing"))   // TestTenant, the emulated browser and the real one (steps 7 and 22)
}
```

---

# Part 1: the domain

Start at the centre: the values, the rows and the rules. None of it depends on Fanbelt's web side, and most of it doesn't depend on Fanbelt at all.

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

data class ShowSeat(val show: ShowId, val seat: SeatRef)
```

A `SeatRef` only says that "F12" is well formed. Whether a show *has* an F12 is the domain's question (step 6). `ShowSeat` is a named pair, the seat at a show, and most of the box office works on it.

**You have:** a vocabulary that can't hold an invalid value.

## Step 2. Rows and tables

**Rows are data classes; tables are typed handles to them.** A row knows its id:

```kotlin
data class Layout(val rows: Int, val seatsPerRow: Int) {
    val seats get() = ('A'..'Z').take(rows).flatMap { row -> (1..seatsPerRow).map { SeatRef.of("$row$it") } }

    operator fun contains(seat: SeatRef) = seat.row < 'A' + rows && seat.number in 1..seatsPerRow
}

data class Show(override val id: ShowId, val title: ShowTitle, val layout: Layout) : Identified<ShowId>

data class Booking(override val id: ShowSeat, val fan: ViewerId, val expiresAt: Instant, val order: OrderId? = null) : Identified<ShowSeat> {
    fun liveAt(now: Instant) = order != null || now < expiresAt
}
```

A booking is identified by the seat at the show. It's reserved by a fan until it expires, or sold once it has an order.

You don't write SQL. Fanbelt stores every row as a JSON document, and you name the tables in a **tables object**:

```kotlin
class BoxOfficeTables(tables: Tables) {
    val shows = tables<ShowId, Show>(SHOWS)
    val bookings = tables<ShowSeat, Booking>(BOOKINGS)
    // orders, and the theatre's own row (step 19) …

    companion object {
        val SHOWS = TableName.of("shows")
        val BOOKINGS = TableName.of("bookings")
        // …
    }
}
```

A `Table<ID, T>` has `get`, `insert`, `update`, `delete`, `count`, and one query, `page`:

```kotlin
tables.bookings.page((Booking::id / ShowSeat::show) eq show).items   // the bookings for one show
```

`where` filters in the database, on ids, value types and enums (`eq`, `neq`, `oneOf`, and `contains` for a case-insensitive substring, combined with `and`), and `/` reaches inside a composite id. `order` is `Oldest`, `Newest`, or a field with a `limit` for "the top N". Pages by `Oldest` or `Newest` come with a cursor for the next page (step 15). When documents don't fit, say for joins or aggregates, you can use your own SQL tables in the same transaction: see [Recipe: your own SQL tables](recipes.md#your-own-sql-tables).

The `Tables` you're given is always one **tenant's**: here, one theatre's. Step 19 says where tenants come from. Until then, there's only ever one, and your code never sees its id.

**You have:** typed access to storage.

## Step 3. The domain class

**The domain class is your logic over your rows.** It's built for each transaction, from that transaction's tables:

```kotlin
class BoxOffice(private val tables: BoxOfficeTables, private val clock: Clock) {
    fun shows() = tables.shows.page().items

    fun show(id: ShowId) = tables.shows.get(id)

    fun bookings(show: ShowId) = tables.bookings.page((Booking::id / ShowSeat::show) eq show).items

    fun live(bookings: List<Booking>) = bookings.filter { it.liveAt(clock.instant()) }

    fun booking(pick: ShowSeat) = tables.bookings.get(pick)?.takeIf { it.liveAt(clock.instant()) }

    fun reserve(pick: ShowSeat, fan: ViewerId) {
        val booking = Booking(pick, fan, clock.instant() + RESERVED_FOR)
        if (tables.bookings.get(booking.id) == null) tables.bookings.insert(booking) else tables.bookings.update(booking.id) { booking }
    }

    fun release(booking: Booking) = tables.bookings.delete(booking.id)
}
```

Three rules:

- **No HTTP, no permissions, no publishing.** It works on rows.
- **Time and randomness are injected.** It takes a `Clock` (and a `Random` when it makes ids, with `random.nextUuid()`), never `Clock.systemUTC()` or `UUID.randomUUID()`, so tests can move time and repeat themselves.
- **A transaction may run more than once,** so there are no external calls here. Payment is taken by background work (step 20).

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
    // states and operations: steps 5 and 6
}
```

- **The function builds the domain for each transaction.** The feature is a class because its domain needs a `Clock`, which only the app creates (`BoxOfficeFeature(runtime.clock)`, step 21). A feature whose domain needs nothing can be an `object`.
- **The actor is whoever is acting, and not every actor is a person.** A fan reserves seats; the background job that takes payment acts as `System`. A feature with nobody to tell apart (the [todo example](../examples/todo)) uses `Unit`, and then nothing passes an actor at all.

**You have:** an empty feature, ready for its two halves.

## Step 5. States

**A state is a named read, and its name is its topic.** It's what part of a page shows:

```kotlin
val shows by state { boxOffice -> boxOffice.shows() }
val bookings by state(ShowId) { boxOffice, id -> boxOffice.bookings(id) }
```

- **It's named after its property.** `shows.topic()` is the topic "shows": the name used to announce that this state changed, and to listen for that.
- **Most states are about one thing, so they take a key.** `bookings.topic(id)` is "bookings:<id>", so the bookings for show A and show B are different topics, and a reservation on one redraws only that one. A stored state's key is a value type (`state(ShowId)` takes its factory), because it's part of the topic.

These are **stored** states, read from the domain. There are two other kinds, derived (step 13) and presence (step 17), and that's all.

**You have:** what people can see, by name.

## Step 6. Operations: Changes, Rejected or Denied

**An operation is one block, in a transaction.** It's given the domain, the actor and a typed input, and ends in one of three ways:

```kotlin
val fan by rule { _, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }

val reserve by operation(fan) { boxOffice, _, pick: ShowSeat, actor ->
    val show = boxOffice.show(pick.show) ?: return@operation Rejected(seatField, "No such show")
    if (pick.seat !in show.layout) return@operation Rejected(seatField, "No such seat at this show")
    val reservedBy = boxOffice.booking(pick)?.fan
    if (reservedBy != null && reservedBy != actor.id) return@operation Rejected(seatField, "Someone just took ${pick.seat}")
    boxOffice.reserve(pick, actor.id)
    Changes.of(bookings.topic(pick.show))
}
```

(`seatField` is `FieldName.of("seat")`, the field the page shows an error against. `fan` is a [rule](#denied-who-may-act): only a fan gets past it, and what it found, an `Actor.Fan`, comes last.)

### Changes: what changed, not how

`Changes.of(topics…)` says it's done and names what changed; `Changes.none()` changes nothing visible.

- **A topic carries no data.** Whoever hears it re-reads the state. Duplicates and reordering do no harm, so there's nothing to version or merge.
- **It's published after commit.** If the transaction rolls back, nothing is published.
- **Only an operation's `Changes` publish.** The domain and the HTTP layer never do.

### Rejected: two fans, one seat

`Rejected(field, message)` says the input or the current state won't do, with a reason for that field. Ann and Bob click F12 at the same moment:

- **A tenant's writes run one at a time.** Each takes the tenant's lock before it reads, so the second check sees the first reservation, and nobody's write can land between a check and its change. Two reservations at once queue; they don't fail.
- **The second fan is told "Someone just took F12",** the same way as any invalid input.
- **A refusal rolls back.** Anything written before it is undone, and nothing is published.

Input that arrives as text is checked the same way. `FieldName.check` turns raw text into a value type or a `Rejected` on that field, as the todo example does for a title, then `add` takes the value or ends with that refusal:

```kotlin
fun todoTitle(raw: String, field: FieldName = FieldName.of("title")) = field.check(TodoTitle, raw, "Title can't be blank or longer than 200 characters")

val title = todoTitle(raw).orElse { return@operation it }
```

`.zip(…)` combines checks on several fields so every error comes back at once.

### Denied: who may act

`Denied` means this actor may not. Who may act is a rule, which loads what it needs, checks it against the actor and passes it down. Only the fan who reserved a seat can release it:

```kotlin
val yourBooking by rule { ctx, actor, pick: ShowSeat ->
    val booking = ctx.domain.booking(pick)
    when {
        actor !is Actor.Fan -> Denied
        booking == null -> Unavailable(seatField, "Your reservation of ${pick.seat} has lapsed")
        booking.fan != actor.id -> Denied
        else -> Allowed(booking)
    }
}

val release by operation(yourBooking) { boxOffice, _, pick, booking ->
    if (booking.order != null) return@operation Rejected(seatField, "${pick.seat} is already paid for")
    boxOffice.release(booking)
    Changes.of(bookings.topic(pick.show))
}
```

- **Permission checks live in a rule,** which runs in the operation's transaction, against exactly the data it changes. Roles are just conditions on the actor and the data.
- **`Denied` has nothing to display.** The page never shows that button. A view can ask the same question to hide it, but the operation is what enforces it.
- **Being refused and not being allowed are different.** A lapsed reservation is `Unavailable`, which reaches the page as a `Rejected` with a reason; someone else's seat is `Denied`.
- **Something you can't know about answers `Hidden`,** a 404. See [Rules](reference.md#rules).

An operation can also return a result for its caller: `Changes.of(setOf(topic), id)`. See [Recipe: create, then go there](recipes.md#create-then-go-there).

**You have:** what people can do, with refusals they can understand.

## Step 7. Test it, with no HTTP

**A missing publish fails a test.** The feature has no web dependency, so the `testing` module's `TestTenant` drives it directly: one in-memory tenant, with the clock, scheduler and randomness controlled by the test:

```kotlin
val tenant = TestTenant()
val boxOffice = BoxOfficeFeature(tenant.runtime.clock)

tenant.execute(boxOffice.reserve, Actor.Fan(ann), ShowSeat(show, SeatRef.of("F12")))   // Accepted, Rejected(messages) or Denied
tenant.read(boxOffice.bookings, show)                                                  // the state's current value
tenant.tick(Duration.ofMinutes(5))                                                     // move the clock
```

`tenant.read` remembers each state's value and the topics it read. If the value changes and none of those topics was published, the next read fails. The likeliest bug in this design is a change that doesn't say what changed, and this catches it in milliseconds.

Write the tests as a **contract**: an interface of things a fan does and sees, and an abstract test class written against it:

```kotlin
interface Fan {
    fun watches(show: String)
    fun reserved(show: String, seat: String): Verdict
    fun sees(show: String): ShowSeen
    // releases, checksOut, basket …
}

abstract class BoxOfficeContract {
    abstract val driver: BoxOfficeDriver

    @Test
    fun `a seat reserved by one fan shows as taken for everyone watching`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        bob.watches(SHOW)

        assertEquals(Verdict.Accepted, ann.reserved(SHOW, "F12"))

        eventually { assertEquals(ShowSeen(mine = emptySet(), taken = setOf("F12")), bob.sees(SHOW)) }
    }
}

class BoxOfficeDomainTest : BoxOfficeContract() {
    override val driver = DomainBoxOfficeDriver()        // drives the features through a TestTenant
}
```

- **Ann acts, and Bob, already watching, sees.** Checking through the person who acted proves nothing about everyone else.
- **`watches` matters in the domain driver too,** because `TestTenant` can only catch a missing publish for a state it has read before.
- **`eventually { }`** retries a check until it passes. Only the real-browser driver (step 22) needs it; it passes first time everywhere else.

**You have:** fast tests of every behaviour, including "everyone watching sees it".

---

# Part 3: the web

Out of Kotlin: two new things, the browser and HTTP.

## Step 8. Just enough http4k and Datastar

**http4k is the server side.** A server is a function from `Request` to `Response` (an `HttpHandler`), and `routes` composes them:

```kotlin
routes("/shows/{show}" bind GET to { request: Request -> Response(OK).body("…") })
```

Everything Fanbelt gives you to mount is one of those functions. A `Filter` wraps a handler (`filter.then(handler)`), a **lens** reads a typed value from a request (`Path.value(ShowId).of("show")` is a `(Request) -> ShowId`, answering 400 if it doesn't parse), and a `PolyHandler` (`poly(…)`) serves HTTP and server-sent events (SSE) together. Templates are Handlebars, given a `ViewModel` data class.

**[Datastar](https://data-star.dev) is the browser side:** a small script that gives HTML attributes behaviour. An attribute posts to the server, and the response is HTML to swap in, or a signal.

**Signals** are the browser's small piece of state: the text in a field, an error message. In attributes, `$errors.seat` reads one and `$title = 'x'` writes one. In Fanbelt you never declare them yourself; the page renders them from a Kotlin class (step 10).

| Attribute | Meaning |
|---|---|
| `data-on:click="…"`, `data-on:submit`, `data-on:change` | Runs the expression on that event. `evt` is the event. |
| `data-bind:title` | Two-way binds an input's value to `$title`. |
| `data-show="$errors.seat"` | Shows the element only while the expression is truthy. |
| `data-text="$errors.seat"` | Sets the element's text. |
| `data-init="…"` | Runs the expression when the element appears. |
| `data-ignore-morph` | Leaves this element alone when the server patches its surroundings. |

Modifiers go after `__`: `data-on:input__debounce.300ms` waits for typing to pause. Signals whose names start with `_` stay in the browser and are never sent.

**Actions** talk to the server: `@post('…')` sends every signal as JSON, and `@get(url)` makes a GET. A response patches **signals**, or **elements**: HTML that Datastar matches to the page *by id* and morphs in, changing only what differs. An action whose response is an event stream keeps it open and applies each patch as it arrives. That's how a page stays live.

That's all of either that this guide needs.

## Step 9. Regions

**A region is a live part of a page, showing one state.**

```kotlin
data class ShowsView(val shows: List<Show>) : ViewModel

val renderer = HandlebarsTemplates().CachingClasspath()
val shows = Region("shows", renderer, boxOffice.shows) { shows, _ -> ShowsView(shows) }
```

```html
<div id="shows">
  <ul>{{#each shows}}<li><a href="shows/{{id}}">{{title}}</a></li>{{/each}}</ul>
</div>
```

- **Model: read once.** The state's value, shared by everyone watching. If it's equal to the last one, nothing is sent, which is why models are data classes.
- **View: per viewer.** `(model, request) -> ViewModel`; ignore the request with `_` unless it differs per viewer (step 16). It reads the model and the request, nothing else.
- **Template: renders the view as HTML.** Its root element carries the region's id; that's how Datastar knows where to morph the next version in.
- **The key comes from the request.** `shows` has no key, one list per theatre, so the region takes none. A keyed state takes a `(Request) -> Key`, such as the show in the URL (step 14).

**You have:** one state, rendered.

## Step 10. Pages

**A page is one route for the first render and the stream.**

```kotlin
data class TheatrePageView(val shell: PageShell) : ViewModel

val workspace = Workspace(infra, "/theatres/{theatre}", theatres::exists)   // finds the tenant: step 19
val theatrePage = Page(workspace, Layout(listOf(shows), renderer) { _, shell -> TheatrePageView(shell) })
```

The `Layout` is what the page draws: its regions, how to render them, and the page template's view model.

Mount it with one bind, `"/" bind theatrePage`. The browser loads the page, with every region rendered into the page's template. Datastar then asks the same URL for an event stream, and gets every region on the page, live, over one connection. (http4k picks between the two by the request's `Accept` header.)

The page template is given a `PageShell`:

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
- **`shell.connect`** opens the stream when the page loads, and reopens it whenever it drops, deploys included. While the page is hidden (another tab, or a page you've navigated away from) it lets the stream go, and reconnects with a fresh paint when the page is shown again.
- **`shell.base`** is the tenant's root, such as `/theatres/demo/`. Every URL in a template is relative to it (`shows/{{id}}`), so templates never mention tenants.
- **`shell.signals`** declares the page's signals from its signals class, `signals = ShowSignals()` (step 11): each field with its default, plus an `errors` entry for each.

**You have:** a page that keeps itself up to date, for every viewer.

## Step 11. Routes

**An operation is bound to a POST route by invoking it** with the workspace, the actor and where its input comes from:

```kotlin
val showId = Path.value(ShowId).of("show")
val seat = Path.value(SeatRef).of("seat")
val asFan = { request: Request -> Actor.Fan(principal(request).id) }

"/shows/{show}/seats/{seat}/reserve" bind POST to boxOffice.reserve(workspace, asFan) { ShowSeat(showId(it), seat(it)) },
"/shows/{show}/seats/{seat}/release" bind POST to boxOffice.release(workspace, asFan) { ShowSeat(showId(it), seat(it)) },
```

```html
<button id="seat-{{seat}}" class="seat" data-on:click="@post('shows/{{../../show}}/seats/{{seat}}/reserve')">{{number}}</button>
```

- **The workspace** finds the tenant the request belongs to (step 19), and the operation runs in that tenant.
- **The actor** is a function of the request. The request says who the *user* is (here, an anonymous cookie set on the home page by the app's `EnsureViewer` filter, and read by `Authenticated`); this function says which *actor* they act as. A real app authenticates with http4k: see [Recipe: authentication](recipes.md#authentication).
- **The input** is any `(Request) -> Input`: path lenses here, so "12F" is a 400 before the operation runs. Input can also come from the signals Datastar posts: type the lambda's parameter as the page's signals class, `{ signals: TodoSignals, _ -> signals.title }`, and name the signals to `reset` on success, as the [todo example](../examples/todo) does.

What the route answers:

| Outcome | Answer |
|---|---|
| Changes | A signals patch clearing any error the page was showing (and resetting the `reset` signals). |
| Rejected | A signals patch setting `errors.seat`, to this viewer only. The page shows it: `<p class="error" data-show="$errors.seat" data-text="$errors.seat"></p>`. |
| Denied | 403, and nothing is shown. |

The route never answers with new UI and never publishes. The page changes because regions re-render, for everyone at once, the person who clicked included. That's why the operation and its route are separate: the operation is the behaviour, and step 7 ran it with no route at all.

**You have:** the full loop.

## Step 12. How a page stays live

**A region re-renders when something it read changes.** Each read records the topics it touched, and the region subscribes to them. When one is published, the region re-reads once, renders for each viewer, and sends HTML only to the screens where it changed. There's no subscription list to maintain.

### Ann reserves F12: every concept, in order

1. **Click.** Datastar posts to `shows/<show>/seats/F12/reserve`; the page's `<base>` makes that `/theatres/demo/shows/<show>/seats/F12/reserve`.
2. **Parse.** The lenses turn the path into a `ShowSeat`, and the cookie becomes `Actor.Fan(ann)`.
3. **Decide.** In one transaction, `reserve` finds F12 free, and Ann reserves it.
4. **Announce.** After commit, `bookings:<show>` is published.
5. **Answer.** Ann's POST gets a signals patch clearing any error. No HTML.
6. **Re-read.** Every region on this show that read its bookings re-reads, once, however many people are watching.
7. **Patch.** Each screen whose HTML changed, Ann's and Bob's alike, gets the new region down its stream, and Datastar morphs it into place.

## Step 13. Derived states

**A derived state is built from other states, in one snapshot.** The show page's seat map needs the show's layout and its live bookings:

```kotlin
val seatMap by derived { ctx, id: ShowId ->
    val live = ctx.domain.live(ctx.read(bookings, id))
    val holders = live.associate { it.id.seat to it.fan }
    val show = ctx.domain.show(id)
    if (show == null) null else SeatMap(show, show.layout.seats.map { seat -> SeatView(seat, holders[seat]) })
}
```

- **`read(bookings, id)` reads another state,** and the derived state subscribes to whatever it read, so the seat map redraws when the bookings change.
- **All the reads happen in one transaction,** so the model is a consistent snapshot.
- **`domain` is an escape hatch** for reads that aren't states, and nothing tracks it. Pair it with a `read`, or `watch(topic)`, of whatever changes it. Here the show itself never changes, so nothing needs to.

A region still shows one state:

```kotlin
val seats = Region("seats", renderer, showId, boxOffice.seatMap, fallback = ShowGoneView) { map, _ ->
    if (map == null) return@Region ShowGoneView
    SeatMapView(map.show.id, map.seats.groupBy { it.seat.row }.map { (row, seats) -> SeatRow(row, …) })
}
```

Several sources make a derived state, not a second kind of region.

**You have:** composite views that can't drift from their sources.

## Step 14. Keys

**The key is *which view*; the topic is *what changed*.** The seat map's key is a `ShowId` from the URL, parsed by the same lens the route uses (`showId`). Everyone watching the same show shares one model, and a reservation on one show redraws that show's seat map and no other.

- **Filters and page sizes belong in the key,** read from the URL. A derived state's key can be any value, such as `ListView(filter, limit)`.
- **The topic stays coarse,** so operations never need to know which views are open. A filtered view reads the coarse state and filters it; when nothing it shows changed, nothing is sent.
- **A filtered view must read something that changes when its *membership* does.** "My tickets" built from each ticket's own topic sees updates to tickets it already shows, but misses one newly assigned to you, because it never read it. Read the coarse state, or have the operation that changes membership publish a topic for the set.

## Step 15. The URL holds view state

**If the server renders from it, it's in the URL.** Which show you're looking at is `/shows/<id>`. Changing what you see is following a link, and the new page is live. Only cosmetic state, such as whether a menu is open, stays in the browser, in `_` signals.

**Absence is a value.** A show can be removed while someone's watching it, or never have existed. The seat map is `null` then, and the view handles it with a "gone" view that carries the region's id too:

- **Disappearing while watched is just another change:** the region re-renders as gone.
- **Arriving at a missing one is a 404.** `subject = seats` on the page's `Layout` makes the page answer 404 if that state is absent at first render. It still renders, so one set of templates covers both.
- **Failures are different.** If a read or render throws, the page reports `RedrawFailed` and shows that region's `fallback` (`ShowGoneView` here, an empty element by default). The rest of the page carries on.

**Paging is the same idea.** An "older" link puts a cursor in the URL (`?before=…`), and the cursor becomes part of the region's key; the stream's URL keeps the query string, so the page stays live on whichever page you're on. The helpdesk's agent page does it:

```kotlin
val before = Query.value(Cursor).optional("before")
Region("agent", renderer, { AgentPage(agentId(it), before(it)) }, activity.agentPage) { … }
```

A region is re-read on every change it watches, so read only what's on screen. Growing lists work the same way: see [Recipe: growing lists](recipes.md#growing-lists).

## Step 16. Per-viewer detail

**Share the model; vary the view.** Your seats are a different colour from everyone else's, but the seat map is rendered once and shared. Each seat carries who reserved it (`data-reservedBy="…"`), and the page, which is rendered per viewer anyway, adds one CSS rule for this viewer:

```html
<style>[data-reservedBy="{{viewer}}"] { background: var(--mine) !important; }</style>
```

Before varying the view, ask whether the browser already knows the difference.

**When the read itself differs per viewer, key the region on the viewer.** "Reserved for you" is keyed on the show and the fan:

```kotlin
val basket = Region("basket", renderer, { request -> Shopper(showId(request), principal(request).id) }, orders.basket) { basket, request ->
    BasketView(showId(request), basket.reserved, …)
}
```

Those renders aren't shared, so keep it for when the data really differs. The [sheets example](../examples/sheets) pushes sharing furthest: a region per row, your own cursor kept in a browser signal, and everyone else's in one shared `<style>` region.

## Step 17. Presence

**Presence is who's here, while their page is open.** It's the third kind of state:

```kotlin
val watching by presence(ShowId, ViewerId)
```

Join it when a page's stream opens; the lease ends when the stream does:

```kotlin
val layout = Layout(listOf(header, seats, basket), renderer, subject = seats, signals = ShowSignals()) { request, shell ->
    ShowPageView(shell, showId(request), principal(request).id)
}
val showPage = Page(workspace, layout, Viewing { scope, request -> boxOffice.watching.join(scope, showId(request), principal(request).id) })
```

- **It isn't an operation:** nothing to validate and no transaction. It's a lease, held while the page is connected, and joining and leaving publish like any change.
- **It reads as a map from member to value.** "83 seats left, watching now" is a derived state over the seat map and presence:

  ```kotlin
  val header by derived { ctx, id: ShowId ->
      val map = ctx.read(seatMap, id)
      if (map == null) null else Header(map.show.title, map.seats.count { it.reservedBy == null }, ctx.read(watching, id).keys)
  }
  ```

- **`Viewing`** is how each browser's connection runs. Its `onConnect` is given the connection's scope and request, and returns an `AutoCloseable`, closed when the stream ends.
- **A member can carry one value,** such as a cursor, which goes when they leave: `presence(ViewerId, CellRef::class)` and `here.set(scope, who, value)`, as sheets does. See [Recipe: per-viewer, short-lived state](recipes.md#per-viewer-short-lived-state).
- **Tests join without a page:** `tenant.join(boxOffice.watching, show, fan)`.

Across nodes, leases live in shared storage, so a dead node's viewers expire (step 23).

**You have:** every concept on the show page.

---

# Part 4: the app

The outer ring: several features, one space per theatre, background work, and the node that serves it.

## Step 18. Several features

**An app is several features over shared infrastructure.** The box office decides who has reserved what; orders turns reservations into paid tickets:

```kotlin
class OrdersFeature(boxOffice: BoxOfficeFeature, clock: Clock, random: Random) :
    Feature<Orders, Actor>({ tables -> Orders(BoxOfficeTables(tables), clock, random) }) {
    val tickets by state(ViewerId) { orders, fan -> orders.of(fan) }

    val basket by derived { ctx, shopper: Shopper ->
        val reserved = ctx.domain.reserved(ctx.read(boxOffice.bookings, shopper.show), shopper.fan)
        Basket(reserved.map { it.id.seat }.sortedWith(seatOrder), ctx.read(tickets, shopper.fan).filter { it.show == shopper.show })
    }

    val fan by rule { _, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }

    val checkout by operation(fan) { orders, _, id: ShowId, actor ->
        orders.checkout(id, actor.id) ?: return@operation Rejected(seatField, "Reserve some seats first")
        Changes.of(boxOffice.bookings.topic(id), tickets.topic(actor.id))
    }
}
```

- **Dependencies run one way.** Orders is given the box office and reads its states; the box office doesn't know orders exists.
- **A derived state can read any feature's states,** in the same transaction.
- **A page mixes regions from any features** over one stream: the show page has the seat map and header from the box office, and the basket from orders.
- **Features share the tenant's tables.** Checkout writes the order and marks its bookings sold in one transaction, and says both changed.
- **One `TestTenant` runs any number of features:** `OrdersFeature(boxOffice, tenant.runtime.clock, tenant.random)`.

## Step 19. Tenancy

**Every request belongs to a tenant, and your code never sees its id.** A tenant is whatever your product calls its separate spaces; here, a theatre.

```kotlin
val workspace = Workspace(infra, "/theatres/{theatre}", theatres::exists)
```

- **`Workspace`** finds the tenant from the URL: `/theatres/demo/…` belongs to `demo`. `workspace.routes(oneTheatre)` mounts a theatre's routes under the template, `workspace.scope(request)` gives its scope, and `workspace.uri(tenant)` its root URL.
- **`Scope`** is one tenant, built per request and shared by every feature. `scope.execute(operation, actor, input)` runs an operation, `scope.read(state, key)` reads a state, and `scope.read(feature) { domain -> … }` reads straight from a domain in a read transaction, with no permission check. Changes go through an operation.
- **Storage is handed out already bound to one tenant,** which is why the domain never saw a tenant id.
- **A tenant's writes queue; different tenants never conflict.**
- **The app owns its tenant registry.** Each theatre gets a row of its own when it's created, in a table declared like any other, and `Theatres` finds them by it:

  ```kotlin
  data class Theatre(val createdAt: Instant, override val id: String = SELF) : Identified<String>   // one row per tenant

  class Theatres(private val storage: DocumentStorage) {
      fun exists(theatre: TenantId) = storage.read { BoxOfficeTables(it.tables(theatre)).theatre.get(Theatre.SELF) != null }
      fun create(theatre: TenantId, …) = storage.write { tenants -> … }      // the theatre's row, and its shows
  }
  ```

  `workspace.routes` answers 404 for any theatre that `exists` says no to, for pages, routes and streams alike.

## Step 20. Background work and time

**Fanbelt runs no background work itself; your app does.** Two habits connect it to the loop:

- **Record the intent in an operation.** Checkout writes a `Pending` order, so it exists if and only if the change committed.
- **Record the outcome through an operation, as `System`,** so pages update like any other change:

```kotlin
val paid by operation { orders, actor, id: OrderId ->
    if (actor != Actor.System) return@operation Denied
    val order = orders.order(id) ?: return@operation Changes.none()
    orders.mark(id, OrderStatus.Confirmed)
    Changes.of(tickets.topic(order.fan))
}
```

The app's `PaymentJobs` is a `Lifecycle` that sweeps every second: it finds the theatres with pending orders (`storage.read { it.having(ORDERS, Order::status eq Pending) }`), charges each order through the runtime's outbound HTTP client, and executes `paid` or `declined`. The client is the runtime's, so tests wire in a fake provider.

**A read says when it goes stale.** Nobody acts when a reservation lapses; time just passes. The seat map says so as it reads:

```kotlin
live.filter { it.order == null }.forEach { ctx.staleAt(it.expiresAt) }
```

The region reads again at the earliest of those moments, and not before, with nothing published. An unchanged model sends nothing. Tests move the clock (`tenant.tick(Duration.ofMinutes(5))`) and watch the seat come free. Nobody sleeps. See [Background work and time](reference.md#background-work-and-time).

## Step 21. Wiring: `App` and `main`

**`App` reads no configuration; `main` picks the infrastructure.** One function wires everything and returns a `Node`:

```kotlin
fun App(runtime: AppRuntime = AppRuntime(), infrastructure: (AppRuntime) -> AppInfra = ::AppInfra): Node {
    val runtime = runtime.withEvents(EventFilters.AddTimestamp(runtime.clock).then(EventFilters.AddEventName()).then(runtime.events))
    val infra = infrastructure(runtime)
    val theatres = Theatres(infra.storage)
    val boxOffice = BoxOfficeFeature(runtime.clock)
    val orders = OrdersFeature(boxOffice, runtime.clock, runtime.random)
    val workspace = Workspace(infra, "/theatres/{theatre}", theatres::exists)
    val renderer = HandlebarsTemplates().CachingClasspath()

    val oneTheatre = poly(
        listOf(BoxOfficeRoutes(boxOffice, orders, workspace)),
        "/shows/{show}" bind ShowPage(workspace, boxOffice, orders, renderer),
        "/" bind TheatrePage(workspace, boxOffice, renderer),
    )

    val home = poly(EnsureViewer(runtime.random).then("/" bind GET to { Response(SEE_OTHER).location(workspace.uri(TenantId.of("demo"))) }))

    val app = PolyFilters.Compression(ContentEncoding.Brotli())
        .then(PolyFilters.ReportTransaction(runtime.events))
        .then(ReportFailures(runtime.events))
        .then(poly(home, workspace.routes(identified.then(oneTheatre))))

    return Node(app, runtime, infra, PaymentJobs(runtime, infra, orders))
}

fun main() {
    val storage = InMemoryDocumentStorage()
    Theatres(storage).create(TenantId.of("demo"), Clock.systemUTC(), SecureRandom(), ShowTitle.of("Midnight Owls") to Layout(rows = 8, seatsPerRow = 12))
    val runtime = AppRuntime(http = SetBaseUriFrom(Uri.of("http://localhost:9200")).then(JavaHttpClient()))   // the payment provider
    App(runtime) { AppInfra(it, storage) }.asServer(Jetty(9000)).start()
}
```

- **`ShowPage`, `TheatrePage` and `BoxOfficeRoutes`** are functions in `web/` that build what you met in steps 9 to 11, so `App` only joins them up.
- **`AppRuntime`** is everything with side effects: the clock, randomness, the scheduler, the events sink and outbound HTTP. Tests pass a `TestRuntime`, with deterministic parts.
- **`AppInfra`** is storage, the bus and presence, shared by every feature; each defaults to in memory. It's given as a function of the runtime, so the app decides what the infrastructure sees (here, events stamped with a timestamp and name).
- **`Node`** is the app's handler and everything with background work (the bus, presence, `PaymentJobs`), in start order. `node.asServer(…).start()` starts them, then serves; `stop()` reverses it.
- **`main` chooses the technology and the starting data.** `App` creates nothing, so tests start empty and create what they need.
- **The wrapping is yours.** Compression, `ReportTransaction` (one event per request, outside `ReportFailures` so it sees the 500s), `ReportFailures` (exceptions become typed events and a 500) and the viewer cookie are ordinary http4k filters, and this is where an app adds its own: OpenTelemetry, auth, health checks, more routes. Fanbelt only needs `workspace.routes(…)` somewhere inside.

**You have:** a runnable app: `./gradlew :http4k-fanbelt-examples-boxoffice:run`.

## Step 22. One contract, three drivers

The contract from step 7 runs three ways:

- **Domain:** `TestTenant`. Fast, and catches a missing publish.
- **UI:** an emulated Datastar browser over the real `App`, with no server and no sleeps:

  ```kotlin
  class UiBoxOfficeDriver(…) : BoxOfficeDriver {
      private val node = App(TestRuntime(scheduler, http = provider)) { AppInfra(it, storage) }
      // each fan: browser(node, scheduler, viewerCookie(id)), clicking "#seat-F12" and reading "p.error"
  }
  ```

  `browser(node, scheduler, cookie)`, from `testing`, is a Selenium `WebDriver` that runs `data-init`, follows `@post` and `@get`, holds the stream open and morphs in patches. The deterministic scheduler drives every redraw, so "Bob sees it" needs no waiting.
- **Real browser:** Playwright, opt-in with `FANBELT_TEST_BROWSER=1`, for what only a real browser shows (morphing, disconnects). `RealBrowser(uri)` drives the running `App`, and checks on other people's screens are wrapped in `eventually { }`.

**A button the page hides is still posted.** Release on someone else's seat has no button, so the driver posts it anyway (`node.provesServerRefuses(path, cookie)`), and only a 403 counts as `Denied`. The test proves the server refuses, not just that the page hid it.

---

# Part 5: running it

## Step 23. Many nodes

**Swap two parts.** To run several nodes behind any load balancer, with no sticky sessions, change the **transport** (how a change reaches the other nodes) and the **presence backend**. The features, the pages and `App` don't change; only `main` does. The [board](../examples/board) puts everything in Postgres:

```kotlin
val db = Database.connect(dataSource)
App(runtime, { runtime ->
    AppInfra(
        runtime,
        ExposedDocumentStorage(db, BoardJson, PostgresDocuments),
        PostgresTransport(dataSource::getConnection, runtime.scheduler, runtime.events),
        ExposedPresenceBackend(db, runtime.clock, BoardJson),
    )
}, token)
```

and the [helpdesk](../examples/helpdesk) keeps Postgres storage with Redis for the rest (`RedisTransport`, `RedisPresenceBackend`).

- **The bus becomes a backplane.** Each node tells the others which topics changed, through Postgres `LISTEN/NOTIFY` or Redis pub/sub. It carries topic names only, no data.
- **A lost message is caught up.** Views on a backplane re-check within five minutes, and send nothing if nothing changed.
- **Each comes from its technology's module** (`postgres`, `redis`, `exposed`), imported only by `main`.
- **The same contract proves it.** `InMemoryCluster`, from `testing`, builds several nodes over one in-memory cluster, and the board's contract runs again with users spread across two nodes, plus a node that dies (`cluster.nodes[1].dies()`) and takes its viewers with it.

## Step 24. Running it for real

- **Storage.** Database storage writes rows as JSON, so give it one named JSON object that registers every value type the rows use (`object TodoJson : ConfigurableMoshi(standardConfig().withFanbeltMappings().value(TodoId).value(TodoTitle).done())`, passed to `SqliteDocumentStorage` or `ExposedDocumentStorage`), and the same one to shared presence.
- **Schema.** Your migrations create every table: one document table per table name (`PostgresDocuments.schema(name)` or `SqliteDocuments.schema(name)`), `fanbelt_presence` (`PresenceTable()`) if presence is in Postgres, and an index for each JSON field you filter on. Fanbelt never creates schema.
- **Configuration.** Each example's `main` uses one technology and reads only its keys: the box office and chat run in memory; todo and sheets use SQLite (`SQLITE_FILE`); the board uses Postgres (`POSTGRES_URL`); the helpdesk, Postgres with Redis (`POSTGRES_URL`, `REDIS_URL`); the exchange, SQLite with Redis. Tests run every example in memory.
- **Deploys.** Pages reconnect by themselves. Changing a stored data class beyond adding a defaulted field is a migration: see [Recipe: evolving stored rows](recipes.md#evolving-stored-rows).
- **Postgres behind a connection pooler:** the backplane needs a direct connection, or nodes silently stop hearing each other. See [Production](reference.md#production).
- **Past a few nodes:** see [Production](reference.md#production).
- **Observability.** Every failure is a typed event on the runtime's `events` (`RequestFailed`, `StreamFailed`, `RedrawFailed`, bus and presence events), and so is recovery. Health checks are your app's, built from them.

---

## What Fanbelt does not do

- **No client state.** No store, no cache, no optimistic UI. Rich widgets can be islands, web components fed by attributes, but nothing on the client owns state: see [Recipe: islands](recipes.md#islands).
- **No sync protocol.** No event payloads, no CRDTs. Topics name what to re-read.
- **No API to design.** Pages post what the user did and receive HTML.
- **No background work, auth or policy.** Those are your app's, wired with ordinary http4k.

## Where to next

- **[The reference](reference.md)** has the API of each concept, the production checklist and the limitations.
- **[The recipes](recipes.md)** have common patterns: authentication, create-then-go-there, revoking access, per-viewer state, validation as you type, and more.
- **The examples** are each self-contained. Each runs one contract through all three drivers (the board, helpdesk and sheets also over a two-node cluster), except the exchange, which is checked by a bot swarm instead:
  - [boxoffice](../examples/boxoffice): this guide;
  - [todo](../examples/todo): the smallest app, input from signals, SQLite;
  - [board](../examples/board): presence, derived states, Postgres for everything, and input from another system ([recipe](recipes.md#input-from-another-system));
  - [helpdesk](../examples/helpdesk): a sealed actor, paging, a background worker, fragments ([one-shot reads](reference.md#one-shot-reads)), two features, and Postgres with Redis;
  - [sheets](../examples/sheets): a region per row, presence carrying a cursor, and per-viewer marks kept in the browser;
  - [exchange](../examples/exchange): three nodes under load, driven by a Playwright bot swarm;
  - [sl4k](../examples/sl4k), a team chat (`./gradlew :http4k-fanbelt-examples-sl4k:run`): threads, reactions, mentions with tab completion, unread counts per member, typing that lapses, private channels and direct messages behind a membership filter, and the gate region that throws out a removed member.

### The concepts, in the order you met them

| Step | Concept | Library type (module, if not `core`) |
|---|---|---|
| – | Modules (Setting up) | `core`, adapters, `testing` |
| 1 | Values | values4k |
| 2 | Rows and tables | `Identified`, `Tables`, `Table`, `Rows`, `Cursor` |
| 3 | Domain class | (yours) |
| 4 | Features and actors | `Feature` |
| 5 | States and topics | `StoredState` |
| 6 | Operations and their outcomes | `Operation`, `Changes`, `Rejected`, `Denied`, `FieldName.check` |
| 7 | Testing without HTTP | `TestTenant`, `Verdict` (`testing`) |
| 9 | Regions | `Region` |
| 10 | Pages | `Page`, `PageShell` |
| 11 | Routes and feedback | `operation(workspace, actor, input)`, `Feedback` |
| 13 | Derived states | `derived`, `ctx.read`, `ctx.watch` |
| 14–15 | Keys, view state in the URL, absence | keys from lenses, nullable models, `subject`, `RedrawFailed` |
| 16 | Per-viewer detail | the view function, viewer-keyed regions |
| 17 | Presence | `PresenceState`, `join`, `set` |
| 18 | Several features | features sharing one `AppInfra`; cross-feature `read` |
| 19 | Tenancy | `Workspace`, `Scope`, `DocumentStorage` |
| 20 | Background work and time | operations as `System`, `Lifecycle`, `staleAt` |
| 21 | Wiring | `AppRuntime`, `AppInfra`, `Node`, `ReportFailures` |
| 22 | Browser tests | `browser`, `RealBrowser` (`testing`) |
| 23 | Many nodes | `Transport`, `PostgresTransport` (`postgres`), `RedisTransport` (`redis`), `InMemoryCluster` (`testing`) |
