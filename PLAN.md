# MCP `2026-07-28` support for http4k `pro/ai/mcp`

## Context

MCP `2026-07-28` is a **structural break, not an incremental release** (full delta in
`UPGRADE_DIFF.md`). The protocol becomes **stateless**: no session, no
`initialize`/`initialized` handshake; every request self-describes its protocol version,
capabilities and identity in `_meta`. Server-initiated requests (sampling / roots /
elicitation) are gone, replaced by **Multi Round-Trip Requests (MRTR)**. Tasks move out of
core into an extension. Roots, Sampling and Logging are deprecated. `ping`,
`logging/setLevel`, `resources/subscribe`, SSE resumability and `Mcp-Session-Id` are removed.

The current implementation is **heavily session-stateful**: `McpProtocol<Transport>` +
`RoutingMcpHandler` dispatch over a `Sessions<T>` map, per-session `ClientTracking`,
server→client calls pushed down a persistent SSE stream via `SessionBasedClient`, and a
`SessionEventStore` for replay. None of that survives statelessly, and the wire model itself
diverges so far that branching one codebase by version would be a tangle.

## Strategy: copy the whole subtree, then strip it to the new protocol

**Fork, don't branch.** Copy the existing MCP module subtree into a parallel `mcp-stateless/`
tree, get the copy green as an exact duplicate, then **migrate the copy down to only the
`2026-07-28` stateless protocol** — each step a *deletion/simplification*, not additive
construction. The existing modules are left **completely untouched**, so every current
dependant (wiretap, a2a-bridge, mpp, x402, conformance) keeps compiling. Endgame (a later
major version) swaps the stateless modules in as the default and drops/renames the old ones.

## Locked design decisions

1. **Parallel `mcp-stateless/` modules, identical packages, mutually exclusive on the classpath.**
   New modules under `pro/ai/mcp-stateless/` (path-based auto-discovery →
   `http4k-ai-mcp-stateless-*`, zero `settings.gradle.kts` edits). **Keep the same package
   names** (`org.http4k.ai.mcp.*`) in the copy so the endgame swap is just an artifact
   swap. Consequence (accepted): the stateful and stateless artifacts **cannot** sit on one
   classpath, so there is **no single-JVM dual-era endpoint** — a deployment picks an era.
   That matches "era is a property of the server".
2. **Transports: Streamable HTTP only.** In the copy, **drop the JSON-RPC, WebSocket, stdio
   and legacy SSE transports** (and their clients/sessions/tests/builders) — a big
   simplification so the hard Stage 2–3 stateless refactor targets a single HTTP transport.
   **stdio is a standard 2026 transport and will be re-added** as a thin stateless
   read-line→engine→write-line pump (see Follow-up); WebSocket (never a spec transport) is an
   optional re-add if demand appears.
3. **MRTR = re-entrant handlers, no server state.** A capability handler returns
   `InputRequired { inputRequests, requestState? }`; the client fulfils and **retries the same
   method (new id)** with `inputResponses` + echoed `requestState`. The server **re-runs the
   handler from the top**; answers arrive as `req.inputResponses[key]` (key chosen by the
   handler when it emitted the request). `requestState` is the only continuity thread — opaque,
   server-minted, echoed verbatim; integrity-protect it if it drives authz. Replaces
   `SessionBasedClient` + `ClientTracking` + blocking queues entirely. Explicit
   return-and-resume only; no suspend/resume sugar yet (YAGNI).
   - **Keep the standard handler typealiases** (`ToolHandler = (ToolRequest) -> ToolResponse`,
     `ResourceHandler`, `PromptHandler`, `CompletionHandler`) unchanged.
   - **`Client` is `NoOp` in the stateless engine.** No back-channel exists, so
     `ToolRequest.client` (`tools.kt:40`, already defaults to `Client.NoOp`) is never
     populated. Give `Client.NoOp` a clear "not available in stateless mode — return
     InputRequired" error so a mis-ported handler fails loudly.
   - **MRTR is additive on the existing request/response types.** Add defaulted
     `inputResponses`/`requestState` to `ToolRequest` (and resource/prompt request types);
     **generalise the existing `ToolResponse.ElicitationRequired` (`tools.kt:72`) into an
     `InputRequired`** variant covering elicitation + sampling + roots.
4. **Tasks: delete now, re-add fresh as an in-module extension (Stage 9).** (Pivoted from
   "park the files" — deleting is consistent with elicitation/sampling/roots and cleaner.) The
   whole tasks feature is **deleted** from the stateless tree now (Task model, `McpTask`,
   `Tasks`/`inMemoryTasks`/`TaskStorage`, `ToolExecution`, client/testing helpers, tests). It
   comes back **fresh** at Stage 9 as `io.modelcontextprotocol/tasks` (MCP-apps extension pattern:
   poll `tasks/get`, `tasks/update`, no session, no `tasks/list`), negotiated via `extensions`.
