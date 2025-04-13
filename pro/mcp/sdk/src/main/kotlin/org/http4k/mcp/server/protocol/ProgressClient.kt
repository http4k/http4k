package org.http4k.mcp.server.protocol

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.ProgressToken
import org.http4k.mcp.Client
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.protocol.messages.fromJsonRpc
import org.http4k.mcp.protocol.messages.toJsonRpc
import org.http4k.mcp.McpResult
import org.http4k.mcp.McpError
import org.http4k.mcp.util.McpJson
import java.time.Duration
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import kotlin.random.Random

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
                            stopReason == null -> org.http4k.mcp.model.CompletionStatus.InProgress
                            else -> org.http4k.mcp.model.CompletionStatus.Finished
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
                            fetchNextTimeout?.toMillis() ?: Long.MAX_VALUE,
                            TimeUnit.MILLISECONDS
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

    override fun progress(progress: Int, total: Double?, description: String?) {
        sessions.request(
            context, McpProgress.Notification(progressToken, progress, total, description)
                .toJsonRpc(McpProgress)
        )
    }
}
