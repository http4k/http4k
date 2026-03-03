# http4k Wiretap — Product Plan

## What This Is

A pro-tier development console that wraps an http4k application and provides a browser-based control panel for observing, testing, and manipulating it. One line
to add, one port to connect to, full visibility and control.

```kotlin
Wiretap { http, oTel, clock -> MyApp(http, oTel) }.asServer(Jetty(8080)).start()
// Your app:     http://localhost:8080/
// Wiretap:      http://localhost:8080/__wiretap
```

## The Product

The console has **eight panels**, each building on existing http4k infrastructure. Every panel implements the `WiretapFunction` interface, exposing both a
browser UI (`http()`) and an MCP tool/prompt (`mcp()`). In addition, Wiretap itself is an MCP server at `/_wiretap/mcp`.

### 0. Home / Overview [BUILT]

Dashboard landing page with live stats and metrics.

**What we built:**

- `home/Index.kt` — renders the overview page
- `home/Stats.kt` — `GetStats` WiretapFunction exposing `GET /stats` (browser) and `get_stats` MCP tool
- Uptime, total request counts (inbound/outbound)
- Latency distribution histogram (0-10ms, 10-50ms, 50-100ms, 100-500ms, 500ms+)
- Traffic timeline — stacked chart showing inbound/outbound over time
- Per-host timelines — separate traffic charts per host
- OTel trace count
- Chaos engine status (inbound + outbound active/inactive with descriptions)
- MCP capabilities summary (tool count, security mode, MCP URL)
- JVM metrics: heap used/max/committed, non-heap used, thread count (live/daemon/peak), GC pause count + total time, CPU usage (process + system), classes
  loaded/unloaded
- `TrafficMetrics` — Micrometer counters with periodic snapshots (10s interval via fixedRateTimer) for timeline charts
- `WiretapStats` — domain object aggregating all stats data
- `JvmMetrics` — domain object for JVM metrics

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
- Detail view: query parameters table, request/response headers, body (with pretty-printing), copy-as-cURL button, OTel trace deep-link
- Filters: direction, method, status range, path search (300ms debounce), host search (300ms debounce) — via `TransactionFilter` data class
- Named filter views with CRUD (built-in: All, Inbound, Outbound; user-created: save/clone/delete) — via `ViewStore` (InMemory or File-backed)
- Clickable filter cells — clicking any value in the list toggles the corresponding filter
- Real-time SSE streaming of new transactions via Datastar
- Resizable columns and panels via custom JS
- `BodyHydration` enum controlling stream buffering (All, RequestOnly, ResponseOnly, None)
- Chaos-affected rows visually marked via `x-http4k-chaos` header detection
- JSON pretty-printing (via Moshi) and XML indentation (via XSLT) in body view — `util/prettify.kt`
- `TransactionMapping.kt` — extension functions `toSummary()`, `toDetail()`, `matches()`
- `TransactionSummary` and `TransactionDetail` — split view models

**Not yet built:**

- SSE/WS/MCP protocol-specific traffic capture
- Header sanitisation (Authorization, Cookie, etc.) — `sanitise` parameter exists in `Wiretap()` signature but no default sanitiser provided

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

### 3. Client — Send Requests (the Postman replacement) [BUILT]

Build and send HTTP requests, import from captured traffic. Split into two sub-panels.

**What exists in http4k:**

- `Replay` — stream of recorded request/response pairs
- `Responder` — replays cached responses
- `ReadWriteStream` / `ReadWriteCache` — storage for traffic
- The entire http4k client stack

**What we built:**

