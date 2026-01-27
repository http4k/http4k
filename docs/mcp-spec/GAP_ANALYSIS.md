# MCP 2025-11-25 Specification Gap Analysis

This document compares the current http4k MCP implementation against the 2025-11-25 MCP specification.

## Executive Summary

The http4k MCP implementation is **largely compliant** with the 2025-11-25 spec. Most core features are implemented including tools, resources, prompts,
sampling, elicitations, and tasks. The gaps identified are relatively minor and mostly involve missing content types, notifications, and error handling
improvements.

---

## Currently Implemented (✓)

### Protocol & Initialization

- [x] Protocol version 2025-11-25 supported
- [x] `VersionedMcpEntity` with `title`, `description`, `icons`, `websiteUrl`
- [x] Version negotiation
- [x] Capability negotiation

### Tools

- [x] Tool definition: `name`, `description`, `title`, `inputSchema`, `outputSchema`, `annotations`, `icons`, `execution`
- [x] `ToolExecution` with `taskSupport` (required/optional/forbidden)
- [x] `tools/list` with pagination
- [x] `tools/call` with task augmentation support
- [x] Tool results: `content`, `structuredContent`, `isError`
- [x] `notifications/tools/list_changed`

### Resources

- [x] Static and templated resources
- [x] Resource attributes: `uri`, `name`, `title`, `description`, `mimeType`, `size`, `annotations`, `icons`
- [x] `resources/list`, `resources/read`, `resources/templates/list`
- [x] Resource subscriptions and notifications
- [x] Resource content types: Text, Blob

### Prompts

- [x] Prompt attributes: `name`, `description`, `title`, `icons`, `arguments`
- [x] `prompts/list`, `prompts/get`
- [x] `notifications/prompts/list_changed`

### Content Types

- [x] `text` - TextContent with annotations
- [x] `image` - ImageContent with annotations
- [x] `audio` - AudioContent with annotations
- [x] `resource` - EmbeddedResource with annotations
- [x] `tool_use` - ToolUseContent
- [x] `tool_result` - ToolResultContent with `structuredContent`

### Sampling

- [x] `sampling/createMessage` request/response
- [x] `tools` and `toolChoice` parameters for tool use in sampling
- [x] `ModelPreferences` with hints, priorities
- [x] Content types in messages

### Elicitations

- [x] Form mode (`mode: "form"`) with `requestedSchema`
- [x] URL mode (`mode: "url"`) with `url`, `elicitationId`
- [x] Response actions: `accept`, `decline`, `cancel`

### Tasks

- [x] Task model: `taskId`, `status`, `statusMessage`, `createdAt`, `ttl`, `pollInterval`
- [x] Task operations: `tasks/get`, `tasks/list`, `tasks/cancel`, `tasks/result`
- [x] `notifications/tasks/status`
- [x] Task augmentation via `TaskMeta`
- [x] `RelatedTaskMetadata` (`io.modelcontextprotocol/related-task`)

### Capabilities

- [x] Server capabilities: `tools`, `prompts`, `resources`, `completions`, `logging`, `experimental`, `tasks`
- [x] Client capabilities: `roots`, `sampling`, `experimental`, `elicitation`, `tasks`
- [x] Task-specific capabilities for both client and server

---

## Identified Gaps

### 1. Missing Content Type: `resource_link`

**Spec Requirement:** The spec defines a `resource_link` content type (distinct from `resource`/EmbeddedResource) that allows tools to return links to resources
without embedding their content.

**Current State:** Only `Content.EmbeddedResource` exists.

**Required Changes:**

```kotlin
// Add to Content.kt
@JsonSerializable
@PolymorphicLabel("resource_link")
data class ResourceLink(
    val uri: Uri,
    val name: ResourceName,
    val description: String? = null,
    val mimeType: MimeType? = null,
    val annotations: Annotations? = null
) : Content()
```

**Files to Change:**

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/model/Content.kt`

---

### 2. Missing Notification: `notifications/elicitation/complete`

**Spec Requirement:** For URL mode elicitation, servers can send `notifications/elicitation/complete` to notify clients when an out-of-band interaction is
complete.

**Current State:** Not implemented.

**Required Changes:**

```kotlin
// Add to McpElicitations.kt
object Complete : McpRpc {
    override val Method = McpRpcMethod.of("notifications/elicitation/complete")

    @JsonSerializable
    data class Notification(
        val elicitationId: ElicitationId
    ) : ServerMessage.Notification
}
```

**Files to Change:**

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/messages/McpElicitations.kt`
- Server protocol handlers to send this notification

---

### 3. Missing Error Type: `URLElicitationRequiredError` (-32042)

**Spec Requirement:** Servers can return a special error (code -32042) indicating that URL elicitation is required before a request can proceed.

