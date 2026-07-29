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

### [ ] Stage 3 — Go stateless (coupled removals + their enabling additions)
The stateless transition — additions land first so each removal stays green.
- [ ] 3a — read `_meta` per request: `protocolVersion` + `clientCapabilities` (+ `clientInfo`);
  validate missing → `-32602`/400 *(addition)*
- [ ] 3b — `server/discover` RPC; `resultType: "complete"` + `serverInfo` on every result *(addition)*
- [ ] 3c — drop SSE resumability: event IDs, `Last-Event-ID`, `SessionEventStore`, `SessionEventTracking`
- [ ] 3d — drop `SessionBasedClient` + the server-initiated Sampling/Roots requests;
  `ToolRequest.client` → `NoOp` (Sampling/Roots return via MRTR, Stage 4)
- [ ] 3e — drop sessions: `Mcp-Session-Id`, `Sessions`/`HttpSessions`/`StdIoMcpSessions`,
  `SessionProvider`, `McpSessionState`, `ClientTracking`; `405` on GET/DELETE; SSE-comment
  keep-alive + `X-Accel-Buffering: no`
- [ ] 3f — drop `initialize`/`notifications/initialized` + `Initializer`/`SimpleInitializeHandler`
- [ ] 3g — `ProtocolVersion` reduced to only `2026-07-28`

### [ ] Stage 4 — MRTR
- Generalise `ToolResponse.ElicitationRequired` → `InputRequired`; add defaulted
  `inputResponses`/`requestState` to the tool/resource/prompt request types. Wire the
  `... | InputRequired` result union into `tools/call`, `prompts/get`, `resources/read` only.
- Handler reads `req.inputResponses[key]`; builder for `InputRequired(inputRequests,
  requestState?)`. Enforce capability-declared (`-32021`); `requestState` HMAC helper. Order:
  elicitation → sampling → roots (mark roots/sampling deprecated).

### [ ] Stage 5 — HTTP mirror headers + error codes
- Require `Mcp-Method`; require `Mcp-Name` on tools/call·resources/read·prompts/get; validate
  header↔body → `HeaderMismatch -32020`; `MCP-Protocol-Version` must match `_meta`. Support
  `x-mcp-header` → `Mcp-Param-{Name}` (Base64 `=?base64?…?=` sentinel). Emit
  `-32020`/`-32021`/`-32022`, resource-not-found → `-32602`. Reuse `ValidateMcpMethodHeader.kt`.

### [ ] Stage 6 — Caching hints + schema loosening
- `ttlMs`/`cacheScope` on `server/discover`, `tools/list`, `prompts/list`, `resources/list`,
  `resources/templates/list`, `resources/read`, `prompts/get`. Deterministic `tools/list`
  order. `structuredContent: unknown`; loosen `input/outputSchema` to full JSON-Schema-2020-12
  with the `$ref` safety rules. `CompleteResultResponse`; `@maxItems 100` on completions.

### [ ] Stage 7 — `subscriptions/listen`
- A single long-lived POST stream + `SubscriptionFilter`, restoring resource-update +
  list-changed notifications dropped in 2c/2d. **Ack first** (honored subset); tag every
  message with `subscriptionId`; never send unrequested types; graceful empty-result close.
  **Two-stream rule:** request-scoped `progress`/`message` only on their request's response
  stream; change notifications only on the listen stream. Reuse
  `ObservableResources`/`ObservableCapability`, re-pointed at the listen stream.

### [ ] Stage 8 — Per-request logging
- Restore `notifications/message`, now request-scoped: per-request level via `_meta.…/logLevel`;
  server MUST NOT emit it unless the request set the field, scoped to that request's stream.

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
