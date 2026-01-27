# MCP 2025-11-25 Specification Upgrade Plan

This document outlines the plan to upgrade the http4k MCP implementation to full compliance with the 2025-11-25 MCP specification.

## Implementation Status

| Gap                                      | Status | Commit                        |
|------------------------------------------|--------|-------------------------------|
| Gap 1: Task field rename                 | ✅ DONE | -                             |
| Gap 2: ResourceLink content type         | ✅ DONE | -                             |
| Gap 3: Client sampling sub-capabilities  | ✅ DONE | -                             |
| Gap 4: Elicitation complete notification | ✅ DONE | -                             |
| Gap 5: URLElicitationRequiredError       | ✅ DONE | -                             |
| Gap 6: Sampling content array            | ✅ DONE | Used Option B (List<Content>) |

## Background

The Model Context Protocol (MCP) specification is versioned, with the current latest version being **2025-11-25**. The http4k implementation already supports
this protocol version and implements the majority of features. This plan addresses the remaining gaps to achieve full compliance.

### Reference Documents

All specification documents are located in `docs/mcp-spec/`:

| Document                 | Location                                             |
|--------------------------|------------------------------------------------------|
| JSON Schema (definitive) | `docs/mcp-spec/schema/schema.json`                   |
| Changelog                | `docs/mcp-spec/2025-11-25/changelog.mdx`             |
| Lifecycle                | `docs/mcp-spec/2025-11-25/basic/lifecycle.mdx`       |
| Tools                    | `docs/mcp-spec/2025-11-25/server/tools.mdx`          |
| Resources                | `docs/mcp-spec/2025-11-25/server/resources.mdx`      |
| Prompts                  | `docs/mcp-spec/2025-11-25/server/prompts.mdx`        |
| Sampling                 | `docs/mcp-spec/2025-11-25/client/sampling.mdx`       |
| Elicitation              | `docs/mcp-spec/2025-11-25/client/elicitation.mdx`    |
| Tasks                    | `docs/mcp-spec/2025-11-25/basic/utilities/tasks.mdx` |

---

## Gap 1: Task Field Rename (`updatedAt` → `lastUpdatedAt`)

### Specification Reference

From `docs/mcp-spec/2025-11-25/basic/utilities/tasks.mdx` (lines 706-714):

> A task represents the execution state of a request. The task state includes:
> - `lastUpdatedAt`: ISO 8601 timestamp when the task status was last updated

From `docs/mcp-spec/schema/schema.json`, the `Task` definition:

```json
"lastUpdatedAt": {
"description": "ISO 8601 timestamp when the task status was last updated.",
"type": "string"
}
```

### Current Implementation

File: `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/model/Task.kt`

```kotlin
@JsonSerializable
data class Task(
    val taskId: TaskId,
    val status: TaskStatus,
    val statusMessage: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,  // <-- Should be lastUpdatedAt
    val ttl: TimeToLive? = null,
    val pollInterval: Int? = null
)
```

### Reasoning

The spec explicitly uses `lastUpdatedAt` for consistency with the naming pattern used elsewhere in the spec (e.g., `Annotations.lastModified`). The current
`updatedAt` name will cause JSON serialization mismatches with compliant clients/servers.

### Changes Required

1. **Rename field in `Task.kt`**: `updatedAt` → `lastUpdatedAt`
2. **Update all usages** across SDK and client modules
3. **Update tests** in conformance module

### Impact

- **Breaking change**: Any existing code referencing `Task.updatedAt` will need updating
- **Wire format change**: JSON output changes from `"updatedAt"` to `"lastUpdatedAt"`

