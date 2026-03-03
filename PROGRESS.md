# Wiretap — Implementation Progress

## Module Structure

- **Module**: `pro/tools/wiretap` (gradle: `:http4k-tools-wiretap`)
- **Dependencies**: http4k-core, http4k-format-moshi, http4k-ops-opentelemetry, http4k-realtime-core, http4k-template-handlebars, http4k-testing-chaos,
  http4k-web-datastar
- **Test deps**: http4k-testing-hamkrest, http4k-server-jetty

## What's Built

### Core Abstraction: WiretapFunction (`WiretapFunction.kt`)

Every panel implements this interface, providing both a browser UI and MCP capability:

```kotlin
interface WiretapFunction {
    fun http(elements: DatastarElementRenderer, html: TemplateRenderer): RoutingHttpHandler
    fun mcp(): ServerCapability
}
```

All panels (Traffic, Chaos, OTel, OpenAPI, InboundClient, OutboundClient, Mcp, GetStats) implement this.

### Entry Point (`Wiretap.kt`)

- `Wiretap(viewStore, transactionStore, traceStore, httpClient, mcpSecurity, clock, sanitise, bodyHydration, appBuilder)` returns `PolyHandler`
- `Wiretap(uri)` — shorthand for wrapping an existing URI
- Combines HTTP routes (dashboard + transparent proxy) with SSE handler (real-time traffic stream) and MCP server
- `WiretapAppBuilder` functional interface: `(HttpHandler, OpenTelemetry, Clock) -> HttpHandler`
- Creates `TrafficMetrics` with Micrometer `MeterRegistry` and periodic snapshot timer (10s interval)
- Assembles all `WiretapFunction` implementations, collects their `mcp()` capabilities
- Wires MCP server at `/_wiretap/mcp` via `WiretapMcp`

### Transparent Proxy (`Proxy.kt`)

- `orElse` bound — catches all requests not matched by `/__wiretap` routes
- **Inbound chain**: bufferBodies -> recordTransaction(Inbound) -> inboundChaos -> user's app
- **Outbound chain**: bufferBodies -> recordTransaction(Outbound) -> outboundChaos -> httpClient
- `BodyHydration` enum (All, RequestOnly, ResponseOnly, None) controls stream buffering
- `recordTransaction` uses `ResponseFilters.ReportHttpTransaction` for timing, wraps in `WiretapTransaction`, stores, records metrics, and notifies subscribers
- `sanitise` parameter: `(HttpTransaction) -> HttpTransaction?` — return null to drop, or modified transaction for redaction

### Domain Layer

- **`TransactionStore`**: Interface with `record`, `list`, `get`, `subscribe`, `clear`. `InMemory` uses `ConcurrentLinkedDeque` ring buffer (500 max) +
  `CopyOnWriteArrayList` subscribers.
- **`TraceStore`**: Interface with `record`, `list`, `traces`, `get`, `clear`. `InMemory` uses `ConcurrentLinkedDeque` ring buffer (5000 max). `traces()` groups
  by traceId.
- **`ViewStore`**: Interface with `list`, `add`, `update`, `remove`. `InMemory` + `File` (Moshi JSON persistence). Three built-in views (All, Inbound,
  Outbound) protected from mutation.
- **`TrafficMetrics`**: Micrometer counters for inbound/outbound requests with latency tracking. Periodic `snapshot()` captures current counts for timeline
  charts. Methods: `totalRequests()`, `inboundCount()`, `outboundCount()`, `latencyCounts()`, `trafficTimeline()`, `hostTimelines()`.
