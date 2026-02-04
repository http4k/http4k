# MCP SDK Remaining Gaps

This document tracks remaining work for MCP 2025-11-25 spec compliance.

## Summary

| Category      | Gaps |
|---------------|------|
| Test Coverage | 0    |

---

## Test Coverage

### Gap 1: Tasks operations tests

**Status:** ✅ Complete

Add to `McpStreamingClientContract`:

- ✅ `can list tasks`
- ✅ `can get task by id`
- ✅ `can cancel task`
- ✅ `can get task result`
- ✅ `receives task update notifications` (client-side already existed)
- ✅ `server Tasks onUpdate callback receives client task updates`

**Also added:**

- ✅ `ServerTasksTest.onUpdate callback is invoked when task is updated` (unit test)

**Files modified:**

- `pro/ai/mcp/client/src/test/kotlin/org/http4k/ai/mcp/client/McpStreamingClientContract.kt`
- `pro/ai/mcp/sdk/src/test/kotlin/org/http4k/ai/mcp/server/capability/ServerTasksTest.kt`

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
- ✅ Server-side Client interface simplified (removed unused Tasks sub-interface, added direct updateTask/storeTaskResult methods)
- ✅ ResourceUpdated notification method name fix
- ✅ URLElicitationRequiredError with -32042 code and elicitations data
- ⏭️ Client-side task storage (skipped - no real clients use server-polls-client pattern)
- ✅ SSE client extends McpStreamingClientContract
- ✅ WebSocket client extends McpStreamingClientContract
- ✅ Resource subscription tests (fixed bugs in HttpStreamingMcpClient and McpProtocol notification routing)
- ✅ Tasks operations tests (list, get, cancel, result, onUpdate callbacks)
