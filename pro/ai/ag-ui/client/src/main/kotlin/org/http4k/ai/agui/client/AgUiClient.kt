/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.client

import org.http4k.ai.agui.AgUiResult
import org.http4k.ai.agui.event.AgUiEvent
import org.http4k.ai.agui.model.RunAgentInput

/**
 * Client for an AG-UI server.
 *
 * Sends a [RunAgentInput] to start an agent run and returns the stream of events the server
 * emits. The initial HTTP/transport failure surfaces as `Failure(AgUiError)`; failures that
 * occur during the run are expressed natively as `RunError` events within the sequence.
 */
fun interface AgUiClient : (RunAgentInput) -> AgUiResult<Sequence<AgUiEvent>>
