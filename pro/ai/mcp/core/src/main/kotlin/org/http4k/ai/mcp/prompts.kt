/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp

import org.http4k.ai.mcp.Client.Companion.NoOp
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Meta.Companion.default
import org.http4k.ai.model.Role
import org.http4k.core.Request
import org.http4k.lens.McpLensTarget

/**
 * A PromptHandler is a function which creates a Prompt from a set of inputs
 */
typealias PromptHandler = (PromptRequest) -> PromptResponse

fun interface PromptFilter {
    operator fun invoke(handler: PromptHandler): PromptHandler
    companion object
}

val PromptFilter.Companion.NoOp: PromptFilter get() = PromptFilter { it }

fun PromptFilter.then(next: PromptFilter): PromptFilter = PromptFilter { this(next(it)) }

fun PromptFilter.then(next: PromptHandler): PromptHandler = this(next)

data class PromptRequest(
    val args: Map<String, String> = emptyMap(),
    override val meta: Meta = default,
    val client: Client = NoOp,
    val connectRequest: Request? = null
) : Map<String, String> by args, CapabilityRequest, McpLensTarget

data class PromptResponse(val messages: List<Message>, val description: String? = null) {
    constructor(vararg messages: Message, description: String? = null) : this(messages.toList(), description)
    constructor(role: Role, content: String) : this(listOf(Message(role, Text(content))))
}
