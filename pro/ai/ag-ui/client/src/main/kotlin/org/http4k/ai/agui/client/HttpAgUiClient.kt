/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.agui.AgUiError
import org.http4k.ai.agui.AgUiResult
import org.http4k.ai.agui.event.AgUiEvent
import org.http4k.ai.agui.model.RunAgentInput
import org.http4k.ai.agui.util.AgUiJson
import org.http4k.ai.agui.util.AgUiJson.json
import org.http4k.core.Accept
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.QualifiedContent
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence

/**
 * HTTP/SSE implementation of [AgUiClient].
 *
 * Posts the [RunAgentInput] as JSON and parses the response as an SSE stream of events.
 * The supplied [HttpHandler] **must** stream the response body — typically
 * `JavaHttpClient(responseBodyMode = Stream)`.
 */
class HttpAgUiClient(
    private val baseUri: Uri,
    private val http: HttpHandler
) : AgUiClient {
    override fun invoke(input: RunAgentInput): AgUiResult<Sequence<AgUiEvent>> {
        val response = http(
            Request(POST, baseUri)
                .with(Header.ACCEPT of Accept(listOf(QualifiedContent(ContentType.TEXT_EVENT_STREAM))))
                .json(input)
        )
        if (!response.status.successful) return Failure(AgUiError.Http(response))

        return Success(
            response.body.stream.chunkedSseSequence()
                .filterIsInstance<SseMessage.Data>()
                .map { AgUiJson.asA<AgUiEvent>(it.data) }
        )
    }
}
