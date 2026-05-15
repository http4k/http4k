/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import org.http4k.ai.agui.model.CustomEventName
import org.http4k.ai.agui.model.RawEventSource
import org.http4k.connect.model.Timestamp
import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("RAW")
data class Raw(
    val event: MoshiNode,
    val source: RawEventSource? = null,
    override val timestamp: Timestamp? = null,
    override val rawEvent: MoshiNode? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("CUSTOM")
data class Custom(
    val name: CustomEventName,
    val value: MoshiNode,
    override val timestamp: Timestamp? = null,
    override val rawEvent: MoshiNode? = null
) : AgUiEvent()
