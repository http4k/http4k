package org.http4k.ai.a2a.server.sse

import dev.forkhandles.result4k.fold
import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.ai.a2a.server.protocol.A2AProtocolResponse.Single
import org.http4k.ai.a2a.server.protocol.A2AProtocolResponse.Stream
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingSseHandler
import org.http4k.routing.sse.bind
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse

fun SseA2aConnection(protocol: A2AProtocol, rpcPath: String = "/"): RoutingSseHandler =
    rpcPath bind { req ->
        protocol(req).fold(
            { protocolResponse ->
                when (protocolResponse) {
                    is Single -> SseResponse(OK) { sse ->
                        sse.send(protocolResponse.response.toSseMessage())
                        sse.close()
                    }

                    is Stream -> SseResponse(OK) { sse ->
                        protocolResponse.responses.forEach { sse.send(it.toSseMessage()) }
                        sse.close()
                    }
                }
            },
            {
                SseResponse(BAD_REQUEST) { sse ->
                    sse.send(it.toSseMessage())
                    sse.close()
                }
            }
        )
    }

private fun A2ANodeType.toSseMessage(): SseMessage.Data =
    SseMessage.Data(with(A2AJson) { asCompactJsonString() })
