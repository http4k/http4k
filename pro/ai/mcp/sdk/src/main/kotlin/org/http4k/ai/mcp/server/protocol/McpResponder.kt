package org.http4k.ai.mcp.server.protocol

import dev.forkhandles.result4k.Result4k
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.ServerMessage
import org.http4k.ai.mcp.protocol.messages.fromJsonRpc
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.server.protocol.ClientRequestContext.ClientCall
import org.http4k.ai.mcp.util.McpJson.asJsonValue
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Request
import org.http4k.format.MoshiNode
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import kotlin.random.Random

class McpResponder<Transport>(
    val transport: Transport,
    val sessions: Sessions<Transport>,
    val tasks: Tasks,
    val logger: Logger,
    val random: Random,
    val clientTracking: Map<Session, ClientTracking>,
    val onError: (Throwable) -> Unit
) {

    inline operator fun <reified IN : ClientMessage.Request> invoke(
        session: Session,
        jsonReq: JsonRpcRequest<MoshiNode>,
        httpReq: Request,
        fn: (IN, Client) -> ServerMessage.Response
    ): Result4k<McpNodeType, McpNodeType> {

        val message: McpNodeType = jsonReq.runCatching { jsonReq.fromJsonRpc<IN>() }
            .mapCatching {
                val progressToken = it._meta.progressToken ?: 0
                val callCtx = ClientCall(progressToken, session, jsonReq.id ?: random.nextLong().asJsonValue())
                try {
                    sessions.assign(callCtx, transport, httpReq)
                    fn(
                        it,
                        SessionBasedClient(
                            progressToken,
                            callCtx,
                            sessions,
                            logger,
                            random,
                            tasks
                        ) { clientTracking[session] })
                } finally {
                    sessions.end(callCtx)
                }
            }
            .map { it.toJsonRpc(jsonReq.id) }
            .recover {
                when (it) {
                    is McpException -> it.error.toJsonRpc(jsonReq.id)
                    else -> {
                        onError(it)
                        ErrorMessage.Companion.InternalError.toJsonRpc(jsonReq.id)
                    }
                }
            }
            .getOrElse { ErrorMessage.Companion.InvalidRequest.toJsonRpc(jsonReq.id) }

        return sessions.respond(transport, session, message)
    }
}
