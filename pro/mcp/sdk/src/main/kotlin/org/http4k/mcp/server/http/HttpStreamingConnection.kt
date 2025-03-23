package org.http4k.mcp.server.http

import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
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
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session.Invalid
import org.http4k.mcp.server.protocol.Session.Valid
import org.http4k.routing.bindSse
import org.http4k.routing.sse
import org.http4k.sse.Sse
import org.http4k.sse.SseResponse

fun HttpStreamingConnection(protocol: McpProtocol<Sse, Response>) =
    "/mcp" bindSse sse(TEXT_EVENT_STREAM.accepted() bindSse { req: Request ->
        when (val session = protocol.validate(req)) {
            is Valid -> SseResponse(
                OK, listOf(
                    CONTENT_TYPE.meta.name to TEXT_EVENT_STREAM.withNoDirectives().value,
                    Header.MCP_SESSION_ID.meta.name to session.sessionId.value,
                )
            ) { sse ->
                with(protocol) {
                    when (req.method) {
                        GET -> {
                            assign(session, sse)
                            protocol.handleInitialize(
                                McpInitialize.Request(
                                    VersionedMcpEntity(
                                        McpEntity.of(session.sessionId.value),
                                        protocol.metaData.entity.version
                                    ),
                                    All
                                ),
                                session.sessionId
                            )
                        }

                        POST -> receive(sse, session.sessionId, req).also { sse.close() }
                        else -> sse.close()
                    }
                }
            }

            is Invalid -> SseResponse(BAD_REQUEST) { it.close() }
        }
    })
