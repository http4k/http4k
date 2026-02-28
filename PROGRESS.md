# Wiretap — Implementation Progress

## Module Structure

- **Module**: `pro/tools/wiretap` (gradle: `:http4k-tools-wiretap`)
- **Dependencies**: http4k-core, http4k-format-moshi, http4k-ops-opentelemetry, http4k-realtime-core, http4k-template-handlebars, http4k-testing-chaos,
  http4k-web-datastar
- **Test deps**: http4k-testing-hamkrest, http4k-server-jetty

## What's Built

### Entry Point (`Wiretap.kt`)

- `Wiretap.Http(filterStore, dependencies, bodyHydration, idSource, appBuilder)` returns `PolyHandler`
- Combines HTTP routes (dashboard + transparent proxy) with SSE handler (real-time traffic stream)
- `ServerFilters.CatchAll()` wraps everything
- `WiretapAppBuilder` functional interface: `(HttpHandler, OpenTelemetry, Clock) -> HttpHandler`
- `WiretapDependencies` holds httpClient (default `JavaHttpClient()`) and clock; builds `OpenTelemetrySdk` via `.oTel(traceStore)`

### Transparent Proxy (`Proxy.kt`)

- `orElse` bound — catches all requests not matched by `/__wiretap` routes
- **Inbound chain**: bufferBodies -> recordTransaction(Inbound) -> inboundChaos -> user's app
- **Outbound chain**: bufferBodies -> recordTransaction(Outbound) -> outboundChaos -> httpClient
- `BodyHydration` enum (All, RequestOnly, ResponseOnly, None) controls stream buffering
- `recordTransaction` uses `ResponseFilters.ReportHttpTransaction` for timing, wraps in `WiretapTransaction`, stores, and notifies subscribers

### Domain Layer

- **`TransactionStore`**: Interface with `record`, `list`, `get`, `subscribe`, `clear`. `InMemory` uses `ConcurrentLinkedDeque` ring buffer (500 max) +
  `CopyOnWriteArrayList` subscribers.
- **`TraceStore`**: Interface with `record`, `list`, `traces`, `get`, `clear`. `InMemory` uses `ConcurrentLinkedDeque` ring buffer (5000 max). `traces()` groups
  by traceId.
- **`FilterStore`**: Interface with `list`, `add`, `update`, `remove`. `InMemory` + `File` (Moshi JSON persistence). Three built-in tabs (All, Inbound,
  Outbound) protected from mutation.
- **`WiretapTransaction`**: `id: Long`, `transaction: HttpTransaction`, `direction: Direction`
- **`Direction`**: Enum `Inbound | Outbound`
- **`FilterConfig`**: Optional direction, host, method, status, path. `normalize()` converts blanks to null.
- **`ChaosConfig`**: Behaviour (ReturnStatus/Latency/NoBody), trigger (Always/PercentageBased/Once/Countdown), request filter (method/path/host). Methods:
  `toBehaviour()`, `toTrigger()`, `toFilterTrigger()`, `toStage()`.
- **`WiretapSpanExporter`**: OTel `SpanExporter` that feeds spans into `TraceStore`.

### Traffic Panel

- **`Index`**: GET `/` — full page render with optional `?trace=` deep-link (looks up transaction by trace ID)
- **`ListTransactions`**: GET `/transactions` — reads `FilterConfig` from Datastar signals, filters list, renders `TransactionRowView` elements, morphs
  `#tx-list`
- **`ViewTransaction`**: GET `/transactions/{id}` — renders `TransactionDetailView`, morphs `#detail-panel`
- **`ClearTransactions`**: DELETE `/transactions` — clears store, returns 204
- **`TrafficStream`**: SSE handler — subscribes to store, pushes new rows via Datastar `sendPatchElements` with prepend mode
- **`Tabs`**: GET/POST/PUT/DELETE `/tabs` — tab bar CRUD, re-renders `#tab-bar`

**`TransactionRowView`** computes: id, direction, method, uri, path (with query string), host (outbound only, with port), status, durationMs, timestamp,
direction styling classes, status class, chaos info.

**`TransactionDetailView`** computes: all row fields + queryParams (parsed from URI), requestHeaders, responseHeaders, requestBody, responseBody, curl (via
`toCurl()`), traceId + shortTraceId (from traceparent header).

### Chaos Panel

- **`Index`**: GET `/chaos/` — static page, all state in Datastar signals
- **`Status`**: GET `/chaos/status` — queries both engines, renders `ChaosStatusView` into `#chaos-status`
- **`Activate`**: POST `/chaos/{direction}/activate` — reads `ChaosConfig` from Datastar signals, calls `engine.enable(config.toStage())`
- **`Deactivate`**: POST `/chaos/{direction}/deactivate` — calls `engine.disable()`
- Side-by-side inbound/outbound panels with behaviour, filter, and trigger configuration

### OTel Trace Panel