- **`WiretapTransaction`**: `id: Long`, `transaction: HttpTransaction`, `direction: Direction`
- **`Direction`**: Enum `Inbound | Outbound`
- **`TransactionFilter`**: Data class with optional `direction`, `host`, `method`, `status`, `path`.
- **`TransactionMapping`**: Extension functions `toSummary()`, `toDetail()`, `matches(filter)` for converting `WiretapTransaction` to view models.
- **`TransactionSummary`**: View model for transaction list rows (id, direction, method, host, path, status, duration, timestamp, etc.)
- **`TransactionDetail`**: View model for transaction detail panel (all summary fields + queryParams, headers, body, curl, traceId)
- **`TraceSummary`**: Domain object for trace list display (traceId, rootSpanName, serviceName, spanCount, duration, timestamp)
- **`WiretapStats`**: Domain object aggregating all overview stats (uptime, request counts, latency counts, traffic timeline, host timelines, trace count, chaos
  status, JVM metrics)
- **`JvmMetrics`**: Domain object for JVM stats (heap, threads, GC, CPU, classes)
- **`ChaosConfig`**: Behaviour (ReturnStatus/Latency/NoBody), trigger (Always/PercentageBased/Once/Countdown), request filter (method/path/host). Methods:
  `toBehaviour()`, `toTrigger()`, `toFilterTrigger()`, `toStage()`.
- **`ChaosStatusData`**: Domain object for chaos engine status display.
- **`WiretapSpanExporter`**: OTel `SpanExporter` that feeds spans into `TraceStore`.

### Home / Overview Panel (`home/`)

- **`Index.kt`**: GET `/` — renders the overview landing page
- **`Stats.kt`**: `GetStats` WiretapFunction:
    - HTTP: `GET /stats` — returns `StatsView` rendered via DatastarElements into `#overview-stats`
    - MCP: `get_stats` tool — returns stats as JSON
    - Collects: uptime, request counts, latency distribution, traffic timeline (stacked chart), per-host timelines, OTel trace count, chaos status, MCP
      capabilities, JVM metrics
    - `StatsView` computes: badge classes, heap percent, CPU percent, latency chart JSON, traffic timeline JSON, host timeline entries
    - `McpCapabilities` data class: security mode, tool count, MCP URL

### Traffic Panel (`traffic/`)

- **`Traffic.kt`**: `WiretapFunction` assembling all traffic routes; `mcp()` exports list/get/clear transaction tools
- **`Index.kt`**: GET `/` — full page render with optional `?trace=` deep-link (looks up transaction by trace ID)
- **`ListTransactions.kt`**: GET `/transactions` — reads `TransactionFilter` from Datastar signals, filters list via `matches()`, renders `TransactionSummary`
  elements, morphs `#tx-list`
- **`ViewTransaction.kt`**: GET `/transactions/{id}` — renders `TransactionDetail`, morphs `#detail-panel`
- **`ClearTransactions.kt`**: DELETE `/transactions` — clears store, returns 204
- **`TrafficStream.kt`**: SSE handler — subscribes to store, pushes new rows via Datastar `sendPatchElements` with prepend mode
- **`ListViews.kt`**: GET `/views` — renders view bar
- **`CreateView.kt`**: POST `/views` — creates new named view
- **`UpdateView.kt`**: PUT `/views` — updates existing view
- **`DeleteView.kt`**: DELETE `/views` — deletes user-created view
- **`TransactionFilterSignals.kt`**: Datastar signal deserialisation for filter state
- **`shared.kt`**: Shared rendering utilities

### Chaos Panel (`chaos/`)

- **`Chaos.kt`**: `WiretapFunction` assembling chaos routes; `mcp()` exports activate/deactivate/status tools
- **`Index.kt`**: GET `/chaos/` — static page, all state in Datastar signals
- **`Status.kt`**: GET `/chaos/status` — queries both engines, renders `ChaosStatusView` into `#chaos-status`
- **`Activate.kt`**: POST `/chaos/{direction}/activate` — reads `ChaosConfig` from Datastar signals, calls `engine.enable(config.toStage())`
- **`Deactivate.kt`**: POST `/chaos/{direction}/deactivate` — calls `engine.disable()`
- Side-by-side inbound/outbound panels with behaviour, filter, and trigger configuration