5. **No cross-era inheritance.** Because the modules are fully separate and self-contained,
   drop the earlier "existing interface extends a minimal one" idea. Each era owns its
   interfaces outright.

## Module topology

New tree, each a copy of its sibling then stripped:

Located at `pro/ai/mcp-stateless/` (sibling of `pro/ai/mcp/`); module names are path-derived
identically either way.

| New module dir | Gradle name | From |
| --- | --- | --- |
| `pro/ai/mcp-stateless/core` | `http4k-ai-mcp-stateless-core` | copy of `core/` |
| `pro/ai/mcp-stateless/sdk` | `http4k-ai-mcp-stateless-sdk` | copy of `sdk/` |
| `pro/ai/mcp-stateless/client` | `http4k-ai-mcp-stateless-client` | copy of `client/` |
| `pro/ai/mcp-stateless/testing` | `http4k-ai-mcp-stateless-testing` | copy of `testing/` |
| `pro/ai/mcp-stateless/conformance` | `http4k-ai-mcp-stateless-conformance` | copy of `conformance/` (deferred) |

Inter-module `project(...)` deps in the copied `build.gradle.kts` files re-point at the
`stateless-*` siblings (not the originals). External deps (`security-oauth`, formats, etc.)
are shared as-is.

## Stages

Each stage: RED → GREEN → REFACTOR; build green before the next. Status:
`[ ]` todo · `[~]` wip · `[x]` done.

### [x] Stage 0 — Fork the subtree (green duplicate baseline) — DONE
- Copied `core`/`sdk`/`client`/`testing` → `pro/ai/mcp-stateless/*`, same package names,
  inter-module deps re-pointed to the `stateless-*` siblings, descriptions tagged `(Stateless)`.
- Whole copied test suite **green** (`:http4k-ai-mcp-stateless-{core,sdk,client,testing}:test`
  BUILD SUCCESSFUL). `conformance` deferred. This is the safety net we strip against.

### [ ] Stage 1 — Drop the extra transports (one per step)
Keep only Streamable HTTP (`server/http/*`). Each step deletes server+client+sessions+tests+builder
and stays green.
- [x] 1a — JSON-RPC transport (`server/jsonrpc/*`, `client/jsonrpc/*`, `JsonRpcSessions`,
  `mcpJsonRpc` builder, 2 rebind tests) — removed, green
- [x] 1b — WebSocket transport (`server/websocket/*`, `client/websocket/*`, `WebsocketSessions`,
  `mcpWebsocket` builder, rebind test) **+ stdio** (`server/stdio/StdIoMcpSessions`, `mcpStdIo`
  builder) — removed, Acme example repointed to `mcp()`, unused imports pruned, green
- [x] 1c — legacy SSE transport (`server/sse/*`, `client/sse/*`, `SseSessions`, `/sse`+`/messages`,
  `mcpSse` builder, SSE rebind test) — removed, `FamilyAgent` example repointed to `mcp()`, green.
  Also deleted `McpProtocolTest` (11 SSE-only tests asserting init-loop + server→client
  roots/sampling/sessions — all removed in Stages 2–3; engine stays covered by
  `HttpStreamingMcpClientTest`). **SSE rebind-protection test kept as a `@Disabled` placeholder**
  in `McpRebindProtectionTest` to restore when SSE returns.
- [x] 1d — dropped the dead `mcpHttpStreaming` deprecated alias. **Kept `mcpHttpNonStreaming`** —
  non-streaming (JSON-only) is a valid Streamable-HTTP response mode and the natural serverless
  fit (used by the Lambda example). `Mcp.kt` now exposes `mcp()` + `mcpHttpNonStreaming()`. Green.

**Stage 1 complete** — only the Streamable HTTP transport remains (JSON-RPC / WebSocket / stdio /
legacy SSE all removed).

