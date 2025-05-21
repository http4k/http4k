package org.http4k.mcp.server.http

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.accepted
import org.http4k.lens.Header
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.mcp.server.protocol.InvalidSession
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session
import org.http4k.routing.sse
import org.http4k.routing.sse.bind
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage.Event
import org.http4k.sse.SseResponse

fun HttpStreamingMcpConnection(protocol: McpProtocol<Sse>, path: String = "/mcp") =
    path bind sse(TEXT_EVENT_STREAM.accepted() bind { req: Request ->
        when (val session = protocol.retrieveSession(req)) {
            is Session -> SseResponse(
                OK, listOf(
                    CONTENT_TYPE.meta.name to TEXT_EVENT_STREAM.withNoDirectives().value,
                    Header.MCP_SESSION_ID.meta.name to session.id.value,
                )
            ) { sse ->
                with(protocol) {
                    when (req.method) {
                        GET -> {
                            assign(Subscription(session), sse, req)
                            handleInitialize(
                                McpInitialize.Request(
                                    VersionedMcpEntity(
                                        McpEntity.of(session.id.value),
                                        metaData.entity.version
                                    ),
                                    All
                                ),
                                session
                            )
                            sse.send(Event("ping", ""))
                        }

                        POST -> sse.use { receive(it, session, req) }

                        else -> sse.close()
                    }
                }
            }

            is InvalidSession -> SseResponse(BAD_REQUEST) { it.close() }
        }
    })
