package org.http4k.mcp

import org.http4k.connect.model.Role
import org.http4k.core.Request
import org.http4k.lens.McpLensTarget
import org.http4k.mcp.model.Content.Text
import org.http4k.mcp.model.Message
import org.http4k.mcp.model.Meta
import org.http4k.mcp.server.protocol.Client
import org.http4k.mcp.server.protocol.Client.Companion.NoOp

/**
 * A PromptHandler is a function which creates a Prompt from a set of inputs
 */
typealias PromptHandler = (PromptRequest) -> PromptResponse

fun interface PromptFilter {
    operator fun invoke(request: PromptHandler): PromptHandler
    companion object
}

val PromptFilter.Companion.NoOp: PromptFilter get() = PromptFilter { it }

fun PromptFilter.then(next: PromptFilter): PromptFilter = PromptFilter { this(next(it)) }

fun PromptFilter.then(next: PromptHandler): PromptHandler = this(next)

data class PromptRequest(
    val args: Map<String, String> = emptyMap(),
    val meta: Meta = Meta.default,
    val client: Client = NoOp,
    val connectRequest: Request? = null
) : Map<String, String> by args, McpLensTarget

data class PromptResponse(val messages: List<Message>, val description: String? = null) {
    constructor(vararg messages: Message, description: String? = null) : this(messages.toList(), description)
    constructor(role: Role, content: String) : this(listOf(Message(role, Text(content))))
}
