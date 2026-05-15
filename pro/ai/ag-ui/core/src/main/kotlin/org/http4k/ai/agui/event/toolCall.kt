/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import org.http4k.ai.agui.model.MessageId
import org.http4k.ai.agui.model.ToolCallId
import org.http4k.ai.model.Role
import org.http4k.ai.model.ToolName
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("TOOL_CALL_START")
data class ToolCallStart(
    val toolCallId: ToolCallId,
    val toolCallName: ToolName,
    val parentMessageId: MessageId? = null,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("TOOL_CALL_ARGS")
data class ToolCallArgs(
    val toolCallId: ToolCallId,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("TOOL_CALL_END")
data class ToolCallEnd(
    val toolCallId: ToolCallId,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("TOOL_CALL_RESULT")
data class ToolCallResult(
    val messageId: MessageId,
    val toolCallId: ToolCallId,
    val content: String,
    val role: Role? = null,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("TOOL_CALL_CHUNK")
data class ToolCallChunk(
    val toolCallId: ToolCallId? = null,
    val toolCallName: ToolName? = null,
    val parentMessageId: MessageId? = null,
    val delta: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()
