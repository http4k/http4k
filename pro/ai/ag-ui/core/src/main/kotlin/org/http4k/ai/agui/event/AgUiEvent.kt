/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic

/**
 * Base of the AG-UI event hierarchy. Events are streamed agent → client over Server-Sent
 * Events (or any other transport). The JSON discriminator is the `type` field, matching the
 * AG-UI core SDK `EventType` enum (e.g. `"RUN_STARTED"`, `"TEXT_MESSAGE_CONTENT"`).
 */
@JsonSerializable
@Polymorphic("type")
sealed class AgUiEvent {
    abstract val timestamp: Long?
    abstract val rawEvent: Any?
}
