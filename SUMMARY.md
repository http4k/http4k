# mcp-stateless — Outstanding Work

Snapshot of what's left, by category, with **S/M/L** effort. Companion to `PLAN.md`.

Conformance state: **92 pass / 10 fail** (`pro/ai/mcp-stateless/conformance/conformance-baseline.yml`).
Of the 10 failures, only **2** are real work; the other 4 are decided won't-fix.

## Conformance
| Item | Size | Notes |
|---|---|---|
| ~~`server-stateless` / MissingCapabilityHttp400~~ | — | **DONE** — buffer-the-head (`PipedSse.onFirstSend` gate): a pre-stream `-32021` now returns `application/json` 400, live streaming preserved. `server-stateless` 30/0, dropped from baseline (now 93/9). |
| **`http-custom-header-server-validation`** (D1, SEP-2243) | **L** | `x-mcp-header`→`Mcp-Param-*` feature + `=?base64?…?=` sentinel validation. The big one. |
| `MCP-Protocol-Version` header ↔ `_meta.protocolVersion` match (Stage 5 deferred) | **S** | Hardening; revisit if a strict conformance run demands it. |
| ~~`dynamic_*` mutation stubs (`test_dynamic_tool/prompt/resource`)~~ | — | **DONE / deleted** — vestigial (no scenario drove them); on-invoke list-mutation → `list_changed` is covered by `test_trigger_tool_change`/`test_trigger_prompt_change` + the SDK `ObservableList` path (contract resources-list-change test). |
| _Won't-fix (decided, not "left"):_ sampling/roots ×3, `json-schema-2020-12` | — | Baselined deliberately. |

## Features (new protocol surface)
| Item | Size | Notes |
|---|---|---|
| **Stage 9 — Tasks** as in-module extension | **L** | Ties to the `HttpMcpClient` capability `// FIXME … tasks?` (S on its own). |
| ~~Stage 6 — completion `@maxItems 100` cap~~ | — | **DONE** — `CompletionCapability` caps values + flags `hasMore`. |
| ~~Stage 6 — per-`*/list` TTL/cache-scope hints~~ | — | **DONE** — `ttlMs`/`cacheScope` construction knobs on `tools()`/`prompts()`/`resources()`. |

## Refactor / cleanup
| Item | Size | Notes |
|---|---|---|
| Stage 10 — simplify the stateless client | **M** | Partially done (dead-param + mirror-header unification). |
| Consolidate subsumed standalone tests (Progress/Mrtr/Subscriptions → contract) | **S** | Contract now covers these behaviours. |
| Stage 8 — rename `Client`→`Notify`/`req.notify` | **S** | Cosmetic, wide mechanical rename. |
| `KNOWN_METHODS` → lookup | — | Investigated → not viable (`@PolymorphicLabel` is SOURCE-retained). Leave. |

## Bugs / tests
| Item | Size | Notes |
|---|---|---|
| ~~OAuth + `subscriptions/listen` SSE fails~~ | — | **DONE / misdiagnosis** — was the contract test client in buffered `BodyMode.Memory`; fixed to `Stream`. Security wraps both faces correctly; subscription tests now run under OAuth in `McpClientContract` (in-memory impl opts out via `streamsSubscriptions`). Reject path covered too: bad/no bearer on a subscription open → clean 401 (`HttpMcpOAuthSubscriptionRejectTest`). |
| ~~`ReservedMetaKeysTest` `@Disabled("FIXME")`~~ | — | **DONE** — stale disable; the nullable `_meta` lens already returns `null` on a missing key. Re-enabled, green. |
| `HttpMcpClientProgressTest` flaky under load | **S** | Streaming-timing; passes 5/5 in isolation. |

## Docs
| Item | Size | Notes |
|---|---|---|
| **Stage 12 — docs + examples** for the stateless modules | **M/L** | Not started (module port done). |

## Endgame (later — major version)
| Item | Size | Notes |
|---|---|---|
| Make stateless canonical (drop/rename old modules) | **L** | Packages already match. |
| Migrate **wiretap** to stateless | **M** | Last remaining dependant (a2a/mpp/x402 ported). |
| Re-add stateless **stdio** transport | **M** | Standard 2026 transport; line-pump, no sessions. |
| Optional WS/JSON-RPC/SSE stateless pumps | **M** | Only if demand appears. |

## Bottom line
- Only substantive **conformance** work: **MissingCapabilityHttp400 (M)** and **D1 (L)**.
- Biggest non-conformance: **Stage 9 Tasks (L)**, **docs (M/L)**, **OAuth-subscription bug (M)**.
- Everything else is S/M polish.
- Most self-contained next step: **MissingCapabilityHttp400 (M)** — already designed.
