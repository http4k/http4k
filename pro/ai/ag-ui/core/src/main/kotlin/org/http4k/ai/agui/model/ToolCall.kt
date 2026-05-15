/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.model

import org.http4k.ai.model.ToolName
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ToolCallFunction(
    val name: ToolName,
    val arguments: String
)

@JsonSerializable
data class ToolCall(
    val id: ToolCallId,
    val function: ToolCallFunction,
    val type: String = "function"
)
