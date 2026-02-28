# http4k Wiretap — Product Plan

## What This Is

A pro-tier development console that wraps an http4k application and provides a browser-based control panel for observing, testing, and manipulating it. One line
to add, one port to connect to, full visibility and control.

```kotlin
Wiretap.Http { http, oTel, clock -> MyApp(http, oTel) }.asServer(Jetty(8080)).start()
// Your app:     http://localhost:8080/
// Wiretap:      http://localhost:8080/__wiretap
```

## The Product

The console has **seven panels**, each building on existing http4k infrastructure:

### 1. Traffic — See Everything [BUILT]

Real-time view of all HTTP traffic flowing in and out.

**What exists in http4k:**

- `ResponseFilters.ReportHttpTransaction` — captures `HttpTransaction` with timing, routing group, labels
- `ReportSseTransaction`, `ReportWsTransaction` — same pattern for SSE and WebSocket
- `ProtocolTransaction` — the shared base type across all protocols
- `HttpTransactionLabeler` — user-supplied custom labels
- `Sink`/`Source`/`Replay` in traffic-capture — pluggable storage

**What we built:**

- `TransactionStore` with in-memory ring buffer (500 entries), subscriber-based real-time push
- Dashboard panel: live-updating table with direction (down/up arrows), method, host, path (with query string), status, duration, timestamp
- Detail view: query parameters table, request/response headers, body, copy-as-cURL button, OTel trace deep-link
- Filters: direction, method, status range, path search (300ms debounce), host search (300ms debounce)
- Named filter tabs with CRUD (built-in: All, Inbound, Outbound; user-created: save/clone/delete)
- Clickable filter cells — clicking any value in the list toggles the corresponding filter
- Real-time SSE streaming of new transactions via Datastar
- Resizable columns and panels via custom JS
- `BodyHydration` enum controlling stream buffering (All, RequestOnly, ResponseOnly, None)
- Chaos-affected rows visually marked via `x-http4k-chaos` header detection

**Not yet built:**

- SSE/WS/MCP protocol-specific traffic capture
- Header sanitisation (Authorization, Cookie, etc.)
- JSON pretty-printing / XML indentation in body view

### 2. Chaos — Break Things on Demand [BUILT]

Inject failures, latency, and misbehaviour through the dashboard UI.

**What exists in http4k:**

- `ChaosEngine` — filter-based chaos injection with atomic enable/disable
- 11 behaviours: `Latency`, `ReturnStatus`, `ThrowException`, `NoBody`, `SnipBody`, `EatMemory`, `StackOverflow`, `KillProcess`, `BlockThread`,
  `ReturnResponse`, `None`
- Composable triggers: `Always`, `Once`, `PercentageBased`, `Deadline`, `Delay`, `MatchRequest`, `Countdown`
- Triggers compose with AND/OR/NOT operators

**What we built:**

- Side-by-side Inbound / Outbound chaos configuration panels
- Behaviour selection: ReturnStatus, Latency, NoBody (with conditional parameter inputs)
- Request filter: method, path (substring), host (substring) — composed with AND logic
- Trigger selection: Always, PercentageBased, Once, Countdown
- Activate/Deactivate buttons per direction
- Status panel showing active/inactive state with engine description
- Two separate `ChaosEngine` instances injected into inbound and outbound filter chains

**Not yet built:**

- Full set of behaviours (currently 3 of 11 exposed in UI)
- Full set of triggers (currently 4 of 7 exposed in UI)
- Live indicator overlay on traffic rows (partially done — `chaosInfo` field exists but no visual badge in row template)

### 3. Client — Send Requests (the Postman replacement) [BUILT — Phase 1]

Build and send HTTP requests against the monitored app, import from captured traffic.

**What exists in http4k:**

- `Replay` — stream of recorded request/response pairs
- `Responder` — replays cached responses
- `ReadWriteStream` / `ReadWriteCache` — storage for traffic
- The entire http4k client stack

**What we built:**

- Request builder UI: method select (GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS), URL input, 5 header key/value rows, body textarea
- Sends through the full proxy chain (record → chaos → app) so requests appear in Traffic panel
- `x-http4k-wiretap: replay` header added to all sent requests for identification
- Replay badge ("R") shown in Traffic panel for replayed requests
- Import from traffic: dropdown listing recent transactions, populates form via Datastar signals
- Response displayed using the same `TransactionDetailView` from the traffic panel (request/response split view)
- Clear button resets all form fields

