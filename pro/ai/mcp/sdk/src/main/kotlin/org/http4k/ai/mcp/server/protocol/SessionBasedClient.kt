package org.http4k.ai.mcp.server.protocol

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.ElicitationResponse
import org.http4k.ai.mcp.McpError.Protocol
import org.http4k.ai.mcp.McpError.Timeout
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.SamplingRequest
import org.http4k.ai.mcp.SamplingResponse
import org.http4k.ai.mcp.model.CompletionStatus.Finished
import org.http4k.ai.mcp.model.CompletionStatus.InProgress
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.protocol.messages.McpElicitations
import org.http4k.ai.mcp.protocol.messages.McpProgress
import org.http4k.ai.mcp.protocol.messages.McpSampling
import org.http4k.ai.mcp.protocol.messages.fromJsonRpc
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.util.McpJson
import java.time.Duration
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.Long.Companion.MAX_VALUE
import kotlin.random.Random

class SessionBasedClient<Transport>(
    private val progressToken: ProgressToken,
    private val context: ClientRequestContext,
    private val sessions: Sessions<Transport>,
    private val random: Random,
    private val clientTracking: () -> ClientTracking?
) : Client {

    override fun elicit(request: ElicitationRequest, fetchNextTimeout: Duration?): McpResult<ElicitationResponse> {
        val id = McpMessageId.random(random)
        val queue = LinkedBlockingDeque<ElicitationResponse>()
        val tracking = clientTracking() ?: return Failure(Protocol(InvalidRequest))

        return when {
            tracking.supportsElicitations -> {
                tracking.trackRequest(id) {
                    with(it.fromJsonRpc<McpElicitations.Response>()) {
                        queue.put(ElicitationResponse(action, content))
                        Finished
                    }
                }

                with(request) {
                    sessions.request(
                        context, McpElicitations.Request(
                            message, requestedSchema, Meta(progressToken)
                        ).toJsonRpc(McpElicitations, McpJson.asJsonObject(id))
                    )
                }

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
}
