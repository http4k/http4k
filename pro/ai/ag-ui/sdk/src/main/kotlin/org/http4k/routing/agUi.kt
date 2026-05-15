/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.agui.AgUiHandler
import org.http4k.ai.agui.event.AgUiEvent
import org.http4k.ai.agui.model.RunAgentInput
import org.http4k.ai.agui.util.AgUiJson
import org.http4k.ai.agui.util.AgUiJson.auto
import org.http4k.ai.agui.util.AgUiJson.json
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.contentType
import org.http4k.sse.SseMessage
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

private val runAgentInputLens = Body.auto<RunAgentInput>().toLens()

/**
 * Create an AG-UI server endpoint.
 *
 * Binds `POST {basePath}` (defaults to `/`) to accept a [RunAgentInput] JSON body and stream
 * the handler's events back as `text/event-stream`. The endpoint contract matches the
 * standard AG-UI HTTP/SSE transport used by the CopilotKit reference clients.
 *
 * Example:
 * ```
 * val server = agUi { input ->
 *     sequenceOf(
 *         RunStarted(input.threadId, input.runId),
 *         TextMessageChunk(MessageId.random(), Role.Assistant, "hello"),
 *         RunFinished(input.threadId, input.runId)
 *     )
 * }
 * ```
 */
fun agUi(basePath: String = "/", handler: AgUiHandler): RoutingHttpHandler =
    CatchAll()
        .then(CatchLensFailure())
        .then(
            routes(
                basePath bind POST to { req ->
                    Response(OK)
                        .contentType(ContentType.TEXT_EVENT_STREAM)
                        .body(handler(req.json()).toSseStream())
                }
            )
        )

internal fun Sequence<AgUiEvent>.toSseStream(): InputStream {
    val pipedIn = PipedInputStream()
    val pipedOut = PipedOutputStream(pipedIn)

    thread(isDaemon = true) {
        pipedOut.use { out ->
            for (event in this) {
                val json = AgUiJson.asJsonString(event, AgUiEvent::class)
                out.write(SseMessage.Data(json).toMessage().toByteArray())
                out.flush()
            }
        }
    }

    return pipedIn
}
