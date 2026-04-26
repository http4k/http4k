/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpLogging
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpRoot
import org.http4k.ai.mcp.protocol.messages.McpSampling
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.util.McpJson
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.lens.MetaKey
import org.http4k.lens.progressToken
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.Long.Companion.MAX_VALUE
import kotlin.random.Random

class SessionBasedClient(
    private val sendToClient: (McpJsonRpcRequest) -> Unit,
    private val session: Session,
    private val logger: Logger,
    private val tasks: Tasks,
    private val roots: Roots,
    private val random: Random,
    private val clientTracking: () -> ClientTracking
) : Client {

    override fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration?): McpResult<ElicitationResponse> {
        val id = McpMessageId.random(random)
        val queue = LinkedBlockingQueue<ElicitationResponse>()
        val tracking = clientTracking()

        return when {
            tracking.supportsElicitation -> {
                tracking.trackRequest(id) {
                    with(McpJson.asA<McpElicitations.Response.Result>(McpJson.compact(it))) {
                        val t = task
                        val response = when {
                            t != null -> ElicitationResponse.Task(t)
                            else -> ElicitationResponse.Ok(action!!, content!!, _meta)
                        }
                        queue.put(response)
                        Finished
                    }
                }

                val protocolRequest = when (request) {
                    is Form -> McpElicitations.Request.Params.Form(
                        request.message,
                        request.requestedSchema,
                        Meta(MetaKey.progressToken<Any>().toLens() of request.progressToken),
                        request.task
                    )

                    is Url -> McpElicitations.Request.Params.Url(
                        request.message,
                        request.url,
                        request.elicitationId,
                        Meta(MetaKey.progressToken<Any>().toLens() of request.progressToken),
                        request.task
                    )
                }

                sendToClient(McpElicitations.Request(protocolRequest, McpJson.asJsonObject(id)))

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
        val tracking = clientTracking()

        return when {
            tracking.supportsSampling -> {
                tracking.trackRequest(id) {
                    with(McpJson.asA<McpSampling.Response.Result>(McpJson.compact(it))) {
                        val t = task
                        val response = when {
                            t != null -> SamplingResponse.Task(t)
                            else -> SamplingResponse.Ok(model!!, role!!, content!!, stopReason)
                        }
                        queue.put(response)
                        when {
                            t != null -> Finished
                            stopReason == null -> InProgress
                            else -> Finished
                        }
                    }
                }

                with(request) {
                    sendToClient(
                        McpSampling.Request(
                        McpSampling.Request.Params(
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
                            _meta = Meta(MetaKey.progressToken<Any>().toLens() of progressToken)
                        ), McpJson.asJsonObject(id)
                        ),
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
                                when (nextMessage) {
                                    is SamplingResponse.Ok -> if (nextMessage.stopReason != null) break
                                    else -> break
                                }
                            }
                        }
                    }
                }
            }

            else -> emptySequence()
        }
    }

    override fun progress(progressToken: ProgressToken, progress: Int, total: Double?, description: String?) {
        sendToClient(
            McpProgress.Notification(McpProgress.Notification.Params(progressToken, progress, total, description))
        )
    }

    override fun log(data: Any, level: LogLevel, logger: String?) {
        if (level >= this.logger.levelFor(session)) {
            sendToClient(
                McpLogging.LoggingMessage.Notification(
                    McpLogging.LoggingMessage.Notification.Params(McpJson.asJsonObject(data), level, logger)
                )
            )
        }
    }

    override fun requestRoots(meta: Meta) {
        val tracking = clientTracking()
        if (tracking.supportsRoots) {
            val messageId = McpMessageId.random(random)
            tracking.trackRequest(messageId) { roots.update(McpJson.asA<McpRoot.List.Response.Result>(McpJson.compact(it))) }

            sendToClient(McpRoot.List.Request(McpRoot.List.Request.Params(), McpJson.asJsonObject(messageId)))
        }
    }


    override fun elicitationComplete(elicitationId: ElicitationId) {
        sendToClient(
            McpElicitations.Complete.Notification(
            McpElicitations.Complete.Notification.Params(elicitationId)
            )
        )
    }

    override fun updateTask(task: Task, meta: Meta) {
        val notification = McpTask.Status.Notification.Params(task, meta)
        tasks.update(session, notification)
        sendToClient(McpTask.Status.Notification(notification))
    }

    override fun storeTaskResult(taskId: TaskId, result: Map<String, Any>) {
        tasks.storeResult(session, taskId, result)
    }
}
