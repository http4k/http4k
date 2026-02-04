# MCP Draft Spec Implementation Plan

## Summary

Update http4k MCP implementation to align with the MCP Draft specification (`DRAFT-2026-v1`).

## Current Status

The http4k implementation is **already highly compliant**. Most draft features are implemented:

| Feature                              | Status    |
|--------------------------------------|-----------|
| Tasks (full system)                  | ✅         |
| Elicitation (Form + URL modes)       | ✅         |
| Roots                                | ✅         |
| Tool.outputSchema                    | ✅         |
| ToolExecution (taskSupport)          | ✅         |
| Tool.icons                           | ✅         |
| AudioContent                         | ✅         |
| ToolUseContent / ToolResultContent   | ✅         |
| ResourceLink                         | ✅         |
| ToolChoice (auto/none/required)      | ✅         |
| ModelPreferences                     | ✅         |
| Annotations.lastModified             | ✅         |
| Implementation description/icons     | ✅         |
| Protocol version DRAFT               | ✅         |
| **extensions field in capabilities** | ❌ Missing |

---

## Gap 1: Extensions Field in Capabilities

### Requirement

The draft spec adds an `extensions` field to both `ClientCapabilities` and `ServerCapabilities` for extension capability negotiation.

### Spec Reference

```typescript
interface ClientCapabilities {
  roots?: { listChanged?: boolean };
  sampling?: { tools?: {}; context?: {} };
  elicitation?: { form?: {}; url?: {} };
  tasks?: { ... };
  experimental?: {};
  extensions?: Record<string, object>;  // NEW
}

interface ServerCapabilities {
  tools?: { listChanged?: boolean };
  prompts?: { listChanged?: boolean };
  resources?: { subscribe?: boolean; listChanged?: boolean };
  completions?: {};
  logging?: {};
  tasks?: { ... };
  experimental?: {};
  extensions?: Record<string, object>;  // NEW
}
```

### Example Usage

```json
{
  "capabilities": {
    "extensions": {
      "io.modelcontextprotocol/ui": {
        "mimeTypes": ["text/html;profile=mcp-app"]
      }
    }
  }
}
```

### Files to Modify

- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/ClientCapabilities.kt`
- `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/ServerCapabilities.kt`

### Implementation

```kotlin
// ClientCapabilities.kt - add field
@JsonSerializable
@ConsistentCopyVisibility
data class ClientCapabilities internal constructor(
    val roots: Roots?,
    val sampling: SamplingCapability?,
    val experimental: Unit?,
    val elicitation: Elicitation?,
    val tasks: Tasks?,
    val extensions: Map<String, Any>? = null  // ADD
)

// ServerCapabilities.kt - add field
@JsonSerializable
@ConsistentCopyVisibility
data class ServerCapabilities internal constructor(
    val tools: ToolCapabilities?,
    val prompts: PromptCapabilities?,
    val resources: ResourceCapabilities?,
    val completions: Unit?,
    val logging: Unit?,
    val experimental: Unit?,
    val tasks: Tasks?,
    val extensions: Map<String, Any>? = null  // ADD
)
```

### Tests

- [ ] Extensions field serializes correctly to JSON
- [ ] Extensions field deserializes correctly from JSON
- [ ] Null extensions field is omitted from JSON output
- [ ] Can negotiate MCP Apps extension capability

---

## Verification

```bash
./gradlew :http4k-ai-mcp-core:test :http4k-ai-mcp-sdk:test :http4k-ai-mcp-client:test
```

---

## Completed

(none yet)
