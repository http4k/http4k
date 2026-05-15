/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.agui.model.RunAgentInput
import org.http4k.ai.agui.server.AgUiHandler
import org.http4k.ai.agui.server.toSseStream
import org.http4k.ai.agui.util.AgUiJson.auto
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.contentType

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
                    val input = runAgentInputLens(req)
                    Response(OK)
                        .contentType(ContentType.TEXT_EVENT_STREAM)
                        .body(handler(input).toSseStream())
                }
            )
        )