### OTel Trace Panel (`otel/`)

- **`OTel.kt`**: `WiretapFunction` assembling OTel routes; `mcp()` exports list/get trace tools
- **`Index.kt`**: GET `/otel/` — with optional `?trace=` deep-link via initial Datastar signals
- **`ListTraces.kt`**: GET `/otel/list` — groups spans by traceId, renders `TraceSummary` rows (root span name, service name, span count, duration, timestamp)
- **`ViewTrace.kt`**: GET `/otel/{traceId}` — builds span tree, flattens depth-first, renders Gantt chart
- **`WiretapSpanExporter.kt`**: Custom OTel `SpanExporter` feeding into `TraceStore`
- **`WiretapOpenTelemetry.kt`**: Builds `OpenTelemetrySdk` with W3C propagation and the custom exporter
- Deep linking: traffic detail has OTel button (`/__wiretap/otel?trace=`), OTel detail has Traffic button (`/__wiretap?trace=`)

### OpenAPI Panel (`openapi/`)

- **`OpenApi.kt`**: `WiretapFunction` that routes to Swagger UI index page; exports empty MCP capability
- **`Index.kt`**: GET handler for `/__wiretap/openapi` — renders `Index` ViewModel via TemplateRenderer
- **`Index.hbs`**: Handlebars template loading Swagger UI v5.32.0 from CDN (unpkg), points SwaggerUIBundle at `/openapi`
- Minimal implementation — all UI comes from CDN-hosted Swagger UI
- Nav link placed between OTel and Inbound Client in header

### Client Panel (`client/`) — Inbound + Outbound

- **`InboundClient.kt`**: `WiretapFunction` — sends requests through the proxy chain (record → chaos → app), so they appear in Traffic as inbound. `mcp()`
  exports `send_inbound_request` tool.
- **`OutboundClient.kt`**: `WiretapFunction` — sends requests directly to external services via httpClient, bypassing the app. `mcp()` exports
  `send_outbound_request` tool.
- **`Index.kt`**: GET — renders request builder page with Datastar signals for method, URL, header pairs, body
- **`SendRequest.kt`**: POST `/send` — reads request from Datastar signals, constructs `Request`, adds `x-http4k-wiretap: replay` header, sends through
  appropriate chain, returns response as `TransactionDetail` into `#client-response`
- **`FormatBody.kt`**: Body formatting utilities for the client panel
- **`HeaderRows.kt`**: Dynamic header row management (add/remove header key/value pairs)
- Two-column layout: left = request builder form, right = response viewer
- Toolbar: method select, URL input, Send button, Clear button, Import dropdown
- Replay identification: `isReplay` field in `TransactionSummary`, blue "R" badge + `tx-row-replay` highlight in traffic rows

### MCP Panel (`mcp/`) — Upstream MCP Client UI

Browser UI for interacting with the monitored app's MCP server.

- **`Mcp.kt`**: `WiretapFunction` — connects to upstream app's `/mcp` endpoint via `HttpNonStreamingMcpClient`. Auto-detects if MCP is available. `mcp()`
  returns empty `CapabilityPack`.
- **`Index.kt`**: GET `/mcp` — renders MCP panel with tabs for Tools, Prompts, Resources, Apps
- **`mcp/client/InboundClient.kt`**: Wires up sub-functions (Tools, Prompts, Resources) and Apps with `McpApps`
- **Tools** (`mcp/client/tools/`):
    - `Tools.kt` — lists available tools
    - `InspectTool.kt` — shows tool details and schema
    - `CallTool.kt` — executes a tool with parameters, displays result
    - Templates: `ToolDetailView.hbs`, `ToolResultView.hbs`
- **Prompts** (`mcp/client/prompts/`):
    - `Prompts.kt` — lists available prompts
    - `InspectPrompt.kt` — shows prompt details
    - `GetPrompt.kt` — retrieves prompt content
    - Templates: `PromptDetailView.hbs`, `PromptResultView.hbs`