- **Inbound Client** (`client/InboundClient.kt`): Sends through the full proxy chain (record → chaos → app) so requests appear in Traffic panel as inbound
- **Outbound Client** (`client/OutboundClient.kt`): Sends directly to external services via httpClient, bypassing the app
- Request builder UI: method select (GET/POST/PUT/DELETE/PATCH/HEAD/OPTIONS), URL input, header key/value rows, body textarea
- `x-http4k-wiretap: replay` header added to all sent requests for identification
- Replay badge ("R") shown in Traffic panel for replayed requests
- Import from traffic: dropdown listing recent transactions, populates form via Datastar signals
- Response displayed using shared `TransactionDetailView` (request/response split view)
- Clear button resets all form fields
- `FormatBody.kt` — body formatting utilities
- `HeaderRows.kt` — dynamic header row management

**Not yet built:**

- Save requests to named collections
- Environment variables (base URL, auth tokens) with switching
- Response viewer with assertions (status code, body contains, header exists)
- Collection runner — execute a sequence of requests and report pass/fail

### 4. Metrics — Integrated into Home Page [PARTIALLY BUILT]

Aggregate views computed from traffic. Rather than a separate panel, metrics are integrated into the Home/Overview page.

**What exists in http4k:**

- `HttpTransaction` has `duration`, `start`, `labels`, `routingGroup`
- `MetricsDefaults` in ops/core — standard labelling for metrics
- Micrometer and OpenTelemetry integrations

**What we built:**

- `TrafficMetrics` (`domain/TrafficMetrics.kt`) — Micrometer counters tracking inbound/outbound requests, with periodic snapshot mechanism for timeline
  computation
- Latency distribution per bucket (0-10ms, 10-50ms, 50-100ms, 100-500ms, 500ms+)
- Stacked traffic timeline (inbound vs outbound over time)
- Per-host timeline charts
- Total request counts
- All integrated into the Home/Overview page via `GetStats` WiretapFunction

**Not yet built:**

- Slowest routes table
- Route-level breakdown
- Filterable by same axes as traffic panel
- Separate dedicated metrics panel (if needed)

### 5. MCP Panel — Protocol-Specific Testing [BUILT — Phase 1]

Monitor and interact with the app's MCP server.

**What exists in http4k:**

- `pro/ai/mcp/` — full MCP implementation (core, client, SDK, testing, conformance)
- MCP uses HTTP as transport, so the traffic monitor already captures it
- MCP conformance testing module exists

**What we built:**

- `mcp/Mcp.kt` — `WiretapFunction` that connects to the upstream app's `/mcp` endpoint
- Uses `HttpNonStreamingMcpClient` to discover and interact with the app's MCP capabilities
- Auto-detection: checks if `/mcp` endpoint exists before activating (`mcpAvailable()`)
- `mcp/client/InboundClient.kt` — wires up sub-functions for Tools, Prompts, Resources, and Apps
- **Tools tab**: list tools, inspect tool details, call tools with parameters, view results (`tools/Tools.kt`, `CallTool.kt`, `InspectTool.kt`)
- **Prompts tab**: list prompts, inspect prompt details, get prompt content (`prompts/Prompts.kt`, `GetPrompt.kt`, `InspectPrompt.kt`)
- **Resources tab**: list resources, inspect resource details, read resource content, inspect templates (`resources/Resources.kt`, `ReadResource.kt`,
  `InspectResource.kt`, `InspectTemplate.kt`)
- **Apps tab**: list MCP apps (`apps/Apps.kt`, `AppsTabContent.kt`)
- Handlebars templates for all views: `ToolDetailView.hbs`, `ToolResultView.hbs`, `PromptDetailView.hbs`, `PromptResultView.hbs`, `ResourceDetailView.hbs`,
  `ResourceResultView.hbs`, `TemplateDetailView.hbs`

**Not yet built:**

- MCP-aware traffic parsing (decode JSON-RPC messages in traffic panel)
- MCP conversation flow timeline
- Schema validation of MCP messages

### 6. OpenAPI — Swagger UI Panel [BUILT]

Embedded Swagger UI for exploring the app's OpenAPI spec.

**What exists in http4k:**

- Full OpenAPI contract support via `http4k-contract`
- OpenAPI spec generation from route definitions

