# Wiretap Test Plan

Tests to add, organised by category. Prune before implementation.

## Architecture Summary

Every feature is a `WiretapFunction` with dual faces:

- `http()` → browser UI (Datastar elements)
- `mcp()` → MCP tool/prompt for AI clients

Some functions are **http-only** (`mcp()` returns empty `CapabilityPack`):
ActivateView, OpenApi, and the entire `mcp/` package (browser UI for upstream MCP server).

**Composites** (wire individual functions together — not tested directly):
Chaos, Traffic, OTel, InboundClient, OutboundClient, Mcp.

**Non-WiretapFunction routing handlers** (plain `RoutingHttpHandler`):
EditHeaders, FormatBody, TrafficStream, and all `Index` functions.

---

## 1. Domain — Fill Gaps in Existing Tests

### TransactionStoreTest (add to existing file)

- [ ] `get returns recorded transaction by id`
- [ ] `get returns null for unknown id`
- [ ] `clear removes all transactions`
- [ ] `list filters by status`
- [ ] `list filters by host`
- [ ] `list with cursor returns only transactions before cursor`
- [ ] `subscribe notifies on new transaction`
- [ ] `unsubscribe stops notifications`
- [ ] `evicts oldest when maxSize exceeded`

### ViewStoreTest (add to existing file)

- [ ] `list returns default views when created with defaults`
- [ ] `update modifies existing view`
- [ ] `remove deletes view by id`
- [ ] `remove nonexistent id is no-op`
- [ ] `built-in views are present in defaults`

### TraceStoreTest (new file)

- [ ] `record and get returns spans for trace id`
- [ ] `get returns empty list for unknown trace id`
- [ ] `traces groups spans by trace id`
- [ ] `evicts oldest spans when maxSpans exceeded`

### TransactionMatchingTest (add to existing file)

- [ ] `filters by direction`
- [ ] `filters by method`
- [ ] `filters by path substring`
- [ ] `filters by host substring`
- [ ] `combines multiple filter fields`
- [ ] `empty filter matches everything`

### BodyHydrationTest (new file — unit-level, not integration)

- [ ] `All returns true for Request`
- [ ] `All returns true for Response`
- [ ] `RequestOnly returns true for Request, false for Response`
- [ ] `ResponseOnly returns true for Response, false for Request`
- [ ] `None returns false for both`

---

## 2. WiretapFunction Tests — Chaos

All follow the ActivateTest pattern: implement `HttpWiretapFunctionContract` + `McpWiretapFunctionContract`, approval-tested.

### DeactivateTest (new file)

- [ ] `http default response` — POST `/{direction}/deactivate`, verify chaos disabled
- [ ] `mcp default response`

### StatusTest (new file)

- [ ] `http default response` — GET `/status`, verify status rendered
- [ ] `http returns active status after chaos enabled`
- [ ] `mcp default response`

---

## 3. WiretapFunction Tests — Traffic

### ClearTransactionsTest (new file)

- [ ] `http default response` — DELETE `/`, verify store cleared
- [ ] `mcp default response`

### ListTransactionsTest (new file)

- [ ] `http default response` — POST `/list` with empty filter
- [ ] `http filters by direction`
- [ ] `mcp default response`

### ViewTransactionTest (new file)

- [ ] `http default response` — GET `/{id}`, verify detail rendered
- [ ] `http returns 404 for unknown id`
- [ ] `mcp default response`
- [ ] `mcp returns error for unknown id`

### ExportHarTest (new file)

- [ ] `http default response` — GET `/{id}/har`, verify HAR JSON
- [ ] `http returns 404 for unknown id`
- [ ] `mcp default response`

### ListViewsTest (new file)

- [ ] `http default response` — GET `/views`
- [ ] `mcp default response`

### CreateViewTest (new file)

- [ ] `http default response` — POST `/views`, verify view added
- [ ] `mcp default response`

### UpdateViewTest (new file)

- [ ] `http default response` — PUT `/views/{id}`, verify view updated
- [ ] `mcp default response`

### DeleteViewTest (new file)

- [ ] `http default response` — DELETE `/views/{id}`, verify view removed
- [ ] `mcp default response`

### ActivateViewTest (new file — http-only, no MCP face)

- [ ] `http default response` — POST `/views/{id}/activate`, verify filter applied

---

## 4. WiretapFunction Tests — Home

### StatsTest (add MCP test to existing file)

- [ ] `mcp default response` — verify `get_stats` tool returns JSON

---

## 5. WiretapFunction Tests — OTel

### ListTracesTest (new file)

- [ ] `http default response` — GET `/list` when empty
- [ ] `http lists traces with data`
- [ ] `mcp default response`

### GetTraceTest (new file)

- [ ] `http default response` — GET `/{traceId}`, verify span tree rendered
- [ ] `http returns 404 for unknown trace`
- [ ] `mcp default response`
- [ ] `mcp returns error for unknown trace`

---

## 6. WiretapFunction Tests — Client

### SendRequestTest (new file)

- [ ] `http default response` — POST `/send`, verify request proxied and detail rendered
- [ ] `mcp default response`

---

## 7. MCP API — Wiretap's Own MCP Server

### AnalyzeTrafficPromptTest (new file)

- [ ] `prompt returns analysis instructions`

### DebugRequestPromptTest (new file)

- [ ] `prompt returns debug instructions with transaction id`

---

## Not in Scope

- **Composite aggregators**: Chaos, Traffic, OTel, InboundClient, OutboundClient, Mcp — these just wire things together
- **Routing-only Index functions**: home/Index, chaos/Index, traffic/Index, otel/Index, client/Index — template rendering only
- **MCP browser panel**: `mcp/` package (Tools, Prompts, Resources, Apps, InboundClient, Mcp) — UI for upstream MCP, http-only stubs
- **OpenApi**: stub with empty MCP face
- **Utilities**: Json, Metrics, Templates, prettify (prettify already tested via TransactionMappingTest)
- **Infrastructure**: Proxy, Wiretap, WiretapAppBuilder, WiretapUi, WiretapOpenTelemetry, WiretapSpanExporter, TrafficStream
- **Existing client tests**: ClientTest already covers InboundClient/OutboundClient index pages and import

---

## Verification

```bash
./gradlew :http4k-tools-wiretap:test
```