- **Resources** (`mcp/client/resources/`):
    - `Resources.kt` — lists available resources
    - `InspectResource.kt` — shows resource details
    - `ReadResource.kt` — reads resource content
    - `InspectTemplate.kt` — shows resource template details
    - Templates: `ResourceDetailView.hbs`, `ResourceResultView.hbs`, `TemplateDetailView.hbs`
- **Apps** (`mcp/client/apps/`):
    - `Apps.kt` — lists MCP apps
- **Tab content views** (`mcp/tools/TabContent.kt`, `mcp/prompts/TabContent.kt`, `mcp/resources/TabContent.kt`, `mcp/apps/TabContent.kt`) — render tab contents
  for each category

### MCP API (`mcp_api/`) — Wiretap as MCP Server

Wiretap exposes its own MCP server at `/_wiretap/mcp`.

- **`WiretapMcp.kt`**: Assembles `mcpHttpStreaming` server from all `WiretapFunction.mcp()` capabilities + dedicated prompts. Takes `McpSecurity` parameter.
- **`AnalyzeTrafficPrompt.kt`**: AI prompt for traffic analysis
- **`DebugRequestPrompt.kt`**: AI prompt for debugging a specific transaction

### Utility Layer (`util/`)

- **`Json.kt`**: Moshi-based JSON utilities (`asFormatString`, `asToolResponse`)
- **`Metrics.kt`**: Creates `MeterRegistry` with JVM metrics binders (memory, threads, GC, CPU, classes)
- **`Templates.kt`**: Handlebars template setup with `CachingClasspath` and `infiniteLoops`
- **`prettify.kt`**: Body pretty-printing — dispatches on content type, handles JSON via Moshi and XML via XSLT

### UI Layer

**Templates** (Handlebars, `.hbs`):

- `layout.hbs` — HTML shell with Bootstrap 5.1.3 CSS, Bootstrap Icons, Datastar 1.0.0-RC.7, wiretap.css
- `header.hbs` — App header with logo, nav (Overview, Traffic, OTel, OpenAPI, Inbound Client, Outbound Client, Chaos), active link highlighting
- `footer.hbs` — Page footer
- `home/Index.hbs` — Overview page layout
- `home/StatsView.hbs` — Stats display with charts (latency histogram, traffic timeline, per-host timelines), JVM metrics, chaos status
- `traffic/Index.hbs` — Full traffic page with views, filters, list+detail panels, deep-link support
- `traffic/ViewBarView.hbs` — View buttons with filter signals, add/clear buttons
- `traffic/TransactionRowView.hbs` — Row with clickable filter cells, view button
- `traffic/TransactionDetailView.hbs` — Toolbar + two-column request/response with query params, headers, body
- `chaos/Index.hbs` — Side-by-side chaos config panels with conditional inputs
- `chaos/ChaosStatusView.hbs` — Active/inactive badges with engine descriptions
- `otel/Index.hbs` — Trace list + detail layout with deep-link support
- `otel/TraceRowView.hbs` — Trace summary row
- `otel/TraceDetailView.hbs` — Gantt chart with timeline, span bars, expandable detail sections
- `openapi/Index.hbs` — Swagger UI via CDN (v5.32.0), points at `/openapi` endpoint
- `client/Index.hbs` — Request builder with method/URL/headers/body form, response panel, import dropdown
- `client/HeaderRowsView.hbs` — Dynamic header row rendering
- `mcp/Index.hbs` — MCP panel with tabs
- `mcp/tools/TabContent.hbs`, `mcp/prompts/TabContent.hbs`, `mcp/resources/TabContent.hbs`, `mcp/apps/TabContent.hbs` — Tab content views
- `mcp/client/tools/ToolDetailView.hbs`, `mcp/client/tools/ToolResultView.hbs` — Tool inspection + result
- `mcp/client/prompts/PromptDetailView.hbs`, `mcp/client/prompts/PromptResultView.hbs` — Prompt inspection + result
- `mcp/client/resources/ResourceDetailView.hbs`, `mcp/client/resources/ResourceResultView.hbs`, `mcp/client/resources/TemplateDetailView.hbs` — Resource
  inspection + result + templates

