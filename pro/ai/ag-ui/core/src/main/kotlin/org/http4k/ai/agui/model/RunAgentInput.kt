/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.model

import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable

/**
 * Payload sent by an AG-UI client to a server to start an agent run.
 *
 * Mirrors the `RunAgentInput` type from the AG-UI core SDK. The server streams back a
 * sequence of [org.http4k.ai.agui.event.AgUiEvent] over text/event-stream.
 */
@JsonSerializable
data class RunAgentInput(
    val threadId: ThreadId,
    val runId: RunId,
    val messages: List<AgUiMessage> = emptyList(),
    val tools: List<AgUiTool> = emptyList(),
    val context: List<Context> = emptyList(),
    val state: MoshiNode? = null,
    val forwardedProps: MoshiNode? = null
)
