package org.http4k.ai.mcp.server.protocol

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationRequest.Form
import org.http4k.ai.mcp.ElicitationRequest.Url
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.McpError.Protocol
import org.http4k.ai.mcp.McpError.Timeout
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.SamplingResponse
import org.http4k.ai.mcp.model.CompletionStatus.Finished
import org.http4k.ai.mcp.model.CompletionStatus.InProgress
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpSampling
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.protocol.messages.fromJsonRpc
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.util.McpJson
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.Long.Companion.MAX_VALUE
import kotlin.random.Random

class SessionBasedClient<Transport>(
    private val progressToken: ProgressToken,
    private val context: ClientRequestContext,
    private val sessions: Sessions<Transport>,
    private val logger: Logger,
    private val random: Random,
    private val clientTracking: () -> ClientTracking?
) : Client {

    override fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration?): McpResult<ElicitationResponse> {
        val id = McpMessageId.random(random)
        val queue = LinkedBlockingQueue<ElicitationResponse>()
        val tracking = clientTracking() ?: return Failure(Protocol(InvalidRequest))

        return when {
            tracking.supportsElicitation -> {
                tracking.trackRequest(id) {
                    with(it.fromJsonRpc<McpElicitations.Response>()) {
                        queue.put(ElicitationResponse(action, content))
                        Finished
                    }
                }

                val protocolRequest = when (request) {
                    is Form -> McpElicitations.Request.Form(
                        request.message,
                        request.requestedSchema,
                        Meta(progressToken)
                    )

                    is Url -> McpElicitations.Request.Url(
                        request.message,
                        request.url,
                        request.elicitationId,
                        Meta(progressToken)
                    )
                }

                sessions.request(
                    context,
                    protocolRequest.toJsonRpc(McpElicitations, McpJson.asJsonObject(id))
                )


                when (val nextMessage = queue.poll(fetchNextTimeout?.toMillis() ?: MAX_VALUE, MILLISECONDS)) {
                    null -> Failure(Timeout)
                    else -> Success(nextMessage)
                }
            }

            else -> Failure(Protocol(InvalidRequest))
        }
    }

    override fun sample(request: SamplingRequest, fetchNextTimeout: Duration?): Sequence<McpResult<SamplingResponse>> {
        val id = McpMessageId.random(random)
        val queue = LinkedBlockingQueue<SamplingResponse>()
        val tracking = clientTracking() ?: return emptySequence()

        return when {
            tracking.supportsSampling -> {
                tracking.trackRequest(id) {
                    with(it.fromJsonRpc<McpSampling.Response>()) {
                        queue.put(SamplingResponse(model, role, content, stopReason))
                        when {
                            stopReason == null -> InProgress
                            else -> Finished
                        }
                    }
                }

                with(request) {
                    sessions.request(
                        context, McpSampling.Request(
                            messages,
                            maxTokens,
                            systemPrompt,
                            includeContext,
                            temperature,
                            stopSequences,
                            modelPreferences,
                            metadata,
                            tools,
                            toolChoice,
                            _meta = Meta(progressToken)
                        ).toJsonRpc(McpSampling, McpJson.asJsonObject(id))
                    )
                }

                sequence {
                    while (true) {
                        when (val nextMessage = queue.poll(
                            fetchNextTimeout?.toMillis() ?: MAX_VALUE,
                            MILLISECONDS
                        )) {
                            null -> {
                                yield(Failure(Timeout))
                                break
                            }

                            else -> {
                                yield(Success(nextMessage))
                                if (nextMessage.stopReason != null) break
                            }
                        }
                    }
                }
            }

            else -> emptySequence()
        }
    }

    override fun progress(progress: Int, total: Double?, description: String?) {
        sessions.request(
            context, McpProgress.Notification(progressToken, progress, total, description)
                .toJsonRpc(McpProgress)
        )
    }

    override fun log(data: Any, level: LogLevel, logger: String?) {
        if (level >= this.logger.levelFor(context.session)) {
            sessions.request(
                context,
                McpLogging.LoggingMessage.Notification(McpJson.asJsonObject(data), level, logger)
                    .toJsonRpc(McpLogging.LoggingMessage)
            )
        }
    }

    override fun elicitationComplete(elicitationId: ElicitationId) {
        sessions.request(
            context,
            McpElicitations.Complete.Notification(elicitationId)
                .toJsonRpc(McpElicitations.Complete)
        )
    }

    override fun tasks(): Client.Tasks = SessionBasedClientTasks(
        progressToken,
        context,
        sessions,
        random,
        clientTracking
    )
}

