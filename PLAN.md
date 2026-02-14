# A2A Protocol Support for http4k - Implementation Plan

## Overview

Implement A2A (Agent-to-Agent) protocol support following MCP patterns with Kotshi/Moshi serialization.

## Module Structure

```
pro/ai/a2a/
├── core/     # Protocol types, messages, serialization (Kotshi/Moshi)
├── sdk/      # Server implementations
└── testing/  # Test utilities and test client
```

**Gradle modules:**

- `pro/ai/a2a/core` → `:http4k-ai-a2a-core`
- `pro/ai/a2a/sdk` → `:http4k-ai-a2a-sdk`
- `pro/ai/a2a/testing` → `:http4k-ai-a2a-testing`

---

## Phase 1: Core Types (`pro/ai/a2a/core`)

### 1.1 Value Types (values4k)

```kotlin
class TaskId private constructor(value: String) : StringValue(value)
class ContextId private constructor(value: String) : StringValue(value)
class MessageId private constructor(value: String) : StringValue(value)
class ArtifactId private constructor(value: String) : StringValue(value)
class A2ARpcMethod private constructor(value: String) : StringValue(value)
```

### 1.2 Model Classes (Kotshi @JsonSerializable)

| Type                | Description                                                                                    |
|---------------------|------------------------------------------------------------------------------------------------|
| `AgentCard`         | Discovery manifest (name, skills, capabilities, security)                                      |
| `AgentSkill`        | Skill definition (id, name, description, examples)                                             |
| `AgentCapabilities` | What the agent supports (streaming, pushNotifications)                                         |
| `Task`              | Stateful work unit (id, contextId, status, artifacts, history)                                 |
| `TaskStatus`        | Status with state + timestamps                                                                 |
| `TaskState`         | Enum: SUBMITTED, WORKING, COMPLETED, FAILED, CANCELED, INPUT_REQUIRED, AUTH_REQUIRED, REJECTED |
| `Message`           | Communication unit (messageId, role, parts)                                                    |
| `Part`              | Sealed: TextPart, RawPart, UrlPart, DataPart                                                   |
| `Artifact`          | Task output (artifactId, name, parts)                                                          |

### 1.3 Protocol Messages (following MCP pattern)

```kotlin
interface A2ARpc { val Method: A2ARpcMethod }
interface A2ARequest
interface A2AResponse

// Example:
object A2ASendMessage : A2ARpc {
    override val Method = A2ARpcMethod.of("message/send")

    @JsonSerializable
    data class Request(val message: Message, val configuration: TaskConfiguration?) : A2ARequest

    sealed interface Response : A2AResponse {
        data class TaskCreated(val task: Task) : Response
        data class DirectMessage(val message: Message) : Response
    }
}
```

**RPC Methods:**
| Method | Description |
|--------|-------------|
| `message/send` | Send message, returns Task or Message |
| `message/stream` | Send with SSE updates |
| `tasks/get` | Get task by ID |
| `tasks/cancel` | Cancel running task |
| `tasks/resubscribe` | Subscribe to task updates |

### 1.4 JSON Serialization

- `A2AJson` object (like `McpJson`)
- Kotshi adapters via KSP
- JSON-RPC helpers

---

## Phase 2: Server SDK (`pro/ai/a2a/sdk`)

### 2.1 Protocol Handler

```kotlin
class A2AProtocol<Transport>(
    private val agentCard: AgentCard,
    private val sessions: Sessions<Transport>,
    private val tasks: Tasks,
    private val messageHandler: MessageHandler
) {
    fun receive(transport: Transport, session: Session, req: Request): Result4k<A2ANodeType, A2ANodeType>
}
```

### 2.2 Task Management

- `Tasks` interface - task lifecycle operations
- `ServerTasks` - in-memory implementation with state machine
- Task state transitions validation

### 2.3 Transports

- `SseA2A` - SSE streaming (SendStreamingMessage, SubscribeToTask)
- `HttpA2A` - HTTP/REST binding for sync operations

### 2.4 Agent Card Endpoint

```kotlin
fun AgentCardEndpoint(card: AgentCard) =
    "/.well-known/agent.json" bind GET to { Response(OK).with(agentCardLens of card) }
```

### 2.5 Security

- `A2ASecurity` interface
- API key, Bearer token support

---

## Phase 3: Client SDK

### 3.1 Client Interface

```kotlin
interface A2AClient : AutoCloseable {
    fun sendMessage(message: Message): A2AResult<A2ASendMessage.Response>
    fun sendStreamingMessage(message: Message): Flow<TaskEvent>
    fun getTask(taskId: TaskId): A2AResult<Task>
    fun cancelTask(taskId: TaskId): A2AResult<Task>
    fun subscribeToTask(taskId: TaskId): Flow<TaskEvent>
}
```

### 3.2 Implementations

- `HttpA2AClient` - sync HTTP
- `SseA2AClient` - SSE streaming

---

## Reference Files (MCP patterns to follow)

| Pattern          | MCP File                                            |
|------------------|-----------------------------------------------------|
| RPC interface    | `pro/ai/mcp/core/.../protocol/messages/McpRpc.kt`   |
| Value types      | `pro/ai/mcp/core/.../model/values.kt`               |
| JSON config      | `pro/ai/mcp/core/.../util/McpJson.kt`               |
| Protocol handler | `pro/ai/mcp/sdk/.../server/protocol/McpProtocol.kt` |
| Sessions         | `pro/ai/mcp/sdk/.../server/protocol/Sessions.kt`    |
| SSE server       | `pro/ai/mcp/sdk/.../server/sse/SseMcp.kt`           |
| Build config     | `pro/ai/mcp/core/build.gradle.kts`                  |

---

## Implementation Order

### Phase 1: Core Module (COMPLETE)

1. [x] **core/build.gradle.kts** - Dependencies, KSP setup
2. [x] **Value types** - TaskId, MessageId, ContextId, ArtifactId, A2ARpcMethod
3. [x] **Model classes** - Part, Message, Task, TaskStatus, TaskState, Artifact, AgentCard, AgentSkill, AgentCapabilities
4. [x] **A2AJson** - ConfigurableA2AJson with Kotshi adapters
5. [x] **Protocol messages** - A2ARpc, A2ARequest, A2AResponse, A2AMessage, A2ATask
6. [x] **A2AError/A2AResult** - Error handling types

### Phase 2: SDK Module (COMPLETE)

7. [x] **sdk/build.gradle.kts** - Dependencies
8. [x] **Tasks interface** - Task lifecycle abstraction with TaskStorage
9. [x] **A2AProtocol** - Main protocol handler
10. [x] **HttpA2A** - HTTP transport binding
11. [ ] **SseA2A** - SSE transport binding (deferred)
12. [x] **AgentCardEndpoint** - Integrated in HttpA2A

### Phase 3: Testing Module (COMPLETE)

13. [x] **testing/build.gradle.kts** - Dependencies
14. [x] **TestA2AClient** - In-memory test client

---

## Deferred (Future Phases)

- SSE streaming transport (SseA2A)
- Push notifications (webhooks)
- gRPC transport
- Extended authentication (OAuth2)
- Production HTTP client
