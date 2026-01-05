package org.http4k.ai.mcp.protocol

enum class ServerProtocolCapability {
    ToolsChanged,
    PromptsChanged,
    ResourcesChanged,
    RootChanged,
    Completions,
    Logging,
    Experimental,
    TaskList,
    TaskCancel,
    TaskToolCall
}

