# Fanbelt

Fanbelt is a library for live, multi-user web apps on http4k. The server holds all the state. Each change is an **operation**, which writes to storage and names what changed. Each part of a page is a **region**, which re-renders when something it read changes, and streams the new HTML to the browser over SSE, where [Datastar](https://data-star.dev) swaps it in. It's the [Orion technique](https://orion-tour.pages.dev/) on the JVM.

**Start here:** [the guide](docs/guide.md) builds one app, a box office, one concept at a time. Then:

- [the reference](docs/reference.md): the API of every concept;
- [the recipes](docs/recipes.md): common patterns;
- [the theory](docs/theory.md): the model behind it, as rules to argue with;
- [the comparison](docs/comparison.md): Fanbelt against the original Orion;
- [the glossary](CONTEXT.md): every term, in one place.

## Modules

Like http4k, Fanbelt is a technology-free core, one module per technology, and a test kit. An app depends on the core and the adapters it uses.

| Module | What's in it | Brings in |
|---|---|---|
| `http4k-fanbelt` | Features, states, rules and operations; regions, pages and routes; tenancy; the bus; presence; jobs; in-memory storage, bus and presence for one node | http4k, values4k, tx4k |
| `http4k-fanbelt-exposed` | Document storage over Exposed, presence in a database table, and `tables.exposed { }` for your own tables | Exposed |
| `http4k-fanbelt-jooq` | `tables.jooq { }` for your own tables | jOOQ |
| `http4k-fanbelt-postgres` | Postgres storage, and a bus over `LISTEN/NOTIFY` | `exposed`, the Postgres driver |
| `http4k-fanbelt-redis` | A bus over Redis pub/sub, and presence in Redis | Lettuce |
| `http4k-fanbelt-testing` | The test kit: `TestTenant`, an emulated Datastar browser, a real-browser driver, several nodes in memory, and marble diagrams | |

Package names follow the concept, not the module: `ExposedPresenceBackend` is in `org.http4k.fanbelt.presence.exposed`.

## Examples

Each example is its own module under `examples/`, laid out the same way ([every app has the same shape](docs/guide.md#every-app-has-the-same-shape)). Run one, then open the URL it prints in two browsers (or one normal and one private window) to watch changes go live.

| Example | Run | What it shows |
|---|---|---|
| Todos | `./gradlew :http4k-fanbelt-examples-todo:run` | The smallest app: operations, validation, one region. Each list is a tenant. |
| Box office | `./gradlew :http4k-fanbelt-examples-boxoffice:run` | The guide's example: fans racing for seats, reservations that lapse, who's watching, and payment taken by a job. |
| Features | `./gradlew :http4k-fanbelt-examples-features:run` | A software team's project space, one small tool per concept: a planning sheet with live cursors and a CSV download, rooms to talk in, a noticeboard, CI builds posted by webhook, and a status read from another service. |
| Chat (sl4k) | `./gradlew :http4k-fanbelt-examples-sl4k:run` | A team chat: unread counts, typing indicators, private channels, threads, reactions, search, attachments and link previews. |

Tests run every example in memory: `./gradlew :http4k-fanbelt-examples-todo:test`.

## Storage

Every tenant shares one database, and each table has a `tenant` column. When an example uses a database, its `main` creates the tables at startup.

| Example | Runs on | Settings |
|---|---|---|
| Todos, box office | memory, one node | none |
| Features | memory, one node (`run`); two nodes in one process sharing memory (`runCluster`); Postgres with Redis (`runDistributed`) | none: `runDistributed` expects the Docker containers below |
| Chat (sl4k) | memory, one node; the deployed `main` adds Redis for the bus and presence | `REDIS_URL` for the deployed `main` only |

With Docker:

```
docker run -d --name fanbelt-pg -e POSTGRES_PASSWORD=secret -p 5432:5432 postgres:16
docker run -d --name fanbelt-redis -p 6379:6379 redis:7
./gradlew :http4k-fanbelt-examples-features:runDistributed
```

`runDistributed` runs as several nodes as it is: start it twice, and the two nodes share storage and the bus.

## Tests

`./gradlew test` runs the library's tests, and each example's scenarios through the domain, an emulated browser and headless Chromium, through Playwright. The Postgres and Redis tests start their own databases with Testcontainers, so they need Docker. Playwright downloads Chromium on first use. Its checks that someone has left wait a few seconds, because a closed tab is only noticed at the stream's next ping.
