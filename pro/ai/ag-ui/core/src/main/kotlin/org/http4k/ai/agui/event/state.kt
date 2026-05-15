/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import org.http4k.ai.agui.model.AgUiMessage
import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("STATE_SNAPSHOT")
data class StateSnapshot(
    val snapshot: MoshiNode,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

/**
 * State delta carrying a JSON Patch (RFC 6902) array.
 */
@JsonSerializable
@PolymorphicLabel("STATE_DELTA")
data class StateDelta(
    val delta: List<MoshiNode>,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("MESSAGES_SNAPSHOT")
data class MessagesSnapshot(
    val messages: List<AgUiMessage>,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()
