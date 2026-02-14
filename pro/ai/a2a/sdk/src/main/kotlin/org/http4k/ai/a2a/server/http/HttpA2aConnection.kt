package org.http4k.ai.a2a.server.http

import dev.forkhandles.result4k.fold
import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.ai.a2a.server.protocol.A2AProtocolResponse.Single
import org.http4k.ai.a2a.server.protocol.A2AProtocolResponse.Stream
import org.http4k.ai.a2a.server.util.toSseStream
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

private val jsonRpcResponseLens = Body.auto<A2ANodeType>().toLens()
