package org.http4k.mcp.server.protocol

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.core.Request
import org.http4k.format.MoshiArray
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.messages.Cancelled
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpLogging
import org.http4k.mcp.protocol.messages.McpPing
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpRoot
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.protocol.messages.ServerMessage
import org.http4k.mcp.protocol.messages.fromJsonRpc
import org.http4k.mcp.protocol.messages.toJsonRpc
import org.http4k.mcp.server.capability.CompletionCapability
import org.http4k.mcp.server.capability.PromptCapability
import org.http4k.mcp.server.capability.ResourceCapability
import org.http4k.mcp.server.capability.ServerCapability
import org.http4k.mcp.server.capability.ServerCompletions
import org.http4k.mcp.server.capability.ServerPrompts
import org.http4k.mcp.server.capability.ServerResources
import org.http4k.mcp.server.capability.ServerRoots
import org.http4k.mcp.server.capability.ServerTools
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.server.protocol.Client.Companion.NoOp
import org.http4k.mcp.server.protocol.ClientRequestContext.ClientCall
import org.http4k.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpJson.asJsonObject
import org.http4k.mcp.util.McpJson.nullNode
import org.http4k.mcp.util.McpJson.parse
import org.http4k.mcp.util.McpNodeType
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Models the MCP protocol in terms of message handling and session management.
 */
