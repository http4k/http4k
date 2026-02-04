# MCP Apps Implementation Plan

## Overview

MCP Apps (SEP-1865) enables servers to deliver interactive user interfaces to hosts through the `io.modelcontextprotocol/ui` extension.

## Reference Spec

Download to `docs/mcp-apps/`:

- `docs/mcp-apps/2026-01-26/apps.mdx` - Released spec (v2026-01-26)
- `docs/mcp-apps/draft/apps.mdx` - Draft spec

Source: `https://github.com/modelcontextprotocol/ext-apps/tree/main/specification`

---

## Core Components

### 1. UI Resources

Resources with `ui://` URI scheme and `text/html;profile=mcp-app` MIME type.

```kotlin
data class UIResourceMeta(
    val csp: McpUiResourceCsp? = null,
    val permissions: UIPermissions? = null,
    val domain: String? = null,
    val prefersBorder: Boolean? = null
)

data class McpUiResourceCsp(
    val connectDomains: List<String>? = null,
    val resourceDomains: List<String>? = null,
    val frameDomains: List<String>? = null,
    val baseUriDomains: List<String>? = null
)

data class UIPermissions(
    val camera: Unit? = null,
    val microphone: Unit? = null,
    val geolocation: Unit? = null,
    val clipboardWrite: Unit? = null
)
```

### 2. Tool-UI Linkage

Tools associate with UI resources through `_meta.ui`:

```kotlin
data class McpUiToolMeta(
    val resourceUri: Uri? = null,
    val visibility: List<UIVisibility>? = null  // ["model", "app"]
)

enum class UIVisibility { model, app }
```

### 3. Host Capabilities & Context

```kotlin
data class HostCapabilities(
    val experimental: Unit? = null,
    val openLinks: Unit? = null,
    val serverTools: ServerToolsCapability? = null,
    val serverResources: ServerResourcesCapability? = null,
    val logging: Unit? = null,
    val sandbox: SandboxCapability? = null
)

data class HostContext(
    val toolInfo: ToolInfo? = null,
    val theme: Theme? = null,           // "light" | "dark"
    val styles: Styles? = null,
    val displayMode: DisplayMode? = null,
    val availableDisplayModes: List<DisplayMode>? = null,
    val containerDimensions: ContainerDimensions? = null,
    val locale: String? = null,         // BCP 47
    val timeZone: String? = null,       // IANA
    val userAgent: String? = null,
    val platform: Platform? = null,     // "web" | "desktop" | "mobile"
    val deviceCapabilities: DeviceCapabilities? = null,
    val safeAreaInsets: SafeAreaInsets? = null
)

enum class Theme { light, dark }
enum class DisplayMode { inline, fullscreen, pip }
enum class Platform { web, desktop, mobile }
```

### 4. App Capabilities (View declares)

```kotlin
data class AppCapabilities(
    val experimental: Unit? = null,
    val tools: ToolsCapability? = null,
    val availableDisplayModes: List<DisplayMode>? = null
)
```

---

## Protocol Messages

### View-to-Host Requests

| Method                    | Purpose        | Params                             |
|---------------------------|----------------|------------------------------------|
| `ui/initialize`           | Handshake      | `{ appCapabilities }`              |
| `ui/open-link`            | Open URL       | `{ url: string }`                  |
| `ui/message`              | Send to chat   | `{ role, content }`                |
| `ui/request-display-mode` | Change display | `{ mode }`                         |
| `ui/update-model-context` | Update context | `{ content?, structuredContent? }` |

### Host-to-View Notifications

| Method                                  | Purpose            | Params                 |
|-----------------------------------------|--------------------|------------------------|
| `ui/notifications/initialized`          | Handshake complete | `{}`                   |
| `ui/notifications/tool-input`           | Complete tool args | `{ arguments }`        |
| `ui/notifications/tool-input-partial`   | Streaming args     | `{ arguments }`        |
| `ui/notifications/tool-result`          | Tool result        | `CallToolResult`       |
| `ui/notifications/tool-cancelled`       | Cancelled          | `{ reason }`           |
| `ui/resource-teardown`                  | Shutdown           | `{ reason }`           |
| `ui/notifications/size-changed`         | Size changed       | `{ width, height }`    |
| `ui/notifications/host-context-changed` | Context update     | `Partial<HostContext>` |

### Sandbox Proxy Messages (Web hosts)

| Method                                    | Purpose            |
|-------------------------------------------|--------------------|
| `ui/notifications/sandbox-proxy-ready`    | Proxy ready        |
| `ui/notifications/sandbox-resource-ready` | HTML ready to load |

---

## Files to Create

### Model Types (in `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/model/`)

- [ ] `UIResourceMeta.kt`
- [ ] `McpUiResourceCsp.kt`
- [ ] `UIPermissions.kt`
- [ ] `McpUiToolMeta.kt`
- [ ] `UIVisibility.kt`
- [ ] `HostCapabilities.kt`
- [ ] `HostContext.kt`
- [ ] `AppCapabilities.kt`
- [ ] `DisplayMode.kt`
- [ ] `Theme.kt`
- [ ] `Platform.kt`
- [ ] `ContainerDimensions.kt`
- [ ] `Styles.kt`
- [ ] `DeviceCapabilities.kt`
- [ ] `SafeAreaInsets.kt`

### Protocol Messages (in `pro/ai/mcp/core/src/main/kotlin/org/http4k/ai/mcp/protocol/messages/`)

- [ ] `McpUiInitialize.kt` - Initialize request/response
- [ ] `McpUiOpenLink.kt` - Open link request
- [ ] `McpUiMessage.kt` - Chat message request
- [ ] `McpUiRequestDisplayMode.kt` - Display mode request
- [ ] `McpUiUpdateModelContext.kt` - Model context update
- [ ] `McpUiNotifications.kt` - All host-to-view notifications

### Extension Registration

- [ ] Update `ClientCapabilities` with extensions support (see PLAN.md)
- [ ] Helper for declaring MCP Apps extension capability

---

## Implementation Phases

### Phase 1: Download Spec & Core Types

1. Download spec files to `docs/mcp-apps/`
2. Create model types for UI resources
3. Create model types for host/app capabilities

### Phase 2: Protocol Messages

1. Define all request/response types
2. Define all notification types
3. Add JSON serialization annotations

### Phase 3: Server-side Support

1. UI resource declaration helpers
2. Tool-UI linkage helpers
3. Extension capability negotiation

### Phase 4: Tests

1. Unit tests for all types
2. Serialization/deserialization tests
3. Example MCP server with UI resource

---

## Prerequisites

- PLAN.md Gap 1 (extensions field) must be completed first

---

## Verification

```bash
./gradlew :http4k-ai-mcp-core:test :http4k-ai-mcp-sdk:test
```

---

## Completed

(none yet)
