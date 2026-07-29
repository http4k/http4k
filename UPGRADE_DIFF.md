# MCP SDK Upgrade Guide: `2025-11-25` → `2026-07-28`

This document details every substantive change between MCP specification revisions
`2025-11-25` and `2026-07-28`, with concrete implementation tasks for an SDK that
implements both client and server sides.

> **`2026-07-28` is a structural break, not an incremental release.** The protocol
> became **stateless**: there is no session, no `initialize`/`initialized` handshake,
> and every request self-describes its protocol version, capabilities, and identity in
> `_meta`. Server-initiated requests (sampling/roots/elicitation) are gone, replaced by
> the **Multi Round-Trip Requests (MRTR)** pattern. Tasks moved out of core into an
> extension. Roots, Sampling, and Logging are deprecated. Plan for a coordinated
> client+server bump — partial upgrades will not interoperate.

**Contents**

1. [Protocol Version & Lifecycle](#1-protocol-version--lifecycle)
2. [Transport (HTTP headers, SSE, sessions, error codes)](#2-transport)
3. [Multi Round-Trip Requests (MRTR)](#3-multi-round-trip-requests-mrtr)
4. [Tools](#4-tools)
5. [Resources](#5-resources)
6. [Prompts](#6-prompts)
7. [Roots](#7-roots)
8. [Sampling](#8-sampling)
9. [Elicitation](#9-elicitation)
10. [Utilities (ping, pagination, logging, caching, tasks, completion)](#10-utilities)
11. [Metadata / Tracing](#11-metadata--tracing)
12. [Authorization](#12-authorization)
13. [Schema / Type Changes](#13-schema--type-changes)
14. [Priority Summary](#14-priority-summary)

---

## 1. Protocol Version & Lifecycle

### 1.1 Stateless protocol — the `initialize` handshake is removed

**Before.** The connection opened with a mandatory three-message handshake, and version
+ capabilities were negotiated **once** for the whole session:

```json
{ "jsonrpc": "2.0", "id": 1, "method": "initialize",
  "params": {
    "protocolVersion": "2025-11-25",
    "capabilities": { "roots": { "listChanged": true }, "sampling": {} },
    "clientInfo": { "name": "ExampleClient", "version": "1.0.0" }
  } }
```
Server replied with its `protocolVersion`/`capabilities`/`serverInfo`/`instructions`,
then the client sent `notifications/initialized`.

**After.** No handshake exists. `initialize`, `InitializeResult`, and
`notifications/initialized` are all removed from the schema. Every request self-describes
via reserved `_meta` keys, and the server accepts or rejects each request independently:

| `_meta` key | Type | Required | Meaning |
| ------------------------------------------- | ------------------ | ---------------------- | ------- |
| `io.modelcontextprotocol/protocolVersion`   | `string`           | **MUST**               | Protocol version for this request |
| `io.modelcontextprotocol/clientCapabilities`| `ClientCapabilities` | **MUST**             | Client capabilities for this request |
| `io.modelcontextprotocol/clientInfo`        | `Implementation`   | SHOULD (unless configured off) | Client name/version |
| `io.modelcontextprotocol/logLevel`          | `LoggingLevel`     | optional (deprecated)  | Min log level for this request |

Servers **SHOULD** include `io.modelcontextprotocol/serverInfo` (`Implementation`) in
every result's `_meta`.

```json
"_meta": {
  "io.modelcontextprotocol/protocolVersion": "2026-07-28",
  "io.modelcontextprotocol/clientInfo": { "name": "ExampleClient", "version": "1.0.0" },
  "io.modelcontextprotocol/clientCapabilities": {}
}
```

Rules:
- A request missing `protocolVersion` or `clientCapabilities` is malformed → reject with
  `-32602` (Invalid params); on HTTP the status **MUST** be `400 Bad Request`.
- Servers **MUST NOT** infer capabilities from prior requests. An empty
  `clientCapabilities` object means "no optional capabilities".
- If the server needs a capability the client did not declare →
  `MissingRequiredClientCapabilityError` (`-32021`) with `data.requiredCapabilities`.

**SDK changes**
- **Client:** Delete the `initialize`/`initialized` state machine. Stamp
  `protocolVersion` + `clientCapabilities` (and normally `clientInfo`) into `_meta` on
  **every** outbound request. Capabilities are now declared per-request, not once.
- **Server:** Read version/capabilities from each request's `_meta` — never from cached
  connection state. Implement `server/discover` (§1.2). Emit `serverInfo` in each result.
  Reject malformed `_meta` with `-32602` and undeclared-capability requests with `-32021`.

### 1.2 New `server/discover` RPC

**Before.** The `initialize` response doubled as the discovery surface for versions,
capabilities, and identity.

**After.** New RPC `server/discover`. Servers **MUST** implement it. Clients **MAY** call
it (up-front version selection) but are not required to — they may fire any RPC inline and
handle `UnsupportedProtocolVersionError`.

Request (`params` is just `_meta`):
```json
{ "jsonrpc": "2.0", "id": "discover-1", "method": "server/discover",
  "params": { "_meta": {
    "io.modelcontextprotocol/protocolVersion": "2026-07-28",
    "io.modelcontextprotocol/clientCapabilities": {}
  } } }
```

Result (`DiscoverResult` — note `supportedVersions` is a **list**, `serverInfo` moved to
`_meta`, and it is a `CacheableResult`):
```json
{ "jsonrpc": "2.0", "id": "discover-1", "result": {
    "resultType": "complete",
    "supportedVersions": ["2026-07-28"],
    "capabilities": { "tools": {}, "resources": {} },
    "instructions": "This server provides weather and resource utilities.",
    "ttlMs": 3600000,
    "cacheScope": "public",
    "_meta": { "io.modelcontextprotocol/serverInfo": { "name": "ExampleServer", "version": "1.0.0" } }
  } }
```

**SDK changes**
- **Client:** Optionally call `server/discover` up front to present identity/capabilities
  and choose a version. Use it as the STDIO dual-era probe (§2.6).
- **Server:** Implement `server/discover` (MUST). Populate `supportedVersions`,
  `capabilities`, `serverInfo` (in `_meta`), and the required `ttlMs`/`cacheScope`.

### 1.3 Version negotiation is per-request

**Before.** Negotiated once during `initialize`; the chosen version rode subsequent
requests via the `MCP-Protocol-Version` header (missing header defaulted to `2025-03-26`).

**After.** No negotiation step. Every request declares its version in
`_meta.io.modelcontextprotocol/protocolVersion` (mirrored to the `MCP-Protocol-Version`
header on HTTP — the two **MUST** match, else `HeaderMismatch` `-32020`). Unsupported
version → `UnsupportedProtocolVersionError` (`-32022`), HTTP `400`, with `data.supported`:

```json
{ "jsonrpc": "2.0", "id": 1, "error": {
    "code": -32022, "message": "Unsupported protocol version",
    "data": { "supported": ["2026-07-28", "2025-11-25"], "requested": "1900-01-01" }
  } }
```
The client **SHOULD** pick a mutually-supported version from `supported` and retry.

**SDK changes**
- **Client:** Handle `-32022` by re-selecting a version from `data.supported`. Cache the
  server's era per process (stdio) / origin (HTTP).
- **Server:** Validate the requested version per request; emit `-32022` with the supported
  list when unsupported.

---

## 2. Transport

### 2.1 Protocol-level sessions removed (`Mcp-Session-Id` gone)

**Before.** Streamable HTTP had a session mechanism: server minted an `Mcp-Session-Id` at
initialization, client echoed it on all subsequent requests, `404` forced a re-`initialize`,
and `DELETE` terminated the session. State was implicitly bound to the session/connection.

**After.** Sessions are gone. A server that only supports `2026-07-28` **SHOULD**:
- return `405 Method Not Allowed` for `GET`/`DELETE` on the MCP endpoint;
- **ignore** any `Mcp-Session-Id` header (never mint or echo one);
- ignore any `Last-Event-ID` header (streams are not resumable).

Cross-call state is now referenced by an **explicit, server-minted handle the client
passes on each request** (e.g. a task ID threaded through tool arguments). A stdio process
is **not** a session — the SDK must not scope conversation state to it.

**SDK changes**
- **Client:** Remove `Mcp-Session-Id` echo logic and the 404 re-init loop.
- **Server:** Stop minting/reading session IDs; return `405` for GET/DELETE. Surface any
  cross-call state as an opaque handle in results, consumed back via tool args.

### 2.2 List endpoints no longer vary per-connection

**Before.** `tools/list`/`resources/list`/`prompts/list` results could vary per session.

**After.** The advertised set **MUST NOT** vary per-connection or as a side effect of other
requests. It MAY vary by the authorization presented (credentials are per-request input,
not connection state). This — together with caching (§5.3) — enables client-side caching.

### 2.3 New required HTTP headers; `x-mcp-header` custom headers

The Streamable HTTP transport now mirrors selected body fields into headers so
intermediaries can route without parsing the body. The body remains source of truth.

| Header | Source | Required for |
| ---------------------- | ----------------------------------------------- | ------------ |
| `MCP-Protocol-Version` | `_meta.io.modelcontextprotocol/protocolVersion` | Every POST (**MUST**, must match body) |
| `Mcp-Method`           | `method`                                        | All requests (**new**) |
| `Mcp-Name`             | `params.name` or `params.uri`                   | `tools/call`, `resources/read`, `prompts/get` (**new**) |
| `Mcp-Param-{Name}`     | tool arg annotated `x-mcp-header: "{Name}"`     | when the tool declares it |

```http
POST /mcp HTTP/1.1
Content-Type: application/json
MCP-Protocol-Version: 2026-07-28
Mcp-Method: tools/call
Mcp-Name: get_weather

{ "jsonrpc": "2.0", "id": 1, "method": "tools/call",
  "params": { "name": "get_weather", "arguments": { "location": "Seattle, WA" },
    "_meta": { "io.modelcontextprotocol/protocolVersion": "2026-07-28",
               "io.modelcontextprotocol/clientCapabilities": {} } } }
```

`x-mcp-header` mirrors a tool argument into an `Mcp-Param-{Name}` header. Constraints: HTTP
token syntax, no CR/LF, case-insensitively unique, primitive types only (not `number`), and
only on properties reachable through a pure chain of `properties` keys (no `items`,
composition, conditionals, `$ref`). Clients **MUST** exclude tools with an invalid
`x-mcp-header` from `tools/list`. Values with non-ASCII/control/leading-trailing-whitespace
(and `Mcp-Name`) **MUST** be Base64-wrapped: `=?base64?SGVsbG8=?=`.

**HeaderMismatch validation.** A server that processes the body **MUST** reject any request
where a header disagrees with the body, or a required header is missing/malformed →
HTTP `400` + JSON-RPC `-32020`:
```json
{ "jsonrpc": "2.0", "id": 1, "error": {
    "code": -32020,
    "message": "Header mismatch: Mcp-Name header value 'foo' does not match body value 'bar'" } }
```

**SDK changes**
- **Client:** Attach `Mcp-Method`, `Mcp-Name`, and any `Mcp-Param-*` headers on every POST
  with the Base64 sentinel encoding. On a `Mcp-Param-*` `HeaderMismatch`, refresh via
  `tools/list` and retry. Drop tools with invalid `x-mcp-header` from listings.
- **Server:** Validate header↔body agreement; emit `-32020` on mismatch/missing required
  header. Support extracting `x-mcp-header`-annotated args.

### 2.4 SSE resumability removed

**Before.** SSE events could carry an `id`; on disconnect the client reconnected with a
`Last-Event-ID` header and the server **MAY** replay missed messages.

**After.** Removed. No SSE event IDs, no `Last-Event-ID`, no replay, no priming event. A
broken response stream loses the in-flight request; the client **MUST** re-issue it as a
**new request with a new JSON-RPC ID**. For long-lived streams, re-send `subscriptions/listen`.
Servers **SHOULD** emit SSE comment keep-alives (`:\r\n`) and set `X-Accel-Buffering: no`.

**SDK changes**
- **Client:** Delete `Last-Event-ID` tracking/replay. Replace "resume stream" with
  "re-issue as a new request".
- **Server:** Stop assigning event IDs / buffering for replay. Add SSE-comment keep-alive
  and `X-Accel-Buffering: no` on long-lived streams.

### 2.5 `subscriptions/listen` replaces the GET stream + subscribe/unsubscribe

**Before.** Server→client change notifications arrived on a standalone SSE stream opened via
HTTP **GET**; resource change subscriptions used `resources/subscribe`/`resources/unsubscribe`.

**After.** A single long-lived request, `subscriptions/listen`, carries an opt-in filter.
The server **MUST NOT** send notification types the client did not request.

| Filter field | Type | Delivers |
| ----------------------- | ---------- | -------- |
| `toolsListChanged`      | `boolean`  | `notifications/tools/list_changed` |
| `promptsListChanged`    | `boolean`  | `notifications/prompts/list_changed` |
| `resourcesListChanged`  | `boolean`  | `notifications/resources/list_changed` |
| `resourceSubscriptions` | `string[]` | `notifications/resources/updated` for the listed URIs (replaces `resources/subscribe`) |

Open request:
```json
{ "jsonrpc": "2.0", "id": 1, "method": "subscriptions/listen",
  "params": { "_meta": { "io.modelcontextprotocol/protocolVersion": "2026-07-28",
                         "io.modelcontextprotocol/clientCapabilities": {} },
    "notifications": { "toolsListChanged": true,
                       "resourceSubscriptions": ["file:///project/config.json"] } } }
```

Acknowledgement — the server **MUST** send this **first**, echoing the honored subset
(unsupported types omitted):
```json
{ "jsonrpc": "2.0", "method": "notifications/subscriptions/acknowledged",
  "params": { "_meta": { "io.modelcontextprotocol/subscriptionId": 1 },
    "notifications": { "toolsListChanged": true,
                       "resourceSubscriptions": ["file:///project/config.json"] } } }
```

Every message on the stream (ack, notifications, final response) carries
`io.modelcontextprotocol/subscriptionId` in `_meta` — **the value is the JSON-RPC `id` of
the `subscriptions/listen` request**. On stdio, clients demultiplex by it.

**Two-stream routing rule (hard requirement):**
- **Request-scoped** notifications (`notifications/progress`, `notifications/message`) flow
  **only** on the response stream of the request they relate to — never on the listen stream.
- **Long-lived change** notifications flow **only** on the `subscriptions/listen` stream,
  and only for opted-in types.

Graceful close — when the server ends the subscription, it **SHOULD** respond to the
original request with an empty result before closing:
```json
{ "jsonrpc": "2.0", "id": 1,
  "result": { "resultType": "complete", "_meta": { "io.modelcontextprotocol/subscriptionId": 1 } } }
```

**SDK changes**
- **Client:** Remove the GET-stream handler and `resources/subscribe`/`unsubscribe`. Open a
  `subscriptions/listen` request with the filter; demux by `subscriptionId`; re-send after a
  stdio reconnect (server holds no subscription state).
- **Server:** Implement `subscriptions/listen`; send the acknowledgement first; tag every
  message with `subscriptionId`; enforce the two-stream routing rule; never send unrequested
  types.

### 2.6 Dual-era detection

- **stdio:** send `server/discover` first. `DiscoverResult` → modern; recognized modern
  error (`-32020/-32021/-32022`) → modern but retry/correct; any other error or timeout →
  legacy, fall back to the `initialize` handshake. Do **not** key the fallback to one error
  code.
- **HTTP:** attempt a modern request; on `400`, inspect the body — a recognized modern
  JSON-RPC error means modern (retry/correct); an empty/unrecognized body means fall back to
  `initialize` (and possibly to deprecated HTTP+SSE).

Era is a property of the **server**; cache it and re-probe if a cached assumption fails.

### 2.7 Error code allocation policy

The implementation-defined range `-32000..-32099` is now partitioned:
- `-32000..-32019` — **implementation-defined** (existing SDK usage grandfathered; the spec
  will never define codes here; receivers must not assign cross-implementation meaning).
- `-32020..-32099` — **reserved for the MCP specification**, allocated sequentially from
  `-32020`.

| Name | Old code | New code | Status |
| --------------------------------- | -------- | -------- | ------ |
| `HeaderMismatch`                  | `-32001` | **`-32020`** | renumbered |
| `MissingRequiredClientCapability` | `-32003` | **`-32021`** | renumbered |
| `UnsupportedProtocolVersion`      | `-32004` | **`-32022`** | renumbered |
| Resource not found                | `-32002` | **`-32602`** | changed to standard Invalid Params |
| URL elicitation required          | `-32042` | — | removed, code permanently reserved |

> Note on the renumbering: the codes `-32001`/`-32003`/`-32004` appear in the changelog as
> the values these errors carried while introduced during the draft cycle. The
> `2025-11-25`-era schema only ever shipped `-32042` (`URL_ELICITATION_REQUIRED`) and the
> resource-not-found `-32002` in prose; treat the table above as the target values your SDK
> must emit for `2026-07-28`.

**SDK changes**
- **Client:** Still **accept** `-32002` from legacy servers. Understand `-32020/-32021/-32022`.
- **Server:** Emit `-32020/-32021/-32022` for the three protocol errors; emit `-32602`
  (not `-32002`) for resource-not-found and malformed `_meta`. Never emit an undefined code
  in `-32020..-32099`.

---

## 3. Multi Round-Trip Requests (MRTR)

**This is the single largest behavioral change.** Server-initiated requests are removed.

**Before.** A server processing a `tools/call` could send a `sampling/createMessage`,
`roots/list`, or `elicitation/create` **request** (with its own `id`) back to the client,
block on the reply, then continue. This required sticky, stateful connections.

**After.** Servers **MUST NOT** initiate requests. Instead the server **responds to the
client's own request** with an `InputRequiredResult` (`resultType: "input_required"`)
carrying the sub-requests; the client gathers the input and **retries the original request**
(with a **new** `id`) carrying `inputResponses` plus the echoed `requestState`.

MRTR is permitted **only** on `tools/call`, `prompts/get`, and `resources/read`.

**Round-trip example:**

1. Client's initial request:
```json
{ "jsonrpc": "2.0", "id": 2, "method": "tools/call",
  "params": { "name": "get_weather", "arguments": { "location": "New York" } } }
```

2. Server's input-required response:
```json
{ "jsonrpc": "2.0", "id": 2, "result": {
    "resultType": "input_required",
    "inputRequests": {
      "github_login": {
        "method": "elicitation/create",
        "params": { "mode": "form", "message": "Please provide your GitHub username",
          "requestedSchema": { "type": "object",
            "properties": { "name": { "type": "string" } }, "required": ["name"] } }
      }
    },
    "requestState": "eyJsb2NhdGlvbiI6Ik5ldyBZb3JrIn0..."
  } }
```

3. Client's retry (**new `id`**, original params + `inputResponses` + echoed `requestState`):
```json
{ "jsonrpc": "2.0", "id": 3, "method": "tools/call",
  "params": { "name": "get_weather", "arguments": { "location": "New York" },
    "inputResponses": { "github_login": { "action": "accept", "content": { "name": "octocat" } } },
    "requestState": "eyJsb2NhdGlvbiI6Ik5ldyBZb3JrIn0..." } }
```

4. Server's final result (`resultType: "complete"`).

**Rules:**
- Server **MUST** include at least one of `inputRequests` or `requestState`.
- Server **MUST NOT** request a capability the client did not declare.
- The initial and retry `id` **MUST** differ (independent requests).
- Client **MUST** echo `requestState` exactly (or omit if none). It is opaque — the client
  **MUST NOT** inspect/parse/modify it.
- If only `requestState` is present (no `inputRequests`), the client MAY retry immediately
  (a load-shedding signal).
- `inputResponses`/`requestState` affect **only** the retry of that one request.
- Server treats `requestState` as attacker-controlled: integrity-protect (HMAC/AEAD) if it
  drives authz, embed principal + short TTL + originating-request id to bound replay.

**SDK changes**
- **Client:** Model sampling/elicitation/roots as fields inside a normal response to
  `tools/call`/`prompts/get`/`resources/read`. Add a collect-and-retry loop with opaque
  `requestState` passthrough. Only satisfy `inputRequests` for capabilities you declared.
- **Server:** Replace outbound `sampling/createMessage`/`roots/list`/`elicitation/create`
  calls with returning an `InputRequiredResult`. Mint and integrity-protect `requestState`;
  resume processing when the retry arrives with matching `inputResponses`.

---

## 4. Tools

### 4.1 `resultType` on results

Every result now carries a required `resultType` (`"complete"` | `"input_required"`).
A `tools/call` result:
```json
{ "jsonrpc": "2.0", "id": 2, "result": {
    "resultType": "complete",
    "content": [ { "type": "text", "text": "Current weather in New York:\n..." } ],
    "isError": false } }
```
Clients **MUST** treat an **absent** `resultType` (older servers) as `"complete"`, and
**MUST** reject any unrecognized value as invalid.

### 4.2 `tools/call` can enter MRTR

A `tools/call` response may be an `InputRequiredResult` (§3). The `CallToolResultResponse`
wrapper's `result` is now `CallToolResult | InputRequiredResult`.

### 4.3 Input/output schema loosening (SEP-2106)

**Before.** `inputSchema`/`outputSchema` were `{ $schema?; type: "object"; properties?; required? }`.

**After.** Any JSON Schema 2020-12 keyword is allowed alongside `type` — `oneOf`, `anyOf`,
`allOf`, `not`, `if`/`then`/`else`, `$ref`/`$defs`/`$anchor`, etc. `inputSchema` still
requires `type: "object"` at the root; `outputSchema` no longer forces a root `type`.

```ts
inputSchema:  { $schema?: string; type: "object"; [key: string]: unknown }
outputSchema?: { $schema?: string; [key: string]: unknown }
```

`$ref` resolution requirements: implementations **MUST NOT** auto-dereference `$ref` values
resolving to a network URI; any opt-in fetch mode **MUST** be disabled by default, enforce
host allowlists, reject loopback/link-local/private addresses, and apply timeout/size limits.
Schemas that fail to validate due to an unresolved external `$ref` **SHOULD** be rejected.

### 4.4 `structuredContent` accepts any JSON value

`structuredContent?: { [key: string]: unknown }` → `structuredContent?: unknown`. It may now
be an object, array, string, number, boolean, or null (conforming to `outputSchema` if set).
Same change on `ToolResultContent.structuredContent`.

### 4.5 Deterministic ordering (SHOULD)

Servers **SHOULD** return tools from `tools/list` in a deterministic order (stable across
requests while the tool set is unchanged) to improve client caching and LLM prompt-cache hits.

### 4.6 `execution` / `taskSupport` removed

The `ToolExecution` interface and `Tool.execution` field (which held
`taskSupport: "forbidden"|"optional"|"required"`) are removed — task support moved to the
tasks extension (§10.5).

**SDK changes**
- **Client:** Emit/parse `resultType`; default missing → `"complete"`. Handle
  `InputRequiredResult` from `tools/call`. Validate against full 2020-12 schemas; apply the
  `$ref` safety rules. Accept non-object `structuredContent`. Drop `Tool.execution`.
- **Server:** Include `resultType: "complete"` on results. Populate `ttlMs`/`cacheScope` on
  `tools/list` (§5.3). Emit tools in a stable order. Remove `execution` from Tool.

---

## 5. Resources

### 5.1 `resources/read` supports MRTR and caching

`ReadResourceRequestParams` now extends `InputResponseRequestParams` (may carry
`inputResponses`/`requestState`). `ReadResourceResultResponse.result` is
`ReadResourceResult | InputRequiredResult`. `ReadResourceResult` now extends `CacheableResult`.

### 5.2 Subscriptions moved to `subscriptions/listen`

`resources/subscribe`/`resources/unsubscribe` are removed; per-resource change subscriptions
are the `resourceSubscriptions` filter of `subscriptions/listen` (§2.5).
`notifications/resources/updated` and `notifications/resources/list_changed` are now only
delivered on an opted-in listen stream.

### 5.3 Caching hints (`CacheableResult`) — SEP-2549

**Before.** No caching fields; clients relied only on `listChanged` notifications.

**After.** Servers **MUST** include two fields on `resultType: "complete"` results from
`server/discover`, `tools/list`, `prompts/list`, `resources/list`,
`resources/templates/list`, and `resources/read`:

- `ttlMs` — freshness hint in ms (`Cache-Control: max-age` semantics), `>= 0`. `0` = stale
  immediately; absent → treat as `0` (older servers); negative → treat as `0`.
- `cacheScope` — `"public"` (no user-specific data; shared caches/proxies may reuse across
  auth contexts) or `"private"` (reuse only within the same auth context).

```json
{ "jsonrpc": "2.0", "id": 1, "result": {
    "resultType": "complete",
    "tools": [ /* ... */ ],
    "nextCursor": "next-page-cursor",
    "ttlMs": 300000, "cacheScope": "public" } }
```

Interim `input_required` results are not cacheable. Results from a request carrying
`inputResponses`/`requestState` **MUST NOT** be cached. Cache key = method +
result-affecting params. Each list page carries its own `ttlMs`, but all pages of one
request share the same `cacheScope`. A `listChanged` notification invalidates a fresh entry.

**SDK changes**
- **Client:** Remove `resources/subscribe`/`unsubscribe`; subscribe via `subscriptions/listen`.
  Add a TTL/scope-aware cache keyed by method+params; honor `listChanged` invalidation.
- **Server:** Populate `ttlMs`/`cacheScope` on the six listed methods. Support MRTR on
  `resources/read`.

---

## 6. Prompts

- `GetPromptRequestParams` now extends `InputResponseRequestParams` (MRTR-capable).
- `GetPromptResultResponse.result` is `GetPromptResult | InputRequiredResult`.
- `ListPromptsResult` now extends `CacheableResult` (`ttlMs`/`cacheScope` required).
- `notifications/prompts/list_changed` is delivered only on an opted-in `subscriptions/listen`
  stream (`promptsListChanged` filter).
- New `ListPromptsResultResponse` / `GetPromptResultResponse` wrapper types.

**SDK changes**
- **Client:** Handle `InputRequiredResult` from `prompts/get`; cache `prompts/list` by TTL.
- **Server:** Emit `ttlMs`/`cacheScope` on `prompts/list`; support MRTR on `prompts/get`.

---

## 7. Roots

**Deprecated (SEP-2577)** and delivered via MRTR.

**Before.** `roots/list` was a server-initiated request; the client advertised
`roots.listChanged` and pushed `notifications/roots/list_changed`.

**After.**
- Roots is **deprecated** — new implementations should pass directories/files via tool
  parameters, resource URIs, or server config instead.
- `roots/list` is no longer a top-level JSON-RPC request — it travels inside
  `InputRequiredResult.inputRequests`; the client returns `ListRootsResult` inside
  `inputResponses` on the retry.
- `notifications/roots/list_changed` is **removed**; the `roots` capability no longer has
  `listChanged` (`roots?: {}`).
- `ListRootsRequest`/`ListRootsResult` no longer extend `JSONRPCRequest`/`Result`.

Capability now declared per-request:
```json
{ "_meta": { "io.modelcontextprotocol/clientCapabilities": { "roots": {} } } }
```

**SDK changes**
- **Client:** Answer `roots/list` inside `inputResponses`. Remove `list_changed` emission.
  Mark the roots API deprecated.
- **Server:** Request roots via `inputRequests` (only if the client declared `roots`). Stop
  expecting `notifications/roots/list_changed`.

---

## 8. Sampling

**Deprecated (SEP-2577)** and delivered via MRTR.

**Before.** `sampling/createMessage` was a server-initiated request.

**After.**
- Sampling is **deprecated** — integrate directly with LLM provider APIs instead.
- `sampling/createMessage` params/results are **unchanged in shape** (messages,
  `modelPreferences`, `tools`/`toolChoice`, `stopReason: "toolUse"` loop, required
  `maxTokens`), but the request is delivered inside `InputRequiredResult.inputRequests` and
  the `CreateMessageResult` is returned inside `inputResponses`.
- `CreateMessageRequest` no longer extends `JSONRPCRequest`; `CreateMessageResult` extends
  only `SamplingMessage` (not `Result`). `CreateMessageRequestParams` no longer extends
  `TaskAugmentedRequestParams`.
- `includeContext` values `"thisServer"`/`"allServers"` are **deprecated** (SEP-2596) — omit
  the field or use `"none"`. Multi-turn tool loops map onto repeated `tools/call` retries.
- `metadata?: object` → `metadata?: JSONObject`.

Capability declared per-request via
`_meta.io.modelcontextprotocol/clientCapabilities.sampling` (`{}`, `{ "tools": {} }`, or
`{ "context": {} }`).

**SDK changes**
- **Client:** Answer sampling inside `inputResponses`. Mark the sampling API deprecated. Stop
  relying on `"thisServer"`/`"allServers"` context.
- **Server:** Request sampling via `inputRequests` (only if declared). Model tool loops as
  successive MRTR retries; you no longer block on a client response.

---

## 9. Elicitation

**Before.** URL-mode elicitation used a server-generated `elicitationId`, an out-of-band
`notifications/elicitation/complete` notification, and a dedicated
`URLElicitationRequiredError` (`-32042`).

**After.** All three are removed and folded into MRTR:
- URL-mode params are now just `mode: "url"`, `url`, `message` (no `elicitationId`).
- `notifications/elicitation/complete` — removed. `URLElicitationRequiredError` (`-32042`) —
  removed (code permanently reserved).
- Correlation across retries uses the opaque `requestState` (server-encoded), not
  `elicitationId`.
- Elicitation requests are delivered inside `InputRequiredResult.inputRequests`; the
  `ElicitResult` is returned inside `inputResponses` on the retry. `ElicitRequest`/
  `ElicitResult`/`ElicitRequest*Params` no longer extend `JSONRPCRequest`/`Result`/
  `TaskAugmentedRequestParams`.
- The three-action model (`accept`/`decline`/`cancel`) and form-mode schema subset are
  unchanged. For URL mode the server determines completion from the echoed `requestState`
  (or its own stored state) on retry; clients SHOULD provide manual retry/cancel controls.

Capability declared per-request:
```json
{ "_meta": { "io.modelcontextprotocol/clientCapabilities": { "elicitation": { "form": {}, "url": {} } } } }
```

**SDK changes**
- **Client:** Delete `elicitationId`, the `notifications/elicitation/complete` handler, and
  `-32042`. Drive URL-mode completion via MRTR retry. Add manual retry/cancel UI for URL mode.
- **Server:** Deliver elicitation via `inputRequests`; encode any correlation id inside
  `requestState`. Remove the completion notification and `-32042`.

---

## 10. Utilities

### 10.1 Ping removed

The `ping` utility (`PingRequest`) is removed entirely with no schema replacement. Remove it
from both client and server; drop any keep-alive built on it (use transport keep-alives —
SSE comments on HTTP).

### 10.2 Logging — `logging/setLevel` removed, per-request level

**Before.** Clients set a connection-wide level via `logging/setLevel`; servers pushed
`notifications/message` freely.

**After.** `logging/setLevel` is removed. Log level is set **per-request** via
`_meta.io.modelcontextprotocol/logLevel`. The server **MUST NOT** emit
`notifications/message` for a request that omitted this field. `notifications/message` is
now **request-scoped** — it flows only on that request's response stream, before the final
result. Invalid level → `-32602`. (Logging is also **deprecated** overall — SEP-2577;
migrate to `stderr` on stdio or OpenTelemetry.)

**SDK changes**
- **Client:** Remove `logging/setLevel`; opt in per request via `_meta.logLevel`.
- **Server:** Remove `logging/setLevel`; only emit `notifications/message` for requests that
  set `logLevel`, scoped to that request's stream.

### 10.3 Pagination

`PaginatedRequest.params` is now **required** (`params: PaginatedRequestParams`, was
optional) — the mandatory `_meta` must always be present. `ListToolsResult`/
`ListResourcesResult`/`ListResourceTemplatesResult`/`ListPromptsResult` now also extend
`CacheableResult` (§5.3). No change to the cursor mechanism itself.

### 10.4 Completion

- New `CompleteResultResponse` wrapper type (`result: CompleteResult`).
- `CompleteResult.completion.values` now documents `@maxItems 100`.
- Otherwise unchanged.

### 10.5 Tasks — moved from core to an extension

**Before.** Tasks were an experimental **core** feature: `task` param augmentation →
`CreateTaskResult`; polling `tasks/get`; blocking `tasks/result`; `tasks/list`;
`tasks/cancel`; `notifications/tasks/status`; per-request/per-tool opt-in via the `tasks`
capability and `Tool.execution.taskSupport`.

**After.** Tasks are **removed from core** and live in the official extension
`io.modelcontextprotocol/tasks`, negotiated via the new `extensions` capability field.
Redesign:

| Aspect | `2025-11-25` (core) | `2026-07-28` (extension) |
| ------------------ | ------------------------------------- | ------------------------------------ |
| Result retrieval   | `tasks/result` (blocks until terminal)| polling via `tasks/get`              |
| Client→server input| via `input_required` stream           | new `tasks/update` method            |
| Listing            | `tasks/list` (paginated)              | **removed**                          |
| Opt-in             | per-request `task` + `execution.taskSupport` | unsolicited handles allowed (no per-request opt-in) |
| Location           | core `basic/utilities/tasks`          | `io.modelcontextprotocol/tasks` extension |

Advertised as `{ "extensions": { "io.modelcontextprotocol/tasks": {} } }`. All core task
types are removed from the schema: `Task`, `TaskStatus`, `TaskMetadata`,
`RelatedTaskMetadata`, `CreateTaskResult`, `Get/List/Cancel` task requests/results,
`TaskStatusNotification`, `TaskAugmentedRequestParams`, `ToolExecution`.

**SDK changes**
- **Client/Server:** Move any task support into an optional extension module keyed by
  `io.modelcontextprotocol/tasks`. Remove all `tasks/*` methods, the `task` request-param,
  `Tool.execution`, and `notifications/tasks/status` from core. Full task semantics now live
  in the extension docs, not the versioned spec.

---

## 11. Metadata / Tracing

### 11.1 `_meta` structure formalized

`_meta` now has a typed hierarchy (`MetaObject` base with documented key-naming rules and
reserved prefixes):

- `RequestMetaObject` (on every request; `_meta` is now **required** on `RequestParams`) —
  `progressToken?`, plus the reserved `io.modelcontextprotocol/*` keys (§1.1).
- `NotificationMetaObject` — `io.modelcontextprotocol/subscriptionId?`.
- `ResultMetaObject` — `io.modelcontextprotocol/serverInfo?`.

Reserved-prefix rule: any prefix whose **second label** is `modelcontextprotocol` or `mcp`
is reserved for MCP (e.g. `io.modelcontextprotocol/`, `dev.mcp/`). All the loose
`_meta?: { [key: string]: unknown }` fields across content/resource/prompt types became
`_meta?: MetaObject`.

### 11.2 OpenTelemetry trace context (SEP-414)

As an exception to the reverse-DNS prefix rule, three **unprefixed** `_meta` keys are
reserved for trace propagation: `traceparent`, `tracestate`, `baggage` (W3C Trace
Context / Baggage formats).

```json
{ "jsonrpc": "2.0", "id": 2, "method": "tools/call",
  "params": { "name": "get_weather", "arguments": { "location": "New York" },
    "_meta": { "traceparent": "00-0af7651916cd43dd8448eb211c80319c-00f067aa0ba902b7-01" } } }
```

**SDK changes**
- **Client/Server:** Propagate `traceparent`/`tracestate`/`baggage` transparently through
  `_meta` (and honor them for span context) without applying the prefix requirement.

---

## 12. Authorization

| Change | Requirement | Migration |
| ------ | ----------- | --------- |
| `iss` validation (RFC 9207) | Client **MUST** validate a present `iss` against the recorded issuer before redeeming the code; reject if AS advertises `authorization_response_iss_parameter_supported: true` but `iss` is absent. Applies to error responses too. Compare via exact string match (no normalization). | Record issuer alongside PKCE/state. |
| `application_type` in DCR | Client **MUST** specify an appropriate `application_type` (native/desktop/mobile/CLI/localhost → `"native"`; remote browser → `"web"`) to avoid OIDC redirect-URI conflicts. | Set it explicitly; omitting defaults to `"web"` under OIDC. |
| Credential-issuer binding | Client **MUST** key persisted credentials by AS `issuer`, **MUST NOT** reuse across ASes, **MUST** re-register when the AS changes. | CIMD `client_id`s are portable and need no re-registration. |
| Client ID Metadata Documents (CIMD) | Preferred registration mechanism. `client_id` is an HTTPS URL (with a path) to a JSON doc containing at least `client_id`, `client_name`, `redirect_uris`; doc `client_id` must equal the URL. AS advertises `client_id_metadata_document_supported: true`. | New priority: pre-registered → CIMD → DCR fallback → prompt user. |
| DCR / RFC 7591 | **Deprecated** in favor of CIMD (still available for backward compatibility). | Adopt CIMD. |

```json
{ "client_id": "https://app.example.com/oauth/client-metadata.json",
  "client_name": "Example MCP Client",
  "redirect_uris": ["http://127.0.0.1:3000/callback", "http://localhost:3000/callback"],
  "grant_types": ["authorization_code"], "response_types": ["code"],
  "token_endpoint_auth_method": "none" }
```

**SDK changes**
- **Client:** Add `iss` validation and issuer-keyed credential storage; set `application_type`
  during DCR; implement CIMD with DCR fallback.
- **Server (AS side, if applicable):** Include `iss` in authorization responses; advertise
  and accept CIMD.

---

## 13. Schema / Type Changes

`LATEST_PROTOCOL_VERSION`: `"2025-11-25"` → `"2026-07-28"`. `JSONRPC_VERSION` unchanged.

### 13.1 New types

**JSON value types** — replace bare `object` in capability/metadata fields:
```ts
export type JSONValue = string | number | boolean | null | JSONObject | JSONArray;
export type JSONObject = { [key: string]: JSONValue };
export type JSONArray = JSONValue[];
```

**`_meta` hierarchy:**
```ts
export type MetaObject = Record<string, unknown>;
export interface RequestMetaObject extends MetaObject {
  progressToken?: ProgressToken;
  "io.modelcontextprotocol/protocolVersion": string;                // required
  "io.modelcontextprotocol/clientInfo"?: Implementation;
  "io.modelcontextprotocol/clientCapabilities": ClientCapabilities; // required
  "io.modelcontextprotocol/logLevel"?: LoggingLevel;                // @deprecated
}
export interface NotificationMetaObject extends MetaObject { "io.modelcontextprotocol/subscriptionId"?: RequestId; }
export interface ResultMetaObject extends MetaObject { "io.modelcontextprotocol/serverInfo"?: Implementation; }
```

**Result infrastructure:**
```ts
export type ResultType = "complete" | "input_required" | string;
export interface CacheableResult extends Result { ttlMs: number; cacheScope: "public" | "private"; }
```

**MRTR types:**
```ts
export type InputRequest  = CreateMessageRequest | ListRootsRequest | ElicitRequest;   // @internal
export type InputResponse = CreateMessageResult | ListRootsResult | ElicitResult;      // @internal
export interface InputRequests  { [key: string]: InputRequest; }
export interface InputResponses { [key: string]: InputResponse; }
export interface InputRequiredResult extends Result { inputRequests?: InputRequests; requestState?: string; }
export interface InputResponseRequestParams extends RequestParams { inputResponses?: InputResponses; requestState?: string; }
```

**Discovery / subscriptions:** `DiscoverRequest`, `DiscoverResult`, `DiscoverResultResponse`,
`SubscriptionFilter`, `SubscriptionsListenRequest(Params)`, `SubscriptionsListenResult`,
`SubscriptionsListenResultMetaObject`, `SubscriptionsListenResultResponse`,
`SubscriptionsAcknowledgedNotification(Params)`.

**`*ResultResponse` JSON-RPC wrappers (all new):**

| Wrapper | `result` type |
| ------------------------------------ | -------------------------------------------- |
| `DiscoverResultResponse`             | `DiscoverResult`                             |
| `ListToolsResultResponse`            | `ListToolsResult`                            |
| `CallToolResultResponse`             | `CallToolResult \| InputRequiredResult`      |
| `ListResourcesResultResponse`        | `ListResourcesResult`                        |
| `ListResourceTemplatesResultResponse`| `ListResourceTemplatesResult`                |
| `ReadResourceResultResponse`         | `ReadResourceResult \| InputRequiredResult`  |
| `ListPromptsResultResponse`          | `ListPromptsResult`                          |
| `GetPromptResultResponse`            | `GetPromptResult \| InputRequiredResult`     |
| `CompleteResultResponse`             | `CompleteResult`                             |
| `SubscriptionsListenResultResponse`  | `SubscriptionsListenResult`                  |

**Error interfaces (new):** `ParseError`, `InvalidRequestError`, `MethodNotFoundError`,
`InvalidParamsError`, `InternalError` (standard-code discriminators), plus
`HeaderMismatchError` (`-32020`), `MissingRequiredClientCapabilityError` (`-32021`,
`data.requiredCapabilities`), `UnsupportedProtocolVersionError` (`-32022`,
`data.{supported, requested}`).

### 13.2 Removed types

| Removed | Replacement |
| ------- | ----------- |
| `InitializeRequest(Params)`, `InitializeResult`, `InitializedNotification` | `DiscoverRequest`/`DiscoverResult`; per-request `_meta` |
| `PingRequest` | — |
| `SetLevelRequest(Params)` (`logging/setLevel`) | `_meta.io.modelcontextprotocol/logLevel` |
| `SubscribeRequest(Params)`, `UnsubscribeRequest(Params)` | `SubscriptionFilter.resourceSubscriptions` |
| `RootsListChangedNotification` | — (roots deprecated) |
| `ElicitationCompleteNotification`, `ElicitRequest…params.elicitationId` | MRTR + `requestState` |
| `URLElicitationRequiredError` / `URL_ELICITATION_REQUIRED` (`-32042`) | — (code reserved) |
| `Task`, `TaskStatus`, `TaskMetadata`, `RelatedTaskMetadata`, `CreateTaskResult`, `Get/List/Cancel` task types, `TaskStatusNotification`, `TaskAugmentedRequestParams` | `io.modelcontextprotocol/tasks` extension |
| `ToolExecution` + `Tool.execution` | tasks extension |
| `ServerRequest` union | **removed** — server no longer initiates requests (MRTR) |

### 13.3 Changed existing types

| Type | Before → After |
| ---- | -------------- |
| `RequestParams._meta` | `{ progressToken?; [k]: unknown }` optional → **required** `RequestMetaObject` |
| `Result` | adds required `resultType: ResultType`; `_meta?: ResultMetaObject` |
| `NotificationParams._meta` | `{ [k]: unknown }` → `NotificationMetaObject` |
| `CancelledNotificationParams.requestId` | optional → **required**; client→server only |
| `ClientCapabilities` | `experimental`/`sampling.*`/`elicitation.*` retyped `object`→`JSONObject`; `roots` → `{}` (no `listChanged`, deprecated); `tasks` **removed**; `extensions?: { [k]: JSONObject }` **added** |
| `ServerCapabilities` | `experimental`/`logging`/`completions` retyped → `JSONObject` (logging deprecated); `tasks` **removed**; `extensions?: { [k]: JSONObject }` **added** |
| `Tool.inputSchema` | `{ $schema?; type:"object"; properties?; required? }` → `{ $schema?; type:"object"; [k]: unknown }` |
| `Tool.outputSchema` | `{ …; type:"object"; properties?; required? }` → `{ $schema?; [k]: unknown }` (no forced root type) |
| `Tool.execution` | **removed** |
| `CallToolResult.structuredContent`, `ToolResultContent.structuredContent` | `{ [k]: unknown }` → `unknown` |
| `ReadResourceResult` | `extends Result` → `extends CacheableResult` |
| `ListResources/ListResourceTemplates/ListPrompts/ListToolsResult` | `+ CacheableResult` |
| `PaginatedRequest.params` | optional → **required** |
| `CreateMessageRequest`, `ElicitRequest`, `ListRootsRequest` | no longer `extends JSONRPCRequest` (now `InputRequest` members) |
| `CreateMessageResult`, `ElicitResult`, `ListRootsResult` | no longer `extends Result` (now `InputResponse` members) |
| `CreateMessageRequestParams.metadata` | `object` → `JSONObject`; `includeContext` values deprecated |
| `Call/GetPrompt/ReadResource RequestParams` | now extend `InputResponseRequestParams` |
| many `_meta?: { [k]: unknown }` | → `_meta?: MetaObject` (Resource, Prompt, all Content types, Root, SamplingMessage, Tool, …) |

### 13.4 Union type changes

| Union | After |
| ----- | ----- |
| `ClientRequest` | `Discover`, `Complete`, `GetPrompt`, `ListPrompts`, `ListResources`, `ListResourceTemplates`, `ReadResource`, `SubscriptionsListen`, `CallTool`, `ListTools` (dropped: Ping, Initialize, SetLevel, Subscribe, Unsubscribe, all Task) |
| `ClientNotification` | **`CancelledNotification`** only (dropped: Progress, Initialized, RootsListChanged, TaskStatus) |
| `ClientResult` | **`EmptyResult`** only (sampling/roots/elicit results now travel as `InputResponse`) |
| `ServerRequest` | **removed entirely** |
| `ServerNotification` | Cancelled, Progress, LoggingMessage, ResourceUpdated, ResourceListChanged, ToolListChanged, PromptListChanged, **SubscriptionsAcknowledged** (dropped: ElicitationComplete, TaskStatus) |
| `ServerResult` | adds `Discover`, `SubscriptionsListen`, `InputRequired`; drops `Initialize`, all Task |

### 13.5 Error code constants

| Constant | 2025-11-25 | 2026-07-28 |
| ------------------------------------ | ---------- | ---------- |
| `PARSE_ERROR` / `INVALID_REQUEST` / `METHOD_NOT_FOUND` / `INVALID_PARAMS` / `INTERNAL_ERROR` | standard | unchanged |
| `URL_ELICITATION_REQUIRED` | `-32042` | removed (reserved) |
| `HEADER_MISMATCH` | — | `-32020` |
| `MISSING_REQUIRED_CLIENT_CAPABILITY` | — | `-32021` |
| `UNSUPPORTED_PROTOCOL_VERSION` | — | `-32022` |

`schema.json` also now correctly types `minimum`/`maximum`/`default` as `number` (not
`integer`) — a generator fix (PR #2710), no behavioral change.

---

## 14. Priority Summary

### Must-Have (protocol compliance — MUST / MUST NOT)

1. **Stateless requests.** Stamp `io.modelcontextprotocol/protocolVersion` (MUST) +
   `clientCapabilities` (MUST) into every request's `_meta`; servers read them per request.
   Reject missing → `-32602` / HTTP `400`.
2. **Remove `initialize`/`initialized`.** Delete the handshake on both sides.
3. **Implement `server/discover`** (server MUST).
4. **`resultType` on every result** (server MUST emit; client MUST default absent →
   `"complete"`, MUST reject unknown values).
5. **MRTR.** Server MUST NOT initiate requests; return `InputRequiredResult` and consume the
   retry's `inputResponses`/`requestState`. Client MUST echo `requestState` opaquely, use a
   new `id` on retry, and only satisfy declared capabilities. Allowed only on `tools/call`,
   `prompts/get`, `resources/read`.
6. **`subscriptions/listen`.** Replace GET stream + `resources/subscribe`/`unsubscribe`;
   server MUST send the acknowledgement first, MUST NOT send unrequested types, MUST tag
   every message with `subscriptionId`; enforce the two-stream routing rule.
7. **Remove sessions.** Drop `Mcp-Session-Id`; return `405` for GET/DELETE.
8. **Remove SSE resumability.** Drop `Last-Event-ID`/event IDs; re-issue broken streams as
   new requests.
9. **HTTP mirror headers.** Required `Mcp-Method`/`Mcp-Name` (+ `MCP-Protocol-Version` must
   match body); validate header↔body and emit `HeaderMismatch` `-32020`; support
   `x-mcp-header`/`Mcp-Param-*` with Base64 sentinel encoding.
10. **Error codes.** Emit `-32020`/`-32021`/`-32022` and `-32602` (resource-not-found);
    still accept `-32002` from legacy servers; never emit undefined `-32020..-32099`.
11. **Logging per-request.** Remove `logging/setLevel`; server MUST NOT emit
    `notifications/message` unless the request set `io.modelcontextprotocol/logLevel`.
12. **Remove `ping`.**
13. **Auth (if implemented).** Validate `iss` (RFC 9207); set `application_type` in DCR; key
    credentials by issuer and re-register on AS change.

### Should-Have (recommended — SHOULD)

1. Include `io.modelcontextprotocol/clientInfo` / `serverInfo` in requests/results.
2. Call `server/discover` up front (and as the STDIO dual-era probe) — RECOMMENDED even for
   modern-only clients.
3. Return tools from `tools/list` in a **deterministic order**.
4. Emit SSE-comment keep-alives + `X-Accel-Buffering: no` on long-lived streams.
5. Adopt Client ID Metadata Documents; treat DCR/RFC 7591 as deprecated fallback.
6. Stop using `includeContext: "thisServer"`/`"allServers"`; omit or use `"none"`.
7. Migrate off deprecated Roots/Sampling/Logging (tool params / provider APIs / stderr+OTel);
   keep implemented for the ≥12-month window but mark deprecated in the public API.
8. Propagate OpenTelemetry `traceparent`/`tracestate`/`baggage` through `_meta`.

### Schema / Type Updates

1. Add `JSONValue`/`JSONObject`/`JSONArray`; replace bare `object` in capabilities/metadata.
2. Add the `_meta` hierarchy (`MetaObject`/`RequestMetaObject`/`NotificationMetaObject`/
   `ResultMetaObject`); make `RequestParams._meta` required.
3. Add `ResultType` + `resultType` to `Result`; add `CacheableResult` (`ttlMs`/`cacheScope`)
   and mix into the six list/read/discover results.
4. Add MRTR types (`InputRequests`/`InputResponses`/`InputRequiredResult`/
   `InputResponseRequestParams`) and rebase `Call/GetPrompt/ReadResource` params on them.
5. Add all `*ResultResponse` wrapper types (some union with `InputRequiredResult`).
6. Add discovery + subscription types; remove `Initialize*`/`Ping`/`SetLevel`/`Subscribe`/
   `Unsubscribe`/`RootsListChanged`/all `Task*`/`ToolExecution`/`ServerRequest`.
7. Retype `structuredContent` → `unknown`; loosen `inputSchema`/`outputSchema` to full
   2020-12; add the `$ref`-resolution safety rules.
8. Update error constants (`-32020`/`-32021`/`-32022`; drop `-32042`) and the new `*Error`
   interfaces.
9. Update the six union types (`ClientRequest`/`ClientNotification`/`ClientResult`/
   `ServerRequest`/`ServerNotification`/`ServerResult`).
10. Move all task machinery into an optional `io.modelcontextprotocol/tasks` extension module.
