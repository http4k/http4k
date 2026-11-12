# fanbelt

Fanbelt: the Orion technique (https://orion-tour.pages.dev/) on http4k: live, multiplayer web apps where every change is an operation that writes to a store and publishes a topic, and every view is a region that re-renders and streams itself to the browser over SSE using Datastar.

The library is split into modules the way http4k is: a technology-free core, one module per technology, and a test kit. An app depends on `core` plus the adapters it uses:

| Module | What's in it | Brings in |
|---|---|---|
| `core` | Features (states and operations); regions, pages and routes; tenancy; the bus (the `Backplane`, with `LocalTransport` for one node); presence (shared, with an in-memory backend); `AppRuntime` and `AppInfra` | http4k, values4k, tx4k |
| `exposed` | `ExposedDocumentStorage` and `PostgresDocuments`, `ExposedPresenceBackend` and `PresenceTable`, and `tables.exposed { }` for your own tables | Exposed |
| `jooq` | `tables.jooq { }`: jOOQ over the operation's transaction, filtered by tenant | jOOQ |
| `sqlite` | `SqliteDocumentStorage` and `SqliteDocuments` | `exposed`, the SQLite driver |
| `postgres` | `PostgresTransport` (`LISTEN/NOTIFY`) | `exposed`, the Postgres driver |
| `redis` | `RedisTransport` and `RedisPresenceBackend` | Lettuce |
| `testing` | The test kit: `TestTenant`, `TestRuntime`, `InMemoryCluster`, the emulated Datastar `browser`, `Verdict`, `eventually`, and `RealBrowser`, `MovableClock` and `MovableScheduler` for contracts in real Chromium | time4k, Selenium API, jsoup, http4k's Playwright support |

Package names don't follow the modules: `ExposedDocumentStorage` is `org.http4k.fanbelt.store.exposed` whichever module you get it from.

Each example app is its own module under `examples/` (`todo`, `board`, `helpdesk`, `sheets`, `exchange`, `boxoffice`, `sl4k`), self-contained, and laid out the same way, with `App.kt` and `main.kt` above `domain/`, `store/`, `feature/`, `view/`, `web/`, `jobs/` and `fakes/` ([what goes where](docs/guide.md#every-app-has-the-same-shape)): its `App()` wires everything it uses, its `main` runs on one technology, and its build file names exactly the modules it needs (todo and sheets: `sqlite`; board: `exposed` and `postgres`; helpdesk: `exposed` and `redis`; exchange: `sqlite` and `redis`; boxoffice and chat: `core` only). Tests run every example in memory. Run an example with `./gradlew :http4k-fanbelt-examples-todo:run` and its tests with `./gradlew :http4k-fanbelt-examples-todo:test`.

**Start here:** [docs/guide.md](docs/guide.md), a step-by-step guide that builds the box office one concept at a time, from plain Kotlin values to multi-node. **Reference:** [docs/reference.md](docs/reference.md), the API of every concept. **Recipes:** [docs/recipes.md](docs/recipes.md), common patterns. Glossary: [CONTEXT.md](CONTEXT.md).

## Examples

Each example prints its URL (a random port) on startup. Open it in two tabs or two browsers to watch the changes go live.

| Example | Run | What it shows |
|---|---|---|
| Todos | `./gradlew :http4k-fanbelt-examples-todo:run`, open `/` and pick or create a list (`/lists/{list}/`) | A live todo list: operations bound to routes, validation, a single region. Each list is a tenant. |
| Board | `./gradlew :http4k-fanbelt-examples-board:run`, open `/` and pick or create a team (`/teams/{team}/b/{board}`) | A kanban per team: presence, per-viewer roles, several regions, parameterised topics, and a live CI build badge on `/teams/demo/b/lobby`, fed by a fake CI that posts to the board's build endpoint every 3 seconds |
| Sheets | `./gradlew :http4k-fanbelt-examples-sheets:run`, open `/` and pick or create a sheet (`/sheets/{sheet}/`) | The benchmark example: one shared 20 × 10 grid with a region per row, in-cell editing, arrow keys and Tab, who's here (presence) and everyone's live cursors |
| Box office | `./gradlew :http4k-fanbelt-examples-boxoffice:run`, open `http://localhost:9000/` | The presentation's example ([`docs/presentation/index.html`](docs/presentation/index.html)): reserve seats live, two fans racing for one seat, reservations that lapse by the clock, who's watching, and checkout paid by a background worker through a fake provider |
| Chat (sl4k) | `./gradlew :http4k-fanbelt-examples-sl4k:run`, open `http://localhost:9000/` | sl4k, a team chat (`/teams/{team}/c/{channel}`): live messages with scrollback, unread counts per member, who's here and who's typing, private channels and direct messages behind a membership filter, and a channel topic, bookmarks, pinned messages and a shared canvas at the top of each channel, file attachments with images shown inline, message formatting, link previews fetched by a job with their images proxied, and live search |
| Exchange | `./gradlew :http4k-fanbelt-examples-exchange:run`, open `http://localhost:9000/` (any of 9000–9002), then optionally `HEADED=true BOTS=4 BROWSERS=4 ./gradlew :http4k-fanbelt-examples-exchange:swarm` | A trading venue (`/venues/{venue}/s/{symbol}`) on three nodes: price-time order matching, a live book with depth chart, candles and tape as server-rendered SVG, positions and a leaderboard marked to a fake price feed polled over HTTP, who's watching, and a Playwright bot swarm trading through the real UI (`BOTS`, `BROWSERS`, `HEADED`, `PAUSE_MS`) |
| Helpdesk | `./gradlew :http4k-fanbelt-examples-helpdesk:run`, open `/` and pick or create an organisation (`/orgs/{org}/`) | Two features (tickets, and the activity journal) on shared pages, two pages sharing a nav region, six regions on one stream, presence across pages, an activity journal, and a retried customer notification, sent by the app's own worker, with live status |

Todo, sheets, the board and the helpdesk have a home page that lists their tenants (lists, sheets, teams or organisations), found by searching storage for the row each one gets when it's created, and creates new ones. The box office, the exchange and sl4k go straight to their `demo` tenant. Each example's `main` creates a `demo` one at startup.

## Storage

Each example's `main` runs on one technology, so between them they cover every one Fanbelt supports. Every tenant shares one database, and each table has a `tenant` column. The tables are created at startup for local runs.

| Example | Technology | Setting |
|---|---|---|
| Todos | SQLite (single node) | `SQLITE_FILE`, default `todos.db` |
| Sheets | SQLite (single node) | `SQLITE_FILE`, default `sheets.db` |
| Box office | Memory (single node) | none |
| Chat (sl4k) | Memory storage on a single node; `:http4k-fanbelt-examples-sl4k:run` uses the local main in its tests, and the deployed main adds Redis for the backplane | none for `:http4k-fanbelt-examples-sl4k:run`; the deployed main needs `REDIS_URL` and takes its port as an argument (default 9000) |
| Exchange | SQLite storage on three nodes in one JVM, with Redis for the backplane and presence; recreated on every start | `SQLITE_FILE` (default `exchange.db`), `REDIS_URL` (default `redis://localhost:6379`), `NODES` (default 3), `SERVER` (`jetty`, `jettyloom`, `helidon` or `undertow`; default `jetty`) |
| Board | Postgres for everything: storage, the `LISTEN/NOTIFY` backplane and presence | `POSTGRES_URL` (required) |
| Helpdesk | Postgres storage, with Redis for the backplane and presence | `POSTGRES_URL` and `REDIS_URL` (required) |

With Docker:

```
docker run -d --name fanbelt-pg -e POSTGRES_PASSWORD=secret -p 5432:5432 postgres:16
docker run -d --name fanbelt-redis -p 6379:6379 redis:7
PG="jdbc:postgresql://localhost:5432/postgres?user=postgres&password=secret"
POSTGRES_URL=$PG ./gradlew :http4k-fanbelt-examples-board:run
POSTGRES_URL=$PG REDIS_URL="redis://localhost:6379" ./gradlew :http4k-fanbelt-examples-helpdesk:run
./gradlew :http4k-fanbelt-examples-exchange:run
```

## Running several nodes

Live updates fan out across processes through a backplane. The board and the helpdesk are multi-node as they stand: start the same command twice and the two nodes share storage and backplane.

## Tests

`./gradlew test` in the fanbelt modules runs the library's tests, and each example's contract through its domain driver and its emulated-browser driver. The library's Postgres and Redis tests start their own databases with Testcontainers, so they need Docker. One setting is opt-in:

| Setting | What it runs |
|---|---|
| `FANBELT_TEST_BROWSER=1` | Every example's contract in real headless Chromium, through Playwright (it downloads Chromium on first use). Presence-leave checks take up to 90 s, because a closed tab is only noticed on a failed SSE heartbeat. |

## Load

`./gradlew :http4k-fanbelt-load:run` runs four scenarios against real in-process nodes and writes each as a table to `load/build/results/<scenario>.md`. Choose some with `-Pscenarios=fanout,hot-tenant`. They run in memory unless `FANBELT_TEST_PG` (a Postgres JDBC url) or `FANBELT_TEST_REDIS` (a Redis URI) is set.

| Scenario | What it measures |
|---|---|
| `fanout` | Three nodes, sixty watchers on one sheet, cells changing: latency from POST to patch on another node, and backplane messages, reads, renders, patches and bytes per change, raw and as brotli would send them. |
| `presence-churn` | The same, with viewers moving their cursors: presence writes, renders and patches per move. |
| `hot-tenant` | Twenty fans racing for the seats at one show: reservations per second, latency, and that no seat is ever reserved for two fans or goes missing. |
| `memory` | A hundred SSE connections each compressing the same real presence patches with brotli: resident memory per connection and bytes per patch, for windows from 64 KB to brotli's default 4 MB. |
