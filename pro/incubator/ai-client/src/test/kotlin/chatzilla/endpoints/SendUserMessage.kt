package chatzilla.endpoints

import chatzilla.ChatHistory
import org.http4k.ai.llm.chat.ChatJson
import org.http4k.ai.llm.chat.ChatJson.datastarModel
import org.http4k.ai.llm.chat.ChatSessionHandler
import org.http4k.ai.llm.chat.ChatSessionState.AwaitingApproval
import org.http4k.ai.llm.chat.ChatSessionState.Processing
import org.http4k.ai.llm.chat.ChatSessionState.Responding
import org.http4k.ai.llm.chat.ChatSessionState.ToolInvocation
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MergeMode.append
import org.http4k.datastar.Selector
import org.http4k.format.asDatastarSignal
import org.http4k.lens.datastarFragments
import org.http4k.lens.datastarSignals
import org.http4k.routing.bind
import org.http4k.template.DatastarFragmentRenderer

fun SendUserMessage(history: ChatHistory, renderer: DatastarFragmentRenderer, handler: ChatSessionHandler) =
    "/message" bind POST to { req: Request ->
        val message = req.datastarModel<IncomingMessage>().message
        val response = Response(OK)
            .datastarFragments(renderer(history.addUser(message)), append, Selector.of("#chat-container"))
            .datastarSignals(ChatJson.asDatastarSignal(IncomingMessage("")))

        when (val newState = handler.onUserMessage(message)) {
            is AwaitingApproval -> response.datastarFragments(
                renderer(history.addToolConsent(newState.pendingTools.first())),
                append,
                Selector.of("#chat-container")
            )

            is Processing -> response.datastarFragments(
                renderer(history.addAi(newState.message)),
                append,
                Selector.of("#chat-container")
            )

            is Responding -> response.datastarFragments(
                renderer(history.addAi(newState.response.message.text ?: "")),
                append,
                Selector.of("#chat-container")
            )

            is ToolInvocation -> response
            else -> response
        }
    }

data class IncomingMessage(val message: String)
