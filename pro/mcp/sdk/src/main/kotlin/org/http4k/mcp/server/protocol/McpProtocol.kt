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
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpError.Timeout
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.model.CompletionStatus.InProgress
import org.http4k.mcp.model.LogLevel
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.Progress
import org.http4k.mcp.model.ProgressToken
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
import org.http4k.mcp.protocol.messages.McpSampling
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
import org.http4k.mcp.server.capability.ServerRequestProgress
import org.http4k.mcp.server.capability.ServerResources
import org.http4k.mcp.server.capability.ServerRoots
import org.http4k.mcp.server.capability.ServerSampling
import org.http4k.mcp.server.capability.ServerTools
import org.http4k.mcp.server.capability.ToolCapability
import org.http4k.mcp.server.protocol.ClientRequestContext.ClientCall
import org.http4k.mcp.server.protocol.ClientRequestContext.Subscription
import org.http4k.mcp.server.protocol.ClientRequestTarget.Entity
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpJson.asJsonObject
import org.http4k.mcp.util.McpJson.nullNode
import org.http4k.mcp.util.McpJson.parse
import org.http4k.mcp.util.McpNodeType
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.Long.Companion.MAX_VALUE
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
    private val sampling: Sampling = ServerSampling(Random),
    private val logger: Logger = ServerLogger(),
    private val roots: Roots = ServerRoots(),
    private val progress: RequestProgress = ServerRequestProgress(),
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

    private val clientRequests = ConcurrentHashMap<Session, ClientTracking>()

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
                        sessions.respond(
                            transport,
                            session,
                            jsonReq.respondTo<McpInitialize.Request> {
                                assign(Subscription(session), transport, httpReq)
                                handleInitialize(it, session)
                            })

                    McpCompletion.Method ->
                        sessions.respond(
                            transport,
                            session,
                            jsonReq.respondTo<McpCompletion.Request> { completions.complete(it, httpReq) }
                        )

                    McpPing.Method -> sessions.respond(
                        transport,
                        session,
                        jsonReq.respondTo<McpPing.Request> { ServerMessage.Response.Empty }
                    )

                    McpPrompt.Get.Method ->
                        sessions.respond(
                            transport,
                            session,
                            jsonReq.respondTo<McpPrompt.Get.Request> { prompts.get(it, httpReq) })

                    McpPrompt.List.Method ->
                        sessions.respond(
                            transport,
                            session,
                            jsonReq.respondTo<McpPrompt.List.Request> { prompts.list(it, httpReq) })

                    McpResource.Template.List.Method ->
                        sessions.respond(transport, session, jsonReq.respondTo<McpResource.Template.List.Request> {
                            resources.listTemplates(it, httpReq)
                        })

                    McpResource.List.Method ->
                        sessions.respond(
                            transport, session,
                            jsonReq.respondTo<McpResource.List.Request> { resources.listResources(it, httpReq) }
                        )

                    McpResource.Read.Method ->
                        sessions.respond(
                            transport,
                            session,
                            jsonReq.respondTo<McpResource.Read.Request> { resources.read(it, httpReq) })

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
                        if (clientRequests[session]?.supportsRoots == true) {
                            val messageId = McpMessageId.random(random)
                            clientRequests[session]?.trackRequest(messageId) { roots.update(it.fromJsonRpc()) }
                            sessions.respond(
                                transport,
                                session,
                                McpRoot.List.Request().toJsonRpc(McpRoot.List, asJsonObject(messageId))
                            )
                        }
                        ok()
                    }

                    McpTool.Call.Method -> {
                        sessions.respond(
                            transport,
                            session,
                            jsonReq.respondTo<McpTool.Call.Request> {
                                val context = it._meta.progress?.let { ClientCall(it, session) }
                                context?.let { sessions.assign(it, transport, httpReq) }
                                tools.call(it, httpReq, Client(session))
                                    .also {
                                        if (context != null) sessions.end(context)
                                    }
                            }
                        )
                    }

                    McpTool.List.Method -> sessions.respond(
                        transport,
                        session,
                        jsonReq.respondTo<McpTool.List.Request> { tools.list(it, httpReq) }
                    )

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
                            else -> clientRequests[session]?.processResult(id, jsonResult)?.let { ok() }
                                ?: error()
                        }
                    }
                }
            }
        }
    }

    private fun Client(session: Session) = object : Client {

        private val responseQueues = ConcurrentHashMap<McpMessageId, BlockingQueue<SamplingResponse>>()

        override fun sample(
            request: SamplingRequest,
            fetchNextTimeout: Duration?
        ): Sequence<McpResult<SamplingResponse>> {
            val queue = LinkedBlockingDeque<SamplingResponse>()
            val id = McpMessageId.random(random)
            responseQueues[id] = queue

            if (clientRequests[session]?.supportsSampling != true) return emptySequence()

            clientRequests[session]?.trackRequest(id) {
                val response: McpSampling.Response = it.fromJsonRpc()
                responseQueues[id]?.put(
                    SamplingResponse(
                        response.model,
                        response.role,
                        response.content,
                        response.stopReason
                    )
                )
                when {
                    response.stopReason == null -> InProgress
                    else -> {
                        responseQueues.remove(id)
                        Finished
                    }
                }
            }

            with(request) {
                sessions.request(
                    request.progressToken.context(session), McpSampling.Request(
                        messages,
                        maxTokens,
                        systemPrompt,
                        includeContext,
                        temperature,
                        stopSequences,
                        modelPreferences,
                        metadata,
                        _meta = Meta(progressToken)
                    ).toJsonRpc(McpSampling, McpJson.asJsonObject(id))
                )
            }
            return sequence {
                while (true) {
                    when (val nextMessage = queue.poll(fetchNextTimeout?.toMillis() ?: MAX_VALUE, MILLISECONDS)) {
                        null -> {
                            yield(Failure(Timeout))
                            break
                        }

                        else -> {
                            yield(Success(nextMessage))

                            if (nextMessage.stopReason != null) {
                                responseQueues.remove(id)
                                break
                            }
                        }
                    }
                }
            }
        }

        override fun report(req: Progress) {
            val mcpReq = with(req) { McpProgress.Notification(progress, total, progressToken) }
                .toJsonRpc(McpProgress)
            sessions.request(req.progressToken.context(session), mcpReq)
        }
    }

    fun handleInitialize(request: McpInitialize.Request, session: Session): McpInitialize.Response {
        val entity = (clientRequests[session] ?: ClientTracking(request).also { clientRequests[session] = it }).entity

        logger.subscribe(session, LogLevel.error) { level, logger, data ->
            sessions.request(
                Subscription(session),
                McpLogging.LoggingMessage.Notification(level, logger, data).toJsonRpc(McpLogging.LoggingMessage)
            )
        }
        prompts.onChange(session) {
            sessions.request(
                Subscription(session),
                McpPrompt.List.Changed.Notification.toJsonRpc(McpPrompt.List.Changed)
            )
        }
        resources.onChange(session) {
            sessions.request(
                Subscription(session),
                McpResource.List.Changed.Notification.toJsonRpc(McpResource.List.Changed)
            )
        }
        tools.onChange(session) {
            sessions.request(
                Subscription(session),
                McpTool.List.Changed.Notification.toJsonRpc(McpTool.List.Changed)
            )
        }
        sampling.onSampleClient(Entity(entity)) { req, id ->
            when {
                clientRequests[session]?.supportsSampling != true -> error("Client does not support sampling")

                else -> {
                    clientRequests[session]?.trackRequest(id) { sampling.receive(id, it.fromJsonRpc()) }
                    sessions.request(Subscription(session), req.toJsonRpc(McpSampling, asJsonObject(id)))
                }
            }
        }

        progress.onProgress(Entity(entity)) {
            sessions.request(Subscription(session), it.toJsonRpc(McpProgress))
        }

        sessions.onClose(session) {
            prompts.remove(session)
            resources.remove(session)
            tools.remove(session)
            logger.unsubscribe(session)
            sampling.remove(Entity(entity))
            progress.remove(Entity(entity))
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
        if (method is Subscription) clientRequests.remove(method.session)
        sessions.end(method)
    }

    fun assign(method: ClientRequestContext, transport: Transport, connectRequest: Request) =
        sessions.assign(method, transport, connectRequest)

    fun transportFor(session: Session) = sessions.transportFor(session)

    private class ClientTracking(initialize: McpInitialize.Request) {
        val entity = initialize.clientInfo.name
        val supportsSampling = initialize.capabilities.sampling != null
        val supportsRoots = initialize.capabilities.roots?.listChanged == true

        private val calls = ConcurrentHashMap<McpMessageId, (JsonRpcResult<McpNodeType>) -> CompletionStatus>()

        fun trackRequest(id: McpMessageId, callback: (JsonRpcResult<McpNodeType>) -> CompletionStatus) {
            calls[id] = callback
        }

        fun processResult(id: McpMessageId, result: JsonRpcResult<MoshiNode>) {
            val done = calls[id]?.invoke(result) ?: Finished
            if (done == Finished) calls.remove(id)
        }
    }
}

private fun ProgressToken?.context(session: Session) = this?.let { ClientCall(it, session) } ?: Subscription(session)

private inline fun <reified IN : ClientMessage.Request> JsonRpcRequest<McpNodeType>.respondTo(fn: (IN) -> ServerMessage.Response) =
    runCatching { fromJsonRpc<IN>() }
        .mapCatching(fn)
        .map { it.toJsonRpc(id) }
        .recover {
            when (it) {
                is McpException -> it.error.toJsonRpc(id)
                else -> InternalError.toJsonRpc(id)
            }
        }
        .getOrElse { InvalidRequest.toJsonRpc(id) }