**Not yet built:**

- Save requests to named collections
- Environment variables (base URL, auth tokens) with switching
- Response viewer with assertions (status code, body contains, header exists)
- Collection runner — execute a sequence of requests and report pass/fail

### 4. Metrics — Histograms and Aggregates

Aggregate views computed from the traffic store.

**What exists in http4k:**

- `HttpTransaction` has `duration`, `start`, `labels`, `routingGroup`
- `MetricsDefaults` in ops/core — standard labelling for metrics
- Micrometer and OpenTelemetry integrations

**What we build:**

- Computed from the `TransactionStore` in real-time — no external metrics system needed
- Latency distribution per route (histogram/heatmap)
- Request rate over time (sparkline)
- Status code breakdown (2xx/3xx/4xx/5xx pie or bar)
- Slowest routes table
- Inbound vs outbound comparison
- All filterable by the same axes as the traffic panel

**Status:** Not started.

### 5. MCP — Protocol-Specific Testing

Monitor and test MCP (Model Context Protocol) traffic specifically.

**What exists in http4k:**

- `pro/ai/mcp/` — full MCP implementation (core, client, SDK, testing, conformance)
- MCP uses HTTP as transport, so the traffic monitor already captures it
- MCP conformance testing module exists

**What we build:**

- MCP-aware traffic parsing — decode JSON-RPC messages, show tool calls, results, errors as structured data rather than raw JSON
- Tool catalogue view — list discovered tools with their schemas
- Tool testing UI — select a tool, fill in parameters (generated from schema), execute, see result
- Conversation flow view — show the back-and-forth of an MCP session as a timeline
- Schema validation — flag requests/responses that don't match the declared schema

**Status:** Not started.

### 6. OpenAPI — Swagger UI Panel [BUILT]

Embedded Swagger UI for exploring the app's OpenAPI spec.

**What exists in http4k:**

- Full OpenAPI contract support via `http4k-contract`
- OpenAPI spec generation from route definitions

**What we built:**

- `OpenApi` WiretapFunction — routes to Swagger UI index, exports empty MCP capability
- Handlebars template (`Index.hbs`) loads Swagger UI v5.32.0 via CDN (unpkg)
- Points SwaggerUIBundle at `/openapi` endpoint
- Custom CSS hides server selector (`.schemes-server-container`), version/title (`.title > span`, `.main > a`, `.main.title`), and expand controls (
  `.expand-operation`)
- Nav link placed between OTel and Inbound Client in header

### 7. OTel Traces — Span Visualisation [BUILT]

Visualise how requests flow through your system via OpenTelemetry spans.

**What exists in http4k:**

- `ServerFilters.OpenTelemetryTracing` — W3C trace context propagation
- OpenTelemetry SDK integration in `http4k-ops-opentelemetry`

**What we built (differs from original plan):**

Rather than using TracerBullet + Mermaid sequence diagrams, we built a direct OTel span visualiser:

- `WiretapSpanExporter` — custom OTel `SpanExporter` that captures spans into a `TraceStore`
- `TraceStore` — in-memory ring buffer (5000 spans), grouped by traceId
- Trace list panel: timestamp, short trace ID, root span name, service name, span count, total duration
- Gantt chart detail view: timeline with positioned bars per span, colour-coded by span kind
- Span detail sections: span info, attributes, baggage attributes, events with timestamps, resource attributes
- Deep linking: traffic detail -> OTel trace (via traceparent header), OTel trace -> traffic (via trace query param)
- Proper tree building from flat span list (handles orphan roots)
- `WiretapDependencies.oTel(traceStore)` wires up `OpenTelemetrySdk` with W3C propagation and the custom exporter

**Not yet built:**

- TracerBullet / Mermaid sequence diagram integration
- Auto-detected actors from outbound call hostnames
- Export diagram as SVG/PNG

---

## Additional Feature Ideas

### Contract Validation

http4k has OpenAPI support. If the user provides an OpenAPI spec, the dashboard could validate live traffic against it and flag violations — wrong status codes,
missing headers, schema mismatches. This turns the monitor into a continuous conformance checker.

### Diff / Regression Mode

Record a "golden" traffic run (using the existing Servirtium infrastructure), then compare subsequent runs against it. Flag new endpoints, changed response
shapes, unexpected status codes. Like approval testing but live in the dashboard.

### Fake Service Toggle