### Files to Modify

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/model/Task.kt`
- All files referencing `Task.updatedAt` (search with `Task.*updatedAt`)

---

## Gap 2: Missing Content Type `resource_link`

### Specification Reference

From `docs/mcp-spec/2025-11-25/server/tools.mdx` (lines 276-294):

> #### Resource Links
>
> A tool **MAY** return links to Resources, to provide additional context or data. In this case, the tool will return a URI that can be subscribed to or fetched
> by the client:
>
> ```json
> {
>   "type": "resource_link",
>   "uri": "file:///project/src/main.rs",
>   "name": "main.rs",
>   "description": "Primary application entry point",
>   "mimeType": "text/x-rust"
> }
> ```
>
> Resource links returned by tools are not guaranteed to appear in the results of a `resources/list` request.

From `docs/mcp-spec/schema/schema.json` (lines 2637-2680):

```json
"ResourceLink": {
"description": "A resource that the server is capable of reading, included in a prompt or tool call result.",
"properties": {
"annotations": {"$ref": "#/$defs/Annotations"},
"description": {"type": "string"
},
"mimeType": {
"type": "string" },
"name": {"type": "string"},
"title": {"type": "string"},
"type": {"const": "resource_link"},
"uri": {"format": "uri", "type": "string"
}
},
"required": ["name", "type", "uri"]
}
```

### Current Implementation

File: `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/model/Content.kt`

The `Content` sealed class has:

- `Text`
- `Image`
- `Audio`
- `EmbeddedResource` (type: "resource")
- `ToolUse`
- `ToolResult`

**Missing**: `ResourceLink` (type: "resource_link")

### Reasoning

`ResourceLink` is distinct from `EmbeddedResource`:

| Aspect             | EmbeddedResource      | ResourceLink              |
|--------------------|-----------------------|---------------------------|
| Type discriminator | `"resource"`          | `"resource_link"`         |
| Contains content   | Yes (text/blob)       | No (just URI)             |
| Purpose            | Inline content        | Reference for later fetch |
| Use case           | Small, immediate data | Large files, lazy loading |

Tools should be able to return links to resources without embedding the full content, allowing clients to decide when/whether to fetch the actual data.

### Changes Required

1. **Add `ResourceLink` to `Content.kt`**:

```kotlin
@JsonSerializable
@PolymorphicLabel("resource_link")
data class ResourceLink(
    val uri: Uri,
    val name: ResourceName,
    val title: String? = null,
    val description: String? = null,
    val mimeType: MimeType? = null,
    val annotations: Annotations? = null
) : Content()
```

2. **Update any content handling** that needs to process resource links

### Impact

- **Additive change**: No breaking changes
- **New capability**: Tools can now return resource links

### Files to Modify

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/model/Content.kt`
- `pro/ai/mcp/conformance/` - Add test for resource link content

---

## Gap 3: Client Sampling Sub-capabilities (`tools`, `context`)

### Specification Reference

From `docs/mcp-spec/2025-11-25/client/sampling.mdx` (lines 42-89):

> Clients **MUST** declare support for tool use via the `sampling.tools` capability to receive tool-enabled sampling requests.
>
> **Basic sampling:**
> ```json
> { "capabilities": { "sampling": {} } }
> ```
>
> **With tool use support:**
> ```json
> { "capabilities": { "sampling": { "tools": {} } } }
> ```
>
> **With context inclusion support (soft-deprecated):**
> ```json
> { "capabilities": { "sampling": { "context": {} } } }
> ```

From `docs/mcp-spec/schema/schema.json` (lines 340-356):

```json
"sampling": {
"description": "Present if the client supports sampling from an LLM.",
"properties": {
"context": {
"description": "Whether the client supports context inclusion via includeContext parameter."
},
"tools": {
"description": "Whether the client supports tool use via tools and toolChoice parameters."
}
}
}
```

### Current Implementation

File: `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/ClientCapabilities.kt`

```kotlin
@JsonSerializable
data class ClientCapabilities internal constructor(
    val roots: Roots?,
    val sampling: Unit?,  // <-- Just Unit, no sub-capabilities
    val experimental: Unit?,
    val elicitation: Elicitation?,
    val tasks: Tasks?,
)
```

### Reasoning

The spec requires servers to check `sampling.tools` before sending tool-enabled sampling requests. Without this sub-capability structure:

1. Servers cannot know if a client supports tool use in sampling
2. Clients cannot indicate they support the `includeContext` parameter (even though soft-deprecated)

### Changes Required

1. **Create `Sampling` data class** with optional sub-capabilities:

```kotlin
@JsonSerializable
data class Sampling(
    val tools: Unit? = null,
    val context: Unit? = null
)
```

2. **Update `ClientCapabilities`** to use `Sampling?` instead of `Unit?`

3. **Add capability flags** to `ClientProtocolCapability`:
    - `SamplingTools`
    - `SamplingContext` (soft-deprecated but should support for completeness)

### Impact

- **Wire format change**: `"sampling": {}` stays same, but can now include `"tools": {}` and `"context": {}`
- **Backward compatible**: Empty `sampling: {}` still works

### Files to Modify

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/ClientCapabilities.kt`
- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/ClientProtocolCapability.kt`

---

## Gap 4: Missing Notification `notifications/elicitation/complete`

