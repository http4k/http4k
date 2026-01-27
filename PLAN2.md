# MCP Client-Side Task Support

## Summary

Enable clients to return tasks instead of immediate results for `sampling/createMessage` and `elicitation/create` requests from the server.

## Background

Per the MCP spec, tasks are bidirectional. We've implemented server-side task creation (tools returning tasks). Now we need client-side task support where:

- Server sends `sampling/createMessage` or `elicitation/create` to client
- Client can return a `CreateTaskResult` instead of immediate result
- Server polls client for task status and retrieves result when complete

### Capability Declaration

Clients declare task support via:

```json
{
  "capabilities": {
    "tasks": {
      "list": {},
      "cancel": {},
      "requests": {
        "sampling": { "createMessage": {} },
        "elicitation": { "create": {} }
      }
    }
  }
}
```

## Current State

### What exists:

- `McpSampling.Request/Response` - sampling messages
- `McpElicitations.Request.Form/Url/Response` - elicitation messages
- `ClientCapabilities.Tasks` - basic tasks capability structure
- Server-side `TaskStorage`, `ServerTasks` for managing tasks

### What's missing:

- Client-side task storage and management
- Server ability to poll client for task status
- `task` field in sampling/elicitation responses
- Client capability to declare `tasks.requests.sampling/elicitation`

## Implementation Plan

### Step 1: Extend ClientCapabilities for task-augmented requests

Add sub-capabilities to declare which requests support tasks:

```kotlin
// ClientCapabilities.kt
@JsonSerializable
data class Tasks(
    val list: Unit? = null,
    val cancel: Unit? = null,
    val requests: TaskRequests? = null
)

@JsonSerializable
data class TaskRequests(
    val sampling: SamplingTaskSupport? = null,
    val elicitation: ElicitationTaskSupport? = null
)

@JsonSerializable
data class SamplingTaskSupport(val createMessage: Unit? = null)

@JsonSerializable
data class ElicitationTaskSupport(val create: Unit? = null)
```

**File**: `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/ClientCapabilities.kt`

### Step 2: Add `task` field to sampling/elicitation responses

Per spec, responses can return either the result OR a task:

```kotlin
// McpSampling.kt
@JsonSerializable
data class Response(
    val model: ModelName? = null,
    val stopReason: StopReason? = null,
    val role: Role? = null,
    val content: List<Content>? = null,
    val task: Task? = null,  // NEW
    override val _meta: Meta = Meta.default
) : ClientMessage.Response, HasMeta
```

```kotlin
// McpElicitations.kt
@JsonSerializable
data class Response(
    val action: ElicitationAction? = null,
    val content: Map<String, Any>? = null,
    val task: Task? = null,  // NEW
    override val _meta: Meta = Meta.default
) : ClientMessage.Response, HasMeta
```

**Files**:

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/messages/McpSampling.kt`
- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/messages/McpElicitations.kt`

### Step 3: Add `SamplingResponse.Task` and `ElicitationResponse.Task` variants

Similar to `ToolResponse.Task`, allow handlers to return tasks:

```kotlin
// In appropriate files
sealed interface SamplingResponse {
    data class Ok(...) : SamplingResponse
    data class Task(val task: org.http4k.ai.mcp.model.Task) : SamplingResponse
}

sealed interface ElicitationResponse {
    data class Ok(...) : ElicitationResponse
    data class Task(val task: org.http4k.ai.mcp.model.Task) : ElicitationResponse
}
```

### Step 4: Server-side polling of client tasks

The server needs to poll client for task status when client returns a task. Add to `Client` interface:

```kotlin
// Client.kt - server's view of client
interface Client {
    // existing...
    fun tasks(): Tasks  // For polling client tasks

    interface Tasks {
        fun get(taskId: TaskId, timeout: Duration? = null): McpResult<Task>
        fun list(timeout: Duration? = null): McpResult<List<Task>>
        fun cancel(taskId: TaskId, timeout: Duration? = null): McpResult<Unit>
        fun result(taskId: TaskId, timeout: Duration? = null): McpResult<Map<String, Any>?>
    }
}
```

Wait - this already exists! The server-side `Client.Tasks` interface is for the server to interact with the *client's* task management.

### Step 5: Client-side task storage

The MCP client needs to store tasks when it returns them:

```kotlin
// New: ClientTaskStorage
interface ClientTaskStorage {
    fun store(task: Task)
    fun get(taskId: TaskId): Task?
    fun list(cursor: Cursor?): TaskPage
    fun delete(taskId: TaskId)
    fun storeResult(taskId: TaskId, result: Map<String, Any>)
    fun resultFor(taskId: TaskId): Map<String, Any>?
}
```

### Step 6: Client handles `tasks/*` requests from server

When server polls client for task status, client needs to handle:

- `tasks/get` - return task status
- `tasks/list` - list client's tasks
- `tasks/cancel` - cancel a client task
- `tasks/result` - return task result

**Files**: `pro/ai/mcp/client/src/main/kotlin/org/http4k/ai/mcp/client/`

## Message Flow

```
Server                          Client
   |                               |
   |-- sampling/createMessage ---->|
   |                               | (client decides to use task)
   |<-- CreateTaskResult(task) ----|
   |                               |
   |-- tasks/get(taskId) --------->|
   |<-- Task(status: working) -----|
   |                               |
   |     ... client processes ...  |
   |                               |
   |-- tasks/get(taskId) --------->|
   |<-- Task(status: completed) ---|
   |                               |
   |-- tasks/result(taskId) ------>|
   |<-- SamplingResult ------------|
```

## Verification

```bash
./gradlew :http4k-ai-mcp-core:test :http4k-ai-mcp-sdk:test :http4k-ai-mcp-client:test
```

## Open Questions

1. Should client task storage be in-memory only, or support persistence?
2. How does `SessionBasedClient.tasks()` (server calling client) work with the transport layer?
3. Do we need new test contracts for client-side task handling?