The connect module's fakes (S3, KMS, etc.) all extend `ChaoticHttpHandler`. The dashboard could let you toggle between real and fake backends for specific
outbound destinations at runtime. The outbound filter is already in the chain — it just needs to route differently based on dashboard config.

### Request Diffing

Select two captured transactions and diff them side-by-side — request vs request, response vs response. Useful for debugging "why did this work yesterday but
not today?"

---

## Architecture — As Built

```
Wiretap.Http(appBuilder) : PolyHandler
|
+-- WiretapDependencies
|   +-- httpClient: HttpHandler (default: JavaHttpClient)
|   +-- clock: Clock (default: systemUTC)
|   +-- oTel(traceStore) -> OpenTelemetrySdk with WiretapSpanExporter
|
+-- TransactionStore.InMemory (ring buffer, 500 entries)
|   +-- record(tx) + subscriber notification
|   +-- subscribe(fn) for SSE push
|
+-- TraceStore.InMemory (ring buffer, 5000 spans)
|   +-- record(span) from WiretapSpanExporter
|   +-- traces() grouped by traceId
|
+-- FilterStore (InMemory or File-backed)
|   +-- Built-in tabs: All, Inbound, Outbound
|   +-- User tabs: CRUD with Moshi JSON persistence
|
+-- ChaosEngine x2 (inbound + outbound)
|
+-- Proxy (transparent, orElse-bound)
|   +-- Inbound chain: bufferBodies -> recordTransaction(Inbound) -> inboundChaos -> app
|   +-- Outbound chain: bufferBodies -> recordTransaction(Outbound) -> outboundChaos -> httpClient
|   +-- BodyHydration controls stream buffering
|
+-- HTTP Routes (/__wiretap/*)
|   +-- Traffic: Index, ListTransactions, ViewTransaction, ClearTransactions, Tabs, TrafficStream(SSE)
|   +-- Chaos: Index, Status, Activate, Deactivate
|   +-- OTel: Index, ListTraces, ViewTrace
|   +-- OpenAPI: Index (Swagger UI via CDN)
|   +-- Client: Index, SendRequest, ImportTransaction, ImportList
|   +-- static(Classpath("public")) -> wiretap.css, wiretap.js
|
+-- SSE Handler (/__wiretap/transactions/stream)
|   +-- Real-time push of new transactions via Datastar PatchElements
|
+-- Templates: Handlebars (CachingClasspath, infiniteLoops enabled)
    +-- DatastarElementRenderer wraps template rendering for SSE morphing
```

### The Entry Point

```kotlin
object Wiretap {
    fun Http(
        filterStore: FilterStore = FilterStore.InMemory(),
        dependencies: WiretapDependencies = WiretapDependencies(),
        bodyHydration: BodyHydration = BodyHydration.All,
        idSource: () -> Long = AtomicLong()::incrementAndGet,
        appBuilder: WiretapAppBuilder
    ): PolyHandler
}

fun interface WiretapAppBuilder {
    operator fun invoke(http: HttpHandler, oTel: OpenTelemetry, clock: Clock): HttpHandler
}
```

Usage:

```kotlin
Wiretap.Http { http, oTel, clock -> MyApp(http, oTel) }.asServer(Jetty(8080)).start()
```

---

## Phased Delivery

### Phase 1: Traffic Monitor [COMPLETE]

- TransactionStore with in-memory ring buffer and subscriber-based push
- Dashboard with traffic list, detail view, filters, tabs
- HTTP inbound + outbound monitoring
- Real-time SSE streaming via Datastar
- Copy-as-cURL, query parameters table

### Phase 2: Datastar UI + Chaos + OTel [COMPLETE]

- Server-rendered UI with Handlebars + Datastar (SSE-pushed DOM morphing)
- Chaos panel — UI over two ChaosEngine instances (inbound + outbound)
- OTel trace viewer — Gantt chart visualisation with span details
- Deep linking between traffic and traces (bidirectional)
- Named filter tabs with persistence

### Phase 3: Client Panel + OpenAPI [IN PROGRESS]

- Request builder [COMPLETE]
- Import from traffic panel [COMPLETE]
- Replay identification in traffic panel [COMPLETE]
- OpenAPI / Swagger UI panel [COMPLETE]
- Collections (save/load)
- Collection runner with assertions

### Phase 4: Metrics

- Histograms and aggregates from traffic store
- Latency distribution, request rate, status breakdown
- Filterable by same axes as traffic panel

### Phase 5: MCP Testing

- Tool catalogue from schema discovery
- Interactive tool tester
- MCP conversation timeline