### Specification Reference

From `docs/mcp-spec/2025-11-25/client/elicitation.mdx` (lines 391-417):

> ### Completion Notifications for URL Mode Elicitation
>
> Servers **MAY** send a `notifications/elicitation/complete` notification when an out-of-band interaction started by URL mode elicitation is completed.
>
> ```json
> {
>   "jsonrpc": "2.0",
>   "method": "notifications/elicitation/complete",
>   "params": {
>     "elicitationId": "550e8400-e29b-41d4-a716-446655440000"
>   }
> }
> ```

From `docs/mcp-spec/schema/schema.json` (lines 1023-1040):

```json
"ElicitationCompleteNotification": {
"description": "An optional notification from the server to the client, informing it of a completion of a out-of-band elicitation request.",
"properties": {
"method": {"const": "notifications/elicitation/complete"},
"params": {
"properties": {
"elicitationId": {
"type": "string"
}
},
"required": ["elicitationId"]
}
}
}
```

### Current Implementation

File: `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/messages/McpElicitations.kt`

Only contains:

- `Request.Form`
- `Request.Url`
- `Response`

**Missing**: `Complete.Notification`

### Reasoning

URL mode elicitation is designed for out-of-band interactions (OAuth flows, payment processing, etc.). The completion notification allows:

1. Servers to signal when the user has completed the external interaction
2. Clients to automatically retry failed requests or update UI
3. Better UX without requiring manual user intervention to continue

### Changes Required

1. **Add `Complete` object to `McpElicitations.kt`**:

```kotlin
object Complete : McpRpc {
    override val Method = McpRpcMethod.of("notifications/elicitation/complete")

    @JsonSerializable
    data class Notification(
        val elicitationId: ElicitationId
    ) : ServerMessage.Notification
}
```

2. **Add server-side support** to send this notification after URL elicitation completes

3. **Add client-side handling** to receive and process this notification

### Impact

- **Additive change**: No breaking changes
- **New capability**: Servers can now notify clients when URL elicitations complete

