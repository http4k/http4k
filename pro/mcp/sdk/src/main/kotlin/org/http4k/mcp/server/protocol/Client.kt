package org.http4k.mcp.server.protocol

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.client.McpError
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.model.CompletionStatus.InProgress
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.ProgressToken
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.protocol.messages.fromJsonRpc
import org.http4k.mcp.protocol.messages.toJsonRpc
import org.http4k.mcp.util.McpJson
import java.time.Duration
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.Long.Companion.MAX_VALUE
import kotlin.random.Random

interface Client {
    fun sample(request: SamplingRequest, fetchNextTimeout: Duration? = null): Sequence<McpResult<SamplingResponse>>
    fun progress(progress: Int, total: Double? = null)

    companion object {
        object NoOp : Client {
            override fun sample(request: SamplingRequest, fetchNextTimeout: Duration?) = error("NoOp")
            override fun progress(progress: Int, total: Double?) = error("NoOp")
        }
    }
}

class ProgressClient<Transport>(
    private val progressToken: ProgressToken,
    private val context: ClientRequestContext,
    private val sessions: Sessions<Transport>,
    private val random: Random,
    private val clientTracking: () -> ClientTracking?
) : Client {
    override fun sample(request: SamplingRequest, fetchNextTimeout: Duration?): Sequence<McpResult<SamplingResponse>> {
        val id = McpMessageId.random(random)
        val queue = LinkedBlockingDeque<SamplingResponse>()
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
                                yield(Failure(McpError.Timeout))
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

    override fun progress(progress: Int, total: Double?) {
        sessions.request(
            context, McpProgress.Notification(progress, total, progressToken)
                .toJsonRpc(McpProgress)
        )
    }
}
