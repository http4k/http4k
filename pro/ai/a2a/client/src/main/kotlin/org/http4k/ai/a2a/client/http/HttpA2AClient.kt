package org.http4k.ai.a2a.client.http

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.a2a.A2AError
import org.http4k.ai.a2a.A2AResult
import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.TaskPushNotificationConfig
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2AJson.auto
import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.client.JavaHttpClient
import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.format.MoshiObject
import org.http4k.format.renderRequest
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.sse.SseMessage
import org.http4k.sse.chunkedSseSequence
import java.util.concurrent.atomic.AtomicLong

private val agentCardLens = Body.auto<AgentCard>().toLens()
private val jsonRpcRequestLens = Body.auto<A2ANodeType>().toLens()

fun A2AClient.Companion.Http(
    baseUri: Uri,
    http: HttpHandler = JavaHttpClient(),
    rpcPath: String = "/",
    agentCardPath: String = "/.well-known/agent.json"
): A2AClient = HttpA2AClient(baseUri, http, rpcPath, agentCardPath)

class HttpA2AClient(
    baseUri: Uri,
    http: HttpHandler,
    private val rpcPath: String = "/",
    private val agentCardPath: String = "/.well-known/agent.json"
) : A2AClient {

    private val client = ClientFilters.SetBaseUriFrom(baseUri).then(http)
    private val requestId = AtomicLong(0)

    override fun agentCard(): A2AResult<AgentCard> {
        val response = client(Request(GET, agentCardPath))
        return Success(agentCardLens(response))
    }

    override fun message(message: Message): A2AResult<A2AMessage.Send.Response> {
        val request = A2AMessage.Send.Request(message)
        val jsonRpcRequest = A2AJson.renderRequest(
            A2AMessage.Send.Method.value,
            A2AJson.asJsonObject(request),
            A2AJson.number(requestId.incrementAndGet())
        )

        val response = client(Request(POST, rpcPath).with(jsonRpcRequestLens of jsonRpcRequest))
        val jsonResult = JsonRpcResult(
            A2AJson,
            A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
        )

        return when {
            jsonResult.isError() -> Failure(A2AError.Protocol(parseErrorMessage(jsonResult.error!!)))
            else -> {
                val resultNode = jsonResult.result as? MoshiObject ?: return Failure(
                    A2AError.Internal(IllegalStateException("Invalid response"))
                )
                val resultFields = A2AJson.fields(resultNode).toMap()

                if (resultFields.containsKey("task")) {
                    Success(A2AJson.asA<A2AMessage.Send.Response.Task>(A2AJson.asFormatString(resultNode)))
                } else {
                    Success(A2AJson.asA<A2AMessage.Send.Response.Message>(A2AJson.asFormatString(resultNode)))
                }
            }
        }
    }

    override fun messageStream(message: Message): A2AResult<Sequence<A2AMessage.Send.Response>> {
        val request = A2AMessage.Stream.Request(message)
        val jsonRpcRequest = A2AJson.renderRequest(
            A2AMessage.Stream.Method.value,
            A2AJson.asJsonObject(request),
            A2AJson.number(requestId.incrementAndGet())
        )

        val response = client(
            Request(POST, rpcPath)
                .with(jsonRpcRequestLens of jsonRpcRequest)
                .header("Accept", "text/event-stream")
        )

        if (!response.status.successful) {
            return Failure(A2AError.Http(response))
        }

        val responseSequence = response.body.stream.chunkedSseSequence()
            .filterIsInstance<SseMessage.Data>()
            .map { sseMessage -> parseStreamResponse(sseMessage.data) }

        return Success(responseSequence)
    }

    private fun parseStreamResponse(json: String): A2AMessage.Send.Response {
        val jsonResult = JsonRpcResult(A2AJson, A2AJson.fields(A2AJson.parse(json) as MoshiObject).toMap())
        val resultNode = jsonResult.result as? MoshiObject
            ?: throw IllegalStateException("Invalid stream response")
        val resultFields = A2AJson.fields(resultNode).toMap()

        return if (resultFields.containsKey("task")) {
            A2AJson.asA<A2AMessage.Send.Response.Task>(A2AJson.asFormatString(resultNode))
        } else {
            A2AJson.asA<A2AMessage.Send.Response.Message>(A2AJson.asFormatString(resultNode))
        }
    }

    override fun tasks(): A2AClient.Tasks = HttpA2ATasks()

    override fun pushNotificationConfigs(): A2AClient.PushNotificationConfigs = HttpA2APushNotificationConfigs()

    private inner class HttpA2ATasks : A2AClient.Tasks {
        override fun get(taskId: TaskId): A2AResult<Task> {
            val request = A2ATask.Get.Request(taskId)
            val jsonRpcRequest = A2AJson.renderRequest(
                A2ATask.Get.Method.value,
                A2AJson.asJsonObject(request),
                A2AJson.number(requestId.incrementAndGet())
            )

            val response = client(Request(POST, rpcPath).with(jsonRpcRequestLens of jsonRpcRequest))
            val jsonResult = JsonRpcResult(
                A2AJson,
                A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
            )

            return when {
                jsonResult.isError() -> Failure(A2AError.Protocol(parseErrorMessage(jsonResult.error!!)))
                else -> {
                    val taskResponse =
                        A2AJson.asA<A2ATask.Get.Response>(A2AJson.asFormatString(jsonResult.result as MoshiObject))
                    Success(taskResponse.task)
                }
            }
        }

        override fun cancel(taskId: TaskId): A2AResult<Task> {
            val request = A2ATask.Cancel.Request(taskId)
            val jsonRpcRequest = A2AJson.renderRequest(
                A2ATask.Cancel.Method.value,
                A2AJson.asJsonObject(request),
                A2AJson.number(requestId.incrementAndGet())
            )

            val response = client(Request(POST, rpcPath).with(jsonRpcRequestLens of jsonRpcRequest))
            val jsonResult = JsonRpcResult(
                A2AJson,
                A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
            )

            return when {
                jsonResult.isError() -> Failure(A2AError.Protocol(parseErrorMessage(jsonResult.error!!)))
                else -> {
                    val taskResponse =
                        A2AJson.asA<A2ATask.Cancel.Response>(A2AJson.asFormatString(jsonResult.result as MoshiObject))
                    Success(taskResponse.task)
                }
            }
        }

        override fun list(request: A2ATask.List.Request): A2AResult<A2ATask.List.Response> {
            val jsonRpcRequest = A2AJson.renderRequest(
                A2ATask.List.Method.value,
                A2AJson.asJsonObject(request),
                A2AJson.number(requestId.incrementAndGet())
            )

            val response = client(Request(POST, rpcPath).with(jsonRpcRequestLens of jsonRpcRequest))
            val jsonResult = JsonRpcResult(
                A2AJson,
                A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
            )

            return when {
                jsonResult.isError() -> Failure(A2AError.Protocol(parseErrorMessage(jsonResult.error!!)))
                else -> Success(A2AJson.asA<A2ATask.List.Response>(A2AJson.asFormatString(jsonResult.result as MoshiObject)))
            }
        }
    }

    private inner class HttpA2APushNotificationConfigs : A2AClient.PushNotificationConfigs {
        override fun set(
            taskId: TaskId,
            config: PushNotificationConfig
        ): A2AResult<TaskPushNotificationConfig> {
            val request = A2APushNotificationConfig.Set.Request(taskId, config)
            val jsonRpcRequest = A2AJson.renderRequest(
                A2APushNotificationConfig.Set.Method.value,
                A2AJson.asJsonObject(request),
                A2AJson.number(requestId.incrementAndGet())
            )

            val response = client(Request(POST, rpcPath).with(jsonRpcRequestLens of jsonRpcRequest))
            val jsonResult = JsonRpcResult(
                A2AJson,
                A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
            )

            return when {
                jsonResult.isError() -> Failure(A2AError.Protocol(parseErrorMessage(jsonResult.error!!)))
                else -> {
                    val setResponse = A2AJson.asA<A2APushNotificationConfig.Set.Response>(
                        A2AJson.asFormatString(jsonResult.result as MoshiObject)
                    )
                    Success(
                        TaskPushNotificationConfig(
                            setResponse.id,
                            setResponse.taskId,
                            setResponse.pushNotificationConfig
                        )
                    )
                }
            }
        }

        override fun get(id: PushNotificationConfigId): A2AResult<TaskPushNotificationConfig> {
            val request = A2APushNotificationConfig.Get.Request(id)
            val jsonRpcRequest = A2AJson.renderRequest(
                A2APushNotificationConfig.Get.Method.value,
                A2AJson.asJsonObject(request),
                A2AJson.number(requestId.incrementAndGet())
            )

            val response = client(Request(POST, rpcPath).with(jsonRpcRequestLens of jsonRpcRequest))
            val jsonResult = JsonRpcResult(
                A2AJson,
                A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
            )

            return when {
                jsonResult.isError() -> Failure(A2AError.Protocol(parseErrorMessage(jsonResult.error!!)))
                else -> {
                    val getResponse = A2AJson.asA<A2APushNotificationConfig.Get.Response>(
                        A2AJson.asFormatString(jsonResult.result as MoshiObject)
                    )
                    Success(
                        TaskPushNotificationConfig(
                            getResponse.id,
                            getResponse.taskId,
                            getResponse.pushNotificationConfig
                        )
                    )
                }
            }
        }

        override fun list(taskId: TaskId): A2AResult<List<TaskPushNotificationConfig>> {
            val request = A2APushNotificationConfig.List.Request(taskId)
            val jsonRpcRequest = A2AJson.renderRequest(
                A2APushNotificationConfig.List.Method.value,
                A2AJson.asJsonObject(request),
                A2AJson.number(requestId.incrementAndGet())
            )

            val response = client(Request(POST, rpcPath).with(jsonRpcRequestLens of jsonRpcRequest))
            val jsonResult = JsonRpcResult(
                A2AJson,
                A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
            )

            return when {
                jsonResult.isError() -> Failure(A2AError.Protocol(parseErrorMessage(jsonResult.error!!)))
                else -> {
                    val listResponse = A2AJson.asA<A2APushNotificationConfig.List.Response>(
                        A2AJson.asFormatString(jsonResult.result as MoshiObject)
                    )
                    Success(listResponse.configs)
                }
            }
        }

        override fun delete(id: PushNotificationConfigId): A2AResult<PushNotificationConfigId> {
            val request = A2APushNotificationConfig.Delete.Request(id)
            val jsonRpcRequest = A2AJson.renderRequest(
                A2APushNotificationConfig.Delete.Method.value,
                A2AJson.asJsonObject(request),
                A2AJson.number(requestId.incrementAndGet())
            )

            val response = client(Request(POST, rpcPath).with(jsonRpcRequestLens of jsonRpcRequest))
            val jsonResult = JsonRpcResult(
                A2AJson,
                A2AJson.fields(A2AJson.parse(response.bodyString()) as MoshiObject).toMap()
            )

            return when {
                jsonResult.isError() -> Failure(A2AError.Protocol(parseErrorMessage(jsonResult.error!!)))
                else -> {
                    val deleteResponse = A2AJson.asA<A2APushNotificationConfig.Delete.Response>(
                        A2AJson.asFormatString(jsonResult.result as MoshiObject)
                    )
                    Success(deleteResponse.id)
                }
            }
        }
    }

    private fun parseErrorMessage(errorNode: A2ANodeType): ErrorMessage {
        val fields = A2AJson.fields(errorNode as MoshiObject).toMap()
        val code = (fields["code"] as? Number)?.toInt() ?: -1
        val message = A2AJson.text(fields["message"] ?: A2AJson.string("Unknown error"))
        return ErrorMessage(code, message)
    }

    override fun close() {}
}
