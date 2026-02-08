# http4k MCP Apps Test Host

## Overview

Minimal test host for MCP Apps. Single dropdown (server-rendered), select tool, app loads in iframe.

## References

- **MCP Apps Spec**: https://modelcontextprotocol.io/docs/extensions/apps
- **Full Spec**: https://github.com/modelcontextprotocol/ext-apps/blob/main/specification/2026-01-26/apps.mdx
- **SDK Repo**: https://github.com/modelcontextprotocol/ext-apps

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  Browser                                                            │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  <select> (server-rendered by http4k)                         │  │
│  │    - Option per UI-enabled tool                               │  │
│  │    - data-server-id, data-resource-uri on each option         │  │
│  └──────────────────────────┬────────────────────────────────────┘  │
│                             │ postMessage                           │
│  ┌──────────────────────────▼────────────────────────────────────┐  │
│  │  <iframe srcdoc="..."> (MCP App HTML)                         │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────────────┘
                              │ fetch
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  http4k Host                                                        │
│                                                                     │
│  GET  /                    → HTML with pre-populated dropdown       │
│  GET  /api/resources       → Fetch UI HTML from MCP server          │
│  POST /api/tools/call      → Proxy tool call to MCP server          │
│                                                                     │
│  MCP Client ───────────────────────────────────────▶ MCP Server     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Two Layers of Communication

The browser JS acts as a relay between the iframe and http4k:

```
App (iframe)                    Browser JS                      http4k
     │                              │                              │
     │  tools/call (postMessage)    │                              │
     │ ──────────────────────────▶  │                              │
     │                              │  /api/tools/call (HTTP)      │
     │                              │ ──────────────────────────▶  │
     │                              │                              │  MCP Server
     │                              │                              │ ──────────▶
     │                              │  ◀──────────────────────────  │
     │  ◀──────────────────────────  │                              │
     │      (postMessage response)  │                              │
```

| Name              | Protocol                  | From → To           |
|-------------------|---------------------------|---------------------|
| `tools/call`      | JSON-RPC over postMessage | App → Browser JS    |
| `/api/tools/call` | HTTP POST                 | Browser JS → http4k |

---

## JSON-RPC Methods (postMessage)

These are JSON-RPC methods the app sends via `postMessage`. Browser JS handles them.

| Method                  | Proxy to http4k?            | What to do                                |
|-------------------------|-----------------------------|-------------------------------------------|
| `ui/initialize`         | No                          | Return capabilities + host context        |
| `tools/call`            | **Yes** → `/api/tools/call` | Forward to http4k, return result          |
| `ui/openLink`           | No                          | `window.open(url, '_blank')`, return `{}` |
| `ui/message`            | No                          | Log it, return `{}`                       |
| `ui/updateModelContext` | No                          | Log it, return `{}`                       |
| `ui/requestDisplayMode` | No                          | Return `{ mode: "inline" }`               |

---

### Method Details

#### `ui/initialize` (required)

App sends this on startup. Return capabilities and context.

```javascript
// Request
{
    "jsonrpc"
:
    "2.0", "id"
:
    1, "method"
:
    "ui/initialize",
        "params"
:
    {
        "name"
    :
        "My App", "version"
    :
        "1.0.0"
    }
}

// Response
{
    "jsonrpc"
:
    "2.0", "id"
:
    1, "result"
:
    {
        "protocolVersion"
    :
        "2025-11-05",
            "capabilities"
    :
        {
            "serverTools"
        :
            {
            }
        ,
            "openLinks"
        :
            {
            }
        ,
            "updateModelContext"
        :
            {
                "text"
            :
                {
                }
            }
        }
    ,
        "hostContext"
    :
        {
            "theme"
        :
            "light",
                "platform"
        :
            "web",
                "displayMode"
        :
            "inline",
                "availableDisplayModes"
        :
            ["inline"]
        }
    }
}
```

#### `tools/call` (required)

App calls a tool. Browser JS proxies to `/api/tools/call`.

```javascript
// Request (postMessage from app)
{
    "jsonrpc"
:
    "2.0", "id"
:
    2, "method"
:
    "tools/call",
        "params"
:
    {
        "name"
    :
        "get-time", "arguments"
    :
        {
        }
    }
}

// Browser JS does: fetch('/api/tools/call', { body: { serverId, name, arguments } })

// Response (postMessage back to app)
{
    "jsonrpc"
:
    "2.0", "id"
:
    2, "result"
:
    {
        "content"
    :
        [{"type": "text", "text": "2026-02-07T12:00:00Z"}]
    }
}
```

#### `ui/openLink` (recommended)

App wants to open a URL.

```javascript
// Request
{
    "jsonrpc"
:
    "2.0", "id"
:
    3, "method"
:
    "ui/openLink",
        "params"
:
    {
        "url"
    :
        "https://example.com"
    }
}

// Response
{
    "jsonrpc"
:
    "2.0", "id"
:
    3, "result"
:
    {
    }
}
```

#### Other methods (optional)

Just log and return `{}`:

- `ui/message` - App sends a message
- `ui/updateModelContext` - App provides context
- `ui/requestDisplayMode` - Return `{ mode: "inline" }`

---

## HTTP Endpoints (http4k)

### `GET /` — Server-rendered HTML

http4k connects to MCP servers at startup, lists UI-enabled tools, renders dropdown.

### `GET /api/resources?serverId=...&uri=...`

Fetches UI resource from MCP server via `resources/read`.