### Files to Modify

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/messages/McpElicitations.kt`
- `pro/ai/mcp/sdk/` - Server-side notification sending
- `pro/ai/mcp/client/` - Client-side notification handling

---

## Gap 5: Missing Error Type `URLElicitationRequiredError` (-32042)

### Specification Reference

From `docs/mcp-spec/2025-11-25/client/elicitation.mdx` (lines 419-448):

> ### URL Elicitation Required Error
>
> When a request cannot be processed until an elicitation is completed, the server **MAY** return a `URLElicitationRequiredError` (code `-32042`).
>
> ```json
> {
>   "jsonrpc": "2.0",
>   "id": 2,
>   "error": {
>     "code": -32042,
>     "message": "This request requires more information.",
>     "data": {
>       "elicitations": [
>         {
>           "mode": "url",
>           "elicitationId": "550e8400-...",
>           "url": "https://mcp.example.com/connect?...",
>           "message": "Authorization is required..."
>         }
>       ]
>     }
>   }
> }
> ```

From `docs/mcp-spec/schema/schema.json` (lines 3859-3873):

```json
"URLElicitationRequiredError": {
"description": "An error response that indicates that the server requires the client to provide additional information via an elicitation request.",
"properties": {
"error": {
"properties": {
"code": {"const": -32042 },
"data": {
"properties": {
"elicitations": {"items": {"$ref": "#/$defs/URLElicitation"}}
}
}
}
}
}
}
```

### Current Implementation

File: `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/McpError.kt`

No specific error type for URL elicitation required.

### Reasoning

This error enables a powerful pattern for handling authorization:

1. Client calls a tool that requires third-party authorization
2. Server returns `URLElicitationRequiredError` with authorization URL
3. Client opens URL, user completes authorization
4. Client retries original request (possibly triggered by completion notification)

This is essential for OAuth flows where the MCP server acts as an OAuth client to third-party services.

### Changes Required

1. **Add error code constant**:

```kotlin
object McpErrorCodes {
    const val URL_ELICITATION_REQUIRED = -32042
}
```

2. **Add error data class**:

```kotlin
@JsonSerializable
data class URLElicitationRequiredErrorData(
    val elicitations: List<McpElicitations.Request.Url>
)
```

3. **Add error creation helper** in protocol layer

4. **Add client-side handling** to recognize and process this error

### Impact

- **Additive change**: No breaking changes
- **New capability**: Servers can request URL elicitation via error response

### Files to Modify

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/McpError.kt`
- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/McpException.kt`
- `pro/ai/mcp/client/` - Client-side error handling

---

## Gap 6: Sampling Response Content (Single vs Array)

### Specification Reference

From `docs/mcp-spec/schema/schema.json` (lines 761-791):

```json
"CreateMessageResult": {
"properties": {
"content": {
"anyOf": [
{"$ref": "#/$defs/TextContent"},
{"$ref": "#/$defs/ImageContent" },
{"$ref": "#/$defs/AudioContent"},
{"$ref": "#/$defs/ToolUseContent"
},
{
"$ref": "#/$defs/ToolResultContent"
},
{
"items": {"$ref": "#/$defs/SamplingMessageContentBlock"},
"type": "array"
}
]
}
}
}
```

From `docs/mcp-spec/2025-11-25/client/sampling.mdx` (lines 241-263), the tool use response example shows an array:

```json
{
    "result": {
        "content": [
            {
                "type": "tool_use",
                "id": "call_abc123",
                "name": "get_weather",
                "input": {
                    ...
                }
            },
            {
                "type": "tool_use",
                "id": "call_def456",
                "name": "get_weather",
                "input": {
                    ...
                }
            }
        ]
    }
}
```

### Current Implementation

File: `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/messages/McpSampling.kt`

```kotlin
@JsonSerializable
data class Response(
    val model: ModelName,
    val stopReason: StopReason?,
    val role: Role,
    val content: Content  // <-- Single Content only
) : ClientMessage.Response
```

### Reasoning

The spec allows `content` to be either:

1. A single content block (text, image, audio, tool_use, tool_result)
2. An array of content blocks

This flexibility is essential for tool use scenarios where the LLM may request multiple tool calls in parallel (e.g., "What's the weather in Paris AND
London?").

### Design Options

**Option A: Polymorphic wrapper type**

```kotlin
sealed class SamplingResponseContent {
    data class Single(val content: Content) : SamplingResponseContent()
    data class Multiple(val contents: List<Content>) : SamplingResponseContent()
}
```

- Pro: Type-safe
- Con: More complex, custom serialization needed

**Option B: Always use `List<Content>`**

```kotlin
val content: List<Content>
```

- Pro: Simple, handles both cases
- Con: Single items serialize as `[{...}]` instead of `{...}`

**Option C: Use `Any` with custom adapter**

```kotlin
val content: Any  // Content or List<Content>
```

- Pro: Matches spec exactly
- Con: Type safety lost

### Recommended Approach

**Option B** with a custom JsonAdapter that:

- Serializes single-item lists as single objects
- Deserializes both single objects and arrays into `List<Content>`

This matches http4k's functional style and maintains type safety while being spec-compliant.

### Changes Required

1. **Change `content` type** to `List<Content>`
2. **Create custom JsonAdapter** for polymorphic single/array handling
3. **Update all response handling code**

### Impact

- **Possibly breaking**: Code expecting `Content` will need to handle `List<Content>`
- **Wire format**: Will correctly serialize/deserialize both formats

### Files to Modify

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/messages/McpSampling.kt`
- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/util/` - Custom adapter
- `pro/ai/mcp/client/` - Response handling updates

---

## Implementation Order

Based on dependencies, breaking changes, and complexity:

### Phase 1: Breaking Changes (Do First)

| # | Gap               | Reason                                                   |
|---|-------------------|----------------------------------------------------------|
| 1 | Task field rename | Breaking change - address early so dependents can update |

### Phase 2: Core Additions (Low Risk)

| # | Gap                              | Reason                    |
|---|----------------------------------|---------------------------|
| 2 | ResourceLink content type        | Additive, isolated change |
| 3 | Client sampling sub-capabilities | Simple structure addition |

### Phase 3: New Features (Medium Complexity)

| # | Gap                               | Reason                                    |
|---|-----------------------------------|-------------------------------------------|
| 4 | Elicitation complete notification | New message type, needs protocol handling |
| 5 | URLElicitationRequiredError       | New error type, needs client handling     |

### Phase 4: Design Decision Required

| # | Gap                    | Reason                                           |
|---|------------------------|--------------------------------------------------|
| 6 | Sampling content array | Requires design choice on serialization approach |

---

## Testing Strategy

Each gap should be implemented with TDD:

1. **Write failing test** demonstrating the gap
2. **Implement minimum code** to pass the test
3. **Refactor** if needed
4. **Add conformance tests** in `pro/ai/mcp/conformance/`

### Conformance Test Additions

| Gap                         | Test Case                                          |
|-----------------------------|----------------------------------------------------|
| Task field rename           | Verify `lastUpdatedAt` in JSON output              |
| ResourceLink                | Tool returning resource link content               |
| Sampling capabilities       | Client declares `sampling.tools`, server validates |
| Elicitation complete        | Server sends notification, client receives         |
| URLElicitationRequiredError | Server returns error, client handles               |
| Sampling content array      | Response with multiple tool_use blocks             |

---

## Rollout Considerations

### Versioning

- All changes target the existing `2025-11-25` protocol version
- No new protocol version needed (these are bug fixes for spec compliance)

### Migration Guide

For the breaking change (Gap 1):

```kotlin
// Before
task.updatedAt