**Test-coverage debt to repay in later stages** (tracked so it isn't silently lost):
- Rebind protection: HTTP cases (`HttpStreamingMcp`/`HttpNonStreamingMcp`) still pass; SSE case is
  a `@Disabled` placeholder — restore with the SSE transport re-add. Verify the stateless HTTP
  transport keeps rebind protection through Stages 2–3.
- Protocol-engine unit tests: `McpProtocolTest` was deleted (SSE-coupled, tested removed
  behaviour). Stages 2–3 must add fresh **stateless** `StatelessMcpProtocolTest` coverage
  (`server/discover`, `_meta` validation, `resultType`, MRTR). Until then the engine is covered
  only end-to-end by `HttpStreamingMcpClientTest`.

### [ ] Stage 2 — Drop the removed features (one per step)
Each feature just disappears until (if) a later stage restores it in its new form. Every step
deletes the message type(s), the `RoutingMcpHandler` branch, client support, and tests, staying green.
- [x] 2a — `ping`: deleted `McpPing`, removed the `RoutingMcpHandler` branch, re-fixtured
  `ValidateMcpMethodHeaderTest` onto `McpTool.List.Request`. **`Event("ping")` SSE keep-alive
  deferred to Stage 3** (entangled with `HttpSessions`). NB: deleting a core message type needs a
  `core` clean rebuild (stale Kotshi/KSP adapter). Green.
- [x] 2b — **server→client-push teardown (reordered).** Discovered that elicitation-completion,
  `roots/list_changed`, and the whole elicitation/sampling/roots flow are inseparable from the
  server→client push subsystem, and that the current form is *replaced* by MRTR (no in-place
  adjustment possible). So rather than surgically extract sub-parts, **deleted the entire
  server→client-push subsystem and re-add via MRTR in Stage 4** — no new machinery needed first.
  Done (all four stateless suites green):
  - [x] elicitation **transport** (`McpElicitations` wire message + `Complete.Notification`,
    `URLElicitationRequiredError` `-32042`, `ToolResponse.ElicitationRequired`, client
    `elicitations()`/`ClientElicitations`, `TestingElicitations`, contract tests). **Kept the
    reusable model/DSL dead-but-present** — `Elicitation.kt`/`ElicitationModel.kt`/lens specs +
    `elicitations.kt` (`ElicitationRequest/Response/Handler`) + `ElicitationId` — because the DSL is
    coupled to them (option (b)); Stage 4 removes the now-dead `elicitationId` when re-adding via MRTR.
  - [x] sampling (`McpSampling`, `sampling.kt`, `ClientSampling`, `TestingSampling`, tests)
  - [x] roots (`McpRoot` incl. `notifications/roots/list_changed`, `Roots`/`inMemoryRoots`, roots
    client/testing, tests, `roots` param dropped from `McpProtocol`/`RoutingMcpHandler`)
  - [x] `SessionBasedClient` deleted → `RoutingMcpHandler` passes `Client.NoOp` to every capability
    call; `Client` trimmed to `progress/log/updateTask/storeTaskResult` (elicit/sample/requestRoots/
    elicitationComplete removed).
  - **NB tool-change + resource-update + progress-on-stream notifications were NOT removed** — they
    flow via the subscribe GET stream (Stage 3) / request response stream, and their tests still pass.
  - Re-adds tracked in **Stage 4** (elicitation via `InputRequired`; sampling/roots likewise, deprecated).

  **Tidy-up ledger (restore before/at the named stage):**
  - `Client.NoOp` progress/log are now **silent no-ops** (were `error()`); tasks methods deleted.
    Re-wire request-scoped: **progress** with the transport work (Stage 3), **logging** Stage 8.
    Stage 8 idea: the residual `Client` (progress + log) is really a request-scoped **notify** seam —
    consider renaming `Client` → `Notify`/`notify`. Also drop the client-side `setLevel`/`logging()`
    accessor (setLevel removed); client sets level per-request via `_meta.logLevel` and receives
    `notifications/message` on the request response stream.
  - Disabled tests (`@Disabled`, with reasons in-code): `TestMcpClientTest.deal with progress`
    (progress); `McpClientContract.task lifecycle …` (tasks/Stage 9). Plus the earlier
    `McpRebindProtectionTest` SSE placeholder.
  - Trimmed test: `TestMcpClientTest.deal with tools` lost its progress-emission assertion (kept
    tools list/call/onChange) — restore the progress assertion when progress is re-wired.
  - Dead-but-present elicitation model (`elicitations.kt`/DSL/`ElicitationId`, incl.
    `ElicitationRequest.Url.elicitationId`) — fold into the MRTR elicitation re-add in Stage 4.
  - **FIXME (3a):** `MetaKey.…().toLens()` is an *optional* getter (missing → null), so required
    reserved keys (`protocolVersion`/`clientCapabilities`) don't blow up on their own. `ReservedMetaKeysTest.
    missing required field returns null` is `@Disabled`. Enforce required-ness at the **3b** validation
    layer (`?: reject -32602`), or add a `required` MetaKey-lens variant that throws `LensFailure`.
  - **Deferred (Stage 5):** `MCP-Protocol-Version` header ↔ `_meta.protocolVersion` match — needs
    heterogeneous `_meta` extraction for a low-likelihood inconsistency (header is already the
    validated routing key). Revisit at conformance (Stage 11) if a strict run demands it.
  - **Deferred (Stage 5):** `x-mcp-header` Base64 `=?base64?…?=` sentinel for non-ASCII/control/
    whitespace `Mcp-Name`/`Mcp-Param` values (spec-MUST). Only bites on non-ASCII names, which don't
    occur in practice — client would encode on send, server decode on read. Revisit at conformance
    (Stage 11) or on a real non-ASCII case.
- [ ] 2d — `resources/subscribe` + `resources/unsubscribe` (resource-update subs return via
  `subscriptions/listen`, Stage 7)
- [x] 2e — logging capability removed: deleted `Logger`/`inMemoryLogger`, `McpLogging.SetLevel`
  (logging/setLevel), and the `logger` param/wiring from `McpProtocol`/`RoutingMcpHandler`. **Kept
  `McpLogging.LoggingMessage.Notification`** + `Client.log` seam (NoOp). Per-request `logLevel` +
  request-scoped `notifications/message` return in **Stage 8** (see notify-rename note). Green.
- [x] 2f — tasks deleted entirely (see decision 4): Task model, `McpTask`, `Tasks`/`inMemoryTasks`/
  `TaskStorage`, `ToolExecution`/`TaskSupport` (`Tool.execution`), `notifications/tasks/status`,
  client `ClientTasks`/`tasks()`, `TestingTasks`, and task tests (`task lifecycle`, `onUpdate`,
  `ServerTasksTest`, `InMemoryTaskStorageTest`). Self-contained `Tasks`/`TaskCancel` capability
  descriptors in `Client/ServerCapabilities` left inert (move to `extensions` at Stage 9). Green.

**Stage 2 complete** — all removed/deprecated features gone; only tools/prompts/resources/completions
+ cancellations remain, capability handlers receive `Client.NoOp`.

### [x] Stage 3 — Go stateless — DONE (green)
3a/3b additive (lenses, error codes, version filter, serverInfo decorator, `server/discover`) then the
**collapse** (3c–3g) landed as one coordinated push (delete-and-rewrite; couldn't be small green steps
because server `initialize`/sessions removal breaks the client handshake + all contract tests at once):
- `McpProtocol` de-generified; `receive(req)` = parse→route (no sessions/`New`/`Existing`/`clientTracking`);
  holds `serverInfo`. `RoutingMcpHandler` stateless (no initializer/session/subscribe; `Client.NoOp`).
  `ValidateMcpMethodHeader` de-sessioned. **serverInfo decorator wired** at the `asHttp` funnel.
- Deleted: `Sessions`(+impls), `SessionProvider`, `McpSessionState`, `ClientTracking`, `Session`,
  `ClientRequestContext`, `server/sessions/*`; the streaming/non-streaming connections → one POST-only
  `HttpMcp` (GET/DELETE→405); `McpInitialize`+`Initializer`/`SimpleInitializeHandler`;
  `McpResource.Subscribe/Unsubscribe`; the `Observable` session-callback layer; `SessionId`+`Mcp-Session-Id`.
- `ProtocolVersion.PUBLISHED = {2026-07-28}` (DRAFT+legacy gone). `mcp()` → `RoutingHttpHandler`.
- **Client**: merged the two HTTP clients → one `HttpMcpClient` (POST-and-read, `start()` no-op, no
  session/listener/callback-registry); deleted `AbstractMcpClient` + per-feature client helpers +
  `McpCallbackRegistry`. Testing: `testMcpClient` = `HttpMcpClient` over the in-memory handler.
- Tests: deleted `McpStreamingClientContract` + streaming/session tests; de-generified `McpClientContract`;
  merged client tests → `HttpMcpClientTest`; rewrote `McpRebindProtectionTest` to the POST handler.
- **Debt RESOLVED**: added `McpClient.discover()` (reads `result._meta.serverInfo` before the client's
  protocol-meta strip); `McpApps` now keys servers by the discovered name; `McpAppsHostTest` ×3 re-enabled + green.

(original checklist kept below for reference)
### [ref] Stage 3 checklist — additions land first so each removal stays green.
- [x] 3a — **model foundation** (TDD-first, green): reserved `_meta` key lenses
  (`protocolVersion`/`clientCapabilities`/`clientInfo`/`serverInfo`/`logLevel`) in `MetaKey.kt` next to
  `progressToken()`; error-code constants — `HeaderMismatch` renumbered `-32001 → -32020`, added
  `MissingRequiredClientCapabilityError -32021` (data.requiredCapabilities) +
  `UnsupportedProtocolVersionError -32022` (data.requested/supported); `-32602` = existing
  `ErrorMessage.InvalidParams` (resource-not-found reuses it, no new constant).
  **`resultType` deferred to Stage 4** — its only values are `complete`/`input_required` and
  `input_required` doesn't exist until MRTR, so it belongs with the result unions there (absent →
  clients default to `complete`).
- [~] 3b — read version + capabilities from each request's `_meta`; validate (missing → `-32602`/400,
  unsupported version → `-32022` w/ `data.supported`, undeclared capability → `-32021`). `serverInfo`
  in each result's `_meta`. **`server/discover` INCLUDED**
  - **version validation — DONE + wired + green.** `ValidateProtocolVersion(supported)` **`McpFilter`**
    (handler layer, per user — reads `Header.MCP_PROTOCOL_VERSION`, which mirrors `_meta.protocolVersion`,
    so fully typed, **no Moshi node surgery**); unsupported → `-32022` w/ requested+supported. Wired into
    the `mcpHandler` chain (`…CatchAll.then(ValidateProtocolVersion(supportedVersions)).then(Routing…)`);
    `McpProtocol` gained `supportedVersions` (from `metaData.protocolVersions`, i.e. `PUBLISHED` now →
    tightens to `{2026-07-28}` at the collapse). Nice split: **inbound validation = McpFilter**,
    **outbound envelope (serverInfo) = serialization funnel**.
  - **`server/discover` — DONE + green.** New `McpDiscover` message (`server/discover`, core) + `CacheScope`
    enum + `DiscoverResult` (supportedVersions list, capabilities, instructions, plain `ttlMs`/`cacheScope`).
    `RoutingMcpHandler` branch → `discover()` collaborator, built from `ServerMetaData` via
    `discoverResultFor(metaData)` (decoupled from `initializer` — survives the 3f handshake removal).
    Tests: core serialization round-trip + `discoverResultFor` behavior. `serverInfo` rides `_meta` via the
    (deferred) decorator.
  - Remaining 3b: capability-declared check (`-32021`) folds into Stage 4/MRTR; missing-version `-32602`
    is masked for now by the header's `defaulted(LATEST)` — revisit at the collapse. **3b effectively done.** — in the stateless model it's just a
  request→response + handler returning `supportedVersions`/`capabilities`/`serverInfo` (all already in
  `ServerMetaData`, reusing the old initialize logic). Give `DiscoverResult` plain `ttlMs`/`cacheScope`
  fields directly (don't need the full `CacheableResult` mixin until Stage 6).
  - **serverInfo decorator — pure fn DONE + green** (`server/ServerInfoDecorator.kt`
    `MoshiNode.withServerInfo(info)`: merges `serverInfo` into `node["result"]["_meta"]` via the
    `MetaKey.serverInfo()` lens; errors/no-result pass through). **Wiring deferred to 3c–3e**: it needs
    `serverInfo` threaded through the serialization funnels (`asHttp` + `HttpSessions.send`), and the SSE
    `sessions.send` path is being deleted in the session removal — so wire it into the single
    POST→response funnel once that collapses, not through soon-deleted dual paths. `serverInfo` =
    `ServerMetaData.entity` (`VersionedMcpEntity`); retain it on the stateless engine when built.
- [ ] 3c — drop SSE resumability: event IDs, `Last-Event-ID`, `SessionEventStore`, `SessionEventTracking`
- [ ] 3d — drop `SessionBasedClient` + the server-initiated Sampling/Roots requests;
  `ToolRequest.client` → `NoOp` (Sampling/Roots return via MRTR, Stage 4)
- [ ] 3e — drop sessions: `Mcp-Session-Id`, `Sessions`/`HttpSessions`/`StdIoMcpSessions`,
  `SessionProvider`, `McpSessionState`, `ClientTracking`; **drop `McpRequest.session`** (→ `McpRequest`
  = `(message, http)`; it's only used by `clientTracking`/`Sessions`/resource-subscribe, all removed
  here); `405` on GET/DELETE; SSE-comment keep-alive + `X-Accel-Buffering: no`
- [ ] 3f — drop `initialize`/`notifications/initialized` + `Initializer`/`SimpleInitializeHandler`
- [ ] 3g — `ProtocolVersion` reduced to only `2026-07-28`

### [~] Stage 4 — MRTR (elicitation ONLY)
**Sampling + roots are DEPRECATED (SEP-2577) — a new implementation does NOT re-add them** (already
deleted in the teardown). MRTR is built once, for **elicitation** (the non-deprecated feature reworked
via MRTR). Same logic ⇒ **logging is deprecated too — reconsider re-adding it at Stage 8** (may just skip).
- **`resultType`**: no field on normal results — clients treat absent as `complete`. Only the
  `InputRequiredResult` carries `resultType: "input_required"` (moved from 3a; only meaningful here).
- **DONE:** MRTR wired across **all three** methods — `tools/call`, `prompts/get`, `resources/read`.
  - `Tool/Prompt/ResourceResponse.InputRequired(inputRequests, requestState?)` variants; request params
    (via `HasInputResponses`) carry defaulted `inputResponses`/`requestState`; result types (via
    `HasInputRequired`) carry `resultType`/`inputRequests`/`requestState`.
  - Server: capabilities thread `mcp.inputResponses.toElicitationResponses()` + `mcp.requestState` into
    the handler request and map an `InputRequired` return to an `input_required` wire result. Wire
    conversions shared in `sdk/.../server/capability/ElicitationWire.kt`.
  - Client: one shared `mrtrLoop<T : HasInputRequired>` (in `HttpMcpClient`) drives call/get/read — on
    `input_required`, run the configured `onElicitation` handler, retry the same method (new id) with
    `inputResponses` + echoed `requestState`, until `complete` (`MAX_MRTR_ROUNDS=8`).
  - Tests: `HttpMcpClientMrtrTest` covers a round-trip for each of the three methods.
- **DONE:** Client self-describes every request via reserved `_meta` (`protocolVersion`, `clientInfo`,
  `clientCapabilities`=elicitation form+url), node-merged at the request funnel (`client/.../util.kt`
  `withClientMeta`) — the client analogue of `ServerInfoDecorator`. `entity`/`version` now feed
  `clientInfo`. Test: `ClientMetaTest`.
- **DONE:** Capability-declared enforcement (`-32021`). `toWireRequests(clientCapabilities)` rejects a
  Form/Url elicitation the client didn't declare in `_meta.clientCapabilities`, throwing
  `MissingRequiredClientCapabilityError`. Reader `Meta.clientCapabilities()` in `ElicitationWire.kt`.
  Test: `ToolCapabilityTest`.
- **TODO (deferred, YAGNI):** `requestState` opaque encode/decode + HMAC — only needed once `requestState`
  drives authz; nothing does yet. URL-mode elicitation round-trip test (Form path is covered).

### [~] Stage 5 — HTTP mirror headers + error codes
- **DONE:** `Mcp-Method` is now universal + required. Client stamps it centrally in `toHttpRequest`
  (from the message's own `method`) so it rides *every* POST, not just the named methods;
  `PopulateMcpHeaders`/`PopulateToolHeaders` drop `Mcp-Method` (single source) and keep
  `Mcp-Name`/`Mcp-Param-*`. Server `ValidateMcpMethodHeader` rejects absence *and* mismatch with
  `HeaderMismatch -32020`. Tests: `ValidateMcpMethodHeaderTest`, `PopulateToolHeadersTest`.
- **DONE (pre-existing):** `Mcp-Name` header↔body validated for tools/call·resources/read·prompts/get
  (`RoutingMcpHandler.validateMcpName` → `-32020`). `ValidateProtocolVersion` → `-32022` (supported set).
  `-32021` (Stage 4). resource-not-found → `InvalidParams -32602` (`inMemoryResources`).
- **TODO (deferred):** `MCP-Protocol-Version` header ↔ `_meta.protocolVersion` match — needs
  heterogeneous `_meta` extraction for a low-likelihood inconsistency (header is already the
  validated routing key). `x-mcp-header` Base64 `=?base64?…?=` sentinel for non-ASCII `Mcp-Name`/
  `Mcp-Param` values (spec-MUST, but only bites on non-ASCII/control/whitespace — names are ASCII
  in practice). Revisit if a real non-ASCII case or a strict conformance run demands it.

### [~] Stage 6 — Caching hints + schema loosening
- **DONE:** `ttlMs`/`cacheScope` on the six `CacheableResult` methods (`tools/list`, `prompts/list`,
  `prompts/get`, `resources/list`, `resources/templates/list`, `resources/read`) — inline fields,
  defaults `0`/`public` (`0` = revalidate every time, safe no-op), matching `McpDiscover` which
  already had them. All seven cacheable result types implement a shared `CacheableResult { ttlMs; cacheScope }`
  interface — the generic handle the future caching client reads the hints through.
- **DONE:** `structuredContent: unknown` — wire field retyped `Map<String,Any>?` → `McpNodeType?`,
  removing the `unwrap() as Map` cast + client convert (fixes latent CCE on non-object). Test:
  `ToolCapabilityTest.structuredContent may be a non-object json value`.
- **DONE (pre-existing):** deterministic `tools/list` order — `inMemoryTools` already `sortedBy { name }`.
- **N/A for stock impl:** `input/outputSchema` are opaque `Map<String,Any>` we neither validate nor
  reject, so full JSON-Schema-2020-12 keywords already pass through untouched; the `$ref` safety rules
  are a *client*-validation concern, not the stock server's.
- **TODO (deferred, minor):** `CompleteResultResponse` wrapper + `@maxItems 100` cap on completion
  values — completions rarely exceed 100 and it's a doc-level SHOULD. Revisit at conformance (Stage 11).
- **DONE (handler-facing cache control, read/get).** Decided split (matches spec §5.3 "each page carries
  its own `ttlMs`, but all pages of one request share the same `cacheScope`"):
  - **`ttlMs` = per-response** — on `ResourceResponse.Ok`/`PromptResponse.Ok`, set by the handler (freshness
    is content-dependent). Round-trips to the client's returned `Ok`. Custom `TtlMs` (`LongValue`) tiny type
    — unit in the name; `Long`-backed so not capped at the ~24-day `Int` ceiling. Test: `CacheHintsTest`.
  - **`cacheScope` = per-capability** — declared on the `Resource`/`Prompt` **model** (`cacheScope =
    private`), default `public` (keeps the infix `bind` DSL intact). It's a static "is this user-specific?"
    fact, not a per-invocation one. Capabilities read it from the model; NOT on the handler response.
    Client's user-facing `Ok` doesn't expose it (see client-caching note). Test: `CacheScopeTest`.
- **TODO (deferred — list-method TTL/scope):** the four `*/list` methods are framework-assembled from
  registered items (no per-request handler response), so they'd take a construction-time config knob on
  `tools()`/`prompts()`/`resources()`. Deferred until a caching consumer exists.
- **TODO (future — client-side caching):** when we build the caching client (see Stage 10), have it read
  `ttlMs`+`cacheScope` off the wire result, key a cache by method + result-affecting params, honour
  `cacheScope` (public → shareable; private → per-auth-context), respect `input_required`/`inputResponses`
  as non-cacheable, and invalidate on `listChanged`. That's why `cacheScope` is deliberately absent from the
  client's user-facing `Ok` — the client consumes it internally rather than surfacing it to the caller.

### [x] Stage 7 — `subscriptions/listen` (functionally complete; server + client)
Single long-lived POST→SSE stream + `SubscriptionFilter`. Reuses master's SSE transport shape +
`Observable` concept; drops all session machinery (lifetime = the one stream). Detailed sub-plan in the
plan file. Progress:
- **DONE (1/5):** message types — `McpSubscriptions.{Listen.Request/Response, Acknowledged.Notification}`,
  `SubscriptionFilter` (`resourceSubscriptions: List<String>` — `Uri` mangles `file:///`), `subscriptionId`
  `_meta` lens. `McpSubscriptionsTest`.
- **DONE (2/5):** `HttpMcp` → `PolyHandler` (http face POST→JSON; sse face POST `subscriptions/listen` w/
  `Accept: text/event-stream`). `McpProtocol.listen(req)` sends ack first (tagged `subscriptionId` = request
  id), holds open. `mcp()` returns `PolyHandler`; `mcpHttpNonStreaming` = http face only. Consumers adapt via
  `.http`. `SubscriptionsListenTest` via `PolyHandler.testSseClient` (the stateless `McpProtocolTest`, no init).
- **DONE (3/5):** `ObservableList` onChange hook restored (keyed by the `Sse` connection, NOT subscriptionId
  — not unique across clients); interface default no-ops. `listen()` wires opted-in `*ListChanged` observers →
  push tagged notifications, removed on `onClose`. Test proves push-on-reassign + unrequested-never-sent.
- **DONE (4/5):** `resourceSubscriptions` — `InMemoryResources` per-URI update registry keyed by the `Sse`
  connection (`subscribeToUpdates`/`triggerUpdated`/`removeUpdateSubscriber`; `Resources` defaults no-op).
  `listen()` pushes tagged `resources/updated` only for subscribed URIs. Test proves URI-scoped delivery.
- **DONE (5/5):** client subscriptions live on the **capability accessors**, each returning an
  `McpResult<AutoCloseable>`:
  - `tools().onListChanged { }`, `prompts().onListChanged { }`, `resources().onListChanged { }`
  - `resources().subscribe(uri) { }` (per-URI `resources/updated`)
  Each builds a one-callback `SubscriptionSpec` (internal) on demand and calls the shared private
  `HttpMcpClient.listen(spec)` → **its own** self-contained SSE stream (own filter, own `AutoCloseable`);
  daemon reads via `Http4kSseClient` and dispatches to that subscription's handlers. `close()` stops one,
  `client.close()` stops all. Notifications are bare signals (no payload) → handlers re-fetch (`list()`/
  `read()`). Reconnect mode is a constructor param (`subscriptionReconnectMode`, default `Immediate`).
  **`HttpMcpClient` default http is `JavaHttpClient(Stream)`** — a buffering (`Memory`) client blocks forever
  on an unbounded SSE body; a custom http used with subscriptions MUST stream. e2e
  `HttpMcpClientSubscriptionsTest` vs a real in-process Helidon server.
  - Design note: landed on per-capability accessors (each its **own** stream) over the top-level
    `subscriptions().listen{}` DSL and a `start()`-based broad stream — puts subscribe where it's
    discoverable while keeping each stream self-contained (no shared registry, no re-listen). Trade-off:
    N subscribe calls = N sockets, each independently closeable.
- **Deferred:** graceful empty-result close on server-initiated end; two-stream `progress`/`message` (Stage 8);
  bounded ack-await for a liveness signal in `listen()` (returns `Success` once the daemon starts); optionally
  multiplex several interests onto one socket (add back a combined-filter builder if a real need appears).

### [ ] Stage 8 — Request-scoped logging + progress
- **`notifications/message` is KEPT** — it's the 2026 request-scoped logging form (flows on the request's
  response stream, gated by per-request `_meta.logLevel`; server MUST NOT emit unless the request set it).
  Logging is *marked* deprecated (12-mo window) but still functional — unlike sampling/roots whose
  server→client *request* forms were fully replaced. So keep `McpLogging.LoggingMessage`, `Client.log`,
  the `logLevel` lens; only `logging/setLevel`/connection-wide levels stay removed (done in 2e).
