package org.http4k.ai.a2a.server.protocol

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.protocol.A2ARpcMethod
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2AResponse
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.protocol.messages.fromJsonRpc
import org.http4k.ai.a2a.protocol.messages.toJsonRpc
import org.http4k.ai.a2a.server.capability.ServerMessages
import org.http4k.ai.a2a.server.capability.ServerPushNotificationConfigs
import org.http4k.ai.a2a.server.capability.ServerTasks
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.nullNode
import org.http4k.ai.a2a.util.A2AJson.parse
import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.core.Request
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.jsonrpc.JsonRpcRequest

/**
 * Models the A2A protocol in terms of message handling.
 */
class A2AProtocol(
    val agentCard: AgentCard,
    private val messages: Messages,
    private val tasks: Tasks = ServerTasks(),
    private val pushNotificationConfigs: PushNotificationConfigs = ServerPushNotificationConfigs(),
    private val onError: (Throwable) -> Unit = { it.printStackTrace(System.err) }
) {
    constructor(
        agentCard: AgentCard,
        messageHandler: MessageHandler,
        tasks: Tasks = ServerTasks(),
        pushNotificationConfigs: PushNotificationConfigs = ServerPushNotificationConfigs(),
        onError: (Throwable) -> Unit = { it.printStackTrace(System.err) }
    ) : this(agentCard, ServerMessages(messageHandler), tasks, pushNotificationConfigs, onError)

    operator fun invoke(httpReq: Request): Result4k<A2AProtocolResponse, A2ANodeType> {
        val rawPayload = runCatching { parse(httpReq.bodyString()) }.getOrElse { return error() }

        return when (rawPayload) {
            is MoshiObject -> processMessage(rawPayload, httpReq)
            else -> error()
        }
    }

    private fun error() = Failure(nullNode())

    private fun processMessage(
        rawPayload: MoshiObject,
        httpReq: Request
    ): Result4k<A2AProtocolResponse, A2ANodeType> {
        val payload = A2AJson.fields(rawPayload).toMap()

        return when {
            payload["method"] != null -> {
                val jsonReq = JsonRpcRequest(A2AJson, payload)

                when (A2ARpcMethod.of(jsonReq.method)) {
                    A2AMessage.Send.Method -> jsonReq.respond {
                        messages.send(jsonReq.fromJsonRpc<A2AMessage.Send.Request>(), httpReq)
                    }

                    A2AMessage.Stream.Method -> handleStream(jsonReq) {
                        messages.stream(jsonReq.fromJsonRpc<A2AMessage.Stream.Request>(), httpReq)
                    }

                    A2ATask.Get.Method -> jsonReq.respond {
                        tasks.get(jsonReq.fromJsonRpc<A2ATask.Get.Request>())
                    }


                    A2ATask.Cancel.Method -> jsonReq.respond {
                        tasks.cancel(jsonReq.fromJsonRpc<A2ATask.Cancel.Request>())
                    }


                    A2ATask.List.Method -> jsonReq.respond {
                        tasks.list(jsonReq.fromJsonRpc<A2ATask.List.Request>())
                    }


                    A2APushNotificationConfig.Set.Method -> jsonReq.respond {
                        pushNotificationConfigs.set(jsonReq.fromJsonRpc<A2APushNotificationConfig.Set.Request>())
                    }


                    A2APushNotificationConfig.Get.Method -> jsonReq.respond {
                        pushNotificationConfigs.get(jsonReq.fromJsonRpc<A2APushNotificationConfig.Get.Request>())
                    }


                    A2APushNotificationConfig.List.Method -> jsonReq.respond {
                        pushNotificationConfigs.list(jsonReq.fromJsonRpc<A2APushNotificationConfig.List.Request>())
                    }


                    A2APushNotificationConfig.Delete.Method -> jsonReq.respond {
                        pushNotificationConfigs.delete(jsonReq.fromJsonRpc<A2APushNotificationConfig.Delete.Request>())
                    }

                    else -> single(MethodNotFound.toJsonRpc(jsonReq.id))
                }
            }

            else -> error()
        }
    }

    private fun <Resp : A2AResponse> handleStream(
        jsonReq: JsonRpcRequest<A2ANodeType>,
        handler: () -> Sequence<Resp>
    ): Result4k<A2AProtocolResponse, A2ANodeType> =
        runCatching {
            Success(A2AProtocolResponse.Stream(handler().map { it.toJsonRpc(jsonReq.id) }))
        }.getOrElse {
            onError(it)
            single(InternalError.toJsonRpc(jsonReq.id))
        }

    private fun <Resp : A2AResponse> JsonRpcRequest<A2ANodeType>.respond(handler: () -> Resp?)
        : Result4k<A2AProtocolResponse, A2ANodeType> =
        runCatching {
            val response = handler()
            if (response != null) {
                single(response.toJsonRpc(this@respond.id))
            } else {
                single(InvalidParams.toJsonRpc(this@respond.id))
            }
        }.getOrElse {
            onError(it)
            single(InternalError.toJsonRpc(id))
        }

    private fun single(response: A2ANodeType) = Success(A2AProtocolResponse.Single(response))
}