internal class SessionBasedClientTasks<Transport>(
    private val progressToken: ProgressToken,
    private val context: ClientRequestContext,
    private val sessions: Sessions<Transport>,
    private val random: Random,
    private val clientTracking: () -> ClientTracking?
) : Client.Tasks {

    override fun get(taskId: TaskId, timeout: Duration?): McpResult<Task> {
        val id = McpMessageId.random(random)
        val queue = LinkedBlockingQueue<Task>()
        val tracking = clientTracking() ?: return Failure(Protocol(InvalidRequest))

        return when {
            tracking.supportsTasks -> {
                tracking.trackRequest(id) {
                    with(it.fromJsonRpc<McpTask.Get.Response>()) {
                        queue.put(task)
                        Finished
                    }
                }

                sessions.request(
                    context,
                    McpTask.Get.Request(taskId, Meta(progressToken))
                        .toJsonRpc(McpTask.Get, McpJson.asJsonObject(id))
                )

                when (val result = queue.poll(timeout?.toMillis() ?: MAX_VALUE, MILLISECONDS)) {
                    null -> Failure(Timeout)
                    else -> Success(result)
                }
            }

            else -> Failure(Protocol(InvalidRequest))
        }
    }

    override fun list(timeout: Duration?): McpResult<List<Task>> {
        val id = McpMessageId.random(random)
        val queue = LinkedBlockingQueue<List<Task>>()
        val tracking = clientTracking() ?: return Failure(Protocol(InvalidRequest))

        return when {
            tracking.supportsTasks -> {
                tracking.trackRequest(id) {
                    with(it.fromJsonRpc<McpTask.List.Response>()) {
                        queue.put(tasks)
                        Finished
                    }
                }

                sessions.request(
                    context,
                    McpTask.List.Request(_meta = Meta(progressToken))
                        .toJsonRpc(McpTask.List, McpJson.asJsonObject(id))
                )

                when (val result = queue.poll(timeout?.toMillis() ?: MAX_VALUE, MILLISECONDS)) {
                    null -> Failure(Timeout)
                    else -> Success(result)
                }
            }

            else -> Failure(Protocol(InvalidRequest))
        }
    }

    override fun cancel(taskId: TaskId, timeout: Duration?): McpResult<Unit> {
        val id = McpMessageId.random(random)
        val queue = LinkedBlockingQueue<Unit>()
        val tracking = clientTracking() ?: return Failure(Protocol(InvalidRequest))

        return when {
            tracking.supportsTasks -> {
                tracking.trackRequest(id) {
                    it.fromJsonRpc<McpTask.Cancel.Response>()
                    queue.put(Unit)
                    Finished
                }

                sessions.request(
                    context,
                    McpTask.Cancel.Request(taskId, Meta(progressToken))
                        .toJsonRpc(McpTask.Cancel, McpJson.asJsonObject(id))
                )

                when (queue.poll(timeout?.toMillis() ?: MAX_VALUE, MILLISECONDS)) {
                    null -> Failure(Timeout)
                    else -> Success(Unit)
                }
            }

            else -> Failure(Protocol(InvalidRequest))
        }
    }

    override fun result(taskId: TaskId, timeout: Duration?): McpResult<Map<String, Any>?> {
        val id = McpMessageId.random(random)
        val queue = LinkedBlockingQueue<Map<String, Any>>()
        val tracking = clientTracking() ?: return Failure(Protocol(InvalidRequest))

        return when {
            tracking.supportsTasks -> {
                tracking.trackRequest(id) {
                    with(it.fromJsonRpc<McpTask.Result.Response>()) {
                        queue.put(result ?: emptyMap())
                        Finished
                    }
                }

                sessions.request(
                    context,
                    McpTask.Result.Request(taskId, Meta(progressToken))
                        .toJsonRpc(McpTask.Result, McpJson.asJsonObject(id))
                )

                when (val result = queue.poll(timeout?.toMillis() ?: MAX_VALUE, MILLISECONDS)) {
                    null -> Failure(Timeout)
                    else -> Success(result.takeIf { it.isNotEmpty() })
                }
            }

            else -> Failure(Protocol(InvalidRequest))
        }
    }

    override fun update(task: Task, meta: Meta, timeout: Duration?) {
        sessions.request(
            context,
            McpTask.Status.Notification(task, meta)
                .toJsonRpc(McpTask.Status)
        )
    }
}