**What we built:**

- `OpenApi` WiretapFunction — routes to Swagger UI index, exports empty MCP capability
- Handlebars template (`Index.hbs`) loads Swagger UI v5.32.0 via CDN (unpkg)
- Points SwaggerUIBundle at `/openapi` endpoint
- Custom CSS hides server selector, version/title, and expand controls
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
- `TraceSummary` — domain object for trace list display
- Trace list panel: timestamp, short trace ID, root span name, service name, span count, total duration
- Gantt chart detail view: timeline with positioned bars per span, colour-coded by span kind
- Span detail sections: span info, attributes, baggage attributes, events with timestamps, resource attributes
- Deep linking: traffic detail -> OTel trace (via traceparent header), OTel trace -> traffic (via trace query param)
- Proper tree building from flat span list (handles orphan roots)
- `WiretapOpenTelemetry.kt` wires up `OpenTelemetrySdk` with W3C propagation and the custom exporter

**Not yet built:**

- TracerBullet / Mermaid sequence diagram integration
- Auto-detected actors from outbound call hostnames
- Export diagram as SVG/PNG

---

## MCP API — Wiretap as MCP Server [BUILT]

Wiretap itself exposes an MCP server at `/_wiretap/mcp`, making every panel's functionality available to AI clients.

**What we built:**

- `mcp_api/WiretapMcp.kt` — assembles `mcpHttpStreaming` server from all `WiretapFunction` capabilities + dedicated prompts
- `mcp_api/AnalyzeTrafficPrompt.kt` — AI prompt for traffic analysis
- `mcp_api/DebugRequestPrompt.kt` — AI prompt for debugging a specific transaction
- Every `WiretapFunction` contributes its `mcp()` capabilities (tools) to the server
- Configurable `McpSecurity` (defaults to `NoMcpSecurity`)
- `McpCapabilities` data class tracks tool count and security mode for display on home page

---

## WiretapFunction — Dual-Face Pattern [BUILT]

Core abstraction: every feature implements both a browser UI and an MCP interface.

```kotlin
interface WiretapFunction {
    fun http(elements: DatastarElementRenderer, html: TemplateRenderer): RoutingHttpHandler
    fun mcp(): ServerCapability
}
```

