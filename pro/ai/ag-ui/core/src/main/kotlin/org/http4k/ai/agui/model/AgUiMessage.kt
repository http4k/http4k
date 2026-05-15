/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.model

import org.http4k.ai.model.Role
import se.ansman.kotshi.JsonSerializable

/**
 * A message in an AG-UI conversation thread.
 *
 * The shape matches the AG-UI core SDK Message type: a free-form record with a [role] and
 * optional [content], [toolCalls] (for assistant tool invocations) and [toolCallId] (for
 * tool-result messages). Use the constants on [Role] (User, Assistant, System, Tool) for
 * standard values.
 */
@JsonSerializable
data class AgUiMessage(
    val id: MessageId,
    val role: Role,
    val content: String? = null,
    val name: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: ToolCallId? = null
)
