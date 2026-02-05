package org.http4k.ai.mcp.protocol

import org.http4k.ai.mcp.protocol.ServerProtocolCapability.Experimental
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.Logging
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.PromptsChanged
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.ResourcesChanged
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.Completions
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.TaskCancel
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.TaskList
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.TaskToolCall
import org.http4k.ai.mcp.protocol.ServerProtocolCapability.ToolsChanged
import se.ansman.kotshi.JsonSerializable

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
    val extensions: Map<String, Any> = emptyMap(),
) {
    constructor(vararg capabilities: ServerProtocolCapability = ServerProtocolCapability.entries.toTypedArray()) : this(
        ToolCapabilities(capabilities.contains(ToolsChanged)),
        PromptCapabilities(capabilities.contains(PromptsChanged)),
        ResourceCapabilities(capabilities.contains(ResourcesChanged)),
        if (capabilities.contains(Completions)) Unit else null,
        if (capabilities.contains(Logging)) Unit else null,
        if (capabilities.contains(Experimental)) Unit else null,
        buildTasks(capabilities.toList())
    )

    fun withExtensions(vararg extensions: Pair<String, Any>) = copy(extensions = extensions.toMap())

    @JsonSerializable
    data class ToolCapabilities(val listChanged: Boolean? = false)

    @JsonSerializable
    data class PromptCapabilities(val listChanged: Boolean = false)

    @JsonSerializable
    data class ResourceCapabilities(val subscribe: Boolean? = false, val listChanged: Boolean? = false)

    @JsonSerializable
    data class Tasks(
        val list: Unit? = null,
        val cancel: Unit? = null,
        val requests: TaskRequests? = null
    )

    @JsonSerializable
    data class TaskRequests(
        val tools: ToolRequests? = null
    )

    @JsonSerializable
    data class ToolRequests(
        val call: Unit? = null
    )

    companion object {
        private fun buildTasks(capabilities: List<ServerProtocolCapability>): Tasks? {
            val hasList = capabilities.contains(TaskList)
            val hasCancel = capabilities.contains(TaskCancel)
            val hasToolCall = capabilities.contains(TaskToolCall)

            if (!hasList && !hasCancel && !hasToolCall) return null

            return Tasks(
                list = if (hasList) Unit else null,
                cancel = if (hasCancel) Unit else null,
                requests = TaskRequests(
                    tools = if (hasToolCall) ToolRequests(Unit) else null
                )
            )
        }
    }
}

