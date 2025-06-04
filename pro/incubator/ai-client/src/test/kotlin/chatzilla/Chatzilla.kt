package chatzilla

import org.http4k.ai.llm.chat.ChatSessionHandler
import org.http4k.ai.llm.chat.ChatSessionState
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.llm.chat.ChatSessionState.Responding
import org.http4k.ai.llm.chat.ChatSessionStateMachine
import org.http4k.ai.llm.memory.InMemory
import org.http4k.ai.llm.memory.LLMMemory
import org.http4k.ai.llm.model.ModelParams
import org.http4k.ai.llm.tools.McpTools
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.ai.llm.chat.ChatJson.auto
import org.http4k.mcp.client.http.HttpStreamingMcpClient
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.Version
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static

data class UserMessageRequest(val message: String)
data class ChatzillaResponse(val state: ChatSessionState, val message: String? = null)

val userMessageLens = Body.auto<UserMessageRequest>().toLens()
val responseLens = Body.auto<ChatzillaResponse>().toLens()

fun Chatzilla(url: Uri): RoutingHttpHandler {
    val client = HttpStreamingMcpClient(
        McpEntity.of("mcp"), Version.of("1.0"),
        url,
    )
    val stateMachine = ChatSessionStateMachine(
        AChatLLM(),
        LLMMemory.InMemory(),
        McpTools(client)
    ) { ModelParams(tools = it) }.start()
    val handler = ChatSessionHandler(stateMachine)

    return routes(
        "/api" bind
            routes(
                "/state" bind GET to {
                    val currentState = handler.currentState()
                    val message = when (currentState) {
                        is Responding -> currentState.response.message.toString()
                        is AwaitingApproval -> "Tool approval needed for: ${currentState.pendingTools.first().name}"
                        else -> null
                    }
                    Response(OK).with(responseLens of ChatzillaResponse(currentState, message))
                },

                "/message" bind POST to { request ->
                    val userMessage = userMessageLens(request)
                    val newState = handler.onUserMessage(userMessage.message)
                    Response(OK).with(responseLens of ChatzillaResponse(newState))
                },

                "/tool/approve" bind POST to {
                    Response(OK).with(responseLens of ChatzillaResponse(handler.onToolApprove()))
                },

                "/tool/reject" bind POST to {
                    Response(OK).with(responseLens of ChatzillaResponse(handler.onToolReject()))
                },

                "/end" bind POST to {
                    Response(OK).with(responseLens of ChatzillaResponse(handler.onEnd()))
                }
            ),
        static(Classpath("public")),
    )
}