- **Progress** (not deprecated) — re-wire `notifications/progress` request-scoped likewise.
- The residual server→client `Client` (progress + log) → rename to `Notify`/`notify` (both fire on the
  request's response stream once streaming returns).

### [ ] Stage 9 — Tasks as in-module extension
- Implement `io.modelcontextprotocol/tasks` inside stateless sdk (MCP-apps pattern; find and
  mirror it), negotiated via the `extensions` capability: `tasks/get` (poll), `tasks/update`,
  cancel; unsolicited handles. **Build fresh** (2f deleted the old tasks feature — no files to
  un-park); move the inert `Tasks`/`TaskRequests` capability descriptors into `extensions`.

### [ ] Stage 10 — Simplify the stateless client
- In the copied client: delete the daemon listener thread, `McpCallbackRegistry`, id-keyed
  queues/latches, handshake, session id. A call = POST (`_meta` + mirror headers) → if
  `InputRequired`, invoke configured input handlers → retry with `inputResponses` +
  `requestState` → until `complete`. Separate `subscriptions/listen` stream; optional up-front
  `server/discover` + `-32022` re-selection. Update `mcp-stateless/testing` `TestMcpClient`.

### [ ] Stage 11 — Conformance + docs
- `mcp-stateless/conformance` green against the 2026 surface; docs/examples for the new modules.

## Endgame (separate, later — major version)

- Make the stateless modules the default: since packages already match, drop the old modules
  or rename them `*-legacy`, and the `stateless-*` artifacts become canonical
  `http4k-ai-mcp-*`. Migrate remaining dependants (wiretap, a2a-bridge, mpp, x402) then.
- **Re-add stateless stdio** (standard 2026 transport — a definite re-add): a thin
  read-line→`StatelessMcpProtocol`→write-line pump, no `Sessions`, each line self-describing via
  `_meta`. Optionally re-add WebSocket/JSON-RPC/SSE as stateless pumps if demand appears.

## Critical files (in the copied `mcp-stateless/` tree)

- **strip (delete):** `server/sse/*`, `server/websocket/*`, `server/jsonrpc/*` + client
  equivalents; `protocol/Sessions.kt`, `ClientTracking.kt`, `SessionBasedClient.kt`,
  `McpSessionState.kt`, `server/sessions/*`, `Initializer.kt`/`SimpleInitializeHandler.kt`;
  `McpPing.kt`, `McpLogging.SetLevel`, `McpResource.Subscribe/Unsubscribe`, `McpInitialize.kt`.
- **rewrite:** `server/protocol/McpProtocol.kt` (`receive` → stateless), `RoutingMcpHandler.kt`
  (drop removed methods, add `server/discover`/`subscriptions/listen`, `InputRequired`);
  `server/http/HttpStreamingMcpConnection.kt` (405 on GET/DELETE, no session);
  `routing/Mcp.kt`; `core/.../tools.kt` (`InputRequired`, `inputResponses`); `Client.kt`
  (`NoOp` message); `ProtocolVersion.kt` (only `2026-07-28`); `lens/mcpExtensions.kt`.
- **add:** `core/.../protocol/messages/McpDiscover.kt`, `McpSubscriptions.kt`; `_meta` typed
  hierarchy; `ResultType`/`CacheableResult`; MRTR types; new error codes.
- **reuse as-is:** capability handlers `server/protocol/{Tools,Prompts,Resources,Completions}.kt`
  + `server/capability/inMemory*.kt` + the `bind` DSL (drop only the `Client`/`session` params
  on the stateless call path).

## Verification

- Per stage: `./gradlew :http4k-ai-mcp-stateless-core:test :http4k-ai-mcp-stateless-sdk:test
  :http4k-ai-mcp-stateless-client:test` green; **existing modules untouched** and still green.
- End-to-end: the stateless client contract drives a real `mcp()` server over Streamable HTTP —
  `server/discover`, `tools/call` happy path, a full **MRTR round-trip** (elicitation:
  input_required → retry → complete), and `subscriptions/listen` ack + a change notification.
- `mcp-stateless/conformance` against the 2026 surface once Stage 10/11 land.