- **`http()`** returns routing handlers for the browser UI (Handlebars + Datastar)
- **`mcp()`** returns MCP server capabilities (tools, prompts, or empty `CapabilityPack`)
- All panels implement this: Traffic, Chaos, OTel, OpenAPI, InboundClient, OutboundClient, Mcp, GetStats
- `WiretapMcp` collects all `mcp()` outputs and serves them at `/_wiretap/mcp`

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
Wiretap(appBuilder) : PolyHandler
|
+-- TransactionStore.InMemory (ring buffer, 500 entries)
|   +-- record(tx) + subscriber notification
|   +-- subscribe(fn) for SSE push
|
+-- TraceStore.InMemory (ring buffer, 5000 spans)
|   +-- record(span) from WiretapSpanExporter
|   +-- traces() grouped by traceId
|
+-- ViewStore (InMemory or File-backed)
|   +-- Built-in views: All, Inbound, Outbound
|   +-- User views: CRUD with Moshi JSON persistence
|
+-- TrafficMetrics (Micrometer counters)
|   +-- Record inbound/outbound requests with latency
|   +-- Periodic snapshots (10s) for timeline charts
|   +-- Per-host timeline tracking
|
+-- ChaosEngine x2 (inbound + outbound)
|
+-- WiretapFunction implementations (each has http() + mcp()):
|   +-- Traffic(transactionStore, viewStore)
|   +-- Chaos(inboundChaos, outboundChaos)
|   +-- OTel(traceStore)
|   +-- InboundClient(clock, transactionStore, proxy)
|   +-- OutboundClient(outboundHttp, clock, transactionStore)
|   +-- OpenApi()
|   +-- Mcp(uri, httpClient, proxy)
|   +-- GetStats(trafficMetrics, traceStore, chaos engines, mcpCapabilities, meterRegistry)
|
+-- Proxy (transparent, orElse-bound)
|   +-- Inbound chain: bufferBodies -> recordTransaction(Inbound) -> inboundChaos -> app
|   +-- Outbound chain: bufferBodies -> recordTransaction(Outbound) -> outboundChaos -> httpClient
|   +-- BodyHydration controls stream buffering
|   +-- sanitise: (HttpTransaction) -> HttpTransaction? parameter for filtering/redacting
|
+-- HTTP Routes (/__wiretap/*)
|   +-- Traffic: Index, ListTransactions, ViewTransaction, ClearTransactions, ListViews, CreateView, UpdateView, DeleteView, TrafficStream(SSE)
|   +-- Home: Index, GetStats
|   +-- Chaos: Index, Status, Activate, Deactivate
|   +-- OTel: Index, ListTraces, ViewTrace
|   +-- OpenAPI: Index (Swagger UI via CDN)
|   +-- Inbound Client: Index, SendRequest, ImportTransaction, ImportList, HeaderRows, FormatBody
|   +-- Outbound Client: Index, SendRequest, HeaderRows, FormatBody
|   +-- MCP Panel: Index, Tools (list/inspect/call), Prompts (list/inspect/get), Resources (list/inspect/read/template), Apps
|   +-- static(Classpath("public")) -> wiretap.css, wiretap.js
|
+-- SSE Handler (/_wiretap/traffic)
|   +-- Real-time push of new transactions via Datastar PatchElements
|
+-- MCP API (/_wiretap/mcp)
|   +-- mcpHttpStreaming server serving all WiretapFunction capabilities
|   +-- AnalyzeTrafficPrompt, DebugRequestPrompt
|
+-- Templates: Handlebars (CachingClasspath, infiniteLoops enabled)
    +-- DatastarElementRenderer wraps template rendering for SSE morphing
```

### The Entry Point

```kotlin
object Wiretap {
    operator fun invoke(
        transactionStore: TransactionStore = TransactionStore.InMemory(),
        traceStore: TraceStore = TraceStore.InMemory(),
        viewStore: ViewStore = ViewStore.InMemory(),
        httpClient: HttpHandler = JavaHttpClient(),
        mcpSecurity: McpSecurity = NoMcpSecurity,
        clock: Clock = Clock.systemUTC(),
        sanitise: (HttpTransaction) -> HttpTransaction? = { it },
        bodyHydration: BodyHydration = BodyHydration.All,
        appBuilder: WiretapAppBuilder
    ): PolyHandler
}

fun interface WiretapAppBuilder {
    operator fun invoke(http: HttpHandler, oTel: OpenTelemetry, clock: Clock): HttpHandler
}
```

Usage:

```kotlin
Wiretap { http, oTel, clock -> MyApp(http, oTel) }.asServer(Jetty(8080)).start()
```

Short form for wrapping an existing URI:

```kotlin
Wiretap(Uri.of("http://localhost:9000")).asServer(Jetty(8080)).start()
```

---

## Phased Delivery

### Phase 1: Traffic Monitor [COMPLETE]

- TransactionStore with in-memory ring buffer and subscriber-based push
- Dashboard with traffic list, detail view, filters, views
- HTTP inbound + outbound monitoring
- Real-time SSE streaming via Datastar
- Copy-as-cURL, query parameters table

### Phase 2: Datastar UI + Chaos + OTel [COMPLETE]

- Server-rendered UI with Handlebars + Datastar (SSE-pushed DOM morphing)
- Chaos panel — UI over two ChaosEngine instances (inbound + outbound)
- OTel trace viewer — Gantt chart visualisation with span details
- Deep linking between traffic and traces (bidirectional)
- Named filter views with persistence

### Phase 3: Client Panel + OpenAPI [COMPLETE]

- Inbound Client — request builder sending through proxy chain [COMPLETE]
- Outbound Client — request builder sending directly to external services [COMPLETE]
- Import from traffic panel [COMPLETE]
- Replay identification in traffic panel [COMPLETE]
- OpenAPI / Swagger UI panel [COMPLETE]
- Collections (save/load) [NOT STARTED]
- Collection runner with assertions [NOT STARTED]

### Phase 4: Home Page + Metrics [PARTIALLY COMPLETE]

- Home/Overview page with live stats [COMPLETE]
- TrafficMetrics with Micrometer counters + periodic snapshots [COMPLETE]
- Latency distribution, traffic timeline, per-host timelines [COMPLETE]
- JVM metrics (heap, threads, GC, CPU) [COMPLETE]
- Route-level metrics breakdown [NOT STARTED]
- Dedicated filterable metrics panel [NOT STARTED]

### Phase 5: MCP [SUBSTANTIALLY COMPLETE]

- MCP Panel — browser UI for upstream app's MCP capabilities [COMPLETE]
    - Tool catalogue, inspection, and interactive execution [COMPLETE]
    - Prompt listing, inspection, and retrieval [COMPLETE]
    - Resource listing, inspection, reading, and template inspection [COMPLETE]
    - Apps listing [COMPLETE]
- MCP API — Wiretap as MCP server at `/_wiretap/mcp` [COMPLETE]
    - All WiretapFunction capabilities exposed as MCP tools [COMPLETE]
    - AnalyzeTrafficPrompt + DebugRequestPrompt [COMPLETE]
- WiretapFunction dual-face pattern [COMPLETE]
- MCP-aware traffic parsing in traffic panel [NOT STARTED]
- MCP conversation timeline [NOT STARTED]

### Phase 6: Advanced

- Contract validation against OpenAPI spec
- Diff/regression mode
- Fake service toggling
- Request diffing
- SSE/WS protocol monitoring

---

## What Already Exists vs What We Built

| Capability               | Existing in http4k                            | Built in Wiretap                                                   | Status  |
|--------------------------|-----------------------------------------------|--------------------------------------------------------------------|---------|
| HTTP traffic capture     | `ReportHttpTransaction`                       | TransactionStore, dashboard UI, SSE push                           | Done    |
| Chaos injection          | `ChaosEngine`, 11 behaviours, triggers        | Dashboard UI with inbound/outbound control                         | Done    |
| OTel trace visualisation | OTel SDK, `OpenTelemetryTracing` filter       | WiretapSpanExporter, Gantt chart viewer, TraceSummary              | Done    |
| OpenAPI / Swagger UI     | `http4k-contract`, OpenAPI spec generation    | Embedded Swagger UI via CDN, custom CSS                            | Done    |
| Traffic recording/replay | `Sink`/`Source`/`Replay`, Servirtium          | Inbound + Outbound clients, import from traffic, replay badge      | Done    |
| Home / Overview          | Micrometer, JVM metrics                       | Stats page, TrafficMetrics, JVM dashboard, traffic timeline charts | Done    |
| MCP Panel                | Full MCP impl in pro/ai/mcp                   | Tool/Prompt/Resource browser + interactive execution               | Done    |
| MCP API                  | MCP server infrastructure                     | Wiretap as MCP server, AI prompts, all tools exposed               | Done    |
| Pretty-printing          | Moshi (JSON), XSLT (XML)                      | `prettify.kt` — content-type dispatch for body formatting          | Done    |
| Metrics / Stats          | `MetricsDefaults`, OTel, Micrometer           | TrafficMetrics counters, latency buckets, timeline snapshots       | Partial |
| SSE/WS traffic capture   | `ReportSseTransaction`, `ReportWsTransaction` | Dashboard panel                                                    | Planned |
| Tracing diagrams         | `TracerBullet`, 8 renderers                   | Mermaid sequence diagrams                                          | Planned |
| Approval testing         | `Approver`, file-based storage                | Contract validation                                                | Planned |

---

## Resolved Design Decisions

1. **Transaction IDs**: Monotonic `AtomicLong` counter — shorter than UUIDs, sortable, debuggable.

2. **JSON serialisation**: Moshi. Used for ViewStore file persistence and Datastar signal deserialisation.

3. **Body capture**: `BodyHydration` enum (All, RequestOnly, ResponseOnly, None). Default `All`. Stream bodies buffered into memory before recording.

4. **Copy-as-cURL**: Generated via `Request.toCurl()` extension. Rendered in hidden `<pre>`, copied via `navigator.clipboard.writeText`.

5. **`Wiretap` IS the `PolyHandler`**: `Wiretap(appBuilder)` wires everything, returns `PolyHandler` for `.asServer()`.

6. **Naming**: `Wiretap`. Evocative, memorable, technically apt — you're tapping all the wires.

7. **UI framework**: Datastar + Handlebars server-rendered templates. No SPA, no client-side framework. SSE-pushed DOM morphing.

8. **Chaos integration**: Two separate `ChaosEngine` instances injected into inbound and outbound filter chains. Dashboard POSTs to activate/deactivate.

9. **OTel integration**: Custom `WiretapSpanExporter` feeds spans into `TraceStore`. `WiretapOpenTelemetry` builds `OpenTelemetrySdk` with W3C
   propagation. Deep linking between traffic and traces via `traceparent` header.

10. **Filter views**: Three built-in (All, Inbound, Outbound) + user-created. `ViewStore` has InMemory and File-backed implementations. Built-in views cannot be
    modified/deleted. Renamed from "Tabs"/"FilterStore" to "Views"/"ViewStore" for clarity.

11. **Default HTTP client**: `JavaHttpClient()` (no external dependency). Changed from original plan's `ApacheClient`.

12. **Template engine**: Handlebars with `setInfiniteLoops(true)` (required for layout partial blocks). `CachingClasspath` mode.

13. **WiretapFunction dual-face pattern**: Every panel implements `WiretapFunction` with `http()` for browser UI and `mcp()` for AI client capabilities. This
    ensures feature parity between browser and MCP access.

14. **Client split**: Inbound Client sends through the proxy chain (appears in traffic as inbound), Outbound Client sends directly via httpClient (bypasses
    app). Both share the same request builder UI (`Index.hbs`, `HeaderRowsView.hbs`).

15. **Metrics approach**: TrafficMetrics uses Micrometer counters with periodic snapshots (10s interval) rather than computing from TransactionStore on-demand.
    Integrated into Home page rather than a separate panel.

16. **MCP API**: Wiretap exposes its own MCP server at `/_wiretap/mcp`. All `WiretapFunction.mcp()` capabilities are collected and served alongside dedicated AI
    prompts (AnalyzeTraffic, DebugRequest).

17. **Transaction sanitisation**: `sanitise: (HttpTransaction) -> HttpTransaction?` parameter on `Wiretap()`. Returns null to drop, or a modified transaction
    for redaction. Default is identity (no sanitisation).

## Open Questions

1. **Client panel storage**: Where do saved collections live? In-memory (lost on restart) vs filesystem vs configurable `Storage<T>` from http4k-connect?
   Filesystem with a configurable path is probably right for dev/test.

2. **Multi-protocol store**: The current `TransactionStore` holds `WiretapTransaction` wrapping `HttpTransaction`. For SSE/WS/MCP, we need to generalise to
   `ProtocolTransaction<*>`. How polymorphic does the store need to be? Simplest: separate stores per protocol with a unified query interface.

3. **TracerBullet integration**: The OTel trace viewer was built independently using direct span data. Should we also integrate TracerBullet's Mermaid sequence
   diagrams as an alternative visualisation, or is the Gantt chart sufficient?
