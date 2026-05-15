/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import org.http4k.ai.agui.model.MessageId
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("REASONING_START")
data class ReasoningStart(
    val messageId: MessageId,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("REASONING_MESSAGE_START")
data class ReasoningMessageStart(
    val messageId: MessageId,
    val role: String = "reasoning",
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("REASONING_MESSAGE_CONTENT")
data class ReasoningMessageContent(
    val messageId: MessageId,
    val delta: String,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("REASONING_MESSAGE_END")
data class ReasoningMessageEnd(
    val messageId: MessageId,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("REASONING_MESSAGE_CHUNK")
data class ReasoningMessageChunk(
    val messageId: MessageId,
    val delta: String = "",
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("REASONING_END")
data class ReasoningEnd(
    val messageId: MessageId,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("REASONING_ENCRYPTED_VALUE")
data class ReasoningEncryptedValue(
    val subtype: String,
    val entityId: String,
    val encryptedValue: String,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()
