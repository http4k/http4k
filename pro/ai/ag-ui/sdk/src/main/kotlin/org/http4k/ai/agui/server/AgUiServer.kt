/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.server

import org.http4k.ai.agui.event.AgUiEvent
import org.http4k.ai.agui.model.RunAgentInput

/**
 * Server-side handler for an AG-UI run. Given the input payload posted by the client, returns
 * a (typically lazy) sequence of events to stream back as SSE.
 */
fun interface AgUiHandler : (RunAgentInput) -> Sequence<AgUiEvent>