// After
task.lastUpdatedAt
```

### Backward Compatibility

- Gaps 2-5 are additive and backward compatible
- Gap 6 may affect clients expecting single `Content` objects

---

## Success Criteria

1. All gaps implemented with tests
2. Conformance module passes with new test cases
3. JSON serialization matches spec schema exactly
4. No regressions in existing functionality

---

# MCP Client Streaming Contract Test Coverage

This section tracks gaps in `McpStreamingClientContract` test coverage.

## Implementation Status

| Gap                                    | Status      |
|----------------------------------------|-------------|
| Gap 7: Tasks operations tests          | NOT STARTED |
| Gap 8: Resource subscription tests     | NOT STARTED |
| Gap 9: SSE client streaming contract   | NOT STARTED |
| Gap 10: WebSocket streaming contract   | NOT STARTED |

---

## Gap 7: Tasks Operations - NOT TESTED

### Current State

Tasks are fully implemented in `HttpStreamingMcpClient` but have **zero tests** in `McpStreamingClientContract`:
- `tasks.list()`
- `tasks.get(taskId)`
- `tasks.cancel(taskId)`
- `tasks.result(taskId)`
- `tasks.onUpdate(taskId, callback)` notification

### Files

- Contract: `pro/ai/mcp/client/src/test/kotlin/org/http4k/ai/mcp/client/McpStreamingClientContract.kt`
- Implementation: `pro/ai/mcp/client/src/main/kotlin/org/http4k/ai/mcp/client/http/HttpStreamingMcpClient.kt`

### Changes Required

Add test methods to `McpStreamingClientContract`:
1. `can list tasks`
2. `can get task by id`
3. `can cancel task`
4. `can get task result`
5. `receives task update notifications`

---

## Gap 8: Resource Subscriptions - NOT TESTED

### Current State

Resource subscribe/unsubscribe callbacks exist but are never tested:
- `resources.subscribe(uri, callback)`
- `resources.unsubscribe(uri)`
- `McpResource.Updated` notification handling

### Files

- Contract: `pro/ai/mcp/client/src/test/kotlin/org/http4k/ai/mcp/client/McpStreamingClientContract.kt`

### Changes Required

Add test methods to `McpStreamingClientContract`:
1. `can subscribe to resource updates`
2. `can unsubscribe from resource updates`
3. `receives resource updated notifications`

---

## Gap 9: SSE Client Missing Streaming Contract

### Current State

`SseMcpClientTest` only extends `McpClientContract`, NOT `McpStreamingClientContract`:
- SSE client supports sampling, elicitations, progress, tasks
- But these features are untested for SSE transport

### Files

- Test: `pro/ai/mcp/client/src/test/kotlin/org/http4k/ai/mcp/client/sse/SseMcpClientTest.kt`

### Changes Required

1. Change `SseMcpClientTest` to extend `McpStreamingClientContract<Sse>` instead of `McpClientContract<Sse>`
2. Ensure all streaming tests pass with SSE transport

---

## Gap 10: WebSocket Client Missing Streaming Contract

### Current State

`WebsocketMcpClientTest` only extends `McpClientContract`, NOT `McpStreamingClientContract`:
- WebSocket client supports all streaming features
- But streaming tests are missing

### Files

- Test: `pro/ai/mcp/client/src/test/kotlin/org/http4k/ai/mcp/client/websocket/WebsocketMcpClientTest.kt`

### Changes Required

1. Change `WebsocketMcpClientTest` to extend `McpStreamingClientContract<Websocket>` instead of `McpClientContract<Websocket>`
2. Ensure all streaming tests pass with WebSocket transport
