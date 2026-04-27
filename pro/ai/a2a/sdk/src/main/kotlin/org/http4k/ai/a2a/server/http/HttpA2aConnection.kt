/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.http

import dev.forkhandles.result4k.fold
import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.ai.a2a.server.protocol.A2AProtocolResponse.Single
import org.http4k.ai.a2a.server.protocol.A2AProtocolResponse.Stream
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.auto
import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.contentType
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

fun HttpA2aConnection(
    protocol: A2AProtocol,
    rpcPath: String = "/"
): RoutingHttpHandler = rpcPath bind Method.POST to { req ->
    protocol(req).fold(
        {
            when (it) {
                is Single -> Response(OK).with(jsonRpcResponseLens of it.response)

                is Stream ->
                    Response(OK)
                        .contentType(ContentType.TEXT_EVENT_STREAM)
                        .body(it.responses.toSseStream())
            }
        },
        { Response(OK).with(jsonRpcResponseLens of it) }
    )
}

private fun Sequence<A2ANodeType>.toSseStream(): InputStream {
    val pipedIn = PipedInputStream()
    val pipedOut = PipedOutputStream(pipedIn)

    thread(isDaemon = true) {
        pipedOut.use { out ->
            for (node in this) {
                val json = with(A2AJson) { node.asCompactJsonString() }
                out.write("data: $json\n\n".toByteArray())
                out.flush()
            }
        }
    }

    return pipedIn
}

private val jsonRpcResponseLens = Body.auto<A2ANodeType>().toLens()
