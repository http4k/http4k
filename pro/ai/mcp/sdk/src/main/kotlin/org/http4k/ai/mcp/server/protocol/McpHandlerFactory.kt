package org.http4k.ai.mcp.server.protocol

import dev.forkhandles.result4k.Result4k
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.ServerMessage
import org.http4k.ai.mcp.protocol.messages.fromJsonRpc
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.util.McpJson.asJsonValue
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import kotlin.random.Random
import kotlin.reflect.KClass

class McpHandlerFactory<Transport>(
    val transport: Transport,
    val sessions: Sessions<Transport>,
    val tasks: Tasks,
    val logger: Logger,
    val random: Random,
    val clientTracking: Map<Session, ClientTracking>,
    val onError: (Throwable) -> Unit,
    val filter: McpFilter
) {
    operator fun <IN : ClientMessage.Request> invoke(
        clazz: KClass<IN>,
        fn: (IN, Client) -> ServerMessage.Response
    ): McpHandler =
        filter.then(
            { req ->
                McpResponse(
                    req.json.runCatching { req.json.fromJsonRpc(clazz) }
                        .mapCatching {
                            val progressToken = it._meta.progressToken ?: 0
                            val callCtx = ClientRequestContext.ClientCall(
                                progressToken,
                                req.session,
                                req.json.id ?: random.nextLong().asJsonValue()
                            )
                            try {
                                sessions.assign(callCtx, transport, req.http)
                                fn(
                                    it,
                                    SessionBasedClient(
                                        progressToken,
                                        callCtx,
                                        sessions,
                                        logger,
                                        random,
                                        tasks
                                    ) { clientTracking[req.session] })
                            } finally {
                                sessions.end(callCtx)
                            }
                        }
                        .map { it.toJsonRpc(req.json.id) }
                        .recover {
                            when (it) {
                                is McpException -> it.error.toJsonRpc(req.json.id)
                                else -> {
                                    onError(it)
                                    ErrorMessage.InternalError.toJsonRpc(req.json.id)
                                }
                            }
                        }
                        .getOrElse { ErrorMessage.InvalidRequest.toJsonRpc(req.json.id) }
                )
            }
        )

    inline fun <reified IN : ClientMessage.Request> responder(
        session: Session, jsonReq: JsonRpcRequest<McpNodeType>, httpReq: Request,
        noinline fn: (IN, Client) -> ServerMessage.Response
    ): Result4k<McpNodeType, McpNodeType> =
        sessions.respond(transport, session, this(IN::class, fn)(McpRequest(session, jsonReq, httpReq)).json)
}
