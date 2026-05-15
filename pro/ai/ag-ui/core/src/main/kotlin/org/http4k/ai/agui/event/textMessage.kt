/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import org.http4k.ai.agui.model.MessageId
import org.http4k.ai.model.Role
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("TEXT_MESSAGE_START")
data class TextMessageStart(
    val messageId: MessageId,
    val role: Role,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("TEXT_MESSAGE_CONTENT")
data class TextMessageContent(
    val messageId: MessageId,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("TEXT_MESSAGE_END")
data class TextMessageEnd(
    val messageId: MessageId,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("TEXT_MESSAGE_CHUNK")
data class TextMessageChunk(
    val messageId: MessageId? = null,
    val role: Role? = null,
    val delta: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()
