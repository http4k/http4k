# MCP SDK Remaining Gaps

This document tracks remaining work for MCP 2025-11-25 spec compliance.

## Summary

| Category          | Gaps |
|-------------------|------|
| Spec Compliance   | 1    |
| Client-Side Tasks | 2    |
| Test Coverage     | 4    |

---

## 1. Spec Compliance

### Gap: URLElicitationRequiredError missing -32042 code mapping

**Status:** Partial - error type exists, code mapping missing

**Current state:** `McpError.kt` has `URLElicitationRequired` data class but no JSON-RPC error code `-32042` is mapped anywhere.

**Spec requirement:** Per `docs/mcp-spec/schema/schema.json`, `URLElicitationRequiredError` should return error code `-32042`.

**Files to modify:**
- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/McpError.kt`
- Protocol layer error handling

---

## 2. Client-Side Task Support

### Gap: ClientTaskStorage interface missing

**Status:** Not implemented

**Context:** When clients return `SamplingResponse.Task` or `ElicitationResponse.Task`, they need storage to track these tasks for when the server polls.

**Required:**
```kotlin
interface ClientTaskStorage {
    fun store(task: Task)
    fun get(taskId: TaskId): Task?
    fun list(cursor: Cursor?): TaskPage
    fun delete(taskId: TaskId)
    fun storeResult(taskId: TaskId, result: Map<String, Any>)
    fun resultFor(taskId: TaskId): Map<String, Any>?
}
```

**Files to create:**

- `pro/ai/mcp/client/src/main/kotlin/org/http4k/ai/mcp/client/storage/ClientTaskStorage.kt`

### Gap: Client cannot handle inbound tasks/* requests from server

**Status:** Partial - outbound works, inbound missing

**Context:** When server polls client for task status (`tasks/get`, `tasks/list`, `tasks/cancel`, `tasks/result`), client has no handlers registered.

**Current state:** `ClientTasks.kt` only handles outbound requests to server and `onUpdate()` notifications.

**Files to modify:**

- `pro/ai/mcp/client/src/main/kotlin/org/http4k/ai/mcp/client/internal/ClientTasks.kt`

---

## 3. Test Coverage

### Gap 7: Tasks operations tests

**Status:** Not started

Add to `McpStreamingClientContract`:

- `can list tasks`
- `can get task by id`
- `can cancel task`
- `can get task result`
- `receives task update notifications`

### Gap 8: Resource subscription tests

**Status:** Not started

Add to `McpStreamingClientContract`:

- `can subscribe to resource updates`
- `can unsubscribe from resource updates`
- `receives resource updated notifications`

### Gap 9: SSE client streaming contract

**Status:** Not started

Change `SseMcpClientTest` to extend `McpStreamingClientContract<Sse>` instead of `McpClientContract<Sse>`.

### Gap 10: WebSocket client streaming contract

**Status:** Not started

Change `WebsocketMcpClientTest` to extend `McpStreamingClientContract<Websocket>` instead of `McpClientContract<Websocket>`.

**Files to modify:**

- `pro/ai/mcp/client/src/test/kotlin/org/http4k/ai/mcp/client/McpStreamingClientContract.kt`
- `pro/ai/mcp/client/src/test/kotlin/org/http4k/ai/mcp/client/sse/SseMcpClientTest.kt`
- `pro/ai/mcp/client/src/test/kotlin/org/http4k/ai/mcp/client/websocket/WebsocketMcpClientTest.kt`

---

## Verification

```bash
./gradlew :http4k-ai-mcp-core:test :http4k-ai-mcp-sdk:test :http4k-ai-mcp-client:test
```

---

## Completed (from previous plans)

- ✅ Task field rename (`updatedAt` → `lastUpdatedAt`)
- ✅ ResourceLink content type
- ✅ Client sampling sub-capabilities (`tools`, `context`)
- ✅ Elicitation complete notification
- ✅ Sampling response content array
- ✅ Extended ClientCapabilities with TaskRequests
- ✅ Task field in sampling/elicitation responses
- ✅ SamplingResponse.Task / ElicitationResponse.Task variants
- ✅ Server-side Client.Tasks interface
- ✅ ResourceUpdated notification method name fix
