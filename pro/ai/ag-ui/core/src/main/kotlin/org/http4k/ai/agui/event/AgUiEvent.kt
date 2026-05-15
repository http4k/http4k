/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import org.http4k.connect.model.Timestamp
import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic

@JsonSerializable
@Polymorphic("type")
sealed class AgUiEvent {
    abstract val timestamp: Timestamp?
    abstract val rawEvent: MoshiNode?
}