class McpProtocol<Transport>(
    internal val metaData: ServerMetaData,
    private val sessions: Sessions<Transport>,
    private val tools: Tools = ServerTools(),
    private val resources: Resources = ServerResources(),
    private val prompts: Prompts = ServerPrompts(),
    private val completions: Completions = ServerCompletions(),
    private val logger: Logger = ServerLogger(),
    private val roots: Roots = ServerRoots(),
    private val random: Random = Random
) {
    constructor(
        metaData: ServerMetaData,
        sessions: Sessions<Transport>,
        vararg capabilities: ServerCapability
    ) : this(
        metaData,
        sessions,
        ServerTools(capabilities.flatMap { it }.filterIsInstance<ToolCapability>()),
        ServerResources(capabilities.flatMap { it }.filterIsInstance<ResourceCapability>()),
        ServerPrompts(capabilities.flatMap { it }.filterIsInstance<PromptCapability>()),
        ServerCompletions(capabilities.flatMap { it }.filterIsInstance<CompletionCapability>()),
    )

    private val clientTracking = ConcurrentHashMap<Session, ClientTracking>()

    fun receive(transport: Transport, session: Session, httpReq: Request): Result4k<McpNodeType, McpNodeType> {
        val rawPayload = runCatching { parse(httpReq.bodyString()) }.getOrElse { return error() }

        return when (rawPayload) {
            is MoshiArray -> {
                Success(
                    MoshiArray(
                        rawPayload.elements
                            .filterIsInstance<MoshiObject>()
                            .map { processMessage(it, transport, session, httpReq) }
                            .map { it.get() }
                    )
                )
            }

            is MoshiObject -> processMessage(rawPayload, transport, session, httpReq)
            else -> error()
        }
    }

    private fun ok() = Success(nullNode())
    private fun error() = Failure(nullNode())

    private fun processMessage(
        rawPayload: MoshiObject,
        transport: Transport,
        session: Session,
        httpReq: Request
    ): Result4k<McpNodeType, McpNodeType> {
        val payload = McpJson.fields(rawPayload).toMap()
        return when {
            payload["method"] != null -> {
                val jsonReq = JsonRpcRequest(McpJson, payload)

                when (McpRpcMethod.of(jsonReq.method)) {
                    McpInitialize.Method ->
                        transport.respondTo<McpInitialize.Request>(session, jsonReq, httpReq) { it, _ ->
                            assign(Subscription(session), transport, httpReq)
                            handleInitialize(it, session)
                        }

                    McpCompletion.Method ->
                        transport.respondTo<McpCompletion.Request>(session, jsonReq, httpReq) { it, c ->
                            completions.complete(it, c, httpReq)
                        }

                    McpPing.Method ->
                        transport.respondTo<McpPing.Request>(session, jsonReq, httpReq) { _, _ ->
                            ServerMessage.Response.Empty
                        }

                    McpPrompt.Get.Method ->
                        transport.respondTo<McpPrompt.Get.Request>(session, jsonReq, httpReq) { it, c ->
                            prompts.get(it, c, httpReq)
                        }

                    McpPrompt.List.Method ->
                        transport.respondTo<McpPrompt.List.Request>(session, jsonReq, httpReq) { it, c ->
                            prompts.list(it, c, httpReq)
                        }

                    McpResource.Template.List.Method ->
                        transport.respondTo<McpResource.Template.List.Request>(session, jsonReq, httpReq) { it, c ->
                            resources.listTemplates(it, c, httpReq)
                        }

                    McpResource.List.Method ->
                        transport.respondTo<McpResource.List.Request>(session, jsonReq, httpReq) { it, c ->
                            resources.listResources(it, c, httpReq)
                        }

                    McpResource.Read.Method -> {
                        transport.respondTo<McpResource.Read.Request>(session, jsonReq, httpReq) { it, c ->
                            resources.read(it, c, httpReq)
                        }
                    }

                    McpResource.Subscribe.Method -> {
                        val subscribeRequest = jsonReq.fromJsonRpc<McpResource.Subscribe.Request>()
                        resources.subscribe(session, subscribeRequest) {
                            sessions.respond(
                                transport,
                                session,
                                McpResource.Updated.Notification(subscribeRequest.uri).toJsonRpc(McpResource.Updated)
                            )
                        }
                        ok()
                    }

                    McpLogging.SetLevel.Method -> {
                        logger.setLevel(session, jsonReq.fromJsonRpc<McpLogging.SetLevel.Request>().level)
                        ok()
                    }

                    McpResource.Unsubscribe.Method -> {
                        resources.unsubscribe(session, jsonReq.fromJsonRpc())
                        ok()
                    }

                    McpInitialize.Initialized.Method -> ok()

                    Cancelled.Method -> ok()

                    McpProgress.Method -> ok()

                    McpRoot.Changed.Method -> {
                        clientTracking[session]?.let {
                            if (it.supportsRoots) {
                                val messageId = McpMessageId.random(random)
                                it.trackRequest(messageId) { roots.update(it.fromJsonRpc()) }

                                sessions.respond(
                                    transport,
                                    session,
                                    McpRoot.List.Request().toJsonRpc(McpRoot.List, asJsonObject(messageId))
                                )
                            }
                        }
                        ok()
                    }

                    McpTool.Call.Method ->
                        transport.respondTo<McpTool.Call.Request>(session, jsonReq, httpReq) { it, c ->
                            tools.call(it, c, httpReq)
                        }

                    McpTool.List.Method ->
                        transport.respondTo<McpTool.List.Request>(session, jsonReq, httpReq) { it, c ->
                            tools.list(it, c, httpReq)
                        }

                    else -> sessions.respond(transport, session, MethodNotFound.toJsonRpc(jsonReq.id))
                }
            }

            else -> {
                val jsonResult = JsonRpcResult(McpJson, payload)
                when {
                    jsonResult.isError() -> ok()
                    else -> with(McpJson) {
                        val id = jsonResult.id?.let { McpMessageId.parse(compact(it)) }
                        when (id) {
                            null -> error()
                            else -> clientTracking[session]?.processResult(id, jsonResult)?.let { ok() }
                                ?: error()
                        }
                    }
                }
            }
        }
    }

    private inline fun <reified IN : ClientMessage.Request> Transport.respondTo(
        session: Session,
        jsonReq: JsonRpcRequest<MoshiNode>,
        httpReq: Request,
        fn: (IN, Client) -> ServerMessage.Response
    ) = sessions.respond(this, session, jsonReq.runCatching { jsonReq.fromJsonRpc<IN>() }
        .mapCatching {
            when (val progress = it._meta.progress) {
                null -> fn(it, NoOp)
                else -> {
                    val context = ClientCall(progress, session)
                    sessions.assign(context, this, httpReq)
                    fn(it, ProgressClient(progress, context, sessions, random) { clientTracking[session] })
                        .also { sessions.end(context) }
                }
            }
        }
        .map { it.toJsonRpc(jsonReq.id) }
        .recover {
            when (it) {
                is McpException -> it.error.toJsonRpc(jsonReq.id)
                else -> {
                    // TODO do better here
                    it.printStackTrace()
                    InternalError.toJsonRpc(jsonReq.id)
                }
            }
        }
        .getOrElse { InvalidRequest.toJsonRpc(jsonReq.id) })

    fun handleInitialize(request: McpInitialize.Request, session: Session): McpInitialize.Response {
        clientTracking[session] = ClientTracking(request)

        val context = Subscription(session)
        logger.subscribe(session, LogLevel.error) { level, logger, data ->
            sessions.request(
                context,
                McpLogging.LoggingMessage.Notification(level, logger, data).toJsonRpc(McpLogging.LoggingMessage)
            )
        }
        prompts.onChange(session) {
            sessions.request(
                context,
                McpPrompt.List.Changed.Notification.toJsonRpc(McpPrompt.List.Changed)
            )
        }
        resources.onChange(session) {
            sessions.request(
                context,
                McpResource.List.Changed.Notification.toJsonRpc(McpResource.List.Changed)
            )
        }
        tools.onChange(session) {
            sessions.request(
                context,
                McpTool.List.Changed.Notification.toJsonRpc(McpTool.List.Changed)
            )
        }
        sessions.onClose(context) {
            prompts.remove(session)
            resources.remove(session)
            tools.remove(session)
            logger.unsubscribe(session)
        }
        return McpInitialize.Response(
            metaData.entity, metaData.capabilities, when {
                metaData.protocolVersions.contains(request.protocolVersion) -> request.protocolVersion
                else -> metaData.protocolVersions.max()
            }
        )
    }

    fun retrieveSession(req: Request) = sessions.retrieveSession(req)

    fun end(method: ClientRequestContext) {
        if (method is Subscription) clientTracking.remove(method.session)
        sessions.end(method)
    }

    fun assign(context: ClientRequestContext, transport: Transport, connectRequest: Request) =
        sessions.assign(context, transport, connectRequest)

    fun transportFor(context: ClientRequestContext) = sessions.transportFor(context)
}