**CSS** (`wiretap.css`):

- Blue gradient header with http4k pipes SVG background
- 9-column grid for transaction rows (direction, time, badge, method, host, path, status, latency, view)
- Status colours: 2xx green, 3xx yellow, 4xx orange, 5xx red
- Direction colours: inbound blue (#0FA8E8), outbound purple (#6f42c1)
- Chaos page: side-by-side panels, active=red / inactive=grey badges
- OTel: 6-column trace list, Gantt chart with positioned bars colour-coded by span kind
- Resizable panels with drag handles
- Client page: toolbar, two-column form/response layout, import dropdown, header rows
- Replay badge: blue circle "R" indicator, light blue row highlight
- Swagger UI overrides: hides server selector, version/title, expand controls
- Home/Overview: stats cards, chart containers, JVM metrics section

**JS** (`wiretap.js`):

- `initResizableColumns()` — drag handles between header cells, updates `--grid-cols` CSS variable
- `initResizablePanel()` — horizontal drag handle between list and detail panels

### External Changes

- **`HandlebarsTemplates.kt`** (`core/template/handlebars`): Used with `setInfiniteLoops(true)` for layout partial blocks
- **`datastarExtensions.kt`** (`core/web/datastar`): `Response.datastarElements()`, `Response.html()`, `Sse.sendPatchElements()`, `datastarModel<T>()` for
  signal deserialisation

### Tests

- **`WiretapIntegrationTest`** (2 tests): Records transactions via real Jetty server; verifies traceparent header captured from OTel tracing
- **`WiretapBodyHydrationTest`** (4 tests): Each `BodyHydration` variant correctly buffers/skips request and response streams
- **`ChaosConfigTest`** (6 tests): Filter trigger matching (empty, method, path substring, host, composed), stage composition
- **`TransactionViewsTest`** (5 tests): Path with/without query string, query params parsing (multiple params, valueless params, empty)
- **`TransactionMappingTest`**: Tests for `toSummary()`, `toDetail()` mapping functions
- **`TransactionMatchingTest`**: Tests for `matches(filter)` filtering logic
- **`TransactionStoreTest`**: Tests for TransactionStore operations (record, list, get, ring buffer eviction, subscriber notification)
- **`ViewStoreTest`**: Tests for ViewStore CRUD operations, built-in view protection
- **`TrafficMetricsTest`**: Tests for Micrometer counter recording, latency buckets, timeline snapshots
- **`StatsTest`**: Tests for GetStats WiretapFunction (HTTP + MCP responses)
- **`ClientTest`** (1 test): Client index page returns OK with HTML
- **`Runner.kt`**: Local dev runner on port 21000 with multi-span OTel traces, baggage, events for manual testing

## Outstanding Work

### Short-term

- Chaos row indicator: `chaosInfo` field exists in view model but no visual badge in row template
- Header sanitisation: `sanitise` parameter exists but no default sanitiser for Authorization, Cookie, etc.

### Client Panel — Next Steps

- Save requests to named collections
- Collection runner with assertions
- Environment variables (base URL, auth tokens) with switching

### Metrics — Next Steps

- Route-level metrics breakdown (slowest routes table)
- Dedicated filterable metrics panel (if separate from home page)

### MCP Panel — Next Steps

- MCP-aware traffic parsing (decode JSON-RPC messages in traffic panel)
- MCP conversation flow timeline
- Schema validation of MCP messages

### Future Panels (see PLAN.md)

- SSE/WS protocol monitoring
- TracerBullet / Mermaid sequence diagram integration
- Contract validation against OpenAPI spec
- Diff / regression mode
- Fake service toggling
- Request diffing