**Current State:** Not implemented.

**Required Changes:**

```kotlin
// Add to McpError.kt or McpException.kt
data class URLElicitationRequiredError(
    val elicitations: List<McpElicitations.Request.Url>
) : McpError {
    companion object {
        const val CODE = -32042
    }
}
```

**Files to Change:**

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/McpError.kt`
- Error handling in protocol layer

---

### 4. Missing Client Sampling Sub-capabilities: `tools` and `context`

**Spec Requirement:** The `sampling` client capability should have optional sub-capabilities:

- `sampling.tools` - indicates client supports tool use in sampling
- `sampling.context` - indicates client supports `includeContext` parameter (soft-deprecated)

**Current State:** `sampling` capability is just `Unit?` without sub-capabilities.

**Required Changes:**

```kotlin
// Update ClientCapabilities.kt
@JsonSerializable
data class Sampling(
    val tools: Unit? = null,
    val context: Unit? = null
)
```

**Files to Change:**

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/ClientCapabilities.kt`
- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/ClientProtocolCapability.kt`

---

### 5. Task Field Name: `lastUpdatedAt` vs `updatedAt`

**Spec Requirement:** The spec uses `lastUpdatedAt` for the timestamp field.

**Current State:** Implementation uses `updatedAt`.

**Required Changes:**

```kotlin
// Update Task.kt
@JsonSerializable
data class Task(
    val taskId: TaskId,
    val status: TaskStatus,
    val statusMessage: String? = null,
    val createdAt: Instant,
    val lastUpdatedAt: Instant,  // Renamed from updatedAt
    val ttl: TimeToLive? = null,
    val pollInterval: Int? = null
)
```

**Files to Change:**

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/model/Task.kt`
- All usages of `updatedAt` in task handling code

---

### 6. Sampling Response Content: Single vs Array

**Spec Requirement:** In `CreateMessageResult`, the `content` field can be:

- A single content block: `TextContent`, `ImageContent`, `AudioContent`, `ToolUseContent`, or `ToolResultContent`
- OR an array of `SamplingMessageContentBlock` items

This is important for tool use scenarios where the LLM may return multiple `tool_use` blocks in a single response.

**Current State:** `McpSampling.Response` has `val content: Content` (single item only).

**Required Changes:**

```kotlin
// Option 1: Use a wrapper type that handles both
@JsonSerializable
sealed class SamplingContent {
    data class Single(val content: Content) : SamplingContent()
    data class Multiple(val contents: List<Content>) : SamplingContent()
}

// Option 2: Always use List<Content> and handle single items as single-element lists
@JsonSerializable
data class Response(
    val model: ModelName,
    val stopReason: StopReason?,
    val role: Role,
    val content: List<Content>  // Always array, serialize single item as array with one element
) : ClientMessage.Response
```

**Note:** The second option (always using List) is simpler but requires custom serialization to handle the spec's flexibility.

**Files to Change:**

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/messages/McpSampling.kt`
- Possibly custom JsonAdapter for this polymorphic field

---

## Lower Priority / Optional Improvements

### 7. JSON Schema Default Dialect (2020-12)

**Spec Note:** The spec establishes JSON Schema 2020-12 as the default dialect. May want to document or enforce this.

### 8. Tool Name Guidelines

**Spec Note:** Tool names SHOULD be 1-128 chars, case-sensitive, alphanumeric plus `_`, `-`, `.`. May want to add validation.

### 9. OpenID Connect Discovery Support

**Spec Note:** Enhanced authorization server discovery with OIDC support. This is an authorization concern that may already be handled.

---

## Recommended Implementation Order

1. **Task field rename** (`updatedAt` → `lastUpdatedAt`) - Simple, breaking change to address early
2. **ResourceLink content type** - New feature, additive
3. **Client sampling sub-capabilities** - Simple capability addition
4. **Elicitation complete notification** - New feature, additive
5. **URLElicitationRequiredError** - New error type, additive
6. **Sampling response content array support** - May require design decision

---

## Impact Assessment

| Gap                         | Breaking Change | Effort | Priority |
|-----------------------------|-----------------|--------|----------|
| Task field rename           | Yes             | Low    | High     |
| ResourceLink content        | No              | Low    | Medium   |
| Sampling sub-capabilities   | No              | Low    | Medium   |
| Elicitation complete        | No              | Medium | Medium   |
| URLElicitationRequiredError | No              | Low    | Low      |
| Sampling content array      | Possibly        | Medium | Low      |

---

## Testing Considerations

The `pro/ai/mcp/conformance` module should be updated to test:

- ResourceLink content in tool responses
- Elicitation complete notification flow
- URLElicitationRequiredError handling
- Sampling with tools returning multiple content blocks