- **`Index`**: GET `/otel/` — with optional `?trace=` deep-link via initial Datastar signals
- **`ListTraces`**: GET `/otel/list` — groups spans by traceId, renders `TraceRowView` (root span name, service name, span count, duration, timestamp)
- **`ViewTrace`**: GET `/otel/{traceId}` — builds span tree, flattens depth-first, renders Gantt chart
- **`SpanNodeView`**: `startOffsetPercent`, `widthPercent` (min 0.5%), depth, attributes (excluding baggage-prefixed), baggageAttributes (prefix stripped),
  events with attributes
- **`TraceDetailView`**: traceId, flattened spans, shortTraceId, totalDurationMs
- Deep linking: traffic detail has OTel button (`/__wiretap/otel?trace=`), OTel detail has Traffic button (`/__wiretap?trace=`)

### Client Panel

- **`Index`**: GET `/client/` — renders request builder page with Datastar signals for method, URL, 5 header pairs, body
- **`SendRequest`**: POST `/client/send` — reads `ClientRequest` from Datastar signals, constructs `Request`, adds `x-http4k-wiretap: replay` header, sends
  through proxy chain, returns response as `TransactionDetailView` (reused from traffic panel) into `#client-response`
- **`ImportTransaction`**: GET `/client/import/{id}` — looks up captured transaction, returns `datastarSignals` to populate form fields
- **`ImportList`**: GET `/client/import` — lists recent 20 transactions as clickable items in dropdown, renders into `#client-import-list`
- **`ClientRequest`**: Data class for Datastar signal deserialization (method, url, h1k..h5k, h1v..h5v, body)
- Two-column layout: left = request builder form, right = response viewer
- Toolbar: method select, URL input, Send button, Clear button, Import dropdown
- Replay identification: `isReplay` field added to `TransactionRowView`, blue "R" badge + `tx-row-replay` highlight in traffic rows

**Wiring changes:**

- `Wiretap.kt`: Extracts `proxy` handler from `Proxy()`, passes to `WiretapUi`
- `WiretapUi.kt`: Accepts `proxy: HttpHandler`, adds `Client(...)` to routes
- `Header.hbs`: Added "Client" nav link between OTel and Chaos

### UI Layer

**Templates** (Handlebars, `.hbs`):

- `layout.hbs` — HTML shell with Bootstrap 5.1.3 CSS, Bootstrap Icons, Datastar 1.0.0-RC.7, wiretap.css
- `Header.hbs` — App header with logo, nav (Traffic, OTel, Client, Chaos), active link highlighting
- `traffic/Index.hbs` — Full traffic page with tabs, filters, list+detail panels, deep-link support
- `traffic/TabBarView.hbs` — Tab buttons with filter signals, add/clear buttons
- `traffic/TransactionRowView.hbs` — Row with clickable filter cells, view button
- `traffic/TransactionDetailView.hbs` — Toolbar + two-column request/response with query params, headers, body
- `chaos/Index.hbs` — Side-by-side chaos config panels with conditional inputs
- `chaos/ChaosStatusView.hbs` — Active/inactive badges with engine descriptions
- `otel/Index.hbs` — Trace list + detail layout with deep-link support
- `otel/TraceRowView.hbs` — Trace summary row
- `otel/TraceDetailView.hbs` — Gantt chart with timeline, span bars, expandable detail sections
- `client/Index.hbs` — Request builder with method/URL/headers/body form, response panel, import dropdown

**CSS** (`wiretap.css`, ~1180 lines):

- Blue gradient header with http4k pipes SVG background
- 9-column grid for transaction rows (direction, time, badge, method, host, path, status, latency, view)
- Status colours: 2xx green, 3xx yellow, 4xx orange, 5xx red
- Direction colours: inbound blue (#0FA8E8), outbound purple (#6f42c1)
- Chaos page: side-by-side panels, active=red / inactive=grey badges
- OTel: 6-column trace list, Gantt chart with positioned bars colour-coded by span kind
- Resizable panels with drag handles
- Client page: toolbar, two-column form/response layout, import dropdown, header rows
- Replay badge: blue circle "R" indicator, light blue row highlight

**JS** (`wiretap.js`, ~80 lines):

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
- **`ClientTest`** (1 test): Client index page returns OK with HTML
- **`Runner.kt`**: Local dev runner on port 21000 with multi-span OTel traces, baggage, events for manual testing

## Outstanding Work

### Short-term

- Column resize JS uses `--grid-cols` CSS variable — may need updating for header variants
- Host filter input always visible in filter bar — could hide when direction is Inbound
- Chaos row indicator: `chaosInfo` field exists in view model but no visual badge in row template
- JSON pretty-printing / XML indentation in body view
- Header sanitisation (Authorization, Cookie, etc.)

### Client Panel — Next Steps

- Save requests to named collections
- Collection runner with assertions
- Environment variables (base URL, auth tokens) with switching

### Future Panels (see PLAN.md)

- Metrics panel (histograms from traffic store)
- MCP panel (protocol-aware traffic parsing, tool tester)
- SSE/WS protocol monitoring
- TracerBullet / Mermaid sequence diagram integration