```json
{
    "html": "<!DOCTYPE html>..."
}
```

### `POST /api/tools/call`

Proxies to MCP server's `tools/call`.

```json
// Request (from browser JS)
{
    "serverId": "0",
    "name": "get-time",
    "arguments": {}
}

// Response (from MCP server, passed back to browser JS)
{
    "content": [
        {
            "type": "text",
            "text": "..."
        }
    ]
}
```

---

## HTML Template

```html
<!DOCTYPE html>
<html>
<head>
    <title>MCP Apps Test Host</title>
    <style>
        body {
            font-family: system-ui;
            max-width: 900px;
            margin: 2rem auto;
            padding: 0 1rem;
        }

        select {
            padding: 0.5rem;
            font-size: 1rem;
            min-width: 300px;
        }

        iframe {
            width: 100%;
            border: 1px solid #ccc;
            min-height: 400px;
            margin-top: 1rem;
        }
    </style>
</head>
<body>
<h1>MCP Apps Test Host</h1>

<select id="tool-select">
    <option value="" disabled selected>Select a tool...</option>
    {{#tools}}
    <option data-server-id="{{serverId}}" data-resource-uri="{{resourceUri}}">
        {{serverName}}: {{toolName}}
    </option>
    {{/tools}}
</select>

<iframe id="app" sandbox="allow-scripts allow-forms" style="display:none;"></iframe>

<script>
    const select = document.getElementById('tool-select');
    const app = document.getElementById('app');
    let currentServerId = null;

    // Load app when tool selected
    select.addEventListener('change', async () => {
        const opt = select.selectedOptions[0];
        currentServerId = opt.dataset.serverId;
        const uri = opt.dataset.resourceUri;

        const {html} = await fetch(
            `/api/resources?serverId=${currentServerId}&uri=${encodeURIComponent(uri)}`
        ).then(r => r.json());

        app.style.display = 'block';
        app.srcdoc = html;
    });

    // Handle JSON-RPC from app (postMessage)
    addEventListener('message', async e => {
        const msg = e.data;
        if (!msg?.jsonrpc || msg.id === undefined) return;

        let result;
        switch (msg.method) {
            case 'ui/initialize':
                result = {
                    protocolVersion: '2025-11-05',
                    capabilities: {serverTools: {}, openLinks: {}, updateModelContext: {text: {}}},
                    hostContext: {theme: 'light', platform: 'web', displayMode: 'inline', availableDisplayModes: ['inline']}
                };
                break;

            case 'tools/call':
                // Proxy to http4k
                result = await fetch('/api/tools/call', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({serverId: currentServerId, ...msg.params})
                }).then(r => r.json());
                break;

            case 'ui/openLink':
                window.open(msg.params.url, '_blank');
                result = {};
                break;

            case 'ui/message':
            case 'ui/updateModelContext':
            case 'ui/requestDisplayMode':
                console.log('[HOST]', msg.method, msg.params);
                result = {};
                break;

            default:
                e.source.postMessage({
                    jsonrpc: '2.0', id: msg.id,
                    error: {code: -32601, message: 'Method not found'}
                }, '*');
                return;
        }

        e.source.postMessage({jsonrpc: '2.0', id: msg.id, result}, '*');
    });
</script>
</body>
</html>
```

---

## http4k Structure

```kotlin
// Startup: connect to MCP servers, list UI-enabled tools
val servers: List<ServerInfo> = config.serverUrls.mapIndexed { index, url ->
    val client = McpClient.connect(url)
    val tools = client.listTools().tools
        .filter { it.meta?.ui?.resourceUri != null }
    ServerInfo(id = index.toString(), name = client.serverInfo.name, client = client, tools = tools)
}

// Routes
routes(
    "/" bind GET to {
        Response(OK).body(templates.render("host.html", mapOf("tools" to allToolOptions)))
    },
    "/api/resources" bind GET to { req ->
        val serverId = req.query("serverId")!!
        val uri = req.query("uri")!!
        val server = servers[serverId.toInt()]
        val resource = server.client.readResource(uri)
        val html = resource.contents.first().text
        Response(OK).body(Json.obj("html" to html))
    },
    "/api/tools/call" bind POST to { req ->
        val body = Json.parse<ToolCallRequest>(req.bodyString())
        val server = servers[body.serverId.toInt()]
        val result = server.client.callTool(body.name, body.arguments)
        Response(OK).body(Json.stringify(result))
    }
)
```

---

## Flow

```
1. http4k starts → connects to MCP servers → lists UI-enabled tools
2. User opens / → gets HTML with pre-populated <select>
3. User selects tool from dropdown
4. Browser JS: GET /api/resources → http4k: resources/read → MCP server
5. Browser JS: iframe.srcdoc = html
6. App: ui/initialize (postMessage) → Browser JS responds with capabilities
7. App: tools/call (postMessage) → Browser JS: POST /api/tools/call → http4k → MCP server
```

---

## Summary

| Layer               | Protocol               | Purpose                             |
|---------------------|------------------------|-------------------------------------|
| App ↔ Browser JS    | postMessage (JSON-RPC) | `ui/initialize`, `tools/call`, etc. |
| Browser JS ↔ http4k | HTTP                   | `/api/resources`, `/api/tools/call` |
| http4k ↔ MCP Server | MCP                    | `resources/read`, `tools/call`      |

**~40 lines of browser JS. 3 HTTP endpoints. No sandbox.**