### Phase 6: Advanced

- Contract validation against OpenAPI spec
- Diff/regression mode
- Fake service toggling
- Request diffing
- SSE/WS protocol monitoring

---

## What Already Exists vs What We Built

| Capability               | Existing in http4k                            | Built in Wiretap                                   | Status  |
|--------------------------|-----------------------------------------------|----------------------------------------------------|---------|
| HTTP traffic capture     | `ReportHttpTransaction`                       | TransactionStore, dashboard UI, SSE push           | Done    |
| Chaos injection          | `ChaosEngine`, 11 behaviours, triggers        | Dashboard UI with inbound/outbound control         | Done    |
| OTel trace visualisation | OTel SDK, `OpenTelemetryTracing` filter       | WiretapSpanExporter, Gantt chart viewer            | Done    |
| OpenAPI / Swagger UI     | `http4k-contract`, OpenAPI spec generation    | Embedded Swagger UI via CDN, custom CSS            | Done    |
| Traffic recording/replay | `Sink`/`Source`/`Replay`, Servirtium          | Request builder, import from traffic, replay badge | Partial |
| SSE/WS traffic capture   | `ReportSseTransaction`, `ReportWsTransaction` | Dashboard panel                                    | Planned |
| Tracing diagrams         | `TracerBullet`, 8 renderers                   | Mermaid sequence diagrams                          | Planned |
| MCP                      | Full implementation in pro/ai/mcp             | MCP-aware parsing, tool tester UI                  | Planned |
| Metrics                  | `MetricsDefaults`, OTel, Micrometer           | In-dashboard histograms from store                 | Planned |
| Approval testing         | `Approver`, file-based storage                | Contract validation                                | Planned |

---

## Resolved Design Decisions

1. **Transaction IDs**: Monotonic `AtomicLong` counter — shorter than UUIDs, sortable, debuggable.

2. **JSON serialisation**: Moshi. Used for FilterStore file persistence and Datastar signal deserialisation.

3. **Body capture**: `BodyHydration` enum (All, RequestOnly, ResponseOnly, None). Default `All`. Stream bodies buffered into memory before recording.

4. **Copy-as-cURL**: Generated via `Request.toCurl()` extension. Rendered in hidden `<pre>`, copied via `navigator.clipboard.writeText`.

5. **`Wiretap` IS the `PolyHandler`**: `Wiretap.Http(appBuilder)` wires everything, returns `PolyHandler` for `.asServer()`.

6. **Naming**: `Wiretap`. Evocative, memorable, technically apt — you're tapping all the wires.

7. **UI framework**: Datastar + Handlebars server-rendered templates. No SPA, no client-side framework. SSE-pushed DOM morphing.

8. **Chaos integration**: Two separate `ChaosEngine` instances injected into inbound and outbound filter chains. Dashboard POSTs to activate/deactivate.

9. **OTel integration**: Custom `WiretapSpanExporter` feeds spans into `TraceStore`. `WiretapDependencies.oTel()` builds `OpenTelemetrySdk` with W3C
   propagation. Deep linking between traffic and traces via `traceparent` header.

10. **Filter tabs**: Three built-in (All, Inbound, Outbound) + user-created. `FilterStore` has InMemory and File-backed implementations. Built-in tabs cannot be
    modified/deleted.

11. **Default HTTP client**: `JavaHttpClient()` (no external dependency). Changed from original plan's `ApacheClient`.

12. **Template engine**: Handlebars with `setInfiniteLoops(true)` (required for layout partial blocks). `CachingClasspath` mode.

## Open Questions

1. **Client panel storage**: Where do saved collections live? In-memory (lost on restart) vs filesystem vs configurable `Storage<T>` from http4k-connect?
   Filesystem with a configurable path is probably right for dev/test.

2. **Multi-protocol store**: The current `TransactionStore` holds `WiretapTransaction` wrapping `HttpTransaction`. For SSE/WS/MCP, we need to generalise to
   `ProtocolTransaction<*>`. How polymorphic does the store need to be? Simplest: separate stores per protocol with a unified query interface.

3. **Header sanitisation**: Not yet implemented. Original plan called for sanitising Authorization, Cookie, etc. at the API/rendering layer. Still needed?

4. **TracerBullet integration**: The OTel trace viewer was built independently using direct span data. Should we also integrate TracerBullet's Mermaid sequence
   diagrams as an alternative visualisation, or is the Gantt chart sufficient?
