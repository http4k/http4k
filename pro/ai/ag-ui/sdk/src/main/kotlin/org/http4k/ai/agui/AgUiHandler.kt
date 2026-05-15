package org.http4k.ai.agui

import org.http4k.ai.agui.event.AgUiEvent
import org.http4k.ai.agui.model.RunAgentInput

/**
 * Server-side handler for an AG-UI run. Given the input payload posted by the client, returns
 * a (typically lazy) sequence of events to stream back as SSE.
 */
fun interface AgUiHandler : (RunAgentInput) -> Sequence<AgUiEvent>
