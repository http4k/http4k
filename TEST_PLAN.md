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

All follow the ActivateTest pattern: implement `HttpWiretapFunctionContract` + `McpWiretapFunctionContract`, approval-tested. HTTP and MCP tests mirror each
other — same behaviour, different face.

### DeactivateTest (new file)

- [ ] `http disables inbound chaos` — POST `/Inbound/deactivate`, assert engine disabled + status rendered
- [ ] `mcp disables inbound chaos` — call tool with direction=Inbound, assert engine disabled

### StatusTest (new file)

- [ ] `http returns inactive status` — GET `/status` with both engines off
- [ ] `http returns active status` — enable chaos first, then GET `/status`
- [ ] `mcp returns inactive status` — call tool, verify both engines inactive
- [ ] `mcp returns active status` — enable chaos first, call tool

---

## 3. WiretapFunction Tests — Traffic

### ClearTransactionsTest (new file)

- [ ] `http clears all transactions` — record some, DELETE `/`, assert store empty
- [ ] `mcp clears all transactions` — record some, call tool, assert store empty

### ListTransactionsTest (new file)

- [ ] `http lists recorded transactions` — record transactions, POST `/list`, verify rows rendered
- [ ] `http returns empty list when no transactions` — POST `/list` on empty store
- [ ] `mcp lists recorded transactions` — record transactions, call tool, verify JSON
- [ ] `mcp returns empty list when no transactions`

### ViewTransactionTest (new file)

- [ ] `http renders transaction detail` — record transaction, GET `/{id}`, verify detail
- [ ] `http returns 404 for unknown id` — GET `/{unknownId}`, assert NOT_FOUND
- [ ] `mcp returns transaction detail` — record transaction, call tool with id
- [ ] `mcp returns error for unknown id` — call tool with nonexistent id

### ExportHarTest (new file)

- [ ] `http exports transaction as HAR JSON` — record transaction, GET `/{id}/har`, verify HAR structure
- [ ] `http returns 404 for unknown id`
- [ ] `mcp exports transaction as HAR JSON` — call tool with id
- [ ] `mcp returns error for unknown id`

### ListViewsTest (new file)

- [ ] `http renders view bar` — GET `/views`, verify default views rendered
- [ ] `mcp lists all views` — call tool, verify default views in JSON

### CreateViewTest (new file)

- [ ] `http creates view and renders updated bar` — POST `/views` with name+filter, verify view added
- [ ] `mcp creates view and returns updated list` — call tool with name+filter

### UpdateViewTest (new file)

- [ ] `http updates view filter` — create view, PUT `/views/{id}` with new filter, verify updated
- [ ] `mcp updates view filter` — create view, call tool with id + new filter

### DeleteViewTest (new file)

- [ ] `http removes view and renders updated bar` — create view, DELETE `/views/{id}`, verify removed
- [ ] `mcp removes view` — create view, call tool with id, verify removed

### ActivateViewTest (new file — http-only, no MCP face)

- [ ] `http applies view filter and renders filtered transactions` — create view with filter, record matching+non-matching transactions, POST
  `/views/{id}/activate`, verify only matching shown

---

## 4. WiretapFunction Tests — Home

### StatsTest (add MCP test to existing file)

- [ ] `mcp returns stats as JSON` — call `get_stats` tool, verify stats structure

---

## 5. WiretapFunction Tests — OTel

### ListTracesTest (new file)

- [ ] `http renders empty trace list` — GET `/list` with no spans
- [ ] `http renders traces after recording spans` — record spans, GET `/list`
- [ ] `mcp returns empty list when no traces` — call tool
- [ ] `mcp returns traces after recording spans` — record spans, call tool

### GetTraceTest (new file)

- [ ] `http renders trace detail with span tree` — record spans, GET `/{traceId}`
- [ ] `http returns 404 for unknown trace` — GET `/{unknownId}`
- [ ] `mcp returns trace detail` — record spans, call tool with trace_id
- [ ] `mcp returns error for unknown trace` — call tool with nonexistent trace_id

---

## 6. WiretapFunction Tests — Client

### SendRequestTest (new file)

- [ ] `http sends request through proxy and renders response detail` — POST `/send` with method+url, verify proxy called and detail rendered
- [ ] `mcp sends request and returns response detail` — call tool with method+url

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
