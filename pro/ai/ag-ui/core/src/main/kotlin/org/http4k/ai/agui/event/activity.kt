/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import org.http4k.ai.agui.model.ActivityType
import org.http4k.ai.agui.model.MessageId
import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("ACTIVITY_SNAPSHOT")
data class ActivitySnapshot(
    val messageId: MessageId,
    val activityType: ActivityType,
    val content: MoshiNode,
    val replace: Boolean? = null,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("ACTIVITY_DELTA")
data class ActivityDelta(
    val messageId: MessageId,
    val activityType: ActivityType,
    val patch: List<MoshiNode>,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()
